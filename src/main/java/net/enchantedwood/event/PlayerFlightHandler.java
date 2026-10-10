package net.enchantedwood.event;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.enchantedwood.item.ModItems;

public class PlayerFlightHandler {

    public static boolean hasRingOfGravitationalMastery(ServerPlayer player) {
        return player.getInventory().hasAnyMatching(stack -> stack.is(ModItems.RING_OF_GRAVITATIONAL_MASTERY));
    }

    public static void register() {
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            for (ServerLevel world : server.getAllLevels()) {
                for (ServerPlayer player : world.players()) {
                    if (player.isCreative() || player.isSpectator()) continue;

                    if (hasRingOfGravitationalMastery(player)) {
                        player.fallDistance = 0.0f;
                        if (!player.getAbilities().mayfly) {
                            player.getAbilities().mayfly = true;
                            player.onUpdateAbilities();
                        }
                        if (player.getAbilities().flying && world.getGameTime() % 3 == 0) {
                            world.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, player.getX(), player.getY() + 0.2, player.getZ(), 2, 0.2, 0.1, 0.2, 0.02);
                            world.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 0.1, player.getZ(), 3, 0.2, 0.1, 0.2, 0.05);
                        }
                        continue;
                    }

                    tickPlayerCape(player);
                    tickPlayerJetpack(player, world);
                }
            }
        });
    }

    public static boolean isWearingActiveJetpack(ServerPlayer player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.is(ModItems.HYDROGEN_JETPACK) && net.enchantedwood.item.custom.HydrogenJetpackItem.getHydrogen(chest) > 0) return true;
        if (chest.is(ModItems.MODULAR_POWER_CHESTPLATE)) {
            if (net.enchantedwood.item.custom.ModularPowerArmorItem.hasModule(chest, "enchantedwood:ion_repulsor_module") && net.enchantedwood.item.custom.ModularPowerArmorItem.getStoredEnergy(chest) >= 25) return true;
            if (net.enchantedwood.item.custom.ModularPowerArmorItem.hasModule(chest, "enchantedwood:hydrogen_thruster_module")) return true;
        }
        return false;
    }

    private static void tickPlayerJetpack(ServerPlayer player, ServerLevel world) {
        if (player.isCreative() || player.isSpectator()) return;
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.is(ModItems.HYDROGEN_JETPACK)) {
            int fuel = net.enchantedwood.item.custom.HydrogenJetpackItem.getHydrogen(chest);
            if (fuel > 0) {
                // Grant flight!
                if (!player.getAbilities().mayfly) {
                    player.getAbilities().mayfly = true;
                    player.onUpdateAbilities();
                }

                // If actively flying in air:
                if (player.getAbilities().flying) {
                    player.fallDistance = 0.0f;

                    // Rocket thruster particles
                    if (world.getGameTime() % 2 == 0) {
                        world.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 0.3, player.getZ(), 2, 0.1, 0.05, 0.1, 0.02);
                        world.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, player.getX(), player.getY() + 0.2, player.getZ(), 1, 0.1, 0.05, 0.1, 0.01);
                    }

                    // Thruster audio loop every second (quiet ambient hum)
                    if (world.getGameTime() % 20 == 0) {
                        world.playSound(null, player.getX(), player.getY(), player.getZ(), net.minecraft.sounds.SoundEvents.ELYTRA_FLYING, net.minecraft.sounds.SoundSource.PLAYERS, 0.035f, 1.4f);
                    }

                    // Consume fuel: 1 mB every 4 ticks (5 mB/s -> 5,000 mB lasts ~16.6 minutes of continuous flight)
                    if (world.getGameTime() % 4 == 0) {
                        net.enchantedwood.item.custom.HydrogenJetpackItem.setHydrogen(chest, fuel - 1);
                    }
                } else if (!player.onGround()) {
                    // Safe glide / fall dampening when jumping or falling while wearing fueled jetpack
                    player.fallDistance = 0.0f;
                }
            } else {
                // Fuel exhausted!
                if (player.getAbilities().mayfly && !hasCapeEquipped(player) && !isWearingFullEnchantedNetherite(player)) {
                    player.getAbilities().mayfly = false;
                    player.getAbilities().flying = false;
                    player.onUpdateAbilities();
                    player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("§c⚠️ Jetpack Fuel Depleted!"));
                    world.playSound(null, player.getX(), player.getY(), player.getZ(), net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.5f);
                }
                // Emergency parachute descent dampener
                if (!player.onGround() && player.getDeltaMovement().y < -0.3) {
                    Vec3 vel = player.getDeltaMovement();
                    player.setDeltaMovement(vel.x, Math.max(vel.y, -0.25), vel.z);
                    player.needsSync = true;
                    player.fallDistance = 0.0f;
                }
            }
        } else if (chest.is(ModItems.MODULAR_POWER_CHESTPLATE)) {
            boolean hasIonRepulsors = net.enchantedwood.item.custom.ModularPowerArmorItem.hasModule(chest, "enchantedwood:ion_repulsor_module");
            boolean hasHydrogenThrusters = net.enchantedwood.item.custom.ModularPowerArmorItem.hasModule(chest, "enchantedwood:hydrogen_thruster_module");

            if (hasIonRepulsors) {
                int energy = net.enchantedwood.item.custom.ModularPowerArmorItem.getStoredEnergy(chest);
                if (energy >= 25) {
                    if (!player.getAbilities().mayfly) {
                        player.getAbilities().mayfly = true;
                        player.onUpdateAbilities();
                    }

                    if (player.getAbilities().flying) {
                        player.fallDistance = 0.0f;

                        // Iron Man repulsor particles (electric sparks and end rod ion energy)
                        if (world.getGameTime() % 2 == 0) {
                            world.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 0.2, player.getZ(), 3, 0.15, 0.05, 0.15, 0.02);
                            world.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, player.getX(), player.getY() + 0.1, player.getZ(), 1, 0.05, 0.02, 0.05, 0.01);
                        }

                        // Ambient repulsor sound
                        if (world.getGameTime() % 20 == 0) {
                            world.playSound(null, player.getX(), player.getY(), player.getZ(), net.minecraft.sounds.SoundEvents.BEACON_AMBIENT, net.minecraft.sounds.SoundSource.PLAYERS, 0.15f, 1.8f);
                        }

                        // Consume FE: 25 FE / tick (500 FE / sec)
                        net.enchantedwood.item.custom.ModularPowerArmorItem.extractEnergy(chest, 25);
                    } else if (!player.onGround()) {
                        player.fallDistance = 0.0f;
                    }
                } else {
                    // Battery out of juice!
                    if (player.getAbilities().mayfly && !hasCapeEquipped(player) && !isWearingFullEnchantedNetherite(player)) {
                        player.getAbilities().mayfly = false;
                        player.getAbilities().flying = false;
                        player.onUpdateAbilities();
                        player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("§c⚡ Chestplate Battery Depleted — Repulsors Offline!"));
                    }
                    if (!player.onGround() && player.getDeltaMovement().y < -0.3) {
                        Vec3 vel = player.getDeltaMovement();
                        player.setDeltaMovement(vel.x, Math.max(vel.y, -0.25), vel.z);
                        player.needsSync = true;
                        player.fallDistance = 0.0f;
                    }
                }
            } else if (hasHydrogenThrusters) {
                int fuel = net.enchantedwood.item.custom.ModularPowerArmorItem.getCustomData(chest).getIntOr("Hydrogen", 0);
                if (fuel <= 0) {
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        ItemStack invStack = player.getInventory().getItem(i);
                        if (invStack.is(ModItems.HYDROGEN_CANISTER)) {
                            invStack.shrink(1);
                            player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.EMPTY_GAS_CANISTER), net.minecraft.util.Prediction.SERVER_ONLY);
                            fuel += 1000;
                            var nbt = net.enchantedwood.item.custom.ModularPowerArmorItem.getCustomData(chest);
                            nbt.putInt("Hydrogen", fuel);
                            net.enchantedwood.item.custom.ModularPowerArmorItem.setCustomData(chest, nbt);
                            player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("§b⚡ Auto-Refueled 1,000 mB Hydrogen from Canister!"));
                            break;
                        }
                    }
                }

                if (fuel > 0) {
                    if (!player.getAbilities().mayfly) {
                        player.getAbilities().mayfly = true;
                        player.onUpdateAbilities();
                    }

                    if (player.getAbilities().flying) {
                        player.fallDistance = 0.0f;
                        if (world.getGameTime() % 2 == 0) {
                            world.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 0.3, player.getZ(), 2, 0.1, 0.05, 0.1, 0.02);
                        }
                        if (world.getGameTime() % 20 == 0) {
                            world.playSound(null, player.getX(), player.getY(), player.getZ(), net.minecraft.sounds.SoundEvents.ELYTRA_FLYING, net.minecraft.sounds.SoundSource.PLAYERS, 0.035f, 1.4f);
                        }
                        if (world.getGameTime() % 4 == 0) {
                            fuel--;
                            var nbt = net.enchantedwood.item.custom.ModularPowerArmorItem.getCustomData(chest);
                            nbt.putInt("Hydrogen", fuel);
                            net.enchantedwood.item.custom.ModularPowerArmorItem.setCustomData(chest, nbt);
                        }
                    } else if (!player.onGround()) {
                        player.fallDistance = 0.0f;
                    }
                } else {
                    if (player.getAbilities().mayfly && !hasCapeEquipped(player) && !isWearingFullEnchantedNetherite(player)) {
                        player.getAbilities().mayfly = false;
                        player.getAbilities().flying = false;
                        player.onUpdateAbilities();
                        player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("§c⚠️ Hydrogen Thrusters Out of Fuel!"));
                    }
                }
            } else {
                if (player.getAbilities().mayfly && !hasCapeEquipped(player) && !isWearingFullEnchantedNetherite(player)) {
                    player.getAbilities().mayfly = false;
                    player.getAbilities().flying = false;
                    player.onUpdateAbilities();
                }
            }
        } else {
            // Jetpack was unequipped (and player does not have Netherite Cape flight)
            if (player.getAbilities().mayfly && !hasCapeEquipped(player) && !isWearingFullEnchantedNetherite(player)) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
        }
    }

    private static void tickPlayerCape(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) return;

        boolean hasCape = hasCapeEquipped(player);
        boolean hasJetpack = isWearingActiveJetpack(player);

        if (!hasCape) {
            // Only disable flight if player is not currently powered by a jetpack
            if (!hasJetpack && player.getAbilities().mayfly) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
            return;
        }

        // Cape is equipped! Check for Full Enchanted Netherite Set
        boolean fullEnchantedNetherite = isWearingFullEnchantedNetherite(player);

        if (fullEnchantedNetherite) {
            // Full Enchanted Netherite -> Grant Creative Flying!
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
        } else {
            // Any other armor / incomplete set -> Disable Creative Flying unless powered by jetpack
            if (!hasJetpack && player.getAbilities().mayfly) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }

            // Controlled Glide & Gentle Fall when falling in mid-air
            if (!player.onGround() && player.getDeltaMovement().y < 0.0) {
                double floatSpeed = getDownwardFloatSpeed(player);
                Vec3 currentVel = player.getDeltaMovement();
                
                // Calculate horizontal forward boost based on player look vector
                Vec3 look = player.getLookAngle();
                double horizSpeed = Math.sqrt(currentVel.x * currentVel.x + currentVel.z * currentVel.z);
                double targetHorizSpeed = Math.max(horizSpeed, 0.25);
                
                double newX = look.x * targetHorizSpeed * 0.7 + currentVel.x * 0.3;
                double newZ = look.z * targetHorizSpeed * 0.7 + currentVel.z * 0.3;
                double newY = Math.max(currentVel.y, floatSpeed);
                
                player.setDeltaMovement(newX, newY, newZ);
                player.needsSync = true;
                player.fallDistance = 0.0f;
                player.resetFallDistance(); // Reset fall distance
            }
        }
    }

    public static boolean hasCapeEquipped(ServerPlayer player) {
        if (PlayerEquipmentState.getEquippedCape(player).is(ModItems.ENCHANTED_CAPE)) return true;
        if (player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.ENCHANTED_CAPE)) return true;
        if (player.getOffhandItem().is(ModItems.ENCHANTED_CAPE)) return true;
        
        // Dynamically check Trinkets API if present
        try {
            Class<?> trinketsApiClass = Class.forName("dev.emi.trinkets.api.TrinketsApi");
            Object optionalComp = trinketsApiClass.getMethod("getTrinketComponent", net.minecraft.world.entity.LivingEntity.class).invoke(null, player);
            if (optionalComp instanceof java.util.Optional<?> opt && opt.isPresent()) {
                Object comp = opt.get();
                Object isEq = comp.getClass().getMethod("isEquipped", net.minecraft.world.item.Item.class).invoke(comp, ModItems.ENCHANTED_CAPE);
                if (isEq instanceof Boolean b && b) return true;
            } else if (optionalComp != null) {
                Object isEq = optionalComp.getClass().getMethod("isEquipped", net.minecraft.world.item.Item.class).invoke(optionalComp, ModItems.ENCHANTED_CAPE);
                if (isEq instanceof Boolean b && b) return true;
            }
        } catch (Throwable ignored) {}
        
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.ENCHANTED_CAPE) && i == 40) return true;
        }
        return false;
    }

    public static boolean isWearingFullEnchantedNetherite(ServerPlayer player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.ENCHANTED_NETHERITE_HELMET)
                && player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.ENCHANTED_NETHERITE_CHESTPLATE)
                && player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.ENCHANTED_NETHERITE_LEGGINGS)
                && player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.ENCHANTED_NETHERITE_BOOTS);
    }

    private static double getDownwardFloatSpeed(ServerPlayer player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.is(ModItems.ENCHANTED_DIAMOND_CHESTPLATE)) {
            return -0.05; // Controlled slow glide
        } else if (chest.is(ModItems.ENCHANTED_COBBLESTONE_CHESTPLATE) || chest.is(ModItems.BRONZE_CHESTPLATE)) {
            return -0.12; // Medium float
        } else if (chest.is(ModItems.ENCHANTED_WOOD_CHESTPLATE)) {
            return -0.08; // Gentle float
        }
        return -0.18; // Base float
    }
}
