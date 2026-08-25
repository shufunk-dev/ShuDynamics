package net.enchantedwood.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.screen.VehicleFabricatorScreenHandler;
import org.jetbrains.annotations.Nullable;

public class VehicleFabricatorBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, SidedInventory, EnergyProvider {
    public static final int INVENTORY_SIZE = 9;
    public static final int VEHICLE_SLOT = 0;
    public static final int SEAT_SLOT = 1;
    public static final int ENGINE_SLOT = 2;
    public static final int CHASSIS_SLOT = 3;
    public static final int SUSPENSION_SLOT = 4;
    public static final int TIRES_SLOT = 5;
    public static final int TRUNK_SLOT = 6;
    public static final int OUTPUT_SLOT = 7;
    public static final int BATTERY_SLOT = 8;

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(10000, 200);

    protected final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energyStorage.getEnergy() & 0xFFFF;
                case 1 -> (energyStorage.getEnergy() >> 16) & 0xFFFF;
                case 2 -> energyStorage.getMaxEnergy() & 0xFFFF;
                case 3 -> (energyStorage.getMaxEnergy() >> 16) & 0xFFFF;
                case 4 -> canFabricate() ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> {
                    int current = energyStorage.getEnergy();
                    int high = current & 0xFFFF0000;
                    energyStorage.setEnergy(high | (value & 0xFFFF));
                }
                case 1 -> {
                    int current = energyStorage.getEnergy();
                    int low = current & 0xFFFF;
                    energyStorage.setEnergy(low | ((value & 0xFFFF) << 16));
                }
            }
        }

        @Override
        public int size() {
            return 5;
        }
    };

    public VehicleFabricatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VEHICLE_FABRICATOR_BLOCK_ENTITY, pos, state);
    }

    public DefaultedList<ItemStack> getInventory() {
        return inventory;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.enchantedwood.vehicle_fabricator");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new VehicleFabricatorScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    public @Nullable EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.inventory.clear();
        Inventories.readData(view, this.inventory);
        this.energyStorage.readData(view);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, this.inventory);
        this.energyStorage.writeData(view);
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, VehicleFabricatorBlockEntity entity) {
        // 1. Charge from battery slot if present
        ItemStack batteryStack = entity.inventory.get(BATTERY_SLOT);
        if (!batteryStack.isEmpty() && batteryStack.getItem() instanceof EnergyProvider provider) {
            EnergyStorage batteryStorage = provider.getEnergyStorage(null);
            if (batteryStorage != null && batteryStorage.getEnergy() > 0 && entity.energyStorage.getEnergy() < entity.energyStorage.getMaxEnergy()) {
                int needed = entity.energyStorage.getMaxEnergy() - entity.energyStorage.getEnergy();
                int extracted = batteryStorage.extractEnergy(Math.min(needed, 100), false);
                entity.energyStorage.insertEnergy(extracted, false);
                entity.markDirty();
            }
        }

        // 2. Auto-unpack ATV when placed into VEHICLE_SLOT if module slots 1-6 are empty
        ItemStack atvStack = entity.inventory.get(VEHICLE_SLOT);
        if (!atvStack.isEmpty() && atvStack.isOf(ModItems.ATV_ITEM)) {
            boolean modulesEmpty = entity.inventory.get(SEAT_SLOT).isEmpty() &&
                                  entity.inventory.get(ENGINE_SLOT).isEmpty() &&
                                  entity.inventory.get(CHASSIS_SLOT).isEmpty() &&
                                  entity.inventory.get(SUSPENSION_SLOT).isEmpty() &&
                                  entity.inventory.get(TIRES_SLOT).isEmpty() &&
                                  entity.inventory.get(TRUNK_SLOT).isEmpty();
            if (modulesEmpty) {
                NbtComponent comp = atvStack.get(DataComponentTypes.CUSTOM_DATA);
                if (comp != null) {
                    NbtCompound tag = comp.copyNbt();
                    entity.unpackModuleFromTag(tag, "Slot_0", ENGINE_SLOT, 1);
                    entity.unpackModuleFromTag(tag, "Slot_1", TIRES_SLOT, 4);
                    entity.unpackModuleFromTag(tag, "Slot_2", SUSPENSION_SLOT, 1);
                    entity.unpackModuleFromTag(tag, "Slot_3", CHASSIS_SLOT, 1);
                    entity.unpackModuleFromTag(tag, "Slot_4", TRUNK_SLOT, 1);
                    entity.inventory.set(SEAT_SLOT, new ItemStack(ModItems.ATV_SEAT));
                    entity.markDirty();
                }
            }
        }
    }

    private void unpackModuleFromTag(NbtCompound tag, String key, int slot, int count) {
        if (tag.contains(key)) {
            String itemId = tag.getString(key).orElse("");
            if (!itemId.isEmpty()) {
                Identifier id = Identifier.of(itemId);
                if (Registries.ITEM.containsId(id)) {
                    int c = tag.getInt("Count_" + key.replace("Slot_", "")).orElse(count);
                    inventory.set(slot, new ItemStack(Registries.ITEM.get(id), Math.max(1, c)));
                }
            }
        }
    }

    public boolean canFabricate() {
        boolean hasSeat = inventory.get(SEAT_SLOT).isOf(ModItems.ATV_SEAT);
        boolean hasEngine = isEngine(inventory.get(ENGINE_SLOT));
        boolean hasChassis = isChassis(inventory.get(CHASSIS_SLOT));
        boolean hasSuspension = isSuspension(inventory.get(SUSPENSION_SLOT));
        boolean hasTires = isTires(inventory.get(TIRES_SLOT));
        boolean outputEmpty = inventory.get(OUTPUT_SLOT).isEmpty();

        return hasSeat && hasEngine && hasChassis && hasSuspension && hasTires && outputEmpty;
    }

    public boolean fabricateOrUpgrade() {
        if (!canFabricate()) return false;

        // Energy requirement: 100 FE
        if (energyStorage.getEnergy() >= 100) {
            energyStorage.extractEnergy(100, false);
        }

        ItemStack atvResult = new ItemStack(ModItems.ATV_ITEM);
        NbtCompound tag = new NbtCompound();

        // Preserve fuel / cargo if vehicle in slot 0 was being modified
        ItemStack originalAtv = inventory.get(VEHICLE_SLOT);
        if (!originalAtv.isEmpty() && originalAtv.isOf(ModItems.ATV_ITEM)) {
            NbtComponent comp = originalAtv.get(DataComponentTypes.CUSTOM_DATA);
            if (comp != null) {
                NbtCompound prev = comp.copyNbt();
                if (prev.contains("FuelLevel")) tag.putInt("FuelLevel", prev.getInt("FuelLevel").orElse(0));
                if (prev.contains("MaxFuel")) tag.putInt("MaxFuel", prev.getInt("MaxFuel").orElse(1000));
            }
        }

        // Write chosen module components
        ItemStack engine = inventory.get(ENGINE_SLOT);
        ItemStack tires = inventory.get(TIRES_SLOT);
        ItemStack suspension = inventory.get(SUSPENSION_SLOT);
        ItemStack chassis = inventory.get(CHASSIS_SLOT);
        ItemStack trunk = inventory.get(TRUNK_SLOT);

        tag.putString("Slot_0", Registries.ITEM.getId(engine.getItem()).toString());
        tag.putInt("Count_0", 1);

        tag.putString("Slot_1", Registries.ITEM.getId(tires.getItem()).toString());
        tag.putInt("Count_1", 1);

        tag.putString("Slot_2", Registries.ITEM.getId(suspension.getItem()).toString());
        tag.putInt("Count_2", 1);

        tag.putString("Slot_3", Registries.ITEM.getId(chassis.getItem()).toString());
        tag.putInt("Count_3", 1);

        if (!trunk.isEmpty() && isTrunk(trunk)) {
            tag.putString("Slot_4", Registries.ITEM.getId(trunk.getItem()).toString());
            tag.putInt("Count_4", 1);
        }

        atvResult.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));

        // Consume ingredients
        inventory.get(SEAT_SLOT).decrement(1);
        inventory.get(ENGINE_SLOT).decrement(1);
        inventory.get(CHASSIS_SLOT).decrement(1);
        inventory.get(SUSPENSION_SLOT).decrement(1);
        inventory.get(TIRES_SLOT).decrement(Math.min(4, inventory.get(TIRES_SLOT).getCount()));
        if (!trunk.isEmpty()) {
            inventory.get(TRUNK_SLOT).decrement(1);
        }
        if (!originalAtv.isEmpty()) {
            originalAtv.decrement(1);
        }

        inventory.set(OUTPUT_SLOT, atvResult);
        markDirty();
        return true;
    }

    public static boolean isEngine(ItemStack stack) {
        return stack.isOf(ModItems.COPPER_ATV_ENGINE) || stack.isOf(ModItems.ALUMINUM_ATV_ENGINE) ||
               stack.isOf(ModItems.STEEL_ATV_ENGINE) || stack.isOf(ModItems.TITANIUM_ATV_ENGINE);
    }

    public static boolean isChassis(ItemStack stack) {
        return stack.isOf(ModItems.ALUMINUM_ATV_CHASSIS) || stack.isOf(ModItems.STEEL_ATV_CHASSIS) ||
               stack.isOf(ModItems.TITANIUM_ATV_CHASSIS);
    }

    public static boolean isTires(ItemStack stack) {
        return stack.isOf(ModItems.RUBBER_TIRE) || stack.isOf(ModItems.STEEL_RIM_TIRE) ||
               stack.isOf(ModItems.TITANIUM_STUDDED_TIRE);
    }

    public static boolean isSuspension(ItemStack stack) {
        return stack.isOf(ModItems.STEEL_SUSPENSION) || stack.isOf(ModItems.TITANIUM_SUSPENSION);
    }

    public static boolean isTrunk(ItemStack stack) {
        return stack.isOf(ModItems.SMALL_CARGO_TRUNK) || stack.isOf(ModItems.MEDIUM_CARGO_TRUNK) ||
               stack.isOf(ModItems.LARGE_CARGO_TRUNK);
    }

    // Inventory Implementation
    @Override
    public int[] getAvailableSlots(Direction side) {
        return new int[]{OUTPUT_SLOT};
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot == OUTPUT_SLOT;
    }

    @Override
    public int size() {
        return inventory.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : inventory) if (!s.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return inventory.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack res = Inventories.splitStack(inventory, slot, amount);
        markDirty();
        return res;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack res = Inventories.removeStack(inventory, slot);
        markDirty();
        return res;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        inventory.set(slot, stack);
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
        inventory.clear();
    }
}
