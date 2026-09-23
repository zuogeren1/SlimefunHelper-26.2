package me.matl114.hacks.modules.ac;

import java.util.Objects;
import me.matl114.events.Event;
import me.matl114.events.Listener;
import me.matl114.events.PacketManager;
import me.matl114.events.impl.EventContainer;
import me.matl114.events.impl.SlotClickAction;
import me.matl114.hacks.api.BaseModule;
import me.matl114.hacks.api.ModulePath;
import me.matl114.hacks.api.ModulePreset;
import me.matl114.hacks.modules.move.LegacySnapRotManager;
import me.matl114.hacks.modules.move.PlayerStateManager;
import me.matl114.hooks.ViaFabricPlusHooks;
import me.matl114.managers.Configs;
import me.matl114.managers.config.ConfigEnum;
import me.matl114.managers.config.EnumRef;
import me.matl114.managers.config.FlagRef;
import me.matl114.managers.config.KeyBindRef;
import me.matl114.managers.input.MultiKeyBind;
import me.matl114.utils.InventoryUtils;
import me.matl114.utils.NetworkUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class DisablerManager extends BaseModule {
    public static DisablerManager INSTANCE;

    public final ModulePath disablers = makePath(Configs.EXTRA_CONFIG, "disablers");

    public final FlagRef enable =
            builder(disablers.addEnable(), Boolean.class).defaultValue(true).build();

    public final KeyBindRef hotkey = moduleEntry(
                    disablers.addHotkey(), new MultiKeyBind(), disablers.addEnable(), moduleMeta(() -> this.currentAC))
            .build();

    public final EnumRef<SupportAC> currentAC = builder(disablers.add("current-ac"), SupportAC.class)
            .defaultValue(SupportAC.NONE)
            .build();

    public final FlagRef grimSelfCheck = builder(disablers.add("grim-self-check"), Boolean.class)
            .defaultValue(false)
            .build();

    public final FlagRef grimMultiplace = builder(disablers.add("grim-multi-place"), Boolean.class)
            .defaultValue(true)
            .build();

    //    public final FlagRef grimMultiBreak = builder(disablers.add("grim-multi-break"), Boolean.class)
    //            .defaultValue(false)
    //            .build();

    public final FlagRef autoFlushPlaceQueue = builder(disablers.add("auto-flush-multi-place-queue"), Boolean.class)
            .defaultValue(true)
            .build();

    public final FlagRef autoFlushPlaceBreakQueue = builder(
                    disablers.add("auto-flush-place-break-queue"), Boolean.class)
            .defaultValue(true)
            .build();

    public DisablerManager() {
        super("Disabler");
        INSTANCE = this;
        bindFlag(enable);
    }

    boolean grimSelfCheckDisabler;

    @Override
    public void registerAll() {
        super.registerAll();
        registerListener(Listener.getServerLeavePoint(), this::onDisconnect);
        registerListener(Listener.getPacketPoint().getChannel(ClientboundRespawnPacket.class), this::onRespawn);
        registerListener(Listener.getCustomListener().getChannel(ModulePreset.class), this::onPresetReload);
        registerListener(Listener.getPostTick(), this::onTick);
        registerListener(
                Listener.getPacketPoint().getChannel(ServerboundUseItemOnPacket.class),
                this::onPlace,
                Integer.MAX_VALUE - 1);
        registerListener(
                Listener.getPacketPoint().getChannel(ServerboundPlayerActionPacket.class),
                this::onBreakAction,
                Integer.MAX_VALUE - 1);
        registerListener(Listener.getPacketPoint().getChannel(ServerboundMovePlayerPacket.class), this::onFlying);
        registerListener(Listener.getPacketPoint().getChannel(ServerboundPongPacket.class), this::onPingPong);
        registerListener(Listener.getPreClickSlot(), this::onGhostHandSwapBack);
        registerListener(
                Listener.getPacketPoint().getChannel(ServerboundSetCarriedItemPacket.class),
                this::onGhostHandSwap,
                Integer.MAX_VALUE - 1);
    }
    // already fixed ,
    public void onRespawn(Event<ClientboundRespawnPacket> respawn) {
        if (!grimSelfCheckDisabler) {
            grimSelfCheckDisabler = true;
        }
    }

    Direction lastDirection;
    Vec3 lastCursor;
    BlockPos lastPos;
    boolean hasPlaceThisTick;

    public boolean isGrimSelfCheckDisabled() {
        return enable.get() && currentAC.get() == SupportAC.GRIM && grimSelfCheck.get() && grimSelfCheckDisabler;
    }

    public boolean isMultiPlaceCheckDisabled() {
        if (enable.get()) {
            return switch (currentAC.get()) {
                case GRIM -> isGrimMultiPlaceDisabled();
                case MATRIX -> false;
                default -> true;
            };
        }
        return false;
    }

    public boolean isMultiRotPlaceCheckDisabled(boolean methodCanMultiRot) {
        return isMultiPlaceCheckDisabled() && (methodCanMultiRot || isRotationPlaceCheckDisabled());
    }

    public boolean isRotationPlaceCheckDisabled() {
        if (enable.get()) {
            return switch (currentAC.get()) {
                case GRIM -> isGrimSelfCheckDisabled();
                case MATRIX -> false;
                default -> true;
            };
        }
        return false;
    }

    public boolean isGrimMultiPlaceDisabled() {
        return enable.get() && currentAC.get() == SupportAC.GRIM && (grimMultiplace.get());
    }

    public void onDisconnect(Event<Void> eventDisconnect) {
        grimSelfCheckDisabler = false;
    }

    boolean hasAnyPlaceActionGrimQueue = false;

    public boolean flushACPlaceQueue() {
        if (autoFlushPlaceQueue.get()) {
            return flushACPlaceQueue0();
        }
        return false;
    }

    private boolean flushACPlaceQueue0() {
        switch (currentAC.get()) {
            case GRIM -> {
                // flush ghost blocks
                // see GrimAC handleQueuedPlaces()
                if (hasAnyPlaceActionGrimQueue) {
                    if (ViaFabricPlusHooks.isSupportDupRot()) {
                        LegacySnapRotManager.INSTANCE.snapAt(mc.player.getXRot(), mc.player.getYRot(), true);
                    } else {
                        int selected = PlayerStateManager.INSTANCE.lastSelectedSlot;
                        int next = selected == 8 ? 7 : 8;
                        Listener.sendPacketNoEvents(new ServerboundSetCarriedItemPacket(next));
                        Listener.sendPacketNoEvents(new ServerboundSetCarriedItemPacket(selected));
                    }
                }
                hasAnyPlaceActionGrimQueue = false;
                return true;
            }
        }
        return false;
    }

    public boolean flushACPlaceBreakQueue() {
        if (autoFlushPlaceBreakQueue.get()) {
            return flushACPlaceQueue0();
        }
        return false;
    }
    // flush delayPlaceQueue when use Inv_Swap ghost hand
    public void onGhostHandSwapBack(Event<SlotClickAction> eventSlot) {
        if (eventSlot.isCancelled()) return;
        // do not flush on high version because they need to pass the post-flying rotation check
        if (enable.get()
                && hasAnyPlaceActionGrimQueue
                && autoFlushPlaceQueue.get()
                && ViaFabricPlusHooks.isSupportDupRot()) {
            flushACPlaceQueue0();
        }
    }

    // grim flush its delayPlaceQueue when swap
    public void onGhostHandSwap(Event<ServerboundSetCarriedItemPacket> event) {
        if (event.isCancelled()) return;
        hasAnyPlaceActionGrimQueue = false;
    }
    // MultiPlace bypass, fix place before delayQueuePlace flush -> grim flag AirLiquidPlace
    public void onPlace(Event<ServerboundUseItemOnPacket> blockPlace) {
        if (blockPlace.isCancelled()) return;
        BlockHitResult hitResult = blockPlace.context.getHitResult();
        Direction direction = hitResult.getDirection();
        Vec3 cursor = hitResult.getLocation();
        BlockPos blockPos = hitResult.getBlockPos();
        if (enable.get() && hasAnyPlaceActionGrimQueue && autoFlushPlaceQueue.get()) {
            flushACPlaceQueue0();
        }
        hasAnyPlaceActionGrimQueue = true;

        if (grimSelfCheckDisabler) {
            hasPlaceThisTick = false;
        }
        if (hasPlaceThisTick && enable.get() && currentAC.get() == SupportAC.GRIM && grimMultiplace.get()) {
            if (direction != lastDirection
                    || !Objects.equals(cursor, lastCursor)
                    || !Objects.equals(blockPos, lastPos)) {
                ServerboundUseItemOnPacket pkt = blockPlace.context;
                PacketManager.schedulePostCallback(pkt, () -> {
                    Listener.sendPacketNoEvents(new ServerboundUseItemOnPacket(
                            pkt.getHand(), pkt.getHitResult(), NetworkUtils.generateNextSequence()));
                });
            }
        }
        lastDirection = direction;
        lastCursor = cursor;
        lastPos = blockPos;
    }
    // fix break before delayQueuePlace flush -> grim flag AirLiquidBreak
    public void onBreakAction(Event<ServerboundPlayerActionPacket> eventBreak) {
        if (eventBreak.isCancelled()) return;
        switch (eventBreak.context.getAction()) {
            case START_DESTROY_BLOCK, STOP_DESTROY_BLOCK -> {}
            default -> {
                return;
            }
        }
        if (enable.get() && hasAnyPlaceActionGrimQueue && autoFlushPlaceBreakQueue.get()) {
            flushACPlaceQueue0();
        }
        hasAnyPlaceActionGrimQueue = false;
    }

    // see GrimAC handleQueuedPlaces
    public void onFlying(Event<ServerboundMovePlayerPacket> playerMoveC2SPacket) {
        hasAnyPlaceActionGrimQueue = false;
    }

    public void onPingPong(Event<ServerboundPongPacket> eventTransaction) {
        int id = eventTransaction.context.getId();
        if (id == (short) id) {
            // grimTransaction
            hasAnyPlaceActionGrimQueue = false;
        }
    }

    public void onTick(Event<Void> event) {
        hasPlaceThisTick = false;
    }

    public void onPresetReload(Event<EventContainer<ModulePreset>> event) {
        switch (event.context.getValue()) {
            case AC_GRIM, AC_GRIM_LEGACY -> {
                currentAC.set(SupportAC.GRIM);
            }
            case AC_MATRIX -> {
                currentAC.set(SupportAC.MATRIX);
            }
            default -> {
                currentAC.set(SupportAC.NONE);
            }
        }
    }

    public enum SupportAC implements ConfigEnum {
        NONE,
        GRIM,
        MATRIX;

        @Override
        public String getConfigEnumType() {
            return "support_disabler_ac";
        }

        @Override
        public Component getDisplay() {
            return Component.literal(this.name());
        }
    }
}
