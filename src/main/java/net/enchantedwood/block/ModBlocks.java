package net.enchantedwood.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.Block;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.text.Text;
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

    public static final Block HYDRAULIC_PRESS = registerBlock("hydraulic_press",
            new net.enchantedwood.block.custom.HydraulicPressBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "hydraulic_press")))
                    .sounds(BlockSoundGroup.NETHERITE)
                    .hardness(5.0f)
                    .resistance(10.0f)
                    .requiresTool()
                    .luminance(state -> state.get(net.enchantedwood.block.custom.HydraulicPressBlock.LIT) ? 13 : 0)));

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

    public static final Block DIGITAL_CONVERTER = registerBlock("digital_converter",
            new net.enchantedwood.block.custom.DigitalConverterBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "digital_converter")))
                    .sounds(BlockSoundGroup.METAL)
                    .hardness(4.0f)
                    .resistance(8.0f)
                    .requiresTool()
                    .luminance(state -> state.get(net.enchantedwood.block.custom.DigitalConverterBlock.LIT) ? 12 : 0)));

    public static final Block SUPER_COMPUTER = registerBlockWithTooltip("super_computer",
            new net.enchantedwood.block.custom.SuperComputerBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "super_computer")))
                    .sounds(BlockSoundGroup.NETHERITE)
                    .hardness(6.0f)
                    .resistance(12.0f)
                    .requiresTool()
                    .luminance(state -> state.get(net.enchantedwood.block.custom.SuperComputerBlock.LIT) ? 14 : 0)),
            Text.literal("§6Quantum Super Computer (Auto-Crafter)"),
            Text.literal("§8Pulls ingredients from Digital Storage Network crystals & auto-crafts recipes at high speed."));

    public static final Block LASER_QUARRY = registerBlockWithTooltip("laser_quarry",
            new net.enchantedwood.block.custom.LaserQuarryBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "laser_quarry")))
                    .sounds(BlockSoundGroup.NETHERITE)
                    .hardness(6.0f)
                    .resistance(12.0f)
                    .requiresTool()
                    .luminance(state -> state.get(net.enchantedwood.block.custom.LaserQuarryBlock.LIT) ? 14 : 0)),
            Text.literal("§6Digital Laser Quarry"),
            Text.literal("§8Autonomous chunk-based precision ore extraction & excavation rig."));

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

    public static final Block DORMANT_RIFT = registerBlockWithoutItem("dormant_rift",
            new net.enchantedwood.block.custom.DormantRiftBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "dormant_rift")))
                    .noCollision()
                    .strength(-1.0F)
                    .sounds(BlockSoundGroup.GLASS)
                    .luminance(state -> 12)
                    .pistonBehavior(net.minecraft.block.piston.PistonBehavior.BLOCK)
                    .nonOpaque()));

    public static final Block ATMOSPHERIC_ANCHOR = registerBlock("atmospheric_anchor",
            new net.enchantedwood.block.custom.AtmosphericAnchorBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "atmospheric_anchor")))
                    .sounds(BlockSoundGroup.NETHERITE)
                    .hardness(6.0f)
                    .resistance(1200.0f)
                    .requiresTool()));

    public static final Block KINETIC_ANCHOR = registerBlock("kinetic_anchor",
            new net.enchantedwood.block.custom.KineticAnchorBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "kinetic_anchor")))
                    .sounds(BlockSoundGroup.NETHERITE)
                    .hardness(6.0f)
                    .resistance(1200.0f)
                    .requiresTool()));

    public static final Block THERMAL_ANCHOR = registerBlock("thermal_anchor",
            new net.enchantedwood.block.custom.ThermalAnchorBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "thermal_anchor")))
                    .sounds(BlockSoundGroup.NETHERITE)
                    .hardness(6.0f)
                    .resistance(1200.0f)
                    .requiresTool()));

    public static final Block METALLURGICAL_ANCHOR = registerBlock("metallurgical_anchor",
            new net.enchantedwood.block.custom.MetallurgicalAnchorBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "metallurgical_anchor")))
                    .sounds(BlockSoundGroup.NETHERITE)
                    .hardness(6.0f)
                    .resistance(1200.0f)
                    .requiresTool()));

    public static final Block PLASMA_ANCHOR = registerBlock("plasma_anchor",
            new net.enchantedwood.block.custom.PlasmaAnchorBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "plasma_anchor")))
                    .sounds(BlockSoundGroup.NETHERITE)
                    .hardness(6.0f)
                    .resistance(1200.0f)
                    .requiresTool()));

    public static final Block DIMENSIONAL_SINGULARITY = registerBlock("dimensional_singularity",
            new net.enchantedwood.block.custom.DimensionalSingularityBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "dimensional_singularity")))
                    .sounds(BlockSoundGroup.NETHERITE)
                    .hardness(10.0f)
                    .resistance(1200.0f)
                    .requiresTool()
                    .luminance(state -> 15)));

    // Nether Metallurgy: Tungsten, Cobalt, Ardite & Manyullyn
    public static final Block NETHER_TUNGSTEN_ORE = registerBlock("nether_tungsten_ore",
            new Block(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "nether_tungsten_ore")))
                    .sounds(BlockSoundGroup.NETHERRACK)
                    .hardness(3.5f)
                    .resistance(3.5f)
                    .requiresTool()));

    public static final Block DEEPSLATE_TUNGSTEN_ORE = registerBlock("deepslate_tungsten_ore",
            new Block(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "deepslate_tungsten_ore")))
                    .sounds(BlockSoundGroup.DEEPSLATE)
                    .hardness(5.0f)
                    .resistance(4.0f)
                    .requiresTool()));

    public static final Block RAW_TUNGSTEN_BLOCK = registerBlock("raw_tungsten_block",
            new Block(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "raw_tungsten_block")))
                    .sounds(BlockSoundGroup.STONE)
                    .hardness(6.0f)
                    .resistance(7.0f)
                    .requiresTool()));

    public static final Block TUNGSTEN_BLOCK = registerBlock("tungsten_block",
            new Block(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "tungsten_block")))
                    .sounds(BlockSoundGroup.NETHERITE)
                    .hardness(6.0f)
                    .resistance(8.0f)
                    .requiresTool()));

    public static final Block COBALT_ORE = registerBlock("cobalt_ore",
            new Block(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "cobalt_ore")))
                    .sounds(BlockSoundGroup.NETHERRACK)
                    .hardness(4.0f)
                    .resistance(4.0f)
                    .requiresTool()));

    public static final Block RAW_COBALT_BLOCK = registerBlock("raw_cobalt_block",
            new Block(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "raw_cobalt_block")))
                    .sounds(BlockSoundGroup.STONE)
                    .hardness(5.5f)
                    .resistance(6.5f)
                    .requiresTool()));

    public static final Block COBALT_BLOCK = registerBlock("cobalt_block",
            new Block(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "cobalt_block")))
                    .sounds(BlockSoundGroup.METAL)
                    .hardness(5.0f)
                    .resistance(7.0f)
                    .requiresTool()));

    public static final Block ARDITE_ORE = registerBlock("ardite_ore",
            new Block(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "ardite_ore")))
                    .sounds(BlockSoundGroup.NETHERRACK)
                    .hardness(4.0f)
                    .resistance(4.0f)
                    .requiresTool()));

    public static final Block RAW_ARDITE_BLOCK = registerBlock("raw_ardite_block",
            new Block(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "raw_ardite_block")))
                    .sounds(BlockSoundGroup.STONE)
                    .hardness(5.5f)
                    .resistance(6.5f)
                    .requiresTool()));

    public static final Block ARDITE_BLOCK = registerBlock("ardite_block",
            new Block(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "ardite_block")))
                    .sounds(BlockSoundGroup.METAL)
                    .hardness(5.0f)
                    .resistance(7.0f)
                    .requiresTool()));

    public static final Block MANYULLYN_BLOCK = registerBlock("manyullyn_block",
            new Block(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "manyullyn_block")))
                    .sounds(BlockSoundGroup.NETHERITE)
                    .hardness(7.0f)
                    .resistance(12.0f)
                    .requiresTool()));

    public static final Block OIL_SAND = registerBlockWithTooltip("oil_sand",
            new Block(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "oil_sand")))
                    .sounds(BlockSoundGroup.SAND)
                    .hardness(0.8f)
                    .resistance(0.8f)),
            Text.literal("§7Mine with a shovel in deserts/badlands to gather §6Crude Oil Sludge§7."));

    public static final Block ASPHALT_BLOCK = registerBlockWithTooltip("asphalt_block",
            new net.enchantedwood.block.custom.AsphaltBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "asphalt_block")))
                    .sounds(BlockSoundGroup.STONE)
                    .hardness(2.0f)
                    .resistance(6.0f)
                    .requiresTool()),
            Text.literal("§a+25% Speed Multiplier §7for players and All-Terrain Vehicles."));

    public static final Block ASPHALT_SLAB = registerBlockWithTooltip("asphalt_slab",
            new net.enchantedwood.block.custom.AsphaltSlabBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "asphalt_slab")))
                    .sounds(BlockSoundGroup.STONE)
                    .hardness(2.0f)
                    .resistance(6.0f)
                    .requiresTool()),
            Text.literal("§a+25% Speed Multiplier §7for players and All-Terrain Vehicles."));

    public static final Block CONCRETE_CURB = registerBlockWithTooltip("concrete_curb",
            new net.enchantedwood.block.custom.ConcreteCurbBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "concrete_curb")))
                    .sounds(BlockSoundGroup.STONE)
                    .hardness(2.0f)
                    .resistance(6.0f)
                    .requiresTool()),
            Text.literal("§7Flanks 5-wide highways with raised outer edge barriers."));

    public static final Block ROAD_TRANSITION_RAMP = registerBlockWithTooltip("road_transition_ramp",
            new net.enchantedwood.block.custom.RoadTransitionRampBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "road_transition_ramp")))
                    .sounds(BlockSoundGroup.STONE)
                    .hardness(2.0f)
                    .resistance(6.0f)
                    .requiresTool()),
            Text.literal("§7Smooth 0-to-8px slope connecting terrain to asphalt pavement."));

    public static final Block FUEL_REFINERY = registerBlockWithTooltip("fuel_refinery",
            new net.enchantedwood.block.custom.FuelRefineryBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "fuel_refinery")))
                    .sounds(BlockSoundGroup.METAL)
                    .hardness(4.0f)
                    .resistance(8.0f)
                    .requiresTool()
                    .luminance(state -> state.get(net.enchantedwood.block.custom.FuelRefineryBlock.LIT) ? 13 : 0)),
            Text.literal("§7Fractional distillation chamber. Consumes §e20 FE/t§7."),
            Text.literal("§8Distills Crude Sludge into Gasoline & crops into Biofuel."));

    public static final Block ROAD_PAVER = registerBlockWithTooltip("road_paver",
            new net.enchantedwood.block.custom.RoadPaverBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "road_paver")))
                    .sounds(BlockSoundGroup.METAL)
                    .hardness(4.0f)
                    .resistance(8.0f)
                    .requiresTool()
                    .luminance(state -> state.get(net.enchantedwood.block.custom.RoadPaverBlock.PAVING) ? 10 : 0)),
            Text.literal("§7Autonomous highway construction crawler. Consumes §e50 FE/step§7."),
            Text.literal("§8Clears a 3-wide path, lays Asphalt foundation, and advances forward."));

    public static final Block VEHICLE_FABRICATOR = registerBlockWithTooltip("vehicle_fabricator",
            new net.enchantedwood.block.custom.VehicleFabricatorBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "vehicle_fabricator")))
                    .sounds(BlockSoundGroup.METAL)
                    .hardness(4.0f)
                    .resistance(8.0f)
                    .requiresTool()),
            Text.literal("§7Vehicle workshop for fabricating & modifying Modular ATVs."),
            Text.literal("§8Assemble custom engines, chassis, tires, suspensions & trunks."));

    // Phase 2: Nether Factory & Tier 4 Power Grid
    public static final Block TUNGSTEN_BATTERY = registerBlockWithTooltip("tungsten_battery",
            new net.enchantedwood.block.custom.TungstenBatteryBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "tungsten_battery")))
                    .sounds(BlockSoundGroup.NETHERITE)
                    .hardness(6.0f)
                    .resistance(12.0f)
                    .requiresTool()),
            Text.literal("§6Tier 4 Heavy Energy Storage (100,000,000 FE)"),
            Text.literal("§8Max Transfer: §e25,000 FE/t§8. Refractory insulation."));

    public static final Block TUNGSTEN_CABLE = registerBlockWithTooltip("tungsten_cable",
            new net.enchantedwood.block.custom.TungstenCableBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "tungsten_cable")))
                    .sounds(BlockSoundGroup.NETHERITE)
                    .hardness(2.5f)
                    .resistance(8.0f)
                    .requiresTool()
                    .nonOpaque()),
            Text.literal("§6Tier 4 Heavy Energy Cable"),
            Text.literal("§8Max Transfer: §e25,000 FE/t§8. Lava & blast proof."));

    public static final Block GEOTHERMAL_GENERATOR = registerBlockWithTooltip("geothermal_generator",
            new net.enchantedwood.block.custom.GeothermalGeneratorBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "geothermal_generator")))
                    .sounds(BlockSoundGroup.NETHERITE)
                    .hardness(5.0f)
                    .resistance(10.0f)
                    .requiresTool()
                    .luminance(state -> state.get(net.enchantedwood.block.custom.GeothermalGeneratorBlock.LIT) ? 14 : 0)),
            Text.literal("§6Tier 4 Geothermal Thermal Generator"),
            Text.literal("§8Generates §e750 FE/t§8 from Lava and Nether heat sources."));

    public static final Block ALLOY_FOUNDRY = registerBlockWithTooltip("alloy_foundry",
            new net.enchantedwood.block.custom.AlloyFoundryBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "alloy_foundry")))
                    .sounds(BlockSoundGroup.NETHERITE)
                    .hardness(5.0f)
                    .resistance(10.0f)
                    .requiresTool()
                    .luminance(state -> state.get(net.enchantedwood.block.custom.AlloyFoundryBlock.LIT) ? 13 : 0)),
            Text.literal("§6Industrial Alloy Induction Foundry"),
            Text.literal("§8Dual-induction melting & casting for Manyullyn & Tungsten Carbide."));

    public static final Block ITEM_SALVAGER = registerBlockWithTooltip("item_salvager",
            new net.enchantedwood.block.custom.ItemSalvagerBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "item_salvager")))
                    .sounds(BlockSoundGroup.NETHERITE)
                    .hardness(5.0f)
                    .resistance(10.0f)
                    .requiresTool()
                    .luminance(state -> state.get(net.enchantedwood.block.custom.ItemSalvagerBlock.LIT) ? 13 : 0)),
            Text.literal("§6Automated Item Salvager & Recycler"),
            Text.literal("§8Deconstructs uncraftables, horse armor, chainmail, minecarts & rails."));

    public static final Block MAGMA_CRUCIBLE = registerBlockWithTooltip("magma_crucible",
            new net.enchantedwood.block.custom.MagmaCrucibleBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "magma_crucible")))
                    .sounds(BlockSoundGroup.NETHERITE)
                    .hardness(5.0f)
                    .resistance(10.0f)
                    .requiresTool()
                    .luminance(state -> state.get(net.enchantedwood.block.custom.MagmaCrucibleBlock.LIT) ? 13 : 0)),
            Text.literal("§6Magma Crucible & Mineral Extractor"),
            Text.literal("§8Melts Basalt, Blackstone & Magma into Lava, Sulfur & Volcanic Ash."));

    public static final Block LAVA_PUMP = registerBlockWithTooltip("lava_pump",
            new net.enchantedwood.block.custom.LavaPumpBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "lava_pump")))
                    .sounds(BlockSoundGroup.NETHERITE)
                    .hardness(5.0f)
                    .resistance(10.0f)
                    .requiresTool()
                    .luminance(state -> state.get(net.enchantedwood.block.custom.LavaPumpBlock.LIT) ? 12 : 0)),
            Text.literal("§6Submersible Thermal Lava Pump"),
            Text.literal("§8Pumps liquid lava from Nether seas into adjacent pipes and generators."));

    public static final Block CRUSHER_MK2 = registerBlockWithTooltip("crusher_mk2",
            new net.enchantedwood.block.custom.CrusherMk2Block(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "crusher_mk2")))
                    .sounds(BlockSoundGroup.NETHERITE)
                    .hardness(6.0f)
                    .resistance(12.0f)
                    .requiresTool()
                    .luminance(state -> state.get(net.enchantedwood.block.custom.CrusherMk2Block.LIT) ? 13 : 0)),
            Text.literal("§6Industrial Crusher MK2"),
            Text.literal("§83x-6x Ore Yield Multiplier + Secondary Mineral Byproducts."));

    public static final Block CORN_CROP = registerBlockWithoutItem("corn_crop",
            new net.enchantedwood.block.custom.CornCropBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "corn_crop")))
                    .noCollision()
                    .ticksRandomly()
                    .breakInstantly()
                    .sounds(BlockSoundGroup.CROP)
                    .pistonBehavior(net.minecraft.block.piston.PistonBehavior.DESTROY)
                    .nonOpaque()));

    public static final Block VOLCANIC_SOIL = registerBlockWithTooltip("volcanic_soil",
            new net.enchantedwood.block.custom.VolcanicSoilBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "volcanic_soil")))
                    .sounds(BlockSoundGroup.MUD)
                    .hardness(0.8f)
                    .resistance(0.8f)
                    .ticksRandomly()),
            Text.literal("§6Volcanic Mineral Soil"),
            Text.literal("§8Self-hydrating fertile soil that accelerates crop and sapling growth automatically."));

    public static final Block SOIL_INFUSER = registerBlockWithTooltip("soil_infuser",
            new net.enchantedwood.block.custom.SoilInfuserBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "soil_infuser")))
                    .sounds(BlockSoundGroup.NETHERITE)
                    .hardness(5.0f)
                    .resistance(10.0f)
                    .requiresTool()
                    .luminance(state -> state.get(net.enchantedwood.block.custom.SoilInfuserBlock.LIT) ? 11 : 0)),
            Text.literal("§6Volcanic Soil Infuser"),
            Text.literal("§8Synthesizes hyper-fertile Volcanic Mineral Soil from Dirt and Volcanic Ash."));

    public static final Block POZZOLANIC_ASPHALT = registerBlockWithTooltip("pozzolanic_asphalt",
            new Block(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "pozzolanic_asphalt")))
                    .sounds(BlockSoundGroup.STONE)
                    .hardness(2.0f)
                    .resistance(6.0f)
                    .requiresTool()),
            Text.literal("§6Pozzolanic Roman Asphalt"),
            Text.literal("§8Ultra-durable ancient Roman pavement made with volcanic ash."));

    public static final Block VOLCANIC_BRICKS = registerBlock("volcanic_bricks",
            new Block(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "volcanic_bricks")))
                    .sounds(BlockSoundGroup.STONE)
                    .hardness(2.5f)
                    .resistance(8.0f)
                    .requiresTool()));

    public static final Block VOLCANIC_BRICK_STAIRS = registerBlock("volcanic_brick_stairs",
            new StairsBlock(VOLCANIC_BRICKS.getDefaultState(), AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "volcanic_brick_stairs")))
                    .sounds(BlockSoundGroup.STONE)
                    .hardness(2.5f)
                    .resistance(8.0f)
                    .requiresTool()));

    public static final Block VOLCANIC_BRICK_SLAB = registerBlock("volcanic_brick_slab",
            new SlabBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "volcanic_brick_slab")))
                    .sounds(BlockSoundGroup.STONE)
                    .hardness(2.5f)
                    .resistance(8.0f)
                    .requiresTool()));

    // 5x5 Multiblock Titanium Lava Reservoir & Titanium Lava Pipes
    public static final Block TITANIUM_LAVA_PIPE = registerBlockWithTooltip("titanium_lava_pipe",
            new net.enchantedwood.block.custom.TitaniumLavaPipeBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "titanium_lava_pipe")))
                    .sounds(BlockSoundGroup.NETHERITE)
                    .hardness(2.0f)
                    .resistance(12.0f)
                    .requiresTool()
                    .nonOpaque()),
            Text.literal("§6Titanium Lava Pipe"),
            Text.literal("§8High-temp titanium alloy pipe (1,668°C rating). Transfers 500 mB/t."));

    public static final Block TITANIUM_TANK_CASING = registerBlockWithTooltip("titanium_tank_casing",
            new net.enchantedwood.block.custom.TitaniumTankCasingBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "titanium_tank_casing")))
                    .sounds(BlockSoundGroup.NETHERITE)
                    .hardness(5.0f)
                    .resistance(18.0f)
                    .requiresTool()),
            Text.literal("§6Titanium Tank Casing"),
            Text.literal("§85x5x5 Multiblock structural frame. Acts as Outbound Valve when formed."));

    public static final Block REINFORCED_TANK_GLASS = registerBlockWithTooltip("reinforced_tank_glass",
            new net.enchantedwood.block.custom.ReinforcedTankGlassBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "reinforced_tank_glass")))
                    .sounds(BlockSoundGroup.GLASS)
                    .hardness(3.0f)
                    .resistance(18.0f)
                    .requiresTool()
                    .nonOpaque()),
            Text.literal("§6Reinforced Tank Glass"),
            Text.literal("§8Pressure-treated quartz viewing glass. Drops itself when mined."));

    public static final Block TITANIUM_TANK_INBOUND_PORT = registerBlockWithTooltip("titanium_tank_inbound_port",
            new net.enchantedwood.block.custom.TitaniumTankInboundPortBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "titanium_tank_inbound_port")))
                    .sounds(BlockSoundGroup.NETHERITE)
                    .hardness(5.0f)
                    .resistance(18.0f)
                    .requiresTool()),
            Text.literal("§6Titanium Tank Inbound Port"),
            Text.literal("§8Top-center 5x5 Multiblock Valve. Inbound lava pipes connect here."));

    // Universal Item Logistics System
    public static final Block ITEM_PIPE = registerBlockWithTooltip("item_pipe",
            new net.enchantedwood.block.custom.ItemPipeBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "item_pipe")))
                    .sounds(BlockSoundGroup.METAL)
                    .hardness(1.5f)
                    .resistance(6.0f)
                    .requiresTool()
                    .nonOpaque()),
            Text.literal("§6Item Transport Pipe"),
            Text.literal("§8Modular 6-way item conduit for routing items between extractors and inserters."));

    public static final Block ITEM_EXTRACTOR = registerBlockWithTooltip("item_extractor",
            new net.enchantedwood.block.custom.ItemExtractorBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "item_extractor")))
                    .sounds(BlockSoundGroup.METAL)
                    .hardness(2.0f)
                    .resistance(6.0f)
                    .requiresTool()
                    .nonOpaque()),
            Text.literal("§6Item Extractor"),
            Text.literal("§8Actively pulls items from machine outputs and chests into the pipe network."));

    public static final Block ITEM_INSERTER = registerBlockWithTooltip("item_inserter",
            new net.enchantedwood.block.custom.ItemInserterBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "item_inserter")))
                    .sounds(BlockSoundGroup.METAL)
                    .hardness(2.0f)
                    .resistance(6.0f)
                    .requiresTool()
                    .nonOpaque()),
            Text.literal("§6Item Inserter"),
            Text.literal("§8Actively injects routed items from the pipe network into target containers and machines."));

    // Nether Metallurgy & Heavy Infrastructure
    public static final Block BASALT_CABLE = registerBlockWithTooltip("basalt_cable",
            new net.enchantedwood.block.custom.BasaltCableBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "basalt_cable")))
                    .sounds(BlockSoundGroup.BASALT)
                    .hardness(3.0f)
                    .resistance(1200.0f)
                    .nonOpaque()),
            Text.literal("§6Basalt-Insulated Super Cable"),
            Text.literal("§e25,600 E/t Transfer §7• 100% Explosion-Proof & Fireproof."));

    public static final Block REINFORCED_OBSIDIAN = registerBlockWithTooltip("reinforced_obsidian",
            new Block(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "reinforced_obsidian")))
                    .sounds(BlockSoundGroup.STONE)
                    .hardness(50.0f)
                    .resistance(1200.0f)
                    .requiresTool()),
            Text.literal("§6Reinforced Obsidian"),
            Text.literal("§8Wither-proof and immune to all explosions."));

    public static final Block VOLCANIC_GLASS = registerBlockWithTooltip("volcanic_glass",
            new net.minecraft.block.TransparentBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, "volcanic_glass")))
                    .sounds(BlockSoundGroup.GLASS)
                    .hardness(2.5f)
                    .resistance(600.0f)
                    .nonOpaque()
                    .allowsSpawning((state, world, pos, type) -> false)
                    .solidBlock((state, world, pos) -> false)
                    .suffocates((state, world, pos) -> false)
                    .blockVision((state, world, pos) -> false)),
            Text.literal("§6Tough Volcanic Glass"),
            Text.literal("§8Blast-resistant crystal glass. Drops itself when mined."));

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

    private static Block registerBlockWithTooltip(String name, Block block, Text... tooltips) {
        RegistryKey<Block> blockKey = RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(EnchantedWoodMod.MOD_ID, name));
        RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(EnchantedWoodMod.MOD_ID, name));

        Block registeredBlock = Registry.register(Registries.BLOCK, blockKey, block);
        Registry.register(Registries.ITEM, itemKey, new net.enchantedwood.item.custom.TooltipBlockItem(registeredBlock, new Item.Settings().registryKey(itemKey).useBlockPrefixedTranslationKey(), tooltips));

        return registeredBlock;
    }

    public static void registerModBlocks() {
        EnchantedWoodMod.LOGGER.info("Registering Enchanted Wood Blocks for " + EnchantedWoodMod.MOD_ID);
    }
}
