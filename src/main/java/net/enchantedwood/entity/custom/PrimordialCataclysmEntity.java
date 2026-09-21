package net.enchantedwood.entity.custom;

import net.enchantedwood.block.ModBlocks;
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

public class PrimordialCataclysmEntity extends HostileEntity {

    private final ServerBossBar bossBar;
    private BlockPos altarPos;
    private int attackTimer = 0;
    private int phase = 1; // 1: 100-66%, 2: 66-33%, 3: <33%
    private boolean riftsSpawned = false;
    private boolean isResetting = false;

    public PrimordialCataclysmEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.bossBar = (ServerBossBar) new ServerBossBar(
                Text.literal("§d✦ The Primordial Cataclysm ✦"),
                BossBar.Color.PINK,
                BossBar.Style.NOTCHED_20
        ).setDarkenSky(true);
        this.experiencePoints = 500;
    }

    public static DefaultAttributeContainer.Builder createPrimordialCataclysmAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.MAX_HEALTH, 2500.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.30)
                .add(EntityAttributes.ATTACK_DAMAGE, 20.0)
                .add(EntityAttributes.ARMOR, 20.0)
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
                player.sendMessage(Text.literal("§c✦ The Primordial Cataclysm warps attacks from outside its arena! ✦"), true);
            }
            return false;
        }

        // Soft damage cap: maximum 50 damage per hit
        float cappedAmount = Math.min(amount, 50.0f);
        return super.damage(world, source, cappedAmount);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new net.minecraft.entity.ai.goal.MeleeAttackGoal(this, 1.3, true));
        this.goalSelector.add(2, new net.minecraft.entity.ai.goal.WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(3, new net.minecraft.entity.ai.goal.LookAtEntityGoal(this, PlayerEntity.class, 32.0f));

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
                sw.spawnParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 1.5, this.getZ(), 3, 0, 0, 0, 0);
                sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1.5f, 0.8f);
                for (ServerPlayerEntity p : sw.getPlayers()) {
                    if (p.squaredDistanceTo(this.altarPos.toCenterPos()) < (160.0 * 160.0)) {
                        p.sendMessage(Text.literal("§d✦ The Primordial Cataclysm has retreated to its altar to regenerate! ✦"), false);
                    }
                }
            }
        }

        if (this.isResetting) {
            this.setTarget(null);
            if (this.getHealth() < this.getMaxHealth()) {
                this.heal(50.0f);
                sw.spawnParticles(ParticleTypes.HEART, this.getX(), this.getY() + 2.0, this.getZ(), 8, 0.5, 0.5, 0.5, 0.05);
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
            sw.spawnParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 2.5, this.getZ(), 10, 0.8, 1.2, 0.8, 0.05);
            sw.spawnParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 2.0, this.getZ(), 15, 0.9, 0.9, 0.9, 0.15);
            sw.spawnParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 1.5, this.getZ(), 20, 1.0, 1.0, 1.0, 0.2);
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

    private void performRealityInversion(ServerWorld sw) {
        sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.HOSTILE, 2.5f, 0.3f);
        sw.spawnParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 2.0, this.getZ(), 6, 0, 0, 0, 0);

        Box arena = this.getBoundingBox().expand(24.0, 10.0, 24.0);
        List<PlayerEntity> players = sw.getEntitiesByClass(PlayerEntity.class, arena, p -> !p.isCreative() && !p.isSpectator());

        for (PlayerEntity p : players) {
            boolean grounded = p.hasStatusEffect(net.enchantedwood.effect.ModStatusEffects.KINETIC_DAMPENING)
                    || p.hasStatusEffect(net.enchantedwood.effect.ModStatusEffects.CELESTIAL_LEAP);

            if (!grounded) {
                p.addVelocity(0, 1.4, 0);
                p.velocityDirty = true;
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 60, 1));
                p.damage(sw, sw.getDamageSources().magic(), 12.0f);
            } else {
                p.sendMessage(Text.literal("§a✔ Your culinary buff neutralized the Reality Inversion!"), true);
            }
        }
    }

    private void performVoidShockwave(ServerWorld sw, LivingEntity target) {
        sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 2.0f, 0.8f);
        Vec3d dir = target.getEntityPos().subtract(this.getEntityPos()).normalize();

        for (int i = 1; i <= 12; i++) {
            Vec3d pos = this.getEntityPos().add(dir.multiply(i)).add(0, 1.0, 0);
            sw.spawnParticles(ParticleTypes.SONIC_BOOM, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
            sw.spawnParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 10, 0.4, 0.4, 0.4, 0.1);

            Box hit = new Box(pos.x - 1.5, pos.y - 1, pos.z - 1.5, pos.x + 1.5, pos.y + 2, pos.z + 1.5);
            for (PlayerEntity p : sw.getEntitiesByClass(PlayerEntity.class, hit, p -> !p.isCreative() && !p.isSpectator())) {
                p.damage(sw, sw.getDamageSources().magic(), 18.0f);
            }
        }
    }

    private void performSingularityEventHorizon(ServerWorld sw) {
        sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), SoundCategory.HOSTILE, 2.5f, 0.2f);
        sw.spawnParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 2.0, this.getZ(), 200, 8.0, 4.0, 8.0, 0.4);

        Box pullBox = this.getBoundingBox().expand(24.0);
        List<PlayerEntity> players = sw.getEntitiesByClass(PlayerEntity.class, pullBox, p -> !p.isCreative() && !p.isSpectator());

        for (PlayerEntity p : players) {
            Vec3d pull = this.getEntityPos().subtract(p.getEntityPos()).normalize().multiply(0.65);
            p.addVelocity(pull.x, 0.15, pull.z);
            p.velocityDirty = true;
        }
    }

    private void performCataclysmicBombardment(ServerWorld sw, LivingEntity target) {
        sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_GHAST_SHOOT, SoundCategory.HOSTILE, 2.0f, 0.5f);
        for (int i = 0; i < 7; i++) {
            double ox = (this.random.nextDouble() - 0.5) * 12.0;
            double oz = (this.random.nextDouble() - 0.5) * 12.0;
            Vec3d strike = target.getEntityPos().add(ox, 0, oz);

            sw.spawnParticles(ParticleTypes.LAVA, strike.x, strike.y + 0.2, strike.z, 30, 1.0, 1.0, 1.0, 0.15);
            sw.spawnParticles(ParticleTypes.FLAME, strike.x, strike.y + 0.5, strike.z, 40, 0.8, 0.8, 0.8, 0.08);

            Box hit = new Box(strike.x - 2.5, strike.y - 1, strike.z - 2.5, strike.x + 2.5, strike.y + 4, strike.z + 2.5);
            for (PlayerEntity p : sw.getEntitiesByClass(PlayerEntity.class, hit, p -> !p.isCreative() && !p.isSpectator())) {
                if (!p.hasStatusEffect(net.enchantedwood.effect.ModStatusEffects.THERMAL_PROTECTION)) {
                    p.setOnFireFor(10.0f);
                    p.damage(sw, sw.getDamageSources().onFire(), 14.0f);
                } else {
                    p.sendMessage(Text.literal("§6✔ Thermal Protection absorbed the Cataclysmic Bombardment!"), true);
                }
            }
        }
    }

    private void spawnDimensionalHorrors(ServerWorld sw) {
        sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 2.0f, 0.8f);
        for (int i = -1; i <= 1; i += 2) {
            ConvergenceCreeperEntity creeper = ModEntities.CONVERGENCE_CREEPER.create(sw, net.minecraft.entity.SpawnReason.EVENT);
            if (creeper != null) {
                creeper.refreshPositionAndAngles(this.getX() + (i * 5), this.getY(), this.getZ() + 3, 0, 0);
                creeper.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20 * 60, 1));
                sw.spawnEntity(creeper);
            }
            ConvergenceSkeletonEntity skeleton = ModEntities.CONVERGENCE_SKELETON.create(sw, net.minecraft.entity.SpawnReason.EVENT);
            if (skeleton != null) {
                skeleton.refreshPositionAndAngles(this.getX() + (i * 5), this.getY(), this.getZ() - 3, 0, 0);
                skeleton.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 20 * 60, 1));
                sw.spawnEntity(skeleton);
            }
        }
    }

    private void performHyperPhaseFlurry(ServerWorld sw, LivingEntity target) {
        Vec3d behind = target.getEntityPos().add(target.getRotationVec(1.0f).multiply(-3.5));
        sw.spawnParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 1.5, this.getZ(), 50, 1.0, 1.5, 1.0, 0.3);

        this.teleport(behind.x, behind.y, behind.z, true);
        sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 2.0f, 1.0f);
        this.tryAttack(sw, target);
    }

    private void performPrimordialMeltdownPulse(ServerWorld sw) {
        Box auraBox = this.getBoundingBox().expand(16.0);
        List<PlayerEntity> players = sw.getEntitiesByClass(PlayerEntity.class, auraBox, p -> !p.isCreative() && !p.isSpectator());

        for (PlayerEntity p : players) {
            p.damage(sw, sw.getDamageSources().magic(), 5.0f);
        }
        sw.spawnParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 2.5, this.getZ(), 2, 0, 0, 0, 0);
    }

    @Override
    public boolean tryAttack(ServerWorld world, Entity target) {
        boolean attacked = super.tryAttack(world, target);
        if (attacked && target instanceof LivingEntity living) {
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 200, 2));
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 200, 2));
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 120, 2));
            world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_WARDEN_ATTACK_IMPACT, SoundCategory.HOSTILE, 1.8f, 0.6f);
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

            // The Pinnacle Creative-Tier Rewards
            this.dropStack(sw, new ItemStack(ModItems.RING_OF_GRAVITATIONAL_MASTERY, 1));
            this.dropStack(sw, new ItemStack(ModItems.INFINITE_DIMENSIONAL_MATRIX, 1));
            this.dropStack(sw, new ItemStack(ModBlocks.TROPHY_OF_OMNIPOTENCE.asItem(), 1));

            // Massive high-tier cache
            this.dropStack(sw, new ItemStack(ModItems.FIRE_CRYSTAL, 24));
            this.dropStack(sw, new ItemStack(ModItems.SULFUR_DUST, 24));
            this.dropStack(sw, new ItemStack(ModItems.DRAGON_FRUIT, 12));
            this.dropStack(sw, new ItemStack(ModItems.WASABI_ROOT, 12));
            this.dropStack(sw, new ItemStack(ModItems.STARFRUIT, 12));

            for (ServerPlayerEntity p : sw.getPlayers()) {
                p.sendMessage(Text.literal("§d✦ Reality stabilizes as The Primordial Cataclysm dissolves! You have conquered ShuDynamics! ✦"), false);
            }
        }
    }
}
