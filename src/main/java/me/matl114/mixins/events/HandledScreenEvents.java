package me.matl114.mixins.events;

import com.llamalad7.mixinextras.sugar.Local;
import me.matl114.events.RenderListener;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
@Environment(EnvType.CLIENT)
public abstract class HandledScreenEvents extends Screen {

    protected HandledScreenEvents(Component title) {
        super(title);
    }

    @Inject(
            method = "renderContents",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lorg/joml/Matrix3x2fStack;translate(FF)Lorg/joml/Matrix3x2f;",
                            shift = At.Shift.AFTER))
    public void onRenderBegin(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        RenderListener.renderHandledScreen(context, (AbstractContainerScreen<?>) (Object) this, mouseX, mouseY, delta);
    }

    @Inject(
            method = "renderSlots",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderSlot(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/inventory/Slot;II)V"))
    public void onRenderSlot(GuiGraphicsExtractor context, int mouseX, int mouseY, CallbackInfo ci, @Local Slot slot) {
        RenderListener.renderSlotInScreen(context, (AbstractContainerScreen<?>) (Object) this, slot);
    }

    // fix mouse scroll dispatch
    @Inject(method = "mouseScrolled", at = @At("RETURN"), cancellable = true)
    public void onMouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount,
            CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            return;
        }
        cir.setReturnValue(super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount));
    }
}
