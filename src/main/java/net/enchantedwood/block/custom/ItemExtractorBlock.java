package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.enchantedwood.block.entity.ItemExtractorBlockEntity;
import net.enchantedwood.block.entity.ItemInserterBlockEntity;
import net.enchantedwood.block.entity.ItemPipeBlockEntity;
import net.enchantedwood.block.entity.ModBlockEntities;
import net.enchantedwood.util.Wrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class ItemExtractorBlock extends BaseEntityBlock implements Wrenchable {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    public static final EnumProperty<PipeSide> NORTH = EnumProperty.create("north", PipeSide.class);
    public static final EnumProperty<PipeSide> SOUTH = EnumProperty.create("south", PipeSide.class);
    public static final EnumProperty<PipeSide> EAST = EnumProperty.create("east", PipeSide.class);
    public static final EnumProperty<PipeSide> WEST = EnumProperty.create("west", PipeSide.class);
    public static final EnumProperty<PipeSide> UP = EnumProperty.create("up", PipeSide.class);
    public static final EnumProperty<PipeSide> DOWN = EnumProperty.create("down", PipeSide.class);

    private static final VoxelShape CORE_SHAPE = Block.box(5.0, 5.0, 5.0, 11.0, 11.0, 11.0);
    private static final VoxelShape NOZZLE_NORTH = Block.box(3.0, 3.0, 0.0, 13.0, 13.0, 5.0);
    private static final VoxelShape NOZZLE_SOUTH = Block.box(3.0, 3.0, 11.0, 13.0, 13.0, 16.0);
    private static final VoxelShape NOZZLE_EAST = Block.box(11.0, 3.0, 3.0, 16.0, 13.0, 13.0);
    private static final VoxelShape NOZZLE_WEST = Block.box(0.0, 3.0, 3.0, 5.0, 13.0, 13.0);
    private static final VoxelShape NOZZLE_UP = Block.box(3.0, 11.0, 3.0, 13.0, 16.0, 13.0);
    private static final VoxelShape NOZZLE_DOWN = Block.box(3.0, 0.0, 3.0, 13.0, 5.0, 13.0);

    private static final VoxelShape ARM_NORTH = Block.box(5.0, 5.0, 0.0, 11.0, 11.0, 5.0);
    private static final VoxelShape ARM_SOUTH = Block.box(5.0, 5.0, 11.0, 11.0, 11.0, 16.0);
    private static final VoxelShape ARM_EAST = Block.box(11.0, 5.0, 5.0, 16.0, 11.0, 11.0);
    private static final VoxelShape ARM_WEST = Block.box(0.0, 5.0, 5.0, 5.0, 11.0, 11.0);
    private static final VoxelShape ARM_UP = Block.box(5.0, 11.0, 5.0, 11.0, 16.0, 11.0);
    private static final VoxelShape ARM_DOWN = Block.box(5.0, 0.0, 5.0, 11.0, 5.0, 11.0);

    public ItemExtractorBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(NORTH, PipeSide.NONE)
                .setValue(SOUTH, PipeSide.NONE)
                .setValue(EAST, PipeSide.NONE)
                .setValue(WEST, PipeSide.NONE)
                .setValue(UP, PipeSide.NONE)
                .setValue(DOWN, PipeSide.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ItemExtractorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        if (world instanceof ServerLevel serverWorld && type == ModBlockEntities.ITEM_EXTRACTOR_BLOCK_ENTITY) {
            return (w, pos, st, blockEntity) -> ItemExtractorBlockEntity.tick(serverWorld, pos, st, (ItemExtractorBlockEntity) blockEntity);
        }
        return null;
    }

    public PipeSide getPipeSide(BlockGetter world, BlockPos pos, Direction side, Direction facing) {
        if (side == facing) return PipeSide.NONE; // Facing direction has the nozzle

        BlockPos neighborPos = pos.relative(side);
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

    public BlockState updateConnections(LevelReader world, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        return state
                .setValue(NORTH, getPipeSide(world, pos, Direction.NORTH, facing))
                .setValue(SOUTH, getPipeSide(world, pos, Direction.SOUTH, facing))
                .setValue(EAST, getPipeSide(world, pos, Direction.EAST, facing))
                .setValue(WEST, getPipeSide(world, pos, Direction.WEST, facing))
                .setValue(UP, getPipeSide(world, pos, Direction.UP, facing))
                .setValue(DOWN, getPipeSide(world, pos, Direction.DOWN, facing));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE_SHAPE;
        Direction facing = state.getValue(FACING);

        shape = Shapes.or(shape, switch (facing) {
            case NORTH -> NOZZLE_NORTH;
            case SOUTH -> NOZZLE_SOUTH;
            case EAST -> NOZZLE_EAST;
            case WEST -> NOZZLE_WEST;
            case UP -> NOZZLE_UP;
            case DOWN -> NOZZLE_DOWN;
        });

        if (state.getValue(NORTH).isConnected() && facing != Direction.NORTH) shape = Shapes.or(shape, ARM_NORTH);
        if (state.getValue(SOUTH).isConnected() && facing != Direction.SOUTH) shape = Shapes.or(shape, ARM_SOUTH);
        if (state.getValue(EAST).isConnected() && facing != Direction.EAST) shape = Shapes.or(shape, ARM_EAST);
        if (state.getValue(WEST).isConnected() && facing != Direction.WEST) shape = Shapes.or(shape, ARM_WEST);
        if (state.getValue(UP).isConnected() && facing != Direction.UP) shape = Shapes.or(shape, ARM_UP);
        if (state.getValue(DOWN).isConnected() && facing != Direction.DOWN) shape = Shapes.or(shape, ARM_DOWN);

        return shape;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Direction facing = ctx.getClickedFace().getOpposite();

        return updateConnections(world, pos, this.defaultBlockState().setValue(FACING, facing));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, net.minecraft.util.RandomSource random) {
        Direction facing = state.getValue(FACING);
        return state.setValue(
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
    public InteractionResult onWrenched(Level world, BlockPos pos, Player player, Direction side) {
        BlockState state = world.getBlockState(pos);
        Direction facing = state.getValue(FACING);

        if (side == facing) {
            Direction[] all = Direction.values();
            Direction next = all[(facing.ordinal() + 1) % all.length];
            BlockState updated = updateConnections(world, pos, state.setValue(FACING, next));
            world.setBlock(pos, updated, Block.UPDATE_ALL);
            world.playSound(null, pos, SoundEvents.COPPER_GRATE_PLACE, SoundSource.BLOCKS, 1.0f, 1.2f);
            if (!world.isClientSide()) {
                player.sendOverlayMessage(Component.literal("§6[Wrench] §aFacing " + next.getSerializedName().toUpperCase()));
            }
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof ItemExtractorBlockEntity extractor) {
            boolean disconnected = extractor.toggleConnection(side);

            BlockPos neighborPos = pos.relative(side);
            BlockEntity neighborBe = world.getBlockEntity(neighborPos);
            if (neighborBe instanceof ItemPipeBlockEntity neighborPipe) {
                neighborPipe.setDisconnected(side.getOpposite(), disconnected);
                BlockState neighborState = world.getBlockState(neighborPos);
                if (neighborState.getBlock() instanceof ItemPipeBlock neighborBlock) {
                    world.setBlock(neighborPos, neighborBlock.updateConnections(world, neighborPos, neighborState), Block.UPDATE_ALL);
                }
            } else if (neighborBe instanceof ItemExtractorBlockEntity neighborExt) {
                neighborExt.setDisconnected(side.getOpposite(), disconnected);
                BlockState neighborState = world.getBlockState(neighborPos);
                if (neighborState.getBlock() instanceof ItemExtractorBlock neighborBlock) {
                    world.setBlock(neighborPos, neighborBlock.updateConnections(world, neighborPos, neighborState), Block.UPDATE_ALL);
                }
            } else if (neighborBe instanceof ItemInserterBlockEntity neighborIns) {
                neighborIns.setDisconnected(side.getOpposite(), disconnected);
                BlockState neighborState = world.getBlockState(neighborPos);
                if (neighborState.getBlock() instanceof ItemInserterBlock neighborBlock) {
                    world.setBlock(neighborPos, neighborBlock.updateConnections(world, neighborPos, neighborState), Block.UPDATE_ALL);
                }
            }

            BlockState updated = updateConnections(world, pos, world.getBlockState(pos));
            world.setBlock(pos, updated, Block.UPDATE_ALL);

            world.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 1.0f, disconnected ? 0.7f : 1.3f);

            if (!world.isClientSide()) {
                if (world instanceof ServerLevel serverWorld) {
                    Vec3 p = Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(side.getUnitVec3i()).scale(0.4));
                    serverWorld.sendParticles(ParticleTypes.WAX_OFF, p.x, p.y, p.z, 6, 0.08, 0.08, 0.08, 0.02);
                }
                player.sendOverlayMessage(Component.literal(disconnected ?
                        "§6[Wrench] §c⛔ Capped & Disconnected §e" + side.getSerializedName().toUpperCase() :
                        "§6[Wrench] §a✔ Uncapped & Connected §e" + side.getSerializedName().toUpperCase()));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult onShiftWrenched(Level world, BlockPos pos, Player player, Direction side) {
        if (!world.isClientSide()) {
            world.destroyBlock(pos, true, player);
            world.playSound(null, pos, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
            player.sendOverlayMessage(Component.literal("§6[Wrench] §eDismantled Item Extractor"));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, net.minecraft.world.entity.player.Player player) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof ItemExtractorBlockEntity extractor) {
            Containers.dropContents(world, pos, extractor.getItems());
        }
        return super.playerWillDestroy(world, pos, state, player);
    }
}
