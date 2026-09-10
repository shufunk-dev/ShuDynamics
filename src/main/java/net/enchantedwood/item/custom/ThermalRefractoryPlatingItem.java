package net.enchantedwood.item.custom;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class ThermalRefractoryPlatingItem extends Item {
    public ThermalRefractoryPlatingItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        textConsumer.accept(Text.literal("§6⚡ Suit Upgrade Module"));
        textConsumer.accept(Text.literal("§6✦ Nether Thermal Refractory Matrix"));
        textConsumer.accept(Text.literal("§7Active cryo-thermal heatsink for Modular Power Exosuit."));
        textConsumer.accept(Text.literal("§c• 100% Fire & Lava Immunity + Lava Surfing §8(when installed)"));
        textConsumer.accept(Text.literal("§e• Active Drain: §f5 FE / tick §7in lava, fire or caldera (0 FE idle)"));
        textConsumer.accept(Text.literal("§8Install into Chestplate or Leggings via Access Panel (V)"));
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }
}
