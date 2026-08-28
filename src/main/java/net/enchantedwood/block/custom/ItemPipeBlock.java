package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.enchantedwood.block.entity.ItemExtractorBlockEntity;
import net.enchantedwood.block.entity.ItemInserterBlockEntity;
import net.enchantedwood.block.entity.ItemPipeBlockEntity;
import net.enchantedwood.block.entity.ModBlockEntities;
import net.enchantedwood.util.Wrenchable;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;

public class ItemPipeBlock extends BlockWithEntity implements Wrenchable {
    public static final MapCodec<ItemPipeBlock> CODEC = createCodec(ItemPipeBlock::new);

    public static final EnumProperty<PipeSide> NORTH = EnumProperty.of("north", PipeSide.class);
    public static final EnumProperty<PipeSide> SOUTH = EnumProperty.of("south", PipeSide.class);
    public static final EnumProperty<PipeSide> EAST = EnumProperty.of("east", PipeSide.class);
    public static final EnumProperty<PipeSide> WEST = EnumProperty.of("west", PipeSide.class);
    public static final EnumProperty<PipeSide> UP = EnumProperty.of("up", PipeSide.class);
    public static final EnumProperty<PipeSide> DOWN = EnumProperty.of("down", PipeSide.class);

    private static final VoxelShape CORE_SHAPE = Block.createCuboidShape(5.0, 5.0, 5.0, 11.0, 11.0, 11.0);
    private static final VoxelShape NORTH_SHAPE = Block.createCuboidShape(5.0, 5.0, 0.0, 11.0, 11.0, 5.0);
    private static final VoxelShape SOUTH_SHAPE = Block.createCuboidShape(5.0, 5.0, 11.0, 11.0, 11.0, 16.0);
    private static final VoxelShape EAST_SHAPE = Block.createCuboidShape(11.0, 5.0, 5.0, 16.0, 11.0, 11.0);
    private static final VoxelShape WEST_SHAPE = Block.createCuboidShape(0.0, 5.0, 5.0, 5.0, 11.0, 11.0);
    private static final VoxelShape UP_SHAPE = Block.createCuboidShape(5.0, 11.0, 5.0, 11.0, 16.0, 11.0);
    private static final VoxelShape DOWN_SHAPE = Block.createCuboidShape(5.0, 0.0, 5.0, 11.0, 5.0, 11.0);

    public ItemPipeBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(NORTH, PipeSide.NONE)
                .with(SOUTH, PipeSide.NONE)
                .with(EAST, PipeSide.NONE)
                .with(WEST, PipeSide.NONE)
                .with(UP, PipeSide.NONE)
                .with(DOWN, PipeSide.NONE));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ItemPipeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world instanceof ServerWorld serverWorld && type == ModBlockEntities.ITEM_PIPE_BLOCK_ENTITY) {
            return (w, pos, st, blockEntity) -> ItemPipeBlockEntity.tick(serverWorld, pos, st, (ItemPipeBlockEntity) blockEntity);
        }
        return null;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        VoxelShape shape = CORE_SHAPE;
        if (state.get(UP).isConnected()) shape = VoxelShapes.union(shape, UP_SHAPE);
        if (state.get(DOWN).isConnected()) shape = VoxelShapes.union(shape, DOWN_SHAPE);
        if (state.get(NORTH).isConnected()) shape = VoxelShapes.union(shape, NORTH_SHAPE);
        if (state.get(SOUTH).isConnected()) shape = VoxelShapes.union(shape, SOUTH_SHAPE);
        if (state.get(WEST).isConnected()) shape = VoxelShapes.union(shape, WEST_SHAPE);
        if (state.get(EAST).isConnected()) shape = VoxelShapes.union(shape, EAST_SHAPE);
        return shape;
    }

    public PipeSide getPipeSide(BlockView world, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.offset(direction);
        BlockState neighborState = world.getBlockState(neighborPos);
        Block block = neighborState.getBlock();

        boolean isNeighborValid = block instanceof ItemPipeBlock ||
                                  block instanceof ItemExtractorBlock ||
                                  block instanceof ItemInserterBlock;

        if (!isNeighborValid) {
            return PipeSide.NONE;
        }

        BlockEntity selfBe = world.getBlockEntity(pos);
        if (selfBe instanceof ItemPipeBlockEntity pipe && pipe.isDisconnected(direction)) {
            return PipeSide.DISCONNECTED;
        }

        BlockEntity neighborBe = world.getBlockEntity(neighborPos);
        if (neighborBe instanceof ItemPipeBlockEntity neighborPipe && neighborPipe.isDisconnected(direction.getOpposite())) {
            return PipeSide.DISCONNECTED;
        }
        if (neighborBe instanceof ItemExtractorBlockEntity neighborExt && neighborExt.isDisconnected(direction.getOpposite())) {
            return PipeSide.DISCONNECTED;
        }
        if (neighborBe instanceof ItemInserterBlockEntity neighborIns && neighborIns.isDisconnected(direction.getOpposite())) {
            return PipeSide.DISCONNECTED;
        }

        return PipeSide.CONNECTED;
    }

    public BlockState updateConnections(WorldView world, BlockPos pos, BlockState state) {
        return state
                .with(NORTH, getPipeSide(world, pos, Direction.NORTH))
                .with(SOUTH, getPipeSide(world, pos, Direction.SOUTH))
                .with(EAST, getPipeSide(world, pos, Direction.EAST))
                .with(WEST, getPipeSide(world, pos, Direction.WEST))
                .with(UP, getPipeSide(world, pos, Direction.UP))
                .with(DOWN, getPipeSide(world, pos, Direction.DOWN));
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        World world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();
        return updateConnections(world, pos, this.getDefaultState());
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, net.minecraft.util.math.random.Random random) {
        return state.with(
                switch (direction) {
                    case NORTH -> NORTH;
                    case SOUTH -> SOUTH;
                    case EAST -> EAST;
                    case WEST -> WEST;
                    case UP -> UP;
                    case DOWN -> DOWN;
                },
                getPipeSide(world, pos, direction)
        );
    }

    @Override
    public ActionResult onWrenched(World world, BlockPos pos, PlayerEntity player, Direction side) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof ItemPipeBlockEntity pipe) {
            boolean disconnected = pipe.toggleConnection(side);

            // Synchronize facing neighbor if present
            BlockPos neighborPos = pos.offset(side);
            BlockEntity neighborBe = world.getBlockEntity(neighborPos);
            if (neighborBe instanceof ItemPipeBlockEntity neighborPipe) {
                neighborPipe.setDisconnected(side.getOpposite(), disconnected);
                BlockState neighborState = world.getBlockState(neighborPos);
                if (neighborState.getBlock() instanceof ItemPipeBlock neighborBlock) {
                    world.setBlockState(neighborPos, neighborBlock.updateConnections(world, neighborPos, neighborState), Block.NOTIFY_ALL);
                }
            } else if (neighborBe instanceof ItemExtractorBlockEntity neighborExt) {
                neighborExt.setDisconnected(side.getOpposite(), disconnected);
                BlockState neighborState = world.getBlockState(neighborPos);
                if (neighborState.getBlock() instanceof ItemExtractorBlock neighborBlock) {
                    world.setBlockState(neighborPos, neighborBlock.updateConnections(world, neighborPos, neighborState), Block.NOTIFY_ALL);
                }
            } else if (neighborBe instanceof ItemInserterBlockEntity neighborIns) {
                neighborIns.setDisconnected(side.getOpposite(), disconnected);
                BlockState neighborState = world.getBlockState(neighborPos);
                if (neighborState.getBlock() instanceof ItemInserterBlock neighborBlock) {
                    world.setBlockState(neighborPos, neighborBlock.updateConnections(world, neighborPos, neighborState), Block.NOTIFY_ALL);
                }
            }

            BlockState updated = updateConnections(world, pos, world.getBlockState(pos));
            world.setBlockState(pos, updated, Block.NOTIFY_ALL);

            world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 1.0f, disconnected ? 0.7f : 1.3f);

            if (!world.isClient()) {
                if (world instanceof ServerWorld serverWorld) {
                    Vec3d p = pos.toCenterPos().add(Vec3d.of(side.getVector()).multiply(0.4));
                    serverWorld.spawnParticles(ParticleTypes.WAX_OFF, p.x, p.y, p.z, 6, 0.08, 0.08, 0.08, 0.02);
                }
                player.sendMessage(Text.literal(disconnected ?
                        "§6[Wrench] §c⛔ Capped & Disconnected §e" + side.asString().toUpperCase() :
                        "§6[Wrench] §a✔ Uncapped & Connected §e" + side.asString().toUpperCase()), true);
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    @Override
    public ActionResult onShiftWrenched(World world, BlockPos pos, PlayerEntity player, Direction side) {
        if (!world.isClient()) {
            world.breakBlock(pos, true, player);
            world.playSound(null, pos, SoundEvents.BLOCK_CHAIN_BREAK, SoundCategory.BLOCKS, 1.0f, 1.0f);
            player.sendMessage(Text.literal("§6[Wrench] §eDismantled Item Transport Pipe"), true);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, net.minecraft.entity.player.PlayerEntity player) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof ItemPipeBlockEntity pipe) {
            ItemScatterer.spawn(world, pos, pipe.getItems());
        }
        return super.onBreak(world, pos, state, player);
    }
}
