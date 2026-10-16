package com.vexira.mysticalterrain.feature;

import com.vexira.mysticalterrain.building.Blueprint;
import com.vexira.mysticalterrain.building.Part;
import com.vexira.mysticalterrain.tree.Noise;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.block.enums.WallShape;
import net.minecraft.entity.EntityType;
import net.minecraft.inventory.LootableInventory;
import net.minecraft.loot.LootTable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;

final class BuildingPlacer implements Blueprint.Visitor {
   private static final BooleanProperty[] SIDES = new BooleanProperty[]{
      Properties.SOUTH, Properties.WEST, Properties.NORTH, Properties.EAST
   };
   private static final List<EnumProperty<WallShape>> WALL_SIDES = List.of(
      Properties.SOUTH_WALL_SHAPE, Properties.WEST_WALL_SHAPE, Properties.NORTH_WALL_SHAPE, Properties.EAST_WALL_SHAPE
   );
   private static final IntProperty NUMBER = com.vexira.mysticalterrain.boss.ritual.FrostBellBlock.NUMBER;
   private static final int[] DX = new int[]{0, -1, 0, 1};
   private static final int[] DZ = new int[]{1, 0, -1, 0};

   private final StructureWorldAccess level;
   private final BuildingConfiguration config;
   private final Blueprint plan;
   private final BlockPos base;
   private final long seed;
   private final BlockBox box;
   private final boolean flooded;
   private final Random random = Random.create();
   private final BlockPos.Mutable pos = new BlockPos.Mutable();

   private BuildingPlacer(StructureWorldAccess level, BuildingConfiguration config, Blueprint plan, BlockPos base, long seed, BlockBox box) {
      this.level = level;
      this.config = config;
      this.plan = plan;
      this.base = base;
      this.seed = seed;
      this.box = box;
      this.flooded = config.anchor() == BuildingConfiguration.Anchor.SEABED;
   }

   static void place(StructureWorldAccess level, BuildingConfiguration config, Blueprint plan, BlockPos base, long seed, BlockBox box) {
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
      if (this.box.contains(this.pos) && !this.level.isOutOfHeightLimit(this.pos)) {
         BlockState current = this.level.getBlockState(this.pos);
         if (!current.isIn(BlockTags.FEATURES_CANNOT_REPLACE)) {
            if (part == Part.AIR) {
               BlockState empty = this.flooded ? Blocks.WATER.getDefaultState() : Blocks.AIR.getDefaultState();
               if (!current.equals(empty)) {
                  this.level.setBlockState(this.pos, empty, 2);
               }
            } else if (part == Part.WATER) {
               this.level.setBlockState(this.pos, Blocks.WATER.getDefaultState(), 2);
            } else if (part == Part.LAVA) {
               this.level.setBlockState(this.pos, Blocks.LAVA.getDefaultState(), 2);
            } else if (part != Part.PLANT && part != Part.COBWEB || current.isAir() || current.isReplaceable()) {
               BlockStateProvider provider = this.config.material(part);
               if (provider == null && (part == Part.STAIRS || part == Part.ROOF_STAIRS)) {
                  provider = this.config.material(Part.ROOF);
               }

               if (provider != null) {
                  this.random.setSeed(Noise.hash(this.seed, this.pos.getX(), this.pos.getY(), this.pos.getZ()));
                  BlockState state = provider.get(this.random, this.pos);
                  if (!state.isAir()) {
                     this.level.setBlockState(this.pos, this.shape(state, x, y, z, part, facing, top), 2);
                  }
               }
            }
         }
      }
   }

   private BlockState shape(BlockState state, int x, int y, int z, Part part, int facing, boolean top) {
      Direction dir = Direction.fromHorizontalQuarterTurns(facing);
      if (state.contains(Properties.HORIZONTAL_FACING)) {
         state = state.with(Properties.HORIZONTAL_FACING, dir);
      }

      if (state.contains(Properties.BLOCK_HALF)) {
         state = state.with(Properties.BLOCK_HALF, top ? BlockHalf.TOP : BlockHalf.BOTTOM);
      }

      if (state.contains(Properties.SLAB_TYPE)) {
         state = state.with(Properties.SLAB_TYPE, top ? SlabType.TOP : SlabType.BOTTOM);
      }

      if (state.contains(Properties.WATERLOGGED)) {
         state = state.with(Properties.WATERLOGGED, this.flooded);
      }

      if (state.contains(Properties.HANGING)) {
         state = state.with(Properties.HANGING, part == Part.HANGING_LIGHT);
      }

      if (state.contains(Properties.LIT)) {
         state = state.with(Properties.LIT, part == Part.LIGHT || part == Part.HANGING_LIGHT);
      }

      if (state.contains(NUMBER)) {
         state = state.with(NUMBER, facing);
      }

      if (state.contains(Properties.PERSISTENT)) {
         state = state.with(Properties.PERSISTENT, true);
      }

      for (int i = 0; i < 4; i++) {
         Part next = this.plan.get(x + DX[i], y, z + DZ[i]);
         boolean join = next != null && next.solid();
         if (state.contains(SIDES[i])) {
            state = state.with(SIDES[i], join);
         }

         if (state.contains(WALL_SIDES.get(i))) {
            state = state.with(WALL_SIDES.get(i), join ? WallShape.TALL : WallShape.LOW);
         }
      }

      return state;
   }

   private void chest(int x, int y, int z, int facing, boolean treasure) {
      RegistryKey<LootTable> table = (treasure ? this.config.treasure() : this.config.loot()).or(this.config::loot).orElse(null);
      this.pos.set(this.base.getX() + x, this.base.getY() + y, this.base.getZ() + z);
      if (table != null && this.box.contains(this.pos)) {
         BlockState chest = Blocks.CHEST.getDefaultState()
            .with(Properties.HORIZONTAL_FACING, Direction.fromHorizontalQuarterTurns(facing))
            .with(Properties.WATERLOGGED, this.flooded);
         this.level.setBlockState(this.pos, chest, 2);
         this.random.setSeed(Noise.hash(this.seed + 6L, this.pos.getX(), this.pos.getY(), this.pos.getZ()));
         LootableInventory.setLootTable(this.level, this.random, this.pos, table);
      }
   }

   private void spawner(int x, int y, int z) {
      this.pos.set(this.base.getX() + x, this.base.getY() + y, this.base.getZ() + z);
      if (!this.config.spawners().isEmpty() && this.box.contains(this.pos)) {
         this.random.setSeed(Noise.hash(this.seed + 7L, this.pos.getX(), this.pos.getY(), this.pos.getZ()));
         EntityType<?> type = this.config.spawners().get(this.random.nextInt(this.config.spawners().size()));
         this.level.setBlockState(this.pos, Blocks.SPAWNER.getDefaultState(), 2);
         BlockEntity entity = this.level.getBlockEntity(this.pos);
         if (entity instanceof MobSpawnerBlockEntity spawner) {
            spawner.setEntityType(type, this.random);
         }
      }
   }
}
