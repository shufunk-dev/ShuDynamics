package com.vexira.mysticalterrain.boss.ai;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.consume.UseAction;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import org.jspecify.annotations.Nullable;

public final class BossContext {
   public @Nullable LivingEntity target;
   public double dist = Double.MAX_VALUE;
   public double hDist = Double.MAX_VALUE;
   public double heightDiff;
   public Vec3d targetVelocity = Vec3d.ZERO;
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
   public @Nullable Vec3d projectilePos;
   public @Nullable Vec3d projectileVelocity;
   public boolean targetSwingStarted;
   public int unreachableTicks;
   private final ArrayDeque<BossContext.Hurt> hurts = new ArrayDeque<>();
   private int lastHurtTick = -100000;
   private @Nullable UUID lastTarget;
   private @Nullable Vec3d lastPos;
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

   public void update(MobEntity boss, ServerWorld level, @Nullable LivingEntity target, List<ServerPlayerEntity> engaged, double preferred, boolean hovering) {
      int now = boss.age;

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
      double reach = 4.5 + boss.getWidth() * 0.5;

      for (ServerPlayerEntity p : engaged) {
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
         if (this.lastTarget == null || !this.lastTarget.equals(target.getUuid()) || this.lastPos == null) {
            this.lastTarget = target.getUuid();
            this.lastPos = target.getEntityPos();
            this.targetVelocity = Vec3d.ZERO;
            this.lastSwinging = target.handSwinging;
            this.lastSwingTime = target.handSwingTicks;
            this.kitingTicks = 0;
            this.unreachableTicks = 0;
            this.pathBad = false;
            this.ticksSinceHitTarget = 100000;
         }

         Vec3d pos = target.getEntityPos();
         Vec3d moved = pos.subtract(this.lastPos);
         if (moved.lengthSquared() > 4.0) {
            moved = Vec3d.ZERO;
         }

         this.targetVelocity = this.targetVelocity.multiply(0.5).add(moved.multiply(0.5));
         this.lastPos = pos;
         this.dist = Math.sqrt(boss.squaredDistanceTo(target));
         double dx = target.getX() - boss.getX();
         double dz = target.getZ() - boss.getZ();
         this.hDist = Math.sqrt(dx * dx + dz * dz);
         this.heightDiff = target.getY() - boss.getY();
         this.targetBlocking = target.isBlocking();
         UseAction use = target.isUsingItem() ? target.getActiveItem().getUseAction() : UseAction.NONE;
         this.targetAiming = use == UseAction.BOW || use == UseAction.CROSSBOW || use == UseAction.TRIDENT || use == UseAction.SPEAR;
         this.targetHealing = use == UseAction.EAT || use == UseAction.DRINK;
         this.targetFlying = target.isGliding();
         this.targetAirborne = !target.isOnGround() && !target.isTouchingWater() && heightAboveGround(level, target) > 2.5;
         this.kitingTicks = this.dist > Math.max(10.0, 2.5 * preferred) ? this.kitingTicks + 1 : 0;
         if (target.getAttacker() == boss) {
            this.ticksSinceHitTarget = target.age - target.getLastAttackedTime();
         } else if (this.ticksSinceHitTarget < 100000) {
            this.ticksSinceHitTarget++;
         }

         this.targetHealth = target.getHealth() / Math.max(1.0F, target.getMaxHealth());
         if (this.hDist > 0.01) {
            Vec3d facing = Vec3d.fromPolar(0.0F, boss.getYaw());
            double cos = (facing.x * dx + facing.z * dz) / this.hDist;
            this.targetBehind = Math.toDegrees(Math.acos(MathHelper.clamp(cos, -1.0, 1.0))) > 110.0;
         } else {
            this.targetBehind = false;
         }

         boolean swinging = target.handSwinging;
         int swingTime = target.handSwingTicks;
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
      this.targetVelocity = Vec3d.ZERO;
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

   private void scanProjectiles(MobEntity boss, ServerWorld level) {
      this.incomingProjectile = false;
      this.projectilePos = null;
      this.projectileVelocity = null;
      Box body = boss.getBoundingBox().expand(0.8);

      for (ProjectileEntity p : level.getEntitiesByClass(
         ProjectileEntity.class, boss.getBoundingBox().expand(8.0), e -> e.isAlive() && e.getOwner() instanceof PlayerEntity
      )) {
         Vec3d v = p.getVelocity();
         if (!(v.lengthSquared() < 0.04)) {
            Vec3d from = p.getEntityPos();
            if (!body.contains(from) && body.raycast(from, from.add(v.multiply(20.0))).isPresent()) {
               this.incomingProjectile = true;
               this.projectileId = p.getId();
               this.projectilePos = from;
               this.projectileVelocity = v;
               return;
            }
         }
      }
   }

   public static double heightAboveGround(ServerWorld level, LivingEntity e) {
      Mutable p = e.getBlockPos().mutableCopy();

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
