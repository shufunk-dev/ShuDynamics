package com.vexira.mysticalterrain.mixin;

import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(BlockPos.class)
public class BlockPosMixin {
   @ModifyConstant(method = "<clinit>", constant = @Constant(intValue = 30000000))
   private static int mysticalterrain$packWider(int limit) {
      return 67000000;
   }
}
