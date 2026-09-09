package net.enchantedwood.event;

import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.effect.ModStatusEffects;
import net.enchantedwood.entity.custom.AtvEntity;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.ModularPowerArmorItem;
import net.enchantedwood.world.dimension.ModDimensions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ConvergenceHazardHandler {

    private static final Map<UUID, Long> LAST_ACID_WARN = new HashMap<>();
    private static final Map<UUID, Long> LAST_THERMAL_WARN = new HashMap<>();
    private static final Map<UUID, Long> LAST_ATMOSPHERIC_WARN = new HashMap<>();

    public static void register() {
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            for (ServerWorld world : server.getWorlds()) {
                if (world.getRegistryKey() != ModDimensions.CONVERGENCE_WORLD_KEY) {
                    continue;
                }

                for (ServerPlayerEntity player : world.getPlayers()) {
                    if (player.isCreative() || player.isSpectator()) continue;
                    tickPlayerHazards(world, player);
                }
            }
        });
    }

    private static void tickPlayerHazards(ServerWorld world, ServerPlayerEntity player) {
        // Vehicle Cabin Protection: Sealed ATV cockpit shields driver and passengers from all environmental hazards
        if (player.getVehicle() instanceof AtvEntity atv && atv.isEnvironmentalCockpitSealed()) {
            return;
        }

        tickAcidHazard(world, player);
        tickThermalHazard(world, player);
        tickAtmosphericHazard(world, player);
    }

    // --- 1. CAUSTIC ACID PRECIPITATION & ACID WATERS ---
    private static void tickAcidHazard(ServerWorld world, ServerPlayerEntity player) {
        BlockPos pos = player.getBlockPos();
        boolean inWater = player.isTouchingWater() || player.isSubmergedInWater();
        boolean inAcidRain = world.isRaining() && world.isSkyVisible(pos);

        if (!inWater && !inAcidRain) return;

        // Check Immunities
        if (player.hasStatusEffect(ModStatusEffects.ACID_PROTECTION)) return;
        if (hasAcidProofPlating(player)) return;

        long now = world.getTime();
        UUID uuid = player.getUuid();

        // Warning Alert (Throttled to once every 8 seconds)
        if (now - LAST_ACID_WARN.getOrDefault(uuid, 0L) >= 160) {
            LAST_ACID_WARN.put(uuid, now);
            player.sendMessage(Text.literal("§c⚠ CORROSIVE ACID: Caustic precipitation & water burning exposed suit! Seek shelter or apply Acid Protection! ⚠"), true);
        }

        // Damage & audio/visual effects every 40 ticks (2.0s)
        if (now % 40 == (Math.abs(uuid.hashCode()) % 40)) {
            player.damage(world, world.getDamageSources().magic(), 2.0f);
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLOCK_LAVA_EXTINGUISH, SoundCategory.PLAYERS, 0.4f, 1.6f);
            world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    3, 0.2, 0.3, 0.2, 0.02);
            world.spawnParticles(ParticleTypes.SNEEZE,
                    player.getX(), player.getY() + 0.8, player.getZ(),
                    2, 0.2, 0.3, 0.2, 0.02);
        }
    }

    private static boolean hasAcidProofPlating(ServerPlayerEntity player) {
        ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
        if (chest.isOf(ModItems.MODULAR_POWER_CHESTPLATE) && ModularPowerArmorItem.hasModule(chest, "enchantedwood:acid_proof_plating")) {
            return true;
        }
        return player.getInventory().contains(new ItemStack(ModItems.ACID_PROOF_PLATING));
    }

    // --- 2. VOLCANIC CALDERA HYPERTHERMIA ---
    private static void tickThermalHazard(ServerWorld world, ServerPlayerEntity player) {
        BlockPos pos = player.getBlockPos();
        boolean isDeepCaldera = player.getY() <= 25;
        boolean nearHeatSource = isNearThermalSource(world, pos);

        if (!isDeepCaldera && !nearHeatSource) return;

        // Check Immunities
        if (player.hasStatusEffect(ModStatusEffects.THERMAL_PROTECTION)) return;
        if (player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) return;
        if (hasThermalRefractoryPlating(player)) return;

        long now = world.getTime();
        UUID uuid = player.getUuid();

        // Warning Alert (Throttled to once every 8 seconds)
        if (now - LAST_THERMAL_WARN.getOrDefault(uuid, 0L) >= 160) {
            LAST_THERMAL_WARN.put(uuid, now);
            player.sendMessage(Text.literal("§6⚠ EXTREME THERMAL CALDERA: Ambient convective heat boiling suit systems! Thermal shielding required! ⚠"), true);
        }

        // Damage & fire ticks every 40 ticks (2.0s)
        if (now % 40 == (Math.abs(uuid.hashCode()) % 40)) {
            player.damage(world, world.getDamageSources().onFire(), 2.0f);
            player.setOnFireFor(3);
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLOCK_FIRE_AMBIENT, SoundCategory.PLAYERS, 0.5f, 1.0f);
            world.spawnParticles(ParticleTypes.FLAME,
                    player.getX(), player.getY() + 0.5, player.getZ(),
                    4, 0.25, 0.25, 0.25, 0.02);
            world.spawnParticles(ParticleTypes.LAVA,
                    player.getX(), player.getY() + 0.2, player.getZ(),
                    1, 0.1, 0.1, 0.1, 0.01);
        }
    }

    private static boolean isNearThermalSource(ServerWorld world, BlockPos pos) {
        for (BlockPos check : BlockPos.iterate(pos.add(-2, -2, -2), pos.add(2, 2, 2))) {
            var state = world.getBlockState(check);
            if (state.isOf(Blocks.MAGMA_BLOCK) || state.isOf(Blocks.LAVA) ||
                state.isOf(ModBlocks.VOLCANIC_SOIL) || state.isOf(ModBlocks.POZZOLANIC_ASPHALT)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasThermalRefractoryPlating(ServerPlayerEntity player) {
        ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
        if (chest.isOf(ModItems.MODULAR_POWER_CHESTPLATE) && ModularPowerArmorItem.hasModule(chest, "enchantedwood:thermal_refractory_plating")) {
            return true;
        }
        return player.getInventory().contains(new ItemStack(ModItems.THERMAL_REFRACTORY_PLATING));
    }

    // --- 3. ATMOSPHERIC HYPOXIA, VACUUM RIFTS & ANOXIC CAVES ---
    private static void tickAtmosphericHazard(ServerWorld world, ServerPlayerEntity player) {
        BlockPos pos = player.getBlockPos();
        boolean isHighAltitude = player.getY() >= 180 && world.isSkyVisible(pos);
        boolean isAnoxicCave = player.getY() <= 35 && !world.isSkyVisible(pos) && world.getLightLevel(pos) <= 7;
        boolean isNearAltarRift = isNearSingularityRift(world, pos);

        if (!isHighAltitude && !isAnoxicCave && !isNearAltarRift) return;

        // Check Immunities / Active Life Support
        if (player.hasStatusEffect(ModStatusEffects.ATMOSPHERIC_PROTECTION)) return;

        // Modular Power Helmet Life-Support Scrubber
        ItemStack helmet = player.getEquippedStack(EquipmentSlot.HEAD);
        if (helmet.isOf(ModItems.MODULAR_POWER_HELMET)) {
            int stored = ModularPowerArmorItem.getStoredEnergy(helmet);
            if (stored >= 1) {
                if (world.getTime() % 2 == 0) {
                    ModularPowerArmorItem.extractEnergy(helmet, 1); // 10 FE / sec life support scrub
                }
                player.setAir(player.getMaxAir());
                return;
            }
        }

        long now = world.getTime();
        UUID uuid = player.getUuid();

        // Rapid Oxygen Depletion in vacuum/anoxic air
        player.setAir(Math.max(0, player.getAir() - 8));

        // Warning Alert (Throttled to once every 8 seconds)
        if (now - LAST_ATMOSPHERIC_WARN.getOrDefault(uuid, 0L) >= 160) {
            LAST_ATMOSPHERIC_WARN.put(uuid, now);
            if (isAnoxicCave) {
                player.sendMessage(Text.literal("§b⚠ ANOXIC CAVERN: Severe hypoxia! Cavern air depleted. Hyper-Oxygenation or life-support required! ⚠"), true);
            } else {
                player.sendMessage(Text.literal("§b⚠ ATMOSPHERIC VACUUM: Severe hypoxia detected! Hyper-Oxygenation or life-support required! ⚠"), true);
            }
        }

        // Suffocation & Darkness once air is completely exhausted
        if (player.getAir() <= 0) {
            if (now % 30 == (Math.abs(uuid.hashCode()) % 30)) {
                player.damage(world, world.getDamageSources().drown(), 2.0f);
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 60, 0, false, false, false));
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENTITY_PLAYER_HURT_DROWN, SoundCategory.PLAYERS, 0.8f, 1.2f);
                world.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                        player.getX(), player.getY() + 1.2, player.getZ(),
                        3, 0.2, 0.3, 0.2, 0.02);
            }
        }
    }

    private static boolean isNearSingularityRift(ServerWorld world, BlockPos pos) {
        for (BlockPos check : BlockPos.iterate(pos.add(-16, -8, -16), pos.add(16, 8, 16))) {
            var state = world.getBlockState(check);
            if (state.isOf(ModBlocks.RESONANCE_ALTAR)) {
                return true;
            }
        }
        return false;
    }
}
