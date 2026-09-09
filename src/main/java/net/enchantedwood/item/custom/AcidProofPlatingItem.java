package net.enchantedwood.item.custom;

import net.enchantedwood.effect.ModStatusEffects;
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

public class AcidProofPlatingItem extends Item {
    public AcidProofPlatingItem(Settings settings) {
        super(settings);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, @Nullable EquipmentSlot slot) {
        if (entity instanceof PlayerEntity player) {
            // Grants continuous Acid Protection & cleanses corrosive debuffs while carried in inventory or installed
            player.addStatusEffect(new StatusEffectInstance(ModStatusEffects.ACID_PROTECTION, 60, 0, true, false, true));
            if (player.hasStatusEffect(StatusEffects.POISON)) {
                player.removeStatusEffect(StatusEffects.POISON);
            }
            if (player.hasStatusEffect(StatusEffects.WITHER)) {
                player.removeStatusEffect(StatusEffects.WITHER);
            }
            if (player.hasStatusEffect(StatusEffects.NAUSEA)) {
                player.removeStatusEffect(StatusEffects.NAUSEA);
            }

            if (world.getTime() % 20 == 0) {
                world.spawnParticles(
                        ParticleTypes.HAPPY_VILLAGER,
                        player.getX(), player.getY() + 0.5, player.getZ(),
                        1, 0.2, 0.3, 0.2, 0.01
                );
            }
        }
        super.inventoryTick(stack, world, entity, slot);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        textConsumer.accept(Text.literal("§a✦ Fluoropolymer Acid-Proof Matrix"));
        textConsumer.accept(Text.literal("§7Grants §ePermanent Acid & Toxin Immunity §7while in inventory or suit."));
        textConsumer.accept(Text.literal("§8 • 100% Immunity to Poison, Wither, and Caustic Acid pools"));
        textConsumer.accept(Text.literal("§8 • Installable into Modular Power Suit via Access Panel (V)"));
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }
}
