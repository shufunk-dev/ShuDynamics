package net.enchantedwood.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class AluminumCableBlockEntity extends BaseCableBlockEntity {
    public static final int CABLE_TRANSFER_RATE = 2_500; // 2,500 FE/t

    public AluminumCableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALUMINUM_CABLE_BLOCK_ENTITY, pos, state, CABLE_TRANSFER_RATE);
    }
}
