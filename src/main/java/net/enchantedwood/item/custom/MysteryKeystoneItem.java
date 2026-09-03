package net.enchantedwood.item.custom;

import net.enchantedwood.block.ModBlocks;
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
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

public class MysteryKeystoneItem extends Item {
    private static final Map<PlayerEntity, Integer> RESONANCE_COUNTERS = new WeakHashMap<>();

    public MysteryKeystoneItem(Settings settings) {
        super(settings);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, @Nullable EquipmentSlot slot) {
        if (!(entity instanceof PlayerEntity player)) return;

        boolean isHeld = slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND;

        if (isHeld) {
            // Apply cosmic dissonance status effects
            if (!player.hasStatusEffect(StatusEffects.NAUSEA)) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 90, 0, false, false, true));
            }
            if (!player.hasStatusEffect(StatusEffects.DARKNESS)) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 70, 0, false, false, true));
            }
            if (!player.hasStatusEffect(StatusEffects.LEVITATION) && player.age % 30 == 0) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 35, 0, false, false, false));
            }

            // Play ambient spatial hum every second
            if (player.age % 20 == 0) {
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLOCK_BEACON_AMBIENT, SoundCategory.PLAYERS, 0.6f, 1.8f);
                player.sendMessage(Text.literal("§d✦ The spatial anomaly resonates with your consciousness... Reality bends around you!"), true);
            }

            int currentTicks = RESONANCE_COUNTERS.getOrDefault(player, 0) + 1;
            RESONANCE_COUNTERS.put(player, currentTicks);

            // Spawn mysterious void particles around player
            world.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                    player.getX(), player.getY() + 1.0, player.getZ(), 4, 0.4, 0.4, 0.4, 0.05);

            // After 60 ticks (3 seconds) of holding, stabilize the anomaly!
            if (currentTicks >= 60) {
                RESONANCE_COUNTERS.remove(player);

                world.spawnParticles(ParticleTypes.END_ROD,
                        player.getX(), player.getY() + 1.0, player.getZ(), 35, 0.5, 0.5, 0.5, 0.1);
                world.spawnParticles(ParticleTypes.PORTAL,
                        player.getX(), player.getY() + 1.0, player.getZ(), 50, 0.6, 0.6, 0.6, 0.5);

                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.PLAYERS, 1.0f, 1.2f);

                // Transform this stack into Keystone #6: Dimensional Singularity
                int count = stack.getCount();
                ItemStack stabilized = new ItemStack(ModBlocks.DIMENSIONAL_SINGULARITY);

                if (count > 1) {
                    stack.decrement(1);
                    if (!player.getInventory().insertStack(stabilized)) {
                        player.dropItem(stabilized, false);
                    }
                } else {
                    if (slot == EquipmentSlot.MAINHAND) {
                        player.setStackInHand(Hand.MAIN_HAND, stabilized);
                    } else if (slot == EquipmentSlot.OFFHAND) {
                        player.setStackInHand(Hand.OFF_HAND, stabilized);
                    } else {
                        player.getInventory().offerOrDrop(stabilized);
                    }
                }

                player.sendMessage(Text.literal("§5✦ §dThe spatial anomaly has attuned to your vital frequency and stabilized into §eKeystone #6: Dimensional Singularity§d!"), false);
            }
        } else {
            if (RESONANCE_COUNTERS.containsKey(player)) {
                // Decay resonance counter if player stops holding the item
                int current = RESONANCE_COUNTERS.get(player);
                if (current > 0) {
                    RESONANCE_COUNTERS.put(player, Math.max(0, current - 2));
                } else {
                    RESONANCE_COUNTERS.remove(player);
                }
            }
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        textConsumer.accept(Text.literal("§5✦ Volatile Spatial Rupture"));
        textConsumer.accept(Text.literal("§7Radiating chaotic gravitational waves from deep subterranean bedrock."));
        textConsumer.accept(Text.literal(""));
        textConsumer.accept(Text.literal("§e▶ Physical Exposure:"));
        textConsumer.accept(Text.literal("§7Holding this anomaly barehanded will attune its frequency to your body."));
        textConsumer.accept(Text.literal("§b▶ Digital Synthesis:"));
        textConsumer.accept(Text.literal("§7Can be safely stabilized inside a Super Computer with containment materials."));
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }
}
