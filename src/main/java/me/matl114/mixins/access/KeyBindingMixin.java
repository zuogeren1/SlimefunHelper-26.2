package me.matl114.mixins.access;

import com.mojang.blaze3d.platform.InputConstants;
import me.matl114.accessors.hacks.KeyBindAccess;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(KeyMapping.class)
public abstract class KeyBindingMixin implements KeyBindAccess {
    @Shadow
    private InputConstants.Key key;

    @Unique
    private static final Minecraft mc = Minecraft.getInstance();

    public void resetKeyState() {
        var handle = mc.getWindow();
        int code = key.getValue();
        if (key.getType() == InputConstants.Type.MOUSE) setDown(GLFW.glfwGetMouseButton(handle.handle(), code) == 1);
        else setDown(InputConstants.isKeyDown(handle, code));
    }

    @Shadow
    public abstract void setDown(boolean b);
}
