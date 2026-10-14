package net.enchantedwood.world.gen.mystical.feature;

import com.mojang.serialization.Codec;
import net.enchantedwood.world.gen.mystical.tree.TreeBuilder;
import net.enchantedwood.world.gen.mystical.tree.TreeParams;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

public class GrandTreeFeature extends Feature<GrandTreeConfiguration> {
    public GrandTreeFeature(Codec<GrandTreeConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<GrandTreeConfiguration> context) {
        StructureWorldAccess level = context.getWorld();
        GrandTreeConfiguration config = context.getConfig();
        Random random = context.getRandom();
        BlockPos origin = context.getOrigin();
        ChunkPos chunk = new ChunkPos(origin);
        int reach = Math.min(config.reach(), 23);

        if (reach > 15) {
            int lo = reach - 16;
            int hi = 31 - reach;
            int x = chunk.getStartX() + lo + random.nextInt(hi - lo + 1);
            int z = chunk.getStartZ() + lo + random.nextInt(hi - lo + 1);
            BlockPos moved = new BlockPos(x, level.getTopY(Heightmap.Type.OCEAN_FLOOR_WG, x, z) + 1, z);
            if (!level.getBiome(moved).equals(level.getBiome(origin))) {
                return false;
            }
            origin = moved;
        }

        BlockPos base = ground(level, context.getGenerator(), origin, config.aquatic());
        if (base != null && (config.aquatic() || level.getFluidState(base).isEmpty()) && !crowded(level, base)) {
            int ceiling = level.getTopYInclusive() - base.getY();
            if (level.getDimension().hasCeiling()) {
                ceiling = Math.min(ceiling, headroom(level, base));
            }

            if (ceiling < 8) {
                return false;
            }

            BlockBox box = new BlockBox(
                chunk.getStartX() - 16, level.getBottomY(), chunk.getStartZ() - 16,
                chunk.getEndX() + 16, level.getTopYInclusive(), chunk.getEndZ() + 16
            );
            TreeParams params = config.sample(random, reach, ceiling);
            TreePlacer.place(level, config, TreeBuilder.build(params), base, params.seed(), box);
            return true;
        } else {
            return false;
        }
    }

    private static boolean crowded(StructureWorldAccess level, BlockPos base) {
        BlockPos.Mutable pos = new BlockPos.Mutable();

        for (int dx = -4; dx <= 4; dx += 2) {
            for (int dz = -4; dz <= 4; dz += 2) {
                if (isWood(level.getBlockState(pos.set(base, dx, 1, dz))) && isWood(level.getBlockState(pos.set(base, dx, 2, dz)))) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isWood(BlockState state) {
        return state.isIn(BlockTags.LOGS);
    }

    private static int headroom(StructureWorldAccess level, BlockPos base) {
        BlockPos.Mutable pos = base.mutableCopy();

        for (int dy = 0; dy < 96; dy++) {
            BlockState state = level.getBlockState(pos.set(base, 0, dy, 0));
            if (!state.isAir() && !state.isIn(BlockTags.LEAVES) && !state.isIn(BlockTags.LOGS)) {
                return dy - 2;
            }
        }

        return 96;
    }

    private static boolean isFoliageOrMushroom(BlockState state) {
        if (state.isAir()) return true;
        if (state.isIn(BlockTags.LEAVES)) return true;
        if (state.isIn(BlockTags.LOGS)) return true;
        if (state.isIn(BlockTags.FLOWERS)) return true;
        if (state.isIn(BlockTags.REPLACEABLE_BY_TREES)) return true;
        var block = state.getBlock();
        return block == net.minecraft.block.Blocks.BROWN_MUSHROOM_BLOCK
            || block == net.minecraft.block.Blocks.RED_MUSHROOM_BLOCK
            || block == net.minecraft.block.Blocks.MUSHROOM_STEM
            || block == net.minecraft.block.Blocks.SHROOMLIGHT
            || block == net.minecraft.block.Blocks.VINE
            || block == net.minecraft.block.Blocks.CAVE_VINES
            || block == net.minecraft.block.Blocks.CAVE_VINES_PLANT;
    }

    private static boolean isValidSoil(BlockState state, boolean aquatic) {
        if (state.isIn(BlockTags.DIRT)
            || state.isIn(BlockTags.SAND)
            || state.isIn(BlockTags.BASE_STONE_OVERWORLD)
            || state.isIn(BlockTags.NYLIUM)
            || state.isIn(BlockTags.TERRACOTTA)
            || state.isOf(net.minecraft.block.Blocks.MUD)
            || state.isOf(net.minecraft.block.Blocks.MUDDY_MANGROVE_ROOTS)) {
            return true;
        }
        if (aquatic && (state.isIn(BlockTags.SAND) || state.isOf(net.minecraft.block.Blocks.GRAVEL) || state.isOf(net.minecraft.block.Blocks.CLAY))) {
            return true;
        }
        return false;
    }

    private static BlockPos ground(StructureWorldAccess level, ChunkGenerator generator, BlockPos start, boolean aquatic) {
        BlockPos.Mutable pos = start.mutableCopy();

        for (int i = 0; i < 64 && pos.getY() > level.getBottomY(); i++) {
            BlockPos below = pos.down();
            BlockState state = level.getBlockState(below);

            if (isFoliageOrMushroom(state)) {
                pos.move(Direction.DOWN);
                continue;
            }

            if (isValidSoil(state, aquatic)) {
                if (state.isSideSolidFullSquare(level, below, Direction.UP)) {
                    return pos.toImmutable();
                }
                return null;
            }

            return null;
        }

        return null;
    }
}
