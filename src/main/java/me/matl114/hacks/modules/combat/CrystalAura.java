package me.matl114.hacks.modules.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import me.matl114.accessors.hacks.PlayerInteractionAccess;
import me.matl114.events.Event;
import me.matl114.events.impl.Render3D;
import me.matl114.events.Listener;
import me.matl114.events.RenderListener;
import me.matl114.events.impl.EventContainer;
import me.matl114.hacks.InteractionTasks;
import me.matl114.hacks.MovTasks;
import me.matl114.hacks.api.BaseModule;
import me.matl114.hacks.api.ModulePath;
import me.matl114.hacks.api.ModulePreset;
import me.matl114.hacks.modules.ac.DisablerManager;
import me.matl114.hacks.modules.interact.InteractExtra;
import me.matl114.hacks.modules.inv.InvExtra;
import me.matl114.hacks.modules.mine.PacketMine;
import me.matl114.hacks.utils.config.EntrySet;
import me.matl114.hacks.utils.render.RenderCollectors;
import me.matl114.hacks.utils.tasks.TimerExecutor;
import me.matl114.managers.Configs;
import me.matl114.managers.Tasks;
import me.matl114.managers.config.DoubleRef;
import me.matl114.managers.config.EnumRef;
import me.matl114.managers.config.FlagRef;
import me.matl114.managers.config.IntRef;
import me.matl114.managers.config.KeyBindRef;
import me.matl114.managers.config.NBTRef;
import me.matl114.managers.input.MultiKeyBind;
import me.matl114.utils.*;
import me.matl114.utils.collections.FlagEntry;
import me.matl114.utils.collections.IndexEntry;
import me.matl114.utils.render.RenderCollector;
import net.minecraft.core.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class CrystalAura extends BaseModule {
    private static final int CRYSTAL_SEARCH_RADIUS = 5;

    public CrystalAura() {
        super("CrystalAura");
        bindFlag(enable);
    }

    public final ModulePath root = makePath(Configs.COMBAT_CONFIG, "combat-utils.crystal-aura");

    public final FlagRef enable = flagBuilder(root.addEnable()).build();

    public final KeyBindRef hotkey =
            moduleEntry(root.addHotkey(), new MultiKeyBind(), root.addEnable()).build();

    public final EnumRef<Configs.LegalInteractMode> mode = builder(root.add("mode"), Configs.LegalInteractMode.class)
            .defaultValue(Configs.LegalInteractMode.NONE)
            .build();

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

    public final FlagRef placeAfterAttack = builder(root.add("place-after-attack"), Boolean.class)
            .defaultValue(true)
            .build();

    public final FlagRef autoBase =
            builder(root.add("auto-base"), Boolean.class).defaultValue(true).build();

    public final FlagRef offhand =
            builder(root.add("offhand"), Boolean.class).defaultValue(false).build();

    public final FlagRef airplaceBase =
            builder(root.add("air-place"), Boolean.class).defaultValue(false).build();

    public final FlagRef zeroTickBase = builder(root.add("zero-tick-base"), Boolean.class)
            .defaultValue(false)
            .build();

    public final DoubleRef baseReduce = builder(root.add("base-value-reduce"), DoubleRef.TYPE)
            .defaultValue(0.9D)
            .build();

    public final FlagRef considerAllTerrain =
            flagBuilder(root.add("consider-all-terrain")).build();

    public final FlagRef autoFill =
            builder(root.add("auto-fill"), Boolean.class).defaultValue(false).build();

    public final KeyBindRef hotkeyFill = toggleHotkey(
                    root.add("auto-fill-hotkey"), new MultiKeyBind(), root.add("auto-fill"))
            .build();

    public final NBTRef<EntrySet<Item>> fillWhiteList = builder(root.add("fill-white-list"), EntrySet.<Item>parameter())
            .defaultValue(new EntrySet<>(BuiltInRegistries.ITEM, List.of(Items.GLASS, Items.OAK_LEAVES)))
            .build();

    public final FlagRef fillIgnoreDamage = builder(root.add("fill-ignore-damage"), Boolean.class)
            .defaultValue(false)
            .build();

    public final DoubleRef selfFinalDamageThreshold = doubleBuilder(root.add("self-final-damage-threshold"))
            .defaultValue(4.0D)
            .validator(Configs.doubleRange(0.0D, 1000.0D))
            .build();

    public final DoubleRef targetDamageThreshold = doubleBuilder(root.add("target-damage-threshold"))
            .defaultValue(16.0D)
            .validator(Configs.doubleRange(0.0D, 1000.0D))
            .build();

    public final FlagRef usePredictor = builder(root.add("damage-use-predictor"), Boolean.class)
            .defaultValue(true)
            .build();

    public final FlagRef notifySupply =
            builder(root.add("notify-supply"), Boolean.class).defaultValue(true).build();

    public final FlagRef swingHand =
            builder(root.add("swing-hand"), Boolean.class).defaultValue(true).build();

    @Override
    public void registerAll() {
        super.registerAll();
        registerListener(Listener.getWorldSwitchPoint(), this::onWorldSwitch);
        registerListener(Listener.getPreHandleInputEvents(), this::onPreInputEvent);
        registerListener(Listener.getPostHandleInputEvents(), this::onPostInputEvent);
        registerListener(PacketMine.getPrePacketMine(), this::onPrePacketMine);
        registerListener(PacketMine.getPostPacketMine(), this::onPostPacketMine);
        registerListener(CombatManager.getRequestEnableEvent(), this::onRequestCombatService);
        registerListener(Listener.getCustomListener().getChannel(ModulePreset.class), this::onPreset);
        registerListener(RenderListener.getRender3DEvent(), this::onRender3D);
    }

    TimerExecutor supplyEndCrystalExecutor = new TimerExecutor();
    TimerExecutor supplyBaseExecutor = new TimerExecutor();
    TimerExecutor supplyFillBlockExecutor = new TimerExecutor();
    List<Player> currentTarget = List.of();
    List<BlockPos> pendingBases = new ArrayList<>();
    Map<EndCrystal, Integer> postRemovals = new HashMap<>();
    Map<BlockPos, Integer> trackedGlasses = new ConcurrentHashMap<>();
    int timer = 0;
    RenderCollector<AABB> debugRender = RenderCollectors.createBoxCollector(true, false, false);

    public void onWorldSwitch(Event<Level> event) {
        currentTarget = List.of();
        postRemovals.clear();
        trackedGlasses.clear();
        timer = 0;
    }

    public boolean attackCrystal(EndCrystal entity) {
        if (!Attack.INSTANCE.attackEntity(entity)) {
            postRemovals.put(entity, Tasks.getTick());
            return true;
        }
        return false;
    }

    public void onPreInputEvent(Event<Void> event) {
        if (checkNull()) {
            return;
        }
        debugRender.clear();
        // 3 ticks for network lag
        postRemovals
                .entrySet()
                .removeIf(entity ->
                        !EntityUtils.isEntityValid(entity.getKey()) || Tasks.getTick() > entity.getValue() + 3);
        if (enable.get()) {
            refreshTargets();
            tickTrackedGlasses();
            // constantly attack, only delay when place
            boolean attack = tickAttack();
            tickPendingBase();

            if ((++timer >= delay.get() || (placeAfterAttack.get() && attack)) && !currentTarget.isEmpty()) {
                timer = 0;
                if (checkSupplies()) {
                    tickPlace();
                }
            }
        } else {
            currentTarget = List.of();
            postRemovals.clear();
            trackedGlasses.clear();
            timer = 0;
        }
    }

    public void onPostInputEvent(Event<Void> event) {
        if (checkNull()) {
            return;
        }
        if (enable.get() && autoFill.get()) {
            if (currentTarget.isEmpty()) {
                return;
            }
            BlockPos pos = PlayerInteractionAccess.of(mc.gameMode).getCurrentMiningPos();
            if (!InteractExtra.INSTANCE.isWithinInteractRange(mc.player.position(), pos)) return;
            BlockState currentState = mc.level.getBlockState(pos);
            if (!currentState.isAir()) return;
            if (!canBridgeFillPos(pos)) {
                return;
            }
            // within range check, do not check crystal range, because human can move
            if (!InteractExtra.INSTANCE.isWithinInteractRange(mc.player.position(), pos.below())) {
                return;
            }
            if (!checkFillPlace(pos)) {
                return;
            }
            // help fix PacketMine fakeAirMine
            if (supplyFillBlock() == null) {
                supplyFillBlockExecutor.run(100, () -> {
                    if (notifySupply.get()) {
                        logI18N("message.module.crystal-aura.no-item.fill-block");
                    }
                });
                return;
            }
            if (!fillIgnoreDamage.get()) {
                Map<Player, Double> damageMap = calculateCrystalDamage(Vec3.atBottomCenterOf(pos), Map.of());
                if (!isSuitableAttackPos(damageMap)) {
                    return;
                }
            }
            FlagEntry<BlockHitResult> hitResult = InteractionTasks.getPlaceSupportingResult(
                    pos, airplaceBase.get(), !mode.get().isLegal());
            if (InteractUtils.canInteractAndPlace(mc.player, hitResult)) {
                placeFill(pos, hitResult.val());
            }
        }
    }

    public void onRender3D(Event<Render3D> event) {
        if (enable.get()) {
            RenderUtils.startDrawVirtual(event.context.stack());
            try {
                debugRender.render3D(event.context.stack());
            } finally {
                RenderUtils.stopDrawVirtual(event.context.stack());
            }
        }
    }

    private boolean checkSupplies() {
        var endCrystal = supplyCrystalItem() != null;
        if (!endCrystal) {
            supplyEndCrystalExecutor.run(100, () -> {
                if (notifySupply.get()) {
                    logI18N("message.module.crystal-aura.no-item.crystal");
                }
            });
            return false;
        }
        return true;
    }

    private List<EndCrystal> findAttackableCrystals() {
        double attackRange = CombatExtra.INSTANCE.getAttackRange();
        return CombatManager.INSTANCE.trackedEndCrystals.stream()
                .filter(EntityUtils::isEntityValid)
                .filter(crystal -> !postRemovals.containsKey(crystal))
                .filter(crystal -> TargetSelector.INSTANCE.isTargetInRange(crystal, attackRange, 0))
                .toList();
    }

    public boolean tickAttack() {
        if (currentTarget.isEmpty()) {
            return false;
        }
        List<Map.Entry<EndCrystal, Map<Player, Double>>> candidates = new ArrayList<>();
        for (EndCrystal crystal : findAttackableCrystals()) {
            if (postRemovals.containsKey(crystal)) {
                continue;
            }
            if (!EntityUtils.isEntityValid(crystal)) {
                continue;
            }
            Map<Player, Double> damageMap = calculateCrystalDamage(crystal.position());
            if (isSuitableExplodePos(damageMap)) {
                candidates.add(Map.entry(crystal, damageMap));
            }
        }
        candidates.sort(Map.Entry.comparingByValue(this::compareCrystalEntries));
        boolean attack = false;
        for (var crystalEntry : candidates) {
            EndCrystal crystal = crystalEntry.getKey();
            debugRender.submit(crystal.getBoundingBox(), ColorUtils.withAlphaInt(Color.MAGENTA, 255));
            if (!attackCrystal(crystal)) {
                break;
            }
            attack = true;
        }
        return attack;
    }

    public void refreshTargets() {
        if (TargetSelector.INSTANCE == null) {
            currentTarget = List.of();
            return;
        }
        currentTarget = TargetSelector.INSTANCE.getAttackableEntities(range.get()).stream()
                .filter(Player.class::isInstance)
                .map(Player.class::cast)
                .filter(EntityUtils::isEntityValid)
                .filter(player -> player != mc.player)
                .toList();
    }

    public void tickPendingBase() {
        for (var re : pendingBases) {
            if (InteractExtra.INSTANCE.isWithinInteractRange(mc.player.position(), re)
                    && isBasePlacedCrystalInAttackRange(re)
                    && canPlaceCrystal(re)) {
                tryPlaceCrystal(re);
            }
        }
        pendingBases.clear();
    }

    public void tickPlace() {
        if (currentTarget.isEmpty()) {
            return;
        }
        boolean legal = mode.get().isLegal();

        EndCrystalOption bestOption = null;
        double bestEnemyDamage = Double.NEGATIVE_INFINITY;
        double bestSelfDamage = Double.POSITIVE_INFINITY;
        boolean canAutoBase = autoBase.get();
        if (canAutoBase) {
            // check
            if (supplyItem(Items.OBSIDIAN) == null) {
                supplyBaseExecutor.run(100, () -> {
                    if (notifySupply.get()) {
                        logI18N("message.module.crystal-aura.no-item.base");
                    }
                });
                canAutoBase = false;
            }
        }
        BlockPos currentPlayerPos = mc.player.blockPosition();
        for (Vec3i delta : interactRangeBlocks) {
            BlockPos pos = currentPlayerPos.offset(delta);
            if (!InteractExtra.INSTANCE.isWithinInteractRange(mc.player.position(), pos)) {
                continue;
            }
            if (!isBasePlacedCrystalInAttackRange(pos)) {
                continue;
            }
            if (hasCrystalBlockingEntity(pos)) {
                continue;
            }
            boolean canPlaceCrystal = canPlaceCrystal(pos);
            EndCrystalOption option;
            if (canPlaceCrystal) {
                option = new DirectPlace(pos);
            } else if (canAutoBase && isSuitableBasePos(pos)) {
                FlagEntry<BlockHitResult> hitResultEntry =
                        InteractionTasks.getPlaceSupportingResult(pos, airplaceBase.get(), !legal);
                if (InteractUtils.canInteractAndPlace(mc.player, hitResultEntry)
                        && InteractExtra.INSTANCE.isWithinInteractRange(
                                mc.player.position(), hitResultEntry.val().getBlockPos())) {
                    option = new AutoBase(pos, hitResultEntry.val());
                } else {
                    continue;
                }
            } else {
                continue;
            }

            Map<Player, Double> damageMap = canPlaceCrystal
                    ? calculateCrystalDamage(getCrystalExplosionPos(pos))
                    : calculateCrystalDamage(
                            getCrystalExplosionPos(pos), Map.of(pos, Blocks.OBSIDIAN.defaultBlockState()));
            if (!isSuitableExplodePos(damageMap)) {
                continue;
            }

            double enemyDamage = getBestEnemyDamage(damageMap);
            double selfDamage = damageMap.getOrDefault(mc.player, Double.POSITIVE_INFINITY);
            double damageValue = enemyDamage * (option.needBase() ? baseReduce.get() : 1.0D);
            if (damageValue > bestEnemyDamage || (damageValue == bestEnemyDamage && selfDamage < bestSelfDamage)) {
                bestOption = option;
                bestEnemyDamage = damageValue;
                bestSelfDamage = selfDamage;
            }
        }

        if (bestOption == null) {
            return;
        }
        debugRender.submit(new AABB(bestOption.basePos()), ColorUtils.withAlphaInt(Color.MAGENTA, 255));
        if (bestOption.needBase()) {
            placeBase(bestOption.basePos(), ((AutoBase) bestOption).basePlaceResult());
        } else {
            tryPlaceCrystal(bestOption.basePos());
        }
    }

    private Map<Player, Double> calculateCrystalDamage(Vec3 explosionPos) {
        return calculateCrystalDamage(explosionPos, Map.of());
    }

    private Map<Player, Double> calculateCrystalDamage(Vec3 explosionPos, Map<BlockPos, BlockState> overrides) {
        Map<Player, Double> damageMap = new LinkedHashMap<>();
        if (mc.level == null || mc.player == null) {
            return damageMap;
        }
        var access = overrides.isEmpty()
                ? ExplosionUtils.fromWorld(mc.level)
                : ExplosionUtils.fromWorldWithOverrides(mc.level, overrides);
        damageMap.put(mc.player, (double) ExplosionUtils.calculateExplosionRawDamage(
                ExplosionUtils.END_CRYSTAL_POWER,
                explosionPos,
                mc.player.getBoundingBox(),
                access,
                ExplosionUtils.ALL_TERRAIN));
        for (Player target : currentTarget) {
            if (!EntityUtils.isEntityValid(target) || target == mc.player) {
                continue;
            }
            Vec3 targetPos = target.position();
            Vec3 realMovement;
            if (usePredictor.get()) {
                Vec3 predictionPos =
                        PositionPredict.INSTANCE.attackPredictArgument.get().predict(target);

                if (predictionPos.distanceToSqr(targetPos) > MathUtils.s2(0.5)) {
                    Vec3 movement = predictionPos.subtract(targetPos);
                    realMovement = MovTasks.simulateMovement(target, targetPos, movement, false);
                } else {
                    realMovement = Vec3.ZERO;
                }
            } else {
                realMovement = Vec3.ZERO;
            }

            damageMap.put(target, (double) ExplosionUtils.calculateExplosionRawDamage(
                    ExplosionUtils.END_CRYSTAL_POWER,
                    explosionPos,
                    target.getBoundingBox().move(realMovement),
                    access,
                    considerAllTerrain.get() ? ExplosionUtils.ALL_TERRAIN : ExplosionUtils.EXPLOSION_RESISTENCE));
        }
        return damageMap;
    }

    private Vec3 getCrystalExplosionPos(BlockPos basePos) {
        return Vec3.atBottomCenterOf(basePos.above());
    }

    private boolean isSuitableAttackPos(Map<Player, Double> damageMap) {
        if (damageMap == null || damageMap.isEmpty()) {
            return false;
        }
        return getBestEnemyDamage(damageMap) >= targetDamageThreshold.get();
    }

    private boolean isSuitableExplodePos(Map<Player, Double> damageMap) {
        if (damageMap == null || damageMap.isEmpty()) {
            return false;
        }
        double selfDamage = damageMap.getOrDefault(mc.player, Double.POSITIVE_INFINITY);
        float finalDamage = DamageUtils.getFinalDamage(
                mc.player,
                (float) selfDamage,
                DamageUtils.createDamageSource(DamageTypes.PLAYER_EXPLOSION, null, mc.player));
        if (finalDamage > selfFinalDamageThreshold.get()) {
            return false;
        }
        return getBestEnemyDamage(damageMap) >= targetDamageThreshold.get();
    }

    private double getBestEnemyDamage(Map<Player, Double> damageMap) {
        double best = Double.NEGATIVE_INFINITY;
        for (Player target : currentTarget) {
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

    private boolean isBasePlacedCrystalInAttackRange(BlockPos basePos) {
        Vec3 place = Vec3.atBottomCenterOf(basePos.above());
        AABB estimatedEndCrystalBox =
                new AABB(place.x - 1, place.y, place.z - 1, place.x + 1, place.y + 2, place.z + 1);
        return estimatedEndCrystalBox.distanceToSqr(mc.player.getEyePosition())
                <= MathUtils.s2(CombatExtra.INSTANCE.getAttackRange());
    }

    private boolean isCrystalBase(BlockState state) {
        return state.is(Blocks.OBSIDIAN) || state.is(Blocks.BEDROCK);
    }

    private boolean canBlockCrystalPlace(Entity entity) {
        return EntityUtils.isEntityValid(entity)
                && (!(entity instanceof EndCrystal crystal) || !postRemovals.containsKey(crystal));
    }

    private boolean hasCrystalBlockingEntity(BlockPos crystalPos) {
        return !mc.level
                .getEntities((Entity) null, new AABB(crystalPos), this::canBlockCrystalPlace)
                .isEmpty();
    }

    private boolean canPlaceCrystalAtPos(BlockPos crystalPos, Map<BlockPos, BlockState> overrides) {
        ExplosionUtils.BlockStateAccess stateAccess = ExplosionUtils.fromWorldWithOverrides(mc.level, overrides);
        BlockState crystalState = stateAccess.getBlockState(crystalPos);
        if (crystalState == null || !crystalState.isAir()) {
            return false;
        }
        BlockState baseState = stateAccess.getBlockState(crystalPos.below());
        if (baseState == null || !isCrystalBase(baseState)) {
            return false;
        }
        return !hasCrystalBlockingEntity(crystalPos);
    }

    private boolean canPlaceCrystal(BlockPos basePos) {
        return canPlaceCrystalAtPos(basePos.above(), Map.of());
    }

    private boolean isSuitableBasePos(BlockPos pos) {
        BlockState state = mc.level.getBlockState(pos);
        return (state.isAir() || state.liquid() || state.canBeReplaced())
                && InteractUtils.canBlockPlace(mc.player, pos, Blocks.OBSIDIAN.defaultBlockState())
                && canPlaceCrystalAtPos(pos.above(), Map.of(pos, Blocks.OBSIDIAN.defaultBlockState()));
    }

    private IndexEntry<ItemStack> supplyItem(Item item) {
        return InventoryUtils.findPlayerItem(stack -> stack.is(item), true, false);
    }

    private IndexEntry<ItemStack> supplyFillBlock() {
        return InventoryUtils.findPlayerItem(
                stack -> {
                    if (!(stack.getItem() instanceof BlockItem blockItem)
                            || !fillWhiteList.get().test(stack.getItem())) {
                        return false;
                    }
                    return true;
                },
                true,
                false);
    }

    private IndexEntry<ItemStack> supplyCrystalItem() {
        return InventoryUtils.findPlayerItem(stack -> stack.is(Items.END_CRYSTAL), true, false);
    }

    private boolean shouldKeepTrackedGlass(BlockPos glassPos) {
        if (fillIgnoreDamage.get()) {
            return true;
        }
        Map<BlockPos, BlockState> overrides =
                Map.of(glassPos, Blocks.AIR.defaultBlockState(), glassPos.below(), Blocks.OBSIDIAN.defaultBlockState());

        Map<Player, Double> damageMap = calculateCrystalDamage(Vec3.atBottomCenterOf(glassPos), overrides);
        return isSuitableAttackPos(damageMap);
    }

    private boolean isTrackedGlassState(BlockState state) {
        if (state == null || state.isAir() || state.liquid()) {
            return false;
        }
        Item item = state.getBlock().asItem();
        return item != Items.AIR && fillWhiteList.get().test(item);
    }

    private void tickTrackedGlasses() {
        if (trackedGlasses.isEmpty()) {
            return;
        }
        if (currentTarget.isEmpty()) {
            trackedGlasses.clear();
            return;
        }
        trackedGlasses.keySet().removeIf(s -> s.distToCenterSqr(mc.player.position()) > MathUtils.s2(8));
        trackedGlasses.entrySet().removeIf(entry -> {
            BlockPos pos = entry.getKey();
            BlockState state = mc.level.getBlockState(pos);
            return !isTrackedGlassState(state) || !shouldKeepTrackedGlass(pos);
        });
    }

    private boolean tryPlaceCrystal(BlockPos basePos) {
        if (!canPlaceCrystal(basePos)) {
            return false;
        }
        IndexEntry<ItemStack> entry = supplyCrystalItem();
        if (entry == null) {
            return false;
        }
        Runnable callback = offhand.get()
                ? InvExtra.INSTANCE.swapInventoryIndexToOffhand(entry.index())
                : InvExtra.INSTANCE.swapInventoryIndexToHand(entry.index());
        if (callback == null) {
            return false;
        }
        InteractionTasks.handlePlaceMode(
                mode.get(),
                RaycastUtils.createHitResult(basePos, mc.player.getEyePosition()),
                offhand.get() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND,
                swingHand.get());
        callback.run();
        return true;
    }

    private boolean placeBase(BlockPos pos, BlockHitResult hitResult) {
        IndexEntry<ItemStack> entry = supplyItem(Items.OBSIDIAN);
        if (entry == null) {
            return false;
        }
        Runnable callback = InvExtra.INSTANCE.swapInventoryIndexToHand(entry.index());
        if (callback == null) {
            return false;
        }
        InteractionTasks.handlePlaceMode(mode.get(), hitResult, InteractionHand.MAIN_HAND, swingHand.get());
        callback.run();
        if (zeroTickBase.get()
                && DisablerManager.INSTANCE.isMultiRotPlaceCheckDisabled(
                        mode.get().canMultiRotPlace())) {
            tryPlaceCrystal(pos);
        } else {
            pendingBases.add(pos);
        }
        return true;
    }

    private boolean placeFill(BlockPos pos, BlockHitResult hitResult) {
        IndexEntry<ItemStack> entry = supplyFillBlock();
        if (entry == null) {
            return false;
        }
        Runnable callback = offhand.get()
                ? InvExtra.INSTANCE.swapInventoryIndexToOffhand(entry.index())
                : InvExtra.INSTANCE.swapInventoryIndexToHand(entry.index());
        if (callback == null) {
            return false;
        }
        mc.level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        InteractionTasks.handlePlaceMode(
                mode.get(),
                hitResult,
                offhand.get() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND,
                swingHand.get());
        callback.run();
        trackedGlasses.put(pos, Tasks.getTick());
        return true;
    }

    private boolean canBridgeFillPos(BlockPos crystalPos) {
        BlockState baseState = mc.level.getBlockState(crystalPos.below());
        boolean availableBase = baseState.is(Blocks.OBSIDIAN) || baseState.is(Blocks.BEDROCK);
        if (!availableBase && autoBase.get()) {
            if (baseState.isAir()) {
                var re = InteractionTasks.getPlaceSupportingResult(
                        crystalPos.below(), airplaceBase.get(), !mode.get().isLegal());
                if (InteractUtils.canInteractAndPlace(mc.player, re)
                        && InteractUtils.canBlockPlace(
                                mc.player, crystalPos.below(), Blocks.OBSIDIAN.defaultBlockState())) {
                    availableBase = true;
                }
            }
        }
        return availableBase && !mc.level.getBlockState(crystalPos).liquid();
    }

    public void onPrePacketMine(Event<PacketMine.Pre> eventPreMine) {
        if (!enable.get() || eventPreMine.isCancelled()) {
            return;
        }
        BlockPos pos = eventPreMine.getArgs(0);
        Integer placeTick = trackedGlasses.get(pos);
        if (placeTick == null) {
            return;
        }
        BlockPos basePos = pos.below();
        boolean olderThanTwoTick = Tasks.getTick() - placeTick > 2;
        Map<BlockPos, BlockState> overrides = Map.of(pos, Blocks.AIR.defaultBlockState());
        if (!olderThanTwoTick
                || !InteractExtra.INSTANCE.isWithinInteractRange(mc.player.position(), basePos)
                || !isBasePlacedCrystalInAttackRange(basePos)
                || !canPlaceCrystalAtPos(pos, overrides)
                || !isSuitableExplodePos(calculateCrystalDamage(Vec3.atBottomCenterOf(pos), overrides))) {
            eventPreMine.cancel();
        }
    }

    private boolean checkFillPlace(BlockPos pos) {
        for (var dir : MathUtils.HORIZONTALS) {
            BlockPos nearBox = pos.relative(dir);
            AABB nearBb = new AABB(nearBox);
            if (mc.level.getBlockState(nearBox).getBlock().getExplosionResistance() < 600
                    && currentTarget.stream().anyMatch(s -> s.getBoundingBox().intersects(nearBb))) {
                return true;
            }
        }
        return false;
    }

    public void onPostPacketMine(Event<PacketMine.Post> eventPostMine) {
        if (!enable.get()) {
            return;
        }
        BlockPos pos = eventPostMine.getArgs(0);
        float progress = eventPostMine.getArgs(1);
        if (progress <= 0.7F) {
            return;
        }
        if (currentTarget.isEmpty()) {
            return;
        }
        if (!autoFill.get()) {
            return;
        }
        if (!canBridgeFillPos(pos)) {
            return;
        }
        // within range check, do not check crystal range, because human can move
        if (!InteractExtra.INSTANCE.isWithinInteractRange(mc.player.position(), pos.below())) {
            return;
        }
        pos = pos.immutable();
        Map<BlockPos, BlockState> overrides = new HashMap<>();
        overrides.put(pos, Blocks.AIR.defaultBlockState());
        // help fix PacketMine fakeAirMine
        mc.level.setServerVerifiedBlockState(pos, Blocks.AIR.defaultBlockState(), 3);
        if (trackedGlasses.containsKey(pos)) {
            if (canPlaceCrystalAtPos(pos, overrides)) {
                trackedGlasses.remove(pos);
                BlockPos checkBaseAndEntity = pos.below();
                tryPlaceCrystal(checkBaseAndEntity);
                return;
            }
        }
        if (!checkFillPlace(pos)) {
            return;
        }
        if (supplyFillBlock() == null) {
            supplyFillBlockExecutor.run(100, () -> {
                if (notifySupply.get()) {
                    logI18N("message.module.crystal-aura.no-item.fill-block");
                }
            });
            return;
        }
        if (!fillIgnoreDamage.get()) {
            Map<Player, Double> damageMap = calculateCrystalDamage(Vec3.atBottomCenterOf(pos), overrides);
            if (!isSuitableAttackPos(damageMap)) {
                return;
            }
        }
        FlagEntry<BlockHitResult> hitResult = InteractionTasks.getPlaceSupportingResult(
                pos, airplaceBase.get(), !mode.get().isLegal());
        if (InteractUtils.canInteractAndPlace(mc.player, hitResult)) {
            placeFill(pos, hitResult.val());
        }
    }

    private int compareCrystalEntries(Map<Player, Double> first, Map<Player, Double> second) {
        double firstEnemy = getBestEnemyDamage(first);
        double secondEnemy = getBestEnemyDamage(second);
        int enemyCompare = Double.compare(secondEnemy, firstEnemy);
        if (enemyCompare != 0) {
            return enemyCompare;
        }
        double firstSelf = first.getOrDefault(mc.player, Double.POSITIVE_INFINITY);
        double secondSelf = second.getOrDefault(mc.player, Double.POSITIVE_INFINITY);
        return Double.compare(firstSelf, secondSelf);
    }

    private void onRequestCombatService(Event<CombatManager.Service> event) {
        if (enable.get()) {
            event.context().enableExplosiveSearch(true);
        }
    }

    public void onPreset(Event<EventContainer<ModulePreset>> event) {
        mode.set(Configs.LegalInteractMode.getFromPreset(event.context.getValue()));
        airplaceBase.set(!event.context.getValue().hasAC());
    }

    public static sealed interface EndCrystalOption permits AutoBase, DirectPlace {
        public BlockPos basePos();

        public boolean needBase();
    }

    public static record AutoBase(BlockPos basePos, BlockHitResult basePlaceResult) implements EndCrystalOption {

        @Override
        public boolean needBase() {
            return true;
        }
    }

    public static record DirectPlace(BlockPos basePos) implements EndCrystalOption {

        @Override
        public boolean needBase() {
            return false;
        }
    }
}
