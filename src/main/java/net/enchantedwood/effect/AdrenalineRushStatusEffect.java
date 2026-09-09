package net.enchantedwood.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.enchantedwood.EnchantedWoodMod;

public class AdrenalineRushStatusEffect extends StatusEffect {
    public AdrenalineRushStatusEffect() {
        super(StatusEffectCategory.BENEFICIAL, 0x70E000); // Wasabi Lime Green
        addAttributeModifier(
                EntityAttributes.ATTACK_SPEED,
                Identifier.of(EnchantedWoodMod.MOD_ID, "adrenaline_attack_speed"),
                0.25, // +25% attack speed
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
        addAttributeModifier(
                EntityAttributes.MOVEMENT_SPEED,
                Identifier.of(EnchantedWoodMod.MOD_ID, "adrenaline_movement_speed"),
                0.20, // +20% movement speed
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyUpdateEffect(ServerWorld world, LivingEntity entity, int amplifier) {
        // Clear debilitating debuffs
        if (entity.hasStatusEffect(StatusEffects.SLOWNESS)) {
            entity.removeStatusEffect(StatusEffects.SLOWNESS);
        }
        if (entity.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            entity.removeStatusEffect(StatusEffects.MINING_FATIGUE);
        }
        if (entity.hasStatusEffect(StatusEffects.WEAKNESS)) {
            entity.removeStatusEffect(StatusEffects.WEAKNESS);
        }

        if (world.getTime() % 15 == 0) {
            world.spawnParticles(
                    ParticleTypes.CRIT,
                    entity.getX(), entity.getY() + 0.5, entity.getZ(),
                    2, 0.2, 0.2, 0.2, 0.05
            );
        }
        return true;
    }
}
