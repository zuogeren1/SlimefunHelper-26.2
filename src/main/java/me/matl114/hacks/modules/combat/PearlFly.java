package me.matl114.hacks.modules.combat;

import java.util.Objects;
import me.matl114.events.Event;
import me.matl114.events.Listener;
import me.matl114.hacks.api.BaseModule;
import me.matl114.hacks.api.ModulePath;
import me.matl114.hacks.modules.inv.InvExtra;
import me.matl114.hacks.modules.move.PlayerStateManager;
import me.matl114.hacks.utils.entity.EntityMovementStatus;
import me.matl114.managers.Configs;
import me.matl114.managers.config.DoubleRef;
import me.matl114.managers.config.FlagRef;
import me.matl114.managers.config.KeyBindRef;
import me.matl114.managers.input.MultiKeyBind;
import me.matl114.utils.*;
import me.matl114.utils.entity.PlayerInputUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class PearlFly extends BaseModule {
    public PearlFly() {
        super("PearlFly");
    }

    public final ModulePath combatUtils = makePath(Configs.COMBAT_CONFIG, "combat-utils");
    public final ModulePath pearl = combatUtils.add("pearl-fly");

    public final ModulePath pearlPhase = combatUtils.add("pearl-phase");
    public final FlagRef enablePearlPhase = flagBuilder(pearlPhase.addEnable()).build();

    public final KeyBindRef hotkey = moduleEntry(pearlPhase.addHotkey(), new MultiKeyBind(), pearlPhase.addEnable())
            .build();

    public final FlagRef enableCrawl =
            flagBuilder(pearlPhase.add("enable-crawl")).build();

    public final FlagRef enableStand =
            flagBuilder(pearlPhase.add("enable-stand")).build();

    public final FlagRef autoCrawl = flagBuilder(pearlPhase.add("auto-crawl")).build();

    public final DoubleRef autoPearlActivateRange = doubleBuilder(pearlPhase.add("auto-activate-range"))
            .defaultValue(0.35)
            .build();

    public final FlagRef useWASDControl =
            flagBuilder(pearlPhase.add("use-wasd-control")).build();

    public final FlagRef enableJump = builder(pearlPhase.add("enable-jump-up"), Boolean.class)
            .defaultValue(true)
            .build();

    public final FlagRef offhand = flagBuilder(pearl.add("offhand")).build();

    @Override
    public void registerAll() {
        super.registerAll();
        registerListener(Listener.getPreHandleInputEvents(), this::onInputEvent);
    }

    public void onInputEvent(Event<Void> event) {
        if (checkNull()) return;
        if (enablePearlPhase.get()) {
            var pose = mc.player.getPose();
            if (pose == Pose.SWIMMING) {
                if (!enableCrawl.get()) {
                    return;
                }
            } else {
                if (!enableStand.get()) {
                    return;
                }
            }
            if (mc.player.getCooldowns().isOnCooldown(new ItemStack(Items.ENDER_PEARL))) {
                return;
            }
            Direction direction = mc.player.getDirection();
            Vec3 pos = Vec3.atCenterOf(mc.player.blockPosition());
            Vec3 ppos = mc.player.position();
            BlockPos pbpos = mc.player.blockPosition();

            BlockPos searchPos;
            if (useWASDControl.get()) {
                var input = PlayerInputUtils.of(mc.options);
                Direction right = direction.getCounterClockWise();
                searchPos = pbpos.offset(direction.getUnitVec3i().multiply(input.forwardSpeed()))
                        .offset(right.getUnitVec3i().multiply(input.sidewaysSpeed()));
                if (enableJump.get() && input.upwardSpeed() > 0) {
                    searchPos = searchPos.relative(Direction.UP, input.upwardSpeed());
                }
                if (!Objects.equals(pbpos, searchPos) && doPearlUse(pbpos, searchPos)) {
                    enablePearlPhase.set(false);
                    return;
                }
            } else {
                if (MathUtils.isInXZRange(pos, ppos, 0.5 - autoPearlActivateRange.get())) {
                    return;
                }
                Direction search = direction;
                Direction result = direction;
                double min = Double.MAX_VALUE;
                do {
                    BlockPos test = pbpos.relative(search);
                    BlockState state = mc.level.getBlockState(test);

                    if (MathUtils.isInXZRange(ppos, Vec3.atCenterOf(test), 0.5 + autoPearlActivateRange.get())) {
                        double sqd = test.distToCenterSqr(ppos);
                        if (sqd < min) {
                            result = search;
                            min = sqd;
                        }
                    }

                    search = search.getClockWise();
                } while (search != direction);
                if (min == Double.MAX_VALUE) {
                    return;
                }
                search = result;
                searchPos = pbpos.relative(search, 1);

                BlockState state = mc.level.getBlockState(searchPos);
                if (!state.isAir() && !state.liquid()) {
                    if (doPearlUse(pbpos, searchPos)) {
                        enablePearlPhase.set(false);
                        return;
                    }
                }
                if (autoCrawl.get() && pose != Pose.SWIMMING) {
                    BlockState state2 = mc.level.getBlockState(searchPos.relative(Direction.UP));
                    if (!state2.isAir() && !state2.liquid()) {
                        if (doPearlUse(pbpos, searchPos)) {
                            enablePearlPhase.set(false);
                            return;
                        }
                    }
                }
            }
        }
    }

    public boolean doPearlUse(BlockPos originPos, BlockPos pos) {
        Pose pose = mc.player.getPose();
        Vec3 look;
        if (pose == Pose.SWIMMING) {
            look = Vec3.atCenterOf(pos).subtract(mc.player.getEyePosition());
        } else {
            look = Vec3.atCenterOf(pos).add(Vec3.atCenterOf(originPos)).scale(0.5).subtract(mc.player.getEyePosition());
        }
        return usePearl(look);
    }

    public boolean usePearl(Vec3 look) {
        look = look.normalize();
        var re = InventoryUtils.findPlayerItem((ss) -> ss.getItem() == Items.ENDER_PEARL, true, false);
        if (re == null) {
            logI18NSub("Pearl", "message.module.pearl-fly.no-pearl");
            return true;
        }

        boolean offHand = offhand.get() || re.index() == 40;
        Runnable runnable = offHand
                ? InvExtra.INSTANCE.swapInventoryIndexToOffhand(re.index())
                : InvExtra.INSTANCE.swapInventoryIndexToHand(re.index());
        if (runnable == null) return false;
        var status = new EntityMovementStatus<>(mc.player);

        PlayerStateManager.setPlayerRotationSafe(mc.player, look);
        InteractionHand hand = offHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        mc.gameMode.useItem(mc.player, hand);
        status.restoreRotation();
        runnable.run();
        return true;
    }
}
