package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.world.dimension.ModDimensions;

import java.util.Set;

public class MiningPortalBlock extends Block {
    public static final MapCodec<MiningPortalBlock> CODEC = createCodec(MiningPortalBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = Properties.HORIZONTAL_AXIS;

    protected static final VoxelShape X_SHAPE = Block.createCuboidShape(6.0, 0.0, 0.0, 10.0, 16.0, 16.0);
    protected static final VoxelShape Z_SHAPE = Block.createCuboidShape(0.0, 0.0, 6.0, 16.0, 16.0, 10.0);

    public MiningPortalBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(AXIS, Direction.Axis.X));
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(AXIS) == Direction.Axis.Z ? Z_SHAPE : X_SHAPE;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, WorldView world, net.minecraft.world.tick.ScheduledTickView tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random) {
        Direction.Axis axis = state.get(AXIS);
        Direction.Axis dirAxis = direction.getAxis();
        if (dirAxis != axis && direction.getAxis().isHorizontal()) {
            return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
        }
        
        BlockPos below = pos.down();
        BlockState belowState = world.getBlockState(below);
        if (!belowState.isOf(this) && !belowState.isOf(ModBlocks.ENCHANTED_COBBLESTONE)) {
            return Blocks.AIR.getDefaultState();
        }
        BlockPos above = pos.up();
        BlockState aboveState = world.getBlockState(above);
        if (!aboveState.isOf(this) && !aboveState.isOf(ModBlocks.ENCHANTED_COBBLESTONE)) {
            return Blocks.AIR.getDefaultState();
        }
        
        return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state, boolean includeData) {
        return ItemStack.EMPTY;
    }

    @Override
    protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity, EntityCollisionHandler handler, boolean isInside) {
        if (world.isClient() || entity.hasVehicle() || entity.hasPassengers() || !entity.canUsePortals(false)) {
            return;
        }

        if (entity instanceof ServerPlayerEntity player) {
            if (player.hasPortalCooldown()) {
                return;
            }

            ServerWorld currentWorld = (ServerWorld) world;
            ServerWorld targetWorld;
            if (currentWorld.getRegistryKey() == ModDimensions.MINING_DIMENSION_WORLD_KEY) {
                targetWorld = currentWorld.getServer().getWorld(World.OVERWORLD);
            } else {
                targetWorld = currentWorld.getServer().getWorld(ModDimensions.MINING_DIMENSION_WORLD_KEY);
            }

            if (targetWorld == null) {
                return;
            }

            teleportPlayer(player, targetWorld, pos, state.get(AXIS));
        }
    }

    private void teleportPlayer(ServerPlayerEntity player, ServerWorld targetWorld, BlockPos portalPos, Direction.Axis axis) {
        int targetX = portalPos.getX();
        int targetZ = portalPos.getZ();
        int targetY = Math.max(64, Math.min(100, portalPos.getY()));

        BlockPos basePos = new BlockPos(targetX, targetY, targetZ);
        buildSafePortalDestination(targetWorld, basePos, axis);

        // Position player safely in front of the destination portal
        Direction frontDir = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        double spawnX = basePos.getX() + 1.5 + frontDir.getOffsetX() * 1.2;
        double spawnY = basePos.getY() + 1.0;
        double spawnZ = basePos.getZ() + 0.5 + frontDir.getOffsetZ() * 1.2;

        player.setPortalCooldown(100);
        player.teleport(targetWorld, spawnX, spawnY, spawnZ, Set.of(), player.getYaw(), player.getPitch(), true);
    }

    private void buildSafePortalDestination(ServerWorld world, BlockPos basePos, Direction.Axis axis) {
        Direction widthDir = axis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
        Direction depthDir = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;

        // Clear air space (4 wide, 5 high, 3 deep) and build platform
        for (int w = -1; w <= 4; w++) {
            for (int d = -2; d <= 2; d++) {
                for (int h = -1; h <= 5; h++) {
                    BlockPos current = basePos.offset(widthDir, w).offset(depthDir, d).up(h);
                    if (h == -1) {
                        // Solid platform below
                        world.setBlockState(current, ModBlocks.ENCHANTED_COBBLESTONE.getDefaultState());
                    } else if (h >= 0 && h <= 4 && d == 0 && (w >= 0 && w <= 3)) {
                        // Portal Frame / Portal Blocks
                        if (w == 0 || w == 3 || h == 0 || h == 4) {
                            world.setBlockState(current, ModBlocks.ENCHANTED_COBBLESTONE.getDefaultState());
                        } else {
                            world.setBlockState(current, ModBlocks.MINING_PORTAL.getDefaultState().with(AXIS, axis));
                        }
                    } else {
                        // Clear air around the portal for safe entry/exit
                        if (!world.isAir(current)) {
                            world.setBlockState(current, Blocks.AIR.getDefaultState());
                        }
                    }
                }
            }
        }
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (random.nextInt(100) == 0) {
            world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.BLOCK_PORTAL_AMBIENT,
                    SoundCategory.BLOCKS, 0.4f, random.nextFloat() * 0.4f + 0.8f);
        }

        double d = pos.getX() + random.nextDouble();
        double e = pos.getY() + random.nextDouble();
        double f = pos.getZ() + random.nextDouble();
        double g = (random.nextFloat() - 0.5) * 0.5;
        double h = (random.nextFloat() - 0.5) * 0.5;
        double j = (random.nextFloat() - 0.5) * 0.5;
        
        world.addParticleClient(new DustParticleEffect(0x10B981, 1.0f), d, e, f, g, h, j);
    }
}
