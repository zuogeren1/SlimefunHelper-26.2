package me.matl114.hooks.impl.xaeroworldmap;

import java.util.List;
import java.util.function.BiConsumer;
import me.matl114.hacks.utils.config.StringFormat;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class MapClickContext {
    StringFormat formatter;
    BiConsumer<ResourceKey<Level>, BlockPos> function;

    public MapClickContext(String format, BiConsumer<ResourceKey<Level>, BlockPos> function) {
        this.formatter = new StringFormat(List.of("pos", "x", "y", "z"), format);
        this.function = function;
    }
}
