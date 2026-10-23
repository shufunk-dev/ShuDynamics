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
import net.enchantedwood.world.dimension.ConvergencePortalManager;
import net.enchantedwood.world.dimension.ModDimensions;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DormantRiftBlock extends Block {
    public static final MapCodec<DormantRiftBlock> CODEC = createCodec(DormantRiftBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = Properties.HORIZONTAL_AXIS;

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
        if (!world.isClient()) {
            player.sendMessage(
                    Text.literal("§5✦ Gateway of Resonance: §aSynchronized §7— Step through to enter §dThe Convergence§7."),
                    true
            );
        }
        return ActionResult.SUCCESS;
    }

    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        ConvergencePortalManager.unregisterGateway(world, pos);
        super.onStateReplaced(state, world, pos, moved);
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
            // Save the exact Overworld entry portal to persistent storage so the player returns home cleanly
            ConvergencePortalManager.setPlayerReturnPoint(targetWorld.getServer(), player.getUuid(), portalPos);

            // Check if there is already an existing active gateway in Convergence
            BlockPos existingSanctuary = ConvergencePortalManager.findExistingPortal(targetWorld, new BlockPos(targetX, targetY, targetZ), player.getUuid(), false);
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
            BlockPos savedReturn = ConvergencePortalManager.getPlayerReturnPoint(targetWorld.getServer(), player.getUuid());
            if (savedReturn != null) {
                targetX = savedReturn.getX();
                targetZ = savedReturn.getZ();
                targetY = savedReturn.getY();
            }
        }

        // Search for existing portal in target world (persistent registry + 128-block radius)
        BlockPos existingPortalPos = ConvergencePortalManager.findExistingPortal(
                targetWorld, new BlockPos(targetX, targetY, targetZ), player.getUuid(), !toConvergence);

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
            // Find true ground surface in a genuine open clearing
            BlockPos basePos = findOpenClearing(targetWorld, targetX, targetZ, targetY);
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
        return net.enchantedwood.event.ConvergenceHazardHandler.isAcidBiome(id) ||
                net.enchantedwood.event.ConvergenceHazardHandler.isThermalBiome(id) ||
                net.enchantedwood.event.ConvergenceHazardHandler.isAnoxicBiome(id) ||
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

        // 1. Clear a wide glade/perimeter around the entire outpost (width -7 to 8, depth -7 to 7, height 0 to 10)
        // so no leaves, vines, or stray trunks clip through windows or hang over the building
        for (int w = -7; w <= 8; w++) {
            for (int d = -7; d <= 7; d++) {
                for (int h = 0; h <= 10; h++) {
                    BlockPos p = basePos.offset(widthDir, w).offset(depthDir, d).up(h);
                    BlockState s = world.getBlockState(p);
                    if (isDenseTrunk(s) || isCanopyOrVegetation(s) || s.isIn(net.minecraft.registry.tag.BlockTags.LEAVES)) {
                        world.setBlockState(p, Blocks.AIR.getDefaultState());
                    }
                }
                // If the ground under cleared vegetation was carved away, establish nice grass lawn
                BlockPos floorP = basePos.offset(widthDir, w).offset(depthDir, d).down();
                BlockState s = world.getBlockState(floorP);
                if (isDenseTrunk(s) || isCanopyOrVegetation(s)) {
                    world.setBlockState(floorP, Blocks.GRASS_BLOCK.getDefaultState());
                }
            }
        }

        // 2. Build fully enclosed Resonance Sanctuary Outpost:
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

        // 3. Extend sturdy solid foundation downwards to the terrain floor to prevent floating outposts
        for (int w = -3; w <= 4; w++) {
            for (int d = -3; d <= 3; d++) {
                for (int down = -2; down >= -24; down--) {
                    BlockPos suppPos = basePos.offset(widthDir, w).offset(depthDir, d).up(down);
                    BlockState existing = world.getBlockState(suppPos);
                    if (existing.isAir() || isCanopyOrVegetation(existing) || isDenseTrunk(existing)) {
                        world.setBlockState(suppPos, Blocks.SMOOTH_BASALT.getDefaultState());
                    } else {
                        break;
                    }
                }
            }
        }

        // 4. Exterior walkway, descending steps and ground access outside the doorway (d == 3, w == 0, 1)
        // Extend 10 blocks out into open terrain so player has a clean paved exit path
        for (int step = 1; step <= 10; step++) {
            for (int w = -1; w <= 2; w++) {
                BlockPos walkPos = basePos.offset(widthDir, w).offset(depthDir, 3 + step);
                // Clear 5 blocks of air for generous open walking space and view
                for (int h = 0; h <= 4; h++) {
                    world.setBlockState(walkPos.up(h), Blocks.AIR.getDefaultState());
                }

                // Path surface and support
                if (w == 0 || w == 1) {
                    for (int down = -1; down >= -24; down--) {
                        BlockPos stepSupp = walkPos.up(down);
                        BlockState existing = world.getBlockState(stepSupp);
                        if (existing.isAir() || isCanopyOrVegetation(existing) || isDenseTrunk(existing)) {
                            world.setBlockState(stepSupp, (down == -1) ? ((step % 2 == 0) ? Blocks.CRYING_OBSIDIAN.getDefaultState() : Blocks.SMOOTH_BASALT.getDefaultState()) : Blocks.SMOOTH_BASALT.getDefaultState());
                        } else {
                            break;
                        }
                    }
                }
            }
        }

        // 5. Pathway gate posts and lanterns at the end of the walkway
        BlockPos gatePostL = basePos.offset(widthDir, -1).offset(depthDir, 13);
        BlockPos gatePostR = basePos.offset(widthDir, 2).offset(depthDir, 13);
        world.setBlockState(gatePostL, Blocks.SMOOTH_BASALT.getDefaultState());
        world.setBlockState(gatePostL.up(1), Blocks.LANTERN.getDefaultState());
        world.setBlockState(gatePostR, Blocks.SMOOTH_BASALT.getDefaultState());
        world.setBlockState(gatePostR.up(1), Blocks.LANTERN.getDefaultState());

        // 6. Exterior emergency rescue ladder along the wall next to the door
        BlockPos ladderWall = basePos.offset(widthDir, -1).offset(depthDir, 3);
        Direction ladderFacing = depthDir; // Facing outwards into the walkway space
        for (int down = 0; down >= -24; down--) {
            BlockPos ladderPos = basePos.offset(widthDir, -1).offset(depthDir, 4).up(down);
            BlockPos wallPos = ladderWall.up(down);
            BlockState current = world.getBlockState(ladderPos);
            if (current.isAir() || isCanopyOrVegetation(current) || isDenseTrunk(current)) {
                // Ensure wall behind ladder exists
                world.setBlockState(wallPos, Blocks.SMOOTH_BASALT.getDefaultState());
                world.setBlockState(ladderPos, Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, ladderFacing));
            } else {
                break;
            }
        }

        // 7. Place protective sanctuary lanterns inside for lighting
        BlockPos lightPos = basePos.offset(widthDir, -2).offset(depthDir, 2).up(0);
        world.setBlockState(lightPos, Blocks.LANTERN.getDefaultState());
        BlockPos lightPos2 = basePos.offset(widthDir, 3).offset(depthDir, 2).up(0);
        world.setBlockState(lightPos2, Blocks.LANTERN.getDefaultState());

        // 8. Register sanctuary center and gateway
        net.enchantedwood.event.ConvergenceHazardHandler.registerSanctuary(basePos);
        ConvergencePortalManager.registerGateway(world, basePos);
    }

    private BlockPos findOpenClearing(ServerWorld world, int originX, int originZ, int defaultY) {
        int step = 4;
        int maxRadius = 96;

        BlockPos bestCandidate = null;
        int minTrunks = Integer.MAX_VALUE;

        // Spiral/concentric search outward from origin
        for (int r = 0; r <= maxRadius; r += step) {
            for (int i = -r; i <= r; i += step) {
                int[][] candidateCoords = (r == 0)
                        ? new int[][]{{originX, originZ}}
                        : new int[][]{
                                {originX + i, originZ - r},
                                {originX + i, originZ + r},
                                {originX - r, originZ + i},
                                {originX + r, originZ + i}
                        };

                for (int[] coord : candidateCoords) {
                    int cx = coord[0];
                    int cz = coord[1];

                    // Check if candidate is in safe haven (Riftwood Haven or Cherry Grove)
                    if (!isDeepSafeHaven(world, cx, cz, RIFTWOOD_HAVEN_ID)
                            && !isBiome(world, cx, cz, RIFTWOOD_HAVEN_ID)
                            && !isBiome(world, cx, cz, CHERRY_GROVE_ID)) {
                        continue;
                    }

                    int groundY = evaluateClearingGround(world, cx, cz);
                    if (groundY <= 0) continue;

                    int trunks = countSurroundingTrunks(world, cx, groundY, cz, 6);
                    if (trunks == 0) {
                        // Perfect open clearing with no trees in the immediate vicinity!
                        return new BlockPos(cx, groundY, cz);
                    }

                    if (trunks < minTrunks) {
                        minTrunks = trunks;
                        bestCandidate = new BlockPos(cx, groundY, cz);
                    }
                }
            }
        }

        if (bestCandidate != null) {
            return bestCandidate;
        }

        // Fallback: search at origin
        world.getChunk(originX >> 4, originZ >> 4, net.minecraft.world.chunk.ChunkStatus.FULL, true);
        int fallbackY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, originX, originZ);
        if (fallbackY < world.getSeaLevel()) fallbackY = world.getSeaLevel() + 1;
        return new BlockPos(originX, fallbackY, originZ);
    }

    private static int evaluateClearingGround(ServerWorld world, int x, int z) {
        world.getChunk(x >> 4, z >> 4, net.minecraft.world.chunk.ChunkStatus.FULL, true);
        int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);
        if (topY < world.getSeaLevel() || topY >= world.getTopYInclusive() - 15) {
            return -1;
        }

        // Check from topY down to find solid dry surface
        BlockPos.Mutable scan = new BlockPos.Mutable(x, topY, z);
        while (scan.getY() >= world.getSeaLevel()) {
            BlockState s = world.getBlockState(scan);
            // If water encountered, this is a lake/ocean - reject!
            if (!s.getFluidState().isEmpty() || s.isLiquid()) {
                return -1;
            }
            if (s.isAir() || isAirOrFoliage(s)) {
                scan.move(Direction.DOWN);
                continue;
            }
            // If it hits a log or mushroom stem, this spot is inside a tree
            if (isDenseTrunk(s)) {
                return -1;
            }
            if (s.isOpaqueFullCube()) {
                // Found genuine solid terrain (dirt, grass, stone, sand, etc.)
                int groundY = scan.getY() + 1;
                // Verify terrain consistency in immediate 3x3 footprint
                int maxDelta = 0;
                int[][] checks = {{-3, 0}, {3, 0}, {0, -3}, {0, 3}, {-3, -3}, {3, 3}};
                for (int[] offset : checks) {
                    int neighborTop = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x + offset[0], z + offset[1]);
                    maxDelta = Math.max(maxDelta, Math.abs(neighborTop - groundY));
                }
                if (maxDelta > 6) {
                    return -1; // Too steep/cliff
                }
                return groundY;
            }
            scan.move(Direction.DOWN);
        }
        return -1;
    }

    private static int countSurroundingTrunks(ServerWorld world, int cx, int groundY, int cz, int radius) {
        int count = 0;
        BlockPos.Mutable scan = new BlockPos.Mutable();
        for (int dx = -radius; dx <= radius; dx += 2) {
            for (int dz = -radius; dz <= radius; dz += 2) {
                for (int dy = 0; dy <= 8; dy += 2) {
                    scan.set(cx + dx, groundY + dy, cz + dz);
                    BlockState s = world.getBlockState(scan);
                    if (isDenseTrunk(s)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static boolean isDenseTrunk(BlockState state) {
        if (state == null || state.isAir()) return false;
        if (state.isIn(net.minecraft.registry.tag.BlockTags.LOGS)) return true;
        Block b = state.getBlock();
        return b == Blocks.MUSHROOM_STEM
                || b == Blocks.BROWN_MUSHROOM_BLOCK
                || b == Blocks.RED_MUSHROOM_BLOCK
                || b == Blocks.SHROOMLIGHT
                || b == Blocks.MANGROVE_ROOTS
                || b == Blocks.MUDDY_MANGROVE_ROOTS;
    }

    private static boolean isAirOrFoliage(BlockState state) {
        if (state == null || state.isAir()) return true;
        if (state.isIn(net.minecraft.registry.tag.BlockTags.LEAVES)) return true;
        if (state.isIn(net.minecraft.registry.tag.BlockTags.FLOWERS)) return true;
        Block b = state.getBlock();
        return b == Blocks.VINE
                || b == Blocks.SHORT_GRASS
                || b == Blocks.TALL_GRASS
                || b == Blocks.FERN
                || b == Blocks.LARGE_FERN
                || b == Blocks.BROWN_MUSHROOM
                || b == Blocks.RED_MUSHROOM;
    }

    private static boolean isCanopyOrVegetation(BlockState state) {
        if (state == null || state.isAir()) return true;
        if (state.isIn(net.minecraft.registry.tag.BlockTags.LEAVES)) return true;
        if (state.isIn(net.minecraft.registry.tag.BlockTags.LOGS)) return true;
        if (state.isIn(net.minecraft.registry.tag.BlockTags.FLOWERS)) return true;
        Block b = state.getBlock();
        return b == Blocks.BROWN_MUSHROOM_BLOCK
                || b == Blocks.RED_MUSHROOM_BLOCK
                || b == Blocks.MUSHROOM_STEM
                || b == Blocks.SHROOMLIGHT
                || b == Blocks.MANGROVE_ROOTS
                || b == Blocks.MUDDY_MANGROVE_ROOTS
                || b == Blocks.VINE
                || b == Blocks.SHORT_GRASS
                || b == Blocks.TALL_GRASS
                || b == Blocks.FERN
                || b == Blocks.LARGE_FERN
                || b == Blocks.BROWN_MUSHROOM
                || b == Blocks.RED_MUSHROOM;
    }

    private static int findTrueGroundSurfaceY(ServerWorld world, int x, int z, int defaultY) {
        int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);
        BlockPos.Mutable scan = new BlockPos.Mutable(x, topY, z);
        while (scan.getY() > world.getBottomY() + 10) {
            BlockState state = world.getBlockState(scan);
            if (isAirOrFoliage(state)) {
                scan.move(Direction.DOWN);
            } else if (!state.isOpaqueFullCube()) {
                scan.move(Direction.DOWN);
            } else {
                return scan.getY() + 1;
            }
        }
        return Math.max(64, Math.min(100, defaultY));
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
