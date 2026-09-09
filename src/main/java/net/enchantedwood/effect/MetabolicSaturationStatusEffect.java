package net.enchantedwood.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;

/**
 * Metabolic Saturation: Represents the 5-second assimilation window after a Hypospray injection.
 * Inoculating again while this effect is active causes Metabolic Inoculant Overload (Chemical Sickness).
 */
public class MetabolicSaturationStatusEffect extends StatusEffect {
    public MetabolicSaturationStatusEffect() {
        super(StatusEffectCategory.NEUTRAL, 0x00E5FF); // Bright Cyan / Medical Blue
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyUpdateEffect(ServerWorld world, LivingEntity entity, int amplifier) {
        // Subtle ambient medical telemetry particle every 20 ticks
        if (world.getTime() % 20 == 0) {
            world.spawnParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    entity.getX(), entity.getY() + 1.0, entity.getZ(),
                    1, 0.2, 0.2, 0.2, 0.01
            );
        }
        return true;
    }
}
