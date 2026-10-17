package com.vexira.mysticalterrain.boss.ritual;

import com.mojang.serialization.MapCodec;
import com.vexira.mysticalterrain.block.MysticalBlocks;
import com.vexira.mysticalterrain.boss.MysticalBosses;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.AbstractBlock.Settings;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.IntProperty;
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
import org.jspecify.annotations.Nullable;

public class RitualAltarBlock extends BlockWithEntity {
   public static final IntProperty STAGE = IntProperty.of("stage", 0, 6);
   private static final VoxelShape SHAPE = VoxelShapes.union(
      Block.createColumnShape(16.0, 0.0, 4.0), new VoxelShape[]{Block.createColumnShape(12.0, 4.0, 12.0), Block.createColumnShape(16.0, 12.0, 15.0)}
   );
   private final Ritual ritual;
   private final MapCodec<RitualAltarBlock> codec;

   public RitualAltarBlock(Ritual ritual, Settings properties) {
      super(properties);
      this.ritual = ritual;
      this.codec = createCodec(p -> new RitualAltarBlock(ritual, p));
      this.setDefaultState((BlockState)((BlockState)this.stateManager.getDefaultState()).with(STAGE, 0));
   }

   public Ritual ritual() {
      return this.ritual;
   }

   protected MapCodec<? extends BlockWithEntity> getCodec() {
      return this.codec;
   }

   protected void appendProperties(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{STAGE});
   }

   protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
      return SHAPE;
   }

   protected BlockRenderType getRenderType(BlockState state) {
      return BlockRenderType.MODEL;
   }

   public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
      return new RitualAltarBlockEntity(pos, state);
   }

   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
      return world.isClient() ? null : validateTicker(type, MysticalBosses.ALTAR_ENTITY, RitualAltarBlockEntity::serverTick);
   }

   private @Nullable Item offering() {
      return switch (this.ritual) {
         case LIRAEL -> MysticalBlocks.MOONPETAL.asItem();
         case GROTHMAW -> MysticalBosses.PRIMAL_EGG;
         case MYCELORD -> MysticalBosses.SPORE_HEART;
         case ASTRAEL -> MysticalBosses.STARLIGHT_VIAL;
         default -> null;
      };
   }

   protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
      if (stack.isEmpty()) {
         return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
      }

      Item required = this.offering();
      int jarIndex = MysticalBosses.jarIndex(stack);
      boolean fits = (required != null && stack.isOf(required)) || (this.ritual == Ritual.SEKHMAR && jarIndex >= 0);
      if (!fits) {
         return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
      }

      if (world.getBlockEntity(pos) instanceof RitualAltarBlockEntity altar) {
         if (world instanceof ServerWorld server) {
            Text refusal = altar.offer(server, player, stack, jarIndex);
            if (refusal != null) {
               player.sendMessage(refusal, true);
               return ActionResult.FAIL;
            }

            stack.decrementUnlessCreative(1, player);
            world.playSound(null, pos, SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.BLOCKS, 1.0F, 0.8F);
         }

         return ActionResult.SUCCESS;
      } else {
         return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
      }
   }

   protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
      if (!world.isClient() && world instanceof ServerWorld server) {
         if (this.ritual == Ritual.HOLLOW_WARDEN) {
            if (world.getBlockEntity(pos) instanceof RitualAltarBlockEntity altar) {
               int lit = RitualFixtures.count(server, pos, MysticalBosses.GLOOM_BRAZIER, GloomBrazierBlock.LIT);
               if (lit >= 6) {
                  player.sendMessage(Text.literal("§5[Ritual] §dAll 6 Gloom Braziers burn! The Hollow Warden stirs..."), false);
                  altar.begin(server);
                  return ActionResult.SUCCESS;
               } else {
                  player.sendMessage(Text.literal("§5[Ritual] §c" + lit + "/6 Gloom Braziers lit. Light all six braziers to awaken the Hollow Warden!"), false);
                  return ActionResult.SUCCESS;
               }
            }
         } else if (this.ritual == Ritual.ARCHON) {
            if (world.getBlockEntity(pos) instanceof RitualAltarBlockEntity altar) {
               int charged = RitualFixtures.count(server, pos, MysticalBosses.RUNE_PILLAR, RunePillarBlock.CHARGED);
               if (charged >= 5) {
                  player.sendMessage(Text.literal("§b[Ritual] §dAll 5 Rune Pillars are charged! Archon stirs..."), false);
                  altar.begin(server);
                  return ActionResult.SUCCESS;
               } else {
                  player.sendMessage(Text.literal("§b[Ritual] §c" + charged + "/5 Rune Pillars charged. Charge all five pillars with Lapis to awaken Archon!"), false);
                  return ActionResult.SUCCESS;
               }
            }
         }

         player.sendMessage(Text.translatable("ritual.mysticalterrain." + this.ritual.id() + ".hint"), false);
      }

      return ActionResult.SUCCESS;
   }

   public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
      int stage = (Integer)state.get(STAGE);
      int count = 1 + stage * 2;

      for (int i = 0; i < count; i++) {
         if (random.nextInt(3) == 0) {
            world.addParticleClient(
               this.ritual.particle(),
               pos.getX() + 0.2 + random.nextDouble() * 0.6,
               pos.getY() + 1.0,
               pos.getZ() + 0.2 + random.nextDouble() * 0.6,
               0.0,
               0.02 + stage * 0.01,
               0.0
            );
         }
      }

      if (stage > 0 && random.nextInt(4) == 0) {
         world.addParticleClient(
            ParticleTypes.ENCHANT,
            pos.getX() + 0.5,
            pos.getY() + 2,
            pos.getZ() + 0.5,
            (random.nextDouble() - 0.5) * 2.0,
            -1.0,
            (random.nextDouble() - 0.5) * 2.0
         );
      }
   }
}
