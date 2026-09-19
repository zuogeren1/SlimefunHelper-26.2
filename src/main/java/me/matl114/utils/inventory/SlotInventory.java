package me.matl114.utils.inventory;

import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SlotInventory implements Container {
    public SlotInventory(List<Slot> slots) {
        this.slots = slots;
    }

    public List<Slot> slots;

    @Override
    public int getContainerSize() {
        return slots.size();
    }

    @Override
    public boolean isEmpty() {
        return slots.stream().allMatch(s -> s.getItem().isEmpty());
    }

    @Override
    public ItemStack getItem(int slot) {
        return slots.get(slot).getItem();
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = slots.get(slot).getItem();
        ItemStack removed;
        if (stack.isEmpty() || amount <= 0) {
            removed = ItemStack.EMPTY;
        } else {
            removed = stack.split(amount);
        }

        if (!removed.isEmpty()) {
            this.setChanged();
        }

        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack itemStack = this.slots.get(slot).getItem();
        if (itemStack.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            this.slots.get(slot).setByPlayer(ItemStack.EMPTY);
            setChanged();
            return itemStack;
        }
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.slots.get(slot).setByPlayer(stack);
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
        for (var i = 0; i < this.slots.size(); i++) {
            slots.get(i).setByPlayer(ItemStack.EMPTY);
        }
        setChanged();
    }
}
