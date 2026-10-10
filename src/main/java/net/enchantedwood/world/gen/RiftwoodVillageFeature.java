package net.enchantedwood.world.gen;

import com.mojang.serialization.Codec;
import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.world.dimension.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
public class RiftwoodVillageFeature implements Feature {
    public static final com.mojang.serialization.MapCodec<RiftwoodVillageFeature> CODEC = com.mojang.serialization.MapCodec.unit(RiftwoodVillageFeature::new);
    public static final ResourceKey<Biome> RIFTWOOD_HAVEN_KEY = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "riftwood_haven"));

    public RiftwoodVillageFeature() {
    }

    @Override
    public com.mojang.serialization.MapCodec<? extends Feature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(net.minecraft.world.level.WorldGenLevel world, net.minecraft.world.level.chunk.ChunkGenerator generator, net.minecraft.util.RandomSource random, net.minecraft.core.BlockPos origin) {
        

        // Strict dimension check
        if (world.getLevel().dimension() != ModDimensions.CONVERGENCE_WORLD_KEY) {
            return false;
        }

        
        

        // Biome check: strictly generates in Riftwood Haven
        if (!world.getBiome(origin).is(RIFTWOOD_HAVEN_KEY)) {
            return false;
        }

        // Deterministic Grid Spacing: exactly 1 village per 24x24 chunk cell (384x384 blocks)
        int chunkX = origin.getX() >> 4;
        int chunkZ = origin.getZ() >> 4;
        if (Math.floorMod(chunkX, 24) != 12 || Math.floorMod(chunkZ, 24) != 12) {
            return false;
        }

        // Center precisely at the middle of the generating chunk (dx=8, dz=8)
        // All buildings comfortably sit within the safe ChunkRegion (radius <= 21 blocks)
        int centerX = (chunkX << 4) + 8;
        int centerZ = (chunkZ << 4) + 8;

        int surfaceY = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, centerX, centerZ) - 1;
        if (surfaceY <= world.getMinY() + 10 || surfaceY >= world.getMaxY() - 30) {
            return false;
        }

        BlockPos center = new BlockPos(centerX, surfaceY, centerZ);
        BlockState centerGround = world.getBlockState(center);
        if (!centerGround.is(Blocks.GRASS_BLOCK) && !centerGround.is(Blocks.DIRT)) {
            return false;
        }

        // 1. Central Plaza & Town Well (7x7: -3 to +3)
        buildTownSquareAndWell(world, center, random);

        // 2. Chieftain's Hall (North of well, offset z - 15)
        buildChieftainsHall(world, center.offset(-4, 0, -15), random);

        // 3. Avocado Timber Cottage (East of well, offset x + 9)
        buildAvocadoCottage(world, center.offset(9, 0, -3), random);

        // 4. Starfruit Timber Cottage (West of well, offset x - 15)
        buildStarfruitCottage(world, center.offset(-15, 0, -3), random);

        // 5. Herbalist / Apothecary Cottage (North-East, offset x + 8, z - 15)
        buildHerbalistCottage(world, center.offset(8, 0, -15), random);

        // 6. Woodcutter / Carpenter's Lodge (North-West, offset x - 16, z - 15)
        buildLumberjackLodge(world, center.offset(-16, 0, -15), random);

        // 7. Weaver & Tailor Cottage (South-East, offset x + 8, z + 7)
        buildWeaverCottage(world, center.offset(8, 0, 7), random);

        // 8. Farmland Plot (South-West of well, offset x - 12, z + 7)
        buildFarmlandPlot(world, center.offset(-12, 0, 7), random);

        // 9. Blacksmith Workshop (South of well, offset z + 8)
        buildBlacksmithWorkshop(world, center.offset(-3, 0, 8), random);

        // 10. Livestock Pasture & Animal Pen (South, offset x - 3, z + 15)
        buildAnimalPen(world, center.offset(-3, 0, 15), random);

        // 11. Street Lamp Posts at Key Crossings
        placeStreetLamps(world, center);

        // 12. Dirt Path Network connecting all buildings to central square
        buildPathNetwork(world, center);

        // 13. Spawn Villagers & Iron Golem
        spawnInhabitants(world, center);

        return true;
    }

    private void buildTownSquareAndWell(WorldGenLevel world, BlockPos center, RandomSource random) {
        // Level central 7x7 square
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                int distSq = dx * dx + dz * dz;
                int y = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, center.getX() + dx, center.getZ() + dz) - 1;
                BlockPos floorPos = new BlockPos(center.getX() + dx, y, center.getZ() + dz);

                // Foundation downward
                for (int dy = -2; dy <= 0; dy++) {
                    world.setBlock(floorPos.above(dy), Blocks.STONE_BRICKS.defaultBlockState(), 2);
                }

                // Well in center (radius 1)
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                    if (dx == 0 && dz == 0) {
                        // Deep water source
                        world.setBlock(floorPos, Blocks.WATER.defaultBlockState(), 2);
                        world.setBlock(floorPos.below(1), Blocks.WATER.defaultBlockState(), 2);
                        world.setBlock(floorPos.below(2), Blocks.STONE_BRICKS.defaultBlockState(), 2);
                    } else {
                        // Cobblestone rim
                        world.setBlock(floorPos.above(1), Blocks.COBBLESTONE.defaultBlockState(), 2);
                    }
                } else if (distSq <= 9) {
                    world.setBlock(floorPos, Blocks.DIRT_PATH.defaultBlockState(), 2);
                }
            }
        }

        // Four fence posts on well rim
        int y = center.getY() + 1;
        BlockPos c = new BlockPos(center.getX(), y, center.getZ());
        world.setBlock(c.offset(1, 1, 1), Blocks.SPRUCE_FENCE.defaultBlockState(), 2);
        world.setBlock(c.offset(-1, 1, 1), Blocks.SPRUCE_FENCE.defaultBlockState(), 2);
        world.setBlock(c.offset(1, 1, -1), Blocks.SPRUCE_FENCE.defaultBlockState(), 2);
        world.setBlock(c.offset(-1, 1, -1), Blocks.SPRUCE_FENCE.defaultBlockState(), 2);

        // Canopy roof over well
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.setBlock(c.offset(dx, 2, dz), Blocks.SPRUCE_SLAB.defaultBlockState(), 2);
            }
        }
        // Bell and lantern
        world.setBlock(c.offset(0, 2, 0), Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true), 2);
        world.setBlock(c.offset(2, 0, 0), Blocks.BELL.defaultBlockState(), 2);
    }

    private void placeDoor(WorldGenLevel world, BlockPos pos, Direction facing) {
        BlockState lower = Blocks.SPRUCE_DOOR.defaultBlockState()
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER)
                .setValue(DoorBlock.FACING, facing);
        BlockState upper = Blocks.SPRUCE_DOOR.defaultBlockState()
                .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER)
                .setValue(DoorBlock.FACING, facing);
        world.setBlock(pos, lower, 2);
        world.setBlock(pos.above(), upper, 2);
    }

    private void placeBed(WorldGenLevel world, BlockPos footPos, Direction facing, Block bedBlock) {
        BlockPos headPos = footPos.relative(facing);
        world.setBlock(footPos, bedBlock.defaultBlockState()
                .setValue(BedBlock.PART, BedPart.FOOT)
                .setValue(BedBlock.FACING, facing), 2);
        world.setBlock(headPos, bedBlock.defaultBlockState()
                .setValue(BedBlock.PART, BedPart.HEAD)
                .setValue(BedBlock.FACING, facing), 2);
    }

    private void buildChieftainsHall(WorldGenLevel world, BlockPos pos, RandomSource random) {
        int width = 9;
        int length = 7;
        int height = 5;

        int baseY = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, pos.getX() + width / 2, pos.getZ() + length / 2) - 1;
        BlockPos start = new BlockPos(pos.getX(), baseY, pos.getZ());

        // Deep Foundation & Floor (down to -7 to prevent floating on hills)
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                BlockPos p = start.offset(x, 0, z);
                for (int dy = -7; dy <= 0; dy++) {
                    world.setBlock(p.above(dy), Blocks.STONE_BRICKS.defaultBlockState(), 2);
                }
            }
        }

        // Walls and Pillars (Avocado Log corners, Starfruit Planks siding, Avocado Wood trim)
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                boolean isCorner = (x == 0 || x == width - 1) && (z == 0 || z == length - 1);
                boolean isEdge = (x == 0 || x == width - 1 || z == 0 || z == length - 1);

                for (int y = 1; y <= height; y++) {
                    BlockPos p = start.offset(x, y, z);
                    if (isCorner) {
                        world.setBlock(p, ModBlocks.STARFRUIT_LOG.defaultBlockState(), 2);
                    } else if (isEdge) {
                        // Doorway at front center
                        if (z == length - 1 && x == width / 2 && y <= 2) {
                            world.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                        } else if (y == 2 && ((x == 2 || x == width - 3) && (z == 0 || z == length - 1))) {
                            world.setBlock(p, Blocks.GLASS_PANE.defaultBlockState(), 2);
                        } else if (y == 2 && ((z == 2 || z == length - 3) && (x == 0 || x == width - 1))) {
                            world.setBlock(p, Blocks.GLASS_PANE.defaultBlockState(), 2);
                        } else if (y == height) {
                            world.setBlock(p, ModBlocks.AVOCADO_WOOD.defaultBlockState(), 2);
                        } else {
                            world.setBlock(p, ModBlocks.STARFRUIT_PLANKS.defaultBlockState(), 2);
                        }
                    } else {
                        world.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        // Fully Enclosed Pitched Roof (Stairs + Ridge)
        for (int z = -1; z <= length; z++) {
            for (int r = 0; r <= 3; r++) {
                BlockPos left = start.offset(r, height + r, z);
                BlockPos right = start.offset(width - 1 - r, height + r, z);
                world.setBlock(left, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST), 2);
                world.setBlock(right, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST), 2);
            }
            world.setBlock(start.offset(width / 2, height + 4, z), Blocks.SPRUCE_SLAB.defaultBlockState(), 2);
        }

        // Fully Enclose the Gable Walls (Front & Back triangular ends)
        for (int r = 0; r <= 3; r++) {
            int y = height + r;
            for (int x = r + 1; x <= width - 2 - r; x++) {
                world.setBlock(start.offset(x, y, length - 1), ModBlocks.STARFRUIT_PLANKS.defaultBlockState(), 2);
                world.setBlock(start.offset(x, y, 0), ModBlocks.STARFRUIT_PLANKS.defaultBlockState(), 2);
            }
        }
        world.setBlock(start.offset(width / 2, height + 2, length - 1), Blocks.GLASS_PANE.defaultBlockState(), 2);
        world.setBlock(start.offset(width / 2, height + 2, 0), Blocks.GLASS_PANE.defaultBlockState(), 2);

        // Entrance Door
        BlockPos doorPos = start.offset(width / 2, 1, length - 1);
        placeDoor(world, doorPos, Direction.SOUTH);

        // Interior: Furniture & Loot
        placeBed(world, start.offset(1, 1, 2), Direction.NORTH, Blocks.BED.red());
        world.setBlock(start.offset(2, 1, 1), Blocks.CRAFTING_TABLE.defaultBlockState(), 2);
        placeBed(world, start.offset(width - 2, 1, 2), Direction.NORTH, Blocks.BED.purple());

        // Chieftain's Loot Chest
        BlockPos chestPos = start.offset(width - 3, 1, 1);
        world.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
        BlockEntity be = world.getBlockEntity(chestPos);
        if (be instanceof ChestBlockEntity chest) {
            chest.setItem(0, new ItemStack(ModItems.AVOCADO, 4 + random.nextInt(6)));
            chest.setItem(1, new ItemStack(ModItems.STARFRUIT, 3 + random.nextInt(5)));
            chest.setItem(2, new ItemStack(Items.EMERALD, 2 + random.nextInt(4)));
            chest.setItem(3, new ItemStack(ModItems.TIN_INGOT, 3 + random.nextInt(5)));
            chest.setItem(4, new ItemStack(Items.BREAD, 6));
        }

        // Chandelier Lantern
        world.setBlock(start.offset(width / 2, height, length / 2), Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true), 2);
    }

    private void buildAvocadoCottage(WorldGenLevel world, BlockPos pos, RandomSource random) {
        int width = 6;
        int length = 6;
        int height = 4;

        int baseY = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, pos.getX() + width / 2, pos.getZ() + length / 2) - 1;
        BlockPos start = new BlockPos(pos.getX(), baseY, pos.getZ());

        // Deep Foundation & Floor (down to -7)
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                BlockPos p = start.offset(x, 0, z);
                for (int dy = -7; dy <= 0; dy++) {
                    world.setBlock(p.above(dy), Blocks.COBBLESTONE.defaultBlockState(), 2);
                }
            }
        }

        // Walls
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                boolean isCorner = (x == 0 || x == width - 1) && (z == 0 || z == length - 1);
                boolean isEdge = (x == 0 || x == width - 1 || z == 0 || z == length - 1);

                for (int y = 1; y <= height; y++) {
                    BlockPos p = start.offset(x, y, z);
                    if (isCorner) {
                        world.setBlock(p, ModBlocks.AVOCADO_LOG.defaultBlockState(), 2);
                    } else if (isEdge) {
                        // Doorway at west side center
                        if (x == 0 && z == length / 2 && y <= 2) {
                            world.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                        } else if (y == 2 && ((x == width - 1 && z == length / 2) || (z == 0 && x == 3) || (z == length - 1 && x == 3))) {
                            world.setBlock(p, Blocks.GLASS_PANE.defaultBlockState(), 2);
                        } else if (y == height) {
                            world.setBlock(p, ModBlocks.AVOCADO_WOOD.defaultBlockState(), 2);
                        } else {
                            world.setBlock(p, ModBlocks.STARFRUIT_PLANKS.defaultBlockState(), 2);
                        }
                    } else {
                        world.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        // Pitched Roof over cottage (Slopes East-West)
        for (int z = -1; z <= length; z++) {
            world.setBlock(start.offset(0, height + 1, z), Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST), 2);
            world.setBlock(start.offset(1, height + 2, z), Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST), 2);
            world.setBlock(start.offset(width - 1, height + 1, z), Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST), 2);
            world.setBlock(start.offset(width - 2, height + 2, z), Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST), 2);
            world.setBlock(start.offset(2, height + 2, z), Blocks.SPRUCE_SLAB.defaultBlockState(), 2);
            world.setBlock(start.offset(3, height + 2, z), Blocks.SPRUCE_SLAB.defaultBlockState(), 2);
        }

        // Gable Walls
        for (int z : new int[]{ 0, length - 1 }) {
            world.setBlock(start.offset(1, height + 1, z), ModBlocks.STARFRUIT_PLANKS.defaultBlockState(), 2);
            world.setBlock(start.offset(2, height + 1, z), ModBlocks.STARFRUIT_PLANKS.defaultBlockState(), 2);
            world.setBlock(start.offset(3, height + 1, z), ModBlocks.STARFRUIT_PLANKS.defaultBlockState(), 2);
            world.setBlock(start.offset(4, height + 1, z), ModBlocks.STARFRUIT_PLANKS.defaultBlockState(), 2);
        }

        placeDoor(world, start.offset(0, 1, length / 2), Direction.WEST);

        // Interior
        placeBed(world, start.offset(width - 2, 1, 2), Direction.NORTH, Blocks.BED.white());
        world.setBlock(start.offset(width - 2, 1, length - 2), Blocks.CRAFTING_TABLE.defaultBlockState(), 2);
        world.setBlock(start.offset(width / 2, height, length / 2), Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true), 2);
    }

    private void buildStarfruitCottage(WorldGenLevel world, BlockPos pos, RandomSource random) {
        int width = 6;
        int length = 6;
        int height = 4;

        int baseY = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, pos.getX() + width / 2, pos.getZ() + length / 2) - 1;
        BlockPos start = new BlockPos(pos.getX(), baseY, pos.getZ());

        // Deep Foundation & Floor (down to -7)
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                BlockPos p = start.offset(x, 0, z);
                for (int dy = -7; dy <= 0; dy++) {
                    world.setBlock(p.above(dy), Blocks.STONE_BRICKS.defaultBlockState(), 2);
                }
            }
        }

        // Walls
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                boolean isCorner = (x == 0 || x == width - 1) && (z == 0 || z == length - 1);
                boolean isEdge = (x == 0 || x == width - 1 || z == 0 || z == length - 1);

                for (int y = 1; y <= height; y++) {
                    BlockPos p = start.offset(x, y, z);
                    if (isCorner) {
                        world.setBlock(p, ModBlocks.STARFRUIT_LOG.defaultBlockState(), 2);
                    } else if (isEdge) {
                        // Doorway at east side center
                        if (x == width - 1 && z == length / 2 && y <= 2) {
                            world.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                        } else if (y == 2 && ((x == 0 && z == length / 2) || (z == 0 && x == 3) || (z == length - 1 && x == 3))) {
                            world.setBlock(p, Blocks.GLASS_PANE.defaultBlockState(), 2);
                        } else if (y == height) {
                            world.setBlock(p, ModBlocks.STARFRUIT_WOOD.defaultBlockState(), 2);
                        } else {
                            world.setBlock(p, ModBlocks.STARFRUIT_PLANKS.defaultBlockState(), 2);
                        }
                    } else {
                        world.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        // Pitched Roof over cottage
        for (int z = -1; z <= length; z++) {
            world.setBlock(start.offset(0, height + 1, z), Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST), 2);
            world.setBlock(start.offset(1, height + 2, z), Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST), 2);
            world.setBlock(start.offset(width - 1, height + 1, z), Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST), 2);
            world.setBlock(start.offset(width - 2, height + 2, z), Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST), 2);
            world.setBlock(start.offset(2, height + 2, z), Blocks.SPRUCE_SLAB.defaultBlockState(), 2);
            world.setBlock(start.offset(3, height + 2, z), Blocks.SPRUCE_SLAB.defaultBlockState(), 2);
        }

        // Gable Walls
        for (int z : new int[]{ 0, length - 1 }) {
            world.setBlock(start.offset(1, height + 1, z), ModBlocks.STARFRUIT_PLANKS.defaultBlockState(), 2);
            world.setBlock(start.offset(2, height + 1, z), ModBlocks.STARFRUIT_PLANKS.defaultBlockState(), 2);
            world.setBlock(start.offset(3, height + 1, z), ModBlocks.STARFRUIT_PLANKS.defaultBlockState(), 2);
            world.setBlock(start.offset(4, height + 1, z), ModBlocks.STARFRUIT_PLANKS.defaultBlockState(), 2);
        }

        placeDoor(world, start.offset(width - 1, 1, length / 2), Direction.EAST);

        // Interior
        placeBed(world, start.offset(1, 1, 2), Direction.NORTH, Blocks.BED.yellow());
        world.setBlock(start.offset(1, 1, length - 2), Blocks.FURNACE.defaultBlockState(), 2);
        world.setBlock(start.offset(width / 2, height, length / 2), Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true), 2);
    }

    private void buildHerbalistCottage(WorldGenLevel world, BlockPos pos, RandomSource random) {
        int width = 6;
        int length = 6;
        int height = 4;

        int baseY = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, pos.getX() + width / 2, pos.getZ() + length / 2) - 1;
        BlockPos start = new BlockPos(pos.getX(), baseY, pos.getZ());

        // Foundation & Floor
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                BlockPos p = start.offset(x, 0, z);
                for (int dy = -7; dy <= 0; dy++) {
                    world.setBlock(p.above(dy), Blocks.COBBLESTONE.defaultBlockState(), 2);
                }
            }
        }

        // Walls (Avocado Logs & Starfruit Planks)
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                boolean isCorner = (x == 0 || x == width - 1) && (z == 0 || z == length - 1);
                boolean isEdge = (x == 0 || x == width - 1 || z == 0 || z == length - 1);

                for (int y = 1; y <= height; y++) {
                    BlockPos p = start.offset(x, y, z);
                    if (isCorner) {
                        world.setBlock(p, ModBlocks.AVOCADO_LOG.defaultBlockState(), 2);
                    } else if (isEdge) {
                        // Doorway at south side (facing main street)
                        if (z == length - 1 && x == width / 2 && y <= 2) {
                            world.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                        } else if (y == 2 && ((z == 0 && x == 3) || (x == 0 && z == 3) || (x == width - 1 && z == 3))) {
                            world.setBlock(p, Blocks.GLASS_PANE.defaultBlockState(), 2);
                        } else if (y == height) {
                            world.setBlock(p, ModBlocks.AVOCADO_WOOD.defaultBlockState(), 2);
                        } else {
                            world.setBlock(p, ModBlocks.STARFRUIT_PLANKS.defaultBlockState(), 2);
                        }
                    } else {
                        world.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        // Roof
        for (int x = -1; x <= width; x++) {
            world.setBlock(start.offset(x, height + 1, 0), Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH), 2);
            world.setBlock(start.offset(x, height + 2, 1), Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH), 2);
            world.setBlock(start.offset(x, height + 1, length - 1), Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH), 2);
            world.setBlock(start.offset(x, height + 2, length - 2), Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH), 2);
            world.setBlock(start.offset(x, height + 2, 2), Blocks.SPRUCE_SLAB.defaultBlockState(), 2);
            world.setBlock(start.offset(x, height + 2, 3), Blocks.SPRUCE_SLAB.defaultBlockState(), 2);
        }

        placeDoor(world, start.offset(width / 2, 1, length - 1), Direction.SOUTH);

        // Interior: Brewing Stand, Cauldron, Botanical Chest
        placeBed(world, start.offset(1, 1, 2), Direction.NORTH, Blocks.BED.lime());
        world.setBlock(start.offset(width - 2, 1, 1), Blocks.BREWING_STAND.defaultBlockState(), 2);
        world.setBlock(start.offset(width - 2, 1, 2), Blocks.CAULDRON.defaultBlockState(), 2);
        world.setBlock(start.offset(1, 1, length - 2), Blocks.FLOWER_POT.defaultBlockState(), 2);

        BlockPos chestPos = start.offset(width - 2, 1, length - 2);
        world.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
        BlockEntity be = world.getBlockEntity(chestPos);
        if (be instanceof ChestBlockEntity chest) {
            chest.setItem(0, new ItemStack(ModItems.AVOCADO, 3 + random.nextInt(4)));
            chest.setItem(1, new ItemStack(ModItems.WASABI_ROOT, 2 + random.nextInt(3)));
            chest.setItem(2, new ItemStack(Items.GLASS_BOTTLE, 4));
            chest.setItem(3, new ItemStack(Items.SUGAR, 5));
        }

        world.setBlock(start.offset(width / 2, height, length / 2), Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true), 2);
    }

    private void buildLumberjackLodge(WorldGenLevel world, BlockPos pos, RandomSource random) {
        int width = 6;
        int length = 6;
        int height = 4;

        int baseY = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, pos.getX() + width / 2, pos.getZ() + length / 2) - 1;
        BlockPos start = new BlockPos(pos.getX(), baseY, pos.getZ());

        // Foundation
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                BlockPos p = start.offset(x, 0, z);
                for (int dy = -7; dy <= 0; dy++) {
                    world.setBlock(p.above(dy), Blocks.STONE_BRICKS.defaultBlockState(), 2);
                }
            }
        }

        // Walls
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                boolean isCorner = (x == 0 || x == width - 1) && (z == 0 || z == length - 1);
                boolean isEdge = (x == 0 || x == width - 1 || z == 0 || z == length - 1);

                for (int y = 1; y <= height; y++) {
                    BlockPos p = start.offset(x, y, z);
                    if (isCorner) {
                        world.setBlock(p, ModBlocks.STARFRUIT_LOG.defaultBlockState(), 2);
                    } else if (isEdge) {
                        if (z == length - 1 && x == width / 2 && y <= 2) {
                            world.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                        } else if (y == 2 && ((z == 0 && x == 3) || (x == 0 && z == 3) || (x == width - 1 && z == 3))) {
                            world.setBlock(p, Blocks.GLASS_PANE.defaultBlockState(), 2);
                        } else if (y == height) {
                            world.setBlock(p, ModBlocks.STARFRUIT_WOOD.defaultBlockState(), 2);
                        } else {
                            world.setBlock(p, ModBlocks.STARFRUIT_PLANKS.defaultBlockState(), 2);
                        }
                    } else {
                        world.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        // Roof
        for (int x = -1; x <= width; x++) {
            world.setBlock(start.offset(x, height + 1, 0), Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH), 2);
            world.setBlock(start.offset(x, height + 2, 1), Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH), 2);
            world.setBlock(start.offset(x, height + 1, length - 1), Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH), 2);
            world.setBlock(start.offset(x, height + 2, length - 2), Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH), 2);
            world.setBlock(start.offset(x, height + 2, 2), Blocks.SPRUCE_SLAB.defaultBlockState(), 2);
            world.setBlock(start.offset(x, height + 2, 3), Blocks.SPRUCE_SLAB.defaultBlockState(), 2);
        }

        placeDoor(world, start.offset(width / 2, 1, length - 1), Direction.SOUTH);

        // Interior: Stonecutter, Woodcrafts, Lumberjack Chest
        placeBed(world, start.offset(1, 1, 2), Direction.NORTH, Blocks.BED.brown());
        world.setBlock(start.offset(width - 2, 1, 1), Blocks.STONECUTTER.defaultBlockState(), 2);
        world.setBlock(start.offset(width - 2, 1, 2), ModBlocks.AVOCADO_LOG.defaultBlockState(), 2);
        world.setBlock(start.offset(width - 2, 2, 2), ModBlocks.STARFRUIT_LOG.defaultBlockState(), 2);

        BlockPos chestPos = start.offset(1, 1, length - 2);
        world.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
        BlockEntity be = world.getBlockEntity(chestPos);
        if (be instanceof ChestBlockEntity chest) {
            chest.setItem(0, new ItemStack(ModBlocks.STARFRUIT_PLANKS, 16));
            chest.setItem(1, new ItemStack(ModBlocks.AVOCADO_LOG, 8));
            chest.setItem(2, new ItemStack(Items.IRON_AXE, 1));
            chest.setItem(3, new ItemStack(Items.STICK, 12));
        }

        world.setBlock(start.offset(width / 2, height, length / 2), Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true), 2);
    }

    private void buildWeaverCottage(WorldGenLevel world, BlockPos pos, RandomSource random) {
        int width = 6;
        int length = 6;
        int height = 4;

        int baseY = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, pos.getX() + width / 2, pos.getZ() + length / 2) - 1;
        BlockPos start = new BlockPos(pos.getX(), baseY, pos.getZ());

        // Foundation
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                BlockPos p = start.offset(x, 0, z);
                for (int dy = -7; dy <= 0; dy++) {
                    world.setBlock(p.above(dy), Blocks.COBBLESTONE.defaultBlockState(), 2);
                }
            }
        }

        // Walls
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                boolean isCorner = (x == 0 || x == width - 1) && (z == 0 || z == length - 1);
                boolean isEdge = (x == 0 || x == width - 1 || z == 0 || z == length - 1);

                for (int y = 1; y <= height; y++) {
                    BlockPos p = start.offset(x, y, z);
                    if (isCorner) {
                        world.setBlock(p, ModBlocks.AVOCADO_LOG.defaultBlockState(), 2);
                    } else if (isEdge) {
                        // Doorway facing west (towards center path)
                        if (x == 0 && z == length / 2 && y <= 2) {
                            world.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                        } else if (y == 2 && ((x == width - 1 && z == length / 2) || (z == 0 && x == 3) || (z == length - 1 && x == 3))) {
                            world.setBlock(p, Blocks.GLASS_PANE.defaultBlockState(), 2);
                        } else if (y == height) {
                            world.setBlock(p, ModBlocks.AVOCADO_WOOD.defaultBlockState(), 2);
                        } else {
                            world.setBlock(p, ModBlocks.STARFRUIT_PLANKS.defaultBlockState(), 2);
                        }
                    } else {
                        world.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        // Roof
        for (int z = -1; z <= length; z++) {
            world.setBlock(start.offset(0, height + 1, z), Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST), 2);
            world.setBlock(start.offset(1, height + 2, z), Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST), 2);
            world.setBlock(start.offset(width - 1, height + 1, z), Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST), 2);
            world.setBlock(start.offset(width - 2, height + 2, z), Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST), 2);
            world.setBlock(start.offset(2, height + 2, z), Blocks.SPRUCE_SLAB.defaultBlockState(), 2);
            world.setBlock(start.offset(3, height + 2, z), Blocks.SPRUCE_SLAB.defaultBlockState(), 2);
        }

        placeDoor(world, start.offset(0, 1, length / 2), Direction.WEST);

        // Interior: Loom, Colored Wool & Carpets
        placeBed(world, start.offset(width - 2, 1, 2), Direction.NORTH, Blocks.BED.cyan());
        world.setBlock(start.offset(width - 2, 1, length - 2), Blocks.LOOM.defaultBlockState(), 2);
        world.setBlock(start.offset(2, 1, 2), Blocks.CARPET.white().defaultBlockState(), 2);
        world.setBlock(start.offset(3, 1, 2), Blocks.CARPET.cyan().defaultBlockState(), 2);

        BlockPos chestPos = start.offset(1, 1, 1);
        world.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
        BlockEntity be = world.getBlockEntity(chestPos);
        if (be instanceof ChestBlockEntity chest) {
            chest.setItem(0, new ItemStack(Items.WOOL.white(), 8));
            chest.setItem(1, new ItemStack(Items.SHEARS, 1));
            chest.setItem(2, new ItemStack(Items.STRING, 12));
            chest.setItem(3, new ItemStack(Items.DYE.cyan(), 4));
        }

        world.setBlock(start.offset(width / 2, height, length / 2), Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true), 2);
    }

    private void buildFarmlandPlot(WorldGenLevel world, BlockPos pos, RandomSource random) {
        int baseY = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, pos.getX() + 3, pos.getZ() + 3) - 1;
        BlockPos start = new BlockPos(pos.getX(), baseY, pos.getZ());

        for (int x = 0; x < 7; x++) {
            for (int z = 0; z < 7; z++) {
                BlockPos p = start.offset(x, 0, z);
                for (int dy = -2; dy < 0; dy++) {
                    world.setBlock(p.above(dy), Blocks.DIRT.defaultBlockState(), 2);
                }

                if (x == 0 || x == 6 || z == 0 || z == 6) {
                    world.setBlock(p, ModBlocks.STARFRUIT_LOG.defaultBlockState(), 2);
                } else if (x == 3 && z == 3) {
                    world.setBlock(p, Blocks.WATER.defaultBlockState(), 2);
                    world.setBlock(p.above(1), Blocks.LILY_PAD.defaultBlockState(), 2);
                } else {
                    world.setBlock(p, Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7), 2);
                    // Plant crops
                    if (random.nextBoolean()) {
                        world.setBlock(p.above(1), ModBlocks.WILD_RICE.defaultBlockState(), 2);
                    } else {
                        world.setBlock(p.above(1), ModBlocks.WILD_CUCUMBER.defaultBlockState(), 2);
                    }
                }
            }
        }

        // Composter on corner
        world.setBlock(start.offset(6, 1, 6), Blocks.COMPOSTER.defaultBlockState(), 2);
    }

    private void buildBlacksmithWorkshop(WorldGenLevel world, BlockPos pos, RandomSource random) {
        int baseY = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, pos.getX() + 3, pos.getZ() + 3) - 1;
        BlockPos start = new BlockPos(pos.getX(), baseY, pos.getZ());

        // Foundation (down to -7)
        for (int x = 0; x < 6; x++) {
            for (int z = 0; z < 5; z++) {
                BlockPos p = start.offset(x, 0, z);
                for (int dy = -7; dy <= 0; dy++) {
                    world.setBlock(p.above(dy), Blocks.POLISHED_ANDESITE.defaultBlockState(), 2);
                }
            }
        }

        // Pillars on 4 corners
        world.setBlock(start.offset(0, 1, 0), Blocks.STONE_BRICK_WALL.defaultBlockState(), 2);
        world.setBlock(start.offset(0, 2, 0), Blocks.STONE_BRICK_WALL.defaultBlockState(), 2);
        world.setBlock(start.offset(5, 1, 0), Blocks.STONE_BRICK_WALL.defaultBlockState(), 2);
        world.setBlock(start.offset(5, 2, 0), Blocks.STONE_BRICK_WALL.defaultBlockState(), 2);
        world.setBlock(start.offset(0, 1, 4), Blocks.STONE_BRICK_WALL.defaultBlockState(), 2);
        world.setBlock(start.offset(0, 2, 4), Blocks.STONE_BRICK_WALL.defaultBlockState(), 2);
        world.setBlock(start.offset(5, 1, 4), Blocks.STONE_BRICK_WALL.defaultBlockState(), 2);
        world.setBlock(start.offset(5, 2, 4), Blocks.STONE_BRICK_WALL.defaultBlockState(), 2);

        // Roof slab canopy
        for (int x = 0; x < 6; x++) {
            for (int z = 0; z < 5; z++) {
                world.setBlock(start.offset(x, 3, z), Blocks.STONE_BRICK_SLAB.defaultBlockState(), 2);
            }
        }

        // Equipment: Anvil, Blast Furnace, Grindstone, Chest
        world.setBlock(start.offset(1, 1, 1), Blocks.ANVIL.defaultBlockState(), 2);
        world.setBlock(start.offset(2, 1, 1), Blocks.BLAST_FURNACE.defaultBlockState(), 2);
        world.setBlock(start.offset(3, 1, 1), Blocks.GRINDSTONE.defaultBlockState(), 2);

        BlockPos chestPos = start.offset(4, 1, 1);
        world.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
        BlockEntity be = world.getBlockEntity(chestPos);
        if (be instanceof ChestBlockEntity chest) {
            chest.setItem(0, new ItemStack(Items.IRON_INGOT, 4));
            chest.setItem(1, new ItemStack(ModItems.TIN_INGOT, 6));
            chest.setItem(2, new ItemStack(Items.COPPER_INGOT, 8));
            chest.setItem(3, new ItemStack(Items.COAL, 12));
        }

        world.setBlock(start.offset(2, 2, 2), Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true), 2);
    }

    private void buildAnimalPen(WorldGenLevel world, BlockPos pos, RandomSource random) {
        int width = 7;
        int length = 6;
        int baseY = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, pos.getX() + width / 2, pos.getZ() + length / 2) - 1;
        BlockPos start = new BlockPos(pos.getX(), baseY, pos.getZ());

        // Floor
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                BlockPos p = start.offset(x, 0, z);
                for (int dy = -4; dy <= 0; dy++) {
                    world.setBlock(p.above(dy), (dy == 0 && random.nextBoolean()) ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.DIRT.defaultBlockState(), 2);
                }
            }
        }

        // Fence perimeter
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                if (x == 0 || x == width - 1 || z == 0 || z == length - 1) {
                    if (z == 0 && x == width / 2) {
                        // Gate facing north (towards village path)
                        world.setBlock(start.offset(x, 1, z), Blocks.SPRUCE_FENCE_GATE.defaultBlockState(), 2);
                    } else {
                        world.setBlock(start.offset(x, 1, z), Blocks.SPRUCE_FENCE.defaultBlockState(), 2);
                    }
                }
            }
        }

        // Hay bales and water trough
        world.setBlock(start.offset(1, 1, length - 2), Blocks.HAY_BLOCK.defaultBlockState(), 2);
        world.setBlock(start.offset(2, 1, length - 2), Blocks.HAY_BLOCK.defaultBlockState(), 2);
        world.setBlock(start.offset(1, 2, length - 2), Blocks.HAY_BLOCK.defaultBlockState(), 2);
        world.setBlock(start.offset(width - 2, 1, length - 2), Blocks.CAULDRON.defaultBlockState(), 2);

        // Corner lamp post
        world.setBlock(start.offset(0, 2, 0), Blocks.SPRUCE_FENCE.defaultBlockState(), 2);
        world.setBlock(start.offset(0, 3, 0), Blocks.LANTERN.defaultBlockState(), 2);

        // Spawn 2 Sheep or 2 Cows
        int animalX = start.getX() + 3;
        int animalZ = start.getZ() + 3;
        int animalY = start.getY() + 1;
        if (random.nextBoolean()) {
            for (int i = 0; i < 2; i++) {
                Sheep sheep = EntityTypes.SHEEP.create(world.getLevel(), EntitySpawnReason.STRUCTURE);
                if (sheep != null) {
                    sheep.snapTo(animalX + (i * 1.2), animalY, animalZ, 0.0F, 0.0F);
                    world.addFreshEntity(sheep);
                }
            }
        } else {
            for (int i = 0; i < 2; i++) {
                Cow cow = EntityTypes.COW.create(world.getLevel(), EntitySpawnReason.STRUCTURE);
                if (cow != null) {
                    cow.snapTo(animalX + (i * 1.2), animalY, animalZ, 0.0F, 0.0F);
                    world.addFreshEntity(cow);
                }
            }
        }
    }

    private void placeStreetLamps(WorldGenLevel world, BlockPos center) {
        BlockPos[] lampLocations = {
                center.offset(4, 0, -4),
                center.offset(-4, 0, -4),
                center.offset(4, 0, 4),
                center.offset(-4, 0, 4)
        };

        for (BlockPos loc : lampLocations) {
            int y = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, loc.getX(), loc.getZ()) - 1;
            BlockPos base = new BlockPos(loc.getX(), y, loc.getZ());
            if (world.getBlockState(base).is(Blocks.GRASS_BLOCK) || world.getBlockState(base).is(Blocks.DIRT) || world.getBlockState(base).is(Blocks.DIRT_PATH)) {
                world.setBlock(base.above(1), Blocks.SPRUCE_FENCE.defaultBlockState(), 2);
                world.setBlock(base.above(2), Blocks.SPRUCE_FENCE.defaultBlockState(), 2);
                world.setBlock(base.above(3), Blocks.LANTERN.defaultBlockState(), 2);
            }
        }
    }

    private void buildPathNetwork(WorldGenLevel world, BlockPos center) {
        // Connect center plaza to each building entry point
        int[][] targetDeltas = {
                { 0, -11 },    // Chieftain's Hall
                { 8, -11 },    // Herbalist Cottage
                { -11, -11 },  // Lumberjack Lodge
                { 9, 0 },      // Avocado Cottage
                { -10, 0 },    // Starfruit Cottage
                { 8, 8 },      // Weaver Cottage
                { -10, 8 },    // Farmland Plot
                { 0, 8 },      // Blacksmith Workshop
                { 0, 15 }      // Animal Pen
        };

        for (int[] delta : targetDeltas) {
            int steps = Math.max(Math.abs(delta[0]), Math.abs(delta[1]));
            for (int s = 0; s <= steps; s++) {
                float f = (float) s / steps;
                int x = center.getX() + (int) (delta[0] * f);
                int z = center.getZ() + (int) (delta[1] * f);
                int y = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                BlockPos p = new BlockPos(x, y, z);
                if (world.getBlockState(p).is(Blocks.GRASS_BLOCK) || world.getBlockState(p).is(Blocks.DIRT)) {
                    world.setBlock(p, Blocks.DIRT_PATH.defaultBlockState(), 2);
                }
            }
        }
    }

    private void spawnInhabitants(WorldGenLevel world, BlockPos center) {
        // Spawn 7 diverse villagers across the village
        int[][] villagerOffsets = {
                { 0, 0 },      // Plaza well
                { 0, -10 },    // Chieftain's hall porch
                { 7, 0 },      // Avocado cottage porch
                { -8, 0 },     // Starfruit cottage porch
                { 7, -10 },    // Herbalist porch
                { -10, -10 },  // Lumberjack porch
                { 7, 7 }       // Weaver porch
        };

        for (int[] offset : villagerOffsets) {
            int x = center.getX() + offset[0];
            int z = center.getZ() + offset[1];
            int y = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) + 1;
            Villager villager = EntityTypes.VILLAGER.create(world.getLevel(), EntitySpawnReason.STRUCTURE);
            if (villager != null) {
                villager.snapTo(x + 0.5, y, z + 0.5, 0.0F, 0.0F);
                villager.finalizeSpawn(world, world.getCurrentDifficultyAt(new BlockPos(x, y, z)), EntitySpawnReason.STRUCTURE, null);
                world.addFreshEntity(villager);
            }
        }

        // Spawn 1 Village Iron Golem Protector
        int golemY = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, center.getX() + 2, center.getZ() + 2) + 1;
        IronGolem golem = EntityTypes.IRON_GOLEM.create(world.getLevel(), EntitySpawnReason.STRUCTURE);
        if (golem != null) {
            golem.snapTo(center.getX() + 2.5, golemY, center.getZ() + 2.5, 0.0F, 0.0F);
            world.addFreshEntity(golem);
        }
    }
}
