package me.matl114.accessors.hacks;

import net.minecraft.client.KeyMapping;

public interface KeyBindAccess {
    public void resetKeyState();

    static KeyBindAccess of(KeyMapping keyBinding) {
        return (KeyBindAccess) keyBinding;
    }
}
