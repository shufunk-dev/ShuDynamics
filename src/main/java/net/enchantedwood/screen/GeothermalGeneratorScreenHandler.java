package net.enchantedwood.screen;

import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.block.entity.GeothermalGeneratorBlockEntity;
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

public class GeothermalGeneratorScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    public GeothermalGeneratorScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(GeothermalGeneratorBlockEntity.INVENTORY_SIZE), new ArrayPropertyDelegate(8));
    }

    public GeothermalGeneratorScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.GEOTHERMAL_GENERATOR_SCREEN_HANDLER, syncId);
        checkSize(inventory, GeothermalGeneratorBlockEntity.INVENTORY_SIZE);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        this.addProperties(propertyDelegate);
        inventory.onOpen(playerInventory.player);

        // Slot 0: Fuel/Bucket Input (x=44, y=25)
        this.addSlot(new Slot(inventory, GeothermalGeneratorBlockEntity.FUEL_SLOT, 44, 25) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(Items.LAVA_BUCKET) || stack.isOf(Items.MAGMA_BLOCK) || stack.isOf(ModItems.FIRE_CRYSTAL);
            }
        });

        // Slot 1: Empty Bucket Output (x=44, y=53)
        this.addSlot(new Slot(inventory, GeothermalGeneratorBlockEntity.BUCKET_OUTPUT_SLOT, 44, 53) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }
        });

        // Slot 2: Gear Upgrade (x=152, y=8)
        this.addSlot(new Slot(inventory, GeothermalGeneratorBlockEntity.GEAR_SLOT, 152, 8) {
            @Override
            public boolean canInsert(ItemStack stack) {
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
            case DIAMOND -> 2.2f;
            case NETHERITE -> 3.0f;
            default -> 1.0f;
        };
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();
            if (invSlot < GeothermalGeneratorBlockEntity.INVENTORY_SIZE) {
                if (!this.insertItem(originalStack, GeothermalGeneratorBlockEntity.INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (originalStack.getItem() instanceof GearItem) {
                    if (!this.insertItem(originalStack, GeothermalGeneratorBlockEntity.GEAR_SLOT, GeothermalGeneratorBlockEntity.GEAR_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (originalStack.isOf(Items.LAVA_BUCKET) || originalStack.isOf(Items.MAGMA_BLOCK) || originalStack.isOf(ModItems.FIRE_CRYSTAL)) {
                    if (!this.insertItem(originalStack, GeothermalGeneratorBlockEntity.FUEL_SLOT, GeothermalGeneratorBlockEntity.FUEL_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (invSlot < 3 + 27) {
                    if (!this.insertItem(originalStack, 3 + 27, this.slots.size(), false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.insertItem(originalStack, 3, 3 + 27, false)) {
                    return ItemStack.EMPTY;
                }
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
