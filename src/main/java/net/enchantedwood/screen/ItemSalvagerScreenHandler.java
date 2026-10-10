package net.enchantedwood.screen;

import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.block.entity.ItemSalvagerBlockEntity;
import net.enchantedwood.item.custom.GearItem;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ItemSalvagerScreenHandler extends AbstractContainerMenu {
    private final Container inventory;
    private final ContainerData propertyDelegate;

    public ItemSalvagerScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(ItemSalvagerBlockEntity.INVENTORY_SIZE), new SimpleContainerData(7));
    }

    public ItemSalvagerScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData propertyDelegate) {
        super(ModScreenHandlers.ITEM_SALVAGER_SCREEN_HANDLER, syncId);
        checkContainerSize(inventory, ItemSalvagerBlockEntity.INVENTORY_SIZE);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        inventory.startOpen(playerInventory.player);
        this.addDataSlots(propertyDelegate);

        // Slot 0: Input Slot (x=49, y=35)
        this.addSlot(new Slot(inventory, ItemSalvagerBlockEntity.INPUT_SLOT, 49, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ItemSalvagerBlockEntity.isSalvageable(stack);
            }
        });

        // Slots 1-4: Output Slots (2x2 grid at 107, 26 / 125, 26 / 107, 44 / 125, 44)
        this.addSlot(new Slot(inventory, ItemSalvagerBlockEntity.OUTPUT_SLOT_1, 107, 26) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
        });
        this.addSlot(new Slot(inventory, ItemSalvagerBlockEntity.OUTPUT_SLOT_2, 125, 26) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
        });
        this.addSlot(new Slot(inventory, ItemSalvagerBlockEntity.OUTPUT_SLOT_3, 107, 44) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
        });
        this.addSlot(new Slot(inventory, ItemSalvagerBlockEntity.OUTPUT_SLOT_4, 125, 44) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
        });

        // Slot 5: Gear Upgrade Slot (x=152, y=8)
        this.addSlot(new Slot(inventory, ItemSalvagerBlockEntity.GEAR_SLOT, 152, 8) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof GearItem;
            }
        });

        // Player Inventory
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player Hotbar
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public int getEnergy() {
        return (this.propertyDelegate.get(2) & 0xFFFF) | ((this.propertyDelegate.get(3) & 0xFFFF) << 16);
    }

    public int getMaxEnergy() {
        int max = (this.propertyDelegate.get(4) & 0xFFFF) | ((this.propertyDelegate.get(5) & 0xFFFF) << 16);
        return max > 0 ? max : ItemSalvagerBlockEntity.CAPACITY;
    }

    public int getCookTime() {
        return this.propertyDelegate.get(0);
    }

    public int getTotalCookTime() {
        int total = this.propertyDelegate.get(1);
        return total > 0 ? total : 120;
    }

    public GearTier getGearTier() {
        int ordinal = this.propertyDelegate.get(6);
        GearTier[] values = GearTier.values();
        if (ordinal >= 0 && ordinal < values.length) {
            return values[ordinal];
        }
        return GearTier.NONE;
    }

    public int getScaledCookProgress(int pixels) {
        int cook = getCookTime();
        int total = getTotalCookTime();
        return (total > 0 && cook > 0) ? (cook * pixels / total) : 0;
    }

    public int getScaledEnergy(int pixels) {
        int max = getMaxEnergy();
        return max > 0 ? (int) (((long) getEnergy() * pixels) / max) : 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();

            if (invSlot < ItemSalvagerBlockEntity.INVENTORY_SIZE) {
                // Moving from machine to player inventory
                if (!this.moveItemStackTo(originalStack, ItemSalvagerBlockEntity.INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player inventory to machine
                if (originalStack.getItem() instanceof GearItem) {
                    if (!this.moveItemStackTo(originalStack, ItemSalvagerBlockEntity.GEAR_SLOT, ItemSalvagerBlockEntity.GEAR_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (ItemSalvagerBlockEntity.isSalvageable(originalStack)) {
                    if (!this.moveItemStackTo(originalStack, ItemSalvagerBlockEntity.INPUT_SLOT, ItemSalvagerBlockEntity.INPUT_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (invSlot >= ItemSalvagerBlockEntity.INVENTORY_SIZE && invSlot < ItemSalvagerBlockEntity.INVENTORY_SIZE + 27) {
                    if (!this.moveItemStackTo(originalStack, ItemSalvagerBlockEntity.INVENTORY_SIZE + 27, this.slots.size(), false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (invSlot >= ItemSalvagerBlockEntity.INVENTORY_SIZE + 27 && invSlot < this.slots.size()) {
                    if (!this.moveItemStackTo(originalStack, ItemSalvagerBlockEntity.INVENTORY_SIZE, ItemSalvagerBlockEntity.INVENTORY_SIZE + 27, false)) {
                        return ItemStack.EMPTY;
                    }
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
