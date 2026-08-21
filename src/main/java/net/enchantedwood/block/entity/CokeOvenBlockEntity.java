package net.enchantedwood.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
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
import net.enchantedwood.block.custom.CokeOvenBlock;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.screen.CokeOvenScreenHandler;
import org.jetbrains.annotations.Nullable;

public class CokeOvenBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, SidedInventory {
    public static final int TOTAL_COOK_TIME = 200; // 10 seconds per Coke Coal

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(2, ItemStack.EMPTY);
    private int cookTime = 0;

    protected final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> cookTime;
                case 1 -> TOTAL_COOK_TIME;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) cookTime = value;
        }

        @Override
        public int size() {
            return 2;
        }
    };

    public CokeOvenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COKE_OVEN_BLOCK_ENTITY, pos, state);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.enchantedwood.coke_oven");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new CokeOvenScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, CokeOvenBlockEntity entity) {
        boolean stateChanged = false;

        ItemStack input = entity.inventory.get(0);
        ItemStack output = entity.inventory.get(1);

        boolean hasValidInput = input.isOf(Items.COAL) || input.isOf(Items.CHARCOAL);
        boolean hasOutputSpace = output.isEmpty() || (output.isOf(ModItems.COKE_COAL) && output.getCount() < output.getMaxCount());

        if (hasValidInput && hasOutputSpace) {
            entity.cookTime++;
            if (entity.cookTime >= TOTAL_COOK_TIME) {
                entity.cookTime = 0;
                input.decrement(1);
                if (output.isEmpty()) {
                    entity.inventory.set(1, new ItemStack(ModItems.COKE_COAL));
                } else {
                    output.increment(1);
                }
            }
            stateChanged = true;
        } else {
            if (entity.cookTime > 0) {
                entity.cookTime = Math.max(0, entity.cookTime - 2);
                stateChanged = true;
            }
        }

        boolean isCookingNow = hasValidInput && hasOutputSpace;
        if (state.get(CokeOvenBlock.LIT) != isCookingNow) {
            world.setBlockState(pos, state.with(CokeOvenBlock.LIT, isCookingNow), 3);
            stateChanged = true;
        }

        if (stateChanged) {
            markDirty(world, pos, state);
        }
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.inventory.clear();
        Inventories.readData(view, this.inventory);
        this.cookTime = view.getInt("CookTime", 0);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, this.inventory);
        view.putInt("CookTime", this.cookTime);
    }

    // SidedInventory
    @Override
    public int[] getAvailableSlots(Direction side) {
        if (side == Direction.DOWN) {
            return new int[]{1}; // Output
        }
        return new int[]{0}; // Input
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot == 0 && (stack.isOf(Items.COAL) || stack.isOf(Items.CHARCOAL));
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot == 1;
    }

    @Override
    public int size() {
        return inventory.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : inventory) {
            if (!s.isEmpty()) return false;
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
        markDirty();
    }
}
