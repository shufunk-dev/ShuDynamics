package net.enchantedwood.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.enchantedwood.block.entity.VehicleFabricatorBlockEntity;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.item.ModItems;

public class VehicleFabricatorScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    public VehicleFabricatorScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(VehicleFabricatorBlockEntity.INVENTORY_SIZE), new ArrayPropertyDelegate(5));
    }

    public VehicleFabricatorScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.VEHICLE_FABRICATOR_SCREEN_HANDLER, syncId);
        checkSize(inventory, VehicleFabricatorBlockEntity.INVENTORY_SIZE);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        inventory.onOpen(playerInventory.player);
        this.addProperties(propertyDelegate);

        // 0: Vehicle Slot (Modification)
        this.addSlot(new Slot(inventory, VehicleFabricatorBlockEntity.VEHICLE_SLOT, 142, 24) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(ModItems.ATV_ITEM);
            }
        });

        // 1: Seat Slot
        this.addSlot(new Slot(inventory, VehicleFabricatorBlockEntity.SEAT_SLOT, 70, 22) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(ModItems.ATV_SEAT);
            }
        });

        // 2: Engine Slot
        this.addSlot(new Slot(inventory, VehicleFabricatorBlockEntity.ENGINE_SLOT, 34, 44) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return VehicleFabricatorBlockEntity.isEngine(stack);
            }
        });

        // 3: Chassis Slot
        this.addSlot(new Slot(inventory, VehicleFabricatorBlockEntity.CHASSIS_SLOT, 70, 54) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return VehicleFabricatorBlockEntity.isChassis(stack);
            }
        });

        // 4: Suspension Slot
        this.addSlot(new Slot(inventory, VehicleFabricatorBlockEntity.SUSPENSION_SLOT, 106, 44) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return VehicleFabricatorBlockEntity.isSuspension(stack);
            }
        });

        // 5: Tires Slot
        this.addSlot(new Slot(inventory, VehicleFabricatorBlockEntity.TIRES_SLOT, 34, 86) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return VehicleFabricatorBlockEntity.isTires(stack);
            }
        });

        // 6: Trunk Slot
        this.addSlot(new Slot(inventory, VehicleFabricatorBlockEntity.TRUNK_SLOT, 106, 86) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return VehicleFabricatorBlockEntity.isTrunk(stack);
            }
        });

        // 7: Output Slot
        this.addSlot(new Slot(inventory, VehicleFabricatorBlockEntity.OUTPUT_SLOT, 142, 82) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }
        });

        // 8: Battery Slot
        this.addSlot(new Slot(inventory, VehicleFabricatorBlockEntity.BATTERY_SLOT, 6, 118) {
            @Override
            public boolean canInsert(ItemStack stack) {
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

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id == 0) {
            if (this.inventory instanceof VehicleFabricatorBlockEntity fabricator) {
                return fabricator.fabricateOrUpgrade();
            }
        }
        return false;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            if (slotIndex < VehicleFabricatorBlockEntity.INVENTORY_SIZE) {
                // Move from machine to player inventory
                if (!this.insertItem(originalStack, VehicleFabricatorBlockEntity.INVENTORY_SIZE, VehicleFabricatorBlockEntity.INVENTORY_SIZE + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Move from player inventory to machine
                if (originalStack.isOf(ModItems.ATV_ITEM)) {
                    if (!this.insertItem(originalStack, VehicleFabricatorBlockEntity.VEHICLE_SLOT, VehicleFabricatorBlockEntity.VEHICLE_SLOT + 1, false)) return ItemStack.EMPTY;
                } else if (originalStack.isOf(ModItems.ATV_SEAT)) {
                    if (!this.insertItem(originalStack, VehicleFabricatorBlockEntity.SEAT_SLOT, VehicleFabricatorBlockEntity.SEAT_SLOT + 1, false)) return ItemStack.EMPTY;
                } else if (VehicleFabricatorBlockEntity.isEngine(originalStack)) {
                    if (!this.insertItem(originalStack, VehicleFabricatorBlockEntity.ENGINE_SLOT, VehicleFabricatorBlockEntity.ENGINE_SLOT + 1, false)) return ItemStack.EMPTY;
                } else if (VehicleFabricatorBlockEntity.isChassis(originalStack)) {
                    if (!this.insertItem(originalStack, VehicleFabricatorBlockEntity.CHASSIS_SLOT, VehicleFabricatorBlockEntity.CHASSIS_SLOT + 1, false)) return ItemStack.EMPTY;
                } else if (VehicleFabricatorBlockEntity.isSuspension(originalStack)) {
                    if (!this.insertItem(originalStack, VehicleFabricatorBlockEntity.SUSPENSION_SLOT, VehicleFabricatorBlockEntity.SUSPENSION_SLOT + 1, false)) return ItemStack.EMPTY;
                } else if (VehicleFabricatorBlockEntity.isTires(originalStack)) {
                    if (!this.insertItem(originalStack, VehicleFabricatorBlockEntity.TIRES_SLOT, VehicleFabricatorBlockEntity.TIRES_SLOT + 1, false)) return ItemStack.EMPTY;
                } else if (VehicleFabricatorBlockEntity.isTrunk(originalStack)) {
                    if (!this.insertItem(originalStack, VehicleFabricatorBlockEntity.TRUNK_SLOT, VehicleFabricatorBlockEntity.TRUNK_SLOT + 1, false)) return ItemStack.EMPTY;
                } else if (originalStack.getItem() instanceof EnergyProvider) {
                    if (!this.insertItem(originalStack, VehicleFabricatorBlockEntity.BATTERY_SLOT, VehicleFabricatorBlockEntity.BATTERY_SLOT + 1, false)) return ItemStack.EMPTY;
                } else {
                    return ItemStack.EMPTY;
                }
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
}
