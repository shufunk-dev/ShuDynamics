package com.vexira.mysticalterrain.boss.ritual;

import com.mojang.serialization.MapCodec;
import com.vexira.mysticalterrain.boss.MysticalBosses;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
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
import net.minecraft.world.World;

public class RunePillarBlock extends Block {
   public static final MapCodec<RunePillarBlock> CODEC = createCodec(RunePillarBlock::new);
   public static final BooleanProperty CHARGED = BooleanProperty.of("charged");

   public RunePillarBlock(Settings properties) {
      super(properties);
      this.setDefaultState((BlockState)((BlockState)this.stateManager.getDefaultState()).with(CHARGED, false));
   }

   @Override
   protected MapCodec<? extends Block> getCodec() {
      return CODEC;
   }

   @Override
   protected void appendProperties(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{CHARGED});
   }

   @Override
   protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
      boolean isCharged = (Boolean)state.get(CHARGED);
      if (isCharged) {
         if (!world.isClient()) {
            player.sendMessage(Text.literal("§7[Ritual] This Rune Pillar is already charged."), false);
         }
         return ActionResult.SUCCESS;
      }

      if (stack.isOf(Items.LAPIS_LAZULI)) {
         if (!world.isClient() && world instanceof ServerWorld server) {
            world.setBlockState(pos, (BlockState)state.with(CHARGED, true), 3);
            stack.decrementUnlessCreative(1, player);
            world.playSound(null, pos, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.BLOCKS, 1.0F, 1.3F);

            RitualAltarBlockEntity altar = RitualFixtures.findAltar(server, pos, Ritual.ARCHON, 32);
            int count = altar != null ? RitualFixtures.count(server, altar.getPos(), MysticalBosses.RUNE_PILLAR, CHARGED) : 1;
            player.sendMessage(Text.literal("§b[Ritual] §aRune Pillar charged! (" + count + "/5 charged)"), false);
            if (altar != null && count >= 5) {
               altar.begin(server);
            }
         }

         return ActionResult.SUCCESS;
      } else {
         return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
      }
   }

   @Override
   protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
      if (!world.isClient()) {
         if ((Boolean)state.get(CHARGED)) {
            player.sendMessage(Text.literal("§7[Ritual] This Rune Pillar is already charged."), false);
         } else {
            player.sendMessage(Text.literal("§b[Ritual] §eCharge this Rune Pillar by right-clicking with Lapis Lazuli."), false);
         }
      }
      return ActionResult.SUCCESS;
   }

   @Override
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
