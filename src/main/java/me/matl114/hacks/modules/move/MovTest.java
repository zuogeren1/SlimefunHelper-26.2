package me.matl114.hacks.modules.move;

import java.util.ArrayDeque;
import java.util.Deque;
import me.matl114.events.Event;
import me.matl114.events.Listener;
import me.matl114.hacks.MovTasks;
import me.matl114.hacks.api.BaseModule;
import me.matl114.hacks.utils.entity.LegalMovementManager;
import me.matl114.utils.Debug;
import me.matl114.versioned.api.VPacket;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;

public class MovTest extends BaseModule implements LegalMovementManager.MovementModifier {
    public static LegalMovementManager.DelegateMovementModifier instance;

    public MovTest() {
        super("MovTest");
        if (instance == null) {
            instance = new LegalMovementManager.DelegateMovementModifier(this::cast);
            // register at here for the first time
            MovTasks.PLAYER_PIPELINE_0.addMovementModifierFactory(() -> instance);
        }
        instance.setDelegate(this::cast);
    }

    public boolean enable() {
        return false;
    }

    @Override
    public void registerAll() {
        super.registerAll();
        registerListener(Listener.getPacketPoint().getChannel(ClientboundPingPacket.class), this::onTransaction);
        registerListener(
                Listener.getPacketPoint().getChannel(ClientboundSetEntityMotionPacket.class), this::onVelocityPacket);
    }

    @Override
    public int priority() {
        // the least important shit
        return 10000000;
    }

    public void onTransaction(Event<ClientboundPingPacket> event) {
        if (enable()) {
            //            delayedPackets.add(event.context());
            //            event.cancel();
        }
    }

    public void onVelocityPacket(Event<ClientboundSetEntityMotionPacket> event) {
        if (enable() && !checkNull() && event.context.id() == mc.player.getId()) {
            //            if(veryBigVelocity == null || (veryBigVelocity.getVelocity().lengthSquared() <
            // event.context.getVelocity().lengthSquared())){
            //                veryBigVelocity = event.context;
            //            }
            //            delayedPackets.add(event.context);
            //            event.cancel();
        }
    }

    Deque<ClientboundSetEntityMotionPacket> delayedPackets = new ArrayDeque<>();

    @Override
    public void applyPreTickModify(Event<LegalMovementManager> movementManagerEvent) {}

    @Override
    public void applyAfterInputTick(Event<LegalMovementManager> movementManagerEvent) {
        if (!enable()) {
            ClientboundSetEntityMotionPacket packet;
            //            while ((packet = delayedPackets.poll()) != null){
            //                Vec3d velocity = packet.getVelocity();
            //                if(velocity.lengthSquared() > mc.player.getVelocity().lengthSquared()){
            //                    Debug.chat("Use cached Velocity");
            //                    mc.player.setVelocity(velocity);
            //                }
            //            }
        }
    }

    Step step;
    int lastOnGround;
    Vec3 storePos;
    boolean runOnGroundThisTick;
    int sleep = 0;
    Packet<?> storedPacket = null;
    Deque<Vec3> posDeque = new ArrayDeque<>();

    @Override
    public void applyBeforeMovementPacketModify(Event<LegalMovementManager> movementManagerEvent) {
        if (enable()) {
            movementManagerEvent.cancel();
            movementManagerEvent.context.playerStatus.restorePos();
            storedPacket = VPacket.newFull(
                    mc.player.getX(),
                    mc.player.getY(),
                    mc.player.getZ(),
                    mc.player.getYRot(),
                    mc.player.getXRot(),
                    mc.player.onGround(),
                    mc.player.horizontalCollision);
        }
    }

    @Override
    public boolean postModify(Event<LegalMovementManager> movementManagerEvent, boolean enabledThisTick) {
        if (storedPacket != null) {
            Listener.sendPacketNoEvents(storedPacket);
            storedPacket = null;
        }
        if (enable()) {
            if (posDeque == null) {
                posDeque = new ArrayDeque<>();
            }
            posDeque.add(mc.player.position());
            Vec3 last19Vec3d = null;
            // >= 21,
            while (posDeque.size() > 20) {
                last19Vec3d = posDeque.removeFirst();
            }
            if (last19Vec3d != null) {
                Debug.chat(
                        "Speed last one sec :",
                        mc.player.position().subtract(last19Vec3d).length());
            }
        } else {
            if (posDeque != null) {
                posDeque.clear();
                posDeque = null;
            }
        }
        return true;
    }

    public static enum Step {
        ON_GROUND_1,
        ON_GROUND_2,
        SLEEP,
        WALK;
    }
}
