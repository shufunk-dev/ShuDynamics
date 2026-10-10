package net.enchantedwood.event;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.EnchantedHeartItem;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerHealthHandler {
    public static final Identifier HEART_HEALTH_MODIFIER_ID = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "heart_locket_health");

    private static final Map<UUID, Float> LAST_HEART_HEALTH_BONUS = new HashMap<>();
    private static final Map<UUID, Long> LAST_DAMAGE_TIME = new HashMap<>();
    private static final Map<UUID, Integer> RECHARGE_TICKS = new HashMap<>();

    public static long getLastDamageTime(UUID uuid) {
        return LAST_DAMAGE_TIME.getOrDefault(uuid, 0L);
    }

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            // Melee Lifesteal: When attacker is a player with VAMPIRIC_VITALITY
            if (source.getEntity() instanceof ServerPlayer attacker) {
                if (attacker.hasEffect(net.enchantedwood.effect.ModStatusEffects.VAMPIRIC_VITALITY)) {
                    float healAmount = Math.max(0.5f, amount * 0.15f);
                    attacker.heal(healAmount);
                    if (attacker.level() instanceof ServerLevel sw) {
                        sw.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART, attacker.getX(), attacker.getY() + 1.0, attacker.getZ(), 2, 0.3, 0.3, 0.3, 0.02);
                    }
                }
            }

            if (entity instanceof ServerPlayer player) {
                // 1. Acid Protection: Negates magic, poison, wither, and corrosive damage
                if (player.hasEffect(net.enchantedwood.effect.ModStatusEffects.ACID_PROTECTION)) {
                    if (source.is(net.minecraft.world.damagesource.DamageTypes.MAGIC) ||
                        source.is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC) ||
                        source.is(net.minecraft.world.damagesource.DamageTypes.WITHER)) {
                        return false;
                    }
                }

                // 2. Thermal Protection: Negates all fire, lava, hot floor, and freeze damage
                if (player.hasEffect(net.enchantedwood.effect.ModStatusEffects.THERMAL_PROTECTION)) {
                    if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE) ||
                        source.is(net.minecraft.tags.DamageTypeTags.IS_FREEZING) ||
                        source.is(net.minecraft.world.damagesource.DamageTypes.HOT_FLOOR)) {
                        player.clearFire();
                        return false;
                    }
                }

                // 3. Atmospheric Protection: Negates drowning, wall suffocation, and vacuum collapse
                if (player.hasEffect(net.enchantedwood.effect.ModStatusEffects.ATMOSPHERIC_PROTECTION)) {
                    if (source.is(net.minecraft.tags.DamageTypeTags.IS_DROWNING) ||
                        source.is(net.minecraft.world.damagesource.DamageTypes.IN_WALL)) {
                        player.setAirSupply(player.getMaxAirSupply());
                        return false;
                    }
                }

                LAST_DAMAGE_TIME.put(player.getUUID(), System.currentTimeMillis());
            }
            return true;
        });

        ServerTickEvents.START_SERVER_TICK.register(server -> {
            for (ServerLevel world : server.getAllLevels()) {
                for (ServerPlayer player : world.players()) {
                    tickPlayerHeartLocket(player);
                }
            }
        });

        // Instant Heart Container Resync on Respawn (fixes keepInventory modifier persistence)
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            LAST_HEART_HEALTH_BONUS.remove(newPlayer.getUUID());
            tickPlayerHeartLocket(newPlayer);
        });
    }

    private static void tickPlayerHeartLocket(ServerPlayer player) {
        if (player.isSpectator()) return;

        UUID uuid = player.getUUID();
        float targetBonus = getEquippedHeartHealthBonus(player);
        Float lastBonus = LAST_HEART_HEALTH_BONUS.getOrDefault(uuid, -1.0f);

        AttributeInstance attribute = player.getAttribute(Attributes.MAX_HEALTH);
        boolean hasModifier = attribute != null && attribute.getModifier(HEART_HEALTH_MODIFIER_ID) != null;

        // Update EntityAttributeModifier for MAX_HEALTH whenever equipped Heart Locket changes OR is missing after respawn
        if (targetBonus != lastBonus || (targetBonus > 0.0f && !hasModifier)) {
            updateMaxHealthAttribute(player, targetBonus, lastBonus);
            LAST_HEART_HEALTH_BONUS.put(uuid, targetBonus);
        }

        if (targetBonus <= 0.0f) return;

        float maxHealth = player.getMaxHealth();
        float currentHealth = player.getHealth();

        // Out-Of-Combat Auto-Recharge (10 seconds after damage -> heals +1 HP every 1.5 seconds)
        long lastDamage = LAST_DAMAGE_TIME.getOrDefault(uuid, 0L);
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastDamage >= 10000L && currentHealth < maxHealth) {
            int ticks = RECHARGE_TICKS.getOrDefault(uuid, 0) + 1;
            if (ticks >= 30) { // 30 ticks = 1.5s
                ticks = 0;
                player.heal(1.0f); // Heal +1 HP
            }
            RECHARGE_TICKS.put(uuid, ticks);
        } else {
            RECHARGE_TICKS.put(uuid, 0);
        }
    }

    private static void updateMaxHealthAttribute(ServerPlayer player, float newBonus, float oldBonus) {
        AttributeInstance attribute = player.getAttribute(Attributes.MAX_HEALTH);
        if (attribute != null) {
            attribute.removeModifier(HEART_HEALTH_MODIFIER_ID);
            if (newBonus > 0.0f) {
                AttributeModifier modifier = new AttributeModifier(
                        HEART_HEALTH_MODIFIER_ID,
                        newBonus,
                        AttributeModifier.Operation.ADD_VALUE
                );
                attribute.addPermanentModifier(modifier);

                // If player's max health expanded, heal up the new health difference on initial equip
                if (oldBonus >= 0.0f && newBonus > oldBonus) {
                    float diff = newBonus - Math.max(0.0f, oldBonus);
                    player.heal(diff);
                }
            } else {
                // Clamped current health if it exceeds base max health upon removal
                if (player.getHealth() > player.getMaxHealth()) {
                    player.setHealth(player.getMaxHealth());
                }
            }
        }
    }

    public static void applyHeartAbsorptionImmediate(ServerPlayer player) {
        float targetBonus = getEquippedHeartHealthBonus(player);
        Float lastBonus = LAST_HEART_HEALTH_BONUS.getOrDefault(player.getUUID(), 0.0f);
        updateMaxHealthAttribute(player, targetBonus, lastBonus);
        LAST_HEART_HEALTH_BONUS.put(player.getUUID(), targetBonus);
    }

    public static float getEquippedHeartHealthBonus(ServerPlayer player) {
        float maxBonus = 0.0f;

        // Check native Heart Container Slot
        maxBonus = Math.max(maxBonus, getHeartValue(PlayerEquipmentState.getEquippedHeart(player)));

        // Check Trinkets API if present
        try {
            Class<?> trinketsApiClass = Class.forName("dev.emi.trinkets.api.TrinketsApi");
            Object optionalComp = trinketsApiClass.getMethod("getTrinketComponent", net.minecraft.world.entity.LivingEntity.class).invoke(null, player);
            if (optionalComp instanceof java.util.Optional<?> opt && opt.isPresent()) {
                Object comp = opt.get();
                if (isItemEquippedInTrinkets(comp, ModItems.NETHERITE_ENCHANTED_HEART)) return 20.0f;
                if (isItemEquippedInTrinkets(comp, ModItems.DIAMOND_ENCHANTED_HEART)) maxBonus = Math.max(maxBonus, 14.0f);
                if (isItemEquippedInTrinkets(comp, ModItems.GOLD_ENCHANTED_HEART)) maxBonus = Math.max(maxBonus, 10.0f);
                if (isItemEquippedInTrinkets(comp, ModItems.IRON_ENCHANTED_HEART)) maxBonus = Math.max(maxBonus, 6.0f);
                if (isItemEquippedInTrinkets(comp, ModItems.ENCHANTED_HEART)) maxBonus = Math.max(maxBonus, 2.0f);
            }
        } catch (Throwable ignored) {}

        return maxBonus;
    }

    private static boolean isItemEquippedInTrinkets(Object comp, net.minecraft.world.item.Item item) {
        try {
            Object isEq = comp.getClass().getMethod("isEquipped", net.minecraft.world.item.Item.class).invoke(comp, item);
            return isEq instanceof Boolean b && b;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static float getHeartValue(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0.0f;
        if (stack.getItem() instanceof EnchantedHeartItem heartItem) {
            return heartItem.getAbsorptionAmount();
        }
        if (stack.is(ModItems.NETHERITE_ENCHANTED_HEART)) return 20.0f;
        if (stack.is(ModItems.DIAMOND_ENCHANTED_HEART)) return 14.0f;
        if (stack.is(ModItems.GOLD_ENCHANTED_HEART)) return 10.0f;
        if (stack.is(ModItems.IRON_ENCHANTED_HEART)) return 6.0f;
        if (stack.is(ModItems.ENCHANTED_HEART)) return 2.0f;
        return 0.0f;
    }
}
