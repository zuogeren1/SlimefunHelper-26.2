package me.matl114.hooks.access;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public interface XaeroGuiMapAccess {

    public ResourceKey<Level> getRightClickDim();

    public int getRightClickX();

    public int getRightClickY();

    public int getRightClickZ();
}
