package me.matl114.mixins.access;

import com.llamalad7.mixinextras.sugar.Local;
import me.matl114.accessors.moonrise.MoonriseChunkBlockCountingAccess;
import me.matl114.utils.CollisionUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
// compat idiot lithium
@Mixin(value = LevelChunkSection.class, priority = 3000)
public abstract class MoonriseChunkBlockCountingMixin implements MoonriseChunkBlockCountingAccess {
    @Shadow
    public abstract void recalcBlockCounts();

    @Unique
    int specialCollidingBlocks = 0;

    public int getSpecialCollidingBlockCount() {
        return specialCollidingBlocks;
    }

    @Inject(method = "<init>(Lnet/minecraft/world/level/chunk/PalettedContainerFactory;)V", at = @At("TAIL"))
    private void calculateBlockCount(PalettedContainerFactory palettesFactory, CallbackInfo ci) {
        this.recalcBlockCounts();
    }

    @Inject(method = "<init>(Lnet/minecraft/world/level/chunk/LevelChunkSection;)V", at = @At("RETURN"))
    private void calculateBlockCount2(LevelChunkSection section, CallbackInfo ci) {
        this.recalcBlockCounts();
    }

    @Inject(method = "read", at = @At(value = "RETURN"))
    private void calculateBlockCount(FriendlyByteBuf buf, CallbackInfo ci) {
        this.recalcBlockCounts();
    }

    @Inject(
            method =
                    "setBlockState(IIILnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
            at =
                    @At(
                            value = "INVOKE",
                            shift = At.Shift.BEFORE,
                            target =
                                    "Lnet/minecraft/world/level/block/state/BlockState;getFluidState()Lnet/minecraft/world/level/material/FluidState;",
                            ordinal = 0))
    private void calculateSpecialCollidingBlocks(
            int x,
            int y,
            int z,
            BlockState state,
            boolean lock,
            CallbackInfoReturnable<BlockState> cir,
            @Local(ordinal = 1) BlockState blockState) {
        if (CollisionUtil.isSpecialCollidingBlock(blockState)) {
            --this.specialCollidingBlocks;
        }
        if (CollisionUtil.isSpecialCollidingBlock(state)) {
            ++this.specialCollidingBlocks;
        }
    }

    @Unique
    private int tmpSpecialCollidingBlocksCounter = 0;

    @Inject(
            method = "recalcBlockCounts",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/chunk/PalettedContainer;count(Lnet/minecraft/world/level/chunk/PalettedContainer$CountConsumer;)V",
                            shift = At.Shift.BEFORE))
    private void preBlockCount(CallbackInfo ci) {
        tmpSpecialCollidingBlocksCounter = 0;
    }

    @Inject(
            method = "recalcBlockCounts",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/chunk/PalettedContainer;count(Lnet/minecraft/world/level/chunk/PalettedContainer$CountConsumer;)V",
                            shift = At.Shift.AFTER))
    private void postBlockCount(CallbackInfo ci) {
        this.specialCollidingBlocks = tmpSpecialCollidingBlocksCounter;
        tmpSpecialCollidingBlocksCounter = 0;
    }

    @ModifyArg(
            method = "recalcBlockCounts",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/chunk/PalettedContainer;count(Lnet/minecraft/world/level/chunk/PalettedContainer$CountConsumer;)V"))
    private PalettedContainer.CountConsumer addCountingWrapper(PalettedContainer.CountConsumer counter) {
        return (PalettedContainer.CountConsumer) (acc, i) -> {
            counter.accept(acc, i);
            if (CollisionUtil.isSpecialCollidingBlock((BlockState) acc)) {
                tmpSpecialCollidingBlocksCounter += i;
            }
        };
    }
}
