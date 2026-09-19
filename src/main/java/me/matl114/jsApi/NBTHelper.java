package me.matl114.jsApi;

import com.google.gson.JsonElement;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.JavaOps;
import com.mojang.serialization.JsonOps;
import me.matl114.utils.ApiMethod;
import me.matl114.versioned.api.VNbt;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

@ApiMethod
public class NBTHelper {
    public static Object convertNbtToJava(Object element) {
        return NbtOps.INSTANCE.convertTo(JavaOps.INSTANCE, JsHelper.unwrap(element, Tag.class));
    }

    public static Tag convertJavaToNbt(Object object) {
        return JavaOps.INSTANCE.convertTo(NbtOps.INSTANCE, object);
    }

    public static JsonElement convertNbtToJson(Object element) {
        return NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, JsHelper.unwrap(element, Tag.class));
    }

    public static Tag convertJsonToNbt(JsonElement object) {
        return JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, object);
    }

    public static String nbtToString(Object element) {
        return VNbt.getInstance().writeNbt(JsHelper.unwrap(element, Tag.class));
    }

    public static Tag stringToNbt(String string) throws CommandSyntaxException {
        return VNbt.getInstance().readNbt(string);
    }
}
