package me.matl114.mixins.access;

import me.matl114.accessors.access.PlayerInteractItemC2SPacketAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(ServerboundUseItemPacket.class)
public abstract class PlayerInteractItemC2SPacketMixin implements PlayerInteractItemC2SPacketAccess {

    @Override
    @Mutable
    @Accessor("hand")
    public abstract void setHand(InteractionHand hand);

    @Override
    @Mutable
    @Accessor("yRot")
    public abstract void setYaw(float yaw);

    @Override
    @Mutable
    @Accessor("xRot")
    public abstract void setPitch(float pitch);

    @Inject(method = "<init>(Lnet/minecraft/world/InteractionHand;IFF)V", at = @At("RETURN"))
    private void trackUseContext(InteractionHand hand, int sequence, float yaw, float pitch, CallbackInfo ci) {
        if (Minecraft.getInstance().player != null) {
            useContext =
                    Minecraft.getInstance().player.getItemInHand(hand).copy();
        }
    }

    @Unique
    ItemStack useContext;

    public void setItemStack(ItemStack stack) {
        useContext = stack;
    }

    public ItemStack getItemStack() {
        return useContext;
    }
}
