package net.enchantedwood.item.custom;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import java.util.List;
import java.util.function.Consumer;

public class TooltipItem extends Item {
    private final List<Text> tooltips;

    public TooltipItem(Settings settings, Text... tooltips) {
        super(settings);
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
