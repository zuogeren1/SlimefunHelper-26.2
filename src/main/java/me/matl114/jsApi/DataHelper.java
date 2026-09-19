package me.matl114.jsApi;

import me.matl114.utils.ApiMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3f;

@ApiMethod
public class DataHelper {
    public static Vec3 createVec(double x, double y, double z) {
        return new Vec3(x, y, z);
    }

    public static Object createPos3d(double x, double y, double z) {
        return JsMacrosBridge.getInstance().wrap(new Vec3(x, y, z));
    }

    public static Vec3 createVec(Object pos3d) {
        if (pos3d instanceof Vec3) {
            return (Vec3) pos3d;
        } else if (pos3d instanceof Vec3i pos) {
            return new Vec3(pos.getX(), pos.getY(), pos.getZ());
        } else if (pos3d instanceof Vector3d pos3D) {
            return new Vec3(pos3D.x, pos3D.y, pos3D.z);
        } else if (pos3d instanceof Vector3f pos3D) {
            return new Vec3(pos3D.x, pos3D.y, pos3D.z);
        } else {
            Object cast = JsMacrosBridge.getInstance().forceUnwrap(pos3d, Object.class);
            if (cast instanceof Vec3 vec3d) {
                return vec3d;
            } else if (cast instanceof Vec3i vec3i) {
                return Vec3.atLowerCornerOf(vec3i);
            } else {
                throw new IllegalArgumentException("Unsupported vec3 type: " + pos3d.getClass());
            }
        }
    }

    public static Object createPos3d(Object pos) {
        return JsMacrosBridge.getInstance().wrap(createVec(pos));
    }

    public static BlockPos createBlockPos(double x, double y, double z) {
        return new BlockPos((int) x, (int) y, (int) z);
    }

    public static BlockPos createBlockPos(Object pos3d) {
        return pos3d instanceof BlockPos pp ? pp : BlockPos.containing(createVec(pos3d));
    }

    public static double getVecX(Vec3 vec3d) {
        return vec3d.x;
    }

    public static double getVecY(Vec3 vec3d) {
        return vec3d.y;
    }

    public static double getVecZ(Vec3 vec3d) {
        return vec3d.z;
    }

    public static double length(Object vec3d) {
        return createVec(vec3d).length();
    }

    public static double lengthSquared(Object vec3d) {
        return createVec(vec3d).lengthSqr();
    }

    public static double horizontalDistance(Object vec3d) {
        return createVec(vec3d).horizontalDistance();
    }

    public static double horizontalDistanceSquared(Object vec3d) {
        return createVec(vec3d).horizontalDistanceSqr();
    }

    public static BlockPos vecToBlockPos(Vec3 vec3d) {
        return BlockPos.containing(vec3d);
    }

    public static Identifier namespacedKey(String id) {
        return Identifier.tryParse(id);
    }

    public static String getIdNamespace(Identifier id) {
        return id.getNamespace();
    }

    public static String getIdKey(Identifier id) {
        return id.getPath();
    }

    public static double squaredDistance(Object vec1, Object vec2) {
        Vec3 vec3d1 = createVec(vec1);
        Vec3 vec3d2 = createVec(vec2);
        return vec3d1.distanceToSqr(vec3d2);
    }

    public static Vec3 normalize(Object vec1) {
        return createVec(vec1).normalize();
    }
}
