package fi.dy.masa.litematica.world;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.Holder;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

public abstract class WorldSchematic extends Level {
    protected WorldSchematic(
            WritableLevelData properties,
            ResourceKey<Level> registryRef,
            RegistryAccess registryManager,
            Holder<DimensionType> dimensionEntry,
            boolean isClient,
            boolean debugWorld,
            long seed,
            int maxChainedNeighborUpdates) {
        super(
                properties,
                registryRef,
                registryManager,
                dimensionEntry,
                isClient,
                debugWorld,
                seed,
                maxChainedNeighborUpdates);
    }
}
