package net.enchantedwood.world.gen;

import com.mojang.serialization.Codec;
import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.world.dimension.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
public class NetherIncursionFeature implements Feature {
    public static final com.mojang.serialization.MapCodec<NetherIncursionFeature> CODEC = com.mojang.serialization.MapCodec.unit(NetherIncursionFeature::new);

    public NetherIncursionFeature() {
    }

    @Override
    public com.mojang.serialization.MapCodec<? extends Feature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(net.minecraft.world.level.WorldGenLevel world, net.minecraft.world.level.chunk.ChunkGenerator generator, net.minecraft.util.RandomSource random, net.minecraft.core.BlockPos origin) {
        

        // STRICT DIMENSION CHECK: Exclusively generates in The Convergence
        if (world.getLevel().dimension() != ModDimensions.CONVERGENCE_WORLD_KEY) {
            return false;
        }

        
        

        // Check if origin biome is one of the Nether incursion biomes
        Holder<Biome> biomeEntry = world.getBiome(origin);
        boolean isWarped = biomeEntry.is(Biomes.WARPED_FOREST);
        boolean isCrimson = biomeEntry.is(Biomes.CRIMSON_FOREST);
        boolean isSoulSand = biomeEntry.is(Biomes.SOUL_SAND_VALLEY);
        boolean isBasalt = biomeEntry.is(Biomes.BASALT_DELTAS);
        boolean isEnd = biomeEntry.is(Biomes.END_HIGHLANDS);

        if (!isWarped && !isCrimson && !isSoulSand && !isBasalt && !isEnd) {
            return false;
        }

        boolean placedAny = false;

        // 1. Surface and Subsurface Terrain Transformation (16x16 column area)
        for (int dx = -8; dx < 8; dx++) {
            for (int dz = -8; dz < 8; dz++) {
                BlockPos colPos = origin.offset(dx, 0, dz);
                Holder<Biome> colBiome = world.getBiome(colPos);

                boolean colWarped = colBiome.is(Biomes.WARPED_FOREST);
                boolean colCrimson = colBiome.is(Biomes.CRIMSON_FOREST);
                boolean colSoulSand = colBiome.is(Biomes.SOUL_SAND_VALLEY);
                boolean colBasalt = colBiome.is(Biomes.BASALT_DELTAS);
                boolean colEnd = colBiome.is(Biomes.END_HIGHLANDS);

                if (!colWarped && !colCrimson && !colSoulSand && !colBasalt && !colEnd) {
                    continue;
                }

                // Find highest solid surface block
                int topY = world.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, colPos.getX(), colPos.getZ()) - 1;
                BlockPos surfacePos = new BlockPos(colPos.getX(), topY, colPos.getZ());

                BlockState surfaceState = world.getBlockState(surfacePos);
                if (!isTerrainReplaceable(surfaceState)) {
                    continue;
                }

                if (colWarped) {
                    // Warped Nylium on top
                    world.setBlock(surfacePos, Blocks.WARPED_NYLIUM.defaultBlockState(), 2);
                    int depth = 3 + random.nextInt(3);
                    for (int d = 1; d <= depth; d++) {
                        BlockPos under = surfacePos.below(d);
                        if (isTerrainReplaceable(world.getBlockState(under))) {
                            world.setBlock(under, Blocks.NETHERRACK.defaultBlockState(), 2);
                        }
                    }
                    placedAny = true;

                    // Huge Warped Fungus Tree Chance (approx 1-3 per chunk)
                    if (random.nextInt(30) == 0 && world.isEmptyBlock(surfacePos.above())) {
                        generateHugeWarpedFungus(world, surfacePos.above(), random);
                    } else if (random.nextInt(5) == 0 && world.isEmptyBlock(surfacePos.above())) {
                        // Flora carpet
                        int floraRoll = random.nextInt(10);
                        if (floraRoll < 4) {
                            world.setBlock(surfacePos.above(), Blocks.WARPED_ROOTS.defaultBlockState(), 2);
                        } else if (floraRoll < 7) {
                            world.setBlock(surfacePos.above(), Blocks.NETHER_SPROUTS.defaultBlockState(), 2);
                        } else if (floraRoll < 8) {
                            world.setBlock(surfacePos.above(), Blocks.WARPED_FUNGUS.defaultBlockState(), 2);
                        } else {
                            int vineHeight = 1 + random.nextInt(4);
                            for (int v = 0; v < vineHeight; v++) {
                                BlockPos vinePos = surfacePos.above(1 + v);
                                if (world.isEmptyBlock(vinePos)) {
                                    world.setBlock(vinePos, Blocks.TWISTING_VINES.defaultBlockState(), 2);
                                } else {
                                    break;
                                }
                            }
                        }
                    }
                } else if (colCrimson) {
                    // Crimson Nylium on top
                    world.setBlock(surfacePos, Blocks.CRIMSON_NYLIUM.defaultBlockState(), 2);
                    int depth = 3 + random.nextInt(3);
                    for (int d = 1; d <= depth; d++) {
                        BlockPos under = surfacePos.below(d);
                        if (isTerrainReplaceable(world.getBlockState(under))) {
                            world.setBlock(under, Blocks.NETHERRACK.defaultBlockState(), 2);
                        }
                    }
                    placedAny = true;

                    // Huge Crimson Fungus Tree Chance
                    if (random.nextInt(30) == 0 && world.isEmptyBlock(surfacePos.above())) {
                        generateHugeCrimsonFungus(world, surfacePos.above(), random);
                    } else if (random.nextInt(5) == 0 && world.isEmptyBlock(surfacePos.above())) {
                        int floraRoll = random.nextInt(10);
                        if (floraRoll < 7) {
                            world.setBlock(surfacePos.above(), Blocks.CRIMSON_ROOTS.defaultBlockState(), 2);
                        } else {
                            world.setBlock(surfacePos.above(), Blocks.CRIMSON_FUNGUS.defaultBlockState(), 2);
                        }
                    }
                } else if (colSoulSand) {
                    BlockState soulState = random.nextBoolean() ? Blocks.SOUL_SAND.defaultBlockState() : Blocks.SOUL_SOIL.defaultBlockState();
                    world.setBlock(surfacePos, soulState, 2);
                    int depth = 2 + random.nextInt(4);
                    for (int d = 1; d <= depth; d++) {
                        BlockPos under = surfacePos.below(d);
                        if (isTerrainReplaceable(world.getBlockState(under))) {
                            world.setBlock(under, soulState, 2);
                        }
                    }
                    placedAny = true;
                } else if (colBasalt) {
                    BlockState basaltState = random.nextFloat() < 0.65f ? Blocks.BASALT.defaultBlockState() :
                            (random.nextFloat() < 0.5f ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.MAGMA_BLOCK.defaultBlockState());
                    world.setBlock(surfacePos, basaltState, 2);
                    int depth = 2 + random.nextInt(3);
                    for (int d = 1; d <= depth; d++) {
                        BlockPos under = surfacePos.below(d);
                        if (isTerrainReplaceable(world.getBlockState(under))) {
                            world.setBlock(under, Blocks.BLACKSTONE.defaultBlockState(), 2);
                        }
                    }
                    placedAny = true;
                } else if (colEnd) {
                    world.setBlock(surfacePos, Blocks.END_STONE.defaultBlockState(), 2);
                    int depth = 3 + random.nextInt(3);
                    for (int d = 1; d <= depth; d++) {
                        BlockPos under = surfacePos.below(d);
                        if (isTerrainReplaceable(world.getBlockState(under))) {
                            world.setBlock(under, Blocks.END_STONE.defaultBlockState(), 2);
                        }
                    }
                    placedAny = true;

                    // Chorus plant chance (authentic End vegetation)
                    if (random.nextInt(25) == 0 && world.isEmptyBlock(surfacePos.above())) {
                        generateChorusPlant(world, surfacePos.above(), random);
                    } else if (random.nextInt(60) == 0 && world.isEmptyBlock(surfacePos.above())) {
                        // Small obsidian / purpur monument spire
                        int spireHeight = 2 + random.nextInt(3);
                        for (int s = 0; s < spireHeight; s++) {
                            BlockPos sPos = surfacePos.above(1 + s);
                            if (world.isEmptyBlock(sPos)) {
                                world.setBlock(sPos, (s == spireHeight - 1 && random.nextBoolean()) ? Blocks.PURPUR_PILLAR.defaultBlockState() : Blocks.OBSIDIAN.defaultBlockState(), 2);
                            }
                        }
                    }
                }
            }
        }

        // 2. Subterranean Netherrack/End Stone & Ore Veins
        int veinsCount = 10 + random.nextInt(8);
        for (int v = 0; v < veinsCount; v++) {
            int vx = origin.getX() + random.nextInt(16) - 8;
            int vz = origin.getZ() + random.nextInt(16) - 8;
            int vy = -40 + random.nextInt(110); // Between Y = -40 and Y = 70
            BlockPos center = new BlockPos(vx, vy, vz);

            int radius = 2 + random.nextInt(3);
            for (int rx = -radius; rx <= radius; rx++) {
                for (int ry = -radius; ry <= radius; ry++) {
                    for (int rz = -radius; rz <= radius; rz++) {
                        if (rx * rx + ry * ry + rz * rz <= radius * radius + random.nextInt(2)) {
                            BlockPos orePos = center.offset(rx, ry, rz);
                            BlockState cur = world.getBlockState(orePos);
                            if (isSubterraneanStone(cur)) {
                                if (isEnd) {
                                    if (random.nextInt(100) < 15) {
                                        world.setBlock(orePos, Blocks.PURPUR_BLOCK.defaultBlockState(), 2);
                                    } else {
                                        world.setBlock(orePos, Blocks.END_STONE.defaultBlockState(), 2);
                                    }
                                } else {
                                    // 22% chance to place an ore, 78% Netherrack base
                                    if (random.nextInt(100) < 22) {
                                        world.setBlock(orePos, getRandomNetherOre(random), 2);
                                    } else {
                                        world.setBlock(orePos, Blocks.NETHERRACK.defaultBlockState(), 2);
                                    }
                                }
                                placedAny = true;
                            }
                        }
                    }
                }
            }
        }

        return placedAny;
    }

    private boolean isTerrainReplaceable(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) ||
               state.is(Blocks.PODZOL) || state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE) ||
               state.is(Blocks.SAND) || state.is(Blocks.GRAVEL) || state.is(Blocks.MUD);
    }

    private boolean isSubterraneanStone(BlockState state) {
        return state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE) || state.is(Blocks.ANDESITE) ||
               state.is(Blocks.DIORITE) || state.is(Blocks.GRANITE) || state.is(Blocks.TUFF);
    }

    private BlockState getRandomNetherOre(RandomSource random) {
        int roll = random.nextInt(130);
        if (roll < 22) return ModBlocks.NETHER_IRON_ORE.defaultBlockState();
        if (roll < 44) return ModBlocks.NETHER_COAL_ORE.defaultBlockState();
        if (roll < 62) return ModBlocks.NETHER_COPPER_ORE.defaultBlockState();
        if (roll < 78) return ModBlocks.NETHER_TIN_ORE.defaultBlockState();
        if (roll < 92) return ModBlocks.NETHER_REDSTONE_ORE.defaultBlockState();
        if (roll < 103) return ModBlocks.NETHER_LAPIS_ORE.defaultBlockState();
        if (roll < 108) return ModBlocks.NETHER_DIAMOND_ORE.defaultBlockState();
        if (roll < 117) return Blocks.NETHER_GOLD_ORE.defaultBlockState();
        if (roll < 125) return Blocks.NETHER_QUARTZ_ORE.defaultBlockState();
        return ModBlocks.NETHER_TUNGSTEN_ORE.defaultBlockState();
    }

    private void generateHugeWarpedFungus(WorldGenLevel world, BlockPos groundPos, RandomSource random) {
        int height = 5 + random.nextInt(6);

        // Trunk
        for (int y = 0; y < height; y++) {
            BlockPos stemPos = groundPos.above(y);
            if (world.isEmptyBlock(stemPos) || world.getBlockState(stemPos).is(Blocks.WARPED_WART_BLOCK)) {
                world.setBlock(stemPos, Blocks.WARPED_STEM.defaultBlockState(), 2);
            }
        }

        // Canopy
        BlockPos top = groundPos.above(height);
        int capRadius = 2 + random.nextInt(2);
        for (int dx = -capRadius; dx <= capRadius; dx++) {
            for (int dz = -capRadius; dz <= capRadius; dz++) {
                for (int dy = -2; dy <= 1; dy++) {
                    int distSq = dx * dx + dz * dz;
                    if (distSq <= capRadius * capRadius) {
                        BlockPos leafPos = top.offset(dx, dy, dz);
                        if (world.isEmptyBlock(leafPos)) {
                            // Scattered shroomlights inside the canopy
                            if (distSq <= 2 && random.nextInt(5) == 0) {
                                world.setBlock(leafPos, Blocks.SHROOMLIGHT.defaultBlockState(), 2);
                            } else {
                                world.setBlock(leafPos, Blocks.WARPED_WART_BLOCK.defaultBlockState(), 2);
                            }
                        }
                    }
                }
            }
        }
    }

    private void generateHugeCrimsonFungus(WorldGenLevel world, BlockPos groundPos, RandomSource random) {
        int height = 5 + random.nextInt(6);

        // Trunk
        for (int y = 0; y < height; y++) {
            BlockPos stemPos = groundPos.above(y);
            if (world.isEmptyBlock(stemPos) || world.getBlockState(stemPos).is(Blocks.NETHER_WART_BLOCK)) {
                world.setBlock(stemPos, Blocks.CRIMSON_STEM.defaultBlockState(), 2);
            }
        }

        // Canopy
        BlockPos top = groundPos.above(height);
        int capRadius = 2 + random.nextInt(2);
        for (int dx = -capRadius; dx <= capRadius; dx++) {
            for (int dz = -capRadius; dz <= capRadius; dz++) {
                for (int dy = -2; dy <= 1; dy++) {
                    int distSq = dx * dx + dz * dz;
                    if (distSq <= capRadius * capRadius) {
                        BlockPos leafPos = top.offset(dx, dy, dz);
                        if (world.isEmptyBlock(leafPos)) {
                            if (distSq <= 2 && random.nextInt(5) == 0) {
                                world.setBlock(leafPos, Blocks.SHROOMLIGHT.defaultBlockState(), 2);
                            } else {
                                world.setBlock(leafPos, Blocks.NETHER_WART_BLOCK.defaultBlockState(), 2);

                                // Weeping vines hanging below canopy rim
                                if (dy == -2 && random.nextInt(4) == 0 && world.isEmptyBlock(leafPos.below())) {
                                    int vineLen = 1 + random.nextInt(3);
                                    for (int v = 1; v <= vineLen; v++) {
                                        BlockPos vinePos = leafPos.below(v);
                                        if (world.isEmptyBlock(vinePos)) {
                                            world.setBlock(vinePos, Blocks.WEEPING_VINES.defaultBlockState(), 2);
                                        } else {
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void generateChorusPlant(WorldGenLevel world, BlockPos pos, RandomSource random) {
        int height = 2 + random.nextInt(4);
        BlockPos current = pos;
        for (int h = 0; h < height; h++) {
            if (world.isEmptyBlock(current)) {
                world.setBlock(current, Blocks.CHORUS_PLANT.defaultBlockState(), 2);
                current = current.above();
            } else {
                break;
            }
        }
        if (world.isEmptyBlock(current)) {
            world.setBlock(current, Blocks.CHORUS_FLOWER.defaultBlockState(), 2);
        }
    }
}
