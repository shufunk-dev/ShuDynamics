package com.vexira.mysticalterrain.feature;

import com.mojang.serialization.MapCodec;
import com.vexira.mysticalterrain.tree.TreeBuilder;
import com.vexira.mysticalterrain.tree.TreeParams;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class GrandTreeFeature implements Feature {
    public static final MapCodec<GrandTreeFeature> CODEC = GrandTreeConfiguration.CODEC.fieldOf("config")
        .xmap(GrandTreeFeature::new, GrandTreeFeature::config);

    private final GrandTreeConfiguration config;

    public GrandTreeFeature(GrandTreeConfiguration config) {
        this.config = config;
    }

    public GrandTreeConfiguration config() {
        return this.config;
    }

    @Override
    public MapCodec<? extends Feature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        ChunkPos chunk = ChunkPos.containing(origin);
        int reach = Math.min(this.config.reach(), 23);

        if (reach > 15) {
            int lo = reach - 16;
            int hi = 31 - reach;
            int x = chunk.getMinBlockX() + lo + random.nextInt(hi - lo + 1);
            int z = chunk.getMinBlockZ() + lo + random.nextInt(hi - lo + 1);
            BlockPos moved = new BlockPos(x, level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) + 1, z);
            if (!level.getBiome(moved).equals(level.getBiome(origin))) {
                return false;
            }
            origin = moved;
        }

        BlockPos base = ground(level, generator, origin, this.config.aquatic());
        if (base != null && (this.config.aquatic() || level.getFluidState(base).isEmpty()) && !crowded(level, base)) {
            int ceiling = level.getMaxY() - base.getY();
            if (level.dimensionType().hasCeiling()) {
                ceiling = Math.min(ceiling, headroom(level, base));
            }

            if (ceiling < 8) {
                return false;
            }

            BoundingBox box = new BoundingBox(
                chunk.getMinBlockX() - 16, level.getMinY(), chunk.getMinBlockZ() - 16,
                chunk.getMaxBlockX() + 16, level.getMaxY(), chunk.getMaxBlockZ() + 16
            );
            TreeParams params = this.config.sample(random, reach, ceiling);
            TreePlacer.place(level, this.config, TreeBuilder.build(params), base, params.seed(), box);
            return true;
        } else {
            return false;
        }
    }

    private static boolean crowded(WorldGenLevel level, BlockPos base) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dx = -4; dx <= 4; dx += 2) {
            for (int dz = -4; dz <= 4; dz += 2) {
                if (isWood(level.getBlockState(pos.setWithOffset(base, dx, 1, dz))) && isWood(level.getBlockState(pos.setWithOffset(base, dx, 2, dz)))) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isWood(BlockState state) {
        return state.is(BlockTags.LOGS);
    }

    private static int headroom(WorldGenLevel level, BlockPos base) {
        BlockPos.MutableBlockPos pos = base.mutable();

        for (int dy = 0; dy < 96; dy++) {
            BlockState state = level.getBlockState(pos.setWithOffset(base, 0, dy, 0));
            if (!state.isAir() && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS)) {
                return dy - 2;
            }
        }

        return 96;
    }

    private static boolean isFoliageOrMushroom(BlockState state) {
        if (state.isAir()) return true;
        if (state.is(BlockTags.LEAVES)) return true;
        if (state.is(BlockTags.LOGS)) return true;
        if (state.is(BlockTags.FLOWERS)) return true;
        if (state.is(BlockTags.REPLACEABLE_BY_TREES)) return true;
        var block = state.getBlock();
        return block == net.minecraft.world.level.block.Blocks.BROWN_MUSHROOM_BLOCK
            || block == net.minecraft.world.level.block.Blocks.RED_MUSHROOM_BLOCK
            || block == net.minecraft.world.level.block.Blocks.MUSHROOM_STEM
            || block == net.minecraft.world.level.block.Blocks.SHROOMLIGHT
            || block == net.minecraft.world.level.block.Blocks.VINE
            || block == net.minecraft.world.level.block.Blocks.CAVE_VINES
            || block == net.minecraft.world.level.block.Blocks.CAVE_VINES_PLANT;
    }

    private static boolean isValidSoil(BlockState state, boolean aquatic) {
        if (state.is(BlockTags.DIRT)
            || state.is(BlockTags.SAND)
            || state.is(BlockTags.BASE_STONE_OVERWORLD)
            || state.is(BlockTags.NYLIUM)
            || state.is(BlockTags.TERRACOTTA)
            || state.is(net.minecraft.world.level.block.Blocks.MUD)
            || state.is(net.minecraft.world.level.block.Blocks.MUDDY_MANGROVE_ROOTS)) {
            return true;
        }
        if (aquatic && (state.is(BlockTags.SAND) || state.is(net.minecraft.world.level.block.Blocks.GRAVEL) || state.is(net.minecraft.world.level.block.Blocks.CLAY))) {
            return true;
        }
        return false;
    }

    private static BlockPos ground(WorldGenLevel level, ChunkGenerator generator, BlockPos start, boolean aquatic) {
        BlockPos.MutableBlockPos pos = start.mutable();

        for (int i = 0; i < 64 && pos.getY() > level.getMinY(); i++) {
            BlockPos below = pos.below();
            BlockState state = level.getBlockState(below);

            if (isFoliageOrMushroom(state)) {
                pos.move(Direction.DOWN);
                continue;
            }

            if (isValidSoil(state, aquatic)) {
                if (state.isFaceSturdy(level, below, Direction.UP)) {
                    return pos.immutable();
                }
                return null;
            }

            return null;
        }

        return null;
    }
}
