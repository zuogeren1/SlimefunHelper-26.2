package me.matl114.mixins.fix;

import me.matl114.hacks.modules.extra.ClientExtra;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.IdMap;
import net.minecraft.world.level.chunk.GlobalPalette;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(GlobalPalette.class)
public abstract class IdListPaletteFixMixin {
    @Shadow
    @Final
    private IdMap<?> registry;

    @Inject(
            method = "valueFor",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/MissingPaletteEntryException;<init>(I)V"),
            cancellable = true)
    private void get(CallbackInfoReturnable<Object> cir) {
        if (ClientExtra.INSTANCE.paletteException.get()) {
            cir.setReturnValue(this.registry.byId(0));
        }
    }
}
