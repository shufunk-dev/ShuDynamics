package com.vexira.mysticalterrain.boss.ritual;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.AbstractBlock.Settings;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class FrostBellBlock extends Block {
   public static final MapCodec<FrostBellBlock> CODEC = createCodec(FrostBellBlock::new);
   public static final IntProperty NUMBER = IntProperty.of("number", 0, 3);
   private static final VoxelShape SHAPE = VoxelShapes.union(
      Block.createColumnShape(4.0, 13.0, 16.0), new VoxelShape[]{Block.createColumnShape(10.0, 3.0, 13.0), Block.createColumnShape(8.0, 0.0, 3.0)}
   );

   public FrostBellBlock(Settings properties) {
      super(properties);
      this.setDefaultState((BlockState)((BlockState)this.stateManager.getDefaultState()).with(NUMBER, 0));
   }

   protected MapCodec<? extends Block> getCodec() {
      return CODEC;
   }

   protected void appendProperties(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{NUMBER});
   }

   protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
      return SHAPE;
   }

   protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
      if (world instanceof ServerWorld server) {
         int n = (Integer)state.get(NUMBER);
         world.playSound(null, pos, SoundEvents.BLOCK_BELL_USE, SoundCategory.BLOCKS, 2.0F, 0.7F + n * 0.25F);
         server.spawnParticles(ParticleTypes.SNOWFLAKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 20, 0.4, 0.4, 0.4, 0.05);
         RitualAltarBlockEntity altar = RitualFixtures.findAltar(server, pos, Ritual.HRIMTHANE, 12);
         if (altar != null) {
            altar.ringBell(server, n);
         } else {
            player.sendMessage(Text.translatable("ritual.mysticalterrain.no_altar"), true);
         }
      }

      return ActionResult.SUCCESS;
   }
}
