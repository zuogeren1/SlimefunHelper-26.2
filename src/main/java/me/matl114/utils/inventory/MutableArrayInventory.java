package me.matl114.utils.inventory;

import java.util.Arrays;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MutableArrayInventory implements Container {
    ItemStack[] stacks;

    public MutableArrayInventory(ItemStack[] stacks) {
        this.stacks = stacks;
    }

    @Override
    public int getContainerSize() {
        return stacks.length;
    }

    @Override
    public boolean isEmpty() {
        return Arrays.stream(stacks).allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return stacks[slot];
    }

    private ItemStack splitStack(int slot, int amount) {
        return slot >= 0 && slot < stacks.length && !((ItemStack) stacks[slot]).isEmpty() && amount > 0
                ? ((ItemStack) stacks[slot]).split(amount)
                : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack itemStack = splitStack(slot, amount);
        if (!itemStack.isEmpty()) {
            this.setChanged();
        }

        return itemStack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack itemStack = (ItemStack) this.stacks[slot];
        if (itemStack.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            this.stacks[slot] = ItemStack.EMPTY; // .set(slot, ItemStack.EMPTY);
            setChanged();
            return itemStack;
        }
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.stacks[slot] = stack; // .set(slot, stack);
        setChanged();
    }

    @Override
    public void setChanged() {}

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        for (var i = 0; i < this.stacks.length; i++) {
            stacks[i] = ItemStack.EMPTY; // .set(i, ItemStack.EMPTY);
        }
        setChanged();
    }
}
