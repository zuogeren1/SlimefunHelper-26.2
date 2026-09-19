package me.matl114.hacks.modules.survival;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import me.matl114.accessors.access.ChunkAccess;
import me.matl114.events.Event;
import me.matl114.events.Listener;
import me.matl114.events.RenderListener;
import me.matl114.hacks.api.BaseModule;
import me.matl114.hacks.api.ModulePath;
import me.matl114.hacks.utils.config.WrapColor;
import me.matl114.hacks.utils.render.RenderCollectors;
import me.matl114.hacks.utils.render.RenderElements;
import me.matl114.hacks.utils.tasks.TimerExecutor;
import me.matl114.managers.Configs;
import me.matl114.managers.config.DoubleRef;
import me.matl114.managers.config.FlagRef;
import me.matl114.managers.config.KeyBindRef;
import me.matl114.managers.config.NBTRef;
import me.matl114.managers.input.MultiKeyBind;
import me.matl114.utils.CommonUtils;
import me.matl114.utils.RenderUtils;
import me.matl114.utils.render.RenderCollector;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TrialSpawnerBlock;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerState;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultSharedData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class TrialInfoESP extends BaseModule {
    public TrialInfoESP() {
        super("TrialESP");
    }

    public final ModulePath root = makePath(Configs.SURVIVAL_CONFIG, "render-utils.trial-esp");

    public final FlagRef enable = flagBuilder(root.addEnable()).build();

    public final KeyBindRef hotkey =
            toggleHotkey(root.addHotkey(), new MultiKeyBind(), root.addEnable()).build();

    public final DoubleRef textScale = doubleBuilder(root.add("text-scale"))
            .defaultValue(0.75D)
            .validator(Configs.doubleRange(0.1D, 4.0D))
            .build();

    public final NBTRef<WrapColor> color = builder(root.add("color"), WrapColor.class)
            .defaultValue(new WrapColor(ChatFormatting.AQUA))
            .build();

    @Override
    public void registerAll() {
        super.registerAll();
        registerListener(Listener.getPostGameTick(), this::onTick);
        registerListener(RenderListener.getRender3DEvent(), this::onRender3D);
        registerListener(Listener.getWorldSwitchPoint(), this::onSwitchWorld);
    }

    public RenderCollector<RenderElements.Text> textCollector = RenderCollectors.createTextCollector();
    private Map<ChunkPos, Set<BlockPos>> cachedVaultsAndTrials = new HashMap<>();
    TimerExecutor cacheClearTimer = new TimerExecutor();

    public void onTick(Event<LocalPlayer> event) {
        if (cacheClearTimer.run(1000)) {
            cachedVaultsAndTrials = new HashMap<>();
        }
        if (enable.get()) {
            for (var chunk : CommonUtils.chunks(false)) {
                ChunkPos pos = chunk.getPos();
                if (!cachedVaultsAndTrials.containsKey(pos)) {
                    Set<BlockPos> sets = new HashSet<>();
                    for (var blockEntities : ChunkAccess.of(chunk).blockEntityEntries()) {
                        var bt = blockEntities.getValue().getType();
                        if (bt == BlockEntityTypes.VAULT || bt == BlockEntityTypes.TRIAL_SPAWNER) {
                            sets.add(blockEntities.getKey());
                        }
                    }
                    cachedVaultsAndTrials.put(pos, sets);
                }
            }
            for (var re : cachedVaultsAndTrials.values()) {
                for (var bp : re) {
                    BlockEntity be = mc.level.getBlockEntity(bp);
                    List<Component> textLines = new ArrayList<>();
                    if (be instanceof TrialSpawnerBlockEntity be1) {
                        BlockState currentState = mc.level.getBlockState(bp);
                        TrialSpawnerState state = currentState.getValue(TrialSpawnerBlock.STATE);
                        if (state == TrialSpawnerState.WAITING_FOR_PLAYERS) {
                            textLines.add(Component.translatable("message.module.trial-info-esp.display.trial-ready"));
                        } else if (state == TrialSpawnerState.COOLDOWN) {
                            OptionalLong cooldownLong = WorldManager.INSTANCE.getTrialSpawnerCooldownStartTime(be1);
                            String time;
                            if (cooldownLong.isPresent()) {
                                long cooldownTime = System.currentTimeMillis() - cooldownLong.getAsLong();
                                long totalSeconds = cooldownTime / 1000;
                                long minutes = totalSeconds / 60; // 总分钟数（不向小时进位）
                                long seconds = totalSeconds % 60; // 剩余的秒数
                                time = minutes + "m" + seconds + "s";
                            } else {
                                time = "?";
                            }
                            textLines.add(
                                    Component.translatable("message.module.trial-info-esp.display.trial-cooldown", time));
                        } else if (state != TrialSpawnerState.INACTIVE) {
                            OptionalLong activeLong = WorldManager.INSTANCE.getTrialSpawnerActiveStartTime(be1);
                            String time;
                            if (activeLong.isPresent()) {
                                long activeTime = System.currentTimeMillis() - activeLong.getAsLong();
                                long totalSeconds = activeTime / 1000;
                                long minutes = totalSeconds / 60;
                                long seconds = totalSeconds % 60;
                                time = minutes + "m" + seconds + "s";
                            } else {
                                time = "?";
                            }
                            textLines.add(
                                    Component.translatable("message.module.trial-info-esp.display.trial-active", time));
                        }
                        var entity = be1.getTrialSpawner().getStateData().getOrCreateDisplayEntity(be1.getTrialSpawner(), mc.level, state);
                        if (entity != null) {
                            textLines.add(Component.translatable(
                                    "message.module.trial-info-esp.display.trial-type",
                                    entity.getType().getDescription()));
                        }
                    } else if (be instanceof VaultBlockEntity be2) {
                        BlockState currentState = mc.level.getBlockState(bp);
                        boolean omin = currentState.getValue(VaultBlock.OMINOUS);
                        textLines.add(
                                omin
                                        ? Component.translatable("message.module.trial-info.esp.display.vault-type.ominous")
                                        : Component.translatable("message.module.trial-info.esp.display.vault-type.common"));
                        VaultSharedData sharedData = be2.getSharedData();
                        var set = sharedData.getConnectedPlayers();
                        if (!set.contains(mc.player.getUUID())) {
                            textLines.add(Component.translatable("message.module.trial-info-esp.display.vault-can-open"));
                        } else {
                            textLines.add(
                                    Component.translatable("message.module.trial-info-esp.display.vault-can-not-open"));
                        }
                        if (!set.isEmpty()) {
                            textLines.add(Component.translatable(
                                    "message.module.trial-info-esp.display.vault-opened-times", set.size()));
                        }
                    }
                    if (!textLines.isEmpty()) {
                        MutableComponent result = Component.empty().append(textLines.get(0));
                        for (int i = 1; i < textLines.size(); i++) {
                            result = result.append(Component.literal("\n")).append(textLines.get(i));
                        }
                        Vec3 textPos = Vec3.atCenterOf(bp).add(0.0D, 0.4, 0.0D);
                        textCollector.submit(
                                new RenderElements.Text(result, textPos, (float) textScale.get()),
                                color.get().withAlpha(255));
                    }
                }
            }
        }
    }

    public void onRender3D(Event<PoseStack> event) {
        if (!enable.get()) {
            return;
        }
        RenderUtils.startDrawVirtual(event.context());
        try {
            textCollector.render3D(event.context());
        } finally {
            RenderUtils.stopDrawVirtual(event.context());
        }
    }

    private void onSwitchWorld(Event<Level> event) {
        cachedVaultsAndTrials.clear();
    }
}
