package com.vexira.mysticalterrain.mixin;

import com.vexira.mysticalterrain.boss.dragon.DragonFx;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.AbstractDragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonLandingPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DragonLandingPhase.class)
public abstract class DragonLandingPhaseMixin extends AbstractDragonPhaseInstance {
   protected DragonLandingPhaseMixin(EnderDragon dragon) {
      super(dragon);
   }

   @Inject(method = "doClientTick", at = @At("HEAD"), cancellable = true)
   private void mysticalterrain$monoBreath(CallbackInfo ci) {
      ci.cancel();
      DragonFx.breathTrail(this.dragon);
   }
}
