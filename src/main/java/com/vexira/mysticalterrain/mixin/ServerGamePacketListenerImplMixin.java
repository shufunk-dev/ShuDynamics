package com.vexira.mysticalterrain.mixin;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerGamePacketListenerImplMixin {
   @ModifyConstant(method = "method_34882", constant = {@Constant(doubleValue = 3.0E7), @Constant(doubleValue = -3.0E7)})
   private static double mysticalterrain$moveBounds(double limit) {
      return Math.copySign(6.7E7, limit);
   }
}
