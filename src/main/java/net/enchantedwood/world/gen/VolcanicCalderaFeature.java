package net.enchantedwood.world.gen;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;
import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.world.dimension.ModDimensions;

public class VolcanicCalderaFeature extends Feature<DefaultFeatureConfig> {

    public VolcanicCalderaFeature(Codec<DefaultFeatureConfig> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();

        // Strictly generate in The Convergence dimension
        if (world.toServerWorld().getRegistryKey() != ModDimensions.CONVERGENCE_WORLD_KEY) {
            return false;
        }

        BlockPos origin = context.getOrigin();
        Random random = context.getRandom();

        // Verify this is Scorched Caldera biome
        var biomeKey = world.getBiome(origin).getKey();
        if (biomeKey.isEmpty() || !biomeKey.get().getValue().equals(Identifier.of(EnchantedWoodMod.MOD_ID, "scorched_caldera"))) {
            return false;
        }

        int originSurfaceY = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, origin.getX(), origin.getZ()) - 1;
        if (originSurfaceY < 50) {
            return false;
        }

        BlockPos.Mutable mutablePos = new BlockPos.Mutable();
        boolean placedAny = false;

        // 1. Transform Surface & Subsurface into Volcanic Strata (16x16 chunk radius)
        for (int dx = -8; dx < 8; dx++) {
            for (int dz = -8; dz < 8; dz++) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;

                mutablePos.set(x, 0, z);
                var colBiome = world.getBiome(mutablePos).getKey();
                if (colBiome.isEmpty() || !colBiome.get().getValue().equals(Identifier.of(EnchantedWoodMod.MOD_ID, "scorched_caldera"))) {
                    continue;
                }

                int topY = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, x, z) - 1;
                if (topY < 50) continue;

                mutablePos.set(x, topY, z);
                BlockState surfaceState = world.getBlockState(mutablePos);

                if (isReplaceableTerrain(surfaceState)) {
                    // Volcanic surface block distribution
                    BlockState volcanicSurface;
                    float roll = random.nextFloat();
                    if (roll < 0.50f) {
                        volcanicSurface = Blocks.BASALT.getDefaultState();
                    } else if (roll < 0.75f) {
                        volcanicSurface = Blocks.BLACKSTONE.getDefaultState();
                    } else if (roll < 0.90f) {
                        volcanicSurface = Blocks.MAGMA_BLOCK.getDefaultState();
                    } else {
                        volcanicSurface = Blocks.SMOOTH_BASALT.getDefaultState();
                    }

                    world.setBlockState(mutablePos, volcanicSurface, 2);
                    placedAny = true;

                    // Subsurface volcanic rock layers (2 to 5 blocks deep)
                    int depth = 2 + random.nextInt(4);
                    for (int d = 1; d <= depth; d++) {
                        BlockPos under = mutablePos.down(d);
                        BlockState underState = world.getBlockState(under);
                        if (isReplaceableTerrain(underState)) {
                            BlockState subState = (random.nextFloat() < 0.65f)
                                    ? Blocks.BLACKSTONE.getDefaultState()
                                    : (random.nextBoolean() ? Blocks.BASALT.getDefaultState() : Blocks.NETHERRACK.getDefaultState());
                            world.setBlockState(under, subState, 2);
                        }
                    }

                    // Basalt columns & Spires protruding on the surface
                    if (volcanicSurface.isOf(Blocks.BASALT) && random.nextInt(18) == 0 && world.isAir(mutablePos.up())) {
                        int spireHeight = 2 + random.nextInt(4);
                        for (int h = 1; h <= spireHeight; h++) {
                            BlockPos spirePos = mutablePos.up(h);
                            if (world.isAir(spirePos)) {
                                world.setBlockState(spirePos, Blocks.BASALT.getDefaultState(), 2);
                            } else {
                                break;
                            }
                        }
                    }

                    // Magma block ambient fire
                    if (volcanicSurface.isOf(Blocks.MAGMA_BLOCK) && random.nextInt(8) == 0 && world.isAir(mutablePos.up())) {
                        world.setBlockState(mutablePos.up(), Blocks.FIRE.getDefaultState(), 2);
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

    private void generateCalderaCrater(StructureWorldAccess world, BlockPos origin, int centerSurfaceY, Random random) {
        int craterRadius = 6 + random.nextInt(6); // 6 to 11 block radius crater bowl
        int craterDepth = 4 + random.nextInt(3);  // 4 to 6 blocks deep into the mountain
        int rimTopY = centerSurfaceY + 1;
        int lavaLevelY = centerSurfaceY - (craterDepth / 2);

        BlockPos.Mutable mutablePos = new BlockPos.Mutable();

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
                            world.setBlockState(mutablePos, Blocks.AIR.getDefaultState(), 2);
                        }
                    }

                    // Fill lava lake in the bottom
                    for (int y = lavaLevelY; y >= bowlBottomY; y--) {
                        mutablePos.set(x, y, z);
                        world.setBlockState(mutablePos, Blocks.LAVA.getDefaultState(), 2);
                    }

                    // Magma & Obsidian lining at the crater floor
                    mutablePos.set(x, bowlBottomY - 1, z);
                    BlockState floorState = (random.nextFloat() < 0.60f)
                            ? Blocks.MAGMA_BLOCK.getDefaultState()
                            : Blocks.OBSIDIAN.getDefaultState();
                    world.setBlockState(mutablePos, floorState, 2);
                    mutablePos.set(x, bowlBottomY - 2, z);
                    world.setBlockState(mutablePos, Blocks.BLACKSTONE.getDefaultState(), 2);

                } else if (distSq <= (craterRadius + 2) * (craterRadius + 2)) {
                    // Rim ridge: build up basalt and blackstone rim
                    int currentTop = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, x, z) - 1;
                    if (currentTop < rimTopY) {
                        for (int y = currentTop + 1; y <= rimTopY; y++) {
                            mutablePos.set(x, y, z);
                            BlockState rimState = (random.nextFloat() < 0.7f)
                                    ? Blocks.BASALT.getDefaultState()
                                    : Blocks.BLACKSTONE.getDefaultState();
                            world.setBlockState(mutablePos, rimState, 2);
                        }
                    }

                    // Rim volcanic spires
                    if (random.nextInt(6) == 0) {
                        int spireHeight = 2 + random.nextInt(4);
                        for (int h = 1; h <= spireHeight; h++) {
                            mutablePos.set(x, rimTopY + h, z);
                            if (world.isAir(mutablePos)) {
                                world.setBlockState(mutablePos, Blocks.BASALT.getDefaultState(), 2);
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
                world.setBlockState(mutablePos, (y == lavaLevelY) ? Blocks.LAVA.getDefaultState() : Blocks.AIR.getDefaultState(), 2);
            }
        }
    }

    private boolean isReplaceableTerrain(BlockState state) {
        if (state.isAir() || state.isOf(Blocks.BEDROCK) || state.isOf(Blocks.BARRIER)) {
            return false;
        }
        return state.isIn(BlockTags.DIRT) ||
                state.isIn(BlockTags.BASE_STONE_OVERWORLD) ||
                state.isIn(BlockTags.TERRACOTTA) ||
                state.isIn(BlockTags.SAND) ||
                state.isOf(Blocks.GRAVEL) ||
                state.isOf(Blocks.STONE) ||
                state.isOf(Blocks.COBBLESTONE) ||
                state.isOf(Blocks.ANDESITE) ||
                state.isOf(Blocks.DIORITE) ||
                state.isOf(Blocks.GRANITE) ||
                state.isOf(Blocks.TUFF) ||
                state.isOf(Blocks.DEEPSLATE) ||
                state.isOf(Blocks.SANDSTONE) ||
                state.isOf(Blocks.RED_SANDSTONE);
    }
}
