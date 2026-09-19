package me.matl114.mixins.hack;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.matl114.hacks.modules.render.NoRender;
import me.matl114.hacks.modules.render.RenderExtra;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {

    @ModifyExpressionValue(
            method = "computeFogColor",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/core/Holder;)Z",
                            ordinal = 0))
    private boolean applyNightVision(boolean original) {
        if (RenderExtra.INSTANCE.nightVision.get()) {
            return true;
        }
        return original;
    }

    @ModifyExpressionValue(
            method = "computeFogColor",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/core/Holder;)Z",
                            ordinal = 1))
    private boolean applyNoEffect(boolean original) {
        if (RenderExtra.INSTANCE != null && RenderExtra.INSTANCE.nightVision.get()) {
            return false;
        }
        return original;
    }

    // 26.2: setupFog 的返回类型由 Vector4f 改成 FogData，且原来 CommandEncoder.mapBuffer 的
    // 调用点已不存在，改为在 RETURN 处直接改返回的 FogData。
    @Inject(
            method =
                    "setupFog(Lnet/minecraft/client/Camera;ILnet/minecraft/client/DeltaTracker;FLnet/minecraft/client/multiplayer/ClientLevel;)Lnet/minecraft/client/renderer/fog/FogData;",
            at = @At("RETURN"))
    private void applyFog(
            Camera camera,
            int viewDistance,
            DeltaTracker renderTickCounter,
            float f,
            ClientLevel clientWorld,
            CallbackInfoReturnable<FogData> cir) {
        if (NoRender.INSTANCE.noDistanceFogVanilla()) {
            FogData fogData = cir.getReturnValue();
            int d = 64 * viewDistance;
            fogData.environmentalStart = d;
            fogData.environmentalEnd = d;
            fogData.renderDistanceStart = d;
            fogData.renderDistanceEnd = d;
        }
    }
}
