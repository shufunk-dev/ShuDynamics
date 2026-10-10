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
import net.minecraft.world.entity.AreaEffectCloud;
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
import java.util.ArrayList;
import java.util.List;

public class ResonanceColossusEntity extends Monster {

    private final ServerBossEvent bossBar;
    private BlockPos altarPos;
    private boolean minionsSpawned = false;
    private int attackTimer = 0;
    private int phase = 1; // 1: 100-65%, 2: 65-30%, 3: <30%
    private boolean isResetting = false;

    private boolean pylonPhaseTriggered = false;
    private boolean shieldActive = false;
    private int activePylons = 0;
    private int resetMessageCooldown = 0;

    public ResonanceColossusEntity(EntityType<? extends Monster> entityType, Level world) {
        super(entityType, world);
        this.bossBar = (ServerBossEvent) new ServerBossEvent(UUID.randomUUID(), 
                Component.literal("§5✦ The Resonance Colossus ✦"),
                BossEvent.BossBarColor.PURPLE,
                BossEvent.BossBarOverlay.NOTCHED_6
        ).setDarkenScreen(true);
        this.xpReward = 150;
    }

    public static AttributeSupplier.Builder createResonanceColossusAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 500.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ATTACK_DAMAGE, 11.0)
                .add(Attributes.ARMOR, 12.0)
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
                player.sendSystemMessage(Component.literal("§c✦ The Resonance Colossus deflects attacks from outside its arena! ✦"));
            }
            return false;
        }
        if (this.shieldActive) {
            if (source.getEntity() instanceof Player player) {
                player.sendOverlayMessage(Component.literal("§5✦ Resonance Shield is active! Destroy the " + this.activePylons + " Resonance Pylon(s)! ✦"));
                world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.HOSTILE, 1.2f, 1.5f);
            }
            return false;
        }
        // Soft damage cap: maximum 25 damage per hit to prevent burst cheesing
        float cappedAmount = Math.min(amount, 25.0f);
        return super.hurtServer(world, source, cappedAmount);
    }

    public void onPylonDestroyed() {
        this.activePylons = Math.max(0, this.activePylons - 1);
        if (this.level() instanceof ServerLevel sw) {
            if (this.activePylons <= 0) {
                this.shieldActive = false;
                sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.0f, 0.8f);
                sw.sendParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 2.0, this.getZ(), 3, 0.5, 0.5, 0.5, 0.0);
                for (ServerPlayer p : sw.players()) {
                    if (p.distanceToSqr(Vec3.atCenterOf(this.altarPos)) < (128.0 * 128.0)) {
                        p.sendOverlayMessage(Component.literal("§a✦ The Resonance Shield has shattered! The Colossus is vulnerable! ✦"));
                    }
                }
            } else {
                for (ServerPlayer p : sw.players()) {
                    if (p.distanceToSqr(Vec3.atCenterOf(this.altarPos)) < (128.0 * 128.0)) {
                        p.sendSystemMessage(Component.literal("§e✦ A Resonance Pylon was destroyed! (" + this.activePylons + " remaining) ✦"));
                    }
                }
            }
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.15, true));
        this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this, Player.class, 16.0f));

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
    public void checkDespawn() {
        if (this.level().getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
            this.discard();
        }
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

        // Fallback altarPos if spawned via egg or uninitialized
        if (this.altarPos == null) {
            this.altarPos = this.blockPosition();
        }

        if (this.resetMessageCooldown > 0) {
            this.resetMessageCooldown--;
        }

        // --- Arena Leash & Reset Protocol ---
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
            // Target lost because player fled beyond reach. Check if any active player is within the arena
            AABB arenaBox = new AABB(this.altarPos).inflate(64.0);
            List<Player> nearby = sw.getEntitiesOfClass(Player.class, arenaBox, p -> !p.isCreative() && !p.isSpectator());
            if (nearby.isEmpty()) {
                targetEscaped = true;
            }
        }

        // Trigger disengage & retreat if out of bounds or all challengers escaped
        if (outOfBounds || targetEscaped) {
            if (!this.isResetting) {
                this.isResetting = true;
                this.setTarget(null);
                this.randomTeleport(this.altarPos.getX() + 0.5, this.altarPos.getY() + 1.0, this.altarPos.getZ() + 0.5, true, state -> true);
                sw.sendParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 1.5, this.getZ(), 2, 0, 0, 0, 0);
                sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.5f, 0.8f);

                if (this.resetMessageCooldown <= 0) {
                    this.resetMessageCooldown = 200; // 10-second cooldown
                    for (ServerPlayer p : sw.players()) {
                        if (p.distanceToSqr(Vec3.atCenterOf(this.altarPos)) < (160.0 * 160.0)) {
                            p.sendOverlayMessage(Component.literal("§e✦ The Resonance Colossus has disengaged and returned to its altar to regenerate! ✦"));
                        }
                    }
                }
            }
        }

        // When in resetting state, continue regenerating until back at full health at the altar
        if (this.isResetting) {
            this.setTarget(null);
            if (this.getHealth() < this.getMaxHealth()) {
                this.heal(15.0f);
                sw.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + 2.0, this.getZ(), 4, 0.5, 0.5, 0.5, 0.05);
            } else {
                this.isResetting = false;
                this.minionsSpawned = false;
                this.pylonPhaseTriggered = false;
                this.shieldActive = false;
                this.activePylons = 0;
                this.phase = 1;
            }
            return;
        }

        // --- Combat Phase Machine ---
        float healthPct = this.getHealth() / this.getMaxHealth();
        if (healthPct > 0.65f) {
            this.phase = 1;
        } else if (healthPct > 0.30f) {
            this.phase = 2;
        } else {
            this.phase = 3;
        }

        // Phase 2 Pylon Shield Trigger at 65% HP
        if (healthPct <= 0.65f && !this.pylonPhaseTriggered && !this.isResetting) {
            this.pylonPhaseTriggered = true;
            this.shieldActive = true;
            this.activePylons = 3;
            sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 2.0f, 0.6f);
            sw.sendParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 2.0, this.getZ(), 4, 0.5, 0.5, 0.5, 0.0);

            for (ServerPlayer p : sw.players()) {
                if (p.distanceToSqr(Vec3.atCenterOf(this.altarPos)) < (128.0 * 128.0)) {
                    p.sendSystemMessage(Component.literal("§5✦ The Colossus projects a Resonance Shield anchored to 3 Pylons! Shatter them! ✦"));
                }
            }

            // Spawn 3 Pylons in a triangle around the altar/colossus
            double radius = 10.0;
            for (int i = 0; i < 3; i++) {
                double angle = (i * (2.0 * Math.PI / 3.0));
                double px = this.altarPos.getX() + 0.5 + Math.cos(angle) * radius;
                double pz = this.altarPos.getZ() + 0.5 + Math.sin(angle) * radius;
                double py = this.getY();

                ResonancePylonEntity pylon = ModEntities.RESONANCE_PYLON.create(sw, net.minecraft.world.entity.EntitySpawnReason.EVENT);
                if (pylon != null) {
                    pylon.snapTo(px, py, pz, (float) Math.toDegrees(angle), 0);
                    pylon.setParentColossus(this);
                    sw.addFreshEntity(pylon);
                    sw.sendParticles(ParticleTypes.END_ROD, px, py + 1.0, pz, 20, 0.5, 1.0, 0.5, 0.1);
                }
            }
        }

        this.attackTimer++;

        // Periodic ambient particles based on phase
        if (this.attackTimer % 10 == 0) {
            sw.sendParticles(this.phase == 3 ? ParticleTypes.FLAME : ParticleTypes.ELECTRIC_SPARK,
                    this.getX(), this.getY() + 2.2, this.getZ(), 6, 0.6, 0.6, 0.6, 0.05);
        }

        LivingEntity target = this.getTarget();
        if (target == null) return;

        // PHASE 1 ABILITIES
        if (this.phase == 1) {
            // Cataclysmic Ground Slam every 12 seconds (240 ticks)
            if (this.attackTimer % 240 == 0) {
                performGroundSlam(sw);
            }
            // Acid Geyser every 15 seconds (300 ticks)
            if (this.attackTimer % 300 == 150) {
                spawnAcidGeyser(sw, target);
            }
        }
        // PHASE 2 ABILITIES
        else if (this.phase == 2) {
            // Minion summon on phase 2 entry
            if (!this.minionsSpawned) {
                this.minionsSpawned = true;
                summonReinforcements(sw);
            }
            // Singularity Vortex every 16 seconds (320 ticks)
            if (this.attackTimer % 320 == 0) {
                performSingularityVortex(sw);
            }
            // Volcanic Rift Artillery every 10 seconds (200 ticks)
            if (this.attackTimer % 200 == 100) {
                fireVolcanicArtillery(sw, target);
            }
        }
        // PHASE 3 ABILITIES (Frenzy)
        else {
            // Hyper-Phase Dash every 6 seconds (120 ticks)
            if (this.attackTimer % 120 == 0) {
                performHyperPhaseDash(sw, target);
            }
            // Resonance Meltdown aura damage
            if (this.attackTimer % 25 == 0) {
                performMeltdownPulse(sw);
            }
        }
    }

    private void performGroundSlam(ServerLevel sw) {
        sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.8f, 0.7f);
        sw.sendParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 0.5, this.getZ(), 3, 0, 0, 0, 0);

        AABB impactBox = this.getBoundingBox().inflate(8.0, 3.0, 8.0);
        List<Player> players = sw.getEntitiesOfClass(Player.class, impactBox, p -> !p.isCreative() && !p.isSpectator());

        for (Player p : players) {
            // Jumped over via Celestial Leap or grounded with Kinetic Dampening?
            boolean resisted = p.hasEffect(net.enchantedwood.effect.ModStatusEffects.CELESTIAL_LEAP) ||
                    p.hasEffect(net.enchantedwood.effect.ModStatusEffects.KINETIC_DAMPENING);

            if (!resisted) {
                p.hurtServer(sw, sw.damageSources().mobAttack(this), 10.0f);
                Vec3 knockback = p.position().subtract(this.position()).normalize().scale(1.5).add(0, 0.5, 0);
                p.setDeltaMovement(knockback);
                p.needsSync = true;
            } else {
                p.sendSystemMessage(Component.literal("§a✔ Your culinary buff neutralized the Cataclysmic Ground Slam!"));
            }
        }
    }

    private void spawnAcidGeyser(ServerLevel sw, LivingEntity target) {
        AreaEffectCloud acidCloud = new AreaEffectCloud(sw, target.getX(), target.getY(), target.getZ());
        acidCloud.setRadius(3.5f);
        acidCloud.setDuration(120); // 6s
        acidCloud.setWaitTime(10);
        acidCloud.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
        acidCloud.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0));
        acidCloud.setCustomParticle(ParticleTypes.WITCH);
        sw.addFreshEntity(acidCloud);
        sw.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.LAVA_EXTINGUISH, SoundSource.HOSTILE, 1.2f, 1.2f);
    }

    private void summonReinforcements(ServerLevel sw) {
        sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.5f, 1.2f);

        for (int i = -1; i <= 1; i += 2) {
            ConvergenceSkeletonEntity skeleton = ModEntities.CONVERGENCE_SKELETON.create(sw, net.minecraft.world.entity.EntitySpawnReason.EVENT);
            if (skeleton != null) {
                skeleton.snapTo(this.getX() + (i * 3), this.getY(), this.getZ() + 2, 0, 0);
                sw.addFreshEntity(skeleton);
            }
        }

        ConvergenceCreeperEntity creeper = ModEntities.CONVERGENCE_CREEPER.create(sw, net.minecraft.world.entity.EntitySpawnReason.EVENT);
        if (creeper != null) {
            creeper.snapTo(this.getX(), this.getY(), this.getZ() - 3, 0, 0);
            sw.addFreshEntity(creeper);
        }
    }

    private void performSingularityVortex(ServerLevel sw) {
        sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.HOSTILE, 1.8f, 0.5f);
        sw.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 2.0, this.getZ(), 80, 4.0, 2.0, 4.0, 0.2);

        AABB pullBox = this.getBoundingBox().inflate(14.0);
        List<Player> players = sw.getEntitiesOfClass(Player.class, pullBox, p -> !p.isCreative() && !p.isSpectator());

        for (Player p : players) {
            boolean resists = p.hasEffect(net.enchantedwood.effect.ModStatusEffects.KINETIC_DAMPENING)
                    || p.hasEffect(net.enchantedwood.effect.ModStatusEffects.CELESTIAL_LEAP);
            if (!resists) {
                Vec3 pull = this.position().subtract(p.position()).normalize().scale(0.25);
                p.push(pull.x, 0.05, pull.z);
                p.needsSync = true;
            }
        }
    }

    private void fireVolcanicArtillery(ServerLevel sw, LivingEntity target) {
        sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GHAST_SHOOT, SoundSource.HOSTILE, 1.4f, 0.8f);

        for (int i = 0; i < 3; i++) {
            double offsetX = (this.random.nextDouble() - 0.5) * 6.0;
            double offsetZ = (this.random.nextDouble() - 0.5) * 6.0;
            Vec3 strikePos = target.position().add(offsetX, 0, offsetZ);

            sw.sendParticles(ParticleTypes.LAVA, strikePos.x, strikePos.y + 0.2, strikePos.z, 15, 0.5, 0.5, 0.5, 0.1);
            sw.sendParticles(ParticleTypes.FLAME, strikePos.x, strikePos.y + 0.5, strikePos.z, 20, 0.4, 0.4, 0.4, 0.05);

            AABB hit = new AABB(strikePos.x - 1.5, strikePos.y - 1.0, strikePos.z - 1.5, strikePos.x + 1.5, strikePos.y + 2.0, strikePos.z + 1.5);
            List<Player> hitPlayers = sw.getEntitiesOfClass(Player.class, hit, p -> !p.isCreative() && !p.isSpectator());

            for (Player p : hitPlayers) {
                if (!p.hasEffect(net.enchantedwood.effect.ModStatusEffects.THERMAL_PROTECTION)) {
                    p.igniteForSeconds(6.0f);
                    p.hurtServer(sw, sw.damageSources().onFire(), 7.0f);
                } else {
                    p.sendOverlayMessage(Component.literal("§6✔ Thermal Protection absorbed the Volcanic Artillery!"));
                }
            }
        }
    }

    private void performHyperPhaseDash(ServerLevel sw, LivingEntity target) {
        Vec3 behind = target.position().add(target.getViewVector(1.0f).scale(-2.5));
        sw.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 1.5, this.getZ(), 30, 0.5, 1.0, 0.5, 0.2);

        this.randomTeleport(behind.x, behind.y, behind.z, true, state -> true);
        sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.5f, 1.2f);
        this.doHurtTarget(sw, target);
    }

    private void performMeltdownPulse(ServerLevel sw) {
        AABB auraBox = this.getBoundingBox().inflate(10.0);
        List<Player> players = sw.getEntitiesOfClass(Player.class, auraBox, p -> !p.isCreative() && !p.isSpectator());

        for (Player p : players) {
            p.hurtServer(sw, sw.damageSources().magic(), 2.0f);
        }
        sw.sendParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 2.5, this.getZ(), 1, 0, 0, 0, 0);
    }

    @Override
    public boolean doHurtTarget(ServerLevel world, Entity target) {
        boolean attacked = super.doHurtTarget(world, target);
        if (attacked && target instanceof LivingEntity living) {
            // Shatterstrike: Slowness II & Weakness II (cleansed by Wasabi Nigiri)
            living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120, 1));
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
            world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.HOSTILE, 1.2f, 0.8f);
        }
        return attacked;
    }

    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);

        if (this.level() instanceof ServerLevel sw) {
            // Reset altar if bound
            if (this.altarPos != null && sw.getBlockState(this.altarPos).getBlock() instanceof ResonanceAltarBlock) {
                sw.setBlockAndUpdate(this.altarPos, sw.getBlockState(this.altarPos).setValue(ResonanceAltarBlock.ACTIVE, false));
            }

            // Player reward: Smart Knockout Relic Drop
            Player killer = null;
            if (damageSource.getEntity() instanceof Player p) {
                killer = p;
            } else {
                killer = sw.getNearestPlayer(this, 128.0);
                if (killer == null && !sw.players().isEmpty()) {
                    killer = sw.players().get(0);
                }
            }

            ItemStack relicToDrop = chooseSmartRelicDrop(killer);
            this.spawnAtLocation(sw, relicToDrop);

            // Guaranteed crafting & culinary materials
            this.spawnAtLocation(sw, new ItemStack(ModItems.FIRE_CRYSTAL, 6));
            this.spawnAtLocation(sw, new ItemStack(ModItems.SULFUR_DUST, 6));
            this.spawnAtLocation(sw, new ItemStack(ModItems.DRAGON_FRUIT, 3));
            this.spawnAtLocation(sw, new ItemStack(ModItems.WASABI_ROOT, 3));
            this.spawnAtLocation(sw, new ItemStack(ModItems.STARFRUIT, 3));

            // Guaranteed Music Disc drop: Rift of the Colossus
            this.spawnAtLocation(sw, new ItemStack(ModItems.MUSIC_DISC_COLOSSUS));

            for (ServerPlayer p : sw.players()) {
                p.sendSystemMessage(Component.literal("§5✦ The Resonance Colossus has collapsed! An ancient Relic has been unearthed! ✦"));
            }
        }
    }

    private ItemStack chooseSmartRelicDrop(Player player) {
        List<ItemStack> unobtained = new ArrayList<>();

        boolean hasCleaver = player != null && hasItemAnywhere(player, ModItems.RESONANCE_CLEAVER);
        boolean hasStaff = player != null && hasItemAnywhere(player, ModItems.SINGULARITY_STAFF);
        boolean hasBento = player != null && hasItemAnywhere(player, ModItems.ETERNAL_BENTO_BOX);

        if (!hasCleaver) unobtained.add(new ItemStack(ModItems.RESONANCE_CLEAVER));
        if (!hasStaff) unobtained.add(new ItemStack(ModItems.SINGULARITY_STAFF));
        if (!hasBento) unobtained.add(new ItemStack(ModItems.ETERNAL_BENTO_BOX));

        // If player is missing any relics, guarantee an unobtained one!
        if (!unobtained.isEmpty()) {
            return unobtained.get(this.random.nextInt(unobtained.size()));
        }

        // If player already has all 3, pick a random relic
        int roll = this.random.nextInt(3);
        if (roll == 0) return new ItemStack(ModItems.RESONANCE_CLEAVER);
        if (roll == 1) return new ItemStack(ModItems.SINGULARITY_STAFF);
        return new ItemStack(ModItems.ETERNAL_BENTO_BOX);
    }

    private boolean hasItemAnywhere(Player player, net.minecraft.world.item.Item item) {
        if (player.getInventory().hasAnyMatching(stack -> stack.is(item))) return true;
        if (player.getEnderChestInventory().hasAnyMatching(stack -> stack.is(item))) return true;
        return false;
    }
}
