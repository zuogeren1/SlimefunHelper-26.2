package me.matl114.versioned.accessors;

import net.minecraft.client.renderer.state.gui.GuiRenderState;

public interface GuiRenderStateLayerAccess {
    public int getDepth();

    public void setDepth(int depth);

    public static GuiRenderStateLayerAccess of(GuiRenderState.Node layer) {
        return (GuiRenderStateLayerAccess) layer;
    }
}
