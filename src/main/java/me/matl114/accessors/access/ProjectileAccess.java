package me.matl114.accessors.access;

import java.util.OptionalInt;
import net.minecraft.world.entity.projectile.Projectile;

public interface ProjectileAccess {
    OptionalInt getOwnerEid();

    static ProjectileAccess of(Projectile projectile) {
        return (ProjectileAccess) projectile;
    }
}
