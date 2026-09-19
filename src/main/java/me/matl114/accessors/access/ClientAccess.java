package me.matl114.accessors.access;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

public interface ClientAccess {
    static ClientAccess of(Minecraft client) {
        return (ClientAccess) client;
    }

    public ClientAccess clone();

    public void setItemUseCooldown(int cooldown);

    public void setAttackCooldown(int cooldown);

    public int getAttackCooldown();

    public int getItemUseCooldown();

    public void simulateRightClick();

    public void simulateLeftClick();

    public InteractionResult simulateUseItem(InteractionHand hand);
}
