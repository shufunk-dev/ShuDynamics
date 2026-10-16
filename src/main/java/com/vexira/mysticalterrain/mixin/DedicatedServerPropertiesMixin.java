package com.vexira.mysticalterrain.mixin;

import net.minecraft.server.dedicated.ServerPropertiesHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ServerPropertiesHandler.class)
public class DedicatedServerPropertiesMixin {
   @ModifyConstant(method = "<init>", constant = @Constant(intValue = 29999984))
   private int mysticalterrain$defaultWorldSize(int size) {
      return 66999984;
   }

   @ModifyConstant(method = "method_16715", constant = @Constant(intValue = 29999984))
   private static int mysticalterrain$maxWorldSize(int size) {
      return 66999984;
   }

   @ModifyArg(method = "method_16715", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_3532;method_15340(III)I"), index = 0)
   private static int mysticalterrain$upgradeVanillaDefault(int size) {
      return size == 29999984 ? 66999984 : size;
   }
}
