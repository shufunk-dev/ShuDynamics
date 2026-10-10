package net.enchantedwood.screen;

import net.enchantedwood.block.entity.VehicleFabricatorBlockEntity;
import net.enchantedwood.energy.EnergyProvider;
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

public class VehicleFabricatorScreenHandler extends AbstractContainerMenu {
    private final Container inventory;
    private final ContainerData propertyDelegate;

    public VehicleFabricatorScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(VehicleFabricatorBlockEntity.INVENTORY_SIZE), new SimpleContainerData(8));
    }

    public VehicleFabricatorScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData propertyDelegate) {
        super(ModScreenHandlers.VEHICLE_FABRICATOR_SCREEN_HANDLER, syncId);
        checkContainerSize(inventory, VehicleFabricatorBlockEntity.INVENTORY_SIZE);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        inventory.startOpen(playerInventory.player);
        this.addDataSlots(propertyDelegate);

        // 0: Vehicle Slot (Modification)
        this.addSlot(new Slot(inventory, VehicleFabricatorBlockEntity.VEHICLE_SLOT, 142, 24) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.ATV_ITEM);
            }
        });

        // 1: Seat Slot
        this.addSlot(new Slot(inventory, VehicleFabricatorBlockEntity.SEAT_SLOT, 70, 22) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.ATV_SEAT);
            }
        });

        // 2: Engine Slot
        this.addSlot(new Slot(inventory, VehicleFabricatorBlockEntity.ENGINE_SLOT, 34, 44) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return VehicleFabricatorBlockEntity.isEngine(stack);
            }
        });

        // 3: Chassis Slot
        this.addSlot(new Slot(inventory, VehicleFabricatorBlockEntity.CHASSIS_SLOT, 70, 54) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return VehicleFabricatorBlockEntity.isChassis(stack);
            }
        });

        // 4: Suspension Slot
        this.addSlot(new Slot(inventory, VehicleFabricatorBlockEntity.SUSPENSION_SLOT, 106, 44) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return VehicleFabricatorBlockEntity.isSuspension(stack);
            }
        });

        // 5: Tires Slot
        this.addSlot(new Slot(inventory, VehicleFabricatorBlockEntity.TIRES_SLOT, 34, 86) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return VehicleFabricatorBlockEntity.isTires(stack);
            }
        });

        // 6: Headlights Slot (Required Core Automotive Part)
        this.addSlot(new Slot(inventory, VehicleFabricatorBlockEntity.HEADLIGHT_SLOT, 70, 86) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return VehicleFabricatorBlockEntity.isHeadlight(stack);
            }
        });

        // 7: Trunk Slot (Optional)
        this.addSlot(new Slot(inventory, VehicleFabricatorBlockEntity.TRUNK_SLOT, 106, 86) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return VehicleFabricatorBlockEntity.isTrunk(stack);
            }
        });

        // 8: Output Slot
        this.addSlot(new Slot(inventory, VehicleFabricatorBlockEntity.OUTPUT_SLOT, 142, 82) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // 9: Battery Slot
        this.addSlot(new Slot(inventory, VehicleFabricatorBlockEntity.BATTERY_SLOT, 6, 118) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof EnergyProvider;
            }
        });

        // Player Inventory
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }

        // Player Hotbar
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 198));
        }
    }

    public int getEnergy() {
        int low = this.propertyDelegate.get(0);
        int high = this.propertyDelegate.get(1);
        return (high << 16) | (low & 0xFFFF);
    }

    public int getMaxEnergy() {
        int low = this.propertyDelegate.get(2);
        int high = this.propertyDelegate.get(3);
        int max = (high << 16) | (low & 0xFFFF);
        return max > 0 ? max : 10000;
    }

    public boolean canFabricate() {
        return this.propertyDelegate.get(4) > 0;
    }

    public int getProgress() {
        return this.propertyDelegate.get(5);
    }

    public int getMaxProgress() {
        int max = this.propertyDelegate.get(6);
        return max > 0 ? max : 200;
    }

    public boolean isFabricating() {
        return this.propertyDelegate.get(7) > 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            if (this.inventory instanceof VehicleFabricatorBlockEntity fabricator) {
                return fabricator.startFabrication();
            }
        }
        return false;
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

            if (slotIndex < VehicleFabricatorBlockEntity.INVENTORY_SIZE) {
                // Move from machine to player inventory
                if (!this.moveItemStackTo(originalStack, VehicleFabricatorBlockEntity.INVENTORY_SIZE, VehicleFabricatorBlockEntity.INVENTORY_SIZE + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Move from player inventory to machine
                if (originalStack.is(ModItems.ATV_ITEM)) {
                    if (!this.moveItemStackTo(originalStack, VehicleFabricatorBlockEntity.VEHICLE_SLOT, VehicleFabricatorBlockEntity.VEHICLE_SLOT + 1, false)) return ItemStack.EMPTY;
                } else if (originalStack.is(ModItems.ATV_SEAT)) {
                    if (!this.moveItemStackTo(originalStack, VehicleFabricatorBlockEntity.SEAT_SLOT, VehicleFabricatorBlockEntity.SEAT_SLOT + 1, false)) return ItemStack.EMPTY;
                } else if (VehicleFabricatorBlockEntity.isEngine(originalStack)) {
                    if (!this.moveItemStackTo(originalStack, VehicleFabricatorBlockEntity.ENGINE_SLOT, VehicleFabricatorBlockEntity.ENGINE_SLOT + 1, false)) return ItemStack.EMPTY;
                } else if (VehicleFabricatorBlockEntity.isChassis(originalStack)) {
                    if (!this.moveItemStackTo(originalStack, VehicleFabricatorBlockEntity.CHASSIS_SLOT, VehicleFabricatorBlockEntity.CHASSIS_SLOT + 1, false)) return ItemStack.EMPTY;
                } else if (VehicleFabricatorBlockEntity.isSuspension(originalStack)) {
                    if (!this.moveItemStackTo(originalStack, VehicleFabricatorBlockEntity.SUSPENSION_SLOT, VehicleFabricatorBlockEntity.SUSPENSION_SLOT + 1, false)) return ItemStack.EMPTY;
                } else if (VehicleFabricatorBlockEntity.isTires(originalStack)) {
                    if (!this.moveItemStackTo(originalStack, VehicleFabricatorBlockEntity.TIRES_SLOT, VehicleFabricatorBlockEntity.TIRES_SLOT + 1, false)) return ItemStack.EMPTY;
                } else if (VehicleFabricatorBlockEntity.isHeadlight(originalStack)) {
                    if (!this.moveItemStackTo(originalStack, VehicleFabricatorBlockEntity.HEADLIGHT_SLOT, VehicleFabricatorBlockEntity.HEADLIGHT_SLOT + 1, false)) return ItemStack.EMPTY;
                } else if (VehicleFabricatorBlockEntity.isTrunk(originalStack)) {
                    if (!this.moveItemStackTo(originalStack, VehicleFabricatorBlockEntity.TRUNK_SLOT, VehicleFabricatorBlockEntity.TRUNK_SLOT + 1, false)) return ItemStack.EMPTY;
                } else if (originalStack.getItem() instanceof EnergyProvider) {
                    if (!this.moveItemStackTo(originalStack, VehicleFabricatorBlockEntity.BATTERY_SLOT, VehicleFabricatorBlockEntity.BATTERY_SLOT + 1, false)) return ItemStack.EMPTY;
                } else {
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
