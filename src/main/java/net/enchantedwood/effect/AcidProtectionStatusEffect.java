package net.enchantedwood.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;

public class AcidProtectionStatusEffect extends StatusEffect {
    public AcidProtectionStatusEffect() {
        super(StatusEffectCategory.BENEFICIAL, 0x52B788); // Emerald Mint Green
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return true; // Apply every tick for continuous cleansing
    }

    @Override
    public boolean applyUpdateEffect(ServerWorld world, LivingEntity entity, int amplifier) {
        // Continuously cleanse corrosive and toxic effects
        if (entity.hasStatusEffect(StatusEffects.POISON)) {
            entity.removeStatusEffect(StatusEffects.POISON);
        }
        if (entity.hasStatusEffect(StatusEffects.WITHER)) {
            entity.removeStatusEffect(StatusEffects.WITHER);
        }
        if (entity.hasStatusEffect(StatusEffects.NAUSEA)) {
            entity.removeStatusEffect(StatusEffects.NAUSEA);
        }

        // Ambient protective particle
        if (world.getTime() % 25 == 0) {
            world.spawnParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    entity.getX(), entity.getY() + 0.8, entity.getZ(),
                    2, 0.3, 0.4, 0.3, 0.02
            );
        }
        return true;
    }
}
