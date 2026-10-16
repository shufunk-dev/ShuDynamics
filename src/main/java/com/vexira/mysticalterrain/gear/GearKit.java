package com.vexira.mysticalterrain.gear;

import com.vexira.mysticalterrain.boss.BossBolt;
import com.vexira.mysticalterrain.boss.BossFx;
import com.vexira.mysticalterrain.boss.BossSounds;
import com.vexira.mysticalterrain.boss.vfx.Vfx;
import java.util.List;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.jspecify.annotations.Nullable;

public final class GearKit {
   private GearKit() {
   }

   public static List<LivingEntity> enemies(ServerWorld level, PlayerEntity p, Vec3d at, double r) {
      return BossFx.enemies(level, p, at, r);
   }

   public static Vec3d eye(PlayerEntity p) {
      return p.getEyePos();
   }

   public static Vec3d flatLook(PlayerEntity p) {
      Vec3d l = p.getRotationVector();
      Vec3d f = new Vec3d(l.x, 0.0, l.z);
      return f.lengthSquared() < 1.0E-4 ? new Vec3d(0.0, 0.0, 1.0) : f.normalize();
   }

   public static @Nullable LivingEntity lookTarget(ServerWorld level, PlayerEntity p, double range) {
      Vec3d eye = eye(p);
      Vec3d look = p.getRotationVector();
      LivingEntity best = null;
      double bestD = range * range;

      for (LivingEntity e : enemies(level, p, eye.add(look.multiply(range / 2.0)), range / 2.0 + 2.0)) {
         Vec3d to = e.getBoundingBox().getCenter().subtract(eye);
         double d = to.lengthSquared();
         if (d < bestD && to.normalize().dotProduct(look) > 0.96 && p.canSee(e)) {
            best = e;
            bestD = d;
         }
      }

      return best;
   }

   public static Vec3d lookPoint(ServerWorld level, PlayerEntity p, double range) {
      Vec3d from = eye(p);
      Vec3d to = from.add(p.getRotationVector().multiply(range));
      BlockHitResult hit = level.raycast(new RaycastContext(from, to, ShapeType.COLLIDER, FluidHandling.NONE, p));
      return hit.getPos();
   }

   public static List<LivingEntity> alongLine(ServerWorld level, PlayerEntity p, Vec3d from, Vec3d dir, double length, double width) {
      return enemies(level, p, from.add(dir.multiply(length / 2.0)), length / 2.0 + width).stream().filter(e -> {
         Vec3d c = e.getBoundingBox().getCenter();
         double along = c.subtract(from).dotProduct(dir);
         return along > 0.0 && along < length && c.distanceTo(from.add(dir.multiply(along))) < width + e.getWidth() / 2.0F;
      }).toList();
   }

   public static void strike(ServerWorld level, PlayerEntity p, LivingEntity e, float damage) {
      e.damage(level, p.getDamageSources().playerAttack(p), damage);
   }

   public static void magic(ServerWorld level, PlayerEntity p, LivingEntity e, float damage) {
      e.damage(level, p.getDamageSources().indirectMagic(p, p), damage);
   }

   public static BossBolt bolt(ServerWorld level, PlayerEntity p, BossBolt.Style style, double speed, float damage) {
      Vec3d from = eye(p).add(p.getRotationVector().multiply(0.8)).add(0.0, -0.2, 0.0);
      return BossBolt.shoot(level, p, style, from, p.getRotationVector().multiply(speed), damage);
   }

   public static void dash(PlayerEntity p, double speed, double lift) {
      Vec3d f = flatLook(p).multiply(speed);
      p.setVelocity(f.x, lift, f.z);
      p.knockedBack = true;
   }

   public static void knockAway(LivingEntity e, Vec3d from, double strength, double lift) {
      Vec3d away = e.getEntityPos().subtract(from).normalize();
      e.addVelocity(away.x * strength, lift, away.z * strength);
      e.knockedBack = true;
   }

   public static boolean teleportBehind(ServerWorld level, PlayerEntity p, LivingEntity target) {
      Vec3d back = Vec3d.fromPolar(0.0F, target.getYaw()).multiply(-1.6);
      Vec3d at = target.getEntityPos().add(back);
      if (!level.isSpaceEmpty(p, p.getBoundingBox().offset(at.subtract(p.getEntityPos())))) {
         return false;
      }

      level.spawnParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY() + 1.0, p.getZ(), 20, 0.3, 0.6, 0.3, 0.02);
      Vfx.smoke(level, p.getEntityPos().add(0.0, 0.6, 0.0), 1840168, 6, 0.5, 1.3);
      Vec3d from = p.getEntityPos().add(0.0, 1.0, 0.0);
      p.requestTeleport(at.x, at.y, at.z);
      Vfx.beam(level, from, at.add(0.0, 1.0, 0.0), Vfx.Palette.SHADOW, 0.35);
      float yaw = (float)(MathHelper.atan2(target.getZ() - at.z, target.getX() - at.x) * 180.0F / (float)Math.PI) - 90.0F;
      p.setYaw(yaw);
      p.setHeadYaw(yaw);
      return true;
   }

   public static void cloud(ServerWorld level, PlayerEntity p, Vec3d at, float radius, int duration, ParticleEffect particle, StatusEffectInstance effect) {
      AreaEffectCloudEntity cloud = new AreaEffectCloudEntity(level, at.x, at.y, at.z);
      cloud.setOwner(p);
      cloud.setRadius(radius);
      cloud.setDuration(duration);
      cloud.setWaitTime(5);
      cloud.setRadiusGrowth(-radius / duration * 0.3F);
      cloud.setParticleType(particle);
      cloud.addEffect(effect);
      level.spawnEntity(cloud);
   }

   public static void keep(PlayerEntity p, RegistryEntry<StatusEffect> effect, int amplifier) {
      StatusEffectInstance cur = p.getStatusEffect(effect);
      if (cur == null || cur.getAmplifier() < amplifier || !cur.isInfinite() && cur.getDuration() < 40) {
         p.addStatusEffect(new StatusEffectInstance(effect, 70, amplifier, true, false, true));
      }
   }

   public static void kick(ServerWorld level, PlayerEntity p, double strength) {
      Vfx.shake(level, p.getEntityPos(), 12.0, strength, 8);
   }

   public static void muzzle(ServerWorld level, PlayerEntity p, Vfx.Palette pal, double size) {
      Vec3d at = tip(p);
      Vfx.flare(level, at, pal.glow(), size * 1.8, 7);
      Vfx.flare(level, at, pal.core(), size, 4);
      Vfx.sparks(level, at, pal.core(), 5, 0.2, 0.16);
   }

   public static Vec3d tip(PlayerEntity p) {
      return eye(p).add(p.getRotationVector().multiply(1.1)).add(0.0, -0.25, 0.0);
   }

   public static void beamLine(ServerWorld level, Vec3d from, Vec3d to, Vfx.Palette pal, double width, ParticleEffect particle, double spacing) {
      Vfx.beam(level, from, to, pal, width);
      Vec3d d = to.subtract(from);
      double len = d.length();
      double t = 0.0;

      while (t < len) {
         Vec3d q = from.add(d.multiply(t / len));
         level.spawnParticles(particle, q.x, q.y, q.z, 1, 0.05, 0.05, 0.05, 0.0);
         t += spacing;
      }
   }

   public static void dashTrail(ServerWorld level, Vec3d start, Vec3d dir, double length, Vfx.Palette pal) {
      for (int k = 0; k <= (int)(length * 1.5); k++) {
         Vfx.trail(level, start.add(0.0, 1.0, 0.0).add(dir.multiply(k / 1.5)), pal, 0.6);
      }
   }

   public static void hitSpark(ServerWorld level, LivingEntity t, Vfx.Palette pal) {
      Vec3d c = t.getBoundingBox().getCenter();
      Vfx.flare(level, c, pal.glow(), 1.1, 5);
      Vfx.sparks(level, c, pal.core(), 4, 0.22, 0.16);
   }

   public static void proc(ServerWorld level, LivingEntity attacker, Vfx.Palette pal) {
      Vec3d c = attacker.getBoundingBox().getCenter();
      Vfx.flare(level, c, pal.glow(), 1.3, 6);
      Vfx.sparks(level, c, pal.core(), 6, 0.25, 0.18);
   }

   public static void empowerFlourish(ServerWorld level, PlayerEntity p, Vfx.Palette pal) {
      Vfx.ring(level, p.getEntityPos(), pal.core(), 2.4, 12);
      Vfx.flare(level, p.getEntityPos().add(0.0, 1.0, 0.0), pal.core(), 2.4, 8);
      Vfx.sparks(level, p.getEntityPos().add(0.0, 1.0, 0.0), pal.glow(), 12, 0.35, 0.2);
   }

   public static void chargeLevel(ServerWorld level, PlayerEntity p, Vfx.Palette pal, int charge, boolean max) {
      Vec3d at = tip(p);
      Vfx.ring(level, p.getEntityPos(), pal.glow(), 0.8 + 0.5 * charge, 8);
      Vfx.flare(level, at, max ? pal.core() : pal.glow(), 0.9 + 0.35 * charge, 6);
      Vfx.sparks(level, at, pal.core(), 3 + charge * 2, 0.18, 0.14);
      if (max) {
         Vfx.gather(level, at, pal, 1.2, 8);
      }
   }

   public static void sound(ServerWorld level, PlayerEntity p, float volume, float pitch, String... keys) {
      for (String key : keys) {
         SoundEvent e = BossSounds.event(key);
         if (e != null) {
            level.playSound(null, p.getX(), p.getY(), p.getZ(), e, SoundCategory.PLAYERS, volume, pitch);
            return;
         }
      }
   }

   public static void procSound(ServerWorld level, LivingEntity at, String weapon) {
      SoundEvent e = BossSounds.event("weapon." + weapon + ".proc");
      if (e != null) {
         level.playSound(null, at.getX(), at.getY(), at.getZ(), e, SoundCategory.PLAYERS, 0.9F, 0.95F + level.getRandom().nextFloat() * 0.1F);
      }
   }

   public static boolean surge(GearUse u) {
      ServerWorld level = u.level();
      PlayerEntity p = u.player();
      Vfx.Palette pal = u.palette();

      for (LivingEntity e : enemies(level, p, p.getEntityPos(), 5.0)) {
         u.magic(e, 6.0F);
         knockAway(e, p.getEntityPos(), 1.3, 0.45);
         Vfx.impact(level, e.getBoundingBox().getCenter(), pal, 0.5);
      }

      Vfx.shockwave(level, p.getEntityPos(), pal, 5.0);
      Vfx.flare(level, p.getEntityPos().add(0.0, 1.0, 0.0), pal.glow(), 2.6, 8);
      Vfx.aura(level, p.getEntityPos(), pal, 1.6);
      kick(level, p, 0.3);
      return true;
   }
}
