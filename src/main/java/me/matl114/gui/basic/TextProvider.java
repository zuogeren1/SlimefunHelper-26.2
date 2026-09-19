package me.matl114.gui.basic;

import net.minecraft.util.FormattedCharSequence;

public interface TextProvider {
    public net.minecraft.network.chat.Component getText(DrawableWidget el);

    // DO NO CALL
    default FormattedCharSequence getLabel(DrawableWidget element) {
        net.minecraft.network.chat.Component text = getText(element);
        return text == null ? null : text.getVisualOrderText();
    }

    static TextProvider of(net.minecraft.network.chat.Component text) {
        return ((b) -> text);
    }
}
