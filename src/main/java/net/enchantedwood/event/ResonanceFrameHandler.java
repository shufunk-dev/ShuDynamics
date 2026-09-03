package net.enchantedwood.event;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.block.custom.ResonanceFrameValidator;

public class ResonanceFrameHandler {

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            BlockPos pos = hitResult.getBlockPos();
            BlockState state = world.getBlockState(pos);

            if (state.isOf(Blocks.CRYING_OBSIDIAN) ||
                state.isOf(ModBlocks.ATMOSPHERIC_ANCHOR) ||
                state.isOf(ModBlocks.KINETIC_ANCHOR) ||
                state.isOf(ModBlocks.THERMAL_ANCHOR) ||
                state.isOf(ModBlocks.METALLURGICAL_ANCHOR) ||
                state.isOf(ModBlocks.PLASMA_ANCHOR) ||
                state.isOf(ModBlocks.DIMENSIONAL_SINGULARITY)) {

                if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer) {
                    if (ResonanceFrameValidator.tryActivateGateway(world, pos, serverPlayer)) {
                        return ActionResult.SUCCESS;
                    }
                }
            }
            return ActionResult.PASS;
        });
    }
}
