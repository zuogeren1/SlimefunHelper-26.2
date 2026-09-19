package me.matl114.hooks.impl.xaeroplus.wrapper;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

@FunctionalInterface
public interface ElementSupplier<T> {
    public T supplyElement(
            final int windowRegionX,
            final int windowRegionZ,
            final int windowRegionSize,
            final ResourceKey<Level> dimension);
}
