package net.enchantedwood.screen;

import net.enchantedwood.block.entity.CircuitFabricatorBlockEntity;
import net.enchantedwood.item.ModItems;
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

public class CircuitFabricatorScreenHandler extends AbstractContainerMenu {
    private final Container inventory;
    private final ContainerData propertyDelegate;

    public CircuitFabricatorScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(CircuitFabricatorBlockEntity.INVENTORY_SIZE), new SimpleContainerData(7));
    }

    public CircuitFabricatorScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData propertyDelegate) {
        super(ModScreenHandlers.CIRCUIT_FABRICATOR_SCREEN_HANDLER, syncId);
        checkContainerSize(inventory, CircuitFabricatorBlockEntity.INVENTORY_SIZE);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        this.addDataSlots(propertyDelegate);
        inventory.startOpen(playerInventory.player);

        // Slot 0: Substrate Slot (Silicon Wafer or Base Chip) at x=34, y=35
        this.addSlot(new Slot(inventory, CircuitFabricatorBlockEntity.SUBSTRATE_SLOT, 34, 35));

        // Slot 1: Component Slot 1 at x=64, y=17
        this.addSlot(new Slot(inventory, CircuitFabricatorBlockEntity.COMPONENT_SLOT_1, 64, 17));

        // Slot 2: Component Slot 2 at x=64, y=35
        this.addSlot(new Slot(inventory, CircuitFabricatorBlockEntity.COMPONENT_SLOT_2, 64, 35));

        // Slot 3: Component Slot 3 at x=64, y=53
        this.addSlot(new Slot(inventory, CircuitFabricatorBlockEntity.COMPONENT_SLOT_3, 64, 53));

        // Slot 4: Output Slot at x=124, y=35
        this.addSlot(new Slot(inventory, CircuitFabricatorBlockEntity.OUTPUT_SLOT, 124, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Slot 5: Gear Upgrade Slot at x=152, y=8
        this.addSlot(new Slot(inventory, CircuitFabricatorBlockEntity.GEAR_SLOT, 152, 8) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof GearItem || stack.is(ModItems.BLAZE_OVERCLOCK_CORE);
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

    public boolean isCrafting() {
        return propertyDelegate.get(0) > 0;
    }

    public int getScaledCookProgress(int pixels) {
        int cookTime = propertyDelegate.get(0);
        int totalCookTime = propertyDelegate.get(1);
        if (totalCookTime <= 0) return 0;
        return (int) (((long) cookTime * pixels) / totalCookTime);
    }

    public int getEnergy() {
        return (this.propertyDelegate.get(2) & 0xFFFF) | ((this.propertyDelegate.get(3) & 0xFFFF) << 16);
    }

    public int getMaxEnergy() {
        int max = (this.propertyDelegate.get(4) & 0xFFFF) | ((this.propertyDelegate.get(5) & 0xFFFF) << 16);
        return max > 0 ? max : CircuitFabricatorBlockEntity.CAPACITY;
    }

    public int getScaledEnergy(int pixels) {
        int energy = getEnergy();
        int max = getMaxEnergy();
        if (max <= 0) return 0;
        return (int) (((long) energy * pixels) / max);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);

        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();

            if (invSlot < CircuitFabricatorBlockEntity.INVENTORY_SIZE) {
                // Moving from machine inventory to player inventory
                if (!this.moveItemStackTo(originalStack, CircuitFabricatorBlockEntity.INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player inventory into machine
                if (originalStack.getItem() instanceof GearItem || originalStack.is(ModItems.BLAZE_OVERCLOCK_CORE)) {
                    if (!this.moveItemStackTo(originalStack, CircuitFabricatorBlockEntity.GEAR_SLOT, CircuitFabricatorBlockEntity.GEAR_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (originalStack.is(ModItems.SILICON_WAFER) || originalStack.is(ModItems.BASIC_COMPUTER_CHIP) || originalStack.is(ModItems.ADVANCED_COMPUTER_CHIP)) {
                    if (!this.moveItemStackTo(originalStack, CircuitFabricatorBlockEntity.SUBSTRATE_SLOT, CircuitFabricatorBlockEntity.SUBSTRATE_SLOT + 1, false)) {
                        if (!this.moveItemStackTo(originalStack, CircuitFabricatorBlockEntity.COMPONENT_SLOT_1, CircuitFabricatorBlockEntity.COMPONENT_SLOT_3 + 1, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                } else if (!this.moveItemStackTo(originalStack, CircuitFabricatorBlockEntity.COMPONENT_SLOT_1, CircuitFabricatorBlockEntity.COMPONENT_SLOT_3 + 1, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (originalStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (originalStack.getCount() == newStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, originalStack);
        }

        return newStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.inventory.stillValid(player);
    }
}
