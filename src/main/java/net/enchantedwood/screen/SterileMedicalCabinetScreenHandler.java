package net.enchantedwood.screen;

import net.enchantedwood.block.entity.SterileMedicalCabinetBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SterileMedicalCabinetScreenHandler extends AbstractContainerMenu {
    private final Container inventory;

    public SterileMedicalCabinetScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(SterileMedicalCabinetBlockEntity.INVENTORY_SIZE));
    }

    public SterileMedicalCabinetScreenHandler(int syncId, Inventory playerInventory, Container inventory) {
        super(ModScreenHandlers.STERILE_MEDICAL_CABINET_SCREEN_HANDLER, syncId);
        checkContainerSize(inventory, SterileMedicalCabinetBlockEntity.INVENTORY_SIZE);
        this.inventory = inventory;
        inventory.startOpen(playerInventory.player);

        // 36 Controlled Medical Slots (4 rows of 9)
        // Row 0: Fabrication & Injector (y = 18)
        for (int col = 0; col < 9; col++) {
            final int slotIndex = col;
            this.addSlot(new Slot(inventory, slotIndex, 8 + col * 18, 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return SterileMedicalCabinetBlockEntity.isItemValidForSlot(slotIndex, stack);
                }
            });
        }

        // Row 1: Extracted Essences (y = 40)
        for (int col = 0; col < 9; col++) {
            final int slotIndex = 9 + col;
            this.addSlot(new Slot(inventory, slotIndex, 8 + col * 18, 40) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return SterileMedicalCabinetBlockEntity.isItemValidForSlot(slotIndex, stack);
                }
            });
        }

        // Row 2: Biological Feeds & Catalysts (y = 62)
        for (int col = 0; col < 9; col++) {
            final int slotIndex = 18 + col;
            this.addSlot(new Slot(inventory, slotIndex, 8 + col * 18, 62) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return SterileMedicalCabinetBlockEntity.isItemValidForSlot(slotIndex, stack);
                }
            });
        }

        // Row 3: Finished & Pure Cartridges (y = 84)
        for (int col = 0; col < 9; col++) {
            final int slotIndex = 27 + col;
            this.addSlot(new Slot(inventory, slotIndex, 8 + col * 18, 84) {
                @Override
                public boolean mayPlace(ItemStack stack) {
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
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();

            // Shift-clicking from cabinet into player inventory
            if (invSlot < SterileMedicalCabinetBlockEntity.INVENTORY_SIZE) {
                if (!this.moveItemStackTo(originalStack, SterileMedicalCabinetBlockEntity.INVENTORY_SIZE, this.slots.size(), true)) {
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
                        if (this.moveItemStackTo(originalStack, targetSlot, targetSlot + 1, false)) {
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
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return newStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.inventory.stillValid(player);
    }
}
