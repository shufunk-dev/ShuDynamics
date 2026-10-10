package net.enchantedwood.block.entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class CleanroomManager {
    // Maps World RegistryKey -> (Scrubber BlockPos -> Set of Interior Air Positions)
    private static final Map<ResourceKey<Level>, Map<BlockPos, Set<BlockPos>>> ACTIVE_CLEANROOMS = new HashMap<>();

    public static synchronized void registerZone(ResourceKey<Level> worldKey, BlockPos scrubberPos, Set<BlockPos> interiorPositions) {
        ACTIVE_CLEANROOMS.computeIfAbsent(worldKey, k -> new HashMap<>()).put(scrubberPos.immutable(), new HashSet<>(interiorPositions));
    }

    public static synchronized void registerCleanroom(Level world, BlockPos scrubberPos, Set<BlockPos> interiorPositions) {
        registerZone(world.dimension(), scrubberPos, interiorPositions);
    }

    public static synchronized void unregisterZone(ResourceKey<Level> worldKey, BlockPos scrubberPos) {
        Map<BlockPos, Set<BlockPos>> worldRooms = ACTIVE_CLEANROOMS.get(worldKey);
        if (worldRooms != null) {
            worldRooms.remove(scrubberPos);
            if (worldRooms.isEmpty()) {
                ACTIVE_CLEANROOMS.remove(worldKey);
            }
        }
    }

    public static synchronized void unregisterCleanroom(Level world, BlockPos scrubberPos) {
        unregisterZone(world.dimension(), scrubberPos);
    }

    public static synchronized boolean isInsideSterileCleanroom(Level world, BlockPos pos) {
        Map<BlockPos, Set<BlockPos>> worldRooms = ACTIVE_CLEANROOMS.get(world.dimension());
        if (worldRooms == null || worldRooms.isEmpty()) {
            return false;
        }

        for (Set<BlockPos> interior : worldRooms.values()) {
            if (interior.contains(pos)) {
                return true;
            }
        }
        return false;
    }
}
