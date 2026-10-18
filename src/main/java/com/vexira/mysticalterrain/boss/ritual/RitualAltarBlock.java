package com.vexira.mysticalterrain.boss.ritual;

import com.mojang.serialization.MapCodec;
import com.vexira.mysticalterrain.block.MysticalBlocks;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class RitualAltarBlock extends BaseEntityBlock {
   public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 6);
   private static final VoxelShape SHAPE = Shapes.or(
      Block.column(16.0, 0.0, 4.0), new VoxelShape[]{Block.column(12.0, 4.0, 12.0), Block.column(16.0, 12.0, 15.0)}
   );
   private final Ritual ritual;
   public RitualAltarBlock(Ritual ritual, Properties properties) {
      super(properties);
      this.ritual = ritual;
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(STAGE, 0));
   }

   public Ritual ritual() {
      return this.ritual;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{STAGE});
   }

   protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new RitualAltarBlockEntity(pos, state);
   }

   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
      return world.isClientSide() ? null : createTickerHelper(type, MysticalBosses.ALTAR_ENTITY, RitualAltarBlockEntity::serverTick);
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

   protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (stack.isEmpty()) {
         return InteractionResult.TRY_WITH_EMPTY_HAND;
      }

      Item required = this.offering();
      int jarIndex = MysticalBosses.jarIndex(stack);
      boolean fits = (required != null && stack.is(required)) || (this.ritual == Ritual.SEKHMAR && jarIndex >= 0);
      if (!fits) {
         return InteractionResult.TRY_WITH_EMPTY_HAND;
      }

      if (world.getBlockEntity(pos) instanceof RitualAltarBlockEntity altar) {
         if (world instanceof ServerLevel server) {
            Component refusal = altar.offer(server, player, stack, jarIndex);
            if (refusal != null) {
               player.sendOverlayMessage(refusal);
               return InteractionResult.FAIL;
            }

            stack.consume(1, player);
            world.playSound(null, pos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0F, 0.8F);
         }

         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.TRY_WITH_EMPTY_HAND;
      }
   }

   protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
      if (!world.isClientSide() && world instanceof ServerLevel server) {
         if (this.ritual == Ritual.HOLLOW_WARDEN) {
            if (world.getBlockEntity(pos) instanceof RitualAltarBlockEntity altar) {
               int lit = RitualFixtures.count(server, pos, MysticalBosses.GLOOM_BRAZIER, GloomBrazierBlock.LIT);
               if (lit >= 6) {
                  player.sendSystemMessage(Component.literal("§5[Ritual] §dAll 6 Gloom Braziers burn! The Hollow Warden stirs..."));
                  altar.begin(server);
                  return InteractionResult.SUCCESS;
               } else {
                  player.sendSystemMessage(Component.literal("§5[Ritual] §c" + lit + "/6 Gloom Braziers lit. Light all six braziers to awaken the Hollow Warden!"));
                  return InteractionResult.SUCCESS;
               }
            }
         } else if (this.ritual == Ritual.ARCHON) {
            if (world.getBlockEntity(pos) instanceof RitualAltarBlockEntity altar) {
               int charged = RitualFixtures.count(server, pos, MysticalBosses.RUNE_PILLAR, RunePillarBlock.CHARGED);
               if (charged >= 5) {
                  player.sendSystemMessage(Component.literal("§b[Ritual] §dAll 5 Rune Pillars are charged! Archon stirs..."));
                  altar.begin(server);
                  return InteractionResult.SUCCESS;
               } else {
                  player.sendSystemMessage(Component.literal("§b[Ritual] §c" + charged + "/5 Rune Pillars charged. Charge all five pillars with Lapis to awaken Archon!"));
                  return InteractionResult.SUCCESS;
               }
            }
         }

         player.sendSystemMessage(Component.translatable("ritual.mysticalterrain." + this.ritual.id() + ".hint"));
      }

      return InteractionResult.SUCCESS;
   }

   public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
      int stage = (Integer)state.getValue(STAGE);
      int count = 1 + stage * 2;

      for (int i = 0; i < count; i++) {
         if (random.nextInt(3) == 0) {
            world.addParticle(
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
         world.addParticle(
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
