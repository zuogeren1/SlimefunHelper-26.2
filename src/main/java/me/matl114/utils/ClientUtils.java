package me.matl114.utils;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.tree.CommandNode;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import com.mojang.blaze3d.pipeline.RenderTarget;

@ApiMethod
public class ClientUtils {
    private static final Minecraft mc = Minecraft.getInstance();

    public static boolean isPlayerOnline() {
        //#if MC >= 26.2
        return mc.player != null && !mc.gui.clientLevelTeardownInProgress;
        //#else
        //$$ return mc.player != null && !mc.clientLevelTeardownInProgress;
        //#endif
    }

    public static boolean isNetworkConnecting() {
        return mc.getSingleplayerServer() != null;
    }

    public static CompletableFuture<List<String>> getServerPluginResources() {
        String command = "/version ";
        return getServerCommandTabResult(command);
    }

    public static List<String> getServerCommands() {
        return mc.getConnection().getCommands().getRoot().getChildren().stream()
                .map(CommandNode::getName)
                .toList();
    }

    public static CompletableFuture<List<String>> getServerCommandTabResult(String command) {
        StringReader ojReader = new StringReader(command);
        ojReader.skip();
        var dispatcher = mc.getConnection().getCommands();
        var parseResult = dispatcher.parse(ojReader, mc.getConnection().getSuggestionsProvider());
        return mc.getConnection()
                .getCommands()
                .getCompletionSuggestions(parseResult)
                .thenApply((suggestions -> {
                    return suggestions.getList().stream()
                            .map(Suggestion::getText)
                            .sorted()
                            .toList();
                }));
    }

    // ==================== 版本差异封装 ====================
    // 26.2 把「当前屏幕 / 覆盖层 / 聊天」搬到了 Minecraft.gui（一个 Gui 实例）上，
    // 26.1.2 上它们还挂在 Minecraft 自己身上。差异集中收在这几个方法里，
    // 业务代码两边共用同一份调用，不必逐处写 //#if。
    // 约定：mainProject（26.2）那一支正常书写，其它版本的分支用 //$$ 注释掉。

    /** 当前屏幕。26.2: {@code Minecraft.gui.screen()}；26.1.2: {@code Minecraft.screen} */
    public static Screen getScreen(Minecraft minecraft) {
        //#if MC >= 26.2
        return minecraft.gui.screen();
        //#else
        //$$ return minecraft.screen;
        //#endif
    }

    public static Screen getScreen() {
        return getScreen(mc);
    }

    /** 覆盖层界面。26.2: {@code Minecraft.gui.overlay()}；26.1.2: {@code Minecraft.getOverlay()} */
    public static Overlay getOverlay(Minecraft minecraft) {
        //#if MC >= 26.2
        return minecraft.gui.overlay();
        //#else
        //$$ return minecraft.getOverlay();
        //#endif
    }

    public static Overlay getOverlay() {
        return getOverlay(mc);
    }

    /** 切换屏幕。26.2: {@code Minecraft.gui.setScreen()}；26.1.2: {@code Minecraft.setScreen()} */
    public static void setScreen(Minecraft minecraft, Screen screen) {
        //#if MC >= 26.2
        minecraft.gui.setScreen(screen);
        //#else
        //$$ minecraft.setScreen(screen);
        //#endif
    }

    /** 聊天组件。26.2: {@code Minecraft.gui.hud.chat}；26.1.2: {@code Minecraft.gui.getChat()} */
    public static ChatComponent getChat(Minecraft minecraft) {
        //#if MC >= 26.2
        return minecraft.gui.hud.chat;
        //#else
        //$$ return minecraft.gui.getChat();
        //#endif
    }

    public static ChatComponent getChat() {
        return getChat(mc);
    }

    /** 主渲染目标。26.2: {@code gameRenderer.mainRenderTarget()}；26.1.2: {@code Minecraft.getMainRenderTarget()} */
    public static RenderTarget getMainRenderTarget(Minecraft minecraft) {
        //#if MC >= 26.2
        return minecraft.gameRenderer.mainRenderTarget();
        //#else
        //$$ return minecraft.getMainRenderTarget();
        //#endif
    }

    public static RenderTarget getMainRenderTarget() {
        return getMainRenderTarget(mc);
    }

    /** 打开聊天界面。26.2: {@code gui.openChatScreen()}；26.1.2: {@code gui.getChat().openScreen()} */
    public static void openChatScreen(Minecraft minecraft, ChatComponent.ChatMethod method) {
        //#if MC >= 26.2
        minecraft.gui.openChatScreen(method);
        //#else
        //$$ minecraft.openChatScreen(method);
        //#endif
    }
}