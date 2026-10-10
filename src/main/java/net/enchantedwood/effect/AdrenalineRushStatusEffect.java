package net.enchantedwood.effect;

import net.enchantedwood.EnchantedWoodMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class AdrenalineRushStatusEffect extends MobEffect {
    public AdrenalineRushStatusEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x70E000); // Wasabi Lime Green
        addAttributeModifier(
                Attributes.ATTACK_SPEED,
                Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "adrenaline_attack_speed"),
                0.25, // +25% attack speed
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
        addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "adrenaline_movement_speed"),
                0.20, // +20% movement speed
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel world, LivingEntity entity, int amplifier) {
        // Clear debilitating debuffs
        if (entity.hasEffect(MobEffects.SLOWNESS)) {
            entity.removeEffect(MobEffects.SLOWNESS);
        }
        if (entity.hasEffect(MobEffects.MINING_FATIGUE)) {
            entity.removeEffect(MobEffects.MINING_FATIGUE);
        }
        if (entity.hasEffect(MobEffects.WEAKNESS)) {
            entity.removeEffect(MobEffects.WEAKNESS);
        }

        if (world.getGameTime() % 15 == 0) {
            world.sendParticles(
                    ParticleTypes.CRIT,
                    entity.getX(), entity.getY() + 0.5, entity.getZ(),
                    2, 0.2, 0.2, 0.2, 0.05
            );
        }
        return true;
    }
}
