package me.matl114.hacks.utils.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import me.matl114.versioned.api.VNbt;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class WorldStorage extends IStorage {
    public static final Codec<WorldStorage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Level.RESOURCE_KEY_CODEC.fieldOf("dim").forGetter(WorldStorage::getDimension),
                    Codec.unboundedMap(Codec.STRING, VNbt.CODEC)
                            .fieldOf("storage")
                            .forGetter(v -> v.storage))
            .apply(instance, WorldStorage::new));

    public WorldStorage() {
        super();
    }

    public WorldStorage(ResourceKey<Level> dimension) {
        this(dimension, new ConcurrentHashMap<>());
    }

    public WorldStorage(ResourceKey<Level> dimension, Map<String, Tag> storage) {
        super(dimension, storage);
    }
}
