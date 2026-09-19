package me.matl114.utils.render;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import me.matl114.utils.collections.IndexEntry;
import me.matl114.versioned.api.VDrawContext;

public interface RenderCollector<B> {
    void submit(B val, int color);

    void clear();

    void render3D(PoseStack matrices);

    void render2D(VDrawContext vDrawContext);

    public abstract static class Impl<B> implements RenderCollector<B> {
        protected List<IndexEntry<B>> entries = new ArrayList<>();

        @Override
        public void submit(B val, int color) {
            entries.add(new IndexEntry<>(color, val));
        }

        public void clear() {
            if (!entries.isEmpty()) {
                entries.clear();
            }
        }
    }
}
