package com.vexira.mysticalterrain.boss.ritual;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FrostBellBlock extends Block {
   public static final IntegerProperty NUMBER = IntegerProperty.create("number", 0, 3);
   private static final VoxelShape SHAPE = Shapes.or(
      Block.column(4.0, 13.0, 16.0), new VoxelShape[]{Block.column(10.0, 3.0, 13.0), Block.column(8.0, 0.0, 3.0)}
   );

   public FrostBellBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(NUMBER, 0));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{NUMBER});
   }

   protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
      if (world instanceof ServerLevel server) {
         RitualAltarBlockEntity altar = RitualFixtures.findAltar(server, pos, Ritual.HRIMTHANE, 12);
         int n = (Integer)state.getValue(NUMBER);

         // Auto-tune if uninitialized or if player is sneaking (crouching)
         if (altar != null && (n == 0 || player.isShiftKeyDown())) {
            if (player.isShiftKeyDown()) {
               n = (n + 1) % 4;
               world.setBlock(pos, state.setValue(NUMBER, n), 3);
               player.sendOverlayMessage(Component.literal("§bFrost Bell tuned to: §f" + n));
               world.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.5F, 0.7F + n * 0.25F);
               return InteractionResult.SUCCESS;
            } else {
               // Calculate orientation relative to altar center:
               // In LairBuilder: {0, -9}=0 (North), {9, 0}=1 (East), {0, 9}=2 (South), {-9, 0}=3 (West)
               BlockPos delta = pos.subtract(altar.getBlockPos());
               double angle = Math.atan2(delta.getX(), -delta.getZ());
               if (angle < 0) angle += Math.PI * 2.0;
               int resolved = (int)Math.round(angle / (Math.PI / 2.0)) % 4;
               if (resolved != n) {
                  n = resolved;
                  world.setBlock(pos, state.setValue(NUMBER, n), 3);
               }
            }
         }

         world.playSound(null, pos, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2.0F, 0.7F + n * 0.25F);
         server.sendParticles(ParticleTypes.SNOWFLAKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 20, 0.4, 0.4, 0.4, 0.05);
         if (altar != null) {
            altar.ringBell(server, n);
         } else {
            player.sendOverlayMessage(Component.translatable("ritual.mysticalterrain.no_altar"));
         }
      }

      return InteractionResult.SUCCESS;
   }
}
