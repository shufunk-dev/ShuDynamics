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
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.Map.Entry;
import org.jspecify.annotations.Nullable;
import com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;

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
   private static final EntityDataAccessor<Integer> LOOK = SynchedEntityData.defineId(AstraelBoss.class, EntityDataSerializers.INT);
   private static final RawAnimation[] LOOKS = new RawAnimation[5];
   public static final float WIDTH = 1.2F;
   public static final float HEIGHT = 3.6F;
   public static final boolean FIRE_IMMUNE = false;
   private final List<Vec3> nodes = new ArrayList<>();
   private int nodeTimer = 100;
   private int starTimer = 80;
   private int healerMarkReady;
   private int minionCache;
   private int minionCheckedAt = -1000;
   private int aegisUntil = -1;
   private int reflectReady;
   private @Nullable Vec3 aegisNovaAt;
   private boolean lightWard;
   private boolean pendingLight;
   private Vec3 lightCenter = Vec3.ZERO;
   private final List<Vec3> sanctuaries = new ArrayList<>();
   private Vec3 lanceFrom = Vec3.ZERO;
   private @Nullable Vec3 cleaveAt;
   private @Nullable Vec3 plungeAt;
   private @Nullable Vec3 diveAt;
   private boolean diveAir;
   private final Vec3[] volleyAims = new Vec3[3];
   private Vec3 volleyLead = Vec3.ZERO;
   private @Nullable Vec3 meteorAt;
   private int meteorTravel;
   private final List<AstraelBoss.Sentence> sentences = new ArrayList<>();
   private @Nullable Vec3 well;
   private @Nullable Vec3 well2;
   private final List<Vec3> conStars = new ArrayList<>();
   private final List<Vec3> choirSpots = new ArrayList<>();
   private final List<Vec3> shelters = new ArrayList<>();
   private Vec3 singAt = Vec3.ZERO;
   private float singTheta;
   private Vec3 radianceAt = Vec3.ZERO;
   private Vec3 fadeAt = Vec3.ZERO;
   private double deathFloor;
   private static final double TUG = 0.36;
   private final Map<LivingEntity, Vec3> pulls = new HashMap<>();
   private final Set<LivingEntity> pulledNow = new HashSet<>();

   public static Builder attributes() {
      return MysticalBoss.bossAttributes(960.0, 14.0, 0.34, 14.0);
   }

   public AstraelBoss(EntityType<? extends Monster> type, Level level) {
      super(type, level, "astrael", BossBarColor.WHITE);
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
   protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(LOOK, 0);
   }

   private int look() {
      return (Integer)this.entityData.get(LOOK);
   }

   private void setLook(int n) {
      this.entityData.set(LOOK, Mth.clamp(n, 0, 4));
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
      super.registerControllers(controllers);
      controllers.add(new AnimationController("look", 0, test -> {
         test.controller().setTransitionTicks(test.controller().getCurrentRawAnimation() != null ? 6 : 0);
         return test.setAndContinue(LOOKS[Mth.clamp(this.look(), 0, 4)]);
      }));
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput view) {
      super.addAdditionalSaveData(view);
      view.putInt("AstraelLook", this.look());
   }

   @Override
   protected void readAdditionalSaveData(ValueInput view) {
      super.readAdditionalSaveData(view);
      this.setLook(Math.min(3, view.getIntOr("AstraelLook", 0)));
      if (this.phase() >= 2) {
         this.bar.setDarkenScreen(true);
      }

      if (this.phase() >= 3) {
         this.bar.setCreateWorldFog(true);
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

               Vec3 f = this.facing();
               this.setDeltaMovement(f.x * 0.75, this.getDeltaMovement().y, f.z * 0.75);
               this.lanceFrom = this.position();
               Vfx.telegraphLine(level, floor(level, this.position()), f, 10.0, 1.6, 5, S);
            }
         })
         .hit((b, level, t, i) -> {
            Vec3 f = this.facing();
            this.spearLine(level, this.lanceFrom.add(0.0, 1.8, 0.0), this.mid().add(f.scale(5.5)), 1.2, 18.0F, 1.0);
            Vec3 tip = this.tip();
            Vfx.beam(level, tip, tip.add(f.scale(5.5)), S, 0.35, 4);
            Vfx.streaks(level, this.mid(), f, S, 12);
            Vfx.glint(level, tip.add(f.scale(5.5)), 16777215, 1.6);
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
               Vfx.telegraphCone(level, floor(level, this.position()), this.getYRot(), 105.0F, 5.2, 7, S);
            }
         })
         .hit((b, level, t, i) -> {
            this.spearArc(level, 5.2, 105.0F, 16.0F, 1.2, this.right(), null, false);
            Vfx.slashArc(level, this.chest(), this.getYRot(), 0.0F, 0.0F, 5.2, S, -210.0F);
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
               this.cleaveAt = floor(level, this.position().add(this.facing().scale(3.0)));
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
               Vfx.telegraphCircle(level, floor(level, this.position().add(this.facing().scale(3.0))), 2.5, 10, S);
            } else if (tick == 16) {
               Vfx.glint(level, this.tip(), 16777215, 1.8);
               Vfx.flare(level, this.tip(), 16767088, 1.6, 6);
            } else if (tick == 19) {
               this.cleaveAt = floor(level, this.position().add(this.facing().scale(3.0)));
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
               Vfx.telegraphCone(level, floor(level, this.position()), this.getYRot(), 40.0F, 4.0, 6, S);
            }
         })
         .hit((b, level, t, i) -> {
            DamageSource src = this.damageSources().mobAttack(this);
            Vec3 f = this.facing();

            for (LivingEntity e : this.foes(level, this.position(), 4.5, 3.5, 7.5)) {
               if (this.inArc(e, 4.0, 45.0)) {
                  this.breakGuard(level, e, 100);
                  if (e.hurtServer(level, src, this.dmg(15.0F))) {
                     Vec3 away = flatDir(e.position().subtract(this.position()), f);
                     e.push(away.x * 1.8, 0.4, away.z * 1.8);
                     e.syncVelocity = true;
                     e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1), this);
                  }
               }
            }

            Vec3 front = this.position().add(0.0, 1.6, 0.0).add(f.scale(1.6));
            Vfx.impact(level, front, S, 0.8);
            Vfx.shards(level, front, 16774870, 16, 0.5);
            Vfx.ring(level, front, S.glow(), 2.5, 8);
            Vfx.shake(level, this.position(), 16.0, 0.4, 6);
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
               Vfx.telegraphCircle(level, floor(level, this.position()), 5.2, 8, S);
            }
         })
         .hit((b, level, t, i) -> {
            this.spearArc(level, 5.2, 180.0F, 17.0F, 1.5, null, null, false);
            Vec3 c = this.chest();
            Vfx.slashArc(level, c, this.getYRot(), 0.0F, 0.0F, 5.2, S, 360.0F);
            Vfx.debris(level, c, Vfx.Debris.FEATHER, 16776176, 12, 0.4);

            for (int k = 0; k < 4; k++) {
               Vec3 out = Vec3.directionFromRotation(0.0F, this.getYRot() + k * 90);
               Vfx.streaks(level, c.add(out.scale(3.0)), Vec3.directionFromRotation(0.0F, this.getYRot() + k * 90 + 90.0F), S, 6);
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
               Vfx.telegraphCone(level, floor(level, this.position()), this.getYRot(), 60.0F, 5.0, 5, S);
            } else if (tick == 26) {
               this.plungeAt = floor(level, this.position().add(this.facing().scale(2.5)));
               Vfx.telegraphCircle(level, this.plungeAt, 3.0, 12, S);
            }
         })
         .hit((b, level, t, i) -> {
            Vec3 c = this.chest();
            float yaw = this.getYRot();
            if (i == 0) {
               this.spearArc(level, 5.0, 60.0F, 14.0F, 0.8, null, null, false);
               Vfx.slashArc(level, c, yaw, 0.0F, 40.0F, 5.0, S, 170.0F);
            } else if (i == 1) {
               this.spearArc(level, 5.0, 60.0F, 14.0F, 0.8, null, null, true);
               Vfx.slashArc(level, c, yaw, 0.0F, -40.0F, 5.0, S, -170.0F);
            } else if (i == 2) {
               for (LivingEntity e : this.spearArc(level, 5.0, 70.0F, 16.0F, 0.4, null, null, true)) {
                  Vec3 v = e.getDeltaMovement();
                  e.setDeltaMovement(v.x, Math.max(v.y, 0.9), v.z);
                  e.syncVelocity = true;
               }

               Vfx.slashArc(level, c, yaw, -10.0F, -90.0F, 5.0, S, 200.0F);
            } else {
               Vec3 p = this.plungeAt != null ? this.plungeAt : floor(level, this.position().add(this.facing().scale(2.5)));
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
               boolean free = level.noCollision(this, this.getBoundingBox().move(0.0, 0.85, 0.0));
               this.setDeltaMovement(0.0, free ? 0.85 : 0.0, 0.0);
               if (tick % 2 == 0) {
                  Vfx.trail(level, this.mid(), S, 1.0);
               }
            } else if (tick == 19) {
               this.diveAir = t != null && t == this.ctx().target && this.ctx().targetAirborne;
               Vec3 aim = t != null ? this.predict(t, 9.0) : this.position();
               this.diveAt = this.diveAir ? aim : floor(level, aim);
               this.turnTo(this.diveAt);
               Vfx.telegraphCircle(level, floor(level, this.diveAt), 4.5, 9, S);
               Vfx.chargeUp(level, this.mid(), S, 9);
            } else if (tick >= 20 && tick < 28 && this.diveAt != null) {
               Vec3 v = this.diveAt.subtract(this.position()).scale(1.0 / (28 - tick));
               this.setDeltaMovement(v);
               Vfx.meteorTrail(level, this.mid(), v, S);
               if (tick == 20) {
                  Vfx.dashTrail(level, this.mid(), this.diveAt.add(0.0, 1.8, 0.0), S);
               }
            } else if (tick == 28) {
               this.setDeltaMovement(Vec3.ZERO);
            } else if (tick >= 36 && tick <= 46) {
               this.rise(0.12);
            }
         })
         .hit((b, level, t, i) -> {
            Vec3 at = this.diveAt != null ? this.diveAt : this.position();
            Vec3 g = floor(level, at);
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
               this.setDeltaMovement(Vec3.ZERO);
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
               float ly = t.getYRot();
               Vec3 look = Vec3.directionFromRotation(0.0F, ly);
               Vec3 side = Vec3.directionFromRotation(0.0F, ly - 90.0F);

               Vec3 to = switch (i) {
                  case 0 -> t.position().add(side.scale(5.0));
                  case 1 -> t.position().subtract(side.scale(5.0));
                  default -> t.position().subtract(look.scale(2.8));
               };
               Vec3 from = this.position();
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
               Vec3 from = this.volleyFrom();
               Vec3 body = new Vec3(0.0, t.getBbHeight() * 0.5, 0.0);
               if (k == 0) {
                  this.volleyLead = this.predict(t, from.distanceTo(t.position()) / this.volleySpeed());
                  this.volleyAims[0] = this.volleyLead.add(body);
               } else if (k == 1) {
                  this.volleyAims[1] = t.position().add(body);
               } else {
                  this.volleyAims[2] = t.position().scale(2.0).subtract(this.volleyLead).add(body);
               }

               Vfx.glint(level, from, 16777215, 1.2);
               Vfx.beam(level, from, this.volleyAims[k], S, 0.08, 4);
            }
         })
         .hit((b, level, t, i) -> {
            Vec3 aim = this.volleyAims[i];
            if (aim == null) {
               if (t == null) {
                  return;
               }

               aim = t.getEyePosition();
            }

            Vec3 from = this.volleyFrom();
            Vec3 dir = aim.subtract(from).normalize();
            boolean dark = this.phase() >= 3;
            BossBolt.Style style = dark ? BossBolt.Style.VOID : BossBolt.Style.STAR;
            double speed = this.volleySpeed();
            BossBolt.shoot(level, this, style, from, dir.scale(speed), this.dmg(12.0F)).aoe(1.5).life(60);
            if (this.phase() >= 2) {
               float off = (float)Math.toRadians(8.0);
               BossBolt.shoot(level, this, style, from, dir.yRot(off).scale(speed), this.dmg(12.0F)).aoe(1.5).life(60);
               BossBolt.shoot(level, this, style, from, dir.yRot(-off).scale(speed), this.dmg(12.0F)).aoe(1.5).life(60);
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
            Vec3 from = this.bladeFrom();
            int n = this.phase() >= 1 ? 3 : 2;

            for (int k = 0; k < n; k++) {
               float off = n == 3 ? (k - 1) * 60.0F : (k == 0 ? -60.0F : 60.0F);
               Vec3 v = Vec3.directionFromRotation(0.0F, this.getYRot() + off).scale(0.45).add(0.0, 0.3, 0.0);
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
               Vec3 from = this.lanceHead();
               int travel = (int)Math.round(from.distanceTo(t.position()) / 2.4);
               Vec3 a = floor(level, this.predict(t, 10 + travel));
               travel = Math.max(1, (int)Math.round(from.distanceTo(a) / 2.4));
               this.meteorAt = a;
               this.meteorTravel = travel;
               Vec3 base = floor(level, this.position());
               Vfx.telegraphLine(level, base, a.subtract(base), flatDist(base, a) + 2.0, 1.4, 10, S);
               Vfx.telegraphCircle(level, a, 3.5, 10 + travel, S);
               Vfx.chargeUp(level, from, S, 10);
            }
         })
         .hit(
            (b, level, t, i) -> {
               Vec3 a = this.meteorAt;
               if (a != null) {
                  this.meteorAt = null;
                  Vec3 from = this.lanceHead();
                  BossBolt.shoot(level, this, BossBolt.Style.STAR, from, a.add(0.0, 0.3, 0.0).subtract(from).normalize().scale(2.4), this.dmg(24.0F))
                     .aoe(3.5)
                     .life(40);
                  Vfx.flare(level, from, S.core(), 2.2, 5);
                  boolean brand = this.phase() >= 1;
                  this.schedule(this.meteorTravel, () -> {
                     Vfx.heavyImpact(level, a, S, 1.0);
                     Vfx.groundCrack(level, a, 3.0, S);
                     if (brand) {
                        Vfx.runeCircle(level, a, 2.5, S, 60, 0.03F, Vfx.Rune.ASTRAL);
                        this.zone(level, a, 2.5, 60, 5.0F, new MobEffectInstance(MobEffects.SLOWNESS, 30, 0));
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
               ServerPlayer second = null;
               float best = -1.0F;

               for (ServerPlayer p : this.engaged()) {
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
                     s.spot = floor(level, s.who.position());
                     Vfx.telegraphCircle(level, s.spot, 2.4, 32 - tick, HOLY);
                     if (tick == 8) {
                        BossFx.markTarget(level, this, s.who, 24);
                     }
                  }
               }
            }
         })
         .hit((b, level, t, i) -> {
            DamageSource src = this.damageSources().mobAttack(this);

            for (AstraelBoss.Sentence s : this.sentences) {
               Vec3 spot = s.spot;
               if (spot != null) {
                  Vfx.beam(level, spot.add(0.0, 30.0, 0.0), spot, HOLY, 1.2, 8);
                  Vfx.pillar(level, spot, HOLY, 26.0, 2.4);
                  Vfx.heavyImpact(level, spot, HOLY, 0.8);

                  for (LivingEntity e : this.foes(level, spot, 2.4, 1.0, 31.0)) {
                     if (e.hurtServer(level, src, this.dmg(26.0F))) {
                        e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0), this);
                        e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1), this);
                        if (s.healing && e == s.who) {
                           e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0), this);
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
         .when((b, t) -> t != null && b.hasLineOfSight(t))
         .utility((b, t, c) -> c.kitingTicks > 60 ? 2.0F : 1.0F)
         .tick((b, level, t, tick) -> {
            if (tick == 6 && t != null) {
               Vfx.chargeUp(level, this.tip(), HOLY, 8);
               Vfx.beam(level, this.tip(), t.getEyePosition(), HOLY, 0.06, 8);
            }
         })
         .hit((b, level, t, i) -> {
            if (t != null && this.hasLineOfSight(t)) {
               Vfx.lightning(level, this.tip(), t.getEyePosition(), HOLY, 3);
               DamageSource src = this.damageSources().mobAttack(this);
               boolean air = t.isFallFlying() || !t.onGround() && !t.isInWater() && BossContext.heightAboveGround(level, t) > 2.5;
               if (air) {
                  t.stopFallFlying();
                  t.removeEffect(MobEffects.SLOW_FALLING);
                  t.hurtServer(level, src, this.dmg(14.0F));
                  Vec3 v = t.getDeltaMovement();
                  t.setDeltaMovement(v.x * 0.2, -1.8, v.z * 0.2);
                  t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1), this);
               } else {
                  t.hurtServer(level, src, this.dmg(12.0F));
                  Vec3 to = flatDir(this.position().subtract(t.position()), Vec3.ZERO);
                  double s = Mth.clamp((flatDist(this.position(), t.position()) - 3.0) / 7.2, 0.35, 1.3);
                  t.setDeltaMovement(to.x * s, 0.3, to.z * s);
               }

               t.syncVelocity = true;
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
                  this.fallingStar(level, BossFx.around(level, this, this.position(), 2.0, 8.0), 20, 2.6, 16.0F);
               } else {
                  this.fallingStar(level, floor(level, this.predict(aim, 20.0)), 20, 2.6, 16.0F);
                  if (this.phase() >= 2) {
                     this.fallingStar(level, BossFx.around(level, this, aim.position(), 1.5, 6.0), 20, 2.6, 16.0F);
                  }

                  if (this.engaged().size() >= 2) {
                     ServerPlayer other = this.otherPlayer(aim);
                     if (other != null) {
                        this.fallingStar(level, floor(level, this.predict(other, 20.0)), 20, 2.6, 16.0F);
                     }
                  }
               }
            }
         })
         .hit((b, level, t, i) -> {
            Vec3 h = this.halo();
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

               for (ServerPlayer p : this.engaged()) {
                  Vec3 w = this.well;
                  if (this.well2 != null && flatDist(p.position(), this.well2) < flatDist(p.position(), w)) {
                     w = this.well2;
                  }

                  this.pullToward(p, w, 10.0, flatDist(p.position(), w) <= 4.5 ? 0.03 : pull);
               }
            }
         })
         .hit((b, level, t, i) -> {
            if (i == 0) {
               this.well = floor(level, t != null ? this.predict(t, 12.0) : this.position());
               this.well2 = null;
               if (this.phase() >= 3) {
                  LivingEntity other = this.otherPlayer(t);
                  LivingEntity aim = other != null ? other : t;
                  Vec3 w2 = floor(level, aim != null ? this.predict(aim, 18.0) : this.position());
                  Vec3 off = w2.subtract(this.well);
                  if (flatDist(w2, this.well) < 7.0) {
                     Vec3 d = flatDir(off, Vec3.directionFromRotation(0.0F, this.getRandom().nextFloat() * 360.0F));
                     w2 = floor(level, this.well.add(d.scale(7.0)));
                  }

                  this.well2 = w2;
               }

               for (Vec3 w : this.wells()) {
                  Vfx.vortex(level, w, 4.5, 4.0, VOID, 24);
                  Vfx.telegraphCircle(level, w, 4.5, 24, S);
               }
            } else {
               for (Vec3 w : this.wells()) {
                  Set<Integer> hit = new HashSet<>();
                  this.blast(level, w, 4.5, 30.0F, 2.2, true, 4.0, hit);

                  for (LivingEntity e : this.foes(level, w, 4.5, 1.5, 4.0)) {
                     if (hit.contains(e.getId())) {
                        e.push(0.0, 0.8, 0.0);
                        e.syncVelocity = true;
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
               Vec3 c = floor(level, t != null ? this.predict(t, 20.0) : this.position());
               double rot = this.getRandom().nextDouble() * Math.PI * 2.0;
               this.addStars(level, c, 6.0, rot);
               if (this.phase() >= 2) {
                  this.addStars(level, c, 3.5, rot + (Math.PI / 5));
               }

               for (int k = 0; k < this.conStars.size(); k++) {
                  Vec3 s = this.conStars.get(k);
                  Vec3 e = this.conEnd(k);
                  Vfx.glint(level, s.add(0.0, 0.8, 0.0), 16774870, 1.2);
                  Vfx.wisps(level, s.add(0.0, 0.6, 0.0), S, 3, 0.6, 30);
                  Vfx.telegraphLine(level, s, e.subtract(s), flatDist(s, e), 2.4, 30, S);
               }
            } else {
               DamageSource src = this.damageSources().mobAttack(this);
               Set<Integer> hit = new HashSet<>();

               for (int k = 0; k < this.conStars.size(); k++) {
                  Vec3 s = this.conStars.get(k);
                  Vec3 e = this.conEnd(k);
                  Vfx.beam(level, s.add(0.0, 0.6, 0.0), e.add(0.0, 0.6, 0.0), S, 0.5, 8);

                  for (LivingEntity v : this.foesOn(level, s, e, 1.2, 3.0, 3.0)) {
                     if (hit.add(v.getId()) && v.hurtServer(level, src, this.dmg(22.0F))) {
                        v.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1), this);
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
               this.radianceAt = floor(level, this.position());
               Vfx.telegraphCircle(level, this.radianceAt, 8.0, 19, S);
               Vfx.chargeUp(level, this.core(), S, 19);
            }
         })
         .hit((b, level, t, i) -> {
            this.blast(level, this.radianceAt, 8.0, 20.0F, 1.4, true);
            Vec3 eye = this.core();
            Vfx.nova(level, eye, S, 8.0);
            Vfx.radialFlash(level, this.position(), 24.0, 16774870, 0.6F);

            for (ServerPlayer p : this.engaged()) {
               double d = p.distanceTo(this);
               if (!(d > 16.0)) {
                  Vec3 toBoss = eye.subtract(p.getEyePosition()).normalize();
                  if (p.getViewVector(1.0F).dot(toBoss) > 0.5) {
                     p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0), this);
                     p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0), this);
                  } else if (d <= 12.0) {
                     p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0), this);
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
            Vfx.ward(level, floor(level, this.position()), 3.0, S, 60);
            this.aegisUntil = this.tickCount + 200;
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
                  Vec3 p = BossFx.around(level, this, this.position(), 3.0, 7.0);
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
               Vec3 g = floor(level, this.position());
               Vfx.telegraphCircle(level, g, 18.0, 55, S);
               Vfx.chargeUp(level, this.core(), S, 55);
               this.shelters.clear();
               int n = this.phase() >= 3 ? 2 : 3;
               double base = this.getRandom().nextDouble() * Math.PI * 2.0;

               for (int k = 0; k < n; k++) {
                  double a = base + k * Math.PI * 2.0 / n + (this.getRandom().nextDouble() - 0.5) * (n == 3 ? 0.5 : 0.9);
                  double r = 8.0 + this.getRandom().nextDouble() * 4.0;
                  Vec3 p = floor(level, new Vec3(g.x + Math.cos(a) * r, g.y, g.z + Math.sin(a) * r));
                  this.shelters.add(p);
                  Vfx.runeCircle(level, p, 2.2, VOID, 60, -0.04F, Vfx.Rune.ASTRAL);
                  Vfx.vortex(level, p, 1.8, 3.0, VOID, 56);
               }
            } else if (tick == 20) {
               Vfx.shake(level, this.position(), 40.0, 0.3, 36);
            }
         })
         .hit((b, level, t, i) -> {
            Vec3 g = floor(level, this.position());
            DamageSource src = this.damageSources().mobAttack(this);

            for (LivingEntity e : this.foes(level, this.position(), 18.0, 18.0, 18.0)) {
               if (!(e.distanceTo(this) > 18.0F)) {
                  if (sheltered(e.position(), this.shelters, 2.2)) {
                     Vfx.shieldBubble(level, e.position(), 1.2, VOID, 10);
                  } else {
                     this.breakGuard(level, e, 60);
                     if (e.hurtServer(level, src, this.dmg(55.0F))) {
                        Vec3 away = flatDir(e.position().subtract(this.position()), this.facing());
                        e.push(away.x * 2.5, 0.6, away.z * 2.5);
                        e.syncVelocity = true;
                        e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0), this);
                     }
                  }
               }
            }

            Vfx.nova(level, this.core(), S, 18.0);
            Vfx.impactFrame(level, this.position(), 40.0, 16777215, 1.0F);
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
               this.singAt = floor(level, this.position());
               this.singTheta = this.getRandom().nextFloat() * 90.0F;
               Vfx.telegraphCircle(level, this.singAt, 7.0, 83, S);
               Vfx.telegraphCircle(level, this.singAt, 16.0, 83, S);
            }

            if (tick <= 20) {
               boolean free = level.noCollision(this, this.getBoundingBox().move(0.0, 0.15, 0.0));
               this.setDeltaMovement(0.0, free ? 0.15 : 0.0, 0.0);
            } else if (tick > 84 && tick <= 100) {
               boolean free = level.noCollision(this, this.getBoundingBox().move(0.0, -0.2, 0.0));
               this.setDeltaMovement(0.0, free ? -0.2 : 0.0, 0.0);
            }

            if (tick == 20) {
               Vfx.vortex(level, this.singAt, 16.0, 6.0, VOID, 64);
            }

            if (tick == 16 || tick == 32) {
               float base = this.singTheta + (tick == 32 ? 45 : 0);

               for (int k = 0; k < 4; k++) {
                  Vfx.telegraphLine(level, this.singAt, Vec3.directionFromRotation(0.0F, base + k * 90), 18.0, 2.0, 24, S);
               }
            }

            if (tick >= 20 && tick < 84) {
               double pull = 0.06 + 0.03 * (tick - 20) / 63.0;

               for (ServerPlayer p : this.engaged()) {
                  this.pullToward(p, this.singAt, 24.0, pull);
               }
            }
         })
         .hit((b, level, t, i) -> {
            DamageSource src = this.damageSources().mobAttack(this);
            Vec3 g = this.singAt;
            if (i >= 2) {
               for (LivingEntity e : this.foes(level, g, 16.0, 3.0, 10.0)) {
                  boolean inner = flatDist(e.position(), g) <= 7.0;
                  if (inner) {
                     this.breakGuard(level, e, 60);
                  }

                  e.hurtServer(level, src, this.dmg(inner ? 60.0F : 22.0F));
                  Vec3 away = flatDir(e.position().subtract(g), this.facing());
                  e.push(away.x * 3.0, 0.8, away.z * 3.0);
                  e.syncVelocity = true;
               }

               Vfx.nova(level, g.add(0.0, 1.0, 0.0), VOID, 16.0);
               Vfx.impactFrame(level, this.position(), 40.0, 11550975, 1.0F);
               Vfx.heavyImpact(level, g, VOID, 3.0);
               Vfx.explosion(level, g.add(0.0, 1.0, 0.0), S, 5.0);
            } else {
               float base = this.singTheta + (i == 1 ? 45 : 0);
               Set<Integer> hit = new HashSet<>();

               for (int k = 0; k < 4; k++) {
                  Vec3 end = g.add(Vec3.directionFromRotation(0.0F, base + k * 90).scale(18.0));
                  Vfx.beam(level, g.add(0.0, 1.0, 0.0), end.add(0.0, 1.0, 0.0), VOID, 0.8, 6);

                  for (LivingEntity e : this.foesOn(level, g, end, 1.0, 1.5, 3.0)) {
                     if (hit.add(e.getId())) {
                        e.hurtServer(level, src, this.dmg(20.0F));
                     }
                  }
               }
            }
         })
         .end((b, level, t, i) -> {
            this.lockout("singularity");
            this.setDeltaMovement(this.getDeltaMovement().scale(0.3));
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
            this.lightCenter = floor(level, home != null ? Vec3.atBottomCenterOf(home) : this.position());
            this.sanctuaries.clear();
            this.shield(this.getMaxHealth() * 0.2F, 170);
            this.lightWard = true;
         })
         .tick((b, level, t, tick) -> {
            if (tick < 138 && this.lightWard && this.shield() <= 0.0F) {
               this.lightWard = false;
               this.forceAction(level, "fading_light");
            } else {
               Vec3 c = this.lightCenter;
               Vec3 perch = tick <= 140 ? c.add(0.0, 7.0, 0.0) : c.add(0.0, this.hoverHeight(), 0.0);
               this.setDeltaMovement(perch.subtract(this.position()).scale(tick <= 24 ? 0.12 : (tick <= 140 ? 0.1 : 0.08)));
               if (tick == 1) {
                  Vfx.telegraphCircle(level, c, 30.0, 139, S);
               } else if (tick == 20) {
                  double rot = this.getRandom().nextDouble() * Math.PI * 2.0;

                  for (int k = 0; k < 4; k++) {
                     double a = rot + k * Math.PI / 2.0;
                     double r = 8.0 + this.getRandom().nextDouble() * 5.0;
                     Vec3 p = floor(level, new Vec3(c.x + Math.cos(a) * r, c.y, c.z + Math.sin(a) * r));
                     this.sanctuaries.add(p);
                     Vfx.runeCircle(level, p, 2.0, HOLY, 125, 0.03F, Vfx.Rune.SOLAR);
                     Vfx.pillar(level, p, HOLY, 6.0, 0.6);
                     Vfx.wisps(level, p.add(0.0, 0.8, 0.0), HOLY, 4, 1.2, 120);
                  }
               } else if (tick == 80) {
                  for (Vec3 p : this.sanctuaries) {
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
               Vec3 c = this.lightCenter;
               DamageSource src = this.damageSources().mobAttack(this);

               for (LivingEntity e : this.foes(level, c, 40.0, 12.0, 40.0)) {
                  if (sheltered(e.position(), this.sanctuaries, 2.2)) {
                     Vfx.shieldBubble(level, e.position(), 1.2, HOLY, 12);
                  } else {
                     this.breakGuard(level, e, 60);
                     if (e.hurtServer(level, src, this.dmg(65.0F))) {
                        Vec3 away = flatDir(e.position().subtract(c), this.facing());
                        e.push(away.x * 1.6, 0.5, away.z * 1.6);
                        e.syncVelocity = true;
                     }
                  }
               }

               Vfx.impactFrame(level, this.position(), 64.0, 16777215, 1.0F);
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
         this.fadeAt = floor(level, this.position());
         Vec3 m = this.mid();
         Vfx.shards(level, m, S.core(), 26, 0.7);
         Vfx.shards(level, m, HOLY.glow(), 14, 0.5);
         Vfx.flare(level, m, 16777215, 3.0, 5);
         Vfx.debris(level, m, Vfx.Debris.FEATHER, 16776176, 20, 0.5);
      }).tick((b, level, t, tick) -> {
         if (tick == 1) {
            Vfx.telegraphCircle(level, this.fadeAt, 4.0, 9, S);
         }

         if (tick <= 10) {
            this.setDeltaMovement(0.0, Math.min(0.0, (this.fadeAt.y - this.getY()) / (11 - tick)), 0.0);
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

   private void cleave(ServerLevel level, @Nullable Vec3 at, float base) {
      Vec3 c = at != null ? at : floor(level, this.position().add(this.facing().scale(3.0)));

      for (LivingEntity e : this.foes(level, this.position(), 7.0, 4.0, 8.0)) {
         if (flatDist(e.position(), c) <= 2.5 + e.getBbWidth() * 0.5 && Math.abs(e.getY() - c.y) <= 3.0 || this.inArc(e, 4.5, 35.0)) {
            this.breakGuard(level, e, 100);
         }
      }

      Set<Integer> hit = new HashSet<>();
      this.blast(level, c, 2.5, base, 1.4, false, 3.0, hit);
      this.spearArc(level, 4.5, 35.0F, base, 1.4, null, hit, false);
      Vfx.slashArc(level, this.chest(), this.getYRot(), 20.0F, 90.0F, 5.0, S, 160.0F);
      Vfx.heavyImpact(level, c, S, 1.2);
      Vfx.groundCrack(level, c, 3.0, S);
      this.fissure(level, c, this.facing());
   }

   private void fissure(ServerLevel level, Vec3 from, Vec3 dir) {
      Vec3 d = flatDir(dir, Vec3.directionFromRotation(0.0F, this.getYRot()));
      Vec3 end = from.add(d.scale(9.0));
      Vfx.telegraphLine(level, from, d, 9.0, 2.2, 16, S);
      this.schedule(16, () -> {
         for (double s = 0.75; s <= 9.0; s += 1.5) {
            Vfx.pillar(level, floor(level, from.add(d.scale(s))), S, 4.0, 0.8);
         }

         DamageSource src = this.damageSources().mobAttack(this);

         for (LivingEntity e : this.foesOn(level, from, end, 1.1, 1.5, 3.0)) {
            if (e.hurtServer(level, src, this.dmg(14.0F))) {
               Vec3 v = e.getDeltaMovement();
               e.setDeltaMovement(v.x, 0.7, v.z);
               e.syncVelocity = true;
            }
         }
      });
   }

   private void afterimage(ServerLevel level, Vec3 at) {
      Vfx.telegraphCircle(level, at, 2.0, 10, S);
      this.schedule(10, () -> {
         this.blast(level, at, 2.0, 10.0F, 0.6, false, 3.0, null);
         Vfx.impact(level, at.add(0.0, 0.5, 0.0), S, 0.7);
      });
   }

   private void fallingStar(ServerLevel level, Vec3 ground, int delay, double r, float base) {
      Vfx.telegraphCircle(level, ground, r, delay, S);
      Vec3 top = ground.add(2.5, 22.0, -2.5);
      Vec3 step = ground.subtract(top).scale(0.125);

      for (int j = 0; j < 8; j++) {
         int at = delay - 8 + j;
         if (at >= 1) {
            Vec3 head = top.add(step.scale(j));
            this.schedule(at, () -> Vfx.meteorTrail(level, head, step, S));
         }
      }

      this.schedule(delay, () -> {
         this.blast(level, ground, r, base, 1.0, false, 12.0, null);
         Vfx.heavyImpact(level, ground, S, 0.6);
         BossSounds.play(level, ground, "boss.astrael.starfall.hit", 2.5F, 1.1F);
      });
   }

   private void meteorWave(ServerLevel level, Vec3 c) {
      for (ServerPlayer p : this.engaged()) {
         Vec3 at = floor(level, this.predict(p, 24.0));
         if (!sheltered(at, this.sanctuaries, 3.0)) {
            this.fallingStar(level, at, 24, 3.0, 18.0F);
         }
      }

      for (int k = 0; k < 2; k++) {
         for (int tries = 0; tries < 6; tries++) {
            Vec3 at = BossFx.around(level, this, c, 0.0, 10.0);
            if (!sheltered(at, this.sanctuaries, 3.0)) {
               this.fallingStar(level, at, 24, 3.0, 18.0F);
               break;
            }
         }
      }
   }

   private void addStars(ServerLevel level, Vec3 c, double radius, double rot) {
      for (int k = 0; k < 5; k++) {
         double a = rot + k * Math.PI * 2.0 / 5.0;
         this.conStars.add(floor(level, new Vec3(c.x + Math.cos(a) * radius, c.y, c.z + Math.sin(a) * radius)));
      }
   }

   private Vec3 conEnd(int k) {
      int group = k / 5 * 5;
      return this.conStars.get(group + (k % 5 + 2) % 5);
   }

   private List<Vec3> wells() {
      List<Vec3> out = new ArrayList<>(2);
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

   private void spawnMinion(ServerLevel level, String kind, Vec3 at, @Nullable LivingEntity target) {
      EntityType<RealmMob> type = RealmMobs.type(kind);
      if (type != null) {
         RealmMob mob = (RealmMob)type.create(level, EntitySpawnReason.MOB_SUMMONED);
         if (mob != null) {
            mob.snapTo(at.x, at.y, at.z, this.getRandom().nextFloat() * 360.0F, 0.0F);
            LivingEntity aim = target != null ? target : this.getTarget();
            if (aim != null) {
               mob.setTarget(aim);
            }

            level.addFreshEntity(mob);
            Vfx.pillar(level, at, S, 2.5, 0.7);
         }
      }
   }

   private void spawnAdds(ServerLevel level, int starlings, int sentinels) {
      if (this.isAlive()) {
         this.minionCheckedAt = -1000;
         int room = 6 - this.minionCount();

         for (int k = 0; k < sentinels + starlings && k < room; k++) {
            this.spawnMinion(level, k < sentinels ? "astral_sentinel" : "starling", BossFx.around(level, this, this.position(), 3.0, 7.0), this.getTarget());
         }

         this.minionCheckedAt = -1000;
      }
   }

   private int minionCount() {
      if (this.tickCount - this.minionCheckedAt >= 20) {
         this.minionCheckedAt = this.tickCount;
         this.minionCache = this.level()
            .getEntitiesOfClass(
               RealmMob.class, this.getBoundingBox().inflate(32.0), m -> m.isAlive() && ("starling".equals(m.bossId()) || "astral_sentinel".equals(m.bossId()))
            )
            .size();
      }

      return this.minionCache;
   }

   private Vec3 facing() {
      return Vec3.directionFromRotation(0.0F, this.getYRot());
   }

   private Vec3 right() {
      return Vec3.directionFromRotation(0.0F, this.getYRot() + 90.0F);
   }

   private Vec3 mid() {
      return this.position().add(0.0, 1.8, 0.0);
   }

   private Vec3 chest() {
      return this.position().add(0.0, 2.5, 0.0);
   }

   private Vec3 head() {
      return this.position().add(0.0, 3.15, 0.0);
   }

   private Vec3 core() {
      return this.position().add(0.0, 2.45, 0.0).add(this.facing().scale(0.25));
   }

   private Vec3 halo() {
      return this.position().add(0.0, 3.4, 0.0).subtract(this.facing().scale(0.4));
   }

   private Vec3 tip() {
      return this.position().add(0.0, 3.3, 0.0).add(this.facing().scale(1.2)).add(this.right().scale(0.4));
   }

   private Vec3 lanceHead() {
      return this.position().add(0.0, 3.2, 0.0).add(this.right().scale(0.7));
   }

   private Vec3 volleyFrom() {
      return this.position().add(0.0, 2.6, 0.0).subtract(this.right().scale(0.8));
   }

   private double volleySpeed() {
      return this.phase() >= 3 ? 1.9 : 1.6;
   }

   private Vec3 bladeFrom() {
      return this.position().add(0.0, 3.6, 0.0).subtract(this.facing().scale(0.4));
   }

   private static Vec3 floor(ServerLevel level, Vec3 at) {
      Vec3 g = BossFx.ground(level, at.x, at.y, at.z);
      return g != null ? g : at;
   }

   private void turnTo(Vec3 at) {
      double dx = at.x - this.getX();
      double dz = at.z - this.getZ();
      if (!(dx * dx + dz * dz < 1.0E-4)) {
         float yaw = (float)(Mth.atan2(dz, dx) * 180.0F / (float)Math.PI) - 90.0F;
         this.setYRot(yaw);
         this.setYBodyRot(yaw);
         this.setYHeadRot(yaw);
      }
   }

   private void sink(ServerLevel level, @Nullable LivingEntity t, double k) {
      double want = (t != null ? t.getY() : floor(level, this.position()).y) + 0.3;
      Vec3 v = this.getDeltaMovement();
      this.setDeltaMovement(v.x, Mth.clamp((want - this.getY()) * k, -0.6, 0.6), v.z);
   }

   private void rise(double dy) {
      Vec3 v = this.getDeltaMovement();
      this.setDeltaMovement(v.x, dy, v.z);
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

      ItemUseAnimation use = e.getUseItem().getUseAnimation();
      return use == ItemUseAnimation.EAT || use == ItemUseAnimation.DRINK;
   }

   private @Nullable ServerPlayer otherPlayer(@Nullable LivingEntity not) {
      List<ServerPlayer> list = this.engaged();
      int n = list.size();
      if (n == 0) {
         return null;
      }

      int start = this.getRandom().nextInt(n);

      for (int k = 0; k < n; k++) {
         ServerPlayer p = list.get((start + k) % n);
         if (p != not) {
            return p;
         }
      }

      return null;
   }

   private static double flatDist(Vec3 a, Vec3 b) {
      double dx = a.x - b.x;
      double dz = a.z - b.z;
      return Math.sqrt(dx * dx + dz * dz);
   }

   private static Vec3 flatDir(Vec3 v, Vec3 fallback) {
      Vec3 h = new Vec3(v.x, 0.0, v.z);
      return h.lengthSqr() > 1.0E-6 ? h.normalize() : fallback;
   }

   private static double segT(Vec3 p, Vec3 a, Vec3 b) {
      double dx = b.x - a.x;
      double dz = b.z - a.z;
      double len2 = dx * dx + dz * dz;
      return len2 < 1.0E-6 ? 0.0 : Mth.clamp(((p.x - a.x) * dx + (p.z - a.z) * dz) / len2, 0.0, 1.0);
   }

   private static double segDist(Vec3 p, Vec3 a, Vec3 b) {
      double t = segT(p, a, b);
      double x = a.x + (b.x - a.x) * t - p.x;
      double z = a.z + (b.z - a.z) * t - p.z;
      return Math.sqrt(x * x + z * z);
   }

   private static boolean sheltered(Vec3 p, List<Vec3> spots, double r) {
      for (Vec3 s : spots) {
         if (flatDist(p, s) <= r) {
            return true;
         }
      }

      return false;
   }

   private List<LivingEntity> foes(ServerLevel level, Vec3 c, double r, double down, double up) {
      AABB box = new AABB(c.x - r - 1.0, c.y - down, c.z - r - 1.0, c.x + r + 1.0, c.y + up, c.z + r + 1.0);
      return level.getEntitiesOfClass(
         LivingEntity.class,
         box,
         e -> e != this
            && e.isAlive()
            && !(e instanceof MysticalBoss)
            && !(e instanceof Player p && (p.isCreative() || p.isSpectator()))
            && flatDist(e.position(), c) <= r + e.getBbWidth() * 0.5
      );
   }

   private List<LivingEntity> foesOn(ServerLevel level, Vec3 a, Vec3 b, double half, double below, double above) {
      Vec3 m = a.add(b).scale(0.5);
      double dy = Math.abs(b.y - a.y) * 0.5;
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : this.foes(level, m, flatDist(a, b) * 0.5 + half, below + dy + 1.0, above + dy + 1.0)) {
         double t = segT(e.position(), a, b);
         double h = e.getY() - (a.y + (b.y - a.y) * t);
         if (h >= -below && h <= above && segDist(e.position(), a, b) <= half + e.getBbWidth() * 0.5) {
            out.add(e);
         }
      }

      return out;
   }

   private boolean inArc(LivingEntity e, double reach, double halfAngle) {
      double dx = e.getX() - this.getX();
      double dz = e.getZ() - this.getZ();
      double d = Math.sqrt(dx * dx + dz * dz);
      if (d > reach + e.getBbWidth() * 0.5 || Math.abs(e.getY() - this.getY()) > 3.5) {
         return false;
      } else if (!(d < 1.0) && !(halfAngle >= 180.0)) {
         Vec3 f = this.facing();
         return Math.toDegrees(Math.acos(Mth.clamp((dx * f.x + dz * f.z) / d, -1.0, 1.0))) <= halfAngle;
      } else {
         return true;
      }
   }

   private List<LivingEntity> spearArc(
      ServerLevel level, double reach, float halfAngle, float base, double push, @Nullable Vec3 shove, @Nullable Set<Integer> hit, boolean pierce
   ) {
      List<LivingEntity> out = new ArrayList<>();
      DamageSource src = this.damageSources().mobAttack(this);
      Vec3 f = this.facing();

      for (LivingEntity e : this.foes(level, this.position(), reach, 3.5, 7.5)) {
         if (this.inArc(e, reach, halfAngle) && (hit == null || hit.add(e.getId()))) {
            if (pierce) {
               e.setInvulnerableTime(0);
            }

            if (e.hurtServer(level, src, this.dmg(base))) {
               Vec3 dir = shove != null ? shove : flatDir(e.position().subtract(this.position()), f);
               e.push(dir.x * push, 0.25, dir.z * push);
               e.syncVelocity = true;
               Vfx.impact(level, e.position().add(0.0, e.getBbHeight() * 0.5, 0.0), S, 0.6);
               out.add(e);
            }
         }
      }

      return out;
   }

   private void spearLine(ServerLevel level, Vec3 a, Vec3 b, double radius, float base, double push) {
      DamageSource src = this.damageSources().mobAttack(this);
      Vec3 dir = flatDir(b.subtract(a), this.facing());

      for (LivingEntity e : this.foesOn(level, a, b, radius, 3.5, 2.0)) {
         if (e.hurtServer(level, src, this.dmg(base))) {
            e.push(dir.x * push, 0.25, dir.z * push);
            e.syncVelocity = true;
            Vfx.impact(level, e.position().add(0.0, e.getBbHeight() * 0.5, 0.0), S, 0.6);
         }
      }
   }

   private void blast(ServerLevel level, Vec3 c, double r, float base, double push, boolean falloff) {
      this.blast(level, c, r, base, push, falloff, 3.5, null);
   }

   private void blast(ServerLevel level, Vec3 c, double r, float base, double push, boolean falloff, double up, @Nullable Set<Integer> hit) {
      DamageSource src = this.damageSources().mobAttack(this);
      Vec3 f = this.facing();

      for (LivingEntity e : this.foes(level, c, r, 1.5, up)) {
         if (hit == null || hit.add(e.getId())) {
            double k = falloff ? Mth.clamp(1.0 - flatDist(e.position(), c) / r, 0.0, 1.0) : 1.0;
            float amount = this.dmg(base) * (float)(falloff ? 0.4 + 0.6 * k : 1.0);
            if (e.hurtServer(level, src, amount)) {
               Vec3 away = flatDir(e.position().subtract(c), f);
               double s = falloff ? 0.5 + k : 1.0;
               e.push(away.x * push * s, 0.3 + 0.3 * k, away.z * push * s);
               e.syncVelocity = true;
            }
         }
      }
   }

   private void wave(ServerLevel level, Vec3 c, double maxR, int ticks, float base, double push) {
      Set<Integer> hit = new HashSet<>();
      Vfx.ring(level, c, S.glow(), maxR, (int)(ticks * 1.3));

      for (int t = 1; t <= ticks; t++) {
         double r = maxR * t / ticks;
         boolean draw = t % 2 == 0;
         this.schedule(t, () -> {
            if (draw) {
               Vfx.ring(level, c, S.core(), r, 4);
            }

            DamageSource src = this.damageSources().mobAttack(this);

            for (LivingEntity e : this.foes(level, c, r + 1.0, 1.0, 2.0)) {
               if (Math.abs(flatDist(e.position(), c) - r) < 1.0 && e.onGround() && hit.add(e.getId()) && e.hurtServer(level, src, this.dmg(base))) {
                  Vec3 away = flatDir(e.position().subtract(c), this.facing());
                  e.push(away.x * push, 0.4, away.z * push);
                  e.syncVelocity = true;
               }
            }
         });
      }
   }

   private void zone(ServerLevel level, Vec3 c, double r, int ticks, float perSecond, @Nullable MobEffectInstance effect) {
      for (int t = 10; t < ticks; t += 20) {
         this.schedule(t, () -> {
            Vfx.embers(level, c.add(0.0, 0.3, 0.0), 16767088, 6, r * 0.7);
            DamageSource src = this.damageSources().indirectMagic(this, this);

            for (LivingEntity e : this.foes(level, c, r, 1.0, 3.0)) {
               e.hurtServer(level, src, this.dmg(perSecond));
               if (effect != null) {
                  e.addEffect(new MobEffectInstance(effect), this);
               }
            }
         });
      }
   }

   private void breakGuard(ServerLevel level, LivingEntity e, int ticks) {
      ItemStack s = e.getItemBlockingWith();
      if (s != null && e instanceof Player p) {
         p.getCooldowns().addCooldown(s, ticks);
         p.stopUsingItem();
         Vec3 m = e.position().add(0.0, e.getBbHeight() * 0.5, 0.0);
         Vfx.shards(level, m, 16774870, 14, 0.5);
         Vfx.glint(level, m, 16777215, 1.4);
      }
   }

   private void pullToward(LivingEntity e, Vec3 c, double radius, double strength) {
      double dx = c.x - e.getX();
      double dz = c.z - e.getZ();
      double d = Math.sqrt(dx * dx + dz * dz);
      if (d <= radius && d > 0.8) {
         this.pulls.merge(e, new Vec3(dx / d * strength, 0.0, dz / d * strength), Vec3::add);
         this.pulledNow.add(e);
      }
   }

   private void flushPulls(ServerLevel level) {
      if (!this.pulls.isEmpty()) {
         this.pulls.keySet().retainAll(this.pulledNow);
         this.pulledNow.clear();

         for (Entry<LivingEntity, Vec3> p : this.pulls.entrySet()) {
            if (p.getValue().lengthSqr() >= 0.1296 && p.getKey().isAlive()) {
               this.tug(level, p.getKey(), p.getValue());
               p.setValue(Vec3.ZERO);
            }
         }
      }
   }

   private void tug(ServerLevel level, LivingEntity e, Vec3 kick) {
      double x = kick.x;
      double z = kick.z;
      if (e instanceof ServerPlayer sp) {
         if (sp.isPassenger() || sp.isSpectator() || sp.getAbilities().flying) {
            return;
         }

         boolean ground = sp.onGround();
         boolean water = sp.isInWater();
         double s = ground ? 1.0 : (water ? 0.6 : 0.4);
         x *= s;
         z *= s;
         if (sp.syncVelocity) {
            sp.setDeltaMovement(sp.getDeltaMovement().add(x, 0.0, z));
            return;
         }

         Vec3 k = sp.getKnownMovement();
         double f = sp.isFallFlying() ? 0.99 : (water ? 0.8 : (ground ? 0.546 : 0.91));
         double y = ground ? -0.0784 : (sp.isFallFlying() ? k.y * 0.98 : (water ? k.y * 0.8 : (k.y - 0.08) * 0.98));
         sp.setDeltaMovement(k.x * f + x, y, k.z * f + z);
         Vfx.streaks(level, sp.position().add(0.0, 1.0, 0.0), new Vec3(x, 0.0, z).normalize().scale(1.6), VOID, 3);
      } else {
         e.push(x, 0.0, z);
      }

      e.syncVelocity = true;
   }

   @Override
   public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
      if (this.aegisUntil > this.tickCount
         && this.shield() > 0.0F
         && this.tickCount >= this.reflectReady
         && source.getDirectEntity() instanceof Projectile
         && source.getEntity() instanceof LivingEntity shooter
         && shooter != this
         && shooter.isAlive()) {
         this.reflectReady = this.tickCount + 10;
         Vec3 m = this.mid();
         Vec3 dir = shooter.getEyePosition().subtract(m).normalize();
         BossBolt.shoot(world, this, BossBolt.Style.STAR, m.add(dir), dir.scale(1.4), this.dmg(8.0F));
         Vfx.glint(world, m.add(dir), 16777215, 1.6);
      }

      return super.hurtServer(world, source, amount);
   }

   @Override
   protected void customServerAiStep(ServerLevel world) {
      super.customServerAiStep(world);
      if (this.isAlive() && !this.inIntro()) {
         if (!this.isNoGravity() && !this.staggered() && !this.isCurrent("fading_light")) {
            this.setNoGravity(true);
         }

         if (this.pendingLight && !this.busy()) {
            this.pendingLight = false;
            this.forceAction(world, "last_light");
         }

         if (this.isCurrent("phase_3") && this.actionTick() <= 24) {
            this.rise(0.07);
         } else if (this.isCurrent("stagger") && this.actionTick() <= 18) {
            Vec3 v = this.getDeltaMovement();
            this.setDeltaMovement(v.x, Math.min(0.0, (floor(world, this.position()).y - this.getY()) / (19 - this.actionTick())), v.z);
         }

         if (this.tickCount % 20 == 0 && !this.busy() && this.look() < this.phase()) {
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
         if (this.tickCount % every == 0) {
            Vfx.embers(world, this.mid(), this.phase() >= 3 ? VOID.glow() : S.core(), 3, 1.0);
         }

         if (this.tickCount % 30 == 0) {
            Vfx.glint(world, this.halo(), 16777215, 1.0);
         }

         this.flushPulls(world);
      } else {
         this.pulls.clear();
         this.pulledNow.clear();
      }
   }

   private void tickAegis(ServerLevel level) {
      if (this.aegisUntil >= 0) {
         if (!(this.shield() <= 0.0F) && this.aegisUntil > this.tickCount) {
            int left = this.aegisUntil - this.tickCount;
            if (left == 20) {
               this.aegisNovaAt = floor(level, this.position());
               Vfx.telegraphCircle(level, this.aegisNovaAt, 6.0, 19, S);
            } else if (left == 1) {
               Vec3 c = this.aegisNovaAt != null ? this.aegisNovaAt : floor(level, this.position());
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

   private void hazards(ServerLevel level) {
      int p = this.phase();
      boolean ultimate = this.isCurrent("supernova") || this.isCurrent("singularity") || this.isCurrent("last_light");
      if (p >= 1) {
         if (this.nodes.isEmpty()) {
            this.buildNodes(level);
         }

         if (this.tickCount % 40 == 0) {
            for (Vec3 n : this.nodes) {
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
         List<ServerPlayer> list = new ArrayList<>(this.engaged());

         for (int n = 0; n < 3 && !list.isEmpty(); n++) {
            ServerPlayer pl = list.remove(this.getRandom().nextInt(list.size()));
            this.fallingStar(level, floor(level, this.predict(pl, 24.0)), 24, 2.5, 14.0F);
         }
      }

      if (p >= 3) {
         this.eventHorizon(level);
      }
   }

   private void buildNodes(ServerLevel level) {
      this.nodes.clear();
      BlockPos home = this.home();
      Vec3 c = home != null ? Vec3.atBottomCenterOf(home) : this.position();
      double rot = this.getRandom().nextDouble() * Math.PI * 2.0;

      for (int k = 0; k < 5; k++) {
         double a = rot + k * Math.PI * 2.0 / 5.0;
         this.nodes.add(floor(level, new Vec3(c.x + Math.cos(a) * 11.0, c.y, c.z + Math.sin(a) * 11.0)));
      }
   }

   private void nodeBeam(ServerLevel level) {
      int k = this.getRandom().nextInt(5);
      int j = this.phase() >= 2 && this.getRandom().nextBoolean() ? (k + 2) % 5 : (k + 1) % 5;
      Vec3 a = this.nodes.get(k);
      Vec3 b = this.nodes.get(j);
      Vfx.telegraphLine(level, a, b.subtract(a), flatDist(a, b), 1.6, 24, S);
      this.schedule(24, () -> {
         Vfx.beam(level, a.add(0.0, 1.0, 0.0), b.add(0.0, 1.0, 0.0), S, 0.6, 8);
         DamageSource src = this.damageSources().indirectMagic(this, this);
         Vec3 side = new Vec3(-(b.z - a.z), 0.0, b.x - a.x);

         for (LivingEntity e : this.foesOn(level, a, b, 0.9, 0.5, 3.0)) {
            e.hurtServer(level, src, this.dmg(10.0F));
            Vec3 on = a.add(b.subtract(a).scale(segT(e.position(), a, b)));
            Vec3 off = flatDir(e.position().subtract(on), flatDir(side, Vec3.ZERO));
            e.push(off.x * 0.8, 0.2, off.z * 0.8);
            e.syncVelocity = true;
         }

         BossSounds.play(level, a.add(b).scale(0.5), "boss.astrael.constellation.hit", 2.0F, 1.0F);
      });
   }

   private void eventHorizon(ServerLevel level) {
      boolean pull = !this.isCurrent("fading_light") && !this.staggered();
      boolean pulse = this.tickCount % 20 == 0;
      if (pull || pulse) {
         Vec3 c = floor(level, this.position());
         if (pull) {
            for (ServerPlayer p : this.engaged()) {
               this.pullToward(p, c, 22.0, 0.03);
            }
         }

         if (pulse) {
            Vfx.auraRing(level, c, VOID, 3.5);
            DamageSource src = this.damageSources().indirectMagic(this, this);

            for (LivingEntity e : this.foes(level, c, 3.5, 1.0, 4.0)) {
               e.hurtServer(level, src, this.dmg(4.0F));
            }

            if (this.tickCount % 40 == 0) {
               BossSounds.play(level, c, "boss.astrael.gravity_well.hit", 0.6F, 0.6F);
            }
         }
      }
   }

   private void markHealer() {
      if (this.phase() >= 1 && this.tickCount % 20 == 0 && this.tickCount >= this.healerMarkReady) {
         LivingEntity t = this.getTarget();

         for (ServerPlayer p : this.engaged()) {
            if (p != t && p.distanceToSqr(this) <= 784.0 && healing(p)) {
               this.markTarget(p, 100);
               this.healerMarkReady = this.tickCount + 200;
               return;
            }
         }
      }
   }

   @Override
   protected void onPhaseChange(ServerLevel level, int phase) {
      Vfx.Palette p = phase >= 3 ? VOID : (phase == 2 ? HOLY : S);
      Vfx.telegraphCircle(level, floor(level, this.position()), 8 + phase, 27, p);
      Vfx.chargeUp(level, this.core(), p, 27);
      if (phase >= 2) {
         this.bar.setDarkenScreen(true);
      }

      if (phase >= 3) {
         this.bar.setCreateWorldFog(true);
      }

      int starlings = phase >= 3 ? 0 : 2;
      int sentinels = phase >= 3 ? 2 : (phase == 2 ? 1 : 0);
      this.schedule(40, () -> this.spawnAdds(level, starlings, sentinels));
   }

   @Override
   protected void phaseBurst(ServerLevel level, int phase) {
      Vec3 g = floor(level, this.position());
      Vec3 m = this.mid();
      this.setLook(Math.min(3, phase));
      if (phase >= 1 && this.nodes.isEmpty()) {
         this.buildNodes(level);

         for (Vec3 n : this.nodes) {
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
         Vfx.impactFrame(level, this.position(), 48.0, 16777215, 0.8F);
         Vfx.shards(level, this.chest(), 15657182, 30, 0.7);
         Vfx.shards(level, this.head(), 14595156, 10, 0.5);
         Vfx.shockwave(level, g, HOLY, 9.0);
         Vfx.runeCircle(level, g, 12.0, S, 60, 0.05F, Vfx.Rune.SOLAR);
         this.blast(level, g, 9.0, 14.0F, 2.0, true);
      } else {
         Vfx.nova(level, m, VOID, 16.0);
         Vfx.impactFrame(level, this.position(), 48.0, 11550975, 1.0F);
         Vfx.vortex(level, g, 12.0, 6.0, VOID, 60);
         Vfx.shockwave(level, g, VOID, 10.0);
         this.blast(level, g, 10.0, 16.0F, 2.2, true);
      }
   }

   @Override
   protected void onStagger(ServerLevel level) {
      this.setNoGravity(false);
      this.schedule(58, () -> {
         if (this.isAlive() && !this.isCurrent("fading_light")) {
            this.setNoGravity(true);
         }
      });
      this.aegisUntil = -1;
      Vec3 h = this.halo();
      Vfx.shards(level, h, 16767088, 12, 0.5);
      Vfx.glint(level, h, 16777215, 1.6);
   }

   @Override
   protected void onEnrage(ServerLevel level) {
      Vfx.nova(level, this.mid(), HOLY, 12.0);
      Vfx.auraRing(level, floor(level, this.position()), S, 3.0);
      this.nodeTimer = Math.min(this.nodeTimer, 40);
      this.starTimer = Math.min(this.starTimer, 40);
   }

   @Override
   protected void desperation(ServerLevel level) {
      if (!this.transitioning() && !this.isCurrent("stagger")) {
         this.forceAction(level, "last_light");
      } else {
         this.pendingLight = true;
      }
   }

   @Override
   protected void onIntroTick(ServerLevel level, int left) {
      int t = this.introTicks() - 1 - left;
      Vec3 pos = this.position();
      Vec3 g = floor(level, pos);
      if (t < 30) {
         double s = t / 30.0;
         Vfx.meteorTrail(level, pos.add(0.0, 1.5 + 15.0 * (1.0 - s * s), 0.0), new Vec3(0.0, -Math.max(0.4, s), 0.0), S);
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
               Vec3 d = Vec3.directionFromRotation(0.0F, k * 45);
               Vfx.streaks(level, this.mid().add(d.scale(1.5)), d, S, 4);
            }
         }
      } else if (t >= 84 && t <= 105 && (t - 84) % 3 == 0) {
         double a = Math.toRadians((t - 84) / 3 * 45);
         Vec3 point = this.halo().add(this.right().scale(Math.cos(a) * 0.55)).add(0.0, Math.sin(a) * 0.55, 0.0);
         Vfx.glint(level, point, 16767088, 0.7);
      } else if (t >= 108 && t <= 118) {
         Vec3 hand = pos.add(0.0, 1.9, 0.0).add(this.right().scale(0.5));
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
   protected void onDeathTick(ServerLevel level, int tick) {
      Vec3 pos = this.position();
      if (tick == 1) {
         this.deathFloor = floor(level, pos).y;
         Vfx.flare(level, this.core(), 16777215, 2.5, 8);
      }

      if (tick >= 12 && tick < 32) {
         this.setDeltaMovement(0.0, Math.min(0.0, (this.deathFloor - this.getY()) / (32 - tick)), 0.0);
      } else if (tick == 32) {
         this.setDeltaMovement(Vec3.ZERO);
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
         Vfx.glint(level, pos.add(this.right().scale(0.45)).add(this.facing().scale(0.4 + s * 1.4)).add(0.0, 1.4 - s * 1.2, 0.0), 16777215, 0.8);
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
         this.bar.setDarkenScreen(false);
         this.bar.setCreateWorldFog(false);
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
      @Nullable Vec3 spot;

      Sentence(LivingEntity who, boolean healing) {
         this.who = who;
         this.healing = healing;
      }
   }
}
