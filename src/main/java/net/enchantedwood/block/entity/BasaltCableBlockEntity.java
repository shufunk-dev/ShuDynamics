package net.enchantedwood.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class BasaltCableBlockEntity extends BaseCableBlockEntity {
    public static final int CABLE_TRANSFER_RATE = 25_600; // 25,600 FE/t

    public BasaltCableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BASALT_CABLE_BE, pos, state, CABLE_TRANSFER_RATE);
    }
}
