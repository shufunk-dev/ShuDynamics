package com.vexira.mysticalterrain.mixin;

import com.vexira.mysticalterrain.boss.dragon.DragonRitual;
import java.util.UUID;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.entity.boss.dragon.EnderDragonSpawnState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderDragonFight.class)
public abstract class EndDragonFightMixin {
   @Shadow
   private @Nullable UUID field_13116;
   @Shadow
   @Final
   private ServerBossBar field_13119;

   @Inject(method = "method_12523", at = @At("HEAD"), cancellable = true)
   private void mysticalterrain$waitForRitual(CallbackInfoReturnable<EnderDragonEntity> cir) {
      if (!DragonRitual.allowsSpawn()) {
         cir.setReturnValue(null);
      }
   }

   @Inject(method = "method_12521", at = @At("HEAD"))
   private void mysticalterrain$crystalRespawnStart(EnderDragonSpawnState stage, CallbackInfo ci) {
      DragonRitual.setAllowSpawn(true);
   }

   @Inject(method = "method_12521", at = @At("RETURN"))
   private void mysticalterrain$crystalRespawnEnd(EnderDragonSpawnState stage, CallbackInfo ci) {
      DragonRitual.setAllowSpawn(false);
   }

   @Inject(method = "method_12538", at = @At("TAIL"))
   private void mysticalterrain$hideEmptyBar(CallbackInfo ci) {
      if (this.field_13116 == null) {
         this.field_13119.setVisible(false);
      }
   }
}
