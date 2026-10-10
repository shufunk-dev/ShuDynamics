package net.enchantedwood.entity.custom;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class ResonancePylonEntity extends Monster {

    @Nullable
    private ResonanceColossusEntity parentColossus;

    public ResonancePylonEntity(EntityType<? extends Monster> entityType, Level world) {
        super(entityType, world);
        this.xpReward = 20;
    }

    public static AttributeSupplier.Builder createPylonAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 50.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    public void setParentColossus(@Nullable ResonanceColossusEntity colossus) {
        this.parentColossus = colossus;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();

        // Stationary lock
        this.setDeltaMovement(0, 0, 0);

        if (this.level() instanceof ServerLevel sw) {
            // Particle beam to sky and parent
            if (this.tickCount % 4 == 0) {
                sw.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 1.5, this.getZ(), 4, 0.2, 0.5, 0.2, 0.02);
                sw.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 1.0, this.getZ(), 6, 0.3, 0.3, 0.3, 0.05);
            }

            if (this.parentColossus != null && this.parentColossus.isAlive()) {
                if (this.tickCount % 10 == 0) {
                    double dx = (this.parentColossus.getX() - this.getX()) / 8.0;
                    double dy = ((this.parentColossus.getY() + 2.0) - (this.getY() + 1.5)) / 8.0;
                    double dz = (this.parentColossus.getZ() - this.getZ()) / 8.0;
                    for (int i = 1; i <= 8; i++) {
                        sw.sendParticles(ParticleTypes.PORTAL,
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
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        if (this.level() instanceof ServerLevel sw) {
            sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.HOSTILE, 1.8f, 0.6f);
            sw.sendParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 1.0, this.getZ(), 2, 0, 0, 0, 0);
            sw.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 1.0, this.getZ(), 10, 0.5, 0.5, 0.5, 0.1);

            if (this.parentColossus != null && this.parentColossus.isAlive()) {
                this.parentColossus.onPylonDestroyed();
            }
        }
    }
}
