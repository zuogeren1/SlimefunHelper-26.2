package me.matl114.accessors.moonrise;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface MoonriseBlockStateBaseAccess {
    public VoxelShape moonrise$getConstantCollisionShape();

    default boolean isConstantCollisionShapeEmpty() {
        return moonrise$getConstantCollisionShape() == null
                || moonrise$getConstantCollisionShape().isEmpty();
    }

    static MoonriseBlockStateBaseAccess of(BlockBehaviour.BlockStateBase state) {
        return (MoonriseBlockStateBaseAccess) state;
    }
}
