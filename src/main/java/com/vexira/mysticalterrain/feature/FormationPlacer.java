package com.vexira.mysticalterrain.feature;

import com.vexira.mysticalterrain.formation.Formation;
import com.vexira.mysticalterrain.formation.Material;
import com.vexira.mysticalterrain.formation.Sink;
import com.vexira.mysticalterrain.tree.Noise;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

final class FormationPlacer implements Sink {
   private static final Direction.Axis[] AXES = new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Y, Direction.Axis.Z};
   private final WorldGenLevel level;
   private final FormationConfiguration config;
   private final BlockPos base;
   private final long seed;
   private final RandomSource random = RandomSource.create();
   private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

   private FormationPlacer(WorldGenLevel level, FormationConfiguration config, BlockPos base, long seed) {
      this.level = level;
      this.config = config;
      this.base = base;
      this.seed = seed;
   }

   static void place(WorldGenLevel level, FormationConfiguration config, Formation formation, BlockPos base, long seed, BoundingBox box) {
      FormationPlacer placer = new FormationPlacer(level, config, base, seed);
      formation.rasterize(
         box.minX() - base.getX(),
         box.minY() - base.getY(),
         box.minZ() - base.getZ(),
         box.maxX() - base.getX(),
         box.maxY() - base.getY(),
         box.maxZ() - base.getZ(),
         placer
      );
      if (config.loot().isPresent()) {
         for (int[] chest : formation.chests()) {
            placer.pos.set(base.getX() + chest[0], base.getY() + chest[1], base.getZ() + chest[2]);
            if (box.isInside(placer.pos)) {
               placer.chest();
            }
         }
      }
   }

   @Override
   public void place(int x, int y, int z, Material material, int axis, boolean force) {
      this.pos.set(this.base.getX() + x, this.base.getY() + y, this.base.getZ() + z);
      BlockState current = this.level.getBlockState(this.pos);
      if (!current.is(BlockTags.FEATURES_CANNOT_REPLACE)) {
         if (material == Material.AIR) {
            if (!current.isAir()) {
               this.level.setBlock(this.pos, Blocks.AIR.defaultBlockState(), 2);
            }
         } else if (force || isSoft(current)) {
            BlockStateProvider provider = this.config.material(material);
            if (provider != null) {
               BlockState state = this.pick(provider);
               if (!state.isAir()) {
                  if (state.hasProperty(BlockStateProperties.AXIS)) {
                     state = state.setValue(BlockStateProperties.AXIS, AXES[axis]);
                  }

                  this.level.setBlock(this.pos, state, 2);
               }
            }
         }
      }
   }

   private void chest() {
      Direction facing = Direction.from2DDataValue(
         (int)(Noise.hash(this.seed + 5L, this.pos.getX(), this.pos.getY(), this.pos.getZ()) & 3L)
      );
      this.level.setBlock(this.pos, Blocks.CHEST.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing), 2);
      this.random.setSeed(Noise.hash(this.seed + 6L, this.pos.getX(), this.pos.getY(), this.pos.getZ()));
      RandomizableContainer.setBlockEntityLootTable(this.level, this.random, this.pos, this.config.loot().get());
   }

   private BlockState pick(BlockStateProvider provider) {
      this.random.setSeed(Noise.hash(this.seed, this.pos.getX(), this.pos.getY(), this.pos.getZ()));
      return provider.getState(this.level, this.random, this.pos);
   }

   private static boolean isSoft(BlockState state) {
      return state.isAir() || state.canBeReplaced() || state.is(BlockTags.LEAVES);
   }
}
