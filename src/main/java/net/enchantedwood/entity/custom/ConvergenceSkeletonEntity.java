package net.enchantedwood.entity.custom;

import net.enchantedwood.item.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;

public class ConvergenceSkeletonEntity extends SkeletonEntity {

    private int blinkCooldown = 0;

    public ConvergenceSkeletonEntity(EntityType<? extends SkeletonEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createConvergenceSkeletonAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.MAX_HEALTH, 30.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.27)
                .add(EntityAttributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void initEquipment(net.minecraft.util.math.random.Random random, LocalDifficulty localDifficulty) {
        super.initEquipment(random, localDifficulty);
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
    }

    @Override
    public void tick() {
        super.tick();

        // Tactical Blink: If a hostile target gets too close (<3.5 blocks), teleport away
        if (!this.getEntityWorld().isClient() && this.getTarget() != null) {
            if (this.blinkCooldown > 0) {
                this.blinkCooldown--;
            } else if (this.squaredDistanceTo(this.getTarget().getEntityPos()) < 14.0) {
                Vec3d away = this.getEntityPos().subtract(this.getTarget().getEntityPos()).normalize().multiply(5.0);
                double targetX = this.getX() + away.x;
                double targetZ = this.getZ() + away.z;
                double targetY = this.getY();

                if (this.teleport(targetX, targetY, targetZ, true)) {
                    this.blinkCooldown = 160; // 8 second cooldown
                    if (this.getEntityWorld() instanceof ServerWorld sw) {
                        sw.spawnParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 1.0, this.getZ(), 20, 0.4, 0.4, 0.4, 0.1);
                        sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1.0f, 1.4f);
                    }
                }
            }
        }
    }

    @Override
    protected void dropEquipment(ServerWorld world, DamageSource source, boolean causedByPlayer) {
        super.dropEquipment(world, source, causedByPlayer);
        if (causedByPlayer) {
            if (this.random.nextFloat() < 0.20f) {
                this.dropStack(world, new ItemStack(ModItems.STARFRUIT));
            }
            if (this.random.nextFloat() < 0.15f) {
                this.dropStack(world, new ItemStack(ModItems.FIRE_CRYSTAL));
            }
        }
    }
}
