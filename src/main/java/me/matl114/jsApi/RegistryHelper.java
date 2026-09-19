package me.matl114.jsApi;

import java.util.Objects;
import me.matl114.utils.ApiMethod;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

@ApiMethod
public class RegistryHelper {
    private static Minecraft mc = Minecraft.getInstance();

    public static <T> Registry<T> getRegistry(String resourceKey) {
        return (Registry<T>) mc.getConnection()
                .registryAccess()
                .lookup(ResourceKey.createRegistryKey(Identifier.tryParse(resourceKey)))
                .orElseThrow();
    }

    public static <T> T getInRegistry(Registry<T> registry, String key) {
        return registry.getValue(Identifier.tryParse(key));
    }

    public static <T> String getIdInRegistry(Registry<T> registry, T value) {
        return Objects.requireNonNull(registry.getKey(value)).toString();
    }
}
