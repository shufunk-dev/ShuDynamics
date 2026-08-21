package net.enchantedwood.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.screen.EnchantedDriveBayScreenHandler;
import org.jetbrains.annotations.Nullable;

public class EnchantedDriveBayBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, SidedInventory {
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(6, ItemStack.EMPTY);

    protected final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            if (index == 0) {
                return getTotalNetworkCapacity();
            }
            if (index == 1) {
                return getTotalStoredItems();
            }
            if (index >= 2 && index <= 7) {
                return getDriveState(index - 2);
            }
            return 0;
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int size() {
            return 8; // 0=cap, 1=stored, 2..7=drive states (0..5)
        }
    };

    public EnchantedDriveBayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENCHANTED_DRIVE_BAY_BLOCK_ENTITY, pos, state);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.enchantedwood.enchanted_drive_bay");
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new EnchantedDriveBayScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    public int getDriveCapacity(int slot) {
        if (slot < 0 || slot >= inventory.size()) return 0;
        ItemStack stack = inventory.get(slot);
        if (stack.isOf(ModItems.STORAGE_CRYSTAL_1K)) return 1000;
        if (stack.isOf(ModItems.STORAGE_CRYSTAL_4K)) return 4000;
        if (stack.isOf(ModItems.STORAGE_CRYSTAL_16K)) return 16000;
        if (stack.isOf(ModItems.STORAGE_CRYSTAL_64K)) return 64000;
        return 0;
    }

    public int getTotalNetworkCapacity() {
        int capacity = 0;
        for (int i = 0; i < 6; i++) {
            capacity += getDriveCapacity(i);
        }
        return capacity;
    }

    public int getTotalStoredItems() {
        if (this.world == null) return 0;
        BlockPos.Mutable mut = new BlockPos.Mutable();
        for (int dx = -16; dx <= 16; dx++) {
            for (int dy = -8; dy <= 8; dy++) {
                for (int dz = -16; dz <= 16; dz++) {
                    mut.set(this.pos.getX() + dx, this.pos.getY() + dy, this.pos.getZ() + dz);
                    BlockEntity be = this.world.getBlockEntity(mut);
                    if (be instanceof EnchantedStorageTerminalBlockEntity terminal) {
                        return terminal.getStoredItemCount();
                    }
                }
            }
        }
        return 0;
    }

    /**
     * Drive LED states:
     * -1: No drive installed
     *  0: Green (Empty drive / 0 items)
     *  1: Yellow (At least 1 item stored, < 80%)
     *  2: Purple (80% to 99% capacity warning)
     *  3: Red (100% Full)
     */
    public int getDriveState(int slot) {
        int cap = getDriveCapacity(slot);
        if (cap <= 0) return -1; // Empty socket

        int totalStored = getTotalStoredItems();
        
        // Calculate items prior to this slot
        int priorCap = 0;
        for (int i = 0; i < slot; i++) {
            priorCap += getDriveCapacity(i);
        }

        int itemsInThisDrive = Math.max(0, Math.min(cap, totalStored - priorCap));

        if (itemsInThisDrive == 0) {
            return 0; // Green (Empty)
        } else if (itemsInThisDrive >= cap) {
            return 3; // Red (Full)
        } else if (itemsInThisDrive >= (int) (cap * 0.80)) {
            return 2; // Purple (80%+ warning)
        } else {
            return 1; // Yellow (1+ item stored)
        }
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        Inventories.readData(view, this.inventory);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, this.inventory);
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return new int[]{0, 1, 2, 3, 4, 5};
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return stack.isOf(ModItems.STORAGE_CRYSTAL_1K)
                || stack.isOf(ModItems.STORAGE_CRYSTAL_4K)
                || stack.isOf(ModItems.STORAGE_CRYSTAL_16K)
                || stack.isOf(ModItems.STORAGE_CRYSTAL_64K);
    }

    public boolean canRemoveDrive(int slot) {
        int capWithoutThis = getTotalNetworkCapacity() - getDriveCapacity(slot);
        return getTotalStoredItems() <= capWithoutThis;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return canRemoveDrive(slot);
    }

    @Override
    public int size() {
        return inventory.size();
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
        if (!canRemoveDrive(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack result = Inventories.splitStack(inventory, slot, amount);
        markDirty();
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        if (!canRemoveDrive(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack result = Inventories.removeStack(inventory, slot);
        markDirty();
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
}
