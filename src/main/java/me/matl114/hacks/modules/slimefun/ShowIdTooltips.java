package me.matl114.hacks.modules.slimefun;

import static me.matl114.utils.ItemStackUtils.*;

import java.util.List;
import me.matl114.events.Event;
import me.matl114.events.RenderListener;
import me.matl114.hacks.api.BaseModule;
import me.matl114.hacks.api.ModulePath;
import me.matl114.managers.Configs;
import me.matl114.managers.config.FlagRef;
import me.matl114.utils.ItemStackUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

public class ShowIdTooltips extends BaseModule {
    public final ModulePath slimefunSettings = makePath(Configs.SLIMEFUN_CONFIG, "slimefun-settings");

    public ShowIdTooltips() {
        super("ShowIdTooltips");
        bindFlag(enable);
    }

    public final FlagRef enable = builder(slimefunSettings.add("enable-tooltips-display"), Boolean.class)
            .defaultValue(true)
            .build();

    @Override
    public void registerAll() {
        super.registerAll();
        registerListener(RenderListener.getTooltipShow(), this::onTooltips);
    }

    public void onTooltips(Event<List<Component>> event) {
        if (isActive()) {
            ItemStack stack = event.getArgs(0);

            final String id = ItemStackUtils.getSfId(stack);
            if (id == null) {
                return;
            }
            final List<Component> lore = event.context();
            // final Identifier identifier = Registries.ITEM.getId(this.getItem());
            boolean found = false;
            for (int i = 0; i < lore.size(); i++) {
                String line = lore.get(i).getString();
                if (("§9§oMinecraft").equals(line)) {
                    lore.set(i, SLIMEFUN_MODID);
                    found = true;
                }
            }
            if (!found) {
                lore.add(SLIMEFUN_MODID);
            }
            lore.add(Component.literal("粘液物品ID: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(id).withStyle(ChatFormatting.GREEN)));
            handleGCEInfo(id, stack, lore);
            handleCLTInfo(id, stack, lore);
        }
    }

    protected static final Component SLIMEFUN_MODID = Component.literal("Slimefun").withStyle(ChatFormatting.BLUE);

    protected static String GCE_CHICKEN_PATH = "geneticchickengineering:gce_pocket_chicken_dna";
    protected static char[] GCE_GENE_DISPLAY_L = new char[] {'b', 'c', 'd', 'f', 's', 'w'};
    protected static char[] GCE_GENE_DISPLAY_U = new char[] {'B', 'C', 'D', 'F', 'S', 'W'};

    public static void handleGCEInfo(String sfid, ItemStack stack, List<Component> lores) {
        if (sfid.startsWith("GCE_")) {
            if (stack != null && ItemStackUtils.hasCustomData(stack)) {
                try {
                    CompoundTag tag = getCustomDataReadOnly(stack);
                    if ((tag = getBukkitValue(tag)) != null
                            && tag.get(GCE_CHICKEN_PATH) instanceof IntArrayTag intArray) {
                        int[] dna = intArray.getAsIntArray();
                        int len = dna.length;
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < 6; i++) {
                            if (len > i && dna[i] == 0 || dna[i] == 1 || dna[i] == 3) {
                                sb.append(dna[i] % 2 == 0 ? GCE_GENE_DISPLAY_L[i] : GCE_GENE_DISPLAY_U[i])
                                        .append(dna[i] / 2 == 0 ? GCE_GENE_DISPLAY_L[i] : GCE_GENE_DISPLAY_U[i]);
                            } else {
                                sb.append("??");
                            }
                        }
                        lores.add(Component.literal("基因工程: ")
                                .withStyle(ChatFormatting.GRAY)
                                .append(Component.literal(sb.toString()).withStyle(ChatFormatting.DARK_PURPLE)));
                    }
                } catch (Throwable e) {
                }
            }
        }
    }

    protected static final String CLT_SEED_PATH = "cultivation:seed_instance";
    protected static final String CLT_SEED_DROP_PATH = "cultivation:drop_rate";
    protected static final String CLT_SEED_GROWTH_PATH = "cultivation:growth_speed";
    protected static final String CLT_SEED_STRENGTH_PATH = "cultivation:strength";

    public static void handleCLTInfo(String sfid, ItemStack stack, List<Component> lores) {
        if (sfid.startsWith("CLT_PLANT")) {
            if (stack != null && ItemStackUtils.hasCustomData(stack)) {
                MutableComponent info = Component.literal("农耕工艺: [").withStyle(ChatFormatting.GRAY);
                CompoundTag tag = getBukkitValueReadOnly(stack);
                if (tag != null) {
                    try {
                        if (tag.contains(CLT_SEED_PATH)) {
                            if (tag.get(CLT_SEED_PATH) instanceof CompoundTag nbt) {

                                int level = nbt.get(CLT_SEED_DROP_PATH) instanceof IntTag it ? it.intValue() : 0;
                                int speed = nbt.get(CLT_SEED_GROWTH_PATH) instanceof IntTag it ? it.intValue() : 0;
                                int strength = nbt.get(CLT_SEED_STRENGTH_PATH) instanceof IntTag it ? it.intValue() : 0;
                                info.append(Component.literal("等级: ").withStyle(ChatFormatting.YELLOW));
                                info.append(Component.literal(String.valueOf(level)).withStyle(ChatFormatting.GRAY));
                                info.append(Component.literal(" 速率: ").withStyle(ChatFormatting.YELLOW));
                                info.append(Component.literal(String.valueOf(speed)).withStyle(ChatFormatting.GRAY));
                                info.append(Component.literal(" 强度: ").withStyle(ChatFormatting.YELLOW));
                                info.append(
                                        Component.literal(String.valueOf(strength)).withStyle(ChatFormatting.GRAY));
                            }

                        } else {
                            info.append(Component.literal("未初始化属性").withStyle(ChatFormatting.RED));
                        }
                    } catch (Throwable e) {
                        info.append(Component.literal("数据错误").withStyle(ChatFormatting.RED));
                    }
                }
                info.append(Component.literal("]").withStyle(ChatFormatting.GRAY));
                lores.add(info);
            }
        }
    }
}
