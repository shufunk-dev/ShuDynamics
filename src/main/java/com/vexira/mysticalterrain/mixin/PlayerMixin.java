package com.vexira.mysticalterrain.mixin;

import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(PlayerEntity.class)
public class PlayerMixin {
   @ModifyConstant(method = "method_5773", constant = {@Constant(doubleValue = 2.9999999E7), @Constant(doubleValue = -2.9999999E7)})
   private double mysticalterrain$clampBounds(double limit) {
      return Math.copySign(6.6999999E7, limit);
   }
}
