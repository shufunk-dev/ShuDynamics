package net.enchantedwood.item.custom;

import net.minecraft.block.Block;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import java.util.List;
import java.util.function.Consumer;

public class TooltipBlockItem extends BlockItem {
    private final List<Text> tooltips;

    public TooltipBlockItem(Block block, Settings settings, Text... tooltips) {
        super(block, settings);
        this.tooltips = List.of(tooltips);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        net.minecraft.component.type.NbtComponent nbtComponent = stack.get(net.minecraft.component.DataComponentTypes.CUSTOM_DATA);
        if (nbtComponent != null) {
            net.minecraft.nbt.NbtCompound nbt = nbtComponent.copyNbt();
            if (nbt.contains("boundX")) {
                int x = nbt.getInt("boundX").orElse(0);
                int y = nbt.getInt("boundY").orElse(0);
                int z = nbt.getInt("boundZ").orElse(0);
                String dim = nbt.getString("boundDimension").orElse("minecraft:overworld");
                String dimName = dim.contains("mining_dimension") ? "Mining Dimension" :
                        dim.contains("nether") ? "Nether" :
                        dim.contains("end") ? "The End" : "Overworld";
                textConsumer.accept(Text.literal("§6✔ Bound Network: §f(" + x + ", " + y + ", " + z + ") in " + dimName));
                textConsumer.accept(Text.literal("§a✨ Automatically reconnects when placed!"));
            }
        }
        for (Text line : tooltips) {
            textConsumer.accept(line);
        }
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }
}
