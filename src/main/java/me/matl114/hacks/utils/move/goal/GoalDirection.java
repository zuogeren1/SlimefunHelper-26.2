package me.matl114.hacks.utils.move.goal;

import me.matl114.hacks.utils.EntityUtils;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public record GoalDirection(float yaw) implements IPathGoal {
    public GoalDirection(Direction direction) {
        this(EntityUtils.directionToPitchYaw(direction).y);
    }

    @Override
    public Vec3 sample() {
        return null;
    }

    @Override
    public boolean isInGoal(Vec3 playerPos) {
        return false;
    }
}
