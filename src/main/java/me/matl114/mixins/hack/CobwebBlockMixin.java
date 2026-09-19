package me.matl114.mixins.hack;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import me.matl114.events.Event;
import me.matl114.events.Listener;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(WebBlock.class)
public abstract class CobwebBlockMixin {
    @Inject(
            method = "entityInside",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/Entity;makeStuckInBlock(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/phys/Vec3;)V",
                            shift = At.Shift.BEFORE),
            cancellable = true)
    public void onEntityCollision(
            BlockState state,
            Level world,
            BlockPos pos,
            Entity entity,
            InsideBlockEffectApplier handler,
            boolean bl,
            CallbackInfo ci,
            @Local Vec3 vec3d,
            @Local LocalRef<Vec3> vec3dLocalRef) {
        if (entity == Minecraft.getInstance().player) {
            Event<Vec3> slowMovement = new Event<>(vec3d, true, true, pos);
            Listener.getPlayerWebSlowPoint().handleValue(slowMovement);
            if (slowMovement.isCancelled()) {
                ci.cancel();
            } else {
                if (slowMovement.context != vec3d) {
                    vec3dLocalRef.set(slowMovement.context);
                }
            }
        }
    }
}
