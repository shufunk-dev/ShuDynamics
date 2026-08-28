package net.enchantedwood.screen;

import net.enchantedwood.block.entity.TitaniumTankControllerBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class TitaniumTankScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    public TitaniumTankScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(TitaniumTankControllerBlockEntity.INVENTORY_SIZE), new ArrayPropertyDelegate(5));
    }

    public TitaniumTankScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.TITANIUM_TANK_SCREEN_HANDLER, syncId);
        checkSize(inventory, TitaniumTankControllerBlockEntity.INVENTORY_SIZE);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        this.addProperties(propertyDelegate);
        inventory.onOpen(playerInventory.player);

        // Slot 0: Bucket Input (Fill or Drain Tank) (x=38, y=26)
        this.addSlot(new Slot(inventory, TitaniumTankControllerBlockEntity.BUCKET_IN_SLOT, 38, 26) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(Items.LAVA_BUCKET) || stack.isOf(Items.BUCKET);
            }
        });

        // Slot 1: Bucket Output (x=38, y=56)
        this.addSlot(new Slot(inventory, TitaniumTankControllerBlockEntity.BUCKET_OUT_SLOT, 38, 56) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }
        });

        // Player Inventory
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public int getLavaAmount() {
        return (propertyDelegate.get(0) & 0xFFFF) | ((propertyDelegate.get(1) & 0xFFFF) << 16);
    }

    public int getMaxLava() {
        return (propertyDelegate.get(2) & 0xFFFF) | ((propertyDelegate.get(3) & 0xFFFF) << 16);
    }

    public boolean isFormed() {
        return propertyDelegate.get(4) == 1;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            if (slotIndex < TitaniumTankControllerBlockEntity.INVENTORY_SIZE) {
                if (!this.insertItem(originalStack, TitaniumTankControllerBlockEntity.INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (originalStack.isOf(Items.LAVA_BUCKET) || originalStack.isOf(Items.BUCKET)) {
                    if (!this.insertItem(originalStack, TitaniumTankControllerBlockEntity.BUCKET_IN_SLOT, TitaniumTankControllerBlockEntity.BUCKET_IN_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (slotIndex < TitaniumTankControllerBlockEntity.INVENTORY_SIZE + 27) {
                    if (!this.insertItem(originalStack, TitaniumTankControllerBlockEntity.INVENTORY_SIZE + 27, this.slots.size(), false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.insertItem(originalStack, TitaniumTankControllerBlockEntity.INVENTORY_SIZE, TitaniumTankControllerBlockEntity.INVENTORY_SIZE + 27, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }
        return newStack;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }
}
