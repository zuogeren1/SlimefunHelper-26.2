package me.matl114.versioned.api;

import me.matl114.versioned.impl.Packet_v1_21_11;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public interface VPacket {
    /**
     * 静态工厂方法 - 创建 OnGroundOnly 数据包
     * @param isOnGround 玩家是否在地面上
     * @param collision 是否发生碰撞（版本兼容参数）
     * @return PlayerMoveC2SPacket 实例
     */
    public static ServerboundMovePlayerPacket newOnGroundOnly(boolean isOnGround, boolean collision) {
        return getInstance().createOnGroundOnly(isOnGround, collision);
    }

    /**
     * 静态工厂方法 - 创建 PositionAndOnGround 数据包
     * @param x X坐标
     * @param y Y坐标
     * @param z Z坐标
     * @param isOnGround 玩家是否在地面上
     * @param collision 是否发生碰撞（版本兼容参数）
     * @return PlayerMoveC2SPacket 实例
     */
    public static ServerboundMovePlayerPacket newPositionAndOnGround(
            double x, double y, double z, boolean isOnGround, boolean collision) {
        return getInstance().createPositionAndOnGround(x, y, z, isOnGround, collision);
    }

    /**
     * 静态工厂方法 - 创建 LookAndOnGround 数据包
     * @param yaw 偏航角
     * @param pitch 俯仰角
     * @param isOnGround 玩家是否在地面上
     * @param collision 是否发生碰撞（版本兼容参数）
     * @return PlayerMoveC2SPacket 实例
     */
    public static ServerboundMovePlayerPacket newLookAndOnGround(
            float yaw, float pitch, boolean isOnGround, boolean collision) {
        return getInstance().createLookAndOnGround(yaw, pitch, isOnGround, collision);
    }

    public static ServerboundMoveVehiclePacket newVehicleMove(Entity entity) {
        return getInstance().createVehicleMove(entity);
    }

    /**
     * 静态工厂方法 - 创建 Full 数据包
     * @param x X坐标
     * @param y Y坐标
     * @param z Z坐标
     * @param yaw 偏航角
     * @param pitch 俯仰角
     * @param isOnGround 玩家是否在地面上
     * @param collision 是否发生碰撞（版本兼容参数）
     * @return PlayerMoveC2SPacket 实例
     */
    public static ServerboundMovePlayerPacket newFull(
            double x, double y, double z, float yaw, float pitch, boolean isOnGround, boolean collision) {
        return getInstance().createFull(x, y, z, yaw, pitch, isOnGround, collision);
    }

    public static boolean getCollisionFlag(ServerboundMovePlayerPacket packet) {
        return packet.horizontalCollision();
    }

    public static Vec3 getVelocity(ClientboundSetEntityMotionPacket entityVelocityUpdateS2CPacket) {
        return entityVelocityUpdateS2CPacket.movement();
    }

    /**
     * 实例方法 - 创建 OnGroundOnly 数据包
     */
    public ServerboundMovePlayerPacket createOnGroundOnly(boolean isOnGround, boolean collision);

    /**
     * 实例方法 - 创建 PositionAndOnGround 数据包
     */
    public ServerboundMovePlayerPacket createPositionAndOnGround(
            double x, double y, double z, boolean isOnGround, boolean collision);

    /**
     * 实例方法 - 创建 LookAndOnGround 数据包
     */
    public ServerboundMovePlayerPacket createLookAndOnGround(float yaw, float pitch, boolean isOnGround, boolean collision);

    /**
     * 实例方法 - 创建 Full 数据包
     */
    public ServerboundMovePlayerPacket createFull(
            double x, double y, double z, float yaw, float pitch, boolean isOnGround, boolean collision);

    public ServerboundMoveVehiclePacket createVehicleMove(Entity entity);
    /**
     * 获取当前版本的 VPacket 实例
     */
    public static final VPacket instance = new Packet_v1_21_11();

    private static VPacket getInstance() {
        // 这里应该根据实际版本检测逻辑来返回正确的实现
        // 暂时返回 1.21.1 的实现
        return instance;
    }
}
