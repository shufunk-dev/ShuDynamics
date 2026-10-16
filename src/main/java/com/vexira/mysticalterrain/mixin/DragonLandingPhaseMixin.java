package com.vexira.mysticalterrain.mixin;

import com.vexira.mysticalterrain.boss.dragon.DragonFx;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.phase.AbstractPhase;
import net.minecraft.entity.boss.dragon.phase.LandingPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LandingPhase.class)
public abstract class DragonLandingPhaseMixin extends AbstractPhase {
   protected DragonLandingPhaseMixin(EnderDragonEntity dragon) {
      super(dragon);
   }

   @Inject(method = "clientTick", at = @At("HEAD"), cancellable = true)
   private void mysticalterrain$monoBreath(CallbackInfo ci) {
      ci.cancel();
      DragonFx.breathTrail(this.dragon);
   }
}
