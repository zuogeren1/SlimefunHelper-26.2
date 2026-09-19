package me.matl114.gui;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import me.matl114.accessors.gui.TextFieldAccess;
import me.matl114.gui.basic.ColorProvider;
import me.matl114.gui.basic.ContentDelegateWidget;
import me.matl114.utils.config.AttrKeyValue;
import me.matl114.utils.config.PropertyTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

public class McWidgetHelpers {
    private static final Minecraft mc = Minecraft.getInstance();

    public static ContentDelegateWidget<MultiLineEditBox> createMultiLineEditBox(
            int x, int y, int dx, int dy, PropertyTracker<MultiLineEditBox, String> valueTracker, String origin) {
        return createEnhancedMultiLine(x, y, dx, dy, valueTracker, origin, null);
        //        EditBoxWidget widget = new EditBoxWidget(mc.textRenderer, x,y, dx, dy, Text.empty(), Text.empty());
        //        widget.setText(origin);
        //        widget.setChangeListener((str)-> valueTracker.valueChange(widget, str));
        //        return new ContentDelegateWidget<>(0,0, 0,0)
        //            .setContentDelegate(widget);
    }

    public static ContentDelegateWidget<MultiLineEditBox> createMultiLineEditBox(
            int x,
            int y,
            int dx,
            int dy,
            PropertyTracker<MultiLineEditBox, String> valueTracker,
            String origin,
            ColorProvider boxColorProvider) {
        return createEnhancedMultiLine(x, y, dx, dy, valueTracker, origin, boxColorProvider);
        //        EditBoxWidget widget = new EditBoxWidget(mc.textRenderer, x,y, dx, dy, Text.empty(), Text.empty());
        //        widget.setText(origin);
        //        widget.setChangeListener((str)-> valueTracker.valueChange(widget, str));
        //        return new ContentDelegateWidget<>(0,0, 0,0)
        //            .setContentDelegate(widget);
    }

    public static <T> ContentDelegateWidget<EditBox> createTextFieldEditBox(
            int x, int y, int dx, int dy, PropertyTracker<T, String> valueTracker, String origin) {
        if (true) return createEnhancedTextBox(x, y, dx, dy, valueTracker, origin, null);
        EditBox textFieldWidget = new EditBox(mc.font, 0, 0, dx, dy, Component.empty());
        textFieldWidget.setMaxLength(32768);
        textFieldWidget.setValue(origin);
        textFieldWidget.setResponder((str) -> valueTracker.valueChange((T) textFieldWidget, str));
        return new ContentDelegateWidget<EditBox>(x, y, 0, 0).setContentDelegate(textFieldWidget);
    }

    public static <T> ContentDelegateWidget<EditBox> createTextFieldEditBox(
            int x,
            int y,
            int dx,
            int dy,
            PropertyTracker<T, String> valueTracker,
            String origin,
            ColorProvider boxColorProvider) {
        if (true) return createEnhancedTextBox(x, y, dx, dy, valueTracker, origin, boxColorProvider);
        EditBox textFieldWidget = new EditBox(mc.font, 0, 0, dx, dy, Component.empty());
        textFieldWidget.setMaxLength(32768);
        textFieldWidget.setValue(origin);
        textFieldWidget.setResponder((str) -> valueTracker.valueChange((T) textFieldWidget, str));
        TextFieldAccess.of(textFieldWidget).setBorderColorProvider(boxColorProvider);
        return new ContentDelegateWidget<EditBox>(x, y, 0, 0).setContentDelegate(textFieldWidget);
    }

    public static <T> ContentDelegateWidget<EditBox> createAttrValueEditBox(
            AttrKeyValue<T> attrKeyValue, int x, int y, int dx, int dy) {
        return new TextContentDelegateWidget<>(
                x, y, new AttrKeyValueTextFieldWidget<>(attrKeyValue, mc.font, 0, 0, dx, dy));
    }

    private static final ColorProvider TEXT_DEFAULT = (el, fo) -> fo ? -1 : -6250336;

    public static ColorProvider getDefaultTextBoxColorProvider() {
        return TEXT_DEFAULT;
    }

    public static ColorProvider getWrongRedTextBoxColorProvider(BooleanSupplier supplier) {
        return (el, fo) -> {
            return supplier.getAsBoolean() ? (fo ? -1 : -6250336) : CommonColors.RED;
        };
    }

    public static void drawTextWidgetBox(
            Renderable drawable,
            GuiGraphicsExtractor context,
            int x,
            int y,
            int width,
            int height,
            boolean focus,
            ColorProvider borderColor) {
        Integer i = borderColor.provideTextColor(drawable, focus);
        if (i != null) {
            context.fill(x, y, x + width, y + height, i);
        }
        context.fill(x + 1, y + 1, x + width - 1, y + height - 1, -16777216);
    }

    public static <T> ContentDelegateWidget<EditBox> createEnhancedTextBox(
            int x,
            int y,
            int dx,
            int dy,
            PropertyTracker<T, String> valueTracker,
            String origin,
            ColorProvider boxColorProvider) {
        EditBox textFieldWidget = new EditBox(mc.font, 0, 0, dx, dy, Component.empty());
        textFieldWidget.setMaxLength(32768);
        textFieldWidget.setValue(origin);
        textFieldWidget.setResponder((str) -> valueTracker.valueChange((T) textFieldWidget, str));
        if (boxColorProvider != null) TextFieldAccess.of(textFieldWidget).setBorderColorProvider(boxColorProvider);
        return new TextContentDelegateWidget<>(x, y, textFieldWidget);
    }

    public static <T> ContentDelegateWidget<MultiLineEditBox> createEnhancedMultiLine(
            int x,
            int y,
            int dx,
            int dy,
            PropertyTracker<MultiLineEditBox, String> valueTracker,
            String origin,
            ColorProvider boxColorProvider) {
        MultiLineEditBox widget = MultiLineEditBox.builder()
                .setX(x)
                .setY(y)
                .setPlaceholder(Component.empty())
                .build(mc.font, dx, dy, Component.empty());
        widget.setValue(origin);
        widget.setValueListener((str) -> valueTracker.valueChange(widget, str));
        if (boxColorProvider != null) {
            TextFieldAccess.of(widget).setBorderColorProvider(boxColorProvider);
        }
        return new TextContentDelegateWidget<>(0, 0, widget);
    }

    public static class TextContentDelegateWidget<T extends AbstractWidget> extends ContentDelegateWidget<T> {

        public TextContentDelegateWidget(int x, int y, T widget) {
            super(x, y, 0, 0);
            this.setContentDelegate(widget);
        }

        boolean startDrag = false;

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
            if (this.startDrag && this.getDelegate() != null) {
                var delegate = this.getDelegate();
                // 设置cursor位置
                float textureScale = getTextureScale();
                TextFieldAccess.of(delegate)
                        .dragSelect(
                                (int) ((mouseX - this.getX() - delegate.getX()) / textureScale),
                                (int) ((mouseY - this.getY() - delegate.getY()) / textureScale),
                                true);
            }
            return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }

        @Override
        public boolean startDrag(Screen screen, double mouseX, double mouseY) {
            if (getDelegate() != null
                    && TextFieldAccess.of(getDelegate()).canStartDrag(mouseX - this.getX(), mouseY - this.getY())) {
                this.startDrag = true;
                return true;
            }
            return false;
        }

        @Override
        public boolean isDragging() {
            return this.getDelegate() != null && this.startDrag;
        }

        @Override
        public void releaseDrag(Screen screen, double mouseX, double mouseY) {
            this.startDrag = false;
            //
        }
    }

    public static class AttrKeyValueTextFieldWidget<T> extends EditBox {
        AttrKeyValue<T> attrKeyValue;
        String lastStoredAttrKeyValue;

        public AttrKeyValueTextFieldWidget(
                AttrKeyValue<T> attrKeyValue, Font textRenderer, int x, int y, int width, int height) {
            super(textRenderer, x, y, width, height, Component.empty());
            setMaxLength(32768);
            setValue(attrKeyValue.getValue());
            this.attrKeyValue = attrKeyValue;
            setResponder(this::syncChanges);
            TextFieldAccess.of(this)
                    .setBorderColorProvider(getWrongRedTextBoxColorProvider(this.attrKeyValue::isValidate));
        }

        public void syncChanges(String valueUpdate) {
            if (Objects.equals(lastStoredAttrKeyValue, attrKeyValue.getValue())) {
                this.attrKeyValue.valueChange(this, valueUpdate);
                String updateValue = attrKeyValue.getValue();
                lastStoredAttrKeyValue = updateValue;
            } else {
                // internal change, update from internal
                lastStoredAttrKeyValue = attrKeyValue.getValue();
                setValue(lastStoredAttrKeyValue);
            }
        }

        private void checkAttrKeyValueUpdate() {
            if (!Objects.equals(lastStoredAttrKeyValue, attrKeyValue.getValue())) {
                lastStoredAttrKeyValue = attrKeyValue.getValue();
                setValue(lastStoredAttrKeyValue);
            }
        }

        public String getValue() {
            checkAttrKeyValueUpdate();
            return super.getValue();
        }

        @Override
        public void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
            checkAttrKeyValueUpdate();
            super.extractWidgetRenderState(context, mouseX, mouseY, deltaTicks);
        }
    }
}
