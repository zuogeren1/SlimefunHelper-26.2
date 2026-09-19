package me.matl114.hacks.utils.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import me.matl114.versioned.api.VNbt;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;

@Getter
public class EntityStorage extends IStorage {
    public final UUID uuid;
    public static final Codec<EntityStorage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    UUIDUtil.AUTHLIB_CODEC.fieldOf("uuid").forGetter(EntityStorage::getUuid),
                    Codec.unboundedMap(Codec.STRING, VNbt.CODEC)
                            .fieldOf("storage")
                            .forGetter(v -> v.storage))
            .apply(instance, EntityStorage::new));

    public EntityStorage(UUID uuid) {
        this(uuid, null);
    }

    public EntityStorage(UUID uuid, Map<String, Tag> elementMap) {
        super(Level.OVERWORLD, elementMap);
        this.uuid = uuid;
    }
}
