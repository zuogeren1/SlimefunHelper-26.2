package me.matl114.accessors.gui;

import java.util.function.Consumer;
import me.matl114.accessors.interfaces.MetadataHolder;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;

public interface ScreenAccess extends MetadataHolder {
    public <T extends GuiEventListener & Renderable & NarratableEntry> T addDrawableChildTo(T val);

    public void removeChildFrom(GuiEventListener val);

    public static ScreenAccess of(Screen screen) {
        return (ScreenAccess) screen;
    }

    public Screen getParent();

    public void setParent(Screen screen);

    public void open();

    public void openFromCurrent();

    public void openFrom(Screen parent);

    public void switchToScreen(Screen anotherScreen);

    public void switchFromCurrent();

    public void addInitTask(Consumer<Screen> runnable);

    public void addCloseFuture(Runnable runnable);
}
