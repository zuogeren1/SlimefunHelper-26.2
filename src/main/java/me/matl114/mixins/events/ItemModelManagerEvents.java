package me.matl114.mixins.events;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import java.util.List;
import javax.annotation.Nullable;
import me.matl114.events.Event;
import me.matl114.events.RenderListener;
import me.matl114.events.model.GuiModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(ItemModelResolver.class)
public abstract class ItemModelManagerEvents {
    @Shadow
    public abstract void updateForTopItem(
            ItemStackRenderState renderState,
            ItemStack stack,
            ItemDisplayContext displayContext,
            @Nullable Level world,
            @Nullable ItemOwner heldItemContext,
            int seed);

    @Inject(method = "appendItemLayers", at = @At("HEAD"))
    public void onItemModelLoad(
            ItemStackRenderState renderState,
            ItemStack stack,
            ItemDisplayContext displayContext,
            Level world,
            ItemOwner heldItemContext,
            int seed,
            CallbackInfo ci,
            @Local(argsOnly = true) LocalRef<ItemStack> argument) {
        Event<ItemStack> itemStackEvent = new Event<>(stack, true, true);
        RenderListener.getItemDataOverrideForModel().handleValue(itemStackEvent);
        if (!itemStackEvent.isCancelled() && itemStackEvent.context() != stack) {
            argument.set(itemStackEvent.context());
        }
    }

    @ModifyExpressionValue(
            method = "appendItemLayers",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;"))
    public Object onItemModelOverride(Object original, @Local(argsOnly = true) ItemStack stack) {
        Event<Identifier> bakedModelEvent = new Event<>(null, true, true, stack);
        RenderListener.getCustomModelOverride().handleValue(bakedModelEvent);
        if (!bakedModelEvent.isCancelled()) {
            Identifier model = bakedModelEvent.context();
            if (model != null) {
                return model;
            }
        }
        // if no modification, just return the origin, do not return the null
        return original;
    }

    @Inject(
            method = "appendItemLayers",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/item/ItemModel;update(Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/renderer/item/ItemModelResolver;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/world/entity/ItemOwner;I)V"))
    public void onItemRenderDetached(
            ItemStackRenderState renderState,
            ItemStack stack,
            ItemDisplayContext displayContext,
            Level world,
            ItemOwner heldItemContext,
            int seed,
            CallbackInfo ci) {
        List<GuiModel> info = RenderListener.getContainedItemInfo(stack);
        GuiModel modelPack = GuiModel.packOrder(info);
        modelPack.update(
                renderState,
                stack,
                (ItemModelResolver) (Object) this,
                displayContext,
                world instanceof ClientLevel cli ? cli : null,
                heldItemContext,
                seed);
    }
}
