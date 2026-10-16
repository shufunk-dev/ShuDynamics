package com.vexira.mysticalterrain.boss.dragon;

import com.mojang.serialization.MapCodec;
import com.vexira.mysticalterrain.boss.BossSounds;
import com.vexira.mysticalterrain.boss.MysticalBosses;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.AbstractBlock.Settings;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class VoidPedestalBlock extends Block {
   public static final MapCodec<VoidPedestalBlock> CODEC = createCodec(VoidPedestalBlock::new);
   public static final BooleanProperty EYE = BooleanProperty.of("eye");
   private static final VoxelShape SHAPE = VoxelShapes.union(
      Block.createColumnShape(14.0, 0.0, 3.0), new VoxelShape[]{Block.createColumnShape(10.0, 3.0, 11.0), Block.createColumnShape(14.0, 11.0, 14.0)}
   );

   public VoidPedestalBlock(Settings properties) {
      super(properties);
      this.setDefaultState((BlockState)((BlockState)this.stateManager.getDefaultState()).with(EYE, false));
   }

   protected MapCodec<? extends Block> getCodec() {
      return CODEC;
   }

   protected void appendProperties(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{EYE});
   }

   protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
      return SHAPE;
   }

   protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
      if (stack.isOf(MysticalBosses.EYE_OF_THE_VOID) && !(Boolean)state.get(EYE)) {
         if (world instanceof ServerWorld server) {
            if (!DragonRitual.canBegin(server)) {
               player.sendMessage(Text.translatable("message.mysticalterrain.dragon_awake"), true);
               return ActionResult.FAIL;
            }

            world.setBlockState(pos, (BlockState)state.with(EYE, true), 3);
            stack.decrementUnlessCreative(1, player);
            world.playSound(null, pos, BossSounds.event("bolt.void.launch"), SoundCategory.BLOCKS, 1.0F, 0.6F);
            server.spawnParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 40, 0.2, 0.3, 0.2, 0.08);
            DragonRitual.onEyePlaced(server, player);
         }

         return ActionResult.SUCCESS;
      } else {
         return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
      }
   }

   public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
      if ((Boolean)state.get(EYE)) {
         for (int i = 0; i < 2; i++) {
            world.addParticleClient(
               ParticleTypes.WHITE_ASH,
               pos.getX() + 0.5,
               pos.getY() + 1.0,
               pos.getZ() + 0.5,
               (random.nextDouble() - 0.5) * 1.5,
               random.nextDouble() * 0.6,
               (random.nextDouble() - 0.5) * 1.5
            );
         }
      } else if (random.nextInt(4) == 0) {
         world.addParticleClient(
            ParticleTypes.WHITE_ASH,
            pos.getX() + 0.3 + random.nextDouble() * 0.4,
            pos.getY() + 0.9,
            pos.getZ() + 0.3 + random.nextDouble() * 0.4,
            0.0,
            0.02,
            0.0
         );
      }
   }
}
