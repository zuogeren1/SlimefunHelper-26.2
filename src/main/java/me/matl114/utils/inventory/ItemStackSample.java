package me.matl114.utils.inventory;

import net.minecraft.world.item.ItemStack;

public record ItemStackSample(ItemStack sample) {
    public static final ItemStackSample EMPTY = new ItemStackSample(ItemStack.EMPTY);

    public static ItemStackSample of(ItemStack stack) {
        if (stack.isEmpty()) {
            return EMPTY;
        }
        var st = stack.copy();
        st.setCount(1);
        return new ItemStackSample(st);
    }

    @Override
    public int hashCode() {
        return ItemStack.hashItemAndComponents(sample);
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof ItemStackSample sample && ItemStack.isSameItemSameComponents(sample.sample, this.sample))
                || (o instanceof ItemStack item && ItemStack.isSameItemSameComponents(item, this.sample));
    }
}
