package net.enchantedwood.block.entity;

import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.block.custom.EnchantedChestBlock;
import net.enchantedwood.screen.EnchantedChestScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EnchantedChestBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, LidBlockEntity, Nameable {
    private final NonNullList<ItemStack> inventory = NonNullList.withSize(162, ItemStack.EMPTY);
    private GearTier gearTier = GearTier.NONE;
    private int activePage = 0;
    private int viewerCount = 0;

    private final ChestLidController lidAnimator = new ChestLidController();

    protected final ContainerData propertyDelegate = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> getGearTier().ordinal();
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
        public int getCount() {
            return 3;
        }
    };

    public EnchantedChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENCHANTED_CHEST_BLOCK_ENTITY, pos, state);
        if (state.hasProperty(EnchantedChestBlock.GEAR_TIER)) {
            this.gearTier = state.getValue(EnchantedChestBlock.GEAR_TIER);
        }
    }

    private final ContainerOpenersCounter stateManager = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level world, BlockPos pos, BlockState state) {
            world.playSound(null, (double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D,
                    SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.5F, world.getRandom().nextFloat() * 0.1F + 0.9F);
        }

        @Override
        protected void onClose(Level world, BlockPos pos, BlockState state) {
            world.playSound(null, (double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D,
                    SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.5F, world.getRandom().nextFloat() * 0.1F + 0.9F);
        }

        @Override
        protected void openerCountChanged(Level world, BlockPos pos, BlockState state, int oldViewerCount, int newViewerCount) {
            world.blockEvent(pos, state.getBlock(), 1, newViewerCount);
        }

        @Override
        public boolean isOwnContainer(Player player) {
            if (player.containerMenu instanceof EnchantedChestScreenHandler handler) {
                return handler.getInventory() == EnchantedChestBlockEntity.this;
            }
            return false;
        }
    };

    public static void clientTick(Level world, BlockPos pos, BlockState state, EnchantedChestBlockEntity entity) {
        entity.lidAnimator.tickLid();
    }

    public static void tick(Level world, BlockPos pos, BlockState state, EnchantedChestBlockEntity entity) {
        if (!entity.remove) {
            entity.stateManager.recheckOpeners(world, pos, state);
        }
        entity.lidAnimator.tickLid();
    }

    @Override
    public boolean triggerEvent(int type, int data) {
        if (type == 1) {
            this.lidAnimator.shouldBeOpen(data > 0);
            return true;
        }
        return super.triggerEvent(type, data);
    }

    public void onOpen(Player player) {
        if (!this.remove && !player.isSpectator()) {
            this.stateManager.incrementOpeners(player, this.getLevel(), this.getBlockPos(), this.getBlockState(), 5.0D);
        }
    }

    public void onClose(Player player) {
        if (!this.remove && !player.isSpectator()) {
            this.stateManager.decrementOpeners(player, this.getLevel(), this.getBlockPos(), this.getBlockState());
        }
    }






    @Override
    public float getOpenNess(float tickDelta) {
        return this.lidAnimator.getOpenness(tickDelta);
    }

    @Override
    public Component getName() {
        return getDisplayName();
    }

    @Override
    public boolean hasCustomName() {
        return getGearTier() != GearTier.NONE;
    }

    @Override
    public Component getCustomName() {
        return hasCustomName() ? getDisplayName() : null;
    }

    @Override
    public Component getDisplayName() {
        GearTier tier = getGearTier();
        if (tier != null && tier != GearTier.NONE) {
            return switch (tier) {
                case COPPER -> Component.translatable("item.enchantedwood.copper_enchanted_chest");
                case BRONZE -> Component.translatable("item.enchantedwood.bronze_enchanted_chest");
                case IRON -> Component.translatable("item.enchantedwood.iron_enchanted_chest");
                case ENCHANTED_IRON -> Component.translatable("item.enchantedwood.enchanted_iron_enchanted_chest");
                case ALUMINUM -> Component.translatable("item.enchantedwood.aluminum_enchanted_chest");
                case STEEL -> Component.translatable("item.enchantedwood.steel_enchanted_chest");
                case GOLD -> Component.translatable("item.enchantedwood.gold_enchanted_chest");
                case TITANIUM -> Component.translatable("item.enchantedwood.titanium_enchanted_chest");
                case DIAMOND -> Component.translatable("item.enchantedwood.diamond_enchanted_chest");
                case NETHERITE -> Component.translatable("item.enchantedwood.netherite_enchanted_chest");
                default -> Component.literal("Enchanted Chest (" + tier.getSerializedName() + ")");
            };
        }
        return Component.translatable("container.enchantedwood.enchanted_chest");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new EnchantedChestScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    public void upgradeTier(GearTier newTier) {
        this.gearTier = newTier;
        setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }

    public GearTier getGearTier() {
        if ((this.gearTier == null || this.gearTier == GearTier.NONE) && this.hasLevel() && this.getBlockState().hasProperty(EnchantedChestBlock.GEAR_TIER)) {
            GearTier stateTier = this.getBlockState().getValue(EnchantedChestBlock.GEAR_TIER);
            if (stateTier != null && stateTier != GearTier.NONE) {
                this.gearTier = stateTier;
            }
        }
        return this.gearTier != null ? this.gearTier : GearTier.NONE;
    }

    public int getMaxSlots() {
        return switch (getGearTier()) {
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
                if (ItemStack.isSameItemSameComponents(a, b)) {
                    int transfer = Math.min(b.getCount(), a.getMaxStackSize() - a.getCount());
                    if (transfer > 0) {
                        a.grow(transfer);
                        b.shrink(transfer);
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
        setChanged();
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        ContainerHelper.loadAllItems(view, this.inventory);
        int tierOrdinal = view.getIntOr("GearTier", -1);
        if (tierOrdinal >= 0 && tierOrdinal < GearTier.values().length) {
            this.gearTier = GearTier.values()[tierOrdinal];
        } else if (this.getBlockState().hasProperty(EnchantedChestBlock.GEAR_TIER)) {
            this.gearTier = this.getBlockState().getValue(EnchantedChestBlock.GEAR_TIER);
        }
        this.activePage = view.getIntOr("ActivePage", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        ContainerHelper.saveAllItems(view, this.inventory);
        view.putInt("GearTier", this.getGearTier().ordinal());
        view.putInt("ActivePage", this.activePage);
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("GearTier", this.getGearTier().ordinal());
        nbt.putInt("ActivePage", this.activePage);
        return nbt;
    }


    @Override
    public int[] getSlotsForFace(Direction side) {
        int max = getMaxSlots();
        int[] slots = new int[max];
        for (int i = 0; i < max; i++) {
            slots[i] = i;
        }
        return slots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot < getMaxSlots();
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot < getMaxSlots();
    }

    @Override
    public int getContainerSize() {
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
    public ItemStack getItem(int slot) {
        return inventory.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ContainerHelper.removeItem(inventory, slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(inventory, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        inventory.set(slot, stack);
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
        inventory.clear();
    }
}
