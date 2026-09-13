package net.enchantedwood.screen;

import net.enchantedwood.block.custom.CastingMode;
import net.enchantedwood.block.entity.CastingPortBlockEntity;
import net.enchantedwood.fluid.MoltenMetal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class CastingPortScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    public CastingPortScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(CastingPortBlockEntity.INVENTORY_SIZE), new ArrayPropertyDelegate(8));
    }

    public CastingPortScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.CASTING_PORT_SCREEN_HANDLER, syncId);
        checkSize(inventory, CastingPortBlockEntity.INVENTORY_SIZE);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        this.addProperties(propertyDelegate);
        inventory.onOpen(playerInventory.player);

        // Slot 0: Solidified Output Slot at x=116, y=35
        this.addSlot(new Slot(inventory, CastingPortBlockEntity.OUTPUT_SLOT, 116, 35) {
            @Override
            public boolean canInsert(ItemStack stack) {
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
        return CastingMode.INGOT;
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

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);

        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            if (invSlot < CastingPortBlockEntity.INVENTORY_SIZE) {
                // Moving from casting port output to player inventory
                if (!this.insertItem(originalStack, CastingPortBlockEntity.INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
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

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }
}
