package me.matl114.hooks.mixin.xaero;

import me.matl114.hacks.modules.survival.XaeroHelper;
import me.matl114.hooks.XaeroHooks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(Gui.class)
public abstract class XaeroInGameHudMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Unique
    Screen cachedScreen;

    @Inject(method = "render", at = @At("HEAD"), order = 1)
    private void onTransparentGuiFix(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (XaeroHelper.INSTANCE.transparentGuiMapFix.get()
                && XaeroHooks.getInstance().isXaeroPlusEnable()
                && XaeroHooks.getInstance().isGuiMap(minecraft.gui.screen())) {
            cachedScreen = minecraft.gui.screen();
            minecraft.gui.setScreen(null);
        }
    }

    @Inject(method = "render", at = @At("HEAD"), order = 99999)
    private void onTransparentGuiRestore(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (cachedScreen != null) {
            minecraft.gui.setScreen(cachedScreen);
            cachedScreen = null;
        }
    }
}
