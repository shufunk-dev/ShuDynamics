package com.vexira.mysticalterrain.boss.ritual;

import com.vexira.mysticalterrain.boss.MysticalBosses;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ItemEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.Nullable;

public final class RitualFixtures {
   static final int REACH = 24;

   private RitualFixtures() {
   }

   public static int count(ServerWorld level, BlockPos altar, Block block, BooleanProperty on) {
      int n = 0;

      for (BlockPos p : BlockPos.iterate(altar.add(-24, -6, -24), altar.add(24, 10, 24))) {
         BlockState s = level.getBlockState(p);
         if (s.isOf(block) && (Boolean)s.get(on)) {
            n++;
         }
      }

      return n;
   }

   static void resetAll(ServerWorld level, BlockPos altar) {
      for (BlockPos p : BlockPos.iterate(altar.add(-24, -6, -24), altar.add(24, 10, 24))) {
         BlockState s = level.getBlockState(p);
         if (s.isOf(MysticalBosses.GLOOM_BRAZIER) && (Boolean)s.get(GloomBrazierBlock.LIT)) {
            level.setBlockState(p, (BlockState)s.with(GloomBrazierBlock.LIT, false), 3);
         } else if (s.isOf(MysticalBosses.RUNE_PILLAR) && (Boolean)s.get(RunePillarBlock.CHARGED)) {
            level.setBlockState(p, (BlockState)s.with(RunePillarBlock.CHARGED, false), 3);
         }
      }
   }

   public static @Nullable RitualAltarBlockEntity findAltar(ServerWorld level, BlockPos near, Ritual ritual, int reach) {
      for (BlockPos p : BlockPos.iterate(near.add(-reach, -reach / 2, -reach), near.add(reach, reach / 2, reach))) {
         if (level.getBlockState(p).getBlock() instanceof RitualAltarBlock altar
            && altar.ritual() == ritual
            && level.getBlockEntity(p) instanceof RitualAltarBlockEntity be) {
            return be;
         }
      }

      return null;
   }

   public static void itemTick(ItemEntity item) {
      if (item.getEntityWorld() instanceof ServerWorld level && item.age % 10 == 0) {
         Ritual ritual;
         if (item.getStack().isOf(MysticalBosses.CINDER_HEART) && item.isInLava()) {
            ritual = Ritual.PYRAXIS;
         } else {
            if (!item.getStack().isOf(MysticalBosses.DROWNED_IDOL) || !item.isTouchingWater()) {
               return;
            }

            ritual = Ritual.BOGROT;
         }

         RitualAltarBlockEntity altar = findAltar(level, item.getBlockPos(), ritual, 8);
         if (altar != null && altar.acceptThrown(level)) {
            item.getStack().decrement(1);
            if (item.getStack().isEmpty()) {
               item.discard();
            }
         }
      }
   }
}
