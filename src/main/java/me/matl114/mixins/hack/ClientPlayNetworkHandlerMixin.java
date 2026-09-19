package me.matl114.mixins.hack;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Set;
import me.matl114.accessors.access.ClientPlayerAccess;
import me.matl114.hacks.modules.extra.BadPacketsFix;
import me.matl114.hacks.modules.move.AutoResync;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Environment(EnvType.CLIENT)
@Mixin(ClientPacketListener.class)
public abstract class ClientPlayNetworkHandlerMixin {

    @Inject(
            method = "handleContainerClose",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/player/LocalPlayer;clientSideCloseContainer()V",
                            shift = At.Shift.BEFORE),
            cancellable = true)
    private void onCloseScreenClearKeepedInv(ClientboundContainerClosePacket packet, CallbackInfo ci) {
        ClientPlayerAccess access = ClientPlayerAccess.of(Minecraft.getInstance().player);
        if (access.getKeepedInvHandler() != null && access.getKeepedInvHandler().containerId == packet.getContainerId()) {
            access.clearKeepedInventory(true);
        }
        if (Minecraft.getInstance().player.containerMenu.containerId != packet.getContainerId()) {
            ci.cancel();
        }
    }

    @Inject(method = "handleContainerSetSlot", at = @At(value = "RETURN"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void onScreenHandlerSlotUpdateSyncToKeeped(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        // Debug.info("Received screen handler slot update packet
        // ",packet.getContainerId(),packet.getSlot(),packet.getItemStack());
        if (Minecraft.getInstance().player != null) {
            ClientPlayerAccess access = ClientPlayerAccess.of(Minecraft.getInstance().player);
            if (access.getKeepedInvHandler() != null && packet.getContainerId() == access.getKeepedInvHandler().containerId) {
                access.getKeepedInvHandler().setItem(packet.getSlot(), packet.getStateId(), packet.getItem());
            }
        }
    }

    @Inject(method = "handleContainerContent", at = @At(value = "RETURN"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void onInventorySyncToKeeped(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        if (Minecraft.getInstance().player != null) {
            ClientPlayerAccess access = ClientPlayerAccess.of(Minecraft.getInstance().player);
            if (access.getKeepedInvHandler() != null && packet.containerId() == access.getKeepedInvHandler().containerId) {
                access.getKeepedInvHandler()
                        .initializeContents(packet.stateId(), packet.items(), packet.carriedItem());
            }
        }
    }

    @Inject(
            method = "handleOpenScreen",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/gui/screens/MenuScreens;create(Lnet/minecraft/world/inventory/MenuType;Lnet/minecraft/client/Minecraft;ILnet/minecraft/network/chat/Component;)V",
                            shift = At.Shift.BEFORE))
    private void onInventoryOpenCloseKeepInventory(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
        // for keepInv
        if (Minecraft.getInstance().player != null) {
            ClientPlayerAccess access = ClientPlayerAccess.of(Minecraft.getInstance().player);
            access.clearKeepedInventory(false);
        }
    }

    @Inject(
            method = "handlePlayerInfoUpdate",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V",
                            shift = At.Shift.BEFORE,
                            remap = false),
            cancellable = true)
    public void onInvalidPlayerEntry(ClientboundPlayerInfoUpdatePacket packet, CallbackInfo ci) {
        if (BadPacketsFix.INSTANCE.fixInvalidPlayerEntryUpdate.get()) {
            ci.cancel();
        }
    }

    @WrapOperation(
            method = "handleMovePlayer",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/multiplayer/ClientPacketListener;setValuesFromPositionPacket(Lnet/minecraft/world/entity/PositionMoveRotation;Ljava/util/Set;Lnet/minecraft/world/entity/Entity;Z)Z"))
    private boolean wrapSetPositionLook(
            PositionMoveRotation pos, Set<Relative> flags, Entity entity, boolean bl, Operation<Boolean> original) {
        if (AutoResync.INSTANCE.noVelocitySetback.get()) {
            Vec3 currentVelocity = entity.getDeltaMovement();
            boolean re = original.call(pos, flags, entity, bl);
            entity.setDeltaMovement(currentVelocity);
            return re;
        } else {
            return original.call(pos, flags, entity, bl);
        }
    }
}
