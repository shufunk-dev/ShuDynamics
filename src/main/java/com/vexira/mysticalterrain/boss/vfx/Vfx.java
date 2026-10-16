package com.vexira.mysticalterrain.boss.vfx;

import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

public final class Vfx {
   private static final double CAST_RANGE = 128.0;

   private Vfx() {
   }

   private static void spawn(
      ServerWorld level, VfxParticles.Kind kind, Vec3d at, int color, float scale, int life, float yaw, int count, double spread, Vec3d velocity, double speed
   ) {
      VfxOptions o = new VfxOptions(VfxParticles.type(kind), color, scale, life, yaw);
      if (count == 0) {
         level.spawnParticles(o, true, true, at.x, at.y, at.z, 0, velocity.x, velocity.y, velocity.z, speed);
      } else {
         level.spawnParticles(o, true, true, at.x, at.y, at.z, count, spread, spread * 0.5, spread, speed);
      }
   }

   private static void cast(ServerWorld level, VfxCastPayload.Effect effect, Vfx.Palette p, List<Vec3d> points, float... args) {
      if (!points.isEmpty()) {
         VfxCastPayload payload = new VfxCastPayload(effect, List.copyOf(points), args, p.core(), p.glow(), p.smoke(), p.debris(), level.getRandom().nextInt());
         Vec3d at = points.get(0);

         for (ServerPlayerEntity player : level.getPlayers()) {
            if (player.getEntityPos().squaredDistanceTo(at) <= 16384.0) {
               ServerPlayNetworking.send(player, payload);
            }
         }
      }
   }

   private static void cast(ServerWorld level, VfxCastPayload.Effect effect, Vfx.Palette p, Vec3d at, float... args) {
      cast(level, effect, p, List.of(at), args);
   }

   private static Vfx.Palette mono(int color) {
      return new Vfx.Palette(color, color, color);
   }

   public static void ring(ServerWorld level, Vec3d at, int color, double radius, int life) {
      spawn(level, VfxParticles.Kind.RING, at.add(0.0, 0.08, 0.0), color, (float)(radius * 2.0), life, 0.0F, 0, 0.0, Vec3d.ZERO, 0.0);
   }

   public static void flare(ServerWorld level, Vec3d at, int color, double size, int life) {
      spawn(level, VfxParticles.Kind.FLARE, at, color, (float)size, life, 0.0F, 0, 0.0, Vec3d.ZERO, 0.0);
   }

   public static void sparks(ServerWorld level, Vec3d at, int color, int count, double speed, double size) {
      cast(level, VfxCastPayload.Effect.SPARKS, mono(color), at, count, (float)speed, (float)size);
   }

   public static void embers(ServerWorld level, Vec3d at, int color, int count, double spread) {
      cast(level, VfxCastPayload.Effect.EMBERS, mono(color), at, count, (float)spread);
   }

   public static void smoke(ServerWorld level, Vec3d at, int color, int count, double spread, double size) {
      cast(level, VfxCastPayload.Effect.SMOKE, mono(color), at, count, (float)spread, (float)size);
   }

   public static void slash(ServerWorld level, Vec3d at, float yaw, int color, double size) {
      spawn(level, VfxParticles.Kind.SLASH, at, color, (float)size, 7, yaw, 0, 0.0, Vec3d.ZERO, 0.0);
   }

   public static void shards(ServerWorld level, Vec3d at, int color, int count, double speed) {
      cast(level, VfxCastPayload.Effect.SHARDS, mono(color), at, count, (float)speed);
   }

   public static void debris(ServerWorld level, Vec3d at, Vfx.Debris kind, int color, int count, double speed) {
      cast(level, VfxCastPayload.Effect.DEBRIS, mono(color), at, count, (float)speed, kind.ordinal());
   }

   public static void glint(ServerWorld level, Vec3d at, int color, double size) {
      cast(level, VfxCastPayload.Effect.GLINT, mono(color), at, (float)size);
   }

   public static void emit(ServerWorld level, VfxOptions o, Vec3d at, Vec3d velocity) {
      level.spawnParticles(o, true, true, at.x, at.y, at.z, 0, velocity.x, velocity.y, velocity.z, 1.0);
   }

   public static void emit(ServerWorld level, VfxOptions o, Vec3d at, int count, double spread, double speed) {
      level.spawnParticles(o, true, true, at.x, at.y, at.z, Math.max(1, count), spread, spread * 0.5, spread, speed);
   }

   public static void impact(ServerWorld level, Vec3d at, Vfx.Palette p, double power) {
      cast(level, VfxCastPayload.Effect.IMPACT, p, at, (float)power);
   }

   public static void shockwave(ServerWorld level, Vec3d at, Vfx.Palette p, double radius) {
      cast(level, VfxCastPayload.Effect.SHOCKWAVE, p, at, (float)radius);
   }

   public static void beam(ServerWorld level, Vec3d from, Vec3d to, Vfx.Palette p, double width) {
      cast(level, VfxCastPayload.Effect.BEAM, p, List.of(from, to), (float)width, 0.0F);
   }

   public static void beam(ServerWorld level, Vec3d from, Vec3d to, Vfx.Palette p, double width, int life) {
      cast(level, VfxCastPayload.Effect.BEAM, p, List.of(from, to), (float)width, life);
   }

   public static void gather(ServerWorld level, Vec3d at, Vfx.Palette p, double radius, int count) {
      cast(level, VfxCastPayload.Effect.GATHER, p, at, (float)radius, count);
   }

   public static void pillar(ServerWorld level, Vec3d at, Vfx.Palette p, double height, double width) {
      cast(level, VfxCastPayload.Effect.PILLAR, p, at, (float)height, (float)width);
   }

   public static void explosion(ServerWorld level, Vec3d at, Vfx.Palette p, double radius) {
      cast(level, VfxCastPayload.Effect.EXPLOSION, p, at, (float)radius);
   }

   public static void aura(ServerWorld level, Vec3d at, Vfx.Palette p, double radius) {
      cast(level, VfxCastPayload.Effect.AURA, p, at, (float)radius);
   }

   public static void warn(ServerWorld level, Vec3d at, Vfx.Palette p, double radius, int ticks) {
      telegraphCircle(level, at, radius, Math.max(6, ticks), p);
   }

   public static void trail(ServerWorld level, Vec3d at, Vfx.Palette p, double size) {
      cast(level, VfxCastPayload.Effect.TRAIL, p, at, (float)size);
   }

   public static void swing(ServerWorld level, Vec3d origin, float yaw, Vfx.Palette p, double reach) {
      cast(level, VfxCastPayload.Effect.SWING, p, origin, yaw, (float)reach);
   }

   public static void telegraphCircle(ServerWorld level, Vec3d center, double radius, int ticks, Vfx.Palette p) {
      cast(level, VfxCastPayload.Effect.TELEGRAPH_CIRCLE, p, center, (float)radius, Math.max(2, ticks));
   }

   public static void telegraphLine(ServerWorld level, Vec3d from, Vec3d dir, double length, double width, int ticks, Vfx.Palette p) {
      cast(level, VfxCastPayload.Effect.TELEGRAPH_LINE, p, List.of(from, from.add(dir)), (float)length, (float)width, Math.max(2, ticks));
   }

   public static void telegraphCone(ServerWorld level, Vec3d origin, float yaw, float halfAngle, double range, int ticks, Vfx.Palette p) {
      cast(level, VfxCastPayload.Effect.TELEGRAPH_CONE, p, origin, yaw, MathHelper.clamp(halfAngle, 5.0F, 180.0F), (float)range, Math.max(2, ticks));
   }

   public static void lightning(ServerWorld level, Vec3d from, Vec3d to, Vfx.Palette p, int branches) {
      cast(level, VfxCastPayload.Effect.LIGHTNING, p, List.of(from, to), MathHelper.clamp(branches, 0, 6));
   }

   public static void chainLightning(ServerWorld level, List<Vec3d> points, Vfx.Palette p) {
      if (points.size() >= 2) {
         cast(level, VfxCastPayload.Effect.CHAIN_LIGHTNING, p, points.size() > 8 ? points.subList(0, 8) : points);
      }
   }

   public static void vortex(ServerWorld level, Vec3d center, double radius, double height, Vfx.Palette p, int ticks) {
      cast(level, VfxCastPayload.Effect.VORTEX, p, center, (float)radius, (float)height, Math.max(10, ticks));
   }

   public static void nova(ServerWorld level, Vec3d center, Vfx.Palette p, double radius) {
      cast(level, VfxCastPayload.Effect.NOVA, p, center, (float)radius);
   }

   public static void runeCircle(ServerWorld level, Vec3d center, double radius, Vfx.Palette p, int life, float spin) {
      runeCircle(level, center, radius, p, life, spin, p.rune());
   }

   public static void runeCircle(ServerWorld level, Vec3d center, double radius, Vfx.Palette p, int life, float spin, Vfx.Rune rune) {
      cast(level, VfxCastPayload.Effect.RUNE_CIRCLE, p, center, (float)radius, Math.max(4, life), spin, rune.ordinal());
   }

   public static void chargeUp(ServerWorld level, Vec3d at, Vfx.Palette p, int ticks) {
      cast(level, VfxCastPayload.Effect.CHARGE_UP, p, at, Math.max(6, ticks));
   }

   public static void meteorTrail(ServerWorld level, Vec3d at, Vec3d velocity, Vfx.Palette p) {
      cast(level, VfxCastPayload.Effect.METEOR_TRAIL, p, List.of(at, at.add(velocity)));
   }

   public static void groundCrack(ServerWorld level, Vec3d center, double radius, Vfx.Palette p) {
      cast(level, VfxCastPayload.Effect.GROUND_CRACK, p, center, (float)radius);
   }

   public static void heavyImpact(ServerWorld level, Vec3d at, Vfx.Palette p, double power) {
      cast(level, VfxCastPayload.Effect.HEAVY_IMPACT, p, at, (float)power);
   }

   public static void shieldBubble(ServerWorld level, Vec3d at, double radius, Vfx.Palette p, int life) {
      cast(level, VfxCastPayload.Effect.SHIELD_BUBBLE, p, at, (float)radius, Math.max(4, life));
   }

   public static void ward(ServerWorld level, Vec3d at, double radius, Vfx.Palette p, int life) {
      cast(level, VfxCastPayload.Effect.WARD, p, at, (float)radius, Math.max(4, life), p.rune().ordinal());
   }

   public static void dashTrail(ServerWorld level, Vec3d from, Vec3d to, Vfx.Palette p) {
      cast(level, VfxCastPayload.Effect.DASH_TRAIL, p, List.of(from, to));
   }

   public static void auraRing(ServerWorld level, Vec3d at, Vfx.Palette p) {
      auraRing(level, at, p, 1.2);
   }

   public static void auraRing(ServerWorld level, Vec3d at, Vfx.Palette p, double radius) {
      cast(level, VfxCastPayload.Effect.AURA_RING, p, at, (float)radius);
   }

   public static void wisps(ServerWorld level, Vec3d center, Vfx.Palette p, int count) {
      wisps(level, center, p, count, 1.6, 50);
   }

   public static void wisps(ServerWorld level, Vec3d center, Vfx.Palette p, int count, double radius, int life) {
      cast(level, VfxCastPayload.Effect.WISPS, p, center, MathHelper.clamp(count, 1, 12), (float)radius, Math.max(10, life));
   }

   public static void slashArc(ServerWorld level, Vec3d origin, float yaw, float pitch, double reach, Vfx.Palette p, float arcDegrees) {
      slashArc(level, origin, yaw, pitch, 0.0F, reach, p, arcDegrees);
   }

   public static void slashArc(ServerWorld level, Vec3d origin, float yaw, float pitch, float roll, double reach, Vfx.Palette p, float arcDegrees) {
      cast(level, VfxCastPayload.Effect.SLASH_ARC, p, origin, yaw, pitch, roll, (float)reach, arcDegrees);
   }

   public static void claw(ServerWorld level, Vec3d origin, float yaw, float pitch, double reach, Vfx.Palette p) {
      cast(level, VfxCastPayload.Effect.CLAW, p, origin, yaw, pitch, (float)reach);
   }

   public static void streaks(ServerWorld level, Vec3d at, Vec3d dir, Vfx.Palette p, int count) {
      cast(level, VfxCastPayload.Effect.STREAKS, p, List.of(at, at.add(dir)), MathHelper.clamp(count, 1, 16));
   }

   public static void shake(ServerWorld level, Vec3d at, double radius, double strength, int ticks) {
      screen(level, at, radius, strength, ticks, 0, 0.0F);
   }

   public static void flash(ServerWorld level, Vec3d at, double radius, int color, float strength) {
      screen(level, at, radius, 0.0, 0, color, strength);
   }

   public static void radialFlash(ServerWorld level, Vec3d at, double radius, int color, float strength) {
      screen(level, at, radius, 0.0, 0, color, strength, 1);
   }

   public static void impactFrame(ServerWorld level, Vec3d at, double radius, int color, float strength) {
      screen(level, at, radius, strength, 8, color, strength, 2);
   }

   public static void screen(ServerWorld level, Vec3d at, double radius, double strength, int ticks, int flashColor, float flash) {
      screen(level, at, radius, strength, ticks, flashColor, flash, 0);
   }

   public static void screen(ServerWorld level, Vec3d at, double radius, double strength, int ticks, int flashColor, float flash, int style) {
      for (ServerPlayerEntity player : level.getPlayers()) {
         double d = player.getEntityPos().distanceTo(at);
         if (!(d > radius)) {
            float falloff = (float)MathHelper.clamp(1.0 - d / radius, 0.15, 1.0);
            ServerPlayNetworking.send(player, new FxPayload((float)strength * falloff, ticks, flashColor, flash * falloff, style));
         }
      }
   }

   public enum Debris {
      FEATHER,
      PETAL,
      LEAF,
      SNOW,
      ASH,
      BONE;
   }

   public record Palette(int core, int glow, int smoke, int debris) {
      public static final Vfx.Palette FIRE = new Vfx.Palette(16769162, 16734740, 3810848, Vfx.Debris.ASH);
      public static final Vfx.Palette FROST = new Vfx.Palette(15924479, 8377599, 12114152, Vfx.Debris.SNOW);
      public static final Vfx.Palette FAE = new Vfx.Palette(16773370, 16743126, 10409610, Vfx.Debris.PETAL);
      public static final Vfx.Palette PRIMAL = new Vfx.Palette(16773808, 14721088, 7232064, Vfx.Debris.BONE);
      public static final Vfx.Palette SHADOW = new Vfx.Palette(15259903, 9063679, 1840168, Vfx.Debris.ASH);
      public static final Vfx.Palette MIRE = new Vfx.Palette(15138736, 8179776, 3820074, Vfx.Debris.LEAF);
      public static final Vfx.Palette SAND = new Vfx.Palette(16774344, 16757818, 13150320);
      public static final Vfx.Palette SPORE = new Vfx.Palette(14221302, 4253896, 5917264);
      public static final Vfx.Palette ARCANE = new Vfx.Palette(15269119, 4245759, 2767456);
      public static final Vfx.Palette STAR = new Vfx.Palette(16777215, 16767088, 3814512);
      public static final Vfx.Palette VOID = new Vfx.Palette(16767231, 11550975, 1444386);
      public static final Vfx.Palette MONO = new Vfx.Palette(16777215, 14212328, 657932);
      public static final Vfx.Palette BLOOD = new Vfx.Palette(16766678, 14688314, 2755086, Vfx.Debris.BONE);
      public static final Vfx.Palette HOLY = new Vfx.Palette(16777204, 16769930, 15260864, Vfx.Debris.FEATHER);
      public static final Vfx.Palette TOXIC = new Vfx.Palette(16056264, 9236538, 3029016, Vfx.Debris.LEAF);
      public static final Vfx.Palette STORM = new Vfx.Palette(16186111, 8367871, 2896968);

      public Palette(int core, int glow, int smoke) {
         this(core, glow, smoke, -1);
      }

      private Palette(int core, int glow, int smoke, Vfx.Debris debris) {
         this(core, glow, smoke, debris.ordinal());
      }

      public Vfx.Palette withDebris(Vfx.@Nullable Debris d) {
         return new Vfx.Palette(this.core, this.glow, this.smoke, d == null ? -1 : d.ordinal());
      }

      public Vfx.@Nullable Debris debrisKind() {
         return this.debris >= 0 && this.debris < Vfx.Debris.values().length ? Vfx.Debris.values()[this.debris] : null;
      }

      public Vfx.Rune rune() {
         if (this.equals(ARCANE) || this.equals(FAE) || this.equals(SPORE)) {
            return Vfx.Rune.ARCANE;
         } else if (this.equals(FIRE) || this.equals(SAND) || this.equals(HOLY) || this.equals(STAR)) {
            return Vfx.Rune.SOLAR;
         } else {
            return !this.equals(SHADOW) && !this.equals(VOID) && !this.equals(MONO) && !this.equals(BLOOD) ? Vfx.Rune.GLYPH : Vfx.Rune.ASTRAL;
         }
      }

      public static Vfx.Palette byName(String name) {
         return switch (name.toUpperCase(Locale.ROOT)) {
            case "FIRE" -> FIRE;
            case "FROST" -> FROST;
            case "FAE" -> FAE;
            case "PRIMAL" -> PRIMAL;
            case "SHADOW" -> SHADOW;
            case "MIRE" -> MIRE;
            case "SAND" -> SAND;
            case "SPORE" -> SPORE;
            case "ARCANE" -> ARCANE;
            case "STAR" -> STAR;
            case "MONO" -> MONO;
            case "BLOOD" -> BLOOD;
            case "HOLY" -> HOLY;
            case "TOXIC" -> TOXIC;
            case "STORM" -> STORM;
            default -> VOID;
         };
      }
   }

   public enum Rune {
      ARCANE,
      SOLAR,
      ASTRAL,
      GLYPH;
   }
}
