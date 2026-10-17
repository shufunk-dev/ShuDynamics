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
import net.minecraft.registry.tag.ItemTags;
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
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class GloomBrazierBlock extends Block {
   public static final MapCodec<GloomBrazierBlock> CODEC = createCodec(GloomBrazierBlock::new);
   public static final BooleanProperty LIT = BooleanProperty.of("lit");
   private static final VoxelShape SHAPE = VoxelShapes.union(Block.createColumnShape(6.0, 0.0, 10.0), Block.createColumnShape(14.0, 10.0, 14.0));

   public GloomBrazierBlock(Settings properties) {
      super(properties);
      this.setDefaultState((BlockState)((BlockState)this.stateManager.getDefaultState()).with(LIT, false));
   }

   @Override
   protected MapCodec<? extends Block> getCodec() {
      return CODEC;
   }

   @Override
   protected void appendProperties(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{LIT});
   }

   @Override
   protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
      return SHAPE;
   }

   @Override
   protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
      boolean isLit = (Boolean)state.get(LIT);
      if (isLit) {
         if (!world.isClient()) {
            player.sendMessage(Text.literal("§7[Ritual] This Gloom Brazier is already burning."), false);
         }
         return ActionResult.SUCCESS;
      }

      boolean canLight = stack.isOf(Items.FLINT_AND_STEEL)
         || stack.isOf(Items.FIRE_CHARGE)
         || stack.isOf(Items.TORCH)
         || stack.isOf(Items.SOUL_TORCH)
         || stack.isIn(ItemTags.CANDLES);

      if (canLight) {
         if (!world.isClient() && world instanceof ServerWorld server) {
            world.setBlockState(pos, (BlockState)state.with(LIT, true), 3);
            world.playSound(null, pos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1.0F, 0.6F);
            world.playSound(null, pos, SoundEvents.PARTICLE_SOUL_ESCAPE.value(), SoundCategory.BLOCKS, 1.0F, 0.5F);
            if (stack.isOf(Items.FLINT_AND_STEEL)) {
               stack.damage(1, player, hand.getEquipmentSlot());
            } else if (stack.isOf(Items.FIRE_CHARGE)) {
               stack.decrementUnlessCreative(1, player);
            }

            RitualAltarBlockEntity altar = RitualFixtures.findAltar(server, pos, Ritual.HOLLOW_WARDEN, 32);
            int count = altar != null ? RitualFixtures.count(server, altar.getPos(), MysticalBosses.GLOOM_BRAZIER, LIT) : 1;
            player.sendMessage(Text.literal("§5[Ritual] §aGloom Brazier ignited! (" + count + "/6 lit)"), false);
            if (altar != null && count >= 6) {
               altar.begin(server);
            }
         }

         return ActionResult.SUCCESS;
      }

      return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
   }

   @Override
   protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
      if (!world.isClient()) {
         if ((Boolean)state.get(LIT)) {
            player.sendMessage(Text.literal("§7[Ritual] This Gloom Brazier is already burning."), false);
         } else {
            player.sendMessage(Text.literal("§5[Ritual] §eIgnite this Gloom Brazier using flint & steel, a fire charge, a torch, or a candle."), false);
         }
      }
      return ActionResult.SUCCESS;
   }

   @Override
   public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
      if ((Boolean)state.get(LIT)) {
         world.addParticleClient(
            ParticleTypes.SOUL_FIRE_FLAME,
            pos.getX() + 0.3 + random.nextDouble() * 0.4,
            pos.getY() + 0.95,
            pos.getZ() + 0.3 + random.nextDouble() * 0.4,
            0.0,
            0.03,
            0.0
         );
         if (random.nextInt(3) == 0) {
            world.addParticleClient(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 0.0, 0.04, 0.0);
         }
      }
   }
}
