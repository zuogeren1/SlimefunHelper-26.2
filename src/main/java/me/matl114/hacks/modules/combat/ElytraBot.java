package me.matl114.hacks.modules.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import lombok.Setter;
import lombok.experimental.Accessors;
import me.matl114.SlimefunHelper;
import me.matl114.accessors.hacks.PlayerInternalAccess;
import me.matl114.events.Event;
import me.matl114.events.Listener;
import me.matl114.events.RenderListener;
import me.matl114.events.impl.EventContainer;
import me.matl114.gui.basic.DrawableWidget;
import me.matl114.gui.basic.DynamicContentWidget;
import me.matl114.hacks.CombatTasks;
import me.matl114.hacks.MovTasks;
import me.matl114.hacks.RenderTasks;
import me.matl114.hacks.api.BaseModule;
import me.matl114.hacks.api.ModulePath;
import me.matl114.hacks.modules.move.ElytraExtra;
import me.matl114.hacks.modules.move.ElytraFlight;
import me.matl114.hacks.modules.move.PlayerStateManager;
import me.matl114.hacks.utils.HotKeyUtils;
import me.matl114.hacks.utils.config.*;
import me.matl114.hacks.utils.entity.PredictorImpl;
import me.matl114.hacks.utils.move.ElytraOptimizeUtils;
import me.matl114.hacks.utils.move.FlightVelocity;
import me.matl114.managers.Configs;
import me.matl114.managers.Tasks;
import me.matl114.managers.config.*;
import me.matl114.managers.input.MultiKeyBind;
import me.matl114.utils.*;
import me.matl114.utils.algorithms.StateMachine;
import me.matl114.utils.entity.PlayerInputUtils;
import me.matl114.versioned.api.VDataFlag;
import me.matl114.versioned.api.VDrawContext;
import me.matl114.versioned.api.VItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.ApiStatus;

public class ElytraBot extends BaseModule {
    public final ModulePath combatBot = makePath(Configs.COMBAT_CONFIG, "combat-bot");
    public final ModulePath elytraBot = combatBot.add("elytra-bot");

    public static ElytraBot INSTANCE;

    public ElytraBot() {
        super("ElytraBot");
        bindFlag(enable);
        INSTANCE = this;
    }

    public final FlagRef enable = flagBuilder(elytraBot.add("enable")).build();

    public final KeyBindRef keyBind = moduleEntry(
                    elytraBot.add("hotkey"), new MultiKeyBind(), elytraBot.add("enable"), moduleMeta(() -> this.mode))
            .build();

    public final IntRef targetRange = intBuilder(elytraBot.add("range"))
            .defaultValue(80)
            .validator(Configs.INT_POSITIVE)
            .build();

    public final EnumRef<Mode> mode =
            builder(elytraBot.add("mode"), Mode.class).defaultValue(Mode.FOLLOW).build();

    public final FlagRef playerOnly = builder(elytraBot.add("player-only"), FlagRef.TYPE)
            .defaultValue(true)
            .build();

    public final FlagRef autoControl =
            flagBuilder(elytraBot.add("auto-control-elytra")).build();

    public final FlagRef dynamicTarget =
            flagBuilder(elytraBot.add("dynamic-target")).build();

    public final FlagRef onlyWhenNoWASD =
            flagBuilder(elytraBot.add("only-when-no-wasd")).build();

    public final DoubleRef combatMaceRange = builder(elytraBot.add("combat-range"), DoubleRef.TYPE)
            .defaultValue(10.0D)
            .build();

    public final DoubleRef combatSpearRange = builder(elytraBot.add("combat-spear-range"), DoubleRef.TYPE)
            .defaultValue(11.0D)
            .build();

    public final FlagRef followFriend = flagBuilder(elytraBot.add("follower-follow-friend"))
            .show(() -> mode.get().isIn(Mode.FOLLOW))
            .build();

    public final NBTRef<OptionalPrimitive<Double>> followOnGroundHeight = builder(
                    elytraBot.add("follow-on-ground-height-extra"), OptionalPrimitive.DOUBLE_TYPE)
            .defaultValue(new OptionalPrimitive<>(true, NBTTypes.DOUBLE_TYPE, 2.0D))
            .show(() -> mode.get().isNotIn(Mode.SPEAR_ARUA))
            .build();

    public final FlagRef maceAttackUseSimple = builder(elytraBot.add("mace-combat-use-extra-attack"), Boolean.class)
            .defaultValue(true)
            .show(() -> mode.get().isIn(Mode.MACE_ARUA))
            .build();

    public final FlagRef maceAttackConsiderUse = flagBuilder(elytraBot.add("mace-combat-consider-use"))
            .show(() -> mode.get().isIn(Mode.MACE_ARUA))
            .build();

    public final DoubleRef maceHeight = builder(elytraBot.add("mace-height"), DoubleRef.TYPE)
            .defaultValue(10.0D)
            .show(() -> mode.get().isIn(Mode.MACE_ARUA))
            .build();

    public final DoubleRef maceHeightGround = builder(elytraBot.add("mace-height-ground"), DoubleRef.TYPE)
            .defaultValue(10.0D)
            .show(() -> mode.get().isIn(Mode.MACE_ARUA))
            .build();

    public final IntRef maceRemainPullUpTick = builder(elytraBot.add("mace-max-extra-pull-up-tick"), IntRef.TYPE)
            .defaultValue(20)
            .show(() -> mode.get().isIn(Mode.MACE_ARUA))
            .build();

    public final DoubleRef maceFollowMinHeight = doubleBuilder(elytraBot.add("mace-max-follow-height"))
            .defaultValue(1.5D)
            .show(() -> mode.get().isIn(Mode.MACE_ARUA))
            .build();

    public final FlagRef maceUsePredictor = flagBuilder(elytraBot.add("mace-pull-up-use-predictor"))
            .show(() -> mode.get().isIn(Mode.MACE_ARUA))
            .build();

    @ApiStatus.Experimental
    public final DoubleRef maceYLevelWeight = doubleBuilder(elytraBot.add("mace-y-level-lerp"))
            .show(() -> mode.get().isIn(Mode.MACE_ARUA))
            .defaultValue(0.0)
            .experimental()
            .build();

    public final FlagRef combatSmoothFlight1 = flagBuilder(elytraBot.add("combat-smooth-flight"))
            .show(() -> mode.get().isIn(Mode.MACE_ARUA))
            .build();

    public final DoubleRef combatSmoothArg1 = doubleBuilder(elytraBot.add("combat-smooth-flight-argument-1"))
            .show(() -> mode.get().isIn(Mode.MACE_ARUA))
            .defaultValue(1.0D)
            .build();

    public final DoubleRef combatSmoothArg11 = doubleBuilder(elytraBot.add("combat-smooth-flight-argument-1-1"))
            .show(() -> mode.get().isIn(Mode.MACE_ARUA))
            .defaultValue(1.0D)
            .build();

    public final DoubleRef combatSmoothArg16 = doubleBuilder(elytraBot.add("combat-smooth-flight-argument-1-6"))
            .defaultValue(16.0D)
            .show(() -> mode.get().isIn(Mode.MACE_ARUA))
            .build();

    public final FlagRef combatSmoothArg15 = flagBuilder(elytraBot.add("combat-smooth-flight-argument-1-5"))
            .show(() -> mode.get().isIn(Mode.MACE_ARUA))
            .build();

    @ApiStatus.Experimental
    public final FlagRef combatSmoothFlag2 = flagBuilder(elytraBot.add("combat-smooth-flight-argument-1-2"))
            .show(() -> mode.get().isIn(Mode.MACE_ARUA))
            .experimental()
            .build();

    @ApiStatus.Experimental
    public final NBTRef<OptionalPrimitive<me.matl114.hacks.utils.config.Vec3>> combatSmoothArg14 = builder(
                    elytraBot.add("combat-smooth-flight-argument-1-4"), OptionalPrimitive.type(me.matl114.hacks.utils.config.Vec3.class))
            .show(() -> mode.get().isIn(Mode.MACE_ARUA))
            .defaultValue(new OptionalPrimitive<>(false, NBTTypes.VEC3_TYPE, new me.matl114.hacks.utils.config.Vec3(10, 0.3, 20)))
            .experimental()
            .build();

    @ApiStatus.Experimental
    public final FlagRef combatSmoothFlight2 = flagBuilder(elytraBot.add("combat-smooth-flight-2"))
            .show(() -> mode.get().isIn(Mode.MACE_ARUA))
            .experimental()
            .build();

    @ApiStatus.Experimental
    public final DoubleRef combatSmoothArg21 = doubleBuilder(elytraBot.add("combat-smooth-flight-argument-2-1"))
            .show(() -> mode.get().isIn(Mode.MACE_ARUA))
            .defaultValue(10.0D)
            .experimental()
            .build();

    @ApiStatus.Experimental
    public final DoubleRef combatSmoothArg22 = doubleBuilder(elytraBot.add("combat-smooth-flight-argument-2-2"))
            .show(() -> mode.get().isIn(Mode.MACE_ARUA))
            .defaultValue(10.0D)
            .experimental()
            .build();

    public final FlagRef angleOptimizePullUp = builder(elytraBot.add("combat-angle-optimize"), Boolean.class)
            .defaultValue(false)
            .show(() -> mode.get().isIn(Mode.MACE_ARUA)
                    && ElytraExtra.INSTANCE.autoRescale.get()
                    && ElytraFlight.INSTANCE.useAutoRescale.get())
            .build();

    public final FlagRef angleOptimizeFollow = builder(elytraBot.add("combat-angle-optimize-follow"), Boolean.class)
            .defaultValue(false)
            .show(() -> mode.get().isIn(Mode.MACE_ARUA)
                    && ElytraExtra.INSTANCE.autoRescale.get()
                    && ElytraFlight.INSTANCE.useAutoRescale.get())
            .build();
    public final NBTRef<LabelVec3> angleOptimizeFollowRange = builder(
                    elytraBot.add("combat-angle-optimize-range"), LabelVec3.class)
            .defaultValue(new LabelVec3(
                    "widget.elytra-bot.angle.normal-flight",
                    "widget.elytra-bot.angle.spear-flight",
                    "widget.elytra-bot.angle.anti-spear-flight",
                    new me.matl114.hacks.utils.config.Vec3(4.0, 4.0, 4.0)))
            .show(() -> mode.get().isIn(Mode.MACE_ARUA)
                    && ElytraExtra.INSTANCE.autoRescale.get()
                    && ElytraFlight.INSTANCE.useAutoRescale.get())
            .build();

    // 这个傻逼玩意， 代表的是 激进的拉升优化
    @ApiStatus.Experimental
    public final FlagRef angleOptimizeRadicalPullup = flagBuilder(elytraBot.add("combat-angle-optimize-radical"))
            .show(() -> mode.get().isIn(Mode.MACE_ARUA)
                    && ElytraExtra.INSTANCE.autoRescale.get()
                    && ElytraFlight.INSTANCE.useAutoRescale.get()
                    && ElytraExtra.INSTANCE.autoRescaleAl.get().isIn(ElytraExtra.Al.V2, ElytraExtra.Al.V3))
            .experimental()
            .build();
    // 直接往外拉
    @ApiStatus.Experimental
    public final NBTRef<OptionalPrimitive<Double>> angleOptimizePullRange = builder(
                    elytraBot.add("combat-angle-optimize-radical-pull-up-optimize-range"),
                    OptionalPrimitive.DOUBLE_TYPE)
            .defaultValue(new OptionalPrimitive<>(false, NBTTypes.DOUBLE_TYPE, 20.0D))
            .show(() -> mode.get().isIn(Mode.MACE_ARUA)
                    && ElytraExtra.INSTANCE.autoRescale.get()
                    && ElytraFlight.INSTANCE.useAutoRescale.get()
                    && ElytraExtra.INSTANCE.autoRescaleAl.get().isIn(ElytraExtra.Al.V2, ElytraExtra.Al.V3))
            .experimental()
            .build();
    // 这个傻逼玩意 代表的是激进的追击角度优化是否有角度限制（在垂直向下的时候禁用角度优化）
    public final NBTRef<OptionalPrimitive<Double>> angleOptimizeRadicalFollow = builder(
                    elytraBot.add("combat-angle-optimize-radical-follow"), OptionalPrimitive.DOUBLE_TYPE)
            .defaultValue(new OptionalPrimitive<>(true, NBTTypes.DOUBLE_TYPE, 75.0D))
            .validator(s -> s.getValue() >= 0.0D && s.getValue() <= 90.0D)
            .show(() -> mode.get().isIn(Mode.MACE_ARUA)
                    && ElytraExtra.INSTANCE.autoRescale.get()
                    && ElytraFlight.INSTANCE.useAutoRescale.get())
            .build();

    // 这个傻逼玩意。在拉高的时候会来回摆。非常的炫酷(何意味
    @ApiStatus.Experimental
    public final NBTRef<OptionalPrimitive<Double>> pullUpAngleOptimize = builder(
                    elytraBot.add("combat-pull-up-angle-optimize"), OptionalPrimitive.DOUBLE_TYPE)
            .defaultValue(new OptionalPrimitive<>(false, NBTTypes.DOUBLE_TYPE, 6.0D))
            .show(() -> mode.get().isIn(Mode.MACE_ARUA)
                    && ElytraExtra.INSTANCE.autoRescale.get()
                    && ElytraFlight.INSTANCE.useAutoRescale.get())
            .experimental()
            .build();

    public final NBTRef<OptionalPrimitive<Double>> maceChaseFollowYBias = builder(
                    elytraBot.add("combat-mace-chase-follow-y-bias"), OptionalPrimitive.DOUBLE_TYPE)
            .show(() -> mode.get().isIn(Mode.MACE_ARUA))
            .defaultValue(new OptionalPrimitive<>(false, NBTTypes.DOUBLE_TYPE, 3.0D))
            .build();

    public final FlagRef logSpearHit = flagBuilder(elytraBot.add("log-spear-hit"))
            .show(() -> mode.get().isIn(Mode.SPEAR_ARUA))
            .build();

    @ApiStatus.Experimental
    public final FlagRef spearTestV2 = builder(elytraBot.add("spear-v2"), Boolean.class)
            .defaultValue(false)
            .show(() -> mode.get().isIn(Mode.SPEAR_ARUA)
                    && ElytraExtra.INSTANCE.autoRescale.get()
                    && ElytraFlight.INSTANCE.useAutoRescale.get())
            .experimental()
            .build();

    @ApiStatus.Experimental
    public final FlagRef spearAngleOptimize = builder(elytraBot.add("spear-angle-optimize"), Boolean.class)
            .defaultValue(false)
            .show(() -> mode.get().isIn(Mode.SPEAR_ARUA)
                    && ElytraExtra.INSTANCE.autoRescale.get()
                    && ElytraFlight.INSTANCE.useAutoRescale.get()
                    && spearTestV2.get())
            .experimental()
            .build();

    public final FlagRef spearAntiSpear = flagBuilder(elytraBot.add("spear-anti-spear"))
            .show(() -> mode.get().isIn(Mode.SPEAR_ARUA) && !this.spearTestV2.get())
            .build();

    public final DoubleRef spearAntiSpearExtraDistance = doubleBuilder(elytraBot.add("spear-anti-spear-extra-distance"))
            .defaultValue(0.0D)
            .show(() -> mode.get().isIn(Mode.SPEAR_ARUA) && !this.spearTestV2.get())
            .build();

    public final FlagRef spearUsePredictor = flagBuilder(elytraBot.add("spear-use-predictor"))
            .show(() -> mode.get().isIn(Mode.SPEAR_ARUA))
            .build();

    public final NBTRef<OptionalPrimitive<Double>> flyAntiSpear = builder(
                    elytraBot.add("fly-anti-spear"), OptionalPrimitive.DOUBLE_TYPE)
            .defaultValue(new OptionalPrimitive<>(true, NBTTypes.DOUBLE_TYPE, 1.0D))
            .show(() ->
                    mode.get().isNotIn(Mode.SPEAR_ARUA) || (mode.get().isIn(Mode.SPEAR_ARUA) && this.spearTestV2.get()))
            .build();

    public final NBTRef<OptionalPrimitive<Double>> flyAntiSpearWhenPullup = builder(
                    elytraBot.add("fly-anti-spear-when-pull-up"), OptionalPrimitive.DOUBLE_TYPE)
            .defaultValue(new OptionalPrimitive<>(false, NBTTypes.DOUBLE_TYPE, 1.0D))
            .show(() -> mode.get().isIn(Mode.MACE_ARUA))
            .build();

    @ApiStatus.Experimental
    public final FlagRef flyAntiSpearDisableWhenSpear = flagBuilder(
                    elytraBot.add("fly-anti-spear-disable-when-using-spear"))
            .show(() -> (mode.get().isNotIn(Mode.SPEAR_ARUA)
                    || (mode.get().isIn(Mode.SPEAR_ARUA) && this.spearTestV2.get())))
            .experimental()
            .build();

    public final FlagRef flyAntiSpearAfterAngle = builder(elytraBot.add("fly-anti-spear-after-angle"), Boolean.class)
            .defaultValue(true)
            .show(() -> (mode.get().isNotIn(Mode.SPEAR_ARUA)
                    || (mode.get().isIn(Mode.SPEAR_ARUA) && this.spearTestV2.get())))
            .build();

    public final FlagRef flyAntiSpearUseSpearResetWhenFollow = flagBuilder(
                    elytraBot.add("fly-anti-spear-use-spear-reset-when-follow"))
            .show(() -> mode.get().isIn(Mode.MACE_ARUA))
            .build();

    public final FlagRef render = flagBuilder(elytraBot.add("render")).build();

    public final KeyBindRef switchMode = hotkey(elytraBot.add("switch-hotkey"))
            .defaultValue(new MultiKeyBind())
            .registerHotkey(HotKeyUtils.wrapAsHandler(this::onSwitch))
            .build();

    public void onSwitch() {
        mode.next();
        logI18N("message.module.elytra-bot.mode-switch", mode.get().getDisplay());
    }

    public void registerAll() {
        super.registerAll();
        registerListener(Listener.getPreTick(), this::onPreTick);
        registerListener(Listener.getCustomListener().getChannel(FlightVelocity.class), this::onElytraChase);
        registerListener(Listener.getPacketPoint().getChannel(ServerboundAttackPacket.class), this::onAttack);
        registerListener(Listener.getPreHandleInputEvents(), this::onInputEvent);
        registerListener(Listener.getPacketPoint().getChannel(ClientboundEntityEventPacket.class), this::onEntityStatus);
        registerListener(RenderListener.getRender3DEvent(), this::onRender);
        if (SlimefunHelper.DEV_ENV) {
            registerListener(RenderListener.getRender2DEvent(), this::onDebugRender);
        }
        registerListener(Listener.getPacketPoint().getChannel(ClientboundDamageEventPacket.class), this::onEntityDamage);
    }

    @Override
    public void addCustomWidgets(Consumer<DrawableWidget> acceptor, int dx, int dy, int dblank) {
        super.addCustomWidgets(acceptor, dx, dy, dblank);
        var widget = createTitleLabel("widget.attack.attack.use-argument", 0, dblank, dx, dy);
        acceptor.accept(new DynamicContentWidget<>(
                () -> {
                    return mode.get().isIn(Mode.MACE_ARUA) ? widget : null;
                },
                0,
                0));
    }

    Entity target;

    @Nullable
    AbstractBotBehaviour currentBehaviour;

    final AbstractBotBehaviour defaultBehaviour = new Follower().setBase(this);
    final AbstractBotBehaviour maceArua = new MaceArua().setBase(this);
    final AbstractBotBehaviour maceAruaGround = new MaceAuraGround().setBase(this);
    final AbstractBotBehaviour spearArua = new SpearAura().setBase(this);

    public boolean canControlFlight() {
        return enable.get()
                && autoControl.get()
                && currentBehaviour != null
                && currentBehaviour.movementDirection != null
                && currentBehaviour.movementDirection.lengthSqr() > 1E-9;
    }

    Entity lastTarget;
    TargetAction currentAction;
    boolean currentInCombatRange;
    boolean currentOnGround;
    double speedMultiplier = 1.0D;
    boolean heightLimitEnvironment = false;

    public boolean isNoPullUpEnvironment() {
        return heightLimitEnvironment || currentOnGround;
    }

    public boolean isTargetUsingSpear() {
        return target instanceof Player otherShit && SpearEnhance.isUsingSpear(otherShit);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public AbstractBotBehaviour getBehaviour() {
        if (enable.get()) {
            return switch (mode.get()) {
                case FOLLOW -> defaultBehaviour;
                case MACE_ARUA -> isNoPullUpEnvironment() ? maceAruaGround : maceArua;
                case SPEAR_ARUA -> spearArua;
            };
        } else {
            return null;
        }
    }

    public void onPreTick(Event<Void> event) {
        var lastBehaviour = currentBehaviour;
        currentBehaviour = getBehaviour();
        if (currentBehaviour != lastBehaviour) {
            if (lastBehaviour != null) {
                lastBehaviour.onDisable();
            }
            if (currentBehaviour != null) {
                currentBehaviour.onEnable();
            }
        }
        if (checkNull()) return;
        if (currentBehaviour != null) {
            heightLimitEnvironment = mc.level.dimensionType().hasCeiling()
                    && mc.player.getY()
                            < mc.level.dimensionType().minY()
                                    + mc.level.dimensionType().logicalHeight();
            refreshTarget();
            updateTargetAction();
            currentBehaviour.onUpdate();
        }
    }

    public double combatRange() {
        return (target instanceof Player pl && SpearEnhance.isUsingSpear(pl))
                ? combatSpearRange.get()
                : combatMaceRange.get();
    }

    public void updateTargetAction() {
        if (target != lastTarget) {
            lastTarget = target;
            currentAction = null;
        }
        if (target != null) {

            // initialize pos
            double combatRange = combatRange();
            currentInCombatRange = TargetSelector.INSTANCE.isWithinAttackRange(
                    mc.player.position(), target.getBoundingBox(), combatRange);
            currentOnGround = target.onGround() || CollisionUtil.isEntitySupported(target);
            if (target instanceof Player pl) {
                // speed < 1, we can easily handle this speed
                if (currentOnGround) {
                    currentAction = TargetAction.SLOW_SPEED;
                } else {
                    List<PredictorImpl.KnownPosition> knownPositions =
                            ((PlayerInternalAccess) target).getPredictorImpl().getLastKnownPositions(3);
                    if (knownPositions.size() < 2) {
                        // 数据不足，默认行为（可改为 TOWARDS 或不做处理）
                        currentAction = TargetAction.CIRCLING;
                    } else {
                        int currentTick = Tasks.getTick();
                        // 1. 最早的点（索引0）是否在10 tick之前
                        PredictorImpl.KnownPosition oldest = knownPositions.get(0);
                        if (currentTick - oldest.tick() > 20) {
                            currentAction = TargetAction.AFK;
                        } else {
                            // 相邻点距离检查
                            net.minecraft.world.phys.Vec3 pos0 = oldest.vec3d();
                            net.minecraft.world.phys.Vec3 pos1 = knownPositions.get(1).vec3d();
                            net.minecraft.world.phys.Vec3 pos2 = knownPositions.get(2).vec3d();

                            double dist01 = pos0.distanceTo(pos1);
                            double dist12 = pos1.distanceTo(pos2);
                            if (dist01 < 1E-6 && dist12 < 1E-6) {
                                currentAction = TargetAction.AFK;
                            }
                            // 2. 若相邻两点距离小于1.5，判定为SLOW_SPEED
                            else if (dist01 < 0.75 && dist12 < 0.75) {
                                currentAction = TargetAction.SLOW_SPEED;
                            } else if (knownPositions.size() >= 3) {
                                // 3. 计算向量 ab 和 bc 的夹角
                                net.minecraft.world.phys.Vec3 ab = pos1.subtract(pos0);
                                net.minecraft.world.phys.Vec3 bc = pos2.subtract(pos1);
                                double dot = ab.dot(bc);
                                double magAB = ab.length();
                                double magBC = bc.length();
                                double angleRad = Math.acos(Math.min(1.0, Math.max(-1.0, dot / (magAB * magBC))));
                                double angleDeg = Math.toDegrees(angleRad);

                                if (angleDeg < 60.0) {
                                    // 方向变化小，判断朝向玩家还是远离玩家
                                    net.minecraft.world.phys.Vec3 playerPos = mc.player.position();
                                    // 使用从最新点(pos2)指向玩家的向量
                                    net.minecraft.world.phys.Vec3 toPlayer = playerPos.subtract(pos2);
                                    // 如果 bc 方向（移动方向）与指向玩家的方向夹角小于90度，视为向玩家靠近
                                    double moveDot = bc.normalize().dot(toPlayer.normalize());
                                    if (moveDot > 0) {
                                        currentAction = TargetAction.TOWARDS; // 向我们来
                                    } else {
                                        currentAction = TargetAction.ESCAPING; // 离我们去
                                    }
                                } else {
                                    currentAction = TargetAction.CIRCLING;
                                }
                            } else {
                                // 点不足3个，默认行为
                                currentAction = TargetAction.TOWARDS;
                            }
                        }
                    }
                }
            } else {
                currentAction = TargetAction.SLOW_SPEED;
            }
        }
    }

    public void onRender(Event<PoseStack> event) {
        if (enable.get() && render.get()) {
            RenderUtils.startDrawVirtual(event.context);
            try {
                PoseStack stack = event.context;
                if (currentBehaviour != null) {
                    net.minecraft.world.phys.Vec3 targetRender = currentBehaviour.movementDirection.add(mc.player.position());
                    if (targetRender != null) {
                        RenderUtils.drawOutlinedBox(
                                stack,
                                targetRender.add(RenderTasks.FROM),
                                targetRender.add(RenderTasks.TO),
                                Color.MAGENTA);
                    }
                }

            } finally {
                RenderUtils.stopDrawVirtual(event.context);
            }
        }
    }

    public void onDebugRender(Event<VDrawContext> eventVDraw) {
        if (enable.get() && render.get() && currentBehaviour != null && target != null) {
            var vdraw = eventVDraw.context;
            vdraw.getMatrices().pushMatrix();
            vdraw.getMatrices().translate(200, 200);
            vdraw.drawText(
                    mc.font,
                    "Action: %s, Combating: %s".formatted(currentAction, String.valueOf(currentInCombatRange)),
                    0,
                    0,
                    -1,
                    true);
            vdraw.getMatrices().popMatrix();
        }
    }

    public void refreshTarget() {
        if (!EntityUtils.isEntityValid(target)
                || target.position().distanceToSqr(mc.player.position()) > targetRange.get()) {
            target = null;
        }
        if (target == null || dynamicTarget.get()) {
            target = currentBehaviour != null ? currentBehaviour.searchTarget() : null;
        }
    }

    //    private boolean isConsideredAsAttackableEntity(Entity entity) {
    //        if (!(entity instanceof Player) && playerOnly.get()) {
    //            return false;
    //        }
    //        var raycastResult = RaycastUtils.raycastSolidBlockResult(mc.player, mc.player.getPos(), entity.getPos());
    //        if (raycastResult == null || raycastResult.getType() == HitResult.Type.MISS) {
    //            return true;
    //        }
    //        Vec3d hitPoint = raycastResult.getPos();
    //        return TargetSelector.INSTANCE.isWithinAttackRange(
    //                hitPoint, entity.getBoundingBox(), CombatExtra.INSTANCE.getAttackAtTargetRange(entity));
    //    }

    public void onElytraChase(Event<EventContainer<FlightVelocity>> event) {
        if (enable.get()) {
            AbstractBotBehaviour behaviour = currentBehaviour;
            if (behaviour != null) {
                if (onlyWhenNoWASD.get()) {
                    PlayerInputUtils.Input input = PlayerInputUtils.of(mc.options);
                    if (input.hasMovementControl()) {
                        behaviour.onPauseControl();
                        return;
                    }
                }
                behaviour.onElytra(event);
            }
        }
    }

    public void onAttack(Event<ServerboundAttackPacket> attack) {
        // 26.2: 攻击语义由 ServerboundAttackPacket 承载
        if (enable.get() && currentBehaviour != null) {
            Entity entity = mc.level.getEntity(attack.context.entityId());
            if (entity != null) {
                currentBehaviour.onAttack(entity);
            }
        }
    }

    public void onInputEvent(Event<Void> event) {
        if (enable.get()) {
            AbstractBotBehaviour behaviour = currentBehaviour;
            if (behaviour != null) {
                behaviour.onInputEvent(event);
            }
        }
    }

    public void onEntityStatus(Event<ClientboundEntityEventPacket> statusS2CPacketEvent) {
        var statusS2CPacket = statusS2CPacketEvent.context;
        if (currentBehaviour instanceof HitListener sp
                && mc.player != null
                && mc.level != null
                && enable.get()
                && statusS2CPacket.getEntity(mc.level) == mc.player
                && statusS2CPacket.getEventId() == VDataFlag.ENTITY_STATUS_KINETIC_ATTACK) {
            if (logSpearHit.get() && mode.get().isIn(Mode.SPEAR_ARUA)) {
                logI18N("message.module.elytra-bot.spear-hit");
            }
            sp.onHit(HitListener.HIT_SPEAR);
        }
    }

    public void onEntityDamage(Event<ClientboundDamageEventPacket> e) {
        if (checkNull()) return;
        if (currentBehaviour instanceof HitListener sp
                && enable.get()
                && e.context.sourceCauseId() == mc.player.getId()
                && mc.level.getEntity(e.context.entityId()) == target) {
            var source = e.context.sourceType().unwrapKey().orElse(null);
            if (DamageUtils.isType(source, "mace_smash")) {
                // we trigger a mace smash
                sp.onHit(HitListener.HIT_MACE);
                return;
            }
            sp.onHit(HitListener.HIT_ATTACK);
        }
    }

    public static interface HitListener {
        static int HIT_ATTACK = 0;
        static int HIT_MACE = 1;
        static int HIT_SPEAR = 2;

        public void onHit(int type);
    }

    @Setter
    @Accessors(chain = true)
    public abstract static class AbstractBotBehaviour {
        // todo： add target anaylsis

        ElytraBot base;
        net.minecraft.world.phys.Vec3 movementDirection = net.minecraft.world.phys.Vec3.ZERO;
        // todo: update target considering blocks , can we async calculate to let
        // use pitch search

        // todo: calculate reachable, if entity can reach reach distance
        public void onElytra(Event<EventContainer<FlightVelocity>> event) {
            if (movementDirection != null && movementDirection.lengthSqr() > 1E-9) {
                net.minecraft.world.phys.Vec3 targetVec = movementDirection;
                double targetVecVelocity = targetVec.length();
                double min =
                        Math.min(targetVecVelocity, event.context.getValue().maxVelocity() * base.speedMultiplier);
                targetVec = targetVec.normalize().scale(min);
                event.context.getValue().velocity(targetVec);
            }
        }

        public Entity searchTarget() {
            return CombatTasks.getTargetSelector()
                    .searchAttackEntity(
                            base.targetRange.get(),
                            true,
                            base.playerOnly.get() ? (e) -> e instanceof Player : null);
        }

        public synchronized void onUpdate() {
            base.speedMultiplier = 1.0D;
        }

        public void onInputEvent(Event<Void> eventInput) {}

        public synchronized void onAttack(Entity entity) {}

        public abstract void onEnable();

        public abstract void onDisable();

        public void onPauseControl() {}

        protected boolean willUseAntiSpear(OptionalPrimitive<Double> op, net.minecraft.world.phys.Vec3 predictorPos) {
            return (op.isPresent()
                    && (!(base.flyAntiSpearDisableWhenSpear.get() && SpearEnhance.isUsingSpear(mc.player)))
                    && Math.abs(op.getValue()) > 1E-6
                    && base.isTargetUsingSpear()
                    && mc.player.position().distanceToSqr(predictorPos) < MathUtils.s2(base.combatSpearRange.get()));
        }

        protected void antiSpear(OptionalPrimitive<Double> op) {
            ;
            net.minecraft.world.phys.Vec3 originalLookHorizontal = movementDirection.with(Direction.Axis.Y, 0);
            if (originalLookHorizontal.lengthSqr() < 1E-2) {
                //
                double range = 3;
                if (mc.player.position().subtract(base.target.position()).horizontalDistance() < range) {
                    movementDirection = movementDirection.with(Direction.Axis.X, 5);
                    originalLookHorizontal = movementDirection.with(Direction.Axis.Y, 0);
                } else {
                    // pulling up, do not antispear
                    return;
                }
            }
            net.minecraft.world.phys.Vec3 vertical = new net.minecraft.world.phys.Vec3(0, 1, 0);
            net.minecraft.world.phys.Vec3 side = vertical.cross(originalLookHorizontal).normalize();
            net.minecraft.world.phys.Vec3 origin = movementDirection.normalize();

            net.minecraft.world.phys.Vec3 multiply = side.scale(op.getValue());
            movementDirection = origin.add(multiply).normalize().scale(10);
        }
    }

    public static class Follower extends AbstractBotBehaviour {
        // todo: add in-hole behaviour, add hole-esp related, add landing

        @Override
        public Entity searchTarget() {
            return CombatTasks.getTargetSelector().searchAttack(base.targetRange.get(), true, 0, this::canBeAttack);
        }

        public boolean canBeAttack(Entity entity) {
            if (entity instanceof Player player
                    && player != mc.player
                    && base.followFriend.get()
                    && !TargetSelector.INSTANCE.isNotFriend(player)) {
                return true;
            } else {
                return TargetSelector.INSTANCE.canAttack(entity)
                        && (!base.playerOnly.get() || entity instanceof Player);
            }
        }

        @Override
        public synchronized void onUpdate() {
            super.onUpdate();
            if (base.target != null) {
                movementDirection = base.target.position().subtract(mc.player.position());
                if (base.currentOnGround) {
                    var op = base.followOnGroundHeight.get();
                    if (op.isPresent()) {
                        movementDirection = movementDirection.add(0, op.getValue(), 0);
                    }
                }
            } else {
                movementDirection = net.minecraft.world.phys.Vec3.ZERO;
            }
        }

        @Override
        public void onEnable() {}

        @Override
        public void onDisable() {}
    }

    public abstract static class AbstractMaceBehaviour extends AbstractBotBehaviour implements HitListener {
        static final int STATE_PULL_UP = 1;
        static final int STATE_FOLLOW = 2;
        static final int STATE_WAIT_ATTACK = 3;
        static final int STATE_NONE = 0;
        public StateMachine stateMachine;
        int startWaitAttack = -1;
        double maxHeightInAttack = Double.MIN_VALUE;
        double startPullUp = Double.MIN_VALUE;
        int startPullUpTick = 0;

        public AbstractMaceBehaviour() {
            stateMachine = new StateMachine(
                    STATE_NONE,
                    this::onCondition,
                    this::onStateNone,
                    this::onStatePullUp,
                    this::onStateFollow,
                    this::onStateWaitAttack);
            stateMachine.registerListener(STATE_WAIT_ATTACK, this::onStartWaitAttack);
            stateMachine.registerListener(STATE_PULL_UP, this::onStartPullUp);
        }

        public int onCondition(StateMachine machine, int state) {
            if (base.target == null) {
                machine.markForEndState();
                movementDirection = net.minecraft.world.phys.Vec3.ZERO;
                return STATE_NONE;
            }
            return state;
        }

        public int onStateNone(StateMachine machine) {
            if (base.target != null) {
                if (PlayerStateManager.INSTANCE.fallDistance > 4
                        && mc.player.position().y() > base.target.position().y() + 4.0) {
                    return STATE_FOLLOW;
                }
                return STATE_PULL_UP;
            }
            movementDirection = net.minecraft.world.phys.Vec3.ZERO;
            machine.markForEndState();
            return STATE_NONE;
        }

        public abstract int onStatePullUp(StateMachine machine);

        public abstract int onStateFollow(StateMachine machine);
        // compat delay attack shit, add cd,
        public int onStateWaitAttack(StateMachine machine) {

            if (base.currentAction != TargetAction.AFK && base.currentAction != TargetAction.SLOW_SPEED) {
                machine.markForEndState();
                return STATE_PULL_UP;
            }
            if (++startWaitAttack > 1) {
                if (base.currentOnGround) {
                    return STATE_NONE;
                } else {
                    return STATE_FOLLOW;
                }
            }
            machine.markForEndState();
            // stay!
            net.minecraft.world.phys.Vec3 targetPos = base.maceUsePredictor.get()
                    ? PositionPredict.INSTANCE
                            .attackPredictArgument
                            .get()
                            .predict(base.target)
                            .with(Direction.Axis.Y, base.target.getY())
                    : base.target.position();
            double lerpY = base.maceYLevelWeight.get();
            double yLerp = targetPos.y * lerpY + base.target.getY() * (1.0D - lerpY);
            targetPos = targetPos.with(Direction.Axis.Y, yLerp);
            setTargetToPlayer(targetPos);
            return STATE_WAIT_ATTACK;
        }

        protected abstract void setTargetToPlayerUpper(net.minecraft.world.phys.Vec3 predictor);

        protected abstract void setTargetToPlayer(net.minecraft.world.phys.Vec3 targetPos);

        protected net.minecraft.world.phys.Vec3 calculateTargetDirection(net.minecraft.world.phys.Vec3 predictorPos) {
            if (!ElytraExtra.INSTANCE.autoRescale.get()
                    || ElytraExtra.INSTANCE.autoRescaleAl.get().isIn(ElytraExtra.Al.V1, ElytraExtra.Al.V2)) {
                //
                if (SpearEnhance.isUsingSpear(mc.player)) {
                    return (predictorPos
                                    .add(0, base.target.getEyeHeight(base.target.getPose()), 0)
                                    .subtract(mc.player.getEyePosition()))
                            .normalize();
                } else {
                    return predictorPos.subtract(mc.player.position()).normalize();
                }
            } else {
                net.minecraft.world.phys.Vec3 legacy;
                if (SpearEnhance.isUsingSpear(mc.player)) {
                    net.minecraft.world.phys.Vec3 targetPos = predictorPos.add(0, base.target.getEyeHeight(base.target.getPose()), 0);
                    legacy = targetPos.subtract(mc.player.getEyePosition());
                } else {
                    legacy = predictorPos.subtract(mc.player.position());
                }

                net.minecraft.world.phys.Vec3 forward = ElytraOptimizeUtils.calculateLookTowardsTargetV3Direction(legacy, 1.7);
                if (legacy.dot(forward) < 0) {
                    return legacy;
                } else {
                    return forward;
                }
            }
        }

        @Override
        public synchronized void onUpdate() {
            super.onUpdate();
            if (mc.player.isFallFlying() || mc.player.getAbilities().flying) {
                maxHeightInAttack = Math.max(maxHeightInAttack, mc.player.getY());
                stateMachine.step();
                // Debug.info("State", stateMachine.getState(), "height", PlayerStateManager.INSTANCE.fallDistance);
                // todo: consider cooldown, do not attack too fast
                if (attackFlag) {
                    if (base.target != null) {
                        // anti shield
                        boolean simpleAttack = shouldAttackSimple();
                        boolean maceAttack = canAttackMace();
                        if (simpleAttack) {
                            // can not deal mace attack anyway
                            Attack.AttackSettings settings = CombatTasks.getAttack()
                                    .createAttackSettings()
                                    .withMaceSwap(false);
                            if (shouldUseAntiShield()) {
                                settings = settings.withAntiShieldSwap(true);
                            }
                            CombatTasks.getAttack().attackEntity(base.target, settings);
                        }
                        if (maceAttack) {
                            Attack.AttackSettings settings =
                                    CombatTasks.getAttack().createAttackSettings();
                            CombatTasks.getAttack()
                                    .attackEntity(
                                            base.target,
                                            settings.withMaceSwap(true)
                                                    .withInvSwap(false)
                                                    .withAntiShieldSwap(false));
                            lastMaceAttackTick = Tasks.getTick();
                            Debug.debug("[ElytraBot] Do mace attack here", Tasks.getTick());
                        }
                    }
                    maxHeightInAttack = mc.player.getY();
                    attackFlag = false;
                }
            } else {
                stateMachine.setState(STATE_NONE);
                movementDirection = net.minecraft.world.phys.Vec3.ZERO;
            }
            lastFallDistance = PlayerStateManager.INSTANCE.fallDistance;
        }

        public void onStartWaitAttack(boolean on) {
            startWaitAttack = 0;
        }

        public void onStartPullUp(boolean on) {
            if (on) {
                startPullUp = mc.player.getY();
                startPullUpTick = Tasks.getTick();
            } else {
                startPullUp = Double.MIN_VALUE;
                startPullUpTick = 0;
            }
        }

        public boolean shouldUseAntiShield() {
            return Attack.shouldUseAntiShield(base.target);
        }

        public boolean shouldPullUpEating() {
            return base.maceAttackConsiderUse.get()
                    && CombatTasks.getAttackAura().checkEating();
        }

        public boolean shouldAttackSimple() {
            boolean useAntiShield = shouldUseAntiShield();
            if (useAntiShield) return true;
            if (VItem.getInstance().isSpear(mc.player.getUseItem())) return false;
            if (!base.maceAttackUseSimple.get()) return false;
            if ((mc.player.getAttackStrengthScale(0.5F) > 0.95F)) {
                if (base.maceAttackConsiderUse.get()
                        && CombatTasks.getAttackAura().checkUsing()) {
                    return false;
                }
                // auto mace, do not hit twice
                if (Attack.INSTANCE.willUseMaceAttack(false)) {
                    return false;
                }
                return true;
            }
            return false;
        }

        public boolean shouldAttackMace() {
            boolean cooldown =
                    (lastMaceAttackSuccessTick < Tasks.getTick()) || Attack.INSTANCE.willUseMaceAttack(false);
            return cooldown && PlayerStateManager.INSTANCE.fallDistance > 1.5D;
        }

        public boolean canAttackMace() {
            boolean cooldown = (lastMaceAttackSuccessTick < Tasks.getTick());
            return Attack.INSTANCE.willUseMaceAttack(true)
                    && (cooldown || PlayerStateManager.INSTANCE.fallDistance > 1.5);
        }

        public synchronized void onPauseControl() {
            stateMachine.setState(STATE_NONE);
        }

        boolean attackFlag = false;
        int lastMaceAttackTick;
        int lastMaceAttackSuccessTick;
        double lastFallDistance;

        public void scheduleAttack() {
            attackFlag = true;
        }
        // todo: check mace swap

        @Override
        public void onEnable() {
            stateMachine.setState(STATE_NONE);
        }

        @Override
        public void onDisable() {}

        @Override
        public synchronized void onHit(int type) {
            // pull up only when after mace attack to miss
            if (lastMaceAttackTick > Tasks.getTick() - 5 && stateMachine.getState() != STATE_PULL_UP) {
                lastMaceAttackSuccessTick = Tasks.getTick();
                stateMachine.setState(STATE_PULL_UP);
            }
        }

        @Override
        public abstract void onAttack(Entity entity);
    }

    public static class MaceArua extends AbstractMaceBehaviour implements HitListener {

        public int onStatePullUp(StateMachine machine) {
            net.minecraft.world.phys.Vec3 testMovement = new net.minecraft.world.phys.Vec3(0, 0.1, 0);
            net.minecraft.world.phys.Vec3 simulation = MovTasks.simulateMovement(mc.player, mc.player.position(), testMovement, true);
            boolean simulationHead = simulation.distanceToSqr(testMovement) > 1E-4;
            boolean shouldForcePullUp = shouldPullUpEating();
            net.minecraft.world.phys.Vec3 predictor = base.maceUsePredictor.get()
                    ? PositionPredict.INSTANCE.attackPredictArgument.get().predict(base.target)
                    : base.target.position();
            if (shouldForcePullUp) {
                setTargetToEat(predictor, simulationHead);
                machine.markForEndState();
                return STATE_PULL_UP;
            }
            if (simulationHead) {
                // can not pull up
                return STATE_FOLLOW;
            } else {
                {
                    double targetY = base.target.getY() + base.maceHeight.get();
                    // check pulling time
                    boolean mayFollow = (mc.player.getY() >= targetY)
                            || (mc.player.getY() > base.target.getY()
                                    && startPullUpTick != 0
                                    && Tasks.getTick()
                                            > base.maceRemainPullUpTick.get()
                                                    + startPullUpTick
                                                    + base.maceHeight.get());

                    if (mayFollow
                            && base.maceChaseFollowYBias.get().isPresent()
                            && mc.player.getY() > base.target.getY()
                            && !base.currentInCombatRange) {
                        double bias = base.maceChaseFollowYBias.get().getValue();
                        net.minecraft.world.phys.Vec3 vec3d = predictor.subtract(mc.player.position());
                        double xz = Math.max(Math.abs(vec3d.x), Math.abs(vec3d.z));
                        double y = Math.abs(vec3d.y);
                        // do not follow if distance not close enough
                        if (y < xz + bias) {
                            mayFollow = false;
                        }
                    }
                    if (!mayFollow) {
                        setTargetToPlayerUpper(predictor);
                        machine.markForEndState();
                        return STATE_PULL_UP;
                    } else {
                        return STATE_FOLLOW;
                    }
                }
            }
        }

        boolean currentTargetUpFly = false;

        public int onStateFollow(StateMachine machine) {
            if (PlayerStateManager.INSTANCE.fallDistance < 1E-6 && lastFallDistance > 1E-6) {
                // we trigger falldistance reset during chase
                return STATE_PULL_UP;
            }
            // already reach the target
            if (shouldPullUpEating()) {
                return STATE_PULL_UP;
            }
            net.minecraft.world.phys.Vec3 targetPos = base.maceUsePredictor.get()
                    ? PositionPredict.INSTANCE.attackPredictArgument.get().predict(base.target)
                    : base.target.position();
            double lerpY = base.maceYLevelWeight.get();
            double baseY = base.target.getY();
            currentTargetUpFly = baseY + 0.5 < targetPos.y;
            double yLerp = currentTargetUpFly ? (targetPos.y * lerpY + baseY * (1.0D - lerpY)) : targetPos.y;
            targetPos = targetPos.with(Direction.Axis.Y, yLerp);
            boolean mayAttack = shouldAttackSimple() || shouldAttackMace();
            boolean targetInRange = TargetSelector.INSTANCE.isWithinAttackRange(
                    mc.player.position(),
                    base.target.getBoundingBox(),
                    CombatTasks.getCombatExtra().getAttackAtTargetRange(base.target));
            // 限制高度 但是对面是往上飞的 不需要
            if (mayAttack && targetInRange) {
                setTargetToPlayer(targetPos);
                scheduleAttack();
                machine.markForEndState();
                if (base.flyAntiSpearUseSpearResetWhenFollow.get()) {
                    SpearEnhance.INSTANCE.setForceSpearReset(true);
                }
                return STATE_WAIT_ATTACK;
            } else {
                net.minecraft.world.phys.Vec3 testMovement = new net.minecraft.world.phys.Vec3(0, -0.1, 0);
                net.minecraft.world.phys.Vec3 simulation = MovTasks.simulateMovement(mc.player, mc.player.position(), testMovement, true);
                boolean simulationFeet = simulation.distanceToSqr(testMovement) > 1E-4;
                // do not follow because no enough height and other people will overhead us
                boolean pullUp = simulationFeet;
                double minimalHeight = base.maceFollowMinHeight.get();
                // shit
                if (!pullUp && base.target.getY() > mc.player.getY() + minimalHeight) {
                    pullUp = true;
                }
                if (pullUp) {
                    if (targetInRange) {
                        setTargetToPlayer(targetPos);
                        scheduleAttack();
                        machine.markForEndState();
                        return STATE_WAIT_ATTACK;
                    } else {
                        return STATE_PULL_UP;
                    }
                }
                setTargetToPlayer(targetPos);
            }
            if (base.flyAntiSpearUseSpearResetWhenFollow.get()) {
                SpearEnhance.INSTANCE.setForceSpearReset(true);
            }
            machine.markForEndState();
            return STATE_FOLLOW;
        }

        private void setTargetToEat(net.minecraft.world.phys.Vec3 predictor, boolean headSimulation) {
            if (headSimulation) {
                net.minecraft.world.phys.Vec3 vec3d = base.target.position().subtract(mc.player.position());
                if (vec3d.horizontalDistance() > base.combatRange()) {
                    setTargetToPlayerUpper(predictor);
                } else {
                    movementDirection =
                            new net.minecraft.world.phys.Vec3(-vec3d.x, 0, -vec3d.z).normalize().scale(10);
                }

            } else {
                setTargetToPlayerUpper(predictor);
            }
        }

        protected void setTargetToPlayerUpper(net.minecraft.world.phys.Vec3 predictor) {

            net.minecraft.world.phys.Vec3 movement = null;
            boolean onGroundSupport = base.currentOnGround;
            boolean executeSmoothHideFlight = false;
            boolean antiSpear = willUseAntiSpear(base.flyAntiSpearWhenPullup.get(), predictor)
                    && (mc.player.getY() > predictor.y()
                            || mc.player.position().subtract(predictor).horizontalDistance() < base.combatSpearRange.get());
            boolean yLow = predictor.y() >= mc.player.getY();
            if (base.combatSmoothFlight1.get() && !onGroundSupport) {
                double combatRange = base.combatMaceRange.get();
                if (base.combatSmoothArg14.get().isPresent()) {
                    me.matl114.hacks.utils.config.Vec3 arguments = base.combatSmoothArg14.get().getValue();
                    double predictorYL = predictor.y - mc.player.getY();
                    if (predictorYL > arguments.x()) {
                        combatRange += Math.min(predictorYL - arguments.x(), arguments.z()) * arguments.y();
                    }
                }
                boolean mayCombatFlight = !base.combatSmoothFlag2.get()
                        || (base.currentAction == TargetAction.CIRCLING
                                || base.currentAction == TargetAction.TOWARDS
                                || base.currentAction == TargetAction.SLOW_SPEED);
                if (mayCombatFlight && yLow) {
                    net.minecraft.world.phys.Vec3 center = base.target.dimensions.makeBoundingBox(predictor).getCenter();
                    {
                        double radius = combatRange + base.combatSmoothArg1.get();
                        Pair<net.minecraft.world.phys.Vec3, net.minecraft.world.phys.Vec3> tangents =
                                MathUtils.getTangentWithSameXZ(center, radius, mc.player.getEyePosition());
                        net.minecraft.world.phys.Vec3 vec3d = tangents.getFirst();
                        net.minecraft.world.phys.Vec3 vec3d2 = tangents.getSecond();
                        net.minecraft.world.phys.Vec3 vec3d3 = vec3d.y < vec3d2.y ? vec3d2 : vec3d;
                        // go upper not horizontal
                        vec3d3 = vec3d3.add(0, 1E-2, 0);
                        if (Math.abs(base.combatSmoothArg11.get()) > 1E-6
                                && (base.combatSmoothArg15.get()
                                                ? center.subtract(mc.player.getEyePosition())
                                                        .horizontalDistanceSqr()
                                                : center.distanceToSqr(mc.player.getEyePosition()))
                                        < MathUtils.s2(base.combatSmoothArg16.get())) {
                            // 垂线
                            vec3d3 = vec3d3.normalize();
                            net.minecraft.world.phys.Vec3 delta = mc.player.getEyePosition().subtract(center);
                            net.minecraft.world.phys.Vec3 horizontalMul =
                                    new net.minecraft.world.phys.Vec3(delta.x, 0, delta.z).normalize().scale(base.combatSmoothArg11.get());
                            vec3d3 = vec3d3.add(horizontalMul).normalize();
                        }
                        if (vec3d3.y > 0) {
                            movement = vec3d3.normalize().scale(10);
                        }
                        executeSmoothHideFlight =
                                center.distanceToSqr(mc.player.getEyePosition()) < MathUtils.s2(radius);
                    }
                }
            }
            if (movement == null
                    && base.combatSmoothFlight2.get()
                    && base.currentAction != TargetAction.ESCAPING
                    && !onGroundSupport) {
                if (predictor.y() < mc.player.getY()
                        && predictor.y() + base.combatSmoothArg22.get() > mc.player.getY()
                        && predictor.subtract(mc.player.position()).horizontalDistance() < base.combatSmoothArg21.get()) {
                    movement = new net.minecraft.world.phys.Vec3(0, 10, 0);
                    if (base.angleOptimizePullUp.get() && base.angleOptimizeRadicalPullup.get()) {
                        net.minecraft.world.phys.Vec3 direction = predictor
                                .subtract(mc.player.position())
                                .normalize()
                                .scale(10);
                        movement = new net.minecraft.world.phys.Vec3(direction.x, 10, direction.z);
                    }
                }
            }
            if (movement == null) {
                // normal pull up
                movement = predictor
                        .with(Direction.Axis.Y, (predictor.y() + (base.maceHeight.get())))
                        .subtract(mc.player.position());
                if (movement.length() < 5) {
                    movement = movement.normalize().scale(5);
                }
            }
            if (base.pullUpAngleOptimize.get().isPresent() && !onGroundSupport && !executeSmoothHideFlight) {
                double horizontalDistance = movement.horizontalDistance();
                if (horizontalDistance > 1E-1) {
                    net.minecraft.world.phys.Vec3 lastMovement = PlayerStateManager.INSTANCE.lastKnownRealMovementSpeed;
                    net.minecraft.world.phys.Vec3 lastHorizontal = lastMovement.with(Direction.Axis.Y, 0);
                    double distance = base.pullUpAngleOptimize.get().getValue();
                    if (distance > horizontalDistance) {
                        if (lastHorizontal.dot(movement) < 0) {
                            movement = movement.multiply(-1, 1, -1);
                        }
                    }
                }
            }
            movementDirection = movement;

            if (!base.flyAntiSpearAfterAngle.get() && antiSpear) {
                antiSpear(base.flyAntiSpearWhenPullup.get());
            }
            if (base.angleOptimizePullUp.get()) {
                if (movementDirection.y > 1E-6) {
                    double horizontal = mc.player.position().subtract(predictor).horizontalDistance();
                    if (base.angleOptimizeRadicalPullup.get()) {
                        if (executeSmoothHideFlight) {
                            double horizontal2 = Math.max(Math.abs(movementDirection.x), Math.abs(movementDirection.z));
                            double sgnX = MathUtils.sgn(movementDirection.x);
                            double sgnZ = MathUtils.sgn(movementDirection.z);
                            movementDirection = new net.minecraft.world.phys.Vec3(sgnX * horizontal2, movementDirection.y, sgnZ * horizontal2);
                        }
                        if (!executeSmoothHideFlight
                                && yLow
                                && base.angleOptimizePullRange.get().isPresent()) {
                            var range = base.angleOptimizePullRange.get().getValue();
                            if (horizontal < range) {
                                net.minecraft.world.phys.Vec3 horizontalDelta = mc.player
                                        .position()
                                        .subtract(predictor)
                                        .with(Direction.Axis.Y, 0)
                                        .normalize()
                                        .scale(6);
                                movementDirection = horizontalDelta.with(Direction.Axis.Y, movementDirection.y);
                            }
                        }
                    } else {
                        double horizontal2 = Math.max(Math.abs(movementDirection.x), Math.abs(movementDirection.z));
                        if (horizontal2 > 0.1) {
                            if (executeSmoothHideFlight) {
                                double sgnX = MathUtils.sgn(movementDirection.x);
                                double sgnZ = MathUtils.sgn(movementDirection.z);
                                movementDirection = new net.minecraft.world.phys.Vec3(sgnX * horizontal2, horizontal2, sgnZ * horizontal2);
                            } else {
                                movementDirection = movementDirection.with(Direction.Axis.Y, horizontal2);
                            }
                        }
                    }
                    movementDirection = ElytraOptimizeUtils.calculateBestPullupSpeed(movementDirection);
                }
            }
            double len = movementDirection.length();
            if (len < 5) {
                movementDirection = movementDirection.normalize().scale(5);
            }
            if (base.flyAntiSpearAfterAngle.get() && antiSpear) {
                antiSpear(base.flyAntiSpearWhenPullup.get());
            }
        }

        protected void setTargetToPlayer(net.minecraft.world.phys.Vec3 targetPos) {
            movementDirection = calculateTargetDirection(targetPos);
            boolean onGroundSupport = base.currentOnGround;
            if (onGroundSupport) {
                // handle on ground target\
                var op = base.followOnGroundHeight.get();
                if (op.isPresent()) {
                    movementDirection = movementDirection.add(0, op.getValue(), 0);
                }
            } else {
                // use real value, because predictors may predict wrong values
                if (targetPos.y > mc.player.getY()) {
                    // to nothing modify
                    movementDirection = movementDirection.with(Direction.Axis.Y, 0);
                    // 我没招了。这还是尽快重开吧
                }
            }
            boolean antiSpear = willUseAntiSpear(base.flyAntiSpear.get(), targetPos);
            // only optimize when target offground
            if (!base.flyAntiSpearAfterAngle.get() && antiSpear) {
                antiSpear(base.flyAntiSpear.get());
            }
            if (!onGroundSupport && base.angleOptimizeFollow.get()) {
                if (movementDirection.y < -1E-6) {
                    var distancePair = base.angleOptimizeFollowRange.get();
                    double distance;
                    if (SpearEnhance.isUsingSpear(mc.player)) {
                        distance = distancePair.y();
                    } else if (antiSpear) {
                        distance = distancePair.z();
                    } else {
                        distance = distancePair.x();
                    }
                    double horizontal = mc.player.position().subtract(targetPos).length();
                    if (horizontal > distance) {
                        double horizontal2 = Math.max(Math.abs(movementDirection.x), Math.abs(movementDirection.z));
                        if (horizontal2 > 0.1) {
                            // rescale
                            if (!base.angleOptimizeRadicalFollow.get().isPresent()
                                    || Math.abs(movementDirection.y)
                                            < movementDirection.horizontalDistance()
                                                    * Math.tan(Math.toRadians(base.angleOptimizeRadicalFollow
                                                            .get()
                                                            .getValue()))) {
                                if (Math.abs(movementDirection.y) > horizontal2) {
                                    movementDirection = movementDirection.with(Direction.Axis.Y, -horizontal2);
                                }
                                movementDirection =
                                        ElytraOptimizeUtils.calculateBestDownForwardSpeed(movementDirection, true);
                            }
                        }
                    }
                }
            }
            double len = movementDirection.length();
            if (len < 5) {
                movementDirection = movementDirection.normalize().scale(5);
            }
            if (base.flyAntiSpearAfterAngle.get() && antiSpear) {
                antiSpear(base.flyAntiSpear.get());
            }
        }

        @Override
        public synchronized void onUpdate() {
            currentTargetUpFly = false;
            super.onUpdate();
        }

        @Override
        public synchronized void onAttack(Entity entity) {
            if ((mc.player.isFallFlying() || mc.player.getAbilities().flying)
                    && stateMachine.getState() != STATE_PULL_UP) {
                // just in few ticks for the attack(current tick)
                // pull up only when after mace attack to miss
                if (lastMaceAttackTick >= Tasks.getTick() - 1 && currentTargetUpFly) {
                    stateMachine.setState(STATE_PULL_UP);
                    stateMachine.step();
                } else {
                    stateMachine.setState(STATE_FOLLOW);
                }
            }
        }
    }

    public static class MaceAuraGround extends AbstractMaceBehaviour implements HitListener {
        private void setTargetToEat(net.minecraft.world.phys.Vec3 predictor, boolean headSimulation) {
            if (headSimulation) {
                net.minecraft.world.phys.Vec3 vec3d = base.target.position().subtract(mc.player.position());
                if (vec3d.horizontalDistance() > base.combatRange()) {
                    setTargetToPlayerUpper(predictor);
                } else {
                    movementDirection =
                            new net.minecraft.world.phys.Vec3(-vec3d.x, 0, -vec3d.z).normalize().scale(10);
                }

            } else {
                setTargetToPlayerUpper(predictor);
            }
        }

        @Override
        public int onStatePullUp(StateMachine machine) {
            boolean shouldForcePullUp = shouldPullUpEating();
            boolean shouldFollow;
            net.minecraft.world.phys.Vec3 testMovement = new net.minecraft.world.phys.Vec3(0, 0.1, 0);
            net.minecraft.world.phys.Vec3 simulation = MovTasks.simulateMovement(mc.player, mc.player.position(), testMovement, true);
            boolean simulationHead = simulation.distanceToSqr(testMovement) > 1E-4;
            double targetY = base.target.getY() + base.maceHeightGround.get();
            // check pulling time
            boolean mayFollow = (mc.player.getY() >= targetY)
                    || (mc.player.getY() > base.target.getY()
                            && startPullUpTick != 0
                            && Tasks.getTick()
                                    > base.maceRemainPullUpTick.get() + startPullUpTick + base.maceHeightGround.get());
            shouldFollow = simulationHead || mayFollow;
            if (shouldFollow) {
                if (!shouldForcePullUp) {
                    return STATE_FOLLOW;
                } else {
                    setTargetToEat(base.target.position(), simulationHead);
                    machine.markForEndState();
                    return STATE_PULL_UP;
                }
            } else {
                net.minecraft.world.phys.Vec3 predictor = base.maceUsePredictor.get()
                        ? PositionPredict.INSTANCE.attackPredictArgument.get().predict(base.target)
                        : base.target.position();
                setTargetToPlayerUpper(predictor);
                machine.markForEndState();
                return STATE_PULL_UP;
            }
        }

        @Override
        public int onStateFollow(StateMachine machine) {
            if (PlayerStateManager.INSTANCE.fallDistance < 1E-6 && lastFallDistance > 1E-6) {
                // we trigger falldistance reset during chase
                return STATE_PULL_UP;
            }
            if (shouldPullUpEating()) {
                return STATE_PULL_UP;
            }
            net.minecraft.world.phys.Vec3 targetPos = base.maceUsePredictor.get()
                    ? PositionPredict.INSTANCE.attackPredictArgument.get().predict(base.target)
                    : base.target.position();
            boolean targetInRange = TargetSelector.INSTANCE.isWithinAttackRange(
                    mc.player.position(),
                    base.target.getBoundingBox(),
                    CombatTasks.getCombatExtra().getAttackAtTargetRange(base.target));
            if (base.flyAntiSpearUseSpearResetWhenFollow.get()) {
                SpearEnhance.INSTANCE.setForceSpearReset(true);
            }
            if (base.currentOnGround) {
                if (targetInRange) {
                    setTargetToPlayer(base.target.position());
                    scheduleAttack();
                    machine.markForEndState();
                    return STATE_WAIT_ATTACK;
                } else {
                    setTargetToPlayer(base.target.position());
                    net.minecraft.world.phys.Vec3 testMovement = movementDirection.normalize().scale(0.1);
                    net.minecraft.world.phys.Vec3 simulation = MovTasks.simulateMovement(mc.player, mc.player.position(), testMovement, true);
                    boolean simulationFeet = simulation.distanceToSqr(testMovement) > 1E-4;
                    if (simulationFeet) {
                        // reset movement
                        movementDirection = net.minecraft.world.phys.Vec3.ZERO;
                        return STATE_PULL_UP;
                    } else {
                        machine.markForEndState();
                        return STATE_FOLLOW;
                    }
                }
            } else {
                net.minecraft.world.phys.Vec3 testVelocity = new net.minecraft.world.phys.Vec3(0, 1, 0);
                net.minecraft.world.phys.Vec3 simulation = MovTasks.simulateMovement(base.target, targetPos, testVelocity, false);
                boolean targetHeadSimulation = simulation.distanceToSqr(testVelocity) > 1E-4;
                if (targetHeadSimulation) {
                    net.minecraft.world.phys.Vec3 target = targetPos.add(0, -1.62 - 2.8 + simulation.length(), 0);
                    movementDirection = target.subtract(mc.player.position());
                    if (movementDirection.y > -1 && targetInRange) {
                        scheduleAttack();
                        machine.markForEndState();
                        return movementDirection.y > 0 ? STATE_PULL_UP : STATE_FOLLOW;
                    } else if (movementDirection.y > 0) {
                        return STATE_PULL_UP;
                    } else {
                        machine.markForEndState();
                        return STATE_FOLLOW;
                    }
                } else {
                    if (targetInRange) {
                        setTargetToPlayer(base.target.position());
                        scheduleAttack();
                        machine.markForEndState();
                        return STATE_FOLLOW;
                    } else {
                        setTargetToPlayer(targetPos);
                        if (movementDirection.y > 0) {
                            movementDirection = net.minecraft.world.phys.Vec3.ZERO;
                            return STATE_PULL_UP;
                        } else {
                            machine.markForEndState();
                            return STATE_FOLLOW;
                        }
                    }
                }
            }
        }

        @Override
        protected void setTargetToPlayerUpper(net.minecraft.world.phys.Vec3 predictor) {
            if (base.currentOnGround) {
                if (predictor.y > mc.player.getY()) {
                    movementDirection = predictor
                            .with(Direction.Axis.Y, (predictor.y() + (base.maceHeightGround.get())))
                            .subtract(mc.player.position());
                } else {
                    net.minecraft.world.phys.Vec3 delta = mc.player.position().subtract(predictor);
                    double horizontal = delta.horizontalDistance();
                    double height = base.maceHeightGround.get();
                    if (horizontal < height && delta.y > horizontal) {
                        movementDirection = delta;
                    } else {
                        net.minecraft.world.phys.Vec3 targetPos = delta.with(Direction.Axis.Y, 0)
                                .normalize()
                                .scale(height)
                                .with(Direction.Axis.Y, height);
                        movementDirection = targetPos.subtract(delta);
                    }
                }
            } else {
                net.minecraft.world.phys.Vec3 testVelocity = new net.minecraft.world.phys.Vec3(0, 1, 0);
                net.minecraft.world.phys.Vec3 simulation = MovTasks.simulateMovement(base.target, predictor, testVelocity, false);
                boolean targetHeadSimulation = simulation.distanceToSqr(testVelocity) > 1E-4;
                // no space above target
                if (targetHeadSimulation) {
                    movementDirection = predictor.add(testVelocity).subtract(mc.player.position());
                } else {
                    net.minecraft.world.phys.Vec3 movement = null;
                    boolean yLow = predictor.y() >= mc.player.getY();
                    if (base.combatSmoothFlight1.get()) {
                        double combatRange = base.combatMaceRange.get();
                        if (base.combatSmoothArg14.get().isPresent()) {
                            me.matl114.hacks.utils.config.Vec3 arguments = base.combatSmoothArg14.get().getValue();
                            double predictorYL = predictor.y - mc.player.getY();
                            if (predictorYL > arguments.x()) {
                                combatRange += Math.min(predictorYL - arguments.x(), arguments.z()) * arguments.y();
                            }
                        }
                        combatRange = combatRange / 2;
                        boolean mayCombatFlight = !base.combatSmoothFlag2.get()
                                || (base.currentAction == TargetAction.CIRCLING
                                        || base.currentAction == TargetAction.TOWARDS
                                        || base.currentAction == TargetAction.SLOW_SPEED);
                        if (mayCombatFlight && yLow) {
                            net.minecraft.world.phys.Vec3 center =
                                    base.target.dimensions.makeBoundingBox(predictor).getCenter();
                            double radius = combatRange + base.combatSmoothArg1.get();
                            Pair<net.minecraft.world.phys.Vec3, net.minecraft.world.phys.Vec3> tangents =
                                    MathUtils.getTangentWithSameXZ(center, radius, mc.player.getEyePosition());
                            net.minecraft.world.phys.Vec3 vec3d = tangents.getFirst();
                            net.minecraft.world.phys.Vec3 vec3d2 = tangents.getSecond();
                            net.minecraft.world.phys.Vec3 vec3d3 = vec3d.y < vec3d2.y ? vec3d2 : vec3d;
                            // go upper not horizontal
                            vec3d3 = vec3d3.add(0, 1E-2, 0);
                            if (Math.abs(base.combatSmoothArg11.get()) > 1E-6
                                    && ((base.combatSmoothArg15.get()
                                                    ? center.subtract(mc.player.getEyePosition())
                                                            .horizontalDistanceSqr()
                                                    : center.distanceToSqr(mc.player.getEyePosition()))
                                            < MathUtils.s2(base.combatSmoothArg16.get()))) {
                                // 垂线
                                vec3d3 = vec3d3.normalize();
                                net.minecraft.world.phys.Vec3 delta = mc.player.getEyePosition().subtract(center);
                                net.minecraft.world.phys.Vec3 horizontalMul = new net.minecraft.world.phys.Vec3(delta.x, 0, delta.z)
                                        .normalize()
                                        .scale(base.combatSmoothArg11.get());
                                vec3d3 = vec3d3.add(horizontalMul).normalize();
                            }
                            if (vec3d3.y > 0) {
                                movement = vec3d3.normalize().scale(10);
                            }
                        }
                    }
                    if (movement == null) {
                        movement = predictor
                                .with(Direction.Axis.Y, (predictor.y() + (base.maceHeightGround.get())))
                                .subtract(mc.player.position());
                        if (movement.length() < 5) {
                            movement = movement.normalize().scale(5);
                        }
                    }
                    movementDirection = movement;
                }
            }
            if (movementDirection.length() < 5) {
                movementDirection = movementDirection.normalize().scale(5);
            }
        }

        @Override
        protected void setTargetToPlayer(net.minecraft.world.phys.Vec3 targetPos) {
            net.minecraft.world.phys.Vec3 playerPos = mc.player.position();
            net.minecraft.world.phys.Vec3 fallbackTargetPos;
            if (base.currentOnGround) {
                fallbackTargetPos =
                        targetPos.add(0, base.followOnGroundHeight.get().orElse(0.5), 0);
                // in case of landing
                ElytraExtra.INSTANCE.autoTakeoff();
            } else {
                fallbackTargetPos = targetPos;
            }
            boolean found = false;
            if (base.currentOnGround) {
                double attackAtTargetRange = CombatTasks.getCombatExtra().getAttackAtTargetRange(base.target);
                net.minecraft.world.phys.Vec3 targetEyePos = base.target.getEyePosition();
                BlockHitResult raycastResult = mc.level.clip(new ClipContext(
                        mc.player.getEyePosition(),
                        targetEyePos,
                        net.minecraft.world.level.ClipContext.Block.COLLIDER,
                        net.minecraft.world.level.ClipContext.Fluid.NONE,
                        mc.player));
                if (raycastResult != null
                        && raycastResult.getType() != HitResult.Type.MISS
                        && raycastResult.getLocation().distanceToSqr(targetEyePos)
                                <= MathUtils.s2(2.0D * attackAtTargetRange)) {
                    BlockPos raycastPos = net.minecraft.core.BlockPos.containing(raycastResult.getLocation());
                    int searchRadius = Math.max(1, (int) Math.ceil(attackAtTargetRange));
                    List<BlockPos> searchPoses = new ArrayList<>();
                    for (int x = raycastPos.getX() - searchRadius; x <= raycastPos.getX() + searchRadius; x++) {
                        for (int y = raycastPos.getY() - searchRadius; y <= raycastPos.getY() + searchRadius; y++) {
                            for (int z = raycastPos.getZ() - searchRadius; z <= raycastPos.getZ() + searchRadius; z++) {
                                searchPoses.add(new BlockPos(x, y, z));
                            }
                        }
                    }
                    searchPoses.sort(
                            Comparator.comparingDouble(pos -> Vec3.atCenterOf(pos).distanceToSqr(targetEyePos)));
                    for (BlockPos pos : searchPoses) {
                        net.minecraft.world.phys.Vec3 centerPos = Vec3.atCenterOf(pos);
                        if (!TargetSelector.INSTANCE.isWithinAttackRange(
                                centerPos, base.target.getBoundingBox(), attackAtTargetRange + 0.5D)) {
                            continue;
                        }
                        if (mc.level
                                        .clip(new ClipContext(
                                                playerPos,
                                                centerPos,
                                                ClipContext.Block.COLLIDER,
                                                ClipContext.Fluid.NONE,
                                                mc.player))
                                        .getType()
                                != HitResult.Type.MISS) {
                            continue;
                        }
                        AABB box = mc.player.dimensions.makeBoundingBox(centerPos);
                        if (!mc.level.noCollision(box)) {
                            continue;
                        }
                        movementDirection = centerPos.subtract(playerPos);
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                movementDirection =
                        calculateTargetDirection(fallbackTargetPos); // fallbackTargetPos.subtract(playerPos);
            }
            if (movementDirection.length() < 5 && movementDirection.length() > 1E-6) {
                movementDirection = movementDirection.normalize().scale(5);
            }
        }

        @Override
        public void onAttack(Entity entity) {
            if ((mc.player.isFallFlying() || mc.player.getAbilities().flying)
                    && stateMachine.getState() != STATE_FOLLOW) {
                // just in few ticks for the attack(current tick)
                // pull up only when after mace attack to miss
                stateMachine.setState(STATE_FOLLOW);
            }
        }
    }

    public static class SpearAura extends AbstractBotBehaviour implements HitListener {
        static final int STATE_NONE = 0;
        static final int STATE_FOLLOW = 1;
        static final int STATE_NEAR_FOLLOW = 2;
        static final int STATE_PULL_OVER = 3;
        int nearFollowTimer;
        int pullOverTimer;
        StateMachine stateMachine;

        //        @Override
        //        public Entity searchTarget() {
        //            return CombatTasks.getTargetSelector()
        //                    .searchAttackEntity(base.targetRange.get(), true, TargetSelector::canPlayerDirectlySee);
        //        }

        public SpearAura() {
            this.stateMachine = new StateMachine(
                    STATE_NONE,
                    this::onStateUpdate,
                    this::onStateNone,
                    this::onStateFollow,
                    this::onStateNearFollow,
                    this::onStatePullOver);
            this.stateMachine.registerListener(STATE_NEAR_FOLLOW, this::onSwitchToNearFollow);
            this.stateMachine.registerListener(STATE_PULL_OVER, this::onSwitchToPullOver);
        }

        public int onStateUpdate(StateMachine machine, int state) {
            if (base.target == null) {
                return STATE_NONE;
            }
            return state;
        }

        public int onStateNone(StateMachine machine) {
            if (base.target != null) {
                if (!VItem.getInstance().isSpear(mc.player.getMainHandItem())
                        && !VItem.getInstance().isSpear(mc.player.getOffhandItem())) {
                    base.logI18N("message.module.elytra-bot.no-spear");
                }
                return STATE_FOLLOW;
            }
            machine.markForEndState();
            movementDirection = net.minecraft.world.phys.Vec3.ZERO;
            return STATE_NONE;
        }

        private net.minecraft.world.phys.Vec3 getTargetPosition() {
            net.minecraft.world.phys.Vec3 predictedPosition = base.spearUsePredictor.get()
                    ? PositionPredict.INSTANCE
                            .getPredictor(base.target)
                            .predict(2, PositionPredict.Mode.PREDICTOR_NV.ordinal(), 10)
                    : base.target.position();
            net.minecraft.world.phys.Vec3 delta = predictedPosition.subtract(base.target.position());
            if (base.currentOnGround) {
                return base.target.getEyePosition().add(delta);
            }

            return base.target.getBoundingBox().getCenter().add(delta);
        }

        public int onStateFollow(StateMachine machine) {
            net.minecraft.world.phys.Vec3 targetPos = getTargetPosition();
            if (mc.player.getEyePosition().distanceToSqr(targetPos) < MathUtils.s2(getActiveRange())) {
                return STATE_NEAR_FOLLOW;
            }
            machine.markForEndState();
            /// compute their
            net.minecraft.world.phys.Vec3 originalLook = targetPos.subtract(mc.player.getEyePosition());
            if (originalLook.length() < 6) {
                originalLook = originalLook.normalize().scale(6);
            }
            if (canAdjustMovement()) {
                if (adjustMovementForSpear((Player) base.target, originalLook, false)) {
                    return STATE_FOLLOW;
                }
            }
            movementDirection = originalLook;
            return STATE_FOLLOW;
        }

        private boolean canAdjustMovement() {
            return base.spearAntiSpear.get() && base.isTargetUsingSpear();
        }

        private boolean adjustMovementForSpear(Player otherShit, net.minecraft.world.phys.Vec3 originalLook, boolean expand) {
            // shit not work
            // handle their shit ass spear

            // filter run away
            //            if (otherShit.getRotationVector().dotProduct(mc.player.getPos().subtract(otherShit.getPos()))
            // < 0) {
            //                return false;
            //            }
            if (otherShit.distanceToSqr(mc.player.position())
                    < MathUtils.s2(getActiveRange() * 2 + base.spearAntiSpearExtraDistance.get())) {
                if (Tasks.getTick() % 5 < 2) {
                    return moveAdjust(originalLook);
                } else {
                    return movementPredictAdjust(originalLook);
                }
            }

            AABB ourBox = expand ? mc.player.getBoundingBox().inflate(0.85, 0.85, 0.85) : mc.player.getBoundingBox();
            net.minecraft.world.phys.Vec3 theirKnownMovement = PositionPredict.INSTANCE.predictKnownMovement(otherShit);
            net.minecraft.world.phys.Vec3 theirPredictedPos = PositionPredict.INSTANCE
                    .spearPredictArgument
                    .get()
                    .predict(otherShit); // predictFlyingPosition(otherShit, 2, 6);
            net.minecraft.world.phys.Vec3 facing = otherShit.getLookAngle();
            Debug.debug("Spear judgement", theirKnownMovement, theirPredictedPos, mc.player.position());
            double reachD = theirKnownMovement.dot(facing);
            net.minecraft.world.phys.Vec3 theirPredictedEyePos = theirPredictedPos.add(0, otherShit.getEyeHeight(otherShit.getPose()), 0);
            net.minecraft.world.phys.Vec3 raycastStart = theirPredictedEyePos.add(facing.scale(getMinRange()));
            net.minecraft.world.phys.Vec3 raycastEnd = theirPredictedEyePos.add(
                    facing.scale(getActiveRange() + reachD + base.spearAntiSpearExtraDistance.get()));
            if (ourBox.clip(raycastStart, raycastEnd).isPresent()) {
                return moveAdjust(originalLook);
            }
            return false;
            // do spear raytrace
        }

        private boolean moveAdjust(net.minecraft.world.phys.Vec3 originalLook) {
            //
            Debug.debug("Judget may hit");
            net.minecraft.world.phys.Vec3 originalLookHorizontal = originalLook.horizontal();
            net.minecraft.world.phys.Vec3 vertical = new net.minecraft.world.phys.Vec3(0, 1, 0);
            net.minecraft.world.phys.Vec3 side = vertical.cross(originalLookHorizontal);
            net.minecraft.world.phys.Vec3 revertDirection =
                    side.normalize().scale(originalLookHorizontal.length()).add(0, originalLookHorizontal.y, 0);
            net.minecraft.world.phys.Vec3 testVector = revertDirection.normalize().scale(0.5);
            net.minecraft.world.phys.Vec3 simulate = MovTasks.simulateMovement(mc.player, mc.player.position(), testVector, false);
            if (simulate.distanceToSqr(testVector) < 0.1) {
                movementDirection = revertDirection;
                Debug.debug("JudgeA", movementDirection);
                RenderTasks.drawBoxMov(
                        mc.player.getBoundingBox(), revertDirection.normalize().scale(1.7), 50, Color.BLUE);
                return true;
            } else {
                revertDirection = revertDirection.reverse();
                testVector = testVector.reverse();
                simulate = MovTasks.simulateMovement(mc.player, mc.player.position(), testVector, false);
                if (simulate.distanceToSqr(testVector) < 0.1) {
                    movementDirection = revertDirection;
                    Debug.debug("JudgeB", movementDirection);
                    RenderTasks.drawBoxMov(
                            mc.player.getBoundingBox(),
                            revertDirection.normalize().scale(1.7),
                            50,
                            Color.BLUE);
                    return true;
                }
            }
            return false;
        }

        private boolean movementPredictAdjust(net.minecraft.world.phys.Vec3 originalLook) {
            return false;
        }

        public int onStateNearFollow(StateMachine machine) {
            if ((base.currentInCombatRange && base.currentAction == TargetAction.CIRCLING)
                    || base.currentAction == TargetAction.TOWARDS
                    || base.currentAction == TargetAction.AFK) {
                if (!SpearEnhance.canSpearKineticAttack()) {
                    return STATE_PULL_OVER;
                }
            }
            net.minecraft.world.phys.Vec3 targetPosition = getTargetPosition();
            if (mc.player.getEyePosition().distanceToSqr(targetPosition) > MathUtils.s2(getActiveRange())) {
                return STATE_FOLLOW;
            } else {
                //                else if (++nearFollowTimer > getMaxAttackPeriod()) {
                //                    // catch up
                //                    state = STATE_PULL_OVER;
                //                    nearFollowTimer = 0;
                //                }
                machine.markForEndState();
                net.minecraft.world.phys.Vec3 look = targetPosition.subtract(mc.player.getEyePosition());
                if (look.length() < 6) {
                    look = look.normalize().scale(6);
                }
                if (canAdjustMovement()) {
                    if (adjustMovementForSpear((Player) base.target, look, false)) {
                        return STATE_NEAR_FOLLOW;
                    }
                }
                net.minecraft.world.phys.Vec3 lookHorizontal = look.with(Direction.Axis.Y, 0);
                net.minecraft.world.phys.Vec3 lastMoveHorizontal =
                        PlayerStateManager.INSTANCE.lastKnownRealMovementSpeed.with(Direction.Axis.Y, 0);
                double dotValue = lookHorizontal.dot(lastMoveHorizontal);
                if (dotValue < 0) {
                    look = look.reverse();
                }
                movementDirection = look;
                return STATE_NEAR_FOLLOW;
            }
        }
        // todo: howto when combating
        public int onStatePullOver(StateMachine machine) {
            net.minecraft.world.phys.Vec3 targetPosition;
            if (++pullOverTimer > getCooldown()) {
                return STATE_FOLLOW;
            } else {
                targetPosition = getTargetPosition();
                // calculate left time
                if (base.currentAction != null) {
                    if (base.currentAction == TargetAction.AFK
                            || base.currentAction == TargetAction.SLOW_SPEED
                            || (base.currentInCombatRange && base.currentAction != TargetAction.ESCAPING)) {
                        // stable
                        int leftTicks = getCooldown() - pullOverTimer;
                        double canChaseDistance = Math.max(0.0D, 1.0D * (leftTicks));
                        if (mc.player.getEyePosition().distanceToSqr(targetPosition) > MathUtils.s2(canChaseDistance)) {
                            return STATE_FOLLOW;
                        }
                    } else if (base.currentAction == TargetAction.ESCAPING) {
                        // chasing
                        // do not too close,
                        double canChaseDistance = getMinRange();
                        if (mc.player.getEyePosition().distanceToSqr(targetPosition) > MathUtils.s2(canChaseDistance)) {
                            return STATE_FOLLOW;
                        }
                    } else {
                        // meeting
                        // escape their attack range, can hit
                        double canChaseDistance = getActiveRange();
                        if (mc.player.getEyePosition().distanceToSqr(targetPosition) > MathUtils.s2(canChaseDistance)) {
                            return STATE_FOLLOW;
                        }
                    }
                }
            }
            machine.markForEndState();
            net.minecraft.world.phys.Vec3 look = targetPosition.subtract(mc.player.getEyePosition());
            if (look.length() < 6) {
                look = look.normalize().scale(6);
            }
            if (canAdjustMovement()) {
                if (adjustMovementForSpear((Player) base.target, look, true)) {
                    return STATE_PULL_OVER;
                }
            }
            if (base.currentOnGround) {
                if (look.lengthSqr() < getMinRange()) {
                    movementDirection = look.reverse().add(0, 1, 0);
                } else {
                    movementDirection = look.reverse();
                }
            } else {
                movementDirection = look.reverse();
                movementDirection = movementDirection.with(Direction.Axis.Y, Math.abs(movementDirection.y));
            }
            //            Vec3d lookHorizontal = movementDirection.withAxis(Direction.Axis.Y, 0);
            //            Vec3d lastMoveHorizontal =
            //                PlayerStateManager.INSTANCE.lastKnownRealMovementSpeed.withAxis(Direction.Axis.Y, 0);
            //            double dotValue = lookHorizontal.dotProduct(lastMoveHorizontal);
            //            if (dotValue < 0) {
            //                movementDirection = movementDirection.negate();
            //                movementDirection = movementDirection.withAxis(Direction.Axis.Y,
            // Math.abs(movementDirection.y));
            //            }
            return STATE_PULL_OVER;
        }

        public void onSwitchToNearFollow(boolean isOn) {
            nearFollowTimer = 0;
        }

        public void onSwitchToPullOver(boolean isOn) {
            pullOverTimer = 0;
        }

        public double getActiveRange() {

            return 8.0D;
        }

        public double getMinRange() {
            return 1.0D;
        }

        public int getCooldown() {
            return 6;
        }

        @Override
        public synchronized void onUpdate() {
            super.onUpdate();
            if (mc.player.isFallFlying() || mc.player.getAbilities().flying) {
                stateMachine.step();
            } else {
                movementDirection = net.minecraft.world.phys.Vec3.ZERO;
                stateMachine.setState(STATE_NONE);
            }
        }

        @Override
        public void onEnable() {
            stateMachine.setState(STATE_NONE);
            pullOverTimer = 0;
            nearFollowTimer = 0;
        }

        @Override
        public void onDisable() {}

        @Override
        public synchronized void onHit(int spear) {
            if (spear == HIT_SPEAR) {

                nearFollowTimer = 0;
                stateMachine.setState(STATE_PULL_OVER);
                pullOverTimer = 0;
            }
        }
    }

    public static class WeaponArua extends AbstractBotBehaviour {

        @Override
        public void onUpdate() {}

        @Override
        public void onEnable() {}

        @Override
        public void onDisable() {}
    }

    public static class LandingControl extends AbstractBotBehaviour {

        @Override
        public void onUpdate() {}

        @Override
        public void onEnable() {}

        @Override
        public void onDisable() {}
    }

    public enum TargetAction {
        ESCAPING,
        TOWARDS,
        CIRCLING,
        SLOW_SPEED,
        AFK;
    }

    public enum Mode implements ConfigEnum {
        FOLLOW,
        MACE_ARUA,
        SPEAR_ARUA;

        @Override
        public String getConfigEnumType() {
            return "elytra_bot_mode";
        }
    }
}
