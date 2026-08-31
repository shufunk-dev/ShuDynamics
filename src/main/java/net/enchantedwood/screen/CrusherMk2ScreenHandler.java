package net.enchantedwood.screen;

import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.block.entity.CrusherMk2BlockEntity;
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

public class CrusherMk2ScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    public CrusherMk2ScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(CrusherMk2BlockEntity.INVENTORY_SIZE), new ArrayPropertyDelegate(7));
    }

    public CrusherMk2ScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.CRUSHER_MK2_SCREEN_HANDLER, syncId);
        checkSize(inventory, CrusherMk2BlockEntity.INVENTORY_SIZE);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        this.addProperties(propertyDelegate);
        inventory.onOpen(playerInventory.player);

        // Slot 0: Ore Input (x=48, y=35)
        this.addSlot(new Slot(inventory, CrusherMk2BlockEntity.INPUT_SLOT, 48, 35));

        // Slot 1: Primary Output (x=106, y=35)
        this.addSlot(new Slot(inventory, CrusherMk2BlockEntity.PRIMARY_OUTPUT_SLOT, 106, 35) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }
        });

        // Slot 2: Secondary Byproduct Output (x=134, y=35)
        this.addSlot(new Slot(inventory, CrusherMk2BlockEntity.BYPRODUCT_OUTPUT_SLOT, 134, 35) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }
        });

        // Slot 3: Gear Upgrade (x=152, y=8)
        this.addSlot(new Slot(inventory, CrusherMk2BlockEntity.GEAR_SLOT, 152, 8) {
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

    public boolean isCooking() {
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
        return max > 0 ? max : CrusherMk2BlockEntity.CAPACITY;
    }

    public int getScaledEnergy(int pixels) {
        int max = getMaxEnergy();
        if (max <= 0) return 0;
        return (int) (((long) getEnergy() * pixels) / max);
    }

    public GearTier getGearTier() {
        int index = propertyDelegate.get(6);
        if (index >= 0 && index < GearTier.values().length) {
            return GearTier.values()[index];
        }
        return GearTier.NONE;
    }

    public int getTierYield() {
        return switch (getGearTier()) {
            case IRON -> 3;
            case COPPER -> 3;
            case BRONZE -> 4;
            case GOLD -> 4;
            case TITANIUM -> 5;
            case DIAMOND -> 5;
            case NETHERITE -> 6;
            default -> 3;
        };
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();
            if (invSlot < CrusherMk2BlockEntity.INVENTORY_SIZE) {
                if (!this.insertItem(originalStack, CrusherMk2BlockEntity.INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (originalStack.getItem() instanceof GearItem) {
                    if (!this.insertItem(originalStack, CrusherMk2BlockEntity.GEAR_SLOT, CrusherMk2BlockEntity.GEAR_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.insertItem(originalStack, CrusherMk2BlockEntity.INPUT_SLOT, CrusherMk2BlockEntity.INPUT_SLOT + 1, false)) {
                    if (invSlot < 4 + 27) {
                        if (!this.insertItem(originalStack, 4 + 27, this.slots.size(), false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!this.insertItem(originalStack, 4, 4 + 27, false)) {
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
