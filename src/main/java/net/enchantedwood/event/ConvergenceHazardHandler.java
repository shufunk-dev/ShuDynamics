package net.enchantedwood.event;

import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.effect.ModStatusEffects;
import net.enchantedwood.entity.custom.AtvEntity;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.ModularPowerArmorItem;
import net.enchantedwood.world.dimension.ModDimensions;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ConvergenceHazardHandler {

    private static final Map<UUID, Long> LAST_ACID_WARN = new HashMap<>();
    private static final Map<UUID, Long> LAST_THERMAL_WARN = new HashMap<>();
    private static final Map<UUID, Long> LAST_ATMOSPHERIC_WARN = new HashMap<>();
    public static final Set<BlockPos> SANCTUARY_CENTERS = new HashSet<>();

    public static void registerSanctuary(BlockPos pos) {
        SANCTUARY_CENTERS.add(pos);
    }

    public static boolean isInsideSanctuary(ServerLevel world, BlockPos pos) {
        // 1. Biome Check: Riftwood Haven is an inherently 100% hazard-free safe zone
        var biomeKey = world.getBiome(pos).unwrapKey();
        if (biomeKey.isPresent() && biomeKey.get().identifier().equals(Identifier.fromNamespaceAndPath("enchantedwood", "riftwood_haven"))) {
            return true;
        }

        // 2. Gateway Sanctuary Outpost radius (within 32 blocks of active gateway)
        for (BlockPos center : SANCTUARY_CENTERS) {
            if (center.closerThan(pos, 32.0)) {
                return true;
            }
        }

        // 3. Fallback: Quick scan within 6 blocks for dormant rift or atmospheric anchor
        for (BlockPos check : BlockPos.betweenClosed(pos.offset(-6, -3, -6), pos.offset(6, 3, 6))) {
            var state = world.getBlockState(check);
            if (state.is(ModBlocks.DORMANT_RIFT) || state.is(ModBlocks.ATMOSPHERIC_ANCHOR)) {
                SANCTUARY_CENTERS.add(check.immutable());
                return true;
            }
        }

        return false;
    }

    public static void register() {
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            for (ServerLevel world : server.getAllLevels()) {
                if (world.dimension() != ModDimensions.CONVERGENCE_WORLD_KEY) {
                    continue;
                }

                for (ServerPlayer player : world.players()) {
                    if (player.isCreative() || player.isSpectator()) continue;
                    tickPlayerHazards(world, player);
                }
            }
        });
    }

    private static void tickPlayerHazards(ServerLevel world, ServerPlayer player) {
        // Vehicle Cabin Protection: Sealed ATV cockpit shields driver and passengers from all environmental hazards
        if (player.getVehicle() instanceof AtvEntity atv && atv.isEnvironmentalCockpitSealed()) {
            return;
        }

        // Sanctuary Protection: All hazards are neutralized inside the Sanctuary and Riftwood Haven
        if (isInsideSanctuary(world, player.blockPosition())) {
            return;
        }

        tickAcidHazard(world, player);
        tickThermalHazard(world, player);
        tickAtmosphericHazard(world, player);
    }

    // --- 1. CAUSTIC ACID PRECIPITATION & ACID WATERS ---
    private static void tickAcidHazard(ServerLevel world, ServerPlayer player) {
        BlockPos pos = player.blockPosition();

        // Biome Scoping: Acid rain and caustic water hazard ONLY exist in the Caustic Mire biome!
        var biomeKey = world.getBiome(pos).unwrapKey();
        boolean isCausticBiome = biomeKey.isPresent() &&
                biomeKey.get().identifier().equals(Identifier.fromNamespaceAndPath("enchantedwood", "caustic_mire"));
        if (!isCausticBiome) return;

        boolean inWater = player.isInWater() || player.isUnderWater();
        boolean inAcidRain = world.isRaining() && world.canSeeSky(pos);

        if (!inWater && !inAcidRain) return;

        // Check Immunities
        if (player.hasEffect(ModStatusEffects.ACID_PROTECTION)) return;
        if (hasAcidProofPlating(player)) return;

        long now = world.getGameTime();
        UUID uuid = player.getUUID();

        // Warning Alert (Throttled to once every 8 seconds)
        if (now - LAST_ACID_WARN.getOrDefault(uuid, 0L) >= 160) {
            LAST_ACID_WARN.put(uuid, now);
            player.sendOverlayMessage(Component.literal("§c⚠ CORROSIVE ACID: Caustic precipitation & water burning exposed suit! Seek shelter or apply Acid Protection! ⚠"));
        }

        // Damage & audio/visual effects every 40 ticks (2.0s)
        if (now % 40 == (Math.abs(uuid.hashCode()) % 40)) {
            player.hurtServer(world, world.damageSources().magic(), 2.0f);
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.LAVA_EXTINGUISH, SoundSource.PLAYERS, 0.4f, 1.6f);
            world.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    3, 0.2, 0.3, 0.2, 0.02);
            world.sendParticles(ParticleTypes.SNEEZE,
                    player.getX(), player.getY() + 0.8, player.getZ(),
                    2, 0.2, 0.3, 0.2, 0.02);
        }
    }

    private static boolean hasAcidProofPlating(ServerPlayer player) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.HEAD, EquipmentSlot.FEET}) {
            ItemStack piece = player.getItemBySlot(slot);
            if (piece.getItem() instanceof ModularPowerArmorItem && ModularPowerArmorItem.hasModule(piece, "enchantedwood:acid_proof_plating")) {
                return ModularSuitHandler.getSuitStoredEnergy(player) > 0;
            }
        }
        return false;
    }

    // --- 2. VOLCANIC CALDERA HYPERTHERMIA ---
    private static void tickThermalHazard(ServerLevel world, ServerPlayer player) {
        BlockPos pos = player.blockPosition();

        var biomeKey = world.getBiome(pos).unwrapKey();
        boolean isCalderaBiome = biomeKey.isPresent() &&
                biomeKey.get().identifier().equals(Identifier.fromNamespaceAndPath("enchantedwood", "scorched_caldera"));

        boolean isDeepCaldera = player.getY() <= 25 && isCalderaBiome;
        boolean nearHeatSource = isNearThermalSource(world, pos);

        if (!isDeepCaldera && !nearHeatSource) return;

        // Check Immunities
        if (player.hasEffect(ModStatusEffects.THERMAL_PROTECTION)) return;
        if (player.hasEffect(MobEffects.FIRE_RESISTANCE)) return;
        if (hasThermalRefractoryPlating(player)) return;

        long now = world.getGameTime();
        UUID uuid = player.getUUID();

        // Warning Alert (Throttled to once every 8 seconds)
        if (now - LAST_THERMAL_WARN.getOrDefault(uuid, 0L) >= 160) {
            LAST_THERMAL_WARN.put(uuid, now);
            player.sendOverlayMessage(Component.literal("§6⚠ EXTREME THERMAL CALDERA: Ambient convective heat boiling suit systems! Thermal shielding required! ⚠"));
        }

        // Damage & fire ticks every 40 ticks (2.0s)
        if (now % 40 == (Math.abs(uuid.hashCode()) % 40)) {
            player.hurtServer(world, world.damageSources().onFire(), 2.0f);
            player.igniteForSeconds(3);
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 0.5f, 1.0f);
            world.sendParticles(ParticleTypes.FLAME,
                    player.getX(), player.getY() + 0.5, player.getZ(),
                    4, 0.25, 0.25, 0.25, 0.02);
            world.sendParticles(ParticleTypes.LAVA,
                    player.getX(), player.getY() + 0.2, player.getZ(),
                    1, 0.1, 0.1, 0.1, 0.01);
        }
    }

    public static boolean isInCausticHazard(ServerLevel world, ServerPlayer player) {
        if (world.dimension() != ModDimensions.CONVERGENCE_WORLD_KEY) return false;
        if (isInsideSanctuary(world, player.blockPosition())) return false;
        if (player.getVehicle() instanceof AtvEntity atv && atv.isEnvironmentalCockpitSealed()) return false;

        BlockPos pos = player.blockPosition();
        var biomeKey = world.getBiome(pos).unwrapKey();
        boolean isCausticBiome = biomeKey.isPresent() &&
                biomeKey.get().identifier().equals(Identifier.fromNamespaceAndPath("enchantedwood", "caustic_mire"));
        if (!isCausticBiome) return false;

        boolean inWater = player.isInWater() || player.isUnderWater();
        boolean inAcidRain = world.isRaining() && world.canSeeSky(pos);
        return inWater || inAcidRain;
    }

    public static boolean isInThermalHazard(ServerLevel world, ServerPlayer player) {
        if (world.dimension() != ModDimensions.CONVERGENCE_WORLD_KEY) return false;
        if (isInsideSanctuary(world, player.blockPosition())) return false;
        if (player.getVehicle() instanceof AtvEntity atv && atv.isEnvironmentalCockpitSealed()) return false;

        BlockPos pos = player.blockPosition();
        var biomeKey = world.getBiome(pos).unwrapKey();
        boolean isCalderaBiome = biomeKey.isPresent() &&
                biomeKey.get().identifier().equals(Identifier.fromNamespaceAndPath("enchantedwood", "scorched_caldera"));

        boolean isDeepCaldera = player.getY() <= 25 && isCalderaBiome;
        boolean nearHeatSource = isNearThermalSource(world, pos);
        return isDeepCaldera || nearHeatSource;
    }

    public static boolean isNearThermalSource(ServerLevel world, BlockPos pos) {
        for (BlockPos check : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 2, 2))) {
            var state = world.getBlockState(check);
            if (state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.LAVA)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasThermalRefractoryPlating(ServerPlayer player) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.HEAD, EquipmentSlot.FEET}) {
            ItemStack piece = player.getItemBySlot(slot);
            if (piece.getItem() instanceof ModularPowerArmorItem && ModularPowerArmorItem.hasModule(piece, "enchantedwood:thermal_refractory_plating")) {
                return ModularSuitHandler.getSuitStoredEnergy(player) > 0;
            }
        }
        return false;
    }

    // --- 3. ATMOSPHERIC HYPOXIA, VACUUM RIFTS & ANOXIC CAVES ---
    private static void tickAtmosphericHazard(ServerLevel world, ServerPlayer player) {
        BlockPos pos = player.blockPosition();

        var biomeKey = world.getBiome(pos).unwrapKey();
        boolean isAnoxicBiome = biomeKey.isPresent() &&
                biomeKey.get().identifier().equals(Identifier.fromNamespaceAndPath("enchantedwood", "anoxic_barrens"));

        boolean isHighAltitude = player.getY() >= 180 && world.canSeeSky(pos);
        boolean isAnoxicCave = player.getY() <= 35 && !world.canSeeSky(pos) && world.getMaxLocalRawBrightness(pos) <= 7;
        boolean isNearAltarRift = isNearSingularityRift(world, pos);

        if (!isAnoxicBiome && !isHighAltitude && !isAnoxicCave && !isNearAltarRift) return;

        // Check Immunities / Active Life Support
        if (player.hasEffect(ModStatusEffects.ATMOSPHERIC_PROTECTION)) return;

        // Modular Power Helmet Life-Support Scrubber
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.is(ModItems.MODULAR_POWER_HELMET)) {
            int stored = ModularPowerArmorItem.getStoredEnergy(helmet);
            if (stored >= 1) {
                if (world.getGameTime() % 2 == 0) {
                    ModularPowerArmorItem.extractEnergy(helmet, 1); // 10 FE / sec life support scrub
                }
                player.setAirSupply(player.getMaxAirSupply());
                return;
            }
        }

        long now = world.getGameTime();
        UUID uuid = player.getUUID();

        // Rapid Oxygen Depletion in vacuum/anoxic air
        player.setAirSupply(Math.max(0, player.getAirSupply() - 8));

        // Warning Alert (Throttled to once every 8 seconds)
        if (now - LAST_ATMOSPHERIC_WARN.getOrDefault(uuid, 0L) >= 160) {
            LAST_ATMOSPHERIC_WARN.put(uuid, now);
            if (isAnoxicBiome) {
                player.sendOverlayMessage(Component.literal("§b⚠ ANOXIC BARRENS: Zero atmospheric oxygen detected! Life-support or Hyper-Oxygenation required! ⚠"));
            } else if (isAnoxicCave) {
                player.sendOverlayMessage(Component.literal("§b⚠ ANOXIC CAVERN: Severe hypoxia! Cavern air depleted. Hyper-Oxygenation or life-support required! ⚠"));
            } else {
                player.sendOverlayMessage(Component.literal("§b⚠ ATMOSPHERIC VACUUM: Severe hypoxia detected! Hyper-Oxygenation or life-support required! ⚠"));
            }
        }

        // Suffocation & Darkness once air is completely exhausted
        if (player.getAirSupply() <= 0) {
            if (now % 30 == (Math.abs(uuid.hashCode()) % 30)) {
                player.hurtServer(world, world.damageSources().drown(), 2.0f);
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false, false));
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_HURT_DROWN, SoundSource.PLAYERS, 0.8f, 1.2f);
                world.sendParticles(ParticleTypes.REVERSE_PORTAL,
                        player.getX(), player.getY() + 1.2, player.getZ(),
                        3, 0.2, 0.3, 0.2, 0.02);
            }
        }
    }

    private static boolean isNearSingularityRift(ServerLevel world, BlockPos pos) {
        for (BlockPos check : BlockPos.betweenClosed(pos.offset(-16, -8, -16), pos.offset(16, 8, 16))) {
            var state = world.getBlockState(check);
            if (state.is(ModBlocks.RESONANCE_ALTAR)) {
                return true;
            }
        }
        return false;
    }
}
