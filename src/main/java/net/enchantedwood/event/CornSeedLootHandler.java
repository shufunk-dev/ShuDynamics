package net.enchantedwood.event;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.util.Identifier;
import net.enchantedwood.item.ModItems;

public class CornSeedLootHandler {
    private static final Identifier SHORT_GRASS_LOOT = Identifier.ofVanilla("blocks/short_grass");
    private static final Identifier TALL_GRASS_LOOT = Identifier.ofVanilla("blocks/tall_grass");
    private static final Identifier FERN_LOOT = Identifier.ofVanilla("blocks/fern");

    private static final Identifier VILLAGE_PLAINS = Identifier.ofVanilla("chests/village/village_plains_house");
    private static final Identifier VILLAGE_SAVANNA = Identifier.ofVanilla("chests/village/village_savanna_house");
    private static final Identifier VILLAGE_DESERT = Identifier.ofVanilla("chests/village/village_desert_house");

    public static void register() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            Identifier id = key.getValue();

            // 1. Breaking Wild Grass / Ferns (Drop chances for agricultural seeds and berries)
            if (id.equals(SHORT_GRASS_LOOT) || id.equals(TALL_GRASS_LOOT) || id.equals(FERN_LOOT)) {
                tableBuilder.pool(LootPool.builder()
                        .conditionally(RandomChanceLootCondition.builder(0.10f))
                        .with(ItemEntry.builder(ModItems.CORN_SEEDS)));
                tableBuilder.pool(LootPool.builder()
                        .conditionally(RandomChanceLootCondition.builder(0.08f))
                        .with(ItemEntry.builder(ModItems.TOMATO_SEEDS)));
                tableBuilder.pool(LootPool.builder()
                        .conditionally(RandomChanceLootCondition.builder(0.08f))
                        .with(ItemEntry.builder(ModItems.ONION_SEEDS)));
                tableBuilder.pool(LootPool.builder()
                        .conditionally(RandomChanceLootCondition.builder(0.08f))
                        .with(ItemEntry.builder(ModItems.LETTUCE_SEEDS)));
                tableBuilder.pool(LootPool.builder()
                        .conditionally(RandomChanceLootCondition.builder(0.08f))
                        .with(ItemEntry.builder(ModItems.SOYBEAN_SEEDS)));
                tableBuilder.pool(LootPool.builder()
                        .conditionally(RandomChanceLootCondition.builder(0.06f))
                        .with(ItemEntry.builder(ModItems.CHILI_PEPPER_SEEDS)));
                tableBuilder.pool(LootPool.builder()
                        .conditionally(RandomChanceLootCondition.builder(0.05f))
                        .with(ItemEntry.builder(ModItems.STRAWBERRY)));
                tableBuilder.pool(LootPool.builder()
                        .conditionally(RandomChanceLootCondition.builder(0.05f))
                        .with(ItemEntry.builder(ModItems.BLUEBERRY)));
            }

            // 2. Village House Chests (Plains, Savanna, Desert)
            if (id.equals(VILLAGE_PLAINS) || id.equals(VILLAGE_SAVANNA) || id.equals(VILLAGE_DESERT)) {
                tableBuilder.pool(LootPool.builder()
                        .conditionally(RandomChanceLootCondition.builder(0.35f))
                        .with(ItemEntry.builder(ModItems.CORN_SEEDS).apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(2.0f, 6.0f))))
                        .with(ItemEntry.builder(ModItems.TOMATO_SEEDS).apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(2.0f, 5.0f))))
                        .with(ItemEntry.builder(ModItems.ONION_SEEDS).apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(2.0f, 5.0f))))
                        .with(ItemEntry.builder(ModItems.LETTUCE_SEEDS).apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(2.0f, 5.0f))))
                        .with(ItemEntry.builder(ModItems.SOYBEAN_SEEDS).apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(2.0f, 5.0f))))
                        .with(ItemEntry.builder(ModItems.CHILI_PEPPER_SEEDS).apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1.0f, 4.0f))))
                        .with(ItemEntry.builder(ModItems.SALT).apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(2.0f, 6.0f)))));
            }
        });
    }

}
