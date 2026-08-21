package net.enchantedwood.item.custom;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.enchantedwood.item.ModItems;
import org.jetbrains.annotations.Nullable;

public class EnchantedArmorItem extends Item {
    public EnchantedArmorItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, @Nullable EquipmentSlot slot) {
        if (entity instanceof PlayerEntity player) {
            applyFullSetBonus(player);
        }
        super.inventoryTick(stack, world, entity, slot);
    }

    private void applyFullSetBonus(PlayerEntity player) {
        ItemStack head = player.getEquippedStack(EquipmentSlot.HEAD);
        ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
        ItemStack legs = player.getEquippedStack(EquipmentSlot.LEGS);
        ItemStack feet = player.getEquippedStack(EquipmentSlot.FEET);

        if (head.isEmpty() || chest.isEmpty() || legs.isEmpty() || feet.isEmpty()) return;

        Item h = head.getItem();
        Item c = chest.getItem();
        Item l = legs.getItem();
        Item f = feet.getItem();

        // Tier 1: Enchanted Wood Armor Set (Speed I + Resistance I)
        if (h == ModItems.ENCHANTED_WOOD_HELMET && c == ModItems.ENCHANTED_WOOD_CHESTPLATE && l == ModItems.ENCHANTED_WOOD_LEGGINGS && f == ModItems.ENCHANTED_WOOD_BOOTS) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, 0, false, false, true));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 40, 0, false, false, true));
        }

        // Tier 2: Enchanted Cobblestone Armor Set (Resistance I + Haste I)
        else if (h == ModItems.ENCHANTED_COBBLESTONE_HELMET && c == ModItems.ENCHANTED_COBBLESTONE_CHESTPLATE && l == ModItems.ENCHANTED_COBBLESTONE_LEGGINGS && f == ModItems.ENCHANTED_COBBLESTONE_BOOTS) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, 0, false, false, true));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 40, 0, false, false, true));
        }

        // Tier 3: Enchanted Diamond Armor Set (Resistance II + Speed I + Regeneration I)
        else if (h == ModItems.ENCHANTED_DIAMOND_HELMET && c == ModItems.ENCHANTED_DIAMOND_CHESTPLATE && l == ModItems.ENCHANTED_DIAMOND_LEGGINGS && f == ModItems.ENCHANTED_DIAMOND_BOOTS) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, 1, false, false, true));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 40, 0, false, false, true));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 40, 0, false, false, true));
        }

        // Tier 4: Enchanted Netherite Armor Set (OP Endgame: Resistance III + Speed III + Strength II + Fire Resistance + Night Vision)
        else if (h == ModItems.ENCHANTED_NETHERITE_HELMET && c == ModItems.ENCHANTED_NETHERITE_CHESTPLATE && l == ModItems.ENCHANTED_NETHERITE_LEGGINGS && f == ModItems.ENCHANTED_NETHERITE_BOOTS) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, 2, false, false, true));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 40, 2, false, false, true));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 40, 1, false, false, true));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 40, 0, false, false, true));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 300, 0, false, false, true));
        }
    }
}
