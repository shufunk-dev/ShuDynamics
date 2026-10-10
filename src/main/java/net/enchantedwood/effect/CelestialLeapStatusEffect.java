package net.enchantedwood.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public class CelestialLeapStatusEffect extends MobEffect {
    public CelestialLeapStatusEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFE600); // Starlight Gold
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel world, LivingEntity entity, int amplifier) {
        // Featherweight descent & zero fall damage
        entity.fallDistance = 0.0f;
        if (!entity.onGround() && entity.getDeltaMovement().y < -0.15) {
            entity.setDeltaMovement(entity.getDeltaMovement().x, Math.max(entity.getDeltaMovement().y, -0.18), entity.getDeltaMovement().z);
        }

        // Maintain Jump Boost II (3-block leaps)
        if (!entity.hasEffect(MobEffects.JUMP_BOOST) || entity.getEffect(MobEffects.JUMP_BOOST).getDuration() < 30) {
            entity.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 60, 1, false, false, false));
        }

        if (world.getGameTime() % 15 == 0) {
            world.sendParticles(
                    ParticleTypes.END_ROD,
                    entity.getX(), entity.getY() + 0.5, entity.getZ(),
                    1, 0.2, 0.2, 0.2, 0.02
            );
        }
        return true;
    }
}
