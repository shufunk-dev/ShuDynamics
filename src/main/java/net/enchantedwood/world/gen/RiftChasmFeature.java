package net.enchantedwood.world.gen;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;
import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.world.dimension.ModDimensions;

public class RiftChasmFeature extends Feature<DefaultFeatureConfig> {

    public RiftChasmFeature(Codec<DefaultFeatureConfig> configCodec) {
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

        int surfaceY = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, origin.getX(), origin.getZ());
        if (surfaceY < 55 || surfaceY > 160) {
            return false;
        }

        BlockPos surfacePos = new BlockPos(origin.getX(), surfaceY, origin.getZ());
        BlockState surfaceState = world.getBlockState(surfacePos.down());
        if (surfaceState.isAir() || surfaceState.isOf(Blocks.WATER) || surfaceState.isOf(Blocks.LAVA)) {
            return false;
        }

        // Chasm parameters
        int chasmLength = 16 + random.nextInt(12); // 16 to 28 blocks long
        int chasmDepth = 28 + random.nextInt(18);  // 28 to 45 blocks deep
        double angle = random.nextDouble() * Math.PI; // orientation angle
        double dx = Math.cos(angle);
        double dz = Math.sin(angle);

        int targetBottomY = Math.max(world.getBottomY() + 10, surfaceY - chasmDepth);

        // Track bounding box of carved air to decorate walls afterwards
        BlockPos.Mutable mutablePos = new BlockPos.Mutable();

        // 1. Carve the descending elliptical chasm
        for (int y = surfaceY; y >= targetBottomY; y--) {
            float progress = (float) (surfaceY - y) / (float) (surfaceY - targetBottomY); // 0.0 (top) to 1.0 (bottom)

            // Width varies organically: starts moderate, widens slightly in middle, narrows or opens into caves
            float baseWidth = 3.5f + MathHelper.sin(progress * (float) Math.PI) * 2.0f;
            float halfLength = (chasmLength * 0.5f) * (1.0f - progress * 0.25f);

            for (float t = -halfLength; t <= halfLength; t += 1.0f) {
                int centerX = (int) (origin.getX() + t * dx);
                int centerZ = (int) (origin.getZ() + t * dz);

                // Add slight wandering serpentine wobble
                int wobbleX = (int) (Math.sin((y * 0.2) + t * 0.1) * 1.5);
                int wobbleZ = (int) (Math.cos((y * 0.2) + t * 0.1) * 1.5);
                centerX += wobbleX;
                centerZ += wobbleZ;

                int radius = Math.round(baseWidth + (random.nextFloat() - 0.5f));
                for (int ox = -radius; ox <= radius; ox++) {
                    for (int oz = -radius; oz <= radius; oz++) {
                        if (ox * ox + oz * oz <= radius * radius) {
                            mutablePos.set(centerX + ox, y, centerZ + oz);
                            BlockState current = world.getBlockState(mutablePos);

                            // Do not carve through bedrock
                            if (current.isOf(Blocks.BEDROCK)) continue;

                            // Carve to air
                            world.setBlockState(mutablePos, Blocks.AIR.getDefaultState(), 2);
                        }
                    }
                }
            }
        }

        // 2. Decorate Chasm Walls (Exposed Stone, Ledges, Glow Lichen, Cascading Water & Ores)
        int scanRadius = (chasmLength / 2) + 6;
        for (int x = origin.getX() - scanRadius; x <= origin.getX() + scanRadius; x++) {
            for (int z = origin.getZ() - scanRadius; z <= origin.getZ() + scanRadius; z++) {
                for (int y = surfaceY + 2; y >= targetBottomY; y--) {
                    mutablePos.set(x, y, z);
                    BlockState state = world.getBlockState(mutablePos);

                    if (state.isAir()) {
                        // Check neighbors to see if this air block is adjacent to a wall
                        for (Direction dir : Direction.values()) {
                            BlockPos neighbor = mutablePos.offset(dir);
                            BlockState neighborState = world.getBlockState(neighbor);

                            if (!neighborState.isAir() && !neighborState.isOf(Blocks.BEDROCK) && !neighborState.isOf(Blocks.WATER)) {
                                // Turn exposed dirt, grass, gravel into solid rock strata
                                if (neighborState.isIn(BlockTags.DIRT) || neighborState.isOf(Blocks.GRAVEL)) {
                                    BlockState rockState = (neighbor.getY() > surfaceY - 6)
                                            ? Blocks.STONE.getDefaultState()
                                            : (random.nextBoolean() ? Blocks.ANDESITE.getDefaultState() : Blocks.GRANITE.getDefaultState());
                                    world.setBlockState(neighbor, rockState, 2);
                                }

                                // Very rare natural exposed ore (0.3% chance, realistic like vanilla ravines)
                                if (random.nextFloat() < 0.003f && neighborState.isOf(Blocks.STONE)) {
                                    BlockState oreState = selectExposedOre(neighbor.getY(), random);
                                    world.setBlockState(neighbor, oreState, 2);
                                }

                                // Stepped ledges for player descent (every 8 blocks vertically, subtle)
                                if (dir == Direction.DOWN && y % 8 == 0 && random.nextFloat() < 0.15f) {
                                    world.setBlockState(mutablePos, Blocks.COBBLESTONE.getDefaultState(), 2);
                                }

                                // Atmospheric Glow Lichen (sparse)
                                if (random.nextFloat() < 0.02f && neighborState.isOpaqueFullCube()) {
                                    if (Blocks.GLOW_LICHEN.getDefaultState().canPlaceAt(world, mutablePos)) {
                                        world.setBlockState(mutablePos, Blocks.GLOW_LICHEN.getDefaultState(), 2);
                                    }
                                }

                                // Subtle nether seepage only at the absolute bottom
                                if (neighbor.getY() < 20 && random.nextFloat() < 0.01f && neighborState.isOf(Blocks.STONE)) {
                                    world.setBlockState(neighbor, Blocks.NETHERRACK.getDefaultState(), 2);
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Add a scenic waterfall cascading into the chasm from a rim ledge
        BlockPos waterSource = new BlockPos(origin.getX(), surfaceY - 2, origin.getZ());
        if (world.getBlockState(waterSource).isAir() && world.getBlockState(waterSource.north()).isOpaqueFullCube()) {
            world.setBlockState(waterSource, Blocks.WATER.getDefaultState(), 2);
        }

        return true;
    }

    private BlockState selectExposedOre(int y, Random random) {
        float roll = random.nextFloat();
        if (y < 0) {
            // Deepslate level
            if (roll < 0.20f) return Blocks.DEEPSLATE_IRON_ORE.getDefaultState();
            if (roll < 0.40f) return Blocks.DEEPSLATE_COPPER_ORE.getDefaultState();
            if (roll < 0.55f) return Blocks.DEEPSLATE_REDSTONE_ORE.getDefaultState();
            if (roll < 0.70f) return ModBlocks.DEEPSLATE_TUNGSTEN_ORE.getDefaultState();
            if (roll < 0.85f) return ModBlocks.FLUORITE_ORE.getDefaultState();
            return Blocks.DEEPSLATE_DIAMOND_ORE.getDefaultState();
        } else if (y < 35) {
            // Mid subterranean level - rich mix including Nether incursion ores
            if (roll < 0.20f) return ModBlocks.NETHER_IRON_ORE.getDefaultState();
            if (roll < 0.40f) return ModBlocks.NETHER_COPPER_ORE.getDefaultState();
            if (roll < 0.55f) return ModBlocks.NETHER_COAL_ORE.getDefaultState();
            if (roll < 0.70f) return ModBlocks.TIN_ORE.getDefaultState();
            if (roll < 0.85f) return Blocks.IRON_ORE.getDefaultState();
            return ModBlocks.NETHER_LAPIS_ORE.getDefaultState();
        } else {
            // Upper cliff level
            if (roll < 0.40f) return Blocks.COAL_ORE.getDefaultState();
            if (roll < 0.70f) return Blocks.COPPER_ORE.getDefaultState();
            if (roll < 0.90f) return Blocks.IRON_ORE.getDefaultState();
            return ModBlocks.TIN_ORE.getDefaultState();
        }
    }
}
