package com.vexira.mysticalterrain.mixin;

import net.minecraft.server.command.ForceLoadCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ForceLoadCommand.class)
public class ForceLoadCommandMixin {
   @ModifyConstant(method = "method_13372", constant = {@Constant(intValue = 30000000), @Constant(intValue = -30000000)})
   private static int mysticalterrain$bounds(int limit) {
      return limit < 0 ? -67000000 : 67000000;
   }
}
