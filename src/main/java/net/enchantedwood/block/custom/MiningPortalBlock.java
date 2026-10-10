package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.world.dimension.ModDimensions;

import java.util.Set;

public class MiningPortalBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    protected static final VoxelShape X_SHAPE = Block.box(6.0, 0.0, 0.0, 10.0, 16.0, 16.0);
    protected static final VoxelShape Z_SHAPE = Block.box(0.0, 0.0, 6.0, 16.0, 16.0, 10.0);

    public MiningPortalBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.Z ? Z_SHAPE : X_SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public BlockState updateShape(BlockState state, LevelReader world, net.minecraft.world.level.ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        Direction.Axis axis = state.getValue(AXIS);
        Direction.Axis dirAxis = direction.getAxis();
        if (dirAxis != axis && direction.getAxis().isHorizontal()) {
            return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
        }
        
        BlockPos below = pos.below();
        BlockState belowState = world.getBlockState(below);
        if (!belowState.is(this) && !belowState.is(ModBlocks.ENCHANTED_COBBLESTONE)) {
            return Blocks.AIR.defaultBlockState();
        }
        BlockPos above = pos.above();
        BlockState aboveState = world.getBlockState(above);
        if (!aboveState.is(this) && !aboveState.is(ModBlocks.ENCHANTED_COBBLESTONE)) {
            return Blocks.AIR.defaultBlockState();
        }
        
        return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
        return ItemStack.EMPTY;
    }

    @Override
    protected void entityInside(BlockState state, Level world, BlockPos pos, Entity entity, InsideBlockEffectApplier handler, boolean isInside) {
        if (world.isClientSide() || entity.isPassenger() || entity.isVehicle() || !entity.canUsePortal(false)) {
            return;
        }

        if (entity instanceof ServerPlayer player) {
            if (player.isOnPortalCooldown()) {
                return;
            }

            ServerLevel currentWorld = (ServerLevel) world;
            ServerLevel targetWorld;
            if (currentWorld.dimension() == ModDimensions.MINING_DIMENSION_WORLD_KEY) {
                targetWorld = currentWorld.getServer().getLevel(Level.OVERWORLD);
            } else {
                targetWorld = currentWorld.getServer().getLevel(ModDimensions.MINING_DIMENSION_WORLD_KEY);
            }

            if (targetWorld == null) {
                return;
            }

            teleportPlayer(player, targetWorld, pos, state.getValue(AXIS));
        }
    }

    private void teleportPlayer(ServerPlayer player, ServerLevel targetWorld, BlockPos portalPos, Direction.Axis axis) {
        int targetX = portalPos.getX();
        int targetZ = portalPos.getZ();
        int targetY = Math.max(targetWorld.getMinY() + 10, Math.min(targetWorld.getMaxY() - 20, portalPos.getY()));

        // 1. Search for existing portal in target world within 16 blocks
        BlockPos existingPortalPos = null;
        for (int dx = -16; dx <= 16; dx++) {
            for (int dz = -16; dz <= 16; dz++) {
                for (int dy = -16; dy <= 16; dy++) {
                    BlockPos check = new BlockPos(targetX + dx, targetY + dy, targetZ + dz);
                    if (targetWorld.hasChunk(check.getX() >> 4, check.getZ() >> 4)) {
                        if (targetWorld.getBlockState(check).is(this)) {
                            existingPortalPos = check;
                            break;
                        }
                    }
                }
                if (existingPortalPos != null) break;
            }
            if (existingPortalPos != null) break;
        }

        double spawnX;
        double spawnY;
        double spawnZ;

        if (existingPortalPos != null) {
            // Re-use existing portal without modifying anything!
            BlockState existingState = targetWorld.getBlockState(existingPortalPos);
            Direction.Axis foundAxis = existingState.hasProperty(AXIS) ? existingState.getValue(AXIS) : axis;
            Direction frontDir = foundAxis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;

            spawnX = existingPortalPos.getX() + 0.5 + frontDir.getStepX() * 1.2;
            spawnY = existingPortalPos.getY();
            spawnZ = existingPortalPos.getZ() + 0.5 + frontDir.getStepZ() * 1.2;
        } else {
            // Only create new portal if none exists
            int safeY = Math.max(64, Math.min(100, targetY));
            BlockPos basePos = new BlockPos(targetX, safeY, targetZ);
            buildSafePortalDestination(targetWorld, basePos, axis);

            Direction frontDir = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
            spawnX = basePos.getX() + 1.5 + frontDir.getStepX() * 1.2;
            spawnY = basePos.getY() + 1.0;
            spawnZ = basePos.getZ() + 0.5 + frontDir.getStepZ() * 1.2;
        }

        player.setPortalCooldown(100);
        player.teleportTo(targetWorld, spawnX, spawnY, spawnZ, Set.of(), player.getYRot(), player.getXRot(), true);
    }

    private void buildSafePortalDestination(ServerLevel world, BlockPos basePos, Direction.Axis axis) {
        Direction widthDir = axis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
        Direction depthDir = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;

        // Clear air space (4 wide, 5 high, 3 deep) and build platform
        for (int w = -1; w <= 4; w++) {
            for (int d = -2; d <= 2; d++) {
                for (int h = -1; h <= 5; h++) {
                    BlockPos current = basePos.relative(widthDir, w).relative(depthDir, d).above(h);
                    if (h == -1) {
                        // Solid platform below
                        world.setBlockAndUpdate(current, ModBlocks.ENCHANTED_COBBLESTONE.defaultBlockState());
                    } else if (h >= 0 && h <= 4 && d == 0 && (w >= 0 && w <= 3)) {
                        // Portal Frame / Portal Blocks
                        if (w == 0 || w == 3 || h == 0 || h == 4) {
                            world.setBlockAndUpdate(current, ModBlocks.ENCHANTED_COBBLESTONE.defaultBlockState());
                        } else {
                            world.setBlockAndUpdate(current, ModBlocks.MINING_PORTAL.defaultBlockState().setValue(AXIS, axis));
                        }
                    } else {
                        // Clear air around the portal for safe entry/exit
                        if (!world.isEmptyBlock(current)) {
                            world.setBlockAndUpdate(current, Blocks.AIR.defaultBlockState());
                        }
                    }
                }
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (random.nextInt(100) == 0) {
            world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.PORTAL_AMBIENT,
                    SoundSource.BLOCKS, 0.4f, random.nextFloat() * 0.4f + 0.8f);
        }

        double d = pos.getX() + random.nextDouble();
        double e = pos.getY() + random.nextDouble();
        double f = pos.getZ() + random.nextDouble();
        double g = (random.nextFloat() - 0.5) * 0.5;
        double h = (random.nextFloat() - 0.5) * 0.5;
        double j = (random.nextFloat() - 0.5) * 0.5;
        
        world.addParticle(new DustParticleOptions(0x10B981, 1.0f), d, e, f, g, h, j);
    }
}
