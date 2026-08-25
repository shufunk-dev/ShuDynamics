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

            // 1. Breaking Wild Grass / Ferns (12% chance to drop Corn Kernels)
            if (id.equals(SHORT_GRASS_LOOT) || id.equals(TALL_GRASS_LOOT) || id.equals(FERN_LOOT)) {
                LootPool.Builder pool = LootPool.builder()
                        .conditionally(RandomChanceLootCondition.builder(0.12f))
                        .with(ItemEntry.builder(ModItems.CORN_SEEDS))
                        .apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(1.0f)));
                tableBuilder.pool(pool);
            }

            // 2. Village House Chests (Plains, Savanna, Desert)
            if (id.equals(VILLAGE_PLAINS) || id.equals(VILLAGE_SAVANNA) || id.equals(VILLAGE_DESERT)) {
                LootPool.Builder pool = LootPool.builder()
                        .conditionally(RandomChanceLootCondition.builder(0.40f))
                        .with(ItemEntry.builder(ModItems.CORN_SEEDS))
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(2.0f, 6.0f)));
                tableBuilder.pool(pool);
            }
        });
    }
}
