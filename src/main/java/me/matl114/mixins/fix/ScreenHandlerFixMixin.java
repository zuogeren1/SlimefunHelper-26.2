package me.matl114.mixins.fix;

import me.matl114.hacks.InvTasks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(AbstractContainerMenu.class)
public abstract class ScreenHandlerFixMixin {

    @Inject(
            method = "clicked",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/inventory/AbstractContainerMenu;doClick(IILnet/minecraft/world/inventory/ContainerInput;Lnet/minecraft/world/entity/player/Player;)V",
                            shift = At.Shift.BEFORE))
    private void onPreSlotClick(
            int slotIndex, int button, ContainerInput actionType, Player player, CallbackInfo ci) {
        if (Minecraft.getInstance().level != null
                && Minecraft.getInstance().level.isClientSide()) {
            InvTasks.SUPPRESS_DROPITEM_SPAWN.set(true);
        }
    }

    @Inject(
            method = "clicked",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/inventory/AbstractContainerMenu;doClick(IILnet/minecraft/world/inventory/ContainerInput;Lnet/minecraft/world/entity/player/Player;)V",
                            shift = At.Shift.AFTER))
    private void onPostSlotClick(
            int slotIndex, int button, ContainerInput actionType, Player player, CallbackInfo ci) {
        InvTasks.SUPPRESS_DROPITEM_SPAWN.set(false);
    }

    @Inject(
            method = "clicked",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/CrashReport;forThrowable(Ljava/lang/Throwable;Ljava/lang/String;)Lnet/minecraft/CrashReport;",
                            shift = At.Shift.BEFORE))
    private void onExceptionHandlePostSlotClick(
            int slotIndex, int button, ContainerInput actionType, Player player, CallbackInfo ci) {
        InvTasks.SUPPRESS_DROPITEM_SPAWN.set(false);
    }
}
