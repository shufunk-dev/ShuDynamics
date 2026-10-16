package com.vexira.mysticalterrain.mixin;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DimensionType.class)
public abstract class DimensionTypeMixin {
   @Shadow
   public abstract int comp_651();

   @Shadow
   public abstract int comp_652();

   @Redirect(method = "<clinit>", at = @At(value = "FIELD", target = "Lnet/minecraft/class_2338;field_10975:I"))
   private static int mysticalterrain$vanillaYBits() {
      return 12;
   }

   @Inject(method = "<init>", at = @At("TAIL"))
   private void mysticalterrain$checkHeight(CallbackInfo ci) {
      int limit = (1 << BlockPos.SIZE_BITS_Y - 1) - 16;
      if (this.comp_651() < -limit || this.comp_651() + this.comp_652() > limit) {
         throw new IllegalStateException("Mystical Terrain's bigger world only has room for dimensions between y " + -limit + " and " + (limit - 1));
      }
   }
}
