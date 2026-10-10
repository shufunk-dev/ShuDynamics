package net.enchantedwood.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class VampiricVitalityStatusEffect extends MobEffect {
    public VampiricVitalityStatusEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xD90429); // Crimson Red
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel world, LivingEntity entity, int amplifier) {
        if (world.getGameTime() % 25 == 0) {
            world.sendParticles(
                    ParticleTypes.HEART,
                    entity.getX(), entity.getY() + 0.9, entity.getZ(),
                    1, 0.25, 0.3, 0.25, 0.02
            );
        }
        return true;
    }
}
