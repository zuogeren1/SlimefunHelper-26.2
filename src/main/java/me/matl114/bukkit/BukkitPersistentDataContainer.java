package me.matl114.bukkit;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public class BukkitPersistentDataContainer {
    public Map<String, Tag> container = new HashMap<>();

    public BukkitPersistentDataContainer() {}

    public void putData(Map<String, Tag> container) {
        this.container.putAll(container);
    }

    public void putData(CompoundTag compound) {
        for (String key : compound.keySet()) {
            this.container.put(key, compound.get(key));
        }
    }

    public CompoundTag toCompound() {
        CompoundTag compound = new CompoundTag();
        for (String key : container.keySet()) {
            compound.put(key, container.get(key));
        }
        return compound;
    }
}
