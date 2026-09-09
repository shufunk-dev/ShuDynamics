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

public class EternalBentoBoxItem extends Item {

    public EternalBentoBoxItem(Settings settings) {
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
            user.sendMessage(Text.literal("§eThe Eternal Bento Box is gathering dimensional nourishment..."), true);
            return ActionResult.PASS;
        }

        user.getItemCooldownManager().set(stack, 20 * 180); // 3-minute recharge cooldown

        if (!world.isClient() && world instanceof ServerWorld serverWorld) {
            // Restore Hunger & Saturation
            user.getHungerManager().add(20, 1.0f);

            // Active Tri-Shield Protection (60s)
            user.addStatusEffect(new StatusEffectInstance(net.enchantedwood.effect.ModStatusEffects.ACID_PROTECTION, 20 * 60, 0));
            user.addStatusEffect(new StatusEffectInstance(net.enchantedwood.effect.ModStatusEffects.THERMAL_PROTECTION, 20 * 60, 0));
            user.addStatusEffect(new StatusEffectInstance(net.enchantedwood.effect.ModStatusEffects.ATMOSPHERIC_PROTECTION, 20 * 60, 0));

            // Survival Buffs
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 20 * 20, 1)); // Regen II
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 20 * 120, 2)); // Absorption III

            // Sound & Particles
            serverWorld.spawnParticles(ParticleTypes.HEART, user.getX(), user.getY() + 1.2, user.getZ(), 8, 0.4, 0.4, 0.4, 0.05);
            serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER, user.getX(), user.getY() + 1.0, user.getZ(), 15, 0.5, 0.5, 0.5, 0.05);
            serverWorld.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 1.0f, 1.0f);
            serverWorld.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 0.6f, 1.5f);

            user.sendMessage(Text.literal("§d✦ Nourished by the Eternal Bento Box! Full Tri-Shield Activated! ✦"), true);
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        textConsumer.accept(Text.literal("§5✦ Relic of the Resonance Colossus ✦"));
        textConsumer.accept(Text.literal("§dThe everlasting culinary cornucopia of The Convergence."));
        textConsumer.accept(Text.literal("§a✔ Infinite Use §7(Never consumed in inventory)"));
        textConsumer.accept(Text.literal("§e✦ Right-Click: §fFeast of the Colossus"));
        textConsumer.accept(Text.literal("§8 • Instantly fills Hunger and Saturation to maximum"));
        textConsumer.accept(Text.literal("§8 • Grants 60s Tri-Shield: Acid, Thermal & Atmospheric Protection"));
        textConsumer.accept(Text.literal("§8 • Grants Regeneration II (0:20) + Absorption III (2:00)"));
        textConsumer.accept(Text.literal("§8 • Recharge Time: 3 minutes"));
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }
}
