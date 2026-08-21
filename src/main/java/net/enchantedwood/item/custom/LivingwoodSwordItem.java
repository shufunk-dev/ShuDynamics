package net.enchantedwood.item.custom;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class LivingwoodSwordItem extends Item {
    public LivingwoodSwordItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getEntityWorld().isClient()) {
            attacker.heal(2.0f);
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 60, 0, false, true));
        }
        super.postHit(stack, target, attacker);
    }
}


