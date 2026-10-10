package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.enchantedwood.block.entity.IceCreamMachineBlockEntity;
import net.enchantedwood.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class IceCreamMachineBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public IceCreamMachineBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IceCreamMachineBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return world instanceof ServerLevel serverWorld
                ? createTickerHelper(type, ModBlockEntities.ICE_CREAM_MACHINE_BLOCK_ENTITY, (w, pos, st, blockEntity) -> IceCreamMachineBlockEntity.tick(serverWorld, pos, st, blockEntity))
                : null;
    }

    private static final VoxelShape BASE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
    // Facing North -> Handle on East (+X, extending from x=16 to x=22)
    private static final VoxelShape SHAPE_NORTH = Shapes.or(BASE, Block.box(16.0, 7.0, 6.0, 22.0, 15.0, 11.0));
    // Facing South -> Handle on West (-X, extending from x=-6 to x=0)
    private static final VoxelShape SHAPE_SOUTH = Shapes.or(BASE, Block.box(-6.0, 7.0, 5.0, 0.0, 15.0, 10.0));
    // Facing East -> Handle on South (+Z, extending from z=16 to z=22)
    private static final VoxelShape SHAPE_EAST = Shapes.or(BASE, Block.box(5.0, 7.0, 16.0, 10.0, 15.0, 22.0));
    // Facing West -> Handle on North (-Z, extending from z=-6 to z=0)
    private static final VoxelShape SHAPE_WEST = Shapes.or(BASE, Block.box(6.0, 7.0, -6.0, 11.0, 15.0, 0.0));

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            default -> BASE;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            default -> BASE;
        };
    }

    public static boolean isHandleHit(Direction facing, Direction hitSide, double relX, double relY, double relZ) {
        Direction handleSide = facing.getClockWise();

        // 1. Ray hit the handle side face directly
        if (hitSide == handleSide) {
            return true;
        }

        // 2. Ray hit the protruding handle geometry outside the standard block cube
        if (facing == Direction.NORTH && relX >= 0.95) return true;
        if (facing == Direction.SOUTH && relX <= 0.05) return true;
        if (facing == Direction.EAST && relZ >= 0.95) return true;
        if (facing == Direction.WEST && relZ <= 0.05) return true;

        // 3. Ray hit the handle hub/arm area on the handle side of the machine
        return switch (facing) {
            case NORTH -> relX >= 0.60 && relY >= 0.25 && relZ >= 0.15 && relZ <= 0.85;
            case SOUTH -> relX <= 0.40 && relY >= 0.25 && relZ >= 0.15 && relZ <= 0.85;
            case EAST  -> relZ >= 0.60 && relY >= 0.25 && relX >= 0.15 && relX <= 0.85;
            case WEST  -> relZ <= 0.40 && relY >= 0.25 && relX >= 0.15 && relX <= 0.85;
            default -> false;
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof IceCreamMachineBlockEntity machine) {
                Direction facing = state.getValue(FACING);
                Direction handleSide = facing.getClockWise();

                // 1. Sneak-click always opens GUI
                if (player.isShiftKeyDown()) {
                    player.openMenu(machine);
                    return InteractionResult.SUCCESS;
                }

                // 2. Check if the player clicked the crank handle (any face of the handle or handle region)
                double relX = hit.getLocation().x - pos.getX();
                double relY = hit.getLocation().y - pos.getY();
                double relZ = hit.getLocation().z - pos.getZ();

                if (isHandleHit(facing, hit.getDirection(), relX, relY, relZ)) {
                    // Check if adjacent block blocks the handle
                    BlockPos neighborPos = pos.relative(handleSide);
                    BlockState neighborState = world.getBlockState(neighborPos);
                    if (neighborState.isRedstoneConductor(world, neighborPos) || neighborState.isSolid()) {
                        world.playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 0.8f, 1.2f);
                        player.sendOverlayMessage(Component.literal("§cCannot churn: Crank handle is blocked by the adjacent block to the " + handleSide.getSerializedName().toUpperCase() + "!"));
                        return InteractionResult.SUCCESS;
                    }

                    // Attempt manual crank
                    if (machine.canChurn()) {
                        machine.manualCrank();
                        world.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.8f, 1.4f);
                        int pct = (machine.getChurnProgress() * 100) / Math.max(1, machine.getMaxChurnProgress());
                        if (pct == 0) {
                            world.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.9f, 1.2f);
                            player.sendOverlayMessage(Component.literal("§a✨ Ice Cream ready! Open the machine to collect it."));
                        } else {
                            player.sendOverlayMessage(Component.literal("§b🌀 Churned ice cream! (" + pct + "%)"));
                        }
                    } else {
                        // Diagnostic feedback: Explain EXACTLY why it cannot churn instead of opening GUI
                        world.playSound(null, pos, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 0.7f, 1.2f);
                        if (machine.getRefrigerationTime() <= 0 && machine.getInventory().get(IceCreamMachineBlockEntity.REFRIGERANT_SLOT).isEmpty()) {
                            player.sendOverlayMessage(Component.literal("§cCannot churn: Missing Refrigerant! Insert Ice, Ice Cubes, or Salt."));
                        } else if (machine.getInventory().get(IceCreamMachineBlockEntity.BASE_SLOT).isEmpty()) {
                            player.sendOverlayMessage(Component.literal("§cCannot churn: Missing Liquid Base! Insert Milk or Soy Milk."));
                        } else if (machine.getInventory().get(IceCreamMachineBlockEntity.SWEETENER_SLOT).isEmpty()) {
                            player.sendOverlayMessage(Component.literal("§cCannot churn: Missing Sweetener! Insert Sugar or Honey."));
                        } else if (!machine.getInventory().get(IceCreamMachineBlockEntity.OUTPUT_SLOT).isEmpty()) {
                            player.sendOverlayMessage(Component.literal("§cCannot churn: Output tray is full! Collect your ice cream."));
                        } else {
                            player.sendOverlayMessage(Component.literal("§cCannot churn: Missing ingredients. Right-click the front to open GUI."));
                        }
                    }
                    return InteractionResult.SUCCESS;
                }

                // 3. Clicking front, top, left, or back opens GUI
                player.openMenu(machine);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel world, BlockPos pos, boolean moved) {
        if (!state.is(world.getBlockState(pos).getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof IceCreamMachineBlockEntity machine) {
                Containers.dropContents(world, pos, machine);
            }
            super.affectNeighborsAfterRemoval(state, world, pos, moved);
        }
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.6;
            double z = pos.getZ() + 0.5;

            if (random.nextDouble() < 0.2) {
                world.playLocalSound(x, y, z, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.4f, 1.5f, false);
            }

            world.addParticle(ParticleTypes.SNOWFLAKE, x + (random.nextDouble() - 0.5) * 0.4, y, z + (random.nextDouble() - 0.5) * 0.4, 0.0, 0.02, 0.0);
            world.addParticle(ParticleTypes.CLOUD, x + (random.nextDouble() - 0.5) * 0.3, y + 0.2, z + (random.nextDouble() - 0.5) * 0.3, 0.0, 0.01, 0.0);
        }
    }
}
