package me.matl114.mixins.access;

import me.matl114.accessors.access.HitResultAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin(HitResult.class)
public abstract class HitResultMixin implements HitResultAccess {
    @Override
    @Mutable
    @Accessor("location")
    public abstract void setPos(Vec3 pos);
}
