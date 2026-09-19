package me.matl114.mixins.events;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.matl114.events.Listener;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketProcessor;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(PacketProcessor.ListenerAndPacket.class)
public abstract class NetworkThreadUtilsEvents {
    @WrapOperation(
            method = "handle",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/network/protocol/Packet;handle(Lnet/minecraft/network/PacketListener;)V"))
    private void wrapPacketHandle(Packet instance, PacketListener t, Operation<Void> original) {
        // do not handle serverbound packet
        if (t.flow() == PacketFlow.SERVERBOUND) {
            original.call(instance, t);
            return;
        }
        Listener.callPacketHandleEvent(instance, t, original::call);
    }
}
