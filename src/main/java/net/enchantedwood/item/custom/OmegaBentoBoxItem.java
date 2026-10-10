package net.enchantedwood.item.custom;

import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class OmegaBentoBoxItem extends Item {

    public OmegaBentoBoxItem(Properties settings) {
        super(settings);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    public static int getCourseMode(ItemStack stack) {
        if (stack.has(DataComponents.CUSTOM_DATA)) {
            CustomData comp = stack.get(DataComponents.CUSTOM_DATA);
            if (comp != null) {
                return comp.copyTag().getIntOr("CourseMode", 0);
            }
        }
        return 0;
    }

    public static void setCourseMode(ItemStack stack, int mode) {
        CompoundTag nbt = new CompoundTag();
        if (stack.has(DataComponents.CUSTOM_DATA)) {
            CustomData comp = stack.get(DataComponents.CUSTOM_DATA);
            if (comp != null) {
                nbt = comp.copyTag();
            }
        }
        nbt.putInt("CourseMode", mode);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
    }

    public static Component getCourseNotification(int mode) {
        return switch (mode) {
            case 1 -> Component.literal("§6✦ Omega Course: §aWasabi Combat Rush §7[Weakness Immunity, Strength II, Haste II]");
            case 2 -> Component.literal("§6✦ Omega Course: §6Honey Mochi Fortification §7[Kinetic Dampening, Resistance III, Absorption IV]");
            default -> Component.literal("§6✦ Omega Course: §eOmega Celestial Feast §7[Tri-Shield 2m, Regen III, Resistance II]");
        };
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);

        // Shift + Right-Click: Cycle Courses
        if (user.isShiftKeyDown()) {
            int currentMode = getCourseMode(stack);
            int nextMode = (currentMode + 1) % 3;
            setCourseMode(stack, nextMode);

            if (!world.isClientSide()) {
                user.sendSystemMessage(getCourseNotification(nextMode));
                world.playSound(null, user.getX(), user.getY(), user.getZ(),
                        SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.8f, 1.2f);
                world.playSound(null, user.getX(), user.getY(), user.getZ(),
                        SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.PLAYERS, 0.8f, 1.5f);
            }
            return InteractionResult.SUCCESS;
        }

        // Normal Right-Click: Eat Active Course
        if (user.getCooldowns().isOnCooldown(stack)) {
            user.sendOverlayMessage(Component.literal("§eThe Omega Bento Box is absorbing dimensional energy..."));
            return InteractionResult.PASS;
        }

        user.getCooldowns().addCooldown(stack, 20 * 60); // Accelerated 60-second recharge cooldown

        if (!world.isClientSide() && world instanceof ServerLevel serverWorld) {
            // Restore Hunger & Saturation
            user.getFoodData().eat(20, 1.0f);

            int mode = getCourseMode(stack);
            switch (mode) {
                case 1 -> {
                    // Course 2: Omega Wasabi Combat Rush
                    user.removeEffect(MobEffects.WEAKNESS);
                    user.removeEffect(MobEffects.SLOWNESS);
                    user.removeEffect(MobEffects.MINING_FATIGUE);
                    user.removeEffect(MobEffects.WITHER);
                    user.removeEffect(MobEffects.POISON);

                    user.addEffect(new MobEffectInstance(net.enchantedwood.effect.ModStatusEffects.ADRENALINE_RUSH, 20 * 300, 0));
                    user.addEffect(new MobEffectInstance(MobEffects.HASTE, 20 * 300, 1)); // Haste II
                    user.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 20 * 180, 1)); // Strength II

                    serverWorld.sendParticles(ParticleTypes.CRIT, user.getX(), user.getY() + 1.2, user.getZ(), 25, 0.5, 0.5, 0.5, 0.1);
                    serverWorld.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.2f, 1.0f);
                    serverWorld.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.5f);

                    user.sendOverlayMessage(Component.literal("§a✦ Omega Wasabi Combat Rush! Weakness Cleansed & Immunized (5:00) + Strength II ✦"));
                }
                case 2 -> {
                    // Course 3: Omega Honey Mochi Fortification
                    user.addEffect(new MobEffectInstance(net.enchantedwood.effect.ModStatusEffects.KINETIC_DAMPENING, 20 * 300, 0));
                    user.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20 * 180, 2)); // Resistance III
                    user.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 180, 3)); // Absorption IV

                    serverWorld.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, user.getX(), user.getY() + 1.2, user.getZ(), 25, 0.5, 0.5, 0.5, 0.1);
                    serverWorld.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.2f, 1.0f);
                    serverWorld.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 1.2f);

                    user.sendOverlayMessage(Component.literal("§6✦ Omega Honey Mochi Fortification! Resistance III & Absorption IV (3:00) ✦"));
                }
                default -> {
                    // Course 1: Omega Celestial Feast
                    user.addEffect(new MobEffectInstance(net.enchantedwood.effect.ModStatusEffects.ACID_PROTECTION, 20 * 120, 0));
                    user.addEffect(new MobEffectInstance(net.enchantedwood.effect.ModStatusEffects.THERMAL_PROTECTION, 20 * 120, 0));
                    user.addEffect(new MobEffectInstance(net.enchantedwood.effect.ModStatusEffects.ATMOSPHERIC_PROTECTION, 20 * 120, 0));

                    user.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 30, 2)); // Regen III
                    user.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 180, 3)); // Absorption IV
                    user.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20 * 120, 1)); // Resistance II

                    serverWorld.sendParticles(ParticleTypes.HEART, user.getX(), user.getY() + 1.2, user.getZ(), 15, 0.5, 0.5, 0.5, 0.05);
                    serverWorld.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, user.getX(), user.getY() + 1.0, user.getZ(), 25, 0.6, 0.6, 0.6, 0.1);
                    serverWorld.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.2f, 1.0f);
                    serverWorld.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8f, 1.4f);

                    // 15% chance to uncover a pristine Haven Bloom music disc
                    if (world.getRandom().nextFloat() < 0.15f) {
                        ItemStack disc = new ItemStack(net.enchantedwood.item.ModItems.MUSIC_DISC_HAVEN_BLOOM);
                        if (!user.getInventory().add(disc)) {
                            user.drop(disc, false, net.minecraft.util.Prediction.SERVER_ONLY);
                        }
                        user.sendSystemMessage(Component.literal("§d✦ You found a hidden Music Disc (Haven Bloom) inside the Omega Bento Box! ✦"));
                        serverWorld.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.4f);
                    }

                    user.sendSystemMessage(Component.literal("§6✦ Omega Sustenance Consumed! Extended Tri-Shield & Divine Regen engaged! ✦"));
                }
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        textConsumer.accept(Component.literal("§6✦ Omega Bento Box ✦"));
        textConsumer.accept(Component.literal("§7Forged by infusing the Eternal Bento Box with Primordial Catalysts."));
        textConsumer.accept(Component.literal("§a✔ Infinite Celestial Sustenance §7(Never consumed)"));

        int mode = getCourseMode(stack);
        String currentName = switch (mode) {
            case 1 -> "§aWasabi Combat Rush";
            case 2 -> "§6Honey Mochi Fortification";
            default -> "§eOmega Celestial Feast";
        };
        textConsumer.accept(Component.literal("§e✦ Active Course: " + currentName));
        textConsumer.accept(Component.literal("§8[Shift + Right-Click to cycle courses]"));
        textConsumer.accept(Component.literal("§7Available Courses:"));
        textConsumer.accept(Component.literal("§8 1. §eOmega Celestial Feast: §7Full Hunger, 2m Tri-Shield, Regen III, Resistance II, Absorption IV"));
        textConsumer.accept(Component.literal("§8 2. §aWasabi Combat Rush: §75m Adrenaline Rush (Weakness/Debuff Immunity), Strength II, Haste II"));
        textConsumer.accept(Component.literal("§8 3. §6Honey Mochi Fortification: §75m Kinetic Dampening, 3m Resistance III, Absorption IV"));
        textConsumer.accept(Component.literal("§8 • Accelerated Cooldown: 60 seconds"));
        textConsumer.accept(Component.literal("§b✦ Ultimate Organic Longevity Relic."));
    }
}
