package me.matl114.mixins.hack;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.matl114.hacks.RenderTasks;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderer.class)
public abstract class EntityRenderDisplayNameMixin {
    @WrapOperation(
            // shouldShowName 的调用点在 EntityRenderer.extractRenderState(T,S,float) 里
            method =
                    "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/entity/EntityRenderer;shouldShowName(Lnet/minecraft/world/entity/Entity;D)Z"))
    private boolean hasLabel(
            EntityRenderer instance, Entity entity, double squaredDistanceToCamera, Operation<Boolean> original) {
        if (entity instanceof Player pl) {
            if (RenderTasks.getNameTag().hideName.get()) {
                return false;
            }
        }
        return original.call(instance, entity, squaredDistanceToCamera);
    }
}
