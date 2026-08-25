package net.enchantedwood.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.block.custom.EnchantedFurnaceBlock;

public class ModBlocks {

    public static final Block ENCHANTED_COBBLESTONE = registerBlock("enchanted_cobblestone",
            new Block(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "enchanted_cobblestone")))
                    .sounds(BlockSoundGroup.STONE)
                    .hardness(2.0f)
                    .resistance(6.0f)
                    .requiresTool()));

    public static final Block ENCHANTED_FURNACE = registerBlock("enchanted_furnace",
            new EnchantedFurnaceBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "enchanted_furnace")))
                    .sounds(BlockSoundGroup.STONE)
                    .hardness(3.5f)
                    .resistance(3.5f)
                    .requiresTool()
                    .luminance(state -> state.get(AbstractFurnaceBlock.LIT) ? 13 : 0)));

    public static final Block CRUSHER = registerBlock("crusher",
            new net.enchantedwood.block.custom.CrusherBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "crusher")))
                    .sounds(BlockSoundGroup.METAL)
                    .hardness(4.0f)
                    .resistance(8.0f)
                    .requiresTool()
                    .luminance(state -> state.get(net.enchantedwood.block.custom.CrusherBlock.LIT) ? 13 : 0)));

    public static final Block DUST_SMELTER = registerBlock("dust_smelter",
            new net.enchantedwood.block.custom.DustSmelterBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "dust_smelter")))
                    .sounds(BlockSoundGroup.METAL)
                    .hardness(4.0f)
                    .resistance(8.0f)
                    .requiresTool()
                    .luminance(state -> state.get(net.enchantedwood.block.custom.DustSmelterBlock.LIT) ? 13 : 0)));

    public static final Block ENCHANTED_CHEST = registerBlock("enchanted_chest",
            new net.enchantedwood.block.custom.EnchantedChestBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "enchanted_chest")))
                    .sounds(BlockSoundGroup.WOOD)
                    .hardness(3.0f)
                    .resistance(6.0f)
                    .nonOpaque()));


    public static final Block ENCHANTED_STORAGE_CONTROLLER = registerBlock("enchanted_storage_controller",
            new net.enchantedwood.block.custom.EnchantedStorageControllerBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "enchanted_storage_controller")))
                    .sounds(BlockSoundGroup.METAL)
                    .hardness(4.0f)
                    .resistance(8.0f)
                    .requiresTool()
                    .luminance(state -> state.get(net.enchantedwood.block.custom.EnchantedStorageControllerBlock.LIT) ? 14 : 0)));

    public static final Block ENCHANTED_DRIVE_BAY = registerBlock("enchanted_drive_bay",
            new net.enchantedwood.block.custom.EnchantedDriveBayBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "enchanted_drive_bay")))
                    .sounds(BlockSoundGroup.METAL)
                    .hardness(4.0f)
                    .resistance(8.0f)
                    .requiresTool()));

    public static final Block ENCHANTED_STORAGE_TERMINAL = registerBlock("enchanted_storage_terminal",
            new net.enchantedwood.block.custom.EnchantedStorageTerminalBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "enchanted_storage_terminal")))
                    .sounds(BlockSoundGroup.METAL)
                    .hardness(4.0f)
                    .resistance(8.0f)
                    .requiresTool()));

    public static final Block ENCHANTED_COAL_BLOCK = registerBlock("enchanted_coal_block",
            new net.enchantedwood.block.custom.EnchantedCoalBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "enchanted_coal_block")))
                    .sounds(BlockSoundGroup.STONE)
                    .hardness(5.0f)
                    .resistance(6.0f)
                    .requiresTool()));

    public static final Block ENCHANTED_LAVA_GENERATOR = registerBlock("enchanted_lava_generator",
            new net.enchantedwood.block.custom.EnchantedLavaGeneratorBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "enchanted_lava_generator")))
                    .sounds(BlockSoundGroup.METAL)
                    .hardness(4.0f)
                    .resistance(8.0f)
                    .requiresTool()
                    .luminance(state -> state.get(net.enchantedwood.block.custom.EnchantedLavaGeneratorBlock.LIT) ? 14 : 0)));

    public static final Block TIN_ORE = registerBlock("tin_ore",
            new Block(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "tin_ore")))
                    .sounds(BlockSoundGroup.STONE)
                    .hardness(3.0f)
                    .resistance(3.0f)
                    .requiresTool()));

    public static final Block DEEPSLATE_TIN_ORE = registerBlock("deepslate_tin_ore",
            new Block(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "deepslate_tin_ore")))
                    .sounds(BlockSoundGroup.DEEPSLATE)
                    .hardness(4.5f)
                    .resistance(3.0f)
                    .requiresTool()));

    public static final Block RAW_TIN_BLOCK = registerBlock("raw_tin_block",
            new Block(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "raw_tin_block")))
                    .sounds(BlockSoundGroup.STONE)
                    .hardness(5.0f)
                    .resistance(6.0f)
                    .requiresTool()));

    public static final Block TIN_BLOCK = registerBlock("tin_block",
            new Block(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "tin_block")))
                    .sounds(BlockSoundGroup.METAL)
                    .hardness(4.0f)
                    .resistance(6.0f)
                    .requiresTool()));

    public static final Block TITANIUM_ORE = registerBlock("titanium_ore",
            new Block(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "titanium_ore")))
                    .sounds(BlockSoundGroup.STONE)
                    .hardness(4.0f)
                    .resistance(4.0f)
                    .requiresTool()));

    public static final Block DEEPSLATE_TITANIUM_ORE = registerBlock("deepslate_titanium_ore",
            new Block(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "deepslate_titanium_ore")))
                    .sounds(BlockSoundGroup.DEEPSLATE)
                    .hardness(5.5f)
                    .resistance(4.0f)
                    .requiresTool()));

    public static final Block RAW_TITANIUM_BLOCK = registerBlock("raw_titanium_block",
            new Block(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "raw_titanium_block")))
                    .sounds(BlockSoundGroup.METAL)
                    .hardness(5.0f)
                    .resistance(6.0f)
                    .requiresTool()));

    public static final Block TITANIUM_BLOCK = registerBlock("titanium_block",
            new Block(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "titanium_block")))
                    .sounds(BlockSoundGroup.METAL)
                    .hardness(6.0f)
                    .resistance(7.0f)
                    .requiresTool()));

    public static final Block COPPER_GENERATOR = registerBlock("copper_generator",
            new net.enchantedwood.block.custom.CopperGeneratorBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "copper_generator")))
                    .sounds(BlockSoundGroup.COPPER)
                    .hardness(3.5f)
                    .resistance(6.0f)
                    .requiresTool()
                    .luminance(state -> state.get(net.enchantedwood.block.custom.CopperGeneratorBlock.LIT) ? 13 : 0)));

    public static final Block COPPER_BATTERY = registerBlock("copper_battery",
            new net.enchantedwood.block.custom.CopperBatteryBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "copper_battery")))
                    .sounds(BlockSoundGroup.COPPER)
                    .hardness(3.5f)
                    .resistance(6.0f)
                    .requiresTool()));

    public static final Block COPPER_CABLE = registerBlock("copper_cable",
            new net.enchantedwood.block.custom.CopperCableBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "copper_cable")))
                    .sounds(BlockSoundGroup.COPPER)
                    .hardness(1.0f)
                    .resistance(2.0f)
                    .nonOpaque()));

    // Phase 2: Aluminum Metallurgy & Gas Transport
    public static final Block BAUXITE_ORE = registerBlock("bauxite_ore",
            new Block(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "bauxite_ore")))
                    .sounds(BlockSoundGroup.STONE)
                    .hardness(3.0f)
                    .resistance(3.0f)
                    .requiresTool()));

    public static final Block DEEPSLATE_BAUXITE_ORE = registerBlock("deepslate_bauxite_ore",
            new Block(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "deepslate_bauxite_ore")))
                    .sounds(BlockSoundGroup.DEEPSLATE)
                    .hardness(4.5f)
                    .resistance(3.0f)
                    .requiresTool()));

    public static final Block RAW_BAUXITE_BLOCK = registerBlock("raw_bauxite_block",
            new Block(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "raw_bauxite_block")))
                    .sounds(BlockSoundGroup.STONE)
                    .hardness(5.0f)
                    .resistance(6.0f)
                    .requiresTool()));

    public static final Block ALUMINUM_BLOCK = registerBlock("aluminum_block",
            new Block(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "aluminum_block")))
                    .sounds(BlockSoundGroup.METAL)
                    .hardness(4.0f)
                    .resistance(6.0f)
                    .requiresTool()));

    public static final Block GAS_PIPE = registerBlock("gas_pipe",
            new net.enchantedwood.block.custom.GasPipeBlock(net.enchantedwood.gas.GasType.OXYGEN, AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "gas_pipe")))
                    .sounds(BlockSoundGroup.GLASS)
                    .hardness(1.0f)
                    .resistance(2.0f)
                    .nonOpaque()));

    public static final Block HYDROGEN_PIPE = registerBlock("hydrogen_pipe",
            new net.enchantedwood.block.custom.GasPipeBlock(net.enchantedwood.gas.GasType.HYDROGEN, AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "hydrogen_pipe")))
                    .sounds(BlockSoundGroup.GLASS)
                    .hardness(1.0f)
                    .resistance(2.0f)
                    .nonOpaque()));

    public static final Block OXYGEN_GENERATOR = registerBlock("oxygen_generator",
            new net.enchantedwood.block.custom.OxygenGeneratorBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "oxygen_generator")))
                    .sounds(BlockSoundGroup.METAL)
                    .hardness(4.0f)
                    .resistance(6.0f)
                    .requiresTool()
                    .luminance(state -> state.get(net.enchantedwood.block.custom.OxygenGeneratorBlock.LIT) ? 12 : 0)));

    public static final Block ALUMINUM_REFINER = registerBlock("aluminum_refiner",
            new net.enchantedwood.block.custom.AluminumRefinerBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "aluminum_refiner")))
                    .sounds(BlockSoundGroup.METAL)
                    .hardness(4.0f)
                    .resistance(6.0f)
                    .requiresTool()
                    .luminance(state -> state.get(net.enchantedwood.block.custom.AluminumRefinerBlock.LIT) ? 13 : 0)));

    public static final Block ALUMINUM_GENERATOR = registerBlock("aluminum_generator",
            new net.enchantedwood.block.custom.AluminumGeneratorBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "aluminum_generator")))
                    .sounds(BlockSoundGroup.METAL)
                    .hardness(4.0f)
                    .resistance(6.0f)
                    .requiresTool()
                    .luminance(state -> state.get(net.enchantedwood.block.custom.AluminumGeneratorBlock.LIT) ? 14 : 0)));

    public static final Block ALUMINUM_BATTERY = registerBlock("aluminum_battery",
            new net.enchantedwood.block.custom.AluminumBatteryBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "aluminum_battery")))
                    .sounds(BlockSoundGroup.METAL)
                    .hardness(4.0f)
                    .resistance(6.0f)
                    .requiresTool()));

    public static final Block ALUMINUM_CABLE = registerBlock("aluminum_cable",
            new net.enchantedwood.block.custom.AluminumCableBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "aluminum_cable")))
                    .sounds(BlockSoundGroup.METAL)
                    .hardness(1.0f)
                    .resistance(2.0f)
                    .nonOpaque()));

    // Phase 3: Coke Coal & Steel Metallurgy
    public static final Block STEEL_BLOCK = registerBlock("steel_block",
            new Block(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "steel_block")))
                    .sounds(BlockSoundGroup.NETHERITE)
                    .hardness(7.0f)
                    .resistance(10.0f)
                    .requiresTool()));

    public static final Block COKE_OVEN = registerBlock("coke_oven",
            new net.enchantedwood.block.custom.CokeOvenBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "coke_oven")))
                    .sounds(BlockSoundGroup.STONE)
                    .hardness(4.5f)
                    .resistance(8.0f)
                    .requiresTool()
                    .luminance(state -> state.get(net.enchantedwood.block.custom.CokeOvenBlock.LIT) ? 13 : 0)));

    public static final Block STEEL_BLAST_FURNACE = registerBlock("steel_blast_furnace",
            new net.enchantedwood.block.custom.SteelBlastFurnaceBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "steel_blast_furnace")))
                    .sounds(BlockSoundGroup.NETHERITE)
                    .hardness(5.5f)
                    .resistance(10.0f)
                    .requiresTool()
                    .luminance(state -> state.get(net.enchantedwood.block.custom.SteelBlastFurnaceBlock.LIT) ? 14 : 0)));

    public static final Block STEEL_GENERATOR = registerBlock("steel_generator",
            new net.enchantedwood.block.custom.SteelGeneratorBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "steel_generator")))
                    .sounds(BlockSoundGroup.NETHERITE)
                    .hardness(5.5f)
                    .resistance(10.0f)
                    .requiresTool()
                    .luminance(state -> state.get(net.enchantedwood.block.custom.SteelGeneratorBlock.LIT) ? 15 : 0)));

    public static final Block STEEL_BATTERY = registerBlock("steel_battery",
            new net.enchantedwood.block.custom.SteelBatteryBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "steel_battery")))
                    .sounds(BlockSoundGroup.NETHERITE)
                    .hardness(5.5f)
                    .resistance(10.0f)
                    .requiresTool()));

    public static final Block STEEL_CABLE = registerBlock("steel_cable",
            new net.enchantedwood.block.custom.SteelCableBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "steel_cable")))
                    .sounds(BlockSoundGroup.NETHERITE)
                    .hardness(1.5f)
                    .resistance(3.0f)
                    .nonOpaque()));

    public static final Block BRONZE_BLOCK = registerBlock("bronze_block",
            new Block(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "bronze_block")))
                    .sounds(BlockSoundGroup.COPPER)
                    .hardness(5.0f)
                    .resistance(6.0f)
                    .requiresTool()));

    public static final Block COKE_COAL_BLOCK = registerBlock("coke_coal_block",
            new Block(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "coke_coal_block")))
                    .sounds(BlockSoundGroup.STONE)
                    .hardness(5.0f)
                    .resistance(6.0f)
                    .requiresTool()));

    public static final Block ENCHANTED_NETHERITE_BLOCK = registerBlock("enchanted_netherite_block",
            new Block(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "enchanted_netherite_block")))
                    .sounds(BlockSoundGroup.NETHERITE)
                    .hardness(50.0f)
                    .resistance(1200.0f)
                    .requiresTool()));

    // Rubber Tree Blocks
    public static final Block RUBBER_LOG = registerBlock("rubber_log",
            new net.minecraft.block.PillarBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "rubber_log")))
                    .sounds(BlockSoundGroup.WOOD)
                    .hardness(2.0f)
                    .resistance(2.0f)));

    public static final Block RUBBER_WOOD = registerBlock("rubber_wood",
            new net.minecraft.block.PillarBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "rubber_wood")))
                    .sounds(BlockSoundGroup.WOOD)
                    .hardness(2.0f)
                    .resistance(2.0f)));

    public static final Block STRIPPED_RUBBER_LOG = registerBlock("stripped_rubber_log",
            new net.minecraft.block.PillarBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "stripped_rubber_log")))
                    .sounds(BlockSoundGroup.WOOD)
                    .hardness(2.0f)
                    .resistance(2.0f)));

    public static final Block STRIPPED_RUBBER_WOOD = registerBlock("stripped_rubber_wood",
            new net.minecraft.block.PillarBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "stripped_rubber_wood")))
                    .sounds(BlockSoundGroup.WOOD)
                    .hardness(2.0f)
                    .resistance(2.0f)));

    public static final Block RUBBER_PLANKS = registerBlock("rubber_planks",
            new Block(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "rubber_planks")))
                    .sounds(BlockSoundGroup.WOOD)
                    .hardness(2.0f)
                    .resistance(3.0f)));

    public static final Block RUBBER_LEAVES = registerBlock("rubber_leaves",
            new net.enchantedwood.block.custom.RubberLeavesBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "rubber_leaves")))
                    .sounds(BlockSoundGroup.GRASS)
                    .hardness(0.2f)
                    .resistance(0.2f)
                    .nonOpaque()
                    .suffocates((state, world, pos) -> false)
                    .blockVision((state, world, pos) -> false)));

    public static final Block RUBBER_SAPLING = registerBlock("rubber_sapling",
            new net.minecraft.block.SaplingBlock(net.enchantedwood.world.ModSaplingGenerators.RUBBER, AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "rubber_sapling")))
                    .sounds(BlockSoundGroup.GRASS)
                    .noCollision()
                    .breakInstantly()));

    public static final Block ENCHANTED_LAMP = registerBlock("enchanted_lamp",
            new net.enchantedwood.block.custom.EnchantedLampBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "enchanted_lamp")))
                    .sounds(BlockSoundGroup.AMETHYST_BLOCK)
                    .luminance(state -> state.get(net.enchantedwood.block.custom.EnchantedLampBlock.LIT) ? 15 : 0)
                    .hardness(0.5f)
                    .resistance(3.0f)
                    .nonOpaque()));

    public static final Block MINING_PORTAL = registerBlockWithoutItem("mining_portal",
            new net.enchantedwood.block.custom.MiningPortalBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "mining_portal")))
                    .noCollision()
                    .strength(-1.0F)
                    .sounds(BlockSoundGroup.GLASS)
                    .luminance(state -> 11)
                    .pistonBehavior(net.minecraft.block.piston.PistonBehavior.BLOCK)
                    .nonOpaque()));

    public static final Block ATMOSPHERIC_ANCHOR = registerBlock("atmospheric_anchor",
            new net.enchantedwood.block.custom.AtmosphericAnchorBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "atmospheric_anchor")))
                    .sounds(BlockSoundGroup.NETHERITE)
                    .hardness(6.0f)
                    .resistance(1200.0f)
                    .requiresTool()));

    public static final Block OIL_SAND = registerBlock("oil_sand",
            new Block(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "oil_sand")))
                    .sounds(BlockSoundGroup.SAND)
                    .hardness(0.8f)
                    .resistance(0.8f)));

    public static final Block ASPHALT_BLOCK = registerBlock("asphalt_block",
            new net.enchantedwood.block.custom.AsphaltBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "asphalt_block")))
                    .sounds(BlockSoundGroup.STONE)
                    .hardness(2.0f)
                    .resistance(6.0f)
                    .requiresTool()));

    public static final Block ASPHALT_SLAB = registerBlock("asphalt_slab",
            new net.minecraft.block.SlabBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "asphalt_slab")))
                    .sounds(BlockSoundGroup.STONE)
                    .hardness(2.0f)
                    .resistance(6.0f)
                    .requiresTool()));

    public static final Block FUEL_REFINERY = registerBlock("fuel_refinery",
            new net.enchantedwood.block.custom.FuelRefineryBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "fuel_refinery")))
                    .sounds(BlockSoundGroup.METAL)
                    .hardness(4.0f)
                    .resistance(8.0f)
                    .requiresTool()
                    .luminance(state -> state.get(net.enchantedwood.block.custom.FuelRefineryBlock.LIT) ? 13 : 0)));

    public static final Block ROAD_PAVER = registerBlock("road_paver",
            new net.enchantedwood.block.custom.RoadPaverBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "road_paver")))
                    .sounds(BlockSoundGroup.METAL)
                    .hardness(4.0f)
                    .resistance(8.0f)
                    .requiresTool()
                    .luminance(state -> state.get(net.enchantedwood.block.custom.RoadPaverBlock.PAVING) ? 10 : 0)));

    public static final Block CORN_CROP = registerBlockWithoutItem("corn_crop",
            new net.enchantedwood.block.custom.CornCropBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "corn_crop")))
                    .noCollision()
                    .ticksRandomly()
                    .breakInstantly()
                    .sounds(BlockSoundGroup.CROP)
                    .pistonBehavior(net.minecraft.block.piston.PistonBehavior.DESTROY)
                    .nonOpaque()));

    private static Block registerBlockWithoutItem(String name, Block block) {
        RegistryKey<Block> blockKey = RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, name));
        return Registry.register(Registries.BLOCK, blockKey, block);
    }

    private static Block registerBlock(String name, Block block) {
        RegistryKey<Block> blockKey = RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, name));
        RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(EnchantedWoodMod.MOD_ID, name));

        Block registeredBlock = Registry.register(Registries.BLOCK, blockKey, block);
        Registry.register(Registries.ITEM, itemKey, new BlockItem(registeredBlock, new Item.Settings().registryKey(itemKey).useBlockPrefixedTranslationKey()));

        return registeredBlock;
    }

    public static void registerModBlocks() {
        EnchantedWoodMod.LOGGER.info("Registering Enchanted Wood Blocks for " + EnchantedWoodMod.MOD_ID);
    }
}
