package net.enchantedwood.screen;

import net.enchantedwood.block.entity.SuperComputerBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class SuperComputerScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    public SuperComputerScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(SuperComputerBlockEntity.TOTAL_SLOTS), new ArrayPropertyDelegate(8));
    }

    public SuperComputerScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.SUPER_COMPUTER_SCREEN_HANDLER, syncId);
        checkSize(inventory, SuperComputerBlockEntity.TOTAL_SLOTS);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        inventory.onOpen(playerInventory.player);
        this.addProperties(propertyDelegate);

        // 1. 3x3 Recipe Programming Matrix (Slots 0..8)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 3; ++col) {
                this.addSlot(new Slot(inventory, col + row * 3, 30 + col * 18, 17 + row * 18));
            }
        }

        // 2. Upgrade Socket (Slot 9)
        this.addSlot(new Slot(inventory, SuperComputerBlockEntity.UPGRADE_SLOT, 8, 53) {
            @Override
            public int getMaxItemCount() {
                return 1;
            }
        });

        // 3. 2x2 Output Buffer (Slots 10..13)
        for (int row = 0; row < 2; ++row) {
            for (int col = 0; col < 2; ++col) {
                this.addSlot(new Slot(inventory, SuperComputerBlockEntity.OUTPUT_START + col + row * 2, 124 + col * 18, 26 + row * 18));
            }
        }

        // 4. Target Recipe Preview Slot (Slot 14 - Display Only)
        this.addSlot(new Slot(inventory, SuperComputerBlockEntity.PREVIEW_SLOT, 96, 18) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }

            @Override
            public boolean canTakeItems(PlayerEntity playerEntity) {
                return false;
            }
        });

        // 5. Player Inventory (3 rows x 9 columns)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // 6. Player Hotbar (1 row x 9 columns)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public int getEnergy() {
        return (this.propertyDelegate.get(1) << 16) | (this.propertyDelegate.get(0) & 0xFFFF);
    }

    public int getMaxEnergy() {
        return (this.propertyDelegate.get(3) << 16) | (this.propertyDelegate.get(2) & 0xFFFF);
    }

    public int getCraftProgress() {
        return this.propertyDelegate.get(4);
    }

    public int getMaxCraftProgress() {
        return this.propertyDelegate.get(5);
    }

    public boolean isNetworkOnline() {
        return this.propertyDelegate.get(6) != 0;
    }

    public boolean hasValidRecipe() {
        return this.propertyDelegate.get(7) != 0;
    }

    public int getScaledProgress(int pixels) {
        int progress = getCraftProgress();
        int max = getMaxCraftProgress();
        if (max <= 0) max = SuperComputerBlockEntity.BASE_CRAFT_TIME;
        return (progress * pixels) / max;
    }

    public int getScaledEnergy(int pixels) {
        int energy = getEnergy();
        int max = getMaxEnergy();
        if (max <= 0) max = SuperComputerBlockEntity.ENERGY_CAPACITY;
        return (int) (((long) energy * pixels) / max);
    }

    @Override
    public void onSlotClick(int slotIndex, int button, net.minecraft.screen.slot.SlotActionType actionType, PlayerEntity player) {
        // 1. Ghost Pattern Matrix (Slots 0..8)
        if (slotIndex >= 0 && slotIndex < 9) {
            Slot slot = this.slots.get(slotIndex);
            ItemStack cursorStack = this.getCursorStack();

            if (actionType == net.minecraft.screen.slot.SlotActionType.PICKUP || actionType == net.minecraft.screen.slot.SlotActionType.QUICK_MOVE) {
                if (!cursorStack.isEmpty()) {
                    // Place 1x ghost copy without consuming the cursor item
                    slot.setStack(cursorStack.copyWithCount(1));
                } else {
                    // Empty cursor click clears the slot
                    slot.setStack(ItemStack.EMPTY);
                }
                slot.markDirty();
                this.sendContentUpdates();
                return;
            } else if (actionType == net.minecraft.screen.slot.SlotActionType.SWAP) {
                // Hotbar key (1..9) pressed over slot: place ghost copy from that hotbar slot
                ItemStack hotbarStack = player.getInventory().getStack(button);
                if (!hotbarStack.isEmpty()) {
                    slot.setStack(hotbarStack.copyWithCount(1));
                } else {
                    slot.setStack(ItemStack.EMPTY);
                }
                slot.markDirty();
                this.sendContentUpdates();
                return;
            } else if (actionType == net.minecraft.screen.slot.SlotActionType.CLONE) {
                // Middle click clears slot
                slot.setStack(ItemStack.EMPTY);
                slot.markDirty();
                this.sendContentUpdates();
                return;
            }
        }

        // 2. Target Preview Slot (Slot 14) is completely non-interactive
        if (slotIndex == SuperComputerBlockEntity.PREVIEW_SLOT) {
            return;
        }

        super.onSlotClick(slotIndex, button, actionType, player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            // Ignore quick moves from the preview slot or ghost matrix
            if (invSlot == SuperComputerBlockEntity.PREVIEW_SLOT || invSlot < 9) {
                if (invSlot < 9) {
                    slot.setStack(ItemStack.EMPTY);
                    slot.markDirty();
                    this.sendContentUpdates();
                }
                return ItemStack.EMPTY;
            }

            // Output buffer (slots 10..13) and Upgrade slot (9) -> Player Inventory (slots 15..50)
            if (invSlot >= 9 && invSlot < SuperComputerBlockEntity.TOTAL_SLOTS) {
                if (!this.insertItem(originalStack, SuperComputerBlockEntity.TOTAL_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // From player inventory:
                // If Blaze Overclock Core, insert into Upgrade Slot (slot 9)
                if (originalStack.isOf(net.enchantedwood.item.ModItems.BLAZE_OVERCLOCK_CORE) && !this.slots.get(SuperComputerBlockEntity.UPGRADE_SLOT).hasStack()) {
                    if (!this.insertItem(originalStack, SuperComputerBlockEntity.UPGRADE_SLOT, SuperComputerBlockEntity.UPGRADE_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    // Otherwise place a ghost copy in the first empty pattern slot (0..8)
                    for (int i = 0; i < 9; i++) {
                        Slot patternSlot = this.slots.get(i);
                        if (!patternSlot.hasStack()) {
                            patternSlot.setStack(originalStack.copyWithCount(1));
                            patternSlot.markDirty();
                            this.sendContentUpdates();
                            break;
                        }
                    }
                    return ItemStack.EMPTY; // Do NOT consume the player's real item!
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
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (this.inventory instanceof SuperComputerBlockEntity be) {
            if (id == 0) {
                be.executeManualCraft(player, false);
                return true;
            } else if (id == 1) {
                be.executeManualCraft(player, true);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }
}
