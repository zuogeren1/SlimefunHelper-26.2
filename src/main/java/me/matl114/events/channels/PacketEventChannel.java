package me.matl114.events.channels;

import me.matl114.events.Event;
import me.matl114.events.Listener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;

public class PacketEventChannel extends EventChannelDispatcher<Packet<?>> {
    public PacketEventChannel() {
        super((v) -> Listener.getMappedPacketClass(v.getClass()));
    }

    public EventChannel<Packet<?>> getPacketSendChannel() {
        return getChannel(Boolean.FALSE);
    }

    public EventChannel<Packet<?>> getPacketReceiveChannel() {
        return getChannel(Boolean.TRUE);
    }

    @Override
    public boolean handleValue(Event<Packet<?>> express) {
        if (express.context.type() != null) {
            if (express.context.type().flow() == PacketFlow.CLIENTBOUND) {
                getPacketReceiveChannel().handleValue(express);
            } else {
                getPacketSendChannel().handleValue(express);
            }
        }
        return super.handleValue(express);
    }

    public <W extends Packet<?>> EventChannel<W> getChannel(Class<W> val) {
        return super.getChannel((Object) val);
    }
}
