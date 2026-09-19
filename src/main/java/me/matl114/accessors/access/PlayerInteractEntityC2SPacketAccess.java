package me.matl114.accessors.access;

import net.minecraft.network.protocol.game.ServerboundInteractPacket;

public interface PlayerInteractEntityC2SPacketAccess {
    void setEntityId(int entityId);

    int getEntityId();

    void setPlayerSneaking(boolean playerSneaking);

    /**
     * 26.2 起攻击被拆分为独立的 {@code ServerboundAttackPacket}，{@code ServerboundInteractPacket}
     * 变成纯交互包（扁平 record：entityId/hand/location/usingSecondaryAction），不再承载攻击语义，
     * 原 Action 多态内部类与 ATTACK_ACTION 常量均已移除。
     *
     * <p>因此该判断在 26.2 下恒为 false。攻击相关逻辑（Criticals / ElytraBot 等）应改为监听
     * {@code ServerboundAttackPacket}，此项属于功能性重构，尚未完成。
     */
    @Deprecated // 26.2 下恒 false，误用会静默得到错误结果；攻击请用 ServerboundAttackPacket 通道
    default boolean isAttack() {
        return false;
    }

    static PlayerInteractEntityC2SPacketAccess of(ServerboundInteractPacket packet) {
        return (PlayerInteractEntityC2SPacketAccess) (Object) packet;
    }
}
