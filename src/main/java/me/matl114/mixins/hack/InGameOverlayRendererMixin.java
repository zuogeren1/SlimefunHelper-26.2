package me.matl114.mixins.hack;

import com.mojang.blaze3d.vertex.PoseStack;
import me.matl114.hacks.modules.render.NoRender;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(ScreenEffectRenderer.class)
public abstract class InGameOverlayRendererMixin {
    // 26.2 起渲染改为"提交节点"模式：
    // renderTex/renderWater/renderFire (MultiBufferSource) -> submitBlockSprite/submitWater/submitFire
    // (SubmitNodeCollector)
    //#if MC >= 26.2
    @Inject(method = "submitBlockSprite", at = @At("HEAD"), cancellable = true)
    //#else
    //$$ @Inject(method = "renderTex", at = @At("HEAD"), cancellable = true)
    //#endif
    private static void onNoRender0(
            TextureAtlasSprite sprite,
            PoseStack matrices,
            SubmitNodeCollector submitNodeCollector,
            int color,
            CallbackInfo ci) {
        if (NoRender.INSTANCE.noWallOverlay()) {
            ci.cancel();
        }
    }

    //#if MC >= 26.2
    @Inject(method = "submitWater", at = @At("HEAD"), cancellable = true)
    //#else
    //$$ @Inject(method = "renderWater", at = @At("HEAD"), cancellable = true)
    //#endif
    private static void onNoRender1(
            Minecraft client, PoseStack matrices, SubmitNodeCollector submitNodeCollector, CallbackInfo ci) {
        if (NoRender.INSTANCE.noLiquidOverlay()) {
            ci.cancel();
        }
    }

    //#if MC >= 26.2
    @Inject(method = "submitFire", at = @At("HEAD"), cancellable = true)
    //#else
    //$$ @Inject(method = "renderFire", at = @At("HEAD"), cancellable = true)
    //#endif
    private static void onNoRender2(
            PoseStack matrices, SubmitNodeCollector submitNodeCollector, TextureAtlasSprite sprite, CallbackInfo ci) {
        if (NoRender.INSTANCE.noFireOverlay()) {
            ci.cancel();
        }
    }
}
