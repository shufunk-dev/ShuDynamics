package net.enchantedwood.block.entity;

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
import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.block.custom.PolymerLoomBlock;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.GearItem;
import net.enchantedwood.screen.PolymerLoomScreenHandler;
import org.jetbrains.annotations.Nullable;

public class PolymerLoomBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, SidedInventory, EnergyProvider {
    public static final int CAPACITY = 100_000;
    public static final int MAX_RECEIVE = 5_000;
    public static final int ENERGY_DRAW = 40; // 40 FE/t

    public static final int SLOT_POLYMER = 0;
    public static final int SLOT_FIBER = 1;
    public static final int SLOT_ADDITIVE = 2;
    public static final int SLOT_OUTPUT = 3;
    public static final int GEAR_SLOT = 4;
    public static final int INVENTORY_SIZE = 5;

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(CAPACITY, MAX_RECEIVE, MAX_RECEIVE, 0);

    private int cookTime = 0;
    private int totalCookTime = 140; // 7 seconds base

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

    public PolymerLoomBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POLYMER_LOOM_BE, pos, state);
    }

    public GearTier getActiveGearTier() {
        ItemStack gearStack = inventory.get(GEAR_SLOT);
        if (!gearStack.isEmpty() && gearStack.getItem() instanceof GearItem gearItem) {
            return gearItem.getGearTier();
        }
        return GearTier.NONE;
    }

    public static int getTierCookTime(GearTier tier) {
        return switch (tier) {
            case IRON -> 110;
            case COPPER -> 95;
            case BRONZE -> 80;
            case GOLD -> 60;
            case DIAMOND -> 40;
            case NETHERITE -> 20;
            case BLAZE_OVERCLOCK -> 15;
            default -> 140;
        };
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, PolymerLoomBlockEntity entity) {
        GearTier tier = entity.getActiveGearTier();
        entity.totalCookTime = getTierCookTime(tier);

        ItemStack output = entity.getMatchingOutput();

        if (!output.isEmpty() && entity.canOutput(output)) {
            int energyRequired = ENERGY_DRAW * (1 + (tier.ordinal() * 2));
            if (entity.energyStorage.getEnergy() >= energyRequired) {
                entity.energyStorage.extractEnergy(energyRequired, false);
                entity.cookTime++;

                if (!state.get(PolymerLoomBlock.LIT)) {
                    world.setBlockState(pos, state.with(PolymerLoomBlock.LIT, true));
                }

                if (entity.cookTime >= entity.totalCookTime) {
                    entity.craft(output);
                    entity.cookTime = 0;
                }
                entity.markDirty();
                return;
            }
        }

        if (entity.cookTime > 0) {
            entity.cookTime = Math.max(0, entity.cookTime - 2);
            entity.markDirty();
        }

        if (state.get(PolymerLoomBlock.LIT)) {
            world.setBlockState(pos, state.with(PolymerLoomBlock.LIT, false));
        }
    }

    private ItemStack getMatchingOutput() {
        ItemStack slot0 = inventory.get(SLOT_POLYMER);
        ItemStack slot1 = inventory.get(SLOT_FIBER);
        ItemStack slot2 = inventory.get(SLOT_ADDITIVE);

        if (slot0.isEmpty() || slot1.isEmpty() || slot2.isEmpty()) return ItemStack.EMPTY;

        Item item0 = slot0.getItem();
        Item item1 = slot1.getItem();
        Item item2 = slot2.getItem();

        // 1. Sterile Polymer Fabric (4×): Rubber + String + Silicon
        if (item0 == ModItems.RUBBER &&
            item1 == Items.STRING &&
            item2 == ModItems.SILICON) {
            return new ItemStack(ModItems.STERILE_POLYMER_FABRIC, 4);
        }

        // 2. Cleanroom Hood: Sterile Fabric + Glass Pane + Tin Ingot
        if (item0 == ModItems.STERILE_POLYMER_FABRIC &&
            item1 == Items.GLASS_PANE &&
            item2 == ModItems.TIN_INGOT) {
            return new ItemStack(ModItems.CLEANROOM_HOOD);
        }

        // 3. Cleanroom Smock: Sterile Fabric + Titanium Ingot + Steel Ingot
        if (item0 == ModItems.STERILE_POLYMER_FABRIC &&
            item1 == ModItems.TITANIUM_INGOT &&
            item2 == ModItems.STEEL_INGOT) {
            return new ItemStack(ModItems.CLEANROOM_SMOCK);
        }

        // 4. Cleanroom Trousers: Sterile Fabric + Aluminum Ingot + Rubber
        if (item0 == ModItems.STERILE_POLYMER_FABRIC &&
            item1 == ModItems.ALUMINUM_INGOT &&
            item2 == ModItems.RUBBER) {
            return new ItemStack(ModItems.CLEANROOM_TROUSERS);
        }

        // 5. Cleanroom Booties: Sterile Fabric + Rubber + Titanium Ingot
        if (item0 == ModItems.STERILE_POLYMER_FABRIC &&
            item1 == ModItems.RUBBER &&
            item2 == ModItems.TITANIUM_INGOT) {
            return new ItemStack(ModItems.CLEANROOM_BOOTIES);
        }

        return ItemStack.EMPTY;
    }

    private boolean canOutput(ItemStack output) {
        ItemStack currentOut = inventory.get(SLOT_OUTPUT);
        if (currentOut.isEmpty()) return true;
        if (!ItemStack.areItemsAndComponentsEqual(currentOut, output)) return false;
        return currentOut.getCount() + output.getCount() <= currentOut.getMaxCount();
    }

    private void craft(ItemStack output) {
        inventory.get(SLOT_POLYMER).decrement(1);
        inventory.get(SLOT_FIBER).decrement(1);
        inventory.get(SLOT_ADDITIVE).decrement(1);

        ItemStack currentOut = inventory.get(SLOT_OUTPUT);
        if (currentOut.isEmpty()) {
            inventory.set(SLOT_OUTPUT, output.copy());
        } else {
            currentOut.increment(output.getCount());
        }
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.inventory.clear();
        Inventories.readData(view, this.inventory);
        this.energyStorage.readData(view);
        this.cookTime = view.getInt("CookTime", 0);
        this.totalCookTime = view.getInt("TotalCookTime", 140);
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
        if (side == Direction.DOWN) return new int[]{SLOT_OUTPUT};
        if (side == Direction.UP) return new int[]{SLOT_POLYMER, SLOT_FIBER, SLOT_ADDITIVE};
        return new int[]{SLOT_POLYMER, SLOT_FIBER, SLOT_ADDITIVE, SLOT_OUTPUT};
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == SLOT_OUTPUT) return false;
        if (slot == GEAR_SLOT) return stack.getItem() instanceof GearItem;
        if (slot == SLOT_POLYMER) return stack.isOf(ModItems.RUBBER) || stack.isOf(ModItems.STERILE_POLYMER_FABRIC);
        if (slot == SLOT_FIBER) return stack.isOf(Items.STRING) || stack.isOf(Items.WHITE_WOOL) || stack.isOf(Items.GLASS_PANE) || stack.isOf(ModItems.ALUMINUM_INGOT) || stack.isOf(ModItems.RUBBER);
        if (slot == SLOT_ADDITIVE) return stack.isOf(ModItems.SILICON) || stack.isOf(ModItems.SILICON_WAFER) || stack.isOf(ModItems.TITANIUM_NUGGET) || stack.isOf(ModItems.RUBBER);
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot == SLOT_OUTPUT;
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
    public Text getDisplayName() {
        return Text.literal("Polymer Loom");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new PolymerLoomScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    public EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }
}
