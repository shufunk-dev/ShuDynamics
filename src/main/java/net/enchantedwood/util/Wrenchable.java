package net.enchantedwood.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public interface Wrenchable {
    InteractionResult onWrenched(Level world, BlockPos pos, Player player, Direction side);
    InteractionResult onShiftWrenched(Level world, BlockPos pos, Player player, Direction side);
}
