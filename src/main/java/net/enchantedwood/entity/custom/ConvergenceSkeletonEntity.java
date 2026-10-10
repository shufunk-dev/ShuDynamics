package net.enchantedwood.entity.custom;

import net.enchantedwood.item.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ConvergenceSkeletonEntity extends Skeleton {

    private int blinkCooldown = 0;

    public ConvergenceSkeletonEntity(EntityType<? extends Skeleton> entityType, Level world) {
        super(entityType, world);
    }

    public static AttributeSupplier.Builder createConvergenceSkeletonAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void populateDefaultEquipmentSlots(net.minecraft.util.RandomSource random, DifficultyInstance localDifficulty) {
        super.populateDefaultEquipmentSlots(random, localDifficulty);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
    }

    @Override
    public void tick() {
        super.tick();

        // Tactical Blink: If a hostile target gets too close (<3.5 blocks), teleport away
        if (!this.level().isClientSide() && this.getTarget() != null) {
            if (this.blinkCooldown > 0) {
                this.blinkCooldown--;
            } else if (this.distanceToSqr(this.getTarget().position()) < 14.0) {
                Vec3 away = this.position().subtract(this.getTarget().position()).normalize().scale(5.0);
                double targetX = this.getX() + away.x;
                double targetZ = this.getZ() + away.z;
                double targetY = this.getY();

                if (this.randomTeleport(targetX, targetY, targetZ, true, state -> true)) {
                    this.blinkCooldown = 160; // 8 second cooldown
                    if (this.level() instanceof ServerLevel sw) {
                        sw.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 1.0, this.getZ(), 20, 0.4, 0.4, 0.4, 0.1);
                        sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0f, 1.4f);
                    }
                }
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel world, DamageSource source, boolean causedByPlayer) {
        super.dropCustomDeathLoot(world, source, causedByPlayer);
        if (causedByPlayer) {
            if (this.random.nextFloat() < 0.20f) {
                this.spawnAtLocation(world, new ItemStack(ModItems.STARFRUIT));
            }
            if (this.random.nextFloat() < 0.15f) {
                this.spawnAtLocation(world, new ItemStack(ModItems.FIRE_CRYSTAL));
            }
        }
    }
}
