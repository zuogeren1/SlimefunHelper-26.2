package me.matl114.hacks.utils.world;

import com.mojang.serialization.Codec;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class IStorage {
    @Getter
    public final ResourceKey<Level> dimension;

    public final Map<String, Tag> storage;

    @Getter
    @Setter
    public boolean dirty = false;

    protected static final Minecraft mc = Minecraft.getInstance();

    public IStorage() {
        this(mc.level.dimension(), null);
    }

    public IStorage(ResourceKey<Level> dimension) {
        this(dimension, null);
    }

    public IStorage(ResourceKey<Level> dimension, Map<String, Tag> storage) {
        this.dimension = dimension;
        this.storage = storage == null ? new ConcurrentHashMap<>() : new ConcurrentHashMap<>(storage);
    }

    public Tag get(String key) {
        return storage.get(key);
    }

    public <T> T get(String key, Codec<T> codec) {
        var re = get(key);
        return re == null ? null : resultOrNull(re, codec);
    }

    public <T> T get(String key, Codec<T> codec, HolderLookup.Provider lookup) {
        var re = get(key);
        return re == null ? null : resultOrNull(re, codec, lookup);
    }

    public static <T> T resultOrNull(Tag re, Codec<T> codec) {
        var tmp = codec.decode(NbtOps.INSTANCE, re);
        return tmp.isSuccess() ? tmp.getOrThrow().getFirst() : null;
    }

    public static <T> T resultOrNull(Tag re, Codec<T> codec, HolderLookup.Provider lookup) {
        var tmp = codec.decode(lookup.createSerializationContext(NbtOps.INSTANCE), re);
        return tmp.isSuccess() ? tmp.getOrThrow().getFirst() : null;
    }

    public void put(String key, Tag value) {
        if (value == null) {
            if (this.storage.remove(key) != null) {
                dirty = true;
            }
        } else {
            this.storage.put(key, value);
            dirty = true;
        }
    }

    public <T> void put(String key, T val, Codec<T> codec) {
        if (val == null) {
            put(key, null);
        } else {
            put(key, codec.encodeStart(NbtOps.INSTANCE, val).getOrThrow());
        }
    }

    public <T> void put(String key, T val, Codec<T> codec, HolderLookup.Provider lookup) {
        if (val == null) {
            put(key, null);
        } else {
            put(
                    key,
                    codec.encodeStart(lookup.createSerializationContext(NbtOps.INSTANCE), val)
                            .getOrThrow());
        }
    }

    public boolean contains(String key) {
        return storage.containsKey(key);
    }

    public boolean isEmpty() {
        return storage.isEmpty();
    }

    public boolean nonEmpty() {
        return !storage.isEmpty();
    }
}
