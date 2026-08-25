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

    public static void registerBlockEntities() {
        EnchantedWoodMod.LOGGER.info("Registering Block Entities for " + EnchantedWoodMod.MOD_ID);
    }
}
