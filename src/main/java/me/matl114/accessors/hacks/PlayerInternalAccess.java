package me.matl114.accessors.hacks;

import me.matl114.hacks.utils.entity.PredictorImpl;
import net.minecraft.world.entity.player.Player;

public interface PlayerInternalAccess extends EntityInternalAccess<Player> {
    public PredictorImpl getPredictorImpl();
}
