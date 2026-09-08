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
        textConsumer.accept(Text.literal("§a• Out-of-Combat Auto-Repair §8(activates after 10s of safety)"));
        textConsumer.accept(Text.literal("§e• Drains 100 FE / durability point §7from piece's installed battery"));
        textConsumer.accept(Text.literal("§8Install into any Modular Power Armor piece via Suit Access Panel (V) or Powered Anvil"));
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }
}
