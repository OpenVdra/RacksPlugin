package com.racks.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * SQLite persistence for placed racks — the plugin's replacement for the data pack's
 * {@code pk:racks database.blocks.racks} NBT list.
 *
 * <p>Every method here blocks on I/O and must be called from {@link RackRepository}'s writer thread,
 * never from a server thread.
 *
 * <h2>Why the position is unique</h2>
 * A block holds at most one rack, so {@code UNIQUE(world, x, y, z)} is not decoration: it is the last
 * line of defence against two racks ever being recorded for one spot, which is the shape a
 * duplication bug would take if a placement were somehow processed twice. Insert uses
 * {@code ON CONFLICT ... DO UPDATE} so a re-placement overwrites rather than throws.
 */
public final class SqliteRackStorage implements AutoCloseable {

    private final HikariDataSource dataSource;
    private final String table;

    private final String insertSql;
    private final String updateSql;
    private final String deleteSql;
    private final String selectAllSql;

    public SqliteRackStorage(Path dataFolder, String fileName, String tablePrefix) {
        this.table = tablePrefix + "racks";
        this.dataSource = new HikariDataSource(buildConfig(dataFolder, fileName));

        this.insertSql = """
                INSERT INTO %1$s (id, world, x, y, z, variant, wall, facing, pose, owner, created_at, item_left, item_right)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
                ON CONFLICT (world, x, y, z) DO UPDATE SET
                    id = excluded.id, variant = excluded.variant, wall = excluded.wall,
                    facing = excluded.facing, pose = excluded.pose, owner = excluded.owner,
                    created_at = excluded.created_at,
                    item_left = excluded.item_left, item_right = excluded.item_right
                """.formatted(table);
        this.updateSql = "UPDATE %1$s SET pose = ?, item_left = ?, item_right = ? WHERE id = ?".formatted(table);
        this.deleteSql = "DELETE FROM %1$s WHERE id = ?".formatted(table);
        this.selectAllSql = """
                SELECT id, world, x, y, z, variant, wall, facing, pose, owner, created_at, item_left, item_right
                FROM %1$s
                """.formatted(table);

        initSchema();
    }

    private void initSchema() {
        String createTable = """
                CREATE TABLE IF NOT EXISTS %1$s (
                    id          INTEGER NOT NULL PRIMARY KEY,
                    world       TEXT    NOT NULL,
                    x           INTEGER NOT NULL,
                    y           INTEGER NOT NULL,
                    z           INTEGER NOT NULL,
                    variant     TEXT    NOT NULL,
                    wall        INTEGER NOT NULL DEFAULT 0,
                    facing      TEXT    NOT NULL DEFAULT 'north',
                    pose        INTEGER NOT NULL DEFAULT 0,
                    owner       TEXT,
                    created_at  INTEGER NOT NULL DEFAULT 0,
                    item_left   BLOB,
                    item_right  BLOB
                )
                """.formatted(table);
        String createIndex =
                "CREATE UNIQUE INDEX IF NOT EXISTS %1$s_position ON %1$s (world, x, y, z)".formatted(table);

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTable);
            stmt.execute(createIndex);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not initialise the Racks database", e);
        }
    }

    /** Every stored rack. Called once on enable, before any listener is registered. */
    public List<RackRow> loadAll() throws SQLException {
        List<RackRow> rows = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(selectAllSql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                rows.add(read(rs));
            }
        }
        return rows;
    }

    /** Highest id in use, or 0 when the table is empty. Seeds the id counter. */
    public int maxId() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT COALESCE(MAX(id), 0) FROM " + table);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public void insert(RackRow row) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            stmt.setInt(1, row.id());
            stmt.setString(2, row.world().toString());
            stmt.setInt(3, row.x());
            stmt.setInt(4, row.y());
            stmt.setInt(5, row.z());
            stmt.setString(6, row.variant());
            stmt.setInt(7, row.wall() ? 1 : 0);
            stmt.setString(8, row.facing());
            stmt.setInt(9, row.pose());
            stmt.setString(10, row.owner() == null ? null : row.owner().toString());
            stmt.setLong(11, row.createdAt());
            stmt.setBytes(12, row.left());
            stmt.setBytes(13, row.right());
            stmt.executeUpdate();
        }
    }

    /** Writes back only what an interaction can change: the pose and the two slots. */
    public void update(RackRow row) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateSql)) {
            stmt.setInt(1, row.pose());
            stmt.setBytes(2, row.left());
            stmt.setBytes(3, row.right());
            stmt.setInt(4, row.id());
            stmt.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    private static RackRow read(ResultSet rs) throws SQLException {
        return new RackRow(
                rs.getInt("id"),
                UUID.fromString(rs.getString("world")),
                rs.getInt("x"), rs.getInt("y"), rs.getInt("z"),
                rs.getString("variant"),
                rs.getInt("wall") != 0,
                rs.getString("facing"),
                rs.getShort("pose"),
                parseUuid(rs.getString("owner")),
                rs.getLong("created_at"),
                rs.getBytes("item_left"),
                rs.getBytes("item_right"));
    }

    private static @Nullable UUID parseUuid(@Nullable String raw) {
        if (raw == null || raw.isEmpty()) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public void close() {
        dataSource.close();
    }

    private static HikariConfig buildConfig(Path dataFolder, String fileName) {
        Path dbFile = dataFolder.resolve(fileName).toAbsolutePath();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile);
        config.setDriverClassName("org.sqlite.JDBC");

        // SQLite is a single-writer file; one connection is both sufficient and correct. More would
        // only contend on the file lock and produce SQLITE_BUSY.
        config.setMaximumPoolSize(1);
        config.setMinimumIdle(1);

        // WAL keeps readers un-blocked by the writer and turns most commits into sequential appends;
        // synchronous=NORMAL is the recommended pairing (fsync at checkpoint rather than on every
        // commit — a power cut can cost the last moments of play, never the database). Both are
        // passed as driver properties, which the Paper-bundled Xerial driver applies as PRAGMAs.
        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("synchronous", "NORMAL");

        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("Racks-SQLite");
        config.setConnectionTimeout(30_000);
        config.setIdleTimeout(0);
        config.setMaxLifetime(0);

        return config;
    }
}
