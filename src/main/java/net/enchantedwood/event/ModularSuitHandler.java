package net.enchantedwood.event;

import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.effect.ModStatusEffects;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.ModularPowerArmorItem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

public class ModularSuitHandler {

    private static final Identifier STEP_ASSIST_MODIFIER_ID = Identifier.of(EnchantedWoodMod.MOD_ID, "suit_step_assist");

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
        tickChestplateModules(player, world);
        tickLeggingsModules(player, world);
        tickBootsModules(player, world);
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

    private static void tickChestplateModules(ServerPlayerEntity player, ServerWorld world) {
        ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
        if (!chest.isOf(ModItems.MODULAR_POWER_CHESTPLATE)) return;

        // Fluoropolymer Acid-Proof Plating Module
        if (ModularPowerArmorItem.hasModule(chest, "enchantedwood:acid_proof_plating")) {
            player.addStatusEffect(new StatusEffectInstance(
                    ModStatusEffects.ACID_PROTECTION,
                    60,
                    0,
                    true,
                    false,
                    true
            ));
            if (player.hasStatusEffect(StatusEffects.POISON)) {
                player.removeStatusEffect(StatusEffects.POISON);
            }
            if (player.hasStatusEffect(StatusEffects.WITHER)) {
                player.removeStatusEffect(StatusEffects.WITHER);
            }
            if (player.hasStatusEffect(StatusEffects.NAUSEA)) {
                player.removeStatusEffect(StatusEffects.NAUSEA);
            }
            if (world.getTime() % 20 == 0) {
                world.spawnParticles(
                        ParticleTypes.HAPPY_VILLAGER,
                        player.getX(), player.getY() + 0.8, player.getZ(),
                        1, 0.2, 0.3, 0.2, 0.01
                );
            }
        }

        // Thermal Refractory Plating Module
        if (ModularPowerArmorItem.hasModule(chest, "enchantedwood:thermal_refractory_plating")) {
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.FIRE_RESISTANCE,
                    60,
                    0,
                    true,
                    false,
                    true
            ));
            player.extinguish();
            if (player.isInLava()) {
                player.setVelocity(player.getVelocity().x * 1.15, Math.max(player.getVelocity().y, 0.1), player.getVelocity().z * 1.15);
                player.fallDistance = 0.0f;
                if (world.getTime() % 10 == 0) {
                    world.spawnParticles(
                            ParticleTypes.FLAME,
                            player.getX(), player.getY() + 0.1, player.getZ(),
                            2, 0.2, 0.0, 0.2, 0.01
                    );
                }
            }
        }
    }

    private static void tickLeggingsModules(ServerPlayerEntity player, ServerWorld world) {
        ItemStack legs = player.getEquippedStack(EquipmentSlot.LEGS);
        if (!legs.isOf(ModItems.MODULAR_POWER_LEGGINGS)) return;

        // Speed Servo Leg Module
        if (ModularPowerArmorItem.hasModule(legs, "enchantedwood:speed_servo_module")) {
            int stored = ModularPowerArmorItem.getStoredEnergy(legs);
            if (stored >= 2) {
                boolean isMoving = player.isSprinting() || player.getVelocity().horizontalLengthSquared() > 0.005;
                if (isMoving) {
                    ModularPowerArmorItem.extractEnergy(legs, 2);
                    if (player.isSprinting() && world.getTime() % 4 == 0) {
                        world.spawnParticles(
                                ParticleTypes.ELECTRIC_SPARK,
                                player.getX(), player.getY() + 0.1, player.getZ(),
                                1, 0.15, 0.05, 0.15, 0.02
                        );
                    }
                }
                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.SPEED,
                        30,
                        1,
                        true,
                        false,
                        true
                ));
            }
        }
    }

    private static void tickBootsModules(ServerPlayerEntity player, ServerWorld world) {
        ItemStack boots = player.getEquippedStack(EquipmentSlot.FEET);
        EntityAttributeInstance stepAttr = player.getAttributeInstance(EntityAttributes.STEP_HEIGHT);

        boolean hasStepAssist = boots.isOf(ModItems.MODULAR_POWER_BOOTS) &&
                ModularPowerArmorItem.hasModule(boots, "enchantedwood:step_assist_module") &&
                ModularPowerArmorItem.getStoredEnergy(boots) > 0;

        if (stepAttr != null) {
            if (hasStepAssist) {
                if (!stepAttr.hasModifier(STEP_ASSIST_MODIFIER_ID)) {
                    stepAttr.addTemporaryModifier(new EntityAttributeModifier(
                            STEP_ASSIST_MODIFIER_ID,
                            0.6,
                            EntityAttributeModifier.Operation.ADD_VALUE
                    ));
                }
                // Passive drain: 1 FE every second while walking on ground
                if (player.isOnGround() && (player.isSprinting() || player.getVelocity().horizontalLengthSquared() > 0.005) && world.getTime() % 20 == 0) {
                    ModularPowerArmorItem.extractEnergy(boots, 1);
                }
            } else if (stepAttr.hasModifier(STEP_ASSIST_MODIFIER_ID)) {
                stepAttr.removeModifier(STEP_ASSIST_MODIFIER_ID);
            }
        }

        if (!boots.isOf(ModItems.MODULAR_POWER_BOOTS)) return;

        // High-Jump Actuator Boot Module
        if (ModularPowerArmorItem.hasModule(boots, "enchantedwood:high_jump_module")) {
            int stored = ModularPowerArmorItem.getStoredEnergy(boots);
            if (stored >= 1) {
                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.JUMP_BOOST,
                        30,
                        1,
                        true,
                        false,
                        true
                ));

                // 100% Fall damage negation upon landing
                if (player.fallDistance > 0.0f) {
                    player.fallDistance = 0.0f;
                }

                if (!player.isOnGround()) {
                    ModularPowerArmorItem.extractEnergy(boots, 1);
                    if (world.getTime() % 4 == 0) {
                        world.spawnParticles(
                                ParticleTypes.CLOUD,
                                player.getX(), player.getY(), player.getZ(),
                                1, 0.1, 0.0, 0.1, 0.01
                        );
                    }
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
                    String chipId = ModularPowerArmorItem.getInstalledChipId(piece);
                    int repairAmount = 2; // Base speed: 2 durability points every 2 seconds
                    if ("enchantedwood:basic_computer_chip".equals(chipId)) {
                        repairAmount = 3;
                    } else if ("enchantedwood:advanced_computer_chip".equals(chipId)) {
                        repairAmount = 6;
                    } else if ("enchantedwood:quantum_computer_chip".equals(chipId)) {
                        repairAmount = 15;
                    }

                    int energyNeeded = repairAmount * 50;
                    int energy = ModularPowerArmorItem.getStoredEnergy(piece);
                    if (energy >= energyNeeded) {
                        ModularPowerArmorItem.extractEnergy(piece, energyNeeded);
                        int currentDmg = piece.getDamage();
                        piece.setDamage(Math.max(0, currentDmg - repairAmount));
                        player.equipStack(slot, piece);
                        player.playerScreenHandler.sendContentUpdates();

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
