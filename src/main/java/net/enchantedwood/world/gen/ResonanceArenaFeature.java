package net.enchantedwood.world.gen;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;
import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.block.custom.ResonanceAltarBlock;
import net.enchantedwood.world.dimension.ModDimensions;

public class ResonanceArenaFeature extends Feature<DefaultFeatureConfig> {

    public ResonanceArenaFeature(Codec<DefaultFeatureConfig> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();

        // Strict dimension lock: only generates in The Convergence
        if (world.toServerWorld().getRegistryKey() != ModDimensions.CONVERGENCE_WORLD_KEY) {
            return false;
        }

        BlockPos origin = context.getOrigin();

        // Deterministic Grid Spacing: Ensure exactly ONE arena per 256x256 block cell
        int cellX = Math.floorDiv(origin.getX(), 256);
        int cellZ = Math.floorDiv(origin.getZ(), 256);
        int targetX = cellX * 256 + 128;
        int targetZ = cellZ * 256 + 128;

        if (Math.abs(origin.getX() - targetX) > 16 || Math.abs(origin.getZ() - targetZ) > 16) {
            return false;
        }

        int surfaceY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, targetX, targetZ);
        if (surfaceY <= world.getBottomY() + 10 || surfaceY >= world.getTopYInclusive() - 20) {
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
                    BlockPos fPos = center.add(dx, dy, dz);
                    world.setBlockState(fPos, Blocks.POLISHED_DEEPSLATE.getDefaultState(), 2);
                }

                // Arena Surface Floor (Y = 0)
                BlockPos floorPos = center.add(dx, 0, dz);
                double dist = Math.sqrt(distSq);

                BlockState floorState;
                if (dist > 10.0) {
                    // Outer border rim
                    floorState = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
                } else if (dist > 8.0) {
                    // Inlay ring
                    floorState = (Math.abs(dx) % 2 == 0 || Math.abs(dz) % 2 == 0)
                            ? Blocks.CRYING_OBSIDIAN.getDefaultState()
                            : Blocks.GILDED_BLACKSTONE.getDefaultState();
                } else if (dist > 4.5) {
                    // Middle battle ring
                    floorState = Blocks.SMOOTH_BASALT.getDefaultState();
                } else if (dist > 2.0) {
                    // Inner ritual circle
                    floorState = (Math.abs(dx) == Math.abs(dz))
                            ? Blocks.AMETHYST_BLOCK.getDefaultState()
                            : Blocks.POLISHED_BLACKSTONE.getDefaultState();
                } else {
                    // Altar Dais Center
                    floorState = Blocks.CRYING_OBSIDIAN.getDefaultState();
                }
                world.setBlockState(floorPos, floorState, 2);

                // Clear unobstructed headroom above arena (Y = 1..10)
                for (int dy = 1; dy <= 10; dy++) {
                    BlockPos airPos = center.add(dx, dy, dz);
                    world.setBlockState(airPos, Blocks.AIR.getDefaultState(), 2);
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
            BlockPos pBase = center.add(p[0], 1, p[1]);
            world.setBlockState(pBase, Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState(), 2);
            world.setBlockState(pBase.up(1), Blocks.CRYING_OBSIDIAN.getDefaultState(), 2);
            world.setBlockState(pBase.up(2), Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState(), 2);
            world.setBlockState(pBase.up(3), Blocks.CHISELED_POLISHED_BLACKSTONE.getDefaultState(), 2);
            world.setBlockState(pBase.up(4), Blocks.SOUL_LANTERN.getDefaultState(), 2);
        }

        // 3. Central Dais: Place the Resonance Altar!
        BlockPos altarPos = center.up(1);
        world.setBlockState(altarPos, ModBlocks.RESONANCE_ALTAR.getDefaultState().with(ResonanceAltarBlock.ACTIVE, false), 2);

        // Surrounding Rune Blocks around Altar
        world.setBlockState(altarPos.north(), Blocks.AMETHYST_BLOCK.getDefaultState(), 2);
        world.setBlockState(altarPos.south(), Blocks.AMETHYST_BLOCK.getDefaultState(), 2);
        world.setBlockState(altarPos.east(), Blocks.AMETHYST_BLOCK.getDefaultState(), 2);
        world.setBlockState(altarPos.west(), Blocks.AMETHYST_BLOCK.getDefaultState(), 2);

        return true;
    }
}
