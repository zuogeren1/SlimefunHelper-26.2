package me.matl114.utils;

import com.mojang.serialization.Codec;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

public class NBTUtils {
    public static Tag getOrDefault(@Nonnull CompoundTag element, String key, Tag defaultValue) {
        return element.tags.getOrDefault(key, defaultValue);
    }

    public static void putIfAbsent(@Nonnull CompoundTag element, String key, Tag value) {
        element.tags.putIfAbsent(key, value);
    }

    public static Tag getOrCreate(@Nonnull CompoundTag element, String key, Supplier<Tag> defaultValue) {
        return element.tags.computeIfAbsent(key, (k) -> defaultValue.get());
    }

    public static Tag computeIfAbsent(
            @Nonnull CompoundTag element, String key, Function<String, Tag> defaultValue) {
        return element.tags.computeIfAbsent(key, defaultValue);
    }

    public static CompoundTag ensurePath(@Nonnull CompoundTag tag, String path) {
        String[] value = path.split("\\.");
        CompoundTag current = tag;
        for (int i = 0; i < value.length; i++) {
            current = (CompoundTag) current.tags.compute(value[i], (k, v) -> {
                if (v instanceof CompoundTag nbtCompound) {
                    return nbtCompound;
                } else {
                    return new CompoundTag();
                }
            });
        }
        return current;
    }

    @Nullable
    public static Tag resolvePath(@Nonnull CompoundTag tag, String path) {
        String[] value = path.split("\\.");
        return resolve(tag, value);
    }

    public static Tag resolve(@Nonnull CompoundTag tag, String... value) {
        CompoundTag current = tag;
        for (int i = 0; i < value.length - 1; i++) {
            if (current.get(value[i]) instanceof CompoundTag nbtCompound) {
                current = nbtCompound;
            } else {
                return null;
            }
        }
        return current.get(value[value.length - 1]);
    }

    public static <W> void putValue(
            @Nonnull CompoundTag tag, String key, W value, Codec<W> codec, HolderLookup.Provider lookup) {
        tag.store(key, codec, lookup.createSerializationContext(NbtOps.INSTANCE), value);
    }

    public static <W> void putValue(@Nonnull CompoundTag tag, String key, W value, Codec<W> codec) {
        tag.store(key, codec, NbtOps.INSTANCE, value);
    }

    public static <W> W getValue(@Nonnull CompoundTag tag, String key, Codec<W> codec) {
        return tag.read(key, codec).orElse(null);
    }

    public static <W> W getValue(
            @Nonnull CompoundTag tag, String key, Codec<W> codec, HolderLookup.Provider lookup) {
        return tag.read(key, codec, lookup.createSerializationContext(NbtOps.INSTANCE)).orElse(null);
    }

    public static <W> W toValue(Tag nbtElement, Codec<W> codec) {
        var re = codec.parse(NbtOps.INSTANCE, nbtElement);
        return re.isSuccess() ? re.getOrThrow() : null;
    }

    public static <W> W toValue(Tag nbtElement, Codec<W> codec, HolderLookup.Provider lookup) {
        var re = codec.parse(lookup.createSerializationContext(NbtOps.INSTANCE), nbtElement);
        return re.isSuccess() ? re.getOrThrow() : null;
    }
}
