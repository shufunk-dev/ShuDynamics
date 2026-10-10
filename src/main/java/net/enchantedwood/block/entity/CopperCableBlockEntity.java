package net.enchantedwood.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class CopperCableBlockEntity extends BaseCableBlockEntity {
    public static final int CABLE_TRANSFER_RATE = 500; // 500 FE/t

    public CopperCableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COPPER_CABLE_BLOCK_ENTITY, pos, state, CABLE_TRANSFER_RATE);
    }
}
