package net.enchantedwood.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;

public class CelestialLeapStatusEffect extends StatusEffect {
    public CelestialLeapStatusEffect() {
        super(StatusEffectCategory.BENEFICIAL, 0xFFE600); // Starlight Gold
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyUpdateEffect(ServerWorld world, LivingEntity entity, int amplifier) {
        // Featherweight descent & zero fall damage
        entity.fallDistance = 0.0f;
        if (!entity.isOnGround() && entity.getVelocity().y < -0.15) {
            entity.setVelocity(entity.getVelocity().x, Math.max(entity.getVelocity().y, -0.18), entity.getVelocity().z);
        }

        // Maintain Jump Boost II (3-block leaps)
        if (!entity.hasStatusEffect(StatusEffects.JUMP_BOOST) || entity.getStatusEffect(StatusEffects.JUMP_BOOST).getDuration() < 30) {
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 60, 1, false, false, false));
        }

        if (world.getTime() % 15 == 0) {
            world.spawnParticles(
                    ParticleTypes.END_ROD,
                    entity.getX(), entity.getY() + 0.5, entity.getZ(),
                    1, 0.2, 0.2, 0.2, 0.02
            );
        }
        return true;
    }
}
