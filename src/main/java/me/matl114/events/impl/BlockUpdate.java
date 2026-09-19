package me.matl114.events.impl;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public record BlockUpdate(ClientLevel world, BlockPos pos, BlockState oldState, BlockState newState) {}
