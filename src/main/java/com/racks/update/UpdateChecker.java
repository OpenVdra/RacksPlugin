package com.racks.update;

import com.racks.scheduler.Scheduler;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One outbound version check, run once on enable and never again.
 *
 * <p>Modrinth is asked first, because that is where releases land. If it cannot be reached, or
 * answers with something unusable, the latest GitHub release stands in — the same jar is published
 * to both, so either source gives the right answer, and a server behind a firewall that blocks one
 * of them still gets told about an update.
 *
 * <p>Nothing here throws. A failed check logs one line and leaves {@link #updateAvailable()} false:
 * a plugin that refuses to start because it could not reach the internet would be far worse than one
 * that quietly misses an update notice. The two JSON reads are regexes rather than a parsed
 * document, so the check pulls in no JSON library for two fields.
 */
public final class UpdateChecker {

    /** Modrinth resolves a project ID in a page URL, so this needs no slug and survives a rename. */
    public static final String MODRINTH_PAGE = "https://modrinth.com/plugin/P7gg6mhX";
    private static final String MODRINTH_API = "https://api.modrinth.com/v2/project/P7gg6mhX/version";
    /** Versions come back newest first, so the first match is the latest one. */
    private static final Pattern MODRINTH_VERSION_PATTERN =
            Pattern.compile("\"version_number\"\\s*:\\s*\"([^\"]+)\"");

    public static final String GITHUB_RELEASES =
            "https://github.com/OpenVdra/RacksPlugin/releases/latest";
    private static final String GITHUB_API =
            "https://api.github.com/repos/OpenVdra/RacksPlugin/releases/latest";
    private static final Pattern GITHUB_TAG_PATTERN =
            Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern GITHUB_HTML_URL_PATTERN =
            Pattern.compile("\"html_url\"\\s*:\\s*\"(https://github\\.com/[^\"]+/releases/[^\"]+)\"");

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final String currentVersion;
    private final Logger log;

    private volatile @Nullable String latestVersion;
    /**
     * Where players are pointed to download the update. Modrinth unless the Modrinth lookup failed
     * and the GitHub fallback answered, in which case it is that release's own page.
     */
    private volatile String downloadUrl = MODRINTH_PAGE;
    private volatile boolean updateAvailable;

    public UpdateChecker(String currentVersion, Logger log) {
        this.currentVersion = currentVersion;
        this.log = log;
    }

    public String currentVersion() {
        return currentVersion;
    }

    public @Nullable String latestVersion() {
        return latestVersion;
    }

    public String downloadUrl() {
        return downloadUrl;
    }

    public boolean updateAvailable() {
        return updateAvailable;
    }

    /** Runs the check off the server thread. Returns immediately. */
    public void checkAsync(Scheduler scheduler) {
        scheduler.runAsync(this::performCheck);
    }

    private void performCheck() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        String version = fetchModrinthVersion(client);
        String source = MODRINTH_PAGE;

        if (version == null) {
            String[] github = fetchGithubLatest(client);
            if (github != null) {
                version = github[0];
                source = github[1];
            }
        }

        if (version == null) {
            log.warn("Could not check for updates: neither Modrinth nor GitHub could be reached.");
            return;
        }

        latestVersion = version;
        downloadUrl = source;
        if (isNewer(version, currentVersion)) {
            updateAvailable = true;
            printUpdateBanner();
        }
    }

    /** @return the newest Modrinth {@code version_number}, or null when it could not be read. */
    private @Nullable String fetchModrinthVersion(HttpClient client) {
        try {
            HttpResponse<String> response = client.send(
                    get(MODRINTH_API).build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return null;

            Matcher matcher = MODRINTH_VERSION_PATTERN.matcher(response.body());
            return matcher.find() ? matcher.group(1) : null;
        } catch (Exception e) {
            log.warn("Could not reach Modrinth for the update check: {}", e.getMessage());
            return null;
        }
    }

    /**
     * @return {@code [version, releaseUrl]} from the latest GitHub release, or null when it could not
     *         be read. A leading {@code v} on the tag is stripped so it compares against the plugin's
     *         own version.
     */
    private String @Nullable [] fetchGithubLatest(HttpClient client) {
        try {
            HttpResponse<String> response = client.send(
                    get(GITHUB_API).header("Accept", "application/vnd.github+json").build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return null;

            Matcher tag = GITHUB_TAG_PATTERN.matcher(response.body());
            if (!tag.find()) return null;
            String version = tag.group(1).replaceFirst("^[vV]", "");

            Matcher html = GITHUB_HTML_URL_PATTERN.matcher(response.body());
            return new String[]{version, html.find() ? html.group(1) : GITHUB_RELEASES};
        } catch (Exception e) {
            log.warn("Could not reach GitHub for the update check: {}", e.getMessage());
            return null;
        }
    }

    private HttpRequest.Builder get(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .header("User-Agent", "Racks/" + currentVersion + " (update-check)")
                .GET();
    }

    private void printUpdateBanner() {
        String rule = "----------------[ Racks ]----------------";
        log.warn(rule);
        log.warn("  A new version is available.");
        log.warn("  Running  : {}", currentVersion);
        log.warn("  Latest   : {}", latestVersion);
        log.warn("  Download : {}", downloadUrl);
        log.warn(rule);
    }

    /** True when {@code latest} is strictly newer than {@code current}. Package-private for testing. */
    static boolean isNewer(String latest, String current) {
        int[] l = parseVersion(latest);
        int[] c = parseVersion(current);
        for (int i = 0; i < Math.max(l.length, c.length); i++) {
            int lv = i < l.length ? l[i] : 0;
            int cv = i < c.length ? c[i] : 0;
            if (lv != cv) return lv > cv;
        }
        return false;
    }

    private static int[] parseVersion(String version) {
        // Drop any pre-release suffix (-SNAPSHOT, -RC1) so only the numbers are compared.
        String[] parts = version.replaceAll("[^0-9.].*", "").split("\\.");
        int[] numbers = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                numbers[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException ignored) {
                // Leave it 0; a version segment that is not a number cannot order anything.
            }
        }
        return numbers;
    }
}
