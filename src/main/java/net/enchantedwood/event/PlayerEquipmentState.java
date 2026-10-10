package net.enchantedwood.event;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
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
            UUID oldUuid = oldPlayer.getUUID();
            UUID newUuid = newPlayer.getUUID();

            ItemStack cape = EQUIPPED_CAPES.getOrDefault(oldUuid, ItemStack.EMPTY);
            ItemStack heart = EQUIPPED_HEARTS.getOrDefault(oldUuid, ItemStack.EMPTY);

            if (!cape.isEmpty()) EQUIPPED_CAPES.put(newUuid, cape.copy());
            if (!heart.isEmpty()) EQUIPPED_HEARTS.put(newUuid, heart.copy());

            savePlayerData(newPlayer);
        });
    }

    public static void savePlayerData(ServerPlayer player) {
        try {
            if (!(player.level() instanceof ServerLevel serverWorld)) return;
            MinecraftServer server = serverWorld.getServer();
            if (server == null) return;

            File saveDir = new File(server.getWorldPath(LevelResource.ROOT).toFile(), "enchantedwood_data");
            if (!saveDir.exists()) saveDir.mkdirs();

            File playerFile = new File(saveDir, player.getStringUUID() + ".json");
            JsonObject json = new JsonObject();

            UUID uuid = player.getUUID();
            ItemStack cape = EQUIPPED_CAPES.getOrDefault(uuid, ItemStack.EMPTY);
            ItemStack heart = EQUIPPED_HEARTS.getOrDefault(uuid, ItemStack.EMPTY);

            if (!cape.isEmpty()) {
                json.addProperty("cape", BuiltInRegistries.ITEM.getKey(cape.getItem()).toString());
            }

            if (!heart.isEmpty()) {
                json.addProperty("heart", BuiltInRegistries.ITEM.getKey(heart.getItem()).toString());
            }

            try (FileWriter writer = new FileWriter(playerFile)) {
                writer.write(json.toString());
            }
        } catch (Throwable ignored) {}
    }

    public static void loadPlayerData(ServerPlayer player) {
        try {
            if (!(player.level() instanceof ServerLevel serverWorld)) return;
            MinecraftServer server = serverWorld.getServer();
            if (server == null) return;

            File saveDir = new File(server.getWorldPath(LevelResource.ROOT).toFile(), "enchantedwood_data");
            File playerFile = new File(saveDir, player.getStringUUID() + ".json");

            if (!playerFile.exists()) return;

            UUID uuid = player.getUUID();
            try (FileReader reader = new FileReader(playerFile)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

                if (json.has("cape")) {
                    Identifier id = Identifier.parse(json.get("cape").getAsString());
                    Item capeItem = BuiltInRegistries.ITEM.getValue(id);
                    if (capeItem != null) {
                        EQUIPPED_CAPES.put(uuid, new ItemStack(capeItem));
                    }
                } else {
                    EQUIPPED_CAPES.remove(uuid);
                }

                if (json.has("heart")) {
                    Identifier id = Identifier.parse(json.get("heart").getAsString());
                    Item heartItem = BuiltInRegistries.ITEM.getValue(id);
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

    public static ItemStack getEquippedCape(ServerPlayer player) {
        return EQUIPPED_CAPES.getOrDefault(player.getUUID(), ItemStack.EMPTY);
    }

    public static ItemStack equipCape(ServerPlayer player, ItemStack newCape) {
        UUID uuid = player.getUUID();
        ItemStack previousCape = EQUIPPED_CAPES.getOrDefault(uuid, ItemStack.EMPTY);
        EQUIPPED_CAPES.put(uuid, newCape.copy());
        savePlayerData(player);
        return previousCape;
    }

    public static ItemStack unequipCape(ServerPlayer player) {
        UUID uuid = player.getUUID();
        ItemStack removed = EQUIPPED_CAPES.remove(uuid);
        savePlayerData(player);
        return removed != null ? removed : ItemStack.EMPTY;
    }

    public static ItemStack getEquippedHeart(ServerPlayer player) {
        return EQUIPPED_HEARTS.getOrDefault(player.getUUID(), ItemStack.EMPTY);
    }

    public static ItemStack equipHeart(ServerPlayer player, ItemStack newHeart) {
        UUID uuid = player.getUUID();
        ItemStack previousHeart = EQUIPPED_HEARTS.getOrDefault(uuid, ItemStack.EMPTY);
        EQUIPPED_HEARTS.put(uuid, newHeart.copy());
        savePlayerData(player);
        PlayerHealthHandler.applyHeartAbsorptionImmediate(player);
        return previousHeart;
    }

    public static ItemStack unequipHeart(ServerPlayer player) {
        UUID uuid = player.getUUID();
        ItemStack removed = EQUIPPED_HEARTS.remove(uuid);
        savePlayerData(player);
        PlayerHealthHandler.applyHeartAbsorptionImmediate(player);
        return removed != null ? removed : ItemStack.EMPTY;
    }
}
