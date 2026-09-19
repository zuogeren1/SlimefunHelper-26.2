package me.matl114.utils;

import java.nio.charset.StandardCharsets;
import java.util.*;
import me.matl114.bukkit.BukkitItemStackUtils;
import me.matl114.versioned.api.VHideFlag;
import me.matl114.versioned.api.VRecord;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

public class CustomItemStackBuilder {
    ItemStack stack = new ItemStack(Items.STONE);
    List<Component> tooltip = new ArrayList<>();

    public static CustomItemStackBuilder builder() {
        return new CustomItemStackBuilder();
    }

    public CustomItemStackBuilder() {}

    public CustomItemStackBuilder type(String type) {
        return type(BuiltInRegistries.ITEM.getValue(Identifier.tryParse(type)));
    }

    public CustomItemStackBuilder type(Item type) {
        if (type != Items.AIR) {
            int cnt = Math.min(1, stack.getCount());
            stack = stack.transmuteCopyIgnoreEmpty(type, cnt);
        }
        return this;
    }

    public CustomItemStackBuilder amount(int amount) {
        stack.setCount(amount);
        return this;
    }

    public CustomItemStackBuilder name(String name) {
        return name(ChatUtils.stringToText(name));
    }

    public CustomItemStackBuilder name(Component name) {
        ItemStackUtils.setOrRemoveChange(this.stack, DataComponents.CUSTOM_NAME, name);
        return this;
    }

    public CustomItemStackBuilder lore() {
        tooltip.clear();
        return this;
    }

    public CustomItemStackBuilder append(Component tooltip) {
        this.tooltip.add(tooltip);
        return this;
    }

    public CustomItemStackBuilder append(String tooltip) {
        return append(ChatUtils.stringToText(tooltip));
    }

    public CustomItemStackBuilder endLore() {
        ItemStackUtils.setOrRemoveChange(this.stack, DataComponents.LORE, new ItemLore(List.copyOf(this.tooltip)));
        return this;
    }

    public CustomItemStackBuilder hideFlag(VHideFlag flag) {
        flag.setHideFlag(this.stack, true);
        return this;
    }

    public CustomItemStackBuilder skullHash(String hash) {
        ItemStackUtils.setOrRemoveChange(
                stack,
                DataComponents.PROFILE,
                VRecord.staticProfile(
                        UUID.nameUUIDFromBytes(hash.getBytes(StandardCharsets.UTF_8)),
                        "CS-CoreLib",
                        BukkitItemStackUtils.buildPropertyMap(VRecord.createProperty(), hash)));
        return this;
    }

    public CustomItemStackBuilder skullOwner(String owner) {
        ItemStackUtils.setOrRemoveChange(stack, DataComponents.PROFILE, VRecord.dynamicProfile(owner));
        return this;
    }

    public CustomItemStackBuilder glint() {
        ItemStackUtils.setOrRemoveChange(stack, DataComponents.ENCHANTMENT_GLINT_OVERRIDE, Boolean.TRUE);
        return this;
    }

    public ItemStack build() {
        return stack.copy();
    }
}
