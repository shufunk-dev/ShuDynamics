package net.enchantedwood.block.entity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestLidAnimator;
import net.minecraft.block.entity.LidOpenable;
import net.minecraft.block.entity.ViewerCountManager;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.block.custom.EnchantedChestBlock;
import net.enchantedwood.screen.EnchantedChestScreenHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EnchantedChestBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, SidedInventory, LidOpenable {
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(162, ItemStack.EMPTY);
    private GearTier gearTier = GearTier.NONE;
    private int activePage = 0;
    private int viewerCount = 0;

    private final ChestLidAnimator lidAnimator = new ChestLidAnimator();

    protected final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> gearTier.ordinal();
                case 1 -> activePage;
                case 2 -> getMaxSlots();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> gearTier = GearTier.values()[Math.min(value, GearTier.values().length - 1)];
                case 1 -> activePage = value;
            }
        }

        @Override
        public int size() {
            return 3;
        }
    };

    public EnchantedChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENCHANTED_CHEST_BLOCK_ENTITY, pos, state);
        if (state.contains(EnchantedChestBlock.GEAR_TIER)) {
            this.gearTier = state.get(EnchantedChestBlock.GEAR_TIER);
        }
    }

    private final ViewerCountManager stateManager = new ViewerCountManager() {
        @Override
        protected void onContainerOpen(World world, BlockPos pos, BlockState state) {
            world.playSound(null, (double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D,
                    SoundEvents.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 0.5F, world.random.nextFloat() * 0.1F + 0.9F);
        }

        @Override
        protected void onContainerClose(World world, BlockPos pos, BlockState state) {
            world.playSound(null, (double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D,
                    SoundEvents.BLOCK_CHEST_CLOSE, SoundCategory.BLOCKS, 0.5F, world.random.nextFloat() * 0.1F + 0.9F);
        }

        @Override
        protected void onViewerCountUpdate(World world, BlockPos pos, BlockState state, int oldViewerCount, int newViewerCount) {
            world.addSyncedBlockEvent(pos, state.getBlock(), 1, newViewerCount);
        }

        @Override
        public boolean isPlayerViewing(PlayerEntity player) {
            if (player.currentScreenHandler instanceof EnchantedChestScreenHandler handler) {
                return handler.getInventory() == EnchantedChestBlockEntity.this;
            }
            return false;
        }
    };

    public static void clientTick(World world, BlockPos pos, BlockState state, EnchantedChestBlockEntity entity) {
        entity.lidAnimator.step();
    }

    public static void tick(World world, BlockPos pos, BlockState state, EnchantedChestBlockEntity entity) {
        if (!entity.removed) {
            entity.stateManager.updateViewerCount(world, pos, state);
        }
        entity.lidAnimator.step();
    }

    @Override
    public boolean onSyncedBlockEvent(int type, int data) {
        if (type == 1) {
            this.lidAnimator.setOpen(data > 0);
            return true;
        }
        return super.onSyncedBlockEvent(type, data);
    }

    public void onOpen(PlayerEntity player) {
        if (!this.removed && !player.isSpectator()) {
            this.stateManager.openContainer(player, this.getWorld(), this.getPos(), this.getCachedState(), 5.0D);
        }
    }

    public void onClose(PlayerEntity player) {
        if (!this.removed && !player.isSpectator()) {
            this.stateManager.closeContainer(player, this.getWorld(), this.getPos(), this.getCachedState());
        }
    }






    @Override
    public float getAnimationProgress(float tickDelta) {
        return this.lidAnimator.getProgress(tickDelta);
    }

    @Override
    public Text getDisplayName() {
        if (this.gearTier != null && this.gearTier != GearTier.NONE) {
            String tierName = switch (this.gearTier) {
                case IRON, ENCHANTED_IRON -> "Iron";
                case COPPER -> "Copper";
                case BRONZE -> "Bronze";
                case ALUMINUM -> "Aluminum";
                case STEEL -> "Steel";
                case GOLD -> "Gold";
                case TITANIUM -> "Titanium";
                case DIAMOND -> "Diamond";
                case NETHERITE -> "Netherite";
                case BLAZE_OVERCLOCK -> "Blaze";
                default -> "";
            };
            if (!tierName.isEmpty()) {
                return Text.literal("Enchanted Chest (" + tierName + ")");
            }
        }
        return Text.translatable("container.enchantedwood.enchanted_chest");
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new EnchantedChestScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    public void upgradeTier(GearTier newTier) {
        this.gearTier = newTier;
        markDirty();
        if (this.world != null) {
            this.world.updateListeners(this.pos, getCachedState(), getCachedState(), net.minecraft.block.Block.NOTIFY_LISTENERS);
        }
    }


    public GearTier getGearTier() {
        return this.gearTier;
    }

    public int getMaxSlots() {
        return switch (gearTier) {
            case IRON, ENCHANTED_IRON -> 72;
            case COPPER -> 81;
            case BRONZE -> 90;
            case ALUMINUM -> 99;
            case STEEL -> 108;
            case GOLD -> 117;
            case TITANIUM -> 126;
            case DIAMOND -> 135;
            case NETHERITE -> 162;
            default -> 54;
        };
    }

    public int getMaxPages() {
        int maxSlots = getMaxSlots();
        return (int) Math.ceil((double) maxSlots / 54.0);
    }

    public void sortInventory() {
        List<ItemStack> nonEmpty = new ArrayList<>();
        for (int i = 0; i < getMaxSlots(); i++) {
            ItemStack stack = inventory.get(i);
            if (!stack.isEmpty()) {
                nonEmpty.add(stack.copy());
            }
        }

        for (int i = 0; i < nonEmpty.size(); i++) {
            ItemStack a = nonEmpty.get(i);
            if (a.isEmpty()) continue;
            for (int j = i + 1; j < nonEmpty.size(); j++) {
                ItemStack b = nonEmpty.get(j);
                if (b.isEmpty()) continue;
                if (ItemStack.areItemsAndComponentsEqual(a, b)) {
                    int transfer = Math.min(b.getCount(), a.getMaxCount() - a.getCount());
                    if (transfer > 0) {
                        a.increment(transfer);
                        b.decrement(transfer);
                    }
                }
            }
        }

        nonEmpty.removeIf(ItemStack::isEmpty);

        nonEmpty.sort(Comparator
                .comparing((ItemStack stack) -> stack.getItem().toString())
                .thenComparing(ItemStack::getCount, Comparator.reverseOrder()));

        for (int i = 0; i < 162; i++) {
            if (i < nonEmpty.size() && i < getMaxSlots()) {
                inventory.set(i, nonEmpty.get(i));
            } else {
                inventory.set(i, ItemStack.EMPTY);
            }
        }
        markDirty();
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        Inventories.readData(view, this.inventory);
        int tierOrdinal = view.getInt("GearTier", 0);
        this.gearTier = GearTier.values()[Math.min(tierOrdinal, GearTier.values().length - 1)];
        this.activePage = view.getInt("ActivePage", 0);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, this.inventory);
        view.putInt("GearTier", this.gearTier.ordinal());
        view.putInt("ActivePage", this.activePage);
    }

    @Override
    public net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket toUpdatePacket() {
        return net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(net.minecraft.registry.RegistryWrapper.WrapperLookup registries) {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("GearTier", this.gearTier.ordinal());
        nbt.putInt("ActivePage", this.activePage);
        return nbt;
    }


    @Override
    public int[] getAvailableSlots(Direction side) {
        int max = getMaxSlots();
        int[] slots = new int[max];
        for (int i = 0; i < max; i++) {
            slots[i] = i;
        }
        return slots;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot < getMaxSlots();
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot < getMaxSlots();
    }

    @Override
    public int size() {
        return inventory.size();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < getMaxSlots(); i++) {
            if (!inventory.get(i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return inventory.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return Inventories.splitStack(inventory, slot, amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(inventory, slot);
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
