package net.enchantedwood.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class ThermalProtectionStatusEffect extends MobEffect {
    public ThermalProtectionStatusEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFF5400); // Blazing Orange
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel world, LivingEntity entity, int amplifier) {
        // Extinguish flames immediately
        if (entity.isOnFire()) {
            entity.clearFire();
        }

        // Prevent freezing in powder snow
        if (entity.getTicksFrozen() > 0) {
            entity.setTicksFrozen(0);
        }

        // Lava buoyancy & mobility
        if (entity.isInLava()) {
            entity.fallDistance = 0.0f;
            entity.setDeltaMovement(entity.getDeltaMovement().x * 1.15, Math.max(entity.getDeltaMovement().y, 0.08), entity.getDeltaMovement().z * 1.15);
            if (world.getGameTime() % 10 == 0) {
                world.sendParticles(
                        ParticleTypes.FLAME,
                        entity.getX(), entity.getY() + 0.2, entity.getZ(),
                        2, 0.2, 0.1, 0.2, 0.01
                );
            }
        }
        return true;
    }
}
