package me.matl114.mixins.fix;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.FriendlyByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Environment(EnvType.CLIENT)
@Mixin(FriendlyByteBuf.class)
public abstract class PacketByteBufFixMixin {
    @ModifyVariable(
            method = "writeUtf(Ljava/lang/String;I)Lnet/minecraft/network/FriendlyByteBuf;",
            at = @At(value = "HEAD"),
            argsOnly = true,
            index = 2)
    public int modifyPacketStringLength(int var) {
        return 262144;
    }
}
