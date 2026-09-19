package me.matl114.events;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

/**
 * 流体事件跨层传参用的上下文。
 *
 * <p>26.2 的调用链里流体类型只在外层、速度向量只在内层：
 * {@code EntityFluidInteraction.applyCurrentTo(TagKey, Entity, double)}
 * → {@code Tracker.applyCurrentTo(Entity, double)}
 * → {@code Entity.addDeltaMovement(Vec3)}。
 * 两个 mixin 分处内外两层，只能靠这里把 {@code TagKey<Fluid>} 传过去。
 *
 * <p>注意：不能把这种 public static 字段放在 Mixin 类里 ——
 * Mixin 会报 {@code contains non-private static field} 并拒绝应用。
 */
public final class FluidTagContext {

    /** 当前正在处理的流体类型，仅在 Tracker.applyCurrentTo 调用期间有效。 */
    public static final ThreadLocal<TagKey<Fluid>> CURRENT = new ThreadLocal<>();

    private FluidTagContext() {}
}
