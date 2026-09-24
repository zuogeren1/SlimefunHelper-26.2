package me.matl114.mixins.interfaces;

import me.matl114.accessors.interfaces.EntityInventory;
import me.matl114.accessors.interfaces.TileInventory;
import me.matl114.utils.world.ContainerPosition;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Environment(EnvType.CLIENT)
@Mixin(ChestMenu.class)
public abstract class ChestScreenHandlerMixin extends AbstractContainerMenu
        implements TileInventory.Handler, EntityInventory.Handler<ContainerEntity> {
    @Unique
    private BlockPos pos;

    protected ChestScreenHandlerMixin(MenuType<?> type, int syncId) {
        super(type, syncId);
    }

    @Unique
    public BlockPos getPos() {
        return this.pos;
    }

    @Unique
    private Block cacheBlockType;

    @Unique
    public Block getBlockType() {
        return cacheBlockType;
    }

    @Unique
    public ClientLevel getWorld() {
        return this.world;
    }

    @Unique
    private ClientLevel world;

    @Unique
    private ContainerPosition containerPosition;

    @Unique
    public ContainerPosition getContainerPosition() {
        return this.containerPosition;
    }

    @Unique
    ContainerEntity vehicleEntity;

    @Nullable
    @Override
    public ContainerEntity getOwner() {
        return vehicleEntity;
    }

    @Override
    public void sync(EntityInventory<ContainerEntity> inventory) {
        this.vehicleEntity = inventory.getOwner();
    }

    @Override
    public AbstractContainerScreen<?> castHandled() {
        throw new UnsupportedOperationException();
    }

    @Override
    public AbstractContainerMenu castHandler() {
        return this;
    }

    @Override
    public void sync(TileInventory tileInventory) {
        this.world = tileInventory.getWorld();
        this.containerPosition = tileInventory.getContainerPosition();
        this.pos = this.containerPosition == null
                ? null
                : this.containerPosition.getFirst().getPos();
        if (this.pos != null)
            this.cacheBlockType = this.world.getBlockState(this.pos).getBlock();
    }
}
