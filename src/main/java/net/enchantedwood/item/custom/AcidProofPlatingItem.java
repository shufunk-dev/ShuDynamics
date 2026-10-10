package net.enchantedwood.item.custom;

import net.enchantedwood.effect.ModStatusEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class AcidProofPlatingItem extends Item {
    public AcidProofPlatingItem(Properties settings) {
        super(settings);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        textConsumer.accept(Component.literal("§6⚡ Suit Upgrade Module"));
        textConsumer.accept(Component.literal("§a✦ Fluoropolymer Acid-Proof Matrix"));
        textConsumer.accept(Component.literal("§7Active chemical neutralizer for Modular Power Exosuit."));
        textConsumer.accept(Component.literal("§b• 100% Acid, Poison, Wither & Nausea Immunity §8(when installed)"));
        textConsumer.accept(Component.literal("§e• Active Drain: §f4 FE / tick §7under caustic exposure (0 FE idle)"));
        textConsumer.accept(Component.literal("§8Install into Chestplate or Leggings via Access Panel (V)"));
        super.appendHoverText(stack, context, displayComponent, textConsumer, type);
    }
}
