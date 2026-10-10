package net.enchantedwood.event;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.block.custom.ResonanceFrameValidator;

public class ResonanceFrameHandler {

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            BlockPos pos = hitResult.getBlockPos();
            BlockState state = world.getBlockState(pos);

            if (state.is(Blocks.CRYING_OBSIDIAN) ||
                state.is(ModBlocks.ATMOSPHERIC_ANCHOR) ||
                state.is(ModBlocks.KINETIC_ANCHOR) ||
                state.is(ModBlocks.THERMAL_ANCHOR) ||
                state.is(ModBlocks.METALLURGICAL_ANCHOR) ||
                state.is(ModBlocks.PLASMA_ANCHOR) ||
                state.is(ModBlocks.DIMENSIONAL_SINGULARITY)) {

                if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                    if (ResonanceFrameValidator.tryActivateGateway(world, pos, serverPlayer)) {
                        return InteractionResult.SUCCESS;
                    }
                }
            }
            return InteractionResult.PASS;
        });
    }
}
