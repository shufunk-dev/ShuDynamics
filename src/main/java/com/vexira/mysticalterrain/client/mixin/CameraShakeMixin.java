package com.vexira.mysticalterrain.client.mixin;

import com.vexira.mysticalterrain.client.vfx.VfxClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(Camera.class)
public abstract class CameraShakeMixin {
   @Shadow
   private float xRot;
   @Shadow
   private float yRot;
   @Shadow
   @Final
   private Quaternionf rotation;

   @Shadow
   protected abstract void setRotation(float yaw, float pitch);

   @Inject(method = "update", at = @At("TAIL"))
   private void mysticalterrain$shake(DeltaTracker deltaTracker, CallbackInfo ci) {
      float partial = deltaTracker.getGameTimeDeltaPartialTick(false);
      float[] o = VfxClient.cameraOffset(partial);
      if (o != null) {
         this.setRotation(this.yRot + o[0], this.xRot + o[1]);
         if (o.length > 2 && o[2] != 0.0F) {
            this.rotation.rotateZ(o[2] * (float) (Math.PI / 180.0));
         }
      }
   }
}