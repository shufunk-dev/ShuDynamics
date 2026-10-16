package com.vexira.mysticalterrain.boss.dragon;

import com.vexira.mysticalterrain.boss.BossSounds;
import com.vexira.mysticalterrain.boss.vfx.Vfx;
import com.vexira.mysticalterrain.boss.vfx.VfxOptions;
import com.vexira.mysticalterrain.boss.vfx.VfxParticles;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Blocks;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.Heightmap.Type;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import net.minecraft.world.gen.feature.EndSpikeFeature;
import net.minecraft.world.gen.feature.EndSpikeFeature.Spike;
import org.jspecify.annotations.Nullable;

public final class DragonFx {
   public static final Vfx.Palette V = Vfx.Palette.MONO;
   public static final int WHITE = 16777215;
   public static final int PALE = 14211288;
   public static final int INK = 657930;
   public static final ParticleEffect POOL = new DustParticleEffect(15263976, 1.6F);
   private static final int ASH = 3815998;

   private DragonFx() {
   }

   public static ParticleEffect orbTrail() {
      return VfxOptions.of(VfxParticles.Kind.SMOKE, 14211288, 0.7, 14).to(3815998).build();
   }

   public static void breathBurst(EnderDragonEntity dragon, boolean first) {
      World level = dragon.getEntityWorld();
      Random r = dragon.getRandom();
      Vec3d mouth = DragonPose.mouth(dragon, 1.0F);
      Vec3d aim = breathAim(dragon, mouth);
      if (first) {
         level.addParticleClient(VfxOptions.of(VfxParticles.Kind.FLARE, 16777215, 3.0, 5).build(), mouth.x, mouth.y, mouth.z, 0.0, 0.0, 0.0);
         level.addParticleClient(VfxOptions.of(VfxParticles.Kind.FLARE, V.glow(), 5.0, 8).build(), mouth.x, mouth.y, mouth.z, 0.0, 0.0, 0.0);
      }

      for (int i = 0; i < 8; i++) {
         Vec3d d = aim.rotateY((float)((i - 3.5) / 3.5 * 30.0 * (float) (Math.PI / 180.0)));
         double v = 0.6 * (0.8 + r.nextDouble() * 0.4);
         level.addParticleClient(
            VfxOptions.of(VfxParticles.Kind.SPARK, 16777215, 0.3, 9).streak(2.5F).gravity(0.0F).drag(0.93F).build(),
            mouth.x,
            mouth.y,
            mouth.z,
            d.x * v * 1.4,
            d.y * v * 1.4,
            d.z * v * 1.4
         );
         level.addParticleClient(
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

   public static void breathStream(EnderDragonEntity dragon) {
      World level = dragon.getEntityWorld();
      Random r = dragon.getRandom();
      Vec3d mouth = DragonPose.mouth(dragon, 1.0F);
      Vec3d aim = breathAim(dragon, mouth);

      for (int i = 0; i < 3; i++) {
         Vec3d d = aim.rotateY((float)(r.nextGaussian() * 10.0 * (float) (Math.PI / 180.0)));
         double v = 0.55 * (0.8 + r.nextDouble() * 0.4);
         level.addParticleClient(
            VfxOptions.of(VfxParticles.Kind.SMOKE, 14211288, 0.9 + r.nextDouble() * 0.7, 18 + r.nextInt(10)).to(3815998).drag(0.9F).build(),
            mouth.x,
            mouth.y,
            mouth.z,
            d.x * v,
            d.y * v,
            d.z * v
         );
      }

      Vec3d d = aim.rotateY((float)(r.nextGaussian() * 6.0 * (float) (Math.PI / 180.0)));
      level.addParticleClient(
         VfxOptions.of(VfxParticles.Kind.SPARK, 16777215, 0.25, 8).streak(2.5F).gravity(0.0F).drag(0.93F).build(),
         mouth.x,
         mouth.y,
         mouth.z,
         d.x * 0.8,
         d.y * 0.8,
         d.z * 0.8
      );
   }

   public static void breathTrail(EnderDragonEntity dragon) {
      World level = dragon.getEntityWorld();
      Random r = dragon.getRandom();
      Vec3d mouth = DragonPose.mouth(dragon, 1.0F);
      Vec3d f = forward(dragon.getYaw());
      Vec3d at = mouth.add(f.multiply(0.6)).add(r.nextGaussian() * 0.2, -0.2 + r.nextGaussian() * 0.15, r.nextGaussian() * 0.2);
      level.addParticleClient(
         VfxOptions.of(VfxParticles.Kind.SMOKE, 14211288, 0.8 + r.nextDouble() * 0.6, 16 + r.nextInt(10)).to(3815998).build(),
         at.x,
         at.y,
         at.z,
         f.x * 0.05,
         -0.03,
         f.z * 0.05
      );
      if (r.nextInt(3) == 0) {
         level.addParticleClient(
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

   private static Vec3d breathAim(EnderDragonEntity dragon, Vec3d mouth) {
      Vec3d f = forward(dragon.getYaw());
      double x = dragon.head.getX() + f.x * 2.5;
      double z = dragon.head.getZ() + f.z * 2.5;
      int top = dragon.getEntityWorld().getTopY(Type.MOTION_BLOCKING, MathHelper.floor(x), MathHelper.floor(z));
      double y = top > dragon.getEntityWorld().getBottomY() + 1 && top < mouth.y - 1.0 ? top : mouth.y - 4.0;
      Vec3d d = new Vec3d(x - mouth.x, y - mouth.y, z - mouth.z);
      return d.lengthSquared() < 1.0E-4 ? f : d.normalize();
   }

   public static Vec3d forward(float yaw) {
      float r = yaw * (float) (Math.PI / 180.0);
      return new Vec3d(MathHelper.sin(r), 0.0, -MathHelper.cos(r));
   }

   public static Vec3d right(float yaw) {
      float r = yaw * (float) (Math.PI / 180.0);
      return new Vec3d(MathHelper.cos(r), 0.0, MathHelper.sin(r));
   }

   public static float dragonYaw(Vec3d dir) {
      return (float)(MathHelper.atan2(dir.x, -dir.z) * 180.0F / (float)Math.PI);
   }

   public static float fxYaw(Vec3d dir) {
      return (float)(MathHelper.atan2(-dir.x, dir.z) * 180.0F / (float)Math.PI);
   }

   public static Vec3d flat(Vec3d v) {
      return new Vec3d(v.x, 0.0, v.z);
   }

   public static double flatDist(Vec3d a, Vec3d b) {
      double dx = a.x - b.x;
      double dz = a.z - b.z;
      return Math.sqrt(dx * dx + dz * dz);
   }

   public static Vec3d away(Vec3d c, Vec3d p) {
      Vec3d d = new Vec3d(p.x - c.x, 0.0, p.z - c.z);
      return d.lengthSquared() < 1.0E-4 ? new Vec3d(1.0, 0.0, 0.0) : d.normalize();
   }

   public static double angle(Vec3d a, Vec3d b) {
      Vec3d fa = flat(a);
      Vec3d fb = flat(b);
      return !(fa.lengthSquared() < 1.0E-6) && !(fb.lengthSquared() < 1.0E-6)
         ? Math.acos(MathHelper.clamp(fa.normalize().dotProduct(fb.normalize()), -1.0, 1.0)) * 180.0F / (float)Math.PI
         : 0.0;
   }

   public static boolean inCone(Vec3d origin, Vec3d dir, double halfAngle, double range, Vec3d p) {
      Vec3d d = flat(p.subtract(origin));
      return d.lengthSquared() > range * range ? false : d.lengthSquared() < 1.0 || angle(dir, d) <= halfAngle;
   }

   public static double toSegment(Vec3d p, Vec3d a, Vec3d b) {
      Vec3d ab = b.subtract(a);
      double len2 = ab.lengthSquared();
      double t = len2 < 1.0E-6 ? 0.0 : MathHelper.clamp(p.subtract(a).dotProduct(ab) / len2, 0.0, 1.0);
      return p.distanceTo(a.add(ab.multiply(t)));
   }

   public static double toSegmentFlat(Vec3d p, Vec3d a, Vec3d b) {
      return toSegment(flat(p), flat(a), flat(b));
   }

   public static Vec3d center(Entity e) {
      return e.getEntityPos().add(0.0, e.getHeight() * 0.5, 0.0);
   }

   public static @Nullable Vec3d ground(ServerWorld level, double x, double z) {
      BlockPos top = level.getTopPosition(Type.MOTION_BLOCKING, BlockPos.ofFloored(x, 0.0, z));
      return top.getY() <= level.getBottomY() + 1 ? null : new Vec3d(x, top.getY(), z);
   }

   public static @Nullable Vec3d groundBelow(ServerWorld level, Vec3d at) {
      Mutable p = BlockPos.ofFloored(at.x, at.y + 0.2, at.z).mutableCopy();

      for (int i = 0; i < 40 && p.getY() > level.getBottomY(); i++) {
         if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty()) {
            return new Vec3d(at.x, p.getY() + 1, at.z);
         }

         p.move(0, -1, 0);
      }

      Vec3d top = ground(level, at.x, at.z);
      return top != null && top.y <= at.y ? top : null;
   }

   public static List<ServerPlayerEntity> players(ServerWorld level, Vec3d c, double r) {
      double r2 = r * r;
      return level.getPlayers(p -> p.isAlive() && !p.isSpectator() && !p.isCreative() && p.getEntityPos().squaredDistanceTo(c) <= r2);
   }

   public static void hurt(ServerWorld level, EnderDragonEntity dragon, LivingEntity victim, float amount, boolean magic) {
      DamageSource source = magic ? dragon.getDamageSources().indirectMagic(dragon, dragon) : dragon.getDamageSources().mobAttack(dragon);
      victim.damage(level, source, amount);
   }

   public static void launch(Entity e, Vec3d velocity) {
      e.setVelocity(velocity);
      e.knockedBack = true;
   }

   public static void nudge(Entity e, Vec3d add) {
      Vec3d base = e instanceof ServerPlayerEntity sp ? sp.getMovement() : e.getVelocity();
      e.setVelocity(base.add(add));
      e.knockedBack = true;
   }

   public static void sound(ServerWorld level, Vec3d at, String key, float volume, float pitch) {
      BossSounds.play(level, at, "boss.ender_dragon." + key, volume, pitch);
   }

   public static void pool(ServerWorld level, EnderDragonEntity dragon, Vec3d at, float radius, int duration) {
      AreaEffectCloudEntity cloud = new AreaEffectCloudEntity(level, at.x, at.y, at.z);
      cloud.setOwner(dragon);
      cloud.setRadius(radius);
      cloud.setDuration(duration);
      cloud.setRadiusGrowth(-radius / duration * 0.5F);
      cloud.setParticleType(POOL);
      cloud.setPotionDurationScale(0.25F);
      cloud.addEffect(new StatusEffectInstance(StatusEffects.INSTANT_DAMAGE));
      level.spawnEntity(cloud);
      Vfx.smoke(level, at.add(0.0, 0.2, 0.0), V.smoke(), 6, radius * 0.6, 1.4);
      Vfx.ring(level, at, V.glow(), radius, 14);
   }

   public static void orbBurst(ServerWorld level, Vec3d at, boolean loud) {
      Vfx.impact(level, at, V, 1.0);
      Vfx.smoke(level, at, V.smoke(), 10, 1.6, 1.8);
      Vfx.ring(level, at.add(0.0, 0.1, 0.0), V.glow(), 3.5, 14);
      if (loud) {
         sound(level, at, "fireball.hit", 2.5F, 0.9F + level.getRandom().nextFloat() * 0.2F);
      }
   }

   public static boolean clear(ServerWorld level, Vec3d from, Vec3d to, Entity dragon) {
      return level.raycast(new RaycastContext(from, to, ShapeType.COLLIDER, FluidHandling.NONE, dragon)).getType()
         == net.minecraft.util.hit.HitResult.Type.MISS;
   }

   public static Vec3d rayEnd(ServerWorld level, Vec3d from, Vec3d to, Entity dragon) {
      HitResult hit = level.raycast(new RaycastContext(from, to, ShapeType.COLLIDER, FluidHandling.NONE, dragon));
      return hit.getType() == net.minecraft.util.hit.HitResult.Type.MISS ? to : hit.getPos();
   }

   public static Vec3d spikeTop(Spike spike) {
      return new Vec3d(spike.getCenterX() + 0.5, spike.getHeight() + 1, spike.getCenterZ() + 0.5);
   }

   public static List<Spike> emptySpikes(ServerWorld level) {
      List<Spike> out = new ArrayList<>();

      for (Spike spike : EndSpikeFeature.getSpikes(level)) {
         if (level.getEntitiesByClass(EndCrystalEntity.class, spike.getBoundingBox(), c -> !c.isRemoved()).isEmpty()) {
            out.add(spike);
         }
      }

      return out;
   }

   public static List<EndCrystalEntity> spikeCrystals(ServerWorld level) {
      List<EndCrystalEntity> out = new ArrayList<>();

      for (Spike spike : EndSpikeFeature.getSpikes(level)) {
         out.addAll(level.getEntitiesByClass(EndCrystalEntity.class, spike.getBoundingBox(), c -> !c.isRemoved()));
      }

      return out;
   }

   public static EndCrystalEntity rebuildCrystal(ServerWorld level, Spike spike) {
      BlockPos top = new BlockPos(spike.getCenterX(), spike.getHeight(), spike.getCenterZ());
      if (level.getBlockState(top).isAir()) {
         level.setBlockState(top, Blocks.BEDROCK.getDefaultState());
      }

      EndCrystalEntity crystal = new EndCrystalEntity(level, top.getX() + 0.5, top.getY() + 1, top.getZ() + 0.5);
      crystal.setShowBottom(true);
      level.spawnEntity(crystal);
      Vec3d base = Vec3d.ofBottomCenter(top.up());
      level.spawnParticles(ParticleTypes.END_ROD, base.x, base.y + 0.5, base.z, 40, 0.3, 0.3, 0.3, 0.15);
      Vfx.pillar(level, Vec3d.ofBottomCenter(top), V, 12.0, 1.4);
      Vfx.explosion(level, base.add(0.0, 0.5, 0.0), V, 2.0);
      BossSounds.play(level, base, "boss.ender_dragon.crystal_recall.hit", 3.0F, 1.4F);
      return crystal;
   }
}
