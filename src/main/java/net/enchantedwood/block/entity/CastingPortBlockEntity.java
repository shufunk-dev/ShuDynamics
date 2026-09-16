package net.enchantedwood.block.entity;

import net.enchantedwood.block.custom.CastingMode;
import net.enchantedwood.fluid.MoltenMetal;
import net.enchantedwood.fluid.MoltenMetalProvider;
import net.enchantedwood.screen.CastingPortScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CastingPortBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, SidedInventory, MoltenMetalProvider {
    public static final int BUFFER_CAPACITY = 2_000; // 2,000 mB buffer
    public static final int CAST_TIME = 20; // 1 second (20 ticks) per cast
    public static final int OUTPUT_SLOT = 0;
    public static final int INVENTORY_SIZE = 1;

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private CastingMode mode = CastingMode.INGOT;
    private MoltenMetal currentFluid = MoltenMetal.NONE;
    private int fluidAmount = 0;
    private int castProgress = 0;

    protected final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> mode.ordinal();
                case 1 -> currentFluid.ordinal();
                case 2 -> fluidAmount & 0xFFFF;
                case 3 -> (fluidAmount >> 16) & 0xFFFF;
                case 4 -> castProgress;
                case 5 -> CAST_TIME;
                case 6 -> BUFFER_CAPACITY & 0xFFFF;
                case 7 -> (BUFFER_CAPACITY >> 16) & 0xFFFF;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> {
                    CastingMode[] modes = CastingMode.values();
                    if (value >= 0 && value < modes.length) mode = modes[value];
                }
                case 4 -> castProgress = value;
            }
        }

        @Override
        public int size() {
            return 8;
        }
    };

    public CastingPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CASTING_PORT_BE, pos, state);
    }

    public CastingMode getMode() {
        return this.mode;
    }

    public void cycleMode() {
        this.mode = this.mode.next();
        this.castProgress = 0;
        markDirty();
    }

    public void setMode(CastingMode newMode) {
        this.mode = newMode;
        this.castProgress = 0;
        markDirty();
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, CastingPortBlockEntity entity) {
        boolean dirty = false;

        // 1. Pull fluid from adjacent tanks, pipes, or smelters if buffer has space
        if (entity.fluidAmount < BUFFER_CAPACITY) {
            for (Direction dir : Direction.values()) {
                BlockEntity neighbor = world.getBlockEntity(pos.offset(dir));
                if (neighbor instanceof MoltenMetalProvider provider && !(neighbor instanceof CastingPortBlockEntity)) {
                    // Pull either currently stored metal or adopt first available
                    if (entity.currentFluid != MoltenMetal.NONE && entity.fluidAmount > 0) {
                        int needed = BUFFER_CAPACITY - entity.fluidAmount;
                        int extracted = provider.extractFluid(entity.currentFluid, needed, false);
                        if (extracted > 0) {
                            entity.fluidAmount += extracted;
                            dirty = true;
                            break;
                        }
                    } else {
                        // Empty buffer: adopt available fluid
                        for (MoltenMetal metal : provider.getContainedFluids()) {
                            if (provider.getFluidAmount(metal) > 0) {
                                int needed = BUFFER_CAPACITY - entity.fluidAmount;
                                int extracted = provider.extractFluid(metal, needed, false);
                                if (extracted > 0) {
                                    entity.currentFluid = metal;
                                    entity.fluidAmount += extracted;
                                    dirty = true;
                                    break;
                                }
                            }
                        }
                        if (entity.fluidAmount > 0) break;
                    }
                }
            }
        }

        // 2. Perform Casting if enough fluid is present
        int cost = entity.mode.getFluidCostMb();
        if (entity.fluidAmount >= cost && entity.currentFluid != MoltenMetal.NONE) {
            Item castItem = switch (entity.mode) {
                case NUGGET -> entity.currentFluid.getNuggetItem();
                case INGOT -> entity.currentFluid.getIngotItem();
                case BLOCK -> entity.currentFluid.getBlockItem();
            };

            if (castItem != null) {
                boolean canDeposit = canDepositAnywhere(world, pos, entity, castItem);

                // Only progress casting if output can accept the finished item
                if (canDeposit && entity.castProgress < CAST_TIME) {
                    entity.castProgress++;
                    dirty = true;
                }

                // If progress reached completion, attempt to deposit and strictly only consume fluid on success
                if (entity.castProgress >= CAST_TIME) {
                    if (canDeposit) {
                        ItemStack produced = new ItemStack(castItem);
                        // Try to auto-deposit into inventory beneath or behind/sides
                        for (Direction dir : new Direction[]{ Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST }) {
                            BlockEntity target = world.getBlockEntity(pos.offset(dir));
                            if (target instanceof Inventory targetInv && !(target instanceof CastingPortBlockEntity)) {
                                produced = insertIntoInventory(targetInv, produced, dir.getOpposite());
                                if (produced.isEmpty()) {
                                    break;
                                }
                            }
                        }

                        // If not completely deposited into adjacent container, store in local output slot
                        if (!produced.isEmpty()) {
                            ItemStack current = entity.inventory.get(OUTPUT_SLOT);
                            if (current.isEmpty()) {
                                entity.inventory.set(OUTPUT_SLOT, produced);
                                produced = ItemStack.EMPTY;
                            } else if (ItemStack.areItemsAndComponentsEqual(current, produced)) {
                                int space = current.getMaxCount() - current.getCount();
                                int toAdd = Math.min(space, produced.getCount());
                                current.increment(toAdd);
                                produced.decrement(toAdd);
                            }
                        }

                        // ONLY deduct fluid and reset progress if item was completely and successfully stored!
                        if (produced.isEmpty()) {
                            entity.fluidAmount -= cost;
                            if (entity.fluidAmount <= 0) {
                                entity.currentFluid = MoltenMetal.NONE;
                            }
                            entity.castProgress = 0;
                            dirty = true;
                        } else {
                            // Output became blocked at the last moment: hold at CAST_TIME without losing fluid
                            entity.castProgress = CAST_TIME;
                        }
                    } else {
                        // Obstructed: hold progress at 100% until output is freed
                        entity.castProgress = CAST_TIME;
                    }
                }
            } else {
                entity.castProgress = 0;
            }
        } else {
            if (entity.castProgress > 0) {
                entity.castProgress = 0;
                dirty = true;
            }
        }

        if (dirty) {
            entity.markDirty();
        }
    }

    private static boolean canDepositAnywhere(ServerWorld world, BlockPos pos, CastingPortBlockEntity entity, Item castItem) {
        ItemStack testStack = new ItemStack(castItem);
        // 1. Check adjacent containers
        for (Direction dir : new Direction[]{ Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST }) {
            BlockEntity target = world.getBlockEntity(pos.offset(dir));
            if (target instanceof Inventory targetInv && !(target instanceof CastingPortBlockEntity)) {
                if (canInsertIntoInventory(targetInv, testStack, dir.getOpposite())) {
                    return true;
                }
            }
        }

        // 2. Check local output slot
        ItemStack current = entity.inventory.get(OUTPUT_SLOT);
        if (current.isEmpty()) {
            return true;
        }
        if (ItemStack.areItemsAndComponentsEqual(current, testStack)) {
            return current.getCount() < current.getMaxCount();
        }
        return false;
    }

    private static boolean canInsertIntoInventory(Inventory inv, ItemStack stack, Direction side) {
        if (inv instanceof SidedInventory sided) {
            int[] slots = sided.getAvailableSlots(side);
            for (int slot : slots) {
                if (!sided.canInsert(slot, stack, side)) continue;
                ItemStack existing = sided.getStack(slot);
                if (existing.isEmpty()) return true;
                if (ItemStack.areItemsAndComponentsEqual(existing, stack) && existing.getCount() < existing.getMaxCount()) {
                    return true;
                }
            }
        } else {
            for (int i = 0; i < inv.size(); i++) {
                if (!inv.isValid(i, stack)) continue;
                ItemStack existing = inv.getStack(i);
                if (existing.isEmpty()) return true;
                if (ItemStack.areItemsAndComponentsEqual(existing, stack) && existing.getCount() < existing.getMaxCount()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static ItemStack insertIntoInventory(Inventory inv, ItemStack stack, Direction side) {
        ItemStack remainder = stack.copy();
        if (inv instanceof SidedInventory sided) {
            int[] slots = sided.getAvailableSlots(side);
            for (int slot : slots) {
                if (remainder.isEmpty()) break;
                if (!sided.canInsert(slot, remainder, side)) continue;
                ItemStack existing = sided.getStack(slot);
                if (existing.isEmpty()) {
                    sided.setStack(slot, remainder.copy());
                    sided.markDirty();
                    return ItemStack.EMPTY;
                } else if (ItemStack.areItemsAndComponentsEqual(existing, remainder)) {
                    int space = existing.getMaxCount() - existing.getCount();
                    if (space > 0) {
                        int toAdd = Math.min(space, remainder.getCount());
                        existing.increment(toAdd);
                        remainder.decrement(toAdd);
                        sided.markDirty();
                    }
                }
            }
        } else {
            for (int i = 0; i < inv.size(); i++) {
                if (remainder.isEmpty()) break;
                if (!inv.isValid(i, remainder)) continue;

                ItemStack existing = inv.getStack(i);
                if (existing.isEmpty()) {
                    inv.setStack(i, remainder.copy());
                    inv.markDirty();
                    return ItemStack.EMPTY;
                } else if (ItemStack.areItemsAndComponentsEqual(existing, remainder)) {
                    int space = existing.getMaxCount() - existing.getCount();
                    if (space > 0) {
                        int toAdd = Math.min(space, remainder.getCount());
                        existing.increment(toAdd);
                        remainder.decrement(toAdd);
                        inv.markDirty();
                    }
                }
            }
        }
        return remainder;
    }

    // ==========================================
    // MOLTEN METAL PROVIDER IMPLEMENTATION
    // ==========================================
    @Override
    public MoltenMetal getFluidType() {
        return this.currentFluid;
    }

    @Override
    public int getFluidAmount(MoltenMetal metal) {
        return (metal == this.currentFluid) ? this.fluidAmount : 0;
    }

    @Override
    public int getMaxFluid() {
        return BUFFER_CAPACITY;
    }

    @Override
    public int insertFluid(MoltenMetal metal, int amount, boolean simulate) {
        if (metal == MoltenMetal.NONE || amount <= 0) return 0;
        if (this.currentFluid != MoltenMetal.NONE && this.currentFluid != metal && this.fluidAmount > 0) return 0;

        int space = BUFFER_CAPACITY - this.fluidAmount;
        int insertable = Math.min(space, amount);
        if (!simulate && insertable > 0) {
            this.currentFluid = metal;
            this.fluidAmount += insertable;
            markDirty();
        }
        return insertable;
    }

    @Override
    public int extractFluid(MoltenMetal metal, int amount, boolean simulate) {
        if (metal == MoltenMetal.NONE || metal != this.currentFluid || amount <= 0) return 0;
        int extractable = Math.min(this.fluidAmount, amount);
        if (!simulate && extractable > 0) {
            this.fluidAmount -= extractable;
            if (this.fluidAmount <= 0) {
                this.currentFluid = MoltenMetal.NONE;
            }
            markDirty();
        }
        return extractable;
    }

    @Override
    public List<MoltenMetal> getContainedFluids() {
        return (this.currentFluid != MoltenMetal.NONE && this.fluidAmount > 0) ? List.of(this.currentFluid) : List.of();
    }

    // ==========================================
    // INVENTORY IMPLEMENTATION
    // ==========================================
    @Override
    public Text getDisplayName() {
        return Text.translatable("block.enchantedwood.casting_port");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new CastingPortScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return new int[]{ OUTPUT_SLOT };
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return true;
    }

    @Override
    public int size() {
        return INVENTORY_SIZE;
    }

    @Override
    public boolean isEmpty() {
        return inventory.get(OUTPUT_SLOT).isEmpty();
    }

    @Override
    public ItemStack getStack(int slot) {
        return inventory.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(inventory, slot, amount);
        if (!result.isEmpty()) markDirty();
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack result = Inventories.removeStack(inventory, slot);
        if (!result.isEmpty()) markDirty();
        return result;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        inventory.set(slot, stack);
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

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.inventory.clear();
        Inventories.readData(view, this.inventory);
        this.mode = CastingMode.values()[Math.min(CastingMode.values().length - 1, Math.max(0, view.getInt("CastingMode", 0)))];
        this.currentFluid = MoltenMetal.fromId(view.getString("FluidType", "none"));
        this.fluidAmount = view.getInt("FluidAmount", 0);
        this.castProgress = view.getInt("CastProgress", 0);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, this.inventory);
        view.putInt("CastingMode", this.mode.ordinal());
        view.putString("FluidType", this.currentFluid.getId());
        view.putInt("FluidAmount", this.fluidAmount);
        view.putInt("CastProgress", this.castProgress);
    }
}
