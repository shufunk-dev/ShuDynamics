package net.enchantedwood.event;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import net.enchantedwood.item.ModItems;

public class CornSeedLootHandler {
    private static final Identifier SHORT_GRASS_LOOT = Identifier.withDefaultNamespace("blocks/short_grass");
    private static final Identifier TALL_GRASS_LOOT = Identifier.withDefaultNamespace("blocks/tall_grass");
    private static final Identifier FERN_LOOT = Identifier.withDefaultNamespace("blocks/fern");

    private static final Identifier VILLAGE_PLAINS = Identifier.withDefaultNamespace("chests/village/village_plains_house");
    private static final Identifier VILLAGE_SAVANNA = Identifier.withDefaultNamespace("chests/village/village_savanna_house");
    private static final Identifier VILLAGE_DESERT = Identifier.withDefaultNamespace("chests/village/village_desert_house");

    public static void register() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            Identifier id = key.identifier();

            // 1. Breaking Wild Grass / Ferns (Drop chances for agricultural seeds and berries)
            if (id.equals(SHORT_GRASS_LOOT) || id.equals(TALL_GRASS_LOOT) || id.equals(FERN_LOOT)) {
                tableBuilder.withPool(LootPool.lootPool()
                        .when(LootItemRandomChanceCondition.randomChance(0.10f))
                        .add(LootItem.lootTableItem(ModItems.CORN_SEEDS)));
                tableBuilder.withPool(LootPool.lootPool()
                        .when(LootItemRandomChanceCondition.randomChance(0.08f))
                        .add(LootItem.lootTableItem(ModItems.TOMATO_SEEDS)));
                tableBuilder.withPool(LootPool.lootPool()
                        .when(LootItemRandomChanceCondition.randomChance(0.08f))
                        .add(LootItem.lootTableItem(ModItems.ONION_SEEDS)));
                tableBuilder.withPool(LootPool.lootPool()
                        .when(LootItemRandomChanceCondition.randomChance(0.08f))
                        .add(LootItem.lootTableItem(ModItems.LETTUCE_SEEDS)));
                tableBuilder.withPool(LootPool.lootPool()
                        .when(LootItemRandomChanceCondition.randomChance(0.08f))
                        .add(LootItem.lootTableItem(ModItems.SOYBEAN_SEEDS)));
                tableBuilder.withPool(LootPool.lootPool()
                        .when(LootItemRandomChanceCondition.randomChance(0.06f))
                        .add(LootItem.lootTableItem(ModItems.CHILI_PEPPER_SEEDS)));
                tableBuilder.withPool(LootPool.lootPool()
                        .when(LootItemRandomChanceCondition.randomChance(0.05f))
                        .add(LootItem.lootTableItem(ModItems.STRAWBERRY)));
                tableBuilder.withPool(LootPool.lootPool()
                        .when(LootItemRandomChanceCondition.randomChance(0.05f))
                        .add(LootItem.lootTableItem(ModItems.BLUEBERRY)));
            }

            // 2. Village House Chests (Plains, Savanna, Desert)
            if (id.equals(VILLAGE_PLAINS) || id.equals(VILLAGE_SAVANNA) || id.equals(VILLAGE_DESERT)) {
                tableBuilder.withPool(LootPool.lootPool()
                        .when(LootItemRandomChanceCondition.randomChance(0.35f))
                        .add(LootItem.lootTableItem(ModItems.CORN_SEEDS).apply(SetItemCountFunction.setCount(ContextIntProviders.between(2, 6))))
                        .add(LootItem.lootTableItem(ModItems.TOMATO_SEEDS).apply(SetItemCountFunction.setCount(ContextIntProviders.between(2, 5))))
                        .add(LootItem.lootTableItem(ModItems.ONION_SEEDS).apply(SetItemCountFunction.setCount(ContextIntProviders.between(2, 5))))
                        .add(LootItem.lootTableItem(ModItems.LETTUCE_SEEDS).apply(SetItemCountFunction.setCount(ContextIntProviders.between(2, 5))))
                        .add(LootItem.lootTableItem(ModItems.SOYBEAN_SEEDS).apply(SetItemCountFunction.setCount(ContextIntProviders.between(2, 5))))
                        .add(LootItem.lootTableItem(ModItems.CHILI_PEPPER_SEEDS).apply(SetItemCountFunction.setCount(ContextIntProviders.between(1, 4))))
                        .add(LootItem.lootTableItem(ModItems.SALT).apply(SetItemCountFunction.setCount(ContextIntProviders.between(2, 6)))));
            }
        });
    }

}
