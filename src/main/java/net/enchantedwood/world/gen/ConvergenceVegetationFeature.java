package net.enchantedwood.world.gen;

import com.mojang.serialization.Codec;
import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.world.dimension.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
public class ConvergenceVegetationFeature implements Feature {
    public static final com.mojang.serialization.MapCodec<ConvergenceVegetationFeature> CODEC = com.mojang.serialization.MapCodec.unit(ConvergenceVegetationFeature::new);

    public ConvergenceVegetationFeature() {
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

        
        
        boolean placedAny = false;

        // 1. Generate Wild Rice patches
        for (int i = 0; i < 16; i++) {
            int dx = random.nextInt(8) - random.nextInt(8);
            int dz = random.nextInt(8) - random.nextInt(8);
            int dy = random.nextInt(4) - random.nextInt(4);
            BlockPos target = origin.offset(dx, dy, dz);

            if (world.isEmptyBlock(target) && ModBlocks.WILD_RICE.defaultBlockState().canSurvive(world, target)) {
                world.setBlock(target, ModBlocks.WILD_RICE.defaultBlockState(), 2);
                placedAny = true;
            }
        }

        // 2. Generate Wild Cucumber shrubs
        for (int i = 0; i < 12; i++) {
            int dx = random.nextInt(10) - random.nextInt(10);
            int dz = random.nextInt(10) - random.nextInt(10);
            int dy = random.nextInt(4) - random.nextInt(4);
            BlockPos target = origin.offset(dx, dy, dz);

            if (world.isEmptyBlock(target) && ModBlocks.WILD_CUCUMBER.defaultBlockState().canSurvive(world, target)) {
                world.setBlock(target, ModBlocks.WILD_CUCUMBER.defaultBlockState(), 2);
                placedAny = true;
            }
        }

        // 3. Generate Wild Wasabi along water banks & damp ground
        for (int i = 0; i < 10; i++) {
            int dx = random.nextInt(8) - random.nextInt(8);
            int dz = random.nextInt(8) - random.nextInt(8);
            int dy = random.nextInt(4) - random.nextInt(4);
            BlockPos target = origin.offset(dx, dy, dz);

            if (world.isEmptyBlock(target) && ModBlocks.WILD_WASABI.defaultBlockState().canSurvive(world, target)) {
                world.setBlock(target, ModBlocks.WILD_WASABI.defaultBlockState(), 2);
                placedAny = true;
            }
        }

        // 4. Generate Wild Dragon Fruit Cacti
        for (int i = 0; i < 8; i++) {
            int dx = random.nextInt(10) - random.nextInt(10);
            int dz = random.nextInt(10) - random.nextInt(10);
            int dy = random.nextInt(4) - random.nextInt(4);
            BlockPos target = origin.offset(dx, dy, dz);

            if (world.isEmptyBlock(target) && ModBlocks.WILD_DRAGON_FRUIT.defaultBlockState().canSurvive(world, target)) {
                world.setBlock(target, ModBlocks.WILD_DRAGON_FRUIT.defaultBlockState(), 2);
                placedAny = true;
            }
        }

        // 5. Chance to generate an Avocado Tree or Starfruit Tree
        float treeRoll = random.nextFloat();
        if (treeRoll < 0.35f) {
            generateAvocadoTree(world, origin, random);
            placedAny = true;
        } else if (treeRoll < 0.65f) {
            generateStarfruitTree(world, origin, random);
            placedAny = true;
        }

        return placedAny;
    }

    private void generateStarfruitTree(WorldGenLevel world, BlockPos pos, RandomSource random) {
        BlockPos surface = pos;
        while (surface.getY() > world.getMinY() && world.isEmptyBlock(surface)) {
            surface = surface.below();
        }
        BlockPos trunkBase = surface.above();
        BlockState ground = world.getBlockState(surface);
        if (!ground.is(Blocks.GRASS_BLOCK) && !ground.is(Blocks.DIRT) && !ground.is(ModBlocks.VOLCANIC_SOIL)) {
            return;
        }

        int height = 4 + random.nextInt(3);

        for (int y = 0; y < height; y++) {
            BlockPos logPos = trunkBase.above(y);
            if (world.isEmptyBlock(logPos) || world.getBlockState(logPos).is(ModBlocks.STARFRUIT_LEAVES)) {
                world.setBlock(logPos, ModBlocks.STARFRUIT_LOG.defaultBlockState(), 2);
            }
        }

        BlockPos canopyCenter = trunkBase.above(height);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -2; dy <= 1; dy++) {
                    if (Math.abs(dx) == 2 && Math.abs(dz) == 2 && (dy == 1 || random.nextBoolean())) {
                        continue;
                    }
                    BlockPos leafPos = canopyCenter.offset(dx, dy, dz);
                    if (world.isEmptyBlock(leafPos)) {
                        world.setBlock(leafPos, ModBlocks.STARFRUIT_LEAVES.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    private void generateAvocadoTree(WorldGenLevel world, BlockPos pos, RandomSource random) {
        BlockPos surface = pos;
        while (surface.getY() > world.getMinY() && world.isEmptyBlock(surface)) {
            surface = surface.below();
        }
        BlockPos trunkBase = surface.above();
        BlockState ground = world.getBlockState(surface);
        if (!ground.is(Blocks.GRASS_BLOCK) && !ground.is(Blocks.DIRT) && !ground.is(ModBlocks.VOLCANIC_SOIL)) {
            return;
        }

        int height = 4 + random.nextInt(3);

        // Trunk
        for (int y = 0; y < height; y++) {
            BlockPos logPos = trunkBase.above(y);
            if (world.isEmptyBlock(logPos) || world.getBlockState(logPos).is(ModBlocks.AVOCADO_LEAVES)) {
                world.setBlock(logPos, ModBlocks.AVOCADO_LOG.defaultBlockState(), 2);
            }
        }

        // Canopy
        BlockPos canopyCenter = trunkBase.above(height);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -2; dy <= 1; dy++) {
                    if (Math.abs(dx) == 2 && Math.abs(dz) == 2 && (dy == 1 || random.nextBoolean())) {
                        continue;
                    }
                    BlockPos leafPos = canopyCenter.offset(dx, dy, dz);
                    if (world.isEmptyBlock(leafPos)) {
                        world.setBlock(leafPos, ModBlocks.AVOCADO_LEAVES.defaultBlockState(), 2);
                    }
                }
            }
        }
    }
}
