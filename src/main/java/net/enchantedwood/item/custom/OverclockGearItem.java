package net.enchantedwood.item.custom;

import java.util.List;
import java.util.function.Consumer;
import net.enchantedwood.block.custom.GearTier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class OverclockGearItem extends GearItem {
    private final List<Component> tooltips;

    public OverclockGearItem(GearTier gearTier, boolean enchanted, Properties settings, Component... tooltips) {
        super(gearTier, enchanted, settings);
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
