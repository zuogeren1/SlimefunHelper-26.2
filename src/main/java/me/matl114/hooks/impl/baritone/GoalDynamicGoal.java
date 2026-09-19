package me.matl114.hooks.impl.baritone;

import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import java.util.function.Supplier;
import net.minecraft.world.phys.Vec3;

public final class GoalDynamicGoal implements Goal {

    private final Supplier<Vec3> targetSupplier;
    private final double radius;

    public GoalDynamicGoal(Supplier<Vec3> targetSupplier, double radius) {
        this.targetSupplier = targetSupplier;
        this.radius = radius;
    }

    @Override
    public boolean isInGoal(int x, int y, int z) {
        Vec3 target = targetSupplier.get();
        if (target == null) {
            return false;
        }

        double dx = x - target.x();
        double dy = y - target.y();
        double dz = z - target.z();
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    @Override
    public double heuristic(int x, int y, int z) {
        Vec3 target = targetSupplier.get();
        if (target == null) {
            return Double.POSITIVE_INFINITY;
        }

        double dx = x - target.x();
        double dy = y - target.y();
        double dz = z - target.z();
        return GoalBlock.calculate(dx, (int) dy, dz);
    }
}
