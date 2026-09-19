package me.matl114.hacks.utils.move.goal;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public record GoalBlockPos(BlockPos pos) implements IPathGoal {
    @Override
    public Vec3 sample() {
        return Vec3.atBottomCenterOf(pos);
    }

    @Override
    public boolean isInGoal(Vec3 playerPos) {
        return Objects.equals(BlockPos.containing(playerPos), pos);
    }
}
