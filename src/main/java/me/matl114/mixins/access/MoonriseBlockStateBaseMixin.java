package me.matl114.mixins.access;

import me.matl114.accessors.moonrise.MoonriseBlockStateBaseAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class MoonriseBlockStateBaseMixin implements MoonriseBlockStateBaseAccess {
    @Unique
    private VoxelShape constantCollisionShape;

    @Shadow
    public abstract VoxelShape getCollisionShape(BlockGetter world, BlockPos pos, CollisionContext context);

    @Unique
    private void initCache0() {
        try {
            constantCollisionShape = getCollisionShape(null, null, null);
        } catch (Throwable e) {
            constantCollisionShape = null;
        }
    }

    @Inject(method = "initCache", at = @At("RETURN"))
    public void onInitCache(CallbackInfo ci) {
        initCache0();
    }

    @Unique
    public VoxelShape moonrise$getConstantCollisionShape() {
        return this.constantCollisionShape;
    }
}
