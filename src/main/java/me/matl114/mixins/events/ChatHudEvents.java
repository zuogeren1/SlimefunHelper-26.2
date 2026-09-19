package me.matl114.mixins.events;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import me.matl114.accessors.events.ChatHudAccess;
import me.matl114.accessors.events.ChatHudLineAccess;
import me.matl114.events.Event;
import me.matl114.events.Listener;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(ChatComponent.class)
public abstract class ChatHudEvents implements ChatHudAccess {
    @Shadow
    @Final
    private List<GuiMessage.Line> trimmedMessages;

    @Shadow
    @Final
    private List<GuiMessage> allMessages;

    @Unique
    public String uniqueId;

    @Unique
    @Override
    public void setUniqueMessageId(String id) {
        this.uniqueId = id;
    }

    @Unique
    @Override
    public ArrayList<GuiMessage.Line> getVisibleLines() {
        return (ArrayList<GuiMessage.Line>) this.trimmedMessages;
    }

    @Inject(
            method =
                    "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void onMessageAdd(
            Component message,
            MessageSignature signatureData,
            GuiMessageTag indicator,
            CallbackInfo ci,
            @Local(argsOnly = true) LocalRef<Component> textLocalRef) {
        if (!Listener.getMessageAddToHud().isEmpty()) {
            Event<Component> addMessageEvent = new Event<>(message, true, true, signatureData, indicator);
            Listener.getMessageAddToHud().handleValue(addMessageEvent);
            if (addMessageEvent.isCancelled()) {
                ci.cancel();
            }
            textLocalRef.set(addMessageEvent.context());
        }
    }

    @Inject(
            method =
                    "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/gui/components/ChatComponent;logChatMessage(Lnet/minecraft/client/multiplayer/chat/GuiMessage;)V",
                            shift = At.Shift.AFTER))
    private void onChatHudLineCreate(
            Component message,
            MessageSignature signatureData,
            GuiMessageTag indicator,
            CallbackInfo ci,
            @Local GuiMessage line) {
        ChatHudLineAccess.of(line).setUniqueMessageId(uniqueId);
    }

    @Unique
    @Override
    public void clearUniqueMessages(String id) {
        this.trimmedMessages.removeIf(
                s -> Objects.equals(ChatHudLineAccess.of(s).getUniqueMessageId(), id));
        this.allMessages.removeIf(s -> Objects.equals(ChatHudLineAccess.of(s).getUniqueMessageId(), id));
    }

    @Inject(method = "addMessageToDisplayQueue", at = @At("HEAD"), cancellable = true)
    private void onVisibleMessageAdd(
            GuiMessage message, CallbackInfo ci, @Local(argsOnly = true) LocalRef<GuiMessage> lineLocalRef) {
        if (!Listener.getMessageAddToVisible().isEmpty()) {
            Event<GuiMessage> addMessageEvent = new Event<>(message, true, true);
            Listener.getMessageAddToVisible().handleValue(addMessageEvent);
            if (addMessageEvent.isCancelled()) {
                ci.cancel();
                return;
            }
            lineLocalRef.set(addMessageEvent.context());
        }
    }

    @ModifyExpressionValue(
            method = "addMessageToDisplayQueue",
            at =
                    @At(
                            value = "NEW",
                            target =
                                    "(ILnet/minecraft/util/FormattedCharSequence;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;Z)Lnet/minecraft/client/multiplayer/chat/GuiMessage$Line;"))
    private GuiMessage.Line onVisibleLineCreate(
            GuiMessage.Line original, @Local(argsOnly = true) GuiMessage line) {
        String unique = ChatHudLineAccess.of(line).getUniqueMessageId();
        if (unique != null) {
            ChatHudLineAccess.of(original).setUniqueMessageId(unique);
        }
        return original;
    }
}
