package me.matl114.hacks.modules.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import me.matl114.events.Event;
import me.matl114.hacks.api.ModulePath;
import me.matl114.hacks.utils.config.*;
import me.matl114.managers.Configs;
import me.matl114.managers.config.FlagRef;
import me.matl114.managers.config.NBTRef;
import me.matl114.utils.ChatUtils;
import me.matl114.utils.ItemStackUtils;
import me.matl114.utils.inventory.ItemStackSample;
import me.matl114.versioned.api.VDrawContext;
import me.matl114.versioned.api.VItem;
import net.minecraft.advancements.predicates.NbtPredicate;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemList extends IRender2DColoredModule {
    public ItemList() {
        super("ItemList");
    }

    @Override
    protected ModulePath createRoot() {
        return makePath(Configs.RENDER_CONFIG, "detect-entity.item-list");
    }

    public FlagRef collectItemFrame;

    FlagRef renderSimple;

    FlagRef renderImportant;
    public NBTRef<PrimitiveList<CompoundTag>> nbtPredicate;
    public NBTRef<EntrySet<Item>> itemType;
    public List<NbtPredicate> predicate;

    public void updatePredicate(List<CompoundTag> compound) {
        if (compound == null || compound.isEmpty()) {
            predicate = null;
        } else {
            predicate = compound.stream().map(NbtPredicate::new).toList();
        }
    }

    public boolean testItem(ItemStack stack) {
        return itemType.get().test(stack.getItem()) || (predicate != null && testItemData(stack));
    }

    private boolean testItemData(ItemStack stack) {
        DataComponentPatch changes = stack.getComponentsPatch();
        try {
            CompoundTag nbtCompound = changes.isEmpty()
                    ? new CompoundTag()
                    : (CompoundTag) DataComponentPatch.CODEC
                            .encodeStart(ItemStackUtils.registry().createSerializationContext(NbtOps.INSTANCE), changes)
                            .getOrThrow();
            for (var re : predicate) {
                if (re.matches(nbtCompound)) return true;
            }
            return false;
        } catch (Throwable e) {
            return false;
        }
    }

    @Override
    protected void initializeSettings() {
        super.initializeSettings();
        collectItemFrame = flagBuilder(hud.add("render-item-frame")).build();
        renderSimple = flagBuilder(hud.add("render-simple")).build();

        renderImportant = flagBuilder(hud.add("render-important")).build();
        nbtPredicate = builder(hud.add("nbt-predicate"), PrimitiveList.<CompoundTag>parameter())
                .defaultValue(new PrimitiveList<>(NBTTypes.NBT_COMPOUND_TYPE, List.of()))
                .updateListener(s -> updatePredicate(s.list()))
                .build();
        itemType = builder(hud.add("item-type"), EntrySet.<Item>parameter())
                .defaultValue(new EntrySet<>(
                        new Regex(
                                "^(.*ton_skull|netherite.*|.*_star|.*_apple|.*potion|tot.*|end_c.*l|obsi.*|.*anchor|expe.*|mace|ely.*|.*shulker.*|trident)$"),
                        BuiltInRegistries.ITEM))
                .build();
    }

    List<Component> simpleItems = new ArrayList<>();
    List<Component> importantItems = new ArrayList<>();

    @Override
    public void onUpdate(Event<Void> event) {
        simpleItems.clear();
        importantItems.clear();
        if (checkNull()) {
            return;
        }
        if (enable.get()) {
            Map<ItemStackSample, Integer> itemMap = new HashMap<>();
            for (var re : mc.level.entitiesForRendering()) {
                if (re instanceof ItemEntity item) {
                    ItemStack stack = item.getItem();
                    if (!stack.isEmpty()) {
                        itemMap.merge(ItemStackSample.of(stack), stack.getCount(), Integer::sum);
                    }
                } else if (re instanceof ItemFrame frame && collectItemFrame.get()) {
                    ItemStack stack = frame.getItem();
                    if (!stack.isEmpty()) {
                        itemMap.merge(ItemStackSample.of(stack), stack.getCount(), Integer::sum);
                    }
                }
            }
            for (var re : itemMap.entrySet()) {
                Component text = ChatUtils.builder()
                        .withColorString("&f")
                        .appendText(
                                VItem.getInstance().getFormattedName(re.getKey().sample()))
                        .withColorString("&f x" + re.getValue())
                        .end()
                        .build();
                if (renderImportant.get() && testItem(re.getKey().sample())) {
                    importantItems.add(text);
                    continue;
                }
                if (renderSimple.get()) {
                    simpleItems.add(text);
                }
            }
        }
    }

    @Override
    public void render2D(VDrawContext vdraw, float partialTicks) {
        if (enable.get()) {
            if (!importantItems.isEmpty() && renderImportant.get()) {
                drawText(vdraw, "重要物品:");
                for (var spec : importantItems) {
                    drawText(vdraw, spec);
                }
            }
            if (!simpleItems.isEmpty() && renderSimple.get()) {
                drawText(vdraw, "物品");
                for (var spec : simpleItems) {
                    drawText(vdraw, spec);
                }
            }
        }
    }
}
