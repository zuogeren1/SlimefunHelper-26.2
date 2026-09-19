package me.matl114.gui.complex.other;

import java.util.function.Consumer;
import me.matl114.gui.basic.RenderHandler;
import me.matl114.versioned.api.VDrawContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Mth;

public class ChatLikeInputWidget extends EditBox {
    Consumer<String> callback;
    int messageHistoryIndex;
    String chatLastMessage = "";

    public ChatLikeInputWidget(Font textRenderer, int x, int y, int width, int height, Consumer<String> enterCallback) {
        super(textRenderer, x, y, width, height, Component.empty());
        this.callback = enterCallback;
        this.setBordered(false);
    }

    private static final Minecraft mc = Minecraft.getInstance();

    public void setChatFromHistory(int offset) {
        int i = this.messageHistoryIndex + offset;
        int j = mc.gui.hud.chat.getRecentChat().size();
        i = Mth.clamp(i, 0, j);
        if (i != this.messageHistoryIndex) {
            if (i == j) {
                this.messageHistoryIndex = j;
                setValue(this.chatLastMessage);
            } else {
                if (this.messageHistoryIndex == j) {
                    // save temp message
                    this.chatLastMessage = getValue();
                }

                setValue((String) mc.gui.hud.chat.getRecentChat().get(i));
                // this.chatInputSuggestor.setWindowActive(false);
                this.messageHistoryIndex = i;
            }
        }
    }

    public boolean keyPressed(KeyEvent input) {
        return super.keyPressed(input) || keyPressed(input.key(), input.scancode(), input.modifiers());
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {

        if (this.isFocused()) {
            if (keyCode != 257 && keyCode != 335) {
                if (keyCode == 265) {
                    this.setChatFromHistory(-1);
                    return true;
                } else if (keyCode == 264) {
                    this.setChatFromHistory(1);
                    return true;
                } else {
                    return false;
                }
            } else {
                this.onAcceptCallback(getValue());
                setValue("");
                return true;
            }
        } else {
            if (keyCode == 265) {
                this.setFocused(true);
                return true;
            }
            return false;
        }
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        context.fill(
                this.getX(),
                this.getY() - 2,
                this.getX() + width,
                this.getY() + height - 2,
                mc.options.getBackgroundColor(Integer.MIN_VALUE));
        RenderHandler.drawHighlightFrame(
                VDrawContext.of(context),
                this.getX() - 1,
                this.getY() - 3,
                width + 2,
                height + 2,
                this.isFocused() ? CommonColors.WHITE : CommonColors.GRAY);
        super.extractWidgetRenderState(context, mouseX, mouseY, deltaTicks);
    }

    public void onAcceptCallback(String value) {
        if (callback != null) {
            callback.accept(value);
        }
        // "send" action may trigger
        resetHistoryIndex();
        // avoid focus failure
        //        setFocused(false);
    }

    public void resetHistoryIndex() {
        this.messageHistoryIndex = mc.gui.hud.chat.getRecentChat().size();
    }
}
