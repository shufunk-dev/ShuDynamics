package net.enchantedwood;

import net.fabricmc.api.ModInitializer;
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
        net.enchantedwood.effect.ModStatusEffects.registerModEffects();
        ModBlocks.registerModBlocks();
        ModItems.registerModItems();
        // Unclamp MAX_HEALTH attribute ceiling from 1024.0 -> 100,000.0
        try {
            if (net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH.value() instanceof net.minecraft.world.entity.ai.attributes.RangedAttribute clamped) {
                for (java.lang.reflect.Field f : net.minecraft.world.entity.ai.attributes.RangedAttribute.class.getDeclaredFields()) {
                    if (f.getType() == double.class) {
                        f.setAccessible(true);
                        double val = f.getDouble(clamped);
                        if (val >= 1000.0) {
                            f.setDouble(clamped, 100000.0);
                            LOGGER.info("Successfully unlocked MAX_HEALTH maxValue to 100,000.0 via reflection!");
                        }
                    }
                }
            }
        } catch (Throwable t) {
            LOGGER.warn("Failed to unclamp MAX_HEALTH via reflection", t);
        }

        net.enchantedwood.entity.ModEntities.registerModEntities();
        net.enchantedwood.sound.ModSounds.registerModSounds();
        ModBlockEntities.registerBlockEntities();
        ModScreenHandlers.registerScreenHandlers();
        net.enchantedwood.network.ModMessages.registerPackets();
        net.enchantedwood.world.dimension.ModDimensions.registerDimensions();
        net.enchantedwood.world.ModWorldGeneration.generateOres();
        net.enchantedwood.event.PlayerEquipmentState.register();
        net.enchantedwood.command.EquipmentCommand.register();
        net.enchantedwood.command.BossCommand.register();
        net.enchantedwood.event.PlayerFlightHandler.register();
        net.enchantedwood.event.PlayerHealthHandler.register();
        net.enchantedwood.event.ModularSuitHandler.register();
        net.enchantedwood.event.WoodenShearsSheepHandler.register();
        net.enchantedwood.event.WoodenShearsHarvestHandler.register();
        net.enchantedwood.event.CornSeedLootHandler.register();
        net.enchantedwood.event.ConvergenceFloraLootHandler.register();
        net.enchantedwood.event.OverworldAnomalyEventHandler.register();
        net.enchantedwood.event.ResonanceFrameHandler.register();
        net.enchantedwood.event.ConvergenceMobSpawnHandler.register();
        net.enchantedwood.event.ConvergenceHazardHandler.register();
        net.enchantedwood.item.custom.HyposprayItem.registerEntityInteraction();

        // Strippable Wood



// Composting Registrations (Corn seeds, grains, leaves, saplings)















// Culinary Expansion Composting




















}
}

