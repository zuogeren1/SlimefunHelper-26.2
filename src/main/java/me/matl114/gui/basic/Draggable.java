package me.matl114.gui.basic;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

public interface Draggable extends GuiEventListener {

    public boolean isDragging();

    public void releaseDrag(Screen screen, double mouseX, double mouseY);

    public boolean startDrag(Screen screen, double mouseX, double mouseY);
}
