package me.matl114.hacks.modules.ac;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;
import me.matl114.events.Event;
import me.matl114.events.Listener;
import me.matl114.hacks.api.BaseModule;
import me.matl114.managers.Tasks;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;

public class PostManager extends BaseModule {
    public static PostManager INSTANCE;

    public PostManager() {
        super("PostManager");
        INSTANCE = this;
    }

    private int peekPingRequest;
    private int lastPingTick;
    private final Deque<Consumer<ClientPacketListener>> postTickHandlers = new ArrayDeque<>(33);
    private final Deque<Consumer<ClientPacketListener>> queuePackets = new ArrayDeque<>(33);

    @Override
    public void registerAll() {
        super.registerAll();
        registerListener(Listener.getPacketPoint().getChannel(ClientboundPingPacket.class), this::peekPingPacketIn);
        registerListener(
                Listener.getPacketPostHandlePoint().getChannel(ClientboundPingPacket.class), this::postPongPacketOut);
        registerListener(
                Listener.getPacketPostSendPoint().getChannel(ServerboundClientTickEndPacket.class), this::postTickEnd);
        registerListener(Listener.getServerLeavePoint(), this::onDisconnectReset);
        registerListener(Listener.getPostGameTick(), this::onWatchPingLongTimeNoSent);
        registerListener(Listener.getPreTick(), this::onPreTick);
    }
    // failure:
    // rewrite pong packets to avoid post check
    // origin:
    // last tick
    // client tick end
    //  <- ping
    // pong ->
    // our post task
    // <- ping
    // pong ->
    // this tick
    //
    // we rewrite as
    // last tick
    // client tick end
    //  <- ping
    // pong ->
    // our post task
    // <- ping
    // pong -> (supressed)
    // this tick
    // this client tick end
    // supressed pong send
    //
    // shit, it doesn't work
    // private boolean hasHandledPongPacket = false;
    private Deque<ServerboundPongPacket> delayedPingPackets = new ArrayDeque<>(33);

    public void addPostTickAction(Consumer<ClientPacketListener> handler) {
        postTickHandlers.add(handler);
    }

    public void addNextPreTickAction(Consumer<ClientPacketListener> packet) {
        if (lastPingTick < Tasks.getTick() - 10) {
            if (mc.getConnection() != null) {
                packet.accept(mc.getConnection());
            }
        } else {
            queuePackets.addLast(packet);
        }
    }

    public void onPreTick(Event<Void> tick) {
        runAllQueuePackets(mc.getConnection());
    }

    public void peekPingPacketIn(Event<ClientboundPingPacket> packetPing) {
        peekPingRequest += 1;
        lastPingTick = Tasks.getTick();
        //        Debug.info("in", packetPing.getPacketId(), peekPingRequest);
    }

    public void postTickEnd(Event<ServerboundClientTickEndPacket> event) {
        runAllPostTickPackets(mc.getConnection());
        // flush pong packets
        //        for(var pongPacket : delayedPingPackets) {
        //            Listener.sendPacketNoEvents(pongPacket);
        //        }
        //        delayedPingPackets.clear();
        //        hasHandledPongPacket = false;
    }

    public void postPongPacketOut(Event<ClientboundPingPacket> event) {
        // we sent the Common Pong in Ping's handle

        // end transaction,
        // fresh queue
        peekPingRequest -= 1;
        //            Debug.info("out",pong.getPacketId(), peekPingRequest);

        // fix anything wrong wtf
        if (peekPingRequest < 0) peekPingRequest = 0;
        // anyway ,flush
        // runAllQueuePackets(mc.getConnection());
    }

    private void runAllPostTickPackets(ClientPacketListener handler) {
        runQueue(handler, postTickHandlers);
    }

    private void runQueue(ClientPacketListener handler, Deque<Consumer<ClientPacketListener>> postTickHandlers) {
        if (!postTickHandlers.isEmpty()) {

            if (handler != null) {
                var iter = postTickHandlers.iterator();
                while (iter.hasNext()) {
                    iter.next().accept(handler);
                    iter.remove();
                }
            } else {
                postTickHandlers.clear();
            }
        }
    }

    private void runAllQueuePackets(ClientPacketListener handler) {
        runQueue(handler, queuePackets);
    }

    private void onDisconnectReset(Event<Void> v) {
        peekPingRequest = 0;
        //        hasHandledPongPacket = false;
    }

    private void onWatchPingLongTimeNoSent(Event<LocalPlayer> v) {
        if (peekPingRequest > 0 && lastPingTick + 20 < Tasks.getTick()) {
            peekPingRequest = 0;
            lastPingTick = Tasks.getTick();
            runAllQueuePackets(mc.getConnection());
        }
    }

    public void onDisconnect(Event<Void> v) {}
}
