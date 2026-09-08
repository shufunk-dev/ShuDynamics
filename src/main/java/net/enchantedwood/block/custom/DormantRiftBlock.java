package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.world.dimension.ModDimensions;

import java.util.Set;

public class DormantRiftBlock extends Block {
    public static final MapCodec<DormantRiftBlock> CODEC = createCodec(DormantRiftBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = Properties.HORIZONTAL_AXIS;

    protected static final VoxelShape X_SHAPE = Block.createCuboidShape(6.0, 0.0, 0.0, 10.0, 16.0, 16.0);
    protected static final VoxelShape Z_SHAPE = Block.createCuboidShape(0.0, 0.0, 6.0, 16.0, 16.0, 10.0);

    public DormantRiftBlock(Settings settings) {
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
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state, boolean includeData) {
        return ItemStack.EMPTY;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        world.playSound(null, pos, SoundEvents.BLOCK_BEACON_AMBIENT, SoundCategory.BLOCKS, 1.0f, 1.8f);
        if (!world.isClient()) {
            player.sendMessage(
                    Text.literal("§5✦ Gateway of Resonance: §aDimensional Alignment Synchronized. §7Step through the threshold to enter §dThe Convergence§7."),
                    false
            );
        }
        return ActionResult.SUCCESS;
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
            boolean toConvergence = currentWorld.getRegistryKey() != ModDimensions.CONVERGENCE_WORLD_KEY;

            if (toConvergence) {
                targetWorld = currentWorld.getServer().getWorld(ModDimensions.CONVERGENCE_WORLD_KEY);
            } else {
                targetWorld = currentWorld.getServer().getWorld(World.OVERWORLD);
            }

            if (targetWorld == null) {
                player.sendMessage(Text.literal("§c⚠️ Dimension Link Error: Target world unavailable."), true);
                return;
            }

            teleportPlayer(player, targetWorld, pos, state.get(AXIS), toConvergence);
        }
    }

    private void teleportPlayer(ServerPlayerEntity player, ServerWorld targetWorld, BlockPos portalPos, Direction.Axis axis, boolean toConvergence) {
        int targetX = portalPos.getX();
        int targetZ = portalPos.getZ();
        int targetY = Math.max(targetWorld.getBottomY() + 10, Math.min(targetWorld.getTopYInclusive() - 20, portalPos.getY()));

        // 1. Search for existing portal in target world within 16 blocks
        BlockPos existingPortalPos = null;
        for (int dx = -16; dx <= 16; dx++) {
            for (int dz = -16; dz <= 16; dz++) {
                for (int dy = -16; dy <= 16; dy++) {
                    BlockPos check = new BlockPos(targetX + dx, targetY + dy, targetZ + dz);
                    if (targetWorld.isChunkLoaded(check.getX() >> 4, check.getZ() >> 4)) {
                        if (targetWorld.getBlockState(check).isOf(this)) {
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
            BlockState existingState = targetWorld.getBlockState(existingPortalPos);
            Direction.Axis foundAxis = existingState.contains(AXIS) ? existingState.get(AXIS) : axis;
            Direction frontDir = foundAxis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;

            spawnX = existingPortalPos.getX() + 0.5 + frontDir.getOffsetX() * 1.2;
            spawnY = existingPortalPos.getY();
            spawnZ = existingPortalPos.getZ() + 0.5 + frontDir.getOffsetZ() * 1.2;
        } else {
            // Find surface or safe height
            int surfaceY = targetWorld.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, targetX, targetZ);
            int safeY = (surfaceY > targetWorld.getBottomY() + 10 && surfaceY < targetWorld.getTopYInclusive() - 10)
                    ? surfaceY
                    : Math.max(64, Math.min(100, targetY));

            BlockPos basePos = new BlockPos(targetX, safeY, targetZ);
            buildSafeResonanceGateway(targetWorld, basePos, axis);

            Direction frontDir = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
            spawnX = basePos.getX() + 1.5 + frontDir.getOffsetX() * 1.2;
            spawnY = basePos.getY() + 1.0;
            spawnZ = basePos.getZ() + 0.5 + frontDir.getOffsetZ() * 1.2;
        }

        player.setPortalCooldown(100);
        player.teleport(targetWorld, spawnX, spawnY, spawnZ, Set.of(), player.getYaw(), player.getPitch(), true);

        if (toConvergence) {
            player.sendMessage(Text.literal("§5✦ §dEntering The Convergence..."), true);
        } else {
            player.sendMessage(Text.literal("§a✦ Returned safely to the Overworld."), true);
        }

        targetWorld.playSound(null, spawnX, spawnY, spawnZ, SoundEvents.BLOCK_PORTAL_TRAVEL, SoundCategory.PLAYERS, 0.8f, 1.2f);
    }

    private void buildSafeResonanceGateway(ServerWorld world, BlockPos basePos, Direction.Axis axis) {
        Direction widthDir = axis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
        Direction depthDir = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;

        Block[] anchors = new Block[] {
                ModBlocks.ATMOSPHERIC_ANCHOR,
                ModBlocks.KINETIC_ANCHOR,
                ModBlocks.THERMAL_ANCHOR,
                ModBlocks.METALLURGICAL_ANCHOR,
                ModBlocks.PLASMA_ANCHOR,
                ModBlocks.DIMENSIONAL_SINGULARITY
        };

        // Clear space and build solid platform + frame
        // Width: -1 to 2 (4 wide: -1 left pillar, 0..1 interior, 2 right pillar)
        // Height: -1 floor, 0..2 interior, 3 roof
        for (int w = -2; w <= 3; w++) {
            for (int d = -2; d <= 2; d++) {
                for (int h = -1; h <= 5; h++) {
                    BlockPos current = basePos.offset(widthDir, w).offset(depthDir, d).up(h);
                    if (h == -1) {
                        // Solid platform
                        world.setBlockState(current, Blocks.CRYING_OBSIDIAN.getDefaultState());
                    } else if (h >= 0 && h <= 3 && d == 0 && (w >= -1 && w <= 2)) {
                        boolean isBorder = (w == -1 || w == 2 || h == 0 || h == 3);
                        if (isBorder) {
                            // Frame blocks: distribute keystones and crying obsidian
                            if (w == -1 && h == 1) {
                                world.setBlockState(current, anchors[0].getDefaultState());
                            } else if (w == -1 && h == 2) {
                                world.setBlockState(current, anchors[1].getDefaultState());
                            } else if (w == 2 && h == 1) {
                                world.setBlockState(current, anchors[2].getDefaultState());
                            } else if (w == 2 && h == 2) {
                                world.setBlockState(current, anchors[3].getDefaultState());
                            } else if (h == 3 && w == 0) {
                                world.setBlockState(current, anchors[4].getDefaultState());
                            } else if (h == 3 && w == 1) {
                                world.setBlockState(current, anchors[5].getDefaultState());
                            } else {
                                world.setBlockState(current, Blocks.CRYING_OBSIDIAN.getDefaultState());
                            }
                        } else {
                            // Interior: Active rift
                            world.setBlockState(current, ModBlocks.DORMANT_RIFT.getDefaultState().with(AXIS, axis));
                        }
                    } else {
                        // Clear air around portal
                        if (!world.isAir(current)) {
                            world.setBlockState(current, Blocks.AIR.getDefaultState());
                        }
                    }
                }
            }
        }
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, WorldView world, net.minecraft.world.tick.ScheduledTickView tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random) {
        // If the frame around this rift breaks, collapse the rift
        BlockPos below = pos.down();
        BlockPos above = pos.up();
        boolean hasSupport = world.getBlockState(below).isOf(this) || !world.isAir(below);
        boolean hasRoof = world.getBlockState(above).isOf(this) || !world.isAir(above);
        if (!hasSupport || !hasRoof) {
            return Blocks.AIR.getDefaultState();
        }
        return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (random.nextInt(80) == 0) {
            world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.BLOCK_BEACON_AMBIENT, SoundCategory.BLOCKS, 0.5f, 1.9f);
        }

        double x = pos.getX() + random.nextDouble();
        double y = pos.getY() + random.nextDouble();
        double z = pos.getZ() + random.nextDouble();
        world.addParticleClient(ParticleTypes.REVERSE_PORTAL, x, y, z, 0, 0.05, 0);
        if (random.nextBoolean()) {
            world.addParticleClient(ParticleTypes.END_ROD, x, y, z, 0, 0.02, 0);
        }
    }
}
