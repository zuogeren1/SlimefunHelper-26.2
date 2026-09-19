package me.matl114.mixins.versioned;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.matl114.hacks.modules.combat.SpearEnhance;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.effects.SpearAnimations;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(SpearAnimations.class)
public abstract class LancingVersionedSpearMixin {
    @WrapOperation(
            method = "thirdPersonHandUse",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;"))
    private static Object fixSpearRender1(
            ItemStack instance, DataComponentType componentType, Operation<Object> original) {
        if (SpearEnhance.INSTANCE.fixOldVersionSpear.get()) {
            return SpearEnhance.INSTANCE.getRealComponent(instance);
        }
        return original.call(instance, componentType);
    }

    @WrapOperation(
            method = "thirdPersonUseItem",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;"))
    private static Object fixSpearRender2(
            ItemStack instance, DataComponentType componentType, Operation<Object> original) {
        if (SpearEnhance.INSTANCE.fixOldVersionSpear.get()) {
            return SpearEnhance.INSTANCE.getRealComponent(instance);
        }
        return original.call(instance, componentType);
    }

    @WrapOperation(
            method = "thirdPersonAttackItem",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;"))
    private static Object fixSpearRender3(
            ItemStack instance, DataComponentType componentType, Operation<Object> original) {
        if (SpearEnhance.INSTANCE.fixOldVersionSpear.get()) {
            return SpearEnhance.INSTANCE.getRealComponent(instance);
        }
        return original.call(instance, componentType);
    }

    @WrapOperation(
            method = "firstPersonUse",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;"))
    private static Object fixSpearRender4(
            ItemStack instance, DataComponentType componentType, Operation<Object> original) {
        if (SpearEnhance.INSTANCE.fixOldVersionSpear.get()) {
            return SpearEnhance.INSTANCE.getRealComponent(instance);
        }
        return original.call(instance, componentType);
    }
}
