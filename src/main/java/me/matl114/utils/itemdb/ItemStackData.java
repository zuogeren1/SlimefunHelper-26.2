package me.matl114.utils.itemdb;

import com.google.gson.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import java.util.Objects;
import javax.annotation.Nonnull;
import me.matl114.utils.CustomItemStackBuilder;
import me.matl114.utils.Debug;
import me.matl114.utils.ItemStackUtils;
import me.matl114.versioned.api.VItem;
import me.matl114.versioned.api.VNbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtException;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public interface ItemStackData {
    public JsonElement getAsJson();

    public void resolveItemStack();

    public boolean isValid();

    public ItemStack getItemStack();

    public ItemStack getIcon();

    /** 26.2: ItemStack 必须在组件绑定之后构造。接口字段不能是可变 private static，
     * 因此用嵌套持有类做惰性缓存。 */
    final class Icons {
        static ItemStack failure = null;
        static ItemStack missing = null;
    }

    public static ItemStack failure() {
        if (Icons.failure == null) {
            Icons.failure = CustomItemStackBuilder.builder()
                    .type(Items.BARRIER)
                    .amount(1)
                    .name("&c物品解析失败")
                    .lore()
                    .append("")
                    .append("&7详细信息请检查日志")
                    .endLore()
                    .build();
        }
        return Icons.failure;
    }

    public static ItemStack missing() {
        if (Icons.missing == null) {
            Icons.missing = CustomItemStackBuilder.builder()
                    .type(Items.STRUCTURE_VOID)
                    .amount(1)
                    .name("&c物品索引缺失")
                    .lore()
                    .append("")
                    .append("&7请修复item-database.json")
                    .endLore()
                    .build();
        }
        return Icons.missing;
    }

    public static ItemStack deserialize(JsonElement json) {
        if (json.isJsonObject()) {
            JsonObject jsonMap = json.getAsJsonObject();
            try {
                ItemStack stack = VItem.getInstance()
                        .getVersionedCodec()
                        .decode(ItemStackUtils.registry().createSerializationContext(JsonOps.INSTANCE), jsonMap)
                        .getOrThrow()
                        .getFirst();
                return stack.isEmpty() ? ItemStack.EMPTY : stack;
            } catch (Throwable e) {
                throw new JsonParseException(e);
            }
        } else {
            String jsonString = json.getAsString();
            try {
                CompoundTag nbtElement = (CompoundTag) VNbt.getInstance().readNbt(jsonString);
                ItemStack stack = VItem.getInstance()
                        .getVersionedCodec()
                        .decode(ItemStackUtils.registry().createSerializationContext(NbtOps.INSTANCE), nbtElement)
                        .getOrThrow()
                        .getFirst();
                return stack.isEmpty() ? ItemStack.EMPTY : stack;
            } catch (Throwable e) {
                throw new NbtException(e.getMessage());
            }
        }
    }

    public static JsonElement serialize(ItemStack stack) {
        if (stack.isEmpty()) {
            return JsonNull.INSTANCE;
        } else {
            CompoundTag nbt = VItem.getInstance().toNbt(stack, ItemStackUtils.registry());
            return new JsonPrimitive(VNbt.getInstance().writeNbt(nbt));
        }
    }

    public static ItemStackData wrapRaw(ItemStack stack) {
        if (stack.isEmpty()) {
            return EMPTY;
        } else {
            return new Wrapper(stack);
        }
    }

    public static ItemStackData wrapCopy(ItemStack stack) {
        if (stack.isEmpty()) {
            return EMPTY;
        } else {
            return new Wrapper(stack.copyWithCount(1));
        }
    }

    public static ItemStackData wrapAsData(ItemStack stack) {
        if (stack.isEmpty()) {
            return EMPTY;
        } else {
            stack = stack.copyWithCount(1);
            JsonElement element = serialize(stack);
            return new DataSource(element, stack);
        }
    }

    public static final ItemStackData EMPTY = new ItemStackData() {

        @Override
        public JsonElement getAsJson() {
            return JsonNull.INSTANCE;
        }

        @Override
        public void resolveItemStack() {}

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public ItemStack getItemStack() {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack getIcon() {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this
                    || (obj instanceof ItemStackData data
                            && data.isValid()
                            && data.getItemStack().isEmpty());
        }

        private static final int EMPTY_HASHCODE = ItemStack.hashItemAndComponents(ItemStack.EMPTY);

        @Override
        public int hashCode() {
            return EMPTY_HASHCODE;
        }
    };

    public static final class DataSource implements ItemStackData {
        JsonElement jsonRaw;
        ItemStack stack;
        boolean resolve = false;
        boolean valid = false;
        Integer cachedHash;

        public DataSource(@Nonnull JsonElement jsonRaw) {
            this.jsonRaw = jsonRaw;
        }

        private DataSource(@Nonnull JsonElement jsonRaw, ItemStack itemStack) {
            this.jsonRaw = jsonRaw;
            this.stack = itemStack;
            this.resolve = true;
            this.valid = true;
        }

        @Override
        public JsonElement getAsJson() {
            return jsonRaw;
        }

        @Override
        public void resolveItemStack() {
            if (!resolve) {
                resolve = true;
                try {
                    stack = ItemStackData.deserialize(this.jsonRaw).copyWithCount(1);
                    valid = true;
                } catch (Throwable e) {
                    try {
                        // it may throw exception if registry is not valid, at that time, do not print message and
                        // jsonRaw
                        ItemStackUtils.registry();
                        if (false) {
                            String jsonMsg = jsonRaw.toString();
                            Debug.info(
                                    "Error while resolving ItemStackData:",
                                    jsonMsg.length() > 256 ? jsonMsg.substring(0, 256) : jsonMsg);
                            String errMsg = e.getMessage();
                            Debug.info(
                                    "Caused by:",
                                    errMsg == null || errMsg.length() < 256 ? errMsg : errMsg.substring(0, 256));
                        }

                    } catch (Throwable t) {
                        // do not print anything
                    }
                    valid = false;
                }
            }
        }

        @Override
        public boolean isValid() {
            resolveItemStack();
            return valid;
        }

        @Override
        public ItemStack getItemStack() {
            resolveItemStack();
            if (valid) {
                return stack;
            } else {
                throw new UnsupportedOperationException("Invalid data");
            }
        }

        @Override
        public int hashCode() {
            if (cachedHash == null) {
                resolveItemStack();
                if (valid) {
                    cachedHash = ItemStack.hashItemAndComponents(stack);
                } else {
                    cachedHash = jsonRaw.hashCode();
                }
            }
            return cachedHash;
        }

        @Override
        public ItemStack getIcon() {
            resolveItemStack();
            if (valid) {
                return stack;
            } else {
                return failure();
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (obj instanceof ItemStackData data) {
                boolean v1 = valid;
                boolean v2 = data.isValid();
                if (v1 && v2) {
                    return ItemStack.isSameItemSameComponents(stack, data.getItemStack());
                } else if (!v1 && !v2) {
                    return Objects.equals(jsonRaw, data.getAsJson());
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    public static final class Wrapper implements ItemStackData {
        ItemStack itemStack;
        Integer cachedHash;
        JsonElement cachedJson;

        public Wrapper(ItemStack itemStack) {
            this.itemStack = itemStack;
        }

        @Override
        public JsonElement getAsJson() {
            if (cachedJson == null) {
                try {
                    cachedJson = serialize(this.itemStack);
                } catch (Throwable e) {
                    cachedJson = JsonNull.INSTANCE;
                }
            }
            return cachedJson;
        }

        @Override
        public void resolveItemStack() {
            // no
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public ItemStack getItemStack() {
            return itemStack;
        }

        @Override
        public ItemStack getIcon() {
            return itemStack;
        }

        @Override
        public int hashCode() {
            if (cachedHash == null) {
                cachedHash = ItemStack.hashItemAndComponents(itemStack);
            }
            return cachedHash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (obj instanceof ItemStackData data) {
                if (data.isValid()) {
                    return ItemStack.isSameItemSameComponents(itemStack, data.getItemStack());
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    public static final record Missing(String customId) implements ItemStackData {

        @Override
        public JsonElement getAsJson() {
            return JsonNull.INSTANCE;
        }

        @Override
        public void resolveItemStack() {}

        @Override
        public boolean isValid() {
            return false;
        }

        @Override
        public ItemStack getItemStack() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ItemStack getIcon() {
            return missing();
        }

        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (obj instanceof Missing ms) {
                return ms.customId.equals(customId);
            } else if (obj instanceof ItemStackData md) {
                return false;
            }
            return false;
        }
    }

    public Codec<ItemStackData> CODEC = Codec.PASSTHROUGH.xmap(
            (dynamic) -> {
                JsonElement jsonElement = dynamic.convert(JsonOps.INSTANCE).getValue();
                if (jsonElement.isJsonNull()) {
                    return EMPTY;
                } else {
                    return new DataSource(jsonElement);
                }
            },
            (data) -> {
                JsonElement jsonElement = data.getAsJson();
                return new Dynamic<>(JsonOps.INSTANCE, jsonElement);
            });
}
