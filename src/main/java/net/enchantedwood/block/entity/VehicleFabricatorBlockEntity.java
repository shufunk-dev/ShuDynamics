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
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
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
import net.enchantedwood.entity.custom.AtvEntity;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.screen.VehicleFabricatorScreenHandler;
import org.jetbrains.annotations.Nullable;

public class VehicleFabricatorBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, SidedInventory, EnergyProvider {
    public static final int INVENTORY_SIZE = 10;
    public static final int VEHICLE_SLOT = 0;
    public static final int SEAT_SLOT = 1;
    public static final int ENGINE_SLOT = 2;
    public static final int CHASSIS_SLOT = 3;
    public static final int SUSPENSION_SLOT = 4;
    public static final int TIRES_SLOT = 5;
    public static final int HEADLIGHT_SLOT = 6;
    public static final int TRUNK_SLOT = 7;
    public static final int OUTPUT_SLOT = 8;
    public static final int BATTERY_SLOT = 9;

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(10000, 200);

    private int progress = 0;
    private int maxProgress = 200;
    private boolean isFabricating = false;

    protected final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energyStorage.getEnergy() & 0xFFFF;
                case 1 -> (energyStorage.getEnergy() >> 16) & 0xFFFF;
                case 2 -> energyStorage.getMaxEnergy() & 0xFFFF;
                case 3 -> (energyStorage.getMaxEnergy() >> 16) & 0xFFFF;
                case 4 -> canFabricate() ? 1 : 0;
                case 5 -> progress;
                case 6 -> maxProgress;
                case 7 -> isFabricating ? 1 : 0;
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
                case 5 -> progress = value;
                case 6 -> maxProgress = value;
                case 7 -> isFabricating = (value != 0);
            }
        }

        @Override
        public int size() {
            return 8;
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
        return Text.translatable("container.enchantedwood.vehicle_fabricator");
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
        this.progress = view.getInt("Progress", 0);
        this.maxProgress = view.getInt("MaxProgress", 200);
        this.isFabricating = view.getBoolean("IsFabricating", false);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, this.inventory);
        this.energyStorage.writeData(view);
        view.putInt("Progress", this.progress);
        view.putInt("MaxProgress", this.maxProgress);
        view.putBoolean("IsFabricating", this.isFabricating);
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, VehicleFabricatorBlockEntity entity) {
        // 1. Charge from battery slot if present
        ItemStack batteryStack = entity.inventory.get(BATTERY_SLOT);
        if (!batteryStack.isEmpty()) {
            EnergyStorage batteryStorage = null;
            if (batteryStack.getItem() instanceof net.enchantedwood.energy.ItemEnergyProvider itemProvider) {
                batteryStorage = itemProvider.getEnergyStorage(batteryStack);
            } else if (batteryStack.getItem() instanceof EnergyProvider provider) {
                batteryStorage = provider.getEnergyStorage(null);
            }

            if (batteryStorage != null && batteryStorage.getEnergy() > 0 && entity.energyStorage.getEnergy() < entity.energyStorage.getMaxEnergy()) {
                int needed = entity.energyStorage.getMaxEnergy() - entity.energyStorage.getEnergy();
                int extracted = batteryStorage.extractEnergy(Math.min(needed, 500), false);
                entity.energyStorage.insertEnergy(extracted, false);
                entity.markDirty();
            }
        }

        // 2. Auto-unpack ATV when placed into VEHICLE_SLOT if module slots 1-7 are empty
        ItemStack atvStack = entity.inventory.get(VEHICLE_SLOT);
        if (!entity.isFabricating && !atvStack.isEmpty() && atvStack.isOf(ModItems.ATV_ITEM)) {
            boolean modulesEmpty = entity.inventory.get(SEAT_SLOT).isEmpty() &&
                                  entity.inventory.get(ENGINE_SLOT).isEmpty() &&
                                  entity.inventory.get(CHASSIS_SLOT).isEmpty() &&
                                  entity.inventory.get(SUSPENSION_SLOT).isEmpty() &&
                                  entity.inventory.get(TIRES_SLOT).isEmpty() &&
                                  entity.inventory.get(HEADLIGHT_SLOT).isEmpty() &&
                                  entity.inventory.get(TRUNK_SLOT).isEmpty();
            if (modulesEmpty) {
                NbtComponent comp = atvStack.get(DataComponentTypes.CUSTOM_DATA);
                if (comp != null) {
                    NbtCompound tag = comp.copyNbt();
                    entity.unpackModuleFromTag(tag, "Slot_0", ENGINE_SLOT, 1);
                    entity.unpackModuleFromTag(tag, "Slot_1", TIRES_SLOT, 4);
                    entity.unpackModuleFromTag(tag, "Slot_2", SUSPENSION_SLOT, 1);
                    entity.unpackModuleFromTag(tag, "Slot_3", CHASSIS_SLOT, 1);

                    // Headlights (Slot_4 in modern saves) vs Trunk (Slot_5 in modern saves)
                    if (tag.contains("Slot_4")) {
                        String s4 = tag.getString("Slot_4").orElse("");
                        if (s4.contains("trunk")) {
                            // Legacy save: Slot_4 was trunk
                            entity.unpackModuleFromTag(tag, "Slot_4", TRUNK_SLOT, 1);
                            entity.inventory.set(HEADLIGHT_SLOT, new ItemStack(ModItems.HALOGEN_HEADLIGHTS));
                        } else {
                            // Modern save: Slot_4 is headlights
                            entity.unpackModuleFromTag(tag, "Slot_4", HEADLIGHT_SLOT, 1);
                            entity.unpackModuleFromTag(tag, "Slot_5", TRUNK_SLOT, 1);
                        }
                    } else if (tag.contains("Headlights")) {
                        String lightId = tag.getString("Headlights").orElse("");
                        if (!lightId.isEmpty() && Registries.ITEM.containsId(Identifier.of(lightId))) {
                            entity.inventory.set(HEADLIGHT_SLOT, new ItemStack(Registries.ITEM.get(Identifier.of(lightId))));
                        } else {
                            entity.inventory.set(HEADLIGHT_SLOT, new ItemStack(ModItems.HALOGEN_HEADLIGHTS));
                        }
                        entity.unpackModuleFromTag(tag, "Slot_5", TRUNK_SLOT, 1);
                    } else {
                        entity.inventory.set(HEADLIGHT_SLOT, new ItemStack(ModItems.HALOGEN_HEADLIGHTS));
                        entity.unpackModuleFromTag(tag, "Slot_5", TRUNK_SLOT, 1);
                    }

                    // Fallback for headlights if empty (required core part)
                    if (entity.inventory.get(HEADLIGHT_SLOT).isEmpty()) {
                        entity.inventory.set(HEADLIGHT_SLOT, new ItemStack(ModItems.HALOGEN_HEADLIGHTS));
                    }

                    entity.inventory.set(SEAT_SLOT, new ItemStack(ModItems.ATV_SEAT));
                    entity.markDirty();
                } else {
                    // Default base parts if ATV was crafted from crafting table without custom data
                    entity.inventory.set(SEAT_SLOT, new ItemStack(ModItems.ATV_SEAT));
                    entity.inventory.set(ENGINE_SLOT, new ItemStack(ModItems.COPPER_ATV_ENGINE));
                    entity.inventory.set(CHASSIS_SLOT, new ItemStack(ModItems.STEEL_ATV_CHASSIS));
                    entity.inventory.set(SUSPENSION_SLOT, new ItemStack(ModItems.STEEL_SUSPENSION));
                    entity.inventory.set(TIRES_SLOT, new ItemStack(ModItems.RUBBER_TIRE, 4));
                    entity.inventory.set(HEADLIGHT_SLOT, new ItemStack(ModItems.HALOGEN_HEADLIGHTS));
                    entity.markDirty();
                }
            }
        }

        // 3. Active Assembly / Tuning Process
        if (entity.isFabricating) {
            if (!entity.canFabricate()) {
                // Cancel if ingredients were removed or output got blocked
                entity.isFabricating = false;
                entity.progress = 0;
                entity.markDirty();
                return;
            }

            // Power consumption: 5 FE/t during fabrication
            if (entity.energyStorage.getEnergy() >= 5) {
                entity.energyStorage.extractEnergy(5, false);
                entity.progress++;

                // Fabrication audio & mechanical sounds
                if (entity.progress % 30 == 0) {
                    world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 0.4f, 1.2f);
                } else if (entity.progress % 15 == 0) {
                    world.playSound(null, pos, SoundEvents.BLOCK_GRINDSTONE_USE, SoundCategory.BLOCKS, 0.5f, 1.4f);
                }

                // Completion
                if (entity.progress >= entity.maxProgress) {
                    entity.finishFabrication();
                    world.playSound(null, pos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 0.8f, 1.2f);
                    world.playSound(null, pos, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.BLOCKS, 0.6f, 1.5f);
                }
                entity.markDirty();
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
                    inventory.set(slot, new ItemStack(Registries.ITEM.get(id), Math.max(count, c)));
                }
            }
        }
    }

    public int calculateTotalTime() {
        int baseTime = 60; // 3 seconds base alignment

        // Engine Tier Time
        ItemStack engine = inventory.get(ENGINE_SLOT);
        if (engine.isOf(ModItems.COPPER_ATV_ENGINE)) baseTime += 40;        // +2.0s
        else if (engine.isOf(ModItems.ALUMINUM_ATV_ENGINE)) baseTime += 80;  // +4.0s
        else if (engine.isOf(ModItems.STEEL_ATV_ENGINE)) baseTime += 140;   // +7.0s
        else if (engine.isOf(ModItems.TITANIUM_ATV_ENGINE)) baseTime += 220;// +11.0s

        // Chassis Tier Time
        ItemStack chassis = inventory.get(CHASSIS_SLOT);
        if (chassis.isOf(ModItems.ALUMINUM_ATV_CHASSIS)) baseTime += 40;    // +2.0s
        else if (chassis.isOf(ModItems.STEEL_ATV_CHASSIS)) baseTime += 80;  // +4.0s
        else if (chassis.isOf(ModItems.TITANIUM_ATV_CHASSIS)) baseTime += 160; // +8.0s

        // Tires Tier Time
        ItemStack tires = inventory.get(TIRES_SLOT);
        if (tires.isOf(ModItems.RUBBER_TIRE)) baseTime += 20;               // +1.0s
        else if (tires.isOf(ModItems.STEEL_RIM_TIRE)) baseTime += 40;       // +2.0s
        else if (tires.isOf(ModItems.TITANIUM_STUDDED_TIRE)) baseTime += 80;// +4.0s

        // Suspension Tier Time
        ItemStack suspension = inventory.get(SUSPENSION_SLOT);
        if (suspension.isOf(ModItems.ALUMINUM_SUSPENSION)) baseTime += 15;   // +0.75s
        else if (suspension.isOf(ModItems.STEEL_SUSPENSION)) baseTime += 30; // +1.5s
        else if (suspension.isOf(ModItems.TITANIUM_SUSPENSION)) baseTime += 60; // +3.0s

        // Cargo Trunk Tier Time
        ItemStack trunk = inventory.get(TRUNK_SLOT);
        if (trunk.isOf(ModItems.SMALL_CARGO_TRUNK)) baseTime += 20;         // +1.0s
        else if (trunk.isOf(ModItems.MEDIUM_CARGO_TRUNK)) baseTime += 40;   // +2.0s
        else if (trunk.isOf(ModItems.LARGE_CARGO_TRUNK)) baseTime += 80;    // +4.0s

        return baseTime;
    }

    public boolean canFabricate() {
        boolean hasSeat = inventory.get(SEAT_SLOT).isOf(ModItems.ATV_SEAT);
        boolean hasEngine = isEngine(inventory.get(ENGINE_SLOT));
        boolean hasChassis = isChassis(inventory.get(CHASSIS_SLOT));
        boolean hasSuspension = isSuspension(inventory.get(SUSPENSION_SLOT));
        boolean hasTires = isTires(inventory.get(TIRES_SLOT)) && inventory.get(TIRES_SLOT).getCount() >= 4;
        boolean hasHeadlights = isHeadlight(inventory.get(HEADLIGHT_SLOT));
        boolean outputEmpty = inventory.get(OUTPUT_SLOT).isEmpty();

        return hasSeat && hasEngine && hasChassis && hasSuspension && hasTires && hasHeadlights && outputEmpty;
    }

    public boolean startFabrication() {
        if (!canFabricate() || isFabricating) return false;

        this.maxProgress = calculateTotalTime();
        this.progress = 0;
        this.isFabricating = true;
        markDirty();
        return true;
    }

    private void finishFabrication() {
        ItemStack atvResult = new ItemStack(ModItems.ATV_ITEM);
        NbtCompound tag = new NbtCompound();

        // Write chosen module components
        ItemStack engine = inventory.get(ENGINE_SLOT);
        ItemStack tires = inventory.get(TIRES_SLOT);
        ItemStack suspension = inventory.get(SUSPENSION_SLOT);
        ItemStack chassis = inventory.get(CHASSIS_SLOT);
        ItemStack headlights = inventory.get(HEADLIGHT_SLOT);
        ItemStack trunk = inventory.get(TRUNK_SLOT);

        // Preserve fuel / cargo / attachments if vehicle in slot 0 was being modified
        ItemStack originalAtv = inventory.get(VEHICLE_SLOT);
        if (!originalAtv.isEmpty() && originalAtv.isOf(ModItems.ATV_ITEM)) {
            NbtComponent comp = originalAtv.get(DataComponentTypes.CUSTOM_DATA);
            if (comp != null) {
                NbtCompound prev = comp.copyNbt();
                if (prev.contains("FuelLevel")) tag.putInt("FuelLevel", prev.getInt("FuelLevel").orElse(0));
                if (prev.contains("MaxFuel")) tag.putInt("MaxFuel", prev.getInt("MaxFuel").orElse(1000));

                // Preserve fuel slot & tool attachment (slots 6 & 7)
                for (int slotIdx : new int[]{AtvEntity.FUEL_SLOT, AtvEntity.TOOL_SLOT}) {
                    if (prev.contains("Slot_" + slotIdx)) {
                        tag.putString("Slot_" + slotIdx, prev.getString("Slot_" + slotIdx).orElse(""));
                        if (prev.contains("Count_" + slotIdx)) {
                            tag.putInt("Count_" + slotIdx, prev.getInt("Count_" + slotIdx).orElse(1));
                        }
                        if (prev.contains("Damage_" + slotIdx)) {
                            tag.putInt("Damage_" + slotIdx, prev.getInt("Damage_" + slotIdx).orElse(0));
                        }
                    }
                }

                // If trunk is present on new ATV, preserve stored cargo (slots 8..34)
                if (!trunk.isEmpty() && isTrunk(trunk)) {
                    for (int slotIdx = AtvEntity.MODULE_SLOTS_COUNT; slotIdx < AtvEntity.TOTAL_INVENTORY_SIZE; slotIdx++) {
                        if (prev.contains("Slot_" + slotIdx)) {
                            tag.putString("Slot_" + slotIdx, prev.getString("Slot_" + slotIdx).orElse(""));
                            if (prev.contains("Count_" + slotIdx)) {
                                tag.putInt("Count_" + slotIdx, prev.getInt("Count_" + slotIdx).orElse(1));
                            }
                            if (prev.contains("Damage_" + slotIdx)) {
                                tag.putInt("Damage_" + slotIdx, prev.getInt("Damage_" + slotIdx).orElse(0));
                            }
                        }
                    }
                } else if (this.world instanceof ServerWorld sw) {
                    // If trunk removed, safely spill cargo items into world
                    for (int slotIdx = AtvEntity.MODULE_SLOTS_COUNT; slotIdx < AtvEntity.TOTAL_INVENTORY_SIZE; slotIdx++) {
                        if (prev.contains("Slot_" + slotIdx)) {
                            String itemId = prev.getString("Slot_" + slotIdx).orElse("");
                            if (!itemId.isEmpty() && Registries.ITEM.containsId(Identifier.of(itemId))) {
                                int c = prev.getInt("Count_" + slotIdx).orElse(1);
                                ItemStack cargoStack = new ItemStack(Registries.ITEM.get(Identifier.of(itemId)), c);
                                if (prev.contains("Damage_" + slotIdx)) {
                                    cargoStack.setDamage(prev.getInt("Damage_" + slotIdx).orElse(0));
                                }
                                net.minecraft.entity.ItemEntity entityItem = new net.minecraft.entity.ItemEntity(
                                    sw, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, cargoStack);
                                sw.spawnEntity(entityItem);
                            }
                        }
                    }
                }
            }
        }

        tag.putString("Slot_0", Registries.ITEM.getId(engine.getItem()).toString());
        tag.putInt("Count_0", 1);

        tag.putString("Slot_1", Registries.ITEM.getId(tires.getItem()).toString());
        tag.putInt("Count_1", 4);

        tag.putString("Slot_2", Registries.ITEM.getId(suspension.getItem()).toString());
        tag.putInt("Count_2", 1);

        tag.putString("Slot_3", Registries.ITEM.getId(chassis.getItem()).toString());
        tag.putInt("Count_3", 1);

        tag.putString("Slot_4", Registries.ITEM.getId(headlights.getItem()).toString());
        tag.putInt("Count_4", 1);
        tag.putString("Headlights", Registries.ITEM.getId(headlights.getItem()).toString());

        if (!trunk.isEmpty() && isTrunk(trunk)) {
            tag.putString("Slot_5", Registries.ITEM.getId(trunk.getItem()).toString());
            tag.putInt("Count_5", 1);
        }

        atvResult.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));

        // Consume ingredients
        inventory.get(SEAT_SLOT).decrement(1);
        inventory.get(ENGINE_SLOT).decrement(1);
        inventory.get(CHASSIS_SLOT).decrement(1);
        inventory.get(SUSPENSION_SLOT).decrement(1);
        inventory.get(HEADLIGHT_SLOT).decrement(1);
        inventory.get(TIRES_SLOT).decrement(4);
        if (!trunk.isEmpty()) {
            inventory.get(TRUNK_SLOT).decrement(1);
        }
        if (!originalAtv.isEmpty()) {
            originalAtv.decrement(1);
        }

        inventory.set(OUTPUT_SLOT, atvResult);
        this.isFabricating = false;
        this.progress = 0;
        markDirty();
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
        return stack.isOf(ModItems.ALUMINUM_SUSPENSION) || stack.isOf(ModItems.STEEL_SUSPENSION) || stack.isOf(ModItems.TITANIUM_SUSPENSION);
    }

    public static boolean isHeadlight(ItemStack stack) {
        return stack.getItem() instanceof net.enchantedwood.item.custom.HeadlightsItem;
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
