package me.matl114.accessors.interfaces;

import me.matl114.utils.world.ContainerPosition;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

public interface TileInventory {
    @Nullable
    public BlockPos getPos();

    @Nullable
    public ClientLevel getWorld();

    @Nullable
    public Block getBlockType();

    @Nullable
    public ContainerPosition getContainerPosition();

    @Nullable
    default boolean isVirtual() {
        return getContainerPosition() == null;
    }

    static TileInventory of(AbstractContainerScreen<?> handledScreen) {
        return (TileInventory) handledScreen;
    }

    public AbstractContainerScreen<?> castHandled();

    default AbstractContainerMenu castHandler() {
        return castHandled().getMenu();
    }

    public static interface Handler extends TileInventory {
        default AbstractContainerScreen<?> castHandled() {
            throw new UnsupportedOperationException();
        }

        default AbstractContainerMenu castHandler() {
            return (AbstractContainerMenu) this;
        }

        public void sync(TileInventory tileInventory);
    }
}
