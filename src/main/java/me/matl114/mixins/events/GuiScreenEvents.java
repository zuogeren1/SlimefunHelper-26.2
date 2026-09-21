package me.matl114.mixins.events;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import me.matl114.accessors.access.GuiScreenAccess;
import me.matl114.events.Event;
import me.matl114.events.Listener;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 屏幕事件：PreSetScreen / MidSetScreen / PostSetScreen。
 *
 * <p>屏幕管理在 26.1.2 属于 {@link Minecraft}（{@code Minecraft.screen} 字段 +
 * {@code Minecraft.setScreen}）；26.2 才整体搬进 {@code Gui}
 * （{@code Gui.screen} / {@code Gui.setScreen}）。所以这里挂在 {@code Minecraft.setScreen}。
 */
@Environment(EnvType.CLIENT)
@Mixin(Minecraft.class)
public abstract class GuiScreenEvents implements GuiScreenAccess {

    @Unique
    @Override
    public void setScreenRaw(Screen screen) {
        this.screen = screen;
    }

    @Shadow
    @Nullable
    public Screen screen;

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    public void onPreSetScreen(Screen screen, CallbackInfo ci, @Local(argsOnly = true) LocalRef<Screen> screenRef) {
        if (!Listener.getPreSetScreen().isEmpty()) {
            Event<Screen> screenEvent = new Event<>(screen, true, true);
            Listener.getPreSetScreen().handleValue(screenEvent);
            if (screenEvent.isCancelled()) {
                ci.cancel();
            } else {
                if (screenEvent.context != screen) {
                    screenRef.set(screenEvent.context);
                }
            }
        }
    }

    /**
     * 在 {@code this.screen = screen} 赋值之后触发，此时 {@code this.screen} 已是新屏幕、
     * 但尚未 {@code added()/init()}。
     *
     * <p>ordinal 数的是**字节码里对 screen 字段的访问序号（读+写都算）**。26.1.2 的
     * {@code Minecraft.setScreen} 里依次是 ①getfield(判空) ②getfield(removed)
     * ③putfield(赋值) ④getfield(判空) ⑤getfield(added)，所以赋值那次是 ordinal = 2。
     * 这个数只能用 javap 数，源码里数会漏掉读操作。
     */
    @Inject(
            method = "setScreen",
            at =
                    @At(
                            value = "FIELD",
                            target = "Lnet/minecraft/client/Minecraft;screen:Lnet/minecraft/client/gui/screens/Screen;",
                            ordinal = 2,
                            shift = At.Shift.AFTER),
            cancellable = true)
    public void onMidSetScreen(Screen screen, CallbackInfo ci) {
        if (!Listener.getMidSetScreen().isEmpty()) {
            Event<Screen> screenEvent = new Event<>(this.screen, true, false);
            Listener.getMidSetScreen().handleValue(screenEvent);
            if (screenEvent.isCancelled()) {
                ci.cancel();
                // FIX: even if post set is cancelled , the screen must be initialized or exception will be thrown
                if (this.screen != null) {
                    Minecraft mc = Minecraft.getInstance();
                    this.screen.init(
                            mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
                }
                Listener.getPostSetScreen().broadcast(this.screen);
            }
        }
    }

    @Inject(method = "setScreen", at = @At("RETURN"))
    public void onPostSetScreen(Screen callbackScreen, CallbackInfo ci) {
        Listener.getPostSetScreen().broadcast(this.screen);
    }
}
