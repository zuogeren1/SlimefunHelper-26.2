package me.matl114.accessors.access;

import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public interface HitResultAccess {
    public abstract void setPos(Vec3 pos);

    public static HitResultAccess of(HitResult hitResult) {
        return (HitResultAccess) hitResult;
    }
}
