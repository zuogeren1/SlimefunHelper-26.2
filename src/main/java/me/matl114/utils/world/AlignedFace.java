package me.matl114.utils.world;

import lombok.Getter;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class AlignedFace {
    @Getter
    Vec3 from;

    @Getter
    Vec3 to;

    @Getter(lazy = true)
    private final Vec3 dimensions = new Vec3(to.x - from.x, to.y - from.y, to.z - from.z);
    //    public Vec3d getDimensions(){
    //        return ;
    //    }
    @Getter(lazy = true)
    private final double area = calculateArea(getDimensions());

    @Getter(lazy = true)
    private final Vec3 center = from.add(to).scale(0.5);

    private double calculateArea(Vec3 dims) {
        return (dims.x * dims.y + dims.y * dims.z + dims.x * dims.z) * 2.0;
    }

    public AlignedFace(Vec3 from, Vec3 to) {
        this.from = new Vec3(Math.min(from.x, to.x), Math.min(from.y, to.y), Math.min(from.z, to.z));
        this.to = new Vec3(Math.max(from.x, to.x), Math.max(from.y, to.y), Math.max(from.z, to.z));
    }

    public AlignedFace truncateY(double minY) {

        return new AlignedFace(
                new Vec3(this.from.x, Math.max(this.from.y, minY), this.from.z),
                new Vec3(this.to.x, Math.max(this.to.y, minY), this.to.z));
    }

    public boolean isEmpty() {
        return Mth.equal(this.getArea(), 0.0F);
    }
}
