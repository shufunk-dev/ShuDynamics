package com.vexira.mysticalterrain.boss.ritual;

import com.vexira.mysticalterrain.boss.MysticalBosses;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jspecify.annotations.Nullable;

public final class RitualFixtures {
   static final int REACH = 24;

   private RitualFixtures() {
   }

   public static int count(ServerLevel level, BlockPos altar, Block block, BooleanProperty on) {
      int n = 0;

      for (BlockPos p : BlockPos.betweenClosed(altar.offset(-24, -6, -24), altar.offset(24, 10, 24))) {
         BlockState s = level.getBlockState(p);
         if (s.is(block) && (Boolean)s.getValue(on)) {
            n++;
         }
      }

      return n;
   }

   static void resetAll(ServerLevel level, BlockPos altar) {
      for (BlockPos p : BlockPos.betweenClosed(altar.offset(-24, -6, -24), altar.offset(24, 10, 24))) {
         BlockState s = level.getBlockState(p);
         if (s.is(MysticalBosses.GLOOM_BRAZIER) && (Boolean)s.getValue(GloomBrazierBlock.LIT)) {
            level.setBlock(p, (BlockState)s.setValue(GloomBrazierBlock.LIT, false), 3);
         } else if (s.is(MysticalBosses.RUNE_PILLAR) && (Boolean)s.getValue(RunePillarBlock.CHARGED)) {
            level.setBlock(p, (BlockState)s.setValue(RunePillarBlock.CHARGED, false), 3);
         }
      }
   }

   public static @Nullable RitualAltarBlockEntity findAltar(ServerLevel level, BlockPos near, Ritual ritual, int reach) {
      for (BlockPos p : BlockPos.betweenClosed(near.offset(-reach, -reach / 2, -reach), near.offset(reach, reach / 2, reach))) {
         if (level.getBlockState(p).getBlock() instanceof RitualAltarBlock altar
            && altar.ritual() == ritual
            && level.getBlockEntity(p) instanceof RitualAltarBlockEntity be) {
            return be;
         }
      }

      return null;
   }

   public static void itemTick(ItemEntity item) {
      if (item.level() instanceof ServerLevel level && item.tickCount % 10 == 0) {
         Ritual ritual;
         if (item.getItem().is(MysticalBosses.CINDER_HEART) && item.isInLava()) {
            ritual = Ritual.PYRAXIS;
         } else {
            if (!item.getItem().is(MysticalBosses.DROWNED_IDOL) || !item.isInWater()) {
               return;
            }

            ritual = Ritual.BOGROT;
         }

         RitualAltarBlockEntity altar = findAltar(level, item.blockPosition(), ritual, 8);
         if (altar != null && altar.acceptThrown(level)) {
            item.getItem().shrink(1);
            if (item.getItem().isEmpty()) {
               item.discard();
            }
         }
      }
   }
}
