package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class GenericCropBlock extends CropBlock {
    public static final int MAX_AGE = 7;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_7;

    private static final VoxelShape[] AGE_TO_SHAPE = new VoxelShape[]{
            Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0),
            Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0),
            Block.box(0.0, 0.0, 0.0, 16.0, 6.0, 16.0),
            Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0),
            Block.box(0.0, 0.0, 0.0, 16.0, 10.0, 16.0),
            Block.box(0.0, 0.0, 0.0, 16.0, 12.0, 16.0),
            Block.box(0.0, 0.0, 0.0, 16.0, 14.0, 16.0),
            Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0)
    };

    private final Supplier<ItemLike> produceSupplier;
    private final Supplier<ItemLike> seedSupplier;

    public GenericCropBlock(Properties settings, Supplier<ItemLike> produceSupplier, Supplier<ItemLike> seedSupplier) {
        super(settings);
        this.produceSupplier = produceSupplier;
        this.seedSupplier = seedSupplier;
    }

    public ItemLike getSeed() {
        return this.seedSupplier.get();
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return this.seedSupplier.get();
    }

    @Override
    public IntegerProperty getAgeProperty() {
        return AGE;
    }

    @Override
    public int getMaxAge() {
        return MAX_AGE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return AGE_TO_SHAPE[this.getAge(state)];
    }

    @Override
    protected boolean mayPlaceOn(BlockState floor, BlockGetter world, BlockPos pos) {
        return floor.is(net.minecraft.world.level.block.Blocks.FARMLAND) || floor.is(net.enchantedwood.block.ModBlocks.VOLCANIC_SOIL);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (this.isMaxAge(state)) {
            if (world.isClientSide()) {
                return InteractionResult.SUCCESS;
            }

            ItemStack held = player.getMainHandItem();
            int fortune = 0;
            ItemEnchantments enchantments = held.get(DataComponents.ENCHANTMENTS);
            if (enchantments != null) {
                for (var entry : enchantments.entrySet()) {
                    if (entry.getKey().is(Enchantments.FORTUNE)) {
                        fortune = entry.getIntValue();
                        break;
                    }
                }
            }

            int produceCount = 1;
            int seedCount = 1;
            if (fortune > 0) {
                produceCount += world.getRandom().nextInt(fortune + 1);
                seedCount += world.getRandom().nextInt(fortune + 1);
            } else if (world.getRandom().nextFloat() < 0.5f) {
                seedCount++;
            }

            ItemLike produce = this.produceSupplier.get();
            if (produce == net.enchantedwood.item.ModItems.SOYBEANS) {
                produceCount = Math.max(2, produceCount + 1);
            }

            popResource(world, pos, new ItemStack(produce, produceCount));
            popResource(world, pos, new ItemStack(this.seedSupplier.get(), seedCount));

            world.playSound(null, pos, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
            BlockState resetState = state.setValue(this.getAgeProperty(), 0);
            world.setBlock(pos, resetState, Block.UPDATE_CLIENTS);
            world.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, resetState));

            if (!player.isCreative() && held.is(ItemTags.HOES)) {
                held.hurtAndBreak(1, (ServerLevel) world, (ServerPlayer) player, item -> {});
            }

            return InteractionResult.SUCCESS;
        }
        return super.useWithoutItem(state, world, pos, player, hit);
    }
}
