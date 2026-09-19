package me.matl114.mixins.fix;

import me.matl114.hacks.modules.extra.ClientExtra;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.level.chunk.LinearPalette;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(LinearPalette.class)
public abstract class ArrayPaletteFixMixin<T> {
    @Shadow
    @Final
    private T[] values;

    @Inject(
            method = "valueFor",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/level/chunk/MissingPaletteEntryException;<init>(I)V"),
            cancellable = true)
    private void onException(int id, CallbackInfoReturnable<T> cir) {
        if (ClientExtra.INSTANCE.paletteException.get()) {
            cir.setReturnValue(this.values[0]);
        }
    }
}
