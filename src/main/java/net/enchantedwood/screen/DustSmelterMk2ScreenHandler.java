package net.enchantedwood.screen;

import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.block.entity.DustSmelterMk2BlockEntity;
import net.enchantedwood.item.ModItems;
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
import net.minecraft.server.world.ServerWorld;

public class DustSmelterMk2ScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    public DustSmelterMk2ScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(DustSmelterMk2BlockEntity.INVENTORY_SIZE), new ArrayPropertyDelegate(7));
    }

    public DustSmelterMk2ScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.DUST_SMELTER_MK2_SCREEN_HANDLER, syncId);
        checkSize(inventory, DustSmelterMk2BlockEntity.INVENTORY_SIZE);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        this.addProperties(propertyDelegate);
        inventory.onOpen(playerInventory.player);

        // Slot 0: Input A (x=52, y=24)
        this.addSlot(new Slot(inventory, DustSmelterMk2BlockEntity.INPUT_SLOT_A, 52, 24));

        // Slot 1: Input B (x=52, y=48)
        this.addSlot(new Slot(inventory, DustSmelterMk2BlockEntity.INPUT_SLOT_B, 52, 48));

        // Slot 2: Output A (x=112, y=24)
        this.addSlot(new Slot(inventory, DustSmelterMk2BlockEntity.OUTPUT_SLOT_A, 112, 24) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }

            @Override
            public void onTakeItem(PlayerEntity player, ItemStack stack) {
                if (inventory instanceof DustSmelterMk2BlockEntity blockEntity) {
                    if (blockEntity.getWorld() instanceof ServerWorld serverWorld) {
                        blockEntity.dropExperience(serverWorld, player);
                    }
                }
                super.onTakeItem(player, stack);
            }
        });

        // Slot 3: Output B (x=112, y=48)
        this.addSlot(new Slot(inventory, DustSmelterMk2BlockEntity.OUTPUT_SLOT_B, 112, 48) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }

            @Override
            public void onTakeItem(PlayerEntity player, ItemStack stack) {
                if (inventory instanceof DustSmelterMk2BlockEntity blockEntity) {
                    if (blockEntity.getWorld() instanceof ServerWorld serverWorld) {
                        blockEntity.dropExperience(serverWorld, player);
                    }
                }
                super.onTakeItem(player, stack);
            }
        });

        // Slot 4: Gear Upgrade (x=152, y=8)
        this.addSlot(new Slot(inventory, DustSmelterMk2BlockEntity.GEAR_SLOT, 152, 8) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.getItem() instanceof GearItem || stack.isOf(ModItems.BLAZE_OVERCLOCK_CORE);
            }
        });

        // Player Inventory (Slots 5..31)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player Hotbar (Slots 32..40)
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
        return max > 0 ? max : DustSmelterMk2BlockEntity.CAPACITY;
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

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            if (invSlot < DustSmelterMk2BlockEntity.INVENTORY_SIZE) {
                // Moving from machine to player inventory
                if (!this.insertItem(originalStack, DustSmelterMk2BlockEntity.INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player inventory to machine
                if (originalStack.getItem() instanceof GearItem || originalStack.isOf(ModItems.BLAZE_OVERCLOCK_CORE)) {
                    if (!this.insertItem(originalStack, DustSmelterMk2BlockEntity.GEAR_SLOT, DustSmelterMk2BlockEntity.GEAR_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (DustSmelterMk2BlockEntity.getOutputItem(originalStack.getItem()) != null) {
                    if (!this.insertItem(originalStack, DustSmelterMk2BlockEntity.INPUT_SLOT_A, DustSmelterMk2BlockEntity.INPUT_SLOT_B + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (invSlot < DustSmelterMk2BlockEntity.INVENTORY_SIZE + 27) {
                    if (!this.insertItem(originalStack, DustSmelterMk2BlockEntity.INVENTORY_SIZE + 27, this.slots.size(), false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.insertItem(originalStack, DustSmelterMk2BlockEntity.INVENTORY_SIZE, DustSmelterMk2BlockEntity.INVENTORY_SIZE + 27, false)) {
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
}
