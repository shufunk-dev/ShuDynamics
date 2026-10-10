package net.enchantedwood.screen;

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

public class DustSmelterScreenHandler extends AbstractContainerMenu {
    private final Container inventory;
    private final ContainerData propertyDelegate;

    public DustSmelterScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(3), new SimpleContainerData(7));
    }

    public DustSmelterScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData propertyDelegate) {
        super(ModScreenHandlers.DUST_SMELTER_SCREEN_HANDLER, syncId);
        checkContainerSize(inventory, 3);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        inventory.startOpen(playerInventory.player);
        this.addDataSlots(propertyDelegate);

        // Machine Slots
        // Slot 0: Dust Input (top center)
        this.addSlot(new Slot(inventory, 0, 56, 17));
        // Slot 1: Gear Upgrade Slot (bottom center)
        this.addSlot(new Slot(inventory, 1, 74, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof GearItem;
            }
        });
        // Slot 2: Output Slot (right)
        this.addSlot(new Slot(inventory, 2, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                if (container instanceof net.enchantedwood.block.entity.DustSmelterBlockEntity blockEntity) {
                    if (blockEntity.getLevel() instanceof net.minecraft.server.level.ServerLevel serverWorld) {
                        blockEntity.dropExperience(serverWorld, player);
                    }
                }
                super.onTake(player, stack);
            }
        });

        // Player Inventory (slots 3..29)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player Hotbar (slots 30..38)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public int getEnergy() {
        return (propertyDelegate.get(1) << 16) | (propertyDelegate.get(0) & 0xFFFF);
    }

    public int getMaxEnergy() {
        return (propertyDelegate.get(3) << 16) | (propertyDelegate.get(2) & 0xFFFF);
    }

    public boolean isCrafting() {
        return propertyDelegate.get(4) > 0;
    }

    public int getScaledCookProgress(int arrowSize) {
        int progress = propertyDelegate.get(4);
        int total = propertyDelegate.get(5);
        return total != 0 && progress != 0 ? progress * arrowSize / total : 0;
    }

    public int getScaledEnergy(int barHeight) {
        int energy = getEnergy();
        int max = getMaxEnergy();
        return max > 0 ? (int) ((long) energy * barHeight / max) : 0;
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

            if (slotIndex == 2) {
                if (!this.moveItemStackTo(originalStack, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(originalStack, newStack);
            } else if (slotIndex >= 3) {
                if (originalStack.getItem() instanceof GearItem) {
                    if (!this.moveItemStackTo(originalStack, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(originalStack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(originalStack, 3, 39, false)) {
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
