package net.enchantedwood.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class SteelCableBlockEntity extends BaseCableBlockEntity {
    public static final int CABLE_TRANSFER_RATE = 12_500; // 12,500 FE/t

    public SteelCableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STEEL_CABLE_BLOCK_ENTITY, pos, state, CABLE_TRANSFER_RATE);
    }
}
