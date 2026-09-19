package me.matl114.mixins.access;

import me.matl114.accessors.access.PlayerInteractEntityC2SPacketAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin(ServerboundInteractPacket.class)
public abstract class PlayerInteractEntityC2SPacketMixin implements PlayerInteractEntityC2SPacketAccess {
    // 26.2 的 ServerboundInteractPacket 已改为扁平 record(entityId, hand, location, usingSecondaryAction)，
    // 不再有 action 字段与 Action 内部类，攻击语义移至独立的 ServerboundAttackPacket。
    @Override
    @Mutable
    @Accessor("entityId")
    public abstract void setEntityId(int entityId);

    @Mutable
    @Accessor("entityId")
    @Override
    public abstract int getEntityId();

    @Override
    @Mutable
    @Accessor("usingSecondaryAction")
    public abstract void setPlayerSneaking(boolean playerSneaking);
}
