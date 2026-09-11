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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.screen.SterileMedicalCabinetScreenHandler;
import org.jetbrains.annotations.Nullable;

import java.util.stream.IntStream;

public class SterileMedicalCabinetBlockEntity extends BlockEntity implements Inventory, NamedScreenHandlerFactory, SidedInventory {
    public static final int INVENTORY_SIZE = 36;
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);

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
            // Row 1: Fabrication & Injectors
            case 0 -> item == ModItems.HYPOSPRAY;
            case 1 -> item == ModItems.EMPTY_CARTRIDGE;
            case 2 -> stack.isOf(Items.GLASS_PANE);
            case 3 -> stack.isOf(Items.GLASS);
            case 4 -> item == ModItems.TITANIUM_INGOT;
            case 5 -> item == ModItems.TITANIUM_NUGGET;
            case 6 -> stack.isOf(Items.QUARTZ);
            case 7 -> stack.isOf(Items.REDSTONE);
            case 8 -> stack.isOf(Items.GLOWSTONE_DUST);

            // Row 2: Extracted Essences
            case 9 -> item == ModItems.ALKALINE_BASE_EXTRACT;
            case 10 -> item == ModItems.CRYO_THERMAL_EXTRACT;
            case 11 -> item == ModItems.OXYGENATED_EXTRACT;
            case 12 -> item == ModItems.CELLULAR_NANITE_EXTRACT;
            case 13 -> item == ModItems.ADRENAL_ESSENCE;
            case 14, 15, 16, 17 -> isEssence(stack);

            // Row 3: Biological Feeds & Catalysts
            case 18 -> stack.isOf(Items.SLIME_BALL);
            case 19 -> stack.isOf(Items.MAGMA_CREAM) || stack.isOf(Items.CRIMSON_FUNGUS);
            case 20 -> stack.isOf(Items.KELP) || stack.isOf(Items.SEAGRASS) || item == ModItems.CUCUMBER;
            case 21 -> item == ModItems.DRAGON_FRUIT || stack.isOf(Items.NETHER_WART);
            case 22 -> stack.isOf(Items.GLOW_BERRIES) || item == ModItems.WASABI_ROOT;
            case 23 -> item == ModItems.SULFUR_DUST;
            case 24 -> stack.isOf(Items.BLAZE_POWDER) || item == ModItems.FIRE_CRYSTAL;
            case 25 -> stack.isOf(Items.GOLDEN_APPLE) || stack.isOf(Items.GHAST_TEAR);
            case 26 -> stack.isOf(Items.SUGAR);

            // Row 4: Finished & Pure Hypospray Cartridges
            case 27 -> item == ModItems.ACID_NEUTRALIZING_CARTRIDGE;
            case 28 -> item == ModItems.HEAT_BUFFER_CARTRIDGE;
            case 29 -> item == ModItems.HYPER_OXYGENATION_CARTRIDGE;
            case 30 -> item == ModItems.NANITE_TRAUMA_CARTRIDGE;
            case 31 -> item == ModItems.ADRENALINE_STIM_CARTRIDGE;
            case 32, 33, 34, 35 -> isCartridge(stack);

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
    public int size() {
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
    public boolean isValid(int slot, ItemStack stack) {
        return isItemValidForSlot(slot, stack);
    }

    @Override
    public void clear() {
        inventory.clear();
        markDirty();
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        if (side == Direction.UP) return SLOTS_TOP;
        if (side == Direction.DOWN) return SLOTS_BOTTOM;
        return SLOTS_ALL;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        if (dir == Direction.DOWN) {
            return slot >= 27; // Extract finished cartridges from bottom
        }
        return true;
    }

    @Override
    public Text getDisplayName() {
        return Text.literal("§b✦ Sterile Medical Cabinet ✦");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new SterileMedicalCabinetScreenHandler(syncId, playerInventory, this);
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        Inventories.readData(view, this.inventory);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, this.inventory);
    }

    @Override
    public net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket toUpdatePacket() {
        return net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(net.minecraft.registry.RegistryWrapper.WrapperLookup registries) {
        NbtCompound nbt = new NbtCompound();
        return nbt;
    }
}
