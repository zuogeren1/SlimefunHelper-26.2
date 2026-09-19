package me.matl114.mixins.events;

import me.matl114.events.Event;
import me.matl114.events.GlobalEventVars;
import me.matl114.events.Listener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Main.class)
public abstract class MainEvents {

    @Inject(
            method = "main",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;run()V", shift = At.Shift.AFTER))
    private static void onMain(String[] args, CallbackInfo ci) {
        // handled crash in the printCrashReportMixin\\
        // printCrashReport will call System.exit, if we see a crashReport here then it is cancelled in the event here
        if (Minecraft.getInstance() == null) {
            return;
        }
        if (GlobalEventVars.crashReport != null) {
            GlobalEventVars.crashReport = null;
            GlobalEventVars.crashReportEvent = null;
            mainLoop(Minecraft.getInstance());
        } else {
            // not a crash
            if (GlobalEventVars.crashReportEvent == null) {
                Minecraft mc = Minecraft.getInstance();
                GlobalEventVars.crashReportEvent = new Event<>(mc, mc.isRunning(), false, (Object) null);
                if (!Listener.getClientMainExit().isEmpty()) {
                    Listener.getClientMainExit().handleValue(GlobalEventVars.crashReportEvent);
                }
            }
            if (Minecraft.getInstance().isRunning()) {
                if (GlobalEventVars.crashReportEvent.isCancelled()) {
                    GlobalEventVars.crashReportEvent = null;
                    GlobalEventVars.crashReport = null;
                    mainLoop(Minecraft.getInstance());
                } else {
                    GlobalEventVars.crashReportEvent = null;
                    GlobalEventVars.crashReport = null;
                    return;
                }
            } else {
                GlobalEventVars.crashReportEvent = null;
            }
        }
    }

    private static void mainLoop(Minecraft mc) {
        while (true) {
            GlobalEventVars.crashReportEvent = null;
            GlobalEventVars.crashReport = null;
            mc.run();
            if (GlobalEventVars.crashReport != null) {
                GlobalEventVars.crashReport = null;
            } else {
                if (!Listener.getClientMainExit().isEmpty()) {
                    Event<Minecraft> event = new Event<>(mc, mc.isRunning(), false, (Object) null);
                    Listener.getClientMainExit().handleValue(event);
                    if (!event.isCancelled()) {
                        break;
                    }
                }
            }
        }
    }
}
