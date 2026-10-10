package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.world.dimension.ConvergencePortalManager;
import net.enchantedwood.world.dimension.ModDimensions;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DormantRiftBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    private static final Identifier RIFTWOOD_HAVEN_ID = Identifier.fromNamespaceAndPath("enchantedwood", "riftwood_haven");
    private static final Identifier CHERRY_GROVE_ID = Identifier.fromNamespaceAndPath("minecraft", "cherry_grove");

    protected static final VoxelShape X_SHAPE = Block.box(6.0, 0.0, 0.0, 10.0, 16.0, 16.0);
    protected static final VoxelShape Z_SHAPE = Block.box(0.0, 0.0, 6.0, 16.0, 16.0, 10.0);

    public DormantRiftBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.Z ? Z_SHAPE : X_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
        return ItemStack.EMPTY;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide()) {
            player.sendOverlayMessage(Component.literal("§5✦ Gateway of Resonance: §aSynchronized §7— Step through to enter §dThe Convergence§7."));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel world, BlockPos pos, boolean moved) {
        ConvergencePortalManager.unregisterGateway(world, pos);
        super.affectNeighborsAfterRemoval(state, world, pos, moved);
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
            boolean toConvergence = currentWorld.dimension() != ModDimensions.CONVERGENCE_WORLD_KEY;

            if (toConvergence) {
                targetWorld = currentWorld.getServer().getLevel(ModDimensions.CONVERGENCE_WORLD_KEY);
            } else {
                targetWorld = currentWorld.getServer().getLevel(Level.OVERWORLD);
            }

            if (targetWorld == null) {
                player.sendSystemMessage(Component.literal("§c⚠️ Dimension Link Error: Target world unavailable."));
                return;
            }

            teleportPlayer(player, targetWorld, pos, state.getValue(AXIS), toConvergence);
        }
    }

    private void teleportPlayer(ServerPlayer player, ServerLevel targetWorld, BlockPos portalPos, Direction.Axis axis, boolean toConvergence) {
        int targetX = portalPos.getX();
        int targetZ = portalPos.getZ();
        int targetY = Math.max(targetWorld.getMinY() + 10, Math.min(targetWorld.getMaxY() - 20, portalPos.getY()));

        if (toConvergence) {
            // Save the exact Overworld entry portal to persistent storage so the player returns home cleanly
            ConvergencePortalManager.setPlayerReturnPoint(targetWorld.getServer(), player.getUUID(), portalPos);

            // Check if there is already an existing active gateway in Convergence
            BlockPos existingSanctuary = ConvergencePortalManager.findExistingPortal(targetWorld, new BlockPos(targetX, targetY, targetZ), player.getUUID(), false);
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
            // Returning to Overworld: retrieve saved return portal from persistent storage
            BlockPos savedReturn = ConvergencePortalManager.getPlayerReturnPoint(targetWorld.getServer(), player.getUUID());
            if (savedReturn != null) {
                targetX = savedReturn.getX();
                targetZ = savedReturn.getZ();
                targetY = savedReturn.getY();
            }
        }

        // Search for existing portal in target world (persistent registry + 128-block radius)
        BlockPos existingPortalPos = ConvergencePortalManager.findExistingPortal(
                targetWorld, new BlockPos(targetX, targetY, targetZ), player.getUUID(), !toConvergence);

        double spawnX;
        double spawnY;
        double spawnZ;
        BlockPos sanctuaryBase = null;

        if (existingPortalPos != null) {
            BlockState existingState = targetWorld.getBlockState(existingPortalPos);
            Direction.Axis foundAxis = existingState.hasProperty(AXIS) ? existingState.getValue(AXIS) : axis;
            Direction frontDir = foundAxis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;

            spawnX = existingPortalPos.getX() + 0.5 + frontDir.getStepX() * 1.2;
            spawnY = existingPortalPos.getY();
            spawnZ = existingPortalPos.getZ() + 0.5 + frontDir.getStepZ() * 1.2;
            sanctuaryBase = existingPortalPos;
        } else {
            // Find surface or safe height
            int surfaceY = targetWorld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, targetX, targetZ);
            int safeY = (surfaceY > targetWorld.getMinY() + 10 && surfaceY < targetWorld.getMaxY() - 10)
                    ? surfaceY
                    : Math.max(64, Math.min(100, targetY));

            BlockPos basePos = new BlockPos(targetX, safeY, targetZ);
            buildSafeResonanceGateway(targetWorld, basePos, axis);
            sanctuaryBase = basePos;

            Direction frontDir = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
            spawnX = basePos.getX() + 0.5 + frontDir.getStepX() * 1.2;
            spawnY = basePos.getY() + 1.0;
            spawnZ = basePos.getZ() + 0.5 + frontDir.getStepZ() * 1.2;
        }

        player.setPortalCooldown(100);
        player.teleportTo(targetWorld, spawnX, spawnY, spawnZ, Set.of(), player.getYRot(), player.getXRot(), true);

        if (toConvergence) {
            // Provide arrival hazard buffer (45s Acid Protection buffer) and register sanctuary
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.enchantedwood.effect.ModStatusEffects.ACID_PROTECTION, 900, 0, false, false, true));
            player.setAirSupply(player.getMaxAirSupply());
            if (sanctuaryBase != null) {
                net.enchantedwood.event.ConvergenceHazardHandler.registerSanctuary(sanctuaryBase);
            }
            player.sendOverlayMessage(Component.literal("§5✦ §dEntering The Convergence... §a[Sanctuary Outpost: Riftwood Haven]"));
        } else {
            player.sendOverlayMessage(Component.literal("§a✦ Returned safely to the Overworld."));
        }

        targetWorld.playSound(null, spawnX, spawnY, spawnZ, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    private BlockPos findSafeConvergenceSpawn(ServerLevel world, int originX, int originZ) {
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

    private BlockPos searchForDeepBiome(ServerLevel world, int centerX, int centerZ, int maxRadius, int step, Identifier targetBiomeId) {
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

    private static boolean isDeepSafeHaven(ServerLevel world, int x, int z, Identifier biomeId) {
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

    private BlockPos searchForBiome(ServerLevel world, int centerX, int centerZ, int maxRadius, int step, Identifier targetBiomeId) {
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

    private static boolean isBiome(ServerLevel world, int x, int z, Identifier biomeId) {
        var key = world.getBiome(new BlockPos(x, 64, z)).unwrapKey();
        return key.isPresent() && key.get().identifier().equals(biomeId);
    }

    private static boolean isDangerousOrNetherBiome(ServerLevel world, int x, int z) {
        var key = world.getBiome(new BlockPos(x, 64, z)).unwrapKey();
        if (key.isEmpty()) return true;
        Identifier id = key.get().identifier();
        if (id.getNamespace().equals("minecraft") && (
                id.getPath().contains("crimson") ||
                id.getPath().contains("warped") ||
                id.getPath().contains("basalt") ||
                id.getPath().contains("soul_sand") ||
                id.getPath().contains("nether"))) {
            return true;
        }
        return id.equals(Identifier.fromNamespaceAndPath("enchantedwood", "caustic_mire")) ||
                id.equals(Identifier.fromNamespaceAndPath("enchantedwood", "scorched_caldera")) ||
                id.equals(Identifier.fromNamespaceAndPath("enchantedwood", "resonance_sanctum")) ||
                id.equals(Identifier.fromNamespaceAndPath("enchantedwood", "anoxic_barrens"));
    }

    private void buildSafeResonanceGateway(ServerLevel world, BlockPos basePos, Direction.Axis axis) {
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
                    BlockPos current = basePos.relative(widthDir, w).relative(depthDir, d).above(h);

                    if (h == -1) {
                        // Sturdy solid foundation
                        BlockState floorState = (Math.abs(w) % 2 == 0 || Math.abs(d) % 2 == 0)
                                ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
                                : Blocks.SMOOTH_BASALT.defaultBlockState();
                        world.setBlockAndUpdate(current, floorState);
                    } else if (h == 4 || h == 5) {
                        // Weather-proof protective roof (shields completely from rain and sky hazards)
                        world.setBlockAndUpdate(current, Blocks.CRYING_OBSIDIAN.defaultBlockState());
                    } else if (d == 0 && (w >= -1 && w <= 2) && h <= 3) {
                        // Portal structure itself
                        boolean isBorder = (w == -1 || w == 2 || h == 0 || h == 3);
                        if (isBorder) {
                            if (w == -1 && h == 1) {
                                world.setBlockAndUpdate(current, anchors[0].defaultBlockState());
                            } else if (w == -1 && h == 2) {
                                world.setBlockAndUpdate(current, anchors[1].defaultBlockState());
                            } else if (w == 2 && h == 1) {
                                world.setBlockAndUpdate(current, anchors[2].defaultBlockState());
                            } else if (w == 2 && h == 2) {
                                world.setBlockAndUpdate(current, anchors[3].defaultBlockState());
                            } else if (h == 3 && w == 0) {
                                world.setBlockAndUpdate(current, anchors[4].defaultBlockState());
                            } else if (h == 3 && w == 1) {
                                world.setBlockAndUpdate(current, anchors[5].defaultBlockState());
                            } else {
                                world.setBlockAndUpdate(current, Blocks.CRYING_OBSIDIAN.defaultBlockState());
                            }
                        } else {
                            world.setBlockAndUpdate(current, ModBlocks.DORMANT_RIFT.defaultBlockState().setValue(AXIS, axis));
                        }
                    } else if (w == -3 || w == 4 || d == -3 || d == 3) {
                        // Outer walls with observation windows and doorway
                        boolean isDoorway = (d == 3 && (w == 0 || w == 1) && h <= 2);
                        boolean isCorner = (w == -3 || w == 4) && (d == -3 || d == 3);
                        if (isDoorway) {
                            world.setBlockAndUpdate(current, Blocks.AIR.defaultBlockState());
                        } else if (isCorner || h == 0 || h == 3) {
                            world.setBlockAndUpdate(current, Blocks.SMOOTH_BASALT.defaultBlockState());
                        } else {
                            // Observation window
                            world.setBlockAndUpdate(current, Blocks.TINTED_GLASS.defaultBlockState());
                        }
                    } else {
                        // Interior space: clear air
                        world.setBlockAndUpdate(current, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }

        // Place protective sanctuary lanterns inside for lighting
        BlockPos lightPos = basePos.relative(widthDir, -2).relative(depthDir, 2).above(0);
        world.setBlockAndUpdate(lightPos, Blocks.LANTERN.defaultBlockState());
        BlockPos lightPos2 = basePos.relative(widthDir, 3).relative(depthDir, 2).above(0);
        world.setBlockAndUpdate(lightPos2, Blocks.LANTERN.defaultBlockState());

        // Register sanctuary center and gateway
        net.enchantedwood.event.ConvergenceHazardHandler.registerSanctuary(basePos);
        ConvergencePortalManager.registerGateway(world, basePos);
    }

    @Override
    public BlockState updateShape(BlockState state, LevelReader world, net.minecraft.world.level.ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        // If the frame around this rift breaks, collapse the rift
        BlockPos below = pos.below();
        BlockPos above = pos.above();
        boolean hasSupport = world.getBlockState(below).is(this) || !world.isEmptyBlock(below);
        boolean hasRoof = world.getBlockState(above).is(this) || !world.isEmptyBlock(above);
        if (!hasSupport || !hasRoof) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (random.nextInt(80) == 0) {
            world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.5f, 1.9f);
        }

        double x = pos.getX() + random.nextDouble();
        double y = pos.getY() + random.nextDouble();
        double z = pos.getZ() + random.nextDouble();
        world.addParticle(ParticleTypes.REVERSE_PORTAL, x, y, z, 0, 0.05, 0);
        if (random.nextBoolean()) {
            world.addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0.02, 0);
        }
    }
}
