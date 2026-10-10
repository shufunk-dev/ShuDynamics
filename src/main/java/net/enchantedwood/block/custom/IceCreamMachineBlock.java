package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.enchantedwood.block.entity.IceCreamMachineBlockEntity;
import net.enchantedwood.block.entity.ModBlockEntities;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public class IceCreamMachineBlock extends BlockWithEntity {
    public static final MapCodec<IceCreamMachineBlock> CODEC = createCodec(IceCreamMachineBlock::new);
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = Properties.LIT;

    public IceCreamMachineBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH).with(LIT, false));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    protected BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new IceCreamMachineBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world instanceof ServerWorld serverWorld
                ? validateTicker(type, ModBlockEntities.ICE_CREAM_MACHINE_BLOCK_ENTITY, (w, pos, st, blockEntity) -> IceCreamMachineBlockEntity.tick(serverWorld, pos, st, blockEntity))
                : null;
    }

    private static final VoxelShape BASE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
    // Facing North -> Handle on East (+X, extending from x=16 to x=22)
    private static final VoxelShape SHAPE_NORTH = VoxelShapes.union(BASE, Block.createCuboidShape(16.0, 7.0, 6.0, 22.0, 15.0, 11.0));
    // Facing South -> Handle on West (-X, extending from x=-6 to x=0)
    private static final VoxelShape SHAPE_SOUTH = VoxelShapes.union(BASE, Block.createCuboidShape(-6.0, 7.0, 5.0, 0.0, 15.0, 10.0));
    // Facing East -> Handle on South (+Z, extending from z=16 to z=22)
    private static final VoxelShape SHAPE_EAST = VoxelShapes.union(BASE, Block.createCuboidShape(5.0, 7.0, 16.0, 10.0, 15.0, 22.0));
    // Facing West -> Handle on North (-Z, extending from z=-6 to z=0)
    private static final VoxelShape SHAPE_WEST = VoxelShapes.union(BASE, Block.createCuboidShape(6.0, 7.0, -6.0, 11.0, 15.0, 0.0));

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(FACING)) {
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            default -> BASE;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(FACING)) {
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            default -> BASE;
        };
    }

    public static boolean isHandleHit(Direction facing, Direction hitSide, double relX, double relY, double relZ) {
        Direction handleSide = facing.rotateYClockwise();

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
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof IceCreamMachineBlockEntity machine) {
                Direction facing = state.get(FACING);
                Direction handleSide = facing.rotateYClockwise();

                // 1. Sneak-click always opens GUI
                if (player.isSneaking()) {
                    player.openHandledScreen(machine);
                    return ActionResult.SUCCESS;
                }

                // 2. Check if the player clicked the crank handle (any face of the handle or handle region)
                double relX = hit.getPos().x - pos.getX();
                double relY = hit.getPos().y - pos.getY();
                double relZ = hit.getPos().z - pos.getZ();

                if (isHandleHit(facing, hit.getSide(), relX, relY, relZ)) {
                    // Check if adjacent block blocks the handle
                    BlockPos neighborPos = pos.offset(handleSide);
                    BlockState neighborState = world.getBlockState(neighborPos);
                    if (neighborState.isSolidBlock(world, neighborPos) || neighborState.blocksMovement()) {
                        world.playSound(null, pos, SoundEvents.BLOCK_CHEST_LOCKED, SoundCategory.BLOCKS, 0.8f, 1.2f);
                        player.sendMessage(Text.literal("§cCannot churn: Crank handle is blocked by the adjacent block to the " + handleSide.asString().toUpperCase() + "!"), true);
                        return ActionResult.SUCCESS;
                    }

                    // Attempt manual crank
                    if (machine.canChurn()) {
                        machine.manualCrank();
                        world.playSound(null, pos, SoundEvents.BLOCK_GRINDSTONE_USE, SoundCategory.BLOCKS, 0.8f, 1.4f);
                        int pct = (machine.getChurnProgress() * 100) / Math.max(1, machine.getMaxChurnProgress());
                        if (pct == 0) {
                            world.playSound(null, pos, SoundEvents.BLOCK_BREWING_STAND_BREW, SoundCategory.BLOCKS, 0.9f, 1.2f);
                            player.sendMessage(Text.literal("§a✨ Ice Cream ready! Open the machine to collect it."), true);
                        } else {
                            player.sendMessage(Text.literal("§b🌀 Churned ice cream! (" + pct + "%)"), true);
                        }
                    } else {
                        // Diagnostic feedback: Explain EXACTLY why it cannot churn instead of opening GUI
                        world.playSound(null, pos, SoundEvents.BLOCK_DISPENSER_FAIL, SoundCategory.BLOCKS, 0.7f, 1.2f);
                        if (machine.getRefrigerationTime() <= 0 && machine.getInventory().get(IceCreamMachineBlockEntity.REFRIGERANT_SLOT).isEmpty()) {
                            player.sendMessage(Text.literal("§cCannot churn: Missing Refrigerant! Insert Ice, Ice Cubes, or Salt."), true);
                        } else if (machine.getInventory().get(IceCreamMachineBlockEntity.BASE_SLOT).isEmpty()) {
                            player.sendMessage(Text.literal("§cCannot churn: Missing Liquid Base! Insert Milk or Soy Milk."), true);
                        } else if (machine.getInventory().get(IceCreamMachineBlockEntity.SWEETENER_SLOT).isEmpty()) {
                            player.sendMessage(Text.literal("§cCannot churn: Missing Sweetener! Insert Sugar or Honey."), true);
                        } else if (!machine.getInventory().get(IceCreamMachineBlockEntity.OUTPUT_SLOT).isEmpty()) {
                            player.sendMessage(Text.literal("§cCannot churn: Output tray is full! Collect your ice cream."), true);
                        } else {
                            player.sendMessage(Text.literal("§cCannot churn: Missing ingredients. Right-click the front to open GUI."), true);
                        }
                    }
                    return ActionResult.SUCCESS;
                }

                // 3. Clicking front, top, left, or back opens GUI
                player.openHandledScreen(machine);
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        if (!state.isOf(world.getBlockState(pos).getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof IceCreamMachineBlockEntity machine) {
                ItemScatterer.spawn(world, pos, machine);
            }
            super.onStateReplaced(state, world, pos, moved);
        }
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (state.get(LIT)) {
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.6;
            double z = pos.getZ() + 0.5;

            if (random.nextDouble() < 0.2) {
                world.playSoundClient(x, y, z, SoundEvents.BLOCK_BREWING_STAND_BREW, SoundCategory.BLOCKS, 0.4f, 1.5f, false);
            }

            world.addParticleClient(ParticleTypes.SNOWFLAKE, x + (random.nextDouble() - 0.5) * 0.4, y, z + (random.nextDouble() - 0.5) * 0.4, 0.0, 0.02, 0.0);
            world.addParticleClient(ParticleTypes.CLOUD, x + (random.nextDouble() - 0.5) * 0.3, y + 0.2, z + (random.nextDouble() - 0.5) * 0.3, 0.0, 0.01, 0.0);
        }
    }
}
