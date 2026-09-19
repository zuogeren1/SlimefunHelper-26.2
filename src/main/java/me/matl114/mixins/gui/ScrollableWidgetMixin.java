package me.matl114.mixins.gui;

import me.matl114.gui.basic.Draggable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Environment(EnvType.CLIENT)
@Mixin(AbstractScrollArea.class)
public abstract class ScrollableWidgetMixin extends AbstractWidget implements Draggable {
    public ScrollableWidgetMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Shadow
    private boolean scrolling;

    @Shadow
    protected abstract boolean scrollbarVisible();

    public boolean isDragging() {
        return scrolling;
    }

    public void releaseDrag(Screen screen, double mouseX, double mouseY) {
        this.scrolling = false;
    }

    public boolean startDrag(Screen screen, double mouseX, double mouseY) {
        if (this.scrollbarVisible()
                && mouseX >= (double) (this.getX() + this.width)
                && mouseX <= (double) (this.getX() + this.width + 8)
                && mouseY >= (double) this.getY()
                && mouseY < (double) (this.getY() + this.height)) {
            this.scrolling = true;
            return true;
        }
        return false;
    }
}
