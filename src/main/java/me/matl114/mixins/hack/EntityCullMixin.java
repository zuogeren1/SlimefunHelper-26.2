package me.matl114.mixins.hack;

import me.matl114.events.Event;
import me.matl114.events.RenderListener;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
//#if MC >= 260200
import net.minecraft.client.renderer.extract.LevelExtractor;
//#else
//$$ import net.minecraft.client.renderer.LevelRenderer;
//#endif
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 26.2 下 {@code RenderListener.getEntityRenderListener()} 的派发点。
 *
 * <p>1.21.11 由 {@code WorldRendererEvents} 挂在 {@code EntityRenderDispatcher.shouldRender} 上
 * 每帧对每个实体派发一次，{@code RenderOptimize.onEntityRender} 借 {@code event.cancel()}
 * 实现"渲染剔除"。26.2 该锚点已不存在（LevelRenderer 里没有任何调用点），
 * 所以派发点改到这里 —— 26.2 的世界渲染提取在 {@code LevelExtractor}：
 * {@code extractVisibleEntities(...)} → {@code extractEntity(Entity, float)}。
 *
 * <p><b>这里只派发事件，不写 {@code EntityRenderState.isInvisible}</b>。原因：
 * <ul>
 *   <li>{@code EntityRenderState} 跨帧复用，写进去没有可靠复位路径 → 实体永久隐形（已实测确认）。
 *   <li>「隐藏实体」({@code NoRender.entity.force-no}) 在两层版本里本来就是网络包层面的实现
 *       （取消 {@code ClientboundAddEntityPacket}，客户端不创建该实体），不需要渲染层兜底。
 *   <li>「隐形实体」({@code NoRender.entity.invisibility}) 由 {@code EntityMixin} 挂在
 *       {@code Entity.isInvisibleTo} 上处理 —— 该方法在 26.2 <b>仍然有效</b>
 *       （{@code LivingEntityRenderer.extractRenderState} 里
 *       {@code state.isInvisibleToPlayer = state.isInvisible && entity.isInvisibleTo(player)}），
 *       保持 1.21.11 的"半透明幽灵 + 无影子"观感，不需要额外写 {@code isInvisible}。
 * </ul>
 */
@Environment(EnvType.CLIENT)
//#if MC >= 260200
@Mixin(LevelExtractor.class)
//#else
//$$ @Mixin(LevelRenderer.class)
//#endif
public abstract class EntityCullMixin {

    @Inject(method = "extractEntity", at = @At("RETURN"))
    private void fireEntityRenderEvent(
            Entity entity, float partialTick, CallbackInfoReturnable<EntityRenderState> cir) {
        Event<Entity> event = new Event<>(entity, true, false);
        RenderListener.getEntityRenderListener().handleValue(event);
    }
}
