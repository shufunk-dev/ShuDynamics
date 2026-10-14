package com.vexira.mysticalterrain.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

public class ColossalTreeFeature extends Feature<ColossalTreeConfiguration> {
   public ColossalTreeFeature(Codec<ColossalTreeConfiguration> codec) {
      super(codec);
   }

   @Override
   public boolean generate(FeatureContext<ColossalTreeConfiguration> context) {
      StructureWorldAccess level = context.getWorld();
      Random random = context.getRandom();
      ColossalTreeConfiguration config = context.getConfig();
      BlockPos origin = context.getOrigin();

      if (!level.getBlockState(origin.down()).isIn(BlockTags.DIRT)) {
         return false;
      }

      int height = config.height().get(random);
      int radius = config.trunkRadius().get(random);
      int crown = config.crownRadius().get(random);

      if (origin.getY() + height + crown > level.getTopYInclusive() - 1) {
         return false;
      }

      for (int y = 1; y <= Math.min(height, 10); y++) {
         if (!isFree(level, origin.up(y))) {
            return false;
         }
      }

      this.placeTrunk(level, random, config, origin, height, radius);
      int branches = config.branchCount().get(random);
      double angle = random.nextDouble() * Math.PI * 2.0;

      for (int i = 0; i < branches; i++) {
         angle += (Math.PI * 2) / Math.max(1, branches) + (random.nextDouble() - 0.5) * 0.9;
         int startY = MathHelper.floor(height * (0.45 + 0.4 * random.nextDouble()));
         int leafRadius = 2 + random.nextInt(2);
         int length = Math.min(crown - random.nextInt(3), 14 - leafRadius - 1);
         this.placeBranch(level, random, config, origin.up(startY), angle, Math.max(radius + 2, length), leafRadius);
      }

      int topRadius = Math.max(3, crown / 2 + 1);
      this.placeFoliage(level, random, config, origin.up(height), topRadius, Math.max(2, topRadius * 2 / 3));
      this.placeFoliage(level, random, config, origin.up(height - topRadius / 2), topRadius + 1, 2);
      return true;
   }

   private void placeTrunk(StructureWorldAccess level, Random random, ColossalTreeConfiguration config, BlockPos origin, int height, int radius) {
      BlockPos.Mutable pos = new BlockPos.Mutable();

      for (int y = -3; y < height; y++) {
         double taper = 1.0 - 0.45 * Math.max(0, y) / height;
         double flare = y < 4 ? (4 - y) * 0.55 : 0.0;
         double r = (radius + 0.5) * taper + flare;
         int reach = MathHelper.ceil(r);

         for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
               if (!(dx * dx + dz * dz > r * r)) {
                  pos.set(origin, dx, y, dz);
                  BlockState existing = level.getBlockState(pos);
                  if (y < 0 ? isSoil(existing) || isFree(level, pos) : isFree(level, pos)) {
                     this.setBlockState(level, pos, log(config, random, pos, Direction.Axis.Y));
                  }
               }
            }
         }
      }
   }

   private void placeBranch(StructureWorldAccess level, Random random, ColossalTreeConfiguration config, BlockPos start, double angle, int length, int leafRadius) {
      double dx = Math.cos(angle);
      double dz = Math.sin(angle);
      double rise = 0.35 + random.nextDouble() * 0.3;
      Direction.Axis axis = Math.abs(dx) > Math.abs(dz) ? Direction.Axis.X : Direction.Axis.Z;
      BlockPos.Mutable pos = new BlockPos.Mutable();
      BlockPos end = start;

      for (int step = 0; step <= length; step++) {
         pos.set(start, (int)Math.round(dx * step), (int)Math.round(rise * step), (int)Math.round(dz * step));
         if (isFree(level, pos)) {
            this.setBlockState(level, pos, log(config, random, pos, axis));
         }
         end = pos.toImmutable();
      }

      this.placeFoliage(level, random, config, end, leafRadius + 1, leafRadius);
   }

   private void placeFoliage(StructureWorldAccess level, Random random, ColossalTreeConfiguration config, BlockPos center, int horizontal, int vertical) {
      BlockPos.Mutable pos = new BlockPos.Mutable();

      for (int dx = -horizontal; dx <= horizontal; dx++) {
         for (int dy = -vertical; dy <= vertical; dy++) {
            for (int dz = -horizontal; dz <= horizontal; dz++) {
               double d = (double)(dx * dx + dz * dz) / (horizontal * horizontal) + (double)(dy * dy) / (vertical * vertical);
               if (!(d > 1.0) && (!(d > 0.7) || !(random.nextFloat() < 0.35))) {
                  pos.set(center, dx, dy, dz);
                  if (level.getBlockState(pos).isAir() || level.getBlockState(pos).isIn(BlockTags.REPLACEABLE_BY_TREES)) {
                     BlockState leaves = config.foliageProvider().get(random, pos);
                     if (leaves.contains(LeavesBlock.PERSISTENT)) {
                        leaves = leaves.with(LeavesBlock.PERSISTENT, true);
                     }
                     this.setBlockState(level, pos, leaves);
                  }
               }
            }
         }
      }
   }

   private static BlockState log(ColossalTreeConfiguration config, Random random, BlockPos pos, Direction.Axis axis) {
      BlockState state = config.trunkProvider().get(random, pos);
      return state.contains(PillarBlock.AXIS) ? state.with(PillarBlock.AXIS, axis) : state;
   }

   private static boolean isFree(StructureWorldAccess level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      return state.isAir() || state.isIn(BlockTags.LEAVES) || state.isIn(BlockTags.REPLACEABLE_BY_TREES);
   }

   public static boolean isSoil(BlockState state) {
      return state.isIn(BlockTags.DIRT) || state.isIn(BlockTags.BASE_STONE_OVERWORLD);
   }
}
