package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.function.Supplier;

public class GenericCropBlock extends CropBlock {
    public static final int MAX_AGE = 7;
    public static final IntProperty AGE = Properties.AGE_7;

    private static final VoxelShape[] AGE_TO_SHAPE = new VoxelShape[]{
            Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 2.0, 16.0),
            Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 4.0, 16.0),
            Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 6.0, 16.0),
            Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 8.0, 16.0),
            Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 10.0, 16.0),
            Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 12.0, 16.0),
            Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 14.0, 16.0),
            Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 16.0, 16.0)
    };

    private final Supplier<ItemConvertible> produceSupplier;
    private final Supplier<ItemConvertible> seedSupplier;

    public GenericCropBlock(Settings settings, Supplier<ItemConvertible> produceSupplier, Supplier<ItemConvertible> seedSupplier) {
        super(settings);
        this.produceSupplier = produceSupplier;
        this.seedSupplier = seedSupplier;
    }

    @Override
    public MapCodec<? extends CropBlock> getCodec() {
        return createCodec(s -> new GenericCropBlock(s, this.produceSupplier, this.seedSupplier));
    }

    public ItemConvertible getSeed() {
        return this.seedSupplier.get();
    }

    @Override
    protected ItemConvertible getSeedsItem() {
        return this.seedSupplier.get();
    }

    @Override
    public IntProperty getAgeProperty() {
        return AGE;
    }

    @Override
    public int getMaxAge() {
        return MAX_AGE;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return AGE_TO_SHAPE[this.getAge(state)];
    }

    @Override
    protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
        return floor.isOf(net.minecraft.block.Blocks.FARMLAND) || floor.isOf(net.enchantedwood.block.ModBlocks.VOLCANIC_SOIL);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (this.isMature(state)) {
            if (world.isClient()) {
                return ActionResult.SUCCESS;
            }

            ItemStack held = player.getMainHandStack();
            int fortune = 0;
            ItemEnchantmentsComponent enchantments = held.get(DataComponentTypes.ENCHANTMENTS);
            if (enchantments != null) {
                for (var entry : enchantments.getEnchantmentEntries()) {
                    if (entry.getKey().matchesKey(Enchantments.FORTUNE)) {
                        fortune = entry.getIntValue();
                        break;
                    }
                }
            }

            int produceCount = 1;
            int seedCount = 1;
            if (fortune > 0) {
                produceCount += world.random.nextInt(fortune + 1);
                seedCount += world.random.nextInt(fortune + 1);
            } else if (world.random.nextFloat() < 0.5f) {
                seedCount++;
            }

            ItemConvertible produce = this.produceSupplier.get();
            if (produce == net.enchantedwood.item.ModItems.SOYBEANS) {
                produceCount = Math.max(2, produceCount + 1);
            }

            dropStack(world, pos, new ItemStack(produce, produceCount));
            dropStack(world, pos, new ItemStack(this.seedSupplier.get(), seedCount));

            world.playSound(null, pos, SoundEvents.BLOCK_CROP_BREAK, SoundCategory.BLOCKS, 1.0f, 1.0f);
            BlockState resetState = state.with(this.getAgeProperty(), 0);
            world.setBlockState(pos, resetState, Block.NOTIFY_LISTENERS);
            world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(player, resetState));

            if (!player.isCreative() && held.isIn(ItemTags.HOES)) {
                held.damage(1, (ServerWorld) world, (ServerPlayerEntity) player, item -> {});
            }

            return ActionResult.SUCCESS;
        }
        return super.onUse(state, world, pos, player, hit);
    }
}
