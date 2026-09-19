package me.matl114.accessors.moonrise;

import net.minecraft.world.level.chunk.LevelChunkSection;

public interface MoonriseChunkBlockCountingAccess {
    int getSpecialCollidingBlockCount();

    static MoonriseChunkBlockCountingAccess of(LevelChunkSection chunk) {
        return (MoonriseChunkBlockCountingAccess) chunk;
    }
}
