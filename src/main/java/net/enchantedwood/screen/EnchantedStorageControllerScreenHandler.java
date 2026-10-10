package net.enchantedwood.screen;

import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.item.ModItems;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class EnchantedStorageControllerScreenHandler extends AbstractContainerMenu {
    private final Container inventory;
    private final ContainerData propertyDelegate;

    public EnchantedStorageControllerScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(3), new SimpleContainerData(10));
    }

    public EnchantedStorageControllerScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData propertyDelegate) {
        super(ModScreenHandlers.ENCHANTED_STORAGE_CONTROLLER_SCREEN_HANDLER, syncId);
        checkContainerSize(inventory, 3);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        inventory.startOpen(playerInventory.player);
        this.addDataSlots(propertyDelegate);

        // Slot 0: Emergency Backup Fuel Slot (Center)
        this.addSlot(new Slot(inventory, 0, 80, 48) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModBlocks.ENCHANTED_COAL_BLOCK.asItem())
                        || stack.is(ModItems.ENCHANTED_LAVA_BUCKET)
                        || stack.is(ModItems.ENCHANTED_COPPER_LAVA_BUCKET);
            }
        });

        // Slot 1: Chunk Loader Module (Left)
        this.addSlot(new Slot(inventory, 1, 36, 48) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.CHUNK_LOADER_MODULE);
            }
        });

        // Slot 2: Interdimensional Card (Right)
        this.addSlot(new Slot(inventory, 2, 124, 48) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.INTERDIMENSIONAL_CARD);
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

    public int getBurnTime() {
        return (propertyDelegate.get(0) & 0xFFFF) | ((propertyDelegate.get(1) & 0xFFFF) << 16);
    }

    public int getTotalBurnTime() {
        return (propertyDelegate.get(2) & 0xFFFF) | ((propertyDelegate.get(3) & 0xFFFF) << 16);
    }

    public int getEnergy() {
        return (propertyDelegate.get(4) & 0xFFFF) | ((propertyDelegate.get(5) & 0xFFFF) << 16);
    }

    public int getMaxEnergy() {
        return (propertyDelegate.get(6) & 0xFFFF) | ((propertyDelegate.get(7) & 0xFFFF) << 16);
    }

    public boolean hasChunkLoader() {
        return propertyDelegate.get(8) > 0;
    }

    public boolean hasInterdimensionalCard() {
        return propertyDelegate.get(9) > 0;
    }

    public boolean isGridPowered() {
        return getEnergy() > 0;
    }

    public boolean isFuelPowered() {
        return getBurnTime() > 0;
    }

    public boolean isOnline() {
        return isGridPowered() || isFuelPowered();
    }

    public int getBurnProgressScaled(int pixels) {
        int total = getTotalBurnTime();
        if (total <= 0) total = 90000;
        int current = getBurnTime();
        return Math.min(pixels, (int) ((long) current * pixels / total));
    }

    public int getScaledEnergy(int pixels) {
        int max = getMaxEnergy();
        if (max <= 0) max = 100_000;
        int cur = getEnergy();
        if (cur > 0) {
            return Math.min(pixels, (int) ((long) cur * pixels / max));
        }
        if (isFuelPowered()) {
            return getBurnProgressScaled(pixels);
        }
        return 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.inventory.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();

            if (slotIndex < 3) {
                if (!this.moveItemStackTo(originalStack, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (originalStack.is(ModItems.CHUNK_LOADER_MODULE)) {
                    if (!this.moveItemStackTo(originalStack, 1, 2, false)) return ItemStack.EMPTY;
                } else if (originalStack.is(ModItems.INTERDIMENSIONAL_CARD)) {
                    if (!this.moveItemStackTo(originalStack, 2, 3, false)) return ItemStack.EMPTY;
                } else if (!this.moveItemStackTo(originalStack, 0, 1, false)) {
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
}
