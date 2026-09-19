package me.matl114.mixins.interfaces;

import javax.annotation.Nullable;
import me.matl114.accessors.interfaces.EntityInventory;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.inventory.MerchantMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Environment(EnvType.CLIENT)
@Mixin(MerchantMenu.class)
public abstract class MerchantScreenHandlerMixin implements EntityInventory.Handler<Villager> {
    @Unique
    Villager owner;

    @Nullable
    @Override
    @Unique
    public Villager getOwner() {
        return owner;
    }

    @Override
    public void sync(EntityInventory<Villager> inventory) {
        this.owner = inventory.getOwner();
    }
}
