package net.enchantedwood.item.custom;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Consumer;

public class SuitModuleItem extends Item {
    private final String moduleType;
    private final List<Text> descriptions;

    public SuitModuleItem(Settings settings, String moduleType, List<Text> descriptions) {
        super(settings.maxCount(16).fireproof());
        this.moduleType = moduleType;
        this.descriptions = descriptions;
    }

    public String getModuleType() {
        return this.moduleType;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        textConsumer.accept(Text.literal("§6⚡ Suit Upgrade Module: §f" + this.moduleType));
        for (Text desc : this.descriptions) {
            textConsumer.accept(desc);
        }
        textConsumer.accept(Text.literal("§8Install into Modular Power Suit via Access Panel (V) or Powered Anvil"));
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }
}
