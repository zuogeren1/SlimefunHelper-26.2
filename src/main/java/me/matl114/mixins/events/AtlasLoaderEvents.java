package me.matl114.mixins.events;

import com.llamalad7.mixinextras.sugar.Local;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import me.matl114.events.Event;
import me.matl114.events.RenderListener;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceList;
import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(SpriteSourceList.class)
public abstract class AtlasLoaderEvents {
    @Inject(
            method = "load",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/texture/atlas/SpriteSourceList;<init>(Ljava/util/List;)V",
                            shift = At.Shift.BEFORE),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private static void loadSources(
            ResourceManager resourceManager,
            Identifier id,
            CallbackInfoReturnable<SpriteSourceList> cir,
            @Local List<SpriteSource> list) {
        Event<Set<Identifier>> resourceReloadEvent =
                new Event<>(new LinkedHashSet<>(), false, false, resourceManager, id);
        RenderListener.getAtlasSourceSupply().handleValue(resourceReloadEvent);
        list.addAll(resourceReloadEvent.context().stream()
                .map(i -> new SingleFile(i, Optional.empty()))
                .toList());
    }
}
