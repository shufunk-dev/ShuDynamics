package net.enchantedwood.block.entity;

import net.enchantedwood.block.custom.IceCreamMachineBlock;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.SimpleEnergyStorage;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.screen.IceCreamMachineScreenHandler;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class IceCreamMachineBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, EnergyProvider {
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

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(CAPACITY, MAX_INSERT, 0, 0);

    private int refrigerationTime = 0;
    private int maxRefrigerationTime = 0;
    private int churnProgress = 0;
    private int maxChurnProgress = 100; // 5 seconds per batch

    private final ContainerData propertyDelegate = new ContainerData() {
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
        public int getCount() {
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
                    refStack.shrink(1);
                }
            }

            if (this.refrigerationTime > 0) {
                this.refrigerationTime = Math.max(0, this.refrigerationTime - 10);
                this.churnProgress += 20;
                if (this.churnProgress >= this.maxChurnProgress) {
                    finishBatch();
                    this.churnProgress = 0;
                }
                setChanged();
                return true;
            }
        }
        return false;
    }

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, IceCreamMachineBlockEntity entity) {
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
                    refStack.shrink(1);
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
                    world.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.7f, 1.2f);
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
        if (state.getValue(IceCreamMachineBlock.LIT) != isRunning) {
            world.setBlock(pos, state.setValue(IceCreamMachineBlock.LIT, isRunning), 3);
            dirty = true;
        }

        if (dirty) {
            entity.setChanged();
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
            if (!ItemStack.isSameItemSameComponents(currentOut, result)) return false;
            if (currentOut.getCount() + result.getCount() > currentOut.getMaxStackSize()) return false;
        }

        ItemStack currentReturn = this.inventory.get(RETURN_SLOT);
        ItemStack returnItem = getContainerReturnItem(base);
        if (!returnItem.isEmpty() && !currentReturn.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(currentReturn, returnItem)) return false;
            if (currentReturn.getCount() + returnItem.getCount() > currentReturn.getMaxStackSize()) return false;
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

        base.shrink(1);
        sweet.shrink(1);
        if (!flavor.isEmpty()) {
            flavor.shrink(1);
        }

        // Output ice cream
        ItemStack currentOut = this.inventory.get(OUTPUT_SLOT);
        if (currentOut.isEmpty()) {
            this.inventory.set(OUTPUT_SLOT, result.copy());
        } else {
            currentOut.grow(result.getCount());
        }

        // Return empty container
        if (!returnItem.isEmpty()) {
            ItemStack currentReturn = this.inventory.get(RETURN_SLOT);
            if (currentReturn.isEmpty()) {
                this.inventory.set(RETURN_SLOT, returnItem.copy());
            } else {
                currentReturn.grow(returnItem.getCount());
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
        if (base.is(Items.MILK_BUCKET)) return new ItemStack(Items.BUCKET);
        return ItemStack.EMPTY;
    }

    private static int getRefrigerationDuration(ItemStack stack) {
        if (stack.is(Items.ICE)) return 600;
        if (stack.is(ModItems.ICE_CUBES)) return 400;
        if (stack.is(Items.PACKED_ICE)) return 1400;
        if (stack.is(Items.BLUE_ICE)) return 3600;
        if (stack.is(Items.SNOW_BLOCK)) return 600;
        if (stack.is(Items.SNOWBALL)) return 150;
        if (stack.is(ModItems.SALT)) return 800;
        return 0;
    }

    private static boolean isBaseLiquid(ItemStack stack) {
        return stack.is(Items.MILK_BUCKET) || stack.is(ModItems.SOY_MILK);
    }

    private static boolean isSweetener(ItemStack stack) {
        return stack.is(Items.SUGAR) || stack.is(Items.HONEY_BOTTLE);
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.inventory.clear();
        ContainerHelper.loadAllItems(view, this.inventory);
        this.energyStorage.readData(view);
        this.refrigerationTime = view.getIntOr("RefrigTime", 0);
        this.maxRefrigerationTime = view.getIntOr("MaxRefrigTime", 0);
        this.churnProgress = view.getIntOr("ChurnProgress", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        ContainerHelper.saveAllItems(view, this.inventory);
        this.energyStorage.writeData(view);
        view.putInt("RefrigTime", this.refrigerationTime);
        view.putInt("MaxRefrigTime", this.maxRefrigerationTime);
        view.putInt("ChurnProgress", this.churnProgress);
    }

    @Override
    public int getContainerSize() {
        return TOTAL_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : this.inventory) if (!s.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.inventory.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(this.inventory, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.inventory, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.inventory.set(slot, stack);
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
        if (side == Direction.UP) return TOP_SLOTS;
        if (side == Direction.DOWN) return BOTTOM_SLOTS;
        return SIDE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == OUTPUT_SLOT || slot == RETURN_SLOT) return false;
        if (slot == REFRIGERANT_SLOT) return getRefrigerationDuration(stack) > 0;
        if (slot == BASE_SLOT) return isBaseLiquid(stack);
        if (slot == SWEETENER_SLOT) return isSweetener(stack);
        return true;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot == OUTPUT_SLOT || slot == RETURN_SLOT;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.enchantedwood.ice_cream_machine");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
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

    public NonNullList<ItemStack> getInventory() {
        return this.inventory;
    }
}
