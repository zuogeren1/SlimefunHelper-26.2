package me.matl114.mixins.fix;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.StringUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(StringUtil.class)
public abstract class StringHelperFixMixin {
    @Inject(method = "isAllowedChatCharacter", at = @At("HEAD"), cancellable = true)
    private static void isValidChar(int c, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(c >= ' ' && c != 127);
    }
}
