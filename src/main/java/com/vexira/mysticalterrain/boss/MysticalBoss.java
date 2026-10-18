package com.vexira.mysticalterrain.boss;

import com.vexira.mysticalterrain.MysticalTerrain;
import com.vexira.mysticalterrain.boss.ai.BossContext;
import com.vexira.mysticalterrain.boss.ai.ThreatTable;
import com.vexira.mysticalterrain.boss.net.BossNet;
import com.vexira.mysticalterrain.boss.net.BossStatePayload;
import com.vexira.mysticalterrain.boss.vfx.Vfx;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import java.util.Map.Entry;
import org.jspecify.annotations.Nullable;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;

public abstract class MysticalBoss extends Monster implements GeoEntity {
   private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(MysticalBoss.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> INTRO = SynchedEntityData.defineId(MysticalBoss.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(MysticalBoss.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(MysticalBoss.class, EntityDataSerializers.INT);
   private static final int S_RUNNING = 1;
   private static final int S_STAGGERED = 2;
   private static final int S_ENRAGED = 4;
   private static final int S_WARDED = 8;
   private static final int S_DODGING = 16;
   private static final int K_MOVE = 0;
   private static final int K_PHASE = 1;
   private static final int K_STAGGER = 2;
   private static final int K_DODGE = 3;
   private static final Identifier PHASE_SPEED = MysticalTerrain.id("boss_phase_speed");
   private static final Set<BossAction.Tag> COUNTER_TAGS = Collections.unmodifiableSet(EnumSet.of(BossAction.Tag.MELEE, BossAction.Tag.GAP_CLOSER));
   private static final String[] DODGES = new String[]{"dodge_back", "dodge_left", "dodge_right"};
   protected final ServerBossEvent bar;
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private final List<BossAction> actions = new ArrayList<>();
   private final BossAction phaseAction;
   private final BossAction[] phaseMoves = new BossAction[3];
   private final BossAction staggerMove;
   private final BossAction[] dodgeMoves = new BossAction[3];
   private final int[] cooldowns;
   private String id;
   private @Nullable BossAction current;
   private int currentKind = 0;
   private int actionTicks;
   private int restTicks = 40;
   private int lonelyTicks;
   private @Nullable BlockPos home;
   private @Nullable DamageSource deathSource;
   private boolean playerKill;
   private final List<MysticalBoss.Scheduled> scheduled = new ArrayList<>();
   private float damageTaken = 1.0F;
   private float damageDealt = 1.0F;
   private int buffTicks;
   private float[] thresholdCache;
   private @Nullable List<Float> thresholdList;
   private final BossContext ctx = new BossContext();
   private final ThreatTable threat = new ThreatTable();
   private final BossNet.Sync sync = new BossNet.Sync();
   private List<ServerPlayer> engaged = List.of();
   private float cooldownCarry;
   private int lastPick = -1;
   private int prevPick = -1;
   private String lastPicked = "-";
   private final Map<String, Float> lastScores = new LinkedHashMap<>();
   private float poise = -1.0F;
   private int poiseIdle;
   private boolean poiseBroken;
   private int staggerImmune;
   private float ward;
   private float wardMax;
   private int wardTicks;
   private int combatTicks;
   private int lastPhaseTicks;
   private boolean desperate;
   private int appliedSpeedPhase = -1;
   private int dodgeCooldown;
   private int dodgeFrames;
   private int lastDodgedProjectile = -1;
   private @Nullable Vec3 dodgeDir;
   private int pendingDodge = -1;
   private boolean freeToDodge;
   private int markTimer = 400;
   private int markTicks;
   private @Nullable UUID marked;
   private int strafeDir = 1;
   private int strafeTimer;
   private int strafeFlipGuard;
   private boolean strafedNow;
   private int orbitDir = 1;
   private int orbitTimer;
   private int dartTimer = 80;
   private int closeInTimer = 100;
   private int closeInTicks;
   private double closeRadius = 3.0;
   private int unreachableFor;
   private int recoverCooldown;
   private int recoverTicks;
   private @Nullable Vec3 recoverAt;
   private boolean recoverBlink;
   private int leapTick;
   private int leapLength;
   private Vec3 leapFrom = Vec3.ZERO;
   private Vec3 leapTarget = Vec3.ZERO;
   private double leapApex;
   private int dashTicks;
   private int dashLength;
   private Vec3 dashDir = Vec3.ZERO;
   private double dashSpeed;
   private float hitScale = 1.0F;
   private static volatile MysticalBoss.@Nullable DebugGuard debugGuard;

   protected MysticalBoss(EntityType<? extends Monster> type, Level level, String id, BossBarColor color) {
      super(type, level);
      this.id = id;
      this.setNoGravity(this.hovers());
      this.xpReward = this.isBoss() ? 400 : 8;
      this.bar = new ServerBossEvent(this.getUUID(), this.getDisplayName(), color, BossBarOverlay.PROGRESS);
      this.bar.setPlayBossMusic(true);
      this.defineActions(this.actions);
      this.phaseAction = BossAction.of("phase", this.phaseSeconds(), this.phaseSeconds() * 0.4).anyTarget().hit(this::onPhaseRoar);

      for (int n = 0; n < 3; n++) {
         this.phaseMoves[n] = BossAction.of("phase_" + (n + 1), 3.0, 1.4).anyTarget().hit((b, l, t, i) -> this.phaseBurst(l, this.phase()));
      }

      this.staggerMove = BossAction.of("stagger", 3.0).anyTarget().lockFacing();

      for (int k = 0; k < 3; k++) {
         this.dodgeMoves[k] = BossAction.of(DODGES[k], 0.8, 0.15).anyTarget().lockFacing().hit((b, l, t, i) -> this.dodgeHop(l));
      }

      this.cooldowns = new int[this.actions.size()];
      if (this.isBoss()) {
         this.setPersistenceRequired();
      }
   }

   public Vfx.Palette palette() {
      return BossFx.paletteFor(this.id);
   }

   public boolean isBoss() {
      return true;
   }

   protected double targetRange() {
      return 40.0;
   }

   public static Builder bossAttributes(double health, double damage, double speed, double armor) {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, health)
         .add(Attributes.ATTACK_DAMAGE, damage)
         .add(Attributes.MOVEMENT_SPEED, speed)
         .add(Attributes.ARMOR, armor)
         .add(Attributes.ARMOR_TOUGHNESS, 6.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
         .add(Attributes.FOLLOW_RANGE, 48.0)
         .add(Attributes.STEP_HEIGHT, 1.6)
         .add(Attributes.SAFE_FALL_DISTANCE, 64.0);
   }

   protected abstract void defineActions(List<BossAction> var1);

   protected abstract float[] phaseThresholds();

   protected abstract SoundEvent roarSound();

   protected double phaseSeconds() {
      return 2.5;
   }

   public int introTicks() {
      return 100;
   }

   public int deathTicks() {
      return 100;
   }

   protected double preferredDistance() {
      return 3.0;
   }

   protected boolean hovers() {
      return false;
   }

   public double renderReach() {
      return this.isBoss() ? 2.5 : 0.0;
   }

   protected double hoverHeight() {
      return 2.5;
   }

   protected void onPhaseChange(ServerLevel level, int phase) {
   }

   protected boolean ownCourse(ServerLevel level, @Nullable LivingEntity target) {
      return false;
   }

   protected void onIntroTick(ServerLevel level, int ticksLeft) {
   }

   protected void onDeathTick(ServerLevel level, int tick) {
   }

   protected boolean v2() {
      return false;
   }

   public final boolean isV2() {
      return this.isBoss() && this.v2();
   }

   public float defense() {
      return 1.0F;
   }

   public int bossLevel() {
      return 50;
   }

   public float poiseMax() {
      return this.getMaxHealth() * 0.12F;
   }

   public int enrageTicks() {
      return 8400;
   }

   protected double runSpeed() {
      return 1.45;
   }

   protected boolean canDodge() {
      return true;
   }

   protected boolean preferBlink() {
      return this.hovers();
   }

   protected boolean immune() {
      return false;
   }

   protected boolean minionsAlive(ServerLevel level) {
      return !level.getEntitiesOfClass(RealmMob.class, this.getBoundingBox().inflate(24.0), LivingEntity::isAlive).isEmpty();
   }

   protected void phaseBurst(ServerLevel level, int phase) {
      Vec3 at = this.position();
      BossFx.current(this);
      BossFx.shockwave(level, this, at, 7.0, 4.0F, 1.6);
      BossFx.knockback(level, this, at, 8.0, 1.2);
      Vfx.explosion(level, at.add(0.0, this.getBbHeight() * 0.5, 0.0), this.palette(), 3.5);
      Vfx.pillar(level, at, this.palette(), this.getBbHeight() * 2.5, 1.6);
      Vfx.shake(level, at, 40.0, 1.1, 20);
   }

   protected void onStagger(ServerLevel level) {
   }

   protected void onEnrage(ServerLevel level) {
   }

   protected void desperation(ServerLevel level) {
   }

   protected void onLand(ServerLevel level) {
   }

   protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(PHASE, 0);
      builder.define(INTRO, 0);
      builder.define(ACTION, -1);
      builder.define(STATE, 0);
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
   }

   public int phase() {
      return (Integer)this.entityData.get(PHASE);
   }

   public boolean inIntro() {
      return (Integer)this.entityData.get(INTRO) > 0;
   }

   public boolean busy() {
      return this.current != null;
   }

   public boolean staggered() {
      return ((Integer)this.entityData.get(STATE) & 2) != 0;
   }

   public boolean enraged() {
      return ((Integer)this.entityData.get(STATE) & 4) != 0;
   }

   public boolean running() {
      return ((Integer)this.entityData.get(STATE) & 1) != 0;
   }

   public boolean dodging() {
      return ((Integer)this.entityData.get(STATE) & 16) != 0;
   }

   public boolean warded() {
      return ((Integer)this.entityData.get(STATE) & 8) != 0;
   }

   private void setState(int bits, boolean on) {
      int s = (Integer)this.entityData.get(STATE);
      int n = on ? s | bits : s & ~bits;
      if (n != s) {
         this.entityData.set(STATE, n);
      }
   }

   public BossContext ctx() {
      return this.ctx;
   }

   public ThreatTable threat() {
      return this.threat;
   }

   public List<ServerPlayer> engaged() {
      return this.engaged;
   }

   public @Nullable BossAction currentMove() {
      return this.current;
   }

   public int actionTick() {
      return this.actionTicks;
   }

   public boolean isLastPhase() {
      return this.phase() >= this.thresholds().length;
   }

   public float poise() {
      return this.poise;
   }

   public boolean leaping() {
      return this.leapLength > 0;
   }

   public boolean invulnerableNow() {
      return this.inIntro() || this.current != null && this.currentKind == 1 || this.dodgeFrames > 0 || this.immune();
   }

   private float[] thresholds() {
      if (this.thresholdCache == null) {
         this.thresholdCache = this.phaseThresholds();
      }

      return this.thresholdCache;
   }

   private List<Float> thresholdList() {
      if (this.thresholdList == null) {
         List<Float> list = new ArrayList<>();

         for (float t : this.thresholds()) {
            list.add(t);
         }

         this.thresholdList = List.copyOf(list);
      }

      return this.thresholdList;
   }

   public void schedule(int delay, Runnable task) {
      this.scheduled.add(new MysticalBoss.Scheduled(this.tickCount + Math.max(1, delay), task));
   }

   private void runScheduled() {
      if (!this.scheduled.isEmpty()) {
         List<MysticalBoss.Scheduled> due = new ArrayList<>();
         this.scheduled.removeIf(sx -> {
            if (sx.at() <= this.tickCount) {
               due.add(sx);
               return true;
            } else {
               return false;
            }
         });

         for (MysticalBoss.Scheduled s : due) {
            BossFx.current(this);
            s.run().run();
         }
      }
   }

   public void buff(int ticks, float taken, float dealt) {
      this.buffTicks = ticks;
      this.damageTaken = taken;
      this.damageDealt = dealt;
   }

   public boolean buffed() {
      return this.buffTicks > 0;
   }

   public float dmg(float base) {
      return base * this.damageDealt * (this.enraged() ? 1.35F : 1.0F);
   }

   public void shield(float amount, int ticks) {
      if (!(amount <= 0.0F)) {
         boolean had = this.ward > 0.0F;
         this.ward = Math.min(this.getMaxHealth() * 0.4F, this.ward + amount);
         this.wardMax = had ? Math.max(this.wardMax, this.ward) : this.ward;
         this.wardTicks = Math.max(this.wardTicks, ticks);
         this.setState(8, true);
         if (this.level() instanceof ServerLevel level) {
            BossFx.wardUp(level, this);
         }
      }
   }

   public float shield() {
      return this.ward;
   }

   public void markTarget(ServerPlayer player, int ticks) {
      this.marked = player.getUUID();
      this.markTicks = Math.max(1, ticks);
      if (this.level() instanceof ServerLevel level) {
         BossFx.markTarget(level, this, player, Math.min(40, this.markTicks));
      }
   }

   protected void cancelMove(ServerLevel level, int rest) {
      this.interrupt(level);
      this.restTicks = Math.max(0, rest);
   }

   protected void setCooldown(String anim, int ticks) {
      int i = this.indexOf(anim);
      if (i >= 0) {
         this.cooldowns[i] = Math.max(0, ticks);
      }
   }

   protected int cooldownLeft(String anim) {
      int i = this.indexOf(anim);
      return i >= 0 ? this.cooldowns[i] : 0;
   }

   public String currentAction() {
      return this.current == null ? "-" : this.current.anim();
   }

   public String bossId() {
      return this.id;
   }

   public void setHome(BlockPos home) {
      this.home = home;
   }

   public @Nullable BlockPos home() {
      return this.home;
   }

   public void beginIntro(BlockPos home) {
      this.home = home;
      if (this.level() instanceof ServerLevel level) {
         this.voice(level, "summon", 6.0F);
      }

      this.entityData.set(INTRO, this.introTicks());
      this.setHealth(1.0F);
   }

   protected void voice(ServerLevel level, String key, float volume) {
      if (this.isBoss()) {
         BossSounds.play(level, this.position(), "boss." + this.id + "." + key, volume, 0.94F + this.random.nextFloat() * 0.12F);
      }
   }

   protected void playStepSound(BlockPos pos, BlockState state) {
      SoundEvent step = this.isBoss() ? BossSounds.event("boss." + this.id + ".step") : null;
      if (step != null) {
         this.playSound(step, 1.2F, 0.9F + this.random.nextFloat() * 0.2F);
      } else {
         super.playStepSound(pos, state);
      }
   }

   protected void customServerAiStep(ServerLevel world) {
      super.customServerAiStep(world);
      this.runScheduled();
      if (this.buffTicks > 0 && --this.buffTicks == 0) {
         this.damageTaken = 1.0F;
         this.damageDealt = 1.0F;
      }

      this.updateBar(world, this.tickCount % 10 == 0);
      int intro = (Integer)this.entityData.get(INTRO);
      if (intro > 0) {
         this.entityData.set(INTRO, intro - 1);
         this.setHealth(Math.max(1.0F, this.getMaxHealth() * (1.0F - (float)(intro - 1) / this.introTicks())));
         this.getNavigation().stop();
         this.setDeltaMovement(this.getDeltaMovement().multiply(0.0, this.hovers() ? 0.0 : 1.0, 0.0));
         this.onIntroTick(world, intro - 1);
         if (intro == 1) {
            world.playSound(null, this.getX(), this.getY(), this.getZ(), this.roarSound(), SoundSource.HOSTILE, 5.0F, 0.9F);
            Vfx.shockwave(world, this.position(), this.palette(), 9.0);
            Vfx.screen(world, this.position(), 48.0, 1.2, 20, this.palette().glow(), 0.35F);
         }

         this.syncState(world);
      } else {
         if (this.isV2()) {
            this.tickV2(world);
         } else {
            this.tickClassic(world);
         }

         this.syncState(world);
      }
   }

   private void tickClassic(ServerLevel level) {
      LivingEntity target = this.findTarget(level);
      this.updateContext(level, target);
      this.checkPhase(level);
      if (this.current != null) {
         this.tickAction(level, target);
      } else if (!this.ownCourse(level, target)) {
         for (int i = 0; i < this.cooldowns.length; i++) {
            if (this.cooldowns[i] > 0) {
               this.cooldowns[i]--;
            }
         }

         if (this.restTicks > 0) {
            this.restTicks--;
         } else if (this.tickCount % 4 == 0) {
            this.pickAction(level, target);
         }

         if (this.current == null) {
            this.move(level, target);
         }
      }

      this.tickMobility(level);
      if (target == null) {
         if (++this.lonelyTicks > 600 && this.getHealth() < this.getMaxHealth() && this.tickCount % 10 == 0) {
            this.heal(this.getMaxHealth() * 0.02F);
         }

         this.returnHome();
      } else {
         this.lonelyTicks = 0;
      }
   }

   private void tickV2(ServerLevel level) {
      this.strafedNow = false;
      LivingEntity target = this.selectTarget(level);
      this.updateContext(level, target);
      this.checkPhase(level);
      this.tickTimers(level, target);
      this.cooldownCarry = this.cooldownCarry + (1.0F + 0.25F * this.phase());
      int dec = (int)this.cooldownCarry;
      this.cooldownCarry -= dec;

      for (int i = 0; i < this.cooldowns.length; i++) {
         this.cooldowns[i] = Math.max(0, this.cooldowns[i] - dec);
      }

      if (this.poiseBroken && this.canStagger()) {
         this.startStagger(level, target);
      }

      boolean running = false;
      this.freeToDodge = false;
      if (this.current != null) {
         this.tickAction(level, target);
      } else if (this.recoverTicks > 0) {
         this.tickRecover(level, target);
      } else if (!this.ownCourse(level, target) && this.leapLength <= 0 && !this.tryDodge(level, target) && !this.tryRecover(level, target)) {
         this.freeToDodge = true;
         if (this.restTicks > 0) {
            this.restTicks--;
         } else if (target != null && this.tickCount % 4 == 0) {
            this.pickActionV2(level, target, null);
         }

         if (this.current == null) {
            running = this.moveV2(level, target);
         }
      }

      this.pendingDodge = -1;
      this.tickMobility(level);
      this.setState(1, running);
      if (!this.strafedNow) {
         this.setXxa(0.0F);
      }

      if (target == null) {
         this.lonelyTicks++;
         if (this.lonelyTicks == 600) {
            this.resetFight();
         }

         if (this.lonelyTicks > 600 && this.getHealth() < this.getMaxHealth() && this.tickCount % 10 == 0) {
            this.heal(this.getMaxHealth() * 0.02F);
         }

         this.returnHome();
      } else {
         this.lonelyTicks = 0;
      }
   }

   private @Nullable LivingEntity findTarget(ServerLevel level) {
      LivingEntity target = this.getTarget();
      if (target != null
         && (!target.isAlive() || target.distanceToSqr(this) > 3136.0 || target instanceof Player p && (p.isCreative() || p.isSpectator()))) {
         target = null;
      }

      if (target == null && this.tickCount % 10 == 0) {
         target = level.getNearestPlayer(
            this.getX(),
            this.getY(),
            this.getZ(),
            this.targetRange(),
            px -> px instanceof Player pl && !pl.isCreative() && !pl.isSpectator() && pl.isAlive()
         );
      }

      this.setTarget(target);
      return target;
   }

   private @Nullable LivingEntity selectTarget(ServerLevel level) {
      this.updateEngaged(level);
      this.threat.tick(this, this.engaged);
      LivingEntity target = null;
      if (this.markTicks > 0) {
         target = this.markedPlayer();
         if (target == null) {
            this.markTicks = 0;
         }
      }

      if (target == null) {
         target = this.threat.pick(this.engaged, this.getTarget(), this);
      }

      this.setTarget(target);
      return target;
   }

   private @Nullable ServerPlayer markedPlayer() {
      if (this.marked == null) {
         return null;
      }

      for (ServerPlayer p : this.engaged) {
         if (p.getUUID().equals(this.marked)) {
            return p;
         }
      }

      return null;
   }

   private void updateEngaged(ServerLevel level) {
      List<ServerPlayer> list = new ArrayList<>();
      double range = this.targetRange();

      for (ServerPlayer p : level.players()) {
         if (p.isAlive() && !p.isCreative() && !p.isSpectator()) {
            double d = p.distanceToSqr(this);
            if (d <= 2304.0 && (d <= range * range || this.threat.get(p) > 1.0F)) {
               list.add(p);
            }
         }
      }

      this.engaged = list;
   }

   private void updateContext(ServerLevel level, @Nullable LivingEntity target) {
      if (this.isBoss()) {
         if (!this.isV2()) {
            this.updateEngaged(level);
         }

         this.ctx.update(this, level, target, this.engaged, this.preferredDistance(), this.hovers());
      }
   }

   private void tickTimers(ServerLevel level, @Nullable LivingEntity target) {
      float pm = this.poiseMax();
      if (pm > 0.0F) {
         if (this.poise < 0.0F || this.poise > pm) {
            this.poise = pm;
         }

         if (++this.poiseIdle > 80 && this.currentKind != 2 && this.poise < pm) {
            this.poise = Math.min(pm, this.poise + pm * 0.015F);
         }
      }

      if (this.staggerImmune > 0) {
         this.staggerImmune--;
      }

      if (this.dodgeCooldown > 0) {
         this.dodgeCooldown--;
      }

      if (this.dodgeFrames > 0) {
         this.dodgeFrames--;
      }

      if (this.recoverCooldown > 0) {
         this.recoverCooldown--;
      }

      if (this.strafeFlipGuard > 0) {
         this.strafeFlipGuard--;
      }

      this.unreachableFor = this.ctx.targetUnreachable ? this.unreachableFor + 1 : 0;
      if (this.wardTicks > 0 && --this.wardTicks == 0 && this.ward > 0.0F) {
         this.ward = 0.0F;
         this.wardMax = 0.0F;
         Vfx.smoke(level, this.position().add(0.0, this.getBbHeight() * 0.5, 0.0), this.palette().smoke(), 6, this.getBbWidth() * 0.5 + 0.5, 1.2);
      }

      this.setState(8, this.ward > 0.0F);
      if (target != null) {
         this.combatTicks++;
         if (this.isLastPhase()) {
            this.lastPhaseTicks++;
         }
      }

      if (!this.enraged()
         && target != null
         && (
            this.enrageTicks() > 0 && this.combatTicks >= this.enrageTicks()
               || this.thresholds().length > 0 && this.isLastPhase() && this.lastPhaseTicks >= 1800
         )) {
         this.enrage(level);
      }

      if (this.enraged() && this.tickCount % 12 == 0) {
         Vfx.embers(level, this.position().add(0.0, this.getBbHeight() * 0.5, 0.0), this.palette().glow(), 4, this.getBbWidth() * 0.6 + 0.4);
      }

      if (!this.desperate && this.isLastPhase() && this.getHealth() <= this.getMaxHealth() * 0.08F) {
         this.desperate = true;
         this.desperation(level);
      }

      if (this.appliedSpeedPhase != this.phase()) {
         this.applyPhaseSpeed();
      }

      this.tickMark(level, target);
   }

   private void tickMark(ServerLevel level, @Nullable LivingEntity target) {
      if (this.markTicks > 0) {
         this.markTicks--;
         ServerPlayer m = this.markedPlayer();
         if (m == null) {
            this.markTicks = 0;
         } else if (this.markTicks % 30 == 0 && this.markTicks > 0) {
            BossFx.markTarget(level, this, m, 20);
         }
      } else if (this.phase() >= 1 && this.engaged.size() >= 2 && target != null) {
         if (--this.markTimer <= 0) {
            this.markTimer = 400 + this.random.nextInt(201);
            List<ServerPlayer> others = new ArrayList<>();

            for (ServerPlayer p : this.engaged) {
               if (p != target) {
                  others.add(p);
               }
            }

            if (!others.isEmpty()) {
               ServerPlayer pick = others.get(this.random.nextInt(others.size()));
               this.marked = pick.getUUID();
               this.markTicks = 120;
               BossFx.markTarget(level, this, pick, 40);
            }
         }
      }
   }

   private void applyPhaseSpeed() {
      this.appliedSpeedPhase = this.phase();
      AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
      if (speed != null) {
         speed.removeModifier(PHASE_SPEED);
         if (this.appliedSpeedPhase > 0) {
            speed.addOrUpdateTransientModifier(new AttributeModifier(PHASE_SPEED, 0.08 * this.appliedSpeedPhase, Operation.ADD_MULTIPLIED_BASE));
         }
      }
   }

   private void enrage(ServerLevel level) {
      this.setState(4, true);
      this.voice(level, "enrage", 6.0F);
      BossFx.enrage(level, this);
      this.onEnrage(level);
   }

   private void resetFight() {
      this.setState(4, false);
      this.threat.clear();
      this.combatTicks = 0;
      this.lastPhaseTicks = 0;
      this.markTicks = 0;
      this.markTimer = 400;
      this.ctx.reset();
      this.poiseBroken = false;
      if (this.poiseMax() > 0.0F) {
         this.poise = this.poiseMax();
      }
   }

   private void checkPhase(ServerLevel level) {
      float[] thresholds = this.thresholds();
      float health = this.getHealth() / this.getMaxHealth();
      int phase = 0;

      for (float t : thresholds) {
         if (health <= t) {
            phase++;
         }
      }

      if (phase > this.phase()) {
         this.entityData.set(PHASE, phase);
         if (this.isV2()) {
            this.poiseBroken = false;
            if (this.poiseMax() > 0.0F) {
               this.poise = this.poiseMax();
            }

            this.stopMobility();
            this.startAction(level, this.phaseMoves[Math.min(phase, 3) - 1], this.getTarget());
            if (phase >= thresholds.length) {
               this.lastPhaseTicks = 0;
            }
         } else {
            this.startAction(level, this.phaseAction, this.getTarget());
         }

         level.playSound(null, this.getX(), this.getY(), this.getZ(), this.roarSound(), SoundSource.HOSTILE, 6.0F, 0.8F);
         this.onPhaseChange(level, phase);
      }
   }

   private void onPhaseRoar(MysticalBoss boss, ServerLevel level, @Nullable LivingEntity target, int index) {
      BossFx.shockwave(level, this, this.position(), 7.0, 4.0F, 1.6);
      Vfx.explosion(level, this.position().add(0.0, this.getBbHeight() * 0.5, 0.0), this.palette(), 3.5);
      Vfx.pillar(level, this.position(), this.palette(), this.getBbHeight() * 2.5, 1.6);
   }

   private void pickAction(ServerLevel level, @Nullable LivingEntity target) {
      double dist = target == null ? Double.MAX_VALUE : Math.sqrt(this.distanceToSqr(target));
      int total = 0;
      List<Integer> options = new ArrayList<>();

      for (int i = 0; i < this.actions.size(); i++) {
         BossAction a = this.actions.get(i);
         if (this.cooldowns[i] <= 0
            && this.phase() >= a.minPhase
            && (!a.needsTarget || target != null)
            && (target == null || !(dist < a.minRange) && !(dist > a.maxRange))
            && (a.condition == null || a.condition.test(this, target))) {
            options.add(i);
            total += a.weight;
         }
      }

      if (!options.isEmpty() && total > 0) {
         int roll = this.random.nextInt(total);

         for (int i : options) {
            BossAction a = this.actions.get(i);
            roll -= a.weight;
            if (roll < 0) {
               this.cooldowns[i] = a.cooldown;
               this.startAction(level, a, target);
               return;
            }
         }
      }
   }

   private boolean eligible(BossAction a, int i, @Nullable LivingEntity target, boolean ignoreCooldown) {
      if (!ignoreCooldown && this.cooldowns[i] > 0) {
         return false;
      }

      int p = this.phase();
      if (p >= a.minPhase && p <= a.maxPhase && (!a.needsTarget || target != null)) {
         if (target != null) {
            double d = Math.sqrt(this.distanceToSqr(target));
            if (d < a.minRange || d > a.maxRange) {
               return false;
            }
         }

         return a.condition == null || a.condition.test(this, target);
      } else {
         return false;
      }
   }

   private float tagUtility(BossAction a, boolean lastPhase, boolean minions) {
      if (a.tags.isEmpty()) {
         return 1.0F;
      }

      BossContext c = this.ctx;
      float u = 1.0F;
      if (a.has(BossAction.Tag.GAP_CLOSER) && (c.kitingTicks > 60 || c.dist > 10.0)) {
         u *= 3.0F;
      }

      if (a.has(BossAction.Tag.RANGED)) {
         if (c.targetUnreachable || c.dist > 8.0) {
            u *= 2.5F;
         }

         if (c.dist < 3.5) {
            u *= 0.4F;
         }
      }

      if (a.has(BossAction.Tag.ANTI_AIR) && (c.targetFlying || c.targetAirborne)) {
         u *= 5.0F;
      }

      if (a.has(BossAction.Tag.GUARD_BREAK) && c.targetBlocking) {
         u *= 4.0F;
      }

      if (a.has(BossAction.Tag.PUNISH) && (c.targetHealing || c.targetAiming)) {
         u *= 4.0F;
      }

      if (a.has(BossAction.Tag.AOE)) {
         u *= 1.0F + 0.8F * Math.max(0, c.surroundCount - 1);
      }

      if (a.has(BossAction.Tag.DEFENSIVE) && c.recentDamage > 0.06F) {
         u *= 3.0F;
      }

      if (a.has(BossAction.Tag.ULTIMATE) && lastPhase) {
         u *= 2.5F;
      }

      if (a.has(BossAction.Tag.BEHIND)) {
         u *= c.targetBehind ? 5.0F : 0.15F;
      }

      if (a.has(BossAction.Tag.SUMMON) && minions) {
         u *= 0.5F;
      }

      if (a.has(BossAction.Tag.MOBILITY) && c.targetUnreachable) {
         u *= 2.0F;
      }

      return u;
   }

   private boolean pickActionV2(ServerLevel level, @Nullable LivingEntity target, @Nullable Set<BossAction.Tag> require) {
      boolean lastPhase = this.isLastPhase();
      boolean minions = false;
      boolean minionsChecked = false;
      int n = this.actions.size();
      float[] weights = new float[n];
      float total = 0.0F;
      this.lastScores.clear();

      for (int i = 0; i < n; i++) {
         BossAction a = this.actions.get(i);
         if (this.eligible(a, i, target, false) && (require == null || !Collections.disjoint(a.tags, require))) {
            if (a.has(BossAction.Tag.SUMMON) && !minionsChecked) {
               minions = this.minionsAlive(level);
               minionsChecked = true;
            }

            float s = a.weight * this.tagUtility(a, lastPhase, minions);
            if (s > 0.0F && a.utility != null) {
               s *= a.utility.score(this, target, this.ctx);
            }

            if (i == this.lastPick) {
               s *= 0.25F;
            } else if (i == this.prevPick) {
               s *= 0.6F;
            }

            this.lastScores.put(a.anim, s);
            if (s > 0.0F) {
               weights[i] = s;
               total += s;
            }
         }
      }

      if (total <= 0.0F) {
         return false;
      }

      float roll = this.random.nextFloat() * total;
      int chosen = -1;

      for (int i = 0; i < n; i++) {
         if (!(weights[i] <= 0.0F)) {
            chosen = i;
            roll -= weights[i];
            if (roll < 0.0F) {
               break;
            }
         }
      }

      this.choose(level, chosen, target);
      return true;
   }

   private void choose(ServerLevel level, int i, @Nullable LivingEntity target) {
      BossAction a = this.actions.get(i);
      this.cooldowns[i] = this.isV2() && this.enraged() ? Math.round(a.cooldown * 0.7F) : a.cooldown;
      this.prevPick = this.lastPick;
      this.lastPick = i;
      this.lastPicked = a.anim;
      this.startAction(level, a, target);
   }

   private int indexOf(String anim) {
      for (int i = 0; i < this.actions.size(); i++) {
         if (this.actions.get(i).anim.equals(anim)) {
            return i;
         }
      }

      return -1;
   }

   private boolean tryCombo(ServerLevel level, BossAction a, @Nullable LivingEntity target) {
      for (BossAction.Combo c : a.combos) {
         float chance = Math.min(0.95F, c.chance() + 0.15F * this.phase());
         if (!(this.random.nextFloat() >= chance)) {
            int j = this.indexOf(c.anim());
            if (j >= 0 && this.eligible(this.actions.get(j), j, target, true)) {
               this.choose(level, j, target);
               return true;
            }
         }
      }

      return false;
   }

   private int restV2() {
      float r = (14 + this.random.nextInt(17)) * Math.max(0.2F, 1.0F - 0.2F * this.phase());
      if (this.enraged()) {
         r *= 0.5F;
      }

      return Math.max(2, Math.round(r));
   }

   private int kindOf(BossAction action) {
      if (action == this.phaseAction) {
         return 1;
      }

      for (BossAction p : this.phaseMoves) {
         if (action == p) {
            return 1;
         }
      }

      if (action == this.staggerMove) {
         return 2;
      }

      for (BossAction d : this.dodgeMoves) {
         if (action == d) {
            return 3;
         }
      }

      return 0;
   }

   protected void startAction(ServerLevel level, BossAction action, @Nullable LivingEntity target) {
      if (this.current != null) {
         this.interrupt(level);
      }

      int kind = this.kindOf(action);
      this.current = action;
      this.currentKind = kind;
      this.actionTicks = 0;
      this.closeInTicks = 0;

      this.entityData.set(ACTION, switch (kind) {
         case 1 -> 999;
         case 2 -> 998;
         case 3 -> 997;
         default -> this.actions.indexOf(action);
      });
      this.getNavigation().stop();
      this.triggerAnim("action", action.anim);
      BossFx.current(this);

      this.voice(level, switch (kind) {
         case 1 -> "phase";
         case 2 -> "stagger";
         case 3 -> "dodge";
         default -> action.anim + ".start";
      }, 3.5F);
      if (action.onStart != null) {
         action.onStart.hit(this, level, target, -1);
      }
   }

   protected void forceAction(ServerLevel level, String anim) {
      for (int i = 0; i < this.actions.size(); i++) {
         if (this.actions.get(i).anim.equals(anim)) {
            this.startAction(level, this.actions.get(i), this.getTarget());
            return;
         }
      }
   }

   private void interrupt(ServerLevel level) {
      BossAction a = this.current;
      if (a != null) {
         int kind = this.currentKind;
         this.current = null;
         this.currentKind = 0;
         this.entityData.set(ACTION, -1);
         if (kind == 2) {
            this.endStagger();
         } else if (kind == 3) {
            this.dodgeFrames = 0;
            this.setState(16, false);
         }

         if (a.onEnd != null) {
            a.onEnd.hit(this, level, this.getTarget(), -2);
         }
      }
   }

   private void finishAction(ServerLevel level, @Nullable LivingEntity target) {
      BossAction a = this.current;
      int kind = this.currentKind;
      this.current = null;
      this.currentKind = 0;
      this.entityData.set(ACTION, -1);
      if (kind == 2) {
         this.endStagger();
      } else if (kind == 3) {
         this.setState(16, false);
      }

      if (a.onEnd != null) {
         a.onEnd.hit(this, level, target, -1);
         if (this.current != null) {
            return;
         }
      }

      if (!this.isV2()) {
         this.restTicks = 12 + this.random.nextInt(18) - this.phase() * 4;
      } else if (kind != 0 || !this.tryCombo(level, a, target)) {
         if (kind != 3 || target == null || !(this.random.nextFloat() < 0.4F) || !this.pickActionV2(level, target, COUNTER_TAGS)) {
            this.restTicks = switch (kind) {
               case 0 -> this.restV2();
               case 3 -> 4;
               default -> 8;
            };
         }
      }
   }

   private void tickAction(ServerLevel level, @Nullable LivingEntity target) {
      BossAction a = this.current;
      int kind = this.currentKind;
      BossFx.current(this);
      this.actionTicks++;
      if (target != null && a.track && kind != 2) {
         this.faceTarget(target, 12.0F);
      }

      if (this.leapLength <= 0 && this.dashTicks <= 0) {
         if (kind == 3 && a.hits.length > 0 && this.actionTicks > a.hits[0]) {
            double f = this.hovers() ? 0.75 : 0.85;
            this.setDeltaMovement(this.getDeltaMovement().multiply(f, 1.0, f));
         } else if (!this.hovers()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.3, 1.0, 0.3));
         } else {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.6));
         }

         if (kind == 0 && !a.advances.isEmpty()) {
            this.applyAdvance(a, target);
         }
      }

      if (a.onTick != null) {
         a.onTick.tick(this, level, target, this.actionTicks);
         if (this.current != a) {
            return;
         }
      }

      for (int i = 0; i < a.hits.length; i++) {
         if (a.hits[i] == this.actionTicks) {
            if (kind == 0) {
               this.voice(level, a.anim + ".hit", 4.0F);
            }

            if (a.onHit != null) {
               a.onHit.hit(this, level, target, i);
               if (this.current != a) {
                  return;
               }
            }
         }
      }

      if (this.actionTicks >= a.length) {
         this.finishAction(level, target);
      }
   }

   private void applyAdvance(BossAction a, @Nullable LivingEntity target) {
      for (BossAction.Advance adv : a.advances) {
         if (this.actionTicks >= adv.from() && this.actionTicks <= adv.to()) {
            Vec3 dir;
            if (a.track && target != null) {
               dir = new Vec3(target.getX() - this.getX(), 0.0, target.getZ() - this.getZ());
               if (adv.speed() > 0.0 && dir.horizontalDistance() < this.getBbWidth() * 0.5 + target.getBbWidth() * 0.5 + 0.6) {
                  return;
               }
            } else {
               dir = Vec3.directionFromRotation(0.0F, this.getYRot());
            }

            if (dir.lengthSqr() < 1.0E-4) {
               return;
            }

            dir = dir.normalize().scale(adv.speed());
            this.setDeltaMovement(dir.x, this.getDeltaMovement().y, dir.z);
            return;
         }
      }
   }

   private boolean canStagger() {
      return this.staggerImmune <= 0 && this.leapLength <= 0 && !this.inIntro()
         ? this.current == null || this.currentKind == 0 && !this.current.superArmor
         : false;
   }

   private void startStagger(ServerLevel level, @Nullable LivingEntity target) {
      this.poiseBroken = false;
      this.stopMobility();
      this.startAction(level, this.staggerMove, target);
      this.setState(2, true);
      this.poise = 0.0F;
      BossFx.staggerBurst(level, this);
      this.onStagger(level);
   }

   private void endStagger() {
      this.setState(2, false);
      if (this.poiseMax() > 0.0F) {
         this.poise = this.poiseMax();
      }

      this.poiseIdle = 0;
      this.poiseBroken = false;
      this.staggerImmune = 400;
   }

   private float dodgeChance() {
      return 0.18F + 0.08F * this.phase() + (this.enraged() ? 0.1F : 0.0F);
   }

   private Vec3[] dodgeDirs() {
      Vec3 fwd = Vec3.directionFromRotation(0.0F, this.getYRot());
      Vec3 left = Vec3.directionFromRotation(0.0F, this.getYRot() - 90.0F);
      return new Vec3[]{fwd.scale(-1.0), left, left.scale(-1.0)};
   }

   private boolean tryDodge(ServerLevel level, @Nullable LivingEntity target) {
      int pending = this.pendingDodge;
      this.pendingDodge = -1;
      if (pending >= 0 && this.dodgeDir != null) {
         this.startDodge(level, pending, this.dodgeDir, target);
         return true;
      }

      if (this.canDodge() && this.dodgeCooldown <= 0 && this.dashTicks <= 0) {
         BossContext c = this.ctx;
         if (c.incomingProjectile && c.projectileId != this.lastDodgedProjectile) {
            this.lastDodgedProjectile = c.projectileId;
            if (this.random.nextFloat() >= this.dodgeChance()) {
               return false;
            }

            Vec3[] dirs = this.dodgeDirs();
            int[] order;
            if (c.projectileVelocity != null && c.projectilePos != null) {
               Vec3 v = c.projectileVelocity.horizontal();
               Vec3 rel = this.position().subtract(c.projectilePos).horizontal();
               Vec3 off = v.lengthSqr() > 1.0E-4 ? rel.subtract(v.scale(rel.dot(v) / v.lengthSqr())) : rel;
               boolean goLeft = off.lengthSqr() < 1.0E-4 ? this.random.nextBoolean() : off.dot(dirs[1]) > 0.0;
               order = goLeft ? new int[]{1, 2, 0} : new int[]{2, 1, 0};
            } else {
               order = this.random.nextBoolean() ? new int[]{0, 1, 2} : new int[]{0, 2, 1};
            }

            for (int k : order) {
               if (this.safeDodge(level, dirs[k])) {
                  this.startDodge(level, k, dirs[k], target);
                  return true;
               }
            }

            return false;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private void startDodge(ServerLevel level, int k, Vec3 dir, @Nullable LivingEntity target) {
      this.dodgeDir = dir;
      this.dodgeCooldown = Math.max(20, 70 - 10 * this.phase());
      this.startAction(level, this.dodgeMoves[k], target);
      this.dodgeFrames = 8;
      this.setState(16, true);
   }

   private boolean evadeMelee(ServerLevel level, DamageSource source) {
      if (this.freeToDodge
         && !this.isDeadOrDying()
         && this.current == null
         && this.leapLength <= 0
         && this.dashTicks <= 0
         && this.recoverTicks <= 0
         && !this.poiseBroken
         && this.pendingDodge < 0
         && this.dodgeCooldown <= 0
         && this.getInvulnerableTime() <= 10
         && this.canDodge()) {
         if (source.getEntity() instanceof ServerPlayer p && source.getDirectEntity() == p && !p.isCreative() && !p.isSpectator()) {
            boolean swing = (source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MACE_SMASH)) && p.getAttackStrengthScale(0.0F) <= 0.0F;
            if (!swing && !source.is(DamageTypes.SPEAR)) {
               return false;
            }

            if (this.random.nextFloat() >= this.dodgeChance()) {
               return false;
            }

            Vec3[] dirs = this.dodgeDirs();
            Vec3 away = this.position().subtract(p.position()).horizontal();
            Vec3 from = away.lengthSqr() > 1.0E-4 ? away.normalize() : dirs[0];
            Integer[] order = new Integer[]{0, 1, 2};
            Arrays.sort(order, (a, b) -> Double.compare(dirs[b].dot(from), dirs[a].dot(from)));
            Integer[] var9 = order;
            int var10 = var9.length;

            for (int var11 = 0; var11 < var10; var11++) {
               int k = var9[var11];
               if (dirs[k].dot(from) > -0.5 && this.safeDodge(level, dirs[k])) {
                  this.pendingDodge = k;
                  this.dodgeDir = dirs[k];
                  this.dodgeCooldown = Math.max(20, 70 - 10 * this.phase());
                  this.dodgeFrames = 8;
                  return true;
               }
            }

            return false;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean safeDodge(ServerLevel level, Vec3 dir) {
      double dist = 3.5;

      for (double k = 1.0; k <= dist; k += 1.25) {
         if (!level.noCollision(this, this.getBoundingBox().move(dir.x * k, this.hovers() ? 0.0 : 0.6, dir.z * k))) {
            return false;
         }
      }

      return this.hovers() || BossFx.safeGround(level, this.position().add(dir.scale(dist)), 3.0);
   }

   private void dodgeHop(ServerLevel level) {
      Vec3 d = this.dodgeDir;
      if (d != null) {
         if (this.hovers()) {
            this.setDeltaMovement(d.x * 0.9, this.getDeltaMovement().y * 0.3, d.z * 0.9);
         } else {
            this.setDeltaMovement(d.x * 0.9, 0.25, d.z * 0.9);
         }

         Vfx.smoke(level, this.position(), this.palette().smoke(), 4, this.getBbWidth() * 0.5 + 0.3, 1.0);
         Vfx.trail(level, this.position().add(0.0, this.getBbHeight() * 0.5, 0.0), this.palette(), 0.6 + this.getBbWidth() * 0.3);
      }
   }

   private boolean tryRecover(ServerLevel level, @Nullable LivingEntity target) {
      if (target != null && this.unreachableFor > 80 && this.recoverCooldown <= 0) {
         this.unreachableFor = 0;
         this.recoverCooldown = 160;
         this.recoverBlink = this.preferBlink();
         Vec3 at = this.recoverPoint(level, target);
         this.recoverAt = at;
         this.recoverTicks = 14;
         this.getNavigation().stop();
         Vfx.warn(level, at, this.palette(), 1.5 + this.getBbWidth() * 0.5, 14);
         Vfx.gather(level, this.position().add(0.0, this.getBbHeight() * 0.5, 0.0), this.palette(), 2.0F + this.getBbWidth(), 14);
         this.voice(level, "dodge", 4.0F);
         return true;
      } else {
         return false;
      }
   }

   private Vec3 recoverPoint(ServerLevel level, LivingEntity target) {
      if (this.recoverBlink) {
         Vec3 at = BossFx.around(level, this, target.position(), 2.5, 4.5);
         return this.hovers() ? at.add(0.0, this.hoverHeight(), 0.0) : at;
      }

      Vec3 to = target.position();
      if (!target.onGround()) {
         Vec3 g = BossFx.ground(level, to.x, to.y, to.z);
         if (g != null) {
            to = g;
         }
      }

      Vec3 off = this.position().subtract(to).horizontal();
      if (off.lengthSqr() > 0.01) {
         to = to.add(off.normalize().scale(1.5 + this.getBbWidth() * 0.5));
      }

      Vec3 d = to.subtract(this.position());
      if (d.length() > 24.0) {
         to = this.position().add(d.normalize().scale(24.0));
      }

      return to;
   }

   private void tickRecover(ServerLevel level, @Nullable LivingEntity target) {
      this.getNavigation().stop();
      if (target != null) {
         this.faceTarget(target, 15.0F);
      }

      if (!this.hovers()) {
         this.setDeltaMovement(this.getDeltaMovement().multiply(0.3, 1.0, 0.3));
      }

      if (--this.recoverTicks <= 0) {
         Vec3 at = this.recoverAt;
         this.recoverAt = null;
         if (at != null) {
            if (this.recoverBlink) {
               this.blinkTo(at);
            } else {
               this.leapTo(at, Math.max(4.0, this.ctx.heightDiff + 3.0));
            }

            this.restTicks = Math.max(this.restTicks, 6);
         }
      }
   }

   protected void move(ServerLevel level, @Nullable LivingEntity target) {
      if (target != null) {
         double dist = Math.sqrt(this.distanceToSqr(target));
         if (this.hovers()) {
            Vec3 to = target.position().subtract(this.position());
            Vec3 flat = new Vec3(to.x, 0.0, to.z);
            Vec3 want = this.position();
            if (flat.length() > 0.01) {
               want = target.position().subtract(flat.normalize().scale(this.preferredDistance()));
            }

            want = new Vec3(want.x, target.getY() + this.hoverHeight() + Mth.sin(this.tickCount * 0.05F) * 0.6, want.z);
            Vec3 step = want.subtract(this.position());
            double speed = this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.6;
            if (step.length() > speed) {
               step = step.normalize().scale(speed);
            }

            this.setDeltaMovement(this.getDeltaMovement().scale(0.7).add(step.scale(0.3)));
            this.faceTarget(target, 10.0F);
         } else if (dist > this.preferredDistance()) {
            if (this.tickCount % 5 == 0) {
               this.getNavigation().moveTo(target, 1.0);
            }
         } else {
            this.getNavigation().stop();
            this.faceTarget(target, 10.0F);
         }
      }
   }

   private boolean moveV2(ServerLevel level, @Nullable LivingEntity target) {
      if (target == null) {
         return false;
      }

      if (this.hovers()) {
         this.hoverMove(level, target);
         return false;
      }

      double dist = this.ctx.dist;
      double pref = this.preferredDistance();
      if (dist > pref + 6.0) {
         if (this.tickCount % 5 == 0) {
            this.pathTo(target, this.runSpeed());
         }

         return !this.getNavigation().isDone();
      } else if (dist > pref + 2.0) {
         if (this.tickCount % 5 == 0) {
            this.pathTo(target, 1.0);
         }

         return false;
      } else {
         this.ctx.notePath(true);
         if (pref >= 6.0 && dist < pref - 3.0) {
            this.strafe(level, target, -0.8F, 0.35F);
         } else {
            this.strafe(level, target, (float)Mth.clamp((dist - pref) * 0.5, -0.6, 0.6), 0.9F);
         }

         return false;
      }
   }

   private void pathTo(LivingEntity target, double speed) {
      Path path = this.getNavigation().createPath(target, 1);
      this.ctx.notePath(path != null && path.canReach());
      this.getNavigation().moveTo(path, speed);
   }

   private void strafe(ServerLevel level, LivingEntity target, float forward, float side) {
      this.getNavigation().stop();
      this.faceTarget(target, 10.0F);
      if (--this.strafeTimer <= 0 || this.horizontalCollision && this.strafeFlipGuard <= 0) {
         this.flipStrafe();
      }

      Vec3 sideDir = Vec3.directionFromRotation(0.0F, this.getYRot() - 90 * this.strafeDir);
      if (this.strafeFlipGuard <= 0 && !BossFx.safeGround(level, this.position().add(sideDir.scale(this.getBbWidth() * 0.5 + 1.2)), 2.0)) {
         this.flipStrafe();
      }

      float fwd = forward;
      if (fwd < 0.0F && !BossFx.safeGround(level, this.position().add(Vec3.directionFromRotation(0.0F, this.getYRot()).scale(-(this.getBbWidth() * 0.5 + 1.2))), 2.0)) {
         fwd = 0.0F;
      }

      this.getMoveControl().strafe(fwd, side * this.strafeDir);
      this.strafedNow = true;
   }

   private void flipStrafe() {
      this.strafeDir = -this.strafeDir;
      this.strafeTimer = 40 + this.random.nextInt(51);
      this.strafeFlipGuard = 10;
   }

   private void hoverMove(ServerLevel level, LivingEntity target) {
      if (this.closeInTicks > 0) {
         this.closeInTicks--;
      } else if (--this.closeInTimer <= 0) {
         this.closeInTimer = 80 + this.random.nextInt(81);
         double r = this.readyMeleeRange();
         if (r > 0.0 && this.random.nextFloat() < 0.6F) {
            this.closeInTicks = 70;
            this.closeRadius = Math.max(1.5, r * 0.6);
         }
      }

      double radius = this.closeInTicks > 0 ? this.closeRadius : this.preferredDistance();
      if (--this.orbitTimer <= 0 || this.horizontalCollision) {
         this.orbitDir = -this.orbitDir;
         this.orbitTimer = 60 + this.random.nextInt(81);
      }

      Vec3 rel = this.position().subtract(target.position());
      double angle = rel.horizontalDistanceSqr() > 0.01 ? Math.atan2(rel.z, rel.x) : this.random.nextDouble() * Math.PI * 2.0;
      angle += this.orbitDir * Math.min(0.12, 0.9 / Math.max(2.0, radius));
      double alt = Math.max(0.8, this.hoverHeight() + Mth.sin(this.tickCount * 0.035F) * 1.1 + Mth.sin(this.tickCount * 0.013F + 1.7F) * 0.7);
      Vec3 want = target.position().add(Math.cos(angle) * radius, alt, Math.sin(angle) * radius);
      Vec3 step = want.subtract(this.position());
      double speed = this.getAttributeValue(Attributes.MOVEMENT_SPEED) * (this.closeInTicks > 0 ? 2.2 : 1.6);
      if (step.length() > speed) {
         step = step.normalize().scale(speed);
      }

      Vec3 v = this.getDeltaMovement().scale(0.7).add(step.scale(0.3));
      if (--this.dartTimer <= 0) {
         this.dartTimer = 60 + this.random.nextInt(61);
         Vec3 tangent = new Vec3(-Math.sin(angle), 0.0, Math.cos(angle)).scale(this.orbitDir * 0.7);
         if (level.noCollision(this, this.getBoundingBox().move(tangent.scale(3.0)))) {
            v = v.add(tangent);
            Vfx.trail(level, this.position().add(0.0, this.getBbHeight() * 0.5, 0.0), this.palette(), 0.9);
         }
      }

      this.setDeltaMovement(v);
      this.faceTarget(target, 10.0F);
   }

   private double readyMeleeRange() {
      double best = -1.0;
      int p = this.phase();

      for (int i = 0; i < this.actions.size(); i++) {
         BossAction a = this.actions.get(i);
         if (a.has(BossAction.Tag.MELEE) && this.cooldowns[i] <= 0 && p >= a.minPhase && p <= a.maxPhase && (best < 0.0 || a.maxRange < best)) {
            best = a.maxRange;
         }
      }

      return best;
   }

   private void returnHome() {
      if (this.home != null && this.current == null) {
         double d = this.home.distToCenterSqr(this.position());
         if (d > 4096.0) {
            this.teleportTo(this.home.getX() + 0.5, this.home.getY() + 1, this.home.getZ() + 0.5);
         } else if (d > 144.0 && this.tickCount % 20 == 0 && !this.hovers()) {
            this.getNavigation().moveTo(this.home.getX() + 0.5, this.home.getY(), this.home.getZ() + 0.5, 0.8);
         }
      }
   }

   public void faceTarget(LivingEntity target, float maxTurn) {
      double dx = target.getX() - this.getX();
      double dz = target.getZ() - this.getZ();
      float want = (float)(Mth.atan2(dz, dx) * 180.0F / (float)Math.PI) - 90.0F;
      float yaw = Mth.approachDegrees(this.getYRot(), want, maxTurn);
      this.setYRot(yaw);
      this.setYBodyRot(yaw);
      this.setYHeadRot(yaw);
      this.getLookControl().setLookAt(target, 30.0F, 30.0F);
   }

   public void leapTo(Vec3 to, double apex) {
      this.leapFrom = this.position();
      this.leapTarget = to;
      double d = to.subtract(this.leapFrom).horizontalDistance();
      this.leapLength = Mth.clamp((int)Math.round(12.0 + d * 0.6), 14, 26);
      this.leapTick = 0;
      this.leapApex = Math.max(0.5, apex);
      this.dashTicks = 0;
      this.setNoGravity(true);
      this.getNavigation().stop();
   }

   public boolean blinkTo(Vec3 to) {
      if (this.level() instanceof ServerLevel level) {
         Vec3 at = this.freeSpot(level, to);
         if (at == null) {
            return false;
         }

         if (this.leapLength > 0) {
            this.leapLength = 0;
            this.setNoGravity(this.hovers());
         }

         this.dashTicks = 0;
         BossFx.blink(level, this, this.position());
         this.teleportTo(at.x, at.y, at.z);
         this.setDeltaMovement(Vec3.ZERO);
         this.getNavigation().stop();
         BossFx.blink(level, this, at);
         return true;
      } else {
         return false;
      }
   }

   public void dash(Vec3 dir, double speed) {
      this.dash(dir, speed, 8);
   }

   public void dash(Vec3 dir, double speed, int ticks) {
      Vec3 h = dir.horizontal();
      if (!(h.lengthSqr() < 1.0E-4)) {
         this.dashDir = h.normalize();
         this.dashSpeed = speed;
         this.dashLength = Math.max(1, ticks);
         this.dashTicks = this.dashLength;
         this.getNavigation().stop();
      }
   }

   public Vec3 predict(LivingEntity target, double ticks) {
      Vec3 v = target == this.ctx.target ? this.ctx.targetVelocity : target.getDeltaMovement();
      double t = Mth.clamp(ticks, 0.0, 40.0);
      Vec3 lead = new Vec3(v.x * t, 0.0, v.z * t);
      if (lead.length() > 8.0) {
         lead = lead.normalize().scale(8.0);
      }

      double dy = target.onGround() ? 0.0 : Mth.clamp(v.y * t, -3.0, 3.0);
      return target.position().add(lead.x, dy, lead.z);
   }

   private void tickMobility(ServerLevel level) {
      if (this.leapLength > 0) {
         this.tickLeap(level);
      } else if (this.dashTicks > 0) {
         this.tickDash(level);
      }
   }

   private void tickLeap(ServerLevel level) {
      this.leapTick++;
      double s = Math.min(1.0, (double)this.leapTick / this.leapLength);
      Vec3 p = this.leapFrom.lerp(this.leapTarget, s).add(0.0, this.leapApex * 4.0 * s * (1.0 - s), 0.0);
      Vec3 v = p.subtract(this.position());
      if (v.lengthSqr() > 9.0) {
         v = v.normalize().scale(3.0);
      }

      this.setDeltaMovement(v);
      if (this.current == null && v.horizontalDistanceSqr() > 0.0025) {
         float yaw = (float)(Mth.atan2(v.z, v.x) * 180.0F / (float)Math.PI) - 90.0F;
         this.setYRot(yaw);
         this.setYBodyRot(yaw);
         this.setYHeadRot(yaw);
      }

      if (this.leapTick % 2 == 0) {
         Vfx.trail(level, this.position().add(0.0, this.getBbHeight() * 0.5, 0.0), this.palette(), 0.6 + this.getBbWidth() * 0.25);
      }

      if (this.leapTick >= this.leapLength || s > 0.55 && this.onGround()) {
         this.land(level);
      }
   }

   private void land(ServerLevel level) {
      this.leapLength = 0;
      this.leapTick = 0;
      this.setNoGravity(this.hovers());
      Vec3 v = this.getDeltaMovement();
      this.setDeltaMovement(v.x * 0.2, this.hovers() ? 0.0 : Math.min(0.0, v.y), v.z * 0.2);
      this.unstick(level);
      BossFx.current(this);
      if (!this.hovers()) {
         BossFx.shockwave(level, this, this.position(), 3.0 + this.getBbWidth() * 0.5, this.dmg(4.0F), 0.9);
      } else {
         Vfx.ring(level, this.position(), this.palette().glow(), 2.5 + this.getBbWidth() * 0.5, 10);
      }

      this.onLand(level);
   }

   private void tickDash(ServerLevel level) {
      this.dashTicks--;
      double s = this.dashSpeed * (0.55 + 0.45 * this.dashTicks / this.dashLength);
      this.setDeltaMovement(this.dashDir.x * s, this.getDeltaMovement().y, this.dashDir.z * s);
      if (this.tickCount % 2 == 0) {
         Vfx.trail(level, this.position().add(0.0, this.getBbHeight() * 0.45, 0.0), this.palette(), 0.5 + this.getBbWidth() * 0.25);
      }
   }

   private void stopMobility() {
      if (this.leapLength > 0) {
         this.leapLength = 0;
         this.leapTick = 0;
         this.setNoGravity(this.hovers());
      }

      this.dashTicks = 0;
      this.recoverTicks = 0;
      this.recoverAt = null;
   }

   private void unstick(ServerLevel level) {
      if (!level.noCollision(this, this.getBoundingBox())) {
         for (int dy = 1; dy <= 4; dy++) {
            if (level.noCollision(this, this.getBoundingBox().move(0.0, dy, 0.0))) {
               this.setPos(this.getX(), this.getY() + dy, this.getZ());
               return;
            }
         }

         Vec3 spot = this.freeSpot(level, this.position());
         if (spot != null) {
            this.setPos(spot.x, spot.y, spot.z);
         }
      }
   }

   private @Nullable Vec3 freeSpot(ServerLevel level, Vec3 to) {
      for (int dy = 0; dy <= 3; dy++) {
         Vec3 p = to.add(0.0, dy, 0.0);
         if (this.fits(level, p)) {
            return p;
         }
      }

      for (int i = 0; i < 10; i++) {
         Vec3 p = BossFx.around(level, this, to, 1.0, 3.5);
         if (this.hovers()) {
            p = p.add(0.0, 1.0, 0.0);
         }

         if (this.fits(level, p)) {
            return p;
         }
      }

      return null;
   }

   private boolean fits(ServerLevel level, Vec3 p) {
      return !level.noCollision(this, this.getBoundingBox().move(p.x - this.getX(), p.y - this.getY(), p.z - this.getZ()))
         ? false
         : this.hovers() || BossFx.safeGround(level, p, 6.0);
   }

   private void updateBar(ServerLevel level, boolean players) {
      if (this.isBoss()) {
         this.bar.setProgress(this.getHealth() / this.getMaxHealth());
         if (players) {
            for (ServerPlayer p : level.players()) {
               boolean near = p.distanceToSqr(this) < 4096.0 && this.isAlive();
               if (near) {
                  this.bar.addPlayer(p);
               } else {
                  this.bar.removePlayer(p);
               }
            }
         }
      }
   }

   public BossStatePayload bossState() {
      float pm = this.isV2() ? this.poiseMax() : 0.0F;
      float poiseFrac = pm > 0.0F ? Mth.clamp(this.poise < 0.0F ? 1.0F : this.poise / pm, 0.0F, 1.0F) : -1.0F;
      String cast = "";
      int castTick = 0;
      int castLength = 0;
      int flags = 0;
      if (this.current != null && this.currentKind == 0) {
         cast = this.current.anim;
         castTick = this.actionTicks;
         castLength = this.current.length;
         if (this.current.telegraphing(this.actionTicks)) {
            flags |= 16;
         }
      }

      if (this.enraged()) {
         flags |= 1;
      }

      if (this.invulnerableNow()) {
         flags |= 2;
      }

      if (this.buffed()) {
         flags |= 4;
      }

      return new BossStatePayload(
         this.bar.getId(),
         this.id,
         this.bossLevel(),
         1.0F / Math.max(0.01F, this.defense()),
         this.getHealth(),
         this.getMaxHealth(),
         this.phase(),
         this.thresholdList(),
         this.ward,
         this.wardMax,
         poiseFrac,
         this.staggered(),
         cast,
         castTick,
         castLength,
         flags
      );
   }

   private void syncState(ServerLevel level) {
      if (this.isBoss()) {
         UUID markedId = null;
         if (this.isAlive() && this.getTarget() instanceof ServerPlayer sp && (this.markTicks > 0 || this.engaged.size() >= 2)) {
            markedId = sp.getUUID();
         }

         this.sync.tick(this.tickCount, this.bar, this.bossState(), markedId);
      }
   }

   public void stopSeenByPlayer(ServerPlayer player) {
      super.stopSeenByPlayer(player);
      this.bar.removePlayer(player);
   }

   public void setCustomName(@Nullable Component name) {
      super.setCustomName(name);
      this.bar.setName(this.getDisplayName());
   }

   public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
      boolean bypass = source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
      if (bypass || !this.inIntro() && (this.current == null || this.currentKind != 1) && this.dodgeFrames <= 0) {
         if (!bypass && this.isV2() && this.evadeMelee(world, source)) {
            return false;
         }

         float scale = this.damageTaken;
         if (this.isBoss() && !bypass) {
            scale *= Math.max(0.01F, this.defense());
            if (this.isV2()) {
               scale /= 1.0F + 0.55F * Math.max(0, this.engaged.size() - 1);
               if (this.staggered()) {
                  scale *= 1.5F;
               }
            }
         }

         this.hitScale = scale > 1.0E-4F ? 1.0F / scale : 0.0F;

         try {
            return super.hurtServer(world, source, amount * scale);
         } finally {
            this.hitScale = 1.0F;
         }
      } else {
         return false;
      }
   }

   protected void actuallyHurt(ServerLevel world, DamageSource source, float amount) {
      if (!this.isBoss()) {
         super.actuallyHurt(world, source, amount);
      } else {
         boolean bypass = source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
         float raw = amount * this.hitScale;
         float absorbed = 0.0F;
         if (this.ward > 0.0F && !bypass && amount > 0.0F) {
            absorbed = Math.min(this.ward, amount);
            this.ward -= absorbed;
            amount -= absorbed;
            if (this.ward <= 0.01F) {
               this.breakWard(world);
            }
         }

         float before = this.getHealth();
         super.actuallyHurt(world, source, amount);
         float taken = Math.max(0.0F, before - this.getHealth()) + absorbed;
         if (taken > 0.0F) {
            this.ctx.recordDamage(this.tickCount, taken);
         }

         if (!(raw <= 0.0F)) {
            if (source.getEntity() instanceof ServerPlayer p && !p.isCreative() && !p.isSpectator()) {
               boolean ranged = source.is(DamageTypeTags.IS_PROJECTILE)
                  || source.getDirectEntity() != p
                  || source.is(DamageTypes.MAGIC)
                  || source.is(DamageTypes.INDIRECT_MAGIC);
               this.threat.add(p, raw * (ranged ? 1.5F : 1.0F));
            }

            if (this.isV2() && this.poiseMax() > 0.0F && !bypass) {
               this.poiseIdle = 0;
               float f = poiseMult(source);
               if (this.current != null && this.currentKind == 0 && this.current.superArmor) {
                  f *= 0.2F;
               }

               if (this.inIntro() || this.staggerImmune > 0 || this.current != null && this.currentKind != 0) {
                  f = 0.0F;
               }

               if (f > 0.0F && this.poise > 0.0F) {
                  this.poise = Math.max(0.0F, this.poise - raw * f);
                  if (this.poise <= 0.0F) {
                     this.poiseBroken = true;
                  }
               }
            }
         }
      }
   }

   private static float poiseMult(DamageSource source) {
      ItemStack weapon = source.getWeaponItem();
      if (weapon == null || !weapon.is(ItemTags.AXES) && !weapon.is(Items.MACE)) {
         return source.getEntity() instanceof Player p
               && source.getDirectEntity() == p
               && p.fallDistance > 0.0
               && !p.onGround()
               && !p.onClimbable()
               && !p.isInWater()
               && !p.isPassenger()
               && !p.isSprinting()
            ? 1.5F
            : 1.0F;
      } else {
         return 1.5F;
      }
   }

   private void breakWard(ServerLevel level) {
      this.ward = 0.0F;
      this.wardMax = 0.0F;
      this.wardTicks = 0;
      this.setState(8, false);
      BossFx.wardShatter(level, this);
      float pm = this.poiseMax();
      if (this.isV2() && pm > 0.0F && this.staggerImmune <= 0 && this.poise > 0.0F) {
         this.poise = Math.max(0.0F, this.poise - pm * 0.5F);
         if (this.poise <= 0.0F) {
            this.poiseBroken = true;
         }
      }
   }

   public boolean isInvulnerableTo(ServerLevel world, DamageSource source) {
      return source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.DROWN) || source.is(DamageTypes.FALL) || super.isInvulnerableTo(world, source);
   }

   public void die(DamageSource damageSource) {
      super.die(damageSource);
      if (this.level() instanceof ServerLevel level) {
         this.interrupt(level);
      }

      this.current = null;
      this.currentKind = 0;
      this.entityData.set(ACTION, -1);
      this.stopMobility();
      this.dodgeFrames = 0;
      this.setState(19, false);
      this.stopTriggeredAnim("action", null);
      this.bar.setProgress(0.0F);
   }

   protected void dropAllDeathLoot(ServerLevel world, DamageSource damageSource) {
      this.deathSource = damageSource;
      this.playerKill = this.lastHurtByPlayerMemoryTime > 0;
   }

   protected void tickDeath() {
      this.deathTime++;
      if (this.level() instanceof ServerLevel level) {
         this.syncState(level);
         if (this.playerKill) {
            this.lastHurtByPlayerMemoryTime = 100;
         }

         this.onDeathTick(level, this.deathTime);
         if (this.deathTime >= this.deathTicks() && !this.isRemoved()) {
            if (this.deathSource != null) {
               super.dropAllDeathLoot(level, this.deathSource);
            }

            if (this.isBoss()) {
               Vfx.explosion(level, this.position().add(0.0, this.getBbHeight() * 0.5, 0.0), this.palette(), 4.5);
            }

            this.bar.removeAllPlayers();
            level.broadcastEntityEvent(this, (byte)60);
            this.remove(RemovalReason.KILLED);
         }
      }
   }

   @Override
   public void remove(RemovalReason reason) {
      if (this.bar != null) {
         this.bar.removeAllPlayers();
      }
      super.remove(reason);
   }

   public boolean removeWhenFarAway(double distanceSquared) {
      return !this.isBoss() && super.removeWhenFarAway(distanceSquared);
   }

   public boolean causeFallDamage(double fallDistance, float damagePerDistance, DamageSource damageSource) {
      return false;
   }

   public void checkDespawn() {
      if (!this.isBoss()) {
         super.checkDespawn();
      }
   }

   protected boolean canRide(Entity entity) {
      return false;
   }

   protected void addAdditionalSaveData(ValueOutput view) {
      super.addAdditionalSaveData(view);
      view.putInt("BossPhase", this.phase());
      if (this.home != null) {
         view.putLong("BossHome", this.home.asLong());
      }

      if (this.isBoss()) {
         view.putBoolean("BossEnraged", this.enraged());
         view.putFloat("BossPoise", this.poise);
         view.putFloat("BossWard", this.ward);
         view.putFloat("BossWardMax", this.wardMax);
         view.putInt("BossWardTicks", this.wardTicks);
         view.putBoolean("BossDesperate", this.desperate);
         view.putInt("BossCombat", this.combatTicks);
         view.putInt("BossLastPhase", this.lastPhaseTicks);
         view.putInt("BossIntro", (Integer)this.entityData.get(INTRO));
      }
   }

   protected void readAdditionalSaveData(ValueInput view) {
      super.readAdditionalSaveData(view);
      this.entityData.set(PHASE, view.getIntOr("BossPhase", 0));
      long home = view.getLongOr("BossHome", Long.MIN_VALUE);
      this.home = home == Long.MIN_VALUE ? null : BlockPos.of(home);
      if (this.hasCustomName()) {
         this.bar.setName(this.getDisplayName());
      }

      this.setNoGravity(this.hovers());
      if (this.isBoss()) {
         this.setState(4, view.getBooleanOr("BossEnraged", false));
         float p = view.getFloatOr("BossPoise", -1.0F);
         this.poise = p <= 0.0F ? -1.0F : p;
         this.ward = Math.max(0.0F, view.getFloatOr("BossWard", 0.0F));
         this.wardMax = Math.max(this.ward, view.getFloatOr("BossWardMax", 0.0F));
         this.wardTicks = view.getIntOr("BossWardTicks", 0);
         if (this.wardTicks <= 0) {
            this.ward = 0.0F;
            this.wardMax = 0.0F;
         }

         this.setState(8, this.ward > 0.0F);
         this.desperate = view.getBooleanOr("BossDesperate", false);
         this.combatTicks = view.getIntOr("BossCombat", 0);
         this.lastPhaseTicks = view.getIntOr("BossLastPhase", 0);
         this.entityData.set(INTRO, Mth.clamp(view.getIntOr("BossIntro", 0), 0, this.introTicks()));
      }
   }

   public List<String> debugLines() {
      List<String> out = new ArrayList<>();
      LivingEntity t = this.getTarget();
      out.add(
         String.format(
            "%s v2=%s phase=%d/%d hp=%.0f/%.0f defense=%.2f move=%s (%d/%d) rest=%d",
            this.id,
            this.isV2(),
            this.phase(),
            this.thresholds().length,
            this.getHealth(),
            this.getMaxHealth(),
            this.defense(),
            this.currentAction(),
            this.actionTicks,
            this.current == null ? 0 : this.current.length,
            this.restTicks
         )
      );
      out.add(
         String.format(
            "poise=%.1f/%.1f staggered=%s immune=%d ward=%.1f/%.1f (%dt) enraged=%s combat=%d lastPhase=%d dodgeCd=%d",
            this.poise,
            this.isV2() ? this.poiseMax() : 0.0F,
            this.staggered(),
            this.staggerImmune,
            this.ward,
            this.wardMax,
            this.wardTicks,
            this.enraged(),
            this.combatTicks,
            this.lastPhaseTicks,
            this.dodgeCooldown
         )
      );
      out.add(
         "target="
            + (t == null ? "-" : t.getName().getString())
            + " engaged="
            + this.engaged.size()
            + " marked="
            + this.markTicks
            + " unreachableFor="
            + this.unreachableFor
            + " leaping="
            + this.leaping()
      );
      out.addAll(this.ctx.describe());
      StringBuilder th = new StringBuilder("threat:");

      for (Entry<UUID, Float> e : this.threat.view().entrySet()) {
         Player p = this.level().getPlayerByUUID(e.getKey());
         th.append(' ')
            .append(p == null ? e.getKey().toString().substring(0, 8) : p.getName().getString())
            .append('=')
            .append(String.format("%.1f", e.getValue()));
      }

      out.add(th.toString());
      List<Entry<String, Float>> scores = new ArrayList<>(this.lastScores.entrySet());
      scores.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));
      StringBuilder sb = new StringBuilder("last pick=" + this.lastPicked + " scores:");

      for (Entry<String, Float> e : scores) {
         sb.append(' ').append(e.getKey()).append('=').append(String.format("%.1f", e.getValue()));
      }

      out.add(sb.toString());
      return out;
   }

   public static void debugGuard(MysticalBoss.@Nullable DebugGuard guard) {
      debugGuard = guard;
   }

   public void tick() {
      MysticalBoss.DebugGuard guard = debugGuard;
      if (guard != null && !this.level().isClientSide()) {
         String before = this.currentAction();

         try {
            super.tick();
         } catch (RuntimeException | LinkageError e) {
            String now = this.currentAction();
            guard.failed(this, now.equals("-") ? before : now, e);
            if (this.level() instanceof ServerLevel level) {
               try {
                  this.cancelMove(level, 20);
               } catch (RuntimeException | LinkageError var7) {
               }
            }
         }
      } else {
         super.tick();
      }
   }

   public void debugSkipIntro() {
      if (this.inIntro()) {
         this.entityData.set(INTRO, 1);
      }
   }

   public int debugSetPhase(int n) {
      if (this.level() instanceof ServerLevel level && !this.isDeadOrDying() && !this.inIntro()) {
         float[] t = this.thresholds();
         int target = Mth.clamp(n, 0, t.length);
         if (target < this.phase()) {
            this.interrupt(level);
            this.entityData.set(PHASE, target);
            this.setHealth(target == 0 ? this.getMaxHealth() : Math.max(1.0F, this.getMaxHealth() * (t[target - 1] - 0.02F)));
            return target;
         }

         for (int p = this.phase() + 1; p <= target; p++) {
            float next = p < t.length ? t[p] : 0.0F;
            float frac = t[p - 1] - Math.min(0.02F, (t[p - 1] - next) * 0.5F);
            this.setHealth(Math.max(1.0F, Math.min(this.getHealth(), this.getMaxHealth() * frac)));
            this.checkPhase(level);
         }

         return this.phase();
      } else {
         return this.phase();
      }
   }

   public boolean debugCast(String anim) {
      if (this.level() instanceof ServerLevel level && !this.isDeadOrDying() && !this.inIntro()) {
         int i = this.indexOf(anim);
         if (i >= 0) {
            this.choose(level, i, this.getTarget());
            return true;
         }

         if (!this.isV2()) {
            return false;
         }

         if (anim.equals("stagger")) {
            this.startStagger(level, this.getTarget());
            return true;
         }

         for (int k = 0; k < DODGES.length; k++) {
            if (DODGES[k].equals(anim)) {
               this.stopMobility();
               this.startDodge(level, k, this.dodgeDirs()[k], this.getTarget());
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public List<String> debugMoves() {
      List<String> out = new ArrayList<>();

      for (BossAction a : this.actions) {
         out.add(a.anim);
      }

      if (this.isV2()) {
         out.add("stagger");
         Collections.addAll(out, DODGES);
      }

      return out;
   }

   public List<BossAction> debugActions() {
      return Collections.unmodifiableList(this.actions);
   }

   public int debugScheduled() {
      return this.scheduled.size();
   }

   protected RawAnimation anim(String name) {
      return RawAnimation.begin().thenPlay("animation." + this.id + "." + name);
   }

   protected RawAnimation loopAnim(String name) {
      return RawAnimation.begin().thenLoop("animation." + this.id + "." + name);
   }

   protected RawAnimation holdAnim(String name) {
      return RawAnimation.begin().thenPlayAndHold("animation." + this.id + "." + name);
   }

   protected String moveAnim() {
      return this.isV2() && this.running() ? "run" : "walk";
   }

   protected String idleAnim() {
      return "idle";
   }

   public void registerControllers(ControllerRegistrar controllers) {
      RawAnimation death = this.holdAnim("death");
      RawAnimation summon = this.holdAnim("summon");
      controllers.add(new AnimationController("main", 8, test -> {
         if (this.isDeadOrDying()) {
            return test.setAndContinue(death);
         }

         if (this.inIntro()) {
            return test.setAndContinue(summon);
         }

         boolean moving = test.isMoving() || this.hovers() && this.getDeltaMovement().horizontalDistanceSqr() > 0.002;
         return test.setAndContinue(this.loopAnim(moving ? this.moveAnim() : this.idleAnim()));
      }));
      AnimationController<MysticalBoss> action = new AnimationController("action", 2, test -> PlayState.STOP);

      for (BossAction a : this.actions) {
         action.triggerableAnim(a.anim, a.length > 0 && this.isHeld(a.anim) ? this.holdAnim(a.anim) : this.anim(a.anim));
      }

      if (this.isV2()) {
         for (int n = 1; n <= 3; n++) {
            action.triggerableAnim("phase_" + n, this.anim("phase_" + n));
         }

         action.triggerableAnim("stagger", this.anim("stagger"));

         for (String d : DODGES) {
            action.triggerableAnim(d, this.anim(d));
         }
      } else {
         action.triggerableAnim("phase", this.anim("phase"));
      }

      controllers.add(action);
   }

   protected boolean isHeld(String anim) {
      return false;
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   @FunctionalInterface
   public interface DebugGuard {
      void failed(MysticalBoss var1, String var2, Throwable var3);
   }

   private record Scheduled(int at, Runnable run) {
   }
}
