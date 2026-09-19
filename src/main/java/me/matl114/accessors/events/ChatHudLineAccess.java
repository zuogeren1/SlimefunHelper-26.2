package me.matl114.accessors.events;

import net.minecraft.client.multiplayer.chat.GuiMessage;

public interface ChatHudLineAccess {
    public void setUniqueMessageId(String uniqueMessageId);

    public String getUniqueMessageId();

    public static ChatHudLineAccess of(GuiMessage chatHudLine) {
        return (ChatHudLineAccess) (Object) chatHudLine;
    }

    public static ChatHudLineAccess of(GuiMessage.Line uniqueMessageId) {
        return (ChatHudLineAccess) (Object) uniqueMessageId;
    }
}
