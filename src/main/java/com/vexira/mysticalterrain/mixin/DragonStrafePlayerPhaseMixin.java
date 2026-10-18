package com.vexira.mysticalterrain.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.vexira.mysticalterrain.boss.dragon.DragonPose;
import com.vexira.mysticalterrain.boss.dragon.DragonReworkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.AbstractDragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonStrafePlayerPhase;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DragonStrafePlayerPhase.class)
public abstract class DragonStrafePlayerPhaseMixin extends AbstractDragonPhaseInstance {
   @Shadow
   private @Nullable LivingEntity attackTarget;

   protected DragonStrafePlayerPhaseMixin(EnderDragon dragon) {
      super(dragon);
   }

   @WrapOperation(method = "doServerTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
   private boolean mysticalterrain$spit(ServerLevel level, Entity entity, Operation<Boolean> original) {
      ((DragonReworkHolder)this.dragon).mysticalterrain$rework().onFireball();
      if (entity instanceof DragonFireball orb) {
         Vec3 mouth = DragonPose.mouth(this.dragon, 1.0F);
         orb.snapTo(mouth.x, mouth.y, mouth.z, 0.0F, 0.0F);
         if (this.attackTarget != null) {
            Vec3 aim = this.attackTarget.position().add(0.0, this.attackTarget.getBbHeight() * 0.5, 0.0).subtract(mouth);
            if (aim.lengthSqr() > 1.0E-4) {
               orb.setDeltaMovement(aim.normalize().scale(orb.accelerationPower));
            }
         }
      }

      return (Boolean)original.call(new Object[]{level, entity});
   }
}
