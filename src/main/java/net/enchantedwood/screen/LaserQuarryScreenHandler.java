package net.enchantedwood.screen;

import net.enchantedwood.block.entity.LaserQuarryBlockEntity;
import net.enchantedwood.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class LaserQuarryScreenHandler extends AbstractContainerMenu {
    private final Container inventory;
    private final ContainerData propertyDelegate;
    public final BlockPos blockPos;

    public LaserQuarryScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(LaserQuarryBlockEntity.INVENTORY_SIZE), new SimpleContainerData(10), BlockPos.ZERO);
    }

    public LaserQuarryScreenHandler(int syncId, Inventory playerInventory, BlockPos pos) {
        this(syncId, playerInventory, new SimpleContainer(LaserQuarryBlockEntity.INVENTORY_SIZE), new SimpleContainerData(10), pos);
    }

    public LaserQuarryScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData propertyDelegate) {
        this(syncId, playerInventory, inventory, propertyDelegate, BlockPos.ZERO);
    }

    public LaserQuarryScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData propertyDelegate, BlockPos pos) {
        super(ModScreenHandlers.LASER_QUARRY_SCREEN_HANDLER, syncId);
        checkContainerSize(inventory, LaserQuarryBlockEntity.INVENTORY_SIZE);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        this.blockPos = pos;
        inventory.startOpen(playerInventory.player);
        this.addDataSlots(propertyDelegate);

        // 3x3 Output Buffer (Slots 0..8) at x: 80, y: 18
        for (int m = 0; m < 3; ++m) {
            for (int l = 0; l < 3; ++l) {
                this.addSlot(new Slot(inventory, l + m * 3, 80 + l * 18, 18 + m * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            }
        }

        // Upgrade Sockets (Slots 9, 10, 11) at x: 152
        // Speed Socket (Slot 9)
        this.addSlot(new Slot(inventory, LaserQuarryBlockEntity.SPEED_SLOT, 152, 18) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.BLAZE_OVERCLOCK_CORE) ||
                        stack.is(ModItems.COPPER_GEAR) || stack.is(ModItems.ENCHANTED_COPPER_GEAR) ||
                        stack.is(ModItems.IRON_GEAR) || stack.is(ModItems.ENCHANTED_IRON_GEAR) ||
                        stack.is(ModItems.GOLD_GEAR) || stack.is(ModItems.ENCHANTED_GOLD_GEAR) ||
                        stack.is(ModItems.DIAMOND_GEAR) || stack.is(ModItems.ENCHANTED_DIAMOND_GEAR) ||
                        stack.is(ModItems.TITANIUM_GEAR) || stack.is(ModItems.ENCHANTED_TITANIUM_GEAR);
            }
        });

        // Range Socket (Slot 10)
        this.addSlot(new Slot(inventory, LaserQuarryBlockEntity.RANGE_SLOT, 152, 36) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.RANGE_UPGRADE_T1) || stack.is(ModItems.RANGE_UPGRADE_T2);
            }
        });

        // Utility / Extraction Socket (Slot 11)
        this.addSlot(new Slot(inventory, LaserQuarryBlockEntity.EXTRACTION_SLOT, 152, 54) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.FORTUNE_CORE) || stack.is(ModItems.SILK_TOUCH_CORE) ||
                        stack.is(ModItems.INTERDIMENSIONAL_CARD) || stack.is(ModItems.CHUNK_LOADER_MODULE) ||
                        stack.is(ModItems.WIRELESS_STORAGE_CRYSTAL);
            }
        });

        // Player Inventory & Hotbar (Slots 12..47)
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player.level().getBlockEntity(this.blockPos) instanceof LaserQuarryBlockEntity quarry) {
            quarry.handleAction(id);
            return true;
        }
        return false;
    }

    public int getEnergy() {
        return ((this.propertyDelegate.get(1) & 0xFFFF) << 16) | (this.propertyDelegate.get(0) & 0xFFFF);
    }

    public int getMaxEnergy() {
        return ((this.propertyDelegate.get(3) & 0xFFFF) << 16) | (this.propertyDelegate.get(2) & 0xFFFF);
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
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();

            if (invSlot < LaserQuarryBlockEntity.INVENTORY_SIZE) {
                // Moving from machine to player inventory
                if (!this.moveItemStackTo(originalStack, LaserQuarryBlockEntity.INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player inventory to upgrade sockets
                if (this.slots.get(LaserQuarryBlockEntity.SPEED_SLOT).mayPlace(originalStack)) {
                    if (!this.moveItemStackTo(originalStack, LaserQuarryBlockEntity.SPEED_SLOT, LaserQuarryBlockEntity.SPEED_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (this.slots.get(LaserQuarryBlockEntity.RANGE_SLOT).mayPlace(originalStack)) {
                    if (!this.moveItemStackTo(originalStack, LaserQuarryBlockEntity.RANGE_SLOT, LaserQuarryBlockEntity.RANGE_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (this.slots.get(LaserQuarryBlockEntity.EXTRACTION_SLOT).mayPlace(originalStack)) {
                    if (!this.moveItemStackTo(originalStack, LaserQuarryBlockEntity.EXTRACTION_SLOT, LaserQuarryBlockEntity.EXTRACTION_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (originalStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return newStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.inventory.stillValid(player);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }
}
