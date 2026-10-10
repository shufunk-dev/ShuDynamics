package net.enchantedwood.entity.custom;

import java.util.UUID;

import net.enchantedwood.block.custom.ResonanceAltarBlock;
import net.enchantedwood.entity.ModEntities;
import net.enchantedwood.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.List;

public class AscendantColossusEntity extends Monster {

    private final ServerBossEvent bossBar;
    private BlockPos altarPos;
    private int attackTimer = 0;
    private int phase = 1; // 1: 100-60%, 2: 60-25%, 3: <25%
    private boolean illusionsSpawned = false;
    private boolean isResetting = false;

    public AscendantColossusEntity(EntityType<? extends Monster> entityType, Level world) {
        super(entityType, world);
        this.bossBar = (ServerBossEvent) new ServerBossEvent(UUID.randomUUID(), 
                Component.literal("§4✦ The Ascendant Colossus ✦"),
                BossEvent.BossBarColor.RED,
                BossEvent.BossBarOverlay.NOTCHED_10
        ).setDarkenScreen(true);
        this.xpReward = 250;
    }

    public static AttributeSupplier.Builder createAscendantColossusAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1200.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 15.0)
                .add(Attributes.ARMOR, 16.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 64.0);
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
    public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
        if (this.isResetting) {
            return false;
        }
        if (source.getEntity() instanceof LivingEntity attacker && !isWithinArena(attacker)) {
            if (attacker instanceof Player player) {
                player.sendSystemMessage(Component.literal("§c✦ The Ascendant Colossus deflects attacks from outside its arena! ✦"));
            }
            return false;
        }

        // Soft damage cap: maximum 35 damage per hit
        float cappedAmount = Math.min(amount, 35.0f);
        return super.hurtServer(world, source, cappedAmount);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.25, true));
        this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this, Player.class, 24.0f));

        this.targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this, Player.class, true));
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
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossBar.removePlayer(player);
    }

    @Override
    public void tick() {
        super.tick();

        this.bossBar.setProgress(this.getHealth() / this.getMaxHealth());

        if (this.level().isClientSide()) {
            return;
        }

        if (!(this.level() instanceof ServerLevel sw)) {
            return;
        }

        if (this.altarPos == null) {
            this.altarPos = this.blockPosition();
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
            AABB arenaBox = new AABB(this.altarPos).inflate(64.0);
            List<Player> nearby = sw.getEntitiesOfClass(Player.class, arenaBox, p -> !p.isCreative() && !p.isSpectator());
            if (nearby.isEmpty()) {
                targetEscaped = true;
            }
        }

        if (outOfBounds || targetEscaped) {
            if (!this.isResetting) {
                this.isResetting = true;
                this.setTarget(null);
                this.randomTeleport(this.altarPos.getX() + 0.5, this.altarPos.getY() + 1.0, this.altarPos.getZ() + 0.5, true, state -> true);
                sw.sendParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 1.5, this.getZ(), 2, 0, 0, 0, 0);
                sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.5f, 0.8f);
                for (ServerPlayer p : sw.players()) {
                    if (p.distanceToSqr(Vec3.atCenterOf(this.altarPos)) < (160.0 * 160.0)) {
                        p.sendOverlayMessage(Component.literal("§4✦ The Ascendant Colossus has disengaged and returned to its altar to regenerate! ✦"));
                    }
                }
            }
        }

        if (this.isResetting) {
            this.setTarget(null);
            if (this.getHealth() < this.getMaxHealth()) {
                this.heal(30.0f);
                sw.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + 2.0, this.getZ(), 6, 0.5, 0.5, 0.5, 0.05);
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
            sw.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 2.2, this.getZ(), 8, 0.8, 0.8, 0.8, 0.05);
            sw.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 2.0, this.getZ(), 10, 0.6, 0.6, 0.6, 0.1);
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

    private void performGroundSlam(ServerLevel sw) {
        sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.0f, 0.6f);
        sw.sendParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 0.5, this.getZ(), 4, 0, 0, 0, 0);

        AABB impactBox = this.getBoundingBox().inflate(10.0, 4.0, 10.0);
        List<Player> players = sw.getEntitiesOfClass(Player.class, impactBox, p -> !p.isCreative() && !p.isSpectator());

        for (Player p : players) {
            boolean resisted = p.hasEffect(net.enchantedwood.effect.ModStatusEffects.CELESTIAL_LEAP) ||
                    p.hasEffect(net.enchantedwood.effect.ModStatusEffects.KINETIC_DAMPENING);

            if (!resisted) {
                p.hurtServer(sw, sw.damageSources().mobAttack(this), 16.0f);
                Vec3 knockback = p.position().subtract(this.position()).normalize().scale(2.0).add(0, 0.8, 0);
                p.setDeltaMovement(knockback);
                p.needsSync = true;
            } else {
                p.sendSystemMessage(Component.literal("§a✔ Your culinary buff absorbed the Ascendant Ground Slam!"));
            }
        }
    }

    private void performSingularityBarrage(ServerLevel sw, LivingEntity target) {
        sw.playSound(null, this.getX(), this.getY() + 2.0, this.getZ(), SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 1.8f, 0.7f);
        for (int i = 0; i < 4; i++) {
            double angle = i * (Math.PI / 2.0);
            double sx = this.getX() + Math.cos(angle) * 3.0;
            double sz = this.getZ() + Math.sin(angle) * 3.0;
            sw.sendParticles(ParticleTypes.SONIC_BOOM, sx, this.getY() + 2.0, sz, 1, 0, 0, 0, 0);

            // Explosive shockwave at target offset
            double tx = target.getX() + (this.random.nextDouble() - 0.5) * 4.0;
            double tz = target.getZ() + (this.random.nextDouble() - 0.5) * 4.0;
            sw.sendParticles(ParticleTypes.EXPLOSION, tx, target.getY() + 0.5, tz, 3, 0.3, 0.3, 0.3, 0.05);

            AABB hit = new AABB(tx - 2, target.getY() - 1, tz - 2, tx + 2, target.getY() + 2, tz + 2);
            for (Player p : sw.getEntitiesOfClass(Player.class, hit, p -> !p.isCreative() && !p.isSpectator())) {
                p.hurtServer(sw, sw.damageSources().magic(), 8.0f);
            }
        }
    }

    private void performVolcanicFirestorm(ServerLevel sw, LivingEntity target) {
        sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GHAST_SHOOT, SoundSource.HOSTILE, 2.0f, 0.6f);
        for (int i = 0; i < 5; i++) {
            double ox = (this.random.nextDouble() - 0.5) * 10.0;
            double oz = (this.random.nextDouble() - 0.5) * 10.0;
            Vec3 strike = target.position().add(ox, 0, oz);

            sw.sendParticles(ParticleTypes.LAVA, strike.x, strike.y + 0.2, strike.z, 25, 0.8, 0.8, 0.8, 0.1);
            sw.sendParticles(ParticleTypes.FLAME, strike.x, strike.y + 0.5, strike.z, 30, 0.6, 0.6, 0.6, 0.05);

            AABB hit = new AABB(strike.x - 2, strike.y - 1, strike.z - 2, strike.x + 2, strike.y + 3, strike.z + 2);
            for (Player p : sw.getEntitiesOfClass(Player.class, hit, p -> !p.isCreative() && !p.isSpectator())) {
                if (!p.hasEffect(net.enchantedwood.effect.ModStatusEffects.THERMAL_PROTECTION)) {
                    p.igniteForSeconds(8.0f);
                    p.hurtServer(sw, sw.damageSources().onFire(), 10.0f);
                } else {
                    p.sendOverlayMessage(Component.literal("§6✔ Thermal Protection absorbed the Ascendant Firestorm!"));
                }
            }
        }
    }

    private void performSingularityVortex(ServerLevel sw) {
        sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.HOSTILE, 2.0f, 0.4f);
        sw.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 2.0, this.getZ(), 120, 6.0, 3.0, 6.0, 0.3);

        AABB pullBox = this.getBoundingBox().inflate(18.0);
        List<Player> players = sw.getEntitiesOfClass(Player.class, pullBox, p -> !p.isCreative() && !p.isSpectator());

        for (Player p : players) {
            boolean resists = p.hasEffect(net.enchantedwood.effect.ModStatusEffects.KINETIC_DAMPENING)
                    || p.hasEffect(net.enchantedwood.effect.ModStatusEffects.CELESTIAL_LEAP);
            if (!resists) {
                Vec3 pull = this.position().subtract(p.position()).normalize().scale(0.4);
                p.push(pull.x, 0.1, pull.z);
                p.needsSync = true;
            }
        }
    }

    private void spawnShadowIllusions(ServerLevel sw) {
        sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.8f, 1.0f);
        for (int i = -1; i <= 1; i += 2) {
            ConvergenceSkeletonEntity minion = ModEntities.CONVERGENCE_SKELETON.create(sw, net.minecraft.world.entity.EntitySpawnReason.EVENT);
            if (minion != null) {
                minion.snapTo(this.getX() + (i * 4), this.getY(), this.getZ(), 0, 0);
                minion.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 60, 1));
                minion.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 20 * 60, 1));
                sw.addFreshEntity(minion);
            }
        }
    }

    private void performHyperPhaseDash(ServerLevel sw, LivingEntity target) {
        Vec3 behind = target.position().add(target.getViewVector(1.0f).scale(-3.0));
        sw.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 1.5, this.getZ(), 40, 0.8, 1.2, 0.8, 0.2);

        this.randomTeleport(behind.x, behind.y, behind.z, true, state -> true);
        sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.8f, 1.1f);
        this.doHurtTarget(sw, target);
    }

    private void performOverdriveMeltdown(ServerLevel sw) {
        AABB auraBox = this.getBoundingBox().inflate(14.0);
        List<Player> players = sw.getEntitiesOfClass(Player.class, auraBox, p -> !p.isCreative() && !p.isSpectator());

        for (Player p : players) {
            p.hurtServer(sw, sw.damageSources().magic(), 4.0f);
        }
        sw.sendParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 2.5, this.getZ(), 2, 0, 0, 0, 0);
    }

    @Override
    public boolean doHurtTarget(ServerLevel world, Entity target) {
        boolean attacked = super.doHurtTarget(world, target);
        if (attacked && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 160, 2));
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 2));
            living.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
            world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.HOSTILE, 1.5f, 0.7f);
        }
        return attacked;
    }

    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);

        if (this.level() instanceof ServerLevel sw) {
            if (this.altarPos != null && sw.getBlockState(this.altarPos).getBlock() instanceof ResonanceAltarBlock) {
                sw.setBlockAndUpdate(this.altarPos, sw.getBlockState(this.altarPos).setValue(ResonanceAltarBlock.ACTIVE, false));
            }

            // Guaranteed Ascendant Relic upgrades & Singularity Heart
            this.spawnAtLocation(sw, new ItemStack(ModItems.PRIMORDIAL_CATALYST, 2));
            this.spawnAtLocation(sw, new ItemStack(ModItems.SINGULARITY_HEART, 1));

            // High tier resources
            this.spawnAtLocation(sw, new ItemStack(ModItems.FIRE_CRYSTAL, 12));
            this.spawnAtLocation(sw, new ItemStack(ModItems.SULFUR_DUST, 12));
            this.spawnAtLocation(sw, new ItemStack(ModItems.DRAGON_FRUIT, 6));
            this.spawnAtLocation(sw, new ItemStack(ModItems.WASABI_ROOT, 6));
            this.spawnAtLocation(sw, new ItemStack(ModItems.STARFRUIT, 6));

            for (ServerPlayer p : sw.players()) {
                p.sendSystemMessage(Component.literal("§4✦ The Ascendant Colossus has fallen! Primordial Catalysts and the Singularity Heart are yours! ✦"));
            }
        }
    }
}
