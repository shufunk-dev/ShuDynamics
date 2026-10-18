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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.Map.Entry;
import org.jspecify.annotations.Nullable;
import com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;

public class MycelordBoss extends MysticalBoss {
   private static final BossSounds SOUNDS = BossSounds.of("mycelord");
   private static final Vfx.Palette P = Vfx.Palette.SPORE.withDebris(Vfx.Debris.ASH);
   private static final Vfx.Palette ROT = new Vfx.Palette(15917311, 12602088, 2759214).withDebris(Vfx.Debris.ASH);
   private static final int VIOLET = 12602088;
   private static final int TEAL = 4253896;
   private static final float[] IMPALE_YAW = new float[]{-30.0F, 30.0F, -12.0F, 12.0F};
   private static final int[] IMPALE_TICK = new int[]{16, 20, 24, 28};
   private static final double[][] IMPALE_AT = new double[][]{{0.3, 2.8}, {0.3, -2.8}, {-1.5, 1.5}, {-1.5, -1.5}};
   private static final double[] IMPALE_LEN = new double[]{16.0, 16.0, 18.0, 18.0};
   private static final int EMERGE_DRAW = 16;
   private static final int EMERGE_FILL = 21;
   private static final int MAX_PATCHES = 8;
   private static final int MAX_MINES = 10;
   private static final int MAX_STRIPS = 8;
   private static final int MAX_SPORELINGS = 8;
   private static final double WALK_FEET = 1.68;
   private static final double RUN_FEET = 3.88;
   public static final float WIDTH = 3.0F;
   public static final float HEIGHT = 6.0F;
   public static final boolean FIRE_IMMUNE = false;
   private final Map<UUID, int[]> spores = new HashMap<>();
   private int castSeq;
   private final Set<Integer> deadCasts = new HashSet<>();
   private int chainTick = -100;
   private final List<MycelordBoss.Patch> patches = new ArrayList<>();
   private final List<MycelordBoss.Mine> mines = new ArrayList<>();
   private final List<MycelordBoss.Strip> strips = new ArrayList<>();
   private final List<Vec3> rings = new ArrayList<>();
   private double ringRadius = 2.2;
   private @Nullable Vec3 fogAt;
   private List<Vec3> fogRings = List.of();
   private double fogRingRadius = 2.2;
   private int fogUntil;
   private int stormNext = -1;
   private int vortexNext;
   private int wardPulse;
   private Vec3 anchor = Vec3.ZERO;
   private float anchorYaw;
   private final Vec3[] impaleFrom = new Vec3[4];
   private final List<Vec3> blightSpots = new ArrayList<>();
   private @Nullable LivingEntity grabbed;
   private Vec3 grabSpot = Vec3.ZERO;
   private boolean vaulting;
   private Vec3 vaultFrom = Vec3.ZERO;
   private Vec3 vaultTo = Vec3.ZERO;
   private double vaultApex = 3.0;
   private @Nullable Vec3 snareAt;
   private @Nullable Vec3 boltAim;
   private @Nullable Vec3 geyserNext;
   private @Nullable Vec3 crownAt;
   private @Nullable LivingEntity sporefallMark;
   private float torrentHalf = 30.0F;
   private double torrentRange = 12.0;
   private boolean underground;
   private int burrowTicks;
   private @Nullable Vec3 emergeAt;
   private boolean pendingDesperation;
   private int phaseSeen;
   private int lonely;
   private int countTick = -1;
   private int sporelingCount;
   private int lookShown = -1;
   private double gaitSpeed;

   public static Builder attributes() {
      return MysticalBoss.bossAttributes(960.0, 16.0, 0.22, 12.0);
   }

   public MycelordBoss(EntityType<? extends Monster> type, Level level) {
      super(type, level, "mycelord", BossBarColor.BLUE);
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
      return 66;
   }

   @Override
   public float poiseMax() {
      return 150.0F;
   }

   @Override
   public int enrageTicks() {
      return 7200;
   }

   @Override
   protected double runSpeed() {
      return 1.35;
   }

   @Override
   protected double preferredDistance() {
      return 5.0;
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
      return 150;
   }

   @Override
   public Vfx.Palette palette() {
      return this.phase() >= 3 ? ROT : P;
   }

   private Vfx.Palette pal() {
      return this.palette();
   }

   @Override
   protected boolean isHeld(String anim) {
      return "rhizome_dive".equals(anim);
   }

   @Override
   protected boolean immune() {
      return this.underground;
   }

   @Override
   protected void defineActions(List<BossAction> out) {
      out.add(
         BossAction.of("shelf_cleave", 1.4, 0.6)
            .range(0.0, 6.5)
            .cooldown(50)
            .weight(14)
            .tags("melee")
            .lockFacing()
            .advance(0.35, 0.6, 0.2)
            .telegraph(0.0, 0.6)
            .combo("rootgrasp_rake", 0.35F)
            .utility((b, t, c) -> c.targetBlocking ? 0.6F : 1.0F)
            .start((b, level, t, i) -> {
               this.begin();
               this.aim(t, 6, 180.0F);
               this.setAnchor(level, 2.0);
               Vfx.telegraphCone(level, this.anchor, this.anchorYaw, 65.0F, 6.5, 12, this.pal());
            })
            .hit((b, level, t, i) -> {
               for (LivingEntity e : this.sector(level, this.anchor, this.anchorYaw, 65.0F, 6.5)) {
                  this.physical(level, e, 18.0F);
                  this.shove(e, this.position(), 1.1, 0.35);
                  Vfx.impact(level, mid(e), this.pal(), 0.7);
               }

               Vfx.slashArc(level, this.local(0.4, 2.4, 0.0), this.anchorYaw, 0.0F, 8.0F, 6.0, this.pal(), 210.0F);
            })
            .end((b, level, t, i) -> this.guard(i))
      );
      out.add(
         BossAction.of("rootgrasp_rake", 1.25, 0.5)
            .range(0.0, 6.0)
            .cooldown(60)
            .weight(11)
            .tags("melee")
            .lockFacing()
            .advance(0.15, 0.5, 0.15)
            .telegraph(0.0, 0.5)
            .combo("cap_crash_feint", 0.15F)
            .combo("cap_crash", 0.25F)
            .start((b, level, t, i) -> {
               this.begin();
               this.aim(t, 6, 90.0F);
               this.setAnchor(level, 2.05);
               Vfx.telegraphCone(level, this.anchor, this.anchorYaw, 45.0F, 6.0, 10, this.pal());
            })
            .hit((b, level, t, i) -> {
               Vec3 hand = this.local(1.2, 1.8, 1.0);

               for (LivingEntity e : this.sector(level, this.anchor, this.anchorYaw, 45.0F, 6.0)) {
                  boolean guarded = e.getItemBlockingWith() != null;
                  this.physical(level, e, 16.0F);
                  if (!guarded) {
                     this.afflict(e, MobEffects.WITHER, 60, 0);
                     this.infest(level, e, 1);
                  }

                  if (e.position().subtract(this.position()).horizontalDistance() > 2.5) {
                     this.shove(e, this.position(), -0.9, 0.15);
                  }
               }

               Vfx.claw(level, hand, this.anchorYaw, 20.0F, 5.5, this.pal());
               Vfx.debris(level, hand, Vfx.Debris.ASH, this.pal().glow(), 8, 0.2);
            })
            .end((b, level, t, i) -> {
               this.guard(i);
               if (i == -1) {
                  this.chainTick = this.tickCount;
               }
            })
      );
      out.add(
         BossAction.of("cap_crash", 2.0, 0.95)
            .range(0.0, 8.5)
            .cooldown(140)
            .weight(12)
            .tags("melee aoe guard_break")
            .lockFacing()
            .superArmor()
            .telegraph(0.0, 0.95)
            .utility((b, t, c) -> c.targetHealth < 0.35F ? 1.5F : 1.0F)
            .start((b, level, t, i) -> {
               this.begin();
               this.aim(t, 10, 90.0F);
               this.setAnchor(level, 0.5);
               Vfx.telegraphLine(level, this.anchor, dirOf(this.anchorYaw), 8.0, 3.6, 19, this.pal());
            })
            .tick((b, level, t, tick) -> {
               if (tick >= 8 && tick <= 18 && tick % 2 == 0) {
                  Vfx.gather(level, this.local(0.0, 5.2, 0.0), this.pal(), 3.0, 8);
               }
            })
            .hit((b, level, t, i) -> this.capCrash(level, 2.1, 32.0F, 1.4))
            .end((b, level, t, i) -> this.guard(i))
      );
      out.add(
         BossAction.of("cap_crash_feint", 2.6, 1.45)
            .range(0.0, 8.5)
            .cooldown(160)
            .phase(2)
            .weight(6)
            .tags("melee aoe guard_break")
            .lockFacing()
            .superArmor()
            .telegraph(0.0, 1.45)
            .when((b, t) -> this.tickCount - this.chainTick <= 1)
            .start((b, level, t, i) -> {
               this.begin();
               this.aim(t, 12, 90.0F);
               this.setAnchor(level, 0.5);
               Vfx.telegraphLine(level, this.anchor, dirOf(this.anchorYaw), 8.0, 4.4, 29, this.pal());
            })
            .tick((b, level, t, tick) -> {
               if (tick == 19) {
                  Vfx.smoke(level, this.local(2.5, 0.3, 0.0), this.pal().smoke(), 6, 1.2, 0.8);
                  Vfx.shake(level, this.position(), 16.0, 0.25, 4);
               } else if (tick >= 20 && tick <= 28 && tick % 2 == 0) {
                  Vfx.gather(level, this.local(0.0, 5.2, 0.0), this.pal(), 3.0, 8);
               }
            })
            .hit((b, level, t, i) -> this.capCrash(level, 2.5, 36.0F, 1.7))
            .end((b, level, t, i) -> this.guard(i))
      );
      out.add(
         BossAction.of("rootquake_stomp", 1.8, 0.7, 1.2)
            .range(0.0, 6.5)
            .cooldown(100)
            .weight(9)
            .tags("melee aoe")
            .superArmor()
            .telegraph(0.0, 1.2)
            .combo("rot_grasp", 0.25F)
            .utility((b, t, c) -> c.surroundCount >= 2 ? 1.8F : (c.dist < 4.0 ? 1.2F : 0.7F))
            .start((b, level, t, i) -> {
               this.begin();
               Vfx.telegraphCircle(level, this.feet(level), 5.0, 14, this.pal());
            })
            .hit((b, level, t, i) -> {
               Vec3 f = this.feet(level);
               if (i == 0) {
                  BossFx.slam(level, this, f, 5.0, 22.0F, 1.2);
                  Vfx.heavyImpact(level, f, this.pal(), 1.0);
                  Vfx.groundCrack(level, f, 5.0, this.pal());
                  Vfx.telegraphCircle(level, f, this.phase() >= 2 ? 9.0 : 4.0, 10, this.pal());
               } else if (this.phase() >= 2) {
                  this.wave(level, f, 9.0, 12, 20.0F, 1.0);
                  Vfx.heavyImpact(level, f, this.pal(), 1.2);
               } else {
                  BossFx.slam(level, this, f, 4.0, 10.0F, 0.8);
                  Vfx.shockwave(level, f, this.pal(), 4.0);
               }
            })
            .end((b, level, t, i) -> this.guard(i))
      );
      out.add(
         BossAction.of("sporeback_eruption", 1.1, 0.45)
            .range(0.0, 7.0)
            .cooldown(80)
            .weight(10)
            .tags("melee aoe behind")
            .lockFacing()
            .telegraph(0.0, 0.45)
            .combo("shelf_cleave", 0.35F)
            .start((b, level, t, i) -> {
               this.begin();
               this.anchor = this.feet(level);
               this.anchorYaw = this.getYRot() + 180.0F;
               Vfx.telegraphCone(level, this.anchor, this.anchorYaw, 70.0F, 7.0, 9, this.pal());
               Vfx.chargeUp(level, this.sacs(), this.pal(), 9);
            })
            .hit((b, level, t, i) -> {
               Vec3 sacs = this.local(0.2, 4.0, 0.0);

               for (LivingEntity e : this.sector(level, this.anchor, this.anchorYaw, 70.0F, 7.0)) {
                  this.magic(level, e, 20.0F);
                  this.afflict(e, MobEffects.POISON, 60, 1);
                  this.infest(level, e, 2);
                  this.shove(e, this.position(), 1.3, 0.4);
               }

               Vfx.explosion(level, sacs, this.pal(), 2.0);
               Vfx.streaks(level, sacs, this.fwd().scale(-1.0), this.pal(), 12);
               Vfx.smoke(level, sacs, this.pal().smoke(), 12, 2.0, 2.0);
            })
            .end((b, level, t, i) -> this.guard(i))
      );
      out.add(
         BossAction.of("rot_grasp", 2.4, 0.55, 1.6)
            .range(0.0, 6.5)
            .cooldown(220)
            .phase(2)
            .weight(9)
            .tags("melee guard_break")
            .lockFacing()
            .superArmor()
            .advance(0.4, 0.55, 0.25)
            .telegraph(0.0, 1.6)
            .utility((b, t, c) -> c.targetFlying ? 0.0F : (c.dist < 5.0 ? 1.3F : 1.0F))
            .start((b, level, t, i) -> {
               this.begin();
               this.releaseGrab();
               this.aim(t, 8, 90.0F);
               this.setAnchor(level, 1.5);
               Vfx.telegraphCone(level, this.anchor, this.anchorYaw, 30.0F, 6.5, 11, this.pal());
               this.grabSpot = this.groundAt(level, this.feet(level).add(dirOf(this.anchorYaw).scale(3.5)), 6);
               Vfx.telegraphCircle(level, this.grabSpot, 3.5, 32, this.pal());
            })
            .tick((b, level, t, tick) -> {
               LivingEntity g = this.grabbed;
               if (g != null && tick >= 12 && tick <= 31) {
                  if (g.isAlive() && g.level() == this.level() && !(g.distanceToSqr(this) > 144.0)) {
                     double k = Math.min(1.0, (tick - 11) / 13.0);
                     Vec3 hand = this.local(3.6 - 2.0 * k, 0.9 + 3.3 * k, 0.6);
                     g.setDeltaMovement(hand.subtract(g.position()).scale(0.6));
                     g.syncVelocity = true;
                     g.resetFallDistance();
                     if (tick % 10 == 0) {
                        this.afflict(g, MobEffects.WITHER, 40, 1);
                        this.infest(level, g, 1);
                     }

                     if (tick % 3 == 0) {
                        Vfx.sparks(level, hand, this.pal().glow(), 6, 0.2, 0.14);
                     }
                  } else {
                     this.releaseGrab();
                  }
               }
            })
            .hit((b, level, t, i) -> {
               if (i == 0) {
                  this.grab(level);
               } else {
                  LivingEntity g = this.grabbed;
                  Vec3 at = this.grabSpot;
                  if (g != null && g.isAlive()) {
                     g.teleportTo(at.x, at.y + 0.1, at.z);
                     g.setDeltaMovement(Vec3.ZERO);
                     this.physical(level, g, 28.0F);
                     this.afflict(g, MobEffects.SLOWNESS, 40, 2);
                     this.infest(level, g, 2);
                  }

                  for (LivingEntity e : this.column(level, at, 3.5, 1.5, 3.0)) {
                     if (e != g) {
                        this.physical(level, e, 14.0F);
                     }
                  }

                  this.releaseGrab();
                  if (g != null && g.isAlive()) {
                     this.shove(g, this.position(), 0.4, 0.8);
                  }

                  Vfx.heavyImpact(level, at, this.pal(), 1.3);
                  Vfx.groundCrack(level, at, 3.5, this.pal());
               }
            })
            .end((b, level, t, i) -> {
               this.guard(i);
               this.releaseGrab();
            })
      );
      out.add(
         BossAction.of("puffball_vault", 2.4, 0.5, 1.5)
            .range(7.0, 24.0)
            .cooldown(200)
            .weight(9)
            .tags("gap_closer aoe")
            .lockFacing()
            .superArmor()
            .telegraph(0.5, 1.5)
            .utility((b, t, c) -> this.vaultScore(t, c))
            .start((b, level, t, i) -> {
               this.begin();
               this.vaulting = false;
               this.aim(t, 20, 180.0F);
            })
            .tick((b, level, t, tick) -> {
               if (tick < 10) {
                  if (tick % 3 == 0) {
                     Vfx.gather(level, this.feet(level), this.pal(), 2.5, 6);
                  }
               } else {
                  if (tick > 10 && tick <= 30 && this.vaulting) {
                     double s = (tick - 10) / 20.0;
                     Vec3 p = this.vaultFrom.lerp(this.vaultTo, s).add(0.0, this.vaultApex * 4.0 * s * (1.0 - s), 0.0);
                     Vec3 v = p.subtract(this.position());
                     if (v.lengthSqr() > 9.0) {
                        v = v.normalize().scale(3.0);
                     }

                     this.setDeltaMovement(v);
                     if (v.horizontalDistanceSqr() > 0.0025) {
                        this.face((float)(Mth.atan2(v.z, v.x) * 180.0F / (float)Math.PI) - 90.0F);
                     }

                     if (tick % 2 == 0) {
                        Vfx.trail(level, this.local(0.0, 3.0, 0.0), this.pal(), 1.2);
                     }
                  }
               }
            })
            .hit((b, level, t, i) -> {
               if (i == 0) {
                  this.vaultLaunch(level, t);
               } else {
                  this.vaultLand(level);
               }
            })
            .end((b, level, t, i) -> {
               this.guard(i);
               if (this.vaulting || i == -2) {
                  this.setNoGravity(false);
               }

               this.vaulting = false;
            })
      );
      out.add(
         BossAction.of("rhizome_dive", 1.6, 1.1)
            .range(6.0, 32.0)
            .cooldown(320)
            .phase(1)
            .weight(8)
            .tags("mobility gap_closer")
            .lockFacing()
            .superArmor()
            .utility((b, t, c) -> !this.canBurrow() ? 0.0F : (c.targetUnreachable ? 2.0F : 1.0F) * (c.kitingTicks > 60 ? 1.5F : 1.0F))
            .start((b, level, t, i) -> {
               this.begin();
               Vfx.groundCrack(level, this.feet(level), 3.5, this.pal());
            })
            .tick((b, level, t, tick) -> {
               if (tick >= 4 && tick <= 22 && tick % 4 == 0) {
                  Vec3 f = this.feet(level);
                  Vfx.debris(level, f, Vfx.Debris.ASH, this.pal().smoke(), 8, 0.2);
                  Vfx.smoke(level, f, this.pal().smoke(), 4, 1.5, 0.6);
               }

               if (tick == 25 && this.underground) {
                  this.setInvisible(true);
               }
            })
            .hit((b, level, t, i) -> {
               this.underground = true;
               this.burrowTicks = 44 - 8 * Math.max(0, this.phase() - 1);
               this.emergeAt = null;
               Vfx.explosion(level, this.feet(level), this.pal(), 1.4);
            })
            .end((b, level, t, i) -> {
               this.guard(i);
               if (i == -2) {
                  this.surface(level, false);
               }
            })
      );
      out.add(
         BossAction.of("rhizome_emerge", 1.8, 0.25)
            .range(0.0, 64.0)
            .cooldown(0)
            .weight(1)
            .tags("aoe gap_closer")
            .anyTarget()
            .superArmor()
            .when((b, t) -> false)
            .combo("shelf_cleave", 0.4F)
            .combo("rot_grasp", 0.2F)
            .start((b, level, t, i) -> this.begin())
            .hit((b, level, t, i) -> {
               Vec3 f = this.feet(level);

               for (LivingEntity e : this.column(level, f, 4.5, 1.5, 4.0)) {
                  this.physical(level, e, 30.0F);
               }

               BossFx.launch(level, this, f, 4.5, 1.0);
               Vfx.heavyImpact(level, f, this.pal(), 1.6);
               Vfx.pillar(level, f, this.pal(), 9.0, 1.4);
               Vfx.groundCrack(level, f, 4.5, this.pal());
               Vfx.debris(level, f, Vfx.Debris.ASH, this.pal().glow(), 24, 0.4);
               if (this.phase() >= 2) {
                  for (int k = 0; k < 6; k++) {
                     double a = k * Math.PI / 3.0 + this.getRandom().nextDouble() * 0.3;
                     Vec3 s = this.groundAt(level, f.add(Math.cos(a) * 6.0, 0.0, Math.sin(a) * 6.0), 6);
                     Vfx.telegraphCircle(level, s, 1.3, 8, this.pal());
                     this.later(8, () -> {
                        for (LivingEntity e : this.column(level, s, 1.3, 1.0, 3.0)) {
                           this.physical(level, e, 12.0F);
                        }

                        Vfx.pillar(level, s, this.pal(), 3.0, 0.6);
                        Vfx.shards(level, s.add(0.0, 0.5, 0.0), this.pal().glow(), 8, 0.35);
                     });
                  }
               }
            })
            .end((b, level, t, i) -> this.guard(i))
      );
      out.add(
         BossAction.of("rhizome_snare", 1.6, 0.85)
            .range(8.0, 26.0)
            .cooldown(240)
            .phase(1)
            .weight(8)
            .tags("ranged gap_closer punish")
            .telegraph(0.15, 0.85)
            .combo("rot_grasp", 0.35F)
            .combo("shelf_cleave", 0.3F)
            .utility((b, t, c) -> (c.kitingTicks > 60 ? 1.5F : 1.0F) * (c.targetFlying ? 1.5F : 1.0F))
            .start((b, level, t, i) -> {
               this.begin();
               this.snareAt = null;
            })
            .tick((b, level, t, tick) -> {
               if (tick == 3 && t != null) {
                  Vec3 at = this.groundAt(level, this.predict(t, 14.0), 40);
                  this.snareAt = at;
                  Vfx.telegraphCircle(level, at, 2.2, 14, this.pal());
                  Vec3 f = this.feet(level);
                  Vec3 d = at.subtract(f).horizontal();
                  if (d.lengthSqr() > 0.01) {
                     Vfx.telegraphLine(level, f, d.normalize(), d.length(), 1.2, 14, this.pal());
                  }
               }
            })
            .hit((b, level, t, i) -> {
               Vec3 at = this.snareAt;
               if (at != null) {
                  for (LivingEntity e : this.column(level, at, 2.2, 1.0, 8.0)) {
                     this.magic(level, e, 14.0F);
                     this.afflict(e, MobEffects.SLOWNESS, 25, 6);
                     this.infest(level, e, 1);
                     if (e.isFallFlying()) {
                        e.stopFallFlying();
                     }

                     Vfx.beam(level, this.local(0.6, 1.5, 1.5), e.position().add(0.0, 1.0, 0.0), this.pal(), 0.35, 12);

                     for (int k = 1; k <= 8; k++) {
                        this.later(k, () -> {
                           if (e.isAlive() && !(e.position().subtract(this.position()).horizontalDistance() <= 3.5)) {
                              Vec3 v = this.position().subtract(e.position()).horizontal().normalize().scale(0.9);
                              e.setDeltaMovement(v.x, 0.12, v.z);
                              e.syncVelocity = true;
                           }
                        });
                     }
                  }

                  Vfx.pillar(level, at, this.pal(), 3.0, 0.8);
                  Vfx.groundCrack(level, at, 2.2, this.pal());
               }
            })
            .end((b, level, t, i) -> this.guard(i))
      );
      out.add(
         BossAction.of("puffball_barrage", 2.2, 0.7, 1.05, 1.4)
            .range(5.0, 30.0)
            .cooldown(110)
            .weight(11)
            .tags("ranged")
            .utility((b, t, c) -> c.dist > 10.0 ? 1.3F : 1.0F)
            .start((b, level, t, i) -> this.begin())
            .hit((b, level, t, i) -> {
               if (t != null) {
                  int n = (this.phase() == 0 ? 1 : 2) + (this.enraged() ? 1 : 0);
                  Vec3 first = this.groundAt(level, this.predict(t, 20.0), 24);
                  Vec3 from = this.local(3.3, 2.2, 0.15);

                  for (int k = 0; k < n; k++) {
                     Vec3 at = k == 0 ? first : this.groundAt(level, BossFx.around(level, this, first, 2.5, 5.0), 6);
                     this.lob(level, from, at);
                     if (this.phase() >= 2 && k % 2 == 0) {
                        this.later(21, () -> this.addMine(level, at));
                     }
                  }

                  Vfx.flare(level, from, this.pal().glow(), 1.2, 5);
               }
            })
            .end((b, level, t, i) -> this.guard(i))
      );
      out.add(
         BossAction.of("gill_torrent", 2.6, 1.0, 1.3, 1.6, 1.9)
            .range(3.0, 13.0)
            .cooldown(160)
            .weight(10)
            .tags("ranged aoe")
            .lockFacing()
            .telegraph(0.0, 1.0)
            .utility((b, t, c) -> c.dist >= 4.0 && c.dist <= 10.0 ? 1.3F : 1.0F)
            .start((b, level, t, i) -> {
               this.begin();
               this.aim(t, 12, 90.0F);
               boolean bloom = this.phase() >= 3;
               this.torrentHalf = bloom ? 40.0F : 30.0F;
               this.torrentRange = bloom ? 14.0 : 12.0;
               Vfx.telegraphCone(level, this.feet(level), this.getYRot(), this.torrentHalf, this.torrentRange, 20, this.pal());
            })
            .tick((b, level, t, tick) -> {
               if (tick >= 21 && tick <= 39 && t != null) {
                  this.aim(t, 0, this.phase() >= 3 ? 4.5F : 3.0F);
               }
            })
            .hit((b, level, t, i) -> {
               Vec3 f = this.feet(level);
               float yaw = this.getYRot();

               for (LivingEntity e : this.sector(level, f, yaw, this.torrentHalf, this.torrentRange)) {
                  this.magic(level, e, 9.0F);
                  this.afflict(e, MobEffects.NAUSEA, 100, 0);
                  this.afflict(e, MobEffects.POISON, 40, 0);
                  this.infest(level, e, 1);
               }

               Vec3 fw = dirOf(yaw);
               Vec3 maw = this.maw();
               Vfx.streaks(level, maw, fw.add(0.0, -0.25, 0.0), this.pal(), 14);
               Vfx.smoke(level, maw.add(fw.scale(this.torrentRange * 0.5)), this.pal().smoke(), 10, this.torrentRange * 0.35, 1.2);
               Vfx.debris(level, maw.add(fw.scale(3.0)), Vfx.Debris.ASH, this.pal().glow(), 12, 0.5);
               double spreadK = Math.tan(Math.toRadians(this.torrentHalf)) * 0.5;

               for (int k = 1; k <= 6; k++) {
                  double along = this.torrentRange * k / 6.5;
                  Vec3 p = f.add(fw.scale(along)).add(0.0, 1.0 + 2.5 * (1.0 - k / 6.0), 0.0);
                  level.sendParticles(ParticleTypes.WARPED_SPORE, p.x, p.y, p.z, 5, along * spreadK, 0.4, along * spreadK, 0.02);
               }
            })
            .end((b, level, t, i) -> this.guard(i))
      );
      out.add(
         BossAction.of("blinding_sporebolt", 0.9, 0.35)
            .range(6.0, 34.0)
            .cooldown(120)
            .weight(6)
            .tags("ranged punish")
            .combo("puffball_vault", 0.3F)
            .utility((b, t, c) -> c.targetUnreachable ? 1.5F : 1.0F)
            .start((b, level, t, i) -> {
               this.begin();
               Vfx.chargeUp(level, this.maw(), this.pal(), 7);
               this.boltAim = null;
               if (t != null) {
                  Vec3 aimAt = this.predict(t, Math.sqrt(this.distanceToSqr(t)) / 2.2);
                  this.boltAim = aimAt;
                  Vec3 f = this.feet(level);
                  Vec3 d = aimAt.subtract(f).horizontal();
                  if (d.lengthSqr() > 0.01) {
                     Vfx.telegraphLine(level, f, d.normalize(), d.length(), 0.8, 7, this.pal());
                  }
               }
            })
            .hit(
               (b, level, t, i) -> {
                  Vec3 aimAt = this.boltAim;
                  if (aimAt != null) {
                     Vec3 maw = this.maw();
                     Vec3 dir = aimAt.add(0.0, 1.0, 0.0).subtract(maw).normalize();
                     int shots = this.phase() >= 2 ? 3 : 1;

                     for (int k = 0; k < shots; k++) {
                        Vec3 d = k == 0 ? dir : dir.yRot((float)Math.toRadians(k == 1 ? 6.0 : -6.0));
                        BossBolt.shoot(level, this, BossBolt.Style.SPORE, maw, d.scale(2.2), this.dmg(16.0F))
                           .effect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0))
                           .life(30);
                     }

                     Vfx.flare(level, maw, this.pal().glow(), 1.4, 5);
                  }
               }
            )
            .end((b, level, t, i) -> this.guard(i))
      );
      out.add(
         BossAction.of("skyrot_geyser", 1.6, 0.9)
            .range(0.0, 36.0)
            .cooldown(140)
            .weight(7)
            .tags("ranged anti_air")
            .combo("blinding_sporebolt", 0.3F)
            .utility((b, t, c) -> !c.targetFlying && !c.targetAirborne ? (c.targetUnreachable ? 3.0F : 0.35F) : 1.0F)
            .start((b, level, t, i) -> {
               this.begin();
               this.geyserNext = null;
               this.anchor = t != null ? this.groundAt(level, this.predict(t, 18.0), 40) : this.feet(level);
               this.geyserMark(level, this.anchor, 18);
            })
            .tick((b, level, t, tick) -> {
               if (this.phase() >= 2 && t != null) {
                  if (tick == 4) {
                     Vec3 g = this.groundAt(level, this.predict(t, 22.0), 40);
                     this.geyserNext = g;
                     this.geyserMark(level, g, 18);
                     this.later(18, () -> this.geyser(level, g));
                  } else if (tick == 8 && this.geyserNext != null) {
                     Vec3 v = this.ctx().target == t ? this.ctx().targetVelocity.horizontal() : t.getDeltaMovement().horizontal();
                     Vec3 d = v.lengthSqr() > 0.001 ? v.normalize() : this.geyserNext.subtract(this.position()).horizontal();
                     d = d.lengthSqr() > 1.0E-4 ? d.normalize() : this.fwd();
                     Vec3 g = this.groundAt(level, this.geyserNext.add(d.scale(4.0)), 40);
                     this.geyserMark(level, g, 18);
                     this.later(18, () -> this.geyser(level, g));
                  }
               }
            })
            .hit((b, level, t, i) -> this.geyser(level, this.anchor))
            .end((b, level, t, i) -> this.guard(i))
      );
      out.add(
         BossAction.of("hyphal_impale", 2.0, 0.8, 1.0, 1.2, 1.4)
            .range(3.0, 18.0)
            .cooldown(170)
            .phase(2)
            .weight(10)
            .tags("ranged aoe")
            .lockFacing()
            .telegraph(0.0, 1.4)
            .utility((b, t, c) -> c.dist > 6.0 ? 1.3F : 0.8F)
            .start((b, level, t, i) -> {
               this.begin();
               this.aim(t, 12, 90.0F);
               this.anchorYaw = this.getYRot();
               Vec3 f = this.feet(level);
               Vec3 fw = dirOf(this.anchorYaw);
               Vec3 left = dirOf(this.anchorYaw - 90.0F);

               for (int k = 0; k < 4; k++) {
                  Vec3 d = dirOf(this.anchorYaw + IMPALE_YAW[k]);
                  this.impaleFrom[k] = this.groundAt(level, f.add(fw.scale(IMPALE_AT[k][0])).add(left.scale(IMPALE_AT[k][1])), 4);
                  Vfx.telegraphLine(level, this.impaleFrom[k], d, IMPALE_LEN[k], 2.0, IMPALE_TICK[k], this.pal());
               }
            })
            .hit((b, level, t, i) -> {
               Vec3 from = this.impaleFrom[i];
               if (from != null) {
                  float yaw = this.anchorYaw + IMPALE_YAW[i];
                  Vec3 d = dirOf(yaw);
                  double len = IMPALE_LEN[i];

                  for (LivingEntity e : this.rect(level, from, yaw, len, 1.3)) {
                     this.physical(level, e, 20.0F);
                     this.shove(e, this.position(), 0.2, 0.7);
                     this.infest(level, e, 1);
                  }

                  Vfx.groundCrack(level, from, 1.2, this.pal());

                  for (int s = 1; s <= (int)(len / 2.0); s++) {
                     Vfx.pillar(level, this.groundAt(level, from.add(d.scale(s * 2 - 1)), 4), this.pal(), 2.5, 0.5);
                  }

                  Vfx.shards(level, from.add(d.scale(5.0)).add(0.0, 0.6, 0.0), this.pal().glow(), 6, 0.35);
                  Vfx.shards(level, from.add(d.scale(len - 5.0)).add(0.0, 0.6, 0.0), this.pal().glow(), 6, 0.35);
                  Vfx.groundCrack(level, this.groundAt(level, from.add(d.scale(len / 2.0)), 4), 2.5, this.pal());
                  if (this.phase() >= 3) {
                     this.addStrip(from, yaw, len, 60);
                  }
               }
            })
            .end((b, level, t, i) -> this.guard(i))
      );
      out.add(
         BossAction.of("spore_maelstrom", 3.0, 1.0, 1.5, 2.0, 2.5)
            .range(0.0, 16.0)
            .cooldown(260)
            .phase(1)
            .weight(8)
            .tags("aoe")
            .anyTarget()
            .superArmor()
            .telegraph(0.0, 2.5)
            .combo("rootquake_stomp", 0.5F)
            .utility((b, t, c) -> (c.engagedPlayers >= 2 ? 1.4F : 1.0F) * (c.dist > 8.0 ? 1.3F : 1.0F))
            .start((b, level, t, i) -> {
               this.begin();
               Vec3 f = this.feet(level);
               Vfx.vortex(level, f, 16.0, 10.0, this.pal(), 60);
               Vfx.runeCircle(level, f, 5.0, this.pal(), 60, 0.08F);
            })
            .tick((b, level, t, tick) -> {
               if (tick >= 10 && tick <= 56 && tick % 4 == 0) {
                  BossFx.pull(level, this, this.feet(level), 16.0, 0.18 + 0.03 * this.phase());
               }
            })
            .hit((b, level, t, i) -> {
               Vec3 f = this.feet(level);

               for (LivingEntity e : this.column(level, f, 16.0, 2.0, 5.0)) {
                  this.magic(level, e, 6.0F);
                  this.afflict(e, MobEffects.POISON, 40, 0);
                  this.infest(level, e, 1);
               }

               Vfx.shockwave(level, f, this.pal(), 4 + 3 * i);
               if (i == 3) {
                  for (LivingEntity e : this.column(level, f, 5.0, 2.0, 4.0)) {
                     this.magic(level, e, 18.0F);
                     this.afflict(e, MobEffects.SLOWNESS, 30, 1);
                  }
               }
            })
            .end((b, level, t, i) -> this.guard(i))
      );
      out.add(
         BossAction.of("blight_field", 2.2, 1.1)
            .range(0.0, 30.0)
            .cooldown(420)
            .phase(1)
            .weight(7)
            .tags("aoe")
            .anyTarget()
            .superArmor()
            .telegraph(0.0, 1.1)
            .utility((b, t, c) -> this.patches.size() >= 6 ? 0.0F : 1.0F)
            .start((b, level, t, i) -> {
               this.begin();
               this.blightSpots.clear();
               int n = 3 + this.phase();
               if (t != null) {
                  this.blightSpots.add(this.groundAt(level, this.predict(t, 22.0), 24));
               }

               for (int tries = 0; this.blightSpots.size() < n && tries < n * 4; tries++) {
                  Vec3 s = BossFx.around(level, this, this.position(), 5.0, 16.0);
                  if (spaced(s, this.blightSpots, 4.0) && s.subtract(this.position()).horizontalDistance() >= 3.0) {
                     this.blightSpots.add(s);
                  }
               }

               for (Vec3 s : this.blightSpots) {
                  Vfx.telegraphCircle(level, s, 3.0, 22, this.pal());
               }
            })
            .hit((b, level, t, i) -> {
               int life = this.patchLife();

               for (Vec3 s : this.blightSpots) {
                  for (LivingEntity e : this.column(level, s, 3.0, 1.0, 3.0)) {
                     this.magic(level, e, 16.0F);
                     this.infest(level, e, 1);
                  }

                  Vfx.groundCrack(level, s, 3.0, this.pal());
                  Vfx.explosion(level, s, this.pal(), 1.2);
                  this.addPatch(s, 3.0, life, true);
               }

               Vfx.heavyImpact(level, this.local(1.5, 0.0, 0.0), this.pal(), 0.9);
            })
            .end((b, level, t, i) -> this.guard(i))
      );
      out.add(
         BossAction.of("shelfshell_ward", 2.0, 0.9)
            .range(0.0, 40.0)
            .cooldown(600)
            .phase(1)
            .weight(6)
            .tags("defensive")
            .anyTarget()
            .utility((b, t, c) -> this.warded() ? 0.0F : (this.getHealth() / this.getMaxHealth() > 0.9F ? 0.3F : 1.0F))
            .start((b, level, t, i) -> {
               this.begin();
               Vfx.gather(level, this.local(0.0, 3.0, 0.0), this.pal(), 4.0, 16);
            })
            .hit((b, level, t, i) -> {
               this.shield(this.getMaxHealth() * 0.12F, 240);
               Vfx.shieldBubble(level, this.local(0.0, 3.0, 0.0), 4.5, this.pal(), 32);
               this.wardPulse = 240;
            })
            .end((b, level, t, i) -> this.guard(i))
      );
      out.add(
         BossAction.of("sporeling_brood", 2.4, 1.3)
            .range(0.0, 32.0)
            .cooldown(500)
            .weight(7)
            .tags("summon")
            .anyTarget()
            .telegraph(0.6, 1.3)
            .utility((b, t, c) -> this.sporelingsNear() >= 6 ? 0.0F : 1.0F)
            .start((b, level, t, i) -> this.begin())
            .tick((b, level, t, tick) -> {
               if (tick >= 12 && tick <= 26 && tick % 3 == 0) {
                  Vfx.flare(level, this.sacs(), this.pal().glow(), 1.0 + tick * 0.05, 6);
               }
            })
            .hit((b, level, t, i) -> {
               Vfx.explosion(level, this.sacs(), this.pal(), 2.0);
               this.brood(level, 2 + this.phase(), this.phase() >= 3);
               if (this.phase() >= 2) {
                  this.summonShaman(level);
               }
            })
            .end((b, level, t, i) -> this.guard(i))
      );
      out.add(
         BossAction.of("deathcap_nova", 4.0, 2.4)
            .range(0.0, 18.0)
            .cooldown(900)
            .phase(1)
            .weight(8)
            .tags("aoe ultimate")
            .anyTarget()
            .superArmor()
            .telegraph(0.2, 2.4)
            .utility((b, t, c) -> c.engagedPlayers >= 2 ? 1.3F : 1.0F)
            .start((b, level, t, i) -> {
               this.begin();
               this.placeRings(level, 3, 8.0, 13.0, 2.2);

               for (Vec3 r : this.rings) {
                  Vfx.ward(level, r, 2.2, P, 48);
                  Vfx.runeCircle(level, r, 2.2, P, 48, 0.05F, Vfx.Rune.ARCANE);
               }

               Vfx.telegraphCircle(level, this.feet(level), 14.0, 48, ROT);
               Vfx.chargeUp(level, this.local(0.0, 4.6, 0.0), ROT, 48);
            })
            .tick((b, level, t, tick) -> {
               if (tick >= 8 && tick <= 46 && tick % 4 == 0) {
                  Vfx.gather(level, this.local(0.0, 3.5, 0.0), ROT, 14.0 - tick * 0.2, 16);
               }
            })
            .hit((b, level, t, i) -> {
               Vec3 f = this.feet(level);
               Vfx.nova(level, this.local(0.0, 3.0, 0.0), ROT, 14.0);
               Vfx.heavyImpact(level, f, ROT, 2.5);
               Vfx.impactFrame(level, f, 40.0, 12602088, 0.6F);

               for (LivingEntity e : this.column(level, f, 14.0, 2.0, 6.0)) {
                  if (inRing(e, this.rings, this.ringRadius)) {
                     this.infest(level, e, -2);
                  } else {
                     this.physical(level, e, 52.0F);
                     this.afflict(e, MobEffects.WITHER, 100, 1);
                     this.infest(level, e, 3);
                     this.shove(e, f, 1.6, 0.5);
                  }
               }

               for (Vec3 r : this.rings) {
                  Vfx.auraRing(level, r, P, 2.2);
               }

               if (this.phase() >= 3) {
                  List<Vec3> safe = List.copyOf(this.rings);
                  double rr = this.ringRadius;
                  this.later(1, () -> this.startFog(level, f, safe, rr));
               }
            })
            .end((b, level, t, i) -> {
               this.guard(i);
               this.rings.clear();
            })
      );
      out.add(
         BossAction.of("sporefall", 4.2, 1.4, 1.8, 2.2, 2.6, 3.4)
            .range(0.0, 32.0)
            .cooldown(1000)
            .phase(2)
            .weight(8)
            .tags("ranged aoe anti_air ultimate")
            .anyTarget()
            .superArmor()
            .telegraph(0.4, 3.4)
            .start((b, level, t, i) -> {
               this.begin();
               this.sporefallMark = t;
               this.crownAt = null;
               if (t instanceof ServerPlayer sp) {
                  this.markTarget(sp, 90);
               }

               Vfx.chargeUp(level, this.local(0.0, 6.0, 0.0), ROT, 28);
            })
            .tick((b, level, t, tick) -> {
               if (tick >= 8 && tick <= 28 && tick % 4 == 0) {
                  Vfx.streaks(level, this.local(0.0, 6.0, 0.0), new Vec3(0.0, 1.0, 0.0), this.pal(), 10);
               }
            })
            .hit((b, level, t, i) -> {
               if (i < 4) {
                  List<ServerPlayer> players = this.engaged();
                  LivingEntity victim = !players.isEmpty() ? (LivingEntity)players.get(this.getRandom().nextInt(players.size())) : t;

                  for (int k = 0; k < 4; k++) {
                     Vec3 at;
                     if (k < 2 && victim != null) {
                        double a = this.getRandom().nextDouble() * Math.PI * 2.0;
                        double r = this.getRandom().nextDouble() * 1.5;
                        at = this.groundAt(level, this.predict(victim, 20.0).add(Math.cos(a) * r, 0.0, Math.sin(a) * r), 24);
                     } else {
                        at = BossFx.around(level, this, this.position(), 4.0, 18.0);
                     }

                     this.sporeMote(level, at);
                  }

                  if (i == 2) {
                     LivingEntity m = this.sporefallMark != null && this.sporefallMark.isAlive() ? this.sporefallMark : t;
                     Vec3 c = m != null ? this.groundAt(level, this.predict(m, 24.0), 24) : this.feet(level);
                     this.crownAt = c;
                     Vfx.telegraphCircle(level, c, 5.5, 24, ROT);
                     Vfx.chargeUp(level, c.add(0.0, 12.0, 0.0), ROT, 24);
                  }
               } else {
                  Vec3 c = this.crownAt;
                  if (c != null) {
                     BossFx.slam(level, this, c, 5.5, 48.0F, 1.5);

                     for (LivingEntity e : this.column(level, c, 5.5, 1.5, 4.0)) {
                        this.afflict(e, MobEffects.WITHER, 80, 0);
                        this.infest(level, e, 2);
                     }

                     Vfx.heavyImpact(level, c, ROT, 2.2);
                     Vfx.groundCrack(level, c, 5.5, ROT);
                     Vfx.nova(level, c, ROT, 6.0);
                  }
               }
            })
            .end((b, level, t, i) -> {
               this.guard(i);
               this.crownAt = null;
               this.sporefallMark = null;
            })
      );
      out.add(
         BossAction.of("the_last_bloom", 7.0, 1.6, 2.6, 3.6, 4.6, 6.0)
            .range(0.0, 64.0)
            .cooldown(0)
            .phase(3)
            .weight(1)
            .tags("aoe ultimate")
            .anyTarget()
            .superArmor()
            .when((b, t) -> false)
            .telegraph(0.0, 6.0)
            .start((b, level, t, i) -> {
               this.begin();
               this.surface(level, false);
               this.rings.clear();
               int extra = Math.max(0, this.engaged().size() - 1);
               this.shield(this.getMaxHealth() * (0.04F + 0.03F * extra), 130);
               Vec3 f = this.feet(level);
               Vfx.shieldBubble(level, this.local(0.0, 3.0, 0.0), 4.5, ROT, 32);
               Vfx.telegraphCircle(level, f, 24.0, 120, ROT);
               Vfx.vortex(level, f, 20.0, 16.0, ROT, 120);
               Vfx.screen(level, this.position(), 48.0, 0.15, 120, ROT.smoke(), 0.3F);
            })
            .tick((b, level, t, tick) -> {
               if (tick % 30 == 0 && tick < 130 && this.shield() > 0.0F) {
                  Vfx.shieldBubble(level, this.local(0.0, 3.0, 0.0), 4.5, ROT, 32);
               }
            })
            .hit((b, level, t, i) -> {
               Vec3 f = this.feet(level);
               switch (i) {
                  case 0:
                     this.placeRings(level, 3, 10.0, 16.0, 2.5);

                     for (Vec3 r : this.rings) {
                        Vfx.ward(level, r, 2.5, P, 90);
                        Vfx.runeCircle(level, r, 2.5, P, 90, 0.04F);
                     }

                     this.wave(level, f, 20.0, 30, 12.0F, 0.8);
                     break;
                  case 1:
                  case 2:
                     this.wave(level, f, 20.0, 30, 14.0F, 0.8);
                     break;
                  case 3:
                     if (this.shield() <= 0.0F) {
                        this.forceAction(level, "husk_rupture");
                        return;
                     }

                     this.wave(level, f, 20.0, 30, 16.0F, 0.8);

                     for (Vec3 r : this.rings) {
                        Vfx.runeCircle(level, r, 2.5, ROT, 28, 0.12F);
                     }
                     break;
                  default:
                     for (LivingEntity e : this.column(level, f, 24.0, 3.0, 10.0)) {
                        if (inRing(e, this.rings, this.ringRadius)) {
                           this.infest(level, e, -2);
                        } else {
                           this.physical(level, e, 66.0F);
                           this.afflict(e, MobEffects.WITHER, 160, 1);
                           this.afflict(e, MobEffects.DARKNESS, 100, 0);
                           this.infest(level, e, 5);
                        }
                     }

                     Vfx.impactFrame(level, f, 64.0, 12602088, 0.9F);
                     Vfx.heavyImpact(level, f, ROT, 3.0);
                     Vfx.nova(level, this.local(0.0, 3.0, 0.0), ROT, 24.0);
                     Vfx.pillar(level, f, ROT, 30.0, 3.0);
                     this.buff(100, 1.25F, 1.0F);
               }
            })
            .end((b, level, t, i) -> {
               this.guard(i);
               this.rings.clear();
            })
      );
      out.add(
         BossAction.of("husk_rupture", 2.6, 0.4)
            .range(0.0, 64.0)
            .cooldown(0)
            .phase(3)
            .weight(1)
            .tags("aoe")
            .anyTarget()
            .when((b, t) -> false)
            .start((b, level, t, i) -> this.begin())
            .hit((b, level, t, i) -> {
               Vec3 f = this.feet(level);
               BossFx.slam(level, this, f, 5.0, 8.0F, 1.4);
               Vfx.nova(level, this.local(0.0, 3.0, 0.0), ROT, 6.0);
               BossFx.wardShatter(level, this);
               Vfx.debris(level, this.local(0.0, 3.0, 0.0), Vfx.Debris.ASH, ROT.glow(), 30, 0.4);
               this.buff(160, 1.5F, 0.8F);
            })
            .end((b, level, t, i) -> this.guard(i))
      );
   }

   private void capCrash(ServerLevel level, double halfWidth, float base, double power) {
      Vec3 dir = dirOf(this.anchorYaw);

      for (LivingEntity e : this.rect(level, this.anchor, this.anchorYaw, 8.0, halfWidth)) {
         this.guardBreak(level, e, 100);
         this.physical(level, e, base);
         this.afflict(e, MobEffects.NAUSEA, 60, 0);
         this.shove(e, this.position(), 0.8, 0.6);
      }

      Vec3 at = this.groundAt(level, this.anchor.add(dir.scale(1.5)), 6);
      Vfx.heavyImpact(level, at, this.pal(), power);
      Vfx.groundCrack(level, at, 4.0, this.pal());
      Vfx.smoke(level, at, this.pal().smoke(), 10, 2.0, 1.5);
      if (this.phase() >= 1) {
         Vfx.telegraphCircle(level, at, 2.5, 20, this.pal());
         int life = this.hazardLife(80);
         this.schedule(20, () -> this.addPatch(at, 2.5, life, false));
      }

      if (this.phase() >= 3) {
         for (int s = -1; s <= 1; s += 2) {
            float yaw = this.anchorYaw + 25 * s;
            Vec3 d = dirOf(yaw);
            this.later(6, () -> Vfx.telegraphLine(level, at, d, 8.0, 1.4, 6, ROT));
            this.later(12, () -> {
               for (LivingEntity e : this.rect(level, at, yaw, 8.0, 1.0)) {
                  this.physical(level, e, 14.0F);
               }

               Vfx.groundCrack(level, this.groundAt(level, at.add(d.scale(4.0)), 4), 3.0, ROT);
            });
         }
      }
   }

   private void grab(ServerLevel level) {
      Vec3 hand = this.local(3.3, 2.35, 0.1);
      Vec3 f = dirOf(this.anchorYaw);
      LivingEntity best = null;
      double bestOff = Double.MAX_VALUE;

      for (LivingEntity e : this.sector(level, this.anchor, this.anchorYaw, 30.0F, 6.5)) {
         if (e instanceof Player) {
            double dx = e.getX() - this.anchor.x;
            double dz = e.getZ() - this.anchor.z;
            double off = Math.abs(dx * f.z - dz * f.x);
            if (off < bestOff) {
               bestOff = off;
               best = e;
            }
         }
      }

      this.grabbed = best;
      if (best != null) {
         Vfx.claw(level, hand, this.anchorYaw, 10.0F, 3.0, this.pal());
         Vfx.flare(level, hand, this.pal().glow(), 1.5, 6);
      } else {
         Vfx.smoke(level, hand, this.pal().smoke(), 8, 0.8, 1.0);
      }
   }

   private void releaseGrab() {
      if (this.grabbed != null) {
         this.grabbed.resetFallDistance();
         this.grabbed = null;
      }
   }

   private void vaultLaunch(ServerLevel level, @Nullable LivingEntity t) {
      Vec3 from = this.position();
      Vec3 to = from;
      double apex = 3.0;
      if (t != null) {
         Vec3 spot = this.landingSpot(level, t, 20);
         double a = this.vaultApex(level, from, spot);
         if (a > 0.0) {
            to = spot;
            apex = a;
         }
      }

      this.vaultFrom = from;
      this.vaultTo = to;
      this.vaultApex = apex;
      Vfx.telegraphCircle(level, to, 6.0, 20, this.pal());
      Vec3 f = this.feet(level);
      BossFx.slam(level, this, f, 3.5, 10.0F, 1.0);
      Vfx.explosion(level, f, this.pal(), 2.2);
      Vfx.pillar(level, f, this.pal(), 4.0, 1.2);
      this.getNavigation().stop();
      this.setNoGravity(true);
      this.vaulting = true;
   }

   private void vaultLand(ServerLevel level) {
      this.setNoGravity(false);
      this.vaulting = false;
      Vec3 to = this.vaultTo;
      if (this.position().distanceTo(to) <= 2.5 && this.fitsAt(level, to)) {
         this.teleportTo(to.x, to.y, to.z);
      }

      this.setDeltaMovement(Vec3.ZERO);
      Vec3 land = this.position().distanceTo(to) <= 1.0 ? to : this.feet(level);
      BossFx.slam(level, this, land, 6.0, 36.0F, 1.6);
      Vfx.heavyImpact(level, land, this.pal(), 1.8);
      Vfx.groundCrack(level, land, 6.0, this.pal());
      if (this.phase() >= 2) {
         for (int k = 0; k < 6; k++) {
            double a = k * Math.PI / 3.0;
            this.addMine(level, this.groundAt(level, land.add(Math.cos(a) * 5.0, 0.0, Math.sin(a) * 5.0), 6));
         }
      }
   }

   private float vaultScore(@Nullable LivingEntity t, BossContext c) {
      if (t != null && this.level() instanceof ServerLevel level) {
         Vec3 spot = this.landingSpot(level, t, 20);
         if (spot.subtract(this.position()).horizontalDistance() < 3.0 || this.vaultApex(level, this.position(), spot) <= 0.0) {
            return 0.0F;
         } else if (c.targetUnreachable) {
            return 0.2F;
         } else {
            return c.kitingTicks > 40 ? 1.5F : 1.0F;
         }
      } else {
         return 0.0F;
      }
   }

   private Vec3 landingSpot(ServerLevel level, LivingEntity t, int lead) {
      Vec3 from = this.position();
      Vec3 aim = this.predict(t, lead);
      Vec3 d = aim.subtract(from).horizontal();
      double len = Math.min(22.0, d.length());
      if (len < 0.5) {
         return from;
      }

      Vec3 dir = d.normalize();

      for (int k = 0; k < 12; k++) {
         double back = k * 1.5;
         if (back > len) {
            break;
         }

         Vec3 p = from.add(dir.scale(len - back));
         Vec3 g = this.groundAt(level, new Vec3(p.x, aim.y, p.z), 12);
         if (BossFx.safeGround(level, g, 3.0) && this.fitsAt(level, g)) {
            return g;
         }
      }

      return from;
   }

   private double vaultApex(ServerLevel level, Vec3 from, Vec3 to) {
      double rise = Math.max(0.0, to.y - from.y);
      AABB box = this.getBoundingBox().deflate(0.1);

      for (int apex = 7; apex >= 3; apex--) {
         double a = apex + rise;
         boolean clear = true;

         for (int k = 1; k <= 3 && clear; k++) {
            double s = k * 0.25;
            Vec3 p = from.lerp(to, s).add(0.0, a * 4.0 * s * (1.0 - s), 0.0);
            clear = level.noCollision(this, box.move(p.x - this.getX(), p.y - this.getY(), p.z - this.getZ()));
         }

         if (clear) {
            return a;
         }
      }

      return -1.0;
   }

   private void lob(ServerLevel level, Vec3 from, Vec3 at) {
      int n = 20;
      double g = 0.05;
      Vec3 aimAt = at.add(0.0, -0.2, 0.0);
      Vec3 v = aimAt.subtract(from).scale(1.0 / n).add(0.0, g * (n + 1) / 2.0, 0.0);
      Vfx.telegraphCircle(level, at, 2.5, n, this.pal());
      BossBolt.shoot(level, this, BossBolt.Style.SPORE, from, v, this.dmg(14.0F))
         .gravity(g)
         .aoe(2.5)
         .effect(new MobEffectInstance(MobEffects.POISON, 80, 1))
         .life(n + 6);
      this.later(n, () -> {
         for (LivingEntity e : this.column(level, at, 2.5, 1.5, 3.0)) {
            this.infest(level, e, 1);
         }
      });
   }

   private void geyserMark(ServerLevel level, Vec3 g, int ticks) {
      Vfx.telegraphCircle(level, g, 2.5, ticks, this.pal());
      Vfx.beam(level, g, g.add(0.0, 22.0, 0.0), this.pal(), 0.12, ticks);
   }

   private void geyser(ServerLevel level, Vec3 g) {
      for (LivingEntity e : this.column(level, g, 2.5, 1.0, 22.0)) {
         this.physical(level, e, 24.0F);
         if (e.isFallFlying()) {
            e.stopFallFlying();
         }

         Vec3 v = e.getDeltaMovement();
         e.setDeltaMovement(v.x * 0.2, -1.2, v.z * 0.2);
         e.syncVelocity = true;
         this.afflict(e, MobEffects.SLOWNESS, 60, 1);
         this.infest(level, e, 2);
      }

      Vfx.pillar(level, g, this.pal(), 22.0, 2.2);
      Vfx.heavyImpact(level, g, this.pal(), 1.0);
      Vfx.streaks(level, g, new Vec3(0.0, 1.0, 0.0), this.pal(), 14);
   }

   private void sporeMote(ServerLevel level, Vec3 at) {
      Vfx.telegraphCircle(level, at, 3.0, 20, this.pal());

      for (int k = 4; k <= 19; k += 2) {
         double h = 20 - k;
         this.later(k, () -> Vfx.meteorTrail(level, at.add(0.0, h, 0.0), new Vec3(0.0, -1.0, 0.0), this.pal()));
      }

      this.later(20, () -> {
         for (LivingEntity e : this.column(level, at, 3.0, 1.0, 20.0)) {
            this.magic(level, e, 26.0F);
            this.afflict(e, MobEffects.POISON, 60, 1);
            this.infest(level, e, 1);
         }

         Vfx.heavyImpact(level, at, this.pal(), 0.9);
      });
   }

   private void wave(ServerLevel level, Vec3 c, double maxR, int ticks, float base, double push) {
      Set<Integer> hit = new HashSet<>();
      Vfx.ring(level, c, this.pal().glow(), maxR, (int)(ticks * 1.3));
      Vfx.flare(level, c.add(0.0, 0.4, 0.0), this.pal().core(), 2.5, 6);

      for (int k = 1; k <= ticks; k++) {
         double r = maxR * k / ticks;
         int step = k;
         this.later(k, () -> {
            if (step % 2 == 1) {
               Vfx.ring(level, c.add(0.0, 0.15, 0.0), this.pal().core(), r, 4);
            }

            if (step % 3 == 0) {
               for (int j = 0; j < 6; j++) {
                  double a = j * Math.PI / 3.0 + step * 0.3;
                  level.sendParticles(ParticleTypes.WARPED_SPORE, c.x + Math.cos(a) * r, c.y + 0.3, c.z + Math.sin(a) * r, 3, 0.3, 0.1, 0.3, 0.01);
               }
            }

            for (LivingEntity e : this.column(level, c, r + 1.0, 1.0, 3.0)) {
               double d = Math.sqrt(sq(e.getX() - c.x) + sq(e.getZ() - c.z));
               if (Math.abs(d - r) < 1.0 && e.onGround() && hit.add(e.getId())) {
                  this.physical(level, e, base);
                  this.shove(e, c, push, 0.4);
               }
            }
         });
      }
   }

   private void brood(ServerLevel level, int count, boolean fast) {
      EntityType<RealmMob> type = RealmMobs.type("sporeling");
      int n = Math.min(count, 8 - this.sporelingsNear());
      if (type != null && n > 0) {
         BossFx.summonMinions(level, this, type, n, 6.0, null, m -> {
            if (fast) {
               m.addEffect(new MobEffectInstance(MobEffects.SPEED, 600, 0));
            }
         });
         this.countTick = -1;
      }
   }

   private void summonShaman(ServerLevel level) {
      EntityType<RealmMob> type = RealmMobs.type("myconid_shaman");
      if (type != null && this.countNear("myconid_shaman") < 2) {
         BossFx.summonMinions(level, this, type, 1, 6.0, null, m -> {});
      }
   }

   private int sporelingsNear() {
      if (this.countTick != this.tickCount) {
         this.countTick = this.tickCount;
         this.sporelingCount = this.countNear("sporeling");
      }

      return this.sporelingCount;
   }

   private int countNear(String kind) {
      return this.level().getEntitiesOfClass(RealmMob.class, this.getBoundingBox().inflate(24.0), e -> e.isAlive() && kind.equals(e.bossId())).size();
   }

   private void placeRings(ServerLevel level, int count, double minR, double maxR, double radius) {
      this.rings.clear();
      this.ringRadius = radius;
      Vec3 c = this.position();
      double base = this.getRandom().nextDouble() * Math.PI * 2.0;

      for (int k = 0; k < count; k++) {
         Vec3 pick = null;
         Vec3 fallback = null;

         for (int tries = 0; tries < 6 && pick == null; tries++) {
            double a = base + k * Math.PI * 2.0 / count + Math.toRadians(this.getRandom().nextDouble() * 40.0 - 20.0);
            double r = minR + this.getRandom().nextDouble() * (maxR - minR);
            Vec3 at = this.groundAt(level, c.add(Math.cos(a) * r, 0.0, Math.sin(a) * r), 12);
            if (fallback == null) {
               fallback = at;
            }

            if (at.subtract(c).horizontalDistance() >= 6.0 && BossFx.safeGround(level, at, 3.0) && spaced(at, this.rings, 5.0)) {
               pick = at;
            }
         }

         Vec3 at = pick != null ? pick : fallback;
         if (at != null) {
            this.rings.add(at);
         }
      }
   }

   private static boolean inRing(LivingEntity e, List<Vec3> rings, double radius) {
      double rr = (radius + 0.3) * (radius + 0.3);

      for (Vec3 r : rings) {
         if (sq(e.getX() - r.x) + sq(e.getZ() - r.z) <= rr && Math.abs(e.getY() - r.y) <= 2.0) {
            return true;
         }
      }

      return false;
   }

   private void startFog(ServerLevel level, Vec3 at, List<Vec3> safe, double radius) {
      this.fogAt = at;
      this.fogRings = safe;
      this.fogRingRadius = radius;
      this.fogUntil = this.tickCount + 80;
      Vfx.vortex(level, at, 14.0, 3.0, ROT, 80);
      Vfx.smoke(level, at.add(0.0, 1.0, 0.0), ROT.smoke(), 16, 8.0, 3.0);
   }

   private boolean canBurrow() {
      return this.level() instanceof ServerLevel level && this.onGround() && BossFx.safeGround(level, this.position(), 1.0);
   }

   @Override
   protected boolean ownCourse(ServerLevel level, @Nullable LivingEntity target) {
      if (!this.underground) {
         return false;
      }

      BossFx.current(this);
      this.getNavigation().stop();
      if (target == null && this.burrowTicks > 17) {
         this.burrowTicks = 17;
      }

      this.burrowTicks--;
      if (this.burrowTicks == 16) {
         Vec3 at = target != null ? this.groundAt(level, this.predict(target, 21.0), 24) : this.position();
         if (!BossFx.safeGround(level, at, 3.0) || !this.fitsAt(level, at)) {
            at = this.position();
         }

         this.emergeAt = at;
         Vfx.telegraphCircle(level, at, 4.5, 21, this.pal());
      }

      Vec3 goal = this.emergeAt != null ? this.emergeAt : (target != null ? this.predict(target, Math.max(0, this.burrowTicks)) : this.position());
      Vec3 d = goal.subtract(this.position()).horizontal();
      double speed = 0.55 + 0.1 * this.phase();
      if (d.length() > speed) {
         d = d.normalize().scale(speed);
      }

      if (d.lengthSqr() > 1.0E-4) {
         Vec3 g = this.groundAt(level, this.position().add(d), 3);
         if (Math.abs(g.y - this.getY()) <= 2.0 && BossFx.safeGround(level, g, 1.0) && this.fitsAt(level, g)) {
            this.setPos(g.x, g.y, g.z);
         }
      }

      this.setDeltaMovement(0.0, Math.min(0.0, this.getDeltaMovement().y), 0.0);
      if (this.tickCount % 2 == 0) {
         Vec3 at = this.position();
         level.sendParticles(ParticleTypes.MYCELIUM, at.x, at.y + 0.2, at.z, 10, 0.8, 0.1, 0.8, 0.05);
         Vfx.debris(level, at, Vfx.Debris.ASH, this.pal().glow(), 4, 0.15);
      }

      if (this.tickCount % 4 == 0) {
         BossFx.burst(level, this.position().add(0.0, 0.2, 0.0), ParticleTypes.MYCELIUM, 10, 0.8, 0.05);
      }

      if (this.tickCount % 6 == 0) {
         Vfx.shockwave(level, this.position(), this.pal(), 1.6);
      }

      if (this.burrowTicks <= 0) {
         Vec3 at = this.emergeAt != null ? this.emergeAt : this.position();
         if (BossFx.safeGround(level, at, 3.0) && this.fitsAt(level, at)) {
            this.teleportTo(at.x, at.y, at.z);
         }

         this.underground = false;
         this.emergeAt = null;
         this.setInvisible(false);
         this.forceAction(level, "rhizome_emerge");
      }

      return true;
   }

   private void surface(ServerLevel level, boolean emerge) {
      if (this.underground) {
         this.underground = false;
         this.burrowTicks = 0;
         this.emergeAt = null;
         this.setInvisible(false);
         Vec3 f = this.feet(level);
         Vfx.explosion(level, f, this.pal(), 1.4);
         Vfx.debris(level, f, Vfx.Debris.ASH, this.pal().glow(), 12, 0.3);
         if (emerge) {
            this.forceAction(level, "rhizome_emerge");
         }
      }
   }

   @Override
   public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
      return this.underground && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) ? false : super.hurtServer(world, source, amount);
   }

   private void begin() {
      this.castSeq++;
      if (this.deadCasts.size() > 64) {
         int keep = this.castSeq - 16;
         this.deadCasts.removeIf(s -> s < keep);
      }
   }

   private void guard(int index) {
      if (index == -2) {
         this.deadCasts.add(this.castSeq);
      }
   }

   private void later(int delay, Runnable r) {
      int seq = this.castSeq;
      this.schedule(delay, () -> {
         if (this.isAlive() && !this.deadCasts.contains(seq)) {
            r.run();
         }
      });
   }

   private void infest(ServerLevel level, LivingEntity e, int n) {
      if (n != 0 && e instanceof ServerPlayer p && p.isAlive() && !p.isCreative() && !p.isSpectator()) {
         int[] s = this.spores.get(p.getUUID());
         if (s == null) {
            if (n < 0) {
               return;
            }

            s = new int[]{0, this.tickCount};
            this.spores.put(p.getUUID(), s);
         }

         s[0] = Mth.clamp(s[0] + n, 0, 5);
         if (n > 0) {
            s[1] = this.tickCount;
         }

         if (s[0] >= 5) {
            this.spores.remove(p.getUUID());
            this.sporeburst(level, p);
         } else if (s[0] <= 0) {
            this.spores.remove(p.getUUID());
         }
      }
   }

   private void sporeburst(ServerLevel level, ServerPlayer p) {
      Vfx.nova(level, p.position().add(0.0, 1.0, 0.0), this.pal(), 2.5);
      p.hurtServer(level, this.damageSources().indirectMagic(this, this), this.dmg(10.0F));
      this.afflict(p, MobEffects.SLOWNESS, 30, 3);
      this.afflict(p, MobEffects.POISON, 60, 1);
      if (this.phase() >= 2 && this.sporelingsNear() < 8) {
         EntityType<RealmMob> type = RealmMobs.type("sporeling");
         RealmMob m = type == null ? null : (RealmMob)type.create(level, EntitySpawnReason.MOB_SUMMONED);
         if (m != null) {
            m.snapTo(p.getX(), p.getY(), p.getZ(), this.getRandom().nextFloat() * 360.0F, 0.0F);
            m.setTarget(p);
            level.addFreshEntity(m);
            this.countTick = -1;
         }
      }
   }

   private void addPatch(Vec3 at, double r, int life, boolean seeds) {
      if (this.patches.size() >= 8) {
         this.patches.remove(0);
      }

      this.patches.add(new MycelordBoss.Patch(at, r, this.tickCount, this.tickCount + life, seeds));
   }

   private void addMine(ServerLevel level, Vec3 at) {
      if (this.mines.size() >= 10) {
         this.mines.remove(0);
      }

      this.mines.add(new MycelordBoss.Mine(at, this.tickCount));
      Vfx.glint(level, at.add(0.0, 0.4, 0.0), this.pal().glow(), 0.8);
   }

   private void addStrip(Vec3 from, float yaw, double length, int life) {
      if (this.strips.size() >= 8) {
         this.strips.remove(0);
      }

      this.strips.add(new MycelordBoss.Strip(from, yaw, length, this.tickCount, this.tickCount + life));
   }

   private int hazardLife(int base) {
      return this.enraged() ? base * 3 / 2 : base;
   }

   private int patchLife() {
      return this.hazardLife(this.phase() >= 3 ? 320 : 240);
   }

   @Override
   protected void customServerAiStep(ServerLevel world) {
      super.customServerAiStep(world);
      if (!this.isDeadOrDying() && !this.inIntro()) {
         BossFx.current(this);
         if (this.isInvisible() && !this.underground) {
            this.setInvisible(false);
         }

         this.phaseSeen = Math.max(this.phaseSeen, this.phase());
         if (this.pendingDesperation && !this.currentAction().startsWith("phase_") && !this.staggered()) {
            this.lastBloom(world);
         }

         if (this.getTarget() == null) {
            if (++this.lonely == 600) {
               this.clearFight();
               this.surface(world, true);
            }
         } else {
            this.lonely = 0;
         }

         this.tickHazards(world);
      }
   }

   private void tickHazards(ServerLevel level) {
      int now = this.tickCount;
      this.tickPatches(level, now);
      this.tickMines(level, now);
      this.tickStrips(level, now);
      this.tickFog(level, now);
      this.tickWard(level);
      this.tickStorm(level, now);
      if (now % 20 == 0) {
         this.tickSpores(level, now);
         if (!this.underground) {
            this.ambient(level, now);
         }
      }
   }

   private void tickPatches(ServerLevel level, int now) {
      if (!this.patches.isEmpty()) {
         Iterator<MycelordBoss.Patch> it = this.patches.iterator();

         while (it.hasNext()) {
            MycelordBoss.Patch p = it.next();
            if (now >= p.until()) {
               it.remove();
               if (p.seeds() && this.phase() >= 2) {
                  this.addMine(level, p.at());
               }
            } else {
               int age = now - p.born();
               if (age > 0 && age % 20 == 0) {
                  for (LivingEntity e : this.column(level, p.at(), p.r(), 2.0, 2.0)) {
                     this.magic(level, e, 3.0F);
                     this.afflict(e, MobEffects.POISON, 60, 0);
                     this.infest(level, e, 1);
                  }
               }

               if (age % 60 == 0) {
                  Vfx.groundCrack(level, p.at(), p.r(), this.pal());
               }

               if (age % 10 == 5) {
                  Vec3 a = p.at();
                  level.sendParticles(ParticleTypes.WARPED_SPORE, a.x, a.y + 0.3, a.z, 6, p.r() * 0.5, 0.2, p.r() * 0.5, 0.01);
                  Vfx.smoke(level, a, this.pal().smoke(), 3, p.r() * 0.6, 0.6);
               }
            }
         }
      }
   }

   private void tickMines(ServerLevel level, int now) {
      if (!this.mines.isEmpty()) {
         Iterator<MycelordBoss.Mine> it = this.mines.iterator();

         while (it.hasNext()) {
            MycelordBoss.Mine m = it.next();
            if (m.burstAt >= 0) {
               if (now >= m.burstAt) {
                  it.remove();
                  this.burstMine(level, m.at);
               }
            } else {
               int age = now - m.placed;
               if (age < 150 && (age < 20 || now % 2 != 0 || !this.playerNear(m.at, 1.8))) {
                  if (age % 30 == 0) {
                     Vfx.glint(level, m.at.add(0.0, 0.4, 0.0), this.pal().glow(), 0.6);
                     Vfx.wisps(level, m.at, this.pal(), 2, 0.5, 30);
                  }
               } else {
                  m.burstAt = now + 10;
                  Vfx.telegraphCircle(level, m.at, 2.5, 10, this.pal());
               }
            }
         }
      }
   }

   private void burstMine(ServerLevel level, Vec3 at) {
      for (LivingEntity e : this.column(level, at, 2.5, 1.5, 2.5)) {
         this.magic(level, e, 14.0F);
         this.afflict(e, MobEffects.POISON, 60, 0);
         this.infest(level, e, 1);
      }

      Vfx.explosion(level, at, this.pal(), 1.6);
   }

   private boolean playerNear(Vec3 at, double r) {
      for (ServerPlayer p : this.engaged()) {
         if (sq(p.getX() - at.x) + sq(p.getZ() - at.z) <= r * r && Math.abs(p.getY() - at.y) < 2.0) {
            return true;
         }
      }

      return false;
   }

   private void tickStrips(ServerLevel level, int now) {
      if (!this.strips.isEmpty()) {
         Iterator<MycelordBoss.Strip> it = this.strips.iterator();

         while (it.hasNext()) {
            MycelordBoss.Strip s = it.next();
            if (now >= s.until()) {
               it.remove();
            } else {
               int age = now - s.born();
               if (age > 0 && age % 20 == 0) {
                  for (LivingEntity e : this.rect(level, s.from(), s.yaw(), s.length(), 1.3)) {
                     this.afflict(e, MobEffects.POISON, 40, 0);
                     this.infest(level, e, 1);
                  }
               }

               if (age % 10 == 0) {
                  Vec3 d = dirOf(s.yaw());

                  for (int k = 0; k < 3; k++) {
                     Vfx.smoke(level, s.from().add(d.scale(s.length() * (k + 0.5) / 3.0)), this.pal().smoke(), 3, 1.0, 0.7);
                  }
               }
            }
         }
      }
   }

   private void tickFog(ServerLevel level, int now) {
      Vec3 at = this.fogAt;
      if (at != null) {
         if (now >= this.fogUntil) {
            this.fogAt = null;
         } else {
            if ((this.fogUntil - now) % 20 == 0) {
               for (LivingEntity e : this.column(level, at, 14.0, 2.0, 6.0)) {
                  if (!inRing(e, this.fogRings, this.fogRingRadius)) {
                     this.afflict(e, MobEffects.POISON, 40, 0);
                  }
               }
            }
         }
      }
   }

   private void tickWard(ServerLevel level) {
      if (this.wardPulse > 0) {
         this.wardPulse--;
         if (this.shield() <= 0.0F) {
            if (this.wardPulse > 5) {
               Vfx.nova(level, this.local(0.0, 3.0, 0.0), this.pal(), 5.0);
               Vfx.debris(level, this.local(0.0, 3.0, 0.0), Vfx.Debris.ASH, this.pal().glow(), 20, 0.35);
            }

            this.wardPulse = 0;
         } else {
            if (this.wardPulse % 20 == 0) {
               Vec3 f = this.feet(level);

               for (LivingEntity e : this.column(level, f, 4.0, 1.5, 5.0)) {
                  this.magic(level, e, 4.0F);
                  this.infest(level, e, 1);
               }

               Vfx.auraRing(level, f, this.pal(), 4.0);
            }

            if (this.wardPulse % 30 == 0) {
               Vfx.shieldBubble(level, this.local(0.0, 3.0, 0.0), 4.5, this.pal(), 32);
            }
         }
      }
   }

   private void tickStorm(ServerLevel level, int now) {
      if (this.phase() >= 3) {
         if (this.stormNext < 0) {
            this.stormNext = now + 60;
         }

         LivingEntity target = this.getTarget();
         if (target != null && !this.underground && !this.currentAction().startsWith("phase_") && now >= this.stormNext) {
            this.stormNext = now + (this.enraged() ? 60 : 100);
            List<ServerPlayer> players = this.engaged();
            LivingEntity p = players.isEmpty() ? target : (LivingEntity)players.get(this.getRandom().nextInt(players.size()));
            this.stormMote(level, this.groundAt(level, this.predict(p, 24.0), 24));

            for (int k = 0; k < 2; k++) {
               this.stormMote(level, this.groundAt(level, BossFx.around(level, this, p.position(), 2.0, 7.0), 4));
            }

            if (now >= this.vortexNext) {
               this.vortexNext = now + 100;
               Vfx.vortex(level, this.feet(level), 14.0, 16.0, ROT, 100);
            }
         }
      }
   }

   private void stormMote(ServerLevel level, Vec3 at) {
      Vfx.telegraphCircle(level, at, 2.2, 24, ROT);

      for (int k = 8; k <= 22; k += 2) {
         double h = (24 - k) * 0.9;
         this.schedule(k, () -> Vfx.meteorTrail(level, at.add(0.0, h, 0.0), new Vec3(0.0, -0.9, 0.0), ROT));
      }

      this.schedule(24, () -> {
         for (LivingEntity e : this.column(level, at, 2.2, 1.0, 4.0)) {
            this.magic(level, e, 12.0F);
            this.infest(level, e, 1);
         }

         Vfx.heavyImpact(level, at, ROT, 0.6);
      });
   }

   private void tickSpores(ServerLevel level, int now) {
      if (!this.spores.isEmpty()) {
         Iterator<Entry<UUID, int[]>> it = this.spores.entrySet().iterator();

         while (it.hasNext()) {
            Entry<UUID, int[]> en = it.next();
            Player p = level.getPlayerByUUID(en.getKey());
            int[] s = en.getValue();
            if (p != null && p.isAlive() && !(p.distanceToSqr(this) > 4096.0)) {
               if (now - s[1] >= 80) {
                  s[0]--;
               }

               if (s[0] <= 0) {
                  it.remove();
               } else {
                  Vfx.wisps(level, p.position().add(0.0, 1.0, 0.0), P, s[0], 0.7, 22);
               }
            } else {
               it.remove();
            }
         }
      }
   }

   private void ambient(ServerLevel level, int now) {
      Vec3 heart = this.heart();
      switch (Math.min(3, this.phase())) {
         case 0:
            Vfx.wisps(level, heart, P, 2, 0.8, 24);
            break;
         case 1:
            Vfx.wisps(level, heart, P, 3, 0.9, 24);
            if (now % 40 == 0) {
               Vfx.flare(level, heart, P.glow(), 1.3, 5);
               this.schedule(5, () -> Vfx.flare(level, this.heart(), P.glow(), 1.1, 5));
            }
            break;
         case 2:
            Vfx.wisps(level, heart, P, 5, 1.2, 24);
            Vfx.debris(level, this.local(0.0, 5.0, 0.0), Vfx.Debris.ASH, P.glow(), 6, 0.12);
            break;
         default:
            Vfx.wisps(level, this.local(0.0, 4.5, 0.0), ROT, 6, 2.6, 24);
            if (now % 40 == 0) {
               Vfx.auraRing(level, this.feet(level), ROT, 3.0);
            }
      }
   }

   private void clearFight() {
      this.spores.clear();
      this.patches.clear();
      this.mines.clear();
      this.strips.clear();
      this.rings.clear();
      this.fogAt = null;
      this.stormNext = -1;
      this.wardPulse = 0;
      this.deadCasts.add(this.castSeq);
      this.releaseGrab();
   }

   private void lastBloom(ServerLevel level) {
      this.pendingDesperation = false;
      this.surface(level, false);
      this.forceAction(level, "the_last_bloom");
   }

   @Override
   protected void phaseBurst(ServerLevel level, int phase) {
      Vec3 f = this.feet(level);
      if (phase <= 1) {
         BossFx.slam(level, this, f, 7.0, 8.0F, 1.6);
         Vfx.nova(level, this.local(0.0, 3.1, 0.0), P, 9.0);
         Vfx.heavyImpact(level, f, P, 1.5);
         BossFx.knockback(level, this, f, 8.0, 1.4);
         this.brood(level, 3, false);
         int life = this.patchLife();

         for (int k = 0; k < 3; k++) {
            Vec3 s = BossFx.around(level, this, this.position(), 6.0, 14.0);
            Vfx.telegraphCircle(level, s, 3.0, 20, P);
            this.schedule(20, () -> this.addPatch(s, 3.0, life, true));
         }
      } else if (phase == 2) {
         BossFx.slam(level, this, f, 8.0, 10.0F, 1.8);
         Vfx.vortex(level, f, 9.0, 12.0, P, 50);
         Vfx.nova(level, this.local(0.0, 3.1, 0.0), P, 10.0);

         for (int k = 0; k < 4; k++) {
            float yaw = this.getYRot() + 45.0F + 90 * k;
            Vec3 d = dirOf(yaw);
            Vec3 from = f.add(d.scale(1.5));
            Vfx.telegraphLine(level, from, d, 12.0, 1.6, 10, P);
            this.later(10, () -> {
               for (LivingEntity e : this.rect(level, from, yaw, 12.0, 1.1)) {
                  this.physical(level, e, 12.0F);
               }

               for (int s = 1; s <= 4; s++) {
                  Vfx.pillar(level, this.groundAt(level, from.add(d.scale(s * 3 - 1.5)), 4), P, 2.5, 0.5);
               }
            });
         }

         this.brood(level, 2, false);
         this.summonShaman(level);
      } else {
         BossFx.slam(level, this, f, 9.0, 12.0F, 2.0);
         Vfx.impactFrame(level, f, 48.0, 12602088, 0.7F);
         Vfx.heavyImpact(level, f, ROT, 2.5);
         Vfx.nova(level, this.local(0.0, 3.0, 0.0), ROT, 12.0);
         Vfx.pillar(level, f, ROT, 26.0, 2.4);
         this.brood(level, 4, true);
         this.stormNext = this.tickCount + 60;
      }
   }

   @Override
   protected void onPhaseChange(ServerLevel level, int phase) {
      this.begin();
      this.surface(level, false);
      this.releaseGrab();
      Vfx.chargeUp(level, this.local(0.5, 3.2, 0.0), phase >= 3 ? ROT : P, 28);
      if (this.phaseSeen < 1 && phase >= 1) {
         this.setCooldown("deathcap_nova", Math.max(this.cooldownLeft("deathcap_nova"), 240));
      }

      if (this.phaseSeen < 2 && phase >= 2) {
         this.setCooldown("sporefall", Math.max(this.cooldownLeft("sporefall"), 300));
      }

      this.phaseSeen = phase;
   }

   @Override
   protected void onStagger(ServerLevel level) {
      this.releaseGrab();
      this.setNoGravity(false);
      this.vaulting = false;
      this.surface(level, false);
      Vfx.debris(level, this.local(0.0, 3.0, 0.0), Vfx.Debris.ASH, this.pal().smoke(), 24, 0.35);
      Vfx.smoke(level, this.local(0.0, 4.0, 0.0), this.pal().smoke(), 10, 2.0, 1.2);

      for (ServerPlayer p : this.engaged()) {
         this.infest(level, p, -1);
      }
   }

   @Override
   protected void onEnrage(ServerLevel level) {
      Vec3 f = this.feet(level);
      Vfx.vortex(level, f, 12.0, 14.0, ROT, 100);
      Vfx.auraRing(level, f, ROT, 5.0);
   }

   @Override
   protected void desperation(ServerLevel level) {
      if (!this.currentAction().startsWith("phase_") && !this.staggered()) {
         this.lastBloom(level);
      } else {
         this.pendingDesperation = true;
      }
   }

   @Override
   protected void onLand(ServerLevel level) {
      Vec3 f = this.feet(level);
      Vfx.heavyImpact(level, f, this.pal(), 0.8);
      Vfx.debris(level, f, Vfx.Debris.ASH, this.pal().glow(), 12, 0.3);
   }

   @Override
   protected void onIntroTick(ServerLevel level, int left) {
      Vec3 f = this.feet(level);
      double t = (this.introTicks() - left) / 20.0;
      if (left == this.introTicks() - 1) {
         Vfx.runeCircle(level, f, 3.0, P, 140, 0.03F);
         Vfx.wisps(level, f.add(0.0, 1.0, 0.0), P, 8, 1.8, 60);
      }

      if (t < 1.5 && left % 6 == 0) {
         Vfx.gather(level, f.add(0.0, 0.5, 0.0), P, 3.0, 10);
         Vfx.flare(level, f.add(0.0, 0.5, 0.0), P.glow(), 0.8 + t * 0.3, 6);
      }

      if (left == 110) {
         Vfx.explosion(level, f.add(0.0, 1.0, 0.0), P, 1.5);
         Vfx.pillar(level, f, P, 6.0, 1.0);
         Vfx.groundCrack(level, f, 4.0, P);
      }

      if (t >= 1.5 && t < 3.5 && left % 6 == 0) {
         Vfx.debris(level, f.add(0.0, 2.0, 0.0), Vfx.Debris.ASH, P.smoke(), 6, 0.2);
         Vfx.smoke(level, f.add(0.0, 1.0, 0.0), P.smoke(), 4, 1.5, 1.0);
      }

      if (left == 70) {
         Vfx.nova(level, this.local(0.0, 5.5, 0.0), P, 6.0);
         Vfx.debris(level, this.local(0.0, 5.0, 0.0), Vfx.Debris.ASH, P.glow(), 30, 0.4);
      }

      if (left == 54 || left == 46) {
         Vfx.groundCrack(level, this.local(0.0, 0.0, left == 54 ? 0.8 : -0.8), 2.5, P);
         Vfx.shake(level, this.position(), 24.0, 0.3, 6);
      }

      if (left == 28) {
         Vfx.chargeUp(level, this.local(0.5, 3.2, 0.0), P, 20);
      }

      if (left == 8) {
         Vfx.radialFlash(level, this.position(), 40.0, 4253896, 0.4F);
         Vfx.wisps(level, this.local(0.0, 4.0, 0.0), P, 12, 3.5, 60);
      }
   }

   @Override
   protected void onDeathTick(ServerLevel level, int tick) {
      Vec3 f = this.feet(level);
      Vec3 heart = this.heart();
      if (tick <= 12 && tick % 4 == 0) {
         Vfx.flare(level, heart, P.glow(), 1.5, 6);
         Vfx.sparks(level, heart, P.core(), 8, 0.25, 0.15);
      }

      if (tick == 28) {
         Vfx.heavyImpact(level, f.add(this.leftDir()), P, 0.8);
         Vfx.groundCrack(level, f, 3.0, P);
      }

      if (tick >= 40 && tick <= 64 && tick % 6 == 4) {
         Vfx.flare(level, this.local(0.95, 1.95, 0.0), P.glow(), Math.max(0.4, 1.4 - (tick - 40) * 0.03), 6);
      }

      if (tick == 68) {
         Vfx.nova(level, this.local(0.0, 5.0, 0.0), P, 10.0);
         Vfx.vortex(level, f, 8.0, 10.0, P, 60);
         Vfx.debris(level, this.local(0.0, 4.0, 0.0), Vfx.Debris.ASH, P.glow(), 30, 0.4);
         Vfx.smoke(level, this.local(0.0, 3.0, 0.0), P.smoke(), 20, 4.0, 3.0);
      }

      if (tick >= 72 && tick <= 120 && tick % 5 == 2) {
         Vfx.smoke(level, this.local(0.0, 2.0, 0.0), P.smoke(), 4, 2.0, 1.5);
         Vfx.debris(level, this.local(0.0, 3.0, 0.0), Vfx.Debris.ASH, P.smoke(), 4, 0.1);
         if (tick % 10 == 2) {
            Vfx.wisps(level, this.local(0.0, 1.5, 0.0), P, 3, 2.2, 40);
         }
      }

      if (tick == 120) {
         Vfx.groundCrack(level, f, 5.0, P);
      }

      if (tick == 145) {
         Vfx.runeCircle(level, f, 5.0, P, 200, 0.01F);
         Vfx.wisps(level, f, P, 12, 5.0, 200);
      }
   }

   @Override
   public void die(DamageSource damageSource) {
      super.die(damageSource);
      if (this.level() instanceof ServerLevel) {
         this.clearFight();
         this.underground = false;
         this.vaulting = false;
         this.setInvisible(false);
         this.setNoGravity(false);
      }
   }

   private static Vec3 dirOf(float yaw) {
      return Vec3.directionFromRotation(0.0F, yaw);
   }

   private Vec3 fwd() {
      return dirOf(this.getYRot());
   }

   private Vec3 leftDir() {
      return dirOf(this.getYRot() - 90.0F);
   }

   private Vec3 local(double forward, double up, double left) {
      return this.position().add(this.fwd().scale(forward)).add(this.leftDir().scale(left)).add(0.0, up, 0.0);
   }

   private Vec3 maw() {
      return this.local(1.4, 3.3, 0.0);
   }

   private Vec3 heart() {
      return this.local(0.55, 3.15, 0.0);
   }

   private Vec3 sacs() {
      return this.local(-0.7, 4.0, 0.0);
   }

   private static Vec3 mid(LivingEntity e) {
      return e.position().add(0.0, e.getBbHeight() * 0.5, 0.0);
   }

   private static double sq(double v) {
      return v * v;
   }

   private Vec3 feet(ServerLevel level) {
      return this.onGround() ? this.position() : this.groundAt(level, this.position(), 24);
   }

   private Vec3 groundAt(ServerLevel level, Vec3 p, int depth) {
      MutableBlockPos m = BlockPos.containing(p.x, p.y + 0.2, p.z).mutable();

      for (int i = 0; i < 4 && solid(level, m); i++) {
         m.move(0, 1, 0);
      }

      if (solid(level, m)) {
         return p;
      }

      for (int i = 0; i < depth; i++) {
         m.move(0, -1, 0);
         BlockState s = level.getBlockState(m);
         VoxelShape shape = s.getCollisionShape(level, m);
         if (!shape.isEmpty()) {
            return new Vec3(p.x, m.getY() + shape.max(Axis.Y), p.z);
         }

         if (!s.getFluidState().isEmpty()) {
            return new Vec3(p.x, m.getY() + 1, p.z);
         }
      }

      return p;
   }

   private static boolean solid(ServerLevel level, BlockPos p) {
      BlockState s = level.getBlockState(p);
      return !s.getCollisionShape(level, p).isEmpty() || !s.getFluidState().isEmpty();
   }

   private boolean fitsAt(ServerLevel level, Vec3 at) {
      return level.noCollision(this, this.getBoundingBox().move(at.x - this.getX(), at.y - this.getY(), at.z - this.getZ()).deflate(0.05));
   }

   private static boolean spaced(Vec3 at, List<Vec3> others, double min) {
      for (Vec3 o : others) {
         if (sq(o.x - at.x) + sq(o.z - at.z) < min * min) {
            return false;
         }
      }

      return true;
   }

   private void aim(@Nullable LivingEntity t, int lead, float maxTurn) {
      if (t != null) {
         Vec3 p = lead > 0 ? this.predict(t, lead) : t.position();
         double dx = p.x - this.getX();
         double dz = p.z - this.getZ();
         if (!(dx * dx + dz * dz < 1.0E-4)) {
            float want = (float)(Mth.atan2(dz, dx) * 180.0F / (float)Math.PI) - 90.0F;
            this.face(Mth.approachDegrees(this.getYRot(), want, maxTurn));
         }
      }
   }

   private void face(float yaw) {
      this.setYRot(yaw);
      this.setYBodyRot(yaw);
      this.setYHeadRot(yaw);
   }

   private void setAnchor(ServerLevel level, double forward) {
      this.anchorYaw = this.getYRot();
      this.anchor = this.feet(level).add(dirOf(this.anchorYaw).scale(forward));
   }

   private List<LivingEntity> inBox(ServerLevel level, AABB box) {
      return level.getEntitiesOfClass(
         LivingEntity.class,
         box,
         e -> e != this && e.isAlive() && !(e instanceof MysticalBoss) && !(e instanceof Player p && (p.isCreative() || p.isSpectator()))
      );
   }

   private List<LivingEntity> column(ServerLevel level, Vec3 c, double r, double below, double above) {
      List<LivingEntity> out = new ArrayList<>();
      double rr = sq(r + 0.3);

      for (LivingEntity e : this.inBox(level, new AABB(c.x - r - 1.0, c.y - below, c.z - r - 1.0, c.x + r + 1.0, c.y + above, c.z + r + 1.0))) {
         if (sq(e.getX() - c.x) + sq(e.getZ() - c.z) <= rr) {
            out.add(e);
         }
      }

      return out;
   }

   private List<LivingEntity> sector(ServerLevel level, Vec3 c, float yaw, float halfAngle, double range) {
      Vec3 f = dirOf(yaw);
      double cos = Math.cos(Math.toRadians(halfAngle));
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : this.inBox(level, new AABB(c.x - range - 1.0, c.y - 1.5, c.z - range - 1.0, c.x + range + 1.0, c.y + 4.5, c.z + range + 1.0))) {
         double dx = e.getX() - c.x;
         double dz = e.getZ() - c.z;
         double d = Math.sqrt(dx * dx + dz * dz);
         if (!(d > range + 0.3) && (d < 1.2 || (dx * f.x + dz * f.z) / d >= cos)) {
            out.add(e);
         }
      }

      return out;
   }

   private List<LivingEntity> rect(ServerLevel level, Vec3 from, float yaw, double length, double halfWidth) {
      Vec3 f = dirOf(yaw);
      Vec3 to = from.add(f.scale(length));
      AABB box = new AABB(Math.min(from.x, to.x), from.y - 2.0, Math.min(from.z, to.z), Math.max(from.x, to.x), from.y + 4.0, Math.max(from.z, to.z))
         .inflate(halfWidth + 0.5, 0.0, halfWidth + 0.5);
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : this.inBox(level, box)) {
         double dx = e.getX() - from.x;
         double dz = e.getZ() - from.z;
         double along = dx * f.x + dz * f.z;
         double side = Math.abs(dx * f.z - dz * f.x);
         if (along >= -0.5 && along <= length + 0.3 && side <= halfWidth) {
            out.add(e);
         }
      }

      return out;
   }

   private boolean physical(ServerLevel level, LivingEntity e, float base) {
      return e.hurtServer(level, this.damageSources().mobAttack(this), this.dmg(base));
   }

   private boolean magic(ServerLevel level, LivingEntity e, float base) {
      return e.hurtServer(level, this.damageSources().indirectMagic(this, this), this.dmg(base));
   }

   private void afflict(LivingEntity e, Holder<MobEffect> effect, int ticks, int amplifier) {
      e.addEffect(new MobEffectInstance(effect, ticks, amplifier), this);
   }

   private void shove(LivingEntity e, Vec3 from, double h, double up) {
      Vec3 away = new Vec3(e.getX() - from.x, 0.0, e.getZ() - from.z);
      away = away.lengthSqr() < 1.0E-4 ? this.fwd() : away.normalize();
      e.push(away.x * h, up, away.z * h);
      e.syncVelocity = true;
   }

   private void guardBreak(ServerLevel level, LivingEntity e, int ticks) {
      if (e instanceof Player p) {
         ItemStack stack = p.getItemBlockingWith();
         if (stack != null) {
            p.getCooldowns().addCooldown(stack, ticks);
            p.stopUsingItem();
            Vec3 at = p.position().add(0.0, p.getBbHeight() * 0.6, 0.0).add(p.getLookAngle().scale(0.5));
            Vfx.shards(level, at, 16777215, 10, 0.35);
            Vfx.glint(level, at, 16777215, 1.2);
         }
      }
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
      super.registerControllers(controllers);
      RawAnimation[] looks = new RawAnimation[4];

      for (int n = 0; n < 4; n++) {
         looks[n] = this.loopAnim("look_" + n);
      }

      RawAnimation walk = this.loopAnim("walk");
      RawAnimation run = this.loopAnim("run");
      controllers.add(new AnimationController("look", 0, test -> {
         AnimationController<MysticalBoss> main = (AnimationController<MysticalBoss>)test.manager().getAnimationControllers().get("main");
         if (main != null) {
            RawAnimation cur = main.getCurrentRawAnimation();
            main.setAnimationSpeed(walk.equals(cur) ? this.gaitRate(1.68) : (run.equals(cur) ? this.gaitRate(3.88) : 1.0));
         }

         boolean dying = this.isDeadOrDying();
         int want = dying ? Math.min(2, Mth.clamp(this.phase(), 0, 3)) : Mth.clamp(this.phase(), 0, 3);
         if (want != this.lookShown) {
            test.controller().setTransitionTicks(this.lookShown < 0 ? 0 : (dying ? 30 : 28));
            this.lookShown = want;
         }

         return test.setAndContinue(looks[want]);
      }));
   }

   private double gaitRate(double feet) {
      return Mth.clamp(this.gaitSpeed / feet, 0.75, 2.0);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level().isClientSide()) {
         this.gaitSpeed = this.gaitSpeed + (20.0 * Mth.length(this.getX() - this.xo, this.getZ() - this.zo) - this.gaitSpeed) * 0.4;
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

   private static final class Mine {
      final Vec3 at;
      final int placed;
      int burstAt = -1;

      Mine(Vec3 at, int placed) {
         this.at = at;
         this.placed = placed;
      }
   }

   private record Patch(Vec3 at, double r, int born, int until, boolean seeds) {
   }

   private record Strip(Vec3 from, float yaw, double length, int born, int until) {
   }
}
