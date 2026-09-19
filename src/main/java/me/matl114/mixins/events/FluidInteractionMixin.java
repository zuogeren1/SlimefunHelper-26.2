package me.matl114.mixins.events;

import me.matl114.events.FluidTagContext;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityFluidInteraction;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 流体推动速度事件（外层）：负责把 {@code TagKey<Fluid>} 传给内层。
 *
 * <p>26.2 的调用链：
 * {@code EntityFluidInteraction.applyCurrentTo(TagKey, Entity, double)}
 * → {@code Tracker.applyCurrentTo(Entity, double)}
 * → {@code Entity.addDeltaMovement(Vec3)}。
 * 流体类型只在外层、速度向量只在内层，两头够不着，靠
 * {@link FluidTagContext} 把 tag 传给内层的 {@link FluidTrackerMixin}。
 *
 * <p>这里用 {@code @Inject} 而不是 {@code @WrapOperation} 包住
 * {@code Tracker.applyCurrentTo} —— 因为 Tracker 是 private 内部类，
 * WrapOperation 的 handler 签名要求写它的类型，Java 里引用不到。
 * 而外层方法的参数里本来就有 TagKey，直接拿即可。
 */
@Mixin(EntityFluidInteraction.class)
public abstract class FluidInteractionMixin {

    @Inject(method = "applyCurrentTo", at = @At("HEAD"))
    private void captureFluidTagStart(TagKey<Fluid> fluidTag, Entity entity, double scale, CallbackInfo ci) {
        FluidTagContext.CURRENT.set(fluidTag);
    }

    @Inject(method = "applyCurrentTo", at = @At("RETURN"))
    private void captureFluidTagEnd(TagKey<Fluid> fluidTag, Entity entity, double scale, CallbackInfo ci) {
        FluidTagContext.CURRENT.remove();
    }
}
