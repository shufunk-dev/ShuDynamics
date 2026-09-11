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

    private int customFuse = 0;
    private boolean burstDetonated = false;

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
                this.customFuse++;

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

            }
        }
    }

    @Override
    public void remove(net.minecraft.entity.Entity.RemovalReason reason) {
        if (!this.getEntityWorld().isClient() && !this.burstDetonated) {
            detonateCorrosiveBurst();
        }
        super.remove(reason);
    }

    private void detonateCorrosiveBurst() {
        if (this.burstDetonated) return;
        this.burstDetonated = true;

        if (this.getEntityWorld() instanceof ServerWorld sw) {
            Vec3d center = this.getEntityPos();

            // 1. Direct Blast Affliction: Guarantees players within 7 blocks of the blast get poisoned & withered
            List<net.minecraft.server.network.ServerPlayerEntity> players = sw.getPlayers(p -> p.squaredDistanceTo(center) <= 49.0 && !p.isCreative() && !p.isSpectator());
            for (net.minecraft.server.network.ServerPlayerEntity p : players) {
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 160, 1)); // Poison II for 8s
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 100, 0)); // Wither I for 5s
            }

            // Also afflict other nearby living entities
            Box blastBox = Box.of(center, 14.0, 10.0, 14.0);
            List<net.minecraft.entity.LivingEntity> targets = sw.getEntitiesByClass(net.minecraft.entity.LivingEntity.class, blastBox, e -> e != this && e.isAlive() && !(e instanceof PlayerEntity));
            for (net.minecraft.entity.LivingEntity target : targets) {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 160, 1));
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 100, 0));
            }

            // 2. Lingering Acid Cloud with zero wait time
            AreaEffectCloudEntity cloud = new AreaEffectCloudEntity(sw, center.x, center.y, center.z);
            cloud.setRadius(5.0f);
            cloud.setRadiusOnUse(-0.5f);
            cloud.setWaitTime(0); // Affects instantly
            cloud.setDuration(240); // 12 seconds
            cloud.setRadiusGrowth(-cloud.getRadius() / (float) cloud.getDuration());
            cloud.addEffect(new StatusEffectInstance(StatusEffects.POISON, 160, 1));
            cloud.addEffect(new StatusEffectInstance(StatusEffects.WITHER, 100, 0));
            cloud.setParticleType(ParticleTypes.WITCH);
            sw.spawnEntity(cloud);

            // 3. Acidic burst particles & sound
            sw.spawnParticles(ParticleTypes.WITCH, center.x, center.y + 0.5, center.z, 50, 1.5, 0.8, 1.5, 0.08);
            sw.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y + 0.5, center.z, 30, 1.0, 0.5, 1.0, 0.05);
            sw.playSound(null, center.x, center.y, center.z, net.minecraft.sound.SoundEvents.ENTITY_SPLASH_POTION_BREAK, net.minecraft.sound.SoundCategory.HOSTILE, 1.0f, 0.6f);
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
