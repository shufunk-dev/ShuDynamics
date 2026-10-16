package com.vexira.mysticalterrain.gear.arsenal;

import com.vexira.mysticalterrain.MysticalTerrain;
import com.vexira.mysticalterrain.boss.BossFx;
import com.vexira.mysticalterrain.boss.BossSounds;
import com.vexira.mysticalterrain.boss.MysticalBoss;
import com.vexira.mysticalterrain.boss.vfx.Vfx;
import com.vexira.mysticalterrain.gear.Active;
import com.vexira.mysticalterrain.gear.ArmorKit;
import com.vexira.mysticalterrain.gear.Charge;
import com.vexira.mysticalterrain.gear.GearKit;
import com.vexira.mysticalterrain.gear.GearState;
import com.vexira.mysticalterrain.gear.GearUse;
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
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStopped;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

public final class GrothmawArsenal extends Arsenal {
   private static final Vfx.Palette PAL = Vfx.Palette.PRIMAL;
   private static final int BONE_TINT = 15260864;
   private static final int BONE_GOLD = 16773808;
   private static final int BLOOD_RED = 14688314;
   private static final int AMBER = 14721088;
   private static final int AMBER_LIGHT = 16762976;
   private static final int UMBER = 7232064;
   private static final Identifier PLATING = MysticalTerrain.id("tyrant_bone_plating");
   private static final String PLATING_KEY = "grothmaw.plating";
   private static final String RAGE_KEY = "grothmaw.rage_cd";
   private static final List<GrothmawArsenal.Task> TASKS = new ArrayList<>();
   private static final Map<LivingEntity, GrothmawArsenal.Shock> AFTERSHOCK = new WeakHashMap<>();
   private static final Map<LivingEntity, GrothmawArsenal.Rend> REND = new WeakHashMap<>();
   private static final Map<LivingEntity, GrothmawArsenal.Sap> SAP = new WeakHashMap<>();
   private static final Map<LivingEntity, GrothmawArsenal.Tomb> TOMBS = new WeakHashMap<>();
   private static final Map<UUID, GrothmawArsenal.Fury> FURY = new HashMap<>();
   private static final Map<UUID, Long> RAGE = new HashMap<>();
   private static final Map<UUID, Long> LEECH = new HashMap<>();
   private static final Map<UUID, GrothmawArsenal.Plate> PLATE = new HashMap<>();
   private static boolean hooked;

   public GrothmawArsenal() {
      super("grothmaw", "tyrant_hide_armor", Vfx.Palette.PRIMAL);
      hook();
   }

   @Override
   protected void weapons() {
      this.weapon("tyrants_fang", 13.0F, -3.3F)
         .primary(Active.of(160, GrothmawArsenal::boneQuake).empowered(50, 1.4F))
         .alternate(Active.of(240, GrothmawArsenal::skullBreaker).empowered(60, 1.5F))
         .charge(Charge.of(3, 16).pose(UseAction.TRIDENT).onLevel(GrothmawArsenal::fangLevel).cast(440, GrothmawArsenal::continentalBreaker))
         .passive(GrothmawArsenal::aftershock)
         .resonancePerHit(14.0F);
      this.weapon("raptor_talons", 7.0F, -1.6F)
         .primary(Active.of(100, GrothmawArsenal::pounce).empowered(30, 1.3F))
         .alternate(Active.of(280, GrothmawArsenal::packHunt).empowered(60, 1.3F))
         .charge(Charge.of(2, 10).perLevel(0.35F).pose(UseAction.BLOCK).onLevel(GrothmawArsenal::talonLevel))
         .passive(GrothmawArsenal::rend)
         .resonancePerHit(7.0F);
      this.weapon("fossil_greataxe", 12.0F, -3.1F)
         .axe()
         .primary(Active.of(140, GrothmawArsenal::reaversSpin).empowered(40, 1.4F))
         .alternate(Active.of(280, GrothmawArsenal::amberTomb).empowered(50, 1.4F))
         .charge(Charge.of(3, 12).perLevel(0.25F).pose(UseAction.SPEAR).onLevel(GrothmawArsenal::axeLevel))
         .passive(GrothmawArsenal::amberSap)
         .resonancePerHit(12.0F);
   }

   @Override
   protected void armor(ArmorKit set) {
      set.knockback(0.25);
      set.twoPiece(GrothmawArsenal::bonePlating);
      set.fourPiece(GrothmawArsenal::furyTick);
      set.onHurt(GrothmawArsenal::hideHurt);
      set.active(560, GrothmawArsenal::extinctionRoar);
   }

   private static synchronized void hook() {
      if (!hooked) {
         hooked = true;
         ServerTickEvents.END_SERVER_TICK.register(GrothmawArsenal::tickAll);
         ServerLifecycleEvents.SERVER_STOPPED.register((ServerStopped)server -> clearAll());
         ServerLivingEntityEvents.AFTER_DAMAGE.register(GrothmawArsenal::afterDamage);
      }
   }

   private static long now(ServerWorld level) {
      return level.getServer().getTicks();
   }

   static void later(ServerWorld level, int delay, Runnable run) {
      TASKS.add(new GrothmawArsenal.Task(now(level) + Math.max(1, delay), run));
   }

   static void every(ServerWorld level, int delay, int period, int count, IntConsumer step) {
      for (int i = 0; i < count; i++) {
         int k = i;
         later(level, delay + i * period, () -> step.accept(k));
      }
   }

   static void track(ServerWorld level, int delay, IntPredicate step) {
      int[] t = new int[]{0};
      Runnable[] self = new Runnable[1];
      self[0] = () -> {
         if (step.test(t[0]++)) {
            later(level, 1, self[0]);
         }
      };
      later(level, delay, self[0]);
   }

   private static void clearAll() {
      TASKS.clear();
      AFTERSHOCK.clear();
      REND.clear();
      SAP.clear();
      TOMBS.clear();
      FURY.clear();
      RAGE.clear();
      LEECH.clear();
      PLATE.clear();
   }

   private static void tickAll(MinecraftServer server) {
      long now = server.getTicks();
      if (!TASKS.isEmpty()) {
         List<GrothmawArsenal.Task> due = new ArrayList<>();
         Iterator<GrothmawArsenal.Task> it = TASKS.iterator();

         while (it.hasNext()) {
            GrothmawArsenal.Task t = it.next();
            if (t.due() <= now) {
               due.add(t);
               it.remove();
            }
         }

         for (GrothmawArsenal.Task t : due) {
            try {
               t.run().run();
            } catch (RuntimeException e) {
               MysticalTerrain.LOGGER.error("Grothmaw gear effect failed", e);
            }
         }
      }

      upkeepRend(now);
      upkeepTombs(now);
      upkeepPlayers(server, now);
   }

   private static void upkeepRend(long now) {
      Iterator<Entry<LivingEntity, GrothmawArsenal.Rend>> it = REND.entrySet().iterator();

      while (it.hasNext()) {
         Entry<LivingEntity, GrothmawArsenal.Rend> en = it.next();
         LivingEntity t = en.getKey();
         GrothmawArsenal.Rend r = en.getValue();
         PlayerEntity owner = r.owner;
         if (t == null || !t.isAlive() || now - r.last > 100L || owner == null || owner.isRemoved()) {
            it.remove();
         } else if (r.count > 0 && now >= r.bleed && t.timeUntilRegen <= 10 && t.getEntityWorld() instanceof ServerWorld level) {
            r.bleed = now + 20L;
            int inv = t.timeUntilRegen;
            GearKit.magic(level, owner, t, 0.6F * r.count);
            t.timeUntilRegen = inv;
            Vfx.embers(level, center(t), 14688314, r.count, 0.25);
         }
      }
   }

   private static void upkeepTombs(long now) {
      if (!TOMBS.isEmpty()) {
         List<LivingEntity> expired = new ArrayList<>();
         List<GrothmawArsenal.Tomb> expiredTombs = new ArrayList<>();
         Iterator<Entry<LivingEntity, GrothmawArsenal.Tomb>> it = TOMBS.entrySet().iterator();

         while (it.hasNext()) {
            Entry<LivingEntity, GrothmawArsenal.Tomb> en = it.next();
            LivingEntity e = en.getKey();
            GrothmawArsenal.Tomb tb = en.getValue();
            if (e == null || !e.isAlive()) {
               it.remove();
            } else if (now >= tb.until) {
               it.remove();
               expired.add(e);
               expiredTombs.add(tb);
            } else {
               boolean heavy = heavy(e);
               if (!heavy) {
                  pin(e, now);
               }

               if (e.getEntityWorld() instanceof ServerWorld level) {
                  long age = now - tb.start;
                  if (age > 0L && age % 10L == 0L) {
                     Vfx.embers(level, center(e), PAL.glow(), 3, 0.5);
                  }

                  if (!heavy && age > 0L && age % 20L == 0L && tb.until - now >= 6L) {
                     bubble(level, e, (int)Math.min(20L, tb.until - now));
                  }
               }
            }
         }

         for (int i = 0; i < expired.size(); i++) {
            shatter(expired.get(i), expiredTombs.get(i));
         }
      }
   }

   private static void upkeepPlayers(MinecraftServer server, long now) {
      Iterator<Entry<UUID, GrothmawArsenal.Plate>> it = PLATE.entrySet().iterator();

      while (it.hasNext()) {
         Entry<UUID, GrothmawArsenal.Plate> en = it.next();
         if (now - en.getValue().seen > 2L) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(en.getKey());
            if (p != null) {
               EntityAttributeInstance max = p.getAttributeInstance(EntityAttributes.MAX_ABSORPTION);
               if (max != null) {
                  max.removeModifier(PLATING);
               }
            }

            it.remove();
         }
      }

      Iterator<Entry<UUID, Long>> rageIt = RAGE.entrySet().iterator();

      while (rageIt.hasNext()) {
         Entry<UUID, Long> en = rageIt.next();
         ServerPlayerEntity p = server.getPlayerManager().getPlayer(en.getKey());
         if (p != null && p.isAlive() && now < en.getValue()) {
            ServerWorld level = p.getEntityWorld();
            Vec3d feet = p.getEntityPos();
            if (now % 20L == 0L) {
               Vfx.auraRing(level, feet, PAL, 1.0);
               Vfx.embers(level, feet.add(0.0, 1.0, 0.0), PAL.glow(), 6, 0.6);
            }

            if (now % 4L == 0L) {
               Vfx.trail(level, feet.add(0.0, 1.2, 0.0), PAL, 0.5);
            }
         } else {
            rageIt.remove();
         }
      }
   }

   static boolean boneQuake(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      float yaw = p.getYaw();
      Vec3d feet = p.getEntityPos();
      Vec3d dir = GearKit.flatLook(p);
      Vec3d start = ground(level, feet.add(dir.multiply(1.5)));
      Vfx.heavyImpact(level, ground(level, feet.add(dir.multiply(1.2))), PAL, 0.7);
      Vfx.groundCrack(level, start, 1.8, PAL);
      dust(level, start, 30, 0.7);
      GearKit.kick(level, p, 0.35);
      Set<LivingEntity> seen = new HashSet<>();
      float[] fan = u.empowered() ? new float[]{0.0F, -22.0F, 22.0F} : new float[]{0.0F};

      for (float off : fan) {
         Vec3d d = yawDir(yaw + off);
         Vec3d from = ground(level, feet.add(d.multiply(1.5)));
         Vfx.telegraphLine(level, from, d, 14.0, 2.4, 5, PAL);
         erupt(u, from, d, 14.0, 1.75, 1.3, 3, 1.8, 0.1, 0.5, seen, e -> quakeHit(u, e));
         if (u.empowered()) {
            Vec3d end = ground(level, from.add(d.multiply(14.0)));
            later(level, 11, () -> tarBurst(u, end));
         }
      }

      later(level, 3, () -> soundAt(level, start.add(dir.multiply(6.0)), 1.2F, 0.9F, "weapon.tyrants_fang.erupt", "boss.grothmaw.bone_quake.hit"));
      return true;
   }

   private static void quakeHit(GearUse u, LivingEntity e) {
      u.strike(e, 14.0F);
      if (heavy(e)) {
         slow(e, u.player(), 0, 20);
      } else {
         launch(e, 0.75);
         slow(e, u.player(), 1, 30);
      }

      Vfx.impact(u.level(), center(e), PAL, 0.6);
      u.resonate(3.0F);
   }

   private static void tarBurst(GearUse u, Vec3d at) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      if (alive(p, level)) {
         Vfx.explosion(level, at, PAL, 2.5);

         for (LivingEntity e : u.enemies(at, 2.5)) {
            rehit(e);
            u.magic(e, 8.0F);
         }

         Vfx.groundCrack(level, at, 2.2, PAL);
         soundAt(level, at, 0.9F, 1.2F, "weapon.tyrants_fang.erupt", "boss.grothmaw.bone_quake.hit");
         every(level, 1, 10, 6, k -> {
            if (alive(p, level)) {
               Vfx.embers(level, at.add(0.0, 0.2, 0.0), PAL.glow(), 6, 1.6);
               Vfx.smoke(level, at, PAL.smoke(), 2, 1.2, 0.8);

               for (LivingEntity ex : u.enemies(at, 3.2)) {
                  if (flatDist(at, ex.getEntityPos()) <= 2.2 + ex.getWidth() / 2.0F && Math.abs(ex.getY() - at.y) < 1.5) {
                     slow(ex, p, 1, 30);
                     u.magic(ex, 1.5F);
                  }
               }
            }
         });
      }
   }

   private static void erupt(
      GearUse u,
      Vec3d start,
      Vec3d dir,
      double length,
      double seg,
      double width,
      int delay,
      double height,
      double growth,
      double thick,
      Set<LivingEntity> seen,
      Consumer<LivingEntity> hit
   ) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      int steps = (int)Math.ceil(length / seg - 1.0E-6);
      every(level, delay, 1, steps, k -> {
         if (alive(p, level)) {
            double a = seg * k;
            double b = Math.min(length, a + seg);
            Vec3d q = ground(level, start.add(dir.multiply((a + b) / 2.0)));
            Vfx.pillar(level, q, PAL, height + growth * k, thick);
            if (k % 2 == 0) {
               Vfx.debris(level, q, Vfx.Debris.BONE, 15260864, 4, 0.4);
               dust(level, q, 6, 0.4);
            }

            if (k % 3 == 0) {
               Vfx.groundCrack(level, q, 1.3, PAL);
            }

            Vec3d from = start.add(dir.multiply(a));

            for (LivingEntity e : GearKit.alongLine(level, p, new Vec3d(from.x, q.y + 0.8, from.z), dir, b - a, width)) {
               if (seen.add(e)) {
                  hit.accept(e);
               }
            }
         }
      });
   }

   static boolean skullBreaker(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      boolean emp = u.empowered();
      Vec3d feet = p.getEntityPos();
      GearKit.dash(p, emp ? 1.1 : 0.95, emp ? 1.05 : 0.75);
      Vfx.shockwave(level, ground(level, feet), PAL, 2.0);
      dust(level, feet, 20, 0.6);
      Vfx.streaks(level, feet.add(0.0, 1.0, 0.0), GearKit.flatLook(p), PAL, 6);
      boolean[] air = new boolean[]{false};
      track(level, 1, t -> {
         if (!alive(p, level)) {
            return false;
         }

         p.onLanding();
         if (!p.isOnGround()) {
            air[0] = true;
         }

         if (t % 2 == 0) {
            Vec3d at = p.getEntityPos().add(0.0, 1.0, 0.0);
            Vfx.trail(level, at, PAL, 1.0);
            Vfx.embers(level, at, PAL.glow(), 2, 0.3);
         }

         boolean landed = t >= 3 && (p.isOnGround() && (air[0] || t >= 10) || p.isTouchingWater());
         if (!landed && t < 40) {
            return true;
         }

         skullLand(u);
         return false;
      });
      return true;
   }

   private static void skullLand(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      boolean emp = u.empowered();
      Vec3d g = ground(level, p.getEntityPos());
      double r = emp ? 6.0 : 4.5;
      long now = now(level);
      Set<LivingEntity> struck = new HashSet<>();

      for (LivingEntity e : u.enemies(g, r + 2.0)) {
         double d = flatDist(g, e.getEntityPos());
         if (!(d > r + e.getWidth() / 2.0F) && !(Math.abs(e.getY() - g.y) > 3.0)) {
            u.strike(e, (float)(18.0 * (1.0 - 0.4 * Math.min(1.0, d / r))));
            if (heavy(e)) {
               slow(e, p, 0, 20);
            } else {
               GearKit.knockAway(e, g, 0.9, 0.55);
               slow(e, p, 2, 40);
            }

            GrothmawArsenal.Shock s = AFTERSHOCK.computeIfAbsent(e, k -> new GrothmawArsenal.Shock());
            s.count = 2;
            s.last = now;
            u.resonate(2.0F);
            struck.add(e);
         }
      }

      float yaw = p.getYaw();
      Vfx.slashArc(level, GearKit.eye(p).add(0.0, -0.3, 0.0), yaw, 20.0F, 90.0F, 3.0, PAL, 170.0F);
      Vfx.heavyImpact(level, g, PAL, emp ? 1.6 : 1.3);
      Vfx.groundCrack(level, g, r, PAL);
      Vfx.debris(level, g, Vfx.Debris.BONE, 15260864, 14, 0.5);
      dust(level, g, 30, r * 0.4);
      GearKit.kick(level, p, 0.6);
      soundAt(level, g, 1.4F, 0.85F, "weapon.tyrants_fang.slam", "weapon.tyrants_fang.use");
      if (emp) {
         for (int i = 0; i < 3; i++) {
            Vfx.pillar(level, ground(level, g.add(yawDir(yaw + 120 * i).multiply(0.6 * r))), PAL, 2.6, 0.6);
         }

         every(level, 1, 1, 12, k -> {
            double ring = 2.0 + 0.5 * (k + 1);
            Vfx.ring(level, g, PAL.glow(), ring, 4);
            if (k % 2 == 0) {
               dustRing(level, g, ring, 8);
            }

            if (alive(p, level)) {
               for (LivingEntity e : u.enemies(g, ring + 2.0)) {
                  double dx = flatDist(g, e.getEntityPos());
                  if (e.isOnGround() && Math.abs(dx - ring) <= 0.5 + e.getWidth() / 2.0F && Math.abs(e.getY() - g.y) < 1.5 && struck.add(e)) {
                     u.magic(e, 6.0F);
                     if (!heavy(e)) {
                        GearKit.knockAway(e, g, 0.8, 0.15);
                     }

                     Vfx.impact(level, center(e), PAL, 0.4);
                  }
               }
            }
         });
      }
   }

   static void fangLevel(ServerWorld level, PlayerEntity p, WeaponKit kit, int c, boolean max) {
      Vec3d feet = p.getEntityPos();
      Vec3d tip = GearKit.tip(p);
      Vfx.groundCrack(level, ground(level, feet), 1.5 + c, PAL);
      Vfx.gather(level, tip, PAL, 1.6, 8);
      Vfx.flare(level, tip, PAL.glow(), 1.0 + 0.4 * c, 6);
      Vfx.shake(level, feet, 8.0, 0.1 * c, 6);
      dust(level, feet, 6 + 4 * c, 0.5 + 0.3 * c);
      if (max) {
         Vfx.chargeUp(level, tip, PAL, 10);
         Vfx.auraRing(level, feet, PAL, 1.4);
      }
   }

   static boolean continentalBreaker(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      int lv = Math.max(1, u.charge());
      float yaw = p.getYaw();
      Vec3d g = ground(level, p.getEntityPos());
      Vfx.heavyImpact(level, g, PAL, 0.9 + 0.3 * lv);
      Vfx.slashArc(level, GearKit.eye(p).add(0.0, -0.3, 0.0), yaw, 20.0F, 90.0F, 3.2, PAL, 180.0F);
      dust(level, g, 40, 1.5);
      GearKit.kick(level, p, 0.4 + 0.15 * lv);

      for (LivingEntity e : u.enemies(g, 3.0)) {
         u.strike(e, 10 + 4 * lv);
         if (!heavy(e)) {
            launch(e, 0.6);
         }
      }

      int n = 2 + 2 * lv;
      double len = 5 + 2 * lv;
      Set<LivingEntity> seen = new HashSet<>();

      for (int i = 0; i < n; i++) {
         Vec3d d = yawDir(yaw + i * 360.0F / n);
         Vfx.telegraphLine(level, g, d, len, 1.6, 6, PAL);
         erupt(u, g, d, len, 1.5, 1.1, 6, 2.0 + 0.2 * lv, 0.0, 0.55, seen, e -> {
            rehit(e);
            u.strike(e, 12 + 5 * lv);
            if (heavy(e)) {
               slow(e, p, 0, 20);
            } else {
               launch(e, 0.7);
               slow(e, p, 1, 40);
            }

            Vfx.impact(level, center(e), PAL, 0.6);
            u.resonate(2.0F);
         });
      }

      later(level, 6, () -> soundAt(level, g, 1.3F, 0.8F, "weapon.tyrants_fang.erupt", "boss.grothmaw.bone_quake.hit"));
      if (lv >= 3) {
         later(level, 16, () -> primordialNova(u, g));
      }

      return true;
   }

   private static void primordialNova(GearUse u, Vec3d g) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      if (alive(p, level)) {
         Vfx.nova(level, g, PAL, 10.0);
         Vfx.radialFlash(level, g, 16.0, PAL.glow(), 0.6F);
         Vfx.groundCrack(level, g, 10.0, PAL);

         for (LivingEntity e : u.enemies(g, 10.0)) {
            rehit(e);
            u.magic(e, 8.0F);
            if (!heavy(e)) {
               GearKit.knockAway(e, g, 0.8, 0.2);
            }
         }

         soundAt(level, g, 1.2F, 0.7F, "weapon.tyrants_fang.release", "weapon.tyrants_fang.erupt", "boss.grothmaw.bone_quake.hit");
      }
   }

   static void aftershock(ServerWorld level, LivingEntity a, LivingEntity t, ItemStack stack) {
      if (a instanceof PlayerEntity p) {
         if (!heavy(t)) {
            GearKit.knockAway(t, a.getEntityPos(), 0.9, 0.25);
         }

         Vfx.impact(level, center(t), PAL, 0.45);
         long now = now(level);
         GrothmawArsenal.Shock s = AFTERSHOCK.computeIfAbsent(t, k -> new GrothmawArsenal.Shock());
         if (now - s.last > 80L) {
            s.count = 0;
         }

         s.count++;
         s.last = now;
         Vec3d g = ground(level, t.getEntityPos());
         if (s.count < 3) {
            Vfx.ring(level, g, PAL.glow(), 0.6 + 0.5 * s.count, 8);
         } else {
            s.count = 0;
            List<LivingEntity> shaken = new ArrayList<>(GearKit.enemies(level, p, g, 2.5));
            if (t.isAlive() && !shaken.contains(t)) {
               shaken.add(t);
            }

            for (LivingEntity e : shaken) {
               rehit(e);
               GearKit.magic(level, p, e, 7.0F);
               if (!heavy(e)) {
                  launch(e, 0.45);
               }
            }

            Vfx.groundCrack(level, g, 2.5, PAL);
            Vfx.shockwave(level, g, PAL, 2.5);
            Vfx.debris(level, g, Vfx.Debris.BONE, 15260864, 8, 0.35);
            Vfx.pillar(level, g, PAL, 1.4, 0.4);
            dust(level, g, 16, 0.8);
            GearKit.procSound(level, t, "tyrants_fang");
         }
      }
   }

   static boolean pounce(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      double range = 14 + 5 * u.charge();
      int chain = u.empowered() ? 2 : 0;
      Set<LivingEntity> raked = new HashSet<>();
      LivingEntity target = GearKit.lookTarget(level, p, range);
      if (target != null) {
         leap(u, target, raked, chain, true);
         return true;
      } else {
         Vec3d feet = p.getEntityPos();
         Vec3d dir = GearKit.flatLook(p);
         GearKit.dash(p, 1.4, 0.45);
         Vfx.dashTrail(level, feet.add(0.0, 0.8, 0.0), feet.add(dir.multiply(7.0)).add(0.0, 0.8, 0.0), PAL);
         Vfx.streaks(level, feet.add(0.0, 1.0, 0.0), dir, PAL, 8);
         dust(level, feet, 12, 0.4);
         track(level, 1, t -> {
            if (!alive(p, level)) {
               return false;
            }

            p.onLanding();
            if (t >= 1) {
               LivingEntity hit = closest(u.enemies(p.getEntityPos(), 4.0), p, raked, 1.8);
               if (hit != null) {
                  rake(u, hit, true, raked);
                  afterRake(u, raked, chain);
                  return false;
               }
            }

            if (t >= 13) {
               cushion(level, p);
               return false;
            } else {
               return true;
            }
         });
         return true;
      }
   }

   private static void leap(GearUse u, LivingEntity target, Set<LivingEntity> raked, int chain, boolean first) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      Vec3d feet = p.getEntityPos();
      Vec3d to = target.getEntityPos().subtract(feet);
      double d = Math.sqrt(to.x * to.x + to.z * to.z);
      Vec3d dir = d < 0.001 ? GearKit.flatLook(p) : new Vec3d(to.x / d, 0.0, to.z / d);
      double speed = Math.min(1.9, d / 11.0);
      double up = Math.min(0.7, 0.42 + 0.02 * d);
      p.setVelocity(dir.x * speed, up, dir.z * speed);
      p.knockedBack = true;
      Vfx.dashTrail(level, feet.add(0.0, 0.8, 0.0), target.getEntityPos().add(0.0, 0.8, 0.0), PAL);
      Vfx.streaks(level, feet.add(0.0, 1.0, 0.0), dir, PAL, 8);
      dust(level, feet, 12, 0.4);
      if (!first) {
         GearKit.sound(level, p, 0.8F, 1.15F, "weapon.raptor_talons.use");
      }

      track(level, 1, t -> {
         if (!alive(p, level)) {
            return false;
         }

         p.onLanding();
         if (target.isAlive() && t < 24) {
            double gap = flatGap(p.getEntityPos(), target);
            Box box = target.getBoundingBox();
            boolean aligned = p.getY() < box.maxY + 1.5 && p.getY() + p.getHeight() > box.minY - 1.0;
            if ((!(gap < 2.0) || !aligned) && (t < 3 || !p.isOnGround() || !(gap < 3.0))) {
               return true;
            }

            rake(u, target, first, raked);
            afterRake(u, raked, chain);
            return false;
         } else {
            cushion(level, p);
            if (!first) {
               finale(u);
            }

            return false;
         }
      });
   }

   private static void rake(GearUse u, LivingEntity t, boolean first, Set<LivingEntity> raked) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      boolean full = first && u.fullyCharged();
      raked.add(t);
      rehit(t);
      u.strike(t, first ? 9.0F : 7.0F);
      addRend(level, p, t, full ? 5 : (first ? 2 : 1));
      u.resonate(4.0F);
      Vec3d c = center(t);
      float yaw = p.getYaw();
      Vfx.claw(level, c, yaw, 20.0F, 2.2, PAL);
      Vfx.impact(level, c, Vfx.Palette.BLOOD, 0.6);
      Vfx.slashArc(level, c, yaw, 0.0F, -35.0F, 1.6, PAL, 120.0F);
      if (full) {
         Vfx.glint(level, c.add(0.0, 0.5, 0.0), 16773808, 1.6);
         Vfx.radialFlash(level, c, 8.0, PAL.glow(), 0.35F);
      }

      Vec3d away = flat(t.getEntityPos().subtract(p.getEntityPos()));
      p.setVelocity(-away.x * 0.3, 0.35, -away.z * 0.3);
      p.knockedBack = true;
      soundAt(level, c, 1.1F, 1.0F + level.getRandom().nextFloat() * 0.1F, "weapon.raptor_talons.rake", "weapon.raptor_talons.use");
   }

   private static void afterRake(GearUse u, Set<LivingEntity> raked, int chain) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      cushion(level, p);
      if (chain <= 0) {
         if (u.empowered()) {
            later(level, 3, () -> finale(u));
         }
      } else {
         later(level, 4, () -> {
            if (alive(p, level)) {
               LivingEntity next = null;
               double best = Double.MAX_VALUE;

               for (LivingEntity e : u.enemies(p.getEntityPos(), 8.0)) {
                  double d = e.squaredDistanceTo(p);
                  if (!raked.contains(e) && d < best && p.canSee(e)) {
                     best = d;
                     next = e;
                  }
               }

               if (next == null) {
                  finale(u);
               } else {
                  leap(u, next, raked, chain - 1, false);
               }
            }
         });
      }
   }

   private static void finale(GearUse u) {
      if (alive(u.player(), u.level())) {
         Vfx.auraRing(u.level(), u.player().getEntityPos(), PAL, 1.0);
      }
   }

   private static @Nullable LivingEntity closest(List<LivingEntity> near, PlayerEntity p, Set<LivingEntity> skip, double reach) {
      LivingEntity best = null;
      double bestGap = reach;
      Box me = p.getBoundingBox();

      for (LivingEntity e : near) {
         double gap = flatGap(p.getEntityPos(), e);
         Box b = e.getBoundingBox();
         if (!skip.contains(e) && gap <= bestGap && b.maxY > me.minY - 1.0 && b.minY < me.maxY + 1.0) {
            best = e;
            bestGap = gap;
         }
      }

      return best;
   }

   static void talonLevel(ServerWorld level, PlayerEntity p, WeaponKit kit, int c, boolean max) {
      Vec3d feet = p.getEntityPos();
      Vec3d tip = GearKit.tip(p);
      Vfx.streaks(level, feet.add(0.0, 0.3, 0.0), GearKit.flatLook(p).multiply(-1.0), PAL, 4);
      Vfx.ring(level, feet, PAL.glow(), 0.9 + 0.4 * c, 8);
      Vfx.flare(level, tip, PAL.glow(), 0.8 + 0.3 * c, 5);
      dust(level, feet, 4 + 3 * c, 0.4);
      if (max) {
         Vfx.glint(level, tip, 16773808, 1.4);
         Vfx.claw(level, tip, p.getYaw(), p.getPitch(), 0.9, PAL);
      }
   }

   static boolean packHunt(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      float yaw = p.getYaw();
      Vec3d feet = p.getEntityPos();
      Vec3d start = ground(level, feet.add(GearKit.flatLook(p).multiply(0.8)));
      float[] fan = u.empowered() ? new float[]{-25.0F, -12.5F, 0.0F, 12.5F, 25.0F} : new float[]{-15.0F, 0.0F, 15.0F};
      Set<LivingEntity> out = new HashSet<>();
      Set<LivingEntity> back = u.empowered() ? new HashSet<>() : null;

      for (float off : fan) {
         raptor(u, start, yawDir(yaw + off), false, out, back);
      }

      Vfx.auraRing(level, feet, PAL, 1.4);
      Vfx.wisps(level, feet.add(0.0, 1.0, 0.0), PAL, 3, 1.0, 10);
      return true;
   }

   private static void raptor(GearUse u, Vec3d start, Vec3d dir, boolean home, Set<LivingEntity> seen, @Nullable Set<LivingEntity> back) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      Vec3d[] at = new Vec3d[]{start, start};
      Vec3d[] heading = new Vec3d[]{dir};
      track(level, 1, t -> {
         if (!alive(p, level)) {
            return false;
         }

         Vec3d head = at[0];
         if (home) {
            if (flatDist(head, p.getEntityPos()) < 1.5) {
               fade(level, head, true);
               return false;
            }

            heading[0] = flat(p.getEntityPos().subtract(head));
         }

         Vec3d d = heading[0];
         Vec3d next = ground(level, head.add(d.multiply(0.9)));
         boolean blocked = next.y - head.y > 1.25 || solid(level, next.add(0.0, 0.8, 0.0));
         if (!blocked) {
            head = next;
            at[0] = next;
         }

         float runYaw = yawOf(d);
         Vfx.flare(level, head.add(0.0, 0.9, 0.0), PAL.glow(), 0.9, 3);
         Vfx.trail(level, head.add(0.0, 0.6, 0.0), PAL, 0.9);
         if (t % 2 == 1) {
            Vfx.dashTrail(level, at[1].add(0.0, 0.6, 0.0), head.add(0.0, 0.6, 0.0), PAL);
            at[1] = head;
         }

         if (t % 3 == 0) {
            dust(level, head, 3, 0.2);
         }

         for (LivingEntity e : u.enemies(head.add(0.0, 0.5, 0.0), 3.5)) {
            if (flatGap(head, e) <= 1.2 && Math.abs(e.getY() - head.y) < 2.0 && seen.add(e)) {
               u.magic(e, 6.0F);
               addRend(level, p, e, 2);
               Vfx.claw(level, center(e), runYaw, 0.0F, 1.6, PAL);
               GearKit.hitSpark(level, e, PAL);
               u.resonate(2.0F);
            }
         }

         if (!blocked && t < 13) {
            return true;
         }

         fade(level, head, home);
         if (back != null) {
            raptor(u, head, d.multiply(-1.0), true, back, null);
         }

         return false;
      });
   }

   private static void fade(ServerWorld level, Vec3d at, boolean beside) {
      Vfx.glint(level, at.add(0.0, 1.0, 0.0), PAL.core(), 1.0);
      if (beside) {
         Vfx.flare(level, at.add(0.0, 0.8, 0.0), PAL.glow(), 1.0, 5);
      } else {
         Vfx.impact(level, at.add(0.0, 0.6, 0.0), PAL, 0.5);
         Vfx.debris(level, at, Vfx.Debris.BONE, 15260864, 5, 0.3);
      }
   }

   static void rend(ServerWorld level, LivingEntity a, LivingEntity t, ItemStack stack) {
      if (a instanceof PlayerEntity p) {
         GearKit.hitSpark(level, t, PAL);
         Vfx.slash(level, center(t), a.getYaw(), 14688314, 0.8);
         addRend(level, p, t, 1);
      }
   }

   private static void addRend(ServerWorld level, PlayerEntity p, LivingEntity t, int stacks) {
      if (t.isAlive()) {
         long now = now(level);
         GrothmawArsenal.Rend r = REND.computeIfAbsent(t, k -> new GrothmawArsenal.Rend());
         if (now >= r.lock) {
            if (now - r.last > 100L) {
               r.count = 0;
            }

            if (r.count == 0) {
               r.bleed = now + 20L;
            }

            r.owner = p;
            r.last = now;
            r.count = Math.min(5, r.count + stacks);
            if (r.count >= 5) {
               eviscerate(level, p, t, r, now);
            }
         }
      }
   }

   private static void eviscerate(ServerWorld level, PlayerEntity p, LivingEntity t, GrothmawArsenal.Rend r, long now) {
      r.count = 0;
      r.lock = now + 40L;
      Vec3d c = center(t);
      float yaw = p.getYaw();
      rehit(t);
      GearKit.magic(level, p, t, 8.0F);
      p.heal(1.5F);
      Vfx.claw(level, c, yaw, 0.0F, 2.4, Vfx.Palette.BLOOD);
      Vfx.slashArc(level, c, yaw, 0.0F, 40.0F, 1.8, Vfx.Palette.BLOOD, 150.0F);
      Vfx.impact(level, c, Vfx.Palette.BLOOD, 0.7);
      Vfx.debris(level, c, Vfx.Debris.BONE, 15260864, 6, 0.3);
      GearKit.procSound(level, t, "raptor_talons");
   }

   static boolean reaversSpin(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      int lv = u.charge();
      double r = 4.5 + 0.5 * lv;
      int n = 1 + lv + (u.empowered() ? 1 : 0);
      float yaw = p.getYaw();
      int lead = u.fullyCharged() ? 3 : 0;
      if (u.fullyCharged()) {
         Vec3d feet = p.getEntityPos();

         for (LivingEntity e : u.enemies(feet, 8.0)) {
            if (!heavy(e)) {
               pull(e, feet, 0.9, 0.1);
            }
         }

         Vfx.vortex(level, ground(level, feet), r, 2.6, PAL, 5 * n + lead);
         Vfx.gather(level, feet.add(0.0, 1.0, 0.0), PAL, 4.0, 16);
      }

      for (int k = 0; k < n; k++) {
         int rev = k;
         if (lead + 5 * k == 0) {
            spinRev(u, 0, n, r, yaw);
         } else {
            later(level, lead + 5 * k, () -> spinRev(u, rev, n, r, yaw));
         }
      }

      return true;
   }

   private static void spinRev(GearUse u, int k, int n, double r, float yaw) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      if (alive(p, level)) {
         Vec3d feet = p.getEntityPos();
         boolean last = k == n - 1;

         for (LivingEntity e : u.enemies(feet, r + 2.0)) {
            if (!(flatDist(feet, e.getEntityPos()) > r + e.getWidth() / 2.0F) && !(Math.abs(e.getY() - feet.y) >= 2.5)) {
               if (k >= 1) {
                  rehit(e);
               }

               u.strike(e, k == 0 ? 12.0F : 4.2F);
               if (k == 0) {
                  addSap(level, p, e);
                  u.resonate(2.0F);
               }

               if (!heavy(e)) {
                  if (last) {
                     GearKit.knockAway(e, feet, 1.0, 0.3);
                  } else {
                     pull(e, feet, 0.25, 0.0);
                  }
               }

               Vec3d c = center(e);
               Vfx.impact(level, c, PAL, 0.5);
               Vfx.debris(level, c, Vfx.Debris.BONE, 15260864, 3, 0.3);
            }
         }

         Vfx.slashArc(level, feet.add(0.0, 1.1, 0.0), yaw + 180 * k, 0.0F, 0.0F, r, PAL, 340.0F);
         Vfx.slashArc(level, feet.add(0.0, 0.7, 0.0), yaw + 90.0F + 180 * k, 0.0F, 8.0F, r - 1.0, PAL, -330.0F);
         if (k == 0 || last) {
            Vec3d g = ground(level, feet);
            Vfx.shockwave(level, g, PAL, r);
            dustRing(level, g, r * 0.8, 10);
         }

         GearKit.kick(level, p, 0.25);
         if (k >= 1) {
            GearKit.sound(level, p, 0.7F, 1.1F + 0.1F * k, "weapon.fossil_greataxe.use");
         }

         if (last && u.empowered()) {
            amberRing(u, ground(level, feet), r);
         }
      }
   }

   private static void amberRing(GearUse u, Vec3d g, double r) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      Vfx.groundCrack(level, g, r, PAL);
      every(level, 1, 5, 12, i -> {
         Vfx.ring(level, g, PAL.glow(), r, 6);

         for (int j = 0; j < 6; j++) {
            double a = (j + i * 0.5) * Math.PI / 3.0;
            Vfx.embers(level, g.add(Math.cos(a) * r, 0.2, Math.sin(a) * r), PAL.glow(), 2, 0.3);
         }

         if (i % 2 == 1 && alive(p, level)) {
            for (LivingEntity e : u.enemies(g, r + 1.5)) {
               double d = flatDist(g, e.getEntityPos());
               if (d >= r - 1.2 && d <= r + 0.6 && Math.abs(e.getY() - g.y) < 2.0) {
                  e.setOnFireFor(3.0F);
                  u.magic(e, 2.0F);
               }
            }
         }
      });
   }

   static void axeLevel(ServerWorld level, PlayerEntity p, WeaponKit kit, int c, boolean max) {
      Vec3d feet = p.getEntityPos();
      float yaw = p.getYaw();
      Vfx.ring(level, feet, PAL.glow(), 1.5 + c, 10);
      Vfx.slashArc(level, feet.add(0.0, 1.0, 0.0), yaw, 0.0F, 0.0F, 1.5 + 0.5 * c, PAL, 90.0F);
      Vfx.streaks(level, feet.add(0.0, 1.0, 0.0), yawDir(yaw + 90.0F), PAL, 3 + c);
      if (max) {
         Vfx.vortex(level, ground(level, feet), 2.5, 2.0, PAL, 12);
         Vfx.glint(level, GearKit.tip(p), 16773808, 1.3);
      }
   }

   static boolean amberTomb(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      float yaw = p.getYaw();
      Vec3d feet = p.getEntityPos();
      boolean emp = u.empowered();
      Vec3d start = ground(level, feet.add(GearKit.flatLook(p).multiply(1.2)));
      Vfx.slashArc(level, GearKit.eye(p).add(0.0, -0.3, 0.0), yaw, 25.0F, 90.0F, 3.2, PAL, 170.0F);
      Vfx.heavyImpact(level, start, PAL, 0.8);
      dust(level, start, 24, 0.6);
      GearKit.kick(level, p, 0.35);
      float shatter = 12.0F * u.power();
      Set<LivingEntity> seen = new HashSet<>();
      float[] fan = emp ? new float[]{0.0F, -25.0F, 25.0F} : new float[]{0.0F};

      for (float off : fan) {
         Vec3d d = yawDir(yaw + off);
         Vec3d from = ground(level, feet.add(d.multiply(1.2)));
         Vfx.telegraphLine(level, from, d, 12.0, 1.8, 8, PAL);
         every(level, 1, 1, 8, s -> {
            if (alive(p, level)) {
               int t = s + 1;
               Vec3d q = ground(level, from.add(d.multiply(1.5 * t)));
               if (t % 2 == 0) {
                  Vfx.groundCrack(level, q, 1.1, PAL);
               }

               Vfx.embers(level, q, PAL.glow(), 4, 0.4);
               Vfx.shards(level, q, 14721088, 3, 0.25);
               Vec3d a = from.add(d.multiply(1.5 * s));

               for (LivingEntity e : GearKit.alongLine(level, p, new Vec3d(a.x, q.y + 0.8, a.z), d, 1.5, 1.4)) {
                  if (seen.add(e)) {
                     u.strike(e, 8.0F);
                     entomb(level, p, e, 50, shatter, emp ? 3.0 : 0.0);
                     u.resonate(3.0F);
                  }
               }
            }
         });
      }

      return true;
   }

   private static void entomb(ServerWorld level, PlayerEntity owner, LivingEntity e, int ticks, float damage, double burst) {
      if (e.isAlive()) {
         long now = now(level);
         GrothmawArsenal.Tomb tb = new GrothmawArsenal.Tomb();
         tb.start = now;
         tb.until = now + ticks;
         tb.damage = damage;
         tb.burst = burst;
         tb.owner = owner;
         TOMBS.put(e, tb);
         Vec3d c = center(e);
         if (heavy(e)) {
            slow(e, owner, 2, 20);
            bubble(level, e, 10);
         } else {
            pin(e, now);
            bubble(level, e, Math.min(20, ticks));
         }

         Vfx.shards(level, c, 16762976, 10, 0.3);
         Vfx.glint(level, c.add(0.0, 0.5, 0.0), PAL.core(), 1.2);
      }
   }

   private static void bubble(ServerWorld level, LivingEntity e, int life) {
      double radius = Math.max(0.8, Math.max(e.getWidth() * 0.8, e.getHeight() * 0.55) + 0.3);
      Vfx.shieldBubble(level, center(e), radius, PAL, life);
   }

   private static void pin(LivingEntity e, long now) {
      Vec3d v = e.getVelocity();
      if (v.x * v.x + v.z * v.z > 1.0E-4 || v.y > 0.0) {
         e.setVelocity(0.0, Math.min(0.0, v.y), 0.0);
         e.knockedBack = true;
      }

      if (e instanceof MobEntity m) {
         m.getNavigation().stop();
      }

      if (now % 10L == 0L || !e.hasStatusEffect(StatusEffects.SLOWNESS)) {
         e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 15, 5, true, false));
         e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 15, 0, true, false));
      }
   }

   private static void shatter(LivingEntity e, GrothmawArsenal.Tomb tb) {
      if (e.isAlive() && e.getEntityWorld() instanceof ServerWorld level) {
         Vec3d var7 = center(e);
         StatusEffectInstance held = e.getStatusEffect(StatusEffects.SLOWNESS);
         if (held != null && held.getAmplifier() == 5) {
            e.removeStatusEffect(StatusEffects.SLOWNESS);
         }

         rehit(e);
         GearKit.magic(level, tb.owner, e, tb.damage);
         Vfx.shards(level, var7, 16762976, 18, 0.55);
         if (tb.burst > 0.0) {
            Vfx.explosion(level, var7, PAL, 2.2);

            for (LivingEntity o : GearKit.enemies(level, tb.owner, var7, tb.burst)) {
               if (o != e) {
                  GearKit.magic(level, tb.owner, o, tb.damage * 0.5F);
               }
            }
         } else {
            Vfx.impact(level, var7, PAL, 0.9);
         }

         Vfx.debris(level, var7, Vfx.Debris.BONE, 15260864, 8, 0.4);
         Vfx.glint(level, var7, 16773808, 1.6);
         soundAt(level, var7, 1.1F, 1.1F + level.getRandom().nextFloat() * 0.15F, "weapon.fossil_greataxe.shatter", "weapon.fossil_greataxe.proc");
      }
   }

   static void amberSap(ServerWorld level, LivingEntity a, LivingEntity t, ItemStack stack) {
      if (a instanceof PlayerEntity p) {
         GrothmawArsenal.Tomb tb = TOMBS.remove(t);
         if (tb != null) {
            shatter(t, tb);
         } else {
            addSap(level, p, t);
         }
      }
   }

   private static void addSap(ServerWorld level, PlayerEntity p, LivingEntity t) {
      if (t.isAlive() && !TOMBS.containsKey(t)) {
         long now = now(level);
         GrothmawArsenal.Sap s = SAP.computeIfAbsent(t, k -> new GrothmawArsenal.Sap());
         if (now - s.last > 120L) {
            s.count = 0;
         }

         s.count++;
         s.last = now;
         Vec3d c = center(t);
         Vfx.embers(level, c, PAL.glow(), 2 + 2 * s.count, 0.35);
         Vfx.flare(level, c, 14721088, 0.5 + 0.15 * s.count, 4);
         if (s.count >= 2) {
            slow(t, p, 0, 60);
         }

         if (s.count >= 4) {
            s.count = 0;
            entomb(level, p, t, 24, 10.0F, 0.0);
            GearKit.procSound(level, t, "fossil_greataxe");
         }
      }
   }

   static void bonePlating(ServerWorld level, PlayerEntity p, int pieces) {
      long now = now(level);
      GrothmawArsenal.Plate pl = PLATE.computeIfAbsent(p.getUuid(), id -> new GrothmawArsenal.Plate());
      pl.seen = now;
      EntityAttributeInstance max = p.getAttributeInstance(EntityAttributes.MAX_ABSORPTION);
      if (max != null && !max.hasModifier(PLATING)) {
         max.addTemporaryModifier(new EntityAttributeModifier(PLATING, 4.0, Operation.ADD_VALUE));
      }

      GearState state = GearState.of(p);
      if (state.ready("grothmaw.plating")) {
         state.cooldown("grothmaw.plating", 240);
         if (p.getAbsorptionAmount() < 4.0F) {
            p.setAbsorptionAmount(4.0F);
            pl.up = true;
            Vfx.auraRing(level, p.getEntityPos(), PAL, 0.9);
            Vfx.debris(level, p.getEntityPos().add(0.0, 1.0, 0.0), Vfx.Debris.BONE, 15260864, 6, 0.15);
            Vfx.glint(level, p.getEntityPos().add(0.0, 1.2, 0.0), 16773808, 0.9);
         }
      }
   }

   static void furyTick(ServerWorld level, PlayerEntity p, int pieces) {
      long now = now(level);
      if (now % 20L == 0L) {
         GrothmawArsenal.Fury f = FURY.get(p.getUuid());
         if (f != null) {
            if (now - f.lastHurt > 120L && f.value > 0.0F) {
               f.value = Math.max(0.0F, f.value - 2.0F);
            }

            if (f.value >= 8.0F) {
               Vfx.embers(level, p.getEntityPos().add(0.0, 1.0, 0.0), PAL.glow(), (int)(f.value / 4.0F), 0.4);
            }
         }
      }
   }

   static void hideHurt(ServerWorld level, PlayerEntity p, @Nullable LivingEntity attacker, DamageSource source, float taken, int pieces) {
      GrothmawArsenal.Plate pl = PLATE.get(p.getUuid());
      if (pl != null && pl.up && p.getAbsorptionAmount() <= 0.0F) {
         pl.up = false;
         Vec3d c = center(p);
         Vfx.shards(level, c, 15260864, 10, 0.4);
         Vfx.debris(level, c, Vfx.Debris.BONE, 15260864, 10, 0.45);
         Vfx.flare(level, c, PAL.glow(), 1.2, 5);
         GearKit.sound(level, p, 0.8F, 1.3F, "weapon.tyrants_fang.proc");
         GearState.of(p).cooldown("grothmaw.plating", 240);
      }

      if (pieces >= 4 && !(taken <= 0.0F)) {
         long now = now(level);
         GrothmawArsenal.Fury f = FURY.computeIfAbsent(p.getUuid(), id -> new GrothmawArsenal.Fury());
         f.value = Math.min(24.0F, f.value + taken);
         f.lastHurt = now;
         GearState state = GearState.of(p);
         if ((f.value >= 24.0F || p.getHealth() < 0.35F * p.getMaxHealth()) && state.ready("grothmaw.rage_cd")) {
            state.cooldown("grothmaw.rage_cd", 600);
            f.value = 0.0F;
            rage(level, p, 160);
            rageBurst(level, p);
         }
      }
   }

   private static void rage(ServerWorld level, PlayerEntity p, int ticks) {
      long now = now(level);
      long until = Math.max(RAGE.getOrDefault(p.getUuid(), 0L), now + ticks);
      RAGE.put(p.getUuid(), until);
      int left = (int)(until - now);
      p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, left, 0));
      p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, left, 0));
      p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, left, 0));
   }

   private static void rageBurst(ServerWorld level, PlayerEntity p) {
      Vec3d feet = p.getEntityPos();
      Vfx.nova(level, feet.add(0.0, 1.0, 0.0), PAL, 4.0);
      Vfx.pillar(level, feet, PAL, 3.2, 0.9);
      Vfx.auraRing(level, feet, PAL, 1.8);
      GearKit.kick(level, p, 0.4);

      for (LivingEntity e : GearKit.enemies(level, p, feet, 5.0)) {
         e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 80, 0), p);
         if (!heavy(e)) {
            GearKit.knockAway(e, feet, 0.8, 0.3);
         }
      }

      GearKit.sound(level, p, 1.0F, 1.35F, "boss.grothmaw.enrage", "armor.tyrant_hide_armor.ability");
   }

   private static void afterDamage(LivingEntity entity, DamageSource source, float base, float taken, boolean blocked) {
      if (!(taken <= 0.0F)
         && source.getAttacker() instanceof PlayerEntity p
         && entity != p
         && source.getSource() == p
         && source.isOf(DamageTypes.PLAYER_ATTACK)
         && p.getEntityWorld() instanceof ServerWorld level) {
         long var11 = now(level);
         Long until = RAGE.get(p.getUuid());
         if (until != null && var11 < until && var11 - LEECH.getOrDefault(p.getUuid(), -100L) >= 10L) {
            LEECH.put(p.getUuid(), var11);
            p.heal(1.0F);
            Vfx.flare(level, center(p), PAL.glow(), 0.8, 4);
            Vfx.embers(level, center(entity), PAL.glow(), 3, 0.3);
         }
      }
   }

   static boolean extinctionRoar(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      float yaw = p.getYaw();
      Vec3d feet = p.getEntityPos();
      Vec3d g = ground(level, feet);
      Vec3d eye = GearKit.eye(p);
      Vec3d look = p.getRotationVector();
      Vfx.telegraphCone(level, g, yaw, 45.0F, 12.0, 4, PAL);
      Vfx.streaks(level, eye, look, PAL, 14);
      Vfx.nova(level, eye.add(look.multiply(1.2)), PAL, 2.5);
      Vfx.radialFlash(level, feet, 10.0, PAL.glow(), 0.4F);
      Vfx.shake(level, feet, 14.0, 0.5, 14);
      dust(level, g, 30, 1.5);
      GearKit.sound(level, p, 1.3F, 1.3F, "boss.grothmaw.roar", "boss.grothmaw.terror_roar.start");
      later(level, 4, () -> {
         if (alive(p, level)) {
            List<LivingEntity> roared = new ArrayList<>();

            for (LivingEntity e : u.enemies(feet, 14.0)) {
               if (inCone(feet, yaw, 45.0, 12.0, e)) {
                  u.magic(e, 10.0F);
                  boolean heavy = heavy(e);
                  if (!heavy) {
                     GearKit.knockAway(e, feet, 1.4, 0.4);
                  }

                  int terror = heavy ? 40 : 100;
                  e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, terror, 1), p);
                  e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, terror, 1), p);
                  Vfx.impact(level, center(e), PAL, 0.6);
                  roared.add(e);
               }
            }

            roared.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(feet)));

            for (int i = 0; i < 5; i++) {
               LivingEntity target = i < roared.size() ? roared.get(i) : null;
               later(level, 2 + 4 * i, () -> boulder(u, target != null && target.isAlive() ? target.getEntityPos() : conePoint(level, feet, yaw)));
            }
         }
      });
      rage(level, p, 120);
      return true;
   }

   private static Vec3d conePoint(ServerWorld level, Vec3d feet, float yaw) {
      float a = yaw + (level.getRandom().nextFloat() * 2.0F - 1.0F) * 45.0F;
      double d = 4.0 + level.getRandom().nextDouble() * 7.0;
      return ground(level, feet.add(yawDir(a).multiply(d)));
   }

   private static void boulder(GearUse u, Vec3d at) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      if (alive(p, level)) {
         Vec3d g = ground(level, at);
         BlockState rock = level.getBlockState(BlockPos.ofFloored(g.x, g.y - 0.5, g.z));
         BlockStateParticleEffect chunks = new BlockStateParticleEffect(
            ParticleTypes.BLOCK, !rock.isAir() && rock.getFluidState().isEmpty() ? rock : Blocks.COBBLESTONE.getDefaultState()
         );
         Vfx.telegraphCircle(level, g, 2.4, 16, PAL);
         Vfx.smoke(level, g.add(0.0, 14.0, 0.0), PAL.smoke(), 6, 1.0, 2.0);
         every(level, 8, 1, 8, k -> {
            Vec3d q = g.add(0.0, 14 * (8 - k) / 8.0, 0.0);
            Vfx.meteorTrail(level, q, new Vec3d(0.0, -1.75, 0.0), PAL);
            level.spawnParticles(chunks, q.x, q.y, q.z, 4, 0.35, 0.35, 0.35, 0.0);
         });
         later(
            level,
            16,
            () -> {
               if (alive(p, level)) {
                  for (LivingEntity e : u.enemies(g, 4.0)) {
                     if (!(flatDist(g, e.getEntityPos()) > 2.4 + e.getWidth() / 2.0F) && !(Math.abs(e.getY() - g.y) > 3.0)) {
                        rehit(e);
                        u.magic(e, 12.0F);
                        slow(e, p, 0, 40);
                        if (!heavy(e)) {
                           GearKit.knockAway(e, g, 0.6, 0.3);
                        }
                     }
                  }

                  Vfx.heavyImpact(level, g, PAL, 0.9);
                  Vfx.shards(level, g, 7232064, 10, 0.5);
                  level.spawnParticles(chunks, g.x, g.y + 0.3, g.z, 30, 1.0, 0.3, 1.0, 0.2);
                  soundAt(
                     level,
                     g,
                     1.4F,
                     0.9F + level.getRandom().nextFloat() * 0.15F,
                     "boss.grothmaw.boulder_hurl.hit",
                     "weapon.tyrants_fang.slam",
                     "weapon.tyrants_fang.use"
                  );
               }
            }
         );
      }
   }

   private static boolean heavy(LivingEntity e) {
      return e instanceof MysticalBoss b && b.isBoss() || e instanceof EnderDragonEntity || e instanceof WitherEntity;
   }

   private static void rehit(LivingEntity e) {
      e.timeUntilRegen = 0;
   }

   private static boolean alive(PlayerEntity p, ServerWorld level) {
      return p.isAlive() && p.getEntityWorld() == level;
   }

   private static Vec3d ground(ServerWorld level, Vec3d v) {
      Vec3d g = BossFx.ground(level, v.x, v.y, v.z);
      return g == null ? v : g;
   }

   private static Vec3d center(LivingEntity e) {
      return e.getBoundingBox().getCenter();
   }

   private static Vec3d yawDir(float yaw) {
      return Vec3d.fromPolar(0.0F, yaw);
   }

   private static float yawOf(Vec3d d) {
      return (float)(MathHelper.atan2(-d.x, d.z) * 180.0F / (float)Math.PI);
   }

   private static Vec3d flat(Vec3d v) {
      Vec3d f = new Vec3d(v.x, 0.0, v.z);
      return f.lengthSquared() < 1.0E-6 ? Vec3d.ZERO : f.normalize();
   }

   private static double flatDist(Vec3d a, Vec3d b) {
      double dx = a.x - b.x;
      double dz = a.z - b.z;
      return Math.sqrt(dx * dx + dz * dz);
   }

   private static double flatGap(Vec3d at, LivingEntity e) {
      Box b = e.getBoundingBox();
      double dx = Math.max(0.0, Math.max(b.minX - at.x, at.x - b.maxX));
      double dz = Math.max(0.0, Math.max(b.minZ - at.z, at.z - b.maxZ));
      return Math.sqrt(dx * dx + dz * dz);
   }

   private static boolean inCone(Vec3d from, float yaw, double half, double range, LivingEntity e) {
      Vec3d to = e.getEntityPos().subtract(from);
      double d = Math.sqrt(to.x * to.x + to.z * to.z);
      if (d > range + e.getWidth() / 2.0F) {
         return false;
      }

      if (d < 1.2) {
         return true;
      }

      Vec3d f = yawDir(yaw);
      return (to.x * f.x + to.z * f.z) / d >= Math.cos(Math.toRadians(half));
   }

   private static void launch(LivingEntity e, double up) {
      e.addVelocity(0.0, up, 0.0);
      e.knockedBack = true;
   }

   private static void pull(LivingEntity e, Vec3d to, double strength, double lift) {
      Vec3d d = flat(to.subtract(e.getEntityPos()));
      e.addVelocity(d.x * strength, lift, d.z * strength);
      e.knockedBack = true;
   }

   private static void slow(LivingEntity e, PlayerEntity source, int amplifier, int ticks) {
      e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, ticks, amplifier), source);
   }

   private static boolean solid(ServerWorld level, Vec3d v) {
      BlockPos pos = BlockPos.ofFloored(v);
      return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
   }

   private static void cushion(ServerWorld level, PlayerEntity p) {
      track(level, 1, t -> {
         if (!alive(p, level)) {
            return false;
         }

         p.onLanding();
         return t < 40 && (t < 2 || !p.isOnGround());
      });
   }

   private static void dust(ServerWorld level, Vec3d at, int count, double spread) {
      BlockPos pos = BlockPos.ofFloored(at.x, at.y - 0.2, at.z);
      BlockState state = level.getBlockState(pos);
      if (state.isAir()) {
         state = level.getBlockState(pos.down());
      }

      if (!state.isAir() && state.getFluidState().isEmpty()) {
         level.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, state), at.x, at.y + 0.15, at.z, count, spread, 0.1, spread, 0.15);
      }
   }

   private static void dustRing(ServerWorld level, Vec3d g, double r, int points) {
      for (int j = 0; j < points; j++) {
         double a = j * Math.PI * 2.0 / points;
         dust(level, g.add(Math.cos(a) * r, 0.0, Math.sin(a) * r), 2, 0.15);
      }
   }

   private static void soundAt(ServerWorld level, Vec3d at, float volume, float pitch, String... keys) {
      for (String key : keys) {
         SoundEvent e = BossSounds.event(key);
         if (e != null) {
            level.playSound(null, at.x, at.y, at.z, e, SoundCategory.PLAYERS, volume, pitch);
            return;
         }
      }
   }

   private static final class Fury {
      float value;
      long lastHurt;
   }

   private static final class Plate {
      long seen;
      boolean up;
   }

   private static final class Rend {
      int count;
      long last = -1000L;
      long lock;
      long bleed;
      @Nullable PlayerEntity owner;
   }

   private static final class Sap {
      int count;
      long last = -1000L;
   }

   private static final class Shock {
      int count;
      long last = -1000L;
   }

   private record Task(long due, Runnable run) {
   }

   private static final class Tomb {
      long start;
      long until;
      float damage;
      double burst;
      PlayerEntity owner;
   }
}
