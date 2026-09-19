package me.matl114.mixins.fix;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Environment(EnvType.CLIENT)
@Mixin(ServerboundHelloPacket.class)
public abstract class LoginHelloPacketNameFixMixin {
    // 运行逻辑
    // client->server helloPacket
    // server->handleHello->client helloPacket
    // server->LoginHandler.fireEvent->readyToAcceptPlayer
    // server->tick->
    @ModifyArg(
            method = "write",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/network/FriendlyByteBuf;writeUtf(Ljava/lang/String;I)Lnet/minecraft/network/FriendlyByteBuf;"),
            index = 0)
    private String onChangeNameSend(String name) {
        //        byte[] nameBytes = new byte[]{33,34,35,36};
        //        return new String(nameBytes, StandardCharsets.UTF_8);//name.substring(0,Math.min(name.length(),16));
        return name == null ? "null" : name.substring(0, Math.min(16, name.length()));
    }
}
