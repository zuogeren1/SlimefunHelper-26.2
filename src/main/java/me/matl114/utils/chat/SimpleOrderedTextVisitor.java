package me.matl114.utils.chat;

import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSink;

public class SimpleOrderedTextVisitor implements FormattedCharSink {
    StringBuilder builder;

    public SimpleOrderedTextVisitor() {
        builder = new StringBuilder();
    }

    public SimpleOrderedTextVisitor(StringBuilder bu) {
        builder = bu;
    }

    @Override
    public boolean accept(int index, Style style, int codePoint) {
        builder.appendCodePoint(codePoint);
        return true;
    }

    public StringBuilder getContent() {
        return this.builder;
    }
}
