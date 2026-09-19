package me.matl114.gui.basic;

import javax.annotation.Nullable;
import net.minecraft.client.gui.components.Renderable;

public interface ColorProvider {
    @Nullable
    public Integer provideTextColor(Renderable widget, boolean isFocused);
}
