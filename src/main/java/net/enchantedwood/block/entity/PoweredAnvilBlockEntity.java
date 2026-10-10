package net.enchantedwood.block.entity;

import net.enchantedwood.block.custom.PoweredAnvilBlock;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.ModularPowerArmorItem;
import net.enchantedwood.screen.PoweredAnvilScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class PoweredAnvilBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, EnergyProvider {
    public static final int INVENTORY_SIZE = 3;
    public static final int INPUT_SLOT = 0;
    public static final int MATERIAL_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;

    public static final int ENERGY_CAPACITY = 50_000;
    public static final int REPAIR_ENERGY_COST = 2_500;

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(ENERGY_CAPACITY, 1_000, 1_000, 0);

    protected final ContainerData propertyDelegate = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energyStorage.getEnergy() & 0xFFFF;
                case 1 -> (energyStorage.getEnergy() >> 16) & 0xFFFF;
                case 2 -> energyStorage.getMaxEnergy() & 0xFFFF;
                case 3 -> (energyStorage.getMaxEnergy() >> 16) & 0xFFFF;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Read-only from client
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public PoweredAnvilBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POWERED_ANVIL, pos, state);
    }

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, PoweredAnvilBlockEntity entity) {
        boolean isLit = entity.energyStorage.getEnergy() >= REPAIR_ENERGY_COST;
        if (state.getValue(PoweredAnvilBlock.LIT) != isLit) {
            world.setBlock(pos, state.setValue(PoweredAnvilBlock.LIT, isLit), 3);
        }
    }

    public boolean canPerformRepair() {
        ItemStack input = this.inventory.get(INPUT_SLOT);
        ItemStack material = this.inventory.get(MATERIAL_SLOT);

        if (input.isEmpty() || !input.isDamaged() || material.isEmpty()) {
            return false;
        }

        if (this.energyStorage.getEnergy() < REPAIR_ENERGY_COST) {
            return false;
        }

        return isValidRepairIngredient(input, material);
    }

    public static boolean isValidRepairIngredient(ItemStack input, ItemStack material) {
        if (input.getItem() instanceof ModularPowerArmorItem) {
            // Modular Power Armor strictly requires Titanium Ingots in the Powered Anvil!
            return material.is(ModItems.TITANIUM_INGOT);
        }

        // Combining two of the same item
        if (material.is(input.getItem()) && material.isDamaged()) {
            return true;
        }

        // Vanilla repairable checks
        net.minecraft.world.item.enchantment.Repairable repairable = input.get(net.minecraft.core.component.DataComponents.REPAIRABLE);
        return repairable != null && repairable.isValidRepairItem(material);
    }

    public ItemStack computeRepairedOutput() {
        if (!canPerformRepair()) return ItemStack.EMPTY;

        ItemStack input = this.inventory.get(INPUT_SLOT);
        ItemStack material = this.inventory.get(MATERIAL_SLOT);
        ItemStack output = input.copy();

        if (material.is(input.getItem())) {
            // Combining items
            int combinedDamage = input.getDamageValue() - (material.getMaxDamage() - material.getDamageValue() + (int) (material.getMaxDamage() * 0.12));
            output.setDamageValue(Math.max(0, combinedDamage));
        } else {
            // Restores up to 25% of maximum durability per ingot
            int repairAmount = Math.max(1, input.getMaxDamage() / 4);
            output.setDamageValue(Math.max(0, input.getDamageValue() - repairAmount));
        }

        return output;
    }

    public void executeRepair(Player player) {
        if (!canPerformRepair()) return;

        ItemStack output = computeRepairedOutput();
        if (output.isEmpty()) return;

        // Deduct energy
        this.energyStorage.extractEnergy(REPAIR_ENERGY_COST, false);

        // Consume 1 repair material
        this.inventory.get(MATERIAL_SLOT).shrink(1);

        // Replace input with repaired output
        this.inventory.set(INPUT_SLOT, output);
        setChanged();

        if (this.level != null) {
            this.level.playSound(null, this.worldPosition, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8f, 1.2f);
        }
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        ContainerHelper.loadAllItems(view, this.inventory);
        this.energyStorage.readData(view);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        ContainerHelper.saveAllItems(view, this.inventory);
        this.energyStorage.writeData(view);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.enchantedwood.powered_anvil");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new PoweredAnvilScreenHandler(syncId, playerInventory, this, this.propertyDelegate, this.worldPosition);
    }

    @Nullable
    @Override
    public EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }

    public SimpleEnergyStorage getEnergyStorage() {
        return this.energyStorage;
    }

    @Override
    public int getContainerSize() {
        return INVENTORY_SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.inventory) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.inventory.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ContainerHelper.removeItem(this.inventory, slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.inventory, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.inventory.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.inventory.clear();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[]{INPUT_SLOT, MATERIAL_SLOT, OUTPUT_SLOT};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == INPUT_SLOT) return stack.isDamaged();
        if (slot == MATERIAL_SLOT) return true;
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot == OUTPUT_SLOT || (slot == INPUT_SLOT && !stack.isDamaged());
    }
}
