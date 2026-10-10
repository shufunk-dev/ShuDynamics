package net.enchantedwood.screen;

import net.enchantedwood.block.entity.InductionSmelterBlockEntity;
import net.enchantedwood.fluid.MoltenMetal;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.GearItem;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class InductionSmelterScreenHandler extends AbstractContainerMenu {
    private final Container inventory;
    private final ContainerData propertyDelegate;

    public InductionSmelterScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(InductionSmelterBlockEntity.INVENTORY_SIZE), new SimpleContainerData(27));
    }

    public InductionSmelterScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData propertyDelegate) {
        super(ModScreenHandlers.INDUCTION_SMELTER_SCREEN_HANDLER, syncId);
        checkContainerSize(inventory, InductionSmelterBlockEntity.INVENTORY_SIZE);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        this.addDataSlots(propertyDelegate);
        inventory.startOpen(playerInventory.player);

        // Slot 0: Input Slot 1 at x=52, y=26
        this.addSlot(new Slot(inventory, InductionSmelterBlockEntity.INPUT_SLOT_1, 52, 26) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return InductionSmelterBlockEntity.getYield(stack) != null;
            }
        });

        // Slot 1: Input Slot 2 at x=52, y=46
        this.addSlot(new Slot(inventory, InductionSmelterBlockEntity.INPUT_SLOT_2, 52, 46) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return InductionSmelterBlockEntity.getYield(stack) != null;
            }
        });

        // Slot 2: Metallurgy Module Slot at x=88, y=18
        this.addSlot(new Slot(inventory, InductionSmelterBlockEntity.MODULE_SLOT, 88, 18) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.METALLURGY_CONTROLLER_CHIP);
            }
        });

        // Slot 3: Gear Upgrade Slot at x=152, y=8
        this.addSlot(new Slot(inventory, InductionSmelterBlockEntity.GEAR_SLOT, 152, 8) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof GearItem || stack.is(ModItems.BLAZE_OVERCLOCK_CORE);
            }
        });

        // Slot 4: Lava Bucket In at x=116, y=18
        this.addSlot(new Slot(inventory, InductionSmelterBlockEntity.LAVA_IN_SLOT, 116, 18) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.LAVA_BUCKET) || stack.is(ModItems.COPPER_LAVA_BUCKET) || stack.is(ModItems.ENCHANTED_LAVA_BUCKET);
            }
        });

        // Slot 5: Lava Bucket Out at x=116, y=52
        this.addSlot(new Slot(inventory, InductionSmelterBlockEntity.LAVA_OUT_SLOT, 116, 52) {
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

    public boolean isSmelting() {
        return isSmeltingSlot1() || isSmeltingSlot2();
    }

    public boolean isSmeltingSlot1() {
        return propertyDelegate.get(0) > 0;
    }

    public boolean isSmeltingSlot2() {
        return propertyDelegate.get(18) > 0;
    }

    public int getScaledCookProgress1(int pixels) {
        int cookTime = propertyDelegate.get(0);
        int totalCookTime = propertyDelegate.get(1);
        if (totalCookTime <= 0) return 0;
        return (int) (((long) cookTime * pixels) / totalCookTime);
    }

    public int getScaledCookProgress2(int pixels) {
        int cookTime = propertyDelegate.get(18);
        int totalCookTime = propertyDelegate.get(19);
        if (totalCookTime <= 0) return 0;
        return (int) (((long) cookTime * pixels) / totalCookTime);
    }

    public int getScaledCookProgress(int pixels) {
        return Math.max(getScaledCookProgress1(pixels), getScaledCookProgress2(pixels));
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

    public Container getInventory() {
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

    public MoltenMetal getTank1Metal() {
        int ord = this.propertyDelegate.get(20);
        MoltenMetal[] metals = MoltenMetal.values();
        if (ord >= 0 && ord < metals.length) {
            return metals[ord];
        }
        return MoltenMetal.NONE;
    }

    public int getTank1Amount() {
        return (this.propertyDelegate.get(21) & 0xFFFF) | ((this.propertyDelegate.get(22) & 0xFFFF) << 16);
    }

    public int getScaledTank1(int pixels) {
        int amt = getTank1Amount();
        if (amt <= 0) return 0;
        return Math.min(pixels, (int) (((long) amt * pixels) / InductionSmelterBlockEntity.HOLDING_TANK_CAPACITY));
    }

    public MoltenMetal getTank2Metal() {
        int ord = this.propertyDelegate.get(23);
        MoltenMetal[] metals = MoltenMetal.values();
        if (ord >= 0 && ord < metals.length) {
            return metals[ord];
        }
        return MoltenMetal.NONE;
    }

    public int getTank2Amount() {
        return (this.propertyDelegate.get(24) & 0xFFFF) | ((this.propertyDelegate.get(25) & 0xFFFF) << 16);
    }

    public int getScaledTank2(int pixels) {
        int amt = getTank2Amount();
        if (amt <= 0) return 0;
        return Math.min(pixels, (int) (((long) amt * pixels) / InductionSmelterBlockEntity.HOLDING_TANK_CAPACITY));
    }

    public boolean isEjectingHoldingTanks() {
        return this.propertyDelegate.get(26) == 1;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);

        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();

            if (invSlot < InductionSmelterBlockEntity.INVENTORY_SIZE) {
                // Machine inventory -> Player inventory
                if (!this.moveItemStackTo(originalStack, InductionSmelterBlockEntity.INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Player inventory -> Machine inventory
                if (originalStack.getItem() instanceof GearItem || originalStack.is(ModItems.BLAZE_OVERCLOCK_CORE)) {
                    if (!this.moveItemStackTo(originalStack, InductionSmelterBlockEntity.GEAR_SLOT, InductionSmelterBlockEntity.GEAR_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (originalStack.is(ModItems.METALLURGY_CONTROLLER_CHIP)) {
                    if (!this.moveItemStackTo(originalStack, InductionSmelterBlockEntity.MODULE_SLOT, InductionSmelterBlockEntity.MODULE_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (originalStack.is(Items.LAVA_BUCKET) || originalStack.is(ModItems.COPPER_LAVA_BUCKET) || originalStack.is(ModItems.ENCHANTED_LAVA_BUCKET)) {
                    if (!this.moveItemStackTo(originalStack, InductionSmelterBlockEntity.LAVA_IN_SLOT, InductionSmelterBlockEntity.LAVA_IN_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (InductionSmelterBlockEntity.getYield(originalStack) != null) {
                    if (!this.moveItemStackTo(originalStack, InductionSmelterBlockEntity.INPUT_SLOT_1, InductionSmelterBlockEntity.INPUT_SLOT_2 + 1, false)) {
                        return ItemStack.EMPTY;
                    }
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

    @Override
    public boolean stillValid(Player player) {
        return this.inventory.stillValid(player);
    }
}
