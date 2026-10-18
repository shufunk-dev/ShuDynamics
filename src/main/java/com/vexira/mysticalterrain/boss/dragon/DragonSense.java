package com.vexira.mysticalterrain.boss.dragon;

import com.vexira.mysticalterrain.boss.ai.ThreatTable;
import com.vexira.mysticalterrain.boss.vfx.Vfx;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.EndSpikeFeature;
import net.minecraft.world.level.levelgen.feature.EndSpikeFeature.EndSpike;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

final class DragonSense {
   private static final double ARENA = 150.0;
   private static final int[][] AROUND = new int[][]{{3, 0}, {-3, 0}, {0, 3}, {0, -3}};
   final ThreatTable threat = new ThreatTable();
   final List<ServerPlayer> engaged = new ArrayList<>();
   @Nullable ServerPlayer target;
   @Nullable ServerPlayer marked;
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

   void tick(ServerLevel level, EnderDragon dragon, Vec3 portal, @Nullable ServerBossEvent bar) {
      this.engaged.clear();

      for (ServerPlayer p : bar != null ? bar.getPlayers() : level.players()) {
         if (p.isAlive() && !p.isSpectator() && !p.isCreative() && p.level() == level && DragonFx.flatDist(p.position(), portal) <= 150.0) {
            this.engaged.add(p);
         }
      }

      this.threat.tick(dragon, this.engaged);

      for (ServerPlayer p : this.engaged) {
         if (usesFood(p) && p.distanceToSqr(dragon) < 1600.0) {
            this.threat.add(p, 0.2F);
         }
      }

      this.recentDamage *= 0.98F;
      this.recentProjectile *= 0.98F;
      if (dragon.tickCount % 10 == 0 || this.target != null && !this.engaged.contains(this.target)) {
         this.target = this.threat.pick(this.engaged, this.target, dragon);
      }

      this.tickMarks(level, dragon);
      this.habits(level, dragon);
   }

   private void tickMarks(ServerLevel level, EnderDragon dragon) {
      if (this.markTicks > 0) {
         this.markTicks--;
         if (this.markTicks == 0 || this.marked == null || !this.engaged.contains(this.marked)) {
            this.marked = null;
            this.markTicks = 0;
         } else if (this.markTicks % 20 == 0) {
            Vfx.auraRing(level, this.marked.position(), DragonFx.V, 1.5);
         }
      }

      if (this.engaged.size() >= 2) {
         if (--this.nextMark <= 0) {
            this.nextMark = 500 + dragon.getRandom().nextInt(201);
            List<ServerPlayer> others = new ArrayList<>(this.engaged);
            others.remove(this.target);
            if (!others.isEmpty()) {
               this.marked = others.get(dragon.getRandom().nextInt(others.size()));
               this.markTicks = 140;
               Vfx.auraRing(level, this.marked.position(), DragonFx.V, 1.5);
               Vfx.flare(level, this.marked.position().add(0.0, this.marked.getBbHeight() + 0.8, 0.0), 16777215, 1.4, 30);
            }
         }
      }
   }

   void clearMarks() {
      this.marked = null;
      this.markTicks = 0;
   }

   @Nullable ServerPlayer target() {
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

   private void habits(ServerLevel level, EnderDragon dragon) {
      ServerPlayer t = this.target();
      if (t == null) {
         this.gliding = this.pillaring = this.aiming = this.healing = this.blocking = this.behind = false;
         this.glidingTicks = this.kitingTicks = this.huggingTicks = this.airTicks = 0;
         this.habitsOf = null;
      } else {
         if (!t.getUUID().equals(this.habitsOf)) {
            this.habitsOf = t.getUUID();
            this.glidingTicks = this.kitingTicks = this.huggingTicks = this.airTicks = 0;
         }

         ItemUseAnimation use = t.isUsingItem() ? t.getUseItem().getUseAnimation() : ItemUseAnimation.NONE;
         this.aiming = use == ItemUseAnimation.BOW || use == ItemUseAnimation.CROSSBOW || use == ItemUseAnimation.TRIDENT || use == ItemUseAnimation.SPEAR;
         this.healing = use == ItemUseAnimation.EAT || use == ItemUseAnimation.DRINK;
         this.blocking = t.isBlocking();
         this.gliding = t.isFallFlying();
         this.pillaring = t.onGround() && pillaring(level, t);
         this.glidingTicks = this.gliding ? this.glidingTicks + 1 : 0;
         this.kitingTicks = t.distanceTo(dragon) > 24.0F ? this.kitingTicks + 1 : 0;
         this.huggingTicks = t.position().distanceTo(dragon.head.position()) <= 6.0 ? this.huggingTicks + 1 : 0;
         boolean high = !t.onGround() && heightAboveGround(level, t) > 3.0;
         this.airTicks = !high && !this.pillaring ? 0 : this.airTicks + 1;
         this.behind = DragonFx.angle(DragonFx.forward(dragon.getYRot()), t.position().subtract(dragon.position())) > 100.0;
      }
   }

   private static boolean usesFood(ServerPlayer p) {
      if (!p.isUsingItem()) {
         return false;
      }

      ItemUseAnimation use = p.getUseItem().getUseAnimation();
      return use == ItemUseAnimation.EAT || use == ItemUseAnimation.DRINK;
   }

   static boolean pillaring(ServerLevel level, ServerPlayer p) {
      for (EndSpike s : EndSpikeFeature.getSpikesForLevel(level)) {
         double dx = p.getX() - (s.getCenterX() + 0.5);
         double dz = p.getZ() - (s.getCenterZ() + 0.5);
         double r = s.getRadius() + 1.5;
         if (dx * dx + dz * dz <= r * r && p.getY() >= s.getHeight() - 1) {
            return true;
         }
      }

      int lowest = Integer.MAX_VALUE;

      for (int[] o : AROUND) {
         int h = level.getHeight(Types.MOTION_BLOCKING, p.getBlockX() + o[0], p.getBlockZ() + o[1]);
         if (h > level.getMinY() + 1) {
            lowest = Math.min(lowest, h);
         }
      }

      return lowest != Integer.MAX_VALUE && p.getY() - lowest >= 5.0;
   }

   static double heightAboveGround(ServerLevel level, ServerPlayer p) {
      Vec3 g = DragonFx.groundBelow(level, p.position());
      return g == null ? 999.0 : p.getY() - g.y;
   }
}
