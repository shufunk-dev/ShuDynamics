package net.enchantedwood.screen;

import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.BatteryItem;
import net.enchantedwood.item.custom.ModularPowerArmorItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.enchantedwood.block.entity.PoweredAnvilBlockEntity;

public class ModularSuitScreenHandler extends AbstractContainerMenu {
    private final Inventory playerInventory;
    private final Container suitInventory;
    private final ContainerData propertyDelegate;
    private final BlockPos anvilPos;

    private int activeTab = 0; // 0=Head, 1=Chest, 2=Legs, 3=Boots
    private boolean isUpdating = false;

    public ModularSuitScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(4), new SimpleContainerData(10), BlockPos.ZERO);
    }

    public ModularSuitScreenHandler(int syncId, Inventory playerInventory, BlockPos anvilPos) {
        this(syncId, playerInventory, new SimpleContainer(4), new SimpleContainerData(10), anvilPos);
    }

    public ModularSuitScreenHandler(int syncId, Inventory playerInventory, Container suitInventory, ContainerData propertyDelegate) {
        this(syncId, playerInventory, suitInventory, propertyDelegate, BlockPos.ZERO);
    }

    public ModularSuitScreenHandler(int syncId, Inventory playerInventory, Container suitInventory, ContainerData propertyDelegate, BlockPos anvilPos) {
        super(ModScreenHandlers.MODULAR_SUIT_SCREEN_HANDLER, syncId);
        this.playerInventory = playerInventory;
        this.suitInventory = suitInventory;
        this.propertyDelegate = propertyDelegate;
        this.addDataSlots(propertyDelegate);

        BlockPos resolvedPos = anvilPos != null ? anvilPos : BlockPos.ZERO;
        if (resolvedPos.equals(BlockPos.ZERO) && playerInventory.player != null && playerInventory.player.level() != null) {
            BlockPos pPos = playerInventory.player.blockPosition();
            for (BlockPos testPos : BlockPos.withinManhattan(pPos, 4)) {
                if (playerInventory.player.level().getBlockEntity(testPos) instanceof PoweredAnvilBlockEntity) {
                    resolvedPos = testPos.immutable();
                    break;
                }
            }
        }
        this.anvilPos = resolvedPos;

        // Slot 0: Battery Slot
        this.addSlot(new Slot(suitInventory, 0, 44, 45) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof BatteryItem;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                syncToActivePiece();
            }
        });

        // Slot 1: Logic Chip Slot
        this.addSlot(new Slot(suitInventory, 1, 72, 45) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.BASIC_COMPUTER_CHIP)
                        || stack.is(ModItems.ADVANCED_COMPUTER_CHIP)
                        || stack.is(ModItems.QUANTUM_COMPUTER_CHIP);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                syncToActivePiece();
            }
        });

        // Slot 2: Module Slot A
        this.addSlot(new Slot(suitInventory, 2, 104, 45) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isModuleAllowed(getActiveTab(), stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                syncToActivePiece();
            }
        });

        // Slot 3: Module Slot B
        this.addSlot(new Slot(suitInventory, 3, 132, 45) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isModuleAllowed(getActiveTab(), stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                syncToActivePiece();
            }
        });

        // Player Inventory (3 rows of 9)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player Hotbar (1 row of 9)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        // Initialize active tab from equipped armor
        loadTab(0);
        updateProperties();
    }

    private EquipmentSlot getSlotForTab(int tab) {
        return switch (tab) {
            case 0 -> EquipmentSlot.HEAD;
            case 1 -> EquipmentSlot.CHEST;
            case 2 -> EquipmentSlot.LEGS;
            case 3 -> EquipmentSlot.FEET;
            default -> EquipmentSlot.HEAD;
        };
    }

    public ItemStack getEquippedPiece(int tab) {
        return this.playerInventory.player.getItemBySlot(getSlotForTab(tab));
    }

    public void loadTab(int newTab) {
        this.activeTab = Math.max(0, Math.min(3, newTab));
        this.isUpdating = true;

        ItemStack piece = getEquippedPiece(this.activeTab);
        if (!piece.isEmpty() && piece.getItem() instanceof ModularPowerArmorItem) {
            // Load Battery
            String batteryId = ModularPowerArmorItem.getInstalledBatteryId(piece);
            if (!batteryId.isEmpty()) {
                Item item = BuiltInRegistries.ITEM.getValue(Identifier.tryParse(batteryId));
                ItemStack bat = new ItemStack(item, 1);
                BatteryItem.setStoredEnergy(bat, ModularPowerArmorItem.getStoredEnergy(piece));
                this.suitInventory.setItem(0, bat);
            } else {
                this.suitInventory.setItem(0, ItemStack.EMPTY);
            }

            // Load Chip
            String chipId = ModularPowerArmorItem.getInstalledChipId(piece);
            if (!chipId.isEmpty()) {
                Item item = BuiltInRegistries.ITEM.getValue(Identifier.tryParse(chipId));
                this.suitInventory.setItem(1, new ItemStack(item, 1));
            } else {
                this.suitInventory.setItem(1, ItemStack.EMPTY);
            }

            // Load Module 0
            String mod0 = ModularPowerArmorItem.getInstalledModuleId(piece, 0);
            if (!mod0.isEmpty()) {
                Item item = BuiltInRegistries.ITEM.getValue(Identifier.tryParse(mod0));
                this.suitInventory.setItem(2, new ItemStack(item, 1));
            } else {
                this.suitInventory.setItem(2, ItemStack.EMPTY);
            }

            // Load Module 1
            String mod1 = ModularPowerArmorItem.getInstalledModuleId(piece, 1);
            if (!mod1.isEmpty()) {
                Item item = BuiltInRegistries.ITEM.getValue(Identifier.tryParse(mod1));
                this.suitInventory.setItem(3, new ItemStack(item, 1));
            } else {
                this.suitInventory.setItem(3, ItemStack.EMPTY);
            }
        } else {
            for (int i = 0; i < 4; i++) {
                this.suitInventory.setItem(i, ItemStack.EMPTY);
            }
        }

        this.isUpdating = false;
        updateProperties();
    }

    public void syncToActivePiece() {
        if (this.isUpdating) return;

        ItemStack piece = getEquippedPiece(this.activeTab);
        if (!piece.isEmpty() && piece.getItem() instanceof ModularPowerArmorItem) {
            // Sync Battery
            ItemStack bat = this.suitInventory.getItem(0);
            if (!bat.isEmpty() && bat.getItem() instanceof BatteryItem bi) {
                ModularPowerArmorItem.setInstalledBatteryId(piece, BuiltInRegistries.ITEM.getKey(bat.getItem()).toString());
                ModularPowerArmorItem.setMaxEnergy(piece, BatteryItem.getStoredEnergy(bat) > 0 || bi.getEnergyStorage(bat) != null ? bi.getEnergyStorage(bat).getMaxEnergy() : 100_000);
                ModularPowerArmorItem.setStoredEnergy(piece, BatteryItem.getStoredEnergy(bat));
            } else {
                ModularPowerArmorItem.setInstalledBatteryId(piece, "");
                ModularPowerArmorItem.setMaxEnergy(piece, 0);
                ModularPowerArmorItem.setStoredEnergy(piece, 0);
            }

            // Sync Chip
            ItemStack chip = this.suitInventory.getItem(1);
            if (!chip.isEmpty()) {
                ModularPowerArmorItem.setInstalledChipId(piece, BuiltInRegistries.ITEM.getKey(chip.getItem()).toString());
            } else {
                ModularPowerArmorItem.setInstalledChipId(piece, "");
            }

            // Sync Module 0
            ItemStack mod0 = this.suitInventory.getItem(2);
            if (!mod0.isEmpty()) {
                ModularPowerArmorItem.setInstalledModuleId(piece, 0, BuiltInRegistries.ITEM.getKey(mod0.getItem()).toString());
            } else {
                ModularPowerArmorItem.setInstalledModuleId(piece, 0, "");
            }

            // Sync Module 1
            ItemStack mod1 = this.suitInventory.getItem(3);
            if (!mod1.isEmpty()) {
                ModularPowerArmorItem.setInstalledModuleId(piece, 1, BuiltInRegistries.ITEM.getKey(mod1.getItem()).toString());
            } else {
                ModularPowerArmorItem.setInstalledModuleId(piece, 1, "");
            }
        }

        updateProperties();
    }

    public void updateProperties() {
        ItemStack piece = getEquippedPiece(this.activeTab);
        int energy = (!piece.isEmpty() && piece.getItem() instanceof ModularPowerArmorItem) ? ModularPowerArmorItem.getStoredEnergy(piece) : 0;
        int max = (!piece.isEmpty() && piece.getItem() instanceof ModularPowerArmorItem) ? ModularPowerArmorItem.getMaxEnergy(piece) : 0;

        this.propertyDelegate.set(0, this.activeTab);
        this.propertyDelegate.set(1, energy & 0xFFFF);
        this.propertyDelegate.set(2, (energy >> 16) & 0xFFFF);
        this.propertyDelegate.set(3, max & 0xFFFF);
        this.propertyDelegate.set(4, (max >> 16) & 0xFFFF);

        for (int t = 0; t < 4; t++) {
            ItemStack p = getEquippedPiece(t);
            boolean hasIt = !p.isEmpty() && p.getItem() instanceof ModularPowerArmorItem;
            this.propertyDelegate.set(5 + t, hasIt ? 1 : 0);
        }

        boolean hasAnvil = this.anvilPos != null && !this.anvilPos.equals(BlockPos.ZERO)
                && this.playerInventory.player != null
                && this.playerInventory.player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(this.anvilPos)) <= 36.0
                && this.playerInventory.player.level().getBlockEntity(this.anvilPos) instanceof PoweredAnvilBlockEntity;
        this.propertyDelegate.set(9, hasAnvil ? 1 : 0);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= 0 && id <= 3) {
            syncToActivePiece();
            loadTab(id);
            return true;
        } else if (id == 4) {
            syncToActivePiece();
            if (this.anvilPos != null && !this.anvilPos.equals(BlockPos.ZERO)
                    && player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(this.anvilPos)) <= 36.0
                    && player.level().getBlockEntity(this.anvilPos) instanceof PoweredAnvilBlockEntity anvil) {
                player.openMenu(anvil);
                return true;
            }
        }
        return false;
    }

    public boolean hasAnvilLinked() {
        return this.propertyDelegate.getCount() > 9 && this.propertyDelegate.get(9) == 1;
    }

    public int getActiveTab() {
        return this.propertyDelegate.get(0);
    }

    public int getCurrentPieceEnergy() {
        return (this.propertyDelegate.get(2) << 16) | (this.propertyDelegate.get(1) & 0xFFFF);
    }

    public int getCurrentPieceMaxEnergy() {
        return (this.propertyDelegate.get(4) << 16) | (this.propertyDelegate.get(3) & 0xFFFF);
    }

    public boolean hasPieceEquipped(int tab) {
        return this.propertyDelegate.get(5 + tab) == 1;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();

            if (invSlot < 4) {
                // Moving from suit slots to player inventory
                if (!this.moveItemStackTo(originalStack, 4, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player inventory to suit slots
                if (originalStack.getItem() instanceof BatteryItem) {
                    if (!this.moveItemStackTo(originalStack, 0, 1, false)) return ItemStack.EMPTY;
                } else if (originalStack.is(ModItems.BASIC_COMPUTER_CHIP) || originalStack.is(ModItems.ADVANCED_COMPUTER_CHIP) || originalStack.is(ModItems.QUANTUM_COMPUTER_CHIP)) {
                    if (!this.moveItemStackTo(originalStack, 1, 2, false)) return ItemStack.EMPTY;
                } else if (isModuleAllowed(getActiveTab(), originalStack)) {
                    if (!this.moveItemStackTo(originalStack, 2, 4, false)) return ItemStack.EMPTY;
                } else {
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

    public static boolean isModuleAllowed(int tab, ItemStack stack) {
        if (stack.getItem() instanceof net.enchantedwood.item.custom.NaniteRepairMatrixItem) {
            return true; // Nanites work across all suit pieces
        }
        if (stack.is(ModItems.NIGHT_VISION_MODULE)) {
            return tab == 0; // Helmet only
        }
        if (stack.is(ModItems.HYDROGEN_THRUSTER_MODULE) || stack.is(ModItems.ION_REPULSOR_MODULE)) {
            return tab == 1; // Chestplate only
        }
        if (stack.is(ModItems.ACID_PROOF_PLATING) || stack.is(ModItems.THERMAL_REFRACTORY_PLATING)) {
            return tab == 1 || tab == 2; // Chestplate or Leggings
        }
        if (stack.is(ModItems.SPEED_SERVO_MODULE)) {
            return tab == 2; // Leggings only
        }
        if (stack.is(ModItems.STEP_ASSIST_MODULE) || stack.is(ModItems.HIGH_JUMP_MODULE)) {
            return tab == 3; // Boots only
        }
        return false;
    }

    @Override
    public void removed(Player player) {
        syncToActivePiece();
        super.removed(player);
    }
}
