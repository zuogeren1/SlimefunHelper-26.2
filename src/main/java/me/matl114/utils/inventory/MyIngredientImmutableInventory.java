package me.matl114.utils.inventory;

import java.util.Arrays;
import me.matl114.hacks.utils.recipes.RecipeIngredient;
import me.matl114.managers.Tasks;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MyIngredientImmutableInventory implements Container {
    public MyIngredientImmutableInventory(RecipeIngredient[] val) {
        this.ingredients = val;
    }

    RecipeIngredient[] ingredients;

    @Override
    public int getContainerSize() {
        return ingredients.length;
    }

    @Override
    public boolean isEmpty() {
        return Arrays.stream(ingredients).allMatch(RecipeIngredient::isEmpty);
    }

    public ItemStack getCurrentItemStack(RecipeIngredient ingredient) {
        ItemStack[] itemStacks = ingredient.matchingStack();
        return itemStacks.length == 0
                ? ItemStack.EMPTY
                : itemStacks[Mth.floor(Tasks.getTick() / 30.0F) % itemStacks.length];
    }

    @Override
    public ItemStack getItem(int slot) {
        return getCurrentItemStack(ingredients[slot]);
    }

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
}
