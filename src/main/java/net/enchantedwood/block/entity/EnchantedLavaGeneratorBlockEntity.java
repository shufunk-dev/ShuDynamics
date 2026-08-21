package net.enchantedwood.block.entity;

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
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.block.custom.EnchantedLavaGeneratorBlock;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.GearItem;
import net.enchantedwood.screen.EnchantedLavaGeneratorScreenHandler;
import org.jetbrains.annotations.Nullable;

public class EnchantedLavaGeneratorBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, SidedInventory {
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(5, ItemStack.EMPTY);

    private int cookTime = 0;
    private int totalCookTime = 600;
    private int burnTime = 0;
    private int totalBurnTime = 0;
    private int lavaAmount = 0; // In mB / mL (Max 10,000 mL)
    public static final int MAX_LAVA = 10000;

    protected final PropertyDelegate propertyDelegate = new PropertyDelegate() {
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
        public int size() {
            return 6;
        }
    };

    public EnchantedLavaGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENCHANTED_LAVA_GENERATOR_BLOCK_ENTITY, pos, state);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.enchantedwood.enchanted_lava_generator");
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
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
            case DIAMOND -> 120;  // 6s
            case NETHERITE -> 60; // 3s
            default -> 600;       // 30s
        };
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, EnchantedLavaGeneratorBlockEntity entity) {
        boolean isBurningOriginally = entity.burnTime > 0;
        boolean stateChanged = false;

        if (entity.burnTime > 0) {
            --entity.burnTime;
        }

        GearTier currentGearTier = entity.getActiveGearTier();
        entity.totalCookTime = getTierCookTime(currentGearTier);

        if (state.get(EnchantedLavaGeneratorBlock.GEAR_TIER) != currentGearTier) {
            state = state.with(EnchantedLavaGeneratorBlock.GEAR_TIER, currentGearTier);
            world.setBlockState(pos, state, 3);
            stateChanged = true;
        }

        ItemStack cobbleStack = entity.inventory.get(0);
        ItemStack fuelStack = entity.inventory.get(1);

        boolean canMelt = entity.canMeltCobble(cobbleStack);

        // Refuel ONLY using Enchanted Coal Block
        if (entity.burnTime <= 0 && canMelt) {
            if (fuelStack.isOf(ModBlocks.ENCHANTED_COAL_BLOCK.asItem())) {
                entity.burnTime = 90000;
                entity.totalBurnTime = 90000;
                fuelStack.decrement(1);
                stateChanged = true;
            }
        }

        // Process Melting Cobble into Lava (1 Cobblestone = 100 mL, 10 Cobble = 1,000 mL / 1 Bucket)
        if (entity.burnTime > 0 && canMelt) {
            ++entity.cookTime;
            if (entity.cookTime >= entity.totalCookTime) {
                entity.cookTime = 0;
                cobbleStack.decrement(1);
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
            if (emptyBucketStack.isOf(Items.BUCKET)) {
                lavaBucketItem = Items.LAVA_BUCKET;
            } else if (emptyBucketStack.isOf(ModItems.COPPER_BUCKET)) {
                lavaBucketItem = ModItems.COPPER_LAVA_BUCKET;
            }

            if (lavaBucketItem != null) {
                if (outputStack.isEmpty()) {
                    emptyBucketStack.decrement(1);
                    entity.lavaAmount -= 1000;
                    entity.inventory.set(4, new ItemStack(lavaBucketItem, 1));
                    stateChanged = true;
                } else if (outputStack.isOf(lavaBucketItem) && outputStack.getCount() < outputStack.getMaxCount()) {
                    emptyBucketStack.decrement(1);
                    entity.lavaAmount -= 1000;
                    outputStack.increment(1);
                    stateChanged = true;
                }
            }
        }

        boolean isBurningNow = entity.burnTime > 0;
        if (isBurningOriginally != isBurningNow) {
            state = state.with(EnchantedLavaGeneratorBlock.LIT, isBurningNow);
            world.setBlockState(pos, state, 3);
            stateChanged = true;
        }

        if (stateChanged) {
            markDirty(world, pos, state);
        }
    }

    private boolean canMeltCobble(ItemStack cobbleStack) {
        if (cobbleStack.isEmpty()) return false;
        if (!cobbleStack.isOf(Items.COBBLESTONE)) return false;
        return this.lavaAmount + 100 <= MAX_LAVA;
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        Inventories.readData(view, this.inventory);
        this.cookTime = view.getInt("CookTime", 0);
        this.totalCookTime = view.getInt("TotalCookTime", 600);
        this.burnTime = view.getInt("BurnTime", 0);
        this.totalBurnTime = view.getInt("TotalBurnTime", 0);
        this.lavaAmount = view.getInt("LavaAmount", 0);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, this.inventory);
        view.putInt("CookTime", this.cookTime);
        view.putInt("TotalCookTime", this.totalCookTime);
        view.putInt("BurnTime", this.burnTime);
        view.putInt("TotalBurnTime", this.totalBurnTime);
        view.putInt("LavaAmount", this.lavaAmount);
    }


    @Override
    public int[] getAvailableSlots(Direction side) {
        if (side == Direction.DOWN) {
            return new int[]{4}; // Output slot
        } else if (side == Direction.UP) {
            return new int[]{0, 3}; // Cobblestone input, Empty bucket input
        } else {
            return new int[]{1}; // Fuel slot
        }
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == 0) return stack.isOf(Items.COBBLESTONE);
        if (slot == 1) return stack.isOf(ModBlocks.ENCHANTED_COAL_BLOCK.asItem());
        if (slot == 2) return stack.getItem() instanceof GearItem gear && gear.isEnchanted();
        if (slot == 3) return stack.isOf(Items.BUCKET) || stack.isOf(ModItems.COPPER_BUCKET);
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot == 4;
    }

    @Override
    public int size() {
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
