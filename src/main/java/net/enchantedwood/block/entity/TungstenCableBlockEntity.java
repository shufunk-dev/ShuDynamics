package net.enchantedwood.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class TungstenCableBlockEntity extends BaseCableBlockEntity {
    public static final int CABLE_TRANSFER_RATE = 25_000; // 25,000 FE/t

    public TungstenCableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TUNGSTEN_CABLE_BE, pos, state, CABLE_TRANSFER_RATE);
    }
}
