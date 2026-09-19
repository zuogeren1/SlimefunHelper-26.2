package me.matl114.versioned.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import me.matl114.versioned.impl.Nbt_v1_21_11;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

public interface VNbt {
    VNbt INSTANCE = new Nbt_v1_21_11();

    public static VNbt getInstance() {
        return INSTANCE;
    }

    public String writeNbt(Tag element);

    public Tag readNbt(String element);

    public Tag readNbtNoRegistry(String element);

    Codec<Tag> CODEC = Codec.PASSTHROUGH.comapFlatMap(
            (dynamic) -> {
                Tag nbtElement = (Tag) dynamic.convert(NbtOps.INSTANCE).getValue();
                return DataResult.success(nbtElement == dynamic.getValue() ? nbtElement.copy() : nbtElement);
            },
            (nbt) -> {
                return new Dynamic<>(NbtOps.INSTANCE, nbt.copy());
            });
}
