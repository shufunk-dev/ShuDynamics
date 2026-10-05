package net.enchantedwood.block.entity;

import net.enchantedwood.block.custom.IceCreamMachineBlock;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.screen.IceCreamMachineScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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

public class IceCreamMachineBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, SidedInventory, EnergyProvider {
    public static final int TOTAL_SLOTS = 6;
    public static final int REFRIGERANT_SLOT = 0;
    public static final int BASE_SLOT = 1;
    public static final int SWEETENER_SLOT = 2;
    public static final int FLAVOR_SLOT = 3;
    public static final int OUTPUT_SLOT = 4;
    public static final int RETURN_SLOT = 5;

    public static final int CAPACITY = 20000;
    public static final int MAX_INSERT = 200;
    public static final int POWER_PER_TICK = 20;

    private static final int[] TOP_SLOTS = new int[]{REFRIGERANT_SLOT, BASE_SLOT, SWEETENER_SLOT, FLAVOR_SLOT};
    private static final int[] BOTTOM_SLOTS = new int[]{OUTPUT_SLOT, RETURN_SLOT};
    private static final int[] SIDE_SLOTS = new int[]{REFRIGERANT_SLOT, BASE_SLOT, SWEETENER_SLOT, FLAVOR_SLOT};

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(TOTAL_SLOTS, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(CAPACITY, MAX_INSERT, 0, 0);

    private int refrigerationTime = 0;
    private int maxRefrigerationTime = 0;
    private int churnProgress = 0;
    private int maxChurnProgress = 100; // 5 seconds per batch

    private final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> IceCreamMachineBlockEntity.this.energyStorage.getEnergy() & 0xFFFF;
                case 1 -> (IceCreamMachineBlockEntity.this.energyStorage.getEnergy() >> 16) & 0xFFFF;
                case 2 -> IceCreamMachineBlockEntity.this.energyStorage.getMaxEnergy() & 0xFFFF;
                case 3 -> (IceCreamMachineBlockEntity.this.energyStorage.getMaxEnergy() >> 16) & 0xFFFF;
                case 4 -> IceCreamMachineBlockEntity.this.churnProgress;
                case 5 -> IceCreamMachineBlockEntity.this.maxChurnProgress;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 4 -> IceCreamMachineBlockEntity.this.churnProgress = value;
                case 5 -> IceCreamMachineBlockEntity.this.maxChurnProgress = value;
            }
        }

        @Override
        public int size() {
            return 6;
        }
    };

    public IceCreamMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ICE_CREAM_MACHINE_BLOCK_ENTITY, pos, state);
    }

    public boolean manualCrank() {
        if (canChurn()) {
            if (this.refrigerationTime <= 0) {
                ItemStack refStack = this.inventory.get(REFRIGERANT_SLOT);
                int time = getRefrigerationDuration(refStack);
                if (time > 0) {
                    this.refrigerationTime = time;
                    this.maxRefrigerationTime = time;
                    refStack.decrement(1);
                }
            }

            if (this.refrigerationTime > 0) {
                this.refrigerationTime = Math.max(0, this.refrigerationTime - 10);
                this.churnProgress += 20;
                if (this.churnProgress >= this.maxChurnProgress) {
                    finishBatch();
                    this.churnProgress = 0;
                }
                markDirty();
                return true;
            }
        }
        return false;
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, IceCreamMachineBlockEntity entity) {
        boolean dirty = false;

        boolean canWork = entity.canChurn();
        boolean hasPower = entity.energyStorage.getEnergy() >= POWER_PER_TICK;

        // Only maintain/consume refrigeration when actively ready to churn!
        if (canWork && hasPower) {
            // Consume refrigerant item ONLY when actively needed
            if (entity.refrigerationTime <= 0) {
                ItemStack refStack = entity.inventory.get(REFRIGERANT_SLOT);
                int time = getRefrigerationDuration(refStack);
                if (time > 0) {
                    entity.refrigerationTime = time;
                    entity.maxRefrigerationTime = time;
                    refStack.decrement(1);
                    dirty = true;
                }
            }

            // Automatic electric churning while refrigerated
            if (entity.refrigerationTime > 0) {
                entity.refrigerationTime--;
                entity.energyStorage.extractEnergy(POWER_PER_TICK, false);
                entity.churnProgress++;

                if (entity.churnProgress >= entity.maxChurnProgress) {
                    entity.finishBatch();
                    entity.churnProgress = 0;
                    world.playSound(null, pos, SoundEvents.BLOCK_BREWING_STAND_BREW, SoundCategory.BLOCKS, 0.7f, 1.2f);
                }
                dirty = true;
            }
        } else {
            // Idle: pause progress and DO NOT drain refrigeration time or eat ice!
            if (entity.churnProgress > 0) {
                entity.churnProgress = Math.max(0, entity.churnProgress - 1);
                dirty = true;
            }
        }

        boolean isRunning = (canWork && hasPower && entity.refrigerationTime > 0);
        if (state.get(IceCreamMachineBlock.LIT) != isRunning) {
            world.setBlockState(pos, state.with(IceCreamMachineBlock.LIT, isRunning), 3);
            dirty = true;
        }

        if (dirty) {
            entity.markDirty();
        }
    }

    public boolean canChurn() {
        if (this.refrigerationTime <= 0) {
            // Need refrigeration
            if (getRefrigerationDuration(this.inventory.get(REFRIGERANT_SLOT)) <= 0) return false;
        }

        ItemStack base = this.inventory.get(BASE_SLOT);
        if (!isBaseLiquid(base)) return false;

        ItemStack sweet = this.inventory.get(SWEETENER_SLOT);
        if (!isSweetener(sweet)) return false;

        ItemStack result = getRecipeResult();
        if (result.isEmpty()) return false;

        ItemStack currentOut = this.inventory.get(OUTPUT_SLOT);
        if (!currentOut.isEmpty()) {
            if (!ItemStack.areItemsAndComponentsEqual(currentOut, result)) return false;
            if (currentOut.getCount() + result.getCount() > currentOut.getMaxCount()) return false;
        }

        ItemStack currentReturn = this.inventory.get(RETURN_SLOT);
        ItemStack returnItem = getContainerReturnItem(base);
        if (!returnItem.isEmpty() && !currentReturn.isEmpty()) {
            if (!ItemStack.areItemsAndComponentsEqual(currentReturn, returnItem)) return false;
            if (currentReturn.getCount() + returnItem.getCount() > currentReturn.getMaxCount()) return false;
        }

        return true;
    }

    private void finishBatch() {
        ItemStack result = getRecipeResult();
        if (result.isEmpty()) return;

        ItemStack base = this.inventory.get(BASE_SLOT);
        ItemStack sweet = this.inventory.get(SWEETENER_SLOT);
        ItemStack flavor = this.inventory.get(FLAVOR_SLOT);

        ItemStack returnItem = getContainerReturnItem(base);

        base.decrement(1);
        sweet.decrement(1);
        if (!flavor.isEmpty()) {
            flavor.decrement(1);
        }

        // Output ice cream
        ItemStack currentOut = this.inventory.get(OUTPUT_SLOT);
        if (currentOut.isEmpty()) {
            this.inventory.set(OUTPUT_SLOT, result.copy());
        } else {
            currentOut.increment(result.getCount());
        }

        // Return empty container
        if (!returnItem.isEmpty()) {
            ItemStack currentReturn = this.inventory.get(RETURN_SLOT);
            if (currentReturn.isEmpty()) {
                this.inventory.set(RETURN_SLOT, returnItem.copy());
            } else {
                currentReturn.increment(returnItem.getCount());
            }
        }
    }

    public ItemStack getRecipeResult() {
        ItemStack flavor = this.inventory.get(FLAVOR_SLOT);
        if (flavor.isEmpty()) {
            return new ItemStack(ModItems.VANILLA_ICE_CREAM, 2);
        }
        Item f = flavor.getItem();
        if (f == ModItems.STRAWBERRY) return new ItemStack(ModItems.STRAWBERRY_ICE_CREAM, 2);
        if (f == ModItems.BLUEBERRY) return new ItemStack(ModItems.BLUEBERRY_ICE_CREAM, 2);
        if (f == Items.COCOA_BEANS) return new ItemStack(ModItems.CHOCOLATE_ICE_CREAM, 2);
        if (f == Items.SWEET_BERRIES) return new ItemStack(ModItems.SWEET_BERRY_ICE_CREAM, 2);
        return new ItemStack(ModItems.VANILLA_ICE_CREAM, 2);
    }

    private static ItemStack getContainerReturnItem(ItemStack base) {
        if (base.isOf(Items.MILK_BUCKET)) return new ItemStack(Items.BUCKET);
        return ItemStack.EMPTY;
    }

    private static int getRefrigerationDuration(ItemStack stack) {
        if (stack.isOf(Items.ICE)) return 600;
        if (stack.isOf(ModItems.ICE_CUBES)) return 400;
        if (stack.isOf(Items.PACKED_ICE)) return 1400;
        if (stack.isOf(Items.BLUE_ICE)) return 3600;
        if (stack.isOf(Items.SNOW_BLOCK)) return 600;
        if (stack.isOf(Items.SNOWBALL)) return 150;
        if (stack.isOf(ModItems.SALT)) return 800;
        return 0;
    }

    private static boolean isBaseLiquid(ItemStack stack) {
        return stack.isOf(Items.MILK_BUCKET) || stack.isOf(ModItems.SOY_MILK);
    }

    private static boolean isSweetener(ItemStack stack) {
        return stack.isOf(Items.SUGAR) || stack.isOf(Items.HONEY_BOTTLE);
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.inventory.clear();
        Inventories.readData(view, this.inventory);
        this.energyStorage.readData(view);
        this.refrigerationTime = view.getInt("RefrigTime", 0);
        this.maxRefrigerationTime = view.getInt("MaxRefrigTime", 0);
        this.churnProgress = view.getInt("ChurnProgress", 0);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, this.inventory);
        this.energyStorage.writeData(view);
        view.putInt("RefrigTime", this.refrigerationTime);
        view.putInt("MaxRefrigTime", this.maxRefrigerationTime);
        view.putInt("ChurnProgress", this.churnProgress);
    }

    @Override
    public int size() {
        return TOTAL_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : this.inventory) if (!s.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return this.inventory.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(this.inventory, slot, amount);
        if (!result.isEmpty()) markDirty();
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(this.inventory, slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        this.inventory.set(slot, stack);
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
        if (side == Direction.UP) return TOP_SLOTS;
        if (side == Direction.DOWN) return BOTTOM_SLOTS;
        return SIDE_SLOTS;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == OUTPUT_SLOT || slot == RETURN_SLOT) return false;
        if (slot == REFRIGERANT_SLOT) return getRefrigerationDuration(stack) > 0;
        if (slot == BASE_SLOT) return isBaseLiquid(stack);
        if (slot == SWEETENER_SLOT) return isSweetener(stack);
        return true;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot == OUTPUT_SLOT || slot == RETURN_SLOT;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.enchantedwood.ice_cream_machine");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new IceCreamMachineScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Nullable
    @Override
    public EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this.energyStorage;
    }

    public int getRefrigerationTime() {
        return this.refrigerationTime;
    }

    public int getChurnProgress() {
        return this.churnProgress;
    }

    public int getMaxChurnProgress() {
        return this.maxChurnProgress;
    }

    public DefaultedList<ItemStack> getInventory() {
        return this.inventory;
    }
}
