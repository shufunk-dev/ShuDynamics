package net.enchantedwood.world.gen;

import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;
import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.world.dimension.ModDimensions;

public class RiftwoodVillageFeature extends Feature<DefaultFeatureConfig> {

    private static final RegistryKey<Biome> RIFTWOOD_HAVEN_KEY =
            RegistryKey.of(RegistryKeys.BIOME, Identifier.of(EnchantedWoodMod.MOD_ID, "riftwood_haven"));

    public RiftwoodVillageFeature(Codec<DefaultFeatureConfig> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();

        // Strict dimension check
        if (world.toServerWorld().getRegistryKey() != ModDimensions.CONVERGENCE_WORLD_KEY) {
            return false;
        }

        BlockPos origin = context.getOrigin();
        Random random = context.getRandom();

        // Biome check: strictly generates in Riftwood Haven
        if (!world.getBiome(origin).matchesKey(RIFTWOOD_HAVEN_KEY)) {
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

        int surfaceY = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, centerX, centerZ) - 1;
        if (surfaceY <= world.getBottomY() + 10 || surfaceY >= world.getTopYInclusive() - 30) {
            return false;
        }

        BlockPos center = new BlockPos(centerX, surfaceY, centerZ);
        BlockState centerGround = world.getBlockState(center);
        if (!centerGround.isOf(Blocks.GRASS_BLOCK) && !centerGround.isOf(Blocks.DIRT)) {
            return false;
        }

        // 1. Central Plaza & Town Well (7x7: -3 to +3)
        buildTownSquareAndWell(world, center, random);

        // 2. Chieftain's Hall (North of well, offset z - 15)
        buildChieftainsHall(world, center.add(-4, 0, -15), random);

        // 3. Avocado Timber Cottage (East of well, offset x + 9)
        buildAvocadoCottage(world, center.add(9, 0, -3), random);

        // 4. Starfruit Timber Cottage (West of well, offset x - 15)
        buildStarfruitCottage(world, center.add(-15, 0, -3), random);

        // 5. Herbalist / Apothecary Cottage (North-East, offset x + 8, z - 15)
        buildHerbalistCottage(world, center.add(8, 0, -15), random);

        // 6. Woodcutter / Carpenter's Lodge (North-West, offset x - 16, z - 15)
        buildLumberjackLodge(world, center.add(-16, 0, -15), random);

        // 7. Weaver & Tailor Cottage (South-East, offset x + 8, z + 7)
        buildWeaverCottage(world, center.add(8, 0, 7), random);

        // 8. Farmland Plot (South-West of well, offset x - 12, z + 7)
        buildFarmlandPlot(world, center.add(-12, 0, 7), random);

        // 9. Blacksmith Workshop (South of well, offset z + 8)
        buildBlacksmithWorkshop(world, center.add(-3, 0, 8), random);

        // 10. Livestock Pasture & Animal Pen (South, offset x - 3, z + 15)
        buildAnimalPen(world, center.add(-3, 0, 15), random);

        // 11. Street Lamp Posts at Key Crossings
        placeStreetLamps(world, center);

        // 12. Dirt Path Network connecting all buildings to central square
        buildPathNetwork(world, center);

        // 13. Spawn Villagers & Iron Golem
        spawnInhabitants(world, center);

        return true;
    }

    private void buildTownSquareAndWell(StructureWorldAccess world, BlockPos center, Random random) {
        // Level central 7x7 square
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                int distSq = dx * dx + dz * dz;
                int y = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, center.getX() + dx, center.getZ() + dz) - 1;
                BlockPos floorPos = new BlockPos(center.getX() + dx, y, center.getZ() + dz);

                // Foundation downward
                for (int dy = -2; dy <= 0; dy++) {
                    world.setBlockState(floorPos.up(dy), Blocks.STONE_BRICKS.getDefaultState(), 2);
                }

                // Well in center (radius 1)
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                    if (dx == 0 && dz == 0) {
                        // Deep water source
                        world.setBlockState(floorPos, Blocks.WATER.getDefaultState(), 2);
                        world.setBlockState(floorPos.down(1), Blocks.WATER.getDefaultState(), 2);
                        world.setBlockState(floorPos.down(2), Blocks.STONE_BRICKS.getDefaultState(), 2);
                    } else {
                        // Cobblestone rim
                        world.setBlockState(floorPos.up(1), Blocks.COBBLESTONE.getDefaultState(), 2);
                    }
                } else if (distSq <= 9) {
                    world.setBlockState(floorPos, Blocks.DIRT_PATH.getDefaultState(), 2);
                }
            }
        }

        // Four fence posts on well rim
        int y = center.getY() + 1;
        BlockPos c = new BlockPos(center.getX(), y, center.getZ());
        world.setBlockState(c.add(1, 1, 1), Blocks.SPRUCE_FENCE.getDefaultState(), 2);
        world.setBlockState(c.add(-1, 1, 1), Blocks.SPRUCE_FENCE.getDefaultState(), 2);
        world.setBlockState(c.add(1, 1, -1), Blocks.SPRUCE_FENCE.getDefaultState(), 2);
        world.setBlockState(c.add(-1, 1, -1), Blocks.SPRUCE_FENCE.getDefaultState(), 2);

        // Canopy roof over well
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.setBlockState(c.add(dx, 2, dz), Blocks.SPRUCE_SLAB.getDefaultState(), 2);
            }
        }
        // Bell and lantern
        world.setBlockState(c.add(0, 2, 0), Blocks.LANTERN.getDefaultState().with(net.minecraft.block.LanternBlock.HANGING, true), 2);
        world.setBlockState(c.add(2, 0, 0), Blocks.BELL.getDefaultState(), 2);
    }

    private void placeDoor(StructureWorldAccess world, BlockPos pos, Direction facing) {
        BlockState lower = Blocks.SPRUCE_DOOR.getDefaultState()
                .with(DoorBlock.HALF, DoubleBlockHalf.LOWER)
                .with(DoorBlock.FACING, facing);
        BlockState upper = Blocks.SPRUCE_DOOR.getDefaultState()
                .with(DoorBlock.HALF, DoubleBlockHalf.UPPER)
                .with(DoorBlock.FACING, facing);
        world.setBlockState(pos, lower, 2);
        world.setBlockState(pos.up(), upper, 2);
    }

    private void placeBed(StructureWorldAccess world, BlockPos footPos, Direction facing, Block bedBlock) {
        BlockPos headPos = footPos.offset(facing);
        world.setBlockState(footPos, bedBlock.getDefaultState()
                .with(BedBlock.PART, BedPart.FOOT)
                .with(BedBlock.FACING, facing), 2);
        world.setBlockState(headPos, bedBlock.getDefaultState()
                .with(BedBlock.PART, BedPart.HEAD)
                .with(BedBlock.FACING, facing), 2);
    }

    private void buildChieftainsHall(StructureWorldAccess world, BlockPos pos, Random random) {
        int width = 9;
        int length = 7;
        int height = 5;

        int baseY = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, pos.getX() + width / 2, pos.getZ() + length / 2) - 1;
        BlockPos start = new BlockPos(pos.getX(), baseY, pos.getZ());

        // Deep Foundation & Floor (down to -7 to prevent floating on hills)
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                BlockPos p = start.add(x, 0, z);
                for (int dy = -7; dy <= 0; dy++) {
                    world.setBlockState(p.up(dy), Blocks.STONE_BRICKS.getDefaultState(), 2);
                }
            }
        }

        // Walls and Pillars (Avocado Log corners, Starfruit Planks siding, Avocado Wood trim)
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                boolean isCorner = (x == 0 || x == width - 1) && (z == 0 || z == length - 1);
                boolean isEdge = (x == 0 || x == width - 1 || z == 0 || z == length - 1);

                for (int y = 1; y <= height; y++) {
                    BlockPos p = start.add(x, y, z);
                    if (isCorner) {
                        world.setBlockState(p, ModBlocks.STARFRUIT_LOG.getDefaultState(), 2);
                    } else if (isEdge) {
                        // Doorway at front center
                        if (z == length - 1 && x == width / 2 && y <= 2) {
                            world.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
                        } else if (y == 2 && ((x == 2 || x == width - 3) && (z == 0 || z == length - 1))) {
                            world.setBlockState(p, Blocks.GLASS_PANE.getDefaultState(), 2);
                        } else if (y == 2 && ((z == 2 || z == length - 3) && (x == 0 || x == width - 1))) {
                            world.setBlockState(p, Blocks.GLASS_PANE.getDefaultState(), 2);
                        } else if (y == height) {
                            world.setBlockState(p, ModBlocks.AVOCADO_WOOD.getDefaultState(), 2);
                        } else {
                            world.setBlockState(p, ModBlocks.STARFRUIT_PLANKS.getDefaultState(), 2);
                        }
                    } else {
                        world.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
                    }
                }
            }
        }

        // Fully Enclosed Pitched Roof (Stairs + Ridge)
        for (int z = -1; z <= length; z++) {
            for (int r = 0; r <= 3; r++) {
                BlockPos left = start.add(r, height + r, z);
                BlockPos right = start.add(width - 1 - r, height + r, z);
                world.setBlockState(left, Blocks.SPRUCE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.EAST), 2);
                world.setBlockState(right, Blocks.SPRUCE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.WEST), 2);
            }
            world.setBlockState(start.add(width / 2, height + 4, z), Blocks.SPRUCE_SLAB.getDefaultState(), 2);
        }

        // Fully Enclose the Gable Walls (Front & Back triangular ends)
        for (int r = 0; r <= 3; r++) {
            int y = height + r;
            for (int x = r + 1; x <= width - 2 - r; x++) {
                world.setBlockState(start.add(x, y, length - 1), ModBlocks.STARFRUIT_PLANKS.getDefaultState(), 2);
                world.setBlockState(start.add(x, y, 0), ModBlocks.STARFRUIT_PLANKS.getDefaultState(), 2);
            }
        }
        world.setBlockState(start.add(width / 2, height + 2, length - 1), Blocks.GLASS_PANE.getDefaultState(), 2);
        world.setBlockState(start.add(width / 2, height + 2, 0), Blocks.GLASS_PANE.getDefaultState(), 2);

        // Entrance Door
        BlockPos doorPos = start.add(width / 2, 1, length - 1);
        placeDoor(world, doorPos, Direction.SOUTH);

        // Interior: Furniture & Loot
        placeBed(world, start.add(1, 1, 2), Direction.NORTH, Blocks.RED_BED);
        world.setBlockState(start.add(2, 1, 1), Blocks.CRAFTING_TABLE.getDefaultState(), 2);
        placeBed(world, start.add(width - 2, 1, 2), Direction.NORTH, Blocks.PURPLE_BED);

        // Chieftain's Loot Chest
        BlockPos chestPos = start.add(width - 3, 1, 1);
        world.setBlockState(chestPos, Blocks.CHEST.getDefaultState(), 2);
        BlockEntity be = world.getBlockEntity(chestPos);
        if (be instanceof ChestBlockEntity chest) {
            chest.setStack(0, new ItemStack(ModItems.AVOCADO, 4 + random.nextInt(6)));
            chest.setStack(1, new ItemStack(ModItems.STARFRUIT, 3 + random.nextInt(5)));
            chest.setStack(2, new ItemStack(Items.EMERALD, 2 + random.nextInt(4)));
            chest.setStack(3, new ItemStack(ModItems.TIN_INGOT, 3 + random.nextInt(5)));
            chest.setStack(4, new ItemStack(Items.BREAD, 6));
        }

        // Chandelier Lantern
        world.setBlockState(start.add(width / 2, height, length / 2), Blocks.LANTERN.getDefaultState().with(net.minecraft.block.LanternBlock.HANGING, true), 2);
    }

    private void buildAvocadoCottage(StructureWorldAccess world, BlockPos pos, Random random) {
        int width = 6;
        int length = 6;
        int height = 4;

        int baseY = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, pos.getX() + width / 2, pos.getZ() + length / 2) - 1;
        BlockPos start = new BlockPos(pos.getX(), baseY, pos.getZ());

        // Deep Foundation & Floor (down to -7)
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                BlockPos p = start.add(x, 0, z);
                for (int dy = -7; dy <= 0; dy++) {
                    world.setBlockState(p.up(dy), Blocks.COBBLESTONE.getDefaultState(), 2);
                }
            }
        }

        // Walls
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                boolean isCorner = (x == 0 || x == width - 1) && (z == 0 || z == length - 1);
                boolean isEdge = (x == 0 || x == width - 1 || z == 0 || z == length - 1);

                for (int y = 1; y <= height; y++) {
                    BlockPos p = start.add(x, y, z);
                    if (isCorner) {
                        world.setBlockState(p, ModBlocks.AVOCADO_LOG.getDefaultState(), 2);
                    } else if (isEdge) {
                        // Doorway at west side center
                        if (x == 0 && z == length / 2 && y <= 2) {
                            world.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
                        } else if (y == 2 && ((x == width - 1 && z == length / 2) || (z == 0 && x == 3) || (z == length - 1 && x == 3))) {
                            world.setBlockState(p, Blocks.GLASS_PANE.getDefaultState(), 2);
                        } else if (y == height) {
                            world.setBlockState(p, ModBlocks.AVOCADO_WOOD.getDefaultState(), 2);
                        } else {
                            world.setBlockState(p, ModBlocks.STARFRUIT_PLANKS.getDefaultState(), 2);
                        }
                    } else {
                        world.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
                    }
                }
            }
        }

        // Pitched Roof over cottage (Slopes East-West)
        for (int z = -1; z <= length; z++) {
            world.setBlockState(start.add(0, height + 1, z), Blocks.SPRUCE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.EAST), 2);
            world.setBlockState(start.add(1, height + 2, z), Blocks.SPRUCE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.EAST), 2);
            world.setBlockState(start.add(width - 1, height + 1, z), Blocks.SPRUCE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.WEST), 2);
            world.setBlockState(start.add(width - 2, height + 2, z), Blocks.SPRUCE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.WEST), 2);
            world.setBlockState(start.add(2, height + 2, z), Blocks.SPRUCE_SLAB.getDefaultState(), 2);
            world.setBlockState(start.add(3, height + 2, z), Blocks.SPRUCE_SLAB.getDefaultState(), 2);
        }

        // Gable Walls
        for (int z : new int[]{ 0, length - 1 }) {
            world.setBlockState(start.add(1, height + 1, z), ModBlocks.STARFRUIT_PLANKS.getDefaultState(), 2);
            world.setBlockState(start.add(2, height + 1, z), ModBlocks.STARFRUIT_PLANKS.getDefaultState(), 2);
            world.setBlockState(start.add(3, height + 1, z), ModBlocks.STARFRUIT_PLANKS.getDefaultState(), 2);
            world.setBlockState(start.add(4, height + 1, z), ModBlocks.STARFRUIT_PLANKS.getDefaultState(), 2);
        }

        placeDoor(world, start.add(0, 1, length / 2), Direction.WEST);

        // Interior
        placeBed(world, start.add(width - 2, 1, 2), Direction.NORTH, Blocks.WHITE_BED);
        world.setBlockState(start.add(width - 2, 1, length - 2), Blocks.CRAFTING_TABLE.getDefaultState(), 2);
        world.setBlockState(start.add(width / 2, height, length / 2), Blocks.LANTERN.getDefaultState().with(net.minecraft.block.LanternBlock.HANGING, true), 2);
    }

    private void buildStarfruitCottage(StructureWorldAccess world, BlockPos pos, Random random) {
        int width = 6;
        int length = 6;
        int height = 4;

        int baseY = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, pos.getX() + width / 2, pos.getZ() + length / 2) - 1;
        BlockPos start = new BlockPos(pos.getX(), baseY, pos.getZ());

        // Deep Foundation & Floor (down to -7)
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                BlockPos p = start.add(x, 0, z);
                for (int dy = -7; dy <= 0; dy++) {
                    world.setBlockState(p.up(dy), Blocks.STONE_BRICKS.getDefaultState(), 2);
                }
            }
        }

        // Walls
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                boolean isCorner = (x == 0 || x == width - 1) && (z == 0 || z == length - 1);
                boolean isEdge = (x == 0 || x == width - 1 || z == 0 || z == length - 1);

                for (int y = 1; y <= height; y++) {
                    BlockPos p = start.add(x, y, z);
                    if (isCorner) {
                        world.setBlockState(p, ModBlocks.STARFRUIT_LOG.getDefaultState(), 2);
                    } else if (isEdge) {
                        // Doorway at east side center
                        if (x == width - 1 && z == length / 2 && y <= 2) {
                            world.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
                        } else if (y == 2 && ((x == 0 && z == length / 2) || (z == 0 && x == 3) || (z == length - 1 && x == 3))) {
                            world.setBlockState(p, Blocks.GLASS_PANE.getDefaultState(), 2);
                        } else if (y == height) {
                            world.setBlockState(p, ModBlocks.STARFRUIT_WOOD.getDefaultState(), 2);
                        } else {
                            world.setBlockState(p, ModBlocks.STARFRUIT_PLANKS.getDefaultState(), 2);
                        }
                    } else {
                        world.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
                    }
                }
            }
        }

        // Pitched Roof over cottage
        for (int z = -1; z <= length; z++) {
            world.setBlockState(start.add(0, height + 1, z), Blocks.SPRUCE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.EAST), 2);
            world.setBlockState(start.add(1, height + 2, z), Blocks.SPRUCE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.EAST), 2);
            world.setBlockState(start.add(width - 1, height + 1, z), Blocks.SPRUCE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.WEST), 2);
            world.setBlockState(start.add(width - 2, height + 2, z), Blocks.SPRUCE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.WEST), 2);
            world.setBlockState(start.add(2, height + 2, z), Blocks.SPRUCE_SLAB.getDefaultState(), 2);
            world.setBlockState(start.add(3, height + 2, z), Blocks.SPRUCE_SLAB.getDefaultState(), 2);
        }

        // Gable Walls
        for (int z : new int[]{ 0, length - 1 }) {
            world.setBlockState(start.add(1, height + 1, z), ModBlocks.STARFRUIT_PLANKS.getDefaultState(), 2);
            world.setBlockState(start.add(2, height + 1, z), ModBlocks.STARFRUIT_PLANKS.getDefaultState(), 2);
            world.setBlockState(start.add(3, height + 1, z), ModBlocks.STARFRUIT_PLANKS.getDefaultState(), 2);
            world.setBlockState(start.add(4, height + 1, z), ModBlocks.STARFRUIT_PLANKS.getDefaultState(), 2);
        }

        placeDoor(world, start.add(width - 1, 1, length / 2), Direction.EAST);

        // Interior
        placeBed(world, start.add(1, 1, 2), Direction.NORTH, Blocks.YELLOW_BED);
        world.setBlockState(start.add(1, 1, length - 2), Blocks.FURNACE.getDefaultState(), 2);
        world.setBlockState(start.add(width / 2, height, length / 2), Blocks.LANTERN.getDefaultState().with(net.minecraft.block.LanternBlock.HANGING, true), 2);
    }

    private void buildHerbalistCottage(StructureWorldAccess world, BlockPos pos, Random random) {
        int width = 6;
        int length = 6;
        int height = 4;

        int baseY = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, pos.getX() + width / 2, pos.getZ() + length / 2) - 1;
        BlockPos start = new BlockPos(pos.getX(), baseY, pos.getZ());

        // Foundation & Floor
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                BlockPos p = start.add(x, 0, z);
                for (int dy = -7; dy <= 0; dy++) {
                    world.setBlockState(p.up(dy), Blocks.COBBLESTONE.getDefaultState(), 2);
                }
            }
        }

        // Walls (Avocado Logs & Starfruit Planks)
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                boolean isCorner = (x == 0 || x == width - 1) && (z == 0 || z == length - 1);
                boolean isEdge = (x == 0 || x == width - 1 || z == 0 || z == length - 1);

                for (int y = 1; y <= height; y++) {
                    BlockPos p = start.add(x, y, z);
                    if (isCorner) {
                        world.setBlockState(p, ModBlocks.AVOCADO_LOG.getDefaultState(), 2);
                    } else if (isEdge) {
                        // Doorway at south side (facing main street)
                        if (z == length - 1 && x == width / 2 && y <= 2) {
                            world.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
                        } else if (y == 2 && ((z == 0 && x == 3) || (x == 0 && z == 3) || (x == width - 1 && z == 3))) {
                            world.setBlockState(p, Blocks.GLASS_PANE.getDefaultState(), 2);
                        } else if (y == height) {
                            world.setBlockState(p, ModBlocks.AVOCADO_WOOD.getDefaultState(), 2);
                        } else {
                            world.setBlockState(p, ModBlocks.STARFRUIT_PLANKS.getDefaultState(), 2);
                        }
                    } else {
                        world.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
                    }
                }
            }
        }

        // Roof
        for (int x = -1; x <= width; x++) {
            world.setBlockState(start.add(x, height + 1, 0), Blocks.SPRUCE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH), 2);
            world.setBlockState(start.add(x, height + 2, 1), Blocks.SPRUCE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH), 2);
            world.setBlockState(start.add(x, height + 1, length - 1), Blocks.SPRUCE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH), 2);
            world.setBlockState(start.add(x, height + 2, length - 2), Blocks.SPRUCE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH), 2);
            world.setBlockState(start.add(x, height + 2, 2), Blocks.SPRUCE_SLAB.getDefaultState(), 2);
            world.setBlockState(start.add(x, height + 2, 3), Blocks.SPRUCE_SLAB.getDefaultState(), 2);
        }

        placeDoor(world, start.add(width / 2, 1, length - 1), Direction.SOUTH);

        // Interior: Brewing Stand, Cauldron, Botanical Chest
        placeBed(world, start.add(1, 1, 2), Direction.NORTH, Blocks.LIME_BED);
        world.setBlockState(start.add(width - 2, 1, 1), Blocks.BREWING_STAND.getDefaultState(), 2);
        world.setBlockState(start.add(width - 2, 1, 2), Blocks.CAULDRON.getDefaultState(), 2);
        world.setBlockState(start.add(1, 1, length - 2), Blocks.FLOWER_POT.getDefaultState(), 2);

        BlockPos chestPos = start.add(width - 2, 1, length - 2);
        world.setBlockState(chestPos, Blocks.CHEST.getDefaultState(), 2);
        BlockEntity be = world.getBlockEntity(chestPos);
        if (be instanceof ChestBlockEntity chest) {
            chest.setStack(0, new ItemStack(ModItems.AVOCADO, 3 + random.nextInt(4)));
            chest.setStack(1, new ItemStack(ModItems.WASABI_ROOT, 2 + random.nextInt(3)));
            chest.setStack(2, new ItemStack(Items.GLASS_BOTTLE, 4));
            chest.setStack(3, new ItemStack(Items.SUGAR, 5));
        }

        world.setBlockState(start.add(width / 2, height, length / 2), Blocks.LANTERN.getDefaultState().with(net.minecraft.block.LanternBlock.HANGING, true), 2);
    }

    private void buildLumberjackLodge(StructureWorldAccess world, BlockPos pos, Random random) {
        int width = 6;
        int length = 6;
        int height = 4;

        int baseY = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, pos.getX() + width / 2, pos.getZ() + length / 2) - 1;
        BlockPos start = new BlockPos(pos.getX(), baseY, pos.getZ());

        // Foundation
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                BlockPos p = start.add(x, 0, z);
                for (int dy = -7; dy <= 0; dy++) {
                    world.setBlockState(p.up(dy), Blocks.STONE_BRICKS.getDefaultState(), 2);
                }
            }
        }

        // Walls
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                boolean isCorner = (x == 0 || x == width - 1) && (z == 0 || z == length - 1);
                boolean isEdge = (x == 0 || x == width - 1 || z == 0 || z == length - 1);

                for (int y = 1; y <= height; y++) {
                    BlockPos p = start.add(x, y, z);
                    if (isCorner) {
                        world.setBlockState(p, ModBlocks.STARFRUIT_LOG.getDefaultState(), 2);
                    } else if (isEdge) {
                        if (z == length - 1 && x == width / 2 && y <= 2) {
                            world.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
                        } else if (y == 2 && ((z == 0 && x == 3) || (x == 0 && z == 3) || (x == width - 1 && z == 3))) {
                            world.setBlockState(p, Blocks.GLASS_PANE.getDefaultState(), 2);
                        } else if (y == height) {
                            world.setBlockState(p, ModBlocks.STARFRUIT_WOOD.getDefaultState(), 2);
                        } else {
                            world.setBlockState(p, ModBlocks.STARFRUIT_PLANKS.getDefaultState(), 2);
                        }
                    } else {
                        world.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
                    }
                }
            }
        }

        // Roof
        for (int x = -1; x <= width; x++) {
            world.setBlockState(start.add(x, height + 1, 0), Blocks.SPRUCE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH), 2);
            world.setBlockState(start.add(x, height + 2, 1), Blocks.SPRUCE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH), 2);
            world.setBlockState(start.add(x, height + 1, length - 1), Blocks.SPRUCE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH), 2);
            world.setBlockState(start.add(x, height + 2, length - 2), Blocks.SPRUCE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH), 2);
            world.setBlockState(start.add(x, height + 2, 2), Blocks.SPRUCE_SLAB.getDefaultState(), 2);
            world.setBlockState(start.add(x, height + 2, 3), Blocks.SPRUCE_SLAB.getDefaultState(), 2);
        }

        placeDoor(world, start.add(width / 2, 1, length - 1), Direction.SOUTH);

        // Interior: Stonecutter, Woodcrafts, Lumberjack Chest
        placeBed(world, start.add(1, 1, 2), Direction.NORTH, Blocks.BROWN_BED);
        world.setBlockState(start.add(width - 2, 1, 1), Blocks.STONECUTTER.getDefaultState(), 2);
        world.setBlockState(start.add(width - 2, 1, 2), ModBlocks.AVOCADO_LOG.getDefaultState(), 2);
        world.setBlockState(start.add(width - 2, 2, 2), ModBlocks.STARFRUIT_LOG.getDefaultState(), 2);

        BlockPos chestPos = start.add(1, 1, length - 2);
        world.setBlockState(chestPos, Blocks.CHEST.getDefaultState(), 2);
        BlockEntity be = world.getBlockEntity(chestPos);
        if (be instanceof ChestBlockEntity chest) {
            chest.setStack(0, new ItemStack(ModBlocks.STARFRUIT_PLANKS, 16));
            chest.setStack(1, new ItemStack(ModBlocks.AVOCADO_LOG, 8));
            chest.setStack(2, new ItemStack(Items.IRON_AXE, 1));
            chest.setStack(3, new ItemStack(Items.STICK, 12));
        }

        world.setBlockState(start.add(width / 2, height, length / 2), Blocks.LANTERN.getDefaultState().with(net.minecraft.block.LanternBlock.HANGING, true), 2);
    }

    private void buildWeaverCottage(StructureWorldAccess world, BlockPos pos, Random random) {
        int width = 6;
        int length = 6;
        int height = 4;

        int baseY = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, pos.getX() + width / 2, pos.getZ() + length / 2) - 1;
        BlockPos start = new BlockPos(pos.getX(), baseY, pos.getZ());

        // Foundation
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                BlockPos p = start.add(x, 0, z);
                for (int dy = -7; dy <= 0; dy++) {
                    world.setBlockState(p.up(dy), Blocks.COBBLESTONE.getDefaultState(), 2);
                }
            }
        }

        // Walls
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                boolean isCorner = (x == 0 || x == width - 1) && (z == 0 || z == length - 1);
                boolean isEdge = (x == 0 || x == width - 1 || z == 0 || z == length - 1);

                for (int y = 1; y <= height; y++) {
                    BlockPos p = start.add(x, y, z);
                    if (isCorner) {
                        world.setBlockState(p, ModBlocks.AVOCADO_LOG.getDefaultState(), 2);
                    } else if (isEdge) {
                        // Doorway facing west (towards center path)
                        if (x == 0 && z == length / 2 && y <= 2) {
                            world.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
                        } else if (y == 2 && ((x == width - 1 && z == length / 2) || (z == 0 && x == 3) || (z == length - 1 && x == 3))) {
                            world.setBlockState(p, Blocks.GLASS_PANE.getDefaultState(), 2);
                        } else if (y == height) {
                            world.setBlockState(p, ModBlocks.AVOCADO_WOOD.getDefaultState(), 2);
                        } else {
                            world.setBlockState(p, ModBlocks.STARFRUIT_PLANKS.getDefaultState(), 2);
                        }
                    } else {
                        world.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
                    }
                }
            }
        }

        // Roof
        for (int z = -1; z <= length; z++) {
            world.setBlockState(start.add(0, height + 1, z), Blocks.SPRUCE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.EAST), 2);
            world.setBlockState(start.add(1, height + 2, z), Blocks.SPRUCE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.EAST), 2);
            world.setBlockState(start.add(width - 1, height + 1, z), Blocks.SPRUCE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.WEST), 2);
            world.setBlockState(start.add(width - 2, height + 2, z), Blocks.SPRUCE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.WEST), 2);
            world.setBlockState(start.add(2, height + 2, z), Blocks.SPRUCE_SLAB.getDefaultState(), 2);
            world.setBlockState(start.add(3, height + 2, z), Blocks.SPRUCE_SLAB.getDefaultState(), 2);
        }

        placeDoor(world, start.add(0, 1, length / 2), Direction.WEST);

        // Interior: Loom, Colored Wool & Carpets
        placeBed(world, start.add(width - 2, 1, 2), Direction.NORTH, Blocks.CYAN_BED);
        world.setBlockState(start.add(width - 2, 1, length - 2), Blocks.LOOM.getDefaultState(), 2);
        world.setBlockState(start.add(2, 1, 2), Blocks.WHITE_CARPET.getDefaultState(), 2);
        world.setBlockState(start.add(3, 1, 2), Blocks.CYAN_CARPET.getDefaultState(), 2);

        BlockPos chestPos = start.add(1, 1, 1);
        world.setBlockState(chestPos, Blocks.CHEST.getDefaultState(), 2);
        BlockEntity be = world.getBlockEntity(chestPos);
        if (be instanceof ChestBlockEntity chest) {
            chest.setStack(0, new ItemStack(Items.WHITE_WOOL, 8));
            chest.setStack(1, new ItemStack(Items.SHEARS, 1));
            chest.setStack(2, new ItemStack(Items.STRING, 12));
            chest.setStack(3, new ItemStack(Items.CYAN_DYE, 4));
        }

        world.setBlockState(start.add(width / 2, height, length / 2), Blocks.LANTERN.getDefaultState().with(net.minecraft.block.LanternBlock.HANGING, true), 2);
    }

    private void buildFarmlandPlot(StructureWorldAccess world, BlockPos pos, Random random) {
        int baseY = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, pos.getX() + 3, pos.getZ() + 3) - 1;
        BlockPos start = new BlockPos(pos.getX(), baseY, pos.getZ());

        for (int x = 0; x < 7; x++) {
            for (int z = 0; z < 7; z++) {
                BlockPos p = start.add(x, 0, z);
                for (int dy = -2; dy < 0; dy++) {
                    world.setBlockState(p.up(dy), Blocks.DIRT.getDefaultState(), 2);
                }

                if (x == 0 || x == 6 || z == 0 || z == 6) {
                    world.setBlockState(p, ModBlocks.STARFRUIT_LOG.getDefaultState(), 2);
                } else if (x == 3 && z == 3) {
                    world.setBlockState(p, Blocks.WATER.getDefaultState(), 2);
                    world.setBlockState(p.up(1), Blocks.LILY_PAD.getDefaultState(), 2);
                } else {
                    world.setBlockState(p, Blocks.FARMLAND.getDefaultState().with(net.minecraft.block.FarmlandBlock.MOISTURE, 7), 2);
                    // Plant crops
                    if (random.nextBoolean()) {
                        world.setBlockState(p.up(1), ModBlocks.WILD_RICE.getDefaultState(), 2);
                    } else {
                        world.setBlockState(p.up(1), ModBlocks.WILD_CUCUMBER.getDefaultState(), 2);
                    }
                }
            }
        }

        // Composter on corner
        world.setBlockState(start.add(6, 1, 6), Blocks.COMPOSTER.getDefaultState(), 2);
    }

    private void buildBlacksmithWorkshop(StructureWorldAccess world, BlockPos pos, Random random) {
        int baseY = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, pos.getX() + 3, pos.getZ() + 3) - 1;
        BlockPos start = new BlockPos(pos.getX(), baseY, pos.getZ());

        // Foundation (down to -7)
        for (int x = 0; x < 6; x++) {
            for (int z = 0; z < 5; z++) {
                BlockPos p = start.add(x, 0, z);
                for (int dy = -7; dy <= 0; dy++) {
                    world.setBlockState(p.up(dy), Blocks.POLISHED_ANDESITE.getDefaultState(), 2);
                }
            }
        }

        // Pillars on 4 corners
        world.setBlockState(start.add(0, 1, 0), Blocks.STONE_BRICK_WALL.getDefaultState(), 2);
        world.setBlockState(start.add(0, 2, 0), Blocks.STONE_BRICK_WALL.getDefaultState(), 2);
        world.setBlockState(start.add(5, 1, 0), Blocks.STONE_BRICK_WALL.getDefaultState(), 2);
        world.setBlockState(start.add(5, 2, 0), Blocks.STONE_BRICK_WALL.getDefaultState(), 2);
        world.setBlockState(start.add(0, 1, 4), Blocks.STONE_BRICK_WALL.getDefaultState(), 2);
        world.setBlockState(start.add(0, 2, 4), Blocks.STONE_BRICK_WALL.getDefaultState(), 2);
        world.setBlockState(start.add(5, 1, 4), Blocks.STONE_BRICK_WALL.getDefaultState(), 2);
        world.setBlockState(start.add(5, 2, 4), Blocks.STONE_BRICK_WALL.getDefaultState(), 2);

        // Roof slab canopy
        for (int x = 0; x < 6; x++) {
            for (int z = 0; z < 5; z++) {
                world.setBlockState(start.add(x, 3, z), Blocks.STONE_BRICK_SLAB.getDefaultState(), 2);
            }
        }

        // Equipment: Anvil, Blast Furnace, Grindstone, Chest
        world.setBlockState(start.add(1, 1, 1), Blocks.ANVIL.getDefaultState(), 2);
        world.setBlockState(start.add(2, 1, 1), Blocks.BLAST_FURNACE.getDefaultState(), 2);
        world.setBlockState(start.add(3, 1, 1), Blocks.GRINDSTONE.getDefaultState(), 2);

        BlockPos chestPos = start.add(4, 1, 1);
        world.setBlockState(chestPos, Blocks.CHEST.getDefaultState(), 2);
        BlockEntity be = world.getBlockEntity(chestPos);
        if (be instanceof ChestBlockEntity chest) {
            chest.setStack(0, new ItemStack(Items.IRON_INGOT, 4));
            chest.setStack(1, new ItemStack(ModItems.TIN_INGOT, 6));
            chest.setStack(2, new ItemStack(Items.COPPER_INGOT, 8));
            chest.setStack(3, new ItemStack(Items.COAL, 12));
        }

        world.setBlockState(start.add(2, 2, 2), Blocks.LANTERN.getDefaultState().with(net.minecraft.block.LanternBlock.HANGING, true), 2);
    }

    private void buildAnimalPen(StructureWorldAccess world, BlockPos pos, Random random) {
        int width = 7;
        int length = 6;
        int baseY = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, pos.getX() + width / 2, pos.getZ() + length / 2) - 1;
        BlockPos start = new BlockPos(pos.getX(), baseY, pos.getZ());

        // Floor
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                BlockPos p = start.add(x, 0, z);
                for (int dy = -4; dy <= 0; dy++) {
                    world.setBlockState(p.up(dy), (dy == 0 && random.nextBoolean()) ? Blocks.COARSE_DIRT.getDefaultState() : Blocks.DIRT.getDefaultState(), 2);
                }
            }
        }

        // Fence perimeter
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                if (x == 0 || x == width - 1 || z == 0 || z == length - 1) {
                    if (z == 0 && x == width / 2) {
                        // Gate facing north (towards village path)
                        world.setBlockState(start.add(x, 1, z), Blocks.SPRUCE_FENCE_GATE.getDefaultState(), 2);
                    } else {
                        world.setBlockState(start.add(x, 1, z), Blocks.SPRUCE_FENCE.getDefaultState(), 2);
                    }
                }
            }
        }

        // Hay bales and water trough
        world.setBlockState(start.add(1, 1, length - 2), Blocks.HAY_BLOCK.getDefaultState(), 2);
        world.setBlockState(start.add(2, 1, length - 2), Blocks.HAY_BLOCK.getDefaultState(), 2);
        world.setBlockState(start.add(1, 2, length - 2), Blocks.HAY_BLOCK.getDefaultState(), 2);
        world.setBlockState(start.add(width - 2, 1, length - 2), Blocks.CAULDRON.getDefaultState(), 2);

        // Corner lamp post
        world.setBlockState(start.add(0, 2, 0), Blocks.SPRUCE_FENCE.getDefaultState(), 2);
        world.setBlockState(start.add(0, 3, 0), Blocks.LANTERN.getDefaultState(), 2);

        // Spawn 2 Sheep or 2 Cows
        int animalX = start.getX() + 3;
        int animalZ = start.getZ() + 3;
        int animalY = start.getY() + 1;
        if (random.nextBoolean()) {
            for (int i = 0; i < 2; i++) {
                SheepEntity sheep = EntityType.SHEEP.create(world.toServerWorld(), SpawnReason.STRUCTURE);
                if (sheep != null) {
                    sheep.refreshPositionAndAngles(animalX + (i * 1.2), animalY, animalZ, 0.0F, 0.0F);
                    world.spawnEntity(sheep);
                }
            }
        } else {
            for (int i = 0; i < 2; i++) {
                CowEntity cow = EntityType.COW.create(world.toServerWorld(), SpawnReason.STRUCTURE);
                if (cow != null) {
                    cow.refreshPositionAndAngles(animalX + (i * 1.2), animalY, animalZ, 0.0F, 0.0F);
                    world.spawnEntity(cow);
                }
            }
        }
    }

    private void placeStreetLamps(StructureWorldAccess world, BlockPos center) {
        BlockPos[] lampLocations = {
                center.add(4, 0, -4),
                center.add(-4, 0, -4),
                center.add(4, 0, 4),
                center.add(-4, 0, 4)
        };

        for (BlockPos loc : lampLocations) {
            int y = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, loc.getX(), loc.getZ()) - 1;
            BlockPos base = new BlockPos(loc.getX(), y, loc.getZ());
            if (world.getBlockState(base).isOf(Blocks.GRASS_BLOCK) || world.getBlockState(base).isOf(Blocks.DIRT) || world.getBlockState(base).isOf(Blocks.DIRT_PATH)) {
                world.setBlockState(base.up(1), Blocks.SPRUCE_FENCE.getDefaultState(), 2);
                world.setBlockState(base.up(2), Blocks.SPRUCE_FENCE.getDefaultState(), 2);
                world.setBlockState(base.up(3), Blocks.LANTERN.getDefaultState(), 2);
            }
        }
    }

    private void buildPathNetwork(StructureWorldAccess world, BlockPos center) {
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
                int y = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, x, z) - 1;
                BlockPos p = new BlockPos(x, y, z);
                if (world.getBlockState(p).isOf(Blocks.GRASS_BLOCK) || world.getBlockState(p).isOf(Blocks.DIRT)) {
                    world.setBlockState(p, Blocks.DIRT_PATH.getDefaultState(), 2);
                }
            }
        }
    }

    private void spawnInhabitants(StructureWorldAccess world, BlockPos center) {
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
            int y = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, x, z) + 1;
            VillagerEntity villager = EntityType.VILLAGER.create(world.toServerWorld(), SpawnReason.STRUCTURE);
            if (villager != null) {
                villager.refreshPositionAndAngles(x + 0.5, y, z + 0.5, 0.0F, 0.0F);
                villager.initialize(world, world.getLocalDifficulty(new BlockPos(x, y, z)), SpawnReason.STRUCTURE, null);
                world.spawnEntity(villager);
            }
        }

        // Spawn 1 Village Iron Golem Protector
        int golemY = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, center.getX() + 2, center.getZ() + 2) + 1;
        IronGolemEntity golem = EntityType.IRON_GOLEM.create(world.toServerWorld(), SpawnReason.STRUCTURE);
        if (golem != null) {
            golem.refreshPositionAndAngles(center.getX() + 2.5, golemY, center.getZ() + 2.5, 0.0F, 0.0F);
            world.spawnEntity(golem);
        }
    }
}
