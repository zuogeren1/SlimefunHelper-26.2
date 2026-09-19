package me.matl114.versioned.impl;

import me.matl114.versioned.api.VPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.world.entity.Entity;

public class Packet_v1_21_11 implements VPacket {

    @Override
    public ServerboundMovePlayerPacket createOnGroundOnly(boolean isOnGround, boolean collision) {
        // 1.21.1 版本不支持 collision 参数，忽略它
        return new ServerboundMovePlayerPacket.StatusOnly(isOnGround, collision);
    }

    @Override
    public ServerboundMovePlayerPacket createPositionAndOnGround(
            double x, double y, double z, boolean isOnGround, boolean collision) {
        // 1.21.1 版本不支持 collision 参数，忽略它
        return new ServerboundMovePlayerPacket.Pos(x, y, z, isOnGround, collision);
    }

    @Override
    public ServerboundMovePlayerPacket createLookAndOnGround(
            float yaw, float pitch, boolean isOnGround, boolean collision) {
        // 1.21.1 版本不支持 collision 参数，忽略它
        return new ServerboundMovePlayerPacket.Rot(yaw, pitch, isOnGround, collision);
    }

    @Override
    public ServerboundMovePlayerPacket createFull(
            double x, double y, double z, float yaw, float pitch, boolean isOnGround, boolean collision) {
        // 1.21.1 版本不支持 collision 参数，忽略它
        return new ServerboundMovePlayerPacket.PosRot(x, y, z, yaw, pitch, isOnGround, collision);
    }

    public ServerboundMoveVehiclePacket createVehicleMove(Entity entity) {
        return ServerboundMoveVehiclePacket.fromEntity(entity);
    }
}
