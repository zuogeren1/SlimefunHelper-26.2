package me.matl114.mixins.events;

import me.matl114.events.Listener;
import me.matl114.events.impl.RecipeBookToggle;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(AbstractRecipeBookScreen.class)
public abstract class RecipeBookScreenEvents implements RecipeUpdateListener {
    @Shadow
    @Final
    private RecipeBookComponent<?> recipeBookComponent;

    @ModifyArg(
            method = "initButton",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/gui/components/ImageButton;<init>(IIIILnet/minecraft/client/gui/components/WidgetSprites;Lnet/minecraft/client/gui/components/Button$OnPress;)V"),
            index = 5)
    public Button.OnPress modifyPressAction(Button.OnPress pressAction) {
        return (button -> {
            pressAction.onPress(button);
            if (!Listener.getPostToggleRecipeBook().isEmpty()) {
                Listener.getPostToggleRecipeBook()
                        .broadcast(new RecipeBookToggle(this, this.recipeBookComponent, button));
            }
        });
    }
}
