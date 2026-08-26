package net.enchantedwood.screen;

import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.block.entity.RoadPaverBlockEntity;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.ItemEnergyProvider;
import net.enchantedwood.item.ModItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class RoadPaverScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    public RoadPaverScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(RoadPaverBlockEntity.INVENTORY_SIZE), new ArrayPropertyDelegate(9));
    }

    public RoadPaverScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.ROAD_PAVER_SCREEN_HANDLER, syncId);
        checkSize(inventory, RoadPaverBlockEntity.INVENTORY_SIZE);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        inventory.onOpen(playerInventory.player);
        this.addProperties(propertyDelegate);

        // Slots 0-8: 3x3 Asphalt Storage Grid (x=62, y=18)
        for (int r = 0; r < 3; ++r) {
            for (int c = 0; c < 3; ++c) {
                this.addSlot(new Slot(inventory, c + r * 3, 62 + c * 18, 18 + r * 18) {
                    @Override
                    public boolean canInsert(ItemStack stack) {
                        return stack.isOf(ModBlocks.ASPHALT_BLOCK.asItem()) || stack.isOf(ModBlocks.ASPHALT_SLAB.asItem());
                    }
                });
            }
        }

        // Slot 9: Battery Charge Slot (x=12, y=56)
        this.addSlot(new Slot(inventory, RoadPaverBlockEntity.BATTERY_SLOT, 12, 56) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.getItem() instanceof ItemEnergyProvider || stack.getItem() instanceof EnergyProvider;
            }
        });

        // Slot 10: Engine Fuel Slot (x=148, y=56)
        this.addSlot(new Slot(inventory, RoadPaverBlockEntity.FUEL_SLOT, 148, 56) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(ModItems.GASOLINE_CANISTER) || stack.isOf(ModItems.BIOFUEL_CANISTER)
                        || stack.isOf(ModItems.HIGH_OCTANE_FUEL_CANISTER) || stack.isOf(net.minecraft.item.Items.COAL)
                        || stack.isOf(net.minecraft.item.Items.CHARCOAL) || stack.isOf(ModItems.COKE_COAL);
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

    public boolean isPaving() {
        return propertyDelegate.get(6) == 1;
    }

    public int getScaledProgress() {
        int progress = propertyDelegate.get(0);
        int maxProgress = propertyDelegate.get(1);
        int barHeight = 36;
        return maxProgress != 0 && progress != 0 ? progress * barHeight / maxProgress : 0;
    }

    public long getEnergy() {
        long low = propertyDelegate.get(2) & 0xFFFFL;
        long high = propertyDelegate.get(3) & 0xFFFFL;
        return (high << 16) | low;
    }

    public long getMaxEnergy() {
        long low = propertyDelegate.get(4) & 0xFFFFL;
        long high = propertyDelegate.get(5) & 0xFFFFL;
        return (high << 16) | low;
    }

    public int getScaledEnergy() {
        long energy = getEnergy();
        long maxEnergy = getMaxEnergy();
        int energyBarHeight = 36;
        return maxEnergy != 0 && energy != 0 ? (int) (energy * energyBarHeight / maxEnergy) : 0;
    }

    public int getFuelLevel() {
        return propertyDelegate.get(7);
    }

    public int getMaxFuel() {
        int max = propertyDelegate.get(8);
        return max > 0 ? max : 3000;
    }

    public int getScaledFuel() {
        int fuel = getFuelLevel();
        int maxFuel = getMaxFuel();
        int fuelBarHeight = 36;
        return maxFuel != 0 && fuel != 0 ? (fuel * fuelBarHeight / maxFuel) : 0;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();
            if (invSlot < RoadPaverBlockEntity.INVENTORY_SIZE) {
                if (!this.insertItem(originalStack, RoadPaverBlockEntity.INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (originalStack.isOf(ModBlocks.ASPHALT_BLOCK.asItem()) || originalStack.isOf(ModBlocks.ASPHALT_SLAB.asItem())) {
                    if (!this.insertItem(originalStack, 0, 9, false)) return ItemStack.EMPTY;
                } else if (originalStack.getItem() instanceof ItemEnergyProvider || originalStack.getItem() instanceof EnergyProvider) {
                    if (!this.insertItem(originalStack, RoadPaverBlockEntity.BATTERY_SLOT, RoadPaverBlockEntity.BATTERY_SLOT + 1, false)) return ItemStack.EMPTY;
                } else if (originalStack.isOf(ModItems.GASOLINE_CANISTER) || originalStack.isOf(ModItems.BIOFUEL_CANISTER)
                        || originalStack.isOf(ModItems.HIGH_OCTANE_FUEL_CANISTER) || originalStack.isOf(net.minecraft.item.Items.COAL)
                        || originalStack.isOf(net.minecraft.item.Items.CHARCOAL) || originalStack.isOf(ModItems.COKE_COAL)) {
                    if (!this.insertItem(originalStack, RoadPaverBlockEntity.FUEL_SLOT, RoadPaverBlockEntity.FUEL_SLOT + 1, false)) return ItemStack.EMPTY;
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
}
