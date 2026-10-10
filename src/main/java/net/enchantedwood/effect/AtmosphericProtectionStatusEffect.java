package net.enchantedwood.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public class AtmosphericProtectionStatusEffect extends MobEffect {
    public AtmosphericProtectionStatusEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x48CAE4); // Sky Cyan
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel world, LivingEntity entity, int amplifier) {
        // Continuously replenish air supply underwater and in vacuums
        entity.setAirSupply(entity.getMaxAirSupply());

        // Remove vision impairments from vacuum/anomalies
        if (entity.hasEffect(MobEffects.DARKNESS)) {
            entity.removeEffect(MobEffects.DARKNESS);
        }
        if (entity.hasEffect(MobEffects.BLINDNESS)) {
            entity.removeEffect(MobEffects.BLINDNESS);
        }

        // Ambient bubble particles
        if (world.getGameTime() % 20 == 0) {
            world.sendParticles(
                    ParticleTypes.BUBBLE_POP,
                    entity.getX(), entity.getY() + 1.2, entity.getZ(),
                    2, 0.2, 0.2, 0.2, 0.01
            );
        }
        return true;
    }
}
