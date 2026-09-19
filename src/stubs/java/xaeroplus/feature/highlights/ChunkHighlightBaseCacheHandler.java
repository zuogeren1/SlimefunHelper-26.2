package xaeroplus.feature.highlights;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongCollection;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public abstract class ChunkHighlightBaseCacheHandler implements ChunkHighlightCache {
    public final Long2LongMap chunks = new Long2LongOpenHashMap();
    public Minecraft mc = Minecraft.getInstance();

    public ChunkHighlightBaseCacheHandler() {
        this.chunks.defaultReturnValue(-1);
    }

    @Override
    public void addHighlight(final int x, final int z) {
        addHighlight(x, z, System.currentTimeMillis());
    }

    @Override
    public void addHighlight(final int x, final int z, final ResourceKey<Level> dimensionId) {
        addHighlight(x, z);
    }

    @Override
    public void addHighlight(final int x, final int z, final long foundTime) {}

    @Override
    public void addHighlight(final int x, final int z, final long foundTime, final ResourceKey<Level> dimensionId) {
        addHighlight(x, z, foundTime);
    }

    @Override
    public void removeHighlight(final int x, final int z) {}

    @Override
    public void removeHighlight(final int x, final int z, final ResourceKey<Level> dimensionId) {
        removeHighlight(x, z);
    }

    @Override
    public void removeHighlights(final LongCollection toRemove) {}

    @Override
    public void removeHighlights(final LongCollection toRemove, ResourceKey<Level> dimensionId) {
        removeHighlights(toRemove);
    }

    @Override
    public boolean isHighlighted(final int x, final int z, ResourceKey<Level> dimensionId) {
        return false;
    }

    @Override
    public Long2LongMap getCacheMap(final ResourceKey<Level> dimension) {
        return chunks;
    }

    public boolean isHighlighted(final long chunkPos) {
        return chunks.containsKey(chunkPos);
    }

    public void replaceState(final Long2LongOpenHashMap state) {}

    public void reset() {}
}
