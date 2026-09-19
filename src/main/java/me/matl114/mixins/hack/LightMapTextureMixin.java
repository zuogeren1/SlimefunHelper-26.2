package me.matl114.mixins.hack;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.matl114.hacks.modules.render.RenderExtra;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(LightmapRenderStateExtractor.class)
public abstract class LightMapTextureMixin {
    // 26.2 起 LightTexture 被 Lightmap 取代（改用 GpuTexture / RenderPass 直接绘制），
    // 光照状态提取移到 LightmapRenderStateExtractor.extract，夜视判断逻辑位于该方法内：
    //   if (player.hasEffect(MobEffects.NIGHT_VISION)) { nightVisionEffectIntensity = ... }
    @ModifyExpressionValue(
            method = "extract",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/core/Holder;)Z",
                            ordinal = 0))
    public boolean alwaysNightVision(boolean original) {
        if (RenderExtra.INSTANCE.nightVision.get()) {
            return true;
        }
        return original;
    }
}
