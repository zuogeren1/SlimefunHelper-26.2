package me.matl114.mixins.events;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.matl114.events.Event;
import me.matl114.events.Listener;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(Level.class)
public abstract class WorldEvents {
    @Shadow
    @Final
    private boolean isClientSide;

    @WrapOperation(
            method = "tickBlockEntities",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/TickingBlockEntity;tick()V"))
    public void shouldTickBlockEntities(TickingBlockEntity instance, Operation<Void> original) {
        if (isClientSide) {
            Event<TickingBlockEntity> event = new Event<>(instance, true, false);
            Listener.getBlockEntityTickListener().handleValue(event);
            if (!event.isCancelled()) {
                try {
                    original.call(instance);
                } catch (Throwable e) {
                    if (Listener.handleException(e, Listener.ExceptionType.BLOCK_ENTITY_TICK, instance, this)) {
                        throw e;
                    }
                } finally {
                }
            }
        } else {
            original.call(instance);
        }
    }
}
