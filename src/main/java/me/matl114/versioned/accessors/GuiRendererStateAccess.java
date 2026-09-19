package me.matl114.versioned.accessors;

import net.minecraft.client.renderer.state.gui.GuiRenderState;

public interface GuiRendererStateAccess {
    public void setLayerToDepth();

    public static GuiRendererStateAccess of(GuiRenderState state) {
        return (GuiRendererStateAccess) state;
    }
}
