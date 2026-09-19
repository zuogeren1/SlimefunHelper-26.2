package me.matl114.mixins.events;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import me.matl114.events.Event;
import me.matl114.events.Listener;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(ConnectScreen.class)
public abstract class ConnectScreenEvents {
    @Inject(
            method =
                    "startConnecting(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/multiplayer/resolver/ServerAddress;Lnet/minecraft/client/multiplayer/ServerData;ZLnet/minecraft/client/multiplayer/TransferState;)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/gui/screens/ConnectScreen;<init>(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/network/chat/Component;)V",
                            shift = At.Shift.BEFORE),
            cancellable = true)
    private static void onPreConnect(
            Screen screen,
            Minecraft client,
            ServerAddress address,
            ServerData info,
            boolean quickPlay,
            TransferState cookieStorage,
            CallbackInfo ci,
            @Local(argsOnly = true) LocalRef<ServerAddress> infoLocalRef) {
        Event<ServerAddress> infoEvent = new Event<>(address, true, true, info);
        Listener.getServerPreConnectPoint().handleValue(infoEvent);
        if (infoEvent.isCancelled()) {
            ci.cancel();
        }
        if (infoEvent.context != address) {
            infoLocalRef.set(infoEvent.context);
        }
    }
}
