package net.enchantedwood.item;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.*;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

import net.minecraft.util.Identifier;
import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.item.custom.BarkskinPickaxeItem;
import net.enchantedwood.item.custom.EnchantedArmorItem;
import net.enchantedwood.item.custom.EnchantedCobblestoneArmorItem;
import net.enchantedwood.item.custom.EnchantedCobblestonePickaxeItem;
import net.enchantedwood.item.custom.EnchantedCobblestoneSwordItem;
import net.enchantedwood.item.custom.HammerItem;
import net.enchantedwood.item.custom.LivingwoodSwordItem;

import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.item.custom.EnchantedRedstoneItem;
import net.enchantedwood.item.custom.GearItem;

import net.minecraft.component.type.FoodComponent;
import java.util.function.Function;

public class ModItems {

    // Materials
    public static final Item INFUSED_HEARTWOOD = registerItem("infused_heartwood", Item::new);
    public static final Item ENCHANTED_DUST = registerItem("enchanted_dust", Item::new);
    public static final Item ENCHANTED_WOOD = registerItem("enchanted_wood", Item::new);
    public static final Item ENCHANTED_COAL = registerItem("enchanted_coal", Item::new);
    public static final Item ENCHANTED_REDSTONE = registerItem("enchanted_redstone", EnchantedRedstoneItem::new);
    public static final Item ENCHANTED_EMERALD = registerItem("enchanted_emerald", net.enchantedwood.item.custom.EnchantedEmeraldItem::new);
    public static final Item ENCHANTED_CAPE = registerItem("enchanted_cape", settings -> new net.enchantedwood.item.custom.EnchantedCapeItem(settings.maxCount(1)));
    public static final Item RESIN = registerItem("resin", Item::new);
    public static final Item RUBBER = registerItem("rubber", Item::new);

    // Agriculture & Crops
    public static final Item CORN = registerItem("corn", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(new FoodComponent.Builder().nutrition(3).saturationModifier(0.6f).build()),
            Text.literal("§7Can be §eRoasted §7on fire for food, or distilled in a"),
            Text.literal("§eFuel Refinery §7(§62 Corn + Empty Canister§7) into §aBiofuel§7.")
    ));
    public static final Item ROASTED_CORN = registerItem("roasted_corn", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.food(new FoodComponent.Builder().nutrition(7).saturationModifier(0.8f).build()),
            Text.literal("§aDelicious roasted sweet corn. Restores 7 food points.")
    ));
    public static final Item CORN_SEEDS = registerItem("corn_seeds", settings -> new net.enchantedwood.item.custom.TooltipBlockItem(
            net.enchantedwood.block.ModBlocks.CORN_CROP,
            settings,
            Text.literal("§7Plant on tilled farmland to grow 8-stage Sweet Corn."),
            Text.literal("§8Obtained by breaking wild grass or crafting with Enchanted Dust.")
    ));

    // Petrochemicals & Fuels
    public static final Item CRUDE_OIL_SLUDGE = registerItem("crude_oil_sludge", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§7Distill in a §eFuel Refinery §7with an §fEmpty Gas Canister§7."),
            Text.literal("§8Outputs: §6Gasoline Canister §8+ §8Mineral Tar §8byproduct.")
    ));
    public static final Item MINERAL_TAR = registerItem("mineral_tar", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§7Petrochemical byproduct used to synthesize §8Asphalt Blocks§7."),
            Text.literal("§8Craft with Cobblestone/Deepslate + Gravel.")
    ));
    public static final Item BIOFUEL_CANISTER = registerItem("biofuel_canister", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.maxCount(16),
            Text.literal("§aEco-Friendly Ethanol Fuel §7for §eATV Engines§7."),
            Text.literal("§8Synthesized in Fuel Refinery from Corn, Wheat, or Potatoes.")
    ));
    public static final Item GASOLINE_CANISTER = registerItem("gasoline_canister", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.maxCount(16),
            Text.literal("§6Refined Hydrocarbon Fuel §7for §eATV Engines§7."),
            Text.literal("§8Combine with 2 Corn in Fuel Refinery for §dHigh-Octane Racing Fuel§8.")
    ));
    public static final Item HIGH_OCTANE_FUEL_CANISTER = registerItem("high_octane_fuel_canister", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings.maxCount(16),
            Text.literal("§dPremium Racing Fuel §7providing maximum acceleration & top speed."),
            Text.literal("§8Required for Titanium Twin-Turbo ATV Engines.")
    ));

    // Modular All-Terrain Vehicle (ATV) & Components
    public static final Item ATV_ITEM = registerItem("atv", settings -> new net.enchantedwood.item.custom.AtvItem(settings.maxCount(1)));
    public static final Item ATV_SEAT = registerItem("atv_seat", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§7Core component for assembling an All-Terrain Vehicle."),
            Text.literal("§8Craft with Leather + Black Wool + Iron Ingot.")
    ));
    public static final Item RUBBER_TIRE = registerItem("rubber_tire", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§7Standard vulcanized rubber tire with balanced all-terrain grip."),
            Text.literal("§8Craft with 4 Rubber around 1 Iron Ingot.")
    ));
    public static final Item STEEL_RIM_TIRE = registerItem("steel_rim_tire", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§7Reinforced steel rim tire with improved highway stability."),
            Text.literal("§8Craft with Rubber Tire + Steel Ingot.")
    ));
    public static final Item TITANIUM_STUDDED_TIRE = registerItem("titanium_studded_tire", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§bStudded ice-grip tire for maximum traction on snow & ice."),
            Text.literal("§8Craft with Rubber Tire + Titanium Ingot.")
    ));
    public static final Item COPPER_ATV_ENGINE = registerItem("copper_atv_engine", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§eStarter Engine §8(~25 km/h) §7• Fuel: Biofuel / Gasoline"),
            Text.literal("§8Craft with Copper Ingots, Piston, Copper Gear, and Redstone.")
    ));
    public static final Item ALUMINUM_ATV_ENGINE = registerItem("aluminum_atv_engine", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§bAgile V4 Engine §8(~40 km/h) §7• Fuel: Biofuel / Gasoline"),
            Text.literal("§8Craft with Aluminum Ingots, Piston, Aluminum Gear, and Redstone.")
    ));
    public static final Item STEEL_ATV_ENGINE = registerItem("steel_atv_engine", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§7High-Torque V8 Engine §8(~55 km/h) §7• Fuel: Gasoline / High-Octane"),
            Text.literal("§8Craft with Steel Ingots, Piston, Steel Gear, and Enchanted Redstone.")
    ));
    public static final Item TITANIUM_ATV_ENGINE = registerItem("titanium_atv_engine", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§dTwin-Turbo Nitro Engine §8(~80 km/h) §7• Fuel: High-Octane Racing Fuel"),
            Text.literal("§8Craft with Titanium Ingots, Piston, Enchanted Steel Gear, and Enchanted Redstone.")
    ));
    public static final Item STEEL_SUSPENSION = registerItem("steel_suspension", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§7Shock absorbers providing §e1.0-block step-up §7& fall absorption."),
            Text.literal("§8Craft with Steel Ingots + Iron Bars.")
    ));
    public static final Item TITANIUM_SUSPENSION = registerItem("titanium_suspension", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§7Heavy-duty suspension providing §e1.5-block step-up §7& full fall negation."),
            Text.literal("§8Craft with Titanium Ingots + Iron Bars.")
    ));
    public static final Item ALUMINUM_ATV_CHASSIS = registerItem("aluminum_atv_chassis", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§bLightweight racing chassis frame for agile handling."),
            Text.literal("§8Craft with 7 Aluminum Ingots in an H-shape.")
    ));
    public static final Item STEEL_ATV_CHASSIS = registerItem("steel_atv_chassis", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§7Reinforced steel chassis offering balanced structural durability."),
            Text.literal("§8Craft with 7 Steel Ingots in an H-shape.")
    ));
    public static final Item TITANIUM_ATV_CHASSIS = registerItem("titanium_atv_chassis", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§dHeavy hazard-shielded chassis built for dimensional exploration."),
            Text.literal("§8Craft with 7 Titanium Ingots in an H-shape.")
    ));
    public static final Item SMALL_CARGO_TRUNK = registerItem("small_cargo_trunk", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§7Adds §e9 inventory slots §7to the ATV rear cargo rack."),
            Text.literal("§8Craft with Iron Ingots around a Chest.")
    ));
    public static final Item MEDIUM_CARGO_TRUNK = registerItem("medium_cargo_trunk", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§7Adds §e18 inventory slots §7to the ATV rear cargo rack."),
            Text.literal("§8Craft with Steel Ingots around a Small Cargo Trunk.")
    ));
    public static final Item LARGE_CARGO_TRUNK = registerItem("large_cargo_trunk", settings -> new net.enchantedwood.item.custom.TooltipItem(
            settings,
            Text.literal("§7Adds §e27 inventory slots §7to the ATV rear cargo rack."),
            Text.literal("§8Craft with Titanium Ingots around a Medium Cargo Trunk.")
    ));

    // Base Gears
    public static final Item IRON_GEAR = registerItem("iron_gear", settings -> new GearItem(GearTier.IRON, false, settings));
    public static final Item COPPER_GEAR = registerItem("copper_gear", settings -> new GearItem(GearTier.COPPER, false, settings));
    public static final Item BRONZE_GEAR = registerItem("bronze_gear", settings -> new GearItem(GearTier.BRONZE, false, settings));
    public static final Item GOLD_GEAR = registerItem("gold_gear", settings -> new GearItem(GearTier.GOLD, false, settings));
    public static final Item DIAMOND_GEAR = registerItem("diamond_gear", settings -> new GearItem(GearTier.DIAMOND, false, settings));
    public static final Item NETHERITE_GEAR = registerItem("netherite_gear", settings -> new GearItem(GearTier.NETHERITE, false, settings));

    // Enchanted Gears
    public static final Item ENCHANTED_IRON_GEAR = registerItem("enchanted_iron_gear", settings -> new GearItem(GearTier.IRON, true, settings));
    public static final Item ENCHANTED_COPPER_GEAR = registerItem("enchanted_copper_gear", settings -> new GearItem(GearTier.COPPER, true, settings));
    public static final Item ENCHANTED_BRONZE_GEAR = registerItem("enchanted_bronze_gear", settings -> new GearItem(GearTier.BRONZE, true, settings));
    public static final Item ENCHANTED_GOLD_GEAR = registerItem("enchanted_gold_gear", settings -> new GearItem(GearTier.GOLD, true, settings));
    public static final Item ENCHANTED_DIAMOND_GEAR = registerItem("enchanted_diamond_gear", settings -> new GearItem(GearTier.DIAMOND, true, settings));
    public static final Item ENCHANTED_NETHERITE_GEAR = registerItem("enchanted_netherite_gear", settings -> new GearItem(GearTier.NETHERITE, true, settings));

    // Raw Ores & Materials
    public static final Item COPPER_BUCKET = registerItem("copper_bucket", settings -> new net.enchantedwood.item.custom.CopperBucketItem(net.minecraft.fluid.Fluids.EMPTY, settings.maxCount(16)));
    public static final Item COPPER_WATER_BUCKET = registerItem("copper_water_bucket", settings -> new net.enchantedwood.item.custom.CopperBucketItem(net.minecraft.fluid.Fluids.WATER, settings.maxCount(1).recipeRemainder(COPPER_BUCKET)));
    public static final Item COPPER_LAVA_BUCKET = registerItem("copper_lava_bucket", settings -> new net.enchantedwood.item.custom.CopperBucketItem(net.minecraft.fluid.Fluids.LAVA, settings.maxCount(1).recipeRemainder(COPPER_BUCKET)));
    public static final Item ENCHANTED_LAVA_BUCKET = registerItem("enchanted_lava_bucket", settings -> new net.enchantedwood.item.custom.CopperBucketItem(net.minecraft.fluid.Fluids.LAVA, settings.maxCount(1).recipeRemainder(Items.BUCKET)));
    public static final Item ENCHANTED_COPPER_LAVA_BUCKET = registerItem("enchanted_copper_lava_bucket", settings -> new net.enchantedwood.item.custom.CopperBucketItem(net.minecraft.fluid.Fluids.LAVA, settings.maxCount(1).recipeRemainder(COPPER_BUCKET)));
    public static final Item RAW_TIN = registerItem("raw_tin", Item::new);
    public static final Item TIN_INGOT = registerItem("tin_ingot", Item::new);
    public static final Item BRONZE_INGOT = registerItem("bronze_ingot", Item::new);
    public static final Item RAW_TITANIUM = registerItem("raw_titanium", Item::new);
    public static final Item TITANIUM_INGOT = registerItem("titanium_ingot", Item::new);
    public static final Item TITANIUM_ROLLER = registerItem("titanium_roller", Item::new);
    public static final Item ENCHANTED_DIAMOND = registerItem("enchanted_diamond", Item::new);
    public static final Item ENCHANTED_NETHERITE_INGOT = registerItem("enchanted_netherite_ingot",
            settings -> new Item(settings.fireproof()) {
                @Override public boolean hasGlint(ItemStack stack) { return true; }
            });

    // Enchanted Health Upgrades (Heart Lockets)
    public static final Item ENCHANTED_HEART = registerItem("enchanted_heart", settings -> new net.enchantedwood.item.custom.EnchantedHeartItem(2.0f, settings.maxCount(1)));
    public static final Item IRON_ENCHANTED_HEART = registerItem("iron_enchanted_heart", settings -> new net.enchantedwood.item.custom.EnchantedHeartItem(6.0f, settings.maxCount(1)));
    public static final Item GOLD_ENCHANTED_HEART = registerItem("gold_enchanted_heart", settings -> new net.enchantedwood.item.custom.EnchantedHeartItem(10.0f, settings.maxCount(1)));
    public static final Item DIAMOND_ENCHANTED_HEART = registerItem("diamond_enchanted_heart", settings -> new net.enchantedwood.item.custom.EnchantedHeartItem(14.0f, settings.maxCount(1)));
    public static final Item NETHERITE_ENCHANTED_HEART = registerItem("netherite_enchanted_heart", settings -> new net.enchantedwood.item.custom.EnchantedHeartItem(20.0f, settings.maxCount(1).fireproof()));

    // Storage Crystals & Wireless Access
    public static final Item STORAGE_CRYSTAL_1K = registerItem("storage_crystal_1k", settings -> new net.enchantedwood.item.custom.StorageCrystalItem(1000, settings.maxCount(1)));
    public static final Item STORAGE_CRYSTAL_4K = registerItem("storage_crystal_4k", settings -> new net.enchantedwood.item.custom.StorageCrystalItem(4000, settings.maxCount(1)));
    public static final Item STORAGE_CRYSTAL_16K = registerItem("storage_crystal_16k", settings -> new net.enchantedwood.item.custom.StorageCrystalItem(16000, settings.maxCount(1)));
    public static final Item STORAGE_CRYSTAL_64K = registerItem("storage_crystal_64k", settings -> new net.enchantedwood.item.custom.StorageCrystalItem(64000, settings.maxCount(1)));
    public static final Item WIRELESS_STORAGE_CRYSTAL = registerItem("wireless_storage_crystal", settings -> new net.enchantedwood.item.custom.WirelessStorageCrystalItem(settings.maxCount(1)));
    public static final Item CHUNK_LOADER_MODULE = registerItem("chunk_loader_module", settings -> new Item(settings.maxCount(1)));
    public static final Item INTERDIMENSIONAL_CARD = registerItem("interdimensional_card", settings -> new Item(settings.maxCount(1)));

    // Ore Dusts
    public static final Item IRON_DUST = registerItem("iron_dust", Item::new);
    public static final Item COPPER_DUST = registerItem("copper_dust", Item::new);
    public static final Item TIN_DUST = registerItem("tin_dust", Item::new);
    public static final Item BRONZE_DUST = registerItem("bronze_dust", Item::new);
    public static final Item TITANIUM_DUST = registerItem("titanium_dust", Item::new);
    public static final Item BAUXITE_DUST = registerItem("bauxite_dust", Item::new);
    public static final Item GOLD_DUST = registerItem("gold_dust", Item::new);
    public static final Item DIAMOND_DUST = registerItem("diamond_dust", Item::new);
    public static final Item NETHERITE_DUST = registerItem("netherite_dust", Item::new);
    public static final Item EMERALD_DUST = registerItem("emerald_dust", Item::new);
    public static final Item COAL_DUST = registerItem("coal_dust", Item::new);

    // Phase 2: Metallurgy & Gas Items
    public static final Item RAW_BAUXITE = registerItem("raw_bauxite", Item::new);
    public static final Item ALUMINUM_INGOT = registerItem("aluminum_ingot", Item::new);
    public static final Item ALUMINUM_GEAR = registerItem("aluminum_gear", settings -> new GearItem(GearTier.ALUMINUM, false, settings));
    public static final Item ENCHANTED_ALUMINUM_GEAR = registerItem("enchanted_aluminum_gear", settings -> new GearItem(GearTier.ALUMINUM, true, settings));
    public static final Item EMPTY_GAS_CANISTER = registerItem("empty_gas_canister", settings -> new Item(settings.maxCount(16)));
    public static final Item OXYGEN_CANISTER = registerItem("oxygen_canister", settings -> new Item(settings.maxCount(16)));
    public static final Item HYDROGEN_CANISTER = registerItem("hydrogen_canister", settings -> new Item(settings.maxCount(16)));
    public static final Item HYDROGEN_JETPACK = registerItem("hydrogen_jetpack", settings -> new net.enchantedwood.item.custom.HydrogenJetpackItem(settings.armor(ModArmorMaterials.ALUMINUM, EquipmentType.CHESTPLATE)));
    public static final Item OXY_HYDROGEN_TORCH = registerItem("oxy_hydrogen_torch", settings -> new net.enchantedwood.item.custom.OxyHydrogenTorchItem(settings));

    // Phase 3: Coke Coal & Steel Metallurgy
    public static final Item COKE_COAL = registerItem("coke_coal", Item::new);
    public static final Item STEEL_INGOT = registerItem("steel_ingot", Item::new);
    public static final Item STEEL_DUST = registerItem("steel_dust", Item::new);
    public static final Item STEEL_GEAR = registerItem("steel_gear", settings -> new GearItem(GearTier.STEEL, false, settings));
    public static final Item ENCHANTED_STEEL_GEAR = registerItem("enchanted_steel_gear", settings -> new GearItem(GearTier.STEEL, true, settings));

    // Steel Tools & Weapons
    public static final Item STEEL_SWORD = registerItem("steel_sword", settings -> new Item(settings.sword(ModMaterials.STEEL, 3.5f, -2.4f)));
    public static final Item STEEL_PICKAXE = registerItem("steel_pickaxe", settings -> new Item(settings.pickaxe(ModMaterials.STEEL, 1.5f, -2.8f)));
    public static final Item STEEL_AXE = registerItem("steel_axe", settings -> new Item(settings.axe(ModMaterials.STEEL, 6.5f, -3.0f)));
    public static final Item STEEL_SHOVEL = registerItem("steel_shovel", settings -> new Item(settings.shovel(ModMaterials.STEEL, 2.0f, -3.0f)));
    public static final Item STEEL_HOE = registerItem("steel_hoe", settings -> new Item(settings.hoe(ModMaterials.STEEL, -2.0f, -1.0f)));
    public static final Item STEEL_HAMMER = registerItem("steel_hammer", settings -> new HammerItem(settings.pickaxe(ModMaterials.STEEL, 5.0f, -3.0f)));

    // Steel Armor
    public static final Item STEEL_HELMET = registerItem("steel_helmet", settings -> new Item(settings.armor(ModArmorMaterials.STEEL, EquipmentType.HELMET)));
    public static final Item STEEL_CHESTPLATE = registerItem("steel_chestplate", settings -> new Item(settings.armor(ModArmorMaterials.STEEL, EquipmentType.CHESTPLATE)));
    public static final Item STEEL_LEGGINGS = registerItem("steel_leggings", settings -> new Item(settings.armor(ModArmorMaterials.STEEL, EquipmentType.LEGGINGS)));
    public static final Item STEEL_BOOTS = registerItem("steel_boots", settings -> new Item(settings.armor(ModArmorMaterials.STEEL, EquipmentType.BOOTS)));

    // Aluminum Tools & Weapons
    public static final Item ALUMINUM_SWORD = registerItem("aluminum_sword", settings -> new Item(settings.sword(ModMaterials.ALUMINUM, 3.0f, -2.4f)));
    public static final Item ALUMINUM_PICKAXE = registerItem("aluminum_pickaxe", settings -> new Item(settings.pickaxe(ModMaterials.ALUMINUM, 1.0f, -2.8f)));
    public static final Item ALUMINUM_AXE = registerItem("aluminum_axe", settings -> new Item(settings.axe(ModMaterials.ALUMINUM, 6.0f, -3.1f)));
    public static final Item ALUMINUM_SHOVEL = registerItem("aluminum_shovel", settings -> new Item(settings.shovel(ModMaterials.ALUMINUM, 1.5f, -3.0f)));
    public static final Item ALUMINUM_HOE = registerItem("aluminum_hoe", settings -> new Item(settings.hoe(ModMaterials.ALUMINUM, -2.0f, -1.0f)));
    public static final Item ALUMINUM_HAMMER = registerItem("aluminum_hammer", settings -> new HammerItem(settings.pickaxe(ModMaterials.ALUMINUM, 4.0f, -3.0f)));

    // Aluminum Armor
    public static final Item ALUMINUM_HELMET = registerItem("aluminum_helmet", settings -> new Item(settings.armor(ModArmorMaterials.ALUMINUM, EquipmentType.HELMET)));
    public static final Item ALUMINUM_CHESTPLATE = registerItem("aluminum_chestplate", settings -> new Item(settings.armor(ModArmorMaterials.ALUMINUM, EquipmentType.CHESTPLATE)));
    public static final Item ALUMINUM_LEGGINGS = registerItem("aluminum_leggings", settings -> new Item(settings.armor(ModArmorMaterials.ALUMINUM, EquipmentType.LEGGINGS)));
    public static final Item ALUMINUM_BOOTS = registerItem("aluminum_boots", settings -> new Item(settings.armor(ModArmorMaterials.ALUMINUM, EquipmentType.BOOTS)));

    // Bronze Tools & Weapons
    public static final Item BRONZE_SWORD = registerItem("bronze_sword", settings -> new Item(settings.sword(ModMaterials.BRONZE, 3.0f, -2.4f)));
    public static final Item BRONZE_PICKAXE = registerItem("bronze_pickaxe", settings -> new Item(settings.pickaxe(ModMaterials.BRONZE, 1.0f, -2.8f)));
    public static final Item BRONZE_AXE = registerItem("bronze_axe", settings -> new Item(settings.axe(ModMaterials.BRONZE, 6.0f, -3.1f)));
    public static final Item BRONZE_SHOVEL = registerItem("bronze_shovel", settings -> new Item(settings.shovel(ModMaterials.BRONZE, 1.5f, -3.0f)));
    public static final Item BRONZE_HOE = registerItem("bronze_hoe", settings -> new Item(settings.hoe(ModMaterials.BRONZE, -2.0f, -1.0f)));

    // Bronze Armor
    public static final Item BRONZE_HELMET = registerItem("bronze_helmet", settings -> new Item(settings.armor(ModArmorMaterials.BRONZE, EquipmentType.HELMET)));
    public static final Item BRONZE_CHESTPLATE = registerItem("bronze_chestplate", settings -> new Item(settings.armor(ModArmorMaterials.BRONZE, EquipmentType.CHESTPLATE)));
    public static final Item BRONZE_LEGGINGS = registerItem("bronze_leggings", settings -> new Item(settings.armor(ModArmorMaterials.BRONZE, EquipmentType.LEGGINGS)));
    public static final Item BRONZE_BOOTS = registerItem("bronze_boots", settings -> new Item(settings.armor(ModArmorMaterials.BRONZE, EquipmentType.BOOTS)));

    // Tin Tools & Weapons
    public static final Item TIN_SWORD = registerItem("tin_sword", settings -> new Item(settings.sword(ModMaterials.TIN, 2.5f, -2.4f)));
    public static final Item TIN_PICKAXE = registerItem("tin_pickaxe", settings -> new Item(settings.pickaxe(ModMaterials.TIN, 1.0f, -2.8f)));
    public static final Item TIN_AXE = registerItem("tin_axe", settings -> new Item(settings.axe(ModMaterials.TIN, 5.5f, -3.2f)));
    public static final Item TIN_SHOVEL = registerItem("tin_shovel", settings -> new Item(settings.shovel(ModMaterials.TIN, 1.0f, -3.0f)));
    public static final Item TIN_HOE = registerItem("tin_hoe", settings -> new Item(settings.hoe(ModMaterials.TIN, -2.0f, -1.0f)));

    // Tin Armor
    public static final Item TIN_HELMET = registerItem("tin_helmet", settings -> new Item(settings.armor(ModArmorMaterials.TIN, EquipmentType.HELMET)));
    public static final Item TIN_CHESTPLATE = registerItem("tin_chestplate", settings -> new Item(settings.armor(ModArmorMaterials.TIN, EquipmentType.CHESTPLATE)));
    public static final Item TIN_LEGGINGS = registerItem("tin_leggings", settings -> new Item(settings.armor(ModArmorMaterials.TIN, EquipmentType.LEGGINGS)));
    public static final Item TIN_BOOTS = registerItem("tin_boots", settings -> new Item(settings.armor(ModArmorMaterials.TIN, EquipmentType.BOOTS)));

    // Titanium Tools & Weapons
    public static final Item TITANIUM_SWORD = registerItem("titanium_sword", settings -> new Item(settings.sword(ModMaterials.TITANIUM, 4.0f, -2.4f)));
    public static final Item TITANIUM_PICKAXE = registerItem("titanium_pickaxe", settings -> new Item(settings.pickaxe(ModMaterials.TITANIUM, 2.0f, -2.8f)));
    public static final Item TITANIUM_AXE = registerItem("titanium_axe", settings -> new Item(settings.axe(ModMaterials.TITANIUM, 7.0f, -3.0f)));
    public static final Item TITANIUM_SHOVEL = registerItem("titanium_shovel", settings -> new Item(settings.shovel(ModMaterials.TITANIUM, 2.5f, -3.0f)));
    public static final Item TITANIUM_HOE = registerItem("titanium_hoe", settings -> new Item(settings.hoe(ModMaterials.TITANIUM, -1.0f, 0.0f)));

    // Titanium Armor
    public static final Item TITANIUM_HELMET = registerItem("titanium_helmet", settings -> new Item(settings.armor(ModArmorMaterials.TITANIUM, EquipmentType.HELMET)));
    public static final Item TITANIUM_CHESTPLATE = registerItem("titanium_chestplate", settings -> new Item(settings.armor(ModArmorMaterials.TITANIUM, EquipmentType.CHESTPLATE)));
    public static final Item TITANIUM_LEGGINGS = registerItem("titanium_leggings", settings -> new Item(settings.armor(ModArmorMaterials.TITANIUM, EquipmentType.LEGGINGS)));
    public static final Item TITANIUM_BOOTS = registerItem("titanium_boots", settings -> new Item(settings.armor(ModArmorMaterials.TITANIUM, EquipmentType.BOOTS)));



    // Tools & Weapons
    public static final Item LIVINGWOOD_SWORD = registerItem("livingwood_sword",
            settings -> new LivingwoodSwordItem(settings.sword(ModMaterials.ENCHANTED_WOOD, 3.0f, -2.4f)));

    public static final Item BARKSKIN_PICKAXE = registerItem("barkskin_pickaxe",
            settings -> new BarkskinPickaxeItem(settings.pickaxe(ModMaterials.ENCHANTED_WOOD, 1.0f, -2.8f)));

    public static final Item IRONWOOD_AXE = registerItem("ironwood_axe",
            settings -> new AxeItem(ModMaterials.ENCHANTED_WOOD, 6.0f, -3.0f, settings) {
                @Override public boolean hasGlint(ItemStack stack) { return true; }
            });

    public static final Item VERDANT_SHOVEL = registerItem("verdant_shovel",
            settings -> new ShovelItem(ModMaterials.ENCHANTED_WOOD, 1.5f, -3.0f, settings) {
                @Override public boolean hasGlint(ItemStack stack) { return true; }
            });

    public static final Item WOODEN_SHEARS = registerItem("wooden_shears", settings -> new net.enchantedwood.item.custom.WoodenShearsItem(settings.maxDamage(30)));

    public static final Item ELDERWOOD_HOE = registerItem("elderwood_hoe",
            settings -> new net.enchantedwood.item.custom.AutoHarvestHoeItem(ModMaterials.ENCHANTED_WOOD, 0.0f, -1.0f, settings));

    // Armor Set
    public static final Item ENCHANTED_WOOD_HELMET = registerItem("enchanted_wood_helmet",
            settings -> new EnchantedArmorItem(settings.armor(ModArmorMaterials.ENCHANTED_WOOD, EquipmentType.HELMET)));

    public static final Item ENCHANTED_WOOD_CHESTPLATE = registerItem("enchanted_wood_chestplate",
            settings -> new EnchantedArmorItem(settings.armor(ModArmorMaterials.ENCHANTED_WOOD, EquipmentType.CHESTPLATE)));

    public static final Item ENCHANTED_WOOD_LEGGINGS = registerItem("enchanted_wood_leggings",
            settings -> new EnchantedArmorItem(settings.armor(ModArmorMaterials.ENCHANTED_WOOD, EquipmentType.LEGGINGS)));

    public static final Item ENCHANTED_WOOD_BOOTS = registerItem("enchanted_wood_boots",
            settings -> new EnchantedArmorItem(settings.armor(ModArmorMaterials.ENCHANTED_WOOD, EquipmentType.BOOTS)));

    // Enchanted Cobblestone Tools & Weapons
    public static final Item ENCHANTED_COBBLESTONE_SWORD = registerItem("enchanted_cobblestone_sword",
            settings -> new EnchantedCobblestoneSwordItem(settings.sword(ModMaterials.ENCHANTED_COBBLESTONE, 3.0f, -2.4f)));

    public static final Item ENCHANTED_COBBLESTONE_PICKAXE = registerItem("enchanted_cobblestone_pickaxe",
            settings -> new EnchantedCobblestonePickaxeItem(settings.pickaxe(ModMaterials.ENCHANTED_COBBLESTONE, 1.0f, -2.8f)));

    public static final Item ENCHANTED_COBBLESTONE_AXE = registerItem("enchanted_cobblestone_axe",
            settings -> new AxeItem(ModMaterials.ENCHANTED_COBBLESTONE, 6.0f, -3.1f, settings) {
                @Override public boolean hasGlint(ItemStack stack) { return true; }
            });

    public static final Item ENCHANTED_COBBLESTONE_SHOVEL = registerItem("enchanted_cobblestone_shovel",
            settings -> new ShovelItem(ModMaterials.ENCHANTED_COBBLESTONE, 1.5f, -3.0f, settings) {
                @Override public boolean hasGlint(ItemStack stack) { return true; }
            });

    public static final Item ENCHANTED_COBBLESTONE_HOE = registerItem("enchanted_cobblestone_hoe",
            settings -> new net.enchantedwood.item.custom.AutoHarvestHoeItem(ModMaterials.ENCHANTED_COBBLESTONE, -1.0f, -1.0f, settings));

    // Base 3x3 Mining Sledgehammers
    public static final Item WOODEN_HAMMER = registerItem("wooden_hammer",
            settings -> new HammerItem(settings.pickaxe(ToolMaterial.WOOD, 2.0f, -3.2f)));
    public static final Item STONE_HAMMER = registerItem("stone_hammer",
            settings -> new HammerItem(settings.pickaxe(ToolMaterial.STONE, 3.0f, -3.2f)));
    public static final Item COPPER_HAMMER = registerItem("copper_hammer",
            settings -> new HammerItem(settings.pickaxe(ModMaterials.COPPER, 3.5f, -3.1f)));
    public static final Item IRON_HAMMER = registerItem("iron_hammer",
            settings -> new HammerItem(settings.pickaxe(ToolMaterial.IRON, 4.0f, -3.0f)));
    public static final Item BRONZE_HAMMER = registerItem("bronze_hammer",
            settings -> new HammerItem(settings.pickaxe(ModMaterials.BRONZE, 4.5f, -3.0f)));
    public static final Item GOLDEN_HAMMER = registerItem("golden_hammer",
            settings -> new HammerItem(settings.pickaxe(ToolMaterial.GOLD, 2.0f, -2.8f)));
    public static final Item DIAMOND_HAMMER = registerItem("diamond_hammer",
            settings -> new HammerItem(settings.pickaxe(ToolMaterial.DIAMOND, 5.0f, -3.0f)));
    public static final Item NETHERITE_HAMMER = registerItem("netherite_hammer",
            settings -> new HammerItem(settings.pickaxe(ToolMaterial.NETHERITE, 6.0f, -2.8f).fireproof()));
    public static final Item TITANIUM_HAMMER = registerItem("titanium_hammer",
            settings -> new HammerItem(settings.pickaxe(ModMaterials.TITANIUM, 5.5f, -2.9f)));

    public static final Item ENCHANTED_COBBLESTONE_HAMMER = registerItem("enchanted_cobblestone_hammer",
            settings -> new HammerItem(settings.pickaxe(ModMaterials.ENCHANTED_COBBLESTONE, 5.0f, -3.2f)) {
                @Override public boolean hasGlint(ItemStack stack) { return true; }
            });

    // Enchanted Cobblestone Armor Set
    public static final Item ENCHANTED_COBBLESTONE_HELMET = registerItem("enchanted_cobblestone_helmet",
            settings -> new EnchantedCobblestoneArmorItem(settings.armor(ModArmorMaterials.ENCHANTED_COBBLESTONE, EquipmentType.HELMET)));

    public static final Item ENCHANTED_COBBLESTONE_CHESTPLATE = registerItem("enchanted_cobblestone_chestplate",
            settings -> new EnchantedCobblestoneArmorItem(settings.armor(ModArmorMaterials.ENCHANTED_COBBLESTONE, EquipmentType.CHESTPLATE)));

    public static final Item ENCHANTED_COBBLESTONE_LEGGINGS = registerItem("enchanted_cobblestone_leggings",
            settings -> new EnchantedCobblestoneArmorItem(settings.armor(ModArmorMaterials.ENCHANTED_COBBLESTONE, EquipmentType.LEGGINGS)));

    public static final Item ENCHANTED_COBBLESTONE_BOOTS = registerItem("enchanted_cobblestone_boots",
            settings -> new EnchantedCobblestoneArmorItem(settings.armor(ModArmorMaterials.ENCHANTED_COBBLESTONE, EquipmentType.BOOTS)));

    // Enchanted Diamond Equipment
    public static final Item ENCHANTED_DIAMOND_HAMMER = registerItem("enchanted_diamond_hammer",
            settings -> new HammerItem(settings.pickaxe(ModMaterials.ENCHANTED_DIAMOND, 6.0f, -3.0f)) {
                @Override public boolean hasGlint(ItemStack stack) { return true; }
            });


    public static final Item ENCHANTED_DIAMOND_HELMET = registerItem("enchanted_diamond_helmet",
            settings -> new EnchantedArmorItem(settings.armor(ModArmorMaterials.ENCHANTED_DIAMOND, EquipmentType.HELMET)));
    public static final Item ENCHANTED_DIAMOND_CHESTPLATE = registerItem("enchanted_diamond_chestplate",
            settings -> new EnchantedArmorItem(settings.armor(ModArmorMaterials.ENCHANTED_DIAMOND, EquipmentType.CHESTPLATE)));
    public static final Item ENCHANTED_DIAMOND_LEGGINGS = registerItem("enchanted_diamond_leggings",
            settings -> new EnchantedArmorItem(settings.armor(ModArmorMaterials.ENCHANTED_DIAMOND, EquipmentType.LEGGINGS)));
    public static final Item ENCHANTED_DIAMOND_BOOTS = registerItem("enchanted_diamond_boots",
            settings -> new EnchantedArmorItem(settings.armor(ModArmorMaterials.ENCHANTED_DIAMOND, EquipmentType.BOOTS)));

    // Enchanted Netherite Equipment (Fireproof!)
    public static final Item ENCHANTED_NETHERITE_HAMMER = registerItem("enchanted_netherite_hammer",
            settings -> new HammerItem(settings.pickaxe(ModMaterials.ENCHANTED_NETHERITE, 8.0f, -2.8f).fireproof()) {
                @Override public boolean hasGlint(ItemStack stack) { return true; }
            });

    public static final Item ENCHANTED_NETHERITE_HELMET = registerItem("enchanted_netherite_helmet",
            settings -> new EnchantedArmorItem(settings.armor(ModArmorMaterials.ENCHANTED_NETHERITE, EquipmentType.HELMET).fireproof()));
    public static final Item ENCHANTED_NETHERITE_CHESTPLATE = registerItem("enchanted_netherite_chestplate",
            settings -> new EnchantedArmorItem(settings.armor(ModArmorMaterials.ENCHANTED_NETHERITE, EquipmentType.CHESTPLATE).fireproof()));
    public static final Item ENCHANTED_NETHERITE_LEGGINGS = registerItem("enchanted_netherite_leggings",
            settings -> new EnchantedArmorItem(settings.armor(ModArmorMaterials.ENCHANTED_NETHERITE, EquipmentType.LEGGINGS).fireproof()));
    public static final Item ENCHANTED_NETHERITE_BOOTS = registerItem("enchanted_netherite_boots",
            settings -> new EnchantedArmorItem(settings.armor(ModArmorMaterials.ENCHANTED_NETHERITE, EquipmentType.BOOTS).fireproof()));

    // Enchanted Chest Items
    public static final Item COPPER_ENCHANTED_CHEST = registerItem("copper_enchanted_chest", settings -> new net.enchantedwood.item.custom.EnchantedChestTierItem(net.enchantedwood.block.custom.GearTier.COPPER, settings));
    public static final Item BRONZE_ENCHANTED_CHEST = registerItem("bronze_enchanted_chest", settings -> new net.enchantedwood.item.custom.EnchantedChestTierItem(net.enchantedwood.block.custom.GearTier.BRONZE, settings));
    public static final Item ENCHANTED_IRON_ENCHANTED_CHEST = registerItem("enchanted_iron_enchanted_chest", settings -> new net.enchantedwood.item.custom.EnchantedChestTierItem(net.enchantedwood.block.custom.GearTier.ENCHANTED_IRON, settings));
    public static final Item GOLD_ENCHANTED_CHEST = registerItem("gold_enchanted_chest", settings -> new net.enchantedwood.item.custom.EnchantedChestTierItem(net.enchantedwood.block.custom.GearTier.GOLD, settings));
    public static final Item DIAMOND_ENCHANTED_CHEST = registerItem("diamond_enchanted_chest", settings -> new net.enchantedwood.item.custom.EnchantedChestTierItem(net.enchantedwood.block.custom.GearTier.DIAMOND, settings));
    public static final Item NETHERITE_ENCHANTED_CHEST = registerItem("netherite_enchanted_chest", settings -> new net.enchantedwood.item.custom.EnchantedChestTierItem(net.enchantedwood.block.custom.GearTier.NETHERITE, settings.fireproof()));

    // Scuba & Underwater Diving Equipment (v1.2.0)
    public static final Item SNORKEL = registerItem("snorkel",
            settings -> new net.enchantedwood.item.custom.SnorkelItem(EquipmentType.HELMET.getEquipmentSlot(), settings.armor(ModArmorMaterials.SCUBA, EquipmentType.HELMET)));
    public static final Item DIVING_MASK = registerItem("diving_mask",
            settings -> new net.enchantedwood.item.custom.ScubaArmorItem(EquipmentType.HELMET.getEquipmentSlot(), settings.armor(ModArmorMaterials.SCUBA, EquipmentType.HELMET)));
    public static final Item SCUBA_CHESTPLATE = registerItem("scuba_chestplate",
            settings -> new net.enchantedwood.item.custom.ScubaArmorItem(EquipmentType.CHESTPLATE.getEquipmentSlot(), settings.armor(ModArmorMaterials.SCUBA, EquipmentType.CHESTPLATE)));
    public static final Item WETSUIT_LEGGINGS = registerItem("wetsuit_leggings",
            settings -> new net.enchantedwood.item.custom.ScubaArmorItem(EquipmentType.LEGGINGS.getEquipmentSlot(), settings.armor(ModArmorMaterials.SCUBA, EquipmentType.LEGGINGS)));
    public static final Item DIVING_FLIPPERS = registerItem("diving_flippers",
            settings -> new net.enchantedwood.item.custom.ScubaArmorItem(EquipmentType.BOOTS.getEquipmentSlot(), settings.armor(ModArmorMaterials.SCUBA, EquipmentType.BOOTS)));
    public static final Item MYSTERY_KEYSTONE = registerItem("mystery_keystone", Item::new);

    // Creative Tab
    public static final RegistryKey<ItemGroup> ENCHANTED_WOOD_GROUP_KEY = RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of(EnchantedWoodMod.MOD_ID, "enchanted_wood_group"));
    public static final ItemGroup ENCHANTED_WOOD_GROUP = FabricItemGroup.builder()
            .icon(() -> new ItemStack(LIVINGWOOD_SWORD))
            .displayName(Text.translatable("itemGroup.enchantedwood.enchanted_wood_group"))
            .entries((displayContext, entries) -> {
                entries.add(net.enchantedwood.block.ModBlocks.ENCHANTED_CHEST);
                entries.add(COPPER_ENCHANTED_CHEST);
                entries.add(BRONZE_ENCHANTED_CHEST);
                entries.add(ENCHANTED_IRON_ENCHANTED_CHEST);
                entries.add(GOLD_ENCHANTED_CHEST);
                entries.add(DIAMOND_ENCHANTED_CHEST);
                entries.add(NETHERITE_ENCHANTED_CHEST);
                entries.add(INFUSED_HEARTWOOD);


                entries.add(ENCHANTED_DUST);
                entries.add(ENCHANTED_WOOD);
                entries.add(ENCHANTED_COAL);
                entries.add(ENCHANTED_REDSTONE);
                entries.add(ENCHANTED_EMERALD);
                entries.add(RAW_TIN);
                entries.add(net.enchantedwood.block.ModBlocks.TIN_ORE);
                entries.add(net.enchantedwood.block.ModBlocks.DEEPSLATE_TIN_ORE);
                entries.add(net.enchantedwood.block.ModBlocks.RAW_TIN_BLOCK);
                entries.add(net.enchantedwood.block.ModBlocks.TIN_BLOCK);
                entries.add(TIN_INGOT);
                entries.add(BRONZE_INGOT);
                entries.add(net.enchantedwood.block.ModBlocks.BRONZE_BLOCK);
                entries.add(IRON_GEAR);
                entries.add(ENCHANTED_IRON_GEAR);
                entries.add(COPPER_GEAR);
                entries.add(ENCHANTED_COPPER_GEAR);
                entries.add(BRONZE_GEAR);
                entries.add(ENCHANTED_BRONZE_GEAR);
                entries.add(GOLD_GEAR);
                entries.add(ENCHANTED_GOLD_GEAR);
                entries.add(DIAMOND_GEAR);
                entries.add(ENCHANTED_DIAMOND_GEAR);
                entries.add(NETHERITE_GEAR);
                entries.add(ENCHANTED_NETHERITE_GEAR);
                entries.add(IRON_DUST);
                entries.add(COPPER_DUST);
                entries.add(TIN_DUST);
                entries.add(BRONZE_DUST);
                entries.add(GOLD_DUST);
                entries.add(DIAMOND_DUST);
                entries.add(NETHERITE_DUST);
                entries.add(RAW_TITANIUM);
                entries.add(TITANIUM_INGOT);
                entries.add(TITANIUM_DUST);
                entries.add(TITANIUM_ROLLER);
                entries.add(net.enchantedwood.block.ModBlocks.TITANIUM_ORE);
                entries.add(net.enchantedwood.block.ModBlocks.DEEPSLATE_TITANIUM_ORE);
                entries.add(net.enchantedwood.block.ModBlocks.RAW_TITANIUM_BLOCK);
                entries.add(net.enchantedwood.block.ModBlocks.TITANIUM_BLOCK);
                entries.add(EMERALD_DUST);
                entries.add(COAL_DUST);
                entries.add(net.enchantedwood.block.ModBlocks.ENCHANTED_COBBLESTONE);
                entries.add(net.enchantedwood.block.ModBlocks.ENCHANTED_FURNACE);
                entries.add(net.enchantedwood.block.ModBlocks.CRUSHER);
                entries.add(net.enchantedwood.block.ModBlocks.DUST_SMELTER);
                entries.add(net.enchantedwood.block.ModBlocks.ENCHANTED_COAL_BLOCK);
                entries.add(net.enchantedwood.block.ModBlocks.ENCHANTED_LAVA_GENERATOR);
                entries.add(net.enchantedwood.block.ModBlocks.COPPER_GENERATOR);
                entries.add(net.enchantedwood.block.ModBlocks.COPPER_BATTERY);
                entries.add(net.enchantedwood.block.ModBlocks.COPPER_CABLE);



                entries.add(net.enchantedwood.block.ModBlocks.ENCHANTED_STORAGE_CONTROLLER);
                entries.add(net.enchantedwood.block.ModBlocks.ENCHANTED_DRIVE_BAY);
                entries.add(net.enchantedwood.block.ModBlocks.ENCHANTED_STORAGE_TERMINAL);
                entries.add(STORAGE_CRYSTAL_1K);
                entries.add(STORAGE_CRYSTAL_4K);
                entries.add(STORAGE_CRYSTAL_16K);
                entries.add(STORAGE_CRYSTAL_64K);
                entries.add(WIRELESS_STORAGE_CRYSTAL);
                entries.add(BRONZE_SWORD);
                entries.add(BRONZE_PICKAXE);
                entries.add(BRONZE_AXE);
                entries.add(BRONZE_SHOVEL);
                entries.add(BRONZE_HOE);
                entries.add(BRONZE_HELMET);
                entries.add(BRONZE_CHESTPLATE);
                entries.add(BRONZE_LEGGINGS);
                entries.add(BRONZE_BOOTS);

                entries.add(TIN_SWORD);
                entries.add(TIN_PICKAXE);
                entries.add(TIN_AXE);
                entries.add(TIN_SHOVEL);
                entries.add(TIN_HOE);
                entries.add(TIN_HELMET);
                entries.add(TIN_CHESTPLATE);
                entries.add(TIN_LEGGINGS);
                entries.add(TIN_BOOTS);

                entries.add(TITANIUM_SWORD);
                entries.add(TITANIUM_PICKAXE);
                entries.add(TITANIUM_AXE);
                entries.add(TITANIUM_SHOVEL);
                entries.add(TITANIUM_HOE);
                entries.add(TITANIUM_HELMET);
                entries.add(TITANIUM_CHESTPLATE);
                entries.add(TITANIUM_LEGGINGS);
                entries.add(TITANIUM_BOOTS);

                entries.add(LIVINGWOOD_SWORD);
                entries.add(BARKSKIN_PICKAXE);
                entries.add(IRONWOOD_AXE);
                entries.add(VERDANT_SHOVEL);
                entries.add(ELDERWOOD_HOE);
                entries.add(WOODEN_SHEARS);
                entries.add(ENCHANTED_WOOD_HELMET);
                entries.add(ENCHANTED_WOOD_CHESTPLATE);
                entries.add(ENCHANTED_WOOD_LEGGINGS);
                entries.add(ENCHANTED_WOOD_BOOTS);
                entries.add(ENCHANTED_DIAMOND);
                entries.add(ENCHANTED_NETHERITE_INGOT);
                entries.add(net.enchantedwood.block.ModBlocks.ENCHANTED_NETHERITE_BLOCK);
                entries.add(ENCHANTED_COBBLESTONE_SWORD);
                entries.add(ENCHANTED_COBBLESTONE_PICKAXE);
                entries.add(ENCHANTED_COBBLESTONE_AXE);
                entries.add(ENCHANTED_COBBLESTONE_SHOVEL);
                entries.add(ENCHANTED_COBBLESTONE_HOE);
                entries.add(WOODEN_HAMMER);
                entries.add(STONE_HAMMER);
                entries.add(COPPER_HAMMER);
                entries.add(IRON_HAMMER);
                entries.add(BRONZE_HAMMER);
                entries.add(GOLDEN_HAMMER);
                entries.add(DIAMOND_HAMMER);
                entries.add(NETHERITE_HAMMER);
                entries.add(TITANIUM_HAMMER);
                entries.add(ENCHANTED_COBBLESTONE_HAMMER);
                entries.add(ENCHANTED_COBBLESTONE_HELMET);
                entries.add(ENCHANTED_COBBLESTONE_CHESTPLATE);
                entries.add(ENCHANTED_COBBLESTONE_LEGGINGS);
                entries.add(ENCHANTED_COBBLESTONE_BOOTS);
                entries.add(ENCHANTED_DIAMOND_HAMMER);
                entries.add(ENCHANTED_DIAMOND_HELMET);
                entries.add(ENCHANTED_DIAMOND_CHESTPLATE);
                entries.add(ENCHANTED_DIAMOND_LEGGINGS);
                entries.add(ENCHANTED_DIAMOND_BOOTS);
                entries.add(ENCHANTED_NETHERITE_HAMMER);
                entries.add(ENCHANTED_NETHERITE_HELMET);
                entries.add(ENCHANTED_NETHERITE_CHESTPLATE);
                entries.add(ENCHANTED_NETHERITE_LEGGINGS);
                entries.add(ENCHANTED_NETHERITE_BOOTS);
                entries.add(ENCHANTED_CAPE);
                entries.add(ENCHANTED_HEART);
                entries.add(IRON_ENCHANTED_HEART);
                entries.add(GOLD_ENCHANTED_HEART);
                entries.add(DIAMOND_ENCHANTED_HEART);
                entries.add(NETHERITE_ENCHANTED_HEART);
                entries.add(COPPER_BUCKET);
                entries.add(COPPER_WATER_BUCKET);
                entries.add(COPPER_LAVA_BUCKET);
                entries.add(ENCHANTED_LAVA_BUCKET);
                entries.add(ENCHANTED_COPPER_LAVA_BUCKET);

                // Energy & Phase 2 Metallurgy
                entries.add(net.enchantedwood.block.ModBlocks.GAS_PIPE);
                entries.add(net.enchantedwood.block.ModBlocks.HYDROGEN_PIPE);
                entries.add(net.enchantedwood.block.ModBlocks.OXYGEN_GENERATOR);
                entries.add(net.enchantedwood.block.ModBlocks.ALUMINUM_REFINER);
                entries.add(net.enchantedwood.block.ModBlocks.ALUMINUM_GENERATOR);
                entries.add(net.enchantedwood.block.ModBlocks.ALUMINUM_BATTERY);
                entries.add(net.enchantedwood.block.ModBlocks.ALUMINUM_CABLE);

                entries.add(net.enchantedwood.block.ModBlocks.BAUXITE_ORE);
                entries.add(net.enchantedwood.block.ModBlocks.DEEPSLATE_BAUXITE_ORE);
                entries.add(net.enchantedwood.block.ModBlocks.RAW_BAUXITE_BLOCK);
                entries.add(net.enchantedwood.block.ModBlocks.ALUMINUM_BLOCK);

                entries.add(RAW_BAUXITE);
                entries.add(BAUXITE_DUST);
                entries.add(ALUMINUM_INGOT);
                entries.add(ALUMINUM_GEAR);
                entries.add(EMPTY_GAS_CANISTER);
                entries.add(OXYGEN_CANISTER);
                entries.add(HYDROGEN_CANISTER);
                entries.add(HYDROGEN_JETPACK);
                entries.add(OXY_HYDROGEN_TORCH);

                entries.add(ENCHANTED_ALUMINUM_GEAR);
                entries.add(ALUMINUM_SWORD);
                entries.add(ALUMINUM_PICKAXE);
                entries.add(ALUMINUM_AXE);
                entries.add(ALUMINUM_SHOVEL);
                entries.add(ALUMINUM_HOE);
                entries.add(ALUMINUM_HAMMER);
                entries.add(ALUMINUM_HELMET);
                entries.add(ALUMINUM_CHESTPLATE);
                entries.add(ALUMINUM_LEGGINGS);
                entries.add(ALUMINUM_BOOTS);

                // Phase 3: Coke Coal & Steel Grid
                entries.add(net.enchantedwood.block.ModBlocks.COKE_OVEN);
                entries.add(net.enchantedwood.block.ModBlocks.STEEL_BLAST_FURNACE);
                entries.add(net.enchantedwood.block.ModBlocks.STEEL_GENERATOR);
                entries.add(net.enchantedwood.block.ModBlocks.STEEL_BATTERY);
                entries.add(net.enchantedwood.block.ModBlocks.STEEL_CABLE);
                entries.add(net.enchantedwood.block.ModBlocks.STEEL_BLOCK);

                entries.add(COKE_COAL);
                entries.add(net.enchantedwood.block.ModBlocks.COKE_COAL_BLOCK);
                entries.add(STEEL_INGOT);
                entries.add(STEEL_DUST);
                entries.add(STEEL_GEAR);
                entries.add(ENCHANTED_STEEL_GEAR);

                entries.add(STEEL_SWORD);
                entries.add(STEEL_PICKAXE);
                entries.add(STEEL_AXE);
                entries.add(STEEL_SHOVEL);
                entries.add(STEEL_HOE);
                entries.add(STEEL_HAMMER);
                entries.add(STEEL_HELMET);
                entries.add(STEEL_CHESTPLATE);
                entries.add(STEEL_LEGGINGS);
                entries.add(STEEL_BOOTS);

                // Rubber Tree & Polymers
                entries.add(net.enchantedwood.block.ModBlocks.RUBBER_LOG);
                entries.add(net.enchantedwood.block.ModBlocks.RUBBER_WOOD);
                entries.add(net.enchantedwood.block.ModBlocks.STRIPPED_RUBBER_LOG);
                entries.add(net.enchantedwood.block.ModBlocks.STRIPPED_RUBBER_WOOD);
                entries.add(net.enchantedwood.block.ModBlocks.RUBBER_PLANKS);
                entries.add(net.enchantedwood.block.ModBlocks.RUBBER_LEAVES);
                entries.add(net.enchantedwood.block.ModBlocks.RUBBER_SAPLING);
                entries.add(RESIN);
                entries.add(RUBBER);

                // Storage Network Upgrades
                entries.add(CHUNK_LOADER_MODULE);
                entries.add(INTERDIMENSIONAL_CARD);

                // Scuba & Underwater Diving Equipment (v1.2.0)
                entries.add(SNORKEL);
                entries.add(DIVING_MASK);
                entries.add(SCUBA_CHESTPLATE);
                entries.add(WETSUIT_LEGGINGS);
                entries.add(DIVING_FLIPPERS);

                // Anomaly Keystones
                entries.add(net.enchantedwood.block.ModBlocks.ATMOSPHERIC_ANCHOR);
                entries.add(net.enchantedwood.block.ModBlocks.KINETIC_ANCHOR);

                // Agriculture & Biofuels
                entries.add(CORN);
                entries.add(ROASTED_CORN);
                entries.add(CORN_SEEDS);

                // Petrochemicals & Fuels
                entries.add(net.enchantedwood.block.ModBlocks.OIL_SAND);
                entries.add(CRUDE_OIL_SLUDGE);
                entries.add(MINERAL_TAR);
                entries.add(BIOFUEL_CANISTER);
                entries.add(GASOLINE_CANISTER);
                entries.add(HIGH_OCTANE_FUEL_CANISTER);
                entries.add(net.enchantedwood.block.ModBlocks.FUEL_REFINERY);
                entries.add(net.enchantedwood.block.ModBlocks.ROAD_PAVER);
                entries.add(net.enchantedwood.block.ModBlocks.ASPHALT_BLOCK);
                entries.add(net.enchantedwood.block.ModBlocks.ASPHALT_SLAB);

                // Modular All-Terrain Vehicles (ATV) & Upgrades
                entries.add(net.enchantedwood.block.ModBlocks.VEHICLE_FABRICATOR);
                entries.add(ATV_ITEM);
                entries.add(ATV_SEAT);
                entries.add(RUBBER_TIRE);
                entries.add(STEEL_RIM_TIRE);
                entries.add(TITANIUM_STUDDED_TIRE);
                entries.add(COPPER_ATV_ENGINE);
                entries.add(ALUMINUM_ATV_ENGINE);
                entries.add(STEEL_ATV_ENGINE);
                entries.add(TITANIUM_ATV_ENGINE);
                entries.add(STEEL_SUSPENSION);
                entries.add(TITANIUM_SUSPENSION);
                entries.add(ALUMINUM_ATV_CHASSIS);
                entries.add(STEEL_ATV_CHASSIS);
                entries.add(TITANIUM_ATV_CHASSIS);
                entries.add(SMALL_CARGO_TRUNK);
                entries.add(MEDIUM_CARGO_TRUNK);
                entries.add(LARGE_CARGO_TRUNK);

                // Enchanted Lighting
                entries.add(net.enchantedwood.block.ModBlocks.ENCHANTED_LAMP);
            })
            .build();

    private static <T extends Item> T registerItem(String name, Function<Item.Settings, T> itemFactory) {
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(EnchantedWoodMod.MOD_ID, name));
        T item = itemFactory.apply(new Item.Settings().registryKey(key));
        return Registry.register(Registries.ITEM, key, item);
    }

    public static void registerModItems() {
        EnchantedWoodMod.LOGGER.info("Registering Enchanted Wood Items for " + EnchantedWoodMod.MOD_ID);
        Registry.register(Registries.ITEM_GROUP, ENCHANTED_WOOD_GROUP_KEY, ENCHANTED_WOOD_GROUP);
    }
}

