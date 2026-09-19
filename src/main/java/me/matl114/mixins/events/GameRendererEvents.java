package me.matl114.mixins.events;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import me.matl114.events.Event;
import me.matl114.events.RenderListener;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class GameRendererEvents {

    @ModifyExpressionValue(
            method = "renderLevel",
            at =
                    @At(
                            // 26.2: 不再走 OptionInstance.get()，改为读 OptionsRenderState.bobView 字段
                            value = "FIELD",
                            target = "Lnet/minecraft/client/renderer/state/OptionsRenderState;bobView:Z",
                            ordinal = 0))
    private boolean onRenderWorld(boolean bobView, @Local PoseStack matrixStack) {
        Event<PoseStack> event = new Event<>(matrixStack, true, false);
        if (!bobView) {
            event.cancel();
        }
        RenderListener.getApplyWorldBobView().handleValue(event);
        return !event.isCancelled();
    }
}
