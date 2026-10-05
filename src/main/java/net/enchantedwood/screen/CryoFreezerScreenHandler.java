package net.enchantedwood.screen;

import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.block.entity.CryoFreezerBlockEntity;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.GearItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class CryoFreezerScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    public CryoFreezerScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(CryoFreezerBlockEntity.INVENTORY_SIZE), new ArrayPropertyDelegate(8));
    }

    public CryoFreezerScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.CRYO_FREEZER_SCREEN_HANDLER, syncId);
        checkSize(inventory, CryoFreezerBlockEntity.INVENTORY_SIZE);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        inventory.onOpen(playerInventory.player);
        this.addProperties(propertyDelegate);

        // 0. Water In Slot (Bucket) at (68, 20)
        this.addSlot(new Slot(inventory, CryoFreezerBlockEntity.WATER_IN_SLOT, 68, 20) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(Items.WATER_BUCKET) || stack.isOf(ModItems.COPPER_WATER_BUCKET);
            }
        });

        // 1. Bucket Return Out Slot at (68, 52)
        this.addSlot(new Slot(inventory, CryoFreezerBlockEntity.BUCKET_OUT_SLOT, 68, 52) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }
        });

        // 2. Solid Material Input Slot (Ice, Packed Ice, Snowball, Ice Cubes) at (98, 35)
        this.addSlot(new Slot(inventory, CryoFreezerBlockEntity.SOLID_IN_SLOT, 98, 35) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(Items.ICE) || stack.isOf(Items.PACKED_ICE) || stack.isOf(Items.SNOWBALL) || stack.isOf(ModItems.ICE_CUBES);
            }
        });

        // 3. Frozen Product Output Slot at (146, 35)
        this.addSlot(new Slot(inventory, CryoFreezerBlockEntity.OUTPUT_SLOT, 146, 35) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }
        });

        // 4. Overclock Gear Slot at standard (152, 8)
        this.addSlot(new Slot(inventory, CryoFreezerBlockEntity.GEAR_SLOT, 152, 8) {
            @Override
            public int getMaxItemCount() {
                return 1;
            }

            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.getItem() instanceof GearItem;
            }
        });

        // Player Inventory (3 rows x 9 columns)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player Hotbar (1 row x 9 columns)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public int getEnergy() {
        return (this.propertyDelegate.get(3) << 16) | (this.propertyDelegate.get(2) & 0xFFFF);
    }

    public int getMaxEnergy() {
        return (this.propertyDelegate.get(5) << 16) | (this.propertyDelegate.get(4) & 0xFFFF);
    }

    public int getWaterAmount() {
        return this.propertyDelegate.get(6);
    }

    public int getFreezeProgress() {
        return this.propertyDelegate.get(0);
    }

    public int getTotalFreezeTime() {
        return this.propertyDelegate.get(1);
    }

    public GearTier getActiveGearTier() {
        int ordinal = this.propertyDelegate.get(7);
        GearTier[] tiers = GearTier.values();
        if (ordinal >= 0 && ordinal < tiers.length) {
            return tiers[ordinal];
        }
        return GearTier.NONE;
    }

    public int getScaledEnergy(int pixels) {
        int energy = getEnergy();
        int max = getMaxEnergy();
        if (max <= 0) max = CryoFreezerBlockEntity.CAPACITY;
        return (int) (((long) energy * pixels) / max);
    }

    public int getScaledWater(int pixels) {
        int water = getWaterAmount();
        int max = CryoFreezerBlockEntity.MAX_WATER;
        return (water * pixels) / max;
    }

    public int getScaledProgress(int pixels) {
        int progress = getFreezeProgress();
        int total = getTotalFreezeTime();
        if (total <= 0) total = 80;
        return (progress * pixels) / total;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            // Machine output / bucket out -> Player inventory
            if (invSlot == CryoFreezerBlockEntity.OUTPUT_SLOT || invSlot == CryoFreezerBlockEntity.BUCKET_OUT_SLOT) {
                if (!this.insertItem(originalStack, 5, 41, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickTransfer(originalStack, newStack);
            }
            // Machine inputs / gear -> Player inventory
            else if (invSlot < 5) {
                if (!this.insertItem(originalStack, 5, 41, false)) {
                    return ItemStack.EMPTY;
                }
            }
            // Player inventory -> Machine slots
            else {
                if (originalStack.getItem() instanceof GearItem) {
                    if (!this.insertItem(originalStack, CryoFreezerBlockEntity.GEAR_SLOT, CryoFreezerBlockEntity.GEAR_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (originalStack.isOf(Items.WATER_BUCKET) || originalStack.isOf(ModItems.COPPER_WATER_BUCKET)) {
                    if (!this.insertItem(originalStack, CryoFreezerBlockEntity.WATER_IN_SLOT, CryoFreezerBlockEntity.WATER_IN_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (originalStack.isOf(Items.ICE) || originalStack.isOf(Items.PACKED_ICE) || originalStack.isOf(Items.SNOWBALL) || originalStack.isOf(ModItems.ICE_CUBES)) {
                    if (!this.insertItem(originalStack, CryoFreezerBlockEntity.SOLID_IN_SLOT, CryoFreezerBlockEntity.SOLID_IN_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (invSlot >= 5 && invSlot < 32) {
                    if (!this.insertItem(originalStack, 32, 41, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (invSlot >= 32 && invSlot < 41) {
                    if (!this.insertItem(originalStack, 5, 32, false)) {
                        return ItemStack.EMPTY;
                    }
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

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }
}
