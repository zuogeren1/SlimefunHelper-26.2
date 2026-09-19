package me.matl114.versioned.api;

import me.matl114.versioned.impl.Entity_v1_21_11;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

public interface VEntity {
    public static final VEntity INSTANCE = new Entity_v1_21_11();

    public static VEntity getInstance() {
        return INSTANCE;
    }

    public static CompoundTag saveEntityNbt(Entity entity) {
        return getInstance().serializeNBT(entity);
    }

    public CompoundTag serializeNBT(Entity entity);
}
