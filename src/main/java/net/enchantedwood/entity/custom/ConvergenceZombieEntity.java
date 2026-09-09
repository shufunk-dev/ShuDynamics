package net.enchantedwood.entity.custom;

import net.enchantedwood.item.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class ConvergenceZombieEntity extends ZombieEntity {

    public ConvergenceZombieEntity(EntityType<? extends ZombieEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createConvergenceZombieAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.MAX_HEALTH, 40.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.ATTACK_DAMAGE, 5.0)
                .add(EntityAttributes.ARMOR, 4.0)
                .add(EntityAttributes.SPAWN_REINFORCEMENTS);
    }

    @Override
    public boolean tryAttack(ServerWorld world, Entity target) {
        boolean attacked = super.tryAttack(world, target);
        if (attacked && target instanceof LivingEntity living) {
            // Inflict debilitating status effects
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, 0));
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 80, 0));

            // Kinetic Ground Slam: Knockback is completely nullified if target has KINETIC_DAMPENING
            boolean hasKineticDampening = living.hasStatusEffect(net.enchantedwood.effect.ModStatusEffects.KINETIC_DAMPENING);
            if (!hasKineticDampening) {
                Vec3d kb = living.getEntityPos().subtract(this.getEntityPos()).normalize().multiply(1.2).add(0, 0.35, 0);
                living.setVelocity(kb);
                living.velocityDirty = true;
            }

            // Resonance shockwave visual & audio cue
            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 0.5, target.getZ(), 12, 0.4, 0.4, 0.4, 0.05);
            world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_WARDEN_ATTACK_IMPACT, SoundCategory.HOSTILE, 0.8f, 1.4f);
        }
        return attacked;
    }

    @Override
    protected void dropEquipment(ServerWorld world, DamageSource source, boolean causedByPlayer) {
        super.dropEquipment(world, source, causedByPlayer);
        if (causedByPlayer) {
            if (this.random.nextFloat() < 0.25f) {
                this.dropStack(world, new ItemStack(ModItems.WASABI_ROOT));
            }
            if (this.random.nextFloat() < 0.20f) {
                this.dropStack(world, new ItemStack(ModItems.SULFUR_DUST));
            }
        }
    }
}
