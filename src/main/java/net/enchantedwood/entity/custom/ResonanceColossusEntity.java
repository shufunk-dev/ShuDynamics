package net.enchantedwood.entity.custom;

import net.enchantedwood.block.custom.ResonanceAltarBlock;
import net.enchantedwood.entity.ModEntities;
import net.enchantedwood.item.ModItems;
import net.minecraft.entity.AreaEffectCloudEntity;
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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class ResonanceColossusEntity extends HostileEntity {

    private final ServerBossBar bossBar;
    private BlockPos altarPos;
    private boolean minionsSpawned = false;
    private int attackTimer = 0;
    private int phase = 1; // 1: 100-65%, 2: 65-30%, 3: <30%
    private boolean isResetting = false;

    private int resetMessageCooldown = 0;

    public ResonanceColossusEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.bossBar = (ServerBossBar) new ServerBossBar(
                Text.literal("§5✦ The Resonance Colossus ✦"),
                BossBar.Color.PURPLE,
                BossBar.Style.NOTCHED_6
        ).setDarkenSky(true);
        this.experiencePoints = 100;
    }

    public static DefaultAttributeContainer.Builder createResonanceColossusAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.MAX_HEALTH, 350.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.24)
                .add(EntityAttributes.ATTACK_DAMAGE, 9.0)
                .add(EntityAttributes.ARMOR, 10.0)
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
                player.sendMessage(Text.literal("§c✦ The Resonance Colossus deflects attacks from outside its arena! ✦"), true);
            }
            return false;
        }
        return super.damage(world, source, amount);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new net.minecraft.entity.ai.goal.MeleeAttackGoal(this, 1.15, true));
        this.goalSelector.add(2, new net.minecraft.entity.ai.goal.WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(3, new net.minecraft.entity.ai.goal.LookAtEntityGoal(this, PlayerEntity.class, 16.0f));

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
    public void checkDespawn() {
        if (this.getEntityWorld().getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
            this.discard();
        }
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

        // Fallback altarPos if spawned via egg or uninitialized
        if (this.altarPos == null) {
            this.altarPos = this.getBlockPos();
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
            Box arenaBox = new Box(this.altarPos).expand(64.0);
            List<PlayerEntity> nearby = sw.getEntitiesByClass(PlayerEntity.class, arenaBox, p -> !p.isCreative() && !p.isSpectator());
            if (nearby.isEmpty()) {
                targetEscaped = true;
            }
        }

        // Trigger disengage & retreat if out of bounds or all challengers escaped
        if (outOfBounds || targetEscaped) {
            if (!this.isResetting) {
                this.isResetting = true;
                this.setTarget(null);
                this.teleport(this.altarPos.getX() + 0.5, this.altarPos.getY() + 1.0, this.altarPos.getZ() + 0.5, true);
                sw.spawnParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 1.5, this.getZ(), 2, 0, 0, 0, 0);
                sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1.5f, 0.8f);

                if (this.resetMessageCooldown <= 0) {
                    this.resetMessageCooldown = 200; // 10-second cooldown
                    for (ServerPlayerEntity p : sw.getPlayers()) {
                        if (p.squaredDistanceTo(this.altarPos.toCenterPos()) < (160.0 * 160.0)) {
                            p.sendMessage(Text.literal("§e✦ The Resonance Colossus has disengaged and returned to its altar to regenerate! ✦"), false);
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
                sw.spawnParticles(ParticleTypes.HEART, this.getX(), this.getY() + 2.0, this.getZ(), 4, 0.5, 0.5, 0.5, 0.05);
            } else {
                this.isResetting = false;
                this.minionsSpawned = false;
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

        this.attackTimer++;

        // Periodic ambient particles based on phase
        if (this.attackTimer % 10 == 0) {
            sw.spawnParticles(this.phase == 3 ? ParticleTypes.FLAME : ParticleTypes.ELECTRIC_SPARK,
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

    private void performGroundSlam(ServerWorld sw) {
        sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 1.8f, 0.7f);
        sw.spawnParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 0.5, this.getZ(), 3, 0, 0, 0, 0);

        Box impactBox = this.getBoundingBox().expand(8.0, 3.0, 8.0);
        List<PlayerEntity> players = sw.getEntitiesByClass(PlayerEntity.class, impactBox, p -> !p.isCreative() && !p.isSpectator());

        for (PlayerEntity p : players) {
            // Jumped over via Celestial Leap or grounded with Kinetic Dampening?
            boolean resisted = p.hasStatusEffect(net.enchantedwood.effect.ModStatusEffects.CELESTIAL_LEAP) ||
                    p.hasStatusEffect(net.enchantedwood.effect.ModStatusEffects.KINETIC_DAMPENING);

            if (!resisted) {
                p.damage(sw, sw.getDamageSources().mobAttack(this), 10.0f);
                Vec3d knockback = p.getEntityPos().subtract(this.getEntityPos()).normalize().multiply(1.5).add(0, 0.5, 0);
                p.setVelocity(knockback);
                p.velocityDirty = true;
            } else {
                p.sendMessage(Text.literal("§a✔ Your culinary buff neutralized the Cataclysmic Ground Slam!"), true);
            }
        }
    }

    private void spawnAcidGeyser(ServerWorld sw, LivingEntity target) {
        AreaEffectCloudEntity acidCloud = new AreaEffectCloudEntity(sw, target.getX(), target.getY(), target.getZ());
        acidCloud.setRadius(3.5f);
        acidCloud.setDuration(120); // 6s
        acidCloud.setWaitTime(10);
        acidCloud.addEffect(new StatusEffectInstance(StatusEffects.POISON, 100, 1));
        acidCloud.addEffect(new StatusEffectInstance(StatusEffects.WITHER, 80, 0));
        acidCloud.setParticleType(ParticleTypes.WITCH);
        sw.spawnEntity(acidCloud);
        sw.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BLOCK_LAVA_EXTINGUISH, SoundCategory.HOSTILE, 1.2f, 1.2f);
    }

    private void summonReinforcements(ServerWorld sw) {
        sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 1.5f, 1.2f);

        for (int i = -1; i <= 1; i += 2) {
            ConvergenceSkeletonEntity skeleton = ModEntities.CONVERGENCE_SKELETON.create(sw, net.minecraft.entity.SpawnReason.EVENT);
            if (skeleton != null) {
                skeleton.refreshPositionAndAngles(this.getX() + (i * 3), this.getY(), this.getZ() + 2, 0, 0);
                sw.spawnEntity(skeleton);
            }
        }

        ConvergenceCreeperEntity creeper = ModEntities.CONVERGENCE_CREEPER.create(sw, net.minecraft.entity.SpawnReason.EVENT);
        if (creeper != null) {
            creeper.refreshPositionAndAngles(this.getX(), this.getY(), this.getZ() - 3, 0, 0);
            sw.spawnEntity(creeper);
        }
    }

    private void performSingularityVortex(ServerWorld sw) {
        sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.HOSTILE, 1.8f, 0.5f);
        sw.spawnParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 2.0, this.getZ(), 80, 4.0, 2.0, 4.0, 0.2);

        Box pullBox = this.getBoundingBox().expand(14.0);
        List<PlayerEntity> players = sw.getEntitiesByClass(PlayerEntity.class, pullBox, p -> !p.isCreative() && !p.isSpectator());

        for (PlayerEntity p : players) {
            boolean resists = p.hasStatusEffect(net.enchantedwood.effect.ModStatusEffects.KINETIC_DAMPENING)
                    || p.hasStatusEffect(net.enchantedwood.effect.ModStatusEffects.CELESTIAL_LEAP);
            if (!resists) {
                Vec3d pull = this.getEntityPos().subtract(p.getEntityPos()).normalize().multiply(0.25);
                p.addVelocity(pull.x, 0.05, pull.z);
                p.velocityDirty = true;
            }
        }
    }

    private void fireVolcanicArtillery(ServerWorld sw, LivingEntity target) {
        sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_GHAST_SHOOT, SoundCategory.HOSTILE, 1.4f, 0.8f);

        for (int i = 0; i < 3; i++) {
            double offsetX = (this.random.nextDouble() - 0.5) * 6.0;
            double offsetZ = (this.random.nextDouble() - 0.5) * 6.0;
            Vec3d strikePos = target.getEntityPos().add(offsetX, 0, offsetZ);

            sw.spawnParticles(ParticleTypes.LAVA, strikePos.x, strikePos.y + 0.2, strikePos.z, 15, 0.5, 0.5, 0.5, 0.1);
            sw.spawnParticles(ParticleTypes.FLAME, strikePos.x, strikePos.y + 0.5, strikePos.z, 20, 0.4, 0.4, 0.4, 0.05);

            Box hit = new Box(strikePos.x - 1.5, strikePos.y - 1.0, strikePos.z - 1.5, strikePos.x + 1.5, strikePos.y + 2.0, strikePos.z + 1.5);
            List<PlayerEntity> hitPlayers = sw.getEntitiesByClass(PlayerEntity.class, hit, p -> !p.isCreative() && !p.isSpectator());

            for (PlayerEntity p : hitPlayers) {
                if (!p.hasStatusEffect(net.enchantedwood.effect.ModStatusEffects.THERMAL_PROTECTION)) {
                    p.setOnFireFor(6.0f);
                    p.damage(sw, sw.getDamageSources().onFire(), 7.0f);
                } else {
                    p.sendMessage(Text.literal("§6✔ Thermal Protection absorbed the Volcanic Artillery!"), true);
                }
            }
        }
    }

    private void performHyperPhaseDash(ServerWorld sw, LivingEntity target) {
        Vec3d behind = target.getEntityPos().add(target.getRotationVec(1.0f).multiply(-2.5));
        sw.spawnParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 1.5, this.getZ(), 30, 0.5, 1.0, 0.5, 0.2);

        this.teleport(behind.x, behind.y, behind.z, true);
        sw.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1.5f, 1.2f);
        this.tryAttack(sw, target);
    }

    private void performMeltdownPulse(ServerWorld sw) {
        Box auraBox = this.getBoundingBox().expand(10.0);
        List<PlayerEntity> players = sw.getEntitiesByClass(PlayerEntity.class, auraBox, p -> !p.isCreative() && !p.isSpectator());

        for (PlayerEntity p : players) {
            p.damage(sw, sw.getDamageSources().magic(), 2.0f);
        }
        sw.spawnParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 2.5, this.getZ(), 1, 0, 0, 0, 0);
    }

    @Override
    public boolean tryAttack(ServerWorld world, Entity target) {
        boolean attacked = super.tryAttack(world, target);
        if (attacked && target instanceof LivingEntity living) {
            // Shatterstrike: Slowness II & Weakness II (cleansed by Wasabi Nigiri)
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 1));
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 120, 1));
            world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_WARDEN_ATTACK_IMPACT, SoundCategory.HOSTILE, 1.2f, 0.8f);
        }
        return attacked;
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);

        if (this.getEntityWorld() instanceof ServerWorld sw) {
            // Reset altar if bound
            if (this.altarPos != null && sw.getBlockState(this.altarPos).getBlock() instanceof ResonanceAltarBlock) {
                sw.setBlockState(this.altarPos, sw.getBlockState(this.altarPos).with(ResonanceAltarBlock.ACTIVE, false));
            }

            // Player reward: Smart Knockout Relic Drop
            PlayerEntity killer = null;
            if (damageSource.getAttacker() instanceof PlayerEntity p) {
                killer = p;
            } else {
                killer = sw.getClosestPlayer(this, 128.0);
                if (killer == null && !sw.getPlayers().isEmpty()) {
                    killer = sw.getPlayers().get(0);
                }
            }

            ItemStack relicToDrop = chooseSmartRelicDrop(killer);
            this.dropStack(sw, relicToDrop);

            // Guaranteed crafting & culinary materials
            this.dropStack(sw, new ItemStack(ModItems.FIRE_CRYSTAL, 6));
            this.dropStack(sw, new ItemStack(ModItems.SULFUR_DUST, 6));
            this.dropStack(sw, new ItemStack(ModItems.DRAGON_FRUIT, 3));
            this.dropStack(sw, new ItemStack(ModItems.WASABI_ROOT, 3));
            this.dropStack(sw, new ItemStack(ModItems.STARFRUIT, 3));

            for (ServerPlayerEntity p : sw.getPlayers()) {
                p.sendMessage(Text.literal("§5✦ The Resonance Colossus has collapsed! An ancient Relic has been unearthed! ✦"), false);
            }
        }
    }

    private ItemStack chooseSmartRelicDrop(PlayerEntity player) {
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

    private boolean hasItemAnywhere(PlayerEntity player, net.minecraft.item.Item item) {
        if (player.getInventory().containsAny(stack -> stack.isOf(item))) return true;
        if (player.getEnderChestInventory().containsAny(stack -> stack.isOf(item))) return true;
        return false;
    }
}
