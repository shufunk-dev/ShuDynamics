package com.vexira.mysticalterrain.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.vexira.mysticalterrain.boss.dragon.DragonPose;
import com.vexira.mysticalterrain.boss.dragon.DragonReworkHolder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.phase.AbstractPhase;
import net.minecraft.entity.boss.dragon.phase.StrafePlayerPhase;
import net.minecraft.entity.projectile.DragonFireballEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(StrafePlayerPhase.class)
public abstract class DragonStrafePlayerPhaseMixin extends AbstractPhase {
   @Shadow
   private @Nullable LivingEntity target;

   protected DragonStrafePlayerPhaseMixin(EnderDragonEntity dragon) {
      super(dragon);
   }

   @WrapOperation(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;spawnEntity(Lnet/minecraft/entity/Entity;)Z"))
   private boolean mysticalterrain$spit(ServerWorld level, Entity entity, Operation<Boolean> original) {
      ((DragonReworkHolder)this.dragon).mysticalterrain$rework().onFireball();
      if (entity instanceof DragonFireballEntity orb) {
         Vec3d mouth = DragonPose.mouth(this.dragon, 1.0F);
         orb.refreshPositionAndAngles(mouth.x, mouth.y, mouth.z, 0.0F, 0.0F);
         if (this.target != null) {
            Vec3d aim = this.target.getEntityPos().add(0.0, this.target.getHeight() * 0.5, 0.0).subtract(mouth);
            if (aim.lengthSquared() > 1.0E-4) {
               orb.setVelocity(aim.normalize().multiply(orb.accelerationPower));
            }
         }
      }

      return (Boolean)original.call(new Object[]{level, entity});
   }
}
