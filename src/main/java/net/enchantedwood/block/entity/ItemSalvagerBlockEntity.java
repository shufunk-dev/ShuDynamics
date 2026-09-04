package net.enchantedwood.block.entity;

import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.block.custom.ItemSalvagerBlock;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.item.custom.GearItem;
import net.enchantedwood.screen.ItemSalvagerScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ItemSalvagerBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, SidedInventory, EnergyProvider {
    public static final int CAPACITY = 50_000;
    public static final int MAX_RECEIVE = 2_500;
    public static final int ENERGY_DRAW = 40; // 40 FE/t

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT_1 = 1;
    public static final int OUTPUT_SLOT_2 = 2;
    public static final int OUTPUT_SLOT_3 = 3;
    public static final int OUTPUT_SLOT_4 = 4;
    public static final int GEAR_SLOT = 5;
    public static final int INVENTORY_SIZE = 6;

    public record SalvageRecipe(Item inputItem, int inputCount, List<ItemStack> outputs, int baseCookTime) {}

    private static final Map<Item, SalvageRecipe> RECIPES = new HashMap<>();

    public static void registerRecipe(Item input, int inputCount, int baseCookTime, ItemStack... outputs) {
        RECIPES.put(input, new SalvageRecipe(input, inputCount, List.of(outputs), baseCookTime));
    }

    static {
        // 1. Horse Armor
        registerRecipe(Items.LEATHER_HORSE_ARMOR, 1, 80, new ItemStack(Items.LEATHER, 7));
        registerRecipe(Items.IRON_HORSE_ARMOR, 1, 140, new ItemStack(Items.IRON_INGOT, 6), new ItemStack(Items.LEATHER, 1));
        registerRecipe(Items.GOLDEN_HORSE_ARMOR, 1, 180, new ItemStack(Items.GOLD_INGOT, 6), new ItemStack(Items.LEATHER, 1));
        registerRecipe(Items.DIAMOND_HORSE_ARMOR, 1, 260, new ItemStack(Items.DIAMOND, 6), new ItemStack(Items.LEATHER, 1));

        // Netherite Horse Armor (if present in item registry)
        Item netheriteHorseArmor = Registries.ITEM.get(Identifier.of("minecraft", "netherite_horse_armor"));
        if (netheriteHorseArmor != Items.AIR) {
            registerRecipe(netheriteHorseArmor, 1, 400,
                    new ItemStack(Items.DIAMOND_HORSE_ARMOR, 1),
                    new ItemStack(Items.NETHERITE_INGOT, 1),
                    new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1));
        }

        // Copper Horse Armor (if present in item registry)
        Item copperHorseArmor = Registries.ITEM.get(Identifier.of("minecraft", "copper_horse_armor"));
        if (copperHorseArmor != Items.AIR) {
            registerRecipe(copperHorseArmor, 1, 120, new ItemStack(Items.COPPER_INGOT, 6), new ItemStack(Items.LEATHER, 1));
        }

        // 2. Chainmail Armor
        registerRecipe(Items.CHAINMAIL_HELMET, 1, 120, new ItemStack(Items.IRON_INGOT, 5), new ItemStack(Items.IRON_NUGGET, 10));
        registerRecipe(Items.CHAINMAIL_CHESTPLATE, 1, 180, new ItemStack(Items.IRON_INGOT, 8), new ItemStack(Items.IRON_NUGGET, 16));
        registerRecipe(Items.CHAINMAIL_LEGGINGS, 1, 160, new ItemStack(Items.IRON_INGOT, 7), new ItemStack(Items.IRON_NUGGET, 14));
        registerRecipe(Items.CHAINMAIL_BOOTS, 1, 100, new ItemStack(Items.IRON_INGOT, 4), new ItemStack(Items.IRON_NUGGET, 8));

        // 3. Minecarts & Hoppers
        registerRecipe(Items.MINECART, 1, 100, new ItemStack(Items.IRON_INGOT, 5));
        registerRecipe(Items.CHEST_MINECART, 1, 100, new ItemStack(Items.MINECART, 1), new ItemStack(Items.CHEST, 1));
        registerRecipe(Items.HOPPER_MINECART, 1, 120, new ItemStack(Items.MINECART, 1), new ItemStack(Items.HOPPER, 1));
        registerRecipe(Items.FURNACE_MINECART, 1, 100, new ItemStack(Items.MINECART, 1), new ItemStack(Items.FURNACE, 1));
        registerRecipe(Items.TNT_MINECART, 1, 100, new ItemStack(Items.MINECART, 1), new ItemStack(Items.TNT, 1));
        registerRecipe(Items.HOPPER, 1, 120, new ItemStack(Items.IRON_INGOT, 5), new ItemStack(Items.CHEST, 1));

        // 4. Rails (Batch Requirements)
        registerRecipe(Items.RAIL, 16, 120, new ItemStack(Items.IRON_INGOT, 6), new ItemStack(Items.STICK, 1));
        registerRecipe(Items.POWERED_RAIL, 6, 140, new ItemStack(Items.GOLD_INGOT, 6), new ItemStack(Items.STICK, 1), new ItemStack(Items.REDSTONE, 1));
        registerRecipe(Items.DETECTOR_RAIL, 6, 120, new ItemStack(Items.IRON_INGOT, 6), new ItemStack(Items.STONE_PRESSURE_PLATE, 1), new ItemStack(Items.REDSTONE, 1));
        registerRecipe(Items.ACTIVATOR_RAIL, 6, 120, new ItemStack(Items.IRON_INGOT, 6), new ItemStack(Items.STICK, 2), new ItemStack(Items.REDSTONE_TORCH, 1));

        // 5. Utility, Brewing & Combat
        registerRecipe(Items.SADDLE, 1, 120, new ItemStack(Items.LEATHER, 3), new ItemStack(Items.IRON_INGOT, 1), new ItemStack(Items.STRING, 1));
        registerRecipe(Items.CAULDRON, 1, 140, new ItemStack(Items.IRON_INGOT, 7));
        registerRecipe(Items.BREWING_STAND, 1, 120, new ItemStack(Items.BLAZE_ROD, 1), new ItemStack(Items.COBBLESTONE, 3));
        registerRecipe(Items.CROSSBOW, 1, 140, new ItemStack(Items.STICK, 3), new ItemStack(Items.STRING, 2), new ItemStack(Items.IRON_INGOT, 1), new ItemStack(Items.TRIPWIRE_HOOK, 1));
        registerRecipe(Items.TRIPWIRE_HOOK, 2, 80, new ItemStack(Items.IRON_INGOT, 1), new ItemStack(Items.STICK, 1), new ItemStack(Items.OAK_PLANKS, 1));

        // 6. Mod Armors
        registerModArmorRecipes();

        // 7. Mod Tools, Weapons & Sledgehammers
        registerModToolRecipes();

        // 8. Gears (Standard & Enchanted)
        registerModGearRecipes();

        // 9. Vehicles, ATV Components & Trunks
        registerModVehicleRecipes();

        // 10. Batteries, Logistics, Crystals & Tech Equipment
        registerModTechAndLogisticsRecipes();

        // 11. Machinery, Generators, Tanks & Anchors
        registerModMachineAndAnchorRecipes();
    }

    private static void registerModArmorRecipes() {
        // Steel Armor
        registerRecipe(net.enchantedwood.item.ModItems.STEEL_HELMET, 1, 120, new ItemStack(net.enchantedwood.item.ModItems.STEEL_INGOT, 5));
        registerRecipe(net.enchantedwood.item.ModItems.STEEL_CHESTPLATE, 1, 180, new ItemStack(net.enchantedwood.item.ModItems.STEEL_INGOT, 8));
        registerRecipe(net.enchantedwood.item.ModItems.STEEL_LEGGINGS, 1, 160, new ItemStack(net.enchantedwood.item.ModItems.STEEL_INGOT, 7));
        registerRecipe(net.enchantedwood.item.ModItems.STEEL_BOOTS, 1, 100, new ItemStack(net.enchantedwood.item.ModItems.STEEL_INGOT, 4));

        // Tungsten Armor
        registerRecipe(net.enchantedwood.item.ModItems.TUNGSTEN_HELMET, 1, 160, new ItemStack(net.enchantedwood.item.ModItems.TUNGSTEN_INGOT, 5));
        registerRecipe(net.enchantedwood.item.ModItems.TUNGSTEN_CHESTPLATE, 1, 240, new ItemStack(net.enchantedwood.item.ModItems.TUNGSTEN_INGOT, 8));
        registerRecipe(net.enchantedwood.item.ModItems.TUNGSTEN_LEGGINGS, 1, 200, new ItemStack(net.enchantedwood.item.ModItems.TUNGSTEN_INGOT, 7));
        registerRecipe(net.enchantedwood.item.ModItems.TUNGSTEN_BOOTS, 1, 120, new ItemStack(net.enchantedwood.item.ModItems.TUNGSTEN_INGOT, 4));

        // Cobalt Armor
        registerRecipe(net.enchantedwood.item.ModItems.COBALT_HELMET, 1, 120, new ItemStack(net.enchantedwood.item.ModItems.COBALT_INGOT, 5));
        registerRecipe(net.enchantedwood.item.ModItems.COBALT_CHESTPLATE, 1, 180, new ItemStack(net.enchantedwood.item.ModItems.COBALT_INGOT, 8));
        registerRecipe(net.enchantedwood.item.ModItems.COBALT_LEGGINGS, 1, 160, new ItemStack(net.enchantedwood.item.ModItems.COBALT_INGOT, 7));
        registerRecipe(net.enchantedwood.item.ModItems.COBALT_BOOTS, 1, 100, new ItemStack(net.enchantedwood.item.ModItems.COBALT_INGOT, 4));

        // Ardite Armor
        registerRecipe(net.enchantedwood.item.ModItems.ARDITE_HELMET, 1, 120, new ItemStack(net.enchantedwood.item.ModItems.ARDITE_INGOT, 5));
        registerRecipe(net.enchantedwood.item.ModItems.ARDITE_CHESTPLATE, 1, 180, new ItemStack(net.enchantedwood.item.ModItems.ARDITE_INGOT, 8));
        registerRecipe(net.enchantedwood.item.ModItems.ARDITE_LEGGINGS, 1, 160, new ItemStack(net.enchantedwood.item.ModItems.ARDITE_INGOT, 7));
        registerRecipe(net.enchantedwood.item.ModItems.ARDITE_BOOTS, 1, 100, new ItemStack(net.enchantedwood.item.ModItems.ARDITE_INGOT, 4));

        // Manyullyn Armor
        registerRecipe(net.enchantedwood.item.ModItems.MANYULLYN_HELMET, 1, 240, new ItemStack(net.enchantedwood.item.ModItems.MANYULLYN_INGOT, 5));
        registerRecipe(net.enchantedwood.item.ModItems.MANYULLYN_CHESTPLATE, 1, 300, new ItemStack(net.enchantedwood.item.ModItems.MANYULLYN_INGOT, 8));
        registerRecipe(net.enchantedwood.item.ModItems.MANYULLYN_LEGGINGS, 1, 280, new ItemStack(net.enchantedwood.item.ModItems.MANYULLYN_INGOT, 7));
        registerRecipe(net.enchantedwood.item.ModItems.MANYULLYN_BOOTS, 1, 200, new ItemStack(net.enchantedwood.item.ModItems.MANYULLYN_INGOT, 4));

        // Aluminum Armor
        registerRecipe(net.enchantedwood.item.ModItems.ALUMINUM_HELMET, 1, 100, new ItemStack(net.enchantedwood.item.ModItems.ALUMINUM_INGOT, 5));
        registerRecipe(net.enchantedwood.item.ModItems.ALUMINUM_CHESTPLATE, 1, 160, new ItemStack(net.enchantedwood.item.ModItems.ALUMINUM_INGOT, 8));
        registerRecipe(net.enchantedwood.item.ModItems.ALUMINUM_LEGGINGS, 1, 140, new ItemStack(net.enchantedwood.item.ModItems.ALUMINUM_INGOT, 7));
        registerRecipe(net.enchantedwood.item.ModItems.ALUMINUM_BOOTS, 1, 80, new ItemStack(net.enchantedwood.item.ModItems.ALUMINUM_INGOT, 4));

        // Bronze Armor
        registerRecipe(net.enchantedwood.item.ModItems.BRONZE_HELMET, 1, 100, new ItemStack(net.enchantedwood.item.ModItems.BRONZE_INGOT, 5));
        registerRecipe(net.enchantedwood.item.ModItems.BRONZE_CHESTPLATE, 1, 160, new ItemStack(net.enchantedwood.item.ModItems.BRONZE_INGOT, 8));
        registerRecipe(net.enchantedwood.item.ModItems.BRONZE_LEGGINGS, 1, 140, new ItemStack(net.enchantedwood.item.ModItems.BRONZE_INGOT, 7));
        registerRecipe(net.enchantedwood.item.ModItems.BRONZE_BOOTS, 1, 80, new ItemStack(net.enchantedwood.item.ModItems.BRONZE_INGOT, 4));

        // Tin Armor
        registerRecipe(net.enchantedwood.item.ModItems.TIN_HELMET, 1, 80, new ItemStack(net.enchantedwood.item.ModItems.TIN_INGOT, 5));
        registerRecipe(net.enchantedwood.item.ModItems.TIN_CHESTPLATE, 1, 140, new ItemStack(net.enchantedwood.item.ModItems.TIN_INGOT, 8));
        registerRecipe(net.enchantedwood.item.ModItems.TIN_LEGGINGS, 1, 120, new ItemStack(net.enchantedwood.item.ModItems.TIN_INGOT, 7));
        registerRecipe(net.enchantedwood.item.ModItems.TIN_BOOTS, 1, 70, new ItemStack(net.enchantedwood.item.ModItems.TIN_INGOT, 4));

        // Titanium Armor
        registerRecipe(net.enchantedwood.item.ModItems.TITANIUM_HELMET, 1, 160, new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_INGOT, 5));
        registerRecipe(net.enchantedwood.item.ModItems.TITANIUM_CHESTPLATE, 1, 220, new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_INGOT, 8));
        registerRecipe(net.enchantedwood.item.ModItems.TITANIUM_LEGGINGS, 1, 200, new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_INGOT, 7));
        registerRecipe(net.enchantedwood.item.ModItems.TITANIUM_BOOTS, 1, 140, new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_INGOT, 4));

        // Enchanted Wood Armor
        registerRecipe(net.enchantedwood.item.ModItems.ENCHANTED_WOOD_HELMET, 1, 100, new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_WOOD, 5));
        registerRecipe(net.enchantedwood.item.ModItems.ENCHANTED_WOOD_CHESTPLATE, 1, 160, new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_WOOD, 8));
        registerRecipe(net.enchantedwood.item.ModItems.ENCHANTED_WOOD_LEGGINGS, 1, 140, new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_WOOD, 7));
        registerRecipe(net.enchantedwood.item.ModItems.ENCHANTED_WOOD_BOOTS, 1, 80, new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_WOOD, 4));

        // Enchanted Cobblestone Armor
        registerRecipe(net.enchantedwood.item.ModItems.ENCHANTED_COBBLESTONE_HELMET, 1, 100, new ItemStack(net.enchantedwood.block.ModBlocks.ENCHANTED_COBBLESTONE.asItem(), 5));
        registerRecipe(net.enchantedwood.item.ModItems.ENCHANTED_COBBLESTONE_CHESTPLATE, 1, 160, new ItemStack(net.enchantedwood.block.ModBlocks.ENCHANTED_COBBLESTONE.asItem(), 8));
        registerRecipe(net.enchantedwood.item.ModItems.ENCHANTED_COBBLESTONE_LEGGINGS, 1, 140, new ItemStack(net.enchantedwood.block.ModBlocks.ENCHANTED_COBBLESTONE.asItem(), 7));
        registerRecipe(net.enchantedwood.item.ModItems.ENCHANTED_COBBLESTONE_BOOTS, 1, 80, new ItemStack(net.enchantedwood.block.ModBlocks.ENCHANTED_COBBLESTONE.asItem(), 4));

        // Enchanted Diamond Armor
        registerRecipe(net.enchantedwood.item.ModItems.ENCHANTED_DIAMOND_HELMET, 1, 240, new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_DIAMOND, 5));
        registerRecipe(net.enchantedwood.item.ModItems.ENCHANTED_DIAMOND_CHESTPLATE, 1, 320, new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_DIAMOND, 8));
        registerRecipe(net.enchantedwood.item.ModItems.ENCHANTED_DIAMOND_LEGGINGS, 1, 280, new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_DIAMOND, 7));
        registerRecipe(net.enchantedwood.item.ModItems.ENCHANTED_DIAMOND_BOOTS, 1, 200, new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_DIAMOND, 4));

        // Enchanted Netherite Armor
        registerRecipe(net.enchantedwood.item.ModItems.ENCHANTED_NETHERITE_HELMET, 1, 360, new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_DIAMOND_HELMET, 1), new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_NETHERITE_INGOT, 1));
        registerRecipe(net.enchantedwood.item.ModItems.ENCHANTED_NETHERITE_CHESTPLATE, 1, 420, new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_DIAMOND_CHESTPLATE, 1), new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_NETHERITE_INGOT, 1));
        registerRecipe(net.enchantedwood.item.ModItems.ENCHANTED_NETHERITE_LEGGINGS, 1, 390, new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_DIAMOND_LEGGINGS, 1), new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_NETHERITE_INGOT, 1));
        registerRecipe(net.enchantedwood.item.ModItems.ENCHANTED_NETHERITE_BOOTS, 1, 330, new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_DIAMOND_BOOTS, 1), new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_NETHERITE_INGOT, 1));

        // Scuba & Diving Gear
        registerRecipe(net.enchantedwood.item.ModItems.SNORKEL, 1, 100, new ItemStack(net.enchantedwood.item.ModItems.RUBBER, 2), new ItemStack(Items.GLASS_PANE, 1), new ItemStack(Items.COPPER_INGOT, 1));
        registerRecipe(net.enchantedwood.item.ModItems.DIVING_MASK, 1, 120, new ItemStack(net.enchantedwood.item.ModItems.RUBBER, 4), new ItemStack(Items.GLASS, 2), new ItemStack(Items.COPPER_INGOT, 1));
        registerRecipe(net.enchantedwood.item.ModItems.SCUBA_CHESTPLATE, 1, 160, new ItemStack(net.enchantedwood.item.ModItems.RUBBER, 6), new ItemStack(net.enchantedwood.item.ModItems.EMPTY_GAS_CANISTER, 2));
        registerRecipe(net.enchantedwood.item.ModItems.WETSUIT_LEGGINGS, 1, 140, new ItemStack(net.enchantedwood.item.ModItems.RUBBER, 7));
        registerRecipe(net.enchantedwood.item.ModItems.DIVING_FLIPPERS, 1, 100, new ItemStack(net.enchantedwood.item.ModItems.RUBBER, 4));
    }

    private static void registerModToolRecipes() {
        // Steel Tools
        registerRecipe(net.enchantedwood.item.ModItems.STEEL_SWORD, 1, 100, new ItemStack(net.enchantedwood.item.ModItems.STEEL_INGOT, 2), new ItemStack(Items.STICK, 1));
        registerRecipe(net.enchantedwood.item.ModItems.STEEL_PICKAXE, 1, 120, new ItemStack(net.enchantedwood.item.ModItems.STEEL_INGOT, 3), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.STEEL_AXE, 1, 120, new ItemStack(net.enchantedwood.item.ModItems.STEEL_INGOT, 3), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.STEEL_SHOVEL, 1, 80, new ItemStack(net.enchantedwood.item.ModItems.STEEL_INGOT, 1), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.STEEL_HOE, 1, 100, new ItemStack(net.enchantedwood.item.ModItems.STEEL_INGOT, 2), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.STEEL_HAMMER, 1, 160, new ItemStack(net.enchantedwood.item.ModItems.STEEL_INGOT, 5), new ItemStack(Items.STICK, 2));

        // Tungsten Tools
        registerRecipe(net.enchantedwood.item.ModItems.TUNGSTEN_SWORD, 1, 120, new ItemStack(net.enchantedwood.item.ModItems.TUNGSTEN_INGOT, 2), new ItemStack(Items.STICK, 1));
        registerRecipe(net.enchantedwood.item.ModItems.TUNGSTEN_PICKAXE, 1, 140, new ItemStack(net.enchantedwood.item.ModItems.TUNGSTEN_INGOT, 3), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.TUNGSTEN_AXE, 1, 140, new ItemStack(net.enchantedwood.item.ModItems.TUNGSTEN_INGOT, 3), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.TUNGSTEN_SHOVEL, 1, 100, new ItemStack(net.enchantedwood.item.ModItems.TUNGSTEN_INGOT, 1), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.TUNGSTEN_HOE, 1, 120, new ItemStack(net.enchantedwood.item.ModItems.TUNGSTEN_INGOT, 2), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.TUNGSTEN_HAMMER, 1, 180, new ItemStack(net.enchantedwood.item.ModItems.TUNGSTEN_INGOT, 5), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.TUNGSTEN_BROAD_AXE, 1, 180, new ItemStack(net.enchantedwood.item.ModItems.TUNGSTEN_INGOT, 6), new ItemStack(Items.STICK, 2));

        // Cobalt Tools
        registerRecipe(net.enchantedwood.item.ModItems.COBALT_SWORD, 1, 100, new ItemStack(net.enchantedwood.item.ModItems.COBALT_INGOT, 2), new ItemStack(Items.STICK, 1));
        registerRecipe(net.enchantedwood.item.ModItems.COBALT_PICKAXE, 1, 120, new ItemStack(net.enchantedwood.item.ModItems.COBALT_INGOT, 3), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.COBALT_AXE, 1, 120, new ItemStack(net.enchantedwood.item.ModItems.COBALT_INGOT, 3), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.COBALT_SHOVEL, 1, 80, new ItemStack(net.enchantedwood.item.ModItems.COBALT_INGOT, 1), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.COBALT_HOE, 1, 100, new ItemStack(net.enchantedwood.item.ModItems.COBALT_INGOT, 2), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.COBALT_HAMMER, 1, 160, new ItemStack(net.enchantedwood.item.ModItems.COBALT_INGOT, 5), new ItemStack(Items.STICK, 2));

        // Ardite Tools
        registerRecipe(net.enchantedwood.item.ModItems.ARDITE_SWORD, 1, 100, new ItemStack(net.enchantedwood.item.ModItems.ARDITE_INGOT, 2), new ItemStack(Items.STICK, 1));
        registerRecipe(net.enchantedwood.item.ModItems.ARDITE_PICKAXE, 1, 120, new ItemStack(net.enchantedwood.item.ModItems.ARDITE_INGOT, 3), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.ARDITE_AXE, 1, 120, new ItemStack(net.enchantedwood.item.ModItems.ARDITE_INGOT, 3), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.ARDITE_SHOVEL, 1, 80, new ItemStack(net.enchantedwood.item.ModItems.ARDITE_INGOT, 1), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.ARDITE_HOE, 1, 100, new ItemStack(net.enchantedwood.item.ModItems.ARDITE_INGOT, 2), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.ARDITE_HAMMER, 1, 160, new ItemStack(net.enchantedwood.item.ModItems.ARDITE_INGOT, 5), new ItemStack(Items.STICK, 2));

        // Manyullyn Tools
        registerRecipe(net.enchantedwood.item.ModItems.MANYULLYN_SWORD, 1, 200, new ItemStack(net.enchantedwood.item.ModItems.MANYULLYN_INGOT, 2), new ItemStack(Items.BLAZE_ROD, 1));
        registerRecipe(net.enchantedwood.item.ModItems.MANYULLYN_PICKAXE, 1, 240, new ItemStack(net.enchantedwood.item.ModItems.MANYULLYN_INGOT, 3), new ItemStack(Items.BLAZE_ROD, 2));
        registerRecipe(net.enchantedwood.item.ModItems.MANYULLYN_AXE, 1, 240, new ItemStack(net.enchantedwood.item.ModItems.MANYULLYN_INGOT, 3), new ItemStack(Items.BLAZE_ROD, 2));
        registerRecipe(net.enchantedwood.item.ModItems.MANYULLYN_SHOVEL, 1, 160, new ItemStack(net.enchantedwood.item.ModItems.MANYULLYN_INGOT, 1), new ItemStack(Items.BLAZE_ROD, 2));
        registerRecipe(net.enchantedwood.item.ModItems.MANYULLYN_HOE, 1, 200, new ItemStack(net.enchantedwood.item.ModItems.MANYULLYN_INGOT, 2), new ItemStack(Items.BLAZE_ROD, 2));
        registerRecipe(net.enchantedwood.item.ModItems.MANYULLYN_HAMMER, 1, 300, new ItemStack(net.enchantedwood.item.ModItems.MANYULLYN_INGOT, 5), new ItemStack(Items.BLAZE_ROD, 2));

        // Aluminum Tools
        registerRecipe(net.enchantedwood.item.ModItems.ALUMINUM_SWORD, 1, 80, new ItemStack(net.enchantedwood.item.ModItems.ALUMINUM_INGOT, 2), new ItemStack(Items.STICK, 1));
        registerRecipe(net.enchantedwood.item.ModItems.ALUMINUM_PICKAXE, 1, 100, new ItemStack(net.enchantedwood.item.ModItems.ALUMINUM_INGOT, 3), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.ALUMINUM_AXE, 1, 100, new ItemStack(net.enchantedwood.item.ModItems.ALUMINUM_INGOT, 3), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.ALUMINUM_SHOVEL, 1, 60, new ItemStack(net.enchantedwood.item.ModItems.ALUMINUM_INGOT, 1), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.ALUMINUM_HOE, 1, 80, new ItemStack(net.enchantedwood.item.ModItems.ALUMINUM_INGOT, 2), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.ALUMINUM_HAMMER, 1, 140, new ItemStack(net.enchantedwood.item.ModItems.ALUMINUM_INGOT, 5), new ItemStack(Items.STICK, 2));

        // Bronze Tools
        registerRecipe(net.enchantedwood.item.ModItems.BRONZE_SWORD, 1, 80, new ItemStack(net.enchantedwood.item.ModItems.BRONZE_INGOT, 2), new ItemStack(Items.STICK, 1));
        registerRecipe(net.enchantedwood.item.ModItems.BRONZE_PICKAXE, 1, 100, new ItemStack(net.enchantedwood.item.ModItems.BRONZE_INGOT, 3), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.BRONZE_AXE, 1, 100, new ItemStack(net.enchantedwood.item.ModItems.BRONZE_INGOT, 3), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.BRONZE_SHOVEL, 1, 60, new ItemStack(net.enchantedwood.item.ModItems.BRONZE_INGOT, 1), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.BRONZE_HOE, 1, 80, new ItemStack(net.enchantedwood.item.ModItems.BRONZE_INGOT, 2), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.BRONZE_HAMMER, 1, 140, new ItemStack(net.enchantedwood.item.ModItems.BRONZE_INGOT, 5), new ItemStack(Items.STICK, 2));

        // Tin Tools
        registerRecipe(net.enchantedwood.item.ModItems.TIN_SWORD, 1, 60, new ItemStack(net.enchantedwood.item.ModItems.TIN_INGOT, 2), new ItemStack(Items.STICK, 1));
        registerRecipe(net.enchantedwood.item.ModItems.TIN_PICKAXE, 1, 80, new ItemStack(net.enchantedwood.item.ModItems.TIN_INGOT, 3), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.TIN_AXE, 1, 80, new ItemStack(net.enchantedwood.item.ModItems.TIN_INGOT, 3), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.TIN_SHOVEL, 1, 50, new ItemStack(net.enchantedwood.item.ModItems.TIN_INGOT, 1), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.TIN_HOE, 1, 60, new ItemStack(net.enchantedwood.item.ModItems.TIN_INGOT, 2), new ItemStack(Items.STICK, 2));

        // Titanium Tools
        registerRecipe(net.enchantedwood.item.ModItems.TITANIUM_SWORD, 1, 140, new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_INGOT, 2), new ItemStack(Items.STICK, 1));
        registerRecipe(net.enchantedwood.item.ModItems.TITANIUM_PICKAXE, 1, 160, new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_INGOT, 3), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.TITANIUM_AXE, 1, 160, new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_INGOT, 3), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.TITANIUM_SHOVEL, 1, 120, new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_INGOT, 1), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.TITANIUM_HOE, 1, 140, new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_INGOT, 2), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.TITANIUM_HAMMER, 1, 200, new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_INGOT, 5), new ItemStack(Items.STICK, 2));

        // Enchanted Wood Tools
        registerRecipe(net.enchantedwood.item.ModItems.LIVINGWOOD_SWORD, 1, 100, new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_WOOD, 2), new ItemStack(Items.STICK, 1));
        registerRecipe(net.enchantedwood.item.ModItems.BARKSKIN_PICKAXE, 1, 120, new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_WOOD, 3), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.IRONWOOD_AXE, 1, 120, new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_WOOD, 3), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.VERDANT_SHOVEL, 1, 80, new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_WOOD, 1), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.ELDERWOOD_HOE, 1, 100, new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_WOOD, 2), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.WOODEN_SHEARS, 1, 40, new ItemStack(Items.OAK_PLANKS, 2));

        // Enchanted Cobblestone Tools
        registerRecipe(net.enchantedwood.item.ModItems.ENCHANTED_COBBLESTONE_SWORD, 1, 100, new ItemStack(net.enchantedwood.block.ModBlocks.ENCHANTED_COBBLESTONE.asItem(), 2), new ItemStack(Items.STICK, 1));
        registerRecipe(net.enchantedwood.item.ModItems.ENCHANTED_COBBLESTONE_PICKAXE, 1, 120, new ItemStack(net.enchantedwood.block.ModBlocks.ENCHANTED_COBBLESTONE.asItem(), 3), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.ENCHANTED_COBBLESTONE_AXE, 1, 120, new ItemStack(net.enchantedwood.block.ModBlocks.ENCHANTED_COBBLESTONE.asItem(), 3), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.ENCHANTED_COBBLESTONE_SHOVEL, 1, 80, new ItemStack(net.enchantedwood.block.ModBlocks.ENCHANTED_COBBLESTONE.asItem(), 1), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.ENCHANTED_COBBLESTONE_HOE, 1, 100, new ItemStack(net.enchantedwood.block.ModBlocks.ENCHANTED_COBBLESTONE.asItem(), 2), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.ENCHANTED_COBBLESTONE_HAMMER, 1, 160, new ItemStack(net.enchantedwood.block.ModBlocks.ENCHANTED_COBBLESTONE.asItem(), 5), new ItemStack(Items.STICK, 2));

        // Vanilla & Base Sledgehammers
        registerRecipe(net.enchantedwood.item.ModItems.WOODEN_HAMMER, 1, 60, new ItemStack(Items.OAK_PLANKS, 5), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.STONE_HAMMER, 1, 80, new ItemStack(Items.COBBLESTONE, 5), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.COPPER_HAMMER, 1, 100, new ItemStack(Items.COPPER_INGOT, 5), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.IRON_HAMMER, 1, 140, new ItemStack(Items.IRON_INGOT, 5), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.GOLDEN_HAMMER, 1, 100, new ItemStack(Items.GOLD_INGOT, 5), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.DIAMOND_HAMMER, 1, 240, new ItemStack(Items.DIAMOND, 5), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.NETHERITE_HAMMER, 1, 360, new ItemStack(net.enchantedwood.item.ModItems.DIAMOND_HAMMER, 1), new ItemStack(Items.NETHERITE_INGOT, 1), new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1));
        registerRecipe(net.enchantedwood.item.ModItems.ENCHANTED_DIAMOND_HAMMER, 1, 280, new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_DIAMOND, 5), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.ENCHANTED_NETHERITE_HAMMER, 1, 400, new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_DIAMOND_HAMMER, 1), new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_NETHERITE_INGOT, 1));
        registerRecipe(net.enchantedwood.item.ModItems.INFERNAL_HAMMER, 1, 360, new ItemStack(net.enchantedwood.item.ModItems.MANYULLYN_INGOT, 5), new ItemStack(Items.BLAZE_ROD, 2), new ItemStack(net.enchantedwood.item.ModItems.FIRE_CRYSTAL, 2));

        // Broad Axes (Timber Harvesting)
        registerRecipe(net.enchantedwood.item.ModItems.WOODEN_BROAD_AXE, 1, 60, new ItemStack(Items.OAK_PLANKS, 6), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.STONE_BROAD_AXE, 1, 80, new ItemStack(Items.COBBLESTONE, 6), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.COPPER_BROAD_AXE, 1, 100, new ItemStack(Items.COPPER_INGOT, 6), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.IRON_BROAD_AXE, 1, 140, new ItemStack(Items.IRON_INGOT, 6), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.GOLDEN_BROAD_AXE, 1, 100, new ItemStack(Items.GOLD_INGOT, 6), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.DIAMOND_BROAD_AXE, 1, 240, new ItemStack(Items.DIAMOND, 6), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.NETHERITE_BROAD_AXE, 1, 360, new ItemStack(net.enchantedwood.item.ModItems.DIAMOND_BROAD_AXE, 1), new ItemStack(Items.NETHERITE_INGOT, 1), new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1));
        registerRecipe(net.enchantedwood.item.ModItems.ALUMINUM_BROAD_AXE, 1, 140, new ItemStack(net.enchantedwood.item.ModItems.ALUMINUM_INGOT, 6), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.BRONZE_BROAD_AXE, 1, 140, new ItemStack(net.enchantedwood.item.ModItems.BRONZE_INGOT, 6), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.STEEL_BROAD_AXE, 1, 160, new ItemStack(net.enchantedwood.item.ModItems.STEEL_INGOT, 6), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.TITANIUM_BROAD_AXE, 1, 200, new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_INGOT, 6), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.COBALT_BROAD_AXE, 1, 160, new ItemStack(net.enchantedwood.item.ModItems.COBALT_INGOT, 6), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.ARDITE_BROAD_AXE, 1, 160, new ItemStack(net.enchantedwood.item.ModItems.ARDITE_INGOT, 6), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.MANYULLYN_BROAD_AXE, 1, 300, new ItemStack(net.enchantedwood.item.ModItems.MANYULLYN_INGOT, 6), new ItemStack(Items.BLAZE_ROD, 2));
        registerRecipe(net.enchantedwood.item.ModItems.ENCHANTED_COBBLESTONE_BROAD_AXE, 1, 160, new ItemStack(net.enchantedwood.block.ModBlocks.ENCHANTED_COBBLESTONE.asItem(), 6), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.ENCHANTED_DIAMOND_BROAD_AXE, 1, 280, new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_DIAMOND, 6), new ItemStack(Items.STICK, 2));
        registerRecipe(net.enchantedwood.item.ModItems.ENCHANTED_NETHERITE_BROAD_AXE, 1, 400, new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_DIAMOND_BROAD_AXE, 1), new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_NETHERITE_INGOT, 1));
    }

    private static void registerModGearRecipes() {
        // Base Gears
        registerRecipe(net.enchantedwood.item.ModItems.IRON_GEAR, 1, 80, new ItemStack(Items.IRON_INGOT, 4), new ItemStack(Items.IRON_NUGGET, 1));
        registerRecipe(net.enchantedwood.item.ModItems.COPPER_GEAR, 1, 80, new ItemStack(Items.COPPER_INGOT, 4), new ItemStack(Items.IRON_NUGGET, 1));
        registerRecipe(net.enchantedwood.item.ModItems.BRONZE_GEAR, 1, 80, new ItemStack(net.enchantedwood.item.ModItems.BRONZE_INGOT, 4), new ItemStack(Items.IRON_NUGGET, 1));
        registerRecipe(net.enchantedwood.item.ModItems.ALUMINUM_GEAR, 1, 80, new ItemStack(net.enchantedwood.item.ModItems.ALUMINUM_INGOT, 4), new ItemStack(Items.IRON_NUGGET, 1));
        registerRecipe(net.enchantedwood.item.ModItems.STEEL_GEAR, 1, 100, new ItemStack(net.enchantedwood.item.ModItems.STEEL_INGOT, 4), new ItemStack(Items.IRON_NUGGET, 1));
        registerRecipe(net.enchantedwood.item.ModItems.GOLD_GEAR, 1, 100, new ItemStack(Items.GOLD_INGOT, 4), new ItemStack(Items.IRON_NUGGET, 1));
        registerRecipe(net.enchantedwood.item.ModItems.TITANIUM_GEAR, 1, 120, new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_INGOT, 4), new ItemStack(Items.IRON_NUGGET, 1));
        registerRecipe(net.enchantedwood.item.ModItems.DIAMOND_GEAR, 1, 160, new ItemStack(Items.DIAMOND, 4), new ItemStack(Items.IRON_INGOT, 1));
        registerRecipe(net.enchantedwood.item.ModItems.NETHERITE_GEAR, 1, 260, new ItemStack(net.enchantedwood.item.ModItems.DIAMOND_GEAR, 1), new ItemStack(Items.NETHERITE_INGOT, 1));

        // Enchanted Gears
        registerRecipe(net.enchantedwood.item.ModItems.ENCHANTED_IRON_GEAR, 1, 120, new ItemStack(net.enchantedwood.item.ModItems.IRON_GEAR, 1), new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_DUST, 4));
        registerRecipe(net.enchantedwood.item.ModItems.ENCHANTED_COPPER_GEAR, 1, 120, new ItemStack(net.enchantedwood.item.ModItems.COPPER_GEAR, 1), new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_DUST, 4));
        registerRecipe(net.enchantedwood.item.ModItems.ENCHANTED_BRONZE_GEAR, 1, 120, new ItemStack(net.enchantedwood.item.ModItems.BRONZE_GEAR, 1), new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_DUST, 4));
        registerRecipe(net.enchantedwood.item.ModItems.ENCHANTED_ALUMINUM_GEAR, 1, 120, new ItemStack(net.enchantedwood.item.ModItems.ALUMINUM_GEAR, 1), new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_DUST, 4));
        registerRecipe(net.enchantedwood.item.ModItems.ENCHANTED_STEEL_GEAR, 1, 140, new ItemStack(net.enchantedwood.item.ModItems.STEEL_GEAR, 1), new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_DUST, 4));
        registerRecipe(net.enchantedwood.item.ModItems.ENCHANTED_GOLD_GEAR, 1, 140, new ItemStack(net.enchantedwood.item.ModItems.GOLD_GEAR, 1), new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_DUST, 4));
        registerRecipe(net.enchantedwood.item.ModItems.ENCHANTED_TITANIUM_GEAR, 1, 160, new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_GEAR, 1), new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_DUST, 4));
        registerRecipe(net.enchantedwood.item.ModItems.ENCHANTED_DIAMOND_GEAR, 1, 200, new ItemStack(net.enchantedwood.item.ModItems.DIAMOND_GEAR, 1), new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_DUST, 4));
        registerRecipe(net.enchantedwood.item.ModItems.ENCHANTED_NETHERITE_GEAR, 1, 300, new ItemStack(net.enchantedwood.item.ModItems.NETHERITE_GEAR, 1), new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_DUST, 4));
    }

    private static void registerModVehicleRecipes() {
        registerRecipe(net.enchantedwood.item.ModItems.ATV_SEAT, 1, 80, new ItemStack(Items.LEATHER, 2), new ItemStack(Items.BLACK_WOOL, 2), new ItemStack(Items.IRON_INGOT, 1));
        registerRecipe(net.enchantedwood.item.ModItems.RUBBER_TIRE, 1, 80, new ItemStack(net.enchantedwood.item.ModItems.RUBBER, 4), new ItemStack(Items.IRON_INGOT, 1));
        registerRecipe(net.enchantedwood.item.ModItems.STEEL_RIM_TIRE, 1, 100, new ItemStack(net.enchantedwood.item.ModItems.RUBBER_TIRE, 1), new ItemStack(net.enchantedwood.item.ModItems.STEEL_INGOT, 1));
        registerRecipe(net.enchantedwood.item.ModItems.TITANIUM_STUDDED_TIRE, 1, 120, new ItemStack(net.enchantedwood.item.ModItems.RUBBER_TIRE, 1), new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_INGOT, 1));

        registerRecipe(net.enchantedwood.item.ModItems.COPPER_ATV_ENGINE, 1, 140, new ItemStack(Items.COPPER_INGOT, 4), new ItemStack(Items.PISTON, 1), new ItemStack(net.enchantedwood.item.ModItems.COPPER_GEAR, 1), new ItemStack(Items.REDSTONE, 1));
        registerRecipe(net.enchantedwood.item.ModItems.ALUMINUM_ATV_ENGINE, 1, 140, new ItemStack(net.enchantedwood.item.ModItems.ALUMINUM_INGOT, 4), new ItemStack(Items.PISTON, 1), new ItemStack(net.enchantedwood.item.ModItems.ALUMINUM_GEAR, 1), new ItemStack(Items.REDSTONE, 1));
        registerRecipe(net.enchantedwood.item.ModItems.STEEL_ATV_ENGINE, 1, 160, new ItemStack(net.enchantedwood.item.ModItems.STEEL_INGOT, 4), new ItemStack(Items.PISTON, 1), new ItemStack(net.enchantedwood.item.ModItems.STEEL_GEAR, 1), new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_REDSTONE, 1));
        registerRecipe(net.enchantedwood.item.ModItems.TITANIUM_ATV_ENGINE, 1, 220, new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_INGOT, 4), new ItemStack(net.enchantedwood.item.ModItems.STEEL_ATV_ENGINE, 1), new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_GEAR, 1));

        registerRecipe(net.enchantedwood.item.ModItems.ALUMINUM_SUSPENSION, 1, 80, new ItemStack(net.enchantedwood.item.ModItems.ALUMINUM_INGOT, 4), new ItemStack(Items.IRON_BARS, 2));
        registerRecipe(net.enchantedwood.item.ModItems.STEEL_SUSPENSION, 1, 100, new ItemStack(net.enchantedwood.item.ModItems.STEEL_INGOT, 4), new ItemStack(Items.IRON_BARS, 2));
        registerRecipe(net.enchantedwood.item.ModItems.TITANIUM_SUSPENSION, 1, 140, new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_INGOT, 4), new ItemStack(Items.IRON_BARS, 2));

        registerRecipe(net.enchantedwood.item.ModItems.ALUMINUM_ATV_CHASSIS, 1, 120, new ItemStack(net.enchantedwood.item.ModItems.ALUMINUM_INGOT, 7));
        registerRecipe(net.enchantedwood.item.ModItems.STEEL_ATV_CHASSIS, 1, 140, new ItemStack(net.enchantedwood.item.ModItems.STEEL_INGOT, 7));
        registerRecipe(net.enchantedwood.item.ModItems.TITANIUM_ATV_CHASSIS, 1, 180, new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_INGOT, 7));

        registerRecipe(net.enchantedwood.item.ModItems.SMALL_CARGO_TRUNK, 1, 120, new ItemStack(Items.IRON_INGOT, 8), new ItemStack(Items.CHEST, 1));
        registerRecipe(net.enchantedwood.item.ModItems.MEDIUM_CARGO_TRUNK, 1, 160, new ItemStack(net.enchantedwood.item.ModItems.STEEL_INGOT, 8), new ItemStack(net.enchantedwood.item.ModItems.SMALL_CARGO_TRUNK, 1));
        registerRecipe(net.enchantedwood.item.ModItems.LARGE_CARGO_TRUNK, 1, 200, new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_INGOT, 8), new ItemStack(net.enchantedwood.item.ModItems.MEDIUM_CARGO_TRUNK, 1));

        // ATV Mining Drill Bits
        registerRecipe(net.enchantedwood.item.ModItems.IRON_DRILL_BIT, 1, 100, new ItemStack(Items.IRON_INGOT, 4), new ItemStack(net.enchantedwood.item.ModItems.IRON_GEAR, 1));
        registerRecipe(net.enchantedwood.item.ModItems.STEEL_DRILL_BIT, 1, 140, new ItemStack(net.enchantedwood.item.ModItems.STEEL_INGOT, 4), new ItemStack(net.enchantedwood.item.ModItems.STEEL_GEAR, 1));
        registerRecipe(net.enchantedwood.item.ModItems.DIAMOND_DRILL_BIT, 1, 220, new ItemStack(Items.DIAMOND, 4), new ItemStack(net.enchantedwood.item.ModItems.DIAMOND_GEAR, 1));
        registerRecipe(net.enchantedwood.item.ModItems.TITANIUM_DRILL_BIT, 1, 260, new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_INGOT, 4), new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_GEAR, 1));
        registerRecipe(net.enchantedwood.item.ModItems.NETHERITE_DRILL_BIT, 1, 360, new ItemStack(Items.NETHERITE_INGOT, 3), new ItemStack(net.enchantedwood.item.ModItems.DIAMOND_DRILL_BIT, 1), new ItemStack(net.enchantedwood.item.ModItems.NETHERITE_GEAR, 1));

        // ATV Tree Harvester Saws
        registerRecipe(net.enchantedwood.item.ModItems.IRON_TREE_SAW, 1, 100, new ItemStack(Items.IRON_INGOT, 4), new ItemStack(net.enchantedwood.item.ModItems.IRON_GEAR, 1), new ItemStack(Items.IRON_AXE, 2));
        registerRecipe(net.enchantedwood.item.ModItems.STEEL_TREE_SAW, 1, 140, new ItemStack(net.enchantedwood.item.ModItems.STEEL_INGOT, 4), new ItemStack(net.enchantedwood.item.ModItems.STEEL_GEAR, 1), new ItemStack(net.enchantedwood.item.ModItems.IRON_TREE_SAW, 1));
        registerRecipe(net.enchantedwood.item.ModItems.DIAMOND_TREE_SAW, 1, 220, new ItemStack(Items.DIAMOND, 4), new ItemStack(net.enchantedwood.item.ModItems.DIAMOND_GEAR, 1), new ItemStack(net.enchantedwood.item.ModItems.STEEL_TREE_SAW, 1));
        registerRecipe(net.enchantedwood.item.ModItems.TITANIUM_TREE_SAW, 1, 260, new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_INGOT, 4), new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_GEAR, 1), new ItemStack(net.enchantedwood.item.ModItems.DIAMOND_TREE_SAW, 1));
        registerRecipe(net.enchantedwood.item.ModItems.NETHERITE_TREE_SAW, 1, 360, new ItemStack(Items.NETHERITE_INGOT, 3), new ItemStack(net.enchantedwood.item.ModItems.NETHERITE_GEAR, 1), new ItemStack(net.enchantedwood.item.ModItems.DIAMOND_TREE_SAW, 1));

        // ATV Crop Harvesters
        registerRecipe(net.enchantedwood.item.ModItems.IRON_CROP_HARVESTER, 1, 100, new ItemStack(Items.IRON_INGOT, 4), new ItemStack(net.enchantedwood.item.ModItems.IRON_GEAR, 1), new ItemStack(Items.IRON_HOE, 2));
        registerRecipe(net.enchantedwood.item.ModItems.STEEL_CROP_HARVESTER, 1, 140, new ItemStack(net.enchantedwood.item.ModItems.STEEL_INGOT, 4), new ItemStack(net.enchantedwood.item.ModItems.STEEL_GEAR, 1), new ItemStack(net.enchantedwood.item.ModItems.IRON_CROP_HARVESTER, 1));
        registerRecipe(net.enchantedwood.item.ModItems.DIAMOND_CROP_HARVESTER, 1, 220, new ItemStack(Items.DIAMOND, 4), new ItemStack(net.enchantedwood.item.ModItems.DIAMOND_GEAR, 1), new ItemStack(net.enchantedwood.item.ModItems.STEEL_CROP_HARVESTER, 1));
        registerRecipe(net.enchantedwood.item.ModItems.TITANIUM_CROP_HARVESTER, 1, 260, new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_INGOT, 4), new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_GEAR, 1), new ItemStack(net.enchantedwood.item.ModItems.DIAMOND_CROP_HARVESTER, 1));
        registerRecipe(net.enchantedwood.item.ModItems.NETHERITE_CROP_HARVESTER, 1, 360, new ItemStack(Items.NETHERITE_INGOT, 3), new ItemStack(net.enchantedwood.item.ModItems.NETHERITE_GEAR, 1), new ItemStack(net.enchantedwood.item.ModItems.DIAMOND_CROP_HARVESTER, 1));

        // ATV Headlights
        registerRecipe(net.enchantedwood.item.ModItems.HALOGEN_HEADLIGHTS, 1, 100, new ItemStack(Items.IRON_INGOT, 4), new ItemStack(Items.GLASS_PANE, 2), new ItemStack(Items.GLOWSTONE_DUST, 1));
        registerRecipe(net.enchantedwood.item.ModItems.LED_FLOODLIGHTS, 1, 140, new ItemStack(net.enchantedwood.item.ModItems.STEEL_INGOT, 4), new ItemStack(Items.REDSTONE_LAMP, 2), new ItemStack(net.enchantedwood.item.ModItems.HALOGEN_HEADLIGHTS, 1));
        registerRecipe(net.enchantedwood.item.ModItems.XENON_HIGH_BEAMS, 1, 220, new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_INGOT, 4), new ItemStack(net.enchantedwood.block.ModBlocks.VOLCANIC_GLASS.asItem(), 2), new ItemStack(net.enchantedwood.item.ModItems.LED_FLOODLIGHTS, 1));

        registerRecipe(net.enchantedwood.item.ModItems.ATV_ITEM, 1, 260, new ItemStack(net.enchantedwood.item.ModItems.STEEL_ATV_CHASSIS, 1), new ItemStack(net.enchantedwood.item.ModItems.COPPER_ATV_ENGINE, 1), new ItemStack(net.enchantedwood.item.ModItems.ATV_SEAT, 1), new ItemStack(net.enchantedwood.item.ModItems.RUBBER_TIRE, 4));
    }

    private static void registerModTechAndLogisticsRecipes() {
        // Batteries & Battery Packs
        registerRecipe(net.enchantedwood.block.ModBlocks.COPPER_BATTERY.asItem(), 1, 100, new ItemStack(Items.COPPER_INGOT, 4), new ItemStack(Items.REDSTONE, 2), new ItemStack(Items.IRON_INGOT, 1));
        registerRecipe(net.enchantedwood.block.ModBlocks.ALUMINUM_BATTERY.asItem(), 1, 120, new ItemStack(net.enchantedwood.item.ModItems.ALUMINUM_INGOT, 4), new ItemStack(Items.REDSTONE, 2), new ItemStack(Items.COPPER_INGOT, 1));
        registerRecipe(net.enchantedwood.block.ModBlocks.STEEL_BATTERY.asItem(), 1, 160, new ItemStack(net.enchantedwood.item.ModItems.STEEL_INGOT, 4), new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_REDSTONE, 2), new ItemStack(net.enchantedwood.block.ModBlocks.ALUMINUM_BATTERY.asItem(), 1));
        registerRecipe(net.enchantedwood.block.ModBlocks.TUNGSTEN_BATTERY.asItem(), 1, 220, new ItemStack(net.enchantedwood.item.ModItems.TUNGSTEN_INGOT, 4), new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_REDSTONE, 2), new ItemStack(net.enchantedwood.block.ModBlocks.STEEL_BATTERY.asItem(), 1));

        registerRecipe(net.enchantedwood.item.ModItems.COPPER_BATTERY_PACK, 1, 120, new ItemStack(net.enchantedwood.block.ModBlocks.COPPER_BATTERY.asItem(), 1), new ItemStack(Items.COPPER_INGOT, 2), new ItemStack(Items.LEATHER, 2));
        registerRecipe(net.enchantedwood.item.ModItems.ALUMINUM_BATTERY_PACK, 1, 140, new ItemStack(net.enchantedwood.block.ModBlocks.ALUMINUM_BATTERY.asItem(), 1), new ItemStack(net.enchantedwood.item.ModItems.ALUMINUM_INGOT, 2), new ItemStack(Items.LEATHER, 2));
        registerRecipe(net.enchantedwood.item.ModItems.STEEL_BATTERY_PACK, 1, 180, new ItemStack(net.enchantedwood.block.ModBlocks.STEEL_BATTERY.asItem(), 1), new ItemStack(net.enchantedwood.item.ModItems.STEEL_INGOT, 2), new ItemStack(Items.LEATHER, 2));
        registerRecipe(net.enchantedwood.item.ModItems.TUNGSTEN_BATTERY_PACK, 1, 240, new ItemStack(net.enchantedwood.block.ModBlocks.TUNGSTEN_BATTERY.asItem(), 1), new ItemStack(net.enchantedwood.item.ModItems.TUNGSTEN_INGOT, 2), new ItemStack(Items.LEATHER, 2));

        // Storage System
        registerRecipe(net.enchantedwood.item.ModItems.STORAGE_CRYSTAL_1K, 1, 120, new ItemStack(Items.REDSTONE, 4), new ItemStack(Items.IRON_INGOT, 4), new ItemStack(Items.QUARTZ, 1));
        registerRecipe(net.enchantedwood.item.ModItems.STORAGE_CRYSTAL_4K, 1, 160, new ItemStack(net.enchantedwood.item.ModItems.STORAGE_CRYSTAL_1K, 4), new ItemStack(Items.GOLD_INGOT, 4), new ItemStack(Items.DIAMOND, 1));
        registerRecipe(net.enchantedwood.item.ModItems.STORAGE_CRYSTAL_16K, 1, 220, new ItemStack(net.enchantedwood.item.ModItems.STORAGE_CRYSTAL_4K, 4), new ItemStack(Items.DIAMOND, 4), new ItemStack(Items.EMERALD, 1));
        registerRecipe(net.enchantedwood.item.ModItems.STORAGE_CRYSTAL_64K, 1, 300, new ItemStack(net.enchantedwood.item.ModItems.STORAGE_CRYSTAL_16K, 4), new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_INGOT, 4), new ItemStack(Items.NETHERITE_INGOT, 1));
        registerRecipe(net.enchantedwood.item.ModItems.WIRELESS_STORAGE_CRYSTAL, 1, 220, new ItemStack(net.enchantedwood.item.ModItems.STORAGE_CRYSTAL_16K, 1), new ItemStack(Items.ENDER_EYE, 2), new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_GEAR, 1));
        registerRecipe(net.enchantedwood.item.ModItems.CHUNK_LOADER_MODULE, 1, 240, new ItemStack(Items.ENDER_PEARL, 4), new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_INGOT, 4), new ItemStack(net.enchantedwood.item.ModItems.DIAMOND_GEAR, 1));
        registerRecipe(net.enchantedwood.item.ModItems.INTERDIMENSIONAL_CARD, 1, 300, new ItemStack(Items.NETHERITE_INGOT, 2), new ItemStack(Items.ENDER_EYE, 2), new ItemStack(net.enchantedwood.item.ModItems.FIRE_CRYSTAL, 1), new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_DIAMOND, 1));

        // Quarry Cores & Upgrades
        registerRecipe(net.enchantedwood.item.ModItems.RANGE_UPGRADE_T1, 1, 160, new ItemStack(Items.GOLD_INGOT, 4), new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_REDSTONE, 4), new ItemStack(Items.DIAMOND, 1));
        registerRecipe(net.enchantedwood.item.ModItems.RANGE_UPGRADE_T2, 1, 260, new ItemStack(Items.DIAMOND, 4), new ItemStack(net.enchantedwood.item.ModItems.RANGE_UPGRADE_T1, 4), new ItemStack(net.enchantedwood.item.ModItems.FIRE_CRYSTAL, 1));
        registerRecipe(net.enchantedwood.item.ModItems.FORTUNE_CORE, 1, 240, new ItemStack(Items.EMERALD, 4), new ItemStack(net.enchantedwood.item.ModItems.GOLD_GEAR, 4), new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_DIAMOND, 1));
        registerRecipe(net.enchantedwood.item.ModItems.SILK_TOUCH_CORE, 1, 200, new ItemStack(Items.AMETHYST_SHARD, 4), new ItemStack(net.enchantedwood.item.ModItems.COPPER_GEAR, 4), new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_EMERALD, 1));

        // Plates
        registerRecipe(net.enchantedwood.item.ModItems.TUNGSTEN_PLATE, 1, 140, new ItemStack(net.enchantedwood.item.ModItems.TUNGSTEN_INGOT, 2));
        registerRecipe(net.enchantedwood.item.ModItems.ARDITE_PLATE, 1, 140, new ItemStack(net.enchantedwood.item.ModItems.ARDITE_INGOT, 2));
        registerRecipe(net.enchantedwood.item.ModItems.COBALT_PLATE, 1, 140, new ItemStack(net.enchantedwood.item.ModItems.COBALT_INGOT, 2));
        registerRecipe(net.enchantedwood.item.ModItems.MANYULLYN_PLATE, 1, 180, new ItemStack(net.enchantedwood.item.ModItems.MANYULLYN_INGOT, 2));

        // Cables, Pipes & Logistics (Batched)
        registerRecipe(net.enchantedwood.block.ModBlocks.COPPER_CABLE.asItem(), 6, 100, new ItemStack(Items.COPPER_INGOT, 3), new ItemStack(net.enchantedwood.item.ModItems.RUBBER, 6));
        registerRecipe(net.enchantedwood.block.ModBlocks.ALUMINUM_CABLE.asItem(), 6, 100, new ItemStack(net.enchantedwood.item.ModItems.ALUMINUM_INGOT, 3), new ItemStack(net.enchantedwood.item.ModItems.RUBBER, 6));
        registerRecipe(net.enchantedwood.block.ModBlocks.STEEL_CABLE.asItem(), 6, 120, new ItemStack(net.enchantedwood.item.ModItems.STEEL_INGOT, 3), new ItemStack(net.enchantedwood.item.ModItems.RUBBER, 6));
        registerRecipe(net.enchantedwood.block.ModBlocks.TUNGSTEN_CABLE.asItem(), 6, 160, new ItemStack(net.enchantedwood.item.ModItems.TUNGSTEN_INGOT, 3), new ItemStack(net.enchantedwood.item.ModItems.RUBBER, 6));
        registerRecipe(net.enchantedwood.block.ModBlocks.BASALT_CABLE.asItem(), 6, 160, new ItemStack(Items.BASALT, 3), new ItemStack(net.enchantedwood.item.ModItems.TUNGSTEN_INGOT, 3), new ItemStack(net.enchantedwood.item.ModItems.RUBBER, 6));

        registerRecipe(net.enchantedwood.block.ModBlocks.ITEM_PIPE.asItem(), 8, 100, new ItemStack(Items.IRON_INGOT, 6), new ItemStack(Items.GLASS, 2));
        registerRecipe(net.enchantedwood.block.ModBlocks.GAS_PIPE.asItem(), 8, 100, new ItemStack(net.enchantedwood.item.ModItems.ALUMINUM_INGOT, 6), new ItemStack(Items.GLASS, 2));
        registerRecipe(net.enchantedwood.block.ModBlocks.HYDROGEN_PIPE.asItem(), 8, 100, new ItemStack(net.enchantedwood.item.ModItems.ALUMINUM_INGOT, 6), new ItemStack(Items.GLASS, 2));
        registerRecipe(net.enchantedwood.block.ModBlocks.TITANIUM_LAVA_PIPE.asItem(), 8, 140, new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_INGOT, 6), new ItemStack(Items.OBSIDIAN, 2));

        registerRecipe(net.enchantedwood.block.ModBlocks.ITEM_EXTRACTOR.asItem(), 1, 120, new ItemStack(Items.IRON_INGOT, 4), new ItemStack(Items.PISTON, 1), new ItemStack(Items.REDSTONE, 1));
        registerRecipe(net.enchantedwood.block.ModBlocks.ITEM_INSERTER.asItem(), 1, 120, new ItemStack(Items.IRON_INGOT, 4), new ItemStack(Items.HOPPER, 1), new ItemStack(Items.REDSTONE, 1));

        registerRecipe(net.enchantedwood.item.ModItems.WRENCH, 1, 80, new ItemStack(net.enchantedwood.item.ModItems.STEEL_INGOT, 4));
        registerRecipe(net.enchantedwood.item.ModItems.TITANIUM_ROLLER, 1, 100, new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_INGOT, 3));
        registerRecipe(net.enchantedwood.item.ModItems.EMPTY_GAS_CANISTER, 1, 80, new ItemStack(net.enchantedwood.item.ModItems.ALUMINUM_INGOT, 4), new ItemStack(net.enchantedwood.item.ModItems.RUBBER, 1));

        // High-Tech Utilities & Upgrades
        registerRecipe(net.enchantedwood.item.ModItems.FIRE_CRYSTAL, 1, 140, new ItemStack(Items.BLAZE_POWDER, 4), new ItemStack(Items.MAGMA_CREAM, 3), new ItemStack(Items.QUARTZ, 1), new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_DUST, 1));
        registerRecipe(net.enchantedwood.item.ModItems.HYDROGEN_JETPACK, 1, 200, new ItemStack(net.enchantedwood.item.ModItems.ALUMINUM_INGOT, 4), new ItemStack(net.enchantedwood.item.ModItems.EMPTY_GAS_CANISTER, 2), new ItemStack(Items.LEATHER, 1));
        registerRecipe(net.enchantedwood.item.ModItems.OXY_HYDROGEN_TORCH, 1, 140, new ItemStack(net.enchantedwood.item.ModItems.ALUMINUM_INGOT, 2), new ItemStack(net.enchantedwood.item.ModItems.EMPTY_GAS_CANISTER, 1), new ItemStack(Items.FLINT_AND_STEEL, 1));
        registerRecipe(net.enchantedwood.item.ModItems.PLASMA_FLAMETHROWER, 1, 320, new ItemStack(net.enchantedwood.item.ModItems.TUNGSTEN_CARBIDE_INGOT, 4), new ItemStack(net.enchantedwood.item.ModItems.FIRE_CRYSTAL, 2), new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_ROLLER, 1));
        registerRecipe(net.enchantedwood.item.ModItems.BLAZE_OVERCLOCK_CORE, 1, 300, new ItemStack(Items.BLAZE_ROD, 4), new ItemStack(net.enchantedwood.item.ModItems.FIRE_CRYSTAL, 4), new ItemStack(net.enchantedwood.item.ModItems.NETHERITE_GEAR, 1));
        registerRecipe(net.enchantedwood.item.ModItems.THERMAL_REFRACTORY_PLATING, 1, 240, new ItemStack(net.enchantedwood.item.ModItems.TUNGSTEN_PLATE, 4), new ItemStack(net.enchantedwood.item.ModItems.VOLCANIC_ASH, 4), new ItemStack(net.enchantedwood.item.ModItems.FIRE_CRYSTAL, 1));
        registerRecipe(net.enchantedwood.item.ModItems.BASALT_FLUX_CATALYST, 1, 160, new ItemStack(Items.BASALT, 4), new ItemStack(net.enchantedwood.item.ModItems.VOLCANIC_ASH, 2), new ItemStack(net.enchantedwood.item.ModItems.FIRE_CRYSTAL, 1));
        registerRecipe(net.enchantedwood.item.ModItems.ENCHANTED_CAPE, 1, 100, new ItemStack(Items.WHITE_WOOL, 4), new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_DUST, 1));
        registerRecipe(net.enchantedwood.item.ModItems.COPPER_BUCKET, 1, 80, new ItemStack(Items.COPPER_INGOT, 3));

        // Heart Lockets
        registerRecipe(net.enchantedwood.item.ModItems.ENCHANTED_HEART, 1, 160, new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_DUST, 4), new ItemStack(net.enchantedwood.item.ModItems.INFUSED_HEARTWOOD, 4), new ItemStack(Items.APPLE, 1));
        registerRecipe(net.enchantedwood.item.ModItems.IRON_ENCHANTED_HEART, 1, 180, new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_HEART, 1), new ItemStack(Items.IRON_INGOT, 8));
        registerRecipe(net.enchantedwood.item.ModItems.GOLD_ENCHANTED_HEART, 1, 220, new ItemStack(net.enchantedwood.item.ModItems.IRON_ENCHANTED_HEART, 1), new ItemStack(Items.GOLD_INGOT, 8));
        registerRecipe(net.enchantedwood.item.ModItems.DIAMOND_ENCHANTED_HEART, 1, 280, new ItemStack(net.enchantedwood.item.ModItems.GOLD_ENCHANTED_HEART, 1), new ItemStack(Items.DIAMOND, 8));
        registerRecipe(net.enchantedwood.item.ModItems.NETHERITE_ENCHANTED_HEART, 1, 380, new ItemStack(net.enchantedwood.item.ModItems.DIAMOND_ENCHANTED_HEART, 1), new ItemStack(Items.NETHERITE_INGOT, 4));

        // Enchanted Chests
        registerRecipe(net.enchantedwood.block.ModBlocks.ENCHANTED_CHEST.asItem(), 1, 120, new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_WOOD, 8), new ItemStack(Items.CHEST, 1));
        registerRecipe(net.enchantedwood.item.ModItems.COPPER_ENCHANTED_CHEST, 1, 140, new ItemStack(net.enchantedwood.block.ModBlocks.ENCHANTED_CHEST.asItem(), 1), new ItemStack(Items.COPPER_INGOT, 8));
        registerRecipe(net.enchantedwood.item.ModItems.BRONZE_ENCHANTED_CHEST, 1, 160, new ItemStack(net.enchantedwood.item.ModItems.COPPER_ENCHANTED_CHEST, 1), new ItemStack(net.enchantedwood.item.ModItems.BRONZE_INGOT, 8));
        registerRecipe(net.enchantedwood.item.ModItems.ENCHANTED_IRON_ENCHANTED_CHEST, 1, 180, new ItemStack(net.enchantedwood.item.ModItems.BRONZE_ENCHANTED_CHEST, 1), new ItemStack(Items.IRON_INGOT, 8));
        registerRecipe(net.enchantedwood.item.ModItems.GOLD_ENCHANTED_CHEST, 1, 220, new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_IRON_ENCHANTED_CHEST, 1), new ItemStack(Items.GOLD_INGOT, 8));
        registerRecipe(net.enchantedwood.item.ModItems.DIAMOND_ENCHANTED_CHEST, 1, 280, new ItemStack(net.enchantedwood.item.ModItems.GOLD_ENCHANTED_CHEST, 1), new ItemStack(Items.DIAMOND, 8));
        registerRecipe(net.enchantedwood.item.ModItems.NETHERITE_ENCHANTED_CHEST, 1, 380, new ItemStack(net.enchantedwood.item.ModItems.DIAMOND_ENCHANTED_CHEST, 1), new ItemStack(Items.NETHERITE_INGOT, 4));
    }

    private static void registerModMachineAndAnchorRecipes() {
        // Machines & Processing
        registerRecipe(net.enchantedwood.block.ModBlocks.CRUSHER.asItem(), 1, 200, new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_INGOT, 6), new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_ROLLER, 2), new ItemStack(Items.REDSTONE, 1));
        registerRecipe(net.enchantedwood.block.ModBlocks.CRUSHER_MK2.asItem(), 1, 260, new ItemStack(net.enchantedwood.block.ModBlocks.CRUSHER.asItem(), 1), new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_GEAR, 4), new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_REDSTONE, 2));
        registerRecipe(net.enchantedwood.block.ModBlocks.DUST_SMELTER.asItem(), 1, 200, new ItemStack(net.enchantedwood.item.ModItems.STEEL_INGOT, 6), new ItemStack(net.enchantedwood.block.ModBlocks.COPPER_CABLE.asItem(), 2), new ItemStack(Items.FURNACE, 1));
        registerRecipe(net.enchantedwood.block.ModBlocks.ALLOY_FOUNDRY.asItem(), 1, 300, new ItemStack(net.enchantedwood.item.ModItems.TUNGSTEN_CARBIDE_INGOT, 4), new ItemStack(net.enchantedwood.item.ModItems.COBALT_PLATE, 2), new ItemStack(net.enchantedwood.item.ModItems.ARDITE_PLATE, 2), new ItemStack(net.enchantedwood.block.ModBlocks.STEEL_BLAST_FURNACE.asItem(), 1));
        registerRecipe(net.enchantedwood.block.ModBlocks.STEEL_BLAST_FURNACE.asItem(), 1, 220, new ItemStack(net.enchantedwood.item.ModItems.STEEL_INGOT, 6), new ItemStack(Items.BLAST_FURNACE, 2), new ItemStack(net.enchantedwood.block.ModBlocks.COKE_COAL_BLOCK.asItem(), 1));
        registerRecipe(net.enchantedwood.block.ModBlocks.COKE_OVEN.asItem(), 1, 140, new ItemStack(Items.BRICK, 8), new ItemStack(Items.FURNACE, 1));

        // Power Generation
        registerRecipe(net.enchantedwood.block.ModBlocks.COPPER_GENERATOR.asItem(), 1, 160, new ItemStack(Items.COPPER_INGOT, 6), new ItemStack(Items.FURNACE, 1), new ItemStack(net.enchantedwood.block.ModBlocks.COPPER_CABLE.asItem(), 2));
        registerRecipe(net.enchantedwood.block.ModBlocks.ALUMINUM_GENERATOR.asItem(), 1, 200, new ItemStack(net.enchantedwood.item.ModItems.ALUMINUM_INGOT, 6), new ItemStack(net.enchantedwood.block.ModBlocks.COPPER_GENERATOR.asItem(), 1), new ItemStack(net.enchantedwood.block.ModBlocks.ALUMINUM_CABLE.asItem(), 2));
        registerRecipe(net.enchantedwood.block.ModBlocks.STEEL_GENERATOR.asItem(), 1, 240, new ItemStack(net.enchantedwood.item.ModItems.STEEL_INGOT, 6), new ItemStack(net.enchantedwood.block.ModBlocks.ALUMINUM_GENERATOR.asItem(), 1), new ItemStack(net.enchantedwood.block.ModBlocks.STEEL_CABLE.asItem(), 2));
        registerRecipe(net.enchantedwood.block.ModBlocks.ENCHANTED_LAVA_GENERATOR.asItem(), 1, 300, new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_INGOT, 6), new ItemStack(net.enchantedwood.block.ModBlocks.STEEL_GENERATOR.asItem(), 1), new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_LAVA_BUCKET, 2));
        registerRecipe(net.enchantedwood.block.ModBlocks.GEOTHERMAL_GENERATOR.asItem(), 1, 360, new ItemStack(net.enchantedwood.item.ModItems.TUNGSTEN_INGOT, 6), new ItemStack(net.enchantedwood.block.ModBlocks.ENCHANTED_LAVA_GENERATOR.asItem(), 1), new ItemStack(Items.MAGMA_BLOCK, 2));

        // Industrial Tech & Infrastructure
        registerRecipe(net.enchantedwood.block.ModBlocks.FUEL_REFINERY.asItem(), 1, 220, new ItemStack(net.enchantedwood.item.ModItems.STEEL_INGOT, 6), new ItemStack(Items.GLASS, 2), new ItemStack(Items.FURNACE, 1));
        registerRecipe(net.enchantedwood.block.ModBlocks.OXYGEN_GENERATOR.asItem(), 1, 200, new ItemStack(net.enchantedwood.item.ModItems.ALUMINUM_INGOT, 6), new ItemStack(net.enchantedwood.item.ModItems.EMPTY_GAS_CANISTER, 2), new ItemStack(net.enchantedwood.block.ModBlocks.COPPER_CABLE.asItem(), 1));
        registerRecipe(net.enchantedwood.block.ModBlocks.ALUMINUM_REFINER.asItem(), 1, 200, new ItemStack(net.enchantedwood.item.ModItems.ALUMINUM_INGOT, 6), new ItemStack(Items.GLASS, 2), new ItemStack(Items.BLAST_FURNACE, 1));
        registerRecipe(net.enchantedwood.block.ModBlocks.MAGMA_CRUCIBLE.asItem(), 1, 260, new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_INGOT, 6), new ItemStack(Items.BLAZE_ROD, 2), new ItemStack(Items.LAVA_BUCKET, 1));
        registerRecipe(net.enchantedwood.block.ModBlocks.LAVA_PUMP.asItem(), 1, 240, new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_INGOT, 6), new ItemStack(Items.BUCKET, 2), new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_ROLLER, 1));
        registerRecipe(net.enchantedwood.block.ModBlocks.SOIL_INFUSER.asItem(), 1, 200, new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_WOOD, 6), new ItemStack(net.enchantedwood.item.ModItems.VOLCANIC_FERTILIZER, 2), new ItemStack(net.enchantedwood.item.ModItems.INFUSED_HEARTWOOD, 1));
        registerRecipe(net.enchantedwood.block.ModBlocks.ROAD_PAVER.asItem(), 1, 240, new ItemStack(net.enchantedwood.item.ModItems.STEEL_INGOT, 6), new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_ROLLER, 2), new ItemStack(net.enchantedwood.block.ModBlocks.ASPHALT_BLOCK.asItem(), 1));
        registerRecipe(net.enchantedwood.block.ModBlocks.VEHICLE_FABRICATOR.asItem(), 1, 220, new ItemStack(net.enchantedwood.item.ModItems.STEEL_INGOT, 6), new ItemStack(net.enchantedwood.item.ModItems.STEEL_GEAR, 2), new ItemStack(Items.CRAFTING_TABLE, 1));
        registerRecipe(net.enchantedwood.block.ModBlocks.HYDRAULIC_PRESS.asItem(), 1, 240, new ItemStack(net.enchantedwood.item.ModItems.STEEL_INGOT, 4), new ItemStack(Items.PISTON, 1), new ItemStack(Items.ANVIL, 1), new ItemStack(net.enchantedwood.block.ModBlocks.COPPER_CABLE.asItem(), 2), new ItemStack(net.enchantedwood.item.ModItems.IRON_GEAR, 1));
        registerRecipe(net.enchantedwood.block.ModBlocks.ENCHANTED_FURNACE.asItem(), 1, 160, new ItemStack(net.enchantedwood.block.ModBlocks.ENCHANTED_COBBLESTONE.asItem(), 8), new ItemStack(Items.FURNACE, 1));
        registerRecipe(net.enchantedwood.block.ModBlocks.ITEM_SALVAGER.asItem(), 1, 260, new ItemStack(net.enchantedwood.item.ModItems.STEEL_INGOT, 4), new ItemStack(net.enchantedwood.item.ModItems.DIAMOND_GEAR, 1), new ItemStack(net.enchantedwood.block.ModBlocks.COPPER_CABLE.asItem(), 3), new ItemStack(net.enchantedwood.block.ModBlocks.CRUSHER.asItem(), 1));

        // Digital Storage Blocks & Automation
        registerRecipe(net.enchantedwood.block.ModBlocks.ENCHANTED_STORAGE_CONTROLLER.asItem(), 1, 260, new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_INGOT, 6), new ItemStack(net.enchantedwood.item.ModItems.DIAMOND_GEAR, 2), new ItemStack(net.enchantedwood.block.ModBlocks.ENCHANTED_CHEST.asItem(), 1));
        registerRecipe(net.enchantedwood.block.ModBlocks.ENCHANTED_STORAGE_TERMINAL.asItem(), 1, 220, new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_INGOT, 6), new ItemStack(Items.GLASS, 2), new ItemStack(net.enchantedwood.item.ModItems.STORAGE_CRYSTAL_1K, 1));
        registerRecipe(net.enchantedwood.block.ModBlocks.ENCHANTED_DRIVE_BAY.asItem(), 1, 220, new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_INGOT, 6), new ItemStack(Items.CHEST, 2), new ItemStack(net.enchantedwood.item.ModItems.DIAMOND_GEAR, 1));
        registerRecipe(net.enchantedwood.block.ModBlocks.DIGITAL_CONVERTER.asItem(), 1, 200, new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_INGOT, 4), new ItemStack(Items.HOPPER, 1), new ItemStack(net.enchantedwood.block.ModBlocks.COPPER_CABLE.asItem(), 2), new ItemStack(Items.QUARTZ, 2));
        registerRecipe(net.enchantedwood.block.ModBlocks.SUPER_COMPUTER.asItem(), 1, 360, new ItemStack(net.enchantedwood.item.ModItems.TUNGSTEN_CARBIDE_INGOT, 4), new ItemStack(net.enchantedwood.item.ModItems.STORAGE_CRYSTAL_16K, 1), new ItemStack(net.enchantedwood.item.ModItems.DIAMOND_GEAR, 2), new ItemStack(net.enchantedwood.block.ModBlocks.DIGITAL_CONVERTER.asItem(), 1));
        registerRecipe(net.enchantedwood.block.ModBlocks.LASER_QUARRY.asItem(), 1, 360, new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_INGOT, 3), new ItemStack(net.enchantedwood.item.ModItems.STEEL_INGOT, 2), new ItemStack(net.enchantedwood.item.ModItems.DIAMOND_GEAR, 2), new ItemStack(net.enchantedwood.block.ModBlocks.DIGITAL_CONVERTER.asItem(), 1));

        // Multiblock Fluid Tanks
        registerRecipe(net.enchantedwood.block.ModBlocks.TITANIUM_TANK_CASING.asItem(), 4, 160, new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_INGOT, 8));
        registerRecipe(net.enchantedwood.block.ModBlocks.REINFORCED_TANK_GLASS.asItem(), 4, 140, new ItemStack(Items.GLASS, 4), new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_INGOT, 4), new ItemStack(Items.OBSIDIAN, 1));
        registerRecipe(net.enchantedwood.block.ModBlocks.TITANIUM_TANK_INBOUND_PORT.asItem(), 1, 180, new ItemStack(net.enchantedwood.block.ModBlocks.TITANIUM_TANK_CASING.asItem(), 1), new ItemStack(Items.HOPPER, 1), new ItemStack(net.enchantedwood.block.ModBlocks.COPPER_CABLE.asItem(), 1));

        // Anchors
        registerRecipe(net.enchantedwood.block.ModBlocks.KINETIC_ANCHOR.asItem(), 1, 260, new ItemStack(net.enchantedwood.item.ModItems.STEEL_INGOT, 4), new ItemStack(Items.PISTON, 2), new ItemStack(net.enchantedwood.item.ModItems.STEEL_GEAR, 2), new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_HEART, 1));
        registerRecipe(net.enchantedwood.block.ModBlocks.THERMAL_ANCHOR.asItem(), 1, 280, new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_INGOT, 4), new ItemStack(net.enchantedwood.item.ModItems.FIRE_CRYSTAL, 2), new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_GEAR, 2), new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_HEART, 1));
        registerRecipe(net.enchantedwood.block.ModBlocks.ATMOSPHERIC_ANCHOR.asItem(), 1, 260, new ItemStack(net.enchantedwood.item.ModItems.ALUMINUM_INGOT, 4), new ItemStack(net.enchantedwood.item.ModItems.OXYGEN_CANISTER, 2), new ItemStack(net.enchantedwood.item.ModItems.ALUMINUM_GEAR, 2), new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_HEART, 1));
        registerRecipe(net.enchantedwood.block.ModBlocks.METALLURGICAL_ANCHOR.asItem(), 1, 300, new ItemStack(net.enchantedwood.item.ModItems.TUNGSTEN_CARBIDE_INGOT, 4), new ItemStack(net.enchantedwood.item.ModItems.BASALT_FLUX_CATALYST, 2), new ItemStack(net.enchantedwood.item.ModItems.STEEL_GEAR, 2), new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_HEART, 1));
        registerRecipe(net.enchantedwood.block.ModBlocks.PLASMA_ANCHOR.asItem(), 1, 340, new ItemStack(net.enchantedwood.item.ModItems.MANYULLYN_INGOT, 4), new ItemStack(net.enchantedwood.item.ModItems.BLAZE_OVERCLOCK_CORE, 2), new ItemStack(net.enchantedwood.item.ModItems.TITANIUM_GEAR, 2), new ItemStack(net.enchantedwood.item.ModItems.ENCHANTED_HEART, 1));
    }

    public static boolean isSalvageable(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (RECIPES.containsKey(stack.getItem())) return true;

        Identifier id = Registries.ITEM.getId(stack.getItem());
        if (id != null) {
            String path = id.getPath();
            if (path.equals("copper_horse_armor")) {
                registerRecipe(stack.getItem(), 1, 120, new ItemStack(Items.COPPER_INGOT, 6), new ItemStack(Items.LEATHER, 1));
                return true;
            } else if (path.equals("netherite_horse_armor")) {
                registerRecipe(stack.getItem(), 1, 400,
                        new ItemStack(Items.DIAMOND_HORSE_ARMOR, 1),
                        new ItemStack(Items.NETHERITE_INGOT, 1),
                        new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1));
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static SalvageRecipe getRecipe(ItemStack stack) {
        if (stack.isEmpty()) return null;
        SalvageRecipe recipe = RECIPES.get(stack.getItem());
        if (recipe != null) return recipe;

        Identifier id = Registries.ITEM.getId(stack.getItem());
        if (id != null) {
            String path = id.getPath();
            if (path.equals("copper_horse_armor")) {
                SalvageRecipe dynamicRecipe = new SalvageRecipe(stack.getItem(), 1, List.of(
                        new ItemStack(Items.COPPER_INGOT, 6),
                        new ItemStack(Items.LEATHER, 1)
                ), 120);
                RECIPES.put(stack.getItem(), dynamicRecipe);
                return dynamicRecipe;
            } else if (path.equals("netherite_horse_armor")) {
                SalvageRecipe dynamicRecipe = new SalvageRecipe(stack.getItem(), 1, List.of(
                        new ItemStack(Items.DIAMOND_HORSE_ARMOR, 1),
                        new ItemStack(Items.NETHERITE_INGOT, 1),
                        new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1)
                ), 400);
                RECIPES.put(stack.getItem(), dynamicRecipe);
                return dynamicRecipe;
            }
        }
        return null;
    }

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(CAPACITY, MAX_RECEIVE, MAX_RECEIVE, 0);

    private int cookTime = 0;
    private int totalCookTime = 120;

    protected final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> cookTime;
                case 1 -> totalCookTime;
                case 2 -> energyStorage.getEnergy() & 0xFFFF;
                case 3 -> (energyStorage.getEnergy() >> 16) & 0xFFFF;
                case 4 -> energyStorage.getMaxEnergy() & 0xFFFF;
                case 5 -> (energyStorage.getMaxEnergy() >> 16) & 0xFFFF;
                case 6 -> getActiveGearTier().ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> cookTime = value;
                case 1 -> totalCookTime = value;
            }
        }

        @Override
        public int size() {
            return 7;
        }
    };

    public ItemSalvagerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ITEM_SALVAGER_BE, pos, state);
    }

    public GearTier getActiveGearTier() {
        ItemStack gearStack = inventory.get(GEAR_SLOT);
        if (gearStack.getItem() instanceof GearItem gearItem) {
            return gearItem.getGearTier();
        }
        return GearTier.NONE;
    }

    public static int getScaledCookTime(GearTier tier, int baseCookTime) {
        double multiplier = switch (tier) {
            case IRON -> 0.85;
            case COPPER -> 0.72;
            case BRONZE -> 0.60;
            case GOLD -> 0.48;
            case DIAMOND -> 0.32;
            case TITANIUM -> 0.22;
            case NETHERITE -> 0.15;
            case BLAZE_OVERCLOCK -> 0.10;
            default -> 1.0;
        };
        return Math.max(10, (int) (baseCookTime * multiplier));
    }

    @Nullable
    public static Item getNuggetForIngot(Item ingot) {
        if (ingot == Items.IRON_INGOT) return Items.IRON_NUGGET;
        if (ingot == Items.GOLD_INGOT) return Items.GOLD_NUGGET;
        if (ingot == net.enchantedwood.item.ModItems.TIN_INGOT) return net.enchantedwood.item.ModItems.TIN_NUGGET;
        if (ingot == net.enchantedwood.item.ModItems.BRONZE_INGOT) return net.enchantedwood.item.ModItems.BRONZE_NUGGET;
        if (ingot == net.enchantedwood.item.ModItems.ALUMINUM_INGOT) return net.enchantedwood.item.ModItems.ALUMINUM_NUGGET;
        if (ingot == net.enchantedwood.item.ModItems.STEEL_INGOT) return net.enchantedwood.item.ModItems.STEEL_NUGGET;
        if (ingot == net.enchantedwood.item.ModItems.TITANIUM_INGOT) return net.enchantedwood.item.ModItems.TITANIUM_NUGGET;
        if (ingot == net.enchantedwood.item.ModItems.TUNGSTEN_INGOT) return net.enchantedwood.item.ModItems.TUNGSTEN_NUGGET;
        if (ingot == net.enchantedwood.item.ModItems.COBALT_INGOT) return net.enchantedwood.item.ModItems.COBALT_NUGGET;
        if (ingot == net.enchantedwood.item.ModItems.ARDITE_INGOT) return net.enchantedwood.item.ModItems.ARDITE_NUGGET;
        if (ingot == net.enchantedwood.item.ModItems.MANYULLYN_INGOT) return net.enchantedwood.item.ModItems.MANYULLYN_NUGGET;
        return null;
    }

    public static List<ItemStack> calculateScaledOutputs(SalvageRecipe recipe, int consumeCount, @Nullable net.minecraft.util.math.random.Random random) {
        if (consumeCount >= recipe.inputCount()) {
            return recipe.outputs().stream().map(ItemStack::copy).toList();
        }

        List<ItemStack> result = new ArrayList<>();
        for (ItemStack out : recipe.outputs()) {
            Item nuggetItem = getNuggetForIngot(out.getItem());
            if (nuggetItem != null) {
                int totalNuggets = out.getCount() * 9;
                int scaledNuggets = (totalNuggets * consumeCount) / recipe.inputCount();
                int remainder = (totalNuggets * consumeCount) % recipe.inputCount();
                if (random != null && remainder > 0 && random.nextInt(recipe.inputCount()) < remainder) {
                    scaledNuggets++;
                }
                int ingots = scaledNuggets / 9;
                int nuggets = scaledNuggets % 9;
                if (ingots > 0) {
                    result.add(new ItemStack(out.getItem(), ingots));
                }
                if (nuggets > 0) {
                    result.add(new ItemStack(nuggetItem, nuggets));
                }
            } else {
                int total = out.getCount() * consumeCount;
                int count = total / recipe.inputCount();
                int remainder = total % recipe.inputCount();
                if (random != null && remainder > 0 && random.nextInt(recipe.inputCount()) < remainder) {
                    count++;
                }
                if (count > 0) {
                    result.add(out.copyWithCount(count));
                }
            }
        }
        return result;
    }

    public static List<ItemStack> calculateMaxScaledOutputs(SalvageRecipe recipe, int consumeCount) {
        if (consumeCount >= recipe.inputCount()) {
            return recipe.outputs().stream().map(ItemStack::copy).toList();
        }

        List<ItemStack> result = new ArrayList<>();
        for (ItemStack out : recipe.outputs()) {
            Item nuggetItem = getNuggetForIngot(out.getItem());
            if (nuggetItem != null) {
                int totalNuggets = out.getCount() * 9;
                int scaledNuggets = (totalNuggets * consumeCount + recipe.inputCount() - 1) / recipe.inputCount();
                int ingots = scaledNuggets / 9;
                int nuggets = scaledNuggets % 9;
                if (ingots > 0) {
                    result.add(new ItemStack(out.getItem(), ingots));
                }
                if (nuggets > 0) {
                    result.add(new ItemStack(nuggetItem, nuggets));
                }
            } else {
                int count = (out.getCount() * consumeCount + recipe.inputCount() - 1) / recipe.inputCount();
                if (count > 0) {
                    result.add(out.copyWithCount(count));
                }
            }
        }
        return result;
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, ItemSalvagerBlockEntity salvager) {
        ItemStack input = salvager.inventory.get(INPUT_SLOT);
        SalvageRecipe recipe = getRecipe(input);

        boolean originallyWorking = state.get(ItemSalvagerBlock.LIT);
        boolean isWorking = false;

        if (recipe != null && !input.isEmpty()) {
            int consumeCount = Math.min(input.getCount(), recipe.inputCount());
            List<ItemStack> maxOutputs = calculateMaxScaledOutputs(recipe, consumeCount);

            if (!maxOutputs.isEmpty() && salvager.canFitOutputs(maxOutputs)) {
                int scaledBaseTime = Math.max(20, (recipe.baseCookTime() * consumeCount) / recipe.inputCount());
                int targetCookTime = getScaledCookTime(salvager.getActiveGearTier(), scaledBaseTime);
                salvager.totalCookTime = targetCookTime;

                if (salvager.energyStorage.getEnergy() >= ENERGY_DRAW) {
                    salvager.energyStorage.extractEnergy(ENERGY_DRAW, false);
                    salvager.cookTime++;
                    isWorking = true;

                    if (salvager.cookTime >= salvager.totalCookTime) {
                        salvager.cookTime = 0;
                        salvager.craftSalvage(recipe, consumeCount, world.getRandom());
                    }
                    markDirty(world, pos, state);
                }
            } else {
                if (salvager.cookTime > 0) {
                    salvager.cookTime = 0;
                    markDirty(world, pos, state);
                }
            }
        } else {
            if (salvager.cookTime > 0) {
                salvager.cookTime = 0;
                markDirty(world, pos, state);
            }
        }

        if (originallyWorking != isWorking) {
            world.setBlockState(pos, state.with(ItemSalvagerBlock.LIT, isWorking), 3);
        }
    }

    private boolean canFitOutputs(List<ItemStack> outputs) {
        // Create simulated copy of slots 1..4
        ItemStack[] simulated = new ItemStack[4];
        for (int i = 0; i < 4; i++) {
            simulated[i] = inventory.get(OUTPUT_SLOT_1 + i).copy();
        }

        for (ItemStack toInsert : outputs) {
            int remaining = toInsert.getCount();
            // Try merge with existing stacks
            for (int i = 0; i < 4; i++) {
                if (ItemStack.areItemsAndComponentsEqual(simulated[i], toInsert)) {
                    int space = simulated[i].getMaxCount() - simulated[i].getCount();
                    int add = Math.min(space, remaining);
                    simulated[i].increment(add);
                    remaining -= add;
                    if (remaining <= 0) break;
                }
            }
            // Try empty slots
            if (remaining > 0) {
                for (int i = 0; i < 4; i++) {
                    if (simulated[i].isEmpty()) {
                        simulated[i] = toInsert.copyWithCount(remaining);
                        remaining = 0;
                        break;
                    }
                }
            }
            if (remaining > 0) return false;
        }
        return true;
    }

    private void craftSalvage(SalvageRecipe recipe, int consumeCount, net.minecraft.util.math.random.Random random) {
        ItemStack input = inventory.get(INPUT_SLOT);
        input.decrement(consumeCount);

        List<ItemStack> outputs = calculateScaledOutputs(recipe, consumeCount, random);

        for (ItemStack toInsert : outputs) {
            int remaining = toInsert.getCount();

            // First merge
            for (int i = 0; i < 4; i++) {
                ItemStack current = inventory.get(OUTPUT_SLOT_1 + i);
                if (ItemStack.areItemsAndComponentsEqual(current, toInsert)) {
                    int space = current.getMaxCount() - current.getCount();
                    int add = Math.min(space, remaining);
                    current.increment(add);
                    remaining -= add;
                    if (remaining <= 0) break;
                }
            }
            // Then empty slot
            if (remaining > 0) {
                for (int i = 0; i < 4; i++) {
                    ItemStack current = inventory.get(OUTPUT_SLOT_1 + i);
                    if (current.isEmpty()) {
                        inventory.set(OUTPUT_SLOT_1 + i, toInsert.copyWithCount(remaining));
                        remaining = 0;
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.inventory.clear();
        Inventories.readData(view, this.inventory);
        this.energyStorage.readData(view);
        this.cookTime = view.getInt("CookTime", 0);
        this.totalCookTime = view.getInt("TotalCookTime", 120);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, this.inventory);
        this.energyStorage.writeData(view);
        view.putInt("CookTime", this.cookTime);
        view.putInt("TotalCookTime", this.totalCookTime);
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        if (side == Direction.UP) {
            return new int[]{INPUT_SLOT};
        }
        return new int[]{OUTPUT_SLOT_1, OUTPUT_SLOT_2, OUTPUT_SLOT_3, OUTPUT_SLOT_4};
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == INPUT_SLOT) {
            return isSalvageable(stack);
        }
        if (slot == GEAR_SLOT) {
            return stack.getItem() instanceof GearItem;
        }
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot >= OUTPUT_SLOT_1 && slot <= OUTPUT_SLOT_4;
    }

    @Override
    public int size() {
        return INVENTORY_SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : inventory) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return inventory.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(inventory, slot, amount);
        if (!result.isEmpty()) markDirty();
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack result = Inventories.removeStack(inventory, slot);
        if (!result.isEmpty()) markDirty();
        return result;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        inventory.set(slot, stack);
        if (stack.getCount() > getMaxCountPerStack()) {
            stack.setCount(getMaxCountPerStack());
        }
        markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return Inventory.canPlayerUse(this, player);
    }

    @Override
    public void clear() {
        inventory.clear();
    }

    @Override
    public EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.enchantedwood.item_salvager");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new ItemSalvagerScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }
}
