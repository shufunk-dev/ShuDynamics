package net.enchantedwood.entity.custom;

import net.enchantedwood.item.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.List;

public class ConvergenceCreeperEntity extends Creeper {

    private int customFuse = 0;
    private boolean burstDetonated = false;

    public ConvergenceCreeperEntity(EntityType<? extends Creeper> entityType, Level world) {
        super(entityType, world);
    }

    public static AttributeSupplier.Builder createConvergenceCreeperAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 26.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            int fuse = this.getSwellDir();
            if (fuse > 0) {
                this.customFuse++;

                // Gravitational Pull: While hissing/priming, pulls nearby players inwards
                if (this.level() instanceof ServerLevel sw) {
                    AABB pullBox = this.getBoundingBox().inflate(5.5);
                    List<Player> players = sw.getEntitiesOfClass(Player.class, pullBox, p -> !p.isCreative() && !p.isSpectator());

                    for (Player player : players) {
                        boolean resistsPull = player.hasEffect(net.enchantedwood.effect.ModStatusEffects.CELESTIAL_LEAP)
                                || player.hasEffect(net.enchantedwood.effect.ModStatusEffects.KINETIC_DAMPENING);
                        if (!resistsPull) {
                            Vec3 pull = this.position().subtract(player.position()).normalize().scale(0.08);
                            player.push(pull.x, pull.y + 0.02, pull.z);
                            player.needsSync = true;
                        }
                    }
                    sw.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 0.8, this.getZ(), 4, 0.3, 0.3, 0.3, 0.02);
                }

            }
        }
    }

    @Override
    public void remove(net.minecraft.world.entity.Entity.RemovalReason reason) {
        if (!this.level().isClientSide() && !this.burstDetonated) {
            detonateCorrosiveBurst();
        }
        super.remove(reason);
    }

    private void detonateCorrosiveBurst() {
        if (this.burstDetonated) return;
        this.burstDetonated = true;

        if (this.level() instanceof ServerLevel sw) {
            Vec3 center = this.position();

            // 1. Direct Blast Affliction: Guarantees players within 7 blocks of the blast get poisoned & withered
            List<net.minecraft.server.level.ServerPlayer> players = sw.getPlayers(p -> p.distanceToSqr(center) <= 49.0 && !p.isCreative() && !p.isSpectator());
            for (net.minecraft.server.level.ServerPlayer p : players) {
                p.addEffect(new MobEffectInstance(MobEffects.POISON, 160, 1)); // Poison II for 8s
                p.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0)); // Wither I for 5s
            }

            // Also afflict other nearby living entities
            AABB blastBox = AABB.ofSize(center, 14.0, 10.0, 14.0);
            List<net.minecraft.world.entity.LivingEntity> targets = sw.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, blastBox, e -> e != this && e.isAlive() && !(e instanceof Player));
            for (net.minecraft.world.entity.LivingEntity target : targets) {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 160, 1));
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0));
            }

            // 2. Lingering Acid Cloud with zero wait time
            AreaEffectCloud cloud = new AreaEffectCloud(sw, center.x, center.y, center.z);
            cloud.setRadius(5.0f);
            cloud.setRadiusOnUse(-0.5f);
            cloud.setWaitTime(0); // Affects instantly
            cloud.setDuration(240); // 12 seconds
            cloud.setRadiusPerTick(-cloud.getRadius() / (float) cloud.getDuration());
            cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 160, 1));
            cloud.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0));
            cloud.setCustomParticle(ParticleTypes.WITCH);
            sw.addFreshEntity(cloud);

            // 3. Acidic burst particles & sound
            sw.sendParticles(ParticleTypes.WITCH, center.x, center.y + 0.5, center.z, 50, 1.5, 0.8, 1.5, 0.08);
            sw.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y + 0.5, center.z, 30, 1.0, 0.5, 1.0, 0.05);
            sw.playSound(null, center.x, center.y, center.z, net.minecraft.sounds.SoundEvents.SPLASH_POTION_BREAK, net.minecraft.sounds.SoundSource.HOSTILE, 1.0f, 0.6f);
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel world, DamageSource source, boolean causedByPlayer) {
        super.dropCustomDeathLoot(world, source, causedByPlayer);
        if (causedByPlayer && this.random.nextFloat() < 0.25f) {
            this.spawnAtLocation(world, new ItemStack(ModItems.DRAGON_FRUIT));
        }
    }
}
