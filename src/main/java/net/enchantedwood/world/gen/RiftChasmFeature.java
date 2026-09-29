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

        // Chasm parameters - dramatic natural fissure
        int chasmLength = 22 + random.nextInt(14); // 22 to 36 blocks long
        double angle = random.nextDouble() * Math.PI; // orientation angle
        double dx = Math.cos(angle);
        double dz = Math.sin(angle);

        // Plunge deep into the subterranean and deepslate layer (Y = -15 to Y = -40)
        int targetBottomY = Math.max(world.getBottomY() + 16, Math.min(surfaceY - 60, -15 - random.nextInt(25)));

        BlockPos.Mutable mutablePos = new BlockPos.Mutable();

        // 1. Carve the descending elliptical chasm
        for (int y = surfaceY; y >= targetBottomY; y--) {
            float progress = (float) (surfaceY - y) / (float) (surfaceY - targetBottomY); // 0.0 (top) to 1.0 (bottom)

            // Width varies organically: starts moderate, widens in the middle, tapers near bottom
            float baseWidth = 3.2f + MathHelper.sin(progress * (float) Math.PI) * 2.2f;
            float halfLength = (chasmLength * 0.5f) * (1.0f - progress * 0.3f);

            for (float t = -halfLength; t <= halfLength; t += 1.0f) {
                int centerX = (int) (origin.getX() + t * dx);
                int centerZ = (int) (origin.getZ() + t * dz);

                // Serpentine wobble along descent
                int wobbleX = (int) (Math.sin((y * 0.18) + t * 0.12) * 1.8);
                int wobbleZ = (int) (Math.cos((y * 0.18) + t * 0.12) * 1.8);
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

        // Check if we are in Scorched Caldera
        var biomeKey = world.getBiome(surfacePos).getKey();
        boolean isCaldera = biomeKey.isPresent() && biomeKey.get().getValue().equals(Identifier.of("enchantedwood", "scorched_caldera"));

        // 2. Decorate Chasm Interior Walls (Only below the surface rim to preserve natural surface grass!)
        int scanRadius = (chasmLength / 2) + 4;
        for (int x = origin.getX() - scanRadius; x <= origin.getX() + scanRadius; x++) {
            for (int z = origin.getZ() - scanRadius; z <= origin.getZ() + scanRadius; z++) {
                // Strictly below surface rim (surfaceY - 2) so surface grass/trees are NOT turned into stone quarry scars
                for (int y = surfaceY - 2; y >= targetBottomY; y--) {
                    mutablePos.set(x, y, z);
                    BlockState state = world.getBlockState(mutablePos);

                    if (state.isAir()) {
                        for (Direction dir : Direction.values()) {
                            BlockPos neighbor = mutablePos.offset(dir);
                            BlockState neighborState = world.getBlockState(neighbor);

                            if (!neighborState.isAir() && !neighborState.isOf(Blocks.BEDROCK) && !neighborState.isOf(Blocks.WATER) && !neighborState.isOf(Blocks.LAVA)) {
                                // Turn exposed dirt, grass, gravel ON INTERIOR WALLS into rock strata
                                if (neighbor.getY() < surfaceY - 1 && (neighborState.isIn(BlockTags.DIRT) || neighborState.isOf(Blocks.GRAVEL))) {
                                    BlockState rockState = (neighbor.getY() > surfaceY - 8)
                                            ? (isCaldera ? Blocks.BASALT.getDefaultState() : Blocks.STONE.getDefaultState())
                                            : (random.nextBoolean() ? Blocks.ANDESITE.getDefaultState() : Blocks.GRANITE.getDefaultState());
                                    world.setBlockState(neighbor, rockState, 2);
                                }

                                // Natural exposed ore along chasm walls
                                if (random.nextFloat() < 0.02f && (neighborState.isOf(Blocks.STONE) || neighborState.isOf(Blocks.DEEPSLATE) || neighborState.isOf(Blocks.BASALT) || neighborState.isOf(Blocks.BLACKSTONE))) {
                                    BlockState oreState = selectExposedOre(neighbor.getY(), random, isCaldera);
                                    world.setBlockState(neighbor, oreState, 2);
                                }

                                // Stepped ledges for player descent (every 8 blocks vertically)
                                if (dir == Direction.DOWN && y % 8 == 0 && random.nextFloat() < 0.20f) {
                                    world.setBlockState(mutablePos, isCaldera ? Blocks.BLACKSTONE.getDefaultState() : Blocks.COBBLESTONE.getDefaultState(), 2);
                                }

                                // Atmospheric Glow Lichen (sparse)
                                if (!isCaldera && random.nextFloat() < 0.025f && neighborState.isOpaqueFullCube()) {
                                    if (Blocks.GLOW_LICHEN.getDefaultState().canPlaceAt(world, mutablePos)) {
                                        world.setBlockState(mutablePos, Blocks.GLOW_LICHEN.getDefaultState(), 2);
                                    }
                                }

                                // Subtle nether/magma seepage in deep levels
                                if (neighbor.getY() < 10 && random.nextFloat() < 0.025f && (neighborState.isOf(Blocks.STONE) || neighborState.isOf(Blocks.DEEPSLATE))) {
                                    world.setBlockState(neighbor, isCaldera ? Blocks.MAGMA_BLOCK.getDefaultState() : Blocks.NETHERRACK.getDefaultState(), 2);
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Cascading waterfall or lavafall plunging into the abyss
        BlockPos fluidSource = new BlockPos(origin.getX(), surfaceY - 2, origin.getZ());
        if (world.getBlockState(fluidSource).isAir() && world.getBlockState(fluidSource.north()).isOpaqueFullCube()) {
            world.setBlockState(fluidSource, isCaldera ? Blocks.LAVA.getDefaultState() : Blocks.WATER.getDefaultState(), 2);
        }

        // 4. Interconnecting Subterranean Cave Branches:
        // Carve 3 to 4 winding horizontal tunnels from the chasm floor and mid-terrace
        // that extend 35 to 60 blocks outwards to connect directly into surrounding cave networks, mineshafts, and deep dark!
        int branchCount = 3 + random.nextInt(2);
        for (int b = 0; b < branchCount; b++) {
            int branchY = (b % 2 == 0)
                    ? targetBottomY + 2 + random.nextInt(4)
                    : (surfaceY + targetBottomY) / 2 + random.nextInt(6) - 3;

            double branchAngle = angle + (Math.PI * 0.5) + (b * (Math.PI * 2.0 / branchCount)) + (random.nextDouble() - 0.5) * 0.4;
            float t = (random.nextFloat() - 0.5f) * (chasmLength * 0.5f);
            int startX = (int) (origin.getX() + t * dx);
            int startZ = (int) (origin.getZ() + t * dz);
            BlockPos branchStart = new BlockPos(startX, branchY, startZ);

            int tunnelLength = 35 + random.nextInt(25); // 35 to 60 blocks long
            carveCaveBranch(world, branchStart, branchAngle, tunnelLength, random, isCaldera);
        }

        return true;
    }

    private void carveCaveBranch(StructureWorldAccess world, BlockPos startPos, double initialAngle, int tunnelLength, Random random, boolean isCaldera) {
        double cx = startPos.getX();
        double cy = startPos.getY();
        double cz = startPos.getZ();
        double currentAngle = initialAngle;
        double pitch = (random.nextDouble() - 0.5) * 0.12;

        BlockPos.Mutable mut = new BlockPos.Mutable();

        for (int step = 0; step < tunnelLength; step++) {
            currentAngle += (random.nextDouble() - 0.5) * 0.28;
            pitch += (random.nextDouble() - 0.5) * 0.10;
            pitch = MathHelper.clamp(pitch, -0.25, 0.25);

            cx += Math.cos(currentAngle) * 1.2;
            cy += pitch;
            cz += Math.sin(currentAngle) * 1.2;

            int blockY = (int) cy;
            if (blockY <= world.getBottomY() + 6 || blockY >= world.getTopYInclusive() - 10) {
                break;
            }

            float radiusX = 2.4f + (float) Math.sin(step * 0.25) * 0.6f;
            float radiusY = 2.0f + (float) Math.cos(step * 0.2) * 0.5f;
            int rx = Math.round(radiusX);
            int ry = Math.round(radiusY);

            for (int ox = -rx; ox <= rx; ox++) {
                for (int oy = -ry; oy <= ry; oy++) {
                    for (int oz = -rx; oz <= rx; oz++) {
                        if ((ox * ox) / (radiusX * radiusX) + (oy * oy) / (radiusY * radiusY) + (oz * oz) / (radiusX * radiusX) <= 1.0f) {
                            mut.set((int) cx + ox, blockY + oy, (int) cz + oz);
                            BlockState cur = world.getBlockState(mut);
                            if (!cur.isOf(Blocks.BEDROCK)) {
                                world.setBlockState(mut, Blocks.AIR.getDefaultState(), 2);
                            }
                        }
                    }
                }
            }

            // Occasional exposed ore along the tunnel walls
            if (random.nextFloat() < 0.08f) {
                BlockPos orePos = new BlockPos((int) cx + rx, blockY, (int) cz);
                BlockState s = world.getBlockState(orePos);
                if (s.isOf(Blocks.STONE) || s.isOf(Blocks.DEEPSLATE)) {
                    world.setBlockState(orePos, selectExposedOre(blockY, random, isCaldera), 2);
                }
            }
        }
    }

    private BlockState selectExposedOre(int y, Random random, boolean isCaldera) {
        float roll = random.nextFloat();
        if (isCaldera) {
            if (y < 0) {
                if (roll < 0.35f) return ModBlocks.DEEPSLATE_ZIRCONIA_ORE.getDefaultState();
                if (roll < 0.70f) return ModBlocks.DEEPSLATE_HAFNIUM_ORE.getDefaultState();
                if (roll < 0.85f) return ModBlocks.FLUORITE_ORE.getDefaultState();
                return Blocks.DEEPSLATE_DIAMOND_ORE.getDefaultState();
            } else if (y < 35) {
                if (roll < 0.35f) return ModBlocks.ZIRCONIA_ORE.getDefaultState();
                if (roll < 0.70f) return ModBlocks.HAFNIUM_ORE.getDefaultState();
                if (roll < 0.85f) return ModBlocks.NETHER_IRON_ORE.getDefaultState();
                return ModBlocks.DEEPSLATE_ZIRCONIA_ORE.getDefaultState();
            } else {
                if (roll < 0.40f) return ModBlocks.ZIRCONIA_ORE.getDefaultState();
                if (roll < 0.75f) return ModBlocks.HAFNIUM_ORE.getDefaultState();
                return Blocks.COAL_ORE.getDefaultState();
            }
        }

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
