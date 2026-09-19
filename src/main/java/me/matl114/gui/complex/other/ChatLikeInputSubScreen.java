package me.matl114.gui.complex.other;

import java.util.function.Consumer;
import me.matl114.gui.McWidgetHelpers;
import me.matl114.gui.basic.ContentDelegateWidget;
import me.matl114.gui.basic.DrawableWidget;
import me.matl114.gui.basic.RenderHandler;
import me.matl114.gui.basic.SubScreenWidget;
import me.matl114.utils.config.PropertyTracker;
import me.matl114.versioned.api.VDrawContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Mth;

public class ChatLikeInputSubScreen extends SubScreenWidget {
    private Consumer<String> callback;

    public ChatLikeInputSubScreen(int x, int y, int dx, int dy, Consumer<String> callback) {
        super(x, y, dx, dy);
        this.callback = callback;
        this.init();
    }

    Minecraft mc = Minecraft.getInstance();
    int messageHistoryIndex;
    String chatLastMessage = "";
    ContentDelegateWidget<EditBox> chatFieldWidget;
    CommandSuggestions suggestor;
    ContentDelegateWidget<DrawableWidget> delegateInputSuggestor;

    protected void init() {
        resetHistoryIndex();
        chatFieldWidget = McWidgetHelpers.createTextFieldEditBox(
                0, 0, this.dx, this.dy, PropertyTracker.event(this::onChatInputUpdate), "");
        chatFieldWidget.getDelegate().setBordered(false);
        chatFieldWidget.addToSub(this);

        // suggestor = new ChatInputSuggestor()
        // currently not decided
    }

    protected void onChatInputUpdate(String value) {}

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        } else if (this.isFocused()) {
            if (keyCode != 257 && keyCode != 335) {
                if (keyCode == 265) {
                    this.setChatFromHistory(-1);
                    return true;
                } else if (keyCode == 264) {
                    this.setChatFromHistory(1);
                    return true;
                }
                // remove scroll chat function
                //           else if (keyCode == 266) {
                //
                // Minecraft.getInstance().inGameHud.getChatHud().scroll(Minecraft.getInstance().inGameHud.getChatHud().getVisibleLineCount() - 1);
                //                 return true;
                //            } else if (keyCode == 267) {
                //
                // Minecraft.getInstance().inGameHud.getChatHud().scroll(-Minecraft.getInstance().inGameHud.getChatHud().getVisibleLineCount() + 1);
                //                return true;
                //            }
                else {
                    return false;
                }
            } else {
                this.onAcceptCallback(this.chatFieldWidget.getDelegate().getValue());
                this.chatFieldWidget.getDelegate().setValue("");
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
    public void renderInDefaultMatrix(
            VDrawContext context, int mouseX, int mouseY, float delta, boolean disableSelect) {
        // draw gray background for chatField
        // sb ojng
        context.fill(
                0,
                -2,
                this.chatFieldWidget.getWidth(),
                this.chatFieldWidget.getHeight() - 2,
                mc.options.getBackgroundColor(Integer.MIN_VALUE));
        RenderHandler.drawHighlightFrame(
                context,
                -1,
                -3,
                this.chatFieldWidget.getWidth() + 2,
                this.chatFieldWidget.getHeight() + 2,
                this.isFocused() ? CommonColors.WHITE : CommonColors.GRAY);
        super.renderInDefaultMatrix(context, mouseX, mouseY, delta, disableSelect);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (false // this.chatInputSuggestor.mouseClicked((double)((int)mouseX), (double)((int)mouseY), button)
        ) {
            return true;
        } else {
            return super.mouseClicked(mouseX, mouseY, button);
        }
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

    public void setChatFromHistory(int offset) {
        int i = this.messageHistoryIndex + offset;
        int j = mc.gui.hud.chat.getRecentChat().size();
        i = Mth.clamp(i, 0, j);
        if (i != this.messageHistoryIndex) {
            if (i == j) {
                this.messageHistoryIndex = j;
                this.chatFieldWidget.getDelegate().setValue(this.chatLastMessage);
            } else {
                if (this.messageHistoryIndex == j) {
                    // save temp message
                    this.chatLastMessage = this.chatFieldWidget.getDelegate().getValue();
                }

                this.chatFieldWidget.getDelegate().setValue((String)
                        mc.gui.hud.chat.getRecentChat().get(i));
                // this.chatInputSuggestor.setWindowActive(false);
                this.messageHistoryIndex = i;
            }
        }
    }
}
