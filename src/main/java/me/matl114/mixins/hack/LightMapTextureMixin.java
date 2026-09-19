package me.matl114.mixins.hack;

import me.matl114.hacks.modules.render.NoRender;
import me.matl114.hacks.modules.render.RenderExtra;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 「始终夜视」与「屏蔽黑暗」。
 *
 * <p>26.2 起 LightTexture 被 Lightmap 取代，光照状态提取在
 * {@link LightmapRenderStateExtractor#extract}。
 *
 * <p><b>始终夜视</b>：26.2 由 {@code LightmapRenderState.nightVisionEffectIntensity} 控制
 * （原版：玩家有夜视效果时 = {@code GameRenderer.nightVisionScale(...)}，否则恒为 0.0F）。
 * 26.2 的 lightmap.fsh 是 {@code color = max(AmbientColor, NightVisionColor * NightVisionFactor)}，
 * 设成 1.0F 即得到与 1.21.11 相当的"整屏提亮"。
 *
 * <p><b>注意不要去改 {@code blockFactor} / {@code skyFactor}</b>：
 * 原版 {@code blockFactor} 约 1.4（blockLightFlicker + 1.4），压成 1.0 会让中等方块光照区变暗；
 * {@code skyFactor} 覆盖成 1.0 在主世界基本是空操作，还会抹掉维度/雷暴/末地闪光的天空光变化。
 * 早期版本就是误改了这两个字段，导致"夜视没生效 + 反而变暗"，现已修正。
 *
 * <p><b>屏蔽黑暗</b>：{@code darknessEffectScale} 由「选项 darknessEffectScale × DARKNESS 效果强度」
 * 算出，归零即可去掉黑暗的压暗。失明(黑暗之外)由 {@code CameraEvents} 处理。
 */
@Environment(EnvType.CLIENT)
@Mixin(LightmapRenderStateExtractor.class)
public abstract class LightMapTextureMixin {

    @Inject(method = "extract", at = @At("RETURN"))
    private void forceNightVisionGround(LightmapRenderState state, float partialTick, CallbackInfo ci) {
        if (RenderExtra.INSTANCE.nightVision.get()) {
            // 26.2 控制夜视的是 nightVisionEffectIntensity（由 GameRenderer.nightVisionScale
            // 写入；不满足条件时恒为 0.0F）。之前误去改 blockFactor/skyFactor：
            //   - 没设 nightVisionEffectIntensity → 夜视实际没生效
            //   - 把 blockFactor 从原版 ~1.4 压到 1.0 → 中等方块光照区反而变暗
            state.nightVisionEffectIntensity = 1.0F;
        }
        // 屏蔽黑暗效果：darknessEffectScale 由「选项 scale × DARKNESS 效果强度」算出，
        // 归零即完全去掉黑暗的压暗
        if (NoRender.INSTANCE.noDarkNess()) {
            state.darknessEffectScale = 0.0F;
        }
    }
}
