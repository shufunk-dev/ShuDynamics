package net.enchantedwood.item.custom;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ThermalRefractoryPlatingItem extends Item {
    public ThermalRefractoryPlatingItem(Properties settings) {
        super(settings);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        textConsumer.accept(Component.literal("§6⚡ Suit Upgrade Module"));
        textConsumer.accept(Component.literal("§6✦ Nether Thermal Refractory Matrix"));
        textConsumer.accept(Component.literal("§7Active cryo-thermal heatsink for Modular Power Exosuit."));
        textConsumer.accept(Component.literal("§c• 100% Fire & Lava Immunity + Lava Surfing §8(when installed)"));
        textConsumer.accept(Component.literal("§e• Active Drain: §f5 FE / tick §7in lava, fire or caldera (0 FE idle)"));
        textConsumer.accept(Component.literal("§8Install into Chestplate or Leggings via Access Panel (V)"));
        super.appendHoverText(stack, context, displayComponent, textConsumer, type);
    }
}
