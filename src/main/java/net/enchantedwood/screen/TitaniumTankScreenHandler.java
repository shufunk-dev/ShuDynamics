package net.enchantedwood.screen;

import net.enchantedwood.block.entity.TitaniumTankControllerBlockEntity;
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

public class TitaniumTankScreenHandler extends AbstractContainerMenu {
    private final Container inventory;
    private final ContainerData propertyDelegate;

    public TitaniumTankScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(TitaniumTankControllerBlockEntity.INVENTORY_SIZE), new SimpleContainerData(7));
    }

    public TitaniumTankScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData propertyDelegate) {
        super(ModScreenHandlers.TITANIUM_TANK_SCREEN_HANDLER, syncId);
        checkContainerSize(inventory, TitaniumTankControllerBlockEntity.INVENTORY_SIZE);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        this.addDataSlots(propertyDelegate);
        inventory.startOpen(playerInventory.player);

        // Slot 0: Bucket Input (Fill or Drain Tank) (x=38, y=26)
        this.addSlot(new Slot(inventory, TitaniumTankControllerBlockEntity.BUCKET_IN_SLOT, 38, 26) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.LAVA_BUCKET) || stack.is(Items.BUCKET);
            }
        });

        // Slot 1: Bucket Output (x=38, y=56)
        this.addSlot(new Slot(inventory, TitaniumTankControllerBlockEntity.BUCKET_OUT_SLOT, 38, 56) {
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

        // Hotbar
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public int getLavaAmount() {
        return (propertyDelegate.get(0) & 0xFFFF) | ((propertyDelegate.get(1) & 0xFFFF) << 16);
    }

    public int getMaxLava() {
        return (propertyDelegate.get(2) & 0xFFFF) | ((propertyDelegate.get(3) & 0xFFFF) << 16);
    }

    public boolean isFormed() {
        return propertyDelegate.get(4) == 1;
    }

    public net.enchantedwood.fluid.MoltenMetal getFluidType() {
        if (propertyDelegate.getCount() > 5) {
            int index = propertyDelegate.get(5);
            net.enchantedwood.fluid.MoltenMetal[] metals = net.enchantedwood.fluid.MoltenMetal.values();
            if (index >= 0 && index < metals.length) {
                return metals[index];
            }
        }
        return net.enchantedwood.fluid.MoltenMetal.LAVA;
    }

    public net.enchantedwood.fluid.MoltenMetal getFilterFluid() {
        if (propertyDelegate.getCount() > 6) {
            int index = propertyDelegate.get(6);
            net.enchantedwood.fluid.MoltenMetal[] metals = net.enchantedwood.fluid.MoltenMetal.values();
            if (index >= 0 && index < metals.length) {
                return metals[index];
            }
        }
        return net.enchantedwood.fluid.MoltenMetal.NONE;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();

            if (slotIndex < TitaniumTankControllerBlockEntity.INVENTORY_SIZE) {
                if (!this.moveItemStackTo(originalStack, TitaniumTankControllerBlockEntity.INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (originalStack.is(Items.LAVA_BUCKET) || originalStack.is(Items.BUCKET)) {
                    if (!this.moveItemStackTo(originalStack, TitaniumTankControllerBlockEntity.BUCKET_IN_SLOT, TitaniumTankControllerBlockEntity.BUCKET_IN_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (slotIndex < TitaniumTankControllerBlockEntity.INVENTORY_SIZE + 27) {
                    if (!this.moveItemStackTo(originalStack, TitaniumTankControllerBlockEntity.INVENTORY_SIZE + 27, this.slots.size(), false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(originalStack, TitaniumTankControllerBlockEntity.INVENTORY_SIZE, TitaniumTankControllerBlockEntity.INVENTORY_SIZE + 27, false)) {
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
