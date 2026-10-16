package com.vexira.mysticalterrain.mixin;

import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ChunkPos.class)
public class ChunkPosMixin {
   @ModifyConstant(method = "<clinit>", constant = @Constant(intValue = 1875066))
   private static int mysticalterrain$invalidChunk(int chunk) {
      return 4194304;
   }
}
