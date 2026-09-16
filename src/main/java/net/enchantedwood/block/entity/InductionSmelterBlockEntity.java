package net.enchantedwood.block.entity;

import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.block.custom.InductionSmelterBlock;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.fluid.LavaProvider;
import net.enchantedwood.fluid.MoltenMetal;
import net.enchantedwood.fluid.MoltenMetalProvider;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.GearItem;
import net.enchantedwood.screen.InductionSmelterScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class InductionSmelterBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, SidedInventory, EnergyProvider, LavaProvider, MoltenMetalProvider {
    public static final int ENERGY_CAPACITY = 50_000;
    public static final int MAX_RECEIVE = 2_500;
    public static final int ENERGY_DRAW = 45; // 45 FE/t
    public static final int LAVA_CAPACITY = 10_000; // 10 buckets
    public static final int LAVA_PER_SMELT = 5; // 5 mB lava per item melted
    public static final int CHAMBER_CAPACITY = 32_400; // 32,400 mB = 360 ingots / 40 blocks

    public static final int INPUT_SLOT_1 = 0;
    public static final int INPUT_SLOT_2 = 1;
    public static final int MODULE_SLOT = 2;
    public static final int GEAR_SLOT = 3;
    public static final int LAVA_IN_SLOT = 4;
    public static final int LAVA_OUT_SLOT = 5;
    public static final int INVENTORY_SIZE = 6;

    public static final int HOLDING_TANK_CAPACITY = 10_800; // 120 ingots per holding tank

    public record SmeltYield(MoltenMetal metal, int amountMb, int cookTime) {}

    private static final Map<Item, SmeltYield> SMELT_RECIPES = new HashMap<>();

    public static void registerYield(Item item, MoltenMetal metal, int amountMb, int cookTime) {
        SMELT_RECIPES.put(item, new SmeltYield(metal, amountMb, cookTime));
    }

    static {
        // --- IRON (100% Value Reclaim) ---
        registerYield(Items.IRON_INGOT, MoltenMetal.IRON, 90, 50);
        registerYield(Items.IRON_NUGGET, MoltenMetal.IRON, 10, 15);
        registerYield(Items.IRON_BLOCK, MoltenMetal.IRON, 810, 200);
        registerYield(Items.RAW_IRON, MoltenMetal.IRON, 90, 60);
        registerYield(Items.RAW_IRON_BLOCK, MoltenMetal.IRON, 810, 240);
        registerYield(ModItems.IRON_DUST, MoltenMetal.IRON, 90, 50);
        registerYield(Items.IRON_SWORD, MoltenMetal.IRON, 180, 80);
        registerYield(Items.IRON_PICKAXE, MoltenMetal.IRON, 270, 100);
        registerYield(Items.IRON_AXE, MoltenMetal.IRON, 270, 100);
        registerYield(Items.IRON_SHOVEL, MoltenMetal.IRON, 90, 60);
        registerYield(Items.IRON_HOE, MoltenMetal.IRON, 180, 80);
        registerYield(Items.IRON_HELMET, MoltenMetal.IRON, 450, 140);
        registerYield(Items.IRON_CHESTPLATE, MoltenMetal.IRON, 720, 180);
        registerYield(Items.IRON_LEGGINGS, MoltenMetal.IRON, 630, 160);
        registerYield(Items.IRON_BOOTS, MoltenMetal.IRON, 360, 120);
        registerYield(Items.IRON_HORSE_ARMOR, MoltenMetal.IRON, 630, 160);
        registerYield(Items.SHEARS, MoltenMetal.IRON, 180, 70);
        registerYield(Items.BUCKET, MoltenMetal.IRON, 270, 90);
        registerYield(Items.SHIELD, MoltenMetal.IRON, 90, 60);
        registerYield(Items.MINECART, MoltenMetal.IRON, 450, 140);
        registerYield(Items.HOPPER, MoltenMetal.IRON, 450, 140);
        registerYield(Items.CAULDRON, MoltenMetal.IRON, 630, 160);
        registerYield(Items.ANVIL, MoltenMetal.IRON, 2790, 300);
        registerYield(Items.CHIPPED_ANVIL, MoltenMetal.IRON, 2790, 300);
        registerYield(Items.DAMAGED_ANVIL, MoltenMetal.IRON, 2790, 300);
        registerYield(Items.IRON_DOOR, MoltenMetal.IRON, 180, 80);
        registerYield(Items.IRON_TRAPDOOR, MoltenMetal.IRON, 360, 120);
        registerYield(Items.IRON_BARS, MoltenMetal.IRON, 33, 30);
        Item chainItem = net.minecraft.registry.Registries.ITEM.get(net.minecraft.util.Identifier.of("minecraft", "chain"));
        if (chainItem != null && chainItem != Items.AIR) {
            registerYield(chainItem, MoltenMetal.IRON, 110, 60);
        }
        registerYield(Items.CHAINMAIL_HELMET, MoltenMetal.IRON, 450, 140);
        registerYield(Items.CHAINMAIL_CHESTPLATE, MoltenMetal.IRON, 720, 180);
        registerYield(Items.CHAINMAIL_LEGGINGS, MoltenMetal.IRON, 630, 160);
        registerYield(Items.CHAINMAIL_BOOTS, MoltenMetal.IRON, 360, 120);
        registerYield(Items.CROSSBOW, MoltenMetal.IRON, 90, 60);
        registerYield(Items.HEAVY_CORE, MoltenMetal.IRON, 810, 240);
        registerYield(Items.IRON_ORE, MoltenMetal.IRON, 90, 80);
        registerYield(Items.DEEPSLATE_IRON_ORE, MoltenMetal.IRON, 90, 80);
        registerYield(ModBlocks.NETHER_IRON_ORE.asItem(), MoltenMetal.IRON, 90, 80);

        // --- GOLD (100% Value Reclaim) ---
        registerYield(Items.GOLD_INGOT, MoltenMetal.GOLD, 90, 50);
        registerYield(Items.GOLD_NUGGET, MoltenMetal.GOLD, 10, 15);
        registerYield(Items.GOLD_BLOCK, MoltenMetal.GOLD, 810, 200);
        registerYield(Items.RAW_GOLD, MoltenMetal.GOLD, 90, 60);
        registerYield(Items.RAW_GOLD_BLOCK, MoltenMetal.GOLD, 810, 240);
        registerYield(ModItems.GOLD_DUST, MoltenMetal.GOLD, 90, 50);
        registerYield(Items.GOLDEN_SWORD, MoltenMetal.GOLD, 180, 80);
        registerYield(Items.GOLDEN_PICKAXE, MoltenMetal.GOLD, 270, 100);
        registerYield(Items.GOLDEN_AXE, MoltenMetal.GOLD, 270, 100);
        registerYield(Items.GOLDEN_SHOVEL, MoltenMetal.GOLD, 90, 60);
        registerYield(Items.GOLDEN_HOE, MoltenMetal.GOLD, 180, 80);
        registerYield(Items.GOLDEN_HELMET, MoltenMetal.GOLD, 450, 140);
        registerYield(Items.GOLDEN_CHESTPLATE, MoltenMetal.GOLD, 720, 180);
        registerYield(Items.GOLDEN_LEGGINGS, MoltenMetal.GOLD, 630, 160);
        registerYield(Items.GOLDEN_BOOTS, MoltenMetal.GOLD, 360, 120);
        registerYield(Items.GOLDEN_HORSE_ARMOR, MoltenMetal.GOLD, 630, 160);
        registerYield(Items.CLOCK, MoltenMetal.GOLD, 360, 120);
        registerYield(Items.GOLDEN_APPLE, MoltenMetal.GOLD, 720, 180);
        registerYield(Items.GLISTERING_MELON_SLICE, MoltenMetal.GOLD, 80, 50);
        registerYield(Items.GOLDEN_CARROT, MoltenMetal.GOLD, 80, 50);
        registerYield(Items.LIGHT_WEIGHTED_PRESSURE_PLATE, MoltenMetal.GOLD, 180, 80);
        registerYield(Items.GOLD_ORE, MoltenMetal.GOLD, 90, 80);
        registerYield(Items.DEEPSLATE_GOLD_ORE, MoltenMetal.GOLD, 90, 80);
        registerYield(Items.NETHER_GOLD_ORE, MoltenMetal.GOLD, 90, 80);

        // --- COPPER ---
        registerYield(Items.COPPER_INGOT, MoltenMetal.COPPER, 90, 50);
        registerYield(ModItems.COPPER_NUGGET, MoltenMetal.COPPER, 10, 15);
        registerYield(Items.COPPER_BLOCK, MoltenMetal.COPPER, 810, 200);
        registerYield(Items.RAW_COPPER, MoltenMetal.COPPER, 90, 60);
        registerYield(Items.RAW_COPPER_BLOCK, MoltenMetal.COPPER, 810, 240);
        registerYield(ModItems.COPPER_DUST, MoltenMetal.COPPER, 90, 50);
        registerYield(Items.COPPER_DOOR, MoltenMetal.COPPER, 180, 80);
        registerYield(Items.COPPER_TRAPDOOR, MoltenMetal.COPPER, 360, 120);
        registerYield(Items.COPPER_GRATE, MoltenMetal.COPPER, 360, 120);
        registerYield(Items.LIGHTNING_ROD, MoltenMetal.COPPER, 270, 90);
        registerYield(Items.SPYGLASS, MoltenMetal.COPPER, 180, 70);
        registerYield(Items.COPPER_ORE, MoltenMetal.COPPER, 90, 80);
        registerYield(Items.DEEPSLATE_COPPER_ORE, MoltenMetal.COPPER, 90, 80);
        registerYield(ModBlocks.NETHER_COPPER_ORE.asItem(), MoltenMetal.COPPER, 90, 80);

        // --- TIN ---
        registerYield(ModItems.TIN_INGOT, MoltenMetal.TIN, 90, 50);
        registerYield(ModItems.TIN_NUGGET, MoltenMetal.TIN, 10, 15);
        registerYield(ModBlocks.TIN_BLOCK.asItem(), MoltenMetal.TIN, 810, 200);
        registerYield(ModItems.RAW_TIN, MoltenMetal.TIN, 90, 60);
        registerYield(ModItems.TIN_DUST, MoltenMetal.TIN, 90, 50);
        registerYield(ModItems.TIN_SWORD, MoltenMetal.TIN, 180, 80);
        registerYield(ModItems.TIN_PICKAXE, MoltenMetal.TIN, 270, 100);
        registerYield(ModItems.TIN_AXE, MoltenMetal.TIN, 270, 100);
        registerYield(ModItems.TIN_SHOVEL, MoltenMetal.TIN, 90, 60);
        registerYield(ModItems.TIN_HOE, MoltenMetal.TIN, 180, 80);
        registerYield(ModItems.TIN_HELMET, MoltenMetal.TIN, 450, 140);
        registerYield(ModItems.TIN_CHESTPLATE, MoltenMetal.TIN, 720, 180);
        registerYield(ModItems.TIN_LEGGINGS, MoltenMetal.TIN, 630, 160);
        registerYield(ModItems.TIN_BOOTS, MoltenMetal.TIN, 360, 120);
        registerYield(ModBlocks.TIN_ORE.asItem(), MoltenMetal.TIN, 90, 80);
        registerYield(ModBlocks.DEEPSLATE_TIN_ORE.asItem(), MoltenMetal.TIN, 90, 80);
        registerYield(ModBlocks.NETHER_TIN_ORE.asItem(), MoltenMetal.TIN, 90, 80);

        // --- BRONZE ---
        registerYield(ModItems.BRONZE_INGOT, MoltenMetal.BRONZE, 90, 60);
        registerYield(ModItems.BRONZE_NUGGET, MoltenMetal.BRONZE, 10, 15);
        registerYield(ModBlocks.BRONZE_BLOCK.asItem(), MoltenMetal.BRONZE, 810, 220);
        registerYield(ModItems.BRONZE_DUST, MoltenMetal.BRONZE, 90, 60);
        registerYield(ModItems.BRONZE_SWORD, MoltenMetal.BRONZE, 180, 90);
        registerYield(ModItems.BRONZE_PICKAXE, MoltenMetal.BRONZE, 270, 110);
        registerYield(ModItems.BRONZE_AXE, MoltenMetal.BRONZE, 270, 110);
        registerYield(ModItems.BRONZE_SHOVEL, MoltenMetal.BRONZE, 90, 70);
        registerYield(ModItems.BRONZE_HOE, MoltenMetal.BRONZE, 180, 90);
        registerYield(ModItems.BRONZE_HELMET, MoltenMetal.BRONZE, 450, 150);
        registerYield(ModItems.BRONZE_CHESTPLATE, MoltenMetal.BRONZE, 720, 190);
        registerYield(ModItems.BRONZE_LEGGINGS, MoltenMetal.BRONZE, 630, 170);
        registerYield(ModItems.BRONZE_BOOTS, MoltenMetal.BRONZE, 360, 130);

        // --- STEEL ---
        registerYield(ModItems.STEEL_INGOT, MoltenMetal.STEEL, 90, 70);
        registerYield(ModItems.STEEL_NUGGET, MoltenMetal.STEEL, 10, 20);
        registerYield(ModBlocks.STEEL_BLOCK.asItem(), MoltenMetal.STEEL, 810, 240);
        registerYield(ModItems.STEEL_DUST, MoltenMetal.STEEL, 90, 70);
        registerYield(ModItems.STEEL_SWORD, MoltenMetal.STEEL, 180, 100);
        registerYield(ModItems.STEEL_PICKAXE, MoltenMetal.STEEL, 270, 120);
        registerYield(ModItems.STEEL_AXE, MoltenMetal.STEEL, 270, 120);
        registerYield(ModItems.STEEL_SHOVEL, MoltenMetal.STEEL, 90, 80);
        registerYield(ModItems.STEEL_HOE, MoltenMetal.STEEL, 180, 100);
        registerYield(ModItems.STEEL_HAMMER, MoltenMetal.STEEL, 450, 150);
        registerYield(ModItems.STEEL_HELMET, MoltenMetal.STEEL, 450, 160);
        registerYield(ModItems.STEEL_CHESTPLATE, MoltenMetal.STEEL, 720, 200);
        registerYield(ModItems.STEEL_LEGGINGS, MoltenMetal.STEEL, 630, 180);
        registerYield(ModItems.STEEL_BOOTS, MoltenMetal.STEEL, 360, 140);

        // --- TITANIUM ---
        registerYield(ModItems.TITANIUM_INGOT, MoltenMetal.TITANIUM, 90, 80);
        registerYield(ModItems.TITANIUM_NUGGET, MoltenMetal.TITANIUM, 10, 20);
        registerYield(ModBlocks.TITANIUM_BLOCK.asItem(), MoltenMetal.TITANIUM, 810, 260);
        registerYield(ModItems.RAW_TITANIUM, MoltenMetal.TITANIUM, 90, 90);
        registerYield(ModItems.TITANIUM_DUST, MoltenMetal.TITANIUM, 90, 80);
        registerYield(ModItems.TITANIUM_SWORD, MoltenMetal.TITANIUM, 180, 110);
        registerYield(ModItems.TITANIUM_PICKAXE, MoltenMetal.TITANIUM, 270, 130);
        registerYield(ModItems.TITANIUM_AXE, MoltenMetal.TITANIUM, 270, 130);
        registerYield(ModItems.TITANIUM_SHOVEL, MoltenMetal.TITANIUM, 90, 90);
        registerYield(ModItems.TITANIUM_HOE, MoltenMetal.TITANIUM, 180, 110);
        registerYield(ModItems.TITANIUM_HELMET, MoltenMetal.TITANIUM, 450, 170);
        registerYield(ModItems.TITANIUM_CHESTPLATE, MoltenMetal.TITANIUM, 720, 210);
        registerYield(ModItems.TITANIUM_LEGGINGS, MoltenMetal.TITANIUM, 630, 190);
        registerYield(ModItems.TITANIUM_BOOTS, MoltenMetal.TITANIUM, 360, 150);
        registerYield(ModBlocks.TITANIUM_ORE.asItem(), MoltenMetal.TITANIUM, 90, 100);
        registerYield(ModBlocks.DEEPSLATE_TITANIUM_ORE.asItem(), MoltenMetal.TITANIUM, 90, 100);

        // --- COBALT ---
        registerYield(ModItems.COBALT_INGOT, MoltenMetal.COBALT, 90, 80);
        registerYield(ModItems.COBALT_NUGGET, MoltenMetal.COBALT, 10, 20);
        registerYield(ModBlocks.COBALT_BLOCK.asItem(), MoltenMetal.COBALT, 810, 260);
        registerYield(ModItems.RAW_COBALT, MoltenMetal.COBALT, 90, 90);
        registerYield(ModItems.COBALT_DUST, MoltenMetal.COBALT, 90, 80);
        registerYield(ModItems.COBALT_SWORD, MoltenMetal.COBALT, 180, 110);
        registerYield(ModItems.COBALT_PICKAXE, MoltenMetal.COBALT, 270, 130);
        registerYield(ModItems.COBALT_AXE, MoltenMetal.COBALT, 270, 130);
        registerYield(ModItems.COBALT_SHOVEL, MoltenMetal.COBALT, 90, 90);
        registerYield(ModItems.COBALT_HOE, MoltenMetal.COBALT, 180, 110);
        registerYield(ModItems.COBALT_HAMMER, MoltenMetal.COBALT, 450, 160);
        registerYield(ModItems.COBALT_HELMET, MoltenMetal.COBALT, 450, 170);
        registerYield(ModItems.COBALT_CHESTPLATE, MoltenMetal.COBALT, 720, 210);
        registerYield(ModItems.COBALT_LEGGINGS, MoltenMetal.COBALT, 630, 190);
        registerYield(ModItems.COBALT_BOOTS, MoltenMetal.COBALT, 360, 150);
        registerYield(ModBlocks.COBALT_ORE.asItem(), MoltenMetal.COBALT, 90, 100);

        // --- ARDITE ---
        registerYield(ModItems.ARDITE_INGOT, MoltenMetal.ARDITE, 90, 80);
        registerYield(ModItems.ARDITE_NUGGET, MoltenMetal.ARDITE, 10, 20);
        registerYield(ModBlocks.ARDITE_BLOCK.asItem(), MoltenMetal.ARDITE, 810, 260);
        registerYield(ModItems.RAW_ARDITE, MoltenMetal.ARDITE, 90, 90);
        registerYield(ModItems.ARDITE_DUST, MoltenMetal.ARDITE, 90, 80);
        registerYield(ModItems.ARDITE_SWORD, MoltenMetal.ARDITE, 180, 110);
        registerYield(ModItems.ARDITE_PICKAXE, MoltenMetal.ARDITE, 270, 130);
        registerYield(ModItems.ARDITE_AXE, MoltenMetal.ARDITE, 270, 130);
        registerYield(ModItems.ARDITE_SHOVEL, MoltenMetal.ARDITE, 90, 90);
        registerYield(ModItems.ARDITE_HOE, MoltenMetal.ARDITE, 180, 110);
        registerYield(ModItems.ARDITE_HAMMER, MoltenMetal.ARDITE, 450, 160);
        registerYield(ModItems.ARDITE_HELMET, MoltenMetal.ARDITE, 450, 170);
        registerYield(ModItems.ARDITE_CHESTPLATE, MoltenMetal.ARDITE, 720, 210);
        registerYield(ModItems.ARDITE_LEGGINGS, MoltenMetal.ARDITE, 630, 190);
        registerYield(ModItems.ARDITE_BOOTS, MoltenMetal.ARDITE, 360, 150);
        registerYield(ModBlocks.ARDITE_ORE.asItem(), MoltenMetal.ARDITE, 90, 100);

        // --- MANYULLYN ---
        registerYield(ModItems.MANYULLYN_INGOT, MoltenMetal.MANYULLYN, 90, 90);
        registerYield(ModItems.MANYULLYN_NUGGET, MoltenMetal.MANYULLYN, 10, 25);
        registerYield(ModBlocks.MANYULLYN_BLOCK.asItem(), MoltenMetal.MANYULLYN, 810, 300);
        registerYield(ModItems.MANYULLYN_DUST, MoltenMetal.MANYULLYN, 90, 90);
        registerYield(ModItems.MANYULLYN_SWORD, MoltenMetal.MANYULLYN, 180, 120);
        registerYield(ModItems.MANYULLYN_PICKAXE, MoltenMetal.MANYULLYN, 270, 140);
        registerYield(ModItems.MANYULLYN_AXE, MoltenMetal.MANYULLYN, 270, 140);
        registerYield(ModItems.MANYULLYN_SHOVEL, MoltenMetal.MANYULLYN, 90, 100);
        registerYield(ModItems.MANYULLYN_HOE, MoltenMetal.MANYULLYN, 180, 120);
        registerYield(ModItems.MANYULLYN_HAMMER, MoltenMetal.MANYULLYN, 450, 180);
        registerYield(ModItems.MANYULLYN_HELMET, MoltenMetal.MANYULLYN, 450, 190);
        registerYield(ModItems.MANYULLYN_CHESTPLATE, MoltenMetal.MANYULLYN, 720, 240);
        registerYield(ModItems.MANYULLYN_LEGGINGS, MoltenMetal.MANYULLYN, 630, 220);
        registerYield(ModItems.MANYULLYN_BOOTS, MoltenMetal.MANYULLYN, 360, 170);

        // --- NETHERITE ---
        registerYield(Items.NETHERITE_SCRAP, MoltenMetal.NETHERITE, 90, 120);
        registerYield(Items.NETHERITE_BLOCK, MoltenMetal.NETHERITE, 810, 350);
        registerYield(ModItems.NETHERITE_DUST, MoltenMetal.NETHERITE, 90, 120);
    }

    public static @Nullable SmeltYield getYield(ItemStack stack) {
        if (stack.isEmpty()) return null;
        return SMELT_RECIPES.get(stack.getItem());
    }

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(ENERGY_CAPACITY, MAX_RECEIVE, 0, 0);
    private final Map<MoltenMetal, Integer> moltenFluids = new EnumMap<>(MoltenMetal.class);

    private int lavaAmount = 0;
    private int cookTime1 = 0;
    private int totalCookTime1 = 100;
    private int cookTime2 = 0;
    private int totalCookTime2 = 100;
    private boolean alloyingEnabled = false;

    // Dedicated Holding Tanks (Input 1 & Input 2)
    private MoltenMetal tank1Metal = MoltenMetal.NONE;
    private int tank1Amount = 0;
    private MoltenMetal tank2Metal = MoltenMetal.NONE;
    private int tank2Amount = 0;
    private boolean isEjectingHoldingTanks = false;

    public GearTier getActiveGearTier() {
        ItemStack gearStack = inventory.get(GEAR_SLOT);
        if (gearStack.isOf(ModItems.BLAZE_OVERCLOCK_CORE)) {
            return GearTier.BLAZE_OVERCLOCK;
        }
        if (gearStack.getItem() instanceof GearItem gearItem) {
            return gearItem.getGearTier();
        }
        return GearTier.NONE;
    }

    public float getSpeedMultiplier() {
        return switch (getActiveGearTier()) {
            case IRON, ENCHANTED_IRON -> 1.5f;
            case COPPER, BRONZE -> 2.0f;
            case ALUMINUM, STEEL, GOLD -> 2.5f;
            case TITANIUM, DIAMOND -> 3.0f;
            case NETHERITE -> 4.0f;
            case BLAZE_OVERCLOCK -> 6.0f;
            default -> 1.0f;
        };
    }

    public boolean hasMetallurgyChip() {
        return inventory.get(MODULE_SLOT).isOf(ModItems.METALLURGY_CONTROLLER_CHIP);
    }

    public boolean isAlloyingEnabled() {
        return alloyingEnabled && hasMetallurgyChip();
    }

    public void setAlloyingEnabled(boolean enabled) {
        this.alloyingEnabled = enabled;
        markDirty();
    }

    public MoltenMetal getTank1Metal() {
        return this.tank1Metal;
    }

    public int getTank1Amount() {
        return this.tank1Amount;
    }

    public MoltenMetal getTank2Metal() {
        return this.tank2Metal;
    }

    public int getTank2Amount() {
        return this.tank2Amount;
    }

    public boolean isEjectingHoldingTanks() {
        return this.isEjectingHoldingTanks;
    }

    public void purgeHoldingTanks() {
        this.tank1Amount = 0;
        this.tank1Metal = MoltenMetal.NONE;
        this.tank2Amount = 0;
        this.tank2Metal = MoltenMetal.NONE;
        this.isEjectingHoldingTanks = false;
        markDirty();
    }

    public void toggleEjectHoldingTanks() {
        if (this.tank1Amount > 0 || this.tank2Amount > 0) {
            this.isEjectingHoldingTanks = !this.isEjectingHoldingTanks;
            markDirty();
        } else {
            this.isEjectingHoldingTanks = false;
        }
    }

    public int getTotalMoltenVolume() {
        int total = 0;
        for (int amount : moltenFluids.values()) {
            total += amount;
        }
        return total;
    }

    public MoltenMetal getMostAbundantFluid() {
        MoltenMetal top = MoltenMetal.NONE;
        int max = 0;
        for (Map.Entry<MoltenMetal, Integer> entry : moltenFluids.entrySet()) {
            if (entry.getValue() > max) {
                max = entry.getValue();
                top = entry.getKey();
            }
        }
        return top;
    }

    protected final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> cookTime1;
                case 1 -> totalCookTime1;
                case 2 -> energyStorage.getEnergy() & 0xFFFF;
                case 3 -> (energyStorage.getEnergy() >> 16) & 0xFFFF;
                case 4 -> energyStorage.getMaxEnergy() & 0xFFFF;
                case 5 -> (energyStorage.getMaxEnergy() >> 16) & 0xFFFF;
                case 6 -> lavaAmount & 0xFFFF;
                case 7 -> (lavaAmount >> 16) & 0xFFFF;
                case 8 -> LAVA_CAPACITY & 0xFFFF;
                case 9 -> (LAVA_CAPACITY >> 16) & 0xFFFF;
                case 10 -> hasMetallurgyChip() ? 1 : 0;
                case 11 -> (alloyingEnabled && hasMetallurgyChip()) ? 1 : 0;
                case 12 -> getTotalMoltenVolume() & 0xFFFF;
                case 13 -> (getTotalMoltenVolume() >> 16) & 0xFFFF;
                case 14 -> getMostAbundantFluid().ordinal();
                case 15 -> getFluidAmount(getMostAbundantFluid()) & 0xFFFF;
                case 16 -> (getFluidAmount(getMostAbundantFluid()) >> 16) & 0xFFFF;
                case 17 -> getActiveGearTier().ordinal();
                case 18 -> cookTime2;
                case 19 -> totalCookTime2;
                case 20 -> tank1Metal.ordinal();
                case 21 -> tank1Amount & 0xFFFF;
                case 22 -> (tank1Amount >> 16) & 0xFFFF;
                case 23 -> tank2Metal.ordinal();
                case 24 -> tank2Amount & 0xFFFF;
                case 25 -> (tank2Amount >> 16) & 0xFFFF;
                case 26 -> isEjectingHoldingTanks ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> cookTime1 = value;
                case 1 -> totalCookTime1 = value;
                case 11 -> alloyingEnabled = (value == 1);
                case 18 -> cookTime2 = value;
                case 19 -> totalCookTime2 = value;
                case 26 -> isEjectingHoldingTanks = (value == 1);
            }
        }

        @Override
        public int size() {
            return 27;
        }
    };

    public InductionSmelterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INDUCTION_SMELTER_BE, pos, state);
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, InductionSmelterBlockEntity entity) {
        boolean dirty = false;

        // 1. Manual Lava Bucket Filling & Draining
        ItemStack lavaIn = entity.inventory.get(LAVA_IN_SLOT);
        ItemStack lavaOut = entity.inventory.get(LAVA_OUT_SLOT);
        if (!lavaIn.isEmpty()) {
            if (lavaIn.isOf(Items.LAVA_BUCKET) || lavaIn.isOf(ModItems.COPPER_LAVA_BUCKET) || lavaIn.isOf(ModItems.ENCHANTED_LAVA_BUCKET)) {
                if (entity.lavaAmount + 1000 <= LAVA_CAPACITY && (lavaOut.isEmpty() || (lavaOut.isOf(Items.BUCKET) && lavaOut.getCount() < lavaOut.getMaxCount()))) {
                    entity.lavaAmount += 1000;
                    lavaIn.decrement(1);
                    if (lavaOut.isEmpty()) {
                        entity.inventory.set(LAVA_OUT_SLOT, new ItemStack(Items.BUCKET));
                    } else {
                        lavaOut.increment(1);
                    }
                    dirty = true;
                }
            }
        }

        // 2. Perform Alloying Reaction (if Chip installed & switch ON)
        // Synthesizes from Tank 1 and Tank 2 into internal Tank 3 (moltenFluids)
        if (entity.isAlloyingEnabled()) {
            // Bronze synthesis: 30 mB Copper + 10 mB Tin -> 40 mB Bronze
            boolean t1Cu = entity.tank1Metal == MoltenMetal.COPPER && entity.tank1Amount >= 30;
            boolean t2Sn = entity.tank2Metal == MoltenMetal.TIN && entity.tank2Amount >= 10;
            boolean t2Cu = entity.tank2Metal == MoltenMetal.COPPER && entity.tank2Amount >= 30;
            boolean t1Sn = entity.tank1Metal == MoltenMetal.TIN && entity.tank1Amount >= 10;

            if ((t1Cu && t2Sn) || (t1Sn && t2Cu)) {
                if (entity.getTotalMoltenVolume() + 40 <= CHAMBER_CAPACITY) {
                    if (t1Cu) {
                        entity.tank1Amount -= 30;
                        entity.tank2Amount -= 10;
                    } else {
                        entity.tank2Amount -= 30;
                        entity.tank1Amount -= 10;
                    }
                    if (entity.tank1Amount <= 0) { entity.tank1Amount = 0; entity.tank1Metal = MoltenMetal.NONE; }
                    if (entity.tank2Amount <= 0) { entity.tank2Amount = 0; entity.tank2Metal = MoltenMetal.NONE; }
                    entity.moltenFluids.put(MoltenMetal.BRONZE, entity.getFluidAmount(MoltenMetal.BRONZE) + 40);
                    dirty = true;
                }
            }

            // Manyullyn synthesis: 10 mB Cobalt + 10 mB Ardite -> 20 mB Manyullyn
            boolean t1Co = entity.tank1Metal == MoltenMetal.COBALT && entity.tank1Amount >= 10;
            boolean t2Ar = entity.tank2Metal == MoltenMetal.ARDITE && entity.tank2Amount >= 10;
            boolean t2Co = entity.tank2Metal == MoltenMetal.COBALT && entity.tank2Amount >= 10;
            boolean t1Ar = entity.tank1Metal == MoltenMetal.ARDITE && entity.tank1Amount >= 10;

            if ((t1Co && t2Ar) || (t1Ar && t2Co)) {
                if (entity.getTotalMoltenVolume() + 20 <= CHAMBER_CAPACITY) {
                    if (t1Co) {
                        entity.tank1Amount -= 10;
                        entity.tank2Amount -= 10;
                    } else {
                        entity.tank2Amount -= 10;
                        entity.tank1Amount -= 10;
                    }
                    if (entity.tank1Amount <= 0) { entity.tank1Amount = 0; entity.tank1Metal = MoltenMetal.NONE; }
                    if (entity.tank2Amount <= 0) { entity.tank2Amount = 0; entity.tank2Metal = MoltenMetal.NONE; }
                    entity.moltenFluids.put(MoltenMetal.MANYULLYN, entity.getFluidAmount(MoltenMetal.MANYULLYN) + 20);
                    dirty = true;
                }
            }
        }

        // 3. Dual Simultaneous Melting Logic from Input Slots
        boolean hasChip = entity.hasMetallurgyChip();
        float speedMultiplier = entity.getSpeedMultiplier();
        boolean isSmelting1 = false;
        boolean isSmelting2 = false;

        // --- Slot 1 Melting ---
        ItemStack in1 = entity.inventory.get(INPUT_SLOT_1);
        SmeltYield y1 = getYield(in1);
        if (y1 != null && entity.canAcceptYieldSlot1(y1, hasChip)) {
            entity.totalCookTime1 = Math.max(10, (int) (y1.cookTime() / speedMultiplier));
            if (entity.energyStorage.getEnergy() >= ENERGY_DRAW && entity.lavaAmount >= LAVA_PER_SMELT) {
                entity.energyStorage.extractEnergy(ENERGY_DRAW, false);
                entity.cookTime1++;
                isSmelting1 = true;
                dirty = true;

                if (entity.cookTime1 >= entity.totalCookTime1) {
                    entity.cookTime1 = 0;
                    entity.lavaAmount -= LAVA_PER_SMELT;
                    in1.decrement(1);
                    if (hasChip) {
                        entity.insertHoldingTank1(y1.metal(), y1.amountMb());
                    } else {
                        entity.insertMoltenMetal(y1.metal(), y1.amountMb(), false);
                    }
                    dirty = true;
                }
            }
        } else {
            if (entity.cookTime1 > 0) {
                entity.cookTime1 = 0;
                dirty = true;
            }
        }

        // --- Slot 2 Melting ---
        ItemStack in2 = entity.inventory.get(INPUT_SLOT_2);
        SmeltYield y2 = getYield(in2);
        if (y2 != null && entity.canAcceptYieldSlot2(y2, hasChip)) {
            entity.totalCookTime2 = Math.max(10, (int) (y2.cookTime() / speedMultiplier));
            if (entity.energyStorage.getEnergy() >= ENERGY_DRAW && entity.lavaAmount >= LAVA_PER_SMELT) {
                entity.energyStorage.extractEnergy(ENERGY_DRAW, false);
                entity.cookTime2++;
                isSmelting2 = true;
                dirty = true;

                if (entity.cookTime2 >= entity.totalCookTime2) {
                    entity.cookTime2 = 0;
                    entity.lavaAmount -= LAVA_PER_SMELT;
                    in2.decrement(1);
                    if (hasChip) {
                        entity.insertHoldingTank2(y2.metal(), y2.amountMb());
                    } else {
                        entity.insertMoltenMetal(y2.metal(), y2.amountMb(), false);
                    }
                    dirty = true;
                }
            }
        } else {
            if (entity.cookTime2 > 0) {
                entity.cookTime2 = 0;
                dirty = true;
            }
        }

        // Update block LIT state
        boolean isSmelting = isSmelting1 || isSmelting2;
        boolean isLit = state.get(InductionSmelterBlock.LIT);
        if (isLit != isSmelting) {
            world.setBlockState(pos, state.with(InductionSmelterBlock.LIT, isSmelting), 3);
        }

        // 4. Auto-Eject Fluid into Connected Pipes or Tanks (ONLY Internal Tank 3!)
        if (world.getTime() % 2 == 0 && entity.getTotalMoltenVolume() > 0) {
            for (Direction dir : Direction.values()) {
                BlockEntity neighbor = world.getBlockEntity(pos.offset(dir));
                if (neighbor instanceof MoltenMetalProvider targetProvider && !(neighbor instanceof InductionSmelterBlockEntity)) {
                    for (MoltenMetal metal : MoltenMetal.values()) {
                        int available = entity.getFluidAmount(metal);
                        if (available > 0 && targetProvider.canInsertFluid(metal)) {
                            int toPush = Math.min(available, 100);
                            int accepted = targetProvider.insertFluid(metal, toPush, false);
                            if (accepted > 0) {
                                entity.extractFluid(metal, accepted, false);
                                dirty = true;
                                break;
                            }
                        }
                    }
                }
            }
        }

        // 5. Active Ejection of Holding Tanks 1 and 2 into connected pipes
        if (entity.isEjectingHoldingTanks) {
            boolean hasFluid = entity.tank1Amount > 0 || entity.tank2Amount > 0;
            if (!hasFluid) {
                entity.isEjectingHoldingTanks = false;
                dirty = true;
            } else {
                for (Direction dir : Direction.values()) {
                    BlockEntity neighbor = world.getBlockEntity(pos.offset(dir));
                    if (neighbor instanceof MoltenMetalProvider targetProvider && !(neighbor instanceof InductionSmelterBlockEntity)) {
                        // Eject from Tank 1
                        if (entity.tank1Amount > 0 && entity.tank1Metal != MoltenMetal.NONE && targetProvider.canInsertFluid(entity.tank1Metal)) {
                            int toPush = Math.min(entity.tank1Amount, 100);
                            int accepted = targetProvider.insertFluid(entity.tank1Metal, toPush, false);
                            if (accepted > 0) {
                                entity.tank1Amount -= accepted;
                                if (entity.tank1Amount <= 0) {
                                    entity.tank1Amount = 0;
                                    entity.tank1Metal = MoltenMetal.NONE;
                                }
                                dirty = true;
                            }
                        }
                        // Eject from Tank 2
                        if (entity.tank2Amount > 0 && entity.tank2Metal != MoltenMetal.NONE && targetProvider.canInsertFluid(entity.tank2Metal)) {
                            int toPush = Math.min(entity.tank2Amount, 100);
                            int accepted = targetProvider.insertFluid(entity.tank2Metal, toPush, false);
                            if (accepted > 0) {
                                entity.tank2Amount -= accepted;
                                if (entity.tank2Amount <= 0) {
                                    entity.tank2Amount = 0;
                                    entity.tank2Metal = MoltenMetal.NONE;
                                }
                                dirty = true;
                            }
                        }
                    }
                }
            }
        }

        if (dirty) {
            entity.markDirty();
        }
    }

    public boolean canAcceptYieldSlot1(SmeltYield yield, boolean hasChip) {
        if (hasChip) {
            if (tank1Amount == 0 || tank1Metal == MoltenMetal.NONE) {
                return yield.amountMb() <= HOLDING_TANK_CAPACITY;
            }
            return tank1Metal == yield.metal() && (tank1Amount + yield.amountMb() <= HOLDING_TANK_CAPACITY);
        } else {
            return getTotalMoltenVolume() + yield.amountMb() <= CHAMBER_CAPACITY;
        }
    }

    public boolean canAcceptYieldSlot2(SmeltYield yield, boolean hasChip) {
        if (hasChip) {
            if (tank2Amount == 0 || tank2Metal == MoltenMetal.NONE) {
                return yield.amountMb() <= HOLDING_TANK_CAPACITY;
            }
            return tank2Metal == yield.metal() && (tank2Amount + yield.amountMb() <= HOLDING_TANK_CAPACITY);
        } else {
            return getTotalMoltenVolume() + yield.amountMb() <= CHAMBER_CAPACITY;
        }
    }

    public void insertHoldingTank1(MoltenMetal metal, int amount) {
        this.tank1Metal = metal;
        this.tank1Amount = Math.min(HOLDING_TANK_CAPACITY, this.tank1Amount + amount);
        markDirty();
    }

    public void insertHoldingTank2(MoltenMetal metal, int amount) {
        this.tank2Metal = metal;
        this.tank2Amount = Math.min(HOLDING_TANK_CAPACITY, this.tank2Amount + amount);
        markDirty();
    }

    // ==========================================
    // FLUID & ENERGY IMPLEMENTATIONS
    // ==========================================
    @Override
    public MoltenMetal getFluidType() {
        return getMostAbundantFluid();
    }

    @Override
    public int getFluidAmount(MoltenMetal metal) {
        return moltenFluids.getOrDefault(metal, 0);
    }

    @Override
    public int getMaxFluid() {
        return CHAMBER_CAPACITY;
    }

    public int insertMoltenMetal(MoltenMetal metal, int amount, boolean simulate) {
        if (metal == MoltenMetal.NONE || metal == MoltenMetal.LAVA || amount <= 0) return 0;
        int currentTotal = getTotalMoltenVolume();
        int space = CHAMBER_CAPACITY - currentTotal;
        int insertable = Math.min(space, amount);
        if (!simulate && insertable > 0) {
            int current = moltenFluids.getOrDefault(metal, 0);
            moltenFluids.put(metal, current + insertable);
            markDirty();
        }
        return insertable;
    }

    @Override
    public boolean canInsertFluid(MoltenMetal metal) {
        if (metal == MoltenMetal.LAVA) {
            return canInsertLava();
        }
        return false; // Smelter produces molten metal from items, does not import molten metal
    }

    @Override
    public boolean canExtractFluid(MoltenMetal metal) {
        if (metal == MoltenMetal.LAVA) {
            return false; // Lava is thermal fuel, not extractable
        }
        return getFluidAmount(metal) > 0;
    }

    @Override
    public int insertFluid(MoltenMetal metal, int amount, boolean simulate) {
        if (metal == MoltenMetal.LAVA) {
            return insertLava(amount, simulate);
        }
        return 0; // Smelter does not take molten metal inputs
    }

    @Override
    public int extractFluid(MoltenMetal metal, int amount, boolean simulate) {
        if (metal == MoltenMetal.NONE || metal == MoltenMetal.LAVA || amount <= 0) return 0;
        int current = getFluidAmount(metal);
        int extractable = Math.min(current, amount);
        if (!simulate && extractable > 0) {
            int remaining = current - extractable;
            if (remaining <= 0) {
                moltenFluids.remove(metal);
            } else {
                moltenFluids.put(metal, remaining);
            }
            markDirty();
        }
        return extractable;
    }

    @Override
    public List<MoltenMetal> getContainedFluids() {
        List<MoltenMetal> list = new ArrayList<>();
        for (Map.Entry<MoltenMetal, Integer> entry : moltenFluids.entrySet()) {
            if (entry.getKey() != MoltenMetal.LAVA && entry.getValue() > 0) {
                list.add(entry.getKey());
            }
        }
        return list;
    }

    @Override
    public int getLavaAmount() {
        return this.lavaAmount;
    }

    @Override
    public int getMaxLava() {
        return LAVA_CAPACITY;
    }

    @Override
    public int insertLava(int amount, boolean simulate) {
        int space = LAVA_CAPACITY - this.lavaAmount;
        int insertable = Math.min(space, amount);
        if (!simulate && insertable > 0) {
            this.lavaAmount += insertable;
            markDirty();
        }
        return insertable;
    }

    @Override
    public int extractLava(int amount, boolean simulate) {
        return 0; // Lava is consumed as thermal fuel, not extracted
    }

    @Override
    public @Nullable EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }

    // ==========================================
    // INVENTORY & SIDED INVENTORY
    // ==========================================
    @Override
    public Text getDisplayName() {
        return Text.translatable("block.enchantedwood.induction_smelter");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new InductionSmelterScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        if (side == Direction.UP) {
            return new int[]{ INPUT_SLOT_1, INPUT_SLOT_2 };
        } else if (side == Direction.DOWN) {
            return new int[]{ LAVA_OUT_SLOT };
        } else {
            return new int[]{ INPUT_SLOT_1, INPUT_SLOT_2, LAVA_IN_SLOT, GEAR_SLOT, MODULE_SLOT, LAVA_OUT_SLOT };
        }
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == INPUT_SLOT_1 || slot == INPUT_SLOT_2) {
            return getYield(stack) != null;
        } else if (slot == GEAR_SLOT) {
            return stack.getItem() instanceof GearItem || stack.isOf(ModItems.BLAZE_OVERCLOCK_CORE);
        } else if (slot == MODULE_SLOT) {
            return stack.isOf(ModItems.METALLURGY_CONTROLLER_CHIP);
        } else if (slot == LAVA_IN_SLOT) {
            return stack.isOf(Items.LAVA_BUCKET) || stack.isOf(ModItems.COPPER_LAVA_BUCKET) || stack.isOf(ModItems.ENCHANTED_LAVA_BUCKET);
        }
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot == LAVA_OUT_SLOT;
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
    protected void readData(ReadView view) {
        super.readData(view);
        this.inventory.clear();
        Inventories.readData(view, this.inventory);
        this.energyStorage.readData(view);
        this.lavaAmount = view.getInt("LavaAmount", 0);
        this.cookTime1 = view.getInt("CookTime1", view.getInt("CookTime", 0));
        this.totalCookTime1 = view.getInt("TotalCookTime1", view.getInt("TotalCookTime", 100));
        this.cookTime2 = view.getInt("CookTime2", 0);
        this.totalCookTime2 = view.getInt("TotalCookTime2", 100);
        this.alloyingEnabled = view.getBoolean("AlloyingEnabled", false);
        this.isEjectingHoldingTanks = view.getBoolean("IsEjectingHoldingTanks", false);

        this.tank1Metal = MoltenMetal.fromId(view.getString("Tank1Metal", "none"));
        this.tank1Amount = view.getInt("Tank1Amount", 0);
        if (this.tank1Amount <= 0) this.tank1Metal = MoltenMetal.NONE;

        this.tank2Metal = MoltenMetal.fromId(view.getString("Tank2Metal", "none"));
        this.tank2Amount = view.getInt("Tank2Amount", 0);
        if (this.tank2Amount <= 0) this.tank2Metal = MoltenMetal.NONE;

        this.moltenFluids.clear();
        for (MoltenMetal metal : MoltenMetal.values()) {
            if (metal != MoltenMetal.NONE) {
                int amt = view.getInt("Fluid_" + metal.getId(), 0);
                if (amt > 0) {
                    if (metal == MoltenMetal.LAVA) {
                        this.lavaAmount = Math.min(LAVA_CAPACITY, this.lavaAmount + amt);
                    } else {
                        this.moltenFluids.put(metal, amt);
                    }
                }
            }
        }
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, this.inventory);
        this.energyStorage.writeData(view);
        view.putInt("LavaAmount", this.lavaAmount);
        view.putInt("CookTime1", this.cookTime1);
        view.putInt("TotalCookTime1", this.totalCookTime1);
        view.putInt("CookTime2", this.cookTime2);
        view.putInt("TotalCookTime2", this.totalCookTime2);
        view.putInt("CookTime", Math.max(this.cookTime1, this.cookTime2));
        view.putInt("TotalCookTime", Math.max(this.totalCookTime1, this.totalCookTime2));
        view.putBoolean("AlloyingEnabled", this.alloyingEnabled);
        view.putBoolean("IsEjectingHoldingTanks", this.isEjectingHoldingTanks);

        view.putString("Tank1Metal", this.tank1Metal.getId());
        view.putInt("Tank1Amount", this.tank1Amount);
        view.putString("Tank2Metal", this.tank2Metal.getId());
        view.putInt("Tank2Amount", this.tank2Amount);

        for (Map.Entry<MoltenMetal, Integer> entry : this.moltenFluids.entrySet()) {
            if (entry.getValue() > 0) {
                view.putInt("Fluid_" + entry.getKey().getId(), entry.getValue());
            }
        }
    }
}
