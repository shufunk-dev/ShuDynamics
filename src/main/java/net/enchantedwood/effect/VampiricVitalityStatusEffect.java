package net.enchantedwood.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;

public class VampiricVitalityStatusEffect extends StatusEffect {
    public VampiricVitalityStatusEffect() {
        super(StatusEffectCategory.BENEFICIAL, 0xD90429); // Crimson Red
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyUpdateEffect(ServerWorld world, LivingEntity entity, int amplifier) {
        if (world.getTime() % 25 == 0) {
            world.spawnParticles(
                    ParticleTypes.HEART,
                    entity.getX(), entity.getY() + 0.9, entity.getZ(),
                    1, 0.25, 0.3, 0.25, 0.02
            );
        }
        return true;
    }
}
