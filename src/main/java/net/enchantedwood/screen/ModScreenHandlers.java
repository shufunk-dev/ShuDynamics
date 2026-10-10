package net.enchantedwood.screen;

import net.enchantedwood.EnchantedWoodMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;

public class ModScreenHandlers {
    public static final MenuType<CrusherScreenHandler> CRUSHER_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "crusher"),
                    new MenuType<>(CrusherScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<DustSmelterScreenHandler> DUST_SMELTER_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "dust_smelter"),
                    new MenuType<>(DustSmelterScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<HydraulicPressScreenHandler> HYDRAULIC_PRESS_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "hydraulic_press"),
                    new MenuType<>(HydraulicPressScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<HarmonicRecordPressScreenHandler> HARMONIC_RECORD_PRESS_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "harmonic_record_press"),
                    new MenuType<>(HarmonicRecordPressScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));


    public static final MenuType<EnchantedLavaGeneratorScreenHandler> ENCHANTED_LAVA_GENERATOR_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "enchanted_lava_generator"),
                    new MenuType<>(EnchantedLavaGeneratorScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<EnchantedChestScreenHandler> ENCHANTED_CHEST_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "enchanted_chest"),
                    new MenuType<>(EnchantedChestScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<EnchantedStorageControllerScreenHandler> ENCHANTED_STORAGE_CONTROLLER_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "enchanted_storage_controller"),
                    new MenuType<>(EnchantedStorageControllerScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<EnchantedDriveBayScreenHandler> ENCHANTED_DRIVE_BAY_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "enchanted_drive_bay"),
                    new MenuType<>(EnchantedDriveBayScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<EnchantedStorageTerminalScreenHandler> ENCHANTED_STORAGE_TERMINAL_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "enchanted_storage_terminal"),
                    new MenuType<>(EnchantedStorageTerminalScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<EquipmentScreenHandler> EQUIPMENT_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "equipment"),
                    new MenuType<>(EquipmentScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<CopperGeneratorScreenHandler> COPPER_GENERATOR_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "copper_generator"),
                    new MenuType<>(CopperGeneratorScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<CopperBatteryScreenHandler> COPPER_BATTERY_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "copper_battery"),
                    new MenuType<>(CopperBatteryScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<OxygenGeneratorScreenHandler> OXYGEN_GENERATOR_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "oxygen_generator"),
                    new MenuType<>(OxygenGeneratorScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<AluminumRefinerScreenHandler> ALUMINUM_REFINER_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "aluminum_refiner"),
                    new MenuType<>(AluminumRefinerScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<AluminumGeneratorScreenHandler> ALUMINUM_GENERATOR_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "aluminum_generator"),
                    new MenuType<>(AluminumGeneratorScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<AluminumBatteryScreenHandler> ALUMINUM_BATTERY_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "aluminum_battery"),
                    new MenuType<>(AluminumBatteryScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    // Phase 3: Coke Coal & Steel Metallurgy
    public static final MenuType<CokeOvenScreenHandler> COKE_OVEN_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "coke_oven"),
                    new MenuType<>(CokeOvenScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<SteelBlastFurnaceScreenHandler> STEEL_BLAST_FURNACE_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "steel_blast_furnace"),
                    new MenuType<>(SteelBlastFurnaceScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<SteelGeneratorScreenHandler> STEEL_GENERATOR_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "steel_generator"),
                    new MenuType<>(SteelGeneratorScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<SteelBatteryScreenHandler> STEEL_BATTERY_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "steel_battery"),
                    new MenuType<>(SteelBatteryScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<FuelRefineryScreenHandler> FUEL_REFINERY_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "fuel_refinery"),
                    new MenuType<>(FuelRefineryScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<RoadPaverScreenHandler> ROAD_PAVER_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "road_paver"),
                    new MenuType<>(RoadPaverScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<RoadPaverMk2ScreenHandler> ROAD_PAVER_MK2_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "road_paver_mk2"),
                    new MenuType<>(RoadPaverMk2ScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<AtvScreenHandler> ATV_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "atv"),
                    new MenuType<>(AtvScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<VehicleFabricatorScreenHandler> VEHICLE_FABRICATOR_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "vehicle_fabricator"),
                    new MenuType<>(VehicleFabricatorScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    // Phase 2: Nether Factory & Tier 4 Grid
    public static final MenuType<TungstenBatteryScreenHandler> TUNGSTEN_BATTERY_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "tungsten_battery"),
                    new MenuType<>(TungstenBatteryScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<GeothermalGeneratorScreenHandler> GEOTHERMAL_GENERATOR_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "geothermal_generator"),
                    new MenuType<>(GeothermalGeneratorScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<AlloyFoundryScreenHandler> ALLOY_FOUNDRY_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "alloy_foundry"),
                    new MenuType<>(AlloyFoundryScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<ItemSalvagerScreenHandler> ITEM_SALVAGER_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "item_salvager"),
                    new MenuType<>(ItemSalvagerScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<MagmaCrucibleScreenHandler> MAGMA_CRUCIBLE_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "magma_crucible"),
                    new MenuType<>(MagmaCrucibleScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<LavaPumpScreenHandler> LAVA_PUMP_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "lava_pump"),
                    new MenuType<>(LavaPumpScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<WaterPumpScreenHandler> WATER_PUMP_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "water_pump"),
                    new MenuType<>(WaterPumpScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<CryoFreezerScreenHandler> CRYO_FREEZER_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "cryo_freezer"),
                    new MenuType<>(CryoFreezerScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<CrusherMk2ScreenHandler> CRUSHER_MK2_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "crusher_mk2"),
                    new MenuType<>(CrusherMk2ScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<DustSmelterMk2ScreenHandler> DUST_SMELTER_MK2_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "dust_smelter_mk2"),
                    new MenuType<>(DustSmelterMk2ScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<SoilInfuserScreenHandler> SOIL_INFUSER_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "soil_infuser"),
                    new MenuType<>(SoilInfuserScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<TitaniumTankScreenHandler> TITANIUM_TANK_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "titanium_tank"),
                    new MenuType<>(TitaniumTankScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<SuperComputerScreenHandler> SUPER_COMPUTER_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "super_computer"),
                    new MenuType<>(SuperComputerScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<LaserQuarryScreenHandler> LASER_QUARRY_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "laser_quarry"),
                    new MenuType<>(LaserQuarryScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<ModularSuitScreenHandler> MODULAR_SUIT_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "modular_suit"),
                    new MenuType<>(ModularSuitScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<PoweredAnvilScreenHandler> POWERED_ANVIL_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "powered_anvil"),
                    new MenuType<>(PoweredAnvilScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<IndustrialCentrifugeScreenHandler> INDUSTRIAL_CENTRIFUGE_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "industrial_centrifuge"),
                    new MenuType<>(IndustrialCentrifugeScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<ChemicalSynthesizerScreenHandler> CHEMICAL_SYNTHESIZER_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "chemical_synthesizer"),
                    new MenuType<>(ChemicalSynthesizerScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<PolymerLoomScreenHandler> POLYMER_LOOM_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "polymer_loom"),
                    new MenuType<>(PolymerLoomScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<SterileMedicalCabinetScreenHandler> STERILE_MEDICAL_CABINET_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "sterile_medical_cabinet"),
                    new MenuType<>(SterileMedicalCabinetScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<CircuitFabricatorScreenHandler> CIRCUIT_FABRICATOR_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "circuit_fabricator"),
                    new MenuType<>(CircuitFabricatorScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<InductionSmelterScreenHandler> INDUCTION_SMELTER_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "induction_smelter"),
                    new MenuType<>(InductionSmelterScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<CastingPortScreenHandler> CASTING_PORT_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "casting_port"),
                    new MenuType<>(CastingPortScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<BrickOvenScreenHandler> BRICK_OVEN_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "brick_oven"),
                    new MenuType<>(BrickOvenScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static final MenuType<IceCreamMachineScreenHandler> ICE_CREAM_MACHINE_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "ice_cream_machine"),
                    new MenuType<>(IceCreamMachineScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public static void registerScreenHandlers() {
        EnchantedWoodMod.LOGGER.info("Registering Screen Handlers for " + EnchantedWoodMod.MOD_ID);
    }
}

