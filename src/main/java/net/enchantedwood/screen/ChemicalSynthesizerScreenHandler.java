package net.enchantedwood.screen;

import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.block.entity.ChemicalSynthesizerBlockEntity;
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

public class ChemicalSynthesizerScreenHandler extends AbstractContainerMenu {
    private final Container inventory;
    private final ContainerData propertyDelegate;

    public ChemicalSynthesizerScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(ChemicalSynthesizerBlockEntity.INVENTORY_SIZE), new SimpleContainerData(7));
    }

    public ChemicalSynthesizerScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData propertyDelegate) {
        super(ModScreenHandlers.CHEMICAL_SYNTHESIZER_SCREEN_HANDLER, syncId);
        checkContainerSize(inventory, ChemicalSynthesizerBlockEntity.INVENTORY_SIZE);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        this.addDataSlots(propertyDelegate);
        inventory.startOpen(playerInventory.player);

        // Slot 0: Empty Cartridge
        this.addSlot(new Slot(inventory, ChemicalSynthesizerBlockEntity.SLOT_CARTRIDGE, 44, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() == ModItems.EMPTY_CARTRIDGE;
            }
        });

        // Slot 1: Primary Essence
        this.addSlot(new Slot(inventory, ChemicalSynthesizerBlockEntity.SLOT_ESSENCE, 44, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ChemicalSynthesizerBlockEntity.isEssence(stack);
            }
        });

        // Slot 2: Catalyst / Stabilizer
        this.addSlot(new Slot(inventory, ChemicalSynthesizerBlockEntity.SLOT_CATALYST, 44, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ChemicalSynthesizerBlockEntity.isCatalyst(stack);
            }
        });

        // Slot 3: Finished Output Cartridge
        this.addSlot(new Slot(inventory, ChemicalSynthesizerBlockEntity.SLOT_OUTPUT, 120, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Slot 4: Gear Upgrade
        this.addSlot(new Slot(inventory, ChemicalSynthesizerBlockEntity.GEAR_SLOT, 152, 8) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof GearItem;
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

    public int getScaledCookProgress(int arrowWidth) {
        int cookTime = this.propertyDelegate.get(0);
        int totalCookTime = this.propertyDelegate.get(1);
        return totalCookTime != 0 && cookTime != 0 ? cookTime * arrowWidth / totalCookTime : 0;
    }

    public int getScaledEnergy(int barHeight) {
        int energy = this.getEnergy();
        int maxEnergy = this.getMaxEnergy();
        return maxEnergy != 0 && energy != 0 ? (int) ((long) energy * barHeight / maxEnergy) : 0;
    }

    public int getEnergy() {
        return (this.propertyDelegate.get(2) & 0xFFFF) | ((this.propertyDelegate.get(3) & 0xFFFF) << 16);
    }

    public int getMaxEnergy() {
        return (this.propertyDelegate.get(4) & 0xFFFF) | ((this.propertyDelegate.get(5) & 0xFFFF) << 16);
    }

    public GearTier getGearTier() {
        int ordinal = this.propertyDelegate.get(6);
        return (ordinal >= 0 && ordinal < GearTier.values().length) ? GearTier.values()[ordinal] : GearTier.NONE;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();

            if (invSlot < ChemicalSynthesizerBlockEntity.INVENTORY_SIZE) {
                if (!this.moveItemStackTo(originalStack, ChemicalSynthesizerBlockEntity.INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (originalStack.getItem() == ModItems.EMPTY_CARTRIDGE) {
                    if (!this.moveItemStackTo(originalStack, ChemicalSynthesizerBlockEntity.SLOT_CARTRIDGE, ChemicalSynthesizerBlockEntity.SLOT_CARTRIDGE + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (ChemicalSynthesizerBlockEntity.isEssence(originalStack)) {
                    if (!this.moveItemStackTo(originalStack, ChemicalSynthesizerBlockEntity.SLOT_ESSENCE, ChemicalSynthesizerBlockEntity.SLOT_ESSENCE + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (ChemicalSynthesizerBlockEntity.isCatalyst(originalStack)) {
                    if (!this.moveItemStackTo(originalStack, ChemicalSynthesizerBlockEntity.SLOT_CATALYST, ChemicalSynthesizerBlockEntity.SLOT_CATALYST + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (originalStack.getItem() instanceof GearItem) {
                    if (!this.moveItemStackTo(originalStack, ChemicalSynthesizerBlockEntity.GEAR_SLOT, ChemicalSynthesizerBlockEntity.GEAR_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (invSlot < ChemicalSynthesizerBlockEntity.INVENTORY_SIZE + 27) {
                        if (!this.moveItemStackTo(originalStack, ChemicalSynthesizerBlockEntity.INVENTORY_SIZE + 27, this.slots.size(), false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!this.moveItemStackTo(originalStack, ChemicalSynthesizerBlockEntity.INVENTORY_SIZE, ChemicalSynthesizerBlockEntity.INVENTORY_SIZE + 27, false)) {
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
