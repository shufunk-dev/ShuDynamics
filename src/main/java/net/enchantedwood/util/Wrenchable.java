package net.enchantedwood.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public interface Wrenchable {
    ActionResult onWrenched(World world, BlockPos pos, PlayerEntity player, Direction side);
    ActionResult onShiftWrenched(World world, BlockPos pos, PlayerEntity player, Direction side);
}
