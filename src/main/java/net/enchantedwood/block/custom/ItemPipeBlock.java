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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class ItemPipeBlock extends BaseEntityBlock implements Wrenchable {

    public static final EnumProperty<PipeSide> NORTH = EnumProperty.create("north", PipeSide.class);
    public static final EnumProperty<PipeSide> SOUTH = EnumProperty.create("south", PipeSide.class);
    public static final EnumProperty<PipeSide> EAST = EnumProperty.create("east", PipeSide.class);
    public static final EnumProperty<PipeSide> WEST = EnumProperty.create("west", PipeSide.class);
    public static final EnumProperty<PipeSide> UP = EnumProperty.create("up", PipeSide.class);
    public static final EnumProperty<PipeSide> DOWN = EnumProperty.create("down", PipeSide.class);

    private static final VoxelShape CORE_SHAPE = Block.box(5.0, 5.0, 5.0, 11.0, 11.0, 11.0);
    private static final VoxelShape NORTH_SHAPE = Block.box(5.0, 5.0, 0.0, 11.0, 11.0, 5.0);
    private static final VoxelShape SOUTH_SHAPE = Block.box(5.0, 5.0, 11.0, 11.0, 11.0, 16.0);
    private static final VoxelShape EAST_SHAPE = Block.box(11.0, 5.0, 5.0, 16.0, 11.0, 11.0);
    private static final VoxelShape WEST_SHAPE = Block.box(0.0, 5.0, 5.0, 5.0, 11.0, 11.0);
    private static final VoxelShape UP_SHAPE = Block.box(5.0, 11.0, 5.0, 11.0, 16.0, 11.0);
    private static final VoxelShape DOWN_SHAPE = Block.box(5.0, 0.0, 5.0, 11.0, 5.0, 11.0);

    public ItemPipeBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, PipeSide.NONE)
                .setValue(SOUTH, PipeSide.NONE)
                .setValue(EAST, PipeSide.NONE)
                .setValue(WEST, PipeSide.NONE)
                .setValue(UP, PipeSide.NONE)
                .setValue(DOWN, PipeSide.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ItemPipeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        if (world instanceof ServerLevel serverWorld && type == ModBlockEntities.ITEM_PIPE_BLOCK_ENTITY) {
            return (w, pos, st, blockEntity) -> ItemPipeBlockEntity.tick(serverWorld, pos, st, (ItemPipeBlockEntity) blockEntity);
        }
        return null;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE_SHAPE;
        if (state.getValue(UP).isConnected()) shape = Shapes.or(shape, UP_SHAPE);
        if (state.getValue(DOWN).isConnected()) shape = Shapes.or(shape, DOWN_SHAPE);
        if (state.getValue(NORTH).isConnected()) shape = Shapes.or(shape, NORTH_SHAPE);
        if (state.getValue(SOUTH).isConnected()) shape = Shapes.or(shape, SOUTH_SHAPE);
        if (state.getValue(WEST).isConnected()) shape = Shapes.or(shape, WEST_SHAPE);
        if (state.getValue(EAST).isConnected()) shape = Shapes.or(shape, EAST_SHAPE);
        return shape;
    }

    public PipeSide getPipeSide(BlockGetter world, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.relative(direction);
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

    public BlockState updateConnections(LevelReader world, BlockPos pos, BlockState state) {
        return state
                .setValue(NORTH, getPipeSide(world, pos, Direction.NORTH))
                .setValue(SOUTH, getPipeSide(world, pos, Direction.SOUTH))
                .setValue(EAST, getPipeSide(world, pos, Direction.EAST))
                .setValue(WEST, getPipeSide(world, pos, Direction.WEST))
                .setValue(UP, getPipeSide(world, pos, Direction.UP))
                .setValue(DOWN, getPipeSide(world, pos, Direction.DOWN));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        return updateConnections(world, pos, this.defaultBlockState());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, net.minecraft.util.RandomSource random) {
        return state.setValue(
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
    public InteractionResult onWrenched(Level world, BlockPos pos, Player player, Direction side) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof ItemPipeBlockEntity pipe) {
            boolean disconnected = pipe.toggleConnection(side);

            // Synchronize facing neighbor if present
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
            player.sendOverlayMessage(Component.literal("§6[Wrench] §eDismantled Item Transport Pipe"));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, net.minecraft.world.entity.player.Player player) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof ItemPipeBlockEntity pipe) {
            Containers.dropContents(world, pos, pipe.getItems());
        }
        return super.playerWillDestroy(world, pos, state, player);
    }
}
