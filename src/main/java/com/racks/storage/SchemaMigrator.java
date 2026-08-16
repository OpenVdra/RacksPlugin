package com.racks.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Versioned, forward-only schema migrator for the racks table.
 *
 * <p>{@link SqliteRackStorage#initSchema()} always runs {@code CREATE TABLE IF NOT EXISTS} first with
 * every column the current version needs, so a <b>fresh</b> install lands on the latest schema
 * directly. This migrator's job is an <b>existing</b> install: a database created by an older plugin
 * version whose table is missing a column added later. It brings such a database up to
 * {@link #CURRENT_VERSION} by running the {@link #STEPS} newer than the version recorded in
 * {@code <prefix>schema_meta}, then stamps the new version.
 *
 * <p><b>Safety.</b> Every step must be idempotent — an additive column step guards itself with
 * {@link #columnExists} before the {@code ALTER TABLE}. That makes it safe to re-run even when the
 * recorded version is missing or behind, for example on a fresh install (where the {@code CREATE}
 * already added the column) or after a previous run was interrupted before its version was stamped.
 * The recorded version is an optimisation and an audit trail, not the sole guard.
 */
final class SchemaMigrator {

    /** The schema version this build expects. Bump this and add a {@link Step} when the schema changes. */
    static final int CURRENT_VERSION = 1;

    private static final Logger log = LoggerFactory.getLogger("Racks");

    private static final String VERSION_KEY = "version";

    /**
     * One forward migration. {@link #apply} runs against an existing database whose schema is older
     * than {@link #version}; it must be idempotent (guard additive DDL with {@link #columnExists}).
     * Receives the racks table's actual (prefixed) name.
     */
    private interface Step {
        int version();
        void apply(Connection conn, String table) throws SQLException;
    }

    // Ordered list of every migration ever shipped. 1.0.0 shipped the schema below directly via
    // CREATE TABLE, so there is nothing to migrate yet — append new steps here as the schema grows.
    // Never edit or reorder a released step, only append.
    private static final List<Step> STEPS = List.of();

    private SchemaMigrator() {}

    /**
     * Brings {@code table}'s schema up to {@link #CURRENT_VERSION}, running only the steps newer than
     * the version recorded in {@code metaTable}, then stamps the version actually reached. Called once
     * from {@link SqliteRackStorage#initSchema()}, right after the base {@code CREATE TABLE}/index. A
     * failure is fatal (rethrown) — running on a half-migrated schema would be worse than not starting.
     */
    static void migrate(DataSource dataSource, String table, String metaTable) {
        try (Connection conn = dataSource.getConnection()) {
            ensureMetaTable(conn, metaTable);
            int from = readVersion(conn, metaTable);
            int version = from;
            for (Step step : STEPS) {
                if (step.version() > version) {
                    step.apply(conn, table);
                    version = step.version();
                    log.info("Applied Racks database schema migration to v{}.", version);
                }
            }
            if (version < CURRENT_VERSION) {
                // Nothing left to migrate for this build; just record where we've landed.
                version = CURRENT_VERSION;
            }
            if (version != from) {
                writeVersion(conn, metaTable, version);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to migrate the Racks database schema", e);
        }
    }

    private static void ensureMetaTable(Connection conn, String metaTable) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS " + metaTable + " (" +
                    "  meta_key   VARCHAR(64) NOT NULL," +
                    "  meta_value VARCHAR(64)," +
                    "  PRIMARY KEY (meta_key)" +
                    ")");
        }
    }

    /**
     * Reads the recorded schema version, or {@code 0} when the meta table has no version row — which
     * covers both a brand-new database and one written before this migrator existed.
     */
    private static int readVersion(Connection conn, String metaTable) throws SQLException {
        try (var ps = conn.prepareStatement(
                "SELECT meta_value FROM " + metaTable + " WHERE meta_key = ?")) {
            ps.setString(1, VERSION_KEY);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return 0;
                try {
                    return Integer.parseInt(rs.getString(1));
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
    }

    /** Upserts the recorded schema version (UPDATE-else-INSERT — SQLite has no bare upsert here). */
    private static void writeVersion(Connection conn, String metaTable, int version) throws SQLException {
        try (var ps = conn.prepareStatement(
                "UPDATE " + metaTable + " SET meta_value = ? WHERE meta_key = ?")) {
            ps.setString(1, Integer.toString(version));
            ps.setString(2, VERSION_KEY);
            if (ps.executeUpdate() > 0) return;
        }
        try (var ps = conn.prepareStatement(
                "INSERT INTO " + metaTable + " (meta_key, meta_value) VALUES (?, ?)")) {
            ps.setString(1, VERSION_KEY);
            ps.setString(2, Integer.toString(version));
            ps.executeUpdate();
        }
    }

    /**
     * Adds {@code column} to {@code table} with the given {@code columnDef} (type + constraints, e.g.
     * {@code "TEXT"}) only if it is not already present — safe on a fresh install (where the
     * {@code CREATE} already added it) and safe to re-run. For use by future {@link Step}s.
     */
    private static void addColumnIfMissing(Connection conn, String table, String column, String columnDef)
            throws SQLException {
        if (columnExists(conn, table, column)) {
            return;
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + columnDef);
        } catch (SQLException e) {
            // Backstop against under-reported metadata: tolerate a duplicate-column failure if the
            // column is in fact already there, rethrow anything else (a genuine schema problem).
            if (!columnExists(conn, table, column)) {
                throw e;
            }
        }
    }

    /** True if {@code table} has a column named {@code column} (case-insensitive). */
    private static boolean columnExists(Connection conn, String table, String column) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, table, null)) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
