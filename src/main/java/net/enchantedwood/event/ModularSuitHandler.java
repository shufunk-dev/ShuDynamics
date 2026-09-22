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
import net.minecraft.text.Text;
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
                // In dark area: check suit battery power (siphons from helmet or chestplate/suit)
                boolean hasPower = getSuitStoredEnergy(player) >= 2;
                if (hasPower) {
                    // Drain 40 FE / sec (throttled to 20 FE every 10 ticks).
                    // Throttling prevents modifying the itemstack NBT every tick, which thrashed GUI slot sync and caused audio artifacts!
                    if (world.getTime() % 10 == 0) {
                        extractSuitEnergy(player, "enchantedwood:night_vision_module", 20);
                    }

                    StatusEffectInstance currentEffect = player.getStatusEffect(StatusEffects.NIGHT_VISION);
                    // Vanilla Minecraft begins warning flicker at <= 200 ticks (10s).
                    // By maintaining duration at 320 ticks (16s) and refreshing at <= 240 ticks (12s),
                    // it never reaches 200 ticks, completely eliminating any flickering!
                    if (currentEffect == null || currentEffect.getDuration() <= 240) {
                        player.addStatusEffect(new StatusEffectInstance(
                                StatusEffects.NIGHT_VISION,
                                320,
                                0,
                                false,
                                false,
                                true
                        ));
                    }
                } else {
                    // Out of power: shut off HUD quietly without spamming audio
                    StatusEffectInstance currentEffect = player.getStatusEffect(StatusEffects.NIGHT_VISION);
                    if (currentEffect != null && currentEffect.getDuration() <= 340) {
                        player.removeStatusEffect(StatusEffects.NIGHT_VISION);
                    }
                }
            } else if (lightLevel >= 9) {
                // Bright area: power down HUD to conserve power quietly
                StatusEffectInstance currentEffect = player.getStatusEffect(StatusEffects.NIGHT_VISION);
                if (currentEffect != null && currentEffect.getDuration() <= 340) {
                    player.removeStatusEffect(StatusEffects.NIGHT_VISION);
                }
            }
        }
    }

    public static boolean hasSuitModule(ServerPlayerEntity player, String moduleId) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack piece = player.getEquippedStack(slot);
            if (piece.getItem() instanceof ModularPowerArmorItem && ModularPowerArmorItem.hasModule(piece, moduleId)) {
                return true;
            }
        }
        return false;
    }

    public static int getSuitStoredEnergy(ServerPlayerEntity player) {
        int total = 0;
        EquipmentSlot[] slots = new EquipmentSlot[]{EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.HEAD, EquipmentSlot.FEET};
        for (EquipmentSlot slot : slots) {
            ItemStack piece = player.getEquippedStack(slot);
            if (piece.getItem() instanceof ModularPowerArmorItem) {
                total += ModularPowerArmorItem.getStoredEnergy(piece);
            }
        }
        return total;
    }

    public static boolean extractSuitEnergy(ServerPlayerEntity player, String moduleId, int amount) {
        EquipmentSlot[] slots = new EquipmentSlot[]{EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.HEAD, EquipmentSlot.FEET};
        // 1. Try host piece first
        for (EquipmentSlot slot : slots) {
            ItemStack piece = player.getEquippedStack(slot);
            if (piece.getItem() instanceof ModularPowerArmorItem && ModularPowerArmorItem.hasModule(piece, moduleId)) {
                if (ModularPowerArmorItem.getStoredEnergy(piece) >= amount) {
                    ModularPowerArmorItem.extractEnergy(piece, amount);
                    return true;
                }
            }
        }
        // 2. Siphon from any other suit piece
        for (EquipmentSlot slot : slots) {
            ItemStack piece = player.getEquippedStack(slot);
            if (piece.getItem() instanceof ModularPowerArmorItem) {
                if (ModularPowerArmorItem.getStoredEnergy(piece) >= amount) {
                    ModularPowerArmorItem.extractEnergy(piece, amount);
                    return true;
                }
            }
        }
        return false;
    }

    private static void tickChestplateModules(ServerPlayerEntity player, ServerWorld world) {
        // Fluoropolymer Acid-Proof Plating Module (Installed in Chestplate, Leggings, etc.)
        if (hasSuitModule(player, "enchantedwood:acid_proof_plating")) {
            boolean hasToxin = player.hasStatusEffect(StatusEffects.POISON)
                    || player.hasStatusEffect(StatusEffects.WITHER)
                    || player.hasStatusEffect(StatusEffects.NAUSEA);
            boolean inAcid = ConvergenceHazardHandler.isInCausticHazard(world, player);
            boolean activeLoad = hasToxin || inAcid;

            boolean hasEnergy;
            if (activeLoad) {
                // Drain 4 FE/t (~80 FE/s) while under active caustic stress
                hasEnergy = extractSuitEnergy(player, "enchantedwood:acid_proof_plating", 4);
            } else {
                hasEnergy = getSuitStoredEnergy(player) > 0;
            }

            if (hasEnergy) {
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
                if (activeLoad && world.getTime() % 10 == 0) {
                    world.spawnParticles(
                            ParticleTypes.HAPPY_VILLAGER,
                            player.getX(), player.getY() + 0.8, player.getZ(),
                            2, 0.2, 0.3, 0.2, 0.01
                    );
                }
            } else {
                if (player.hasStatusEffect(ModStatusEffects.ACID_PROTECTION)) {
                    player.removeStatusEffect(ModStatusEffects.ACID_PROTECTION);
                }
                if (activeLoad && world.getTime() % 40 == 0) {
                    player.sendMessage(Text.literal("§c⚠ ACID SHIELD COLLAPSED: 0 FE! Corrosive acid burning suit! ⚠"), true);
                }
            }
        }

        // Thermal Refractory Plating Module (Installed in Chestplate, Leggings, etc.)
        if (hasSuitModule(player, "enchantedwood:thermal_refractory_plating")) {
            boolean inLava = player.isInLava();
            boolean onFire = player.isOnFire();
            boolean inCaldera = ConvergenceHazardHandler.isInThermalHazard(world, player);
            boolean activeLoad = inLava || onFire || inCaldera;

            boolean hasEnergy;
            if (activeLoad) {
                // Drain 5 FE/t (~100 FE/s) while under active thermal stress
                hasEnergy = extractSuitEnergy(player, "enchantedwood:thermal_refractory_plating", 5);
            } else {
                hasEnergy = getSuitStoredEnergy(player) > 0;
            }

            if (hasEnergy) {
                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.FIRE_RESISTANCE,
                        60,
                        0,
                        true,
                        false,
                        true
                ));
                player.extinguish();
                if (inLava) {
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
            } else {
                if (player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
                    player.removeStatusEffect(StatusEffects.FIRE_RESISTANCE);
                }
                if (activeLoad && world.getTime() % 40 == 0) {
                    player.sendMessage(Text.literal("§c⚠ THERMAL SHIELD OVERHEATED: 0 FE! Heatsinks offline! ⚠"), true);
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
        if (world.getTime() % 40 != 0) return; // Tick every 2 seconds

        EquipmentSlot[] armorSlots = new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        };

        // Check if ANY equipped piece has a Nanite Repair Matrix installed!
        boolean hasNaniteNetwork = false;
        String bestChipId = "";
        ItemStack primaryNanitePiece = ItemStack.EMPTY;

        for (EquipmentSlot slot : armorSlots) {
            ItemStack piece = player.getEquippedStack(slot);
            if (piece.getItem() instanceof ModularPowerArmorItem &&
                    ModularPowerArmorItem.hasModule(piece, "enchantedwood:nanite_repair_matrix")) {
                hasNaniteNetwork = true;
                if (primaryNanitePiece.isEmpty()) {
                    primaryNanitePiece = piece;
                }
                String chipId = ModularPowerArmorItem.getInstalledChipId(piece);
                if ("enchantedwood:quantum_computer_chip".equals(chipId)) {
                    bestChipId = chipId;
                } else if ("enchantedwood:advanced_computer_chip".equals(chipId) && !"enchantedwood:quantum_computer_chip".equals(bestChipId)) {
                    bestChipId = chipId;
                } else if ("enchantedwood:basic_computer_chip".equals(chipId) && bestChipId.isEmpty()) {
                    bestChipId = chipId;
                }
            }
        }

        if (!hasNaniteNetwork) return;

        // Logic Core scales combat lockout:
        // Unchipped = 10s, Basic = 6s, Advanced = 3s, Quantum = 0s (continuous battlefield nanite repair)
        long combatCooldownMs = 10_000L;
        if ("enchantedwood:basic_computer_chip".equals(bestChipId)) {
            combatCooldownMs = 6_000L;
        } else if ("enchantedwood:advanced_computer_chip".equals(bestChipId)) {
            combatCooldownMs = 3_000L;
        } else if ("enchantedwood:quantum_computer_chip".equals(bestChipId)) {
            combatCooldownMs = 0L;
        }

        long lastDamage = PlayerHealthHandler.getLastDamageTime(player.getUuid());
        if (combatCooldownMs > 0 && System.currentTimeMillis() - lastDamage < combatCooldownMs) {
            return;
        }

        int repairAmount = 2; // Base speed: 2 durability points every 2 seconds
        if ("enchantedwood:basic_computer_chip".equals(bestChipId)) {
            repairAmount = 3;
        } else if ("enchantedwood:advanced_computer_chip".equals(bestChipId)) {
            repairAmount = 6;
        } else if ("enchantedwood:quantum_computer_chip".equals(bestChipId)) {
            repairAmount = 15;
        }

        int energyNeeded = repairAmount * 50;
        boolean repairedAny = false;

        for (EquipmentSlot slot : armorSlots) {
            ItemStack piece = player.getEquippedStack(slot);
            if (piece.getItem() instanceof ModularPowerArmorItem && piece.isDamaged()) {
                // Check energy: draw from piece first, then from primary nanite piece if needed
                int energyAvailable = ModularPowerArmorItem.getStoredEnergy(piece);
                boolean canDrawFromPiece = energyAvailable >= energyNeeded;
                boolean canDrawFromHost = !canDrawFromPiece && !primaryNanitePiece.isEmpty() && ModularPowerArmorItem.getStoredEnergy(primaryNanitePiece) >= energyNeeded;

                if (canDrawFromPiece) {
                    ModularPowerArmorItem.extractEnergy(piece, energyNeeded);
                } else if (canDrawFromHost) {
                    ModularPowerArmorItem.extractEnergy(primaryNanitePiece, energyNeeded);
                } else {
                    continue; // Not enough energy to repair this piece
                }

                boolean wasLocked = ModularPowerArmorItem.isChassisLocked(piece);
                int currentDmg = piece.getDamage();
                int newDmg = Math.max(0, currentDmg - repairAmount);
                piece.setDamage(newDmg);
                player.equipStack(slot, piece);
                repairedAny = true;

                // Notify player if Nanites successfully rebooted a locked chassis
                if (wasLocked && newDmg < piece.getMaxDamage() - 1) {
                    player.sendMessage(
                            Text.literal("§a§l[SYSTEM REBOOT] §e" + piece.getName().getString() + " §7restored online by Nanite Network!"),
                            true
                    );
                    world.playSound(
                            null, player.getX(), player.getY(), player.getZ(),
                            net.minecraft.sound.SoundEvents.BLOCK_BEACON_ACTIVATE,
                            net.minecraft.sound.SoundCategory.PLAYERS,
                            1.0f, 1.4f
                    );
                }
            }
        }

        if (repairedAny) {
            player.playerScreenHandler.sendContentUpdates();
            world.spawnParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    3, 0.2, 0.3, 0.2, 0.05
            );
        }
    }
}
