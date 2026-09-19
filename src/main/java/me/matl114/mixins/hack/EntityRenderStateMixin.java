package me.matl114.mixins.hack;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import me.matl114.hacks.modules.move.ElytraExtra;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(HumanoidMobRenderer.class)
public abstract class EntityRenderStateMixin {
    @ModifyExpressionValue(
            method = "extractHumanoidRenderState",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isFallFlying()Z"))
    private static boolean updateBipedRenderState(boolean original, @Local(argsOnly = true) LivingEntity livingEntity) {
        if (livingEntity.isFallFlying() && livingEntity == Minecraft.getInstance().player) {
            if (ElytraExtra.INSTANCE.renderFix.get() && ElytraExtra.INSTANCE.isCurrentArmorGliding()) {
                return false;
            }
        }
        return original;
    }
}
