package com.vexira.mysticalterrain.boss.dragon;

import com.mojang.serialization.MapCodec;
import com.vexira.mysticalterrain.boss.BossSounds;
import com.vexira.mysticalterrain.boss.MysticalBosses;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
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

   @Override
   protected MapCodec<? extends Block> getCodec() {
      return CODEC;
   }

   @Override
   protected void appendProperties(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{EYE});
   }

   @Override
   protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
      return SHAPE;
   }

   @Override
   protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
      if (stack.isOf(MysticalBosses.EYE_OF_THE_VOID) && !(Boolean)state.get(EYE)) {
         if (world instanceof ServerWorld server) {
            if (!DragonRitual.canBegin(server)) {
               player.sendMessage(Text.literal("§c[Void Ritual] The Void Dragon is already awake!"), false);
               return ActionResult.FAIL;
            }

            world.setBlockState(pos, (BlockState)state.with(EYE, true), 3);
            stack.decrementUnlessCreative(1, player);
            SoundEvent sound = BossSounds.event("bolt.void.launch");
            if (sound == null) {
               sound = SoundEvents.BLOCK_END_PORTAL_FRAME_FILL;
            }
            world.playSound(null, pos, sound, SoundCategory.BLOCKS, 1.0F, 0.6F);
            server.spawnParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 40, 0.2, 0.3, 0.2, 0.08);
            DragonRitual.onEyePlaced(server, player);
         }

         return ActionResult.SUCCESS;
      } else {
         return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
      }
   }

   @Override
   protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
      if (!world.isClient()) {
         if ((Boolean)state.get(EYE)) {
            player.sendMessage(Text.literal("§7[Void Ritual] An Eye of the Void is already set in this pedestal."), false);
         } else {
            player.sendMessage(Text.literal("§5[Void Ritual] §dRight-click with an Eye of the Void to empower this pedestal."), false);
         }
      }
      return ActionResult.SUCCESS;
   }

   @Override
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
