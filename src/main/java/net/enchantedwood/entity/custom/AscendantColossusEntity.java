package net.enchantedwood.entity.custom;

import net.enchantedwood.block.custom.ResonanceAltarBlock;
import net.enchantedwood.entity.ModEntities;
import net.enchantedwood.item.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

public class AscendantColossusEntity extends HostileEntity {

    private final ServerBossBar bossBar;
    private BlockPos altarPos;
    private int attackTimer = 0;
    private int phase = 1; // 1: 100-60%, 2: 60-25%, 3: <25%
    private boolean illusionsSpawned = false;
    private boolean isResetting = false;

    public AscendantColossusEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.bossBar = (ServerBossBar) new ServerBossBar(
                Text.literal("§4✦ The Ascendant Colossus ✦"),
                BossBar.Color.RED,
                BossBar.Style.NOTCHED_10
        ).setDarkenSky(true);
        this.experiencePoints = 250;
    }

    public static DefaultAttributeContainer.Builder createAscendantColossusAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.MAX_HEALTH, 1200.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.28)
                .add(EntityAttributes.ATTACK_DAMAGE, 15.0)
                .add(EntityAttributes.ARMOR, 16.0)
                .add(EntityAttributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(EntityAttributes.FOLLOW_RANGE, 64.0);
    }

    public void setAltarPos(BlockPos pos) {
        this.altarPos = pos;
    }

    public boolean isWithinArena(Entity entity) {
        if (this.altarPos == null || entity == null) return true;
        double dx = entity.getX() - (this.altarPos.getX() + 0.5);
        double dz = entity.getZ() - (this.altarPos.getZ() + 0.5);
        double dy = Math.abs(entity.getY() - this.altarPos.getY());
        return (dx * dx + dz * dz <= 64.0 * 64.0) && dy <= 40.0;
    }

    @Override
    public void setTarget(@org.jetbrains.annotations.Nullable LivingEntity target) {
        if (target != null && (this.isResetting || !isWithinArena(target))) {
            super.setTarget(null);
            return;
        }
        super.setTarget(target);
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        if (this.isResetting) {
            return false;
        }
        if (source.getAttacker() instanceof LivingEntity attacker && !isWithinArena(attacker)) {
            if (attacker instanceof PlayerEntity player) {
                player.sendMessage(Text.literal("§c✦ The Ascendant Colossus deflects attacks from outside its arena! ✦"), true);
            }
            return false;
        }

        // Soft damage cap: maximum 35 damage per hit
        float cappedAmount = Math.min(amount, 35.0f);
        return super.damage(world, source, cappedAmount);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new net.minecraft.entity.ai.goal.MeleeAttackGoal(this, 1.25, true));
        this.goalSelector.add(2, new net.minecraft.entity.ai.goal.WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(3, new net.minecraft.entity.ai.goal.LookAtEntityGoal(this, PlayerEntity.class, 24.0f));

        this.targetSelector.add(1, new net.minecraft.entity.ai.goal.ActiveTargetGoal<>(this, PlayerEntity.class, true));
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
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        this.bossBar.addPlayer(player);
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        this.bossBar.removePlayer(player);
    }

    @Override
    public void tick() {
        super.tick();

        this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());

        if (this.getEntityWorld().isClient()) {
            return;
        }

        if (!(this.getEntityWorld() instanceof ServerWorld sw)) {
            return;
        }

        if (this.altarPos == null) {
            this.altarPos = this.getBlockPos();
        }

        // Arena Leash
        double hDistSq = (this.getX() - (this.altarPos.getX() + 0.5)) * (this.getX() - (this.altarPos.getX() + 0.5))
                + (this.getZ() - (this.altarPos.getZ() + 0.5)) * (this.getZ() - (this.altarPos.getZ() + 0.5));
        boolean outOfBounds = hDistSq > (64.0 * 64.0);

        LivingEntity currentTarget = this.getTarget();
        boolean targetEscaped = false;

        if (currentTarget != null) {
            if (!isWithinArena(currentTarget) || !currentTarget.isAlive()) {
                targetEscaped = true;
                this.setTarget(null);
            }
        } else if (this.getHealth() < this.getMaxHealth()) {
            Box arenaBox = new Box(this.altarPos).expand(64.0);
            List<PlayerEntity> nearby = sw.getEntitiesByClass(PlayerEntity.class, arenaBox, p -> !p.isCreative() && !p.isSpectator());
            if (nearby.isEmpty()) {
                targetEscaped = true;
            }
        }

        if (outOfBounds || targetEscaped) {
            if (!this.isResetting) {
                this.isResetting = true;
                this.setTarget(null);
                this.teleport(this.altarPos.getX() + 0.5, this.altarPos.getY() + 1.0, this.altarPos.getZ() + 0.5, true);
                sw.spawnParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 1.5, this.getZ(), 2, 0, 0, 0, 0);
                sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1.5f, 0.8f);
                for (ServerPlayerEntity p : sw.getPlayers()) {
                    if (p.squaredDistanceTo(this.altarPos.toCenterPos()) < (160.0 * 160.0)) {
                        p.sendMessage(Text.literal("§4✦ The Ascendant Colossus has disengaged and returned to its altar to regenerate! ✦"), false);
                    }
                }
            }
        }

        if (this.isResetting) {
            this.setTarget(null);
            if (this.getHealth() < this.getMaxHealth()) {
                this.heal(30.0f);
                sw.spawnParticles(ParticleTypes.HEART, this.getX(), this.getY() + 2.0, this.getZ(), 6, 0.5, 0.5, 0.5, 0.05);
            } else {
                this.isResetting = false;
                this.illusionsSpawned = false;
                this.phase = 1;
            }
            return;
        }

        float healthPct = this.getHealth() / this.getMaxHealth();
        if (healthPct > 0.60f) {
            this.phase = 1;
        } else if (healthPct > 0.25f) {
            this.phase = 2;
        } else {
            this.phase = 3;
        }

        this.attackTimer++;

        // Crimson & void particle aura
        if (this.attackTimer % 8 == 0) {
            sw.spawnParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 2.2, this.getZ(), 8, 0.8, 0.8, 0.8, 0.05);
            sw.spawnParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 2.0, this.getZ(), 10, 0.6, 0.6, 0.6, 0.1);
        }

        LivingEntity target = this.getTarget();
        if (target == null) return;

        // PHASE 1: Singularity Barrage & Ground Slam
        if (this.phase == 1) {
            if (this.attackTimer % 180 == 0) {
                performGroundSlam(sw);
            }
            if (this.attackTimer % 140 == 70) {
                performSingularityBarrage(sw, target);
            }
        }
        // PHASE 2: Volcanic Firestorm & Illusions
        else if (this.phase == 2) {
            if (!this.illusionsSpawned) {
                this.illusionsSpawned = true;
                spawnShadowIllusions(sw);
            }
            if (this.attackTimer % 160 == 0) {
                performVolcanicFirestorm(sw, target);
            }
            if (this.attackTimer % 200 == 100) {
                performSingularityVortex(sw);
            }
        }
        // PHASE 3: Overdrive Meltdown & Hyper Dash
        else {
            if (this.attackTimer % 80 == 0) {
                performHyperPhaseDash(sw, target);
            }
            if (this.attackTimer % 20 == 0) {
                performOverdriveMeltdown(sw);
            }
        }
    }

    private void performGroundSlam(ServerWorld sw) {
        sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 2.0f, 0.6f);
        sw.spawnParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 0.5, this.getZ(), 4, 0, 0, 0, 0);

        Box impactBox = this.getBoundingBox().expand(10.0, 4.0, 10.0);
        List<PlayerEntity> players = sw.getEntitiesByClass(PlayerEntity.class, impactBox, p -> !p.isCreative() && !p.isSpectator());

        for (PlayerEntity p : players) {
            boolean resisted = p.hasStatusEffect(net.enchantedwood.effect.ModStatusEffects.CELESTIAL_LEAP) ||
                    p.hasStatusEffect(net.enchantedwood.effect.ModStatusEffects.KINETIC_DAMPENING);

            if (!resisted) {
                p.damage(sw, sw.getDamageSources().mobAttack(this), 16.0f);
                Vec3d knockback = p.getEntityPos().subtract(this.getEntityPos()).normalize().multiply(2.0).add(0, 0.8, 0);
                p.setVelocity(knockback);
                p.velocityDirty = true;
            } else {
                p.sendMessage(Text.literal("§a✔ Your culinary buff absorbed the Ascendant Ground Slam!"), true);
            }
        }
    }

    private void performSingularityBarrage(ServerWorld sw, LivingEntity target) {
        sw.playSound(null, this.getX(), this.getY() + 2.0, this.getZ(), SoundEvents.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1.8f, 0.7f);
        for (int i = 0; i < 4; i++) {
            double angle = i * (Math.PI / 2.0);
            double sx = this.getX() + Math.cos(angle) * 3.0;
            double sz = this.getZ() + Math.sin(angle) * 3.0;
            sw.spawnParticles(ParticleTypes.SONIC_BOOM, sx, this.getY() + 2.0, sz, 1, 0, 0, 0, 0);

            // Explosive shockwave at target offset
            double tx = target.getX() + (this.random.nextDouble() - 0.5) * 4.0;
            double tz = target.getZ() + (this.random.nextDouble() - 0.5) * 4.0;
            sw.spawnParticles(ParticleTypes.EXPLOSION, tx, target.getY() + 0.5, tz, 3, 0.3, 0.3, 0.3, 0.05);

            Box hit = new Box(tx - 2, target.getY() - 1, tz - 2, tx + 2, target.getY() + 2, tz + 2);
            for (PlayerEntity p : sw.getEntitiesByClass(PlayerEntity.class, hit, p -> !p.isCreative() && !p.isSpectator())) {
                p.damage(sw, sw.getDamageSources().magic(), 8.0f);
            }
        }
    }

    private void performVolcanicFirestorm(ServerWorld sw, LivingEntity target) {
        sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_GHAST_SHOOT, SoundCategory.HOSTILE, 2.0f, 0.6f);
        for (int i = 0; i < 5; i++) {
            double ox = (this.random.nextDouble() - 0.5) * 10.0;
            double oz = (this.random.nextDouble() - 0.5) * 10.0;
            Vec3d strike = target.getEntityPos().add(ox, 0, oz);

            sw.spawnParticles(ParticleTypes.LAVA, strike.x, strike.y + 0.2, strike.z, 25, 0.8, 0.8, 0.8, 0.1);
            sw.spawnParticles(ParticleTypes.FLAME, strike.x, strike.y + 0.5, strike.z, 30, 0.6, 0.6, 0.6, 0.05);

            Box hit = new Box(strike.x - 2, strike.y - 1, strike.z - 2, strike.x + 2, strike.y + 3, strike.z + 2);
            for (PlayerEntity p : sw.getEntitiesByClass(PlayerEntity.class, hit, p -> !p.isCreative() && !p.isSpectator())) {
                if (!p.hasStatusEffect(net.enchantedwood.effect.ModStatusEffects.THERMAL_PROTECTION)) {
                    p.setOnFireFor(8.0f);
                    p.damage(sw, sw.getDamageSources().onFire(), 10.0f);
                } else {
                    p.sendMessage(Text.literal("§6✔ Thermal Protection absorbed the Ascendant Firestorm!"), true);
                }
            }
        }
    }

    private void performSingularityVortex(ServerWorld sw) {
        sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.HOSTILE, 2.0f, 0.4f);
        sw.spawnParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 2.0, this.getZ(), 120, 6.0, 3.0, 6.0, 0.3);

        Box pullBox = this.getBoundingBox().expand(18.0);
        List<PlayerEntity> players = sw.getEntitiesByClass(PlayerEntity.class, pullBox, p -> !p.isCreative() && !p.isSpectator());

        for (PlayerEntity p : players) {
            boolean resists = p.hasStatusEffect(net.enchantedwood.effect.ModStatusEffects.KINETIC_DAMPENING)
                    || p.hasStatusEffect(net.enchantedwood.effect.ModStatusEffects.CELESTIAL_LEAP);
            if (!resists) {
                Vec3d pull = this.getEntityPos().subtract(p.getEntityPos()).normalize().multiply(0.4);
                p.addVelocity(pull.x, 0.1, pull.z);
                p.velocityDirty = true;
            }
        }
    }

    private void spawnShadowIllusions(ServerWorld sw) {
        sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 1.8f, 1.0f);
        for (int i = -1; i <= 1; i += 2) {
            ConvergenceSkeletonEntity minion = ModEntities.CONVERGENCE_SKELETON.create(sw, net.minecraft.entity.SpawnReason.EVENT);
            if (minion != null) {
                minion.refreshPositionAndAngles(this.getX() + (i * 4), this.getY(), this.getZ(), 0, 0);
                minion.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20 * 60, 1));
                minion.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 20 * 60, 1));
                sw.spawnEntity(minion);
            }
        }
    }

    private void performHyperPhaseDash(ServerWorld sw, LivingEntity target) {
        Vec3d behind = target.getEntityPos().add(target.getRotationVec(1.0f).multiply(-3.0));
        sw.spawnParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 1.5, this.getZ(), 40, 0.8, 1.2, 0.8, 0.2);

        this.teleport(behind.x, behind.y, behind.z, true);
        sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1.8f, 1.1f);
        this.tryAttack(sw, target);
    }

    private void performOverdriveMeltdown(ServerWorld sw) {
        Box auraBox = this.getBoundingBox().expand(14.0);
        List<PlayerEntity> players = sw.getEntitiesByClass(PlayerEntity.class, auraBox, p -> !p.isCreative() && !p.isSpectator());

        for (PlayerEntity p : players) {
            p.damage(sw, sw.getDamageSources().magic(), 4.0f);
        }
        sw.spawnParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 2.5, this.getZ(), 2, 0, 0, 0, 0);
    }

    @Override
    public boolean tryAttack(ServerWorld world, Entity target) {
        boolean attacked = super.tryAttack(world, target);
        if (attacked && target instanceof LivingEntity living) {
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 160, 2));
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 160, 2));
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 100, 1));
            world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_WARDEN_ATTACK_IMPACT, SoundCategory.HOSTILE, 1.5f, 0.7f);
        }
        return attacked;
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);

        if (this.getEntityWorld() instanceof ServerWorld sw) {
            if (this.altarPos != null && sw.getBlockState(this.altarPos).getBlock() instanceof ResonanceAltarBlock) {
                sw.setBlockState(this.altarPos, sw.getBlockState(this.altarPos).with(ResonanceAltarBlock.ACTIVE, false));
            }

            // Guaranteed Ascendant Relic upgrades & Singularity Heart
            this.dropStack(sw, new ItemStack(ModItems.PRIMORDIAL_CATALYST, 2));
            this.dropStack(sw, new ItemStack(ModItems.SINGULARITY_HEART, 1));

            // High tier resources
            this.dropStack(sw, new ItemStack(ModItems.FIRE_CRYSTAL, 12));
            this.dropStack(sw, new ItemStack(ModItems.SULFUR_DUST, 12));
            this.dropStack(sw, new ItemStack(ModItems.DRAGON_FRUIT, 6));
            this.dropStack(sw, new ItemStack(ModItems.WASABI_ROOT, 6));
            this.dropStack(sw, new ItemStack(ModItems.STARFRUIT, 6));

            for (ServerPlayerEntity p : sw.getPlayers()) {
                p.sendMessage(Text.literal("§4✦ The Ascendant Colossus has fallen! Primordial Catalysts and the Singularity Heart are yours! ✦"), false);
            }
        }
    }
}
