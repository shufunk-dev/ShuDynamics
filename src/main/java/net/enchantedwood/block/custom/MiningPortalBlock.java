package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.entity.player.PlayerEntity;
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
        
        // If adjacent frame or portal block broken, break self
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
            if (player.portalManager != null && player.portalManager.isInPortal()) {
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

            teleportPlayer(player, currentWorld, targetWorld, pos, state.get(AXIS));
        }
    }

    private void teleportPlayer(ServerPlayerEntity player, ServerWorld fromWorld, ServerWorld targetWorld, BlockPos portalPos, Direction.Axis axis) {
        BlockPos targetPos = findOrCreateDestinationPortal(targetWorld, portalPos, axis);
        player.resetPortalCooldown();
        player.teleport(targetWorld, targetPos.getX() + 0.5, targetPos.getY() + 0.1, targetPos.getZ() + 0.5, Set.of(), player.getYaw(), player.getPitch(), true);
    }

    private BlockPos findOrCreateDestinationPortal(ServerWorld targetWorld, BlockPos sourcePos, Direction.Axis axis) {
        int searchRadius = 32;
        int checkY = Math.max(targetWorld.getBottomY() + 10, Math.min(targetWorld.getTopYInclusive() - 20, sourcePos.getY()));

        // Search for existing portal near destination coordinates
        for (BlockPos checkPos : BlockPos.iterate(
                sourcePos.getX() - searchRadius, checkY - 16, sourcePos.getZ() - searchRadius,
                sourcePos.getX() + searchRadius, checkY + 16, sourcePos.getZ() + searchRadius)) {
            if (targetWorld.getBlockState(checkPos).isOf(this)) {
                return checkPos.toImmutable();
            }
        }

        // Create new portal frame at safe height
        int spawnY = checkY;
        BlockPos.Mutable mut = new BlockPos.Mutable(sourcePos.getX(), targetWorld.getTopYInclusive() - 10, sourcePos.getZ());
        while (mut.getY() > targetWorld.getBottomY() + 10) {
            if (!targetWorld.isAir(mut) && targetWorld.getBlockState(mut).isOpaqueFullCube()) {
                spawnY = mut.getY() + 1;
                break;
            }
            mut.move(Direction.DOWN);
        }

        if (spawnY <= targetWorld.getBottomY() + 10) {
            spawnY = 64; // Default safe level
        }

        BlockPos basePos = new BlockPos(sourcePos.getX(), spawnY, sourcePos.getZ());
        buildPortalStructure(targetWorld, basePos, axis);
        return basePos.up();
    }

    public static void buildPortalStructure(ServerWorld world, BlockPos basePos, Direction.Axis axis) {
        Direction dir = axis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;

        // Build a 4-wide platform and 4x5 portal frame
        for (int x = -1; x <= 2; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos platPos = basePos.offset(dir, x).offset(axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH, z).down();
                world.setBlockState(platPos, ModBlocks.ENCHANTED_COBBLESTONE.getDefaultState());
            }
        }

        // Frame: width 4 (positions 0, 1, 2, 3), height 5 (levels 0, 1, 2, 3, 4)
        for (int w = 0; w < 4; w++) {
            for (int h = 0; h < 5; h++) {
                BlockPos framePos = basePos.offset(dir, w).up(h);
                if (w == 0 || w == 3 || h == 0 || h == 4) {
                    world.setBlockState(framePos, ModBlocks.ENCHANTED_COBBLESTONE.getDefaultState());
                } else {
                    // Inside portal blocks
                    world.setBlockState(framePos, ModBlocks.MINING_PORTAL.getDefaultState().with(AXIS, axis));
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
        
        // Emerald green glow particles
        world.addParticleClient(new DustParticleEffect(0x10B981, 1.0f), d, e, f, g, h, j);
    }
}
