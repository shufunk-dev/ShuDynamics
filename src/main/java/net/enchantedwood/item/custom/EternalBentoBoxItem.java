package net.enchantedwood.item.custom;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.function.Consumer;

public class EternalBentoBoxItem extends Item {

    public EternalBentoBoxItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    public static int getCourseMode(ItemStack stack) {
        if (stack.contains(DataComponentTypes.CUSTOM_DATA)) {
            NbtComponent comp = stack.get(DataComponentTypes.CUSTOM_DATA);
            if (comp != null) {
                return comp.copyNbt().getInt("CourseMode", 0);
            }
        }
        return 0;
    }

    public static void setCourseMode(ItemStack stack, int mode) {
        NbtCompound nbt = new NbtCompound();
        if (stack.contains(DataComponentTypes.CUSTOM_DATA)) {
            NbtComponent comp = stack.get(DataComponentTypes.CUSTOM_DATA);
            if (comp != null) {
                nbt = comp.copyNbt();
            }
        }
        nbt.putInt("CourseMode", mode);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    public static Text getCourseNotification(int mode) {
        return switch (mode) {
            case 1 -> Text.literal("§d✦ Course Selected: §aWasabi Combat Rush §7[Weakness Immunity, Haste II, +25% Spd]");
            case 2 -> Text.literal("§d✦ Course Selected: §6Honey Mochi Fortification §7[Kinetic Dampening, Resistance, Absorption]");
            default -> Text.literal("§d✦ Course Selected: §eFeast of the Colossus §7[Tri-Shield, Regen II, Absorption III]");
        };
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        // Shift + Right-Click: Cycle Courses
        if (user.isSneaking()) {
            int currentMode = getCourseMode(stack);
            int nextMode = (currentMode + 1) % 3;
            setCourseMode(stack, nextMode);

            if (!world.isClient()) {
                user.sendMessage(getCourseNotification(nextMode), true);
                world.playSound(null, user.getX(), user.getY(), user.getZ(),
                        SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.PLAYERS, 0.8f, 1.2f);
                world.playSound(null, user.getX(), user.getY(), user.getZ(),
                        SoundEvents.ITEM_BUNDLE_DROP_CONTENTS, SoundCategory.PLAYERS, 0.8f, 1.5f);
            }
            return ActionResult.SUCCESS;
        }

        // Normal Right-Click: Eat Active Course
        if (user.getItemCooldownManager().isCoolingDown(stack)) {
            user.sendMessage(Text.literal("§eThe Eternal Bento Box is gathering dimensional nourishment..."), true);
            return ActionResult.PASS;
        }

        user.getItemCooldownManager().set(stack, 20 * 180); // 3-minute recharge cooldown

        if (!world.isClient() && world instanceof ServerWorld serverWorld) {
            // Restore Hunger & Saturation
            user.getHungerManager().add(20, 1.0f);

            int mode = getCourseMode(stack);
            switch (mode) {
                case 1 -> {
                    // Course 2: Wasabi Combat Rush
                    user.removeStatusEffect(StatusEffects.WEAKNESS);
                    user.removeStatusEffect(StatusEffects.SLOWNESS);
                    user.removeStatusEffect(StatusEffects.MINING_FATIGUE);

                    user.addStatusEffect(new StatusEffectInstance(net.enchantedwood.effect.ModStatusEffects.ADRENALINE_RUSH, 20 * 300, 0));
                    user.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 20 * 300, 1)); // Haste II

                    serverWorld.spawnParticles(ParticleTypes.CRIT, user.getX(), user.getY() + 1.2, user.getZ(), 20, 0.4, 0.4, 0.4, 0.1);
                    serverWorld.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 1.0f, 1.0f);
                    serverWorld.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.9f, 1.5f);

                    user.sendMessage(Text.literal("§a✦ Wasabi Combat Rush Activated! Weakness Cleansed & Immunized (5:00) ✦"), true);
                }
                case 2 -> {
                    // Course 3: Honey Mochi Fortification
                    user.addStatusEffect(new StatusEffectInstance(net.enchantedwood.effect.ModStatusEffects.KINETIC_DAMPENING, 20 * 300, 0));
                    user.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 20 * 300, 0));
                    user.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 20 * 180, 1)); // Absorption II

                    serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER, user.getX(), user.getY() + 1.2, user.getZ(), 20, 0.4, 0.4, 0.4, 0.05);
                    serverWorld.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 1.0f, 1.0f);
                    serverWorld.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.0f, 1.2f);

                    user.sendMessage(Text.literal("§6✦ Honey Mochi Fortification Activated! Kinetic Dampening & Resistance (5:00) ✦"), true);
                }
                default -> {
                    // Course 1: Feast of the Colossus
                    user.addStatusEffect(new StatusEffectInstance(net.enchantedwood.effect.ModStatusEffects.ACID_PROTECTION, 20 * 60, 0));
                    user.addStatusEffect(new StatusEffectInstance(net.enchantedwood.effect.ModStatusEffects.THERMAL_PROTECTION, 20 * 60, 0));
                    user.addStatusEffect(new StatusEffectInstance(net.enchantedwood.effect.ModStatusEffects.ATMOSPHERIC_PROTECTION, 20 * 60, 0));

                    user.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 20 * 20, 1)); // Regen II
                    user.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 20 * 120, 2)); // Absorption III

                    serverWorld.spawnParticles(ParticleTypes.HEART, user.getX(), user.getY() + 1.2, user.getZ(), 8, 0.4, 0.4, 0.4, 0.05);
                    serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER, user.getX(), user.getY() + 1.0, user.getZ(), 15, 0.5, 0.5, 0.5, 0.05);
                    serverWorld.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 1.0f, 1.0f);
                    serverWorld.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 0.6f, 1.5f);

                    // 8% chance to uncover a pristine Haven Bloom music disc tucked inside the bento box!
                    if (world.getRandom().nextFloat() < 0.08f) {
                        ItemStack disc = new ItemStack(net.enchantedwood.item.ModItems.MUSIC_DISC_HAVEN_BLOOM);
                        if (!user.getInventory().insertStack(disc)) {
                            user.dropItem(disc, false);
                        }
                        user.sendMessage(Text.literal("§d✦ You found a hidden Music Disc (Haven Bloom) inside the Bento Box! ✦"), false);
                        serverWorld.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.8f, 1.4f);
                    }

                    user.sendMessage(Text.literal("§d✦ Nourished by the Eternal Bento Box! Full Tri-Shield Activated! ✦"), true);
                }
            }
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        textConsumer.accept(Text.literal("§5✦ Relic of the Resonance Colossus ✦"));
        textConsumer.accept(Text.literal("§dThe everlasting culinary cornucopia of The Convergence."));
        textConsumer.accept(Text.literal("§a✔ Infinite Use §7(Never consumed in inventory)"));

        int mode = getCourseMode(stack);
        String currentName = switch (mode) {
            case 1 -> "§aWasabi Combat Rush";
            case 2 -> "§6Honey Mochi Fortification";
            default -> "§eFeast of the Colossus";
        };
        textConsumer.accept(Text.literal("§e✦ Active Course: " + currentName));
        textConsumer.accept(Text.literal("§8[Shift + Right-Click to cycle courses]"));
        textConsumer.accept(Text.literal("§7Available Courses:"));
        textConsumer.accept(Text.literal("§8 1. §eFeast of the Colossus: §7Full Hunger, 60s Tri-Shield, Regen II, Absorption III"));
        textConsumer.accept(Text.literal("§8 2. §aWasabi Combat Rush: §75m Adrenaline Rush (+25% Atk Spd, Weakness/Slowness Immunity), 5m Haste II"));
        textConsumer.accept(Text.literal("§8 3. §6Honey Mochi Fortification: §75m Kinetic Dampening, Resistance, Absorption II"));
        textConsumer.accept(Text.literal("§8 • Recharge Time: 3 minutes"));
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }
}

