package me.matl114.hacks.modules.combat;

import me.matl114.events.Event;
import me.matl114.events.Listener;
import me.matl114.events.impl.EventContainer;
import me.matl114.hacks.api.BaseModule;
import me.matl114.hacks.api.ModulePath;
import me.matl114.hacks.api.ModulePreset;
import me.matl114.managers.Configs;
import me.matl114.managers.Tasks;
import me.matl114.managers.config.DoubleRef;
import me.matl114.managers.config.FlagRef;
import me.matl114.utils.Debug;
import me.matl114.versioned.api.VDataFlag;
import me.matl114.versioned.api.VItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundCooldownPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.HitResult;

public class CombatExtra extends BaseModule {
    public static CombatExtra INSTANCE;

    public CombatExtra() {
        super("CombatExtra");
        INSTANCE = this;
    }

    public final ModulePath combat = makePath(Configs.COMBAT_CONFIG, "attack");

    public final DoubleRef range =
            doubleBuilder(combat.add("att-range")).defaultValue(0.0D).build();

    public final DoubleRef boatAttackRange =
            doubleBuilder(combat.add("boat-reach-range")).defaultValue(0.0D).build();

    public final FlagRef shieldPredict =
            flagBuilder(combat.add("shielding-setback-log")).build();

    public final FlagRef useAttack = flagBuilder(combat.add("shielding-attack")).build();

    public final FlagRef rideAttack = flagBuilder(combat.add("riding-attack")).build();

    public final FlagRef noCooldown = flagBuilder(combat.add("cancel-interval")).build();

    private boolean ridingBypass(Entity entity) {
        return entity.isPassenger();
    }

    public double getAttackAtTargetRange(Entity entity) {
        double d = ridingBypass(mc.player) || ridingBypass(entity) ? boatAttackRange.get() : range.get();
        return mc.player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE) + d;
    }

    public double getAttackRange() {
        double d = ridingBypass(mc.player) ? boatAttackRange.get() : range.get();
        return mc.player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE) + d;
    }

    @Override
    public void registerAll() {
        super.registerAll();
        registerListener(
                Listener.getPacketPoint().getChannel(ClientboundSetEntityDataPacket.class), this::onShieldSetback);
        registerListener(
                Listener.getPacketPoint().getChannel(ClientboundCooldownPacket.class), this::asyncUpdateShieldCooldown);
        registerListener(Listener.getAttackAction(), this::onUseAttackNoSlow);
        registerListener(Listener.getCustomListener().getChannel(ModulePreset.class), this::onModulePreset);
    }

    public int shieldExceptionspam = 0;

    public void onUseAttackNoSlow(Event<HitResult> event) {
        //        if (shieldAttack.get() && mc.player.isUsingItem()) {
        //            MovTasks.getNoSlowDown().setPreAttackUseTick();
        //        }
    }

    public void onShieldSetback(Event<ClientboundSetEntityDataPacket> trackerUpdateS2CPacketEvent) {
        if (trackerUpdateS2CPacketEvent.isCancelled()) {
            return;
        }
        var trackerUpdateS2CPacket = trackerUpdateS2CPacketEvent.context();
        if (shieldPredict.get()
                && mc.player != null
                && trackerUpdateS2CPacket.id() == mc.player.getId()
                && mc.player.isUsingItem()
                && VItem.getInstance().isShield(mc.player.getUseItem())
                && !mc.player.getCooldowns().isOnCooldown(mc.player.getUseItem())) {
            // shield not in cooldown
            // block shield from
            for (var trackerUpdate : trackerUpdateS2CPacket.packedItems()) {
                // the ordinal  of LIVING FLAGS in LivingEntity, may vary with versionsl pls check
                if (trackerUpdate.id() == VDataFlag.ID_LIVING_FLAGS) {
                    byte byteValue = ((Number) trackerUpdate.value()).byteValue();
                    boolean bl = (byteValue & (1 << VDataFlag.USING_ITEM_FLAG_INDEX)) > 0;
                    InteractionHand hand = (byteValue & (1 << VDataFlag.OFFHAND_ACTIVE_FLAG_INDEX)) > 0
                            ? InteractionHand.OFF_HAND
                            : InteractionHand.MAIN_HAND;
                    // cooldown should be ok,
                    // the only position the server disable shield correctly should be cooldown
                    // so we kick it back
                    if (!bl && hand == mc.player.getUsedItemHand()) {
                        // using shield , but banned
                        if (shieldExceptionspam + 4 < Tasks.getTick()) {
                            shieldExceptionspam = Tasks.getTick();
                            Debug.chat(Component.literal("[AC] 阻挡异常盾牌禁用").withStyle(ChatFormatting.RED));
                        }
                        // trackerUpdateS2CPacketEvent.cancel();
                    }
                }
            }
        }
    }

    public void asyncUpdateShieldCooldown(Event<ClientboundCooldownPacket> packetEvent) {
        if (packetEvent.isCancelled()) {
            return;
        }
        ClientboundCooldownPacket packet = packetEvent.context();
        if (packet.duration() > 0) {
            try {
                synchronized (CombatExtra.class) {
                    // async update, synchronize to protect concurrent cooldown update,
                    mc.player.getCooldowns().addCooldown(packet.cooldownGroup(), packet.duration());
                    //                if(mc.player.isUsingItem() && mc.player.getActiveItem().getItem() == shield){
                    //
                    //                }
                    // consume packet
                    packetEvent.cancel();
                }

            } catch (Throwable e) {
                // any exception

            }
        }
    }

    public void onModulePreset(Event<EventContainer<ModulePreset>> event) {
        switch (event.context.getValue()) {
            case HACKING -> {
                range.set(3.0);
                boatAttackRange.set(3.0);
            }
            case AC_GRIM, AC_GRIM_LEGACY -> {
                range.set(0.0);
                boatAttackRange.set(3.0);
            }
            default -> {
                range.set(0.0);
                boatAttackRange.set(0.0);
            }
        }
    }
}
