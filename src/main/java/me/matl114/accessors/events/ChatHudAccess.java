package me.matl114.accessors.events;

import java.util.ArrayList;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;

public interface ChatHudAccess {
    public void setUniqueMessageId(String id);

    public ArrayList<GuiMessage.Line> getVisibleLines();

    public void clearUniqueMessages(String id);

    public static ChatHudAccess of(ChatComponent chatHud) {
        return (ChatHudAccess) chatHud;
    }
}
