package net.enchantedwood.block.entity;

import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.block.custom.PolymerLoomBlock;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.GearItem;
import net.enchantedwood.screen.PolymerLoomScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class PolymerLoomBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, EnergyProvider {
    public static final int CAPACITY = 100_000;
    public static final int MAX_RECEIVE = 5_000;
    public static final int ENERGY_DRAW = 40; // 40 FE/t

    public static final int SLOT_POLYMER = 0;
    public static final int SLOT_FIBER = 1;
    public static final int SLOT_ADDITIVE = 2;
    public static final int SLOT_OUTPUT = 3;
    public static final int GEAR_SLOT = 4;
    public static final int INVENTORY_SIZE = 5;

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(CAPACITY, MAX_RECEIVE, MAX_RECEIVE, 0);

    private int cookTime = 0;
    private int totalCookTime = 140; // 7 seconds base

    protected final ContainerData propertyDelegate = new ContainerData() {
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
        public int getCount() {
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

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, PolymerLoomBlockEntity entity) {
        GearTier tier = entity.getActiveGearTier();
        entity.totalCookTime = getTierCookTime(tier);

        ItemStack output = entity.getMatchingOutput();

        if (!output.isEmpty() && entity.canOutput(output)) {
            int energyRequired = ENERGY_DRAW * (1 + (tier.ordinal() * 2));
            if (entity.energyStorage.getEnergy() >= energyRequired) {
                entity.energyStorage.extractEnergy(energyRequired, false);
                entity.cookTime++;

                if (!state.getValue(PolymerLoomBlock.LIT)) {
                    world.setBlockAndUpdate(pos, state.setValue(PolymerLoomBlock.LIT, true));
                }

                if (entity.cookTime >= entity.totalCookTime) {
                    entity.craft(output);
                    entity.cookTime = 0;
                }
                entity.setChanged();
                return;
            }
        }

        if (entity.cookTime > 0) {
            entity.cookTime = Math.max(0, entity.cookTime - 2);
            entity.setChanged();
        }

        if (state.getValue(PolymerLoomBlock.LIT)) {
            world.setBlockAndUpdate(pos, state.setValue(PolymerLoomBlock.LIT, false));
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
        if (!ItemStack.isSameItemSameComponents(currentOut, output)) return false;
        return currentOut.getCount() + output.getCount() <= currentOut.getMaxStackSize();
    }

    private void craft(ItemStack output) {
        inventory.get(SLOT_POLYMER).shrink(1);
        inventory.get(SLOT_FIBER).shrink(1);
        inventory.get(SLOT_ADDITIVE).shrink(1);

        ItemStack currentOut = inventory.get(SLOT_OUTPUT);
        if (currentOut.isEmpty()) {
            inventory.set(SLOT_OUTPUT, output.copy());
        } else {
            currentOut.grow(output.getCount());
        }
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.inventory.clear();
        ContainerHelper.loadAllItems(view, this.inventory);
        this.energyStorage.readData(view);
        this.cookTime = view.getIntOr("CookTime", 0);
        this.totalCookTime = view.getIntOr("TotalCookTime", 140);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        ContainerHelper.saveAllItems(view, this.inventory);
        this.energyStorage.writeData(view);
        view.putInt("CookTime", this.cookTime);
        view.putInt("TotalCookTime", this.totalCookTime);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) return new int[]{SLOT_OUTPUT};
        if (side == Direction.UP) return new int[]{SLOT_POLYMER, SLOT_FIBER, SLOT_ADDITIVE};
        return new int[]{SLOT_POLYMER, SLOT_FIBER, SLOT_ADDITIVE, SLOT_OUTPUT};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == SLOT_OUTPUT) return false;
        if (slot == GEAR_SLOT) return stack.getItem() instanceof GearItem;
        if (slot == SLOT_POLYMER) return stack.is(ModItems.RUBBER) || stack.is(ModItems.STERILE_POLYMER_FABRIC);
        if (slot == SLOT_FIBER) return stack.is(Items.STRING) || stack.is(Items.WOOL.white()) || stack.is(Items.GLASS_PANE) || stack.is(ModItems.ALUMINUM_INGOT) || stack.is(ModItems.RUBBER);
        if (slot == SLOT_ADDITIVE) return stack.is(ModItems.SILICON) || stack.is(ModItems.SILICON_WAFER) || stack.is(ModItems.TITANIUM_NUGGET) || stack.is(ModItems.RUBBER);
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot == SLOT_OUTPUT;
    }

    @Override
    public int getContainerSize() {
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
    public ItemStack getItem(int slot) {
        return inventory.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(inventory, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = ContainerHelper.takeItem(inventory, slot);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        inventory.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        inventory.clear();
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Polymer Loom");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new PolymerLoomScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    public EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }
}
