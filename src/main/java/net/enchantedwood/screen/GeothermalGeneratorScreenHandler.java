package net.enchantedwood.screen;

import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.block.entity.GeothermalGeneratorBlockEntity;
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

public class GeothermalGeneratorScreenHandler extends AbstractContainerMenu {
    private final Container inventory;
    private final ContainerData propertyDelegate;

    public GeothermalGeneratorScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(GeothermalGeneratorBlockEntity.INVENTORY_SIZE), new SimpleContainerData(8));
    }

    public GeothermalGeneratorScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData propertyDelegate) {
        super(ModScreenHandlers.GEOTHERMAL_GENERATOR_SCREEN_HANDLER, syncId);
        checkContainerSize(inventory, GeothermalGeneratorBlockEntity.INVENTORY_SIZE);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        this.addDataSlots(propertyDelegate);
        inventory.startOpen(playerInventory.player);

        // Slot 0: Fuel/Bucket Input (x=44, y=25)
        this.addSlot(new Slot(inventory, GeothermalGeneratorBlockEntity.FUEL_SLOT, 44, 25) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.LAVA_BUCKET) || stack.is(Items.MAGMA_BLOCK) || stack.is(ModItems.FIRE_CRYSTAL);
            }
        });

        // Slot 1: Empty Bucket Output (x=44, y=53)
        this.addSlot(new Slot(inventory, GeothermalGeneratorBlockEntity.BUCKET_OUTPUT_SLOT, 44, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Slot 2: Gear Upgrade (x=152, y=8)
        this.addSlot(new Slot(inventory, GeothermalGeneratorBlockEntity.GEAR_SLOT, 152, 8) {
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

    public boolean isBurning() {
        return propertyDelegate.get(0) > 0;
    }

    public int getScaledFuelProgress(int pixels) {
        int burnTime = propertyDelegate.get(0);
        int totalBurnTime = propertyDelegate.get(1);
        if (totalBurnTime <= 0) return 0;
        return (int) (((long) burnTime * pixels) / totalBurnTime);
    }

    public int getEnergy() {
        return (this.propertyDelegate.get(2) & 0xFFFF) | ((this.propertyDelegate.get(3) & 0xFFFF) << 16);
    }

    public int getMaxEnergy() {
        int max = (this.propertyDelegate.get(4) & 0xFFFF) | ((this.propertyDelegate.get(5) & 0xFFFF) << 16);
        return max > 0 ? max : GeothermalGeneratorBlockEntity.CAPACITY;
    }

    public int getScaledEnergy(int pixels) {
        int max = getMaxEnergy();
        if (max <= 0) return 0;
        return (int) (((long) getEnergy() * pixels) / max);
    }

    public int getLavaAmount() {
        return this.propertyDelegate.get(6);
    }

    public int getScaledLava(int pixels) {
        return (int) (((long) getLavaAmount() * pixels) / GeothermalGeneratorBlockEntity.MAX_LAVA);
    }

    public GearTier getGearTier() {
        int index = propertyDelegate.get(7);
        if (index >= 0 && index < GearTier.values().length) {
            return GearTier.values()[index];
        }
        return GearTier.NONE;
    }

    public float getGearMultiplier() {
        return switch (getGearTier()) {
            case IRON -> 1.25f;
            case COPPER -> 1.4f;
            case BRONZE -> 1.6f;
            case GOLD -> 1.8f;
            case TITANIUM -> 2.0f;
            case DIAMOND -> 2.2f;
            case NETHERITE -> 3.0f;
            default -> 1.0f;
        };
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();
            if (invSlot < GeothermalGeneratorBlockEntity.INVENTORY_SIZE) {
                if (!this.moveItemStackTo(originalStack, GeothermalGeneratorBlockEntity.INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (originalStack.getItem() instanceof GearItem) {
                    if (!this.moveItemStackTo(originalStack, GeothermalGeneratorBlockEntity.GEAR_SLOT, GeothermalGeneratorBlockEntity.GEAR_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (originalStack.is(Items.LAVA_BUCKET) || originalStack.is(Items.MAGMA_BLOCK) || originalStack.is(ModItems.FIRE_CRYSTAL)) {
                    if (!this.moveItemStackTo(originalStack, GeothermalGeneratorBlockEntity.FUEL_SLOT, GeothermalGeneratorBlockEntity.FUEL_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (invSlot < 3 + 27) {
                    if (!this.moveItemStackTo(originalStack, 3 + 27, this.slots.size(), false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(originalStack, 3, 3 + 27, false)) {
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
