package me.matl114.mixins.access;

import me.matl114.accessors.moonrise.MoonriseVoxelShapeAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import net.minecraft.world.phys.shapes.SliceShape;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(value = SliceShape.class, priority = 2000)
public abstract class MoonriseSliceShapeMixin extends VoxelShape {
    protected MoonriseSliceShapeMixin(DiscreteVoxelShape voxels) {
        super(voxels);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void moonriseinitCache(VoxelShape shape, Direction.Axis axis, int sliceWidth, CallbackInfo ci) {
        MoonriseVoxelShapeAccess.of(this).moonrise$initCache();
    }
}
