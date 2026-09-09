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
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class ConvergenceSpiderEntity extends SpiderEntity {

    private int leapCooldown = 0;

    public ConvergenceSpiderEntity(EntityType<? extends SpiderEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createConvergenceSpiderAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.MAX_HEALTH, 30.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.35)
                .add(EntityAttributes.ATTACK_DAMAGE, 4.0);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.getEntityWorld().isClient() && this.getTarget() != null) {
            if (this.leapCooldown > 0) {
                this.leapCooldown--;
            } else if (this.isOnGround()) {
                double distSq = this.squaredDistanceTo(this.getTarget().getEntityPos());
                if (distSq >= 16.0 && distSq <= 81.0 && this.canSee(this.getTarget())) {
                    // Phase Leap: burst jump toward target
                    Vec3d leap = this.getTarget().getEntityPos().subtract(this.getEntityPos()).normalize().multiply(1.3).add(0, 0.42, 0);
                    this.setVelocity(leap);
                    this.velocityDirty = true;
                    this.leapCooldown = 100; // 5 second cooldown

                    if (this.getEntityWorld() instanceof ServerWorld sw) {
                        sw.spawnParticles(ParticleTypes.WITCH, this.getX(), this.getY() + 0.5, this.getZ(), 16, 0.4, 0.3, 0.4, 0.05);
                        sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1.0f, 1.8f);
                    }
                }
            }
        }
    }

    @Override
    public boolean tryAttack(ServerWorld world, Entity target) {
        boolean attacked = super.tryAttack(world, target);
        if (attacked && target instanceof LivingEntity living) {
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 80, 1)); // Poison II
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 60, 0));
        }
        return attacked;
    }

    @Override
    protected void dropEquipment(ServerWorld world, DamageSource source, boolean causedByPlayer) {
        super.dropEquipment(world, source, causedByPlayer);
        if (causedByPlayer) {
            if (this.random.nextFloat() < 0.25f) {
                this.dropStack(world, new ItemStack(ModItems.AVOCADO));
            }
            if (this.random.nextFloat() < 0.15f) {
                this.dropStack(world, new ItemStack(ModItems.SILICON));
            }
        }
    }
}
