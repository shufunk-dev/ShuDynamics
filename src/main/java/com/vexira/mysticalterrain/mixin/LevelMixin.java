package com.vexira.mysticalterrain.mixin;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(World.class)
public abstract class LevelMixin implements WorldView {
   @Shadow
   private static boolean method_8558(BlockPos pos) {
      throw new AssertionError();
   }

   @ModifyConstant(method = "method_8558", constant = {@Constant(intValue = 30000000), @Constant(intValue = -30000000)})
   private static int mysticalterrain$bounds(int limit) {
      return limit < 0 ? -67000000 : 67000000;
   }

   @ModifyConstant(
      method = "method_8624(Lnet/minecraft/class_2902$class_2903;II)I",
      constant = {@Constant(intValue = 30000000), @Constant(intValue = -30000000)}
   )
   private int mysticalterrain$heightBounds(int limit) {
      return limit < 0 ? -67000000 : 67000000;
   }

   public int getLightLevel(BlockPos pos, int ambientDarkness) {
      return method_8558(pos) ? this.getBaseLightLevel(pos, ambientDarkness) : 15;
   }
}
