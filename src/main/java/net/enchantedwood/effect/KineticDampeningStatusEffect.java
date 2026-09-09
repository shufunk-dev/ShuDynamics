package net.enchantedwood.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.enchantedwood.EnchantedWoodMod;

public class KineticDampeningStatusEffect extends StatusEffect {
    public KineticDampeningStatusEffect() {
        super(StatusEffectCategory.BENEFICIAL, 0xFFB703); // Honey Amber
        addAttributeModifier(
                EntityAttributes.KNOCKBACK_RESISTANCE,
                Identifier.of(EnchantedWoodMod.MOD_ID, "kinetic_knockback_resistance"),
                1.0, // 100% Knockback Resistance
                EntityAttributeModifier.Operation.ADD_VALUE
        );
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyUpdateEffect(ServerWorld world, LivingEntity entity, int amplifier) {
        // Absorb impact and kinetic shock
        entity.fallDistance = 0.0f;

        if (world.getTime() % 20 == 0) {
            world.spawnParticles(
                    ParticleTypes.FALLING_HONEY,
                    entity.getX(), entity.getY() + 0.8, entity.getZ(),
                    2, 0.25, 0.25, 0.25, 0.01
            );
        }
        return true;
    }
}
