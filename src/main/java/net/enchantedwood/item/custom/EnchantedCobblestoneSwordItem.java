package net.enchantedwood.item.custom;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class EnchantedCobblestoneSwordItem extends Item {
    public EnchantedCobblestoneSwordItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getEntityWorld().isClient()) {
            attacker.heal(6.0f); // 3 full hearts instant heal (3x wooden 2.0f)
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 120, 2, false, true)); // Regen III for 6 seconds
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 100, 1, false, true)); // Strength II for 5 seconds
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 2, false, true)); // Slowness III on target
        }
        super.postHit(stack, target, attacker);
    }
}
