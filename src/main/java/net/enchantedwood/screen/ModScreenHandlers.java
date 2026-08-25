package net.enchantedwood.screen;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.enchantedwood.EnchantedWoodMod;

public class ModScreenHandlers {
    public static final ScreenHandlerType<CrusherScreenHandler> CRUSHER_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(EnchantedWoodMod.MOD_ID, "crusher"),
                    new ScreenHandlerType<>(CrusherScreenHandler::new, net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA_FEATURES));

    public static final ScreenHandlerType<DustSmelterScreenHandler> DUST_SMELTER_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(EnchantedWoodMod.MOD_ID, "dust_smelter"),
                    new ScreenHandlerType<>(DustSmelterScreenHandler::new, net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA_FEATURES));

    public static final ScreenHandlerType<EnchantedLavaGeneratorScreenHandler> ENCHANTED_LAVA_GENERATOR_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(EnchantedWoodMod.MOD_ID, "enchanted_lava_generator"),
                    new ScreenHandlerType<>(EnchantedLavaGeneratorScreenHandler::new, net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA_FEATURES));

    public static final ScreenHandlerType<EnchantedChestScreenHandler> ENCHANTED_CHEST_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(EnchantedWoodMod.MOD_ID, "enchanted_chest"),
                    new ScreenHandlerType<>(EnchantedChestScreenHandler::new, net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA_FEATURES));

    public static final ScreenHandlerType<EnchantedStorageControllerScreenHandler> ENCHANTED_STORAGE_CONTROLLER_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(EnchantedWoodMod.MOD_ID, "enchanted_storage_controller"),
                    new ScreenHandlerType<>(EnchantedStorageControllerScreenHandler::new, net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA_FEATURES));

    public static final ScreenHandlerType<EnchantedDriveBayScreenHandler> ENCHANTED_DRIVE_BAY_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(EnchantedWoodMod.MOD_ID, "enchanted_drive_bay"),
                    new ScreenHandlerType<>(EnchantedDriveBayScreenHandler::new, net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA_FEATURES));

    public static final ScreenHandlerType<EnchantedStorageTerminalScreenHandler> ENCHANTED_STORAGE_TERMINAL_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(EnchantedWoodMod.MOD_ID, "enchanted_storage_terminal"),
                    new ScreenHandlerType<>(EnchantedStorageTerminalScreenHandler::new, net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA_FEATURES));

    public static final ScreenHandlerType<EquipmentScreenHandler> EQUIPMENT_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(EnchantedWoodMod.MOD_ID, "equipment"),
                    new ScreenHandlerType<>(EquipmentScreenHandler::new, net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA_FEATURES));

    public static final ScreenHandlerType<CopperGeneratorScreenHandler> COPPER_GENERATOR_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(EnchantedWoodMod.MOD_ID, "copper_generator"),
                    new ScreenHandlerType<>(CopperGeneratorScreenHandler::new, net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA_FEATURES));

    public static final ScreenHandlerType<CopperBatteryScreenHandler> COPPER_BATTERY_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(EnchantedWoodMod.MOD_ID, "copper_battery"),
                    new ScreenHandlerType<>(CopperBatteryScreenHandler::new, net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA_FEATURES));

    public static final ScreenHandlerType<OxygenGeneratorScreenHandler> OXYGEN_GENERATOR_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(EnchantedWoodMod.MOD_ID, "oxygen_generator"),
                    new ScreenHandlerType<>(OxygenGeneratorScreenHandler::new, net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA_FEATURES));

    public static final ScreenHandlerType<AluminumRefinerScreenHandler> ALUMINUM_REFINER_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(EnchantedWoodMod.MOD_ID, "aluminum_refiner"),
                    new ScreenHandlerType<>(AluminumRefinerScreenHandler::new, net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA_FEATURES));

    public static final ScreenHandlerType<AluminumGeneratorScreenHandler> ALUMINUM_GENERATOR_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(EnchantedWoodMod.MOD_ID, "aluminum_generator"),
                    new ScreenHandlerType<>(AluminumGeneratorScreenHandler::new, net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA_FEATURES));

    public static final ScreenHandlerType<AluminumBatteryScreenHandler> ALUMINUM_BATTERY_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(EnchantedWoodMod.MOD_ID, "aluminum_battery"),
                    new ScreenHandlerType<>(AluminumBatteryScreenHandler::new, net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA_FEATURES));

    // Phase 3: Coke Coal & Steel Metallurgy
    public static final ScreenHandlerType<CokeOvenScreenHandler> COKE_OVEN_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(EnchantedWoodMod.MOD_ID, "coke_oven"),
                    new ScreenHandlerType<>(CokeOvenScreenHandler::new, net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA_FEATURES));

    public static final ScreenHandlerType<SteelBlastFurnaceScreenHandler> STEEL_BLAST_FURNACE_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(EnchantedWoodMod.MOD_ID, "steel_blast_furnace"),
                    new ScreenHandlerType<>(SteelBlastFurnaceScreenHandler::new, net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA_FEATURES));

    public static final ScreenHandlerType<SteelGeneratorScreenHandler> STEEL_GENERATOR_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(EnchantedWoodMod.MOD_ID, "steel_generator"),
                    new ScreenHandlerType<>(SteelGeneratorScreenHandler::new, net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA_FEATURES));

    public static final ScreenHandlerType<SteelBatteryScreenHandler> STEEL_BATTERY_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(EnchantedWoodMod.MOD_ID, "steel_battery"),
                    new ScreenHandlerType<>(SteelBatteryScreenHandler::new, net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA_FEATURES));

    public static final ScreenHandlerType<FuelRefineryScreenHandler> FUEL_REFINERY_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(EnchantedWoodMod.MOD_ID, "fuel_refinery"),
                    new ScreenHandlerType<>(FuelRefineryScreenHandler::new, net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA_FEATURES));

    public static final ScreenHandlerType<RoadPaverScreenHandler> ROAD_PAVER_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(EnchantedWoodMod.MOD_ID, "road_paver"),
                    new ScreenHandlerType<>(RoadPaverScreenHandler::new, net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA_FEATURES));

    public static final ScreenHandlerType<AtvScreenHandler> ATV_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(EnchantedWoodMod.MOD_ID, "atv"),
                    new ScreenHandlerType<>(AtvScreenHandler::new, net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA_FEATURES));

    public static void registerScreenHandlers() {
        EnchantedWoodMod.LOGGER.info("Registering Screen Handlers for " + EnchantedWoodMod.MOD_ID);
    }
}
