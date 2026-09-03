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

    public static final ScreenHandlerType<HydraulicPressScreenHandler> HYDRAULIC_PRESS_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(EnchantedWoodMod.MOD_ID, "hydraulic_press"),
                    new ScreenHandlerType<>(HydraulicPressScreenHandler::new, net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA_FEATURES));

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

    public static final ScreenHandlerType<VehicleFabricatorScreenHandler> VEHICLE_FABRICATOR_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(EnchantedWoodMod.MOD_ID, "vehicle_fabricator"),
                    new ScreenHandlerType<>(VehicleFabricatorScreenHandler::new, net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA_FEATURES));

    // Phase 2: Nether Factory & Tier 4 Grid
    public static final ScreenHandlerType<TungstenBatteryScreenHandler> TUNGSTEN_BATTERY_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(EnchantedWoodMod.MOD_ID, "tungsten_battery"),
                    new ScreenHandlerType<>(TungstenBatteryScreenHandler::new, net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA_FEATURES));

    public static final ScreenHandlerType<GeothermalGeneratorScreenHandler> GEOTHERMAL_GENERATOR_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(EnchantedWoodMod.MOD_ID, "geothermal_generator"),
                    new ScreenHandlerType<>(GeothermalGeneratorScreenHandler::new, net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA_FEATURES));

    public static final ScreenHandlerType<AlloyFoundryScreenHandler> ALLOY_FOUNDRY_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(EnchantedWoodMod.MOD_ID, "alloy_foundry"),
                    new ScreenHandlerType<>(AlloyFoundryScreenHandler::new, net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA_FEATURES));

    public static final ScreenHandlerType<ItemSalvagerScreenHandler> ITEM_SALVAGER_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(EnchantedWoodMod.MOD_ID, "item_salvager"),
                    new ScreenHandlerType<>(ItemSalvagerScreenHandler::new, net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA_FEATURES));

    public static final ScreenHandlerType<MagmaCrucibleScreenHandler> MAGMA_CRUCIBLE_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(EnchantedWoodMod.MOD_ID, "magma_crucible"),
                    new ScreenHandlerType<>(MagmaCrucibleScreenHandler::new, net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA_FEATURES));

    public static final ScreenHandlerType<LavaPumpScreenHandler> LAVA_PUMP_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(EnchantedWoodMod.MOD_ID, "lava_pump"),
                    new ScreenHandlerType<>(LavaPumpScreenHandler::new, net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA_FEATURES));

    public static final ScreenHandlerType<CrusherMk2ScreenHandler> CRUSHER_MK2_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(EnchantedWoodMod.MOD_ID, "crusher_mk2"),
                    new ScreenHandlerType<>(CrusherMk2ScreenHandler::new, net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA_FEATURES));

    public static final ScreenHandlerType<SoilInfuserScreenHandler> SOIL_INFUSER_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(EnchantedWoodMod.MOD_ID, "soil_infuser"),
                    new ScreenHandlerType<>(SoilInfuserScreenHandler::new, net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA_FEATURES));

    public static final ScreenHandlerType<TitaniumTankScreenHandler> TITANIUM_TANK_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(EnchantedWoodMod.MOD_ID, "titanium_tank"),
                    new ScreenHandlerType<>(TitaniumTankScreenHandler::new, net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA_FEATURES));

    public static final ScreenHandlerType<SuperComputerScreenHandler> SUPER_COMPUTER_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(EnchantedWoodMod.MOD_ID, "super_computer"),
                    new ScreenHandlerType<>(SuperComputerScreenHandler::new, net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA_FEATURES));

    public static final ScreenHandlerType<LaserQuarryScreenHandler> LASER_QUARRY_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(EnchantedWoodMod.MOD_ID, "laser_quarry"),
                    new ScreenHandlerType<>(LaserQuarryScreenHandler::new, net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA_FEATURES));

    public static void registerScreenHandlers() {
        EnchantedWoodMod.LOGGER.info("Registering Screen Handlers for " + EnchantedWoodMod.MOD_ID);
    }
}
