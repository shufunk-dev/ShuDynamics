package com.vexira.mysticalterrain.mixin;

import com.vexira.mysticalterrain.boss.dragon.DragonRitual;
import java.util.UUID;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.dimension.end.DragonRespawnStage;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderDragonFight.class)
public abstract class EnderDragonFightMixin {
   @Shadow
   private @Nullable UUID dragonUUID;
   @Shadow
   @Final
   private ServerBossEvent dragonEvent;

   @Inject(method = "createNewDragon", at = @At("HEAD"), cancellable = true)
   private void mysticalterrain$waitForRitual(CallbackInfoReturnable<EnderDragon> cir) {
      if (!DragonRitual.allowsSpawn()) {
         cir.setReturnValue(null);
      }
   }

   @Inject(method = "setRespawnStage", at = @At("HEAD"))
   private void mysticalterrain$crystalRespawnStart(DragonRespawnStage stage, CallbackInfo ci) {
      DragonRitual.setAllowSpawn(true);
   }

   @Inject(method = "setRespawnStage", at = @At("RETURN"))
   private void mysticalterrain$crystalRespawnEnd(DragonRespawnStage stage, CallbackInfo ci) {
      DragonRitual.setAllowSpawn(false);
   }

   @Inject(method = "tick", at = @At("TAIL"))
   private void mysticalterrain$hideEmptyBar(CallbackInfo ci) {
      if (this.dragonUUID == null) {
         this.dragonEvent.setVisible(false);
      }
   }
}
