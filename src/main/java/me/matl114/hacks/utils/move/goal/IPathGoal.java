package me.matl114.hacks.utils.move.goal;

import net.minecraft.world.phys.Vec3;

public sealed interface IPathGoal
        permits GoalBlockPos, GoalDirection, GoalDynamic, GoalFollow, GoalList, GoalNear, GoalNearBlockPos {
    public Vec3 sample();

    public boolean isInGoal(Vec3 playerPos);
}
