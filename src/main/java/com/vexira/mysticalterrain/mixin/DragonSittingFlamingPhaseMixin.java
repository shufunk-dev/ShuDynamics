package com.vexira.mysticalterrain.mixin;

import com.vexira.mysticalterrain.boss.dragon.DragonFx;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.phase.AbstractSittingPhase;
import net.minecraft.entity.boss.dragon.phase.SittingFlamingPhase;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SittingFlamingPhase.class)
public abstract class DragonSittingFlamingPhaseMixin extends AbstractSittingPhase {
   @Shadow
   private int ticks;

   protected DragonSittingFlamingPhaseMixin(EnderDragonEntity dragon) {
      super(dragon);
   }

   @ModifyArg(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/AreaEffectCloudEntity;setParticleType(Lnet/minecraft/particle/ParticleEffect;)V"), require = 0)
   private ParticleEffect mysticalterrain$monoFlame(ParticleEffect original) {
      return DragonFx.POOL;
   }

   @Inject(method = "clientTick", at = @At("HEAD"), cancellable = true)
   private void mysticalterrain$monoBreath(CallbackInfo ci) {
      ci.cancel();
      this.ticks++;
      if (this.ticks % 2 == 0 && this.ticks < 10) {
         DragonFx.breathBurst(this.dragon, this.ticks == 2);
      } else if (this.ticks > 10 && this.ticks < 200 && this.ticks % 3 == 0) {
         DragonFx.breathStream(this.dragon);
      }
   }
}
