package net.enchantedwood.event;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.EnchantedHeartItem;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerHealthHandler {
    public static final Identifier HEART_HEALTH_MODIFIER_ID = Identifier.of(EnchantedWoodMod.MOD_ID, "heart_locket_health");

    private static final Map<UUID, Float> LAST_HEART_HEALTH_BONUS = new HashMap<>();
    private static final Map<UUID, Long> LAST_DAMAGE_TIME = new HashMap<>();
    private static final Map<UUID, Integer> RECHARGE_TICKS = new HashMap<>();

    public static void register() {
        // Record damage timestamp for out-of-combat auto-recharge timer (10s)
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player) {
                LAST_DAMAGE_TIME.put(player.getUuid(), System.currentTimeMillis());
            }
            return true;
        });

        ServerTickEvents.START_SERVER_TICK.register(server -> {
            for (ServerWorld world : server.getWorlds()) {
                for (ServerPlayerEntity player : world.getPlayers()) {
                    tickPlayerHeartLocket(player);
                }
            }
        });
    }

    private static void tickPlayerHeartLocket(ServerPlayerEntity player) {
        if (player.isSpectator()) return;

        UUID uuid = player.getUuid();
        float targetBonus = getEquippedHeartHealthBonus(player);
        Float lastBonus = LAST_HEART_HEALTH_BONUS.getOrDefault(uuid, -1.0f);

        // Update EntityAttributeModifier for MAX_HEALTH whenever equipped Heart Locket changes
        if (targetBonus != lastBonus) {
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

    private static void updateMaxHealthAttribute(ServerPlayerEntity player, float newBonus, float oldBonus) {
        EntityAttributeInstance attribute = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (attribute != null) {
            attribute.removeModifier(HEART_HEALTH_MODIFIER_ID);
            if (newBonus > 0.0f) {
                EntityAttributeModifier modifier = new EntityAttributeModifier(
                        HEART_HEALTH_MODIFIER_ID,
                        newBonus,
                        EntityAttributeModifier.Operation.ADD_VALUE
                );
                attribute.addPersistentModifier(modifier);

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

    public static void applyHeartAbsorptionImmediate(ServerPlayerEntity player) {
        float targetBonus = getEquippedHeartHealthBonus(player);
        Float lastBonus = LAST_HEART_HEALTH_BONUS.getOrDefault(player.getUuid(), 0.0f);
        updateMaxHealthAttribute(player, targetBonus, lastBonus);
        LAST_HEART_HEALTH_BONUS.put(player.getUuid(), targetBonus);
    }

    public static float getEquippedHeartHealthBonus(ServerPlayerEntity player) {
        float maxBonus = 0.0f;

        // Check native Heart Container Slot
        maxBonus = Math.max(maxBonus, getHeartValue(PlayerEquipmentState.getEquippedHeart(player)));

        // Check Trinkets API if present
        try {
            Class<?> trinketsApiClass = Class.forName("dev.emi.trinkets.api.TrinketsApi");
            Object optionalComp = trinketsApiClass.getMethod("getTrinketComponent", net.minecraft.entity.LivingEntity.class).invoke(null, player);
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

    private static boolean isItemEquippedInTrinkets(Object comp, net.minecraft.item.Item item) {
        try {
            Object isEq = comp.getClass().getMethod("isEquipped", net.minecraft.item.Item.class).invoke(comp, item);
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
        if (stack.isOf(ModItems.NETHERITE_ENCHANTED_HEART)) return 20.0f;
        if (stack.isOf(ModItems.DIAMOND_ENCHANTED_HEART)) return 14.0f;
        if (stack.isOf(ModItems.GOLD_ENCHANTED_HEART)) return 10.0f;
        if (stack.isOf(ModItems.IRON_ENCHANTED_HEART)) return 6.0f;
        if (stack.isOf(ModItems.ENCHANTED_HEART)) return 2.0f;
        return 0.0f;
    }
}
