package me.matl114.hooks.mixin.baritone;

import baritone.cache.ChunkPacker;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(ChunkPacker.class)
@Environment(EnvType.CLIENT)
public abstract class BaritoneChunkPackerFixMixin {
    @Unique
    private static final BlockState a = Blocks.AIR.defaultBlockState();

    @WrapOperation(
            method = "a(Lnet/minecraft/world/level/chunk/LevelChunk;)Lbaritone/cache/CachedChunk;",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lbaritone/utils/BlockStateInterface;a(Lnet/minecraft/world/level/chunk/LevelChunk;III)Lnet/minecraft/world/level/block/state/BlockState;"),
            require = 0)
    private static BlockState fixWorldAccessIndexOutOfBound(
            LevelChunk chunk, int x, int y, int z, Operation<BlockState> original) {
        if (y < 0 || y >= (chunk.getSections().length << 4)) {
            return a;
        }
        return original.call(chunk, x, y, z);
    }
}
