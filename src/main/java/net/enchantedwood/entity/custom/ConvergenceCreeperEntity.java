package net.enchantedwood.entity.custom;

import net.enchantedwood.item.ModItems;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

public class ConvergenceCreeperEntity extends CreeperEntity {

    private boolean cloudSpawned = false;

    public ConvergenceCreeperEntity(EntityType<? extends CreeperEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createConvergenceCreeperAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.MAX_HEALTH, 26.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.28);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.getEntityWorld().isClient()) {
            int fuse = this.getFuseSpeed();
            if (fuse > 0) {
                // Gravitational Pull: While hissing/priming, pulls nearby players inwards
                if (this.getEntityWorld() instanceof ServerWorld sw) {
                    Box pullBox = this.getBoundingBox().expand(5.5);
                    List<PlayerEntity> players = sw.getEntitiesByClass(PlayerEntity.class, pullBox, p -> !p.isCreative() && !p.isSpectator());

                    for (PlayerEntity player : players) {
                        boolean resistsPull = player.hasStatusEffect(net.enchantedwood.effect.ModStatusEffects.CELESTIAL_LEAP)
                                || player.hasStatusEffect(net.enchantedwood.effect.ModStatusEffects.KINETIC_DAMPENING);
                        if (!resistsPull) {
                            Vec3d pull = this.getEntityPos().subtract(player.getEntityPos()).normalize().multiply(0.08);
                            player.addVelocity(pull.x, pull.y + 0.02, pull.z);
                            player.velocityDirty = true;
                        }
                    }
                    sw.spawnParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 0.8, this.getZ(), 4, 0.3, 0.3, 0.3, 0.02);
                }

                // If about to detonate, prepare lingering corrosive acid cloud
                if (this.getLerpedFuseTime(1.0f) >= 0.90f && !this.cloudSpawned) {
                    this.cloudSpawned = true;
                    spawnCorrosiveAcidCloud();
                }
            }
        }
    }

    private void spawnCorrosiveAcidCloud() {
        if (this.getEntityWorld() instanceof ServerWorld sw) {
            AreaEffectCloudEntity cloud = new AreaEffectCloudEntity(sw, this.getX(), this.getY(), this.getZ());
            cloud.setRadius(3.5f);
            cloud.setRadiusOnUse(-0.5f);
            cloud.setWaitTime(10);
            cloud.setDuration(160); // 8 seconds
            cloud.setRadiusGrowth(-cloud.getRadius() / (float) cloud.getDuration());
            cloud.addEffect(new StatusEffectInstance(StatusEffects.POISON, 100, 1));
            cloud.addEffect(new StatusEffectInstance(StatusEffects.WITHER, 80, 0));
            cloud.setParticleType(ParticleTypes.WITCH);
            sw.spawnEntity(cloud);
        }
    }

    @Override
    protected void dropEquipment(ServerWorld world, DamageSource source, boolean causedByPlayer) {
        super.dropEquipment(world, source, causedByPlayer);
        if (causedByPlayer && this.random.nextFloat() < 0.25f) {
            this.dropStack(world, new ItemStack(ModItems.DRAGON_FRUIT));
        }
    }
}
