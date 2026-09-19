package me.matl114.hacks.utils.move.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public record GoalNear(Vec3 center, double radius) implements IPathGoal {
    public GoalNear(BlockPos center, double radius) {
        this(Vec3.atBottomCenterOf(center), radius + 0.5);
    }

    @Override
    public Vec3 sample() {
        return center;
    }

    @Override
    public boolean isInGoal(Vec3 playerPos) {
        Vec3 delta = playerPos.subtract(center);
        return Math.abs(delta.x) + Math.abs(delta.y) + Math.abs(delta.z) <= radius;
    }
}
