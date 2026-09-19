package me.matl114.jsApi;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import me.matl114.utils.ApiMethod;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

@ApiMethod
public class KeyBindingHelper {
    public static Options options = Minecraft.getInstance().options;
    private static final Map<String, KeyMapping> keyBindings = new HashMap<String, KeyMapping>();

    static {
        try {
            Class<?> clazz = Options.class;
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getType() == KeyMapping.class) {
                    field.setAccessible(true);
                    KeyMapping keyBinding = (KeyMapping) field.get(options);
                    keyBindings.put(keyBinding.getName(), keyBinding);
                }
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static KeyMapping getKeyBinding(String key) {
        return keyBindings.get(key);
    }

    public static KeyMapping getFowardKeyBinding() {
        return getKeyBinding("key.forward");
    }

    public static KeyMapping getBackwardKeyBinding() {
        return getKeyBinding("key.back");
    }

    public static KeyMapping getLeftKeyBinding() {
        return getKeyBinding("key.left");
    }

    public static KeyMapping getRightKeyBinding() {
        return getKeyBinding("key.right");
    }

    public static KeyMapping getJumpKeyBinding() {
        return getKeyBinding("key.jump");
    }

    public static KeyMapping getSneakKeyBinding() {
        return getKeyBinding("key.sneak");
    }

    public static KeyMapping getSprintBinding() {
        return getKeyBinding("key.sprint");
    }

    public static KeyMapping getSwapKeyBinding() {
        return getKeyBinding("key.swapOffhand");
    }

    public static KeyMapping getInventoryKeyBinding() {
        return getKeyBinding("key.inventory");
    }

    public static KeyMapping getUseKeyBinding() {
        return getKeyBinding("key.use");
    }

    public static KeyMapping getAttackKeyBinding() {
        return getKeyBinding("key.attack");
    }

    public static KeyMapping getDropKeyBinding() {
        return getKeyBinding("key.drop");
    }

    public static void setPress(KeyMapping keyBinding, boolean pressed) {
        keyBinding.setDown(pressed);
    }

    public static boolean isPressed(KeyMapping keyBinding) {
        return keyBinding.isDown();
    }

    public static boolean wasPressed(KeyMapping keyBinding) {
        return keyBinding.consumeClick();
    }

    public static void reset(KeyMapping keyBinding) {
        while (keyBinding.consumeClick()) {}
        keyBinding.setDown(false);
    }
}
