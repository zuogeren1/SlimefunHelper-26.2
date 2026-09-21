package me.matl114.mixins.hack;

import me.matl114.hacks.modules.render.NoRender;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.renderer.state.level.WeatherRenderState;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 「无天气」。
 *
 * <p>26.2 里世界渲染的提取阶段整体从 {@code LevelRenderer} 搬走了，
 * {@code WeatherEffectRenderer.extractRenderState} 不再由 renderLevel 调用，
 * 所以原来的 {@code @WrapWithCondition(method="renderLevel", ...)} 挂不上。
 *
 * <p>改为直接挂在 {@code extractRenderState} 自身：命中时把 intensity 归零、
 * 清空雨雪列并取消原方法，效果等价于"不生成天气几何"。
 */
@Environment(EnvType.CLIENT)
@Mixin(WeatherEffectRenderer.class)
public abstract class WeatherEffectRendererMixin {

    // 26.1.2 的签名比 26.2 多一个 int ticks，且世界参数是 Level（26.2 是 ClientLevel）
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void onNoWeather(
            Level level, int ticks, float partialTick, Vec3 cameraPos, WeatherRenderState state, CallbackInfo ci) {
        if (NoRender.INSTANCE.noWeather()) {
            state.intensity = 0.0F;
            state.rainColumns.clear();
            state.snowColumns.clear();
            ci.cancel();
        }
    }
}
