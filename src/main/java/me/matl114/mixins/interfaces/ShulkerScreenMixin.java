package me.matl114.mixins.interfaces;

import me.matl114.accessors.interfaces.TileInventory;
import me.matl114.hacks.InteractionTasks;
import me.matl114.hacks.InvTasks;
import me.matl114.utils.world.ContainerPosition;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(ShulkerBoxScreen.class)
public abstract class ShulkerScreenMixin extends AbstractContainerScreen<ShulkerBoxMenu> implements TileInventory {
    @Unique
    private BlockPos pos;

    public ShulkerScreenMixin(ShulkerBoxMenu handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
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
    public AbstractContainerScreen<?> castHandled() {
        return this;
    }

    @Unique
    private ClientLevel world;

    @Unique
    private ContainerPosition containerPosition;

    @Unique
    public ContainerPosition getContainerPosition() {
        return this.containerPosition;
    }

    @Inject(
            method = "<init>",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;<init>(Lnet/minecraft/world/inventory/AbstractContainerMenu;Lnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/network/chat/Component;II)V",
                            shift = At.Shift.AFTER))
    private void tryInitBlockPos(ShulkerBoxMenu handler, Inventory inventory, Component title, CallbackInfo ci) {
        this.world = Minecraft.getInstance().level;
        // everything
        this.pos = InteractionTasks.predictBlockScreenFrom((b) -> b instanceof ShulkerBoxBlock);
        if (this.pos != null && this.world != null) {
            var state = this.world.getBlockState(this.pos);
            cacheBlockType = state.getBlock();
            if (this.cacheBlockType instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                this.containerPosition = ContainerPosition.resolveDoubleChest(world, pos, state);
            } else {
                this.containerPosition = ContainerPosition.ofSingle(world, pos);
            }
        }
        if (this.menu instanceof TileInventory.Handler handler1) {
            handler1.sync(this);
        }
    }
}
