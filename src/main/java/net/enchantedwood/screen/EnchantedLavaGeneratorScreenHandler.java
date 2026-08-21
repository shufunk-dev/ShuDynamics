package net.enchantedwood.screen;

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
import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.GearItem;

public class EnchantedLavaGeneratorScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    public EnchantedLavaGeneratorScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(5), new ArrayPropertyDelegate(6));
    }

    public EnchantedLavaGeneratorScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.ENCHANTED_LAVA_GENERATOR_SCREEN_HANDLER, syncId);
        checkSize(inventory, 5);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        inventory.onOpen(playerInventory.player);
        this.addProperties(propertyDelegate);

        // Machine Slots
        // Slot 0: Cobblestone Input
        this.addSlot(new Slot(inventory, 0, 44, 17) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(Items.COBBLESTONE);
            }
        });

        // Slot 1: Fuel Slot (Enchanted Coal Block ONLY)
        this.addSlot(new Slot(inventory, 1, 26, 53) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(ModBlocks.ENCHANTED_COAL_BLOCK.asItem());
            }
        });

        // Slot 2: Gear Upgrade Slot
        this.addSlot(new Slot(inventory, 2, 62, 53) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.getItem() instanceof GearItem gear && gear.isEnchanted();
            }
        });

        // Slot 3: Empty Bucket Input
        this.addSlot(new Slot(inventory, 3, 108, 17) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(Items.BUCKET) || stack.isOf(ModItems.COPPER_BUCKET);
            }
        });

        // Slot 4: Output Slot
        this.addSlot(new Slot(inventory, 4, 108, 53) {
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

        // Player Hotbar
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public boolean isCrafting() {
        return propertyDelegate.get(0) > 0;
    }

    public boolean isBurning() {
        return propertyDelegate.get(2) > 0;
    }

    public int getScaledCookProgress() {
        int progress = propertyDelegate.get(0);
        int total = propertyDelegate.get(1);
        int arrowSize = 24;
        return total != 0 && progress != 0 ? progress * arrowSize / total : 0;
    }

    public int getScaledFuelProgress() {
        int fuelTime = propertyDelegate.get(2);
        int totalFuel = propertyDelegate.get(3);
        int flameSize = 14;
        return totalFuel != 0 ? fuelTime * flameSize / totalFuel : 0;
    }

    public int getScaledLavaProgress() {
        int lava = propertyDelegate.get(5);
        int maxLava = 10000;
        int barHeight = 52;
        return lava * barHeight / maxLava;
    }

    public int getLavaAmount() {
        return propertyDelegate.get(5);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);

        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            if (invSlot < 5) {
                if (!this.insertItem(originalStack, 5, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (originalStack.isOf(Items.COBBLESTONE)) {
                    if (!this.insertItem(originalStack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (originalStack.isOf(ModBlocks.ENCHANTED_COAL_BLOCK.asItem())) {
                    if (!this.insertItem(originalStack, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (originalStack.getItem() instanceof GearItem gear && gear.isEnchanted()) {
                    if (!this.insertItem(originalStack, 2, 3, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (originalStack.isOf(Items.BUCKET) || originalStack.isOf(ModItems.COPPER_BUCKET)) {
                    if (!this.insertItem(originalStack, 3, 4, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (invSlot >= 5 && invSlot < 32) {
                    if (!this.insertItem(originalStack, 32, 41, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (invSlot >= 32 && invSlot < 41) {
                    if (!this.insertItem(originalStack, 5, 32, false)) {
                        return ItemStack.EMPTY;
                    }
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
