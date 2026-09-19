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

    /** 目标类 AbstractScrollArea 上"内容是否溢出/可滚动"的判断（旧版叫 overflows）。本类只 extends AbstractWidget，必须靠 @Shadow 拿。 */
    @Shadow
    protected abstract boolean scrollable();

    @Shadow
    protected abstract boolean isOverScrollbar(double mouseX, double mouseY);

    public boolean isDragging() {
        return scrolling;
    }

    public void releaseDrag(Screen screen, double mouseX, double mouseY) {
        this.scrolling = false;
    }

    public boolean startDrag(Screen screen, double mouseX, double mouseY) {
        // 26.2 移除了 scrollbarVisible()。注意不能直接用 isOverScrollbar()：
        //   - 它是「右内侧」scrollbarWidth()（默认 6）px 带，旧版是右边界及其「右外侧」8px
        //   - 它不含"内容溢出"前置判断（旧版 overflows() ≡ scrollable() ≡ maxScrollAmount() > 0），
        //     26.2 把该判断上移到调用方 updateScrolling 了
        // 因此按 1.21.11 的原语义重写，保持命中区与前置条件一致。
        if (this.scrollable()
                && mouseX >= (double) this.getRight()
                && mouseX <= (double) (this.getRight() + 8)
                && mouseY >= (double) this.getY()
                && mouseY < (double) this.getBottom()) {
            this.scrolling = true;
            return true;
        }
        return false;
    }
}
