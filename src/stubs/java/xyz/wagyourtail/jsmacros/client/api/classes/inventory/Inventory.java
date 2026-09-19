package xyz.wagyourtail.jsmacros.client.api.classes.inventory;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public class Inventory<T extends AbstractContainerScreen<?>> {

    public T getRawContainer() {
        return null;
    }

    public static Inventory<?> create() {
        return null;
    }

    public static Inventory<?> create(Screen s) {
        return null;
    }
}
