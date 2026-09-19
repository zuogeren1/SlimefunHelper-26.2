package me.matl114.mixins.hack;

import me.matl114.hacks.modules.render.NoRender;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Inject(method = "extractTransparentBackground", at = @At("HEAD"), cancellable = true)
    private void renderBackground(GuiGraphicsExtractor context, CallbackInfo ci) {
        if (NoRender.INSTANCE.noGuiBackGroundOverlay()) {
            ci.cancel();
        }
    }
}
