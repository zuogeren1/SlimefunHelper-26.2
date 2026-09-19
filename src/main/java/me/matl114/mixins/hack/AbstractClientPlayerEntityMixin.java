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
            // 原值 0.17F 高于真实速度（速度 II 约 0.14），等于压根没 clamp；
            // 压到基准步行速度 0.1 才能真正消掉加速带来的 FOV 拉伸
            original = Math.min(original, 0.1F);
        }
        return original;
    }

    @ModifyExpressionValue(
            method = "getFieldOfViewModifier",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;isUsingItem()Z"))
    private boolean onNoUseItemFov(boolean original) {
        if (NoRender.INSTANCE.noUseItemFov()) {
            return false;
        }
        return original;
    }
}
