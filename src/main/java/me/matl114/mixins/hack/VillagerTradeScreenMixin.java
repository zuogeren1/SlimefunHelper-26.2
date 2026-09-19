package me.matl114.mixins.hack;

import me.matl114.accessors.access.MerchantScreenAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MerchantScreen.class)
@Environment(EnvType.CLIENT)
public abstract class VillagerTradeScreenMixin extends AbstractContainerScreen<MerchantMenu>
        implements MerchantScreenAccess {
    public VillagerTradeScreenMixin(MerchantMenu handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Shadow
    private int shopItem;

    @Shadow
    protected abstract void postButtonClick();

    @Accessor("shopItem")
    public abstract int getSelectedIndex();

    @Unique
    @Override
    public void setSelectedIndex(int index) {
        this.shopItem = index;
        postButtonClick();
    }
}
