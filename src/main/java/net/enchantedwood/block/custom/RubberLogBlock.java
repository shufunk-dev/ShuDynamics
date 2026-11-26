package net.enchantedwood.block.custom;

import net.enchantedwood.item.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PillarBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

public class RubberLogBlock extends PillarBlock {
    public static final BooleanProperty HAS_RESIN = BooleanProperty.of("has_resin");

    public RubberLogBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(AXIS, Direction.Axis.Y).with(HAS_RESIN, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(HAS_RESIN);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState state = super.getPlacementState(ctx);
        return state != null ? state.with(HAS_RESIN, false) : null;
    }

    @Override
    protected boolean hasRandomTicks(BlockState state) {
        return !state.get(HAS_RESIN);
    }

    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        super.randomTick(state, world, pos, random);
        if (!state.get(HAS_RESIN) && random.nextInt(5) == 0) {
            world.setBlockState(pos, state.with(HAS_RESIN, true), Block.NOTIFY_ALL);
        }
    }

    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!state.get(HAS_RESIN) && stack.isOf(Items.BONE_MEAL)) {
            if (world instanceof ServerWorld serverWorld) {
                world.setBlockState(pos, state.with(HAS_RESIN, true), Block.NOTIFY_ALL);
                world.playSound(null, pos, SoundEvents.ITEM_BONE_MEAL_USE, SoundCategory.BLOCKS, 1.0f, 1.0f);
                if (!player.isCreative()) {
                    stack.decrement(1);
                }
                serverWorld.syncWorldEvent(1505, pos, 15);
            }
            return ActionResult.SUCCESS;
        }
        return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (state.get(HAS_RESIN)) {
            if (world instanceof ServerWorld) {
                int count = 1 + world.random.nextInt(3); // 1-3 Resin
                dropStack(world, pos, new ItemStack(ModItems.RESIN, count));
                world.playSound(null, pos, SoundEvents.ITEM_HONEYCOMB_WAX_ON, SoundCategory.BLOCKS, 1.0f, 1.2f);
                world.playSound(null, pos, SoundEvents.BLOCK_SLIME_BLOCK_PLACE, SoundCategory.BLOCKS, 0.8f, 1.5f);
                BlockState nextState = state.with(HAS_RESIN, false);
                world.setBlockState(pos, nextState, Block.NOTIFY_ALL);
                world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(player, nextState));
            }
            return ActionResult.SUCCESS;
        }
        return super.onUse(state, world, pos, player, hit);
    }
}
