package com.jsmacrosce.jsmacros.client.api.helper.world;

import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public class BlockDataHelper extends BaseHelper<BlockState> {
    private static final Minecraft mc = Minecraft.getInstance();

    public BlockDataHelper(BlockState b, BlockEntity e, BlockPos bp) {
        super(b);
    }
}
