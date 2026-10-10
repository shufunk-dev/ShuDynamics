package net.enchantedwood.block.custom;

import net.enchantedwood.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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

    public static boolean tryActivateGateway(Level world, BlockPos clickedPos, ServerPlayer player) {
        for (Direction dir : Direction.values()) {
            BlockPos airPos = clickedPos.relative(dir);
            if (world.isEmptyBlock(airPos) || world.getBlockState(airPos).is(ModBlocks.DORMANT_RIFT)) {
                if (checkAndActivateOnAxis(world, airPos, Direction.Axis.X, player)) return true;
                if (checkAndActivateOnAxis(world, airPos, Direction.Axis.Z, player)) return true;
            }
        }
        return false;
    }

    private static boolean checkAndActivateOnAxis(Level world, BlockPos startPos, Direction.Axis axis, ServerPlayer player) {
        Direction widthDir = axis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;

        // Find bottom-left of candidate interior
        BlockPos.MutableBlockPos current = startPos.mutable();
        while ((world.isEmptyBlock(current.below()) || world.getBlockState(current.below()).is(ModBlocks.DORMANT_RIFT)) && current.getY() > world.getMinY()) {
            current.move(Direction.DOWN);
        }
        while (world.isEmptyBlock(current.relative(widthDir.getOpposite())) || world.getBlockState(current.relative(widthDir.getOpposite())).is(ModBlocks.DORMANT_RIFT)) {
            current.move(widthDir.getOpposite());
        }

        BlockPos bottomLeft = current.immutable();

        // Must be exactly 2 wide and 3 high
        int width = 2;
        int height = 3;

        List<BlockPos> interiorPositions = new ArrayList<>();
        Set<Block> foundKeystones = new HashSet<>();
        int cryingObsidianCount = 0;

        for (int w = -1; w <= width; w++) {
            for (int h = -1; h <= height; h++) {
                BlockPos pos = bottomLeft.relative(widthDir, w).above(h);
                boolean isBorder = (w == -1 || w == width || h == -1 || h == height);
                boolean isCorner = (w == -1 || w == width) && (h == -1 || h == height);

                if (isBorder) {
                    if (!isCorner) {
                        BlockState state = world.getBlockState(pos);
                        if (state.is(Blocks.CRYING_OBSIDIAN)) {
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
                    if (!world.isEmptyBlock(pos) && !world.getBlockState(pos).is(ModBlocks.DORMANT_RIFT)) {
                        return false;
                    }
                    interiorPositions.add(pos);
                }
            }
        }

        // Must contain all 6 distinct keystones and at least 4 crying obsidian
        if (foundKeystones.size() >= 6 && cryingObsidianCount >= 4) {
            // Check if already fully activated to prevent duplicate sound/message spam
            boolean alreadyIgnited = true;
            for (BlockPos pos : interiorPositions) {
                if (!world.getBlockState(pos).is(ModBlocks.DORMANT_RIFT)) {
                    alreadyIgnited = false;
                    break;
                }
            }
            if (alreadyIgnited) {
                return false;
            }

            // Fill interior with Dormant Rift blocks
            for (BlockPos pos : interiorPositions) {
                world.setBlockAndUpdate(pos, ModBlocks.DORMANT_RIFT.defaultBlockState().setValue(DormantRiftBlock.AXIS, axis));
            }

            if (!world.isClientSide() && world instanceof ServerLevel serverWorld) {
                net.enchantedwood.world.dimension.ConvergencePortalManager.registerGateway(serverWorld, bottomLeft);
                for (BlockPos pos : interiorPositions) {
                    serverWorld.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.3, 0.4, 0.3, 0.05);
                    serverWorld.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 15, 0.4, 0.5, 0.4, 0.1);
                }

                world.playSound(null, bottomLeft, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1.2f, 0.9f);
                world.playSound(null, bottomLeft, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.5f, 1.8f);

                if (serverWorld.getServer() != null) {
                    serverWorld.getServer().getPlayerList().broadcastSystemMessage(
                            Component.literal("§5✦ [Spatial Sensors] §dThe 6 Keystones achieve critical harmonic resonance! The dimensional barrier ruptures—the Gateway to The Convergence is OPEN!"),
                            false
                    );

                    if (player != null) {
                        var advEntry = serverWorld.getServer().getAdvancements().get(net.minecraft.resources.Identifier.fromNamespaceAndPath("enchantedwood", "anomalies/gateway_of_resonance"));
                        if (advEntry != null) {
                            player.getAdvancements().award(advEntry, "activated_gateway");
                        }

                        // Award Music Disc: Rip the Sky Wide (Convergence)!
                        ItemStack disc = new ItemStack(net.enchantedwood.item.ModItems.MUSIC_DISC_CONVERGENCE);
                        if (!player.getInventory().add(disc)) {
                            player.drop(disc, false, net.minecraft.util.Prediction.SERVER_ONLY);
                        }
                        player.sendSystemMessage(Component.literal("§5✦ The dimensional rift frequency crystallized into a Music Disc (Convergence)! ✦"));
                    }
                }
            }
            return true;
        }

        return false;
    }
}
