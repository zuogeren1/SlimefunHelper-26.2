package me.matl114.mixins.gui;

import java.util.function.Consumer;
import javax.annotation.Nonnull;
import me.matl114.accessors.gui.TextFieldAccess;
import me.matl114.gui.McWidgetHelpers;
import me.matl114.gui.basic.ColorProvider;
import me.matl114.utils.ScreenUtils;
import me.matl114.utils.config.PropertyTracker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractTextAreaWidget;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(MultiLineEditBox.class)
public abstract class EditBoxWidgetMixin extends AbstractTextAreaWidget implements TextFieldAccess {
    @Unique
    private static final ColorProvider ORIGIN_PROVIDER = McWidgetHelpers.getDefaultTextBoxColorProvider();

    @Unique
    public void setBorderColorProvider(ColorProvider provider) {
        this.boxColorProvider = provider == null ? ORIGIN_PROVIDER : provider;
    }

    @Shadow
    public abstract void setValueListener(Consumer<String> changeListener);

    @Shadow
    @Final
    private MultilineTextField textField;

    @Shadow
    protected abstract void seekCursorScreen(double mouseX, double mouseY);

    @Shadow
    protected abstract double scrollRate();

    @Unique
    public void setListener(PropertyTracker<TextFieldAccess, String> tracker) {
        setValueListener((str) -> tracker.valueChange(this, str));
    }

    @Unique
    @Nonnull
    private ColorProvider boxColorProvider = ORIGIN_PROVIDER;

    public EditBoxWidgetMixin(int i, int j, int k, int l, Component text) {
        // 26.2: AbstractTextAreaWidget 构造新增 ScrollbarSettings 参数
        super(i, j, k, l, text, net.minecraft.client.gui.components.AbstractScrollArea.ScrollbarSettings.NO_SCROLL);
    }
    // override ALL EditBox behaviour
    @Override
    protected void extractBorder(GuiGraphicsExtractor context, int x, int y, int width, int height) {
        McWidgetHelpers.drawTextWidgetBox(this, context, x, y, width, height, this.isFocused(), this.boxColorProvider);
    }

    @Inject(method = "setFocused", at = @At("HEAD"))
    private void resetSelectOnRelease(boolean focused, CallbackInfo ci) {
        if (!focused) {
            resetSelect();
        }
    }

    @Inject(method = "keyPressed", at = @At(value = "RETURN"), cancellable = true)
    public void fixInventoryKeyPressedWhenFocused(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
        if (this.isFocused()
                && Minecraft.getInstance().options.keyInventory.matches(input)) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    public boolean canStartDrag(double mouseX, double mouseY) {
        return this.isWithinBounds(mouseX, mouseY) || super.scrolling;
    }

    @Unique
    private boolean isWithinBounds(double x, double y) {
        return x >= (double) this.getX()
                && y >= (double) this.getY()
                && x < (double) this.getRight()
                && y < (double) this.getBottom();
    }

    @Unique
    public void dragSelect(int deltaX, int deltaY, boolean shiftDownAction) {
        //        if(deltaX >= 1 && deltaX <= this.getWidth() -1 && deltaY >= 1 && deltaY <= this.getHeight() -1){

        //        }else {
        // if(deltaY < 0 || deltaY > this.getHeight() || deltaX < 0 || deltaX > this.getWidth()){
        if (!super.scrolling) {
            this.textField.setSelecting(true);
            this.seekCursorScreen(this.getX() + deltaX, this.getY() + deltaY);
            this.textField.setSelecting(ScreenUtils.hasShiftDown());
        }

        //            if(deltaY < 0){
        //                this.setScrollY(this.getScrollY() - 2.0f * this.getDeltaYPerScroll());
        //            }else if(deltaY >  this.getHeight()){
        //                this.setScrollY(this.getScrollY() + 2.0f * this.getDeltaYPerScroll());
        //            }else{
        //
        //            }
        //        }
        // }
    }

    @Unique
    public void resetSelect() {
        if (this.textField.hasSelection()) {
            this.textField.setSelecting(false);
            this.textField.selectCursor = this.textField.cursor();
        }
    }
}
