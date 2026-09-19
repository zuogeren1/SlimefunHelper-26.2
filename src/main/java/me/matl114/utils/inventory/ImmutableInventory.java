package me.matl114.utils.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public abstract class ImmutableInventory implements Container {
    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {}

    @Override
    public void setChanged() {}

    @Override
    public boolean stillValid(Player player) {
        return false;
    }

    @Override
    public void clearContent() {}

    @Override
    public boolean isEmpty() {
        int size = getContainerSize();
        for (var re = 0; re < size; ++re) {
            var item = getItem(re);
            if (item.count() != 0) return false;
        }
        return true;
    }
}
