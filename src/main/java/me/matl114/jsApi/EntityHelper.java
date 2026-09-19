package me.matl114.jsApi;

import java.util.Locale;
import me.matl114.accessors.events.EntityAccess;
import me.matl114.utils.ApiMethod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;

@ApiMethod
public class EntityHelper {
    public static int getEntityId(Entity entity) {
        return entity.getId();
    }

    public static boolean getEntityFlag(Entity entity, int flag) {
        return EntityAccess.of(entity).getDataFlag(flag);
    }

    public static void setEntityFlag(Entity entity, int flag, boolean value) {
        EntityAccess.of(entity).setDataFlag(flag, value);
    }

    public static Pose getEntityPose(Entity entity) {
        return entity.getPose();
    }

    public static void setEntityPose(Entity entity, String pose) {
        entity.setPose(Pose.valueOf(pose.toUpperCase(Locale.ROOT)));
    }

    public static Vec3 getEntityVelocity(Entity entity) {
        return entity.getDeltaMovement();
    }

    public static void setEntityVelocity(Entity entity, Vec3 velocity) {
        entity.setDeltaMovement(velocity);
    }

    public static EntityType getEntityType(Entity entity) {
        return entity.getType();
    }

    public static String getEntityTypeName(EntityType entityType) {
        return EntityType.getKey(entityType).toString();
    }

    public static EntityType getEntityTypeByName(String name) {
        return BuiltInRegistries.ENTITY_TYPE
                .getOptional(net.minecraft.resources.Identifier.tryParse(name))
                .orElse(null);
    }
}
