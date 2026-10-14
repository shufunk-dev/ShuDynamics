package net.enchantedwood.world.gen.mystical.feature;

import net.enchantedwood.world.gen.mystical.tree.Noise;
import net.enchantedwood.world.gen.mystical.tree.Part;
import net.enchantedwood.world.gen.mystical.tree.Sink;
import net.enchantedwood.world.gen.mystical.tree.Tree;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.AbstractPlantStemBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CaveVines;
import net.minecraft.block.CaveVinesBodyBlock;
import net.minecraft.block.CaveVinesHeadBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;

final class TreePlacer implements Sink {
    private static final Direction.Axis[] AXES = new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Y, Direction.Axis.Z};
    private final StructureWorldAccess level;
    private final TreeBlocks blocks;
    private final BlockPos base;
    private final long seed;
    private final BlockBox box;
    private final double foot;
    private final Map<Long, Integer> grounds = new HashMap<>();
    private final Random random = Random.create();
    private final BlockPos.Mutable pos = new BlockPos.Mutable();
    private long[] leaves = new long[512];
    private int leafCount;

    private TreePlacer(StructureWorldAccess level, TreeBlocks blocks, BlockPos base, long seed, BlockBox box, double foot) {
        this.level = level;
        this.blocks = blocks;
        this.base = base;
        this.seed = seed;
        this.box = box;
        this.foot = foot;
    }

    static void place(StructureWorldAccess level, GrandTreeConfiguration config, Tree tree, BlockPos base, long seed, BlockBox box) {
        TreePlacer placer = new TreePlacer(level, config.blocks(), base, seed, box, tree.foot());
        tree.rasterize(
            box.getMinX() - base.getX(),
            box.getMinY() - base.getY(),
            box.getMinZ() - base.getZ(),
            box.getMaxX() - base.getX(),
            box.getMaxY() - base.getY(),
            box.getMaxZ() - base.getZ(),
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
        if (!this.level.isOutOfHeightLimit(this.pos)) {
            BlockState current = this.level.getBlockState(this.pos);
            switch (part) {
                case ROOT:
                case TRUNK:
                case BRANCH:
                    boolean buried = part == Part.ROOT || (part == Part.TRUNK && y <= 1);
                    if (isSoft(current) || current.isIn(BlockTags.LOGS) || (buried && current.isIn(BlockTags.BASE_STONE_OVERWORLD))) {
                        BlockState state = this.wood(part);
                        if (state.contains(PillarBlock.AXIS)) {
                            state = state.with(PillarBlock.AXIS, AXES[axis]);
                        }
                        this.set(state);
                    }
                    break;
                case VEIN:
                    if (this.blocks.vein().isPresent()
                        && !current.isAir()
                        && (current.isOf(this.wood(Part.TRUNK).getBlock())
                            || current.isOf(this.wood(Part.ROOT).getBlock())
                            || current.isOf(this.wood(Part.BRANCH).getBlock()))) {
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
        BlockPos.Mutable at = new BlockPos.Mutable(x, this.base.getY() + 8, z);

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
        return provider.get(this.random, this.pos);
    }

    private BlockState settle(BlockState state, BlockState current) {
        if (state.contains(LeavesBlock.PERSISTENT)) {
            state = state.with(LeavesBlock.PERSISTENT, true);
        }
        if (state.contains(Properties.WATERLOGGED)) {
            state = state.with(Properties.WATERLOGGED, current.getFluidState().isIn(FluidTags.WATER) && current.getFluidState().isStill());
        }
        return state;
    }

    private void set(BlockState state) {
        this.level.setBlockState(this.pos, state, Block.NOTIFY_LISTENERS);
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
                    int length = this.blocks.hangingLength().get(this.random);
                    if (state.isOf(Blocks.VINE)) {
                        Direction side = Direction.fromHorizontalQuarterTurns((int)(Noise.hash(this.seed + 8L, this.pos.getX(), this.pos.getY(), this.pos.getZ()) & 3L));
                        this.chain(this.pos.offset(side), state.with(VineBlock.getFacingProperty(side.getOpposite()), true), length);
                    } else {
                        this.chain(this.pos.down(), state, length);
                    }
                }
            }
        }
    }

    private void chain(BlockPos start, BlockState state, int length) {
        BlockPos.Mutable at = start.mutableCopy();
        int n = 0;

        while (n < length && this.box.contains(at) && this.level.isAir(at)) {
            n++;
            at.move(Direction.DOWN);
        }

        if (n != 0 && (n == 1 ? tip(state) : body(state)).canPlaceAt(this.level, start)) {
            at.set(start);

            for (int i = 0; i < n; i++) {
                this.level.setBlockState(at, i == n - 1 ? tip(state) : body(state), Block.NOTIFY_LISTENERS);
                at.move(Direction.DOWN);
            }
        }
    }

    private static BlockState body(BlockState state) {
        if (state.isOf(Blocks.CAVE_VINES)) {
            return Blocks.CAVE_VINES_PLANT.getDefaultState().with(CaveVinesBodyBlock.BERRIES, state.get(CaveVinesHeadBlock.BERRIES));
        } else if (state.isOf(Blocks.WEEPING_VINES)) {
            return Blocks.WEEPING_VINES_PLANT.getDefaultState();
        } else {
            return state.contains(AbstractPlantStemBlock.AGE) ? state.with(AbstractPlantStemBlock.AGE, 0) : state;
        }
    }

    private static BlockState tip(BlockState state) {
        return state.contains(AbstractPlantStemBlock.AGE) ? state.with(AbstractPlantStemBlock.AGE, 25) : state;
    }

    private void cover(Tree tree) {
        if (this.blocks.cover().isPresent()) {
            int reach = tree.reach();
            int x0 = Math.max(this.box.getMinX(), this.base.getX() - reach);
            int x1 = Math.min(this.box.getMaxX(), this.base.getX() + reach);
            int z0 = Math.max(this.box.getMinZ(), this.base.getZ() - reach);
            int z1 = Math.min(this.box.getMaxZ(), this.base.getZ() + reach);
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
                                if (here.isAir() && !ground.isIn(BlockTags.LOGS)) {
                                    BlockState state = this.pick(this.blocks.cover().get());
                                    if (state.canPlaceAt(this.level, this.pos)) {
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
        return state.isIn(BlockTags.LOGS) || state.isIn(BlockTags.LEAVES);
    }

    private static boolean isSoft(BlockState state) {
        return state.isAir()
            || state.isIn(BlockTags.REPLACEABLE_BY_TREES)
            || state.isIn(BlockTags.LEAVES)
            || state.isIn(BlockTags.FLOWERS)
            || state.isIn(BlockTags.DIRT);
    }
}
