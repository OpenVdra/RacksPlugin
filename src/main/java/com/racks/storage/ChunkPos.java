package com.racks.storage;

import com.racks.model.RackKey;

import java.util.UUID;

/** A chunk in a specific world. Bucket key for the per-chunk wall-support sweep. */
public record ChunkPos(UUID world, int x, int z) {

    public static ChunkPos of(RackKey key) {
        return new ChunkPos(key.world(), key.chunkX(), key.chunkZ());
    }
}
