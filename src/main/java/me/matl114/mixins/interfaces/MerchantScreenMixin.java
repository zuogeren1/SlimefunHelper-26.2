package me.matl114.mixins.interfaces;

import javax.annotation.Nullable;
import me.matl114.accessors.interfaces.EntityInventory;
import me.matl114.hacks.InteractionTasks;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends AbstractContainerScreen<MerchantMenu>
        implements EntityInventory<Villager> {
    @Unique
    Villager owner;

    public MerchantScreenMixin(MerchantMenu handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Nullable
    @Override
    @Unique
    public Villager getOwner() {
        return owner;
    }

    @Inject(
            method = "<init>",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;<init>(Lnet/minecraft/world/inventory/AbstractContainerMenu;Lnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/network/chat/Component;II)V",
                            shift = At.Shift.AFTER))
    private void onInit(MerchantMenu handler, Inventory inventory, Component title, CallbackInfo ci) {
        owner = (Villager) InteractionTasks.predictScreenFrom(e -> e instanceof Villager);
        if (this.menu instanceof EntityInventory.Handler handler1) {
            handler1.sync(this);
        }
    }
}
