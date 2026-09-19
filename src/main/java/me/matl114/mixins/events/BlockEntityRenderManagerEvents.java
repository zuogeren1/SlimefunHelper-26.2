package me.matl114.mixins.events;

import com.mojang.blaze3d.vertex.PoseStack;
import me.matl114.events.Event;
import me.matl114.events.RenderListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderManagerEvents {
    // this method clash with sodium
    //    @Inject(method = "getRenderState", at = @At("HEAD"), cancellable = true)
    //    public  void onRenderBlockEntity(BlockEntity blockEntity, float tickProgress,
    // ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay, CallbackInfoReturnable<BlockEntityRenderState>
    // cir){
    //
    //    }
    //
    @Inject(
            method = "submit",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;submit(Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V"),
            cancellable = true)
    public void onRenderBlockEntity(
            BlockEntityRenderState renderState,
            PoseStack matrices,
            SubmitNodeCollector queue,
            CameraRenderState cameraRenderState,
            CallbackInfo ci) {
        BlockPos pos = renderState.blockPos;
        ClientLevel world = Minecraft.getInstance().level;
        if (world != null) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity != null) {
                Event<BlockEntity> event = new Event<>(blockEntity, true, false);
                RenderListener.getBlockEntityRenderListener().handleValue(event);
                if (event.isCancelled()) {
                    ci.cancel();
                }
            }
        }
    }
}
