package me.matl114.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;

@Getter
@Setter
@Accessors(fluent = true, chain = true)
@AllArgsConstructor
public class MetadataUpdate {
    final Entity entity;
    SynchedEntityData.DataValue<?> metadata;
}
