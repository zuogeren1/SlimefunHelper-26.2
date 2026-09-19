package me.matl114.utils.inventory;

import java.util.List;
import lombok.AllArgsConstructor;
import net.minecraft.world.item.ItemStack;

@AllArgsConstructor
public class ImmutableListInventory extends ImmutableInventory {
    List<ItemStack> itemStacks;

    @Override
    public int getContainerSize() {
        return itemStacks.size();
    }

    @Override
    public ItemStack getItem(int slot) {
        return itemStacks.get(slot);
    }
}
