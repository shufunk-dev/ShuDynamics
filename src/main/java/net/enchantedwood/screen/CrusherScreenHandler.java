package net.enchantedwood.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.enchantedwood.item.custom.GearItem;

public class CrusherScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    public CrusherScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(3), new ArrayPropertyDelegate(7));
    }

    public CrusherScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.CRUSHER_SCREEN_HANDLER, syncId);
        checkSize(inventory, 3);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        inventory.onOpen(playerInventory.player);
        this.addProperties(propertyDelegate);

        // Machine Slots
        // Slot 0: Ore Input (top center)
        this.addSlot(new Slot(inventory, 0, 56, 17));
        // Slot 1: Gear Upgrade Slot (bottom center, under arrow)
        this.addSlot(new Slot(inventory, 1, 74, 53) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.getItem() instanceof GearItem;
            }
        });
        // Slot 2: Output Slot (right)
        this.addSlot(new Slot(inventory, 2, 116, 35) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }

            @Override
            public void onTakeItem(PlayerEntity player, ItemStack stack) {
                super.onTakeItem(player, stack);
            }
        });

        // Player Inventory (3 rows x 9 columns: slots 3..29)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player Hotbar (slots 30..38)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public int getEnergy() {
        return (propertyDelegate.get(1) << 16) | (propertyDelegate.get(0) & 0xFFFF);
    }

    public int getMaxEnergy() {
        return (propertyDelegate.get(3) << 16) | (propertyDelegate.get(2) & 0xFFFF);
    }

    public boolean isCrafting() {
        return propertyDelegate.get(4) > 0;
    }

    public int getScaledCookProgress(int arrowSize) {
        int progress = propertyDelegate.get(4);
        int total = propertyDelegate.get(5);
        return total != 0 && progress != 0 ? progress * arrowSize / total : 0;
    }

    public int getScaledEnergy(int barHeight) {
        int energy = getEnergy();
        int max = getMaxEnergy();
        return max > 0 ? (int) ((long) energy * barHeight / max) : 0;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            if (slotIndex == 2) {
                if (!this.insertItem(originalStack, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickTransfer(originalStack, newStack);
            } else if (slotIndex >= 3) {
                if (originalStack.getItem() instanceof GearItem) {
                    if (!this.insertItem(originalStack, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.insertItem(originalStack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.insertItem(originalStack, 3, 39, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }

            if (originalStack.getCount() == newStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTakeItem(player, originalStack);
        }

        return newStack;
    }
}
