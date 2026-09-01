package net.enchantedwood.block.entity;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.block.ModBlocks;

public class ModBlockEntities {
    public static final BlockEntityType<EnchantedFurnaceBlockEntity> ENCHANTED_FURNACE_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "enchanted_furnace"),
                    FabricBlockEntityTypeBuilder.create(EnchantedFurnaceBlockEntity::new, ModBlocks.ENCHANTED_FURNACE).build()
            );

    public static final BlockEntityType<CrusherBlockEntity> CRUSHER_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "crusher"),
                    FabricBlockEntityTypeBuilder.create(CrusherBlockEntity::new, ModBlocks.CRUSHER).build()
            );

    public static final BlockEntityType<DustSmelterBlockEntity> DUST_SMELTER_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "dust_smelter"),
                    FabricBlockEntityTypeBuilder.create(DustSmelterBlockEntity::new, ModBlocks.DUST_SMELTER).build()
            );


    public static final BlockEntityType<EnchantedLavaGeneratorBlockEntity> ENCHANTED_LAVA_GENERATOR_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "enchanted_lava_generator"),
                    FabricBlockEntityTypeBuilder.create(EnchantedLavaGeneratorBlockEntity::new, ModBlocks.ENCHANTED_LAVA_GENERATOR).build()
            );

    public static final BlockEntityType<EnchantedChestBlockEntity> ENCHANTED_CHEST_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "enchanted_chest"),
                    FabricBlockEntityTypeBuilder.create(EnchantedChestBlockEntity::new, ModBlocks.ENCHANTED_CHEST).build()
            );

    public static final BlockEntityType<EnchantedStorageControllerBlockEntity> ENCHANTED_STORAGE_CONTROLLER_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "enchanted_storage_controller"),
                    FabricBlockEntityTypeBuilder.create(EnchantedStorageControllerBlockEntity::new, ModBlocks.ENCHANTED_STORAGE_CONTROLLER).build()
            );

    public static final BlockEntityType<EnchantedDriveBayBlockEntity> ENCHANTED_DRIVE_BAY_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "enchanted_drive_bay"),
                    FabricBlockEntityTypeBuilder.create(EnchantedDriveBayBlockEntity::new, ModBlocks.ENCHANTED_DRIVE_BAY).build()
            );

    public static final BlockEntityType<EnchantedStorageTerminalBlockEntity> ENCHANTED_STORAGE_TERMINAL_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "enchanted_storage_terminal"),
                    FabricBlockEntityTypeBuilder.create(EnchantedStorageTerminalBlockEntity::new, ModBlocks.ENCHANTED_STORAGE_TERMINAL).build()
            );

    public static final BlockEntityType<DigitalConverterBlockEntity> DIGITAL_CONVERTER_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "digital_converter"),
                    FabricBlockEntityTypeBuilder.create(DigitalConverterBlockEntity::new, ModBlocks.DIGITAL_CONVERTER).build()
            );

    public static final BlockEntityType<SuperComputerBlockEntity> SUPER_COMPUTER_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "super_computer"),
                    FabricBlockEntityTypeBuilder.create(SuperComputerBlockEntity::new, ModBlocks.SUPER_COMPUTER).build()
            );

    public static final BlockEntityType<CopperGeneratorBlockEntity> COPPER_GENERATOR_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "copper_generator"),
                    FabricBlockEntityTypeBuilder.create(CopperGeneratorBlockEntity::new, ModBlocks.COPPER_GENERATOR).build()
            );

    public static final BlockEntityType<CopperBatteryBlockEntity> COPPER_BATTERY_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "copper_battery"),
                    FabricBlockEntityTypeBuilder.create(CopperBatteryBlockEntity::new, ModBlocks.COPPER_BATTERY).build()
            );

    public static final BlockEntityType<CopperCableBlockEntity> COPPER_CABLE_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "copper_cable"),
                    FabricBlockEntityTypeBuilder.create(CopperCableBlockEntity::new, ModBlocks.COPPER_CABLE).build()
            );

    public static final BlockEntityType<GasPipeBlockEntity> GAS_PIPE_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "gas_pipe"),
                    FabricBlockEntityTypeBuilder.create(GasPipeBlockEntity::new, ModBlocks.GAS_PIPE).build()
            );

    public static final BlockEntityType<GasPipeBlockEntity> HYDROGEN_PIPE_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "hydrogen_pipe"),
                    FabricBlockEntityTypeBuilder.create(GasPipeBlockEntity::new, ModBlocks.HYDROGEN_PIPE).build()
            );

    public static final BlockEntityType<OxygenGeneratorBlockEntity> OXYGEN_GENERATOR_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "oxygen_generator"),
                    FabricBlockEntityTypeBuilder.create(OxygenGeneratorBlockEntity::new, ModBlocks.OXYGEN_GENERATOR).build()
            );

    public static final BlockEntityType<AluminumRefinerBlockEntity> ALUMINUM_REFINER_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "aluminum_refiner"),
                    FabricBlockEntityTypeBuilder.create(AluminumRefinerBlockEntity::new, ModBlocks.ALUMINUM_REFINER).build()
            );

    public static final BlockEntityType<AluminumGeneratorBlockEntity> ALUMINUM_GENERATOR_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "aluminum_generator"),
                    FabricBlockEntityTypeBuilder.create(AluminumGeneratorBlockEntity::new, ModBlocks.ALUMINUM_GENERATOR).build()
            );

    public static final BlockEntityType<AluminumBatteryBlockEntity> ALUMINUM_BATTERY_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "aluminum_battery"),
                    FabricBlockEntityTypeBuilder.create(AluminumBatteryBlockEntity::new, ModBlocks.ALUMINUM_BATTERY).build()
            );

    public static final BlockEntityType<AluminumCableBlockEntity> ALUMINUM_CABLE_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "aluminum_cable"),
                    FabricBlockEntityTypeBuilder.create(AluminumCableBlockEntity::new, ModBlocks.ALUMINUM_CABLE).build()
            );

    // Phase 3: Coke Coal & Steel Metallurgy
    public static final BlockEntityType<CokeOvenBlockEntity> COKE_OVEN_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "coke_oven"),
                    FabricBlockEntityTypeBuilder.create(CokeOvenBlockEntity::new, ModBlocks.COKE_OVEN).build()
            );

    public static final BlockEntityType<SteelBlastFurnaceBlockEntity> STEEL_BLAST_FURNACE_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "steel_blast_furnace"),
                    FabricBlockEntityTypeBuilder.create(SteelBlastFurnaceBlockEntity::new, ModBlocks.STEEL_BLAST_FURNACE).build()
            );

    public static final BlockEntityType<SteelGeneratorBlockEntity> STEEL_GENERATOR_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "steel_generator"),
                    FabricBlockEntityTypeBuilder.create(SteelGeneratorBlockEntity::new, ModBlocks.STEEL_GENERATOR).build()
            );

    public static final BlockEntityType<SteelBatteryBlockEntity> STEEL_BATTERY_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "steel_battery"),
                    FabricBlockEntityTypeBuilder.create(SteelBatteryBlockEntity::new, ModBlocks.STEEL_BATTERY).build()
            );

    public static final BlockEntityType<SteelCableBlockEntity> STEEL_CABLE_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "steel_cable"),
                    FabricBlockEntityTypeBuilder.create(SteelCableBlockEntity::new, ModBlocks.STEEL_CABLE).build()
            );

    public static final BlockEntityType<EnchantedLampBlockEntity> ENCHANTED_LAMP_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "enchanted_lamp"),
                    FabricBlockEntityTypeBuilder.create(EnchantedLampBlockEntity::new, ModBlocks.ENCHANTED_LAMP).build()
            );

    public static final BlockEntityType<FuelRefineryBlockEntity> FUEL_REFINERY_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "fuel_refinery"),
                    FabricBlockEntityTypeBuilder.create(FuelRefineryBlockEntity::new, ModBlocks.FUEL_REFINERY).build()
            );

    public static final BlockEntityType<RoadPaverBlockEntity> ROAD_PAVER_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "road_paver"),
                    FabricBlockEntityTypeBuilder.create(RoadPaverBlockEntity::new, ModBlocks.ROAD_PAVER).build()
            );

    public static final BlockEntityType<VehicleFabricatorBlockEntity> VEHICLE_FABRICATOR_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "vehicle_fabricator"),
                    FabricBlockEntityTypeBuilder.create(VehicleFabricatorBlockEntity::new, ModBlocks.VEHICLE_FABRICATOR).build()
            );

    // Phase 2: Nether Factory & Tier 4 Power Grid
    public static final BlockEntityType<TungstenBatteryBlockEntity> TUNGSTEN_BATTERY_BE =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "tungsten_battery"),
                    FabricBlockEntityTypeBuilder.create(TungstenBatteryBlockEntity::new, ModBlocks.TUNGSTEN_BATTERY).build()
            );

    public static final BlockEntityType<TungstenCableBlockEntity> TUNGSTEN_CABLE_BE =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "tungsten_cable"),
                    FabricBlockEntityTypeBuilder.create(TungstenCableBlockEntity::new, ModBlocks.TUNGSTEN_CABLE).build()
            );

    public static final BlockEntityType<BasaltCableBlockEntity> BASALT_CABLE_BE =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "basalt_cable"),
                    FabricBlockEntityTypeBuilder.create(BasaltCableBlockEntity::new, ModBlocks.BASALT_CABLE).build()
            );

    public static final BlockEntityType<GeothermalGeneratorBlockEntity> GEOTHERMAL_GENERATOR_BE =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "geothermal_generator"),
                    FabricBlockEntityTypeBuilder.create(GeothermalGeneratorBlockEntity::new, ModBlocks.GEOTHERMAL_GENERATOR).build()
            );

    public static final BlockEntityType<AlloyFoundryBlockEntity> ALLOY_FOUNDRY_BE =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "alloy_foundry"),
                    FabricBlockEntityTypeBuilder.create(AlloyFoundryBlockEntity::new, ModBlocks.ALLOY_FOUNDRY).build()
            );

    public static final BlockEntityType<ItemSalvagerBlockEntity> ITEM_SALVAGER_BE =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "item_salvager"),
                    FabricBlockEntityTypeBuilder.create(ItemSalvagerBlockEntity::new, ModBlocks.ITEM_SALVAGER).build()
            );

    public static final BlockEntityType<MagmaCrucibleBlockEntity> MAGMA_CRUCIBLE_BE =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "magma_crucible"),
                    FabricBlockEntityTypeBuilder.create(MagmaCrucibleBlockEntity::new, ModBlocks.MAGMA_CRUCIBLE).build()
            );

    public static final BlockEntityType<LavaPumpBlockEntity> LAVA_PUMP_BE =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "lava_pump"),
                    FabricBlockEntityTypeBuilder.create(LavaPumpBlockEntity::new, ModBlocks.LAVA_PUMP).build()
            );

    public static final BlockEntityType<CrusherMk2BlockEntity> CRUSHER_MK2_BE =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "crusher_mk2"),
                    FabricBlockEntityTypeBuilder.create(CrusherMk2BlockEntity::new, ModBlocks.CRUSHER_MK2).build()
            );

    public static final BlockEntityType<SoilInfuserBlockEntity> SOIL_INFUSER_BE =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "soil_infuser"),
                    FabricBlockEntityTypeBuilder.create(SoilInfuserBlockEntity::new, ModBlocks.SOIL_INFUSER).build()
            );

    public static final BlockEntityType<TitaniumLavaPipeBlockEntity> TITANIUM_LAVA_PIPE_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "titanium_lava_pipe"),
                    FabricBlockEntityTypeBuilder.create(TitaniumLavaPipeBlockEntity::new, ModBlocks.TITANIUM_LAVA_PIPE).build()
            );

    public static final BlockEntityType<TitaniumTankCasingBlockEntity> TITANIUM_TANK_CASING_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "titanium_tank_casing"),
                    FabricBlockEntityTypeBuilder.create(TitaniumTankCasingBlockEntity::new, ModBlocks.TITANIUM_TANK_CASING).build()
            );

    public static final BlockEntityType<TitaniumTankControllerBlockEntity> TITANIUM_TANK_CONTROLLER_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "titanium_tank_controller"),
                    FabricBlockEntityTypeBuilder.create(TitaniumTankControllerBlockEntity::new, ModBlocks.TITANIUM_TANK_INBOUND_PORT).build()
            );

    // Universal Item Logistics System
    public static final BlockEntityType<ItemPipeBlockEntity> ITEM_PIPE_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "item_pipe"),
                    FabricBlockEntityTypeBuilder.create(ItemPipeBlockEntity::new, ModBlocks.ITEM_PIPE).build()
            );

    public static final BlockEntityType<ItemExtractorBlockEntity> ITEM_EXTRACTOR_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "item_extractor"),
                    FabricBlockEntityTypeBuilder.create(ItemExtractorBlockEntity::new, ModBlocks.ITEM_EXTRACTOR).build()
            );

    public static final BlockEntityType<ItemInserterBlockEntity> ITEM_INSERTER_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "item_inserter"),
                    FabricBlockEntityTypeBuilder.create(ItemInserterBlockEntity::new, ModBlocks.ITEM_INSERTER).build()
            );

    public static final BlockEntityType<LaserQuarryBlockEntity> LASER_QUARRY_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(EnchantedWoodMod.MOD_ID, "laser_quarry"),
                    FabricBlockEntityTypeBuilder.create(LaserQuarryBlockEntity::new, ModBlocks.LASER_QUARRY).build()
            );

    public static void registerBlockEntities() {
        EnchantedWoodMod.LOGGER.info("Registering Block Entities for " + EnchantedWoodMod.MOD_ID);
    }
}
