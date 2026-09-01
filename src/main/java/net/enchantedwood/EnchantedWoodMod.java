package net.enchantedwood;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.registry.FuelRegistryEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.block.entity.ModBlockEntities;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.screen.ModScreenHandlers;

public class EnchantedWoodMod implements ModInitializer {
    public static final String MOD_ID = "enchantedwood";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Enchanted Wood Mod!");
        ModBlocks.registerModBlocks();
        ModItems.registerModItems();
        net.enchantedwood.entity.ModEntities.registerModEntities();
        net.enchantedwood.sound.ModSounds.registerModSounds();
        ModBlockEntities.registerBlockEntities();
        ModScreenHandlers.registerScreenHandlers();
        net.enchantedwood.network.ModMessages.registerC2SPackets();
        net.enchantedwood.world.dimension.ModDimensions.registerDimensions();
        net.enchantedwood.world.ModWorldGeneration.generateOres();
        net.enchantedwood.event.PlayerEquipmentState.register();
        net.enchantedwood.command.EquipmentCommand.register();
        net.enchantedwood.event.PlayerFlightHandler.register();
        net.enchantedwood.event.PlayerHealthHandler.register();
        net.enchantedwood.event.WoodenShearsSheepHandler.register();
        net.enchantedwood.event.WoodenShearsHarvestHandler.register();
        net.enchantedwood.event.CornSeedLootHandler.register();

        // Strippable Rubber Wood
        net.fabricmc.fabric.api.registry.StrippableBlockRegistry.register(ModBlocks.RUBBER_LOG, ModBlocks.STRIPPED_RUBBER_LOG);
        net.fabricmc.fabric.api.registry.StrippableBlockRegistry.register(ModBlocks.RUBBER_WOOD, ModBlocks.STRIPPED_RUBBER_WOOD);

        // 5x-6x Longer Burn Time Fuels + Copper Lava Bucket (1000s = 20000 ticks)
        FuelRegistryEvents.BUILD.register((builder, context) -> {
            builder.add(ModItems.ENCHANTED_DUST.asItem(), 8000);
            builder.add(ModItems.ENCHANTED_COAL.asItem(), 10000);
            builder.add(ModBlocks.ENCHANTED_COAL_BLOCK.asItem(), 90000);
            builder.add(ModItems.COKE_COAL.asItem(), 3200);
            builder.add(ModBlocks.COKE_COAL_BLOCK.asItem(), 28800);
            builder.add(ModItems.COPPER_LAVA_BUCKET.asItem(), 20000);
            builder.add(ModItems.ENCHANTED_LAVA_BUCKET.asItem(), 60000);
            builder.add(ModItems.ENCHANTED_COPPER_LAVA_BUCKET.asItem(), 60000);
        });
    }
}
