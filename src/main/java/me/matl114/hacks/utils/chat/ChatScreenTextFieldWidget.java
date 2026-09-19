package me.matl114.hacks.utils.chat;

import me.matl114.accessors.access.ChatScreenAccess;
import me.matl114.hacks.ChatTasks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class ChatScreenTextFieldWidget extends EditBox {
    ChatScreen chatScreen;

    public ChatScreenTextFieldWidget(ChatScreen chatScreen) {
        super(
                Minecraft.getInstance().fontFilterFishy,
                4,
                chatScreen.height - 12,
                chatScreen.width - 4,
                12,
                Component.translatable("chat.editBox"));
        this.chatScreen = chatScreen;
    }

    protected MutableComponent createNarrationMessage() {
        return super.createNarrationMessage()
                .append(ChatScreenAccess.of(chatScreen).getSuggestor().getNarrationMessage());
    }

    public void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        if (ChatTasks.getChatTools().obfLogin.get()) {
            if (!ChatTasks.getChatExtra().onChatObfRender(this, context, mouseX, mouseY, deltaTicks)) {
                super.extractWidgetRenderState(context, mouseX, mouseY, deltaTicks);
            }
        } else {
            super.extractWidgetRenderState(context, mouseX, mouseY, deltaTicks);
        }
    }

    public String getValue() {
        // Debug.info(isTrulyFocused());
        return super.getValue();
    }
}
