package net.enchantedwood.screen;

import net.enchantedwood.block.custom.CastingMode;
import net.enchantedwood.block.entity.CastingPortBlockEntity;
import net.enchantedwood.fluid.MoltenMetal;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CastingPortScreenHandler extends AbstractContainerMenu {
    private final Container inventory;
    private final ContainerData propertyDelegate;

    public CastingPortScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(CastingPortBlockEntity.INVENTORY_SIZE), new SimpleContainerData(9));
    }

    public CastingPortScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData propertyDelegate) {
        super(ModScreenHandlers.CASTING_PORT_SCREEN_HANDLER, syncId);
        checkContainerSize(inventory, CastingPortBlockEntity.INVENTORY_SIZE);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        this.addDataSlots(propertyDelegate);
        inventory.startOpen(playerInventory.player);

        // Slot 0: Solidified Output Slot at x=116, y=35
        this.addSlot(new Slot(inventory, CastingPortBlockEntity.OUTPUT_SLOT, 116, 35) {
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

        // Player Hotbar
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public CastingMode getMode() {
        int ord = propertyDelegate.get(0);
        CastingMode[] modes = CastingMode.values();
        if (ord >= 0 && ord < modes.length) return modes[ord];
        return CastingMode.STANDBY;
    }

    public MoltenMetal getFluidType() {
        int ord = propertyDelegate.get(1);
        MoltenMetal[] metals = MoltenMetal.values();
        if (ord >= 0 && ord < metals.length) return metals[ord];
        return MoltenMetal.NONE;
    }

    public int getFluidAmount() {
        return (propertyDelegate.get(2) & 0xFFFF) | ((propertyDelegate.get(3) & 0xFFFF) << 16);
    }

    public int getScaledFluid(int pixels) {
        int amount = getFluidAmount();
        int max = CastingPortBlockEntity.BUFFER_CAPACITY;
        if (max <= 0) return 0;
        return (int) (((long) amount * pixels) / max);
    }

    public int getScaledProgress(int pixels) {
        int progress = propertyDelegate.get(4);
        int total = propertyDelegate.get(5);
        if (total <= 0) return 0;
        return (int) (((long) progress * pixels) / total);
    }

    public boolean isNetworkOnline() {
        return propertyDelegate.get(8) == 1;
    }

    public Container getInventory() {
        return this.inventory;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);

        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();

            if (invSlot < CastingPortBlockEntity.INVENTORY_SIZE) {
                // Moving from casting port output to player inventory
                if (!this.moveItemStackTo(originalStack, CastingPortBlockEntity.INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
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

    @Override
    public boolean stillValid(Player player) {
        return this.inventory.stillValid(player);
    }
}
