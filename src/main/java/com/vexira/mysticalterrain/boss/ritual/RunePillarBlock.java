package com.vexira.mysticalterrain.boss.ritual;

import com.mojang.serialization.MapCodec;
import com.vexira.mysticalterrain.boss.MysticalBosses;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;

public class RunePillarBlock extends Block {
   public static final BooleanProperty CHARGED = BooleanProperty.create("charged");

   public RunePillarBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(CHARGED, false));
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{CHARGED});
   }

   @Override
   protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      boolean isCharged = (Boolean)state.getValue(CHARGED);
      if (isCharged) {
         if (!world.isClientSide()) {
            player.sendSystemMessage(Component.literal("§7[Ritual] This Rune Pillar is already charged."));
         }
         return InteractionResult.SUCCESS;
      }

      if (stack.is(Items.LAPIS_LAZULI)) {
         if (!world.isClientSide() && world instanceof ServerLevel server) {
            world.setBlock(pos, (BlockState)state.setValue(CHARGED, true), 3);
            stack.consume(1, player);
            world.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.0F, 1.3F);

            RitualAltarBlockEntity altar = RitualFixtures.findAltar(server, pos, Ritual.ARCHON, 32);
            int count = altar != null ? RitualFixtures.count(server, altar.getBlockPos(), MysticalBosses.RUNE_PILLAR, CHARGED) : 1;
            player.sendSystemMessage(Component.literal("§b[Ritual] §aRune Pillar charged! (" + count + "/5 charged)"));
            if (altar != null && count >= 5) {
               altar.begin(server);
            }
         }

         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.TRY_WITH_EMPTY_HAND;
      }
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
      if (!world.isClientSide()) {
         if ((Boolean)state.getValue(CHARGED)) {
            player.sendSystemMessage(Component.literal("§7[Ritual] This Rune Pillar is already charged."));
         } else {
            player.sendSystemMessage(Component.literal("§b[Ritual] §eCharge this Rune Pillar by right-clicking with Lapis Lazuli."));
         }
      }
      return InteractionResult.SUCCESS;
   }

   @Override
   public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
      if ((Boolean)state.getValue(CHARGED)) {
         world.addParticle(
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
