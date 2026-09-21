package me.matl114.mixins.hack;

import com.mojang.blaze3d.vertex.PoseStack;
import me.matl114.hacks.modules.render.NoRender;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(ScreenEffectRenderer.class)
public abstract class InGameOverlayRendererMixin {
    // 26.1.2 的 ScreenEffectRenderer 仍是 renderTex/renderWater/renderFire + MultiBufferSource；
    // 26.2 才改成 submitBlockSprite/submitWater/submitFire + SubmitNodeCollector
    @Inject(method = "renderTex", at = @At("HEAD"), cancellable = true)
    private static void onNoRender0(
            TextureAtlasSprite sprite, PoseStack matrices, MultiBufferSource bufferSource, CallbackInfo ci) {
        if (NoRender.INSTANCE.noWallOverlay()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderWater", at = @At("HEAD"), cancellable = true)
    private static void onNoRender1(
            Minecraft client, PoseStack matrices, MultiBufferSource bufferSource, CallbackInfo ci) {
        if (NoRender.INSTANCE.noLiquidOverlay()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderFire", at = @At("HEAD"), cancellable = true)
    private static void onNoRender2(
            PoseStack matrices, MultiBufferSource bufferSource, TextureAtlasSprite sprite, CallbackInfo ci) {
        if (NoRender.INSTANCE.noFireOverlay()) {
            ci.cancel();
        }
    }
}
