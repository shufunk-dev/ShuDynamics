package net.enchantedwood.event;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.enchantedwood.item.ModItems;

public class PlayerFlightHandler {

    public static void register() {
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            for (ServerWorld world : server.getWorlds()) {
                for (ServerPlayerEntity player : world.getPlayers()) {
                    tickPlayerCape(player);
                    tickPlayerJetpack(player, world);
                }
            }
        });
    }

    private static void tickPlayerJetpack(ServerPlayerEntity player, ServerWorld world) {
        if (player.isCreative() || player.isSpectator()) return;
        ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
        if (chest.isOf(ModItems.HYDROGEN_JETPACK)) {
            int fuel = net.enchantedwood.item.custom.HydrogenJetpackItem.getHydrogen(chest);
            if (fuel > 0 && !player.isOnGround()) {
                Vec3d vel = player.getVelocity();
                if (vel.y < 0.35) {
                    player.setVelocity(vel.x, Math.min(vel.y + 0.08, 0.45), vel.z);
                    player.velocityDirty = true;
                    player.fallDistance = 0.0f;

                    if (world.getTime() % 4 == 0) {
                        net.enchantedwood.item.custom.HydrogenJetpackItem.setHydrogen(chest, fuel - 1);
                        world.spawnParticles(net.minecraft.particle.ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 0.3, player.getZ(), 2, 0.1, 0.05, 0.1, 0.02);
                        world.spawnParticles(net.minecraft.particle.ParticleTypes.CLOUD, player.getX(), player.getY() + 0.2, player.getZ(), 1, 0.1, 0.05, 0.1, 0.01);
                    }
                }
            }
        }
    }

    private static void tickPlayerCape(ServerPlayerEntity player) {
        if (player.isCreative() || player.isSpectator()) return;

        boolean hasCape = hasCapeEquipped(player);

        if (!hasCape) {
            if (player.getAbilities().allowFlying) {
                player.getAbilities().allowFlying = false;
                player.getAbilities().flying = false;
                player.sendAbilitiesUpdate();
            }
            return;
        }

        // Cape is equipped! Check for Full Enchanted Netherite Set
        boolean fullEnchantedNetherite = isWearingFullEnchantedNetherite(player);

        if (fullEnchantedNetherite) {
            // Full Enchanted Netherite -> Grant Creative Flying!
            if (!player.getAbilities().allowFlying) {
                player.getAbilities().allowFlying = true;
                player.sendAbilitiesUpdate();
            }
        } else {
            // Any other armor / incomplete set -> Disable Creative Flying, grant Tiered Downward Float
            if (player.getAbilities().allowFlying) {
                player.getAbilities().allowFlying = false;
                player.getAbilities().flying = false;
                player.sendAbilitiesUpdate();
            }

            // Controlled Glide & Gentle Fall when falling in mid-air
            if (!player.isOnGround() && player.getVelocity().y < 0.0) {
                double floatSpeed = getDownwardFloatSpeed(player);
                Vec3d currentVel = player.getVelocity();
                
                // Calculate horizontal forward boost based on player look vector
                Vec3d look = player.getRotationVector();
                double horizSpeed = Math.sqrt(currentVel.x * currentVel.x + currentVel.z * currentVel.z);
                double targetHorizSpeed = Math.max(horizSpeed, 0.25);
                
                double newX = look.x * targetHorizSpeed * 0.7 + currentVel.x * 0.3;
                double newZ = look.z * targetHorizSpeed * 0.7 + currentVel.z * 0.3;
                double newY = Math.max(currentVel.y, floatSpeed);
                
                player.setVelocity(newX, newY, newZ);
                player.velocityDirty = true;
                player.fallDistance = 0.0f;
                player.onLanding(); // Reset fall distance
            }
        }
    }

    public static boolean hasCapeEquipped(ServerPlayerEntity player) {
        if (PlayerEquipmentState.getEquippedCape(player).isOf(ModItems.ENCHANTED_CAPE)) return true;
        if (player.getEquippedStack(EquipmentSlot.CHEST).isOf(ModItems.ENCHANTED_CAPE)) return true;
        if (player.getOffHandStack().isOf(ModItems.ENCHANTED_CAPE)) return true;
        
        // Dynamically check Trinkets API if present
        try {
            Class<?> trinketsApiClass = Class.forName("dev.emi.trinkets.api.TrinketsApi");
            Object optionalComp = trinketsApiClass.getMethod("getTrinketComponent", net.minecraft.entity.LivingEntity.class).invoke(null, player);
            if (optionalComp instanceof java.util.Optional<?> opt && opt.isPresent()) {
                Object comp = opt.get();
                Object isEq = comp.getClass().getMethod("isEquipped", net.minecraft.item.Item.class).invoke(comp, ModItems.ENCHANTED_CAPE);
                if (isEq instanceof Boolean b && b) return true;
            } else if (optionalComp != null) {
                Object isEq = optionalComp.getClass().getMethod("isEquipped", net.minecraft.item.Item.class).invoke(optionalComp, ModItems.ENCHANTED_CAPE);
                if (isEq instanceof Boolean b && b) return true;
            }
        } catch (Throwable ignored) {}
        
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(ModItems.ENCHANTED_CAPE) && i == 40) return true;
        }
        return false;
    }

    public static boolean isWearingFullEnchantedNetherite(ServerPlayerEntity player) {
        return player.getEquippedStack(EquipmentSlot.HEAD).isOf(ModItems.ENCHANTED_NETHERITE_HELMET)
                && player.getEquippedStack(EquipmentSlot.CHEST).isOf(ModItems.ENCHANTED_NETHERITE_CHESTPLATE)
                && player.getEquippedStack(EquipmentSlot.LEGS).isOf(ModItems.ENCHANTED_NETHERITE_LEGGINGS)
                && player.getEquippedStack(EquipmentSlot.FEET).isOf(ModItems.ENCHANTED_NETHERITE_BOOTS);
    }

    private static double getDownwardFloatSpeed(ServerPlayerEntity player) {
        ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
        if (chest.isOf(ModItems.ENCHANTED_DIAMOND_CHESTPLATE)) {
            return -0.05; // Controlled slow glide
        } else if (chest.isOf(ModItems.ENCHANTED_COBBLESTONE_CHESTPLATE) || chest.isOf(ModItems.BRONZE_CHESTPLATE)) {
            return -0.12; // Medium float
        } else if (chest.isOf(ModItems.ENCHANTED_WOOD_CHESTPLATE)) {
            return -0.08; // Gentle float
        }
        return -0.18; // Base float
    }
}
