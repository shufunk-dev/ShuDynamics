package com.vexira.mysticalterrain.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.vexira.mysticalterrain.boss.dragon.DragonFx;
import com.vexira.mysticalterrain.boss.dragon.DragonReworkHolder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.projectile.DragonFireballEntity;
import net.minecraft.entity.projectile.ExplosiveProjectileEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DragonFireballEntity.class)
public abstract class DragonFireballMixin extends ExplosiveProjectileEntity {
   protected DragonFireballMixin(EntityType<? extends ExplosiveProjectileEntity> type, World level) {
      super(type, level);
   }

   protected boolean canHit(Entity entity) {
      return entity instanceof EnderDragonPart part && part.owner == this.getOwner() ? false : super.canHit(entity);
   }

   @Inject(method = "getParticleType", at = @At("HEAD"), cancellable = true)
   private void mysticalterrain$monoTrail(CallbackInfoReturnable<ParticleEffect> cir) {
      cir.setReturnValue(DragonFx.orbTrail());
   }

   @ModifyArg(method = "onCollision", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/AreaEffectCloudEntity;setParticleType(Lnet/minecraft/particle/ParticleEffect;)V"), require = 0)
   private ParticleEffect mysticalterrain$monoPool(ParticleEffect original) {
      return DragonFx.POOL;
   }

   @ModifyArg(method = "onCollision", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/AreaEffectCloudEntity;setDuration(I)V"), require = 0)
   private int mysticalterrain$longerPools(int duration) {
      return this.getOwner() instanceof EnderDragonEntity dragon && ((DragonReworkHolder)dragon).mysticalterrain$rework().fightPhase() >= 2
         ? duration * 2
         : duration;
   }

   @WrapOperation(
      method = "onCollision",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;syncWorldEvent(ILnet/minecraft/util/math/BlockPos;I)V"),
      require = 0
   )
   private void mysticalterrain$monoBurst(World level, int event, BlockPos pos, int data, Operation<Void> original) {
      if (event == 2006 && level instanceof ServerWorld server) {
         DragonFx.orbBurst(server, Vec3d.ofBottomCenter(pos).add(0.0, 0.3, 0.0), data == 1);
      } else {
         original.call(new Object[]{level, event, pos, data});
      }
   }
}
