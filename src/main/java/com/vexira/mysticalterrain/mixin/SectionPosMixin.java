package com.vexira.mysticalterrain.mixin;

import net.minecraft.util.math.ChunkSectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ChunkSectionPos.class)
public class SectionPosMixin {
   @Overwrite
   public static long method_18685(int x, int y, int z) {
      return (x & 8388607L) << 41 | (z & 8388607L) << 18 | y & 262143L;
   }

   @Overwrite
   public static int method_18686(long packed) {
      return (int)(packed >> 41);
   }

   @Overwrite
   public static int method_18689(long packed) {
      return (int)(packed << 46 >> 46);
   }

   @Overwrite
   public static int method_18690(long packed) {
      return (int)(packed << 23 >> 41);
   }

   @Overwrite
   public static long method_18693(long packed) {
      return packed & -262144L;
   }
}
