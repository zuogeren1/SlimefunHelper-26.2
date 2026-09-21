package me.matl114.utils;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.tree.CommandNode;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;

@ApiMethod
public class ClientUtils {
    private static final Minecraft mc = Minecraft.getInstance();

    public static boolean isPlayerOnline() {
        return mc.player != null && !mc.clientLevelTeardownInProgress;
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
}
