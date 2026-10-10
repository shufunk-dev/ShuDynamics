package net.enchantedwood.block.entity;

import net.enchantedwood.item.ModItems;
import net.enchantedwood.screen.SterileMedicalCabinetScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.stream.IntStream;

public class SterileMedicalCabinetBlockEntity extends BlockEntity implements Container, MenuProvider, WorldlyContainer {
    public static final int INVENTORY_SIZE = 36;
    private final NonNullList<ItemStack> inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);

    private static final int[] SLOTS_TOP = IntStream.range(0, 32).toArray();
    private static final int[] SLOTS_BOTTOM = IntStream.range(27, 36).toArray();
    private static final int[] SLOTS_ALL = IntStream.range(0, 36).toArray();

    public SterileMedicalCabinetBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STERILE_MEDICAL_CABINET_BE, pos, state);
    }

    public static boolean isEssence(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item == ModItems.ALKALINE_BASE_EXTRACT ||
               item == ModItems.CRYO_THERMAL_EXTRACT ||
               item == ModItems.OXYGENATED_EXTRACT ||
               item == ModItems.CELLULAR_NANITE_EXTRACT ||
               item == ModItems.ADRENAL_ESSENCE;
    }

    public static boolean isCartridge(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item == ModItems.ACID_NEUTRALIZING_CARTRIDGE ||
               item == ModItems.HEAT_BUFFER_CARTRIDGE ||
               item == ModItems.HYPER_OXYGENATION_CARTRIDGE ||
               item == ModItems.NANITE_TRAUMA_CARTRIDGE ||
               item == ModItems.ADRENALINE_STIM_CARTRIDGE;
    }

    public static boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();

        return switch (slot) {
            // Row 1: Cartridge Fabrication & Injectors
            case 0 -> item == ModItems.HYPOSPRAY;
            case 1 -> item == ModItems.EMPTY_CARTRIDGE;
            case 2 -> stack.is(Items.GLASS_PANE);
            case 3 -> stack.is(Items.GLASS);
            case 4 -> item == ModItems.TIN_INGOT;
            case 5 -> item == ModItems.TITANIUM_INGOT || item == ModItems.TITANIUM_NUGGET;
            case 6 -> stack.is(Items.QUARTZ);
            case 7 -> stack.is(Items.REDSTONE);
            case 8 -> stack.is(Items.GLOWSTONE_DUST);

            // Row 2: Synthesized Essences & Recycled Byproducts
            case 9 -> item == ModItems.ALKALINE_BASE_EXTRACT;
            case 10 -> item == ModItems.CRYO_THERMAL_EXTRACT;
            case 11 -> item == ModItems.OXYGENATED_EXTRACT;
            case 12 -> item == ModItems.CELLULAR_NANITE_EXTRACT;
            case 13 -> item == ModItems.ADRENAL_ESSENCE;
            case 14 -> item == ModItems.VOLCANIC_ASH;
            case 15 -> stack.is(Items.BONE_MEAL);
            case 16 -> item == ModItems.SULFUR_DUST;
            case 17 -> stack.is(Items.SUGAR);

            // Row 3: Biological Feedstocks, Catalysts & Cleanroom Textiles
            case 18 -> stack.is(Items.SLIME_BALL);
            case 19 -> stack.is(Items.MAGMA_CREAM) || stack.is(Items.CRIMSON_FUNGUS);
            case 20 -> stack.is(Items.KELP) || stack.is(Items.SEAGRASS) || item == ModItems.CUCUMBER;
            case 21 -> item == ModItems.DRAGON_FRUIT || stack.is(Items.NETHER_WART);
            case 22 -> stack.is(Items.GLOW_BERRIES) || item == ModItems.WASABI_ROOT;
            case 23 -> stack.is(Items.BLAZE_POWDER) || item == ModItems.FIRE_CRYSTAL;
            case 24 -> stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE) || stack.is(Items.GHAST_TEAR);
            case 25 -> item == ModItems.ALUMINUM_INGOT || stack.is(Items.IRON_INGOT);
            case 26 -> item == ModItems.STERILE_POLYMER_FABRIC;

            // Row 4: Finished Cartridges (Standard & ✦ Pure) & Dispensary Buffer
            case 27 -> item == ModItems.ACID_NEUTRALIZING_CARTRIDGE;
            case 28 -> item == ModItems.HEAT_BUFFER_CARTRIDGE;
            case 29 -> item == ModItems.HYPER_OXYGENATION_CARTRIDGE;
            case 30 -> item == ModItems.NANITE_TRAUMA_CARTRIDGE;
            case 31 -> item == ModItems.ADRENALINE_STIM_CARTRIDGE;
            case 32, 33, 34, 35 -> isCartridge(stack) || item == ModItems.HYPOSPRAY;

            default -> false;
        };
    }

    public static boolean isMedicalItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            if (isItemValidForSlot(i, stack)) return true;
        }
        return false;
    }

    @Override
    public int getContainerSize() {
        return INVENTORY_SIZE;
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
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return isItemValidForSlot(slot, stack);
    }

    @Override
    public void clearContent() {
        inventory.clear();
        setChanged();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.UP) return SLOTS_TOP;
        if (side == Direction.DOWN) return SLOTS_BOTTOM;
        return SLOTS_ALL;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        if (dir == Direction.DOWN) {
            return slot >= 27; // Extract finished cartridges from bottom
        }
        return true;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("§b✦ Sterile Medical Cabinet ✦");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new SterileMedicalCabinetScreenHandler(syncId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        ContainerHelper.loadAllItems(view, this.inventory);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        ContainerHelper.saveAllItems(view, this.inventory);
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag nbt = new CompoundTag();
        return nbt;
    }
}
