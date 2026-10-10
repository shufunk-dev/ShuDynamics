package net.enchantedwood.item.custom;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class NaniteRepairMatrixItem extends Item {
    public NaniteRepairMatrixItem(Properties settings) {
        super(settings.stacksTo(16).fireResistant());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        textConsumer.accept(Component.literal("§6⚡ Suit Upgrade Module"));
        textConsumer.accept(Component.literal("§7Microscopic autonomous nanites engineered for rapid chassis regeneration."));
        textConsumer.accept(Component.literal("§b• Global Suit Network: §7Auto-repairs ALL equipped modular armor pieces"));
        textConsumer.accept(Component.literal("§a• Out-of-Combat Protocol §8(activates after 10s of safety)"));
        textConsumer.accept(Component.literal("§e• Drains 50 FE / durability point §7(accelerated by Logic Chips)"));
        textConsumer.accept(Component.literal("§8Install into ANY Modular Power Armor piece via Suit Access Panel (V)"));
        super.appendHoverText(stack, context, displayComponent, textConsumer, type);
    }
}
