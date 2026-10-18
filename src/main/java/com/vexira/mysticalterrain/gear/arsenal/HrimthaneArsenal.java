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
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStopped;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public final class HrimthaneArsenal extends Arsenal {
   private static final Vfx.Palette FROST = Vfx.Palette.FROST;
   private static final int CORE = FROST.core();
   private static final int GLOW = FROST.glow();
   private static final Vfx.Debris SNOW = Vfx.Debris.SNOW;
   private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);
   private static final Vec3 DOWN = new Vec3(0.0, -1.0, 0.0);
   private static final int MAX_RIME = 5;
   private static final int RIME_LIFE = 120;
   private static final String PIERCE = "hrimthane.pierce";
   private static final String CARAPACE_KEY = "hrimthane.carapace";
   private static final String RETORT = "hrimthane.retort";
   private static final Map<LivingEntity, HrimthaneArsenal.Rime> RIME = new WeakHashMap<>();
   private static final List<HrimthaneArsenal.Task> TASKS = new ArrayList<>();
   private static final List<HrimthaneArsenal.Task> PENDING = new ArrayList<>();
   private static final List<HrimthaneArsenal.Shard> SHARDS = new ArrayList<>();
   private static final List<HrimthaneArsenal.Shard> NEW_SHARDS = new ArrayList<>();
   private static final Set<UUID> CARAPACE = new HashSet<>();
   private static boolean hooked;
   private static long clock;

   public HrimthaneArsenal() {
      super("hrimthane", "wyrmscale_armor", Vfx.Palette.FROST);
      if (!hooked) {
         hooked = true;
         ServerTickEvents.END_SERVER_TICK.register(HrimthaneArsenal::tick);
         ServerLifecycleEvents.SERVER_STOPPED.register((ServerStopped)server -> reset());
      }
   }

   @Override
   protected void weapons() {
      this.weapon("rimebreaker_axe", 11.0F, -3.1F)
         .axe()
         .primary(Active.of(160, HrimthaneArsenal::glacierCrush).empowered(60, 1.3F))
         .alternate(Active.of(200, HrimthaneArsenal::tailReaper).empowered(40, 1.25F))
         .charge(Charge.of(3, 10).pose(ItemUseAnimation.TRIDENT).perLevel(0.35F).onLevel(HrimthaneArsenal::crushLevel))
         .passive(HrimthaneArsenal::rimebite)
         .resonancePerHit(12.0F);
      this.weapon("glacial_lance", 9.0F, -2.8F)
         .primary(Active.of(100, HrimthaneArsenal::hoarfrostLance).empowered(40))
         .alternate(Active.of(240, HrimthaneArsenal::frostBurrow).empowered(50))
         .charge(Charge.of(3, 8).pose(ItemUseAnimation.TRIDENT).perLevel(0.4F).onLevel(HrimthaneArsenal::lanceLevel))
         .passive(HrimthaneArsenal::impalingFrost)
         .resonancePerHit(10.0F);
      this.weapon("icicle_repeater", 3.0F, -2.4F)
         .primary(Active.of(80, HrimthaneArsenal::icicleBarrage).empowered(30, 1.0F))
         .alternate(Active.of(280, HrimthaneArsenal::frozenPrison).empowered(50))
         .charge(Charge.of(5, 8).pose(ItemUseAnimation.BOW).cast(Active.of(320, HrimthaneArsenal::wintersCrown).empowered(60)).onLevel(HrimthaneArsenal::crownLevel))
         .passive(HrimthaneArsenal::quickReload)
         .resonancePerHit(8.0F);
   }

   @Override
   protected void armor(ArmorKit set) {
      set.twoPiece(HrimthaneArsenal::rimeCarapace);
      set.fourPiece(HrimthaneArsenal::permafrost);
      set.allowDamage((level, p, source, amount, pieces) -> pieces < 2 || !source.is(DamageTypeTags.IS_FREEZING));
      set.onHurt(HrimthaneArsenal::wyrmscaleHurt);
      set.active(600, HrimthaneArsenal::absoluteZero);
   }

   static boolean glacierCrush(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      int c = u.charge();
      float pw = u.power();
      double r = 3.5 + 0.5 * c;
      Vec3 at = ground(level, p.position().add(GearKit.flatLook(p).scale(2.2)));
      Vfx.slashArc(level, p.position().add(0.0, 1.4, 0.0), p.getYRot(), 20.0F, 90.0F, 3.0, FROST, 150.0F);
      List<LivingEntity> hits = foes(level, p, at, r);
      Set<LivingEntity> primed = primed(level, hits);

      for (LivingEntity e : hits) {
         if (primed.contains(e)) {
            shatter(level, p, e, pw);
         }

         deal(level, p, e, 16.0F * pw, true);
         rime(level, p, e, 2 + c);
         GearKit.knockAway(e, at, 0.4, 0.55);
         u.resonate(3.0F);
      }

      Vfx.heavyImpact(level, at, FROST, 1.0 + 0.25 * c);
      Vfx.groundCrack(level, at, r, FROST);
      Vfx.shockwave(level, at, FROST, r);
      spikeRing(level, at, r, 8 + 2 * c, 1.6 + 0.3 * c, 0.35, true);
      Vfx.debris(level, at.add(0.0, 1.0, 0.0), SNOW, CORE, 12 + 4 * c, 0.5);
      GearKit.kick(level, p, 0.35 + 0.1 * c);
      Vfx.telegraphCircle(level, at, r + 2.5, 6, FROST);
      later(level, p, 6, () -> {
         for (LivingEntity ex : foes(level, p, at, r + 2.5)) {
            if (flatDist(ex, at) >= r - 0.5) {
               deal(level, p, ex, 8.0F * pw, false);
               rime(level, p, ex, 1);
            }
         }

         Vfx.shockwave(level, at, FROST, r + 2.5);
         spikeRing(level, at, r + 2.0, 10, 1.2, 0.3, false);
      });
      if (u.empowered()) {
         Vfx.telegraphCircle(level, at, 7.0, 12, FROST);
         later(level, p, 12, () -> {
            for (LivingEntity ex : foes(level, p, at, 7.0)) {
               if (flatDist(ex, at) >= r) {
                  deal(level, p, ex, 12.0F * pw, false);
                  rime(level, p, ex, 1);
                  ex.push(0.0, 0.6, 0.0);
                  ex.syncVelocity = true;
               }
            }

            Vfx.nova(level, at, FROST, 7.0);
            Vfx.groundCrack(level, at, 7.0, FROST);
            spikeRing(level, at, 6.5, 14, 2.8, 0.5, false);
            GearKit.kick(level, p, 0.3);
         });
      }

      return true;
   }

   static void crushLevel(ServerLevel level, Player p, WeaponKit w, int lvl, boolean max) {
      Vec3 tip = GearKit.tip(p);
      Vfx.ring(level, p.position().add(0.0, 0.05, 0.0), GLOW, 3.2 - 0.6 * lvl, 8);
      Vfx.flare(level, tip, max ? CORE : GLOW, 0.9 + 0.4 * lvl, 6);
      Vfx.shards(level, tip, CORE, 2 + 2 * lvl, 0.2);
      Vfx.debris(level, p.position().add(0.0, 2.4, 0.0), SNOW, CORE, 3 + 2 * lvl, 0.08);
      if (max) {
         Vfx.runeCircle(level, ground(level, p.position()), 1.6, FROST, 30, 0.05F, Vfx.Rune.GLYPH);
         Vfx.gather(level, tip, FROST, 1.4, 10);
         Vfx.glint(level, tip, CORE, 1.2);
      }
   }

   static boolean tailReaper(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      float pw = u.power();
      float yaw = p.getYRot();
      Vec3 origin = p.position().add(0.0, 1.0, 0.0);
      Vfx.slashArc(level, origin, yaw, 0.0F, 0.0F, 4.5, FROST, 340.0F);
      Vfx.slashArc(level, origin.add(0.0, -0.3, 0.0), yaw + 180.0F, 0.0F, 12.0F, 3.2, FROST, -340.0F);
      Vfx.ring(level, p.position().add(0.0, 0.05, 0.0), GLOW, 4.5, 10);
      Vfx.debris(level, origin, SNOW, CORE, 16, 0.5);
      Vec3 f = GearKit.flatLook(p);

      for (int k = 0; k < 4; k++) {
         Vec3 dir = f.yRot(k * (float) (Math.PI / 2));
         Vfx.streaks(level, origin.add(dir.scale(2.5)), dir.yRot((float) (-Math.PI / 2)), FROST, 4);
      }

      int n = reap(level, p, u, 4.5, pw);
      if (n > 0) {
         p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60, n >= 3 ? 1 : 0, false, false, true));
         Vfx.shieldBubble(level, p.position().add(0.0, 1.0, 0.0), 1.3, FROST, 20);
         Vfx.auraRing(level, p.position(), FROST, 1.2);
      }

      if (u.empowered()) {
         later(level, p, 6, () -> {
            for (LivingEntity e : foes(level, p, p.position(), 7.0)) {
               Vec3 to = p.position().subtract(e.position()).horizontal();
               if (to.lengthSqr() > 1.0E-4) {
                  to = to.normalize();
                  e.push(to.x * 0.7, 0.15, to.z * 0.7);
                  e.syncVelocity = true;
               }
            }

            Vfx.vortex(level, p.position(), 3.0, 2.5, FROST, 10);
         });
         later(level, p, 10, () -> {
            Vec3 o = p.position().add(0.0, 1.0, 0.0);
            Vfx.slashArc(level, o, p.getYRot(), 0.0F, 8.0F, 5.5, FROST, -360.0F);
            Vfx.ring(level, p.position().add(0.0, 0.05, 0.0), GLOW, 5.5, 10);
            Vfx.debris(level, o, SNOW, CORE, 14, 0.55);
            reap(level, p, u, 5.5, pw);
            GearKit.kick(level, p, 0.3);
         });
      }

      return true;
   }

   private static int reap(ServerLevel level, Player p, GearUse u, double reach, float pw) {
      double y = p.getY();
      List<LivingEntity> hits = foes(level, p, p.position().add(0.0, 1.0, 0.0), reach)
         .stream()
         .filter(ex -> ex.getBoundingBox().minY - y < 2.5 && y - ex.getBoundingBox().maxY < 2.5)
         .toList();
      Set<LivingEntity> primed = primed(level, hits);

      for (LivingEntity e : hits) {
         if (primed.contains(e)) {
            shatter(level, p, e, pw);
         }

         deal(level, p, e, 14.0F * pw, true);
         rime(level, p, e, 1);
         GearKit.knockAway(e, p.position(), 0.9, 0.25);
         GearKit.hitSpark(level, e, FROST);
         Vfx.shards(level, center(e), CORE, 6, 0.4);
         u.resonate(2.0F);
      }

      return hits.size();
   }

   static void rimebite(ServerLevel level, LivingEntity a, LivingEntity t, ItemStack s) {
      Player p = a instanceof Player pl ? pl : null;
      boolean broke = p != null && shatter(level, p, t, 1.0F);
      rime(level, p, t, 1, 5);
      GearKit.hitSpark(level, t, FROST);
      Vfx.shards(level, center(t), CORE, 3, 0.25);
      if (!broke) {
         GearKit.procSound(level, t, "rimebreaker_axe");
      }
   }

   static boolean hoarfrostLance(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      int c = u.charge();
      float pw = u.power();
      double range = 18 + 6 * c;
      double width = 0.9 + 0.3 * c;
      Vec3 eye = GearKit.eye(p);
      Vec3 look = p.getLookAngle();
      Vec3 end = clipTo(level, p, eye, look, range);
      double len = end.distanceTo(eye);
      Vec3 tip = GearKit.tip(p);
      Vfx.beam(level, tip, end, FROST, 0.45 + 0.1 * c, 6);
      Vfx.streaks(level, tip, look, FROST, 10);
      GearKit.muzzle(level, p, FROST, 1.2);
      int dots = Math.min(18, (int)(len / 2.0));

      for (int i = 1; i <= dots; i++) {
         Vfx.shards(level, eye.add(look.scale(2.0 * i)), CORE, 1, 0.12);
      }

      Vfx.explosion(level, end, FROST, 1.2 + 0.3 * c);
      Vfx.glint(level, end, CORE, 1.2);
      List<LivingEntity> hits = new ArrayList<>(GearKit.alongLine(level, p, eye, look, len, width));
      hits.sort(Comparator.comparingDouble(ex -> ex.distanceToSqr(eye)));
      Set<LivingEntity> primed = primed(level, hits);

      for (LivingEntity e : hits) {
         if (primed.contains(e)) {
            shatter(level, p, e, pw);
         }

         deal(level, p, e, 14.0F * pw, false);
         rime(level, p, e, 1 + c);
         Vfx.impact(level, center(e), FROST, 0.5);
         u.resonate(3.0F);
         if (e.isAlive() && !e.onGround()) {
            Vec3 v = e.getDeltaMovement();
            e.setDeltaMovement(v.x * 0.3, -1.2, v.z * 0.3);
            e.syncVelocity = true;
            e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 2), p);
            Vfx.streaks(level, center(e), DOWN, FROST, 6);
         }
      }

      if (u.empowered() && !hits.isEmpty()) {
         LivingEntity first = hits.get(0);
         Vec3 from = center(first);
         Set<LivingEntity> done = new HashSet<>(hits);

         for (int side = -1; side <= 1; side += 2) {
            Vec3 dir = Vec3.directionFromRotation(p.getXRot(), p.getYRot() + side * 15);
            Vec3 to = clipTo(level, p, from, dir, 12.0);

            for (LivingEntity e : GearKit.alongLine(level, p, from, dir, to.distanceTo(from), 0.9)) {
               if (done.add(e)) {
                  deal(level, p, e, 10.0F * pw, false);
                  rime(level, p, e, 1);
                  Vfx.impact(level, center(e), FROST, 0.4);
               }
            }

            Vfx.beam(level, from, to, FROST, 0.35, 5);
            Vfx.glint(level, to, CORE, 1.0);
         }
      }

      if (u.fullyCharged()) {
         rimeSpikes(level, p, len, pw);
      }

      return true;
   }

   private static void rimeSpikes(ServerLevel level, Player p, double len, float pw) {
      Vec3 feet = p.position();
      Vec3 flat = GearKit.flatLook(p);
      double reach = Math.min(len, 24.0);
      Vfx.telegraphLine(level, ground(level, feet), flat, reach, 1.4, 5, FROST);
      Set<LivingEntity> struck = new HashSet<>();
      int i = 0;

      for (double d = 1.5; d <= reach; d += 1.5) {
         double dist = d;
         int idx = i++;
         later(level, p, 5 + idx / 3, () -> {
            Vec3 g = ground(level, feet.add(flat.scale(dist)));
            Vfx.pillar(level, g, FROST, 2.2, 0.4);
            if (idx % 2 == 1) {
               Vfx.groundCrack(level, g, 1.2, FROST);
            }

            for (LivingEntity e : foes(level, p, g, 1.5)) {
               if (struck.add(e)) {
                  deal(level, p, e, 8.0F * pw, false);
                  e.push(0.0, 0.7, 0.0);
                  e.syncVelocity = true;
                  rime(level, p, e, 1);
               }
            }
         });
      }
   }

   static void lanceLevel(ServerLevel level, Player p, WeaponKit w, int lvl, boolean max) {
      Vec3 tip = GearKit.tip(p);
      Vfx.telegraphLine(level, ground(level, p.position()), GearKit.flatLook(p), 18 + 6 * lvl, 0.9, 8, FROST);
      Vfx.streaks(level, tip, p.getLookAngle(), FROST, 3 + 2 * lvl);
      Vfx.flare(level, tip, max ? CORE : GLOW, 0.8 + 0.35 * lvl, 6);
      if (max) {
         Vfx.glint(level, tip, CORE, 1.4);
         Vfx.gather(level, tip, FROST, 1.2, 10);
      }
   }

   static boolean frostBurrow(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      Vec3 start = p.position();
      Vec3 g = floor(level, start, 0.6, 3);
      if (g != null && !(start.y - g.y > 3.0)) {
         float pw = u.power();
         boolean emp = u.empowered();
         Vec3 dir = GearKit.flatLook(p);
         Vfx.heavyImpact(level, g, FROST, 0.6);
         Vfx.groundCrack(level, g, 1.8, FROST);
         Vfx.debris(level, start.add(0.0, 0.5, 0.0), SNOW, CORE, 16, 0.5);
         Vfx.telegraphLine(level, g, dir, 15.0, 1.4, 10, FROST);
         p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 14, 4, true, false));
         Set<LivingEntity> spiked = new HashSet<>();
         later(level, p, 1, () -> burrow(level, p, u, dir, pw, emp, 1, start, spiked));
         return true;
      } else {
         return false;
      }
   }

   private static void burrow(ServerLevel level, Player p, GearUse u, Vec3 dir, float pw, boolean emp, int k, Vec3 last, Set<LivingEntity> spiked) {
      Vec3 feet = p.position();
      boolean blocked = k > 1 && p.horizontalCollision || k > 3 && feet.subtract(last).horizontalDistance() < 0.2;
      if (k <= 10 && !blocked && foes(level, p, feet.add(0.0, 0.9, 0.0), 1.6).isEmpty()) {
         Vec3 v = p.getDeltaMovement();
         p.setDeltaMovement(dir.x * 1.5, Math.min(0.0, v.y), dir.z * 1.5);
         p.syncVelocity = true;
         Vfx.smoke(level, feet.add(0.0, 0.3, 0.0), FROST.smoke(), 3, 0.4, 1.2);
         Vfx.trail(level, feet.add(0.0, 0.4, 0.0), FROST, 0.5);
         if (k % 3 == 1) {
            Vfx.groundCrack(level, ground(level, feet), 1.1, FROST);
            Vfx.shards(level, feet.add(0.0, 0.3, 0.0), CORE, 4, 0.35);
         }

         if (emp && k % 2 == 0) {
            later(level, p, 4, () -> {
               Vec3 at = ground(level, feet);
               Vfx.pillar(level, at, FROST, 1.8, 0.3);

               for (LivingEntity e : foes(level, p, at, 1.5)) {
                  if (spiked.add(e)) {
                     deal(level, p, e, 8.0F * pw, false);
                     e.push(0.0, 0.6, 0.0);
                     e.syncVelocity = true;
                     rime(level, p, e, 1);
                  }
               }
            });
         }

         later(level, p, 1, () -> burrow(level, p, u, dir, pw, emp, k + 1, feet, spiked));
      } else {
         erupt(level, p, u, pw, emp);
      }
   }

   private static void erupt(ServerLevel level, Player p, GearUse u, float pw, boolean emp) {
      Vec3 at = p.position();
      Vec3 g = ground(level, at);
      double r = emp ? 5.0 : 3.5;
      List<LivingEntity> hits = foes(level, p, g, r);
      Set<LivingEntity> primed = primed(level, hits);

      for (LivingEntity e : hits) {
         if (primed.contains(e)) {
            shatter(level, p, e, pw);
         }

         deal(level, p, e, 18.0F * pw, true);
         rime(level, p, e, 2);
         e.push(0.0, 0.9, 0.0);
         e.syncVelocity = true;
         u.resonate(4.0F);
      }

      Vec3 v = p.getDeltaMovement();
      p.setDeltaMovement(v.x * 0.3, 0.85, v.z * 0.3);
      p.syncVelocity = true;
      p.resetFallDistance();
      p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 30, 0, true, false));
      Vfx.pillar(level, g, FROST, 6.0, 1.2);
      Vfx.heavyImpact(level, g, FROST, 1.2);
      Vfx.nova(level, at.add(0.0, 0.5, 0.0), FROST, r);
      Vfx.shards(level, at.add(0.0, 1.0, 0.0), CORE, 24, 0.8);
      spikeRing(level, g, 0.7 * r, 6, 2.2, 0.35, false);
      Vfx.debris(level, at.add(0.0, 1.0, 0.0), SNOW, CORE, 20, 0.6);
      GearKit.kick(level, p, 0.4);
      sfx(level, at, "weapon.glacial_lance.erupt", 1.6F, 1.0F);
   }

   static void impalingFrost(ServerLevel level, LivingEntity a, LivingEntity t, ItemStack s) {
      Player p = a instanceof Player pl ? pl : null;
      int before = stacks(level, t);
      rime(level, p, t, 1, 5);
      GearKit.hitSpark(level, t, FROST);
      if (p != null && before >= 3 && GearState.of(p).ready("hrimthane.pierce")) {
         GearState.of(p).cooldown("hrimthane.pierce", 10);
         Vec3 c = center(t);
         Vec3 dir = GearKit.flatLook(p);
         double reach = 4.0F + t.getBbWidth() / 2.0F;
         Vec3 end = c.add(dir.scale(reach));

         for (LivingEntity e : GearKit.alongLine(level, p, c, dir, reach, 0.8)) {
            if (e != t) {
               deal(level, p, e, 6.0F, false);
               rime(level, p, e, 1);
            }
         }

         Vfx.beam(level, c, end, FROST, 0.3, 4);
         Vfx.streaks(level, c, dir, FROST, 8);
         Vfx.shards(level, end, CORE, 6, 0.3);
         GearKit.procSound(level, t, "glacial_lance");
      }
   }

   static boolean icicleBarrage(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      float pw = u.power();
      int rounds = u.empowered() ? 5 : 3;
      int pierce = u.empowered() ? 1 : 0;
      volley(level, p, pw, pierce, 0);

      for (int k = 1; k < rounds; k++) {
         int round = k;
         later(level, p, 3 * k, () -> volley(level, p, pw, pierce, round));
      }

      return true;
   }

   private static void volley(ServerLevel level, Player p, float pw, int pierce, int round) {
      Set<LivingEntity> rimed = new HashSet<>();
      boolean[] sounded = new boolean[]{false};
      RandomSource rnd = level.getRandom();
      Vec3 tip = GearKit.tip(p);

      for (int side = -1; side <= 1; side += 2) {
         float yaw = p.getYRot() + side * 3 + (rnd.nextFloat() - 0.5F) * 2.0F;
         float pitch = p.getXRot() + (rnd.nextFloat() - 0.5F) * 1.2F;
         HrimthaneArsenal.Shard s = shard(level, p, tip, Vec3.directionFromRotation(pitch, yaw).scale(2.4), 14, 0.6);
         s.pierce = pierce;
         s.onHit = (sh, e, at) -> {
            deal(level, p, e, 5.0F * pw, false);
            if (rimed.add(e)) {
               rime(level, p, e, 1);
            }

            Vfx.impact(level, at, FROST, 0.35);
            Vfx.shards(level, at, CORE, 4, 0.3);
            GearState.of(p).addResonance("icicle_repeater", 2.0F);
            if (!sounded[0]) {
               sounded[0] = true;
               sfx(level, at, "bolt.ice.impact", 0.6F, 1.2F);
            }
         };
         s.onEnd = (sh, e, at) -> {
            if (sh.blocked) {
               Vfx.shards(level, at, CORE, 5, 0.25);
               Vfx.glint(level, at, CORE, 0.6);
            }
         };
      }

      GearKit.muzzle(level, p, FROST, 0.8);
      Vfx.streaks(level, tip, p.getLookAngle(), FROST, 4);
      if (round > 0) {
         sfx(level, p.position(), "bolt.ice.launch", 0.6F, 1.35F);
      }
   }

   static boolean frozenPrison(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      float pw = u.power();
      boolean emp = u.empowered();
      HrimthaneArsenal.Shard s = shard(level, p, GearKit.tip(p), p.getLookAngle().scale(1.0), 28, 1.0);
      s.comet = true;
      s.target = GearKit.lookTarget(level, p, 28.0);
      s.homing = s.target != null ? 0.08 : 0.0;
      s.fx = sh -> {
         if (sh.age % 3 == 0) {
            Vfx.flare(level, sh.pos, GLOW, 1.3, 4);
            Vfx.sparks(level, sh.pos, CORE, 2, 0.1, 0.12);
         }
      };
      s.onHit = (sh, e, at) -> prison(level, p, e, at, pw, emp);
      s.onEnd = (sh, e, at) -> prison(level, p, null, at, pw, emp);
      GearKit.muzzle(level, p, FROST, 1.1);
      return true;
   }

   private static void prison(ServerLevel level, Player p, @Nullable LivingEntity struck, Vec3 at, float pw, boolean emp) {
      if (struck != null) {
         freeze(level, p, struck);
      }

      for (LivingEntity e : foes(level, p, at, 3.5)) {
         deal(level, p, e, 8.0F * pw, false);
         if (e != struck) {
            rime(level, p, e, 3);
         }

         GearState.of(p).addResonance("icicle_repeater", 4.0F);
      }

      Vec3 g = ground(level, at);
      Vfx.nova(level, at, FROST, 3.5);
      Vfx.shards(level, at, CORE, 20, 0.7);
      Vfx.groundCrack(level, g, 3.5, FROST);
      Vfx.debris(level, at, SNOW, CORE, 14, 0.4);
      sfx(level, at, "bolt.ice.impact", 1.2F, 0.8F);
      if (emp) {
         Vfx.ward(level, g, 3.5, FROST, 80);
         repeat(level, p, 20, 20, 4, i -> {
            for (LivingEntity ex : foes(level, p, g, 3.5)) {
               rime(level, p, ex, 1);
               ex.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 2), p);
            }

            Vfx.ring(level, g.add(0.0, 0.05, 0.0), GLOW, 3.5, 12);
            Vfx.debris(level, g.add(0.0, 2.0, 0.0), SNOW, CORE, 6, 0.1);
         });
      }
   }

   static boolean wintersCrown(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      LivingEntity main = GearKit.lookTarget(level, p, 40.0);
      if (main == null) {
         main = nearAim(level, p, 32.0, 0.8);
      }

      Vec3 centre = main != null ? main.position() : GearKit.lookPoint(level, p, 40.0);
      List<LivingEntity> pool = new ArrayList<>(GearKit.enemies(level, p, centre, 10.0));
      if (main != null && !pool.contains(main)) {
         pool.add(main);
      }

      if (pool.isEmpty()) {
         return false;
      }

      pool.sort(Comparator.comparingDouble(e -> e.position().distanceToSqr(centre)));
      if (main != null) {
         pool.remove(main);
         pool.add(0, main);
      }

      float pw = u.power();
      boolean emp = u.empowered();
      int n = u.charge() + (emp ? 2 : 0);
      Vec3 crown = crownCentre(p);
      Vfx.flare(level, crown, CORE, 2.5, 8);
      Vfx.ring(level, crown, GLOW, 2.0, 10);
      Vfx.streaks(level, crown, UP, FROST, 8);
      GearKit.kick(level, p, 0.2);
      spear(level, p, 0, pool.get(0), pool, pw, emp);

      for (int k = 1; k < n; k++) {
         int slot = k;
         LivingEntity target = pool.get(k % pool.size());
         repeat(level, p, 2, 2, k - 1, i -> hang(level, p, slot));
         later(level, p, 2 * k, () -> spear(level, p, slot, target, pool, pw, emp));
      }

      return true;
   }

   private static void spear(ServerLevel level, Player p, int slot, LivingEntity target, List<LivingEntity> pool, float pw, boolean emp) {
      Vec3 from = crownSlot(level, p, slot);
      Vec3 out = from.subtract(crownCentre(p)).horizontal();
      out = out.lengthSqr() > 1.0E-4 ? out.normalize() : GearKit.flatLook(p);
      HrimthaneArsenal.Shard s = shard(level, p, from, out.scale(0.2).add(0.0, 0.9, 0.0), 40, 0.8);
      s.comet = true;
      s.target = target;
      s.pool = pool;
      s.homing = 0.3;
      s.homingDelay = 3;
      s.speed = 1.2;
      s.maxSpeed = 2.2;
      s.accel = 0.2;
      s.onHit = (sh, e, at) -> spearHit(level, p, e, at, pw, emp);
      s.onEnd = (sh, e, at) -> {
         LivingEntity t = sh.target;
         boolean near = sh.blocked && t != null && t.isAlive() && boxDist(t.getBoundingBox(), at) < 1.5;
         spearHit(level, p, near ? t : null, at, pw, emp);
      };
      Vfx.flare(level, from, CORE, 1.0, 5);
      Vfx.shards(level, from, CORE, 3, 0.2);
      sfx(level, from, "bolt.ice.launch", 0.7F, 1.1F + 0.05F * slot);
   }

   private static void spearHit(ServerLevel level, Player p, @Nullable LivingEntity main, Vec3 at, float pw, boolean emp) {
      if (main != null) {
         if (brittle(level, main)) {
            shatter(level, p, main, pw);
         }

         deal(level, p, main, 12.0F * pw, false);
         rime(level, p, main, 2);
      }

      for (LivingEntity e : foes(level, p, at, 1.5)) {
         if (e != main) {
            deal(level, p, e, 6.0F * pw, false);
         }
      }

      Vfx.impact(level, at, FROST, 0.7);
      Vfx.shards(level, at, CORE, 12, 0.5);
      Vfx.glint(level, at, CORE, 1.0);
      sfx(level, at, "bolt.ice.impact", 1.0F, 1.1F);
      if (emp) {
         Vec3 g = ground(level, at);
         Vfx.pillar(level, g, FROST, 2.5, 0.35);

         for (LivingEntity e : foes(level, p, g, 1.5)) {
            e.push(0.0, 0.6, 0.0);
            e.syncVelocity = true;
            deal(level, p, e, 6.0F * pw, false);
         }
      }
   }

   static void crownLevel(ServerLevel level, Player p, WeaponKit w, int lvl, boolean max) {
      Vec3 pos = crownSlot(level, p, lvl - 1);
      Vfx.flare(level, pos, CORE, 1.0, 6);
      Vfx.shards(level, pos, CORE, 4, 0.15);
      Vfx.ring(level, p.position().add(0.0, 0.05, 0.0), GLOW, 0.8 + 0.4 * lvl, 8);
      if (max) {
         Vfx.runeCircle(level, ground(level, p.position()), 2.2, FROST, 40, 0.06F, Vfx.Rune.GLYPH);
         Vfx.glint(level, crownCentre(p), CORE, 1.2);
      }
   }

   static void quickReload(ServerLevel level, LivingEntity a, LivingEntity t, ItemStack s) {
      Player p = a instanceof Player pl ? pl : null;
      rime(level, p, t, 1, 5);
      GearKit.hitSpark(level, t, FROST);
      if (p != null) {
         GearState state = GearState.of(p);
         int left = state.remaining("icicle_repeater.primary");
         if (left > 0) {
            state.cooldown("icicle_repeater.primary", Math.max(1, left - 20));
            GearKit.procSound(level, t, "icicle_repeater");
            Vfx.glint(level, GearKit.tip(p), CORE, 0.8);
         }
      }
   }

   private static @Nullable LivingEntity nearAim(ServerLevel level, Player p, double range, double minDot) {
      Vec3 eye = GearKit.eye(p);
      Vec3 look = p.getLookAngle();
      LivingEntity best = null;
      double bestDot = minDot;

      for (LivingEntity e : GearKit.enemies(level, p, eye.add(look.scale(range / 2.0)), range * 0.75)) {
         AABB box = e.getBoundingBox();
         Vec3 ray = eye.add(look.scale(Mth.clamp(box.getCenter().subtract(eye).dot(look), 0.0, range)));
         Vec3 to = new Vec3(
               Mth.clamp(ray.x, box.minX, box.maxX), Mth.clamp(ray.y, box.minY, box.maxY), Mth.clamp(ray.z, box.minZ, box.maxZ)
            )
            .subtract(eye);
         double d = to.length();
         double dot = d < 0.001 ? 1.0 : to.dot(look) / d;
         if (d <= range && dot > bestDot && p.hasLineOfSight(e)) {
            best = e;
            bestDot = dot;
         }
      }

      return best;
   }

   private static Vec3 crownCentre(Player p) {
      return p.position().add(0.0, p.getBbHeight() + 0.9, 0.0);
   }

   private static Vec3 crownSlot(ServerLevel level, Player p, int k) {
      double deg = level.getGameTime() / 2L * 12.0 + (k < 5 ? k * 72 : (k - 5) * 72 + 36);
      double a = Math.toRadians(deg);
      return crownCentre(p).add(Math.cos(a) * 0.9, 0.0, Math.sin(a) * 0.9);
   }

   private static void rimeCarapace(ServerLevel level, Player p, int pieces) {
      p.setTicksFrozen(0);
      if (p.onGround()) {
         frostWalk(level, p);
      }

      if (level.getGameTime() % 20L == 0L && GearState.of(p).ready("hrimthane.carapace") && !p.hasEffect(MobEffects.ABSORPTION)) {
         p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, 0, true, false, true));
         CARAPACE.add(p.getUUID());
         Vfx.shieldBubble(level, p.position().add(0.0, 1.0, 0.0), 1.2, FROST, 16);
         Vfx.shards(level, chest(p), CORE, 6, 0.3);
         sfx(level, p.position(), "weapon.rimebreaker_axe.freeze", 0.5F, 1.4F);
      }
   }

   private static void frostWalk(ServerLevel level, Player p) {
      BlockPos c = p.blockPosition().below();
      BlockState ice = Blocks.FROSTED_ICE.defaultBlockState();

      for (BlockPos q : BlockPos.betweenClosed(c.offset(-2, 0, -2), c.offset(2, 0, 2))) {
         if (!(q.distSqr(c) > 6.5)) {
            BlockState s = level.getBlockState(q);
            if (s.is(Blocks.WATER) && s.getFluidState().isSource() && level.getBlockState(q.above()).isAir() && level.isUnobstructed(ice, q, CollisionContext.empty())) {
               level.setBlockAndUpdate(q.immutable(), ice);
            }
         }
      }
   }

   private static void permafrost(ServerLevel level, Player p, int pieces) {
      long t = level.getGameTime();
      if (t % 10L == 0L) {
         Vfx.debris(level, p.position().add(0.0, 2.2, 0.0), SNOW, CORE, 2, 0.05);
      }

      if (t % 40L == 0L) {
         boolean any = false;

         for (LivingEntity e : foes(level, p, p.position(), 5.0)) {
            int before = stacks(level, e);
            if (rime(level, p, e, 1, 3) > before) {
               any = true;
            }
         }

         if (any) {
            Vfx.ring(level, p.position().add(0.0, 0.05, 0.0), GLOW, 5.0, 16);
         }
      }
   }

   private static void wyrmscaleHurt(ServerLevel level, Player p, @Nullable LivingEntity attacker, DamageSource source, float taken, int pieces) {
      if (CARAPACE.contains(p.getUUID()) && p.getAbsorptionAmount() <= 0.0F) {
         CARAPACE.remove(p.getUUID());
         GearState.of(p).cooldown("hrimthane.carapace", 240);
         Vec3 chest = chest(p);

         for (LivingEntity e : foes(level, p, p.position(), 3.0)) {
            deal(level, p, e, 4.0F, false);
            rime(level, p, e, 1);
            GearKit.knockAway(e, p.position(), 0.6, 0.2);
         }

         Vfx.shards(level, chest, CORE, 18, 0.7);
         Vfx.debris(level, chest, SNOW, CORE, 12, 0.4);
         Vfx.flare(level, chest, GLOW, 1.8, 6);
         sfx(level, p.position(), "weapon.rimebreaker_axe.shatter", 0.8F, 1.3F);
      }

      if (pieces >= 4 && attacker != null && attacker.isAlive()) {
         GearState state = GearState.of(p);
         boolean melee = source.getDirectEntity() == attacker;
         if (state.ready("hrimthane.retort") && (melee || !(attacker.distanceToSqr(p) > 144.0))) {
            state.cooldown("hrimthane.retort", 10);
            if (melee) {
               rime(level, p, attacker, 2, 5);
               attacker.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1), p);
            } else {
               rime(level, p, attacker, 1, 5);
            }

            Vec3 chest = chest(p);
            Vec3 to = center(attacker).subtract(chest);
            Vfx.streaks(level, chest, to.lengthSqr() > 1.0E-4 ? to.normalize() : GearKit.flatLook(p), FROST, 6);
            GearKit.proc(level, attacker, FROST);
            Vfx.shards(level, center(attacker), CORE, 6, 0.35);
            sfx(level, attacker.position(), "weapon.glacial_lance.proc", 0.7F, 1.2F);
         }
      }
   }

   static boolean absoluteZero(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      Vec3 feet = ground(level, p.position());
      Vec3 chest = feet.add(0.0, 1.2, 0.0);
      p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 16, 2, true, false));
      p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 16, 6, true, false));
      Vfx.telegraphCircle(level, feet, 8.0, 16, FROST);
      Vfx.runeCircle(level, feet, 8.0, FROST, 36, 0.08F, Vfx.Rune.GLYPH);
      Vfx.chargeUp(level, chest, FROST, 16);
      Vfx.gather(level, chest, FROST, 6.0, 16);
      wings(level, p, 3.2, 110.0F);
      later(level, p, 8, () -> wings(level, p, 3.8, 130.0F));
      later(level, p, 16, () -> {
         List<LivingEntity> hits = foes(level, p, feet, 8.0);
         Set<LivingEntity> primed = primed(level, hits);

         for (LivingEntity e : hits) {
            if (primed.contains(e)) {
               shatter(level, p, e, 1.0F);
            }

            deal(level, p, e, 20.0F, false);
            rime(level, p, e, 5);
            GearKit.knockAway(e, feet, 0.6, 0.3);
         }

         Vfx.nova(level, chest, FROST, 8.0);
         Vfx.heavyImpact(level, feet, FROST, 2.0);
         Vfx.groundCrack(level, feet, 8.0, FROST);
         Vfx.debris(level, chest, SNOW, CORE, 30, 0.6);
         Vfx.shards(level, chest, CORE, 30, 0.9);
         sfx(level, feet, "weapon.rimebreaker_axe.zero", 2.0F, 0.9F);
         Vfx.vortex(level, feet, 7.0, 6.0, FROST, 120);
         repeat(level, p, 20, 20, 6, i -> {
            for (LivingEntity ex : foes(level, p, feet, 7.0)) {
               deal(level, p, ex, 3.0F, false);
               rime(level, p, ex, 1);
            }

            if (p.position().distanceToSqr(feet) <= 49.0) {
               p.addEffect(new MobEffectInstance(MobEffects.SPEED, 30, 1, true, false));
               p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 30, 0, true, false));
            }

            Vfx.ring(level, feet.add(0.0, 0.05, 0.0), GLOW, 7.0, 20);
            Vfx.debris(level, feet.add(0.0, 3.0, 0.0), SNOW, CORE, 10, 0.2);
         });
      });
      return true;
   }

   private static void wings(ServerLevel level, Player p, double reach, float arc) {
      Vec3 shoulders = p.position().add(0.0, 1.5, 0.0);
      float back = p.getYRot() + 180.0F;
      Vfx.slashArc(level, shoulders, back + 60.0F, -10.0F, 60.0F, reach, FROST, arc);
      Vfx.slashArc(level, shoulders, back - 60.0F, -10.0F, -60.0F, reach, FROST, arc);
   }

   private static int rime(ServerLevel level, @Nullable Player src, LivingEntity t, int amount) {
      return rime(level, src, t, amount, 5);
   }

   static int rime(ServerLevel level, @Nullable Player src, LivingEntity t, int amount, int cap) {
      if (t != src && t.isAlive() && !t.isSpectator() && amount > 0) {
         long now = level.getGameTime();
         HrimthaneArsenal.Rime r = entry(level, t, true);
         if (r.frozenUntil > now) {
            return 0;
         }

         int max = Math.min(cap, 5);
         if (r.thawUntil > now) {
            max = Math.min(max, 4);
         }

         if (isWyrm(t)) {
            max = Math.min(max, 3);
         }

         int before = r.stacks;
         int after = Math.max(before, Math.min(max, before + amount));
         r.expires = now + 120L;
         if (after >= 5) {
            freezeSolid(level, t, r);
            return 5;
         }

         r.stacks = after;
         if (after == before) {
            return after;
         }

         if (after >= 3) {
            t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, after >= 4 ? 1 : 0), src);
         }

         if (t.canFreeze()) {
            t.setTicksFrozen(Math.max(t.getTicksFrozen(), 28 * after));
         }

         Vec3 c = center(t);
         Vfx.ring(level, t.position().add(0.0, 0.05, 0.0), GLOW, 0.5 + 0.15 * after + t.getBbWidth() * 0.5, 10);
         Vfx.shards(level, c, CORE, after + 1, 0.2);
         if (after >= 3) {
            Vfx.debris(level, c, SNOW, CORE, 2 + after, 0.12);
         }

         return after;
      } else {
         return 0;
      }
   }

   static void freeze(ServerLevel level, @Nullable Player src, LivingEntity t) {
      if (t != src && t.isAlive() && !t.isSpectator()) {
         if (isWyrm(t)) {
            rime(level, src, t, 3, 3);
         } else {
            HrimthaneArsenal.Rime r = entry(level, t, true);
            if (r.frozenUntil <= level.getGameTime()) {
               freezeSolid(level, t, r);
            }
         }
      }
   }

   private static void freezeSolid(ServerLevel level, LivingEntity t, HrimthaneArsenal.Rime r) {
      long now = level.getGameTime();
      boolean boss = isBoss(t);
      boolean player = t instanceof Player;
      int d = !boss && !player ? (!t.canFreeze() ? 25 : 50) : 20;
      r.stacks = 0;
      r.expires = now;
      r.frozenUntil = now + d;
      r.brittleUntil = now + d + 20L;
      r.thawUntil = now + d + (boss ? 240 : 120);
      if (boss) {
         t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, d, 2));
      } else if (player) {
         t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, d, 3));
      } else {
         t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, d, 5));
         t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, d, 1));
         Vec3 v = t.getDeltaMovement();
         t.setDeltaMovement(0.0, Math.min(0.0, v.y), 0.0);
         t.syncVelocity = true;
      }

      if (t.canFreeze()) {
         t.setTicksFrozen(t.getTicksRequiredToFreeze() + 2 * d);
      }

      Vec3 c = center(t);
      double radius = Math.max(0.8, 0.5 * Math.max(t.getBbWidth(), t.getBbHeight() * 0.6) + 0.35);
      Vfx.shieldBubble(level, c, radius, FROST, d);
      Vfx.shards(level, c, CORE, 14, 0.5);
      Vfx.debris(level, c, SNOW, CORE, 12, 0.3);
      Vfx.glint(level, top(t), CORE, 1.0);
      Vfx.ring(level, t.position().add(0.0, 0.05, 0.0), GLOW, 1.5 + t.getBbWidth() * 0.5, 14);
      sfx(level, c, "weapon.rimebreaker_axe.freeze", 1.1F, 1.0F);
   }

   static boolean brittle(ServerLevel level, LivingEntity t) {
      HrimthaneArsenal.Rime r = entry(level, t, false);
      return r != null && r.brittleUntil > level.getGameTime();
   }

   static int stacks(ServerLevel level, LivingEntity t) {
      HrimthaneArsenal.Rime r = entry(level, t, false);
      return r == null ? 0 : r.stacks;
   }

   static boolean shatter(ServerLevel level, Player p, LivingEntity t, float power) {
      long now = level.getGameTime();
      HrimthaneArsenal.Rime r = entry(level, t, false);
      if (r != null && r.brittleUntil > now && t.isAlive()) {
         r.brittleUntil = 0L;
         r.frozenUntil = Math.min(r.frozenUntil, now);
         MobEffectInstance slow = t.getEffect(MobEffects.SLOWNESS);
         if (slow != null && slow.getAmplifier() >= 2) {
            t.removeEffect(MobEffects.SLOWNESS);
         }

         if (t.canFreeze()) {
            t.setTicksFrozen(0);
         }

         Vec3 c = center(t);
         Vec3 feet = t.position();
         deal(level, p, t, 12.0F * power, false);

         for (LivingEntity e : foes(level, p, feet, 2.5 + t.getBbWidth() / 2.0F)) {
            if (e != t) {
               deal(level, p, e, 6.0F * power, false);
               rime(level, p, e, 1);
            }
         }

         Vfx.heavyImpact(level, feet, FROST, 0.8);
         Vfx.shards(level, c, CORE, 24, 0.9);
         Vfx.debris(level, c, SNOW, CORE, 16, 0.5);
         Vfx.glint(level, c, CORE, 1.4);
         Vfx.ring(level, feet.add(0.0, 0.05, 0.0), GLOW, 2.5 + t.getBbWidth() * 0.5, 12);
         sfx(level, c, "weapon.rimebreaker_axe.shatter", 1.3F, 1.0F);
         return true;
      } else {
         return false;
      }
   }

   private static Set<LivingEntity> primed(ServerLevel level, List<LivingEntity> hits) {
      Set<LivingEntity> out = new HashSet<>();

      for (LivingEntity e : hits) {
         if (brittle(level, e)) {
            out.add(e);
         }
      }

      return out;
   }

   private static HrimthaneArsenal.@Nullable Rime entry(ServerLevel level, LivingEntity t, boolean create) {
      long now = level.getGameTime();
      HrimthaneArsenal.Rime r = RIME.get(t);
      if (r != null && r.expires <= now) {
         r.stacks = 0;
      }

      if (r != null && r.stacks == 0 && r.frozenUntil <= now && r.brittleUntil <= now && r.thawUntil <= now) {
         RIME.remove(t);
         r = null;
      }

      if (r == null && create) {
         r = new HrimthaneArsenal.Rime();
         RIME.put(t, r);
      }

      return r;
   }

   private static boolean isWyrm(LivingEntity e) {
      return BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath().equals("hrimthane");
   }

   private static boolean isBoss(LivingEntity e) {
      return e instanceof MysticalBoss b ? b.isBoss() : e instanceof EnderDragon || e instanceof WitherBoss;
   }

   private static void later(ServerLevel level, Player p, int ticks, Runnable r) {
      PENDING.add(new HrimthaneArsenal.Task(clock + Math.max(1, ticks), () -> {
         if (alive(p, level)) {
            r.run();
         }
      }));
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

   private static HrimthaneArsenal.Shard shard(ServerLevel level, Player owner, Vec3 from, Vec3 vel, int life, double radius) {
      HrimthaneArsenal.Shard s = new HrimthaneArsenal.Shard(level, owner, from, vel, life, radius);
      NEW_SHARDS.add(s);
      return s;
   }

   private static void tick(MinecraftServer server) {
      clock++;
      if (!PENDING.isEmpty()) {
         TASKS.addAll(PENDING);
         PENDING.clear();
      }

      if (!TASKS.isEmpty()) {
         List<HrimthaneArsenal.Task> due = new ArrayList<>();
         TASKS.removeIf(tx -> tx.at() <= clock && due.add(tx));

         for (HrimthaneArsenal.Task t : due) {
            try {
               t.run().run();
            } catch (RuntimeException ex) {
               MysticalTerrain.LOGGER.error("Hrimthane gear task failed", ex);
            }
         }
      }

      if (!NEW_SHARDS.isEmpty()) {
         SHARDS.addAll(NEW_SHARDS);
         NEW_SHARDS.clear();
      }

      if (!SHARDS.isEmpty()) {
         SHARDS.removeIf(s -> {
            try {
               return step(s);
            } catch (RuntimeException ex) {
               MysticalTerrain.LOGGER.error("Hrimthane gear shard failed", ex);
               return true;
            }
         });
      }

      if (clock % 2L == 0L) {
         crownHover(server);
      }

      if (clock % 10L == 0L && !RIME.isEmpty()) {
         rimeTells(clock % 100L == 0L);
      }
   }

   private static boolean step(HrimthaneArsenal.Shard s) {
      if (!alive(s.owner, s.level)) {
         return true;
      }

      s.age++;
      if (s.homing > 0.0 && s.age > s.homingDelay) {
         if (s.target != null && (!s.target.isAlive() || s.target.isRemoved())) {
            s.target = null;
            if (s.pool != null) {
               for (LivingEntity e : s.pool) {
                  if (e.isAlive() && !e.isRemoved()) {
                     s.target = e;
                     break;
                  }
               }
            }
         }

         if (s.target != null) {
            s.speed = Math.min(s.maxSpeed, s.speed + s.accel);
            Vec3 to = s.target.getBoundingBox().getCenter().subtract(s.pos);
            if (to.lengthSqr() > 1.0E-4) {
               s.vel = s.vel.lerp(to.normalize().scale(s.speed), s.homing);
            }
         }
      }

      Vec3 next = s.pos.add(s.vel);
      BlockHitResult block = s.level.clip(new ClipContext(s.pos, next, Block.COLLIDER, Fluid.NONE, s.owner));
      boolean wall = block.getType() != Type.MISS;
      Vec3 end = wall ? block.getLocation() : next;
      double seg = end.distanceTo(s.pos);
      Vec3 from = s.pos;
      List<LivingEntity> near = new ArrayList<>();
      List<Vec3> points = new ArrayList<>();

      for (LivingEntity e : GearKit.enemies(s.level, s.owner, from.add(end).scale(0.5), s.radius + seg / 2.0 + 6.0)) {
         if (!s.hit.contains(e)) {
            AABB box = e.getBoundingBox().inflate(s.radius);
            Vec3 at = box.contains(from) ? from : (Vec3)box.clip(from, end).orElse(null);
            if (at != null) {
               near.add(e);
               points.add(at);
            }
         }
      }

      if (!near.isEmpty()) {
         List<Integer> order = new ArrayList<>();

         for (int i = 0; i < near.size(); i++) {
            order.add(i);
         }

         order.sort(Comparator.comparingDouble(ix -> points.get(ix).distanceToSqr(from)));

         for (int i : order) {
            LivingEntity e = near.get(i);
            s.hit.add(e);
            if (s.onHit != null) {
               s.onHit.at(s, e, points.get(i));
            }

            if (--s.pierce < 0) {
               return true;
            }
         }
      }

      if (wall) {
         s.blocked = true;
         if (s.onEnd != null) {
            s.onEnd.at(s, null, end);
         }

         return true;
      } else {
         s.pos = next;
         if (s.comet) {
            Vfx.meteorTrail(s.level, s.pos, s.vel, FROST);
         } else {
            Vfx.trail(s.level, s.pos, FROST, 0.3);
         }

         if (s.fx != null) {
            s.fx.accept(s);
         }

         if (s.age >= s.life) {
            if (s.onEnd != null) {
               s.onEnd.at(s, null, s.pos);
            }

            return true;
         } else {
            return false;
         }
      }
   }

   private static void crownHover(MinecraftServer server) {
      for (ServerPlayer p : server.getPlayerList().getPlayers()) {
         if (p.isAlive() && p.isUsingItem() && p.getUseItem().getItem() instanceof MysticWeapon w && "icicle_repeater".equals(w.gearId())) {
            int n = Math.min(5, p.getTicksUsingItem() / 8);

            for (int k = 0; k < n; k++) {
               hang(p.level(), p, k);
            }
         }
      }
   }

   private static void hang(ServerLevel level, Player p, int k) {
      Vec3 pos = crownSlot(level, p, k);
      Vfx.glint(level, pos, CORE, 0.35);
      Vfx.beam(level, pos.add(0.0, 0.45, 0.0), pos.add(0.0, -0.45, 0.0), FROST, 0.12);
   }

   private static void rimeTells(boolean purge) {
      Iterator<Entry<LivingEntity, HrimthaneArsenal.Rime>> it = RIME.entrySet().iterator();

      while (it.hasNext()) {
         Entry<LivingEntity, HrimthaneArsenal.Rime> en = it.next();
         LivingEntity e = en.getKey();
         HrimthaneArsenal.Rime r = en.getValue();
         if (e != null && e.isAlive() && !e.isRemoved() && e.level() instanceof ServerLevel level) {
            long var9 = level.getGameTime();
            int stacks = r.expires > var9 ? r.stacks : 0;
            if (r.brittleUntil > var9) {
               Vfx.glint(level, top(e), CORE, 0.7);
            } else if (stacks >= 3) {
               Vfx.sparks(level, center(e), CORE, stacks - 1, 0.06, 0.12);
            } else if (purge && stacks == 0 && r.frozenUntil <= var9 && r.thawUntil <= var9) {
               it.remove();
            }
         } else if (purge) {
            it.remove();
         }
      }
   }

   private static void reset() {
      TASKS.clear();
      PENDING.clear();
      SHARDS.clear();
      NEW_SHARDS.clear();
      RIME.clear();
      CARAPACE.clear();
      clock = 0L;
   }

   private static void deal(ServerLevel level, Player p, LivingEntity e, float damage, boolean melee) {
      if (e.isAlive() && !(damage <= 0.0F)) {
         e.setInvulnerableTime(0);
         if (melee) {
            GearKit.strike(level, p, e, damage);
         } else {
            GearKit.magic(level, p, e, damage);
         }
      }
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

   private static double flatDist(LivingEntity e, Vec3 at) {
      double dx = e.getX() - at.x;
      double dz = e.getZ() - at.z;
      return Math.max(0.0, Math.sqrt(dx * dx + dz * dz) - e.getBbWidth() / 2.0F);
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

   private static Vec3 clipTo(ServerLevel level, Player p, Vec3 from, Vec3 dir, double range) {
      return level.clip(new ClipContext(from, from.add(dir.scale(range)), Block.COLLIDER, Fluid.NONE, p)).getLocation();
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

   private static void spikeRing(ServerLevel level, Vec3 center, double radius, int count, double height, double width, boolean shards) {
      double phase = level.getRandom().nextDouble() * Math.PI * 2.0;

      for (int i = 0; i < count; i++) {
         double a = phase + i * Math.PI * 2.0 / count;
         Vec3 rim = ground(level, center.add(Math.cos(a) * radius, 0.0, Math.sin(a) * radius));
         Vfx.pillar(level, rim, FROST, height, width);
         if (shards) {
            Vfx.shards(level, rim.add(0.0, 0.5, 0.0), CORE, 3, 0.35);
         }
      }
   }

   private static void sfx(ServerLevel level, Vec3 at, String key, float volume, float pitch) {
      SoundEvent e = BossSounds.event(key);
      if (e != null) {
         level.playSound(null, at.x, at.y, at.z, e, SoundSource.PLAYERS, volume, pitch * (0.95F + level.getRandom().nextFloat() * 0.1F));
      }
   }

   @FunctionalInterface
   private interface OnShard {
      void at(HrimthaneArsenal.Shard var1, @Nullable LivingEntity var2, Vec3 var3);
   }

   private static final class Rime {
      int stacks;
      long expires;
      long frozenUntil;
      long brittleUntil;
      long thawUntil;
   }

   private static final class Shard {
      final ServerLevel level;
      final Player owner;
      Vec3 pos;
      Vec3 vel;
      int age;
      int life;
      double radius;
      double speed;
      double maxSpeed;
      double accel;
      int pierce;
      double homing;
      int homingDelay;
      @Nullable LivingEntity target;
      @Nullable List<LivingEntity> pool;
      final Set<LivingEntity> hit = new HashSet<>();
      boolean comet;
      boolean blocked;
      HrimthaneArsenal.@Nullable OnShard onHit;
      HrimthaneArsenal.@Nullable OnShard onEnd;
      @Nullable Consumer<HrimthaneArsenal.Shard> fx;

      Shard(ServerLevel level, Player owner, Vec3 pos, Vec3 vel, int life, double radius) {
         this.level = level;
         this.owner = owner;
         this.pos = pos;
         this.vel = vel;
         this.life = life;
         this.radius = radius;
         this.speed = vel.length();
         this.maxSpeed = this.speed;
      }
   }

   private record Task(long at, Runnable run) {
   }
}
