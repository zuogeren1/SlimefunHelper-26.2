package me.matl114.hacks.utils.move.goal;

import me.matl114.utils.MathUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public record GoalFollow(Entity entity) implements IPathGoal {
    @Override
    public Vec3 sample() {
        return entity.position();
    }

    @Override
    public boolean isInGoal(Vec3 playerPos) {
        return entity.position().distanceToSqr(playerPos)
                <= MathUtils.s2(0.3 + (entity.getDimensions(entity.getPose()).width() / 2));
    }
}
