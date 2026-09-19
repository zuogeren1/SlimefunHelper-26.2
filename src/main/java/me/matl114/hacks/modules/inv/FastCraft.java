package me.matl114.hacks.modules.inv;

import lombok.Getter;
import me.matl114.accessors.access.HandledScreenAccess;
import me.matl114.events.Event;
import me.matl114.events.Listener;
import me.matl114.events.impl.RecipeBookToggle;
import me.matl114.gui.basic.*;
import me.matl114.gui.complex.other.TradeInformationSubScreen;
import me.matl114.gui.elements.ButtonElement;
import me.matl114.gui.elements.SlotElement;
import me.matl114.hacks.InvTasks;
import me.matl114.hacks.RecipeTasks;
import me.matl114.hacks.api.BaseModule;
import me.matl114.hacks.api.ModulePath;
import me.matl114.hacks.utils.HotKeyUtils;
import me.matl114.managers.Configs;
import me.matl114.managers.TaskManagers;
import me.matl114.managers.config.FlagRef;
import me.matl114.utils.ChatUtils;
import me.matl114.utils.Debug;
import me.matl114.utils.ScreenUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import org.spongepowered.asm.mixin.Unique;

public class FastCraft extends BaseModule {
    public final ModulePath fastCraft = makePath(Configs.INV_CONFIG, "fast-craft");

    public FastCraft() {
        super("FastCraft");
        bindFlag(enable);
    }

    public final FlagRef enable =
            flagBuilder(fastCraft.add("enable-fastcraft-buttons")).build();

    public final FlagRef dropCraft = flagBuilder(fastCraft.add("drop-craft")).build();

    @Override
    public void registerAll() {
        super.registerAll();
        registerListener(
                Listener.getPostInitializeScreen().getChannel(AbstractContainerScreen.class),
                this::onCraftScreenInitialize);
        registerListener(Listener.getPostToggleRecipeBook(), this::onRecipeBookToggle);
        registerListener(Listener.getClickCraftingRecipe(), this::onRecipeClicked);
        TaskManagers.getToggleManager().register(TaskManagers.PREFIX_BUTTON_TOGGLE + "." + "drop-craft", dropCraft);
    }

    public void onCraftScreenInitialize(Event<Screen> event) {
        if (enable.get()) {
            if (event.context instanceof CraftingScreen craftingScreen) {
                lastScreen = null;
                addCraftingInventoryButton(craftingScreen);
            } else if (event.context instanceof InventoryScreen inventoryScreen) {
                lastScreen = null;
                if (!mc.gameMode.getPlayerMode().isCreative()) {
                    addInventoryButton(inventoryScreen);
                }
            } else if (event.context instanceof MerchantScreen merchantScreen) {
                lastScreen = null;
                addMerchantInformation(merchantScreen);
            }
        }
    }

    public void onRecipeBookToggle(Event<RecipeBookToggle> event) {
        if (event.context().provider() == lastScreen) {
            // recalculate x
            lastScreenWidget.setX(HandledScreenAccess.of(lastScreen).getScreenX());
        }
    }

    public void onRecipeClicked(Event<RecipeDisplayId> event) {
        if (!isLock()) {
            lastCrafted = event.context();
        }
    }

    public void toggleRecipeLock() {
        lock = !lock;
        Debug.chat("Toggle RecipeLock", lock);
    }

    @Getter
    private RecipeDisplayId lastCrafted;

    public void placeLastCraftingRecipe(
            AbstractContainerScreen<? extends AbstractCraftingMenu> craftingScreen, boolean doCraft) {
        // var recipeBook = craftingScreen.getRecipeBookWidget();
        RecipeDisplayId last = lastCrafted;
        if (last != null) {
            mc.gameMode.handlePlaceRecipe(craftingScreen.getMenu().containerId, last, true);
            if (doCraft) {
                int maxCraft = 64;
                for (Ingredient material : RecipeTasks.getIngredients(last)) {
                    for (ItemStack val :
                            RecipeTasks.streamIngredientOptions(material).toList()) {
                        maxCraft = Math.min(maxCraft, val.getMaxStackSize());
                    }
                }
                int slot = craftingScreen.getMenu().getResultSlot().getContainerSlot();
                craftAtSlotIndex(craftingScreen, maxCraft, slot);
            }
        } else {
            Debug.chat("Crafting History Is Empty");
        }
    }

    public void craftAtSlotIndex(AbstractContainerScreen<?> screen, int maxCraft, int slot) {
        // Debug.info("What's wrong?",doCraft);

        boolean dropCraft = this.dropCraft.get();
        // Debug.info("Drop craft?",dropCraft);
        if (dropCraft) {
            for (int i = 0; i < maxCraft; ++i) {
                InvTasks.getClickExecutor().execute(() -> {
                    mc.gameMode.handleContainerInput(
                            screen.getMenu().containerId, slot, 0, ContainerInput.THROW, mc.player);
                });
            }
        } else {
            InvTasks.getClickExecutor().execute(() -> {
                mc.gameMode.handleContainerInput(
                        screen.getMenu().containerId, slot, 1, ContainerInput.QUICK_MOVE, mc.player);
            });
        }
    }

    @Getter
    boolean lock;

    private ItemStack getDisplayItemStack() {
        if (lastCrafted != null) {
            return RecipeTasks.getRecipeResult(lastCrafted);
        } else {
            return new ItemStack(Items.BARRIER);
        }
    }

    private ItemStack getLockItem() {
        if (lock) {
            return new ItemStack(Items.BARRIER);
        } else {
            return ItemStack.EMPTY;
        }
    }

    SubScreenWidget lastScreenWidget = null;
    AbstractContainerScreen<?> lastScreen = null;

    @Unique
    private void addCraftingInventoryButton(CraftingScreen screen) {
        HandledScreenAccess access = HandledScreenAccess.of(screen);
        SubScreenWidget recipeSubScreen = SubScreenWidget.instance(access.getScreenX(), 0, screen.width, screen.height);

        ExecutableWidget putLastRecipeButton = ExecutableWidget.instance(120, screen.height / 2 - 25, 24, 12)
                .setElementHandler(new ButtonElement(
                                TextProvider.of(Component.translatable("widget.fast-craft.craft")),
                                ButtonAction.run(() -> placeLastCraftingRecipe(screen, ScreenUtils.hasShiftDown())))
                        .withTooltips(TooltipHandler.of(
                                ChatUtils.parseTooltipsTranslation("widget.fast-craft.craft.tooltips", ""))))
                .addToSub(recipeSubScreen);

        ExecutableWidget toggleLockRecipeButton = ExecutableWidget.instance(95, screen.height / 2 - 25, 24, 12)
                .setElementHandler(new ButtonElement(
                                TextProvider.of(Component.translatable("widget.fast-craft.lock")),
                                ButtonAction.run(this::toggleRecipeLock))
                        .withTooltips(TooltipHandler.of(
                                ChatUtils.parseTooltipsTranslation("widget.fast-craft.lock.tooltips", ""))))
                .addToSub(recipeSubScreen);
        Runnable toggle = HotKeyUtils.wrapFlagAsToggle("fast-craft.drop-craft", dropCraft);

        ExecutableWidget toggleDropButton = ExecutableWidget.instance(120, screen.height / 2 - 72, 24, 12)
                .setElementHandler(new ButtonElement(
                                TextProvider.of(Component.translatable("widget.fast-craft.toggle-drop")),
                                ButtonAction.run(toggle))
                        .withTooltips(TooltipHandler.of(
                                ChatUtils.parseTooltipsTranslation("widget.fast-craft.toggle-drop.tooltips", ""))))
                .addToSub(recipeSubScreen);

        DrawableWidget itemDisplay = DisplayWidget.instance(150, access.getScreenY() + 56, 18, 18)
                .setRenderHandler(new SlotElement(this::getDisplayItemStack)
                        .setSlotFrame(false)
                        .setInSlot(false))
                .addToSub(recipeSubScreen);

        DrawableWidget lockItemDisplay = DisplayWidget.instance(150 + 10, access.getScreenY() + 56 + 10, 7, 7)
                .setRenderHandler(new SlotElement(this::getLockItem)
                        .setInSlot(false)
                        .setSlotFrame(false)
                        .withRenderCondition(v -> isLock()))
                .addToSub(recipeSubScreen);

        access.addDrawableChildTo(recipeSubScreen);
        lastScreen = screen;
        lastScreenWidget = recipeSubScreen;
    }

    @Unique
    private void addInventoryButton(InventoryScreen screen) {
        HandledScreenAccess access = HandledScreenAccess.of(screen);
        SubScreenWidget recipeSubScreen = SubScreenWidget.instance(access.getScreenX(), 0, screen.width, screen.height);

        ExecutableWidget putLastRecipeButton = ExecutableWidget.instance(150, screen.height / 2 - 38, 24, 12)
                .setElementHandler(new ButtonElement(
                                TextProvider.of(Component.translatable("widget.fast-craft.craft")),
                                ButtonAction.run(() -> placeLastCraftingRecipe(screen, ScreenUtils.hasShiftDown())))
                        .withTooltips(TooltipHandler.of(
                                ChatUtils.parseTooltipsTranslation("widget.fast-craft.craft.tooltips", ""))))
                .addToSub(recipeSubScreen);

        ExecutableWidget toggleLockRecipeButton = ExecutableWidget.instance(150, screen.height / 2 - 25, 24, 12)
                .setElementHandler(new ButtonElement(
                                TextProvider.of(Component.translatable("widget.fast-craft.lock")),
                                ButtonAction.run(this::toggleRecipeLock))
                        .withTooltips(TooltipHandler.of(
                                ChatUtils.parseTooltipsTranslation("widget.fast-craft.lock.tooltips", ""))))
                .addToSub(recipeSubScreen);
        Runnable toggle = HotKeyUtils.wrapFlagAsToggle("fast-craft.drop-craft", dropCraft);

        ExecutableWidget toggleDropButton = ExecutableWidget.instance(150, screen.height / 2 - 72, 24, 12)
                .setElementHandler(new ButtonElement(
                                TextProvider.of(Component.translatable("widget.fast-craft.toggle-drop")),
                                ButtonAction.run(toggle))
                        .withTooltips(TooltipHandler.of(
                                ChatUtils.parseTooltipsTranslation("widget.fast-craft.toggle-drop.tooltips", ""))))
                .addToSub(recipeSubScreen);

        DrawableWidget itemDisplay = DisplayWidget.instance(132, access.getScreenY() + 55, 18, 18)
                .setRenderHandler(new SlotElement(this::getDisplayItemStack)
                        .setSlotFrame(false)
                        .setInSlot(false))
                .addToSub(recipeSubScreen);

        DrawableWidget lockItemDisplay = DisplayWidget.instance(132 + 10, access.getScreenY() + 55 + 10, 7, 7)
                .setRenderHandler(new SlotElement(this::getLockItem)
                        .setInSlot(false)
                        .setSlotFrame(false)
                        .withRenderCondition(v -> isLock()))
                .addToSub(recipeSubScreen);

        access.addDrawableChildTo(recipeSubScreen);
        lastScreen = screen;
        lastScreenWidget = recipeSubScreen;
    }

    private void addMerchantInformation(MerchantScreen merchantScreen) {
        var access = HandledScreenAccess.of(merchantScreen);
        access.addDrawableChildTo(new TradeInformationSubScreen(
                access.getScreenX(), access.getScreenY(), (MerchantScreen) merchantScreen));
    }
}
