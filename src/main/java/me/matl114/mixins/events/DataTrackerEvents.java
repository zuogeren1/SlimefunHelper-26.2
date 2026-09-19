package me.matl114.mixins.events;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import java.util.ArrayList;
import java.util.List;
import me.matl114.events.Event;
import me.matl114.events.Listener;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.syncher.SyncedDataHolder;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SynchedEntityData.class)
@Environment(EnvType.CLIENT)
public abstract class DataTrackerEvents {
    @Final
    @Shadow
    private SyncedDataHolder entity;

    @Inject(method = "assignValues", at = @At("HEAD"))
    private void callDataTrackerEntryUpdateEvents(
            List<SynchedEntityData.DataValue<?>> entries,
            CallbackInfo ci,
            @Local(argsOnly = true) LocalRef<List<SynchedEntityData.DataValue<?>>> entryRef) {
        // make it removable
        if (Listener.getEntityTrackDataUpdate().isEmpty()) return;
        if (this.entity instanceof Entity entity) {
            // recreate List to avoid immutableList
            List<SynchedEntityData.DataValue<?>> entryList = new ArrayList<>();
            var iterator = entries.iterator();
            while (iterator.hasNext()) {
                SynchedEntityData.DataValue<?> serializedEntry = iterator.next();
                Event<SynchedEntityData.DataValue<?>> serializedEntryMutableObject =
                        new Event<>(serializedEntry, true, true, this.entity);
                Listener.getEntityTrackDataUpdate().handleValue(serializedEntryMutableObject);
                if (serializedEntryMutableObject.isCancelled() || serializedEntryMutableObject.context() == null) {
                    // skip current serializedEntry
                    continue;
                } else {
                    entryList.add(serializedEntryMutableObject.context());
                }
            }
            entryRef.set(entryList);
        }
    }
}
