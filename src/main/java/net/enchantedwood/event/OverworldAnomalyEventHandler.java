package net.enchantedwood.event;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.enchantedwood.block.ModBlocks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OverworldAnomalyEventHandler {

    public enum AnomalyType {
        ALTITUDE,
        GRAVITY,
        CHRONO,
        BEDROCK,
        PLASMA_FLARE
    }

    private static class ActiveAnomaly {
        final AnomalyType type;
        int ticksRemaining;

        ActiveAnomaly(AnomalyType type, int duration) {
            this.type = type;
            this.ticksRemaining = duration;
        }
    }

    private static final Map<UUID, ActiveAnomaly> ACTIVE_ANOMALIES = new HashMap<>();

    public static void register() {
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            for (ServerWorld world : server.getWorlds()) {
                boolean isOverworld = world.getRegistryKey() == World.OVERWORLD;
                boolean isNether = world.getRegistryKey() == World.NETHER;

                if (!isOverworld && !isNether) continue;

                for (ServerPlayerEntity player : world.getPlayers()) {
                    if (player.isCreative() || player.isSpectator()) continue;
                    tickPlayerAnomalies(world, player, isOverworld, isNether);
                }
            }
        });
    }

    private static void tickPlayerAnomalies(ServerWorld world, ServerPlayerEntity player, boolean isOverworld, boolean isNether) {
        UUID uuid = player.getUuid();

        // 1. Tick currently running anomaly
        if (ACTIVE_ANOMALIES.containsKey(uuid)) {
            ActiveAnomaly active = ACTIVE_ANOMALIES.get(uuid);
            active.ticksRemaining--;

            handleAnomalyStep(world, player, active.type, active.ticksRemaining);

            if (active.ticksRemaining <= 0) {
                concludeAnomaly(world, player, active.type);
                ACTIVE_ANOMALIES.remove(uuid);
            }
            return;
        }

        // 2. Check triggers only once every second (20 ticks)
        if (world.getTime() % 20 != (Math.abs(uuid.hashCode()) % 20)) return;

        // --- NETHER ANOMALY ---
        if (isNether) {
            // Solar Plasma Flare: Near lava sea level Y <= 40
            if (!player.getCommandTags().contains("sd_anomaly_plasma") && player.getY() <= 40) {
                if (world.random.nextFloat() < 0.08f) {
                    triggerAnomaly(world, player, AnomalyType.PLASMA_FLARE, 100); // 5 seconds
                    return;
                }
            }
            return;
        }

        // --- OVERWORLD ANOMALIES ---
        // Anomaly A: Altitude Collapse (Mountain peak Y >= 160 under open sky)
        if (!player.getCommandTags().contains("sd_anomaly_altitude") && player.getY() >= 160 && world.isSkyVisible(player.getBlockPos())) {
            if (world.random.nextFloat() < 0.10f) {
                triggerAnomaly(world, player, AnomalyType.ALTITUDE, 100); // 5 seconds
                return;
            }
        }

        // Anomaly B: Zero-G Gravitational Surge (Deep underground Y <= 0 or night surface)
        if (!player.getCommandTags().contains("sd_anomaly_gravity")) {
            boolean underground = player.getY() <= 0 && !world.isSkyVisible(player.getBlockPos());
            boolean nightSurface = world.isNight() && world.isSkyVisible(player.getBlockPos());
            if ((underground || nightSurface) && world.random.nextFloat() < 0.08f) {
                triggerAnomaly(world, player, AnomalyType.GRAVITY, 120); // 6 seconds
                return;
            }
        }

        // Anomaly C: Chrono-Static Pulse (Driving ATV or near industrial tech)
        if (!player.getCommandTags().contains("sd_anomaly_chrono")) {
            boolean isDriving = player.hasVehicle();
            boolean nearTech = isNearIndustrialTech(world, player.getBlockPos());
            if ((isDriving || nearTech) && world.random.nextFloat() < 0.06f) {
                triggerAnomaly(world, player, AnomalyType.CHRONO, 80); // 4 seconds
                return;
            }
        }

        // Anomaly D: Subterranean Void Tremor (Near Bedrock Y <= -50)
        if (!player.getCommandTags().contains("sd_anomaly_bedrock") && player.getY() <= -50) {
            if (world.random.nextFloat() < 0.08f) {
                triggerAnomaly(world, player, AnomalyType.BEDROCK, 80); // 4 seconds
            }
        }
    }

    private static boolean isNearIndustrialTech(ServerWorld world, BlockPos pos) {
        for (BlockPos check : BlockPos.iterate(pos.add(-4, -2, -4), pos.add(4, 2, 4))) {
            var state = world.getBlockState(check);
            if (state.isOf(ModBlocks.STEEL_BATTERY) || state.isOf(ModBlocks.TUNGSTEN_BATTERY) ||
                state.isOf(ModBlocks.COPPER_GENERATOR) || state.isOf(ModBlocks.ALUMINUM_GENERATOR) ||
                state.isOf(ModBlocks.STEEL_GENERATOR) || state.isOf(ModBlocks.SUPER_COMPUTER) ||
                state.isOf(ModBlocks.LASER_QUARRY)) {
                return true;
            }
        }
        return false;
    }

    private static void triggerAnomaly(ServerWorld world, ServerPlayerEntity player, AnomalyType type, int durationTicks) {
        ACTIVE_ANOMALIES.put(player.getUuid(), new ActiveAnomaly(type, durationTicks));

        switch (type) {
            case ALTITUDE -> {
                player.addCommandTag("sd_anomaly_altitude");
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.PLAYERS, 1.2f, 1.4f);
                player.sendMessage(Text.literal("§b❄ The atmosphere suddenly collapses into a vacuum... You struggle to breathe!"), true);
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 70, 0, false, false, false));
                player.setAir(Math.min(player.getAir(), 40));
            }
            case GRAVITY -> {
                player.addCommandTag("sd_anomaly_gravity");
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0f, 0.5f);
                player.sendMessage(Text.literal("§5🌀 Local gravity collapsed! You drift into zero-gravity..."), true);
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 70, 0, false, false, false));
            }
            case CHRONO -> {
                player.addCommandTag("sd_anomaly_chrono");
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ITEM_TRIDENT_THUNDER.value(), SoundCategory.PLAYERS, 0.8f, 1.8f);
                player.sendMessage(Text.literal("§e⚡ Chrono-Electromagnetic Surge! Instruments overloaded."), true);
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 60, 0, false, false, false));
            }
            case BEDROCK -> {
                player.addCommandTag("sd_anomaly_bedrock");
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENTITY_WARDEN_ROAR, SoundCategory.PLAYERS, 1.0f, 0.4f);
                player.sendMessage(Text.literal("§4👁 A colossal resonance echoes beneath the bedrock... Something stirs on the other side."), true);
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 70, 0, false, false, false));
            }
            case PLASMA_FLARE -> {
                player.addCommandTag("sd_anomaly_plasma");
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.2f, 0.6f);
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 1.0f, 0.5f);
                player.sendMessage(Text.literal("§6🔥 Solar Plasma Wave! Superheated extraterrestrial radiation washes over you..."), true);
                // Safe temporary fire resistance so player is never harmed
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 200, 0, false, false, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 100, 0, false, false, false));
            }
        }
    }

    private static void handleAnomalyStep(ServerWorld world, ServerPlayerEntity player, AnomalyType type, int remainingTicks) {
        switch (type) {
            case ALTITUDE -> {
                if (remainingTicks % 15 == 0) {
                    world.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.PLAYERS, 1.0f, 1.5f);
                    world.spawnParticles(ParticleTypes.SNOWFLAKE,
                            player.getX(), player.getY() + 1.2, player.getZ(), 6, 0.4, 0.3, 0.4, 0.02);
                }
                player.setAir(Math.min(player.getAir(), 20));
            }
            case GRAVITY -> {
                if (remainingTicks % 8 == 0) {
                    world.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                            player.getX(), player.getY() + 0.5, player.getZ(), 8, 0.4, 0.6, 0.4, 0.05);
                }
            }
            case CHRONO -> {
                if (remainingTicks % 10 == 0) {
                    world.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                            player.getX(), player.getY() + 1.0, player.getZ(), 5, 0.3, 0.4, 0.3, 0.1);
                }
            }
            case BEDROCK -> {
                if (remainingTicks % 20 == 0) {
                    world.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.BLOCK_RESPAWN_ANCHOR_AMBIENT, SoundCategory.BLOCKS, 1.2f, 0.5f);
                }
            }
            case PLASMA_FLARE -> {
                if (remainingTicks % 8 == 0) {
                    world.spawnParticles(ParticleTypes.FLAME,
                            player.getX(), player.getY() + 1.0, player.getZ(), 10, 0.5, 0.5, 0.5, 0.08);
                    world.spawnParticles(ParticleTypes.LAVA,
                            player.getX(), player.getY() + 0.5, player.getZ(), 3, 0.3, 0.2, 0.3, 0.02);
                }
            }
        }
    }

    private static void concludeAnomaly(ServerWorld world, ServerPlayerEntity player, AnomalyType type) {
        switch (type) {
            case ALTITUDE -> {
                player.setAir(300);
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENTITY_PLAYER_BREATH, SoundCategory.PLAYERS, 1.2f, 1.0f);
                player.sendMessage(Text.literal("§7...The air stabilizes. A temporary tear in the atmospheric layer?"), false);
            }
            case GRAVITY -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 160, 0, false, false, false));
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.PLAYERS, 0.7f, 1.6f);
                player.sendMessage(Text.literal("§dGravity snaps back into alignment. Something massive is bending spatial curvature from beyond."), false);
            }
            case CHRONO -> {
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 0.8f, 1.5f);
                player.sendMessage(Text.literal("§6The electromagnetic field quiets down. A rogue radio transmission leaked through reality."), false);
            }
            case BEDROCK -> {
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLOCK_BEACON_AMBIENT, SoundCategory.PLAYERS, 0.8f, 0.8f);
                player.sendMessage(Text.literal("§8...The tremors subside. Whatever it was has receded into the dark abyss."), false);
            }
            case PLASMA_FLARE -> {
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLOCK_LAVA_EXTINGUISH, SoundCategory.PLAYERS, 1.0f, 1.2f);
                player.sendMessage(Text.literal("§e...The thermal wave dissipates. A solar flare leaked through a rift from an alien star system."), false);
            }
        }
    }
}
