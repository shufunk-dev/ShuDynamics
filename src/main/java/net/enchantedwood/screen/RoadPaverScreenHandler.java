package net.enchantedwood.screen;

import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.block.entity.RoadPaverBlockEntity;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.ItemEnergyProvider;
import net.enchantedwood.item.ModItems;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class RoadPaverScreenHandler extends AbstractContainerMenu {
    private final Container inventory;
    private final ContainerData propertyDelegate;

    public RoadPaverScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(RoadPaverBlockEntity.INVENTORY_SIZE), new SimpleContainerData(9));
    }

    public RoadPaverScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData propertyDelegate) {
        super(ModScreenHandlers.ROAD_PAVER_SCREEN_HANDLER, syncId);
        checkContainerSize(inventory, RoadPaverBlockEntity.INVENTORY_SIZE);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        inventory.startOpen(playerInventory.player);
        this.addDataSlots(propertyDelegate);

        // Slots 0-8: 3x3 Asphalt Storage Grid (x=62, y=18)
        for (int r = 0; r < 3; ++r) {
            for (int c = 0; c < 3; ++c) {
                this.addSlot(new Slot(inventory, c + r * 3, 62 + c * 18, 18 + r * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return stack.is(ModBlocks.ASPHALT_BLOCK.asItem()) || stack.is(ModBlocks.ASPHALT_SLAB.asItem())
                                || stack.is(net.minecraft.world.item.Items.CLAY_BALL) || stack.is(net.minecraft.world.item.Items.CLAY)
                                || stack.is(ModBlocks.CONCRETE_CURB.asItem());
                    }
                });
            }
        }

        // Slot 9: Battery Charge Slot (x=12, y=56)
        this.addSlot(new Slot(inventory, RoadPaverBlockEntity.BATTERY_SLOT, 12, 56) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ItemEnergyProvider || stack.getItem() instanceof EnergyProvider;
            }
        });

        // Slot 10: Engine Fuel Slot (x=148, y=56)
        this.addSlot(new Slot(inventory, RoadPaverBlockEntity.FUEL_SLOT, 148, 56) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.GASOLINE_CANISTER) || stack.is(ModItems.BIOFUEL_CANISTER)
                        || stack.is(ModItems.HIGH_OCTANE_FUEL_CANISTER) || stack.is(net.minecraft.world.item.Items.COAL)
                        || stack.is(net.minecraft.world.item.Items.CHARCOAL) || stack.is(ModItems.COKE_COAL);
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
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();
            if (invSlot < RoadPaverBlockEntity.INVENTORY_SIZE) {
                if (!this.moveItemStackTo(originalStack, RoadPaverBlockEntity.INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (originalStack.is(ModBlocks.ASPHALT_BLOCK.asItem()) || originalStack.is(ModBlocks.ASPHALT_SLAB.asItem())
                        || originalStack.is(net.minecraft.world.item.Items.CLAY_BALL) || originalStack.is(net.minecraft.world.item.Items.CLAY)
                        || originalStack.is(ModBlocks.CONCRETE_CURB.asItem())) {
                    if (!this.moveItemStackTo(originalStack, 0, 9, false)) return ItemStack.EMPTY;
                } else if (originalStack.getItem() instanceof ItemEnergyProvider || originalStack.getItem() instanceof EnergyProvider) {
                    if (!this.moveItemStackTo(originalStack, RoadPaverBlockEntity.BATTERY_SLOT, RoadPaverBlockEntity.BATTERY_SLOT + 1, false)) return ItemStack.EMPTY;
                } else if (originalStack.is(ModItems.GASOLINE_CANISTER) || originalStack.is(ModItems.BIOFUEL_CANISTER)
                        || originalStack.is(ModItems.HIGH_OCTANE_FUEL_CANISTER) || originalStack.is(net.minecraft.world.item.Items.COAL)
                        || originalStack.is(net.minecraft.world.item.Items.CHARCOAL) || originalStack.is(ModItems.COKE_COAL)) {
                    if (!this.moveItemStackTo(originalStack, RoadPaverBlockEntity.FUEL_SLOT, RoadPaverBlockEntity.FUEL_SLOT + 1, false)) return ItemStack.EMPTY;
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
}
