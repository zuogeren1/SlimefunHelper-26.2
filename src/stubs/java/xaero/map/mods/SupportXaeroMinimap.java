package xaero.map.mods;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;

public class SupportXaeroMinimap {
    public int compatibilityVersion;
    private boolean deathpoints = true;
    private boolean refreshWaypoints = true;
    private MinimapWorld waypointWorld;
    private MinimapWorld mapWaypointWorld;
    private ResourceKey<Level> mapDimId;
    private double dimDiv;
    private WaypointSet waypointSet;
    private boolean allSets;

    public void requestWaypointsRefresh() {
        this.refreshWaypoints = true;
    }

    public KeyMapping getWaypointKeyBinding() {
        return null;
    }

    public KeyMapping getTempWaypointKeyBinding() {
        return null;
    }

    public KeyMapping getTempWaypointsMenuKeyBinding() {
        return null;
    }
}
