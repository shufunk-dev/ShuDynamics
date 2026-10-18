package com.vexira.mysticalterrain.gear;

import com.vexira.mysticalterrain.boss.BossBolt;
import com.vexira.mysticalterrain.boss.BossFx;
import com.vexira.mysticalterrain.boss.BossSounds;
import com.vexira.mysticalterrain.boss.vfx.Vfx;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class GearKit {
   private GearKit() {
   }

   public static List<LivingEntity> enemies(ServerLevel level, Player p, Vec3 at, double r) {
      return BossFx.enemies(level, p, at, r);
   }

   public static Vec3 eye(Player p) {
      return p.getEyePosition();
   }

   public static Vec3 flatLook(Player p) {
      Vec3 l = p.getLookAngle();
      Vec3 f = new Vec3(l.x, 0.0, l.z);
      return f.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : f.normalize();
   }

   public static @Nullable LivingEntity lookTarget(ServerLevel level, Player p, double range) {
      Vec3 eye = eye(p);
      Vec3 look = p.getLookAngle();
      LivingEntity best = null;
      double bestD = range * range;

      for (LivingEntity e : enemies(level, p, eye.add(look.scale(range / 2.0)), range / 2.0 + 2.0)) {
         Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
         double d = to.lengthSqr();
         if (d < bestD && to.normalize().dot(look) > 0.96 && p.hasLineOfSight(e)) {
            best = e;
            bestD = d;
         }
      }

      return best;
   }

   public static Vec3 lookPoint(ServerLevel level, Player p, double range) {
      Vec3 from = eye(p);
      Vec3 to = from.add(p.getLookAngle().scale(range));
      BlockHitResult hit = level.clip(new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, p));
      return hit.getLocation();
   }

   public static List<LivingEntity> alongLine(ServerLevel level, Player p, Vec3 from, Vec3 dir, double length, double width) {
      return enemies(level, p, from.add(dir.scale(length / 2.0)), length / 2.0 + width).stream().filter(e -> {
         Vec3 c = e.getBoundingBox().getCenter();
         double along = c.subtract(from).dot(dir);
         return along > 0.0 && along < length && c.distanceTo(from.add(dir.scale(along))) < width + e.getBbWidth() / 2.0F;
      }).toList();
   }

   public static void strike(ServerLevel level, Player p, LivingEntity e, float damage) {
      e.hurtServer(level, p.damageSources().playerAttack(p), damage);
   }

   public static void magic(ServerLevel level, Player p, LivingEntity e, float damage) {
      e.hurtServer(level, p.damageSources().indirectMagic(p, p), damage);
   }

   public static BossBolt bolt(ServerLevel level, Player p, BossBolt.Style style, double speed, float damage) {
      Vec3 from = eye(p).add(p.getLookAngle().scale(0.8)).add(0.0, -0.2, 0.0);
      return BossBolt.shoot(level, p, style, from, p.getLookAngle().scale(speed), damage);
   }

   public static void dash(Player p, double speed, double lift) {
      Vec3 f = flatLook(p).scale(speed);
      p.setDeltaMovement(f.x, lift, f.z);
      p.syncVelocity = true;
   }

   public static void knockAway(LivingEntity e, Vec3 from, double strength, double lift) {
      Vec3 away = e.position().subtract(from).normalize();
      e.push(away.x * strength, lift, away.z * strength);
      e.syncVelocity = true;
   }

   public static boolean teleportBehind(ServerLevel level, Player p, LivingEntity target) {
      Vec3 back = Vec3.directionFromRotation(0.0F, target.getYRot()).scale(-1.6);
      Vec3 at = target.position().add(back);
      if (!level.noCollision(p, p.getBoundingBox().move(at.subtract(p.position())))) {
         return false;
      }

      level.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY() + 1.0, p.getZ(), 20, 0.3, 0.6, 0.3, 0.02);
      Vfx.smoke(level, p.position().add(0.0, 0.6, 0.0), 1840168, 6, 0.5, 1.3);
      Vec3 from = p.position().add(0.0, 1.0, 0.0);
      p.teleportTo(at.x, at.y, at.z);
      Vfx.beam(level, from, at.add(0.0, 1.0, 0.0), Vfx.Palette.SHADOW, 0.35);
      float yaw = (float)(Mth.atan2(target.getZ() - at.z, target.getX() - at.x) * 180.0F / (float)Math.PI) - 90.0F;
      p.setYRot(yaw);
      p.setYHeadRot(yaw);
      return true;
   }

   public static void cloud(ServerLevel level, Player p, Vec3 at, float radius, int duration, ParticleOptions particle, MobEffectInstance effect) {
      AreaEffectCloud cloud = new AreaEffectCloud(level, at.x, at.y, at.z);
      cloud.setOwner(p);
      cloud.setRadius(radius);
      cloud.setDuration(duration);
      cloud.setWaitTime(5);
      cloud.setRadiusPerTick(-radius / duration * 0.3F);
      cloud.setCustomParticle(particle);
      cloud.addEffect(effect);
      level.addFreshEntity(cloud);
   }

   public static void keep(Player p, Holder<MobEffect> effect, int amplifier) {
      MobEffectInstance cur = p.getEffect(effect);
      if (cur == null || cur.getAmplifier() < amplifier || !cur.isInfiniteDuration() && cur.getDuration() < 40) {
         p.addEffect(new MobEffectInstance(effect, 70, amplifier, true, false, true));
      }
   }

   public static void kick(ServerLevel level, Player p, double strength) {
      Vfx.shake(level, p.position(), 12.0, strength, 8);
   }

   public static void muzzle(ServerLevel level, Player p, Vfx.Palette pal, double size) {
      Vec3 at = tip(p);
      Vfx.flare(level, at, pal.glow(), size * 1.8, 7);
      Vfx.flare(level, at, pal.core(), size, 4);
      Vfx.sparks(level, at, pal.core(), 5, 0.2, 0.16);
   }

   public static Vec3 tip(Player p) {
      return eye(p).add(p.getLookAngle().scale(1.1)).add(0.0, -0.25, 0.0);
   }

   public static void beamLine(ServerLevel level, Vec3 from, Vec3 to, Vfx.Palette pal, double width, ParticleOptions particle, double spacing) {
      Vfx.beam(level, from, to, pal, width);
      Vec3 d = to.subtract(from);
      double len = d.length();
      double t = 0.0;

      while (t < len) {
         Vec3 q = from.add(d.scale(t / len));
         level.sendParticles(particle, q.x, q.y, q.z, 1, 0.05, 0.05, 0.05, 0.0);
         t += spacing;
      }
   }

   public static void dashTrail(ServerLevel level, Vec3 start, Vec3 dir, double length, Vfx.Palette pal) {
      for (int k = 0; k <= (int)(length * 1.5); k++) {
         Vfx.trail(level, start.add(0.0, 1.0, 0.0).add(dir.scale(k / 1.5)), pal, 0.6);
      }
   }

   public static void hitSpark(ServerLevel level, LivingEntity t, Vfx.Palette pal) {
      Vec3 c = t.getBoundingBox().getCenter();
      Vfx.flare(level, c, pal.glow(), 1.1, 5);
      Vfx.sparks(level, c, pal.core(), 4, 0.22, 0.16);
   }

   public static void proc(ServerLevel level, LivingEntity attacker, Vfx.Palette pal) {
      Vec3 c = attacker.getBoundingBox().getCenter();
      Vfx.flare(level, c, pal.glow(), 1.3, 6);
      Vfx.sparks(level, c, pal.core(), 6, 0.25, 0.18);
   }

   public static void empowerFlourish(ServerLevel level, Player p, Vfx.Palette pal) {
      Vfx.ring(level, p.position(), pal.core(), 2.4, 12);
      Vfx.flare(level, p.position().add(0.0, 1.0, 0.0), pal.core(), 2.4, 8);
      Vfx.sparks(level, p.position().add(0.0, 1.0, 0.0), pal.glow(), 12, 0.35, 0.2);
   }

   public static void chargeLevel(ServerLevel level, Player p, Vfx.Palette pal, int charge, boolean max) {
      Vec3 at = tip(p);
      Vfx.ring(level, p.position(), pal.glow(), 0.8 + 0.5 * charge, 8);
      Vfx.flare(level, at, max ? pal.core() : pal.glow(), 0.9 + 0.35 * charge, 6);
      Vfx.sparks(level, at, pal.core(), 3 + charge * 2, 0.18, 0.14);
      if (max) {
         Vfx.gather(level, at, pal, 1.2, 8);
      }
   }

   public static void sound(ServerLevel level, Player p, float volume, float pitch, String... keys) {
      for (String key : keys) {
         SoundEvent e = BossSounds.event(key);
         if (e != null) {
            level.playSound(null, p.getX(), p.getY(), p.getZ(), e, SoundSource.PLAYERS, volume, pitch);
            return;
         }
      }
   }

   public static void procSound(ServerLevel level, LivingEntity at, String weapon) {
      SoundEvent e = BossSounds.event("weapon." + weapon + ".proc");
      if (e != null) {
         level.playSound(null, at.getX(), at.getY(), at.getZ(), e, SoundSource.PLAYERS, 0.9F, 0.95F + level.getRandom().nextFloat() * 0.1F);
      }
   }

   public static boolean surge(GearUse u) {
      ServerLevel level = u.level();
      Player p = u.player();
      Vfx.Palette pal = u.palette();

      for (LivingEntity e : enemies(level, p, p.position(), 5.0)) {
         u.magic(e, 6.0F);
         knockAway(e, p.position(), 1.3, 0.45);
         Vfx.impact(level, e.getBoundingBox().getCenter(), pal, 0.5);
      }

      Vfx.shockwave(level, p.position(), pal, 5.0);
      Vfx.flare(level, p.position().add(0.0, 1.0, 0.0), pal.glow(), 2.6, 8);
      Vfx.aura(level, p.position(), pal, 1.6);
      kick(level, p, 0.3);
      return true;
   }
}
