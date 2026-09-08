package net.enchantedwood.screen;

import net.enchantedwood.block.entity.PoweredAnvilBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class PoweredAnvilScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;
    private final BlockPos blockPos;

    public PoweredAnvilScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(PoweredAnvilBlockEntity.INVENTORY_SIZE), new ArrayPropertyDelegate(4), BlockPos.ORIGIN);
    }

    public PoweredAnvilScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate, BlockPos blockPos) {
        super(ModScreenHandlers.POWERED_ANVIL_SCREEN_HANDLER, syncId);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        this.blockPos = blockPos;
        this.addProperties(propertyDelegate);

        // Slot 0: Damaged Equipment Input
        this.addSlot(new Slot(inventory, PoweredAnvilBlockEntity.INPUT_SLOT, 38, 45));

        // Slot 1: Repair Material (Titanium Ingot, etc.)
        this.addSlot(new Slot(inventory, PoweredAnvilBlockEntity.MATERIAL_SLOT, 76, 45));

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

    public int getEnergy() {
        return (this.propertyDelegate.get(1) << 16) | (this.propertyDelegate.get(0) & 0xFFFF);
    }

    public int getMaxEnergy() {
        int max = (this.propertyDelegate.get(3) << 16) | (this.propertyDelegate.get(2) & 0xFFFF);
        return max > 0 ? max : PoweredAnvilBlockEntity.ENERGY_CAPACITY;
    }

    public ItemStack getInputStack() {
        return this.inventory.getStack(PoweredAnvilBlockEntity.INPUT_SLOT);
    }

    public ItemStack getMaterialStack() {
        return this.inventory.getStack(PoweredAnvilBlockEntity.MATERIAL_SLOT);
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id == 0) {
            // Repair Action
            if (player.getEntityWorld().getBlockEntity(this.blockPos) instanceof PoweredAnvilBlockEntity anvil) {
                anvil.executeRepair(player);
                return true;
            }
        } else if (id == 1) {
            // Switch to Suit Bay Tab
            player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, inv, p) -> new ModularSuitScreenHandler(syncId, inv),
                    Text.literal("Modular Suit Access Panel")
            ));
            return true;
        }
        return false;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            if (invSlot < 2) {
                if (!this.insertItem(originalStack, 2, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (originalStack.isDamaged()) {
                    if (!this.insertItem(originalStack, 0, 1, false)) return ItemStack.EMPTY;
                } else {
                    if (!this.insertItem(originalStack, 1, 2, false)) return ItemStack.EMPTY;
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
}
