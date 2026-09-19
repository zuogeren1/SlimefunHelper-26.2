package me.matl114.hacks.utils.render;

import me.matl114.managers.config.ConfigEnum;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.world.item.ItemStack;

public class ItemStackDisplayUtils {

    public static int getDamageDisplayColor(int damage, int damageMax) {
        damage = damageMax - damage;
        if (damage < damageMax * 0.33) {
            return CommonColors.RED;
        } else if (damage < damageMax * 0.66) {
            return CommonColors.YELLOW;
        } else {
            return CommonColors.GREEN;
        }
    }

    public static int getDamageDisplayColor(ItemStack stack) {
        return getDamageDisplayColor(stack.getDamageValue(), stack.getMaxDamage());
    }

    public static int getColorByPercentage(int percentage) {
        if (percentage <= 33) {
            return CommonColors.RED;
        } else if (percentage <= 66) {
            return CommonColors.YELLOW;
        } else {
            return CommonColors.GREEN;
        }
    }

    public static int getDurabilityPercentage(ItemStack stackOverride) {
        if (stackOverride.getMaxDamage() > 0) {
            return ((stackOverride.getMaxDamage() - stackOverride.getDamageValue()) * 100)
                    / stackOverride.getMaxDamage();
        } else {
            return 100;
        }
    }

    public static Component getDamageShowText(ItemStack stack, DamageDisplay display) {
        int damage2 = stack.getDamageValue();
        int damage = stack.getMaxDamage();
        int damageLeft = damage - damage2;
        return switch (display) {
            case DAMAGE -> {
                yield Component.literal("-%d".formatted(damage2));
            }
            case DAMAGE_LEFT -> {
                yield Component.literal("%d".formatted(damageLeft));
            }
            case PERCENTAGE -> {
                yield Component.literal("%d%%".formatted((damageLeft * 100) / damage));
            }
            default -> {
                yield null;
            }
        };
    }

    public static enum DamageDisplay implements ConfigEnum {
        NONE,
        DAMAGE_LEFT,
        DAMAGE,
        PERCENTAGE;

        @Override
        public String getConfigEnumType() {
            return "equipment_hud_damage_display_type";
        }
    }
}
