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
import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.item.ModItems;

public class EnchantedStorageControllerScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    public EnchantedStorageControllerScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(3), new ArrayPropertyDelegate(6));
    }

    public EnchantedStorageControllerScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.ENCHANTED_STORAGE_CONTROLLER_SCREEN_HANDLER, syncId);
        checkSize(inventory, 3);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        inventory.onOpen(playerInventory.player);
        this.addProperties(propertyDelegate);

        // Slot 0: Emergency Backup Fuel Slot (Center)
        this.addSlot(new Slot(inventory, 0, 80, 48) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(ModBlocks.ENCHANTED_COAL_BLOCK.asItem())
                        || stack.isOf(ModItems.ENCHANTED_LAVA_BUCKET)
                        || stack.isOf(ModItems.ENCHANTED_COPPER_LAVA_BUCKET);
            }
        });

        // Slot 1: Chunk Loader Module (Left)
        this.addSlot(new Slot(inventory, 1, 36, 48) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(ModItems.CHUNK_LOADER_MODULE);
            }
        });

        // Slot 2: Interdimensional Card (Right)
        this.addSlot(new Slot(inventory, 2, 124, 48) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(ModItems.INTERDIMENSIONAL_CARD);
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
        return propertyDelegate.get(0);
    }

    public int getTotalBurnTime() {
        return propertyDelegate.get(1);
    }

    public int getEnergy() {
        return propertyDelegate.get(2);
    }

    public int getMaxEnergy() {
        return propertyDelegate.get(3);
    }

    public boolean hasChunkLoader() {
        return propertyDelegate.get(4) > 0;
    }

    public boolean hasInterdimensionalCard() {
        return propertyDelegate.get(5) > 0;
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
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            if (slotIndex < 3) {
                if (!this.insertItem(originalStack, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (originalStack.isOf(ModItems.CHUNK_LOADER_MODULE)) {
                    if (!this.insertItem(originalStack, 1, 2, false)) return ItemStack.EMPTY;
                } else if (originalStack.isOf(ModItems.INTERDIMENSIONAL_CARD)) {
                    if (!this.insertItem(originalStack, 2, 3, false)) return ItemStack.EMPTY;
                } else if (!this.insertItem(originalStack, 0, 1, false)) {
                    return ItemStack.EMPTY;
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
