package net.enchantedwood.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public class AcidProtectionStatusEffect extends MobEffect {
    public AcidProtectionStatusEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x52B788); // Emerald Mint Green
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true; // Apply every tick for continuous cleansing
    }

    @Override
    public boolean applyEffectTick(ServerLevel world, LivingEntity entity, int amplifier) {
        // Continuously cleanse corrosive and toxic effects
        if (entity.hasEffect(MobEffects.POISON)) {
            entity.removeEffect(MobEffects.POISON);
        }
        if (entity.hasEffect(MobEffects.WITHER)) {
            entity.removeEffect(MobEffects.WITHER);
        }
        if (entity.hasEffect(MobEffects.NAUSEA)) {
            entity.removeEffect(MobEffects.NAUSEA);
        }

        // Ambient protective particle
        if (world.getGameTime() % 25 == 0) {
            world.sendParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    entity.getX(), entity.getY() + 0.8, entity.getZ(),
                    2, 0.3, 0.4, 0.3, 0.02
            );
        }
        return true;
    }
}
