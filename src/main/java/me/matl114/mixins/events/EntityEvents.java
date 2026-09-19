package me.matl114.mixins.events;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.List;
import me.matl114.accessors.events.EntityAccess;
import me.matl114.events.Event;
import me.matl114.events.Listener;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(Entity.class)
public abstract class EntityEvents<T extends Entity> implements EntityAccess<T> {
    @ModifyExpressionValue(
            method = "moveRelative",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/Entity;getInputVector(Lnet/minecraft/world/phys/Vec3;FF)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 onModifyVelocity(Vec3 original) {
        if (!checkClientPlayer()) return original;
        Event<Vec3> vec3d = new Event<>(original, true, true);
        Listener.getPlayerVelocityTick().handleValue(vec3d);
        if (vec3d.isCancelled()) {
            return Vec3.ZERO;
        } else {
            return vec3d.context();
        }
    }

    @Shadow
    protected abstract void setSharedFlag(int index, boolean value);

    @Shadow
    protected abstract boolean getSharedFlag(int index);

    @Shadow
    protected abstract void checkFallDamage(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition);

    @Shadow
    public abstract InteractionResult interact(Player player, InteractionHand hand);

    @Unique
    public void setDataFlag(int index, boolean val) {
        this.setSharedFlag(index, val);
    }

    @Unique
    public boolean getDataFlag(int index) {
        return getSharedFlag(index);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void onEntityTickUpdate(CallbackInfo ci) {
        Entity entity = (Entity) (Object) (this);
        Listener.getEntityMidTickListener().broadcast(entity);
    }

    @Inject(method = "onSyncedDataUpdated(Ljava/util/List;)V", at = @At("HEAD"))
    public void onEntityDataUpdate(List<SynchedEntityData.DataValue<?>> dataEntries, CallbackInfo ci) {
        Entity entity = (Entity) (Object) (this);
        Listener.getEntityDataListener().broadcast(entity, dataEntries);
    }

    @Inject(method = "setRemoved", at = @At("RETURN"))
    public void onEntityRemoved(Entity.RemovalReason reason, CallbackInfo ci) {
        Listener.getEntityRemoveListener().broadcast((Entity) (Object) this, reason);
    }

    @WrapOperation(
            method = "updateFluidHeightAndDoFluidPushing",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/phys/Vec3;add(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
                            ordinal = 1))
    public Vec3 onEntityUpdateVelocity(
            Vec3 instance, Vec3 vec, Operation<Vec3> original, @Local(argsOnly = true) TagKey<Fluid> tagKey) {
        if (checkClientPlayer()) {
            Event<Vec3> eventVec3d = new Event<>(vec, true, true, tagKey);
            Listener.getPlayerFluidVelocityPoint().handleValue(eventVec3d);
            if (eventVec3d.isCancelled()) {
                return instance;
            }
            return original.call(instance, eventVec3d.context);
        } else {
            return original.call(instance, vec);
        }
    }
}
