package com.vexira.mysticalterrain.mixin;

import net.minecraft.world.border.WorldBorder.Properties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Properties.class)
public class WorldBorderSettingsMixin {
   @ModifyConstant(method = "<clinit>", constant = @Constant(doubleValue = 5.999997E7F))
   private static double mysticalterrain$defaultSize(double size) {
      return 1.3399997E8F;
   }

   @ModifyConstant(method = "method_74150", constant = {@Constant(doubleValue = 2.9999984E7), @Constant(doubleValue = -2.9999984E7)})
   private static double mysticalterrain$centerBounds(double limit) {
      return Math.copySign(6.6999984E7, limit);
   }
}
