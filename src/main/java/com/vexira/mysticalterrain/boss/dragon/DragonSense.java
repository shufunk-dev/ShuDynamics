package com.vexira.mysticalterrain.boss.dragon;

import com.vexira.mysticalterrain.boss.ai.ThreatTable;
import com.vexira.mysticalterrain.boss.vfx.Vfx;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.item.consume.UseAction;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap.Type;
import net.minecraft.world.gen.feature.EndSpikeFeature;
import net.minecraft.world.gen.feature.EndSpikeFeature.Spike;
import org.jspecify.annotations.Nullable;

final class DragonSense {
   private static final double ARENA = 150.0;
   private static final int[][] AROUND = new int[][]{{3, 0}, {-3, 0}, {0, 3}, {0, -3}};
   final ThreatTable threat = new ThreatTable();
   final List<ServerPlayerEntity> engaged = new ArrayList<>();
   @Nullable ServerPlayerEntity target;
   @Nullable ServerPlayerEntity marked;
   int markTicks;
   int nextMark = 600;
   int idleTicks;
   boolean gliding;
   boolean pillaring;
   boolean aiming;
   boolean healing;
   boolean blocking;
   boolean behind;
   int glidingTicks;
   int kitingTicks;
   int huggingTicks;
   int airTicks;
   private @Nullable UUID habitsOf;
   private float recentDamage;
   private float recentProjectile;

   void tick(ServerWorld level, EnderDragonEntity dragon, Vec3d portal, @Nullable ServerBossBar bar) {
      this.engaged.clear();

      for (ServerPlayerEntity p : bar != null ? bar.getPlayers() : level.getPlayers()) {
         if (p.isAlive() && !p.isSpectator() && !p.isCreative() && p.getEntityWorld() == level && DragonFx.flatDist(p.getEntityPos(), portal) <= 150.0) {
            this.engaged.add(p);
         }
      }

      this.threat.tick(dragon, this.engaged);

      for (ServerPlayerEntity p : this.engaged) {
         if (usesFood(p) && p.squaredDistanceTo(dragon) < 1600.0) {
            this.threat.add(p, 0.2F);
         }
      }

      this.recentDamage *= 0.98F;
      this.recentProjectile *= 0.98F;
      if (dragon.age % 10 == 0 || this.target != null && !this.engaged.contains(this.target)) {
         this.target = this.threat.pick(this.engaged, this.target, dragon);
      }

      this.tickMarks(level, dragon);
      this.habits(level, dragon);
   }

   private void tickMarks(ServerWorld level, EnderDragonEntity dragon) {
      if (this.markTicks > 0) {
         this.markTicks--;
         if (this.markTicks == 0 || this.marked == null || !this.engaged.contains(this.marked)) {
            this.marked = null;
            this.markTicks = 0;
         } else if (this.markTicks % 20 == 0) {
            Vfx.auraRing(level, this.marked.getEntityPos(), DragonFx.V, 1.5);
         }
      }

      if (this.engaged.size() >= 2) {
         if (--this.nextMark <= 0) {
            this.nextMark = 500 + dragon.getRandom().nextInt(201);
            List<ServerPlayerEntity> others = new ArrayList<>(this.engaged);
            others.remove(this.target);
            if (!others.isEmpty()) {
               this.marked = others.get(dragon.getRandom().nextInt(others.size()));
               this.markTicks = 140;
               Vfx.auraRing(level, this.marked.getEntityPos(), DragonFx.V, 1.5);
               Vfx.flare(level, this.marked.getEntityPos().add(0.0, this.marked.getHeight() + 0.8, 0.0), 16777215, 1.4, 30);
            }
         }
      }
   }

   void clearMarks() {
      this.marked = null;
      this.markTicks = 0;
   }

   @Nullable ServerPlayerEntity target() {
      return this.marked != null && this.markTicks > 0 ? this.marked : this.target;
   }

   boolean kiting() {
      return this.kitingTicks >= 100;
   }

   boolean hugging() {
      return this.huggingTicks >= 40;
   }

   void recordDamage(float raw, boolean projectile) {
      this.recentDamage += raw;
      if (projectile) {
         this.recentProjectile += raw;
      }
   }

   float projectileShare() {
      return this.recentDamage > 1.0F ? this.recentProjectile / this.recentDamage : 0.0F;
   }

   void reset() {
      this.threat.clear();
      this.clearMarks();
      this.target = null;
      this.recentDamage = 0.0F;
      this.recentProjectile = 0.0F;
   }

   private void habits(ServerWorld level, EnderDragonEntity dragon) {
      ServerPlayerEntity t = this.target();
      if (t == null) {
         this.gliding = this.pillaring = this.aiming = this.healing = this.blocking = this.behind = false;
         this.glidingTicks = this.kitingTicks = this.huggingTicks = this.airTicks = 0;
         this.habitsOf = null;
      } else {
         if (!t.getUuid().equals(this.habitsOf)) {
            this.habitsOf = t.getUuid();
            this.glidingTicks = this.kitingTicks = this.huggingTicks = this.airTicks = 0;
         }

         UseAction use = t.isUsingItem() ? t.getActiveItem().getUseAction() : UseAction.NONE;
         this.aiming = use == UseAction.BOW || use == UseAction.CROSSBOW || use == UseAction.TRIDENT || use == UseAction.SPEAR;
         this.healing = use == UseAction.EAT || use == UseAction.DRINK;
         this.blocking = t.isBlocking();
         this.gliding = t.isGliding();
         this.pillaring = t.isOnGround() && pillaring(level, t);
         this.glidingTicks = this.gliding ? this.glidingTicks + 1 : 0;
         this.kitingTicks = t.distanceTo(dragon) > 24.0F ? this.kitingTicks + 1 : 0;
         this.huggingTicks = t.getEntityPos().distanceTo(dragon.head.getEntityPos()) <= 6.0 ? this.huggingTicks + 1 : 0;
         boolean high = !t.isOnGround() && heightAboveGround(level, t) > 3.0;
         this.airTicks = !high && !this.pillaring ? 0 : this.airTicks + 1;
         this.behind = DragonFx.angle(DragonFx.forward(dragon.getYaw()), t.getEntityPos().subtract(dragon.getEntityPos())) > 100.0;
      }
   }

   private static boolean usesFood(ServerPlayerEntity p) {
      if (!p.isUsingItem()) {
         return false;
      }

      UseAction use = p.getActiveItem().getUseAction();
      return use == UseAction.EAT || use == UseAction.DRINK;
   }

   static boolean pillaring(ServerWorld level, ServerPlayerEntity p) {
      for (Spike s : EndSpikeFeature.getSpikes(level)) {
         double dx = p.getX() - (s.getCenterX() + 0.5);
         double dz = p.getZ() - (s.getCenterZ() + 0.5);
         double r = s.getRadius() + 1.5;
         if (dx * dx + dz * dz <= r * r && p.getY() >= s.getHeight() - 1) {
            return true;
         }
      }

      int lowest = Integer.MAX_VALUE;

      for (int[] o : AROUND) {
         int h = level.getTopY(Type.MOTION_BLOCKING, p.getBlockX() + o[0], p.getBlockZ() + o[1]);
         if (h > level.getBottomY() + 1) {
            lowest = Math.min(lowest, h);
         }
      }

      return lowest != Integer.MAX_VALUE && p.getY() - lowest >= 5.0;
   }

   static double heightAboveGround(ServerWorld level, ServerPlayerEntity p) {
      Vec3d g = DragonFx.groundBelow(level, p.getEntityPos());
      return g == null ? 999.0 : p.getY() - g.y;
   }
}
