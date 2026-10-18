package com.vexira.mysticalterrain.boss.ai;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class BossContext {
   public @Nullable LivingEntity target;
   public double dist = Double.MAX_VALUE;
   public double hDist = Double.MAX_VALUE;
   public double heightDiff;
   public Vec3 targetVelocity = Vec3.ZERO;
   public boolean targetBlocking;
   public boolean targetAiming;
   public boolean targetFlying;
   public boolean targetAirborne;
   public boolean targetHealing;
   public boolean targetUnreachable;
   public int kitingTicks;
   public int ticksSinceHitTarget = 100000;
   public int ticksSinceHurt = 100000;
   public float recentDamage;
   public int engagedPlayers;
   public int surroundCount;
   public float targetHealth = 1.0F;
   public boolean targetBehind;
   public boolean incomingProjectile;
   public int projectileId = -1;
   public @Nullable Vec3 projectilePos;
   public @Nullable Vec3 projectileVelocity;
   public boolean targetSwingStarted;
   public int unreachableTicks;
   private final ArrayDeque<BossContext.Hurt> hurts = new ArrayDeque<>();
   private int lastHurtTick = -100000;
   private @Nullable UUID lastTarget;
   private @Nullable Vec3 lastPos;
   private boolean lastSwinging;
   private int lastSwingTime;
   private boolean pathBad;

   public void recordDamage(int tick, float amount) {
      this.hurts.addLast(new BossContext.Hurt(tick, amount));
      this.lastHurtTick = tick;
   }

   public void notePath(boolean reachable) {
      this.pathBad = !reachable;
   }

   public void reset() {
      this.hurts.clear();
      this.lastTarget = null;
      this.kitingTicks = 0;
      this.unreachableTicks = 0;
      this.targetUnreachable = false;
      this.pathBad = false;
      this.clearTarget();
   }

   public void update(Mob boss, ServerLevel level, @Nullable LivingEntity target, List<ServerPlayer> engaged, double preferred, boolean hovering) {
      int now = boss.tickCount;

      while (!this.hurts.isEmpty() && now - this.hurts.peekFirst().tick() > 100) {
         this.hurts.pollFirst();
      }

      float sum = 0.0F;

      for (BossContext.Hurt h : this.hurts) {
         sum += h.amount();
      }

      this.recentDamage = sum / Math.max(1.0F, boss.getMaxHealth());
      this.ticksSinceHurt = now - this.lastHurtTick;
      this.engagedPlayers = engaged.size();
      int around = 0;
      double reach = 4.5 + boss.getBbWidth() * 0.5;

      for (ServerPlayer p : engaged) {
         double dx = p.getX() - boss.getX();
         double dz = p.getZ() - boss.getZ();
         if (dx * dx + dz * dz <= reach * reach && Math.abs(p.getY() - boss.getY()) < 4.0) {
            around++;
         }
      }

      this.surroundCount = around;
      this.scanProjectiles(boss, level);
      this.target = target;
      if (target == null) {
         this.clearTarget();
      } else {
         if (this.lastTarget == null || !this.lastTarget.equals(target.getUUID()) || this.lastPos == null) {
            this.lastTarget = target.getUUID();
            this.lastPos = target.position();
            this.targetVelocity = Vec3.ZERO;
            this.lastSwinging = target.isSwinging();
            this.lastSwingTime = target.getCurrentSwing() != null ? target.getCurrentSwing().durationTicks() : 0;
            this.kitingTicks = 0;
            this.unreachableTicks = 0;
            this.pathBad = false;
            this.ticksSinceHitTarget = 100000;
         }

         Vec3 pos = target.position();
         Vec3 moved = pos.subtract(this.lastPos);
         if (moved.lengthSqr() > 4.0) {
            moved = Vec3.ZERO;
         }

         this.targetVelocity = this.targetVelocity.scale(0.5).add(moved.scale(0.5));
         this.lastPos = pos;
         this.dist = Math.sqrt(boss.distanceToSqr(target));
         double dx = target.getX() - boss.getX();
         double dz = target.getZ() - boss.getZ();
         this.hDist = Math.sqrt(dx * dx + dz * dz);
         this.heightDiff = target.getY() - boss.getY();
         this.targetBlocking = target.isBlocking();
         ItemUseAnimation use = target.isUsingItem() ? target.getUseItem().getUseAnimation() : ItemUseAnimation.NONE;
         this.targetAiming = use == ItemUseAnimation.BOW || use == ItemUseAnimation.CROSSBOW || use == ItemUseAnimation.TRIDENT || use == ItemUseAnimation.SPEAR;
         this.targetHealing = use == ItemUseAnimation.EAT || use == ItemUseAnimation.DRINK;
         this.targetFlying = target.isFallFlying();
         this.targetAirborne = !target.onGround() && !target.isInWater() && heightAboveGround(level, target) > 2.5;
         this.kitingTicks = this.dist > Math.max(10.0, 2.5 * preferred) ? this.kitingTicks + 1 : 0;
         if (target.getLastHurtByMob() == boss) {
            this.ticksSinceHitTarget = target.tickCount - target.getLastHurtByMobTimestamp();
         } else if (this.ticksSinceHitTarget < 100000) {
            this.ticksSinceHitTarget++;
         }

         this.targetHealth = target.getHealth() / Math.max(1.0F, target.getMaxHealth());
         if (this.hDist > 0.01) {
            Vec3 facing = Vec3.directionFromRotation(0.0F, boss.getYRot());
            double cos = (facing.x * dx + facing.z * dz) / this.hDist;
            this.targetBehind = Math.toDegrees(Math.acos(Mth.clamp(cos, -1.0, 1.0))) > 110.0;
         } else {
            this.targetBehind = false;
         }

         boolean swinging = target.isSwinging();
         int swingTime = target.getCurrentSwing() != null ? target.getCurrentSwing().durationTicks() : 0;
         this.targetSwingStarted = swinging && (!this.lastSwinging || swingTime < this.lastSwingTime);
         this.lastSwinging = swinging;
         this.lastSwingTime = swingTime;
         boolean raw;
         if (hovering) {
            raw = boss.horizontalCollision && this.hDist > preferred + 4.0;
         } else {
            raw = this.pathBad && this.dist > 3.5 || this.heightDiff > 4.0 && this.targetVelocity.y > -0.05;
         }

         this.unreachableTicks = raw ? this.unreachableTicks + 1 : 0;
         this.targetUnreachable = this.unreachableTicks > 40;
      }
   }

   private void clearTarget() {
      this.target = null;
      this.dist = Double.MAX_VALUE;
      this.hDist = Double.MAX_VALUE;
      this.heightDiff = 0.0;
      this.targetVelocity = Vec3.ZERO;
      this.targetBlocking = false;
      this.targetAiming = false;
      this.targetFlying = false;
      this.targetAirborne = false;
      this.targetHealing = false;
      this.targetUnreachable = false;
      this.targetBehind = false;
      this.targetSwingStarted = false;
      this.targetHealth = 1.0F;
      this.kitingTicks = 0;
      this.unreachableTicks = 0;
   }

   private void scanProjectiles(Mob boss, ServerLevel level) {
      this.incomingProjectile = false;
      this.projectilePos = null;
      this.projectileVelocity = null;
      AABB body = boss.getBoundingBox().inflate(0.8);

      for (Projectile p : level.getEntitiesOfClass(
         Projectile.class, boss.getBoundingBox().inflate(8.0), e -> e.isAlive() && e.getOwner() instanceof Player
      )) {
         Vec3 v = p.getDeltaMovement();
         if (!(v.lengthSqr() < 0.04)) {
            Vec3 from = p.position();
            if (!body.contains(from) && body.clip(from, from.add(v.scale(20.0))).isPresent()) {
               this.incomingProjectile = true;
               this.projectileId = p.getId();
               this.projectilePos = from;
               this.projectileVelocity = v;
               return;
            }
         }
      }
   }

   public static double heightAboveGround(ServerLevel level, LivingEntity e) {
      MutableBlockPos p = e.blockPosition().mutable();

      for (int i = 0; i < 8; i++) {
         BlockState s = level.getBlockState(p);
         if (!s.getCollisionShape(level, p).isEmpty() || !s.getFluidState().isEmpty()) {
            return e.getY() - (p.getY() + 1);
         }

         p.move(0, -1, 0);
      }

      return 8.0;
   }

   public List<String> describe() {
      List<String> out = new ArrayList<>();
      out.add(
         String.format(
            "dist=%.1f h=%.1f dy=%.1f vel=%.2f,%.2f,%.2f kiting=%d sinceHit=%d sinceHurt=%d recentDmg=%.3f",
            this.dist == Double.MAX_VALUE ? -1.0 : this.dist,
            this.hDist == Double.MAX_VALUE ? -1.0 : this.hDist,
            this.heightDiff,
            this.targetVelocity.x,
            this.targetVelocity.y,
            this.targetVelocity.z,
            this.kitingTicks,
            this.ticksSinceHitTarget,
            this.ticksSinceHurt,
            this.recentDamage
         )
      );
      out.add(
         String.format(
            "blocking=%s aiming=%s flying=%s airborne=%s healing=%s unreachable=%s(%d) behind=%s swing=%s projectile=%s",
            this.targetBlocking,
            this.targetAiming,
            this.targetFlying,
            this.targetAirborne,
            this.targetHealing,
            this.targetUnreachable,
            this.unreachableTicks,
            this.targetBehind,
            this.targetSwingStarted,
            this.incomingProjectile
         )
      );
      out.add(String.format("engaged=%d surround=%d targetHp=%.2f", this.engagedPlayers, this.surroundCount, this.targetHealth));
      return out;
   }

   private record Hurt(int tick, float amount) {
   }
}
