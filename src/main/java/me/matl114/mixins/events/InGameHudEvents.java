package me.matl114.mixins.events;

import me.matl114.events.RenderListener;
import me.matl114.versioned.api.VDrawContext;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
//#if MC >= 260200
import net.minecraft.client.gui.Hud;
//#else
//$$ import net.minecraft.client.gui.Gui;
//#endif
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC >= 260200
@Mixin(Hud.class)
//#else
//$$ @Mixin(Gui.class)
//#endif
public abstract class InGameHudEvents {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void renderPlayerList(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        VDrawContext vdraw = VDrawContext.of(context);
        context.nextStratum();
        vdraw.pushMatrix();
        try {
            RenderListener.getRender2DEvent()
                    .broadcast(
                            vdraw,
                            tickCounter.getGameTimeDeltaPartialTick(false),
//#if MC >= 260200
                            minecraft.gameRenderer.gameRenderState.guiRenderState.isHudHidden);
//#else
//$$                             minecraft.gameRenderer.gameRenderState.optionsRenderState.hideGui);
//#endif
        } finally {
            vdraw.popMatrix();
        }
    }
}
