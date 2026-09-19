package me.matl114.accessors.access;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface ChunkAccess {
    public Iterable<Map.Entry<BlockPos, BlockEntity>> blockEntityEntries();

    public static ChunkAccess of(net.minecraft.world.level.chunk.ChunkAccess chunk) {
        return (ChunkAccess) chunk;
    }
}
