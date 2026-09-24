package me.matl114.hacks.modules.render;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import me.matl114.events.Event;
import me.matl114.events.impl.Render2D;
import me.matl114.events.RenderListener;
import me.matl114.gui.Constants;
import me.matl114.hacks.api.ModulePath;
import me.matl114.hacks.utils.render.ItemStackDisplayUtils;
import me.matl114.managers.Configs;
import me.matl114.managers.config.DoubleRef;
import me.matl114.managers.config.FlagRef;
import me.matl114.utils.*;
import me.matl114.versioned.api.VDrawContext;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector2d;

public class NameTag extends INameTag {
    public NameTag() {
        super("NameTag");
    }

    protected ModulePath createRoot() {
        return makePath(Configs.RENDER_CONFIG, "player-info.name-tag");
    }

    protected void initializeModuleSettings() {
        height = doubleBuilder(nameTag.add("player-extra-height"))
                .defaultValue(1.0D)
                .build();
        size = doubleBuilder(nameTag.add("player-size")).defaultValue(1.0D).build();
        hideName = flagBuilder(nameTag.add("hide-vanilla")).build();
    }

    //
    //    public final FlagRef enablePlayer =
    //            flagBuilder(nameTag.add("show-player-head")).build();
    //
    //    public final FlagRef enableList =
    //            flagBuilder(nameTag.add("show-player-list")).build();

    public DoubleRef height;

    public DoubleRef size;

    public FlagRef hideName;

    public void onRender(Event<Render2D> event) {
        if (checkNull()) {
            return;
        }
        if (enable.get() && !event.context.hudHidden() && nameTagInfos != null) {
            var stack = event.context.drawContext();
            Matrix4f cam = RenderListener.getWorldModelViewMatrix();
            Matrix4f proj = RenderListener.getWorldBasicProjectionMatrix();
            Function<Vec3, Vector2d> projector = RenderUtils.createProjector(cam, proj);
            for (var entity : nameTagInfos) {
                if (entity.player != mc.getCameraEntity()) {
                    onRenderPlayer(entity, stack, projector, event.context.partialTicks());
                }
            }
        }
    }

    public void onRenderPlayer(
            PlayerNameTagInfo player, VDrawContext vdraw, Function<Vec3, Vector2d> projector, float tick) {
        Vec3 pos = player.player.getPosition(tick).add(0, player.player.getBbHeight() + 0.5, 0);
        Vector2d screenPos = projector.apply(pos);
        if (screenPos != null) {
            vdraw.pushMatrix();
            try {
                vdraw.getMatrices().translate((float) screenPos.x, (float) screenPos.y);
                handleSize(vdraw);
                handleNameLinePlayer(vdraw, player);
                handleEquipmentPlayer(vdraw, player);
                handleOtherInfoLine(vdraw, player);
                handleEffectDisplayPlayer(vdraw, player);
                //               if(screenPos.x == 0F && screenPos.y == 0F){
                //                    vdraw.getMatrices().translate((float) screenPos.x + 1.0F, (float) screenPos.y);
                //                }else{
                //                  // Debug.chat("Print", screenPos);
                //                    vdraw.getMatrices().translate((float) screenPos.x + 1.0F, (float) screenPos.y);
                //               }
                // vdraw.drawText(mc.textRenderer, player.getNameForScoreboard(), 0,0, -1, true);
            } finally {
                vdraw.popMatrix();
            }
        }
    }

    public void handleSize(VDrawContext vdraw) {
        vdraw.getMatrices().translate(0, -(float) height.get());
        vdraw.getMatrices().scale((float) size.get(), (float) size.get());
    }

    public void handleNameLinePlayer(VDrawContext vdraw, PlayerNameTagInfo player) {
        if (player.nameDisplay != null) {
            float length = player.nameLength;
            float lengthHalf = length / 2.0F;
            vdraw.getMatrices().translate(0, -HEIGHT);
            vdraw.drawText(mc.font, player.nameDisplay.getVisualOrderText(), (int) -lengthHalf, 0, -1, true);
        }
    }

    public void handleEquipmentPlayer(VDrawContext vdraw, PlayerNameTagInfo player) {
        if (player.equipments != null) {
            ItemStack[] stacks = player.equipments;
            int startX = -stacks.length * 9;
            boolean hasDurability = false;
            vdraw.getMatrices().pushMatrix();
            vdraw.getMatrices().scale(0.75F, 0.75F);
            for (var i = 0; i < stacks.length; ++i) {
                if (stacks[i].isEmpty()) {
                    EquipmentSlot slot = SLOTS[i];
                    vdraw.drawGuiTexture(Constants.EMPTY_SLOT_TO_SPRITE.get(slot), startX + i * 18, -17, 16, 16);
                } else {
                    vdraw.drawItem(stacks[i], startX + i * 18, -17, 999, 0);
                    ItemStack stackOverride = stacks[i];
                    vdraw.drawItemInSlot(mc.font, stackOverride, startX + i * 18, -17, null);
                    if (equipmentPercentage.get().isNotIn(ItemStackDisplayUtils.DamageDisplay.NONE)
                            && stackOverride.getCount() == 1
                            && stackOverride.isDamageableItem()) {
                        hasDurability = true;
                        FormattedCharSequence dur = ItemStackDisplayUtils.getDamageShowText(
                                        stackOverride, equipmentPercentage.get())
                                .getVisualOrderText();
                        float width = mc.font.getSplitter().stringWidth(dur);
                        int color = ItemStackDisplayUtils.getDamageDisplayColor(stackOverride);
                        vdraw.drawText(
                                mc.font,
                                dur,
                                startX + i * 18 + 9 + (int) ((-width - 1) / 2),
                                -16 - (int) HEIGHT,
                                color,
                                true);
                    }
                }
            }
            vdraw.getMatrices().popMatrix();
            vdraw.getMatrices().translate(0, hasDurability ? -HEIGHT * 2.0F : -HEIGHT * 1.5F);
        }
    }

    public void handleOtherInfoLine(VDrawContext vdraw, PlayerNameTagInfo player) {
        if (player.otherInfoDisplay != null) {
            float length = player.otherInfoLength;
            float lengthHalf = length / 2.0F;
            vdraw.getMatrices().translate(0, -HEIGHT * 0.75F);
            vdraw.getMatrices().pushMatrix();
            vdraw.getMatrices().scale(0.75F, 0.75F);
            vdraw.drawText(mc.font, player.otherInfoDisplay.getVisualOrderText(), (int) -lengthHalf, 0, -1, true);
            vdraw.popMatrix();
        }
    }

    public void handleEffectDisplayPlayer(VDrawContext vdraw, PlayerNameTagInfo player) {
        if (player.visibleEffects != null) {
            List<Map.Entry<Holder<MobEffect>, Component>> line =
                    player.visibleEffects.entrySet().stream().toList();
            int size = line.size();
            for (var i = 0; i < size; i += 3) {
                int endI = Math.min(i + 3, size);
                if (endI == i) continue;
                float width = (endI - i - 1);
                for (var j = i; j < endI; j++) {
                    width += 9;
                    width += mc.font.getSplitter().stringWidth(line.get(j).getValue());
                }
                vdraw.getMatrices().translate(0, -HEIGHT);
                vdraw.getMatrices().pushMatrix();
                {
                    vdraw.getMatrices().translate(-width / 2, 0);
                    for (var j = i; j < endI; j++) {
                        var entry = line.get(j);
                        vdraw.getMatrices().pushMatrix();
                        {
                            vdraw.getMatrices().scale(0.5F, 0.5F);
                            statusEffectRenderer.render(
                                    1, 1, vdraw, entry.getKey().value());
                        }
                        vdraw.getMatrices().popMatrix();
                        vdraw.getMatrices().translate(9, 0);
                        vdraw.drawText(
                                mc.font,
                                entry.getValue().getVisualOrderText(),
                                0,
                                0,
                                potionColor.get().withAlpha(255),
                                true);
                        vdraw.getMatrices()
                                .translate(
                                        mc.font
                                                        .getSplitter()
                                                        .stringWidth(line.get(j).getValue())
                                                + 1,
                                        0);
                    }
                }
                vdraw.getMatrices().popMatrix();
            }
        }
    }
}
