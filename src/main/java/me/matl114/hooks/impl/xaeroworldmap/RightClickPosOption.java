package me.matl114.hooks.impl.xaeroworldmap;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

public class RightClickPosOption extends RightClickOption {
    MapClickContext context;
    BlockPos pos;
    ResourceKey<Level> world;

    public RightClickPosOption(
            MapClickContext context,
            ResourceKey<Level> currentWorld,
            BlockPos pos,
            int index,
            IRightClickableElement target) {
        super(
                context.formatter.format(
                        "%d %d %d".formatted(pos.getX(), pos.getY(), pos.getZ()),
                        String.valueOf(pos.getX()),
                        String.valueOf(pos.getY()),
                        String.valueOf(pos.getZ())),
                index,
                target);
        this.pos = pos;
        this.world = currentWorld;
        ;
        this.context = context;
    }

    @Override
    public void onAction(Screen var1) {
        context.function.accept(world, pos);
    }
}
