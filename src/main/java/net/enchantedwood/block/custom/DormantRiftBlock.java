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
import net.minecraft.util.Identifier;
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

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DormantRiftBlock extends Block {
    public static final MapCodec<DormantRiftBlock> CODEC = createCodec(DormantRiftBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = Properties.HORIZONTAL_AXIS;

    private static final Map<UUID, BlockPos> OVERWORLD_RETURN_POINTS = new ConcurrentHashMap<>();
    private static final Identifier RIFTWOOD_HAVEN_ID = Identifier.of("enchantedwood", "riftwood_haven");
    private static final Identifier CHERRY_GROVE_ID = Identifier.of("minecraft", "cherry_grove");

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

        if (toConvergence) {
            // Save the exact Overworld entry portal so the player returns home cleanly
            OVERWORLD_RETURN_POINTS.put(player.getUuid(), portalPos);

            // Check if there is already an existing active sanctuary in Convergence
            BlockPos existingSanctuary = findExistingConvergencePortal(targetWorld);
            if (existingSanctuary != null) {
                targetX = existingSanctuary.getX();
                targetZ = existingSanctuary.getZ();
                targetY = existingSanctuary.getY();
            } else {
                BlockPos safeSpot = findSafeConvergenceSpawn(targetWorld, targetX, targetZ);
                targetX = safeSpot.getX();
                targetZ = safeSpot.getZ();
            }
        } else {
            // Returning to Overworld: retrieve saved return portal if available
            BlockPos savedReturn = OVERWORLD_RETURN_POINTS.get(player.getUuid());
            if (savedReturn != null) {
                targetX = savedReturn.getX();
                targetZ = savedReturn.getZ();
                targetY = savedReturn.getY();
            }
        }

        // 1. Search for existing portal in target world within 24 blocks of target
        BlockPos existingPortalPos = null;
        for (int dx = -24; dx <= 24; dx++) {
            for (int dz = -24; dz <= 24; dz++) {
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
        BlockPos sanctuaryBase = null;

        if (existingPortalPos != null) {
            BlockState existingState = targetWorld.getBlockState(existingPortalPos);
            Direction.Axis foundAxis = existingState.contains(AXIS) ? existingState.get(AXIS) : axis;
            Direction frontDir = foundAxis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;

            spawnX = existingPortalPos.getX() + 0.5 + frontDir.getOffsetX() * 1.2;
            spawnY = existingPortalPos.getY();
            spawnZ = existingPortalPos.getZ() + 0.5 + frontDir.getOffsetZ() * 1.2;
            sanctuaryBase = existingPortalPos;
        } else {
            // Find surface or safe height
            int surfaceY = targetWorld.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, targetX, targetZ);
            int safeY = (surfaceY > targetWorld.getBottomY() + 10 && surfaceY < targetWorld.getTopYInclusive() - 10)
                    ? surfaceY
                    : Math.max(64, Math.min(100, targetY));

            BlockPos basePos = new BlockPos(targetX, safeY, targetZ);
            buildSafeResonanceGateway(targetWorld, basePos, axis);
            sanctuaryBase = basePos;

            Direction frontDir = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
            spawnX = basePos.getX() + 0.5 + frontDir.getOffsetX() * 1.2;
            spawnY = basePos.getY() + 1.0;
            spawnZ = basePos.getZ() + 0.5 + frontDir.getOffsetZ() * 1.2;
        }

        player.setPortalCooldown(100);
        player.teleport(targetWorld, spawnX, spawnY, spawnZ, Set.of(), player.getYaw(), player.getPitch(), true);

        if (toConvergence) {
            // Provide arrival hazard buffer (45s Acid Protection buffer) and register sanctuary
            player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                    net.enchantedwood.effect.ModStatusEffects.ACID_PROTECTION, 900, 0, false, false, true));
            player.setAir(player.getMaxAir());
            if (sanctuaryBase != null) {
                net.enchantedwood.event.ConvergenceHazardHandler.registerSanctuary(sanctuaryBase);
            }
            player.sendMessage(Text.literal("§5✦ §dEntering The Convergence... §a[Sanctuary Outpost: Riftwood Haven]"), true);
        } else {
            player.sendMessage(Text.literal("§a✦ Returned safely to the Overworld."), true);
        }

        targetWorld.playSound(null, spawnX, spawnY, spawnZ, SoundEvents.BLOCK_PORTAL_TRAVEL, SoundCategory.PLAYERS, 0.8f, 1.2f);
    }

    private BlockPos findExistingConvergencePortal(ServerWorld convergenceWorld) {
        for (BlockPos pos : net.enchantedwood.event.ConvergenceHazardHandler.SANCTUARY_CENTERS) {
            if (convergenceWorld.getBlockState(pos).isOf(this) ||
                    convergenceWorld.getBlockState(pos.up()).isOf(this) ||
                    convergenceWorld.getBlockState(pos.down()).isOf(this)) {
                return pos;
            }
        }
        return null;
    }

    private BlockPos findSafeConvergenceSpawn(ServerWorld world, int originX, int originZ) {
        // 1. If origin coordinate is already deep in Riftwood Haven, use it
        if (isDeepSafeHaven(world, originX, originZ, RIFTWOOD_HAVEN_ID)) {
            return new BlockPos(originX, 64, originZ);
        }

        // 2. Search outward around origin for deep Riftwood Haven (requiring 64-block safe buffer)
        BlockPos havenPos = searchForDeepBiome(world, originX, originZ, 1600, 32, RIFTWOOD_HAVEN_ID);
        if (havenPos != null) {
            return havenPos;
        }

        // 3. Search around (0, 0) where Riftwood Haven is centered
        if (originX != 0 || originZ != 0) {
            havenPos = searchForDeepBiome(world, 0, 0, 1200, 32, RIFTWOOD_HAVEN_ID);
            if (havenPos != null) {
                return havenPos;
            }
        }

        // 4. Fallback: standard haven search
        havenPos = searchForBiome(world, originX, originZ, 1600, 24, RIFTWOOD_HAVEN_ID);
        if (havenPos != null) {
            return havenPos;
        }

        // 5. Fallback search: Cherry Grove safe haven
        BlockPos cherryPos = searchForDeepBiome(world, originX, originZ, 1200, 32, CHERRY_GROVE_ID);
        if (cherryPos != null) {
            return cherryPos;
        }

        return new BlockPos(0, 64, 0);
    }

    private BlockPos searchForDeepBiome(ServerWorld world, int centerX, int centerZ, int maxRadius, int step, Identifier targetBiomeId) {
        for (int r = step; r <= maxRadius; r += step) {
            for (int i = -r; i <= r; i += step) {
                if (isDeepSafeHaven(world, centerX + i, centerZ - r, targetBiomeId)) {
                    return new BlockPos(centerX + i, 64, centerZ - r);
                }
                if (isDeepSafeHaven(world, centerX + i, centerZ + r, targetBiomeId)) {
                    return new BlockPos(centerX + i, 64, centerZ + r);
                }
                if (isDeepSafeHaven(world, centerX - r, centerZ + i, targetBiomeId)) {
                    return new BlockPos(centerX - r, 64, centerZ + i);
                }
                if (isDeepSafeHaven(world, centerX + r, centerZ + i, targetBiomeId)) {
                    return new BlockPos(centerX + r, 64, centerZ + i);
                }
            }
        }
        return null;
    }

    private static boolean isDeepSafeHaven(ServerWorld world, int x, int z, Identifier biomeId) {
        int[] d = {-64, 0, 64};
        for (int dx : d) {
            for (int dz : d) {
                if (!isBiome(world, x + dx, z + dz, biomeId)) {
                    return false;
                }
            }
        }
        return true;
    }

    private BlockPos searchForBiome(ServerWorld world, int centerX, int centerZ, int maxRadius, int step, Identifier targetBiomeId) {
        for (int r = step; r <= maxRadius; r += step) {
            for (int i = -r; i <= r; i += step) {
                if (isBiome(world, centerX + i, centerZ - r, targetBiomeId)) {
                    return new BlockPos(centerX + i, 64, centerZ - r);
                }
                if (isBiome(world, centerX + i, centerZ + r, targetBiomeId)) {
                    return new BlockPos(centerX + i, 64, centerZ + r);
                }
                if (isBiome(world, centerX - r, centerZ + i, targetBiomeId)) {
                    return new BlockPos(centerX - r, 64, centerZ + i);
                }
                if (isBiome(world, centerX + r, centerZ + i, targetBiomeId)) {
                    return new BlockPos(centerX + r, 64, centerZ + i);
                }
            }
        }
        return null;
    }

    private static boolean isBiome(ServerWorld world, int x, int z, Identifier biomeId) {
        var key = world.getBiome(new BlockPos(x, 64, z)).getKey();
        return key.isPresent() && key.get().getValue().equals(biomeId);
    }

    private static boolean isDangerousOrNetherBiome(ServerWorld world, int x, int z) {
        var key = world.getBiome(new BlockPos(x, 64, z)).getKey();
        if (key.isEmpty()) return true;
        Identifier id = key.get().getValue();
        if (id.getNamespace().equals("minecraft") && (
                id.getPath().contains("crimson") ||
                id.getPath().contains("warped") ||
                id.getPath().contains("basalt") ||
                id.getPath().contains("soul_sand") ||
                id.getPath().contains("nether"))) {
            return true;
        }
        return id.equals(Identifier.of("enchantedwood", "caustic_mire")) ||
                id.equals(Identifier.of("enchantedwood", "scorched_caldera")) ||
                id.equals(Identifier.of("enchantedwood", "resonance_sanctum"));
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

        // Build fully enclosed Resonance Sanctuary Outpost:
        // Floor: h == -1
        // Interior: h == 0..3
        // Roof: h == 4..5
        // Width: -3 to 4, Depth: -3 to 3
        for (int w = -3; w <= 4; w++) {
            for (int d = -3; d <= 3; d++) {
                for (int h = -1; h <= 5; h++) {
                    BlockPos current = basePos.offset(widthDir, w).offset(depthDir, d).up(h);

                    if (h == -1) {
                        // Sturdy solid foundation
                        BlockState floorState = (Math.abs(w) % 2 == 0 || Math.abs(d) % 2 == 0)
                                ? Blocks.CRYING_OBSIDIAN.getDefaultState()
                                : Blocks.SMOOTH_BASALT.getDefaultState();
                        world.setBlockState(current, floorState);
                    } else if (h == 4 || h == 5) {
                        // Weather-proof protective roof (shields completely from rain and sky hazards)
                        world.setBlockState(current, Blocks.CRYING_OBSIDIAN.getDefaultState());
                    } else if (d == 0 && (w >= -1 && w <= 2) && h <= 3) {
                        // Portal structure itself
                        boolean isBorder = (w == -1 || w == 2 || h == 0 || h == 3);
                        if (isBorder) {
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
                            world.setBlockState(current, ModBlocks.DORMANT_RIFT.getDefaultState().with(AXIS, axis));
                        }
                    } else if (w == -3 || w == 4 || d == -3 || d == 3) {
                        // Outer walls with observation windows and doorway
                        boolean isDoorway = (d == 3 && (w == 0 || w == 1) && h <= 2);
                        boolean isCorner = (w == -3 || w == 4) && (d == -3 || d == 3);
                        if (isDoorway) {
                            world.setBlockState(current, Blocks.AIR.getDefaultState());
                        } else if (isCorner || h == 0 || h == 3) {
                            world.setBlockState(current, Blocks.SMOOTH_BASALT.getDefaultState());
                        } else {
                            // Observation window
                            world.setBlockState(current, Blocks.TINTED_GLASS.getDefaultState());
                        }
                    } else {
                        // Interior space: clear air
                        world.setBlockState(current, Blocks.AIR.getDefaultState());
                    }
                }
            }
        }

        // Place protective sanctuary lanterns inside for lighting
        BlockPos lightPos = basePos.offset(widthDir, -2).offset(depthDir, 2).up(0);
        world.setBlockState(lightPos, Blocks.LANTERN.getDefaultState());
        BlockPos lightPos2 = basePos.offset(widthDir, 3).offset(depthDir, 2).up(0);
        world.setBlockState(lightPos2, Blocks.LANTERN.getDefaultState());

        // Register sanctuary center
        net.enchantedwood.event.ConvergenceHazardHandler.registerSanctuary(basePos);
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
