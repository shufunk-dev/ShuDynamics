package com.vexira.mysticalterrain.mixin;

import net.minecraft.server.command.WorldBorderCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(WorldBorderCommand.class)
public class WorldBorderCommandMixin {
   @ModifyConstant(
      method = {"method_13858", "method_13854", "<clinit>"},
      constant = {@Constant(doubleValue = 5.999997E7F), @Constant(doubleValue = -5.999997E7F)}
   )
   private static double mysticalterrain$maxSize(double size) {
      return Math.copySign(1.3399997E8F, size);
   }

   @ModifyConstant(method = {"method_13869", "<clinit>"}, constant = @Constant(doubleValue = 2.9999984E7))
   private static double mysticalterrain$maxCenter(double center) {
      return 6.6999984E7;
   }
}
