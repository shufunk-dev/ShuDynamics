package net.enchantedwood.block.entity;

import net.enchantedwood.item.ModItems;
import net.enchantedwood.screen.EnchantedDriveBayScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class EnchantedDriveBayBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer {
    private final NonNullList<ItemStack> inventory = NonNullList.withSize(6, ItemStack.EMPTY);

    protected final ContainerData propertyDelegate = new ContainerData() {
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
        public int getCount() {
            return 8; // 0=cap, 1=stored, 2..7=drive states (0..5)
        }
    };

    public EnchantedDriveBayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENCHANTED_DRIVE_BAY_BLOCK_ENTITY, pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.enchantedwood.enchanted_drive_bay");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new EnchantedDriveBayScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    public int getDriveCapacity(int slot) {
        if (slot < 0 || slot >= inventory.size()) return 0;
        ItemStack stack = inventory.get(slot);
        if (stack.is(ModItems.STORAGE_CRYSTAL_1K)) return 1000;
        if (stack.is(ModItems.STORAGE_CRYSTAL_4K)) return 4000;
        if (stack.is(ModItems.STORAGE_CRYSTAL_16K)) return 16000;
        if (stack.is(ModItems.STORAGE_CRYSTAL_64K)) return 64000;
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
        if (this.level == null) return 0;
        BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
        for (int dx = -16; dx <= 16; dx++) {
            for (int dy = -8; dy <= 8; dy++) {
                for (int dz = -16; dz <= 16; dz++) {
                    mut.set(this.worldPosition.getX() + dx, this.worldPosition.getY() + dy, this.worldPosition.getZ() + dz);
                    BlockEntity be = this.level.getBlockEntity(mut);
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
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        ContainerHelper.loadAllItems(view, this.inventory);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        ContainerHelper.saveAllItems(view, this.inventory);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[]{0, 1, 2, 3, 4, 5};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return stack.is(ModItems.STORAGE_CRYSTAL_1K)
                || stack.is(ModItems.STORAGE_CRYSTAL_4K)
                || stack.is(ModItems.STORAGE_CRYSTAL_16K)
                || stack.is(ModItems.STORAGE_CRYSTAL_64K);
    }

    public boolean canRemoveDrive(int slot) {
        int capWithoutThis = getTotalNetworkCapacity() - getDriveCapacity(slot);
        return getTotalStoredItems() <= capWithoutThis;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return canRemoveDrive(slot);
    }

    @Override
    public int getContainerSize() {
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
    public ItemStack getItem(int slot) {
        return inventory.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (!canRemoveDrive(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack result = ContainerHelper.removeItem(inventory, slot, amount);
        setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (!canRemoveDrive(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack result = ContainerHelper.takeItem(inventory, slot);
        setChanged();
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
}
