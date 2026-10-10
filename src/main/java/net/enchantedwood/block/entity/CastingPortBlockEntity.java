package net.enchantedwood.block.entity;

import net.enchantedwood.block.custom.CastingMode;
import net.enchantedwood.fluid.MoltenMetal;
import net.enchantedwood.fluid.MoltenMetalProvider;
import net.enchantedwood.screen.CastingPortScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CastingPortBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, MoltenMetalProvider {
    public static final int BUFFER_CAPACITY = 2_000; // 2,000 mB buffer
    public static final int CAST_TIME = 20; // 1 second (20 ticks) per cast
    public static final int OUTPUT_SLOT = 0;
    public static final int INVENTORY_SIZE = 1;

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private CastingMode mode = CastingMode.STANDBY;
    private MoltenMetal currentFluid = MoltenMetal.NONE;
    private int fluidAmount = 0;
    private int castProgress = 0;

    private BlockPos boundNetworkPos = null;
    private String boundDimension = "minecraft:overworld";

    protected final ContainerData propertyDelegate = new ContainerData() {
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
                case 8 -> isNetworkOnline() ? 1 : 0;
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
        public int getCount() {
            return 9;
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
        setChanged();
    }

    public boolean isCasting() {
        return this.castProgress > 0;
    }

    public void setMode(CastingMode newMode) {
        this.mode = newMode;
        this.castProgress = 0;
        setChanged();
    }

    public void bindNetwork(BlockPos pos, String dimension) {
        this.boundNetworkPos = pos;
        this.boundDimension = dimension != null ? dimension : "minecraft:overworld";
        setChanged();
    }

    public @Nullable BlockPos getBoundNetworkPos() {
        return this.boundNetworkPos;
    }

    public boolean isNetworkOnline() {
        return getNetworkTerminal() != null;
    }

    public @Nullable EnchantedStorageTerminalBlockEntity getNetworkTerminal() {
        if (this.level == null) return null;

        // 1. Check bound remote network if set via Wrench
        if (this.boundNetworkPos != null && this.level.getServer() != null) {
            ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, Identifier.parse(this.boundDimension));
            ServerLevel targetWorld = this.level.getServer().getLevel(dimKey);
            if (targetWorld != null) {
                BlockEntity be = targetWorld.getBlockEntity(this.boundNetworkPos);
                if (be instanceof EnchantedStorageTerminalBlockEntity terminal && terminal.isNetworkOnline()) {
                    return terminal;
                } else if (be instanceof EnchantedStorageControllerBlockEntity ctrl && ctrl.isOnline()) {
                    BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
                    for (int dx = -16; dx <= 16; dx++) {
                        for (int dy = -8; dy <= 8; dy++) {
                            for (int dz = -16; dz <= 16; dz++) {
                                mut.set(this.boundNetworkPos.getX() + dx, this.boundNetworkPos.getY() + dy, this.boundNetworkPos.getZ() + dz);
                                BlockEntity candidate = targetWorld.getBlockEntity(mut);
                                if (candidate instanceof EnchantedStorageTerminalBlockEntity t && t.isNetworkOnline()) {
                                    return t;
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Proximity fallback: search 32-block local base radius
        BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
        for (int dx = -32; dx <= 32; dx++) {
            for (int dy = -16; dy <= 16; dy++) {
                for (int dz = -32; dz <= 32; dz++) {
                    mut.set(this.worldPosition.getX() + dx, this.worldPosition.getY() + dy, this.worldPosition.getZ() + dz);
                    BlockEntity be = this.level.getBlockEntity(mut);
                    if (be instanceof EnchantedStorageTerminalBlockEntity terminal && terminal.isNetworkOnline()) {
                        return terminal;
                    }
                }
            }
        }
        return null;
    }

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, CastingPortBlockEntity entity) {
        boolean dirty = false;

        // 1. Pull fluid from adjacent tanks, pipes, or smelters if buffer has space
        if (entity.fluidAmount < BUFFER_CAPACITY) {
            for (Direction dir : Direction.values()) {
                BlockEntity neighbor = world.getBlockEntity(pos.relative(dir));
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
                            if (metal != MoltenMetal.NONE && metal != MoltenMetal.LAVA) {
                                int extracted = provider.extractFluid(metal, BUFFER_CAPACITY, false);
                                if (extracted > 0) {
                                    entity.currentFluid = metal;
                                    entity.fluidAmount = extracted;
                                    dirty = true;
                                    break;
                                }
                            }
                        }
                        if (dirty) break;
                    }
                }
            }
        }

        // 2. Solidify molten metal based on selected mode
        int cost = entity.mode.getFluidCostMb();
        if (entity.mode != CastingMode.STANDBY && entity.fluidAmount >= cost && entity.currentFluid != MoltenMetal.NONE) {
            Item castItem = switch (entity.mode) {
                case INGOT -> entity.currentFluid.getIngotItem();
                case BLOCK -> entity.currentFluid.getBlockItem();
                case NUGGET -> entity.currentFluid.getNuggetItem();
                case STANDBY -> null;
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

                        // A. First priority: Wireless beam directly into Digital Storage Network!
                        EnchantedStorageTerminalBlockEntity terminal = entity.getNetworkTerminal();
                        if (terminal != null && terminal.isNetworkOnline()) {
                            produced = terminal.depositItem(produced);
                            if (produced.isEmpty()) {
                                world.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.4f, 1.8f);
                                world.sendParticles(ParticleTypes.PORTAL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.1, 0.1, 0.1, 0.05);
                            }
                        }

                        // B. Second priority: Auto-deposit into adjacent containers (beneath or sides)
                        if (!produced.isEmpty()) {
                            for (Direction dir : new Direction[]{ Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST }) {
                                BlockEntity target = world.getBlockEntity(pos.relative(dir));
                                if (target instanceof Container targetInv && !(target instanceof CastingPortBlockEntity)) {
                                    produced = insertIntoInventory(targetInv, produced, dir.getOpposite());
                                    if (produced.isEmpty()) {
                                        break;
                                    }
                                }
                            }
                        }

                        // C. Third priority: Store in local output slot
                        if (!produced.isEmpty()) {
                            ItemStack current = entity.inventory.get(OUTPUT_SLOT);
                            if (current.isEmpty()) {
                                entity.inventory.set(OUTPUT_SLOT, produced);
                                produced = ItemStack.EMPTY;
                            } else if (ItemStack.isSameItemSameComponents(current, produced)) {
                                int space = current.getMaxStackSize() - current.getCount();
                                int toAdd = Math.min(space, produced.getCount());
                                current.grow(toAdd);
                                produced.shrink(toAdd);
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
            entity.setChanged();
        }
    }

    private static boolean canDepositAnywhere(ServerLevel world, BlockPos pos, CastingPortBlockEntity entity, Item castItem) {
        // 1. Wireless Digital Storage Network
        EnchantedStorageTerminalBlockEntity terminal = entity.getNetworkTerminal();
        if (terminal != null && terminal.isNetworkOnline()) {
            return true;
        }

        ItemStack testStack = new ItemStack(castItem);
        // 2. Adjacent containers
        for (Direction dir : new Direction[]{ Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST }) {
            BlockEntity target = world.getBlockEntity(pos.relative(dir));
            if (target instanceof Container targetInv && !(target instanceof CastingPortBlockEntity)) {
                if (canInsertIntoInventory(targetInv, testStack, dir.getOpposite())) {
                    return true;
                }
            }
        }

        // 3. Local output slot
        ItemStack current = entity.inventory.get(OUTPUT_SLOT);
        if (current.isEmpty()) {
            return true;
        }
        if (ItemStack.isSameItemSameComponents(current, testStack)) {
            return current.getCount() < current.getMaxStackSize();
        }
        return false;
    }

    private static boolean canInsertIntoInventory(Container inv, ItemStack stack, Direction side) {
        if (inv instanceof WorldlyContainer sided) {
            int[] slots = sided.getSlotsForFace(side);
            for (int slot : slots) {
                if (!sided.canPlaceItemThroughFace(slot, stack, side)) continue;
                ItemStack existing = sided.getItem(slot);
                if (existing.isEmpty()) return true;
                if (ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() < existing.getMaxStackSize()) {
                    return true;
                }
            }
        } else {
            for (int i = 0; i < inv.getContainerSize(); i++) {
                if (!inv.canPlaceItem(i, stack)) continue;
                ItemStack existing = inv.getItem(i);
                if (existing.isEmpty()) return true;
                if (ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() < existing.getMaxStackSize()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static ItemStack insertIntoInventory(Container inv, ItemStack stack, Direction side) {
        ItemStack remainder = stack.copy();
        if (inv instanceof WorldlyContainer sided) {
            int[] slots = sided.getSlotsForFace(side);
            for (int slot : slots) {
                if (remainder.isEmpty()) break;
                if (!sided.canPlaceItemThroughFace(slot, remainder, side)) continue;
                ItemStack existing = sided.getItem(slot);
                if (existing.isEmpty()) {
                    sided.setItem(slot, remainder.copy());
                    sided.setChanged();
                    return ItemStack.EMPTY;
                } else if (ItemStack.isSameItemSameComponents(existing, remainder)) {
                    int space = existing.getMaxStackSize() - existing.getCount();
                    if (space > 0) {
                        int toAdd = Math.min(space, remainder.getCount());
                        existing.grow(toAdd);
                        remainder.shrink(toAdd);
                        sided.setChanged();
                    }
                }
            }
        } else {
            for (int i = 0; i < inv.getContainerSize(); i++) {
                if (remainder.isEmpty()) break;
                if (!inv.canPlaceItem(i, remainder)) continue;

                ItemStack existing = inv.getItem(i);
                if (existing.isEmpty()) {
                    inv.setItem(i, remainder.copy());
                    inv.setChanged();
                    return ItemStack.EMPTY;
                } else if (ItemStack.isSameItemSameComponents(existing, remainder)) {
                    int space = existing.getMaxStackSize() - existing.getCount();
                    if (space > 0) {
                        int toAdd = Math.min(space, remainder.getCount());
                        existing.grow(toAdd);
                        remainder.shrink(toAdd);
                        inv.setChanged();
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
            setChanged();
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
            setChanged();
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
    public Component getDisplayName() {
        return Component.translatable("block.enchantedwood.casting_port");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new CastingPortScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[]{ OUTPUT_SLOT };
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return true;
    }

    @Override
    public int getContainerSize() {
        return INVENTORY_SIZE;
    }

    @Override
    public boolean isEmpty() {
        return inventory.get(OUTPUT_SLOT).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return inventory.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(inventory, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = ContainerHelper.takeItem(inventory, slot);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        inventory.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        inventory.clear();
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.inventory.clear();
        ContainerHelper.loadAllItems(view, this.inventory);
        this.mode = CastingMode.values()[Math.min(CastingMode.values().length - 1, Math.max(0, view.getIntOr("CastingMode", 0)))];
        this.currentFluid = MoltenMetal.fromId(view.getStringOr("FluidType", "none"));
        this.fluidAmount = view.getIntOr("FluidAmount", 0);
        this.castProgress = view.getIntOr("CastProgress", 0);
        if (view.contains("BoundX")) {
            this.boundNetworkPos = new BlockPos(view.getIntOr("BoundX", 0), view.getIntOr("BoundY", 0), view.getIntOr("BoundZ", 0));
            this.boundDimension = view.getStringOr("BoundDim", "minecraft:overworld");
        } else {
            this.boundNetworkPos = null;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        ContainerHelper.saveAllItems(view, this.inventory);
        view.putInt("CastingMode", this.mode.ordinal());
        view.putString("FluidType", this.currentFluid.getId());
        view.putInt("FluidAmount", this.fluidAmount);
        view.putInt("CastProgress", this.castProgress);
        if (this.boundNetworkPos != null) {
            view.putInt("BoundX", this.boundNetworkPos.getX());
            view.putInt("BoundY", this.boundNetworkPos.getY());
            view.putInt("BoundZ", this.boundNetworkPos.getZ());
            view.putString("BoundDim", this.boundDimension);
        }
    }
}
