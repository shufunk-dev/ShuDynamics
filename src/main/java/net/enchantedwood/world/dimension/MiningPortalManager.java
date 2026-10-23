package net.enchantedwood.world.dimension;

import net.enchantedwood.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.storage.LevelResource;
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
                CompoundTag root = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
                if (root != null) {
                    CompoundTag returnsNbt = root.getCompoundOrEmpty("player_returns");
                    for (String key : returnsNbt.keySet()) {
                        try {
                            UUID uuid = UUID.fromString(key);
                            long posLong = returnsNbt.getLongOr(key, 0L);
                            if (posLong != 0L) {
                                PLAYER_RETURN_POINTS.put(uuid, BlockPos.of(posLong));
                            }
                        } catch (Exception ignored) {
                        }
                    }

                    ListTag overworldList = root.getListOrEmpty("overworld_portals");
                    for (int i = 0; i < overworldList.size(); i++) {
                        Tag el = overworldList.get(i);
                        if (el instanceof LongTag nbtLong) {
                            OVERWORLD_PORTALS.add(BlockPos.of(nbtLong.longValue()));
                        }
                    }

                    ListTag miningList = root.getListOrEmpty("mining_portals");
                    for (int i = 0; i < miningList.size(); i++) {
                        Tag el = miningList.get(i);
                        if (el instanceof LongTag nbtLong) {
                            MINING_PORTALS.add(BlockPos.of(nbtLong.longValue()));
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
            CompoundTag root = new CompoundTag();

            CompoundTag returnsNbt = new CompoundTag();
            for (Map.Entry<UUID, BlockPos> entry : PLAYER_RETURN_POINTS.entrySet()) {
                returnsNbt.putLong(entry.getKey().toString(), entry.getValue().asLong());
            }
            root.put("player_returns", returnsNbt);

            ListTag overworldList = new ListTag();
            for (BlockPos pos : OVERWORLD_PORTALS) {
                overworldList.add(LongTag.valueOf(pos.asLong()));
            }
            root.put("overworld_portals", overworldList);

            ListTag miningList = new ListTag();
            for (BlockPos pos : MINING_PORTALS) {
                miningList.add(LongTag.valueOf(pos.asLong()));
            }
            root.put("mining_portals", miningList);

            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            NbtIo.writeCompressed(root, file);
        } catch (IOException e) {
            LOGGER.error("Failed to save mining portals to disk", e);
        }
    }

    private static Path getSaveFilePath(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("shudynamics_mining_portals.dat");
    }

    public static void setPlayerReturnPoint(MinecraftServer server, UUID playerUuid, BlockPos overworldPos) {
        ensureLoaded(server);
        PLAYER_RETURN_POINTS.put(playerUuid, overworldPos.immutable());
        OVERWORLD_PORTALS.add(overworldPos.immutable());
        save(server);
    }

    public static BlockPos getPlayerReturnPoint(MinecraftServer server, UUID playerUuid) {
        ensureLoaded(server);
        return PLAYER_RETURN_POINTS.get(playerUuid);
    }

    public static void registerPortal(ServerLevel world, BlockPos pos) {
        if (world == null || pos == null) return;
        ensureLoaded(world.getServer());
        BlockPos immutablePos = pos.immutable();
        if (world.dimension() == Level.OVERWORLD) {
            OVERWORLD_PORTALS.add(immutablePos);
        } else if (world.dimension() == ModDimensions.MINING_DIMENSION_WORLD_KEY) {
            MINING_PORTALS.add(immutablePos);
        }
        save(world.getServer());
    }

    public static void unregisterPortal(ServerLevel world, BlockPos pos) {
        if (world == null || pos == null) return;
        ensureLoaded(world.getServer());
        if (world.dimension() == Level.OVERWORLD) {
            OVERWORLD_PORTALS.removeIf(p -> p.distSqr(pos) <= 9);
            PLAYER_RETURN_POINTS.entrySet().removeIf(e -> e.getValue().distSqr(pos) <= 9);
        } else if (world.dimension() == ModDimensions.MINING_DIMENSION_WORLD_KEY) {
            MINING_PORTALS.removeIf(p -> p.distSqr(pos) <= 9);
        }
        save(world.getServer());
    }

    public static BlockPos findExistingPortal(ServerLevel targetWorld, BlockPos targetPos, UUID playerUuid, boolean returningToOverworld) {
        if (targetWorld == null) return null;
        ensureLoaded(targetWorld.getServer());

        if (returningToOverworld) {
            // 1. Try saved player return point
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

            // 2. Check registered Overworld portals sorted by distance
            BlockPos closestGw = OVERWORLD_PORTALS.stream()
                    .filter(gw -> gw.distSqr(targetPos) <= 256 * 256)
                    .min(Comparator.comparingDouble(gw -> gw.distSqr(targetPos)))
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

            // 3. Scan 128-block area in loaded/generated chunks
            BlockPos scanned = scanForPortalInRadius(targetWorld, targetPos, 128);
            if (scanned != null) {
                OVERWORLD_PORTALS.add(scanned.immutable());
                save(targetWorld.getServer());
                return scanned;
            }

            return null;
        } else {
            // Entering The Mining Dimension
            // 1. Check registered Mining portals
            BlockPos closestGw = MINING_PORTALS.stream()
                    .filter(gw -> gw.distSqr(targetPos) <= 256 * 256)
                    .min(Comparator.comparingDouble(gw -> gw.distSqr(targetPos)))
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

            // 2. Scan 128-block area in chunks
            BlockPos scanned = scanForPortalInRadius(targetWorld, targetPos, 128);
            if (scanned != null) {
                MINING_PORTALS.add(scanned.immutable());
                save(targetWorld.getServer());
                return scanned;
            }

            return null;
        }
    }

    public static BlockPos findExactPortalPos(ServerLevel world, BlockPos pos) {
        if (world == null || pos == null) return null;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos check = pos.offset(dx, dy, dz);
                    if (world.hasChunk(check.getX() >> 4, check.getZ() >> 4)) {
                        if (world.getBlockState(check).is(ModBlocks.MINING_PORTAL)) {
                            return check;
                        }
                    }
                }
            }
        }
        return null;
    }

    public static BlockPos scanForPortalInRadius(ServerLevel world, BlockPos center, int radiusBlocks) {
        int centerChunkX = center.getX() >> 4;
        int centerChunkZ = center.getZ() >> 4;
        int chunkRadius = Math.max(1, (radiusBlocks + 15) >> 4);

        BlockPos closest = null;
        double closestDistSq = Double.MAX_VALUE;

        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                int cx = centerChunkX + dx;
                int cz = centerChunkZ + dz;
                if (!world.hasChunk(cx, cz)) {
                    continue;
                }
                LevelChunk chunk = world.getChunk(cx, cz);
                if (chunk == null) continue;

                LevelChunkSection[] sections = chunk.getSections();
                for (int sIdx = 0; sIdx < sections.length; sIdx++) {
                    LevelChunkSection section = sections[sIdx];
                    if (section == null || section.hasOnlyAir()) continue;
                    if (!section.maybeHas(state -> state.is(ModBlocks.MINING_PORTAL))) continue;

                    int sectionBaseY = world.getSectionYFromSectionIndex(sIdx) << 4;
                    for (int lx = 0; lx < 16; lx++) {
                        for (int lz = 0; lz < 16; lz++) {
                            for (int ly = 0; ly < 16; ly++) {
                                if (section.getBlockState(lx, ly, lz).is(ModBlocks.MINING_PORTAL)) {
                                    BlockPos found = new BlockPos((cx << 4) + lx, sectionBaseY + ly, (cz << 4) + lz);
                                    double distSq = found.distSqr(center);
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
