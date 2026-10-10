package net.enchantedwood.event;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
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
            for (ServerLevel world : server.getAllLevels()) {
                boolean isOverworld = world.dimension() == Level.OVERWORLD;
                boolean isNether = world.dimension() == Level.NETHER;

                if (!isOverworld && !isNether) continue;

                for (ServerPlayer player : world.players()) {
                    if (player.isSpectator()) continue;
                    tickPlayerAnomalies(world, player, isOverworld, isNether);
                }
            }
        });
    }

    private static void tickPlayerAnomalies(ServerLevel world, ServerPlayer player, boolean isOverworld, boolean isNether) {
        UUID uuid = player.getUUID();

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
        if (world.getGameTime() % 20 != (Math.abs(uuid.hashCode()) % 20)) return;

        // --- NETHER ANOMALY ---
        if (isNether) {
            // Solar Plasma Flare: Near lava sea level Y <= 40
            boolean needsPlasma = !player.entityTags().contains("sd_anomaly_plasma") || !hasAdvancement(world, player, "anomalies/anomaly_plasma");
            if (needsPlasma && player.getY() <= 40) {
                if (world.getRandom().nextFloat() < 0.40f) {
                    triggerAnomaly(world, player, AnomalyType.PLASMA_FLARE, 100); // 5 seconds
                    return;
                }
            }
            return;
        }

        // --- OVERWORLD ANOMALIES ---
        // Anomaly A: Altitude Collapse (Mountain peak Y >= 160 under open sky)
        boolean needsAltitude = !player.entityTags().contains("sd_anomaly_altitude") || !hasAdvancement(world, player, "anomalies/anomaly_altitude");
        if (needsAltitude && player.getY() >= 160 && world.canSeeSky(player.blockPosition())) {
            if (world.getRandom().nextFloat() < 0.40f) {
                triggerAnomaly(world, player, AnomalyType.ALTITUDE, 100); // 5 seconds
                return;
            }
        }

        // Anomaly B: Zero-G Gravitational Surge (Deep underground Y <= 0 or night surface)
        boolean needsGravity = !player.entityTags().contains("sd_anomaly_gravity") || !hasAdvancement(world, player, "anomalies/anomaly_gravity");
        if (needsGravity) {
            boolean underground = player.getY() <= 0 && !world.canSeeSky(player.blockPosition());
            boolean nightSurface = world.isDarkOutside() && world.canSeeSky(player.blockPosition());
            if ((underground || nightSurface) && world.getRandom().nextFloat() < 0.40f) {
                triggerAnomaly(world, player, AnomalyType.GRAVITY, 120); // 6 seconds
                return;
            }
        }

        // Anomaly C: Chrono-Static Pulse (Driving ATV or near industrial tech)
        boolean needsChrono = !player.entityTags().contains("sd_anomaly_chrono") || !hasAdvancement(world, player, "anomalies/anomaly_chrono");
        if (needsChrono) {
            boolean isDriving = player.isPassenger();
            boolean nearTech = isNearIndustrialTech(world, player.blockPosition());
            if ((isDriving || nearTech) && world.getRandom().nextFloat() < 0.35f) {
                triggerAnomaly(world, player, AnomalyType.CHRONO, 80); // 4 seconds
                return;
            }
        }

        // Anomaly D: Subterranean Void Tremor (Near Bedrock Y <= -50)
        boolean needsBedrock = !player.entityTags().contains("sd_anomaly_bedrock") || !hasAdvancement(world, player, "anomalies/anomaly_bedrock");
        if (needsBedrock && player.getY() <= -50) {
            if (world.getRandom().nextFloat() < 0.40f) {
                triggerAnomaly(world, player, AnomalyType.BEDROCK, 80); // 4 seconds
            }
        }
    }

    private static boolean isNearIndustrialTech(ServerLevel world, BlockPos pos) {
        for (BlockPos check : BlockPos.betweenClosed(pos.offset(-4, -2, -4), pos.offset(4, 2, 4))) {
            var state = world.getBlockState(check);
            if (state.is(ModBlocks.STEEL_BATTERY) || state.is(ModBlocks.TUNGSTEN_BATTERY) ||
                state.is(ModBlocks.COPPER_GENERATOR) || state.is(ModBlocks.ALUMINUM_GENERATOR) ||
                state.is(ModBlocks.STEEL_GENERATOR) || state.is(ModBlocks.SUPER_COMPUTER) ||
                state.is(ModBlocks.LASER_QUARRY)) {
                return true;
            }
        }
        return false;
    }

    private static void triggerAnomaly(ServerLevel world, ServerPlayer player, AnomalyType type, int durationTicks) {
        ACTIVE_ANOMALIES.put(player.getUUID(), new ActiveAnomaly(type, durationTicks));

        switch (type) {
            case ALTITUDE -> {
                player.addTag("sd_anomaly_altitude");
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.2f, 1.4f);
                if (player.hasEffect(net.enchantedwood.effect.ModStatusEffects.ATMOSPHERIC_PROTECTION)) {
                    player.sendOverlayMessage(Component.literal("§b✦ Atmospheric Protection Shield: Vacuum collapse filtered! Air supply 100% stable."));
                } else {
                    player.sendOverlayMessage(Component.literal("§b❄ The atmosphere suddenly collapses into a vacuum... You struggle to breathe!"));
                    player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 70, 0, false, false, false));
                    player.setAirSupply(Math.min(player.getAirSupply(), 40));
                }
            }
            case GRAVITY -> {
                player.addTag("sd_anomaly_gravity");
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 0.5f);
                player.sendOverlayMessage(Component.literal("§5🌀 Local gravity collapsed! You drift into zero-gravity..."));
                player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 70, 0, false, false, false));
            }
            case CHRONO -> {
                player.addTag("sd_anomaly_chrono");
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.8f, 1.8f);
                player.sendOverlayMessage(Component.literal("§e⚡ Chrono-Electromagnetic Surge! Instruments overloaded."));
                player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 60, 0, false, false, false));
            }
            case BEDROCK -> {
                player.addTag("sd_anomaly_bedrock");
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 1.0f, 0.4f);
                player.sendOverlayMessage(Component.literal("§4👁 A colossal resonance echoes beneath the bedrock... Something stirs on the other side."));
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 70, 0, false, false, false));
            }
            case PLASMA_FLARE -> {
                player.addTag("sd_anomaly_plasma");
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.2f, 0.6f);
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 0.5f);
                if (player.hasEffect(net.enchantedwood.effect.ModStatusEffects.THERMAL_PROTECTION)) {
                    player.sendOverlayMessage(Component.literal("§6✦ Thermal Protection Shield: Deflected solar plasma radiation wave effortlessly!"));
                } else {
                    player.sendOverlayMessage(Component.literal("§6🔥 Solar Plasma Wave! Superheated extraterrestrial radiation washes over you..."));
                }
                // Safe temporary fire resistance so player is never harmed
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0, false, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false, false));
            }
        }

        grantAnomalyAdvancement(world, player, type);
    }

    private static void grantAnomalyAdvancement(ServerLevel world, ServerPlayer player, AnomalyType type) {
        if (world.getServer() == null) return;
        var loader = world.getServer().getAdvancements();
        var tracker = player.getAdvancements();

        String anomalyId;
        String criterionKey;
        switch (type) {
            case ALTITUDE -> {
                anomalyId = "anomalies/anomaly_altitude";
                criterionKey = "witnessed_altitude";
            }
            case GRAVITY -> {
                anomalyId = "anomalies/anomaly_gravity";
                criterionKey = "witnessed_gravity";
            }
            case CHRONO -> {
                anomalyId = "anomalies/anomaly_chrono";
                criterionKey = "witnessed_chrono";
            }
            case BEDROCK -> {
                anomalyId = "anomalies/anomaly_bedrock";
                criterionKey = "witnessed_bedrock";
            }
            case PLASMA_FLARE -> {
                anomalyId = "anomalies/anomaly_plasma";
                criterionKey = "witnessed_plasma";
            }
            default -> { return; }
        }

        var branchAdv = loader.get(Identifier.fromNamespaceAndPath("enchantedwood", "anomalies/spatial_ruptures"));
        if (branchAdv != null) {
            tracker.award(branchAdv, "auto_unlock");
        }

        var indAdv = loader.get(Identifier.fromNamespaceAndPath("enchantedwood", anomalyId));
        if (indAdv != null) {
            tracker.award(indAdv, "witnessed_anomaly");
        }

        var masterAdv = loader.get(Identifier.fromNamespaceAndPath("enchantedwood", "anomalies/cosmic_echoes"));
        if (masterAdv != null) {
            tracker.award(masterAdv, criterionKey);
        }
    }

    private static boolean hasAdvancement(ServerLevel world, ServerPlayer player, String path) {
        if (world.getServer() == null) return false;
        var adv = world.getServer().getAdvancements().get(Identifier.fromNamespaceAndPath("enchantedwood", path));
        if (adv == null) return false;
        return player.getAdvancements().getOrStartProgress(adv).isDone();
    }

    private static void handleAnomalyStep(ServerLevel world, ServerPlayer player, AnomalyType type, int remainingTicks) {
        switch (type) {
            case ALTITUDE -> {
                if (remainingTicks % 15 == 0) {
                    world.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0f, 1.5f);
                    world.sendParticles(ParticleTypes.SNOWFLAKE,
                            player.getX(), player.getY() + 1.2, player.getZ(), 6, 0.4, 0.3, 0.4, 0.02);
                }
                player.setAirSupply(Math.min(player.getAirSupply(), 20));
            }
            case GRAVITY -> {
                if (remainingTicks % 8 == 0) {
                    world.sendParticles(ParticleTypes.REVERSE_PORTAL,
                            player.getX(), player.getY() + 0.5, player.getZ(), 8, 0.4, 0.6, 0.4, 0.05);
                }
            }
            case CHRONO -> {
                if (remainingTicks % 10 == 0) {
                    world.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            player.getX(), player.getY() + 1.0, player.getZ(), 5, 0.3, 0.4, 0.3, 0.1);
                }
            }
            case BEDROCK -> {
                if (remainingTicks % 20 == 0) {
                    world.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.RESPAWN_ANCHOR_AMBIENT, SoundSource.BLOCKS, 1.2f, 0.5f);
                }
            }
            case PLASMA_FLARE -> {
                if (remainingTicks % 8 == 0) {
                    world.sendParticles(ParticleTypes.FLAME,
                            player.getX(), player.getY() + 1.0, player.getZ(), 10, 0.5, 0.5, 0.5, 0.08);
                    world.sendParticles(ParticleTypes.LAVA,
                            player.getX(), player.getY() + 0.5, player.getZ(), 3, 0.3, 0.2, 0.3, 0.02);
                }
            }
        }
    }

    private static void concludeAnomaly(ServerLevel world, ServerPlayer player, AnomalyType type) {
        switch (type) {
            case ALTITUDE -> {
                player.setAirSupply(300);
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_BREATH, SoundSource.PLAYERS, 1.2f, 1.0f);
                player.sendSystemMessage(Component.literal("§7...The air stabilizes. A temporary tear in the atmospheric layer?"));
            }
            case GRAVITY -> {
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 160, 0, false, false, false));
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.7f, 1.6f);
                player.sendSystemMessage(Component.literal("§dGravity snaps back into alignment. Something massive is bending spatial curvature from beyond."));
            }
            case CHRONO -> {
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.8f, 1.5f);
                player.sendSystemMessage(Component.literal("§6The electromagnetic field quiets down. A rogue radio transmission leaked through reality."));
            }
            case BEDROCK -> {
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.8f, 0.8f);
                player.sendSystemMessage(Component.literal("§8...The tremors subside. Whatever it was has receded into the dark abyss."));
            }
            case PLASMA_FLARE -> {
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.LAVA_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 1.2f);
                player.sendSystemMessage(Component.literal("§e...The thermal wave dissipates. A solar flare leaked through a rift from an alien star system."));
            }
        }
    }
}
