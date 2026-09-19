package me.matl114.versioned.impl;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.matl114.utils.ItemStackUtils;
import me.matl114.versioned.api.VNbt;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTagVisitor;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;

public class Nbt_v1_21_11 implements VNbt {
    @Override
    public String writeNbt(Tag element) {
        StringTagVisitor writer = new StringTagVisitor();
        element.accept(writer);
        return writer.build();
    }

    @Override
    public Tag readNbt(String element) {
        try {
            return TagParser.create(ItemStackUtils.registry().createSerializationContext(NbtOps.INSTANCE))
                    .parseFully(element);
        } catch (CommandSyntaxException e) {
            throw new RuntimeException("Could not deserialize found element ", e);
        }
    }

    @Override
    public Tag readNbtNoRegistry(String element) {
        try {
            return TagParser.create(NbtOps.INSTANCE).parseFully(element);
        } catch (CommandSyntaxException e) {
            throw new RuntimeException("Could not deserialize found element ", e);
        }
    }
}
