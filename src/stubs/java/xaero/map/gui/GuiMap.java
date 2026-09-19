package xaero.map.gui;

import java.util.ArrayList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaero.map.MapProcessor;
import xaero.map.gui.dropdown.rightclick.GuiRightClickMenu;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

public class GuiMap extends Screen implements IRightClickableElement {
    private MapProcessor mapProcessor;
    private int rightClickX;
    private int rightClickY;
    private int rightClickZ;
    private ResourceKey<Level> rightClickDim;
    private GuiRightClickMenu rightClickMenu;
    private MapTileSelection mapTileSelection;

    protected GuiMap(Component title) {
        super(title);
    }

    public ArrayList<RightClickOption> getRightClickOptions() {
        return new ArrayList<>();
    }

    public void onRightClickClosed() {}
}
