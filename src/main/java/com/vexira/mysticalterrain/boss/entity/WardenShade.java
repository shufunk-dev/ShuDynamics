package com.vexira.mysticalterrain.boss.entity;

import com.vexira.mysticalterrain.boss.BossFx;
import com.vexira.mysticalterrain.boss.vfx.Vfx;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.DefaultAttributeContainer.Builder;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class WardenShade extends HostileEntity implements GeoEntity {
   private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.hollow_warden.idle");
   private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.hollow_warden.walk");
   private static final RawAnimation FLURRY = RawAnimation.begin().thenPlay("animation.hollow_warden.flurry");
   private static final RawAnimation FORM_1 = RawAnimation.begin().thenLoop("animation.hollow_warden.form_1");
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private int attackTicks;

   public WardenShade(EntityType<? extends HostileEntity> type, World level) {
      super(type, level);
      this.setNoGravity(true);
      this.experiencePoints = 0;
   }

   public static Builder createAttributes() {
      return HostileEntity.createHostileAttributes()
         .add(EntityAttributes.MAX_HEALTH, 30.0)
         .add(EntityAttributes.ATTACK_DAMAGE, 6.0)
         .add(EntityAttributes.MOVEMENT_SPEED, 0.3)
         .add(EntityAttributes.FOLLOW_RANGE, 40.0);
   }

   protected void mobTick(ServerWorld world) {
      super.mobTick(world);
      if (this.age % 5 == 0) {
         Vfx.smoke(world, this.getEntityPos().add(0.0, 1.0, 0.0), Vfx.Palette.SHADOW.smoke(), 1, 0.6, 1.8);
      }

      if (this.age > 400) {
         Vfx.flare(world, this.getEntityPos().add(0.0, 2.0, 0.0), Vfx.Palette.SHADOW.glow(), 3.0, 8);
         world.spawnParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 2.0, this.getZ(), 30, 0.5, 1.5, 0.5, 0.02);
         this.discard();
      } else {
         LivingEntity target = this.getTarget();
         if (target == null || !target.isAlive()) {
            target = world.getClosestPlayer(this, 32.0);
            if (target instanceof PlayerEntity p && (p.isCreative() || p.isSpectator())) {
               target = null;
            }

            this.setTarget(target);
         }

         if (target != null) {
            Vec3d to = target.getEntityPos().add(0.0, 0.5, 0.0).subtract(this.getEntityPos());
            if (to.length() > 2.5) {
               this.setVelocity(this.getVelocity().multiply(0.7).add(to.normalize().multiply(0.09)));
            }

            float yaw = (float)Math.toDegrees(Math.atan2(-to.x, to.z));
            this.setYaw(yaw);
            this.setBodyYaw(yaw);
            if (this.attackTicks > 0) {
               this.attackTicks--;
               if (this.attackTicks == 22) {
                  BossFx.current(null);
                  Vfx.swing(world, this.getEntityPos(), this.getYaw(), Vfx.Palette.SHADOW, 3.0);
                  BossFx.melee(world, this, 4.0, 80.0F, 6.0F, 0.4);
               }
            } else if (to.length() < 3.5) {
               this.attackTicks = 32;
               this.triggerAnim("action", "flurry");
            }
         }
      }
   }

   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController("main", 5, test -> test.setAndContinue(this.getVelocity().horizontalLengthSquared() > 0.002 ? WALK : IDLE)));
      controllers.add(new AnimationController("action", 2, test -> PlayState.STOP).triggerableAnim("flurry", FLURRY));
      controllers.add(new AnimationController("form", 0, test -> test.setAndContinue(FORM_1)));
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}
