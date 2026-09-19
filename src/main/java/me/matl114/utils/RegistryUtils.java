package me.matl114.utils;

import static net.minecraft.core.registries.Registries.*;

import com.google.common.collect.ImmutableMap;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.minecraft.core.*;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class RegistryUtils {
    public static <T> Set<T> parseWhiteList(Registry<T> registry, Pattern regex) {
        Set<T> newBlocks = new LinkedHashSet<>();
        try {
            Predicate<String> predicate = regex.asMatchPredicate();
            for (var blockIds : registry.keySet()) {
                if (predicate.test(blockIds.getPath())) {
                    newBlocks.add(registry.getValue(blockIds));
                }
            }
        } catch (Throwable e) {

        }
        return newBlocks;
    }

    public static <T> Set<T> parseWhiteList(Registry<T> registry, String regex) {
        return parseWhiteList(registry, Pattern.compile(regex));
    }

    public static <T> Set<Holder<T>> parseEntryWhiteList(Registry<T> registry, String regex) {
        Set<Holder<T>> newBlocks = new LinkedHashSet<>();
        try {
            Predicate<String> predicate = Pattern.compile(regex).asMatchPredicate();
            for (var blockIds : registry.keySet()) {
                if (predicate.test(blockIds.getPath())) {
                    Holder<T> reg = registry.get(blockIds).orElse(null);
                    if (reg != null) {
                        newBlocks.add(reg);
                    }
                }
            }
        } catch (Throwable e) {

        }
        return newBlocks;
    }

    private static Class<?> fromType(Type type) {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        } else if (type instanceof ParameterizedType pt) {
            return (Class<?>) pt.getRawType();
        } else if (type instanceof TypeVariable<?> tv) {
            // 理论上不会出现，但可回退到第一个上界
            return fromType(tv.getBounds()[0]);
        } else if (type instanceof WildcardType wt) {
            // 取上界（extends）或下界（super），一般取第一个上界
            return fromType(wt.getUpperBounds()[0]);
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

    public static final Map<ResourceKey<?>, Class<?>> REGISTRY_KEY_TO_ICON;
    public static final HashMap<Class<?>, ResourceKey<?>> ICON_TO_REGISTRY_KEY;

    public static <T> Class<T> getRegistryType(Registry<T> registry) {
        return getRegistryType((ResourceKey<Registry<T>>) registry.key());
    }

    public static <T> Class<T> getRegistryType(ResourceKey<Registry<T>> key) {
        return (Class<T>) REGISTRY_KEY_TO_ICON.get(key);
    }

    public static <W> Holder<W> getRegistryEntry(HolderLookup.Provider lookup, ResourceKey<W> key) {
        return lookup.get(key).orElse(null);
    }

    public static <W> Holder<W> getRegistryEntry(
            RegistryAccess lookup, ResourceKey<? extends Registry<? extends W>> key, W value) {
        return lookup.lookup(key).map(s -> s.wrapAsHolder(value)).orElse(null);
    }

    public static <W> Registry<W> getRegistry(RegistryAccess lookup, ResourceKey<Registry<W>> key) {
        return lookup.lookup(key).orElse(null);
    }

    public static <T> ResourceKey<? extends Registry<T>> getRegistryTypeKey(T value) {
        Class<?> clazz = value.getClass();
        while (clazz != Object.class) {
            ResourceKey key = ICON_TO_REGISTRY_KEY.get(clazz);
            if (key != null) {
                ICON_TO_REGISTRY_KEY.put(value.getClass(), key);
                return key;
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    static {
        Map<ResourceKey<?>, Class<?>> clazzMap = new HashMap<>();
        try {
            for (Field field : Registries.class.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) || !Modifier.isFinal(field.getModifiers())) continue;

                Type genericType = field.getGenericType();
                // 必须是 ParameterizedType 且 raw type 为 RegistryKey
                if (!(genericType instanceof ParameterizedType keyType)) continue;
                if (keyType.getRawType() != ResourceKey.class) continue;

                Type[] keyArgs = keyType.getActualTypeArguments();
                if (keyArgs.length != 1) continue;
                Type registryKeyArg = keyArgs[0];

                // 期望这个参数是 Registry<X> 类型
                if (!(registryKeyArg instanceof ParameterizedType registryType)) continue;
                if (registryType.getRawType() != Registry.class) continue;

                // 取出 Registry<X> 中的 X
                Type[] registryArgs = registryType.getActualTypeArguments();
                if (registryArgs.length != 1) continue;
                Type elementType = registryArgs[0];
                Class<?> clazz = fromType(elementType);
                clazzMap.put((ResourceKey<?>) field.get(null), clazz);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        clazzMap.put(ITEM, Item.class);
        clazzMap.put(BLOCK, Block.class);
        clazzMap.put(ATTRIBUTE, Attribute.class);
        clazzMap.put(ENCHANTMENT, Enchantment.class);
        clazzMap.put(BLOCK_ENTITY_TYPE, BlockEntityType.class);
        clazzMap.put(ENTITY_TYPE, EntityType.class);
        clazzMap.put(MOB_EFFECT, MobEffect.class);
        REGISTRY_KEY_TO_ICON = ImmutableMap.copyOf(clazzMap);
        ICON_TO_REGISTRY_KEY = new HashMap<>(REGISTRY_KEY_TO_ICON.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey, (k, v) -> v)));
    }
}
