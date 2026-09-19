package me.matl114.hacks.utils.config;

import javax.annotation.Nullable;
import me.matl114.managers.config.NBTParsable;
import me.matl114.managers.config.NBTType;
import me.matl114.utils.config.BaseAttrKeyValue;
import me.matl114.utils.config.WrapperFactory;
import me.matl114.utils.config.kv.AttrKeyValues;
import me.matl114.versioned.api.VNbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public record NBTData(Tag nbtElement) implements NBTParsable<NBTData> {
    public static final NBTType<NBTData> TYPE = new NBTType<NBTData>(
            "nbtdata",
            VNbt.CODEC.xmap(NBTData::new, NBTData::nbtElement),
            BaseAttrKeyValue.getWidgetFactory(),
            AttrKeyValues.NBT_FACTORY.concat(WrapperFactory.of(s -> new NBTData((CompoundTag) s), NBTData::nbtElement)),
            new NBTData(new CompoundTag()));

    @Override
    public NBTType<NBTData> type() {
        return TYPE;
    }

    @Nullable
    public CompoundTag compound() {
        return nbtElement instanceof CompoundTag compound ? (CompoundTag) nbtElement : null;
    }
}
