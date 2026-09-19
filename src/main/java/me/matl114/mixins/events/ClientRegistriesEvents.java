package me.matl114.mixins.events;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.List;
import java.util.Map;
import me.matl114.events.Event;
import me.matl114.events.Listener;
import net.minecraft.client.multiplayer.RegistryDataCollector;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RegistryDataCollector.class)
public abstract class ClientRegistriesEvents {
    @ModifyExpressionValue(
            method = "resolveRegistryTags",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/tags/TagNetworkSerialization$NetworkPayload;resolve(Lnet/minecraft/core/Registry;)Lnet/minecraft/tags/TagLoader$LoadResult;"))
    private static <T> TagLoader.LoadResult<T> onRegistryTagload(
            TagLoader.LoadResult<T> original,
            @Local(argsOnly = true) ResourceKey<? extends Registry<? extends T>> registryKey) {
        Map<TagKey<T>, List<Holder<T>>> tagMap = original.tags();
        Event<Map<TagKey<T>, List<Holder<T>>>> event = new Event<>(tagMap, false, true, original.key());
        Listener.getRegistryTagKeyReload().handleValue((Event) event);
        if (event.context != tagMap) {
            return new TagLoader.LoadResult<>(original.key(), event.context);
        }
        return original;
    }
}
