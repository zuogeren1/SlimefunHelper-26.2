package me.matl114.hacks.modules.interact;

import java.util.*;
import me.matl114.accessors.moonrise.MoonriseBlockStateBaseAccess;
import me.matl114.events.Event;
import me.matl114.events.Listener;
import me.matl114.events.impl.EventContainer;
import me.matl114.hacks.InteractionTasks;
import me.matl114.hacks.api.BaseModule;
import me.matl114.hacks.api.ModulePath;
import me.matl114.hacks.api.ModulePreset;
import me.matl114.hacks.modules.inv.InvExtra;
import me.matl114.hacks.utils.enums.GhostHandMode;
import me.matl114.hacks.utils.enums.LegalInteractMode;
import me.matl114.managers.Configs;
import me.matl114.managers.config.EnumRef;
import me.matl114.managers.config.FlagRef;
import me.matl114.managers.config.IntRef;
import me.matl114.managers.config.KeyBindRef;
import me.matl114.managers.input.MultiKeyBind;
import me.matl114.utils.InteractUtils;
import me.matl114.utils.InventoryUtils;
import me.matl114.utils.collections.FlagEntry;
import me.matl114.utils.collections.IndexEntry;
import net.minecraft.core.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class Scaffold extends BaseModule {
    //    public static final String[] ENABLE = {"interact-scaffold", "scaffold"};
    //    public static final String[] ENABLE_HOTKEY = {"interact-scaffold", "scaffold-hotkey"};
    //    public static final String[] INTERACT_SCAFFOLD_LEGAL = {"interact-scaffold", "legal-mode"};
    //    public static final String[] INTERACT_SCAFFOLD_TARGET_MODE = {"interact-scaffold", "legal-targeting"};
    //    public static final String[] INTERACT_SCAFFOLD_COOLDOWN_OVERRIDE = {
    //        "interact-scaffold", "scaffold-cooldown-override"
    //    };

    public Scaffold() {
        super("Scaffold");
        bindFlag(enable);
    }

    List<Vec3i> searchOffsets;

    public void updateSearchRange(int range) {
        searchOffsets = new ArrayList<>();
        for (int x = -range; x <= range; x++) {
            for (int y = -3; y <= 0; y++) { // y <= 0
                for (int z = -range; z <= range; z++) {
                    if (x == 0 && y == 0 && z == 0) continue; // 过滤零点
                    searchOffsets.add(new Vec3i(x, y, z));
                }
            }
        }
        searchOffsets.sort(Comparator.comparingInt(v ->
                (int) Math.max(Math.max(Math.abs(v.getX()), Math.abs(v.getY())), Math.abs(v.getZ()))));
    }

    final ModulePath scaffold = makePath(Configs.INTERACT_CONFIG, "interact-scaffold");

    public final FlagRef enable = flagBuilder(scaffold.addEnable()).build();

    public final KeyBindRef keyBind = moduleEntry(scaffold.addHotkey(), new MultiKeyBind(), scaffold.addEnable())
            .build();

    //    public final FlagRef legal =
    //            flagBuilder(Configs.INTERACT_CONFIG, INTERACT_SCAFFOLD_LEGAL).build();

    public final EnumRef<LegalInteractMode> legalMode = builder(
                    scaffold.add("legal-targeting"), LegalInteractMode.class)
            .defaultValue(LegalInteractMode.USEITEM_PACKET)
            .build();

    public final FlagRef airplace = flagBuilder(scaffold.add("air-place")).build();

    public final IntRef delay =
            intBuilder(scaffold.add("delay")).defaultValue(1).build();

    public final FlagRef offhand = flagBuilder(scaffold.add("offhand-enable")).build();

    public final FlagRef swapHand = flagBuilder(scaffold.add("swap-hand")).build();

    public final IntRef expandYDepth = builder(scaffold.add("expand-interact-y-depth"), IntRef.TYPE)
            .defaultValue(0)
            .validator(Configs.intRange(0, 3))
            .build();

    public final IntRef expandInteractRange = builder(scaffold.add("expand-interact-range"), IntRef.TYPE)
            .defaultValue(1)
            .updateListener(this::updateSearchRange)
            .validator(Configs.intRange(0, 3))
            .build();

    public final EnumRef<GhostHandMode> ghostHand = builder(scaffold.add("ghost-hand-mode"), GhostHandMode.class)
            .defaultValue(GhostHandMode.INV_SWAP)
            .build();

    public final FlagRef swingHand = builder(scaffold.add("swing-hand"), Boolean.class)
            .defaultValue(true)
            .build();

    //    public final IntRef cooldownOverride = builder(
    //                    Configs.INTERACT_CONFIG, INTERACT_SCAFFOLD_COOLDOWN_OVERRIDE, IntRef.TYPE)
    //            .defaultValue(-1)
    //            .build();

    @Override
    public void registerAll() {
        super.registerAll();
        registerListener(Listener.getPreHandleInputEvents(), this::onRightClick);
        registerListener(Listener.getCustomListener().getChannel(ModulePreset.class), this::onPresetReload);
    }

    int delayTick = 0;

    private void placeBlockLegally(int hand, BlockHitResult result) {
        boolean offhandOk = offhand.get() || hand == 40;
        Runnable callback = InvExtra.INSTANCE.swapItemToHand(hand, offhandOk, ghostHand.get());
        if (callback == null) {
            return;
        }
        try {
            if (mc.hitResult instanceof BlockHitResult result1) {
                // same block same side
                // use vanilla crosshairtarget
                if (Objects.equals(result1.getBlockPos(), result.getBlockPos())
                        && Objects.equals(result1.getDirection(), result.getDirection())
                        && Objects.equals(result1.getType(), result.getType())) {
                    InteractionTasks.interactBlock(
                            offhandOk ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, result1, swingHand.get());
                    return;
                }
            }

            if (legalMode.get().isLegal()) {
                var mode = legalMode.get();
                // todo: delay movement fix
                InteractionTasks.handlePlaceMode(
                        mode,
                        result,
                        offhandOk ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND,
                        swingHand.get());
            } else {
                InteractionTasks.interactBlock(
                        offhandOk ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, result, swingHand.get());
            }
        } finally {
            callback.run();
        }
    }

    private Set<Item> availableItemBlocks;

    public int supplyBlock() {
        if (availableItemBlocks == null) {
            availableItemBlocks = new HashSet<>();
            for (var item : BuiltInRegistries.ITEM) {
                if (item instanceof BlockItem blockItem
                        && !blockItem.getBlock().defaultBlockState().isAir()
                        && blockItem
                                .getBlock()
                                .defaultBlockState()
                                .isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) {
                    availableItemBlocks.add(blockItem);
                }
            }
        }
        // do not consider offHand, because some game do not support
        IndexEntry<ItemStack> stackEntry = InventoryUtils.findPlayerItem(
                (item) -> availableItemBlocks.contains(item.getItem()),
                ghostHand.get().getSearchSize(offhand.get()),
                true,
                false,
                true,
                true);
        return stackEntry == null ? -1 : stackEntry.index();

        // search block in backpack
    }

    public void onRightClick(Event<Void> rightClickEvent) {

        // check scaffold when player right pressed the mouse
        // todo: check this
        if (mc.player != null && enable.get()) {
            // check hand item
            if (++delayTick < delay.get()) {
                return;
            }
            delayTick = 0;
            int idx = supplyBlock();

            if (idx < 0) {
                return;
            }
            // Debug.chat("tick", ClientAccess.of(mc).getCooldown());
            // check if we can have any scaffold
            // todo add lerp to config
            Vec3 playerPos = mc.player.position(); // mc.player.getLerpedPos(2.0F); // mc.player.getPos();
            // do not predict y level
            playerPos = new Vec3(playerPos.x, mc.player.getY(), playerPos.z);

            BlockPos testPos1 = BlockPos.containing(playerPos.subtract(0, 0.500001F, 0));
            BlockState blockState = mc.level.getBlockState(testPos1);
            // test if the supporting block can support player
            if (!blockState.isAir()
                    && !MoonriseBlockStateBaseAccess.of(blockState).isConstantCollisionShapeEmpty()) {

                return;
            }

            if (blockState.canBeReplaced()) {
                BlockHitResult hitResult = guessTheBestPlacePositionForTargetingBlock(
                        playerPos.add(0, mc.player.dimensions.eyeHeight(), 0), testPos1);
                if (hitResult != null) {
                    // Debug.chat("interact", hitResult.getBlockPos(), hitResult.getSide(), hitResult.getPos());
                    placeBlockLegally(idx, hitResult);
                    // todo should we autostack

                    return;
                }
            }
        }
    }

    //    //fixme delete log
    //    //fixme lefthand work
    //    //fixme speed effect
    //    private int lastScaffoldTick = 0;
    //    public void onStopUseItem(Event<Hand> eventUseItem){
    //        if(Tasks.getTick() < lastScaffoldTick + 3){
    //            eventUseItem.cancel();
    //        }
    //    }

    public BlockHitResult guessTheBestPlacePositionForTargetingBlock(Vec3 predictedEyePos, BlockPos pos) {
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult hitResult = ((BlockHitResult) mc.hitResult);
            BlockPos targetPos = hitResult.getBlockPos();
            Direction dir = hitResult.getDirection();
            BlockPos estimatePlacingPos = targetPos.relative(dir);
            // use vanilla
            if (InteractUtils.canCubePlace(mc.player, estimatePlacingPos) && Objects.equals(estimatePlacingPos, pos)) {
                return hitResult;
            }
        }
        FlagEntry<BlockHitResult> hitResult;
        BlockState state = mc.level.getBlockState(pos);
        if (state.canBeReplaced() && InteractUtils.canCubePlace(mc.player, pos)) {
            hitResult = InteractionTasks.getPlaceSupportingResult(
                    predictedEyePos, pos, airplace.get(), !legalMode.get().isLegal());
            if (hitResult != null && InteractUtils.canInteractAndPlace(mc.player, hitResult)) return hitResult.val();
        }

        for (var vec3d : searchOffsets) {
            if (vec3d.getY() >= -expandYDepth.get()) {
                BlockPos checkPos = pos.offset(vec3d);
                state = mc.level.getBlockState(checkPos);
                // filter can place blocks
                if (state.canBeReplaced() && InteractUtils.canCubePlace(mc.player, checkPos)) {
                    hitResult = InteractionTasks.getPlaceSupportingResult(
                            checkPos,
                            !legalMode.get().isLegal(),
                            !legalMode.get().isLegal());
                    if (hitResult != null) return hitResult.val();
                }
            }
        }

        return null;
    }

    public void onPresetReload(Event<EventContainer<ModulePreset>> event) {
        legalMode.set(LegalInteractMode.getFromPreset(event.context.getValue()));
        airplace.set(!event.context.getValue().hasAC());
    }
}
