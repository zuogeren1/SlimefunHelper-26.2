package me.matl114.mixins.hack;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.matl114.hacks.modules.render.NoRender;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerEntityMixin {
    @ModifyExpressionValue(
            method = "getFieldOfViewModifier",
            at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Abilities;flying:Z"))
    private boolean onNoFlyFov(boolean original) {
        if (NoRender.INSTANCE.noFlyFov()) {
            return false;
        }
        return original;
    }

    @ModifyExpressionValue(
            method = "getFieldOfViewModifier",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/player/AbstractClientPlayer;getAttributeValue(Lnet/minecraft/core/Holder;)D"))
    private double onNoSpeedFov(double original) {
        if (NoRender.INSTANCE.noSlowDownFov()) {
            original = Math.max(original, 0.1F);
        }
        if (NoRender.INSTANCE.noSpeedFov()) {
            original = Math.min(original, 0.17F);
        }
        return original;
    }

    @ModifyExpressionValue(
            method = "getFieldOfViewModifier",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/player/AbstractClientPlayer;isUsingItem()Z"))
    private boolean onNoUseItemFov(boolean original) {
        if (NoRender.INSTANCE.noUseItemFov()) {
            return false;
        }
        return original;
    }
}
