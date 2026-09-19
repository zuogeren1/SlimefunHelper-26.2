package me.matl114.mixins.fix;

import me.matl114.accessors.interfaces.MetadataHolder;
import me.matl114.hacks.RenderTasks;
import me.matl114.hacks.modules.render.RenderOptimize;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSignRenderer.class)
public abstract class SignBlockEntityRendererFixMixin {

    @Inject(
            method =
                    "extractRenderState(Lnet/minecraft/world/level/block/entity/SignBlockEntity;Lnet/minecraft/client/renderer/blockentity/state/SignRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
            at = @At("RETURN"))
    public void onSignBlockEntityStateUpdate(
            SignBlockEntity signBlockEntity,
            SignRenderState signBlockEntityRenderState,
            float f,
            Vec3 vec3d,
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlayCommand,
            CallbackInfo ci) {
        RenderOptimize optimize = RenderTasks.getRenderOptimize();
        if (optimize.enableBlockLabelRenderOpt.get()
                && signBlockEntity instanceof MetadataHolder holder
                && holder.getMetadata().get(optimize, RenderOptimize.KEY_RENDER_CONTROL)
                        instanceof RenderOptimize.RenderController controller) {
            if (controller.hideLabelBack()) {
                signBlockEntityRenderState.backText = null;
            }
            if (controller.hideLabelFront()) {
                signBlockEntityRenderState.frontText = null;
            }
        }
    }
}
