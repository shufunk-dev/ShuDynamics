package net.enchantedwood.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Metabolic Saturation: Represents the 5-second assimilation window after a Hypospray injection.
 * Inoculating again while this effect is active causes Metabolic Inoculant Overload (Chemical Sickness).
 */
public class MetabolicSaturationStatusEffect extends MobEffect {
    public MetabolicSaturationStatusEffect() {
        super(MobEffectCategory.NEUTRAL, 0x00E5FF); // Bright Cyan / Medical Blue
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel world, LivingEntity entity, int amplifier) {
        // Subtle ambient medical telemetry particle every 20 ticks
        if (world.getGameTime() % 20 == 0) {
            world.sendParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    entity.getX(), entity.getY() + 1.0, entity.getZ(),
                    1, 0.2, 0.2, 0.2, 0.01
            );
        }
        return true;
    }
}
