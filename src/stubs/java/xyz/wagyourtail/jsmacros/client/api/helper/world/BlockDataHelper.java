package xyz.wagyourtail.jsmacros.client.api.helper.world;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import xyz.wagyourtail.jsmacros.core.helpers.BaseHelper;

public class BlockDataHelper extends BaseHelper<BlockState> {
    private static final Minecraft mc = Minecraft.getInstance();

    public BlockDataHelper(BlockState b, BlockEntity e, BlockPos bp) {
        super(b);
    }
}
