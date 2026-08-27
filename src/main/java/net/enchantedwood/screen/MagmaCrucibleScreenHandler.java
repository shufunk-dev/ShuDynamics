package net.enchantedwood.screen;

import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.block.entity.MagmaCrucibleBlockEntity;
import net.enchantedwood.item.custom.GearItem;
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

public class MagmaCrucibleScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    public MagmaCrucibleScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(MagmaCrucibleBlockEntity.INVENTORY_SIZE), new ArrayPropertyDelegate(8));
    }

    public MagmaCrucibleScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.MAGMA_CRUCIBLE_SCREEN_HANDLER, syncId);
        checkSize(inventory, MagmaCrucibleBlockEntity.INVENTORY_SIZE);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        this.addProperties(propertyDelegate);
        inventory.onOpen(playerInventory.player);

        // Slot 0: Geology Input (x=44, y=35)
        this.addSlot(new Slot(inventory, MagmaCrucibleBlockEntity.INPUT_SLOT, 44, 35));

        // Slot 1: Mineral Output (x=123, y=25)
        this.addSlot(new Slot(inventory, MagmaCrucibleBlockEntity.MINERAL_OUTPUT_SLOT, 123, 25) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }
        });

        // Slot 2: Bucket Input (x=147, y=25)
        this.addSlot(new Slot(inventory, MagmaCrucibleBlockEntity.BUCKET_INPUT_SLOT, 147, 25) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(Items.BUCKET);
            }
        });

        // Slot 3: Bucket Output (x=147, y=53)
        this.addSlot(new Slot(inventory, MagmaCrucibleBlockEntity.BUCKET_OUTPUT_SLOT, 147, 53) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }
        });

        // Slot 4: Gear Upgrade (x=152, y=8)
        this.addSlot(new Slot(inventory, MagmaCrucibleBlockEntity.GEAR_SLOT, 152, 8) {
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
        return max > 0 ? max : MagmaCrucibleBlockEntity.CAPACITY;
    }

    public int getScaledEnergy(int pixels) {
        int max = getMaxEnergy();
        if (max <= 0) return 0;
        return (int) (((long) getEnergy() * pixels) / max);
    }

    public int getLavaAmount() {
        return this.propertyDelegate.get(6);
    }

    public int getScaledLava(int pixels) {
        return (int) (((long) getLavaAmount() * pixels) / MagmaCrucibleBlockEntity.MAX_LAVA);
    }

    public GearTier getGearTier() {
        int index = propertyDelegate.get(7);
        if (index >= 0 && index < GearTier.values().length) {
            return GearTier.values()[index];
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
            if (invSlot < MagmaCrucibleBlockEntity.INVENTORY_SIZE) {
                if (!this.insertItem(originalStack, MagmaCrucibleBlockEntity.INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (originalStack.getItem() instanceof GearItem) {
                    if (!this.insertItem(originalStack, MagmaCrucibleBlockEntity.GEAR_SLOT, MagmaCrucibleBlockEntity.GEAR_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (originalStack.isOf(Items.BUCKET)) {
                    if (!this.insertItem(originalStack, MagmaCrucibleBlockEntity.BUCKET_INPUT_SLOT, MagmaCrucibleBlockEntity.BUCKET_INPUT_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.insertItem(originalStack, MagmaCrucibleBlockEntity.INPUT_SLOT, MagmaCrucibleBlockEntity.INPUT_SLOT + 1, false)) {
                    if (invSlot < 5 + 27) {
                        if (!this.insertItem(originalStack, 5 + 27, this.slots.size(), false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!this.insertItem(originalStack, 5, 5 + 27, false)) {
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
