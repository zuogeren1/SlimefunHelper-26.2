package me.matl114.mixins.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import me.matl114.events.RenderListener;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.resources.model.ClientItemInfoLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(ClientItemInfoLoader.class)
public abstract class ItemAssetsLoaderEvents {
    @Inject(method = "scheduleLoad", at = @At("RETURN"), cancellable = true)
    private static void onItemAssetLoad(
            ResourceManager resourceManager,
            Executor executor,
            CallbackInfoReturnable<CompletableFuture<ClientItemInfoLoader.LoadedClientInfos>> cir) {
        CompletableFuture<ClientItemInfoLoader.LoadedClientInfos> future = cir.getReturnValue();
        CompletableFuture<Map<Identifier, ClientItem>> customLoadingAssets = CompletableFuture.supplyAsync(
                () -> {
                    Collection<Identifier> ids = RenderListener.getReloadingResources(resourceManager);
                    Map<Identifier, ClientItem> autoAssets = new HashMap<>(ids.size());
                    for (Identifier id : ids) {
                        autoAssets.put(
                                id,
                                new ClientItem(
                                        new CuboidItemModelWrapper.Unbaked(id, java.util.Optional.empty(), new ArrayList<>()),
                                        new ClientItem.Properties(true, true, 1.0F)));
                    }
                    return autoAssets;
                },
                executor);
        cir.setReturnValue(CompletableFuture.allOf(future, customLoadingAssets).thenApplyAsync((async) -> {
            ClientItemInfoLoader.LoadedClientInfos result = future.join();
            Map<Identifier, ClientItem> autoAssets = customLoadingAssets.join();
            for (var entry : autoAssets.entrySet()) {
                // do not override models that already exists
                result.contents().putIfAbsent(entry.getKey(), entry.getValue());
            }
            return result;
        }));
    }
}
