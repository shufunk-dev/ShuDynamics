package net.enchantedwood.block.custom;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

public class FruitLeavesBlock extends LeavesBlock implements BonemealableBlock {
    public static final int MAX_AGE = 3;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;

    private final Supplier<ItemLike> fruitSupplier;

    public FruitLeavesBlock(Properties settings, Supplier<ItemLike> fruitSupplier) {
        super(AmbientLeavesBlockSoundPlayer.noAmbientSound(), settings);
        this.fruitSupplier = fruitSupplier;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(DISTANCE, 7)
                .setValue(PERSISTENT, false)
                .setValue(WATERLOGGED, false)
                .setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(AGE);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return super.isRandomlyTicking(state) || state.getValue(AGE) < MAX_AGE;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        super.randomTick(state, world, pos, random);
        if (!decaying(state)) {
            int age = state.getValue(AGE);
            if (age < MAX_AGE && random.nextInt(5) == 0 && world.getRawBrightness(pos.above(), 0) >= 9) {
                BlockState nextState = state.setValue(AGE, age + 1);
                world.setBlock(pos, nextState, Block.UPDATE_CLIENTS);
                world.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(nextState));
            }
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        int age = state.getValue(AGE);
        if (age < MAX_AGE && stack.is(Items.BONE_MEAL)) {
            return InteractionResult.PASS;
        }
        return super.useItemOn(stack, state, world, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        int age = state.getValue(AGE);
        if (age == MAX_AGE) {
            if (world instanceof ServerLevel) {
                int count = 1 + world.getRandom().nextInt(2);
                popResource(world, pos, new ItemStack(this.fruitSupplier.get(), count));
            }
            world.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0f, 0.8f + world.getRandom().nextFloat() * 0.4f);
            BlockState nextState = state.setValue(AGE, 0);
            world.setBlock(pos, nextState, Block.UPDATE_CLIENTS);
            world.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, nextState));
            return InteractionResult.SUCCESS;
        }
        return super.useWithoutItem(state, world, pos, player, hit);
    }

    @Override
    protected void spawnAfterBreak(BlockState state, ServerLevel world, BlockPos pos, ItemStack tool, boolean dropExperience) {
        super.spawnAfterBreak(state, world, pos, tool, dropExperience);
        if (state.getValue(AGE) == MAX_AGE) {
            popResource(world, pos, new ItemStack(this.fruitSupplier.get(), 1));
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader world, BlockPos pos, BlockState state, BonemealSource source) {
        return state.getValue(AGE) < MAX_AGE;
    }

    @Override
    public boolean isBonemealSuccess(Level world, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel world, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        int nextAge = Math.min(MAX_AGE, state.getValue(AGE) + 1);
        world.setBlock(pos, state.setValue(AGE, nextAge), Block.UPDATE_CLIENTS);
    }
}
