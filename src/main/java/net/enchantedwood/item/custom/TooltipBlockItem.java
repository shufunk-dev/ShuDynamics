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
        for (Text line : tooltips) {
            textConsumer.accept(line);
        }
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }
}
