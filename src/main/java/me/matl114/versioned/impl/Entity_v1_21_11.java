package me.matl114.versioned.impl;

import me.matl114.versioned.api.VEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.TagValueOutput;

public class Entity_v1_21_11 implements VEntity {
    @Override
    public CompoundTag serializeNBT(Entity entity) {
        var writeView = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        entity.saveWithoutId(writeView);
        return writeView.buildResult();
    }
}
