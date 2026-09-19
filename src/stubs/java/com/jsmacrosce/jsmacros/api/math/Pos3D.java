package com.jsmacrosce.jsmacros.api.math;

import net.minecraft.world.phys.Vec3;

public class Pos3D extends Pos2D {
    public double z;

    public Pos3D(Vec3 vec) {
        this(vec.x(), vec.y(), vec.z());
    }

    public Pos3D(double x, double y, double z) {
        super(x, y);
        this.z = z;
    }

    public double getZ() {
        return z;
    }
}
