package net.enchantedwood.screen;

import net.enchantedwood.block.ModBlocks;
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
import net.minecraft.world.item.Items;

public class EnchantedLavaGeneratorScreenHandler extends AbstractContainerMenu {
    private final Container inventory;
    private final ContainerData propertyDelegate;

    public EnchantedLavaGeneratorScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(5), new SimpleContainerData(6));
    }

    public EnchantedLavaGeneratorScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData propertyDelegate) {
        super(ModScreenHandlers.ENCHANTED_LAVA_GENERATOR_SCREEN_HANDLER, syncId);
        checkContainerSize(inventory, 5);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        inventory.startOpen(playerInventory.player);
        this.addDataSlots(propertyDelegate);

        // Machine Slots
        // Slot 0: Cobblestone Input
        this.addSlot(new Slot(inventory, 0, 44, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.COBBLESTONE);
            }
        });

        // Slot 1: Fuel Slot (Enchanted Coal Block ONLY)
        this.addSlot(new Slot(inventory, 1, 26, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModBlocks.ENCHANTED_COAL_BLOCK.asItem());
            }
        });

        // Slot 2: Gear Upgrade Slot
        this.addSlot(new Slot(inventory, 2, 62, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof GearItem gear && gear.isEnchanted();
            }
        });

        // Slot 3: Empty Bucket Input
        this.addSlot(new Slot(inventory, 3, 108, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.BUCKET) || stack.is(ModItems.COPPER_BUCKET);
            }
        });

        // Slot 4: Output Slot
        this.addSlot(new Slot(inventory, 4, 108, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
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
    public boolean stillValid(Player player) {
        return this.inventory.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);

        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();

            if (invSlot < 5) {
                if (!this.moveItemStackTo(originalStack, 5, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (originalStack.is(Items.COBBLESTONE)) {
                    if (!this.moveItemStackTo(originalStack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (originalStack.is(ModBlocks.ENCHANTED_COAL_BLOCK.asItem())) {
                    if (!this.moveItemStackTo(originalStack, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (originalStack.getItem() instanceof GearItem gear && gear.isEnchanted()) {
                    if (!this.moveItemStackTo(originalStack, 2, 3, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (originalStack.is(Items.BUCKET) || originalStack.is(ModItems.COPPER_BUCKET)) {
                    if (!this.moveItemStackTo(originalStack, 3, 4, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (invSlot >= 5 && invSlot < 32) {
                    if (!this.moveItemStackTo(originalStack, 32, 41, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (invSlot >= 32 && invSlot < 41) {
                    if (!this.moveItemStackTo(originalStack, 5, 32, false)) {
                        return ItemStack.EMPTY;
                    }
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
}
