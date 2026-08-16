package com.racks.update;

import com.racks.lang.LanguageManager;
import com.racks.scheduler.Scheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Tells operators about an update when they join. Nobody else is notified, and nothing is sent when
 * the check found nothing or never finished.
 */
public final class UpdateNotifyListener implements Listener {

    /** Long enough for the join messages to finish scrolling past before this one lands. */
    private static final long DELAY_TICKS = 40L;

    private final Scheduler scheduler;
    private final UpdateChecker checker;
    private final LanguageManager lang;

    public UpdateNotifyListener(Scheduler scheduler, UpdateChecker checker, LanguageManager lang) {
        this.scheduler = scheduler;
        this.checker = checker;
        this.lang = lang;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!checker.updateAvailable()) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.isOp()) {
            return;
        }

        scheduler.runAtEntityLater(player, () -> {
            if (!player.isOnline()) {
                return;
            }
            player.sendMessage(lang.get("update.available",
                    "current", checker.currentVersion(),
                    "latest", String.valueOf(checker.latestVersion())));

            // The click has to be attached to the component here rather than written as a
            // <click:...> tag in messages.yml: a placeholder inside a tag attribute is not
            // substituted per viewer. The visible label stays translatable, so it still resolves in
            // each operator's own language, and the hover shows where the click actually goes.
            String url = checker.downloadUrl();
            Component link = lang.get("update.download-label")
                    .color(NamedTextColor.AQUA)
                    .decorate(TextDecoration.UNDERLINED)
                    .clickEvent(ClickEvent.openUrl(url))
                    .hoverEvent(HoverEvent.showText(Component.text(url)));
            player.sendMessage(lang.getRich("update.download", "url", link));
        }, DELAY_TICKS);
    }
}
