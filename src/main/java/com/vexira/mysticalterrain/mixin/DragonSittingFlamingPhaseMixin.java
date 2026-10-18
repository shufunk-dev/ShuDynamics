package com.vexira.mysticalterrain.mixin;

import com.vexira.mysticalterrain.boss.dragon.DragonFx;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.AbstractDragonSittingPhase;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonSittingFlamingPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DragonSittingFlamingPhase.class)
public abstract class DragonSittingFlamingPhaseMixin extends AbstractDragonSittingPhase {
   @Shadow
   private int flameTicks;

   protected DragonSittingFlamingPhaseMixin(EnderDragon dragon) {
      super(dragon);
   }

   @ModifyArg(method = "doServerTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/AreaEffectCloud;setCustomParticle(Lnet/minecraft/core/particles/ParticleOptions;)V"), require = 0)
   private ParticleOptions mysticalterrain$monoFlame(ParticleOptions original) {
      return DragonFx.POOL;
   }

   @Inject(method = "doClientTick", at = @At("HEAD"), cancellable = true)
   private void mysticalterrain$monoBreath(CallbackInfo ci) {
      ci.cancel();
      this.flameTicks++;
      if (this.flameTicks % 2 == 0 && this.flameTicks < 10) {
         DragonFx.breathBurst(this.dragon, this.flameTicks == 2);
      } else if (this.flameTicks > 10 && this.flameTicks < 200 && this.flameTicks % 3 == 0) {
         DragonFx.breathStream(this.dragon);
      }
   }
}
