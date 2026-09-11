package net.enchantedwood.world.gen;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;
import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.world.dimension.ModDimensions;

public class ConvergenceVegetationFeature extends Feature<DefaultFeatureConfig> {
    public ConvergenceVegetationFeature(Codec<DefaultFeatureConfig> configCodec) {
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
        boolean placedAny = false;

        // 1. Generate Wild Rice patches
        for (int i = 0; i < 16; i++) {
            int dx = random.nextInt(8) - random.nextInt(8);
            int dz = random.nextInt(8) - random.nextInt(8);
            int dy = random.nextInt(4) - random.nextInt(4);
            BlockPos target = origin.add(dx, dy, dz);

            if (world.isAir(target) && ModBlocks.WILD_RICE.getDefaultState().canPlaceAt(world, target)) {
                world.setBlockState(target, ModBlocks.WILD_RICE.getDefaultState(), 2);
                placedAny = true;
            }
        }

        // 2. Generate Wild Cucumber shrubs
        for (int i = 0; i < 12; i++) {
            int dx = random.nextInt(10) - random.nextInt(10);
            int dz = random.nextInt(10) - random.nextInt(10);
            int dy = random.nextInt(4) - random.nextInt(4);
            BlockPos target = origin.add(dx, dy, dz);

            if (world.isAir(target) && ModBlocks.WILD_CUCUMBER.getDefaultState().canPlaceAt(world, target)) {
                world.setBlockState(target, ModBlocks.WILD_CUCUMBER.getDefaultState(), 2);
                placedAny = true;
            }
        }

        // 3. Generate Wild Wasabi along water banks & damp ground
        for (int i = 0; i < 10; i++) {
            int dx = random.nextInt(8) - random.nextInt(8);
            int dz = random.nextInt(8) - random.nextInt(8);
            int dy = random.nextInt(4) - random.nextInt(4);
            BlockPos target = origin.add(dx, dy, dz);

            if (world.isAir(target) && ModBlocks.WILD_WASABI.getDefaultState().canPlaceAt(world, target)) {
                world.setBlockState(target, ModBlocks.WILD_WASABI.getDefaultState(), 2);
                placedAny = true;
            }
        }

        // 4. Generate Wild Dragon Fruit Cacti
        for (int i = 0; i < 8; i++) {
            int dx = random.nextInt(10) - random.nextInt(10);
            int dz = random.nextInt(10) - random.nextInt(10);
            int dy = random.nextInt(4) - random.nextInt(4);
            BlockPos target = origin.add(dx, dy, dz);

            if (world.isAir(target) && ModBlocks.WILD_DRAGON_FRUIT.getDefaultState().canPlaceAt(world, target)) {
                world.setBlockState(target, ModBlocks.WILD_DRAGON_FRUIT.getDefaultState(), 2);
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

    private void generateStarfruitTree(StructureWorldAccess world, BlockPos pos, Random random) {
        BlockPos surface = pos;
        while (surface.getY() > world.getBottomY() && world.isAir(surface)) {
            surface = surface.down();
        }
        BlockPos trunkBase = surface.up();
        BlockState ground = world.getBlockState(surface);
        if (!ground.isOf(Blocks.GRASS_BLOCK) && !ground.isOf(Blocks.DIRT) && !ground.isOf(ModBlocks.VOLCANIC_SOIL)) {
            return;
        }

        int height = 4 + random.nextInt(3);

        for (int y = 0; y < height; y++) {
            BlockPos logPos = trunkBase.up(y);
            if (world.isAir(logPos) || world.getBlockState(logPos).isOf(ModBlocks.STARFRUIT_LEAVES)) {
                world.setBlockState(logPos, ModBlocks.STARFRUIT_LOG.getDefaultState(), 2);
            }
        }

        BlockPos canopyCenter = trunkBase.up(height);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -2; dy <= 1; dy++) {
                    if (Math.abs(dx) == 2 && Math.abs(dz) == 2 && (dy == 1 || random.nextBoolean())) {
                        continue;
                    }
                    BlockPos leafPos = canopyCenter.add(dx, dy, dz);
                    if (world.isAir(leafPos)) {
                        world.setBlockState(leafPos, ModBlocks.STARFRUIT_LEAVES.getDefaultState(), 2);
                    }
                }
            }
        }
    }

    private void generateAvocadoTree(StructureWorldAccess world, BlockPos pos, Random random) {
        BlockPos surface = pos;
        while (surface.getY() > world.getBottomY() && world.isAir(surface)) {
            surface = surface.down();
        }
        BlockPos trunkBase = surface.up();
        BlockState ground = world.getBlockState(surface);
        if (!ground.isOf(Blocks.GRASS_BLOCK) && !ground.isOf(Blocks.DIRT) && !ground.isOf(ModBlocks.VOLCANIC_SOIL)) {
            return;
        }

        int height = 4 + random.nextInt(3);

        // Trunk
        for (int y = 0; y < height; y++) {
            BlockPos logPos = trunkBase.up(y);
            if (world.isAir(logPos) || world.getBlockState(logPos).isOf(ModBlocks.AVOCADO_LEAVES)) {
                world.setBlockState(logPos, ModBlocks.AVOCADO_LOG.getDefaultState(), 2);
            }
        }

        // Canopy
        BlockPos canopyCenter = trunkBase.up(height);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -2; dy <= 1; dy++) {
                    if (Math.abs(dx) == 2 && Math.abs(dz) == 2 && (dy == 1 || random.nextBoolean())) {
                        continue;
                    }
                    BlockPos leafPos = canopyCenter.add(dx, dy, dz);
                    if (world.isAir(leafPos)) {
                        world.setBlockState(leafPos, ModBlocks.AVOCADO_LEAVES.getDefaultState(), 2);
                    }
                }
            }
        }
    }
}
