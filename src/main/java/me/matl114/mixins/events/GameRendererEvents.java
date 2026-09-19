package me.matl114.mixins.events;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import me.matl114.events.Event;
import me.matl114.events.RenderListener;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class GameRendererEvents {

    @ModifyExpressionValue(
            method = "renderLevel",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;",
                            ordinal = 0))
    private Object onRenderWorld(Object original, @Local PoseStack matrixStack) {
        if (original instanceof Boolean bl) {
            Event<PoseStack> event = new Event<>(matrixStack, true, false);
            if (!bl) {
                event.cancel();
            }
            RenderListener.getApplyWorldBobView().handleValue(event);
            return !event.isCancelled();
        }
        return original;
    }

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    public void onGetFov(CallbackInfoReturnable<Float> cir) {
        float fov = cir.getReturnValueF();
        Event<Float> fovEvent = new Event<>(fov, false, true);
        RenderListener.getFovGetListener().handleValue(fovEvent);
        float fov2 = fovEvent.context;
        if (fov2 != fov) {
            cir.setReturnValue(fov2);
            return;
        }
    }
}
