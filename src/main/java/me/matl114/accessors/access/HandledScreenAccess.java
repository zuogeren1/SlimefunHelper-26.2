package me.matl114.accessors.access;

import javax.annotation.Nullable;
import me.matl114.accessors.gui.ScreenAccess;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

public interface HandledScreenAccess extends ScreenAccess {
    @Nullable
    public Slot reallyGetSlotAt(double var1, double var3);

    static HandledScreenAccess of(AbstractContainerScreen var0) {
        return (HandledScreenAccess) var0;
    }

    public int getScreenX();

    public int getScreenY();

    public int getScreenBackgroundX();

    public int getScreenBackgroundY();
}
