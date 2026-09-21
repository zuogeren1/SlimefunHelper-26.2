package me.matl114.mixins.hack;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import me.matl114.hacks.modules.render.NoRender;
import me.matl114.hacks.modules.render.RenderExtra;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(value = Gui.class, priority = 10)
public abstract class InGameHudMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Unique
    private boolean tmpValue3;

    @Unique
    private boolean tmpValue;

    @Unique
    private boolean tmpValue2;

    @Inject(
            at = @At("HEAD"),
            method =
                    "extractTabList(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V")
    private void rejectWurstHud(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (RenderExtra.INSTANCE.noWurstHud.get()) {
            this.tmpValue3 = true;
            this.tmpValue2 = Minecraft.getInstance().gameRenderer.gameRenderState.optionsRenderState.hideGui;
            minecraft.gameRenderer.gameRenderState.optionsRenderState.hideGui = false;
            if (!this.minecraft.debugEntries.isOverlayVisible()) {
                this.tmpValue = true;
                this.minecraft.debugEntries.isOverlayVisible = true; // setF3Enabled(true);
            } else {
                this.tmpValue = false;
            }
        }
    }

    @Inject(
            method = "extractTabList",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/multiplayer/ClientLevel;getScoreboard()Lnet/minecraft/world/scores/Scoreboard;",
                            shift = At.Shift.AFTER))
    private void resetHudData(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (tmpValue3) {
            tmpValue3 = false;
            minecraft.gameRenderer.gameRenderState.optionsRenderState.hideGui = this.tmpValue2;
            if (this.tmpValue) {
                minecraft.debugEntries.isOverlayVisible = false;
            }
        }
    }

    @WrapWithCondition(
            method = "extractCameraOverlays",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/gui/Gui;extractSpyglassOverlay(Lnet/minecraft/client/gui/GuiGraphicsExtractor;F)V"))
    private boolean onRenderSpyGlass(Gui instance, GuiGraphicsExtractor context, float scale) {
        if (NoRender.INSTANCE.noItemOverlay()) {
            return false;
        }
        return true;
    }

    @WrapWithCondition(
            method = "extractCameraOverlays",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/gui/Gui;extractTextureOverlay(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/resources/Identifier;F)V",
                            ordinal = 0))
    private boolean onRenderHeadItem(Gui instance, GuiGraphicsExtractor context, Identifier texture, float opacity) {
        if (NoRender.INSTANCE.noItemOverlay()) {
            return false;
        }
        return true;
    }

    @WrapWithCondition(
            method = "extractCameraOverlays",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/gui/Gui;extractTextureOverlay(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/resources/Identifier;F)V",
                            ordinal = 1))
    private boolean onRenderFreeze(Gui instance, GuiGraphicsExtractor context, Identifier texture, float opacity) {
        if (NoRender.INSTANCE.noFreezeOverlay()) {
            return false;
        }
        return true;
    }

    @WrapWithCondition(
            method = "extractCameraOverlays",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/gui/Gui;extractPortalOverlay(Lnet/minecraft/client/gui/GuiGraphicsExtractor;F)V"))
    private boolean onRenderPortal(Gui instance, GuiGraphicsExtractor context, float scale) {
        if (NoRender.INSTANCE.noPortalOverlay()) {
            return false;
        }
        return true;
    }

    @WrapWithCondition(
            method = "extractCameraOverlays",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/gui/Gui;extractVignette(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/entity/Entity;)V"))
    private boolean onRenderVignette(Gui instance, GuiGraphicsExtractor context, Entity entity) {
        if (NoRender.INSTANCE.noVignetteOverlay()) {
            return false;
        }
        return true;
    }
}
