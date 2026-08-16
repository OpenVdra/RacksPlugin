# storage/

The persistence layer: an in-memory index of every placed rack, a single-threaded write-behind writer
in front of SQLite, and the schema's version migrator. Callers only ever see `RackRepository`; nothing
outside this package holds a `SqliteRackStorage` reference.

## Files

| File | Responsibility |
|---|---|
| `RackRepository` | The in-memory index everyone reads through, plus the write-behind queue |
| `SqliteRackStorage` | The SQL layer: schema init, `loadAll`, per-row insert/update/delete |
| `RackRow` | Immutable, Bukkit-free snapshot of one rack — the hand-off type to the writer thread |
| `SchemaMigrator` | Versioned, forward-only column steps for existing databases; package-private, called from `SqliteRackStorage` |
| `ChunkPos` | Bucket key (world, chunk x, chunk z) used to index racks per chunk |

## Reads never touch the database

Racks are world objects, not per-player data — there are at most a few thousand on a server and they
are all loaded once, in `RackRepository.loadAll()`, before any listener is registered. So `get(RackKey)`,
called on every interaction with any rack, is a single `ConcurrentHashMap` lookup: no I/O, no lock, no
allocation. There is no lazy per-owner loading here the way per-player plugins need it — the whole
dataset already fits comfortably in memory.

## Writes go straight to the writer queue — there is no dirty-tracking cache, and no autosave interval

This is a deliberate difference from plugins that cache per-player data (e.g. an ender chest plugin
caching each online player's inventory): those buffer changes in memory as "dirty" and flush them on a
timer, because a busy player can mutate their data many times a second and batching keeps that off the
database. Racks don't have that problem — placing, breaking, swapping an item or turning a rack is a
comparatively rare, deliberate player action, not a per-tick stream of writes. So there is nothing worth
batching:

- `RackRepository.add/save/remove` snapshot the change into an immutable `RackRow` **on the calling
  thread** (the thread that owns the rack's block — the only one allowed to touch its `ItemStack`s), then
  hand the row to a single-threaded `ExecutorService` (`submit(...)`), which is the actual write-behind:
  the SQL statement runs off the calling thread, so a slow disk never blocks gameplay, but it runs
  *now*, not at the next tick of some timer.
- Because the writer is a single thread and its queue is FIFO, and a given rack is only ever mutated
  from its own region thread, writes for one rack reach SQLite in exactly the order they happened — the
  last one wins and it is the right one. See `RackRepository`'s class javadoc ("Why this cannot
  duplicate items") for the full argument.
- `RackRepository.flush()` (called on disable) just drains this queue — it is not a periodic flush of
  buffered dirty state, since none exists. There is consequently **no `database.autosave-interval`-style
  config**: there is no interval to configure, because nothing waits.
- The worst case of a hard crash is losing whichever single write was in flight, not a whole autosave
  window's worth of changes.

## Schema: `<prefix>racks` (default `racks`)

One row per placed rack. `UNIQUE(world, x, y, z)` is the last line of defence against two racks ever
being recorded for one block — insert uses `ON CONFLICT ... DO UPDATE` so a re-placement overwrites
rather than throws. `update` only touches what an interaction can change: `pose`, `item_left`,
`item_right`.

Items are stored as serialized bytes (`ItemCodec`), decoded on load; a row whose items fail to decode
is logged and skipped rather than loaded empty, so a later plugin update can still recover it (see
`RackRepository.toRack`).

## Schema versioning

`SqliteRackStorage.initSchema()` runs `CREATE TABLE IF NOT EXISTS` first — a **fresh** install always
gets every current column directly from that statement. `SchemaMigrator.migrate()` runs right after,
against an **existing** database: it brings a table created by an older plugin version up to
`SchemaMigrator.CURRENT_VERSION` by running only the migration steps newer than the version recorded in
`<prefix>schema_meta`, then stamps the version reached.

**To add a column:** bump `SchemaMigrator.CURRENT_VERSION`, append a new `Step` to `STEPS` (guard the
`ALTER TABLE` with `columnExists`/`addColumnIfMissing` so it is safe to re-run), and add the column to
the `CREATE TABLE` in `SqliteRackStorage.initSchema()` so a fresh install gets it too. Never edit or
reorder a released step — only append.

## Chunk-scoped lookups

`RackRepository` buckets racks by `ChunkPos` for two reasons: `RackChunkListener` needs "which racks
are in this chunk" when its entities load, and the wall-support sweep needs "which chunks currently
hold a wall rack" without walking the whole index. Wall racks are kept in their own map alongside the
general one, not filtered out of it on demand, because the sweep asks its question every few ticks per
chunk and a chunk holding only ground racks then needs no sweep task scheduled at all.
