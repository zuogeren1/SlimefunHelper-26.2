package me.matl114.gui.complex.other;

import java.util.Objects;
import me.matl114.accessors.gui.TextFieldAccess;
import me.matl114.gui.McWidgetHelpers;
import me.matl114.utils.config.Value;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class ValueSyncTextFieldWidget<T> extends EditBox {
    Value<T> attrKeyValue;
    String lastStoredAttrKeyValue;

    public ValueSyncTextFieldWidget(
            Value<T> attrKeyValue, Font textRenderer, int x, int y, int width, int height) {
        super(textRenderer, x, y, width, height, Component.empty());
        setMaxLength(32768);
        setValue(attrKeyValue.getInput());
        this.attrKeyValue = attrKeyValue;
        setResponder(this::syncChanges);
        TextFieldAccess.of(this)
                .setBorderColorProvider(McWidgetHelpers.getWrongRedTextBoxColorProvider(this.attrKeyValue::isValidate));
    }

    public void syncChanges(String valueUpdate) {
        if (Objects.equals(lastStoredAttrKeyValue, attrKeyValue.getInput())) {
            this.attrKeyValue.setInput(valueUpdate);
            String updateValue = attrKeyValue.getInput();
            lastStoredAttrKeyValue = updateValue;
        } else {
            // internal change, update from internal
            lastStoredAttrKeyValue = attrKeyValue.getInput();
            setValue(lastStoredAttrKeyValue);
        }
    }

    private void checkAttrKeyValueUpdate() {
        if (!Objects.equals(lastStoredAttrKeyValue, attrKeyValue.getInput())) {
            lastStoredAttrKeyValue = attrKeyValue.getInput();
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
