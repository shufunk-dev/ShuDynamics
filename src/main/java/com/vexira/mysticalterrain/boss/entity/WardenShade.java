package com.vexira.mysticalterrain.boss.entity;

import com.vexira.mysticalterrain.boss.BossFx;
import com.vexira.mysticalterrain.boss.vfx.Vfx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;

public class WardenShade extends Monster implements GeoEntity {
   private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.hollow_warden.idle");
   private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.hollow_warden.walk");
   private static final RawAnimation FLURRY = RawAnimation.begin().thenPlay("animation.hollow_warden.flurry");
   private static final RawAnimation FORM_1 = RawAnimation.begin().thenLoop("animation.hollow_warden.form_1");
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private int attackTicks;

   public WardenShade(EntityType<? extends Monster> type, Level level) {
      super(type, level);
      this.setNoGravity(true);
      this.xpReward = 0;
   }

   public static Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 30.0)
         .add(Attributes.ATTACK_DAMAGE, 6.0)
         .add(Attributes.MOVEMENT_SPEED, 0.3)
         .add(Attributes.FOLLOW_RANGE, 40.0);
   }

   protected void customServerAiStep(ServerLevel world) {
      super.customServerAiStep(world);
      if (this.tickCount % 5 == 0) {
         Vfx.smoke(world, this.position().add(0.0, 1.0, 0.0), Vfx.Palette.SHADOW.smoke(), 1, 0.6, 1.8);
      }

      if (this.tickCount > 400) {
         Vfx.flare(world, this.position().add(0.0, 2.0, 0.0), Vfx.Palette.SHADOW.glow(), 3.0, 8);
         world.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 2.0, this.getZ(), 30, 0.5, 1.5, 0.5, 0.02);
         this.discard();
      } else {
         LivingEntity target = this.getTarget();
         if (target == null || !target.isAlive()) {
            target = world.getNearestPlayer(this, 32.0);
            if (target instanceof Player p && (p.isCreative() || p.isSpectator())) {
               target = null;
            }

            this.setTarget(target);
         }

         if (target != null) {
            Vec3 to = target.position().add(0.0, 0.5, 0.0).subtract(this.position());
            if (to.length() > 2.5) {
               this.setDeltaMovement(this.getDeltaMovement().scale(0.7).add(to.normalize().scale(0.09)));
            }

            float yaw = (float)Math.toDegrees(Math.atan2(-to.x, to.z));
            this.setYRot(yaw);
            this.setYBodyRot(yaw);
            if (this.attackTicks > 0) {
               this.attackTicks--;
               if (this.attackTicks == 22) {
                  BossFx.current(null);
                  Vfx.swing(world, this.position(), this.getYRot(), Vfx.Palette.SHADOW, 3.0);
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
      controllers.add(new AnimationController("main", 5, test -> test.setAndContinue(this.getDeltaMovement().horizontalDistanceSqr() > 0.002 ? WALK : IDLE)));
      controllers.add(new AnimationController("action", 2, test -> PlayState.STOP).triggerableAnim("flurry", FLURRY));
      controllers.add(new AnimationController("form", 0, test -> test.setAndContinue(FORM_1)));
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}
