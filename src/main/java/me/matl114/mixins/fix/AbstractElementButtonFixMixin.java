package me.matl114.mixins.fix;

import me.matl114.accessors.gui.CustomFocusBehaviourScreenAccess;
import me.matl114.gui.basic.DrawableWidget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(AbstractContainerEventHandler.class)
public class AbstractElementButtonFixMixin {
    @Inject(method = "setFocused(Lnet/minecraft/client/gui/components/events/GuiEventListener;)V", at = @At("HEAD"), cancellable = true)
    private void onSetFocused(GuiEventListener focused, CallbackInfo ci) {
        if ((Object) this instanceof CustomFocusBehaviourScreenAccess access
                && !access.canFocusButtonWhenClicked()
                && focused instanceof DrawableWidget bw) {
            ci.cancel();
        }
    }
}
