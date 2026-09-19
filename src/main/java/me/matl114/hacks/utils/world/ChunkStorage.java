package me.matl114.hacks.utils.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import me.matl114.versioned.api.VNbt;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

@Getter
public class ChunkStorage extends IStorage {
    public static final Codec<ChunkStorage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Level.RESOURCE_KEY_CODEC.fieldOf("dim").forGetter(ChunkStorage::getDimension),
                    ChunkPos.CODEC.fieldOf("chunk-pos").forGetter(ChunkStorage::getChunkPos),
                    Codec.unboundedMap(Codec.STRING, VNbt.CODEC)
                            .fieldOf("storage")
                            .forGetter(v -> v.storage))
            .apply(instance, ChunkStorage::new));
    public final ChunkPos chunkPos;

    public ChunkStorage(final ChunkPos chunkPos) {
        super();
        this.chunkPos = chunkPos;
    }

    public ChunkStorage(ResourceKey<Level> dimension, ChunkPos chunkPos) {
        this(dimension, chunkPos, new ConcurrentHashMap<>());
    }

    public ChunkStorage(ResourceKey<Level> dimension, ChunkPos chunkPos, Map<String, Tag> storage) {
        super(dimension, storage);
        this.chunkPos = chunkPos;
    }
}
