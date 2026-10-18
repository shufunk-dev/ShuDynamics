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
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public final class PyraxisArsenal extends Arsenal {
   private static final Vfx.Palette FIRE = Vfx.Palette.FIRE;
   private static final Vfx.Palette WHITE_HOT = new Vfx.Palette(16777204, 16762976, 2759188).withDebris(Vfx.Debris.ASH);
   private static final Vfx.Palette OBSIDIAN = new Vfx.Palette(14207216, 6965898, 1314840).withDebris(Vfx.Debris.ASH);
   private static final Vfx.Debris ASH = Vfx.Debris.ASH;
   private static final Vfx.Rune SOLAR = Vfx.Rune.SOLAR;
   private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);
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
                  entity instanceof Player p && source.is(DamageTypeTags.IS_FALL) && NO_FALL.getOrDefault(p.getUUID(), Long.MIN_VALUE) >= clock
               )
            );
      }
   }

   @Override
   protected void weapons() {
      this.weapon("emberlord_maul", 12.0F, -3.2F)
         .primary(Active.of(160, PyraxisArsenal::craterSlam).empowered(60, 1.3F))
         .alternate(Active.of(240, PyraxisArsenal::calderaLeap).empowered(40, 1.25F))
         .charge(Charge.of(3, 10).pose(ItemUseAnimation.TRIDENT).perLevel(0.35F).onLevel(PyraxisArsenal::tectonicLevel))
         .passive(PyraxisArsenal::aftershock)
         .resonancePerHit(12.0F);
      this.weapon("cinderfang", 6.0F, -1.6F)
         .primary(Active.of(100, PyraxisArsenal::cinderRush).empowered(30, 1.25F))
         .alternate(Active.of(180, PyraxisArsenal::ashenStep).empowered(40, 1.25F))
         .charge(
            Charge.of(3, 8).pose(ItemUseAnimation.SPEAR).cast(Active.of(280, PyraxisArsenal::flashpoint).empowered(50, 1.0F)).onLevel(PyraxisArsenal::flashpointLevel)
         )
         .passive(PyraxisArsenal::searingWounds)
         .resonancePerHit(8.0F);
      this.weapon("magmaheart_staff", 4.0F, -2.4F)
         .primary(Active.of(80, PyraxisArsenal::slagVolley).empowered(30, 1.2F))
         .alternate(Active.of(200, PyraxisArsenal::ventTheDeep).empowered(50, 1.25F))
         .charge(
            Charge.of(4, 10).pose(ItemUseAnimation.TRIDENT).cast(Active.of(320, PyraxisArsenal::meteorCall).empowered(60, 1.25F)).onLevel(PyraxisArsenal::meteorLevel)
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
      ServerLevel level = u.level();
      Player p = u.player();
      int c = u.charge();
      float pw = u.power();
      float ds = 0.5F + 0.5F * pw;
      double r = 3.5 + 0.75 * c;
      Vec3 at = ground(level, p.position().add(GearKit.flatLook(p).scale(2.2)));
      Vfx.slashArc(level, p.position().add(0.0, 1.5, 0.0), p.getYRot(), 15.0F, 90.0F, 3.2, FIRE, 160.0F);
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

   private static void fissures(ServerLevel level, Player p, Vec3 at, int c, float pw) {
      Vec3 look = GearKit.flatLook(p);
      List<Vec3> dirs = new ArrayList<>();
      if (c >= 3) {
         for (float a : new float[]{-25.0F, 0.0F, 25.0F}) {
            dirs.add(look.yRot(a * (float) (Math.PI / 180.0)));
         }
      } else {
         dirs.add(look);
      }

      for (Vec3 d : dirs) {
         Vfx.telegraphLine(level, at, d, 9.0, 1.4, 8, FIRE);
      }

      later(level, p, 8, () -> {
         Set<LivingEntity> done = new HashSet<>();

         for (Vec3 d : dirs) {
            Vec3 end = ground(level, at.add(d.scale(9.0)));

            for (LivingEntity e : GearKit.enemies(level, p, at.add(d.scale(4.5)), 9.0)) {
               if (!done.contains(e) && onSegment(e, at, end, 1.4, 2.0)) {
                  done.add(e);
                  hit(level, p, e, 12.0F * pw, false);
                  heat(level, p, e, 2);
                  launch(e, 0.5);
               }
            }

            for (double s = 0.0; s <= 9.01; s += 2.5) {
               Vfx.groundCrack(level, ground(level, at.add(d.scale(s))), 1.4, FIRE);
            }

            for (int k = 3; k <= 9; k += 3) {
               Vfx.pillar(level, ground(level, at.add(d.scale(k))), FIRE, 2.5, 0.5);
            }

            Vfx.streaks(level, at.add(0.0, 0.3, 0.0), d, FIRE, 8);
            seam(level, p, at, end, 0.9, 80, true);
         }

         sfx(level, at, "weapon.emberlord_maul.erupt", 1.2F, 0.9F);
      });
   }

   private static void hellfireRing(ServerLevel level, Player p, Vec3 at, double r0, float pw) {
      Set<LivingEntity> burned = new HashSet<>();
      later(level, p, 4, () -> {
         Vfx.telegraphCircle(level, at, 8.0, 10, FIRE);
         sfx(level, at, "weapon.emberlord_maul.erupt", 1.0F, 1.1F);
      });
      repeat(level, p, 5, 1, 10, k -> {
         double radius = r0 + (8.0 - r0) * (k + 1) / 10.0;

         for (LivingEntity e : GearKit.enemies(level, p, at, radius + 3.0)) {
            double d = flatDist(e.position(), at);
            double w = e.getBbWidth() * 0.5;
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

   static void tectonicLevel(ServerLevel level, Player p, WeaponKit w, int lvl, boolean max) {
      Vec3 feet = ground(level, p.position());
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
      ServerLevel level = u.level();
      Player p = u.player();
      Vec3 from = p.position();
      Vec3 lp = GearKit.lookPoint(level, p, 14.0);
      Vec3 off = new Vec3(lp.x - from.x, 0.0, lp.z - from.z);
      Vec3 dir;
      double dist;
      if (off.lengthSqr() < 0.25) {
         dir = GearKit.flatLook(p);
         dist = 8.0;
      } else {
         dir = off.normalize();
         dist = Mth.clamp(off.length(), 3.0, 14.0);
      }

      Vec3 target = ground(level, new Vec3(from.x + dir.x * dist, Math.max(from.y, lp.y), from.z + dir.z * dist));
      double h = Math.min(1.5, dist / 10.2);
      p.setDeltaMovement(dir.x * h, 1.0, dir.z * h);
      p.syncVelocity = true;
      p.resetFallDistance();
      noFall(p, 60);
      LEAPS.removeIf(l -> l.p == p);
      LEAPS.add(new PyraxisArsenal.Leap(p, level, u));
      Vec3 feet = ground(level, from);
      Vfx.telegraphCircle(level, target, 4.0, 20, FIRE);
      Vfx.explosion(level, feet.add(0.0, 0.2, 0.0), FIRE, 1.2);
      Vfx.streaks(level, feet, UP, FIRE, 8);
      Vfx.groundCrack(level, feet, 1.6, FIRE);
      return true;
   }

   private static boolean stepLeap(PyraxisArsenal.Leap l) {
      Player p = l.p;
      if (!alive(p, l.level)) {
         return true;
      }

      l.age++;
      p.resetFallDistance();
      noFall(p, 20);
      Vfx.meteorTrail(l.level, p.position().add(0.0, 0.9, 0.0), p.getKnownMovement(), FIRE);
      if ((l.age < 4 || !p.onGround() && !p.isInWater() && !p.isInLava()) && l.age < 40) {
         return false;
      }

      land(l);
      return true;
   }

   private static void land(PyraxisArsenal.Leap l) {
      Player p = l.p;
      ServerLevel level = l.level;
      Vec3 at = ground(level, p.position());

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
         RandomSource rnd = level.getRandom();
         Vec3 f = GearKit.flatLook(p);
         double base = Math.atan2(f.z, f.x);

         for (int i = 0; i < 3; i++) {
            double a = base + i * Math.PI * 2.0 / 3.0;
            double d = 3.5 + rnd.nextDouble() * 1.5;
            Vec3 m = ground(level, at.add(Math.cos(a) * d, 0.0, Math.sin(a) * d));
            meteor(level, p, m.add(rnd.nextBoolean() ? 3.0 : -3.0, 16.0, rnd.nextBoolean() ? 3.0 : -3.0), m, 12, 2.5, 12.0F * l.pw, 3);
         }
      }
   }

   static void aftershock(ServerLevel level, LivingEntity a, LivingEntity t, ItemStack s) {
      if (a instanceof Player p) {
         heat(level, p, t, 1);
         GearKit.hitSpark(level, t, FIRE);
         Vfx.embers(level, center(t), FIRE.core(), 4, 0.3);
         PyraxisArsenal.Heat h = HEAT.get(t);
         if (h != null) {
            if (!p.getUUID().equals(h.maulBy)) {
               h.maulBy = p.getUUID();
               h.maulHits = 0;
            }

            if (++h.maulHits >= 3) {
               h.maulHits = 0;
               Vec3 feet = ground(level, t.position());
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
      ServerLevel level = u.level();
      Player p = u.player();
      Vec3 dir = GearKit.flatLook(p);
      Vec3 start = p.position();
      RUSHES.removeIf(r -> r.p == p);
      RUSHES.add(new PyraxisArsenal.Rush(p, level, u, start, dir));
      p.setDeltaMovement(dir.x * 2.0, Math.max(p.getKnownMovement().y, 0.05), dir.z * 2.0);
      p.syncVelocity = true;
      Vfx.streaks(level, start.add(0.0, 1.0, 0.0), dir, FIRE, 10);
      return true;
   }

   private static boolean stepRush(PyraxisArsenal.Rush r) {
      Player p = r.p;
      ServerLevel level = r.level;
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

      Vfx.embers(level, p.position().add(0.0, 0.5, 0.0), FIRE.core(), 3, 0.3);
      if (r.age < 7 && !blocked(level, p, r.dir)) {
         p.setDeltaMovement(r.dir.x * 2.0, Math.max(p.getKnownMovement().y, 0.05), r.dir.z * 2.0);
         p.syncVelocity = true;
         return false;
      } else {
         endRush(r);
         return true;
      }
   }

   private static void endRush(PyraxisArsenal.Rush r) {
      Player p = r.p;
      ServerLevel level = r.level;
      Vec3 end = p.position();
      p.setDeltaMovement(r.dir.x * 0.3, p.getKnownMovement().y, r.dir.z * 0.3);
      p.syncVelocity = true;
      Vfx.dashTrail(level, r.start.add(0.0, 1.0, 0.0), end.add(0.0, 1.0, 0.0), FIRE);
      if (flatDist(r.start, end) > 1.0) {
         seam(level, p, ground(level, r.start), ground(level, end), 0.8, 60, false);
      }

      if (r.empowered) {
         later(level, p, 5, () -> returnStrike(level, p, r));
      }
   }

   private static void returnStrike(ServerLevel level, Player p, PyraxisArsenal.Rush r) {
      Vec3 end = p.position();
      Vec3 back = r.start.subtract(end);
      double len = back.length();
      if (!(len < 1.5)) {
         boolean clear = level.clip(new ClipContext(end.add(0.0, 1.0, 0.0), r.start.add(0.0, 1.0, 0.0), Block.COLLIDER, Fluid.NONE, p))
                  .getType()
               == Type.MISS
            && level.noCollision(p, p.getBoundingBox().move(back));
         if (clear) {
            p.teleportTo(r.start.x, r.start.y, r.start.z);
            p.resetFallDistance();
            Vfx.dashTrail(level, end.add(0.0, 1.0, 0.0), r.start.add(0.0, 1.0, 0.0), WHITE_HOT);
            Vec3 d = back.normalize();
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
      ServerLevel level = u.level();
      Player p = u.player();
      float pw = u.power();
      boolean emp = u.empowered();
      double r = emp ? 4.0 : 3.0;
      Vec3 effigy = p.position();
      Vec3 back = GearKit.flatLook(p).scale(-1.0);
      p.setDeltaMovement(back.x * 1.3, 0.45, back.z * 1.3);
      p.syncVelocity = true;
      p.resetFallDistance();
      noFall(p, 40);
      Vec3 body = effigy.add(0.0, 1.0, 0.0);
      Vec3 g = ground(level, effigy);
      Vfx.flare(level, body, FIRE.glow(), 2.4, 16);
      Vfx.wisps(level, body, FIRE, 6, 0.6, 16);
      Vfx.telegraphCircle(level, g, r, 16, FIRE);
      Vfx.dashTrail(level, body, clipTo(level, p, body, back, 6.0), FIRE);
      SEAR_UNTIL.put(p.getUUID(), clock + 60L);
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

   static void flashpointLevel(ServerLevel level, Player p, WeaponKit w, int lvl, boolean max) {
      Vec3 at = GearKit.tip(p);
      Vfx.flare(level, at, max ? WHITE_HOT.core() : FIRE.glow(), 0.8 + 0.35 * lvl, 6);
      Vfx.sparks(level, at, FIRE.core(), 3 + 2 * lvl, 0.18, 0.14);
      Vfx.ring(level, p.position(), FIRE.glow(), Math.max(0.6, 2.4 - 0.5 * lvl), 8);
      if (max) {
         Vfx.gather(level, at, FIRE, 1.2, 10);
         Vfx.glint(level, at, WHITE_HOT.core(), 1.0);
      }
   }

   static boolean flashpoint(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      float k = 1.0F + 0.25F * u.charge();
      LivingEntity t = brandTarget(level, p, 10 + 4 * u.charge());
      if (t != null && blink(level, p, t)) {
         if (u.fullyCharged() && stacks(t) < 3) {
            heat(level, p, t, 3 - stacks(t));
         }

         Vec3 c = center(t);
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
            Vec3[] last = new Vec3[]{c};
            repeat(level, p, 5, 5, 2, i -> {
               LivingEntity next = null;
               double best = 64.0;

               for (LivingEntity e : GearKit.enemies(level, p, last[0], 10.0)) {
                  double d = center(e).distanceToSqr(last[0]);
                  if (!chained.contains(e) && stacks(e) >= 1 && d <= best) {
                     best = d;
                     next = e;
                  }
               }

               if (next != null && blink(level, p, next)) {
                  chained.add(next);
                  Vec3 nc = center(next);
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

   private static void xCut(ServerLevel level, Player p, Vec3 at, double size) {
      float yaw = yawTo(p.position(), at);
      Vfx.slashArc(level, at, yaw, 0.0F, 60.0F, 2.2 * size, FIRE, 200.0F);
      Vfx.slashArc(level, at, yaw, 0.0F, -60.0F, 2.2 * size, FIRE, -200.0F);
   }

   static void searingWounds(ServerLevel level, LivingEntity a, LivingEntity t, ItemStack s) {
      if (a instanceof Player p) {
         boolean crit = p.fallDistance > 0.0 && !p.onGround() && !p.onClimbable() && !p.isInWater();
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

         Long sear = SEAR_UNTIL.get(p.getUUID());
         if (sear != null && sear >= clock) {
            SEAR_UNTIL.remove(p.getUUID());
            hit(level, p, t, 6.0F, false);
            heat(level, p, t, 2);
            Vfx.slashArc(level, center(t), p.getYRot(), 0.0F, -40.0F, 1.6, WHITE_HOT, 140.0F);
            GearKit.procSound(level, t, "cinderfang");
         }
      }
   }

   static boolean slagVolley(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      float pw = u.power();
      boolean emp = u.empowered();
      float[] yaws = emp ? new float[]{-14.0F, -7.0F, 0.0F, 7.0F, 14.0F} : new float[]{-8.0F, 0.0F, 8.0F};
      Vec3 look = p.getLookAngle();
      Vec3 from = GearKit.eye(p).add(look.scale(0.8));

      for (float y : yaws) {
         Vec3 v = look.yRot(-y * (float) (Math.PI / 180.0)).scale(1.25).add(0.0, 0.12, 0.0);
         glob(level, p, from, v, 8.0F * pw, emp ? 2 : 1, emp ? 2.8 : 2.2, true);
      }

      GearKit.muzzle(level, p, FIRE, 1.2);
      Vfx.smoke(level, GearKit.tip(p), FIRE.smoke(), 3, 0.3, 1.0);
      return true;
   }

   static boolean ventTheDeep(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      float pw = u.power();
      boolean emp = u.empowered();
      Vec3 me = p.position();
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

      pts.sort(Comparator.comparingDouble(vx -> vx.at().distanceToSqr(me)));
      if (pts.size() > 8) {
         pts = new ArrayList<>(pts.subList(0, 8));
      }

      if (pts.isEmpty()) {
         Vec3 at = aim(level, p, 24.0);
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

   private static void eruption(ServerLevel level, Player p, Vec3 at, double r, float dmg, double height, boolean loud, boolean emp, float pw) {
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
         near.sort(Comparator.comparingDouble(e -> e.distanceToSqr(at)));

         for (int i = 0; i < 2 && !near.isEmpty(); i++) {
            LivingEntity t = near.get(Math.min(i, near.size() - 1));
            Vec3 from = at.add(0.0, 1.2, 0.0);
            Vec3 d = center(t).subtract(from);
            Vec3 v = (d.lengthSqr() < 1.0E-4 ? UP : d.normalize()).scale(0.9).add(0.0, 0.25, 0.0);
            PyraxisArsenal.Glob g = glob(level, p, from, v, 6.0F * pw, 1, 1.6, false);
            g.gravity = 0.02;
            g.life = 40;
            g.homing = t;
            g.homingStrength = 0.12;
         }
      }
   }

   static void meteorLevel(ServerLevel level, Player p, WeaponKit w, int lvl, boolean max) {
      Vfx.flare(level, GearKit.tip(p), FIRE.glow(), 0.9 + 0.3 * lvl, 6);
      Vec3 aim = aim(level, p, 32.0);
      if (aim != null) {
         Vfx.runeCircle(level, aim, 1.2 + 0.6 * lvl, FIRE, 12, 0.08F, SOLAR);
         Vfx.flare(level, aim.add(0.0, 14.0, 0.0), FIRE.core(), 1.0 + 0.5 * lvl, 10);
         if (max) {
            Vfx.chargeUp(level, aim.add(0.0, 14.0, 0.0), WHITE_HOT, 20);
         }
      }
   }

   static boolean meteorCall(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      Vec3 aim = aim(level, p, 32.0);
      if (aim == null) {
         return false;
      }

      float pw = u.power();
      boolean full = u.fullyCharged();
      int c = Math.max(1, u.charge());
      RandomSource rnd = level.getRandom();
      double phase = rnd.nextDouble() * Math.PI * 2.0;
      int ring = full ? 4 : c;

      for (int i = 0; i < ring; i++) {
         double a = phase + i * Math.PI * 2.0 / ring + (rnd.nextDouble() - 0.5) * 0.5;
         double d = 2.0 + rnd.nextDouble() * 2.0;
         Vec3 m = ground(level, aim.add(Math.cos(a) * d, 0.0, Math.sin(a) * d));
         Vec3 from = m.add(rnd.nextBoolean() ? 4.0 : -4.0, 22.0, rnd.nextBoolean() ? 4.0 : -4.0);
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
            Vec3 mx = cinderSpot(level, p, aim);
            meteor(level, p, mx.add(rnd.nextBoolean() ? 3.0 : -3.0, 18.0, rnd.nextBoolean() ? 3.0 : -3.0), mx, 16, 2.0, 10.0F * pw, 2);
         });
      }

      return true;
   }

   private static Vec3 cinderSpot(ServerLevel level, Player p, Vec3 aim) {
      RandomSource rnd = level.getRandom();
      List<LivingEntity> near = GearKit.enemies(level, p, aim, 10.0);
      if (!near.isEmpty()) {
         LivingEntity e = near.get(rnd.nextInt(near.size()));
         Vec3 v = e.getDeltaMovement();
         return ground(level, e.position().add(v.x * 16.0, 0.0, v.z * 16.0));
      } else {
         double a = rnd.nextDouble() * Math.PI * 2.0;
         double d = 2.0 + rnd.nextDouble() * 3.0;
         return ground(level, aim.add(Math.cos(a) * d, 0.0, Math.sin(a) * d));
      }
   }

   static void backdraft(ServerLevel level, LivingEntity a, LivingEntity t, ItemStack s) {
      if (a instanceof Player p) {
         heat(level, p, t, 1);
         hit(level, p, t, 3.0F, false);
         shove(t, a.position(), 0.9, 0.25);
         Vec3 c = center(t);
         Vec3 away = flat(c.subtract(a.position()));
         Vfx.streaks(level, c, away.lengthSqr() < 1.0E-4 ? GearKit.flatLook(p) : away.normalize(), FIRE, 5);
         Vfx.flare(level, c, FIRE.glow(), 1.3, 5);
         GearState st = GearState.of(p);
         if (st.ready("pyraxis.backdraft")) {
            st.cooldown("pyraxis.backdraft", 10);
            GearKit.procSound(level, t, "magmaheart_staff");
         }
      }
   }

   private static void forgeborn(ServerLevel level, Player p, int pieces) {
      if (level.getGameTime() % 20L == 0L) {
         GearKit.keep(p, MobEffects.FIRE_RESISTANCE, 0);
         boolean hot = p.isInLava() || p.getRemainingFireTicks() > 0 || level.getBlockState(p.blockPosition()).is(BlockTags.FIRE) || inOwnMagma(p);
         if (hot && !(p.getHealth() >= p.getMaxHealth())) {
            p.heal(1.0F);
            if (level.getGameTime() % 40L == 0L) {
               Vfx.auraRing(level, p.position(), FIRE, 0.9);
               Vfx.embers(level, chest(p), FIRE.core(), 4, 0.4);
            }
         }
      }
   }

   private static boolean fireproof(ServerLevel level, Player p, DamageSource source, float amount, int pieces) {
      return pieces < 2 || !source.is(DamageTypeTags.IS_FIRE);
   }

   private static void obsidianCrust(ServerLevel level, Player p, int pieces) {
      UUID id = p.getUUID();
      long t = level.getGameTime();
      if (t % 20L == 0L) {
         if (CRUSTED.contains(id) && !p.hasEffect(MobEffects.ABSORPTION)) {
            CRUSTED.remove(id);
         }

         if (!CRUSTED.contains(id) && GearState.of(p).ready("pyraxis.crust") && !p.hasEffect(MobEffects.ABSORPTION)) {
            regrow(level, p);
         }
      }

      if (t % 40L == 0L && CRUSTED.contains(id)) {
         RandomSource rnd = level.getRandom();
         Vfx.glint(level, chest(p).add((rnd.nextDouble() - 0.5) * 0.8, (rnd.nextDouble() - 0.5) * 0.8, (rnd.nextDouble() - 0.5) * 0.8), OBSIDIAN.core(), 0.35);
      }

      Long unbound = UNBOUND_UNTIL.get(id);
      if (unbound != null && unbound >= clock) {
         if (clock % 2L == 0L) {
            Vfx.trail(level, p.position().add(0.0, 0.2, 0.0), FIRE, 0.5);
         }

         if (clock % 10L == 0L) {
            for (LivingEntity e : GearKit.enemies(level, p, p.position(), 2.5)) {
               heat(level, p, e, 1, 3);
            }

            Vfx.embers(level, chest(p), FIRE.core(), 4, 0.6);
         }
      }
   }

   private static void regrow(ServerLevel level, Player p) {
      UUID id = p.getUUID();
      if (CRUSTED.contains(id) || !p.hasEffect(MobEffects.ABSORPTION) || !(p.getAbsorptionAmount() > 0.0F)) {
         p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, 1, true, false, true));
         CRUSTED.add(id);
         Vfx.shieldBubble(level, p.position().add(0.0, 1.0, 0.0), 1.2, OBSIDIAN, 14);
         Vfx.shards(level, chest(p), OBSIDIAN.core(), 8, 0.2);
         sfx(level, p.position(), "weapon.emberlord_maul.crust", 0.5F, 1.4F);
      }
   }

   private static void crustHurt(ServerLevel level, Player p, @Nullable LivingEntity attacker, DamageSource source, float taken, int pieces) {
      UUID id = p.getUUID();
      if (pieces >= 4 && CRUSTED.contains(id)) {
         GearState st = GearState.of(p);
         if (attacker != null && attacker.isAlive() && source.getDirectEntity() == attacker && st.ready("pyraxis.shell")) {
            st.cooldown("pyraxis.shell", 10);
            heat(level, p, attacker, 2);
            burn(level, p, attacker, 3.0F);
            Vec3 c = chest(p);
            Vec3 to = center(attacker).subtract(c);
            Vfx.streaks(level, c, to.lengthSqr() < 1.0E-4 ? GearKit.flatLook(p) : to.normalize(), FIRE, 5);
            GearKit.proc(level, attacker, FIRE);
            sfx(level, attacker.position(), "weapon.cinderfang.proc", 0.7F, 1.2F);
         }

         if (p.getAbsorptionAmount() <= 0.0F) {
            crustBreak(level, p);
         }
      }
   }

   private static void crustBreak(ServerLevel level, Player p) {
      UUID id = p.getUUID();
      CRUSTED.remove(id);
      GearState.of(p).cooldown("pyraxis.crust", 300);
      Vec3 c = chest(p);

      for (LivingEntity e : foes(level, p, p.position(), 4.0)) {
         hit(level, p, e, 10.0F, false);
         heat(level, p, e, 2);
         shove(e, p.position(), 0.9, 0.35);
      }

      Vfx.shards(level, c, OBSIDIAN.core(), 24, 0.8);
      Vfx.debris(level, c, ASH, FIRE.smoke(), 14, 0.5);
      Vfx.explosion(level, c, FIRE, 3.0);
      Vfx.nova(level, c, FIRE, 4.0);
      sfx(level, p.position(), "weapon.emberlord_maul.crust", 1.2F, 0.9F);
      UNBOUND_UNTIL.put(id, clock + 120L);
      p.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 120, 0, true, false, true));
      p.addEffect(new MobEffectInstance(MobEffects.SPEED, 120, 0, true, false, true));
   }

   static boolean heartOfTheVolcano(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      Vec3 feet = ground(level, p.position());
      p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 22, 2, true, false, true));
      p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 22, 3, true, false, true));
      Vfx.runeCircle(level, feet, 7.0, FIRE, 40, 0.08F, SOLAR);
      Vfx.telegraphCircle(level, feet, 7.0, 20, FIRE);
      Vfx.chargeUp(level, chest(p), WHITE_HOT, 20);
      Vfx.vortex(level, feet, 7.0, 4.0, FIRE, 20);
      Vfx.gather(level, chest(p), FIRE, 6.0, 16);
      repeat(level, p, 0, 2, 10, i -> {
         Vec3 c = p.position();

         for (LivingEntity e : GearKit.enemies(level, p, c, 10.0)) {
            if (!isBoss(e)) {
               pull(e, c, 0.18, 0.02);
            }
         }
      });
      later(level, p, 20, () -> supernova(level, p));
      return true;
   }

   private static void supernova(ServerLevel level, Player p) {
      Vec3 feet = ground(level, p.position());
      Vec3 c = chest(p);
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
         Vfx.streaks(level, c, new Vec3(Math.cos(a), 0.0, Math.sin(a)), FIRE, 6);
      }

      sfx(level, feet, "weapon.emberlord_maul.supernova", 2.0F, 0.85F);
      boomSound(level, feet, best);
      pool(level, p, feet, 6.0, 100, true);
      GearState.of(p).cooldown("pyraxis.crust", 1);
      regrow(level, p);
   }

   static int heat(ServerLevel level, @Nullable Player src, LivingEntity t, int add) {
      return heat(level, src, t, add, 5);
   }

   static int heat(ServerLevel level, @Nullable Player src, LivingEntity t, int add, int cap) {
      if (!(t instanceof Player) && t.isAlive() && !t.isRemoved() && add > 0) {
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
            t.igniteForSeconds(3.0F);
         }

         Vfx.embers(level, center(t), FIRE.core(), 2 + after, 0.25 + 0.05 * after);
         if (after >= 5 && !h.molten) {
            h.molten = true;
            Vfx.glint(level, top(t), WHITE_HOT.core(), 1.2);
            Vfx.ring(level, t.position(), FIRE.glow(), 1.0 + t.getBbWidth() * 0.5, 12);
            Vfx.flare(level, center(t), WHITE_HOT.glow(), 1.6, 8);
            sfx(level, t.position(), "weapon.emberlord_maul.proc", 0.8F, 0.7F);
            if (!isBoss(t)) {
               t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0, false, false, true));
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

   private static int detonate(ServerLevel level, Player p, LivingEntity t, float scale, @Nullable Set<LivingEntity> skip) {
      PyraxisArsenal.Heat h = HEAT.get(t);
      int n = h == null ? 0 : h.stacks;
      if (n <= 0) {
         return 0;
      }

      h.stacks = 0;
      h.molten = false;
      Vec3 c = center(t);
      Vec3 f = t.position();
      hit(level, p, t, 3.0F * n * (n == 5 ? 1.5F : 1.0F) * scale, false);

      for (LivingEntity o : GearKit.enemies(level, p, c, 5.5)) {
         if (o != t && (skip == null || !skip.contains(o)) && !(center(o).distanceTo(c) > 2.5 + o.getBbWidth() * 0.5)) {
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

   private static void boomSound(ServerLevel level, Vec3 at, int n) {
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
            if (e == null || !e.isAlive() || e.isRemoved() || h.stacks <= 0 || !(e.level() instanceof ServerLevel)) {
               it.remove();
            } else if (h.next <= clock) {
               due.add(e);
            }
         }

         for (LivingEntity e : due) {
            PyraxisArsenal.Heat h = HEAT.get(e);
            if (h != null && h.stacks > 0 && e.level() instanceof ServerLevel level) {
               h.next = clock + 20L;
               int n = h.stacks;
               Player owner = h.owner != null && h.owner.isAlive() && !h.owner.isRemoved() ? h.owner : null;
               burn(level, owner, e, n);
               Vec3 c = center(e);
               Vfx.embers(level, c, FIRE.core(), n, e.getBbWidth() * 0.4);
               if (h.molten) {
                  Vfx.flare(level, c, WHITE_HOT.glow(), 1.1, 6);
                  Vfx.smoke(level, top(e), FIRE.smoke(), 2, 0.2, 1.0);
                  if (e.isAlive() && !isBoss(e)) {
                     e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0, false, false, true));
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

   private static void pool(ServerLevel level, Player p, Vec3 at, double r, int ticks, boolean drawn) {
      addHazard(new PyraxisArsenal.Hazard(p, level, 0, at, at, r, clock, clock + ticks));
      if (!drawn) {
         Vfx.groundCrack(level, at, r, FIRE);
      }

      Vfx.aura(level, at, FIRE, r);
   }

   private static void seam(ServerLevel level, Player p, Vec3 a, Vec3 b, double halfWidth, int ticks, boolean drawn) {
      addHazard(new PyraxisArsenal.Hazard(p, level, 1, a, b, halfWidth, clock, clock + ticks));
      if (!drawn) {
         crackSeam(level, a, b);
      }
   }

   private static void patch(ServerLevel level, Player p, Vec3 at, double r, int ticks, boolean drawn) {
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

   private static List<PyraxisArsenal.Hazard> ownHazards(Player p, double within) {
      List<PyraxisArsenal.Hazard> out = new ArrayList<>();
      Vec3 q = p.position();

      for (PyraxisArsenal.Hazard h : HAZARDS) {
         if (h.owner() == p && h.until() > clock && h.level() == p.level()) {
            double d = h.kind() == 1 ? flatDist(q, closest(q, h.a(), h.b())) : flatDist(q, h.a());
            if (d - h.r() <= within) {
               out.add(h);
            }
         }
      }

      return out;
   }

   static boolean inOwnMagma(Player p) {
      for (PyraxisArsenal.Hazard h : HAZARDS) {
         if (h.owner() == p && h.until() > clock && h.level() == p.level() && inside(h, p)) {
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
         Vec3 mid = h.a().lerp(h.b(), 0.5);
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
         Map<Player, Map<LivingEntity, float[]>> hits = new HashMap<>();

         for (PyraxisArsenal.Hazard h : new ArrayList<>(HAZARDS)) {
            ServerLevel level = h.level();
            RandomSource rnd = level.getRandom();
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
            if (ex.isAlive() && ex.level() instanceof ServerLevel levelx) {
               burn(levelx, owner, ex, dose[0]);
               if (heatPulse) {
                  heat(levelx, owner, ex, 1, 3);
               }

               if (dose[1] > 0.0F && ex.isAlive() && !isBoss(ex)) {
                  ex.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 0, false, false, true));
               }
            }
         }));
      }
   }

   private static Vec3 somewhere(PyraxisArsenal.Hazard h, RandomSource rnd) {
      if (h.kind() == 1) {
         Vec3 q = h.a().lerp(h.b(), rnd.nextDouble());
         return q.add((rnd.nextDouble() - 0.5) * h.r() * 2.0, 0.15, (rnd.nextDouble() - 0.5) * h.r() * 2.0);
      } else {
         double a = rnd.nextDouble() * Math.PI * 2.0;
         double d = Math.sqrt(rnd.nextDouble()) * h.r();
         return h.a().add(Math.cos(a) * d, 0.15, Math.sin(a) * d);
      }
   }

   private static void crackSeam(ServerLevel level, Vec3 a, Vec3 b) {
      int n = Math.max(1, (int)Math.ceil(a.distanceTo(b) / 2.5));

      for (int i = 0; i <= n; i++) {
         Vfx.groundCrack(level, a.lerp(b, (double)i / n), 1.0, FIRE);
      }
   }

   private static PyraxisArsenal.Glob glob(ServerLevel level, Player owner, Vec3 from, Vec3 vel, float dmg, int heat, double burst, boolean patch) {
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
         Vec3 to = center(g.homing).subtract(g.pos);
         if (to.lengthSqr() > 1.0E-4) {
            g.vel = g.vel.lerp(to.normalize().scale(g.vel.length()), g.homingStrength);
         }
      }

      Vec3 from = g.pos;
      Vec3 next = from.add(g.vel);
      BlockHitResult block = g.level.clip(new ClipContext(from, next, Block.COLLIDER, Fluid.NONE, g.owner));
      boolean wall = block.getType() != Type.MISS;
      Vec3 end = wall ? block.getLocation() : next;
      Vec3 hitAt = null;
      double bestD = Double.MAX_VALUE;

      for (LivingEntity e : GearKit.enemies(g.level, g.owner, from.add(end).scale(0.5), g.radius + end.distanceTo(from) / 2.0 + 4.0)) {
         AABB box = e.getBoundingBox().inflate(g.radius);
         Vec3 at = box.contains(from) ? from : (Vec3)box.clip(from, end).orElse(null);
         if (at != null && at.distanceToSqr(from) < bestD) {
            bestD = at.distanceToSqr(from);
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

   private static void burstGlob(PyraxisArsenal.Glob g, Vec3 at) {
      ServerLevel level = g.level;
      Player p = g.owner;

      for (LivingEntity e : foes(level, p, at, g.burst)) {
         hit(level, p, e, g.dmg, false);
         heat(level, p, e, g.heat);
         resonate(p, "magmaheart_staff", g.patch ? 1.5F : 1.0F);
      }

      Vfx.impact(level, at, FIRE, 0.8);
      Vfx.embers(level, at, FIRE.core(), 6, g.burst * 0.5);
      if (g.patch) {
         Vec3 f = floor(level, at, 0.3, 2);
         if (f != null && at.y - f.y <= 1.5) {
            patch(level, p, f, 1.2, 60, false);
         }
      }

      sfx(level, at, "bolt.meteor.impact", 0.8F, 1.3F);
   }

   private static void meteor(ServerLevel level, Player p, Vec3 from, Vec3 at, int fall, double r, float dmg, int kind) {
      PyraxisArsenal.Meteor m = new PyraxisArsenal.Meteor(level, p, from, at, fall, r, dmg, kind);
      NEW_METEORS.add(m);
      Vfx.telegraphCircle(level, at, r, m.fall, FIRE);
      Vfx.meteorTrail(level, from, at.subtract(from).scale(1.0 / m.fall), kind == 1 ? WHITE_HOT : FIRE);
      sfx(level, from, "bolt.meteor.launch", 1.6F, 0.9F);
   }

   private static boolean stepMeteor(PyraxisArsenal.Meteor m) {
      if (!alive(m.owner, m.level)) {
         return true;
      } else {
         m.age++;
         if (m.age < m.fall) {
            Vec3 vel = m.at.subtract(m.from).scale(1.0 / m.fall);
            Vfx.meteorTrail(m.level, m.from.lerp(m.at, (double)m.age / m.fall), vel, m.kind == 1 ? WHITE_HOT : FIRE);
            return false;
         } else {
            impact(m);
            return true;
         }
      }
   }

   private static void impact(PyraxisArsenal.Meteor m) {
      ServerLevel level = m.level;
      Player p = m.owner;
      Vec3 at = m.at;
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

   private static void later(ServerLevel level, Player p, int ticks, Runnable r) {
      PENDING.add(new PyraxisArsenal.Task(clock + Math.max(1, ticks), () -> {
         if (alive(p, level)) {
            r.run();
         }
      }));
   }

   private static void soon(ServerLevel level, Player p, int ticks, Runnable r) {
      if (ticks <= 0) {
         r.run();
      } else {
         later(level, p, ticks, r);
      }
   }

   private static void repeat(ServerLevel level, Player p, int first, int every, int times, IntConsumer step) {
      for (int i = 0; i < times; i++) {
         int n = i;
         later(level, p, first + every * i, () -> step.accept(n));
      }
   }

   private static boolean alive(Player p, ServerLevel level) {
      return p.isAlive() && !p.isRemoved() && p.level() == level;
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
      for (ServerPlayer p : server.getPlayerList().getPlayers()) {
         if (p.isAlive() && !p.isSpectator()) {
            ServerLevel level = p.level();
            Long sear = SEAR_UNTIL.get(p.getUUID());
            if (sear != null && sear >= clock) {
               Vfx.embers(level, GearKit.tip(p), FIRE.core(), 2, 0.1);
            }

            if (p.isUsingItem() && p.getUseItem().getItem() instanceof MysticWeapon w) {
               if ("cinderfang".equals(w.gearId())) {
                  int n = Math.min(3, p.getTicksUsingItem() / 8);
                  LivingEntity t = brandTarget(level, p, 10 + 4 * n);
                  if (t != null) {
                     Vfx.ring(level, t.position(), FIRE.glow(), 0.8 + t.getBbWidth() * 0.5, 5);
                     Vfx.glint(level, top(t), FIRE.core(), 0.5);
                  }
               } else if ("magmaheart_staff".equals(w.gearId())) {
                  int n = Math.min(4, p.getTicksUsingItem() / 10);
                  Vec3 at = aim(level, p, 32.0);
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

   private static void hit(ServerLevel level, Player p, LivingEntity e, float dmg, boolean melee) {
      if (e.isAlive() && !(dmg <= 0.0F)) {
         e.setInvulnerableTime(0);
         if (melee) {
            GearKit.strike(level, p, e, dmg);
         } else {
            GearKit.magic(level, p, e, dmg);
         }
      }
   }

   private static void burn(ServerLevel level, @Nullable Player p, LivingEntity e, float dmg) {
      if (e.isAlive() && !(dmg <= 0.0F)) {
         int iv = e.getInvulnerableTime();
         e.setInvulnerableTime(0);
         e.hurtServer(level, p != null ? level.damageSources().source(DamageTypes.MAGIC, p) : level.damageSources().magic(), dmg);
         e.setInvulnerableTime(iv);
      }
   }

   private static void resonate(Player p, String weapon, float amount) {
      GearState.of(p).addResonance(weapon, amount);
   }

   private static boolean isBoss(LivingEntity e) {
      return e instanceof MysticalBoss b ? b.isBoss() : e instanceof EnderDragon || e instanceof WitherBoss;
   }

   private static void shove(LivingEntity e, Vec3 from, double strength, double lift) {
      double k = 1.0 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
      if (!(k <= 0.0)) {
         Vec3 d = new Vec3(e.getX() - from.x, 0.0, e.getZ() - from.z);
         d = d.lengthSqr() < 1.0E-4 ? Vec3.ZERO : d.normalize();
         e.push(d.x * strength * k, lift * k, d.z * strength * k);
         e.syncVelocity = true;
      }
   }

   private static void pull(LivingEntity e, Vec3 to, double strength, double lift) {
      double k = 1.0 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
      Vec3 d = new Vec3(to.x - e.getX(), 0.0, to.z - e.getZ());
      if (!(k <= 0.0) && !(d.lengthSqr() < 1.0)) {
         d = d.normalize();
         e.push(d.x * strength * k, lift * k, d.z * strength * k);
         e.syncVelocity = true;
      }
   }

   private static void launch(LivingEntity e, double up) {
      double k = 1.0 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
      if (k > 0.0) {
         e.push(0.0, up * k, 0.0);
         e.syncVelocity = true;
      }
   }

   private static void noFall(Player p, int ticks) {
      NO_FALL.merge(p.getUUID(), clock + ticks, Long::max);
   }

   private static boolean blocked(ServerLevel level, Player p, Vec3 dir) {
      AABB box = p.getBoundingBox().move(dir.x * 0.8, 0.05, dir.z * 0.8);
      return !level.noCollision(p, box) && !level.noCollision(p, box.move(0.0, 0.55, 0.0));
   }

   private static @Nullable LivingEntity brandTarget(ServerLevel level, Player p, double range) {
      Vec3 eye = GearKit.eye(p);
      Vec3 look = p.getLookAngle();
      Vec3 end = eye.add(look.scale(range));
      LivingEntity direct = null;
      double directD = Double.MAX_VALUE;
      LivingEntity near = null;
      double nearScore = Double.MAX_VALUE;

      for (LivingEntity e : GearKit.enemies(level, p, eye.add(look.scale(range / 2.0)), range / 2.0 + 3.0)) {
         AABB box = e.getBoundingBox().inflate(0.25);
         Optional<Vec3> through = box.contains(eye) ? Optional.of(eye) : box.clip(eye, end);
         if (through.isPresent()) {
            double d = through.get().distanceToSqr(eye);
            if (d < directD && p.hasLineOfSight(e)) {
               direct = e;
               directD = d;
            }
         } else {
            Vec3 to = center(e).subtract(eye);
            double along = to.dot(look);
            if (!(along < 0.5) && !(along > range)) {
               double miss = Math.sqrt(box.distanceToSqr(eye.add(look.scale(along))));
               double score = miss / along;
               boolean close = miss <= 0.6 + 0.2 * along || to.normalize().dot(look) > 0.96;
               if (close && score < nearScore && p.hasLineOfSight(e)) {
                  near = e;
                  nearScore = score;
               }
            }
         }
      }

      return direct != null ? direct : near;
   }

   private static boolean blink(ServerLevel level, Player p, LivingEntity t) {
      Vec3 from = p.position();
      double back = 1.6 + t.getBbWidth() * 0.5;
      Vec3 behind = t.position().add(Vec3.directionFromRotation(0.0F, t.getYRot()).scale(-back));
      Vec3 toMe = new Vec3(from.x - t.getX(), 0.0, from.z - t.getZ());
      Vec3 side = t.position().add((toMe.lengthSqr() < 1.0E-4 ? GearKit.flatLook(p).scale(-1.0) : toMe.normalize()).scale(back));
      Vec3 to = null;

      for (Vec3 c : List.of(behind, side)) {
         Vec3 g = floor(level, c, 1.0, 3);
         Vec3 spot = g != null ? g : c;
         if (level.noCollision(p, p.getBoundingBox().move(spot.subtract(from)))) {
            to = spot;
            break;
         }
      }

      if (to == null) {
         return p.distanceTo(t) <= 4.0 + t.getBbWidth() * 0.5;
      }

      Vec3 path = to.subtract(from);
      Vfx.dashTrail(level, from.add(0.0, 1.0, 0.0), to.add(0.0, 1.0, 0.0), FIRE);
      if (path.lengthSqr() > 1.0E-4) {
         Vfx.streaks(level, from.add(0.0, 1.0, 0.0), path.normalize(), FIRE, 12);
      }

      Vfx.flare(level, from.add(0.0, 1.0, 0.0), FIRE.glow(), 1.8, 6);
      Vec3 look = center(t).subtract(to.add(0.0, p.getEyeHeight(), 0.0));
      float yaw = (float)(Mth.atan2(look.z, look.x) * 180.0F / (float)Math.PI) - 90.0F;
      float pitch = (float)(-Mth.atan2(look.y, Math.sqrt(look.x * look.x + look.z * look.z)) * 180.0F / (float)Math.PI);
      p.teleportTo(level, to.x, to.y, to.z, Set.of(), yaw, pitch, true);
      p.resetFallDistance();
      noFall(p, 20);
      return true;
   }

   private static List<LivingEntity> foes(ServerLevel level, Player p, Vec3 at, double r) {
      return GearKit.enemies(level, p, at, r + 5.0).stream().filter(e -> boxDist(e.getBoundingBox(), at) <= r).toList();
   }

   private static double boxDist(AABB b, Vec3 at) {
      double dx = Math.max(Math.max(b.minX - at.x, 0.0), at.x - b.maxX);
      double dy = Math.max(Math.max(b.minY - at.y, 0.0), at.y - b.maxY);
      double dz = Math.max(Math.max(b.minZ - at.z, 0.0), at.z - b.maxZ);
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   private static double flatDist(Vec3 a, Vec3 b) {
      double dx = a.x - b.x;
      double dz = a.z - b.z;
      return Math.sqrt(dx * dx + dz * dz);
   }

   private static Vec3 closest(Vec3 q, Vec3 a, Vec3 b) {
      double abx = b.x - a.x;
      double abz = b.z - a.z;
      double len2 = abx * abx + abz * abz;
      double t = len2 < 1.0E-6 ? 0.0 : Mth.clamp(((q.x - a.x) * abx + (q.z - a.z) * abz) / len2, 0.0, 1.0);
      return a.lerp(b, t);
   }

   private static boolean onSegment(LivingEntity e, Vec3 a, Vec3 b, double halfWidth, double dy) {
      Vec3 q = e.position();
      Vec3 k = closest(q, a, b);
      return flatDist(q, k) <= halfWidth + e.getBbWidth() * 0.5 && Math.abs(q.y - k.y) <= dy;
   }

   private static boolean inCircle(LivingEntity e, Vec3 c, double r, double dy) {
      return flatDist(e.position(), c) <= r + e.getBbWidth() * 0.5 && Math.abs(e.getY() - c.y) <= dy;
   }

   private static Vec3 flat(Vec3 v) {
      return new Vec3(v.x, 0.0, v.z);
   }

   private static Vec3 center(LivingEntity e) {
      return e.getBoundingBox().getCenter();
   }

   private static Vec3 top(LivingEntity e) {
      return e.position().add(0.0, e.getBbHeight() + 0.3, 0.0);
   }

   private static Vec3 chest(Player p) {
      return p.position().add(0.0, 1.2, 0.0);
   }

   private static float yawTo(Vec3 from, Vec3 to) {
      return (float)(Mth.atan2(to.z - from.z, to.x - from.x) * 180.0F / (float)Math.PI) - 90.0F;
   }

   private static Vec3 clipTo(ServerLevel level, Player p, Vec3 from, Vec3 dir, double range) {
      return level.clip(new ClipContext(from, from.add(dir.scale(range)), Block.COLLIDER, Fluid.NONE, p)).getLocation();
   }

   private static @Nullable Vec3 aim(ServerLevel level, Player p, double range) {
      Vec3 from = GearKit.eye(p);
      Vec3 look = p.getLookAngle();
      BlockHitResult hit = level.clip(new ClipContext(from, from.add(look.scale(range)), Block.COLLIDER, Fluid.NONE, p));
      boolean miss = hit.getType() == Type.MISS;
      Vec3 at = miss ? from.add(look.scale(range)) : hit.getLocation().subtract(look.scale(0.2));
      return floor(level, at, 0.5, miss ? 12 : 8);
   }

   private static Vec3 ground(ServerLevel level, Vec3 at) {
      Vec3 g = floor(level, at, 1.5, 8);
      return g != null ? g : at;
   }

   private static @Nullable Vec3 floor(ServerLevel level, Vec3 at, double up, int depth) {
      MutableBlockPos q = BlockPos.containing(at.x, at.y + up, at.z).mutable();
      int steps = depth + (int)Math.ceil(up) + 1;

      for (int i = 0; i < steps; i++) {
         BlockState s = level.getBlockState(q);
         VoxelShape shape = s.getCollisionShape(level, q);
         if (!shape.isEmpty()) {
            return new Vec3(at.x, q.getY() + shape.max(Axis.Y), at.z);
         }

         FluidState f = s.getFluidState();
         if (!f.isEmpty()) {
            return new Vec3(at.x, q.getY() + f.getHeight(level, q), at.z);
         }

         q.move(0, -1, 0);
      }

      return null;
   }

   private static void sfx(ServerLevel level, Vec3 at, String key, float volume, float pitch) {
      SoundEvent e = BossSounds.event(key);
      if (e != null) {
         level.playSound(null, at.x, at.y, at.z, e, SoundSource.PLAYERS, volume, pitch * (0.95F + level.getRandom().nextFloat() * 0.1F));
      }
   }

   private static final class Glob {
      final ServerLevel level;
      final Player owner;
      Vec3 pos;
      Vec3 vel;
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

      Glob(ServerLevel level, Player owner, Vec3 pos, Vec3 vel, float dmg, int heat, double burst, boolean patch) {
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

   private record Hazard(Player owner, ServerLevel level, int kind, Vec3 a, Vec3 b, double r, long born, long until) {
   }

   private static final class Heat {
      int stacks;
      long last;
      long next;
      @Nullable Player owner;
      boolean molten;
      int maulHits;
      @Nullable UUID maulBy;
   }

   private static final class Leap {
      final Player p;
      final ServerLevel level;
      final GearUse u;
      final float pw;
      final boolean empowered;
      int age;

      Leap(Player p, ServerLevel level, GearUse u) {
         this.p = p;
         this.level = level;
         this.u = u;
         this.pw = u.power();
         this.empowered = u.empowered();
      }
   }

   private static final class Meteor {
      final ServerLevel level;
      final Player owner;
      final Vec3 from;
      final Vec3 at;
      final int fall;
      final double r;
      final float dmg;
      final int kind;
      int age;

      Meteor(ServerLevel level, Player owner, Vec3 from, Vec3 at, int fall, double r, float dmg, int kind) {
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
      final Player p;
      final ServerLevel level;
      final GearUse u;
      final Vec3 start;
      final Vec3 dir;
      final float pw;
      final boolean empowered;
      final Set<LivingEntity> cut = new HashSet<>();
      int age;

      Rush(Player p, ServerLevel level, GearUse u, Vec3 start, Vec3 dir) {
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

   private record Vent(Vec3 at, double r, PyraxisArsenal.Hazard from) {
   }
}
