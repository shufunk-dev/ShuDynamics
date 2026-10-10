package net.enchantedwood.item.custom;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class EnchantedCobblestoneSwordItem extends Item {
    public EnchantedCobblestoneSwordItem(Properties settings) {
        super(settings);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide()) {
            attacker.heal(6.0f); // 3 full hearts instant heal (3x wooden 2.0f)
            attacker.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 2, false, true)); // Regen III for 6 seconds
            attacker.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 100, 1, false, true)); // Strength II for 5 seconds
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 2, false, true)); // Slowness III on target
        }
        super.hurtEnemy(stack, target, attacker);
    }
}
