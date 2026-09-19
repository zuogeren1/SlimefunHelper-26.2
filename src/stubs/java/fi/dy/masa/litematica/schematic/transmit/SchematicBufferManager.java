package fi.dy.masa.litematica.schematic.transmit;

import fi.dy.masa.litematica.util.FileType;
import net.minecraft.nbt.CompoundTag;

public class SchematicBufferManager {

    public void createBuffer(
            String name,
            int totalExpectedSlices,
            long totalExpectedSize,
            FileType type,
            final long sessionKey,
            CompoundTag optional) {}

    public void createBuffer(
            int totalExpectedSlices,
            long totalExpectedSize,
            FileType type,
            final long sessionKey,
            CompoundTag optional) {}
}
