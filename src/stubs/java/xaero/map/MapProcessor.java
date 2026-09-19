package xaero.map;

import net.minecraft.client.multiplayer.ClientPacketListener;
import xaero.map.world.MapWorld;

public class MapProcessor {
    private MapWorld mapWorld;

    public MapWorld getMapWorld() {
        return this.mapWorld;
    }

    private String getMainId(int version, ClientPacketListener connection) {
        return "";
    }
}
