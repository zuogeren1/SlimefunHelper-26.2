package me.matl114.mixins.fix;

import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardDebugCrashFix {
    @Shadow
    private long debugCrashKeyTime;

    @Inject(
            method = "tick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/ReportedException;<init>(Lnet/minecraft/CrashReport;)V",
                            shift = At.Shift.BEFORE))
    private void onResetDebugCrashTime(CallbackInfo ci) {
        debugCrashKeyTime = 0L;
    }
}
