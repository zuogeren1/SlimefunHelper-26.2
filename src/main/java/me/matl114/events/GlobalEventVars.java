package me.matl114.events;

import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;

public class GlobalEventVars {
    public static CrashReport crashReport = null;
    public static Event<Minecraft> crashReportEvent = null;
    public static AtomicInteger cmd = new AtomicInteger(0);

    public int getModCnt() {
        return cmd.incrementAndGet();
    }
}
