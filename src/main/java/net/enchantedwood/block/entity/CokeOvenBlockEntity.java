package net.enchantedwood.block.entity;

import net.enchantedwood.block.custom.CokeOvenBlock;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.screen.CokeOvenScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class CokeOvenBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer {
    public static final int TOTAL_COOK_TIME = 200; // 10 seconds per Coke Coal
    public static final int INVENTORY_SIZE = 3;

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int TAR_SLOT = 2;

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private int cookTime = 0;

    protected final ContainerData propertyDelegate = new ContainerData() {
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
        public int getCount() {
            return 2;
        }
    };

    public CokeOvenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COKE_OVEN_BLOCK_ENTITY, pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.enchantedwood.coke_oven");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new CokeOvenScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, CokeOvenBlockEntity entity) {
        boolean stateChanged = false;

        ItemStack input = entity.inventory.get(INPUT_SLOT);
        ItemStack output = entity.inventory.get(OUTPUT_SLOT);
        ItemStack tarOutput = entity.inventory.get(TAR_SLOT);

        boolean hasValidInput = isValidInput(input);
        boolean hasOutputSpace = output.isEmpty() || (output.is(ModItems.COKE_COAL) && output.getCount() < output.getMaxStackSize());
        boolean hasTarSpace = tarOutput.isEmpty() || (tarOutput.is(ModItems.MINERAL_TAR) && tarOutput.getCount() < tarOutput.getMaxStackSize());

        if (hasValidInput && hasOutputSpace && hasTarSpace) {
            entity.cookTime++;
            if (entity.cookTime >= TOTAL_COOK_TIME) {
                entity.cookTime = 0;
                input.shrink(1);

                // 1. Primary Output: Coke Coal
                if (output.isEmpty()) {
                    entity.inventory.set(OUTPUT_SLOT, new ItemStack(ModItems.COKE_COAL));
                } else {
                    output.grow(1);
                }

                // 2. Byproduct Output: Mineral Tar
                if (tarOutput.isEmpty()) {
                    entity.inventory.set(TAR_SLOT, new ItemStack(ModItems.MINERAL_TAR));
                } else {
                    tarOutput.grow(1);
                }
            }
            stateChanged = true;
        } else {
            if (entity.cookTime > 0) {
                entity.cookTime = Math.max(0, entity.cookTime - 2);
                stateChanged = true;
            }
        }

        boolean isCookingNow = hasValidInput && hasOutputSpace && hasTarSpace;
        if (state.getValue(CokeOvenBlock.LIT) != isCookingNow) {
            world.setBlock(pos, state.setValue(CokeOvenBlock.LIT, isCookingNow), 3);
            stateChanged = true;
        }

        if (stateChanged) {
            setChanged(world, pos, state);
        }
    }

    private static boolean isValidInput(ItemStack stack) {
        return stack.is(Items.COAL) || stack.is(Items.CHARCOAL) || stack.is(ItemTags.LOGS);
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.inventory.clear();
        ContainerHelper.loadAllItems(view, this.inventory);
        this.cookTime = view.getIntOr("CookTime", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        ContainerHelper.saveAllItems(view, this.inventory);
        view.putInt("CookTime", this.cookTime);
    }

    // SidedInventory
    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return new int[]{OUTPUT_SLOT, TAR_SLOT};
        }
        return new int[]{INPUT_SLOT, OUTPUT_SLOT, TAR_SLOT};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot == INPUT_SLOT && isValidInput(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot == OUTPUT_SLOT || slot == TAR_SLOT;
    }

    @Override
    public int getContainerSize() {
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
        setChanged();
    }
}
