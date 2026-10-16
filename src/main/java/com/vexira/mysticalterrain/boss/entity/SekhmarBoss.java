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
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.DefaultAttributeContainer.Builder;
import net.minecraft.entity.boss.BossBar.Color;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.jspecify.annotations.Nullable;
import software.bernie.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

public class SekhmarBoss extends MysticalBoss {
   private static final BossSounds SOUNDS = BossSounds.of("sekhmar");
   private static final Vfx.Palette P = Vfx.Palette.SAND;
   private static final Vfx.Palette SUN = new Vfx.Palette(16777200, 16762944, 13150320).withDebris(Vfx.Debris.FEATHER);
   private static final Vfx.Palette SOUL = new Vfx.Palette(14745599, 4249840, 1714746).withDebris(Vfx.Debris.ASH);
   private static final Vfx.Palette ECLIPSE = new Vfx.Palette(16773312, 16742938, 788486).withDebris(Vfx.Debris.ASH);
   private static final int GOLD = 15251008;
   private static final int SAND_COLOR = 14203000;
   private static final DustParticleEffect DUST = new DustParticleEffect(14860926, 1.6F);
   private static final TrackedData<Boolean> ECLIPSED = DataTracker.registerData(SekhmarBoss.class, TrackedDataHandlerRegistry.BOOLEAN);
   private static final int[][] VENTS = new int[][]{{10, 0}, {0, 10}, {-10, 0}, {0, -10}};
   public static final float WIDTH = 2.4F;
   public static final float HEIGHT = 4.0F;
   public static final boolean FIRE_IMMUNE = false;
   private boolean feint;
   private boolean burrowed;
   private @Nullable Vec3d burrowAt;
   private @Nullable Vec3d pounceDir;
   private @Nullable Vec3d pounceLand;
   private Vec3d pounceFrom = Vec3d.ZERO;
   private double pounceSpeed;
   private @Nullable Vec3d strikeAt;
   private @Nullable LivingEntity curseVictim;
   private @Nullable LivingEntity spitVictim;
   private float beamCenter;
   private float beamYaw0;
   private float beamYaw1;
   private float beamPitch;
   private int beamWindow = -1;
   private final Set<Integer> beamHit = new HashSet<>();
   private @Nullable Vec3d laneFrom;
   private final List<Vec3d> lanes = new ArrayList<>();
   private final List<Vec3d> waveA = new ArrayList<>();
   private final List<Vec3d> waveB = new ArrayList<>();
   private Vec3d[] shade = new Vec3d[0];
   private float eclipseOffset;
   private int eclipseSign = 1;
   private boolean eclipseWardBroken;
   private final List<SekhmarBoss.Pit> pits = new ArrayList<>();
   private final List<SekhmarBoss.Shaft> shafts = new ArrayList<>();
   private final List<SekhmarBoss.Meteor> meteors = new ArrayList<>();
   private final List<SekhmarBoss.Scorch> scorches = new ArrayList<>();
   private final List<RealmMob> oath = new ArrayList<>();
   private int hazardLevel = -1;
   private int ventTimer;
   private int veilTimer;
   private int shaftGrace;
   private int shaftPause;
   private int ankhTicks;
   private int losBlockedTicks;
   private int extinguished;
   private int scarabCount;
   private int guardianCount;
   private @Nullable Vec3d fightOrigin;

   public static Builder attributes() {
      return MysticalBoss.bossAttributes(1000.0, 16.0, 0.22, 14.0);
   }

   public SekhmarBoss(EntityType<? extends HostileEntity> type, World level) {
      super(type, level, "sekhmar", Color.YELLOW);
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
      return 68;
   }

   @Override
   protected float[] phaseThresholds() {
      return new float[]{0.75F, 0.5F, 0.25F};
   }

   @Override
   public float poiseMax() {
      return this.getMaxHealth() * 0.14F;
   }

   @Override
   public int enrageTicks() {
      return 7200;
   }

   @Override
   protected double runSpeed() {
      return 1.7;
   }

   @Override
   protected double preferredDistance() {
      return this.phase() == 2 ? 5.0 : (this.phase() == 3 ? 4.5 : 4.0);
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
   protected boolean immune() {
      return this.burrowed;
   }

   @Override
   public int introTicks() {
      return 140;
   }

   @Override
   public int deathTicks() {
      return 160;
   }

   private Vec3d look() {
      return Vec3d.fromPolar(0.0F, this.getYaw());
   }

   private Vec3d front(double dist, double up) {
      return this.getEntityPos().add(this.look().multiply(dist)).add(0.0, up, 0.0);
   }

   private Vec3d right(double dist) {
      return Vec3d.fromPolar(0.0F, this.getYaw() + 90.0F).multiply(dist);
   }

   private Vec3d crookTip() {
      return this.front(1.1, 3.9).add(this.right(1.0));
   }

   private Vec3d crookBlade() {
      return this.front(1.8, 3.1).add(this.right(0.8));
   }

   private Vec3d flailTip() {
      return this.front(0.9, 3.8).subtract(this.right(1.0));
   }

   private Vec3d uraeusPos() {
      return this.front(1.3, 4.35);
   }

   private Vec3d sunDisc() {
      return this.front(-0.3, 4.8);
   }

   private Vec3d chest() {
      return this.front(0.9, 3.0);
   }

   private Vec3d eyes() {
      return this.front(0.9, 4.1);
   }

   private Vec3d anchor() {
      BlockPos home = this.home();
      if (home != null) {
         return Vec3d.ofBottomCenter(home);
      } else {
         return this.fightOrigin != null ? this.fightOrigin : this.getEntityPos();
      }
   }

   @Override
   protected void defineActions(List<BossAction> out) {
      this.defineMelee(out);
      this.defineMobility(out);
      this.defineRanged(out);
      this.defineSummons(out);
      this.defineUltimates(out);
   }

   private void defineMelee(List<BossAction> out) {
      out.add(
         BossAction.of("sphinx_rake", 1.3, 0.45, 0.85)
            .range(0.0, 5.0)
            .cooldown(50)
            .weight(14)
            .tags("melee")
            .telegraph(0.15, 0.85)
            .advance(0.15, 0.45, 0.22)
            .combo("crook_reap", 0.35F)
            .combo("flail_lash", 0.2F)
            .utility((b, t, c) -> c.dist < 4.5 ? 1.3F : 1.0F)
            .tick((b, level, target, t) -> {
               if (t == 3) {
                  Vfx.telegraphCone(level, this.getEntityPos(), this.getYaw() + 15.0F, 40.0F, 5.0, 6, P);
               } else if (t == 11) {
                  Vfx.telegraphCone(level, this.getEntityPos(), this.getYaw() - 15.0F, 40.0F, 5.0, 6, P);
               }
            })
            .hit((b, level, target, i) -> {
               List<LivingEntity> hit = this.cleave(level, 5.0, 45.0F, i == 0 ? 15.0F : -15.0F, 16.0F, 0.6);
               Vfx.claw(level, this.front(1.3, 1.3), this.getYaw() + (i == 0 ? 20 : -20), 15.0F, 4.2, P);
               Vfx.sparks(level, this.front(2.5, 0.5), P.glow(), 10, 0.35, 0.18);
               if (i == 1) {
                  for (LivingEntity e : hit) {
                     e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 0), this);
                  }
               }
            })
      );
      out.add(
         BossAction.of("crook_reap", 1.5, 0.8)
            .range(0.0, 6.5)
            .cooldown(90)
            .weight(12)
            .tags("melee")
            .telegraph(0.3, 0.8)
            .advance(0.3, 0.6, 0.15)
            .combo("flail_lash", 0.4F)
            .combo("sphinx_rake", 0.2F)
            .utility((b, t, c) -> c.dist > 3.5 ? 1.6F : 0.9F)
            .tick((b, level, target, t) -> {
               if (t == 6) {
                  Vfx.telegraphCone(level, this.getEntityPos(), this.getYaw(), 110.0F, 6.5, 10, P);
               }
            })
            .hit((b, level, target, i) -> {
               Vec3d pull = this.front(2.0, 0.0);

               for (LivingEntity e : this.inCone(level, this.getEntityPos(), this.getYaw(), 110.0F, 6.5, 4.5)) {
                  if (this.hurt(level, e, 20.0F, false, true)) {
                     Vec3d to = pull.subtract(e.getEntityPos());
                     if (this.hDist(e.getEntityPos()) > 3.0 && to.horizontalLengthSquared() > 0.25) {
                        Vec3d d = this.flat(to).multiply(1.1);
                        e.setVelocity(d.x, 0.25, d.z);
                        e.knockedBack = true;
                        Vfx.streaks(level, e.getEntityPos().add(0.0, 1.0, 0.0), this.flat(to).multiply(3.0), P, 6);
                     } else {
                        shove(e, this.flat(e.getEntityPos().subtract(this.getEntityPos())), 0.5, 0.25);
                     }
                  }
               }

               Vfx.slashArc(level, this.chest(), this.getYaw(), 5.0F, 0.0F, 6.0, P, 220.0F);
               Vfx.debris(level, this.front(3.0, 0.4), Vfx.Debris.ASH, 14203000, 14, 0.35);
            })
      );
      out.add(
         BossAction.of("flail_lash", 1.7, 0.6, 0.85, 1.1)
            .range(0.0, 5.5)
            .cooldown(80)
            .weight(12)
            .tags("melee")
            .telegraph(0.2, 1.1)
            .advance(0.4, 1.1, 0.1)
            .combo("sphinx_rake", 0.15F)
            .utility((b, t, c) -> c.targetBlocking ? 0.6F : 1.0F)
            .tick((b, level, target, t) -> {
               if (t == 4) {
                  Vfx.telegraphCone(level, this.getEntityPos(), this.getYaw(), 35.0F, 5.5, 8, P);
               } else if (t == 12 || t == 17) {
                  Vfx.telegraphCone(level, this.getEntityPos(), this.getYaw(), 35.0F, 5.5, 5, P);
               }
            })
            .hit((b, level, target, i) -> {
               List<LivingEntity> hit = this.cleave(level, 5.5, 35.0F, 0.0F, 14.0F, 0.3);
               float roll = i == 0 ? 45.0F : (i == 1 ? -45.0F : 90.0F);
               Vfx.slashArc(level, this.chest(), this.getYaw(), 10.0F, roll, 5.5, P, 150.0F);
               Vfx.sparks(level, this.front(3.0, 1.0), 15251008, 8, 0.4, 0.16);
               if (i == 2) {
                  for (LivingEntity e : hit) {
                     e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1), this);
                     e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 60, 0), this);
                  }

                  Vfx.groundCrack(level, groundAt(level, this.front(3.5, 0.0)), 1.6, P);
               }
            })
      );
      out.add(
         BossAction.of("regalia_combo", 1.4, 0.45, 0.9)
            .phase(1)
            .range(0.0, 6.5)
            .cooldown(150)
            .weight(12)
            .tags("melee")
            .telegraph(0.1, 0.9)
            .advance(0.2, 0.45, 0.25)
            .combo("verdict_swift", 0.8F)
            .combo("verdict_delayed", 0.8F)
            .utility((b, t, c) -> c.targetBlocking ? 1.8F : 1.0F)
            .tick((b, level, target, t) -> {
               if (t == 2) {
                  Vfx.telegraphLine(level, groundAt(level, this.front(1.0, 0.0)), this.look(), 6.5, 1.4, 7, P);
               } else if (t == 10) {
                  this.strikeAt = groundAt(level, this.front(3.0, 0.0));
                  Vfx.telegraphCircle(level, this.strikeAt, 2.5, 8, P);
               }
            })
            .hit((b, level, target, i) -> {
               if (i == 0) {
                  Vec3d f = this.look();

                  for (LivingEntity e : this.inLine(level, this.front(0.8, 0.0), f, 6.5, 0.9, 3.0)) {
                     if (this.hurt(level, e, 18.0F, false, true)) {
                        shove(e, f, 0.5, 0.2);
                     }
                  }

                  Vfx.slashArc(level, this.chest(), this.getYaw(), 0.0F, 0.0F, 6.5, P, 50.0F);
                  Vfx.streaks(level, this.chest(), f.multiply(6.0), P, 8);
               } else {
                  Vec3d at = this.strikeAt != null ? this.strikeAt : groundAt(level, this.front(3.0, 0.0));
                  BossFx.slam(level, this, at, 2.5, 20.0F, 0.8);
                  Vfx.heavyImpact(level, at, P, 0.7);
               }
            })
            .end((b, l, t, i) -> {
               this.strikeAt = null;
               this.feint = i == -1 && this.random.nextFloat() < 0.35F + 0.1F * this.phase();
            })
      );
      out.add(
         BossAction.of("verdict_swift", 1.1, 0.4)
            .phase(1)
            .range(0.0, 7.5)
            .cooldown(20)
            .weight(0)
            .tags("melee aoe guard_break")
            .superArmor()
            .telegraph(0.05, 0.4)
            .advance(0.05, 0.35, 0.25)
            .when((b, t) -> !this.feint)
            .tick((b, level, target, t) -> {
               if (t == 1) {
                  this.strikeAt = this.lead(level, target, 3.2, 1.75);
                  Vfx.telegraphCircle(level, this.strikeAt, 3.5, 7, P);
               }
            })
            .hit((b, level, target, i) -> {
               Vec3d at = this.strikeAt != null ? this.strikeAt : groundAt(level, this.front(3.2, 0.0));
               this.verdict(level, at, 3.5, 30.0F, 1.2, 0.0);
               Vfx.heavyImpact(level, at, P, 1.2);
               Vfx.groundCrack(level, at, 3.5, P);
               Vfx.slashArc(level, this.chest(), this.getYaw(), 0.0F, 90.0F, 4.0, SUN, 120.0F);
            })
            .end((b, l, t, i) -> this.strikeAt = null)
      );
      out.add(
         BossAction.of("verdict_delayed", 1.7, 1.0)
            .phase(1)
            .range(0.0, 7.5)
            .cooldown(20)
            .weight(0)
            .tags("melee aoe guard_break")
            .superArmor()
            .telegraph(0.05, 1.0)
            .advance(0.75, 0.95, 0.3)
            .when((b, t) -> this.feint)
            .tick((b, level, target, t) -> {
               if (t == 1) {
                  this.strikeAt = this.lead(level, target, 3.8, 1.5);
                  Vfx.telegraphCircle(level, this.strikeAt, 4.0, 19, P);
               } else if (t >= 5 && t <= 16 && (t - 5) % 3 == 0) {
                  Vfx.gather(level, this.crookTip(), SUN, 1.6, 6);
                  Vfx.smoke(level, this.crookTip().add(0.0, -0.6, 0.0), P.smoke(), 3, 0.3, 0.8);
               }
            })
            .hit((b, level, target, i) -> {
               Vec3d at = this.strikeAt != null ? this.strikeAt : groundAt(level, this.front(3.8, 0.0));
               this.verdict(level, at, 4.0, 34.0F, 0.4, 0.6);
               BossFx.launch(level, this, at, 4.0, 0.6);
               Vfx.heavyImpact(level, at, P, 1.5);
               Vfx.groundCrack(level, at, 4.2, P);
            })
            .end((b, l, t, i) -> this.strikeAt = null)
      );
      out.add(
         BossAction.of("tail_scourge", 1.2, 0.5)
            .range(0.0, 6.5)
            .cooldown(70)
            .weight(10)
            .tags("melee behind")
            .lockFacing()
            .telegraph(0.05, 0.5)
            .when((b, t) -> this.ctx().targetBehind || this.anyoneBehind(6.0))
            .tick((b, level, target, t) -> {
               if (t == 1) {
                  Vfx.telegraphCone(level, this.getEntityPos(), this.getYaw() + 180.0F, 80.0F, 6.0, 9, P);
               }
            })
            .hit((b, level, target, i) -> {
               for (LivingEntity e : this.inCone(level, this.getEntityPos(), this.getYaw() + 180.0F, 80.0F, 6.0, 4.0)) {
                  if (this.hurt(level, e, 22.0F, false, true)) {
                     shove(e, this.flat(e.getEntityPos().subtract(this.getEntityPos())), 1.6, 0.4);
                     if (this.phase() >= 2) {
                        e.setOnFireFor(3.0F);
                     }
                  }
               }

               Vec3d f = this.look();
               Vfx.slashArc(level, this.getEntityPos().add(0.0, 1.2, 0.0).subtract(f.multiply(1.5)), this.getYaw() + 180.0F, 0.0F, 0.0F, 5.5, P, 160.0F);
               Vfx.streaks(level, this.getEntityPos().add(0.0, 0.8, 0.0).subtract(f.multiply(2.0)), f.multiply(-4.0), P, 10);
            })
      );
      out.add(
         BossAction.of("colossus_stomp", 2.2, 1.3)
            .range(0.0, 6.0)
            .cooldown(170)
            .weight(9)
            .tags("melee aoe guard_break")
            .superArmor()
            .telegraph(0.4, 1.3)
            .combo("obelisk_fissure", 0.25F)
            .utility((b, t, c) -> (c.surroundCount >= 2 ? 1.8F : 1.0F) * (c.targetBlocking ? 1.5F : 1.0F))
            .tick((b, level, target, t) -> {
               if (t == 8) {
                  this.strikeAt = groundAt(level, this.front(2.2, 0.0));
                  Vfx.telegraphCircle(level, this.strikeAt, 5.5, 18, P);
                  Vfx.chargeUp(level, this.front(1.5, 4.5), P, 18);
               }
            })
            .hit((b, level, target, i) -> {
               Vec3d at = this.strikeAt != null ? this.strikeAt : groundAt(level, this.front(2.2, 0.0));

               for (LivingEntity e : this.column(level, at, 5.5, 4.0)) {
                  this.guardBreak(level, e);
               }

               BossFx.slam(level, this, at, 5.5, 38.0F, 1.4);
               BossFx.launch(level, this, at, 5.5, 0.5);
               Vfx.heavyImpact(level, at, P, 1.4);
               Vfx.groundCrack(level, at, 5.5, P);
               if (this.phase() >= 2) {
                  BossFx.wave(level, this, at, 9.0, 10, 12.0F, DUST, 1.0, true);
               }
            })
            .end((b, l, t, i) -> this.strikeAt = null)
      );
   }

   private void defineMobility(List<BossAction> out) {
      out.add(
         BossAction.of("sphinx_pounce", 1.7, 1.1)
            .range(6.0, 20.0)
            .cooldown(120)
            .weight(10)
            .tags("gap_closer melee")
            .lockFacing()
            .telegraph(0.2, 1.1)
            .combo("sphinx_rake", 0.3F)
            .combo("crook_reap", 0.25F)
            .utility((b, t, c) -> c.targetFlying ? 0.2F : (c.kitingTicks > 40 ? 1.5F : 1.0F))
            .tick((b, level, target, t) -> {
               if (t < 10 && target != null) {
                  this.faceTarget(target, 14.0F);
               }

               if (t == 4) {
                  Vec3d land = target != null ? this.groundUnder(level, this.predict(target, 18.0)) : groundAt(level, this.front(8.0, 0.0));
                  double reach = 15.95;
                  if (this.hDist(land) > reach) {
                     Vec3d at = this.getEntityPos().add(this.flat(land.subtract(this.getEntityPos())).multiply(reach));
                     land = this.groundUnder(level, new Vec3d(at.x, Math.max(this.getY(), land.y) + 1.0, at.z));
                  }

                  this.pounceLand = land;
                  double len = Math.max(1.0, Math.min(this.hDist(land) - 1.5, 12.0));
                  Vfx.telegraphLine(level, this.getEntityPos(), this.flat(land.subtract(this.getEntityPos())), len, 2.6, 18, P);
                  Vfx.telegraphCircle(level, land, 3.8, 18, P);
               } else if (t == 10 && this.pounceLand != null) {
                  this.pounceFrom = this.getEntityPos();
                  this.pounceDir = this.flat(this.pounceLand.subtract(this.getEntityPos()));
                  this.pounceSpeed = MathHelper.clamp((this.hDist(this.pounceLand) - 2.2) / 11.0, 0.35, 1.25);
                  this.face(this.yawTo(this.pounceLand));
               }

               if (t >= 10 && t <= 21 && this.pounceDir != null) {
                  this.setVelocity(this.pounceDir.x * this.pounceSpeed, this.getVelocity().y, this.pounceDir.z * this.pounceSpeed);
               }

               if (t == 21) {
                  Vfx.dashTrail(level, this.pounceFrom.add(0.0, 1.5, 0.0), this.getEntityPos().add(0.0, 1.5, 0.0), P);
               }
            })
            .hit((b, level, target, i) -> {
               BossFx.slam(level, this, this.getEntityPos().add(this.look().multiply(1.5)), 3.8, 26.0F, 1.0);
               Vfx.heavyImpact(level, this.getEntityPos(), P, 1.1);
               Vfx.claw(level, this.front(1.5, 1.2), this.getYaw(), 20.0F, 3.5, P);
            })
            .end((b, l, t, i) -> {
               this.pounceDir = null;
               this.pounceLand = null;
            })
      );
      out.add(
         BossAction.of("tomb_burrow", 2.2, 0.6, 1.5)
            .phase(1)
            .range(5.0, 32.0)
            .cooldown(280)
            .weight(8)
            .tags("mobility gap_closer")
            .superArmor()
            .telegraph(0.6, 1.5)
            .combo("crook_reap", 0.45F)
            .combo("tail_scourge", 0.3F)
            .utility((b, t, c) -> !c.targetUnreachable && this.losBlockedTicks <= 60 ? (c.kitingTicks > 60 ? 1.5F : (c.dist < 9.0 ? 0.3F : 1.0F)) : 3.0F)
            .start((b, level, target, i) -> {
               Vfx.vortex(level, this.getEntityPos(), 2.5, 3.0, P, 14);
               Vfx.groundCrack(level, this.getEntityPos(), 3.0, P);
            })
            .tick((b, level, target, t) -> {
               if (this.burrowed && this.burrowAt != null && t > 12 && t < 30 && t % 3 == 1) {
                  Vfx.debris(level, this.burrowAt.add(0.0, 0.2, 0.0), Vfx.Debris.ASH, 14203000, 8, 0.3);
                  Vfx.smoke(level, this.burrowAt, P.smoke(), 4, 1.4, 1.2);
                  Vfx.shake(level, this.burrowAt, 8.0, 0.3, 3);
               }
            })
            .hit((b, level, target, i) -> {
               if (i == 0) {
                  this.burrowed = true;
                  Vec3d to = target != null ? this.groundUnder(level, this.predict(target, 18.0)) : this.getEntityPos();
                  this.blinkTo(to);
                  this.burrowAt = this.getEntityPos();
                  Vfx.telegraphCircle(level, this.burrowAt, 3.0, 18, P);
               } else {
                  this.burrowed = false;
                  Vec3d at = this.burrowAt != null ? this.burrowAt : this.getEntityPos();

                  for (LivingEntity e : this.column(level, at, 3.0, 7.0)) {
                     if (this.hurt(level, e, 30.0F, false, true)) {
                        e.addVelocity(0.0, 1.1, 0.0);
                        e.knockedBack = true;
                     }
                  }

                  Vfx.pillar(level, at, P, 7.0, 1.6);
                  Vfx.heavyImpact(level, at, P, 1.2);
               }
            })
            .end((b, l, t, i) -> {
               this.burrowed = false;
               this.burrowAt = null;
            })
      );
      out.add(
         BossAction.of("wing_buffet", 1.4, 0.65)
            .phase(2)
            .range(0.0, 9.0)
            .cooldown(160)
            .weight(9)
            .tags("aoe anti_air")
            .telegraph(0.2, 0.65)
            .combo("sandspear_volley", 0.45F)
            .combo("sphinx_pounce", 0.3F)
            .utility((b, t, c) -> (c.surroundCount >= 2 ? 1.8F : 1.0F) * (c.dist < 3.5 ? 1.4F : 1.0F))
            .tick((b, level, target, t) -> {
               if (t == 4) {
                  Vfx.telegraphCone(level, this.getEntityPos(), this.getYaw(), 60.0F, 9.0, 9, P);
               }
            })
            .hit((b, level, target, i) -> {
               Vec3d f = this.look();

               for (LivingEntity e : this.inCone(level, this.getEntityPos(), this.getYaw(), 60.0F, 9.0, 6.0)) {
                  boolean air = !e.isOnGround() && BossContext.heightAboveGround(level, e) > 1.5;
                  boolean landed = this.hurt(level, e, 14.0F, false, true);
                  Vec3d away = this.flat(e.getEntityPos().subtract(this.getEntityPos()));
                  if (air) {
                     e.setVelocity(away.x * 0.4, -1.2, away.z * 0.4);
                     if (landed) {
                        e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20, 1), this);
                     }
                  } else {
                     e.addVelocity(away.x * 2.0, 0.3, away.z * 2.0);
                  }

                  e.knockedBack = true;
                  e.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 40, 0), this);
               }

               Vfx.streaks(level, this.chest().add(this.right(2.0)), f.multiply(6.0), P, 16);
               Vfx.streaks(level, this.chest().add(this.right(-2.0)), f.multiply(6.0), P, 16);
               Vfx.smoke(level, this.front(4.0, 1.0), P.smoke(), 14, 3.0, 2.2);
               Vfx.debris(level, this.front(2.0, 2.5), Vfx.Debris.FEATHER, 15779424, 14, 0.4);
            })
      );
   }

   private void defineRanged(List<BossAction> out) {
      out.add(
         BossAction.of("sandspear_volley", 1.5, 0.55, 0.8, 1.05)
            .range(5.0, 32.0)
            .cooldown(90)
            .weight(10)
            .tags("ranged")
            .telegraph(0.1, 1.05)
            .utility((b, t, c) -> c.kitingTicks > 40 ? 1.6F : (c.dist > 12.0 ? 1.3F : 1.0F))
            .tick((b, level, target, t) -> {
               if (t == 2) {
                  Vfx.chargeUp(level, this.flailTip(), P, 9);
               }
            })
            .hit((b, level, target, i) -> {
               if (target != null) {
                  Vec3d from = this.flailTip();
                  double dist = from.distanceTo(target.getEntityPos());
                  Vec3d aim = this.predict(target, dist / 1.3).add(0.0, target.getHeight() * 0.6, 0.0);
                  Vec3d side = this.flat(aim.subtract(from)).rotateY((float) (Math.PI / 2));
                  Vec3d a = i == 0 ? aim : aim.add(side.multiply(i == 1 ? 2.2 : -2.2));
                  if (this.phase() >= 2) {
                     this.sandspear(level, from, a.add(side.multiply(1.1)));
                     this.sandspear(level, from, a.add(side.multiply(-1.1)));
                  } else {
                     this.sandspear(level, from, a);
                  }

                  Vfx.flare(level, from, P.glow(), 1.6, 6);
               }
            })
      );
      out.add(
         BossAction.of("soul_curse", 1.6, 0.9)
            .range(0.0, 26.0)
            .cooldown(180)
            .weight(8)
            .tags("ranged punish")
            .telegraph(0.3, 0.9)
            .utility((b, t, c) -> this.losBlockedTicks > 40 ? 0.3F : (!c.targetHealing && !c.targetAiming ? 1.0F : 1.5F))
            .tick((b, level, target, t) -> {
               if (t == 6) {
                  LivingEntity v = this.punishTarget(target, 26.0);
                  this.curseVictim = v;
                  if (v != null) {
                     Vfx.runeCircle(level, groundAt(level, v.getEntityPos()), 1.6, SOUL, 12, 0.08F, Vfx.Rune.GLYPH);
                     BossFx.markTarget(level, this, v, 12);
                  }

                  Vfx.gather(level, this.crookTip(), SOUL, 1.8, 8);
               }
            })
            .hit((b, level, target, i) -> {
               LivingEntity v = this.alive(this.curseVictim, 30.0) ? this.curseVictim : target;
               this.curseVictim = null;
               if (v != null) {
                  this.curse(level, v);
               }
            })
            .end((b, l, t, i) -> this.curseVictim = null)
      );
      out.add(
         BossAction.of("uraeus_spit", 0.9, 0.4)
            .range(4.0, 28.0)
            .cooldown(100)
            .weight(5)
            .tags("ranged punish")
            .telegraph(0.05, 0.4)
            .utility((b, t, c) -> !c.targetHealing && !c.targetAiming ? 0.5F : 2.5F)
            .tick(
               (b, level, target, t) -> {
                  if (t == 1) {
                     LivingEntity v = this.punishTarget(target, 28.0);
                     this.spitVictim = v;
                     Vfx.glint(level, this.uraeusPos(), 16742976, 1.2);
                     if (v != null) {
                        Vfx.telegraphLine(
                           level,
                           this.getEntityPos(),
                           this.flat(v.getEntityPos().subtract(this.getEntityPos())),
                           Math.max(2.0, this.hDist(v.getEntityPos())),
                           0.8,
                           7,
                           SUN
                        );
                     }
                  }
               }
            )
            .hit((b, level, target, i) -> {
               LivingEntity v = this.alive(this.spitVictim, 32.0) ? this.spitVictim : target;
               this.spitVictim = null;
               if (v != null) {
                  Vec3d from = this.uraeusPos();
                  double dist = from.distanceTo(v.getEyePos());
                  Vec3d aim = this.predict(v, dist / 2.4).add(0.0, v.getHeight() * 0.6, 0.0);
                  Vec3d vel = aim.subtract(from).normalize().multiply(2.4);
                  this.spit(level, from, vel);
                  if (usingItem(v)) {
                     this.spit(level, from, vel.rotateY(0.14F));
                     this.spit(level, from, vel.rotateY(-0.14F));
                  }

                  Vfx.flare(level, from, SUN.glow(), 1.2, 5);
               }
            })
            .end((b, l, t, i) -> this.spitVictim = null)
      );
      out.add(
         BossAction.of("obelisk_fissure", 1.8, 0.9)
            .range(3.0, 22.0)
            .cooldown(130)
            .weight(10)
            .tags("ranged aoe")
            .telegraph(0.3, 0.9)
            .utility((b, t, c) -> c.kitingTicks <= 40 && this.losBlockedTicks <= 40 ? 1.0F : 1.5F)
            .tick((b, level, target, t) -> {
               if (t == 6) {
                  Vec3d base = target != null ? this.flat(this.predict(target, 12.0).subtract(this.getEntityPos())) : this.look();
                  float[] fan = this.phase() >= 2 ? new float[]{-28.0F, -14.0F, 0.0F, 14.0F, 28.0F} : new float[]{-14.0F, 0.0F, 14.0F};
                  this.laneFrom = groundAt(level, this.front(2.0, 0.0));
                  this.lanes.clear();

                  for (float a : fan) {
                     Vec3d d = base.rotateY(a * (float) (Math.PI / 180.0));
                     this.lanes.add(d);
                     Vfx.telegraphLine(level, this.laneFrom, d, 16.0, 1.6, 12, P);
                  }
               }
            })
            .hit((b, level, target, i) -> {
               Vec3d o = this.laneFrom != null ? this.laneFrom : this.getEntityPos();
               Set<Integer> hit = new HashSet<>();

               for (Vec3d d : List.copyOf(this.lanes)) {
                  for (int k = 0; k < 10; k++) {
                     Vec3d p = o.add(d.multiply(0.8 + k * 1.6));
                     Vec3d spot = new Vec3d(p.x, o.y, p.z);
                     boolean sound = k == 5;
                     if (k == 0) {
                        this.obelisk(level, spot, hit, sound);
                     } else {
                        this.schedule(k * 2, () -> this.obelisk(level, spot, hit, sound));
                     }
                  }
               }
            })
      );
      out.add(
         BossAction.of("quicksand_maw", 1.9, 1.0)
            .phase(1)
            .range(0.0, 24.0)
            .cooldown(240)
            .weight(8)
            .tags("ranged aoe")
            .telegraph(0.4, 1.0)
            .utility((b, t, c) -> this.pits.size() >= this.maxPits() ? 0.0F : (c.kitingTicks > 40 ? 1.4F : 1.0F))
            .tick((b, level, target, t) -> {
               if (t == 8) {
                  this.strikeAt = target != null ? this.groundUnder(level, this.predict(target, 12.0)) : groundAt(level, this.front(5.0, 0.0));
                  Vfx.telegraphCircle(level, this.strikeAt, 3.5, 12, P);
               }
            })
            .hit((b, level, target, i) -> {
               Vec3d c = this.strikeAt != null ? this.strikeAt : groundAt(level, this.front(5.0, 0.0));

               while (this.pits.size() >= this.maxPits()) {
                  this.pits.remove(0);
               }

               this.pits.add(new SekhmarBoss.Pit(c));
               Vfx.vortex(level, c, 3.5, 1.2, P, 160);
               Vfx.groundCrack(level, c, 3.5, P);
               Vfx.shockwave(level, c, P, 3.5);
            })
            .end((b, l, t, i) -> this.strikeAt = null)
      );
      out.add(
         BossAction.of("ra_sunbeam", 3.4, 1.3, 1.6, 1.9, 2.2, 2.5)
            .phase(2)
            .range(4.0, 26.0)
            .cooldown(260)
            .weight(8)
            .tags("ranged")
            .superArmor()
            .lockFacing()
            .telegraph(0.45, 2.5)
            .utility((b, t, c) -> (c.dist > 8.0 ? 1.5F : 0.8F) * (c.surroundCount >= 2 ? 1.3F : 1.0F))
            .start((b, level, target, i) -> {
               float sign = this.random.nextBoolean() ? 1.0F : -1.0F;
               this.beamCenter = target != null ? this.yawTo(this.predict(target, 10.0)) : this.getYaw();
               this.beamYaw0 = this.beamCenter - 55.0F * sign;
               this.beamYaw1 = this.beamCenter + 55.0F * sign;
               this.beamPitch = 0.0F;
               this.beamWindow = -1;
               this.beamHit.clear();
            })
            .tick(
               (b, level, target, t) -> {
                  if (t == 9) {
                     Vfx.telegraphCone(level, this.getEntityPos(), this.beamCenter, 55.0F, 24.0, 17, SUN);
                     Vfx.chargeUp(level, this.sunDisc(), SUN, 17);
                  }

                  if (t >= 9 && t <= 25) {
                     this.face(MathHelper.stepUnwrappedAngleTowards(this.getYaw(), this.beamYaw0, 9.0F));
                  }

                  if (t == 26) {
                     this.face(this.beamYaw0);
                     this.beamPitch = target != null
                        ? MathHelper.clamp(pitchTo(this.crookBlade(), target.getEntityPos().add(0.0, target.getHeight() * 0.6, 0.0)), -15.0F, 15.0F)
                        : 0.0F;
                  }

                  if (t >= 26 && t <= 50) {
                     this.sunbeam(level, t);
                  }
               }
            )
            .end((b, l, t, i) -> {
               this.beamWindow = -1;
               this.beamHit.clear();
            })
      );
      out.add(
         BossAction.of("solar_pillars", 2.2, 1.3, 1.8)
            .phase(1)
            .range(0.0, 32.0)
            .cooldown(220)
            .weight(9)
            .tags("ranged aoe anti_air")
            .telegraph(0.5, 1.8)
            .utility((b, t, c) -> c.targetUnreachable ? 2.0F : 1.0F)
            .tick((b, level, target, t) -> {
               if (t == 10) {
                  this.waveA.clear();
                  this.waveB.clear();
                  Vec3d a = target != null ? this.groundUnder(level, this.predict(target, 16.0)) : groundAt(level, this.front(6.0, 0.0));
                  this.waveA.add(a);

                  for (int k = 0; k < 3; k++) {
                     this.waveA.add(this.near(level, a, 1.5, 5.0));
                  }

                  for (Vec3d p : this.waveA) {
                     Vfx.telegraphCircle(level, p, 1.8, 16, SUN);
                  }

                  if (this.phase() >= 2) {
                     for (ServerPlayerEntity p : this.engaged()) {
                        if (p != target && this.waveB.size() < 3 && p.squaredDistanceTo(this) < 1024.0) {
                           this.waveB.add(this.groundUnder(level, this.predict(p, 26.0)));
                        }
                     }

                     Vec3d c = target != null ? target.getEntityPos() : this.getEntityPos();

                     while (this.waveB.size() < 3) {
                        this.waveB.add(this.near(level, c, 3.0, 7.0));
                     }

                     for (Vec3d p : this.waveB) {
                        Vfx.telegraphCircle(level, p, 1.8, 26, SUN);
                     }
                  }
               }
            })
            .hit((b, level, target, i) -> {
               if (i == 1 && this.waveB.isEmpty()) {
                  Vfx.flare(level, this.flailTip(), SUN.glow(), 1.6, 6);
               } else {
                  for (Vec3d p : List.copyOf(i == 0 ? this.waveA : this.waveB)) {
                     this.sunColumn(level, p);
                  }
               }
            })
      );
   }

   private void defineSummons(List<BossAction> out) {
      out.add(
         BossAction.of("scarab_tide", 2.0, 1.1)
            .range(0.0, 32.0)
            .cooldown(380)
            .weight(6)
            .tags("summon")
            .anyTarget()
            .utility((b, t, c) -> this.scarabCount >= 10 ? 0.0F : 1.0F)
            .tick((b, level, target, t) -> {
               if (t >= 4 && t <= 16 && t % 3 == 1) {
                  Vfx.gather(level, this.flailTip(), P, 2.2, 7);
               }

               if (t == 12) {
                  this.strikeAt = groundAt(level, this.front(1.5, 0.0));
                  Vfx.telegraphCircle(level, this.strikeAt, 3.0, 10, P);
               }
            })
            .hit((b, level, target, i) -> {
               Vec3d at = this.strikeAt != null ? this.strikeAt : groundAt(level, this.front(1.5, 0.0));
               Vfx.shockwave(level, at, P, 4.5);
               Vfx.groundCrack(level, at, 3.0, P);
               BossFx.slam(level, this, at, 3.0, 4.0F, 0.6);
               this.summon(level, "scarab", Math.min(4 + 2 * this.phase(), 10 - this.count(level, "scarab")), 6.0);
            })
            .end((b, l, t, i) -> this.strikeAt = null)
      );
      out.add(
         BossAction.of("tomb_legion", 2.6, 1.5)
            .phase(1)
            .range(0.0, 32.0)
            .cooldown(640)
            .weight(6)
            .tags("summon")
            .anyTarget()
            .superArmor()
            .utility((b, t, c) -> this.guardianCount >= 3 ? 0.0F : 1.0F)
            .tick((b, level, target, t) -> {
               if (t == 6) {
                  Vfx.runeCircle(level, this.getEntityPos(), 7.0, P, 24, 0.04F, Vfx.Rune.GLYPH);
               }
            })
            .hit((b, level, target, i) -> {
               this.summon(level, "tomb_guardian", Math.min(this.phase() >= 3 ? 3 : 2, 3 - this.count(level, "tomb_guardian")), 7.0);
               Vfx.heavyImpact(level, groundAt(level, this.front(2.0, 0.0)), P, 0.9);
            })
      );
      out.add(
         BossAction.of("ankh_ward", 1.8, 0.9)
            .range(0.0, 40.0)
            .cooldown(600)
            .weight(6)
            .tags("defensive")
            .anyTarget()
            .utility((b, t, c) -> this.shield() > 0.0F ? 0.0F : (this.getHealth() / this.getMaxHealth() < 0.9F ? 1.2F : 0.5F))
            .tick((b, level, target, t) -> {
               if (t == 2) {
                  Vfx.runeCircle(level, this.getEntityPos(), 3.5, SUN, 20, 0.05F, Vfx.Rune.SOLAR);
               }
            })
            .hit((b, level, target, i) -> {
               this.shield(this.getMaxHealth() * (this.phase() >= 3 ? 0.16F : 0.12F), 200);
               Vfx.ward(level, this.getEntityPos(), 3.2, SUN, 200);
               this.ankhTicks = 200;
            })
      );
   }

   private void defineUltimates(List<BossAction> out) {
      out.add(
         BossAction.of("wrath_of_ra", 4.6, 2.6, 3.4)
            .phase(2)
            .range(0.0, 40.0)
            .cooldown(1000)
            .weight(6)
            .tags("aoe ultimate")
            .anyTarget()
            .superArmor()
            .telegraph(0.4, 2.6)
            .utility((b, t, c) -> c.engagedPlayers >= 2 ? 1.4F : 1.0F)
            .tick((b, level, target, t) -> {
               if (t == 8) {
                  Vfx.chargeUp(level, this.sunDisc(), SUN, 44);
                  Vfx.runeCircle(level, this.getEntityPos(), 14.0, SUN, 60, 0.03F, Vfx.Rune.SOLAR);
                  Vfx.telegraphCircle(level, this.getEntityPos(), 14.0, 44, SUN);
                  this.shade = this.shadePoints(level);

                  for (Vec3d s : this.shade) {
                     Vfx.ward(level, s, 2.2, SOUL, 44);
                  }
               }
            })
            .hit((b, level, target, i) -> {
               Vec3d at = this.getEntityPos();
               if (i == 1) {
                  BossFx.wave(level, this, at, 16.0, 14, 22.0F, DUST, 1.2, true);
               } else {
                  Vfx.nova(level, at, SUN, 14.0);
                  Vfx.impactFrame(level, at, 32.0, 16774336, 0.8F);
                  Vfx.heavyImpact(level, at, SUN, 2.5);
                  Vec3d sun = this.sunDisc();

                  for (LivingEntity e : this.column(level, at, 14.0, 6.0)) {
                     if (this.inShade(e) || !this.sees(level, sun, e)) {
                        Vfx.glint(level, e.getEyePos(), SOUL.glow(), 0.8);
                     } else if (this.hurt(level, e, 55.0F, false, true)) {
                        e.setOnFireFor(6.0F);
                     }
                  }
               }
            })
            .end((b, l, t, i) -> this.shade = new Vec3d[0])
      );
      out.add(
         BossAction.of("sunfall", 5.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0)
            .phase(3)
            .range(0.0, 40.0)
            .cooldown(720)
            .weight(7)
            .tags("ranged aoe ultimate")
            .anyTarget()
            .superArmor()
            .telegraph(0.5, 4.0)
            .tick((b, level, target, t) -> {
               if (t >= 14 && t <= 64 && (t - 14) % 10 == 0) {
                  List<Vec3d> pts = new ArrayList<>();

                  for (ServerPlayerEntity p : this.engaged()) {
                     if (pts.size() < 4 && p.squaredDistanceTo(this) < 1600.0) {
                        pts.add(this.groundUnder(level, this.predict(p, 16.0)));
                     }
                  }

                  if (pts.isEmpty() && target != null) {
                     pts.add(this.groundUnder(level, this.predict(target, 16.0)));
                  }

                  pts.add(this.near(level, this.getEntityPos(), 4.0, 12.0));

                  for (Vec3d pt : pts) {
                     Vfx.telegraphCircle(level, pt, 2.6, 16, SUN);
                     double h = ceilingAbove(level, pt, 14);
                     double a = this.random.nextDouble() * Math.PI * 2.0;
                     this.meteors.add(new SekhmarBoss.Meteor(pt.add(Math.cos(a) * 2.0, Math.max(2.0, h - 0.6), Math.sin(a) * 2.0), pt, this.age + 16, 16));
                  }
               }
            })
            .hit((b, level, target, i) -> Vfx.flare(level, this.crookTip(), SUN.glow(), 1.6, 5))
      );
      out.add(
         BossAction.of("eclipse", 8.0, 2.0, 3.0, 4.0, 5.0, 6.8)
            .phase(3)
            .range(0.0, 60.0)
            .cooldown(99999)
            .weight(0)
            .tags("aoe ultimate")
            .anyTarget()
            .superArmor()
            .lockFacing()
            .telegraph(5.2, 6.8)
            .start((b, level, target, i) -> {
               this.dataTracker.set(ECLIPSED, true);
               this.shield(this.getMaxHealth() * 0.15F, 160);
               this.eclipseWardBroken = false;
               this.eclipseOffset = this.random.nextFloat() * 90.0F;
               this.eclipseSign = this.random.nextBoolean() ? 1 : -1;

               for (ServerPlayerEntity p : this.engaged()) {
                  p.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 200, 0), this);
               }

               Vfx.radialFlash(level, this.getEntityPos(), 40.0, 1707012, 0.6F);
               Vfx.runeCircle(level, this.getEntityPos(), 18.0, ECLIPSE, 160, -0.03F, Vfx.Rune.SOLAR);
            })
            .tick((b, level, target, t) -> this.eclipseTick(level, t))
            .hit((b, level, target, i) -> {
               Vec3d c = this.getEntityPos();
               if (i < 4) {
                  Vfx.flash(level, c, 24.0, 16742938, 0.25F);
                  Vfx.shake(level, c, 24.0, 0.4, 6);
               } else {
                  Vfx.nova(level, c, ECLIPSE, 18.0);
                  Vfx.impactFrame(level, c, 40.0, 16773312, 1.0F);
                  Vfx.heavyImpact(level, c, ECLIPSE, 3.0);
                  float base = this.eclipseWardBroken ? 35.0F : 70.0F;

                  for (LivingEntity e : this.column(level, c, 18.0, 8.0)) {
                     if (!(this.hDist(e.getEntityPos()) <= 3.5)) {
                        if (this.hurt(level, e, base, false, true)) {
                           e.setOnFireFor(8.0F);
                        }

                        shove(e, this.flat(e.getEntityPos().subtract(c)), 1.8, 0.5);
                     }
                  }
               }
            })
            .end((b, l, t, i) -> {
               this.dataTracker.set(ECLIPSED, false);
               if (i == -1 && this.eclipseWardBroken) {
                  this.extinguished = 100;
                  this.buff(100, 1.5F, 1.0F);
                  BossFx.staggerBurst(l, this);
               }
            })
      );
   }

   private List<LivingEntity> cleave(ServerWorld level, double reach, float halfAngle, float yawOffset, float base, double push) {
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : this.inCone(level, this.getEntityPos(), this.getYaw() + yawOffset, halfAngle, reach, 4.5)) {
         if (this.hurt(level, e, base, false, true)) {
            shove(e, this.flat(e.getEntityPos().subtract(this.getEntityPos())), push, 0.25 + push * 0.15);
            Vfx.impact(level, e.getEntityPos().add(0.0, e.getHeight() * 0.5, 0.0), P, 0.7);
            out.add(e);
         }
      }

      return out;
   }

   private void verdict(ServerWorld level, Vec3d at, double radius, float base, double push, double up) {
      for (LivingEntity e : this.column(level, at, radius, 4.0)) {
         this.guardBreak(level, e);
         if (this.hurt(level, e, base, false, true)) {
            shove(e, this.flat(e.getEntityPos().subtract(at)), push, 0.3 + up * 0.2);
         }
      }
   }

   private Vec3d lead(ServerWorld level, @Nullable LivingEntity target, double reach, double maxAdvance) {
      Vec3d dir = this.look();
      double adv = maxAdvance;
      if (target != null) {
         dir = this.flat(target.getEntityPos().subtract(this.getEntityPos()));
         adv = MathHelper.clamp(this.hDist(target.getEntityPos()) - (this.getWidth() * 0.5 + target.getWidth() * 0.5 + 0.6), 0.0, maxAdvance);
      }

      return groundAt(level, this.getEntityPos().add(dir.multiply(reach + adv)));
   }

   private void sandspear(ServerWorld level, Vec3d from, Vec3d aim) {
      BossBolt.shoot(level, this, BossBolt.Style.SAND, from, aim.subtract(from).normalize().multiply(1.3), this.dmg(12.0F))
         .aoe(1.2)
         .effect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 0));
   }

   private void spit(ServerWorld level, Vec3d from, Vec3d velocity) {
      BossBolt.shoot(level, this, BossBolt.Style.STAR, from, velocity, this.dmg(14.0F)).aoe(0.8).effect(new StatusEffectInstance(StatusEffects.POISON, 80, 1));
   }

   private void curse(ServerWorld level, LivingEntity v) {
      Vec3d from = this.crookTip();
      Vec3d eye = v.getEyePos();
      BlockHitResult r = level.raycast(new RaycastContext(from, eye, ShapeType.COLLIDER, FluidHandling.NONE, this));
      if (r.getType() != Type.MISS) {
         Vfx.beam(level, from, r.getPos(), SOUL, 0.6, 6);
         Vfx.impact(level, r.getPos(), SOUL, 0.6);
      } else {
         Vfx.beam(level, from, eye, SOUL, 0.6, 6);
         Vfx.flare(level, eye, SOUL.glow(), 2.8, 10);
         float base = 16.0F;
         if (v.isUsingItem()) {
            v.clearActiveItem();
            base += 8.0F;
         }

         if (this.hurt(level, v, base, true, true)) {
            v.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 100, 1), this);
            v.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 120, 0), this);
         }

         if (this.phase() >= 1) {
            List<Vec3d> points = new ArrayList<>();
            points.add(from);
            points.add(eye);

            for (ServerPlayerEntity p : this.engaged()) {
               if (points.size() >= 4) {
                  break;
               }

               if (p != v && !(p.squaredDistanceTo(v) > 64.0) && this.sees(level, eye, p)) {
                  points.add(p.getEyePos());
                  if (this.hurt(level, p, 10.0F, true, true)) {
                     p.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 60, 0), this);
                  }
               }
            }

            if (points.size() > 2) {
               Vfx.chainLightning(level, points, SOUL);
            }
         }
      }
   }

   private void obelisk(ServerWorld level, Vec3d p, Set<Integer> hit, boolean sound) {
      Vec3d g = floorBelow(level, p.add(0.0, 2.0, 0.0), 5);
      if (g != null) {
         Vfx.pillar(level, g, P, 2.5, 0.8);
         Vfx.shards(level, g, P.core(), 5, 0.45);
         if (sound) {
            BossSounds.play(level, g, "boss.sekhmar.obelisk_fissure.hit", 1.2F, 1.1F);
         }

         for (LivingEntity e : this.column(level, g, 1.1, 3.0)) {
            if (hit.add(e.getId()) && this.hurt(level, e, 18.0F, false, true)) {
               e.addVelocity(0.0, 0.7, 0.0);
               e.knockedBack = true;
            }
         }
      }
   }

   private void sunbeam(ServerWorld level, int t) {
      float yaw = MathHelper.lerp((t - 26) / 24.0F, this.beamYaw0, this.beamYaw1);
      this.face(yaw);
      Vec3d from = this.crookBlade();
      Vec3d dir = Vec3d.fromPolar(this.beamPitch, yaw);
      Vec3d end = this.clipEnd(level, from, from.add(dir.multiply(24.0)));
      Vfx.beam(level, from, end, SUN, 0.8);
      if ((t - 26) % 4 == 0) {
         Vec3d g = floorBelow(level, end.subtract(dir.multiply(0.4)), 6);
         if (g != null) {
            Vfx.groundCrack(level, g, 1.2, SUN);
         }
      }

      int window = Math.min(4, (t - 26) / 6);
      if (window != this.beamWindow) {
         this.beamWindow = window;
         this.beamHit.clear();
      }

      for (LivingEntity e : level.getEntitiesByClass(LivingEntity.class, new Box(from, end).expand(2.5), x -> this.foe(x) && touches(x, from, end, 1.0, 2.2))) {
         if (!this.beamHit.contains(e.getId()) && this.sees(level, from, e)) {
            this.beamHit.add(e.getId());
            if (this.hurt(level, e, 16.0F, false, true)) {
               e.setOnFireFor(4.0F);
            }
         }
      }
   }

   private void sunColumn(ServerWorld level, Vec3d p) {
      double h = ceilingAbove(level, p, 14);
      Vfx.beam(level, p.add(0.0, h, 0.0), p, SUN, 1.8, 10);
      Vfx.pillar(level, p, SUN, h, 1.8);
      Vfx.heavyImpact(level, p, SUN, 0.6);

      for (LivingEntity e : this.column(level, p, 1.8, h)) {
         if (this.hurt(level, e, 26.0F, false, true)) {
            e.setOnFireFor(5.0F);
         }
      }
   }

   private Vec3d[] shadePoints(ServerWorld level) {
      List<Vec3d> out = new ArrayList<>();
      double off = this.random.nextDouble() * Math.PI * 2.0;

      for (int k = 0; k < 3; k++) {
         double a = off + k * Math.PI * 2.0 / 3.0;

         for (double r = 10.0; r >= 7.0; r--) {
            Vec3d g = floorBelow(level, new Vec3d(this.getX() + Math.cos(a) * r, this.getY() + 1.5, this.getZ() + Math.sin(a) * r), 5);
            if (g != null && Math.abs(g.y - this.getY()) < 3.0 && open(level, g)) {
               out.add(g);
               break;
            }
         }
      }

      return out.toArray(new Vec3d[0]);
   }

   private boolean inShade(LivingEntity e) {
      for (Vec3d s : this.shade) {
         double dx = e.getX() - s.x;
         double dz = e.getZ() - s.z;
         if (dx * dx + dz * dz <= 4.840000000000001) {
            return true;
         }
      }

      return false;
   }

   private void eclipseTick(ServerWorld level, int t) {
      if (!this.eclipseWardBroken && this.shield() <= 0.0F) {
         this.eclipseWardBroken = true;
         Vfx.nova(level, this.chest(), SUN, 4.0);
      }

      Vec3d c = this.getEntityPos();
      if (t == 16) {
         for (int k = 0; k < 4; k++) {
            Vfx.telegraphLine(level, c, Vec3d.fromPolar(0.0F, this.eclipseOffset + k * 90), 20.0, 1.6, 24, ECLIPSE);
         }
      }

      if (t >= 40 && t <= 100) {
         float angle = this.eclipseOffset + this.eclipseSign * 90.0F * (t - 40) / 60.0F;
         Vec3d from = c.add(0.0, 1.0, 0.0);
         Vec3d[] ends = new Vec3d[4];

         for (int k = 0; k < 4; k++) {
            ends[k] = this.clipEnd(level, from, from.add(Vec3d.fromPolar(0.0F, angle + k * 90).multiply(20.0)));
            Vfx.beam(level, from, ends[k], ECLIPSE, 1.0);
         }

         if ((t - 40) % 5 == 0) {
            for (LivingEntity e : this.column(level, c, 20.5, 3.5)) {
               for (Vec3d end : ends) {
                  if (touches(e, from, end, 1.3, 2.5)) {
                     if (this.hurt(level, e, 12.0F, true, true)) {
                        e.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 40, 0), this);
                     }
                     break;
                  }
               }
            }
         }
      }

      if (t == 100) {
         Vfx.chargeUp(level, this.sunDisc(), ECLIPSE, 36);
         Vfx.telegraphCircle(level, c, 18.0, 36, ECLIPSE);
         Vfx.ward(level, c, 3.5, SOUL, 36);
      }
   }

   private int maxPits() {
      return this.phase() >= 3 ? 3 : 2;
   }

   private void summon(ServerWorld level, String kind, int n, double radius) {
      EntityType<RealmMob> type = RealmMobs.type(kind);
      if (n > 0 && type != null) {
         BossFx.summonMinions(level, this, type, n, radius, null, mob -> Vfx.groundCrack(level, mob.getEntityPos(), 1.6, P));
         if (kind.equals("scarab")) {
            this.scarabCount += n;
         } else {
            this.guardianCount += n;
         }
      }
   }

   private void spawnAt(ServerWorld level, String kind, Vec3d at) {
      EntityType<RealmMob> type = RealmMobs.type(kind);
      if (type != null) {
         RealmMob mob = (RealmMob)type.create(level, SpawnReason.MOB_SUMMONED);
         if (mob != null) {
            mob.refreshPositionAndAngles(at.x, at.y, at.z, this.random.nextFloat() * 360.0F, 0.0F);
            if (this.getTarget() != null) {
               mob.setTarget(this.getTarget());
            }

            level.spawnEntity(mob);
            Vfx.pillar(level, at, P, 2.5, 0.7);
            Vfx.smoke(level, at, P.smoke(), 6, 0.6, 1.0);
         }
      }
   }

   @Override
   protected void mobTick(ServerWorld world) {
      super.mobTick(world);
      if (!this.inIntro() && this.isAlive()) {
         this.tickSekhmar(world);
      }
   }

   private void tickSekhmar(ServerWorld level) {
      BossFx.current(this);
      LivingEntity target = this.getTarget();
      boolean fighting = target != null;
      if (fighting && this.home() == null && this.fightOrigin == null) {
         this.fightOrigin = this.getEntityPos();
      }

      BossAction move = this.currentMove();
      boolean transition = move != null && move.anim().startsWith("phase_");
      if (this.hazardLevel < 0 || this.hazardLevel > this.phase() || this.hazardLevel < this.phase() && !transition) {
         this.hazardLevel = this.phase();
      }

      if (this.shaftPause > 0) {
         this.shaftPause--;
      }

      if (this.extinguished > 0) {
         this.extinguished--;
      }

      this.tickPits(level);
      this.tickMeteors(level);
      this.tickScorches(level);
      this.tickAnkh(level);
      if (this.age % 20 == 0) {
         this.scanMinions(level);
         this.oathbound(level);
      }

      if (fighting) {
         this.tickVents(level);
         this.tickShafts(level);
         this.tickVeil(level);
      }

      if (this.hazardLevel >= 1 && this.age % 20 == 0) {
         Vfx.embers(level, this.eyes(), SOUL.glow(), 2, 0.4);
      }

      if (this.hazardLevel >= 2 && this.age % 12 == 0) {
         Vfx.embers(level, this.sunDisc(), SUN.glow(), 3, 0.9);
      }

      if (this.hazardLevel >= 3 && this.age % 10 == 0) {
         Vfx.embers(level, this.chest(), SUN.core(), 4, 1.6);
      }

      if (this.age % 5 == 0) {
         this.losBlockedTicks = target != null && !this.sees(level, this.sunDisc(), target) ? this.losBlockedTicks + 5 : 0;
      }
   }

   private void tickPits(ServerWorld level) {
      Iterator<SekhmarBoss.Pit> it = this.pits.iterator();

      while (it.hasNext()) {
         SekhmarBoss.Pit p = it.next();
         if (++p.age >= 160) {
            Vfx.smoke(level, p.c, P.smoke(), 8, 2.2, 1.4);
            it.remove();
         } else {
            boolean pulse = p.age % 20 == 0;

            for (LivingEntity e : this.column(level, p.c, 3.5, 3.0)) {
               double dx = p.c.x - e.getX();
               double dz = p.c.z - e.getZ();
               double d = Math.sqrt(dx * dx + dz * dz);
               if (p.age % 4 == 0 && d > 0.3) {
                  e.addVelocity(dx / d * 0.24, 0.0, dz / d * 0.24);
                  e.knockedBack = true;
               }

               if (p.age % 5 == 0) {
                  e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 12, 2), this);
               }

               if (pulse) {
                  if (d <= 1.2 + e.getWidth() * 0.5) {
                     this.hurt(level, e, 14.0F, true, false);
                     e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20, 4), this);
                  } else {
                     this.hurt(level, e, 3.0F, true, false);
                  }
               }
            }

            if (pulse) {
               Vfx.ring(level, p.c, P.glow(), 3.5, 12);
               Vfx.smoke(level, p.c, P.smoke(), 4, 2.0, 1.2);
            }
         }
      }
   }

   private void tickMeteors(ServerWorld level) {
      Iterator<SekhmarBoss.Meteor> it = this.meteors.iterator();
      List<SekhmarBoss.Meteor> landed = new ArrayList<>();

      while (it.hasNext()) {
         SekhmarBoss.Meteor m = it.next();
         if (this.age >= m.land()) {
            it.remove();
            landed.add(m);
         } else {
            double s = 1.0 - (double)(m.land() - this.age) / m.life();
            Vec3d vel = m.to().subtract(m.from()).multiply(1.0 / m.life());
            Vfx.meteorTrail(level, m.from().lerp(m.to(), MathHelper.clamp(s, 0.0, 1.0)), vel, SUN);
         }
      }

      for (SekhmarBoss.Meteor m : landed) {
         this.landMeteor(level, m);
      }
   }

   private void landMeteor(ServerWorld level, SekhmarBoss.Meteor m) {
      Vec3d at = m.to();
      Vfx.heavyImpact(level, at, SUN, 1.3);
      Vfx.groundCrack(level, at, 2.0, SUN);

      for (LivingEntity e : this.column(level, at, 2.6, 4.0)) {
         if (this.hurt(level, e, 34.0F, false, true)) {
            e.setOnFireFor(5.0F);
            e.addVelocity(0.0, 0.4, 0.0);
            e.knockedBack = true;
         }
      }

      this.scorches.add(new SekhmarBoss.Scorch(at, this.age));
   }

   private void tickScorches(ServerWorld level) {
      Iterator<SekhmarBoss.Scorch> it = this.scorches.iterator();

      while (it.hasNext()) {
         SekhmarBoss.Scorch s = it.next();
         int age = this.age - s.born();
         if (age >= 80) {
            it.remove();
         } else {
            if (age % 10 == 5) {
               Vfx.embers(level, s.at().add(0.0, 0.2, 0.0), SUN.glow(), 3, 1.2);
            }

            if (age % 20 == 10) {
               for (LivingEntity e : this.column(level, s.at(), 2.0, 2.5)) {
                  this.hurt(level, e, 4.0F, true, false);
               }
            }
         }
      }
   }

   private void tickAnkh(ServerWorld level) {
      if (this.ankhTicks > 0) {
         if (this.shield() <= 0.0F) {
            this.ankhTicks = 0;
         } else {
            this.ankhTicks--;
            if (this.ankhTicks > 0 && this.ankhTicks % 40 == 0) {
               Vfx.nova(level, this.getEntityPos(), SUN, 5.0);
               BossFx.slam(level, this, this.getEntityPos(), 5.0, 8.0F, 0.8);
            }

            if (this.ankhTicks == 0) {
               this.heal(this.getMaxHealth() * 0.04F);
               Vfx.auraRing(level, this.getEntityPos(), SUN, 2.5);
            }
         }
      }
   }

   private void scanMinions(ServerWorld level) {
      EntityType<RealmMob> scarab = RealmMobs.type("scarab");
      EntityType<RealmMob> guardian = RealmMobs.type("tomb_guardian");
      int s = 0;
      int g = 0;
      this.oath.clear();

      for (RealmMob mob : level.getEntitiesByClass(RealmMob.class, this.getBoundingBox().expand(32.0), LivingEntity::isAlive)) {
         if (mob.getType() == scarab) {
            s++;
         } else if (mob.getType() == guardian) {
            g++;
            if (mob.squaredDistanceTo(this) <= 144.0) {
               this.oath.add(mob);
            }
         }
      }

      this.scarabCount = s;
      this.guardianCount = g;
   }

   private void oathbound(ServerWorld level) {
      this.oath.removeIf(gx -> !gx.isAlive());
      if (!this.oath.isEmpty() && this.extinguished <= 0) {
         this.buff(25, 0.8F, 1.0F);

         for (RealmMob g : this.oath) {
            Vfx.beam(level, g.getEyePos(), this.chest(), P, 0.25, 20);
         }
      }
   }

   private void tickVents(ServerWorld level) {
      if (this.hazardLevel >= 1 && --this.ventTimer <= 0) {
         int interval = this.hazardLevel >= 3 ? 100 : (this.hazardLevel == 2 ? 120 : 160);
         this.ventTimer = this.enraged() ? interval / 2 : interval;
         Vec3d[] vs = this.vents(level);
         int first = this.random.nextInt(4);
         int n = this.hazardLevel >= 3 ? 2 : 1;

         for (int k = 0; k < n; k++) {
            Vec3d v = vs[(first + k * 2) % 4];
            if (v != null) {
               Vfx.telegraphCircle(level, v, 2.5, 20, SOUL);
               this.schedule(20, () -> this.ventBurst(level, v));
            }
         }
      }
   }

   private void ventBurst(ServerWorld level, Vec3d v) {
      Vfx.pillar(level, v, SOUL, 6.0, 1.6);
      BossSounds.play(level, v, "boss.sekhmar.soul_curse.hit", 2.0F, 0.8F);

      for (LivingEntity e : this.column(level, v, 2.5, 4.0)) {
         if (this.hurt(level, e, 16.0F, true, true)) {
            e.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 60, 0), this);
         }
      }
   }

   private Vec3d[] vents(World level) {
      Vec3d a = this.anchor();
      Vec3d[] out = new Vec3d[4];

      for (int k = 0; k < 4; k++) {
         double x = a.x + VENTS[k][0];
         double z = a.z + VENTS[k][1];
         BlockPos floor = BlockPos.ofFloored(x, a.y - 0.5, z);
         out[k] = !level.getBlockState(floor).getCollisionShape(level, floor).isEmpty()
            ? new Vec3d(x, a.y, z)
            : floorBelow(level, new Vec3d(x, a.y + 2.0, z), 6);
      }

      return out;
   }

   private void tickShafts(ServerWorld level) {
      int want = this.hazardLevel < 2 ? 0 : (this.hazardLevel >= 3 ? 3 : 2) + (this.enraged() ? 1 : 0);
      if (want != this.shafts.size()) {
         this.shafts.clear();
         double off = this.random.nextDouble() * Math.PI * 2.0;

         for (int k = 0; k < want; k++) {
            this.shafts.add(new SekhmarBoss.Shaft(off + k * Math.PI * 2.0 / want));
         }

         this.shaftGrace = 30;

         for (SekhmarBoss.Shaft s : this.shafts) {
            Vec3d p = this.shaftPoint(level, s);
            if (p != null) {
               Vfx.telegraphCircle(level, p, 1.6, 30, SUN);
            }
         }
      }

      if (!this.shafts.isEmpty()) {
         double speed = this.hazardLevel >= 3 ? 0.02 : 0.012;

         for (SekhmarBoss.Shaft s : this.shafts) {
            s.angle += speed;
         }

         if (this.shaftGrace > 0) {
            this.shaftGrace--;
         } else if (this.shaftPause <= 0 && this.age % 10 == 0) {
            for (SekhmarBoss.Shaft s : this.shafts) {
               Vec3d p = this.shaftPoint(level, s);
               if (p != null) {
                  double h = ceilingAbove(level, p, 14);
                  Vfx.beam(level, p.add(0.0, h, 0.0), p, SUN, 1.4, 12);
                  Vfx.ring(level, p, SUN.glow(), 1.6, 10);

                  for (LivingEntity e : this.column(level, p, 1.6, h)) {
                     if (this.hurt(level, e, 6.0F, false, false)) {
                        e.setOnFireFor(3.0F);
                     }
                  }
               }
            }
         }
      }
   }

   private @Nullable Vec3d shaftPoint(World level, SekhmarBoss.Shaft s) {
      Vec3d a = this.anchor();
      double r = this.hazardLevel >= 3 ? 5.0 + 4.0 * Math.abs(Math.sin(s.angle * 1.5)) : 7.0;
      Vec3d g = floorBelow(level, new Vec3d(a.x + Math.cos(s.angle) * r, a.y + 1.5, a.z + Math.sin(s.angle) * r), 5);
      return g != null && Math.abs(g.y - a.y) < 3.0 ? g : null;
   }

   private void tickVeil(ServerWorld level) {
      if (this.hazardLevel >= 3) {
         this.veilTimer++;
         if (this.veilTimer % 200 == 1) {
            Vfx.vortex(level, this.anchor(), 14.0, 8.0, P, 200);
         }

         if (this.veilTimer % 20 == 0) {
            for (ServerPlayerEntity p : this.engaged()) {
               if (!(p.squaredDistanceTo(this) <= 144.0)) {
                  p.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 50, 0), this);
                  this.hurt(level, p, 2.0F, true, false);
                  Vec3d to = this.flat(this.getEntityPos().subtract(p.getEntityPos()));
                  p.addVelocity(to.x * 0.3, 0.05, to.z * 0.3);
                  p.knockedBack = true;
                  Vfx.streaks(level, p.getEntityPos().add(0.0, 1.0, 0.0), to.multiply(3.0), P, 5);
               }
            }
         }

         if (this.veilTimer % 400 == 200 && this.scarabCount < 10) {
            Vec3d a = this.anchor();

            for (int k = 0; k < 2; k++) {
               this.spawnAt(level, "scarab", this.near(level, a, 9.0, 11.0));
            }

            this.scarabCount += 2;
         }
      }
   }

   @Override
   protected void onPhaseChange(ServerWorld level, int phase) {
      BossFx.current(this);
      this.burrowed = false;
      if (phase == 1) {
         this.schedule(10, () -> Vfx.shards(level, this.front(1.1, 4.2), 15251008, 14, 0.35));
         Vfx.chargeUp(level, this.eyes(), SOUL, 28);
      } else if (phase == 2) {
         Vfx.chargeUp(level, this.sunDisc(), SUN, 28);
         Vfx.runeCircle(level, this.getEntityPos(), 8.0, SUN, 60, 0.05F, Vfx.Rune.SOLAR);
         this.setCooldown("wrath_of_ra", 200);
      } else {
         for (int k = 0; k < 7; k++) {
            this.schedule(k * 4, () -> Vfx.embers(level, this.getEntityPos().add(0.0, 1.0 + this.random.nextDouble() * 2.8, 0.0), SUN.glow(), 6, 1.4));
         }

         this.setCooldown("sunfall", 160);
      }
   }

   @Override
   protected void phaseBurst(ServerWorld level, int phase) {
      BossFx.current(this);
      Vec3d at = this.getEntityPos();
      this.hazardLevel = phase;
      if (phase == 1) {
         Vfx.nova(level, at, SOUL, 9.0);
         Vfx.heavyImpact(level, at, SOUL, 1.2);
         List<LivingEntity> hit = BossFx.victims(level, this, at, 8.0);
         BossFx.slam(level, this, at, 8.0, 10.0F, 1.6);

         for (LivingEntity e : hit) {
            e.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 60, 0), this);
         }

         this.summon(level, "scarab", Math.min(4, 10 - this.count(level, "scarab")), 6.0);
         this.ventTimer = 60;
      } else if (phase == 2) {
         Vfx.nova(level, at, SUN, 11.0);
         Vfx.pillar(level, at, SUN, ceilingAbove(level, at, 14), 2.4);
         Vfx.heavyImpact(level, at, SUN, 1.6);
         List<LivingEntity> hit = BossFx.victims(level, this, at, 10.0);
         BossFx.slam(level, this, at, 10.0, 14.0F, 1.8);

         for (LivingEntity e : hit) {
            e.setOnFireFor(4.0F);
         }
      } else {
         Vfx.nova(level, at, SUN, 13.0);
         Vfx.impactFrame(level, at, 36.0, 16771232, 0.9F);
         Vfx.heavyImpact(level, at, SUN, 2.2);
         BossFx.slam(level, this, at, 12.0, 18.0F, 2.0);
         this.shield(this.getMaxHealth() * 0.1F, 300);
         this.summon(level, "tomb_guardian", Math.min(2, 3 - this.count(level, "tomb_guardian")), 7.0);
         this.veilTimer = 0;
      }
   }

   @Override
   protected void onStagger(ServerWorld level) {
      for (SekhmarBoss.Pit p : this.pits) {
         Vfx.smoke(level, p.c, P.smoke(), 10, 2.5, 1.6);
      }

      this.pits.clear();
      this.shaftPause = 60;
      Vfx.flash(level, this.getEntityPos(), 16.0, 3811856, 0.2F);
   }

   @Override
   protected void onEnrage(ServerWorld level) {
      Vfx.nova(level, this.getEntityPos(), SUN, 12.0);
   }

   @Override
   protected void desperation(ServerWorld level) {
      BossAction move = this.currentMove();
      if (move != null && move.anim().startsWith("phase_")) {
         this.schedule(Math.max(2, move.length() - this.actionTick() + 2), () -> {
            if (this.isAlive()) {
               this.forceAction(level, "eclipse");
            }
         });
      } else {
         this.forceAction(level, "eclipse");
      }
   }

   @Override
   protected void onIntroTick(ServerWorld level, int left) {
      int e = this.introTicks() - left;
      Vec3d at = this.getEntityPos();
      if (e == 1) {
         Vfx.vortex(level, at, 4.0, 5.0, P, 70);
         Vfx.groundCrack(level, at, 5.0, P);
         Vfx.runeCircle(level, at, 6.0, P, 140, 0.02F, Vfx.Rune.GLYPH);
      }

      if (e < 70 && e % 4 == 0) {
         Vfx.smoke(level, at.add(0.0, 0.3 + e * 0.03, 0.0), P.smoke(), 5, 1.8, 1.6);
      }

      if (e == 30) {
         for (Vec3d v : this.vents(level)) {
            if (v != null) {
               Vfx.pillar(level, v, SOUL, 4.0, 0.8);
            }
         }
      }

      if (e == 64) {
         Vfx.shockwave(level, at, P, 5.0);
         BossFx.knockback(level, this, at, 6.0, 1.0);
      }

      if (e == 84) {
         Vfx.flare(level, this.eyes(), SOUL.glow(), 1.4, 20);
      }

      if (e == 112) {
         Vfx.pillar(level, at, SUN, ceilingAbove(level, at, 14), 2.0);
         Vfx.radialFlash(level, at, 40.0, 16774336, 0.5F);
         Vfx.nova(level, at, P, 8.0);
      }
   }

   @Override
   protected void onDeathTick(ServerWorld level, int tick) {
      Vec3d at = this.getEntityPos();
      if (tick <= 40 && tick % 8 == 0) {
         Vfx.wisps(level, this.front(0.9, 4.0), SOUL, 3);
      }

      if (tick == 36) {
         Vfx.heavyImpact(level, groundAt(level, this.front(1.5, 0.0)), P, 0.8);
      }

      if (tick == 44) {
         Vfx.shards(level, this.crookTip(), 15251008, 10, 0.3);
      }

      if (tick == 60) {
         Vec3d from = this.front(0.5, 4.2);
         Vfx.beam(level, from, from.add(0.0, ceilingAbove(level, from, 14), 0.0), SUN, 0.6, 24);
      }

      if (tick == 84) {
         Vfx.nova(level, at, ECLIPSE, 6.0);
      }

      if (tick >= 100 && tick <= 150 && tick % 4 == 0) {
         Vec3d p = at.add(0.0, 4.2 * (1.0 - (tick - 100) / 50.0), 0.0);
         Vfx.smoke(level, p, P.smoke(), 5, 1.2, 1.6);
         Vfx.debris(level, p, Vfx.Debris.ASH, 14203000, 8, 0.2);
      }

      if (tick == 150) {
         Vfx.explosion(level, at.add(0.0, 1.0, 0.0), P, 2.5);
         Vfx.wisps(level, at.add(0.0, 1.5, 0.0), SOUL, 12, 3.0, 60);

         for (Vec3d v : this.vents(level)) {
            if (v != null) {
               Vfx.pillar(level, v, SOUL, 5.0, 0.9);
            }
         }
      }
   }

   @Override
   public boolean damage(ServerWorld world, DamageSource source, float amount) {
      return this.burrowed && !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY) ? false : super.damage(world, source, amount);
   }

   @Override
   public void onDeath(DamageSource damageSource) {
      this.burrowed = false;
      this.pits.clear();
      this.shafts.clear();
      this.meteors.clear();
      this.scorches.clear();
      this.oath.clear();
      this.ankhTicks = 0;
      super.onDeath(damageSource);
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
   protected void initDataTracker(net.minecraft.entity.data.DataTracker.Builder builder) {
      super.initDataTracker(builder);
      builder.add(ECLIPSED, false);
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
      super.registerControllers(controllers);
      RawAnimation[] looks = new RawAnimation[]{
         this.loopAnim("aspect_0"), this.loopAnim("aspect_1"), this.loopAnim("aspect_2"), this.loopAnim("aspect_3"), this.loopAnim("aspect_eclipse")
      };
      controllers.add(new AnimationController("aspect", 28, test -> {
         test.controller().setTransitionTicks(this.age < 20 ? 0 : 28);
         return test.setAndContinue(looks[this.dataTracker.get(ECLIPSED) ? 4 : MathHelper.clamp(this.phase(), 0, 3)]);
      }));
   }

   private boolean foe(LivingEntity e) {
      return e != this && e.isAlive() && !(e instanceof MysticalBoss) && !(e instanceof PlayerEntity p && (p.isCreative() || p.isSpectator()));
   }

   private boolean alive(@Nullable LivingEntity e, double range) {
      return e != null && e.isAlive() && e.squaredDistanceTo(this) <= range * range;
   }

   private boolean hurt(ServerWorld level, LivingEntity e, float base, boolean magic, boolean pierce) {
      if (pierce) {
         e.timeUntilRegen = 0;
      }

      DamageSource src = magic ? this.getDamageSources().indirectMagic(this, this) : this.getDamageSources().mobAttack(this);
      return e.damage(level, src, this.dmg(base));
   }

   private static void shove(LivingEntity e, Vec3d dir, double push, double up) {
      e.addVelocity(dir.x * push, up, dir.z * push);
      e.knockedBack = true;
   }

   private void guardBreak(ServerWorld level, LivingEntity e) {
      ItemStack s = e.getBlockingItem();
      if (s != null) {
         if (e instanceof PlayerEntity p) {
            p.getItemCooldownManager().set(s, 100);
         }

         e.clearActiveItem();
         Vec3d at = e.getEntityPos().add(0.0, e.getHeight() * 0.6, 0.0);
         Vfx.shards(level, at, 16777215, 10, 0.35);
         Vfx.glint(level, at, 15251008, 1.2);
      }
   }

   private static boolean usingItem(LivingEntity e) {
      return e.isUsingItem() && !e.getActiveItem().contains(DataComponentTypes.BLOCKS_ATTACKS);
   }

   private @Nullable LivingEntity punishTarget(@Nullable LivingEntity target, double range) {
      if (target != null && usingItem(target)) {
         return target;
      }

      for (ServerPlayerEntity p : this.engaged()) {
         if (usingItem(p) && p.squaredDistanceTo(this) <= range * range) {
            return p;
         }
      }

      return target;
   }

   private boolean anyoneBehind(double range) {
      return !this.inCone(this.getEntityWorld(), this.getEntityPos(), this.getYaw() + 180.0F, 80.0F, range, 4.0).isEmpty();
   }

   private int count(ServerWorld level, String kind) {
      EntityType<RealmMob> type = RealmMobs.type(kind);
      return type == null ? 0 : level.getEntitiesByType(type, this.getBoundingBox().expand(32.0), LivingEntity::isAlive).size();
   }

   private Vec3d flat(Vec3d v) {
      Vec3d h = new Vec3d(v.x, 0.0, v.z);
      return h.lengthSquared() < 1.0E-6 ? this.look() : h.normalize();
   }

   private double hDist(Vec3d p) {
      double dx = p.x - this.getX();
      double dz = p.z - this.getZ();
      return Math.sqrt(dx * dx + dz * dz);
   }

   private float yawTo(Vec3d p) {
      return (float)(MathHelper.atan2(p.z - this.getZ(), p.x - this.getX()) * 180.0F / (float)Math.PI) - 90.0F;
   }

   private static float pitchTo(Vec3d from, Vec3d to) {
      Vec3d d = to.subtract(from);
      return (float)(-MathHelper.atan2(d.y, d.horizontalLength()) * 180.0F / (float)Math.PI);
   }

   private void face(float yaw) {
      this.setYaw(yaw);
      this.setBodyYaw(yaw);
      this.setHeadYaw(yaw);
   }

   private static @Nullable Vec3d floorBelow(World level, Vec3d p, int drop) {
      Mutable m = BlockPos.ofFloored(p.x, p.y + 0.5, p.z).mutableCopy();

      for (int i = 0; i <= drop; i++) {
         BlockPos below = m.down();
         if (level.getBlockState(m).getCollisionShape(level, m).isEmpty() && !level.getBlockState(below).getCollisionShape(level, below).isEmpty()) {
            return new Vec3d(p.x, m.getY(), p.z);
         }

         m.move(0, -1, 0);
      }

      return null;
   }

   private static Vec3d groundAt(World level, Vec3d p) {
      Vec3d g = floorBelow(level, p, 12);
      return g != null ? g : p;
   }

   private Vec3d groundUnder(World level, Vec3d p) {
      Vec3d g = floorBelow(level, new Vec3d(p.x, Math.min(p.y, this.getY() + 4.0), p.z), 14);
      if (g == null) {
         g = floorBelow(level, p, 14);
      }

      return g != null ? g : p;
   }

   private Vec3d near(World level, Vec3d center, double minR, double maxR) {
      for (int i = 0; i < 8; i++) {
         double a = this.random.nextDouble() * Math.PI * 2.0;
         double r = minR + this.random.nextDouble() * (maxR - minR);
         Vec3d g = floorBelow(level, new Vec3d(center.x + Math.cos(a) * r, center.y + 1.5, center.z + Math.sin(a) * r), 5);
         if (g != null && Math.abs(g.y - center.y) < 3.0) {
            return g;
         }
      }

      return groundAt(level, center);
   }

   private static boolean open(World level, Vec3d at) {
      BlockPos p = BlockPos.ofFloored(at.x, at.y + 0.1, at.z);
      return level.getBlockState(p).getCollisionShape(level, p).isEmpty() && level.getBlockState(p.up()).getCollisionShape(level, p.up()).isEmpty();
   }

   private static double ceilingAbove(World level, Vec3d p, int max) {
      Mutable m = BlockPos.ofFloored(p.x, p.y + 0.1, p.z).mutableCopy();

      for (int i = 0; i < max; i++) {
         if (!level.getBlockState(m).getCollisionShape(level, m).isEmpty()) {
            return Math.max(1.0, m.getY() - p.y);
         }

         m.move(0, 1, 0);
      }

      return max;
   }

   private boolean sees(World level, Vec3d from, LivingEntity e) {
      return level.raycast(new RaycastContext(from, e.getEyePos(), ShapeType.COLLIDER, FluidHandling.NONE, this)).getType() == Type.MISS;
   }

   private Vec3d clipEnd(World level, Vec3d from, Vec3d to) {
      BlockHitResult r = level.raycast(new RaycastContext(from, to, ShapeType.COLLIDER, FluidHandling.NONE, this));
      return r.getType() == Type.MISS ? to : r.getPos();
   }

   private List<LivingEntity> column(World level, Vec3d base, double r, double h) {
      Box box = new Box(base.x - r, base.y - 0.5, base.z - r, base.x + r, base.y + h, base.z + r);
      return level.getEntitiesByClass(LivingEntity.class, box, e -> {
         if (!this.foe(e)) {
            return false;
         }

         double dx = e.getX() - base.x;
         double dz = e.getZ() - base.z;
         double rr = r + e.getWidth() * 0.5;
         return dx * dx + dz * dz <= rr * rr;
      });
   }

   private List<LivingEntity> inLine(World level, Vec3d from, Vec3d dir, double len, double halfWidth, double h) {
      Vec3d d = this.flat(dir);
      Vec3d to = from.add(d.multiply(len));
      Box box = new Box(Math.min(from.x, to.x), from.y - 1.0, Math.min(from.z, to.z), Math.max(from.x, to.x), from.y + h, Math.max(from.z, to.z))
         .expand(halfWidth + 1.0, 0.0, halfWidth + 1.0);
      return level.getEntitiesByClass(LivingEntity.class, box, e -> {
         if (!this.foe(e)) {
            return false;
         }

         double rx = e.getX() - from.x;
         double rz = e.getZ() - from.z;
         double along = rx * d.x + rz * d.z;
         double perp = Math.abs(rx * d.z - rz * d.x);
         return along >= -0.5 && along <= len + e.getWidth() * 0.5 && perp <= halfWidth + e.getWidth() * 0.5;
      });
   }

   private List<LivingEntity> inCone(World level, Vec3d origin, float yaw, float halfAngle, double range, double height) {
      Vec3d f = Vec3d.fromPolar(0.0F, yaw);
      Box box = new Box(origin.x - range - 1.0, origin.y - 1.5, origin.z - range - 1.0, origin.x + range + 1.0, origin.y + height, origin.z + range + 1.0);
      return level.getEntitiesByClass(LivingEntity.class, box, e -> {
         if (!this.foe(e)) {
            return false;
         }

         double dx = e.getX() - origin.x;
         double dz = e.getZ() - origin.z;
         double d = Math.sqrt(dx * dx + dz * dz);
         if (d > range + e.getWidth() * 0.5) {
            return false;
         }

         if (d < 1.5) {
            return true;
         }

         double cos = (dx * f.x + dz * f.z) / d;
         return Math.toDegrees(Math.acos(MathHelper.clamp(cos, -1.0, 1.0))) <= halfAngle;
      });
   }

   private static boolean touches(LivingEntity e, Vec3d from, Vec3d to, double radius, double vertical) {
      Vec3d d = to.subtract(from);
      double len2 = d.lengthSquared();
      Vec3d c = e.getEntityPos().add(0.0, e.getHeight() * 0.5, 0.0);
      double s = len2 < 1.0E-6 ? 0.0 : MathHelper.clamp(c.subtract(from).dotProduct(d) / len2, 0.0, 1.0);
      Vec3d q = from.add(d.multiply(s));
      double dx = c.x - q.x;
      double dz = c.z - q.z;
      double r = radius + e.getWidth() * 0.5;
      return dx * dx + dz * dz <= r * r && Math.abs(c.y - q.y) <= vertical;
   }

   private record Meteor(Vec3d from, Vec3d to, int land, int life) {
   }

   private static final class Pit {
      final Vec3d c;
      int age;

      Pit(Vec3d c) {
         this.c = c;
      }
   }

   private record Scorch(Vec3d at, int born) {
   }

   private static final class Shaft {
      double angle;

      Shaft(double angle) {
         this.angle = angle;
      }
   }
}
