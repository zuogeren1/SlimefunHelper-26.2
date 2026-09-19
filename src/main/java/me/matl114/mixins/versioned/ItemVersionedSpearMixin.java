package me.matl114.mixins.versioned;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.matl114.hacks.modules.combat.SpearEnhance;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(Item.class)
public abstract class ItemVersionedSpearMixin {
    @WrapOperation(
            method = "getUseAnimation",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/item/ItemStack;has(Lnet/minecraft/core/component/DataComponentType;)Z",
                            ordinal = 1))
    private boolean fixSpearUse1(ItemStack instance, DataComponentType componentType, Operation<Boolean> original) {
        if (componentType == DataComponents.KINETIC_WEAPON && SpearEnhance.INSTANCE.fixOldVersionSpear.get()) {
            return SpearEnhance.INSTANCE.getRealComponent(instance) != null;
        }
        return original.call(instance, componentType);
    }

    @WrapOperation(
            method = "getUseDuration",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/item/ItemStack;has(Lnet/minecraft/core/component/DataComponentType;)Z",
                            ordinal = 1))
    private boolean fixSpearUse2(ItemStack instance, DataComponentType componentType, Operation<Boolean> original) {
        if (componentType == DataComponents.KINETIC_WEAPON && SpearEnhance.INSTANCE.fixOldVersionSpear.get()) {
            return SpearEnhance.INSTANCE.getRealComponent(instance) != null;
        }
        return original.call(instance, componentType);
    }
}
