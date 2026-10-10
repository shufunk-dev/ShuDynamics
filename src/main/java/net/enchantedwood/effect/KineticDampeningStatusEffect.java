package net.enchantedwood.effect;

import net.enchantedwood.EnchantedWoodMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class KineticDampeningStatusEffect extends MobEffect {
    public KineticDampeningStatusEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFB703); // Honey Amber
        addAttributeModifier(
                Attributes.KNOCKBACK_RESISTANCE,
                Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "kinetic_knockback_resistance"),
                1.0, // 100% Knockback Resistance
                AttributeModifier.Operation.ADD_VALUE
        );
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel world, LivingEntity entity, int amplifier) {
        // Absorb impact and kinetic shock
        entity.fallDistance = 0.0f;

        if (world.getGameTime() % 20 == 0) {
            world.sendParticles(
                    ParticleTypes.FALLING_HONEY,
                    entity.getX(), entity.getY() + 0.8, entity.getZ(),
                    2, 0.25, 0.25, 0.25, 0.01
            );
        }
        return true;
    }
}
