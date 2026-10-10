package net.enchantedwood.item.custom;

import net.enchantedwood.item.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class EnchantedArmorItem extends Item {
    public EnchantedArmorItem(Properties settings) {
        super(settings);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, @Nullable EquipmentSlot slot) {
        if (entity instanceof Player player) {
            applyFullSetBonus(player);
        }
        super.inventoryTick(stack, world, entity, slot);
    }

    private void applyFullSetBonus(Player player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack feet = player.getItemBySlot(EquipmentSlot.FEET);

        if (head.isEmpty() || chest.isEmpty() || legs.isEmpty() || feet.isEmpty()) return;

        Item h = head.getItem();
        Item c = chest.getItem();
        Item l = legs.getItem();
        Item f = feet.getItem();

        // Tier 1: Enchanted Wood Armor Set (Speed I + Resistance I)
        if (h == ModItems.ENCHANTED_WOOD_HELMET && c == ModItems.ENCHANTED_WOOD_CHESTPLATE && l == ModItems.ENCHANTED_WOOD_LEGGINGS && f == ModItems.ENCHANTED_WOOD_BOOTS) {
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, 0, false, false, true));
        }

        // Tier 2: Enchanted Cobblestone Armor Set (Resistance I + Haste I)
        else if (h == ModItems.ENCHANTED_COBBLESTONE_HELMET && c == ModItems.ENCHANTED_COBBLESTONE_CHESTPLATE && l == ModItems.ENCHANTED_COBBLESTONE_LEGGINGS && f == ModItems.ENCHANTED_COBBLESTONE_BOOTS) {
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.HASTE, 40, 0, false, false, true));
        }

        // Tier 3: Enchanted Diamond Armor Set (Resistance II + Speed I + Regeneration I)
        else if (h == ModItems.ENCHANTED_DIAMOND_HELMET && c == ModItems.ENCHANTED_DIAMOND_CHESTPLATE && l == ModItems.ENCHANTED_DIAMOND_LEGGINGS && f == ModItems.ENCHANTED_DIAMOND_BOOTS) {
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 1, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, false, false, true));
        }

        // Tier 4: Enchanted Netherite Armor Set (OP Endgame: Resistance III + Speed III + Strength II + Fire Resistance + Night Vision)
        else if (h == ModItems.ENCHANTED_NETHERITE_HELMET && c == ModItems.ENCHANTED_NETHERITE_CHESTPLATE && l == ModItems.ENCHANTED_NETHERITE_LEGGINGS && f == ModItems.ENCHANTED_NETHERITE_BOOTS) {
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 2, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, 2, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 40, 1, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, false, false, true));
        }
    }
}
