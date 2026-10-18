package com.vexira.mysticalterrain.feature;

import com.vexira.mysticalterrain.building.Blueprint;
import com.vexira.mysticalterrain.building.Part;
import com.vexira.mysticalterrain.tree.Noise;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.LootTable;

final class BuildingPlacer implements Blueprint.Visitor {
   private static final BooleanProperty[] SIDES = new BooleanProperty[]{
      BlockStateProperties.SOUTH, BlockStateProperties.WEST, BlockStateProperties.NORTH, BlockStateProperties.EAST
   };
   private static final List<EnumProperty<WallSide>> WALL_SIDES = List.of(
      BlockStateProperties.SOUTH_WALL, BlockStateProperties.WEST_WALL, BlockStateProperties.NORTH_WALL, BlockStateProperties.EAST_WALL
   );
   private static final IntegerProperty NUMBER = com.vexira.mysticalterrain.boss.ritual.FrostBellBlock.NUMBER;
   private static final int[] DX = new int[]{0, -1, 0, 1};
   private static final int[] DZ = new int[]{1, 0, -1, 0};

   private final WorldGenLevel level;
   private final BuildingConfiguration config;
   private final Blueprint plan;
   private final BlockPos base;
   private final long seed;
   private final BoundingBox box;
   private final boolean flooded;
   private final RandomSource random = RandomSource.create();
   private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

   private BuildingPlacer(WorldGenLevel level, BuildingConfiguration config, Blueprint plan, BlockPos base, long seed, BoundingBox box) {
      this.level = level;
      this.config = config;
      this.plan = plan;
      this.base = base;
      this.seed = seed;
      this.box = box;
      this.flooded = config.anchor() == BuildingConfiguration.Anchor.SEABED;
   }

   static void place(WorldGenLevel level, BuildingConfiguration config, Blueprint plan, BlockPos base, long seed, BoundingBox box) {
      BuildingPlacer placer = new BuildingPlacer(level, config, plan, base, seed, box);
      plan.forEach(placer);

      for (int[] c : plan.chests()) {
         placer.chest(c[0], c[1], c[2], c[3], c[4] == 1);
      }

      for (int[] s : plan.spawners()) {
         placer.spawner(s[0], s[1], s[2]);
      }
   }

   @Override
   public void cell(int x, int y, int z, Part part, int facing, boolean top) {
      this.pos.set(this.base.getX() + x, this.base.getY() + y, this.base.getZ() + z);
      if (this.box.isInside(this.pos) && !this.level.isOutsideBuildHeight(this.pos)) {
         BlockState current = this.level.getBlockState(this.pos);
         if (!current.is(BlockTags.FEATURES_CANNOT_REPLACE)) {
            if (part == Part.AIR) {
               BlockState empty = this.flooded ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
               if (!current.equals(empty)) {
                  this.level.setBlock(this.pos, empty, 2);
               }
            } else if (part == Part.WATER) {
               this.level.setBlock(this.pos, Blocks.WATER.defaultBlockState(), 2);
            } else if (part == Part.LAVA) {
               this.level.setBlock(this.pos, Blocks.LAVA.defaultBlockState(), 2);
            } else if (part != Part.PLANT && part != Part.COBWEB || current.isAir() || current.canBeReplaced()) {
               BlockStateProvider provider = this.config.material(part);
               if (provider == null && (part == Part.STAIRS || part == Part.ROOF_STAIRS)) {
                  provider = this.config.material(Part.ROOF);
               }

               if (provider != null) {
                  this.random.setSeed(Noise.hash(this.seed, this.pos.getX(), this.pos.getY(), this.pos.getZ()));
                  BlockState state = provider.getState(this.level, this.random, this.pos);
                  if (!state.isAir()) {
                     this.level.setBlock(this.pos, this.shape(state, x, y, z, part, facing, top), 2);
                  }
               }
            }
         }
      }
   }

   private BlockState shape(BlockState state, int x, int y, int z, Part part, int facing, boolean top) {
      Direction dir = Direction.from2DDataValue(facing);
      if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
         state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, dir);
      }

      if (state.hasProperty(BlockStateProperties.HALF)) {
         state = state.setValue(BlockStateProperties.HALF, top ? Half.TOP : Half.BOTTOM);
      }

      if (state.hasProperty(BlockStateProperties.SLAB_TYPE)) {
         state = state.setValue(BlockStateProperties.SLAB_TYPE, top ? SlabType.TOP : SlabType.BOTTOM);
      }

      if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
         state = state.setValue(BlockStateProperties.WATERLOGGED, this.flooded);
      }

      if (state.hasProperty(BlockStateProperties.HANGING)) {
         state = state.setValue(BlockStateProperties.HANGING, part == Part.HANGING_LIGHT);
      }

      if (state.hasProperty(BlockStateProperties.LIT)) {
         state = state.setValue(BlockStateProperties.LIT, part == Part.LIGHT || part == Part.HANGING_LIGHT);
      }

      if (state.hasProperty(NUMBER)) {
         state = state.setValue(NUMBER, facing);
      }

      if (state.hasProperty(BlockStateProperties.PERSISTENT)) {
         state = state.setValue(BlockStateProperties.PERSISTENT, true);
      }

      for (int i = 0; i < 4; i++) {
         Part next = this.plan.get(x + DX[i], y, z + DZ[i]);
         boolean join = next != null && next.solid();
         if (state.hasProperty(SIDES[i])) {
            state = state.setValue(SIDES[i], join);
         }

         if (state.hasProperty(WALL_SIDES.get(i))) {
            state = state.setValue(WALL_SIDES.get(i), join ? WallSide.TALL : WallSide.LOW);
         }
      }

      return state;
   }

   private void chest(int x, int y, int z, int facing, boolean treasure) {
      ResourceKey<LootTable> table = (treasure ? this.config.treasure() : this.config.loot()).or(this.config::loot).orElse(null);
      this.pos.set(this.base.getX() + x, this.base.getY() + y, this.base.getZ() + z);
      if (table != null && this.box.isInside(this.pos)) {
         BlockState chest = Blocks.CHEST.defaultBlockState()
            .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.from2DDataValue(facing))
            .setValue(BlockStateProperties.WATERLOGGED, this.flooded);
         this.level.setBlock(this.pos, chest, 2);
         this.random.setSeed(Noise.hash(this.seed + 6L, this.pos.getX(), this.pos.getY(), this.pos.getZ()));
         RandomizableContainer.setBlockEntityLootTable(this.level, this.random, this.pos, table);
      }
   }

   private void spawner(int x, int y, int z) {
      this.pos.set(this.base.getX() + x, this.base.getY() + y, this.base.getZ() + z);
      if (!this.config.spawners().isEmpty() && this.box.isInside(this.pos)) {
         this.random.setSeed(Noise.hash(this.seed + 7L, this.pos.getX(), this.pos.getY(), this.pos.getZ()));
         EntityType<?> type = this.config.spawners().get(this.random.nextInt(this.config.spawners().size()));
         this.level.setBlock(this.pos, Blocks.SPAWNER.defaultBlockState(), 2);
         BlockEntity entity = this.level.getBlockEntity(this.pos);
         if (entity instanceof SpawnerBlockEntity spawner) {
            spawner.setEntityId(type, this.random);
         }
      }
   }
}
