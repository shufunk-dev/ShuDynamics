package net.enchantedwood.entity.custom;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ResonancePylonEntity extends HostileEntity {

    @Nullable
    private ResonanceColossusEntity parentColossus;

    public ResonancePylonEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 20;
    }

    public static DefaultAttributeContainer.Builder createPylonAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.MAX_HEALTH, 50.0)
                .add(EntityAttributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(EntityAttributes.ARMOR, 6.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.0);
    }

    public void setParentColossus(@Nullable ResonanceColossusEntity colossus) {
        this.parentColossus = colossus;
    }

    @Override
    public boolean cannotDespawn() {
        return true;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();

        // Stationary lock
        this.setVelocity(0, 0, 0);

        if (this.getEntityWorld() instanceof ServerWorld sw) {
            // Particle beam to sky and parent
            if (this.age % 4 == 0) {
                sw.spawnParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 1.5, this.getZ(), 4, 0.2, 0.5, 0.2, 0.02);
                sw.spawnParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 1.0, this.getZ(), 6, 0.3, 0.3, 0.3, 0.05);
            }

            if (this.parentColossus != null && this.parentColossus.isAlive()) {
                if (this.age % 10 == 0) {
                    double dx = (this.parentColossus.getX() - this.getX()) / 8.0;
                    double dy = ((this.parentColossus.getY() + 2.0) - (this.getY() + 1.5)) / 8.0;
                    double dz = (this.parentColossus.getZ() - this.getZ()) / 8.0;
                    for (int i = 1; i <= 8; i++) {
                        sw.spawnParticles(ParticleTypes.PORTAL,
                                this.getX() + dx * i,
                                this.getY() + 1.5 + dy * i,
                                this.getZ() + dz * i,
                                1, 0, 0, 0, 0);
                    }
                }
            }
        }
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);
        if (this.getEntityWorld() instanceof ServerWorld sw) {
            sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), SoundCategory.HOSTILE, 1.8f, 0.6f);
            sw.spawnParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 1.0, this.getZ(), 2, 0, 0, 0, 0);
            sw.spawnParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 1.0, this.getZ(), 10, 0.5, 0.5, 0.5, 0.1);

            if (this.parentColossus != null && this.parentColossus.isAlive()) {
                this.parentColossus.onPylonDestroyed();
            }
        }
    }
}
