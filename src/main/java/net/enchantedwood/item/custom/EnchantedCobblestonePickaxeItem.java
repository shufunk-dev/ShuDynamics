package net.enchantedwood.item.custom;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

public class EnchantedCobblestonePickaxeItem extends Item {
    public EnchantedCobblestonePickaxeItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, @Nullable EquipmentSlot slot) {
        if (entity instanceof LivingEntity livingEntity && slot == EquipmentSlot.MAINHAND) {
            livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 40, 2, false, false, true)); // Haste III
            livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, 1, false, false, true)); // Resistance II
        }
        super.inventoryTick(stack, world, entity, slot);
    }
}
