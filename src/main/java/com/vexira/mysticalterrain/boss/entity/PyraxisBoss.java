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
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;

public class PyraxisBoss extends MysticalBoss {
   private static final BossSounds SOUNDS = BossSounds.of("pyraxis");
   private static final Vfx.Palette FIRE = Vfx.Palette.FIRE;
   private static final Vfx.Palette WHITE_HOT = new Vfx.Palette(16777204, 16762976, 2759188).withDebris(Vfx.Debris.ASH);
   private static final Vfx.Palette OBSIDIAN = new Vfx.Palette(14207216, 6965898, 1314840).withDebris(Vfx.Debris.ASH);
   private static final int CRUST = 2761256;
   private static final int ASH = 3813938;
   private static final int[] CALLS = new int[]{3, 4, 5, 5};
   private static final int POOL = 0;
   private static final int SEAM = 1;
   private static final int BURN = 2;
   private static final int STEP = 3;
   private static final int MAX_HAZARDS = 24;
   private static final int MAX_STEPS = 8;
   private static final int MAX_METEORS = 40;
   private static final int MAX_MINIONS = 6;
   private static final RawAnimation LOOK_SUMMON = RawAnimation.begin().thenPlayAndHold("animation.pyraxis.look_summon");
   private static final RawAnimation LOOK_DEATH = RawAnimation.begin().thenPlayAndHold("animation.pyraxis.look_death");
   private static final RawAnimation[] LOOK = new RawAnimation[4];
   private static final RawAnimation[] LOOK_IN = new RawAnimation[4];
   public static final float WIDTH = 2.4F;
   public static final float HEIGHT = 5.0F;
   public static final boolean FIRE_IMMUNE = true;
   private final List<PyraxisBoss.Meteor> meteors = new ArrayList<>();
   private final List<PyraxisBoss.Hazard> hazards = new ArrayList<>();
   private final List<PyraxisBoss.Shot> shots = new ArrayList<>();
   private final Set<Integer> hitIds = new HashSet<>();
   private boolean submerged;
   private int dazeTicks;
   private int carapaceTicks;
   private boolean finalShell;
   private int ventTimer = 200;
   private int rainTimer = 60;
   private int stepTick;
   private int minionTick = -100;
   private int minionCache;
   private Vec3 skidFrom = Vec3.ZERO;
   private @Nullable Vec3 slamAt;
   private @Nullable Vec3 leapAt;
   private @Nullable Vec3 landAt;
   private @Nullable Vec3 diveAt;
   private Vec3 groundPt = Vec3.ZERO;
   private Vec3 chargeDir = new Vec3(0.0, 0.0, 1.0);
   private Vec3 chargePrev = Vec3.ZERO;
   private boolean crashed;
   private float lashYaw;
   private int turnSign = 1;
   private Vec3 execFrom = Vec3.ZERO;
   private Vec3 execDir = new Vec3(0.0, 0.0, 1.0);
   private Vec3 lanceFrom = Vec3.ZERO;
   private Vec3 lanceTo = Vec3.ZERO;
   private Vec3 fissureFrom = Vec3.ZERO;
   private Vec3[] fissureDirs = new Vec3[0];
   private boolean arcActive;
   private Vec3 arcFrom = Vec3.ZERO;
   private Vec3 arcTo = Vec3.ZERO;
   private int arcTick;
   private int arcLen = 1;
   private double arcApex;
   private int lookPhase = -1;
   private RawAnimation look = LOOK[0];

   public static Builder attributes() {
      return MysticalBoss.bossAttributes(1000.0, 16.0, 0.25, 16.0);
   }

   public PyraxisBoss(EntityType<? extends Monster> type, Level level) {
      super(type, level, "pyraxis", BossBarColor.RED);
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
      return 60;
   }

   @Override
   protected float[] phaseThresholds() {
      return new float[]{0.75F, 0.5F, 0.25F};
   }

   @Override
   public float poiseMax() {
      return this.getMaxHealth() * (this.phase() >= 2 ? 0.14F : 0.18F);
   }

   @Override
   public int enrageTicks() {
      return 7200;
   }

   @Override
   protected double runSpeed() {
      return 1.5;
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
   protected boolean immune() {
      return this.submerged;
   }

   @Override
   public int introTicks() {
      return 140;
   }

   @Override
   public int deathTicks() {
      return 150;
   }

   @Override
   public Vfx.Palette palette() {
      return this.pal();
   }

   private Vfx.Palette pal() {
      return this.phase() < 3 && !this.enraged() ? FIRE : WHITE_HOT;
   }

   @Override
   protected void defineActions(List<BossAction> out) {
      this.fists(out);
      this.closers(out);
      this.ranged(out);
      this.area(out);
      this.brand(out);
      this.ultimates(out);
   }

   private void fists(List<BossAction> out) {
      out.add(
         BossAction.of("cinder_jab", 1.0, 0.4)
            .range(0.0, 5.0)
            .cooldown(40)
            .weight(14)
            .tags("melee")
            .lockFacing()
            .telegraph(0.0, 0.4)
            .advance(0.12, 0.38, 0.16)
            .combo("magma_hook", 0.55F)
            .utility((b, t, c) -> b.phase() >= 2 ? 0.6F : 1.0F)
            .start((b, l, t, i) -> {
               this.aim(t, 6.0);
               Vfx.telegraphCone(l, this.local(1.0, 0.0, 0.0), this.getYRot(), 45.0F, 5.0, 8, this.pal());
            })
            .hit((b, l, t, i) -> {
               BossFx.strike(l, this, 5.0, 45.0F, 14.0F, 0.9);
               BossFx.cone(l, this, 5.0, 45.0F, e -> e.igniteForSeconds(2.0F));
               Vfx.impact(l, this.local(3.4, 2.4, 0.9), this.pal(), 0.6);
               Vfx.streaks(l, this.local(1.5, 2.4, 0.9), this.ahead(), this.pal(), 6);
            })
      );
      out.add(
         BossAction.of("magma_hook", 1.3, 0.55)
            .range(0.0, 5.5)
            .cooldown(60)
            .weight(12)
            .tags("melee")
            .lockFacing()
            .telegraph(0.1, 0.55)
            .advance(0.2, 0.5, 0.14)
            .combo("crater_fist_delayed", 0.15F)
            .combo("crater_fist", 0.4F)
            .start((b, l, t, i) -> {
               this.aim(t, 8.0);
               Vfx.telegraphCone(l, this.local(1.0, 0.0, 0.0), this.getYRot(), 80.0F, 5.5, 11, this.pal());
            })
            .hit((b, l, t, i) -> {
               BossFx.strike(l, this, 5.5, 80.0F, 18.0F, 1.4);
               BossFx.cone(l, this, 5.5, 80.0F, e -> e.igniteForSeconds(3.0F));
               Vfx.slashArc(l, this.local(0.0, 3.3, 0.0), this.getYRot(), 0.0F, 0.0F, 5.5, this.pal(), -170.0F);
               Vfx.sparks(l, this.local(3.6, 3.0, 0.4), this.pal().core(), 12, 0.35, 0.18);
            })
      );
      out.add(
         BossAction.of("crater_fist", 1.6, 0.8)
            .range(0.0, 6.0)
            .cooldown(100)
            .weight(10)
            .tags("melee aoe")
            .lockFacing()
            .telegraph(0.0, 0.8)
            .advance(0.35, 0.75, 0.12)
            .utility((b, t, c) -> c.surroundCount >= 2 ? 1.4F : 1.0F)
            .start((b, l, t, i) -> {
               this.aim(t, 10.0);
               this.slamAt = groundAt(l, this.local(3.88, 0.0, 0.0));
               Vfx.telegraphCircle(l, this.slamAt, 3.8, 16, this.pal());
            })
            .tick((b, l, t, tick) -> {
               if (tick >= 6 && tick <= 14 && tick % 2 == 0) {
                  Vfx.gather(l, this.local(-1.9, 5.4, 0.0), this.pal(), 3.0, 8);
               }
            })
            .hit((b, l, t, i) -> {
               Vec3 at = this.slamPoint(l, 3.9);
               BossFx.slam(l, this, at, 3.8, 30.0F, 1.6);
               Vfx.heavyImpact(l, at, this.pal(), 1.2);
               Vfx.groundCrack(l, at, 3.5, this.pal());
               if (this.phase() >= 1) {
                  this.addHazard(l, 0, at, at, 2.5, 120);
               }
            })
      );
      out.add(
         BossAction.of("crater_fist_delayed", 2.2, 1.4)
            .range(0.0, 6.5)
            .cooldown(100)
            .weight(0)
            .phase(1)
            .tags("melee aoe")
            .lockFacing()
            .telegraph(0.0, 1.4)
            .advance(0.35, 0.75, 0.12)
            .advance(1.15, 1.35, 0.1)
            .start((b, l, t, i) -> {
               this.aim(t, 14.0);
               this.slamAt = groundAt(l, this.local(4.38, 0.0, 0.0));
               Vfx.telegraphCircle(l, this.slamAt, 4.2, 28, this.pal());
            })
            .tick((b, l, t, tick) -> {
               if (tick >= 6 && tick <= 14 && tick % 2 == 0) {
                  Vfx.gather(l, this.local(-1.9, 5.4, 0.0), this.pal(), 3.0, 8);
               } else if (tick == 16) {
                  for (int side = -1; side <= 1; side += 2) {
                     Vec3 fist = this.local(-1.1, 5.8, 1.65 * side);
                     Vfx.flare(l, fist, 16777215, 1.6, 4);
                     Vfx.glint(l, fist, 16777215, 1.4);
                     Vfx.sparks(l, fist, this.pal().core(), 10, 0.3, 0.15);
                  }

                  Vfx.shake(l, this.position(), 16.0, 0.2, 4);
               } else if (tick >= 18 && tick <= 26 && tick % 2 == 0) {
                  Vfx.gather(l, this.local(-2.2, 5.1, 0.0), this.pal(), 3.5, 10);
               }
            })
            .hit((b, l, t, i) -> {
               Vec3 at = this.slamPoint(l, 4.4);
               BossFx.slam(l, this, at, 4.2, 34.0F, 1.8);
               Vfx.heavyImpact(l, at, this.pal(), 1.4);
               Vfx.groundCrack(l, at, 4.0, this.pal());
               this.addHazard(l, 0, at, at, 3.0, 140);
            })
      );
      out.add(
         BossAction.of("tail_lash", 1.2, 0.5)
            .range(0.0, 7.0)
            .cooldown(70)
            .weight(16)
            .tags("melee behind")
            .lockFacing()
            .when((b, t) -> b.ctx().targetBehind)
            .telegraph(0.0, 0.5)
            .combo("magma_hook", 0.35F)
            .start((b, l, t, i) -> {
               this.lashYaw = this.getYRot();
               Vec3 right = Vec3.directionFromRotation(0.0F, this.lashYaw + 90.0F);
               this.turnSign = t != null && !((t.getX() - this.getX()) * right.x + (t.getZ() - this.getZ()) * right.z >= 0.0) ? -1 : 1;
               Vfx.telegraphCone(l, this.position(), this.lashYaw + 180.0F, 100.0F, 7.0, 10, this.pal());
            })
            .tick((b, l, t, tick) -> {
               if (tick >= 11 && tick <= 18) {
                  this.turnTo(this.getYRot() + 22.5F * this.turnSign);
               }
            })
            .hit((b, l, t, i) -> {
               Vec3 rear = Vec3.directionFromRotation(0.0F, this.lashYaw + 180.0F);
               Vec3 left = Vec3.directionFromRotation(0.0F, this.lashYaw - 90.0F);
               DamageSource src = this.damageSources().mobAttack(this);

               for (LivingEntity e : BossFx.victims(l, this, this.position(), 7.0)) {
                  Vec3 to = new Vec3(e.getX() - this.getX(), 0.0, e.getZ() - this.getZ());
                  if ((!(to.lengthSqr() > 1.0) || !(to.normalize().dot(rear) < -0.174)) && e.hurtServer(l, src, this.dmg(20.0F))) {
                     e.push(left.x * 1.8, 0.5, left.z * 1.8);
                     e.syncVelocity = true;
                     e.igniteForSeconds(3.0F);
                     Vfx.impact(l, e.position().add(0.0, e.getBbHeight() * 0.5, 0.0), this.pal(), 0.6);
                  }
               }

               Vfx.slashArc(l, this.local(0.0, 1.4, 0.0), this.lashYaw + 180.0F, 10.0F, 0.0F, 7.0, this.pal(), 200.0F);
               Vfx.debris(l, this.local(-4.0, 0.3, 0.0), Vfx.Debris.ASH, this.pal().glow(), 12, 0.3);
            })
      );
      out.add(
         BossAction.of("anvil_breaker", 1.8, 0.95)
            .range(0.0, 5.5)
            .cooldown(160)
            .weight(7)
            .tags("melee guard_break")
            .superArmor()
            .lockFacing()
            .telegraph(0.0, 0.95)
            .advance(0.55, 0.9, 0.1)
            .utility((b, t, c) -> c.targetBlocking ? 1.5F : 0.6F)
            .start((b, l, t, i) -> {
               this.aim(t, 10.0);
               this.slamAt = groundAt(l, this.local(4.05, 0.0, 0.0));
               Vfx.telegraphCircle(l, this.slamAt, 3.2, 19, this.pal());
               Vfx.chargeUp(l, this.local(-0.4, 5.8, 0.0), this.pal(), 19);
            })
            .hit((b, l, t, i) -> {
               Vec3 at = this.slamPoint(l, 4.05);
               List<LivingEntity> struck = BossFx.victims(l, this, at, 3.2);

               for (LivingEntity e : struck) {
                  if (e.isBlocking()) {
                     breakGuard(l, e, 5.0F);
                  }
               }

               BossFx.slam(l, this, at, 3.2, 28.0F, 1.2);

               for (LivingEntity e : struck) {
                  if (e.isAlive()) {
                     e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1), this);
                  }
               }

               Vfx.heavyImpact(l, at, this.pal(), 1.3);
               Vfx.groundCrack(l, at, 3.0, this.pal());
            })
      );
   }

   private void closers(List<BossAction> out) {
      out.add(
         BossAction.of("caldera_leap", 2.4, 0.6, 1.7)
            .range(7.0, 28.0)
            .cooldown(180)
            .weight(9)
            .tags("gap_closer aoe mobility")
            .superArmor()
            .lockFacing()
            .telegraph(0.0, 1.7)
            .combo("hellfire_ring", 0.25F)
            .utility((b, t, c) -> c.targetFlying ? 0.2F : (c.targetUnreachable ? 1.5F : 1.0F))
            .start((b, l, t, i) -> {
               this.aim(t, 34.0);
               this.leapAt = null;
            })
            .tick((b, l, t, tick) -> {
               if (tick >= 4 && tick <= 10 && (tick - 4) % 3 == 0) {
                  Vfx.gather(l, this.local(0.0, 1.0, 0.0), this.pal(), 3.0, 8);
               }

               if (tick >= 13 && tick <= 33) {
                  this.tickArc();
                  if (tick % 2 == 0) {
                     Vfx.trail(l, this.local(0.0, 2.5, 0.0), this.pal(), 1.4);
                  }
               }
            })
            .hit((b, l, t, i) -> {
               if (i == 0) {
                  Vec3 to = this.leapTarget(l, t);
                  Vec3 from = this.position();
                  this.leapAt = to;
                  this.faceAt(to);
                  this.startArc(to, 22, 7.0);
                  Vfx.telegraphCircle(l, to, 5.0, 22, this.pal());
                  Vfx.heavyImpact(l, from, this.pal(), 0.6);
                  Vfx.dashTrail(l, from.add(0.0, 0.5, 0.0), from.add(0.0, 4.5, 0.0).add(flat(to.subtract(from)).scale(2.0)), this.pal());
               } else {
                  this.endArc(l, true);
                  Vec3 at = this.leapAt != null ? this.leapAt : this.position();
                  BossFx.slam(l, this, at, 5.0, 36.0F, 1.8);
                  Vfx.heavyImpact(l, at, this.pal(), 1.6);
                  Vfx.groundCrack(l, at, 5.0, this.pal());
                  if (this.phase() >= 1) {
                     this.addHazard(l, 0, at, at, 3.5, 160);
                  }
               }
            })
            .end((b, l, t, i) -> {
               if (i == -2) {
                  this.endArc(l, false);
               }
            })
      );
      out.add(
         BossAction.of("molten_charge", 2.4, 1.5)
            .range(8.0, 26.0)
            .cooldown(200)
            .weight(8)
            .phase(1)
            .tags("gap_closer melee punish")
            .superArmor()
            .lockFacing()
            .telegraph(0.0, 0.6)
            .advance(0.6, 1.4, 0.9)
            .combo("anvil_breaker", 0.3F)
            .utility((b, t, c) -> !c.targetUnreachable && !c.targetFlying ? (c.kitingTicks > 60 ? 1.5F : 1.0F) : 0.0F)
            .start((b, l, t, i) -> {
               this.aim(t, 18.0);
               this.chargeDir = this.ahead();
               this.chargePrev = this.position();
               this.hitIds.clear();
               this.crashed = false;
               Vfx.telegraphLine(l, this.position(), this.chargeDir, 15.0, 4.0, 12, this.pal());
            })
            .tick((b, l, t, tick) -> {
               if (tick == 6) {
                  Vfx.groundCrack(l, this.local(0.5, 0.0, -0.6), 1.5, this.pal());
               }

               if (tick >= 12 && tick <= 28) {
                  this.chargeContact(l);
                  if (tick % 2 == 0) {
                     Vfx.dashTrail(l, this.chargePrev.add(0.0, 1.5, 0.0), this.position().add(0.0, 1.5, 0.0), this.pal());
                     Vfx.streaks(l, this.local(1.5, 2.0, 0.0), this.chargeDir.scale(-1.0), this.pal(), 6);
                     this.chargePrev = this.position();
                  }

                  if (!this.crashed && tick >= 14 && this.horizontalCollision) {
                     this.crashed = true;
                     Vfx.heavyImpact(l, this.local(1.8, 1.5, 0.0), this.pal(), 1.2);
                     BossFx.slam(l, this, this.local(1.8, 0.0, 0.0), 3.0, 20.0F, 1.2);
                     this.daze(l, 50, 1.3F);
                  }
               }
            })
            .hit((b, l, t, i) -> {
               BossFx.strike(l, this, 4.5, 60.0F, 28.0F, 2.0);
               BossFx.launch(l, this, this.local(2.0, 0.0, 0.0), 3.0, 0.9);
               Vfx.slashArc(l, this.local(0.0, 4.5, 0.0), this.getYRot(), -20.0F, -90.0F, 4.5, this.pal(), 120.0F);
               Vfx.heavyImpact(l, this.local(2.5, 1.0, 0.0), this.pal(), 0.9);
            })
      );
      out.add(
         BossAction.of("magma_dive", 2.8, 0.9, 2.0)
            .range(5.0, 24.0)
            .cooldown(240)
            .weight(6)
            .phase(1)
            .tags("mobility gap_closer")
            .superArmor()
            .lockFacing()
            .telegraph(0.9, 2.0)
            .combo("cinder_jab", 0.35F)
            .utility((b, t, c) -> c.surroundCount >= 3 ? 2.5F : (c.targetUnreachable ? 0.3F : 1.0F))
            .start((b, l, t, i) -> {
               this.diveAt = null;
               Vfx.groundCrack(l, this.position(), 3.5, this.pal());
            })
            .tick((b, l, t, tick) -> {
               if (tick >= 2 && tick <= 18 && tick % 3 == 2) {
                  Vfx.embers(l, this.position().add(0.0, 0.3, 0.0), this.pal().glow(), 6, 1.6);
                  Vfx.smoke(l, this.position(), this.pal().smoke(), 3, 1.4, 1.2);
               }

               if (tick == 14) {
                  BossFx.knockback(l, this, this.position(), 3.5, 0.9);
                  Vfx.explosion(l, this.position().add(0.0, 0.5, 0.0), this.pal(), 2.0);
               }

               Vec3 at = this.diveAt;
               if (at != null && tick >= 19 && tick <= 39) {
                  if (tick % 4 == 3) {
                     Vfx.embers(l, at.add(0.0, 0.2, 0.0), this.pal().glow(), 5, 1.8);
                  }

                  if (tick == 30) {
                     Vfx.chargeUp(l, at.add(0.0, 0.5, 0.0), this.pal(), 10);
                  }
               }
            })
            .hit((b, l, t, i) -> {
               if (i == 0) {
                  this.submerged = true;
                  Vec3 to = this.diveSpot(l, t);
                  this.diveAt = to;
                  this.getNavigation().stop();
                  this.teleportTo(to.x, to.y, to.z);
                  this.setDeltaMovement(Vec3.ZERO);
                  if (t != null) {
                     this.faceAt(t.position());
                  }

                  Vfx.groundCrack(l, to, 4.0, this.pal());
                  Vfx.telegraphCircle(l, to, 4.0, 22, this.pal());
               } else {
                  this.submerged = false;
                  Vec3 at = this.diveAt != null ? this.diveAt : this.position();
                  List<LivingEntity> burnt = BossFx.victims(l, this, at, 4.0);
                  BossFx.slam(l, this, at, 4.0, 30.0F, 1.0);
                  BossFx.launch(l, this, at, 4.0, 1.2);

                  for (LivingEntity e : burnt) {
                     e.igniteForSeconds(5.0F);
                  }

                  Vfx.pillar(l, at, this.pal(), 9.0, 2.2);
                  Vfx.explosion(l, at.add(0.0, 1.0, 0.0), this.pal(), 3.5);
                  Vfx.heavyImpact(l, at, this.pal(), 1.2);
               }
            })
            .end((b, l, t, i) -> this.submerged = false)
      );
   }

   private void ranged(List<BossAction> out) {
      out.add(
         BossAction.of("meteor_call", 2.6, 0.9, 1.2, 1.5, 1.8, 2.1)
            .range(6.0, 40.0)
            .cooldown(200)
            .weight(8)
            .tags("ranged aoe")
            .telegraph(0.0, 2.1)
            .utility((b, t, c) -> !c.targetAirborne && !c.targetFlying ? 1.0F : 0.3F)
            .start((b, l, t, i) -> {
               Vfx.runeCircle(l, this.position(), 3.5, this.pal(), 52, 0.06F, Vfx.Rune.SOLAR);
               Vfx.chargeUp(l, this.local(0.0, 6.0, 0.0), this.pal(), 18);
            })
            .hit((b, l, t, i) -> {
               if (i < CALLS[Math.min(3, this.phase())]) {
                  Vec3 hand = this.local(0.8, 5.7, i % 2 == 0 ? 0.15 : -0.15);
                  Vfx.flare(l, hand, this.pal().core(), 1.4, 6);
                  Vfx.pillar(l, this.local(0.0, 5.5, 0.0), this.pal(), 10.0, 0.5);
                  LivingEntity v = (LivingEntity)(i % 2 == 1 ? this.randomEngaged() : t);
                  if (v == null) {
                     v = t;
                  }

                  if (v != null) {
                     Vec3 at = groundAt(l, this.predict(v, 24.0));
                     if (i > 0) {
                        at = this.jitter(l, at, 1.2);
                     }

                     this.meteor(l, at, 24, 22.0F, 3.0, this.phase() >= 2);
                     if (this.phase() >= 3) {
                        this.meteor(l, this.jitter(l, at, 2.5), 24, 22.0F, 3.0, true);
                     }
                  }
               }
            })
      );
      out.add(
         BossAction.of("slag_volley", 1.8, 0.55, 0.9, 1.25)
            .range(6.0, 30.0)
            .cooldown(110)
            .weight(10)
            .tags("ranged")
            .telegraph(0.0, 1.25)
            .utility((b, t, c) -> c.dist > 10.0 ? 1.3F : 1.0F)
            .hit((b, l, t, i) -> {
               if (t != null) {
                  Vec3 hand = this.local(1.85, 4.35, i % 2 == 0 ? 1.0 : -1.0);
                  int flight = Mth.clamp((int)Math.round(Math.sqrt(this.distanceToSqr(t)) * 1.1), 12, 30);
                  Vec3 aimAt = groundAt(l, this.predict(t, flight));
                  if (i == 2 && this.phase() >= 2) {
                     Vec3 lead = aimAt.subtract(t.position()).horizontal();
                     if (lead.lengthSqr() < 2.25) {
                        Vec3 away = flat(t.position().subtract(this.position()));
                        lead = new Vec3(-away.z, 0.0, away.x).scale(this.getRandom().nextBoolean() ? 1.5 : -1.5);
                     }

                     aimAt = groundAt(l, t.position().subtract(lead));
                  }

                  this.lob(l, hand, aimAt, flight);
               }
            })
      );
      out.add(
         BossAction.of("rending_fissure", 2.0, 0.9)
            .range(4.0, 24.0)
            .cooldown(150)
            .weight(9)
            .tags("ranged aoe")
            .lockFacing()
            .telegraph(0.0, 0.9)
            .utility((b, t, c) -> !c.targetAirborne && !(c.heightDiff > 3.0) ? 1.0F : 0.2F)
            .start((b, l, t, i) -> {
               this.aim(t, 18.0);
               int ph = this.phase();
               float[] offs = ph >= 3 ? new float[]{-36.0F, -18.0F, 0.0F, 18.0F, 36.0F} : (ph >= 1 ? new float[]{-22.0F, 0.0F, 22.0F} : new float[]{0.0F});
               this.fissureFrom = groundAt(l, this.local(1.5, 0.0, 0.0));
               this.fissureDirs = new Vec3[offs.length];

               for (int k = 0; k < offs.length; k++) {
                  this.fissureDirs[k] = Vec3.directionFromRotation(0.0F, this.getYRot() + offs[k]);
                  Vfx.telegraphLine(l, this.fissureFrom, this.fissureDirs[k], 22.0, 2.4, 18, this.pal());
               }

               this.hitIds.clear();
            })
            .tick((b, l, t, tick) -> {
               if (tick >= 19 && tick <= 32) {
                  int k = tick - 18;
                  DamageSource src = this.damageSources().mobAttack(this);

                  for (Vec3 dir : this.fissureDirs) {
                     Vec3 p = groundAt(l, this.fissureFrom.add(dir.scale(1.5 * k)));
                     Vfx.impact(l, p, this.pal(), 0.5);
                     if (k % 2 == 0) {
                        Vfx.pillar(l, p, this.pal(), 3.5, 0.8);
                     }

                     for (LivingEntity e : BossFx.victims(l, this, p, 1.4)) {
                        if (this.hitIds.add(e.getId()) && e.hurtServer(l, src, this.dmg(26.0F))) {
                           e.push(0.0, 0.8, 0.0);
                           e.syncVelocity = true;
                           e.igniteForSeconds(4.0F);
                        }
                     }
                  }

                  if (tick == 32 && this.phase() >= 1) {
                     for (Vec3 dir : this.fissureDirs) {
                        this.addHazard(l, 1, this.fissureFrom, groundAt(l, this.fissureFrom.add(dir.scale(22.0))), 0.8, 100);
                     }
                  }
               }
            })
            .hit((b, l, t, i) -> {
               Vec3 foot = this.local(0.4, 0.0, -0.55);
               Vfx.heavyImpact(l, foot, this.pal(), 1.0);
               Vfx.groundCrack(l, foot, 2.5, this.pal());
            })
      );
      out.add(
         BossAction.of("furnace_breath", 2.8, 0.9, 1.2, 1.5, 1.8, 2.1)
            .range(2.0, 12.0)
            .cooldown(160)
            .weight(9)
            .tags("ranged aoe")
            .lockFacing()
            .telegraph(0.0, 0.9)
            .utility((b, t, c) -> c.targetBlocking ? 1.3F : 1.0F)
            .start((b, l, t, i) -> {
               this.aim(t, 0.0);
               Vfx.telegraphCone(l, this.position(), this.getYRot(), 35.0F, 12.0, 18, this.pal());
               Vfx.chargeUp(l, this.mouth(), this.pal(), 16);
            })
            .tick((b, l, t, tick) -> {
               if (tick >= 16 && tick <= 46) {
                  if (t != null) {
                     this.turnTo(Mth.approachDegrees(this.getYRot(), this.yawTo(t.position()), 2.5F));
                  }

                  Vec3 mouth = this.mouth();
                  Vec3 dir = Vec3.directionFromRotation(20.0F, this.getYRot());
                  Vfx.streaks(l, mouth, dir, this.pal(), 4);
                  if (tick % 2 == 0) {
                     for (int k = 1; k <= 3; k++) {
                        Vfx.embers(l, mouth.add(dir.scale(3 * k)), this.pal().core(), 3, 0.3 * k);
                     }

                     Vfx.smoke(l, mouth.add(dir.scale(11.0)), this.pal().smoke(), 3, 1.2, 1.4);
                  }

                  if (this.phase() >= 2 && tick % 6 == 0) {
                     Vec3 p = groundAt(l, this.position().add(flat(dir).scale(8.0)));
                     this.addHazard(l, 2, p, p, 1.5, 60);
                  }
               }
            })
            .hit((b, l, t, i) -> {
               DamageSource magic = this.damageSources().indirectMagic(this, this);
               BossFx.cone(l, this, 12.0, 30.0F, e -> {
                  e.hurtServer(l, magic, this.dmg(9.0F));
                  e.igniteForSeconds(5.0F);
               });
            })
      );
      out.add(
         BossAction.of("searing_lance", 1.2, 0.55)
            .range(5.0, 32.0)
            .cooldown(100)
            .weight(5)
            .tags("ranged punish")
            .lockFacing()
            .telegraph(0.0, 0.55)
            .utility((b, t, c) -> this.punishTarget() != null ? 1.2F : 0.35F)
            .start((b, l, t, i) -> {
               ServerPlayer punish = this.punishTarget();
               LivingEntity v = (LivingEntity)(punish != null ? punish : t);
               if (v == null) {
                  this.lanceFrom = this.position();
                  this.lanceTo = this.position();
               } else {
                  this.lanceTo = this.predict(v, 11.0).add(0.0, v.getBbHeight() * 0.6, 0.0);
                  this.faceAt(this.lanceTo);
                  this.lanceFrom = this.local(2.3, 4.6, -0.2);
                  Vec3 d = this.lanceTo.subtract(this.lanceFrom);
                  Vfx.telegraphLine(l, groundAt(l, this.lanceFrom), flat(d), d.horizontalDistance() + 2.0, 1.2, 11, this.pal());
                  Vfx.chargeUp(l, this.core(), this.pal(), 11);
               }
            })
            .hit(
               (b, l, t, i) -> {
                  Vec3 d = this.lanceTo.subtract(this.lanceFrom);
                  if (!(d.lengthSqr() < 1.0E-4)) {
                     Vec3 dir = d.normalize();
                     Vec3 end = l.clip(
                           new ClipContext(this.lanceFrom, this.lanceFrom.add(dir.scale(d.length() + 4.0)), Block.COLLIDER, Fluid.NONE, this)
                        )
                        .getLocation();
                     Vfx.beam(l, this.lanceFrom, end, this.pal(), 0.55, 8);
                     Vfx.impact(l, end, this.pal(), 0.8);
                     DamageSource magic = this.damageSources().indirectMagic(this, this);
                     double reach = this.lanceFrom.distanceTo(end) * 0.5 + 2.0;

                     for (LivingEntity e : BossFx.victims(l, this, this.lanceFrom.lerp(end, 0.5), reach)) {
                        if (crosses(e, this.lanceFrom, end, 0.4)) {
                           boolean using = e.isUsingItem();
                           if (e.hurtServer(l, magic, this.dmg(20.0F))) {
                              if (using) {
                                 e.stopUsingItem();
                                 if (e instanceof ServerPlayer p) {
                                    this.markTarget(p, 100);
                                 }
                              }

                              e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1), this);
                              e.igniteForSeconds(4.0F);
                           }
                        }
                     }
                  }
               }
            )
      );
      out.add(
         BossAction.of("pyroclast_flak", 1.6, 0.6, 0.8, 1.0, 1.2)
            .range(0.0, 40.0)
            .cooldown(120)
            .weight(4)
            .tags("ranged anti_air")
            .telegraph(0.0, 0.6)
            .utility((b, t, c) -> !c.targetFlying && !c.targetAirborne ? 0.1F : 1.0F)
            .start((b, l, t, i) -> Vfx.chargeUp(l, this.local(0.0, 5.5, 0.0), this.pal(), 12))
            .hit((b, l, t, i) -> {
               if (t != null) {
                  Vec3 hand = this.local(1.0, 5.3, i % 2 == 0 ? 1.25 : -1.25);
                  Vec3 aimAt = this.predict(t, 12.0).add(0.0, t.getBbHeight() * 0.5, 0.0);
                  Vec3 d = aimAt.subtract(hand);
                  Vec3 dir = d.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 1.0, 0.0) : d.normalize();
                  BossBolt bolt = BossBolt.shoot(l, this, BossBolt.Style.METEOR, hand, dir.scale(1.3), this.dmg(12.0F)).homing(t, 0.12).aoe(2.5).life(60);
                  this.shots.add(new PyraxisBoss.Shot(bolt, true, 2.5));
                  Vfx.flare(l, hand, this.pal().core(), 1.2, 5);
                  Vfx.streaks(l, hand, dir, this.pal(), 5);
               }
            })
      );
   }

   private void area(List<BossAction> out) {
      out.add(
         BossAction.of("hellfire_ring", 2.6, 1.3)
            .range(0.0, 14.0)
            .cooldown(240)
            .weight(8)
            .phase(1)
            .tags("aoe")
            .superArmor()
            .anyTarget()
            .telegraph(0.0, 1.3)
            .utility((b, t, c) -> c.surroundCount >= 2 ? 1.5F : 1.0F)
            .start((b, l, t, i) -> {
               Vfx.telegraphCircle(l, this.position(), 16.0, 26, this.pal());
               Vfx.runeCircle(l, this.position(), 4.0, this.pal(), 26, 0.08F, Vfx.Rune.SOLAR);
            })
            .tick((b, l, t, tick) -> {
               if (tick >= 5 && tick <= 21 && tick % 2 == 1) {
                  Vfx.slashArc(l, this.local(0.0, 2.5, 0.0), this.getYRot() + tick * 22, 0.0F, 3.5, this.pal(), 90.0F);
               }
            })
            .hit((b, l, t, i) -> {
               Vec3 c = this.position();
               Vfx.nova(l, c, this.pal(), 5.0);
               Vfx.heavyImpact(l, c, this.pal(), 1.0);
               BossFx.wave(l, this, c, 16.0, 13, 30.0F, ParticleTypes.FLAME, 1.2, true);
               if (this.phase() >= 2) {
                  Vfx.telegraphCircle(l, c, 16.0, 12, this.pal());
                  this.schedule(12, () -> BossFx.wave(l, this, c, 16.0, 13, 28.0F, ParticleTypes.FLAME, 1.2, true));
               }

               if (this.phase() >= 3) {
                  this.schedule(12, () -> Vfx.telegraphCircle(l, c, 16.0, 12, this.pal()));
                  this.schedule(24, () -> BossFx.wave(l, this, c, 16.0, 13, 26.0F, ParticleTypes.FLAME, 1.2, true));
               }
            })
      );
      out.add(
         BossAction.of("obsidian_carapace", 2.0, 1.0)
            .range(0.0, 40.0)
            .cooldown(600)
            .weight(5)
            .anyTarget()
            .tags("defensive")
            .when((b, t) -> !b.warded())
            .utility((b, t, c) -> c.recentDamage > 0.04F ? 1.5F : 0.6F)
            .start((b, l, t, i) -> {
               Vfx.gather(l, this.local(0.7, 3.6, 0.0), this.pal(), 4.0, 20);
               Vfx.runeCircle(l, this.position(), 3.5, this.pal(), 20, -0.05F, Vfx.Rune.SOLAR);
            })
            .hit((b, l, t, i) -> {
               this.shield(this.getMaxHealth() * 0.12F, 200);
               this.carapaceTicks = 200;
               Vfx.shieldBubble(l, this.position().add(0.0, 2.5, 0.0), 3.4, OBSIDIAN, 200);
               BossFx.knockback(l, this, this.position(), 4.5, 1.4);
               Vfx.auraRing(l, this.position(), this.pal(), 3.5);
               Vfx.shards(l, this.local(0.6, 3.6, 0.0), 2761256, 20, 0.3);
            })
      );
      out.add(
         BossAction.of("forge_call", 2.4, 1.2)
            .range(0.0, 40.0)
            .cooldown(700)
            .weight(5)
            .phase(1)
            .anyTarget()
            .tags("summon")
            .when((b, t) -> b.level() instanceof ServerLevel sl && this.minionCount(sl) < 5)
            .start((b, l, t, i) -> Vfx.chargeUp(l, this.local(0.8, 3.8, 0.0), this.pal(), 24))
            .hit((b, l, t, i) -> {
               BossFx.knockback(l, this, this.position(), 5.0, 1.3);
               Vfx.nova(l, this.position(), this.pal(), 6.0);
               this.summonAdds(l, 1 + this.phase(), Math.max(0, this.phase() - 1));
            })
      );
   }

   private void brand(List<BossAction> out) {
      out.add(
         BossAction.of("brand_cleave", 2.0, 0.75, 1.35)
            .range(0.0, 7.5)
            .cooldown(110)
            .weight(13)
            .phase(2)
            .tags("melee")
            .lockFacing()
            .telegraph(0.0, 1.35)
            .advance(0.45, 0.72, 0.2)
            .advance(1.1, 1.3, 0.14)
            .combo("brand_execution", 0.35F)
            .start((b, l, t, i) -> {
               this.aim(t, 15.0);
               Vec3 core = this.core();
               Vfx.flare(l, core, this.pal().core(), 2.0, 6);
               Vfx.sparks(l, core, this.pal().glow(), 12, 0.3, 0.16);
               Vfx.telegraphCone(l, this.local(1.2, 0.0, 0.0), this.getYRot(), 85.0F, 7.5, 15, this.pal());
            })
            .tick((b, l, t, tick) -> {
               if (tick == 32) {
                  Vec3 hand = this.local(1.15, 4.0, -2.6);
                  Vfx.sparks(l, hand, this.pal().glow(), 10, 0.25, 0.15);
                  Vfx.embers(l, hand, this.pal().core(), 8, 0.8);
               }
            })
            .hit((b, l, t, i) -> {
               if (i == 0) {
                  BossFx.strike(l, this, 7.5, 85.0F, 26.0F, 1.4);
                  BossFx.cone(l, this, 7.5, 85.0F, e -> e.igniteForSeconds(4.0F));
                  Vfx.slashArc(l, this.local(0.0, 3.2, 0.0), this.getYRot(), 0.0F, 0.0F, 7.5, this.pal(), -200.0F);
                  if (t != null) {
                     this.turnTo(Mth.approachDegrees(this.getYRot(), this.yawTo(this.predict(t, 12.0)), 40.0F));
                  }

                  Vfx.telegraphCone(l, this.local(0.7, 0.0, 0.0), this.getYRot(), 85.0F, 7.5, 12, this.pal());
               } else {
                  BossFx.strike(l, this, 7.5, 85.0F, 24.0F, 1.6);
                  BossFx.cone(l, this, 7.5, 85.0F, e -> e.igniteForSeconds(4.0F));
                  Vfx.slashArc(l, this.local(0.0, 3.0, 0.0), this.getYRot(), -10.0F, -35.0F, 7.5, this.pal(), 200.0F);
               }
            })
      );
      out.add(
         BossAction.of("brand_execution", 2.4, 1.1)
            .range(0.0, 16.0)
            .cooldown(180)
            .weight(9)
            .phase(2)
            .tags("melee aoe")
            .superArmor()
            .lockFacing()
            .telegraph(0.0, 1.1)
            .utility((b, t, c) -> c.targetBlocking ? 1.3F : 1.0F)
            .start((b, l, t, i) -> {
               this.aim(t, 22.0);
               this.execFrom = this.position();
               this.execDir = this.ahead();
               Vfx.telegraphLine(l, this.execFrom, this.execDir, 18.0, 3.0, 22, this.pal());
               Vfx.chargeUp(l, this.local(0.2, 7.0, 0.0), this.pal(), 22);
            })
            .hit(
               (b, l, t, i) -> {
                  Vec3 at = groundAt(l, this.execFrom.add(this.execDir.scale(5.0)));
                  List<LivingEntity> slammed = BossFx.victims(l, this, at, 3.0);
                  BossFx.slam(l, this, at, 3.0, 42.0F, 1.4);
                  Vec3 a = this.execFrom.add(this.execDir.scale(2.0));
                  Vec3 z = this.execFrom.add(this.execDir.scale(18.0));
                  DamageSource src = this.damageSources().mobAttack(this);

                  for (LivingEntity e : BossFx.victims(l, this, this.execFrom.add(this.execDir.scale(10.0)), 10.0)) {
                     if (!slammed.contains(e)
                        && !(Math.abs(e.getY() - this.execFrom.y) > 3.0)
                        && !(segDistSqr(e.getX(), e.getZ(), a, z) > 2.25)
                        && e.hurtServer(l, src, this.dmg(34.0F))) {
                        e.push(0.0, 1.0, 0.0);
                        e.syncVelocity = true;
                        e.igniteForSeconds(5.0F);
                     }
                  }

                  Vfx.heavyImpact(l, at, this.pal(), 1.5);
                  Vfx.groundCrack(l, at, 3.5, this.pal());

                  for (int k = 0; k < 8; k++) {
                     Vec3 p = groundAt(l, this.execFrom.add(this.execDir.scale(4 + 2 * k)));
                     this.schedule(k, () -> Vfx.pillar(l, p, this.pal(), 5.0, 1.0));
                  }

                  if (this.phase() >= 3) {
                     this.addHazard(l, 1, groundAt(l, a), groundAt(l, z), 0.8, 120);
                  }
               }
            )
      );
   }

   private void ultimates(List<BossAction> out) {
      out.add(
         BossAction.of("cataclysm", 6.0, 1.0, 2.0, 2.5, 3.0, 3.5, 4.6)
            .range(0.0, 32.0)
            .cooldown(900)
            .weight(6)
            .phase(2)
            .anyTarget()
            .tags("ultimate aoe ranged")
            .superArmor()
            .lockFacing()
            .telegraph(0.0, 4.6)
            .utility((b, t, c) -> c.engagedPlayers >= 2 ? 1.3F : 1.0F)
            .start((b, l, t, i) -> {
               this.groundPt = this.position();
               this.landAt = null;
               Vfx.chargeUp(l, this.core(), WHITE_HOT, 20);
               Vfx.runeCircle(l, this.position(), 6.0, this.pal(), 120, 0.05F, Vfx.Rune.SOLAR);
               Vfx.telegraphCircle(l, this.position(), 6.0, 20, this.pal());
            })
            .tick((b, l, t, tick) -> {
               if (tick >= 21 && tick <= 83) {
                  this.tickArc();
                  if (tick % 4 == 0) {
                     Vfx.embers(l, this.position().add(0.0, 2.5, 0.0), this.pal().core(), 6, 1.4);
                     Vfx.trail(l, this.position().add(0.0, 2.5, 0.0), this.pal(), 1.6);
                  }
               } else if (tick == 84) {
                  Vec3 to = this.landAt != null ? this.landAt : this.groundPt;
                  this.faceAt(to);
                  this.startArc(to, 8, 0.0);
               } else if (tick >= 85 && tick <= 91) {
                  this.tickArc();
                  Vfx.trail(l, this.position().add(0.0, 2.5, 0.0), this.pal(), 1.8);
               }
            })
            .hit((b, l, t, i) -> {
               switch (i) {
                  case 0:
                     Vec3 c = this.position();
                     BossFx.slam(l, this, c, 6.0, 20.0F, 2.0);
                     Vfx.heavyImpact(l, c, this.pal(), 1.2);
                     this.groundPt = c;
                     int up = 0;
                     int dy = 1;

                     while (dy <= 12 && l.noCollision(this, this.getBoundingBox().move(0.0, dy, 0.0))) {
                        up = dy++;
                     }

                     this.startArc(c.add(0.0, up, 0.0), 16, 0.0);
                     break;
                  case 5:
                     this.endArc(l, true);
                     Vec3 at = this.landAt != null ? this.landAt : this.position();
                     BossFx.slam(l, this, at, 9.0, 58.0F, 2.6);
                     Vfx.heavyImpact(l, at, this.pal(), 2.5);
                     Vfx.impactFrame(l, at, 24.0, this.pal().core(), 0.8F);
                     Vfx.groundCrack(l, at, 8.0, this.pal());
                     Vfx.nova(l, at, this.pal(), 9.0);
                     this.addHazard(l, 0, at, at, 4.0, 160);
                     break;
                  default:
                     for (ServerPlayer p : this.engaged()) {
                        this.meteor(l, groundAt(l, this.predict(p, 20.0)), 20, 26.0F, 3.5, true);
                     }

                     int extra = this.phase() >= 3 ? 3 : 2;

                     for (int k = 0; k < extra; k++) {
                        this.meteor(l, BossFx.around(l, this, this.groundPt, 2.0, 14.0), 20, 26.0F, 3.5, true);
                     }

                     Vfx.flare(l, this.core(), this.pal().core(), 3.0, 6);
                     if (i == 4) {
                        Vec3 to = t != null ? groundAt(l, this.predict(t, 22.0)) : this.groundPt;
                        Vec3 d = to.subtract(this.groundPt);
                        if (d.horizontalDistance() > 20.0) {
                           to = groundAt(l, this.groundPt.add(flat(d).scale(20.0)));
                        }

                        if (!BossFx.safeGround(l, to, 4.0)) {
                           to = this.groundPt;
                        }

                        this.landAt = to;
                        Vfx.telegraphCircle(l, to, 9.0, 22, this.pal());
                     }
               }
            })
            .end((b, l, t, i) -> {
               if (i == -2) {
                  this.endArc(l, false);
               }
            })
      );
      out.add(
         BossAction.of("heart_of_the_volcano", 4.5, 3.4)
            .range(0.0, 18.0)
            .cooldown(800)
            .weight(8)
            .phase(3)
            .anyTarget()
            .tags("ultimate aoe")
            .superArmor()
            .lockFacing()
            .telegraph(0.0, 3.4)
            .start((b, l, t, i) -> {
               Vfx.telegraphCircle(l, this.position(), 16.0, 68, this.pal());
               Vfx.chargeUp(l, this.core(), WHITE_HOT, 68);
            })
            .tick((b, l, t, tick) -> {
               if (tick == 12) {
                  Vfx.vortex(l, this.position(), 16.0, 9.0, this.pal(), 50);
               }

               if (tick >= 12 && tick <= 60 && tick % 2 == 0) {
                  BossFx.pull(l, this, this.position(), 20.0, 0.09);
               }

               if (tick == 60) {
                  Vfx.flash(l, this.position(), 32.0, 16774352, 0.3F);
               }
            })
            .hit((b, l, t, i) -> {
               Vec3 c = this.position();
               List<LivingEntity> burnt = BossFx.victims(l, this, c, 16.0);
               BossFx.slam(l, this, c, 16.0, 64.0F, 2.6);

               for (LivingEntity e : burnt) {
                  e.igniteForSeconds(8.0F);
               }

               Vfx.nova(l, c, WHITE_HOT, 16.0);
               Vfx.heavyImpact(l, c, this.pal(), 3.0);
               Vfx.impactFrame(l, c, 32.0, 16774352, 1.0F);
               Vfx.pillar(l, c, this.pal(), 24.0, 3.0);

               for (int k = 0; k < 8; k++) {
                  double a = k * Math.PI / 4.0;
                  Vec3 p = groundAt(l, c.add(Math.cos(a) * 8.0, 0.0, Math.sin(a) * 8.0));
                  this.addHazard(l, 0, p, p, 2.5, 100);
               }
            })
      );
      out.add(
         BossAction.of("final_eruption", 7.0, 2.0, 3.5, 5.0, 6.0)
            .range(0.0, 64.0)
            .cooldown(12000)
            .weight(0)
            .phase(3)
            .anyTarget()
            .tags("ultimate aoe")
            .superArmor()
            .lockFacing()
            .telegraph(0.0, 6.0)
            .start((b, l, t, i) -> {
               this.dazeTicks = 0;
               this.finalShell = true;
               this.shield(this.getMaxHealth() * 0.06F, 140);
               this.buff(140, 0.6F, 1.0F);
               Vfx.telegraphCircle(l, this.position(), 22.0, 120, WHITE_HOT);
               Vfx.runeCircle(l, this.position(), 8.0, this.pal(), 140, 0.1F, Vfx.Rune.SOLAR);
               Vfx.telegraphCircle(l, this.position(), 8.0, 40, this.pal());
            })
            .tick((b, l, t, tick) -> {
               if (tick < 120 && tick % 8 == 4) {
                  Vfx.gather(l, this.core(), this.pal(), 5.0, 12);
               }
            })
            .hit((b, l, t, i) -> {
               Vec3 c = this.position();
               if (i < 3) {
                  double r = 8 + 2 * i;
                  BossFx.slam(l, this, c, r, 20 + 4 * i, 1.6);
                  Vfx.nova(l, c, this.pal(), r);
                  if (i < 2) {
                     Vfx.telegraphCircle(l, c, r + 2.0, 30, this.pal());
                  }
               } else if (this.finalShell) {
                  List<LivingEntity> burnt = BossFx.victims(l, this, c, 22.0);
                  BossFx.slam(l, this, c, 22.0, 70.0F, 3.0);

                  for (LivingEntity e : burnt) {
                     e.igniteForSeconds(10.0F);
                  }

                  Vfx.impactFrame(l, c, 48.0, 16777215, 1.0F);
                  Vfx.nova(l, c, WHITE_HOT, 22.0);
                  Vfx.heavyImpact(l, c, this.pal(), 3.0);
                  Vfx.pillar(l, c, this.pal(), 30.0, 4.0);

                  for (int k = 0; k < 10; k++) {
                     Vec3 p = BossFx.around(l, this, c, 3.0, 16.0);
                     this.addHazard(l, 0, p, p, 3.0, 200);
                  }
               }
            })
            .end((b, l, t, i) -> this.finalShell = false)
      );
   }

   private Vec3 local(double fwd, double up, double left) {
      Vec3 f = Vec3.directionFromRotation(0.0F, this.getYRot());
      Vec3 l = Vec3.directionFromRotation(0.0F, this.getYRot() - 90.0F);
      return this.position().add(f.x * fwd + l.x * left, up, f.z * fwd + l.z * left);
   }

   private Vec3 ahead() {
      return Vec3.directionFromRotation(0.0F, this.getYRot());
   }

   private Vec3 core() {
      return this.local(1.0, 3.3, 0.0);
   }

   private Vec3 mouth() {
      return this.local(1.8, 3.2, 0.0);
   }

   private static Vec3 groundAt(ServerLevel l, Vec3 p) {
      Vec3 g = BossFx.ground(l, p.x, p.y + 2.0, p.z);
      return g != null ? g : p;
   }

   private static Vec3 flat(Vec3 v) {
      Vec3 h = new Vec3(v.x, 0.0, v.z);
      return h.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : h.normalize();
   }

   private Vec3 jitter(ServerLevel l, Vec3 at, double r) {
      return groundAt(l, at.add((this.getRandom().nextDouble() * 2.0 - 1.0) * r, 0.0, (this.getRandom().nextDouble() * 2.0 - 1.0) * r));
   }

   private Vec3 slamPoint(ServerLevel l, double fwd) {
      return this.slamAt != null ? this.slamAt : groundAt(l, this.local(fwd, 0.0, 0.0));
   }

   private float yawTo(Vec3 p) {
      return (float)(Mth.atan2(p.z - this.getZ(), p.x - this.getX()) * 180.0F / (float)Math.PI) - 90.0F;
   }

   private void turnTo(float yaw) {
      this.setYRot(yaw);
      this.setYBodyRot(yaw);
      this.setYHeadRot(yaw);
   }

   private void faceAt(Vec3 p) {
      double dx = p.x - this.getX();
      double dz = p.z - this.getZ();
      if (dx * dx + dz * dz > 1.0E-4) {
         this.turnTo(this.yawTo(p));
      }
   }

   private void aim(@Nullable LivingEntity t, double lead) {
      if (t != null) {
         this.faceAt(this.predict(t, lead));
      }
   }

   private @Nullable ServerPlayer randomEngaged() {
      List<ServerPlayer> list = this.engaged();
      return list.isEmpty() ? null : list.get(this.getRandom().nextInt(list.size()));
   }

   private @Nullable ServerPlayer punishTarget() {
      ServerPlayer best = null;
      double bestD = 1024.0;

      for (ServerPlayer p : this.engaged()) {
         if (p.isUsingItem()) {
            ItemUseAnimation u = p.getUseItem().getUseAnimation();
            if (u == ItemUseAnimation.EAT || u == ItemUseAnimation.DRINK || u == ItemUseAnimation.BOW || u == ItemUseAnimation.CROSSBOW || u == ItemUseAnimation.SPEAR || u == ItemUseAnimation.TRIDENT) {
               double d = p.distanceToSqr(this);
               if (d < bestD && this.hasLineOfSight(p)) {
                  best = p;
                  bestD = d;
               }
            }
         }
      }

      return best;
   }

   private int minionCount(ServerLevel l) {
      if (this.tickCount - this.minionTick >= 20 || this.minionTick > this.tickCount) {
         this.minionTick = this.tickCount;
         this.minionCache = l.getEntitiesOfClass(RealmMob.class, this.getBoundingBox().inflate(32.0), LivingEntity::isAlive).size();
      }

      return this.minionCache;
   }

   private void summonAdds(ServerLevel l, int imps, int brutes) {
      this.minionTick = -100;
      int room = Math.max(0, 6 - this.minionCount(l));
      int b = Math.min(brutes, room);
      int n = Math.min(imps, room - b);
      if (b > 0) {
         BossFx.summonMinions(l, this, RealmMobs.type("magma_brute"), b, 7.0, null, m -> {});
      }

      if (n > 0) {
         BossFx.summonMinions(l, this, RealmMobs.type("cinder_imp"), n, 9.0, null, m -> {});
      }

      this.minionTick = -100;
   }

   private static void breakGuard(ServerLevel l, LivingEntity e, float seconds) {
      ItemStack s = e.getItemBlockingWith();
      if (s != null) {
         BlocksAttacks blocks = (BlocksAttacks)s.get(DataComponents.BLOCKS_ATTACKS);
         if (blocks != null) {
            blocks.disable(l, e, seconds, s);
         }

         if (e instanceof Player p) {
            p.getCooldowns().addCooldown(s, Math.round(seconds * 20.0F));
         }

         if (e.isUsingItem()) {
            e.stopUsingItem();
         }

         Vfx.shards(l, e.position().add(0.0, 1.2, 0.0), 14211288, 12, 0.4);
         Vfx.glint(l, e.position().add(0.0, 1.4, 0.0), 16777215, 1.2);
      }
   }

   private Vec3 leapTarget(ServerLevel l, @Nullable LivingEntity t) {
      if (t == null) {
         return this.position();
      }

      Vec3 to = groundAt(l, this.predict(t, 22.0));
      Vec3 d = to.subtract(this.position());
      if (d.horizontalDistance() > 26.0) {
         to = groundAt(l, this.position().add(flat(d).scale(26.0)));
      }

      if (!BossFx.safeGround(l, to, 4.0)) {
         to = t.position();
      }

      return to;
   }

   private Vec3 diveSpot(ServerLevel l, @Nullable LivingEntity t) {
      if (this.ctx().surroundCount >= 3) {
         Vec3 sum = Vec3.ZERO;
         int n = 0;

         for (ServerPlayer p : this.engaged()) {
            if (p.distanceToSqr(this) < 64.0) {
               sum = sum.add(p.position());
               n++;
            }
         }

         Vec3 centre = n > 0 ? sum.scale(1.0 / n) : this.position();

         for (int k = 0; k < 6; k++) {
            Vec3 p = BossFx.around(l, this, centre, 10.0, 14.0);
            if (p.distanceToSqr(centre) >= 81.0 && this.fitsAt(l, p)) {
               return p;
            }
         }
      } else if (t != null) {
         Vec3 p = groundAt(l, this.predict(t, 22.0));
         if (this.fitsAt(l, p)) {
            return p;
         }

         for (int k = 0; k < 4; k++) {
            Vec3 q = BossFx.around(l, this, t.position(), 2.0, 4.5);
            if (this.fitsAt(l, q)) {
               return q;
            }
         }
      }

      return this.position();
   }

   private boolean fitsAt(ServerLevel l, Vec3 p) {
      return l.noCollision(this, this.getBoundingBox().move(p.subtract(this.position()))) && BossFx.safeGround(l, p, 3.0);
   }

   private void chargeContact(ServerLevel l) {
      Vec3 head = this.local(1.6, 1.5, 0.0);
      Vec3 side = new Vec3(-this.chargeDir.z, 0.0, this.chargeDir.x);
      DamageSource src = this.damageSources().mobAttack(this);

      for (LivingEntity e : BossFx.victims(l, this, head, 2.4)) {
         if (this.hitIds.add(e.getId()) && e.hurtServer(l, src, this.dmg(24.0F))) {
            double s = (e.getX() - this.getX()) * side.x + (e.getZ() - this.getZ()) * side.z >= 0.0 ? 1.0 : -1.0;
            e.push(side.x * 1.6 * s + this.chargeDir.x * 0.6, 0.6, side.z * 1.6 * s + this.chargeDir.z * 0.6);
            e.syncVelocity = true;
            e.igniteForSeconds(4.0F);
            Vfx.impact(l, e.position().add(0.0, e.getBbHeight() * 0.5, 0.0), this.pal(), 0.8);
         }
      }
   }

   private void lob(ServerLevel l, Vec3 hand, Vec3 aimAt, int ticks) {
      double g = 0.05;
      Vec3 d = aimAt.add(0.0, -0.3, 0.0).subtract(hand);
      Vec3 v0 = new Vec3(d.x / ticks, (d.y + g * ticks * (ticks + 1) / 2.0) / ticks, d.z / ticks);
      BossBolt bolt = BossBolt.shoot(l, this, BossBolt.Style.METEOR, hand, v0, this.dmg(16.0F)).gravity(g).aoe(2.2).life(ticks + 20);
      this.shots.add(new PyraxisBoss.Shot(bolt, false, 2.2));
      Vfx.telegraphCircle(l, aimAt, 2.2, ticks, this.pal());
      Vfx.flare(l, hand, this.pal().core(), 1.3, 5);
      Vfx.sparks(l, hand, this.pal().glow(), 8, 0.25, 0.15);
   }

   private static boolean crosses(LivingEntity e, Vec3 a, Vec3 b, double pad) {
      AABB box = e.getBoundingBox().inflate(pad);
      return box.contains(a) || box.clip(a, b).isPresent();
   }

   private static double segDistSqr(double x, double z, Vec3 a, Vec3 b) {
      double vx = b.x - a.x;
      double vz = b.z - a.z;
      double len = vx * vx + vz * vz;
      double s = len < 1.0E-6 ? 0.0 : Mth.clamp(((x - a.x) * vx + (z - a.z) * vz) / len, 0.0, 1.0);
      double px = a.x + vx * s - x;
      double pz = a.z + vz * s - z;
      return px * px + pz * pz;
   }

   private void startArc(Vec3 to, int ticks, double apex) {
      this.arcFrom = this.position();
      this.arcTo = to;
      this.arcTick = 0;
      this.arcLen = Math.max(1, ticks);
      this.arcApex = apex;
      this.arcActive = true;
      this.setNoGravity(true);
      this.getNavigation().stop();
   }

   private void tickArc() {
      if (this.arcActive) {
         double s = Math.min(1.0, (double)(++this.arcTick) / this.arcLen);
         Vec3 p = this.arcFrom.lerp(this.arcTo, s).add(0.0, this.arcApex * 4.0 * s * (1.0 - s), 0.0);
         Vec3 v = p.subtract(this.position());
         if (v.lengthSqr() > 9.0) {
            v = v.normalize().scale(3.0);
         }

         this.setDeltaMovement(v);
      }
   }

   private void endArc(ServerLevel l, boolean snap) {
      if (this.arcActive) {
         this.arcActive = false;
         this.setNoGravity(false);
         if (snap && l.noCollision(this, this.getBoundingBox().move(this.arcTo.subtract(this.position())))) {
            this.setPos(this.arcTo.x, this.arcTo.y, this.arcTo.z);
         }

         Vec3 v = this.getDeltaMovement();
         this.setDeltaMovement(v.x * 0.2, Math.min(0.0, v.y), v.z * 0.2);
         this.resetFallDistance();
         if (!l.noCollision(this, this.getBoundingBox())) {
            for (int dy = 1; dy <= 4; dy++) {
               if (l.noCollision(this, this.getBoundingBox().move(0.0, dy, 0.0))) {
                  this.setPos(this.getX(), this.getY() + dy, this.getZ());
                  break;
               }
            }
         }
      }
   }

   private void meteor(ServerLevel l, Vec3 at, int fall, float base, double r, boolean crater) {
      if (this.meteors.size() < 40) {
         Vfx.telegraphCircle(l, at, r, fall, this.pal());
         Vec3 from = at.add((this.getRandom().nextDouble() - 0.5) * 12.0, 28.0, (this.getRandom().nextDouble() - 0.5) * 12.0);
         this.meteors.add(new PyraxisBoss.Meteor(from, at, this.tickCount, fall, base, r, crater));
         BossSounds.play(l, from, "bolt.meteor.launch", 2.0F, 0.8F);
      }
   }

   private void tickMeteors(ServerLevel l) {
      if (!this.meteors.isEmpty()) {
         List<PyraxisBoss.Meteor> landed = null;
         boolean crowded = this.meteors.size() > 10;
         Iterator<PyraxisBoss.Meteor> it = this.meteors.iterator();

         while (it.hasNext()) {
            PyraxisBoss.Meteor m = it.next();
            int age = this.tickCount - m.spawn();
            if (age >= m.fall()) {
               it.remove();
               if (landed == null) {
                  landed = new ArrayList<>();
               }

               landed.add(m);
            } else if (!crowded || age % 2 == 0) {
               Vec3 v = m.at().subtract(m.from()).scale(1.0 / m.fall());
               Vfx.meteorTrail(l, m.from().lerp(m.at(), (double)age / m.fall()), v, this.pal());
            }
         }

         if (landed != null) {
            for (PyraxisBoss.Meteor m : landed) {
               this.meteorLands(l, m);
            }
         }
      }
   }

   private void meteorLands(ServerLevel l, PyraxisBoss.Meteor m) {
      BossFx.slam(l, this, m.at(), m.r(), m.dmg(), 0.8);
      Vfx.heavyImpact(l, m.at(), this.pal(), 0.9);
      Vfx.groundCrack(l, m.at(), m.r() * 0.9, this.pal());
      BossSounds.play(l, m.at(), "bolt.meteor.impact", 2.0F, 0.9F);
      if (m.crater()) {
         this.addHazard(l, 2, m.at(), m.at(), 2.0, 100);
      }
   }

   private void tickShots(ServerLevel l) {
      if (!this.shots.isEmpty()) {
         Iterator<PyraxisBoss.Shot> it = this.shots.iterator();

         while (it.hasNext()) {
            PyraxisBoss.Shot s = it.next();
            if (s.bolt.isRemoved()) {
               it.remove();

               for (LivingEntity e : BossFx.victims(l, this, s.last, s.r + 0.8)) {
                  e.igniteForSeconds(4.0F);
               }

               if (!s.flak && this.phase() >= 2) {
                  Vec3 p = groundAt(l, s.last);
                  this.addHazard(l, 2, p, p, 1.5, 60);
               }
            } else {
               s.last = s.bolt.position();
               if (s.flak) {
                  for (ServerPlayer p : this.engaged()) {
                     if (p.isFallFlying() && p.distanceToSqr(s.last) < 9.0) {
                        p.stopFallFlying();
                        p.push(0.0, -0.6, 0.0);
                        p.syncVelocity = true;
                        Vfx.impact(l, p.position().add(0.0, 0.9, 0.0), this.pal(), 0.7);
                     }
                  }
               }
            }
         }
      }
   }

   private void addHazard(ServerLevel l, int kind, Vec3 a, Vec3 b, double r, int ticks) {
      if (kind == 3) {
         int steps = 0;
         int oldest = -1;

         for (int k = 0; k < this.hazards.size(); k++) {
            if (this.hazards.get(k).kind() == 3) {
               steps++;
               if (oldest < 0) {
                  oldest = k;
               }
            }
         }

         if (steps >= 8 && oldest >= 0) {
            this.hazards.remove(oldest);
         }
      }

      if (this.hazards.size() >= 24) {
         this.hazards.remove(0);
      }

      PyraxisBoss.Hazard h = new PyraxisBoss.Hazard(kind, a, b, r, this.tickCount, this.tickCount + ticks);
      this.hazards.add(h);
      this.crack(l, h);
   }

   private void crack(ServerLevel l, PyraxisBoss.Hazard h) {
      if (h.kind() == 1) {
         double len = h.a().distanceTo(h.b());
         int n = Math.max(1, (int)Math.ceil(len / 3.5));

         for (int k = 0; k < n; k++) {
            Vfx.groundCrack(l, groundAt(l, h.a().lerp(h.b(), (k + 0.5) / n)), 1.8, this.pal());
         }
      } else {
         Vfx.groundCrack(l, h.a(), h.r(), this.pal());
      }
   }

   private void tickHazards(ServerLevel l) {
      if (!this.hazards.isEmpty()) {
         int now = this.tickCount;
         this.hazards.removeIf(hx -> hx.until() <= now);
         Vfx.Palette pal = this.pal();
         DamageSource magic = now % 10 == 0 ? this.damageSources().indirectMagic(this, this) : null;

         for (PyraxisBoss.Hazard h : this.hazards) {
            int age = now - h.born();
            if (age > 0 && age % 60 == 0) {
               this.crack(l, h);
            }

            if (age % 20 == 10) {
               switch (h.kind()) {
                  case 0:
                     Vfx.aura(l, h.a(), pal, h.r());
                     break;
                  case 1:
                     for (int k = 0; k < 2; k++) {
                        Vfx.embers(l, h.a().lerp(h.b(), this.getRandom().nextDouble()).add(0.0, 0.2, 0.0), pal.glow(), 3, 0.5);
                     }
                     break;
                  default:
                     Vfx.embers(l, h.a().add(0.0, 0.2, 0.0), pal.glow(), 2, h.r() * 0.6);
               }
            }

            if (magic != null) {
               for (ServerPlayer p : this.engaged()) {
                  if (inside(h, p)) {
                     p.hurtServer(l, magic, this.dmg(2.0F));
                     p.igniteForSeconds(3.0F);
                     if (h.kind() == 0) {
                        p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 0), this);
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean inside(PyraxisBoss.Hazard h, LivingEntity e) {
      if (h.kind() == 1) {
         double vx = h.b().x - h.a().x;
         double vz = h.b().z - h.a().z;
         double len = vx * vx + vz * vz;
         double s = len < 1.0E-6 ? 0.0 : Mth.clamp(((e.getX() - h.a().x) * vx + (e.getZ() - h.a().z) * vz) / len, 0.0, 1.0);
         double dy = e.getY() - (h.a().y + (h.b().y - h.a().y) * s);
         return dy > -1.0 && dy < 1.6 && segDistSqr(e.getX(), e.getZ(), h.a(), h.b()) <= h.r() * h.r();
      } else {
         double dy = e.getY() - h.a().y;
         double dx = e.getX() - h.a().x;
         double dz = e.getZ() - h.a().z;
         return dy > -1.0 && dy < 1.6 && dx * dx + dz * dz <= h.r() * h.r();
      }
   }

   @Override
   protected void customServerAiStep(ServerLevel world) {
      super.customServerAiStep(world);
      if (this.isAlive()) {
         BossFx.current(this);
         this.tickMeteors(world);
         this.tickShots(world);
         this.tickHazards(world);
         if (this.carapaceTicks > 0 && (--this.carapaceTicks <= 0 || this.shield() <= 0.0F)) {
            this.carapaceTicks = 0;
         }

         BossAction cur = this.currentMove();
         if (this.dazeTicks > 0 && cur != null) {
            this.dazeTicks = 0;
         }

         String anim = cur == null ? "" : cur.anim();
         if (this.arcActive && !anim.equals("caldera_leap") && !anim.equals("cataclysm")) {
            this.endArc(world, false);
         }

         if (this.submerged && !anim.equals("magma_dive")) {
            this.submerged = false;
         }

         if (!this.inIntro()) {
            this.ambient(world);
            this.arena(world);
         }
      }
   }

   private void arena(ServerLevel l) {
      BossAction cur = this.currentMove();
      List<ServerPlayer> players = this.engaged();
      if ((cur == null || !cur.anim().startsWith("phase_")) && this.getTarget() != null && !players.isEmpty()) {
         float pace = this.enraged() ? 0.6F : 1.0F;
         if (this.phase() >= 2 && --this.ventTimer <= 0) {
            this.ventTimer = Math.round((160 + this.getRandom().nextInt(61)) * pace);

            for (ServerPlayer p : players) {
               this.vent(l, groundAt(l, this.predict(p, 30.0)));
            }

            if (this.phase() >= 3) {
               this.vent(l, BossFx.around(l, this, this.position(), 3.0, 12.0));
            }
         }

         if ((this.phase() >= 3 || this.enraged()) && --this.rainTimer <= 0) {
            this.rainTimer = this.enraged() ? 40 : 60;
            ServerPlayer p = players.get(this.getRandom().nextInt(players.size()));
            this.meteor(l, groundAt(l, this.predict(p, 24.0)), 24, 16.0F, 2.5, false);
         }
      }
   }

   private void vent(ServerLevel l, Vec3 at) {
      Vfx.telegraphCircle(l, at, 2.2, 30, this.pal());
      Vfx.groundCrack(l, at, 2.0, this.pal());
      this.schedule(30, () -> this.ventBlast(l, at, 2.2, 18.0F));
   }

   private void ventBlast(ServerLevel l, Vec3 at, double r, float base) {
      Vfx.pillar(l, at, this.pal(), 8.0, 1.2);
      Vfx.explosion(l, at.add(0.0, 0.5, 0.0), this.pal(), 2.0);
      List<LivingEntity> burnt = BossFx.victims(l, this, at, r);
      BossFx.hurt(l, this, at, r, base, null);
      BossFx.launch(l, this, at, r, 1.0);

      for (LivingEntity e : burnt) {
         e.igniteForSeconds(4.0F);
      }
   }

   private void ambient(ServerLevel l) {
      int t = this.tickCount;
      int p = this.phase();
      Vfx.Palette pal = this.pal();
      if (t % 10 == 0) {
         Vfx.smoke(l, this.local(-0.25, 4.8, 0.5), pal.smoke(), 2, 0.2, 1.0);
         Vfx.smoke(l, this.local(-0.25, 4.8, -0.5), pal.smoke(), 2, 0.2, 1.0);
      }

      if (t % (p >= 1 ? 8 : 16) == 0) {
         Vfx.embers(l, this.core(), pal.core(), p >= 1 ? 4 : 2, 0.4);
      }

      if (p >= 1 && t % 12 == 6) {
         Vfx.flare(l, this.local(-0.25, 4.95, 0.5), pal.glow(), 0.9, 6);
         Vfx.flare(l, this.local(-0.25, 4.95, -0.5), pal.glow(), 0.9, 6);
      }

      if (p >= 2) {
         if (t % 14 == 0) {
            Vfx.flare(l, this.local(0.2, 5.4, 1.4), pal.core(), 0.7, 6);
            Vfx.flare(l, this.local(0.2, 5.4, -1.4), pal.core(), 0.7, 6);
         }

         if (t % 10 == 5) {
            for (int side = -1; side <= 1; side += 2) {
               Vec3 fist = this.local(0.0, 1.3, 1.5 * side);
               l.sendParticles(ParticleTypes.FALLING_LAVA, fist.x, fist.y, fist.z, 1, 0.2, 0.1, 0.2, 0.0);
            }
         }
      }

      if (p >= 3) {
         if (t % 6 == 0) {
            Vfx.embers(l, this.position().add(0.0, 2.5, 0.0), pal.core(), 5, 1.4);
         }

         if (t % 10 == 0) {
            double a = t * 0.26;
            Vfx.glint(l, this.local(-0.9, 5.25 + Math.sin(a) * 0.9, Math.cos(a) * 0.9), pal.core(), 0.6);
         }
      }

      if (this.carapaceTicks > 0 && t % 8 == 0) {
         Vfx.sparks(l, this.core(), OBSIDIAN.glow(), 4, 0.2, 0.12);
      }

      if (this.dodging()) {
         Vfx.sparks(l, this.position().add(0.0, 0.2, 0.0), pal.glow(), 4, 0.3, 0.15);
         if (t % 2 == 0) {
            Vfx.dashTrail(l, this.skidFrom.add(0.0, 1.0, 0.0), this.position().add(0.0, 1.0, 0.0), pal);
            this.skidFrom = this.position();
         }
      } else {
         this.skidFrom = this.position();
      }
   }

   @Override
   protected void playStepSound(BlockPos pos, BlockState state) {
      super.playStepSound(pos, state);
      if (this.level() instanceof ServerLevel l && this.isAlive() && !this.inIntro()) {
         if (this.tickCount - this.stepTick >= 8 || this.stepTick > this.tickCount) {
            this.stepTick = this.tickCount;
            Vfx.smoke(l, this.position(), this.pal().smoke(), 3, 0.9, 1.1);
            if (this.phase() >= 3) {
               this.addHazard(l, 3, this.position(), this.position(), 1.2, 60);
            }
         }
      }
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
      super.registerControllers(controllers);
      controllers.add(new AnimationController("look", 0, test -> test.setAndContinue(this.lookAnim())));
   }

   private RawAnimation lookAnim() {
      if (this.isDeadOrDying()) {
         return LOOK_DEATH;
      }

      if (this.inIntro()) {
         this.lookPhase = 0;
         this.look = LOOK[0];
         return LOOK_SUMMON;
      }

      int p = Mth.clamp(this.phase(), 0, 3);
      if (p != this.lookPhase) {
         boolean first = this.lookPhase < 0 || p < this.lookPhase;
         this.lookPhase = p;
         this.look = first ? LOOK[p] : LOOK_IN[p];
      }

      return this.look;
   }

   private void daze(ServerLevel l, int ticks, float taken) {
      if (this.currentMove() != null) {
         this.cancelMove(l, 10);
      }

      this.endArc(l, false);
      this.submerged = false;
      this.getNavigation().stop();
      this.triggerAnim("action", "stagger");
      this.dazeTicks = ticks;
      this.buff(ticks, taken, 1.0F);
      BossFx.staggerBurst(l, this);
      this.voice(l, "stagger", 3.5F);
   }

   @Override
   protected boolean ownCourse(ServerLevel level, @Nullable LivingEntity target) {
      if (this.dazeTicks <= 0) {
         return false;
      }

      this.dazeTicks--;
      this.getNavigation().stop();
      this.setDeltaMovement(this.getDeltaMovement().multiply(0.3, 1.0, 0.3));
      if (this.dazeTicks % 10 == 0) {
         Vfx.wisps(level, this.local(0.0, 5.4, 0.0), this.pal(), 3, 0.9, 12);
      }

      return true;
   }

   @Override
   public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
      boolean bypass = source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
      if (this.submerged && !bypass) {
         return false;
      }

      float wardBefore = this.shield();
      boolean thorns = wardBefore > 0.0F && this.carapaceTicks > 0;
      boolean hurt = super.hurtServer(world, source, amount);
      if (hurt && this.isAlive()) {
         Entity attacker = source.getEntity();
         if (thorns && attacker instanceof LivingEntity a && a != this && a.isAlive() && a.distanceToSqr(this) < 36.0) {
            a.igniteForSeconds(4.0F);
            a.hurtServer(world, this.damageSources().thorns(this), this.dmg(4.0F));
            Vfx.sparks(world, a.position().add(0.0, a.getBbHeight() * 0.5, 0.0), this.pal().glow(), 8, 0.3, 0.16);
         }

         if (wardBefore > 0.0F && this.shield() <= 0.0F) {
            BossAction cur = this.currentMove();
            if (this.finalShell && cur != null && cur.anim().equals("final_eruption")) {
               this.shellBroken(world, attacker);
            } else if (attacker instanceof ServerPlayer p) {
               this.markTarget(p, 120);
            }
         }

         return hurt;
      } else {
         return hurt;
      }
   }

   private void shellBroken(ServerLevel l, @Nullable Entity breaker) {
      this.finalShell = false;
      Vfx.explosion(l, this.core(), this.pal(), 3.0);
      BossFx.slam(l, this, this.position(), 6.0, 20.0F, 1.2);
      if (breaker instanceof ServerPlayer p) {
         this.markTarget(p, 100);
      }

      this.daze(l, 60, 1.5F);
   }

   @Override
   public void die(DamageSource damageSource) {
      super.die(damageSource);
      this.meteors.clear();
      this.hazards.clear();
      this.shots.clear();
      this.submerged = false;
      this.dazeTicks = 0;
      this.carapaceTicks = 0;
      this.finalShell = false;
      if (this.level() instanceof ServerLevel l) {
         this.endArc(l, false);
      }
   }

   @Override
   protected void onPhaseChange(ServerLevel level, int phase) {
      int brutes = phase >= 2 ? 1 : 0;
      this.schedule(28, () -> this.summonAdds(level, 2, brutes));
      if (phase >= 2) {
         this.ventTimer = Math.max(this.ventTimer, 120);
      }
   }

   @Override
   protected void phaseBurst(ServerLevel level, int phase) {
      Vec3 c = this.position();
      switch (phase) {
         case 1:
            Vfx.explosion(level, this.local(0.9, 3.6, 0.0), this.pal(), 4.0);
            Vfx.shards(level, this.local(1.0, 3.6, 0.0), 2761256, 30, 0.6);
            Vfx.nova(level, c, FIRE, 10.0);
            BossFx.slam(level, this, c, 9.0, 14.0F, 2.2);
            break;
         case 2:
            Vfx.heavyImpact(level, c, this.pal(), 2.0);
            Vfx.shards(level, this.local(0.0, 4.4, 1.6), OBSIDIAN.glow(), 18, 0.55);
            Vfx.shards(level, this.local(0.0, 4.4, -1.6), OBSIDIAN.glow(), 18, 0.55);
            Vfx.shards(level, this.local(0.8, 5.0, 0.0), OBSIDIAN.glow(), 12, 0.45);
            Vfx.runeCircle(level, c, 8.0, this.pal(), 60, 0.06F, Vfx.Rune.SOLAR);

            for (int k = 0; k < 8; k++) {
               double a = k * Math.PI / 4.0;
               Vec3 at = groundAt(level, c.add(Math.cos(a) * 7.0, 0.0, Math.sin(a) * 7.0));
               Vfx.telegraphCircle(level, at, 2.0, 16, this.pal());
               this.schedule(16, () -> this.ventBlast(level, at, 2.0, 16.0F));
            }

            BossFx.slam(level, this, c, 10.0, 16.0F, 2.4);
            break;
         default:
            Vfx.impactFrame(level, c, 40.0, WHITE_HOT.core(), 0.9F);
            Vfx.nova(level, c, WHITE_HOT, 14.0);
            Vfx.pillar(level, c, WHITE_HOT, 20.0, 2.5);
            BossFx.slam(level, this, c, 12.0, 18.0F, 2.8);
            this.rainTimer = 30;
      }
   }

   @Override
   protected void onStagger(ServerLevel level) {
      Vfx.explosion(level, this.core(), this.pal(), 2.0);
      Vfx.smoke(level, this.core(), this.pal().smoke(), 10, 1.0, 1.6);
      if (this.getLastHurtByMob() instanceof ServerPlayer p) {
         this.markTarget(p, 160);
      }
   }

   @Override
   protected void onEnrage(ServerLevel level) {
      Vfx.nova(level, this.position(), WHITE_HOT, 10.0);
      Vfx.auraRing(level, this.position(), WHITE_HOT, 4.0);
      this.rainTimer = Math.min(this.rainTimer, 40);
      this.ventTimer = Math.round(this.ventTimer * 0.6F);
   }

   @Override
   protected void desperation(ServerLevel level) {
      BossAction cur = this.currentMove();
      if (cur != null && cur.anim().startsWith("phase_")) {
         this.schedule(Math.max(1, cur.length() - this.actionTick() + 2), () -> this.eruptNow(level));
      } else {
         this.eruptNow(level);
      }
   }

   private void eruptNow(ServerLevel level) {
      if (this.isAlive()) {
         this.forceAction(level, "final_eruption");
         Vfx.impactFrame(level, this.position(), 40.0, WHITE_HOT.core(), 0.6F);
      }
   }

   @Override
   protected void onIntroTick(ServerLevel level, int ticksLeft) {
      int e = this.introTicks() - ticksLeft;
      Vfx.Palette pal = this.pal();
      if (e == 1) {
         Vfx.runeCircle(level, this.position(), 6.0, pal, 139, 0.03F, Vfx.Rune.SOLAR);
         Vfx.groundCrack(level, this.position(), 5.0, pal);
      }

      if (e % 3 == 0) {
         level.sendParticles(ParticleTypes.LAVA, this.getX(), this.getY() + 0.3, this.getZ(), 3, 1.5, 0.2, 1.5, 0.1);
         Vfx.smoke(level, this.position().add(0.0, 0.5, 0.0), pal.smoke(), 3, 1.6, 1.4);
      }

      if (e % 10 == 0) {
         Vfx.embers(level, this.position().add(0.0, 0.5, 0.0), pal.core(), 10, 2.0);
      }

      switch (e) {
         case 24:
            Vfx.explosion(level, this.position(), pal, 2.0);
            Vfx.shake(level, this.position(), 32.0, 0.6, 12);
            break;
         case 72:
            Vfx.heavyImpact(level, this.local(3.7, 0.0, -0.8), pal, 0.8);
            break;
         case 84:
            Vfx.heavyImpact(level, this.local(3.6, 0.0, 1.4), pal, 0.8);
            break;
         case 98:
            Vfx.smoke(level, this.position().add(0.0, 1.5, 0.0), pal.smoke(), 10, 1.8, 2.0);
            Vfx.debris(level, this.position().add(0.0, 2.0, 0.0), Vfx.Debris.ASH, 3813938, 16, 0.3);
            break;
         case 112:
         case 118:
            Vfx.flare(level, this.core(), pal.core(), 2.2, 6);
            Vfx.shake(level, this.position(), 24.0, 0.35, 6);
            break;
         case 124:
            Vfx.nova(level, this.position(), pal, 8.0);
            Vfx.pillar(level, this.position(), pal, 14.0, 1.6);
            BossFx.knockback(level, this, this.position(), 6.0, 1.2);
      }
   }

   @Override
   protected void onDeathTick(ServerLevel level, int tick) {
      Vfx.Palette pal = this.pal();
      if (tick == 1) {
         Vfx.explosion(level, this.core(), pal, 2.0);
         Vfx.impactFrame(level, this.position(), 32.0, pal.core(), 0.7F);
      }

      if (tick >= 12 && tick <= 36 && tick % 4 == 0) {
         Vec3 crack = this.position()
            .add(
               (this.getRandom().nextDouble() - 0.5) * this.getBbWidth(),
               0.5 + this.getRandom().nextDouble() * this.getBbHeight() * 0.8,
               (this.getRandom().nextDouble() - 0.5) * this.getBbWidth()
            );
         Vfx.flare(level, crack, 16777215, 1.1, 6);
         Vfx.sparks(level, crack, pal.glow(), 6, 0.25, 0.18);
      }

      if (tick == 52) {
         Vfx.heavyImpact(level, this.position(), pal, 0.8);
      }

      if (tick == 64) {
         Vfx.beam(level, this.core(), this.local(1.0, 24.0, 0.0), pal, 0.6, 24);
      }

      if (tick >= 104 && tick <= 128 && tick % 4 == 0) {
         Vfx.smoke(level, this.position().add(0.0, 2.0, 0.0), pal.smoke(), 6, 1.2, 1.6);
         Vfx.debris(level, this.position().add(0.0, 2.5, 0.0), Vfx.Debris.ASH, 3813938, 8, 0.2);
      }

      if (tick == 136) {
         Vfx.explosion(level, this.position().add(0.0, 1.0, 0.0), OBSIDIAN, 3.0);
         Vfx.debris(level, this.position().add(0.0, 2.0, 0.0), Vfx.Debris.ASH, 3813938, 30, 0.35);
         Vfx.shards(level, this.position().add(0.0, 2.0, 0.0), 2761256, 30, 0.5);
         Vfx.shake(level, this.position(), 32.0, 0.8, 16);
      }
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

   static {
      for (int n = 0; n < 4; n++) {
         LOOK[n] = RawAnimation.begin().thenLoop("animation.pyraxis.look_" + n);
         LOOK_IN[n] = n == 0 ? LOOK[0] : RawAnimation.begin().thenPlay("animation.pyraxis.look_" + n + "_in").thenLoop("animation.pyraxis.look_" + n);
      }
   }

   private record Hazard(int kind, Vec3 a, Vec3 b, double r, int born, int until) {
   }

   private record Meteor(Vec3 from, Vec3 at, int spawn, int fall, float dmg, double r, boolean crater) {
   }

   private static final class Shot {
      final BossBolt bolt;
      final boolean flak;
      final double r;
      Vec3 last;

      Shot(BossBolt bolt, boolean flak, double r) {
         this.bolt = bolt;
         this.flak = flak;
         this.r = r;
         this.last = bolt.position();
      }
   }
}
