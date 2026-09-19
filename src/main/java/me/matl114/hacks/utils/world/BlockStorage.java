package me.matl114.hacks.utils.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import me.matl114.versioned.api.VNbt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

@Getter
public class BlockStorage extends IStorage {
    public static final Codec<BlockStorage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Identifier.CODEC
                            .xmap(BuiltInRegistries.BLOCK::getValue, BuiltInRegistries.BLOCK::getKey)
                            .fieldOf("type")
                            .forGetter(BlockStorage::getType),
                    Level.RESOURCE_KEY_CODEC.fieldOf("dim").forGetter(BlockStorage::getDimension),
                    BlockPos.CODEC.fieldOf("pos").forGetter(BlockStorage::getPos),
                    Codec.unboundedMap(Codec.STRING, VNbt.CODEC)
                            .fieldOf("storage")
                            .forGetter(v -> v.storage))
            .apply(instance, BlockStorage::new));
    public Block type;
    public final BlockPos pos;

    public void setType(Block type) {
        if (this.type == type) return;
        this.type = type;
        dirty = true;
    }

    public BlockStorage(BlockPos pos) {
        super();
        this.pos = pos;
        this.type = mc.level.getBlockState(pos).getBlock();
    }

    public BlockStorage(ResourceKey<Level> dimension, BlockPos pos) {
        this(Blocks.AIR, dimension, pos);
    }

    public BlockStorage(final Block type, final ResourceKey<Level> dimension, final BlockPos pos) {
        this(type, dimension, pos, new ConcurrentHashMap<>());
    }

    public BlockStorage(
            final Block type, final ResourceKey<Level> dimension, final BlockPos pos, Map<String, Tag> storage) {
        super(dimension, storage);
        this.type = type;

        this.pos = pos;
    }
}
