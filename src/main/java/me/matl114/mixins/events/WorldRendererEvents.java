package me.matl114.mixins.events;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import me.matl114.events.Event;
import me.matl114.events.RenderListener;
import me.matl114.versioned.impl.Render_v1_21_11;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class WorldRendererEvents {
    /**
     * 26.2 的世界渲染改为两阶段：先 {@code submitFeatures(...)} 把所有内容提交进
     * {@link SubmitNodeCollector}，随后 {@code prepareFrame(...)} 统一排序渲染。
     *
     * <p>因此自定义几何的注入点从旧的 {@code renderLevel} RETURN 改到 {@code submitFeatures}
     * 的 RETURN —— 此时 vanilla 内容已提交完毕，我们追加的几何会与之一起参与排序与渲染，
     * 深度与遮挡关系保持正确。
     */
    @Inject(method = "submitFeatures", at = @At("RETURN"))
    private void onAfterSubmitFeatures(
            LevelRenderState levelRenderState,
            SubmitNodeCollector submitNodeCollector,
            boolean renderOutline,
            CallbackInfo ci) {
        CameraRenderState cameraState = levelRenderState.cameraRenderState;
        RenderListener.setWorldModelViewMatrix(new Matrix4f(cameraState.viewRotationMatrix));
        RenderListener.setWorldProjectionMatrix(new Matrix4f(cameraState.projectionMatrix));

        PoseStack matrixStack = new PoseStack();
        matrixStack.mulPose(cameraState.viewRotationMatrix);

        Render_v1_21_11.beginSubmit(submitNodeCollector, matrixStack);
        try {
            DeltaTracker tracker = Minecraft.getInstance().getDeltaTracker();
            float tickDelta = tracker == null ? 0.0F : tracker.getGameTimeDeltaPartialTick(false);
            RenderListener.renderWorldTasks(matrixStack, tickDelta);
        } finally {
            Render_v1_21_11.endSubmit();
        }
    }

    @WrapOperation(
            method = "extractVisibleEntities",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;shouldRender(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/culling/Frustum;DDD)Z"))
    public boolean onEntityRenderEvent(
            EntityRenderDispatcher instance,
            Entity entity,
            Frustum frustum,
            double x,
            double y,
            double z,
            Operation<Boolean> original) {
        Event<Entity> event = new Event<>(entity, true, false);
        RenderListener.getEntityRenderListener().handleValue(event);
        if (event.isCancelled()) {
            return false;
        }
        return original.call(instance, entity, frustum, x, y, z);
    }
}
