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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer.Builder;
import net.minecraft.entity.boss.BossBar.Color;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.jspecify.annotations.Nullable;
import software.bernie.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

public class LiraelBoss extends MysticalBoss {
   private static final BossSounds SOUNDS = BossSounds.of("lirael");
   public static final float WIDTH = 1.2F;
   public static final float HEIGHT = 3.4F;
   public static final boolean FIRE_IMMUNE = false;
   private static final Vfx.Palette FAE = Vfx.Palette.FAE;
   private static final Vfx.Palette MOON = new Vfx.Palette(16777215, 12111103, 6320288);
   private static final Vfx.Palette THORN = new Vfx.Palette(16770284, 14166106, 2503710).withDebris(Vfx.Debris.LEAF);
   private static final Vfx.Palette ECLIPSE = new Vfx.Palette(16050431, 10116351, 1182238).withDebris(Vfx.Debris.PETAL);
   private static final int PETAL = 16744132;
   private static final int LEAF = 5937736;
   private static final int PINK = 16759008;
   private static final int CRIMSON = 14166106;
   private boolean vanished;
   private int stepGuard;
   private int wardFx;
   private boolean wasDodging;
   private Vec3d dodgeFrom = Vec3d.ZERO;
   private int dodgeTrail;
   private final List<LiraelBoss.Patch> patches = new ArrayList<>();
   private int moonfallTimer = 140;
   private @Nullable Vec3d cageCenter;
   private double cageRadius = 22.0;
   private double cageSpin;
   private boolean cageUp;
   private boolean cagePending;
   private int hazardEpoch;
   private int emptyTicks;
   private boolean bloomPending;
   private boolean bloomArmed;
   private int nextBeat;
   private int staggerDrop;
   private @Nullable Vec3d moveCenter;
   private final List<Vec3d> spots = new ArrayList<>();
   private final List<List<Vec3d>> waves = new ArrayList<>();
   private final List<Vec3d> sanctuaries = new ArrayList<>();
   private final List<Vec3d> decoys = new ArrayList<>();
   private @Nullable Vec3d trueSpot;
   private @Nullable Vec3d huntLand;
   private @Nullable LivingEntity charmVictim;
   private @Nullable Vec3d lineFrom;
   private Vec3d lineDir = Vec3d.ZERO;
   private double lineLen;
   private float beamYaw0;
   private int beamDir = 1;
   private float beamPitch = 9.0F;
   private boolean beaming;
   private final Map<Integer, Integer> beamHits = new HashMap<>();
   private final Map<Integer, Integer> thornsHit = new HashMap<>();
   private final RawAnimation[] looks = new RawAnimation[4];

   public static Builder attributes() {
      return MysticalBoss.bossAttributes(840.0, 10.0, 0.3, 10.0);
   }

   public LiraelBoss(EntityType<? extends HostileEntity> type, World level) {
      super(type, level, "lirael", Color.PINK);
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
      return this.getMaxHealth() * 0.1F;
   }

   @Override
   public int enrageTicks() {
      return 7200;
   }

   @Override
   protected double runSpeed() {
      return 1.6;
   }

   @Override
   protected boolean canDodge() {
      return true;
   }

   @Override
   protected boolean preferBlink() {
      return true;
   }

   @Override
   protected boolean hovers() {
      return true;
   }

   @Override
   protected double hoverHeight() {
      return 1.6;
   }

   @Override
   protected double preferredDistance() {
      return switch (Math.min(3, this.phase())) {
         case 0 -> 7.5;
         case 1 -> 7.0;
         case 2 -> 6.5;
         default -> 5.5;
      };
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

   @Override
   protected boolean immune() {
      return this.vanished;
   }

   private Vec3d fwd() {
      return Vec3d.fromPolar(0.0F, this.getYaw());
   }

   private Vec3d local(double right, double up, double forward) {
      Vec3d f = this.fwd();
      Vec3d r = Vec3d.fromPolar(0.0F, this.getYaw() + 90.0F);
      return new Vec3d(this.getX() + f.x * forward + r.x * right, this.getY() + up, this.getZ() + f.z * forward + r.z * right);
   }

   private Vec3d tip() {
      return this.local(0.2, 3.75, 0.63);
   }

   private Vec3d thrustTip() {
      return this.local(0.0, 1.9, 4.1);
   }

   private Vec3d beamFrom() {
      return this.local(-0.17, 1.82, 2.82);
   }

   private Vec3d chest() {
      return this.local(0.0, 2.3, 0.0);
   }

   private Vec3d eyes() {
      return this.local(0.0, 3.13, 0.0);
   }

   private Vec3d crown() {
      return this.local(0.0, 3.9, 0.0);
   }

   private Vec3d waist() {
      return this.local(0.0, 1.75, 0.0);
   }

   private Vec3d brooch() {
      return this.local(0.0, 2.28, 0.17);
   }

   private Vec3d feet(ServerWorld level) {
      Vec3d g = groundBelow(level, this.getX(), this.getY() + 0.5, this.getZ(), 8);
      return g != null ? g : this.getEntityPos();
   }

   private static @Nullable Vec3d groundBelow(ServerWorld level, double x, double y, double z, int depth) {
      Mutable p = BlockPos.ofFloored(x, y, z).mutableCopy();

      for (int i = 0; i <= depth; i++) {
         BlockState s = level.getBlockState(p);
         VoxelShape shape = s.getCollisionShape(level, p);
         if (!shape.isEmpty()) {
            return new Vec3d(x, p.getY() + shape.getMax(Axis.Y), z);
         }

         if (!s.getFluidState().isEmpty()) {
            return new Vec3d(x, p.getY() + 0.9, z);
         }

         p.move(0, -1, 0);
      }

      return null;
   }

   private Vec3d spotGround(ServerWorld level, Vec3d p) {
      Vec3d g = groundBelow(level, p.x, p.y + 1.0, p.z, 10);
      if (g == null) {
         g = BossFx.ground(level, p.x, p.y, p.z);
      }

      return g != null ? g : p;
   }

   private Vec3d ringPoint(ServerWorld level, Vec3d c, double radius, double angle) {
      double x = c.x + Math.cos(angle) * radius;
      double z = c.z + Math.sin(angle) * radius;
      Vec3d g = groundBelow(level, x, c.y + 4.0, z, 12);
      return g != null ? g : new Vec3d(x, c.y, z);
   }

   private float yawTo(double x, double z) {
      return (float)(MathHelper.atan2(z - this.getZ(), x - this.getX()) * 180.0F / (float)Math.PI) - 90.0F;
   }

   private static float yawOf(Vec3d dir) {
      return (float)(MathHelper.atan2(dir.z, dir.x) * 180.0F / (float)Math.PI) - 90.0F;
   }

   private static double flat(LivingEntity e, Vec3d c) {
      double dx = e.getX() - c.x;
      double dz = e.getZ() - c.z;
      return Math.sqrt(dx * dx + dz * dz);
   }

   private static boolean inBand(Vec3d c, LivingEntity e, double r0, double r1, double dy) {
      double h = flat(e, c);
      return h >= r0 && h < r1 && Math.abs(e.getY() - c.y) <= dy;
   }

   private List<LivingEntity> cone(ServerWorld level, double range, float halfAngle) {
      List<LivingEntity> out = new ArrayList<>();
      Vec3d f = this.fwd();

      for (LivingEntity e : BossFx.victims(level, this, this.getEntityPos(), range + 5.0)) {
         double dx = e.getX() - this.getX();
         double dz = e.getZ() - this.getZ();
         double h = Math.sqrt(dx * dx + dz * dz);
         if (!(h > range + e.getWidth() * 0.5)
            && !(e.getY() < this.getY() - 5.0)
            && !(e.getY() > this.getY() + this.getHeight())
            && (!(h > 1.0) || !(Math.toDegrees(Math.acos(MathHelper.clamp((f.x * dx + f.z * dz) / h, -1.0, 1.0))) > halfAngle))) {
            out.add(e);
         }
      }

      return out;
   }

   private List<LivingEntity> strike(ServerWorld level, double range, float halfAngle, float base, double push, Vfx.Palette p) {
      List<LivingEntity> hit = new ArrayList<>();

      for (LivingEntity e : this.cone(level, range, halfAngle)) {
         if (this.phys(level, e, base)) {
            hit.add(e);
            Vfx.impact(level, e.getEntityPos().add(0.0, e.getHeight() * 0.5, 0.0), p, 0.7);
            Vec3d away = new Vec3d(e.getX() - this.getX(), 0.0, e.getZ() - this.getZ());
            away = away.lengthSquared() < 1.0E-4 ? this.fwd() : away.normalize();
            e.addVelocity(away.x * push, 0.25 + push * 0.15, away.z * push);
            e.knockedBack = true;
         }
      }

      return hit;
   }

   private List<LivingEntity> around(ServerWorld level, Vec3d c, double r, double dy) {
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : BossFx.victims(level, this, c, r + dy + 1.0)) {
         if (inBand(c, e, 0.0, r + e.getWidth() * 0.5, dy)) {
            out.add(e);
         }
      }

      return out;
   }

   private List<LivingEntity> column(ServerWorld level, Vec3d pt, double r, double height) {
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : BossFx.victims(level, this, pt.add(0.0, height * 0.5, 0.0), height * 0.5 + r + 2.0)) {
         double dy = e.getY() - pt.y;
         if (flat(e, pt) <= r + e.getWidth() * 0.5 && dy >= -0.5 && dy <= height) {
            out.add(e);
         }
      }

      return out;
   }

   private boolean magic(ServerWorld level, LivingEntity e, float base) {
      return e.damage(level, this.getDamageSources().indirectMagic(this, this), this.dmg(base));
   }

   private boolean phys(ServerWorld level, LivingEntity e, float base) {
      return e.damage(level, this.getDamageSources().mobAttack(this), this.dmg(base));
   }

   private void effect(LivingEntity e, RegistryEntry<StatusEffect> effect, int ticks, int amp) {
      e.addStatusEffect(new StatusEffectInstance(effect, ticks, amp), this);
   }

   private void guardBreak(ServerWorld level, LivingEntity e, int ticks) {
      if (e.isBlocking()) {
         ItemStack s = e.getActiveItem();
         e.clearActiveItem();
         if (e instanceof PlayerEntity p && !s.isEmpty()) {
            p.getItemCooldownManager().set(s, ticks);
         }

         Vec3d at = e.getEyePos().add(e.getRotationVec(1.0F).multiply(0.5)).add(0.0, -0.4, 0.0);
         Vfx.shards(level, at, 16759008, 14, 0.45);
         Vfx.glint(level, at, 16777215, 1.4);
      }
   }

   private void duelHeight(@Nullable LivingEntity t) {
      if (t != null) {
         Vec3d v = this.getVelocity();
         this.setVelocity(v.x, MathHelper.clamp((t.getY() + 0.1 - this.getY()) * 0.35, -0.35, 0.25), v.z);
      }
   }

   private void petals(ServerWorld level, Vec3d at, int count, double speed) {
      Vfx.debris(level, at, Vfx.Debris.PETAL, 16744132, count, speed);
   }

   private void leaves(ServerWorld level, Vec3d at, int count, double speed) {
      Vfx.debris(level, at, Vfx.Debris.LEAF, 5937736, count, speed);
   }

   private List<RealmMob> myMinions(ServerWorld level, double r) {
      return level.getEntitiesByClass(
         RealmMob.class, this.getBoundingBox().expand(r), m -> m.isAlive() && (m.bossId().equals("thorn_sprite") || m.bossId().equals("briar_knight"))
      );
   }

   private void summonCourt(ServerWorld level, int sprites, int knights, boolean glyphs) {
      int room = Math.max(0, 8 - this.myMinions(level, 32.0).size());
      int k = Math.min(knights, room);
      int s = Math.min(sprites, room - k);
      boolean strong = this.phase() >= 2;
      EntityType<RealmMob> knight = RealmMobs.type("briar_knight");
      if (k > 0 && knight != null) {
         BossFx.summonMinions(level, this, knight, k, 5.0, null, m -> {
            m.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 6000, 0), this);
            if (strong) {
               m.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 6000, 0), this);
            }

            if (glyphs) {
               Vfx.runeCircle(level, m.getEntityPos(), 1.2, FAE, 30, 0.05F, Vfx.Rune.GLYPH);
            }
         });
      }

      EntityType<RealmMob> sprite = RealmMobs.type("thorn_sprite");
      if (s > 0 && sprite != null) {
         BossFx.summonMinions(level, this, sprite, s, 4.0, null, m -> {
            m.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 6000, 0), this);
            if (glyphs) {
               Vfx.runeCircle(level, m.getEntityPos(), 1.2, FAE, 30, 0.05F, Vfx.Rune.GLYPH);
            }
         });
      }
   }

   @Override
   protected void defineActions(List<BossAction> out) {
      this.courtMoves(out);
      this.briarMoves(out);
      this.moonMoves(out);
      this.huntMoves(out);
   }

   private void courtMoves(List<BossAction> out) {
      out.add(
         BossAction.of("thornsong_thrust", 1.1, 0.5)
            .range(2.0, 9.0)
            .cooldown(70)
            .weight(14)
            .tags("melee gap_closer")
            .telegraph(0.1, 0.5)
            .advance(0.3, 0.5, 0.75)
            .combo("waltz_cut", 0.35F)
            .combo("crescent_waltz", 0.35F)
            .combo("courtly_feint", 0.2F)
            .utility((b, t, c) -> (c.kitingTicks > 40 ? 1.5F : 1.0F) * (c.dist > 6.0 ? 1.3F : 1.0F))
            .tick((b, l, t, k) -> {
               if (k <= 10) {
                  this.duelHeight(t);
               }

               if (k == 2) {
                  Vfx.telegraphLine(l, this.feet(l), this.fwd(), 7.5, 1.6, 8, FAE);
                  Vfx.glint(l, this.tip(), FAE.core(), 0.8);
               }
            })
            .hit((b, l, t, i) -> {
               this.strike(l, 4.4, 28.0F, 20.0F, 0.9, FAE);
               Vfx.slashArc(l, this.chest(), this.getYaw(), 0.0F, 4.4, FAE, 40.0F);
               Vfx.streaks(l, this.chest(), this.fwd(), FAE, 8);
               Vfx.glint(l, this.thrustTip(), FAE.core(), 1.2);
               if (this.phase() >= 3) {
                  Vec3d from = this.thrustTip();
                  Vec3d dir = t != null ? t.getEntityPos().add(0.0, t.getHeight() * 0.6, 0.0).subtract(from) : this.fwd();
                  dir = dir.lengthSquared() < 1.0E-4 ? this.fwd() : dir.normalize();
                  BossBolt.shoot(l, this, BossBolt.Style.THORN, from, dir.multiply(1.3), this.dmg(12.0F)).life(10);
               }
            })
      );
      out.add(
         BossAction.of("waltz_cut", 1.8, 0.4, 0.85, 1.35)
            .range(0.0, 5.5)
            .cooldown(110)
            .weight(12)
            .maxPhase(1)
            .tags("melee")
            .telegraph(0.1, 1.35)
            .advance(0.2, 0.4, 0.25)
            .advance(0.65, 0.85, 0.25)
            .advance(1.1, 1.35, 0.3)
            .combo("courtly_feint", 0.2F)
            .combo("fae_step", 0.2F)
            .tick((b, l, t, k) -> {
               if (k <= 27) {
                  this.duelHeight(t);
               }

               if (k == 2 || k == 11) {
                  Vfx.telegraphCone(l, this.feet(l), this.getYaw(), 70.0F, 4.6, 6, FAE);
               } else if (k == 18) {
                  Vfx.telegraphCircle(l, this.feet(l), 5.0, 9, FAE);
               }
            })
            .hit((b, l, t, i) -> {
               if (i < 2) {
                  this.strike(l, 4.6, 70.0F, 15.0F, 0.5, FAE);
                  Vfx.slashArc(l, this.chest(), this.getYaw(), 5.0F, i == 0 ? 0.0F : 35.0F, 4.6, FAE, i == 0 ? -160.0F : 160.0F);
               } else {
                  this.strike(l, 5.0, 180.0F, 22.0F, 1.2, FAE);
                  Vfx.slashArc(l, this.waist(), this.getYaw(), 0.0F, 0.0F, 5.0, FAE, 360.0F);
                  this.petals(l, this.waist(), 16, 0.45);
               }
            })
      );
      out.add(
         BossAction.of("courtly_feint", 1.6, 1.15)
            .range(0.0, 6.0)
            .cooldown(170)
            .weight(7)
            .tags("melee guard_break")
            .telegraph(0.85, 1.15)
            .advance(0.9, 1.15, 0.5)
            .utility((b, t, c) -> (c.targetBlocking ? 2.0F : 1.0F) * (c.ticksSinceHurt < 20 ? 1.3F : 1.0F))
            .tick((b, l, t, k) -> {
               if (k >= 12 && k <= 23) {
                  this.duelHeight(t);
               }

               if (k >= 5 && k <= 9 && k % 2 == 1) {
                  Vfx.glint(l, this.tip(), FAE.core(), 0.6 + (k - 5) * 0.1);
               } else if (k == 17) {
                  Vfx.telegraphLine(l, this.feet(l), this.fwd(), 6.0, 1.4, 6, FAE);
                  Vfx.chargeUp(l, this.tip(), FAE, 6);
               }
            })
            .hit((b, l, t, i) -> {
               for (LivingEntity e : this.cone(l, 4.8, 25.0F)) {
                  this.guardBreak(l, e, 100);
               }

               this.strike(l, 4.8, 25.0F, 26.0F, 1.0, FAE);
               Vfx.slashArc(l, this.chest(), this.getYaw(), 0.0F, 4.8, FAE, 30.0F);
               Vec3d at = t != null ? t.getEntityPos().add(0.0, t.getHeight() * 0.5, 0.0) : this.thrustTip();
               Vfx.impact(l, at, FAE, 1.0);
               Vfx.shards(l, at, 16759008, 12, 0.45);
               Vfx.glint(l, this.thrustTip(), 16777215, 2.0);
            })
      );
      out.add(
         BossAction.of("moonlit_volley", 1.3, 0.55, 0.8)
            .range(5.0, 34.0)
            .cooldown(50)
            .weight(13)
            .tags("ranged")
            .telegraph(0.1, 0.55)
            .utility(
               (b, t, c) -> (c.kitingTicks <= 40 && !c.targetUnreachable ? 1.0F : 1.5F) * (this.phase() == 3 ? 0.7F : 1.0F) * (c.targetBlocking ? 0.6F : 1.0F)
            )
            .tick((b, l, t, k) -> {
               if (k == 1) {
                  Vfx.chargeUp(l, this.tip(), MOON, 10);
               }
            })
            .hit(
               (b, l, t, i) -> {
                  if (t != null) {
                     double f = MathHelper.clamp(this.distanceTo(t) - 1.5, 0.0, 4.2);
                     if (i == 0) {
                        this.fan(
                           l,
                           t,
                           this.local(0.0, 2.25, f),
                           BossBolt.Style.THORN,
                           3 + this.phase(),
                           9.0F,
                           0.9,
                           this.dmg(11.0F),
                           new StatusEffectInstance(StatusEffects.POISON, 60, 0),
                           true
                        );
                     } else {
                        this.fan(
                           l,
                           t,
                           this.local(0.8, 3.6, f),
                           BossBolt.Style.STAR,
                           2 + this.phase(),
                           6.0F,
                           1.05,
                           this.dmg(12.0F),
                           new StatusEffectInstance(StatusEffects.GLOWING, 100, 0),
                           false
                        );
                     }
                  }
               }
            )
      );
      out.add(
         BossAction.of("bewitching_charm", 1.5, 0.85)
            .range(5.0, 24.0)
            .cooldown(260)
            .weight(5)
            .tags("ranged punish")
            .telegraph(0.15, 0.85)
            .utility((b, t, c) -> !c.targetHealing && !c.targetAiming ? 0.6F : 3.0F)
            .tick((b, l, t, k) -> {
               if (k == 3) {
                  LivingEntity v = this.charmTarget(t);
                  this.charmVictim = v;
                  Vfx.chargeUp(l, this.eyes(), FAE, 14);
                  if (v != null) {
                     Vfx.runeCircle(l, this.spotGround(l, v.getEntityPos()), 1.3, FAE, 14, 0.08F, Vfx.Rune.GLYPH);
                  }
               }
            })
            .hit((b, l, t, i) -> this.gaze(l))
            .end((b, l, t, i) -> this.charmVictim = null)
      );
      out.add(
         BossAction.of("fae_step", 0.9, 0.3)
            .range(0.0, 28.0)
            .cooldown(100)
            .weight(8)
            .tags("mobility gap_closer")
            .combo("thornsong_thrust", 0.45F)
            .combo("briar_lash", 0.3F)
            .utility(
               (b, t, c) -> (c.targetBehind ? 2.5F : 1.0F)
                  * (c.surroundCount >= 2 ? 2.0F : 1.0F)
                  * (c.dist > 12.0 ? 2.0F : 1.0F)
                  * (c.ticksSinceHurt < 20 ? 1.5F : 1.0F)
            )
            .tick((b, l, t, k) -> {
               if (k == 5) {
                  this.stepGuard = 5;
               }
            })
            .hit((b, l, t, i) -> this.petalStep(l, t))
            .end((b, l, t, i) -> {
               if (i == -2) {
                  this.stepGuard = 0;
               }
            })
      );
      out.add(
         BossAction.of("rosebud_ward", 1.6, 0.8)
            .range(0.0, 40.0)
            .anyTarget()
            .cooldown(560)
            .weight(6)
            .tags("defensive")
            .utility((b, t, c) -> this.warded() ? 0.0F : (c.recentDamage > 0.08F ? 2.0F : 1.0F) * (this.phase() >= 2 ? 1.4F : 1.0F))
            .start((b, l, t, i) -> Vfx.gather(l, this.chest(), FAE, 2.5, 16))
            .hit((b, l, t, i) -> {
               this.shield(this.getMaxHealth() * (0.14F + 0.02F * this.phase()), 200);
               this.wardFx = 200;
               Vfx.shieldBubble(l, this.chest(), 2.2, FAE, 24);
               Vfx.runeCircle(l, this.feet(l), 2.4, FAE, 60, 0.03F, Vfx.Rune.ARCANE);
               this.petals(l, this.chest(), 14, 0.35);
            })
      );
      out.add(
         BossAction.of("call_the_court", 2.0, 1.1)
            .range(0.0, 40.0)
            .anyTarget()
            .cooldown(640)
            .weight(6)
            .tags("summon")
            .utility(
               (b, t, c) -> this.getEntityWorld() instanceof ServerWorld sl && this.myMinions(sl, 32.0).size() >= 5
                  ? 0.0F
                  : (c.engagedPlayers >= 2 ? 1.5F : 1.0F)
            )
            .tick((b, l, t, k) -> {
               if (k >= 4 && k <= 20 && k % 4 == 0) {
                  Vfx.gather(l, this.crown(), FAE, 3.0, 8);
               }
            })
            .hit((b, l, t, i) -> {
               int p = this.phase();
               this.summonCourt(l, 2 + p, p >= 1 ? (p >= 3 ? 2 : 1) : 0, true);
               Vec3d g = this.feet(l);
               Vfx.nova(l, g, FAE, 3.5);
               Vfx.auraRing(l, g, FAE, 2.5);
            })
      );
      out.add(
         BossAction.of("petal_gale", 1.4, 0.75)
            .range(0.0, 6.0)
            .cooldown(140)
            .weight(8)
            .maxPhase(1)
            .tags("aoe behind")
            .utility((b, t, c) -> (1.0F + 0.5F * c.surroundCount) * (c.ticksSinceHurt < 30 && c.dist < 4.0 ? 2.0F : 1.0F))
            .tick((b, l, t, k) -> {
               if (k == 2) {
                  this.moveCenter = this.feet(l);
                  Vfx.telegraphCircle(l, this.moveCenter, 6.0, 13, FAE);
               }
            })
            .hit((b, l, t, i) -> {
               Vec3d g = this.moveCenter != null ? this.moveCenter : this.feet(l);
               BossFx.slam(l, this, g, 6.0, 16.0F, 1.8);
               BossFx.effect(l, this, g, 6.0, new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1));
               Vfx.nova(l, g.add(0.0, 0.5, 0.0), FAE, 6.0);
               this.petals(l, this.chest(), 30, 0.7);

               for (int n = 0; n < 6; n++) {
                  Vec3d d = Vec3d.fromPolar(0.0F, this.getYaw() + n * 60);
                  Vfx.streaks(l, g.add(0.0, 1.0, 0.0).add(d.multiply(1.5)), d.multiply(2.5), FAE, 3);
               }
            })
      );
      out.add(
         BossAction.of("grasping_briars", 1.4, 0.9)
            .range(4.0, 26.0)
            .cooldown(120)
            .weight(10)
            .tags("ranged")
            .utility((b, t, c) -> (c.kitingTicks > 40 ? 2.0F : 1.0F) * (t != null && t.isOnGround() ? 1.2F : 1.0F) * (c.targetFlying ? 0.2F : 1.0F))
            .start((b, l, t, i) -> {
               this.spots.clear();
               if (t != null) {
                  this.spots.add(this.spotGround(l, this.predict(t, 18.0)));
                  if (this.phase() >= 2) {
                     Vec3d far = this.predict(t, 30.0);
                     Vec3d d = t.getEntityPos().subtract(this.getEntityPos()).getHorizontal();
                     Vec3d side = d.lengthSquared() < 1.0E-4 ? Vec3d.fromPolar(0.0F, this.getYaw() + 90.0F) : new Vec3d(-d.z, 0.0, d.x).normalize();
                     this.spots.add(this.spotGround(l, far.add(side.multiply(3.0))));
                     this.spots.add(this.spotGround(l, far.subtract(side.multiply(3.0))));
                  }

                  for (Vec3d s : this.spots) {
                     Vfx.telegraphCircle(l, s, 3.0, 18, THORN);
                  }
               }
            })
            .hit((b, l, t, i) -> {
               Set<Integer> hit = new HashSet<>();

               for (Vec3d s : this.spots) {
                  for (LivingEntity e : this.around(l, s, 3.0, 3.0)) {
                     if (hit.add(e.getId()) && this.phys(l, e, 18.0F)) {
                        this.effect(e, StatusEffects.SLOWNESS, 30, 6);
                        this.effect(e, StatusEffects.POISON, 60, 0);
                     }
                  }

                  Vfx.groundCrack(l, s, 3.0, THORN);
                  Vfx.pillar(l, s, THORN, 2.5, 0.8);
                  Vfx.shards(l, s.add(0.0, 0.5, 0.0), 5937736, 8, 0.4);
                  this.leaves(l, s.add(0.0, 0.6, 0.0), 8, 0.35);
                  if (this.phase() >= 1) {
                     this.addPatch(l, s, 2.5, 240);
                  }
               }
            })
      );
      out.add(BossAction.of("heavenward_thorns", 1.2, 0.6).range(0.0, 44.0).cooldown(90).weight(6).tags("ranged anti_air").when((b, t) -> {
         BossContext c = b.ctx();
         return c.targetFlying || c.targetAirborne || c.heightDiff > 3.5 || c.targetUnreachable;
      }).utility((b, t, c) -> c.targetFlying ? 2.0F : 1.0F).start((b, l, t, i) -> {
         this.moveCenter = null;
         if (t != null) {
            Vec3d p = t.isGliding() && t == this.ctx().target ? t.getEntityPos().add(this.ctx().targetVelocity.multiply(12.0)) : this.predict(t, 12.0);
            this.moveCenter = p;
            Vfx.chargeUp(l, p.add(0.0, 0.9, 0.0), MOON, 12);
            Vec3d g = groundBelow(l, p.x, p.y + 0.5, p.z, 30);
            if (g != null) {
               Vfx.telegraphCircle(l, g, 2.0, 12, MOON);
            }
         }
      }).hit((b, l, t, i) -> {
         Vec3d p = this.moveCenter;
         if (p != null) {
            Vfx.lightning(l, p.add(0.0, 16.0, 0.0), p, MOON, 3);
            Vfx.impact(l, p, MOON, 1.2);

            for (LivingEntity e : BossFx.victims(l, this, p.add(0.0, 0.9, 0.0), 2.4)) {
               this.magic(l, e, 22.0F);
               if (e.isGliding()) {
                  e.stopGliding();
               }

               e.removeStatusEffect(StatusEffects.SLOW_FALLING);
               e.removeStatusEffect(StatusEffects.LEVITATION);
               Vec3d v = e.getVelocity();
               e.setVelocity(v.x * 0.2, -1.6, v.z * 0.2);
               e.knockedBack = true;
               this.effect(e, StatusEffects.SLOWNESS, 40, 0);
            }
         }
      }));
   }

   private void briarMoves(List<BossAction> out) {
      out.add(
         BossAction.of("briar_lash", 1.2, 0.55)
            .range(0.0, 6.5)
            .cooldown(90)
            .weight(10)
            .phase(1)
            .tags("melee aoe behind")
            .utility((b, t, c) -> c.surroundCount >= 2 ? 2.0F : 1.0F)
            .tick((b, l, t, k) -> {
               if (k == 2) {
                  Vfx.telegraphCircle(l, this.feet(l), 6.0, 9, THORN);
               }
            })
            .hit((b, l, t, i) -> {
               for (LivingEntity e : this.cone(l, 6.0, 180.0F)) {
                  if (this.phys(l, e, 20.0F)) {
                     this.effect(e, StatusEffects.POISON, 60, 0);
                     Vec3d away = new Vec3d(e.getX() - this.getX(), 0.0, e.getZ() - this.getZ());
                     away = away.lengthSquared() < 1.0E-4 ? this.fwd() : away.normalize();
                     e.addVelocity(away.x * 1.2, 0.3, away.z * 1.2);
                     e.knockedBack = true;
                     Vfx.claw(l, e.getEntityPos().add(0.0, e.getHeight() * 0.6, 0.0), this.yawTo(e.getX(), e.getZ()), 0.0F, 1.2, THORN);
                  }
               }

               Vfx.slashArc(l, this.waist(), this.getYaw(), 0.0F, 0.0F, 6.0, THORN, 360.0F);
               this.leaves(l, this.waist(), 16, 0.5);
            })
      );
      out.add(
         BossAction.of("strangling_briar", 1.4, 0.7)
            .range(4.0, 18.0)
            .cooldown(220)
            .weight(7)
            .phase(1)
            .tags("ranged punish")
            .combo("waltz_cut", 0.5F)
            .combo("crescent_waltz", 0.5F)
            .combo("thornsong_thrust", 0.3F)
            .utility((b, t, c) -> (!c.targetAiming && !c.targetHealing ? 1.0F : 3.0F) * (c.targetUnreachable ? 2.0F : 1.0F))
            .tick((b, l, t, k) -> {
               if (k == 2) {
                  Vec3d f = this.feet(l);
                  Vec3d d = (t != null ? this.predict(t, 12.0) : f.add(this.fwd())).subtract(f).getHorizontal();
                  this.lineDir = d.lengthSquared() < 1.0E-4 ? this.fwd() : d.normalize();
                  this.lineFrom = f;
                  this.lineLen = Math.min(18.0F, (t != null ? this.distanceTo(t) : 10.0F) + 2.0F);
                  Vfx.telegraphLine(l, f, this.lineDir, this.lineLen, 1.4, 12, THORN);
               }
            })
            .hit((b, l, t, i) -> this.lance(l))
            .end((b, l, t, i) -> this.lineFrom = null)
      );
      out.add(
         BossAction.of("garden_of_thorns", 2.2, 1.2).range(0.0, 30.0).anyTarget().cooldown(340).weight(7).phase(1).tags("ranged aoe").start((b, l, t, i) -> {
            this.spots.clear();

            for (ServerPlayerEntity p : this.engaged()) {
               if (this.spots.size() >= 4) {
                  break;
               }

               this.spots.add(this.spotGround(l, this.predict(p, 24.0)));
            }

            LivingEntity anchor = t != null ? t : (this.engaged().isEmpty() ? null : (LivingEntity)this.engaged().get(0));
            Vec3d base = anchor != null ? anchor.getEntityPos() : this.feet(l);

            for (int n = 0; n < 2 && this.spots.size() < 6; n++) {
               this.spots.add(BossFx.around(l, this, base, 3.0, 5.0));
            }

            for (Vec3d s : this.spots) {
               Vfx.telegraphCircle(l, s, 2.8, 24, THORN);
            }
         }).hit((b, l, t, i) -> {
            Set<Integer> hit = new HashSet<>();

            for (Vec3d s : this.spots) {
               for (LivingEntity e : this.around(l, s, 2.8, 3.0)) {
                  if (hit.add(e.getId()) && this.phys(l, e, 24.0F)) {
                     this.effect(e, StatusEffects.POISON, 80, 0);
                     e.addVelocity(0.0, 0.5, 0.0);
                     e.knockedBack = true;
                  }
               }

               Vfx.pillar(l, s, THORN, 3.5, 1.2);
               Vfx.groundCrack(l, s, 2.8, THORN);
               this.leaves(l, s.add(0.0, 0.8, 0.0), 10, 0.4);
               this.addPatch(l, s, 2.6, 240);
            }
         })
      );
      out.add(
         BossAction.of("queens_verdict", 2.4, 1.3)
            .range(0.0, 8.0)
            .cooldown(260)
            .weight(9)
            .phase(1)
            .superArmor()
            .tags("melee aoe guard_break")
            .telegraph(0.2, 1.3)
            .utility((b, t, c) -> (c.targetBlocking ? 2.0F : 1.0F) * (c.surroundCount >= 2 ? 1.5F : 1.0F))
            .tick((b, l, t, k) -> {
               if (k == 4) {
                  this.moveCenter = this.feet(l);
                  Vfx.telegraphCircle(l, this.moveCenter, 7.0, 22, FAE);
                  Vfx.chargeUp(l, this.tip(), FAE, 18);
               }

               if (k >= 4 && k <= 17) {
                  this.setVelocity(0.0, 0.2, 0.0);
               } else if (k >= 22 && k <= 36) {
                  double dy = (this.moveCenter != null ? this.moveCenter.y : this.feet(l).y) + 0.75 - this.getY();
                  this.setVelocity(0.0, k <= 25 ? MathHelper.clamp(dy, -1.3, 0.0) : MathHelper.clamp(dy, -0.5, 0.0), 0.0);
               }
            })
            .hit((b, l, t, i) -> {
               Vec3d g = this.moveCenter != null ? this.moveCenter : this.feet(l);

               for (LivingEntity e : BossFx.victims(l, this, g, 7.0)) {
                  this.guardBreak(l, e, 100);
               }

               BossFx.slam(l, this, g, 7.0, 36.0F, 1.6);
               BossFx.launch(l, this, g, 7.0, 0.5);
               Vfx.heavyImpact(l, g, FAE, 1.6);
               Vfx.groundCrack(l, g, 7.0, THORN);
               Vfx.nova(l, g, MOON, 7.0);
               if (this.phase() >= 2) {
                  BossFx.wave(l, this, g, 12.0, 12, 14.0F, ParticleTypes.CHERRY_LEAVES, 0.9, true);
               }
            })
      );
   }

   private void moonMoves(List<BossAction> out) {
      out.add(
         BossAction.of("moonfall", 2.0, 1.0, 1.2, 1.4, 1.6, 1.8)
            .range(6.0, 40.0)
            .cooldown(220)
            .weight(9)
            .phase(2)
            .tags("ranged aoe")
            .telegraph(0.0, 1.8)
            .utility((b, t, c) -> c.kitingTicks > 40 ? 1.6F : 1.0F)
            .start((b, l, t, i) -> {
               this.spots.clear();
               this.moonColumn(l, t, 0);
            })
            .tick((b, l, t, k) -> {
               if (k > 0 && k <= 16 && k % 4 == 0) {
                  this.moonColumn(l, t, k / 4);
               }
            })
            .hit((b, l, t, i) -> {
               if (i < this.spots.size()) {
                  Vec3d pt = this.spots.get(i);

                  for (LivingEntity e : this.column(l, pt, 2.6, 10.0)) {
                     if (this.magic(l, e, 24.0F)) {
                        this.effect(e, StatusEffects.GLOWING, 100, 0);
                     }
                  }

                  Vfx.pillar(l, pt, MOON, 12.0, 1.4);
                  Vfx.impact(l, pt, MOON, 1.0);
                  Vfx.glint(l, pt.add(0.0, 1.2, 0.0), MOON.core(), 1.2);
               }
            })
      );
      out.add(
         BossAction.of("crescent_waltz", 2.0, 0.35, 0.7, 1.05, 1.55)
            .range(0.0, 6.0)
            .cooldown(120)
            .weight(12)
            .phase(2)
            .tags("melee")
            .telegraph(0.1, 1.55)
            .advance(0.15, 0.35, 0.3)
            .advance(0.5, 0.7, 0.3)
            .advance(0.85, 1.05, 0.3)
            .advance(1.3, 1.55, 0.35)
            .combo("lunar_scythe", 0.3F)
            .tick((b, l, t, k) -> {
               if (k <= 31) {
                  this.duelHeight(t);
               }

               switch (k) {
                  case 1:
                     Vfx.telegraphCone(l, this.feet(l), this.getYaw(), 30.0F, 4.8, 6, FAE);
                     break;
                  case 8:
                     Vfx.telegraphCone(l, this.feet(l), this.getYaw(), 80.0F, 4.8, 6, MOON);
                     break;
                  case 15:
                     Vfx.telegraphCone(l, this.feet(l), this.getYaw(), 80.0F, 4.8, 6, FAE);
                     break;
                  case 21:
                     Vec3d f = this.feet(l);
                     this.lineFrom = f;
                     this.lineDir = this.fwd();
                     Vfx.telegraphLine(l, f, this.lineDir, 10.0, 2.0, 10, MOON);
                     Vfx.telegraphCone(l, f, this.getYaw(), 90.0F, 5.0, 10, MOON);
               }
            })
            .hit((b, l, t, i) -> this.crescentCut(l, i))
            .end((b, l, t, i) -> this.lineFrom = null)
      );
      out.add(
         BossAction.of("lunar_scythe", 1.6, 0.95)
            .range(0.0, 9.0)
            .cooldown(170)
            .weight(9)
            .phase(2)
            .superArmor()
            .lockFacing()
            .tags("melee aoe")
            .telegraph(0.1, 0.95)
            .start((b, l, t, i) -> {
               if (t != null) {
                  this.faceTarget(t, 360.0F);
               }
            })
            .tick((b, l, t, k) -> {
               if (k <= 19) {
                  this.duelHeight(t);
               }

               if (k == 2) {
                  Vfx.telegraphCone(l, this.feet(l), this.getYaw(), 75.0F, 9.0, 17, MOON);
               }
            })
            .hit((b, l, t, i) -> {
               List<LivingEntity> reaped = this.cone(l, 9.0, 75.0F);
               this.strike(l, 9.0, 75.0F, 30.0F, 1.4, MOON);

               for (LivingEntity e : reaped) {
                  this.effect(e, StatusEffects.SLOWNESS, 40, 0);
               }

               Vfx.slashArc(l, this.chest(), this.getYaw(), 8.0F, 0.0F, 9.0, MOON, 200.0F);
               Vfx.streaks(l, this.chest(), this.fwd(), MOON, 10);
               Vfx.glint(l, this.local(0.0, 1.6, 2.2), MOON.core(), 1.4);
            })
      );
      out.add(
         BossAction.of("moonbeam", 3.4, 1.2, 1.6, 2.0, 2.4, 2.8)
            .range(6.0, 30.0)
            .cooldown(280)
            .weight(7)
            .phase(2)
            .lockFacing()
            .tags("ranged")
            .telegraph(0.0, 1.2)
            .utility((b, t, c) -> (c.kitingTicks > 40 ? 1.5F : 1.0F) * (c.dist > 12.0 ? 1.3F : 1.0F))
            .start((b, l, t, i) -> {
               if (t != null) {
                  this.faceTarget(t, 360.0F);
               }

               this.beamYaw0 = this.getYaw();
               this.beamDir = -this.beamDir;
               this.beaming = true;
               this.beamHits.clear();
               this.beamPitch = 9.0F;
               if (t != null) {
                  Vec3d from = this.beamFrom();
                  double hd = Math.max(1.0, flat(t, from));
                  this.beamPitch = (float)MathHelper.clamp(Math.toDegrees(Math.atan2(from.y - (t.getY() + 1.0), hd)), 4.0, 24.0);
               }

               Vfx.telegraphCone(l, this.feet(l), this.beamYaw0, 55.0F, 28.0, 24, MOON);
               Vfx.chargeUp(l, this.tip(), MOON, 22);
            })
            .tick((b, l, t, k) -> this.sweepBeam(l, k))
            .end((b, l, t, i) -> {
               this.beaming = false;
               this.beamHits.clear();
            })
      );
      out.add(
         BossAction.of("eclipse", 5.5, 2.2, 2.8, 3.4, 4.6)
            .range(0.0, 40.0)
            .anyTarget()
            .cooldown(1400)
            .weight(6)
            .phase(2)
            .superArmor()
            .lockFacing()
            .tags("ranged aoe ultimate")
            .telegraph(0.2, 4.6)
            .start((b, l, t, i) -> {
               Vec3d c = this.feet(l);
               this.moveCenter = c;
               this.sanctuaries.clear();
               this.waves.clear();
               Vfx.chargeUp(l, this.crown(), ECLIPSE, 44);
               Vfx.runeCircle(l, c, 6.0, ECLIPSE, 110, 0.04F, Vfx.Rune.ASTRAL);
               Vfx.screen(l, c, 48.0, 0.15, 30, 1182238, 0.45F);
               this.setVelocity(0.0, 0.15, 0.0);
            })
            .tick((b, l, t, k) -> {
               Vec3d c = this.moveCenter;
               if (c != null) {
                  if (k <= 16) {
                     this.setVelocity(0.0, 0.15, 0.0);
                  }

                  if (k == 16) {
                     this.placeSanctuaries(l, c);
                  } else if (k == 18 || k == 30 || k == 42) {
                     this.eclipseWave(l);
                  } else if (k == 48) {
                     Vfx.telegraphCircle(l, c, 18.0, 44, ECLIPSE);
                  }
               }
            })
            .hit((b, l, t, i) -> {
               if (i < 3) {
                  this.eclipseShards(l, i);
               } else {
                  this.totality(l);
               }
            })
            .end((b, l, t, i) -> {
               this.sanctuaries.clear();
               this.waves.clear();
            })
      );
   }

   private void huntMoves(List<BossAction> out) {
      out.add(
         BossAction.of("wild_hunt", 3.3, 0.8, 1.7, 2.6)
            .range(5.0, 32.0)
            .cooldown(260)
            .weight(10)
            .phase(3)
            .superArmor()
            .lockFacing()
            .tags("melee gap_closer mobility")
            .telegraph(0.2, 2.6)
            .utility((b, t, c) -> (c.kitingTicks > 40 ? 2.0F : 1.0F) * (c.dist > 12.0 ? 1.5F : 1.0F))
            .tick((b, l, t, k) -> {
               if (k != 4 && k != 22 && k != 40) {
                  int d = (k - 13) % 18;
                  if (k >= 13 && k <= 51 && d >= 0 && d <= 2 && this.huntLand != null) {
                     this.setVelocity(0.0, -1.8, 0.0);
                     if (d == 0) {
                        Vfx.dashTrail(l, this.getEntityPos().add(0.0, 2.0, 0.0), this.huntLand.add(0.0, 1.0, 0.0), FAE);
                     }
                  }
               } else {
                  this.huntBlink(l, t, (k - 4) / 18);
               }
            })
            .hit((b, l, t, i) -> this.huntStrike(l, i))
            .end((b, l, t, i) -> this.huntLand = null)
      );
      out.add(
         BossAction.of("glamour_ambush", 2.2, 1.5)
            .range(3.0, 24.0)
            .cooldown(280)
            .weight(8)
            .phase(3)
            .tags("melee mobility gap_closer")
            .telegraph(0.95, 1.5)
            .tick((b, l, t, k) -> {
               if (k == 7) {
                  this.vanish(l, t);
               } else if (k == 19) {
                  this.ambushTells(l, t);
               } else if (k == 25) {
                  this.ambushBlink(l, t);
               } else if (k == 26) {
                  this.clearVanish();
               }

               if (k >= 26 && k <= 30) {
                  this.duelHeight(t);
               }
            })
            .hit((b, l, t, i) -> {
               for (LivingEntity e : this.strike(l, 4.6, 30.0F, 28.0F, 1.0, FAE)) {
                  this.effect(e, StatusEffects.SLOWNESS, 40, 1);
               }

               Vfx.slashArc(l, this.chest(), this.getYaw(), 0.0F, 4.6, FAE, 40.0F);
               Vfx.glint(l, this.thrustTip(), FAE.core(), 1.4);

               for (Vec3d d : this.decoys) {
                  this.petals(l, d.add(0.0, 1.2, 0.0), 16, 0.4);
                  Vfx.smoke(l, d.add(0.0, 1.0, 0.0), 16759008, 6, 0.6, 1.2);
               }

               this.decoys.clear();
            })
            .end((b, l, t, i) -> {
               this.clearVanish();
               this.trueSpot = null;
               this.decoys.clear();
            })
      );
      out.add(
         BossAction.of("thornbloom_cataclysm", 5.6, 2.4, 3.2, 4.0, 4.8)
            .range(0.0, 24.0)
            .anyTarget()
            .cooldown(1600)
            .weight(6)
            .phase(3)
            .superArmor()
            .lockFacing()
            .tags("aoe ultimate")
            .telegraph(0.4, 4.8)
            .start((b, l, t, i) -> {
               Vec3d c = this.feet(l);
               this.moveCenter = c;
               Vfx.runeCircle(l, c, 4.0, THORN, 112, 0.05F, Vfx.Rune.GLYPH);
               Vfx.chargeUp(l, this.chest(), THORN, 40);
            })
            .tick((b, l, t, k) -> {
               Vec3d c = this.moveCenter;
               if (c != null) {
                  if (k == 18 || k == 66) {
                     Vfx.telegraphCircle(l, c, 6.5, 30, THORN);
                  } else if (k == 34) {
                     this.ringTelegraph(l, c, 16, 9.5, 2.4, 30);
                  } else if (k == 50) {
                     this.ringTelegraph(l, c, 22, 15.5, 2.6, 30);
                  }
               }
            })
            .hit((b, l, t, i) -> this.bloomRing(l, i))
      );
      out.add(
         BossAction.of("last_bloom", 7.5, 1.0, 7.0)
            .range(0.0, 64.0)
            .anyTarget()
            .cooldown(0)
            .weight(1)
            .phase(3)
            .when((b, t) -> false)
            .superArmor()
            .lockFacing()
            .tags("aoe defensive ultimate")
            .telegraph(1.0, 7.0)
            .start((b, l, t, i) -> {
               this.moveCenter = this.feet(l);
               this.bloomArmed = false;
               Vfx.screen(l, this.getEntityPos(), 48.0, 0.2, 30, 1706516, 0.45F);
               Vfx.chargeUp(l, this.chest(), THORN, 20);
            })
            .tick((b, l, t, k) -> this.bloomTick(l, k))
            .hit((b, l, t, i) -> {
               if (i == 0) {
                  this.sealBud(l);
               } else {
                  this.bloom(l);
               }
            })
            .end((b, l, t, i) -> this.bloomArmed = false)
      );
   }

   private void fan(
      ServerWorld level,
      LivingEntity target,
      Vec3d from,
      BossBolt.Style style,
      int count,
      float spread,
      double speed,
      float damage,
      StatusEffectInstance effect,
      boolean homing
   ) {
      double ticks = from.distanceTo(target.getEyePos()) / speed;
      Vec3d aim = this.predict(target, ticks).add(0.0, target.getStandingEyeHeight(), 0.0).subtract(from);
      aim = aim.lengthSquared() < 1.0E-4 ? this.fwd() : aim.normalize();

      for (int n = 0; n < count; n++) {
         float a = (n - (count - 1) / 2.0F) * spread;
         BossBolt bolt = BossBolt.shoot(level, this, style, from, aim.rotateY(a * (float) (Math.PI / 180.0)).multiply(speed), damage)
            .life(60)
            .effect(new StatusEffectInstance(effect));
         if (homing) {
            bolt.homing(target, 0.025);
         }
      }

      Vfx.flare(level, from, style == BossBolt.Style.THORN ? FAE.glow() : MOON.glow(), 1.8, 6);
      Vfx.flare(level, from, 16777215, 0.9, 4);
      Vfx.sparks(level, from, MOON.core(), 8, 0.3, 0.2);
   }

   private @Nullable LivingEntity charmTarget(@Nullable LivingEntity target) {
      ServerPlayerEntity best = null;
      double bestD = 576.0;

      for (ServerPlayerEntity p : this.engaged()) {
         double d = p.squaredDistanceTo(this);
         if (p.isUsingItem() && d <= bestD && this.canSee(p)) {
            best = p;
            bestD = d;
         }
      }

      return (LivingEntity)(best != null ? best : target);
   }

   private void gaze(ServerWorld level) {
      LivingEntity v = this.charmVictim;
      this.charmVictim = null;
      if (v != null && v.isAlive()) {
         Vec3d eyes = this.eyes();
         Vec3d toHer = eyes.subtract(v.getEyePos());
         boolean looking = toHer.lengthSquared() > 1.0E-4 && v.getRotationVec(1.0F).dotProduct(toHer.normalize()) > 0.45 && v.canSee(this);
         if (!looking) {
            this.effect(v, StatusEffects.GLOWING, 60, 0);
            Vfx.glint(level, eyes, FAE.glow(), 0.9);
            Vfx.smoke(level, eyes, 16759008, 4, 0.3, 0.6);
         } else {
            v.clearActiveItem();
            this.magic(level, v, 8.0F);
            this.effect(v, StatusEffects.SLOWNESS, 60, 1);
            this.effect(v, StatusEffects.WEAKNESS, 100, 0);
            this.effect(v, StatusEffects.NAUSEA, 60, 0);
            Vec3d pull = this.local(0.0, 0.0, 2.5).subtract(v.getEntityPos()).getHorizontal();
            if (pull.lengthSquared() > 1.0E-4) {
               pull = pull.normalize().multiply(1.4);
               v.addVelocity(pull.x, 0.35, pull.z);
               v.knockedBack = true;
            }

            Vfx.beam(level, eyes, v.getEyePos(), FAE, 0.35, 8);
            this.petals(level, v.getEyePos(), 12, 0.3);
            Vfx.flare(level, v.getEyePos(), FAE.glow(), 1.6, 8);
         }
      }
   }

   private void petalStep(ServerWorld level, @Nullable LivingEntity target) {
      if (target != null) {
         Vec3d from = this.getEntityPos();
         Vec3d tp = target.getEntityPos();
         Vec3d dest;
         if (this.distanceTo(target) > 10.0F) {
            Vec3d toMe = from.subtract(tp).getHorizontal();
            dest = toMe.lengthSquared() > 1.0E-4 ? tp.add(toMe.normalize().multiply(3.2)) : tp;
         } else {
            Vec3d look = target.getRotationVector().getHorizontal();
            if (look.lengthSquared() < 1.0E-4) {
               look = Vec3d.fromPolar(0.0F, target.getYaw());
            }

            dest = tp.subtract(look.normalize().multiply(3.2));
         }

         if (!this.blinkTo(dest.add(0.0, this.hoverHeight(), 0.0))) {
            this.blinkTo(BossFx.around(level, this, tp, 3.0, 5.0).add(0.0, this.hoverHeight(), 0.0));
         }

         this.faceTarget(target, 360.0F);
         Vfx.dashTrail(level, from.add(0.0, 1.7, 0.0), this.getEntityPos().add(0.0, 1.7, 0.0), FAE);
         this.petals(level, from.add(0.0, 1.7, 0.0), 12, 0.35);
         this.petals(level, this.getEntityPos().add(0.0, 1.7, 0.0), 12, 0.35);
         if (this.phase() >= 2) {
            Vec3d mine = this.spotGround(level, from.subtract(0.0, 1.0, 0.0));
            Vfx.telegraphCircle(level, mine, 2.5, 20, MOON);
            int epoch = this.hazardEpoch;
            this.schedule(20, () -> {
               if (epoch == this.hazardEpoch && this.isAlive()) {
                  for (LivingEntity e : this.around(level, mine, 2.5, 3.0)) {
                     this.magic(level, e, 16.0F);
                  }

                  Vfx.nova(level, mine, MOON, 2.5);
               }
            });
         }
      }
   }

   private void lance(ServerWorld level) {
      Vec3d f = this.lineFrom;
      if (f != null) {
         Vec3d a = f.add(0.0, 1.0, 0.0);
         Vec3d dir = this.lineDir;
         double len = this.lineLen;
         LivingEntity best = null;
         double bestAlong = Double.MAX_VALUE;

         for (LivingEntity e : BossFx.victims(level, this, a.add(dir.multiply(len * 0.5)), len * 0.5 + 3.0)) {
            Vec3d rel = e.getEntityPos().add(0.0, e.getHeight() * 0.5, 0.0).subtract(a);
            double along = rel.x * dir.x + rel.z * dir.z;
            if (!(along < 0.0) && !(along > len)) {
               double ox = rel.x - dir.x * along;
               double oz = rel.z - dir.z * along;
               if (!(Math.sqrt(ox * ox + oz * oz) > 1.3 + e.getWidth() * 0.5) && !(Math.abs(rel.y) > 2.5) && along < bestAlong) {
                  best = e;
                  bestAlong = along;
               }
            }
         }

         if (best == null) {
            Vfx.groundCrack(level, f.add(dir.multiply(len)), 1.5, THORN);
         } else {
            Vec3d chestAt = best.getEntityPos().add(0.0, best.getHeight() * 0.6, 0.0);
            best.clearActiveItem();
            this.phys(level, best, 12.0F);
            this.effect(best, StatusEffects.SLOWNESS, 30, 3);
            Vec3d pt = this.local(0.0, 0.0, 2.6);
            Vec3d v = pt.subtract(best.getEntityPos()).multiply(0.32);
            if (v.length() > 2.2) {
               v = v.normalize().multiply(2.2);
            }

            best.setVelocity(v.x, v.y + 0.35, v.z);
            best.knockedBack = true;
            Vfx.beam(level, this.chest(), chestAt, THORN, 0.3, 10);
            Vfx.dashTrail(level, chestAt, pt, THORN);
            Vfx.claw(level, chestAt, this.yawTo(best.getX(), best.getZ()), 0.0F, 1.2, THORN);
         }
      }
   }

   private void moonColumn(ServerWorld level, @Nullable LivingEntity target, int k) {
      List<LivingEntity> order = new ArrayList<>();
      if (target != null) {
         order.add(target);
      }

      for (ServerPlayerEntity p : this.engaged()) {
         if (p != target) {
            order.add(p);
         }
      }

      Vec3d pt;
      if (order.isEmpty()) {
         pt = BossFx.around(level, this, this.feet(level), 3.0, 8.0);
      } else {
         pt = this.spotGround(level, this.predict(order.get(k % order.size()), 20.0));
      }

      this.spots.add(pt);
      Vfx.telegraphCircle(level, pt, 2.6, 20, MOON);
   }

   private void crescentCut(ServerWorld level, int i) {
      float yaw = this.getYaw();
      switch (i) {
         case 0:
            this.strike(level, 4.8, 30.0F, 14.0F, 0.6, FAE);
            Vfx.streaks(level, this.chest(), this.fwd(), FAE, 6);
            Vfx.glint(level, this.thrustTip(), FAE.core(), 1.0);
            break;
         case 1:
            this.strike(level, 4.8, 80.0F, 16.0F, 0.7, MOON);
            Vfx.slashArc(level, this.chest(), yaw, 5.0F, 30.0F, 4.8, MOON, 170.0F);
            break;
         case 2:
            this.strike(level, 4.8, 80.0F, 16.0F, 0.7, FAE);
            Vfx.slashArc(level, this.chest(), yaw, 5.0F, -30.0F, 4.8, FAE, -170.0F);
            break;
         default:
            this.strike(level, 5.0, 90.0F, 24.0F, 1.2, MOON);
            Vfx.slashArc(level, this.chest(), yaw, 5.0F, 45.0F, 5.0, MOON, 200.0F);
            Vfx.slashArc(level, this.chest(), yaw, 5.0F, -45.0F, 5.0, FAE, -200.0F);
            Vec3d f = this.lineFrom;
            if (f == null) {
               return;
            }

            Vec3d dir = this.lineDir;

            for (LivingEntity e : BossFx.victims(level, this, f.add(dir.multiply(7.5)), 6.0)) {
               double rx = e.getX() - f.x;
               double rz = e.getZ() - f.z;
               double along = rx * dir.x + rz * dir.z;
               double side = Math.abs(rx * dir.z - rz * dir.x);
               if (along >= 5.0 && along <= 10.0 && side <= 1.0 + e.getWidth() * 0.5 && Math.abs(e.getY() - f.y) <= 3.0) {
                  this.phys(level, e, 18.0F);
               }
            }

            Vfx.slashArc(level, f.add(dir.multiply(7.0)).add(0.0, 1.2, 0.0), yawOf(dir), 0.0F, 3.0, MOON, 160.0F);
      }
   }

   private void sweepBeam(ServerWorld level, int k) {
      if (this.beaming && k >= 22 && k <= 58) {
         float yaw = this.beamYaw0 - 55 * this.beamDir + 110 * this.beamDir * (k - 22) / 36.0F;
         this.setYaw(yaw);
         this.setBodyYaw(yaw);
         this.setHeadYaw(yaw);
         Vec3d from = this.beamFrom();
         Vec3d to = from.add(Vec3d.fromPolar(this.beamPitch, yaw).multiply(28.0));
         HitResult hit = level.raycast(new RaycastContext(from, to, ShapeType.COLLIDER, FluidHandling.NONE, this));
         boolean blocked = hit.getType() != Type.MISS;
         Vec3d end = blocked ? hit.getPos() : to;
         if (k % 2 == 0) {
            Vfx.beam(level, from, end, MOON, 0.7);
         }

         if (k % 6 == 0 && blocked) {
            Vfx.groundCrack(level, end, 1.4, MOON);
         }

         if (k >= 24) {
            this.beamDamage(level, from, end, k);
         }
      }
   }

   private void beamDamage(ServerWorld level, Vec3d from, Vec3d end, int k) {
      Vec3d mid = from.add(end).multiply(0.5);

      for (LivingEntity e : BossFx.victims(level, this, mid, from.distanceTo(end) * 0.5 + 3.0)) {
         Integer last = this.beamHits.get(e.getId());
         if (last == null || k - last >= 8) {
            Box box = e.getBoundingBox().expand(1.3);
            if ((box.contains(from) || box.raycast(from, end).isPresent()) && this.magic(level, e, 13.0F)) {
               this.beamHits.put(e.getId(), k);
               this.effect(e, StatusEffects.GLOWING, 100, 0);
               this.effect(e, StatusEffects.WEAKNESS, 60, 0);
               Vfx.impact(level, e.getEntityPos().add(0.0, e.getHeight() * 0.5, 0.0), MOON, 0.6);
            }
         }
      }
   }

   private void placeSanctuaries(ServerWorld level, Vec3d c) {
      this.sanctuaries.clear();
      double a0 = this.random.nextDouble() * Math.PI * 2.0;

      for (int n = 0; n < 3; n++) {
         double a = a0 + n * Math.PI * 2.0 / 3.0;
         Vec3d spot = null;

         for (int tries = 0; tries < 6 && spot == null; tries++) {
            Vec3d p = this.ringPoint(level, c, 9.0 + this.random.nextDouble() * 3.0, a);
            if (BossFx.safeGround(level, p, 2.0)) {
               spot = p;
            }

            a += Math.toRadians(20.0);
         }

         if (spot == null) {
            spot = this.ringPoint(level, c, 10.0, a0 + n * Math.PI * 2.0 / 3.0);
         }

         this.sanctuaries.add(spot);
         Vfx.runeCircle(level, spot, 2.2, MOON, 80, 0.03F, Vfx.Rune.ASTRAL);
         Vfx.wisps(level, spot.add(0.0, 0.5, 0.0), MOON, 5, 1.8, 76);
         Vfx.pillar(level, spot, MOON, 6.0, 0.6);
      }
   }

   private void eclipseWave(ServerWorld level) {
      List<Vec3d> wave = new ArrayList<>();

      for (ServerPlayerEntity p : this.engaged()) {
         if (wave.size() >= 4) {
            break;
         }

         Vec3d pt = this.spotGround(level, this.predict(p, 26.0));
         wave.add(pt);
         Vfx.telegraphCircle(level, pt, 3.0, 26, ECLIPSE);
      }

      this.waves.add(wave);
   }

   private void eclipseShards(ServerWorld level, int i) {
      if (i < this.waves.size()) {
         Set<Integer> hit = new HashSet<>();

         for (Vec3d pt : this.waves.get(i)) {
            Vfx.lightning(level, pt.add(0.0, 18.0, 0.0), pt, ECLIPSE, 2);
            Vfx.heavyImpact(level, pt, ECLIPSE, 1.2);

            for (LivingEntity e : this.around(level, pt, 3.0, 4.0)) {
               if (hit.add(e.getId()) && this.magic(level, e, 30.0F)) {
                  this.effect(e, StatusEffects.SLOWNESS, 40, 1);
               }
            }
         }
      }
   }

   private void totality(ServerWorld level) {
      Vec3d c = this.moveCenter != null ? this.moveCenter : this.feet(level);

      for (LivingEntity e : BossFx.victims(level, this, c, 20.0)) {
         boolean safe = false;

         for (Vec3d s : this.sanctuaries) {
            if (flat(e, s) <= 2.4 && Math.abs(e.getY() - s.y) < 4.0) {
               safe = true;
               break;
            }
         }

         if (safe) {
            Vfx.shieldBubble(level, e.getEntityPos().add(0.0, 0.9, 0.0), 1.2, MOON, 16);
            Vfx.glint(level, e.getEyePos(), MOON.core(), 1.0);
         } else if (this.magic(level, e, 58.0F)) {
            this.effect(e, StatusEffects.WITHER, 60, 0);
            Vec3d away = new Vec3d(e.getX() - c.x, 0.0, e.getZ() - c.z);
            away = away.lengthSquared() < 1.0E-4 ? this.fwd() : away.normalize();
            e.addVelocity(away.x * 1.2, 0.4, away.z * 1.2);
            e.knockedBack = true;
         }
      }

      Vfx.nova(level, c, ECLIPSE, 18.0);
      Vfx.impactFrame(level, c, 40.0, 15259903, 0.8F);
      Vfx.radialFlash(level, c, 40.0, 16777215, 0.6F);
      Vfx.explosion(level, this.chest(), ECLIPSE, 3.0);
   }

   private void huntBlink(ServerWorld level, @Nullable LivingEntity target, int k) {
      LivingEntity quarry = k == 1 ? this.weakest(24.0) : null;
      if (quarry == null) {
         quarry = target;
      }

      if (quarry == null) {
         this.huntLand = null;
      } else {
         Vec3d land = this.spotGround(level, this.predict(quarry, 12.0));
         if (!this.blinkTo(land.add(0.0, 5.5, 0.0))) {
            land = BossFx.around(level, this, quarry.getEntityPos(), 2.0, 4.0);
            this.blinkTo(land.add(0.0, 5.5, 0.0));
         }

         this.faceTarget(quarry, 360.0F);
         this.huntLand = land;
         Vfx.telegraphCircle(level, land, k == 2 ? 4.0 : 3.0, 12, MOON);
         this.petals(level, this.getEntityPos().add(0.0, 1.7, 0.0), 12, 0.35);
      }
   }

   private void huntStrike(ServerWorld level, int k) {
      Vec3d land = this.huntLand;
      if (land != null) {
         if (level.isSpaceEmpty(this, this.getBoundingBox().offset(land.x - this.getX(), land.y + 0.1 - this.getY(), land.z - this.getZ()))) {
            this.requestTeleport(land.x, land.y + 0.1, land.z);
         }

         boolean last = k == 2;
         BossFx.slam(level, this, land, last ? 4.0 : 3.0, last ? 30.0F : 24.0F, 1.0);
         Vfx.heavyImpact(level, land, FAE, last ? 1.6 : 1.0);
         Vfx.groundCrack(level, land, 3.0, THORN);
      }
   }

   private @Nullable ServerPlayerEntity weakest(double r) {
      ServerPlayerEntity best = null;

      for (ServerPlayerEntity p : this.engaged()) {
         if (p.squaredDistanceTo(this) <= r * r && (best == null || p.getHealth() < best.getHealth())) {
            best = p;
         }
      }

      return best;
   }

   private void vanish(ServerWorld level, @Nullable LivingEntity target) {
      this.vanished = true;
      this.setInvisible(true);
      BossFx.blink(level, this, this.getEntityPos());
      this.petals(level, this.getEntityPos().add(0.0, 1.7, 0.0), 20, 0.4);
      this.decoys.clear();
      this.trueSpot = null;
      if (target != null) {
         float back = target.getYaw() + 180.0F;
         int real = this.random.nextFloat() < 0.6F ? 0 : this.random.nextInt(3);

         for (int n = 0; n < 3; n++) {
            Vec3d s = this.spotGround(level, target.getEntityPos().add(Vec3d.fromPolar(0.0F, back + 120 * n).multiply(4.0)));
            Vfx.vortex(level, s, 1.0, 3.0, FAE, 23);
            if (n == real) {
               this.trueSpot = s;
            } else {
               this.decoys.add(s);
            }
         }
      }
   }

   private void ambushTells(ServerWorld level, @Nullable LivingEntity target) {
      Vec3d s = this.trueSpot;
      if (s != null) {
         Vfx.chargeUp(level, s.add(0.0, 1.6, 0.0), MOON, 11);
         Vec3d d = (target != null ? target.getEntityPos() : s.add(this.fwd())).subtract(s).getHorizontal();
         Vfx.telegraphLine(level, s, d.lengthSquared() > 1.0E-4 ? d.normalize() : this.fwd(), d.length() + 2.5, 1.6, 11, MOON);
      }

      for (Vec3d d : this.decoys) {
         Vfx.chargeUp(level, d.add(0.0, 1.6, 0.0), FAE, 11);
      }
   }

   private void ambushBlink(ServerWorld level, @Nullable LivingEntity target) {
      Vec3d s = this.trueSpot;
      if (s != null && !this.blinkTo(s.add(0.0, 0.6, 0.0)) && target != null) {
         this.blinkTo(BossFx.around(level, this, target.getEntityPos(), 3.0, 5.0).add(0.0, 0.6, 0.0));
      }

      if (target != null) {
         this.faceTarget(target, 360.0F);
      }
   }

   private void clearVanish() {
      if (this.vanished || this.isInvisible()) {
         this.vanished = false;
         this.setInvisible(false);
      }
   }

   private void ringTelegraph(ServerWorld level, Vec3d c, int count, double radius, double r, int ticks) {
      double a0 = this.random.nextDouble() * Math.PI * 2.0;

      for (int n = 0; n < count; n++) {
         Vfx.telegraphCircle(level, this.ringPoint(level, c, radius, a0 + n * Math.PI * 2.0 / count), r, ticks, THORN);
      }
   }

   private void bloomRing(ServerWorld level, int i) {
      Vec3d c = this.moveCenter;
      if (c != null) {
         double r0 = i == 1 ? 6.5 : (i == 2 ? 12.5 : 0.0);
         double r1 = i == 1 ? 12.5 : (i == 2 ? 18.5 : 6.5);

         for (LivingEntity e : BossFx.victims(level, this, c, r1 + 4.0)) {
            if (inBand(c, e, r0, r1, 4.0) && this.phys(level, e, 46.0F)) {
               this.effect(e, StatusEffects.POISON, 80, 1);
               e.addVelocity(0.0, 0.7, 0.0);
               e.knockedBack = true;
            }
         }

         int n = i == 1 ? 12 : (i == 2 ? 16 : 8);
         double mid = (r0 + r1) * 0.5;
         double a0 = this.random.nextDouble() * Math.PI * 2.0;

         for (int p = 0; p < n; p++) {
            Vec3d at = this.ringPoint(level, c, mid, a0 + p * Math.PI * 2.0 / n);
            Vfx.pillar(level, at, THORN, 4.0, 1.0);
            if (p % 2 == 0) {
               Vfx.groundCrack(level, at, 2.0, THORN);
            }
         }

         if (i == 3) {
            Vfx.explosion(level, c.add(0.0, 1.0, 0.0), THORN, 6.0);
            Vfx.impactFrame(level, c, 40.0, 16759008, 0.9F);
            this.petals(level, this.chest(), 30, 0.8);

            for (int p = 0; p < 3; p++) {
               this.addPatch(level, this.ringPoint(level, c, 7.0 + this.random.nextDouble() * 5.0, this.random.nextDouble() * Math.PI * 2.0), 2.5, 240);
            }
         }
      }
   }

   private void sealBud(ServerWorld level) {
      Vec3d g = this.moveCenter != null ? this.moveCenter : this.feet(level);
      this.shield(this.getMaxHealth() * 0.3F, 122);
      Vfx.shieldBubble(level, this.chest(), 2.6, THORN, 130);
      Vfx.ward(level, g, 3.0, FAE, 130);
      Vfx.telegraphCircle(level, g, 26.0, 120, THORN);
      this.summonCourt(level, 4, 0, false);
      this.bloomArmed = true;
      this.nextBeat = 21;
   }

   private void bloomTick(ServerWorld level, int k) {
      if (this.bloomArmed && k > 20 && k < 140) {
         if (!this.warded()) {
            this.bloomArmed = false;
            Vfx.explosion(level, this.chest(), FAE, 3.0);
            this.petals(level, this.chest(), 30, 0.8);
            Vfx.impactFrame(level, this.chest(), 32.0, 16777215, 0.6F);
            this.buff(160, 1.5F, 0.6F);
            this.voice(level, "stagger", 4.0F);
            this.stopTriggeredAnim("action", "last_bloom");
            this.cancelMove(level, 30);
         } else {
            if (k >= this.nextBeat) {
               Vec3d g = this.moveCenter != null ? this.moveCenter : this.feet(level);
               Vfx.auraRing(level, g, THORN, 2.0);
               Vfx.flare(level, this.brooch(), THORN.glow(), 1.4, 6);
               Vfx.flare(level, this.brooch(), 16777215, 0.6, 4);
               this.nextBeat = k + Math.max(6, 16 - (k - 20) * 10 / 120);
            }
         }
      }
   }

   private void bloom(ServerWorld level) {
      if (this.bloomArmed) {
         this.bloomArmed = false;
         Set<LivingEntity> hit = new LinkedHashSet<>();

         for (ServerPlayerEntity p : this.engaged()) {
            if (p.squaredDistanceTo(this) <= 2304.0) {
               hit.add(p);
            }
         }

         hit.addAll(BossFx.victims(level, this, this.getEntityPos(), 32.0));

         for (LivingEntity e : hit) {
            if (this.magic(level, e, 62.0F)) {
               this.effect(e, StatusEffects.WITHER, 100, 1);
               this.effect(e, StatusEffects.WEAKNESS, 200, 1);
            }
         }

         this.heal(this.getMaxHealth() * 0.1F);
         Vec3d g = this.moveCenter != null ? this.moveCenter : this.feet(level);
         Vfx.nova(level, g, THORN, 30.0);
         Vfx.impactFrame(level, g, 64.0, 16759008, 1.0F);
         Vfx.explosion(level, this.chest(), FAE, 5.0);
         this.petals(level, this.chest(), 30, 0.9);
      }
   }

   private void addPatch(ServerWorld level, Vec3d c, double r, int life) {
      while (this.patches.size() >= 6) {
         this.patches.remove(0);
      }

      this.patches.add(new LiraelBoss.Patch(c, r, life));
      Vfx.groundCrack(level, c, r, THORN);
      Vfx.runeCircle(level, c, r, THORN, life, 0.01F, Vfx.Rune.GLYPH);
   }

   private void tickPatches(ServerWorld level) {
      if (!this.patches.isEmpty()) {
         boolean hurt = this.age % 10 == 0;
         boolean fx = this.age % 40 == 0;
         boolean crack = this.age % 60 == 0;
         Iterator<LiraelBoss.Patch> it = this.patches.iterator();

         while (it.hasNext()) {
            LiraelBoss.Patch p = it.next();
            if (--p.left <= 0) {
               it.remove();
            } else {
               if (hurt) {
                  for (ServerPlayerEntity pl : this.engaged()) {
                     if (inBand(p.c, pl, 0.0, p.r + 0.3, 2.0) && this.magic(level, pl, 3.0F)) {
                        this.effect(pl, StatusEffects.SLOWNESS, 30, 1);
                        if (!pl.hasStatusEffect(StatusEffects.POISON)) {
                           this.effect(pl, StatusEffects.POISON, 40, 0);
                        }
                     }
                  }
               }

               if (fx) {
                  this.leaves(level, p.c.add(0.0, 0.4, 0.0), 4, 0.2);
                  Vfx.sparks(level, p.c.add(0.0, 0.3, 0.0), 14166106, 4, 0.15, 0.15);
               }

               if (crack) {
                  Vfx.groundCrack(level, p.c, p.r, THORN);
               }
            }
         }
      }
   }

   private void tickMoonfall(ServerWorld level) {
      if (this.phase() >= 2 && !this.engaged().isEmpty() && !this.staggered() && !this.invulnerableNow()) {
         BossAction cur = this.currentMove();
         if (cur == null || !cur.anim().equals("eclipse") && !cur.anim().equals("last_bloom")) {
            if (--this.moonfallTimer <= 0) {
               int base = this.phase() >= 3 ? 100 : 140;
               this.moonfallTimer = this.enraged() ? Math.round(base * 0.6F) : base;
               ServerPlayerEntity p = this.engaged().get(this.random.nextInt(this.engaged().size()));
               Vec3d pt = this.spotGround(level, this.predict(p, 30.0));
               Vfx.telegraphCircle(level, pt, 2.5, 30, MOON);
               int epoch = this.hazardEpoch;
               this.schedule(30, () -> {
                  if (epoch == this.hazardEpoch && this.isAlive()) {
                     for (LivingEntity e : this.column(level, pt, 2.5, 10.0)) {
                        if (this.magic(level, e, 18.0F)) {
                           this.effect(e, StatusEffects.GLOWING, 60, 0);
                        }
                     }

                     Vfx.pillar(level, pt, MOON, 14.0, 1.2);
                     Vfx.impact(level, pt, MOON, 0.9);
                  }
               });
            }
         }
      }
   }

   private Vec3d pickCageCenter(ServerWorld level) {
      BlockPos h = this.home();
      if (h != null) {
         Vec3d hc = Vec3d.ofBottomCenter(h);
         if (hc.distanceTo(this.getEntityPos()) <= 24.0) {
            Vec3d g = groundBelow(level, hc.x, hc.y + 2.0, hc.z, 8);
            return g != null ? g : hc;
         }
      }

      return this.feet(level);
   }

   private void raiseCage(ServerWorld level) {
      Vec3d c = this.cageCenter != null ? this.cageCenter : this.pickCageCenter(level);
      this.cageCenter = c;
      this.cageRadius = 22.0;
      this.cageUp = true;
      this.cagePending = false;

      for (int n = 0; n < 16; n++) {
         Vfx.groundCrack(level, this.ringPoint(level, c, this.cageRadius, n * Math.PI * 2.0 / 16.0), 2.2, THORN);
      }

      this.cageFx(level, c);
   }

   private void tickCage(ServerWorld level) {
      if (this.phase() >= 3) {
         if (!this.cageUp) {
            if (!this.cagePending && !this.engaged().isEmpty()) {
               this.cageCenter = this.pickCageCenter(level);
               this.raiseCage(level);
            }
         } else {
            Vec3d c = this.cageCenter;
            if (c == null) {
               this.cageUp = false;
            } else {
               if (this.cageRadius > 13.0) {
                  this.cageRadius = Math.max(13.0, this.cageRadius - 0.005);
               }

               if (this.age % 10 == 0) {
                  for (ServerPlayerEntity p : this.engaged()) {
                     double dx = p.getX() - c.x;
                     double dz = p.getZ() - c.z;
                     double h = Math.sqrt(dx * dx + dz * dz);
                     if (!(h <= this.cageRadius)) {
                        this.magic(level, p, 4.0F);
                        this.effect(p, StatusEffects.SLOWNESS, 30, 2);
                        if (!p.hasStatusEffect(StatusEffects.POISON)) {
                           this.effect(p, StatusEffects.POISON, 40, 0);
                        }

                        if (h > 0.001) {
                           p.addVelocity(-dx / h * 0.6, 0.2, -dz / h * 0.6);
                        }

                        p.knockedBack = true;
                     }
                  }
               }

               if (this.age % 40 == 0) {
                  this.cageFx(level, c);
               }
            }
         }
      }
   }

   private void cageFx(ServerWorld level, Vec3d c) {
      Vfx.ring(level, c.add(0.0, 0.2, 0.0), THORN.glow(), this.cageRadius, 40);
      this.cageSpin += 0.26;

      for (int n = 0; n < 12; n++) {
         Vec3d p = this.ringPoint(level, c, this.cageRadius, this.cageSpin + n * Math.PI * 2.0 / 12.0);
         Vfx.pillar(level, p, THORN, 2.2, 0.5);
         if (n % 2 == 0) {
            this.leaves(level, p.add(0.0, 1.0, 0.0), 3, 0.25);
         }
      }
   }

   private void clearHazards() {
      this.patches.clear();
      this.sanctuaries.clear();
      this.waves.clear();
      this.cageUp = false;
      this.cageCenter = null;
      this.moonfallTimer = 140;
      this.hazardEpoch++;
   }

   @Override
   protected void mobTick(ServerWorld world) {
      super.mobTick(world);
      if (this.isAlive() && !this.inIntro()) {
         if (this.stepGuard > 0) {
            this.stepGuard--;
         }

         if (!this.vanished && this.isInvisible()) {
            this.setInvisible(false);
         }

         if (this.bloomPending && !this.staggered() && !this.invulnerableNow()) {
            this.bloomPending = false;
            this.forceAction(world, "last_bloom");
         }

         this.tickStaggerDrop(world);
         this.tickWardFx(world);
         this.tickDodgeFlair(world);
         if (this.engaged().isEmpty()) {
            if (++this.emptyTicks == 200) {
               this.clearHazards();
            }
         } else {
            this.emptyTicks = 0;
         }

         this.tickPatches(world);
         this.tickMoonfall(world);
         this.tickCage(world);
         this.ambient(world);
      }
   }

   private void tickStaggerDrop(ServerWorld level) {
      if (this.staggerDrop > 0) {
         if (!this.staggered()) {
            this.staggerDrop = 0;
         } else {
            Vec3d v = this.getVelocity();
            Vec3d g = groundBelow(level, this.getX(), this.getY() + 0.5, this.getZ(), 8);
            double dy = g != null ? g.y + 0.8 - this.getY() : 0.0;
            if (!(Math.abs(dy) <= 0.05) && --this.staggerDrop > 0) {
               this.setVelocity(v.x * 0.5, MathHelper.clamp(dy, -0.35, 0.2), v.z * 0.5);
            } else {
               this.staggerDrop = 0;
               this.setVelocity(v.x * 0.5, 0.0, v.z * 0.5);
               Vec3d at = g != null ? g : this.getEntityPos();
               this.petals(level, at.add(0.0, 0.6, 0.0), 24, 0.5);
               Vfx.impact(level, at, FAE, 0.8);
            }
         }
      }
   }

   private void tickWardFx(ServerWorld level) {
      if (this.wardFx > 0) {
         if (!this.warded()) {
            this.wardFx = 0;
         } else {
            if (--this.wardFx % 20 == 0 && this.wardFx > 0) {
               Vfx.shieldBubble(level, this.chest(), 2.2, FAE, 24);
            }
         }
      }
   }

   private void tickDodgeFlair(ServerWorld level) {
      boolean d = this.dodging();
      if (d && !this.wasDodging) {
         this.dodgeFrom = this.getEntityPos();
         this.dodgeTrail = 7;
         this.petals(level, this.getEntityPos().add(0.0, 1.7, 0.0), 10, 0.35);
      }

      this.wasDodging = d;
      if (this.dodgeTrail > 0 && --this.dodgeTrail == 0) {
         Vfx.dashTrail(level, this.dodgeFrom.add(0.0, 1.7, 0.0), this.getEntityPos().add(0.0, 1.7, 0.0), FAE);
      }
   }

   private void ambient(ServerWorld level) {
      if (!this.isInvisible()) {
         int t = this.age;
         if (t % 10 == 0) {
            this.petals(level, this.local(0.0, 0.3, 0.0), 2, 0.12);
         }

         if (t % 50 == 0) {
            Vfx.wisps(level, this.chest(), FAE, 3);
         }

         if (this.phase() >= 1 && t % 16 == 0) {
            double side = this.random.nextBoolean() ? 0.82 : -0.82;
            Vec3d tipAt = this.random.nextBoolean() ? this.local(side, 3.44, 0.52) : this.local(side * 0.56, 0.7, -1.56);
            Vfx.embers(level, tipAt, 14166106, 2, 0.15);
            this.leaves(level, tipAt, 1, 0.1);
         }

         if (this.phase() >= 2) {
            if (t % 50 == 25) {
               Vfx.wisps(level, this.chest(), MOON, 5, 2.0, 50);
            }

            if (t % 12 == 0) {
               Vfx.glint(level, this.local(0.0, 3.69, -0.33), MOON.core(), 0.5);
            }
         }

         if (this.phase() >= 3 && t % 40 == 0) {
            Vfx.auraRing(level, this.feet(level), THORN, 2.0);
            Vfx.embers(level, this.chest(), ECLIPSE.glow(), 4, 1.0);
         }
      }
   }

   @Override
   public boolean damage(ServerWorld world, DamageSource source, float amount) {
      boolean bypass = source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY);
      if (bypass || !this.vanished && this.stepGuard <= 0) {
         LivingEntity attacker = null;
         if (!bypass
            && this.warded()
            && !source.isOf(DamageTypes.THORNS)
            && !source.isIn(DamageTypeTags.IS_PROJECTILE)
            && source.getAttacker() instanceof LivingEntity a
            && source.getSource() == a
            && a.squaredDistanceTo(this) <= 25.0) {
            attacker = a;
         }

         boolean landed = super.damage(world, source, amount);
         if (landed && attacker != null && attacker.isAlive()) {
            this.thorns(world, attacker);
         }

         return landed;
      } else {
         return false;
      }
   }

   private void thorns(ServerWorld level, LivingEntity a) {
      int now = this.age;
      Integer last = this.thornsHit.get(a.getId());
      if (last == null || now - last >= 10) {
         this.thornsHit.put(a.getId(), now);
         if (this.thornsHit.size() > 32) {
            this.thornsHit.entrySet().removeIf(e -> now - e.getValue() > 200);
         }

         a.damage(level, this.getDamageSources().thorns(this), this.dmg(5.0F));
         this.effect(a, StatusEffects.POISON, 60, 0);
         Vec3d away = a.getEntityPos().subtract(this.getEntityPos()).getHorizontal();
         if (away.lengthSquared() > 1.0E-4) {
            away = away.normalize().multiply(0.5);
            a.addVelocity(away.x, 0.15, away.z);
            a.knockedBack = true;
         }

         Vfx.claw(level, a.getEntityPos().add(0.0, a.getHeight() * 0.6, 0.0), this.yawTo(a.getX(), a.getZ()), 0.0F, 1.2, THORN);
      }
   }

   @Override
   protected void onPhaseChange(ServerWorld level, int phase) {
      this.clearVanish();
      this.stepGuard = 0;
      this.beaming = false;
      Vfx.chargeUp(level, this.chest(), phase == 1 ? THORN : (phase == 2 ? MOON : ECLIPSE), 28);
      if (phase == 2) {
         this.setCooldown("eclipse", 300);
         this.moonfallTimer = 100;
         Vfx.embers(level, this.local(0.0, 3.3, 0.3), MOON.glow(), 20, 0.6);
      } else if (phase >= 3) {
         this.setCooldown("thornbloom_cataclysm", 400);
         this.setCooldown("wild_hunt", 60);
         this.cageCenter = this.pickCageCenter(level);
         this.cagePending = true;
      }
   }

   @Override
   protected void phaseBurst(ServerWorld level, int phase) {
      Vec3d g = this.feet(level);
      if (phase == 1) {
         BossFx.slam(level, this, g, 7.0, 10.0F, 1.6);
         Vfx.explosion(level, this.chest(), THORN, 3.5);
         Vfx.groundCrack(level, g, 8.0, THORN);
         this.petals(level, this.waist(), 30, 0.8);

         for (int n = 0; n < 4; n++) {
            this.addPatch(level, this.ringPoint(level, g, 7.0 + this.random.nextDouble() * 4.0, Math.toRadians(45 + 90 * n)), 2.5, 240);
         }

         this.summonCourt(level, 2, 2, false);
         Vfx.shake(level, this.getEntityPos(), 40.0, 1.0, 16);
      } else if (phase == 2) {
         BossFx.knockback(level, this, g, 8.0, 1.4);
         Vfx.nova(level, this.chest(), MOON, 10.0);
         Vfx.pillar(level, g, MOON, 24.0, 1.6);
         Vfx.radialFlash(level, this.chest(), 40.0, 16777215, 0.5F);
         this.summonCourt(level, 4, 0, false);
      } else {
         BossFx.slam(level, this, g, 8.0, 14.0F, 2.0);
         Vfx.heavyImpact(level, g, ECLIPSE, 2.0);
         Vfx.impactFrame(level, this.chest(), 40.0, 16759008, 0.8F);
         this.raiseCage(level);
         this.summonCourt(level, 3, 2, false);
      }
   }

   @Override
   protected void onStagger(ServerWorld level) {
      this.staggerDrop = 24;
      this.clearVanish();
      this.stepGuard = 0;
      this.beaming = false;
   }

   @Override
   protected void onEnrage(ServerWorld level) {
      Vfx.auraRing(level, this.feet(level), THORN, 3.0);
      Vfx.nova(level, this.chest(), THORN, 5.0);
      this.summonCourt(level, 0, 1, false);
   }

   @Override
   protected void desperation(ServerWorld level) {
      this.bloomPending = true;
   }

   @Override
   protected void onIntroTick(ServerWorld level, int left) {
      int t = this.introTicks() - 1 - left;
      if (t == 0) {
         Vfx.runeCircle(level, this.feet(level), 5.0, FAE, 140, 0.02F, Vfx.Rune.ARCANE);
         Vfx.beam(level, this.getEntityPos().add(0.0, 28.0, 0.0), this.getEntityPos().add(0.0, 1.0, 0.0), MOON, 1.0, 60);
      }

      if (t < 60 && t % 4 == 0) {
         this.petals(level, this.local(0.0, 1.2, 0.0), 3, 0.25);
      }

      if (t % 20 == 0) {
         Vfx.auraRing(level, this.feet(level), FAE, 2.0);
      }

      if (t == 24) {
         Vfx.wisps(level, this.chest(), MOON, 6, 2.0, 60);
      } else if (t == 60) {
         Vfx.nova(level, this.chest(), FAE, 4.0);
         this.petals(level, this.chest(), 30, 0.7);
         Vfx.flash(level, this.getEntityPos(), 32.0, 16769268, 0.35F);
      } else if (t == 84) {
         Vec3d right = Vec3d.fromPolar(0.0F, this.getYaw() + 90.0F);
         Vfx.streaks(level, this.chest(), right.multiply(2.5), FAE, 8);
         Vfx.streaks(level, this.chest(), right.multiply(-2.5), FAE, 8);
         Vfx.wisps(level, this.chest(), FAE, 8, 2.5, 50);
      } else if (t == 108) {
         Vfx.glint(level, this.eyes(), FAE.core(), 1.0);
      } else if (t == 120) {
         Vec3d g = this.feet(level);
         Vfx.shockwave(level, g, FAE, 6.0);
         BossFx.knockback(level, this, g, 6.0, 1.0);
         Vfx.runeCircle(level, g, 3.0, MOON, 30, 0.08F, Vfx.Rune.ASTRAL);
         Vfx.shake(level, this.getEntityPos(), 40.0, 0.6, 12);
      }
   }

   @Override
   protected void onDeathTick(ServerWorld level, int tick) {
      int t = tick - 1;
      if (t == 0) {
         this.clearHazards();

         for (RealmMob m : this.myMinions(level, 40.0)) {
            this.petals(level, m.getEntityPos().add(0.0, m.getHeight() * 0.5, 0.0), 8, 0.3);
            Vfx.smoke(level, m.getEntityPos().add(0.0, 0.5, 0.0), 16759008, 4, 0.4, 0.8);
            m.discard();
         }

         Vfx.impactFrame(level, this.chest(), 40.0, 16765168, 0.7F);
      }

      Vec3d v = this.getVelocity();
      Vec3d floor = groundBelow(level, this.getX(), this.getY() + 0.5, this.getZ(), 8);
      double dy = floor != null && t < 50 ? floor.y + 0.9 - this.getY() : 0.0;
      this.setVelocity(v.x * 0.5, MathHelper.clamp(dy, -0.09, 0.09), v.z * 0.5);
      if (t >= 6 && t <= 24 && t % 3 == 0) {
         Vfx.sparks(level, this.chest(), FAE.glow(), 6, 0.3, 0.18);
         Vfx.shards(level, this.chest(), 16759008, 6, 0.35);
      }

      if (t == 40) {
         Vfx.impact(level, this.spotGround(level, this.local(0.25, 0.0, 1.35)), FAE, 0.8);
      }

      if (t >= 24 && t <= 52 && t % 4 == 0) {
         this.petals(level, this.local(1.2, 3.0, -0.3), 3, 0.2);
         this.petals(level, this.local(-1.2, 3.0, -0.3), 3, 0.2);
      }

      if (t >= 52 && t <= 80 && t % 6 == 0) {
         Vfx.wisps(level, this.chest(), MOON, 3, 1.2, 40);
      }

      if (t >= 80 && t <= 124 && t % 3 == 0) {
         this.petals(level, this.local(0.0, 1.4, 0.0), 6, 0.35);
         Vfx.smoke(level, this.local(0.0, 1.2, 0.0), 16759008, 3, 0.6, 1.0);
      }

      if (t == 132) {
         Vfx.pillar(level, this.feet(level), MOON, 24.0, 1.4);
         Vfx.nova(level, this.chest(), FAE, 5.0);
         Vfx.radialFlash(level, this.chest(), 40.0, 16771318, 0.5F);
      } else if (t == 138) {
         Vfx.flash(level, this.getEntityPos(), 40.0, 16777215, 0.4F);
      }
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
      super.registerControllers(controllers);

      for (int n = 0; n < 4; n++) {
         this.looks[n] = this.holdAnim("look_" + n);
      }

      controllers.add(new AnimationController("look", 0, test -> test.setAndContinue(this.looks[MathHelper.clamp(this.phase(), 0, 3)])).additiveAnimations());
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

   private static final class Patch {
      final Vec3d c;
      final double r;
      int left;

      Patch(Vec3d c, double r, int left) {
         this.c = c;
         this.r = r;
         this.left = left;
      }
   }
}
