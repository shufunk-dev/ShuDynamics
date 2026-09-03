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
import net.minecraft.util.math.BlockPos;
import net.enchantedwood.block.entity.LaserQuarryBlockEntity;
import net.enchantedwood.item.ModItems;

public class LaserQuarryScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;
    public final BlockPos blockPos;

    public LaserQuarryScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(LaserQuarryBlockEntity.INVENTORY_SIZE), new ArrayPropertyDelegate(10), BlockPos.ORIGIN);
    }

    public LaserQuarryScreenHandler(int syncId, PlayerInventory playerInventory, BlockPos pos) {
        this(syncId, playerInventory, new SimpleInventory(LaserQuarryBlockEntity.INVENTORY_SIZE), new ArrayPropertyDelegate(10), pos);
    }

    public LaserQuarryScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        this(syncId, playerInventory, inventory, propertyDelegate, BlockPos.ORIGIN);
    }

    public LaserQuarryScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate, BlockPos pos) {
        super(ModScreenHandlers.LASER_QUARRY_SCREEN_HANDLER, syncId);
        checkSize(inventory, LaserQuarryBlockEntity.INVENTORY_SIZE);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        this.blockPos = pos;
        inventory.onOpen(playerInventory.player);
        this.addProperties(propertyDelegate);

        // 3x3 Output Buffer (Slots 0..8) at x: 80, y: 18
        for (int m = 0; m < 3; ++m) {
            for (int l = 0; l < 3; ++l) {
                this.addSlot(new Slot(inventory, l + m * 3, 80 + l * 18, 18 + m * 18) {
                    @Override
                    public boolean canInsert(ItemStack stack) {
                        return false;
                    }
                });
            }
        }

        // Upgrade Sockets (Slots 9, 10, 11) at x: 152
        // Speed Socket (Slot 9)
        this.addSlot(new Slot(inventory, LaserQuarryBlockEntity.SPEED_SLOT, 152, 18) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(ModItems.BLAZE_OVERCLOCK_CORE) ||
                        stack.isOf(ModItems.COPPER_GEAR) || stack.isOf(ModItems.ENCHANTED_COPPER_GEAR) ||
                        stack.isOf(ModItems.IRON_GEAR) || stack.isOf(ModItems.ENCHANTED_IRON_GEAR) ||
                        stack.isOf(ModItems.GOLD_GEAR) || stack.isOf(ModItems.ENCHANTED_GOLD_GEAR) ||
                        stack.isOf(ModItems.DIAMOND_GEAR) || stack.isOf(ModItems.ENCHANTED_DIAMOND_GEAR) ||
                        stack.isOf(ModItems.TITANIUM_GEAR) || stack.isOf(ModItems.ENCHANTED_TITANIUM_GEAR);
            }
        });

        // Range Socket (Slot 10)
        this.addSlot(new Slot(inventory, LaserQuarryBlockEntity.RANGE_SLOT, 152, 36) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(ModItems.RANGE_UPGRADE_T1) || stack.isOf(ModItems.RANGE_UPGRADE_T2);
            }
        });

        // Utility / Extraction Socket (Slot 11)
        this.addSlot(new Slot(inventory, LaserQuarryBlockEntity.EXTRACTION_SLOT, 152, 54) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(ModItems.FORTUNE_CORE) || stack.isOf(ModItems.SILK_TOUCH_CORE) ||
                        stack.isOf(ModItems.INTERDIMENSIONAL_CARD) || stack.isOf(ModItems.CHUNK_LOADER_MODULE);
            }
        });

        // Player Inventory & Hotbar (Slots 12..47)
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (player.getEntityWorld().getBlockEntity(this.blockPos) instanceof LaserQuarryBlockEntity quarry) {
            quarry.handleAction(id);
            return true;
        }
        return false;
    }

    public int getEnergy() {
        return (this.propertyDelegate.get(1) << 16) | (this.propertyDelegate.get(0) & 0xFFFF);
    }

    public int getMaxEnergy() {
        return (this.propertyDelegate.get(3) << 16) | (this.propertyDelegate.get(2) & 0xFFFF);
    }

    public int getMode() {
        return this.propertyDelegate.get(4);
    }

    public boolean isPaused() {
        return this.propertyDelegate.get(5) == 1;
    }

    public int getScanY() {
        return (short) this.propertyDelegate.get(6);
    }

    public int getTotalMinedCount() {
        return this.propertyDelegate.get(7);
    }

    public int getRangeChunkRadius() {
        return this.propertyDelegate.get(8);
    }

    public boolean isNetworkOnline() {
        return this.propertyDelegate.get(9) >= 1;
    }

    public int getNetworkStatus() {
        return this.propertyDelegate.get(9);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            if (invSlot < LaserQuarryBlockEntity.INVENTORY_SIZE) {
                // Moving from machine to player inventory
                if (!this.insertItem(originalStack, LaserQuarryBlockEntity.INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player inventory to upgrade sockets
                if (this.slots.get(LaserQuarryBlockEntity.SPEED_SLOT).canInsert(originalStack)) {
                    if (!this.insertItem(originalStack, LaserQuarryBlockEntity.SPEED_SLOT, LaserQuarryBlockEntity.SPEED_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (this.slots.get(LaserQuarryBlockEntity.RANGE_SLOT).canInsert(originalStack)) {
                    if (!this.insertItem(originalStack, LaserQuarryBlockEntity.RANGE_SLOT, LaserQuarryBlockEntity.RANGE_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (this.slots.get(LaserQuarryBlockEntity.EXTRACTION_SLOT).canInsert(originalStack)) {
                    if (!this.insertItem(originalStack, LaserQuarryBlockEntity.EXTRACTION_SLOT, LaserQuarryBlockEntity.EXTRACTION_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
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

    private void addPlayerInventory(PlayerInventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(PlayerInventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }
}
