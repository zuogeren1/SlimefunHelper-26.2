package me.matl114.mixins.fix;

import java.util.OptionalInt;
import me.matl114.accessors.access.ProjectileAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(Projectile.class)
public abstract class ProjectileOwnerFixMixin extends Entity implements ProjectileAccess {
    @Shadow
    protected EntityReference<Entity> owner;

    @Shadow
    public abstract void setOwner(Entity owner);

    @Unique
    OptionalInt ownerEid = OptionalInt.empty();

    public ProjectileOwnerFixMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Inject(method = "recreateFromPacket", at = @At("RETURN"))
    private void onSpawnPacket(ClientboundAddEntityPacket packet, CallbackInfo ci) {
        if (packet.getData() == 0) {
            ownerEid = OptionalInt.empty();
        } else {
            ownerEid = OptionalInt.of(packet.getData());
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (ownerEid.isPresent() && this.owner == null) {
            var entity = this.level().getEntity(this.ownerEid.getAsInt());
            if (entity != null) {
                setOwner(entity);
            }
        }
    }

    @Override
    public OptionalInt getOwnerEid() {
        return ownerEid;
    }
}
