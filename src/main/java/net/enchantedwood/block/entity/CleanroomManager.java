package net.enchantedwood.block.entity;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CleanroomManager {
    // Maps World RegistryKey -> (Scrubber BlockPos -> Set of Interior Air Positions)
    private static final Map<RegistryKey<World>, Map<BlockPos, Set<BlockPos>>> ACTIVE_CLEANROOMS = new HashMap<>();

    public static synchronized void registerZone(RegistryKey<World> worldKey, BlockPos scrubberPos, Set<BlockPos> interiorPositions) {
        ACTIVE_CLEANROOMS.computeIfAbsent(worldKey, k -> new HashMap<>()).put(scrubberPos.toImmutable(), new HashSet<>(interiorPositions));
    }

    public static synchronized void registerCleanroom(World world, BlockPos scrubberPos, Set<BlockPos> interiorPositions) {
        registerZone(world.getRegistryKey(), scrubberPos, interiorPositions);
    }

    public static synchronized void unregisterZone(RegistryKey<World> worldKey, BlockPos scrubberPos) {
        Map<BlockPos, Set<BlockPos>> worldRooms = ACTIVE_CLEANROOMS.get(worldKey);
        if (worldRooms != null) {
            worldRooms.remove(scrubberPos);
            if (worldRooms.isEmpty()) {
                ACTIVE_CLEANROOMS.remove(worldKey);
            }
        }
    }

    public static synchronized void unregisterCleanroom(World world, BlockPos scrubberPos) {
        unregisterZone(world.getRegistryKey(), scrubberPos);
    }

    public static synchronized boolean isInsideSterileCleanroom(World world, BlockPos pos) {
        Map<BlockPos, Set<BlockPos>> worldRooms = ACTIVE_CLEANROOMS.get(world.getRegistryKey());
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
