package xaeroplus.feature.render.ellipse;

import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

@FunctionalInterface
public interface EllipseSupplier {
    /**
     * Window = region xz center +- size. meaning the area is: (size*2)^2
     */
    List<Ellipse> getEllipses(
            final int windowRegionX,
            final int windowRegionZ,
            final int windowRegionSize,
            final ResourceKey<Level> dimension);
}
