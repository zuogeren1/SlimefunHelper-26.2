package me.matl114.accessors.access;

import me.matl114.accessors.gui.ScreenAccess;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;

public interface ChatScreenAccess extends ScreenAccess {
    public EditBox getInputWidget();

    public void resetMessageHistoryIndex();

    public CommandSuggestions getSuggestor();

    static ChatScreenAccess of(ChatScreen screen) {
        return (ChatScreenAccess) screen;
    }
}
