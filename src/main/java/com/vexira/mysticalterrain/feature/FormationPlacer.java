package com.vexira.mysticalterrain.feature;

import com.vexira.mysticalterrain.formation.Formation;
import com.vexira.mysticalterrain.formation.Material;
import com.vexira.mysticalterrain.formation.Sink;
import com.vexira.mysticalterrain.tree.Noise;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.inventory.LootableInventory;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;

final class FormationPlacer implements Sink {
   private static final Direction.Axis[] AXES = new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Y, Direction.Axis.Z};
   private final StructureWorldAccess level;
   private final FormationConfiguration config;
   private final BlockPos base;
   private final long seed;
   private final Random random = Random.create();
   private final BlockPos.Mutable pos = new BlockPos.Mutable();

   private FormationPlacer(StructureWorldAccess level, FormationConfiguration config, BlockPos base, long seed) {
      this.level = level;
      this.config = config;
      this.base = base;
      this.seed = seed;
   }

   static void place(StructureWorldAccess level, FormationConfiguration config, Formation formation, BlockPos base, long seed, BlockBox box) {
      FormationPlacer placer = new FormationPlacer(level, config, base, seed);
      formation.rasterize(
         box.getMinX() - base.getX(),
         box.getMinY() - base.getY(),
         box.getMinZ() - base.getZ(),
         box.getMaxX() - base.getX(),
         box.getMaxY() - base.getY(),
         box.getMaxZ() - base.getZ(),
         placer
      );
      if (config.loot().isPresent()) {
         for (int[] chest : formation.chests()) {
            placer.pos.set(base.getX() + chest[0], base.getY() + chest[1], base.getZ() + chest[2]);
            if (box.contains(placer.pos)) {
               placer.chest();
            }
         }
      }
   }

   @Override
   public void place(int x, int y, int z, Material material, int axis, boolean force) {
      this.pos.set(this.base.getX() + x, this.base.getY() + y, this.base.getZ() + z);
      BlockState current = this.level.getBlockState(this.pos);
      if (!current.isIn(BlockTags.FEATURES_CANNOT_REPLACE)) {
         if (material == Material.AIR) {
            if (!current.isAir()) {
               this.level.setBlockState(this.pos, Blocks.AIR.getDefaultState(), 2);
            }
         } else if (force || isSoft(current)) {
            BlockStateProvider provider = this.config.material(material);
            if (provider != null) {
               BlockState state = this.pick(provider);
               if (!state.isAir()) {
                  if (state.contains(Properties.AXIS)) {
                     state = state.with(Properties.AXIS, AXES[axis]);
                  }

                  this.level.setBlockState(this.pos, state, 2);
               }
            }
         }
      }
   }

   private void chest() {
      Direction facing = Direction.fromHorizontalQuarterTurns(
         (int)(Noise.hash(this.seed + 5L, this.pos.getX(), this.pos.getY(), this.pos.getZ()) & 3L)
      );
      this.level.setBlockState(this.pos, Blocks.CHEST.getDefaultState().with(Properties.HORIZONTAL_FACING, facing), 2);
      this.random.setSeed(Noise.hash(this.seed + 6L, this.pos.getX(), this.pos.getY(), this.pos.getZ()));
      LootableInventory.setLootTable(this.level, this.random, this.pos, this.config.loot().get());
   }

   private BlockState pick(BlockStateProvider provider) {
      this.random.setSeed(Noise.hash(this.seed, this.pos.getX(), this.pos.getY(), this.pos.getZ()));
      return provider.get(this.random, this.pos);
   }

   private static boolean isSoft(BlockState state) {
      return state.isAir() || state.isReplaceable() || state.isIn(BlockTags.LEAVES);
   }
}
