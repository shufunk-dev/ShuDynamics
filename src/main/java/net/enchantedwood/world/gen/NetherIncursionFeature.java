package net.enchantedwood.world.gen;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;
import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.world.dimension.ModDimensions;

public class NetherIncursionFeature extends Feature<DefaultFeatureConfig> {
    public NetherIncursionFeature(Codec<DefaultFeatureConfig> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();

        // STRICT DIMENSION CHECK: Exclusively generates in The Convergence
        if (world.toServerWorld().getRegistryKey() != ModDimensions.CONVERGENCE_WORLD_KEY) {
            return false;
        }

        BlockPos origin = context.getOrigin();
        Random random = context.getRandom();

        // Check if origin biome is one of the Nether incursion biomes
        RegistryEntry<Biome> biomeEntry = world.getBiome(origin);
        boolean isWarped = biomeEntry.matchesKey(BiomeKeys.WARPED_FOREST);
        boolean isCrimson = biomeEntry.matchesKey(BiomeKeys.CRIMSON_FOREST);
        boolean isSoulSand = biomeEntry.matchesKey(BiomeKeys.SOUL_SAND_VALLEY);
        boolean isBasalt = biomeEntry.matchesKey(BiomeKeys.BASALT_DELTAS);

        if (!isWarped && !isCrimson && !isSoulSand && !isBasalt) {
            return false;
        }

        boolean placedAny = false;

        // 1. Surface and Subsurface Terrain Transformation (16x16 column area)
        for (int dx = -8; dx < 8; dx++) {
            for (int dz = -8; dz < 8; dz++) {
                BlockPos colPos = origin.add(dx, 0, dz);
                RegistryEntry<Biome> colBiome = world.getBiome(colPos);

                boolean colWarped = colBiome.matchesKey(BiomeKeys.WARPED_FOREST);
                boolean colCrimson = colBiome.matchesKey(BiomeKeys.CRIMSON_FOREST);
                boolean colSoulSand = colBiome.matchesKey(BiomeKeys.SOUL_SAND_VALLEY);
                boolean colBasalt = colBiome.matchesKey(BiomeKeys.BASALT_DELTAS);

                if (!colWarped && !colCrimson && !colSoulSand && !colBasalt) {
                    continue;
                }

                // Find highest solid surface block
                int topY = world.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE_WG, colPos.getX(), colPos.getZ()) - 1;
                BlockPos surfacePos = new BlockPos(colPos.getX(), topY, colPos.getZ());

                BlockState surfaceState = world.getBlockState(surfacePos);
                if (!isTerrainReplaceable(surfaceState)) {
                    continue;
                }

                if (colWarped) {
                    // Warped Nylium on top
                    world.setBlockState(surfacePos, Blocks.WARPED_NYLIUM.getDefaultState(), 2);
                    int depth = 3 + random.nextInt(3);
                    for (int d = 1; d <= depth; d++) {
                        BlockPos under = surfacePos.down(d);
                        if (isTerrainReplaceable(world.getBlockState(under))) {
                            world.setBlockState(under, Blocks.NETHERRACK.getDefaultState(), 2);
                        }
                    }
                    placedAny = true;

                    // Huge Warped Fungus Tree Chance (approx 1-3 per chunk)
                    if (random.nextInt(30) == 0 && world.isAir(surfacePos.up())) {
                        generateHugeWarpedFungus(world, surfacePos.up(), random);
                    } else if (random.nextInt(5) == 0 && world.isAir(surfacePos.up())) {
                        // Flora carpet
                        int floraRoll = random.nextInt(10);
                        if (floraRoll < 4) {
                            world.setBlockState(surfacePos.up(), Blocks.WARPED_ROOTS.getDefaultState(), 2);
                        } else if (floraRoll < 7) {
                            world.setBlockState(surfacePos.up(), Blocks.NETHER_SPROUTS.getDefaultState(), 2);
                        } else if (floraRoll < 8) {
                            world.setBlockState(surfacePos.up(), Blocks.WARPED_FUNGUS.getDefaultState(), 2);
                        } else {
                            int vineHeight = 1 + random.nextInt(4);
                            for (int v = 0; v < vineHeight; v++) {
                                BlockPos vinePos = surfacePos.up(1 + v);
                                if (world.isAir(vinePos)) {
                                    world.setBlockState(vinePos, Blocks.TWISTING_VINES.getDefaultState(), 2);
                                } else {
                                    break;
                                }
                            }
                        }
                    }
                } else if (colCrimson) {
                    // Crimson Nylium on top
                    world.setBlockState(surfacePos, Blocks.CRIMSON_NYLIUM.getDefaultState(), 2);
                    int depth = 3 + random.nextInt(3);
                    for (int d = 1; d <= depth; d++) {
                        BlockPos under = surfacePos.down(d);
                        if (isTerrainReplaceable(world.getBlockState(under))) {
                            world.setBlockState(under, Blocks.NETHERRACK.getDefaultState(), 2);
                        }
                    }
                    placedAny = true;

                    // Huge Crimson Fungus Tree Chance
                    if (random.nextInt(30) == 0 && world.isAir(surfacePos.up())) {
                        generateHugeCrimsonFungus(world, surfacePos.up(), random);
                    } else if (random.nextInt(5) == 0 && world.isAir(surfacePos.up())) {
                        int floraRoll = random.nextInt(10);
                        if (floraRoll < 7) {
                            world.setBlockState(surfacePos.up(), Blocks.CRIMSON_ROOTS.getDefaultState(), 2);
                        } else {
                            world.setBlockState(surfacePos.up(), Blocks.CRIMSON_FUNGUS.getDefaultState(), 2);
                        }
                    }
                } else if (colSoulSand) {
                    BlockState soulState = random.nextBoolean() ? Blocks.SOUL_SAND.getDefaultState() : Blocks.SOUL_SOIL.getDefaultState();
                    world.setBlockState(surfacePos, soulState, 2);
                    int depth = 2 + random.nextInt(4);
                    for (int d = 1; d <= depth; d++) {
                        BlockPos under = surfacePos.down(d);
                        if (isTerrainReplaceable(world.getBlockState(under))) {
                            world.setBlockState(under, soulState, 2);
                        }
                    }
                    placedAny = true;
                } else if (colBasalt) {
                    BlockState basaltState = random.nextFloat() < 0.65f ? Blocks.BASALT.getDefaultState() :
                            (random.nextFloat() < 0.5f ? Blocks.BLACKSTONE.getDefaultState() : Blocks.MAGMA_BLOCK.getDefaultState());
                    world.setBlockState(surfacePos, basaltState, 2);
                    int depth = 2 + random.nextInt(3);
                    for (int d = 1; d <= depth; d++) {
                        BlockPos under = surfacePos.down(d);
                        if (isTerrainReplaceable(world.getBlockState(under))) {
                            world.setBlockState(under, Blocks.BLACKSTONE.getDefaultState(), 2);
                        }
                    }
                    placedAny = true;
                }
            }
        }

        // 2. Subterranean Netherrack & Nether Overworld Ore Veins
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
                            BlockPos orePos = center.add(rx, ry, rz);
                            BlockState cur = world.getBlockState(orePos);
                            if (isSubterraneanStone(cur)) {
                                // 22% chance to place an ore, 78% Netherrack base
                                if (random.nextInt(100) < 22) {
                                    world.setBlockState(orePos, getRandomNetherOre(random), 2);
                                } else {
                                    world.setBlockState(orePos, Blocks.NETHERRACK.getDefaultState(), 2);
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
        return state.isOf(Blocks.GRASS_BLOCK) || state.isOf(Blocks.DIRT) || state.isOf(Blocks.COARSE_DIRT) ||
               state.isOf(Blocks.PODZOL) || state.isOf(Blocks.STONE) || state.isOf(Blocks.DEEPSLATE) ||
               state.isOf(Blocks.SAND) || state.isOf(Blocks.GRAVEL) || state.isOf(Blocks.MUD);
    }

    private boolean isSubterraneanStone(BlockState state) {
        return state.isOf(Blocks.STONE) || state.isOf(Blocks.DEEPSLATE) || state.isOf(Blocks.ANDESITE) ||
               state.isOf(Blocks.DIORITE) || state.isOf(Blocks.GRANITE) || state.isOf(Blocks.TUFF);
    }

    private BlockState getRandomNetherOre(Random random) {
        int roll = random.nextInt(130);
        if (roll < 22) return ModBlocks.NETHER_IRON_ORE.getDefaultState();
        if (roll < 44) return ModBlocks.NETHER_COAL_ORE.getDefaultState();
        if (roll < 62) return ModBlocks.NETHER_COPPER_ORE.getDefaultState();
        if (roll < 78) return ModBlocks.NETHER_TIN_ORE.getDefaultState();
        if (roll < 92) return ModBlocks.NETHER_REDSTONE_ORE.getDefaultState();
        if (roll < 103) return ModBlocks.NETHER_LAPIS_ORE.getDefaultState();
        if (roll < 108) return ModBlocks.NETHER_DIAMOND_ORE.getDefaultState();
        if (roll < 117) return Blocks.NETHER_GOLD_ORE.getDefaultState();
        if (roll < 125) return Blocks.NETHER_QUARTZ_ORE.getDefaultState();
        return ModBlocks.NETHER_TUNGSTEN_ORE.getDefaultState();
    }

    private void generateHugeWarpedFungus(StructureWorldAccess world, BlockPos groundPos, Random random) {
        int height = 5 + random.nextInt(6);

        // Trunk
        for (int y = 0; y < height; y++) {
            BlockPos stemPos = groundPos.up(y);
            if (world.isAir(stemPos) || world.getBlockState(stemPos).isOf(Blocks.WARPED_WART_BLOCK)) {
                world.setBlockState(stemPos, Blocks.WARPED_STEM.getDefaultState(), 2);
            }
        }

        // Canopy
        BlockPos top = groundPos.up(height);
        int capRadius = 2 + random.nextInt(2);
        for (int dx = -capRadius; dx <= capRadius; dx++) {
            for (int dz = -capRadius; dz <= capRadius; dz++) {
                for (int dy = -2; dy <= 1; dy++) {
                    int distSq = dx * dx + dz * dz;
                    if (distSq <= capRadius * capRadius) {
                        BlockPos leafPos = top.add(dx, dy, dz);
                        if (world.isAir(leafPos)) {
                            // Scattered shroomlights inside the canopy
                            if (distSq <= 2 && random.nextInt(5) == 0) {
                                world.setBlockState(leafPos, Blocks.SHROOMLIGHT.getDefaultState(), 2);
                            } else {
                                world.setBlockState(leafPos, Blocks.WARPED_WART_BLOCK.getDefaultState(), 2);
                            }
                        }
                    }
                }
            }
        }
    }

    private void generateHugeCrimsonFungus(StructureWorldAccess world, BlockPos groundPos, Random random) {
        int height = 5 + random.nextInt(6);

        // Trunk
        for (int y = 0; y < height; y++) {
            BlockPos stemPos = groundPos.up(y);
            if (world.isAir(stemPos) || world.getBlockState(stemPos).isOf(Blocks.NETHER_WART_BLOCK)) {
                world.setBlockState(stemPos, Blocks.CRIMSON_STEM.getDefaultState(), 2);
            }
        }

        // Canopy
        BlockPos top = groundPos.up(height);
        int capRadius = 2 + random.nextInt(2);
        for (int dx = -capRadius; dx <= capRadius; dx++) {
            for (int dz = -capRadius; dz <= capRadius; dz++) {
                for (int dy = -2; dy <= 1; dy++) {
                    int distSq = dx * dx + dz * dz;
                    if (distSq <= capRadius * capRadius) {
                        BlockPos leafPos = top.add(dx, dy, dz);
                        if (world.isAir(leafPos)) {
                            if (distSq <= 2 && random.nextInt(5) == 0) {
                                world.setBlockState(leafPos, Blocks.SHROOMLIGHT.getDefaultState(), 2);
                            } else {
                                world.setBlockState(leafPos, Blocks.NETHER_WART_BLOCK.getDefaultState(), 2);

                                // Weeping vines hanging below canopy rim
                                if (dy == -2 && random.nextInt(4) == 0 && world.isAir(leafPos.down())) {
                                    int vineLen = 1 + random.nextInt(3);
                                    for (int v = 1; v <= vineLen; v++) {
                                        BlockPos vinePos = leafPos.down(v);
                                        if (world.isAir(vinePos)) {
                                            world.setBlockState(vinePos, Blocks.WEEPING_VINES.getDefaultState(), 2);
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
}
