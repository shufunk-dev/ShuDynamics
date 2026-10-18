package com.vexira.mysticalterrain.boss.dragon;

import com.mojang.serialization.MapCodec;
import com.vexira.mysticalterrain.boss.BossSounds;
import com.vexira.mysticalterrain.boss.MysticalBosses;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class VoidPedestalBlock extends Block {
   public static final BooleanProperty EYE = BooleanProperty.create("eye");
   private static final VoxelShape SHAPE = Shapes.or(
      Block.column(14.0, 0.0, 3.0), new VoxelShape[]{Block.column(10.0, 3.0, 11.0), Block.column(14.0, 11.0, 14.0)}
   );

   public VoidPedestalBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(EYE, false));
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{EYE});
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   @Override
   protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (stack.is(MysticalBosses.EYE_OF_THE_VOID) && !(Boolean)state.getValue(EYE)) {
         if (world instanceof ServerLevel server) {
            if (!DragonRitual.canBegin(server)) {
               player.sendSystemMessage(Component.literal("§c[Void Ritual] The Void Dragon is already awake!"));
               return InteractionResult.FAIL;
            }

            world.setBlock(pos, (BlockState)state.setValue(EYE, true), 3);
            stack.consume(1, player);
            SoundEvent sound = BossSounds.event("bolt.void.launch");
            if (sound == null) {
               sound = SoundEvents.END_PORTAL_FRAME_FILL;
            }
            world.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 0.6F);
            server.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 40, 0.2, 0.3, 0.2, 0.08);
            DragonRitual.onEyePlaced(server, player);
         }

         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.TRY_WITH_EMPTY_HAND;
      }
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
      if (!world.isClientSide()) {
         if ((Boolean)state.getValue(EYE)) {
            player.sendSystemMessage(Component.literal("§7[Void Ritual] An Eye of the Void is already set in this pedestal."));
         } else {
            player.sendSystemMessage(Component.literal("§5[Void Ritual] §dRight-click with an Eye of the Void to empower this pedestal."));
         }
      }
      return InteractionResult.SUCCESS;
   }

   @Override
   public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
      if ((Boolean)state.getValue(EYE)) {
         for (int i = 0; i < 2; i++) {
            world.addParticle(
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
         world.addParticle(
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
