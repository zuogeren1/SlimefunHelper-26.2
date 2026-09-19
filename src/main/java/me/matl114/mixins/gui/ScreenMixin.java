package me.matl114.mixins.gui;

import me.matl114.accessors.gui.CustomFocusBehaviourScreenAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(Screen.class)
public abstract class ScreenMixin extends AbstractContainerEventHandler {

    @Override
    public GuiEventListener getFocused() {
        GuiEventListener focused = super.getFocused();
        //  Debug.info("getFocused called");
        if (focused == null
                && (Object) this instanceof CustomFocusBehaviourScreenAccess access
                && access.autoSelectDefaultElementWhenNotFocused()
                && (focused = access.getDefaultElement()) != null) {
            // Debug.info("to default Value");
            this.setFocused(focused);
        }
        return focused;
    }

    @Inject(
            method = "keyPressed",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/gui/screens/Screen;changeFocus(Lnet/minecraft/client/gui/ComponentPath;)V",
                            shift = At.Shift.BEFORE),
            cancellable = true)
    private void onKeyPressed(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
        if (this instanceof CustomFocusBehaviourScreenAccess access && !access.enableSwitchUsingNavigation()) {
            cir.setReturnValue(false);
        }
    }
}
