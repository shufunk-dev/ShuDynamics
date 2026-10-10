package net.enchantedwood.world.gen;

import com.mojang.serialization.Codec;
import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.block.custom.ResonanceAltarBlock;
import net.enchantedwood.world.dimension.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
public class ResonanceArenaFeature implements Feature {
    public static final com.mojang.serialization.MapCodec<ResonanceArenaFeature> CODEC = com.mojang.serialization.MapCodec.unit(ResonanceArenaFeature::new);

    public ResonanceArenaFeature() {
    }

    @Override
    public com.mojang.serialization.MapCodec<? extends Feature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(net.minecraft.world.level.WorldGenLevel world, net.minecraft.world.level.chunk.ChunkGenerator generator, net.minecraft.util.RandomSource random, net.minecraft.core.BlockPos origin) {
        

        // Strict dimension lock: only generates in The Convergence
        if (world.getLevel().dimension() != ModDimensions.CONVERGENCE_WORLD_KEY) {
            return false;
        }

        

        // Deterministic Grid Spacing: Ensure exactly ONE arena per 256x256 block cell
        int cellX = Math.floorDiv(origin.getX(), 256);
        int cellZ = Math.floorDiv(origin.getZ(), 256);
        int targetX = cellX * 256 + 128;
        int targetZ = cellZ * 256 + 128;

        if (Math.abs(origin.getX() - targetX) > 16 || Math.abs(origin.getZ() - targetZ) > 16) {
            return false;
        }

        int surfaceY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, targetX, targetZ);
        if (surfaceY <= world.getMinY() + 10 || surfaceY >= world.getMaxY() - 20) {
            return false;
        }

        BlockPos center = new BlockPos(targetX, surfaceY, targetZ);
        int radius = 12;

        // 1. Foundation and Circular Arena Floor (Radius 12, Diameter 25)
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double distSq = dx * dx + dz * dz;
                if (distSq > radius * radius) {
                    continue;
                }

                // Foundation downward to guarantee solid ground on any terrain slope
                for (int dy = -4; dy <= -1; dy++) {
                    BlockPos fPos = center.offset(dx, dy, dz);
                    world.setBlock(fPos, Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 2);
                }

                // Arena Surface Floor (Y = 0)
                BlockPos floorPos = center.offset(dx, 0, dz);
                double dist = Math.sqrt(distSq);

                BlockState floorState;
                if (dist > 10.0) {
                    // Outer border rim
                    floorState = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
                } else if (dist > 8.0) {
                    // Inlay ring
                    floorState = (Math.abs(dx) % 2 == 0 || Math.abs(dz) % 2 == 0)
                            ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
                            : Blocks.GILDED_BLACKSTONE.defaultBlockState();
                } else if (dist > 4.5) {
                    // Middle battle ring
                    floorState = Blocks.SMOOTH_BASALT.defaultBlockState();
                } else if (dist > 2.0) {
                    // Inner ritual circle
                    floorState = (Math.abs(dx) == Math.abs(dz))
                            ? Blocks.AMETHYST_BLOCK.defaultBlockState()
                            : Blocks.POLISHED_BLACKSTONE.defaultBlockState();
                } else {
                    // Altar Dais Center
                    floorState = Blocks.CRYING_OBSIDIAN.defaultBlockState();
                }
                world.setBlock(floorPos, floorState, 2);

                // Clear unobstructed headroom above arena (Y = 1..10)
                for (int dy = 1; dy <= 10; dy++) {
                    BlockPos airPos = center.offset(dx, dy, dz);
                    world.setBlock(airPos, Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }

        // 2. Four Ritual Obelisk Pillars (at cardinal offsets of 8 blocks)
        int[][] pillarOffsets = {
                { 8, 0 },
                { -8, 0 },
                { 0, 8 },
                { 0, -8 }
        };

        for (int[] p : pillarOffsets) {
            BlockPos pBase = center.offset(p[0], 1, p[1]);
            world.setBlock(pBase, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), 2);
            world.setBlock(pBase.above(1), Blocks.CRYING_OBSIDIAN.defaultBlockState(), 2);
            world.setBlock(pBase.above(2), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), 2);
            world.setBlock(pBase.above(3), Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState(), 2);
            world.setBlock(pBase.above(4), Blocks.SOUL_LANTERN.defaultBlockState(), 2);
        }

        // 3. Central Dais: Place the Resonance Altar!
        BlockPos altarPos = center.above(1);
        world.setBlock(altarPos, ModBlocks.RESONANCE_ALTAR.defaultBlockState().setValue(ResonanceAltarBlock.ACTIVE, false), 2);

        // Surrounding Rune Blocks around Altar
        world.setBlock(altarPos.north(), Blocks.AMETHYST_BLOCK.defaultBlockState(), 2);
        world.setBlock(altarPos.south(), Blocks.AMETHYST_BLOCK.defaultBlockState(), 2);
        world.setBlock(altarPos.east(), Blocks.AMETHYST_BLOCK.defaultBlockState(), 2);
        world.setBlock(altarPos.west(), Blocks.AMETHYST_BLOCK.defaultBlockState(), 2);

        return true;
    }
}
