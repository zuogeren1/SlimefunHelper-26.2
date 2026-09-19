package xaeroplus.feature.highlights;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import java.util.concurrent.CompletableFuture;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class ChunkHighlightLocalCache extends ChunkHighlightBaseCacheHandler {
    private static final int maxNumber = 5000;

    public ChunkHighlightLocalCache() {
        super();
    }

    @Override
    public void addHighlight(final int x, final int z) {}

    @Override
    public void addHighlight(final int x, final int z, final long foundTime) {}

    private void limitChunksSize() {}

    @Override
    public CompletableFuture<Long2LongMap> getHighlightsInCustomWindow(
            final int windowRegionX,
            final int windowRegionZ,
            final int windowRegionSize,
            final ResourceKey<Level> dimension) {
        return null;
    }

    @Override
    public void handleTick() {}

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {}
}
