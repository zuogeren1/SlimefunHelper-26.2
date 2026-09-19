package me.matl114.hacks;

import java.util.function.Consumer;
import lombok.Getter;
import me.matl114.hacks.api.ModuleManager;
import me.matl114.hacks.modules.ac.DisablerManager;
import me.matl114.hacks.modules.ac.PacketOrderManager;
import me.matl114.hacks.modules.ac.PostManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

public class ACTasks {
    public static void init() {}

    // represent that is there any anti-cheats transactions

    private static final Minecraft mc = Minecraft.getInstance();
    // anti grim's post check
    // sent after pong packet

    public static void addPostTickAction(Consumer<ClientPacketListener> handler) {
        postManager.addPostTickAction(handler);
    }

    public static void addPostTransactionAction(Consumer<ClientPacketListener> packet) {
        postManager.addNextPreTickAction(packet);
    }

    @Getter
    private static PostManager postManager;

    @Getter
    private static DisablerManager disablerManager;

    @Getter
    private static PacketOrderManager packetOrderManager;

    private static void initModules(ModuleManager moduleManager) {
        postManager = new PostManager().register(moduleManager);
        disablerManager = new DisablerManager().register(moduleManager);
        packetOrderManager = new PacketOrderManager().register(moduleManager);
    }

    static {
        ExtraTasks.getModuleManager().registerFactories(ACTasks::initModules);
    }
}
