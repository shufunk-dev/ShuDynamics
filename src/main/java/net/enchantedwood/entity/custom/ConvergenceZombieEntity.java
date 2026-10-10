package net.enchantedwood.entity.custom;

import net.enchantedwood.item.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ConvergenceZombieEntity extends Zombie {

    public ConvergenceZombieEntity(EntityType<? extends Zombie> entityType, Level world) {
        super(entityType, world);
    }

    public static AttributeSupplier.Builder createConvergenceZombieAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE);
    }

    @Override
    public boolean doHurtTarget(ServerLevel world, Entity target) {
        boolean attacked = super.doHurtTarget(world, target);
        if (attacked && target instanceof LivingEntity living) {
            // Inflict debilitating status effects
            living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 0));
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));

            // Kinetic Ground Slam: Knockback is completely nullified if target has KINETIC_DAMPENING
            boolean hasKineticDampening = living.hasEffect(net.enchantedwood.effect.ModStatusEffects.KINETIC_DAMPENING);
            if (!hasKineticDampening) {
                Vec3 kb = living.position().subtract(this.position()).normalize().scale(1.2).add(0, 0.35, 0);
                living.setDeltaMovement(kb);
                living.needsSync = true;
            }

            // Resonance shockwave visual & audio cue
            world.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 0.5, target.getZ(), 12, 0.4, 0.4, 0.4, 0.05);
            world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.HOSTILE, 0.8f, 1.4f);
        }
        return attacked;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel world, DamageSource source, boolean causedByPlayer) {
        super.dropCustomDeathLoot(world, source, causedByPlayer);
        if (causedByPlayer) {
            if (this.random.nextFloat() < 0.25f) {
                this.spawnAtLocation(world, new ItemStack(ModItems.WASABI_ROOT));
            }
            if (this.random.nextFloat() < 0.20f) {
                this.spawnAtLocation(world, new ItemStack(ModItems.SULFUR_DUST));
            }
        }
    }
}
