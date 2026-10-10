package net.enchantedwood.screen;

import net.enchantedwood.entity.custom.AtvEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class AtvScreenHandler extends AbstractContainerMenu {
    private final Container inventory;

    public AtvScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(AtvEntity.TOTAL_INVENTORY_SIZE));
    }

    public AtvScreenHandler(int syncId, Inventory playerInventory, Container inventory) {
        super(ModScreenHandlers.ATV_SCREEN_HANDLER, syncId);
        checkContainerSize(inventory, AtvEntity.TOTAL_INVENTORY_SIZE);
        this.inventory = inventory;

        inventory.startOpen(playerInventory.player);

        // 1. Installed Vehicle Components (Locked / Display-Only; modify at Vehicle Fabricator)
        // Column 1: Engine, Tires, Suspension
        this.addSlot(new ReadOnlyModuleSlot(inventory, AtvEntity.ENGINE_SLOT, 10, 18));
        this.addSlot(new ReadOnlyModuleSlot(inventory, AtvEntity.TIRE_SLOT, 10, 36));
        this.addSlot(new ReadOnlyModuleSlot(inventory, AtvEntity.SUSPENSION_SLOT, 10, 54));

        // Column 2: Chassis, Headlights, Trunk
        this.addSlot(new ReadOnlyModuleSlot(inventory, AtvEntity.CHASSIS_SLOT, 28, 18));
        this.addSlot(new ReadOnlyModuleSlot(inventory, AtvEntity.HEADLIGHT_SLOT, 28, 36));
        this.addSlot(new ReadOnlyModuleSlot(inventory, AtvEntity.TRUNK_SLOT, 28, 54));

        // Slot 6: Fuel / Battery Slot (Interactive; accepts fuel & batteries)
        this.addSlot(new Slot(inventory, AtvEntity.FUEL_SLOT, 142, 18) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isFuel(stack);
            }
        });

        // Slot 7: Front Tool Attachment Slot (Interactive; accepts Drill Bits, Tree Saws, Crop Harvesters)
        this.addSlot(new Slot(inventory, AtvEntity.TOOL_SLOT, 142, 54) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isToolAttachment(stack);
            }
        });

        // 2. Trunk Storage (Slots 8-16: 9 cargo slots in 3x3 center rack)
        for (int r = 0; r < 3; ++r) {
            for (int c = 0; c < 3; ++c) {
                this.addSlot(new Slot(inventory, AtvEntity.MODULE_SLOTS_COUNT + c + r * 3, 62 + c * 18, 18 + r * 18));
            }
        }

        // 3. Player Inventory (3x9)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // 4. Player Hotbar (9 slots)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public static boolean isToolAttachment(ItemStack stack) {
        return stack.getItem() instanceof net.enchantedwood.item.custom.DrillBitItem
                || stack.getItem() instanceof net.enchantedwood.item.custom.TreeSawItem
                || stack.getItem() instanceof net.enchantedwood.item.custom.CropHarvesterItem;
    }

    public static boolean isFuel(ItemStack stack) {
        return stack.is(net.enchantedwood.item.ModItems.GASOLINE_CANISTER)
                || stack.is(net.enchantedwood.item.ModItems.BIOFUEL_CANISTER)
                || stack.is(net.enchantedwood.item.ModItems.HIGH_OCTANE_FUEL_CANISTER)
                || stack.is(net.minecraft.world.item.Items.COAL)
                || stack.is(net.minecraft.world.item.Items.CHARCOAL)
                || stack.getItem() instanceof net.enchantedwood.energy.EnergyProvider;
    }

    private static class ReadOnlyModuleSlot extends Slot {
        public ReadOnlyModuleSlot(Container inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player playerEntity) {
            return false;
        }
    }

    public Container getAtvInventory() {
        return this.inventory;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();

            if (invSlot < 17) { // ATV Slots (0..16)
                if (invSlot < 6) {
                    // Cannot move locked module slots (Engine, Tires, Suspension, Chassis, Headlights, Trunk)
                    return ItemStack.EMPTY;
                }
                // Move from ATV fuel slot (6), tool slot (7), or trunk storage (8-16) to player inventory
                if (!this.moveItemStackTo(originalStack, 17, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from Player Inventory into ATV:
                if (isToolAttachment(originalStack)) {
                    // Try tool slot first
                    if (!this.moveItemStackTo(originalStack, 7, 8, false)) {
                        // Then trunk storage
                        if (!this.moveItemStackTo(originalStack, 8, 17, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                } else if (isFuel(originalStack)) {
                    // Try fuel slot first
                    if (!this.moveItemStackTo(originalStack, 6, 7, false)) {
                        // Then trunk storage
                        if (!this.moveItemStackTo(originalStack, 8, 17, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                } else {
                    // Regular items go directly into cargo trunk storage
                    if (!this.moveItemStackTo(originalStack, 8, 17, false)) {
                        return ItemStack.EMPTY;
                    }
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
