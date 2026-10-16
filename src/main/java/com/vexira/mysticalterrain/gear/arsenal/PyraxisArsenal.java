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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import java.util.function.IntConsumer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AllowDamage;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStopped;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.registry.tag.BlockTags;
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

public final class PyraxisArsenal extends Arsenal {
   private static final Vfx.Palette FIRE = Vfx.Palette.FIRE;
   private static final Vfx.Palette WHITE_HOT = new Vfx.Palette(16777204, 16762976, 2759188).withDebris(Vfx.Debris.ASH);
   private static final Vfx.Palette OBSIDIAN = new Vfx.Palette(14207216, 6965898, 1314840).withDebris(Vfx.Debris.ASH);
   private static final Vfx.Debris ASH = Vfx.Debris.ASH;
   private static final Vfx.Rune SOLAR = Vfx.Rune.SOLAR;
   private static final Vec3d UP = new Vec3d(0.0, 1.0, 0.0);
   private static final String MAUL = "emberlord_maul";
   private static final String FANG = "cinderfang";
   private static final String STAFF = "magmaheart_staff";
   private static final int MAX_HEAT = 5;
   private static final int POOL = 0;
   private static final int SEAM = 1;
   private static final int PATCH = 2;
   private static final int METEOR = 0;
   private static final int GREAT = 1;
   private static final int CINDER = 2;
   private static final int LEAP = 3;
   private static final String CRUST = "pyraxis.crust";
   private static final String SHELL = "pyraxis.shell";
   private static final String BACKDRAFT = "pyraxis.backdraft";
   private static final Map<LivingEntity, PyraxisArsenal.Heat> HEAT = new WeakHashMap<>();
   private static final List<PyraxisArsenal.Task> TASKS = new ArrayList<>();
   private static final List<PyraxisArsenal.Task> PENDING = new ArrayList<>();
   private static final List<PyraxisArsenal.Hazard> HAZARDS = new ArrayList<>();
   private static final List<PyraxisArsenal.Glob> GLOBS = new ArrayList<>();
   private static final List<PyraxisArsenal.Glob> NEW_GLOBS = new ArrayList<>();
   private static final List<PyraxisArsenal.Meteor> METEORS = new ArrayList<>();
   private static final List<PyraxisArsenal.Meteor> NEW_METEORS = new ArrayList<>();
   private static final List<PyraxisArsenal.Leap> LEAPS = new ArrayList<>();
   private static final List<PyraxisArsenal.Rush> RUSHES = new ArrayList<>();
   private static final Map<UUID, Long> SEAR_UNTIL = new HashMap<>();
   private static final Map<UUID, Long> UNBOUND_UNTIL = new HashMap<>();
   private static final Map<UUID, Long> NO_FALL = new HashMap<>();
   private static final Set<UUID> CRUSTED = new HashSet<>();
   private static boolean hooked;
   private static long clock;

   public PyraxisArsenal() {
      super("pyraxis", "molten_armor", Vfx.Palette.FIRE);
      if (!hooked) {
         hooked = true;
         ServerTickEvents.END_SERVER_TICK.register(PyraxisArsenal::tick);
         ServerLifecycleEvents.SERVER_STOPPED.register((ServerStopped)server -> reset());
         ServerLivingEntityEvents.ALLOW_DAMAGE
            .register(
               (AllowDamage)(entity, source, amount) -> !(
                  entity instanceof PlayerEntity p && source.isIn(DamageTypeTags.IS_FALL) && NO_FALL.getOrDefault(p.getUuid(), Long.MIN_VALUE) >= clock
               )
            );
      }
   }

   @Override
   protected void weapons() {
      this.weapon("emberlord_maul", 12.0F, -3.2F)
         .primary(Active.of(160, PyraxisArsenal::craterSlam).empowered(60, 1.3F))
         .alternate(Active.of(240, PyraxisArsenal::calderaLeap).empowered(40, 1.25F))
         .charge(Charge.of(3, 10).pose(UseAction.TRIDENT).perLevel(0.35F).onLevel(PyraxisArsenal::tectonicLevel))
         .passive(PyraxisArsenal::aftershock)
         .resonancePerHit(12.0F);
      this.weapon("cinderfang", 6.0F, -1.6F)
         .primary(Active.of(100, PyraxisArsenal::cinderRush).empowered(30, 1.25F))
         .alternate(Active.of(180, PyraxisArsenal::ashenStep).empowered(40, 1.25F))
         .charge(
            Charge.of(3, 8).pose(UseAction.SPEAR).cast(Active.of(280, PyraxisArsenal::flashpoint).empowered(50, 1.0F)).onLevel(PyraxisArsenal::flashpointLevel)
         )
         .passive(PyraxisArsenal::searingWounds)
         .resonancePerHit(8.0F);
      this.weapon("magmaheart_staff", 4.0F, -2.4F)
         .primary(Active.of(80, PyraxisArsenal::slagVolley).empowered(30, 1.2F))
         .alternate(Active.of(200, PyraxisArsenal::ventTheDeep).empowered(50, 1.25F))
         .charge(
            Charge.of(4, 10).pose(UseAction.TRIDENT).cast(Active.of(320, PyraxisArsenal::meteorCall).empowered(60, 1.25F)).onLevel(PyraxisArsenal::meteorLevel)
         )
         .passive(PyraxisArsenal::backdraft)
         .resonancePerHit(10.0F);
   }

   @Override
   protected void armor(ArmorKit set) {
      set.twoPiece(PyraxisArsenal::forgeborn);
      set.fourPiece(PyraxisArsenal::obsidianCrust);
      set.allowDamage(PyraxisArsenal::fireproof);
      set.onHurt(PyraxisArsenal::crustHurt);
      set.active(600, PyraxisArsenal::heartOfTheVolcano);
   }

   static boolean craterSlam(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      int c = u.charge();
      float pw = u.power();
      float ds = 0.5F + 0.5F * pw;
      double r = 3.5 + 0.75 * c;
      Vec3d at = ground(level, p.getEntityPos().add(GearKit.flatLook(p).multiply(2.2)));
      Vfx.slashArc(level, p.getEntityPos().add(0.0, 1.5, 0.0), p.getYaw(), 15.0F, 90.0F, 3.2, FIRE, 160.0F);
      List<LivingEntity> hits = foes(level, p, at, r).stream().filter(e -> Math.abs(e.getY() - at.y) < 2.5).toList();
      Set<LivingEntity> struck = new HashSet<>(hits);
      int best = 0;

      for (LivingEntity e : hits) {
         hit(level, p, e, 18.0F * pw, true);
         best = Math.max(best, detonate(level, p, e, ds, struck));
         heat(level, p, e, 1);
         shove(e, at, 0.5, 0.6);
         u.resonate(3.0F);
      }

      boomSound(level, at, best);
      Vfx.heavyImpact(level, at, FIRE, 1.0 + 0.3 * c);
      Vfx.groundCrack(level, at, r, FIRE);
      Vfx.shockwave(level, at, FIRE, r);
      int spurts = 6 + 2 * c;
      double phase = level.getRandom().nextDouble() * Math.PI * 2.0;

      for (int i = 0; i < spurts; i++) {
         double a = phase + i * Math.PI * 2.0 / spurts;
         Vfx.pillar(level, ground(level, at.add(Math.cos(a) * r, 0.0, Math.sin(a) * r)), FIRE, 1.4 + 0.4 * c, 0.35);
      }

      Vfx.debris(level, at.add(0.0, 1.0, 0.0), ASH, FIRE.smoke(), 10 + 4 * c, 0.4);
      GearKit.kick(level, p, 0.4 + 0.12 * c);
      pool(level, p, at, 2.5 + 0.5 * c, 100, true);
      if (c >= 2) {
         fissures(level, p, at, c, pw);
      }

      if (u.empowered()) {
         hellfireRing(level, p, at, r, pw);
      }

      return true;
   }

   private static void fissures(ServerWorld level, PlayerEntity p, Vec3d at, int c, float pw) {
      Vec3d look = GearKit.flatLook(p);
      List<Vec3d> dirs = new ArrayList<>();
      if (c >= 3) {
         for (float a : new float[]{-25.0F, 0.0F, 25.0F}) {
            dirs.add(look.rotateY(a * (float) (Math.PI / 180.0)));
         }
      } else {
         dirs.add(look);
      }

      for (Vec3d d : dirs) {
         Vfx.telegraphLine(level, at, d, 9.0, 1.4, 8, FIRE);
      }

      later(level, p, 8, () -> {
         Set<LivingEntity> done = new HashSet<>();

         for (Vec3d d : dirs) {
            Vec3d end = ground(level, at.add(d.multiply(9.0)));

            for (LivingEntity e : GearKit.enemies(level, p, at.add(d.multiply(4.5)), 9.0)) {
               if (!done.contains(e) && onSegment(e, at, end, 1.4, 2.0)) {
                  done.add(e);
                  hit(level, p, e, 12.0F * pw, false);
                  heat(level, p, e, 2);
                  launch(e, 0.5);
               }
            }

            for (double s = 0.0; s <= 9.01; s += 2.5) {
               Vfx.groundCrack(level, ground(level, at.add(d.multiply(s))), 1.4, FIRE);
            }

            for (int k = 3; k <= 9; k += 3) {
               Vfx.pillar(level, ground(level, at.add(d.multiply(k))), FIRE, 2.5, 0.5);
            }

            Vfx.streaks(level, at.add(0.0, 0.3, 0.0), d, FIRE, 8);
            seam(level, p, at, end, 0.9, 80, true);
         }

         sfx(level, at, "weapon.emberlord_maul.erupt", 1.2F, 0.9F);
      });
   }

   private static void hellfireRing(ServerWorld level, PlayerEntity p, Vec3d at, double r0, float pw) {
      Set<LivingEntity> burned = new HashSet<>();
      later(level, p, 4, () -> {
         Vfx.telegraphCircle(level, at, 8.0, 10, FIRE);
         sfx(level, at, "weapon.emberlord_maul.erupt", 1.0F, 1.1F);
      });
      repeat(level, p, 5, 1, 10, k -> {
         double radius = r0 + (8.0 - r0) * (k + 1) / 10.0;

         for (LivingEntity e : GearKit.enemies(level, p, at, radius + 3.0)) {
            double d = flatDist(e.getEntityPos(), at);
            double w = e.getWidth() * 0.5;
            if (!burned.contains(e) && !(d < radius - 0.8 - w) && !(d > radius + 0.4 + w) && !(Math.abs(e.getY() - at.y) >= 1.2)) {
               burned.add(e);
               hit(level, p, e, 10.0F * pw, false);
               heat(level, p, e, 2);
               shove(e, at, 0.5, 0.35);
            }
         }

         if (k % 2 == 1) {
            double phase = level.getRandom().nextDouble() * Math.PI * 2.0;

            for (int i = 0; i < 6; i++) {
               double a = phase + i * Math.PI / 3.0;
               Vfx.embers(level, at.add(Math.cos(a) * radius, 0.3, Math.sin(a) * radius), FIRE.core(), 3, 0.3);
            }

            Vfx.ring(level, at.add(0.0, 0.2, 0.0), FIRE.glow(), radius, 4);
         }

         if (k == 9) {
            Vfx.shockwave(level, at, FIRE, 8.0);
         }
      });
   }

   static void tectonicLevel(ServerWorld level, PlayerEntity p, WeaponKit w, int lvl, boolean max) {
      Vec3d feet = ground(level, p.getEntityPos());
      Vfx.groundCrack(level, feet, 1.0 + 0.8 * lvl, FIRE);
      Vfx.flare(level, GearKit.tip(p), max ? FIRE.core() : FIRE.glow(), 0.9 + 0.4 * lvl, 6);
      Vfx.embers(level, feet.add(0.0, 0.3, 0.0), FIRE.core(), 4 + 3 * lvl, 0.8 + 0.4 * lvl);
      Vfx.shake(level, feet, 6.0, 0.1 * lvl, 6);
      if (max) {
         Vfx.runeCircle(level, feet, 2.2, FIRE, 40, 0.06F, SOLAR);
         Vfx.chargeUp(level, GearKit.tip(p), WHITE_HOT, 12);
         Vfx.pillar(level, feet, FIRE, 2.2, 0.4);
      }
   }

   static boolean calderaLeap(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      Vec3d from = p.getEntityPos();
      Vec3d lp = GearKit.lookPoint(level, p, 14.0);
      Vec3d off = new Vec3d(lp.x - from.x, 0.0, lp.z - from.z);
      Vec3d dir;
      double dist;
      if (off.lengthSquared() < 0.25) {
         dir = GearKit.flatLook(p);
         dist = 8.0;
      } else {
         dir = off.normalize();
         dist = MathHelper.clamp(off.length(), 3.0, 14.0);
      }

      Vec3d target = ground(level, new Vec3d(from.x + dir.x * dist, Math.max(from.y, lp.y), from.z + dir.z * dist));
      double h = Math.min(1.5, dist / 10.2);
      p.setVelocity(dir.x * h, 1.0, dir.z * h);
      p.knockedBack = true;
      p.onLanding();
      noFall(p, 60);
      LEAPS.removeIf(l -> l.p == p);
      LEAPS.add(new PyraxisArsenal.Leap(p, level, u));
      Vec3d feet = ground(level, from);
      Vfx.telegraphCircle(level, target, 4.0, 20, FIRE);
      Vfx.explosion(level, feet.add(0.0, 0.2, 0.0), FIRE, 1.2);
      Vfx.streaks(level, feet, UP, FIRE, 8);
      Vfx.groundCrack(level, feet, 1.6, FIRE);
      return true;
   }

   private static boolean stepLeap(PyraxisArsenal.Leap l) {
      PlayerEntity p = l.p;
      if (!alive(p, l.level)) {
         return true;
      }

      l.age++;
      p.onLanding();
      noFall(p, 20);
      Vfx.meteorTrail(l.level, p.getEntityPos().add(0.0, 0.9, 0.0), p.getMovement(), FIRE);
      if ((l.age < 4 || !p.isOnGround() && !p.isTouchingWater() && !p.isInLava()) && l.age < 40) {
         return false;
      }

      land(l);
      return true;
   }

   private static void land(PyraxisArsenal.Leap l) {
      PlayerEntity p = l.p;
      ServerWorld level = l.level;
      Vec3d at = ground(level, p.getEntityPos());

      for (LivingEntity e : foes(level, p, at, 4.0)) {
         hit(level, p, e, 16.0F * l.pw, true);
         heat(level, p, e, 2);
         shove(e, at, 0.3, 0.7);
         l.u.resonate(3.0F);
      }

      Vfx.heavyImpact(level, at, FIRE, 1.3);
      Vfx.groundCrack(level, at, 4.0, FIRE);
      Vfx.shockwave(level, at, FIRE, 4.5);
      GearKit.kick(level, p, 0.5);
      pool(level, p, at, 3.0, 100, true);
      sfx(level, at, "weapon.emberlord_maul.land", 1.4F, 0.9F);
      if (l.empowered) {
         Random rnd = level.getRandom();
         Vec3d f = GearKit.flatLook(p);
         double base = Math.atan2(f.z, f.x);

         for (int i = 0; i < 3; i++) {
            double a = base + i * Math.PI * 2.0 / 3.0;
            double d = 3.5 + rnd.nextDouble() * 1.5;
            Vec3d m = ground(level, at.add(Math.cos(a) * d, 0.0, Math.sin(a) * d));
            meteor(level, p, m.add(rnd.nextBoolean() ? 3.0 : -3.0, 16.0, rnd.nextBoolean() ? 3.0 : -3.0), m, 12, 2.5, 12.0F * l.pw, 3);
         }
      }
   }

   static void aftershock(ServerWorld level, LivingEntity a, LivingEntity t, ItemStack s) {
      if (a instanceof PlayerEntity p) {
         heat(level, p, t, 1);
         GearKit.hitSpark(level, t, FIRE);
         Vfx.embers(level, center(t), FIRE.core(), 4, 0.3);
         PyraxisArsenal.Heat h = HEAT.get(t);
         if (h != null) {
            if (!p.getUuid().equals(h.maulBy)) {
               h.maulBy = p.getUuid();
               h.maulHits = 0;
            }

            if (++h.maulHits >= 3) {
               h.maulHits = 0;
               Vec3d feet = ground(level, t.getEntityPos());
               List<LivingEntity> near = new ArrayList<>(foes(level, p, feet, 2.0));
               if (!near.contains(t)) {
                  near.add(t);
               }

               for (LivingEntity e : near) {
                  hit(level, p, e, 6.0F, false);
                  heat(level, p, e, 1);
               }

               Vfx.pillar(level, feet, FIRE, 2.4, 0.6);
               Vfx.groundCrack(level, feet, 1.8, FIRE);
               Vfx.impact(level, feet, FIRE, 0.8);
               GearKit.procSound(level, t, "emberlord_maul");
            }
         }
      }
   }

   static boolean cinderRush(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      Vec3d dir = GearKit.flatLook(p);
      Vec3d start = p.getEntityPos();
      RUSHES.removeIf(r -> r.p == p);
      RUSHES.add(new PyraxisArsenal.Rush(p, level, u, start, dir));
      p.setVelocity(dir.x * 2.0, Math.max(p.getMovement().y, 0.05), dir.z * 2.0);
      p.knockedBack = true;
      Vfx.streaks(level, start.add(0.0, 1.0, 0.0), dir, FIRE, 10);
      return true;
   }

   private static boolean stepRush(PyraxisArsenal.Rush r) {
      PlayerEntity p = r.p;
      ServerWorld level = r.level;
      if (!alive(p, level)) {
         return true;
      }

      r.age++;

      for (LivingEntity e : foes(level, p, center(p), 1.8)) {
         if (r.cut.add(e)) {
            hit(level, p, e, 9.0F * r.pw, true);
            heat(level, p, e, 2);
            Vfx.slashArc(level, center(e), yawTo(r.start, center(e)), 0.0F, r.cut.size() % 2 == 0 ? 35.0F : -35.0F, 1.8, FIRE, 120.0F);
            GearKit.hitSpark(level, e, FIRE);
            r.u.resonate(2.0F);
         }
      }

      Vfx.embers(level, p.getEntityPos().add(0.0, 0.5, 0.0), FIRE.core(), 3, 0.3);
      if (r.age < 7 && !blocked(level, p, r.dir)) {
         p.setVelocity(r.dir.x * 2.0, Math.max(p.getMovement().y, 0.05), r.dir.z * 2.0);
         p.knockedBack = true;
         return false;
      } else {
         endRush(r);
         return true;
      }
   }

   private static void endRush(PyraxisArsenal.Rush r) {
      PlayerEntity p = r.p;
      ServerWorld level = r.level;
      Vec3d end = p.getEntityPos();
      p.setVelocity(r.dir.x * 0.3, p.getMovement().y, r.dir.z * 0.3);
      p.knockedBack = true;
      Vfx.dashTrail(level, r.start.add(0.0, 1.0, 0.0), end.add(0.0, 1.0, 0.0), FIRE);
      if (flatDist(r.start, end) > 1.0) {
         seam(level, p, ground(level, r.start), ground(level, end), 0.8, 60, false);
      }

      if (r.empowered) {
         later(level, p, 5, () -> returnStrike(level, p, r));
      }
   }

   private static void returnStrike(ServerWorld level, PlayerEntity p, PyraxisArsenal.Rush r) {
      Vec3d end = p.getEntityPos();
      Vec3d back = r.start.subtract(end);
      double len = back.length();
      if (!(len < 1.5)) {
         boolean clear = level.raycast(new RaycastContext(end.add(0.0, 1.0, 0.0), r.start.add(0.0, 1.0, 0.0), ShapeType.COLLIDER, FluidHandling.NONE, p))
                  .getType()
               == Type.MISS
            && level.isSpaceEmpty(p, p.getBoundingBox().offset(back));
         if (clear) {
            p.requestTeleport(r.start.x, r.start.y, r.start.z);
            p.onLanding();
            Vfx.dashTrail(level, end.add(0.0, 1.0, 0.0), r.start.add(0.0, 1.0, 0.0), WHITE_HOT);
            Vec3d d = back.normalize();
            float yaw = yawTo(end, r.start);
            int n = 0;

            for (LivingEntity e : GearKit.alongLine(level, p, end.add(0.0, 0.9, 0.0), d, len, 1.6)) {
               hit(level, p, e, 9.0F * r.pw, true);
               heat(level, p, e, 2);
               Vfx.slashArc(level, center(e), yaw, 0.0F, n++ % 2 == 0 ? 60.0F : -60.0F, 1.8, WHITE_HOT, 140.0F);
               GearKit.hitSpark(level, e, FIRE);
               r.u.resonate(2.0F);
            }

            sfx(level, r.start, "weapon.cinderfang.use", 0.9F, 1.3F);
         }
      }
   }

   static boolean ashenStep(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      float pw = u.power();
      boolean emp = u.empowered();
      double r = emp ? 4.0 : 3.0;
      Vec3d effigy = p.getEntityPos();
      Vec3d back = GearKit.flatLook(p).multiply(-1.0);
      p.setVelocity(back.x * 1.3, 0.45, back.z * 1.3);
      p.knockedBack = true;
      p.onLanding();
      noFall(p, 40);
      Vec3d body = effigy.add(0.0, 1.0, 0.0);
      Vec3d g = ground(level, effigy);
      Vfx.flare(level, body, FIRE.glow(), 2.4, 16);
      Vfx.wisps(level, body, FIRE, 6, 0.6, 16);
      Vfx.telegraphCircle(level, g, r, 16, FIRE);
      Vfx.dashTrail(level, body, clipTo(level, p, body, back, 6.0), FIRE);
      SEAR_UNTIL.put(p.getUuid(), clock + 60L);
      if (emp) {
         Vfx.vortex(level, effigy, 3.0, 2.5, FIRE, 16);
         repeat(level, p, 0, 2, 9, i -> {
            for (LivingEntity e : GearKit.enemies(level, p, effigy, 6.0)) {
               if (!isBoss(e)) {
                  pull(e, effigy, 0.22, 0.02);
               }
            }
         });
      }

      later(level, p, 16, () -> {
         for (LivingEntity e : foes(level, p, effigy.add(0.0, 0.8, 0.0), r)) {
            hit(level, p, e, 14.0F * pw, false);
            heat(level, p, e, 2);
            shove(e, effigy, 0.9, 0.35);
            u.resonate(2.0F);
         }

         Vfx.explosion(level, effigy.add(0.0, 0.8, 0.0), FIRE, r);
         Vfx.groundCrack(level, g, r - 0.5, FIRE);
         Vfx.debris(level, body, ASH, FIRE.smoke(), 12, 0.45);
         sfx(level, effigy, "weapon.cinderfang.effigy", 1.2F, 1.0F);
         if (emp) {
            pool(level, p, g, 2.5, 80, true);
         }
      });
      return true;
   }

   static void flashpointLevel(ServerWorld level, PlayerEntity p, WeaponKit w, int lvl, boolean max) {
      Vec3d at = GearKit.tip(p);
      Vfx.flare(level, at, max ? WHITE_HOT.core() : FIRE.glow(), 0.8 + 0.35 * lvl, 6);
      Vfx.sparks(level, at, FIRE.core(), 3 + 2 * lvl, 0.18, 0.14);
      Vfx.ring(level, p.getEntityPos(), FIRE.glow(), Math.max(0.6, 2.4 - 0.5 * lvl), 8);
      if (max) {
         Vfx.gather(level, at, FIRE, 1.2, 10);
         Vfx.glint(level, at, WHITE_HOT.core(), 1.0);
      }
   }

   static boolean flashpoint(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      float k = 1.0F + 0.25F * u.charge();
      LivingEntity t = brandTarget(level, p, 10 + 4 * u.charge());
      if (t != null && blink(level, p, t)) {
         if (u.fullyCharged() && stacks(t) < 3) {
            heat(level, p, t, 3 - stacks(t));
         }

         Vec3d c = center(t);
         hit(level, p, t, 10.0F * k, true);
         int n = detonate(level, p, t, k, null);
         u.resonate(5.0F);
         xCut(level, p, c, 1.0);
         Vfx.glint(level, c, WHITE_HOT.core(), 1.4);
         if (u.fullyCharged() && n == 5) {
            Vfx.impactFrame(level, c, 10.0, WHITE_HOT.core(), 0.6F);
         }

         boomSound(level, c, n);
         if (u.empowered()) {
            Set<LivingEntity> chained = new HashSet<>();
            chained.add(t);
            Vec3d[] last = new Vec3d[]{c};
            repeat(level, p, 5, 5, 2, i -> {
               LivingEntity next = null;
               double best = 64.0;

               for (LivingEntity e : GearKit.enemies(level, p, last[0], 10.0)) {
                  double d = center(e).squaredDistanceTo(last[0]);
                  if (!chained.contains(e) && stacks(e) >= 1 && d <= best) {
                     best = d;
                     next = e;
                  }
               }

               if (next != null && blink(level, p, next)) {
                  chained.add(next);
                  Vec3d nc = center(next);
                  last[0] = nc;
                  hit(level, p, next, 7.0F * k, true);
                  int m = detonate(level, p, next, 0.7F * k, null);
                  xCut(level, p, nc, 0.8);
                  boomSound(level, nc, m);
                  u.resonate(2.0F);
               }
            });
         }

         return true;
      } else {
         return false;
      }
   }

   private static void xCut(ServerWorld level, PlayerEntity p, Vec3d at, double size) {
      float yaw = yawTo(p.getEntityPos(), at);
      Vfx.slashArc(level, at, yaw, 0.0F, 60.0F, 2.2 * size, FIRE, 200.0F);
      Vfx.slashArc(level, at, yaw, 0.0F, -60.0F, 2.2 * size, FIRE, -200.0F);
   }

   static void searingWounds(ServerWorld level, LivingEntity a, LivingEntity t, ItemStack s) {
      if (a instanceof PlayerEntity p) {
         boolean crit = p.fallDistance > 0.0 && !p.isOnGround() && !p.isClimbing() && !p.isTouchingWater();
         heat(level, p, t, crit ? 2 : 1);
         if (crit) {
            GearState st = GearState.of(p);
            String key = "cinderfang.primary";
            int left = st.remaining(key);
            if (left > 0) {
               st.cooldown(key, Math.max(1, left - 20));
            }

            Vfx.glint(level, center(t), FIRE.core(), 0.9);
            GearKit.procSound(level, t, "cinderfang");
         }

         Long sear = SEAR_UNTIL.get(p.getUuid());
         if (sear != null && sear >= clock) {
            SEAR_UNTIL.remove(p.getUuid());
            hit(level, p, t, 6.0F, false);
            heat(level, p, t, 2);
            Vfx.slashArc(level, center(t), p.getYaw(), 0.0F, -40.0F, 1.6, WHITE_HOT, 140.0F);
            GearKit.procSound(level, t, "cinderfang");
         }
      }
   }

   static boolean slagVolley(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      float pw = u.power();
      boolean emp = u.empowered();
      float[] yaws = emp ? new float[]{-14.0F, -7.0F, 0.0F, 7.0F, 14.0F} : new float[]{-8.0F, 0.0F, 8.0F};
      Vec3d look = p.getRotationVector();
      Vec3d from = GearKit.eye(p).add(look.multiply(0.8));

      for (float y : yaws) {
         Vec3d v = look.rotateY(-y * (float) (Math.PI / 180.0)).multiply(1.25).add(0.0, 0.12, 0.0);
         glob(level, p, from, v, 8.0F * pw, emp ? 2 : 1, emp ? 2.8 : 2.2, true);
      }

      GearKit.muzzle(level, p, FIRE, 1.2);
      Vfx.smoke(level, GearKit.tip(p), FIRE.smoke(), 3, 0.3, 1.0);
      return true;
   }

   static boolean ventTheDeep(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      float pw = u.power();
      boolean emp = u.empowered();
      Vec3d me = p.getEntityPos();
      List<PyraxisArsenal.Vent> pts = new ArrayList<>();

      for (PyraxisArsenal.Hazard h : ownHazards(p, 24.0)) {
         if (h.kind() == 1) {
            for (int i = 0; i < 3; i++) {
               pts.add(new PyraxisArsenal.Vent(h.a().lerp(h.b(), i / 2.0), 1.8, h));
            }
         } else {
            pts.add(new PyraxisArsenal.Vent(h.a(), h.r() + 0.5, h));
         }
      }

      pts.sort(Comparator.comparingDouble(vx -> vx.at().squaredDistanceTo(me)));
      if (pts.size() > 8) {
         pts = new ArrayList<>(pts.subList(0, 8));
      }

      if (pts.isEmpty()) {
         Vec3d at = aim(level, p, 24.0);
         if (at == null) {
            return false;
         }

         Vfx.telegraphCircle(level, at, 2.5, 14, FIRE);
         Vfx.groundCrack(level, at, 2.2, FIRE);
         later(level, p, 14, () -> {
            eruption(level, p, at, 2.5, 18.0F * pw, 8.0, false, emp, pw);
            sfx(level, at, "weapon.emberlord_maul.erupt", 1.0F, 0.8F);
         });
         return true;
      } else {
         Set<PyraxisArsenal.Hazard> used = new HashSet<>();

         for (PyraxisArsenal.Vent v : pts) {
            used.add(v.from());
         }

         HAZARDS.removeIf(used::contains);
         Vfx.runeCircle(level, ground(level, me), 2.0, FIRE, 20, -0.06F, SOLAR);
         Vfx.beam(level, GearKit.tip(p), pts.get(0).at().add(0.0, 1.0, 0.0), FIRE, 0.25, 6);

         for (int i = 0; i < pts.size(); i++) {
            PyraxisArsenal.Vent v = pts.get(i);
            boolean loud = i < 3;
            soon(level, p, 2 * i, () -> eruption(level, p, v.at(), v.r(), 14.0F * pw, 6.0, loud, emp, pw));
         }

         return true;
      }
   }

   private static void eruption(ServerWorld level, PlayerEntity p, Vec3d at, double r, float dmg, double height, boolean loud, boolean emp, float pw) {
      for (LivingEntity e : foes(level, p, at, r)) {
         if (!(Math.abs(e.getY() - at.y) > 3.0)) {
            hit(level, p, e, dmg, false);
            heat(level, p, e, 2);
            launch(e, 0.9);
            resonate(p, "magmaheart_staff", 2.0F);
         }
      }

      Vfx.pillar(level, at, FIRE, height, 1.0 + 0.2 * r);
      Vfx.explosion(level, at.add(0.0, 0.5, 0.0), FIRE, r);
      Vfx.groundCrack(level, at, r, FIRE);
      Vfx.debris(level, at.add(0.0, 1.0, 0.0), ASH, FIRE.smoke(), 8, 0.5);
      if (loud) {
         sfx(level, at, "weapon.magmaheart_staff.erupt", 1.3F, 1.0F);
      }

      if (emp) {
         List<LivingEntity> near = new ArrayList<>(GearKit.enemies(level, p, at, 10.0));
         near.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(at)));

         for (int i = 0; i < 2 && !near.isEmpty(); i++) {
            LivingEntity t = near.get(Math.min(i, near.size() - 1));
            Vec3d from = at.add(0.0, 1.2, 0.0);
            Vec3d d = center(t).subtract(from);
            Vec3d v = (d.lengthSquared() < 1.0E-4 ? UP : d.normalize()).multiply(0.9).add(0.0, 0.25, 0.0);
            PyraxisArsenal.Glob g = glob(level, p, from, v, 6.0F * pw, 1, 1.6, false);
            g.gravity = 0.02;
            g.life = 40;
            g.homing = t;
            g.homingStrength = 0.12;
         }
      }
   }

   static void meteorLevel(ServerWorld level, PlayerEntity p, WeaponKit w, int lvl, boolean max) {
      Vfx.flare(level, GearKit.tip(p), FIRE.glow(), 0.9 + 0.3 * lvl, 6);
      Vec3d aim = aim(level, p, 32.0);
      if (aim != null) {
         Vfx.runeCircle(level, aim, 1.2 + 0.6 * lvl, FIRE, 12, 0.08F, SOLAR);
         Vfx.flare(level, aim.add(0.0, 14.0, 0.0), FIRE.core(), 1.0 + 0.5 * lvl, 10);
         if (max) {
            Vfx.chargeUp(level, aim.add(0.0, 14.0, 0.0), WHITE_HOT, 20);
         }
      }
   }

   static boolean meteorCall(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      Vec3d aim = aim(level, p, 32.0);
      if (aim == null) {
         return false;
      }

      float pw = u.power();
      boolean full = u.fullyCharged();
      int c = Math.max(1, u.charge());
      Random rnd = level.getRandom();
      double phase = rnd.nextDouble() * Math.PI * 2.0;
      int ring = full ? 4 : c;

      for (int i = 0; i < ring; i++) {
         double a = phase + i * Math.PI * 2.0 / ring + (rnd.nextDouble() - 0.5) * 0.5;
         double d = 2.0 + rnd.nextDouble() * 2.0;
         Vec3d m = ground(level, aim.add(Math.cos(a) * d, 0.0, Math.sin(a) * d));
         Vec3d from = m.add(rnd.nextBoolean() ? 4.0 : -4.0, 22.0, rnd.nextBoolean() ? 4.0 : -4.0);
         soon(level, p, full ? 5 * i : 5 * (i + 1), () -> meteor(level, p, from, m, 18, 3.0, 14.0F * pw, 0));
      }

      if (full) {
         later(level, p, 15, () -> meteor(level, p, aim.add(0.0, 32.0, 0.0), aim, 24, 5.0, 28.0F * pw, 1));
      } else {
         meteor(level, p, aim.add(rnd.nextBoolean() ? 4.0 : -4.0, 22.0, rnd.nextBoolean() ? 4.0 : -4.0), aim, 18, 3.0, 14.0F * pw, 0);
      }

      GearKit.muzzle(level, p, FIRE, 1.4);
      Vfx.beam(level, GearKit.tip(p), GearKit.tip(p).add(0.0, 12.0, 0.0), FIRE, 0.2, 8);
      if (u.empowered()) {
         int last = full ? 39 : 5 * c + 18;
         repeat(level, p, last, 10, 4, i -> {
            Vec3d mx = cinderSpot(level, p, aim);
            meteor(level, p, mx.add(rnd.nextBoolean() ? 3.0 : -3.0, 18.0, rnd.nextBoolean() ? 3.0 : -3.0), mx, 16, 2.0, 10.0F * pw, 2);
         });
      }

      return true;
   }

   private static Vec3d cinderSpot(ServerWorld level, PlayerEntity p, Vec3d aim) {
      Random rnd = level.getRandom();
      List<LivingEntity> near = GearKit.enemies(level, p, aim, 10.0);
      if (!near.isEmpty()) {
         LivingEntity e = near.get(rnd.nextInt(near.size()));
         Vec3d v = e.getVelocity();
         return ground(level, e.getEntityPos().add(v.x * 16.0, 0.0, v.z * 16.0));
      } else {
         double a = rnd.nextDouble() * Math.PI * 2.0;
         double d = 2.0 + rnd.nextDouble() * 3.0;
         return ground(level, aim.add(Math.cos(a) * d, 0.0, Math.sin(a) * d));
      }
   }

   static void backdraft(ServerWorld level, LivingEntity a, LivingEntity t, ItemStack s) {
      if (a instanceof PlayerEntity p) {
         heat(level, p, t, 1);
         hit(level, p, t, 3.0F, false);
         shove(t, a.getEntityPos(), 0.9, 0.25);
         Vec3d c = center(t);
         Vec3d away = flat(c.subtract(a.getEntityPos()));
         Vfx.streaks(level, c, away.lengthSquared() < 1.0E-4 ? GearKit.flatLook(p) : away.normalize(), FIRE, 5);
         Vfx.flare(level, c, FIRE.glow(), 1.3, 5);
         GearState st = GearState.of(p);
         if (st.ready("pyraxis.backdraft")) {
            st.cooldown("pyraxis.backdraft", 10);
            GearKit.procSound(level, t, "magmaheart_staff");
         }
      }
   }

   private static void forgeborn(ServerWorld level, PlayerEntity p, int pieces) {
      if (level.getTime() % 20L == 0L) {
         GearKit.keep(p, StatusEffects.FIRE_RESISTANCE, 0);
         boolean hot = p.isInLava() || p.getFireTicks() > 0 || level.getBlockState(p.getBlockPos()).isIn(BlockTags.FIRE) || inOwnMagma(p);
         if (hot && !(p.getHealth() >= p.getMaxHealth())) {
            p.heal(1.0F);
            if (level.getTime() % 40L == 0L) {
               Vfx.auraRing(level, p.getEntityPos(), FIRE, 0.9);
               Vfx.embers(level, chest(p), FIRE.core(), 4, 0.4);
            }
         }
      }
   }

   private static boolean fireproof(ServerWorld level, PlayerEntity p, DamageSource source, float amount, int pieces) {
      return pieces < 2 || !source.isIn(DamageTypeTags.IS_FIRE);
   }

   private static void obsidianCrust(ServerWorld level, PlayerEntity p, int pieces) {
      UUID id = p.getUuid();
      long t = level.getTime();
      if (t % 20L == 0L) {
         if (CRUSTED.contains(id) && !p.hasStatusEffect(StatusEffects.ABSORPTION)) {
            CRUSTED.remove(id);
         }

         if (!CRUSTED.contains(id) && GearState.of(p).ready("pyraxis.crust") && !p.hasStatusEffect(StatusEffects.ABSORPTION)) {
            regrow(level, p);
         }
      }

      if (t % 40L == 0L && CRUSTED.contains(id)) {
         Random rnd = level.getRandom();
         Vfx.glint(level, chest(p).add((rnd.nextDouble() - 0.5) * 0.8, (rnd.nextDouble() - 0.5) * 0.8, (rnd.nextDouble() - 0.5) * 0.8), OBSIDIAN.core(), 0.35);
      }

      Long unbound = UNBOUND_UNTIL.get(id);
      if (unbound != null && unbound >= clock) {
         if (clock % 2L == 0L) {
            Vfx.trail(level, p.getEntityPos().add(0.0, 0.2, 0.0), FIRE, 0.5);
         }

         if (clock % 10L == 0L) {
            for (LivingEntity e : GearKit.enemies(level, p, p.getEntityPos(), 2.5)) {
               heat(level, p, e, 1, 3);
            }

            Vfx.embers(level, chest(p), FIRE.core(), 4, 0.6);
         }
      }
   }

   private static void regrow(ServerWorld level, PlayerEntity p) {
      UUID id = p.getUuid();
      if (CRUSTED.contains(id) || !p.hasStatusEffect(StatusEffects.ABSORPTION) || !(p.getAbsorptionAmount() > 0.0F)) {
         p.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 2400, 1, true, false, true));
         CRUSTED.add(id);
         Vfx.shieldBubble(level, p.getEntityPos().add(0.0, 1.0, 0.0), 1.2, OBSIDIAN, 14);
         Vfx.shards(level, chest(p), OBSIDIAN.core(), 8, 0.2);
         sfx(level, p.getEntityPos(), "weapon.emberlord_maul.crust", 0.5F, 1.4F);
      }
   }

   private static void crustHurt(ServerWorld level, PlayerEntity p, @Nullable LivingEntity attacker, DamageSource source, float taken, int pieces) {
      UUID id = p.getUuid();
      if (pieces >= 4 && CRUSTED.contains(id)) {
         GearState st = GearState.of(p);
         if (attacker != null && attacker.isAlive() && source.getSource() == attacker && st.ready("pyraxis.shell")) {
            st.cooldown("pyraxis.shell", 10);
            heat(level, p, attacker, 2);
            burn(level, p, attacker, 3.0F);
            Vec3d c = chest(p);
            Vec3d to = center(attacker).subtract(c);
            Vfx.streaks(level, c, to.lengthSquared() < 1.0E-4 ? GearKit.flatLook(p) : to.normalize(), FIRE, 5);
            GearKit.proc(level, attacker, FIRE);
            sfx(level, attacker.getEntityPos(), "weapon.cinderfang.proc", 0.7F, 1.2F);
         }

         if (p.getAbsorptionAmount() <= 0.0F) {
            crustBreak(level, p);
         }
      }
   }

   private static void crustBreak(ServerWorld level, PlayerEntity p) {
      UUID id = p.getUuid();
      CRUSTED.remove(id);
      GearState.of(p).cooldown("pyraxis.crust", 300);
      Vec3d c = chest(p);

      for (LivingEntity e : foes(level, p, p.getEntityPos(), 4.0)) {
         hit(level, p, e, 10.0F, false);
         heat(level, p, e, 2);
         shove(e, p.getEntityPos(), 0.9, 0.35);
      }

      Vfx.shards(level, c, OBSIDIAN.core(), 24, 0.8);
      Vfx.debris(level, c, ASH, FIRE.smoke(), 14, 0.5);
      Vfx.explosion(level, c, FIRE, 3.0);
      Vfx.nova(level, c, FIRE, 4.0);
      sfx(level, p.getEntityPos(), "weapon.emberlord_maul.crust", 1.2F, 0.9F);
      UNBOUND_UNTIL.put(id, clock + 120L);
      p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 120, 0, true, false, true));
      p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 120, 0, true, false, true));
   }

   static boolean heartOfTheVolcano(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      Vec3d feet = ground(level, p.getEntityPos());
      p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 22, 2, true, false, true));
      p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 22, 3, true, false, true));
      Vfx.runeCircle(level, feet, 7.0, FIRE, 40, 0.08F, SOLAR);
      Vfx.telegraphCircle(level, feet, 7.0, 20, FIRE);
      Vfx.chargeUp(level, chest(p), WHITE_HOT, 20);
      Vfx.vortex(level, feet, 7.0, 4.0, FIRE, 20);
      Vfx.gather(level, chest(p), FIRE, 6.0, 16);
      repeat(level, p, 0, 2, 10, i -> {
         Vec3d c = p.getEntityPos();

         for (LivingEntity e : GearKit.enemies(level, p, c, 10.0)) {
            if (!isBoss(e)) {
               pull(e, c, 0.18, 0.02);
            }
         }
      });
      later(level, p, 20, () -> supernova(level, p));
      return true;
   }

   private static void supernova(ServerWorld level, PlayerEntity p) {
      Vec3d feet = ground(level, p.getEntityPos());
      Vec3d c = chest(p);
      List<LivingEntity> hits = foes(level, p, feet, 7.0);
      Set<LivingEntity> all = new HashSet<>(hits);
      int best = 0;

      for (LivingEntity e : hits) {
         best = Math.max(best, detonate(level, p, e, 1.0F, all));
         hit(level, p, e, 22.0F, false);
         heat(level, p, e, 3);
         shove(e, feet, 1.1, 0.5);
      }

      Vfx.nova(level, c, FIRE, 7.0);
      Vfx.heavyImpact(level, feet, WHITE_HOT, 2.0);
      Vfx.groundCrack(level, feet, 7.0, FIRE);
      Vfx.pillar(level, feet, FIRE, 9.0, 1.4);

      for (int k = 0; k < 8; k++) {
         double a = k * Math.PI / 4.0;
         Vfx.streaks(level, c, new Vec3d(Math.cos(a), 0.0, Math.sin(a)), FIRE, 6);
      }

      sfx(level, feet, "weapon.emberlord_maul.supernova", 2.0F, 0.85F);
      boomSound(level, feet, best);
      pool(level, p, feet, 6.0, 100, true);
      GearState.of(p).cooldown("pyraxis.crust", 1);
      regrow(level, p);
   }

   static int heat(ServerWorld level, @Nullable PlayerEntity src, LivingEntity t, int add) {
      return heat(level, src, t, add, 5);
   }

   static int heat(ServerWorld level, @Nullable PlayerEntity src, LivingEntity t, int add, int cap) {
      if (!(t instanceof PlayerEntity) && t.isAlive() && !t.isRemoved() && add > 0) {
         PyraxisArsenal.Heat h = HEAT.computeIfAbsent(t, k -> new PyraxisArsenal.Heat());
         if (h.stacks <= 0) {
            h.stacks = 0;
            h.next = clock + 20L;
         }

         int after = Math.max(h.stacks, Math.min(Math.min(cap, 5), h.stacks + add));
         h.stacks = after;
         h.last = clock;
         if (src != null) {
            h.owner = src;
         }

         if (after >= 3) {
            t.setOnFireFor(3.0F);
         }

         Vfx.embers(level, center(t), FIRE.core(), 2 + after, 0.25 + 0.05 * after);
         if (after >= 5 && !h.molten) {
            h.molten = true;
            Vfx.glint(level, top(t), WHITE_HOT.core(), 1.2);
            Vfx.ring(level, t.getEntityPos(), FIRE.glow(), 1.0 + t.getWidth() * 0.5, 12);
            Vfx.flare(level, center(t), WHITE_HOT.glow(), 1.6, 8);
            sfx(level, t.getEntityPos(), "weapon.emberlord_maul.proc", 0.8F, 0.7F);
            if (!isBoss(t)) {
               t.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 0, false, false, true));
            }
         }

         return after;
      } else {
         return stacks(t);
      }
   }

   static int stacks(LivingEntity t) {
      PyraxisArsenal.Heat h = HEAT.get(t);
      return h == null ? 0 : Math.max(0, h.stacks);
   }

   private static int detonate(ServerWorld level, PlayerEntity p, LivingEntity t, float scale, @Nullable Set<LivingEntity> skip) {
      PyraxisArsenal.Heat h = HEAT.get(t);
      int n = h == null ? 0 : h.stacks;
      if (n <= 0) {
         return 0;
      }

      h.stacks = 0;
      h.molten = false;
      Vec3d c = center(t);
      Vec3d f = t.getEntityPos();
      hit(level, p, t, 3.0F * n * (n == 5 ? 1.5F : 1.0F) * scale, false);

      for (LivingEntity o : GearKit.enemies(level, p, c, 5.5)) {
         if (o != t && (skip == null || !skip.contains(o)) && !(center(o).distanceTo(c) > 2.5 + o.getWidth() * 0.5)) {
            hit(level, p, o, 1.5F * n * scale, false);
            heat(level, p, o, 1);
         }
      }

      Vfx.explosion(level, c, n == 5 ? WHITE_HOT : FIRE, 1.0 + 0.3 * n);
      Vfx.pillar(level, f, FIRE, 1.2 + 0.5 * n, 0.4 + 0.08 * n);
      Vfx.debris(level, c, ASH, FIRE.smoke(), 4 + 2 * n, 0.35);
      Vfx.glint(level, c, WHITE_HOT.core(), 0.8 + 0.15 * n);
      if (n == 5) {
         Vfx.shockwave(level, f, FIRE, 2.5);
      }

      return n;
   }

   private static void boomSound(ServerWorld level, Vec3d at, int n) {
      if (n > 0) {
         sfx(level, at, "weapon.emberlord_maul.detonate", 0.9F + 0.1F * n, 1.15F - 0.05F * n);
      }
   }

   private static void heatTick() {
      if (!HEAT.isEmpty()) {
         List<LivingEntity> due = new ArrayList<>();
         Iterator<Entry<LivingEntity, PyraxisArsenal.Heat>> it = HEAT.entrySet().iterator();

         while (it.hasNext()) {
            Entry<LivingEntity, PyraxisArsenal.Heat> en = it.next();
            LivingEntity e = en.getKey();
            PyraxisArsenal.Heat h = en.getValue();
            if (e == null || !e.isAlive() || e.isRemoved() || h.stacks <= 0 || !(e.getEntityWorld() instanceof ServerWorld)) {
               it.remove();
            } else if (h.next <= clock) {
               due.add(e);
            }
         }

         for (LivingEntity e : due) {
            PyraxisArsenal.Heat h = HEAT.get(e);
            if (h != null && h.stacks > 0 && e.getEntityWorld() instanceof ServerWorld level) {
               h.next = clock + 20L;
               int n = h.stacks;
               PlayerEntity owner = h.owner != null && h.owner.isAlive() && !h.owner.isRemoved() ? h.owner : null;
               burn(level, owner, e, n);
               Vec3d c = center(e);
               Vfx.embers(level, c, FIRE.core(), n, e.getWidth() * 0.4);
               if (h.molten) {
                  Vfx.flare(level, c, WHITE_HOT.glow(), 1.1, 6);
                  Vfx.smoke(level, top(e), FIRE.smoke(), 2, 0.2, 1.0);
                  if (e.isAlive() && !isBoss(e)) {
                     e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 0, false, false, true));
                  }
               }

               if (clock - h.last >= 100L) {
                  h.stacks--;
                  if (h.stacks < 5) {
                     h.molten = false;
                  }
               }
            }
         }
      }
   }

   private static void pool(ServerWorld level, PlayerEntity p, Vec3d at, double r, int ticks, boolean drawn) {
      addHazard(new PyraxisArsenal.Hazard(p, level, 0, at, at, r, clock, clock + ticks));
      if (!drawn) {
         Vfx.groundCrack(level, at, r, FIRE);
      }

      Vfx.aura(level, at, FIRE, r);
   }

   private static void seam(ServerWorld level, PlayerEntity p, Vec3d a, Vec3d b, double halfWidth, int ticks, boolean drawn) {
      addHazard(new PyraxisArsenal.Hazard(p, level, 1, a, b, halfWidth, clock, clock + ticks));
      if (!drawn) {
         crackSeam(level, a, b);
      }
   }

   private static void patch(ServerWorld level, PlayerEntity p, Vec3d at, double r, int ticks, boolean drawn) {
      addHazard(new PyraxisArsenal.Hazard(p, level, 2, at, at, r, clock, clock + ticks));
      if (!drawn) {
         Vfx.groundCrack(level, at, r, FIRE);
      }
   }

   private static void addHazard(PyraxisArsenal.Hazard h) {
      int mine = 0;

      for (PyraxisArsenal.Hazard o : HAZARDS) {
         if (o.owner() == h.owner()) {
            mine++;
         }
      }

      if (mine >= 6) {
         for (int i = 0; i < HAZARDS.size(); i++) {
            if (HAZARDS.get(i).owner() == h.owner()) {
               HAZARDS.remove(i);
               break;
            }
         }
      }

      if (HAZARDS.size() >= 48) {
         HAZARDS.remove(0);
      }

      HAZARDS.add(h);
   }

   private static List<PyraxisArsenal.Hazard> ownHazards(PlayerEntity p, double within) {
      List<PyraxisArsenal.Hazard> out = new ArrayList<>();
      Vec3d q = p.getEntityPos();

      for (PyraxisArsenal.Hazard h : HAZARDS) {
         if (h.owner() == p && h.until() > clock && h.level() == p.getEntityWorld()) {
            double d = h.kind() == 1 ? flatDist(q, closest(q, h.a(), h.b())) : flatDist(q, h.a());
            if (d - h.r() <= within) {
               out.add(h);
            }
         }
      }

      return out;
   }

   static boolean inOwnMagma(PlayerEntity p) {
      for (PyraxisArsenal.Hazard h : HAZARDS) {
         if (h.owner() == p && h.until() > clock && h.level() == p.getEntityWorld() && inside(h, p)) {
            return true;
         }
      }

      return false;
   }

   private static boolean inside(PyraxisArsenal.Hazard h, LivingEntity e) {
      return h.kind() == 1 ? onSegment(e, h.a(), h.b(), h.r(), 1.5) : inCircle(e, h.a(), h.r(), 1.5);
   }

   private static List<LivingEntity> victims(PyraxisArsenal.Hazard h) {
      if (h.kind() == 1) {
         Vec3d mid = h.a().lerp(h.b(), 0.5);
         return GearKit.enemies(h.level(), h.owner(), mid, h.a().distanceTo(h.b()) / 2.0 + h.r() + 3.0).stream().filter(e -> inside(h, e)).toList();
      } else {
         return GearKit.enemies(h.level(), h.owner(), h.a(), h.r() + 3.0).stream().filter(e -> inside(h, e)).toList();
      }
   }

   private static void hazardTick() {
      if (!HAZARDS.isEmpty()) {
         HAZARDS.removeIf(hx -> hx.until() <= clock || !alive(hx.owner(), hx.level()));
         boolean pulse = clock % 10L == 0L;
         boolean heatPulse = clock % 20L == 0L;
         Map<PlayerEntity, Map<LivingEntity, float[]>> hits = new HashMap<>();

         for (PyraxisArsenal.Hazard h : new ArrayList<>(HAZARDS)) {
            ServerWorld level = h.level();
            Random rnd = level.getRandom();
            long age = clock - h.born();
            long left = h.until() - clock;
            boolean big = h.r() >= 5.0;
            if (age > 0L && age % 20L == 0L) {
               for (int i = 0; i < (big ? 4 : 2); i++) {
                  Vfx.embers(level, somewhere(h, rnd), FIRE.core(), 3, 0.3);
               }
            }

            if (age % 40L == 20L) {
               Vfx.smoke(level, somewhere(h, rnd).add(0.0, 0.3, 0.0), FIRE.smoke(), 2, 0.4, 1.2);
            }

            if (age > 0L && age % (big ? 40 : 60) == 0L && left >= 40L) {
               if (h.kind() == 1) {
                  crackSeam(level, h.a(), h.b());
               } else {
                  Vfx.groundCrack(level, h.a(), h.r(), FIRE);
               }
            }

            if (pulse) {
               Map<LivingEntity, float[]> mine = hits.computeIfAbsent(h.owner(), k -> new HashMap<>());

               for (LivingEntity e : victims(h)) {
                  float[] strongest = mine.computeIfAbsent(e, k -> new float[2]);
                  strongest[0] = Math.max(strongest[0], h.kind() == 2 ? 1.0F : 1.5F);
                  if (h.kind() == 0) {
                     strongest[1] = 1.0F;
                  }
               }
            }
         }

         hits.forEach((owner, map) -> map.forEach((ex, dose) -> {
            if (ex.isAlive() && ex.getEntityWorld() instanceof ServerWorld levelx) {
               burn(levelx, owner, ex, dose[0]);
               if (heatPulse) {
                  heat(levelx, owner, ex, 1, 3);
               }

               if (dose[1] > 0.0F && ex.isAlive() && !isBoss(ex)) {
                  ex.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20, 0, false, false, true));
               }
            }
         }));
      }
   }

   private static Vec3d somewhere(PyraxisArsenal.Hazard h, Random rnd) {
      if (h.kind() == 1) {
         Vec3d q = h.a().lerp(h.b(), rnd.nextDouble());
         return q.add((rnd.nextDouble() - 0.5) * h.r() * 2.0, 0.15, (rnd.nextDouble() - 0.5) * h.r() * 2.0);
      } else {
         double a = rnd.nextDouble() * Math.PI * 2.0;
         double d = Math.sqrt(rnd.nextDouble()) * h.r();
         return h.a().add(Math.cos(a) * d, 0.15, Math.sin(a) * d);
      }
   }

   private static void crackSeam(ServerWorld level, Vec3d a, Vec3d b) {
      int n = Math.max(1, (int)Math.ceil(a.distanceTo(b) / 2.5));

      for (int i = 0; i <= n; i++) {
         Vfx.groundCrack(level, a.lerp(b, (double)i / n), 1.0, FIRE);
      }
   }

   private static PyraxisArsenal.Glob glob(ServerWorld level, PlayerEntity owner, Vec3d from, Vec3d vel, float dmg, int heat, double burst, boolean patch) {
      PyraxisArsenal.Glob g = new PyraxisArsenal.Glob(level, owner, from, vel, dmg, heat, burst, patch);
      NEW_GLOBS.add(g);
      return g;
   }

   private static boolean stepGlob(PyraxisArsenal.Glob g) {
      if (!alive(g.owner, g.level)) {
         return true;
      }

      g.age++;
      g.vel = g.vel.add(0.0, -g.gravity, 0.0);
      if (g.homing != null && g.homing.isAlive() && !g.homing.isRemoved()) {
         Vec3d to = center(g.homing).subtract(g.pos);
         if (to.lengthSquared() > 1.0E-4) {
            g.vel = g.vel.lerp(to.normalize().multiply(g.vel.length()), g.homingStrength);
         }
      }

      Vec3d from = g.pos;
      Vec3d next = from.add(g.vel);
      BlockHitResult block = g.level.raycast(new RaycastContext(from, next, ShapeType.COLLIDER, FluidHandling.NONE, g.owner));
      boolean wall = block.getType() != Type.MISS;
      Vec3d end = wall ? block.getPos() : next;
      Vec3d hitAt = null;
      double bestD = Double.MAX_VALUE;

      for (LivingEntity e : GearKit.enemies(g.level, g.owner, from.add(end).multiply(0.5), g.radius + end.distanceTo(from) / 2.0 + 4.0)) {
         Box box = e.getBoundingBox().expand(g.radius);
         Vec3d at = box.contains(from) ? from : (Vec3d)box.raycast(from, end).orElse(null);
         if (at != null && at.squaredDistanceTo(from) < bestD) {
            bestD = at.squaredDistanceTo(from);
            hitAt = at;
         }
      }

      if (hitAt != null) {
         burstGlob(g, hitAt);
         return true;
      }

      if (wall) {
         burstGlob(g, end);
         return true;
      }

      g.pos = next;
      Vfx.trail(g.level, g.pos, FIRE, 0.45);
      if (g.age % 2 == 0) {
         Vfx.embers(g.level, g.pos, FIRE.core(), 1, 0.05);
      }

      if (g.age >= g.life) {
         burstGlob(g, g.pos);
         return true;
      } else {
         return false;
      }
   }

   private static void burstGlob(PyraxisArsenal.Glob g, Vec3d at) {
      ServerWorld level = g.level;
      PlayerEntity p = g.owner;

      for (LivingEntity e : foes(level, p, at, g.burst)) {
         hit(level, p, e, g.dmg, false);
         heat(level, p, e, g.heat);
         resonate(p, "magmaheart_staff", g.patch ? 1.5F : 1.0F);
      }

      Vfx.impact(level, at, FIRE, 0.8);
      Vfx.embers(level, at, FIRE.core(), 6, g.burst * 0.5);
      if (g.patch) {
         Vec3d f = floor(level, at, 0.3, 2);
         if (f != null && at.y - f.y <= 1.5) {
            patch(level, p, f, 1.2, 60, false);
         }
      }

      sfx(level, at, "bolt.meteor.impact", 0.8F, 1.3F);
   }

   private static void meteor(ServerWorld level, PlayerEntity p, Vec3d from, Vec3d at, int fall, double r, float dmg, int kind) {
      PyraxisArsenal.Meteor m = new PyraxisArsenal.Meteor(level, p, from, at, fall, r, dmg, kind);
      NEW_METEORS.add(m);
      Vfx.telegraphCircle(level, at, r, m.fall, FIRE);
      Vfx.meteorTrail(level, from, at.subtract(from).multiply(1.0 / m.fall), kind == 1 ? WHITE_HOT : FIRE);
      sfx(level, from, "bolt.meteor.launch", 1.6F, 0.9F);
   }

   private static boolean stepMeteor(PyraxisArsenal.Meteor m) {
      if (!alive(m.owner, m.level)) {
         return true;
      } else {
         m.age++;
         if (m.age < m.fall) {
            Vec3d vel = m.at.subtract(m.from).multiply(1.0 / m.fall);
            Vfx.meteorTrail(m.level, m.from.lerp(m.at, (double)m.age / m.fall), vel, m.kind == 1 ? WHITE_HOT : FIRE);
            return false;
         } else {
            impact(m);
            return true;
         }
      }
   }

   private static void impact(PyraxisArsenal.Meteor m) {
      ServerWorld level = m.level;
      PlayerEntity p = m.owner;
      Vec3d at = m.at;
      switch (m.kind) {
         case 1:
            for (LivingEntity e : foes(level, p, at, m.r)) {
               hit(level, p, e, m.dmg, false);
               heat(level, p, e, 3);
               shove(e, at, 1.0, 0.6);
               resonate(p, "magmaheart_staff", 1.0F);
            }

            Vfx.heavyImpact(level, at, WHITE_HOT, 2.0);
            Vfx.nova(level, at.add(0.0, 0.5, 0.0), FIRE, 5.0);
            Vfx.groundCrack(level, at, 5.0, FIRE);
            pool(level, p, at, 3.5, 120, true);
            sfx(level, at, "weapon.magmaheart_staff.impact", 2.0F, 0.8F);
            break;
         case 2:
            for (LivingEntity e : foes(level, p, at, m.r)) {
               hit(level, p, e, m.dmg, false);
               heat(level, p, e, 1);
            }

            Vfx.explosion(level, at.add(0.0, 0.3, 0.0), FIRE, 2.0);
            sfx(level, at, "bolt.meteor.impact", 1.2F, 1.1F);
            break;
         case 3:
            for (LivingEntity e : foes(level, p, at, m.r)) {
               hit(level, p, e, m.dmg, false);
               heat(level, p, e, 1);
            }

            Vfx.explosion(level, at.add(0.0, 0.3, 0.0), FIRE, 2.5);
            Vfx.groundCrack(level, at, 2.0, FIRE);
            patch(level, p, at, 1.2, 60, true);
            sfx(level, at, "bolt.meteor.impact", 1.4F, 1.0F);
            break;
         default:
            for (LivingEntity e : foes(level, p, at, m.r)) {
               hit(level, p, e, m.dmg, false);
               heat(level, p, e, 2);
               shove(e, at, 0.4, 0.5);
               resonate(p, "magmaheart_staff", 1.0F);
            }

            Vfx.heavyImpact(level, at, FIRE, 0.9);
            Vfx.groundCrack(level, at, 2.5, FIRE);
            patch(level, p, at, 1.5, 80, true);
            sfx(level, at, "bolt.meteor.impact", 1.8F, 1.0F);
      }
   }

   private static void later(ServerWorld level, PlayerEntity p, int ticks, Runnable r) {
      PENDING.add(new PyraxisArsenal.Task(clock + Math.max(1, ticks), () -> {
         if (alive(p, level)) {
            r.run();
         }
      }));
   }

   private static void soon(ServerWorld level, PlayerEntity p, int ticks, Runnable r) {
      if (ticks <= 0) {
         r.run();
      } else {
         later(level, p, ticks, r);
      }
   }

   private static void repeat(ServerWorld level, PlayerEntity p, int first, int every, int times, IntConsumer step) {
      for (int i = 0; i < times; i++) {
         int n = i;
         later(level, p, first + every * i, () -> step.accept(n));
      }
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
         List<PyraxisArsenal.Task> due = new ArrayList<>();
         TASKS.removeIf(tx -> tx.at() <= clock && due.add(tx));

         for (PyraxisArsenal.Task t : due) {
            guard("task", t.run());
         }
      }

      guard("heat", PyraxisArsenal::heatTick);
      guard("hazards", PyraxisArsenal::hazardTick);
      if (!GLOBS.isEmpty()) {
         GLOBS.removeIf(g -> stepSafely(() -> stepGlob(g)));
      }

      if (!NEW_GLOBS.isEmpty()) {
         GLOBS.addAll(NEW_GLOBS);
         NEW_GLOBS.clear();
      }

      if (!METEORS.isEmpty()) {
         METEORS.removeIf(m -> stepSafely(() -> stepMeteor(m)));
      }

      if (!NEW_METEORS.isEmpty()) {
         METEORS.addAll(NEW_METEORS);
         NEW_METEORS.clear();
      }

      if (!LEAPS.isEmpty()) {
         LEAPS.removeIf(l -> stepSafely(() -> stepLeap(l)));
      }

      if (!RUSHES.isEmpty()) {
         RUSHES.removeIf(r -> stepSafely(() -> stepRush(r)));
      }

      if (clock % 4L == 0L) {
         guard("tells", () -> playerTells(server));
      }

      if (clock % 200L == 0L) {
         NO_FALL.values().removeIf(until -> until < clock);
         SEAR_UNTIL.values().removeIf(until -> until < clock);
         UNBOUND_UNTIL.values().removeIf(until -> until < clock);
      }
   }

   private static boolean stepSafely(PyraxisArsenal.Step s) {
      try {
         return s.step();
      } catch (RuntimeException ex) {
         MysticalTerrain.LOGGER.error("Pyraxis gear tracker failed", ex);
         return true;
      }
   }

   private static void guard(String what, Runnable r) {
      try {
         r.run();
      } catch (RuntimeException ex) {
         MysticalTerrain.LOGGER.error("Pyraxis gear " + what + " failed", ex);
      }
   }

   private static void playerTells(MinecraftServer server) {
      for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
         if (p.isAlive() && !p.isSpectator()) {
            ServerWorld level = p.getEntityWorld();
            Long sear = SEAR_UNTIL.get(p.getUuid());
            if (sear != null && sear >= clock) {
               Vfx.embers(level, GearKit.tip(p), FIRE.core(), 2, 0.1);
            }

            if (p.isUsingItem() && p.getActiveItem().getItem() instanceof MysticWeapon w) {
               if ("cinderfang".equals(w.gearId())) {
                  int n = Math.min(3, p.getItemUseTime() / 8);
                  LivingEntity t = brandTarget(level, p, 10 + 4 * n);
                  if (t != null) {
                     Vfx.ring(level, t.getEntityPos(), FIRE.glow(), 0.8 + t.getWidth() * 0.5, 5);
                     Vfx.glint(level, top(t), FIRE.core(), 0.5);
                  }
               } else if ("magmaheart_staff".equals(w.gearId())) {
                  int n = Math.min(4, p.getItemUseTime() / 10);
                  Vec3d at = aim(level, p, 32.0);
                  if (at != null) {
                     Vfx.ring(level, at.add(0.0, 0.1, 0.0), FIRE.glow(), 1.5 + 0.5 * n, 5);
                     Vfx.beam(level, at, at.add(0.0, 14.0, 0.0), FIRE, 0.08, 5);
                  }
               }
            }
         }
      }
   }

   private static void reset() {
      HEAT.clear();
      TASKS.clear();
      PENDING.clear();
      HAZARDS.clear();
      GLOBS.clear();
      NEW_GLOBS.clear();
      METEORS.clear();
      NEW_METEORS.clear();
      LEAPS.clear();
      RUSHES.clear();
      SEAR_UNTIL.clear();
      UNBOUND_UNTIL.clear();
      NO_FALL.clear();
      CRUSTED.clear();
      clock = 0L;
   }

   private static void hit(ServerWorld level, PlayerEntity p, LivingEntity e, float dmg, boolean melee) {
      if (e.isAlive() && !(dmg <= 0.0F)) {
         e.timeUntilRegen = 0;
         if (melee) {
            GearKit.strike(level, p, e, dmg);
         } else {
            GearKit.magic(level, p, e, dmg);
         }
      }
   }

   private static void burn(ServerWorld level, @Nullable PlayerEntity p, LivingEntity e, float dmg) {
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

   private static boolean isBoss(LivingEntity e) {
      return e instanceof MysticalBoss b ? b.isBoss() : e instanceof EnderDragonEntity || e instanceof WitherEntity;
   }

   private static void shove(LivingEntity e, Vec3d from, double strength, double lift) {
      double k = 1.0 - MathHelper.clamp(e.getAttributeValue(EntityAttributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
      if (!(k <= 0.0)) {
         Vec3d d = new Vec3d(e.getX() - from.x, 0.0, e.getZ() - from.z);
         d = d.lengthSquared() < 1.0E-4 ? Vec3d.ZERO : d.normalize();
         e.addVelocity(d.x * strength * k, lift * k, d.z * strength * k);
         e.knockedBack = true;
      }
   }

   private static void pull(LivingEntity e, Vec3d to, double strength, double lift) {
      double k = 1.0 - MathHelper.clamp(e.getAttributeValue(EntityAttributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
      Vec3d d = new Vec3d(to.x - e.getX(), 0.0, to.z - e.getZ());
      if (!(k <= 0.0) && !(d.lengthSquared() < 1.0)) {
         d = d.normalize();
         e.addVelocity(d.x * strength * k, lift * k, d.z * strength * k);
         e.knockedBack = true;
      }
   }

   private static void launch(LivingEntity e, double up) {
      double k = 1.0 - MathHelper.clamp(e.getAttributeValue(EntityAttributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
      if (k > 0.0) {
         e.addVelocity(0.0, up * k, 0.0);
         e.knockedBack = true;
      }
   }

   private static void noFall(PlayerEntity p, int ticks) {
      NO_FALL.merge(p.getUuid(), clock + ticks, Long::max);
   }

   private static boolean blocked(ServerWorld level, PlayerEntity p, Vec3d dir) {
      Box box = p.getBoundingBox().offset(dir.x * 0.8, 0.05, dir.z * 0.8);
      return !level.isSpaceEmpty(p, box) && !level.isSpaceEmpty(p, box.offset(0.0, 0.55, 0.0));
   }

   private static @Nullable LivingEntity brandTarget(ServerWorld level, PlayerEntity p, double range) {
      Vec3d eye = GearKit.eye(p);
      Vec3d look = p.getRotationVector();
      Vec3d end = eye.add(look.multiply(range));
      LivingEntity direct = null;
      double directD = Double.MAX_VALUE;
      LivingEntity near = null;
      double nearScore = Double.MAX_VALUE;

      for (LivingEntity e : GearKit.enemies(level, p, eye.add(look.multiply(range / 2.0)), range / 2.0 + 3.0)) {
         Box box = e.getBoundingBox().expand(0.25);
         Optional<Vec3d> through = box.contains(eye) ? Optional.of(eye) : box.raycast(eye, end);
         if (through.isPresent()) {
            double d = through.get().squaredDistanceTo(eye);
            if (d < directD && p.canSee(e)) {
               direct = e;
               directD = d;
            }
         } else {
            Vec3d to = center(e).subtract(eye);
            double along = to.dotProduct(look);
            if (!(along < 0.5) && !(along > range)) {
               double miss = Math.sqrt(box.squaredMagnitude(eye.add(look.multiply(along))));
               double score = miss / along;
               boolean close = miss <= 0.6 + 0.2 * along || to.normalize().dotProduct(look) > 0.96;
               if (close && score < nearScore && p.canSee(e)) {
                  near = e;
                  nearScore = score;
               }
            }
         }
      }

      return direct != null ? direct : near;
   }

   private static boolean blink(ServerWorld level, PlayerEntity p, LivingEntity t) {
      Vec3d from = p.getEntityPos();
      double back = 1.6 + t.getWidth() * 0.5;
      Vec3d behind = t.getEntityPos().add(Vec3d.fromPolar(0.0F, t.getYaw()).multiply(-back));
      Vec3d toMe = new Vec3d(from.x - t.getX(), 0.0, from.z - t.getZ());
      Vec3d side = t.getEntityPos().add((toMe.lengthSquared() < 1.0E-4 ? GearKit.flatLook(p).multiply(-1.0) : toMe.normalize()).multiply(back));
      Vec3d to = null;

      for (Vec3d c : List.of(behind, side)) {
         Vec3d g = floor(level, c, 1.0, 3);
         Vec3d spot = g != null ? g : c;
         if (level.isSpaceEmpty(p, p.getBoundingBox().offset(spot.subtract(from)))) {
            to = spot;
            break;
         }
      }

      if (to == null) {
         return p.distanceTo(t) <= 4.0 + t.getWidth() * 0.5;
      }

      Vec3d path = to.subtract(from);
      Vfx.dashTrail(level, from.add(0.0, 1.0, 0.0), to.add(0.0, 1.0, 0.0), FIRE);
      if (path.lengthSquared() > 1.0E-4) {
         Vfx.streaks(level, from.add(0.0, 1.0, 0.0), path.normalize(), FIRE, 12);
      }

      Vfx.flare(level, from.add(0.0, 1.0, 0.0), FIRE.glow(), 1.8, 6);
      Vec3d look = center(t).subtract(to.add(0.0, p.getStandingEyeHeight(), 0.0));
      float yaw = (float)(MathHelper.atan2(look.z, look.x) * 180.0F / (float)Math.PI) - 90.0F;
      float pitch = (float)(-MathHelper.atan2(look.y, Math.sqrt(look.x * look.x + look.z * look.z)) * 180.0F / (float)Math.PI);
      p.teleport(level, to.x, to.y, to.z, Set.of(), yaw, pitch, true);
      p.onLanding();
      noFall(p, 20);
      return true;
   }

   private static List<LivingEntity> foes(ServerWorld level, PlayerEntity p, Vec3d at, double r) {
      return GearKit.enemies(level, p, at, r + 5.0).stream().filter(e -> boxDist(e.getBoundingBox(), at) <= r).toList();
   }

   private static double boxDist(Box b, Vec3d at) {
      double dx = Math.max(Math.max(b.minX - at.x, 0.0), at.x - b.maxX);
      double dy = Math.max(Math.max(b.minY - at.y, 0.0), at.y - b.maxY);
      double dz = Math.max(Math.max(b.minZ - at.z, 0.0), at.z - b.maxZ);
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   private static double flatDist(Vec3d a, Vec3d b) {
      double dx = a.x - b.x;
      double dz = a.z - b.z;
      return Math.sqrt(dx * dx + dz * dz);
   }

   private static Vec3d closest(Vec3d q, Vec3d a, Vec3d b) {
      double abx = b.x - a.x;
      double abz = b.z - a.z;
      double len2 = abx * abx + abz * abz;
      double t = len2 < 1.0E-6 ? 0.0 : MathHelper.clamp(((q.x - a.x) * abx + (q.z - a.z) * abz) / len2, 0.0, 1.0);
      return a.lerp(b, t);
   }

   private static boolean onSegment(LivingEntity e, Vec3d a, Vec3d b, double halfWidth, double dy) {
      Vec3d q = e.getEntityPos();
      Vec3d k = closest(q, a, b);
      return flatDist(q, k) <= halfWidth + e.getWidth() * 0.5 && Math.abs(q.y - k.y) <= dy;
   }

   private static boolean inCircle(LivingEntity e, Vec3d c, double r, double dy) {
      return flatDist(e.getEntityPos(), c) <= r + e.getWidth() * 0.5 && Math.abs(e.getY() - c.y) <= dy;
   }

   private static Vec3d flat(Vec3d v) {
      return new Vec3d(v.x, 0.0, v.z);
   }

   private static Vec3d center(LivingEntity e) {
      return e.getBoundingBox().getCenter();
   }

   private static Vec3d top(LivingEntity e) {
      return e.getEntityPos().add(0.0, e.getHeight() + 0.3, 0.0);
   }

   private static Vec3d chest(PlayerEntity p) {
      return p.getEntityPos().add(0.0, 1.2, 0.0);
   }

   private static float yawTo(Vec3d from, Vec3d to) {
      return (float)(MathHelper.atan2(to.z - from.z, to.x - from.x) * 180.0F / (float)Math.PI) - 90.0F;
   }

   private static Vec3d clipTo(ServerWorld level, PlayerEntity p, Vec3d from, Vec3d dir, double range) {
      return level.raycast(new RaycastContext(from, from.add(dir.multiply(range)), ShapeType.COLLIDER, FluidHandling.NONE, p)).getPos();
   }

   private static @Nullable Vec3d aim(ServerWorld level, PlayerEntity p, double range) {
      Vec3d from = GearKit.eye(p);
      Vec3d look = p.getRotationVector();
      BlockHitResult hit = level.raycast(new RaycastContext(from, from.add(look.multiply(range)), ShapeType.COLLIDER, FluidHandling.NONE, p));
      boolean miss = hit.getType() == Type.MISS;
      Vec3d at = miss ? from.add(look.multiply(range)) : hit.getPos().subtract(look.multiply(0.2));
      return floor(level, at, 0.5, miss ? 12 : 8);
   }

   private static Vec3d ground(ServerWorld level, Vec3d at) {
      Vec3d g = floor(level, at, 1.5, 8);
      return g != null ? g : at;
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

   private static final class Glob {
      final ServerWorld level;
      final PlayerEntity owner;
      Vec3d pos;
      Vec3d vel;
      final float dmg;
      final int heat;
      final double burst;
      final boolean patch;
      double gravity = 0.045;
      double radius = 0.35;
      int life = 50;
      int age;
      @Nullable LivingEntity homing;
      double homingStrength;

      Glob(ServerWorld level, PlayerEntity owner, Vec3d pos, Vec3d vel, float dmg, int heat, double burst, boolean patch) {
         this.level = level;
         this.owner = owner;
         this.pos = pos;
         this.vel = vel;
         this.dmg = dmg;
         this.heat = heat;
         this.burst = burst;
         this.patch = patch;
      }
   }

   private record Hazard(PlayerEntity owner, ServerWorld level, int kind, Vec3d a, Vec3d b, double r, long born, long until) {
   }

   private static final class Heat {
      int stacks;
      long last;
      long next;
      @Nullable PlayerEntity owner;
      boolean molten;
      int maulHits;
      @Nullable UUID maulBy;
   }

   private static final class Leap {
      final PlayerEntity p;
      final ServerWorld level;
      final GearUse u;
      final float pw;
      final boolean empowered;
      int age;

      Leap(PlayerEntity p, ServerWorld level, GearUse u) {
         this.p = p;
         this.level = level;
         this.u = u;
         this.pw = u.power();
         this.empowered = u.empowered();
      }
   }

   private static final class Meteor {
      final ServerWorld level;
      final PlayerEntity owner;
      final Vec3d from;
      final Vec3d at;
      final int fall;
      final double r;
      final float dmg;
      final int kind;
      int age;

      Meteor(ServerWorld level, PlayerEntity owner, Vec3d from, Vec3d at, int fall, double r, float dmg, int kind) {
         this.level = level;
         this.owner = owner;
         this.from = from;
         this.at = at;
         this.fall = Math.max(2, fall);
         this.r = r;
         this.dmg = dmg;
         this.kind = kind;
      }
   }

   private static final class Rush {
      final PlayerEntity p;
      final ServerWorld level;
      final GearUse u;
      final Vec3d start;
      final Vec3d dir;
      final float pw;
      final boolean empowered;
      final Set<LivingEntity> cut = new HashSet<>();
      int age;

      Rush(PlayerEntity p, ServerWorld level, GearUse u, Vec3d start, Vec3d dir) {
         this.p = p;
         this.level = level;
         this.u = u;
         this.start = start;
         this.dir = dir;
         this.pw = u.power();
         this.empowered = u.empowered();
      }
   }

   @FunctionalInterface
   private interface Step {
      boolean step();
   }

   private record Task(long at, Runnable run) {
   }

   private record Vent(Vec3d at, double r, PyraxisArsenal.Hazard from) {
   }
}
