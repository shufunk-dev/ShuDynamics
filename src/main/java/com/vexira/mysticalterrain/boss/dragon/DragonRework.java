package com.vexira.mysticalterrain.boss.dragon;

import com.vexira.mysticalterrain.boss.MysticalBosses;
import com.vexira.mysticalterrain.boss.net.BossNet;
import com.vexira.mysticalterrain.boss.net.BossStatePayload;
import com.vexira.mysticalterrain.boss.vfx.Vfx;
import com.vexira.mysticalterrain.boss.vfx.VfxOptions;
import com.vexira.mysticalterrain.boss.vfx.VfxParticles;
import com.vexira.mysticalterrain.mixin.EnderDragonFightAccessor;
import com.vexira.mysticalterrain.mixin.EnderDragonFightInvoker;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonChargePlayerPhase;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonStrafePlayerPhase;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.EndPodiumFeature;
import net.minecraft.world.level.levelgen.feature.EndSpikeFeature;
import net.minecraft.world.level.levelgen.feature.EndSpikeFeature.EndSpike;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class DragonRework {
   public static final int NORMAL = 0;
   public static final int SUMMONING = 1;
   public static final int SWEEP = 2;
   public static final int CHANNEL = 3;
   public static final int DIVE = 4;
   public static final int SLAM = 5;
   public static final int ROAR = 6;
   public static final int STUNNED = 7;
   public static final int PLUNGE = 8;
   public static final int FIREBALL = 9;
   public static final int STRAFE_BREATH = 10;
   public static final int RAKE_DIVE = 11;
   public static final int TALON_RAPTURE = 12;
   public static final int WING_WARD = 13;
   public static final int VOID_BEAM = 14;
   public static final int GRAVITY_WELL = 15;
   public static final int METEOR_RAIN = 16;
   public static final int SHATTERDIVE = 17;
   public static final int OBLIVION_NOVA = 18;
   public static final int TAIL_SWEEP = 19;
   public static final int RENDING_BITE = 20;
   public static final int VOID_FISSURE = 21;
   public static final int WING_STORM = 22;
   public static final int POUNCE = 23;
   public static final int CRASH = 24;
   public static final int STAGGER = 25;
   public static final int RECOVER = 26;
   public static final int PHASE_1 = 27;
   public static final int PHASE_2 = 28;
   public static final int PHASE_3 = 29;
   public static final int APPROACH = 30;
   public static final int RELOCATE = 31;
   public static final float MAX_HEALTH = 800.0F;
   public static final float DEFENSE = 0.5F;
   public static final int LEVEL = 80;
   public static final float POISE_MAX = 160.0F;
   public static final int SUMMON_TICKS = 160;
   public static final int DEATH_TICKS = 200;
   private static final float[] THRESHOLDS = new float[]{0.7F, 0.4F, 0.15F};
   private static final List<Float> BAR_THRESHOLDS = BossStatePayload.thresholds(0.7F, 0.4F, 0.15F);
   private static final int SOFT_ENRAGE_TICKS = 18000;
   private static final int RESET_TICKS = 1800;
   private static final float PERCH_BUDGET = 96.0F;
   private static final int[][] AIR_REST = new int[][]{{100, 160}, {80, 120}, {60, 90}, {40, 70}};
   private static final int[][] PERCH_REST = new int[][]{{30, 50}, {26, 40}, {20, 32}, {14, 24}};
   private static final Vfx.Palette V = DragonFx.V;
   private static final int WHITE = 16777215;
   private static final int CLIP_LAG = 2;
   private static final double[][] SWEEP_JAW = new double[][]{
      {0.0, 4.17, 3.61, -0.01},
      {0.25, 2.21, 6.1, 0.0},
      {0.45, 1.64, 6.35, 0.0},
      {0.5, 1.71, 6.29, 0.12},
      {0.55, 2.82, 5.59, -0.61},
      {0.65, 4.32, 1.72, -1.38},
      {0.7, 4.4, 1.34, -1.51},
      {1.3, 4.63, 1.62, -0.85},
      {1.9, 4.63, 1.58, 0.83},
      {2.45, 4.43, 1.44, 1.47},
      {2.65, 4.39, 1.68, 1.34},
      {2.9, 4.24, 3.47, 0.19},
      {3.0, 4.17, 3.61, -0.01}
   };
   private static final double[][] BEAM_JAW = new double[][]{
      {0.0, 4.87, 2.27, 0.0},
      {0.3, 4.85, 2.61, 0.0},
      {0.85, 3.87, 5.32, 0.0},
      {1.35, 2.78, 6.26, 0.0},
      {1.45, 3.29, 5.84, 0.0},
      {1.5, 4.63, 3.7, 0.0},
      {1.55, 4.89, 2.09, 0.0},
      {1.85, 4.6, 1.66, 0.32},
      {2.7, 4.7, 2.24, -0.33},
      {3.5, 4.87, 2.27, 0.0}
   };
   private static final double[][] WELL_JAW = new double[][]{
      {0.0, 4.87, 2.27, 0.0},
      {0.2, 4.84, 2.54, 0.0},
      {0.45, 4.4, 4.67, 0.0},
      {0.75, 2.28, 6.97, 0.0},
      {1.05, 1.12, 7.42, 0.01},
      {1.15, 2.43, 6.9, 0.0},
      {1.2, 4.16, 5.27, 0.0},
      {1.25, 4.72, 3.65, 0.0},
      {1.3, 4.64, 3.3, 0.0},
      {2.2, 4.59, 2.67, 0.11},
      {2.95, 4.57, 3.44, -0.02},
      {3.1, 4.52, 3.77, 0.0},
      {3.15, 4.33, 4.35, 0.0},
      {3.2, 3.59, 5.78, 0.0},
      {3.3, 3.04, 6.5, 0.0},
      {3.35, 3.78, 5.78, 0.0},
      {3.4, 5.15, 2.29, 0.15},
      {3.45, 5.14, 2.41, -0.08},
      {3.5, 4.96, 0.97, 0.07},
      {4.0, 4.87, 2.27, 0.0}
   };
   private static final double[] TALON_STRIKE = new double[]{-0.75, -0.8};
   private static final double[] TALON_JAW = new double[]{4.4, 2.7};
   private static final double[][] NOVA_JAW = new double[][]{
      {0.0, 4.87, 2.27, 0.0},
      {0.8, 3.69, -0.72, 0.0},
      {1.4, 3.27, -1.18, 0.0},
      {2.2, 3.85, -0.73, 0.01},
      {2.6, 4.53, 0.35, -0.01},
      {3.0, 4.58, 0.16, 0.0},
      {3.35, 5.02, 1.74, -0.01},
      {3.85, 5.09, 1.56, 0.02},
      {4.15, 5.05, 3.04, -0.02},
      {4.25, 5.12, 2.56, -0.02},
      {4.4, 4.57, 0.13, 0.01},
      {4.55, 4.32, -0.26, 0.0},
      {4.65, 4.48, 0.14, 0.0},
      {4.7, 4.76, 0.98, 0.0},
      {4.75, 4.9, 2.92, 0.0},
      {4.8, 3.41, 6.1, 0.04}
   };
   private final EnderDragon dragon;
   private final DragonSense sense = new DragonSense();
   private final DragonHazards hazards = new DragonHazards(this);
   private final BossNet.Sync sync = new BossNet.Sync();
   private final int[] cooldowns = new int[DragonMove.values().length];
   private final Deque<DragonMove> forced = new ArrayDeque<>();
   private final List<DragonRework.Reflect> reflected = new ArrayList<>();
   private final List<Vec3> lines = new ArrayList<>();
   private final List<EndSpike> recall = new ArrayList<>();
   private boolean initialized;
   private int stateTicks;
   private int stage;
   private Vec3 portal = Vec3.ZERO;
   private @Nullable DragonMove move;
   private @Nullable DragonMove last1;
   private @Nullable DragonMove last2;
   private @Nullable ServerPlayer victim;
   private int rest = 120;
   private boolean takeoffAfter;
   private Vec3 pa = Vec3.ZERO;
   private Vec3 pb = Vec3.ZERO;
   private Vec3 pc = Vec3.ZERO;
   private double speed;
   private int pass;
   private int passes;
   private float channelDamage;
   private int channelLength = 140;
   private int shatterStart;
   private int shatterLength;
   private @Nullable Vec3 hoverAt;
   private float summonStartHealth = 1.0F;
   private @Nullable Vec3 travelTo;
   private double travelSpeed;
   private int travelMax;
   private boolean travelTurning;
   private boolean travelLoop;
   private boolean travelAbort;
   private boolean travelPhase;
   private @Nullable DragonMove travelThen;
   private int fightPhase;
   private int pendingPhase = -1;
   private boolean transitioning;
   private boolean lastLight;
   private int combatTicks;
   private float poise = 160.0F;
   private int poiseIdle;
   private int poiseImmune;
   private boolean breakPending;
   private boolean staggered;
   private int groundedFor = 100;
   private float aegis;
   private float aegisCap;
   private int aegisIdle;
   private int aegisMessage;
   private float obsidian;
   private int obsidianTicks;
   private int reflectTicks;
   private int overextended;
   private @Nullable DamageSource lastSource;
   private int lastSourceTick;
   private int perchTicks;
   private float perchDamage;
   private int landingTimer = 900;
   private @Nullable EnderDragonPhase<?> lastPhase;
   private int vanillaTicks;
   private float lastYaw = Float.NaN;
   private Vec3 lastPos = Vec3.ZERO;
   private @Nullable ServerPlayer carried;
   private float carriedOwn;
   private float carriedOthers;
   private @Nullable ServerPlayer thrown;
   private int thrownTicks;
   private boolean orbFan;

   public DragonRework(EnderDragon dragon) {
      this.dragon = dragon;
   }

   private int state() {
      return ((DragonReworkHolder)this.dragon).mysticalterrain$state() & 0xFF;
   }

   private void setState(int state) {
      DragonReworkHolder holder = (DragonReworkHolder)this.dragon;
      int raw = holder.mysticalterrain$state();
      int serial = raw >>> 16 & 0xFF;
      if (state != 0) {
         serial = serial + 1 & 0xFF;
      }

      holder.mysticalterrain$setState(this.bits(state, serial));
      this.stateTicks = 0;
      this.stage = 0;
   }

   private void syncBits() {
      DragonReworkHolder holder = (DragonReworkHolder)this.dragon;
      int raw = holder.mysticalterrain$state();
      int bits = this.bits(raw & 0xFF, raw >>> 16 & 0xFF);
      if (bits != raw) {
         holder.mysticalterrain$setState(bits);
      }
   }

   private int bits(int state, int serial) {
      return state & 0xFF | (this.fightPhase & 3) << 8 | (this.enraged() ? 1024 : 0) | (serial & 0xFF) << 16;
   }

   public boolean steering() {
      int s = this.state();
      return s != 0 && s != 9;
   }

   public boolean holdsPhase() {
      return this.steering();
   }

   public boolean summoning() {
      return this.state() == 1;
   }

   public int fightPhase() {
      return this.fightPhase;
   }

   public int stateForTest() {
      return this.state();
   }

   EnderDragon dragon() {
      return this.dragon;
   }

   List<ServerPlayer> engaged() {
      return this.sense.engaged;
   }

   private boolean enraged() {
      return this.fightPhase >= 2 || this.combatTicks >= 18000;
   }

   float dmg(float base) {
      float f = base;
      if (this.fightPhase >= 2) {
         f *= 1.25F;
      }

      if (this.combatTicks >= 18000) {
         f *= 1.35F;
      }

      return f;
   }

   public void onFireball() {
      this.orbFan = true;
      if (this.state() == 0) {
         this.move = DragonMove.FIREBALL;
         this.setState(9);
      }
   }

   public void tick(ServerLevel level) {
      if (!this.initialized) {
         this.initialized = true;
         float frac = this.dragon.getHealth() / this.dragon.getMaxHealth();
         this.dragon.getAttribute(Attributes.MAX_HEALTH).setBaseValue(800.0);
         this.dragon.setHealth(800.0F * frac);
         this.hazards.refreshCrystals(level);
         if (this.state() != 1) {
            if (this.hazards.crystals().isEmpty()) {
               this.fightPhase = Math.max(1, this.fightPhase);
            }

            this.resetAegis();
         }
      }

      if (this.dying()) {
         if (this.state() != 0) {
            this.interrupt(level);
            this.setState(0);
         }

         if (this.dragon.getHealth() > 0.0F) {
            this.dragon.setHealth(0.0F);
         }
      } else {
         this.portal = this.portalTop(level);
         this.stateTicks++;

         for (int i = 0; i < this.cooldowns.length; i++) {
            if (this.cooldowns[i] > 0) {
               this.cooldowns[i]--;
            }
         }

         boolean active = this.state() != 1;
         if (active) {
            this.sense.tick(level, this.dragon, this.portal, this.bar());
            this.tickFight(level);
         }

         this.hazards.tick(level, this.portal, active && !this.blocked());
         if (this.orbFan) {
            this.orbFan = false;
            this.fanOrbs(level);
         }

         this.trackVanillaPhase(level);
         switch (this.state()) {
            case 0:
               this.tickNormal(level);
               break;
            case 1:
               this.tickSummon(level);
               break;
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 8:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
            default:
               this.tickMove(level);
               break;
            case 7:
               this.tickStunned(level);
               break;
            case 9:
               this.tickFireball(level);
               break;
            case 24:
               this.tickCrash(level);
               break;
            case 25:
               this.hover();
               if (this.stateTicks >= 28) {
                  this.setState(7);
               }
               break;
            case 26:
               this.tickRecover(level);
               break;
            case 27:
            case 28:
            case 29:
               this.tickPhaseClip(level);
               break;
            case 30:
            case 31:
               this.tickTravel(level);
         }

         this.capTurn();
         this.lastPos = this.dragon.position();
         this.syncBits();
         this.syncBar(level);
      }
   }

   private boolean dying() {
      return this.dragon.isDeadOrDying() || this.dragon.getPhaseManager().getCurrentPhase().getPhase() == EnderDragonPhase.DYING;
   }

   private void tickFight(ServerLevel level) {
      if (this.sense.engaged.isEmpty()) {
         if (++this.sense.idleTicks == 1800) {
            this.resetFight();
         }
      } else {
         this.sense.idleTicks = 0;
         if (++this.combatTicks == 18000) {
            this.softEnrage(level);
         }
      }

      if (this.poiseImmune > 0) {
         this.poiseImmune--;
      }

      if (this.overextended > 0) {
         this.overextended--;
      }

      if (this.reflectTicks > 0) {
         this.reflectTicks--;
      }

      if (this.aegisMessage > 0) {
         this.aegisMessage--;
      }

      if (this.obsidianTicks > 0 && --this.obsidianTicks == 0) {
         this.obsidian = 0.0F;
      }

      if (!this.staggered && ++this.poiseIdle > 80 && this.poise < 160.0F) {
         this.poise = Math.min(160.0F, this.poise + 2.3999999F);
      }

      if (this.fightPhase == 0 && ++this.aegisIdle > 100 && this.dragon.tickCount % 20 == 0 && this.aegis < this.aegisCap) {
         this.aegis = Math.min(this.aegisCap, this.aegis + 1.0F);
      }

      this.tickReflections();
      this.tickThrown(level);
      this.checkPhases(level);
      if (this.breakPending && this.canBreak()) {
         this.breakPoise(level);
      }

      EndCrystal crystal = this.dragon.nearestCrystal;
      if (crystal != null && !crystal.isRemoved() && this.dragon.tickCount % 3 == 0) {
         Vfx.beam(level, crystal.position().add(0.0, 1.2, 0.0), this.core(), V, 0.25, 4);
      }
   }

   private void resetFight() {
      this.sense.reset();
      float max = this.dragon.getMaxHealth();
      float top = this.fightPhase == 0 ? 1.0F : THRESHOLDS[this.fightPhase - 1];
      if (this.pendingPhase < 0 && this.dragon.getHealth() < top * max) {
         this.dragon.setHealth(top * max);
      }

      this.combatTicks = 0;
      if (this.fightPhase < 3) {
         this.hazards.stopEdge();
      }

      if (this.fightPhase == 0) {
         this.resetAegis();
      }
   }

   private void softEnrage(ServerLevel level) {
      Vec3 core = this.core();
      DragonFx.sound(level, core, "enrage", 10.0F, 0.9F);
      Vfx.pillar(level, this.dragon.position(), V, 40.0, 3.0);
      Vfx.screen(level, core, 128.0, 1.0, 24, 16777215, 0.4F);
      this.hazards.startEdge();
   }

   private void resetAegis() {
      if (this.fightPhase == 0) {
         this.aegisCap = 12.0F * this.hazards.crystals().size();
         this.aegis = this.aegisCap;
      }
   }

   private void checkPhases(ServerLevel level) {
      if (this.pendingPhase >= 0) {
         if (!this.transitioning) {
            this.beginTransition(level);
         }
      } else {
         float frac = this.dragon.getHealth() / this.dragon.getMaxHealth();
         int want = -1;
         if (this.fightPhase == 0 && this.hazards.crystals().isEmpty()) {
            want = 1;
         } else if (this.fightPhase == 1 && frac <= THRESHOLDS[1] + 1.0E-4F) {
            want = 2;
         } else if (this.fightPhase == 2 && frac <= THRESHOLDS[2] + 1.0E-4F) {
            want = 3;
         }

         if (want > 0) {
            this.pendingPhase = want;
            this.fightPhase = want;
            this.beginTransition(level);
         } else {
            if (this.fightPhase == 3 && !this.lastLight && frac <= 0.05F) {
               this.lastLight(level);
            }
         }
      }
   }

   private void beginTransition(ServerLevel level) {
      this.transitioning = true;
      this.forced.clear();
      this.breakPending = false;
      this.poise = 160.0F;
      DragonFx.sound(level, this.dragon.position(), "phase", 8.0F, 1.0F);
      if (this.grounded()) {
         this.interrupt(level);
         if (this.state() != 26) {
            this.setState(26);
            DragonFx.sound(level, this.dragon.position(), "recover.start", 6.0F, 1.0F);
         }
      } else {
         this.interrupt(level);
         this.flyToPhaseHover();
      }
   }

   private void flyToPhaseHover() {
      this.dragon.getPhaseManager().setPhase(EnderDragonPhase.HOVERING);
      this.travel(31, this.portal.add(0.0, 24.0, 0.0), 1.2, 100, null, false, false);
      this.travelPhase = true;
   }

   private void tickPhaseClip(ServerLevel level) {
      this.hover();
      int t = this.stateTicks;
      if (t == 28) {
         this.phaseBurst(level, this.pendingPhase);
      }

      if (t >= 60) {
         int done = this.pendingPhase;
         this.pendingPhase = -1;
         this.transitioning = false;
         this.poise = 160.0F;
         this.setState(0);
         this.dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
         this.rest = 20;
         this.resetLanding();
         if (done == 2) {
            this.forced.add(DragonMove.SHATTERDIVE);
         } else if (done == 3) {
            this.forced.add(DragonMove.OBLIVION_NOVA);
         }
      }
   }

   private void phaseBurst(ServerLevel level, int phase) {
      Vec3 core = this.core();
      if (phase == 1) {
         this.aegis = 0.0F;
         this.aegisCap = 0.0F;
         Vfx.nova(level, this.portal.add(0.0, 20.0, 0.0), V, 24.0);

         for (EndSpike s : EndSpikeFeature.getSpikesForLevel(level)) {
            Vec3 top = DragonFx.spikeTop(s).add(0.0, 1.0, 0.0);
            Vfx.shards(level, top, 16777215, 14, 0.6);
            Vfx.sparks(level, top, V.glow(), 10, 0.5, 0.2);
         }

         for (ServerPlayer p : DragonFx.players(level, this.portal, 16.0)) {
            Vec3 out = DragonFx.away(this.portal, p.position());
            DragonFx.launch(p, new Vec3(out.x * 1.2, 0.4, out.z * 1.2));
         }
      } else if (phase == 2) {
         Vfx.explosion(level, core, V, 6.0);
         Vfx.pillar(level, this.portal, V, 40.0, 4.0);

         for (ServerPlayer p : this.sense.engaged) {
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false), this.dragon);
         }

         this.hazards.openRifts(level, this.portal);
         DragonFx.sound(level, core, "enrage", 10.0F, 1.0F);
      } else {
         Vfx.impactFrame(level, core, 128.0, 16777215, 0.8F);
         Vfx.nova(level, core, V, 64.0);
         this.hazards.startEdge();
      }

      Vfx.shake(level, core, 96.0, 0.8, 20);
   }

   private void lastLight(ServerLevel level) {
      this.lastLight = true;
      Vfx.flash(level, this.core(), 128.0, 16777215, 0.5F);
      DragonFx.sound(level, this.core(), "roar", 10.0F, 0.7F);
      this.forced.clear();
      if (this.state() != 18) {
         this.forced.add(DragonMove.OBLIVION_NOVA);
      }

      this.forced.add(DragonMove.SHATTERDIVE);
      int s = this.state();
      boolean busy = s == 18 || this.grounded() || s == 4 || s == 17 || s == 8 || s == 5;
      if (!busy && s != 0) {
         this.interrupt(level);
         this.setState(0);
         if (!this.perched()) {
            this.dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
         }
      }

      this.rest = 0;
   }

   private void tickNormal(ServerLevel level) {
      EnderDragonPhase<?> vp = this.vanillaPhase();
      if (vp == EnderDragonPhase.HOVERING) {
         this.dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
         vp = EnderDragonPhase.HOLDING_PATTERN;
      }

      this.vanillaPerchMoves(level, vp);
      if (!this.forced.isEmpty()) {
         DragonMove m = this.forced.poll();
         this.start(level, m, this.sense.target());
      } else {
         this.tickPerch(vp);
         if (this.rest > 0) {
            this.rest--;
         } else {
            this.pick(level, vp);
         }
      }
   }

   private void tickPerch(EnderDragonPhase<?> vp) {
      if (this.perched()) {
         if (this.fightPhase >= 1 && this.perchTicks >= this.perchLimit() || this.perchDamage >= 96.0F) {
            this.perchTicks = 0;
            this.perchDamage = 0.0F;
            this.dragon.getPhaseManager().setPhase(EnderDragonPhase.TAKEOFF);
         }
      } else if (this.fightPhase >= 1 && vp == EnderDragonPhase.HOLDING_PATTERN && --this.landingTimer <= 0) {
         this.resetLanding();
         this.dragon.getPhaseManager().setPhase(EnderDragonPhase.LANDING_APPROACH);
      }
   }

   private int perchLimit() {
      return this.fightPhase >= 3 ? 240 : (this.fightPhase == 2 ? 320 : 400);
   }

   private void resetLanding() {
      int[] range = this.fightPhase >= 3 ? new int[]{520, 720} : (this.fightPhase == 2 ? new int[]{640, 900} : new int[]{800, 1100});
      this.landingTimer = range[0] + this.dragon.getRandom().nextInt(range[1] - range[0] + 1);
   }

   private void pick(ServerLevel level, EnderDragonPhase<?> vp) {
      boolean perched = this.perched();
      boolean air = vp == EnderDragonPhase.HOLDING_PATTERN || vp == EnderDragonPhase.STRAFE_PLAYER || vp == EnderDragonPhase.CHARGING_PLAYER;
      if ((perched || air) && vp != EnderDragonPhase.SITTING_ATTACKING) {
         ServerPlayer t = this.sense.target();
         if (t != null) {
            DragonMove[] all = DragonMove.values();
            float[] weights = new float[all.length];
            float total = 0.0F;

            for (DragonMove m : all) {
               if (m.pickable() && m.available(this.fightPhase) && this.cooldowns[m.ordinal()] <= 0 && m.where == DragonMove.Where.PERCH == perched) {
                  float u = this.utility(level, m, t);
                  if (!(u <= 0.0F)) {
                     float w = m.weight(this.fightPhase) * u * (m == this.last1 ? 0.25F : (m == this.last2 ? 0.6F : 1.0F));
                     weights[m.ordinal()] = w;
                     total += w;
                  }
               }
            }

            if (total <= 0.0F) {
               this.rest = 10;
            } else {
               float r = this.dragon.getRandom().nextFloat() * total;

               for (DragonMove m : all) {
                  float w = weights[m.ordinal()];
                  if (!(w <= 0.0F)) {
                     r -= w;
                     if (r <= 0.0F) {
                        this.start(level, m, t);
                        return;
                     }
                  }
               }
            }
         }
      }
   }

   private float utility(ServerLevel level, DragonMove m, ServerPlayer t) {
      DragonSense s = this.sense;
      double dist = t.position().distanceTo(this.dragon.position());

      return switch (m) {
         case STRAFE_BREATH -> t.onGround() && !(DragonFx.flatDist(t.position(), this.portal) > 100.0) ? (!s.kiting() && !s.healing ? 1.0F : 1.5F) : 0.0F;
         case RAKE_DIVE -> dist > 110.0 ? 0.0F : (!s.gliding && !s.pillaring ? 1.0F : 2.0F);
         case ROAR -> this.gliderWithin(28.0) ? 6.0F : 1.0F;
         case TALON_RAPTURE -> t.getY() < this.portal.y - 10.0 ? 0.0F : (s.gliding ? 3.0F : (s.pillaring ? 2.5F : (s.aiming ? 1.5F : 1.0F)));
         case WING_WARD -> !s.aiming && !(s.projectileShare() >= 0.5F) && !this.projectileInbound(level) ? 1.0F : 5.0F;
         case VOID_BEAM -> !s.aiming && !s.kiting() ? (s.healing ? 1.5F : 1.0F) : 2.0F;
         case GRAVITY_WELL -> !s.pillaring && s.airTicks < 60 && s.glidingTicks < 80 ? 1.0F : 8.0F;
         case METEOR_RAIN -> t.onGround() ? 1.0F : 0.0F;
         case CRYSTAL_RECALL -> DragonFx.emptySpikes(level).size() < 2 ? 0.0F : (this.belowMidBand() ? 2.0F : 1.0F);
         case SHATTERDIVE, OBLIVION_NOVA -> 1.0F;
         case BREATH_SWEEP -> this.playersInFront(100.0, 22.0) > 0 ? 1.0F : 0.0F;
         case TAIL_SWEEP -> this.playerBehindHips() ? 6.0F : 1.0F;
         case RENDING_BITE -> this.playerAtHead(7.0) ? (s.blocking ? 1.5F : 1.0F) : 0.0F;
         case VOID_FISSURE -> this.playersInFrontBetween(6.0, 22.0) ? 1.0F : 0.0F;
         case WING_STORM -> this.playersInFront(60.0, 10.0) < 2 && !s.hugging() ? 1.0F : 2.0F;
         case POUNCE -> t.onGround() && dist >= 8.0 && dist <= 20.0 && DragonFx.groundBelow(level, t.position().add(0.0, 1.0, 0.0)) != null
            ? (!s.aiming && !s.kiting() ? 1.0F : 2.0F)
            : 0.0F;
         default -> 0.0F;
      };
   }

   private boolean belowMidBand() {
      float frac = this.dragon.getHealth() / this.dragon.getMaxHealth();
      float top = this.fightPhase == 0 ? 1.0F : THRESHOLDS[this.fightPhase - 1];
      float bottom = this.fightPhase >= 3 ? 0.0F : THRESHOLDS[this.fightPhase];
      return frac < (top + bottom) * 0.5F;
   }

   private boolean gliderWithin(double r) {
      for (ServerPlayer p : this.sense.engaged) {
         if (p.isFallFlying() && p.distanceTo(this.dragon) <= r) {
            return true;
         }
      }

      return false;
   }

   private boolean projectileInbound(ServerLevel level) {
      Vec3 core = this.core();
      AABB box = this.dragon.getBoundingBox().inflate(24.0);

      for (Projectile p : level.getEntitiesOfClass(Projectile.class, box, e -> e.getOwner() instanceof Player)) {
         if (p.getDeltaMovement().lengthSqr() > 0.04 && p.getDeltaMovement().dot(core.subtract(p.position())) > 0.0) {
            return true;
         }
      }

      return false;
   }

   private int playersInFront(double halfAngle, double range) {
      int n = 0;
      Vec3 head = this.dragon.head.position();

      for (ServerPlayer p : this.sense.engaged) {
         if (Math.abs(p.getY() - this.dragon.getY()) < 8.0
            && DragonFx.inCone(this.dragon.position(), this.fwd(), halfAngle, range + 6.5, p.position())
            && p.position().distanceTo(head) <= range) {
            n++;
         }
      }

      return n;
   }

   private boolean playersInFrontBetween(double min, double max) {
      Vec3 head = this.dragon.head.position();

      for (ServerPlayer p : this.sense.engaged) {
         double d = DragonFx.flatDist(p.position(), head);
         if (d >= min && d <= max && DragonFx.angle(this.fwd(), p.position().subtract(this.dragon.position())) < 80.0) {
            return true;
         }
      }

      return false;
   }

   private boolean playerBehindHips() {
      Vec3 hips = this.hips();

      for (ServerPlayer p : this.sense.engaged) {
         if (DragonFx.flatDist(p.position(), hips) <= 12.0 && DragonFx.angle(this.fwd(), p.position().subtract(this.dragon.position())) > 60.0) {
            return true;
         }
      }

      return false;
   }

   private boolean playerAtHead(double range) {
      Vec3 head = this.dragon.head.position();

      for (ServerPlayer p : this.sense.engaged) {
         if (p.position().distanceTo(head) <= range && DragonFx.angle(this.fwd(), p.position().subtract(this.dragon.position())) < 70.0) {
            return true;
         }
      }

      return false;
   }

   private int restFor(boolean perched) {
      int[] r = (perched ? PERCH_REST : AIR_REST)[Mth.clamp(this.fightPhase, 0, 3)];
      int v = r[0] + this.dragon.getRandom().nextInt(r[1] - r[0] + 1);
      return this.combatTicks >= 18000 ? (int)(v * 0.7F) : v;
   }

   private void setCooldown(DragonMove m) {
      int cd = m.cooldownTicks(this.fightPhase);
      if (cd > 0) {
         this.cooldowns[m.ordinal()] = this.combatTicks >= 18000 ? (int)(cd * 0.7F) : cd;
      }
   }

   private void start(ServerLevel level, DragonMove m, @Nullable ServerPlayer t) {
      this.victim = t;
      this.setCooldown(m);
      this.last2 = this.last1;
      this.last1 = m;
      Vec3 pos = this.dragon.position();
      switch (m) {
         case STRAFE_BREATH: {
            Vec3 tp = t != null ? t.position() : pos.add(this.fwd().scale(30.0));
            Vec3 d = DragonFx.flat(tp.subtract(pos));
            d = d.lengthSqr() < 1.0 ? this.fwd() : d.normalize();
            Vec3 g = DragonFx.groundBelow(level, tp.add(0.0, 0.5, 0.0));
            double y = (g != null ? g.y : tp.y) + 7.0;
            this.pb = d;
            this.speed = y;
            this.travel(30, new Vec3(tp.x - d.x * 30.0, y, tp.z - d.z * 30.0), 1.6, 100, m, true, false);
            break;
         }
         case RAKE_DIVE:
            this.passes = this.fightPhase >= 3 ? 3 : (this.fightPhase == 2 && this.dragon.getRandom().nextBoolean() ? 2 : 1);
            this.pass = 1;
            this.begin(level, m);
            break;
         case ROAR:
         case WING_WARD:
         case VOID_BEAM:
         case GRAVITY_WELL:
         default:
            this.begin(level, m);
            break;
         case TALON_RAPTURE: {
            Vec3 tp = t != null ? t.position() : pos.add(this.fwd().scale(30.0));
            Vec3 d = DragonFx.flat(tp.subtract(pos));
            d = d.lengthSqr() < 1.0 ? this.fwd() : d.normalize();
            this.travel(30, tp.subtract(d.scale(24.0)).add(0.0, 10.0, 0.0), 1.6, 100, m, true, false);
            break;
         }
         case METEOR_RAIN:
            this.travel(31, this.portal.add(0.0, 28.0, 0.0), 1.2, 80, m, false, false);
            break;
         case CRYSTAL_RECALL:
            this.travel(31, this.portal.add(0.0, 32.0, 0.0), 1.0, 300, m, false, true);
            break;
         case SHATTERDIVE:
            this.beginClimb(level);
            break;
         case OBLIVION_NOVA:
            this.travel(31, this.novaPoint(level), 1.3, 100, m, false, false);
      }
   }

   private void begin(ServerLevel level, DragonMove m) {
      this.move = m;
      if (m.where == DragonMove.Where.HOVER) {
         this.dragon.getPhaseManager().setPhase(EnderDragonPhase.HOVERING);
      }

      this.setState(m.state);
      DragonFx.sound(level, this.dragon.position(), m.anim + ".start", 6.0F, 1.0F);
      this.tickMove(level);
   }

   private void travel(int state, Vec3 to, double speed, int max, @Nullable DragonMove then, boolean turning, boolean abort) {
      this.setState(state);
      this.move = null;
      this.travelTo = to;
      this.travelSpeed = speed;
      this.travelMax = max;
      this.travelThen = then;
      this.travelTurning = turning;
      this.travelAbort = abort;
      this.travelLoop = false;
      this.travelPhase = false;
   }

   private void travelLoop(Vec3 to, double speed, int ticks, DragonMove then) {
      this.travel(30, to, speed, ticks, then, true, false);
      this.travelLoop = true;
   }

   private void tickTravel(ServerLevel level) {
      Vec3 to = this.travelTo != null ? this.travelTo : this.dragon.position();
      boolean there;
      if (this.travelLoop) {
         this.flyTo(to, this.travelSpeed, 9.0F);
         there = this.stateTicks >= this.travelMax;
      } else if (this.travelTurning && this.stateTicks < 60) {
         there = this.flyTo(to, this.travelSpeed, 8.0F);
      } else {
         there = this.steerTo(to, this.travelSpeed);
      }

      if (this.state() == 30 && this.stateTicks % 3 == 0) {
         Vfx.trail(level, this.core(), V, 2.0);
      }

      if (there || this.stateTicks >= this.travelMax) {
         if (!there && this.travelAbort) {
            this.finishAir();
         } else if (this.travelPhase) {
            this.setState(27 + Mth.clamp(this.pendingPhase, 1, 3) - 1);
         } else {
            DragonMove next = this.travelThen;
            if (next == null) {
               this.finishAir();
            } else {
               this.begin(level, next);
            }
         }
      }
   }

   private boolean flyTo(Vec3 to, double speed, float turn) {
      Vec3 pos = this.dragon.position();
      Vec3 d = to.subtract(pos);
      if (d.length() < Math.max(3.0, speed)) {
         return true;
      }

      Vec3 flat = DragonFx.flat(d);
      float yaw = this.dragon.getYRot();
      if (flat.lengthSqr() > 1.0E-4) {
         yaw = Mth.approachDegrees(yaw, DragonFx.dragonYaw(flat), turn);
         this.dragon.setYRot(yaw);
      }

      Vec3 f = DragonFx.forward(yaw);
      double climb = Mth.clamp(d.y * 0.1, -speed * 0.6, speed * 0.6);
      this.dragon.setPos(pos.x + f.x * speed, pos.y + climb, pos.z + f.z * speed);
      this.dragon.setDeltaMovement(Vec3.ZERO);
      return false;
   }

   private boolean steerTo(@Nullable Vec3 to, double blocksPerTick) {
      if (to == null) {
         return true;
      }

      Vec3 pos = this.dragon.position();
      Vec3 d = to.subtract(pos);
      double dist = d.length();
      if (dist < Math.max(1.0, blocksPerTick)) {
         this.dragon.setPos(to.x, to.y, to.z);
         return true;
      }

      Vec3 step = d.scale(blocksPerTick / dist);
      this.dragon.setPos(pos.x + step.x, pos.y + step.y, pos.z + step.z);
      this.dragon.setDeltaMovement(Vec3.ZERO);
      if (d.horizontalDistanceSqr() > 1.0) {
         this.dragon.setYRot(Mth.approachDegrees(this.dragon.getYRot(), DragonFx.dragonYaw(d), 6.0F));
      }

      return false;
   }

   private void hover() {
      this.dragon.setDeltaMovement(Vec3.ZERO);
   }

   private void glide(double blocksPerTick) {
      Vec3 f = this.fwd();
      Vec3 pos = this.dragon.position();
      this.dragon.setPos(pos.x + f.x * blocksPerTick, pos.y, pos.z + f.z * blocksPerTick);
      this.dragon.setDeltaMovement(Vec3.ZERO);
   }

   private void turnToward(Vec3 dir, float maxDegrees) {
      Vec3 flat = DragonFx.flat(dir);
      if (flat.lengthSqr() > 1.0E-4) {
         this.dragon.setYRot(Mth.approachDegrees(this.dragon.getYRot(), DragonFx.dragonYaw(flat), maxDegrees));
      }
   }

   private void faceVictim(float maxDegrees) {
      if (this.victim != null && this.victim.isAlive()) {
         this.turnToward(this.victim.position().subtract(this.dragon.position()), maxDegrees);
      }
   }

   private void finishAir() {
      this.move = null;
      this.reflectTicks = 0;
      this.setState(0);
      this.dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
      this.rest = this.restFor(false);
   }

   private void finishPerch() {
      this.move = null;
      if (this.takeoffAfter) {
         this.takeoff();
      } else {
         this.setState(0);
         this.rest = this.restFor(true);
      }
   }

   private void takeoff() {
      this.takeoffAfter = false;
      this.move = null;
      this.setState(0);
      this.dragon.getPhaseManager().setPhase(EnderDragonPhase.TAKEOFF);
      this.rest = this.restFor(false);
   }

   private void interrupt(ServerLevel level) {
      if (this.carried != null) {
         this.carried = null;
      }

      this.reflectTicks = 0;
      this.move = null;
      this.takeoffAfter = false;
      this.travelThen = null;
      this.travelPhase = false;
      this.lines.clear();
   }

   private void tickMove(ServerLevel level) {
      DragonMove m = this.move;
      int t = this.stateTicks;
      if (m != null && m.state == this.state() && t > 0 && m.hitAt(t)) {
         DragonFx.sound(level, this.dragon.position(), m.anim + ".hit", 6.0F, 0.95F + this.dragon.getRandom().nextFloat() * 0.1F);
      }

      switch (this.state()) {
         case 2:
            this.tickBreathSweep(level);
            break;
         case 3:
            this.tickRecall(level);
            break;
         case 4:
            this.tickClimb(level);
            break;
         case 5:
            this.tickSlam(level);
            break;
         case 6:
            this.tickRoar(level);
            break;
         case 7:
         case 9:
         default:
            this.move = null;
            this.setState(0);
            break;
         case 8:
            this.tickPlunge(level);
            break;
         case 10:
            this.tickStrafeBreath(level);
            break;
         case 11:
            this.tickRake(level);
            break;
         case 12:
            this.tickTalon(level);
            break;
         case 13:
            this.tickWingWard(level);
            break;
         case 14:
            this.tickVoidBeam(level);
            break;
         case 15:
            this.tickGravityWell(level);
            break;
         case 16:
            this.tickMeteorRain(level);
            break;
         case 17:
            this.tickApex(level);
            break;
         case 18:
            this.tickNova(level);
            break;
         case 19:
            this.tickTailSweep(level);
            break;
         case 20:
            this.tickBite(level);
            break;
         case 21:
            this.tickFissure(level);
            break;
         case 22:
            this.tickWingStorm(level);
            break;
         case 23:
            this.tickPounce(level);
      }
   }

   private boolean hit(int t) {
      return this.move != null && this.move.hitAt(t);
   }

   private void tickFireball(ServerLevel level) {
      if (this.stateTicks == 4) {
         Vec3 mouth = this.mouth();
         Vfx.flare(level, mouth, 16777215, 2.6, 5);
         Vfx.flare(level, mouth, V.glow(), 4.5, 8);
         Vfx.sparks(level, mouth, V.glow(), 10, 0.4, 0.25);
      }

      if (this.stateTicks > 20) {
         this.move = null;
         this.setState(0);
      }
   }

   private void fanOrbs(ServerLevel level) {
      if (this.fightPhase >= 1) {
         AABB box = this.dragon.getBoundingBox().inflate(16.0);

         for (DragonFireball orb : level.getEntitiesOfClass(DragonFireball.class, box, e -> e.getOwner() == this.dragon && e.tickCount <= 1)) {
            this.onOrb(level, orb);
         }
      }
   }

   public void onOrb(ServerLevel level, DragonFireball orb) {
      if (this.fightPhase >= 1) {
         float[] angles = this.fightPhase >= 3 ? new float[]{-24.0F, -12.0F, 12.0F, 24.0F} : new float[]{-12.0F, 12.0F};
         Vec3 v = orb.getDeltaMovement();
         if (!(v.lengthSqr() < 1.0E-6)) {
            for (float deg : angles) {
               Vec3 dir = v.yRot(deg * (float) (Math.PI / 180.0));
               DragonFireball extra = new DragonFireball(level, this.dragon, dir.normalize());
               extra.snapTo(orb.getX(), orb.getY(), orb.getZ(), 0.0F, 0.0F);
               extra.setDeltaMovement(dir);
               level.addFreshEntity(extra);
            }
         }
      }
   }

   private void tickRoar(ServerLevel level) {
      int t = this.stateTicks;
      this.glide(0.3);
      Vec3 head = this.dragon.head.position();
      if (t == 0) {
         Vfx.chargeUp(level, this.mouth(), V, 12);
      }

      if (t == 12) {
         Vfx.nova(level, this.core(), V, 18.0);
         Vfx.shake(level, head, 64.0, 0.7, 20);

         for (ServerPlayer p : DragonFx.players(level, head, 18.0)) {
            Vec3 out = DragonFx.away(head, p.position());
            if (p.isFallFlying()) {
               Vec3 g = DragonFx.ground(level, p.getX(), p.getZ());
               if (g == null) {
                  DragonFx.launch(p, new Vec3(out.x * 0.8, 0.2, out.z * 0.8));
               } else {
                  p.stopFallFlying();
                  DragonFx.hurt(level, this.dragon, p, this.dmg(6.0F), false);
                  DragonFx.launch(p, new Vec3(out.x * 1.6, -0.4, out.z * 1.6));
                  if (p.getY() - g.y > 20.0) {
                     p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0), this.dragon);
                  }
               }
            } else {
               DragonFx.launch(p, new Vec3(out.x * 0.8, 0.3, out.z * 0.8));
               p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false), this.dragon);
            }
         }
      }

      if (t >= 48) {
         this.finishAir();
      }
   }

   private void tickStrafeBreath(ServerLevel level) {
      int t = this.stateTicks;
      Vec3 d = this.pb;
      this.turnToward(d, 8.0F);
      Vec3 pos = this.dragon.position();
      double y = pos.y + Mth.clamp(this.speed - pos.y, -0.4, 0.4);
      this.dragon.setPos(pos.x + d.x, y, pos.z + d.z);
      this.dragon.setDeltaMovement(Vec3.ZERO);
      if (t == 0) {
         Vec3 g = DragonFx.ground(level, pos.x, pos.z);
         Vfx.telegraphLine(level, g != null ? g : new Vec3(pos.x, this.speed - 7.0, pos.z), d, 52.0, 4.0, 16, V);
      }

      Vec3 mouth = this.at(4.3, -0.5);
      Vec3 g = DragonFx.ground(level, mouth.x + d.x * 2.0, mouth.z + d.z * 2.0);
      if (t >= 16 && t <= 50 && t % 2 == 0 && g != null) {
         Vfx.beam(level, mouth, g, V, 0.8);
         Vfx.smoke(level, g, V.smoke(), 3, 0.6, 1.2);
      }

      if (this.hit(t) && g != null) {
         for (ServerPlayer p : DragonFx.players(level, g, 6.0)) {
            if (DragonFx.flatDist(p.position(), g) <= 2.5 && Math.abs(p.getY() - g.y) <= 4.0) {
               DragonFx.hurt(level, this.dragon, p, this.dmg(7.0F), false);
            }
         }

         DragonFx.pool(level, this.dragon, g, 2.4F, this.fightPhase >= 2 ? 140 : 80);
         Vfx.impact(level, g, V, 0.8);
      }

      if (t >= 60) {
         this.finishAir();
      }
   }

   private void tickRake(ServerLevel level) {
      int t = this.stateTicks;
      ServerPlayer tg = this.victim;
      Vec3 pos = this.dragon.position();
      if (t == 0) {
         Vec3 aim = tg != null && tg.isAlive() ? predict(tg, 20.0) : pos.add(this.fwd().scale(24.0)).add(0.0, -20.0, 0.0);
         Vec3 g = DragonFx.groundBelow(level, aim.add(0.0, 1.0, 0.0));
         this.pa = g != null ? g : aim;
         this.pc = pos;
      }

      if (t <= 8) {
         Vec3 toP = DragonFx.flat(this.pa.subtract(pos));
         this.turnToward(toP, 8.0F);
         Vec3 f = this.fwd();
         this.dragon.setPos(pos.x + f.x * 0.5, pos.y + 0.375, pos.z + f.z * 0.5);
         if (t == 4) {
            Vec3 g0 = DragonFx.ground(level, pos.x, pos.z);
            Vec3 from = g0 != null ? g0 : new Vec3(pos.x, this.pa.y, pos.z);
            Vec3 line = DragonFx.flat(this.pa.subtract(from));
            Vfx.telegraphLine(level, from, line.lengthSqr() > 1.0E-4 ? line.normalize() : f, line.length() + 10.0, 5.0, 16, V);
         }

         if (t == 8) {
            Vec3 d = this.pa.add(0.0, 2.2, 0.0).subtract(this.dragon.position());
            this.speed = Mth.clamp(d.length() / 12.0, 1.2, 2.6);
            this.pb = d.lengthSqr() > 1.0E-4 ? d.normalize() : f;
            this.pc = this.dragon.position();
         }
      } else if (t <= 20) {
         Vec3 d = this.pa.add(0.0, 2.2, 0.0).subtract(pos);
         double step = Math.min(this.speed, d.length());
         if (step > 0.01) {
            Vec3 s = d.normalize().scale(step);
            this.dragon.setPos(pos.x + s.x, pos.y + s.y, pos.z + s.z);
         }

         this.turnToward(this.pb, 10.0F);
         if (t % 2 == 0) {
            Vfx.streaks(level, this.core(), this.pb.scale(-3.0), V, 4);
         }

         if (t == 20) {
            this.rakeHit(level);
         }
      } else {
         Vec3 h = DragonFx.flat(this.pb);
         h = h.lengthSqr() > 1.0E-4 ? h.normalize() : this.fwd();
         this.dragon.setPos(pos.x + h.x * 0.707, pos.y + 0.707, pos.z + h.z * 0.707);
      }

      this.dragon.setDeltaMovement(Vec3.ZERO);
      if (t >= 40) {
         if (this.pass < this.passes) {
            this.pass++;
            Vec3 back = tg != null && tg.isAlive() ? tg.position().add(0.0, 12.0, 0.0) : this.pa.add(0.0, 12.0, 0.0);
            this.travelLoop(back, 1.4, 40, DragonMove.RAKE_DIVE);
         } else if (this.passes >= 3 && this.fightPhase >= 3 && this.cooldowns[DragonMove.SHATTERDIVE.ordinal()] <= 0) {
            this.start(level, DragonMove.SHATTERDIVE, tg);
         } else {
            this.finishAir();
         }
      }
   }

   private void rakeHit(ServerLevel level) {
      Vec3 belly = this.grabPoint();
      Vec3 h = DragonFx.flat(this.pb);
      h = h.lengthSqr() > 1.0E-4 ? h.normalize() : this.fwd();

      for (ServerPlayer p : DragonFx.players(level, belly, 5.0)) {
         if (!(DragonFx.center(p).distanceTo(belly) > 3.5)) {
            DragonFx.hurt(level, this.dragon, p, this.dmg(14.0F), false);
            DragonFx.launch(p, new Vec3(h.x * 1.2, 0.9, h.z * 1.2));
         }
      }

      Vfx.claw(level, belly, DragonFx.fxYaw(h), 20.0F, 4.5, V);
      Vfx.dashTrail(level, this.pc, this.dragon.position(), V);
      Vfx.streaks(level, belly, this.pb.scale(4.0), V, 12);
      Vfx.impact(level, belly, V, 1.0);
   }

   private void tickTalon(ServerLevel level) {
      int t = this.stateTicks;
      ServerPlayer tg = this.victim;
      Vec3 pos = this.dragon.position();
      if (t == 0) {
         Vec3 at = tg != null && tg.isAlive() ? predict(tg, 20.0) : pos.add(this.fwd().scale(24.0)).add(0.0, -10.0, 0.0);
         Vec3 d = DragonFx.flat(at.subtract(pos));
         d = d.lengthSqr() < 1.0E-4 ? this.fwd() : d.normalize();
         this.pa = at.add(0.0, 0.9 - TALON_STRIKE[1], 0.0).subtract(d.scale(TALON_STRIKE[0]));
         Vec3 jaw = this.pa.add(d.scale(TALON_JAW[0]));
         Vec3 g = DragonFx.ground(level, jaw.x, jaw.z);
         double lift = g == null ? 0.0 : g.y + 0.6 - (this.pa.y + TALON_JAW[1]);
         if (lift > 0.0 && lift < 2.5) {
            this.pa = this.pa.add(0.0, lift, 0.0);
         }

         this.pb = d;
         this.speed = Mth.clamp(this.pa.distanceTo(pos) / 20.0, 1.2, 2.4);
         this.carried = null;
      }

      if (t < 20) {
         Vec3 d = this.pa.subtract(pos);
         double step = Math.min(this.speed, d.length());
         if (step > 0.01) {
            Vec3 s = d.normalize().scale(step);
            this.dragon.setPos(pos.x + s.x, pos.y + s.y, pos.z + s.z);
         }

         this.turnToward(this.pb, 8.0F);
         if (t == 2) {
            Vec3 g0 = DragonFx.ground(level, pos.x, pos.z);
            Vec3 from = g0 != null ? g0 : new Vec3(pos.x, this.pa.y + 0.1, pos.z);
            Vfx.telegraphLine(level, from, this.pb, DragonFx.flatDist(from, this.pa) + 4.0, 4.0, 18, V);
            if (tg != null) {
               Vec3 feet = DragonFx.groundBelow(level, tg.position().add(0.0, 0.5, 0.0));
               if (feet != null) {
                  Vfx.telegraphCircle(level, feet, 3.0, 18, V);
               }
            }
         }

         if (t % 2 == 0) {
            Vfx.streaks(level, this.core(), this.pb.scale(-3.0), V, 3);
         }
      } else if (t == 20) {
         this.grab(level);
      } else if (t < 64) {
         if (this.stage == 2 && t >= 32) {
            this.finishAir();
            return;
         }

         double ang = Math.atan2(pos.z - this.portal.z, pos.x - this.portal.x) + 0.05;
         Vec3 goal = new Vec3(this.portal.x + Math.cos(ang) * 20.0, this.portal.y + 14.0, this.portal.z + Math.sin(ang) * 20.0);
         Vec3 d = goal.subtract(pos);
         Vec3 flat = DragonFx.flat(d);
         Vec3 step = flat.length() > 1.0 ? flat.normalize() : flat;
         this.dragon.setPos(pos.x + step.x, pos.y + Mth.clamp(d.y, -0.35, 0.35), pos.z + step.z);
         this.turnToward(step, 8.0F);
         this.carryTick(level);
      }

      if (t == 64) {
         this.throwCarried(level);
      }

      this.dragon.setDeltaMovement(Vec3.ZERO);
      if (t >= 80) {
         this.finishAir();
      }
   }

   private void grab(ServerLevel level) {
      Vec3 talon = this.talonPoint();
      ServerPlayer best = null;
      if (this.victim != null && this.victim.isAlive() && DragonFx.center(this.victim).distanceTo(talon) <= 3.5) {
         best = this.victim;
      } else {
         double bestD = 3.5;

         for (ServerPlayer p : this.sense.engaged) {
            double d = DragonFx.center(p).distanceTo(talon);
            if (d <= bestD) {
               best = p;
               bestD = d;
            }
         }
      }

      if (best == null) {
         this.stage = 2;
         this.overextended = 40;
         Vfx.sparks(level, talon, 16777215, 12, 0.4, 0.2);
         Vfx.claw(level, talon, DragonFx.fxYaw(this.fwd()), 60.0F, 2.5, V);
      } else {
         this.stage = 1;
         this.carried = best;
         this.carriedOwn = 0.0F;
         this.carriedOthers = 0.0F;
         best.stopFallFlying();
         DragonFx.hurt(level, this.dragon, best, this.dmg(8.0F), false);
         best.sendOverlayMessage(Component.translatableWithFallback("message.mysticalterrain.dragon.grabbed", "Strike the dragon to break free!"));
         Vfx.impact(level, talon, V, 1.2);
         Vfx.claw(level, talon, DragonFx.fxYaw(this.fwd()), 60.0F, 3.0, V);
      }
   }

   private void carryTick(ServerLevel level) {
      ServerPlayer p = this.carried;
      if (p != null) {
         if (p.isAlive() && !p.isSpectator() && p.level() == level) {
            Vec3 talon = this.carryPoint();
            if (!(DragonFx.center(p).distanceTo(talon) > 6.0) && !(this.carriedOwn >= 14.0F) && !(this.carriedOthers >= 40.0F)) {
               Vec3 velocity = this.dragon.position().subtract(this.lastPos);
               Vec3 hang = talon.add(0.0, -1.7, 0.0);
               DragonFx.launch(p, hang.subtract(p.position()).scale(0.6).add(velocity));
               p.resetFallDistance();
               if (this.stateTicks % 4 == 0) {
                  Vfx.streaks(level, talon, velocity.scale(-4.0), V, 3);
               }
            } else {
               this.breakFree(level);
            }
         } else {
            this.carried = null;
         }
      }
   }

   private void breakFree(ServerLevel level) {
      ServerPlayer p = this.carried;
      this.carried = null;
      if (p != null) {
         this.stage = 3;
         this.sense.threat.add(p, 15.0F);
         this.losePoise(40.0F);
         Vec3 talon = this.carryPoint();
         Vfx.shards(level, talon, 16777215, 16, 0.5);
         Vfx.flare(level, talon, 16777215, 3.0, 6);
         DragonFx.sound(level, talon, "hurt", 5.0F, 1.1F);
      }
   }

   private void throwCarried(ServerLevel level) {
      ServerPlayer p = this.carried;
      this.carried = null;
      if (p != null) {
         Vec3 out = DragonFx.flat(p.position().subtract(this.portal));
         out = out.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : out.normalize();
         Vec3 aim = this.portal.add(out.scale(8.0 + this.dragon.getRandom().nextDouble() * 6.0));
         Vec3 d = DragonFx.flat(aim.subtract(p.position()));
         d = d.lengthSqr() > 1.0E-4 ? d.normalize() : out.scale(-1.0);
         DragonFx.launch(p, new Vec3(d.x * 1.8, -1.4, d.z * 1.8));
         this.thrown = p;
         this.thrownTicks = 0;
         Vfx.streaks(level, DragonFx.center(p), new Vec3(d.x * 4.0, -3.0, d.z * 4.0), V, 10);
      }
   }

   public List<Entity> spare(List<Entity> hit) {
      if (this.carried == null && this.thrown == null) {
         return hit;
      }

      List<Entity> out = new ArrayList<>(hit);
      out.remove(this.carried);
      out.remove(this.thrown);
      return out;
   }

   private void tickThrown(ServerLevel level) {
      ServerPlayer p = this.thrown;
      if (p != null) {
         this.thrownTicks++;
         if (p.isAlive() && p.level() == level && this.thrownTicks <= 60) {
            p.resetFallDistance();
            if (this.thrownTicks > 2 && p.onGround()) {
               this.thrown = null;
               Vec3 at = p.position();
               DragonFx.hurt(level, this.dragon, p, this.dmg(12.0F), false);
               Vfx.heavyImpact(level, at, V, 0.8);
               DragonFx.sound(level, at, "talon_rapture.hit", 5.0F, 0.8F);

               for (ServerPlayer o : DragonFx.players(level, at, 3.0)) {
                  if (o != p) {
                     DragonFx.hurt(level, this.dragon, o, this.dmg(5.0F), false);
                     Vec3 away = DragonFx.away(at, o.position());
                     DragonFx.launch(o, new Vec3(away.x * 0.8, 0.3, away.z * 0.8));
                  }
               }
            }
         } else {
            this.thrown = null;
         }
      }
   }

   private void tickWingWard(ServerLevel level) {
      int t = this.stateTicks;
      this.hover();
      this.faceVictim(6.0F);
      if (t == 8) {
         this.obsidian = 48.0F;
         this.obsidianTicks = 160;
         this.reflectTicks = 36;
         Vfx.shieldBubble(level, this.core(), 5.0, V, 36);
         Vfx.flare(level, this.core(), 16777215, 5.0, 8);
      }

      if (t == 44) {
         Vec3 core = this.core();
         Vfx.shockwave(level, core, V, 8.0);
         Vfx.streaks(level, core, this.fwd().scale(4.0), V, 10);

         for (ServerPlayer p : DragonFx.players(level, core, 8.0)) {
            Vec3 out = DragonFx.away(core, p.position());
            DragonFx.launch(p, new Vec3(out.x * 0.6, 0.3, out.z * 0.6));
         }
      }

      if (t >= 50) {
         this.finishAir();
      }
   }

   private void tickVoidBeam(ServerLevel level) {
      int t = this.stateTicks;
      ServerPlayer tg = this.victim;
      Vec3 pos = this.dragon.position();
      if (t == 0) {
         this.pa = tg != null ? tg.position() : pos.add(this.fwd().scale(20.0)).add(0.0, -18.0, 0.0);
      }

      this.hover();
      Vec3 g = DragonFx.ground(level, pos.x, pos.z);
      double wantY = (g != null ? g.y : pos.y - 18.0) + 18.0;
      this.dragon.setPos(pos.x, pos.y + Mth.clamp(wantY - pos.y, -0.25, 0.25), pos.z);
      this.turnToward(this.pa.subtract(pos), 5.0F);
      if (tg != null && tg.isAlive()) {
         double cap = t < 30 ? 0.35 : 0.22;
         Vec3 d = tg.position().subtract(this.pa);
         this.pa = d.length() <= cap ? tg.position() : this.pa.add(d.normalize().scale(cap));
      }

      Vec3 mouth = this.jaw(BEAM_JAW, t);
      if (t == 0) {
         Vfx.chargeUp(level, this.jaw(BEAM_JAW, 30), V, 30);
         Vec3 g0 = g != null ? g : new Vec3(pos.x, this.pa.y, pos.z);
         Vec3 dir = DragonFx.flat(this.pa.subtract(g0));
         Vfx.telegraphLine(level, g0, dir.lengthSqr() > 1.0E-4 ? dir.normalize() : this.fwd(), Math.max(4.0, dir.length()), 2.0, 30, V);
      }

      if (t < 30 && t % 4 == 0) {
         Vfx.beam(level, mouth, this.pa, V, 0.12, 4);
      }

      if (t >= 30 && t <= 66 && t % 2 == 0) {
         Vec3 aim = this.pa.subtract(mouth);
         Vec3 end = DragonFx.rayEnd(level, mouth, this.pa.add(aim.normalize().scale(8.0)), this.dragon);
         Vfx.beam(level, mouth, end, V, 1.2);
         if (this.hit(t)) {
            for (ServerPlayer p : DragonFx.players(level, mouth, 80.0)) {
               Vec3 c = DragonFx.center(p);
               if (c.distanceTo(end) <= 2.2 || DragonFx.toSegment(c, mouth, end) <= 1.5) {
                  DragonFx.hurt(level, this.dragon, p, this.dmg(7.0F), false);
               }
            }

            Vfx.groundCrack(level, end, 1.6, V);
            Vfx.impact(level, end, V, 0.7);
            if (this.fightPhase >= 2 && (t - 30) % 16 == 8) {
               DragonFx.pool(level, this.dragon, end, 2.0F, 100);
            }
         }
      }

      if (t >= 70) {
         this.finishAir();
      }
   }

   private void tickGravityWell(ServerLevel level) {
      int t = this.stateTicks;
      ServerPlayer tg = this.victim;
      this.hover();
      this.faceVictim(5.0F);
      if (t == 0) {
         Vfx.chargeUp(level, this.jaw(WELL_JAW, 24), V, 24);
         this.pa = this.dragon.position().add(this.fwd().scale(12.0)).add(0.0, -10.0, 0.0);
      }

      if (t == 10) {
         Vec3 at = tg != null && tg.isAlive() ? predict(tg, 10.0) : this.pa;
         this.pa = at.add(0.0, 1.0, 0.0);
         Vec3 g = DragonFx.groundBelow(level, this.pa);
         Vfx.telegraphCircle(level, g != null ? g : this.pa, 8.0, 22, V);
      }

      if (t == 24) {
         Vfx.vortex(level, this.pa, 8.0, 8.0, V, 48);
      }

      if (t >= 24 && t < 68) {
         if (t % 4 == 0) {
            Vfx.beam(level, this.jaw(WELL_JAW, t), this.pa, V, 0.3);
         }

         for (ServerPlayer p : DragonFx.players(level, this.pa, 8.0)) {
            Vec3 in = this.pa.subtract(p.position());
            Vec3 pull = in.lengthSqr() > 1.0E-4 ? in.normalize().scale(0.09) : Vec3.ZERO;
            DragonFx.nudge(p, pull.add(0.0, -0.06, 0.0));
         }

         if (this.hit(t)) {
            for (ServerPlayer p : DragonFx.players(level, this.pa, 3.0)) {
               DragonFx.hurt(level, this.dragon, p, this.dmg(5.0F), true);
            }

            Vfx.flare(level, this.pa, 16777215, 3.0, 6);
            Vfx.ring(level, this.pa, V.glow(), 3.0, 8);
         }
      }

      if (t == 68) {
         Vec3 g = DragonFx.groundBelow(level, this.pa);
         Vfx.nova(level, this.pa, V, 8.0);
         Vfx.heavyImpact(level, g != null ? g : this.pa, V, 1.0);

         for (ServerPlayer p : DragonFx.players(level, this.pa, 8.0)) {
            DragonFx.hurt(level, this.dragon, p, this.dmg(8.0F), false);
            if (p.isFallFlying()) {
               p.stopFallFlying();
            }

            Vec3 out = DragonFx.away(this.pa, p.position());
            DragonFx.launch(p, new Vec3(out.x * 0.7, -1.0, out.z * 0.7));
         }
      }

      if (t >= 80) {
         this.finishAir();
      }
   }

   private void tickMeteorRain(ServerLevel level) {
      int t = this.stateTicks;
      this.hover();
      this.faceVictim(5.0F);
      if (t == 0) {
         Vfx.chargeUp(level, this.core(), V, 30);
      }

      if (this.hit(t)) {
         List<ServerPlayer> engaged = this.sense.engaged;
         int n = Math.min(5, 2 + Math.max(0, engaged.size() - 1)) + (this.fightPhase >= 3 ? 1 : 0);
         int volley = (t - 30) / 10;

         for (int i = 0; i < n; i++) {
            Vec3 at = null;
            if (i < n - 1 && !engaged.isEmpty()) {
               ServerPlayer p = engaged.get((volley + i) % engaged.size());
               at = DragonFx.groundBelow(level, predict(p, 20.0).add(0.0, 1.0, 0.0));
            }

            if (at == null) {
               at = this.randomIsland(level, 32.0);
            }

            if (at != null) {
               this.hazards.meteor(level, at, false);
            }
         }

         if (t == 80 && this.victim != null && this.victim.isAlive()) {
            Vec3 at = DragonFx.groundBelow(level, predict(this.victim, 20.0).add(0.0, 1.0, 0.0));
            if (at != null) {
               this.hazards.meteor(level, at, true);
            }
         }

         Vfx.flare(level, this.core(), 16777215, 4.0, 6);
      }

      if (t >= 100) {
         this.finishAir();
      }
   }

   private @Nullable Vec3 randomIsland(ServerLevel level, double radius) {
      for (int i = 0; i < 10; i++) {
         double a = this.dragon.getRandom().nextDouble() * Math.PI * 2.0;
         double r = Math.sqrt(this.dragon.getRandom().nextDouble()) * radius;
         Vec3 g = DragonFx.ground(level, this.portal.x + Math.cos(a) * r, this.portal.z + Math.sin(a) * r);
         if (g != null && Math.abs(g.y - this.portal.y) < 16.0) {
            return g;
         }
      }

      return null;
   }

   private void tickRecall(ServerLevel level) {
      int t = this.stateTicks;
      this.hover();
      Vec3 core = this.core();
      if (t == 0) {
         this.recall.clear();
         List<EndSpike> empty = new ArrayList<>(DragonFx.emptySpikes(level));
         int want = this.fightPhase >= 2 ? 3 : 2;

         while (!empty.isEmpty() && this.recall.size() < want) {
            this.recall.add(empty.remove(this.dragon.getRandom().nextInt(empty.size())));
         }

         this.channelDamage = 0.0F;
         this.channelLength = this.fightPhase >= 2 ? 120 : 140;

         for (EndSpike spike : this.recall) {
            Vfx.pillar(level, DragonFx.spikeTop(spike), V, 16.0, 1.4);
         }

         level.playSound(null, core.x, core.y, core.z, MysticalBosses.DRAGON_CHANNEL, SoundSource.HOSTILE, 6.0F, 0.8F);
      }

      if (t % 6 == 0) {
         for (EndSpike spike : this.recall) {
            Vfx.beam(level, DragonFx.spikeTop(spike).add(0.0, 1.0, 0.0), core, V, 0.45, 8);
         }
      }

      if (t % 10 == 0) {
         Vfx.flare(level, core, 16777215, 3.0 + t / 60.0, 12);
         Vfx.ring(level, core, V.glow(), 6.0, 12);
      }

      if (t > 0 && t % 20 == 0) {
         level.playSound(null, core.x, core.y, core.z, MysticalBosses.DRAGON_CHANNEL, SoundSource.HOSTILE, 4.0F, 0.6F + t / 300.0F);
      }

      if (this.channelDamage >= (this.fightPhase >= 2 ? 55 : 40)) {
         Vfx.explosion(level, core, V, 4.5);
         DragonFx.sound(level, core, "hurt", 6.0F, 0.6F);
         this.staggered = true;
         this.breakPending = false;
         this.groundedFor = 120;
         this.move = null;
         this.startCrash(level);
      } else {
         if (t >= this.channelLength) {
            boolean shroud = this.fightPhase >= 2;

            for (EndSpike spike : this.recall) {
               EndCrystal crystal = DragonFx.rebuildCrystal(level, spike);
               if (shroud) {
                  this.hazards.shroud(level, crystal, 200);
               }

               this.aegis += 12.0F;
            }

            this.aegisCap = Math.max(this.aegisCap, this.aegis);
            this.hazards.refreshCrystals(level);
            EnderDragonFight fight = this.dragon.getDragonFight();
            if (fight != null) {
               ((EnderDragonFightInvoker)fight).mysticalterrain$updateCrystalCount();
            }

            this.recall.clear();
            this.dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
            this.begin(level, DragonMove.ROAR);
         }
      }
   }

   private void beginClimb(ServerLevel level) {
      ServerPlayer t = this.victim;
      Vec3 p = this.portal;
      if (t != null && t.isAlive() && DragonFx.flatDist(t.position(), this.portal) > 20.0) {
         Vec3 pr = predict(t, 60.0);
         Vec3 off = new Vec3(pr.x - this.portal.x, 0.0, pr.z - this.portal.z);
         if (off.length() > 36.0) {
            off = off.normalize().scale(36.0);
         }

         Vec3 g = DragonFx.ground(level, this.portal.x + off.x, this.portal.z + off.z);
         if (g != null && Math.abs(g.y - this.portal.y) < 20.0) {
            p = g;
         }
      }

      this.pa = p;
      this.move = DragonMove.SHATTERDIVE;
      this.dragon.getPhaseManager().setPhase(EnderDragonPhase.HOVERING);
      this.setState(4);
      this.shatterStart = this.dragon.tickCount;
      this.shatterLength = (int)(this.dragon.position().distanceTo(p.add(0.0, 58.0, 0.0)) / 1.3) + 50 + 20;
      level.playSound(null, this.dragon.getX(), this.dragon.getY(), this.dragon.getZ(), MysticalBosses.DRAGON_ROAR, SoundSource.HOSTILE, 8.0F, 0.7F);
   }

   private void tickClimb(ServerLevel level) {
      if (this.stateTicks % 2 == 0) {
         Vfx.trail(level, this.core(), V, 2.5);
      }

      if (this.steerTo(this.pa.add(0.0, 58.0, 0.0), 1.3) || this.stateTicks > 200) {
         this.shatterLength = this.dragon.tickCount - this.shatterStart + 50 + 20;
         this.move = DragonMove.SHATTERDIVE;
         this.setState(17);
         DragonFx.sound(level, this.dragon.position(), "shatterdive.start", 10.0F, 1.0F);
         this.tickApex(level);
      }
   }

   private void tickApex(ServerLevel level) {
      int t = this.stateTicks;
      this.hover();
      if (t == 0) {
         Vfx.telegraphCircle(level, this.pa, 14.0, 70, V);
         Vfx.runeCircle(level, this.pa, 6.0, V, 70, -0.05F, Vfx.Rune.ASTRAL);
      }

      if (t % 4 == 0) {
         Vfx.gather(level, this.core(), V, 10.0, 20);
      }

      if (t >= 48) {
         this.move = DragonMove.DIVE;
         this.setState(8);
         this.speed = 0.6;
         this.pc = this.dragon.position();
         DragonFx.sound(level, this.dragon.position(), "dive.start", 8.0F, 1.0F);
      }
   }

   private void tickPlunge(ServerLevel level) {
      this.speed = Math.min(3.6, this.speed + 0.25);
      Vec3 from = this.dragon.position();
      if (this.stage == 0 && from.distanceTo(this.pa) < 6.0 * this.speed) {
         this.stage = 1;
         DragonFx.sound(level, this.pa, "slam.start", 8.0F, 1.0F);
      }

      boolean there = this.steerTo(this.pa, this.speed);
      if (this.stateTicks % 2 == 0) {
         Vfx.dashTrail(level, from.add(0.0, 2.0, 0.0), this.dragon.position().add(0.0, 2.0, 0.0), V);
         Vfx.streaks(level, this.core(), new Vec3(0.0, 4.0, 0.0), V, 6);
      }

      if (there || this.stateTicks > 80) {
         this.move = DragonMove.SLAM;
         this.dragon.getPhaseManager().setPhase(EnderDragonPhase.SITTING_SCANNING);
         this.setState(5);
         this.tickSlam(level);
      }
   }

   private void tickSlam(ServerLevel level) {
      int t = this.stateTicks;
      this.hover();
      if (t == 2) {
         this.slamImpact(level, this.pa);
      }

      if (t >= 40) {
         this.move = null;
         this.takeoffAfter = true;
         DragonMove next = this.fightPhase >= 3
            ? DragonMove.WING_STORM
            : (this.fightPhase == 2 && this.dragon.getRandom().nextBoolean() ? DragonMove.TAIL_SWEEP : null);
         if (next != null) {
            this.setCooldown(next);
            this.begin(level, next);
         } else {
            this.takeoff();
         }
      }
   }

   private void slamImpact(ServerLevel level, Vec3 at) {
      Vfx.heavyImpact(level, at, V, 2.5);
      Vfx.groundCrack(level, at, 12.0, V);
      Vfx.impactFrame(level, at, 96.0, 16777215, 0.7F);
      Vfx.pillar(level, at, V, 40.0, 4.0);

      for (ServerPlayer p : DragonFx.players(level, at, 30.0)) {
         float falloff = (float)Math.max(0.0, 1.0 - p.position().distanceTo(at) / 30.0);
         DragonFx.hurt(level, this.dragon, p, this.dmg(6.0F + 20.0F * falloff), false);
         Vec3 out = DragonFx.away(at, p.position());
         double push = 1.2 + 2.2 * falloff;
         DragonFx.launch(p, new Vec3(out.x * push, 0.6 + 0.6 * falloff, out.z * push));
      }

      double r = this.fightPhase >= 3 ? 8.0 : 9.0;

      for (int i = 0; i < 4; i++) {
         double a = i * Math.PI / 2.0 + (Math.PI / 4);
         Vec3 g = DragonFx.ground(level, at.x + Math.cos(a) * r, at.z + Math.sin(a) * r);
         if (g != null) {
            DragonFx.pool(level, this.dragon, g, 3.5F, 160);
         }
      }
   }

   private Vec3 novaPoint(ServerLevel level) {
      List<EndSpike> spikes = EndSpikeFeature.getSpikesForLevel(level);
      double ang;
      if (!spikes.isEmpty()) {
         EndSpike s = spikes.get(this.dragon.getRandom().nextInt(spikes.size()));
         ang = Math.atan2(s.getCenterZ() + 0.5 - this.portal.z, s.getCenterX() + 0.5 - this.portal.x);
      } else {
         ang = this.dragon.getRandom().nextDouble() * Math.PI * 2.0;
      }

      ang += (this.dragon.getRandom().nextFloat() * 40.0F - 20.0F) * (float) (Math.PI / 180.0);
      double x = this.portal.x + Math.cos(ang) * 34.0;
      double z = this.portal.z + Math.sin(ang) * 34.0;
      Vec3 g = DragonFx.ground(level, x, z);
      return new Vec3(x, (g != null ? g.y : this.portal.y) + 8.0, z);
   }

   private void tickNova(ServerLevel level) {
      int t = this.stateTicks;
      this.hover();
      this.turnToward(this.portal.subtract(this.dragon.position()), 4.0F);
      Vec3 core = this.core();
      if (t == 0) {
         Vfx.chargeUp(level, core, V, 96);
         Vec3 g = DragonFx.ground(level, this.dragon.getX(), this.dragon.getZ());
         Vfx.runeCircle(level, g != null ? g : this.dragon.position().add(0.0, -8.0, 0.0), 6.0, V, 96, 0.03F, Vfx.Rune.ASTRAL);

         for (ServerPlayer p : this.sense.engaged) {
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0, false, false), this.dragon);
            p.sendOverlayMessage(Component.translatableWithFallback("message.mysticalterrain.dragon.oblivion", "Break its line of sight!"));
         }
      }

      if (t < 96 && t % 8 == 0) {
         Vfx.gather(level, core, V, 8.0 + t / 12.0, 16);
      }

      if (t >= 10 && t < 96 && t % 10 == 0) {
         Vec3 head = this.jaw(NOVA_JAW, t);

         for (ServerPlayer p : this.sense.engaged) {
            if (DragonFx.clear(level, head, p.getEyePosition(), this.dragon)) {
               Vfx.beam(level, head, p.getEyePosition(), V, 0.12, 10);
            }
         }
      }

      if (t == 96) {
         Vec3 head = this.jawAt(NOVA_JAW, 4.8);
         Vfx.impactFrame(level, core, 160.0, 16777215, 1.0F);
         Vfx.nova(level, core, V, 64.0);
         Vfx.explosion(level, core, V, 6.0);

         for (ServerPlayer p : this.sense.engaged) {
            if (!(p.position().distanceTo(core) > 96.0)) {
               if (DragonFx.clear(level, head, p.getEyePosition(), this.dragon)) {
                  DragonFx.hurt(level, this.dragon, p, this.dmg(30.0F), true);
                  p.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1), this.dragon);
                  DragonFx.nudge(p, new Vec3(0.0, 0.6, 0.0));
               } else {
                  Vfx.shake(level, p.position(), 3.0, 0.3, 10);
               }
            }
         }
      }

      if (t >= 120) {
         this.finishAir();
      }
   }

   private void tickBreathSweep(ServerLevel level) {
      int t = this.stateTicks;
      Vec3 mouth = this.jaw(SWEEP_JAW, t);
      if (t == 0) {
         Vfx.telegraphCone(level, this.headGround(level), DragonFx.fxYaw(this.fwd()), 70.0F, 20.0, 16, V);
         Vfx.chargeUp(level, this.jaw(SWEEP_JAW, 11), V, 13);
      }

      if (t < 13 && t % 2 == 0) {
         Vfx.gather(level, mouth, V, 5.0, 14);
      }

      float s = t / 20.0F;
      if (s >= 0.65F && s <= 2.55F) {
         float k = (s - 0.65F) / 1.9F;
         k = k < 0.5F ? 4.0F * k * k * k : 1.0F - (float)Math.pow(-2.0F * k + 2.0F, 3.0) / 2.0F;
         float angle = (this.dragon.getYRot() - 50.0F + 100.0F * k) * (float) (Math.PI / 180.0);
         Vec3 dir = new Vec3(Mth.sin(angle), -0.35, -Mth.cos(angle)).normalize();
         if (t % 2 == 0) {
            Vfx.beam(level, mouth, mouth.add(dir.scale(18.0)), V, 0.9);
            Vfx.emit(level, VfxOptions.of(VfxParticles.Kind.SPARK, 16777215, 0.35, 12).streak(3.0F).gravity(0.0F).drag(0.96F).build(), mouth, dir.scale(1.2));

            for (int i = 0; i < 5; i++) {
               Vfx.smoke(level, mouth.add(dir.scale(3 + i * 3)), 14211288, 3, 0.4, 1.2);
            }
         }

         if (this.hit(t)) {
            Vec3 flat = DragonFx.flat(dir).normalize();
            Vec3 a = mouth.add(flat.scale(3.0));
            Vec3 b = mouth.add(flat.scale(18.0));

            for (ServerPlayer p : DragonFx.players(level, this.dragon.position(), 28.0)) {
               if (Math.abs(p.getY() - this.dragon.getY()) <= 8.0 && DragonFx.toSegmentFlat(p.position(), a, b) <= 2.5) {
                  DragonFx.hurt(level, this.dragon, p, this.dmg(8.0F), false);
               }
            }

            double along = 6.0 + this.dragon.getRandom().nextDouble() * 10.0;
            Vec3 g = DragonFx.groundBelow(level, mouth.add(flat.scale(along)));
            if (g != null) {
               DragonFx.pool(level, this.dragon, g, 2.6F, this.fightPhase >= 2 ? 160 : 100);
            }
         }
      }

      if (t >= 60) {
         this.finishPerch();
      }
   }

   private void tickTailSweep(ServerLevel level) {
      int t = this.stateTicks;
      Vec3 hips = this.hips();
      if (t == 0) {
         Vec3 g = DragonFx.groundBelow(level, hips.add(0.0, 1.0, 0.0));
         Vfx.telegraphCircle(level, g != null ? g : hips, 11.0, 21, V);
      }

      if (t == 21) {
         Vec3 f = this.fwd();

         for (ServerPlayer p : DragonFx.players(level, hips, 14.0)) {
            if (!(DragonFx.flatDist(p.position(), hips) > 11.0)) {
               Vec3 fromBody = p.position().subtract(this.dragon.position());
               if (!(DragonFx.flat(fromBody).lengthSqr() > 1.0) || !(DragonFx.angle(f, fromBody) < 60.0)) {
                  DragonFx.hurt(level, this.dragon, p, this.dmg(14.0F), false);
                  Vec3 out = DragonFx.away(hips, p.position());
                  DragonFx.launch(p, new Vec3(out.x * 1.6, 0.5, out.z * 1.6));
               }
            }
         }

         Vfx.slashArc(level, hips.add(0.0, 1.2, 0.0), DragonFx.fxYaw(f.scale(-1.0)), 0.0F, 11.0, V, 240.0F);
         Vfx.streaks(level, hips.add(0.0, 1.0, 0.0), this.right().scale(5.0), V, 10);
      }

      if (t >= 44) {
         this.finishPerch();
      }
   }

   private void tickBite(ServerLevel level) {
      int t = this.stateTicks;
      this.faceVictim(4.0F);
      Vec3 f = this.fwd();
      if (t == 2 || t == 13) {
         Vfx.telegraphCone(level, this.headGround(level), DragonFx.fxYaw(f), 35.0F, t == 2 ? 7.0 : 8.0, 8, V);
      }

      if (t == 10 || t == 21) {
         boolean second = t == 21;
         double range = second ? 8.0 : 7.0;
         Vec3 head = this.dragon.head.position();
         Vec3 origin = head.subtract(f.scale(1.5));

         for (ServerPlayer p : DragonFx.players(level, head, range + 3.0)) {
            if (!(Math.abs(p.getY() - head.y) > 5.0) && DragonFx.inCone(origin, f, 35.0, range + 1.5, p.position())) {
               if (second && p.isBlocking()) {
                  ItemStack shield = p.getUseItem();
                  p.getCooldowns().addCooldown(shield, 100);
                  p.stopUsingItem();
                  DragonFx.hurt(level, this.dragon, p, this.dmg(14.0F) * 0.5F, false);
                  Vfx.shards(level, DragonFx.center(p), 16777215, 10, 0.4);
               } else {
                  DragonFx.hurt(level, this.dragon, p, this.dmg(second ? 14.0F : 12.0F), false);
               }

               if (!second) {
                  Vec3 out = DragonFx.away(head, p.position());
                  DragonFx.launch(p, new Vec3(out.x * 0.6, 0.25, out.z * 0.6));
               }
            }
         }

         Vec3 snap = second ? this.at(4.9, 2.2) : this.at(4.5, 3.7);
         Vfx.slashArc(level, snap, DragonFx.fxYaw(f), 25.0F, 90.0F, 3.5, V, 120.0F);
         Vfx.sparks(level, snap.add(f.scale(1.5)), 16777215, 10, 0.35, 0.2);
      }

      if (t >= 32) {
         this.finishPerch();
      }
   }

   private void tickFissure(ServerLevel level) {
      int t = this.stateTicks;
      if (t < 14) {
         this.faceVictim(4.0F);
      }

      if (t == 14) {
         Vec3 c0 = this.claw();
         Vec3 g = DragonFx.groundBelow(level, c0.add(0.0, 1.5, 0.0));
         this.pa = g != null ? g : c0;
         Vec3 f = this.fwd();
         this.lines.clear();

         for (ServerPlayer p : this.sense.engaged) {
            Vec3 d = DragonFx.flat(p.position().subtract(this.pa));
            if (this.lines.size() < 3 && d.lengthSqr() > 1.0 && DragonFx.angle(f, d) < 90.0) {
               this.lines.add(d.normalize());
            }
         }

         if (this.lines.isEmpty()) {
            Vec3 base = this.victim != null ? DragonFx.flat(this.victim.position().subtract(this.pa)) : f;
            base = base.lengthSqr() > 1.0E-4 ? base.normalize() : f;

            for (float off : new float[]{-25.0F, 0.0F, 25.0F}) {
               this.lines.add(base.yRot(off * (float) (Math.PI / 180.0)));
            }
         }

         for (Vec3 d : this.lines) {
            Vfx.telegraphLine(level, this.pa, d, 22.0, 2.5, 14, V);
         }

         Vfx.groundCrack(level, this.pa, 3.0, V);
         Vfx.claw(level, this.pa.add(0.0, 0.6, 0.0), DragonFx.fxYaw(f), 60.0F, 2.5, V);

         for (ServerPlayer p : DragonFx.players(level, this.pa, 3.0)) {
            DragonFx.hurt(level, this.dragon, p, this.dmg(10.0F), false);
         }
      }

      if (t == 28) {
         for (Vec3 d : this.lines) {
            for (int i = 2; i <= 22; i += 2) {
               Vec3 q = this.pa.add(d.scale(i));
               Vec3 g = DragonFx.groundBelow(level, q.add(0.0, 3.0, 0.0));
               if (g != null) {
                  Vfx.pillar(level, g, V, 5.0, 1.2);
               }
            }

            if (this.fightPhase >= 2) {
               Vec3 end = DragonFx.groundBelow(level, this.pa.add(d.scale(22.0)).add(0.0, 3.0, 0.0));
               if (end != null) {
                  DragonFx.pool(level, this.dragon, end, 2.2F, 120);
               }
            }
         }

         for (ServerPlayer p : DragonFx.players(level, this.pa, 26.0)) {
            for (Vec3 d : this.lines) {
               if (Math.abs(p.getY() - this.pa.y) <= 4.0 && DragonFx.toSegmentFlat(p.position(), this.pa, this.pa.add(d.scale(22.0))) <= 1.6) {
                  DragonFx.hurt(level, this.dragon, p, this.dmg(11.0F), false);
                  DragonFx.launch(p, new Vec3(0.0, 0.9, 0.0));
                  break;
               }
            }
         }
      }

      if (t >= 40) {
         this.lines.clear();
         this.finishPerch();
      }
   }

   private void tickWingStorm(ServerLevel level) {
      int t = this.stateTicks;
      Vec3 f = this.fwd();
      Vec3 chest = this.chest();
      Vec3 g = DragonFx.groundBelow(level, chest);
      Vec3 cg = g != null ? g : this.dragon.position();
      if (t == 0) {
         Vfx.telegraphCone(level, cg, DragonFx.fxYaw(f), 60.0F, 18.0, 18, V);
      }

      if (this.hit(t)) {
         for (ServerPlayer p : DragonFx.players(level, cg, 20.0)) {
            if (!(Math.abs(p.getY() - cg.y) > 6.0) && DragonFx.inCone(cg, f, 60.0, 18.0, p.position())) {
               DragonFx.hurt(level, this.dragon, p, this.dmg(3.0F), false);
               Vec3 out = DragonFx.away(cg, p.position());
               DragonFx.launch(p, new Vec3(out.x * 1.4, 0.35, out.z * 1.4));
            }
         }

         AABB box = new AABB(cg.x - 18.0, cg.y - 4.0, cg.z - 18.0, cg.x + 18.0, cg.y + 16.0, cg.z + 18.0);

         for (Projectile pr : level.getEntitiesOfClass(Projectile.class, box, e -> e.getOwner() instanceof Player)) {
            if (DragonFx.inCone(cg, f, 60.0, 18.0, pr.position()) && pr.getDeltaMovement().lengthSqr() > 0.01) {
               pr.setDeltaMovement(pr.getDeltaMovement().scale(-1.0));
               pr.setOwner(this.dragon);
               pr.syncVelocity = true;
            }
         }

         Vfx.streaks(level, chest, f.scale(12.0), V, 12);
         Vfx.smoke(level, cg.add(f.scale(4.0)), V.smoke(), 8, 3.0, 1.6);
      }

      if (t >= 52) {
         this.finishPerch();
      }
   }

   private void tickPounce(ServerLevel level) {
      int t = this.stateTicks;
      ServerPlayer tg = this.victim;
      if (t < 14) {
         this.faceVictim(4.0F);
      }

      if (t == 0) {
         this.pa = this.dragon.position();
      }

      if (t == 6) {
         Vec3 at = tg != null && tg.isAlive() ? predict(tg, 16.0) : this.dragon.position().add(this.fwd().scale(12.0));
         Vec3 g = DragonFx.groundBelow(level, at.add(0.0, 2.0, 0.0));
         this.pa = g != null ? g : this.dragon.position();
         Vfx.telegraphCircle(level, this.pa, 5.0, 16, V);
      }

      if (t == 14) {
         this.pc = this.dragon.position();
      }

      if (t > 14 && t <= 22) {
         float k = (t - 14) / 8.0F;
         Vec3 p = this.pc.lerp(this.pa, k).add(0.0, 20.0F * k * (1.0F - k), 0.0);
         this.dragon.setPos(p.x, p.y, p.z);
         this.turnToward(this.pa.subtract(this.pc), 4.0F);
      }

      if (t == 22) {
         for (ServerPlayer p : DragonFx.players(level, this.pa, 5.0)) {
            DragonFx.hurt(level, this.dragon, p, this.dmg(16.0F), false);
            Vec3 out = DragonFx.away(this.pa, p.position());
            DragonFx.launch(p, new Vec3(out.x * 1.3, 0.5, out.z * 1.3));
         }

         Vfx.heavyImpact(level, this.pa, V, 1.2);
         Vfx.groundCrack(level, this.pa, 5.0, V);
      }

      if (t >= 44) {
         float r = this.dragon.getRandom().nextFloat();
         DragonMove next = r < 0.4F ? DragonMove.RENDING_BITE : (r < 0.7F ? DragonMove.TAIL_SWEEP : null);
         if (next != null && next.available(this.fightPhase)) {
            this.setCooldown(next);
            this.begin(level, next);
         } else {
            this.finishPerch();
         }
      }
   }

   private void trackVanillaPhase(ServerLevel level) {
      EnderDragonPhase<?> p = this.vanillaPhase();
      if (p != this.lastPhase) {
         EnderDragonPhase<?> old = this.lastPhase;
         this.lastPhase = p;
         this.vanillaTicks = 0;
         this.onVanillaPhase(level, old, p);
      } else if (this.state() == 0) {
         this.vanillaTicks++;
      }

      if (this.perched()) {
         this.perchTicks++;
      }
   }

   private void onVanillaPhase(ServerLevel level, @Nullable EnderDragonPhase<?> old, EnderDragonPhase<?> now) {
      ServerPlayer t = this.sense.target();
      if (now == EnderDragonPhase.STRAFE_PLAYER && t != null) {
         ((DragonStrafePlayerPhase)this.dragon.getPhaseManager().getPhase(EnderDragonPhase.STRAFE_PLAYER)).setTarget(t);
      } else if (now == EnderDragonPhase.CHARGING_PLAYER && t != null) {
         ((DragonChargePlayerPhase)this.dragon.getPhaseManager().getPhase(EnderDragonPhase.CHARGING_PLAYER)).setTarget(t.position());
      } else if (now == EnderDragonPhase.SITTING_ATTACKING && this.state() == 0) {
         DragonFx.sound(level, this.dragon.position(), "perch_roar.start", 6.0F, 1.0F);
      } else if (now == EnderDragonPhase.SITTING_FLAMING && this.state() == 0) {
         Vfx.telegraphCone(level, this.headGround(level), DragonFx.fxYaw(this.fwd()), 25.0F, 10.0, 10, V);
         DragonFx.sound(level, this.mouth(), "breath.start", 5.0F, 1.0F);
      }

      if (isSitting(now) && (old == null || !isSitting(old))) {
         this.perchTicks = 0;
         this.perchDamage = 0.0F;
      }

      if (now == EnderDragonPhase.TAKEOFF) {
         this.resetLanding();
      }
   }

   private void vanillaPerchMoves(ServerLevel level, EnderDragonPhase<?> vp) {
      if (vp == EnderDragonPhase.SITTING_ATTACKING && this.vanillaTicks == 15) {
         this.dominionRoar(level);
      } else if (vp == EnderDragonPhase.SITTING_FLAMING) {
         if (this.vanillaTicks == 10) {
            DragonFx.sound(level, this.mouth(), "breath.hit", 5.0F, 1.0F);
         }

         if (this.vanillaTicks >= 10 && this.vanillaTicks < 200 && this.vanillaTicks % 4 == 0) {
            Vec3 mouth = this.mouth();
            Vec3 g = this.dropped(level, this.dragon.head.position().add(this.fwd().scale(2.5)));
            Vfx.beam(level, mouth, g, V, 0.6);
            Vfx.smoke(level, g.add(0.0, 0.3, 0.0), 14211288, 4, 1.2, 1.4);
         }
      }
   }

   private void dominionRoar(ServerLevel level) {
      Vec3 pos = this.dragon.position();
      Vfx.shockwave(level, pos, V, 10.0);
      DragonFx.sound(level, pos, "perch_roar.hit", 6.0F, 1.0F);

      for (ServerPlayer p : DragonFx.players(level, pos, 24.0)) {
         if (DragonFx.flatDist(p.position(), pos) <= 10.0) {
            Vec3 out = DragonFx.away(pos, p.position());
            DragonFx.launch(p, new Vec3(out.x * 1.2, 0.35, out.z * 1.2));
            if (this.fightPhase >= 2) {
               DragonFx.hurt(level, this.dragon, p, this.dmg(6.0F), false);
               p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1), this.dragon);
            }
         }

         p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, false), this.dragon);
      }
   }

   private boolean grounded() {
      int s = this.state();
      return s == 24 || s == 25 || s == 7 || s == 26;
   }

   private boolean blocked() {
      int s = this.state();
      return s == 1 || this.pendingPhase >= 0 || s == 27 || s == 28 || s == 29;
   }

   private boolean canBreak() {
      if (!this.blocked() && !this.grounded() && !this.staggered) {
         int s = this.state();
         DragonMove m = this.move;
         return m == null || !m.superArmor() || s != 16 && s != 18 && s != 4 && s != 17 && s != 8 && s != 5;
      } else {
         return false;
      }
   }

   private void losePoise(float amount) {
      if (this.poiseImmune <= 0 && !this.staggered && !this.blocked()) {
         this.poiseIdle = 0;
         this.poise = Math.max(0.0F, this.poise - amount);
         if (this.poise <= 0.0F) {
            this.breakPending = true;
         }
      }
   }

   private void breakPoise(ServerLevel level) {
      this.breakPending = false;
      this.staggered = true;
      Vec3 core = this.core();
      Vfx.shards(level, core, 16777215, 24, 0.7);
      Vfx.sparks(level, core, V.glow(), 24, 0.6, 0.24);
      Vfx.flare(level, core, 16777215, 6.0, 6);
      Vfx.ring(level, this.dragon.position(), V.glow(), 10.0, 14);
      Vfx.shake(level, core, 32.0, 0.6, 10);
      boolean recallBroken = this.state() == 3;
      boolean wasPerched = this.perched() && !recallBroken;
      this.interrupt(level);
      this.groundedFor = recallBroken ? 120 : (this.fightPhase >= 3 ? 80 : 100);
      if (wasPerched) {
         this.dragon.getPhaseManager().setPhase(EnderDragonPhase.SITTING_SCANNING);
         this.setState(25);
         DragonFx.sound(level, core, "stagger", 8.0F, 1.0F);
      } else {
         this.startCrash(level);
      }
   }

   private void startCrash(ServerLevel level) {
      Vec3 pos = this.dragon.position();
      Vec3 land = null;
      Vec3 below = DragonFx.ground(level, pos.x, pos.z);
      if (below != null && below.y < pos.y && DragonFx.flatDist(below, this.portal) <= 48.0) {
         land = below;
      }

      if (land == null) {
         Vec3 d = DragonFx.flat(this.portal.subtract(pos));
         double max = Math.min(d.length(), 33.599999999999994);
         Vec3 dir = d.lengthSqr() > 1.0E-4 ? d.normalize() : Vec3.ZERO;

         for (double s = 1.0; s <= max; s++) {
            Vec3 q = DragonFx.ground(level, pos.x + dir.x * s, pos.z + dir.z * s);
            if (q != null && q.y < pos.y && DragonFx.flatDist(q, this.portal) <= 48.0) {
               land = q;
               break;
            }
         }
      }

      if (land == null) {
         land = this.portal;
      }

      this.pa = land;
      this.pc = pos;
      this.dragon.getPhaseManager().setPhase(EnderDragonPhase.SITTING_SCANNING);
      this.setState(24);
      DragonFx.sound(level, pos, "crash.start", 8.0F, 1.0F);
   }

   private void tickCrash(ServerLevel level) {
      int t = this.stateTicks;
      this.hover();
      if (t <= 24) {
         float k = t / 24.0F;
         this.dragon
            .setPos(Mth.lerp(k, this.pc.x, this.pa.x), Mth.lerp(k * k, this.pc.y, this.pa.y), Mth.lerp(k, this.pc.z, this.pa.z));
         if (t % 2 == 0) {
            Vfx.smoke(level, this.core(), V.smoke(), 4, 1.5, 1.6);
         }
      }

      if (t == 24) {
         Vfx.heavyImpact(level, this.pa, V, 1.5);
         Vfx.groundCrack(level, this.pa, 6.0, V);
         DragonFx.sound(level, this.pa, "crash.hit", 8.0F, 0.9F);

         for (ServerPlayer p : DragonFx.players(level, this.pa, 5.0)) {
            DragonFx.hurt(level, this.dragon, p, this.dmg(8.0F), false);
            Vec3 out = DragonFx.away(this.pa, p.position());
            DragonFx.launch(p, new Vec3(out.x, 0.4, out.z));
         }
      }

      if (t >= 32) {
         this.setState(7);
      }
   }

   private void tickStunned(ServerLevel level) {
      int t = this.stateTicks;
      this.hover();
      Vec3 head = this.at(4.4, 1.2).add(this.right().scale(0.55));
      if (t % 5 == 0) {
         Vfx.sparks(level, head.add(0.0, 1.0, 0.0), V.glow(), 6, 0.2, 0.2);
      }

      if (t % 20 == 0) {
         Vfx.glint(level, head.add(0.0, 1.6, 0.0), 16777215, 1.2);
      }

      if (t >= this.groundedFor) {
         this.setState(26);
         DragonFx.sound(level, this.dragon.position(), "recover.start", 6.0F, 1.0F);
      }
   }

   private void tickRecover(ServerLevel level) {
      this.hover();
      if (this.stateTicks >= 36) {
         this.staggered = false;
         this.poise = 160.0F;
         this.poiseImmune = 400;
         this.move = null;
         if (this.pendingPhase >= 0) {
            this.flyToPhaseHover();
         } else {
            this.setState(0);
            this.dragon.getPhaseManager().setPhase(EnderDragonPhase.TAKEOFF);
            this.rest = this.restFor(false);
         }
      }
   }

   public boolean hurt(ServerLevel level, EnderDragonPart part, DamageSource source, float amount) {
      DragonPhaseInstance phase = this.dragon.getPhaseManager().getCurrentPhase();
      if (phase.getPhase() != EnderDragonPhase.DYING && !this.dragon.isDeadOrDying()) {
         Entity attacker = source.getEntity();
         boolean admin = source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
            || attacker == null && source.getDirectEntity() == null && !source.is(DamageTypes.BAD_RESPAWN_POINT);
         if (admin) {
            if (!source.is(DamageTypeTags.ALWAYS_HURTS_ENDER_DRAGONS) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
               return false;
            }

            ((DragonAccess)this.dragon).mysticalterrain$reallyHurt(level, source, amount);
            this.afterHurt(phase);
            return true;
         } else {
            if (this.blocked()) {
               return false;
            }

            boolean grounded = this.grounded();
            float f = grounded ? amount : phase.onHurt(source, amount);
            if (f < 0.01F) {
               return false;
            }

            if (!(attacker instanceof Player) && !source.is(DamageTypeTags.ALWAYS_HURTS_ENDER_DRAGONS)) {
               return true;
            }

            if (source == this.lastSource && this.dragon.tickCount == this.lastSourceTick) {
               return false;
            }

            this.lastSource = source;
            this.lastSourceTick = this.dragon.tickCount;
            if (this.reflectTicks > 0 && source.getDirectEntity() instanceof Projectile shot && !(shot instanceof DragonFireball)) {
               this.reflected.add(new DragonRework.Reflect(shot, shot.getOwner(), shot.getDeltaMovement().length()));
               Vfx.glint(level, shot.position(), 16777215, 1.4);
               Vfx.sparks(level, shot.position(), V.glow(), 8, 0.3, 0.2);
               return false;
            }

            boolean projectile = source.is(DamageTypeTags.IS_PROJECTILE) || attacker != null && source.getDirectEntity() != attacker;
            boolean head = part == this.dragon.head;
            float mult;
            if (grounded) {
               mult = head ? 1.5F : 1.0F;
            } else if (head) {
               mult = 1.0F;
            } else {
               mult = switch (part.name) {
                  case "neck" -> 0.75F;
                  case "body" -> 0.6F;
                  default -> 0.4F;
               };
            }

            boolean bed = source.is(DamageTypes.BAD_RESPAWN_POINT);
            if (bed) {
               mult *= 0.25F;
            }

            if (this.overextended > 0) {
               mult *= 1.2F;
            }

            float raw = f;
            float dmg = f * mult * 0.5F * this.multiplayer();
            if (attacker instanceof ServerPlayer sp) {
               this.sense.threat.add(sp, raw * (projectile ? 1.5F : 1.0F));
               if (sp == this.carried) {
                  this.carriedOwn += raw;
               } else if (this.carried != null) {
                  this.carriedOthers += raw;
               }
            }

            this.sense.recordDamage(raw, projectile);
            if (this.state() == 3) {
               this.channelDamage += raw;
            }

            if (!bed && !grounded) {
               this.losePoise(raw * this.poiseMult(source, head, projectile));
            }

            this.aegisIdle = 0;
            float soaked = 0.0F;
            if (this.obsidian > 0.0F && dmg > 0.0F) {
               float w = Math.min(this.obsidian, dmg);
               this.obsidian -= w;
               dmg -= w;
               soaked += w;
            }

            if (this.aegis > 0.0F && dmg > 0.0F) {
               float w = Math.min(this.aegis, dmg);
               this.aegis -= w;
               dmg -= w;
               soaked += w;
            }

            if (soaked > 0.0F) {
               Vec3 at = part.position().add(0.0, part.getBbHeight() * 0.5, 0.0);
               Vfx.glint(level, at, 16777215, 1.2);
               Vfx.shards(level, at, 14211288, 6, 0.3);
               if (this.aegis <= 0.0F && this.obsidian <= 0.0F) {
                  Vfx.shards(level, this.core(), 16777215, 20, 0.6);
                  Vfx.flare(level, this.core(), 16777215, 4.0, 6);
               }
            }

            float health = this.dragon.getHealth();
            float floor = this.floor() * this.dragon.getMaxHealth();
            if (dmg > 0.0F && health - dmg < floor) {
               dmg = Math.max(0.0F, health - floor);
            }

            if (dmg <= 0.001F) {
               if (soaked <= 0.0F
                  && this.fightPhase == 0
                  && !this.hazards.crystals().isEmpty()
                  && attacker instanceof ServerPlayer sp
                  && this.aegisMessage <= 0) {
                  this.aegisMessage = 40;
                  sp.sendOverlayMessage(Component.translatableWithFallback("message.mysticalterrain.dragon.aegis", "The crystals shield the Sovereign"));
                  Vfx.glint(level, part.position(), 16777215, 1.0);
               }

               return soaked > 0.0F;
            } else {
               ((DragonAccess)this.dragon).mysticalterrain$reallyHurt(level, source, dmg);
               if (this.perched()) {
                  this.perchDamage = this.perchDamage + (health - this.dragon.getHealth());
               }

               this.afterHurt(phase);
               return true;
            }
         }
      } else {
         return false;
      }
   }

   private void afterHurt(DragonPhaseInstance phase) {
      if (this.dragon.isDeadOrDying() && !phase.isSitting()) {
         this.dragon.setHealth(1.0F);
         this.dragon.getPhaseManager().setPhase(EnderDragonPhase.DYING);
      }
   }

   private float floor() {
      if (this.fightPhase == 0) {
         return this.hazards.crystals().isEmpty() ? THRESHOLDS[1] : THRESHOLDS[0];
      } else if (this.fightPhase == 1) {
         return THRESHOLDS[1];
      } else {
         return this.fightPhase == 2 ? THRESHOLDS[2] : 0.0F;
      }
   }

   private float multiplayer() {
      int n = Math.max(1, this.sense.engaged.size());
      return 1.0F / (1.0F + 0.5F * (n - 1));
   }

   private float poiseMult(DamageSource source, boolean head, boolean projectile) {
      float f = head ? 1.5F : 1.0F;
      ItemStack weapon = source.getWeaponItem();
      if (weapon == null || !weapon.is(ItemTags.AXES) && !weapon.is(Items.MACE)) {
         if (source.getEntity() instanceof Player p
            && source.getDirectEntity() == p
            && p.fallDistance > 0.0
            && !p.onGround()
            && !p.onClimbable()
            && !p.isInWater()
            && !p.isPassenger()
            && !p.isSprinting()) {
            f *= 1.5F;
         }
      } else {
         f *= 1.5F;
      }

      if (projectile) {
         f *= 0.5F;
      }

      DragonMove m = this.move;
      if (m != null && m.superArmor()) {
         f *= 0.2F;
      }

      return f;
   }

   private void tickReflections() {
      for (DragonRework.Reflect r : this.reflected) {
         Projectile p = r.projectile();
         if (!p.isRemoved() && r.owner() != null && r.owner().isAlive()) {
            Vec3 to = r.owner().getEyePosition().subtract(p.position());
            if (!(to.lengthSqr() < 1.0E-4)) {
               p.setOwner(this.dragon);
               p.setDeltaMovement(to.normalize().scale(Math.max(1.0, r.speed()) * 1.2));
               p.syncVelocity = true;
            }
         }
      }

      this.reflected.clear();
   }

   public void onCrystalDestroyed(ServerLevel level, EndCrystal crystal, DamageSource source) {
      this.hazards.refreshCrystals(level);
      if (this.fightPhase == 0) {
         this.aegisCap = Math.max(0.0F, this.aegisCap - 12.0F);
      }

      if (this.aegis > 0.0F) {
         this.aegis = Math.max(0.0F, this.aegis - 12.0F);
         Vec3 core = this.core();
         Vfx.shards(level, core, 16777215, 18, 0.6);
         Vfx.flare(level, core, 16777215, 4.0, 5);
         Vfx.ring(level, this.dragon.position(), V.glow(), 6.0, 12);
      }

      if (source.getEntity() instanceof ServerPlayer p) {
         this.sense.threat.add(p, 20.0F);
      }
   }

   public void beginSummon(Vec3 hoverAt) {
      this.initialized = true;
      this.dragon.getAttribute(Attributes.MAX_HEALTH).setBaseValue(800.0);
      this.summonStartHealth = 1.0F;
      this.dragon.setHealth(1.0F);
      this.dragon.getPhaseManager().setPhase(EnderDragonPhase.HOVERING);
      this.fightPhase = 0;
      this.pendingPhase = -1;
      this.lastLight = false;
      this.combatTicks = 0;
      this.setState(1);
      this.hoverAt = hoverAt;
   }

   private void tickSummon(ServerLevel level) {
      float f = Math.min(1.0F, this.stateTicks / 160.0F);
      this.dragon.setHealth(Mth.lerp(f, this.summonStartHealth, 800.0F));
      if (this.hoverAt == null) {
         this.hoverAt = this.portal.add(0.0, 28.0, 0.0);
      }

      double e = 1.0 - Math.pow(1.0F - f, 3.0);
      Vec3 from = new Vec3(this.hoverAt.x, this.hoverAt.y - 75.0, this.hoverAt.z);
      Vec3 pos = from.lerp(this.hoverAt, e);
      this.dragon.setPos(pos.x, pos.y, pos.z);
      this.dragon.setDeltaMovement(Vec3.ZERO);
      Vec3 mid = this.dragon.position().add(0.0, 2.0, 0.0);
      if (this.stateTicks % 3 == 0) {
         Vfx.gather(level, mid, V, 9.0, 18);
         Vfx.trail(level, mid, V, 3.5);
      }

      if (this.stateTicks % 20 == 0) {
         Vfx.ring(level, this.dragon.position().add(0.0, -2.0, 0.0), V.glow(), 14.0, 22);
         Vfx.shake(level, this.dragon.position(), 72.0, 0.25 + 0.5 * f, 20);
      }

      if (this.stateTicks % 4 == 0) {
         level.sendParticles(ParticleTypes.END_ROD, mid.x, mid.y, mid.z, 24, 4.0, 3.0, 4.0, 0.12);
         Vfx.smoke(level, this.dragon.position(), V.smoke(), 8, 3.0, 2.0);
      }

      if (this.stateTicks >= 160) {
         Vfx.explosion(level, mid, V, 7.0);
         Vfx.screen(level, this.dragon.position(), 96.0, 1.6, 30, V.glow(), 0.5F);
         this.dragon.setHealth(800.0F);
         this.hazards.refreshCrystals(level);
         this.fightPhase = this.hazards.crystals().isEmpty() ? 1 : 0;
         this.resetAegis();
         this.combatTicks = 0;
         this.poise = 160.0F;
         this.resetLanding();
         this.dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
         this.begin(level, DragonMove.ROAR);
      }
   }

   public void customDeath(ServerLevel level) {
      this.dragon.dragonDeathTime++;
      int t = this.dragon.dragonDeathTime;
      if (t == 1) {
         this.interrupt(level);
         this.forced.clear();
         this.hazards.clear();
         this.thrown = null;
      }

      EnderDragonFight deathFight = this.dragon.getDragonFight();
      if (deathFight != null) {
         deathFight.updateDragon(this.dragon);
      }

      this.dragon.setDeltaMovement(Vec3.ZERO);
      if (t < 150) {
         int ground = level.getHeight(Types.MOTION_BLOCKING, this.dragon.getBlockX(), this.dragon.getBlockZ());
         if (this.dragon.getY() > ground + 6) {
            this.dragon.setPos(this.dragon.getX(), this.dragon.getY() - 0.06, this.dragon.getZ());
         }
      }

      Vec3 c = this.dragon.position().add(0.0, 2.0, 0.0);
      if (t == 1) {
         DragonFx.sound(level, c, "death", 10.0F, 1.0F);
      }

      if (t == 20 || t == 55 || t == 90 || t == 128 || t == 152) {
         DragonFx.sound(level, c, "death_crack", 8.0F, 0.9F + t / 400.0F);
      }

      if (t == 110) {
         DragonFx.sound(level, c, "death_swell", 8.0F, 1.0F);
      }

      if (t == 178) {
         DragonFx.sound(level, c, "death_burst", 12.0F, 1.0F);
      }

      this.deathEffects(level);
      this.syncBar(level);
      if (t >= 200 && !this.dragon.isRemoved()) {
         EnderDragonFight fight = this.dragon.getDragonFight();
         int xp = fight != null && !fight.hasPreviouslyKilledDragon() ? 12000 : 500;
         ExperienceOrb.award(level, this.dragon.position(), xp);
         if (fight != null) {
            fight.setDragonKilled(this.dragon);
         }

         this.dragon.remove(RemovalReason.KILLED);
         this.dragon.gameEvent(GameEvent.ENTITY_DIE);
      }
   }

   private void deathEffects(ServerLevel level) {
      int t = this.dragon.dragonDeathTime;
      Vec3 c = this.dragon.position().add(0.0, 2.0, 0.0);
      if (t % 6 == 0 && t < 178) {
         Vec3 crack = c.add(
            (this.dragon.getRandom().nextDouble() - 0.5) * 8.0,
            (this.dragon.getRandom().nextDouble() - 0.3) * 4.0,
            (this.dragon.getRandom().nextDouble() - 0.5) * 8.0
         );
         Vfx.flare(level, crack, V.core(), 2.0 + t / 60.0, 8);
         Vfx.sparks(level, crack, V.glow(), 8, 0.35, 0.25);
      }

      if (t > 70 && t < 178 && t % 4 == 0) {
         Vfx.gather(level, c, V, 7.0, 12);
         Vfx.flare(level, c, V.glow(), 2.0 + (t - 70) / 25.0, 6);
      }

      if (t == 90 || t == 110 || t == 128 || t == 142 || t == 152) {
         Vfx.shake(level, c, 96.0, 0.5, 10);
      }

      if (t == 178) {
         Vfx.explosion(level, c, V, 10.0);
         Vfx.pillar(level, this.dragon.position(), V, 60.0, 5.0);
         Vfx.screen(level, c, 160.0, 2.0, 40, 16777215, 0.8F);
      }
   }

   private @Nullable ServerBossEvent bar() {
      EnderDragonFight fight = this.dragon.getDragonFight();
      return fight == null ? null : ((EnderDragonFightAccessor)fight).mysticalterrain$dragonEvent();
   }

   private void syncBar(ServerLevel level) {
      ServerBossEvent bar = this.bar();
      if (bar != null) {
         String cast = "";
         int castTick = 0;
         int castLength = 0;
         boolean telegraph = false;
         int s = this.state();
         boolean dead = this.dragon.isDeadOrDying();
         if (!dead) {
            switch (s) {
               case 0:
                  EnderDragonPhase<?> vp = this.vanillaPhase();
                  if (vp == EnderDragonPhase.SITTING_ATTACKING) {
                     cast = DragonMove.PERCH_ROAR.anim;
                     castTick = Math.min(this.vanillaTicks, 40);
                     castLength = 40;
                     telegraph = this.vanillaTicks < 15;
                  } else if (vp == EnderDragonPhase.SITTING_FLAMING) {
                     cast = DragonMove.BREATH.anim;
                     castTick = Math.min(this.vanillaTicks, 200);
                     castLength = 200;
                     telegraph = this.vanillaTicks < 10;
                  }
               case 1:
               case 7:
               case 24:
               case 25:
               case 26:
               case 27:
               case 28:
               case 29:
               case 30:
               case 31:
                  break;
               case 2:
               case 5:
               case 6:
               case 9:
               case 10:
               case 11:
               case 12:
               case 13:
               case 14:
               case 15:
               case 16:
               case 18:
               case 19:
               case 20:
               case 21:
               case 22:
               case 23:
               default:
                  DragonMove m = this.move;
                  if (m != null && m.state == s) {
                     cast = m.anim;
                     castTick = Math.min(this.stateTicks, m.length);
                     castLength = m.length;
                     telegraph = m.telegraphing(this.stateTicks);
                  }
                  break;
               case 3:
                  cast = DragonMove.CRYSTAL_RECALL.anim;
                  castTick = Math.min(this.stateTicks, this.channelLength);
                  castLength = this.channelLength;
                  telegraph = true;
                  break;
               case 4:
               case 8:
               case 17:
                  cast = DragonMove.SHATTERDIVE.anim;
                  castTick = this.dragon.tickCount - this.shatterStart;
                  castLength = Math.max(castTick + 1, this.shatterLength);
                  telegraph = s != 4;
            }
         }

         int flags = 0;
         if (this.enraged()) {
            flags |= 1;
         }

         if (dead || this.invulnerableNow()) {
            flags |= 2;
         }

         if (this.obsidianTicks > 0 && this.obsidian > 0.0F) {
            flags |= 4;
         }

         if (telegraph) {
            flags |= 16;
         }

         float shield = this.aegis + this.obsidian;
         float shieldMax = Math.max(this.aegisCap, this.aegis) + (this.obsidianTicks > 0 ? 48 : 0);
         float poiseFrac = this.staggered ? 0.0F : Mth.clamp(this.poise / 160.0F, 0.0F, 1.0F);
         BossStatePayload payload = new BossStatePayload(
            bar.getId(),
            "ender_dragon",
            80,
            2.0F,
            Math.max(0.0F, this.dragon.getHealth()),
            this.dragon.getMaxHealth(),
            this.fightPhase,
            BAR_THRESHOLDS,
            shield,
            shieldMax,
            poiseFrac,
            this.staggered && !dead,
            cast,
            castTick,
            castLength,
            flags
         );
         UUID marked = null;
         if (this.sense.markTicks > 0 && this.sense.marked != null) {
            marked = this.sense.marked.getUUID();
         } else if (this.sense.engaged.size() >= 2 && this.sense.target != null) {
            marked = this.sense.target.getUUID();
         }

         this.sync.tick(this.dragon.tickCount, bar, payload, marked);
      }
   }

   private boolean invulnerableNow() {
      return this.blocked()
         ? true
         : this.fightPhase == 0
            && !this.hazards.crystals().isEmpty()
            && this.aegis <= 0.0F
            && this.dragon.getHealth() <= THRESHOLDS[0] * this.dragon.getMaxHealth() + 0.5F;
   }

   private EnderDragonPhase<?> vanillaPhase() {
      return this.dragon.getPhaseManager().getCurrentPhase().getPhase();
   }

   private static boolean isSitting(EnderDragonPhase<?> p) {
      return p == EnderDragonPhase.SITTING_SCANNING || p == EnderDragonPhase.SITTING_ATTACKING || p == EnderDragonPhase.SITTING_FLAMING;
   }

   private boolean perched() {
      return isSitting(this.vanillaPhase());
   }

   private void capTurn() {
      if (!Float.isNaN(this.lastYaw) && (this.perched() || this.grounded())) {
         float yaw = Mth.approachDegrees(this.lastYaw, this.dragon.getYRot(), 4.0F);
         if (yaw != this.dragon.getYRot()) {
            this.dragon.setYRot(yaw);
            this.dragon.yBodyRot = yaw;
         }
      }

      this.lastYaw = this.dragon.getYRot();
   }

   private Vec3 fwd() {
      return DragonFx.forward(this.dragon.getYRot());
   }

   private Vec3 right() {
      return DragonFx.right(this.dragon.getYRot());
   }

   private Vec3 at(double fwd, double up) {
      return this.dragon.position().add(this.fwd().scale(fwd)).add(0.0, up, 0.0);
   }

   private Vec3 mouth() {
      return DragonPose.mouth(this.dragon, 1.0F);
   }

   private Vec3 jaw(double[][] track, int t) {
      return this.jawAt(track, (t - 2) / 20.0);
   }

   private Vec3 jawAt(double[][] track, double s) {
      int i = 1;

      while (i < track.length - 1 && track[i][0] < s) {
         i++;
      }

      double[] a = track[i - 1];
      double[] b = track[i];
      double u = Mth.clamp((s - a[0]) / (b[0] - a[0]), 0.0, 1.0);
      return this.at(Mth.lerp(u, a[1], b[1]), Mth.lerp(u, a[2], b[2])).add(this.right().scale(Mth.lerp(u, a[3], b[3])));
   }

   private Vec3 core() {
      return this.at(1.1, 2.0);
   }

   private Vec3 grabPoint() {
      return this.at(-1.0, 1.0);
   }

   private Vec3 talonPoint() {
      return this.at(TALON_STRIKE[0], TALON_STRIKE[1]);
   }

   private Vec3 carryPoint() {
      return this.at(-3.2, 0.2);
   }

   private Vec3 hips() {
      return this.dragon.position().add(this.fwd().scale(-2.0));
   }

   private Vec3 chest() {
      return this.dragon.position().add(this.fwd().scale(2.0)).add(0.0, 2.0, 0.0);
   }

   private Vec3 claw() {
      return this.at(1.5, 0.0).add(this.right().scale(0.85));
   }

   private Vec3 headGround(ServerLevel level) {
      return this.dropped(level, this.mouth());
   }

   private Vec3 dropped(ServerLevel level, Vec3 p) {
      Vec3 g = DragonFx.groundBelow(level, p);
      return g != null ? g : new Vec3(p.x, this.dragon.getY(), p.z);
   }

   static Vec3 predict(ServerPlayer p, double ticks) {
      Vec3 v = p.getKnownMovement();
      Vec3 lead = new Vec3(v.x, 0.0, v.z).scale(ticks);
      if (lead.length() > 10.0) {
         lead = lead.normalize().scale(10.0);
      }

      return p.position().add(lead);
   }

   private Vec3 portalTop(ServerLevel level) {
      BlockPos origin = this.dragon.getFightOrigin();
      BlockPos pos = level.getHeightmapPos(Types.MOTION_BLOCKING_NO_LEAVES, origin != null ? origin : BlockPos.ZERO);
      return Vec3.atBottomCenterOf(pos);
   }

   public List<EndSpike> emptySpikes(ServerLevel level) {
      return DragonFx.emptySpikes(level);
   }

   public void forceMove(ServerLevel level, int stateId) {
      switch (stateId) {
         case 0:
            if (!this.blocked() && !this.dying()) {
               this.interrupt(level);
               this.finishAir();
            }
            break;
         case 1:
         case 6:
         case 7:
         case 9:
         case 10:
         case 11:
         case 12:
         case 13:
         case 14:
         case 15:
         case 16:
         default:
            DragonMove m = DragonMove.byState(stateId);
            if (m != null) {
               this.forceMove(level, m.anim);
            }
            break;
         case 2:
            this.forceMove(level, DragonMove.BREATH_SWEEP.anim);
            break;
         case 3:
            this.forceMove(level, DragonMove.CRYSTAL_RECALL.anim);
            break;
         case 4:
         case 5:
         case 8:
         case 17:
            this.forceMove(level, DragonMove.SHATTERDIVE.anim);
      }
   }

   public void forceMove(ServerLevel level, String anim) {
      DragonMove m = DragonMove.byAnim(anim);
      if (m != null && m.pickable() && !this.blocked() && !this.dying() && !this.grounded()) {
         this.interrupt(level);
         if (this.portal == Vec3.ZERO) {
            this.portal = this.portalTop(level);
         }

         ServerPlayer t = this.sense.target();
         if (t == null) {
            List<ServerPlayer> near = DragonFx.players(level, this.dragon.position(), 160.0);
            t = near.isEmpty() ? null : near.get(0);
         }

         if (m.where == DragonMove.Where.PERCH && !this.perched()) {
            this.dragon.getPhaseManager().setPhase(EnderDragonPhase.SITTING_SCANNING);
            this.takeoffAfter = true;
         }

         this.start(level, m, t);
      }
   }

   public void save(ValueOutput output) {
      output.putBoolean("MTInit", this.initialized);
      output.putInt("MTFightPhase", this.fightPhase);
      output.putInt("MTPendingPhase", this.pendingPhase);
      output.putFloat("MTAegis", this.aegis);
      output.putFloat("MTAegisCap", this.aegisCap);
      output.putFloat("MTPoise", this.poise);
      output.putInt("MTPoiseImmune", this.poiseImmune);
      output.putInt("MTCombat", this.combatTicks);
      output.putBoolean("MTLastLight", this.lastLight);
      output.putInt("MTLanding", this.landingTimer);
      output.putInt("MTSummon", this.state() == 1 ? Math.max(1, this.stateTicks) : 0);
      output.putFloat("MTSummonFrom", this.summonStartHealth);
   }

   public void load(ValueInput input) {
      this.initialized = input.getBooleanOr("MTInit", false);
      this.fightPhase = Mth.clamp(input.getIntOr("MTFightPhase", 0), 0, 3);
      this.pendingPhase = input.getIntOr("MTPendingPhase", -1);
      this.transitioning = false;
      this.aegis = input.getFloatOr("MTAegis", 0.0F);
      this.aegisCap = input.getFloatOr("MTAegisCap", 0.0F);
      this.poise = input.getFloatOr("MTPoise", 160.0F);
      this.poiseImmune = input.getIntOr("MTPoiseImmune", 0);
      this.combatTicks = input.getIntOr("MTCombat", 0);
      this.lastLight = input.getBooleanOr("MTLastLight", false);
      this.landingTimer = input.getIntOr("MTLanding", 900);
      this.summonStartHealth = input.getFloatOr("MTSummonFrom", 1.0F);
      if (this.fightPhase >= 3 || this.combatTicks >= 18000) {
         this.hazards.startEdge();
      }

      int summon = input.getIntOr("MTSummon", 0);
      if (summon > 0) {
         this.setState(1);
         this.stateTicks = summon;
      }
   }

   private record Reflect(Projectile projectile, @Nullable Entity owner, double speed) {
   }
}
