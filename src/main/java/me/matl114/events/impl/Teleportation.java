package me.matl114.events.impl;

import net.minecraft.world.phys.Vec3;

public record Teleportation(int teleportId, double x, double y, double z, float pitch, float yaw) {
    public Vec3 vec3d() {
        return new Vec3(x, y, z);
    }
}
