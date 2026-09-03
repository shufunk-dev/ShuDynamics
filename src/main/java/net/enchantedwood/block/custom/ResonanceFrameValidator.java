package net.enchantedwood.block.custom;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.enchantedwood.block.ModBlocks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ResonanceFrameValidator {

    private static final Set<Block> REQUIRED_KEYSTONES = Set.of(
            ModBlocks.ATMOSPHERIC_ANCHOR,
            ModBlocks.KINETIC_ANCHOR,
            ModBlocks.THERMAL_ANCHOR,
            ModBlocks.METALLURGICAL_ANCHOR,
            ModBlocks.PLASMA_ANCHOR,
            ModBlocks.DIMENSIONAL_SINGULARITY
    );

    public static boolean tryActivateGateway(World world, BlockPos clickedPos, ServerPlayerEntity player) {
        for (Direction dir : Direction.values()) {
            BlockPos airPos = clickedPos.offset(dir);
            if (world.isAir(airPos) || world.getBlockState(airPos).isOf(ModBlocks.DORMANT_RIFT)) {
                if (checkAndActivateOnAxis(world, airPos, Direction.Axis.X, player)) return true;
                if (checkAndActivateOnAxis(world, airPos, Direction.Axis.Z, player)) return true;
            }
        }
        return false;
    }

    private static boolean checkAndActivateOnAxis(World world, BlockPos startPos, Direction.Axis axis, ServerPlayerEntity player) {
        Direction widthDir = axis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;

        // Find bottom-left of candidate interior
        BlockPos.Mutable current = startPos.mutableCopy();
        while ((world.isAir(current.down()) || world.getBlockState(current.down()).isOf(ModBlocks.DORMANT_RIFT)) && current.getY() > world.getBottomY()) {
            current.move(Direction.DOWN);
        }
        while (world.isAir(current.offset(widthDir.getOpposite())) || world.getBlockState(current.offset(widthDir.getOpposite())).isOf(ModBlocks.DORMANT_RIFT)) {
            current.move(widthDir.getOpposite());
        }

        BlockPos bottomLeft = current.toImmutable();

        // Must be exactly 2 wide and 3 high
        int width = 2;
        int height = 3;

        List<BlockPos> interiorPositions = new ArrayList<>();
        Set<Block> foundKeystones = new HashSet<>();
        int cryingObsidianCount = 0;

        for (int w = -1; w <= width; w++) {
            for (int h = -1; h <= height; h++) {
                BlockPos pos = bottomLeft.offset(widthDir, w).up(h);
                boolean isBorder = (w == -1 || w == width || h == -1 || h == height);
                boolean isCorner = (w == -1 || w == width) && (h == -1 || h == height);

                if (isBorder) {
                    if (!isCorner) {
                        BlockState state = world.getBlockState(pos);
                        if (state.isOf(Blocks.CRYING_OBSIDIAN)) {
                            cryingObsidianCount++;
                        } else if (REQUIRED_KEYSTONES.contains(state.getBlock())) {
                            foundKeystones.add(state.getBlock());
                        } else {
                            // Non-corner border must be crying obsidian or a keystone!
                            return false;
                        }
                    }
                } else {
                    // Interior must be air or already dormant rift
                    if (!world.isAir(pos) && !world.getBlockState(pos).isOf(ModBlocks.DORMANT_RIFT)) {
                        return false;
                    }
                    interiorPositions.add(pos);
                }
            }
        }

        // Must contain all 6 distinct keystones and at least 4 crying obsidian
        if (foundKeystones.size() >= 6 && cryingObsidianCount >= 4) {
            // Fill interior with Dormant Rift blocks
            for (BlockPos pos : interiorPositions) {
                world.setBlockState(pos, ModBlocks.DORMANT_RIFT.getDefaultState().with(DormantRiftBlock.AXIS, axis));
            }

            if (!world.isClient() && world instanceof ServerWorld serverWorld) {
                for (BlockPos pos : interiorPositions) {
                    serverWorld.spawnParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.3, 0.4, 0.3, 0.05);
                    serverWorld.spawnParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 15, 0.4, 0.5, 0.4, 0.1);
                }

                world.playSound(null, bottomLeft, SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.BLOCKS, 1.2f, 0.9f);
                world.playSound(null, bottomLeft, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1.5f, 1.8f);

                if (serverWorld.getServer() != null) {
                    serverWorld.getServer().getPlayerManager().broadcast(
                            Text.literal("§5✦ [Spatial Sensors] §dThe 6 Keystones hum in harmonic resonance... The Gateway stirs, waiting for the celestial rift to align."),
                            false
                    );

                    if (player != null) {
                        var advEntry = serverWorld.getServer().getAdvancementLoader().get(net.minecraft.util.Identifier.of("enchantedwood", "anomalies/gateway_of_resonance"));
                        if (advEntry != null) {
                            player.getAdvancementTracker().grantCriterion(advEntry, "activated_gateway");
                        }
                    }
                }
            }
            return true;
        }

        return false;
    }
}
