package net.enchantedwood.screen;

import net.enchantedwood.block.entity.InductionSmelterBlockEntity;
import net.enchantedwood.fluid.MoltenMetal;
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

public class InductionSmelterScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    public InductionSmelterScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(InductionSmelterBlockEntity.INVENTORY_SIZE), new ArrayPropertyDelegate(18));
    }

    public InductionSmelterScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.INDUCTION_SMELTER_SCREEN_HANDLER, syncId);
        checkSize(inventory, InductionSmelterBlockEntity.INVENTORY_SIZE);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        this.addProperties(propertyDelegate);
        inventory.onOpen(playerInventory.player);

        // Slot 0: Input Slot 1 at x=52, y=26
        this.addSlot(new Slot(inventory, InductionSmelterBlockEntity.INPUT_SLOT_1, 52, 26) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return InductionSmelterBlockEntity.getYield(stack) != null;
            }
        });

        // Slot 1: Input Slot 2 at x=52, y=46
        this.addSlot(new Slot(inventory, InductionSmelterBlockEntity.INPUT_SLOT_2, 52, 46) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return InductionSmelterBlockEntity.getYield(stack) != null;
            }
        });

        // Slot 2: Metallurgy Module Slot at x=88, y=18
        this.addSlot(new Slot(inventory, InductionSmelterBlockEntity.MODULE_SLOT, 88, 18) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(ModItems.METALLURGY_CONTROLLER_CHIP);
            }
        });

        // Slot 3: Gear Upgrade Slot at x=152, y=8
        this.addSlot(new Slot(inventory, InductionSmelterBlockEntity.GEAR_SLOT, 152, 8) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.getItem() instanceof GearItem || stack.isOf(ModItems.BLAZE_OVERCLOCK_CORE);
            }
        });

        // Slot 4: Lava Bucket In at x=116, y=18
        this.addSlot(new Slot(inventory, InductionSmelterBlockEntity.LAVA_IN_SLOT, 116, 18) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(Items.LAVA_BUCKET) || stack.isOf(ModItems.COPPER_LAVA_BUCKET) || stack.isOf(ModItems.ENCHANTED_LAVA_BUCKET);
            }
        });

        // Slot 5: Lava Bucket Out at x=116, y=52
        this.addSlot(new Slot(inventory, InductionSmelterBlockEntity.LAVA_OUT_SLOT, 116, 52) {
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

    public boolean isSmelting() {
        return propertyDelegate.get(0) > 0;
    }

    public int getScaledCookProgress(int pixels) {
        int cookTime = propertyDelegate.get(0);
        int totalCookTime = propertyDelegate.get(1);
        if (totalCookTime <= 0) return 0;
        return (int) (((long) cookTime * pixels) / totalCookTime);
    }

    public int getEnergy() {
        return (this.propertyDelegate.get(2) & 0xFFFF) | ((this.propertyDelegate.get(3) & 0xFFFF) << 16);
    }

    public int getMaxEnergy() {
        int max = (this.propertyDelegate.get(4) & 0xFFFF) | ((this.propertyDelegate.get(5) & 0xFFFF) << 16);
        return max > 0 ? max : InductionSmelterBlockEntity.ENERGY_CAPACITY;
    }

    public int getScaledEnergy(int pixels) {
        int energy = getEnergy();
        int max = getMaxEnergy();
        if (max <= 0) return 0;
        return (int) (((long) energy * pixels) / max);
    }

    public int getLava() {
        return (this.propertyDelegate.get(6) & 0xFFFF) | ((this.propertyDelegate.get(7) & 0xFFFF) << 16);
    }

    public int getMaxLava() {
        int max = (this.propertyDelegate.get(8) & 0xFFFF) | ((this.propertyDelegate.get(9) & 0xFFFF) << 16);
        return max > 0 ? max : InductionSmelterBlockEntity.LAVA_CAPACITY;
    }

    public int getScaledLava(int pixels) {
        int lava = getLava();
        int max = getMaxLava();
        if (max <= 0) return 0;
        return (int) (((long) lava * pixels) / max);
    }

    public boolean hasChip() {
        return this.propertyDelegate.get(10) == 1;
    }

    public Inventory getInventory() {
        return this.inventory;
    }

    public boolean isAlloyingEnabled() {
        return this.propertyDelegate.get(11) == 1;
    }

    public int getTotalMoltenVolume() {
        return (this.propertyDelegate.get(12) & 0xFFFF) | ((this.propertyDelegate.get(13) & 0xFFFF) << 16);
    }

    public int getScaledMoltenVolume(int pixels) {
        int volume = getTotalMoltenVolume();
        if (volume <= 0) return 0;
        return Math.min(pixels, (int) (((long) volume * pixels) / InductionSmelterBlockEntity.CHAMBER_CAPACITY));
    }

    public MoltenMetal getMostAbundantFluid() {
        int ord = this.propertyDelegate.get(14);
        MoltenMetal[] metals = MoltenMetal.values();
        if (ord >= 0 && ord < metals.length) {
            return metals[ord];
        }
        return MoltenMetal.NONE;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);

        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            if (invSlot < InductionSmelterBlockEntity.INVENTORY_SIZE) {
                // Machine inventory -> Player inventory
                if (!this.insertItem(originalStack, InductionSmelterBlockEntity.INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Player inventory -> Machine inventory
                if (originalStack.getItem() instanceof GearItem || originalStack.isOf(ModItems.BLAZE_OVERCLOCK_CORE)) {
                    if (!this.insertItem(originalStack, InductionSmelterBlockEntity.GEAR_SLOT, InductionSmelterBlockEntity.GEAR_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (originalStack.isOf(ModItems.METALLURGY_CONTROLLER_CHIP)) {
                    if (!this.insertItem(originalStack, InductionSmelterBlockEntity.MODULE_SLOT, InductionSmelterBlockEntity.MODULE_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (originalStack.isOf(Items.LAVA_BUCKET) || originalStack.isOf(ModItems.COPPER_LAVA_BUCKET) || originalStack.isOf(ModItems.ENCHANTED_LAVA_BUCKET)) {
                    if (!this.insertItem(originalStack, InductionSmelterBlockEntity.LAVA_IN_SLOT, InductionSmelterBlockEntity.LAVA_IN_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (InductionSmelterBlockEntity.getYield(originalStack) != null) {
                    if (!this.insertItem(originalStack, InductionSmelterBlockEntity.INPUT_SLOT_1, InductionSmelterBlockEntity.INPUT_SLOT_2 + 1, false)) {
                        return ItemStack.EMPTY;
                    }
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

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }
}
