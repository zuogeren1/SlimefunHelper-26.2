package me.matl114.utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.advancements.criterion.*;
import net.minecraft.advancements.criterion.DamageSourcePredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.ConditionalEffect;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.level.storage.loot.predicates.DamageSourceCondition;

public class DamageUtils {
    public static final Minecraft mc = Minecraft.getInstance();

    public static boolean isType(ResourceKey<DamageType> key, String type) {
        return key != null && Objects.equals(key.identifier().getPath(), type);
    }

    public static double getAttributeValue(
            Holder<Attribute> entry, Player player, ItemStack stack, EquipmentSlot slot) {
        double att = player.getAttributeBaseValue(entry);
        ItemAttributeModifiers modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (modifiers != null && !modifiers.modifiers().isEmpty()) {
            att = applyOperations(modifiers.modifiers(), entry, att, slot);
        }
        return att;
    }

    public static double getArmorValue(Player player, ItemStack stack, EquipmentSlot slot) {
        return getAttributeValue(Attributes.ARMOR, player, stack, slot);
    }

    public static double getArmorToughnessValue(Player player, ItemStack stack, EquipmentSlot slot) {
        return getAttributeValue(Attributes.ARMOR_TOUGHNESS, player, stack, slot);
    }

    public static double getAttackSpeed(Player player, ItemStack stack) {
        double speed = player.getAttributeBaseValue(Attributes.ATTACK_SPEED);
        ItemAttributeModifiers modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (modifiers != null && !modifiers.modifiers().isEmpty()) {
            speed = applyOperations(modifiers.modifiers(), Attributes.ATTACK_SPEED, speed, EquipmentSlot.MAINHAND);
        }
        return speed;
    }

    public static double applyOperations(
            List<ItemAttributeModifiers.Entry> modifiers,
            Holder<Attribute> entityAttribute,
            double base,
            EquipmentSlot slot) {
        double d = base;
        Iterator var6 = modifiers.iterator();

        while (var6.hasNext()) {
            ItemAttributeModifiers.Entry entry = (ItemAttributeModifiers.Entry) var6.next();
            if (entry.slot().test(slot) && Objects.equals(entityAttribute, entry.attribute())) {
                double e = entry.modifier().amount();
                double var10001;
                switch (entry.modifier().operation()) {
                    case ADD_VALUE -> var10001 = e;
                    case ADD_MULTIPLIED_BASE -> var10001 = e * base;
                    case ADD_MULTIPLIED_TOTAL -> var10001 = e * d;
                    default -> throw new MatchException((String) null, (Throwable) null);
                }

                d += var10001;
            }
        }
        return d;
    }

    public static double getEnchantmentBonus(Player player, Entity target, ItemStack stack) {
        ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);
        float bonus = 0.0F;
        if (enchantments != null && !enchantments.isEmpty()) {
            for (var entry : enchantments.entrySet()) {
                Holder<Enchantment> enchantment = entry.getKey();
                int level = entry.getIntValue();

                // 锋利 (Sharpness)
                if (enchantment.is(Enchantments.SHARPNESS)) {
                    bonus += 1.0f + (level - 1) * 0.5f;
                }
                // 亡灵杀手 (Smite)
                else if (enchantment.is(Enchantments.SMITE)
                        && target.getType().builtInRegistryHolder().is(EntityTypeTags.SENSITIVE_TO_SMITE)) {
                    bonus += 2.5f * level;
                }
                // 节肢杀手 (Bane of Arthropods)
                else if (enchantment.is(Enchantments.BANE_OF_ARTHROPODS)
                        && target.getType()
                                .builtInRegistryHolder()
                                .is(EntityTypeTags.SENSITIVE_TO_BANE_OF_ARTHROPODS)) {
                    bonus += 2.5f * level;
                }
                // 穿刺 (Impaling) —— 仅对三叉戟且目标为水生生物生效
                else if (enchantment.is(Enchantments.IMPALING)
                        && target.getType().builtInRegistryHolder().is(EntityTypeTags.SENSITIVE_TO_IMPALING)) {
                    bonus += 2.5f * level;
                }
            }
        }
        return bonus;
    }

    public static double getMaceAttackBonus(ItemStack stack, float height) {
        if (height <= 1.5) {
            // 原版在 shouldDealAdditionalDamage 中要求 >1.5 且不在滑翔
            // 此处仅返回 0，上层调用者应自行判断条件
            return 0.0;
        }
        double baseBonus;
        if (height <= 3.0) {
            baseBonus = 4.0 * height;
        } else if (height <= 8.0) {
            baseBonus = 12.0 + 2.0 * (height - 3.0);
        } else {
            baseBonus = 22.0 + (height - 8.0);
        }

        // 2. 密度附魔加成
        ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);
        if (enchantments != null) {
            int densityLevel = ItemStackUtils.getEnchantmentLevel(enchantments, Enchantments.DENSITY);
            baseBonus += densityLevel * 0.5 * height;
        }

        return baseBonus;
    }

    public static double getAttackDamage(Player player, Entity livingEntity, ItemStack stack) {
        double att = player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
        ItemAttributeModifiers modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (modifiers != null && !modifiers.modifiers().isEmpty()) {
            att = applyOperations(modifiers.modifiers(), Attributes.ATTACK_DAMAGE, att, EquipmentSlot.MAINHAND);
        }
        att += getEnchantmentBonus(player, livingEntity, stack);
        return att;
    }

    public static double getAttackDamage(
            Player player, LivingEntity livingEntity, ItemStack stack, float cooldownProgress) {
        return getAttackDamage(player, livingEntity, stack);
    }

    public static double getAttackDamage(LivingEntity livingEntity, ItemStack stack) {
        return getAttackDamage(mc.player, livingEntity, stack);
    }

    public static float getRealAttackDamage(Player player, Entity livingEntity, ItemStack stack, double fallDistance) {
        var attribute = AttributeUtils.getAttributeWith(player, Map.of(EquipmentSlot.MAINHAND, stack));
        float f = player.isAutoSpinAttack() ? 8.0F : (float) attribute.getValue(Attributes.ATTACK_DAMAGE);
        DamageSource damageSource = createDamageSource(player, player, stack);
        float g = player.getAttackStrengthScale(0.5F);
        float h = g * (getDamageAgainst(stack, f, damageSource) - f);
        f *= (0.2F + g * g * 0.8F);
        if (stack.getItem() instanceof MaceItem mace) {
            f += (float) getSmashDamageBonus(stack, damageSource, fallDistance);
        }
        if (canDealCritical(player, fallDistance)) {
            f *= 1.5F;
        }
        float i = f + h;
        return i;
    }

    private static float getDamageAgainst(ItemStack weapon, float baseDamage, DamageSource damageSource) {
        return EnchantmentUtils.calculate(
                weapon,
                ((current, enchantment, level) -> {
                    for (var ench : enchantment.value().getEffects(EnchantmentEffectComponents.DAMAGE)) {
                        if (EnchantmentUtils.matchPartialCondition(ench, (lootCondition -> {
                            if (lootCondition instanceof DamageSourceCondition damageSourcePredicate) {
                                return damageSourcePredicate.predicate().isEmpty()
                                        || matchDamageSource(
                                                damageSource,
                                                damageSourcePredicate
                                                        .predicate()
                                                        .get());
                            } else {
                                return true;
                            }
                        }))) {
                            current = ench.effect().process(level, randomSource, current);
                        }
                    }
                    return current;
                }),
                baseDamage);
    }

    public static float getMultipliedDamageByDifficulty(Level world, float amount) {
        if (world.getDifficulty() == Difficulty.PEACEFUL) {
            amount = 0.0F;
        }

        if (world.getDifficulty() == Difficulty.EASY) {
            amount = Math.min(amount / 2.0F + 1.0F, amount);
        }

        if (world.getDifficulty() == Difficulty.HARD) {
            amount = amount * 3.0F / 2.0F;
        }
        return amount;
    }

    public static DamageSource createDamageSource(
            ResourceKey<DamageType> type, @Nullable Entity source, @Nullable Entity attacker) {
        Holder<DamageType> re =
                RegistryUtils.getRegistryEntry(mc.getConnection().registryAccess(), type);
        re = re == null
                ? RegistryUtils.getRegistryEntry(mc.getConnection().registryAccess(), DamageTypes.PLAYER_ATTACK)
                : re;
        return new DamageSource(re, source, attacker);
    }

    public static DamageSource createDirectDamageSource(ResourceKey<DamageType> type, @Nullable Entity attacker) {
        return createDamageSource(type, attacker, attacker);
    }

    public static DamageSource createDirectDamageSource(Player attacker, ItemStack weapon) {
        return createDamageSource(attacker, attacker, weapon);
    }

    public static DamageSource createDamageSource(Entity source, Player attacker, ItemStack weapon) {
        DamageSource newSource;
        try {
            newSource = attacker.createAttackSource(weapon);
        } catch (Throwable e) {
            newSource = createDamageSource(DamageTypes.PLAYER_ATTACK, source, attacker);
        }
        return newSource;
    }

    public static boolean canDealCritical(LivingEntity attacker, double fallDistance) {
        return fallDistance > 0.0
                && !attacker.onGround()
                && !attacker.onClimbable()
                && !attacker.isInWater()
                && !attacker.hasEffect(MobEffects.BLINDNESS)
                && !attacker.isPassenger()
                && !attacker.isSprinting();
    }

    public static double getSmashDamageBonus(ItemStack stack, DamageSource source, double f) {
        double g;
        if (f <= 3.0) {
            g = 4.0 * f;
        } else if (f <= 8.0) {
            g = 12.0 + 2.0 * (f - 3.0);
        } else {
            g = 22.0 + f - 8.0;
        }
        return g
                + EnchantmentUtils.calculate(
                                stack,
                                ((current, enchantment, level) -> {
                                    for (var re : enchantment
                                            .value()
                                            .getEffects(EnchantmentEffectComponents.SMASH_DAMAGE_PER_FALLEN_BLOCK)) {
                                        current = re.effect().process(level, randomSource, current);
                                    }
                                    return current;
                                }),
                                0.0F)
                        * f;
    }

    public static final int STAGE_IS_INVULNERABLE = 0;
    public static final int STAGE_MULTIPLY_DIFFICULTY = 1;
    public static final int STAGE_CALCULATE_HURT_TIME = 2;
    public static final int STAGE_APPLY_ARMOR = 3;
    public static final int STAGE_APPLY_PROTECTION = 4;
    public static final int STAGE_APPLY_DAMAGE_AND_ABSORPTION = 5;

    public static boolean isPlayerInvulnerableTo(DamageSource source) {
        return false;
    }

    public static float getFinalDamage(Player player, float rawDamage, DamageSource damageSource) {
        DamageContext context = fromPlayer(player).build();
        rawDamage = getDamageAfterDifficulty(rawDamage, damageSource, context);
        if (rawDamage <= 0.0F) {
            return 0.0F;
        }
        rawDamage = getDamageAfterHurtTime(rawDamage, damageSource, context);
        if (rawDamage <= 0.0F) {
            return 0.0F;
        }
        rawDamage = DamageUtils.getDamageAfterArmorReduce(rawDamage, damageSource, context);
        if (rawDamage <= 0.0F) {
            return 0.0F;
        }
        rawDamage = DamageUtils.getDamageAfterEffectAndProtection(rawDamage, damageSource, context);
        if (rawDamage <= 0.0F) {
            return 0.0F;
        }
        return rawDamage;
    }

    public static float getDamageAfterDifficulty(float currentVal, DamageSource source, DamageContext context) {
        if (source.scalesWithDifficulty()) {
            return getMultipliedDamageByDifficulty(context.world, currentVal);
        }
        return currentVal;
    }

    public static float getDamageAfterHurtTime(float currentVal, DamageSource source, DamageContext context) {
        return currentVal;
    }

    public static float getDamageAfterArmorReduce(float currentVal, DamageSource source, DamageContext context) {
        if (!source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            currentVal = getDamageLeftAfterArmor(currentVal, source, context.armor, context.armorToughness);
        }

        return currentVal;
    }

    public static float getDamageAfterEffectAndProtection(
            float currentVal, DamageSource source, DamageContext context) {
        if (source.is(DamageTypeTags.BYPASSES_EFFECTS)) {
            return currentVal;
        } else {
            if (context.statusEffects.containsKey(MobEffects.RESISTANCE)
                    && !source.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
                int i = (context.statusEffects.get(MobEffects.RESISTANCE) + 1) * 5;
                int j = 25 - i;
                float f = currentVal * (float) j;
                float g = currentVal;
                currentVal = Math.max(f / 25.0F, 0.0F);
            }

            if (currentVal <= 0.0F) {
                return 0.0F;
            } else if (source.is(DamageTypeTags.BYPASSES_ENCHANTMENTS)) {
                return currentVal;
            } else {
                float k =
                        getProtectionAmount(context.armorSlots, EnchantmentEffectComponents.DAMAGE_PROTECTION, source);

                if (k > 0.0F) {
                    currentVal = CombatRules.getDamageAfterMagicAbsorb(currentVal, k);
                }

                return currentVal;
            }
        }
    }

    public static void damageOrAbsorption(LivingEntity livingEntity, DamageSource source, float amount) {
        float f = amount;
        amount = Math.max(amount - livingEntity.getAbsorptionAmount(), 0.0F);
        livingEntity.setAbsorptionAmount(livingEntity.getAbsorptionAmount() - (f - amount));

        if (amount != 0.0F) {
            livingEntity.getCombatTracker().recordDamage(source, amount);
            livingEntity.setHealth(livingEntity.getHealth() - amount);
            livingEntity.setAbsorptionAmount(livingEntity.getAbsorptionAmount() - amount);
        }
    }

    private static float getDamageLeftAfterArmor(
            float damageAmount, DamageSource damageSource, float armor, float armorToughness) {
        float i;
        label12:
        {
            float f = 2.0F + armorToughness / 4.0F;
            float g = Mth.clamp(armor - damageAmount / f, armor * 0.2F, 20.0F);
            float h = g / 25.0F;
            ItemStack itemStack = damageSource.getWeaponItem();
            if (itemStack != null) {

                i = Mth.clamp(
                        getConditionalMultiplierByWeaponEnchantment(
                                itemStack, EnchantmentEffectComponents.ARMOR_EFFECTIVENESS, damageSource, h),
                        0.0F,
                        1.0F);
                break label12;
            }

            i = h;
        }

        float j = 1.0F - i;
        return damageAmount * j;
    }

    private static final RandomSource randomSource = new SingleThreadedRandomSource(1145141919);

    private static float getConditionalMultiplierByWeaponEnchantment(
            ItemStack stack,
            DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> listComponentType,
            DamageSource source,
            float h) {
        return EnchantmentUtils.calculate(
                stack,
                (v, en, i) -> {
                    Enchantment ench = en.value();
                    for (var re : ench.getEffects(listComponentType)) {
                        if (EnchantmentUtils.matchPartialCondition(re, (loot) -> {
                            if (loot instanceof DamageSourceCondition damage) {
                                return damage.predicate().isEmpty()
                                        || matchDamageSource(
                                                source, damage.predicate().get());
                            } else {
                                return true;
                            }
                        })) {
                            v = re.effect().process(i, randomSource, v);
                        }
                    }
                    return v;
                },
                h);
    }

    private static float getProtectionAmount(
            Map<EquipmentSlot, ItemStack> equipments,
            DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> listComponentType,
            DamageSource source) {
        return EnchantmentUtils.calculate(
                equipments,
                (current, enchantment, level, stack, slot) -> {
                    for (var re : enchantment.value().getEffects(listComponentType)) {
                        if (EnchantmentUtils.matchPartialCondition(re, (loot -> {
                            if (loot instanceof DamageSourceCondition damage) {
                                return damage.predicate().isEmpty()
                                        || matchDamageSource(
                                                source, damage.predicate().get());
                            } else {
                                return true;
                            }
                        }))) {
                            current = re.effect().process(level, randomSource, current);
                        }
                    }
                    return current;
                },
                0.0F);
    }

    public static boolean matchDamageSource(DamageSource source, DamageSourcePredicate predicate) {
        for (var re : predicate.tags()) {
            if (!re.matches(source.typeHolder())) {
                return false;
            }
        }
        if (predicate.directEntity().isPresent()
                && !matchEntityTypes(
                        source.getDirectEntity(), predicate.directEntity().get())) {
            return false;
        }
        if (predicate.sourceEntity().isPresent()
                && !matchEntityTypes(
                        source.getEntity(), predicate.sourceEntity().get())) {
            return false;
        }
        if (predicate.isDirect().isPresent() && predicate.isDirect().get() != source.isDirect()) {
            return false;
        }
        return true;
    }

    private static boolean matchEntityTypes(Entity entity, EntityPredicate predicate) {
        if (entity == null) {
            return false;
        }
        // 26.1.2 的 EntityPredicate 是 record，直接暴露 entityType()。
        // （26.2 改成 final class + parts map，那里才需要遍历 parts 取 EntityTypePredicate。）
        if (predicate.entityType().isPresent()
                && !predicate.entityType().get().matches(entity.getType().builtInRegistryHolder())) {
            return false;
        }
        return true;
    }

    public static DamageContext.Builder fromPlayer(Player player) {
        DamageContext.Builder builder = new DamageContext.Builder();
        builder.withArmor((float) player.getAttributes().getValue(Attributes.ARMOR));
        builder.withArmorToughness((float) player.getAttributes().getValue(Attributes.ARMOR_TOUGHNESS));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            builder.withArmorSlot(slot, stack);
        }
        for (var re : player.getActiveEffects()) {
            builder.withStatusEffect(re);
        }
        return builder;
    }

    public static final class DamageContext {
        private final Level world;
        private final float armor;
        private final float armorToughness;
        private final Map<EquipmentSlot, ItemStack> armorSlots;
        private final Map<Holder<net.minecraft.world.effect.MobEffect>, Integer> statusEffects;

        private DamageContext(DamageContext.Builder builder) {
            this.world = builder.world;
            this.armor = builder.armor;
            this.armorToughness = builder.armorToughness;
            this.armorSlots = Map.copyOf(builder.armorSlots);
            this.statusEffects = Map.copyOf(builder.statusEffects);
        }

        public static final class Builder {
            private Level world;
            private float armor;
            private float armorToughness;
            private final Map<EquipmentSlot, ItemStack> armorSlots = new HashMap<>();
            private final Map<Holder<net.minecraft.world.effect.MobEffect>, Integer> statusEffects = new HashMap<>();

            private Builder() {
                this.world = Minecraft.getInstance().level;
            }

            private Builder(DamageContext context) {
                this.world = context.world;
                this.armor = context.armor;
                this.armorToughness = context.armorToughness;
                this.armorSlots.putAll(context.armorSlots);
                this.statusEffects.putAll(context.statusEffects);
            }

            public Builder withWorld(Level world) {
                this.world = world;
                return this;
            }

            public Builder withArmor(float armor) {
                this.armor = armor;
                return this;
            }

            public Builder withArmorToughness(float armorToughness) {
                this.armorToughness = armorToughness;
                return this;
            }

            public Builder withArmorSlots(Map<EquipmentSlot, ItemStack> armorSlots) {
                this.armorSlots.putAll(armorSlots);
                return this;
            }

            public Builder withArmorSlot(EquipmentSlot slot, ItemStack stack) {
                this.armorSlots.put(slot, stack);
                return this;
            }

            public Builder withStatusEffect(Holder<net.minecraft.world.effect.MobEffect> effect, int amplifier) {
                if (effect != null && amplifier >= 0) {
                    this.statusEffects.merge(effect, amplifier, Math::max);
                }
                return this;
            }

            public Builder withPotionEffect(Holder<net.minecraft.world.effect.MobEffect> effect, int amplifier) {
                return withStatusEffect(effect, amplifier);
            }

            public Builder withStatusEffect(MobEffectInstance statusEffect) {
                return withStatusEffect(statusEffect.getEffect(), statusEffect.getAmplifier());
            }

            public DamageContext build() {
                return new DamageContext(this);
            }
        }
    }
}
