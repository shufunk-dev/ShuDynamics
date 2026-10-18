package com.vexira.mysticalterrain.boss.entity;

import com.vexira.mysticalterrain.boss.BossAction;
import com.vexira.mysticalterrain.boss.BossBolt;
import com.vexira.mysticalterrain.boss.BossFx;
import com.vexira.mysticalterrain.boss.BossSounds;
import com.vexira.mysticalterrain.boss.MysticalBoss;
import com.vexira.mysticalterrain.boss.RealmMob;
import com.vexira.mysticalterrain.boss.RealmMobs;
import com.vexira.mysticalterrain.boss.ai.BossContext;
import com.vexira.mysticalterrain.boss.vfx.Vfx;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;

public class HrimthaneBoss extends MysticalBoss {
   private static final BossSounds SOUNDS = BossSounds.of("hrimthane");
   private static final Vfx.Palette FROST = Vfx.Palette.FROST;
   private static final int PALE = 15268095;
   public static final float WIDTH = 3.0F;
   public static final float HEIGHT = 4.0F;
   public static final boolean FIRE_IMMUNE = false;
   private static final double[][] SHATTER = new double[][]{
      {112.0, 0.8, 0.0, 6.2},
      {113.0, 0.26, 0.0, 5.4},
      {114.0, 0.0, 0.0, 5.05},
      {116.0, -0.3, 0.0, 4.7},
      {117.0, -0.67, 0.0, 4.2},
      {118.0, -1.03, 0.0, 3.7},
      {116.0, -3.0, 3.8, 3.9},
      {117.0, -3.0, -3.8, 3.9},
      {119.0, -2.2, 1.35, 3.0},
      {120.0, -2.2, -1.35, 3.0},
      {114.0, -9.0, 0.0, 0.4},
      {116.0, -8.2, 0.0, 0.5},
      {118.0, -7.6, 0.0, 0.5},
      {120.0, -7.1, 0.0, 0.6},
      {122.0, -6.5, 0.0, 0.6},
      {124.0, -5.95, 0.0, 0.7},
      {126.0, -5.4, 0.0, 0.8},
      {128.0, -4.75, 0.0, 0.8},
      {130.0, -4.1, 0.0, 0.9},
      {132.0, -3.5, 0.0, 1.0},
      {133.0, -2.8, 0.8, 0.4},
      {133.0, -2.8, -0.8, 0.4},
      {134.0, -2.8, 0.0, 1.1},
      {136.0, -2.1, 0.0, 1.3},
      {138.0, -1.4, 0.0, 1.5},
      {139.0, 0.8, 1.15, 3.2},
      {140.0, 0.8, -1.15, 3.2}
   };
   private int burrowTicks;
   private @Nullable Vec3 burrowLock;
   private @Nullable UUID burrowHunt;
   private int diveChain;
   private int stormTicks;
   private int nextFallingIce;
   private final List<HrimthaneBoss.Ice> ice = new ArrayList<>();
   private int markCooldown;
   private @Nullable UUID huntMark;
   private int huntMarkUntil;
   private boolean desperate;
   private boolean pendingZero;
   private float lockedYaw;
   private float breathBaseYaw;
   private @Nullable Vec3 moveSpot;
   private @Nullable Vec3 flightFrom;
   private @Nullable Vec3 flightTo;
   private @Nullable Vec3 lanceAim;
   private @Nullable Vec3 dashStart;
   private Vec3 lungeDir = Vec3.ZERO;
   private double lungeSpeed = 1.3;
   private final Set<Integer> rammed = new HashSet<>();
   private final List<Vec3> spikeDirs = new ArrayList<>();
   private final List<BlockPos> prisonBlocks = new ArrayList<>();
   private int prisonRelease;
   private int minionCount;
   private final RawAnimation[] looks = new RawAnimation[4];
   private static final double CHOP_FROM = 1.5;
   private static final double CHOP_LEN = 4.5;
   private static final double CHOP_HALF = 1.8;
   private static final float REAP_AIM = 45.0F;
   private static final double REAP_REACH = 5.8;
   private static final double REAP_TOP = 1.9;

   public static Builder attributes() {
      return MysticalBoss.bossAttributes(960.0, 14.0, 0.28, 14.0);
   }

   public HrimthaneBoss(EntityType<? extends Monster> type, Level level) {
      super(type, level, "hrimthane", BossBarColor.BLUE);
   }

   @Override
   protected boolean v2() {
      return true;
   }

   @Override
   public float defense() {
      return 0.42F;
   }

   @Override
   public int bossLevel() {
      return 62;
   }

   @Override
   public float poiseMax() {
      return 150.0F;
   }

   @Override
   public int enrageTicks() {
      return 9600;
   }

   @Override
   protected double runSpeed() {
      return 1.6;
   }

   @Override
   protected double preferredDistance() {
      return 5.5;
   }

   @Override
   protected boolean canDodge() {
      return true;
   }

   @Override
   protected boolean preferBlink() {
      return false;
   }

   @Override
   protected float[] phaseThresholds() {
      return new float[]{0.75F, 0.5F, 0.25F};
   }

   @Override
   public int introTicks() {
      return 140;
   }

   @Override
   public int deathTicks() {
      return 160;
   }

   @Override
   protected boolean isHeld(String anim) {
      return anim.equals("frost_burrow");
   }

   @Override
   protected boolean immune() {
      return this.burrowTicks > 0;
   }

   @Override
   protected SoundEvent roarSound() {
      return SOUNDS.roar();
   }

   protected SoundEvent getAmbientSound() {
      return SOUNDS.ambient();
   }

   protected SoundEvent getHurtSound(DamageSource source) {
      return SOUNDS.hurt();
   }

   protected SoundEvent getDeathSound() {
      return SOUNDS.death();
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController("look", 10, test -> test.setAndContinue(this.lookAnim(Math.min(3, this.phase())))));
      super.registerControllers(controllers);
   }

   private RawAnimation lookAnim(int i) {
      RawAnimation r = this.looks[i];
      if (r == null) {
         r = this.loopAnim("look_" + i);
         this.looks[i] = r;
      }

      return r;
   }

   private Vec3 fwd() {
      return Vec3.directionFromRotation(0.0F, this.getYRot());
   }

   private Vec3 groundAt(ServerLevel level, Vec3 p) {
      Vec3 g = BossFx.ground(level, p.x, p.y, p.z);
      return g != null ? g : p;
   }

   private Vec3 front(ServerLevel level, double d) {
      return this.groundAt(level, this.position().add(this.fwd().scale(d)));
   }

   private Vec3 mouth() {
      return this.position().add(this.fwd().scale(4.4)).add(0.0, 3.9, 0.0);
   }

   private Vec3 mouthLow() {
      return this.position().add(this.fwd().scale(5.8)).add(0.0, 0.9, 0.0);
   }

   private Vec3 breathMouth(float yaw) {
      return this.position().add(Vec3.directionFromRotation(0.0F, yaw).scale(4.0)).add(0.0, 3.6, 0.0);
   }

   private Vec3 tailTip() {
      return this.position().subtract(this.fwd().scale(9.6)).add(0.0, 0.3, 0.0);
   }

   private Vec3 lanceMouth() {
      return this.position().add(this.fwd().scale(3.3)).add(0.0, 4.15, 0.0);
   }

   private Vec3 spitMouth() {
      return this.position().add(this.fwd().scale(3.9)).add(0.0, 3.4, 0.0);
   }

   private Vec3 crown() {
      return this.position().add(this.fwd().scale(1.1)).add(0.0, 5.75, 0.0);
   }

   private Vec3 turned(Vec3 at, float deg) {
      double dx = at.x - this.getX();
      double dz = at.z - this.getZ();
      float yaw = (float)(Mth.atan2(dz, dx) * 180.0F / (float)Math.PI) - 90.0F + deg;
      return this.position().add(Vec3.directionFromRotation(0.0F, yaw).scale(Math.sqrt(dx * dx + dz * dz)));
   }

   private double flatDist(Vec3 p) {
      double dx = p.x - this.getX();
      double dz = p.z - this.getZ();
      return Math.sqrt(dx * dx + dz * dz);
   }

   private double reaperReach() {
      return this.phase() >= 2 ? 11.0 : 10.0;
   }

   private void facePoint(Vec3 at, float maxTurn) {
      double dx = at.x - this.getX();
      double dz = at.z - this.getZ();
      if (!(dx * dx + dz * dz < 1.0E-4)) {
         float want = (float)(Mth.atan2(dz, dx) * 180.0F / (float)Math.PI) - 90.0F;
         float yaw = Mth.approachDegrees(this.getYRot(), want, maxTurn);
         this.setYRot(yaw);
         this.setYBodyRot(yaw);
         this.setYHeadRot(yaw);
      }
   }

   private void keepYaw() {
      this.setYRot(this.lockedYaw);
      this.setYBodyRot(this.lockedYaw);
      this.setYHeadRot(this.lockedYaw);
   }

   private boolean commitAt(int t, int at, @Nullable Vec3 aim, float maxTurn) {
      if (t > at) {
         this.keepYaw();
         return false;
      }

      if (aim != null) {
         this.facePoint(aim, maxTurn);
      }

      if (t < at) {
         return false;
      }

      this.lockedYaw = this.getYRot();
      return true;
   }

   private boolean commit(int t, int at, @Nullable LivingEntity target, float maxTurn) {
      return this.commitAt(t, at, target != null && t <= at ? target.position() : null, maxTurn);
   }

   private List<LivingEntity> inCone(ServerLevel level, Vec3 origin, float yaw, double range, float halfDeg) {
      Vec3 f = Vec3.directionFromRotation(0.0F, yaw);
      double cos = Math.cos(Math.toRadians(halfDeg));
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : BossFx.victims(level, this, origin, range)) {
         double dx = e.getX() - origin.x;
         double dz = e.getZ() - origin.z;
         double h = Math.sqrt(dx * dx + dz * dz);
         if (h < 1.0 || (dx * f.x + dz * f.z) / h >= cos) {
            out.add(e);
         }
      }

      return out;
   }

   private List<LivingEntity> alongLine(ServerLevel level, Vec3 from, Vec3 dir, double len, double halfWidth) {
      Vec3 d = new Vec3(dir.x, 0.0, dir.z);
      d = d.lengthSqr() < 1.0E-4 ? this.fwd() : d.normalize();
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : BossFx.victims(level, this, from.add(d.scale(len * 0.5)), len * 0.5 + halfWidth + 1.0)) {
         double rx = e.getX() - from.x;
         double rz = e.getZ() - from.z;
         double along = rx * d.x + rz * d.z;
         double side = Math.abs(rx * d.z - rz * d.x);
         double r = e.getBbWidth() * 0.5;
         if (along >= -r && along <= len + r && side <= halfWidth + r) {
            out.add(e);
         }
      }

      return out;
   }

   private List<LivingEntity> onFan(ServerLevel level, Vec3 origin, float yaw, float halfDeg, double range, double top) {
      Vec3 f = Vec3.directionFromRotation(0.0F, yaw);
      double half = Math.toRadians(halfDeg);
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : BossFx.victims(level, this, origin, range + 2.0)) {
         if (!(e.getY() - origin.y > top) && !(e.getY() + e.getBbHeight() < origin.y)) {
            double dx = e.getX() - origin.x;
            double dz = e.getZ() - origin.z;
            double h = Math.sqrt(dx * dx + dz * dz);
            double off = h < 1.0E-4 ? 0.0 : Math.acos(Mth.clamp((dx * f.x + dz * f.z) / h, -1.0, 1.0)) - half;
            double gap;
            if (off <= 0.0) {
               gap = h - range;
            } else if (off >= Math.PI / 2) {
               gap = h;
            } else if (h * Math.cos(off) <= range) {
               gap = h * Math.sin(off);
            } else {
               gap = Math.sqrt(h * h + range * range - 2.0 * h * range * Math.cos(off));
            }

            if (gap <= e.getBbWidth() * 0.5) {
               out.add(e);
            }
         }
      }

      return out;
   }

   private List<LivingEntity> victimsIn(ServerLevel level, AABB box) {
      return level.getEntitiesOfClass(
         LivingEntity.class,
         box,
         e -> e != this && e.isAlive() && !(e instanceof MysticalBoss) && !(e instanceof Player p && (p.isCreative() || p.isSpectator()))
      );
   }

   private static boolean airborne(ServerLevel level, LivingEntity e) {
      return e.isFallFlying() || !e.onGround() && BossContext.heightAboveGround(level, e) > 1.5;
   }

   private static boolean aiming(Player p) {
      if (!p.isUsingItem()) {
         return false;
      }

      ItemUseAnimation u = p.getUseItem().getUseAnimation();
      return u == ItemUseAnimation.BOW || u == ItemUseAnimation.CROSSBOW || u == ItemUseAnimation.TRIDENT;
   }

   private static void frostbite(LivingEntity e, int ticks) {
      int cap = e.getTicksRequiredToFreeze() + 160;
      e.setTicksFrozen(Math.max(e.getTicksFrozen(), Math.min(cap, e.getTicksFrozen() + ticks)));
   }

   private void slow(LivingEntity e, int ticks, int amplifier) {
      e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, amplifier), this);
   }

   private static void shove(LivingEntity e, Vec3 from, double push, double up) {
      Vec3 away = new Vec3(e.getX() - from.x, 0.0, e.getZ() - from.z);
      away = away.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : away.normalize();
      e.push(away.x * push, up, away.z * push);
      e.syncVelocity = true;
   }

   private static void breakGuard(LivingEntity e) {
      if (e instanceof Player p) {
         ItemStack s = p.getItemBlockingWith();
         if (s != null) {
            p.getCooldowns().addCooldown(s, 100);
            p.stopUsingItem();
         }
      }
   }

   private static BlockParticleOption snowBlock() {
      return new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SNOW_BLOCK.defaultBlockState());
   }

   private static void blockBurst(ServerLevel level, Vec3 at, BlockState state, int count, double spread, double speed) {
      level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), at.x, at.y, at.z, count, spread, spread * 0.5, spread, speed);
   }

   private int countMinions(ServerLevel level) {
      return level.getEntitiesOfClass(RealmMob.class, this.getBoundingBox().inflate(32.0), LivingEntity::isAlive).size();
   }

   private void summon(ServerLevel level, String kind, int n) {
      int count = Math.min(n, 5 - this.countMinions(level));
      if (count > 0) {
         BossFx.summonMinions(level, this, RealmMobs.type(kind), count, 8.0, null, m -> {});
      }

      this.minionCount = this.countMinions(level);
   }

   private void fallingIce(ServerLevel level, Vec3 spot, float base, int warn) {
      if (this.ice.size() < 32) {
         Vfx.telegraphCircle(level, spot, 2.0, warn, FROST);
         this.ice.add(new HrimthaneBoss.Ice(spot, base, this.tickCount + warn));
      }
   }

   private void tickIce(ServerLevel level) {
      if (!this.ice.isEmpty()) {
         Iterator<HrimthaneBoss.Ice> it = this.ice.iterator();

         while (it.hasNext()) {
            HrimthaneBoss.Ice c = it.next();
            int left = c.hitAt() - this.tickCount;
            if (left > 0) {
               if (left <= 16) {
                  Vfx.meteorTrail(level, c.spot().add(0.0, 14 * left / 16.0, 0.0), new Vec3(0.0, -0.9, 0.0), FROST);
               }
            } else {
               it.remove();
               BossFx.hurt(level, this, c.spot(), 2.0, c.base(), new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
               Vfx.heavyImpact(level, c.spot(), FROST, 0.6);
               Vfx.pillar(level, c.spot(), FROST, 3.0, 0.8);
               Vfx.shards(level, c.spot().add(0.0, 0.5, 0.0), FROST.core(), 10, 0.5);
            }
         }
      }
   }

   private Vec3 spotNear(ServerLevel level, Vec3 at, double r) {
      double a = this.random.nextDouble() * Math.PI * 2.0;
      double d = this.random.nextDouble() * r;
      return this.groundAt(level, new Vec3(at.x + Math.cos(a) * d, at.y, at.z + Math.sin(a) * d));
   }

   private void iceRing(ServerLevel level, Vec3 c, int count, double r, int warn, float base) {
      for (int k = 0; k < count; k++) {
         double a = k * Math.PI * 2.0 / count;
         Vec3 spot = this.groundAt(level, c.add(Math.cos(a) * r, 0.0, Math.sin(a) * r));
         Vfx.telegraphCircle(level, spot, 1.5, warn, FROST);
         this.schedule(warn, () -> {
            BossFx.hurt(level, this, spot, 1.5, base, new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
            Vfx.pillar(level, spot, FROST, 4.0, 0.9);
            Vfx.shards(level, spot.add(0.0, 0.5, 0.0), FROST.core(), 6, 0.4);
         });
      }
   }

   @Override
   protected void defineActions(List<BossAction> out) {
      this.huntMoves(out);
      this.rangedMoves(out);
      this.burrowMoves(out);
      this.wingMoves(out);
      this.carapaceMoves(out);
      this.crownMoves(out);
   }

   private void huntMoves(List<BossAction> out) {
      out.add(
         BossAction.of("rime_fang", 1.1, 0.55)
            .range(0.0, 7.0)
            .cooldown(50)
            .weight(14)
            .tags("melee")
            .lockFacing()
            .telegraph(0.2, 0.55)
            .advance(0.4, 0.55, 0.3)
            .combo("shatter_claws", 0.25F)
            .combo("tail_reaper", 0.15F)
            .tick((b, level, target, t) -> {
               if (this.commit(t, 4, target, 20.0F)) {
                  Vfx.telegraphCone(level, this.front(level, 1.5), this.lockedYaw, 28.0F, 7.5, 7, FROST);
               }
            })
            .hit((b, level, target, i) -> {
               BossFx.strike(level, this, 7.0, 30.0F, 20.0F, 0.9);
               BossFx.cone(level, this, 7.0, 30.0F, e -> {
                  this.slow(e, 60, 1);
                  frostbite(e, 60);
               });
               Vec3 m = this.mouthLow();
               Vfx.slashArc(level, m, this.lockedYaw, 25.0F, 90.0F, 3.0, FROST, 120.0F);
               Vfx.impact(level, m, FROST, 0.9);
               Vfx.shards(level, m, FROST.core(), 8, 0.4);
            })
      );
      out.add(
         BossAction.of("shatter_claws", 1.5, 0.45, 0.85)
            .range(0.0, 5.5)
            .cooldown(70)
            .weight(12)
            .tags("melee")
            .lockFacing()
            .telegraph(0.15, 0.85)
            .advance(0.3, 0.8, 0.12)
            .combo("glacier_crush", 0.25F)
            .combo("glacier_crush_feint", 0.15F)
            .tick((b, level, target, t) -> {
               if (t == 10) {
                  if (target != null) {
                     this.facePoint(target.position(), 30.0F);
                  }

                  this.lockedYaw = this.getYRot();
                  Vfx.telegraphCone(level, this.position(), this.lockedYaw, 55.0F, 5.5, 7, FROST);
               } else if (this.commit(t, 3, target, 20.0F)) {
                  Vfx.telegraphCone(level, this.position(), this.lockedYaw, 55.0F, 5.5, 6, FROST);
               }
            })
            .hit((b, level, target, i) -> {
               Vec3 at = this.position().add(this.fwd().scale(2.0)).add(0.0, 1.6, 0.0);
               if (i == 0) {
                  BossFx.strike(level, this, 5.5, 55.0F, 16.0F, 0.6);
                  Vfx.claw(level, at, this.lockedYaw, 25.0F, 4.5, FROST);
               } else {
                  BossFx.strike(level, this, 5.5, 55.0F, 18.0F, 1.1);
                  BossFx.cone(level, this, 5.5, 55.0F, e -> this.slow(e, 40, 0));
                  Vfx.slashArc(level, at, this.lockedYaw, 25.0F, -40.0F, 4.5, FROST, 120.0F);
               }

               Vfx.shards(level, at, FROST.core(), 6, 0.4);
            })
      );
      out.add(
         BossAction.of("glacier_crush", 1.9, 0.95)
            .range(0.0, 7.0)
            .cooldown(160)
            .weight(9)
            .tags("melee aoe guard_break")
            .superArmor()
            .lockFacing()
            .telegraph(0.3, 0.95)
            .utility((b, t, c) -> c.targetBlocking ? 1.5F : 1.0F)
            .tick((b, level, target, t) -> {
               if (this.commit(t, 6, target, 20.0F)) {
                  this.moveSpot = this.front(level, 3.5);
                  Vfx.telegraphCircle(level, this.moveSpot, 5.5, 13, FROST);
               }
            })
            .hit((b, level, target, i) -> this.crush(level, 34.0F, 1.8))
      );
      out.add(
         BossAction.of("glacier_crush_feint", 2.4, 1.45)
            .range(0.0, 7.0)
            .cooldown(160)
            .weight(1)
            .phase(1)
            .tags("melee aoe guard_break")
            .superArmor()
            .lockFacing()
            .telegraph(0.3, 1.45)
            .utility((b, t, c) -> 0.0F)
            .tick((b, level, target, t) -> {
               if (this.commit(t, 6, target, 20.0F)) {
                  this.moveSpot = this.front(level, 3.5);
                  Vfx.telegraphCircle(level, this.moveSpot, 5.5, 23, FROST);
               } else if (t == 18 && this.moveSpot != null) {
                  Vfx.debris(level, this.moveSpot, Vfx.Debris.SNOW, FROST.core(), 10, 0.3);
                  Vfx.shards(level, this.moveSpot.add(0.0, 0.5, 0.0), FROST.core(), 6, 0.3);
               }
            })
            .hit((b, level, target, i) -> this.crush(level, 38.0F, 2.0))
      );
      out.add(
         BossAction.of("tail_reaper", 1.4, 0.7)
            .range(0.0, 10.0)
            .cooldown(80)
            .weight(10)
            .tags("melee behind aoe")
            .lockFacing()
            .telegraph(0.1, 0.7)
            .when((b, t) -> b.ctx().targetBehind || b.ctx().surroundCount >= 2)
            .combo("rime_fang", 0.2F)
            .combo("maelstrom_coil", 0.1F)
            .start((b, level, target, i) -> this.lockedYaw = this.getYRot())
            .tick((b, level, target, t) -> {
               this.keepYaw();
               if (t == 2) {
                  Vfx.telegraphCone(level, this.position(), this.lockedYaw + 180.0F, 100.0F, this.reaperReach(), 12, FROST);
               }
            })
            .hit((b, level, target, i) -> {
               Vec3 pos = this.position();
               DamageSource src = this.damageSources().mobAttack(this);
               float base = this.phase() >= 2 ? 26.0F : 22.0F;

               for (LivingEntity e : this.inCone(level, pos, this.lockedYaw + 180.0F, this.reaperReach(), 100.0F)) {
                  e.hurtServer(level, src, this.dmg(base));
                  shove(e, pos, 1.6, 0.4);
                  this.slow(e, 40, 1);
                  frostbite(e, 60);
               }

               Vfx.slashArc(level, pos.subtract(this.fwd().scale(3.0)).add(0.0, 1.0, 0.0), this.lockedYaw + 180.0F, 0.0F, 0.0F, 7.0, FROST, -200.0F);
               Vfx.streaks(level, this.tailTip(), Vec3.directionFromRotation(0.0F, this.lockedYaw - 90.0F).scale(4.0), FROST, 12);
               Vfx.debris(level, this.tailTip(), Vfx.Debris.SNOW, FROST.core(), 12, 0.4);
            })
      );
   }

   private void crush(ServerLevel level, float base, double push) {
      Vec3 c = this.moveSpot != null ? this.moveSpot : this.front(level, 3.5);
      List<LivingEntity> hit = BossFx.victims(level, this, c, 5.5);

      for (LivingEntity e : hit) {
         breakGuard(e);
      }

      BossFx.slam(level, this, c, 5.5, base, push);

      for (LivingEntity e : hit) {
         frostbite(e, 80);
      }

      Vfx.heavyImpact(level, c, FROST, 1.2);
      Vfx.groundCrack(level, c, 5.5, FROST);
      Vfx.debris(level, c, Vfx.Debris.SNOW, FROST.core(), 20, 0.5);

      for (int k = 0; k < 6; k++) {
         double a = k * Math.PI / 3.0 + Math.toRadians(this.lockedYaw);
         Vfx.pillar(level, this.groundAt(level, c.add(Math.cos(a) * 5.0, 0.0, Math.sin(a) * 5.0)), FROST, 1.8, 0.6);
      }

      Vfx.shards(level, c.add(0.0, 0.5, 0.0), FROST.core(), 14, 0.7);
      if (this.phase() >= 2) {
         BossFx.wave(level, this, c, 10.0, 12, 14.0F, snowBlock(), 1.2, true);
      }
   }

   private void rangedMoves(List<BossAction> out) {
      out.add(
         BossAction.of("frost_breath", 3.0, 0.9, 1.3, 1.7, 2.1, 2.5)
            .range(3.0, 15.0)
            .cooldown(200)
            .weight(10)
            .tags("ranged aoe")
            .lockFacing()
            .telegraph(0.2, 0.9)
            .tick((b, level, target, t) -> {
               if (this.commitAt(t, 4, t <= 4 && target != null ? this.predict(target, 20.0) : null, 30.0F)) {
                  this.breathBaseYaw = this.lockedYaw;
                  Vfx.telegraphCone(level, this.front(level, 2.0), this.breathBaseYaw, 40.0F, 15.0, 14, FROST);
               }

               if (t >= 17 && t <= 51 && t % 2 == 1) {
                  float yaw = this.breathBaseYaw + (float)(-20.0 + 40.0 * Mth.clamp((t - 18) / 32.0, 0.0, 1.0));
                  Vec3 dir = Vec3.directionFromRotation(12.0F, yaw);
                  Vec3 m = this.breathMouth(yaw);
                  Vfx.streaks(level, m, dir.scale(12.0), FROST, 10);
                  Vfx.smoke(level, m.add(dir.scale(8.0)), FROST.smoke(), 3, 1.4, 1.6);
                  Vfx.debris(level, m.add(dir.scale(6.0)), Vfx.Debris.SNOW, FROST.core(), 4, 0.3);
                  if (t % 6 == 5) {
                     Vfx.shards(level, m.add(dir.scale(13.0)), FROST.core(), 6, 0.4);
                  }
               }
            })
            .hit((b, level, target, i) -> {
               float yaw = this.breathBaseYaw - 20.0F + 10 * i;
               Vec3 origin = this.front(level, 1.5);
               DamageSource src = this.damageSources().freeze();

               for (LivingEntity e : this.inCone(level, origin, yaw, 15.0, this.phase() >= 3 ? 22.0F : 18.0F)) {
                  e.hurtServer(level, src, this.dmg(9.0F));
                  this.slow(e, 60, 1);
                  frostbite(e, 40);
               }

               if (this.phase() >= 1 && i % 2 == 0) {
                  Vec3 spot = this.groundAt(level, origin.add(Vec3.directionFromRotation(0.0F, yaw).scale(8.0)));
                  BossFx.zone(level, this, spot, 2.5, 100, ParticleTypes.SNOWFLAKE, this.dmg(1.0F), new MobEffectInstance(MobEffects.SLOWNESS, 30, 1));
               }
            })
      );
      out.add(
         BossAction.of("icicle_barrage", 1.8, 0.6, 0.95, 1.3)
            .range(5.0, 28.0)
            .cooldown(100)
            .weight(10)
            .tags("ranged")
            .start((b, level, target, i) -> Vfx.chargeUp(level, this.spitMouth(), FROST, 12))
            .hit(
               (b, level, target, i) -> {
                  if (target != null) {
                     Vec3 m = this.spitMouth();
                     double lead = m.distanceTo(target.position()) / 1.4;
                     Vec3 aim = i == 2 ? target.position() : this.predict(target, lead * (i == 1 ? 1.2 : 1.0));
                     Vec3 dir = aim.add(0.0, 1.0, 0.0).subtract(m);
                     if (!(dir.lengthSqr() < 1.0E-4)) {
                        dir = dir.normalize();
                        int half = this.phase() >= 3 ? 3 : 2;

                        for (int k = -half; k <= half; k++) {
                           BossBolt bolt = BossBolt.shoot(level, this, BossBolt.Style.ICE, m, dir.yRot(k * 0.1F).scale(1.4), this.dmg(11.0F))
                              .effect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0));
                           if (this.phase() >= 1) {
                              bolt.homing(target, 0.03);
                           }
                        }

                        Vfx.flare(level, m, FROST.core(), 1.6, 5);
                        Vfx.glint(level, m, FROST.core(), 1.2);
                     }
                  }
               }
            )
      );
      out.add(
         BossAction.of("rime_spikes", 2.0, 0.9)
            .range(4.0, 22.0)
            .cooldown(140)
            .weight(9)
            .tags("ranged aoe")
            .lockFacing()
            .telegraph(0.3, 0.9)
            .tick((b, level, target, t) -> {
               Vec3 want = t <= 6 && target != null ? this.predict(target, 18.0) : null;
               if (this.commitAt(t, 6, want, 20.0F)) {
                  Vec3 d = want != null ? new Vec3(want.x - this.getX(), 0.0, want.z - this.getZ()) : this.fwd();
                  d = d.lengthSqr() < 1.0E-4 ? this.fwd() : d.normalize();
                  this.moveSpot = this.groundAt(level, this.position().add(d.scale(2.5)));
                  this.spikeDirs.clear();
                  this.spikeDirs.add(d);
                  if (this.phase() >= 2) {
                     this.spikeDirs.add(d.yRot((float)Math.toRadians(25.0)));
                     this.spikeDirs.add(d.yRot((float)Math.toRadians(-25.0)));
                  }

                  for (Vec3 s : this.spikeDirs) {
                     Vfx.telegraphLine(level, this.moveSpot, s, 21.0, 2.2, 12, FROST);
                  }
               }
            })
            .hit((b, level, target, i) -> this.spikes(level))
      );
      out.add(
         BossAction.of("glacial_lunge", 1.9, 1.25)
            .range(8.0, 20.0)
            .cooldown(150)
            .weight(9)
            .tags("melee gap_closer")
            .lockFacing()
            .telegraph(0.2, 1.25)
            .combo("rime_fang", 0.2F)
            .utility((b, t, c) -> c.kitingTicks > 40 ? 2.0F : 1.0F)
            .start((b, level, target, i) -> this.rammed.clear())
            .tick((b, level, target, t) -> {
               if (this.commitAt(t, 4, t <= 4 && target != null ? this.predict(target, 22.0) : null, 30.0F)) {
                  this.lungeDir = this.fwd();
                  double reach = target != null ? Math.min(20.0, this.flatDist(target.position())) : 10.0;
                  this.lungeSpeed = Mth.clamp((reach - 5.5) / 6.75, 0.9, 2.2);
                  Vfx.telegraphLine(level, this.position(), this.lungeDir, Math.min(reach + 2.0, 20.0), 3.0, 21, FROST);
               }

               if (t == 15) {
                  this.dashStart = this.position();
                  this.dash(this.lungeDir, this.lungeSpeed, 9);
               }

               if (t >= 15 && t <= 25) {
                  Vec3 d = this.lungeDir;
                  DamageSource src = this.damageSources().mobAttack(this);

                  for (LivingEntity e : BossFx.victims(level, this, this.position().add(d.scale(2.5)), 2.6)) {
                     if (this.rammed.add(e.getId())) {
                        e.hurtServer(level, src, this.dmg(20.0F));
                        e.push(d.x * 1.6, 0.5, d.z * 1.6);
                        e.syncVelocity = true;
                     }
                  }

                  Vfx.streaks(level, this.position().add(0.0, 1.0, 0.0), d.scale(3.0), FROST, 4);
                  if (t % 2 == 0) {
                     Vfx.debris(level, this.position(), Vfx.Debris.SNOW, FROST.core(), 4, 0.3);
                  }
               }
            })
            .hit((b, level, target, i) -> {
               BossFx.strike(level, this, 6.5, 30.0F, 26.0F, 1.2);
               Vfx.heavyImpact(level, this.mouthLow(), FROST, 0.8);
               if (this.dashStart != null) {
                  Vfx.dashTrail(level, this.dashStart, this.position(), FROST);
               }
            })
      );
      out.add(
         BossAction.of("hoarfrost_lance", 1.5, 0.85)
            .range(6.0, 32.0)
            .cooldown(140)
            .weight(7)
            .tags("ranged punish anti_air")
            .lockFacing()
            .telegraph(0.15, 0.85)
            .combo("icicle_barrage", 0.2F)
            .utility((b, t, c) -> !c.targetAiming && !c.targetFlying && !c.targetAirborne ? 0.5F : 2.0F)
            .tick(
               (b, level, target, t) -> {
                  if (this.commit(t, 3, target, 30.0F)) {
                     Vec3 m = this.mouth();
                     Vec3 aim = target != null ? this.predict(target, 14.0).add(0.0, 1.0, 0.0) : m.add(this.fwd().scale(20.0));
                     this.lanceAim = aim;
                     Vfx.beam(level, m, aim, FROST, 0.12, 14);
                     Vec3 flat = new Vec3(aim.x - m.x, 0.0, aim.z - m.z);
                     Vfx.telegraphLine(
                        level,
                        this.groundAt(level, m),
                        flat.lengthSqr() < 1.0E-4 ? this.fwd() : flat.normalize(),
                        Math.min(32.0, flat.length() + 2.0),
                        1.4,
                        14,
                        FROST
                     );
                  }
               }
            )
            .hit((b, level, target, i) -> this.lance(level))
      );
      out.add(
         BossAction.of("frozen_prison", 2.0, 1.0)
            .range(0.0, 26.0)
            .cooldown(360)
            .weight(5)
            .tags("ranged punish")
            .telegraph(0.5, 1.0)
            .utility((b, t, c) -> c.targetHealing ? 2.0F : (c.targetAiming ? 0.5F : 0.3F))
            .tick((b, level, target, t) -> {
               if (target != null) {
                  if (t == 2) {
                     Vfx.runeCircle(level, target.position(), 1.8, FROST, 18, 0.06F, Vfx.Rune.GLYPH);
                  } else if (t == 10) {
                     this.moveSpot = this.groundAt(level, this.predict(target, 10.0));
                     Vfx.telegraphCircle(level, this.moveSpot, 1.6, 10, FROST);
                  }
               }
            })
            .hit((b, level, target, i) -> this.prison(level, target))
      );
      out.add(
         BossAction.of("call_of_the_rime", 2.2, 1.3)
            .range(0.0, 40.0)
            .cooldown(800)
            .weight(5)
            .tags("summon")
            .anyTarget()
            .utility((b, t, c) -> this.minionCount >= 4 ? 0.0F : 1.0F)
            .tick((b, level, target, t) -> {
               if (t == 4) {
                  Vfx.runeCircle(level, this.position(), 5.0, FROST, 40, 0.05F, Vfx.Rune.GLYPH);
               }
            })
            .hit((b, level, target, i) -> {
               Vfx.nova(level, this.position(), FROST, 8.0);
               int p = this.phase();
               if (p == 0) {
                  this.summon(level, "frost_wisp", 3);
               } else if (p == 1) {
                  this.summon(level, "frost_wisp", 2);
                  this.summon(level, "rime_stalker", 1);
               } else {
                  this.summon(level, "rime_stalker", 2);
                  this.summon(level, "frost_wisp", 2);
               }
            })
      );
   }

   private void spikes(ServerLevel level) {
      Vec3 from = this.moveSpot != null ? this.moveSpot : this.front(level, 2.5);
      List<Vec3> dirs = this.spikeDirs.isEmpty() ? List.of(this.fwd()) : List.copyOf(this.spikeDirs);
      Vfx.heavyImpact(level, this.front(level, 2.0), FROST, 0.8);

      for (Vec3 d : dirs) {
         Vfx.telegraphLine(level, from, d, 21.0, 2.2, 14, FROST);
      }

      Set<Integer> struck = new HashSet<>();

      for (int k = 0; k < 14; k++) {
         double reach = 1.5 * k;
         Runnable burst = () -> {
            for (Vec3 d : dirs) {
               this.spike(level, from.add(d.scale(reach)), struck);
            }
         };
         if (k == 0) {
            burst.run();
         } else {
            this.schedule(k, burst);
         }
      }
   }

   private void spike(ServerLevel level, Vec3 p, Set<Integer> struck) {
      Vec3 at = BossFx.ground(level, p.x, p.y, p.z);
      if (at != null) {
         Vfx.pillar(level, at, FROST, 2.8, 0.7);
         Vfx.shards(level, at.add(0.0, 0.6, 0.0), FROST.core(), 6, 0.5);
         DamageSource src = this.damageSources().mobAttack(this);

         for (LivingEntity e : BossFx.victims(level, this, at, 1.4)) {
            if (struck.add(e.getId())) {
               e.hurtServer(level, src, this.dmg(18.0F));
               this.slow(e, 60, 1);
               e.push(0.0, 0.8, 0.0);
               e.syncVelocity = true;
            }
         }
      }
   }

   private void lance(ServerLevel level) {
      Vec3 m = this.lanceMouth();
      Vec3 aim = this.lanceAim != null ? this.lanceAim : m.add(this.fwd().scale(20.0));
      Vec3 dir = aim.subtract(m);
      dir = dir.lengthSqr() < 1.0E-4 ? this.fwd() : dir.normalize();
      Vec3 end = m.add(dir.scale(32.0));
      HitResult block = level.clip(new ClipContext(m, end, Block.COLLIDER, Fluid.NONE, this));
      if (block.getType() != Type.MISS) {
         end = block.getLocation();
      }

      double len = end.distanceTo(m);
      DamageSource src = this.damageSources().mobAttack(this);
      Vec3 first = null;
      double firstAlong = Double.MAX_VALUE;

      for (LivingEntity e : this.victimsIn(level, new AABB(m, end).inflate(1.6))) {
         Vec3 c = e.position().add(0.0, e.getBbHeight() * 0.5, 0.0);
         double along = Mth.clamp(c.subtract(m).dot(dir), 0.0, len);
         if (!(c.distanceTo(m.add(dir.scale(along))) > 1.2 + e.getBbWidth() * 0.5)) {
            e.hurtServer(level, src, this.dmg(28.0F));
            this.slow(e, 60, 2);
            frostbite(e, 100);
            e.stopFallFlying();
            e.push(0.0, -0.8, 0.0);
            e.syncVelocity = true;
            if (along < firstAlong) {
               firstAlong = along;
               first = c;
            }
         }
      }

      Vfx.beam(level, m, end, FROST, 0.6, 6);

      for (double s = 3.0; s < len; s += 3.0) {
         Vfx.shards(level, m.add(dir.scale(s)), FROST.core(), 3, 0.25);
      }

      Vfx.impact(level, first != null ? first : end, FROST, 1.0);
      Vfx.streaks(level, m, dir.scale(6.0), FROST, 10);
   }

   private void prison(ServerLevel level, @Nullable LivingEntity target) {
      Vec3 spot = this.moveSpot;
      if (spot != null) {
         Vfx.pillar(level, spot, FROST, 4.0, 1.2);
         Vfx.shards(level, spot.add(0.0, 1.0, 0.0), FROST.core(), 12, 0.5);
         if (target != null && target.isAlive()) {
            double dx = target.getX() - spot.x;
            double dz = target.getZ() - spot.z;
            if (!(dx * dx + dz * dz > 3.24)) {
               this.placePrison(level, target.blockPosition());
               target.hurtServer(level, this.damageSources().freeze(), this.dmg(14.0F));
               frostbite(target, 140);
               this.slow(target, 60, 3);
               this.schedule(40, () -> {
                  this.releasePrison(level);
                  Vfx.explosion(level, spot.add(0.0, 1.0, 0.0), FROST, 1.8);
                  if (target.isAlive() && target.position().distanceToSqr(spot) <= 6.25) {
                     target.hurtServer(level, this.damageSources().mobAttack(this), this.dmg(20.0F));
                  }
               });
            }
         }
      }
   }

   private void placePrison(ServerLevel level, BlockPos c) {
      this.releasePrison(level);
      BlockState state = Blocks.PACKED_ICE.defaultBlockState();
      AABB self = this.getBoundingBox();

      for (BlockPos p : BlockPos.betweenClosed(c.offset(-1, -1, -1), c.offset(1, 2, 1))) {
         boolean inside = p.getX() == c.getX() && p.getZ() == c.getZ() && p.getY() >= c.getY() && p.getY() <= c.getY() + 1;
         if (!inside && level.getBlockState(p).isAir() && !self.intersects(new AABB(p))) {
            level.setBlockAndUpdate(p, state);
            this.prisonBlocks.add(p.immutable());
         }
      }

      this.prisonRelease = this.tickCount + 60;
   }

   private void releasePrison(ServerLevel level) {
      if (!this.prisonBlocks.isEmpty()) {
         BlockParticleOption shard = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState());

         for (BlockPos p : this.prisonBlocks) {
            if (level.isLoaded(p) && level.getBlockState(p).is(Blocks.PACKED_ICE)) {
               level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
               level.sendParticles(shard, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.05);
            }
         }

         this.prisonBlocks.clear();
      }
   }

   private void burrowMoves(List<BossAction> out) {
      out.add(
         BossAction.of("frost_burrow", 1.6, 1.2)
            .range(0.0, 48.0)
            .cooldown(380)
            .weight(7)
            .tags("mobility gap_closer")
            .anyTarget()
            .telegraph(0.5, 1.2)
            .utility(
               (b, t, c) -> (!c.targetUnreachable || c.unreachableTicks <= 40) && this.farMarked() == null
                  ? (c.kitingTicks > 80 ? 3.0F : (c.dist > 14.0 ? 1.5F : (c.dist < 10.0 ? 0.4F : 1.0F)))
                  : 3.0F
            )
            .tick((b, level, target, t) -> {
               if (t == 10) {
                  Vfx.telegraphCircle(level, this.position(), 4.0, 14, FROST);
               }
            })
            .hit((b, level, target, i) -> this.dive(level, target))
            .end((b, level, target, i) -> {
               if (i == -2 && this.burrowTicks > 0) {
                  this.surface();
               }
            })
      );
      out.add(
         BossAction.of("erupt", 1.7, 0.3)
            .range(0.0, 64.0)
            .cooldown(0)
            .weight(1)
            .tags("melee aoe anti_air")
            .anyTarget()
            .utility((b, t, c) -> 0.0F)
            .combo("rime_fang", 0.25F)
            .combo("tail_reaper", 0.15F)
            .start((b, level, target, i) -> this.setInvisible(false))
            .hit((b, level, target, i) -> this.erupt(level))
            .end(
               (b, level, target, i) -> {
                  if (i == -1
                     && this.phase() >= 2
                     && this.diveChain < (!this.desperate && this.phase() < 3 ? 2 : 3)
                     && this.random.nextFloat() < (this.phase() >= 3 ? 0.6F : 0.45F)) {
                     this.diveChain++;
                     this.forceAction(level, "frost_burrow");
                  } else {
                     this.diveChain = 0;
                  }
               }
            )
      );
   }

   private void dive(ServerLevel level, @Nullable LivingEntity target) {
      Vec3 c = this.position();
      BossFx.slam(level, this, c, 4.0, 12.0F, 1.2);
      Vfx.heavyImpact(level, c, FROST, 1.0);
      Vfx.groundCrack(level, c, 4.0, FROST);
      Vfx.debris(level, c, Vfx.Debris.SNOW, FROST.core(), 16, 0.5);
      blockBurst(level, c.add(0.0, 0.5, 0.0), Blocks.PACKED_ICE.defaultBlockState(), 60, 1.5, 0.3);
      this.setInvisible(true);
      this.burrowTicks = 40 + 4 * this.phase();
      this.burrowLock = null;
      ServerPlayer mark = this.farMarked();
      UUID hunt = mark != null ? mark.getUUID() : null;
      if (hunt == null) {
         float best = -1.0F;

         for (ServerPlayer p : this.engaged()) {
            float th = this.threat().get(p);
            if (p.distanceToSqr(this) > 100.0 && th > best) {
               best = th;
               hunt = p.getUUID();
            }
         }
      }

      if (hunt == null && target != null) {
         hunt = target.getUUID();
      }

      this.burrowHunt = hunt;
   }

   private void surface() {
      this.burrowTicks = 0;
      this.burrowLock = null;
      this.setInvisible(false);
   }

   private void erupt(ServerLevel level) {
      Vec3 c = this.position();
      List<LivingEntity> hit = BossFx.victims(level, this, c, 4.5);
      BossFx.slam(level, this, c, 4.5, 36.0F, 1.2);
      BossFx.launch(level, this, c, 4.5, 1.1);

      for (LivingEntity e : hit) {
         frostbite(e, 100);
      }

      DamageSource src = this.damageSources().mobAttack(this);

      for (LivingEntity e : this.victimsIn(level, new AABB(c.x - 2.5, c.y, c.z - 2.5, c.x + 2.5, c.y + 10.0, c.z + 2.5))) {
         if (e.getY() > c.y + 2.0) {
            e.hurtServer(level, src, this.dmg(30.0F));
            e.stopFallFlying();
            this.slow(e, 60, 1);
         }
      }

      Vfx.explosion(level, c.add(0.0, 1.0, 0.0), FROST, 3.5);
      Vfx.pillar(level, c, FROST, 10.0, 1.8);
      Vfx.heavyImpact(level, c, FROST, 1.4);
      Vfx.debris(level, c, Vfx.Debris.SNOW, FROST.core(), 24, 0.6);
      blockBurst(level, c.add(0.0, 0.5, 0.0), Blocks.PACKED_ICE.defaultBlockState(), 100, 2.0, 0.5);
      Vfx.shards(level, c.add(0.0, 1.0, 0.0), FROST.core(), 26, 0.9);
      if (this.phase() >= 1) {
         BossFx.zone(level, this, c, 3.2, 100, ParticleTypes.SNOWFLAKE, this.dmg(2.0F), new MobEffectInstance(MobEffects.SLOWNESS, 30, 1));
      }
   }

   private void wingMoves(List<BossAction> out) {
      out.add(
         BossAction.of("skyfall_crash", 2.6, 1.7)
            .range(7.0, 26.0)
            .cooldown(260)
            .weight(8)
            .phase(1)
            .tags("gap_closer mobility aoe")
            .superArmor()
            .lockFacing()
            .telegraph(0.55, 1.7)
            .tick((b, level, target, t) -> {
               if (t < 11) {
                  if (target != null) {
                     this.facePoint(target.position(), 20.0F);
                  }
               } else if (t == 11) {
                  this.takeOff(level, target);
               } else if (t < 34) {
                  this.flightStep(level, t);
               }
            })
            .hit((b, level, target, i) -> this.skyfall(level))
            .end((b, level, target, i) -> this.setNoGravity(false))
      );
      out.add(
         BossAction.of("gale_of_ruin", 2.2, 0.9, 1.3)
            .range(0.0, 24.0)
            .cooldown(220)
            .weight(7)
            .phase(1)
            .tags("anti_air aoe ranged")
            .lockFacing()
            .telegraph(0.2, 1.3)
            .combo("frost_breath", 0.3F)
            .utility((b, t, c) -> c.surroundCount >= 2 ? 1.5F : 1.0F)
            .tick((b, level, target, t) -> {
               if (this.commit(t, 4, target, 20.0F)) {
                  Vfx.telegraphCone(level, this.position(), this.lockedYaw, 70.0F, 16.0, 14, FROST);
               } else if (t == 19) {
                  Vfx.telegraphCone(level, this.position(), this.lockedYaw, 70.0F, 16.0, 7, FROST);
               }
            })
            .hit((b, level, target, i) -> this.gale(level, i))
      );
      out.add(
         BossAction.of("rime_bulwark", 2.0, 0.9)
            .range(0.0, 40.0)
            .cooldown(640)
            .weight(6)
            .phase(1)
            .tags("defensive")
            .anyTarget()
            .superArmor()
            .telegraph(0.3, 0.9)
            .utility((b, t, c) -> b.warded() ? 0.0F : (c.surroundCount < 2 && !(c.recentDamage > 0.08F) ? 1.0F : 2.0F))
            .tick((b, level, target, t) -> {
               if (t == 6) {
                  Vfx.telegraphCircle(level, this.position(), 5.0, 12, FROST);
               }
            })
            .hit((b, level, target, i) -> {
               Vec3 pos = this.position();
               this.shield(this.getMaxHealth() * (this.phase() >= 2 ? 0.08F : 0.06F), 200);
               Vfx.shieldBubble(level, pos.add(0.0, 1.5, 0.0), 4.0, FROST, 200);
               BossFx.slam(level, this, pos, 5.0, 10.0F, 1.4);
               Vfx.nova(level, pos, FROST, 5.0);
            })
      );
      out.add(
         BossAction.of("avalanche", 3.4, 1.6)
            .range(0.0, 20.0)
            .cooldown(720)
            .weight(6)
            .phase(1)
            .tags("ultimate aoe")
            .anyTarget()
            .superArmor()
            .telegraph(0.3, 1.6)
            .tick((b, level, target, t) -> {
               if (t == 6) {
                  Vfx.telegraphCircle(level, this.position(), 8.0, 26, FROST);
                  Vfx.chargeUp(level, this.position().add(0.0, 6.0, 0.0), FROST, 26);
               }
            })
            .hit((b, level, target, i) -> this.avalanche(level))
      );
   }

   private void takeOff(ServerLevel level, @Nullable LivingEntity target) {
      Vec3 from = this.position();
      Vec3 to = from;
      if (target != null) {
         Vec3 want = this.predict(target, 23.0);
         Vec3 g = BossFx.ground(level, want.x, want.y, want.z);
         if (g != null && BossFx.safeGround(level, g, 3.0) && level.noCollision(this, this.getBoundingBox().move(g.subtract(from)))) {
            to = g;
         }
      }

      this.flightFrom = from;
      this.flightTo = to;
      this.setNoGravity(true);
      if (to.subtract(from).horizontalDistanceSqr() > 0.25) {
         this.facePoint(to, 180.0F);
      }

      this.lockedYaw = this.getYRot();
      Vfx.telegraphCircle(level, to, 5.5, 23, FROST);
      Vfx.debris(level, from, Vfx.Debris.SNOW, FROST.core(), 16, 0.5);
      Vfx.streaks(level, from.add(0.0, 1.0, 0.0), new Vec3(0.0, 3.0, 0.0), FROST, 8);
   }

   private void flightStep(ServerLevel level, int t) {
      if (this.flightFrom != null && this.flightTo != null) {
         double u = (t - 11) / 23.0;
         Vec3 p = this.flightFrom.lerp(this.flightTo, u).add(0.0, 28.0 * u * (1.0 - u), 0.0);
         Vec3 v = p.subtract(this.position());
         if (v.lengthSqr() > 9.0) {
            v = v.normalize().scale(3.0);
         }

         this.setDeltaMovement(v);
         this.keepYaw();
         if (t % 2 == 0 && v.lengthSqr() > 1.0E-4) {
            Vfx.streaks(level, this.position().add(0.0, 2.0, 0.0), v.normalize().scale(3.0), FROST, 6);
         }

         if (t == 16 || t == 24) {
            Vfx.debris(level, this.position().add(0.0, 2.5, 0.0), Vfx.Debris.SNOW, FROST.core(), 12, 0.5);
         }
      }
   }

   private void skyfall(ServerLevel level) {
      Vec3 c = this.flightTo != null ? this.flightTo : this.position();
      if (level.noCollision(this, this.getBoundingBox().move(c.subtract(this.position())))) {
         this.setPos(c.x, c.y, c.z);
      }

      this.setDeltaMovement(Vec3.ZERO);
      this.setNoGravity(false);
      List<LivingEntity> hit = BossFx.victims(level, this, c, 5.5);
      BossFx.slam(level, this, c, 5.5, 38.0F, 1.6);

      for (LivingEntity e : hit) {
         frostbite(e, 80);
      }

      Vfx.heavyImpact(level, c, FROST, 1.6);
      Vfx.groundCrack(level, c, 5.5, FROST);
      Vfx.nova(level, c, FROST, 5.5);
      if (this.phase() >= 2) {
         this.iceRing(level, c, 6, 7.0, 8, 18.0F);
      }
   }

   private void gale(ServerLevel level, int i) {
      Vec3 pos = this.position();
      Vec3 f = this.fwd();
      float base = i == 0 ? 12.0F : 14.0F;
      double push = i == 0 ? 2.0 : 2.4;
      DamageSource src = this.damageSources().mobAttack(this);
      List<LivingEntity> hit = this.inCone(level, pos, this.lockedYaw, 16.0, 70.0F);

      for (LivingEntity e : hit) {
         boolean air = airborne(level, e);
         e.hurtServer(level, src, this.dmg(base));
         e.push(f.x * push, 0.3, f.z * push);
         if (air) {
            e.stopFallFlying();
            Vec3 v = e.getDeltaMovement();
            e.setDeltaMovement(v.x, -1.0, v.z);
            if (i == 1) {
               e.removeEffect(MobEffects.SLOW_FALLING);
               e.removeEffect(MobEffects.LEVITATION);
            }
         }

         e.syncVelocity = true;
         this.slow(e, 60, 1);
      }

      for (ServerPlayer p : this.engaged()) {
         if (!hit.contains(p) && p.distanceToSqr(this) <= 576.0 && airborne(level, p)) {
            p.push(0.0, -0.6, 0.0);
            p.syncVelocity = true;
         }
      }

      Vfx.streaks(level, pos.add(0.0, 2.0, 0.0), f.scale(6.0), FROST, 16);
      Vfx.debris(level, pos.add(f.scale(6.0)), Vfx.Debris.SNOW, FROST.core(), 24, 0.8);
      Vfx.smoke(level, pos.add(f.scale(5.0)), FROST.smoke(), 10, 4.0, 2.0);
      Vec3 side = Vec3.directionFromRotation(0.0F, this.lockedYaw - 90.0F).scale(4.0);
      Vfx.vortex(level, pos.add(side).add(0.0, 3.0, 0.0), 1.5, 2.0, FROST, 10);
      Vfx.vortex(level, pos.subtract(side).add(0.0, 3.0, 0.0), 1.5, 2.0, FROST, 10);
   }

   private void avalanche(ServerLevel level) {
      Vec3 c = this.position();
      BossFx.slam(level, this, c, 8.0, 52.0F, 2.2);
      BossFx.wave(level, this, c, 20.0, 20, 30.0F, snowBlock(), 1.4, true);
      int n = 0;

      for (ServerPlayer p : this.engaged()) {
         if (n >= 8) {
            break;
         }

         this.fallingIce(level, this.spotNear(level, this.predict(p, 20.0), 2.0), 18.0F, 20);
         n++;
      }

      while (n < 8) {
         this.fallingIce(level, BossFx.around(level, this, c, 6.0, 18.0), 18.0F, 20);
         n++;
      }

      Vfx.heavyImpact(level, c, FROST, 2.5);
      Vfx.groundCrack(level, c, 8.0, FROST);
      Vfx.nova(level, c, FROST, 8.0);
      Vfx.debris(level, c, Vfx.Debris.SNOW, FROST.core(), 30, 1.0);
   }

   private void carapaceMoves(List<BossAction> out) {
      out.add(
         BossAction.of("glacial_scythe", 2.0, 0.75, 1.35)
            .range(0.0, 7.0)
            .cooldown(150)
            .weight(11)
            .phase(2)
            .tags("melee aoe")
            .lockFacing()
            .telegraph(0.2, 1.35)
            .combo("maelstrom_coil", 0.2F)
            .tick((b, level, target, t) -> {
               if (t == 17) {
                  if (target != null) {
                     this.facePoint(this.turned(target.position(), -45.0F), 25.0F);
                  }

                  this.lockedYaw = this.getYRot();
                  Vfx.telegraphCone(level, this.position(), this.lockedYaw + 45.0F, 45.0F, 5.8, 10, FROST);
               } else if (this.commitAt(t, 4, t <= 4 && target != null ? this.turned(target.position(), -30.0F) : null, 20.0F)) {
                  Vec3 chop = Vec3.directionFromRotation(0.0F, this.lockedYaw + 30.0F);
                  Vfx.telegraphLine(level, this.groundAt(level, this.position().add(chop.scale(1.5))), chop, 4.5, 3.6, 11, FROST);
               }
            })
            .hit((b, level, target, i) -> this.scythe(level, i))
      );
      out.add(
         BossAction.of("maelstrom_coil", 2.4, 0.8, 1.4)
            .range(0.0, 9.0)
            .cooldown(200)
            .weight(8)
            .phase(2)
            .tags("melee aoe")
            .superArmor()
            .lockFacing()
            .telegraph(0.1, 1.4)
            .utility((b, t, c) -> c.surroundCount < 2 && !c.targetBehind ? 1.0F : 2.0F)
            .start((b, level, target, i) -> this.lockedYaw = this.getYRot())
            .tick((b, level, target, t) -> {
               this.keepYaw();
               if (t == 2) {
                  Vfx.telegraphCircle(level, this.position(), 8.5, 14, FROST);
               } else if (t == 18) {
                  Vfx.telegraphCircle(level, this.position(), 9.5, 10, FROST);
               }
            })
            .hit((b, level, target, i) -> {
               Vec3 pos = this.position();
               if (i == 0) {
                  BossFx.slam(level, this, pos, 8.5, 24.0F, 1.4);
                  Vfx.slashArc(level, pos.add(0.0, 1.0, 0.0), this.lockedYaw + 180.0F, 0.0F, 0.0F, 8.0, FROST, 330.0F);
               } else {
                  List<LivingEntity> hit = BossFx.victims(level, this, pos, 9.5);
                  BossFx.slam(level, this, pos, 9.5, 28.0F, 2.0);

                  for (LivingEntity e : hit) {
                     this.slow(e, 60, 1);
                  }

                  Vfx.slashArc(level, pos.add(0.0, 0.8, 0.0), this.lockedYaw, 0.0F, 0.0F, 9.0, FROST, -330.0F);
                  Vfx.vortex(level, pos, 8.0, 3.0, FROST, 16);
                  Vfx.debris(level, pos, Vfx.Debris.SNOW, FROST.core(), 20, 0.8);
               }
            })
      );
      out.add(
         BossAction.of("eternal_winter", 4.0, 2.0)
            .range(0.0, 40.0)
            .cooldown(1000)
            .weight(6)
            .phase(2)
            .tags("ultimate aoe ranged")
            .anyTarget()
            .superArmor()
            .telegraph(0.5, 2.0)
            .when((b, t) -> this.stormTicks <= 0)
            .tick((b, level, target, t) -> {
               if (t == 10) {
                  Vec3 pos = this.position();
                  Vfx.telegraphCircle(level, pos, 26.0, 30, FROST);
                  Vfx.chargeUp(level, pos.add(0.0, 6.0, 0.0), FROST, 30);
                  Vfx.vortex(level, pos, 26.0, 12.0, FROST, 30);
               }
            })
            .hit((b, level, target, i) -> {
               Vec3 pos = this.position();
               List<LivingEntity> hit = BossFx.victims(level, this, pos, 26.0);
               BossFx.slam(level, this, pos, 26.0, 55.0F, 1.0);

               for (LivingEntity e : hit) {
                  frostbite(e, 100);
                  this.slow(e, 100, 1);
               }

               Vfx.nova(level, pos, FROST, 26.0);
               Vfx.radialFlash(level, pos, 40.0, 15268095, 0.7F);
               Vfx.debris(level, pos, Vfx.Debris.SNOW, FROST.core(), 30, 1.2);
               this.stormTicks = 300;
               Vfx.vortex(level, pos, 26.0, 14.0, FROST, 300);
            })
      );
   }

   private void scythe(ServerLevel level, int i) {
      Vec3 pos = this.position();
      Vec3 f = this.fwd();
      DamageSource src = this.damageSources().mobAttack(this);
      if (i == 0) {
         Vec3 chop = Vec3.directionFromRotation(0.0F, this.lockedYaw + 30.0F);

         for (LivingEntity e : this.alongLine(level, pos.add(chop.scale(1.5)), chop, 4.5, 1.8)) {
            e.hurtServer(level, src, this.dmg(32.0F));
            shove(e, pos, 1.0, -0.2);
            frostbite(e, 80);
         }

         Vfx.slashArc(level, pos.add(chop.scale(4.3)).add(0.0, 2.0, 0.0), this.lockedYaw + 30.0F, 60.0F, 90.0F, 2.0, FROST, 150.0F);

         for (double d : new double[]{3.0, 4.5, 6.0}) {
            Vfx.groundCrack(level, this.groundAt(level, pos.add(chop.scale(d))), 1.5, FROST);
         }

         Vfx.heavyImpact(level, this.groundAt(level, pos.add(chop.scale(4.5))), FROST, 1.0);
      } else {
         Vec3 left = Vec3.directionFromRotation(0.0F, this.lockedYaw - 90.0F);

         for (LivingEntity e : this.onFan(level, pos, this.lockedYaw + 45.0F, 45.0F, 5.8, 1.9)) {
            e.hurtServer(level, src, this.dmg(28.0F));
            e.push(left.x * 1.6, 0.3, left.z * 1.6);
            e.syncVelocity = true;
         }

         Vfx.slashArc(level, pos.add(0.0, 1.0, 0.0), this.lockedYaw + 45.0F, 0.0F, 0.0F, 5.8, FROST, 90.0F);
         Vfx.streaks(level, pos.add(f.scale(4.8)).add(0.0, 1.0, 0.0), left.scale(4.0), FROST, 12);
      }
   }

   private void crownMoves(List<BossAction> out) {
      out.add(
         BossAction.of("winters_crown", 2.2, 1.0, 1.3, 1.6)
            .range(4.0, 40.0)
            .cooldown(180)
            .weight(10)
            .phase(3)
            .tags("ranged anti_air")
            .telegraph(0.2, 1.0)
            .tick((b, level, target, t) -> {
               if (t == 4) {
                  Vfx.chargeUp(level, this.crown(), FROST, 16);

                  for (ServerPlayer p : this.engaged()) {
                     Vfx.runeCircle(level, p.position(), 1.2, FROST, 16, 0.08F, Vfx.Rune.GLYPH);
                  }
               }
            })
            .hit(
               (b, level, target, i) -> {
                  Vec3 from = this.crown();
                  double speed = i == 2 ? 1.2 : 0.9;
                  int n = 0;

                  for (ServerPlayer p : this.engaged()) {
                     if (n >= 4) {
                        break;
                     }

                     Vec3 dir = this.predict(p, from.distanceTo(p.position()) / 0.9).add(0.0, 1.0, 0.0).subtract(from);
                     if (!(dir.lengthSqr() < 1.0E-4)) {
                        BossBolt.shoot(level, this, BossBolt.Style.ICE, from, dir.normalize().scale(speed), this.dmg(14.0F))
                           .homing(p, 0.08)
                           .aoe(1.5)
                           .effect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
                        n++;
                     }
                  }

                  Vfx.glint(level, from, FROST.core(), 1.5);
                  Vfx.flare(level, from, FROST.glow(), 2.0, 5);
               }
            )
      );
      out.add(
         BossAction.of("absolute_zero", 5.0, 3.6)
            .range(0.0, 40.0)
            .cooldown(900)
            .weight(5)
            .phase(3)
            .tags("ultimate aoe")
            .anyTarget()
            .superArmor()
            .telegraph(0.4, 3.6)
            .when((b, t) -> this.desperate)
            .tick((b, level, target, t) -> {
               if (t == 8) {
                  Vec3 pos = this.position();
                  this.moveSpot = pos;
                  Vfx.telegraphCircle(level, pos, 26.0, 64, FROST);
                  Vfx.runeCircle(level, pos, 5.0, FROST, 64, 0.03F, Vfx.Rune.GLYPH);
                  Vfx.chargeUp(level, pos.add(0.0, 2.0, 0.0), FROST, 64);
                  Vfx.vortex(level, pos, 26.0, 8.0, FROST, 64);
               } else if (t >= 16 && t <= 68 && (t - 16) % 20 == 0 && this.moveSpot != null) {
                  DamageSource src = this.damageSources().freeze();

                  for (LivingEntity e : BossFx.victims(level, this, this.moveSpot, 5.0)) {
                     e.hurtServer(level, src, this.dmg(4.0F));
                     frostbite(e, 20);
                  }
               }
            })
            .hit((b, level, target, i) -> this.absoluteZero(level))
            .end((b, level, target, i) -> {
               if (i == -1) {
                  this.buff(60, 1.25F, 1.0F);
               }
            })
      );
   }

   private void absoluteZero(ServerLevel level) {
      Vec3 c = this.moveSpot != null ? this.moveSpot : this.position();
      DamageSource src = this.damageSources().mobAttack(this);

      for (LivingEntity e : BossFx.victims(level, this, c, 26.0)) {
         double dx = e.getX() - c.x;
         double dz = e.getZ() - c.z;
         if (!(dx * dx + dz * dz <= 25.0) || !(Math.abs(e.getY() - c.y) < 4.0)) {
            e.hurtServer(level, src, this.dmg(68.0F));
            e.setTicksFrozen(Math.max(e.getTicksFrozen(), e.getTicksRequiredToFreeze() + 160));
            this.slow(e, 80, 3);
         }
      }

      Vfx.nova(level, c, FROST, 26.0);
      Vfx.impactFrame(level, c, 40.0, 15268095, 1.0F);
      Vfx.heavyImpact(level, c, FROST, 3.0);
      Vfx.debris(level, c, Vfx.Debris.SNOW, FROST.core(), 30, 1.4);
   }

   private @Nullable LivingEntity hunted(@Nullable LivingEntity target) {
      if (this.burrowHunt != null) {
         for (ServerPlayer p : this.engaged()) {
            if (p.getUUID().equals(this.burrowHunt)) {
               return p;
            }
         }
      }

      return target;
   }

   @Override
   protected boolean ownCourse(ServerLevel level, @Nullable LivingEntity target) {
      if (this.burrowTicks <= 0) {
         return false;
      }

      BossFx.current(this);
      this.burrowTicks--;
      this.getNavigation().stop();
      this.setDeltaMovement(0.0, Math.min(0.0, this.getDeltaMovement().y), 0.0);
      if (this.burrowLock == null) {
         LivingEntity prey = this.hunted(target);
         boolean close = false;
         if (prey != null && this.burrowTicks > 16) {
            Vec3 want = this.predict(prey, 10.0);
            Vec3 d = new Vec3(want.x - this.getX(), 0.0, want.z - this.getZ());
            double len = d.length();
            if (len <= 1.5) {
               close = true;
            } else {
               double speed = this.phase() >= 3 ? 0.95 : 0.7 + 0.08 * this.phase();
               Vec3 step = d.scale(Math.min(speed, len - 1.0) / len);
               Vec3 at = BossFx.ground(level, this.getX() + step.x, this.getY(), this.getZ() + step.z);
               if (at != null
                  && Math.abs(at.y - this.getY()) <= 2.5
                  && BossFx.safeGround(level, at, 3.0)
                  && level.noCollision(this, this.getBoundingBox().move(at.subtract(this.position())))) {
                  this.setPos(at.x, at.y, at.z);
               }

               this.facePoint(want, 30.0F);
            }

            if (this.burrowTicks % 2 == 0) {
               Vec3 at = this.position();
               Vfx.groundCrack(level, at, 1.2, FROST);
               Vfx.shards(level, at.add(0.0, 0.2, 0.0), FROST.core(), 3, 0.3);
               BlockState below = level.getBlockState(this.blockPosition().below());
               if (!below.isAir()) {
                  blockBurst(level, at.add(0.0, 0.2, 0.0), below, 10, 0.6, 0.1);
               }
            }

            if (this.burrowTicks % 8 == 0) {
               this.voice(level, "step", 2.0F);
            }
         }

         if (this.burrowTicks <= 16 || close) {
            this.burrowTicks = Math.min(this.burrowTicks, 16);
            this.burrowLock = this.position();
            Vfx.telegraphCircle(level, this.burrowLock, 4.5, this.burrowTicks + 6, FROST);
            Vfx.groundCrack(level, this.burrowLock, 4.5, FROST);
         }
      }

      if (this.burrowTicks <= 0) {
         this.burrowLock = null;
         this.setInvisible(false);
         this.forceAction(level, "erupt");
      }

      return true;
   }

   private @Nullable ServerPlayer farMarked() {
      if (this.huntMark != null && this.tickCount < this.huntMarkUntil) {
         for (ServerPlayer p : this.engaged()) {
            if (p.getUUID().equals(this.huntMark) && p.distanceToSqr(this) > 256.0) {
               return p;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private void huntersMark() {
      List<ServerPlayer> players = this.engaged();
      if (this.phase() >= 1 && this.markCooldown <= 0 && players.size() >= 2) {
         float top = 0.0F;

         for (ServerPlayer p : players) {
            top = Math.max(top, this.threat().get(p));
         }

         for (ServerPlayer p : players) {
            if (!(p.distanceToSqr(this) <= 256.0) && (top > 0.5F && this.threat().get(p) >= top * 0.8F || aiming(p))) {
               this.markTarget(p, 120);
               this.huntMark = p.getUUID();
               this.huntMarkUntil = this.tickCount + 120;
               this.markCooldown = 600;
               return;
            }
         }
      }
   }

   @Override
   protected void customServerAiStep(ServerLevel world) {
      super.customServerAiStep(world);
      BossFx.current(this);
      this.tickIce(world);
      if (!this.prisonBlocks.isEmpty() && this.tickCount >= this.prisonRelease) {
         this.releasePrison(world);
      }

      if (!this.inIntro() && !this.isDeadOrDying()) {
         if (this.tickCount % 20 == 0) {
            this.minionCount = this.countMinions(world);
         }

         if (this.markCooldown > 0) {
            this.markCooldown--;
         }

         if (this.pendingZero && this.currentMove() == null && this.burrowTicks <= 0 && !this.leaping()) {
            this.pendingZero = false;
            this.forceAction(world, "absolute_zero");
            this.setCooldown("absolute_zero", 900);
         }

         this.tickStorm(world);
         if (this.getTarget() != null) {
            List<ServerPlayer> players = this.engaged();
            if (this.phase() >= 2 && this.stormTicks <= 0) {
               if (this.nextFallingIce <= 0) {
                  this.nextFallingIce = this.iceInterval();
               } else if (--this.nextFallingIce <= 0) {
                  for (ServerPlayer p : players) {
                     this.fallingIce(world, this.spotNear(world, this.predict(p, 20.0), 1.5), 22.0F, 24);
                     if (this.phase() >= 3) {
                        this.fallingIce(world, this.spotNear(world, this.predict(p, 34.0), 2.5), 22.0F, 24);
                     }
                  }

                  this.nextFallingIce = this.iceInterval();
               }
            }

            if (this.enraged()) {
               if (this.tickCount % 100 == 0) {
                  for (ServerPlayer p : players) {
                     this.fallingIce(world, this.spotNear(world, this.predict(p, 20.0), 1.5), 20.0F, 20);
                  }
               }

               if (this.tickCount % 200 == 0) {
                  Vfx.vortex(world, this.position(), 24.0, 12.0, FROST, 200);
               }
            }

            if (this.phase() >= 3 && this.tickCount % 10 == 0) {
               world.sendParticles(ParticleTypes.SNOWFLAKE, this.getX(), this.getY() + 9.0, this.getZ(), 24, 14.0, 2.0, 14.0, 0.01);
            }

            if (this.tickCount % 20 == 0) {
               this.huntersMark();
            }
         }
      }
   }

   private int iceInterval() {
      return this.phase() >= 3 ? 160 + this.random.nextInt(41) : 280 + this.random.nextInt(81);
   }

   private void tickStorm(ServerLevel level) {
      if (this.stormTicks > 0) {
         this.stormTicks--;
         List<ServerPlayer> players = this.engaged();
         if (!players.isEmpty()) {
            if (this.stormTicks % 8 == 0) {
               ServerPlayer p = players.get(this.random.nextInt(players.size()));
               this.fallingIce(level, this.spotNear(level, this.predict(p, 16.0), 3.0), 16.0F, 16);
            }

            if (this.stormTicks % 20 == 0) {
               for (ServerPlayer p : players) {
                  double dx = p.getX() - this.getX();
                  double dz = p.getZ() - this.getZ();
                  double d2 = dx * dx + dz * dz;
                  if (!(d2 > 900.0) && !(d2 < 0.01)) {
                     double d = Math.sqrt(d2);
                     frostbite(p, 30);
                     p.push(-dz / d * 0.25, 0.0, dx / d * 0.25);
                     p.syncVelocity = true;
                     Vfx.debris(level, p.position().add(0.0, 1.5, 0.0), Vfx.Debris.SNOW, FROST.core(), 6, 0.3);
                  }
               }
            }
         }
      }
   }

   @Override
   public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
      boolean bypass = source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
      if (this.burrowTicks > 0 && !bypass) {
         return false;
      }

      boolean thorns = !bypass && this.warded() && !source.is(DamageTypes.THORNS);
      boolean hurt = super.hurtServer(world, source, amount);
      if (hurt
         && thorns
         && source.getEntity() instanceof ServerPlayer p
         && source.getDirectEntity() == p
         && !p.isCreative()
         && p.distanceToSqr(this) <= 36.0) {
         p.hurtServer(world, this.damageSources().freeze(), this.dmg(4.0F));
         this.slow(p, 40, 0);
         Vfx.glint(world, p.position().add(0.0, p.getBbHeight() * 0.6, 0.0), FROST.core(), 1.0);
      }

      return hurt;
   }

   public boolean isPickable() {
      return super.isPickable() && !this.isInvisible();
   }

   public boolean isPushable() {
      return super.isPushable() && !this.isInvisible();
   }

   public boolean canBeHitByProjectile() {
      return super.canBeHitByProjectile() && !this.isInvisible();
   }

   protected void pushEntities() {
      if (!this.isInvisible()) {
         super.pushEntities();
      }
   }

   protected void updateInvisibilityStatus() {
      super.updateInvisibilityStatus();
      this.setInvisible(this.burrowTicks > 0);
   }

   @Override
   public void die(DamageSource damageSource) {
      this.stormTicks = 0;
      this.pendingZero = false;
      this.ice.clear();
      if (this.burrowTicks > 0) {
         this.surface();
      }

      super.die(damageSource);
      this.setNoGravity(false);
      if (this.level() instanceof ServerLevel level) {
         this.releasePrison(level);
      }
   }

   @Override
   protected void onPhaseChange(ServerLevel level, int phase) {
      BossFx.current(this);
      if (this.burrowTicks > 0) {
         this.surface();
      }

      this.setNoGravity(false);
      Vfx.telegraphCircle(level, this.position(), phase >= 3 ? 9.0 : (phase == 2 ? 8.0 : 7.0), 28, FROST);
      if (phase == 1) {
         this.summon(level, "frost_wisp", 2);
      } else if (phase == 2) {
         this.summon(level, "rime_stalker", 2);
         this.nextFallingIce = 280 + this.random.nextInt(81);
      } else {
         this.summon(level, "frost_wisp", 3);
         this.nextFallingIce = 160 + this.random.nextInt(41);
      }
   }

   @Override
   protected void phaseBurst(ServerLevel level, int phase) {
      BossFx.current(this);
      Vec3 pos = this.position();
      if (phase <= 1) {
         Vfx.explosion(level, pos.add(0.0, 3.0, 0.0), FROST, 4.0);
         Vfx.shards(level, pos.add(0.0, 3.0, 0.0), FROST.core(), 40, 1.0);
         Vfx.debris(level, pos, Vfx.Debris.SNOW, FROST.core(), 30, 1.0);
         BossFx.slam(level, this, pos, 7.0, 10.0F, 1.4);
         BossFx.knockback(level, this, pos, 9.0, 1.6);
         Vec3 left = Vec3.directionFromRotation(0.0F, this.getYRot() - 90.0F);
         Vfx.streaks(level, pos.add(left.scale(1.2)).add(0.0, 2.8, 0.0), left.scale(5.0), FROST, 12);
         Vfx.streaks(level, pos.subtract(left.scale(1.2)).add(0.0, 2.8, 0.0), left.scale(-5.0), FROST, 12);
         Vfx.nova(level, pos, FROST, 9.0);
      } else if (phase == 2) {
         Vec3 from = pos.add(0.0, 3.0, 0.0);

         for (int k = 0; k < 12; k++) {
            double a = k * Math.PI * 2.0 / 12.0;
            Vec3 v = new Vec3(Math.cos(a) * 0.6, 0.4, Math.sin(a) * 0.6);
            BossBolt.shoot(level, this, BossBolt.Style.ICE, from.add(v.x * 2.0, 0.0, v.z * 2.0), v, this.dmg(12.0F)).gravity(0.04).aoe(1.5);
         }

         BossFx.slam(level, this, pos, 8.0, 14.0F, 1.6);
         Vfx.nova(level, pos, FROST, 8.0);
         Vfx.groundCrack(level, pos, 9.0, FROST);
      } else {
         Vfx.impactFrame(level, pos, 40.0, 15268095, 0.8F);
         Vfx.nova(level, pos, FROST, 12.0);
         Vfx.pillar(level, pos, FROST, 14.0, 2.4);
         BossFx.slam(level, this, pos, 9.0, 16.0F, 2.0);
         this.iceRing(level, pos, 8, 10.0, 10, 18.0F);
      }
   }

   @Override
   protected void onStagger(ServerLevel level) {
      this.setNoGravity(false);
      if (this.burrowTicks > 0) {
         this.surface();
      }

      Vec3 at = this.front(level, 3.0);
      Vfx.heavyImpact(level, at, FROST, 0.8);
      Vfx.debris(level, at, Vfx.Debris.SNOW, FROST.core(), 16, 0.5);
      Vfx.shards(level, this.position().add(0.0, 2.0, 0.0), FROST.core(), 12, 0.5);
   }

   @Override
   protected void onEnrage(ServerLevel level) {
      BossFx.current(this);
      Vfx.vortex(level, this.position(), 24.0, 12.0, FROST, 200);
      this.summon(level, "rime_stalker", 2);
   }

   @Override
   protected void desperation(ServerLevel level) {
      this.desperate = true;
      Vfx.auraRing(level, this.position(), FROST, 6.0);
      BossAction a = this.currentMove();
      boolean held = a != null && (a.anim().startsWith("phase_") || a.anim().equals("stagger"));
      if (!held && !this.leaping() && this.burrowTicks <= 0) {
         this.forceAction(level, "absolute_zero");
         this.setCooldown("absolute_zero", 900);
      } else {
         this.pendingZero = true;
      }
   }

   @Override
   protected void onLand(ServerLevel level) {
      Vfx.groundCrack(level, this.position(), 3.0, FROST);
      Vfx.shards(level, this.position().add(0.0, 0.5, 0.0), FROST.core(), 10, 0.5);
   }

   @Override
   protected void onIntroTick(ServerLevel level, int left) {
      int e = this.introTicks() - left;
      Vec3 pos = this.position();
      if (e <= 20 && e % 4 == 0) {
         Vfx.groundCrack(level, pos, 2.0 + e * 0.15, FROST);
         Vfx.shards(level, pos.add(0.0, 0.3, 0.0), FROST.core(), 6, 0.4);
         Vfx.shake(level, pos, 24.0, 0.4, 6);
      }

      if (e == 20) {
         Vfx.explosion(level, pos.add(0.0, 2.0, 0.0), FROST, 3.0);
         Vfx.pillar(level, pos, FROST, 8.0, 1.6);
         Vfx.debris(level, pos, Vfx.Debris.SNOW, FROST.core(), 30, 0.8);
         blockBurst(level, pos.add(0.0, 0.5, 0.0), Blocks.PACKED_ICE.defaultBlockState(), 80, 1.5, 0.4);
      }

      if (e > 20 && e <= 64 && e % 3 == 0) {
         double a = e * 0.35;
         double r = 3.0 + (e - 20) / 44.0 * 2.0;
         Vec3 p = pos.add(Math.cos(a) * r, 0.4, Math.sin(a) * r);
         Vfx.shards(level, p, FROST.core(), 3, 0.3);
         Vfx.debris(level, p, Vfx.Debris.SNOW, FROST.core(), 3, 0.2);
      }

      if (e == 64) {
         Vfx.wisps(level, pos.add(0.0, 2.0, 0.0), FROST, 8, 4.0, 40);
      }

      if (e == 84) {
         Vfx.auraRing(level, pos, FROST, 6.0);
      }

      if (e == 100) {
         Vfx.nova(level, pos, FROST, 12.0);
         Vfx.radialFlash(level, pos, 40.0, 15268095, 0.6F);
         Vfx.shake(level, pos, 40.0, 1.4, 30);
         Vfx.debris(level, pos, Vfx.Debris.SNOW, FROST.core(), 30, 1.0);
      }
   }

   @Override
   protected void onDeathTick(ServerLevel level, int tick) {
      Vec3 pos = this.position();
      Vec3 f = this.fwd();
      Vec3 left = Vec3.directionFromRotation(0.0F, this.getYRot() - 90.0F);
      if (tick == 1) {
         this.releasePrison(level);
         this.stormTicks = 0;
         this.ice.clear();
         if (this.isInvisible()) {
            this.setInvisible(false);
         }
      }

      if (tick <= 28 && tick % 4 == 0) {
         Vec3 p = pos.subtract(f.scale(this.random.nextDouble() * 9.0)).add(0.0, 0.8, 0.0);
         Vfx.debris(level, p, Vfx.Debris.SNOW, FROST.core(), 6, 0.3);
         Vfx.shards(level, p, FROST.core(), 5, 0.4);
      }

      if (tick == 8 || tick == 18) {
         Vfx.groundCrack(level, this.groundAt(level, pos.subtract(f.scale(8.0))), 1.5, FROST);
      }

      if (tick == 34) {
         Vfx.heavyImpact(level, this.front(level, 3.0), FROST, 0.8);
      }

      if (tick == 56) {
         Vfx.nova(level, pos, FROST, 8.0);
         Vfx.shake(level, pos, 32.0, 0.9, 16);
      }

      if (tick >= 68 && tick <= 110 && (tick - 68) % 6 == 0) {
         Vec3 p = pos.add(f.scale(3.5 - this.random.nextDouble() * 12.5)).add(0.0, 0.6 + this.random.nextDouble() * 2.0, 0.0);
         Vfx.glint(level, p, FROST.core(), 1.0);
      }

      for (double[] s : SHATTER) {
         if ((int)s[0] == tick) {
            Vec3 p = pos.add(f.scale(s[1])).add(left.scale(s[2])).add(0.0, s[3], 0.0);
            Vfx.shards(level, p, FROST.core(), 10, 0.6);
            Vfx.debris(level, p, Vfx.Debris.SNOW, FROST.core(), 6, 0.4);
         }
      }

      if (tick == 112) {
         Vec3 head = pos.add(f.scale(0.8)).add(0.0, 6.2, 0.0);
         Vfx.explosion(level, head, FROST, 1.5);
         Vfx.shards(level, head, FROST.core(), 20, 0.7);
      }

      if (tick == 142) {
         Vec3 heart = pos.add(f.scale(-0.4)).add(0.0, 3.45, 0.0);
         Vfx.glint(level, heart, FROST.core(), 2.2);
         Vfx.flare(level, heart, FROST.glow(), 3.0, 8);
      }

      if (tick == 144) {
         Vec3 chest = pos.add(f.scale(-0.9)).add(0.0, 2.7, 0.0);
         Vfx.explosion(level, chest, FROST, 3.5);
         Vfx.shards(level, chest, FROST.core(), 50, 1.1);
         blockBurst(level, chest, Blocks.BLUE_ICE.defaultBlockState(), 160, 2.5, 0.4);
         Vfx.impactFrame(level, pos, 32.0, 15268095, 0.6F);
      }

      if (tick == 146) {
         Vfx.nova(level, pos, FROST, 10.0);
      }

      if (tick >= 146 && tick % 4 == 2) {
         Vfx.smoke(level, pos.add(0.0, 0.8, 0.0), FROST.smoke(), 6, 3.5, 2.0);
      }
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput view) {
      super.addAdditionalSaveData(view);
      view.putInt("HrimBurrow", this.burrowTicks);
      view.putInt("HrimStorm", this.stormTicks);
      view.putBoolean("HrimDesperate", this.desperate);
      if (!this.prisonBlocks.isEmpty()) {
         int[] a = new int[this.prisonBlocks.size() * 3];

         for (int k = 0; k < this.prisonBlocks.size(); k++) {
            BlockPos p = this.prisonBlocks.get(k);
            a[k * 3] = p.getX();
            a[k * 3 + 1] = p.getY();
            a[k * 3 + 2] = p.getZ();
         }

         view.putIntArray("HrimPrison", a);
      }
   }

   @Override
   protected void readAdditionalSaveData(ValueInput view) {
      super.readAdditionalSaveData(view);
      if (view.getIntOr("HrimBurrow", 0) > 0) {
         this.setInvisible(false);
      }

      this.burrowTicks = 0;
      this.burrowLock = null;
      this.stormTicks = Math.max(0, view.getIntOr("HrimStorm", 0));
      this.desperate = view.getBooleanOr("HrimDesperate", false);
      this.prisonBlocks.clear();
      view.getIntArray("HrimPrison").ifPresent(a -> {
         for (int k = 0; k + 2 < a.length; k += 3) {
            this.prisonBlocks.add(new BlockPos(a[k], a[k + 1], a[k + 2]));
         }
      });
      this.prisonRelease = 0;
   }

   private record Ice(Vec3 spot, float base, int hitAt) {
   }
}
