package com.vexira.mysticalterrain.mixin;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Entity.class)
public class EntityMixin {
   @ModifyConstant(method = "method_30634(DDD)V", constant = {@Constant(doubleValue = 3.0E7), @Constant(doubleValue = -3.0E7)})
   private double mysticalterrain$snapBounds(double limit) {
      return Math.copySign(6.7E7, limit);
   }

   @ModifyConstant(method = "method_5651", constant = {@Constant(doubleValue = 3.0000512E7), @Constant(doubleValue = -3.0000512E7)})
   private double mysticalterrain$loadBounds(double limit) {
      return Math.copySign(6.700051E7F, limit);
   }
}
