package com.vexira.mysticalterrain.boss.entity;

import com.vexira.mysticalterrain.boss.BossAction;
import com.vexira.mysticalterrain.boss.BossBolt;
import com.vexira.mysticalterrain.boss.BossEntities;
import com.vexira.mysticalterrain.boss.BossFx;
import com.vexira.mysticalterrain.boss.BossSounds;
import com.vexira.mysticalterrain.boss.MysticalBoss;
import com.vexira.mysticalterrain.boss.RealmMob;
import com.vexira.mysticalterrain.boss.RealmMobs;
import com.vexira.mysticalterrain.boss.vfx.Vfx;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
import net.minecraft.item.consume.UseAction;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;
import software.bernie.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

public class HollowWardenBoss extends MysticalBoss {
   private static final BossSounds SOUNDS = BossSounds.of("hollow_warden");
   private static final Vfx.Palette PAL = Vfx.Palette.SHADOW;
   private static final Vfx.Palette SOUL = new Vfx.Palette(15138815, 6086911, 1318956).withDebris(Vfx.Debris.ASH);
   private static final Vfx.Palette VIOLET = new Vfx.Palette(16181503, 10514687, 1708080).withDebris(Vfx.Debris.ASH);
   private static final int IRON = 5920868;
   private static final int MAX_ADDS = 6;
   private static final int PHASE_LEAD = 27;
   private static final String[] ENRAGE_HASTE = new String[]{"soul_volley", "grasping_dark", "soul_pyre", "penance"};
   private static final TrackedData<Integer> FORM = DataTracker.registerData(HollowWardenBoss.class, TrackedDataHandlerRegistry.INTEGER);
   private static final RawAnimation[] FORMS = new RawAnimation[4];
   public static final float WIDTH = 1.4F;
   public static final float HEIGHT = 4.2F;
   public static final boolean FIRE_IMMUNE = false;
   private final List<HollowWardenBoss.Spot> spots = new ArrayList<>();
   private final List<Vec3d> points = new ArrayList<>();
   private final List<HollowWardenBoss.Cell> cells = new ArrayList<>();
   private final Map<UUID, LivingEntity> tethers = new HashMap<>();
   private final List<HollowWardenBoss.Line> plan = new ArrayList<>();
   private final List<HollowWardenBoss.Line> lines = new ArrayList<>();
   private final List<HollowWardenBoss.Strike> restless = new ArrayList<>();
   private final List<HollowWardenBoss.Zone> zones = new ArrayList<>();
   private final Map<UUID, Integer> thornsAt = new HashMap<>();
   private final List<Vec3d> verdictCells = new ArrayList<>();
   private final List<BossBolt> piercing = new ArrayList<>();
   private @Nullable Vec3d spot;
   private @Nullable LivingEntity condemned;
   private @Nullable LivingEntity hooked;
   private int hookStart;
   private Vec3d aimFrom = Vec3d.ZERO;
   private Vec3d aimDir = new Vec3d(0.0, 0.0, 1.0);
   private double passLen;
   private Vec3d passStart = Vec3d.ZERO;
   private Vec3d passEnd = Vec3d.ZERO;
   private Vec3d nightCenter = Vec3d.ZERO;
   private Vec3d verdictCenter = Vec3d.ZERO;
   private double verdictRot;
   private double burstAngle;
   private int lastUlt = -100000;
   private int restlessTimer;
   private int vigilTicks;
   private int immuneTicks;
   private boolean verdictPending;
   private int minionCount;
   private int minionsAt = -1000;
   private int lonely;
   private boolean wasDodging;
   private int dodgeWake;
   private Vec3d dodgeFrom = Vec3d.ZERO;

   public static Builder attributes() {
      return MysticalBoss.bossAttributes(780.0, 14.0, 0.3, 6.0);
   }

   public HollowWardenBoss(EntityType<? extends HostileEntity> type, World level) {
      super(type, level, "hollow_warden", Color.PURPLE);
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
      return 64;
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
      return 1.5;
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
      return 0.6;
   }

   @Override
   protected double preferredDistance() {
      return 4.5;
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
   protected float[] phaseThresholds() {
      return new float[]{0.7F, 0.45F, 0.2F};
   }

   @Override
   protected boolean immune() {
      return this.immuneTicks > 0;
   }

   @Override
   protected String moveAnim() {
      return this.getVelocity().horizontalLengthSquared() > 0.09 ? "run" : "walk";
   }

   @Override
   protected boolean minionsAlive(ServerWorld level) {
      return super.minionsAlive(level) || !level.getEntitiesByClass(WardenShade.class, this.getBoundingBox().expand(24.0), LivingEntity::isAlive).isEmpty();
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
      builder.add(FORM, 0);
   }

   public int form() {
      return (Integer)this.dataTracker.get(FORM);
   }

   private void setForm(int f) {
      int n = MathHelper.clamp(f, 0, 3);
      if (n != this.form()) {
         this.dataTracker.set(FORM, n);
      }
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
      super.registerControllers(controllers);
      controllers.add(new AnimationController("form", 8, test -> test.setAndContinue(FORMS[MathHelper.clamp(this.form(), 0, 3)])));
   }

   @Override
   protected void defineActions(List<BossAction> out) {
      out.add(
         BossAction.of("flurry", 1.6, 0.5, 0.8, 1.1)
            .range(0.0, 5.5)
            .cooldown(50)
            .weight(14)
            .tags("melee")
            .advance(0.15, 1.0, 0.1)
            .telegraph(0.1, 1.1)
            .combo("key_cleave", 0.3F)
            .tick((b, level, target, t) -> {
               if (t <= 10) {
                  this.descend(target, 10 - t);
               }

               if (t == 2) {
                  this.cone(level, 0.0F, 70.0F, 5.0, 8);
               } else if (t == 11 || t == 17) {
                  this.cone(level, 0.0F, 70.0F, 5.0, 5);
               }
            })
            .hit((b, level, target, i) -> {
               this.rake(level, 5.0, 70.0F, 0.0F, 14.0F, 0.5, new StatusEffectInstance(StatusEffects.WITHER, 40, 0), i > 0);
               Vfx.slashArc(level, this.chest(), this.getYaw(), 10.0F, i == 0 ? 45.0F : (i == 1 ? -45.0F : 90.0F), 4.6, PAL, 150.0F);
               if (i == 2) {
                  Vfx.claw(level, this.chest(), this.getYaw(), 20.0F, 4.2, PAL);
               }
            })
      );
      out.add(
         BossAction.of("key_cleave", 1.5, 0.75)
            .range(0.0, 6.0)
            .cooldown(70)
            .weight(12)
            .tags("melee")
            .advance(0.45, 0.75, 0.16)
            .telegraph(0.15, 0.75)
            .combo("false_lock", 0.15F)
            .combo("gloomwing_rend", 0.3F)
            .combo("lock_breaker", 0.35F)
            .tick((b, level, target, t) -> {
               if (t <= 15) {
                  this.descend(target, 15 - t);
               }

               if (t == 3) {
                  this.cone(level, 0.0F, 100.0F, 5.8, 12);
               } else if (t == 11) {
                  Vfx.glint(level, this.at(-0.85, -2.35, 3.2), SOUL.core(), 1.2);
               }
            })
            .hit((b, level, target, i) -> {
               this.rake(level, 5.8, 100.0F, 0.0F, 22.0F, 1.1, null, false);
               Vfx.slashArc(level, this.chest(), this.getYaw(), 5.0F, 0.0F, 5.8, PAL, 210.0F);
               Vfx.sparks(level, this.at(5.0, -0.7, 2.5), SOUL.glow(), 10, 0.35, 0.18);
               this.schedule(4, () -> {
                  Vec3d g = this.at(1.35, 2.9, 2.3);
                  Vfx.glint(level, g, SOUL.core(), 1.4);
                  Vfx.sparks(level, g, SOUL.core(), 8, 0.25, 0.16);
               });
            })
      );
      out.add(
         BossAction.of("lock_breaker", 1.8, 0.95)
            .range(0.0, 6.0)
            .cooldown(140)
            .weight(9)
            .tags("melee aoe guard_break")
            .superArmor()
            .advance(0.6, 0.9, 0.12)
            .telegraph(0.3, 0.95)
            .utility((b, t, c) -> c.targetBlocking ? 1.6F : 1.0F)
            .tick((b, level, target, t) -> {
               if (t <= 19) {
                  this.descend(target, 19 - t);
               }

               if (t == 6) {
                  this.spot = floor(level, this.at(4.0, -0.3, 0.0));
                  tele(level, this.spot, 4.0, 13, PAL);
                  Vfx.chargeUp(level, this.keyRaised(), SOUL, 13);
               }
            })
            .hit((b, level, target, i) -> this.lockSlam(level, 32.0F, 1.4, false))
      );
      out.add(
         BossAction.of("censer_spin", 2.0, 0.7, 1.0, 1.3)
            .range(0.0, 5.5)
            .cooldown(120)
            .weight(9)
            .tags("melee aoe")
            .telegraph(0.0, 1.3)
            .utility((b, t, c) -> (1.0F + 0.6F * Math.max(0, c.surroundCount - 1)) * (c.dist > 4.0 ? 0.5F : 1.0F))
            .tick((b, level, target, t) -> {
               if (t == 1) {
                  tele(level, floor(level, this.getEntityPos()), 5.0, 13, SOUL);
               } else if (t == 15 || t == 21) {
                  tele(level, floor(level, this.getEntityPos()), 5.0, 5, SOUL);
               }
            })
            .hit((b, level, target, i) -> {
               this.rake(level, 5.0, 180.0F, 0.0F, 16.0F, 0.9, new StatusEffectInstance(StatusEffects.WITHER, 40, 0), i > 0);
               Vfx.slashArc(level, this.at(0.0, 0.0, 2.2), this.getYaw() + 120 * i, 0.0F, 5.0, SOUL, 360.0F);
               if (i == 2) {
                  Vfx.wisps(level, this.getEntityPos(), SOUL, 4);
               }
            })
      );
      out.add(
         BossAction.of("backhand_rake", 1.3, 0.55)
            .range(0.0, 5.0)
            .cooldown(60)
            .weight(10)
            .tags("melee behind")
            .lockFacing()
            .telegraph(0.1, 0.55)
            .utility((b, t, c) -> c.targetBehind ? 1.5F : 0.0F)
            .combo("flurry", 0.25F)
            .tick((b, level, target, t) -> {
               if (t == 2) {
                  this.cone(level, 180.0F, 80.0F, 5.0, 9);
               }
            })
            .hit((b, level, target, i) -> {
               this.rake(level, 5.0, 80.0F, 180.0F, 20.0F, 1.3, null, false);
               Vfx.slashArc(level, this.chest(), this.getYaw() + 180.0F, 10.0F, 30.0F, 4.8, PAL, 170.0F);
            })
      );
      out.add(
         BossAction.of("shackle_lash", 1.6, 0.7)
            .range(4.0, 16.0)
            .cooldown(110)
            .weight(11)
            .tags("ranged gap_closer")
            .telegraph(0.2, 0.7)
            .utility((b, t, c) -> c.targetFlying ? 0.0F : (c.kitingTicks <= 40 && !(c.dist > 9.0) ? 0.8F : 2.0F))
            .combo("flurry", 0.45F)
            .combo("key_cleave", 0.35F)
            .tick((b, level, target, t) -> {
               if (t == 4) {
                  this.aimFrom = this.chest();
                  this.aimDir = target != null ? this.flatDir(this.predict(target, 10.0).subtract(this.getEntityPos())) : this.facing();
                  Vfx.telegraphLine(level, floor(level, this.getEntityPos()), this.aimDir, 16.0, 1.4, 10, SOUL);
               }

               if (t >= 4 && t <= 13 && t % 3 == 1) {
                  Vfx.gather(level, this.at(-0.3, -1.3, 1.65), SOUL, 1.4, 6);
               }
            })
            .hit((b, level, target, i) -> this.shackle(level))
      );
      out.add(
         BossAction.of("soul_volley", 1.8, 0.6, 0.9, 1.2)
            .range(5.0, 32.0)
            .cooldown(70)
            .weight(12)
            .tags("ranged")
            .telegraph(0.3, 1.2)
            .combo("shackle_lash", 0.2F)
            .tick((b, level, target, t) -> {
               if (t == 6 || t == 12 || t == 18) {
                  Vfx.chargeUp(level, this.volleyHand(t / 6 - 1), SOUL, 6);
               }
            })
            .hit((b, level, target, i) -> this.volley(level, target, i))
      );
      out.add(
         BossAction.of("condemn", 1.2, 0.55)
            .range(3.0, 36.0)
            .cooldown(90)
            .weight(6)
            .tags("ranged punish")
            .telegraph(0.05, 0.55)
            .utility((b, t, c) -> !c.targetHealing && !c.targetAiming && !this.anyUsing() ? 0.3F : 1.5F)
            .start((b, level, target, i) -> {
               this.condemned = this.pickCondemned(target);
               if (this.condemned != null) {
                  BossFx.markTarget(level, this, this.condemned, 12);
               }
            })
            .tick((b, level, target, t) -> {
               if (t == 1) {
                  LivingEntity c = this.condemned != null && this.condemned.isAlive() ? this.condemned : target;
                  this.spot = floor(level, c != null ? this.predict(c, 10.0) : this.at(6.0, 0.0, 0.0));
                  tele(level, this.spot, 2.0, 10, SOUL);
                  Vfx.chargeUp(level, this.spot.add(0.0, 2.5, 0.0), SOUL, 10);
               }
            })
            .hit((b, level, target, i) -> this.condemn(level))
      );
      out.add(
         BossAction.of("gallows_hook", 1.7, 0.8)
            .range(3.0, 28.0)
            .cooldown(140)
            .weight(4)
            .tags("ranged anti_air")
            .telegraph(0.2, 0.8)
            .utility((b, t, c) -> !c.targetFlying && !c.targetAirborne && c.unreachableTicks <= 40 ? 0.15F : 2.0F)
            .tick((b, level, target, t) -> {
               if (t == 4) {
                  if (target != null) {
                     BossFx.markTarget(level, this, target, 12);
                  }

                  Vfx.chargeUp(level, this.hookHand(), SOUL, 12);
               }
            })
            .hit((b, level, target, i) -> this.hook(level, target))
      );
      out.add(
         BossAction.of("grasping_dark", 2.2, 1.1)
            .range(3.0, 22.0)
            .cooldown(130)
            .weight(10)
            .tags("ranged aoe")
            .telegraph(0.2, 1.1)
            .combo("headsmans_drop", 0.3F)
            .utility((b, t, c) -> c.targetAirborne ? 0.3F : 1.0F)
            .tick((b, level, target, t) -> {
               if (t == 4) {
                  this.planGrasp(level, target);
               }
            })
            .hit((b, level, target, i) -> this.grasp(level))
      );
      out.add(
         BossAction.of("gloom_step", 1.0, 0.45)
            .range(3.0, 26.0)
            .cooldown(160)
            .weight(8)
            .tags("mobility gap_closer")
            .telegraph(0.05, 0.45)
            .utility((b, t, c) -> c.kitingTicks <= 40 && c.unreachableTicks <= 20 && !(c.recentDamage > 0.05F) ? 0.7F : 1.8F)
            .combo("flurry", 0.45F)
            .combo("key_cleave", 0.3F)
            .tick((b, level, target, t) -> {
               if (t == 1) {
                  this.spot = this.flankSpot(level, target);
                  tele(level, this.spot, 1.4, 8, PAL);
               } else if (t == 5) {
                  this.immuneTicks = 6;
                  Vfx.smoke(level, this.getEntityPos().add(0.0, 0.2, 0.0), PAL.smoke(), 10, 1.0, 1.6);
               }
            })
            .hit((b, level, target, i) -> {
               Vec3d dest = this.spot != null ? this.spot : floor(level, this.at(4.0, 0.0, 0.0));
               Vec3d from = this.getEntityPos();
               if (this.blinkTo(dest.add(0.0, 0.6, 0.0))) {
                  if (target != null) {
                     this.faceTarget(target, 360.0F);
                  }

                  Vfx.dashTrail(level, from.add(0.0, 1.4, 0.0), this.getEntityPos().add(0.0, 1.4, 0.0), PAL);
               }
            })
      );
      out.add(
         BossAction.of("iron_vigil", 2.0, 0.9)
            .range(0.0, 40.0)
            .cooldown(600)
            .weight(6)
            .maxPhase(2)
            .tags("defensive")
            .superArmor()
            .anyTarget()
            .utility((b, t, c) -> b.warded() ? 0.0F : 1.0F)
            .hit((b, level, target, i) -> {
               this.shield(this.getMaxHealth() * 0.14F, 200);
               this.vigilTicks = 200;
               this.thornsAt.clear();
               this.vigilFx(level);
            })
      );
      out.add(
         BossAction.of("rollcall", 2.4, 1.3)
            .range(0.0, 30.0)
            .cooldown(700)
            .weight(7)
            .tags("summon")
            .anyTarget()
            .telegraph(0.6, 1.3)
            .when((b, t) -> this.minions() < Math.min(6, 3 + this.engaged().size()))
            .tick((b, level, target, t) -> {
               if (t == 12) {
                  this.points.clear();
                  int n = Math.min(6 - this.countMinions(level), this.rollcallSize());
                  Vec3d around = target != null ? target.getEntityPos() : this.getEntityPos();

                  for (int k = 0; k < n; k++) {
                     Vec3d s = BossFx.around(level, this, around, 4.0, 7.0);
                     this.points.add(s);
                     tele(level, s, 1.2, 14, PAL);
                  }
               }
            })
            .hit((b, level, target, i) -> this.rollcall(level, target))
      );
      out.add(
         BossAction.of("false_lock", 2.3, 1.45)
            .range(0.0, 6.5)
            .cooldown(160)
            .weight(0)
            .phase(1)
            .tags("melee aoe guard_break")
            .superArmor()
            .advance(1.2, 1.4, 0.12)
            .telegraph(0.3, 1.45)
            .tick((b, level, target, t) -> {
               if (t <= 29) {
                  this.descend(target, 29 - t);
               }

               if (t == 6) {
                  this.spot = floor(level, this.at(4.0, -0.3, 0.0));
                  tele(level, this.spot, 4.0, 23, PAL);
                  Vfx.chargeUp(level, this.keyRaised(), SOUL, 13);
               } else if (t == 19) {
                  Vec3d key = this.at(0.5, -0.3, 5.0);
                  Vfx.sparks(level, key, SOUL.core(), 10, 0.3, 0.16);
                  Vfx.flare(level, key, SOUL.glow(), 1.2, 4);
               }
            })
            .hit((b, level, target, i) -> this.lockSlam(level, 34.0F, 1.5, true))
      );
      out.add(
         BossAction.of("headsmans_drop", 2.4, 1.5)
            .range(7.0, 26.0)
            .cooldown(200)
            .weight(9)
            .phase(1)
            .tags("gap_closer aoe")
            .superArmor()
            .telegraph(0.2, 1.5)
            .combo("key_cleave", 0.3F)
            .utility((b, t, c) -> c.targetAirborne ? 0.4F : (c.dist > 9.0 ? 1.4F : 1.0F))
            .start((b, level, target, i) -> this.spot = null)
            .tick((b, level, target, t) -> {
               if (this.spot == null && t >= 4 && t <= 16) {
                  int left = 30 - t;
                  Vec3d land = floor(level, target != null ? this.predict(target, left) : this.at(8.0, 0.0, 0.0));
                  Vec3d dir = this.flatDir(land.subtract(this.getEntityPos()));
                  double hd = land.subtract(this.getEntityPos()).horizontalLength();
                  Vec3d body = floor(level, land.subtract(dir.multiply(Math.min(4.2, Math.max(0.0, hd - 2.0)))));
                  int len = MathHelper.clamp((int)Math.round(12.0 + body.subtract(this.getEntityPos()).horizontalLength() * 0.6), 14, 26);
                  if (len > left || t == 16) {
                     this.spot = land;
                     tele(level, land, 4.5, left, PAL);
                     this.leapTo(body, 6.0);
                  }
               }
            })
            .hit((b, level, target, i) -> {
               Vec3d land = this.spot != null ? this.spot : floor(level, this.getEntityPos());
               this.blast(level, land, 4.5, 38.0F, 1.5, true, null);
               Vfx.heavyImpact(level, land, PAL, 1.5);
               Vfx.groundCrack(level, land, 4.5, PAL);
            })
      );
      out.add(
         BossAction.of("penance", 2.6, 1.4)
            .range(0.0, 28.0)
            .cooldown(260)
            .weight(8)
            .phase(1)
            .tags("ranged aoe")
            .anyTarget()
            .telegraph(0.4, 1.4)
            .utility((b, t, c) -> c.engagedPlayers >= 2 ? 1.4F : 1.0F)
            .combo("headsmans_drop", 0.35F)
            .combo("grasping_dark", 0.3F)
            .tick((b, level, target, t) -> {
               if (t == 8) {
                  this.planPenance(level, target);
               }
            })
            .hit((b, level, target, i) -> this.penance(level))
      );
      out.add(
         BossAction.of("soul_pyre", 2.0, 1.0)
            .range(0.0, 22.0)
            .cooldown(220)
            .weight(9)
            .phase(1)
            .tags("ranged aoe")
            .anyTarget()
            .telegraph(0.3, 1.0)
            .tick((b, level, target, t) -> {
               if (t == 6) {
                  this.planPyre(level, target);
               }
            })
            .hit((b, level, target, i) -> {
               Vec3d lantern = this.at(2.6, 1.1, 0.65);

               for (Vec3d p : this.points) {
                  Vfx.beam(level, lantern, p, SOUL, 0.35, 6);
                  Vfx.pillar(level, p, SOUL, 3.0, 0.9);
                  this.blast(level, p, 2.2, 16.0F, 0.3, false, null);
                  this.zones.add(new HollowWardenBoss.Zone(p, 2.2, this.age + 100));
               }

               this.points.clear();
            })
      );
      out.add(
         BossAction.of("the_long_night", 4.4, 2.0, 2.9, 3.4)
            .range(0.0, 30.0)
            .cooldown(900)
            .weight(5)
            .phase(1)
            .tags("ranged aoe ultimate")
            .superArmor()
            .anyTarget()
            .telegraph(0.05, 3.4)
            .utility((b, t, c) -> this.ultGate())
            .start((b, level, target, i) -> this.lastUlt = this.age)
            .tick((b, level, target, t) -> {
               if (t <= 28) {
                  this.rise(0.09);
               } else if (t >= 70 && t <= 88) {
                  this.sink(this.nightCenter.y + 1.2, 88 - t);
               }

               if (t == 1) {
                  this.nightCenter = floor(level, this.getEntityPos());
                  Vfx.chargeUp(level, this.chest(), SOUL, 39);
                  Vfx.ward(level, this.nightCenter, 5.5, SOUL, 40);
                  Vfx.runeCircle(level, this.nightCenter, 5.5, SOUL, 40, 0.05F, Vfx.Rune.SOLAR);
                  tele(level, this.nightCenter, 24.0, 39, PAL);
               } else if (t == 30) {
                  Vfx.screen(level, this.getEntityPos(), 32.0, 0.6, 10, 656914, 0.4F);
               } else if (t == 41) {
                  tele(level, this.nightCenter, 5.5, 17, PAL);
               } else if (t == 62) {
                  Vfx.chargeUp(level, this.chest(), SOUL, 6);
               }
            })
            .hit((b, level, target, i) -> this.longNight(level, i))
      );
      out.add(
         BossAction.of("phantom_passage", 1.5, 0.6, 1.1)
            .range(4.0, 18.0)
            .cooldown(180)
            .weight(9)
            .phase(2)
            .tags("mobility gap_closer melee")
            .lockFacing()
            .telegraph(0.1, 1.1)
            .utility((b, t, c) -> c.kitingTicks > 40 ? 1.5F : 1.0F)
            .combo("backhand_rake", 0.55F)
            .tick((b, level, target, t) -> {
               if (t == 2) {
                  this.aimFrom = this.getEntityPos();
                  this.aimDir = target != null ? this.flatDir(this.predict(target, 10.0).subtract(this.aimFrom)) : this.facing();
                  this.passLen = MathHelper.clamp(this.ctx().hDist + 4.0, 6.0, 18.0);
                  float yaw = yawOf(this.aimDir);
                  this.setYaw(yaw);
                  this.setBodyYaw(yaw);
                  this.setHeadYaw(yaw);
                  Vfx.telegraphLine(level, floor(level, this.aimFrom), this.aimDir, this.passLen, 2.4, 10, PAL);
               }
            })
            .hit((b, level, target, i) -> this.passage(level, i))
      );
      out.add(
         BossAction.of("hall_of_chains", 3.0, 1.6)
            .range(0.0, 24.0)
            .cooldown(320)
            .weight(7)
            .phase(2)
            .tags("ranged aoe")
            .superArmor()
            .anyTarget()
            .telegraph(0.2, 1.6)
            .utility((b, t, c) -> 1.0F + 0.3F * Math.max(0, c.engagedPlayers - 1))
            .tick((b, level, target, t) -> {
               if (t == 4) {
                  this.planChains(level, target);
               }
            })
            .hit((b, level, target, i) -> this.chains(level))
      );
      out.add(
         BossAction.of("gloomwing_rend", 1.8, 0.8, 1.2)
            .range(0.0, 8.0)
            .cooldown(140)
            .weight(10)
            .phase(2)
            .tags("melee aoe")
            .advance(0.5, 1.2, 0.12)
            .telegraph(0.2, 1.2)
            .tick((b, level, target, t) -> {
               if (t <= 16) {
                  this.descend(target, 16 - t);
               }

               if (t == 4) {
                  this.cone(level, -20.0F, 60.0F, 8.0, 12);
               } else if (t == 17) {
                  this.cone(level, 20.0F, 60.0F, 8.0, 7);
               }
            })
            .hit((b, level, target, i) -> {
               this.rake(level, 8.0, 60.0F, i == 0 ? -20.0F : 20.0F, 24.0F, 1.4, null, i > 0);
               Vfx.slashArc(level, this.at(0.0, 0.0, 3.2), this.getYaw(), 20.0F, i == 0 ? 50.0F : -50.0F, 8.0, PAL, 160.0F);
               Vfx.streaks(level, this.at(1.5, 0.0, 1.6), this.facing(), PAL, 10);
            })
      );
      out.add(
         BossAction.of("life_sentence", 5.0, 1.2, 4.0)
            .range(0.0, 24.0)
            .cooldown(1100)
            .weight(5)
            .phase(2)
            .tags("aoe ultimate")
            .superArmor()
            .anyTarget()
            .telegraph(0.05, 4.0)
            .utility((b, t, c) -> this.ultGate() * (c.engagedPlayers >= 2 ? 1.3F : 1.0F))
            .start((b, level, target, i) -> {
               this.lastUlt = this.age;
               this.tethers.clear();
            })
            .tick((b, level, target, t) -> {
               if (t == 1) {
                  Vfx.chargeUp(level, this.chest(), SOUL, 23);
                  Vec3d f = floor(level, this.getEntityPos());

                  for (ServerPlayerEntity p : this.engaged()) {
                     double d = flatDist(p.getEntityPos(), f);
                     if (d <= 24.0 && d > 0.5) {
                        Vfx.telegraphLine(level, f, p.getEntityPos().subtract(f), d, 0.8, 23, SOUL);
                     }
                  }
               } else if (t == 50) {
                  tele(level, floor(level, this.getEntityPos()), 6.0, 30, SOUL);
               }

               if (t >= 25 && t <= 79) {
                  this.haul(level, t);
               }
            })
            .hit((b, level, target, i) -> this.sentence(level, i))
            .end((b, level, target, i) -> this.tethers.clear())
      );
      out.add(
         BossAction.of("hollow_verdict", 6.0, 2.0, 3.0, 4.2, 5.0)
            .range(0.0, 40.0)
            .cooldown(20)
            .weight(0)
            .phase(3)
            .tags("aoe ultimate")
            .superArmor()
            .anyTarget()
            .when((b, t) -> false)
            .telegraph(0.05, 5.0)
            .start((b, level, target, i) -> this.lastUlt = this.age)
            .tick((b, level, target, t) -> {
               if (t <= 20) {
                  this.rise(0.12);
               } else if (t >= 88 && t <= 99) {
                  this.sink(this.verdictCenter.y + 0.6, 99 - t);
               }

               if (t == 1) {
                  this.planVerdict(level);
               } else if (t == 42) {
                  for (int k = 0; k < 8; k++) {
                     Vfx.telegraphLine(level, this.verdictCenter, ray(this.verdictRot + 45 * k), 20.0, 1.6, 18, PAL);
                  }
               } else if (t == 86) {
                  tele(level, this.verdictCenter, 7.0, 14, SOUL);
               }
            })
            .hit((b, level, target, i) -> this.verdict(level, i))
      );
   }

   private void lockSlam(ServerWorld level, float base, double push, boolean feint) {
      Vec3d tip = this.spot != null ? this.spot : floor(level, this.at(4.0, -0.3, 0.0));

      for (ServerPlayerEntity p : this.engaged()) {
         if (flatDist(p.getEntityPos(), this.getEntityPos()) <= 7.0) {
            this.guardBreak(level, p, 100);
         }
      }

      this.blast(level, tip, 4.0, base, push, true, null);
      if (!feint) {
         for (LivingEntity e : this.foes(level, tip, 4.0, 1.5, 3.0)) {
            e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 1), this);
         }
      }

      Vfx.heavyImpact(level, tip, PAL, feint ? 1.4 : 1.2);
      Vfx.groundCrack(level, tip, 4.5, PAL);
      Vfx.runeCircle(level, tip, 2.0, SOUL, 40, 0.06F, Vfx.Rune.GLYPH);
      if (feint) {
         this.wave(level, floor(level, this.getEntityPos()), 8.0, 10, 16.0F, 1.0, SOUL);
         Vfx.shake(level, tip, 20.0, 0.5, 8);
      }
   }

   private void shackle(ServerWorld level) {
      Vec3d a = this.aimFrom;
      Vec3d end = a.add(this.aimDir.multiply(16.0));
      LivingEntity best = null;
      double bestD = Double.MAX_VALUE;

      for (LivingEntity e : this.foesOnSegment(level, a, end, 1.3, this.getY() - 3.5, this.getY() + this.getHeight())) {
         double d = flatDist(e.getEntityPos(), a);
         if (d < bestD) {
            bestD = d;
            best = e;
         }
      }

      Vec3d hand = this.at(2.5, 0.2, 2.25);
      if (best == null) {
         Vfx.beam(level, hand, end, SOUL, 0.35, 8);
         Vfx.sparks(level, end, SOUL.core(), 8, 0.3, 0.16);
      } else {
         Vec3d m = mid(best);
         Vfx.beam(level, hand, m, SOUL, 0.35, 8);
         Vfx.impact(level, m, SOUL, 0.8);
         if (best.damage(level, this.getDamageSources().mobAttack(this), this.dmg(14.0F))) {
            Vec3d pull = this.getEntityPos().subtract(best.getEntityPos()).getHorizontal().multiply(0.16);
            if (pull.length() > 2.2) {
               pull = pull.normalize().multiply(2.2);
            }

            best.setVelocity(pull.x, 0.35, pull.z);
            best.knockedBack = true;
            best.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 30, 1), this);
            if (pull.lengthSquared() > 1.0E-4) {
               Vfx.streaks(level, m, pull.normalize(), SOUL, 10);
            }
         }
      }
   }

   private void volley(ServerWorld level, @Nullable LivingEntity target, int i) {
      Vec3d from = this.volleyHand(i);
      Vec3d dir;
      if (target != null) {
         double flight = from.distanceTo(target.getEyePos()) / 0.8;
         dir = this.predict(target, flight).add(0.0, 1.1, 0.0).subtract(from).normalize();
      } else {
         dir = this.facing();
      }

      Vfx.flare(level, from, SOUL.glow(), 1.4, 5);
      this.pierce(
         BossBolt.shoot(level, this, BossBolt.Style.SHADOW, from, dir.multiply(0.8), this.dmg(9.0F))
            .homing(target, 0.03)
            .effect(new StatusEffectInstance(StatusEffects.WITHER, 60, 0))
            .life(80)
      );
      if (this.phase() >= 2) {
         for (int s = -1; s <= 1; s += 2) {
            this.pierce(
               BossBolt.shoot(level, this, BossBolt.Style.SHADOW, from, rotY(dir, 8 * s).multiply(0.8), this.dmg(9.0F))
                  .effect(new StatusEffectInstance(StatusEffects.WITHER, 60, 0))
                  .life(80)
            );
         }
      }

      if (this.enraged()) {
         this.pierce(
            BossBolt.shoot(level, this, BossBolt.Style.SHADOW, from, rotY(dir, i % 2 == 0 ? 4.0 : -4.0).multiply(0.8), this.dmg(9.0F))
               .effect(new StatusEffectInstance(StatusEffects.WITHER, 60, 0))
               .life(80)
         );
      }
   }

   private void pierce(BossBolt bolt) {
      this.piercing.add(bolt);
   }

   private void tickPiercing(ServerWorld level) {
      if (!this.piercing.isEmpty()) {
         Iterator<BossBolt> it = this.piercing.iterator();

         while (it.hasNext()) {
            BossBolt bolt = it.next();
            if (!bolt.isRemoved() && bolt.getEntityWorld() == level) {
               Vec3d from = bolt.getEntityPos();
               Vec3d to = from.add(bolt.getVelocity().multiply(1.2));

               for (LivingEntity e : level.getEntitiesByClass(LivingEntity.class, new Box(from, to).expand(1.0), this::foe)) {
                  if (e.timeUntilRegen > 10) {
                     Box box = e.getBoundingBox().expand(0.35);
                     if (box.contains(from) || box.raycast(from, to).isPresent()) {
                        e.timeUntilRegen = 0;
                     }
                  }
               }
            } else {
               it.remove();
            }
         }
      }
   }

   private void condemn(ServerWorld level) {
      Vec3d s = this.spot != null ? this.spot : floor(level, this.at(6.0, 0.0, 0.0));
      Vfx.lightning(level, s.add(0.0, 11.0, 0.0), s, SOUL, 3);
      Vfx.impact(level, s, SOUL, 1.0);
      DamageSource src = this.getDamageSources().indirectMagic(this, this);

      for (LivingEntity e : this.foes(level, s, 2.0, 1.5, 3.0)) {
         e.damage(level, src, this.dmg(12.0F));
         e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 50, 2), this);
         e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 60, 0), this);
         e.clearActiveItem();
      }

      this.condemned = null;
   }

   private void hook(ServerWorld level, @Nullable LivingEntity target) {
      Vec3d hand = this.hookHand();
      if (target != null && target.isAlive() && !(this.squaredDistanceTo(target) > 784.0) && this.canSee(target)) {
         Vec3d m = mid(target);
         Vfx.beam(level, hand, m, SOUL, 0.4, 10);
         Vfx.impact(level, m, SOUL, 0.8);
         target.damage(level, this.getDamageSources().indirectMagic(this, this), this.dmg(12.0F));
         if (target.isGliding()) {
            target.stopGliding();
         }

         Vec3d h = this.getEntityPos().subtract(target.getEntityPos()).getHorizontal().multiply(0.12);
         if (h.length() > 1.6) {
            h = h.normalize().multiply(1.6);
         }

         target.setVelocity(h.x, -1.4, h.z);
         target.knockedBack = true;
         this.hooked = target;
         this.hookStart = this.age;
      } else {
         Vfx.sparks(level, hand.add(this.facing().multiply(2.0)), SOUL.core(), 10, 0.35, 0.16);
         Vfx.flare(level, hand, SOUL.glow(), 1.0, 4);
      }
   }

   private void planGrasp(ServerWorld level, @Nullable LivingEntity target) {
      this.spots.clear();
      Vec3d c0 = floor(level, target != null ? this.predict(target, 18.0) : this.at(5.0, 0.0, 0.0));
      this.spots.add(new HollowWardenBoss.Spot(c0, 2.6, true));
      if (this.phase() >= 1) {
         for (int k = 0; k < 2; k++) {
            this.spots.add(new HollowWardenBoss.Spot(BossFx.around(level, this, c0, 2.5, 4.5), 2.0, false));
         }
      }

      int extra = 0;

      for (ServerPlayerEntity p : this.engaged()) {
         if (p != target && extra < 3) {
            this.spots.add(new HollowWardenBoss.Spot(floor(level, this.predict(p, 18.0)), 2.0, true));
            extra++;
         }
      }

      for (HollowWardenBoss.Spot s : this.spots) {
         tele(level, s.at(), s.r(), 18, PAL);
         Vfx.runeCircle(level, s.at(), s.r(), PAL, 18, 0.05F, Vfx.Rune.GLYPH);
      }
   }

   private void grasp(ServerWorld level) {
      DamageSource src = this.getDamageSources().mobAttack(this);
      Set<Integer> hit = new HashSet<>();

      for (HollowWardenBoss.Spot s : this.spots) {
         for (LivingEntity e : this.foes(level, s.at(), s.r(), 1.5, 3.0)) {
            if (hit.add(e.getId())) {
               if (e.damage(level, src, this.dmg(22.0F))) {
                  e.addVelocity(0.0, 0.6, 0.0);
                  e.knockedBack = true;
               }

               e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 30, 3), this);
            }
         }

         if (s.major()) {
            Vfx.heavyImpact(level, s.at(), PAL, 0.8);
         } else {
            Vfx.impact(level, s.at().add(0.0, 0.3, 0.0), PAL, 0.8);
         }

         Vfx.pillar(level, s.at(), PAL, 3.5, 0.9);
         Vfx.claw(level, s.at().add(0.0, 0.5, 0.0), this.getYaw(), -60.0F, 2.5, PAL);
      }

      this.spots.clear();
   }

   private Vec3d flankSpot(ServerWorld level, @Nullable LivingEntity target) {
      if (target == null) {
         return floor(level, this.at(4.0, 0.0, 0.0));
      }

      Vec3d facing = Vec3d.fromPolar(0.0F, target.getYaw());
      Vec3d left = Vec3d.fromPolar(0.0F, target.getYaw() - 90.0F);
      Vec3d[] tries = new Vec3d[]{facing.multiply(-3.5), left.multiply(3.5), left.multiply(-3.5)};

      for (Vec3d off : tries) {
         Vec3d p = target.getEntityPos().add(off);
         if (BossFx.safeGround(level, p, 3.0)) {
            return floor(level, p);
         }
      }

      return floor(level, target.getEntityPos().add(facing.multiply(3.5)));
   }

   private void vigilFx(ServerWorld level) {
      Vfx.shieldBubble(level, this.chest(), 2.2, PAL, 40);
      Vfx.ward(level, floor(level, this.getEntityPos()), 2.5, SOUL, 40);
   }

   private int rollcallSize() {
      return switch (this.phase()) {
         case 0 -> 2;
         case 1 -> 3;
         default -> 4;
      };
   }

   private void rollcall(ServerWorld level, @Nullable LivingEntity target) {
      int room = 6 - this.countMinions(level);
      int n = Math.min(room, this.points.size());
      int p = this.phase();

      for (int k = 0; k < n; k++) {
         Vec3d at = this.points.get(k);
         if (p != 0 && (p >= 3 || k >= 2) && (p < 3 || k >= 3)) {
            this.spawnRealm(level, p >= 3 ? "shade_wraith" : "gloom_hound", at, target);
         } else {
            this.spawnShade(level, at, target);
         }
      }

      this.minionCount = this.minionCount + Math.max(0, n);
      this.points.clear();
      if (this.engaged().size() >= 2) {
         List<ServerPlayerEntity> others = new ArrayList<>();

         for (ServerPlayerEntity sp : this.engaged()) {
            if (sp != target) {
               others.add(sp);
            }
         }

         if (!others.isEmpty()) {
            this.markTarget(others.get(this.getRandom().nextInt(others.size())), 100);
         }
      }

      Vfx.nova(level, this.chest(), SOUL, 6.0);
      Vfx.wisps(level, this.getEntityPos(), SOUL, 8);
   }

   private void planPenance(ServerWorld level, @Nullable LivingEntity target) {
      this.points.clear();
      int n = Math.min(Math.max(1, this.engaged().size()), 1 + this.phase());
      List<LivingEntity> who = new ArrayList<>();
      if (target != null) {
         who.add(target);
      }

      for (ServerPlayerEntity p : this.engaged()) {
         if (p != target && who.size() < n) {
            who.add(p);
         }
      }

      for (LivingEntity p : who) {
         Vec3d c = floor(level, this.predict(p, 20.0));
         this.points.add(c);
         tele(level, c, 2.2, 20, PAL);
         Vfx.runeCircle(level, c, 2.2, PAL, 20, 0.05F, Vfx.Rune.GLYPH);
      }
   }

   private void penance(ServerWorld level) {
      DamageSource src = this.getDamageSources().mobAttack(this);

      for (Vec3d c : this.points) {
         for (LivingEntity e : this.foes(level, c, 2.2, 1.5, 3.0)) {
            e.damage(level, src, this.dmg(18.0F));
            if (e.isAlive() && !this.imprisoned(e)) {
               this.cells.add(new HollowWardenBoss.Cell(c, e, this.age + 60));
            }
         }

         for (int k = 0; k < 10; k++) {
            double a = k * Math.PI * 2.0 / 10.0;
            Vec3d bar = c.add(Math.cos(a) * 2.2, 0.0, Math.sin(a) * 2.2);
            Vfx.beam(level, bar, bar.add(0.0, 3.2, 0.0), PAL, 0.18, 60);
         }

         Vfx.runeCircle(level, c, 2.2, PAL, 60, 0.02F, Vfx.Rune.GLYPH);
         Vfx.impact(level, c.add(0.0, 0.4, 0.0), PAL, 0.8);
      }

      this.points.clear();
   }

   private boolean imprisoned(LivingEntity e) {
      for (HollowWardenBoss.Cell c : this.cells) {
         if (c.who == e) {
            return true;
         }
      }

      return false;
   }

   private void planPyre(ServerWorld level, @Nullable LivingEntity target) {
      this.points.clear();
      Vec3d c0 = floor(level, target != null ? this.predict(target, 14.0) : this.at(5.0, 0.0, 0.0));
      this.points.add(c0);
      int total = this.phase() >= 2 ? 6 : 4;

      for (int k = 1; k < total; k++) {
         this.points.add(BossFx.around(level, this, c0, 3.0, 6.0));
      }

      if (this.phase() >= 2) {
         int extra = 0;

         for (ServerPlayerEntity p : this.engaged()) {
            if (p != target && extra < 3) {
               this.points.add(floor(level, this.predict(p, 14.0)));
               extra++;
            }
         }
      }

      for (Vec3d p : this.points) {
         tele(level, p, 2.2, 14, SOUL);
      }
   }

   private void longNight(ServerWorld level, int i) {
      Vec3d c = this.nightCenter;
      DamageSource src = this.getDamageSources().mobAttack(this);
      if (i == 0) {
         for (ServerPlayerEntity p : this.engaged()) {
            double d = flatDist(p.getEntityPos(), c);
            if (d > 5.5 && d <= 32.0) {
               this.guardBreak(level, p, 100);
               p.damage(level, src, this.dmg(52.0F));
               p.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 160, 0), this);
               p.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 60, 0), this);
            }
         }

         Vfx.nova(level, this.chest(), PAL, 24.0);
         Vfx.screen(level, this.getEntityPos(), 32.0, 0.8, 14, 656914, 0.7F);
      } else if (i == 1) {
         for (LivingEntity e : this.foes(level, c, 5.5, 1.5, 5.0)) {
            if (e.damage(level, src, this.dmg(34.0F))) {
               Vec3d away = this.flatDir(e.getEntityPos().subtract(c));
               e.addVelocity(away.x * 1.8, 0.4, away.z * 1.8);
               e.knockedBack = true;
            }
         }

         Vfx.heavyImpact(level, c, SOUL, 1.5);
      } else {
         boolean late = this.phase() >= 2;
         this.nightBolts(level, c, late ? 12 : 16, 0.0);
         if (late) {
            this.schedule(3, () -> this.nightBolts(level, c, 12, 15.0));
            this.shades(level, 2, this.getTarget());
         }

         Vfx.flare(level, this.chest(), SOUL.glow(), 3.0, 8);
         Vfx.nova(level, c.add(0.0, 1.2, 0.0), PAL, 4.0);
      }
   }

   private void nightBolts(ServerWorld level, Vec3d c, int n, double offsetDeg) {
      if (this.isAlive()) {
         for (int k = 0; k < n; k++) {
            Vec3d d = ray(offsetDeg + k * 360.0 / n);
            Vec3d from = c.add(d.multiply(1.6)).add(0.0, 1.3, 0.0);
            this.pierce(
               BossBolt.shoot(level, this, BossBolt.Style.SHADOW, from, d.multiply(0.7), this.dmg(8.0F))
                  .effect(new StatusEffectInstance(StatusEffects.WITHER, 40, 0))
                  .life(40)
            );
         }
      }
   }

   private void passage(ServerWorld level, int i) {
      if (i == 0) {
         Vec3d from = this.aimFrom;
         Vec3d dest = from.add(this.aimDir.multiply(this.passLen));
         Vec3d g = BossFx.ground(level, dest.x, dest.y, dest.z);
         if (g != null) {
            dest = new Vec3d(dest.x, g.y + 0.6, dest.z);
         }

         Vec3d start = this.getEntityPos();
         this.passStart = start;
         this.passEnd = this.blinkTo(dest) ? this.getEntityPos() : start;
         double travelled = flatDist(start, this.passEnd);
         if (!(travelled < 1.0)) {
            Vfx.dashTrail(level, start.add(0.0, 1.4, 0.0), this.passEnd.add(0.0, 1.4, 0.0), PAL);
            Vfx.streaks(level, start.lerp(this.passEnd, 0.5).add(0.0, 1.4, 0.0), this.aimDir, PAL, 12);
            DamageSource src = this.getDamageSources().mobAttack(this);
            double lo = Math.min(start.y, this.passEnd.y) - 3.5;
            double hi = Math.max(start.y, this.passEnd.y) + this.getHeight();

            for (LivingEntity e : this.foesOnSegment(level, start, this.passEnd, 1.6, lo, hi)) {
               e.damage(level, src, this.dmg(20.0F));
               e.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 40, 1), this);
            }

            Vfx.telegraphLine(level, floor(level, start), this.aimDir, travelled, 2.0, 10, PAL);
         }
      } else {
         Vec3d a = floor(level, this.passStart);
         Vec3d b = floor(level, this.passEnd);
         if (!(flatDist(a, b) < 1.0)) {
            DamageSource src = this.getDamageSources().mobAttack(this);

            for (LivingEntity e : this.foesOnSegment(level, a, b, 1.2, Math.min(a.y, b.y) - 1.5, Math.max(a.y, b.y) + 3.0)) {
               if (e.damage(level, src, this.dmg(14.0F))) {
                  e.addVelocity(0.0, 0.7, 0.0);
                  e.knockedBack = true;
               }
            }

            for (int k = 0; k < 3; k++) {
               Vfx.groundCrack(level, floor(level, a.lerp(b, (k + 0.5) / 3.0)), 1.6, PAL);
            }

            Vfx.explosion(level, a.lerp(b, 0.5).add(0.0, 0.6, 0.0), PAL, 2.0);
         }
      }
   }

   private void planChains(ServerWorld level, @Nullable LivingEntity target) {
      this.plan.clear();
      Vec3d c = floor(level, this.getEntityPos());
      int n = this.phase() >= 3 ? 8 : 6;
      double base;
      if (target != null) {
         double dx = target.getX() - c.x;
         double dz = target.getZ() - c.z;
         double d = Math.sqrt(dx * dx + dz * dz);
         double off = d > 1.6 ? Math.asin(Math.min(1.0, 1.5 / d)) : 0.5;
         base = Math.atan2(dz, dx) + (this.getRandom().nextBoolean() ? off : -off);
      } else {
         base = this.getRandom().nextDouble() * Math.PI * 2.0;
      }

      for (int k = 0; k < n; k++) {
         double a = base + k * Math.PI * 2.0 / n;
         Vec3d dir = new Vec3d(Math.cos(a), 0.0, Math.sin(a));
         this.plan.add(new HollowWardenBoss.Line(c, c.add(dir.multiply(18.0)), 0, 0));
         Vfx.telegraphLine(level, c, dir, 18.0, 1.4, 28, PAL);
      }
   }

   private void chains(ServerWorld level) {
      DamageSource src = this.getDamageSources().mobAttack(this);
      Set<Integer> hit = new HashSet<>();
      Vec3d chest = this.chest();

      for (HollowWardenBoss.Line l : this.plan) {
         for (LivingEntity e : this.foesOnSegment(level, l.a(), l.b(), 0.6, l.a().y - 1.5, l.a().y + 3.0)) {
            if (hit.add(e.getId())) {
               e.damage(level, src, this.dmg(22.0F));
               e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 1), this);
            }
         }

         this.lines.add(new HollowWardenBoss.Line(l.a(), l.b(), this.age, this.age + 100));
         Vfx.beam(level, chest, l.b().add(0.0, 0.4, 0.0), PAL, 0.3, 8);
         Vfx.beam(level, l.a().add(0.0, 0.3, 0.0), l.b().add(0.0, 0.3, 0.0), PAL, 0.25, 22);
         Vfx.sparks(level, l.b().add(0.0, 0.3, 0.0), PAL.core(), 8, 0.3, 0.16);
         Vfx.groundCrack(level, l.b(), 1.5, PAL);
      }

      this.plan.clear();
   }

   private void haul(ServerWorld level, int t) {
      if (!this.tethers.isEmpty()) {
         Vec3d chest = this.chest();
         Iterator<LivingEntity> it = this.tethers.values().iterator();

         while (it.hasNext()) {
            LivingEntity e = it.next();
            if (e.isAlive() && !e.isRemoved() && e.getEntityWorld() == level && !(this.squaredDistanceTo(e) > 1600.0)) {
               if (t % 2 == 1) {
                  Vfx.beam(level, chest, mid(e), SOUL, 0.3);
               }

               Vec3d to = this.getEntityPos().subtract(e.getEntityPos()).getHorizontal();
               double d = to.length();
               if (d > 1.5) {
                  drag(e, to.x / d * 0.07, to.z / d * 0.07);
               }

               if (t % 10 == 5) {
                  e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20, 0), this);
               }
            } else {
               it.remove();
            }
         }
      }
   }

   private void sentence(ServerWorld level, int i) {
      Vec3d chest = this.chest();
      if (i == 0) {
         this.tethers.clear();
         DamageSource magic = this.getDamageSources().indirectMagic(this, this);

         for (ServerPlayerEntity p : this.engaged()) {
            if (!(flatDist(p.getEntityPos(), this.getEntityPos()) > 24.0)) {
               p.damage(level, magic, this.dmg(10.0F));
               this.tethers.put(p.getUuid(), p);
               Vfx.beam(level, chest, mid(p), SOUL, 0.4, 6);
               Vfx.impact(level, mid(p), SOUL, 0.6);
            }
         }
      } else {
         DamageSource src = this.getDamageSources().mobAttack(this);

         for (LivingEntity e : new ArrayList<>(this.tethers.values())) {
            if (e.isAlive()) {
               if (e instanceof PlayerEntity pl) {
                  this.guardBreak(level, pl, 100);
               }

               e.damage(level, src, this.dmg(58.0F));
               Vec3d to = this.getEntityPos().subtract(e.getEntityPos()).getHorizontal();
               Vec3d v = to.lengthSquared() > 1.0E-4 ? to.normalize().multiply(1.6) : Vec3d.ZERO;
               e.setVelocity(v.x, 0.3, v.z);
               e.knockedBack = true;
               e.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 60, 1), this);
               Vfx.beam(level, chest, mid(e), SOUL, 0.5, 6);
            }
         }

         Vec3d f = floor(level, this.getEntityPos());

         for (LivingEntity e : this.foes(level, f, 6.0, 1.5, 6.0)) {
            if (!this.tethers.containsKey(e.getUuid()) && e.damage(level, src, this.dmg(30.0F))) {
               Vec3d away = this.flatDir(e.getEntityPos().subtract(f));
               e.addVelocity(away.x * 1.6, 0.4, away.z * 1.6);
               e.knockedBack = true;
            }
         }

         Vfx.impactFrame(level, chest, 24.0, 10285055, 0.8F);
         Vfx.nova(level, chest, SOUL, 8.0);
         Vfx.heavyImpact(level, f, PAL, 2.0);
         this.tethers.clear();
      }
   }

   private void planVerdict(ServerWorld level) {
      this.verdictCenter = floor(level, this.getEntityPos());
      this.verdictRot = this.getRandom().nextDouble() * 360.0;
      this.verdictCells.clear();

      for (int k = 0; k < 4; k++) {
         Vec3d p = this.verdictCenter.add(ray(this.verdictRot + 22.5 + 90 * k).multiply(9.0 + this.getRandom().nextDouble() * 2.0));
         Vec3d cell = floor(level, p);
         this.verdictCells.add(cell);
         Vfx.ward(level, cell, 2.0, SOUL, 100);
         Vfx.runeCircle(level, cell, 2.0, SOUL, 100, 0.04F, Vfx.Rune.GLYPH);
      }

      tele(level, this.verdictCenter, 22.0, 83, PAL);
      Vfx.chargeUp(level, this.chest(), SOUL, 39);
   }

   private void verdict(ServerWorld level, int i) {
      Vec3d c = this.verdictCenter;
      DamageSource src = this.getDamageSources().mobAttack(this);
      switch (i) {
         case 0:
            this.wave(level, c, 20.0, 16, 26.0F, 1.2, PAL);
            Vfx.nova(level, c, PAL, 6.0);
            break;
         case 1:
            Set<Integer> hit = new HashSet<>();
            Vec3d chest = this.chest();

            for (int k = 0; k < 8; k++) {
               Vec3d end = c.add(ray(this.verdictRot + 45 * k).multiply(20.0));

               for (LivingEntity e : this.foesOnSegment(level, c, end, 0.8, c.y - 1.5, c.y + 3.0)) {
                  if (hit.add(e.getId())) {
                     e.damage(level, src, this.dmg(30.0F));
                  }
               }

               Vfx.beam(level, chest, end.add(0.0, 0.3, 0.0), PAL, 0.35, 10);
            }
            break;
         case 2:
            for (ServerPlayerEntity p : this.engaged()) {
               if (!(flatDist(p.getEntityPos(), c) > 32.0)) {
                  boolean spared = false;

                  for (Vec3d cell : this.verdictCells) {
                     if (flatDist(p.getEntityPos(), cell) <= 2.0 && Math.abs(p.getY() - cell.y) <= 3.0) {
                        spared = true;
                        break;
                     }
                  }

                  if (spared) {
                     Vfx.glint(level, mid(p), SOUL.core(), 1.4);
                     Vfx.auraRing(level, p.getEntityPos(), SOUL);
                  } else {
                     this.guardBreak(level, p, 100);
                     p.damage(level, src, this.dmg(66.0F));
                     p.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 100, 1), this);
                     Vfx.lightning(level, p.getEntityPos().add(0.0, 12.0, 0.0), p.getEntityPos(), SOUL, 4);
                  }
               }
            }

            Vfx.impactFrame(level, c, 32.0, 15259903, 0.9F);
            Vfx.heavyImpact(level, c, PAL, 2.5);
            break;
         default:
            this.slam(level, c, 7.0, 30.0F, 2.0);
            Vfx.nova(level, this.chest(), SOUL, 7.0);
      }
   }

   @Override
   protected void onPhaseChange(ServerWorld level, int phase) {
      this.clearHazards();
      BossFx.current(this);
      Vec3d f = floor(level, this.getEntityPos());
      if (phase <= 1) {
         Vfx.chargeUp(level, this.chest(), SOUL, 27);
         tele(level, f, 7.0, 27, SOUL);
      } else if (phase == 2) {
         this.burstAngle = this.getRandom().nextDouble() * 45.0;

         for (int k = 0; k < 8; k++) {
            Vfx.telegraphLine(level, f, ray(this.burstAngle + 45 * k), 14.0, 1.4, 27, PAL);
         }
      } else {
         Vfx.chargeUp(level, this.head(), VIOLET, 27);
         Vfx.runeCircle(level, f, 10.0, VIOLET, 60, 0.03F, Vfx.Rune.ASTRAL);
         tele(level, f, 10.0, 27, VIOLET);
      }
   }

   @Override
   protected void phaseBurst(ServerWorld level, int phase) {
      this.setForm(phase);
      BossFx.current(this);
      Vec3d f = floor(level, this.getEntityPos());
      Vec3d c = this.chest();
      if (phase <= 1) {
         Vfx.nova(level, c, SOUL, 7.0);
         this.blast(level, f, 7.0, 12.0F, 1.6, true, null);
         Vfx.shards(level, this.head(), 5920868, 16, 0.5);
         Vfx.debris(level, this.head(), Vfx.Debris.ASH, PAL.smoke(), 12, 0.3);
         Vfx.heavyImpact(level, f, PAL, 1.2);
         this.shades(level, 2, this.getTarget());
      } else if (phase == 2) {
         Vfx.shards(level, c.add(0.0, 0.6, 0.0), 5920868, 24, 0.6);
         Vfx.debris(level, c, Vfx.Debris.ASH, PAL.smoke(), 20, 0.4);
         DamageSource src = this.getDamageSources().mobAttack(this);
         Set<Integer> hit = new HashSet<>();

         for (int k = 0; k < 8; k++) {
            Vec3d dir = ray(this.burstAngle + 45 * k);
            Vec3d end = f.add(dir.multiply(14.0));

            for (LivingEntity e : this.foesOnSegment(level, f, end, 0.7, f.y - 1.5, f.y + 3.0)) {
               if (hit.add(e.getId()) && e.damage(level, src, this.dmg(20.0F))) {
                  e.addVelocity(dir.x * 1.2, 0.35, dir.z * 1.2);
                  e.knockedBack = true;
               }
            }

            Vfx.beam(level, c, end.add(0.0, 0.4, 0.0), PAL, 0.3, 8);
         }

         Vec3d back = this.facing().multiply(-1.0);
         Vec3d left = this.left();
         Vfx.streaks(level, c, back.add(left).normalize(), PAL, 12);
         Vfx.streaks(level, c, back.subtract(left).normalize(), PAL, 12);
         this.spawnRealms(level, "gloom_hound", 2, this.getTarget());
      } else {
         Vfx.impactFrame(level, c, 24.0, 15259903, 0.6F);
         Vfx.nova(level, c, VIOLET, 10.0);
         this.slam(level, f, 10.0, 16.0F, 2.0);

         for (ServerPlayerEntity p : this.engaged()) {
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 100, 0), this);
         }

         this.spawnRealms(level, "shade_wraith", 2, this.getTarget());
         this.restlessTimer = 100;
      }
   }

   @Override
   protected void onStagger(ServerWorld level) {
      this.clearHazards();
      Vfx.flare(level, this.chest(), SOUL.glow(), 1.0, 6);
      Vfx.debris(level, this.chest(), Vfx.Debris.ASH, PAL.smoke(), 10, 0.25);
   }

   @Override
   protected void onEnrage(ServerWorld level) {
      Vfx.auraRing(level, this.getEntityPos(), PAL, 3.5);
      Vfx.wisps(level, this.chest(), SOUL, 8);
      this.shades(level, 2, this.getTarget());

      for (String a : ENRAGE_HASTE) {
         this.setCooldown(a, this.cooldownLeft(a) / 2);
      }
   }

   @Override
   protected void desperation(ServerWorld level) {
      BossAction m = this.currentMove();
      if (m != null && m.anim().startsWith("phase_")) {
         this.verdictPending = true;
      } else {
         this.shield(this.getMaxHealth() * 0.08F, 140);
         this.forceAction(level, "hollow_verdict");
      }
   }

   private void clearHazards() {
      this.cells.clear();
      this.tethers.clear();
      this.lines.clear();
      this.restless.clear();
      this.restlessTimer = 0;
      this.vigilTicks = 0;
      this.immuneTicks = 0;
      this.thornsAt.clear();
   }

   @Override
   public boolean damage(ServerWorld world, DamageSource source, float amount) {
      boolean bypass = source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY);
      if (this.immuneTicks > 0 && !bypass) {
         return false;
      }

      boolean thorns = this.vigilTicks > 0 && this.warded();
      boolean hit = super.damage(world, source, amount);
      if (hit && source.getAttacker() instanceof LivingEntity attacker && attacker != this) {
         if (this.tethers.remove(attacker.getUuid()) != null) {
            Vec3d m = this.chest().lerp(mid(attacker), 0.5);
            Vfx.shards(world, m, SOUL.glow(), 14, 0.45);
            Vfx.flare(world, m, SOUL.glow(), 1.6, 6);
         }

         if (thorns
            && attacker instanceof PlayerEntity p
            && !p.isCreative()
            && !p.isSpectator()
            && flatDist(p.getEntityPos(), this.getEntityPos()) <= 4.5 + this.getWidth() * 0.5) {
            int last = this.thornsAt.getOrDefault(p.getUuid(), -100);
            if (this.age - last >= 10) {
               this.thornsAt.put(p.getUuid(), this.age);
               p.damage(world, this.getDamageSources().indirectMagic(this, this), this.dmg(6.0F));
               Vec3d away = this.flatDir(p.getEntityPos().subtract(this.getEntityPos()));
               p.addVelocity(away.x, 0.3, away.z);
               p.knockedBack = true;
               Vfx.sparks(world, mid(p), SOUL.core(), 8, 0.3, 0.16);
            }
         }
      }

      return hit;
   }

   @Override
   protected void mobTick(ServerWorld world) {
      super.mobTick(world);
      if (this.isAlive() && !this.inIntro()) {
         BossFx.current(this);
         if (this.immuneTicks > 0) {
            this.immuneTicks--;
         }

         BossAction move = this.currentMove();
         boolean phaseClip = move != null && move.anim().startsWith("phase_");
         if (this.form() != this.phase() && !phaseClip) {
            this.setForm(this.phase());
         }

         if (this.verdictPending && !phaseClip) {
            this.verdictPending = false;
            this.shield(this.getMaxHealth() * 0.08F, 140);
            this.forceAction(world, "hollow_verdict");
            move = this.currentMove();
         }

         this.tickCells(world);
         this.tickLines(world);
         this.tickZones(world);
         this.tickRestless(world, move, phaseClip);
         this.tickHook(world);
         this.tickPiercing(world);
         this.tickVigil(world);
         this.ambient(world);
         this.dodgeWake(world);
         if (this.getTarget() == null) {
            if (++this.lonely == 100) {
               this.clearHazards();
            }
         } else {
            this.lonely = 0;
         }
      }
   }

   private void tickCells(ServerWorld level) {
      if (!this.cells.isEmpty()) {
         Iterator<HollowWardenBoss.Cell> it = this.cells.iterator();

         while (it.hasNext()) {
            HollowWardenBoss.Cell c = it.next();
            LivingEntity e = c.who;
            if (this.age < c.until && e.isAlive() && !e.isRemoved() && e.getEntityWorld() == level) {
               double d = flatDist(e.getEntityPos(), c.center);
               if (d > 2.2) {
                  Vec3d to = new Vec3d(c.center.x - e.getX(), 0.0, c.center.z - e.getZ()).multiply(0.6 / d);
                  e.setVelocity(to.x, Math.min(0.1, known(e).y), to.z);
                  e.knockedBack = true;
                  if (++c.tries % 10 == 0) {
                     e.damage(level, this.getDamageSources().indirectMagic(this, this), this.dmg(4.0F));
                     sound(level, c.center, "penance.hit", 2.0F);
                     Vfx.sparks(level, mid(e), PAL.core(), 8, 0.3, 0.16);
                  }
               } else if (this.age % 10 == 0) {
                  e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 15, 0), this);
               }
            } else {
               it.remove();
               if (this.age >= c.until) {
                  Vfx.shards(level, c.center.add(0.0, 1.2, 0.0), PAL.glow(), 14, 0.45);
               }
            }
         }
      }
   }

   private void tickLines(ServerWorld level) {
      if (!this.lines.isEmpty()) {
         this.lines.removeIf(lx -> this.age >= lx.until());

         for (HollowWardenBoss.Line l : this.lines) {
            int age = this.age - l.born();
            if (age > 0 && age % 20 == 0) {
               Vfx.beam(level, l.a().add(0.0, 0.3, 0.0), l.b().add(0.0, 0.3, 0.0), PAL, 0.25, 22);
            }
         }

         if (this.age % 10 == 0 && !this.lines.isEmpty()) {
            DamageSource magic = this.getDamageSources().indirectMagic(this, this);

            for (ServerPlayerEntity p : this.engaged()) {
               for (HollowWardenBoss.Line l : this.lines) {
                  if (Math.abs(p.getY() - l.a().y) <= 2.5 && segDist(p.getEntityPos(), l.a(), l.b()) <= 0.8) {
                     p.damage(level, magic, this.dmg(6.0F));
                     p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20, 0), this);
                     break;
                  }
               }
            }
         }
      }
   }

   private void tickZones(ServerWorld level) {
      if (!this.zones.isEmpty()) {
         DamageSource magic = this.getDamageSources().indirectMagic(this, this);
         Iterator<HollowWardenBoss.Zone> it = this.zones.iterator();

         while (it.hasNext()) {
            HollowWardenBoss.Zone z = it.next();
            int left = z.until() - this.age;
            if (left <= 0) {
               it.remove();
            } else {
               if (this.age % 5 == 0) {
                  level.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, z.at().x, z.at().y + 0.3, z.at().z, 5, z.r() * 0.6, 0.15, z.r() * 0.6, 0.01);
               }

               if (left % 20 == 10) {
                  Vfx.aura(level, z.at(), SOUL, z.r());

                  for (LivingEntity e : this.foes(level, z.at(), z.r(), 1.0, 3.0)) {
                     e.damage(level, magic, this.dmg(4.0F));
                     e.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 40, 0), this);
                  }
               }
            }
         }
      }
   }

   private void tickRestless(ServerWorld level, @Nullable BossAction move, boolean phaseClip) {
      if (!this.restless.isEmpty()) {
         Vec3d struck = null;
         Iterator<HollowWardenBoss.Strike> it = this.restless.iterator();

         while (it.hasNext()) {
            HollowWardenBoss.Strike s = it.next();
            if (this.age >= s.when()) {
               it.remove();
               this.restlessStrike(level, s.at());
               struck = s.at();
            }
         }

         if (struck != null) {
            sound(level, struck, "grasping_dark.hit", 3.0F);
         }
      }

      if (this.phase() < 3) {
         this.restlessTimer = 0;
      } else {
         int interval = this.enraged() ? 100 : 140;
         if (this.restlessTimer <= 0) {
            this.restlessTimer = interval;
         } else {
            boolean paused = phaseClip || this.staggered() || move != null && move.anim().equals("hollow_verdict");
            if (!paused && --this.restlessTimer <= 0) {
               this.restlessTimer = interval;
               int n = 0;

               for (ServerPlayerEntity p : this.engaged()) {
                  if (n++ >= 3) {
                     break;
                  }

                  Vec3d s = floor(level, this.predict(p, 30.0));
                  tele(level, s, 2.0, 30, PAL);
                  this.restless.add(new HollowWardenBoss.Strike(s, this.age + 30));
               }
            }
         }
      }
   }

   private void restlessStrike(ServerWorld level, Vec3d s) {
      DamageSource src = this.getDamageSources().mobAttack(this);

      for (LivingEntity e : this.foes(level, s, 2.0, 1.5, 3.0)) {
         if (e.damage(level, src, this.dmg(16.0F))) {
            e.addVelocity(0.0, 0.5, 0.0);
            e.knockedBack = true;
         }

         e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 1), this);
      }

      Vfx.pillar(level, s, PAL, 4.0, 0.8);
      Vfx.groundCrack(level, s, 2.0, PAL);
   }

   private void tickHook(ServerWorld level) {
      LivingEntity e = this.hooked;
      if (e != null) {
         if (e.isAlive() && !e.isRemoved() && e.getEntityWorld() == level && this.age <= this.hookStart + 30) {
            if (this.age >= this.hookStart + 2 && e.isOnGround()) {
               this.hooked = null;
               Vec3d land = floor(level, e.getEntityPos());
               e.timeUntilRegen = 0;
               this.blast(level, land, 2.5, 20.0F, 0.8, true, null);
               e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 1), this);
               Vfx.heavyImpact(level, land, PAL, 1.0);
               sound(level, land, "headsmans_drop.hit", 3.0F);
            }
         } else {
            this.hooked = null;
         }
      }
   }

   private void tickVigil(ServerWorld level) {
      if (this.vigilTicks > 0) {
         if (!this.warded()) {
            this.vigilTicks = 0;
         } else {
            this.vigilTicks--;
            if (this.vigilTicks == 30) {
               tele(level, floor(level, this.getEntityPos()), 7.0, 30, SOUL);
            }

            if (this.vigilTicks == 0) {
               Vec3d f = floor(level, this.getEntityPos());
               this.slam(level, f, 7.0, 26.0F, 1.6);
               Vfx.nova(level, this.chest(), SOUL, 7.0);
               this.thornsAt.clear();
            } else if (this.vigilTicks % 40 == 0) {
               this.vigilFx(level);
            }
         }
      }
   }

   private void ambient(ServerWorld level) {
      int t = this.age;
      if (t % 4 == 0) {
         Vfx.smoke(level, this.getEntityPos().add(0.0, 0.2, 0.0), PAL.smoke(), 2, 0.6, 1.4);
      }

      if (t % 20 == 0) {
         Vfx.embers(level, this.getEntityPos().add(0.0, 1.5, 0.0), PAL.glow(), 3, 0.9);
      }

      int f = this.form();
      if (f >= 1 && t % 10 == 0) {
         Vfx.flare(level, this.chest(), SOUL.glow(), 0.8, 5);
      }

      if (f >= 2 && t % 8 == 0) {
         Vfx.smoke(level, this.at(-0.4, t % 16 == 0 ? 1.3 : -1.3, 3.2), PAL.smoke(), 2, 0.4, 1.0);
      }

      if (f >= 3 && t % 6 == 0) {
         Vfx.embers(level, this.head(), VIOLET.glow(), 2, 0.6);
      }
   }

   private void dodgeWake(ServerWorld level) {
      boolean d = this.dodging();
      if (d && !this.wasDodging) {
         this.dodgeFrom = this.getEntityPos();
         this.dodgeWake = 0;
      }

      this.wasDodging = d;
      if (d) {
         this.dodgeWake++;
         if (this.dodgeWake == 5 || this.dodgeWake == 8) {
            Vfx.dashTrail(level, this.dodgeFrom.add(0.0, 1.6, 0.0), this.getEntityPos().add(0.0, 1.6, 0.0), PAL);
            this.dodgeFrom = this.getEntityPos();
         }
      }
   }

   @Override
   protected void onIntroTick(ServerWorld level, int left) {
      BossFx.current(this);
      Vec3d f = floor(level, this.getEntityPos());
      switch (left) {
         case 14:
            Vfx.nova(level, this.chest(), PAL, 6.0);
            Vfx.runeCircle(level, this.at(4.4, 0.9, 3.3), 0.9, SOUL, 20, 0.1F, Vfx.Rune.GLYPH);
            Vfx.shake(level, this.getEntityPos(), 24.0, 0.35, 8);
            break;
         case 44:
            Vfx.glint(level, this.at(1.5, -1.55, 1.8), SOUL.core(), 1.6);
            break;
         case 64:
            Vfx.wisps(level, this.chest(), SOUL, 8);
            break;
         case 72:
            Vfx.flare(level, this.head(), SOUL.core(), 2.2, 8);
            break;
         case 116:
            for (int k = 0; k < 4; k++) {
               Vec3d p = f.add(ray(45 + 90 * k).multiply(3.0));
               Vfx.beam(level, p, p.add(0.0, 5.0, 0.0), SOUL, 0.3, 50);
            }
            break;
         case 139:
            this.setForm(0);
            Vfx.runeCircle(level, f, 4.0, PAL, 140, 0.03F, Vfx.Rune.GLYPH);
            Vfx.vortex(level, f, 2.5, 4.0, PAL, 60);
            break;
         default:
            if (left > 0 && left % 10 == 0) {
               if (left % 40 == 0) {
                  Vfx.wisps(level, f.add(0.0, 0.6, 0.0), SOUL, 3, 1.4, 30);
               } else {
                  Vfx.smoke(level, f.add(0.0, 0.2, 0.0), PAL.smoke(), 6, 1.6, 1.8);
               }
            }
      }
   }

   @Override
   protected void onDeathTick(ServerWorld level, int tick) {
      BossFx.current(this);
      if (tick <= 60) {
         Vec3d g = BossFx.ground(level, this.getX(), this.getY(), this.getZ());
         if (g != null && g.y <= this.getY() + 0.5) {
            this.sink(g.y + this.hoverHeight(), Math.max(2, 30 - tick));
         }
      }

      switch (tick) {
         case 1:
            this.clearHazards();
            this.zones.clear();
            this.hooked = null;
            this.verdictPending = false;
            Vfx.flare(level, this.head(), SOUL.core(), 2.5, 8);
            break;
         case 14:
         case 20:
         case 26:
            Vec3d p = tick == 14 ? this.at(-0.8, 0.5, 1.4) : (tick == 20 ? this.at(-0.8, -0.5, 1.4) : this.at(0.5, 0.7, 1.1));
            Vfx.sparks(level, p, SOUL.core(), 10, 0.35, 0.16);
            Vfx.shards(level, p, 5920868, 8, 0.4);
            break;
         case 48:
            Vec3d l = this.at(-0.35, 0.4, 0.0);
            Vfx.explosion(level, l, SOUL, 1.5);
            Vfx.debris(level, l, Vfx.Debris.ASH, SOUL.glow(), 12, 0.35);
            break;
         case 60:
            Vec3d tip = floor(level, this.at(2.75, -0.85, 0.0));
            Vfx.heavyImpact(level, tip, PAL, 0.8);
            Vfx.groundCrack(level, tip, 2.0, PAL);
            break;
         case 104:
            Vec3d c = this.chest();
            Vfx.wisps(level, c, SOUL, 12, 3.0, 80);
            Vfx.nova(level, c, SOUL, 5.0);
            Vfx.pillar(level, this.getEntityPos(), SOUL, 8.0, 1.2);
            Vfx.radialFlash(level, c, 24.0, 10285055, 0.5F);
            break;
         default:
            if (tick >= 80 && tick <= 100 && tick % 6 == 2 || tick >= 120 && tick % 5 == 0) {
               Vfx.smoke(level, this.getEntityPos().add(0.0, 0.4, 0.0), PAL.smoke(), 6, 1.2, 1.6);
            }
      }
   }

   private static boolean isAdd(LivingEntity e) {
      if (!e.isAlive()) {
         return false;
      } else {
         return e instanceof WardenShade ? true : e instanceof RealmMob m && ("shade_wraith".equals(m.bossId()) || "gloom_hound".equals(m.bossId()));
      }
   }

   private int countMinions(ServerWorld level) {
      this.minionCount = level.getEntitiesByClass(LivingEntity.class, this.getBoundingBox().expand(32.0), HollowWardenBoss::isAdd).size();
      this.minionsAt = this.age;
      return this.minionCount;
   }

   private int minions() {
      if (this.age - this.minionsAt >= 20 && this.getEntityWorld() instanceof ServerWorld level) {
         this.countMinions(level);
      }

      return this.minionCount;
   }

   private void shades(ServerWorld level, int n, @Nullable LivingEntity target) {
      int k = Math.min(n, 6 - this.countMinions(level));

      for (int i = 0; i < k; i++) {
         this.spawnShade(level, BossFx.around(level, this, this.getEntityPos(), 3.0, 6.0), target);
      }

      this.minionCount = this.minionCount + Math.max(0, k);
   }

   private void spawnRealms(ServerWorld level, String id, int n, @Nullable LivingEntity target) {
      int k = Math.min(n, 6 - this.countMinions(level));

      for (int i = 0; i < k; i++) {
         this.spawnRealm(level, id, BossFx.around(level, this, this.getEntityPos(), 3.0, 7.0), target);
      }

      this.minionCount = this.minionCount + Math.max(0, k);
   }

   private void spawnShade(ServerWorld level, Vec3d at, @Nullable LivingEntity target) {
      WardenShade shade = (WardenShade)BossEntities.WARDEN_SHADE.create(level, SpawnReason.MOB_SUMMONED);
      if (shade != null) {
         shade.refreshPositionAndAngles(at.x, at.y + 0.6, at.z, this.getYaw(), 0.0F);
         if (target != null) {
            shade.setTarget(target);
         }

         level.spawnEntity(shade);
         Vfx.pillar(level, at, PAL, 4.0, 0.9);
         Vfx.smoke(level, at.add(0.0, 1.0, 0.0), PAL.smoke(), 6, 0.8, 2.0);
      }
   }

   private void spawnRealm(ServerWorld level, String id, Vec3d at, @Nullable LivingEntity target) {
      EntityType<RealmMob> type = RealmMobs.type(id);
      if (type != null) {
         RealmMob mob = (RealmMob)type.create(level, SpawnReason.MOB_SUMMONED);
         if (mob != null) {
            double up = "shade_wraith".equals(id) ? 1.5 : 0.0;
            mob.refreshPositionAndAngles(at.x, at.y + up, at.z, this.getRandom().nextFloat() * 360.0F, 0.0F);
            if (target != null) {
               mob.setTarget(target);
            }

            level.spawnEntity(mob);
            Vfx.pillar(level, at, PAL, 3.0, 0.8);
            Vfx.smoke(level, at.add(0.0, 0.8, 0.0), PAL.smoke(), 5, 0.6, 1.6);
         }
      }
   }

   private float ultGate() {
      return this.age - this.lastUlt < 400 ? 0.0F : 1.0F;
   }

   private static int usingKind(LivingEntity e) {
      if (!e.isUsingItem()) {
         return 0;
      } else {
         UseAction u = e.getActiveItem().getUseAction();
         if (u != UseAction.EAT && u != UseAction.DRINK) {
            return u != UseAction.BOW && u != UseAction.CROSSBOW && u != UseAction.TRIDENT && u != UseAction.SPEAR ? 0 : 1;
         } else {
            return 2;
         }
      }
   }

   private boolean anyUsing() {
      for (ServerPlayerEntity p : this.engaged()) {
         if (usingKind(p) > 0) {
            return true;
         }
      }

      return false;
   }

   private @Nullable LivingEntity pickCondemned(@Nullable LivingEntity target) {
      LivingEntity aiming = null;

      for (ServerPlayerEntity p : this.engaged()) {
         int k = usingKind(p);
         if (k == 2) {
            return p;
         }

         if (k == 1 && aiming == null) {
            aiming = p;
         }
      }

      return aiming != null ? aiming : target;
   }

   private void guardBreak(ServerWorld level, PlayerEntity p, int ticks) {
      ItemStack s = p.getBlockingItem();
      if (s != null) {
         p.getItemCooldownManager().set(s, ticks);
         p.clearActiveItem();
         Vec3d m = mid(p).add(Vec3d.fromPolar(0.0F, p.getYaw()).multiply(0.4));
         Vfx.shards(level, m, 5920868, 12, 0.45);
         Vfx.sparks(level, m, SOUL.core(), 10, 0.35, 0.16);
      }
   }

   private Vec3d at(double fwd, double left, double up) {
      Vec3d f = this.facing();
      Vec3d l = this.left();
      return this.getEntityPos().add(f.x * fwd + l.x * left, up, f.z * fwd + l.z * left);
   }

   private Vec3d facing() {
      return Vec3d.fromPolar(0.0F, this.getYaw());
   }

   private Vec3d left() {
      return Vec3d.fromPolar(0.0F, this.getYaw() - 90.0F);
   }

   private Vec3d chest() {
      return this.at(0.1, 0.0, 2.7);
   }

   private Vec3d head() {
      return this.at(0.35, 0.0, 3.6);
   }

   private Vec3d keyRaised() {
      return this.at(1.0, -0.2, 4.6);
   }

   private Vec3d hookHand() {
      return this.at(1.2, -0.4, 4.4);
   }

   private Vec3d volleyHand(int i) {
      return switch (i) {
         case 0 -> this.at(2.2, 1.1, 2.45);
         case 1 -> this.at(2.3, -1.1, 2.45);
         default -> this.at(2.0, 0.0, 2.5);
      };
   }

   private static Vec3d mid(LivingEntity e) {
      return e.getEntityPos().add(0.0, e.getHeight() * 0.5, 0.0);
   }

   private static Vec3d floor(ServerWorld level, Vec3d p) {
      Vec3d g = BossFx.ground(level, p.x, p.y, p.z);
      return g != null ? g : p;
   }

   private static Vec3d ray(double deg) {
      double a = Math.toRadians(deg);
      return new Vec3d(Math.cos(a), 0.0, Math.sin(a));
   }

   private static Vec3d rotY(Vec3d v, double deg) {
      return v.rotateY((float)Math.toRadians(deg));
   }

   private Vec3d flatDir(Vec3d v) {
      Vec3d h = new Vec3d(v.x, 0.0, v.z);
      return h.lengthSquared() < 1.0E-6 ? this.facing() : h.normalize();
   }

   private static float yawOf(Vec3d dir) {
      return (float)(MathHelper.atan2(dir.z, dir.x) * 180.0F / (float)Math.PI) - 90.0F;
   }

   private static double flatDist(Vec3d a, Vec3d b) {
      double dx = a.x - b.x;
      double dz = a.z - b.z;
      return Math.sqrt(dx * dx + dz * dz);
   }

   private static double segDist(Vec3d p, Vec3d a, Vec3d b) {
      double abx = b.x - a.x;
      double abz = b.z - a.z;
      double len2 = abx * abx + abz * abz;
      double s = len2 < 1.0E-6 ? 0.0 : MathHelper.clamp(((p.x - a.x) * abx + (p.z - a.z) * abz) / len2, 0.0, 1.0);
      double dx = p.x - (a.x + abx * s);
      double dz = p.z - (a.z + abz * s);
      return Math.sqrt(dx * dx + dz * dz);
   }

   private static Vec3d known(LivingEntity e) {
      return e instanceof ServerPlayerEntity sp ? sp.getMovement() : e.getVelocity();
   }

   private static void drag(LivingEntity e, double x, double z) {
      if (e instanceof ServerPlayerEntity sp) {
         if (sp.hasVehicle()) {
            return;
         }

         Vec3d k = sp.getMovement();
         double f = sp.isGliding() ? 0.99 : (sp.isTouchingWater() ? 0.8 : (sp.isOnGround() ? 0.546 : 0.91));
         double y = sp.isOnGround() ? -0.0784 : (sp.isGliding() ? k.y * 0.98 : (k.y - 0.08) * 0.98);
         sp.setVelocity(k.x * f + x, y, k.z * f + z);
      } else {
         e.addVelocity(x, 0.0, z);
      }

      e.knockedBack = true;
   }

   private void rise(double vy) {
      Vec3d v = this.getVelocity();
      this.setVelocity(v.x, vy, v.z);
   }

   private void sink(double y, int ticksLeft) {
      Vec3d v = this.getVelocity();
      this.setVelocity(v.x, MathHelper.clamp((y - this.getY()) / Math.max(2, ticksLeft), -0.35, 0.2), v.z);
   }

   private void descend(@Nullable LivingEntity target, int ticksLeft) {
      if (target != null) {
         this.sink(target.getY() + 0.3, ticksLeft);
      }
   }

   private static void tele(ServerWorld level, Vec3d c, double r, int ticks, Vfx.Palette p) {
      Vfx.telegraphCircle(level, c, r, Math.max(1, ticks), p);
   }

   private void cone(ServerWorld level, float yawOffset, float halfAngle, double range, int ticks) {
      Vfx.telegraphCone(level, floor(level, this.getEntityPos()), this.getYaw() + yawOffset, halfAngle, range, ticks, PAL);
   }

   private static void sound(ServerWorld level, Vec3d at, String key, float volume) {
      BossSounds.play(level, at, "boss.hollow_warden." + key, volume, 0.94F + level.getRandom().nextFloat() * 0.12F);
   }

   private boolean foe(LivingEntity e) {
      return e != this
         && e.isAlive()
         && !(e instanceof MysticalBoss)
         && !(e instanceof WardenShade)
         && !(e instanceof PlayerEntity p && (p.isCreative() || p.isSpectator()));
   }

   private List<LivingEntity> foes(ServerWorld level, Vec3d c, double r, double below, double above) {
      return level.getEntitiesByClass(
         LivingEntity.class,
         new Box(c.x - r - 1.0, c.y - below, c.z - r - 1.0, c.x + r + 1.0, c.y + above, c.z + r + 1.0),
         e -> this.foe(e) && e.getY() >= c.y - below && e.getY() <= c.y + above && flatDist(e.getEntityPos(), c) <= r + e.getWidth() * 0.5
      );
   }

   private List<LivingEntity> foesOnSegment(ServerWorld level, Vec3d a, Vec3d b, double width, double yLo, double yHi) {
      Box box = new Box(
         Math.min(a.x, b.x) - width - 1.0, yLo, Math.min(a.z, b.z) - width - 1.0, Math.max(a.x, b.x) + width + 1.0, yHi + 0.1, Math.max(a.z, b.z) + width + 1.0
      );
      return level.getEntitiesByClass(
         LivingEntity.class, box, e -> this.foe(e) && e.getY() >= yLo && e.getY() <= yHi && segDist(e.getEntityPos(), a, b) <= width + e.getWidth() * 0.5
      );
   }

   private void rake(
      ServerWorld level, double reach, float halfAngle, float yawOffset, float base, double push, @Nullable StatusEffectInstance fx, boolean pierce
   ) {
      DamageSource src = this.getDamageSources().mobAttack(this);
      Vec3d f = Vec3d.fromPolar(0.0F, this.getYaw() + yawOffset);

      for (LivingEntity e : level.getEntitiesByClass(LivingEntity.class, this.getBoundingBox().expand(reach, 4.0, reach), this::foe)) {
         if (!(e.getY() < this.getY() - 3.5) && !(e.getY() > this.getY() + this.getHeight())) {
            double dx = e.getX() - this.getX();
            double dz = e.getZ() - this.getZ();
            double d = Math.sqrt(dx * dx + dz * dz);
            if (!(d > reach + e.getWidth() * 0.5)
               && (!(d > 1.2) || !(halfAngle < 180.0F) || !(Math.toDegrees(Math.acos(MathHelper.clamp((dx * f.x + dz * f.z) / d, -1.0, 1.0))) > halfAngle))) {
               if (pierce) {
                  e.timeUntilRegen = 0;
               }

               if (e.damage(level, src, this.dmg(base))) {
                  Vec3d away = d > 0.001 ? new Vec3d(dx / d, 0.0, dz / d) : f;
                  e.addVelocity(away.x * push, 0.25, away.z * push);
                  e.knockedBack = true;
                  Vfx.impact(level, mid(e), PAL, 0.6);
                  if (fx != null) {
                     e.addStatusEffect(new StatusEffectInstance(fx), this);
                  }
               }
            }
         }
      }
   }

   private void blast(ServerWorld level, Vec3d c, double r, float base, double push, boolean falloff, @Nullable StatusEffectInstance fx) {
      DamageSource src = this.getDamageSources().mobAttack(this);

      for (LivingEntity e : this.foes(level, c, r, 1.5, 3.5)) {
         double k = falloff ? MathHelper.clamp(1.0 - flatDist(e.getEntityPos(), c) / r, 0.0, 1.0) : 1.0;
         if (e.damage(level, src, this.dmg(base) * (float)(falloff ? 0.4 + 0.6 * k : 1.0))) {
            Vec3d away = this.flatDir(e.getEntityPos().subtract(c));
            double s = push * (falloff ? 0.5 + k : 1.0);
            e.addVelocity(away.x * s, 0.3 + 0.3 * k, away.z * s);
            e.knockedBack = true;
         }

         if (fx != null) {
            e.addStatusEffect(new StatusEffectInstance(fx), this);
         }
      }
   }

   private void slam(ServerWorld level, Vec3d c, double r, float base, double push) {
      this.blast(level, c, r, base, push, true, null);
      Vfx.shockwave(level, c, PAL, r);
   }

   private void wave(ServerWorld level, Vec3d c, double maxR, int ticks, float base, double push, Vfx.Palette p) {
      Set<Integer> hit = new HashSet<>();
      Vfx.ring(level, c, p.glow(), maxR, (int)(ticks * 1.3));
      Vfx.flare(level, c.add(0.0, 0.4, 0.0), p.core(), 2.5, 6);

      for (int t = 1; t <= ticks; t++) {
         double r = maxR * t / ticks;
         boolean draw = t % 2 == 0;
         this.schedule(t, () -> {
            if (this.isAlive()) {
               if (draw) {
                  Vfx.ring(level, c, p.core(), r, 4);
               }

               DamageSource src = this.getDamageSources().mobAttack(this);

               for (LivingEntity e : this.foes(level, c, r + 1.0, 1.0, 2.0)) {
                  if (Math.abs(flatDist(e.getEntityPos(), c) - r) < 1.0 && e.isOnGround() && hit.add(e.getId()) && e.damage(level, src, this.dmg(base))) {
                     Vec3d away = this.flatDir(e.getEntityPos().subtract(c));
                     e.addVelocity(away.x * push, 0.4, away.z * push);
                     e.knockedBack = true;
                  }
               }
            }
         });
      }
   }

   static {
      for (int i = 0; i < 4; i++) {
         FORMS[i] = RawAnimation.begin().thenLoop("animation.hollow_warden.form_" + i);
      }
   }

   private static final class Cell {
      final Vec3d center;
      final LivingEntity who;
      final int until;
      int tries;

      Cell(Vec3d center, LivingEntity who, int until) {
         this.center = center;
         this.who = who;
         this.until = until;
      }
   }

   private record Line(Vec3d a, Vec3d b, int born, int until) {
   }

   private record Spot(Vec3d at, double r, boolean major) {
   }

   private record Strike(Vec3d at, int when) {
   }

   private record Zone(Vec3d at, double r, int until) {
   }
}
