package com.vexira.mysticalterrain.gear.arsenal;

import com.vexira.mysticalterrain.MysticalTerrain;
import com.vexira.mysticalterrain.boss.BossSounds;
import com.vexira.mysticalterrain.boss.MysticalBoss;
import com.vexira.mysticalterrain.boss.vfx.Vfx;
import com.vexira.mysticalterrain.gear.Active;
import com.vexira.mysticalterrain.gear.ArmorKit;
import com.vexira.mysticalterrain.gear.Charge;
import com.vexira.mysticalterrain.gear.GearKit;
import com.vexira.mysticalterrain.gear.GearState;
import com.vexira.mysticalterrain.gear.GearUse;
import com.vexira.mysticalterrain.gear.MysticWeapon;
import com.vexira.mysticalterrain.gear.WeaponKit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AllowDamage;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStopped;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.ElderGuardianEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.jspecify.annotations.Nullable;

public final class LiraelArsenal extends Arsenal {
   private static final Vfx.Palette FAE = Vfx.Palette.FAE;
   private static final Vfx.Palette MOON = new Vfx.Palette(16777215, 12111103, 6320288);
   private static final Vfx.Palette THORN = new Vfx.Palette(16770284, 14166106, 2503710).withDebris(Vfx.Debris.LEAF);
   private static final Vfx.Palette ECLIPSE = new Vfx.Palette(16050431, 10116351, 1182238).withDebris(Vfx.Debris.PETAL);
   private static final Vfx.Debris PETAL = Vfx.Debris.PETAL;
   private static final Vfx.Debris LEAF = Vfx.Debris.LEAF;
   private static final Vec3d UP = new Vec3d(0.0, 1.0, 0.0);
   private static final String SCEPTER = "thornqueen_scepter";
   private static final String RAPIER = "briar_rapier";
   private static final String FAN = "petalstorm_fan";
   private static final String SAP = "lirael.sap";
   private static final String SCATTER = "lirael.scatter";
   private static final String ECHO = "lirael.echo";
   private static final String LAST_BLOOM = "lirael.last_bloom";
   private static final int MAX_THORNS = 5;
   private static final int THORN_LIFE = 120;
   private static final int BLOOM_LIFE = 100;
   private static final int MAX_DARTS = 20;
   private static final double COS_100 = Math.cos(Math.toRadians(100.0));
   private static final int MOONTHORN = 0;
   private static final int SHARD = 1;
   private static final int CRESCENT = 2;
   private static final Map<LivingEntity, LiraelArsenal.Thorns> THORNS = new WeakHashMap<>();
   private static final List<LiraelArsenal.Task> TASKS = new ArrayList<>();
   private static final List<LiraelArsenal.Task> PENDING = new ArrayList<>();
   private static final List<LiraelArsenal.Dart> DARTS = new ArrayList<>();
   private static final List<LiraelArsenal.Lunge> LUNGES = new ArrayList<>();
   private static final List<LiraelArsenal.Glide> GLIDES = new ArrayList<>();
   private static final List<LiraelArsenal.Tempest> TEMPESTS = new ArrayList<>();
   private static final List<LiraelArsenal.Bramble> BRAMBLES = new ArrayList<>();
   private static final Map<UUID, LiraelArsenal.Echo> DOUBLES = new HashMap<>();
   private static final Map<UUID, LiraelArsenal.Glamour> GLAMOUR = new HashMap<>();
   private static final Map<MobEntity, LiraelArsenal.Beguile> BEGUILED = new WeakHashMap<>();
   private static final Map<UUID, LiraelArsenal.Parry> PARRY = new HashMap<>();
   private static final Map<UUID, LiraelArsenal.Parry> FRAMES = new HashMap<>();
   private static final Map<UUID, Long> RIPOSTE_AT = new HashMap<>();
   private static final Map<UUID, Long> BUD_UNTIL = new HashMap<>();
   private static final Map<UUID, Long> BLOOM_SFX = new HashMap<>();
   private static final Map<UUID, Long> DART_SFX = new HashMap<>();
   private static final Map<UUID, Boolean> LAST_READY = new HashMap<>();
   private static final Map<Integer, Long> PRICKED = new HashMap<>();
   private static boolean hooked;
   private static long clock;

   public LiraelArsenal() {
      super("lirael", "thorn_regalia", Vfx.Palette.FAE);
      if (!hooked) {
         hooked = true;
         ServerTickEvents.END_SERVER_TICK.register(LiraelArsenal::tick);
         ServerLifecycleEvents.SERVER_STOPPED.register((ServerStopped)server -> reset());
         ServerLivingEntityEvents.ALLOW_DAMAGE.register((AllowDamage)(entity, source, amount) -> !(entity instanceof ServerPlayerEntity p && parry(p, source)));
      }
   }

   @Override
   protected void weapons() {
      this.weapon("thornqueen_scepter", 4.0F, -2.4F)
         .primary(Active.of(80, LiraelArsenal::moonlitVolley).empowered(40, 1.2F))
         .alternate(Active.of(200, LiraelArsenal::graspingBriars).empowered(50, 1.25F))
         .charge(
            Charge.of(4, 10).pose(UseAction.TRIDENT).cast(Active.of(320, LiraelArsenal::moonfall).empowered(60, 1.25F)).onLevel(LiraelArsenal::moonfallLevel)
         )
         .passive(LiraelArsenal::thornsap)
         .resonancePerHit(10.0F);
      this.weapon("briar_rapier", 7.0F, -2.0F)
         .primary(Active.of(100, LiraelArsenal::thornsongThrust).empowered(30, 1.25F))
         .alternate(Active.of(180, LiraelArsenal::petalStep).empowered(40, 1.25F))
         .charge(
            Charge.of(1, 5).pose(UseAction.BLOCK).cast(Active.of(240, LiraelArsenal::courtlyRiposte).empowered(50, 1.25F)).onLevel(LiraelArsenal::riposteLevel)
         )
         .passive(LiraelArsenal::thornbite)
         .resonancePerHit(8.0F);
      this.weapon("petalstorm_fan", 6.0F, -2.0F)
         .primary(Active.of(100, LiraelArsenal::petalGale).empowered(30, 1.25F))
         .alternate(Active.of(200, LiraelArsenal::wingbeatWaltz).empowered(40, 1.25F))
         .charge(
            Charge.of(3, 10).pose(UseAction.BOW).cast(Active.of(300, LiraelArsenal::petalTempest).empowered(60, 1.25F)).onLevel(LiraelArsenal::tempestLevel)
         )
         .passive(LiraelArsenal::scatteringPetals)
         .resonancePerHit(10.0F);
   }

   @Override
   protected void armor(ArmorKit set) {
      set.twoPiece(LiraelArsenal::moonlitGrace);
      set.fourPiece(LiraelArsenal::briarMantle);
      set.allowDamage(LiraelArsenal::graceGuard);
      set.allowDeath(LiraelArsenal::lastBloom);
      set.onHurt(LiraelArsenal::briarPrick);
      set.active(600, LiraelArsenal::thornbloom);
   }

   static boolean moonlitVolley(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      float pw = u.power();
      int pierce = u.empowered() ? 1 : 0;
      List<LivingEntity> targets = volleyTargets(level, p, 28.0, 3);
      LiraelArsenal.Volley v = new LiraelArsenal.Volley();
      volleyWave(level, p, new float[]{-30.0F, -15.0F, 0.0F, 15.0F, 30.0F}, targets, 0, pw, pierce, v);
      Vec3d tip = GearKit.tip(p);
      GearKit.muzzle(level, p, MOON, 1.0);
      Vfx.ring(level, tip, MOON.glow(), 0.9, 8);
      Vfx.glint(level, tip, MOON.core(), 1.0);
      Vfx.debris(level, tip, PETAL, FAE.glow(), 5, 0.25);
      if (u.empowered()) {
         later(level, p, 6, () -> {
            volleyWave(level, p, new float[]{-10.0F, 0.0F, 10.0F}, targets, 5, pw, pierce, v);
            GearKit.muzzle(level, p, MOON, 0.8);
            Vfx.glint(level, GearKit.tip(p), MOON.core(), 0.8);
            Vfx.debris(level, GearKit.tip(p), PETAL, FAE.glow(), 3, 0.2);
         });
      }

      return true;
   }

   private static List<LivingEntity> volleyTargets(ServerWorld level, PlayerEntity p, double range, int max) {
      List<LivingEntity> out = new ArrayList<>();
      LivingEntity first = GearKit.lookTarget(level, p, range);
      if (first != null) {
         out.add(first);
      }

      Vec3d eye = GearKit.eye(p);
      Vec3d look = p.getRotationVector();
      List<LivingEntity> rest = new ArrayList<>();

      for (LivingEntity e : GearKit.enemies(level, p, eye, range)) {
         if (e != first) {
            Vec3d to = center(e).subtract(eye);
            double d = to.lengthSquared();
            if (d > 1.0E-4 && d <= range * range && to.normalize().dotProduct(look) > 0.85 && p.canSee(e)) {
               rest.add(e);
            }
         }
      }

      rest.sort(Comparator.comparingDouble(ex -> ex.squaredDistanceTo(eye)));

      for (LivingEntity e : rest) {
         if (out.size() >= max) {
            break;
         }

         out.add(e);
      }

      return out;
   }

   private static void volleyWave(
      ServerWorld level, PlayerEntity p, float[] yaws, List<LivingEntity> targets, int first, float pw, int pierce, LiraelArsenal.Volley v
   ) {
      Vec3d look = p.getRotationVector();
      Vec3d from = GearKit.tip(p);

      for (int i = 0; i < yaws.length; i++) {
         Vec3d dir = look.rotateY(yaws[i] * (float) (Math.PI / 180.0)).add(0.0, 0.12, 0.0).normalize();
         LiraelArsenal.Dart d = dart(level, p, 0, from, dir.multiply(0.9), 40, 0.45);
         if (d == null) {
            return;
         }

         d.target = targets.isEmpty() ? null : targets.get((first + i) % targets.size());
         d.homing = 0.18;
         d.homingDelay = 4;
         d.maxSpeed = 1.3;
         d.accel = 0.05;
         d.pierce = pierce;
         d.dmg = 6.0F * pw;
         d.onHit = (dart, e, at) -> {
            hit(dart.level, dart.owner, e, dart.dmg * amp(e), false);
            int n = v.thorns.getOrDefault(e, 0);
            if (n < 2) {
               v.thorns.put(e, n + 1);
               thorn(dart.level, dart.owner, e, 1);
            }

            resonate(dart.owner, "thornqueen_scepter", 1.0F);
            Vfx.impact(dart.level, at, MOON, 0.45);
            Vfx.shards(dart.level, at, THORN.core(), 4, 0.25);
         };
      }
   }

   static boolean graspingBriars(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      Vec3d at = aimGround(level, p, 24.0);
      if (at == null) {
         return false;
      }

      float pw = u.power();
      Vec3d tip = GearKit.tip(p);
      Vfx.telegraphCircle(level, at, 3.5, 10, THORN);
      Vfx.runeCircle(level, at, 3.5, THORN, 20, 0.05F, Vfx.Rune.GLYPH);
      Vfx.beam(level, tip, at.add(0.0, 0.3, 0.0), THORN, 0.18, 8);
      Vfx.flare(level, tip, THORN.glow(), 1.0, 6);
      later(level, p, 10, () -> {
         erupt(level, p, at, 3.5, pw);
         bramble(level, p, at, 3.0);
         sfx(level, at, "weapon.thornqueen_scepter.alt", 1.1F, 0.8F);
      });
      if (u.empowered()) {
         later(
            level,
            p,
            4,
            () -> {
               for (LivingEntity e : GearKit.enemies(level, p, at, 12.0)
                  .stream()
                  .filter(ex -> flatDist(ex, at) > 3.5)
                  .sorted(Comparator.comparingDouble(ex -> ex.squaredDistanceTo(at)))
                  .limit(2L)
                  .toList()) {
                  Vec3d c = ground(level, e.getEntityPos());
                  Vfx.telegraphCircle(level, c, 2.5, 8, THORN);
                  Vfx.beam(level, at.add(0.0, 0.3, 0.0), c.add(0.0, 0.3, 0.0), THORN, 0.12, 8);
                  later(level, p, 8, () -> {
                     erupt(level, p, c, 2.5, pw);
                     bramble(level, p, c, 2.5);
                     sfx(level, c, "weapon.thornqueen_scepter.alt", 0.8F, 0.95F);
                  });
               }
            }
         );
      }

      return true;
   }

   private static void erupt(ServerWorld level, PlayerEntity p, Vec3d at, double r, float pw) {
      for (LivingEntity e : disc(level, p, at, r, 2.0, 2.0)) {
         hit(level, p, e, 14.0F * pw * amp(e), false);
         thorn(level, p, e, 2);
         if (boss(e)) {
            e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20, 0, false, false, true));
         } else if (e.isAlive()) {
            root(e, 40);
            Vec3d pull = at.subtract(e.getEntityPos());
            e.addVelocity(pull.x * 0.15, 0.1, pull.z * 0.15);
            e.knockedBack = true;
            Vfx.ring(level, e.getEntityPos(), THORN.glow(), 0.8, 40);
         }

         resonate(p, "thornqueen_scepter", 2.0F);
      }

      Vfx.groundCrack(level, at, r, THORN);
      Vfx.shockwave(level, at, THORN, r);
      int lashes = r >= 3.0 ? 8 : 6;
      double phase = level.getRandom().nextDouble() * Math.PI * 2.0;

      for (int k = 0; k < lashes; k++) {
         double a = phase + k * Math.PI * 2.0 / lashes;
         Vec3d rim = ground(level, at.add(Math.cos(a) * r, 0.0, Math.sin(a) * r));
         Vfx.claw(level, rim.add(0.0, 1.0, 0.0), yawTo(at, rim), -60.0F, 1.6, THORN);
         Vfx.shards(level, rim.add(0.0, 0.4, 0.0), THORN.core(), 3, 0.35);
      }

      Vfx.pillar(level, at, THORN, 2.4, 0.5);
      Vfx.debris(level, at.add(0.0, 0.5, 0.0), LEAF, THORN.smoke(), 16, 0.35);
   }

   static boolean moonfall(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      int lvl = Math.max(1, u.charge());
      float pw = u.power();
      float scale = 0.5F + 0.5F * pw;
      boolean emp = u.empowered();
      LiraelArsenal.Heal heal = new LiraelArsenal.Heal();
      Vec3d g = aimGround(level, p, 32.0);
      Vec3d aim = g != null ? g : GearKit.lookPoint(level, p, 32.0);
      int cols = 1 + lvl;
      List<Vec3d> spots = new ArrayList<>();
      spots.add(aim);
      List<LivingEntity> foes = new ArrayList<>(GearKit.enemies(level, p, aim, 10.0));
      foes.sort(
         Comparator.<LivingEntity>comparingInt(e -> blooming(e) ? 0 : 1).thenComparingInt(e -> -stacks(e)).thenComparingDouble(e -> e.squaredDistanceTo(aim))
      );

      for (LivingEntity e : foes) {
         if (spots.size() >= cols) {
            break;
         }

         Vec3d v = e.getVelocity();
         spots.add(ground(level, e.getEntityPos().add(v.x * 10.0, 0.0, v.z * 10.0)));
      }

      int left = cols - spots.size();
      double phase = level.getRandom().nextDouble() * Math.PI * 2.0;

      for (int j = 0; j < left; j++) {
         double a = phase + j * Math.PI * 2.0 / left;
         spots.add(ground(level, aim.add(Math.cos(a) * 3.0, 0.0, Math.sin(a) * 3.0)));
      }

      for (int k = 0; k < spots.size(); k++) {
         Vec3d c = spots.get(k);
         boolean first = k == 0;
         if (first) {
            Vfx.telegraphCircle(level, c, 2.2, 12, MOON);
         } else {
            later(level, p, 6 * k, () -> Vfx.telegraphCircle(level, c, 2.2, 12, MOON));
         }

         later(level, p, 6 * k + 12, () -> column(level, p, c, pw, scale, emp, heal, first));
      }

      if (lvl >= 4) {
         Vfx.telegraphCircle(level, aim, 5.0, 46, MOON);
         later(level, p, 46, () -> fullMoon(level, p, aim, pw, scale, heal));
      }

      Vec3d tip = GearKit.tip(p);
      GearKit.muzzle(level, p, MOON, 1.1);
      Vfx.beam(level, tip, tip.add(0.0, 5.0, 0.0), MOON, 0.15, 6);
      Vfx.glint(level, tip, MOON.core(), 1.2);
      return true;
   }

   private static void column(ServerWorld level, PlayerEntity p, Vec3d c, float pw, float scale, boolean emp, LiraelArsenal.Heal heal, boolean first) {
      Set<LivingEntity> struck = new HashSet<>();

      for (LivingEntity e : disc(level, p, c, 2.2, 1.0, 10.0)) {
         struck.add(e);
         boolean bloom = blooming(e);
         hit(level, p, e, 14.0F * pw * amp(e), false);
         if (bloom) {
            harvest(level, p, e, scale, heal);
         } else {
            thorn(level, p, e, 1);
         }

         if (!boss(e) && e.isAlive()) {
            e.addVelocity(0.0, 0.35, 0.0);
            e.knockedBack = true;
         }

         resonate(p, "thornqueen_scepter", 2.0F);
      }

      settle(level, p, heal);
      Vfx.beam(level, c.add(0.0, 14.0, 0.0), c, MOON, 0.9, 8);
      Vfx.pillar(level, c, MOON, 6.0, 1.1);
      Vfx.shockwave(level, c, MOON, 2.4);
      Vfx.glint(level, c.add(0.0, 2.0, 0.0), MOON.core(), 1.4);
      Vfx.shards(level, c.add(0.0, 0.5, 0.0), MOON.core(), 8, 0.4);
      if (first) {
         sfx(level, c, "weapon.thornqueen_scepter.column", 1.2F, 1.0F);
      }

      if (emp) {
         Vec3d from = c.add(0.0, 1.2, 0.0);
         List<LivingEntity> pool = new ArrayList<>(GearKit.enemies(level, p, from, 8.0));
         pool.sort(Comparator.<LivingEntity>comparingInt(e -> struck.contains(e) ? 1 : 0).thenComparingDouble(e -> e.squaredDistanceTo(from)));
         double a0 = level.getRandom().nextDouble() * Math.PI * 2.0;

         for (int i = 0; i < 3; i++) {
            double a = a0 + i * Math.PI * 2.0 / 3.0;
            Vec3d dir = new Vec3d(Math.cos(a), 0.4, Math.sin(a)).normalize();
            LiraelArsenal.Dart d = dart(level, p, 1, from, dir.multiply(0.8), 30, 0.35);
            if (d == null) {
               break;
            }

            d.target = pool.isEmpty() ? null : pool.get(i % Math.min(3, pool.size()));
            d.homing = 0.25;
            d.homingDelay = 3;
            d.maxSpeed = 1.2;
            d.accel = 0.05;
            d.dmg = 5.0F * pw;
            d.onHit = (dart, e, at) -> {
               hit(dart.level, dart.owner, e, dart.dmg * amp(e), false);
               thorn(dart.level, dart.owner, e, 1);
               Vfx.impact(dart.level, at, MOON, 0.35);
               Vfx.shards(dart.level, at, MOON.core(), 3, 0.25);
            };
         }
      }
   }

   private static void fullMoon(ServerWorld level, PlayerEntity p, Vec3d aim, float pw, float scale, LiraelArsenal.Heal heal) {
      for (LivingEntity e : disc(level, p, aim, 5.0, 1.0, 10.0)) {
         hit(level, p, e, 26.0F * pw * amp(e), false);
         harvest(level, p, e, scale, heal);
         if (!boss(e) && e.isAlive()) {
            GearKit.knockAway(e, aim, 0.6, 0.4);
         }
      }

      settle(level, p, heal);
      Vfx.beam(level, aim.add(0.0, 20.0, 0.0), aim, MOON, 2.4, 12);
      Vfx.heavyImpact(level, aim, MOON, 1.6);
      Vfx.nova(level, aim.add(0.0, 1.0, 0.0), MOON, 5.0);
      Vfx.runeCircle(level, aim, 5.0, MOON, 40, -0.06F, Vfx.Rune.ASTRAL);
      Vfx.lightning(level, aim.add(0.0, 16.0, 0.0), aim, MOON, 4);
      sfx(level, aim, "weapon.thornqueen_scepter.fullmoon", 1.8F, 0.9F);
   }

   static void moonfallLevel(ServerWorld level, PlayerEntity p, WeaponKit w, int lvl, boolean max) {
      Vec3d feet = ground(level, p.getEntityPos());
      Vec3d tip = GearKit.tip(p);
      Vfx.runeCircle(level, feet, 1.4 + 0.4 * lvl, MOON, 24, 0.05F, Vfx.Rune.ASTRAL);
      Vfx.flare(level, tip, MOON.core(), 0.7 + 0.25 * lvl, 6);
      Vfx.wisps(level, chest(p), MOON, lvl, 1.0, 20);
      if (max) {
         Vfx.chargeUp(level, tip, MOON, 12);
         Vfx.pillar(level, feet, MOON, 3.0, 0.4);
      }
   }

   static void thornsap(ServerWorld level, LivingEntity a, LivingEntity t, ItemStack s) {
      if (a instanceof PlayerEntity p) {
         glamourStrike(level, p, t);
         thorn(level, p, t, 1);
         GearKit.hitSpark(level, t, THORN);
         GearState st = GearState.of(p);
         if (blooming(t) && st.ready("lirael.sap")) {
            st.cooldown("lirael.sap", 20);
            p.heal(2.0F);
            Vfx.dashTrail(level, center(t), chest(p), FAE);
            Vfx.auraRing(level, p.getEntityPos(), FAE, 0.7);
            GearKit.procSound(level, t, "thornqueen_scepter");
         }
      }
   }

   static boolean thornsongThrust(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      Long rip = RIPOSTE_AT.get(p.getUuid());
      if (rip != null && clock - rip < 12L) {
         return false;
      }

      Vec3d dir = Vec3d.fromPolar(MathHelper.clamp(p.getPitch(), -20.0F, 20.0F), p.getYaw()).normalize();
      lunge(new LiraelArsenal.Lunge(level, p, dir, 5, 1.2, u.power(), u.empowered(), false));
      Vfx.streaks(level, chest(p), dir, FAE, 10);
      Vfx.flare(level, GearKit.tip(p), FAE.core(), 1.2, 6);
      return true;
   }

   private static void lunge(LiraelArsenal.Lunge l) {
      for (LiraelArsenal.Lunge o : LUNGES) {
         if (o.p == l.p) {
            o.done = true;
         }
      }

      LUNGES.add(l);
      l.p.setVelocity(l.dir.multiply(l.speed));
      l.p.knockedBack = true;
      l.p.onLanding();
   }

   private static void crescentCut(ServerWorld level, PlayerEntity p, Vec3d dir, float pw) {
      Vec3d flat = dir.getHorizontal().lengthSquared() > 1.0E-4 ? dir.getHorizontal().normalize() : GearKit.flatLook(p);

      for (LivingEntity e : disc(level, p, p.getEntityPos(), 3.5, 2.0, 3.0)) {
         Vec3d to = e.getEntityPos().subtract(p.getEntityPos()).getHorizontal();
         if (!(to.lengthSquared() > 1.0E-4) || !(to.normalize().dotProduct(flat) <= -0.5)) {
            hit(level, p, e, 10.0F * pw * amp(e), true);
            thorn(level, p, e, 1);
            GearKit.hitSpark(level, e, MOON);
         }
      }

      Vec3d chest = chest(p);
      float yaw = yawOf(flat);
      Vfx.slashArc(level, chest, yaw, 0.0F, 0.0F, 3.5, MOON, 240.0F);
      Vfx.slashArc(level, chest, yaw, 0.0F, 20.0F, 3.2, FAE, -200.0F);
      Vfx.ring(level, p.getEntityPos(), MOON.glow(), 3.5, 10);
      sfx(level, chest, "weapon.briar_rapier.release", 1.1F, 1.2F);
   }

   static boolean petalStep(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      float pw = u.power();
      LivingEntity target = GearKit.lookTarget(level, p, 18.0);
      Vec3d spot = null;
      if (target != null) {
         Vec3d f = Vec3d.fromPolar(0.0F, target.getYaw());
         double d = target.getWidth() / 2.0F + 1.0;

         for (Vec3d side : new Vec3d[]{f.multiply(-1.0), f.rotateY((float) (Math.PI / 2)), f.rotateY((float) (-Math.PI / 2))}) {
            Vec3d at = target.getEntityPos().add(side.multiply(d));
            if (fits(level, p, at)) {
               spot = at;
               break;
            }
         }
      } else {
         Vec3d flat = GearKit.flatLook(p);
         Vec3d from = p.getEntityPos().add(0.0, 0.9, 0.0);
         BlockHitResult hr = level.raycast(new RaycastContext(from, from.add(flat.multiply(8.0)), ShapeType.COLLIDER, FluidHandling.NONE, p));
         double reach = hr.getType() == Type.MISS ? 8.0 : hr.getPos().distanceTo(from) - 0.5;

         for (double d = reach; d >= 2.0; d -= 0.5) {
            Vec3d at = p.getEntityPos().add(flat.multiply(d));
            if (fits(level, p, at)) {
               spot = at;
               break;
            }
         }
      }

      if (spot == null) {
         return false;
      }

      Vec3d from = p.getEntityPos();
      Vec3d fromChest = chest(p);
      if (!blinkTo(level, p, spot, target)) {
         return false;
      }

      Vfx.debris(level, fromChest, PETAL, FAE.glow(), 20, 0.4);
      Vfx.vortex(level, from, 0.8, 2.2, FAE, 8);
      Vfx.flare(level, fromChest, FAE.glow(), 1.8, 6);
      Vec3d chest = chest(p);
      Vfx.dashTrail(level, fromChest, chest, FAE);
      Vfx.debris(level, chest, PETAL, FAE.glow(), 16, 0.35);
      Vfx.glint(level, chest, FAE.core(), 1.2);
      if (target != null && target.isAlive()) {
         hit(level, p, target, 10.0F * pw * amp(target), true);
         thorn(level, p, target, 2);
         resonate(p, "briar_rapier", 3.0F);
         Vfx.slashArc(level, chest, yawTo(p.getEntityPos(), target.getEntityPos()), 0.0F, -35.0F, 2.6, THORN, 150.0F);
         Vfx.impact(level, center(target), THORN, 0.7);
      }

      glamour(level, p, 60);
      if (u.empowered()) {
         DOUBLES.put(p.getUuid(), new LiraelArsenal.Echo(level, from, clock + 80L));
         Vfx.pillar(level, from, FAE, 2.0, 0.35);
         Vfx.wisps(level, from.add(0.0, 1.0, 0.0), FAE, 4, 0.5, 80);
      }

      p.onLanding();
      return true;
   }

   static boolean courtlyRiposte(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      float pw = u.power();
      float scale = 0.5F + 0.5F * pw;
      LiraelArsenal.Heal heal = new LiraelArsenal.Heal();
      LiraelArsenal.Parry pr = PARRY.remove(p.getUuid());
      LivingEntity a = pr != null && clock - pr.at() <= 6L ? pr.attacker() : null;
      if (a != null && a.isAlive() && !a.isRemoved() && a.getEntityWorld() == level) {
         RIPOSTE_AT.put(p.getUuid(), clock);
         double dist = a.distanceTo(p);
         boolean near = false;
         if (dist <= 12.0) {
            Vec3d away = p.getEntityPos().subtract(a.getEntityPos()).getHorizontal();
            Vec3d dir = away.lengthSquared() > 1.0E-4 ? away.normalize() : GearKit.flatLook(p).multiply(-1.0);
            Vec3d spot = a.getEntityPos().add(dir.multiply(a.getWidth() / 2.0F + 1.2));
            Vec3d from = chest(p);
            if (blinkTo(level, p, spot, a)) {
               near = true;
               Vfx.dashTrail(level, from, chest(p), MOON);
            } else {
               near = dist <= 4.0;
            }
         }

         if (near) {
            riposte(level, p, a, pw, scale, heal, u.empowered());
         } else {
            Vec3d tip = GearKit.tip(p);
            Vec3d to = center(a).subtract(tip);
            LiraelArsenal.Dart d = dart(level, p, 2, tip, (to.lengthSquared() > 1.0E-4 ? to.normalize() : p.getRotationVector()).multiply(1.6), 30, 0.8);
            if (d != null) {
               d.target = a;
               d.homing = 0.2;
               d.homingDelay = 0;
               d.dmg = 16.0F * pw;
               d.onHit = (dart, e, at) -> {
                  hit(dart.level, dart.owner, e, dart.dmg * amp(e), false);
                  thorn(dart.level, dart.owner, e, 2);
                  harvest(dart.level, dart.owner, e, scale, heal);
                  settle(dart.level, dart.owner, heal);
                  Vfx.impact(dart.level, at, MOON, 0.8);
                  Vfx.slashArc(dart.level, center(e), yawOf(dart.vel), 0.0F, 45.0F, 2.0, MOON, 150.0F);
                  Vfx.glint(dart.level, at, MOON.core(), 1.2);
               };
            }

            Vfx.slashArc(level, chest(p), p.getYaw(), p.getPitch(), 0.0F, 2.6, MOON, 150.0F);
            Vfx.flare(level, tip, MOON.core(), 1.4, 6);
         }
      } else {
         feint(level, p, pw);
      }

      settle(level, p, heal);
      return true;
   }

   private static void riposte(ServerWorld level, PlayerEntity p, LivingEntity a, float pw, float scale, LiraelArsenal.Heal heal, boolean emp) {
      hit(level, p, a, 20.0F * pw * amp(a), true);
      thorn(level, p, a, 3);
      harvest(level, p, a, scale, heal);
      if (!boss(a) && a.isAlive()) {
         GearKit.knockAway(a, p.getEntityPos(), 0.6, 0.25);
         a.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 40, 0), p);
      }

      resonate(p, "briar_rapier", 4.0F);
      Vec3d chest = chest(p);
      float yaw = yawTo(p.getEntityPos(), a.getEntityPos());
      float pitch = p.getPitch();
      Vfx.slashArc(level, chest, yaw, pitch, 45.0F, 3.0, MOON, 170.0F);
      Vfx.slashArc(level, chest, yaw, pitch, -45.0F, 3.0, THORN, -170.0F);
      Vfx.heavyImpact(level, a.getEntityPos(), MOON, 0.8);
      Vfx.impactFrame(level, center(a), 6.0, MOON.core(), 0.3F);
      if (emp) {
         for (LivingEntity e : disc(level, p, p.getEntityPos(), 5.0, 2.0, 3.0)) {
            if (e != a) {
               hit(level, p, e, 12.0F * pw * amp(e), true);
               harvest(level, p, e, scale, heal);
               GearKit.hitSpark(level, e, MOON);
            }
         }

         Vfx.slashArc(level, chest, yaw, 0.0F, 0.0F, 5.0, MOON, 360.0F);
         Vfx.nova(level, chest, MOON, 5.0);
         sfx(level, chest, "weapon.briar_rapier.release", 1.2F, 0.9F);
      }
   }

   private static void feint(ServerWorld level, PlayerEntity p, float pw) {
      Vec3d dir = Vec3d.fromPolar(MathHelper.clamp(p.getPitch(), -20.0F, 20.0F), p.getYaw()).normalize();
      lunge(new LiraelArsenal.Lunge(level, p, dir, 3, 1.1666666666666667, pw, false, true));
      Vec3d chest = chest(p);
      Vfx.streaks(level, chest, dir, FAE, 6);
      Vfx.slashArc(level, chest, p.getYaw(), p.getPitch(), 90.0F, 2.4, FAE, 60.0F);
      Vfx.glint(level, GearKit.tip(p), FAE.core(), 0.8);
   }

   static void riposteLevel(ServerWorld level, PlayerEntity p, WeaponKit w, int lvl, boolean max) {
      Vec3d tip = GearKit.tip(p);
      Vfx.ring(level, p.getEntityPos(), MOON.glow(), 1.0, 20);
      Vfx.flare(level, tip, MOON.glow(), 0.8, 20);
      Vfx.glint(level, tip, MOON.core(), 0.8);
   }

   private static boolean parry(ServerPlayerEntity p, DamageSource source) {
      UUID id = p.getUuid();
      LiraelArsenal.Parry frame = FRAMES.get(id);
      if (frame != null) {
         if (clock - frame.at() > 4L) {
            FRAMES.remove(id);
         } else if (source.getAttacker() == frame.attacker() && !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            deflect(source);
            return true;
         }
      }

      if (p.isUsingItem() && p.getActiveItem().getItem() instanceof MysticWeapon w && "briar_rapier".equals(w.gearId())) {
         int held = p.getItemUseTime();
         if (held < 5 || held > 25) {
            return false;
         } else if (!(source.getAttacker() instanceof LivingEntity a && a != p && !(a instanceof PlayerEntity))) {
            return false;
         } else if (!source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)
            && !source.isIn(DamageTypeTags.IS_FALL)
            && !source.isIn(DamageTypeTags.IS_FIRE)
            && !source.isIn(DamageTypeTags.IS_DROWNING)
            && !source.isIn(DamageTypeTags.IS_FREEZING)) {
            Vec3d src = source.getPosition();
            Vec3d from = src != null ? src : a.getEntityPos();
            Vec3d to = from.subtract(p.getEntityPos()).getHorizontal();
            Vec3d look = p.getRotationVector().getHorizontal();
            if (to.lengthSquared() > 1.0E-4 && look.lengthSquared() > 1.0E-4 && to.normalize().dotProduct(look.normalize()) < COS_100) {
               return false;
            }

            if (a.distanceTo(p) > 24.0F) {
               return false;
            }

            ServerWorld level = p.getEntityWorld();
            LiraelArsenal.Parry pr = new LiraelArsenal.Parry(a, clock);
            PARRY.put(id, pr);
            FRAMES.put(id, pr);
            deflect(source);
            Vec3d pt = GearKit.eye(p).add(p.getRotationVector().multiply(0.8));
            Vfx.glint(level, pt, MOON.core(), 1.8);
            Vfx.ring(level, pt, MOON.glow(), 1.4, 8);
            Vfx.shards(level, pt, MOON.core(), 10, 0.5);
            Vfx.sparks(level, pt, FAE.core(), 12, 0.5, 0.15);
            Vfx.flash(level, pt, 10.0, MOON.core(), 0.35F);
            sfx(level, pt, "weapon.briar_rapier.parry", 1.2F, 1.3F);
            later(level, p, 1, () -> {
               if (p.isUsingItem() && p.getActiveItem().getItem() instanceof MysticWeapon mw && "briar_rapier".equals(mw.gearId())) {
                  p.stopUsingItem();
               }
            });
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static void deflect(DamageSource source) {
      if (source.getSource() instanceof ProjectileEntity pr && !pr.isRemoved()) {
         if (pr.getEntityWorld() instanceof ServerWorld level) {
            Vfx.shards(level, pr.getEntityPos(), MOON.core(), 4, 0.3);
         }

         pr.discard();
      }
   }

   static void thornbite(ServerWorld level, LivingEntity a, LivingEntity t, ItemStack s) {
      if (a instanceof PlayerEntity p) {
         glamourStrike(level, p, t);
         boolean crit = p.fallDistance > 0.0
            && !p.isOnGround()
            && !p.isClimbing()
            && !p.isTouchingWater()
            && !p.hasVehicle()
            && !p.hasStatusEffect(StatusEffects.BLINDNESS);
         thorn(level, p, t, crit ? 2 : 1);
         GearKit.hitSpark(level, t, THORN);
         if (crit) {
            Vfx.glint(level, center(t), FAE.core(), 0.9);
            GearKit.procSound(level, t, "briar_rapier");
         }

         echo(level, p, t);
      }
   }

   private static void echo(ServerWorld level, PlayerEntity p, LivingEntity t) {
      LiraelArsenal.Echo d = DOUBLES.get(p.getUuid());
      if (d != null && d.level() == level && clock < d.until() && !(t.squaredDistanceTo(d.at()) > 256.0)) {
         GearState st = GearState.of(p);
         if (st.ready("lirael.echo")) {
            st.cooldown("lirael.echo", 10);
            Vec3d from = d.at().add(0.0, 1.0, 0.0);
            later(level, p, 3, () -> {
               if (t.isAlive() && t.getEntityWorld() == level) {
                  Vec3d c = center(t);
                  Vfx.dashTrail(level, from, c, FAE);
                  Vfx.slashArc(level, c, yawTo(from, t.getEntityPos()), 0.0F, 45.0F, 1.8, FAE, 120.0F);
                  hit(level, p, t, 6.0F * amp(t), false);
                  thorn(level, p, t, 1);
                  GearKit.procSound(level, t, "briar_rapier");
               }
            });
         }
      }
   }

   static boolean petalGale(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      float pw = u.power();
      gust(level, p, 8.0, pw, false);
      if (u.empowered()) {
         later(level, p, 5, () -> gust(level, p, 10.0, pw, false));
         later(level, p, 10, () -> gust(level, p, 12.0, pw, true));
      }

      return true;
   }

   private static void gust(ServerWorld level, PlayerEntity p, double range, float pw, boolean last) {
      Vec3d chest = chest(p);
      Vec3d look = GearKit.flatLook(p);
      float yaw = p.getYaw();

      for (LivingEntity e : disc(level, p, p.getEntityPos(), range, 3.0, 4.8)) {
         Vec3d to = e.getEntityPos().subtract(p.getEntityPos()).getHorizontal();
         if (!(to.lengthSquared() > 1.0E-4) || !(to.normalize().dotProduct(look) < 0.64)) {
            hit(level, p, e, 9.0F * pw * amp(e), false);
            thorn(level, p, e, 1);
            if (!boss(e) && e.isAlive()) {
               e.addVelocity(look.x * 1.2, 0.3, look.z * 1.2);
               e.knockedBack = true;
            }

            resonate(p, "petalstorm_fan", 1.0F);
            Vec3d c = center(e);
            Vfx.impact(level, c, FAE, 0.45);
            Vfx.debris(level, c, PETAL, FAE.glow(), 4, 0.3);
         }
      }

      Box box = new Box(chest, chest).expand(range);

      for (ProjectileEntity pr : level.getEntitiesByClass(ProjectileEntity.class, box, x -> x.isAlive() && !(x.getOwner() instanceof PlayerEntity))) {
         Vec3d to = pr.getEntityPos().subtract(chest);
         Vec3d flat = to.getHorizontal();
         if (!(to.lengthSquared() > range * range)
            && !(Math.abs(to.y) > range * 0.6)
            && (!(flat.lengthSquared() > 1.0) || !(flat.normalize().dotProduct(look) < 0.64))) {
            Vec3d at = pr.getEntityPos();
            pr.discard();
            Vfx.debris(level, at, PETAL, FAE.glow(), 6, 0.3);
            Vfx.glint(level, at, FAE.core(), 0.8);
         }
      }

      Vfx.slashArc(level, chest, yaw, p.getPitch(), 0.0F, 3.5, FAE, 110.0F);

      for (int k = -1; k <= 1; k++) {
         Vfx.streaks(level, chest, look.rotateY(k * 25 * (float) (Math.PI / 180.0)), FAE, 10);
      }

      double step = range / 4.0;

      for (int k = 1; k <= 4; k++) {
         Vfx.slash(level, chest.add(look.multiply(step * k)), yaw, FAE.glow(), 1.0 + 0.5 * k);
      }

      Vfx.debris(level, chest.add(look.multiply(3.0)), PETAL, FAE.glow(), 20, 0.7);
      Vfx.debris(level, chest.add(look.multiply(range * 0.75)), PETAL, THORN.glow(), 10, 0.5);
      GearKit.kick(level, p, 0.15);
      if (last) {
         Vfx.telegraphCone(level, ground(level, p.getEntityPos()), yaw, 50.0F, range, 4, FAE);
         sfx(level, chest, "weapon.petalstorm_fan.use", 1.2F, 0.85F);
      }
   }

   static boolean wingbeatWaltz(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      float pw = u.power();

      for (LivingEntity e : disc(level, p, p.getEntityPos(), 4.5, 2.0, 3.8)) {
         hit(level, p, e, 12.0F * pw * amp(e), false);
         thorn(level, p, e, 2);
         if (!boss(e) && e.isAlive()) {
            GearKit.knockAway(e, p.getEntityPos(), 0.8, 0.35);
         }

         resonate(p, "petalstorm_fan", 2.0F);
         Vfx.impact(level, center(e), FAE, 0.4);
      }

      Vec3d chest = chest(p);
      float yaw = p.getYaw();
      Vfx.slashArc(level, chest, yaw, 0.0F, 0.0F, 4.5, FAE, 360.0F);
      Vfx.slashArc(level, chest, yaw + 180.0F, 0.0F, 18.0F, 4.0, THORN, -330.0F);
      Vfx.shockwave(level, ground(level, p.getEntityPos()), FAE, 4.5);
      Vfx.debris(level, chest, PETAL, FAE.glow(), 24, 0.55);
      Vfx.streaks(level, p.getEntityPos(), UP, FAE, 10);
      Vfx.wisps(level, chest, FAE, 4, 1.2, 30);
      Vec3d f = GearKit.flatLook(p);
      p.setVelocity(-f.x * 0.45, 0.85, -f.z * 0.45);
      p.knockedBack = true;
      p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 50, 0, true, false, true));
      p.onLanding();

      for (LiraelArsenal.Glide g : GLIDES) {
         if (g.p == p) {
            g.done = true;
         }
      }

      GLIDES.add(new LiraelArsenal.Glide(level, p, pw, u.empowered()));
      return true;
   }

   private static void secondWhirl(LiraelArsenal.Glide g) {
      ServerWorld level = g.level;
      PlayerEntity p = g.p;

      for (LivingEntity e : disc(level, p, p.getEntityPos(), 5.5, 2.5, 3.5)) {
         hit(level, p, e, 12.0F * g.pw * amp(e), false);
         thorn(level, p, e, 2);
         if (!boss(e) && e.isAlive()) {
            GearKit.knockAway(e, p.getEntityPos(), 0.9, 0.4);
         }
      }

      Vec3d chest = chest(p);
      Vfx.slashArc(level, chest, p.getYaw(), 0.0F, 0.0F, 5.5, FAE, 360.0F);
      Vfx.nova(level, chest, FAE, 5.5);
      Vfx.debris(level, chest, PETAL, FAE.glow(), 30, 0.7);
      sfx(level, chest, "weapon.petalstorm_fan.alt", 1.1F, 1.2F);
   }

   static boolean petalTempest(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      int lvl = Math.max(1, u.charge());
      Vec3d f = GearKit.flatLook(p);

      for (LiraelArsenal.Tempest old : TEMPESTS) {
         if (old.p == p && !old.done) {
            burst(old);
         }
      }

      LiraelArsenal.Tempest t = new LiraelArsenal.Tempest(level, p, ground(level, p.getEntityPos().add(f.multiply(2.0))), f, lvl, u.power(), u.empowered());
      TEMPESTS.add(t);
      Vec3d chest = chest(p);
      Vfx.vortex(level, t.pos, t.r, 3.6, FAE, 10);
      Vfx.debris(level, chest, PETAL, FAE.glow(), 16, 0.6);
      Vfx.streaks(level, chest, f, FAE, 10);
      Vfx.flare(level, GearKit.tip(p), FAE.core(), 1.4, 6);
      return true;
   }

   private static void burst(LiraelArsenal.Tempest t) {
      if (!t.done) {
         t.done = true;
         ServerWorld level = t.level;
         PlayerEntity p = t.p;

         for (LivingEntity e : disc(level, p, t.pos, t.r + 1.0, 3.5, 3.5)) {
            hit(level, p, e, (10 + 4 * t.lvl) * t.pw * amp(e), false);
            harvest(level, p, e, t.scale, t.heal);
            if (!boss(e) && e.isAlive()) {
               GearKit.knockAway(e, t.pos, 0.7, 0.5);
            }
         }

         Vec3d c = t.pos.add(0.0, 1.0, 0.0);
         Vfx.nova(level, c, FAE, t.r + 1.0);
         Vfx.explosion(level, c, FAE, t.r);
         Vfx.debris(level, t.pos.add(0.0, 1.5, 0.0), PETAL, FAE.glow(), 30, 0.8);
         sfx(level, t.pos, "weapon.petalstorm_fan.burst", 1.4F, 1.1F);
         if (p.getEntityWorld() == level) {
            settle(level, p, t.heal);
         }

         if (t.empowered) {
            for (Entry<LivingEntity, Integer> en : t.held.entrySet()) {
               if (en.getValue() >= 20 && en.getKey() instanceof MobEntity m && m.isAlive() && !boss(m)) {
                  beguile(level, p, m);
               }
            }
         }
      }
   }

   static void tempestLevel(ServerWorld level, PlayerEntity p, WeaponKit w, int lvl, boolean max) {
      Vec3d chest = chest(p);
      Vfx.ring(level, p.getEntityPos(), FAE.glow(), 1.0 + 0.5 * lvl, 10);
      Vfx.debris(level, chest, PETAL, FAE.glow(), 6 + 4 * lvl, 0.45);
      Vfx.flare(level, GearKit.tip(p), FAE.core(), 0.8 + 0.3 * lvl, 6);
      if (max) {
         Vfx.chargeUp(level, chest, FAE, 10);
      }
   }

   static void scatteringPetals(ServerWorld level, LivingEntity a, LivingEntity t, ItemStack s) {
      if (a instanceof PlayerEntity p) {
         glamourStrike(level, p, t);
         thorn(level, p, t, 1);
         GearKit.hitSpark(level, t, FAE);
         GearState st = GearState.of(p);
         if (blooming(t) && st.ready("lirael.scatter")) {
            st.cooldown("lirael.scatter", 10);
            Vec3d c = center(t);
            List<LivingEntity> near = GearKit.enemies(level, p, c, 7.0)
               .stream()
               .filter(ex -> ex != t && boxDist(ex.getBoundingBox(), c) <= 5.0)
               .sorted(Comparator.comparingDouble(ex -> ex.squaredDistanceTo(c)))
               .limit(2L)
               .toList();

            for (LivingEntity e : near) {
               later(level, p, 2, () -> {
                  if (e.isAlive()) {
                     hit(level, p, e, 4.0F * amp(e), false);
                     thorn(level, p, e, 1);
                     Vfx.dashTrail(level, c, center(e), FAE);
                     Vfx.debris(level, center(e), PETAL, FAE.glow(), 5, 0.3);
                  }
               });
            }

            if (!near.isEmpty()) {
               GearKit.procSound(level, t, "petalstorm_fan");
            }
         }
      }
   }

   private static void moonlitGrace(ServerWorld level, PlayerEntity p, int pieces) {
      if (clock % 2L == 0L
         && !p.isOnGround()
         && p.isSneaking()
         && p.getMovement().y < -0.15
         && !p.isGliding()
         && !p.getAbilities().flying
         && !p.isTouchingWater()) {
         p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 6, 0, true, false, false));
         if (clock % 4L == 0L) {
            Vfx.trail(level, p.getEntityPos().add(0.0, 1.2, 0.0), FAE, 0.35);
         }

         if (clock % 8L == 0L) {
            Vfx.debris(level, p.getEntityPos().add(0.0, 1.0, 0.0), PETAL, FAE.glow(), 2, 0.12);
         }
      }

      if (clock % 20L == 0L && level.isNight() && level.isSkyVisible(p.getBlockPos().up())) {
         GearKit.keep(p, StatusEffects.REGENERATION, 0);
         if (clock % 60L == 0L) {
            Vfx.auraRing(level, p.getEntityPos(), MOON, 0.8);
         }
      }
   }

   private static void briarMantle(ServerWorld level, PlayerEntity p, int pieces) {
      boolean ready = GearState.of(p).ready("lirael.last_bloom");
      Boolean was = LAST_READY.put(p.getUuid(), ready);
      Vec3d chest = chest(p);
      if (ready && was != null && !was) {
         Vfx.debris(level, chest, PETAL, THORN.glow(), 6, 0.2);
         Vfx.auraRing(level, p.getEntityPos(), THORN, 0.8);
      }

      if (ready && clock % 80L == 0L) {
         Vfx.glint(level, jitter(level, chest, 0.4), THORN.glow(), 0.4);
      }
   }

   private static boolean graceGuard(ServerWorld level, PlayerEntity p, DamageSource source, float amount, int pieces) {
      if (source.isIn(DamageTypeTags.IS_FALL)) {
         return false;
      }

      Long bud = BUD_UNTIL.get(p.getUuid());
      return bud == null || bud <= clock || source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY);
   }

   private static void briarPrick(ServerWorld level, PlayerEntity p, @Nullable LivingEntity attacker, DamageSource source, float taken, int pieces) {
      if (pieces >= 4
         && attacker != null
         && source.getSource() == attacker
         && !source.isOf(DamageTypes.THORNS)
         && attacker.isAlive()
         && !(attacker.squaredDistanceTo(p) > 36.0)) {
         Long last = PRICKED.get(attacker.getId());
         if (last == null || clock - last >= 10L) {
            PRICKED.put(attacker.getId(), clock);
            attacker.timeUntilRegen = 0;
            attacker.damage(level, p.getDamageSources().thorns(p), 4.0F);
            thorn(level, p, attacker, 2);
            Vfx.claw(level, center(attacker), yawTo(p.getEntityPos(), attacker.getEntityPos()), 0.0F, 1.2, THORN);
            GearKit.proc(level, attacker, THORN);
            GearKit.procSound(level, attacker, "briar_rapier");
         }
      }
   }

   private static boolean lastBloom(ServerWorld level, PlayerEntity p, DamageSource source, float amount, int pieces) {
      if (pieces >= 4 && !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         GearState st = GearState.of(p);
         if (!st.ready("lirael.last_bloom")) {
            return true;
         }

         st.cooldown("lirael.last_bloom", 3600);
         p.setHealth(1.0F);
         BUD_UNTIL.put(p.getUuid(), clock + 30L);

         for (StatusEffectInstance fx : new ArrayList<StatusEffectInstance>(p.getStatusEffects())) {
            if (((StatusEffect)fx.getEffectType().value()).getCategory() == StatusEffectCategory.HARMFUL) {
               p.removeStatusEffect(fx.getEffectType());
            }
         }

         p.extinguish();
         Vec3d dm = p.getVelocity();
         p.setVelocity(0.0, Math.min(0.0, dm.y), 0.0);
         p.knockedBack = true;
         p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 30, 6, false, false, true));
         Vec3d chest = chest(p);
         Vec3d feet = ground(level, p.getEntityPos());
         Vfx.shieldBubble(level, chest, 1.3, THORN, 30);
         Vfx.vortex(level, feet, 1.2, 2.4, ECLIPSE, 30);
         Vfx.runeCircle(level, feet, 2.5, THORN, 30, 0.08F, Vfx.Rune.GLYPH);
         Vfx.debris(level, chest, PETAL, THORN.glow(), 14, 0.3);
         sfx(level, chest, "weapon.thornqueen_scepter.lastbloom", 1.4F, 1.0F);
         later(level, p, 30, () -> bloomBack(level, p));
         return false;
      } else {
         return true;
      }
   }

   private static void bloomBack(ServerWorld level, PlayerEntity p) {
      BUD_UNTIL.remove(p.getUuid());
      p.setHealth(Math.max(p.getHealth(), p.getMaxHealth() * 0.4F));

      for (LivingEntity e : disc(level, p, p.getEntityPos(), 5.0, 3.0, 3.0)) {
         hit(level, p, e, 12.0F * amp(e), false);
         thorn(level, p, e, 2);
         if (!boss(e) && e.isAlive()) {
            GearKit.knockAway(e, p.getEntityPos(), 1.0, 0.4);
         }
      }

      glamour(level, p, 60);
      Vec3d chest = chest(p);
      Vec3d feet = ground(level, p.getEntityPos());
      Vfx.nova(level, chest, THORN, 5.0);
      Vfx.heavyImpact(level, feet, FAE, 1.2);
      Vfx.debris(level, chest, PETAL, FAE.glow(), 30, 0.8);
      Vfx.radialFlash(level, chest, 10.0, FAE.glow(), 0.5F);
      Vfx.auraRing(level, feet, FAE, 1.4);
      sfx(level, chest, "weapon.thornqueen_scepter.bloom", 1.4F, 0.9F);
   }

   static boolean thornbloom(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      Vec3d c = ground(level, p.getEntityPos());
      LiraelArsenal.Heal heal = new LiraelArsenal.Heal();
      p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 36, 0, true, false, true));
      Vec3d dm = p.getMovement();
      p.setVelocity(dm.x * 0.3, 0.35, dm.z * 0.3);
      p.knockedBack = true;
      p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 40, 0, true, false, true));
      Vec3d chest = chest(p);
      Vfx.runeCircle(level, c, 9.5, THORN, 40, 0.05F, Vfx.Rune.GLYPH);
      Vfx.telegraphCircle(level, c, 3.5, 8, THORN);
      Vfx.telegraphCircle(level, c, 6.5, 16, THORN);
      Vfx.telegraphCircle(level, c, 9.5, 24, THORN);
      Vfx.wisps(level, chest, FAE, 8, 1.8, 40);
      Vfx.chargeUp(level, chest, THORN, 8);
      double[] radii = new double[]{3.5, 6.5, 9.5};
      float[] pitches = new float[]{1.2F, 1.0F, 0.85F};

      for (int k = 1; k <= 3; k++) {
         int wave = k;
         later(level, p, 8 * k, () -> thornWave(level, p, c, radii[wave - 1], wave, pitches[wave - 1]));
      }

      later(level, p, 32, () -> {
         for (LivingEntity e : disc(level, p, c, 9.5, 3.0, 3.0)) {
            harvest(level, p, e, 1.0F, heal);
         }

         settle(level, p, heal);
         Vec3d ch = chest(p);
         Vfx.nova(level, ch, FAE, 9.5);
         Vfx.heavyImpact(level, c, THORN, 1.6);
         Vfx.debris(level, ch, PETAL, FAE.glow(), 30, 0.9);
         Vfx.radialFlash(level, ch, 14.0, THORN.glow(), 0.45F);
      });
      return true;
   }

   private static void thornWave(ServerWorld level, PlayerEntity p, Vec3d c, double r, int k, float pitch) {
      for (LivingEntity e : disc(level, p, c, r, 3.0, 3.0)) {
         hit(level, p, e, 12.0F * amp(e), false);
         thorn(level, p, e, 2);
         root(e, 20);
      }

      Vfx.shockwave(level, c, THORN, r);
      Vfx.groundCrack(level, c, r, THORN);
      int n = 3 + 3 * k;
      double phase = level.getRandom().nextDouble() * Math.PI * 2.0;

      for (int i = 0; i < n; i++) {
         double a = phase + i * Math.PI * 2.0 / n;
         Vec3d rim = ground(level, c.add(Math.cos(a) * r, 0.0, Math.sin(a) * r));
         Vfx.pillar(level, rim, THORN, 1.4 + 0.3 * k, 0.25);
         if (i % 2 == 0) {
            Vfx.shards(level, rim.add(0.0, 0.3, 0.0), THORN.core(), 5, 0.45);
         }

         Vfx.debris(level, rim, PETAL, THORN.glow(), 4, 0.35);
      }

      sfx(level, c, "weapon.thornqueen_scepter.thornbloom", 1.3F, pitch);
   }

   static int thorn(ServerWorld level, @Nullable PlayerEntity src, LivingEntity t, int n) {
      if (n > 0 && !(t instanceof PlayerEntity) && t != src && t.isAlive() && !t.isRemoved()) {
         LiraelArsenal.Thorns th = THORNS.get(t);
         if (th == null) {
            th = new LiraelArsenal.Thorns();
            th.last = t.getEntityPos();
            th.nextTear = clock + 10L;
            THORNS.put(t, th);
         }

         th.level = level;
         if (src != null) {
            th.owner = src;
         }

         Vec3d c = center(t);
         Vfx.sparks(level, c, THORN.glow(), 1 + n, 0.12, 0.08);
         if (th.bloomUntil > clock) {
            return 0;
         }

         th.stacks = Math.min(5, th.stacks + n);
         th.expires = clock + 120L;
         if (th.stacks >= 5) {
            bloom(level, src, t, th);
            return 0;
         }

         if (th.stacks >= 3) {
            Vfx.glint(level, jitter(level, c, 0.4), THORN.glow(), 0.25 + 0.05 * th.stacks);
         }

         return th.stacks;
      } else {
         return stacks(t);
      }
   }

   private static void bloom(ServerWorld level, @Nullable PlayerEntity src, LivingEntity t, LiraelArsenal.Thorns th) {
      th.stacks = 0;
      th.bloomUntil = clock + 100L;
      root(t, 30);
      Vec3d c = center(t);
      Vec3d top = top(t);
      Vfx.debris(level, c, PETAL, THORN.glow(), 12, 0.25);
      Vfx.flare(level, top, THORN.glow(), 1.4, 10);
      Vfx.ring(level, t.getEntityPos(), THORN.glow(), 1.2 + t.getWidth() * 0.5, 14);
      Vfx.glint(level, top, FAE.core(), 1.0);
      Vfx.wisps(level, c, THORN, 5, 0.5 + 0.5 * t.getWidth(), 100);
      if (src != null && stamp(BLOOM_SFX, src, 10)) {
         sfx(level, c, "weapon.thornqueen_scepter.bloom", 0.8F, 1.15F);
      }
   }

   static boolean blooming(LivingEntity t) {
      LiraelArsenal.Thorns th = THORNS.get(t);
      return th != null && th.bloomUntil > clock;
   }

   static float amp(LivingEntity t) {
      return blooming(t) ? 1.2F : 1.0F;
   }

   static int stacks(LivingEntity t) {
      LiraelArsenal.Thorns th = THORNS.get(t);
      return th == null ? 0 : th.stacks;
   }

   static float harvest(ServerWorld level, PlayerEntity p, LivingEntity t, float scale, LiraelArsenal.Heal heal) {
      LiraelArsenal.Thorns th = THORNS.get(t);
      if (th != null && !t.isRemoved()) {
         Vec3d c = center(t);
         float dealt;
         if (th.bloomUntil > clock) {
            th.bloomUntil = 0L;
            dealt = 16.0F * scale;
            hit(level, p, t, dealt, false);

            for (LivingEntity e : GearKit.enemies(level, p, c, 5.0)) {
               if (e != t && boxDist(e.getBoundingBox(), c) <= 3.0) {
                  hit(level, p, e, 5.0F * scale * amp(e), false);
                  thorn(level, p, e, 1);
               }
            }

            heal.owed += 4.0F;
            Vfx.explosion(level, c, THORN, 1.4);
            Vfx.debris(level, c, PETAL, FAE.glow(), 16, 0.5);
            Vfx.glint(level, c, FAE.core(), 1.3);
         } else {
            if (th.stacks <= 0) {
               return 0.0F;
            }

            int n = th.stacks;
            th.stacks = 0;
            dealt = 2.5F * n * scale;
            hit(level, p, t, dealt, false);
            heal.owed += 0.6F * n;
            Vfx.impact(level, c, THORN, 0.5 + 0.1 * n);
            Vfx.shards(level, c, THORN.core(), 3 + n, 0.35);
         }

         if (heal.streams++ < 4) {
            Vfx.dashTrail(level, c, chest(p), FAE);
         }

         return dealt;
      } else {
         return 0.0F;
      }
   }

   static void settle(ServerWorld level, PlayerEntity p, LiraelArsenal.Heal heal) {
      if (!(heal.owed <= 0.0F)) {
         float amount = Math.min(heal.owed, 8.0F - heal.given);
         heal.owed = 0.0F;
         if (!(amount <= 0.0F) && p.isAlive() && p.getEntityWorld() == level) {
            heal.given += amount;
            p.heal(amount);
            Vfx.auraRing(level, p.getEntityPos(), FAE, 0.9);
            Vfx.glint(level, chest(p), FAE.core(), 0.7);

            for (ServerPlayerEntity o : level.getPlayers()) {
               if (o != p && o.isAlive() && !o.isSpectator() && o.squaredDistanceTo(p) <= 64.0) {
                  o.heal(amount / 2.0F);
                  Vfx.auraRing(level, o.getEntityPos(), FAE, 0.9);
                  Vfx.glint(level, chest(o), FAE.core(), 0.7);
               }
            }

            if (!heal.sounded) {
               heal.sounded = true;
               sfx(level, chest(p), "weapon.thornqueen_scepter.harvest", 1.0F, 1.1F);
            }
         }
      }
   }

   private static void glamour(ServerWorld level, PlayerEntity p, int ticks) {
      GLAMOUR.put(p.getUuid(), new LiraelArsenal.Glamour(p, clock, clock + ticks));
      loseTrack(level, p);
   }

   private static void loseTrack(ServerWorld level, PlayerEntity p) {
      for (MobEntity m : level.getEntitiesByClass(MobEntity.class, p.getBoundingBox().expand(16.0), x -> x.getTarget() == p && !boss(x))) {
         m.setTarget(null);
      }
   }

   private static void glamourStrike(ServerWorld level, PlayerEntity p, LivingEntity t) {
      LiraelArsenal.Glamour g = GLAMOUR.get(p.getUuid());
      if (g != null && g.p == p && clock < g.until) {
         GLAMOUR.remove(p.getUuid());
         thorn(level, p, t, 2);
         Vec3d c = center(t);
         Vfx.glint(level, c, FAE.core(), 1.2);
         Vfx.slashArc(level, chest(p), yawTo(p.getEntityPos(), t.getEntityPos()), 0.0F, -30.0F, 2.2, FAE, 120.0F);
         Vfx.debris(level, c, PETAL, FAE.glow(), 8, 0.35);
         GearKit.procSound(level, t, "briar_rapier");
         later(level, p, 1, () -> {
            if (t.isAlive() && t.getEntityWorld() == level) {
               hit(level, p, t, 6.0F * amp(t), true);
            }
         });
      }
   }

   private static void glamours() {
      if (!GLAMOUR.isEmpty()) {
         Iterator<LiraelArsenal.Glamour> it = GLAMOUR.values().iterator();

         while (it.hasNext()) {
            LiraelArsenal.Glamour g = it.next();
            PlayerEntity p = g.p;
            if (clock < g.until && p.isAlive() && !p.isRemoved() && p.getEntityWorld() instanceof ServerWorld level) {
               long age = clock - g.start;
               if (age % 5L == 0L) {
                  if (age <= 40L) {
                     loseTrack(level, p);
                  }

                  Vfx.glint(level, jitter(level, chest(p), 0.5), FAE.core(), 0.3);
               }

               if (age % 10L == 0L) {
                  Vfx.debris(level, chest(p), PETAL, FAE.glow(), 2, 0.1);
               }
            } else {
               it.remove();
            }
         }
      }
   }

   private static void beguile(ServerWorld level, PlayerEntity owner, MobEntity m) {
      BEGUILED.put(m, new LiraelArsenal.Beguile(owner, level, clock, clock + 80L));
      Vfx.wisps(level, top(m), FAE, 2, 0.45, 80);
   }

   private static void beguiles() {
      if (!BEGUILED.isEmpty()) {
         Iterator<Entry<MobEntity, LiraelArsenal.Beguile>> it = BEGUILED.entrySet().iterator();

         while (it.hasNext()) {
            Entry<MobEntity, LiraelArsenal.Beguile> en = it.next();
            MobEntity m = en.getKey();
            LiraelArsenal.Beguile b = en.getValue();
            if (m != null && m.isAlive() && !m.isRemoved() && m.getEntityWorld() == b.level && clock < b.until) {
               long age = clock - b.start;
               if (age % 5L == 0L) {
                  LivingEntity best = null;
                  LivingEntity bestBoss = null;

                  for (LivingEntity e : GearKit.enemies(b.level, b.owner, m.getEntityPos(), 12.0)) {
                     if (e != m && e instanceof Monster) {
                        if (boss(e)) {
                           if (bestBoss == null || e.squaredDistanceTo(m) < bestBoss.squaredDistanceTo(m)) {
                              bestBoss = e;
                           }
                        } else if (best == null || e.squaredDistanceTo(m) < best.squaredDistanceTo(m)) {
                           best = e;
                        }
                     }
                  }

                  m.setTarget(best != null ? best : bestBoss);
               }

               if (age % 10L == 0L) {
                  Vfx.glint(b.level, top(m), FAE.glow(), 0.5);
               }
            } else {
               it.remove();
            }
         }
      }
   }

   private static void bramble(ServerWorld level, PlayerEntity p, Vec3d c, double r) {
      LiraelArsenal.Bramble oldest = null;
      int mine = 0;

      for (LiraelArsenal.Bramble b : BRAMBLES) {
         if (b.owner() == p) {
            mine++;
            if (oldest == null || b.born() < oldest.born()) {
               oldest = b;
            }
         }
      }

      if (mine >= 3) {
         BRAMBLES.remove(oldest);
      }

      if (BRAMBLES.size() >= 24) {
         BRAMBLES.remove(0);
      }

      BRAMBLES.add(new LiraelArsenal.Bramble(p, level, c, r, clock, clock + 100L));
      Vfx.groundCrack(level, c, r, THORN);
      Vfx.runeCircle(level, c, r, THORN, 100, 0.02F, Vfx.Rune.GLYPH);
   }

   private static void brambles() {
      if (!BRAMBLES.isEmpty()) {
         boolean pulse = clock % 20L == 0L;
         Map<PlayerEntity, Set<LivingEntity>> pulsed = new HashMap<>();
         List<Runnable> hits = new ArrayList<>();
         Iterator<LiraelArsenal.Bramble> it = BRAMBLES.iterator();

         while (it.hasNext()) {
            LiraelArsenal.Bramble b = it.next();
            if (clock < b.until() && alive(b.owner(), b.level())) {
               ServerWorld level = b.level();
               if (clock - b.born() == 60L) {
                  Vfx.groundCrack(level, b.c(), b.r(), THORN);
               }

               if (pulse) {
                  Vfx.debris(level, inDisc(level, b.c(), b.r()).add(0.0, 0.3, 0.0), LEAF, THORN.smoke(), 2, 0.15);
                  Vfx.sparks(level, inDisc(level, b.c(), b.r()).add(0.0, 0.4, 0.0), THORN.glow(), 2, 0.1, 0.08);
                  Set<LivingEntity> done = pulsed.computeIfAbsent(b.owner(), k -> new HashSet<>());
                  PlayerEntity owner = b.owner();

                  for (LivingEntity e : disc(level, owner, b.c(), b.r(), 1.5, 1.5)) {
                     if (done.add(e)) {
                        hits.add(() -> {
                           tear(level, owner, e, 2.0F);
                           thorn(level, owner, e, 1);
                           if (!boss(e) && e.isAlive()) {
                              e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 30, 1, false, false, true));
                           }
                        });
                     }
                  }
               }
            } else {
               it.remove();
            }
         }

         hits.forEach(Runnable::run);
      }
   }

   private static boolean stepLunge(LiraelArsenal.Lunge l) {
      PlayerEntity p = l.p;
      if (!l.done && alive(p, l.level)) {
         ServerWorld level = l.level;
         l.age++;
         Vec3d now = chest(p);
         cut(l, l.prev, now.add(l.dir.multiply(0.8)));
         if (now.squaredDistanceTo(l.prev) > 0.01) {
            Vfx.dashTrail(level, l.prev, now, FAE);
         }

         l.prev = now;
         boolean wall = l.age > 1 && p.horizontalCollision;
         if (!l.done && l.age < l.ticks && !wall) {
            p.setVelocity(l.dir.multiply(l.speed));
            p.knockedBack = true;
            p.onLanding();
            return false;
         }

         if (!l.done && !wall) {
            Vec3d goal = l.start.add(l.dir.multiply(l.speed * l.ticks + 0.8));
            Vec3d rest = level.raycast(new RaycastContext(now, goal, ShapeType.COLLIDER, FluidHandling.NONE, p)).getPos();
            if (rest.squaredDistanceTo(now) > 0.04) {
               cut(l, now, rest);
            }
         }

         p.setVelocity(p.getVelocity().multiply(0.25));
         p.knockedBack = true;
         if (!l.feint) {
            Vfx.beam(level, l.start, now, FAE, 0.25, 6);
            Vfx.flare(level, GearKit.tip(p), FAE.core(), 1.4, 6);
            if (l.empowered) {
               crescentCut(level, p, l.dir, l.pw);
            }
         }

         return true;
      } else {
         return true;
      }
   }

   private static void cut(LiraelArsenal.Lunge l, Vec3d a, Vec3d b) {
      ServerWorld level = l.level;
      PlayerEntity p = l.p;

      for (LivingEntity e : nearSegment(level, p, a, b, 1.3)) {
         if (l.done) {
            return;
         }

         if (l.hit.add(e)) {
            Vec3d c = center(e);
            if (l.feint) {
               hit(level, p, e, 10.0F * l.pw * amp(e), true);
               thorn(level, p, e, 1);
               GearKit.hitSpark(level, e, FAE);
               Vfx.glint(level, c, FAE.core(), 0.8);
               l.done = true;
               return;
            }

            hit(level, p, e, 12.0F * l.pw * amp(e), true);
            thorn(level, p, e, 2);
            if (!boss(e) && e.isAlive()) {
               e.addVelocity(l.dir.x * 0.3, 0.1, l.dir.z * 0.3);
               e.knockedBack = true;
            }

            resonate(p, "briar_rapier", 2.0F);
            Vfx.glint(level, c, FAE.core(), 1.0);
            Vfx.impact(level, c, THORN, 0.6);
            Vfx.streaks(level, c, l.dir, THORN, 6);
            Vfx.debris(level, c, PETAL, THORN.glow(), 5, 0.35);
         }
      }
   }

   private static boolean stepGlide(LiraelArsenal.Glide g) {
      PlayerEntity p = g.p;
      if (!g.done && alive(p, g.level)) {
         g.age++;
         p.onLanding();
         if (g.age % 2 == 0) {
            Vfx.trail(g.level, p.getEntityPos().add(0.0, 0.9, 0.0), FAE, 0.4);
         }

         if (g.age % 6 == 0) {
            Vfx.debris(g.level, p.getEntityPos().add(0.0, 1.0, 0.0), PETAL, FAE.glow(), 2, 0.15);
         }

         boolean landed = g.age >= 6 && (p.isOnGround() || p.isTouchingWater());
         if (g.empowered && !g.whirled && (landed || g.age >= 40)) {
            g.whirled = true;
            secondWhirl(g);
         }

         return landed || g.age >= 50;
      } else {
         return true;
      }
   }

   private static boolean stepTempest(LiraelArsenal.Tempest t) {
      if (t.done) {
         return true;
      }

      PlayerEntity p = t.p;
      if (p.isAlive() && !p.isRemoved()) {
         if (p.getEntityWorld() != t.level) {
            burst(t);
            return true;
         }

         ServerWorld level = t.level;
         t.age++;
         Vec3d g = floor(level, t.pos.add(t.dir.multiply(0.3)), 1.0, 3);
         if (g != null && !(g.y - t.pos.y > 1.05)) {
            t.pos = g;

            for (LivingEntity e : disc(level, p, t.pos, t.r + 2.0, 3.0, 3.0)) {
               if (!boss(e)) {
                  Vec3d to = t.pos.subtract(e.getEntityPos()).getHorizontal();
                  double d = to.length();
                  if (!(d < 0.001)) {
                     Vec3d pull = to.multiply(Math.min(1.0, d) * 0.12 / d);
                     e.addVelocity(pull.x, flatDist(e, t.pos) <= t.r ? 0.04 : 0.0, pull.z);
                     e.knockedBack = true;
                  }
               }
            }

            if (t.age % 10 == 0) {
               for (LivingEntity e : disc(level, p, t.pos, t.r, 3.5, 3.5)) {
                  hit(level, p, e, 4.0F * t.pw * amp(e), false);
                  thorn(level, p, e, 1);
                  t.held.merge(e, 10, Integer::sum);
               }

               Vfx.streaks(level, t.pos.add(0.0, 1.5, 0.0), t.dir, FAE, 4);
               Vfx.slash(level, t.pos.add(0.0, 1.2 + level.getRandom().nextDouble(), 0.0), level.getRandom().nextFloat() * 360.0F, THORN.glow(), 1.2);
            }

            if (t.age % 5 == 0) {
               Vfx.vortex(level, t.pos, t.r, 3.6, FAE, 7);
            }

            if (t.age % 2 == 0) {
               double a = level.getRandom().nextDouble() * Math.PI * 2.0;
               double rr = level.getRandom().nextDouble() * t.r;
               Vec3d at = t.pos.add(Math.cos(a) * rr, 0.5 + level.getRandom().nextDouble() * 2.5, Math.sin(a) * rr);
               Vfx.debris(level, at, PETAL, FAE.glow(), 3, 0.4);
            }

            if (t.age >= t.life) {
               burst(t);
               return true;
            } else {
               return false;
            }
         } else {
            burst(t);
            return true;
         }
      } else {
         return true;
      }
   }

   private static void doubles() {
      if (!DOUBLES.isEmpty()) {
         Iterator<LiraelArsenal.Echo> it = DOUBLES.values().iterator();

         while (it.hasNext()) {
            LiraelArsenal.Echo d = it.next();
            if (clock >= d.until()) {
               it.remove();
            } else if ((d.until() - clock) % 10L == 0L) {
               Vfx.debris(d.level(), d.at().add(0.0, 1.0, 0.0), PETAL, FAE.glow(), 3, 0.15);
            }
         }
      }
   }

   private static void holds(MinecraftServer server) {
      for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
         if (p.isUsingItem() && p.getActiveItem().getItem() instanceof MysticWeapon w) {
            ServerWorld level = p.getEntityWorld();
            int t = p.getItemUseTime();
            switch (w.gearId()) {
               case "thornqueen_scepter":
                  if (t % 4 == 0) {
                     int n = Math.min(4, t / 10);
                     Vec3d g = aimGround(level, p, 32.0);
                     Vec3d aim = g != null ? g : GearKit.lookPoint(level, p, 32.0);
                     Vfx.ring(level, aim.add(0.0, 0.1, 0.0), MOON.glow(), 1.2 + 0.35 * n, 6);
                     Vfx.beam(level, aim.add(0.0, 14.0, 0.0), aim, MOON, 0.06 + 0.02 * n, 5);
                     if (t % 8 == 0) {
                        Vfx.glint(level, aim.add(0.0, 1.5, 0.0), MOON.core(), 0.6);
                     }
                  }
                  break;
               case "briar_rapier":
                  if (t >= 5 && t <= 25 && t % 4 == 0) {
                     Vfx.glint(level, GearKit.tip(p), MOON.core(), 0.35);
                  } else if (t == 26) {
                     Vfx.ring(level, p.getEntityPos(), FAE.smoke(), 0.8, 6);
                  }
                  break;
               case "petalstorm_fan":
                  if (t % 4 == 0) {
                     int n = Math.min(3, t / 10);
                     Vfx.vortex(level, p.getEntityPos(), 0.9 + 0.35 * n, 2.2 + 0.3 * n, FAE, 6);
                     if (t % 8 == 0) {
                        Vfx.debris(level, chest(p), PETAL, FAE.glow(), 2 + n, 0.35);
                     }
                  }
            }
         }
      }
   }

   private static LiraelArsenal.@Nullable Dart dart(ServerWorld level, PlayerEntity owner, int kind, Vec3d from, Vec3d vel, int life, double radius) {
      int mine = 0;

      for (LiraelArsenal.Dart d : DARTS) {
         if (d.owner == owner) {
            mine++;
         }
      }

      if (mine >= 20) {
         return null;
      }

      LiraelArsenal.Dart d = new LiraelArsenal.Dart(level, owner, kind, from, vel, life, radius);
      DARTS.add(d);
      return d;
   }

   private static boolean stepDart(LiraelArsenal.Dart d) {
      if (!alive(d.owner, d.level)) {
         return true;
      }

      d.age++;
      if (d.age > d.life) {
         Vfx.flare(d.level, d.pos, MOON.glow(), 0.5, 4);
         return true;
      }

      d.speed = Math.min(d.maxSpeed, d.speed + d.accel);
      Vec3d dir = d.vel.lengthSquared() > 1.0E-6 ? d.vel.normalize() : new Vec3d(0.0, 0.0, 1.0);
      LivingEntity t = d.target;
      if (t != null && d.age > d.homingDelay && t.isAlive() && !t.isRemoved() && t.getEntityWorld() == d.level) {
         Vec3d want = center(t).subtract(d.pos);
         if (want.lengthSquared() > 1.0E-4) {
            dir = dir.lerp(want.normalize(), d.homing).normalize();
         }
      }

      d.vel = dir.multiply(d.speed);
      Vec3d next = d.pos.add(d.vel);
      BlockHitResult bh = d.level.raycast(new RaycastContext(d.pos, next, ShapeType.COLLIDER, FluidHandling.NONE, d.owner));
      boolean blocked = bh.getType() != Type.MISS;
      Vec3d end = blocked ? bh.getPos() : next;

      for (LivingEntity e : nearSegment(d.level, d.owner, d.pos, end, d.radius)) {
         if (d.hit.add(e)) {
            Vec3d at = e.getBoundingBox().expand(d.radius).raycast(d.pos, end).orElse(center(e));
            if (d.onHit != null) {
               d.onHit.hit(d, e, at);
            }

            if (stamp(DART_SFX, d.owner, 3)) {
               sfx(d.level, at, "bolt.thorn.impact", 0.6F, 1.3F);
            }

            if (--d.pierce < 0) {
               return true;
            }
         }
      }

      d.pos = end;
      switch (d.kind) {
         case 0:
            Vfx.trail(d.level, d.pos, MOON, 0.3);
            if (d.age % 3 == 0) {
               Vfx.glint(d.level, d.pos, THORN.glow(), 0.22);
            }
            break;
         case 1:
            Vfx.trail(d.level, d.pos, MOON, 0.22);
            break;
         default:
            if (d.age % 2 == 0) {
               Vfx.slash(d.level, d.pos, yawOf(d.vel), MOON.glow(), 1.4);
            }

            Vfx.trail(d.level, d.pos, MOON, 0.5);
      }

      if (blocked) {
         Vfx.impact(d.level, end, MOON, 0.35);
         Vfx.shards(d.level, end, MOON.core(), 3, 0.2);
         return true;
      } else {
         return false;
      }
   }

   private static void later(ServerWorld level, PlayerEntity p, int ticks, Runnable r) {
      PENDING.add(new LiraelArsenal.Task(clock + Math.max(1, ticks), () -> {
         if (alive(p, level)) {
            r.run();
         }
      }));
   }

   private static boolean alive(PlayerEntity p, ServerWorld level) {
      return p.isAlive() && !p.isRemoved() && p.getEntityWorld() == level;
   }

   private static void tick(MinecraftServer server) {
      clock++;
      if (!PENDING.isEmpty()) {
         TASKS.addAll(PENDING);
         PENDING.clear();
      }

      if (!TASKS.isEmpty()) {
         List<LiraelArsenal.Task> due = new ArrayList<>();
         TASKS.removeIf(tx -> tx.at() <= clock && due.add(tx));

         for (LiraelArsenal.Task t : due) {
            safely(t.run(), "task");
         }
      }

      safely(LiraelArsenal::thornsTick, "thorns");
      run(DARTS, LiraelArsenal::stepDart, "dart");
      run(LUNGES, LiraelArsenal::stepLunge, "lunge");
      run(GLIDES, LiraelArsenal::stepGlide, "glide");
      run(TEMPESTS, LiraelArsenal::stepTempest, "tempest");
      safely(LiraelArsenal::brambles, "brambles");
      safely(LiraelArsenal::glamours, "glamour");
      safely(LiraelArsenal::beguiles, "beguile");
      safely(() -> holds(server), "holds");
      safely(LiraelArsenal::doubles, "doubles");
      if (clock % 200L == 0L) {
         prune();
      }
   }

   private static <T> void run(List<T> list, Predicate<T> step, String what) {
      if (!list.isEmpty()) {
         List<T> batch = new ArrayList<>(list);
         list.clear();

         for (T t : batch) {
            boolean done;
            try {
               done = step.test(t);
            } catch (RuntimeException ex) {
               MysticalTerrain.LOGGER.error("Lirael gear " + what + " failed", ex);
               done = true;
            }

            if (!done) {
               list.add(t);
            }
         }
      }
   }

   private static void safely(Runnable r, String what) {
      try {
         r.run();
      } catch (RuntimeException ex) {
         MysticalTerrain.LOGGER.error("Lirael gear " + what + " failed", ex);
      }
   }

   private static void thornsTick() {
      if (!THORNS.isEmpty()) {
         List<Runnable> tears = new ArrayList<>();
         Iterator<Entry<LivingEntity, LiraelArsenal.Thorns>> it = THORNS.entrySet().iterator();

         while (it.hasNext()) {
            Entry<LivingEntity, LiraelArsenal.Thorns> en = it.next();
            LivingEntity e = en.getKey();
            LiraelArsenal.Thorns th = en.getValue();
            if (e != null && th.level != null && e.isAlive() && !e.isRemoved() && e.getEntityWorld() == th.level) {
               ServerWorld level = th.level;
               if (th.bloomUntil > 0L && th.bloomUntil <= clock) {
                  th.bloomUntil = 0L;
                  Vfx.debris(level, center(e), PETAL, THORN.smoke(), 6, 0.15);
               }

               if (th.stacks > 0 && th.expires <= clock) {
                  th.stacks = 0;
               }

               if (th.stacks > 0 && clock >= th.nextTear) {
                  th.nextTear = clock + 10L;
                  Vec3d now = e.getEntityPos();
                  Vec3d was = th.last;
                  th.last = now;
                  if (was != null && now.subtract(was).horizontalLength() > 0.2) {
                     int n = th.stacks;
                     PlayerEntity owner = th.owner != null && th.owner.isAlive() && th.owner.getEntityWorld() == level ? th.owner : null;
                     tears.add(() -> {
                        tear(level, owner, e, 0.3F * n);
                        Vfx.sparks(level, center(e), THORN.glow(), n, 0.1, 0.07);
                     });
                  }
               } else if (th.stacks == 0) {
                  th.last = e.getEntityPos();
               }

               if (th.stacks == 0 && th.bloomUntil == 0L) {
                  it.remove();
               }
            } else {
               it.remove();
            }
         }

         tears.forEach(Runnable::run);
      }
   }

   private static void prune() {
      BLOOM_SFX.values().removeIf(t -> clock - t > 40L);
      DART_SFX.values().removeIf(t -> clock - t > 40L);
      PRICKED.values().removeIf(t -> clock - t > 40L);
      RIPOSTE_AT.values().removeIf(t -> clock - t > 40L);
      BUD_UNTIL.values().removeIf(t -> t < clock - 40L);
      PARRY.values().removeIf(pr -> clock - pr.at() > 40L);
      FRAMES.values().removeIf(pr -> clock - pr.at() > 4L);
   }

   private static void reset() {
      THORNS.clear();
      TASKS.clear();
      PENDING.clear();
      DARTS.clear();
      LUNGES.clear();
      GLIDES.clear();
      TEMPESTS.clear();
      BRAMBLES.clear();
      DOUBLES.clear();
      GLAMOUR.clear();
      BEGUILED.clear();
      PARRY.clear();
      FRAMES.clear();
      RIPOSTE_AT.clear();
      BUD_UNTIL.clear();
      BLOOM_SFX.clear();
      DART_SFX.clear();
      LAST_READY.clear();
      PRICKED.clear();
      clock = 0L;
   }

   static void hit(ServerWorld level, PlayerEntity p, LivingEntity e, float dmg, boolean melee) {
      if (e.isAlive() && !(dmg <= 0.0F)) {
         e.timeUntilRegen = 0;
         if (melee) {
            GearKit.strike(level, p, e, dmg);
         } else {
            GearKit.magic(level, p, e, dmg);
         }
      }
   }

   static void tear(ServerWorld level, @Nullable PlayerEntity p, LivingEntity e, float dmg) {
      if (e.isAlive() && !(dmg <= 0.0F)) {
         int iv = e.timeUntilRegen;
         e.timeUntilRegen = 0;
         e.damage(level, p != null ? level.getDamageSources().create(DamageTypes.MAGIC, p) : level.getDamageSources().magic(), dmg);
         e.timeUntilRegen = iv;
      }
   }

   private static void resonate(PlayerEntity p, String weapon, float amount) {
      GearState.of(p).addResonance(weapon, amount);
   }

   private static boolean stamp(Map<UUID, Long> map, PlayerEntity p, int gap) {
      Long last = map.get(p.getUuid());
      if (last != null && clock - last < gap) {
         return false;
      }

      map.put(p.getUuid(), clock);
      return true;
   }

   private static void root(LivingEntity e, int ticks) {
      if (!boss(e) && e.isAlive()) {
         e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, ticks, 4, false, false, true));
         Vec3d v = e.getVelocity();
         e.setVelocity(0.0, Math.min(0.0, v.y), 0.0);
         e.knockedBack = true;
      }
   }

   private static boolean boss(LivingEntity e) {
      return e instanceof MysticalBoss b
         ? b.isBoss()
         : e instanceof EnderDragonEntity || e instanceof WitherEntity || e instanceof WardenEntity || e instanceof ElderGuardianEntity;
   }

   private static boolean fits(ServerWorld level, PlayerEntity p, Vec3d at) {
      return level.isSpaceEmpty(p, p.getBoundingBox().offset(at.subtract(p.getEntityPos())));
   }

   private static boolean blinkTo(ServerWorld level, PlayerEntity p, Vec3d at, @Nullable LivingEntity face) {
      if (!fits(level, p, at)) {
         return false;
      }

      float yaw = p.getYaw();
      float pitch = p.getPitch();
      if (face != null) {
         yaw = yawTo(at, face.getEntityPos());
         Vec3d to = center(face).subtract(at.add(0.0, p.getEyeY() - p.getY(), 0.0));
         pitch = (float)(-(MathHelper.atan2(to.y, to.horizontalLength()) * 180.0F / (float)Math.PI));
      }

      p.teleport(level, at.x, at.y, at.z, Set.of(), yaw, pitch, true);
      p.onLanding();
      return true;
   }

   private static List<LivingEntity> disc(ServerWorld level, PlayerEntity p, Vec3d c, double r, double below, double above) {
      double h = (above + below) / 2.0 + 1.0;
      Vec3d mid = c.add(0.0, (above - below) / 2.0, 0.0);
      return GearKit.enemies(level, p, mid, r + h + 4.0)
         .stream()
         .filter(e -> flatDist(e, c) <= r && e.getBoundingBox().maxY >= c.y - below && e.getBoundingBox().minY <= c.y + above)
         .toList();
   }

   private static List<LivingEntity> nearSegment(ServerWorld level, PlayerEntity p, Vec3d a, Vec3d b, double width) {
      Vec3d mid = a.add(b).multiply(0.5);
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : GearKit.enemies(level, p, mid, a.distanceTo(b) / 2.0 + width + 4.0)) {
         Box box = e.getBoundingBox().expand(width);
         if (box.contains(a) || box.contains(b) || box.raycast(a, b).isPresent()) {
            out.add(e);
         }
      }

      out.sort(Comparator.comparingDouble(ex -> ex.squaredDistanceTo(a)));
      return out;
   }

   private static double boxDist(Box b, Vec3d at) {
      double dx = Math.max(Math.max(b.minX - at.x, 0.0), at.x - b.maxX);
      double dy = Math.max(Math.max(b.minY - at.y, 0.0), at.y - b.maxY);
      double dz = Math.max(Math.max(b.minZ - at.z, 0.0), at.z - b.maxZ);
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   private static double flatDist(LivingEntity e, Vec3d at) {
      double dx = e.getX() - at.x;
      double dz = e.getZ() - at.z;
      return Math.max(0.0, Math.sqrt(dx * dx + dz * dz) - e.getWidth() / 2.0F);
   }

   private static Vec3d center(LivingEntity e) {
      return e.getBoundingBox().getCenter();
   }

   private static Vec3d chest(LivingEntity e) {
      return e.getEntityPos().add(0.0, e.getHeight() * 0.6, 0.0);
   }

   private static Vec3d top(LivingEntity e) {
      return new Vec3d(e.getX(), e.getBoundingBox().maxY + 0.3, e.getZ());
   }

   private static float yawTo(Vec3d from, Vec3d to) {
      return (float)(MathHelper.atan2(to.z - from.z, to.x - from.x) * 180.0F / (float)Math.PI) - 90.0F;
   }

   private static float yawOf(Vec3d dir) {
      return (float)(MathHelper.atan2(dir.z, dir.x) * 180.0F / (float)Math.PI) - 90.0F;
   }

   private static Vec3d jitter(ServerWorld level, Vec3d at, double r) {
      Random rand = level.getRandom();
      return at.add((rand.nextDouble() * 2.0 - 1.0) * r, (rand.nextDouble() * 2.0 - 1.0) * r, (rand.nextDouble() * 2.0 - 1.0) * r);
   }

   private static Vec3d inDisc(ServerWorld level, Vec3d c, double r) {
      double a = level.getRandom().nextDouble() * Math.PI * 2.0;
      double d = Math.sqrt(level.getRandom().nextDouble()) * r;
      return c.add(Math.cos(a) * d, 0.0, Math.sin(a) * d);
   }

   private static Vec3d ground(ServerWorld level, Vec3d at) {
      Vec3d g = floor(level, at, 1.5, 8);
      return g != null ? g : at;
   }

   private static @Nullable Vec3d aimGround(ServerWorld level, PlayerEntity p, double range) {
      Vec3d lp = GearKit.lookPoint(level, p, range).subtract(p.getRotationVector().multiply(0.2));
      return floor(level, lp, 0.0, 8);
   }

   private static @Nullable Vec3d floor(ServerWorld level, Vec3d at, double up, int depth) {
      Mutable q = BlockPos.ofFloored(at.x, at.y + up, at.z).mutableCopy();
      int steps = depth + (int)Math.ceil(up) + 1;

      for (int i = 0; i < steps; i++) {
         BlockState s = level.getBlockState(q);
         VoxelShape shape = s.getCollisionShape(level, q);
         if (!shape.isEmpty()) {
            return new Vec3d(at.x, q.getY() + shape.getMax(Axis.Y), at.z);
         }

         FluidState f = s.getFluidState();
         if (!f.isEmpty()) {
            return new Vec3d(at.x, q.getY() + f.getHeight(level, q), at.z);
         }

         q.move(0, -1, 0);
      }

      return null;
   }

   private static void sfx(ServerWorld level, Vec3d at, String key, float volume, float pitch) {
      SoundEvent e = BossSounds.event(key);
      if (e != null) {
         level.playSound(null, at.x, at.y, at.z, e, SoundCategory.PLAYERS, volume, pitch * (0.95F + level.getRandom().nextFloat() * 0.1F));
      }
   }

   private static final class Beguile {
      final PlayerEntity owner;
      final ServerWorld level;
      final long start;
      final long until;

      Beguile(PlayerEntity owner, ServerWorld level, long start, long until) {
         this.owner = owner;
         this.level = level;
         this.start = start;
         this.until = until;
      }
   }

   private record Bramble(PlayerEntity owner, ServerWorld level, Vec3d c, double r, long born, long until) {
   }

   private static final class Dart {
      final ServerWorld level;
      final PlayerEntity owner;
      final int kind;
      Vec3d pos;
      Vec3d vel;
      int age;
      final int life;
      int homingDelay;
      final double radius;
      double speed;
      double maxSpeed;
      double accel;
      double homing;
      @Nullable LivingEntity target;
      int pierce;
      final Set<LivingEntity> hit = new HashSet<>();
      float dmg;
      LiraelArsenal.@Nullable OnDart onHit;

      Dart(ServerWorld level, PlayerEntity owner, int kind, Vec3d pos, Vec3d vel, int life, double radius) {
         this.level = level;
         this.owner = owner;
         this.kind = kind;
         this.pos = pos;
         this.vel = vel;
         this.life = life;
         this.radius = radius;
         this.speed = vel.length();
         this.maxSpeed = this.speed;
      }
   }

   private record Echo(ServerWorld level, Vec3d at, long until) {
   }

   private static final class Glamour {
      final PlayerEntity p;
      final long start;
      final long until;

      Glamour(PlayerEntity p, long start, long until) {
         this.p = p;
         this.start = start;
         this.until = until;
      }
   }

   private static final class Glide {
      final ServerWorld level;
      final PlayerEntity p;
      final float pw;
      final boolean empowered;
      int age;
      boolean whirled;
      boolean done;

      Glide(ServerWorld level, PlayerEntity p, float pw, boolean empowered) {
         this.level = level;
         this.p = p;
         this.pw = pw;
         this.empowered = empowered;
      }
   }

   private static final class Heal {
      float owed;
      float given;
      int streams;
      boolean sounded;
   }

   private static final class Lunge {
      final ServerWorld level;
      final PlayerEntity p;
      final Vec3d dir;
      final int ticks;
      final double speed;
      final float pw;
      final boolean empowered;
      final boolean feint;
      final Vec3d start;
      final Set<LivingEntity> hit = new HashSet<>();
      Vec3d prev;
      int age;
      boolean done;

      Lunge(ServerWorld level, PlayerEntity p, Vec3d dir, int ticks, double speed, float pw, boolean empowered, boolean feint) {
         this.level = level;
         this.p = p;
         this.dir = dir;
         this.ticks = ticks;
         this.speed = speed;
         this.pw = pw;
         this.empowered = empowered;
         this.feint = feint;
         this.start = LiraelArsenal.chest(p);
         this.prev = this.start;
      }
   }

   @FunctionalInterface
   private interface OnDart {
      void hit(LiraelArsenal.Dart var1, LivingEntity var2, Vec3d var3);
   }

   private record Parry(LivingEntity attacker, long at) {
   }

   private record Task(long at, Runnable run) {
   }

   private static final class Tempest {
      final ServerWorld level;
      final PlayerEntity p;
      final Vec3d dir;
      final int lvl;
      final int life;
      final double r;
      final float pw;
      final float scale;
      final boolean empowered;
      final Map<LivingEntity, Integer> held = new HashMap<>();
      final LiraelArsenal.Heal heal = new LiraelArsenal.Heal();
      Vec3d pos;
      int age;
      boolean done;

      Tempest(ServerWorld level, PlayerEntity p, Vec3d pos, Vec3d dir, int lvl, float pw, boolean empowered) {
         this.level = level;
         this.p = p;
         this.pos = pos;
         this.dir = dir;
         this.lvl = lvl;
         this.life = 40 + 20 * lvl;
         this.r = 2.0 + 0.5 * lvl;
         this.pw = pw;
         this.scale = 0.5F + 0.5F * pw;
         this.empowered = empowered;
      }
   }

   private static final class Thorns {
      int stacks;
      long expires;
      long bloomUntil;
      long nextTear;
      @Nullable Vec3d last;
      @Nullable PlayerEntity owner;
      @Nullable ServerWorld level;
   }

   private static final class Volley {
      final Map<LivingEntity, Integer> thorns = new HashMap<>();
   }
}
