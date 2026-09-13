package net.enchantedwood.world.dimension;

import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.event.ConvergenceHazardHandler;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtLong;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ConvergencePortalManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("ConvergencePortalManager");

    private static final Map<UUID, BlockPos> PLAYER_RETURN_POINTS = new ConcurrentHashMap<>();
    private static final Set<BlockPos> OVERWORLD_GATEWAYS = ConcurrentHashMap.newKeySet();
    private static final Set<BlockPos> CONVERGENCE_GATEWAYS = ConcurrentHashMap.newKeySet();

    private static boolean loaded = false;

    public static synchronized void ensureLoaded(MinecraftServer server) {
        if (loaded || server == null) return;
        Path file = getSaveFilePath(server);
        if (Files.exists(file)) {
            try {
                NbtCompound root = NbtIo.readCompressed(file, NbtSizeTracker.ofUnlimitedBytes());
                if (root != null) {
                    NbtCompound returnsNbt = root.getCompoundOrEmpty("player_returns");
                    for (String key : returnsNbt.getKeys()) {
                        try {
                            UUID uuid = UUID.fromString(key);
                            long posLong = returnsNbt.getLong(key, 0L);
                            if (posLong != 0L) {
                                PLAYER_RETURN_POINTS.put(uuid, BlockPos.fromLong(posLong));
                            }
                        } catch (Exception ignored) {
                        }
                    }

                    NbtList overworldList = root.getListOrEmpty("overworld_gateways");
                    for (int i = 0; i < overworldList.size(); i++) {
                        NbtElement el = overworldList.get(i);
                        if (el instanceof NbtLong nbtLong) {
                            OVERWORLD_GATEWAYS.add(BlockPos.fromLong(nbtLong.longValue()));
                        }
                    }

                    NbtList convList = root.getListOrEmpty("convergence_gateways");
                    for (int i = 0; i < convList.size(); i++) {
                        NbtElement el = convList.get(i);
                        if (el instanceof NbtLong nbtLong) {
                            CONVERGENCE_GATEWAYS.add(BlockPos.fromLong(nbtLong.longValue()));
                        }
                    }
                    LOGGER.info("Loaded {} player returns, {} Overworld gateways, {} Convergence gateways.",
                            PLAYER_RETURN_POINTS.size(), OVERWORLD_GATEWAYS.size(), CONVERGENCE_GATEWAYS.size());
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load saved resonance gateways from disk", e);
            }
        }
        loaded = true;
    }

    public static synchronized void save(MinecraftServer server) {
        if (server == null) return;
        Path file = getSaveFilePath(server);
        try {
            NbtCompound root = new NbtCompound();

            NbtCompound returnsNbt = new NbtCompound();
            for (Map.Entry<UUID, BlockPos> entry : PLAYER_RETURN_POINTS.entrySet()) {
                returnsNbt.putLong(entry.getKey().toString(), entry.getValue().asLong());
            }
            root.put("player_returns", returnsNbt);

            NbtList overworldList = new NbtList();
            for (BlockPos pos : OVERWORLD_GATEWAYS) {
                overworldList.add(NbtLong.of(pos.asLong()));
            }
            root.put("overworld_gateways", overworldList);

            NbtList convList = new NbtList();
            for (BlockPos pos : CONVERGENCE_GATEWAYS) {
                convList.add(NbtLong.of(pos.asLong()));
            }
            root.put("convergence_gateways", convList);

            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            NbtIo.writeCompressed(root, file);
        } catch (IOException e) {
            LOGGER.error("Failed to save resonance gateways to disk", e);
        }
    }

    private static Path getSaveFilePath(MinecraftServer server) {
        return server.getSavePath(WorldSavePath.ROOT).resolve("shudynamics_gateways.dat");
    }

    public static void setPlayerReturnPoint(MinecraftServer server, UUID playerUuid, BlockPos overworldPos) {
        ensureLoaded(server);
        PLAYER_RETURN_POINTS.put(playerUuid, overworldPos.toImmutable());
        OVERWORLD_GATEWAYS.add(overworldPos.toImmutable());
        save(server);
    }

    public static BlockPos getPlayerReturnPoint(MinecraftServer server, UUID playerUuid) {
        ensureLoaded(server);
        return PLAYER_RETURN_POINTS.get(playerUuid);
    }

    public static void registerGateway(ServerWorld world, BlockPos pos) {
        if (world == null || pos == null) return;
        ensureLoaded(world.getServer());
        BlockPos immutablePos = pos.toImmutable();
        if (world.getRegistryKey() == World.OVERWORLD) {
            OVERWORLD_GATEWAYS.add(immutablePos);
        } else if (world.getRegistryKey() == ModDimensions.CONVERGENCE_WORLD_KEY) {
            CONVERGENCE_GATEWAYS.add(immutablePos);
            ConvergenceHazardHandler.registerSanctuary(immutablePos);
        }
        save(world.getServer());
    }

    public static void unregisterGateway(ServerWorld world, BlockPos pos) {
        if (world == null || pos == null) return;
        ensureLoaded(world.getServer());
        if (world.getRegistryKey() == World.OVERWORLD) {
            OVERWORLD_GATEWAYS.removeIf(p -> p.getSquaredDistance(pos) <= 9);
            PLAYER_RETURN_POINTS.entrySet().removeIf(e -> e.getValue().getSquaredDistance(pos) <= 9);
        } else if (world.getRegistryKey() == ModDimensions.CONVERGENCE_WORLD_KEY) {
            CONVERGENCE_GATEWAYS.removeIf(p -> p.getSquaredDistance(pos) <= 9);
        }
        save(world.getServer());
    }

    public static BlockPos findExistingPortal(ServerWorld targetWorld, BlockPos targetPos, UUID playerUuid, boolean returningToOverworld) {
        if (targetWorld == null) return null;
        ensureLoaded(targetWorld.getServer());

        if (returningToOverworld) {
            // 1. Try saved player return point
            if (playerUuid != null) {
                BlockPos saved = PLAYER_RETURN_POINTS.get(playerUuid);
                if (saved != null) {
                    targetWorld.getChunk(saved.getX() >> 4, saved.getZ() >> 4, ChunkStatus.FULL, true);
                    BlockPos exact = findExactRiftPos(targetWorld, saved);
                    if (exact != null) {
                        return exact;
                    }
                }
            }

            // 2. Check registered Overworld gateways sorted by distance
            BlockPos closestGw = OVERWORLD_GATEWAYS.stream()
                    .filter(gw -> gw.getSquaredDistance(targetPos) <= 256 * 256)
                    .min(Comparator.comparingDouble(gw -> gw.getSquaredDistance(targetPos)))
                    .orElse(null);

            if (closestGw != null) {
                targetWorld.getChunk(closestGw.getX() >> 4, closestGw.getZ() >> 4, ChunkStatus.FULL, true);
                BlockPos exact = findExactRiftPos(targetWorld, closestGw);
                if (exact != null) {
                    return exact;
                } else {
                    OVERWORLD_GATEWAYS.remove(closestGw);
                    save(targetWorld.getServer());
                }
            }

            // 3. Scan 128-block area in loaded chunks
            BlockPos scanned = scanForRiftInRadius(targetWorld, targetPos, 128);
            if (scanned != null) {
                OVERWORLD_GATEWAYS.add(scanned.toImmutable());
                save(targetWorld.getServer());
                return scanned;
            }

            return null;
        } else {
            // Entering The Convergence
            // 1. Check registered Convergence gateways and sanctuaries
            Set<BlockPos> candidates = ConcurrentHashMap.newKeySet();
            candidates.addAll(CONVERGENCE_GATEWAYS);
            candidates.addAll(ConvergenceHazardHandler.SANCTUARY_CENTERS);

            BlockPos closestGw = candidates.stream()
                    .filter(gw -> gw.getSquaredDistance(targetPos) <= 256 * 256)
                    .min(Comparator.comparingDouble(gw -> gw.getSquaredDistance(targetPos)))
                    .orElse(null);

            if (closestGw != null) {
                targetWorld.getChunk(closestGw.getX() >> 4, closestGw.getZ() >> 4, ChunkStatus.FULL, true);
                BlockPos exact = findExactRiftPos(targetWorld, closestGw);
                if (exact != null) {
                    return exact;
                }
            }

            // 2. Scan 128-block area
            BlockPos scanned = scanForRiftInRadius(targetWorld, targetPos, 128);
            if (scanned != null) {
                CONVERGENCE_GATEWAYS.add(scanned.toImmutable());
                ConvergenceHazardHandler.registerSanctuary(scanned.toImmutable());
                save(targetWorld.getServer());
                return scanned;
            }

            return null;
        }
    }

    public static BlockPos findExactRiftPos(ServerWorld world, BlockPos pos) {
        if (world == null || pos == null) return null;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos check = pos.add(dx, dy, dz);
                    if (world.isChunkLoaded(check.getX() >> 4, check.getZ() >> 4)) {
                        if (world.getBlockState(check).isOf(ModBlocks.DORMANT_RIFT)) {
                            return check;
                        }
                    }
                }
            }
        }
        return null;
    }

    public static BlockPos scanForRiftInRadius(ServerWorld world, BlockPos center, int radiusBlocks) {
        int centerChunkX = center.getX() >> 4;
        int centerChunkZ = center.getZ() >> 4;
        int chunkRadius = Math.max(1, (radiusBlocks + 15) >> 4);

        BlockPos closest = null;
        double closestDistSq = Double.MAX_VALUE;

        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                int cx = centerChunkX + dx;
                int cz = centerChunkZ + dz;
                if (!world.isChunkLoaded(cx, cz)) {
                    continue;
                }
                WorldChunk chunk = world.getChunk(cx, cz);
                if (chunk == null) continue;

                ChunkSection[] sections = chunk.getSectionArray();
                for (int sIdx = 0; sIdx < sections.length; sIdx++) {
                    ChunkSection section = sections[sIdx];
                    if (section == null || section.isEmpty()) continue;
                    if (!section.hasAny(state -> state.isOf(ModBlocks.DORMANT_RIFT))) continue;

                    int sectionBaseY = world.sectionIndexToCoord(sIdx) << 4;
                    for (int lx = 0; lx < 16; lx++) {
                        for (int lz = 0; lz < 16; lz++) {
                            for (int ly = 0; ly < 16; ly++) {
                                if (section.getBlockState(lx, ly, lz).isOf(ModBlocks.DORMANT_RIFT)) {
                                    BlockPos found = new BlockPos((cx << 4) + lx, sectionBaseY + ly, (cz << 4) + lz);
                                    double distSq = found.getSquaredDistance(center);
                                    if (distSq < closestDistSq) {
                                        closestDistSq = distSq;
                                        closest = found;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return closest;
    }
}
