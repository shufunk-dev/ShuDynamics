package net.enchantedwood.screen;

import net.enchantedwood.block.entity.PoweredAnvilBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class PoweredAnvilScreenHandler extends AbstractContainerMenu {
    private final Container inventory;
    private final ContainerData propertyDelegate;
    private final BlockPos blockPos;

    public PoweredAnvilScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(PoweredAnvilBlockEntity.INVENTORY_SIZE), new SimpleContainerData(4), BlockPos.ZERO);
    }

    public PoweredAnvilScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData propertyDelegate, BlockPos blockPos) {
        super(ModScreenHandlers.POWERED_ANVIL_SCREEN_HANDLER, syncId);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        this.blockPos = blockPos;
        this.addDataSlots(propertyDelegate);

        // Slot 0: Damaged Equipment Input
        this.addSlot(new Slot(inventory, PoweredAnvilBlockEntity.INPUT_SLOT, 38, 45));

        // Slot 1: Repair Material (Titanium Ingot, etc.)
        this.addSlot(new Slot(inventory, PoweredAnvilBlockEntity.MATERIAL_SLOT, 76, 45));

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

    public int getEnergy() {
        return (this.propertyDelegate.get(1) << 16) | (this.propertyDelegate.get(0) & 0xFFFF);
    }

    public int getMaxEnergy() {
        int max = (this.propertyDelegate.get(3) << 16) | (this.propertyDelegate.get(2) & 0xFFFF);
        return max > 0 ? max : PoweredAnvilBlockEntity.ENERGY_CAPACITY;
    }

    public ItemStack getInputStack() {
        return this.inventory.getItem(PoweredAnvilBlockEntity.INPUT_SLOT);
    }

    public ItemStack getMaterialStack() {
        return this.inventory.getItem(PoweredAnvilBlockEntity.MATERIAL_SLOT);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            // Repair Action
            if (player.level().getBlockEntity(this.blockPos) instanceof PoweredAnvilBlockEntity anvil) {
                anvil.executeRepair(player);
                return true;
            }
        } else if (id == 1) {
            // Switch to Suit Bay Tab
            player.openMenu(new SimpleMenuProvider(
                    (syncId, inv, p) -> new ModularSuitScreenHandler(syncId, inv, this.blockPos),
                    Component.literal("Modular Suit Access Panel")
            ));
            return true;
        }
        return false;
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

            if (invSlot < 2) {
                if (!this.moveItemStackTo(originalStack, 2, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (originalStack.isDamaged()) {
                    if (!this.moveItemStackTo(originalStack, 0, 1, false)) return ItemStack.EMPTY;
                } else {
                    if (!this.moveItemStackTo(originalStack, 1, 2, false)) return ItemStack.EMPTY;
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
}
