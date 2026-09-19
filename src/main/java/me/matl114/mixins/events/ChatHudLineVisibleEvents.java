package me.matl114.mixins.events;

import me.matl114.accessors.events.ChatHudLineAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Environment(EnvType.CLIENT)
@Mixin(GuiMessage.Line.class)
public abstract class ChatHudLineVisibleEvents implements ChatHudLineAccess {
    @Unique
    private String uniqueMessageId;

    @Unique
    @Override
    public void setUniqueMessageId(String uniqueMessageId) {
        this.uniqueMessageId = uniqueMessageId;
    }

    @Unique
    @Override
    public String getUniqueMessageId() {
        return this.uniqueMessageId;
    }
}
