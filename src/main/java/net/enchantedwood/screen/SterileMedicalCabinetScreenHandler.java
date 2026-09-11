package net.enchantedwood.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.enchantedwood.block.entity.SterileMedicalCabinetBlockEntity;

public class SterileMedicalCabinetScreenHandler extends ScreenHandler {
    private final Inventory inventory;

    public SterileMedicalCabinetScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(SterileMedicalCabinetBlockEntity.INVENTORY_SIZE));
    }

    public SterileMedicalCabinetScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory) {
        super(ModScreenHandlers.STERILE_MEDICAL_CABINET_SCREEN_HANDLER, syncId);
        checkSize(inventory, SterileMedicalCabinetBlockEntity.INVENTORY_SIZE);
        this.inventory = inventory;
        inventory.onOpen(playerInventory.player);

        // 36 Controlled Medical Slots (4 rows of 9)
        // Row 0: Fabrication & Injector (y = 18)
        for (int col = 0; col < 9; col++) {
            final int slotIndex = col;
            this.addSlot(new Slot(inventory, slotIndex, 8 + col * 18, 18) {
                @Override
                public boolean canInsert(ItemStack stack) {
                    return SterileMedicalCabinetBlockEntity.isItemValidForSlot(slotIndex, stack);
                }
            });
        }

        // Row 1: Extracted Essences (y = 40)
        for (int col = 0; col < 9; col++) {
            final int slotIndex = 9 + col;
            this.addSlot(new Slot(inventory, slotIndex, 8 + col * 18, 40) {
                @Override
                public boolean canInsert(ItemStack stack) {
                    return SterileMedicalCabinetBlockEntity.isItemValidForSlot(slotIndex, stack);
                }
            });
        }

        // Row 2: Biological Feeds & Catalysts (y = 62)
        for (int col = 0; col < 9; col++) {
            final int slotIndex = 18 + col;
            this.addSlot(new Slot(inventory, slotIndex, 8 + col * 18, 62) {
                @Override
                public boolean canInsert(ItemStack stack) {
                    return SterileMedicalCabinetBlockEntity.isItemValidForSlot(slotIndex, stack);
                }
            });
        }

        // Row 3: Finished & Pure Cartridges (y = 84)
        for (int col = 0; col < 9; col++) {
            final int slotIndex = 27 + col;
            this.addSlot(new Slot(inventory, slotIndex, 8 + col * 18, 84) {
                @Override
                public boolean canInsert(ItemStack stack) {
                    return SterileMedicalCabinetBlockEntity.isItemValidForSlot(slotIndex, stack);
                }
            });
        }

        // Player Inventory (3 rows, y = 114)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 114 + row * 18));
            }
        }

        // Player Hotbar (1 row, y = 172)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 172));
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            // Shift-clicking from cabinet into player inventory
            if (invSlot < SterileMedicalCabinetBlockEntity.INVENTORY_SIZE) {
                if (!this.insertItem(originalStack, SterileMedicalCabinetBlockEntity.INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Shift-clicking from player inventory into cabinet: strictly reject non-medical items
                if (!SterileMedicalCabinetBlockEntity.isMedicalItem(originalStack)) {
                    return ItemStack.EMPTY;
                }

                boolean inserted = false;
                // Try specific dedicated slot first
                for (int targetSlot = 0; targetSlot < SterileMedicalCabinetBlockEntity.INVENTORY_SIZE; targetSlot++) {
                    if (SterileMedicalCabinetBlockEntity.isItemValidForSlot(targetSlot, originalStack)) {
                        if (this.insertItem(originalStack, targetSlot, targetSlot + 1, false)) {
                            inserted = true;
                            if (originalStack.isEmpty()) break;
                        }
                    }
                }

                if (!inserted) {
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
