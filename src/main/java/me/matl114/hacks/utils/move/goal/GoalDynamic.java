package me.matl114.hacks.utils.move.goal;

import java.util.function.Supplier;
import me.matl114.utils.MathUtils;
import net.minecraft.world.phys.Vec3;

public record GoalDynamic(Supplier<Vec3> supplier, double radius) implements IPathGoal {
    @Override
    public Vec3 sample() {
        return supplier.get();
    }

    @Override
    public boolean isInGoal(Vec3 playerPos) {
        return supplier.get().distanceToSqr(playerPos) <= MathUtils.s2(radius);
    }
}
