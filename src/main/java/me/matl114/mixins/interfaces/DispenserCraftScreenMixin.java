package me.matl114.mixins.interfaces;

import me.matl114.accessors.interfaces.TileInventory;
import me.matl114.hacks.InvTasks;
import me.matl114.utils.world.ContainerPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.DispenserScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DispenserScreen.class)
public abstract class DispenserCraftScreenMixin extends AbstractContainerScreen<DispenserMenu>
        implements TileInventory {

    @Shadow
    protected abstract void init();

    @Unique
    private BlockPos pos;

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
    private ClientLevel world;

    @Unique
    public ClientLevel getWorld() {
        return this.world;
    }

    @Unique
    private ContainerPosition containerPosition;

    @Unique
    public ContainerPosition getContainerPosition() {
        return this.containerPosition;
    }

    @Unique
    public AbstractContainerScreen<?> castHandled() {
        return this;
    }

    public DispenserCraftScreenMixin(AbstractContainerMenu handler, Inventory inventory, Component title) {
        super((DispenserMenu) handler, inventory, title);
    }

    @Inject(
            method = "<init>",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;<init>(Lnet/minecraft/world/inventory/AbstractContainerMenu;Lnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/network/chat/Component;)V",
                            shift = At.Shift.AFTER))
    protected void tryInitBlockPos(DispenserMenu handler, Inventory inventory, Component title, CallbackInfo ci) {
        this.world = Minecraft.getInstance().level;
        this.pos = InvTasks.predictScreenFrom((b) -> b == Blocks.DISPENSER || b == Blocks.DROPPER);
        if (this.pos != null && this.world != null) {
            cacheBlockType = this.world.getBlockState(this.pos).getBlock();
            containerPosition = ContainerPosition.ofSingle(world, pos);
        }
        if (this.menu instanceof TileInventory.Handler handler1) {
            handler1.sync(this);
        }
    }
}
