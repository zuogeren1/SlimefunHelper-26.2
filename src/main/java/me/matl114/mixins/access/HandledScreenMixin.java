package me.matl114.mixins.access;

import me.matl114.accessors.access.HandledScreenAccess;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin extends Screen implements HandledScreenAccess {
    protected HandledScreenMixin(Component title) {
        super(title);
    }

    @Shadow
    protected abstract Slot getHoveredSlot(double x, double y);

    @Override
    @Unique
    public Slot reallyGetSlotAt(double var1, double var3) {
        return getHoveredSlot(var1, var3);
    }

    @Shadow
    protected int leftPos;

    @Shadow
    protected int topPos;

    @Accessor("leftPos")
    public abstract int getScreenX();

    @Accessor("topPos")
    public abstract int getScreenY();

    @Accessor("imageWidth")
    public abstract int getScreenBackgroundX();

    @Accessor("imageHeight")
    public abstract int getScreenBackgroundY();
}
