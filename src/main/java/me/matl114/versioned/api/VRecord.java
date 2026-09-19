package me.matl114.versioned.api;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import java.util.UUID;
import net.minecraft.world.item.component.ResolvableProfile;

public interface VRecord {
    public static UUID getId(GameProfile profile) {
        return profile.id();
    }

    public static String getName(GameProfile profile) {
        return profile.name();
    }

    public static PropertyMap getProperties(GameProfile profile) {
        return profile.properties();
    }

    public static UUID getGameProfileId(ResolvableProfile profileComponent) {
        return profileComponent.partialProfile().id();
    }

    public static String getGameProfileName(ResolvableProfile profileComponent) {
        return profileComponent.partialProfile().name();
    }

    public static PropertyMap getGameProfileProperties(ResolvableProfile profileComponent) {
        return profileComponent.partialProfile().properties();
    }

    public static ResolvableProfile staticProfile(UUID uuid, String name, PropertyMap properties) {

        return ResolvableProfile.createResolved(new GameProfile(uuid, name, properties));
    }

    public static ResolvableProfile dynamicProfile(String name) {
        return ResolvableProfile.createUnresolved(name);
    }

    public static ResolvableProfile withProperty(ResolvableProfile component, PropertyMap properties) {
        return ResolvableProfile.createResolved(new GameProfile(
                component.partialProfile().id(), component.partialProfile().name(), properties));
    }

    public static PropertyMap createProperty(Multimap<String, Property> ppt) {
        return new PropertyMap(LinkedHashMultimap.create(ppt));
    }

    public static PropertyMap createProperty() {
        return new PropertyMap(LinkedHashMultimap.create());
    }
}
