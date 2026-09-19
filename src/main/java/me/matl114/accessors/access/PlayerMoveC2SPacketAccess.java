package me.matl114.accessors.access;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public interface PlayerMoveC2SPacketAccess {
    public void setOnGround(boolean onGround);

    public void setPitch(float pitch);

    public void setYaw(float yaw);

    public void setCause(Cause cause);

    public Cause getCause();

    public static ServerboundMovePlayerPacket setCause(ServerboundMovePlayerPacket packet, Cause cause) {
        PlayerMoveC2SPacketAccess.of(packet).setCause(cause);
        return packet;
    }

    public static ServerboundMovePlayerPacket setCauseFrom(ServerboundMovePlayerPacket packet, ServerboundMovePlayerPacket packet2) {
        return setCause(packet, of(packet2).getCause());
    }

    public static PlayerMoveC2SPacketAccess of(ServerboundMovePlayerPacket packet) {
        return (PlayerMoveC2SPacketAccess) packet;
    }
    //
    //    public void setManual(boolean manual);
    //
    //    public boolean isManual();
    public enum Cause {
        SET_BACK,
        PLAYER_MOVEMENT,
        HACKING_PACKETS,
        LEGACY_SNAP,
        TRIGGER_SIMULATION;
    }
}
