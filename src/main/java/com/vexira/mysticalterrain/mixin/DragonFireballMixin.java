package com.vexira.mysticalterrain.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.vexira.mysticalterrain.boss.dragon.DragonFx;
import com.vexira.mysticalterrain.boss.dragon.DragonReworkHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DragonFireball.class)
public abstract class DragonFireballMixin extends AbstractHurtingProjectile {
   protected DragonFireballMixin(EntityType<? extends AbstractHurtingProjectile> type, Level level) {
      super(type, level);
   }

   protected boolean canHitEntity(Entity entity) {
      return entity instanceof EnderDragonPart part && part.parentMob == this.getOwner() ? false : super.canHitEntity(entity);
   }

   @Inject(method = "getTrailParticle", at = @At("HEAD"), cancellable = true)
   private void mysticalterrain$monoTrail(CallbackInfoReturnable<ParticleOptions> cir) {
      cir.setReturnValue(DragonFx.orbTrail());
   }

   @ModifyArg(method = "onHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/AreaEffectCloud;setCustomParticle(Lnet/minecraft/core/particles/ParticleOptions;)V"), require = 0)
   private ParticleOptions mysticalterrain$monoPool(ParticleOptions original) {
      return DragonFx.POOL;
   }

   @ModifyArg(method = "onHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/AreaEffectCloud;setDuration(I)V"), require = 0)
   private int mysticalterrain$longerPools(int duration) {
      return this.getOwner() instanceof EnderDragon dragon && ((DragonReworkHolder)dragon).mysticalterrain$rework().fightPhase() >= 2
         ? duration * 2
         : duration;
   }

   @WrapOperation(
      method = "onHit",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;levelEvent(ILnet/minecraft/core/BlockPos;I)V"),
      require = 0
   )
   private void mysticalterrain$monoBurst(Level level, int event, BlockPos pos, int data, Operation<Void> original) {
      if (event == 2006 && level instanceof ServerLevel server) {
         DragonFx.orbBurst(server, Vec3.atBottomCenterOf(pos).add(0.0, 0.3, 0.0), data == 1);
      } else {
         original.call(new Object[]{level, event, pos, data});
      }
   }
}
