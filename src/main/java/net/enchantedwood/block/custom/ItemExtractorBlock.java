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
import net.minecraft.state.property.Properties;
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

public class ItemExtractorBlock extends BlockWithEntity implements Wrenchable {
    public static final MapCodec<ItemExtractorBlock> CODEC = createCodec(ItemExtractorBlock::new);
    public static final EnumProperty<Direction> FACING = Properties.FACING;

    public static final EnumProperty<PipeSide> NORTH = EnumProperty.of("north", PipeSide.class);
    public static final EnumProperty<PipeSide> SOUTH = EnumProperty.of("south", PipeSide.class);
    public static final EnumProperty<PipeSide> EAST = EnumProperty.of("east", PipeSide.class);
    public static final EnumProperty<PipeSide> WEST = EnumProperty.of("west", PipeSide.class);
    public static final EnumProperty<PipeSide> UP = EnumProperty.of("up", PipeSide.class);
    public static final EnumProperty<PipeSide> DOWN = EnumProperty.of("down", PipeSide.class);

    private static final VoxelShape CORE_SHAPE = Block.createCuboidShape(5.0, 5.0, 5.0, 11.0, 11.0, 11.0);
    private static final VoxelShape NOZZLE_NORTH = Block.createCuboidShape(3.0, 3.0, 0.0, 13.0, 13.0, 5.0);
    private static final VoxelShape NOZZLE_SOUTH = Block.createCuboidShape(3.0, 3.0, 11.0, 13.0, 13.0, 16.0);
    private static final VoxelShape NOZZLE_EAST = Block.createCuboidShape(11.0, 3.0, 3.0, 16.0, 13.0, 13.0);
    private static final VoxelShape NOZZLE_WEST = Block.createCuboidShape(0.0, 3.0, 3.0, 5.0, 13.0, 13.0);
    private static final VoxelShape NOZZLE_UP = Block.createCuboidShape(3.0, 11.0, 3.0, 13.0, 16.0, 13.0);
    private static final VoxelShape NOZZLE_DOWN = Block.createCuboidShape(3.0, 0.0, 3.0, 13.0, 5.0, 13.0);

    private static final VoxelShape ARM_NORTH = Block.createCuboidShape(5.0, 5.0, 0.0, 11.0, 11.0, 5.0);
    private static final VoxelShape ARM_SOUTH = Block.createCuboidShape(5.0, 5.0, 11.0, 11.0, 11.0, 16.0);
    private static final VoxelShape ARM_EAST = Block.createCuboidShape(11.0, 5.0, 5.0, 16.0, 11.0, 11.0);
    private static final VoxelShape ARM_WEST = Block.createCuboidShape(0.0, 5.0, 5.0, 5.0, 11.0, 11.0);
    private static final VoxelShape ARM_UP = Block.createCuboidShape(5.0, 11.0, 5.0, 11.0, 16.0, 11.0);
    private static final VoxelShape ARM_DOWN = Block.createCuboidShape(5.0, 0.0, 5.0, 11.0, 5.0, 11.0);

    public ItemExtractorBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH)
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
        builder.add(FACING, NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ItemExtractorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world instanceof ServerWorld serverWorld && type == ModBlockEntities.ITEM_EXTRACTOR_BLOCK_ENTITY) {
            return (w, pos, st, blockEntity) -> ItemExtractorBlockEntity.tick(serverWorld, pos, st, (ItemExtractorBlockEntity) blockEntity);
        }
        return null;
    }

    public PipeSide getPipeSide(BlockView world, BlockPos pos, Direction side, Direction facing) {
        if (side == facing) return PipeSide.NONE; // Facing direction has the nozzle

        BlockPos neighborPos = pos.offset(side);
        BlockState neighborState = world.getBlockState(neighborPos);
        Block block = neighborState.getBlock();

        boolean isNeighborValid = block instanceof ItemPipeBlock ||
                                  block instanceof ItemExtractorBlock ||
                                  block instanceof ItemInserterBlock;

        if (!isNeighborValid) {
            return PipeSide.NONE;
        }

        BlockEntity selfBe = world.getBlockEntity(pos);
        if (selfBe instanceof ItemExtractorBlockEntity extractor && extractor.isDisconnected(side)) {
            return PipeSide.DISCONNECTED;
        }

        BlockEntity neighborBe = world.getBlockEntity(neighborPos);
        if (neighborBe instanceof ItemPipeBlockEntity neighborPipe && neighborPipe.isDisconnected(side.getOpposite())) {
            return PipeSide.DISCONNECTED;
        }
        if (neighborBe instanceof ItemExtractorBlockEntity neighborExt && neighborExt.isDisconnected(side.getOpposite())) {
            return PipeSide.DISCONNECTED;
        }
        if (neighborBe instanceof ItemInserterBlockEntity neighborIns && neighborIns.isDisconnected(side.getOpposite())) {
            return PipeSide.DISCONNECTED;
        }

        return PipeSide.CONNECTED;
    }

    public BlockState updateConnections(WorldView world, BlockPos pos, BlockState state) {
        Direction facing = state.get(FACING);
        return state
                .with(NORTH, getPipeSide(world, pos, Direction.NORTH, facing))
                .with(SOUTH, getPipeSide(world, pos, Direction.SOUTH, facing))
                .with(EAST, getPipeSide(world, pos, Direction.EAST, facing))
                .with(WEST, getPipeSide(world, pos, Direction.WEST, facing))
                .with(UP, getPipeSide(world, pos, Direction.UP, facing))
                .with(DOWN, getPipeSide(world, pos, Direction.DOWN, facing));
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        VoxelShape shape = CORE_SHAPE;
        Direction facing = state.get(FACING);

        shape = VoxelShapes.union(shape, switch (facing) {
            case NORTH -> NOZZLE_NORTH;
            case SOUTH -> NOZZLE_SOUTH;
            case EAST -> NOZZLE_EAST;
            case WEST -> NOZZLE_WEST;
            case UP -> NOZZLE_UP;
            case DOWN -> NOZZLE_DOWN;
        });

        if (state.get(NORTH).isConnected() && facing != Direction.NORTH) shape = VoxelShapes.union(shape, ARM_NORTH);
        if (state.get(SOUTH).isConnected() && facing != Direction.SOUTH) shape = VoxelShapes.union(shape, ARM_SOUTH);
        if (state.get(EAST).isConnected() && facing != Direction.EAST) shape = VoxelShapes.union(shape, ARM_EAST);
        if (state.get(WEST).isConnected() && facing != Direction.WEST) shape = VoxelShapes.union(shape, ARM_WEST);
        if (state.get(UP).isConnected() && facing != Direction.UP) shape = VoxelShapes.union(shape, ARM_UP);
        if (state.get(DOWN).isConnected() && facing != Direction.DOWN) shape = VoxelShapes.union(shape, ARM_DOWN);

        return shape;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        World world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();
        Direction facing = ctx.getSide().getOpposite();

        return updateConnections(world, pos, this.getDefaultState().with(FACING, facing));
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, net.minecraft.util.math.random.Random random) {
        Direction facing = state.get(FACING);
        return state.with(
                switch (direction) {
                    case NORTH -> NORTH;
                    case SOUTH -> SOUTH;
                    case EAST -> EAST;
                    case WEST -> WEST;
                    case UP -> UP;
                    case DOWN -> DOWN;
                },
                getPipeSide(world, pos, direction, facing)
        );
    }

    @Override
    public ActionResult onWrenched(World world, BlockPos pos, PlayerEntity player, Direction side) {
        BlockState state = world.getBlockState(pos);
        Direction facing = state.get(FACING);

        if (side == facing) {
            Direction[] all = Direction.values();
            Direction next = all[(facing.ordinal() + 1) % all.length];
            BlockState updated = updateConnections(world, pos, state.with(FACING, next));
            world.setBlockState(pos, updated, Block.NOTIFY_ALL);
            world.playSound(null, pos, SoundEvents.BLOCK_COPPER_GRATE_PLACE, SoundCategory.BLOCKS, 1.0f, 1.2f);
            if (!world.isClient()) {
                player.sendMessage(Text.literal("§6[Wrench] §aFacing " + next.asString().toUpperCase()), true);
            }
            return ActionResult.SUCCESS;
        }

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof ItemExtractorBlockEntity extractor) {
            boolean disconnected = extractor.toggleConnection(side);

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
            player.sendMessage(Text.literal("§6[Wrench] §eDismantled Item Extractor"), true);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, net.minecraft.entity.player.PlayerEntity player) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof ItemExtractorBlockEntity extractor) {
            ItemScatterer.spawn(world, pos, extractor.getItems());
        }
        return super.onBreak(world, pos, state, player);
    }
}
