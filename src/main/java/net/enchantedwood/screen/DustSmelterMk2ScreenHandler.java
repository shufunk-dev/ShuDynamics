package net.enchantedwood.screen;

import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.block.entity.DustSmelterMk2BlockEntity;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.GearItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class DustSmelterMk2ScreenHandler extends AbstractContainerMenu {
    private final Container inventory;
    private final ContainerData propertyDelegate;

    public DustSmelterMk2ScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(DustSmelterMk2BlockEntity.INVENTORY_SIZE), new SimpleContainerData(7));
    }

    public DustSmelterMk2ScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData propertyDelegate) {
        super(ModScreenHandlers.DUST_SMELTER_MK2_SCREEN_HANDLER, syncId);
        checkContainerSize(inventory, DustSmelterMk2BlockEntity.INVENTORY_SIZE);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        this.addDataSlots(propertyDelegate);
        inventory.startOpen(playerInventory.player);

        // Slot 0: Input A (x=52, y=24)
        this.addSlot(new Slot(inventory, DustSmelterMk2BlockEntity.INPUT_SLOT_A, 52, 24));

        // Slot 1: Input B (x=52, y=48)
        this.addSlot(new Slot(inventory, DustSmelterMk2BlockEntity.INPUT_SLOT_B, 52, 48));

        // Slot 2: Output A (x=112, y=24)
        this.addSlot(new Slot(inventory, DustSmelterMk2BlockEntity.OUTPUT_SLOT_A, 112, 24) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                if (container instanceof DustSmelterMk2BlockEntity blockEntity) {
                    if (blockEntity.getLevel() instanceof ServerLevel serverWorld) {
                        blockEntity.dropExperience(serverWorld, player);
                    }
                }
                super.onTake(player, stack);
            }
        });

        // Slot 3: Output B (x=112, y=48)
        this.addSlot(new Slot(inventory, DustSmelterMk2BlockEntity.OUTPUT_SLOT_B, 112, 48) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                if (container instanceof DustSmelterMk2BlockEntity blockEntity) {
                    if (blockEntity.getLevel() instanceof ServerLevel serverWorld) {
                        blockEntity.dropExperience(serverWorld, player);
                    }
                }
                super.onTake(player, stack);
            }
        });

        // Slot 4: Gear Upgrade (x=152, y=8)
        this.addSlot(new Slot(inventory, DustSmelterMk2BlockEntity.GEAR_SLOT, 152, 8) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof GearItem || stack.is(ModItems.BLAZE_OVERCLOCK_CORE);
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
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();

            if (invSlot < DustSmelterMk2BlockEntity.INVENTORY_SIZE) {
                // Moving from machine to player inventory
                if (!this.moveItemStackTo(originalStack, DustSmelterMk2BlockEntity.INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player inventory to machine
                if (originalStack.getItem() instanceof GearItem || originalStack.is(ModItems.BLAZE_OVERCLOCK_CORE)) {
                    if (!this.moveItemStackTo(originalStack, DustSmelterMk2BlockEntity.GEAR_SLOT, DustSmelterMk2BlockEntity.GEAR_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (DustSmelterMk2BlockEntity.getOutputItem(originalStack.getItem()) != null) {
                    if (!this.moveItemStackTo(originalStack, DustSmelterMk2BlockEntity.INPUT_SLOT_A, DustSmelterMk2BlockEntity.INPUT_SLOT_B + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (invSlot < DustSmelterMk2BlockEntity.INVENTORY_SIZE + 27) {
                    if (!this.moveItemStackTo(originalStack, DustSmelterMk2BlockEntity.INVENTORY_SIZE + 27, this.slots.size(), false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(originalStack, DustSmelterMk2BlockEntity.INVENTORY_SIZE, DustSmelterMk2BlockEntity.INVENTORY_SIZE + 27, false)) {
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
}
