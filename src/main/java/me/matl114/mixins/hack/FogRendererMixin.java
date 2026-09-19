package me.matl114.mixins.hack;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import me.matl114.hacks.modules.render.NoRender;
import me.matl114.hacks.modules.render.RenderExtra;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.joml.Vector4f;
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
                            target =
                                    "Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/core/Holder;)Z",
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
                            target =
                                    "Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/core/Holder;)Z",
                            ordinal = 1))
    private boolean applyNoEffect(boolean original) {
        if (RenderExtra.INSTANCE != null && RenderExtra.INSTANCE.nightVision.get()) {
            return false;
        }
        return original;
    }

    @Inject(
            method =
                    "setupFog(Lnet/minecraft/client/Camera;ILnet/minecraft/client/DeltaTracker;FLnet/minecraft/client/multiplayer/ClientLevel;)Lorg/joml/Vector4f;",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lcom/mojang/blaze3d/systems/CommandEncoder;mapBuffer(Lcom/mojang/blaze3d/buffers/GpuBuffer;ZZ)Lcom/mojang/blaze3d/buffers/GpuBuffer$MappedView;",
                            shift = At.Shift.BEFORE))
    private void applyFog(
            Camera camera,
            int viewDistance,
            DeltaTracker renderTickCounter,
            float f,
            ClientLevel clientWorld,
            CallbackInfoReturnable<Vector4f> cir,
            @Local FogData fogData,
            @Local Vector4f color) {
        if (NoRender.INSTANCE.noDistanceFogVanilla()) {
            int d = 64 * viewDistance;
            fogData.environmentalStart = d;
            fogData.environmentalEnd = d;
            fogData.renderDistanceStart = d;
            fogData.renderDistanceEnd = d;
        }
    }
}
