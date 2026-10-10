package net.enchantedwood.world.gen;

import com.mojang.serialization.Codec;
import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.world.dimension.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
public class VolcanicCalderaFeature implements Feature {
    public static final com.mojang.serialization.MapCodec<VolcanicCalderaFeature> CODEC = com.mojang.serialization.MapCodec.unit(VolcanicCalderaFeature::new);

    public VolcanicCalderaFeature() {
    }

    @Override
    public com.mojang.serialization.MapCodec<? extends Feature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(net.minecraft.world.level.WorldGenLevel world, net.minecraft.world.level.chunk.ChunkGenerator generator, net.minecraft.util.RandomSource random, net.minecraft.core.BlockPos origin) {
        

        // Strictly generate in The Convergence dimension
        if (world.getLevel().dimension() != ModDimensions.CONVERGENCE_WORLD_KEY) {
            return false;
        }

        
        

        // Verify this is Scorched Caldera biome
        var biomeKey = world.getBiome(origin).unwrapKey();
        if (biomeKey.isEmpty() || !biomeKey.get().identifier().equals(Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "scorched_caldera"))) {
            return false;
        }

        int originSurfaceY = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, origin.getX(), origin.getZ()) - 1;
        if (originSurfaceY < 50) {
            return false;
        }

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        boolean placedAny = false;

        // 1. Transform Surface & Subsurface into Volcanic Strata (16x16 chunk radius)
        for (int dx = -8; dx < 8; dx++) {
            for (int dz = -8; dz < 8; dz++) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;

                mutablePos.set(x, 0, z);
                var colBiome = world.getBiome(mutablePos).unwrapKey();
                if (colBiome.isEmpty() || !colBiome.get().identifier().equals(Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "scorched_caldera"))) {
                    continue;
                }

                int topY = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                if (topY < 50) continue;

                mutablePos.set(x, topY, z);
                BlockState surfaceState = world.getBlockState(mutablePos);

                if (isReplaceableTerrain(surfaceState)) {
                    // Volcanic surface block distribution
                    BlockState volcanicSurface;
                    float roll = random.nextFloat();
                    if (roll < 0.50f) {
                        volcanicSurface = Blocks.BASALT.defaultBlockState();
                    } else if (roll < 0.75f) {
                        volcanicSurface = Blocks.BLACKSTONE.defaultBlockState();
                    } else if (roll < 0.90f) {
                        volcanicSurface = Blocks.MAGMA_BLOCK.defaultBlockState();
                    } else {
                        volcanicSurface = Blocks.SMOOTH_BASALT.defaultBlockState();
                    }

                    world.setBlock(mutablePos, volcanicSurface, 2);
                    placedAny = true;

                    // Subsurface volcanic rock layers (2 to 5 blocks deep)
                    int depth = 2 + random.nextInt(4);
                    for (int d = 1; d <= depth; d++) {
                        BlockPos under = mutablePos.below(d);
                        BlockState underState = world.getBlockState(under);
                        if (isReplaceableTerrain(underState)) {
                            BlockState subState = (random.nextFloat() < 0.65f)
                                    ? Blocks.BLACKSTONE.defaultBlockState()
                                    : (random.nextBoolean() ? Blocks.BASALT.defaultBlockState() : Blocks.NETHERRACK.defaultBlockState());
                            world.setBlock(under, subState, 2);
                        }
                    }

                    // Basalt columns & Spires protruding on the surface
                    if (volcanicSurface.is(Blocks.BASALT) && random.nextInt(18) == 0 && world.isEmptyBlock(mutablePos.above())) {
                        int spireHeight = 2 + random.nextInt(4);
                        for (int h = 1; h <= spireHeight; h++) {
                            BlockPos spirePos = mutablePos.above(h);
                            if (world.isEmptyBlock(spirePos)) {
                                world.setBlock(spirePos, Blocks.BASALT.defaultBlockState(), 2);
                            } else {
                                break;
                            }
                        }
                    }

                    // Magma block ambient fire
                    if (volcanicSurface.is(Blocks.MAGMA_BLOCK) && random.nextInt(8) == 0 && world.isEmptyBlock(mutablePos.above())) {
                        world.setBlock(mutablePos.above(), Blocks.FIRE.defaultBlockState(), 2);
                    }
                }
            }
        }

        // 2. Active Volcanic Caldera Crater & Lava Lake Generation
        // Generates on high crests/peaks (Y >= 90) or with high probability in peak zones
        if (originSurfaceY >= 90 && random.nextInt(3) == 0) {
            generateCalderaCrater(world, origin, originSurfaceY, random);
        }

        return placedAny;
    }

    private void generateCalderaCrater(WorldGenLevel world, BlockPos origin, int centerSurfaceY, RandomSource random) {
        int craterRadius = 6 + random.nextInt(6); // 6 to 11 block radius crater bowl
        int craterDepth = 4 + random.nextInt(3);  // 4 to 6 blocks deep into the mountain
        int rimTopY = centerSurfaceY + 1;
        int lavaLevelY = centerSurfaceY - (craterDepth / 2);

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        // 1. Carve circular bowl & build fortified basalt/magma rim
        for (int rx = -craterRadius - 2; rx <= craterRadius + 2; rx++) {
            for (int rz = -craterRadius - 2; rz <= craterRadius + 2; rz++) {
                double distSq = rx * rx + rz * rz;
                double maxDistSq = craterRadius * craterRadius;
                int x = origin.getX() + rx;
                int z = origin.getZ() + rz;

                if (distSq <= maxDistSq) {
                    // Inside the crater bowl
                    double normDist = Math.sqrt(distSq) / craterRadius; // 0.0 at center, 1.0 at edge
                    int bowlBottomY = centerSurfaceY - (int) ((1.0 - normDist * 0.5) * craterDepth);

                    // Carve air above lava level
                    for (int y = rimTopY + 3; y > lavaLevelY; y--) {
                        mutablePos.set(x, y, z);
                        if (!world.getBlockState(mutablePos).isAir()) {
                            world.setBlock(mutablePos, Blocks.AIR.defaultBlockState(), 2);
                        }
                    }

                    // Fill lava lake in the bottom
                    for (int y = lavaLevelY; y >= bowlBottomY; y--) {
                        mutablePos.set(x, y, z);
                        world.setBlock(mutablePos, Blocks.LAVA.defaultBlockState(), 2);
                    }

                    // Magma & Obsidian lining at the crater floor
                    mutablePos.set(x, bowlBottomY - 1, z);
                    BlockState floorState = (random.nextFloat() < 0.60f)
                            ? Blocks.MAGMA_BLOCK.defaultBlockState()
                            : Blocks.OBSIDIAN.defaultBlockState();
                    world.setBlock(mutablePos, floorState, 2);
                    mutablePos.set(x, bowlBottomY - 2, z);
                    world.setBlock(mutablePos, Blocks.BLACKSTONE.defaultBlockState(), 2);

                } else if (distSq <= (craterRadius + 2) * (craterRadius + 2)) {
                    // Rim ridge: build up basalt and blackstone rim
                    int currentTop = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                    if (currentTop < rimTopY) {
                        for (int y = currentTop + 1; y <= rimTopY; y++) {
                            mutablePos.set(x, y, z);
                            BlockState rimState = (random.nextFloat() < 0.7f)
                                    ? Blocks.BASALT.defaultBlockState()
                                    : Blocks.BLACKSTONE.defaultBlockState();
                            world.setBlock(mutablePos, rimState, 2);
                        }
                    }

                    // Rim volcanic spires
                    if (random.nextInt(6) == 0) {
                        int spireHeight = 2 + random.nextInt(4);
                        for (int h = 1; h <= spireHeight; h++) {
                            mutablePos.set(x, rimTopY + h, z);
                            if (world.isEmptyBlock(mutablePos)) {
                                world.setBlock(mutablePos, Blocks.BASALT.defaultBlockState(), 2);
                            }
                        }
                    }
                }
            }
        }

        // 2. Create 1 to 2 Lava Overflow Breaches (cascading lava flows down mountain flanks)
        int breachCount = 1 + random.nextInt(2);
        for (int i = 0; i < breachCount; i++) {
            double angle = random.nextDouble() * 2.0 * Math.PI;
            int breachX = origin.getX() + (int) ((craterRadius + 1) * Math.cos(angle));
            int breachZ = origin.getZ() + (int) ((craterRadius + 1) * Math.sin(angle));

            // Cut a channel through the rim and place an active lava source
            for (int y = rimTopY + 1; y >= lavaLevelY; y--) {
                mutablePos.set(breachX, y, breachZ);
                world.setBlock(mutablePos, (y == lavaLevelY) ? Blocks.LAVA.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }

    private boolean isReplaceableTerrain(BlockState state) {
        if (state.isAir() || state.is(Blocks.BEDROCK) || state.is(Blocks.BARRIER)) {
            return false;
        }
        return state.is(BlockTags.DIRT) ||
                state.is(BlockTags.BASE_STONE_OVERWORLD) ||
                state.is(BlockTags.TERRACOTTA) ||
                state.is(BlockTags.SAND) ||
                state.is(Blocks.GRAVEL) ||
                state.is(Blocks.STONE) ||
                state.is(Blocks.COBBLESTONE) ||
                state.is(Blocks.ANDESITE) ||
                state.is(Blocks.DIORITE) ||
                state.is(Blocks.GRANITE) ||
                state.is(Blocks.TUFF) ||
                state.is(Blocks.DEEPSLATE) ||
                state.is(Blocks.SANDSTONE) ||
                state.is(Blocks.RED_SANDSTONE);
    }
}
