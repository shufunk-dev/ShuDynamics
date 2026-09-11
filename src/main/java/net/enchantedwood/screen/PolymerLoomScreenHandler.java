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
import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.block.entity.PolymerLoomBlockEntity;
import net.enchantedwood.item.custom.GearItem;

public class PolymerLoomScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    public PolymerLoomScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(PolymerLoomBlockEntity.INVENTORY_SIZE), new ArrayPropertyDelegate(7));
    }

    public PolymerLoomScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.POLYMER_LOOM_SCREEN_HANDLER, syncId);
        checkSize(inventory, PolymerLoomBlockEntity.INVENTORY_SIZE);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        this.addProperties(propertyDelegate);
        inventory.onOpen(playerInventory.player);

        // Slot 0: Raw Polymer / Rubber
        this.addSlot(new Slot(inventory, PolymerLoomBlockEntity.SLOT_POLYMER, 44, 17));

        // Slot 1: Textile Fibers / String / Wool
        this.addSlot(new Slot(inventory, PolymerLoomBlockEntity.SLOT_FIBER, 44, 35));

        // Slot 2: Additive / Silicon / Reinforcement
        this.addSlot(new Slot(inventory, PolymerLoomBlockEntity.SLOT_ADDITIVE, 44, 53));

        // Slot 3: Finished Output
        this.addSlot(new Slot(inventory, PolymerLoomBlockEntity.SLOT_OUTPUT, 120, 35) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }
        });

        // Slot 4: Gear Upgrade
        this.addSlot(new Slot(inventory, PolymerLoomBlockEntity.GEAR_SLOT, 152, 8) {
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
        int tierOrdinal = this.propertyDelegate.get(6);
        GearTier[] tiers = GearTier.values();
        if (tierOrdinal >= 0 && tierOrdinal < tiers.length) {
            return tiers[tierOrdinal];
        }
        return GearTier.NONE;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);

        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            if (invSlot < PolymerLoomBlockEntity.INVENTORY_SIZE) {
                if (!this.insertItem(originalStack, PolymerLoomBlockEntity.INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (originalStack.getItem() instanceof GearItem) {
                    if (!this.insertItem(originalStack, PolymerLoomBlockEntity.GEAR_SLOT, PolymerLoomBlockEntity.GEAR_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.insertItem(originalStack, PolymerLoomBlockEntity.SLOT_POLYMER, PolymerLoomBlockEntity.SLOT_ADDITIVE + 1, false)) {
                    if (invSlot < PolymerLoomBlockEntity.INVENTORY_SIZE + 27) {
                        if (!this.insertItem(originalStack, PolymerLoomBlockEntity.INVENTORY_SIZE + 27, this.slots.size(), false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!this.insertItem(originalStack, PolymerLoomBlockEntity.INVENTORY_SIZE, PolymerLoomBlockEntity.INVENTORY_SIZE + 27, false)) {
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
