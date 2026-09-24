package me.matl114.events.impl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.ItemStack;

@Data
@AllArgsConstructor
@Getter
@Accessors(fluent = true, chain = true)
public class UseItemOnBlock {
    @Setter
    BlockHitResult hitResult;

    @Setter
    InteractionResult actionResult;

    boolean placeBlock;

    final InteractionHand hand;

    ItemStack handItem;
}
