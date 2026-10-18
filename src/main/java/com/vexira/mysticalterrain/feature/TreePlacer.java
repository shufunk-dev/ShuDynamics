package com.vexira.mysticalterrain.feature;

import com.vexira.mysticalterrain.tree.Noise;
import com.vexira.mysticalterrain.tree.Part;
import com.vexira.mysticalterrain.tree.Sink;
import com.vexira.mysticalterrain.tree.Tree;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVinesBlock;
import net.minecraft.world.level.block.CaveVinesPlantBlock;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public final class TreePlacer implements Sink {
    private static final Direction.Axis[] AXES = new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Y, Direction.Axis.Z};
    private final WorldGenLevel level;
    private final TreeBlocks blocks;
    private final BlockPos base;
    private final long seed;
    private final BoundingBox box;
    private final double foot;
    private final Map<Long, Integer> grounds = new HashMap<>();
    private final RandomSource random = RandomSource.create();
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    private long[] leaves = new long[512];
    private int leafCount;

    private TreePlacer(WorldGenLevel level, TreeBlocks blocks, BlockPos base, long seed, BoundingBox box, double foot) {
        this.level = level;
        this.blocks = blocks;
        this.base = base;
        this.seed = seed;
        this.box = box;
        this.foot = foot;
    }

    public static void place(WorldGenLevel level, GrandTreeConfiguration config, Tree tree, BlockPos base, long seed, BoundingBox box) {
        TreePlacer placer = new TreePlacer(level, config.blocks(), base, seed, box, tree.foot());
        tree.rasterize(
            box.minX() - base.getX(),
            box.minY() - base.getY(),
            box.minZ() - base.getZ(),
            box.maxX() - base.getX(),
            box.maxY() - base.getY(),
            box.maxZ() - base.getZ(),
            placer
        );
        placer.hang();
        placer.cover(tree);
    }

    @Override
    public void place(int x, int y, int z, Part part, int axis) {
        if (part == Part.ROOT) {
            int shift = this.drape(x, y, z);
            if (shift == Integer.MIN_VALUE) {
                return;
            }
            y += shift;
        }

        this.pos.set(this.base.getX() + x, this.base.getY() + y, this.base.getZ() + z);
        if (!this.level.isOutsideBuildHeight(this.pos)) {
            BlockState current = this.level.getBlockState(this.pos);
            switch (part) {
                case ROOT:
                case TRUNK:
                case BRANCH:
                    boolean buried = part == Part.ROOT || (part == Part.TRUNK && y <= 1);
                    if (isSoft(current) || current.is(BlockTags.LOGS) || (buried && current.is(BlockTags.BASE_STONE_OVERWORLD))) {
                        BlockState state = this.wood(part);
                        if (state.hasProperty(RotatedPillarBlock.AXIS)) {
                            state = state.setValue(RotatedPillarBlock.AXIS, AXES[axis]);
                        }
                        this.set(state);
                    }
                    break;
                case VEIN:
                    if (this.blocks.vein().isPresent()
                        && !current.isAir()
                        && (current.is(this.wood(Part.TRUNK).getBlock())
                            || current.is(this.wood(Part.ROOT).getBlock())
                            || current.is(this.wood(Part.BRANCH).getBlock()))) {
                        this.set(this.pick(this.blocks.vein().get()));
                    }
                    break;
                case LEAVES:
                    if (this.blocks.leaves().isPresent() && isSoft(current)) {
                        boolean fruit = this.blocks.fruit().isPresent() && Noise.unit(this.seed + 99L, x, y, z) < this.blocks.fruitChance();
                        this.set(this.settle(this.pick(fruit ? this.blocks.fruit().get() : this.blocks.leaves().get()), current));
                        this.remember(this.pos);
                    }
                    break;
                case FRUIT:
                    if (this.blocks.fruit().isPresent() && isSoft(current)) {
                        this.set(this.settle(this.pick(this.blocks.fruit().get()), current));
                    }
                    break;
            }
        }
    }

    private int drape(int x, int y, int z) {
        int ground = this.ground(this.base.getX() + x, this.base.getZ() + z);
        double w = Math.min(1.0, Math.max(0.0, (Math.sqrt(x * x + z * z) - this.foot) / 3.0));
        int dy = Math.max(-8, Math.min(5, ground - (this.base.getY() - 1)));
        int shift = (int)Math.round(dy * w);
        return y <= 2 && this.base.getY() + y + shift - ground > 2 ? Integer.MIN_VALUE : shift;
    }

    private int ground(int x, int z) {
        return this.grounds.computeIfAbsent(BlockPos.asLong(x, 0, z), key -> this.scan(x, z));
    }

    private int scan(int x, int z) {
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(x, this.base.getY() + 8, z);

        while (at.getY() > this.base.getY() - 17) {
            BlockState state = this.level.getBlockState(at);
            if (!isSoft(state) && !isTree(state)) {
                return at.getY();
            }
            at.move(Direction.DOWN);
        }

        return this.base.getY() - 17;
    }

    private BlockState wood(Part part) {
        return this.pick(switch (part) {
            case ROOT -> this.blocks.rootOrTrunk();
            case BRANCH -> this.blocks.branchOrTrunk();
            default -> this.blocks.trunk();
        });
    }

    private BlockState pick(BlockStateProvider provider) {
        this.random.setSeed(Noise.hash(this.seed, this.pos.getX(), this.pos.getY(), this.pos.getZ()));
        return provider.getState(this.level, this.random, this.pos);
    }

    private BlockState settle(BlockState state, BlockState current) {
        if (state.hasProperty(LeavesBlock.PERSISTENT)) {
            state = state.setValue(LeavesBlock.PERSISTENT, true);
        }
        if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
            state = state.setValue(BlockStateProperties.WATERLOGGED, current.getFluidState().is(FluidTags.WATER) && current.getFluidState().isSource());
        }
        return state;
    }

    private void set(BlockState state) {
        this.level.setBlock(this.pos, state, Block.UPDATE_CLIENTS);
    }

    private void remember(BlockPos leaf) {
        if (this.leafCount == this.leaves.length) {
            this.leaves = Arrays.copyOf(this.leaves, this.leafCount * 2);
        }
        this.leaves[this.leafCount++] = leaf.asLong();
    }

    private void hang() {
        if (this.blocks.hanging().isPresent()) {
            for (int i = 0; i < this.leafCount; i++) {
                this.pos.set(this.leaves[i]);
                if (Noise.unit(this.seed + 7L, this.pos.getX(), this.pos.getY(), this.pos.getZ()) < this.blocks.hangingChance()) {
                    BlockState state = this.pick(this.blocks.hanging().get());
                    int length = this.blocks.hangingLength().sample(this.random);
                    if (state.is(Blocks.VINE)) {
                        Direction side = Direction.from2DDataValue((int)(Noise.hash(this.seed + 8L, this.pos.getX(), this.pos.getY(), this.pos.getZ()) & 3L));
                        this.chain(this.pos.relative(side), state.setValue(VineBlock.getPropertyForFace(side.getOpposite()), true), length);
                    } else {
                        this.chain(this.pos.below(), state, length);
                    }
                }
            }
        }
    }

    private void chain(BlockPos start, BlockState state, int length) {
        BlockPos.MutableBlockPos at = start.mutable();
        int n = 0;

        while (n < length && this.box.isInside(at) && this.level.isEmptyBlock(at)) {
            n++;
            at.move(Direction.DOWN);
        }

        if (n != 0 && (n == 1 ? tip(state) : body(state)).canSurvive(this.level, start)) {
            at.set(start);

            for (int i = 0; i < n; i++) {
                this.level.setBlock(at, i == n - 1 ? tip(state) : body(state), Block.UPDATE_CLIENTS);
                at.move(Direction.DOWN);
            }
        }
    }

    private static BlockState body(BlockState state) {
        if (state.is(Blocks.CAVE_VINES)) {
            return Blocks.CAVE_VINES_PLANT.defaultBlockState().setValue(CaveVinesPlantBlock.BERRIES, state.getValue(CaveVinesBlock.BERRIES));
        } else if (state.is(Blocks.WEEPING_VINES)) {
            return Blocks.WEEPING_VINES_PLANT.defaultBlockState();
        } else {
            return state.hasProperty(GrowingPlantHeadBlock.AGE) ? state.setValue(GrowingPlantHeadBlock.AGE, 0) : state;
        }
    }

    private static BlockState tip(BlockState state) {
        return state.hasProperty(GrowingPlantHeadBlock.AGE) ? state.setValue(GrowingPlantHeadBlock.AGE, 25) : state;
    }

    private void cover(Tree tree) {
        if (this.blocks.cover().isPresent()) {
            int reach = tree.reach();
            int x0 = Math.max(this.box.minX(), this.base.getX() - reach);
            int x1 = Math.min(this.box.maxX(), this.base.getX() + reach);
            int z0 = Math.max(this.box.minZ(), this.base.getZ() - reach);
            int z1 = Math.min(this.box.maxZ(), this.base.getZ() + reach);
            double limit = reach * 0.85;

            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    double dx = x - this.base.getX();
                    double dz = z - this.base.getZ();
                    if (!(dx * dx + dz * dz > limit * limit) && (Noise.unit(this.seed + 11L, x, 0, z) < this.blocks.coverChance())) {
                        for (int y = this.base.getY() + 6; y > this.base.getY() - 10; y--) {
                            this.pos.set(x, y - 1, z);
                            BlockState ground = this.level.getBlockState(this.pos);
                            if (!isSoft(ground)) {
                                this.pos.set(x, y, z);
                                BlockState here = this.level.getBlockState(this.pos);
                                if (here.isAir() && !ground.is(BlockTags.LOGS)) {
                                    BlockState state = this.pick(this.blocks.cover().get());
                                    if (state.canSurvive(this.level, this.pos)) {
                                        this.set(state);
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    static boolean isTree(BlockState state) {
        return state.is(BlockTags.LOGS)
            || state.is(BlockTags.LEAVES)
            || state.is(Blocks.BROWN_MUSHROOM_BLOCK)
            || state.is(Blocks.RED_MUSHROOM_BLOCK)
            || state.is(Blocks.MUSHROOM_STEM)
            || state.is(Blocks.SHROOMLIGHT);
    }

    private static boolean isSoft(BlockState state) {
        return state.isAir()
            || state.is(BlockTags.REPLACEABLE_BY_TREES)
            || state.is(BlockTags.LEAVES)
            || state.is(BlockTags.FLOWERS)
            || state.is(BlockTags.DIRT)
            || state.is(Blocks.BROWN_MUSHROOM_BLOCK)
            || state.is(Blocks.RED_MUSHROOM_BLOCK)
            || state.is(Blocks.MUSHROOM_STEM)
            || state.is(Blocks.SHROOMLIGHT);
    }
}
