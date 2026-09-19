package me.matl114.mixins.access;

import java.util.OptionalInt;
import me.matl114.accessors.access.FireworkRocketEntityAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Environment(EnvType.CLIENT)
@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketEntityMixin extends Entity implements FireworkRocketEntityAccess {
    @Shadow
    @Final
    private static EntityDataAccessor<OptionalInt> DATA_ATTACHED_TO_TARGET;

    @Shadow
    private int life;

    public FireworkRocketEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Unique
    public boolean isFallFlyingAccelerator() {
        return this.entityData.get(DATA_ATTACHED_TO_TARGET).isPresent();
    }

    @Unique
    public int getLiveTicks() {
        return this.life;
    }
}
