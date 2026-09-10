package net.enchantedwood.screen;

import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.block.entity.IndustrialCentrifugeBlockEntity;
import net.enchantedwood.item.custom.GearItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class IndustrialCentrifugeScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    public IndustrialCentrifugeScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(IndustrialCentrifugeBlockEntity.INVENTORY_SIZE), new ArrayPropertyDelegate(7));
    }

    public IndustrialCentrifugeScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.INDUSTRIAL_CENTRIFUGE_SCREEN_HANDLER, syncId);
        checkSize(inventory, IndustrialCentrifugeBlockEntity.INVENTORY_SIZE);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        this.addProperties(propertyDelegate);
        inventory.onOpen(playerInventory.player);

        // Slot 0: Raw Input
        this.addSlot(new Slot(inventory, IndustrialCentrifugeBlockEntity.INPUT_SLOT, 48, 35));

        // Slot 1: Output Essence
        this.addSlot(new Slot(inventory, IndustrialCentrifugeBlockEntity.OUTPUT_SLOT_1, 106, 35) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }
        });

        // Slot 2: Output Byproduct
        this.addSlot(new Slot(inventory, IndustrialCentrifugeBlockEntity.OUTPUT_SLOT_2, 134, 35) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }
        });

        // Slot 3: Gear Upgrade Slot
        this.addSlot(new Slot(inventory, IndustrialCentrifugeBlockEntity.GEAR_SLOT, 152, 8) {
            @Override
            public boolean canInsert(ItemStack stack) {
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

    public int getScaledCookProgress(int arrowWidth) {
        int cookTime = this.propertyDelegate.get(0);
        int totalCookTime = this.propertyDelegate.get(1);
        return totalCookTime != 0 && cookTime != 0 ? cookTime * arrowWidth / totalCookTime : 0;
    }

    public int getScaledEnergy(int barHeight) {
        int energy = this.getEnergy();
        int maxEnergy = this.getMaxEnergy();
        return maxEnergy != 0 && energy != 0 ? (int) ((long) energy * barHeight / maxEnergy) : 0;
    }

    public int getEnergy() {
        return (this.propertyDelegate.get(2) & 0xFFFF) | ((this.propertyDelegate.get(3) & 0xFFFF) << 16);
    }

    public int getMaxEnergy() {
        return (this.propertyDelegate.get(4) & 0xFFFF) | ((this.propertyDelegate.get(5) & 0xFFFF) << 16);
    }

    public GearTier getGearTier() {
        int ordinal = this.propertyDelegate.get(6);
        return (ordinal >= 0 && ordinal < GearTier.values().length) ? GearTier.values()[ordinal] : GearTier.NONE;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            if (invSlot < IndustrialCentrifugeBlockEntity.INVENTORY_SIZE) {
                if (!this.insertItem(originalStack, IndustrialCentrifugeBlockEntity.INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (originalStack.getItem() instanceof GearItem) {
                    if (!this.insertItem(originalStack, IndustrialCentrifugeBlockEntity.GEAR_SLOT, IndustrialCentrifugeBlockEntity.GEAR_SLOT + 1, false)) {
                        if (!this.insertItem(originalStack, IndustrialCentrifugeBlockEntity.INPUT_SLOT, IndustrialCentrifugeBlockEntity.INPUT_SLOT + 1, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                } else if (!this.insertItem(originalStack, IndustrialCentrifugeBlockEntity.INPUT_SLOT, IndustrialCentrifugeBlockEntity.INPUT_SLOT + 1, false)) {
                    if (invSlot < IndustrialCentrifugeBlockEntity.INVENTORY_SIZE + 27) {
                        if (!this.insertItem(originalStack, IndustrialCentrifugeBlockEntity.INVENTORY_SIZE + 27, this.slots.size(), false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!this.insertItem(originalStack, IndustrialCentrifugeBlockEntity.INVENTORY_SIZE, IndustrialCentrifugeBlockEntity.INVENTORY_SIZE + 27, false)) {
                        return ItemStack.EMPTY;
                    }
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
