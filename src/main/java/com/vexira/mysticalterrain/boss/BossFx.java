package com.vexira.mysticalterrain.boss;

import com.vexira.mysticalterrain.boss.entity.WardenShade;
import com.vexira.mysticalterrain.boss.vfx.Vfx;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.Heightmap.Type;
import org.jspecify.annotations.Nullable;

public final class BossFx {
   private static @Nullable MysticalBoss current;

   private BossFx() {
   }

   public static void current(@Nullable MysticalBoss boss) {
      current = boss;
   }

   public static Vfx.Palette pal() {
      return current != null ? current.palette() : Vfx.Palette.VOID;
   }

   private static Vfx.Palette pal(LivingEntity attacker) {
      return attacker instanceof MysticalBoss mb ? mb.palette() : pal();
   }

   private static boolean big() {
      return current == null || current.isBoss();
   }

   public static Vfx.Palette paletteFor(String id) {
      String boss = id;
      RealmMobs.Kind kind = RealmMobs.kind(id);
      if (kind != null) {
         boss = kind.boss();
      }
      return switch (boss) {
         case "pyraxis" -> Vfx.Palette.FIRE;
         case "hrimthane" -> Vfx.Palette.FROST;
         case "lirael" -> Vfx.Palette.FAE;
         case "grothmaw" -> Vfx.Palette.PRIMAL;
         case "hollow_warden", "warden_shade" -> Vfx.Palette.SHADOW;
         case "bogrot" -> Vfx.Palette.MIRE;
         case "sekhmar" -> Vfx.Palette.SAND;
         case "mycelord" -> Vfx.Palette.SPORE;
         case "archon" -> Vfx.Palette.ARCANE;
         case "astrael" -> Vfx.Palette.STAR;
         default -> Vfx.Palette.VOID;
      };
   }

   public static List<LivingEntity> victims(ServerWorld level, LivingEntity boss, Vec3d center, double radius) {
      return level.getEntitiesByClass(
         LivingEntity.class,
         new Box(center, center).expand(radius, radius * 0.75, radius),
         e -> e != boss
            && e.isAlive()
            && !(e instanceof MysticalBoss)
            && !(e instanceof WardenShade)
            && !(e instanceof PlayerEntity p && (p.isCreative() || p.isSpectator()))
            && e.getEntityPos().squaredDistanceTo(center) <= radius * radius
      );
   }

   public static List<LivingEntity> enemies(ServerWorld level, LivingEntity owner, Vec3d center, double radius) {
      return owner instanceof PlayerEntity
         ? level.getEntitiesByClass(
            LivingEntity.class,
            new Box(center, center).expand(radius, radius * 0.75, radius),
            e -> e != owner
               && e.isAlive()
               && !(e instanceof PlayerEntity)
               && !(e instanceof ArmorStandEntity)
               && !(e instanceof TameableEntity t && t.isOwner(owner))
               && e.getEntityPos().squaredDistanceTo(center) <= radius * radius
         )
         : victims(level, owner, center, radius);
   }

   public static void shockwave(ServerWorld level, LivingEntity boss, Vec3d center, double radius, float damage, double push) {
      DamageSource source = boss.getDamageSources().mobAttack(boss);

      for (LivingEntity e : victims(level, boss, center, radius)) {
         double d = Math.sqrt(e.getEntityPos().squaredDistanceTo(center));
         float f = (float)(1.0 - d / radius);
         e.damage(level, source, damage * (0.4F + 0.6F * f));
         Vec3d away = new Vec3d(e.getX() - center.x, 0.0, e.getZ() - center.z);
         away = away.lengthSquared() < 1.0E-4 ? new Vec3d(1.0, 0.0, 0.0) : away.normalize();
         e.addVelocity(away.x * push * (0.5 + f), 0.35 + 0.35 * f, away.z * push * (0.5 + f));
         e.knockedBack = true;
      }

      BlockState ground = level.getBlockState(BlockPos.ofFloored(center).down());
      ParticleEffect dust = (ParticleEffect)(ground.isAir() ? ParticleTypes.CLOUD : new BlockStateParticleEffect(ParticleTypes.BLOCK, ground));
      ring(level, center.add(0.0, 0.2, 0.0), radius * 0.5, 32, dust, 3);
      ring(level, center.add(0.0, 0.2, 0.0), radius, 48, dust, 3);
      level.spawnParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.5, center.z, 3, radius * 0.2, 0.2, radius * 0.2, 0.0);
      if (big()) {
         Vfx.shockwave(level, center, pal(boss), radius);
      } else {
         Vfx.ring(level, center, pal(boss).glow(), radius, 10);
         Vfx.flare(level, center.add(0.0, 0.3, 0.0), pal(boss).core(), radius * 0.5, 5);
      }
   }

   public static void cone(ServerWorld level, LivingEntity boss, double range, float halfAngle, Consumer<LivingEntity> effect) {
      Vec3d eye = boss.getEntityPos().add(0.0, boss.getHeight() * 0.5, 0.0);
      Vec3d facing = Vec3d.fromPolar(0.0F, boss.getYaw());

      for (LivingEntity e : victims(level, boss, eye, range)) {
         Vec3d to = new Vec3d(e.getX() - boss.getX(), 0.0, e.getZ() - boss.getZ());
         if (to.lengthSquared() < 1.0 || Math.toDegrees(Math.acos(MathHelper.clamp(to.normalize().dotProduct(facing), -1.0, 1.0))) <= halfAngle) {
            effect.accept(e);
         }
      }
   }

   public static void melee(ServerWorld level, LivingEntity boss, double range, float halfAngle, float damage, double push) {
      DamageSource source = boss.getDamageSources().mobAttack(boss);
      Vfx.Palette p = pal(boss);
      Vfx.swing(level, boss.getEntityPos(), boss.getYaw(), p, range * (big() ? 1.0 : 0.7));
      cone(level, boss, range, halfAngle, e -> {
         if (e.damage(level, source, damage)) {
            Vfx.impact(level, e.getEntityPos().add(0.0, e.getHeight() * 0.5, 0.0), p, big() ? 0.7 : 0.35);
            Vec3d away = new Vec3d(e.getX() - boss.getX(), 0.0, e.getZ() - boss.getZ()).normalize();
            e.addVelocity(away.x * push, 0.25 + push * 0.15, away.z * push);
            e.knockedBack = true;
         }
      });
   }

   public static void effect(ServerWorld level, LivingEntity boss, Vec3d center, double radius, StatusEffectInstance effect) {
      for (LivingEntity e : victims(level, boss, center, radius)) {
         e.addStatusEffect(new StatusEffectInstance(effect), boss);
      }
   }

   public static void ring(ServerWorld level, Vec3d center, double radius, int points, ParticleEffect particle, int count) {
      for (int i = 0; i < points; i++) {
         double a = i * Math.PI * 2.0 / points;
         level.spawnParticles(particle, center.x + Math.cos(a) * radius, center.y, center.z + Math.sin(a) * radius, count, 0.15, 0.1, 0.15, 0.02);
      }
   }

   public static void line(ServerWorld level, Vec3d from, Vec3d to, ParticleEffect particle, double spacing) {
      if (level.getTime() % 3L == 0L) {
         Vfx.beam(level, from, to, pal(), big() ? 0.55 : 0.3);
      }

      Vec3d d = to.subtract(from);
      double len = d.length();
      double s = 0.0;

      while (s < len) {
         Vec3d p = from.add(d.multiply(s / len));
         level.spawnParticles(particle, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
         s += spacing;
      }
   }

   public static void burst(ServerWorld level, Vec3d at, ParticleEffect particle, int count, double spread, double speed) {
      level.spawnParticles(particle, at.x, at.y, at.z, count, spread, spread, spread, speed);
      Vfx.Palette p = pal();
      Vfx.flare(level, at, p.glow(), 1.2 + spread * 1.5, 8);
      Vfx.flare(level, at, p.core(), 0.6 + spread * 0.8, 5);
      Vfx.sparks(level, at, p.core(), Math.min(24, 4 + count / 3), 0.2 + speed * 2.0 + spread * 0.15, 0.2);
   }

   public static @Nullable Vec3d ground(ServerWorld level, double x, double y, double z) {
      Mutable p = BlockPos.ofFloored(x, y + 6.0, z).mutableCopy();

      for (int i = 0; i < 18; i++) {
         if (!level.getBlockState(p).isAir() && level.getBlockState(p.up()).isAir()) {
            return new Vec3d(x, p.getY() + 1, z);
         }

         p.move(0, -1, 0);
      }

      BlockPos top = level.getTopPosition(Type.MOTION_BLOCKING_NO_LEAVES, BlockPos.ofFloored(x, y, z));
      return Math.abs(top.getY() - y) < 12.0 ? Vec3d.ofBottomCenter(top) : null;
   }

   public static Vec3d around(ServerWorld level, LivingEntity boss, Vec3d center, double minR, double maxR) {
      for (int i = 0; i < 8; i++) {
         double a = boss.getRandom().nextDouble() * Math.PI * 2.0;
         double r = minR + boss.getRandom().nextDouble() * (maxR - minR);
         Vec3d g = ground(level, center.x + Math.cos(a) * r, center.y, center.z + Math.sin(a) * r);
         if (g != null) {
            return g;
         }
      }

      return center;
   }

   public static <T extends MobEntity> void summonMinions(
      ServerWorld level, MysticalBoss boss, EntityType<T> type, int count, double radius, @Nullable String nameKey, Consumer<T> setup
   ) {
      for (int i = 0; i < count; i++) {
         Vec3d at = around(level, boss, boss.getEntityPos(), 2.0, radius);
         T mob = (T)type.create(level, SpawnReason.MOB_SUMMONED);
         if (mob != null) {
            mob.refreshPositionAndAngles(at.x, at.y, at.z, boss.getRandom().nextFloat() * 360.0F, 0.0F);
            if (nameKey != null) {
               mob.setCustomName(Text.translatable(nameKey));
            }

            if (boss.getTarget() != null) {
               mob.setTarget(boss.getTarget());
            }

            setup.accept(mob);
            level.spawnEntity(mob);
            level.spawnParticles(ParticleTypes.POOF, at.x, at.y + 0.5, at.z, 12, 0.3, 0.4, 0.3, 0.02);
            Vfx.pillar(level, at, boss.palette(), 2.5, 0.7);
         }
      }
   }

   public static void telegraph(ServerWorld level, MysticalBoss boss, Vec3d at, double radius, int delay, ParticleEffect warn, Consumer<Vec3d> strike) {
      Vfx.Palette p = boss.palette();
      Vfx.warn(level, at, p, radius, delay);
      boss.schedule(Math.max(1, delay / 2), () -> Vfx.warn(level, at, p, radius * 0.8, delay - delay / 2));
      boss.schedule(delay, () -> Vfx.impact(level, at, p, Math.max(0.6, Math.min(1.8, radius * 0.45))));

      for (int t = 0; t < delay; t += 3) {
         double shrink = 1.0 - (double)t / delay * 0.4;
         boss.schedule(t, () -> ring(level, at.add(0.0, 0.15, 0.0), radius * shrink, Math.max(8, (int)(radius * 8.0)), warn, 1));
      }

      boss.schedule(delay, () -> strike.accept(at));
   }

   public static void wave(
      ServerWorld level, MysticalBoss boss, Vec3d center, double maxRadius, int ticks, float damage, ParticleEffect particle, double push, boolean jumpable
   ) {
      Set<Integer> hit = new HashSet<>();
      DamageSource source = boss.getDamageSources().mobAttack(boss);
      Vfx.ring(level, center, boss.palette().glow(), maxRadius, (int)(ticks * 1.3));
      Vfx.ring(level, center, boss.palette().core(), maxRadius * 0.92, ticks);
      Vfx.flare(level, center.add(0.0, 0.4, 0.0), boss.palette().core(), 2.5, 6);

      for (int t = 1; t <= ticks; t++) {
         double r = maxRadius * t / ticks;
         boss.schedule(t, () -> {
            ring(level, center.add(0.0, 0.3, 0.0), r, Math.max(12, (int)(r * 6.0)), particle, 1);

            for (LivingEntity e : victims(level, boss, center, r + 1.0)) {
               double d = Math.sqrt((e.getX() - center.x) * (e.getX() - center.x) + (e.getZ() - center.z) * (e.getZ() - center.z));
               if (Math.abs(d - r) < 1.0 && (!jumpable || e.isOnGround()) && hit.add(e.getId())) {
                  e.damage(level, source, boss.dmg(damage));
                  Vec3d away = new Vec3d(e.getX() - center.x, 0.0, e.getZ() - center.z).normalize();
                  e.addVelocity(away.x * push, 0.4, away.z * push);
                  e.knockedBack = true;
               }
            }
         });
      }
   }

   public static void zone(
      ServerWorld level,
      MysticalBoss boss,
      Vec3d center,
      double radius,
      int duration,
      ParticleEffect particle,
      float damagePerSecond,
      @Nullable StatusEffectInstance effect
   ) {
      Vfx.Palette p = boss.palette();

      for (int t = 0; t < duration; t += 5) {
         int tick = t;
         boss.schedule(t, () -> {
            level.spawnParticles(particle, center.x, center.y + 0.3, center.z, (int)(radius * radius * 1.2), radius * 0.6, 0.15, radius * 0.6, 0.01);
            if (tick % 20 == 0) {
               Vfx.aura(level, center, p, radius);

               for (LivingEntity e : victims(level, boss, center, radius)) {
                  if (damagePerSecond > 0.0F) {
                     e.damage(level, boss.getDamageSources().indirectMagic(boss, boss), damagePerSecond);
                  }

                  if (effect != null) {
                     e.addStatusEffect(new StatusEffectInstance(effect), boss);
                  }
               }
            }
         });
      }
   }

   public static void pillar(ServerWorld level, Vec3d at, double height, ParticleEffect particle, int density) {
      Vfx.pillar(level, at, pal(), height, big() ? 0.9 : 0.5);

      for (double y = 0.0; y < height; y += 0.5) {
         level.spawnParticles(particle, at.x, at.y + y, at.z, density, 0.25, 0.1, 0.25, 0.02);
      }
   }

   public static void pull(ServerWorld level, LivingEntity boss, Vec3d center, double radius, double strength) {
      Vfx.gather(level, center.add(0.0, 0.8, 0.0), pal(), radius, (int)Math.min(30.0, radius * 3.0));

      for (LivingEntity e : victims(level, boss, center, radius)) {
         Vec3d to = center.subtract(e.getEntityPos());
         if (to.lengthSquared() > 1.0) {
            Vec3d v = to.normalize().multiply(strength);
            e.addVelocity(v.x, Math.max(0.05, v.y), v.z);
            e.knockedBack = true;
         }
      }
   }

   public static void launch(ServerWorld level, LivingEntity boss, Vec3d center, double radius, double up) {
      Vfx.ring(level, center, pal().glow(), radius, 10);
      Vfx.sparks(level, center, pal().core(), (int)(radius * 4.0), 0.5, 0.2);

      for (LivingEntity e : victims(level, boss, center, radius)) {
         e.addVelocity(0.0, up, 0.0);
         e.knockedBack = true;
      }
   }

   public static void tempBlocks(ServerWorld level, MysticalBoss boss, Iterable<BlockPos> positions, BlockState state, int duration) {
      List<BlockPos> placed = new ArrayList<>();

      for (BlockPos p : positions) {
         if (level.getBlockState(p).isAir()) {
            level.setBlockState(p, state);
            placed.add(p.toImmutable());
         }
      }

      boss.schedule(
         duration,
         () -> {
            for (BlockPos px : placed) {
               if (level.getBlockState(px).isOf(state.getBlock())) {
                  level.setBlockState(px, Blocks.AIR.getDefaultState());
                  level.spawnParticles(
                     new BlockStateParticleEffect(ParticleTypes.BLOCK, state), px.getX() + 0.5, px.getY() + 0.5, px.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.05
                  );
               }
            }
         }
      );
   }

   public static void strikesAround(
      ServerWorld level, MysticalBoss boss, Vec3d center, int count, double spread, double radius, int delay, ParticleEffect warn, Consumer<Vec3d> strike
   ) {
      for (int i = 0; i < count; i++) {
         Vec3d at = i == 0
            ? (ground(level, center.x, center.y, center.z) != null ? ground(level, center.x, center.y, center.z) : center)
            : around(level, boss, center, 1.5, spread);
         telegraph(level, boss, at, radius, delay + i * 4, warn, strike);
      }
   }

   public static void hurt(ServerWorld level, MysticalBoss boss, Vec3d at, double radius, float damage, @Nullable StatusEffectInstance effect) {
      Vfx.impact(level, at, boss.palette(), Math.max(0.4, Math.min(1.6, radius * 0.4)) * (boss.isBoss() ? 1.0 : 0.6));

      for (LivingEntity e : victims(level, boss, at, radius)) {
         e.damage(level, boss.getDamageSources().mobAttack(boss), boss.dmg(damage));
         if (effect != null) {
            e.addStatusEffect(new StatusEffectInstance(effect), boss);
         }
      }
   }

   public static void strike(ServerWorld level, MysticalBoss boss, double range, float halfAngle, float base, double push) {
      current(boss);
      melee(level, boss, range, halfAngle, boss.dmg(base), push);
   }

   public static void slam(ServerWorld level, MysticalBoss boss, Vec3d center, double radius, float base, double push) {
      current(boss);
      shockwave(level, boss, center, radius, boss.dmg(base), push);
   }

   public static void knockback(ServerWorld level, LivingEntity boss, Vec3d center, double radius, double push) {
      for (LivingEntity e : victims(level, boss, center, radius)) {
         Vec3d away = new Vec3d(e.getX() - center.x, 0.0, e.getZ() - center.z);
         away = away.lengthSquared() < 1.0E-4 ? new Vec3d(1.0, 0.0, 0.0) : away.normalize();
         double f = 1.0 - Math.sqrt(e.getEntityPos().squaredDistanceTo(center)) / radius;
         e.addVelocity(away.x * push * (0.5 + f), 0.3 + 0.3 * f, away.z * push * (0.5 + f));
         e.knockedBack = true;
      }
   }

   public static boolean safeGround(ServerWorld level, Vec3d at, double maxDrop) {
      Mutable p = BlockPos.ofFloored(at.x, at.y + 0.5, at.z).mutableCopy();

      for (int i = 0; i <= maxDrop + 1.0; i++) {
         BlockState s = level.getBlockState(p);
         if (!s.getFluidState().isEmpty()) {
            return false;
         }

         if (!s.getCollisionShape(level, p).isEmpty()) {
            return i > 0 || level.getBlockState(p.up()).getCollisionShape(level, p.up()).isEmpty();
         }

         p.move(0, -1, 0);
      }

      return false;
   }

   public static void staggerBurst(ServerWorld level, MysticalBoss boss) {
      Vfx.Palette p = boss.palette();
      Vec3d mid = boss.getEntityPos().add(0.0, boss.getHeight() * 0.6, 0.0);
      Vfx.shards(level, mid, p.core(), 18, 0.55);
      Vfx.shards(level, mid, 16777215, 8, 0.4);
      Vfx.sparks(level, mid, p.glow(), 24, 0.6, 0.24);
      Vfx.flare(level, mid, 16777215, 2.2 + boss.getWidth() * 0.4, 5);
      Vfx.flare(level, mid, p.glow(), 3.6 + boss.getWidth() * 0.6, 10);
      Vfx.ring(level, boss.getEntityPos(), p.glow(), 3.0 + boss.getWidth(), 14);
      Vfx.ring(level, boss.getEntityPos(), 16777215, 1.6 + boss.getWidth() * 0.5, 9);
      Vfx.shake(level, boss.getEntityPos(), 20.0, 0.5, 8);
   }

   public static void wardUp(ServerWorld level, MysticalBoss boss) {
      Vfx.Palette p = boss.palette();
      Vec3d mid = boss.getEntityPos().add(0.0, boss.getHeight() * 0.5, 0.0);
      Vfx.flare(level, mid, p.core(), boss.getHeight() * 0.9, 8);
      Vfx.flare(level, mid, p.glow(), boss.getHeight() * 1.5, 14);
      Vfx.ring(level, boss.getEntityPos(), p.glow(), 1.5 + boss.getWidth(), 16);
      Vfx.gather(level, mid, p, 2.5 + boss.getWidth(), 16);
   }

   public static void wardShatter(ServerWorld level, MysticalBoss boss) {
      Vfx.Palette p = boss.palette();
      Vec3d mid = boss.getEntityPos().add(0.0, boss.getHeight() * 0.5, 0.0);
      Vfx.shards(level, mid, p.core(), 26, 0.7);
      Vfx.shards(level, mid, p.glow(), 14, 0.5);
      Vfx.flare(level, mid, 16777215, boss.getHeight() * 0.8, 5);
      Vfx.ring(level, boss.getEntityPos(), p.core(), 2.5 + boss.getWidth(), 12);
      Vfx.screen(level, mid, 24.0, 0.4, 8, p.glow(), 0.2F);
   }

   public static void markTarget(ServerWorld level, MysticalBoss boss, LivingEntity marked, int ticks) {
      Vfx.Palette p = boss.palette();
      Vec3d head = marked.getEntityPos().add(0.0, marked.getHeight() + 0.8, 0.0);
      Vfx.flare(level, head, p.glow(), 1.4, Math.min(40, ticks));
      Vfx.flare(level, head, p.core(), 0.7, Math.min(30, ticks));
      Vfx.sparks(level, head, p.core(), 6, 0.15, 0.16);
      Vfx.warn(level, marked.getEntityPos(), p, 1.4, Math.min(30, ticks));
   }

   public static void blink(ServerWorld level, MysticalBoss boss, Vec3d at) {
      Vfx.Palette p = boss.palette();
      Vec3d mid = at.add(0.0, boss.getHeight() * 0.5, 0.0);
      Vfx.flare(level, mid, p.core(), 1.2 + boss.getHeight() * 0.4, 6);
      Vfx.flare(level, mid, p.glow(), 2.0 + boss.getHeight() * 0.7, 10);
      Vfx.sparks(level, mid, p.glow(), 14, 0.35, 0.2);
      Vfx.smoke(level, at, p.smoke(), 6, 0.6 + boss.getWidth() * 0.4, 1.2);
      Vfx.ring(level, at, p.glow(), 1.2 + boss.getWidth() * 0.6, 10);
   }

   public static void enrage(ServerWorld level, MysticalBoss boss) {
      Vfx.Palette p = boss.palette();
      Vfx.screen(level, boss.getEntityPos(), 48.0, 0.9, 24, p.glow(), 0.4F);
      Vfx.pillar(level, boss.getEntityPos(), p, boss.getHeight() * 3.0F, 1.6);
      Vfx.explosion(level, boss.getEntityPos().add(0.0, boss.getHeight() * 0.5, 0.0), p, 2.0);
   }
}
