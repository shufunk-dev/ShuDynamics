package net.enchantedwood.item.custom;

import net.enchantedwood.block.ModBlocks;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

public class MysteryKeystoneItem extends Item {
    private static final Map<Player, Integer> RESONANCE_COUNTERS = new WeakHashMap<>();

    public MysteryKeystoneItem(Properties settings) {
        super(settings);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, @Nullable EquipmentSlot slot) {
        if (!(entity instanceof Player player)) return;

        boolean isHeld = slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND;

        if (isHeld) {
            // Apply cosmic dissonance status effects
            if (!player.hasEffect(MobEffects.NAUSEA)) {
                player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 90, 0, false, false, true));
            }
            if (!player.hasEffect(MobEffects.DARKNESS)) {
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 70, 0, false, false, true));
            }
            if (!player.hasEffect(MobEffects.LEVITATION) && player.tickCount % 30 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 35, 0, false, false, false));
            }

            // Play ambient spatial hum every second
            if (player.tickCount % 20 == 0) {
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.6f, 1.8f);
                player.sendOverlayMessage(Component.literal("§d✦ The spatial anomaly resonates with your consciousness... Reality bends around you!"));
            }

            int currentTicks = RESONANCE_COUNTERS.getOrDefault(player, 0) + 1;
            RESONANCE_COUNTERS.put(player, currentTicks);

            // Spawn mysterious void particles around player
            world.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    player.getX(), player.getY() + 1.0, player.getZ(), 4, 0.4, 0.4, 0.4, 0.05);

            // After 60 ticks (3 seconds) of holding, stabilize the anomaly!
            if (currentTicks >= 60) {
                RESONANCE_COUNTERS.remove(player);

                world.sendParticles(ParticleTypes.END_ROD,
                        player.getX(), player.getY() + 1.0, player.getZ(), 35, 0.5, 0.5, 0.5, 0.1);
                world.sendParticles(ParticleTypes.PORTAL,
                        player.getX(), player.getY() + 1.0, player.getZ(), 50, 0.6, 0.6, 0.6, 0.5);

                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1.0f, 1.2f);

                // Transform this stack into Keystone #6: Dimensional Singularity
                int count = stack.getCount();
                ItemStack stabilized = new ItemStack(ModBlocks.DIMENSIONAL_SINGULARITY);

                if (count > 1) {
                    stack.shrink(1);
                    if (!player.getInventory().add(stabilized)) {
                        player.drop(stabilized, false, net.minecraft.util.Prediction.SERVER_ONLY);
                    }
                } else {
                    if (slot == EquipmentSlot.MAINHAND) {
                        player.setItemInHand(InteractionHand.MAIN_HAND, stabilized);
                    } else if (slot == EquipmentSlot.OFFHAND) {
                        player.setItemInHand(InteractionHand.OFF_HAND, stabilized);
                    } else {
                        player.getInventory().placeItemBackInInventory(stabilized, net.minecraft.util.Prediction.SERVER_ONLY);
                    }
                }

                player.sendSystemMessage(Component.literal("§5✦ §dThe spatial anomaly has attuned to your vital frequency and stabilized into §eKeystone #6: Dimensional Singularity§d!"));
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
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        textConsumer.accept(Component.literal("§5✦ Volatile Spatial Rupture"));
        textConsumer.accept(Component.literal("§7Radiating chaotic gravitational waves from deep subterranean bedrock."));
        textConsumer.accept(Component.literal(""));
        textConsumer.accept(Component.literal("§e▶ Physical Exposure:"));
        textConsumer.accept(Component.literal("§7Holding this anomaly barehanded will attune its frequency to your body."));
        textConsumer.accept(Component.literal("§b▶ Digital Synthesis:"));
        textConsumer.accept(Component.literal("§7Can be safely stabilized inside a Super Computer with containment materials."));
        super.appendHoverText(stack, context, displayComponent, textConsumer, type);
    }
}
