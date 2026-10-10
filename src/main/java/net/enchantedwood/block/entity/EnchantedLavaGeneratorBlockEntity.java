package net.enchantedwood.block.entity;

import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.block.custom.EnchantedLavaGeneratorBlock;
import net.enchantedwood.fluid.LavaProvider;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.GearItem;
import net.enchantedwood.screen.EnchantedLavaGeneratorScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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

public class EnchantedLavaGeneratorBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, LavaProvider {
    private final NonNullList<ItemStack> inventory = NonNullList.withSize(5, ItemStack.EMPTY);

    private int cookTime = 0;
    private int totalCookTime = 600;
    private int burnTime = 0;
    private int totalBurnTime = 0;
    private int lavaAmount = 0; // In mB / mL (Max 10,000 mL)
    public static final int MAX_LAVA = 10000;

    protected final ContainerData propertyDelegate = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> cookTime;
                case 1 -> totalCookTime;
                case 2 -> burnTime;
                case 3 -> totalBurnTime;
                case 4 -> getActiveGearTier().ordinal();
                case 5 -> lavaAmount;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> cookTime = value;
                case 1 -> totalCookTime = value;
                case 2 -> burnTime = value;
                case 3 -> totalBurnTime = value;
                case 5 -> lavaAmount = value;
            }
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    public EnchantedLavaGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENCHANTED_LAVA_GENERATOR_BLOCK_ENTITY, pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.enchantedwood.enchanted_lava_generator");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new EnchantedLavaGeneratorScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    public GearTier getActiveGearTier() {
        ItemStack gearStack = inventory.get(2);
        if (gearStack.getItem() instanceof GearItem gearItem) {
            if (gearItem.isEnchanted()) {
                return gearItem.getGearTier();
            }
        }
        return GearTier.NONE;
    }

    public static int getTierCookTime(GearTier tier) {
        return switch (tier) {
            case COPPER -> 480;   // 24s
            case BRONZE -> 360;   // 18s
            case GOLD -> 240;     // 12s
            case TITANIUM -> 180; // 9s
            case DIAMOND -> 120;  // 6s
            case NETHERITE -> 60; // 3s
            case BLAZE_OVERCLOCK -> 30; // 1.5s
            default -> 600;       // 30s
        };
    }

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, EnchantedLavaGeneratorBlockEntity entity) {
        boolean isBurningOriginally = entity.burnTime > 0;
        boolean stateChanged = false;

        if (entity.burnTime > 0) {
            --entity.burnTime;
        }

        GearTier currentGearTier = entity.getActiveGearTier();
        entity.totalCookTime = getTierCookTime(currentGearTier);

        if (state.getValue(EnchantedLavaGeneratorBlock.GEAR_TIER) != currentGearTier) {
            state = state.setValue(EnchantedLavaGeneratorBlock.GEAR_TIER, currentGearTier);
            world.setBlock(pos, state, 3);
            stateChanged = true;
        }

        ItemStack cobbleStack = entity.inventory.get(0);
        ItemStack fuelStack = entity.inventory.get(1);

        boolean canMelt = entity.canMeltCobble(cobbleStack);

        // Refuel ONLY using Enchanted Coal Block
        if (entity.burnTime <= 0 && canMelt) {
            if (fuelStack.is(ModBlocks.ENCHANTED_COAL_BLOCK.asItem())) {
                entity.burnTime = 90000;
                entity.totalBurnTime = 90000;
                fuelStack.shrink(1);
                stateChanged = true;
            }
        }

        // Process Melting Cobble into Lava (1 Cobblestone = 100 mL, 10 Cobble = 1,000 mL / 1 Bucket)
        if (entity.burnTime > 0 && canMelt) {
            ++entity.cookTime;
            if (entity.cookTime >= entity.totalCookTime) {
                entity.cookTime = 0;
                cobbleStack.shrink(1);
                entity.lavaAmount = Math.min(MAX_LAVA, entity.lavaAmount + 100);
                stateChanged = true;
            }
        } else {
            if (entity.cookTime > 0) {
                entity.cookTime = Math.max(0, entity.cookTime - 2);
            }
        }

        // Process Filling Empty Buckets from internal Lava buffer (1,000 mL = 1 Lava Bucket)
        if (entity.lavaAmount >= 1000) {
            ItemStack emptyBucketStack = entity.inventory.get(3);
            ItemStack outputStack = entity.inventory.get(4);

            Item lavaBucketItem = null;
            if (emptyBucketStack.is(Items.BUCKET)) {
                lavaBucketItem = Items.LAVA_BUCKET;
            } else if (emptyBucketStack.is(ModItems.COPPER_BUCKET)) {
                lavaBucketItem = ModItems.COPPER_LAVA_BUCKET;
            }

            if (lavaBucketItem != null) {
                if (outputStack.isEmpty()) {
                    emptyBucketStack.shrink(1);
                    entity.lavaAmount -= 1000;
                    entity.inventory.set(4, new ItemStack(lavaBucketItem, 1));
                    stateChanged = true;
                } else if (outputStack.is(lavaBucketItem) && outputStack.getCount() < outputStack.getMaxStackSize()) {
                    emptyBucketStack.shrink(1);
                    entity.lavaAmount -= 1000;
                    outputStack.grow(1);
                    stateChanged = true;
                }
            }
        }

        boolean isBurningNow = entity.burnTime > 0;
        if (isBurningOriginally != isBurningNow) {
            state = state.setValue(EnchantedLavaGeneratorBlock.LIT, isBurningNow);
            world.setBlock(pos, state, 3);
            stateChanged = true;
        }

        if (stateChanged) {
            setChanged(world, pos, state);
        }
    }

    private boolean canMeltCobble(ItemStack cobbleStack) {
        if (cobbleStack.isEmpty()) return false;
        if (!cobbleStack.is(Items.COBBLESTONE)) return false;
        return this.lavaAmount + 100 <= MAX_LAVA;
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        ContainerHelper.loadAllItems(view, this.inventory);
        this.cookTime = view.getIntOr("CookTime", 0);
        this.totalCookTime = view.getIntOr("TotalCookTime", 600);
        this.burnTime = view.getIntOr("BurnTime", 0);
        this.totalBurnTime = view.getIntOr("TotalBurnTime", 0);
        this.lavaAmount = view.getIntOr("LavaAmount", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        ContainerHelper.saveAllItems(view, this.inventory);
        view.putInt("CookTime", this.cookTime);
        view.putInt("TotalCookTime", this.totalCookTime);
        view.putInt("BurnTime", this.burnTime);
        view.putInt("TotalBurnTime", this.totalBurnTime);
        view.putInt("LavaAmount", this.lavaAmount);
    }


    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return new int[]{4}; // Output slot
        } else if (side == Direction.UP) {
            return new int[]{0, 3}; // Cobblestone input, Empty bucket input
        } else {
            return new int[]{1}; // Fuel slot
        }
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == 0) return stack.is(Items.COBBLESTONE);
        if (slot == 1) return stack.is(ModBlocks.ENCHANTED_COAL_BLOCK.asItem());
        if (slot == 2) return stack.getItem() instanceof GearItem gear && gear.isEnchanted();
        if (slot == 3) return stack.is(Items.BUCKET) || stack.is(ModItems.COPPER_BUCKET);
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot == 4;
    }

    @Override
    public int getContainerSize() {
        return inventory.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : inventory) {
            if (!stack.isEmpty()) return false;
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

    @Override
    public int getLavaAmount() {
        return this.lavaAmount;
    }

    @Override
    public int getMaxLava() {
        return MAX_LAVA;
    }

    @Override
    public int insertLava(int amount, boolean simulate) {
        return 0; // Only exports lava
    }

    @Override
    public boolean canInsertLava() {
        return false;
    }

    @Override
    public int extractLava(int amount, boolean simulate) {
        int extracted = Math.min(this.lavaAmount, amount);
        if (!simulate && extracted > 0) {
            this.lavaAmount -= extracted;
            setChanged();
        }
        return extracted;
    }

    @Override
    public boolean canExtractLava() {
        return this.lavaAmount > 0;
    }
}
