package me.matl114.mixins.events;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.matl114.events.Event;
import me.matl114.events.RenderListener;
import me.matl114.hacks.modules.render.NoRender;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * FOV 事件。
 *
 * <p>26.2 把 FOV 从 {@code GameRenderer.getFov()} 搬到了 {@link Camera}
 * （{@code Camera.fov} 字段，由 {@code Camera.update(DeltaTracker)} 里的
 * {@code calculateFov(float)} 计算），因此这个钩子从 {@code GameRendererEvents}
 * 迁移到这里。
 */
@Mixin(Camera.class)
public abstract class CameraEvents {
    @WrapOperation(
            method = "update",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;calculateFov(F)F"))
    private float onGetFov(Camera instance, float partialTick, Operation<Float> original) {
        float fov = original.call(instance, partialTick);
        Event<Float> fovEvent = new Event<>(fov, false, true);
        RenderListener.getFovGetListener().handleValue(fovEvent);
        return fovEvent.context;
    }
    /**
     * 屏蔽失明（BLINDNESS）带来的视野/迷雾压制。
     *
     * <p>26.2 里 {@code Camera.extractRenderState(CameraRenderState, float)} 会读
     * {@code MobEffects.BLINDNESS}，原本挂在别处的判断挂不上了，改为在这里把
     * {@code hasEffect(BLINDNESS)} 结果强制为 false。
     */
    // 26.2 的 Camera.extractRenderState 里恰好有两次 hasEffect：
    //   206: MobEffects.BLINDNESS  -> hasEffect   (ordinal 0)
    //   216: MobEffects.DARKNESS   -> hasEffect   (ordinal 1)
    // 合成 doesMobEffectBlockSky = hasEffect(BLINDNESS) || hasEffect(DARKNESS)。
    // 必须带 ordinal 分开处理，否则无 ordinal 会两处都改 —— 只开「屏蔽失明」会连黑暗一起关掉；
    // 只开「屏蔽黑暗」则此锚点完全不起作用。

    @ModifyExpressionValue(
            method = "extractRenderState",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/core/Holder;)Z",
                            ordinal = 0))
    private boolean onNoBlindness(boolean original) {
        if (NoRender.INSTANCE.noBlindness()) {
            return false;
        }
        return original;
    }

    @ModifyExpressionValue(
            method = "extractRenderState",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/core/Holder;)Z",
                            ordinal = 1))
    private boolean onNoDarkness(boolean original) {
        if (NoRender.INSTANCE.noDarkNess()) {
            return false;
        }
        return original;
    }
}
