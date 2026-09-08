package net.enchantedwood.block.entity;

import net.enchantedwood.block.custom.PoweredAnvilBlock;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.ModularPowerArmorItem;
import net.enchantedwood.screen.PoweredAnvilScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public class PoweredAnvilBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, SidedInventory, EnergyProvider {
    public static final int INVENTORY_SIZE = 3;
    public static final int INPUT_SLOT = 0;
    public static final int MATERIAL_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;

    public static final int ENERGY_CAPACITY = 50_000;
    public static final int REPAIR_ENERGY_COST = 2_500;

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(ENERGY_CAPACITY, 1_000, 1_000, 0);

    protected final PropertyDelegate propertyDelegate = new PropertyDelegate() {
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
        public int size() {
            return 4;
        }
    };

    public PoweredAnvilBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POWERED_ANVIL, pos, state);
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, PoweredAnvilBlockEntity entity) {
        boolean isLit = entity.energyStorage.getEnergy() >= REPAIR_ENERGY_COST;
        if (state.get(PoweredAnvilBlock.LIT) != isLit) {
            world.setBlockState(pos, state.with(PoweredAnvilBlock.LIT, isLit), 3);
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
            return material.isOf(ModItems.TITANIUM_INGOT);
        }

        // Combining two of the same item
        if (material.isOf(input.getItem()) && material.isDamaged()) {
            return true;
        }

        // Vanilla repairable checks
        net.minecraft.component.type.RepairableComponent repairable = input.get(net.minecraft.component.DataComponentTypes.REPAIRABLE);
        return repairable != null && repairable.matches(material);
    }

    public ItemStack computeRepairedOutput() {
        if (!canPerformRepair()) return ItemStack.EMPTY;

        ItemStack input = this.inventory.get(INPUT_SLOT);
        ItemStack material = this.inventory.get(MATERIAL_SLOT);
        ItemStack output = input.copy();

        if (material.isOf(input.getItem())) {
            // Combining items
            int combinedDamage = input.getDamage() - (material.getMaxDamage() - material.getDamage() + (int) (material.getMaxDamage() * 0.12));
            output.setDamage(Math.max(0, combinedDamage));
        } else {
            // Restores up to 25% of maximum durability per ingot
            int repairAmount = Math.max(1, input.getMaxDamage() / 4);
            output.setDamage(Math.max(0, input.getDamage() - repairAmount));
        }

        return output;
    }

    public void executeRepair(PlayerEntity player) {
        if (!canPerformRepair()) return;

        ItemStack output = computeRepairedOutput();
        if (output.isEmpty()) return;

        // Deduct energy
        this.energyStorage.extractEnergy(REPAIR_ENERGY_COST, false);

        // Consume 1 repair material
        this.inventory.get(MATERIAL_SLOT).decrement(1);

        // Replace input with repaired output
        this.inventory.set(INPUT_SLOT, output);
        markDirty();

        if (this.world != null) {
            this.world.playSound(null, this.pos, SoundEvents.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 0.8f, 1.2f);
        }
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        Inventories.readData(view, this.inventory);
        this.energyStorage.readData(view);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, this.inventory);
        this.energyStorage.writeData(view);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.enchantedwood.powered_anvil");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new PoweredAnvilScreenHandler(syncId, playerInventory, this, this.propertyDelegate, this.pos);
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
    public int size() {
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
    public ItemStack getStack(int slot) {
        return this.inventory.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return Inventories.splitStack(this.inventory, slot, amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(this.inventory, slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        this.inventory.set(slot, stack);
        if (stack.getCount() > getMaxCountPerStack()) {
            stack.setCount(getMaxCountPerStack());
        }
        markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return Inventory.canPlayerUse(this, player);
    }

    @Override
    public void clear() {
        this.inventory.clear();
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return new int[]{INPUT_SLOT, MATERIAL_SLOT, OUTPUT_SLOT};
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == INPUT_SLOT) return stack.isDamaged();
        if (slot == MATERIAL_SLOT) return true;
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot == OUTPUT_SLOT || (slot == INPUT_SLOT && !stack.isDamaged());
    }
}
