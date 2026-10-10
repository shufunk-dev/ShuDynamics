package net.enchantedwood.item.custom;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class TooltipItem extends Item {
    private final List<Component> tooltips;

    public TooltipItem(Properties settings, Component... tooltips) {
        super(settings);
        this.tooltips = List.of(tooltips);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        for (Component line : tooltips) {
            textConsumer.accept(line);
        }
        super.appendHoverText(stack, context, displayComponent, textConsumer, type);
    }
}
