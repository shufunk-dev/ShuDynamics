package com.vexira.mysticalterrain.boss.entity;

import com.vexira.mysticalterrain.boss.BossAction;
import com.vexira.mysticalterrain.boss.BossBolt;
import com.vexira.mysticalterrain.boss.BossFx;
import com.vexira.mysticalterrain.boss.BossSounds;
import com.vexira.mysticalterrain.boss.MysticalBoss;
import com.vexira.mysticalterrain.boss.MysticalBosses;
import com.vexira.mysticalterrain.boss.RealmMob;
import com.vexira.mysticalterrain.boss.RealmMobs;
import com.vexira.mysticalterrain.boss.ai.BossContext;
import com.vexira.mysticalterrain.boss.vfx.Vfx;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;

public class ArchonBoss extends MysticalBoss {
   private static final BossSounds SOUNDS = BossSounds.of("archon");
   private static final Vfx.Palette P = Vfx.Palette.ARCANE;
   private static final int WHITE = 16777215;
   private static final double EYE_CLEAR = 4.0;
   private static final double GOLEM_CLEAR = 2.0;
   private static final RawAnimation[] FORMS = new RawAnimation[4];
   private static final RawAnimation SPIN;
   public static final float WIDTH = 2.2F;
   public static final float HEIGHT = 5.0F;
   public static final boolean FIRE_IMMUNE = false;
   private @Nullable Vec3 anchorAt;
   private final List<ArchonBoss.Node> pillars = new ArrayList<>();
   private int epoch;
   private int lonely;
   private int sentryTimer = 128;
   private int pulseTimer = 200;
   private int linkedEyes;
   private int constructs;
   private int constructsAt = -1000;
   private int phaseSeen;
   private int formShown = -1;
   private int formWant = -1;
   private int formAt;
   private @Nullable Vec3 slamAt;
   private @Nullable Vec3 beamDir;
   private Vec3 beamEnd = Vec3.ZERO;
   private final Vec3[] sideEnds = new Vec3[2];
   private Vec3 lanceFrom = Vec3.ZERO;
   private @Nullable Vec3 lanceEnd;
   private @Nullable Vec3 wellAt;
   private double wellR = 4.0;
   private @Nullable Vec3 stepDest;
   private boolean stepAmbush;
   private @Nullable Vec3 cageAt;
   private Vec3 driveDir = Vec3.ZERO;
   private Vec3 driveAim = Vec3.ZERO;
   private Vec3 drivePrev = Vec3.ZERO;
   private double driveTravel;
   private boolean driveStopped;
   private final Set<Integer> driveHit = new HashSet<>();
   private Vec3 prismFrom = Vec3.ZERO;
   private final List<Vec3> prismEnds = new ArrayList<>();
   private float prismYaw;
   private float prismPitch;
   private float prismStep;
   private int prismCount;
   private List<ArchonBoss.Node> latticeNodes = List.of();
   private ArchonBoss.@Nullable Lattice lattice;
   private int latticeUntil;
   private final List<ArchonBoss.Prison> prisons = new ArrayList<>();
   private Vec3 rendG0 = Vec3.ZERO;
   private Vec3 rendDir = Vec3.ZERO;
   private Vec3 thrustDir = Vec3.ZERO;
   private Vec3 overloadAt = Vec3.ZERO;
   private final Vec3[] monoliths = new Vec3[4];
   private Vec3 sigilC = Vec3.ZERO;
   private List<ArchonBoss.Node> sigilNodes = List.of();
   private final List<Vec3> golemSpots = new ArrayList<>();
   private final List<Vec3> eyeSpots = new ArrayList<>();
   private Vec3 collapseC = Vec3.ZERO;
   private float collapseStart;
   private boolean collapsePending;
   private boolean fractured;
   private boolean noUltimates;

   public static Builder attributes() {
      return MysticalBoss.bossAttributes(960.0, 12.0, 0.25, 8.0);
   }

   public ArchonBoss(EntityType<? extends Monster> type, Level level) {
      super(type, level, "archon", BossBarColor.BLUE);
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
      return 72;
   }

   @Override
   public float poiseMax() {
      return this.getMaxHealth() * 0.16F;
   }

   @Override
   protected double runSpeed() {
      return 1.45;
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
      return 1.5;
   }

   @Override
   protected double preferredDistance() {
      return 9.0;
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
   protected float[] phaseThresholds() {
      return new float[]{0.75F, 0.5F, 0.25F};
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
      super.registerControllers(controllers);
      controllers.add(new AnimationController("form", 6, test -> test.setAndContinue(FORMS[this.form()])));
      controllers.add(new AnimationController("spin", 0, test -> {
         if (!this.isDeadOrDying() && !this.inIntro()) {
            test.setControllerSpeed(this.getDeltaMovement().horizontalDistanceSqr() > 0.002 ? 2.0F : 1.0F);
            return test.setAndContinue(SPIN);
         } else {
            return PlayState.STOP;
         }
      }));
   }

   private int form() {
      int want = Mth.clamp(this.phase(), 0, 3);
      if (this.formShown < 0 || this.inIntro()) {
         this.formShown = want;
      } else if (want != this.formShown) {
         if (want != this.formWant) {
            this.formWant = want;
            this.formAt = this.tickCount + 28;
         }

         if (this.tickCount >= this.formAt) {
            this.formShown = want;
         }
      }

      return this.formShown;
   }

   @Override
   protected void defineActions(List<BossAction> out) {
      out.add(this.runeJab());
      out.add(this.gauntletCrush());
      out.add(this.repulsionPulse());
      out.add(this.runeMissiles());
      out.add(this.heartBeam());
      out.add(this.sigilField());
      out.add(this.phaseStep());
      out.add(this.runicAegis());
      out.add(this.prismLance());
      out.add(this.gravityWell());
      out.add(this.manaBurn());
      out.add(this.callConstructs());
      out.add(this.prismSplit());
      out.add(this.runePrison());
      out.add(this.cometDrive());
      out.add(this.pentagramLattice());
      out.add(this.runebladeCleave());
      out.add(this.runebladeRend());
      out.add(this.falseEdge());
      out.add(this.runebladeWhirl());
      out.add(this.arcaneOverload());
      out.add(this.monolithRain());
      out.add(this.sigilOfUnmaking());
      out.add(this.heartcoreCollapse());
   }

   private BossAction runeJab() {
      return BossAction.of("rune_jab", 1.3, 0.4, 0.75)
         .range(0.0, 5.5)
         .cooldown(50)
         .maxPhase(1)
         .weight(12)
         .tags("melee")
         .telegraph(0.0, 0.75)
         .advance(0.1, 0.35, 0.25)
         .combo("gauntlet_crush", 0.3F)
         .utility((b, t, c) -> c.dist < 4.0 ? 1.5F : 1.0F)
         .start((b, level, t, i) -> Vfx.telegraphCone(level, this.floor(level, this.position()), this.getYRot(), 45.0F, 5.5, 8, P))
         .tick((b, level, t, tick) -> {
            if (tick <= 15) {
               this.descend(t, 0.5);
            }

            if (tick == 8) {
               Vfx.telegraphCone(level, this.floor(level, this.position()), this.getYRot(), 55.0F, 5.5, 7, P);
            }
         })
         .hit((b, level, t, i) -> {
            float yaw = this.getYRot();
            if (i == 0) {
               Vfx.slashArc(level, this.local(-0.6, 1.8, 0.4), yaw, 0.0F, -30.0F, 4.5, P, -100.0F);

               for (LivingEntity e : this.inArc(level, 5.0, 40.0F, yaw)) {
                  if (this.phys(level, e, 16.0F)) {
                     shoveFrom(e, this.position(), 0.6, 0.2);
                     hitFx(level, e, 0.6);
                  }
               }
            } else {
               Vfx.slashArc(level, this.local(0.6, 1.8, 0.4), yaw, 0.0F, 30.0F, 5.0, P, 120.0F);

               for (LivingEntity e : this.inArc(level, 5.5, 55.0F, yaw)) {
                  if (this.phys(level, e, 18.0F)) {
                     shoveFrom(e, this.position(), 1.1, 0.25);
                     this.afflict(e, MobEffects.SLOWNESS, 20, 0);
                     hitFx(level, e, 0.7);
                  }
               }
            }
         });
   }

   private BossAction gauntletCrush() {
      return BossAction.of("gauntlet_crush", 1.9, 1.05)
         .range(0.0, 6.0)
         .cooldown(140)
         .weight(9)
         .tags("melee aoe guard_break")
         .superArmor()
         .lockFacing()
         .telegraph(0.35, 1.05)
         .advance(0.35, 0.9, 0.18)
         .utility((b, t, c) -> c.surroundCount >= 2 ? 1.3F : 1.0F)
         .start((b, level, t, i) -> {
            if (t != null) {
               this.faceTarget(t, 180.0F);
            }

            this.slamAt = null;
         })
         .tick((b, level, t, tick) -> {
            if (tick <= 21) {
               this.descend(t, 0.5);
            }

            if (tick == 7) {
               Vec3 at = this.floor(level, this.position().add(this.facing().scale(4.2)));
               this.slamAt = at;
               Vfx.telegraphCircle(level, at, 3.8, 14, P);
               Vfx.runeCircle(level, at, 3.8, P, 14, 0.06F, Vfx.Rune.GLYPH);
            }
         })
         .hit((b, level, t, i) -> {
            Vec3 at = this.slamAt != null ? this.slamAt : this.floor(level, this.position().add(this.facing().scale(1.9)));

            for (LivingEntity e : this.inCircle(level, at, 3.8)) {
               this.guardBreak(level, e, 100);
               if (this.phys(level, e, 32.0F)) {
                  shoveFrom(e, at, 1.4, 0.6);
               }
            }

            Vfx.heavyImpact(level, at, P, 1.2);
            Vfx.groundCrack(level, at, 4.2, P);
            if (this.phase() >= 2) {
               BossFx.wave(level, this, at, 8.0, 10, 12.0F, ParticleTypes.ENCHANT, 1.0, true);
            }
         });
   }

   private BossAction repulsionPulse() {
      return BossAction.of("repulsion_pulse", 1.2, 0.5)
         .range(0.0, 7.0)
         .cooldown(120)
         .weight(8)
         .tags("melee aoe behind")
         .lockFacing()
         .telegraph(0.0, 0.5)
         .utility((b, t, c) -> c.surroundCount >= 2 ? 3.0F : 1.0F)
         .start((b, level, t, i) -> {
            Vec3 g = this.floor(level, this.position());
            Vfx.telegraphCone(level, g, this.getYRot() + 180.0F, 80.0F, 7.0, 10, P);
            Vfx.telegraphCircle(level, g, 2.5, 10, P);
         })
         .tick((b, level, t, tick) -> {
            if (tick <= 10) {
               this.descend(t, 0.5);
            }
         })
         .hit((b, level, t, i) -> {
            Vec3 g = this.floor(level, this.position());
            Vec3 f = this.facing();

            for (LivingEntity e : this.inCircle(level, g, 7.0)) {
               Vec3 to = new Vec3(e.getX() - this.getX(), 0.0, e.getZ() - this.getZ());
               double d = to.length();
               boolean behind = d > 0.001 && Math.toDegrees(Math.acos(Mth.clamp(to.dot(f) / d, -1.0, 1.0))) > 100.0;
               if (behind || !(d > 2.5 + e.getBbWidth() * 0.5)) {
                  this.magic(level, e, 14.0F);
                  shoveFrom(e, this.position(), 2.0, 0.5);
                  this.afflict(e, MobEffects.SLOWNESS, 40, 1);
               }
            }

            Vfx.nova(level, this.local(0.0, 2.5, -0.6), P, 3.0);
            Vfx.streaks(level, this.heart(), f.scale(-1.0), P, 12);
            Vfx.shockwave(level, g, P, 7.0);
         });
   }

   private BossAction runeMissiles() {
      return BossAction.of("rune_missiles", 1.6, 0.5, 0.8, 1.1)
         .range(5.0, 32.0)
         .cooldown(60)
         .weight(12)
         .tags("ranged")
         .telegraph(0.3, 1.1)
         .utility((b, t, c) -> c.dist > 10.0 ? 1.3F : 1.0F)
         .tick((b, level, t, tick) -> {
            if (tick == 3 || tick == 9 || tick == 15) {
               Vfx.gather(level, this.local((tick == 9 ? 1 : -1) * 1.45, 3.4, 0.0), P, 1.4, 6);
            }
         })
         .hit(
            (b, level, t, i) -> {
               if (t != null && t.isAlive()) {
                  int side = i == 1 ? 1 : -1;
                  Vec3 from = this.local(0.95 * side, 3.05, 2.1);
                  double dist = from.distanceTo(t.position());
                  Vec3 aim = this.predict(t, dist / 0.75).add(0.0, t.getBbHeight() * 0.6, 0.0);
                  Vec3 dir = aim.subtract(from).normalize();
                  int n = (this.phase() >= 1 ? 2 : 1) + (this.enraged() ? 1 : 0);

                  for (int k = 0; k < n; k++) {
                     float off = n == 1 ? 0.0F : -7.0F + 14.0F * k / (n - 1);
                     BossBolt.shoot(level, this, BossBolt.Style.RUNE, from, dir.yRot(off * (float) (Math.PI / 180.0)).scale(0.75), this.dmg(9.0F))
                        .homing(t, 0.04)
                        .life(90);
                  }

                  Vfx.flare(level, from, P.glow(), 1.8, 6);
                  Vfx.sparks(level, from, P.core(), 8, 0.25, 0.18);
                  if (this.phase() >= 3 && i == 2) {
                     Vec3 h = this.heart();
                     Vec3 d0 = aim.subtract(h).normalize();

                     for (int k = 0; k < 5; k++) {
                        BossBolt.shoot(
                              level, this, BossBolt.Style.RUNE, h, d0.yRot((-20 + 10 * k) * (float) (Math.PI / 180.0)).scale(0.7), this.dmg(7.0F)
                           )
                           .life(60);
                     }

                     Vfx.flare(level, h, P.core(), 2.6, 6);
                  }
               }
            }
         );
   }

   private BossAction heartBeam() {
      return BossAction.of("heart_beam", 3.4, 1.0, 1.4, 1.8, 2.2, 2.6, 3.0)
         .range(6.0, 26.0)
         .cooldown(220)
         .weight(9)
         .tags("ranged")
         .telegraph(0.0, 1.0)
         .utility((b, t, c) -> c.dist < 6.0 ? 0.0F : (c.kitingTicks > 40 ? 2.0F : 1.0F))
         .start((b, level, t, i) -> {
            Vec3 h = this.heart();
            Vec3 aim = t != null ? this.predict(t, 20.0).add(0.0, t.getEyeHeight(), 0.0) : h.add(this.facing().scale(10.0)).add(0.0, -2.0, 0.0);
            this.beamDir = aim.subtract(h).normalize();
            this.beamEnd = h;
            this.sideEnds[0] = null;
            this.sideEnds[1] = null;
            Vfx.chargeUp(level, h, P, 20);
            Vfx.telegraphLine(level, this.floor(level, h), this.flat(this.beamDir), 24.0, 1.6, 20, P);
         })
         .tick((b, level, t, tick) -> {
            Vec3 h = this.heart();
            if (tick < 20) {
               if (tick % 2 == 0) {
                  Vfx.gather(level, h, P, 3.5, 8);
               }
            } else if (tick <= 62) {
               Vec3 dir = this.beamDir != null ? this.beamDir : this.facing();
               if (t != null && t.isAlive()) {
                  Vec3 want = t.getEyePosition().subtract(h).normalize();
                  double k = (this.phase() >= 2 ? 0.13 : 0.1) + (this.enraged() ? 0.03 : 0.0);
                  dir = dir.lerp(want, k).normalize();
               }

               this.beamDir = dir;
               this.beamEnd = this.rayEnd(level, h, dir, 26.0);
               Vfx.beam(level, h, this.beamEnd, P, 0.7);
               if (tick % 3 == 0) {
                  Vfx.impact(level, this.beamEnd, P, 0.4);
               }

               if (tick % 8 == 0) {
                  Vfx.groundCrack(level, this.beamEnd, 1.2, P);
               }

               if (tick == 20) {
                  Vfx.flash(level, h, 28.0, P.glow(), 0.18F);
                  Vfx.flare(level, h, P.core(), 4.0, 8);
               }

               if (this.phase() >= 3) {
                  double prog = Mth.clamp((tick - 20) / 42.0, 0.0, 1.0);
                  float ang = (float)((20.0 + 40.0 * prog) * (float) (Math.PI / 180.0));

                  for (int s = 0; s < 2; s++) {
                     this.sideEnds[s] = this.rayEnd(level, h, dir.yRot(s == 0 ? ang : -ang), 26.0);
                     if (tick % 2 == 0) {
                        Vfx.beam(level, h, this.sideEnds[s], P, 0.5, 3);
                     }
                  }
               }
            }
         })
         .hit((b, level, t, i) -> {
            Vec3 h = this.heart();

            for (LivingEntity e : this.onLine(level, h, this.beamEnd, 1.2)) {
               this.magic(level, e, 9.0F);
               this.afflict(e, MobEffects.SLOWNESS, 20, 0);
            }

            if (this.phase() >= 3) {
               for (Vec3 end : this.sideEnds) {
                  if (end != null) {
                     for (LivingEntity e : this.onLine(level, h, end, 0.5)) {
                        this.magic(level, e, 7.0F);
                     }
                  }
               }
            }
         })
         .end((b, level, t, i) -> this.beamDir = null);
   }

   private BossAction sigilField() {
      return BossAction.of("sigil_field", 2.6, 1.0)
         .range(0.0, 28.0)
         .cooldown(200)
         .weight(10)
         .anyTarget()
         .tags("ranged aoe")
         .telegraph(0.3, 1.0)
         .utility((b, t, c) -> c.targetVelocity.horizontalDistance() < 0.05 ? 1.5F : (c.engagedPlayers >= 2 ? 1.3F : 1.0F))
         .tick((b, level, t, tick) -> {
            if (tick >= 6 && tick <= 16 && tick % 2 == 0) {
               double a = tick * 0.9;
               Vfx.flare(level, this.heart().add(Math.cos(a) * 1.2, 2.6, Math.sin(a) * 1.2), P.glow(), 1.2, 10);
            }
         })
         .hit((b, level, t, i) -> {
            Vec3 center = this.floor(level, t != null ? this.predict(t, 30.0) : this.position());
            List<Vec3> pts = new ArrayList<>();
            pts.add(center);
            int p = this.phase();

            for (int k = 0; k < 3 + 2 * p; k++) {
               pts.add(BossFx.around(level, this, center, 2.5, 7.0));
            }

            if (p >= 2) {
               int others = 0;

               for (ServerPlayer pl : this.engaged()) {
                  if (pl != t && others < 3) {
                     others++;
                     pts.add(this.floor(level, this.predict(pl, 30.0)));

                     for (int k = 0; k < p; k++) {
                        pts.add(BossFx.around(level, this, pl.position(), 1.5, 4.5));
                     }
                  }
               }
            }

            List<Vec3> early = new ArrayList<>();
            List<Vec3> late = new ArrayList<>();

            for (int k = 0; k < pts.size(); k++) {
               Vec3 at = pts.get(k);
               int delay = 30 + (p >= 2 && k % 2 == 1 ? 12 : 0);
               Vfx.telegraphCircle(level, at, 2.6, delay, P);
               Vfx.runeCircle(level, at, 2.6, P, delay, 0.06F, Vfx.Rune.ARCANE);
               this.later(delay, () -> {
                  Vfx.pillar(level, at, P, 6.0, 1.0);
                  Vfx.impact(level, at, P, 1.0);

                  for (LivingEntity e : this.inCircle(level, at, 2.6)) {
                     this.magic(level, e, 16.0F);
                     e.push(0.0, 0.7, 0.0);
                     e.syncVelocity = true;
                  }

                  if (p >= 2) {
                     Vfx.groundCrack(level, at, 2.2, P);
                  }
               });
               if (p >= 2) {
                  (delay > 30 ? late : early).add(at);
               }
            }

            if (!early.isEmpty()) {
               this.later(30, () -> this.scars(level, early, 2.2, 100, 3.0F, MobEffects.SLOWNESS, 30, 0));
            }

            if (!late.isEmpty()) {
               this.later(42, () -> this.scars(level, late, 2.2, 100, 3.0F, MobEffects.SLOWNESS, 30, 0));
            }
         });
   }

   private BossAction phaseStep() {
      return BossAction.of("phase_step", 1.0, 0.45)
         .range(0.0, 40.0)
         .cooldown(150)
         .weight(7)
         .anyTarget()
         .tags("mobility gap_closer")
         .combo("rune_jab", 0.35F)
         .combo("runeblade_cleave", 0.4F)
         .combo("prism_lance", 0.25F)
         .utility((b, t, c) -> {
            if (c.surroundCount < 2 && (!(c.dist < 3.5) || c.ticksSinceHurt >= 20)) {
               return c.kitingTicks <= 60 && !(c.dist > 14.0) && !c.targetUnreachable ? 0.4F : 2.5F;
            } else {
               return 3.0F;
            }
         })
         .start((b, level, t, i) -> this.planStep(level, t))
         .hit((b, level, t, i) -> {
            Vec3 dest = this.stepDest;
            if (dest != null) {
               Vec3 was = this.heart();
               if (this.blinkTo(dest)) {
                  if (t != null) {
                     this.faceTarget(t, 180.0F);
                  }

                  Vfx.dashTrail(level, was, this.heart(), P);
                  Vfx.lightning(level, was, this.heart(), P, 2);
                  if (this.stepAmbush) {
                     Vec3 g = this.floor(level, this.position());

                     for (LivingEntity e : this.inCircle(level, g, 2.0)) {
                        this.magic(level, e, 10.0F);
                        shoveFrom(e, this.position(), 0.8, 0.3);
                     }

                     Vfx.nova(level, this.heart(), P, 2.5);
                  }
               }
            }
         })
         .end((b, level, t, i) -> {
            this.stepDest = null;
            if (this.phase() >= 2) {
               this.setCooldown("phase_step", this.cd(110));
            }
         });
   }

   private void planStep(ServerLevel level, @Nullable LivingEntity t) {
      BossContext c = this.ctx();
      boolean escape = t == null || c.surroundCount >= 2 || c.dist < 3.5 && c.ticksSinceHurt < 20;
      Vec3 dest;
      if (!escape) {
         Vec3 tp = t.position();
         dest = tp.add(this.flat(this.position().subtract(tp)).scale(4.0));
         if (this.phase() >= 1) {
            Vec3 behind = tp.subtract(this.flat(Vec3.directionFromRotation(0.0F, t.getYRot())).scale(2.8));
            if (BossFx.safeGround(level, behind, 3.0)) {
               dest = behind;
            }
         }

         dest = dest.add(0.0, 0.8, 0.0);
      } else {
         Vec3 base = t == null ? this.anchor(level) : this.anchorOrTarget(level, t);
         Vec3 best = null;
         double bestScore = -1.0;

         for (int k = 0; k < 6; k++) {
            Vec3 at = BossFx.around(level, this, base, 8.0, 12.0);
            double near = Double.MAX_VALUE;

            for (ServerPlayer p : this.engaged()) {
               near = Math.min(near, p.position().distanceToSqr(at));
            }

            if (best == null || near > bestScore) {
               best = at;
               bestScore = near;
            }
         }

         dest = best.add(0.0, this.hoverHeight(), 0.0);
      }

      this.stepAmbush = !escape;
      this.stepDest = dest;
      Vec3 g = this.floor(level, dest);
      Vfx.telegraphCircle(level, g, 1.8, 9, P);
      Vfx.runeCircle(level, g, 1.6, P, 12, 0.1F, Vfx.Rune.ARCANE);
      Vfx.gather(level, this.heart(), P, 3.0, 10);
   }

   private BossAction runicAegis() {
      return BossAction.of("runic_aegis", 1.8, 0.7)
         .range(0.0, 48.0)
         .cooldown(600)
         .weight(5)
         .anyTarget()
         .tags("defensive")
         .telegraph(0.0, 0.7)
         .utility((b, t, c) -> this.shield() > 0.0F ? 0.0F : (c.targetAiming ? 2.0F : 1.0F))
         .hit((b, level, t, i) -> {
            this.shield(this.getMaxHealth() * (0.1F + 0.03F * this.phase()), 200);
            Vec3 g = this.floor(level, this.position());
            Vfx.shieldBubble(level, this.local(0.0, 2.5, 0.0), 3.4, P, 36);
            Vfx.ward(level, g, 3.0, P, 40);
            Vfx.runeCircle(level, g, 3.2, P, 40, 0.04F, Vfx.Rune.GLYPH);
         })
         .end((b, level, t, i) -> {
            if (this.phase() >= 2) {
               this.setCooldown("runic_aegis", this.cd(480));
            }
         });
   }

   private BossAction prismLance() {
      return BossAction.of("prism_lance", 1.2, 0.6)
         .range(5.0, 40.0)
         .cooldown(90)
         .weight(6)
         .tags("ranged punish")
         .lockFacing()
         .telegraph(0.0, 0.6)
         .utility((b, t, c) -> c.targetAiming ? 1.5F : (c.dist > 14.0 ? 1.2F : 1.0F))
         .start((b, level, t, i) -> {
            if (t != null) {
               this.faceTarget(t, 180.0F);
            }

            Vec3 h = this.heart();
            Vec3 aim = t != null ? this.predict(t, 12.0).add(0.0, t.getBbHeight() * 0.6, 0.0) : h.add(this.facing().scale(20.0)).add(0.0, -3.0, 0.0);
            Vec3 dir = aim.subtract(h).normalize();
            this.lanceFrom = h;
            this.lanceEnd = this.rayEnd(level, h, dir, 40.0);
            double len = Math.max(2.0, hd(h, this.lanceEnd));
            Vfx.telegraphLine(level, this.floor(level, h), this.flat(dir), len, 1.2, 12, P);
            Vfx.beam(level, h, this.lanceEnd, P, 0.12, 12);
         })
         .hit((b, level, t, i) -> {
            Vec3 end = this.lanceEnd;
            if (end != null) {
               Vec3 from = this.lanceFrom;

               for (LivingEntity e : this.onLine(level, from, end, 1.0)) {
                  boolean using = e.isUsingItem();
                  if (using) {
                     e.stopUsingItem();
                     Vfx.shards(level, e.getEyePosition(), P.core(), 10, 0.4);
                  }

                  this.magic(level, e, using ? 24.0F : 18.0F);
               }

               Vfx.beam(level, from, end, P, 0.9, 5);
               Vfx.lightning(level, from, end, P, 3);
               Vfx.impact(level, end, P, 1.0);
               Vfx.glint(level, end, P.core(), 1.6);
               this.lanceEnd = null;
            }
         });
   }

   private BossAction gravityWell() {
      return BossAction.of("gravity_well", 2.2, 1.2)
         .range(0.0, 30.0)
         .cooldown(180)
         .weight(5)
         .tags("ranged anti_air")
         .telegraph(0.0, 1.2)
         .utility((b, t, c) -> t != null && t.isFallFlying() ? 2.0F : (c.targetUnreachable ? 1.5F : (!c.targetFlying && !c.targetAirborne ? 0.6F : 1.0F)))
         .start((b, level, t, i) -> {
            double r = this.phase() >= 2 ? 5.0 : 4.0;
            Vec3 at = this.floor(level, t != null ? this.predict(t, 24.0) : this.position());
            this.wellR = r;
            this.wellAt = at;
            Vfx.telegraphCircle(level, at, r, 24, P);
            Vfx.vortex(level, at, r, 14.0, P, 24);
            Vfx.runeCircle(level, at, r, P, 24, -0.08F, Vfx.Rune.GLYPH);
         })
         .tick((b, level, t, tick) -> {
            Vec3 at = this.wellAt;
            if (at != null && tick >= 12 && tick <= 23) {
               for (LivingEntity e : this.inCircle(level, at, this.wellR + 2.0, 1.0, 16.0)) {
                  Vec3 to = new Vec3(at.x - e.getX(), 0.0, at.z - e.getZ());
                  double d = to.length();
                  Vec3 pull = d > 0.3 ? to.scale(0.12 / d) : Vec3.ZERO;
                  e.push(pull.x, -0.08, pull.z);
                  e.syncVelocity = true;
               }
            }
         })
         .hit((b, level, t, i) -> {
            Vec3 at = this.wellAt;
            if (at != null) {
               double r = this.wellR;

               for (LivingEntity e : this.inCircle(level, at, r, 1.0, 16.0)) {
                  if (!e.onGround()) {
                     e.stopFallFlying();
                     e.setDeltaMovement(0.0, -2.4, 0.0);
                     e.syncVelocity = true;
                  }

                  this.magic(level, e, 16.0F);
                  this.afflict(e, MobEffects.SLOWNESS, 60, 1);
               }

               Vfx.nova(level, at, P, r);
               Vfx.pillar(level, at, P, 12.0, 1.4);
               Vfx.shake(level, at, 20.0, 0.5, 8);
               if (this.phase() >= 2) {
                  this.scars(level, List.of(at), r - 1.0, 80, 0.0F, MobEffects.SLOWNESS, 30, 2);
               }

               this.wellAt = null;
            }
         });
   }

   private BossAction manaBurn() {
      return BossAction.of("mana_burn", 2.0, 1.0)
         .phase(1)
         .range(0.0, 22.0)
         .cooldown(360)
         .weight(5)
         .anyTarget()
         .tags("aoe punish")
         .telegraph(0.0, 1.0)
         .utility((b, t, c) -> {
            int n = t == null ? 0 : buffs(t);
            if (n >= 2) {
               return Math.min(3.0F, 1.0F + 0.4F * n);
            } else {
               return n == 0 && !c.targetHealing ? 0.3F : 1.0F;
            }
         })
         .start((b, level, t, i) -> {
            Vfx.chargeUp(level, this.heart(), P, 20);
            Vfx.runeCircle(level, this.floor(level, this.position()), 20.0, P, 20, 0.05F, Vfx.Rune.GLYPH);
         })
         .tick((b, level, t, tick) -> {
            if (tick >= 4 && tick < 20 && tick % 3 == 0) {
               Vfx.flare(level, this.heart(), P.glow(), 2.0 + tick * 0.15, 5);
            }
         })
         .hit((b, level, t, i) -> {
            Vec3 h = this.heart();
            int shown = 0;

            for (LivingEntity e : BossFx.victims(level, this, h, 20.0)) {
               if (this.los(level, h, e.getEyePosition())) {
                  int stripped = 0;

                  for (MobEffectInstance inst : new ArrayList<MobEffectInstance>(e.getActiveEffects())) {
                     if (((MobEffect)inst.getEffect().value()).isBeneficial() && !inst.isAmbient()) {
                        e.removeEffect(inst.getEffect());
                        stripped++;
                     }
                  }

                  float base = 6 + Math.min(20, 5 * stripped);
                  if (eatingOrDrinking(e)) {
                     base += 8.0F;
                     e.stopUsingItem();
                  }

                  this.magic(level, e, base);
                  this.afflict(e, MobEffects.HUNGER, 160, 1);
                  if (shown++ < 8) {
                     Vfx.lightning(level, h, e.getEyePosition(), P, 2);
                  }
               }
            }

            Vfx.explosion(level, h, P, 2.2);
            Vfx.nova(level, h, P, 6.0);
         });
   }

   private BossAction callConstructs() {
      return BossAction.of("call_constructs", 2.4, 1.4)
         .phase(1)
         .range(0.0, 48.0)
         .cooldown(700)
         .weight(6)
         .anyTarget()
         .tags("summon")
         .telegraph(0.0, 1.4)
         .utility((b, t, c) -> {
            int n = this.constructsNear();
            return n >= 5 ? 0.0F : (n == 0 ? 1.5F : 1.0F);
         })
         .start((b, level, t, i) -> {
            this.golemSpots.clear();
            this.eyeSpots.clear();
            Vec3 near = t != null ? t.position() : this.position();

            for (int k = 0; k < (this.phase() >= 2 ? 2 : 1); k++) {
               this.golemSpots.add(this.clearSpot(level, near, 2.0, 5.0, 2.0));
            }

            for (int k = 0; k < 1 + this.phase(); k++) {
               this.eyeSpots.add(this.clearSpot(level, this.position(), 3.0, 7.0, 4.0));
            }

            for (Vec3 p : this.golemSpots) {
               Vfx.runeCircle(level, p, 1.5, P, 28, 0.1F, Vfx.Rune.GLYPH);
               Vfx.telegraphCircle(level, p, 1.2, 28, P);
            }

            for (Vec3 p : this.eyeSpots) {
               Vfx.runeCircle(level, p, 1.5, P, 28, 0.1F, Vfx.Rune.GLYPH);
               Vfx.telegraphCircle(level, p, 1.2, 28, P);
            }
         })
         .hit((b, level, t, i) -> {
            for (Vec3 p : this.golemSpots) {
               this.spawnConstruct(level, "rune_golem", playerNear(level, p, 2.0) ? this.clearSpot(level, p, 2.0, 4.0, 2.0) : p, 0.0, t);
            }

            for (Vec3 p : this.eyeSpots) {
               this.spawnConstruct(level, "arcane_eye", playerNear(level, p, 4.0) ? this.clearSpot(level, this.position(), 3.0, 7.0, 4.0) : p, 1.5, t);
            }

            this.golemSpots.clear();
            this.eyeSpots.clear();
            this.constructsAt = -1000;
         });
   }

   private BossAction prismSplit() {
      return BossAction.of("prism_split", 2.4, 1.2)
         .phase(1)
         .range(5.0, 28.0)
         .cooldown(200)
         .weight(9)
         .tags("ranged aoe")
         .superArmor()
         .telegraph(0.4, 1.2)
         .utility((b, t, c) -> c.engagedPlayers >= 2 ? 1.3F : 1.0F)
         .start((b, level, t, i) -> this.prismEnds.clear())
         .tick((b, level, t, tick) -> {
            if (tick == 8) {
               Vec3 h = this.heart();
               this.prismFrom = h;
               Vec3 aim = t != null ? this.predict(t, 16.0).add(0.0, t.getBbHeight() * 0.6, 0.0) : h.add(this.facing().scale(12.0)).add(0.0, -3.0, 0.0);
               Vec3 to = aim.subtract(h);
               this.prismYaw = yawOf(to);
               this.prismPitch = pitchOf(to);
               this.prismCount = this.phase() >= 3 ? 5 : 3;
               this.prismStep = this.phase() >= 3 ? 18.0F : 22.0F;
               this.prismEnds.clear();

               for (int k = 0; k < this.prismCount; k++) {
                  float yaw = this.prismYaw + (k - (this.prismCount - 1) / 2.0F) * this.prismStep;
                  Vec3 dir = Vec3.directionFromRotation(this.prismPitch, yaw);
                  Vec3 end = this.rayEnd(level, h, dir, 24.0);
                  this.prismEnds.add(end);
                  Vfx.telegraphLine(level, this.floor(level, h), this.flat(dir), Math.min(24.0, hd(h, end) + 1.0), 1.4, 16, P);
               }
            }

            if (tick >= 8 && tick <= 22 && tick % 2 == 0) {
               Vfx.gather(level, this.heart(), P, 3.0, 10);
            }
         })
         .hit((b, level, t, i) -> {
            Vec3 h = this.prismFrom;
            this.fireBeams(level, h, this.prismEnds, 16.0F);
            if (this.phase() >= 2 && this.prismCount > 0) {
               List<Vec3> ends = new ArrayList<>();

               for (int k = 0; k <= this.prismCount; k++) {
                  float yaw = this.prismYaw + (k - this.prismCount / 2.0F) * this.prismStep;
                  Vec3 dir = Vec3.directionFromRotation(this.prismPitch, yaw);
                  Vec3 end = this.rayEnd(level, h, dir, 24.0);
                  ends.add(end);
                  Vfx.telegraphLine(level, this.floor(level, h), this.flat(dir), Math.min(24.0, hd(h, end) + 1.0), 1.4, 10, P);
               }

               this.later(10, () -> this.fireBeams(level, h, ends, 14.0F));
            }
         });
   }

   private void fireBeams(ServerLevel level, Vec3 from, List<Vec3> ends, float base) {
      Set<Integer> hit = new HashSet<>();

      for (Vec3 end : ends) {
         for (LivingEntity e : this.onLine(level, from, end, 1.2)) {
            if (hit.add(e.getId())) {
               this.magic(level, e, base);
            }
         }

         Vfx.beam(level, from, end, P, 0.8, 8);
         Vfx.impact(level, end, P, 0.8);
         Vfx.groundCrack(level, end, 1.5, P);
      }

      Vfx.flare(level, from, P.core(), 4.0, 8);
   }

   private BossAction runePrison() {
      return BossAction.of("rune_prison", 2.0, 1.0)
         .phase(1)
         .range(4.0, 24.0)
         .cooldown(280)
         .weight(7)
         .tags("ranged")
         .telegraph(0.0, 1.0)
         .combo("prism_lance", 0.3F)
         .combo("heart_beam", 0.2F)
         .utility((b, t, c) -> c.kitingTicks > 60 ? 2.5F : (c.targetAiming ? 1.5F : 1.0F))
         .start((b, level, t, i) -> {
            Vec3 at = this.floor(level, t != null ? this.predict(t, 20.0) : this.position());
            this.cageAt = at;
            Vfx.telegraphCircle(level, at, 3.2, 20, P);
            Vfx.runeCircle(level, at, 3.2, P, 20, 0.1F, Vfx.Rune.GLYPH);
         })
         .hit((b, level, t, i) -> {
            Vec3 at = this.cageAt;
            if (at != null) {
               int dur = this.phase() >= 3 ? 90 : 70;
               List<LivingEntity> caught = this.inCircle(level, at, 3.2);
               Vfx.ward(level, at, 3.2, P, dur);

               for (int k = 0; k < 6; k++) {
                  double a = k * Math.PI / 3.0;
                  Vfx.pillar(level, this.floor(level, at.add(Math.cos(a) * 3.2, 0.5, Math.sin(a) * 3.2)), P, 3.5, 0.5);
               }

               for (LivingEntity e : caught) {
                  this.afflict(e, MobEffects.SLOWNESS, dur, 1);
               }

               Vfx.flare(level, at.add(0.0, 1.2, 0.0), P.core(), 3.5, 8);
               if (!caught.isEmpty()) {
                  this.prisons.add(new ArchonBoss.Prison(at, caught, this.tickCount + dur, this.epoch));
               } else {
                  this.later(dur, () -> Vfx.nova(level, at, P, 3.2));
               }

               this.cageAt = null;
            }
         });
   }

   private BossAction cometDrive() {
      return BossAction.of("comet_drive", 1.8, 0.85)
         .phase(1)
         .range(7.0, 22.0)
         .cooldown(180)
         .weight(8)
         .tags("melee gap_closer")
         .superArmor()
         .lockFacing()
         .telegraph(0.35, 0.85)
         .combo("gauntlet_crush", 0.3F)
         .combo("runeblade_whirl", 0.3F)
         .utility((b, t, c) -> c.targetUnreachable ? 0.3F : 1.0F)
         .start((b, level, t, i) -> {
            if (t != null) {
               this.faceTarget(t, 180.0F);
            }

            this.driveHit.clear();
            this.driveStopped = false;
            this.driveDir = this.facing();
            this.driveAim = this.position().add(this.facing().scale(8.0));
            this.driveTravel = 0.0;
            Vfx.gather(level, this.heart(), P, 3.0, 12);
         })
         .tick((b, level, t, tick) -> {
            if (tick < 10) {
               this.descend(t, 0.5);
            }

            if (tick == 7) {
               Vec3 aim = t != null ? this.predict(t, 10.0) : this.position().add(this.facing().scale(8.0));
               this.driveAim = aim;
               this.driveDir = this.flat(aim.subtract(this.position()));
               this.face(aim);
               this.driveTravel = Mth.clamp(hd(this.position(), aim) - 1.5, 0.0, 20.0);
               Vfx.telegraphLine(level, this.floor(level, this.position()), this.driveDir, this.driveTravel + 3.0, 2.6, 10, P);
            }

            if (tick >= 10 && tick <= 16) {
               if (tick == 10) {
                  this.drivePrev = this.heart();
               }

               if (this.driveStopped || tick > 10 && this.horizontalCollision) {
                  this.driveStopped = true;
                  this.setDeltaMovement(0.0, this.getDeltaMovement().y * 0.5, 0.0);
                  return;
               }

               double sp = this.driveTravel / 7.0;
               double vy = Mth.clamp((this.driveAim.y + 0.5 - this.getY()) * 0.3, -0.6, 0.6);
               Vec3 v = new Vec3(this.driveDir.x * sp, vy, this.driveDir.z * sp);
               this.setDeltaMovement(v);
               Vec3 h = this.heart();
               if (tick > 10) {
                  Vfx.dashTrail(level, this.drivePrev, h, P);
               }

               Vfx.meteorTrail(level, h, v, P);
               this.drivePrev = h;
               Vec3 side = new Vec3(-this.driveDir.z, 0.0, this.driveDir.x);

               for (LivingEntity e : this.inCircle(level, this.position(), 1.8 + this.getBbWidth() * 0.5, 3.0, 5.0)) {
                  if (this.driveHit.add(e.getId()) && this.phys(level, e, 20.0F)) {
                     double s = (e.getX() - this.getX()) * side.x + (e.getZ() - this.getZ()) * side.z;
                     Vec3 push = side.scale(s >= 0.0 ? 1.2 : -1.2);
                     e.push(push.x, 0.35, push.z);
                     e.syncVelocity = true;
                     hitFx(level, e, 0.7);
                  }
               }
            }
         })
         .hit((b, level, t, i) -> {
            this.setDeltaMovement(0.0, this.getDeltaMovement().y * 0.3, 0.0);
            Vec3 g = this.floor(level, this.position().add(this.facing().scale(2.4)));

            for (LivingEntity e : this.inCircle(level, g, 3.5)) {
               if (this.phys(level, e, 26.0F)) {
                  shoveFrom(e, g, 1.3, 0.5);
               }
            }

            Vfx.heavyImpact(level, g, P, 1.0);
            Vfx.groundCrack(level, g, 3.5, P);
            Vfx.shockwave(level, g, P, 3.5);
         })
         .end((b, level, t, i) -> {
            if (i == -2) {
               this.setDeltaMovement(Vec3.ZERO);
            }
         });
   }

   private BossAction pentagramLattice() {
      return BossAction.of("pentagram_lattice", 2.6, 1.3)
         .phase(1)
         .range(0.0, 48.0)
         .cooldown(500)
         .weight(7)
         .anyTarget()
         .tags("ranged aoe")
         .superArmor()
         .telegraph(0.3, 1.3)
         .utility((b, t, c) -> this.latticeUntil > this.tickCount ? 0.0F : 1.0F)
         .start((b, level, t, i) -> {
            this.latticeNodes = this.latticePoints(level, t);

            for (ArchonBoss.Node n : this.latticeNodes) {
               Vfx.chargeUp(level, n.top(), P, 26);
               Vfx.runeCircle(level, n.ground(), 1.5, P, 106, 0.08F, Vfx.Rune.GLYPH);
            }
         })
         .tick((b, level, t, tick) -> {
            if (tick == 6) {
               List<ArchonBoss.Node> n = this.latticeNodes;

               for (int k = 0; k < n.size(); k++) {
                  Vec3 a = n.get(k).ground();
                  Vec3 c = n.get((k + 2) % n.size()).ground();
                  Vfx.telegraphLine(level, a, c.subtract(a), hd(a, c), 1.4, 20, P);
               }
            }
         })
         .hit((b, level, t, i) -> {
            List<ArchonBoss.Node> nodes = this.latticeNodes;
            if (nodes.size() >= 3) {
               List<Vec3> ground = new ArrayList<>();
               List<Vec3> chain = new ArrayList<>();
               double cx = 0.0;
               double cz = 0.0;
               double cy = 0.0;

               for (ArchonBoss.Node n : nodes) {
                  ground.add(n.ground());
                  chain.add(n.top());
                  cx += n.ground().x;
                  cy += n.ground().y;
                  cz += n.ground().z;
               }

               Vec3 center = new Vec3(cx / nodes.size(), cy / nodes.size(), cz / nodes.size());
               boolean outer = this.phase() >= 2;
               boolean rotate = this.phase() >= 3;
               ArchonBoss.Lattice l = new ArchonBoss.Lattice(center, List.copyOf(ground), this.tickCount, this.epoch, outer, rotate);
               this.lattice = l;
               this.latticeUntil = this.tickCount + 80;
               int s = ground.size();
               if (!rotate) {
                  for (int k = 0; k < s; k++) {
                     Vfx.beam(level, ground.get(k).add(0.0, 1.0, 0.0), ground.get((k + 2) % s).add(0.0, 1.0, 0.0), P, 0.6, 80);
                  }
               }

               if (outer) {
                  List<Vec3> at = latticeAt(l, 20);

                  for (int k = 0; k < s; k++) {
                     Vec3 a = at.get(k);
                     Vec3 c = at.get((k + 1) % s);
                     Vfx.telegraphLine(level, a, c.subtract(a), hd(a, c), 1.4, 20, P);
                  }
               }

               chain.add(this.heart());
               Vfx.chainLightning(level, chain, P);
            }
         });
   }

   private BossAction runebladeCleave() {
      return BossAction.of("runeblade_cleave", 1.5, 0.7)
         .phase(2)
         .range(0.0, 6.0)
         .cooldown(70)
         .weight(12)
         .tags("melee aoe")
         .telegraph(0.0, 0.7)
         .combo("runeblade_rend", 0.35F)
         .combo("false_edge", 0.25F)
         .start((b, level, t, i) -> Vfx.telegraphCone(level, this.floor(level, this.position()), this.getYRot(), 100.0F, 6.0, 14, P))
         .tick((b, level, t, tick) -> {
            if (tick <= 14) {
               this.descend(t, 0.5);
            }
         })
         .hit((b, level, t, i) -> {
            float yaw = this.getYRot();
            Vec3 left = Vec3.directionFromRotation(0.0F, yaw - 90.0F);

            for (LivingEntity e : this.inArc(level, 6.0, 100.0F, yaw)) {
               this.physMagic(level, e, 26.0F, 6.0F);
               e.push(left.x * 1.3, 0.3, left.z * 1.3);
               e.syncVelocity = true;
               hitFx(level, e, 0.8);
            }

            Vfx.slashArc(level, this.local(0.0, 2.0, 0.0), yaw, 5.0F, 0.0F, 6.0, P, 200.0F);
            Vfx.glint(level, this.local(-2.1, 0.5, 5.8), P.core(), 1.4);
         });
   }

   private BossAction runebladeRend() {
      return BossAction.of("runeblade_rend", 1.9, 0.65, 1.2)
         .phase(2)
         .range(0.0, 8.0)
         .cooldown(140)
         .weight(9)
         .tags("melee guard_break")
         .superArmor()
         .lockFacing()
         .telegraph(0.0, 1.2)
         .combo("runeblade_whirl", 0.2F)
         .start((b, level, t, i) -> {
            if (t != null) {
               this.faceTarget(t, 180.0F);
            }

            this.rendDir = this.facing();
            this.rendG0 = this.floor(level, this.position());
            Vfx.telegraphLine(level, this.rendG0, this.rendDir, 8.0, 2.2, 13, P);
         })
         .tick((b, level, t, tick) -> {
            if (tick <= 24) {
               this.descend(t, 0.5);
            }
         })
         .hit((b, level, t, i) -> {
            Vec3 g0 = this.rendG0;
            Vec3 dir = this.rendDir;
            if (i == 0) {
               for (LivingEntity e : this.onStrip(level, g0, g0.add(dir.scale(8.0)), 1.1, 1.0, 3.5)) {
                  this.guardBreak(level, e, 100);
                  if (this.phys(level, e, 30.0F)) {
                     Vec3 v = e.getDeltaMovement();
                     e.setDeltaMovement(v.x, -0.6, v.z);
                     e.syncVelocity = true;
                     hitFx(level, e, 0.9);
                  }
               }

               Vfx.slashArc(level, this.local(0.0, 3.0, 0.5), this.getYRot(), 0.0F, 90.0F, 7.0, P, 160.0F);
               Vec3 chop = this.floor(level, g0.add(dir.scale(5.0)));
               Vfx.heavyImpact(level, chop, P, 1.2);
               Vfx.groundCrack(level, chop, 2.5, P);
               Vfx.telegraphLine(level, chop, dir, 10.0, 2.0, 11, P);
            } else {
               Vec3 a = g0.add(dir.scale(5.0));

               for (LivingEntity e : this.onStrip(level, a, g0.add(dir.scale(15.0)), 1.0, 1.0, 3.5)) {
                  this.magic(level, e, 16.0F);
                  e.push(0.0, 0.9, 0.0);
                  e.syncVelocity = true;
               }

               for (int s = 5; s <= 15; s += 2) {
                  Vec3 p = this.floor(level, g0.add(dir.scale(s)).add(0.0, 1.0, 0.0));
                  Vfx.pillar(level, p, P, 4.0, 0.8);
                  if (s == 9 || s == 13) {
                     Vfx.groundCrack(level, p, 2.0, P);
                  }
               }
            }
         });
   }

   private BossAction falseEdge() {
      return BossAction.of("false_edge", 1.9, 1.3)
         .phase(2)
         .range(0.0, 8.0)
         .cooldown(160)
         .weight(6)
         .tags("melee gap_closer")
         .superArmor()
         .lockFacing()
         .telegraph(0.7, 1.3)
         .advance(1.0, 1.3, 0.5)
         .utility((b, t, c) -> c.targetBlocking ? 0.5F : 1.0F)
         .start((b, level, t, i) -> {
            if (t != null) {
               this.faceTarget(t, 180.0F);
            }

            this.rendDir = this.facing();
            this.rendG0 = this.floor(level, this.position());
            Vfx.telegraphLine(level, this.rendG0, this.rendDir, 8.0, 2.2, 13, P);
         })
         .tick((b, level, t, tick) -> {
            if (tick <= 26) {
               this.descend(t, 0.5);
            }

            if (tick == 13) {
               for (int s = 1; s <= 7; s += 2) {
                  Vec3 p = this.rendG0.add(this.rendDir.scale(s)).add(0.0, 0.3, 0.0);
                  Vfx.smoke(level, p, P.smoke(), 3, 0.5, 0.8);
                  Vfx.sparks(level, p, P.glow(), 3, 0.12, 0.12);
               }
            }

            if (tick == 14) {
               Vec3 aim = t != null ? this.predict(t, 12.0) : this.position().add(this.facing().scale(6.0));
               this.face(aim);
               this.thrustDir = this.facing();
               Vfx.telegraphLine(level, this.floor(level, this.position()), this.thrustDir, 12.0, 1.6, 12, P);
            }
         })
         .hit((b, level, t, i) -> {
            Vec3 g = this.floor(level, this.position());
            Vec3 dir = this.thrustDir;

            for (LivingEntity e : this.onStrip(level, g, g.add(dir.scale(9.0)), 0.8, 1.0, 3.5)) {
               if (this.phys(level, e, 28.0F)) {
                  hitFx(level, e, 0.9);
               }

               for (int k = 1; k <= 3; k++) {
                  this.later(20 * k, () -> {
                     if (e.isAlive()) {
                        this.magic(level, e, 3.0F);
                        Vfx.sparks(level, e.position().add(0.0, e.getBbHeight() * 0.6, 0.0), P.glow(), 5, 0.15, 0.12);
                     }
                  });
               }
            }

            Vfx.streaks(level, this.local(0.0, 2.0, 1.0), dir, P, 12);
            Vfx.slashArc(level, this.local(0.0, 2.0, 0.0), this.getYRot(), 0.0F, 0.0F, 7.0, P, 60.0F);
            Vfx.impact(level, this.position().add(dir.scale(7.0)).add(0.0, 1.6, 0.0), P, 0.9);
         });
   }

   private BossAction runebladeWhirl() {
      return BossAction.of("runeblade_whirl", 1.8, 0.7, 1.2)
         .phase(2)
         .range(0.0, 7.5)
         .cooldown(160)
         .weight(7)
         .tags("melee aoe behind")
         .superArmor()
         .telegraph(0.0, 1.2)
         .utility((b, t, c) -> c.surroundCount >= 2 ? 2.0F : 1.0F)
         .start((b, level, t, i) -> Vfx.telegraphCircle(level, this.floor(level, this.position()), 6.5, 14, P))
         .tick((b, level, t, tick) -> {
            if (tick <= 24) {
               this.descend(t, 0.5);
            }

            if (tick == 14) {
               Vfx.telegraphCircle(level, this.floor(level, this.position()), 7.5, 10, P);
            }
         })
         .hit((b, level, t, i) -> {
            float yaw = this.getYRot();
            if (i == 0) {
               for (LivingEntity e : this.inCircle(level, this.position(), 6.5, 3.0, 5.0)) {
                  if (this.phys(level, e, 22.0F)) {
                     shoveFrom(e, this.position(), 1.0, 0.3);
                     hitFx(level, e, 0.7);
                  }
               }

               Vfx.slashArc(level, this.local(0.0, 2.2, 0.0), yaw, 0.0F, 0.0F, 6.5, P, -360.0F);
            } else {
               for (LivingEntity e : this.inCircle(level, this.position(), 7.5, 3.0, 5.0)) {
                  this.physMagic(level, e, 24.0F, 8.0F);
                  shoveFrom(e, this.position(), 1.5, 0.4);
                  hitFx(level, e, 0.9);
               }

               Vfx.slashArc(level, this.local(0.0, 2.4, 0.0), yaw, 0.0F, 15.0F, 7.5, P, -360.0F);
               Vfx.shockwave(level, this.floor(level, this.position()), P, 7.5);
            }
         });
   }

   private BossAction arcaneOverload() {
      return BossAction.of("arcane_overload", 4.0, 2.6)
         .phase(2)
         .range(0.0, 18.0)
         .cooldown(700)
         .weight(6)
         .anyTarget()
         .tags("aoe ultimate")
         .superArmor()
         .telegraph(0.0, 2.6)
         .utility((b, t, c) -> this.noUltimates ? 0.0F : (c.engagedPlayers >= 2 ? 1.3F : 1.0F))
         .start((b, level, t, i) -> {
            Vec3 g = this.floor(level, this.position());
            this.overloadAt = g;
            Vfx.telegraphCircle(level, g, 13.0, 52, P);
            Vfx.runeCircle(level, g, 13.0, P, 52, 0.05F, Vfx.Rune.ARCANE);
            Vfx.chargeUp(level, this.heart(), P, 52);
            Vfx.vortex(level, g, 3.0, 5.0, P, 52);

            for (ArchonBoss.Node n : this.latticePoints(level, t)) {
               if (n.real()) {
                  Vec3 away = this.flat(n.ground().subtract(g));
                  Vfx.ward(level, this.floor(level, n.ground().add(away.scale(1.5)).add(0.0, 1.0, 0.0)), 1.2, P, 52);
               }
            }
         })
         .tick((b, level, t, tick) -> {
            if (tick < 50) {
               Vec3 h = this.heart();
               if (tick % 2 == 0) {
                  Vfx.gather(level, h, P, 8.0 - tick * 0.12, 12);
                  this.drawIn(h, 16.0, 0.06);
               }

               if (tick % 10 == 0) {
                  Vec3 d = new Vec3(this.getRandom().nextDouble() - 0.5, this.getRandom().nextDouble() - 0.5, this.getRandom().nextDouble() - 0.5);
                  if (d.lengthSqr() < 0.001) {
                     d = new Vec3(1.0, 0.0, 0.0);
                  }

                  Vfx.lightning(level, h, h.add(d.normalize().scale(3.0 + this.getRandom().nextDouble() * 3.0)), P, 1);
               }
            }
         })
         .hit((b, level, t, i) -> {
            Vec3 g = this.overloadAt;
            Vec3 h = this.heart();
            boolean last = this.phase() >= 3;

            for (LivingEntity e : this.inCircle(level, g, 13.0)) {
               if (!this.los(level, h, e.position().add(0.0, e.getBbHeight() * 0.6, 0.0))) {
                  Vfx.glint(level, e.getEyePosition(), P.core(), 0.8);
               } else {
                  this.magic(level, e, last ? 44.0F : 40.0F);
                  shoveFrom(e, h, 2.2, 0.5);
                  this.afflict(e, MobEffects.SLOWNESS, 60, 1);
               }
            }

            int n = last ? 16 : 12;

            for (int k = 0; k < n; k++) {
               double a = k * Math.PI * 2.0 / n;
               double dy = k % 2 == 0 ? -0.05 : -0.15;
               BossBolt.shoot(level, this, BossBolt.Style.RUNE, h, new Vec3(Math.cos(a) * 0.6, dy, Math.sin(a) * 0.6), this.dmg(8.0F)).life(40);
            }

            Vfx.impactFrame(level, h, 40.0, P.glow(), 0.8F);
            Vfx.nova(level, h, P, 13.0);
            Vfx.heavyImpact(level, g, P, 2.2);
            Vfx.pillar(level, g, P, 22.0, 2.4);
         });
   }

   private BossAction monolithRain() {
      return BossAction.of("monolith_rain", 2.8, 1.0, 1.4, 1.8, 2.2)
         .phase(3)
         .range(0.0, 32.0)
         .cooldown(260)
         .weight(9)
         .anyTarget()
         .tags("ranged aoe anti_air")
         .telegraph(0.2, 2.2)
         .utility((b, t, c) -> c.kitingTicks > 60 ? 1.5F : 1.0F)
         .start((b, level, t, i) -> {
            for (int k = 0; k < 4; k++) {
               this.monoliths[k] = null;
            }
         })
         .tick((b, level, t, tick) -> {
            for (int k = 0; k < 4; k++) {
               int hit = 20 + 8 * k;
               if (tick == hit - 16) {
                  LivingEntity v = this.monolithVictim(k, t);
                  Vec3 p = this.floor(level, v != null ? this.predict(v, 16.0) : this.position());
                  this.monoliths[k] = p;
                  Vfx.telegraphCircle(level, p, 3.0, 16, P);
                  Vfx.runeCircle(level, p, 3.0, P, 16, 0.12F, Vfx.Rune.GLYPH);
               }

               Vec3 p = this.monoliths[k];
               if (p != null && tick >= hit - 8 && tick <= hit) {
                  Vfx.meteorTrail(level, p.add(0.0, 16.0 * (hit - tick) / 8.0, 0.0), new Vec3(0.0, -2.0, 0.0), P);
               }
            }
         })
         .hit((b, level, t, i) -> {
            Vec3 p = this.monoliths[i];
            if (p != null) {
               for (LivingEntity e : this.inCircle(level, p, 3.0, 1.0, 14.0)) {
                  boolean high = e.getY() > p.y + 3.0;
                  this.phys(level, e, 34.0F);
                  if (high) {
                     e.stopFallFlying();
                     Vec3 v = e.getDeltaMovement();
                     e.setDeltaMovement(v.x, -2.0, v.z);
                     e.syncVelocity = true;
                  } else {
                     shoveFrom(e, p, 1.2, 0.4);
                  }
               }

               Vfx.heavyImpact(level, p, P, 1.2);
               Vfx.groundCrack(level, p, 3.0, P);
               Vfx.pillar(level, p, P, 5.0, 1.0);
               Vfx.shards(level, p.add(0.0, 1.0, 0.0), P.core(), 12, 0.5);
            }
         });
   }

   private @Nullable LivingEntity monolithVictim(int k, @Nullable LivingEntity target) {
      List<ServerPlayer> e = this.engaged();
      if (k % 2 == 1 && e.size() >= 2) {
         List<ServerPlayer> others = new ArrayList<>();

         for (ServerPlayer p : e) {
            if (p != target) {
               others.add(p);
            }
         }

         if (!others.isEmpty()) {
            return (LivingEntity)others.get(this.getRandom().nextInt(others.size()));
         }
      }

      return target;
   }

   private BossAction sigilOfUnmaking() {
      return BossAction.of("sigil_of_unmaking", 5.0, 1.8, 2.6, 3.4, 4.2)
         .phase(3)
         .range(0.0, 48.0)
         .cooldown(900)
         .weight(7)
         .anyTarget()
         .tags("aoe ultimate")
         .superArmor()
         .telegraph(0.0, 4.2)
         .utility((b, t, c) -> this.noUltimates ? 0.0F : 1.0F)
         .start((b, level, t, i) -> {
            Vec3 c = this.arenaCenter(level, t);
            this.sigilC = c;
            this.sigilNodes = this.nodesAround(level, c);
            Vfx.telegraphCircle(level, c, 6.0, 36, P);
            Vfx.runeCircle(level, c, 12.0, P, 36, 0.03F, Vfx.Rune.GLYPH);
         })
         .tick((b, level, t, tick) -> {
            Vec3 c = this.sigilC;
            if (tick == 2) {
               this.blinkTo(c.add(0.0, this.hoverHeight() + 2.0, 0.0));
            }

            if (tick == 36) {
               Vfx.telegraphCircle(level, c, 15.0, 16, P);
               Vfx.ward(level, c, 6.0, P, 16);
            }

            if (tick == 52) {
               Vfx.telegraphCircle(level, c, 7.0, 16, P);
            }

            if (tick == 68) {
               Vfx.telegraphCircle(level, c, 15.0, 16, P);

               for (ArchonBoss.Node n : this.sigilNodes) {
                  Vfx.ward(level, n.ground(), 2.2, P, 16);
               }
            }
         })
         .hit((b, level, t, i) -> {
            Vec3 c = this.sigilC;
            switch (i) {
               case 0:
                  for (LivingEntity e : this.inCircle(level, c, 6.0, 1.0, 4.0)) {
                     this.magic(level, e, 30.0F);
                     this.afflict(e, MobEffects.SLOWNESS, 40, 1);
                  }

                  Vfx.nova(level, c, P, 6.0);
                  break;
               case 1:
                  for (LivingEntity e : this.inCircle(level, c, 15.0, 1.0, 4.0)) {
                     if (hd(e.position(), c) > 6.0) {
                        this.magic(level, e, 30.0F);
                     }
                  }

                  Vfx.shockwave(level, c, P, 15.0);

                  for (int k = 0; k < 8; k++) {
                     double a = k * Math.PI / 4.0;
                     Vfx.pillar(level, this.floor(level, c.add(Math.cos(a) * 11.0, 1.0, Math.sin(a) * 11.0)), P, 5.0, 1.0);
                  }
                  break;
               case 2:
                  for (LivingEntity e : this.inCircle(level, c, 7.0, 1.0, 4.0)) {
                     this.magic(level, e, 30.0F);
                  }

                  Vfx.nova(level, c, P, 7.0);
                  break;
               default:
                  for (LivingEntity e : this.inCircle(level, c, 15.0, 1.0, 4.0)) {
                     boolean safe = false;

                     for (ArchonBoss.Node n : this.sigilNodes) {
                        if (hd(e.position(), n.ground()) <= 2.2) {
                           safe = true;
                           break;
                        }
                     }

                     if (!safe) {
                        this.magic(level, e, 40.0F);
                        shoveFrom(e, c, 1.5, 0.4);
                     }
                  }

                  Vfx.impactFrame(level, this.heart(), 48.0, P.core(), 0.9F);
                  Vfx.heavyImpact(level, c, P, 2.5);
                  Vfx.nova(level, c, P, 15.0);
            }
         });
   }

   private BossAction heartcoreCollapse() {
      return BossAction.of("heartcore_collapse", 6.0, 4.6)
         .phase(3)
         .range(0.0, 64.0)
         .cooldown(20)
         .weight(0)
         .anyTarget()
         .tags("aoe ultimate")
         .telegraph(0.0, 4.6)
         .utility((b, t, c) -> 0.0F)
         .start((b, level, t, i) -> {
            this.collapseStart = this.getHealth();
            Vec3 c = this.arenaCenter(level, t);
            this.collapseC = c;
            this.blinkTo(c.add(0.0, 2.0, 0.0));
            Vfx.telegraphCircle(level, c, 16.0, 92, P);
            Vfx.vortex(level, c, 14.0, 8.0, P, 92);
            Vfx.runeCircle(level, c, 15.0, P, 92, -0.08F, Vfx.Rune.ARCANE);
            Vfx.chargeUp(level, this.heart(), P, 92);
            Vfx.chainLightning(level, this.chainPoints(level), P);
         })
         .tick((b, level, t, tick) -> {
            if (tick <= 88) {
               Vec3 h = this.heart();
               if (tick % 2 == 0) {
                  this.drawIn(h, 20.0, 0.09);
               }

               if (tick % 6 == 0) {
                  List<ArchonBoss.Node> nodes = this.latticePoints(level, t);
                  if (!nodes.isEmpty()) {
                     Vfx.lightning(level, h, nodes.get(this.getRandom().nextInt(nodes.size())).top(), P, 2);
                  }
               }

               if (tick >= 20 && !this.fractured && this.collapseStart - this.getHealth() >= this.getMaxHealth() * 0.03F) {
                  this.fracture(level, true);
               }
            }
         })
         .hit((b, level, t, i) -> {
            Vec3 h = this.heart();

            for (LivingEntity e : BossFx.victims(level, this, h, 16.0)) {
               this.magic(level, e, 48.0F);
               shoveFrom(e, h, 3.0, 0.6);
               this.afflict(e, MobEffects.WEAKNESS, 200, 0);
            }

            Vfx.impactFrame(level, h, 64.0, P.core(), 1.0F);
            Vfx.nova(level, h, P, 16.0);
            Vfx.heavyImpact(level, this.collapseC, P, 3.0);
            Vfx.explosion(level, h, P, 4.0);
         })
         .end((b, level, t, i) -> {
            if (i == -2 && !this.fractured && this.isAlive()) {
               this.fracture(level, false);
            }
         });
   }

   private void fracture(ServerLevel level, boolean cancel) {
      if (!this.fractured) {
         this.fractured = true;
         this.noUltimates = true;
         BossAction m = this.currentMove();
         if (cancel && m != null && "heartcore_collapse".equals(m.anim())) {
            this.cancelMove(level, 30);
         }

         BossFx.staggerBurst(level, this);
         BossFx.wardShatter(level, this);
         Vec3 h = this.heart();

         for (LivingEntity e : BossFx.victims(level, this, h, 8.0)) {
            this.magic(level, e, 14.0F);
            shoveFrom(e, h, 1.0, 0.4);
         }

         Vfx.nova(level, h, P, 8.0);
         Vfx.shards(level, h, 16777215, 20, 0.6);
         this.buff(160, 1.5F, 1.0F);
      }
   }

   @Override
   protected void customServerAiStep(ServerLevel world) {
      this.ensureArena(world);
      super.customServerAiStep(world);
      if (!this.isDeadOrDying() && !this.inIntro()) {
         BossFx.current(this);
         LivingEntity target = this.getTarget();
         if (target == null) {
            if (++this.lonely == 100) {
               this.epoch++;
               this.latticeUntil = 0;
               this.lattice = null;
               this.prisons.clear();
            }
         } else {
            this.lonely = 0;
         }

         if (this.collapsePending && !this.staggered() && !this.invulnerableNow()) {
            BossAction m = this.currentMove();
            if (m == null || !m.has(BossAction.Tag.ULTIMATE)) {
               this.collapsePending = false;
               this.forceAction(world, "heartcore_collapse");
            }
         }

         this.tickLattice(world);
         this.tickPrisons(world);
         if (this.tickCount % 20 == 0) {
            this.tether(world);
         }

         if (this.warded() && this.tickCount % 10 == 0) {
            Vfx.ring(world, this.floor(world, this.position()), P.glow(), 3.0, 12);
            if (this.tickCount % 20 == 0) {
               Vfx.shieldBubble(world, this.local(0.0, 2.5, 0.0), 3.4, P, 22);
            } else {
               Vfx.flare(world, this.heart(), P.glow(), 3.2, 10);
            }
         }

         this.ambient(world);
         if (target != null) {
            this.hazards(world, target);
         }
      }
   }

   private void hazards(ServerLevel level, LivingEntity target) {
      int p = this.phase();
      boolean free = !this.invulnerableNow();
      if (p >= 1) {
         if (this.sentryTimer > 0) {
            this.sentryTimer--;
         }

         if (this.sentryTimer <= 0 && free) {
            int interval = p == 1 ? 240 : (p == 2 ? 180 : 140);
            this.sentryTimer = this.enraged() ? Math.round(interval * 0.6F) : interval;
            this.sentries(level, target, Math.min(3, p));
         }
      }

      if (p >= 3) {
         if (this.pulseTimer > 0) {
            this.pulseTimer--;
         }

         BossAction m = this.currentMove();
         if (this.pulseTimer <= 0 && free && (m == null || !m.has(BossAction.Tag.ULTIMATE))) {
            this.pulseTimer = 200;
            this.gravityPulse(level);
         }
      }
   }

   private void sentries(ServerLevel level, LivingEntity target, int count) {
      List<ArchonBoss.Node> nodes = new ArrayList<>();

      for (ArchonBoss.Node n : this.latticePoints(level, target)) {
         if (hd(n.ground(), target.position()) >= 4.0) {
            nodes.add(n);
         }
      }

      nodes.sort(Comparator.comparingDouble(nx -> hd(nx.ground(), target.position())));

      for (int k = 0; k < Math.min(count, nodes.size()); k++) {
         ArchonBoss.Node n = nodes.get(k);
         Vec3 top = n.top();
         Vec3 aim = this.predict(target, 30.0).add(0.0, 1.0, 0.0);
         Vec3 dir = aim.subtract(top);
         if (!(dir.lengthSqr() < 0.001)) {
            Vfx.chargeUp(level, top, P, 30);
            Vfx.telegraphLine(level, n.ground(), this.flat(dir), dir.horizontalDistance() + 6.0, 1.2, 30, P);
            Vec3 nd = dir.normalize();
            double len = dir.length() + 6.0;
            this.later(30, () -> {
               Vec3 end = this.rayEnd(level, top, nd, len);

               for (LivingEntity e : this.onLine(level, top, end, 1.0)) {
                  this.magic(level, e, 12.0F);
                  this.afflict(e, MobEffects.SLOWNESS, 20, 0);
               }

               Vfx.beam(level, top, end, P, 0.6, 6);
               Vfx.lightning(level, top, end, P, 2);
               Vfx.impact(level, end, P, 0.8);
               BossSounds.play(level, top, "boss.archon.prism_lance.hit", 2.0F, 1.15F);
            });
         }
      }
   }

   private void gravityPulse(ServerLevel level) {
      Vfx.gather(level, this.heart(), P, 14.0, 20);
      Vfx.runeCircle(level, this.floor(level, this.position()), 14.0, P, 20, -0.1F, Vfx.Rune.GLYPH);
      this.later(20, () -> {
         BossFx.current(this);
         BossFx.pull(level, this, this.floor(level, this.position()), 14.0, 0.7);
      });
   }

   private void tether(ServerLevel level) {
      List<RealmMob> eyes = level.getEntitiesOfClass(
         RealmMob.class, this.getBoundingBox().inflate(16.0), mx -> mx.isAlive() && "arcane_eye".equals(mx.bossId()) && mx.distanceToSqr(this) <= 256.0
      );
      this.linkedEyes = eyes.size();
      Vec3 h = this.heart();

      for (int k = 0; k < Math.min(4, eyes.size()); k++) {
         RealmMob m = eyes.get(k);
         Vfx.beam(level, m.position().add(0.0, m.getBbHeight() * 0.5, 0.0), h, P, 0.2, 16);
      }
   }

   private int constructsNear() {
      if (this.tickCount - this.constructsAt >= 20) {
         this.constructsAt = this.tickCount;
         this.constructs = this.level().getEntitiesOfClass(RealmMob.class, this.getBoundingBox().inflate(24.0), LivingEntity::isAlive).size();
      }

      return this.constructs;
   }

   private void ambient(ServerLevel level) {
      int p = this.phase();
      int t = this.tickCount;
      if (p >= 1 && t % 20 == 0) {
         Vfx.flare(level, this.heart(), P.glow(), 1.6, 8);
      }

      if (p >= 2) {
         if (t % 20 == 10) {
            Vfx.glint(level, this.local(0.0, 4.5, -0.44), P.core(), 1.0);
         }

         if (t % 10 == 0) {
            Vfx.sparks(level, this.local(2.16, 1.6, -0.37), P.glow(), 3, 0.08, 0.12);
         }
      }

      if (p >= 3) {
         if (t % 10 == 5) {
            Vfx.lightning(level, this.heart(), this.local(this.getRandom().nextBoolean() ? 1.22 : -1.22, 3.2, 0.0), P, 0);
         }

         if (t % 10 == 0) {
            Vfx.embers(level, this.heart(), P.glow(), 3, 1.2);
         }

         if (t % 60 == 0) {
            Vfx.runeCircle(level, this.floor(level, this.position()), 3.0, P, 60, 0.03F, Vfx.Rune.GLYPH);
         }
      }
   }

   private void tickLattice(ServerLevel level) {
      ArchonBoss.Lattice l = this.lattice;
      if (l != null) {
         int age = this.tickCount - l.start();
         if (l.epoch() == this.epoch && age <= 80) {
            int s = l.ground().size();
            boolean outerLive = l.outer() && age >= 20;
            if (l.rotate() && age % 5 == 0) {
               List<Vec3> pts = latticeAt(l, age);

               for (int k = 0; k < s; k++) {
                  Vfx.beam(level, pts.get(k).add(0.0, 1.0, 0.0), pts.get((k + 2) % s).add(0.0, 1.0, 0.0), P, 0.6, 6);
                  if (outerLive) {
                     Vfx.beam(level, pts.get(k).add(0.0, 1.0, 0.0), pts.get((k + 1) % s).add(0.0, 1.0, 0.0), P, 0.45, 6);
                  }
               }
            } else if (!l.rotate() && l.outer() && age == 20) {
               List<Vec3> pts = l.ground();

               for (int k = 0; k < s; k++) {
                  Vfx.beam(level, pts.get(k).add(0.0, 1.0, 0.0), pts.get((k + 1) % s).add(0.0, 1.0, 0.0), P, 0.45, 60);
               }
            }

            if (age > 0 && age % 10 == 0) {
               List<Vec3> pts = latticeAt(l, age);
               double minX = Double.MAX_VALUE;
               double minY = Double.MAX_VALUE;
               double minZ = Double.MAX_VALUE;
               double maxX = -Double.MAX_VALUE;
               double maxY = -Double.MAX_VALUE;
               double maxZ = -Double.MAX_VALUE;

               for (Vec3 v : pts) {
                  minX = Math.min(minX, v.x);
                  minY = Math.min(minY, v.y);
                  minZ = Math.min(minZ, v.z);
                  maxX = Math.max(maxX, v.x);
                  maxY = Math.max(maxY, v.y);
                  maxZ = Math.max(maxZ, v.z);
               }

               AABB box = new AABB(minX - 2.0, minY - 1.0, minZ - 2.0, maxX + 2.0, maxY + 3.0, maxZ + 2.0);

               for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, this::victim)) {
                  boolean hit = false;

                  for (int k = 0; k < s && !hit; k++) {
                     hit = onEdge(e, pts.get(k), pts.get((k + 2) % s)) || outerLive && onEdge(e, pts.get(k), pts.get((k + 1) % s));
                  }

                  if (hit) {
                     this.magic(level, e, 8.0F);
                     this.afflict(e, MobEffects.SLOWNESS, 20, 0);
                     Vfx.sparks(level, e.position().add(0.0, 1.0, 0.0), P.core(), 6, 0.2, 0.14);
                  }
               }
            }
         } else {
            this.lattice = null;
         }
      }
   }

   private static boolean onEdge(LivingEntity e, Vec3 a, Vec3 b) {
      double lo = Math.min(a.y, b.y);
      double hi = Math.max(a.y, b.y);
      return !(e.getY() < lo - 0.5) && !(e.getY() > hi + 2.5) ? segDistH(e.getX(), e.getZ(), a, b) <= 1.2 : false;
   }

   private static List<Vec3> latticeAt(ArchonBoss.Lattice l, int age) {
      if (!l.rotate()) {
         return l.ground();
      }

      float ang = 36.0F * Mth.clamp(age, 0, 80) / 80.0F * (float) (Math.PI / 180.0);
      List<Vec3> out = new ArrayList<>(l.ground().size());

      for (Vec3 g : l.ground()) {
         Vec3 off = new Vec3(g.x - l.center().x, 0.0, g.z - l.center().z).yRot(ang);
         out.add(new Vec3(l.center().x + off.x, g.y, l.center().z + off.z));
      }

      return out;
   }

   private void tickPrisons(ServerLevel level) {
      if (!this.prisons.isEmpty()) {
         Iterator<ArchonBoss.Prison> it = this.prisons.iterator();

         while (it.hasNext()) {
            ArchonBoss.Prison p = it.next();
            if (p.epoch != this.epoch) {
               it.remove();
            } else if (this.tickCount < p.end) {
               if ((p.end - this.tickCount) % 4 == 0) {
                  for (LivingEntity e : p.caught) {
                     if (e.isAlive() && !p.broken.contains(e.getId()) && hd(e.position(), p.at) > 3.3) {
                        p.broken.add(e.getId());
                        this.magic(level, e, 14.0F);
                        this.afflict(e, MobEffects.SLOWNESS, 60, 3);
                        Vfx.explosion(level, e.position().add(0.0, 1.0, 0.0), P, 1.4);
                     }
                  }
               }
            } else {
               for (LivingEntity e : p.caught) {
                  if (e.isAlive() && !p.broken.contains(e.getId()) && hd(e.position(), p.at) <= 3.3) {
                     this.magic(level, e, 6.0F);
                     this.afflict(e, MobEffects.LEVITATION, 10, 0);
                  }
               }

               Vfx.nova(level, p.at, P, 3.2);
               it.remove();
            }
         }
      }
   }

   @Override
   protected void onPhaseChange(ServerLevel level, int phase) {
      this.epoch++;
      this.latticeUntil = 0;
      this.lattice = null;
      this.sentryTimer = 128;

      for (int p = this.phaseSeen + 1; p <= phase; p++) {
         switch (p) {
            case 1:
               this.setCooldown("call_constructs", 200);
               this.setCooldown("pentagram_lattice", 240);
               break;
            case 2:
               this.setCooldown("arcane_overload", 400);
               break;
            case 3:
               this.setCooldown("sigil_of_unmaking", 160);
               this.setCooldown("monolith_rain", 60);
               this.pulseTimer = 200;
         }
      }

      this.phaseSeen = Math.max(this.phaseSeen, phase);
   }

   @Override
   protected void phaseBurst(ServerLevel level, int phase) {
      Vec3 g = this.floor(level, this.position());
      Vec3 h = this.heart();
      BossFx.slam(level, this, g, 7.0, 6.0F, 1.6);
      BossFx.knockback(level, this, g, 9.0, 1.2);
      Vfx.shake(level, this.position(), 40.0, 1.0, 18);
      if (phase <= 1) {
         Vfx.shards(level, h, P.core(), 30, 0.7);
         Vfx.nova(level, h, P, 7.0);
         Vfx.runeCircle(level, g, 8.0, P, 60, 0.04F, Vfx.Rune.GLYPH);
         Vfx.chainLightning(level, this.chainPoints(level), P);
         this.summon(level, "arcane_eye", 2);
      } else if (phase == 2) {
         Vfx.radialFlash(level, h, 40.0, P.glow(), 0.5F);
         Vfx.slashArc(level, this.local(0.0, 2.2, 0.0), this.getYRot(), 0.0F, 0.0F, 6.0, P, -360.0F);
         Vfx.glint(level, this.eyePos().add(0.0, 1.0, 0.0), P.core(), 2.5);
         this.summon(level, "arcane_eye", 2);
         this.summon(level, "rune_golem", 1);
      } else {
         Vfx.impactFrame(level, h, 48.0, P.glow(), 0.9F);
         Vfx.heavyImpact(level, g, P, 2.0);
         Vfx.nova(level, g, P, 9.0);
         Vfx.chainLightning(level, this.chainPoints(level), P);
         this.summon(level, "arcane_eye", 3);
         this.summon(level, "rune_golem", 1);
      }
   }

   private void summon(ServerLevel level, String kind, int count) {
      boolean eye = "arcane_eye".equals(kind);

      for (int k = 0; k < count; k++) {
         this.spawnConstruct(level, kind, this.clearSpot(level, this.position(), 2.0, 6.0, eye ? 4.0 : 2.0), eye ? 1.5 : 0.0, this.getTarget());
      }
   }

   private Vec3 clearSpot(ServerLevel level, Vec3 center, double minR, double maxR, double clear) {
      Vec3 best = center;
      double bestD = -1.0;

      for (int k = 0; k < 8; k++) {
         Vec3 p = BossFx.around(level, this, center, minR, maxR);
         double d = nearestPlayer(level, p);
         if (d >= clear) {
            return p;
         }

         if (d > bestD) {
            bestD = d;
            best = p;
         }
      }

      return best;
   }

   private static boolean playerNear(ServerLevel level, Vec3 p, double clear) {
      return nearestPlayer(level, p) < clear;
   }

   private static double nearestPlayer(ServerLevel level, Vec3 p) {
      double best = Double.MAX_VALUE;

      for (Player pl : level.players()) {
         if (!pl.isSpectator() && Math.abs(pl.getY() - p.y) < 4.0) {
            best = Math.min(best, hd(pl.position(), p));
         }
      }

      return best;
   }

   @Override
   protected void onStagger(ServerLevel level) {
      this.epoch++;
      this.latticeUntil = 0;
      this.lattice = null;
      Vec3 h = this.heart();

      for (int k = 0; k < 3; k++) {
         Vfx.lightning(level, h, BossFx.around(level, this, this.position(), 2.0, 4.0), P, 2);
      }

      Vfx.shards(level, h, P.core(), 20, 0.6);
   }

   @Override
   protected void onEnrage(ServerLevel level) {
      Vfx.auraRing(level, this.floor(level, this.position()), P, 4.0);
      List<Vec3> tops = new ArrayList<>();

      for (ArchonBoss.Node n : this.latticePoints(level, this.getTarget())) {
         tops.add(n.top());
      }

      Vfx.chainLightning(level, tops, P);
      Vfx.radialFlash(level, this.heart(), 40.0, P.glow(), 0.5F);
   }

   @Override
   protected void desperation(ServerLevel level) {
      this.collapsePending = true;
   }

   @Override
   public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
      if (this.linkedEyes > 0 && this.isAlive()) {
         amount *= 1.0F - 0.06F * Math.min(3, this.linkedEyes);
      }

      return super.hurtServer(world, source, amount);
   }

   public ProjectileDeflection deflection(Projectile projectile) {
      if (this.warded() && this.isAlive()) {
         if (this.level() instanceof ServerLevel level) {
            Vfx.glint(level, projectile.position(), P.core(), 1.2);
            Vfx.sparks(level, projectile.position(), P.glow(), 6, 0.25, 0.14);
         }

         return ProjectileDeflection.REVERSE;
      } else {
         return super.deflection(projectile);
      }
   }

   @Override
   public void die(DamageSource damageSource) {
      super.die(damageSource);
      this.epoch++;
      this.lattice = null;
      this.latticeUntil = 0;
      this.prisons.clear();
      this.beamDir = null;
      this.linkedEyes = 0;
      this.collapsePending = false;
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput view) {
      super.addAdditionalSaveData(view);
      BossAction m = this.currentMove();
      boolean collapsing = m != null && "heartcore_collapse".equals(m.anim()) && !this.fractured;
      view.putBoolean("ArchonCollapse", this.collapsePending || collapsing);
      view.putBoolean("ArchonFractured", this.fractured);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput view) {
      super.readAdditionalSaveData(view);
      this.collapsePending = view.getBooleanOr("ArchonCollapse", false);
      this.fractured = view.getBooleanOr("ArchonFractured", false);
      this.noUltimates = this.fractured;
      this.phaseSeen = this.phase();
   }

   @Override
   protected void onIntroTick(ServerLevel level, int ticksLeft) {
      int t = this.introTicks() - ticksLeft;
      Vec3 below = this.groundBelow(level, this.position(), 6);
      Vec3 a = this.anchorAt;
      if (a != null && hd(a, this.position()) < 3.0 && a.y <= this.getY() + 0.5) {
         below = new Vec3(this.getX(), a.y, this.getZ());
      }

      if (below != null && t < 40) {
         this.setDeltaMovement(0.0, Mth.clamp((below.y + 2.1 - this.getY()) * 0.2, -0.12, 0.12), 0.0);
      }

      Vec3 g = below != null ? below : this.position();
      if (t == 1) {
         Vfx.runeCircle(level, this.anchor(level), 6.0, P, 139, 0.02F, Vfx.Rune.GLYPH);
      }

      if (t >= 2 && t <= 30 && t % 4 == 2) {
         Vec3 h = this.heart();

         for (ArchonBoss.Node n : this.pillars) {
            Vfx.beam(level, n.top(), h, P, 0.35, 5);
         }
      }

      switch (t) {
         case 40:
            Vfx.impact(level, this.local(0.0, 1.8, 0.0), P, 0.8);
            break;
         case 60:
            Vfx.heavyImpact(level, g, P, 0.8);
            Vfx.shake(level, this.position(), 20.0, 0.4, 8);
            break;
         case 64:
            Vfx.flare(level, this.local(-1.2, 3.2, 0.0), P.glow(), 1.6, 8);
            Vfx.shards(level, this.local(-1.2, 3.2, 0.0), P.core(), 8, 0.35);
            break;
         case 70:
            Vfx.flare(level, this.local(1.2, 3.2, 0.0), P.glow(), 1.6, 8);
            Vfx.shards(level, this.local(1.2, 3.2, 0.0), P.core(), 8, 0.35);
            break;
         case 86:
            Vfx.flare(level, this.palm(-1), P.glow(), 1.4, 8);
            Vfx.flare(level, this.palm(1), P.glow(), 1.4, 8);
            break;
         case 100:
            Vfx.glint(level, this.eyePos(), P.core(), 1.5);
            Vfx.ring(level, this.eyePos(), P.glow(), 1.2, 10);
            break;
         case 104:
            Vfx.nova(level, this.heart(), P, 6.0);
            Vfx.radialFlash(level, this.heart(), 32.0, P.glow(), 0.5F);
            break;
         case 118:
            Vfx.flare(level, this.eyePos(), 16777215, 2.2, 8);
            Vfx.shake(level, this.position(), 32.0, 0.6, 14);
            Vfx.runeCircle(level, g, 9.0, P, 22, 0.08F, Vfx.Rune.ARCANE);
            break;
         case 130:
            Vfx.auraRing(level, g, P, 4.0);
      }
   }

   @Override
   protected void onDeathTick(ServerLevel level, int tick) {
      Vec3 below = this.groundBelow(level, this.position(), 8);
      if (below != null && tick <= 60) {
         this.setDeltaMovement(0.0, Mth.clamp((below.y + 2.4 - this.getY()) * 0.2, -0.2, 0.2), 0.0);
      }

      Vec3 g = below != null ? below : this.position();
      Vec3 h = this.heart();
      if (tick <= 12 && (tick == 1 || tick % 6 == 0)) {
         Vfx.lightning(level, h, this.floor(level, g.add(this.getRandom().nextGaussian() * 2.5, 0.5, this.getRandom().nextGaussian() * 2.5)), P, 2);
      }

      if (tick == 24) {
         Vfx.impact(level, g, P, 0.8);
         Vfx.shockwave(level, g, P, 3.0);
      }

      if (tick == 26 || tick == 30) {
         Vfx.impact(level, g.add(this.getRandom().nextGaussian(), 0.0, this.getRandom().nextGaussian()), P, 0.6);
      }

      if (tick >= 28 && tick <= 40 && tick % 4 == 0) {
         double a = tick * 0.9;
         Vfx.impact(level, g.add(Math.cos(a) * 1.2, 0.0, Math.sin(a) * 1.2), P, 0.5);
      }

      if (tick == 46 && this.phase() >= 2) {
         Vec3 stab = this.floor(level, this.local(2.1, 0.5, 0.0));
         Vfx.heavyImpact(level, stab, P, 0.6);
         Vfx.groundCrack(level, stab, 1.5, P);
      }

      if (tick == 56 || tick == 62) {
         Vfx.impact(level, this.floor(level, this.palm(tick == 56 ? -1 : 1)), P, 0.6);
      }

      if (tick == 76 || tick == 82) {
         Vec3 s = this.local(tick == 76 ? -1.22 : 1.22, 2.0, 0.0);
         Vfx.impact(level, this.floor(level, s), P, 0.7);
         Vfx.shards(level, s, P.core(), 8, 0.35);
      }

      if (tick == 108) {
         Vfx.flare(level, this.eyePos(), 16777215, 1.2, 6);
         Vfx.smoke(level, this.eyePos(), P.smoke(), 6, 0.4, 1.0);
      }

      if (tick == 112) {
         Vfx.chargeUp(level, h, P, 24);
      }

      if (tick > 112 && tick < 136 && tick % 4 == 0) {
         Vfx.lightning(level, h, this.floor(level, g.add(this.getRandom().nextGaussian() * 2.0, 0.5, this.getRandom().nextGaussian() * 2.0)), P, 2);
      }

      if (tick == 136) {
         Vfx.impactFrame(level, h, 40.0, P.core(), 0.8F);
         Vfx.nova(level, h, P, 10.0);
         Vfx.explosion(level, h, P, 3.5);
         Vfx.chainLightning(level, this.chainPoints(level), P);
      }

      if (tick == 148) {
         Vfx.heavyImpact(level, g, P, 1.0);
         Vfx.groundCrack(level, g, 4.0, P);
         Vfx.smoke(level, g.add(0.0, 0.5, 0.0), P.smoke(), 10, 1.5, 1.4);
      }

      if (tick == 150) {
         Vfx.runeCircle(level, g, 4.0, P, 20, -0.02F, Vfx.Rune.GLYPH);
      }
   }

   private void ensureArena(ServerLevel level) {
      if (this.anchorAt == null) {
         BlockPos home = this.home();
         Vec3 base = home != null ? Vec3.atBottomCenterOf(home) : this.floor(level, this.position());
         this.anchorAt = base;
         BlockPos c = home != null ? home : BlockPos.containing(base);
         Map<Long, BlockPos> columns = new HashMap<>();

         for (BlockPos p : BlockPos.betweenClosed(c.offset(-12, -4, -12), c.offset(12, 6, 12))) {
            if (level.getBlockState(p).is(MysticalBosses.RUNE_PILLAR)) {
               double d = hd(Vec3.atBottomCenterOf(p), base);
               if (d >= 4.0 && d <= 14.0) {
                  long key = BlockPos.asLong(p.getX(), 0, p.getZ());
                  BlockPos had = columns.get(key);
                  if (had == null || p.getY() > had.getY()) {
                     columns.put(key, p.immutable());
                  }
               }
            }
         }

         List<BlockPos> found = new ArrayList<>(columns.values());
         found.sort(Comparator.comparingDouble(px -> Math.abs(hd(Vec3.atBottomCenterOf(px), base) - 9.0)));
         this.pillars.clear();
         if (found.size() >= 5) {
            List<BlockPos> five = new ArrayList<>(found.subList(0, 5));
            five.sort(Comparator.comparingDouble(px -> Math.atan2(px.getZ() + 0.5 - base.z, px.getX() + 0.5 - base.x)));

            for (BlockPos p : five) {
               this.pillars.add(this.realNode(level, p, base));
            }
         } else {
            double a0 = found.isEmpty() ? -Math.PI / 2 : Math.atan2(found.get(0).getZ() + 0.5 - base.z, found.get(0).getX() + 0.5 - base.x);

            for (int k = 0; k < 5; k++) {
               double a = a0 + k * Math.PI * 2.0 / 5.0;
               Vec3 v = new Vec3(base.x + Math.cos(a) * 9.0, base.y, base.z + Math.sin(a) * 9.0);
               BlockPos near = null;

               for (BlockPos p : found) {
                  if (hd(Vec3.atBottomCenterOf(p), v) < 3.0) {
                     near = p;
                  }
               }

               this.pillars.add(near != null ? this.realNode(level, near, base) : this.virtualNode(level, v));
            }
         }
      }
   }

   private ArchonBoss.Node realNode(ServerLevel level, BlockPos p, Vec3 base) {
      BlockPos q = p;

      for (int i = 0; i < 3 && !level.getBlockState(q.above()).isAir(); i++) {
         q = q.above();
      }

      Vec3 top = Vec3.atBottomCenterOf(q.above()).add(0.0, 0.5, 0.0);
      double px = p.getX() + 0.5;
      double pz = p.getZ() + 0.5;
      Vec3 toward = this.flat(new Vec3(base.x - px, 0.0, base.z - pz));
      Vec3 g = this.groundBelow(level, new Vec3(px + toward.x * 1.4, p.getY() + 0.5, pz + toward.z * 1.4), 8);
      return new ArchonBoss.Node(new Vec3(px, g != null ? g.y : base.y, pz), top, true);
   }

   private ArchonBoss.Node virtualNode(ServerLevel level, Vec3 at) {
      Vec3 g = this.floor(level, at.add(0.0, 1.0, 0.0));
      return new ArchonBoss.Node(g, g.add(0.0, 2.5, 0.0), false);
   }

   private Vec3 anchor(ServerLevel level) {
      this.ensureArena(level);
      return this.anchorAt != null ? this.anchorAt : this.position();
   }

   private Vec3 anchorOrTarget(ServerLevel level, LivingEntity t) {
      Vec3 a = this.anchor(level);
      return hd(t.position(), a) <= 16.0 ? a : t.position();
   }

   private List<ArchonBoss.Node> latticePoints(ServerLevel level, @Nullable LivingEntity target) {
      Vec3 a = this.anchor(level);
      return target != null && !(hd(target.position(), a) <= 16.0) ? this.pentagon(level, this.floor(level, target.position())) : this.pillars;
   }

   private List<ArchonBoss.Node> nodesAround(ServerLevel level, Vec3 center) {
      return hd(center, this.anchor(level)) < 3.0 ? this.pillars : this.pentagon(level, center);
   }

   private List<ArchonBoss.Node> pentagon(ServerLevel level, Vec3 c) {
      List<ArchonBoss.Node> out = new ArrayList<>(5);

      for (int k = 0; k < 5; k++) {
         double a = k * Math.PI * 2.0 / 5.0 - (Math.PI / 2);
         out.add(this.virtualNode(level, new Vec3(c.x + Math.cos(a) * 9.0, c.y, c.z + Math.sin(a) * 9.0)));
      }

      return out;
   }

   private Vec3 arenaCenter(ServerLevel level, @Nullable LivingEntity t) {
      Vec3 a = this.anchor(level);
      return t != null && !(hd(t.position(), a) <= 20.0) ? this.floor(level, this.position()) : a;
   }

   private List<Vec3> chainPoints(ServerLevel level) {
      List<Vec3> out = new ArrayList<>();
      out.add(this.heart());

      for (ArchonBoss.Node n : this.latticePoints(level, this.getTarget())) {
         out.add(n.top());
      }

      return out;
   }

   private Vec3 local(double right, double up, double fwd) {
      double yaw = this.getYRot() * (float) (Math.PI / 180.0);
      double sin = Math.sin(yaw);
      double cos = Math.cos(yaw);
      return new Vec3(this.getX() - sin * fwd - cos * right, this.getY() + up, this.getZ() + cos * fwd - sin * right);
   }

   private Vec3 heart() {
      return this.local(0.0, 2.75, 0.49);
   }

   private Vec3 eyePos() {
      return this.local(0.0, 3.9, 0.4);
   }

   private Vec3 palm(int side) {
      return this.local(1.63 * side, 2.1, 0.12);
   }

   private Vec3 facing() {
      return Vec3.directionFromRotation(0.0F, this.getYRot());
   }

   private Vec3 flat(Vec3 v) {
      Vec3 h = new Vec3(v.x, 0.0, v.z);
      return h.lengthSqr() < 1.0E-6 ? this.facing() : h.normalize();
   }

   private static float yawOf(Vec3 d) {
      return (float)(Mth.atan2(d.z, d.x) * 180.0F / (float)Math.PI) - 90.0F;
   }

   private static float pitchOf(Vec3 d) {
      return (float)(-Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * 180.0F / (float)Math.PI);
   }

   private void face(Vec3 at) {
      if (!(hd(at, this.position()) < 0.05)) {
         float yaw = yawOf(at.subtract(this.position()));
         this.setYRot(yaw);
         this.setYBodyRot(yaw);
         this.setYHeadRot(yaw);
      }
   }

   private static double hd(Vec3 a, Vec3 b) {
      double dx = a.x - b.x;
      double dz = a.z - b.z;
      return Math.sqrt(dx * dx + dz * dz);
   }

   private int cd(int ticks) {
      return this.enraged() ? Math.round(ticks * 0.7F) : ticks;
   }

   private Vec3 floor(ServerLevel level, Vec3 at) {
      Vec3 g = this.groundBelow(level, at, 24);
      return g != null ? g : at;
   }

   private @Nullable Vec3 groundBelow(ServerLevel level, Vec3 at, int depth) {
      MutableBlockPos p = BlockPos.containing(at.x, at.y + 0.6, at.z).mutable();

      for (int i = 0; i <= depth; i++) {
         BlockState s = level.getBlockState(p);
         VoxelShape shape = s.getCollisionShape(level, p);
         if (!shape.isEmpty() || !s.getFluidState().isEmpty()) {
            double top = shape.isEmpty() ? 1.0 : Math.min(1.0, shape.max(Axis.Y));
            return new Vec3(at.x, p.getY() + top, at.z);
         }

         p.move(0, -1, 0);
      }

      return null;
   }

   private Vec3 rayEnd(ServerLevel level, Vec3 from, Vec3 dir, double len) {
      return level.clip(new ClipContext(from, from.add(dir.scale(len)), Block.COLLIDER, Fluid.NONE, this)).getLocation();
   }

   private boolean los(ServerLevel level, Vec3 from, Vec3 to) {
      return level.clip(new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, this)).getType() == Type.MISS;
   }

   private void descend(@Nullable LivingEntity t, double above) {
      if (t != null) {
         Vec3 v = this.getDeltaMovement();
         this.setDeltaMovement(v.x, Mth.clamp((t.getY() + above - this.getY()) * 0.25, -0.3, 0.3), v.z);
      }
   }

   private boolean victim(LivingEntity e) {
      return e != this && e.isAlive() && !(e instanceof MysticalBoss) && !(e instanceof Player p && (p.isCreative() || p.isSpectator()));
   }

   private List<LivingEntity> inCircle(ServerLevel level, Vec3 c, double r) {
      return this.inCircle(level, c, r, 1.0, 3.5);
   }

   private List<LivingEntity> inCircle(ServerLevel level, Vec3 c, double r, double below, double above) {
      AABB box = new AABB(c.x - r - 1.0, c.y - below, c.z - r - 1.0, c.x + r + 1.0, c.y + above + 0.01, c.z + r + 1.0);
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, this::victim)) {
         double rr = r + e.getBbWidth() * 0.5;
         double dx = e.getX() - c.x;
         double dz = e.getZ() - c.z;
         if (dx * dx + dz * dz <= rr * rr && e.getY() >= c.y - below && e.getY() <= c.y + above) {
            out.add(e);
         }
      }

      return out;
   }

   private List<LivingEntity> inArc(ServerLevel level, double range, float halfAngle, float yaw) {
      Vec3 f = Vec3.directionFromRotation(0.0F, yaw);
      AABB box = new AABB(
         this.getX() - range - 1.0, this.getY() - 3.0, this.getZ() - range - 1.0, this.getX() + range + 1.0, this.getY() + 5.0, this.getZ() + range + 1.0
      );
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, this::victim)) {
         if (!(e.getY() < this.getY() - 3.0) && !(e.getY() > this.getY() + 5.0)) {
            double dx = e.getX() - this.getX();
            double dz = e.getZ() - this.getZ();
            double d = Math.sqrt(dx * dx + dz * dz);
            if (!(d > range + e.getBbWidth() * 0.5)
               && (d < this.getBbWidth() * 0.5 + 0.6 || Math.toDegrees(Math.acos(Mth.clamp((dx * f.x + dz * f.z) / d, -1.0, 1.0))) <= halfAngle)) {
               out.add(e);
            }
         }
      }

      return out;
   }

   private List<LivingEntity> onLine(ServerLevel level, Vec3 from, Vec3 to, double halfWidth) {
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(halfWidth + 1.5), this::victim)) {
         Vec3 c = e.position().add(0.0, e.getBbHeight() * 0.5, 0.0);
         if (segDist(c, from, to) <= halfWidth + 0.3) {
            out.add(e);
         }
      }

      return out;
   }

   private List<LivingEntity> onStrip(ServerLevel level, Vec3 a, Vec3 b, double halfWidth, double below, double above) {
      double lo = Math.min(a.y, b.y) - below;
      double hi = Math.max(a.y, b.y) + above;
      AABB box = new AABB(
         Math.min(a.x, b.x) - halfWidth - 1.0,
         lo,
         Math.min(a.z, b.z) - halfWidth - 1.0,
         Math.max(a.x, b.x) + halfWidth + 1.0,
         hi + 0.01,
         Math.max(a.z, b.z) + halfWidth + 1.0
      );
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, this::victim)) {
         if (e.getY() >= lo && e.getY() <= hi && segDistH(e.getX(), e.getZ(), a, b) <= halfWidth + 0.3) {
            out.add(e);
         }
      }

      return out;
   }

   private static double segDist(Vec3 p, Vec3 a, Vec3 b) {
      Vec3 ab = b.subtract(a);
      double len2 = ab.lengthSqr();
      double t = len2 < 1.0E-6 ? 0.0 : Mth.clamp(p.subtract(a).dot(ab) / len2, 0.0, 1.0);
      return p.distanceTo(a.add(ab.scale(t)));
   }

   private static double segDistH(double px, double pz, Vec3 a, Vec3 b) {
      double abx = b.x - a.x;
      double abz = b.z - a.z;
      double len2 = abx * abx + abz * abz;
      double t = len2 < 1.0E-6 ? 0.0 : Mth.clamp(((px - a.x) * abx + (pz - a.z) * abz) / len2, 0.0, 1.0);
      double dx = px - (a.x + abx * t);
      double dz = pz - (a.z + abz * t);
      return Math.sqrt(dx * dx + dz * dz);
   }

   private boolean magic(ServerLevel level, LivingEntity e, float base) {
      return e.hurtServer(level, this.damageSources().indirectMagic(this, this), this.dmg(base));
   }

   private boolean phys(ServerLevel level, LivingEntity e, float base) {
      return e.hurtServer(level, this.damageSources().mobAttack(this), this.dmg(base));
   }

   private void physMagic(ServerLevel level, LivingEntity e, float physBase, float magicBase) {
      this.phys(level, e, physBase);
      e.setInvulnerableTime(0);
      this.magic(level, e, magicBase);
   }

   private void afflict(LivingEntity e, Holder<MobEffect> effect, int ticks, int amplifier) {
      e.addEffect(new MobEffectInstance(effect, ticks, amplifier), this);
   }

   private static void shoveFrom(LivingEntity e, Vec3 from, double horizontal, double up) {
      Vec3 away = new Vec3(e.getX() - from.x, 0.0, e.getZ() - from.z);
      away = away.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : away.normalize();
      e.push(away.x * horizontal, up, away.z * horizontal);
      e.syncVelocity = true;
   }

   private static void hitFx(ServerLevel level, LivingEntity e, double power) {
      Vfx.impact(level, e.position().add(0.0, e.getBbHeight() * 0.55, 0.0), P, power);
   }

   private void guardBreak(ServerLevel level, LivingEntity e, int ticks) {
      ItemStack s = e.getItemBlockingWith();
      if (s != null) {
         if (e instanceof Player p) {
            p.getCooldowns().addCooldown(s, ticks);
         }

         e.stopUsingItem();
         Vfx.shards(level, e.getEyePosition(), P.core(), 10, 0.4);
      }
   }

   private void drawIn(Vec3 to, double radius, double strength) {
      for (ServerPlayer p : this.engaged()) {
         double d = hd(p.position(), to);
         if (!(d > radius) && !(d < 1.0)) {
            p.push((to.x - p.getX()) / d * strength, 0.0, (to.z - p.getZ()) / d * strength);
            p.syncVelocity = true;
         }
      }
   }

   private static int buffs(LivingEntity e) {
      int n = 0;

      for (MobEffectInstance inst : e.getActiveEffects()) {
         if (((MobEffect)inst.getEffect().value()).isBeneficial() && !inst.isAmbient()) {
            n++;
         }
      }

      return n;
   }

   private static boolean eatingOrDrinking(LivingEntity e) {
      if (!e.isUsingItem()) {
         return false;
      }

      ItemUseAnimation use = e.getUseItem().getUseAnimation();
      return use == ItemUseAnimation.EAT || use == ItemUseAnimation.DRINK;
   }

   private void spawnConstruct(ServerLevel level, String kind, Vec3 at, double lift, @Nullable LivingEntity target) {
      EntityType<RealmMob> type = RealmMobs.type(kind);
      if (type != null) {
         RealmMob m = (RealmMob)type.create(level, EntitySpawnReason.MOB_SUMMONED);
         if (m != null) {
            m.snapTo(at.x, at.y + lift, at.z, this.getRandom().nextFloat() * 360.0F, 0.0F);
            if (target != null) {
               m.setTarget(target);
            }

            level.addFreshEntity(m);
            Vfx.pillar(level, at, P, 3.0, 0.8);
            Vfx.impact(level, at, P, 0.8);
         }
      }
   }

   private void scars(
      ServerLevel level, List<Vec3> spots, double r, int duration, float dps, @Nullable Holder<MobEffect> effect, int effectTicks, int amplifier
   ) {
      if (!spots.isEmpty() && !(r <= 0.0)) {
         double minX = Double.MAX_VALUE;
         double minY = Double.MAX_VALUE;
         double minZ = Double.MAX_VALUE;
         double maxX = -Double.MAX_VALUE;
         double maxY = -Double.MAX_VALUE;
         double maxZ = -Double.MAX_VALUE;

         for (Vec3 s : spots) {
            Vfx.runeCircle(level, s, r, P, duration, 0.02F, Vfx.Rune.GLYPH);
            minX = Math.min(minX, s.x);
            minY = Math.min(minY, s.y);
            minZ = Math.min(minZ, s.z);
            maxX = Math.max(maxX, s.x);
            maxY = Math.max(maxY, s.y);
            maxZ = Math.max(maxZ, s.z);
         }

         AABB box = new AABB(minX - r - 1.0, minY - 1.0, minZ - r - 1.0, maxX + r + 1.0, maxY + 2.5, maxZ + r + 1.0);

         for (int t = 20; t <= duration; t += 20) {
            this.later(t, () -> {
               for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, this::victim)) {
                  for (Vec3 s : spots) {
                     if (hd(e.position(), s) <= r && e.getY() >= s.y - 1.0 && e.getY() <= s.y + 2.5) {
                        if (dps > 0.0F) {
                           this.magic(level, e, dps);
                        }

                        if (effect != null) {
                           this.afflict(e, effect, effectTicks, amplifier);
                        }
                        break;
                     }
                  }
               }
            });
         }
      }
   }

   private void later(int delay, Runnable task) {
      int e = this.epoch;
      this.schedule(delay, () -> {
         if (e == this.epoch && this.isAlive()) {
            task.run();
         }
      });
   }

   static {
      for (int i = 0; i < 4; i++) {
         FORMS[i] = RawAnimation.begin().thenLoop("animation.archon.form_" + i);
      }

      SPIN = RawAnimation.begin().thenLoop("animation.archon.spin");
   }

   private record Lattice(Vec3 center, List<Vec3> ground, int start, int epoch, boolean outer, boolean rotate) {
   }

   private record Node(Vec3 ground, Vec3 top, boolean real) {
   }

   private static final class Prison {
      final Vec3 at;
      final List<LivingEntity> caught;
      final Set<Integer> broken = new HashSet<>();
      final int end;
      final int epoch;

      Prison(Vec3 at, List<LivingEntity> caught, int end, int epoch) {
         this.at = at;
         this.caught = caught;
         this.end = end;
         this.epoch = epoch;
      }
   }
}
