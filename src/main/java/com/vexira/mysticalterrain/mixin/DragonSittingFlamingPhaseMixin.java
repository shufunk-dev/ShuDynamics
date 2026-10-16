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
   private int field_7053;

   protected DragonSittingFlamingPhaseMixin(EnderDragonEntity dragon) {
      super(dragon);
   }

   @ModifyArg(method = "method_6855", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_1295;method_5608(Lnet/minecraft/class_2394;)V"), require = 0)
   private ParticleEffect mysticalterrain$monoFlame(ParticleEffect original) {
      return DragonFx.POOL;
   }

   @Inject(method = "method_6853", at = @At("HEAD"), cancellable = true)
   private void mysticalterrain$monoBreath(CallbackInfo ci) {
      ci.cancel();
      this.field_7053++;
      if (this.field_7053 % 2 == 0 && this.field_7053 < 10) {
         DragonFx.breathBurst(this.dragon, this.field_7053 == 2);
      } else if (this.field_7053 > 10 && this.field_7053 < 200 && this.field_7053 % 3 == 0) {
         DragonFx.breathStream(this.dragon);
      }
   }
}
