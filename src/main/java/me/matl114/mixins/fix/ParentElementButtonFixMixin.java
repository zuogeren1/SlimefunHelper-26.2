package me.matl114.mixins.fix;

import me.matl114.accessors.gui.CustomFocusBehaviourScreenAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(ContainerEventHandler.class)
public interface ParentElementButtonFixMixin {
    @Shadow
    public abstract void setFocused(@Nullable GuiEventListener focused);

    @Inject(method = "mouseClicked", at = @At("RETURN"))
    default void mouseClicked(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        boolean returnValue = cir.getReturnValueZ();
        if (!returnValue) {
            // Debug.info("miss!");
            GuiEventListener defaultVal = null;
            if (((ContainerEventHandler) ((Object) this)) instanceof CustomFocusBehaviourScreenAccess access) {
                // force=!access.doKeepButtonWhenClicked();
                defaultVal = access.getDefaultElement();
            }
            this.setFocused(defaultVal);
        }
    }
}
