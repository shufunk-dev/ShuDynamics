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
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ConvergenceSpiderEntity extends Spider {

    private int leapCooldown = 0;

    public ConvergenceSpiderEntity(EntityType<? extends Spider> entityType, Level world) {
        super(entityType, world);
    }

    public static AttributeSupplier.Builder createConvergenceSpiderAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.ATTACK_DAMAGE, 4.0);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide() && this.getTarget() != null) {
            if (this.leapCooldown > 0) {
                this.leapCooldown--;
            } else if (this.onGround()) {
                double distSq = this.distanceToSqr(this.getTarget().position());
                if (distSq >= 16.0 && distSq <= 81.0 && this.hasLineOfSight(this.getTarget())) {
                    // Phase Leap: burst jump toward target
                    Vec3 leap = this.getTarget().position().subtract(this.position()).normalize().scale(1.3).add(0, 0.42, 0);
                    this.setDeltaMovement(leap);
                    this.needsSync = true;
                    this.leapCooldown = 100; // 5 second cooldown

                    if (this.level() instanceof ServerLevel sw) {
                        sw.sendParticles(ParticleTypes.WITCH, this.getX(), this.getY() + 0.5, this.getZ(), 16, 0.4, 0.3, 0.4, 0.05);
                        sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0f, 1.8f);
                    }
                }
            }
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel world, Entity target) {
        boolean attacked = super.doHurtTarget(world, target);
        if (attacked && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1)); // Poison II
            living.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 60, 0));
        }
        return attacked;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel world, DamageSource source, boolean causedByPlayer) {
        super.dropCustomDeathLoot(world, source, causedByPlayer);
        if (causedByPlayer) {
            if (this.random.nextFloat() < 0.25f) {
                this.spawnAtLocation(world, new ItemStack(ModItems.AVOCADO));
            }
            if (this.random.nextFloat() < 0.15f) {
                this.spawnAtLocation(world, new ItemStack(ModItems.SILICON));
            }
        }
    }
}
