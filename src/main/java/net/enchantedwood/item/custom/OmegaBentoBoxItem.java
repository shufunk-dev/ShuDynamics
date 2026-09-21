package net.enchantedwood.item.custom;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.List;
import java.util.function.Consumer;

public class OmegaBentoBoxItem extends Item {

    public OmegaBentoBoxItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (user.getItemCooldownManager().isCoolingDown(stack)) {
            user.sendMessage(Text.literal("§eThe Omega Bento Box is absorbing dimensional energy..."), true);
            return ActionResult.PASS;
        }

        user.getItemCooldownManager().set(stack, 20 * 60); // 60-second recharge cooldown

        if (!world.isClient() && world instanceof ServerWorld serverWorld) {
            user.getHungerManager().add(20, 1.0f);

            // Extended Tri-Shield Protection (120s)
            user.addStatusEffect(new StatusEffectInstance(net.enchantedwood.effect.ModStatusEffects.ACID_PROTECTION, 20 * 120, 0));
            user.addStatusEffect(new StatusEffectInstance(net.enchantedwood.effect.ModStatusEffects.THERMAL_PROTECTION, 20 * 120, 0));
            user.addStatusEffect(new StatusEffectInstance(net.enchantedwood.effect.ModStatusEffects.ATMOSPHERIC_PROTECTION, 20 * 120, 0));

            // Survival Buffs
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 20 * 30, 2)); // Regen III
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 20 * 180, 3)); // Absorption IV
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 20 * 120, 1)); // Resistance II

            serverWorld.spawnParticles(ParticleTypes.HEART, user.getX(), user.getY() + 1.2, user.getZ(), 15, 0.5, 0.5, 0.5, 0.05);
            serverWorld.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, user.getX(), user.getY() + 1.0, user.getZ(), 25, 0.6, 0.6, 0.6, 0.1);
            serverWorld.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 1.2f, 1.0f);

            user.sendMessage(Text.literal("§6✦ Omega Sustenance Consumed! Tri-Shield & Divine Regen engaged! ✦"), true);
            return ActionResult.SUCCESS;
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        textConsumer.accept(Text.literal("§6✦ Omega Bento Box ✦"));
        textConsumer.accept(Text.literal("§7Forged by infusing the Eternal Bento Box with Primordial Catalysts."));
        textConsumer.accept(Text.literal("§e✦ Right-Click: §6Infinite Celestial Sustenance"));
        textConsumer.accept(Text.literal("§8 • Fully restores Hunger & Saturation"));
        textConsumer.accept(Text.literal("§8 • Grants Tri-Shield Protection (Acid, Thermal, Atmospheric) for 2:00"));
        textConsumer.accept(Text.literal("§8 • Grants Regen III (0:30), Absorption IV (3:00), Resistance II (2:00)"));
        textConsumer.accept(Text.literal("§8 • Accelerated Cooldown: 60 seconds"));
        textConsumer.accept(Text.literal("§b✦ Ultimate Organic Longevity Relic."));
    }
}
