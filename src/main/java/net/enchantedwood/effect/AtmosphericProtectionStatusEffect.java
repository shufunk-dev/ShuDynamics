package net.enchantedwood.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;

public class AtmosphericProtectionStatusEffect extends StatusEffect {
    public AtmosphericProtectionStatusEffect() {
        super(StatusEffectCategory.BENEFICIAL, 0x48CAE4); // Sky Cyan
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyUpdateEffect(ServerWorld world, LivingEntity entity, int amplifier) {
        // Continuously replenish air supply underwater and in vacuums
        entity.setAir(entity.getMaxAir());

        // Remove vision impairments from vacuum/anomalies
        if (entity.hasStatusEffect(StatusEffects.DARKNESS)) {
            entity.removeStatusEffect(StatusEffects.DARKNESS);
        }
        if (entity.hasStatusEffect(StatusEffects.BLINDNESS)) {
            entity.removeStatusEffect(StatusEffects.BLINDNESS);
        }

        // Ambient bubble particles
        if (world.getTime() % 20 == 0) {
            world.spawnParticles(
                    ParticleTypes.BUBBLE_POP,
                    entity.getX(), entity.getY() + 1.2, entity.getZ(),
                    2, 0.2, 0.2, 0.2, 0.01
            );
        }
        return true;
    }
}
