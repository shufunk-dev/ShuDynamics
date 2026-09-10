package net.enchantedwood.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class BasaltCableBlockEntity extends BaseCableBlockEntity {
    public static final int CABLE_TRANSFER_RATE = 25_600; // 25,600 FE/t

    public BasaltCableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BASALT_CABLE_BE, pos, state, CABLE_TRANSFER_RATE);
    }
}
