package me.matl114.gui.presets.lists;

import me.matl114.gui.basic.*;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;

public class ListUnmodifiableWidget extends ScrollableListWidget {
    ListEntryWidgetController controller;

    public ListUnmodifiableWidget(ListEntryWidgetController controller, int x, int y, int dx, int dy) {
        super(x, y, dx, dy);
        this.controller = controller;
        refreshList();
    }

    protected void refreshList() {
        clearScrollingWidget();
        int size = controller.size();
        for (int i = 0; i < size; ++i) {
            ContentDelegateWidget<?> widget = wrapWidget(controller.getEntryWidget(i), i, 0, 0);
            addScrollingWidget(widget);
        }
    }

    protected <T extends GuiEventListener & Renderable & NarratableEntry> ContentDelegateWidget<T> wrapWidget(
            T widget, int listIndex, int startX, int startY) {
        int height = controller.height();
        int curHeight = startY + height * listIndex;
        ContentDelegateWidget<T> wrap1 =
                new ContentDelegateWidget<T>(startX, curHeight, controller.width(), height).setContentDelegate(widget);

        //        int buttonSize = Math.min(20, height);
        return wrap1;
    }
}
