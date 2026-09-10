package net.enchantedwood.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class CopperCableBlockEntity extends BaseCableBlockEntity {
    public static final int CABLE_TRANSFER_RATE = 500; // 500 FE/t

    public CopperCableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COPPER_CABLE_BLOCK_ENTITY, pos, state, CABLE_TRANSFER_RATE);
    }
}
