package net.enchantedwood.screen;

import net.enchantedwood.entity.custom.AtvEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class AtvScreenHandler extends ScreenHandler {
    private final Inventory inventory;

    public AtvScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(AtvEntity.TOTAL_INVENTORY_SIZE));
    }

    public AtvScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory) {
        super(ModScreenHandlers.ATV_SCREEN_HANDLER, syncId);
        checkSize(inventory, AtvEntity.TOTAL_INVENTORY_SIZE);
        this.inventory = inventory;

        inventory.onOpen(playerInventory.player);

        // 1. Module Slots
        // Slot 0: Engine (x=18, y=18)
        this.addSlot(new Slot(inventory, AtvEntity.ENGINE_SLOT, 18, 18) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(net.enchantedwood.item.ModItems.COPPER_ATV_ENGINE)
                        || stack.isOf(net.enchantedwood.item.ModItems.ALUMINUM_ATV_ENGINE)
                        || stack.isOf(net.enchantedwood.item.ModItems.STEEL_ATV_ENGINE)
                        || stack.isOf(net.enchantedwood.item.ModItems.TITANIUM_ATV_ENGINE);
            }
        });
        // Slot 1: Tires (x=18, y=38)
        this.addSlot(new Slot(inventory, AtvEntity.TIRE_SLOT, 18, 38) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(net.enchantedwood.item.ModItems.RUBBER_TIRE)
                        || stack.isOf(net.enchantedwood.item.ModItems.STEEL_RIM_TIRE)
                        || stack.isOf(net.enchantedwood.item.ModItems.TITANIUM_STUDDED_TIRE);
            }
        });
        // Slot 2: Suspension (x=18, y=58)
        this.addSlot(new Slot(inventory, AtvEntity.SUSPENSION_SLOT, 18, 58) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(net.enchantedwood.item.ModItems.STEEL_SUSPENSION)
                        || stack.isOf(net.enchantedwood.item.ModItems.TITANIUM_SUSPENSION);
            }
        });
        // Slot 3: Chassis (x=142, y=18)
        this.addSlot(new Slot(inventory, AtvEntity.CHASSIS_SLOT, 142, 18) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(net.enchantedwood.item.ModItems.ALUMINUM_ATV_CHASSIS)
                        || stack.isOf(net.enchantedwood.item.ModItems.STEEL_ATV_CHASSIS)
                        || stack.isOf(net.enchantedwood.item.ModItems.TITANIUM_ATV_CHASSIS);
            }
        });
        // Slot 4: Cargo Trunk (x=142, y=38)
        this.addSlot(new Slot(inventory, AtvEntity.TRUNK_SLOT, 142, 38) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(net.enchantedwood.item.ModItems.SMALL_CARGO_TRUNK)
                        || stack.isOf(net.enchantedwood.item.ModItems.MEDIUM_CARGO_TRUNK)
                        || stack.isOf(net.enchantedwood.item.ModItems.LARGE_CARGO_TRUNK);
            }
        });
        // Slot 5: Fuel / Battery (x=142, y=58)
        this.addSlot(new Slot(inventory, AtvEntity.FUEL_SLOT, 142, 58) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(net.enchantedwood.item.ModItems.GASOLINE_CANISTER)
                        || stack.isOf(net.enchantedwood.item.ModItems.BIOFUEL_CANISTER)
                        || stack.isOf(net.enchantedwood.item.ModItems.HIGH_OCTANE_FUEL_CANISTER)
                        || stack.isOf(net.minecraft.item.Items.COAL)
                        || stack.isOf(net.minecraft.item.Items.CHARCOAL)
                        || stack.getItem() instanceof net.enchantedwood.energy.EnergyProvider;
            }
        });

        // 2. Trunk Storage (Slots 6-14: 9 slots starting at x=44, y=28 - 3x3 compact rack)
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

    public Inventory getAtvInventory() {
        return this.inventory;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();
            if (invSlot < 15) { // ATV slots (6 module + 9 trunk)
                if (!this.insertItem(originalStack, 15, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.insertItem(originalStack, 0, 15, false)) {
                return ItemStack.EMPTY;
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
