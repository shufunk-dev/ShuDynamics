package com.vexira.mysticalterrain.client.mixin;

import com.vexira.mysticalterrain.client.vfx.VfxClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
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
   private float field_18717;
   @Shadow
   private float field_18718;
   @Shadow
   @Final
   private Quaternionf field_21518;

   @Shadow
   protected abstract void method_19325(float var1, float var2);

   @Inject(method = "method_19321", at = @At("TAIL"))
   private void mysticalterrain$shake(World level, Entity entity, boolean detached, boolean mirrored, float partial, CallbackInfo ci) {
      float[] o = VfxClient.cameraOffset(partial);
      if (o != null) {
         this.method_19325(this.field_18718 + o[0], this.field_18717 + o[1]);
         if (o.length > 2 && o[2] != 0.0F) {
            this.field_21518.rotateZ(o[2] * (float) (Math.PI / 180.0));
         }
      }
   }
}
