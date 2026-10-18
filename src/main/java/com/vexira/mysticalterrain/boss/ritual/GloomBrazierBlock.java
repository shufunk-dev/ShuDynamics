package com.vexira.mysticalterrain.boss.ritual;

import com.mojang.serialization.MapCodec;
import com.vexira.mysticalterrain.boss.MysticalBosses;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

public class GloomBrazierBlock extends Block {
   public static final BooleanProperty LIT = BooleanProperty.create("lit");
   private static final VoxelShape SHAPE = Shapes.or(Block.column(6.0, 0.0, 10.0), Block.column(14.0, 10.0, 14.0));

   public GloomBrazierBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(LIT, false));
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{LIT});
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   @Override
   protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      boolean isLit = (Boolean)state.getValue(LIT);
      if (isLit) {
         if (!world.isClientSide()) {
            player.sendSystemMessage(Component.literal("§7[Ritual] This Gloom Brazier is already burning."));
         }
         return InteractionResult.SUCCESS;
      }

      boolean canLight = stack.is(Items.FLINT_AND_STEEL)
         || stack.is(Items.FIRE_CHARGE)
         || stack.is(Items.TORCH)
         || stack.is(Items.SOUL_TORCH)
         || stack.is(ItemTags.CANDLES);

      if (canLight) {
         if (!world.isClientSide() && world instanceof ServerLevel server) {
            world.setBlock(pos, (BlockState)state.setValue(LIT, true), 3);
            world.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 0.6F);
            world.playSound(null, pos, SoundEvents.SOUL_ESCAPE.value(), SoundSource.BLOCKS, 1.0F, 0.5F);
            if (stack.is(Items.FLINT_AND_STEEL)) {
               stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            } else if (stack.is(Items.FIRE_CHARGE)) {
               stack.consume(1, player);
            }

            RitualAltarBlockEntity altar = RitualFixtures.findAltar(server, pos, Ritual.HOLLOW_WARDEN, 32);
            int count = altar != null ? RitualFixtures.count(server, altar.getBlockPos(), MysticalBosses.GLOOM_BRAZIER, LIT) : 1;
            player.sendSystemMessage(Component.literal("§5[Ritual] §aGloom Brazier ignited! (" + count + "/6 lit)"));
            if (altar != null && count >= 6) {
               altar.begin(server);
            }
         }

         return InteractionResult.SUCCESS;
      }

      return InteractionResult.TRY_WITH_EMPTY_HAND;
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
      if (!world.isClientSide()) {
         if ((Boolean)state.getValue(LIT)) {
            player.sendSystemMessage(Component.literal("§7[Ritual] This Gloom Brazier is already burning."));
         } else {
            player.sendSystemMessage(Component.literal("§5[Ritual] §eIgnite this Gloom Brazier using flint & steel, a fire charge, a torch, or a candle."));
         }
      }
      return InteractionResult.SUCCESS;
   }

   @Override
   public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
      if ((Boolean)state.getValue(LIT)) {
         world.addParticle(
            ParticleTypes.SOUL_FIRE_FLAME,
            pos.getX() + 0.3 + random.nextDouble() * 0.4,
            pos.getY() + 0.95,
            pos.getZ() + 0.3 + random.nextDouble() * 0.4,
            0.0,
            0.03,
            0.0
         );
         if (random.nextInt(3) == 0) {
            world.addParticle(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 0.0, 0.04, 0.0);
         }
      }
   }
}
