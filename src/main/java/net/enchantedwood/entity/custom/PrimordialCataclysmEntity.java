package net.enchantedwood.entity.custom;

import java.util.UUID;

import net.enchantedwood.block.ModBlocks;
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

public class PrimordialCataclysmEntity extends Monster {

    private final ServerBossEvent bossBar;
    private BlockPos altarPos;
    private int attackTimer = 0;
    private int phase = 1; // 1: 100-66%, 2: 66-33%, 3: <33%
    private boolean riftsSpawned = false;
    private boolean isResetting = false;

    public PrimordialCataclysmEntity(EntityType<? extends Monster> entityType, Level world) {
        super(entityType, world);
        this.bossBar = (ServerBossEvent) new ServerBossEvent(UUID.randomUUID(), 
                Component.literal("§d✦ The Primordial Cataclysm ✦"),
                BossEvent.BossBarColor.PINK,
                BossEvent.BossBarOverlay.NOTCHED_20
        ).setDarkenScreen(true);
        this.xpReward = 500;
    }

    public static AttributeSupplier.Builder createPrimordialCataclysmAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 2500.0)
                .add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.ATTACK_DAMAGE, 20.0)
                .add(Attributes.ARMOR, 20.0)
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
                player.sendSystemMessage(Component.literal("§c✦ The Primordial Cataclysm warps attacks from outside its arena! ✦"));
            }
            return false;
        }

        // Soft damage cap: maximum 50 damage per hit
        float cappedAmount = Math.min(amount, 50.0f);
        return super.hurtServer(world, source, cappedAmount);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.3, true));
        this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this, Player.class, 32.0f));

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
                sw.sendParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 1.5, this.getZ(), 3, 0, 0, 0, 0);
                sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.5f, 0.8f);
                for (ServerPlayer p : sw.players()) {
                    if (p.distanceToSqr(Vec3.atCenterOf(this.altarPos)) < (160.0 * 160.0)) {
                        p.sendOverlayMessage(Component.literal("§d✦ The Primordial Cataclysm has retreated to its altar to regenerate! ✦"));
                    }
                }
            }
        }

        if (this.isResetting) {
            this.setTarget(null);
            if (this.getHealth() < this.getMaxHealth()) {
                this.heal(50.0f);
                sw.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + 2.0, this.getZ(), 8, 0.5, 0.5, 0.5, 0.05);
            } else {
                this.isResetting = false;
                this.riftsSpawned = false;
                this.phase = 1;
            }
            return;
        }

        float healthPct = this.getHealth() / this.getMaxHealth();
        if (healthPct > 0.66f) {
            this.phase = 1;
        } else if (healthPct > 0.33f) {
            this.phase = 2;
        } else {
            this.phase = 3;
        }

        this.attackTimer++;

        // Radiant cosmic void aura
        if (this.attackTimer % 6 == 0) {
            sw.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 2.5, this.getZ(), 10, 0.8, 1.2, 0.8, 0.05);
            sw.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 2.0, this.getZ(), 15, 0.9, 0.9, 0.9, 0.15);
            sw.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 1.5, this.getZ(), 20, 1.0, 1.0, 1.0, 0.2);
        }

        LivingEntity target = this.getTarget();
        if (target == null) return;

        // PHASE 1: Reality Inversion & Void Shockwaves
        if (this.phase == 1) {
            if (this.attackTimer % 200 == 0) {
                performRealityInversion(sw);
            }
            if (this.attackTimer % 120 == 60) {
                performVoidShockwave(sw, target);
            }
        }
        // PHASE 2: Event Horizon & Dimensional Rifts
        else if (this.phase == 2) {
            if (!this.riftsSpawned) {
                this.riftsSpawned = true;
                spawnDimensionalHorrors(sw);
            }
            if (this.attackTimer % 180 == 0) {
                performSingularityEventHorizon(sw);
            }
            if (this.attackTimer % 140 == 70) {
                performCataclysmicBombardment(sw, target);
            }
        }
        // PHASE 3: Reality Collapse Flurry & Meltdown
        else {
            if (this.attackTimer % 60 == 0) {
                performHyperPhaseFlurry(sw, target);
            }
            if (this.attackTimer % 15 == 0) {
                performPrimordialMeltdownPulse(sw);
            }
        }
    }

    private void performRealityInversion(ServerLevel sw) {
        sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.HOSTILE, 2.5f, 0.3f);
        sw.sendParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 2.0, this.getZ(), 6, 0, 0, 0, 0);

        AABB arena = this.getBoundingBox().inflate(24.0, 10.0, 24.0);
        List<Player> players = sw.getEntitiesOfClass(Player.class, arena, p -> !p.isCreative() && !p.isSpectator());

        for (Player p : players) {
            boolean grounded = p.hasEffect(net.enchantedwood.effect.ModStatusEffects.KINETIC_DAMPENING)
                    || p.hasEffect(net.enchantedwood.effect.ModStatusEffects.CELESTIAL_LEAP);

            if (!grounded) {
                p.push(0, 1.4, 0);
                p.needsSync = true;
                p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 60, 1));
                p.hurtServer(sw, sw.damageSources().magic(), 12.0f);
            } else {
                p.sendSystemMessage(Component.literal("§a✔ Your culinary buff neutralized the Reality Inversion!"));
            }
        }
    }

    private void performVoidShockwave(ServerLevel sw, LivingEntity target) {
        sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.0f, 0.8f);
        Vec3 dir = target.position().subtract(this.position()).normalize();

        for (int i = 1; i <= 12; i++) {
            Vec3 pos = this.position().add(dir.scale(i)).add(0, 1.0, 0);
            sw.sendParticles(ParticleTypes.SONIC_BOOM, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
            sw.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 10, 0.4, 0.4, 0.4, 0.1);

            AABB hit = new AABB(pos.x - 1.5, pos.y - 1, pos.z - 1.5, pos.x + 1.5, pos.y + 2, pos.z + 1.5);
            for (Player p : sw.getEntitiesOfClass(Player.class, hit, p -> !p.isCreative() && !p.isSpectator())) {
                p.hurtServer(sw, sw.damageSources().magic(), 18.0f);
            }
        }
    }

    private void performSingularityEventHorizon(ServerLevel sw) {
        sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.HOSTILE, 2.5f, 0.2f);
        sw.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 2.0, this.getZ(), 200, 8.0, 4.0, 8.0, 0.4);

        AABB pullBox = this.getBoundingBox().inflate(24.0);
        List<Player> players = sw.getEntitiesOfClass(Player.class, pullBox, p -> !p.isCreative() && !p.isSpectator());

        for (Player p : players) {
            Vec3 pull = this.position().subtract(p.position()).normalize().scale(0.65);
            p.push(pull.x, 0.15, pull.z);
            p.needsSync = true;
        }
    }

    private void performCataclysmicBombardment(ServerLevel sw, LivingEntity target) {
        sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GHAST_SHOOT, SoundSource.HOSTILE, 2.0f, 0.5f);
        for (int i = 0; i < 7; i++) {
            double ox = (this.random.nextDouble() - 0.5) * 12.0;
            double oz = (this.random.nextDouble() - 0.5) * 12.0;
            Vec3 strike = target.position().add(ox, 0, oz);

            sw.sendParticles(ParticleTypes.LAVA, strike.x, strike.y + 0.2, strike.z, 30, 1.0, 1.0, 1.0, 0.15);
            sw.sendParticles(ParticleTypes.FLAME, strike.x, strike.y + 0.5, strike.z, 40, 0.8, 0.8, 0.8, 0.08);

            AABB hit = new AABB(strike.x - 2.5, strike.y - 1, strike.z - 2.5, strike.x + 2.5, strike.y + 4, strike.z + 2.5);
            for (Player p : sw.getEntitiesOfClass(Player.class, hit, p -> !p.isCreative() && !p.isSpectator())) {
                if (!p.hasEffect(net.enchantedwood.effect.ModStatusEffects.THERMAL_PROTECTION)) {
                    p.igniteForSeconds(10.0f);
                    p.hurtServer(sw, sw.damageSources().onFire(), 14.0f);
                } else {
                    p.sendOverlayMessage(Component.literal("§6✔ Thermal Protection absorbed the Cataclysmic Bombardment!"));
                }
            }
        }
    }

    private void spawnDimensionalHorrors(ServerLevel sw) {
        sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 2.0f, 0.8f);
        for (int i = -1; i <= 1; i += 2) {
            ConvergenceCreeperEntity creeper = ModEntities.CONVERGENCE_CREEPER.create(sw, net.minecraft.world.entity.EntitySpawnReason.EVENT);
            if (creeper != null) {
                creeper.snapTo(this.getX() + (i * 5), this.getY(), this.getZ() + 3, 0, 0);
                creeper.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 60, 1));
                sw.addFreshEntity(creeper);
            }
            ConvergenceSkeletonEntity skeleton = ModEntities.CONVERGENCE_SKELETON.create(sw, net.minecraft.world.entity.EntitySpawnReason.EVENT);
            if (skeleton != null) {
                skeleton.snapTo(this.getX() + (i * 5), this.getY(), this.getZ() - 3, 0, 0);
                skeleton.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 20 * 60, 1));
                sw.addFreshEntity(skeleton);
            }
        }
    }

    private void performHyperPhaseFlurry(ServerLevel sw, LivingEntity target) {
        Vec3 behind = target.position().add(target.getViewVector(1.0f).scale(-3.5));
        sw.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 1.5, this.getZ(), 50, 1.0, 1.5, 1.0, 0.3);

        this.randomTeleport(behind.x, behind.y, behind.z, true, state -> true);
        sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 2.0f, 1.0f);
        this.doHurtTarget(sw, target);
    }

    private void performPrimordialMeltdownPulse(ServerLevel sw) {
        AABB auraBox = this.getBoundingBox().inflate(16.0);
        List<Player> players = sw.getEntitiesOfClass(Player.class, auraBox, p -> !p.isCreative() && !p.isSpectator());

        for (Player p : players) {
            p.hurtServer(sw, sw.damageSources().magic(), 5.0f);
        }
        sw.sendParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 2.5, this.getZ(), 2, 0, 0, 0, 0);
    }

    @Override
    public boolean doHurtTarget(ServerLevel world, Entity target) {
        boolean attacked = super.doHurtTarget(world, target);
        if (attacked && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 2));
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 2));
            living.addEffect(new MobEffectInstance(MobEffects.WITHER, 120, 2));
            world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.HOSTILE, 1.8f, 0.6f);
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

            // The Pinnacle Creative-Tier Rewards
            this.spawnAtLocation(sw, new ItemStack(ModItems.RING_OF_GRAVITATIONAL_MASTERY, 1));
            this.spawnAtLocation(sw, new ItemStack(ModItems.INFINITE_DIMENSIONAL_MATRIX, 1));
            this.spawnAtLocation(sw, new ItemStack(ModBlocks.TROPHY_OF_OMNIPOTENCE.asItem(), 1));

            // Massive high-tier cache
            this.spawnAtLocation(sw, new ItemStack(ModItems.FIRE_CRYSTAL, 24));
            this.spawnAtLocation(sw, new ItemStack(ModItems.SULFUR_DUST, 24));
            this.spawnAtLocation(sw, new ItemStack(ModItems.DRAGON_FRUIT, 12));
            this.spawnAtLocation(sw, new ItemStack(ModItems.WASABI_ROOT, 12));
            this.spawnAtLocation(sw, new ItemStack(ModItems.STARFRUIT, 12));

            for (ServerPlayer p : sw.players()) {
                p.sendSystemMessage(Component.literal("§d✦ Reality stabilizes as The Primordial Cataclysm dissolves! You have conquered ShuDynamics! ✦"));
            }
        }
    }
}
