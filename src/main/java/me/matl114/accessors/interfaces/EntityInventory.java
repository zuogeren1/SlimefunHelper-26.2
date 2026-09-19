package me.matl114.accessors.interfaces;

import javax.annotation.Nullable;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;

public interface EntityInventory<T> {
    @Nullable
    T getOwner();

    public AbstractContainerScreen<?> castHandled();

    default AbstractContainerMenu castHandler() {
        return castHandled().getMenu();
    }

    public interface Handler<T> extends EntityInventory<T> {
        default AbstractContainerScreen<?> castHandled() {
            throw new UnsupportedOperationException();
        }

        default AbstractContainerMenu castHandler() {
            return (AbstractContainerMenu) this;
        }

        public void sync(EntityInventory<T> inventory);
    }
}
