package me.matl114.utils.world;

import java.util.Iterator;
import java.util.NoSuchElementException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.world.level.chunk.ChunkAccess;

public class ChunkIterator implements Iterator<ChunkAccess> {
    private static final Minecraft mc = Minecraft.getInstance();
    private final ClientChunkCache.Storage map = (mc.level.getChunkSource()).storage;
    private final boolean onlyWithLoadedNeighbours;

    private ChunkAccess chunk;
    private final int minX, maxX, maxZ;
    private int currentX, currentZ;

    public ChunkIterator(boolean onlyWithLoadedNeighbours) {
        this.onlyWithLoadedNeighbours = onlyWithLoadedNeighbours;
        int realRange = Math.min(map.chunkRadius, Math.max(2, mc.options.getEffectiveRenderDistance()) + 3);
        int centerX = map.viewCenterX;
        int centerZ = map.viewCenterZ;
        minX = centerX - realRange;
        maxX = centerX + realRange;
        int minZ = centerZ - realRange;
        maxZ = centerZ + realRange;
        currentX = minX;
        currentZ = minZ;
        getNext();
    }

    private ChunkAccess getNext() {
        ChunkAccess prev = chunk;
        chunk = null;
        search:
        while (currentZ <= maxZ) {
            while (currentX <= maxX) {
                int idx = map.getIndex(currentX++, currentZ);
                chunk = map.chunks.get(idx);
                if (chunk != null && (!onlyWithLoadedNeighbours || isInRadius(chunk))) break search;
            }
            currentZ++;
            currentX = minX;
        }

        return prev;
    }

    private boolean isInRadius(ChunkAccess chunk) {
        int x = chunk.getPos().x;
        int z = chunk.getPos().z;

        return mc.level.getChunkSource().hasChunk(x + 1, z)
                && mc.level.getChunkSource().hasChunk(x - 1, z)
                && mc.level.getChunkSource().hasChunk(x, z + 1)
                && mc.level.getChunkSource().hasChunk(x, z - 1);
    }

    @Override
    public boolean hasNext() {
        return chunk != null;
    }

    @Override
    public ChunkAccess next() {
        if (chunk == null) {
            throw new NoSuchElementException();
        }
        return getNext();
    }
}
