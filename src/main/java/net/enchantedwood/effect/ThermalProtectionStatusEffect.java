package net.enchantedwood.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;

public class ThermalProtectionStatusEffect extends StatusEffect {
    public ThermalProtectionStatusEffect() {
        super(StatusEffectCategory.BENEFICIAL, 0xFF5400); // Blazing Orange
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyUpdateEffect(ServerWorld world, LivingEntity entity, int amplifier) {
        // Extinguish flames immediately
        if (entity.isOnFire()) {
            entity.extinguish();
        }

        // Prevent freezing in powder snow
        if (entity.getFrozenTicks() > 0) {
            entity.setFrozenTicks(0);
        }

        // Lava buoyancy & mobility
        if (entity.isInLava()) {
            entity.fallDistance = 0.0f;
            entity.setVelocity(entity.getVelocity().x * 1.15, Math.max(entity.getVelocity().y, 0.08), entity.getVelocity().z * 1.15);
            if (world.getTime() % 10 == 0) {
                world.spawnParticles(
                        ParticleTypes.FLAME,
                        entity.getX(), entity.getY() + 0.2, entity.getZ(),
                        2, 0.2, 0.1, 0.2, 0.01
                );
            }
        }
        return true;
    }
}
