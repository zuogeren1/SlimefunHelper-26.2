package me.matl114.hacks.modules.combat;

import java.util.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import me.matl114.events.Event;
import me.matl114.events.Listener;
import me.matl114.events.impl.EventContainer;
import me.matl114.hacks.InteractionTasks;
import me.matl114.hacks.MovTasks;
import me.matl114.hacks.api.BaseModule;
import me.matl114.hacks.api.ModulePath;
import me.matl114.hacks.api.ModulePreset;
import me.matl114.hacks.modules.interact.InteractExtra;
import me.matl114.hacks.modules.inv.InvExtra;
import me.matl114.hacks.modules.mine.PacketMine;
import me.matl114.hacks.utils.enums.LegalInteractMode;
import me.matl114.hacks.utils.tasks.TimerExecutor;
import me.matl114.managers.Configs;
import me.matl114.managers.config.*;
import me.matl114.managers.input.MultiKeyBind;
import me.matl114.utils.*;
import me.matl114.utils.collections.FlagEntry;
import me.matl114.utils.collections.IndexEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import me.matl114.hacks.utils.EntityUtils;

public class AnchorAura extends BaseModule {
    private static final float ANCHOR_POWER = 5.0F;
    private static final int ANCHOR_SEARCH_RADIUS = 5;
    private static final double NEARBY_TARGET_SEARCH_RADIUS = 1.0D;
    private static final double PREDICT_POSITION_SWITCH_DISTANCE = 0.5D;

    public AnchorAura() {
        super("AnchorAura");
        bindFlag(enable);
    }

    public final ModulePath root = makePath(Configs.COMBAT_CONFIG, "combat-utils.anchor-aura");

    public final FlagRef enable = flagBuilder(root.addEnable()).build();

    public final KeyBindRef hotkey =
            moduleEntry(root.addHotkey(), new MultiKeyBind(), root.addEnable()).build();

    public final EnumRef<LegalInteractMode> mode = builder(root.add("mode"), LegalInteractMode.class)
            .defaultValue(LegalInteractMode.NONE)
            .build();

    public final FlagRef airplace =
            builder(root.add("air-place"), Boolean.class).defaultValue(false).build();

    public final IntRef range = intBuilder(root.add("range")).defaultValue(10).build();
    private List<Vec3i> interactRangeBlocks = new ArrayList<>();
    public final DoubleRef interactRange = doubleBuilder(root.add("interact-range"))
            .defaultValue(4.5)
            .updateListener(s -> interactRangeBlocks = MathUtils.create3DPointListAroundPlayer(s))
            .build();

    public final IntRef delay = intBuilder(root.add("delay"))
            .defaultValue(3)
            .validator(Configs.INT_POSITIVE)
            .build();

    public final IntRef mul = intBuilder(root.add("multiply"))
            .defaultValue(1)
            .validator(Configs.INT_POSITIVE)
            .build();

    public final DoubleRef selfFinalDamageThreshold = doubleBuilder(root.add("self-final-damage-threshold"))
            .defaultValue(4.0D)
            .validator(Configs.doubleRange(0.0D, 1000.0D))
            .build();

    public final DoubleRef targetDamageThreshold = doubleBuilder(root.add("target-damage-threshold"))
            .defaultValue(16.0D)
            .validator(Configs.doubleRange(0.0D, 1000.0D))
            .build();

    public final FlagRef considerAllTerrain =
            flagBuilder(root.add("consider-all-terrain")).build();

    public final FlagRef packetMineBridge =
            flagBuilder(root.add("packet-mine-bridge")).build();

    public final KeyBindRef packetMineBridgeHotkey = toggleHotkey(
                    root.add("packet-mine-bridge-hotkey"), new MultiKeyBind(), root.add("packet-mine-bridge"))
            .build();

    public final FlagRef place0TickSupply =
            flagBuilder(root.add("zero-tick-place-supply")).build();

    public final FlagRef use0TickSupply = flagBuilder(root.add("zero-tick-use")).build();

    public final FlagRef swingHand =
            builder(root.add("swing-hand"), Boolean.class).defaultValue(true).build();

    final TimerExecutor noSupplyExecutor = new TimerExecutor();

    @Override
    public void registerAll() {
        super.registerAll();
        registerListener(Listener.getWorldSwitchPoint(), this::onSwitchWorld);
        registerListener(Listener.getPreHandleInputEvents(), this::onPreInputEvents);
        registerListener(PacketMine.getPostPacketMine(), this::onPacketMine);
        registerListener(PacketMine.getPrePacketMine(), this::onPrePacketMine);
        registerListener(Listener.getCustomListener().getChannel(ModulePreset.class), this::onPreset);
    }

    public Map<BlockPos, AnchorCache> trackedAnchorPositions = new LinkedHashMap<>();
    public List<Player> targetEntity = List.of();

    public void onSwitchWorld(Event<Level> event) {
        trackedAnchorPositions.clear();
        targetEntity = List.of();
    }

    public void refreshTarget() {
        targetEntity = TargetSelector.INSTANCE.getAttackableEntities(range.get()).stream()
                .filter(Player.class::isInstance)
                .map(Player.class::cast)
                .filter(EntityUtils::isEntityValid)
                .filter(player -> player != mc.player)
                .toList();
    }

    public boolean checkSupplies() {
        boolean hasAnchor = supplyItem(Items.RESPAWN_ANCHOR) != null;
        boolean hasGlowStone = supplyItem(Items.GLOWSTONE) != null;
        if (!hasAnchor || !hasGlowStone) {
            noSupplyExecutor.run(100, () -> {
                if (!hasAnchor && !hasGlowStone) {
                    logI18N(
                            "message.module.anchor-arua.no-item.double",
                            Items.RESPAWN_ANCHOR.getName(new ItemStack(Items.RESPAWN_ANCHOR)),
                            Items.GLOWSTONE.getName(new ItemStack(Items.GLOWSTONE)));
                } else if (!hasAnchor) {
                    logI18N(
                            "message.module.anchor-arua.no-item",
                            Items.RESPAWN_ANCHOR.getName(new ItemStack(Items.RESPAWN_ANCHOR)));
                } else {
                    logI18N(
                            "message.module.anchor-arua.no-item",
                            Items.GLOWSTONE.getName(new ItemStack(Items.GLOWSTONE)));
                }
            });
            return false;
        }
        return true;
    }

    public Map<Player, Double> calculateAnchorDamage(BlockPos pos) {
        return calculateAnchorDamage(pos, null, true);
    }

    private Map<Player, Double> calculateAnchorDamage(
            BlockPos pos, Map<Player, AABB> predictedBoxes, boolean usePredict) {
        Map<Player, Double> damageMap = new LinkedHashMap<>();
        if (mc.level == null || mc.player == null) {
            return damageMap;
        }
        Vec3 explosionPos = Vec3.atCenterOf(pos);
        Map<BlockPos, BlockState> overrides = Map.of(pos, Blocks.AIR.defaultBlockState());
        var access = ExplosionUtils.fromWorldWithOverrides(mc.level, overrides);
        damageMap.put(mc.player, (double) ExplosionUtils.calculateExplosionRawDamage(
                ANCHOR_POWER, explosionPos, mc.player.getBoundingBox(), access, ExplosionUtils.ALL_TERRAIN));
        for (Player target : targetEntity) {
            if (!EntityUtils.isEntityValid(target) || target == mc.player) {
                continue;
            }
            AABB targetBox = getTargetDamageBox(target, predictedBoxes, usePredict);
            damageMap.put(target, (double) ExplosionUtils.calculateExplosionRawDamage(
                    ANCHOR_POWER,
                    explosionPos,
                    targetBox,
                    access,
                    considerAllTerrain.get() ? ExplosionUtils.ALL_TERRAIN : ExplosionUtils.EXPLOSION_RESISTENCE));
        }
        return damageMap;
    }

    public void tickAnchorPosition() {
        Map<Player, AABB> predictedBoxes = new HashMap<>();

        var iter = trackedAnchorPositions.entrySet().iterator();
        while (iter.hasNext()) {
            var entry = iter.next();
            BlockPos pos = entry.getKey();
            BlockState blockState = mc.level.getBlockState(pos);
            if (blockState.getBlock() instanceof RespawnAnchorBlock
                    && InteractExtra.INSTANCE.isWithinInteractRange(mc.player.position(), pos)) {
                Map<Player, Double> damageMap = calculateAnchorDamage(pos, predictedBoxes, true);
                if (isSuitableExplodePos(pos, damageMap)) {
                    entry.getValue().damageCache = damageMap;
                    // do not override this
                    if (entry.getValue().powerLevel == 0) {
                        entry.getValue().powerLevel = blockState.getValue(RespawnAnchorBlock.CHARGE);
                    }
                } else {
                    iter.remove();
                }
            } else {
                iter.remove();
            }
        }
        BlockPos playerPos = mc.player.blockPosition();
        for (var blocks : interactRangeBlocks) {
            BlockPos testPos = playerPos.offset(blocks);
            BlockState state = mc.level.getBlockState(testPos);

            if (state.getBlock() instanceof RespawnAnchorBlock
                    && !trackedAnchorPositions.containsKey(testPos)
                    && InteractExtra.INSTANCE.isWithinInteractRange(mc.player.position(), testPos)) {
                Map<Player, Double> damageMap = calculateAnchorDamage(testPos, predictedBoxes, true);
                if (isSuitableExplodePos(testPos, damageMap)) {
                    trackedAnchorPositions.put(
                            testPos, new AnchorCache(state.getValue(RespawnAnchorBlock.CHARGE), damageMap));
                }
            }
        }
    }

    private boolean isSuitableExplodePos(BlockPos pos, Map<Player, Double> damageMap) {
        if (damageMap == null || damageMap.isEmpty()) {
            return false;
        }
        double selfDamage = damageMap.getOrDefault(mc.player, Double.POSITIVE_INFINITY);
        float finalDamage = DamageUtils.getFinalDamage(
                mc.player,
                (float) selfDamage,
                DamageUtils.createDamageSource(DamageTypes.PLAYER_EXPLOSION, null, null));
        if (finalDamage > selfFinalDamageThreshold.get()) {
            return false;
        }
        return getBestEnemyDamage(damageMap) >= targetDamageThreshold.get();
    }

    //    private boolean isSuitablePacketMineBridgePos(BlockPos pos, Map<Player, Double> damageMap) {
    //        return damageMap != null
    //                && !damageMap.isEmpty()
    //                && getBestEnemyDamage(damageMap) > Double.NEGATIVE_INFINITY
    //                && hasNearbyMiningTarget(pos, damageMap);
    //    }

    public boolean isSuitablePosition(BlockPos pos) {
        BlockState state = mc.level.getBlockState(pos);
        return (state.isAir() || state.liquid() || state.canBeReplaced())
                && InteractUtils.canBlockPlace(mc.player, pos, Blocks.RESPAWN_ANCHOR.defaultBlockState());
    }

    int timer = 0;

    public void onPreInputEvents(Event<Void> event) {
        if (checkNull()) return;
        if (enable.get()) {
            if (!InteractUtils.canRespawnAnchorExplode(mc.level)) {
                logI18N(
                        "message.module.anchor-arua.invalid-dimension",
                        mc.level.dimension().identifier());
                enable.set(false);
                return;
            }
            refreshTarget();
            tickAnchorPosition();
            if (++timer >= delay.get() && !targetEntity.isEmpty()) {
                timer = 0;
                if (!checkSupplies()) return;
                if (!tickAnchorExplode()) {
                    tickAnchorPlace();
                }
            }
        } else {
            trackedAnchorPositions.clear();
            targetEntity = List.of();
        }
    }

    public boolean tickAnchorExplode() {
        List<Map.Entry<BlockPos, AnchorCache>> entries = new ArrayList<>(trackedAnchorPositions.entrySet());
        entries.sort(Map.Entry.comparingByValue(this::compareAnchorEntries));
        int multiply = mul.get();
        int success = 0;
        for (var i = 0; i < entries.size() && success < multiply; ++i) {
            BlockPos targetPos = entries.get(i).getKey();
            if (InteractExtra.INSTANCE.isWithinInteractRange(mc.player.position(), targetPos)) {
                success += litBlockPos(targetPos, entries.get(i).getValue());
            }
        }
        return success >= multiply;
    }

    public IndexEntry<ItemStack> supplyItem(Item item) {
        return InventoryUtils.findPlayerItem(s -> s.is(item), true, false);
    }

    public IndexEntry<ItemStack> supplyNoItem(Item item) {
        var re = InventoryUtils.findPlayerItem(s -> !s.is(item), true, true);
        return re == null ? InventoryUtils.getSelectedItem() : re;
    }

    public int litBlockPos(BlockPos targetPos, AnchorCache cache) {
        BlockState state = mc.level.getBlockState(targetPos);
        int interactCount = 0;
        if (state.getBlock() instanceof RespawnAnchorBlock) {
            int level = cache.powerLevel;
            BlockHitResult hitResult = RaycastUtils.createRealHitResult(targetPos);
            if (level == 0) {
                var glowstone = supplyItem(Items.GLOWSTONE);
                if (glowstone == null) {
                    return interactCount;
                }
                var callback = InvExtra.INSTANCE.swapInventoryIndexToHand(glowstone.index());
                if (callback == null) {
                    return interactCount;
                }
                InteractionTasks.handlePlaceMode(mode.get(), hitResult, InteractionHand.MAIN_HAND, swingHand.get());
                interactCount++;
                level = 1;
                cache.powerLevel = level;
                callback.run();
                if (!use0TickSupply.get()) {
                    return interactCount;
                }
            }
            if (level > 0) {
                var noGlowStone = supplyNoItem(Items.GLOWSTONE);
                var callback = InvExtra.INSTANCE.swapInventoryIndexToHand(noGlowStone.index());
                if (callback == null) {
                    return interactCount;
                }
                InteractionTasks.handlePlaceMode(mode.get(), hitResult, InteractionHand.MAIN_HAND, swingHand.get());
                interactCount++;
                level = 0;
                cache.powerLevel = level;
                callback.run();
                if (place0TickSupply.get()) {
                    var anchor = supplyItem(Items.RESPAWN_ANCHOR);
                    if (anchor == null) {
                        return interactCount;
                    }
                    var callback2 = InvExtra.INSTANCE.swapInventoryIndexToHand(anchor.index());
                    if (callback2 == null) {
                        return interactCount;
                    }
                    mc.level.setBlockAndUpdate(targetPos, Blocks.AIR.defaultBlockState());
                    InteractionTasks.handlePlaceMode(mode.get(), hitResult, InteractionHand.MAIN_HAND, swingHand.get());
                    interactCount++;
                    callback2.run();
                } else {
                    trackedAnchorPositions.remove(targetPos);
                }
            }
        }
        return interactCount;
    }

    public void tickAnchorPlace() {
        if (targetEntity.isEmpty()) {
            return;
        }
        Map<Player, AABB> predictedBoxes = new HashMap<>();

        BlockPos bestPos = null;

        FlagEntry<BlockHitResult> bestHitResult = null;
        Map<Player, Double> bestDamageMap = null;
        double bestEnemyDamage = Double.NEGATIVE_INFINITY;
        BlockPos currentPos = mc.player.blockPosition();
        for (Vec3i delta : interactRangeBlocks) {
            BlockPos pos = currentPos.offset(delta);
            if (!InteractExtra.INSTANCE.isWithinInteractRange(mc.player.position(), pos)) {
                continue;
            }
            if (!isSuitablePosition(pos)) {
                continue;
            }
            FlagEntry<BlockHitResult> hitResult = InteractionTasks.getPlaceSupportingResult(
                    pos, airplace.get(), !mode.get().isLegal());
            if (!InteractUtils.canInteractAndPlace(mc.player, hitResult)) {
                continue;
            }
            if (!InteractExtra.INSTANCE.isWithinInteractRange(
                    mc.player.position(), hitResult.val().getBlockPos())) {
                continue;
            }
            Map<Player, Double> damageMap = calculateAnchorDamage(pos, predictedBoxes, true);
            // ...
            if (!isSuitableExplodePos(pos, damageMap)) {
                continue;
            }

            double enemyDamage = getBestEnemyDamage(damageMap);
            if (enemyDamage > bestEnemyDamage) {
                bestPos = pos;
                bestHitResult = hitResult;
                bestDamageMap = damageMap;
                bestEnemyDamage = enemyDamage;
            }
        }
        if (bestPos != null) {
            if (InteractUtils.canInteractAndPlace(mc.player, bestHitResult)) {
                if (placeAnchor(bestPos, bestHitResult.val())) {
                    updateAnchor(bestPos, bestDamageMap);
                }
            }
        }
    }

    public boolean placeAnchor(BlockPos pos) {
        FlagEntry<BlockHitResult> hitResult = InteractionTasks.getPlaceSupportingResult(
                pos, airplace.get(), !mode.get().isLegal());
        if (InteractUtils.canInteractAndPlace(mc.player, hitResult)
                && InteractExtra.INSTANCE.isWithinInteractRange(
                        mc.player.position(), hitResult.val().getBlockPos())) {
            return placeAnchor(pos, hitResult.val());
        }
        return false;
    }

    public boolean placeAnchor(BlockPos pos, BlockHitResult hitResult) {
        var entry = supplyItem(Items.RESPAWN_ANCHOR);
        if (entry == null) return false;
        var runnable = InvExtra.INSTANCE.swapInventoryIndexToHand(entry.index());
        if (runnable == null) return false;
        InteractionTasks.handlePlaceMode(mode.get(), hitResult, InteractionHand.MAIN_HAND, swingHand.get());
        runnable.run();
        return true;
    }

    public void updateAnchor(BlockPos pos, Map<Player, Double> damageMap) {
        Map<Player, Double> safeDamageMap =
                damageMap == null ? calculateAnchorDamage(pos) : new LinkedHashMap<>(damageMap);
        trackedAnchorPositions.put(pos, new AnchorCache(0, safeDamageMap));
    }

    private AABB getTargetDamageBox(Player target, Map<Player, AABB> predictedBoxes, boolean usePredict) {
        if (!usePredict) {
            return target.getBoundingBox();
        }
        if (predictedBoxes == null) {
            return createPredictedTargetBox(target);
        }
        return predictedBoxes.computeIfAbsent(target, this::createPredictedTargetBox);
    }

    private AABB createPredictedTargetBox(Player target) {
        AABB currentBox = target.getBoundingBox();
        Vec3 currentPos = target.position();
        Vec3 predictedPos = PositionPredict.INSTANCE.attackPredictArgument.get().predict(target);
        if (predictedPos == null
                || predictedPos.distanceToSqr(currentPos) <= MathUtils.s2(PREDICT_POSITION_SWITCH_DISTANCE)) {
            return currentBox;
        }
        Vec3 realMovement = MovTasks.simulateMovement(target, currentPos, predictedPos.subtract(currentPos), false);
        return currentBox.move(realMovement);
    }

    private double getBestEnemyDamage(Map<Player, Double> damageMap) {
        double best = Double.NEGATIVE_INFINITY;
        for (Player target : targetEntity) {
            if (!EntityUtils.isEntityValid(target) || target == mc.player) {
                continue;
            }
            Double damage = damageMap.get(target);
            if (damage != null && damage > best) {
                best = damage;
            }
        }
        return best;
    }

    public void onPrePacketMine(Event<PacketMine.Pre> eventPreMine) {
        if (enable.get() && !eventPreMine.isCancelled()) {
            BlockPos pos = eventPreMine.getArgs(0);
            if (trackedAnchorPositions.containsKey(pos)
                    && mc.level.getBlockState(pos).getBlock() instanceof RespawnAnchorBlock) {
                eventPreMine.cancel();
            }
        }
    }

    public void onPacketMine(Event<PacketMine.Post> eventPostMine) {
        if (enable.get() && packetMineBridge.get() && !targetEntity.isEmpty()) {
            // bridge
            float floatValue = eventPostMine.getArgs(1);
            if (floatValue > 0.7f) {
                BlockPos pos = eventPostMine.getArgs(0);
                mc.level.setServerVerifiedBlockState(pos, Blocks.AIR.defaultBlockState(), 3);
                Map<Player, Double> damageMap = calculateAnchorDamage(pos, null, false);
                if (isSuitableExplodePos(pos, damageMap)) {
                    if (placeAnchor(pos)) {
                        updateAnchor(pos, damageMap);
                    }
                }
            }
        }
    }

    private int compareAnchorEntries(AnchorCache first, AnchorCache second) {
        double firstEnemy = getBestEnemyDamage(first.damageCache);
        double secondEnemy = getBestEnemyDamage(second.damageCache);
        int enemyCompare = Double.compare(secondEnemy, firstEnemy);
        if (enemyCompare != 0) {
            return enemyCompare;
        }
        double firstSelf = first.damageCache.getOrDefault(mc.player, 0.0D);
        double secondSelf = second.damageCache.getOrDefault(mc.player, 0.0D);
        int selfCompare = Double.compare(firstSelf, secondSelf);
        if (selfCompare != 0) {
            return selfCompare;
        }
        return Integer.compare(first.powerLevel, second.powerLevel);
    }

    public void onPreset(Event<EventContainer<ModulePreset>> event) {
        mode.set(LegalInteractMode.getFromPreset(event.context.getValue()));
        airplace.set(!event.context.getValue().hasAC());
    }

    @AllArgsConstructor
    @NoArgsConstructor
    public static class AnchorCache {
        int powerLevel;
        Map<Player, Double> damageCache;
    }
}
