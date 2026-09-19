package me.matl114.mixins.access;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import me.matl114.accessors.moonrise.MoonriseVoxelShapeAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.phys.shapes.ArrayVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(value = ArrayVoxelShape.class, priority = 2000)
public abstract class MoonriseArrayVoxelShapeMixin extends VoxelShape {
    protected MoonriseArrayVoxelShapeMixin(DiscreteVoxelShape voxels) {
        super(voxels);
    }

    @Inject(
            method =
                    "<init>(Lnet/minecraft/world/phys/shapes/DiscreteVoxelShape;Lit/unimi/dsi/fastutil/doubles/DoubleList;Lit/unimi/dsi/fastutil/doubles/DoubleList;Lit/unimi/dsi/fastutil/doubles/DoubleList;)V",
            at = @At("RETURN"))
    private void moonriseinitCache(
            DiscreteVoxelShape shape, DoubleList xPoints, DoubleList yPoints, DoubleList zPoints, CallbackInfo ci) {
        MoonriseVoxelShapeAccess.of(this).moonrise$initCache();
    }
}
