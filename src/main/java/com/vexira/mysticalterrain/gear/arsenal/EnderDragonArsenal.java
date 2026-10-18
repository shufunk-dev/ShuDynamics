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
import com.vexira.mysticalterrain.gear.WeaponKit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStopped;
import org.jspecify.annotations.Nullable;

public final class EnderDragonArsenal extends Arsenal {
   private static final Vfx.Palette MONO = Vfx.Palette.MONO;
   private static final int WHITE = 16777215;
   private static final int BLACK = 657932;
   private static final float MARK_BONUS = 1.3F;
   private static final List<EnderDragonArsenal.Task> TASKS = new ArrayList<>();
   private static final List<EnderDragonArsenal.Live> LIVE = new ArrayList<>();
   private static final Map<LivingEntity, long[]> SCARS = new WeakHashMap<>();
   private static final Map<LivingEntity, Long> MARKS = new WeakHashMap<>();
   private static final Map<UUID, EnderDragonArsenal.Aegis> AEGIS = new HashMap<>();
   private static boolean hooked;
   private static final int END_LIFE = 0;
   private static final int END_BLOCK = 1;
   private static final int END_ENEMY = 2;
   private static final int END_HOME = 3;

   public EnderDragonArsenal() {
      super("ender_dragon", "void_dragon_armor", Vfx.Palette.MONO);
      hook();
   }

   @Override
   protected void weapons() {
      this.weapon("voidreaver", 12.0F, -3.0F)
         .primary(Active.of(160, EnderDragonArsenal::riftcleave).empowered(60))
         .alternate(Active.of(280, EnderDragonArsenal::shatterfall).empowered(50))
         .charge(Charge.of(3, 12).perLevel(0.4F).pose(ItemUseAnimation.TRIDENT).onLevel(EnderDragonArsenal::edgeLevel))
         .passive(EnderDragonArsenal::riftscar)
         .resonancePerHit(12.0F);
      this.weapon("wyrmspine_glaive", 10.0F, -2.7F)
         .primary(Active.of(140, EnderDragonArsenal::tailTempest).empowered(40))
         .alternate(Active.of(180, EnderDragonArsenal::talonRake).empowered(40))
         .charge(
            Charge.of(3, 10).pose(ItemUseAnimation.TRIDENT).onLevel(EnderDragonArsenal::javelinLevel).cast(Active.of(240, EnderDragonArsenal::javelin).empowered(50))
         )
         .passive(EnderDragonArsenal::skyrend)
         .resonancePerHit(10.0F);
      this.weapon("endseer_staff", 5.0F, -2.4F)
         .primary(Active.of(80, EnderDragonArsenal::endOrbs).empowered(30))
         .alternate(Active.of(320, EnderDragonArsenal::eventHorizon).empowered(60))
         .charge(Charge.of(4, 10).pose(ItemUseAnimation.BOW).onLevel(EnderDragonArsenal::lanceLevel).cast(Active.of(280, EnderDragonArsenal::voidLance).empowered(40)))
         .passive(EnderDragonArsenal::endsight)
         .resonancePerHit(8.0F);
   }

   @Override
   protected void armor(ArmorKit set) {
      set.toughness(4.0).knockback(0.125);
      set.twoPiece((level, p, pieces) -> {
         if (level.getGameTime() % 10L == 0L && p.hasEffect(MobEffects.DARKNESS)) {
            p.removeEffect(MobEffects.DARKNESS);
         }
      });
      set.fourPiece(EnderDragonArsenal::aegisTick);
      set.allowDamage(EnderDragonArsenal::guard);
      set.active(600, EnderDragonArsenal::lastLight);
   }

   private static void hook() {
      if (!hooked) {
         hooked = true;
         ServerTickEvents.END_LEVEL_TICK.register(EnderDragonArsenal::tickWorld);
         ServerLifecycleEvents.SERVER_STOPPED.register((ServerStopped)server -> {
            TASKS.clear();
            LIVE.clear();
            SCARS.clear();
            MARKS.clear();
            AEGIS.clear();
         });
      }
   }

   private static void later(ServerLevel level, int ticks, Runnable run) {
      TASKS.add(new EnderDragonArsenal.Task(level, level.getGameTime() + Math.max(1, ticks), run));
   }

   private static void every(ServerLevel level, EnderDragonArsenal.Ticker ticker) {
      LIVE.add(new EnderDragonArsenal.Live(level, ticker));
   }

   private static void tickWorld(ServerLevel level) {
      if (!TASKS.isEmpty()) {
         long now = level.getGameTime();
         List<EnderDragonArsenal.Task> due = new ArrayList<>();
         Iterator<EnderDragonArsenal.Task> it = TASKS.iterator();

         while (it.hasNext()) {
            EnderDragonArsenal.Task t = it.next();
            if (t.level() == level && t.due() <= now) {
               due.add(t);
               it.remove();
            }
         }

         for (EnderDragonArsenal.Task t : due) {
            try {
               t.run().run();
            } catch (RuntimeException e) {
               MysticalTerrain.LOGGER.error("Ender Dragon gear task failed", e);
            }
         }
      }

      if (!LIVE.isEmpty()) {
         for (EnderDragonArsenal.Live l : new ArrayList<>(LIVE)) {
            if (l.level == level) {
               boolean keep;
               try {
                  keep = l.ticker.tick(++l.age);
               } catch (RuntimeException e) {
                  MysticalTerrain.LOGGER.error("Ender Dragon gear effect failed", e);
                  keep = false;
               }

               if (!keep) {
                  LIVE.remove(l);
               }
            }
         }
      }
   }

   static boolean alive(Player p, ServerLevel level) {
      return p.isAlive() && !p.isRemoved() && p.level() == level;
   }

   private static void plainDraw(EnderDragonArsenal.Tracer t, int age) {
      Vfx.trail(t.level, t.pos, MONO, t.size);
      if (age % 2 == 0) {
         Vfx.smoke(t.level, t.pos, 657932, 1, 0.05, t.size * 1.4);
      }
   }

   private static void orbDraw(EnderDragonArsenal.Tracer t, int age) {
      Vfx.flare(t.level, t.pos, 16777215, 0.5, 3);
      plainDraw(t, age);
   }

   private static void spineDraw(EnderDragonArsenal.Tracer t, int age) {
      Vfx.meteorTrail(t.level, t.pos, t.vel, MONO);
      plainDraw(t, age);
   }

   private static double segDist(Vec3 a, Vec3 b, Vec3 c) {
      Vec3 ab = b.subtract(a);
      double len = ab.lengthSqr();
      double k = len < 1.0E-8 ? 0.0 : Mth.clamp(c.subtract(a).dot(ab) / len, 0.0, 1.0);
      return c.distanceTo(a.add(ab.scale(k)));
   }

   static boolean movable(LivingEntity e) {
      return !(e instanceof MysticalBoss b && b.isBoss()) && !(e instanceof EnderDragon) && !(e instanceof WitherBoss);
   }

   static void fling(LivingEntity e, Vec3 from, double out, double up) {
      if (movable(e)) {
         Vec3 away = new Vec3(e.getX() - from.x, 0.0, e.getZ() - from.z);
         away = away.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0.0F, e.getYRot()).scale(-1.0) : away.normalize();
         Vec3 v = e.getDeltaMovement();
         e.setDeltaMovement(v.x * 0.3 + away.x * out, Math.max(v.y, 0.0) + up, v.z * 0.3 + away.z * out);
         e.syncVelocity = true;
      }
   }

   static void lift(LivingEntity e, double up) {
      if (movable(e)) {
         Vec3 v = e.getDeltaMovement();
         e.setDeltaMovement(v.x, Math.max(v.y, 0.0) + up, v.z);
         e.syncVelocity = true;
      }
   }

   static void nudge(LivingEntity e, Vec3 dv) {
      if (movable(e)) {
         e.setDeltaMovement(e.getDeltaMovement().add(dv));
         e.syncVelocity = true;
      }
   }

   static void zap(ServerLevel level, Player p, LivingEntity e, float damage) {
      e.setInvulnerableTime(0);
      GearKit.magic(level, p, e, damage);
   }

   static void cut(ServerLevel level, Player p, LivingEntity e, float damage) {
      e.setInvulnerableTime(0);
      GearKit.strike(level, p, e, damage);
   }

   static List<LivingEntity> near(ServerLevel level, Player p, Vec3 c, double r) {
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : GearKit.enemies(level, p, c, r + 4.0)) {
         if (gap(e, c) <= r) {
            out.add(e);
         }
      }

      return out;
   }

   static double gap(LivingEntity e, Vec3 c) {
      AABB b = e.getBoundingBox();
      double dx = Math.max(0.0, Math.max(b.minX - c.x, c.x - b.maxX));
      double dy = Math.max(0.0, Math.max(b.minY - c.y, c.y - b.maxY));
      double dz = Math.max(0.0, Math.max(b.minZ - c.z, c.z - b.maxZ));
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   static List<LivingEntity> strip(ServerLevel level, Player p, Vec3 from, Vec3 dir, double length, double width, double height) {
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : GearKit.enemies(level, p, from.add(dir.scale(length / 2.0)), length / 2.0 + width + 4.0)) {
         double ox = e.getX() - from.x;
         double oz = e.getZ() - from.z;
         double along = ox * dir.x + oz * dir.z;
         double side = Math.abs(ox * dir.z - oz * dir.x);
         AABB b = e.getBoundingBox();
         if (along > -width && along < length + width && side < width + e.getBbWidth() / 2.0F && b.maxY > from.y - 2.5 && b.minY < from.y + height + 1.0) {
            out.add(e);
         }
      }

      return out;
   }

   static @Nullable Vec3 floor(ServerLevel level, double x, double y, double z, int up, int down) {
      MutableBlockPos pos = BlockPos.containing(x, y + up, z).mutable();
      boolean air = false;

      for (int i = 0; i <= up + down; i++) {
         BlockState state = level.getBlockState(pos);
         VoxelShape shape = state.getCollisionShape(level, pos);
         boolean fluid = !state.getFluidState().isEmpty();
         if (shape.isEmpty() && !fluid) {
            air = true;
         } else if (air) {
            return new Vec3(x, pos.getY() + (shape.isEmpty() ? 0.9 : shape.max(Axis.Y)), z);
         }

         pos.move(0, -1, 0);
      }

      return null;
   }

   static Vec3 groundAt(ServerLevel level, Vec3 at) {
      Vec3 g = floor(level, at.x, at.y, at.z, 2, 5);
      return g != null ? g : at;
   }

   static Vec3 center(LivingEntity e) {
      return e.getBoundingBox().getCenter();
   }

   static Vec3 chest(Player p) {
      return p.position().add(0.0, p.getBbHeight() * 0.62, 0.0);
   }

   static Vec3 flat(Vec3 v) {
      Vec3 f = new Vec3(v.x, 0.0, v.z);
      return f.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : f.normalize();
   }

   static BlockHitResult ray(ServerLevel level, Player p, Vec3 from, Vec3 dir, double length) {
      return level.clip(new ClipContext(from, from.add(dir.scale(length)), Block.COLLIDER, Fluid.NONE, p));
   }

   static void soundAt(ServerLevel level, Vec3 at, float volume, float pitch, String... keys) {
      for (String key : keys) {
         SoundEvent e = BossSounds.event(key);
         if (e != null) {
            level.playSound(null, at.x, at.y, at.z, e, SoundSource.PLAYERS, volume, pitch);
            return;
         }
      }
   }

   static boolean isMarked(ServerLevel level, LivingEntity e) {
      Long until = MARKS.get(e);
      return until != null && until > level.getGameTime();
   }

   static float mark(ServerLevel level, LivingEntity e) {
      return isMarked(level, e) ? 1.3F : 1.0F;
   }

   static void rift(
      ServerLevel level, Player p, Vec3 from, Vec3 dir, double length, int delay, float damage, double height, String weapon, float endBlast
   ) {
      Vec3 d = flat(dir);
      Vec3 start = groundAt(level, from);
      Vfx.telegraphLine(level, start, d, length, 2.2, delay, MONO);

      for (double k = 0.0; k <= length; k += 2.0) {
         Vfx.smoke(level, start.add(d.scale(k)).add(0.0, 0.3, 0.0), 657932, 1, 0.3, 1.1);
      }

      later(level, delay, () -> {
         if (alive(p, level)) {
            int n = 0;

            for (double kx = 1.5; kx <= length; n++) {
               Vec3 q = start.add(d.scale(kx));
               Vec3 g = floor(level, q.x, start.y, q.z, 3, 4);
               if (g != null) {
                  Vfx.pillar(level, g, MONO, height, 1.1);
                  Vfx.shards(level, g.add(0.0, 0.4, 0.0), 16777215, 4, 0.25);
                  if (n % 3 == 0) {
                     Vfx.groundCrack(level, g, 1.6, MONO);
                  }
               }

               kx += 2.0;
            }

            for (LivingEntity e : strip(level, p, start, d, length, 1.4, height)) {
               zap(level, p, e, damage);
               lift(e, 0.6);
               GearState.of(p).addResonance(weapon, 3.0F);
            }

            soundAt(level, start.add(d.scale(length / 2.0)), 1.4F, 0.85F, "weapon.voidreaver.rift", "weapon.voidreaver.use");
            Vfx.shake(level, start, 14.0, 0.25, 6);
            if (endBlast > 0.0F) {
               Vec3 end = groundAt(level, start.add(d.scale(length)));
               Vfx.explosion(level, end.add(0.0, 0.5, 0.0), MONO, 3.0);
               Vfx.smoke(level, end.add(0.0, 0.8, 0.0), 657932, 8, 1.2, 1.8);
               Vfx.shards(level, end.add(0.0, 0.6, 0.0), 16777215, 12, 0.45);

               for (LivingEntity e : near(level, p, end.add(0.0, 0.8, 0.0), 3.0)) {
                  zap(level, p, e, endBlast);
                  lift(e, 0.5);
               }
            }
         }
      });
   }

   static boolean riftcleave(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      Vec3 feet = p.position();
      Vec3 chest = chest(p);
      Vec3 dir = GearKit.flatLook(p);
      float yaw = p.getYRot();
      float power = u.power();
      int lvl = u.charge();

      for (LivingEntity e : near(level, p, chest, 4.5)) {
         Vec3 to = center(e).subtract(chest);
         to = new Vec3(to.x, 0.0, to.z);
         if (!(to.lengthSqr() > 1.4) || !(to.normalize().dot(dir) < 0.342)) {
            cut(level, p, e, 12.0F * power);
            fling(e, feet, 0.5, 0.25);
            GearKit.hitSpark(level, e, MONO);
         }
      }

      Vfx.slashArc(level, chest, yaw, 0.0F, 18.0F, 4.5, MONO, 220.0F);
      Vfx.streaks(level, chest, p.getLookAngle(), MONO, 8);

      for (int k = -1; k <= 1; k++) {
         Vfx.smoke(level, chest.add(dir.yRot(k * 0.7F).scale(2.6)), 657932, 1, 0.3, 1.3);
      }

      Vfx.glint(level, GearKit.tip(p), 16777215, 0.9);
      GearKit.kick(level, p, 0.3 + 0.08 * lvl);
      double length = 10 + 4 * lvl;
      double height = 3 + lvl + (u.empowered() ? 1 : 0);
      float blast = u.fullyCharged() ? 10.0F * power : 0.0F;
      Vec3 from = feet.add(dir.scale(1.5));
      if (u.empowered()) {
         for (int k = -1; k <= 1; k++) {
            rift(level, p, from, dir.yRot((float)Math.toRadians(20 * k)), length, 6, 9.0F * power, height, "voidreaver", blast);
         }
      } else {
         rift(level, p, from, dir, length, 6, 9.0F * power, height, "voidreaver", blast);
      }

      if (u.fullyCharged()) {
         Vfx.impactFrame(level, chest, 20.0, 16777215, 0.45F);
      }

      return true;
   }

   static void edgeLevel(ServerLevel level, Player p, WeaponKit kit, int charge, boolean max) {
      Vec3 feet = p.position();
      Vec3 dir = GearKit.flatLook(p);
      Vec3 t = GearKit.tip(p);
      double length = 10 + 4 * charge;
      Vec3 from = groundAt(level, feet.add(dir.scale(1.5)));
      if (GearState.of(p).resonance(kit.id()) >= 60.0F) {
         for (int k = -1; k <= 1; k++) {
            Vfx.telegraphLine(level, from, dir.yRot((float)Math.toRadians(20 * k)), length, 2.2, 8, MONO);
         }
      } else {
         Vfx.telegraphLine(level, from, dir, length, 2.2, 8, MONO);
      }

      Vfx.groundCrack(level, groundAt(level, feet), 0.8 + 0.4 * charge, MONO);
      Vfx.smoke(level, t, 657932, 2, 0.15, 1.0);
      Vfx.smoke(level, t.add(0.0, -0.6, 0.0), 657932, 1, 0.15, 0.9);
      if (max) {
         Vfx.flare(level, t, 16777215, 1.8, 6);
         Vfx.glint(level, t, 16777215, 1.2);
         GearKit.kick(level, p, 0.15);
      } else {
         Vfx.glint(level, t, 16777215, 0.5 + 0.2 * charge);
      }
   }

   static boolean shatterfall(GearUse u) {
      final ServerLevel level = u.level();
      final Player p = u.player();
      final Vec3 feet = p.position();
      final boolean air = !p.onGround() && !p.isInWater() && !p.onClimbable();
      double r = u.empowered() ? 6.5 : 5.0;
      final double startY = p.getY();
      if (air) {
         Vec3 look = p.getLookAngle();
         p.setDeltaMovement(look.x * 0.4, -2.6, look.z * 0.4);
         BlockHitResult below = ray(level, p, feet, new Vec3(look.x * 0.15, -1.0, look.z * 0.15).normalize(), 48.0);
         if (below.getType() != Type.MISS) {
            Vfx.telegraphCircle(level, below.getLocation(), r, Math.max(3, (int)((feet.y - below.getLocation().y) / 2.6)), MONO);
         }

         Vfx.streaks(level, chest(p), new Vec3(0.0, -1.0, 0.0), MONO, 10);
         Vfx.flare(level, chest(p), 16777215, 1.4, 6);
      } else {
         Vec3 aim = GearKit.lookPoint(level, p, 12.0);
         Vec3 off = new Vec3(aim.x - feet.x, 0.0, aim.z - feet.z);
         Vec3 dir = off.lengthSqr() < 0.001 ? GearKit.flatLook(p) : off.normalize();
         double dist = Mth.clamp(off.length(), 3.0, 10.0);
         Vec3 land = feet.add(dir.scale(dist));
         Vec3 g = floor(level, land.x, Math.max(aim.y, feet.y), land.z, 3, 10);
         double dy = g == null ? 0.0 : Mth.clamp(g.y - feet.y, -8.0, 2.5);
         double vy = 0.75;
         int flight = flightTicks(vy, dy);
         double vh = Math.min(1.8, dist * 0.09 / (1.0 - Math.pow(0.91, flight)));
         p.setDeltaMovement(dir.x * vh, vy, dir.z * vh);
         Vfx.telegraphCircle(level, g != null ? g : land, r, flight, MONO);
         Vfx.ring(level, feet, 16777215, 1.6, 8);
         Vfx.smoke(level, feet.add(0.0, 0.3, 0.0), 657932, 4, 0.6, 1.3);
         Vfx.streaks(level, chest(p), new Vec3(dir.x, 0.9, dir.z).normalize(), MONO, 6);
      }

      p.syncVelocity = true;
      p.resetFallDistance();
      every(level, new EnderDragonArsenal.Ticker() {
         private Vec3 prev = feet;
         private boolean left = air;

         @Override
         public boolean tick(int age) {
            if (!EnderDragonArsenal.alive(p, level)) {
               return false;
            }

            p.resetFallDistance();
            Vec3 now = p.position();
            if (!p.onGround()) {
               this.left = true;
            }

            if (age % 2 == 0 && now.distanceToSqr(this.prev) > 0.09) {
               Vfx.dashTrail(level, this.prev.add(0.0, 1.0, 0.0), now.add(0.0, 1.0, 0.0), EnderDragonArsenal.MONO);
               Vfx.smoke(level, now.add(0.0, 0.8, 0.0), 657932, 1, 0.2, 1.1);
               this.prev = now;
            }

            boolean landed = this.left && (p.onGround() || p.isInWater());
            if (landed || age >= 40 || !this.left && age >= 15) {
               EnderDragonArsenal.crater(u, air && startY - p.getY() >= 6.0 ? 1.25F : 1.0F);
               return false;
            } else {
               return true;
            }
         }
      });
      return true;
   }

   private static int flightTicks(double vy, double dy) {
      double y = 0.0;
      double v = vy;

      for (int t = 1; t <= 40; t++) {
         y += v;
         v = (v - 0.08) * 0.98;
         if (v < 0.0 && y <= dy) {
            return t;
         }
      }

      return 40;
   }

   private static void crater(GearUse u, float bonus) {
      ServerLevel level = u.level();
      Player p = u.player();
      Vec3 feet = p.position();
      double r = u.empowered() ? 6.5 : 5.0;

      for (LivingEntity e : near(level, p, feet.add(0.0, 0.5, 0.0), r)) {
         double d = Math.min(r, gap(e, feet));
         cut(level, p, e, (float)(16.0 * (1.0 - 0.5 * d / r)) * bonus * u.power());
         fling(e, feet, 0.9, 0.7);
         e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1), p);
      }

      Vfx.heavyImpact(level, feet, MONO, 1.2);
      Vfx.groundCrack(level, feet, r, MONO);
      Vfx.shockwave(level, feet, MONO, r);
      Vfx.shards(level, feet.add(0.0, 0.4, 0.0), 16777215, 8, 0.45);

      for (int i = 0; i < 8; i++) {
         double a = i * Math.PI / 4.0;
         Vfx.smoke(level, feet.add(Math.sin(a) * r * 0.6, 0.3, Math.cos(a) * r * 0.6), 657932, 1, 0.3, 1.4);
      }

      GearKit.sound(level, p, 1.5F, u.empowered() ? 0.75F : 0.85F, "weapon.voidreaver.land", "weapon.voidreaver.alt");
      if (u.empowered()) {
         Vec3 look = GearKit.flatLook(p);

         for (int k = 0; k < 4; k++) {
            Vec3 d = look.yRot((float)(k * Math.PI / 2.0));
            rift(level, p, feet.add(d.scale(1.2)), d, 8.0, 6, 9.0F * u.power(), 3.0, "voidreaver", 0.0F);
         }
      }
   }

   static void riftscar(ServerLevel level, LivingEntity a, LivingEntity t, ItemStack s) {
      if (a instanceof Player p) {
         long now = level.getGameTime();
         long[] scar = SCARS.computeIfAbsent(t, k -> new long[2]);
         if (now - scar[1] > 100L) {
            scar[0] = 0L;
         }

         scar[1] = now;
         scar[0]++;
         Vec3 c = center(t);
         if (scar[0] < 3L) {
            Vfx.glint(level, c, 16777215, 0.4 + 0.2 * scar[0]);
            Vfx.smoke(level, c, 657932, 1, 0.2, 0.9);
         } else {
            scar[0] = 0L;
            zap(level, p, t, 8.0F);
            t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 1), p);
            GearState.of(p).addResonance("voidreaver", 15.0F);
            Vfx.gather(level, c, MONO, 1.4, 10);
            Vfx.flare(level, c, 16777215, 1.6, 6);
            Vfx.ring(level, c, 16777215, 1.6, 10);
            Vfx.shards(level, c, 16777215, 10, 0.35);
            Vfx.smoke(level, c, 657932, 4, 0.5, 1.2);
            GearKit.procSound(level, t, "voidreaver");
         }
      }
   }

   static boolean tailTempest(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      Vec3 feet = p.position();
      Vec3 chest = chest(p);
      float yaw = p.getYRot();

      for (LivingEntity e : near(level, p, chest, 5.5)) {
         cut(level, p, e, 11.0F * u.power());
         fling(e, feet, 1.2, 0.45);
         GearKit.hitSpark(level, e, MONO);
         u.resonate(4.0F);
      }

      Vfx.slashArc(level, chest, yaw, 0.0F, 5.5, MONO, 330.0F);
      Vfx.slashArc(level, chest, yaw, 0.0F, 12.0F, 4.8, MONO, -330.0F);

      for (int i = 0; i < 8; i++) {
         double a = i * Math.PI / 4.0;
         Vec3 d = new Vec3(Math.sin(a), 0.0, Math.cos(a));
         Vfx.streaks(level, chest.add(d.scale(1.2)), d, MONO, 2);
         Vfx.smoke(level, feet.add(d.scale(1.8)).add(0.0, 0.25, 0.0), 657932, 1, 0.25, 1.2);
      }

      GearKit.kick(level, p, 0.2);
      if (u.empowered()) {
         float power = u.power();
         later(level, 8, () -> {
            if (alive(p, level)) {
               Vec3 f = p.position();
               Vec3 c = chest(p);
               Vfx.slashArc(level, c, p.getYRot(), 0.0F, 7.5, MONO, -330.0F);
               Vfx.vortex(level, f, 3.0, 2.0, MONO, 10);
               Vfx.smoke(level, f.add(0.0, 0.4, 0.0), 657932, 6, 2.0, 1.4);

               for (LivingEntity e : near(level, p, c, 7.5)) {
                  cut(level, p, e, 9.0F * power);
                  if (movable(e)) {
                     Vec3 in = new Vec3(f.x - e.getX(), 0.0, f.z - e.getZ());
                     in = in.lengthSqr() < 1.0E-4 ? Vec3.ZERO : in.normalize().scale(0.6);
                     e.setDeltaMovement(in.x, 0.6, in.z);
                     e.syncVelocity = true;
                  }

                  GearKit.hitSpark(level, e, MONO);
               }

               GearKit.sound(level, p, 1.2F, 0.8F, "weapon.wyrmspine_glaive.use");
               GearKit.kick(level, p, 0.25);
            }
         });
      }

      return true;
   }

   static boolean talonRake(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      Vec3 feet = p.position();
      Vec3 chest = chest(p);
      Vec3 dir = GearKit.flatLook(p);
      float yaw = p.getYRot();
      BlockHitResult wall = ray(level, p, chest, dir, 10.0);
      double length = wall.getType() == Type.MISS ? 10.0 : Math.max(1.5, wall.getLocation().distanceTo(chest) - 0.4);
      List<LivingEntity> hit = strip(level, p, feet, dir, length + 0.5, 1.6, 3.0);
      GearKit.dash(p, 2.4, 0.2);

      for (LivingEntity e : hit) {
         cut(level, p, e, 12.0F * u.power());
         lift(e, 0.8);
         Vfx.claw(level, center(e), yaw, 0.0F, 2.0, MONO);
         GearKit.hitSpark(level, e, MONO);
      }

      Vfx.dashTrail(level, chest, chest.add(dir.scale(length)), MONO);
      Vfx.streaks(level, chest, dir, MONO, 10);

      for (double k = 0.0; k <= length; k += 2.0) {
         Vfx.smoke(level, feet.add(dir.scale(k)).add(0.0, 0.6, 0.0), 657932, 1, 0.25, 1.1);
      }

      if (!hit.isEmpty()) {
         GearState state = u.state();
         int left = state.remaining("wyrmspine_glaive.primary");
         if (left > 0) {
            state.cooldown("wyrmspine_glaive.primary", Math.max(1, left - 40));
         }
      }

      if (u.empowered()) {
         rift(level, p, feet, dir, Math.max(4.0, length), 10, 8.0F * u.power(), 3.0, "wyrmspine_glaive", 0.0F);
      }

      GearKit.kick(level, p, 0.2);
      return true;
   }

   static void javelinLevel(ServerLevel level, Player p, WeaponKit kit, int charge, boolean max) {
      Vec3 t = GearKit.tip(p);
      Vfx.gather(level, t, MONO, 1.2, 8);
      Vfx.glint(level, t, 16777215, 0.5 + 0.3 * charge);
      Vfx.smoke(level, t, 657932, 1, 0.1, 0.9);
      if (max) {
         Vfx.flare(level, t, 16777215, 1.6, 6);
         GearKit.kick(level, p, 0.12);
      }
   }

   static boolean javelin(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      int lvl = Math.max(1, u.charge());
      float damage = (10 + 5 * lvl) * u.power();
      float power = u.power();
      boolean pin = lvl >= 3;
      boolean back = u.empowered();
      Vec3 look = p.getLookAngle();
      EnderDragonArsenal.Tracer t = new EnderDragonArsenal.Tracer(level, p, GearKit.eye(p).add(look.scale(0.8)).add(0.0, -0.15, 0.0), look.scale(2.2));
      t.gravity = 0.015;
      t.life = 40;
      t.radius = 0.8;
      t.size = 0.6;
      t.pierce = lvl;
      t.draw = EnderDragonArsenal::spineDraw;
      t.onHit = (tr, e) -> {
         zap(level, p, e, damage);
         Vec3 c = center(e);
         Vfx.impact(level, c, MONO, 0.6);
         Vfx.shards(level, c, 16777215, 5, 0.3);
         Vfx.glint(level, c, 16777215, 0.8);
      };
      t.onEnd = (tr, at, why) -> {
         if (why == 1) {
            Vfx.impact(level, at, MONO, 0.5);
            Vfx.smoke(level, at, 657932, 2, 0.3, 1.0);
         }

         if (pin) {
            pinned(level, p, tr.stopped, at, 10.0F * power);
         }

         if (back) {
            spineReturn(level, p, at, damage * 0.6F);
         }
      };
      every(level, t);
      GearKit.muzzle(level, p, MONO, 1.2);
      Vfx.streaks(level, GearKit.eye(p), look, MONO, 6);
      GearKit.kick(level, p, 0.2);
      return true;
   }

   private static void pinned(ServerLevel level, Player p, @Nullable LivingEntity on, Vec3 at, float damage) {
      if (on != null && on.isAlive()) {
         on.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 12, 3), p);
      }

      every(level, age -> {
         Vec3 c = on != null && on.isAlive() && !on.isRemoved() && on.level() == level ? center(on) : at;
         if (age < 12) {
            if (age % 3 == 0) {
               Vfx.flare(level, c, 16777215, 0.6 + age * 0.08, 4);
               Vfx.smoke(level, c, 657932, 1, 0.1, 0.8);
            }

            return true;
         } else {
            if (!alive(p, level)) {
               return false;
            }

            Vfx.nova(level, c, MONO, 3.5);
            Vfx.smoke(level, c, 657932, 12, 1.0, 1.8);
            Vfx.shards(level, c, 16777215, 10, 0.4);

            for (LivingEntity e : near(level, p, c, 3.5)) {
               zap(level, p, e, damage);
               lift(e, 0.9);
            }

            soundAt(level, c, 1.3F, 1.0F, "weapon.wyrmspine_glaive.burst", "weapon.wyrmspine_glaive.release");
            return false;
         }
      });
   }

   private static void spineReturn(ServerLevel level, Player p, Vec3 from, float damage) {
      Vec3 to = chest(p).subtract(from);
      EnderDragonArsenal.Tracer t = new EnderDragonArsenal.Tracer(
         level, p, from, (to.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 1.0, 0.0) : to.normalize()).scale(0.8)
      );
      t.maxSpeed = 2.4;
      t.ramp = 0.4;
      t.homing = 0.5;
      t.home = () -> chest(p);
      t.ghost = true;
      t.pierce = 64;
      t.life = 40;
      t.radius = 0.8;
      t.size = 0.6;
      t.draw = EnderDragonArsenal::spineDraw;
      t.onHit = (tr, e) -> {
         zap(level, p, e, damage);
         Vfx.impact(level, center(e), MONO, 0.45);
         Vfx.shards(level, center(e), 16777215, 4, 0.3);
      };
      t.onEnd = (tr, at, why) -> {
         Vfx.glint(level, why == 3 ? GearKit.tip(p) : at, 16777215, 1.0);
         Vfx.flare(level, why == 3 ? GearKit.tip(p) : at, 16777215, 0.9, 4);
      };
      every(level, t);
   }

   static void skyrend(ServerLevel level, LivingEntity a, LivingEntity t, ItemStack s) {
      if (a instanceof Player p) {
         if (!t.onGround() && !t.isInWater()) {
            zap(level, p, t, 6.0F);
            if (movable(t)) {
               Vec3 v = t.getDeltaMovement();
               t.setDeltaMovement(v.x * 0.5, -1.4, v.z * 0.5);
               t.syncVelocity = true;
            }

            Vec3 c = center(t);
            Vfx.ring(level, t.position(), 16777215, 1.4, 8);
            Vfx.streaks(level, c, new Vec3(0.0, -1.0, 0.0), MONO, 6);
            Vfx.glint(level, c, 16777215, 0.9);
            Vfx.smoke(level, c, 657932, 1, 0.3, 1.0);
            GearKit.procSound(level, t, "wyrmspine_glaive");
            GearState.of(p).addResonance("wyrmspine_glaive", 5.0F);
         } else {
            lift(t, 0.35);
            Vfx.ring(level, t.position(), 16777215, 0.9, 6);
         }
      }
   }

   static boolean endOrbs(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      LivingEntity target = orbTarget(level, p);
      Vec3 look = p.getLookAngle();
      Vec3 tip = GearKit.tip(p);
      Vec3 side = look.cross(new Vec3(0.0, 1.0, 0.0));
      side = side.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
      Vec3 up = side.cross(look).normalize();
      boolean emp = u.empowered();
      int n = emp ? 5 : 3;
      float spread = emp ? 15.0F : 25.0F;
      float damage = 7.0F * u.power();

      for (int i = 0; i < n; i++) {
         float fan = (i - (n - 1) / 2.0F) * spread;
         double a = Math.toRadians(fan * 2.0F);
         Vec3 at = tip.add(side.scale(Math.sin(a) * 0.8)).add(up.scale(Math.cos(a) * 0.8));
         EnderDragonArsenal.Tracer t = new EnderDragonArsenal.Tracer(level, p, at, look.yRot((float)Math.toRadians(-fan)).scale(0.6));
         t.maxSpeed = 1.1;
         t.ramp = 0.05;
         t.homing = 0.12;
         t.target = target;
         t.life = 60;
         t.radius = 0.7;
         t.size = 0.45;
         t.draw = EnderDragonArsenal::orbDraw;
         t.onHit = (tr, e) -> orbImpact(level, p, tr.pos, e, damage, emp);
         t.onEnd = (tr, at2, why) -> {
            if (why == 1) {
               orbImpact(level, p, at2, null, damage, emp);
            } else if (why == 0) {
               Vfx.flare(level, at2, 16777215, 0.7, 4);
               Vfx.smoke(level, at2, 657932, 2, 0.2, 0.9);
            }
         };
         every(level, t);
      }

      Vfx.gather(level, tip, MONO, 1.2, 8);
      GearKit.muzzle(level, p, MONO, 1.0);
      return true;
   }

   private static @Nullable LivingEntity orbTarget(ServerLevel level, Player p) {
      LivingEntity best = GearKit.lookTarget(level, p, 32.0);
      if (best != null) {
         return best;
      }

      Vec3 eye = GearKit.eye(p);
      Vec3 look = p.getLookAngle();
      double bestD = Double.MAX_VALUE;

      for (LivingEntity e : GearKit.enemies(level, p, eye, 24.0)) {
         Vec3 to = center(e).subtract(eye);
         double along = to.dot(look);
         if (!(along <= 0.0) && isMarked(level, e)) {
            double off = to.subtract(look.scale(along)).length() / along;
            if (off < bestD && p.hasLineOfSight(e)) {
               best = e;
               bestD = off;
            }
         }
      }

      if (best != null) {
         return best;
      }

      Vec3 aim = GearKit.lookPoint(level, p, 32.0);
      bestD = 256.0;

      for (LivingEntity e : GearKit.enemies(level, p, aim, 16.0)) {
         double d = e.distanceToSqr(aim);
         if (d < bestD && p.hasLineOfSight(e)) {
            best = e;
            bestD = d;
         }
      }

      return best;
   }

   private static void orbImpact(ServerLevel level, Player p, Vec3 at, @Nullable LivingEntity e, float damage, boolean empowered) {
      if (e != null) {
         zap(level, p, e, damage * mark(level, e));
         GearState.of(p).addResonance("endseer_staff", 3.0F);
      }

      for (LivingEntity o : near(level, p, at, 1.5)) {
         if (o != e) {
            zap(level, p, o, damage * 0.5F * mark(level, o));
         }
      }

      Vfx.impact(level, at, MONO, 0.5);
      Vfx.ring(level, at, 16777215, 1.2, 8);
      Vfx.smoke(level, at, 657932, 1, 0.2, 1.0);
      soundAt(level, at, 0.9F, 1.1F + level.getRandom().nextFloat() * 0.1F, "weapon.endseer_staff.orb", "weapon.endseer_staff.proc");
      if (empowered) {
         singularity(level, p, at);
      }
   }

   private static void singularity(ServerLevel level, Player p, Vec3 at) {
      Vfx.vortex(level, at, 1.5, 1.5, MONO, 20);
      every(level, age -> {
         if (age <= 20 && alive(p, level)) {
            for (LivingEntity e : near(level, p, at, 2.5)) {
               Vec3 d = at.subtract(center(e));
               if (d.lengthSqr() > 0.04) {
                  nudge(e, d.normalize().scale(0.08));
               }
            }

            if (age % 5 == 0) {
               Vfx.smoke(level, at, 657932, 1, 0.3, 0.9);
            }

            return true;
         } else {
            return false;
         }
      });
   }

   static boolean eventHorizon(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      Vec3 aim = GearKit.lookPoint(level, p, 24.0);
      Vec3 g = floor(level, aim.x, aim.y, aim.z, 2, 8);
      Vec3 w = g != null ? g : aim;
      boolean emp = u.empowered();
      double r = emp ? 7.0 : 5.0;
      double pullR = emp ? 8.0 : 6.0;
      int collapse = emp ? 80 : 60;
      float power = u.power();
      Vfx.telegraphCircle(level, w, r, 10, MONO);
      Vfx.runeCircle(level, w, r, MONO, collapse + 10, 0.06F, Vfx.Rune.ASTRAL);
      Vfx.smoke(level, w.add(0.0, 0.3, 0.0), 657932, 6, r * 0.5, 1.4);
      Vfx.beam(level, GearKit.tip(p), w.add(0.0, 0.5, 0.0), MONO, 0.12, 6);
      Vec3 core = w.add(0.0, 0.6, 0.0);
      every(level, age -> {
         if (!alive(p, level)) {
            return false;
         }

         if (age >= collapse) {
            Vfx.nova(level, core, MONO, r);
            Vfx.heavyImpact(level, w, MONO, 1.0);
            Vfx.smoke(level, core, 657932, 14, r * 0.4, 2.0);
            Vfx.shards(level, core, 16777215, 12, 0.45);

            for (LivingEntity e : near(level, p, core, r)) {
               zap(level, p, e, 14.0F * power * mark(level, e));
               if (movable(e)) {
                  Vec3 v = e.getDeltaMovement();
                  e.setDeltaMovement(v.x * 0.3, -0.8, v.z * 0.3);
                  e.syncVelocity = true;
               }

               e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1), p);
               GearState.of(p).addResonance("endseer_staff", 4.0F);
            }

            soundAt(level, core, 1.6F, emp ? 1.0F : 1.1F, "weapon.endseer_staff.collapse", "weapon.endseer_staff.alt");
            return false;
         } else {
            if (age == 10) {
               Vfx.vortex(level, w, r, emp ? 6.0 : 5.0, MONO, collapse - 10);
            }

            if (age >= 10) {
               for (LivingEntity e : near(level, p, core, pullR)) {
                  Vec3 d = new Vec3(core.x - e.getX(), 0.0, core.z - e.getZ());
                  double dist = d.length();
                  if (dist > 0.3) {
                     double s = 0.1 * Math.min(1.0, dist / 2.0);
                     nudge(e, new Vec3(d.x / dist * s, -0.03, d.z / dist * s));
                  }
               }

               if ((age - 10) % 10 == 0) {
                  for (LivingEntity e : near(level, p, core, 3.0)) {
                     zap(level, p, e, 3.0F * power * mark(level, e));
                     Vfx.shards(level, center(e), 16777215, 3, 0.2);
                  }
               }

               if (age % 6 == 0) {
                  double a = age * 0.45;
                  Vfx.smoke(level, core.add(Math.sin(a) * r * 0.8, 0.2, Math.cos(a) * r * 0.8), 657932, 1, 0.2, 1.2);
                  Vfx.glint(level, core.add(0.0, 0.4, 0.0), 16777215, 0.5 + 0.5 * age / collapse);
               }
            }

            return true;
         }
      });
      return true;
   }

   static void lanceLevel(ServerLevel level, Player p, WeaponKit kit, int charge, boolean max) {
      boolean pierce = GearState.of(p).resonance(kit.id()) >= 40.0F;
      Vec3 eye = GearKit.eye(p);
      Vec3 look = p.getLookAngle();
      double length = 16 + 6 * charge;
      Vec3 end = pierce ? eye.add(look.scale(length)) : ray(level, p, eye, look, length).getLocation();
      Vec3 t = GearKit.tip(p);
      Vfx.beam(level, t, end, MONO, 0.08, 12);
      if (charge == 1) {
         Vfx.runeCircle(level, groundAt(level, p.position()), 1.5, MONO, 50, 0.05F, Vfx.Rune.ASTRAL);
      }

      if (max) {
         Vfx.chargeUp(level, t, MONO, 6);
         Vfx.glint(level, t, 16777215, 1.2);
      } else {
         Vfx.glint(level, t, 16777215, 0.4 + 0.2 * charge);
      }
   }

   static boolean voidLance(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      int lvl = Math.max(1, u.charge());
      float power = u.power();
      boolean emp = u.empowered();
      boolean full = lvl >= 4;
      Vec3 eye = GearKit.eye(p);
      Vec3 look = p.getLookAngle();
      double length = 16 + 6 * lvl;
      BlockHitResult hit = ray(level, p, eye, look, length);
      boolean blocked = !emp && hit.getType() != Type.MISS;
      Vec3 end = blocked ? hit.getLocation() : eye.add(look.scale(length));
      double dist = end.distanceTo(eye);
      Vfx.beam(level, eye.add(look.scale(0.8)), end, MONO, 0.9 + 0.15 * lvl, full ? 20 : 6);
      double step = Math.max(1.5, dist / 16.0);

      for (double k = 1.5; k < dist; k += step) {
         Vfx.smoke(level, eye.add(look.scale(k)), 657932, 1, 0.15, 1.2);
      }

      if (blocked && hit.getDirection() == Direction.UP) {
         Vfx.groundCrack(level, end, 1.5, MONO);
      }

      Vfx.impact(level, end, MONO, 0.8);

      for (LivingEntity e : GearKit.alongLine(level, p, eye, look, dist, 1.2)) {
         zap(level, p, e, (8 + 4 * lvl) * power * mark(level, e));
         nudge(e, look.scale(0.4));
      }

      if (full) {
         Vfx.impactFrame(level, eye, 24.0, 16777215, 0.5F);

         for (int i = 1; i <= 4; i++) {
            later(level, 5 * i, () -> {
               if (alive(p, level)) {
                  for (LivingEntity ex : GearKit.alongLine(level, p, eye, look, dist, 1.2)) {
                     zap(level, p, ex, 4.0F * power * mark(level, ex));
                  }

                  for (int k = 1; k <= 3; k++) {
                     Vfx.shards(level, eye.add(look.scale(dist * k / 4.0)), 16777215, 3, 0.25);
                  }
               }
            });
         }
      }

      if (emp) {
         List<Vec3> chain = new ArrayList<>();
         chain.add(end);
         Set<LivingEntity> struck = new LinkedHashSet<>();
         Vec3 from = end;

         for (int i = 0; i < 3; i++) {
            LivingEntity next = null;
            double bestD = 64.0;

            for (LivingEntity e : GearKit.enemies(level, p, end, 8.0)) {
               double d = center(e).distanceToSqr(from);
               if (!struck.contains(e) && d < bestD) {
                  next = e;
                  bestD = d;
               }
            }

            if (next == null) {
               break;
            }

            struck.add(next);
            from = center(next);
            chain.add(from);
         }

         if (chain.size() > 1) {
            Vfx.chainLightning(level, chain, MONO);

            for (LivingEntity e : struck) {
               zap(level, p, e, 8.0F * power);
               Vfx.glint(level, center(e), 16777215, 0.8);
            }
         }
      }

      GearKit.kick(level, p, 0.2 + 0.05 * lvl);
      return true;
   }

   static void endsight(ServerLevel level, LivingEntity a, LivingEntity t, ItemStack s) {
      if (a instanceof Player p) {
         boolean fresh = !isMarked(level, t);
         MARKS.put(t, level.getGameTime() + 160L);
         t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 160, 0, false, false), p);
         if (fresh) {
            GearState.of(p).addResonance("endseer_staff", 10.0F);
         }

         Vfx.flare(level, t.position().add(0.0, t.getBbHeight() + 0.6, 0.0), 16777215, 0.9, 8);
         Vfx.glint(level, t.getEyePosition(), 16777215, 0.8);
         Vfx.ring(level, center(t), 16777215, 1.0, 8);
         GearKit.procSound(level, t, "endseer_staff");
      }
   }

   static void aegisTick(ServerLevel level, Player p, int pieces) {
      long now = level.getGameTime();
      EnderDragonArsenal.Aegis a = AEGIS.get(p.getUUID());
      if (a == null || a.seen < now - 2L) {
         a = new EnderDragonArsenal.Aegis();
         a.nextRegen = now + 240L;
         AEGIS.put(p.getUUID(), a);
      }

      a.seen = now;
      Vec3 chest = chest(p);
      if (a.shards >= 3) {
         a.nextRegen = now + 240L;
      } else if (now >= a.nextRegen) {
         a.shards++;
         a.nextRegen = now + 240L;
         Vfx.flare(level, chest, 16777215, 0.8, 6);
         Vfx.glint(level, chest, 16777215, 0.7);
      }

      if (a.shards > 0 && now % 40L == 0L) {
         Vfx.wisps(level, chest, MONO, a.shards, 1.1, 44);

         for (int i = 0; i < a.shards; i++) {
            double ang = now * 0.1 + i * Math.PI * 2.0 / 3.0;
            Vfx.glint(level, chest.add(Math.sin(ang) * 1.1, 0.1, Math.cos(ang) * 1.1), 16777215, 0.35);
         }
      }
   }

   static boolean guard(ServerLevel level, Player p, DamageSource source, float amount, int pieces) {
      if (source.is(DamageTypeTags.IS_FALL)) {
         dragonfall(level, p, amount);
         return false;
      } else {
         return pieces < 4 || !aegisBlock(level, p, source, amount);
      }
   }

   private static void dragonfall(ServerLevel level, Player p, float amount) {
      GearState state = GearState.of(p);
      if (!(amount < 3.0F) && state.ready("ender_dragon.fall")) {
         state.cooldown("ender_dragon.fall", 20);
         Vec3 feet = p.position();
         double r = Math.min(2.5 + amount * 0.25, 6.0);

         for (LivingEntity e : near(level, p, feet.add(0.0, 0.5, 0.0), r)) {
            zap(level, p, e, Math.min(amount * 1.5F, 24.0F));
            fling(e, feet, 0.8, 0.4);
         }

         Vfx.heavyImpact(level, feet, MONO, Math.min(0.4 + amount / 16.0F, 1.3));
         Vfx.groundCrack(level, feet, r, MONO);
         Vfx.smoke(level, feet.add(0.0, 0.3, 0.0), 657932, 6, r * 0.4, 1.4);
         GearKit.sound(level, p, 0.6F + amount / 20.0F, 0.9F, "weapon.voidreaver.land", "weapon.voidreaver.alt");
      }
   }

   private static boolean aegisBlock(ServerLevel level, Player p, DamageSource source, float amount) {
      long now = level.getGameTime();
      EnderDragonArsenal.Aegis a = AEGIS.get(p.getUUID());
      if (a == null || a.seen < now - 2L || amount < 3.0F || source.getEntity() == null || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         return false;
      }

      if (now < a.guardUntil) {
         return true;
      }

      if (a.shards > 0 && p.getInvulnerableTime() <= 10) {
         a.shards--;
         a.guardUntil = now + 10L;
         Vec3 c = chest(p);
         Set<LivingEntity> hit = new LinkedHashSet<>(near(level, p, c, 3.0));
         if (source.getEntity() instanceof LivingEntity attacker
            && !(attacker instanceof Player)
            && attacker.isAlive()
            && attacker.level() == level
            && attacker.distanceToSqr(p) < 2304.0) {
            hit.add(attacker);
         }

         for (LivingEntity e : hit) {
            zap(level, p, e, 6.0F);
         }

         Vfx.shards(level, c, 16777215, 14, 0.35);
         Vfx.nova(level, c, MONO, 2.5);
         Vfx.smoke(level, c, 657932, 4, 0.6, 1.3);
         Entity from = source.getDirectEntity() != null ? source.getDirectEntity() : source.getEntity();
         if (from != null && from != p) {
            Vfx.glint(level, from.getBoundingBox().getCenter(), 16777215, 1.0);
         }

         soundAt(level, c, 1.0F, 1.15F + level.getRandom().nextFloat() * 0.1F, "weapon.endseer_staff.shard", "weapon.endseer_staff.proc");
         return true;
      } else {
         return false;
      }
   }

   static boolean lastLight(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      Vec3 v = p.getDeltaMovement();
      p.setDeltaMovement(v.x * 0.3, 0.9, v.z * 0.3);
      p.syncVelocity = true;
      p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 30, 0, false, false, true));
      p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 30, 2, false, false, true));
      Vec3 chest = chest(p);
      double rise = 4.4;
      BlockHitResult roof = ray(level, p, p.getEyePosition(), new Vec3(0.0, 1.0, 0.0), rise + 0.3);
      if (roof.getType() != Type.MISS) {
         rise = Math.max(0.0, roof.getLocation().y - p.getEyeY() - 0.3);
      }

      Vec3 apex = chest.add(0.0, rise, 0.0);
      Vfx.chargeUp(level, apex, MONO, 20);
      Vfx.shieldBubble(level, chest, 1.6, MONO, 12);
      Vfx.runeCircle(level, groundAt(level, p.position()), 6.0, MONO, 30, 0.05F, Vfx.Rune.ASTRAL);
      Vfx.smoke(level, p.position().add(0.0, 0.3, 0.0), 657932, 6, 1.2, 1.5);
      every(level, age -> {
         if (age <= 20 && alive(p, level)) {
            Vec3 c = chest(p);
            if (age % 2 == 0) {
               double rr = Math.max(0.6, 2.8 - age * 0.11);

               for (int k = 0; k < 2; k++) {
                  double ang = age * 0.55 + k * Math.PI;
                  Vfx.smoke(level, c.add(Math.sin(ang) * rr, k == 0 ? -0.4 : 0.4, Math.cos(ang) * rr), 657932, 1, 0.1, 1.2);
               }
            }

            if (age == 10) {
               Vfx.shieldBubble(level, c, 1.6, MONO, 10);
            }

            if (age < 20) {
               return true;
            }

            Vfx.impactFrame(level, c, 24.0, 16777215, 0.5F);
            Vfx.nova(level, c, MONO, 9.0);
            Vfx.explosion(level, c, MONO, 3.0);
            Vfx.smoke(level, c, 657932, 16, 1.5, 2.2);
            Vfx.shards(level, c, 16777215, 20, 0.6);

            for (LivingEntity e : near(level, p, c, 9.0)) {
               zap(level, p, e, gap(e, c) <= 4.0 ? 24.0F : 16.0F);
               fling(e, c, 1.4, 0.5);
               e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1), p);
               e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false), p);
            }

            p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));
            EnderDragonArsenal.Aegis a = AEGIS.get(p.getUUID());
            if (a != null) {
               a.shards = 3;
               Vfx.wisps(level, c, MONO, 3, 1.1, 44);
            }

            soundAt(level, c, 2.0F, 0.8F, "weapon.endseer_staff.last_light", "armor.void_dragon_armor.ability");
            return false;
         } else {
            return false;
         }
      });
      return true;
   }

   private static final class Aegis {
      private int shards;
      private long nextRegen;
      private long seen;
      private long guardUntil;
   }

   private static final class Live {
      private final ServerLevel level;
      private final EnderDragonArsenal.Ticker ticker;
      private int age;

      private Live(ServerLevel level, EnderDragonArsenal.Ticker ticker) {
         this.level = level;
         this.ticker = ticker;
      }
   }

   private record Task(ServerLevel level, long due, Runnable run) {
   }

   @FunctionalInterface
   private interface Ticker {
      boolean tick(int var1);
   }

   private static final class Tracer implements EnderDragonArsenal.Ticker {
      private final ServerLevel level;
      private final Player owner;
      private Vec3 pos;
      private Vec3 vel;
      private double maxSpeed;
      private double ramp;
      private double homing;
      private double gravity;
      private double radius = 0.6;
      private double size = 0.5;
      private int life = 60;
      private int pierce;
      private boolean ghost;
      private @Nullable LivingEntity target;
      private @Nullable Supplier<Vec3> home;
      private @Nullable LivingEntity stopped;
      private final Set<Integer> hit = new HashSet<>();
      private EnderDragonArsenal.TracerHit onHit = (t, e) -> {};
      private EnderDragonArsenal.TracerEnd onEnd = (t, at, why) -> {};
      private EnderDragonArsenal.TracerDraw draw = EnderDragonArsenal::plainDraw;

      private Tracer(ServerLevel level, Player owner, Vec3 pos, Vec3 vel) {
         this.level = level;
         this.owner = owner;
         this.pos = pos;
         this.vel = vel;
         this.maxSpeed = vel.length();
      }

      private @Nullable Vec3 goal() {
         if (this.home != null) {
            return this.home.get();
         }

         LivingEntity t = this.target;
         return t != null && t.isAlive() && !t.isRemoved() && t.level() == this.level ? t.getBoundingBox().getCenter() : null;
      }

      @Override
      public boolean tick(int age) {
         if (!EnderDragonArsenal.alive(this.owner, this.level)) {
            return false;
         }

         if (age > this.life) {
            this.onEnd.end(this, this.pos, 0);
            return false;
         }

         double speed = this.vel.length();
         if (this.ramp > 0.0) {
            speed = Math.min(this.maxSpeed, speed + this.ramp);
         }

         Vec3 goal = this.goal();
         if (goal != null && this.homing > 0.0 && speed > 1.0E-4) {
            Vec3 want = goal.subtract(this.pos);
            if (want.lengthSqr() > 1.0E-4) {
               Vec3 steer = this.vel.scale(1.0 / Math.max(1.0E-4, this.vel.length())).lerp(want.normalize(), this.homing);
               this.vel = (steer.lengthSqr() < 1.0E-6 ? want.normalize() : steer.normalize()).scale(speed);
            }
         } else if (this.ramp > 0.0 && this.vel.lengthSqr() > 1.0E-6) {
            this.vel = this.vel.normalize().scale(speed);
         }

         this.vel = this.vel.add(0.0, -this.gravity, 0.0);
         Vec3 from = this.pos;
         Vec3 to = from.add(this.vel);
         if (!this.level.isLoaded(BlockPos.containing(to))) {
            this.onEnd.end(this, from, 0);
            return false;
         }

         Vec3 wall = null;
         if (!this.ghost) {
            BlockHitResult bh = this.level.clip(new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, this.owner));
            if (bh.getType() != Type.MISS) {
               wall = bh.getLocation();
               to = wall;
            }
         }

         List<LivingEntity> found = new ArrayList<>();
         Vec3 a = from;
         Vec3 b = to;

         for (LivingEntity e : GearKit.enemies(this.level, this.owner, a.add(b).scale(0.5), a.distanceTo(b) / 2.0 + this.radius + 4.0)) {
            AABB box = e.getBoundingBox().inflate(this.radius);
            if (!this.hit.contains(e.getId()) && (box.contains(a) || box.clip(a, b).isPresent())) {
               found.add(e);
            }
         }

         found.sort(Comparator.comparingDouble(ex -> ex.getBoundingBox().getCenter().distanceToSqr(a)));

         for (LivingEntity e : found) {
            this.hit.add(e.getId());
            AABB box = e.getBoundingBox().inflate(this.radius);
            this.pos = box.contains(a) ? a : box.clip(a, b).orElse(a);
            this.onHit.hit(this, e);
            if (this.pierce-- <= 0) {
               this.stopped = e;
               this.onEnd.end(this, this.pos, 2);
               return false;
            }
         }

         if (wall != null) {
            this.pos = wall;
            this.onEnd.end(this, wall, 1);
            return false;
         } else {
            this.pos = b;
            if (this.home != null && EnderDragonArsenal.segDist(a, b, this.home.get()) < 1.5) {
               this.onEnd.end(this, b, 3);
               return false;
            } else {
               this.draw.draw(this, age);
               return true;
            }
         }
      }
   }

   @FunctionalInterface
   private interface TracerDraw {
      void draw(EnderDragonArsenal.Tracer var1, int var2);
   }

   @FunctionalInterface
   private interface TracerEnd {
      void end(EnderDragonArsenal.Tracer var1, Vec3 var2, int var3);
   }

   @FunctionalInterface
   private interface TracerHit {
      void hit(EnderDragonArsenal.Tracer var1, LivingEntity var2);
   }
}
