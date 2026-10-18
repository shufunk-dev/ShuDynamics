package com.vexira.mysticalterrain.boss.dragon;

import com.vexira.mysticalterrain.boss.BossSounds;
import com.vexira.mysticalterrain.boss.vfx.Vfx;
import com.vexira.mysticalterrain.boss.vfx.VfxOptions;
import com.vexira.mysticalterrain.boss.vfx.VfxParticles;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.EndSpikeFeature;
import net.minecraft.world.level.levelgen.feature.EndSpikeFeature.EndSpike;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class DragonFx {
   public static final Vfx.Palette V = Vfx.Palette.MONO;
   public static final int WHITE = 16777215;
   public static final int PALE = 14211288;
   public static final int INK = 657930;
   public static final ParticleOptions POOL = new DustParticleOptions(15263976, 1.6F);
   private static final int ASH = 3815998;

   private DragonFx() {
   }

   public static ParticleOptions orbTrail() {
      return VfxOptions.of(VfxParticles.Kind.SMOKE, 14211288, 0.7, 14).to(3815998).build();
   }

   public static void breathBurst(EnderDragon dragon, boolean first) {
      Level level = dragon.level();
      RandomSource r = dragon.getRandom();
      Vec3 mouth = DragonPose.mouth(dragon, 1.0F);
      Vec3 aim = breathAim(dragon, mouth);
      if (first) {
         level.addParticle(VfxOptions.of(VfxParticles.Kind.FLARE, 16777215, 3.0, 5).build(), mouth.x, mouth.y, mouth.z, 0.0, 0.0, 0.0);
         level.addParticle(VfxOptions.of(VfxParticles.Kind.FLARE, V.glow(), 5.0, 8).build(), mouth.x, mouth.y, mouth.z, 0.0, 0.0, 0.0);
      }

      for (int i = 0; i < 8; i++) {
         Vec3 d = aim.yRot((float)((i - 3.5) / 3.5 * 30.0 * (float) (Math.PI / 180.0)));
         double v = 0.6 * (0.8 + r.nextDouble() * 0.4);
         level.addParticle(
            VfxOptions.of(VfxParticles.Kind.SPARK, 16777215, 0.3, 9).streak(2.5F).gravity(0.0F).drag(0.93F).build(),
            mouth.x,
            mouth.y,
            mouth.z,
            d.x * v * 1.4,
            d.y * v * 1.4,
            d.z * v * 1.4
         );
         level.addParticle(
            VfxOptions.of(VfxParticles.Kind.SMOKE, 14211288, 1.1 + r.nextDouble() * 0.9, 20 + r.nextInt(12)).to(3815998).drag(0.9F).build(),
            mouth.x + r.nextGaussian() * 0.2,
            mouth.y + r.nextGaussian() * 0.2,
            mouth.z + r.nextGaussian() * 0.2,
            d.x * v,
            d.y * v,
            d.z * v
         );
      }
   }

   public static void breathStream(EnderDragon dragon) {
      Level level = dragon.level();
      RandomSource r = dragon.getRandom();
      Vec3 mouth = DragonPose.mouth(dragon, 1.0F);
      Vec3 aim = breathAim(dragon, mouth);

      for (int i = 0; i < 3; i++) {
         Vec3 d = aim.yRot((float)(r.nextGaussian() * 10.0 * (float) (Math.PI / 180.0)));
         double v = 0.55 * (0.8 + r.nextDouble() * 0.4);
         level.addParticle(
            VfxOptions.of(VfxParticles.Kind.SMOKE, 14211288, 0.9 + r.nextDouble() * 0.7, 18 + r.nextInt(10)).to(3815998).drag(0.9F).build(),
            mouth.x,
            mouth.y,
            mouth.z,
            d.x * v,
            d.y * v,
            d.z * v
         );
      }

      Vec3 d = aim.yRot((float)(r.nextGaussian() * 6.0 * (float) (Math.PI / 180.0)));
      level.addParticle(
         VfxOptions.of(VfxParticles.Kind.SPARK, 16777215, 0.25, 8).streak(2.5F).gravity(0.0F).drag(0.93F).build(),
         mouth.x,
         mouth.y,
         mouth.z,
         d.x * 0.8,
         d.y * 0.8,
         d.z * 0.8
      );
   }

   public static void breathTrail(EnderDragon dragon) {
      Level level = dragon.level();
      RandomSource r = dragon.getRandom();
      Vec3 mouth = DragonPose.mouth(dragon, 1.0F);
      Vec3 f = forward(dragon.getYRot());
      Vec3 at = mouth.add(f.scale(0.6)).add(r.nextGaussian() * 0.2, -0.2 + r.nextGaussian() * 0.15, r.nextGaussian() * 0.2);
      level.addParticle(
         VfxOptions.of(VfxParticles.Kind.SMOKE, 14211288, 0.8 + r.nextDouble() * 0.6, 16 + r.nextInt(10)).to(3815998).build(),
         at.x,
         at.y,
         at.z,
         f.x * 0.05,
         -0.03,
         f.z * 0.05
      );
      if (r.nextInt(3) == 0) {
         level.addParticle(
            VfxOptions.of(VfxParticles.Kind.SPARK, 16777215, 0.22, 7).build(),
            at.x,
            at.y,
            at.z,
            f.x * 0.25 + r.nextGaussian() * 0.05,
            -0.12,
            f.z * 0.25 + r.nextGaussian() * 0.05
         );
      }
   }

   private static Vec3 breathAim(EnderDragon dragon, Vec3 mouth) {
      Vec3 f = forward(dragon.getYRot());
      double x = dragon.head.getX() + f.x * 2.5;
      double z = dragon.head.getZ() + f.z * 2.5;
      int top = dragon.level().getHeight(Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
      double y = top > dragon.level().getMinY() + 1 && top < mouth.y - 1.0 ? top : mouth.y - 4.0;
      Vec3 d = new Vec3(x - mouth.x, y - mouth.y, z - mouth.z);
      return d.lengthSqr() < 1.0E-4 ? f : d.normalize();
   }

   public static Vec3 forward(float yaw) {
      float r = yaw * (float) (Math.PI / 180.0);
      return new Vec3(Mth.sin(r), 0.0, -Mth.cos(r));
   }

   public static Vec3 right(float yaw) {
      float r = yaw * (float) (Math.PI / 180.0);
      return new Vec3(Mth.cos(r), 0.0, Mth.sin(r));
   }

   public static float dragonYaw(Vec3 dir) {
      return (float)(Mth.atan2(dir.x, -dir.z) * 180.0F / (float)Math.PI);
   }

   public static float fxYaw(Vec3 dir) {
      return (float)(Mth.atan2(-dir.x, dir.z) * 180.0F / (float)Math.PI);
   }

   public static Vec3 flat(Vec3 v) {
      return new Vec3(v.x, 0.0, v.z);
   }

   public static double flatDist(Vec3 a, Vec3 b) {
      double dx = a.x - b.x;
      double dz = a.z - b.z;
      return Math.sqrt(dx * dx + dz * dz);
   }

   public static Vec3 away(Vec3 c, Vec3 p) {
      Vec3 d = new Vec3(p.x - c.x, 0.0, p.z - c.z);
      return d.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : d.normalize();
   }

   public static double angle(Vec3 a, Vec3 b) {
      Vec3 fa = flat(a);
      Vec3 fb = flat(b);
      return !(fa.lengthSqr() < 1.0E-6) && !(fb.lengthSqr() < 1.0E-6)
         ? Math.acos(Mth.clamp(fa.normalize().dot(fb.normalize()), -1.0, 1.0)) * 180.0F / (float)Math.PI
         : 0.0;
   }

   public static boolean inCone(Vec3 origin, Vec3 dir, double halfAngle, double range, Vec3 p) {
      Vec3 d = flat(p.subtract(origin));
      return d.lengthSqr() > range * range ? false : d.lengthSqr() < 1.0 || angle(dir, d) <= halfAngle;
   }

   public static double toSegment(Vec3 p, Vec3 a, Vec3 b) {
      Vec3 ab = b.subtract(a);
      double len2 = ab.lengthSqr();
      double t = len2 < 1.0E-6 ? 0.0 : Mth.clamp(p.subtract(a).dot(ab) / len2, 0.0, 1.0);
      return p.distanceTo(a.add(ab.scale(t)));
   }

   public static double toSegmentFlat(Vec3 p, Vec3 a, Vec3 b) {
      return toSegment(flat(p), flat(a), flat(b));
   }

   public static Vec3 center(Entity e) {
      return e.position().add(0.0, e.getBbHeight() * 0.5, 0.0);
   }

   public static @Nullable Vec3 ground(ServerLevel level, double x, double z) {
      BlockPos top = level.getHeightmapPos(Types.MOTION_BLOCKING, BlockPos.containing(x, 0.0, z));
      return top.getY() <= level.getMinY() + 1 ? null : new Vec3(x, top.getY(), z);
   }

   public static @Nullable Vec3 groundBelow(ServerLevel level, Vec3 at) {
      MutableBlockPos p = BlockPos.containing(at.x, at.y + 0.2, at.z).mutable();

      for (int i = 0; i < 40 && p.getY() > level.getMinY(); i++) {
         if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty()) {
            return new Vec3(at.x, p.getY() + 1, at.z);
         }

         p.move(0, -1, 0);
      }

      Vec3 top = ground(level, at.x, at.z);
      return top != null && top.y <= at.y ? top : null;
   }

   public static List<ServerPlayer> players(ServerLevel level, Vec3 c, double r) {
      double r2 = r * r;
      return level.getPlayers(p -> p.isAlive() && !p.isSpectator() && !p.isCreative() && p.position().distanceToSqr(c) <= r2);
   }

   public static void hurt(ServerLevel level, EnderDragon dragon, LivingEntity victim, float amount, boolean magic) {
      DamageSource source = magic ? dragon.damageSources().indirectMagic(dragon, dragon) : dragon.damageSources().mobAttack(dragon);
      victim.hurtServer(level, source, amount);
   }

   public static void launch(Entity e, Vec3 velocity) {
      e.setDeltaMovement(velocity);
      e.syncVelocity = true;
   }

   public static void nudge(Entity e, Vec3 add) {
      Vec3 base = e instanceof ServerPlayer sp ? sp.getKnownMovement() : e.getDeltaMovement();
      e.setDeltaMovement(base.add(add));
      e.syncVelocity = true;
   }

   public static void sound(ServerLevel level, Vec3 at, String key, float volume, float pitch) {
      BossSounds.play(level, at, "boss.ender_dragon." + key, volume, pitch);
   }

   public static void pool(ServerLevel level, EnderDragon dragon, Vec3 at, float radius, int duration) {
      AreaEffectCloud cloud = new AreaEffectCloud(level, at.x, at.y, at.z);
      cloud.setOwner(dragon);
      cloud.setRadius(radius);
      cloud.setDuration(duration);
      cloud.setRadiusPerTick(-radius / duration * 0.5F);
      cloud.setCustomParticle(POOL);
      cloud.setPotionDurationScale(0.25F);
      cloud.addEffect(new MobEffectInstance(MobEffects.INSTANT_DAMAGE));
      level.addFreshEntity(cloud);
      Vfx.smoke(level, at.add(0.0, 0.2, 0.0), V.smoke(), 6, radius * 0.6, 1.4);
      Vfx.ring(level, at, V.glow(), radius, 14);
   }

   public static void orbBurst(ServerLevel level, Vec3 at, boolean loud) {
      Vfx.impact(level, at, V, 1.0);
      Vfx.smoke(level, at, V.smoke(), 10, 1.6, 1.8);
      Vfx.ring(level, at.add(0.0, 0.1, 0.0), V.glow(), 3.5, 14);
      if (loud) {
         sound(level, at, "fireball.hit", 2.5F, 0.9F + level.getRandom().nextFloat() * 0.2F);
      }
   }

   public static boolean clear(ServerLevel level, Vec3 from, Vec3 to, Entity dragon) {
      return level.clip(new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, dragon)).getType()
         == net.minecraft.world.phys.HitResult.Type.MISS;
   }

   public static Vec3 rayEnd(ServerLevel level, Vec3 from, Vec3 to, Entity dragon) {
      HitResult hit = level.clip(new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, dragon));
      return hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? to : hit.getLocation();
   }

   public static Vec3 spikeTop(EndSpike spike) {
      return new Vec3(spike.getCenterX() + 0.5, spike.getHeight() + 1, spike.getCenterZ() + 0.5);
   }

   public static List<EndSpike> emptySpikes(ServerLevel level) {
      List<EndSpike> out = new ArrayList<>();

      for (EndSpike spike : EndSpikeFeature.getSpikesForLevel(level)) {
         if (level.getEntitiesOfClass(EndCrystal.class, spike.getTopBoundingBox(), c -> !c.isRemoved()).isEmpty()) {
            out.add(spike);
         }
      }

      return out;
   }

   public static List<EndCrystal> spikeCrystals(ServerLevel level) {
      List<EndCrystal> out = new ArrayList<>();

      for (EndSpike spike : EndSpikeFeature.getSpikesForLevel(level)) {
         out.addAll(level.getEntitiesOfClass(EndCrystal.class, spike.getTopBoundingBox(), c -> !c.isRemoved()));
      }

      return out;
   }

   public static EndCrystal rebuildCrystal(ServerLevel level, EndSpike spike) {
      BlockPos top = new BlockPos(spike.getCenterX(), spike.getHeight(), spike.getCenterZ());
      if (level.getBlockState(top).isAir()) {
         level.setBlockAndUpdate(top, Blocks.BEDROCK.defaultBlockState());
      }

      EndCrystal crystal = new EndCrystal(level, top.getX() + 0.5, top.getY() + 1, top.getZ() + 0.5);
      crystal.setShowBottom(true);
      level.addFreshEntity(crystal);
      Vec3 base = Vec3.atBottomCenterOf(top.above());
      level.sendParticles(ParticleTypes.END_ROD, base.x, base.y + 0.5, base.z, 40, 0.3, 0.3, 0.3, 0.15);
      Vfx.pillar(level, Vec3.atBottomCenterOf(top), V, 12.0, 1.4);
      Vfx.explosion(level, base.add(0.0, 0.5, 0.0), V, 2.0);
      BossSounds.play(level, base, "boss.ender_dragon.crystal_recall.hit", 3.0F, 1.4F);
      return crystal;
   }
}
