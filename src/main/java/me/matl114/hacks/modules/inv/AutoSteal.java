package me.matl114.hacks.modules.inv;

import me.matl114.events.Event;
import me.matl114.events.Listener;
import me.matl114.hacks.api.BaseModule;
import me.matl114.hacks.api.ModulePath;
import me.matl114.hacks.utils.config.EntrySet;
import me.matl114.hacks.utils.config.Regex;
import me.matl114.managers.Configs;
import me.matl114.managers.config.FlagRef;
import me.matl114.managers.config.KeyBindRef;
import me.matl114.managers.config.NBTRef;
import me.matl114.managers.input.MultiKeyBind;
import me.matl114.utils.ChatUtils;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class AutoSteal extends BaseModule {
    // todo:
    // title steal
    // item filter
    // 0 tick steal code
    // auto shulker dump
    // with hotkeys

    public final ModulePath autoInv = makePath(Configs.INV_CONFIG, "auto-inv");
    public final ModulePath steal = autoInv.add("steal");

    // ===== 配置字段 =====
    // 主开关（FlagRef），同时也作为模块启用标志，需调用 bindFlag 绑定
    public final FlagRef enable = flagBuilder(steal.add("enable")).build();

    // 主开关切换快捷键（toggleHotkey）
    public final KeyBindRef autoStealToggleKey = moduleEntry(
                    steal.add("toggle-key"),
                    new MultiKeyBind(), // 默认按键 R（可自定义）
                    steal.add("enable") // 关联的开关配置路径
                    )
            .build();

    // 标题正则（NBTRef 类型，存储正则表达式）
    public final NBTRef<Regex> titleRegex = builder(steal.add("title-regex"), Regex.class)
            .defaultValue(new Regex(".*")) // 默认匹配所有标题
            .build();

    // 物品过滤器（RegistryRegex 类型，基于物品注册表过滤）
    public final NBTRef<EntrySet<Item>> itemFilter = builder(steal.add("item-filter"), EntrySet.<Item>parameter())
            .defaultValue(new EntrySet<>(new Regex(".*"), BuiltInRegistries.ITEM))
            .build();

    public AutoSteal() {
        super("AutoSteal");
        bindFlag(enable);
    }

    @Override
    public void registerAll() {
        super.registerAll();
        registerListener(Listener.getPreGameTick(), this::onInventoryTick);
    }

    public void onInventoryTick(Event<LocalPlayer> event) {
        if (mc.screen instanceof AbstractContainerScreen<?> handle && enable.get()) {
            Component text = handle.getTitle();
            String titleName = text == null ? "" : ChatUtils.textToPlainString(text);
            if (titleRegex.get().test(titleName)) {
                var screenHandler = handle.getMenu();
                if (screenHandler != null && !(screenHandler instanceof CreativeModeInventoryScreen.ItemPickerMenu)) {
                    for (var slot : screenHandler.slots) {
                        if (slot.container instanceof Inventory playerInventory) {
                            break;
                        } else {
                            // not a player inventory
                            ItemStack stack = slot.getItem();
                            if (!stack.isEmpty() && itemFilter.get().test(stack.getItem())) {
                                mc.gameMode.handleContainerInput(
                                        screenHandler.containerId,
                                        slot.getContainerSlot(),
                                        0,
                                        ContainerInput.QUICK_MOVE,
                                        mc.player);
                            }
                        }
                    }
                }
            }
        }
    }
}
