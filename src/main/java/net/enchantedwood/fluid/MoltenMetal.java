package net.enchantedwood.fluid;

import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.item.ModItems;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.StringIdentifiable;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public enum MoltenMetal implements StringIdentifiable {
    NONE("none", "None", 0x00000000, null, null, null),
    LAVA("lava", "Lava", 0xFFD95B10, null, null, null),
    IRON("iron", "Molten Iron", 0xFFD8D8D8, () -> Items.IRON_NUGGET, () -> Items.IRON_INGOT, () -> Items.IRON_BLOCK),
    GOLD("gold", "Molten Gold", 0xFFFEE649, () -> Items.GOLD_NUGGET, () -> Items.GOLD_INGOT, () -> Items.GOLD_BLOCK),
    COPPER("copper", "Molten Copper", 0xFFE77C56, () -> ModItems.COPPER_NUGGET, () -> Items.COPPER_INGOT, () -> Items.COPPER_BLOCK),
    TIN("tin", "Molten Tin", 0xFFBAC8CF, () -> ModItems.TIN_NUGGET, () -> ModItems.TIN_INGOT, () -> ModBlocks.TIN_BLOCK.asItem()),
    BRONZE("bronze", "Molten Bronze", 0xFFDE8735, () -> ModItems.BRONZE_NUGGET, () -> ModItems.BRONZE_INGOT, () -> ModBlocks.BRONZE_BLOCK.asItem()),
    ALUMINUM("aluminum", "Molten Aluminum", 0xFFCCD5DD, () -> ModItems.ALUMINUM_NUGGET, () -> ModItems.ALUMINUM_INGOT, () -> ModBlocks.ALUMINUM_BLOCK.asItem()),
    STEEL("steel", "Molten Steel", 0xFF5F6570, () -> ModItems.STEEL_NUGGET, () -> ModItems.STEEL_INGOT, () -> ModBlocks.STEEL_BLOCK.asItem()),
    TITANIUM("titanium", "Molten Titanium", 0xFFC8CCDC, () -> ModItems.TITANIUM_NUGGET, () -> ModItems.TITANIUM_INGOT, () -> ModBlocks.TITANIUM_BLOCK.asItem()),
    COBALT("cobalt", "Molten Cobalt", 0xFF1C5FD4, () -> ModItems.COBALT_NUGGET, () -> ModItems.COBALT_INGOT, () -> ModBlocks.COBALT_BLOCK.asItem()),
    ARDITE("ardite", "Molten Ardite", 0xFFBF3715, () -> ModItems.ARDITE_NUGGET, () -> ModItems.ARDITE_INGOT, () -> ModBlocks.ARDITE_BLOCK.asItem()),
    MANYULLYN("manyullyn", "Molten Manyullyn", 0xFF9E2BB5, () -> ModItems.MANYULLYN_NUGGET, () -> ModItems.MANYULLYN_INGOT, () -> ModBlocks.MANYULLYN_BLOCK.asItem()),
    TUNGSTEN("tungsten", "Molten Tungsten", 0xFF353C42, () -> ModItems.TUNGSTEN_NUGGET, () -> ModItems.TUNGSTEN_INGOT, () -> ModBlocks.TUNGSTEN_BLOCK.asItem()),
    NETHERITE("netherite", "Molten Netherite", 0xFF4A3C42, () -> Items.NETHERITE_SCRAP, () -> Items.NETHERITE_INGOT, () -> Items.NETHERITE_BLOCK);

    private final String id;
    private final String displayName;
    private final int color;
    private final @Nullable Supplier<Item> nuggetSupplier;
    private final @Nullable Supplier<Item> ingotSupplier;
    private final @Nullable Supplier<Item> blockSupplier;

    MoltenMetal(String id, String displayName, int color,
                @Nullable Supplier<Item> nuggetSupplier,
                @Nullable Supplier<Item> ingotSupplier,
                @Nullable Supplier<Item> blockSupplier) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.nuggetSupplier = nuggetSupplier;
        this.ingotSupplier = ingotSupplier;
        this.blockSupplier = blockSupplier;
    }

    @Override
    public String asString() {
        return this.id;
    }

    public String getId() {
        return this.id;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public int getColor() {
        return this.color;
    }

    public @Nullable Item getNuggetItem() {
        return nuggetSupplier != null ? nuggetSupplier.get() : null;
    }

    public @Nullable Item getIngotItem() {
        return ingotSupplier != null ? ingotSupplier.get() : null;
    }

    public @Nullable Item getBlockItem() {
        return blockSupplier != null ? blockSupplier.get() : null;
    }

    public static MoltenMetal fromId(String id) {
        for (MoltenMetal metal : values()) {
            if (metal.id.equalsIgnoreCase(id)) {
                return metal;
            }
        }
        return NONE;
    }

    public static @Nullable MoltenMetal fromItem(net.minecraft.item.ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.isOf(Items.LAVA_BUCKET)) return LAVA;
        if (stack.isOf(Items.RAW_IRON) || stack.isOf(Items.RAW_IRON_BLOCK)) return IRON;
        if (stack.isOf(Items.RAW_GOLD) || stack.isOf(Items.RAW_GOLD_BLOCK)) return GOLD;
        if (stack.isOf(Items.RAW_COPPER) || stack.isOf(Items.RAW_COPPER_BLOCK)) return COPPER;

        Item item = stack.getItem();
        for (MoltenMetal metal : values()) {
            if (metal == NONE || metal == LAVA) continue;
            if (metal.getIngotItem() != null && item == metal.getIngotItem()) return metal;
            if (metal.getNuggetItem() != null && item == metal.getNuggetItem()) return metal;
            if (metal.getBlockItem() != null && item == metal.getBlockItem()) return metal;
        }
        return null;
    }
}
