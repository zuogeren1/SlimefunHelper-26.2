package me.matl114.hacks.modules.move;

import me.matl114.hacks.MovTasks;
import me.matl114.hacks.api.BaseModule;
import me.matl114.hacks.api.ModulePath;
import me.matl114.hacks.utils.HotKeyUtils;
import me.matl114.managers.Configs;
import me.matl114.managers.config.DoubleRef;
import me.matl114.managers.config.FlagRef;
import me.matl114.managers.config.KeyBindRef;
import me.matl114.managers.input.MultiKeyBind;
import me.matl114.utils.Debug;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class ForwardTp extends BaseModule {
    public final ModulePath quickMove = makePath(Configs.MOV_CONFIG, "quick-move");

    public ForwardTp() {
        super("ForwardTp");
    }

    public final KeyBindRef frontKey = hotkey(quickMove.add("quick-move"), new MultiKeyBind())
            .registerHotkey(HotKeyUtils.wrapAsHandler(this::quickMovFront))
            .build();

    public final KeyBindRef wallKey = hotkey(quickMove.add("quick-to-wall"), new MultiKeyBind())
            .registerHotkey(HotKeyUtils.wrapAsHandler(this::quickMovTowardsWall))
            .build();

    public final DoubleRef maxDistance = builder(quickMove.add("max-distance"), DoubleRef.TYPE)
            .defaultValue(120.0D)
            .validator(Configs.doubleRange(0.0D, 114514.0D))
            .build();

    public final FlagRef useTpMethod = builder(quickMove.add("ignore-move-collision"), FlagRef.TYPE)
            .defaultValue(true)
            .build();

    final double distance = 0.1;

    public boolean quickMovFront() {
        LocalPlayer player = mc.player;
        if (player == null) return false;
        Vec3 vec3d = player.position();
        Vec3 lookat = player.getLookAngle().normalize();
        Vec3 lastAvailablePos = calculateAvailableMovPlace(mc.player, vec3d, lookat, distance, maxDistance.get());
        //        DEBUG_RENDER_COLLISION_RENDERING = true;
        //        STATIC_DEBUG_COLOR = Color.RED;
        //        RenderTasks.debugBox(mc.player.dimensions.getBoxAt(lastAvailablePos));
        //        STATIC_DEBUG_COLOR = Color.YELLOW;
        //        collide(mc.player, lastAvailablePos.subtract(vec3d));
        //        DEBUG_RENDER_COLLISION_RENDERING = false;

        if (lastAvailablePos != vec3d) {
            if (useTpMethod.get()) {
                MovTasks.executeTp(lastAvailablePos, 2147483647, false, true);
            } else {
                MovTasks.farawayMoveTo(lastAvailablePos, true);
            }
            return true;
        } else {
            Debug.chat("no available position in front of you!");
            return true;
        }
    }

    public boolean quickMovTowardsWall() {
        LocalPlayer player = mc.player;
        if (player == null) return false;
        Vec3 vec3d = player.position();
        Vec3 lookat = player.getLookAngle().normalize();
        Vec3 lastAvailablePos = calculateNextWallPosition(mc.player, vec3d, lookat, distance, maxDistance.get());
        if (lastAvailablePos != vec3d) {
            if (useTpMethod.get()) {
                MovTasks.executeTp(lastAvailablePos, 2147483647, false, true);
            } else {
                MovTasks.farawayMoveTo(lastAvailablePos, true);
            }
            return true;
        } else {
            Debug.chat("no available position in front of you!");
            return true;
        }
    }

    public Vec3 calculateAvailableMovPlace(Entity executor, Vec3 curPose, Vec3 lookAt, double delta, double max) {
        //        double stepHeight = executor.getStepHeight();
        //        boolean onGround = executor.isOnGround();
        lookAt = lookAt.normalize();
        Vec3 maxinumMovement = lookAt.scale(max);
        // fixme: when max too high , creating cache costs too much
        // fixme: should in lower case and higher case when searching i
        // fixme: most of case we move less than 100
        MovTasks.CollisionContext engin =
                new MovTasks.CollisionCache(executor, curPose, curPose.add(maxinumMovement), false);
        //        Box originalBox = executor.dimensions.getBoxAt(curPose);
        //        Box originalBox = executor.getBoundingBox();
        //        Box bigBox = makeCollectorBoxInvolvingCollision(originalBox, maxinumMovement, stepHeight, onGround);
        //        List<VoxelShape> involvedVoxel = new ArrayList<>();
        //        List<Box> involvedAABB = new ArrayList<>();
        //        CollisionUtil.getCollisions(
        //            executor.getWorld(), executor, bigBox, involvedVoxel, involvedAABB,
        //            CollisionUtil.COLLISION_FLAG_CHECK_BORDER,
        //            null, null, null
        //        );

        lookAt = lookAt.scale(delta);
        Vec3 originalPos = curPose;
        Vec3 lastAvailablePos = curPose;
        boolean hasWall = false;
        for (double i = 0; i < max; i += delta) {
            curPose = curPose.add(lookAt);
            //            BlockPos pos1= BlockPos.ofFloored(vec3d);
            //            BlockPos pos2 = pos1.up();
            // check
            boolean noCollision = useTpMethod.get();
            boolean value;
            if (noCollision) {
                value = true;
            } else {
                Vec3 totalMovement = curPose.subtract(originalPos);
                Vec3 sim = engin.simulateMovement(
                        executor,
                        originalPos,
                        totalMovement); // collideWithTrustedList(originalBox, totalMovement, involvedVoxel,
                // involvedAABB, stepHeight, onGround);
                value = MovTasks.validMovementAsServer(totalMovement, sim);
            }

            boolean pass = value && !engin.checkEnvironmentCollision(executor, curPose, true);

            if (pass) {
                lastAvailablePos = curPose;
                if (hasWall) {
                    break;
                } else {
                    continue;
                }
            } else {
                hasWall = true;
            }
        }
        return lastAvailablePos;
    }

    public static Vec3 calculateNextWallPosition(Entity executor, Vec3 curPose, Vec3 lookAt, double delta, double max) {
        //        double stepHeight = executor.getStepHeight();
        //        boolean onGround = executor.isOnGround();
        lookAt = lookAt.normalize();
        Vec3 maxinumMovement = lookAt.scale(max);
        //        Box originalBox = executor.getBoundingBox();
        //        Box bigBox = makeCollectorBoxInvolvingCollision(originalBox, maxinumMovement, stepHeight, onGround);
        //        List<VoxelShape> involvedVoxel = new ArrayList<>();
        //        List<Box> involvedAABB = new ArrayList<>();
        //        CollisionUtil.getCollisions(
        //            executor.getWorld(), executor, bigBox, involvedVoxel, involvedAABB,
        //            CollisionUtil.COLLISION_FLAG_CHECK_BORDER,
        //            null, null, null
        //        );
        MovTasks.CollisionContext engin =
                new MovTasks.CollisionCache(executor, curPose, curPose.add(maxinumMovement), false);

        lookAt = lookAt.scale(delta);
        Vec3 originalPos = curPose;
        Vec3 lastAvailablePos = curPose;
        for (double i = 0; i < max; i += delta) {
            curPose = curPose.add(lookAt);
            //            BlockPos pos1= BlockPos.ofFloored(vec3d);
            //            BlockPos pos2 = pos1.up();
            // check

            boolean value;

            Vec3 totalMovement = curPose.subtract(originalPos);
            Vec3 sim = engin.simulateMovement(
                    executor,
                    originalPos,
                    totalMovement); // collideWithTrustedList(originalBox, totalMovement, involvedVoxel, involvedAABB,
            // stepHeight, onGround);
            value = MovTasks.validMovementAsServer(totalMovement, sim);

            boolean pass = value && !engin.checkEnvironmentCollision(executor, curPose, true);

            if (pass) {
                lastAvailablePos = curPose;
            } else {
                break;
            }
        }
        return lastAvailablePos;
    }
}
