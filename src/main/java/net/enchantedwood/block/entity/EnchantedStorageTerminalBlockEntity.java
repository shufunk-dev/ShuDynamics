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
import net.enchantedwood.block.custom.EnchantedStorageControllerBlock;
import net.enchantedwood.screen.EnchantedStorageTerminalScreenHandler;
import org.jetbrains.annotations.Nullable;

public class EnchantedStorageTerminalBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, SidedInventory {
    public static final int STORAGE_SLOTS = 54; // 6 rows x 9 columns
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(STORAGE_SLOTS, ItemStack.EMPTY);

    protected final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> getNetworkCapacity();
                case 1 -> getStoredItemCount();
                case 2 -> isNetworkOnline() ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int size() {
            return 3;
        }
    };

    public EnchantedStorageTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENCHANTED_STORAGE_TERMINAL_BLOCK_ENTITY, pos, state);
    }

    public int getStoredItemCount() {
        int count = 0;
        for (ItemStack stack : inventory) {
            if (!stack.isEmpty()) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public int getNetworkCapacity() {
        if (this.world == null) return 0;
        int totalCap = 0;
        // Search within 16 blocks for connected drive bays
        BlockPos.Mutable mut = new BlockPos.Mutable();
        for (int dx = -16; dx <= 16; dx++) {
            for (int dy = -8; dy <= 8; dy++) {
                for (int dz = -16; dz <= 16; dz++) {
                    mut.set(this.pos.getX() + dx, this.pos.getY() + dy, this.pos.getZ() + dz);
                    BlockEntity be = this.world.getBlockEntity(mut);
                    if (be instanceof EnchantedDriveBayBlockEntity driveBay) {
                        totalCap += driveBay.getTotalNetworkCapacity();
                    }
                }
            }
        }
        return totalCap; // 0 if no drives installed
    }

    public boolean isNetworkOnline() {
        if (this.world == null) return true;
        // Check for nearby controller power
        BlockPos.Mutable mut = new BlockPos.Mutable();
        for (int dx = -16; dx <= 16; dx++) {
            for (int dy = -8; dy <= 8; dy++) {
                for (int dz = -16; dz <= 16; dz++) {
                    mut.set(this.pos.getX() + dx, this.pos.getY() + dy, this.pos.getZ() + dz);
                    BlockState bs = this.world.getBlockState(mut);
                    if (bs.getBlock() instanceof EnchantedStorageControllerBlock) {
                        return bs.get(EnchantedStorageControllerBlock.LIT);
                    }
                }
            }
        }
        return true;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.enchantedwood.enchanted_storage_terminal");
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new EnchantedStorageTerminalScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
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
        int[] slots = new int[STORAGE_SLOTS];
        for (int i = 0; i < STORAGE_SLOTS; i++) slots[i] = i;
        return slots;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return getStoredItemCount() + stack.getCount() <= getNetworkCapacity();
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return true;
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
        ItemStack result = Inventories.splitStack(inventory, slot, amount);
        markDirty();
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
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
