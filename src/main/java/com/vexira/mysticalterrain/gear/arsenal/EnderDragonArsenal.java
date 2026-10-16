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
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStopped;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
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
         .charge(Charge.of(3, 12).perLevel(0.4F).pose(UseAction.TRIDENT).onLevel(EnderDragonArsenal::edgeLevel))
         .passive(EnderDragonArsenal::riftscar)
         .resonancePerHit(12.0F);
      this.weapon("wyrmspine_glaive", 10.0F, -2.7F)
         .primary(Active.of(140, EnderDragonArsenal::tailTempest).empowered(40))
         .alternate(Active.of(180, EnderDragonArsenal::talonRake).empowered(40))
         .charge(
            Charge.of(3, 10).pose(UseAction.TRIDENT).onLevel(EnderDragonArsenal::javelinLevel).cast(Active.of(240, EnderDragonArsenal::javelin).empowered(50))
         )
         .passive(EnderDragonArsenal::skyrend)
         .resonancePerHit(10.0F);
      this.weapon("endseer_staff", 5.0F, -2.4F)
         .primary(Active.of(80, EnderDragonArsenal::endOrbs).empowered(30))
         .alternate(Active.of(320, EnderDragonArsenal::eventHorizon).empowered(60))
         .charge(Charge.of(4, 10).pose(UseAction.BOW).onLevel(EnderDragonArsenal::lanceLevel).cast(Active.of(280, EnderDragonArsenal::voidLance).empowered(40)))
         .passive(EnderDragonArsenal::endsight)
         .resonancePerHit(8.0F);
   }

   @Override
   protected void armor(ArmorKit set) {
      set.toughness(4.0).knockback(0.125);
      set.twoPiece((level, p, pieces) -> {
         if (level.getTime() % 10L == 0L && p.hasStatusEffect(StatusEffects.DARKNESS)) {
            p.removeStatusEffect(StatusEffects.DARKNESS);
         }
      });
      set.fourPiece(EnderDragonArsenal::aegisTick);
      set.allowDamage(EnderDragonArsenal::guard);
      set.active(600, EnderDragonArsenal::lastLight);
   }

   private static void hook() {
      if (!hooked) {
         hooked = true;
         ServerTickEvents.END_WORLD_TICK.register(EnderDragonArsenal::tickWorld);
         ServerLifecycleEvents.SERVER_STOPPED.register((ServerStopped)server -> {
            TASKS.clear();
            LIVE.clear();
            SCARS.clear();
            MARKS.clear();
            AEGIS.clear();
         });
      }
   }

   private static void later(ServerWorld level, int ticks, Runnable run) {
      TASKS.add(new EnderDragonArsenal.Task(level, level.getTime() + Math.max(1, ticks), run));
   }

   private static void every(ServerWorld level, EnderDragonArsenal.Ticker ticker) {
      LIVE.add(new EnderDragonArsenal.Live(level, ticker));
   }

   private static void tickWorld(ServerWorld level) {
      if (!TASKS.isEmpty()) {
         long now = level.getTime();
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

   static boolean alive(PlayerEntity p, ServerWorld level) {
      return p.isAlive() && !p.isRemoved() && p.getEntityWorld() == level;
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

   private static double segDist(Vec3d a, Vec3d b, Vec3d c) {
      Vec3d ab = b.subtract(a);
      double len = ab.lengthSquared();
      double k = len < 1.0E-8 ? 0.0 : MathHelper.clamp(c.subtract(a).dotProduct(ab) / len, 0.0, 1.0);
      return c.distanceTo(a.add(ab.multiply(k)));
   }

   static boolean movable(LivingEntity e) {
      return !(e instanceof MysticalBoss b && b.isBoss()) && !(e instanceof EnderDragonEntity) && !(e instanceof WitherEntity);
   }

   static void fling(LivingEntity e, Vec3d from, double out, double up) {
      if (movable(e)) {
         Vec3d away = new Vec3d(e.getX() - from.x, 0.0, e.getZ() - from.z);
         away = away.lengthSquared() < 1.0E-4 ? Vec3d.fromPolar(0.0F, e.getYaw()).multiply(-1.0) : away.normalize();
         Vec3d v = e.getVelocity();
         e.setVelocity(v.x * 0.3 + away.x * out, Math.max(v.y, 0.0) + up, v.z * 0.3 + away.z * out);
         e.knockedBack = true;
      }
   }

   static void lift(LivingEntity e, double up) {
      if (movable(e)) {
         Vec3d v = e.getVelocity();
         e.setVelocity(v.x, Math.max(v.y, 0.0) + up, v.z);
         e.knockedBack = true;
      }
   }

   static void nudge(LivingEntity e, Vec3d dv) {
      if (movable(e)) {
         e.setVelocity(e.getVelocity().add(dv));
         e.knockedBack = true;
      }
   }

   static void zap(ServerWorld level, PlayerEntity p, LivingEntity e, float damage) {
      e.timeUntilRegen = 0;
      GearKit.magic(level, p, e, damage);
   }

   static void cut(ServerWorld level, PlayerEntity p, LivingEntity e, float damage) {
      e.timeUntilRegen = 0;
      GearKit.strike(level, p, e, damage);
   }

   static List<LivingEntity> near(ServerWorld level, PlayerEntity p, Vec3d c, double r) {
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : GearKit.enemies(level, p, c, r + 4.0)) {
         if (gap(e, c) <= r) {
            out.add(e);
         }
      }

      return out;
   }

   static double gap(LivingEntity e, Vec3d c) {
      Box b = e.getBoundingBox();
      double dx = Math.max(0.0, Math.max(b.minX - c.x, c.x - b.maxX));
      double dy = Math.max(0.0, Math.max(b.minY - c.y, c.y - b.maxY));
      double dz = Math.max(0.0, Math.max(b.minZ - c.z, c.z - b.maxZ));
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   static List<LivingEntity> strip(ServerWorld level, PlayerEntity p, Vec3d from, Vec3d dir, double length, double width, double height) {
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity e : GearKit.enemies(level, p, from.add(dir.multiply(length / 2.0)), length / 2.0 + width + 4.0)) {
         double ox = e.getX() - from.x;
         double oz = e.getZ() - from.z;
         double along = ox * dir.x + oz * dir.z;
         double side = Math.abs(ox * dir.z - oz * dir.x);
         Box b = e.getBoundingBox();
         if (along > -width && along < length + width && side < width + e.getWidth() / 2.0F && b.maxY > from.y - 2.5 && b.minY < from.y + height + 1.0) {
            out.add(e);
         }
      }

      return out;
   }

   static @Nullable Vec3d floor(ServerWorld level, double x, double y, double z, int up, int down) {
      Mutable pos = BlockPos.ofFloored(x, y + up, z).mutableCopy();
      boolean air = false;

      for (int i = 0; i <= up + down; i++) {
         BlockState state = level.getBlockState(pos);
         VoxelShape shape = state.getCollisionShape(level, pos);
         boolean fluid = !state.getFluidState().isEmpty();
         if (shape.isEmpty() && !fluid) {
            air = true;
         } else if (air) {
            return new Vec3d(x, pos.getY() + (shape.isEmpty() ? 0.9 : shape.getMax(Axis.Y)), z);
         }

         pos.move(0, -1, 0);
      }

      return null;
   }

   static Vec3d groundAt(ServerWorld level, Vec3d at) {
      Vec3d g = floor(level, at.x, at.y, at.z, 2, 5);
      return g != null ? g : at;
   }

   static Vec3d center(LivingEntity e) {
      return e.getBoundingBox().getCenter();
   }

   static Vec3d chest(PlayerEntity p) {
      return p.getEntityPos().add(0.0, p.getHeight() * 0.62, 0.0);
   }

   static Vec3d flat(Vec3d v) {
      Vec3d f = new Vec3d(v.x, 0.0, v.z);
      return f.lengthSquared() < 1.0E-4 ? new Vec3d(0.0, 0.0, 1.0) : f.normalize();
   }

   static BlockHitResult ray(ServerWorld level, PlayerEntity p, Vec3d from, Vec3d dir, double length) {
      return level.raycast(new RaycastContext(from, from.add(dir.multiply(length)), ShapeType.COLLIDER, FluidHandling.NONE, p));
   }

   static void soundAt(ServerWorld level, Vec3d at, float volume, float pitch, String... keys) {
      for (String key : keys) {
         SoundEvent e = BossSounds.event(key);
         if (e != null) {
            level.playSound(null, at.x, at.y, at.z, e, SoundCategory.PLAYERS, volume, pitch);
            return;
         }
      }
   }

   static boolean isMarked(ServerWorld level, LivingEntity e) {
      Long until = MARKS.get(e);
      return until != null && until > level.getTime();
   }

   static float mark(ServerWorld level, LivingEntity e) {
      return isMarked(level, e) ? 1.3F : 1.0F;
   }

   static void rift(
      ServerWorld level, PlayerEntity p, Vec3d from, Vec3d dir, double length, int delay, float damage, double height, String weapon, float endBlast
   ) {
      Vec3d d = flat(dir);
      Vec3d start = groundAt(level, from);
      Vfx.telegraphLine(level, start, d, length, 2.2, delay, MONO);

      for (double k = 0.0; k <= length; k += 2.0) {
         Vfx.smoke(level, start.add(d.multiply(k)).add(0.0, 0.3, 0.0), 657932, 1, 0.3, 1.1);
      }

      later(level, delay, () -> {
         if (alive(p, level)) {
            int n = 0;

            for (double kx = 1.5; kx <= length; n++) {
               Vec3d q = start.add(d.multiply(kx));
               Vec3d g = floor(level, q.x, start.y, q.z, 3, 4);
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

            soundAt(level, start.add(d.multiply(length / 2.0)), 1.4F, 0.85F, "weapon.voidreaver.rift", "weapon.voidreaver.use");
            Vfx.shake(level, start, 14.0, 0.25, 6);
            if (endBlast > 0.0F) {
               Vec3d end = groundAt(level, start.add(d.multiply(length)));
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
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      Vec3d feet = p.getEntityPos();
      Vec3d chest = chest(p);
      Vec3d dir = GearKit.flatLook(p);
      float yaw = p.getYaw();
      float power = u.power();
      int lvl = u.charge();

      for (LivingEntity e : near(level, p, chest, 4.5)) {
         Vec3d to = center(e).subtract(chest);
         to = new Vec3d(to.x, 0.0, to.z);
         if (!(to.lengthSquared() > 1.4) || !(to.normalize().dotProduct(dir) < 0.342)) {
            cut(level, p, e, 12.0F * power);
            fling(e, feet, 0.5, 0.25);
            GearKit.hitSpark(level, e, MONO);
         }
      }

      Vfx.slashArc(level, chest, yaw, 0.0F, 18.0F, 4.5, MONO, 220.0F);
      Vfx.streaks(level, chest, p.getRotationVector(), MONO, 8);

      for (int k = -1; k <= 1; k++) {
         Vfx.smoke(level, chest.add(dir.rotateY(k * 0.7F).multiply(2.6)), 657932, 1, 0.3, 1.3);
      }

      Vfx.glint(level, GearKit.tip(p), 16777215, 0.9);
      GearKit.kick(level, p, 0.3 + 0.08 * lvl);
      double length = 10 + 4 * lvl;
      double height = 3 + lvl + (u.empowered() ? 1 : 0);
      float blast = u.fullyCharged() ? 10.0F * power : 0.0F;
      Vec3d from = feet.add(dir.multiply(1.5));
      if (u.empowered()) {
         for (int k = -1; k <= 1; k++) {
            rift(level, p, from, dir.rotateY((float)Math.toRadians(20 * k)), length, 6, 9.0F * power, height, "voidreaver", blast);
         }
      } else {
         rift(level, p, from, dir, length, 6, 9.0F * power, height, "voidreaver", blast);
      }

      if (u.fullyCharged()) {
         Vfx.impactFrame(level, chest, 20.0, 16777215, 0.45F);
      }

      return true;
   }

   static void edgeLevel(ServerWorld level, PlayerEntity p, WeaponKit kit, int charge, boolean max) {
      Vec3d feet = p.getEntityPos();
      Vec3d dir = GearKit.flatLook(p);
      Vec3d t = GearKit.tip(p);
      double length = 10 + 4 * charge;
      Vec3d from = groundAt(level, feet.add(dir.multiply(1.5)));
      if (GearState.of(p).resonance(kit.id()) >= 60.0F) {
         for (int k = -1; k <= 1; k++) {
            Vfx.telegraphLine(level, from, dir.rotateY((float)Math.toRadians(20 * k)), length, 2.2, 8, MONO);
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
      final ServerWorld level = u.level();
      final PlayerEntity p = u.player();
      final Vec3d feet = p.getEntityPos();
      final boolean air = !p.isOnGround() && !p.isTouchingWater() && !p.isClimbing();
      double r = u.empowered() ? 6.5 : 5.0;
      final double startY = p.getY();
      if (air) {
         Vec3d look = p.getRotationVector();
         p.setVelocity(look.x * 0.4, -2.6, look.z * 0.4);
         BlockHitResult below = ray(level, p, feet, new Vec3d(look.x * 0.15, -1.0, look.z * 0.15).normalize(), 48.0);
         if (below.getType() != Type.MISS) {
            Vfx.telegraphCircle(level, below.getPos(), r, Math.max(3, (int)((feet.y - below.getPos().y) / 2.6)), MONO);
         }

         Vfx.streaks(level, chest(p), new Vec3d(0.0, -1.0, 0.0), MONO, 10);
         Vfx.flare(level, chest(p), 16777215, 1.4, 6);
      } else {
         Vec3d aim = GearKit.lookPoint(level, p, 12.0);
         Vec3d off = new Vec3d(aim.x - feet.x, 0.0, aim.z - feet.z);
         Vec3d dir = off.lengthSquared() < 0.001 ? GearKit.flatLook(p) : off.normalize();
         double dist = MathHelper.clamp(off.length(), 3.0, 10.0);
         Vec3d land = feet.add(dir.multiply(dist));
         Vec3d g = floor(level, land.x, Math.max(aim.y, feet.y), land.z, 3, 10);
         double dy = g == null ? 0.0 : MathHelper.clamp(g.y - feet.y, -8.0, 2.5);
         double vy = 0.75;
         int flight = flightTicks(vy, dy);
         double vh = Math.min(1.8, dist * 0.09 / (1.0 - Math.pow(0.91, flight)));
         p.setVelocity(dir.x * vh, vy, dir.z * vh);
         Vfx.telegraphCircle(level, g != null ? g : land, r, flight, MONO);
         Vfx.ring(level, feet, 16777215, 1.6, 8);
         Vfx.smoke(level, feet.add(0.0, 0.3, 0.0), 657932, 4, 0.6, 1.3);
         Vfx.streaks(level, chest(p), new Vec3d(dir.x, 0.9, dir.z).normalize(), MONO, 6);
      }

      p.knockedBack = true;
      p.onLanding();
      every(level, new EnderDragonArsenal.Ticker() {
         private Vec3d prev = feet;
         private boolean left = air;

         @Override
         public boolean tick(int age) {
            if (!EnderDragonArsenal.alive(p, level)) {
               return false;
            }

            p.onLanding();
            Vec3d now = p.getEntityPos();
            if (!p.isOnGround()) {
               this.left = true;
            }

            if (age % 2 == 0 && now.squaredDistanceTo(this.prev) > 0.09) {
               Vfx.dashTrail(level, this.prev.add(0.0, 1.0, 0.0), now.add(0.0, 1.0, 0.0), EnderDragonArsenal.MONO);
               Vfx.smoke(level, now.add(0.0, 0.8, 0.0), 657932, 1, 0.2, 1.1);
               this.prev = now;
            }

            boolean landed = this.left && (p.isOnGround() || p.isTouchingWater());
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
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      Vec3d feet = p.getEntityPos();
      double r = u.empowered() ? 6.5 : 5.0;

      for (LivingEntity e : near(level, p, feet.add(0.0, 0.5, 0.0), r)) {
         double d = Math.min(r, gap(e, feet));
         cut(level, p, e, (float)(16.0 * (1.0 - 0.5 * d / r)) * bonus * u.power());
         fling(e, feet, 0.9, 0.7);
         e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 1), p);
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
         Vec3d look = GearKit.flatLook(p);

         for (int k = 0; k < 4; k++) {
            Vec3d d = look.rotateY((float)(k * Math.PI / 2.0));
            rift(level, p, feet.add(d.multiply(1.2)), d, 8.0, 6, 9.0F * u.power(), 3.0, "voidreaver", 0.0F);
         }
      }
   }

   static void riftscar(ServerWorld level, LivingEntity a, LivingEntity t, ItemStack s) {
      if (a instanceof PlayerEntity p) {
         long now = level.getTime();
         long[] scar = SCARS.computeIfAbsent(t, k -> new long[2]);
         if (now - scar[1] > 100L) {
            scar[0] = 0L;
         }

         scar[1] = now;
         scar[0]++;
         Vec3d c = center(t);
         if (scar[0] < 3L) {
            Vfx.glint(level, c, 16777215, 0.4 + 0.2 * scar[0]);
            Vfx.smoke(level, c, 657932, 1, 0.2, 0.9);
         } else {
            scar[0] = 0L;
            zap(level, p, t, 8.0F);
            t.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20, 1), p);
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
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      Vec3d feet = p.getEntityPos();
      Vec3d chest = chest(p);
      float yaw = p.getYaw();

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
         Vec3d d = new Vec3d(Math.sin(a), 0.0, Math.cos(a));
         Vfx.streaks(level, chest.add(d.multiply(1.2)), d, MONO, 2);
         Vfx.smoke(level, feet.add(d.multiply(1.8)).add(0.0, 0.25, 0.0), 657932, 1, 0.25, 1.2);
      }

      GearKit.kick(level, p, 0.2);
      if (u.empowered()) {
         float power = u.power();
         later(level, 8, () -> {
            if (alive(p, level)) {
               Vec3d f = p.getEntityPos();
               Vec3d c = chest(p);
               Vfx.slashArc(level, c, p.getYaw(), 0.0F, 7.5, MONO, -330.0F);
               Vfx.vortex(level, f, 3.0, 2.0, MONO, 10);
               Vfx.smoke(level, f.add(0.0, 0.4, 0.0), 657932, 6, 2.0, 1.4);

               for (LivingEntity e : near(level, p, c, 7.5)) {
                  cut(level, p, e, 9.0F * power);
                  if (movable(e)) {
                     Vec3d in = new Vec3d(f.x - e.getX(), 0.0, f.z - e.getZ());
                     in = in.lengthSquared() < 1.0E-4 ? Vec3d.ZERO : in.normalize().multiply(0.6);
                     e.setVelocity(in.x, 0.6, in.z);
                     e.knockedBack = true;
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
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      Vec3d feet = p.getEntityPos();
      Vec3d chest = chest(p);
      Vec3d dir = GearKit.flatLook(p);
      float yaw = p.getYaw();
      BlockHitResult wall = ray(level, p, chest, dir, 10.0);
      double length = wall.getType() == Type.MISS ? 10.0 : Math.max(1.5, wall.getPos().distanceTo(chest) - 0.4);
      List<LivingEntity> hit = strip(level, p, feet, dir, length + 0.5, 1.6, 3.0);
      GearKit.dash(p, 2.4, 0.2);

      for (LivingEntity e : hit) {
         cut(level, p, e, 12.0F * u.power());
         lift(e, 0.8);
         Vfx.claw(level, center(e), yaw, 0.0F, 2.0, MONO);
         GearKit.hitSpark(level, e, MONO);
      }

      Vfx.dashTrail(level, chest, chest.add(dir.multiply(length)), MONO);
      Vfx.streaks(level, chest, dir, MONO, 10);

      for (double k = 0.0; k <= length; k += 2.0) {
         Vfx.smoke(level, feet.add(dir.multiply(k)).add(0.0, 0.6, 0.0), 657932, 1, 0.25, 1.1);
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

   static void javelinLevel(ServerWorld level, PlayerEntity p, WeaponKit kit, int charge, boolean max) {
      Vec3d t = GearKit.tip(p);
      Vfx.gather(level, t, MONO, 1.2, 8);
      Vfx.glint(level, t, 16777215, 0.5 + 0.3 * charge);
      Vfx.smoke(level, t, 657932, 1, 0.1, 0.9);
      if (max) {
         Vfx.flare(level, t, 16777215, 1.6, 6);
         GearKit.kick(level, p, 0.12);
      }
   }

   static boolean javelin(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      int lvl = Math.max(1, u.charge());
      float damage = (10 + 5 * lvl) * u.power();
      float power = u.power();
      boolean pin = lvl >= 3;
      boolean back = u.empowered();
      Vec3d look = p.getRotationVector();
      EnderDragonArsenal.Tracer t = new EnderDragonArsenal.Tracer(level, p, GearKit.eye(p).add(look.multiply(0.8)).add(0.0, -0.15, 0.0), look.multiply(2.2));
      t.gravity = 0.015;
      t.life = 40;
      t.radius = 0.8;
      t.size = 0.6;
      t.pierce = lvl;
      t.draw = EnderDragonArsenal::spineDraw;
      t.onHit = (tr, e) -> {
         zap(level, p, e, damage);
         Vec3d c = center(e);
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

   private static void pinned(ServerWorld level, PlayerEntity p, @Nullable LivingEntity on, Vec3d at, float damage) {
      if (on != null && on.isAlive()) {
         on.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 12, 3), p);
      }

      every(level, age -> {
         Vec3d c = on != null && on.isAlive() && !on.isRemoved() && on.getEntityWorld() == level ? center(on) : at;
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

   private static void spineReturn(ServerWorld level, PlayerEntity p, Vec3d from, float damage) {
      Vec3d to = chest(p).subtract(from);
      EnderDragonArsenal.Tracer t = new EnderDragonArsenal.Tracer(
         level, p, from, (to.lengthSquared() < 1.0E-4 ? new Vec3d(0.0, 1.0, 0.0) : to.normalize()).multiply(0.8)
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

   static void skyrend(ServerWorld level, LivingEntity a, LivingEntity t, ItemStack s) {
      if (a instanceof PlayerEntity p) {
         if (!t.isOnGround() && !t.isTouchingWater()) {
            zap(level, p, t, 6.0F);
            if (movable(t)) {
               Vec3d v = t.getVelocity();
               t.setVelocity(v.x * 0.5, -1.4, v.z * 0.5);
               t.knockedBack = true;
            }

            Vec3d c = center(t);
            Vfx.ring(level, t.getEntityPos(), 16777215, 1.4, 8);
            Vfx.streaks(level, c, new Vec3d(0.0, -1.0, 0.0), MONO, 6);
            Vfx.glint(level, c, 16777215, 0.9);
            Vfx.smoke(level, c, 657932, 1, 0.3, 1.0);
            GearKit.procSound(level, t, "wyrmspine_glaive");
            GearState.of(p).addResonance("wyrmspine_glaive", 5.0F);
         } else {
            lift(t, 0.35);
            Vfx.ring(level, t.getEntityPos(), 16777215, 0.9, 6);
         }
      }
   }

   static boolean endOrbs(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      LivingEntity target = orbTarget(level, p);
      Vec3d look = p.getRotationVector();
      Vec3d tip = GearKit.tip(p);
      Vec3d side = look.crossProduct(new Vec3d(0.0, 1.0, 0.0));
      side = side.lengthSquared() < 1.0E-4 ? new Vec3d(1.0, 0.0, 0.0) : side.normalize();
      Vec3d up = side.crossProduct(look).normalize();
      boolean emp = u.empowered();
      int n = emp ? 5 : 3;
      float spread = emp ? 15.0F : 25.0F;
      float damage = 7.0F * u.power();

      for (int i = 0; i < n; i++) {
         float fan = (i - (n - 1) / 2.0F) * spread;
         double a = Math.toRadians(fan * 2.0F);
         Vec3d at = tip.add(side.multiply(Math.sin(a) * 0.8)).add(up.multiply(Math.cos(a) * 0.8));
         EnderDragonArsenal.Tracer t = new EnderDragonArsenal.Tracer(level, p, at, look.rotateY((float)Math.toRadians(-fan)).multiply(0.6));
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

   private static @Nullable LivingEntity orbTarget(ServerWorld level, PlayerEntity p) {
      LivingEntity best = GearKit.lookTarget(level, p, 32.0);
      if (best != null) {
         return best;
      }

      Vec3d eye = GearKit.eye(p);
      Vec3d look = p.getRotationVector();
      double bestD = Double.MAX_VALUE;

      for (LivingEntity e : GearKit.enemies(level, p, eye, 24.0)) {
         Vec3d to = center(e).subtract(eye);
         double along = to.dotProduct(look);
         if (!(along <= 0.0) && isMarked(level, e)) {
            double off = to.subtract(look.multiply(along)).length() / along;
            if (off < bestD && p.canSee(e)) {
               best = e;
               bestD = off;
            }
         }
      }

      if (best != null) {
         return best;
      }

      Vec3d aim = GearKit.lookPoint(level, p, 32.0);
      bestD = 256.0;

      for (LivingEntity e : GearKit.enemies(level, p, aim, 16.0)) {
         double d = e.squaredDistanceTo(aim);
         if (d < bestD && p.canSee(e)) {
            best = e;
            bestD = d;
         }
      }

      return best;
   }

   private static void orbImpact(ServerWorld level, PlayerEntity p, Vec3d at, @Nullable LivingEntity e, float damage, boolean empowered) {
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

   private static void singularity(ServerWorld level, PlayerEntity p, Vec3d at) {
      Vfx.vortex(level, at, 1.5, 1.5, MONO, 20);
      every(level, age -> {
         if (age <= 20 && alive(p, level)) {
            for (LivingEntity e : near(level, p, at, 2.5)) {
               Vec3d d = at.subtract(center(e));
               if (d.lengthSquared() > 0.04) {
                  nudge(e, d.normalize().multiply(0.08));
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
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      Vec3d aim = GearKit.lookPoint(level, p, 24.0);
      Vec3d g = floor(level, aim.x, aim.y, aim.z, 2, 8);
      Vec3d w = g != null ? g : aim;
      boolean emp = u.empowered();
      double r = emp ? 7.0 : 5.0;
      double pullR = emp ? 8.0 : 6.0;
      int collapse = emp ? 80 : 60;
      float power = u.power();
      Vfx.telegraphCircle(level, w, r, 10, MONO);
      Vfx.runeCircle(level, w, r, MONO, collapse + 10, 0.06F, Vfx.Rune.ASTRAL);
      Vfx.smoke(level, w.add(0.0, 0.3, 0.0), 657932, 6, r * 0.5, 1.4);
      Vfx.beam(level, GearKit.tip(p), w.add(0.0, 0.5, 0.0), MONO, 0.12, 6);
      Vec3d core = w.add(0.0, 0.6, 0.0);
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
                  Vec3d v = e.getVelocity();
                  e.setVelocity(v.x * 0.3, -0.8, v.z * 0.3);
                  e.knockedBack = true;
               }

               e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 1), p);
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
                  Vec3d d = new Vec3d(core.x - e.getX(), 0.0, core.z - e.getZ());
                  double dist = d.length();
                  if (dist > 0.3) {
                     double s = 0.1 * Math.min(1.0, dist / 2.0);
                     nudge(e, new Vec3d(d.x / dist * s, -0.03, d.z / dist * s));
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

   static void lanceLevel(ServerWorld level, PlayerEntity p, WeaponKit kit, int charge, boolean max) {
      boolean pierce = GearState.of(p).resonance(kit.id()) >= 40.0F;
      Vec3d eye = GearKit.eye(p);
      Vec3d look = p.getRotationVector();
      double length = 16 + 6 * charge;
      Vec3d end = pierce ? eye.add(look.multiply(length)) : ray(level, p, eye, look, length).getPos();
      Vec3d t = GearKit.tip(p);
      Vfx.beam(level, t, end, MONO, 0.08, 12);
      if (charge == 1) {
         Vfx.runeCircle(level, groundAt(level, p.getEntityPos()), 1.5, MONO, 50, 0.05F, Vfx.Rune.ASTRAL);
      }

      if (max) {
         Vfx.chargeUp(level, t, MONO, 6);
         Vfx.glint(level, t, 16777215, 1.2);
      } else {
         Vfx.glint(level, t, 16777215, 0.4 + 0.2 * charge);
      }
   }

   static boolean voidLance(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      int lvl = Math.max(1, u.charge());
      float power = u.power();
      boolean emp = u.empowered();
      boolean full = lvl >= 4;
      Vec3d eye = GearKit.eye(p);
      Vec3d look = p.getRotationVector();
      double length = 16 + 6 * lvl;
      BlockHitResult hit = ray(level, p, eye, look, length);
      boolean blocked = !emp && hit.getType() != Type.MISS;
      Vec3d end = blocked ? hit.getPos() : eye.add(look.multiply(length));
      double dist = end.distanceTo(eye);
      Vfx.beam(level, eye.add(look.multiply(0.8)), end, MONO, 0.9 + 0.15 * lvl, full ? 20 : 6);
      double step = Math.max(1.5, dist / 16.0);

      for (double k = 1.5; k < dist; k += step) {
         Vfx.smoke(level, eye.add(look.multiply(k)), 657932, 1, 0.15, 1.2);
      }

      if (blocked && hit.getSide() == Direction.UP) {
         Vfx.groundCrack(level, end, 1.5, MONO);
      }

      Vfx.impact(level, end, MONO, 0.8);

      for (LivingEntity e : GearKit.alongLine(level, p, eye, look, dist, 1.2)) {
         zap(level, p, e, (8 + 4 * lvl) * power * mark(level, e));
         nudge(e, look.multiply(0.4));
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
                     Vfx.shards(level, eye.add(look.multiply(dist * k / 4.0)), 16777215, 3, 0.25);
                  }
               }
            });
         }
      }

      if (emp) {
         List<Vec3d> chain = new ArrayList<>();
         chain.add(end);
         Set<LivingEntity> struck = new LinkedHashSet<>();
         Vec3d from = end;

         for (int i = 0; i < 3; i++) {
            LivingEntity next = null;
            double bestD = 64.0;

            for (LivingEntity e : GearKit.enemies(level, p, end, 8.0)) {
               double d = center(e).squaredDistanceTo(from);
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

   static void endsight(ServerWorld level, LivingEntity a, LivingEntity t, ItemStack s) {
      if (a instanceof PlayerEntity p) {
         boolean fresh = !isMarked(level, t);
         MARKS.put(t, level.getTime() + 160L);
         t.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 160, 0, false, false), p);
         if (fresh) {
            GearState.of(p).addResonance("endseer_staff", 10.0F);
         }

         Vfx.flare(level, t.getEntityPos().add(0.0, t.getHeight() + 0.6, 0.0), 16777215, 0.9, 8);
         Vfx.glint(level, t.getEyePos(), 16777215, 0.8);
         Vfx.ring(level, center(t), 16777215, 1.0, 8);
         GearKit.procSound(level, t, "endseer_staff");
      }
   }

   static void aegisTick(ServerWorld level, PlayerEntity p, int pieces) {
      long now = level.getTime();
      EnderDragonArsenal.Aegis a = AEGIS.get(p.getUuid());
      if (a == null || a.seen < now - 2L) {
         a = new EnderDragonArsenal.Aegis();
         a.nextRegen = now + 240L;
         AEGIS.put(p.getUuid(), a);
      }

      a.seen = now;
      Vec3d chest = chest(p);
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

   static boolean guard(ServerWorld level, PlayerEntity p, DamageSource source, float amount, int pieces) {
      if (source.isIn(DamageTypeTags.IS_FALL)) {
         dragonfall(level, p, amount);
         return false;
      } else {
         return pieces < 4 || !aegisBlock(level, p, source, amount);
      }
   }

   private static void dragonfall(ServerWorld level, PlayerEntity p, float amount) {
      GearState state = GearState.of(p);
      if (!(amount < 3.0F) && state.ready("ender_dragon.fall")) {
         state.cooldown("ender_dragon.fall", 20);
         Vec3d feet = p.getEntityPos();
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

   private static boolean aegisBlock(ServerWorld level, PlayerEntity p, DamageSource source, float amount) {
      long now = level.getTime();
      EnderDragonArsenal.Aegis a = AEGIS.get(p.getUuid());
      if (a == null || a.seen < now - 2L || amount < 3.0F || source.getAttacker() == null || source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         return false;
      }

      if (now < a.guardUntil) {
         return true;
      }

      if (a.shards > 0 && p.timeUntilRegen <= 10) {
         a.shards--;
         a.guardUntil = now + 10L;
         Vec3d c = chest(p);
         Set<LivingEntity> hit = new LinkedHashSet<>(near(level, p, c, 3.0));
         if (source.getAttacker() instanceof LivingEntity attacker
            && !(attacker instanceof PlayerEntity)
            && attacker.isAlive()
            && attacker.getEntityWorld() == level
            && attacker.squaredDistanceTo(p) < 2304.0) {
            hit.add(attacker);
         }

         for (LivingEntity e : hit) {
            zap(level, p, e, 6.0F);
         }

         Vfx.shards(level, c, 16777215, 14, 0.35);
         Vfx.nova(level, c, MONO, 2.5);
         Vfx.smoke(level, c, 657932, 4, 0.6, 1.3);
         Entity from = source.getSource() != null ? source.getSource() : source.getAttacker();
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
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      Vec3d v = p.getVelocity();
      p.setVelocity(v.x * 0.3, 0.9, v.z * 0.3);
      p.knockedBack = true;
      p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 30, 0, false, false, true));
      p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 30, 2, false, false, true));
      Vec3d chest = chest(p);
      double rise = 4.4;
      BlockHitResult roof = ray(level, p, p.getEyePos(), new Vec3d(0.0, 1.0, 0.0), rise + 0.3);
      if (roof.getType() != Type.MISS) {
         rise = Math.max(0.0, roof.getPos().y - p.getEyeY() - 0.3);
      }

      Vec3d apex = chest.add(0.0, rise, 0.0);
      Vfx.chargeUp(level, apex, MONO, 20);
      Vfx.shieldBubble(level, chest, 1.6, MONO, 12);
      Vfx.runeCircle(level, groundAt(level, p.getEntityPos()), 6.0, MONO, 30, 0.05F, Vfx.Rune.ASTRAL);
      Vfx.smoke(level, p.getEntityPos().add(0.0, 0.3, 0.0), 657932, 6, 1.2, 1.5);
      every(level, age -> {
         if (age <= 20 && alive(p, level)) {
            Vec3d c = chest(p);
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
               e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 1), p);
               e.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 100, 0, false, false), p);
            }

            p.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 200, 1));
            EnderDragonArsenal.Aegis a = AEGIS.get(p.getUuid());
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
      private final ServerWorld level;
      private final EnderDragonArsenal.Ticker ticker;
      private int age;

      private Live(ServerWorld level, EnderDragonArsenal.Ticker ticker) {
         this.level = level;
         this.ticker = ticker;
      }
   }

   private record Task(ServerWorld level, long due, Runnable run) {
   }

   @FunctionalInterface
   private interface Ticker {
      boolean tick(int var1);
   }

   private static final class Tracer implements EnderDragonArsenal.Ticker {
      private final ServerWorld level;
      private final PlayerEntity owner;
      private Vec3d pos;
      private Vec3d vel;
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
      private @Nullable Supplier<Vec3d> home;
      private @Nullable LivingEntity stopped;
      private final Set<Integer> hit = new HashSet<>();
      private EnderDragonArsenal.TracerHit onHit = (t, e) -> {};
      private EnderDragonArsenal.TracerEnd onEnd = (t, at, why) -> {};
      private EnderDragonArsenal.TracerDraw draw = EnderDragonArsenal::plainDraw;

      private Tracer(ServerWorld level, PlayerEntity owner, Vec3d pos, Vec3d vel) {
         this.level = level;
         this.owner = owner;
         this.pos = pos;
         this.vel = vel;
         this.maxSpeed = vel.length();
      }

      private @Nullable Vec3d goal() {
         if (this.home != null) {
            return this.home.get();
         }

         LivingEntity t = this.target;
         return t != null && t.isAlive() && !t.isRemoved() && t.getEntityWorld() == this.level ? t.getBoundingBox().getCenter() : null;
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

         Vec3d goal = this.goal();
         if (goal != null && this.homing > 0.0 && speed > 1.0E-4) {
            Vec3d want = goal.subtract(this.pos);
            if (want.lengthSquared() > 1.0E-4) {
               Vec3d steer = this.vel.multiply(1.0 / Math.max(1.0E-4, this.vel.length())).lerp(want.normalize(), this.homing);
               this.vel = (steer.lengthSquared() < 1.0E-6 ? want.normalize() : steer.normalize()).multiply(speed);
            }
         } else if (this.ramp > 0.0 && this.vel.lengthSquared() > 1.0E-6) {
            this.vel = this.vel.normalize().multiply(speed);
         }

         this.vel = this.vel.add(0.0, -this.gravity, 0.0);
         Vec3d from = this.pos;
         Vec3d to = from.add(this.vel);
         if (!this.level.isPosLoaded(BlockPos.ofFloored(to))) {
            this.onEnd.end(this, from, 0);
            return false;
         }

         Vec3d wall = null;
         if (!this.ghost) {
            BlockHitResult bh = this.level.raycast(new RaycastContext(from, to, ShapeType.COLLIDER, FluidHandling.NONE, this.owner));
            if (bh.getType() != Type.MISS) {
               wall = bh.getPos();
               to = wall;
            }
         }

         List<LivingEntity> found = new ArrayList<>();
         Vec3d a = from;
         Vec3d b = to;

         for (LivingEntity e : GearKit.enemies(this.level, this.owner, a.add(b).multiply(0.5), a.distanceTo(b) / 2.0 + this.radius + 4.0)) {
            Box box = e.getBoundingBox().expand(this.radius);
            if (!this.hit.contains(e.getId()) && (box.contains(a) || box.raycast(a, b).isPresent())) {
               found.add(e);
            }
         }

         found.sort(Comparator.comparingDouble(ex -> ex.getBoundingBox().getCenter().squaredDistanceTo(a)));

         for (LivingEntity e : found) {
            this.hit.add(e.getId());
            Box box = e.getBoundingBox().expand(this.radius);
            this.pos = box.contains(a) ? a : box.raycast(a, b).orElse(a);
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
      void end(EnderDragonArsenal.Tracer var1, Vec3d var2, int var3);
   }

   @FunctionalInterface
   private interface TracerHit {
      void hit(EnderDragonArsenal.Tracer var1, LivingEntity var2);
   }
}
