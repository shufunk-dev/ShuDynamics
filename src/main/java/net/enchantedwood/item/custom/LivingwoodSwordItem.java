package net.enchantedwood.item.custom;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class LivingwoodSwordItem extends Item {
    public LivingwoodSwordItem(Properties settings) {
        super(settings);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide()) {
            attacker.heal(2.0f);
            attacker.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, true));
        }
        super.hurtEnemy(stack, target, attacker);
    }
}


