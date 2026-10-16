package com.vexira.mysticalterrain.boss.entity;

import com.vexira.mysticalterrain.boss.BossAction;
import com.vexira.mysticalterrain.boss.BossBolt;
import com.vexira.mysticalterrain.boss.BossFx;
import com.vexira.mysticalterrain.boss.BossSounds;
import com.vexira.mysticalterrain.boss.MysticalBoss;
import com.vexira.mysticalterrain.boss.RealmMob;
import com.vexira.mysticalterrain.boss.RealmMobs;
import com.vexira.mysticalterrain.boss.vfx.Vfx;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BlocksAttacksComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.DefaultAttributeContainer.Builder;
import net.minecraft.entity.boss.BossBar.Color;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jspecify.annotations.Nullable;
import software.bernie.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

public class GrothmawBoss extends MysticalBoss {
   private static final BossSounds SOUNDS = BossSounds.of("grothmaw");
   private static final Vfx.Palette PAL = Vfx.Palette.PRIMAL;
   private static final int BONE = 15260864;
   private static final float[] POISE = new float[]{0.16F, 0.14F, 0.12F, 0.1F};
   private static final double NOVA = 18.0;
   private static final float[] REAVER_CLUB = new float[]{149.0F, 171.0F, 216.0F, 293.0F, 429.0F, 466.0F, 485.0F};
   private static final double[][] MAW = new double[][]{
      {0.9, 6.2, 0.0, 2.0},
      {1.2, 5.0, 0.0, 4.2},
      {1.5, 4.4, 0.8, 6.0},
      {1.7, 4.4, -0.8, 6.0},
      {1.9, 4.6, 2.8, 0.6},
      {2.25, 4.4, 0.0, 5.0},
      {2.6, 4.6, -2.8, 0.6},
      {3.0, 3.0, 1.5, 5.6},
      {3.3, 6.0, 0.0, 4.6}
   };
   public static final float WIDTH = 3.2F;
   public static final float HEIGHT = 4.5F;
   public static final boolean FIRE_IMMUNE = false;
   private final List<GrothmawBoss.Egg> eggs = new ArrayList<>();
   private final Set<Integer> trampled = new HashSet<>();
   private final List<Vec3d> hollows = new ArrayList<>();
   private final List<Vec3d> lines = new ArrayList<>();
   private GrothmawBoss.@Nullable Arc arc;
   private @Nullable Vec3d spot;
   private Vec3d lineFrom = Vec3d.ZERO;
   private Vec3d lineDir = new Vec3d(0.0, 0.0, 1.0);
   private Vec3d lastPos = Vec3d.ZERO;
   private float chargeYaw;
   private float breakerYaw;
   private int charges;
   private int rockfallTimer = 200;
   private int instinctTimer = 40;
   private int cullCooldown;
   private int guardClearCooldown;
   private int blockingTicks;
   private boolean lastStand;
   private boolean lastStandPending;
   private @Nullable LivingEntity grabbed;
   private float gripDamage;
   private boolean gripBroken;

   public static Builder attributes() {
      return MysticalBoss.bossAttributes(1000.0, 20.0, 0.28, 12.0);
   }

   public GrothmawBoss(EntityType<? extends HostileEntity> type, World level) {
      super(type, level, "grothmaw", Color.GREEN);
   }

   @Override
   protected boolean v2() {
      return true;
   }

   @Override
   public float defense() {
      return 0.4F;
   }

   @Override
   public int bossLevel() {
      return 62;
   }

   @Override
   public float poiseMax() {
      return this.getMaxHealth() * POISE[Math.clamp(this.phase(), 0, 3)];
   }

   @Override
   public int enrageTicks() {
      return 7200;
   }

   @Override
   protected double runSpeed() {
      return 1.6 + (this.enraged() ? 0.15 : 0.0);
   }

   @Override
   protected String moveAnim() {
      return this.running() && this.phase() >= 2 ? "run_fast" : super.moveAnim();
   }

   @Override
   public double renderReach() {
      return 8.5;
   }

   @Override
   protected double preferredDistance() {
      return 4.5;
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
      return 140;
   }

   public float getPathfindingFavor(BlockPos pos, WorldView world) {
      return 0.0F;
   }

   private Vec3d fwd() {
      return Vec3d.fromPolar(0.0F, this.getYaw());
   }

   private Vec3d left() {
      return Vec3d.fromPolar(0.0F, this.getYaw() - 90.0F);
   }

   private Vec3d at(double f, double l, double u) {
      Vec3d a = this.fwd();
      Vec3d b = this.left();
      return this.getEntityPos().add(a.x * f + b.x * l, u, a.z * f + b.z * l);
   }

   private Vec3d snout(double d, double h) {
      return this.at(d, 0.0, h);
   }

   private Vec3d heart() {
      return this.at(1.7, 0.0, 3.2);
   }

   private static Vec3d mid(LivingEntity e) {
      return e.getEntityPos().add(0.0, e.getHeight() * 0.5, 0.0);
   }

   private Vec3d groundAt(ServerWorld level, Vec3d p) {
      Vec3d g = BossFx.ground(level, p.x, p.y - 3.5, p.z);
      return g != null ? g : p;
   }

   private Vec3d near(ServerWorld level, Vec3d c, double min, double max) {
      double a = this.random.nextDouble() * Math.PI * 2.0;
      double r = min + this.random.nextDouble() * (max - min);
      return this.groundAt(level, c.add(Math.cos(a) * r, 0.0, Math.sin(a) * r));
   }

   private static ParticleEffect dust(ServerWorld level, Vec3d at) {
      BlockState s = level.getBlockState(BlockPos.ofFloored(at.x, at.y - 0.5, at.z));
      return (ParticleEffect)(s.isAir() ? ParticleTypes.CLOUD : new BlockStateParticleEffect(ParticleTypes.BLOCK, s));
   }

   private void setFacing(float yaw) {
      this.setYaw(yaw);
      this.setBodyYaw(yaw);
      this.setHeadYaw(yaw);
   }

   private void aimAt(Vec3d p, float maxTurn) {
      double dx = p.x - this.getX();
      double dz = p.z - this.getZ();
      if (!(dx * dx + dz * dz < 0.25)) {
         float want = (float)(MathHelper.atan2(dz, dx) * 180.0F / (float)Math.PI) - 90.0F;
         this.setFacing(MathHelper.stepUnwrappedAngleTowards(this.getYaw(), want, maxTurn));
      }
   }

   private void snapAim(@Nullable LivingEntity target, double lead, float maxTurn) {
      if (target != null) {
         this.aimAt(this.predict(target, lead), maxTurn);
      }
   }

   private boolean aim(@Nullable LivingEntity target, int t, int n, double lead) {
      if (t > n) {
         return false;
      }

      if (target != null) {
         this.faceTarget(target, 10.0F);
         if (t == n) {
            this.snapAim(target, lead, 25.0F);
         }
      }

      return t == n;
   }

   private Vec3d landing(ServerWorld level, Vec3d want, double max, @Nullable LivingEntity target) {
      double dx = want.x - this.getX();
      double dz = want.z - this.getZ();
      double d = Math.sqrt(dx * dx + dz * dz);
      if (d > max) {
         want = new Vec3d(this.getX() + dx / d * max, want.y, this.getZ() + dz / d * max);
      }

      Vec3d g = BossFx.ground(level, want.x, want.y - 3.5, want.z);
      if (g != null && BossFx.safeGround(level, g, 3.0)) {
         for (int dy = 0; dy <= 2; dy++) {
            if (level.isSpaceEmpty(this, this.getBoundingBox().offset(g.x - this.getX(), g.y + dy - this.getY(), g.z - this.getZ()))) {
               return g;
            }
         }
      }

      if (target != null) {
         Vec3d t = target.getEntityPos();
         Vec3d tg = BossFx.ground(level, t.x, t.y, t.z);
         return tg != null ? tg : t;
      } else {
         return this.getEntityPos();
      }
   }

   private List<LivingEntity> targets(ServerWorld level, Box box) {
      return level.getEntitiesByClass(
         LivingEntity.class,
         box,
         e -> e != this && e.isAlive() && !(e instanceof MysticalBoss) && !(e instanceof PlayerEntity p && (p.isCreative() || p.isSpectator()))
      );
   }

   private List<LivingEntity> cone(ServerWorld level, Vec3d o, float yaw, float halfAngle, double range) {
      List<LivingEntity> out = new ArrayList<>();
      Vec3d dir = Vec3d.fromPolar(0.0F, yaw);

      for (LivingEntity e : this.targets(level, new Box(o.x - range - 1.0, o.y - 2.0, o.z - range - 1.0, o.x + range + 1.0, o.y + 5.0, o.z + range + 1.0))) {
         double dx = e.getX() - o.x;
         double dz = e.getZ() - o.z;
         double d = Math.sqrt(dx * dx + dz * dz);
         if (!(d > range + e.getWidth() * 0.5)
            && (!(d > 1.0) || !(Math.toDegrees(Math.acos(MathHelper.clamp((dx * dir.x + dz * dir.z) / d, -1.0, 1.0))) > halfAngle))) {
            out.add(e);
         }
      }

      return out;
   }

   private List<LivingEntity> inCircle(ServerWorld level, Vec3d c, double r) {
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : this.targets(level, new Box(c.x - r - 1.0, c.y - 2.0, c.z - r - 1.0, c.x + r + 1.0, c.y + 4.0, c.z + r + 1.0))) {
         double dx = e.getX() - c.x;
         double dz = e.getZ() - c.z;
         double rr = r + e.getWidth() * 0.5;
         if (dx * dx + dz * dz <= rr * rr) {
            out.add(e);
         }
      }

      return out;
   }

   private static void shove(LivingEntity e, Vec3d from, double push, double up) {
      if (!(push <= 0.0) || !(up <= 0.0)) {
         Vec3d away = new Vec3d(e.getX() - from.x, 0.0, e.getZ() - from.z);
         away = away.lengthSquared() < 1.0E-4 ? Vec3d.ZERO : away.normalize();
         e.addVelocity(away.x * push, up, away.z * push);
         e.knockedBack = true;
      }
   }

   private List<LivingEntity> hurtAll(
      ServerWorld level, List<LivingEntity> victims, Vec3d from, float base, double push, double up, @Nullable StatusEffectInstance effect, float guard
   ) {
      List<LivingEntity> hit = new ArrayList<>();
      DamageSource source = this.getDamageSources().mobAttack(this);
      float amount = this.dmg(base);

      for (LivingEntity e : victims) {
         if (guard > 0.0F) {
            this.breakGuard(level, e, guard);
         }

         if (e.damage(level, source, amount)) {
            hit.add(e);
            shove(e, from, push, up);
            if (effect != null) {
               e.addStatusEffect(new StatusEffectInstance(effect), this);
            }
         }
      }

      return hit;
   }

   private List<LivingEntity> coneHit(
      ServerWorld level, Vec3d o, float yaw, float halfAngle, double range, float base, double push, double up, @Nullable StatusEffectInstance effect
   ) {
      return this.hurtAll(level, this.cone(level, o, yaw, halfAngle, range), o, base, push, up, effect, 0.0F);
   }

   private List<LivingEntity> circleHit(ServerWorld level, Vec3d c, double r, float base, double push, double up, @Nullable StatusEffectInstance effect) {
      return this.hurtAll(level, this.inCircle(level, c, r), c, base, push, up, effect, 0.0F);
   }

   private List<LivingEntity> lineHit(ServerWorld level, Vec3d from, Vec3d dir, double length, double width, float base, double up) {
      return this.hurtAll(level, this.strip(level, from, dir, length, width), from, base, 0.0, up, null, 0.0F);
   }

   private List<LivingEntity> strip(ServerWorld level, Vec3d from, Vec3d dir, double length, double width) {
      Vec3d d = new Vec3d(dir.x, 0.0, dir.z).normalize();
      Vec3d end = from.add(d.multiply(length));
      double half = width * 0.5 + 0.4;
      Box box = new Box(
         Math.min(from.x, end.x) - half,
         from.y - 3.0,
         Math.min(from.z, end.z) - half,
         Math.max(from.x, end.x) + half,
         from.y + 4.0,
         Math.max(from.z, end.z) + half
      );
      List<LivingEntity> in = new ArrayList<>();

      for (LivingEntity e : this.targets(level, box)) {
         double rx = e.getX() - from.x;
         double rz = e.getZ() - from.z;
         double s = rx * d.x + rz * d.z;
         if (s >= -0.5 && s <= length + 0.5 && Math.abs(rx * d.z - rz * d.x) <= half) {
            in.add(e);
         }
      }

      return in;
   }

   private void breakGuard(ServerWorld level, LivingEntity e, float seconds) {
      ItemStack stack = e.getBlockingItem();
      if (stack != null) {
         BlocksAttacksComponent blocks = (BlocksAttacksComponent)stack.get(DataComponentTypes.BLOCKS_ATTACKS);
         if (blocks != null) {
            blocks.applyShieldCooldown(level, e, seconds, stack);
            Vfx.shards(level, e.getEyePos(), 16777215, 10, 0.4);
         }
      }
   }

   private void tickArc(int t) {
      GrothmawBoss.Arc a = this.arc;
      if (a != null && t > a.start()) {
         if (t >= a.start() + a.len()) {
            this.endArc();
         } else {
            double s = Math.min(1.0, (double)(t - a.start()) / (a.len() - 1));
            this.setNoGravity(true);
            Vec3d v = a.point(s).subtract(this.getEntityPos());
            if (v.lengthSquared() > 9.0) {
               v = v.normalize().multiply(3.0);
            }

            this.setVelocity(v);
         }
      }
   }

   private void endArc() {
      GrothmawBoss.Arc a = this.arc;
      this.arc = null;
      this.setNoGravity(false);
      if (a != null) {
         Vec3d d = a.to().subtract(this.getEntityPos());
         if (d.lengthSquared() < 2.25 && this.getEntityWorld().isSpaceEmpty(this, this.getBoundingBox().offset(d))) {
            this.setPosition(a.to().x, a.to().y, a.to().z);
         }

         this.setVelocity(0.0, Math.min(0.0, this.getVelocity().y), 0.0);
         if (!this.getEntityWorld().isSpaceEmpty(this, this.getBoundingBox())) {
            for (int dy = 1; dy <= 4; dy++) {
               if (this.getEntityWorld().isSpaceEmpty(this, this.getBoundingBox().offset(0.0, dy, 0.0))) {
                  this.setPosition(this.getX(), this.getY() + dy, this.getZ());
                  break;
               }
            }
         }
      }
   }

   @Override
   protected void defineActions(List<BossAction> out) {
      out.add(
         BossAction.of("rending_bite", 1.1, 0.5)
            .range(0.0, 7.0)
            .cooldown(40)
            .weight(14)
            .tags("melee")
            .lockFacing()
            .combo("crushing_jaws", 0.35F)
            .combo("feint_snap", 0.2F)
            .advance(0.3, 0.5, 0.35)
            .telegraph(0.1, 0.5)
            .utility((b, t, ctx) -> ctx.dist < 5.0 ? 1.3F : 1.0F)
            .tick((b, level, target, t) -> {
               if (this.aim(target, t, 2, 5.0)) {
                  Vfx.telegraphCone(level, this.getEntityPos(), this.getYaw(), 40.0F, 8.4, 8, PAL);
               }
            })
            .hit((b, level, target, i) -> this.jaws(level, this.coneHit(level, this.getEntityPos(), this.getYaw(), 40.0F, 7.0, 18.0F, 1.0, 0.3, null), 0.8))
      );
      out.add(
         BossAction.of("feint_snap", 1.6, 0.95)
            .range(0.0, 8.0)
            .cooldown(90)
            .phase(1)
            .weight(8)
            .tags("melee")
            .lockFacing()
            .combo("tail_reaver", 0.2F)
            .advance(0.75, 0.95, 0.45)
            .telegraph(0.35, 0.95)
            .utility((b, t, ctx) -> ctx.ticksSinceHurt < 40 ? 1.5F : 1.0F)
            .tick((b, level, target, t) -> {
               if (this.aim(target, t, 7, 7.0)) {
                  Vfx.telegraphCone(level, this.getEntityPos(), this.getYaw(), 45.0F, 9.8, 12, PAL);
               }

               if (t >= 6 && t <= 14 && (t - 6) % 4 == 0) {
                  Vfx.flare(level, this.snout(4.4, 6.3), PAL.glow(), 1.2, 5);
               }
            })
            .hit((b, level, target, i) -> this.jaws(level, this.coneHit(level, this.getEntityPos(), this.getYaw(), 45.0F, 8.0, 22.0F, 1.2, 0.35, null), 0.9))
      );
      out.add(
         BossAction.of("crushing_jaws", 1.5, 0.55, 1.05)
            .range(0.0, 7.0)
            .cooldown(70)
            .weight(11)
            .tags("melee")
            .lockFacing()
            .combo("tail_reaver", 0.3F)
            .advance(0.35, 0.55, 0.3)
            .telegraph(0.05, 1.05)
            .tick((b, level, target, t) -> {
               if (this.aim(target, t, 1, 6.0)) {
                  Vfx.telegraphCone(level, this.getEntityPos(), this.getYaw(), 45.0F, 8.2, 10, PAL);
               }

               if (t == 12) {
                  Vfx.telegraphCone(level, this.getEntityPos(), this.getYaw(), 30.0F, 7.5, 9, PAL);
               }
            })
            .hit((b, level, target, i) -> {
               if (i == 0) {
                  this.coneHit(level, this.getEntityPos(), this.getYaw(), 45.0F, 7.0, 16.0F, 0.0, 0.0, new StatusEffectInstance(StatusEffects.SLOWNESS, 20, 1));
                  Vfx.impact(level, this.snout(6.0, 2.0), PAL, 0.7);
               } else {
                  this.coneHit(level, this.getEntityPos(), this.getYaw(), 30.0F, 7.5, 20.0F, 0.4, 0.9, new StatusEffectInstance(StatusEffects.WITHER, 40, 0));
                  Vfx.slashArc(level, this.snout(4.0, 3.0), this.getYaw(), -20.0F, -90.0F, 3.5, PAL, 140.0F);
                  Vfx.debris(level, this.snout(5.0, 3.0), Vfx.Debris.BONE, PAL.core(), 8, 0.3);
               }
            })
      );
      out.add(
         BossAction.of("skull_breaker", 1.7, 0.85)
            .range(0.0, 8.0)
            .cooldown(110)
            .weight(10)
            .tags("melee guard_break")
            .superArmor()
            .lockFacing()
            .combo("heel_stomp", 0.35F)
            .advance(0.6, 0.85, 0.2)
            .telegraph(0.0, 0.85)
            .utility((b, t, ctx) -> ctx.targetBlocking ? 1.0F : 0.8F)
            .start((b, level, target, i) -> {
               this.snapAim(target, 10.0, 25.0F);
               this.spot = this.groundAt(level, this.at(7.0, 0.0, 0.0));
               Vfx.telegraphCircle(level, this.spot, 3.2, 17, PAL);
            })
            .hit((b, level, target, i) -> {
               Vec3d p = this.spot != null ? this.spot : this.groundAt(level, this.at(7.0, 0.0, 0.0));
               this.hurtAll(level, this.inCircle(level, p, 3.2), p, 30.0F, 1.4, 0.6, null, 5.0F);
               Vfx.heavyImpact(level, p, PAL, 1.2);
               Vfx.groundCrack(level, p, 3.5, PAL);
            })
      );
      out.add(
         BossAction.of("sky_bite", 1.5, 0.7)
            .range(0.0, 12.0)
            .cooldown(90)
            .weight(6)
            .tags("melee anti_air")
            .lockFacing()
            .telegraph(0.0, 0.7)
            .utility((b, t, ctx) -> !ctx.targetFlying && !ctx.targetAirborne && !(ctx.heightDiff > 2.0) ? 0.0F : 1.0F)
            .start((b, level, target, i) -> {
               Vfx.chargeUp(level, this.snout(3.0, 5.0), PAL, 14);
               this.spot = this.groundAt(level, target != null ? this.predict(target, 14.0) : this.at(4.0, 0.0, 0.0));
               Vfx.telegraphCircle(level, this.spot, 3.0, 14, PAL);
            })
            .tick((b, level, target, t) -> {
               this.aim(target, t, 8, 4.0);
               if (t == 20) {
                  BossFx.knockback(level, this, this.getEntityPos(), 4.0, 1.0);
                  Vfx.shockwave(level, this.getEntityPos(), PAL, 4.0);
               }
            })
            .hit((b, level, target, i) -> this.skyBite(level))
      );
      out.add(
         BossAction.of("tail_reaver", 1.8, 0.9)
            .range(0.0, 8.5)
            .cooldown(90)
            .weight(10)
            .tags("melee aoe")
            .lockFacing()
            .telegraph(0.1, 0.9)
            .utility((b, t, ctx) -> ctx.targetBehind ? 1.5F : 1.0F)
            .tick((b, level, target, t) -> {
               if (t == 2) {
                  Vfx.telegraphCircle(level, this.groundAt(level, this.getEntityPos()), 8.5, 16, PAL);
               }

               if (t >= 8 && t <= 20 && t % 2 == 0) {
                  Vec3d club = this.getEntityPos().add(Vec3d.fromPolar(0.0F, this.getYaw() + REAVER_CLUB[(t - 8) / 2]).multiply(7.0)).add(0.0, 2.0, 0.0);
                  Vfx.trail(level, club, PAL, 1.2);
                  level.spawnParticles(dust(level, club.add(0.0, -2.0, 0.0)), club.x, club.y - 1.8, club.z, 3, 0.4, 0.1, 0.4, 0.05);
               }

               if (t == 22 && this.phase() >= 2) {
                  BossFx.wave(level, this, this.groundAt(level, this.getEntityPos()), 9.0, 10, 14.0F, ParticleTypes.FLAME, 1.0, true);
               }
            })
            .hit((b, level, target, i) -> {
               Vec3d c = this.getEntityPos();

               for (LivingEntity e : this.circleHit(level, c, 8.5, 28.0F, 2.2, 0.5, null)) {
                  Vfx.heavyImpact(level, mid(e), PAL, 0.6);
               }

               Vfx.slashArc(level, c.add(0.0, 1.4, 0.0), this.getYaw(), 0.0F, 0.0F, 8.5, PAL, -330.0F);
               if (this.phase() >= 2) {
                  Vfx.groundCrack(level, c, 9.0, PAL);
               }
            })
      );
      out.add(
         BossAction.of("club_backswing", 1.0, 0.4)
            .range(0.0, 9.0)
            .cooldown(50)
            .weight(10)
            .tags("melee behind")
            .lockFacing()
            .combo("tail_reaver", 0.3F)
            .telegraph(0.05, 0.4)
            .tick((b, level, target, t) -> {
               if (t == 1) {
                  Vfx.telegraphCone(level, this.getEntityPos(), this.getYaw() + 180.0F, 75.0F, 9.0, 7, PAL);
               }
            })
            .hit((b, level, target, i) -> {
               this.coneHit(level, this.getEntityPos(), this.getYaw() + 180.0F, 75.0F, 9.0, 24.0F, 2.0, 0.4, null);
               Vfx.slashArc(level, this.getEntityPos().add(0.0, 1.2, 0.0), this.getYaw() + 180.0F, 0.0F, 0.0F, 8.5, PAL, 160.0F);
            })
      );
      out.add(
         BossAction.of("heel_stomp", 1.2, 0.6)
            .range(0.0, 6.0)
            .cooldown(60)
            .weight(10)
            .tags("melee aoe")
            .lockFacing()
            .combo("tectonic_stomp", 0.3F)
            .telegraph(0.1, 0.6)
            .utility((b, t, ctx) -> ctx.dist < 3.5 ? 2.5F : 1.0F)
            .tick((b, level, target, t) -> {
               if (this.aim(target, t, 2, 6.0)) {
                  this.spot = this.groundAt(level, this.at(2.5, 1.2, 0.0));
                  Vfx.telegraphCircle(level, this.spot, 3.8, 10, PAL);
               }
            })
            .hit((b, level, target, i) -> {
               Vec3d f = this.spot != null ? this.spot : this.groundAt(level, this.at(2.5, 1.2, 0.0));
               BossFx.slam(level, this, f, 3.8, 20.0F, 1.0);
               BossFx.effect(level, this, f, 3.8, new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 1));
               Vfx.heavyImpact(level, f, PAL, 0.9);
               Vfx.groundCrack(level, f, 3.5, PAL);
            })
      );
      out.add(
         BossAction.of("tectonic_stomp", 2.2, 0.8, 1.5)
            .range(0.0, 12.0)
            .cooldown(140)
            .weight(9)
            .tags("aoe")
            .superArmor()
            .telegraph(0.0, 1.5)
            .utility((b, t, ctx) -> this.engaged().size() >= 2 ? 1.3F : 1.0F)
            .start((b, level, target, i) -> Vfx.telegraphCircle(level, this.groundAt(level, this.getEntityPos()), 4.5, 16, PAL))
            .tick((b, level, target, t) -> {
               if (t == 18) {
                  Vfx.telegraphCircle(level, this.groundAt(level, this.getEntityPos()), 5.0, 12, PAL);
               }
            })
            .hit((b, level, target, i) -> this.tectonic(level, i))
      );
      out.add(
         BossAction.of("bone_quake", 2.0, 1.0)
            .range(4.0, 20.0)
            .cooldown(120)
            .weight(10)
            .tags("ranged aoe")
            .lockFacing()
            .telegraph(0.3, 1.0)
            .utility((b, t, ctx) -> ctx.dist > 7.0 ? 1.5F : 0.6F)
            .tick((b, level, target, t) -> {
               if (this.aim(target, t, 6, 14.0)) {
                  this.lines.clear();
                  this.lineFrom = this.groundAt(level, this.at(2.5, 0.0, 0.0));
                  int n = this.phase() >= 2 ? 3 : 1;

                  for (int k = 0; k < n; k++) {
                     Vec3d dir = Vec3d.fromPolar(0.0F, this.getYaw() + (k == 0 ? 0 : (k == 1 ? 22 : -22)));
                     this.lines.add(dir);
                     Vfx.telegraphLine(level, this.lineFrom, dir, 20.0, 2.6, 14, PAL);
                  }
               }
            })
            .hit((b, level, target, i) -> this.boneQuake(level))
      );
      out.add(
         BossAction.of("stampede", 2.6, 0.8)
            .range(7.0, 26.0)
            .cooldown(160)
            .weight(11)
            .tags("gap_closer")
            .superArmor()
            .lockFacing()
            .combo("stampede_turn", 0.35F)
            .combo("tail_reaver", 0.2F)
            .advance(0.8, 1.9, 0.95)
            .telegraph(0.05, 0.8)
            .utility((b, t, ctx) -> ctx.targetUnreachable ? 0.2F : (ctx.kitingTicks > 40 ? 1.5F : 1.0F))
            .start((b, level, target, i) -> {
               this.snapAim(target, 16.0, 60.0F);
               this.trampled.clear();
               this.charges = 0;
            })
            .tick((b, level, target, t) -> {
               if (t == 1) {
                  Vfx.telegraphLine(level, this.groundAt(level, this.getEntityPos()), this.fwd(), 25.0, 3.6, 15, PAL);
               }

               if (t == 4 || t == 9) {
                  this.paw(level);
               }

               this.charge(level, t, 17, 38, 18, 32.0F);
            })
            .hit((b, level, target, i) -> this.launch(level))
      );
      out.add(
         BossAction.of("stampede_turn", 1.8, 0.6)
            .range(4.0, 24.0)
            .cooldown(200)
            .phase(2)
            .weight(0)
            .tags("gap_closer")
            .superArmor()
            .lockFacing()
            .combo("stampede_turn", 0.1F)
            .combo("tail_reaver", 0.25F)
            .advance(0.6, 1.5, 1.0)
            .telegraph(0.1, 0.6)
            .when((b, t) -> this.charges < (this.phase() >= 3 ? 2 : 1))
            .start((b, level, target, i) -> {
               this.charges++;
               this.trampled.clear();
               this.chargeYaw = this.getYaw();
            })
            .tick((b, level, target, t) -> {
               if (t == 2) {
                  Vec3d d = target != null ? this.predict(target, 10.0).subtract(this.getEntityPos()) : this.fwd();
                  Vec3d dir = new Vec3d(d.x, 0.0, d.z);
                  dir = dir.lengthSquared() < 1.0E-4 ? this.fwd() : dir.normalize();
                  this.chargeYaw = (float)(MathHelper.atan2(dir.z, dir.x) * 180.0F / (float)Math.PI) - 90.0F;
                  Vfx.telegraphLine(level, this.groundAt(level, this.getEntityPos()), dir, 22.5, 3.6, 10, PAL);
               }

               if (t >= 2 && t <= 10) {
                  this.setFacing(MathHelper.stepUnwrappedAngleTowards(this.getYaw(), this.chargeYaw, 25.0F));
               } else if (t == 11) {
                  this.setFacing(this.chargeYaw);
               }

               this.charge(level, t, 13, 30, 14, 28.0F);
            })
            .hit((b, level, target, i) -> this.launch(level))
      );
      out.add(
         BossAction.of("shoulder_check", 1.2, 0.55)
            .range(3.5, 11.0)
            .cooldown(80)
            .weight(9)
            .tags("gap_closer punish melee")
            .lockFacing()
            .combo("rending_bite", 0.3F)
            .combo("feint_snap", 0.2F)
            .advance(0.25, 0.55, 0.55)
            .telegraph(0.05, 0.55)
            .utility((b, t, ctx) -> ctx.dist >= 4.0 && ctx.dist <= 9.0 ? 1.3F : 1.0F)
            .tick((b, level, target, t) -> {
               if (this.aim(target, t, 1, 8.0)) {
                  this.lineFrom = this.groundAt(level, this.getEntityPos());
                  this.lineDir = this.fwd();
                  Vfx.telegraphLine(level, this.lineFrom, this.lineDir, 9.0, 3.4, 10, PAL);
               }

               if (t >= 5 && t <= 11) {
                  Vfx.streaks(level, this.getEntityPos().add(0.0, 2.0, 0.0), this.fwd(), PAL, 4);
                  if (t % 2 == 1) {
                     level.spawnParticles(dust(level, this.getEntityPos()), this.getX(), this.getY() + 0.2, this.getZ(), 8, 1.0, 0.1, 1.0, 0.1);
                  }
               }
            })
            .hit((b, level, target, i) -> {
               this.hurtAll(level, this.strip(level, this.lineFrom, this.lineDir, 9.0, 3.4), this.lineFrom, 22.0F, 2.6, 0.6, null, 0.0F);
               Vfx.heavyImpact(level, this.at(3.5, 0.0, 0.0), PAL, 0.7);
            })
      );
      out.add(
         BossAction.of("crater_leap", 2.6, 1.8)
            .range(8.0, 24.0)
            .cooldown(180)
            .weight(10)
            .tags("gap_closer aoe")
            .superArmor()
            .lockFacing()
            .telegraph(0.6, 1.8)
            .utility((b, t, ctx) -> ctx.targetUnreachable ? 0.3F : (ctx.dist > 12.0 ? 1.5F : 1.0F))
            .tick((b, level, target, t) -> {
               if (t < 12 && target != null) {
                  this.faceTarget(target, 10.0F);
               }

               if (t == 4) {
                  Vfx.chargeUp(level, this.getEntityPos().add(0.0, 1.0, 0.0), PAL, 8);
               }

               if (t == 12) {
                  Vec3d l = this.landing(level, target != null ? this.predict(target, 24.0) : this.at(12.0, 0.0, 0.0), 22.0, target);
                  this.aimAt(l, 60.0F);
                  this.spot = l;
                  this.arc = new GrothmawBoss.Arc(this.getEntityPos(), l, 12, 24, 6.0);
                  Vfx.telegraphCircle(level, l, 7.0, 24, PAL);
                  Vfx.shockwave(level, this.getEntityPos(), PAL, 4.0);
                  level.spawnParticles(dust(level, this.getEntityPos()), this.getX(), this.getY() + 0.2, this.getZ(), 30, 1.5, 0.2, 1.5, 0.2);
               }

               if (t > 12) {
                  this.tickArc(t);
                  if (t < 36 && t % 2 == 0) {
                     Vfx.trail(level, this.getEntityPos().add(0.0, 2.5, 0.0), PAL, 1.6);
                  }
               }
            })
            .hit((b, level, target, i) -> {
               Vec3d l = this.spot != null ? this.spot : this.getEntityPos();
               BossFx.slam(level, this, l, 7.0, 38.0F, 1.8);
               Vfx.heavyImpact(level, l, PAL, 1.6);
               Vfx.groundCrack(level, l, 6.0, PAL);
               if (this.phase() >= 2) {
                  BossFx.wave(level, this, l, 12.0, 14, 16.0F, dust(level, l), 1.0, true);
               }
            })
            .end((b, level, target, i) -> this.endArc())
      );
      out.add(
         BossAction.of("tail_vault", 1.2, 0.45)
            .range(0.0, 7.0)
            .cooldown(180)
            .phase(1)
            .weight(6)
            .tags("mobility aoe")
            .combo("boulder_hurl", 0.4F)
            .combo("stampede", 0.35F)
            .utility((b, t, ctx) -> ctx.surroundCount >= 2 ? 3.0F : (ctx.targetBehind && ctx.dist < 4.0 ? 1.5F : 0.5F))
            .start((b, level, target, i) -> Vfx.telegraphCircle(level, this.groundAt(level, this.getEntityPos()), 3.5, 9, PAL))
            .hit((b, level, target, i) -> {
               BossFx.slam(level, this, this.getEntityPos(), 3.5, 10.0F, 1.4);
               this.vault(level);
            })
      );
      out.add(
         BossAction.of("boulder_hurl", 2.0, 1.1)
            .range(6.0, 30.0)
            .cooldown(110)
            .weight(10)
            .tags("ranged")
            .telegraph(0.4, 1.1)
            .utility((b, t, ctx) -> (ctx.targetUnreachable ? 3.0F : 1.0F) * (ctx.targetFlying ? 1.5F : 1.0F))
            .tick((b, level, target, t) -> {
               if (t == 7) {
                  Vec3d dig = this.snout(5.0, 0.2);
                  Vfx.impact(level, dig, PAL, 0.9);
                  Vfx.shards(level, dig, PAL.smoke(), 14, 0.5);
                  level.spawnParticles(dust(level, dig), dig.x, dig.y + 0.2, dig.z, 16, 0.6, 0.2, 0.6, 0.15);
               }

               if (t >= 8 && t <= 20 && (t - 8) % 3 == 0) {
                  Vfx.gather(level, this.snout(4.0, 3.5), PAL, 2.5, 8);
               }
            })
            .hit((b, level, target, i) -> this.hurl(level, target))
      );
      out.add(
         BossAction.of("amber_spit", 1.0, 0.45)
            .range(4.0, 26.0)
            .cooldown(70)
            .phase(1)
            .weight(9)
            .tags("ranged punish")
            .telegraph(0.0, 0.45)
            .start((b, level, target, i) -> Vfx.chargeUp(level, this.snout(5.6, 3.1), PAL, 9))
            .hit((b, level, target, i) -> this.spit(level, target))
      );
      out.add(
         BossAction.of("terror_roar", 2.4, 1.0)
            .range(0.0, 16.0)
            .cooldown(260)
            .phase(1)
            .weight(7)
            .anyTarget()
            .tags("aoe")
            .superArmor()
            .lockFacing()
            .telegraph(0.1, 1.0)
            .utility((b, t, ctx) -> (this.engaged().size() >= 2 ? 1.5F : 1.0F) * (ctx.targetBlocking ? 1.5F : 1.0F))
            .tick((b, level, target, t) -> {
               if (this.aim(target, t, 2, 10.0)) {
                  Vfx.telegraphCone(level, this.getEntityPos(), this.getYaw(), 70.0F, 16.0, 18, PAL);
               }

               if (t >= 4 && t <= 18 && (t - 4) % 3 == 0) {
                  Vfx.gather(level, this.snout(4.0, 4.0), PAL, 3.0, 8);
               }
            })
            .hit((b, level, target, i) -> this.roar(level))
      );
      out.add(
         BossAction.of("bone_bulwark", 2.2, 0.5, 1.6)
            .range(0.0, 40.0)
            .cooldown(480)
            .weight(6)
            .anyTarget()
            .tags("defensive aoe")
            .superArmor()
            .telegraph(0.5, 1.6)
            .utility((b, t, ctx) -> this.shield() > 0.0F ? 0.0F : 1.0F)
            .tick((b, level, target, t) -> {
               if (t == 10) {
                  Vfx.telegraphCircle(level, this.groundAt(level, this.getEntityPos()), 5.5, 22, PAL);
               }
            })
            .hit((b, level, target, i) -> {
               Vec3d c = this.getEntityPos();
               if (i == 0) {
                  this.shield(this.getMaxHealth() * (this.phase() >= 2 ? 0.1F : 0.12F), 200);
                  Vfx.shieldBubble(level, c.add(0.0, 2.2, 0.0), 3.4, PAL, 200);
               } else {
                  BossFx.slam(level, this, c, 5.5, 18.0F, 1.8);
                  Vfx.debris(level, c.add(0.0, 3.0, 0.0), Vfx.Debris.BONE, 15260864, 24, 0.6);
                  Vfx.shards(level, c.add(0.0, 3.0, 0.0), PAL.core(), 20, 0.7);
                  Vfx.nova(level, c, PAL, 5.5);
               }
            })
      );
      out.add(
         BossAction.of("raptor_brood", 2.2, 1.2)
            .range(0.0, 30.0)
            .cooldown(600)
            .phase(1)
            .weight(7)
            .anyTarget()
            .tags("summon")
            .utility((b, t, ctx) -> this.minions(this.getEntityWorld()) >= 4 ? 0.0F : 1.0F)
            .tick((b, level, target, t) -> {
               if (t == 18 || t == 21) {
                  Vfx.flare(level, this.snout(5.0, 5.9), PAL.glow(), 1.0, 5);
                  Vfx.sparks(level, this.snout(5.0, 5.9), PAL.core(), 6, 0.25, 0.15);
               }
            })
            .hit((b, level, target, i) -> this.brood(level))
      );
      out.add(
         BossAction.of("wall_crash", 2.5, 0.15)
            .range(0.0, 64.0)
            .cooldown(0)
            .weight(0)
            .anyTarget()
            .tags("aoe")
            .lockFacing()
            .when((b, t) -> false)
            .start((b, level, target, i) -> {
               this.buff(50, 1.5F, 1.0F);
               Vfx.explosion(level, this.snout(3.0, 2.5), PAL, 3.0);
               Vfx.shake(level, this.getEntityPos(), 24.0, 1.0, 12);
               Vec3d back = this.fwd().multiply(-0.4);
               this.setVelocity(back.x, this.getVelocity().y, back.z);
            })
            .tick((b, level, target, t) -> {
               if (t >= 12 && t <= 40 && (t - 12) % 8 == 0) {
                  Vfx.wisps(level, this.at(3.5, 0.0, 4.5), PAL, 4, 1.2, 10);
               }
            })
            .hit((b, level, target, i) -> {
               Vec3d p = this.at(3.0, 0.0, 0.0);
               BossFx.knockback(level, this, p, 3.0, 1.0);
               Vfx.debris(level, p.add(0.0, 2.5, 0.0), Vfx.Debris.BONE, 15260864, 12, 0.5);
               level.spawnParticles(dust(level, p), p.x, p.y + 0.3, p.z, 20, 1.2, 0.3, 1.2, 0.15);
            })
      );
      out.add(
         BossAction.of("continental_breaker", 4.5, 2.0, 2.6, 3.2)
            .range(0.0, 26.0)
            .cooldown(900)
            .phase(2)
            .weight(8)
            .anyTarget()
            .tags("aoe ultimate")
            .superArmor()
            .lockFacing()
            .telegraph(0.1, 3.2)
            .utility((b, t, ctx) -> ctx.surroundCount >= 2 ? 1.5F : 1.0F)
            .start((b, level, target, i) -> {
               Vfx.chargeUp(level, this.heart(), PAL, 18);
               Vfx.runeCircle(level, this.groundAt(level, this.getEntityPos()), 6.0, PAL, 40, 0.05F, Vfx.Rune.GLYPH);
            })
            .tick((b, level, target, t) -> {
               if (t < 18 && target != null) {
                  this.faceTarget(target, 10.0F);
               }

               if (t == 18) {
                  Vec3d l = this.landing(level, target != null ? this.predict(target, 22.0) : this.at(8.0, 0.0, 0.0), 14.0, target);
                  this.aimAt(l, 60.0F);
                  this.spot = l;
                  this.arc = new GrothmawBoss.Arc(this.getEntityPos(), l, 18, 22, 9.0);
                  Vfx.telegraphCircle(level, l, 7.0, 22, PAL);
                  Vfx.heavyImpact(level, this.getEntityPos(), PAL, 1.0);
               }

               if (t > 18) {
                  this.tickArc(t);
                  if (t < 40) {
                     if (t % 2 == 0) {
                        Vfx.trail(level, this.getEntityPos().add(0.0, 2.5, 0.0), PAL, 2.0);
                     }

                     if (t >= 30) {
                        Vfx.meteorTrail(level, this.getEntityPos().add(0.0, 2.0, 0.0), this.getVelocity(), PAL);
                     }
                  }
               }
            })
            .hit((b, level, target, i) -> this.breaker(level, i))
            .end((b, level, target, i) -> this.endArc())
      );
      out.add(
         BossAction.of("worldeater_maw", 4.0, 0.9, 1.9, 2.6, 3.3)
            .range(0.0, 9.0)
            .cooldown(700)
            .phase(3)
            .weight(9)
            .tags("melee ultimate")
            .lockFacing()
            .advance(0.7, 0.9, 0.55)
            .telegraph(0.0, 0.9)
            .utility((b, t, ctx) -> ctx.targetAirborne ? 0.0F : (ctx.targetHealth < 0.5F ? 2.0F : 1.0F))
            .start((b, level, target, i) -> {
               this.grabbed = null;
               this.gripBroken = false;
               this.gripDamage = 0.0F;
               Vfx.chargeUp(level, this.snout(4.4, 1.0), PAL, 18);
            })
            .tick((b, level, target, t) -> {
               if (this.aim(target, t, 4, 8.0)) {
                  Vfx.telegraphCone(level, this.getEntityPos(), this.getYaw(), 30.0F, 11.7, 14, PAL);
               }

               if (t >= 4 && t <= 16 && t % 4 == 0) {
                  Vfx.flare(level, this.snout(4.4, 1.0), PAL.glow(), 1.5, 5);
               }

               if (t == 26 || t == 40) {
                  this.spot = this.groundAt(level, this.at(4.6, t == 26 ? 2.8 : -2.8, 0.0));
                  Vfx.telegraphCircle(level, this.spot, 3.0, 12, PAL);
               }

               this.carry(level, t);
            })
            .hit((b, level, target, i) -> this.maw(level, i))
            .end((b, level, target, i) -> this.releaseGrab())
      );
      out.add(
         BossAction.of("extinction_event", 7.0, 1.5, 2.3, 3.1, 3.9, 6.0)
            .range(0.0, 40.0)
            .cooldown(1200)
            .phase(3)
            .weight(20)
            .anyTarget()
            .tags("aoe ultimate")
            .superArmor()
            .lockFacing()
            .telegraph(0.0, 6.0)
            .when((b, t) -> this.lastStand)
            .start((b, level, target, i) -> this.extinctionStart(level))
            .tick((b, level, target, t) -> {
               if (t == 90) {
                  Vfx.telegraphCircle(level, this.groundAt(level, this.getEntityPos()), 18.0, 30, PAL);
                  Vfx.chargeUp(level, this.heart(), PAL, 30);
               }
            })
            .hit((b, level, target, i) -> {
               if (i < 4) {
                  this.meteorVolley(level);
               } else {
                  this.primordialNova(level);
               }
            })
            .end((b, level, target, i) -> {
               if (i == -1) {
                  this.buff(100, 1.25F, 1.0F);
                  Vfx.smoke(level, this.getEntityPos(), PAL.smoke(), 16, 3.0, 2.5);
               }

               this.hollows.clear();
            })
      );
   }

   private void jaws(ServerWorld level, List<LivingEntity> hit, double power) {
      Vec3d o = this.snout(3.5, 3.2);
      Vfx.slashArc(level, o, this.getYaw(), 10.0F, 90.0F, 3.0, PAL, 120.0F);
      Vfx.slashArc(level, o, this.getYaw(), 10.0F, -90.0F, 3.0, PAL, 120.0F);
      int n = 0;

      for (LivingEntity e : hit) {
         if (n++ >= 4) {
            break;
         }

         Vfx.impact(level, mid(e), PAL, power);
      }
   }

   private void skyBite(ServerWorld level) {
      Vec3d head = this.at(4.0, 0.0, 7.0);
      Vec3d col = this.spot != null ? this.spot : this.groundAt(level, this.at(4.0, 0.0, 0.0));
      Box box = new Box(head, head).expand(4.5).union(new Box(col.x - 3.5, col.y - 0.5, col.z - 3.5, col.x + 3.5, col.y + 9.0, col.z + 3.5));
      List<LivingEntity> in = new ArrayList<>();

      for (LivingEntity e : this.targets(level, box)) {
         double cx = e.getX() - col.x;
         double cz = e.getZ() - col.z;
         double rr = 3.0 + e.getWidth() * 0.5;
         boolean column = cx * cx + cz * cz <= rr * rr && e.getY() >= col.y - 0.5 && e.getY() <= col.y + 9.0;
         if (column || mid(e).squaredDistanceTo(head) <= 20.25) {
            in.add(e);
         }
      }

      for (LivingEntity e : this.hurtAll(level, in, this.getEntityPos(), 26.0F, 0.0, 0.0, null, 0.0F)) {
         e.stopGliding();
         e.removeStatusEffect(StatusEffects.SLOW_FALLING);
         Vec3d v = e.getVelocity();
         e.setVelocity(v.x * 0.2, -1.4, v.z * 0.2);
         e.knockedBack = true;
      }

      Vfx.slashArc(level, this.at(4.0, 0.0, 6.0), this.getYaw(), -70.0F, -90.0F, 3.5, PAL, 150.0F);
      Vfx.impact(level, head, PAL, 0.9);
   }

   private void tectonic(ServerWorld level, int i) {
      Vec3d c = this.getEntityPos();
      Vec3d g = this.groundAt(level, c);
      ParticleEffect dust = dust(level, g);
      if (i == 0) {
         BossFx.slam(level, this, c, 4.5, 18.0F, 1.2);
         BossFx.wave(level, this, g, 11.0, 14, 14.0F, dust, 0.8, true);
         Vfx.heavyImpact(level, this.at(1.5, 1.2, 0.0), PAL, 1.0);
      } else {
         BossFx.slam(level, this, c, 5.0, 20.0F, 1.4);
         BossFx.wave(level, this, g, 13.0, 16, 16.0F, dust, 0.9, true);
         Vfx.heavyImpact(level, this.at(1.5, -1.2, 0.0), PAL, 1.2);
         if (this.phase() >= 3) {
            Vfx.shockwave(level, c, PAL, 3.0);
            this.schedule(10, () -> BossFx.wave(level, this, g, 15.0, 18, 16.0F, dust, 1.0, true));
         }
      }

      if (this.phase() >= 2) {
         Vfx.groundCrack(level, c, 8.0, PAL);
      }
   }

   private void boneQuake(ServerWorld level) {
      Vec3d from = this.lineFrom;
      if (this.lines.isEmpty()) {
         from = this.groundAt(level, this.at(2.5, 0.0, 0.0));
         this.lines.add(this.fwd());
      }

      List<Vec3d> dirs = List.copyOf(this.lines);
      Vec3d start = from;

      for (Vec3d dir : dirs) {
         this.lineHit(level, start, dir, 20.0, 2.6, 26.0F, 0.9);
         this.spikes(level, start, dir, 20.0, dirs.size() > 1 ? 3 : 2);
      }

      if (this.phase() >= 3) {
         for (Vec3d dir : dirs) {
            Vfx.telegraphLine(level, start, dir, 20.0, 2.6, 24, PAL);
         }

         this.schedule(24, () -> {
            for (Vec3d dirx : dirs) {
               this.lineHit(level, start, dirx, 20.0, 2.6, 18.0F, 0.6);
               this.spikes(level, start, dirx, 20.0, 3);
               this.tarPool(level, this.groundAt(level, start.add(new Vec3d(dirx.x, 0.0, dirx.z).normalize().multiply(10.0))));
            }
         });
      }
   }

   private void spikes(ServerWorld level, Vec3d from, Vec3d dir, double length, int every) {
      Vec3d d = new Vec3d(dir.x, 0.0, dir.z).normalize();
      int n = (int)(length / 1.2);

      for (int k = every; k <= n; k += every) {
         Vec3d p = this.groundAt(level, from.add(d.multiply(k * 1.2)));
         Vfx.pillar(level, p, PAL, 2.5, 0.6);
         Vfx.debris(level, p, Vfx.Debris.BONE, 15260864, 3, 0.4);
         if (k / every % 2 == 0) {
            Vfx.groundCrack(level, p, 1.2 * every, PAL);
         }
      }
   }

   private void paw(ServerWorld level) {
      Vec3d foot = this.at(0.8, -1.2, 0.0);
      level.spawnParticles(dust(level, foot), foot.x, foot.y + 0.2, foot.z, 14, 0.5, 0.1, 0.5, 0.12);
      Vfx.smoke(level, foot, PAL.smoke(), 3, 0.6, 1.2);
      Vfx.shake(level, this.getEntityPos(), 16.0, 0.3, 6);
   }

   private void launch(ServerWorld level) {
      Vfx.heavyImpact(level, this.at(-1.0, 0.0, 0.0), PAL, 0.6);
      Vfx.streaks(level, this.getEntityPos().add(0.0, 2.0, 0.0), this.fwd(), PAL, 10);
      this.lastPos = this.getEntityPos();
   }

   private void charge(ServerWorld level, int t, int from, int to, int wallFrom, float base) {
      if (t >= from && t <= to) {
         if (this.horizontalCollision && t >= wallFrom) {
            this.forceAction(level, "wall_crash");
         } else {
            Vec3d c = this.at(2.0, 0.0, 0.0);
            double r = 2.0;
            Vec3d f = this.fwd();
            DamageSource source = this.getDamageSources().mobAttack(this);

            for (LivingEntity e : this.targets(level, new Box(c.x - r - 1.0, c.y - 1.0, c.z - r - 1.0, c.x + r + 1.0, c.y + 3.5, c.z + r + 1.0))) {
               double dx = e.getX() - c.x;
               double dz = e.getZ() - c.z;
               double rr = r + e.getWidth() * 0.5;
               if (!(dx * dx + dz * dz > rr * rr) && this.trampled.add(e.getId()) && e.damage(level, source, this.dmg(base))) {
                  e.addVelocity(f.x * 2.6, 0.9, f.z * 2.6);
                  e.knockedBack = true;
                  Vfx.heavyImpact(level, mid(e), PAL, 0.8);
               }
            }

            if (t % 2 == 0) {
               Vfx.dashTrail(level, this.lastPos, this.getEntityPos(), PAL);
               level.spawnParticles(dust(level, this.getEntityPos()), this.getX(), this.getY() + 0.2, this.getZ(), 10, 1.2, 0.1, 1.2, 0.15);
               this.lastPos = this.getEntityPos();
            }

            if (t % 5 == 0) {
               Vfx.shake(level, this.getEntityPos(), 20.0, 0.4, 5);
               this.voice(level, "step", 2.0F);
            }
         }
      }
   }

   private void vault(ServerWorld level) {
      Vec3d best = null;
      double bestScore = -1.0;

      for (float off : new float[]{0.0F, 35.0F, -35.0F}) {
         Vec3d dir = Vec3d.fromPolar(0.0F, this.getYaw() + 180.0F + off);
         Vec3d dest = this.getEntityPos().add(dir.multiply(8.0));
         boolean clear = BossFx.safeGround(level, dest, 3.0);

         for (int k = 2; clear && k <= 8; k += 2) {
            clear = level.isSpaceEmpty(this, this.getBoundingBox().offset(dir.x * k, 0.6, dir.z * k));
         }

         if (clear) {
            double score = off == 0.0F ? 0.5 : 0.0;

            for (ServerPlayerEntity p : this.engaged()) {
               score += Math.sqrt(p.squaredDistanceTo(dest));
            }

            if (score > bestScore) {
               bestScore = score;
               best = dir;
            }
         }
      }

      if (best != null) {
         this.dash(best, 1.25, 9);
         this.setVelocity(this.getVelocity().x, 0.3, this.getVelocity().z);
         Vfx.dashTrail(level, this.getEntityPos(), this.getEntityPos().add(best.multiply(8.0)), PAL);
      }
   }

   private void hurl(ServerWorld level, @Nullable LivingEntity target) {
      Vec3d from = this.snout(6.0, 3.6);
      Vec3d l = target != null ? this.predict(target, 20.0) : this.groundAt(level, this.at(16.0, 0.0, 0.0));
      if (target != null && target.isOnGround()) {
         l = this.groundAt(level, l);
      }

      double g = 0.06;
      int flight = 20;
      Vec3d d = l.subtract(from);
      Vec3d v = new Vec3d(d.x / flight, d.y / flight + g * (flight + 1) * 0.5, d.z / flight);
      BossBolt.shoot(level, this, BossBolt.Style.SAND, from, v, this.dmg(24.0F)).gravity(g).aoe(3.2).life(40);
      Vfx.telegraphCircle(level, l, 3.2, flight, PAL);
      Vfx.flare(level, from, PAL.core(), 2.5, 6);
      if (this.phase() >= 2) {
         Vec3d at = l;
         this.schedule(flight, () -> {
            for (int k = 0; k < 3; k++) {
               Vec3d p = this.near(level, at, 2.5, 4.0);
               Vfx.telegraphCircle(level, p, 1.8, 12, PAL);
               this.schedule(12, () -> {
                  this.circleHit(level, p, 1.8, 12.0F, 0.5, 0.3, null);
                  Vfx.impact(level, p, PAL, 0.6);
                  Vfx.debris(level, p, Vfx.Debris.BONE, 15260864, 6, 0.4);
               });
            }
         });
      }
   }

   private void spit(ServerWorld level, @Nullable LivingEntity target) {
      Vec3d from = this.snout(5.8, 2.9);
      Vec3d aim;
      double flight;
      if (target != null) {
         flight = Math.max(1.0, from.distanceTo(target.getEntityPos()) / 1.6);
         aim = this.predict(target, flight).add(0.0, target.getHeight() * 0.5, 0.0);
      } else {
         flight = 8.0;
         aim = this.at(16.0, 0.0, 1.0);
      }

      Vec3d dir = aim.subtract(from).normalize();
      int n = this.phase() >= 3 ? 3 : 1;

      for (int k = 0; k < n; k++) {
         float off = k == 0 ? 0.0F : (k == 1 ? 12 : -12);
         Vec3d v = dir.rotateY((float)Math.toRadians(off)).multiply(1.6);
         BossBolt.shoot(level, this, BossBolt.Style.SAND, from, v, this.dmg(14.0F))
            .aoe(1.8)
            .effect(new StatusEffectInstance(StatusEffects.SLOWNESS, 50, 2))
            .life(40);
      }

      Vfx.flare(level, from, PAL.glow(), 1.8, 5);
      if (this.phase() >= 2 && target != null) {
         Vec3d pool = this.groundAt(level, aim);
         this.schedule((int)Math.round(flight), () -> this.tarPool(level, pool));
      }
   }

   private void tarPool(ServerWorld level, Vec3d center) {
      BossFx.zone(level, this, center, 2.6, 120, ParticleTypes.LANDING_HONEY, this.dmg(3.0F), new StatusEffectInstance(StatusEffects.SLOWNESS, 30, 1));
      Vfx.groundCrack(level, center, 2.6, PAL);
   }

   private void roar(ServerWorld level) {
      Vec3d c = this.getEntityPos();
      level.playSound(null, this.getX(), this.getY(), this.getZ(), this.roarSound(), SoundCategory.HOSTILE, 6.0F, 0.75F);
      DamageSource source = this.getDamageSources().mobAttack(this);

      for (LivingEntity e : this.cone(level, c, this.getYaw(), 70.0F, 16.0)) {
         this.breakGuard(level, e, 3.0F);
         double dx = e.getX() - c.x;
         double dz = e.getZ() - c.z;
         float falloff = (float)(1.0 - 0.5 * Math.min(1.0, Math.sqrt(dx * dx + dz * dz) / 16.0));
         if (e.damage(level, source, this.dmg(16.0F) * falloff)) {
            shove(e, c, 2.4, 0.5);
            e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 160, 0), this);
            e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1), this);
         }
      }

      BossFx.knockback(level, this, c, 7.0, 1.4);
      Vfx.streaks(level, this.snout(5.2, 6.0), this.fwd(), PAL, 16);
      Vfx.radialFlash(level, c, 24.0, PAL.glow(), 0.35F);
      Vfx.shockwave(level, this.snout(5.2, 6.0), PAL, 6.0);
      Vfx.shake(level, c, 28.0, 1.0, 20);
      if (this.phase() >= 2) {
         this.buff(200, 0.85F, 1.2F);
         Vfx.auraRing(level, c, PAL, 3.5);
         Vfx.embers(level, c.add(0.0, 2.5, 0.0), PAL.glow(), 16, 2.0);
      }

      if (this.phase() >= 3) {
         this.rockfall(level, this.volleySize());
      }
   }

   private int minions(World level) {
      return level.getEntitiesByClass(RealmMob.class, this.getBoundingBox().expand(32.0), LivingEntity::isAlive).size();
   }

   private void brood(ServerWorld level) {
      int near = this.minions(level) + this.eggs.size();
      int n = Math.min(this.phase() >= 2 ? 3 : 2, 6 - near);
      Vec3d from = this.snout(5.1, 5.6);
      Vfx.flare(level, from, PAL.glow(), 1.4, 6);

      for (int k = 0; k < n; k++) {
         Vec3d egg = BossFx.around(level, this, this.getEntityPos(), 5.0, 11.0);

         for (int s = 2; s <= 12; s += 2) {
            Vec3d p = eggArc(from, egg, s / 12.0);
            Vec3d v = eggArc(from, egg, (s + 1) / 12.0).subtract(p);
            this.schedule(s, () -> Vfx.meteorTrail(level, p, v, PAL));
         }

         this.schedule(12, () -> this.layEgg(level, egg));
      }

      EntityType<RealmMob> shellback = RealmMobs.type("shellback");
      if (this.phase() >= 3 && near + n < 6 && shellback != null) {
         BossFx.summonMinions(level, this, shellback, 1, 6.0, null, m -> Vfx.groundCrack(level, m.getEntityPos(), 2.0, PAL));
      }
   }

   private static Vec3d eggArc(Vec3d from, Vec3d to, double s) {
      return from.lerp(to, s).add(0.0, 12.0 * s * (1.0 - s), 0.0);
   }

   private void layEgg(ServerWorld level, Vec3d at) {
      this.eggs.add(new GrothmawBoss.Egg(at, 60));
      Vfx.telegraphCircle(level, at, 1.3, 60, PAL);
      Vfx.runeCircle(level, at, 1.0, PAL, 60, 0.05F, Vfx.Rune.GLYPH);
      Vfx.impact(level, at, PAL, 0.4);
   }

   private void tickEggs(ServerWorld level, boolean paused) {
      if (!this.eggs.isEmpty()) {
         Iterator<GrothmawBoss.Egg> it = this.eggs.iterator();

         while (it.hasNext()) {
            GrothmawBoss.Egg egg = it.next();
            if (this.crushed(egg)) {
               Vfx.debris(level, egg.at, Vfx.Debris.BONE, 15260864, 14, 0.4);
               Vfx.impact(level, egg.at, PAL, 0.5);
               it.remove();
            } else if (!paused) {
               if (--egg.left <= 0) {
                  it.remove();
                  this.hatch(level, egg.at);
               } else if (egg.left % 10 == 0) {
                  level.spawnParticles(
                     new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.TURTLE_EGG.getDefaultState()),
                     egg.at.x,
                     egg.at.y + 0.3,
                     egg.at.z,
                     4,
                     0.25,
                     0.15,
                     0.25,
                     0.05
                  );
                  Vfx.flare(level, egg.at.add(0.0, 0.4, 0.0), PAL.glow(), 0.8, 8);
               }
            }
         }
      }
   }

   private boolean crushed(GrothmawBoss.Egg egg) {
      for (ServerPlayerEntity p : this.engaged()) {
         double dx = p.getX() - egg.at.x;
         double dz = p.getZ() - egg.at.z;
         if (dx * dx + dz * dz <= 1.6900000000000002 && Math.abs(p.getY() - egg.at.y) < 1.5) {
            return true;
         }
      }

      return false;
   }

   private void hatch(ServerWorld level, Vec3d at) {
      EntityType<RealmMob> type = RealmMobs.type("primal_raptor");
      RealmMob raptor = type == null ? null : (RealmMob)type.create(level, SpawnReason.MOB_SUMMONED);
      if (raptor != null) {
         raptor.refreshPositionAndAngles(at.x, at.y, at.z, this.random.nextFloat() * 360.0F, 0.0F);
         if (this.getTarget() != null) {
            raptor.setTarget(this.getTarget());
         }

         raptor.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 1200, 0));
         level.spawnEntity(raptor);
         Vfx.pillar(level, at, PAL, 2.5, 0.7);
         Vfx.debris(level, at.add(0.0, 0.5, 0.0), Vfx.Debris.BONE, 15260864, 10, 0.3);
      }
   }

   private void breaker(ServerWorld level, int i) {
      Vec3d l = this.spot != null ? this.spot : this.getEntityPos();
      if (i == 0) {
         BossFx.slam(level, this, l, 7.0, 50.0F, 2.2);
         Vfx.heavyImpact(level, l, PAL, 2.5);
         Vfx.impactFrame(level, l, 24.0, PAL.glow(), 0.8F);
         Vfx.groundCrack(level, l, 8.0, PAL);
         this.breakerYaw = this.getYaw();
         this.fissureLines(level, l, this.breakerYaw);
      } else if (i == 1) {
         this.fissures(level, l, this.breakerYaw);
         this.fissureLines(level, l, this.breakerYaw + 36.0F);
         Vfx.telegraphCircle(level, l, 5.0, 12, PAL);
      } else {
         this.fissures(level, l, this.breakerYaw + 36.0F);
         BossFx.slam(level, this, l, 5.0, 30.0F, 1.6);
         BossFx.wave(level, this, l, 16.0, 20, 30.0F, dust(level, l), 1.2, true);
         Vfx.nova(level, l, PAL, 10.0);
      }
   }

   private void fissureLines(ServerWorld level, Vec3d l, float base) {
      for (int k = 0; k < 5; k++) {
         Vfx.telegraphLine(level, l, Vec3d.fromPolar(0.0F, base + k * 72), 16.0, 2.8, 12, PAL);
      }
   }

   private void fissures(ServerWorld level, Vec3d l, float base) {
      for (int k = 0; k < 5; k++) {
         Vec3d dir = Vec3d.fromPolar(0.0F, base + k * 72);
         this.lineHit(level, l, dir, 16.0, 2.8, 42.0F, 1.0);
         this.spikes(level, l, dir, 16.0, 3);
      }
   }

   private Vec3d mawPoint(double sec) {
      double[] a = MAW[0];
      if (sec <= a[0]) {
         return this.at(a[1], a[2], a[3]);
      }

      for (int k = 1; k < MAW.length; k++) {
         double[] b = MAW[k];
         if (sec <= b[0]) {
            double f = (sec - a[0]) / (b[0] - a[0]);
            return this.at(MathHelper.lerp(f, a[1], b[1]), MathHelper.lerp(f, a[2], b[2]), MathHelper.lerp(f, a[3], b[3]));
         }

         a = b;
      }

      return this.at(a[1], a[2], a[3]);
   }

   private void carry(ServerWorld level, int t) {
      LivingEntity v = this.grabbed;
      if (v != null && t > 18) {
         if (v.isAlive() && !v.isRemoved() && v.getEntityWorld() == level && !(v instanceof PlayerEntity p && (p.isCreative() || p.isSpectator()))) {
            Vec3d jaw = this.mawPoint(t / 20.0);
            double y = jaw.y - 0.9;
            Vec3d g = BossFx.ground(level, jaw.x, jaw.y, jaw.z);
            if (g != null && y < g.y) {
               y = g.y;
            }

            if (level.isSpaceEmpty(v, v.getBoundingBox().offset(jaw.x - v.getX(), y - v.getY(), jaw.z - v.getZ()))) {
               v.requestTeleport(jaw.x, y, jaw.z);
            }

            v.setVelocity(Vec3d.ZERO);
            v.onLanding();
            v.knockedBack = true;
         } else {
            this.grabbed = null;
         }
      }
   }

   private void maw(ServerWorld level, int i) {
      DamageSource source = this.getDamageSources().mobAttack(this);
      if (i != 0) {
         if (i != 1 && i != 2) {
            LivingEntity v = this.grabbed;
            this.grabbed = null;
            if (v != null && v.isAlive()) {
               Vec3d from = mid(v);
               Vec3d f = this.fwd();
               v.damage(level, source, this.dmg(18.0F));
               v.onLanding();
               v.setVelocity(f.x * 2.2, 1.0, f.z * 2.2);
               v.knockedBack = true;
               Vfx.dashTrail(level, from, from.add(f.multiply(6.0)).add(0.0, 2.0, 0.0), PAL);
               Vfx.impactFrame(level, from, 16.0, PAL.glow(), 0.6F);
            }
         } else {
            Vec3d p = this.spot != null ? this.spot : this.groundAt(level, this.at(4.6, i == 1 ? 2.8 : -2.8, 0.0));
            LivingEntity v = this.grabbed;
            if (v != null) {
               v.damage(level, source, this.dmg(14.0F));
            }

            List<LivingEntity> others = this.inCircle(level, p, 3.0);
            others.remove(v);
            this.hurtAll(level, others, p, 18.0F, 1.0, 0.4, null, 0.0F);
            Vfx.heavyImpact(level, p, PAL, 1.2);
            Vfx.groundCrack(level, p, 3.0, PAL);
         }
      } else {
         List<LivingEntity> in = this.cone(level, this.getEntityPos(), this.getYaw(), 30.0F, 9.5);
         ServerPlayerEntity prey = null;
         double best = Double.MAX_VALUE;

         for (LivingEntity e : in) {
            if (e instanceof ServerPlayerEntity p && p.squaredDistanceTo(this) < best) {
               best = p.squaredDistanceTo(this);
               prey = p;
            }
         }

         this.hurtAll(level, in, this.getEntityPos(), 16.0F, 0.0, 0.0, null, 0.0F);
         this.jaws(level, List.of(), 1.0);
         if (prey != null && prey.isAlive()) {
            this.grabbed = prey;
            this.gripDamage = 0.0F;
            this.gripBroken = false;
            prey.stopRiding();
            prey.stopGliding();
            Vfx.impact(level, mid(prey), PAL, 1.1);
         } else {
            this.buff(60, 1.3F, 1.0F);
         }
      }
   }

   private void releaseGrab() {
      LivingEntity v = this.grabbed;
      this.grabbed = null;
      this.gripDamage = 0.0F;
      if (v != null && v.isAlive()) {
         v.setVelocity(Vec3d.ZERO);
         v.onLanding();
         v.knockedBack = true;
      }
   }

   private void extinctionStart(ServerWorld level) {
      if (this.cooldownLeft("extinction_event") <= 0) {
         this.setCooldown("extinction_event", 1200);
      }

      this.hollows.clear();

      for (int tries = 0; tries < 20 && this.hollows.size() < 3; tries++) {
         Vec3d p = BossFx.around(level, this, this.getEntityPos(), 8.0, 13.0);
         double dx = p.x - this.getX();
         double dz = p.z - this.getZ();
         if (!(dx * dx + dz * dz < 49.0) && BossFx.safeGround(level, p, 3.0)) {
            boolean apart = true;

            for (Vec3d h : this.hollows) {
               apart &= h.squaredDistanceTo(p) >= 36.0;
            }

            if (apart) {
               this.hollows.add(p);
               Vfx.ward(level, p, 2.0, PAL, 125);
               Vfx.runeCircle(level, p, 2.0, PAL, 125, 0.03F, Vfx.Rune.GLYPH);
            }
         }
      }

      Vfx.screen(level, this.getEntityPos(), 48.0, 0.6, 30, 2101256, 0.35F);
      Vfx.chargeUp(level, this.heart(), PAL, 30);
      Vfx.vortex(level, this.getEntityPos(), 5.0, 10.0, PAL, 140);
   }

   private boolean inHollow(Vec3d p, double margin) {
      double r = 2.0 + margin;

      for (Vec3d h : this.hollows) {
         double dx = p.x - h.x;
         double dz = p.z - h.z;
         if (dx * dx + dz * dz < r * r) {
            return true;
         }
      }

      return false;
   }

   private void meteorVolley(ServerWorld level) {
      for (ServerPlayerEntity p : this.engaged()) {
         Vec3d a = this.groundAt(level, this.predict(p, 20.0));
         this.dropRock(level, a, 20, 20.0F, 2.6);
         this.dropRock(level, this.near(level, a, 1.5, 4.0), 20, 20.0F, 2.6);
      }

      for (int k = 0; k < 2; k++) {
         this.dropRock(level, BossFx.around(level, this, this.getEntityPos(), 4.0, 14.0), 20, 20.0F, 2.6);
      }

      Vfx.radialFlash(level, this.getEntityPos(), 32.0, PAL.glow(), 0.25F);
   }

   private void primordialNova(ServerWorld level) {
      Vec3d c = this.getEntityPos();
      DamageSource source = this.getDamageSources().mobAttack(this);
      float full = this.dmg(70.0F);

      for (LivingEntity e : this.targets(level, new Box(c, c).expand(19.0, 8.0, 19.0))) {
         double dx = e.getX() - c.x;
         double dz = e.getZ() - c.z;
         if (!(dx * dx + dz * dz > 324.0)) {
            boolean safe = this.inHollow(e.getEntityPos(), 0.3);
            if (e.damage(level, source, safe ? full * 0.2F : full) && !safe) {
               shove(e, c, 3.0, 1.0);
            }
         }
      }

      Vfx.nova(level, c, PAL, 18.0);
      Vfx.impactFrame(level, c, 40.0, PAL.glow(), 1.0F);
      Vfx.heavyImpact(level, c, PAL, 3.0);
      Vfx.pillar(level, c, PAL, 20.0, 2.5);
   }

   private void rock(ServerWorld level, Vec3d at, int delay, float base, double radius) {
      Vfx.telegraphCircle(level, at, radius, delay, PAL);
      Vfx.smoke(level, at.add(0.0, 18.0, 0.0), PAL.smoke(), 6, 1.5, 2.5);
      int fall = Math.min(10, delay - 1);

      for (int k = delay - fall; k < delay; k++) {
         Vec3d p = at.add(0.0, 18.0 * (delay - k) / fall, 0.0);
         this.schedule(k, () -> Vfx.meteorTrail(level, p, new Vec3d(0.0, -1.8, 0.0), PAL));
      }

      this.schedule(delay, () -> {
         this.circleHit(level, at, radius, base, 0.8, 0.3, new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 0));
         Vfx.heavyImpact(level, at, PAL, 0.8);
         Vfx.shards(level, at, PAL.smoke(), 10, 0.5);
         level.spawnParticles(dust(level, at), at.x, at.y + 0.2, at.z, 16, radius * 0.4, 0.1, radius * 0.4, 0.15);
      });
   }

   private void dropRock(ServerWorld level, Vec3d at, int delay, float base, double radius) {
      Vec3d p = at;

      for (int k = 0; k < 4 && this.inHollow(p, radius); k++) {
         p = this.near(level, at, 3.0, 6.0);
      }

      if (!this.inHollow(p, radius)) {
         this.rock(level, p, delay, base, radius);
      }
   }

   private void rockfall(ServerWorld level, int count) {
      List<ServerPlayerEntity> players = this.engaged();
      if (!players.isEmpty() && count > 0) {
         Vec3d first = this.groundAt(level, this.predict((LivingEntity)players.get(this.random.nextInt(players.size())), 30.0));
         this.dropRock(level, first, 30, 16.0F, 2.4);

         for (int k = 1; k < count; k++) {
            Vec3d at = players.size() > 1 && this.random.nextBoolean()
               ? this.groundAt(level, this.predict((LivingEntity)players.get(this.random.nextInt(players.size())), 30.0))
               : this.near(level, first, 1.5, 4.0);
            this.dropRock(level, at, 30, 16.0F, 2.4);
         }
      }
   }

   private int volleySize() {
      return this.phase() >= 3 ? 4 : this.phase() + 1;
   }

   private void tickRockfall(ServerWorld level) {
      if (this.phase() >= 1 && this.getTarget() != null && --this.rockfallTimer <= 0) {
         int p = Math.min(this.phase(), 3);
         int interval = p == 1 ? 220 + this.random.nextInt(81) : (p == 2 ? 180 + this.random.nextInt(61) : 120 + this.random.nextInt(41));
         this.rockfallTimer = this.enraged() ? Math.round(interval * 0.6F) : interval;
         this.rockfall(level, this.volleySize());
      }
   }

   private boolean hazardsPaused() {
      BossAction m = this.currentMove();
      if (m == null) {
         return false;
      }

      String a = m.anim();
      return a.startsWith("phase_") || a.equals("continental_breaker") || a.equals("worldeater_maw") || a.equals("extinction_event");
   }

   private boolean inPhaseClip() {
      BossAction m = this.currentMove();
      return m != null && m.anim().startsWith("phase_");
   }

   private void apexInstinct() {
      List<ServerPlayerEntity> players = this.engaged();
      if (players.size() >= 2) {
         LivingEntity target = this.getTarget();

         for (ServerPlayerEntity p : players) {
            if (p != target && !(p.squaredDistanceTo(this) > 576.0) && p.isUsingItem()) {
               UseAction use = p.getActiveItem().getUseAction();
               if ((use == UseAction.EAT || use == UseAction.DRINK) && this.random.nextFloat() < 0.35F) {
                  this.markTarget(p, 100);
                  this.setCooldown("amber_spit", 0);
                  return;
               }
            }
         }

         if (this.cullCooldown <= 0) {
            for (ServerPlayerEntity p : players) {
               if (p != target && p.squaredDistanceTo(this) <= 256.0 && p.getHealth() < p.getMaxHealth() * 0.3F && this.random.nextFloat() < 0.25F) {
                  this.markTarget(p, 80);
                  this.cullCooldown = 600;
                  return;
               }
            }
         }
      }
   }

   private void tryLastStand(ServerWorld level) {
      if (this.lastStandPending && !this.inPhaseClip()) {
         this.lastStandPending = false;
         this.forceAction(level, "extinction_event");
      }
   }

   @Override
   protected void mobTick(ServerWorld world) {
      super.mobTick(world);
      if (this.isAlive() && !this.inIntro()) {
         if (this.gripBroken) {
            this.gripBroken = false;
            if (this.grabbed != null) {
               this.releaseGrab();
               Vfx.shards(world, this.snout(4.0, 4.0), 15260864, 12, 0.5);
               this.forceAction(world, "wall_crash");
            }
         }

         this.tryLastStand(world);
         boolean paused = this.hazardsPaused();
         this.tickEggs(world, paused);
         if (!paused) {
            this.tickRockfall(world);
            if (this.phase() >= 3 && this.running() && this.isOnGround() && this.age % 12 == 0) {
               BossFx.wave(world, this, this.getEntityPos(), 5.0, 8, 8.0F, ParticleTypes.CLOUD, 0.6, true);
               Vfx.shockwave(world, this.getEntityPos(), PAL, 2.5);
            }
         }

         if (this.cullCooldown > 0) {
            this.cullCooldown--;
         }

         if (this.guardClearCooldown > 0) {
            this.guardClearCooldown--;
         }

         if (this.phase() >= 1 && --this.instinctTimer <= 0) {
            this.instinctTimer = 40;
            this.apexInstinct();
         }

         this.blockingTicks = this.ctx().targetBlocking ? this.blockingTicks + 1 : 0;
         if (this.blockingTicks >= 30 && this.guardClearCooldown <= 0 && this.cooldownLeft("skull_breaker") > 0) {
            this.setCooldown("skull_breaker", 0);
            this.guardClearCooldown = 200;
         }

         if (this.buffed() && this.age % 6 == 0) {
            Vfx.embers(world, this.getEntityPos().add(0.0, 3.0, 0.0), 16756800, 5, 2.0);
         }
      }
   }

   @Override
   public boolean damage(ServerWorld world, DamageSource source, float amount) {
      boolean wardUp = this.warded();
      if (!super.damage(world, source, amount)) {
         return false;
      }

      if (this.grabbed != null && source.getAttacker() != this.grabbed) {
         this.gripDamage += amount;
         if (this.gripDamage >= this.getMaxHealth() * 0.04F) {
            this.gripBroken = true;
         }
      }

      if (wardUp
         && source.getAttacker() instanceof LivingEntity attacker
         && source.getSource() == attacker
         && attacker != this
         && attacker.isAlive()
         && attacker.squaredDistanceTo(this) < 64.0) {
         attacker.damage(world, this.getDamageSources().thorns(this), this.dmg(6.0F));
         shove(attacker, this.getEntityPos(), 0.6, 0.2);
         Vfx.shards(world, attacker.getEyePos(), PAL.core(), 6, 0.3);
      }

      return true;
   }

   @Override
   protected void onPhaseChange(ServerWorld level, int phase) {
      this.charges = 0;
      this.rockfallTimer = 60;
      if (phase >= 3) {
         Vfx.screen(level, this.getEntityPos(), 48.0, 0.8, 30, 3150852, 0.3F);
      }
   }

   @Override
   protected void phaseBurst(ServerWorld level, int phase) {
      Vec3d c = this.getEntityPos();
      if (phase <= 1) {
         Vfx.debris(level, this.at(0.0, 1.2, 4.0), Vfx.Debris.BONE, 15260864, 24, 0.7);
         Vfx.debris(level, this.at(0.0, -1.2, 4.0), Vfx.Debris.BONE, 15260864, 24, 0.7);
         Vfx.heavyImpact(level, c, PAL, 1.5);
         Vfx.nova(level, c, PAL, 8.0);
         BossFx.slam(level, this, c, 7.0, 8.0F, 1.6);
         BossFx.knockback(level, this, c, 9.0, 1.4);
         this.rockfall(level, 2);
      } else if (phase == 2) {
         Vfx.nova(level, this.heart(), PAL, 10.0);
         Vfx.pillar(level, c, PAL, 12.0, 2.0);
         Vfx.groundCrack(level, c, 10.0, PAL);
         BossFx.slam(level, this, c, 8.0, 10.0F, 1.8);
         this.rockfall(level, 4);
      } else {
         Vfx.explosion(level, c.add(0.0, 4.0, 0.0), PAL, 5.0);
         Vfx.impactFrame(level, c, 32.0, PAL.glow(), 0.9F);
         Vfx.nova(level, c, PAL, 14.0);
         Vfx.debris(level, c.add(0.0, 4.5, 0.0), Vfx.Debris.BONE, 15260864, 30, 1.0);
         BossFx.slam(level, this, c, 9.0, 12.0F, 2.0);
         this.rockfall(level, 5);
      }
   }

   @Override
   protected void onStagger(ServerWorld level) {
      this.releaseGrab();
      this.charges = 0;
      Vfx.debris(level, this.getEntityPos().add(0.0, 3.5, 0.0), Vfx.Debris.BONE, 15260864, 16, 0.5);
      if (this.phase() >= 2) {
         Vfx.flare(level, this.heart(), PAL.glow(), 3.0, 10);
      }
   }

   @Override
   protected void onEnrage(ServerWorld level) {
      Vfx.flare(level, this.at(4.2, 0.0, 4.8), 16765024, 2.5, 20);
      Vfx.auraRing(level, this.getEntityPos(), PAL, 4.0);
      Vfx.nova(level, this.getEntityPos(), PAL, 8.0);
   }

   @Override
   protected void desperation(ServerWorld level) {
      this.lastStand = true;
      this.lastStandPending = true;
      this.tryLastStand(level);
   }

   @Override
   protected void onLand(ServerWorld level) {
      Vfx.groundCrack(level, this.getEntityPos(), 4.0, PAL);
   }

   @Override
   protected void onIntroTick(ServerWorld level, int left) {
      int e = this.introTicks() - left;
      Vec3d c = this.getEntityPos();
      if (e < 20 && e % 6 == 0) {
         Vfx.shards(level, c.add(0.0, 1.5, 0.0), 15260864, 6, 0.3);
         level.spawnParticles(
            new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.TURTLE_EGG.getDefaultState()), c.x, c.y + 1.5, c.z, 12, 0.9, 0.8, 0.9, 0.15
         );
      }

      switch (e) {
         case 20:
            Vfx.debris(level, c.add(0.0, 1.5, 0.0), Vfx.Debris.BONE, 15260864, 30, 0.7);
            Vfx.shards(level, c.add(0.0, 1.5, 0.0), 15260864, 20, 0.6);
            Vfx.impact(level, c, PAL, 1.0);
            Vfx.flare(level, c.add(0.0, 1.5, 0.0), PAL.glow(), 4.0, 10);
            break;
         case 40:
         case 58:
            Vfx.shockwave(level, c, PAL, 4.0);
            Vfx.auraRing(level, c, PAL, 3.0);
            break;
         case 74:
         case 82:
            Vec3d foot = this.at(0.5, e == 74 ? 1.2 : -1.2, 0.0);
            Vfx.heavyImpact(level, foot, PAL, 0.6);
            Vfx.shake(level, c, 24.0, 0.5, 8);
            level.spawnParticles(dust(level, foot), foot.x, foot.y + 0.2, foot.z, 14, 0.6, 0.1, 0.6, 0.12);
            break;
         case 98:
         case 101:
         case 104:
            Vfx.gather(level, this.snout(4.0, 4.0), PAL, 3.0, 8);
            break;
         case 106:
            level.playSound(null, this.getX(), this.getY(), this.getZ(), this.roarSound(), SoundCategory.HOSTILE, 6.0F, 0.7F);
            Vfx.screen(level, c, 40.0, 1.2, 30, PAL.glow(), 0.25F, 1);
            Vfx.nova(level, c, PAL, 12.0);

            for (int k = 0; k < 4; k++) {
               Vfx.streaks(level, c.add(0.0, 2.0, 0.0), Vec3d.fromPolar(0.0F, this.getYaw() + k * 90), PAL, 8);
            }

            BossFx.knockback(level, this, c, 10.0, 1.4);
            break;
         case 136:
            level.spawnParticles(dust(level, c), c.x, c.y + 0.2, c.z, 24, 1.6, 0.1, 1.6, 0.12);
      }
   }

   @Override
   protected void onDeathTick(ServerWorld level, int tick) {
      Vec3d c = this.getEntityPos();
      switch (tick) {
         case 8:
            Vfx.impact(level, c.add(0.0, 3.0, 0.0), PAL, 1.0);
            break;
         case 28:
            level.spawnParticles(dust(level, c), c.x, c.y + 0.2, c.z, 20, 1.4, 0.1, 1.4, 0.1);
            Vfx.shake(level, c, 20.0, 0.5, 8);
            break;
         case 44:
            level.playSound(null, this.getX(), this.getY(), this.getZ(), this.roarSound(), SoundCategory.HOSTILE, 4.0F, 0.6F);
            break;
         case 60:
            Vfx.flare(level, this.heart(), PAL.glow(), 3.0, 12);
            break;
         case 88:
            Vfx.heavyImpact(level, c, PAL, 1.4);
            Vfx.groundCrack(level, c, 6.0, PAL);
            Vfx.shake(level, c, 32.0, 1.2, 20);
            BossFx.knockback(level, this, c, 6.0, 0.8);
            BossFx.ring(level, c.add(0.0, 0.2, 0.0), 4.0, 24, dust(level, c), 2);
            break;
         case 112:
         case 124:
            Vfx.wisps(level, this.at(1.7, 0.0, 1.5), PAL, 8, 2.0, 40);
            Vfx.embers(level, this.at(1.7, 0.0, 1.5), PAL.glow(), 8, 2.0);
            break;
         case 136:
            Vfx.nova(level, c, PAL, 6.0);
            Vfx.debris(level, c.add(0.0, 2.0, 0.0), Vfx.Debris.BONE, 15260864, 20, 0.4);
      }
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
      RawAnimation[] looks = new RawAnimation[4];

      for (int i = 0; i < 4; i++) {
         looks[i] = this.loopAnim("look_" + i);
      }

      controllers.add(new AnimationController("look", 0, test -> test.setAndContinue(looks[Math.clamp(this.phase(), 0, 3)])));
      super.registerControllers(controllers);
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
   protected void writeCustomData(WriteView view) {
      super.writeCustomData(view);
      view.putBoolean("GrothmawLastStand", this.lastStand);
   }

   @Override
   protected void readCustomData(ReadView view) {
      super.readCustomData(view);
      this.lastStand = view.getBoolean("GrothmawLastStand", false);
   }

   private record Arc(Vec3d from, Vec3d to, int start, int len, double apex) {
      Vec3d point(double s) {
         return this.from.lerp(this.to, s).add(0.0, this.apex * 4.0 * s * (1.0 - s), 0.0);
      }
   }

   private static final class Egg {
      final Vec3d at;
      int left;

      Egg(Vec3d at, int left) {
         this.at = at;
         this.left = left;
      }
   }
}
