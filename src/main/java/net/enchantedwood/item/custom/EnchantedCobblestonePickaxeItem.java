package net.enchantedwood.item.custom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class EnchantedCobblestonePickaxeItem extends Item {
    public EnchantedCobblestonePickaxeItem(Properties settings) {
        super(settings);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, @Nullable EquipmentSlot slot) {
        if (entity instanceof LivingEntity livingEntity && slot == EquipmentSlot.MAINHAND) {
            livingEntity.addEffect(new MobEffectInstance(MobEffects.HASTE, 40, 2, false, false, true)); // Haste III
            livingEntity.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 1, false, false, true)); // Resistance II
        }
        super.inventoryTick(stack, world, entity, slot);
    }
}
