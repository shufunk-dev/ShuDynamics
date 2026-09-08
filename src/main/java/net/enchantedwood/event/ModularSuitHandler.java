package net.enchantedwood.event;

import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.ModularPowerArmorItem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

public class ModularSuitHandler {

    public static void register() {
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            for (ServerWorld world : server.getWorlds()) {
                for (ServerPlayerEntity player : world.getPlayers()) {
                    tickPlayerSuit(player, world);
                }
            }
        });
    }

    private static void tickPlayerSuit(ServerPlayerEntity player, ServerWorld world) {
        if (player.isSpectator()) return;

        tickHelmetModules(player, world);
        tickNaniteRepairs(player, world);
    }

    private static void tickHelmetModules(ServerPlayerEntity player, ServerWorld world) {
        ItemStack helmet = player.getEquippedStack(EquipmentSlot.HEAD);
        if (!helmet.isOf(ModItems.MODULAR_POWER_HELMET)) return;

        // Check Adaptive Night Vision HUD
        if (ModularPowerArmorItem.hasModule(helmet, "enchantedwood:night_vision_module")) {
            int lightLevel = world.getLightLevel(player.getBlockPos());
            if (lightLevel <= 6) {
                // In dark area: check battery power
                int storedEnergy = ModularPowerArmorItem.getStoredEnergy(helmet);
                if (storedEnergy >= 2) {
                    // Drain 2 FE every tick (40 FE / sec) continuously while active in dark
                    ModularPowerArmorItem.extractEnergy(helmet, 2);

                    boolean hadNightVision = player.hasStatusEffect(StatusEffects.NIGHT_VISION);
                    // Refresh with 240 ticks (12 seconds) so it never flickers
                    player.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.NIGHT_VISION,
                            240,
                            0,
                            false,
                            false,
                            true
                    ));

                    if (!hadNightVision) {
                        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE,
                                SoundCategory.PLAYERS,
                                0.4f, 1.8f);
                    }
                } else {
                    // Out of power: shut off HUD
                    StatusEffectInstance currentEffect = player.getStatusEffect(StatusEffects.NIGHT_VISION);
                    if (currentEffect != null && currentEffect.getDuration() <= 260) {
                        player.removeStatusEffect(StatusEffects.NIGHT_VISION);
                        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE,
                                SoundCategory.PLAYERS,
                                0.3f, 2.0f);
                    }
                }
            } else if (lightLevel >= 9) {
                // Bright area: power down HUD to conserve power
                StatusEffectInstance currentEffect = player.getStatusEffect(StatusEffects.NIGHT_VISION);
                if (currentEffect != null && currentEffect.getDuration() <= 260) {
                    player.removeStatusEffect(StatusEffects.NIGHT_VISION);
                    world.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE,
                            SoundCategory.PLAYERS,
                            0.3f, 2.0f);
                }
            }
        }
    }

    private static void tickNaniteRepairs(ServerPlayerEntity player, ServerWorld world) {
        long lastDamage = PlayerHealthHandler.getLastDamageTime(player.getUuid());
        if (System.currentTimeMillis() - lastDamage < 10_000L) return; // Must be out of combat for 10s

        if (world.getTime() % 40 != 0) return; // Tick every 2 seconds

        EquipmentSlot[] armorSlots = new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        };

        for (EquipmentSlot slot : armorSlots) {
            ItemStack piece = player.getEquippedStack(slot);
            if (piece.getItem() instanceof ModularPowerArmorItem && piece.isDamaged()) {
                if (ModularPowerArmorItem.hasModule(piece, "enchantedwood:nanite_repair_matrix")) {
                    int energy = ModularPowerArmorItem.getStoredEnergy(piece);
                    if (energy >= 100) {
                        ModularPowerArmorItem.extractEnergy(piece, 100);
                        piece.setDamage(Math.max(0, piece.getDamage() - 1));
                        world.spawnParticles(
                                ParticleTypes.ELECTRIC_SPARK,
                                player.getX(), player.getY() + 1.0, player.getZ(),
                                2, 0.2, 0.3, 0.2, 0.05
                        );
                    }
                }
            }
        }
    }
}
