package com.vexira.mysticalterrain.mixin;

import net.minecraft.world.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(WorldBorder.class)
public class WorldBorderMixin {
   @ModifyConstant(method = "<init>(Lnet/minecraft/class_2784$class_5200;)V", constant = @Constant(intValue = 29999984))
   private int mysticalterrain$absoluteMaxSize(int size) {
      return 66999984;
   }

   @ModifyConstant(method = "<init>(Lnet/minecraft/class_2784$class_5200;)V", constant = @Constant(doubleValue = 5.999997E7F))
   private double mysticalterrain$initialSize(double size) {
      return 1.3399997E8F;
   }

   @ModifyArg(method = "method_75885", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_2784;method_11969(D)V"))
   private double mysticalterrain$upgradeSavedSize(double size) {
      return size == 5.999997E7F ? 1.3399997E8F : size;
   }
}
