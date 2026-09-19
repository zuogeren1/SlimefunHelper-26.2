package xaeroplus;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import xaeroplus.feature.render.DrawManager;

public class Globals {
    public static final DrawManager drawManager = new DrawManager();
    public static final Identifier guiTextures = null;
    // cache and only update this on new world loads
    public static boolean nullOverworldDimensionFolder = false;
    public static int minimapScaleMultiplier = 1;
    public static int minimapSizeMultiplier = 1;
    public static boolean shouldResetFBO = false;
    public static boolean minimapSettingsInitialized = false;
    public static boolean switchingDimension = false;
    public static boolean disableDrawCullingOverride = false;
    public static boolean atomicMoveAvailable = false;
    public static boolean transparentWmBgApplyMapBlend = false;
    public static boolean transparentWmBgApplyMapFrameBlend = false;
    public static boolean bypassVertexCountLimit = false;

    public static ResourceKey<Level> getCurrentDimensionId() {
        return null;
    }
    // This can only be shared under the assumption region and texture cache writes are non-concurrent
    // sharing the underlying byte array reduces GC spam
    // at cost of a few MB higher idle RAM usage
    public static ByteArrayOutputStream zipFastByteBuffer = new ByteArrayOutputStream();
    public static final Supplier<ExecutorService> cacheRefreshExecutorService = null;
    public static final Supplier<ExecutorService> moduleExecutorService = null;

    public static void switchToDimension(final ResourceKey<Level> newDimId) {}

    public static void setNullOverworldDimFolderIfAble(final boolean b) {}
}
