package net.enchantedwood.item.custom;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class NaniteRepairMatrixItem extends Item {
    public NaniteRepairMatrixItem(Settings settings) {
        super(settings.maxCount(16).fireproof());
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        textConsumer.accept(Text.literal("§6⚡ Suit Upgrade Module"));
        textConsumer.accept(Text.literal("§7Microscopic autonomous nanites engineered for rapid chassis regeneration."));
        textConsumer.accept(Text.literal("§b• Global Suit Network: §7Auto-repairs ALL equipped modular armor pieces"));
        textConsumer.accept(Text.literal("§a• Out-of-Combat Protocol §8(activates after 10s of safety)"));
        textConsumer.accept(Text.literal("§e• Drains 50 FE / durability point §7(accelerated by Logic Chips)"));
        textConsumer.accept(Text.literal("§8Install into ANY Modular Power Armor piece via Suit Access Panel (V)"));
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }
}
