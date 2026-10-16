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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
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
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;
import software.bernie.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

public class AstraelBoss extends MysticalBoss {
   private static final BossSounds SOUNDS = BossSounds.of("astrael");
   private static final Vfx.Palette S = Vfx.Palette.STAR;
   private static final Vfx.Palette HOLY = Vfx.Palette.HOLY;
   private static final Vfx.Palette VOID = Vfx.Palette.VOID;
   private static final int WHITE = 16777215;
   private static final int STAR_WHITE = 16774870;
   private static final int WHITE_GOLD = 15657182;
   private static final int GOLD = 14595156;
   private static final int GLOW_GOLD = 16767088;
   private static final int FEATHER = 16776176;
   private static final String[] ULTIMATES = new String[]{"supernova", "singularity", "last_light"};
   private static final TrackedData<Integer> LOOK = DataTracker.registerData(AstraelBoss.class, TrackedDataHandlerRegistry.INTEGER);
   private static final RawAnimation[] LOOKS = new RawAnimation[5];
   public static final float WIDTH = 1.2F;
   public static final float HEIGHT = 3.6F;
   public static final boolean FIRE_IMMUNE = false;
   private final List<Vec3d> nodes = new ArrayList<>();
   private int nodeTimer = 100;
   private int starTimer = 80;
   private int healerMarkReady;
   private int minionCache;
   private int minionCheckedAt = -1000;
   private int aegisUntil = -1;
   private int reflectReady;
   private @Nullable Vec3d aegisNovaAt;
   private boolean lightWard;
   private boolean pendingLight;
   private Vec3d lightCenter = Vec3d.ZERO;
   private final List<Vec3d> sanctuaries = new ArrayList<>();
   private Vec3d lanceFrom = Vec3d.ZERO;
   private @Nullable Vec3d cleaveAt;
   private @Nullable Vec3d plungeAt;
   private @Nullable Vec3d diveAt;
   private boolean diveAir;
   private final Vec3d[] volleyAims = new Vec3d[3];
   private Vec3d volleyLead = Vec3d.ZERO;
   private @Nullable Vec3d meteorAt;
   private int meteorTravel;
   private final List<AstraelBoss.Sentence> sentences = new ArrayList<>();
   private @Nullable Vec3d well;
   private @Nullable Vec3d well2;
   private final List<Vec3d> conStars = new ArrayList<>();
   private final List<Vec3d> choirSpots = new ArrayList<>();
   private final List<Vec3d> shelters = new ArrayList<>();
   private Vec3d singAt = Vec3d.ZERO;
   private float singTheta;
   private Vec3d radianceAt = Vec3d.ZERO;
   private Vec3d fadeAt = Vec3d.ZERO;
   private double deathFloor;
   private static final double TUG = 0.36;
   private final Map<LivingEntity, Vec3d> pulls = new HashMap<>();
   private final Set<LivingEntity> pulledNow = new HashSet<>();

   public static Builder attributes() {
      return MysticalBoss.bossAttributes(960.0, 14.0, 0.34, 14.0);
   }

   public AstraelBoss(EntityType<? extends HostileEntity> type, World level) {
      super(type, level, "astrael", Color.WHITE);
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
      return 78;
   }

   @Override
   public float poiseMax() {
      return 105.0F;
   }

   @Override
   public int enrageTicks() {
      return 7200;
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
      return 1.8;
   }

   @Override
   protected double preferredDistance() {
      return 6.5;
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
   protected void initDataTracker(net.minecraft.entity.data.DataTracker.Builder builder) {
      super.initDataTracker(builder);
      builder.add(LOOK, 0);
   }

   private int look() {
      return (Integer)this.dataTracker.get(LOOK);
   }

   private void setLook(int n) {
      this.dataTracker.set(LOOK, MathHelper.clamp(n, 0, 4));
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
      super.registerControllers(controllers);
      controllers.add(new AnimationController("look", 0, test -> {
         test.controller().setTransitionTicks(test.controller().getCurrentRawAnimation() != null ? 6 : 0);
         return test.setAndContinue(LOOKS[MathHelper.clamp(this.look(), 0, 4)]);
      }));
   }

   @Override
   protected void writeCustomData(WriteView view) {
      super.writeCustomData(view);
      view.putInt("AstraelLook", this.look());
   }

   @Override
   protected void readCustomData(ReadView view) {
      super.readCustomData(view);
      this.setLook(Math.min(3, view.getInt("AstraelLook", 0)));
      if (this.phase() >= 2) {
         this.bar.setDarkenSky(true);
      }

      if (this.phase() >= 3) {
         this.bar.setThickenFog(true);
      }
   }

   @Override
   protected void defineActions(List<BossAction> out) {
      out.add(this.dawnLance());
      out.add(this.crescentSweep());
      out.add(this.zenithCleave());
      out.add(this.zenithFeint());
      out.add(this.aegisBreaker());
      out.add(this.orbitRend());
      out.add(this.meridianDance());
      out.add(this.cometDive());
      out.add(this.starstep());
      out.add(this.astralVolley());
      out.add(this.haloBlades());
      out.add(this.meteorLance());
      out.add(this.judgment());
      out.add(this.heavensTether());
      out.add(this.starfall());
      out.add(this.gravityWell());
      out.add(this.constellation());
      out.add(this.radiance());
      out.add(this.aegisOfStars());
      out.add(this.choirOfStars());
      out.add(this.supernova());
      out.add(this.singularity());
      out.add(this.lastLight());
      out.add(this.fadingLight());
   }

   private BossAction dawnLance() {
      return BossAction.of("dawn_lance", 1.2, 0.55)
         .range(0.0, 10.0)
         .cooldown(50)
         .weight(14)
         .tags("melee gap_closer")
         .lockFacing()
         .telegraph(0.3, 0.55)
         .advance(0.3, 0.55, 0.75)
         .combo("crescent_sweep", 0.45F)
         .combo("orbit_rend", 0.15F)
         .start((b, level, t, i) -> {
            if (t != null) {
               this.faceTarget(t, 180.0F);
            }
         })
         .tick((b, level, t, tick) -> {
            if (tick <= 6) {
               this.sink(level, t, 0.3);
            }

            if (tick == 6) {
               if (t != null) {
                  this.turnTo(this.predict(t, 5.0));
               }

               Vec3d f = this.facing();
               this.setVelocity(f.x * 0.75, this.getVelocity().y, f.z * 0.75);
               this.lanceFrom = this.getEntityPos();
               Vfx.telegraphLine(level, floor(level, this.getEntityPos()), f, 10.0, 1.6, 5, S);
            }
         })
         .hit((b, level, t, i) -> {
            Vec3d f = this.facing();
            this.spearLine(level, this.lanceFrom.add(0.0, 1.8, 0.0), this.mid().add(f.multiply(5.5)), 1.2, 18.0F, 1.0);
            Vec3d tip = this.tip();
            Vfx.beam(level, tip, tip.add(f.multiply(5.5)), S, 0.35, 4);
            Vfx.streaks(level, this.mid(), f, S, 12);
            Vfx.glint(level, tip.add(f.multiply(5.5)), 16777215, 1.6);
         });
   }

   private BossAction crescentSweep() {
      return BossAction.of("crescent_sweep", 1.3, 0.6)
         .range(0.0, 5.5)
         .cooldown(60)
         .weight(12)
         .tags("melee")
         .telegraph(0.25, 0.6)
         .advance(0.35, 0.6, 0.25)
         .combo("zenith_cleave", 0.35F)
         .combo("zenith_feint", 0.25F)
         .tick((b, level, t, tick) -> {
            if (tick <= 12) {
               this.sink(level, t, 0.3);
            }

            if (tick == 5) {
               Vfx.telegraphCone(level, floor(level, this.getEntityPos()), this.getYaw(), 105.0F, 5.2, 7, S);
            }
         })
         .hit((b, level, t, i) -> {
            this.spearArc(level, 5.2, 105.0F, 16.0F, 1.2, this.right(), null, false);
            Vfx.slashArc(level, this.chest(), this.getYaw(), 0.0F, 0.0F, 5.2, S, -210.0F);
            Vfx.debris(level, this.chest(), Vfx.Debris.FEATHER, 16776176, 6, 0.3);
         });
   }

   private BossAction zenithCleave() {
      return BossAction.of("zenith_cleave", 1.6, 0.8)
         .range(0.0, 6.0)
         .cooldown(110)
         .weight(9)
         .tags("melee aoe guard_break")
         .telegraph(0.3, 0.8)
         .advance(0.5, 0.75, 0.2)
         .tick((b, level, t, tick) -> {
            if (tick < 9) {
               this.rise(0.08);
            } else if (tick <= 16) {
               this.sink(level, t, 0.35);
            }

            if (tick == 6) {
               this.cleaveAt = floor(level, this.getEntityPos().add(this.facing().multiply(3.0)));
               Vfx.telegraphCircle(level, this.cleaveAt, 2.5, 10, S);
            }
         })
         .hit((b, level, t, i) -> this.cleave(level, this.cleaveAt, 26.0F))
         .end((b, level, t, i) -> this.cleaveAt = null);
   }

   private BossAction zenithFeint() {
      return BossAction.of("zenith_feint", 2.0, 1.45)
         .range(0.0, 6.5)
         .cooldown(140)
         .weight(4)
         .phase(1)
         .tags("melee aoe guard_break")
         .telegraph(0.95, 1.45)
         .advance(1.2, 1.4, 0.25)
         .utility((b, t, c) -> b.phase() >= 2 ? 1.0F : 0.0F)
         .tick((b, level, t, tick) -> {
            if (tick < 9) {
               this.rise(0.08);
            } else if (tick >= 20 && tick <= 29) {
               this.sink(level, t, 0.35);
            }

            if (tick == 6) {
               Vfx.telegraphCircle(level, floor(level, this.getEntityPos().add(this.facing().multiply(3.0))), 2.5, 10, S);
            } else if (tick == 16) {
               Vfx.glint(level, this.tip(), 16777215, 1.8);
               Vfx.flare(level, this.tip(), 16767088, 1.6, 6);
            } else if (tick == 19) {
               this.cleaveAt = floor(level, this.getEntityPos().add(this.facing().multiply(3.0)));
               Vfx.telegraphCircle(level, this.cleaveAt, 2.5, 10, S);
            }
         })
         .hit((b, level, t, i) -> this.cleave(level, this.cleaveAt, 28.0F))
         .end((b, level, t, i) -> this.cleaveAt = null);
   }

   private BossAction aegisBreaker() {
      return BossAction.of("aegis_breaker", 1.0, 0.45)
         .range(0.0, 4.5)
         .cooldown(90)
         .weight(8)
         .tags("melee guard_break")
         .telegraph(0.15, 0.45)
         .advance(0.25, 0.45, 0.45)
         .combo("zenith_cleave", 0.4F)
         .utility((b, t, c) -> c.ticksSinceHitTarget > 100 ? 1.5F : 1.0F)
         .tick((b, level, t, tick) -> {
            if (tick <= 9) {
               this.sink(level, t, 0.3);
            }

            if (tick == 3) {
               Vfx.telegraphCone(level, floor(level, this.getEntityPos()), this.getYaw(), 40.0F, 4.0, 6, S);
            }
         })
         .hit((b, level, t, i) -> {
            DamageSource src = this.getDamageSources().mobAttack(this);
            Vec3d f = this.facing();

            for (LivingEntity e : this.foes(level, this.getEntityPos(), 4.5, 3.5, 7.5)) {
               if (this.inArc(e, 4.0, 45.0)) {
                  this.breakGuard(level, e, 100);
                  if (e.damage(level, src, this.dmg(15.0F))) {
                     Vec3d away = flatDir(e.getEntityPos().subtract(this.getEntityPos()), f);
                     e.addVelocity(away.x * 1.8, 0.4, away.z * 1.8);
                     e.knockedBack = true;
                     e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 1), this);
                  }
               }
            }

            Vec3d front = this.getEntityPos().add(0.0, 1.6, 0.0).add(f.multiply(1.6));
            Vfx.impact(level, front, S, 0.8);
            Vfx.shards(level, front, 16774870, 16, 0.5);
            Vfx.ring(level, front, S.glow(), 2.5, 8);
            Vfx.shake(level, this.getEntityPos(), 16.0, 0.4, 6);
         });
   }

   private BossAction orbitRend() {
      return BossAction.of("orbit_rend", 1.4, 0.6)
         .range(0.0, 5.5)
         .cooldown(80)
         .weight(10)
         .tags("melee aoe behind")
         .telegraph(0.2, 0.6)
         .tick((b, level, t, tick) -> {
            if (tick <= 12) {
               this.sink(level, t, 0.3);
            }

            if (tick == 4) {
               Vfx.telegraphCircle(level, floor(level, this.getEntityPos()), 5.2, 8, S);
            }
         })
         .hit((b, level, t, i) -> {
            this.spearArc(level, 5.2, 180.0F, 17.0F, 1.5, null, null, false);
            Vec3d c = this.chest();
            Vfx.slashArc(level, c, this.getYaw(), 0.0F, 0.0F, 5.2, S, 360.0F);
            Vfx.debris(level, c, Vfx.Debris.FEATHER, 16776176, 12, 0.4);

            for (int k = 0; k < 4; k++) {
               Vec3d out = Vec3d.fromPolar(0.0F, this.getYaw() + k * 90);
               Vfx.streaks(level, c.add(out.multiply(3.0)), Vec3d.fromPolar(0.0F, this.getYaw() + k * 90 + 90.0F), S, 6);
            }
         });
   }

   private BossAction meridianDance() {
      return BossAction.of("meridian_dance", 2.4, 0.45, 0.85, 1.25, 1.9)
         .range(0.0, 6.5)
         .cooldown(170)
         .weight(11)
         .phase(2)
         .tags("melee aoe")
         .telegraph(0.2, 1.9)
         .advance(0.25, 0.45, 0.35)
         .advance(0.65, 0.85, 0.3)
         .advance(1.05, 1.25, 0.3)
         .advance(1.6, 1.9, 0.25)
         .tick((b, level, t, tick) -> {
            if (tick > 25 && tick < 32) {
               this.rise(0.06);
            } else if (tick <= 38) {
               this.sink(level, t, 0.3);
            }

            if (tick == 4 || tick == 12 || tick == 20) {
               Vfx.telegraphCone(level, floor(level, this.getEntityPos()), this.getYaw(), 60.0F, 5.0, 5, S);
            } else if (tick == 26) {
               this.plungeAt = floor(level, this.getEntityPos().add(this.facing().multiply(2.5)));
               Vfx.telegraphCircle(level, this.plungeAt, 3.0, 12, S);
            }
         })
         .hit((b, level, t, i) -> {
            Vec3d c = this.chest();
            float yaw = this.getYaw();
            if (i == 0) {
               this.spearArc(level, 5.0, 60.0F, 14.0F, 0.8, null, null, false);
               Vfx.slashArc(level, c, yaw, 0.0F, 40.0F, 5.0, S, 170.0F);
            } else if (i == 1) {
               this.spearArc(level, 5.0, 60.0F, 14.0F, 0.8, null, null, true);
               Vfx.slashArc(level, c, yaw, 0.0F, -40.0F, 5.0, S, -170.0F);
            } else if (i == 2) {
               for (LivingEntity e : this.spearArc(level, 5.0, 70.0F, 16.0F, 0.4, null, null, true)) {
                  Vec3d v = e.getVelocity();
                  e.setVelocity(v.x, Math.max(v.y, 0.9), v.z);
                  e.knockedBack = true;
               }

               Vfx.slashArc(level, c, yaw, -10.0F, -90.0F, 5.0, S, 200.0F);
            } else {
               Vec3d p = this.plungeAt != null ? this.plungeAt : floor(level, this.getEntityPos().add(this.facing().multiply(2.5)));
               this.blast(level, p, 3.0, 24.0F, 1.6, true);
               Vfx.heavyImpact(level, p, S, 1.0);
               Vfx.groundCrack(level, p, 3.0, S);
            }
         })
         .end((b, level, t, i) -> this.plungeAt = null);
   }

   private BossAction cometDive() {
      return BossAction.of("comet_dive", 2.4, 1.4)
         .range(6.0, 26.0)
         .cooldown(180)
         .weight(9)
         .tags("gap_closer aoe anti_air")
         .lockFacing()
         .superArmor()
         .telegraph(0.95, 1.4)
         .tick((b, level, t, tick) -> {
            if (tick >= 5 && tick <= 16) {
               boolean free = level.isSpaceEmpty(this, this.getBoundingBox().offset(0.0, 0.85, 0.0));
               this.setVelocity(0.0, free ? 0.85 : 0.0, 0.0);
               if (tick % 2 == 0) {
                  Vfx.trail(level, this.mid(), S, 1.0);
               }
            } else if (tick == 19) {
               this.diveAir = t != null && t == this.ctx().target && this.ctx().targetAirborne;
               Vec3d aim = t != null ? this.predict(t, 9.0) : this.getEntityPos();
               this.diveAt = this.diveAir ? aim : floor(level, aim);
               this.turnTo(this.diveAt);
               Vfx.telegraphCircle(level, floor(level, this.diveAt), 4.5, 9, S);
               Vfx.chargeUp(level, this.mid(), S, 9);
            } else if (tick >= 20 && tick < 28 && this.diveAt != null) {
               Vec3d v = this.diveAt.subtract(this.getEntityPos()).multiply(1.0 / (28 - tick));
               this.setVelocity(v);
               Vfx.meteorTrail(level, this.mid(), v, S);
               if (tick == 20) {
                  Vfx.dashTrail(level, this.mid(), this.diveAt.add(0.0, 1.8, 0.0), S);
               }
            } else if (tick == 28) {
               this.setVelocity(Vec3d.ZERO);
            } else if (tick >= 36 && tick <= 46) {
               this.rise(0.12);
            }
         })
         .hit((b, level, t, i) -> {
            Vec3d at = this.diveAt != null ? this.diveAt : this.getEntityPos();
            Vec3d g = floor(level, at);
            if (this.diveAir) {
               this.blast(level, at.add(0.0, -3.5, 0.0), 3.5, 30.0F, 1.2, false, 7.0, null);
            }

            this.blast(level, g, 4.5, 34.0F, 1.8, true);
            Vfx.heavyImpact(level, g, S, 1.6);
            Vfx.groundCrack(level, g, 4.0, S);
            Vfx.nova(level, g.add(0.0, 1.0, 0.0), S, 5.0);
            if (this.phase() >= 2) {
               Vfx.runeCircle(level, g, 3.0, S, 60, 0.04F, Vfx.Rune.SOLAR);
               this.zone(level, g, 3.0, 60, 5.0F, null);
            }

            if (this.phase() >= 3) {
               this.wave(level, g, 9.0, 12, 12.0F, 1.0);
            }
         })
         .end((b, level, t, i) -> {
            if (i == -2) {
               this.setVelocity(Vec3d.ZERO);
            }

            this.diveAt = null;
         });
   }

   private BossAction starstep() {
      return BossAction.of("starstep", 1.2, 0.2, 0.55, 0.9)
         .range(0.0, 24.0)
         .cooldown(140)
         .weight(7)
         .tags("mobility gap_closer")
         .combo("orbit_rend", 0.5F)
         .combo("dawn_lance", 0.4F)
         .utility((b, t, c) -> (c.kitingTicks <= 40 && !c.targetUnreachable ? 1.0F : 2.5F) * (c.targetBlocking ? 2.0F : 1.0F))
         .hit((b, level, t, i) -> {
            if (t != null && (i != 1 || this.phase() >= 1)) {
               float ly = t.getYaw();
               Vec3d look = Vec3d.fromPolar(0.0F, ly);
               Vec3d side = Vec3d.fromPolar(0.0F, ly - 90.0F);

               Vec3d to = switch (i) {
                  case 0 -> t.getEntityPos().add(side.multiply(5.0));
                  case 1 -> t.getEntityPos().subtract(side.multiply(5.0));
                  default -> t.getEntityPos().subtract(look.multiply(2.8));
               };
               Vec3d from = this.getEntityPos();
               if (this.blinkTo(to.add(0.0, this.hoverHeight(), 0.0))) {
                  this.faceTarget(t, 180.0F);
                  Vfx.dashTrail(level, from.add(0.0, 1.8, 0.0), this.mid(), S);
                  Vfx.debris(level, this.mid(), Vfx.Debris.FEATHER, 16776176, 8, 0.3);
                  if (this.phase() >= 2) {
                     this.afterimage(level, floor(level, from));
                  }
               }
            }
         });
   }

   private BossAction astralVolley() {
      return BossAction.of("astral_volley", 1.6, 0.6, 0.85, 1.1)
         .range(5.0, 32.0)
         .cooldown(90)
         .weight(10)
         .tags("ranged")
         .combo("judgment", 0.3F)
         .utility((b, t, c) -> (c.kitingTicks > 40 ? 1.8F : 1.0F) * (c.dist < 5.0 ? 0.5F : 1.0F))
         .start((b, level, t, i) -> Arrays.fill(this.volleyAims, null))
         .tick((b, level, t, tick) -> {
            int k = tick == 8 ? 0 : (tick == 13 ? 1 : (tick == 18 ? 2 : -1));
            if (k >= 0 && t != null) {
               Vec3d from = this.volleyFrom();
               Vec3d body = new Vec3d(0.0, t.getHeight() * 0.5, 0.0);
               if (k == 0) {
                  this.volleyLead = this.predict(t, from.distanceTo(t.getEntityPos()) / this.volleySpeed());
                  this.volleyAims[0] = this.volleyLead.add(body);
               } else if (k == 1) {
                  this.volleyAims[1] = t.getEntityPos().add(body);
               } else {
                  this.volleyAims[2] = t.getEntityPos().multiply(2.0).subtract(this.volleyLead).add(body);
               }

               Vfx.glint(level, from, 16777215, 1.2);
               Vfx.beam(level, from, this.volleyAims[k], S, 0.08, 4);
            }
         })
         .hit((b, level, t, i) -> {
            Vec3d aim = this.volleyAims[i];
            if (aim == null) {
               if (t == null) {
                  return;
               }

               aim = t.getEyePos();
            }

            Vec3d from = this.volleyFrom();
            Vec3d dir = aim.subtract(from).normalize();
            boolean dark = this.phase() >= 3;
            BossBolt.Style style = dark ? BossBolt.Style.VOID : BossBolt.Style.STAR;
            double speed = this.volleySpeed();
            BossBolt.shoot(level, this, style, from, dir.multiply(speed), this.dmg(12.0F)).aoe(1.5).life(60);
            if (this.phase() >= 2) {
               float off = (float)Math.toRadians(8.0);
               BossBolt.shoot(level, this, style, from, dir.rotateY(off).multiply(speed), this.dmg(12.0F)).aoe(1.5).life(60);
               BossBolt.shoot(level, this, style, from, dir.rotateY(-off).multiply(speed), this.dmg(12.0F)).aoe(1.5).life(60);
            }

            Vfx.flare(level, from, dark ? VOID.glow() : S.glow(), 1.2, 4);
            this.volleyAims[i] = null;
         });
   }

   private BossAction haloBlades() {
      return BossAction.of("halo_blades", 2.4, 0.8, 1.2, 1.6, 2.0)
         .range(4.0, 28.0)
         .cooldown(150)
         .weight(8)
         .maxPhase(2)
         .tags("ranged")
         .tick((b, level, t, tick) -> {
            if (tick == 13 || tick == 21 || tick == 29 || tick == 37) {
               Vfx.glint(level, this.bladeFrom(), 16777215, 1.4);
            }
         })
         .hit((b, level, t, i) -> {
            Vec3d from = this.bladeFrom();
            int n = this.phase() >= 1 ? 3 : 2;

            for (int k = 0; k < n; k++) {
               float off = n == 3 ? (k - 1) * 60.0F : (k == 0 ? -60.0F : 60.0F);
               Vec3d v = Vec3d.fromPolar(0.0F, this.getYaw() + off).multiply(0.45).add(0.0, 0.3, 0.0);
               BossBolt.shoot(level, this, BossBolt.Style.STAR, from, v, this.dmg(9.0F)).homing(t, 0.06).life(80);
            }

            Vfx.ring(level, from, S.glow(), 1.8, 6);
            Vfx.flare(level, from, S.core(), 1.8, 5);
         });
   }

   private BossAction meteorLance() {
      return BossAction.of("meteor_lance", 2.2, 1.2)
         .range(8.0, 36.0)
         .cooldown(160)
         .weight(8)
         .tags("ranged punish")
         .telegraph(0.7, 1.2)
         .utility((b, t, c) -> c.kitingTicks > 40 ? 1.5F : 1.0F)
         .tick((b, level, t, tick) -> {
            if (tick == 14 && t != null) {
               Vec3d from = this.lanceHead();
               int travel = (int)Math.round(from.distanceTo(t.getEntityPos()) / 2.4);
               Vec3d a = floor(level, this.predict(t, 10 + travel));
               travel = Math.max(1, (int)Math.round(from.distanceTo(a) / 2.4));
               this.meteorAt = a;
               this.meteorTravel = travel;
               Vec3d base = floor(level, this.getEntityPos());
               Vfx.telegraphLine(level, base, a.subtract(base), flatDist(base, a) + 2.0, 1.4, 10, S);
               Vfx.telegraphCircle(level, a, 3.5, 10 + travel, S);
               Vfx.chargeUp(level, from, S, 10);
            }
         })
         .hit(
            (b, level, t, i) -> {
               Vec3d a = this.meteorAt;
               if (a != null) {
                  this.meteorAt = null;
                  Vec3d from = this.lanceHead();
                  BossBolt.shoot(level, this, BossBolt.Style.STAR, from, a.add(0.0, 0.3, 0.0).subtract(from).normalize().multiply(2.4), this.dmg(24.0F))
                     .aoe(3.5)
                     .life(40);
                  Vfx.flare(level, from, S.core(), 2.2, 5);
                  boolean brand = this.phase() >= 1;
                  this.schedule(this.meteorTravel, () -> {
                     Vfx.heavyImpact(level, a, S, 1.0);
                     Vfx.groundCrack(level, a, 3.0, S);
                     if (brand) {
                        Vfx.runeCircle(level, a, 2.5, S, 60, 0.03F, Vfx.Rune.ASTRAL);
                        this.zone(level, a, 2.5, 60, 5.0F, new StatusEffectInstance(StatusEffects.SLOWNESS, 30, 0));
                     }
                  });
               }
            }
         );
   }

   private BossAction judgment() {
      return BossAction.of("judgment", 2.6, 1.6)
         .range(0.0, 40.0)
         .cooldown(200)
         .weight(7)
         .tags("ranged punish anti_air")
         .telegraph(0.4, 1.6)
         .utility((b, t, c) -> c.targetUnreachable ? 3.0F : 1.0F)
         .start((b, level, t, i) -> {
            this.sentences.clear();
            if (t != null) {
               this.sentences.add(new AstraelBoss.Sentence(t, this.ctx().targetHealing));
            }

            if (this.phase() >= 2 && this.engaged().size() >= 2) {
               ServerPlayerEntity second = null;
               float best = -1.0F;

               for (ServerPlayerEntity p : this.engaged()) {
                  float th = this.threat().get(p);
                  if (p != t && th > best) {
                     best = th;
                     second = p;
                  }
               }

               if (second != null) {
                  this.sentences.add(new AstraelBoss.Sentence(second, healing(second)));
               }
            }
         })
         .tick((b, level, t, tick) -> {
            if (tick == 8 || tick == 14 || tick == 20 || tick == 26) {
               for (AstraelBoss.Sentence s : this.sentences) {
                  if (s.who.isAlive()) {
                     s.spot = floor(level, s.who.getEntityPos());
                     Vfx.telegraphCircle(level, s.spot, 2.4, 32 - tick, HOLY);
                     if (tick == 8) {
                        BossFx.markTarget(level, this, s.who, 24);
                     }
                  }
               }
            }
         })
         .hit((b, level, t, i) -> {
            DamageSource src = this.getDamageSources().mobAttack(this);

            for (AstraelBoss.Sentence s : this.sentences) {
               Vec3d spot = s.spot;
               if (spot != null) {
                  Vfx.beam(level, spot.add(0.0, 30.0, 0.0), spot, HOLY, 1.2, 8);
                  Vfx.pillar(level, spot, HOLY, 26.0, 2.4);
                  Vfx.heavyImpact(level, spot, HOLY, 0.8);

                  for (LivingEntity e : this.foes(level, spot, 2.4, 1.0, 31.0)) {
                     if (e.damage(level, src, this.dmg(26.0F))) {
                        e.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 100, 0), this);
                        e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 1), this);
                        if (s.healing && e == s.who) {
                           e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 0), this);
                        }
                     }
                  }
               }
            }
         })
         .end((b, level, t, i) -> this.sentences.clear());
   }

   private BossAction heavensTether() {
      return BossAction.of("heavens_tether", 1.4, 0.7)
         .range(3.0, 30.0)
         .cooldown(160)
         .weight(5)
         .tags("ranged anti_air")
         .when((b, t) -> t != null && b.canSee(t))
         .utility((b, t, c) -> c.kitingTicks > 60 ? 2.0F : 1.0F)
         .tick((b, level, t, tick) -> {
            if (tick == 6 && t != null) {
               Vfx.chargeUp(level, this.tip(), HOLY, 8);
               Vfx.beam(level, this.tip(), t.getEyePos(), HOLY, 0.06, 8);
            }
         })
         .hit((b, level, t, i) -> {
            if (t != null && this.canSee(t)) {
               Vfx.lightning(level, this.tip(), t.getEyePos(), HOLY, 3);
               DamageSource src = this.getDamageSources().mobAttack(this);
               boolean air = t.isGliding() || !t.isOnGround() && !t.isTouchingWater() && BossContext.heightAboveGround(level, t) > 2.5;
               if (air) {
                  t.stopGliding();
                  t.removeStatusEffect(StatusEffects.SLOW_FALLING);
                  t.damage(level, src, this.dmg(14.0F));
                  Vec3d v = t.getVelocity();
                  t.setVelocity(v.x * 0.2, -1.8, v.z * 0.2);
                  t.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1), this);
               } else {
                  t.damage(level, src, this.dmg(12.0F));
                  Vec3d to = flatDir(this.getEntityPos().subtract(t.getEntityPos()), Vec3d.ZERO);
                  double s = MathHelper.clamp((flatDist(this.getEntityPos(), t.getEntityPos()) - 3.0) / 7.2, 0.35, 1.3);
                  t.setVelocity(to.x * s, 0.3, to.z * s);
               }

               t.knockedBack = true;
            }
         });
   }

   private BossAction starfall() {
      return BossAction.of("starfall", 3.2, 1.1, 1.5, 1.9, 2.3, 2.7)
         .range(0.0, 36.0)
         .cooldown(220)
         .weight(8)
         .phase(1)
         .anyTarget()
         .tags("ranged aoe anti_air")
         .telegraph(0.1, 2.7)
         .tick((b, level, t, tick) -> {
            if (tick >= 2 && tick <= 34 && (tick - 2) % 8 == 0) {
               LivingEntity aim = (LivingEntity)(t != null ? t : this.otherPlayer(null));
               if (aim == null) {
                  this.fallingStar(level, BossFx.around(level, this, this.getEntityPos(), 2.0, 8.0), 20, 2.6, 16.0F);
               } else {
                  this.fallingStar(level, floor(level, this.predict(aim, 20.0)), 20, 2.6, 16.0F);
                  if (this.phase() >= 2) {
                     this.fallingStar(level, BossFx.around(level, this, aim.getEntityPos(), 1.5, 6.0), 20, 2.6, 16.0F);
                  }

                  if (this.engaged().size() >= 2) {
                     ServerPlayerEntity other = this.otherPlayer(aim);
                     if (other != null) {
                        this.fallingStar(level, floor(level, this.predict(other, 20.0)), 20, 2.6, 16.0F);
                     }
                  }
               }
            }
         })
         .hit((b, level, t, i) -> {
            Vec3d h = this.halo();
            Vfx.flare(level, h, 16767088, 2.0, 6);
            Vfx.glint(level, h, 16777215, 1.6);
         });
   }

   private BossAction gravityWell() {
      return BossAction.of("gravity_well", 2.5, 0.8, 2.0)
         .range(0.0, 22.0)
         .cooldown(220)
         .weight(8)
         .phase(1)
         .tags("ranged aoe")
         .telegraph(0.8, 2.0)
         .tick((b, level, t, tick) -> {
            if (tick > 16 && tick < 40 && this.well != null) {
               double pull = this.phase() >= 3 ? 0.1 : 0.085;

               for (ServerPlayerEntity p : this.engaged()) {
                  Vec3d w = this.well;
                  if (this.well2 != null && flatDist(p.getEntityPos(), this.well2) < flatDist(p.getEntityPos(), w)) {
                     w = this.well2;
                  }

                  this.pullToward(p, w, 10.0, flatDist(p.getEntityPos(), w) <= 4.5 ? 0.03 : pull);
               }
            }
         })
         .hit((b, level, t, i) -> {
            if (i == 0) {
               this.well = floor(level, t != null ? this.predict(t, 12.0) : this.getEntityPos());
               this.well2 = null;
               if (this.phase() >= 3) {
                  LivingEntity other = this.otherPlayer(t);
                  LivingEntity aim = other != null ? other : t;
                  Vec3d w2 = floor(level, aim != null ? this.predict(aim, 18.0) : this.getEntityPos());
                  Vec3d off = w2.subtract(this.well);
                  if (flatDist(w2, this.well) < 7.0) {
                     Vec3d d = flatDir(off, Vec3d.fromPolar(0.0F, this.getRandom().nextFloat() * 360.0F));
                     w2 = floor(level, this.well.add(d.multiply(7.0)));
                  }

                  this.well2 = w2;
               }

               for (Vec3d w : this.wells()) {
                  Vfx.vortex(level, w, 4.5, 4.0, VOID, 24);
                  Vfx.telegraphCircle(level, w, 4.5, 24, S);
               }
            } else {
               for (Vec3d w : this.wells()) {
                  Set<Integer> hit = new HashSet<>();
                  this.blast(level, w, 4.5, 30.0F, 2.2, true, 4.0, hit);

                  for (LivingEntity e : this.foes(level, w, 4.5, 1.5, 4.0)) {
                     if (hit.contains(e.getId())) {
                        e.addVelocity(0.0, 0.8, 0.0);
                        e.knockedBack = true;
                     }
                  }

                  Vfx.nova(level, w.add(0.0, 1.0, 0.0), S, 5.0);
                  Vfx.explosion(level, w.add(0.0, 0.5, 0.0), S, 3.0);
                  Vfx.heavyImpact(level, w, S, 1.0);
               }
            }
         })
         .end((b, level, t, i) -> {
            this.well = null;
            this.well2 = null;
         });
   }

   private BossAction constellation() {
      return BossAction.of("constellation", 3.0, 1.0, 2.5)
         .range(0.0, 26.0)
         .cooldown(240)
         .weight(7)
         .phase(1)
         .tags("ranged aoe")
         .telegraph(1.0, 2.5)
         .hit((b, level, t, i) -> {
            if (i == 0) {
               this.conStars.clear();
               Vec3d c = floor(level, t != null ? this.predict(t, 20.0) : this.getEntityPos());
               double rot = this.getRandom().nextDouble() * Math.PI * 2.0;
               this.addStars(level, c, 6.0, rot);
               if (this.phase() >= 2) {
                  this.addStars(level, c, 3.5, rot + (Math.PI / 5));
               }

               for (int k = 0; k < this.conStars.size(); k++) {
                  Vec3d s = this.conStars.get(k);
                  Vec3d e = this.conEnd(k);
                  Vfx.glint(level, s.add(0.0, 0.8, 0.0), 16774870, 1.2);
                  Vfx.wisps(level, s.add(0.0, 0.6, 0.0), S, 3, 0.6, 30);
                  Vfx.telegraphLine(level, s, e.subtract(s), flatDist(s, e), 2.4, 30, S);
               }
            } else {
               DamageSource src = this.getDamageSources().mobAttack(this);
               Set<Integer> hit = new HashSet<>();

               for (int k = 0; k < this.conStars.size(); k++) {
                  Vec3d s = this.conStars.get(k);
                  Vec3d e = this.conEnd(k);
                  Vfx.beam(level, s.add(0.0, 0.6, 0.0), e.add(0.0, 0.6, 0.0), S, 0.5, 8);

                  for (LivingEntity v : this.foesOn(level, s, e, 1.2, 3.0, 3.0)) {
                     if (hit.add(v.getId()) && v.damage(level, src, this.dmg(22.0F))) {
                        v.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1), this);
                     }
                  }

                  if (k < 5) {
                     Vfx.heavyImpact(level, s, S, 0.5);
                  } else {
                     Vfx.impact(level, s.add(0.0, 0.4, 0.0), S, 0.5);
                  }
               }

               this.conStars.clear();
            }
         })
         .end((b, level, t, i) -> this.conStars.clear());
   }

   private BossAction radiance() {
      return BossAction.of("radiance", 2.0, 1.0)
         .range(0.0, 9.0)
         .cooldown(180)
         .weight(7)
         .anyTarget()
         .tags("aoe punish")
         .telegraph(0.1, 1.0)
         .tick((b, level, t, tick) -> {
            if (tick == 1) {
               this.radianceAt = floor(level, this.getEntityPos());
               Vfx.telegraphCircle(level, this.radianceAt, 8.0, 19, S);
               Vfx.chargeUp(level, this.core(), S, 19);
            }
         })
         .hit((b, level, t, i) -> {
            this.blast(level, this.radianceAt, 8.0, 20.0F, 1.4, true);
            Vec3d eye = this.core();
            Vfx.nova(level, eye, S, 8.0);
            Vfx.radialFlash(level, this.getEntityPos(), 24.0, 16774870, 0.6F);

            for (ServerPlayerEntity p : this.engaged()) {
               double d = p.distanceTo(this);
               if (!(d > 16.0)) {
                  Vec3d toBoss = eye.subtract(p.getEyePos()).normalize();
                  if (p.getRotationVec(1.0F).dotProduct(toBoss) > 0.5) {
                     p.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 80, 0), this);
                     p.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 80, 0), this);
                  } else if (d <= 12.0) {
                     p.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 40, 0), this);
                  }
               }
            }
         });
   }

   private BossAction aegisOfStars() {
      return BossAction.of("aegis_of_stars", 1.6, 0.8)
         .range(0.0, 40.0)
         .cooldown(520)
         .weight(6)
         .anyTarget()
         .tags("defensive")
         .utility((b, t, c) -> b.warded() ? 0.0F : 1.0F)
         .hit((b, level, t, i) -> {
            this.shield(this.getMaxHealth() * (this.phase() >= 2 ? 0.14F : 0.1F), 200);
            Vfx.ward(level, floor(level, this.getEntityPos()), 3.0, S, 60);
            this.aegisUntil = this.age + 200;
            this.aegisNovaAt = null;
         });
   }

   private BossAction choirOfStars() {
      return BossAction.of("choir_of_stars", 2.2, 1.4)
         .range(0.0, 40.0)
         .cooldown(600)
         .weight(5)
         .anyTarget()
         .tags("summon")
         .when((b, t) -> this.minionCount() < 6)
         .tick((b, level, t, tick) -> {
            if (tick == 8) {
               this.choirSpots.clear();
               int n = Math.min(3, 6 - this.minionCount());

               for (int k = 0; k < n; k++) {
                  Vec3d p = BossFx.around(level, this, this.getEntityPos(), 3.0, 7.0);
                  this.choirSpots.add(p);
                  Vfx.telegraphCircle(level, p, 1.2, 20, S);
                  Vfx.pillar(level, p, S, 1.5, 0.4);
               }
            }
         })
         .hit((b, level, t, i) -> {
            int p = this.phase();
            int sentinels = p >= 3 ? 2 : (p == 2 ? 1 : 0);

            for (int k = 0; k < this.choirSpots.size(); k++) {
               this.spawnMinion(level, k < sentinels ? "astral_sentinel" : "starling", this.choirSpots.get(k), t);
            }

            this.choirSpots.clear();
            this.minionCheckedAt = -1000;
         })
         .end((b, level, t, i) -> this.choirSpots.clear());
   }

   private BossAction supernova() {
      return BossAction.of("supernova", 4.0, 2.8)
         .range(0.0, 24.0)
         .cooldown(900)
         .weight(6)
         .phase(2)
         .anyTarget()
         .tags("aoe ultimate")
         .superArmor()
         .telegraph(0.1, 2.8)
         .tick((b, level, t, tick) -> {
            if (tick == 1) {
               Vec3d g = floor(level, this.getEntityPos());
               Vfx.telegraphCircle(level, g, 18.0, 55, S);
               Vfx.chargeUp(level, this.core(), S, 55);
               this.shelters.clear();
               int n = this.phase() >= 3 ? 2 : 3;
               double base = this.getRandom().nextDouble() * Math.PI * 2.0;

               for (int k = 0; k < n; k++) {
                  double a = base + k * Math.PI * 2.0 / n + (this.getRandom().nextDouble() - 0.5) * (n == 3 ? 0.5 : 0.9);
                  double r = 8.0 + this.getRandom().nextDouble() * 4.0;
                  Vec3d p = floor(level, new Vec3d(g.x + Math.cos(a) * r, g.y, g.z + Math.sin(a) * r));
                  this.shelters.add(p);
                  Vfx.runeCircle(level, p, 2.2, VOID, 60, -0.04F, Vfx.Rune.ASTRAL);
                  Vfx.vortex(level, p, 1.8, 3.0, VOID, 56);
               }
            } else if (tick == 20) {
               Vfx.shake(level, this.getEntityPos(), 40.0, 0.3, 36);
            }
         })
         .hit((b, level, t, i) -> {
            Vec3d g = floor(level, this.getEntityPos());
            DamageSource src = this.getDamageSources().mobAttack(this);

            for (LivingEntity e : this.foes(level, this.getEntityPos(), 18.0, 18.0, 18.0)) {
               if (!(e.distanceTo(this) > 18.0F)) {
                  if (sheltered(e.getEntityPos(), this.shelters, 2.2)) {
                     Vfx.shieldBubble(level, e.getEntityPos(), 1.2, VOID, 10);
                  } else {
                     this.breakGuard(level, e, 60);
                     if (e.damage(level, src, this.dmg(55.0F))) {
                        Vec3d away = flatDir(e.getEntityPos().subtract(this.getEntityPos()), this.facing());
                        e.addVelocity(away.x * 2.5, 0.6, away.z * 2.5);
                        e.knockedBack = true;
                        e.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 60, 0), this);
                     }
                  }
               }
            }

            Vfx.nova(level, this.core(), S, 18.0);
            Vfx.impactFrame(level, this.getEntityPos(), 40.0, 16777215, 1.0F);
            Vfx.pillar(level, g, S, 30.0, 3.5);
            Vfx.heavyImpact(level, g, S, 2.5);
            Vfx.debris(level, this.core(), Vfx.Debris.FEATHER, 16776176, 30, 0.6);
         })
         .end((b, level, t, i) -> {
            this.lockout("supernova");
            this.shelters.clear();
         });
   }

   private BossAction singularity() {
      return BossAction.of("singularity", 5.0, 2.0, 2.8, 4.2)
         .range(0.0, 28.0)
         .cooldown(1000)
         .weight(6)
         .phase(3)
         .anyTarget()
         .tags("aoe ultimate")
         .superArmor()
         .telegraph(0.1, 4.2)
         .tick((b, level, t, tick) -> {
            if (tick == 1) {
               this.singAt = floor(level, this.getEntityPos());
               this.singTheta = this.getRandom().nextFloat() * 90.0F;
               Vfx.telegraphCircle(level, this.singAt, 7.0, 83, S);
               Vfx.telegraphCircle(level, this.singAt, 16.0, 83, S);
            }

            if (tick <= 20) {
               boolean free = level.isSpaceEmpty(this, this.getBoundingBox().offset(0.0, 0.15, 0.0));
               this.setVelocity(0.0, free ? 0.15 : 0.0, 0.0);
            } else if (tick > 84 && tick <= 100) {
               boolean free = level.isSpaceEmpty(this, this.getBoundingBox().offset(0.0, -0.2, 0.0));
               this.setVelocity(0.0, free ? -0.2 : 0.0, 0.0);
            }

            if (tick == 20) {
               Vfx.vortex(level, this.singAt, 16.0, 6.0, VOID, 64);
            }

            if (tick == 16 || tick == 32) {
               float base = this.singTheta + (tick == 32 ? 45 : 0);

               for (int k = 0; k < 4; k++) {
                  Vfx.telegraphLine(level, this.singAt, Vec3d.fromPolar(0.0F, base + k * 90), 18.0, 2.0, 24, S);
               }
            }

            if (tick >= 20 && tick < 84) {
               double pull = 0.06 + 0.03 * (tick - 20) / 63.0;

               for (ServerPlayerEntity p : this.engaged()) {
                  this.pullToward(p, this.singAt, 24.0, pull);
               }
            }
         })
         .hit((b, level, t, i) -> {
            DamageSource src = this.getDamageSources().mobAttack(this);
            Vec3d g = this.singAt;
            if (i >= 2) {
               for (LivingEntity e : this.foes(level, g, 16.0, 3.0, 10.0)) {
                  boolean inner = flatDist(e.getEntityPos(), g) <= 7.0;
                  if (inner) {
                     this.breakGuard(level, e, 60);
                  }

                  e.damage(level, src, this.dmg(inner ? 60.0F : 22.0F));
                  Vec3d away = flatDir(e.getEntityPos().subtract(g), this.facing());
                  e.addVelocity(away.x * 3.0, 0.8, away.z * 3.0);
                  e.knockedBack = true;
               }

               Vfx.nova(level, g.add(0.0, 1.0, 0.0), VOID, 16.0);
               Vfx.impactFrame(level, this.getEntityPos(), 40.0, 11550975, 1.0F);
               Vfx.heavyImpact(level, g, VOID, 3.0);
               Vfx.explosion(level, g.add(0.0, 1.0, 0.0), S, 5.0);
            } else {
               float base = this.singTheta + (i == 1 ? 45 : 0);
               Set<Integer> hit = new HashSet<>();

               for (int k = 0; k < 4; k++) {
                  Vec3d end = g.add(Vec3d.fromPolar(0.0F, base + k * 90).multiply(18.0));
                  Vfx.beam(level, g.add(0.0, 1.0, 0.0), end.add(0.0, 1.0, 0.0), VOID, 0.8, 6);

                  for (LivingEntity e : this.foesOn(level, g, end, 1.0, 1.5, 3.0)) {
                     if (hit.add(e.getId())) {
                        e.damage(level, src, this.dmg(20.0F));
                     }
                  }
               }
            }
         })
         .end((b, level, t, i) -> {
            this.lockout("singularity");
            this.setVelocity(this.getVelocity().multiply(0.3));
         });
   }

   private BossAction lastLight() {
      return BossAction.of("last_light", 8.0, 2.4, 3.6, 4.8, 7.0)
         .range(0.0, 64.0)
         .cooldown(20)
         .weight(0)
         .phase(3)
         .anyTarget()
         .tags("aoe ultimate")
         .telegraph(0.1, 7.0)
         .start((b, level, t, i) -> {
            this.setLook(4);
            BlockPos home = this.home();
            this.lightCenter = floor(level, home != null ? Vec3d.ofBottomCenter(home) : this.getEntityPos());
            this.sanctuaries.clear();
            this.shield(this.getMaxHealth() * 0.2F, 170);
            this.lightWard = true;
         })
         .tick((b, level, t, tick) -> {
            if (tick < 138 && this.lightWard && this.shield() <= 0.0F) {
               this.lightWard = false;
               this.forceAction(level, "fading_light");
            } else {
               Vec3d c = this.lightCenter;
               Vec3d perch = tick <= 140 ? c.add(0.0, 7.0, 0.0) : c.add(0.0, this.hoverHeight(), 0.0);
               this.setVelocity(perch.subtract(this.getEntityPos()).multiply(tick <= 24 ? 0.12 : (tick <= 140 ? 0.1 : 0.08)));
               if (tick == 1) {
                  Vfx.telegraphCircle(level, c, 30.0, 139, S);
               } else if (tick == 20) {
                  double rot = this.getRandom().nextDouble() * Math.PI * 2.0;

                  for (int k = 0; k < 4; k++) {
                     double a = rot + k * Math.PI / 2.0;
                     double r = 8.0 + this.getRandom().nextDouble() * 5.0;
                     Vec3d p = floor(level, new Vec3d(c.x + Math.cos(a) * r, c.y, c.z + Math.sin(a) * r));
                     this.sanctuaries.add(p);
                     Vfx.runeCircle(level, p, 2.0, HOLY, 125, 0.03F, Vfx.Rune.SOLAR);
                     Vfx.pillar(level, p, HOLY, 6.0, 0.6);
                     Vfx.wisps(level, p.add(0.0, 0.8, 0.0), HOLY, 4, 1.2, 120);
                  }
               } else if (tick == 80) {
                  for (Vec3d p : this.sanctuaries) {
                     Vfx.wisps(level, p.add(0.0, 0.8, 0.0), HOLY, 4, 1.2, 120);
                  }
               }

               if (tick == 24 || tick == 48 || tick == 72) {
                  this.meteorWave(level, c);
               } else if (tick == 100) {
                  Vfx.chargeUp(level, this.core(), HOLY, 40);
               }
            }
         })
         .hit((b, level, t, i) -> {
            if (i >= 3) {
               Vec3d c = this.lightCenter;
               DamageSource src = this.getDamageSources().mobAttack(this);

               for (LivingEntity e : this.foes(level, c, 40.0, 12.0, 40.0)) {
                  if (sheltered(e.getEntityPos(), this.sanctuaries, 2.2)) {
                     Vfx.shieldBubble(level, e.getEntityPos(), 1.2, HOLY, 12);
                  } else {
                     this.breakGuard(level, e, 60);
                     if (e.damage(level, src, this.dmg(65.0F))) {
                        Vec3d away = flatDir(e.getEntityPos().subtract(c), this.facing());
                        e.addVelocity(away.x * 1.6, 0.5, away.z * 1.6);
                        e.knockedBack = true;
                     }
                  }
               }

               Vfx.impactFrame(level, this.getEntityPos(), 64.0, 16777215, 1.0F);
               Vfx.nova(level, c.add(0.0, 1.0, 0.0), HOLY, 30.0);
               Vfx.heavyImpact(level, c, S, 3.0);
               Vfx.pillar(level, c, HOLY, 40.0, 5.0);
            }
         })
         .end((b, level, t, i) -> {
            this.lightWard = false;
            this.setLook(3);
            this.lockout("last_light");
         });
   }

   private BossAction fadingLight() {
      return BossAction.of("fading_light", 3.0, 0.5).range(0.0, 64.0).cooldown(20).weight(0).phase(3).anyTarget().tags("aoe").start((b, level, t, i) -> {
         this.setNoGravity(false);
         this.buff(60, 1.5F, 1.0F);
         this.fadeAt = floor(level, this.getEntityPos());
         Vec3d m = this.mid();
         Vfx.shards(level, m, S.core(), 26, 0.7);
         Vfx.shards(level, m, HOLY.glow(), 14, 0.5);
         Vfx.flare(level, m, 16777215, 3.0, 5);
         Vfx.debris(level, m, Vfx.Debris.FEATHER, 16776176, 20, 0.5);
      }).tick((b, level, t, tick) -> {
         if (tick == 1) {
            Vfx.telegraphCircle(level, this.fadeAt, 4.0, 9, S);
         }

         if (tick <= 10) {
            this.setVelocity(0.0, Math.min(0.0, (this.fadeAt.y - this.getY()) / (11 - tick)), 0.0);
         }

         if (tick == 48) {
            this.setNoGravity(true);
         }
      }).hit((b, level, t, i) -> {
         this.blast(level, this.fadeAt, 4.0, 8.0F, 1.0, true);
         Vfx.heavyImpact(level, this.fadeAt, S, 0.8);
         Vfx.groundCrack(level, this.fadeAt, 4.0, S);
      }).end((b, level, t, i) -> this.setNoGravity(true));
   }

   private void cleave(ServerWorld level, @Nullable Vec3d at, float base) {
      Vec3d c = at != null ? at : floor(level, this.getEntityPos().add(this.facing().multiply(3.0)));

      for (LivingEntity e : this.foes(level, this.getEntityPos(), 7.0, 4.0, 8.0)) {
         if (flatDist(e.getEntityPos(), c) <= 2.5 + e.getWidth() * 0.5 && Math.abs(e.getY() - c.y) <= 3.0 || this.inArc(e, 4.5, 35.0)) {
            this.breakGuard(level, e, 100);
         }
      }

      Set<Integer> hit = new HashSet<>();
      this.blast(level, c, 2.5, base, 1.4, false, 3.0, hit);
      this.spearArc(level, 4.5, 35.0F, base, 1.4, null, hit, false);
      Vfx.slashArc(level, this.chest(), this.getYaw(), 20.0F, 90.0F, 5.0, S, 160.0F);
      Vfx.heavyImpact(level, c, S, 1.2);
      Vfx.groundCrack(level, c, 3.0, S);
      this.fissure(level, c, this.facing());
   }

   private void fissure(ServerWorld level, Vec3d from, Vec3d dir) {
      Vec3d d = flatDir(dir, Vec3d.fromPolar(0.0F, this.getYaw()));
      Vec3d end = from.add(d.multiply(9.0));
      Vfx.telegraphLine(level, from, d, 9.0, 2.2, 16, S);
      this.schedule(16, () -> {
         for (double s = 0.75; s <= 9.0; s += 1.5) {
            Vfx.pillar(level, floor(level, from.add(d.multiply(s))), S, 4.0, 0.8);
         }

         DamageSource src = this.getDamageSources().mobAttack(this);

         for (LivingEntity e : this.foesOn(level, from, end, 1.1, 1.5, 3.0)) {
            if (e.damage(level, src, this.dmg(14.0F))) {
               Vec3d v = e.getVelocity();
               e.setVelocity(v.x, 0.7, v.z);
               e.knockedBack = true;
            }
         }
      });
   }

   private void afterimage(ServerWorld level, Vec3d at) {
      Vfx.telegraphCircle(level, at, 2.0, 10, S);
      this.schedule(10, () -> {
         this.blast(level, at, 2.0, 10.0F, 0.6, false, 3.0, null);
         Vfx.impact(level, at.add(0.0, 0.5, 0.0), S, 0.7);
      });
   }

   private void fallingStar(ServerWorld level, Vec3d ground, int delay, double r, float base) {
      Vfx.telegraphCircle(level, ground, r, delay, S);
      Vec3d top = ground.add(2.5, 22.0, -2.5);
      Vec3d step = ground.subtract(top).multiply(0.125);

      for (int j = 0; j < 8; j++) {
         int at = delay - 8 + j;
         if (at >= 1) {
            Vec3d head = top.add(step.multiply(j));
            this.schedule(at, () -> Vfx.meteorTrail(level, head, step, S));
         }
      }

      this.schedule(delay, () -> {
         this.blast(level, ground, r, base, 1.0, false, 12.0, null);
         Vfx.heavyImpact(level, ground, S, 0.6);
         BossSounds.play(level, ground, "boss.astrael.starfall.hit", 2.5F, 1.1F);
      });
   }

   private void meteorWave(ServerWorld level, Vec3d c) {
      for (ServerPlayerEntity p : this.engaged()) {
         Vec3d at = floor(level, this.predict(p, 24.0));
         if (!sheltered(at, this.sanctuaries, 3.0)) {
            this.fallingStar(level, at, 24, 3.0, 18.0F);
         }
      }

      for (int k = 0; k < 2; k++) {
         for (int tries = 0; tries < 6; tries++) {
            Vec3d at = BossFx.around(level, this, c, 0.0, 10.0);
            if (!sheltered(at, this.sanctuaries, 3.0)) {
               this.fallingStar(level, at, 24, 3.0, 18.0F);
               break;
            }
         }
      }
   }

   private void addStars(ServerWorld level, Vec3d c, double radius, double rot) {
      for (int k = 0; k < 5; k++) {
         double a = rot + k * Math.PI * 2.0 / 5.0;
         this.conStars.add(floor(level, new Vec3d(c.x + Math.cos(a) * radius, c.y, c.z + Math.sin(a) * radius)));
      }
   }

   private Vec3d conEnd(int k) {
      int group = k / 5 * 5;
      return this.conStars.get(group + (k % 5 + 2) % 5);
   }

   private List<Vec3d> wells() {
      List<Vec3d> out = new ArrayList<>(2);
      if (this.well != null) {
         out.add(this.well);
      }

      if (this.well2 != null) {
         out.add(this.well2);
      }

      return out;
   }

   private void lockout(String self) {
      for (String a : ULTIMATES) {
         if (!a.equals(self)) {
            this.setCooldown(a, Math.max(this.cooldownLeft(a), 300));
         }
      }
   }

   private void spawnMinion(ServerWorld level, String kind, Vec3d at, @Nullable LivingEntity target) {
      EntityType<RealmMob> type = RealmMobs.type(kind);
      if (type != null) {
         RealmMob mob = (RealmMob)type.create(level, SpawnReason.MOB_SUMMONED);
         if (mob != null) {
            mob.refreshPositionAndAngles(at.x, at.y, at.z, this.getRandom().nextFloat() * 360.0F, 0.0F);
            LivingEntity aim = target != null ? target : this.getTarget();
            if (aim != null) {
               mob.setTarget(aim);
            }

            level.spawnEntity(mob);
            Vfx.pillar(level, at, S, 2.5, 0.7);
         }
      }
   }

   private void spawnAdds(ServerWorld level, int starlings, int sentinels) {
      if (this.isAlive()) {
         this.minionCheckedAt = -1000;
         int room = 6 - this.minionCount();

         for (int k = 0; k < sentinels + starlings && k < room; k++) {
            this.spawnMinion(level, k < sentinels ? "astral_sentinel" : "starling", BossFx.around(level, this, this.getEntityPos(), 3.0, 7.0), this.getTarget());
         }

         this.minionCheckedAt = -1000;
      }
   }

   private int minionCount() {
      if (this.age - this.minionCheckedAt >= 20) {
         this.minionCheckedAt = this.age;
         this.minionCache = this.getEntityWorld()
            .getEntitiesByClass(
               RealmMob.class, this.getBoundingBox().expand(32.0), m -> m.isAlive() && ("starling".equals(m.bossId()) || "astral_sentinel".equals(m.bossId()))
            )
            .size();
      }

      return this.minionCache;
   }

   private Vec3d facing() {
      return Vec3d.fromPolar(0.0F, this.getYaw());
   }

   private Vec3d right() {
      return Vec3d.fromPolar(0.0F, this.getYaw() + 90.0F);
   }

   private Vec3d mid() {
      return this.getEntityPos().add(0.0, 1.8, 0.0);
   }

   private Vec3d chest() {
      return this.getEntityPos().add(0.0, 2.5, 0.0);
   }

   private Vec3d head() {
      return this.getEntityPos().add(0.0, 3.15, 0.0);
   }

   private Vec3d core() {
      return this.getEntityPos().add(0.0, 2.45, 0.0).add(this.facing().multiply(0.25));
   }

   private Vec3d halo() {
      return this.getEntityPos().add(0.0, 3.4, 0.0).subtract(this.facing().multiply(0.4));
   }

   private Vec3d tip() {
      return this.getEntityPos().add(0.0, 3.3, 0.0).add(this.facing().multiply(1.2)).add(this.right().multiply(0.4));
   }

   private Vec3d lanceHead() {
      return this.getEntityPos().add(0.0, 3.2, 0.0).add(this.right().multiply(0.7));
   }

   private Vec3d volleyFrom() {
      return this.getEntityPos().add(0.0, 2.6, 0.0).subtract(this.right().multiply(0.8));
   }

   private double volleySpeed() {
      return this.phase() >= 3 ? 1.9 : 1.6;
   }

   private Vec3d bladeFrom() {
      return this.getEntityPos().add(0.0, 3.6, 0.0).subtract(this.facing().multiply(0.4));
   }

   private static Vec3d floor(ServerWorld level, Vec3d at) {
      Vec3d g = BossFx.ground(level, at.x, at.y, at.z);
      return g != null ? g : at;
   }

   private void turnTo(Vec3d at) {
      double dx = at.x - this.getX();
      double dz = at.z - this.getZ();
      if (!(dx * dx + dz * dz < 1.0E-4)) {
         float yaw = (float)(MathHelper.atan2(dz, dx) * 180.0F / (float)Math.PI) - 90.0F;
         this.setYaw(yaw);
         this.setBodyYaw(yaw);
         this.setHeadYaw(yaw);
      }
   }

   private void sink(ServerWorld level, @Nullable LivingEntity t, double k) {
      double want = (t != null ? t.getY() : floor(level, this.getEntityPos()).y) + 0.3;
      Vec3d v = this.getVelocity();
      this.setVelocity(v.x, MathHelper.clamp((want - this.getY()) * k, -0.6, 0.6), v.z);
   }

   private void rise(double dy) {
      Vec3d v = this.getVelocity();
      this.setVelocity(v.x, dy, v.z);
   }

   private boolean isCurrent(String anim) {
      BossAction a = this.currentMove();
      return a != null && a.anim().equals(anim);
   }

   private boolean transitioning() {
      BossAction a = this.currentMove();
      return a != null && a.anim().startsWith("phase_");
   }

   private static boolean healing(LivingEntity e) {
      if (!e.isUsingItem()) {
         return false;
      }

      UseAction use = e.getActiveItem().getUseAction();
      return use == UseAction.EAT || use == UseAction.DRINK;
   }

   private @Nullable ServerPlayerEntity otherPlayer(@Nullable LivingEntity not) {
      List<ServerPlayerEntity> list = this.engaged();
      int n = list.size();
      if (n == 0) {
         return null;
      }

      int start = this.getRandom().nextInt(n);

      for (int k = 0; k < n; k++) {
         ServerPlayerEntity p = list.get((start + k) % n);
         if (p != not) {
            return p;
         }
      }

      return null;
   }

   private static double flatDist(Vec3d a, Vec3d b) {
      double dx = a.x - b.x;
      double dz = a.z - b.z;
      return Math.sqrt(dx * dx + dz * dz);
   }

   private static Vec3d flatDir(Vec3d v, Vec3d fallback) {
      Vec3d h = new Vec3d(v.x, 0.0, v.z);
      return h.lengthSquared() > 1.0E-6 ? h.normalize() : fallback;
   }

   private static double segT(Vec3d p, Vec3d a, Vec3d b) {
      double dx = b.x - a.x;
      double dz = b.z - a.z;
      double len2 = dx * dx + dz * dz;
      return len2 < 1.0E-6 ? 0.0 : MathHelper.clamp(((p.x - a.x) * dx + (p.z - a.z) * dz) / len2, 0.0, 1.0);
   }

   private static double segDist(Vec3d p, Vec3d a, Vec3d b) {
      double t = segT(p, a, b);
      double x = a.x + (b.x - a.x) * t - p.x;
      double z = a.z + (b.z - a.z) * t - p.z;
      return Math.sqrt(x * x + z * z);
   }

   private static boolean sheltered(Vec3d p, List<Vec3d> spots, double r) {
      for (Vec3d s : spots) {
         if (flatDist(p, s) <= r) {
            return true;
         }
      }

      return false;
   }

   private List<LivingEntity> foes(ServerWorld level, Vec3d c, double r, double down, double up) {
      Box box = new Box(c.x - r - 1.0, c.y - down, c.z - r - 1.0, c.x + r + 1.0, c.y + up, c.z + r + 1.0);
      return level.getEntitiesByClass(
         LivingEntity.class,
         box,
         e -> e != this
            && e.isAlive()
            && !(e instanceof MysticalBoss)
            && !(e instanceof PlayerEntity p && (p.isCreative() || p.isSpectator()))
            && flatDist(e.getEntityPos(), c) <= r + e.getWidth() * 0.5
      );
   }

   private List<LivingEntity> foesOn(ServerWorld level, Vec3d a, Vec3d b, double half, double below, double above) {
      Vec3d m = a.add(b).multiply(0.5);
      double dy = Math.abs(b.y - a.y) * 0.5;
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : this.foes(level, m, flatDist(a, b) * 0.5 + half, below + dy + 1.0, above + dy + 1.0)) {
         double t = segT(e.getEntityPos(), a, b);
         double h = e.getY() - (a.y + (b.y - a.y) * t);
         if (h >= -below && h <= above && segDist(e.getEntityPos(), a, b) <= half + e.getWidth() * 0.5) {
            out.add(e);
         }
      }

      return out;
   }

   private boolean inArc(LivingEntity e, double reach, double halfAngle) {
      double dx = e.getX() - this.getX();
      double dz = e.getZ() - this.getZ();
      double d = Math.sqrt(dx * dx + dz * dz);
      if (d > reach + e.getWidth() * 0.5 || Math.abs(e.getY() - this.getY()) > 3.5) {
         return false;
      } else if (!(d < 1.0) && !(halfAngle >= 180.0)) {
         Vec3d f = this.facing();
         return Math.toDegrees(Math.acos(MathHelper.clamp((dx * f.x + dz * f.z) / d, -1.0, 1.0))) <= halfAngle;
      } else {
         return true;
      }
   }

   private List<LivingEntity> spearArc(
      ServerWorld level, double reach, float halfAngle, float base, double push, @Nullable Vec3d shove, @Nullable Set<Integer> hit, boolean pierce
   ) {
      List<LivingEntity> out = new ArrayList<>();
      DamageSource src = this.getDamageSources().mobAttack(this);
      Vec3d f = this.facing();

      for (LivingEntity e : this.foes(level, this.getEntityPos(), reach, 3.5, 7.5)) {
         if (this.inArc(e, reach, halfAngle) && (hit == null || hit.add(e.getId()))) {
            if (pierce) {
               e.timeUntilRegen = 0;
            }

            if (e.damage(level, src, this.dmg(base))) {
               Vec3d dir = shove != null ? shove : flatDir(e.getEntityPos().subtract(this.getEntityPos()), f);
               e.addVelocity(dir.x * push, 0.25, dir.z * push);
               e.knockedBack = true;
               Vfx.impact(level, e.getEntityPos().add(0.0, e.getHeight() * 0.5, 0.0), S, 0.6);
               out.add(e);
            }
         }
      }

      return out;
   }

   private void spearLine(ServerWorld level, Vec3d a, Vec3d b, double radius, float base, double push) {
      DamageSource src = this.getDamageSources().mobAttack(this);
      Vec3d dir = flatDir(b.subtract(a), this.facing());

      for (LivingEntity e : this.foesOn(level, a, b, radius, 3.5, 2.0)) {
         if (e.damage(level, src, this.dmg(base))) {
            e.addVelocity(dir.x * push, 0.25, dir.z * push);
            e.knockedBack = true;
            Vfx.impact(level, e.getEntityPos().add(0.0, e.getHeight() * 0.5, 0.0), S, 0.6);
         }
      }
   }

   private void blast(ServerWorld level, Vec3d c, double r, float base, double push, boolean falloff) {
      this.blast(level, c, r, base, push, falloff, 3.5, null);
   }

   private void blast(ServerWorld level, Vec3d c, double r, float base, double push, boolean falloff, double up, @Nullable Set<Integer> hit) {
      DamageSource src = this.getDamageSources().mobAttack(this);
      Vec3d f = this.facing();

      for (LivingEntity e : this.foes(level, c, r, 1.5, up)) {
         if (hit == null || hit.add(e.getId())) {
            double k = falloff ? MathHelper.clamp(1.0 - flatDist(e.getEntityPos(), c) / r, 0.0, 1.0) : 1.0;
            float amount = this.dmg(base) * (float)(falloff ? 0.4 + 0.6 * k : 1.0);
            if (e.damage(level, src, amount)) {
               Vec3d away = flatDir(e.getEntityPos().subtract(c), f);
               double s = falloff ? 0.5 + k : 1.0;
               e.addVelocity(away.x * push * s, 0.3 + 0.3 * k, away.z * push * s);
               e.knockedBack = true;
            }
         }
      }
   }

   private void wave(ServerWorld level, Vec3d c, double maxR, int ticks, float base, double push) {
      Set<Integer> hit = new HashSet<>();
      Vfx.ring(level, c, S.glow(), maxR, (int)(ticks * 1.3));

      for (int t = 1; t <= ticks; t++) {
         double r = maxR * t / ticks;
         boolean draw = t % 2 == 0;
         this.schedule(t, () -> {
            if (draw) {
               Vfx.ring(level, c, S.core(), r, 4);
            }

            DamageSource src = this.getDamageSources().mobAttack(this);

            for (LivingEntity e : this.foes(level, c, r + 1.0, 1.0, 2.0)) {
               if (Math.abs(flatDist(e.getEntityPos(), c) - r) < 1.0 && e.isOnGround() && hit.add(e.getId()) && e.damage(level, src, this.dmg(base))) {
                  Vec3d away = flatDir(e.getEntityPos().subtract(c), this.facing());
                  e.addVelocity(away.x * push, 0.4, away.z * push);
                  e.knockedBack = true;
               }
            }
         });
      }
   }

   private void zone(ServerWorld level, Vec3d c, double r, int ticks, float perSecond, @Nullable StatusEffectInstance effect) {
      for (int t = 10; t < ticks; t += 20) {
         this.schedule(t, () -> {
            Vfx.embers(level, c.add(0.0, 0.3, 0.0), 16767088, 6, r * 0.7);
            DamageSource src = this.getDamageSources().indirectMagic(this, this);

            for (LivingEntity e : this.foes(level, c, r, 1.0, 3.0)) {
               e.damage(level, src, this.dmg(perSecond));
               if (effect != null) {
                  e.addStatusEffect(new StatusEffectInstance(effect), this);
               }
            }
         });
      }
   }

   private void breakGuard(ServerWorld level, LivingEntity e, int ticks) {
      ItemStack s = e.getBlockingItem();
      if (s != null && e instanceof PlayerEntity p) {
         p.getItemCooldownManager().set(s, ticks);
         p.clearActiveItem();
         Vec3d m = e.getEntityPos().add(0.0, e.getHeight() * 0.5, 0.0);
         Vfx.shards(level, m, 16774870, 14, 0.5);
         Vfx.glint(level, m, 16777215, 1.4);
      }
   }

   private void pullToward(LivingEntity e, Vec3d c, double radius, double strength) {
      double dx = c.x - e.getX();
      double dz = c.z - e.getZ();
      double d = Math.sqrt(dx * dx + dz * dz);
      if (d <= radius && d > 0.8) {
         this.pulls.merge(e, new Vec3d(dx / d * strength, 0.0, dz / d * strength), Vec3d::add);
         this.pulledNow.add(e);
      }
   }

   private void flushPulls(ServerWorld level) {
      if (!this.pulls.isEmpty()) {
         this.pulls.keySet().retainAll(this.pulledNow);
         this.pulledNow.clear();

         for (Entry<LivingEntity, Vec3d> p : this.pulls.entrySet()) {
            if (p.getValue().lengthSquared() >= 0.1296 && p.getKey().isAlive()) {
               this.tug(level, p.getKey(), p.getValue());
               p.setValue(Vec3d.ZERO);
            }
         }
      }
   }

   private void tug(ServerWorld level, LivingEntity e, Vec3d kick) {
      double x = kick.x;
      double z = kick.z;
      if (e instanceof ServerPlayerEntity sp) {
         if (sp.hasVehicle() || sp.isSpectator() || sp.getAbilities().flying) {
            return;
         }

         boolean ground = sp.isOnGround();
         boolean water = sp.isTouchingWater();
         double s = ground ? 1.0 : (water ? 0.6 : 0.4);
         x *= s;
         z *= s;
         if (sp.knockedBack) {
            sp.setVelocity(sp.getVelocity().add(x, 0.0, z));
            return;
         }

         Vec3d k = sp.getMovement();
         double f = sp.isGliding() ? 0.99 : (water ? 0.8 : (ground ? 0.546 : 0.91));
         double y = ground ? -0.0784 : (sp.isGliding() ? k.y * 0.98 : (water ? k.y * 0.8 : (k.y - 0.08) * 0.98));
         sp.setVelocity(k.x * f + x, y, k.z * f + z);
         Vfx.streaks(level, sp.getEntityPos().add(0.0, 1.0, 0.0), new Vec3d(x, 0.0, z).normalize().multiply(1.6), VOID, 3);
      } else {
         e.addVelocity(x, 0.0, z);
      }

      e.knockedBack = true;
   }

   @Override
   public boolean damage(ServerWorld world, DamageSource source, float amount) {
      if (this.aegisUntil > this.age
         && this.shield() > 0.0F
         && this.age >= this.reflectReady
         && source.getSource() instanceof ProjectileEntity
         && source.getAttacker() instanceof LivingEntity shooter
         && shooter != this
         && shooter.isAlive()) {
         this.reflectReady = this.age + 10;
         Vec3d m = this.mid();
         Vec3d dir = shooter.getEyePos().subtract(m).normalize();
         BossBolt.shoot(world, this, BossBolt.Style.STAR, m.add(dir), dir.multiply(1.4), this.dmg(8.0F));
         Vfx.glint(world, m.add(dir), 16777215, 1.6);
      }

      return super.damage(world, source, amount);
   }

   @Override
   protected void mobTick(ServerWorld world) {
      super.mobTick(world);
      if (this.isAlive() && !this.inIntro()) {
         if (!this.hasNoGravity() && !this.staggered() && !this.isCurrent("fading_light")) {
            this.setNoGravity(true);
         }

         if (this.pendingLight && !this.busy()) {
            this.pendingLight = false;
            this.forceAction(world, "last_light");
         }

         if (this.isCurrent("phase_3") && this.actionTick() <= 24) {
            this.rise(0.07);
         } else if (this.isCurrent("stagger") && this.actionTick() <= 18) {
            Vec3d v = this.getVelocity();
            this.setVelocity(v.x, Math.min(0.0, (floor(world, this.getEntityPos()).y - this.getY()) / (19 - this.actionTick())), v.z);
         }

         if (this.age % 20 == 0 && !this.busy() && this.look() < this.phase()) {
            this.setLook(this.phase());
         }

         this.tickAegis(world);
         if (this.getTarget() != null) {
            if (!this.transitioning()) {
               this.hazards(world);
            }

            this.markHealer();
         }

         int every = this.enraged() ? 5 : 10;
         if (this.age % every == 0) {
            Vfx.embers(world, this.mid(), this.phase() >= 3 ? VOID.glow() : S.core(), 3, 1.0);
         }

         if (this.age % 30 == 0) {
            Vfx.glint(world, this.halo(), 16777215, 1.0);
         }

         this.flushPulls(world);
      } else {
         this.pulls.clear();
         this.pulledNow.clear();
      }
   }

   private void tickAegis(ServerWorld level) {
      if (this.aegisUntil >= 0) {
         if (!(this.shield() <= 0.0F) && this.aegisUntil > this.age) {
            int left = this.aegisUntil - this.age;
            if (left == 20) {
               this.aegisNovaAt = floor(level, this.getEntityPos());
               Vfx.telegraphCircle(level, this.aegisNovaAt, 6.0, 19, S);
            } else if (left == 1) {
               Vec3d c = this.aegisNovaAt != null ? this.aegisNovaAt : floor(level, this.getEntityPos());
               this.blast(level, c, 6.0, 18.0F, 1.8, true);
               Vfx.nova(level, c.add(0.0, 1.0, 0.0), S, 6.0);
               BossSounds.play(level, c, "boss.astrael.radiance.hit", 3.0F, 1.2F);
               this.aegisUntil = -1;
            }

            if (left % 20 == 0) {
               Vfx.shieldBubble(level, this.mid(), 2.6, S, 21);
            }
         } else {
            this.aegisUntil = -1;
         }
      }
   }

   private void hazards(ServerWorld level) {
      int p = this.phase();
      boolean ultimate = this.isCurrent("supernova") || this.isCurrent("singularity") || this.isCurrent("last_light");
      if (p >= 1) {
         if (this.nodes.isEmpty()) {
            this.buildNodes(level);
         }

         if (this.age % 40 == 0) {
            for (Vec3d n : this.nodes) {
               Vfx.glint(level, n.add(0.0, 1.2, 0.0), 16774870, 1.2);
               Vfx.wisps(level, n.add(0.0, 1.0, 0.0), S, 3, 0.6, 40);
            }
         }

         if (!ultimate && --this.nodeTimer <= 0) {
            this.nodeTimer = (int)((p >= 3 ? 100 : (p == 2 ? 120 : 160)) * (this.enraged() ? 0.7F : 1.0F));
            this.nodeBeam(level);
         }
      }

      if (p >= 2 && !ultimate && --this.starTimer <= 0) {
         this.starTimer = (int)((p >= 3 ? 80 : 110) * (this.enraged() ? 0.6F : 1.0F));
         List<ServerPlayerEntity> list = new ArrayList<>(this.engaged());

         for (int n = 0; n < 3 && !list.isEmpty(); n++) {
            ServerPlayerEntity pl = list.remove(this.getRandom().nextInt(list.size()));
            this.fallingStar(level, floor(level, this.predict(pl, 24.0)), 24, 2.5, 14.0F);
         }
      }

      if (p >= 3) {
         this.eventHorizon(level);
      }
   }

   private void buildNodes(ServerWorld level) {
      this.nodes.clear();
      BlockPos home = this.home();
      Vec3d c = home != null ? Vec3d.ofBottomCenter(home) : this.getEntityPos();
      double rot = this.getRandom().nextDouble() * Math.PI * 2.0;

      for (int k = 0; k < 5; k++) {
         double a = rot + k * Math.PI * 2.0 / 5.0;
         this.nodes.add(floor(level, new Vec3d(c.x + Math.cos(a) * 11.0, c.y, c.z + Math.sin(a) * 11.0)));
      }
   }

   private void nodeBeam(ServerWorld level) {
      int k = this.getRandom().nextInt(5);
      int j = this.phase() >= 2 && this.getRandom().nextBoolean() ? (k + 2) % 5 : (k + 1) % 5;
      Vec3d a = this.nodes.get(k);
      Vec3d b = this.nodes.get(j);
      Vfx.telegraphLine(level, a, b.subtract(a), flatDist(a, b), 1.6, 24, S);
      this.schedule(24, () -> {
         Vfx.beam(level, a.add(0.0, 1.0, 0.0), b.add(0.0, 1.0, 0.0), S, 0.6, 8);
         DamageSource src = this.getDamageSources().indirectMagic(this, this);
         Vec3d side = new Vec3d(-(b.z - a.z), 0.0, b.x - a.x);

         for (LivingEntity e : this.foesOn(level, a, b, 0.9, 0.5, 3.0)) {
            e.damage(level, src, this.dmg(10.0F));
            Vec3d on = a.add(b.subtract(a).multiply(segT(e.getEntityPos(), a, b)));
            Vec3d off = flatDir(e.getEntityPos().subtract(on), flatDir(side, Vec3d.ZERO));
            e.addVelocity(off.x * 0.8, 0.2, off.z * 0.8);
            e.knockedBack = true;
         }

         BossSounds.play(level, a.add(b).multiply(0.5), "boss.astrael.constellation.hit", 2.0F, 1.0F);
      });
   }

   private void eventHorizon(ServerWorld level) {
      boolean pull = !this.isCurrent("fading_light") && !this.staggered();
      boolean pulse = this.age % 20 == 0;
      if (pull || pulse) {
         Vec3d c = floor(level, this.getEntityPos());
         if (pull) {
            for (ServerPlayerEntity p : this.engaged()) {
               this.pullToward(p, c, 22.0, 0.03);
            }
         }

         if (pulse) {
            Vfx.auraRing(level, c, VOID, 3.5);
            DamageSource src = this.getDamageSources().indirectMagic(this, this);

            for (LivingEntity e : this.foes(level, c, 3.5, 1.0, 4.0)) {
               e.damage(level, src, this.dmg(4.0F));
            }

            if (this.age % 40 == 0) {
               BossSounds.play(level, c, "boss.astrael.gravity_well.hit", 0.6F, 0.6F);
            }
         }
      }
   }

   private void markHealer() {
      if (this.phase() >= 1 && this.age % 20 == 0 && this.age >= this.healerMarkReady) {
         LivingEntity t = this.getTarget();

         for (ServerPlayerEntity p : this.engaged()) {
            if (p != t && p.squaredDistanceTo(this) <= 784.0 && healing(p)) {
               this.markTarget(p, 100);
               this.healerMarkReady = this.age + 200;
               return;
            }
         }
      }
   }

   @Override
   protected void onPhaseChange(ServerWorld level, int phase) {
      Vfx.Palette p = phase >= 3 ? VOID : (phase == 2 ? HOLY : S);
      Vfx.telegraphCircle(level, floor(level, this.getEntityPos()), 8 + phase, 27, p);
      Vfx.chargeUp(level, this.core(), p, 27);
      if (phase >= 2) {
         this.bar.setDarkenSky(true);
      }

      if (phase >= 3) {
         this.bar.setThickenFog(true);
      }

      int starlings = phase >= 3 ? 0 : 2;
      int sentinels = phase >= 3 ? 2 : (phase == 2 ? 1 : 0);
      this.schedule(40, () -> this.spawnAdds(level, starlings, sentinels));
   }

   @Override
   protected void phaseBurst(ServerWorld level, int phase) {
      Vec3d g = floor(level, this.getEntityPos());
      Vec3d m = this.mid();
      this.setLook(Math.min(3, phase));
      if (phase >= 1 && this.nodes.isEmpty()) {
         this.buildNodes(level);

         for (Vec3d n : this.nodes) {
            Vfx.pillar(level, n, S, 8.0, 0.8);
         }
      }

      if (phase <= 1) {
         Vfx.nova(level, m, S, 10.0);
         Vfx.heavyImpact(level, g, S, 1.5);
         Vfx.shards(level, this.halo(), 16767088, 24, 0.6);
         Vfx.debris(level, m, Vfx.Debris.FEATHER, 16776176, 20, 0.5);
         Vfx.shockwave(level, g, S, 8.0);
         this.blast(level, g, 8.0, 10.0F, 1.6, true);
      } else if (phase == 2) {
         Vfx.nova(level, m, HOLY, 14.0);
         Vfx.impactFrame(level, this.getEntityPos(), 48.0, 16777215, 0.8F);
         Vfx.shards(level, this.chest(), 15657182, 30, 0.7);
         Vfx.shards(level, this.head(), 14595156, 10, 0.5);
         Vfx.shockwave(level, g, HOLY, 9.0);
         Vfx.runeCircle(level, g, 12.0, S, 60, 0.05F, Vfx.Rune.SOLAR);
         this.blast(level, g, 9.0, 14.0F, 2.0, true);
      } else {
         Vfx.nova(level, m, VOID, 16.0);
         Vfx.impactFrame(level, this.getEntityPos(), 48.0, 11550975, 1.0F);
         Vfx.vortex(level, g, 12.0, 6.0, VOID, 60);
         Vfx.shockwave(level, g, VOID, 10.0);
         this.blast(level, g, 10.0, 16.0F, 2.2, true);
      }
   }

   @Override
   protected void onStagger(ServerWorld level) {
      this.setNoGravity(false);
      this.schedule(58, () -> {
         if (this.isAlive() && !this.isCurrent("fading_light")) {
            this.setNoGravity(true);
         }
      });
      this.aegisUntil = -1;
      Vec3d h = this.halo();
      Vfx.shards(level, h, 16767088, 12, 0.5);
      Vfx.glint(level, h, 16777215, 1.6);
   }

   @Override
   protected void onEnrage(ServerWorld level) {
      Vfx.nova(level, this.mid(), HOLY, 12.0);
      Vfx.auraRing(level, floor(level, this.getEntityPos()), S, 3.0);
      this.nodeTimer = Math.min(this.nodeTimer, 40);
      this.starTimer = Math.min(this.starTimer, 40);
   }

   @Override
   protected void desperation(ServerWorld level) {
      if (!this.transitioning() && !this.isCurrent("stagger")) {
         this.forceAction(level, "last_light");
      } else {
         this.pendingLight = true;
      }
   }

   @Override
   protected void onIntroTick(ServerWorld level, int left) {
      int t = this.introTicks() - 1 - left;
      Vec3d pos = this.getEntityPos();
      Vec3d g = floor(level, pos);
      if (t < 30) {
         double s = t / 30.0;
         Vfx.meteorTrail(level, pos.add(0.0, 1.5 + 15.0 * (1.0 - s * s), 0.0), new Vec3d(0.0, -Math.max(0.4, s), 0.0), S);
      } else if (t == 30) {
         Vfx.heavyImpact(level, g, S, 2.0);
         Vfx.nova(level, g.add(0.0, 1.0, 0.0), S, 8.0);
         Vfx.groundCrack(level, g, 5.0, S);
         BossFx.knockback(level, this, pos, 6.0, 1.4);
         Vfx.radialFlash(level, pos, 40.0, 16777215, 0.5F);
      } else if (t <= 60) {
         if (t % 4 == 0) {
            Vfx.embers(level, pos.add(0.0, 0.6, 0.0), S.core(), 4, 1.2);
         }

         if (t == 32) {
            Vfx.runeCircle(level, g, 5.0, S, 60, 0.02F, Vfx.Rune.ASTRAL);
         } else if (t == 60) {
            Vfx.debris(level, this.mid(), Vfx.Debris.FEATHER, 16776176, 24, 0.5);

            for (int k = 0; k < 8; k++) {
               Vec3d d = Vec3d.fromPolar(0.0F, k * 45);
               Vfx.streaks(level, this.mid().add(d.multiply(1.5)), d, S, 4);
            }
         }
      } else if (t >= 84 && t <= 105 && (t - 84) % 3 == 0) {
         double a = Math.toRadians((t - 84) / 3 * 45);
         Vec3d point = this.halo().add(this.right().multiply(Math.cos(a) * 0.55)).add(0.0, Math.sin(a) * 0.55, 0.0);
         Vfx.glint(level, point, 16767088, 0.7);
      } else if (t >= 108 && t <= 118) {
         Vec3d hand = pos.add(0.0, 1.9, 0.0).add(this.right().multiply(0.5));
         if (t % 2 == 0) {
            Vfx.gather(level, hand, S, 1.5, 10);
         }

         if (t == 118) {
            Vfx.flare(level, hand, S.core(), 2.0, 6);
         }
      } else if (t == 124) {
         Vfx.flare(level, this.tip(), S.core(), 2.5, 8);
         Vfx.radialFlash(level, pos, 40.0, S.glow(), 0.4F);
      }
   }

   @Override
   protected void onDeathTick(ServerWorld level, int tick) {
      Vec3d pos = this.getEntityPos();
      if (tick == 1) {
         this.deathFloor = floor(level, pos).y;
         Vfx.flare(level, this.core(), 16777215, 2.5, 8);
      }

      if (tick >= 12 && tick < 32) {
         this.setVelocity(0.0, Math.min(0.0, (this.deathFloor - this.getY()) / (32 - tick)), 0.0);
      } else if (tick == 32) {
         this.setVelocity(Vec3d.ZERO);
         this.setNoGravity(false);
         Vfx.heavyImpact(level, floor(level, pos), S, 0.8);
      }

      if (tick <= 12 && tick % 3 == 0) {
         Vfx.sparks(level, this.core(), S.core(), 5, 0.3, 0.2);
      }

      if (tick >= 20 && tick <= 44 && tick % 4 == 0) {
         Vfx.shards(level, this.halo().add(0.0, -(tick - 20) * 0.04, 0.0), 16767088, 5, 0.15);
      }

      if (tick >= 48 && tick <= 60 && tick % 3 == 0) {
         double s = (tick - 48) / 12.0;
         Vfx.glint(level, pos.add(this.right().multiply(0.45)).add(this.facing().multiply(0.4 + s * 1.4)).add(0.0, 1.4 - s * 1.2, 0.0), 16777215, 0.8);
      }

      if (tick == 72) {
         Vfx.chargeUp(level, this.core(), S, 30);
      }

      if (tick >= 100 && tick <= 128 && (tick - 100) % 6 == 0) {
         int k = tick - 100;
         Vfx.pillar(level, floor(level, pos), S, 4.0 + k * 0.6, 0.6 + k * 0.03);
         Vfx.debris(level, this.mid(), Vfx.Debris.FEATHER, 16776176, 6, 0.25);
         Vfx.wisps(level, this.mid(), S, 4, 1.2, 30);
      }

      if (tick == 136) {
         Vfx.flare(level, pos.add(0.0, 6.0, 0.0), 16777215, 3.0, 20);
         Vfx.nova(level, this.mid(), S, 10.0);
         Vfx.radialFlash(level, pos, 48.0, 16777215, 0.6F);
         this.bar.setDarkenSky(false);
         this.bar.setThickenFog(false);
      }
   }

   static {
      for (int i = 0; i < 5; i++) {
         LOOKS[i] = RawAnimation.begin().thenLoop("animation.astrael.look_" + i);
      }
   }

   private static final class Sentence {
      final LivingEntity who;
      final boolean healing;
      @Nullable Vec3d spot;

      Sentence(LivingEntity who, boolean healing) {
         this.who = who;
         this.healing = healing;
      }
   }
}
