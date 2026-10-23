package net.enchantedwood.world.dimension;

import net.enchantedwood.block.ModBlocks;
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

public class MiningPortalManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("MiningPortalManager");

    private static final Map<UUID, BlockPos> PLAYER_RETURN_POINTS = new ConcurrentHashMap<>();
    private static final Set<BlockPos> OVERWORLD_PORTALS = ConcurrentHashMap.newKeySet();
    private static final Set<BlockPos> MINING_PORTALS = ConcurrentHashMap.newKeySet();

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

                    NbtList overworldList = root.getListOrEmpty("overworld_portals");
                    for (int i = 0; i < overworldList.size(); i++) {
                        NbtElement el = overworldList.get(i);
                        if (el instanceof NbtLong nbtLong) {
                            OVERWORLD_PORTALS.add(BlockPos.fromLong(nbtLong.longValue()));
                        }
                    }

                    NbtList miningList = root.getListOrEmpty("mining_portals");
                    for (int i = 0; i < miningList.size(); i++) {
                        NbtElement el = miningList.get(i);
                        if (el instanceof NbtLong nbtLong) {
                            MINING_PORTALS.add(BlockPos.fromLong(nbtLong.longValue()));
                        }
                    }
                    LOGGER.info("Loaded {} player returns, {} Overworld mining portals, {} Mining dimension portals.",
                            PLAYER_RETURN_POINTS.size(), OVERWORLD_PORTALS.size(), MINING_PORTALS.size());
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load saved mining portals from disk", e);
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
            for (BlockPos pos : OVERWORLD_PORTALS) {
                overworldList.add(NbtLong.of(pos.asLong()));
            }
            root.put("overworld_portals", overworldList);

            NbtList miningList = new NbtList();
            for (BlockPos pos : MINING_PORTALS) {
                miningList.add(NbtLong.of(pos.asLong()));
            }
            root.put("mining_portals", miningList);

            Files.createDirectories(file.getParent());
            NbtIo.writeCompressed(root, file);
        } catch (IOException e) {
            LOGGER.error("Failed to save mining portals to disk", e);
        }
    }

    private static Path getSaveFilePath(MinecraftServer server) {
        return server.getSavePath(WorldSavePath.ROOT).resolve("data").resolve("mining_portals.dat");
    }

    public static void setPlayerReturnPoint(MinecraftServer server, UUID playerUuid, BlockPos overworldPos) {
        ensureLoaded(server);
        PLAYER_RETURN_POINTS.put(playerUuid, overworldPos.toImmutable());
        OVERWORLD_PORTALS.add(overworldPos.toImmutable());
        save(server);
    }

    public static BlockPos getPlayerReturnPoint(MinecraftServer server, UUID playerUuid) {
        ensureLoaded(server);
        return PLAYER_RETURN_POINTS.get(playerUuid);
    }

    public static void registerPortal(ServerWorld world, BlockPos pos) {
        if (world == null || pos == null) return;
        ensureLoaded(world.getServer());
        BlockPos immutablePos = pos.toImmutable();
        if (world.getRegistryKey() == World.OVERWORLD) {
            OVERWORLD_PORTALS.add(immutablePos);
        } else if (world.getRegistryKey() == ModDimensions.MINING_DIMENSION_WORLD_KEY) {
            MINING_PORTALS.add(immutablePos);
        }
        save(world.getServer());
    }

    public static void unregisterPortal(ServerWorld world, BlockPos pos) {
        if (world == null || pos == null) return;
        ensureLoaded(world.getServer());
        if (world.getRegistryKey() == World.OVERWORLD) {
            OVERWORLD_PORTALS.removeIf(p -> p.getSquaredDistance(pos) <= 9);
            PLAYER_RETURN_POINTS.entrySet().removeIf(e -> e.getValue().getSquaredDistance(pos) <= 9);
        } else if (world.getRegistryKey() == ModDimensions.MINING_DIMENSION_WORLD_KEY) {
            MINING_PORTALS.removeIf(p -> p.getSquaredDistance(pos) <= 9);
        }
        save(world.getServer());
    }

    public static BlockPos findExistingPortal(ServerWorld targetWorld, BlockPos targetPos, UUID playerUuid, boolean returningToOverworld) {
        if (targetWorld == null) return null;
        ensureLoaded(targetWorld.getServer());

        if (returningToOverworld) {
            if (playerUuid != null) {
                BlockPos saved = PLAYER_RETURN_POINTS.get(playerUuid);
                if (saved != null) {
                    targetWorld.getChunk(saved.getX() >> 4, saved.getZ() >> 4, ChunkStatus.FULL, true);
                    BlockPos exact = findExactPortalPos(targetWorld, saved);
                    if (exact != null) {
                        return exact;
                    }
                }
            }

            BlockPos closestGw = OVERWORLD_PORTALS.stream()
                    .filter(gw -> gw.getSquaredDistance(targetPos) <= 256 * 256)
                    .min(Comparator.comparingDouble(gw -> gw.getSquaredDistance(targetPos)))
                    .orElse(null);

            if (closestGw != null) {
                targetWorld.getChunk(closestGw.getX() >> 4, closestGw.getZ() >> 4, ChunkStatus.FULL, true);
                BlockPos exact = findExactPortalPos(targetWorld, closestGw);
                if (exact != null) {
                    return exact;
                } else {
                    OVERWORLD_PORTALS.remove(closestGw);
                    save(targetWorld.getServer());
                }
            }

            BlockPos scanned = scanForPortalInRadius(targetWorld, targetPos, 128);
            if (scanned != null) {
                OVERWORLD_PORTALS.add(scanned.toImmutable());
                save(targetWorld.getServer());
                return scanned;
            }

            return null;
        } else {
            BlockPos closestGw = MINING_PORTALS.stream()
                    .filter(gw -> gw.getSquaredDistance(targetPos) <= 256 * 256)
                    .min(Comparator.comparingDouble(gw -> gw.getSquaredDistance(targetPos)))
                    .orElse(null);

            if (closestGw != null) {
                targetWorld.getChunk(closestGw.getX() >> 4, closestGw.getZ() >> 4, ChunkStatus.FULL, true);
                BlockPos exact = findExactPortalPos(targetWorld, closestGw);
                if (exact != null) {
                    return exact;
                } else {
                    MINING_PORTALS.remove(closestGw);
                    save(targetWorld.getServer());
                }
            }

            BlockPos scanned = scanForPortalInRadius(targetWorld, targetPos, 128);
            if (scanned != null) {
                MINING_PORTALS.add(scanned.toImmutable());
                save(targetWorld.getServer());
                return scanned;
            }

            return null;
        }
    }

    public static BlockPos findExactPortalPos(ServerWorld world, BlockPos pos) {
        if (world == null || pos == null) return null;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos check = pos.add(dx, dy, dz);
                    if (world.isChunkLoaded(check.getX() >> 4, check.getZ() >> 4)) {
                        if (world.getBlockState(check).isOf(ModBlocks.MINING_PORTAL)) {
                            return check;
                        }
                    }
                }
            }
        }
        return null;
    }

    public static BlockPos scanForPortalInRadius(ServerWorld world, BlockPos center, int radiusBlocks) {
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
                    if (!section.hasAny(state -> state.isOf(ModBlocks.MINING_PORTAL))) continue;

                    int sectionBaseY = world.sectionIndexToCoord(sIdx) << 4;
                    for (int lx = 0; lx < 16; lx++) {
                        for (int lz = 0; lz < 16; lz++) {
                            for (int ly = 0; ly < 16; ly++) {
                                if (section.getBlockState(lx, ly, lz).isOf(ModBlocks.MINING_PORTAL)) {
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
