package com.vexira.mysticalterrain.boss.ritual;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.AbstractBlock.Settings;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class RunePillarBlock extends Block {
   public static final MapCodec<RunePillarBlock> CODEC = createCodec(RunePillarBlock::new);
   public static final BooleanProperty CHARGED = BooleanProperty.of("charged");

   public RunePillarBlock(Settings properties) {
      super(properties);
      this.setDefaultState((BlockState)((BlockState)this.stateManager.getDefaultState()).with(CHARGED, false));
   }

   protected MapCodec<? extends Block> getCodec() {
      return CODEC;
   }

   protected void appendProperties(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{CHARGED});
   }

   protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
      if (stack.isOf(Items.LAPIS_LAZULI) && !(Boolean)state.get(CHARGED)) {
         if (!world.isClient()) {
            world.setBlockState(pos, (BlockState)state.with(CHARGED, true), 3);
            stack.decrementUnlessCreative(1, player);
            world.playSound(null, pos, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.BLOCKS, 1.0F, 1.3F);
         }

         return ActionResult.SUCCESS;
      } else {
         return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
      }
   }

   public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
      if ((Boolean)state.get(CHARGED)) {
         world.addParticleClient(
            ParticleTypes.ENCHANT,
            pos.getX() + 0.5,
            pos.getY() + 1.6,
            pos.getZ() + 0.5,
            (random.nextDouble() - 0.5) * 2.0,
            -1.2,
            (random.nextDouble() - 0.5) * 2.0
         );
      }
   }
}
