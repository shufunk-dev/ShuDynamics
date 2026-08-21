package net.enchantedwood.event;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerEquipmentState {
    private static final Map<UUID, ItemStack> EQUIPPED_CAPES = new HashMap<>();
    private static final Map<UUID, ItemStack> EQUIPPED_HEARTS = new HashMap<>();

    public static void register() {
        // Load data on player join
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            loadPlayerData(handler.getPlayer());
        });

        // Save data on player disconnect
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            savePlayerData(handler.getPlayer());
        });

        // Persist equipped items across death/respawn and dimension changes
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            UUID oldUuid = oldPlayer.getUuid();
            UUID newUuid = newPlayer.getUuid();

            ItemStack cape = EQUIPPED_CAPES.getOrDefault(oldUuid, ItemStack.EMPTY);
            ItemStack heart = EQUIPPED_HEARTS.getOrDefault(oldUuid, ItemStack.EMPTY);

            if (!cape.isEmpty()) EQUIPPED_CAPES.put(newUuid, cape.copy());
            if (!heart.isEmpty()) EQUIPPED_HEARTS.put(newUuid, heart.copy());

            savePlayerData(newPlayer);
        });
    }

    public static void savePlayerData(ServerPlayerEntity player) {
        try {
            if (!(player.getEntityWorld() instanceof ServerWorld serverWorld)) return;
            MinecraftServer server = serverWorld.getServer();
            if (server == null) return;

            File saveDir = new File(server.getSavePath(WorldSavePath.ROOT).toFile(), "enchantedwood_data");
            if (!saveDir.exists()) saveDir.mkdirs();

            File playerFile = new File(saveDir, player.getUuidAsString() + ".json");
            JsonObject json = new JsonObject();

            UUID uuid = player.getUuid();
            ItemStack cape = EQUIPPED_CAPES.getOrDefault(uuid, ItemStack.EMPTY);
            ItemStack heart = EQUIPPED_HEARTS.getOrDefault(uuid, ItemStack.EMPTY);

            if (!cape.isEmpty()) {
                json.addProperty("cape", Registries.ITEM.getId(cape.getItem()).toString());
            }

            if (!heart.isEmpty()) {
                json.addProperty("heart", Registries.ITEM.getId(heart.getItem()).toString());
            }

            try (FileWriter writer = new FileWriter(playerFile)) {
                writer.write(json.toString());
            }
        } catch (Throwable ignored) {}
    }

    public static void loadPlayerData(ServerPlayerEntity player) {
        try {
            if (!(player.getEntityWorld() instanceof ServerWorld serverWorld)) return;
            MinecraftServer server = serverWorld.getServer();
            if (server == null) return;

            File saveDir = new File(server.getSavePath(WorldSavePath.ROOT).toFile(), "enchantedwood_data");
            File playerFile = new File(saveDir, player.getUuidAsString() + ".json");

            if (!playerFile.exists()) return;

            UUID uuid = player.getUuid();
            try (FileReader reader = new FileReader(playerFile)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

                if (json.has("cape")) {
                    Identifier id = Identifier.of(json.get("cape").getAsString());
                    Item capeItem = Registries.ITEM.get(id);
                    if (capeItem != null) {
                        EQUIPPED_CAPES.put(uuid, new ItemStack(capeItem));
                    }
                } else {
                    EQUIPPED_CAPES.remove(uuid);
                }

                if (json.has("heart")) {
                    Identifier id = Identifier.of(json.get("heart").getAsString());
                    Item heartItem = Registries.ITEM.get(id);
                    if (heartItem != null) {
                        EQUIPPED_HEARTS.put(uuid, new ItemStack(heartItem));
                    }
                } else {
                    EQUIPPED_HEARTS.remove(uuid);
                }
                PlayerHealthHandler.applyHeartAbsorptionImmediate(player);
            }
        } catch (Throwable ignored) {}
    }

    public static ItemStack getEquippedCape(ServerPlayerEntity player) {
        return EQUIPPED_CAPES.getOrDefault(player.getUuid(), ItemStack.EMPTY);
    }

    public static ItemStack equipCape(ServerPlayerEntity player, ItemStack newCape) {
        UUID uuid = player.getUuid();
        ItemStack previousCape = EQUIPPED_CAPES.getOrDefault(uuid, ItemStack.EMPTY);
        EQUIPPED_CAPES.put(uuid, newCape.copy());
        savePlayerData(player);
        return previousCape;
    }

    public static ItemStack unequipCape(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        ItemStack removed = EQUIPPED_CAPES.remove(uuid);
        savePlayerData(player);
        return removed != null ? removed : ItemStack.EMPTY;
    }

    public static ItemStack getEquippedHeart(ServerPlayerEntity player) {
        return EQUIPPED_HEARTS.getOrDefault(player.getUuid(), ItemStack.EMPTY);
    }

    public static ItemStack equipHeart(ServerPlayerEntity player, ItemStack newHeart) {
        UUID uuid = player.getUuid();
        ItemStack previousHeart = EQUIPPED_HEARTS.getOrDefault(uuid, ItemStack.EMPTY);
        EQUIPPED_HEARTS.put(uuid, newHeart.copy());
        savePlayerData(player);
        PlayerHealthHandler.applyHeartAbsorptionImmediate(player);
        return previousHeart;
    }

    public static ItemStack unequipHeart(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        ItemStack removed = EQUIPPED_HEARTS.remove(uuid);
        savePlayerData(player);
        PlayerHealthHandler.applyHeartAbsorptionImmediate(player);
        return removed != null ? removed : ItemStack.EMPTY;
    }
}
