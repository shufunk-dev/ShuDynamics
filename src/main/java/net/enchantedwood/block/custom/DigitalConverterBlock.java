package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.enchantedwood.block.entity.DigitalConverterBlockEntity;
import net.enchantedwood.block.entity.ModBlockEntities;

public class DigitalConverterBlock extends HorizontalFacingBlock implements BlockEntityProvider {
    public static final MapCodec<DigitalConverterBlock> CODEC = createCodec(DigitalConverterBlock::new);
    public static final BooleanProperty LIT = Properties.LIT;

    public DigitalConverterBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(LIT, false));
    }

    @Override
    protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
        return CODEC;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new DigitalConverterBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world instanceof ServerWorld serverWorld && type == ModBlockEntities.DIGITAL_CONVERTER_BLOCK_ENTITY) {
            return (w, pos, st, blockEntity) -> DigitalConverterBlockEntity.tick(serverWorld, pos, st, (DigitalConverterBlockEntity) blockEntity);
        }
        return null;
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, net.minecraft.util.math.random.Random random) {
        if (state.get(LIT)) {
            double d = pos.getX() + 0.5;
            double e = pos.getY() + 0.5;
            double f = pos.getZ() + 0.5;

            Direction direction = state.get(FACING);
            Direction.Axis axis = direction.getAxis();
            double h = random.nextDouble() * 0.6 - 0.3;
            double i = axis == Direction.Axis.X ? direction.getOffsetX() * 0.52 : h;
            double j = random.nextDouble() * 6.0 / 16.0;
            double k = axis == Direction.Axis.Z ? direction.getOffsetZ() * 0.52 : h;

            world.addParticleClient(ParticleTypes.ENCHANT, d + i, e + j, f + k, 0.0, 0.05, 0.0);
            if (random.nextDouble() < 0.1) {
                world.addParticleClient(ParticleTypes.ELECTRIC_SPARK, d, e, f, 0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient()) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof DigitalConverterBlockEntity converter) {
                boolean online = converter.isNetworkOnline();
                int stored = converter.getNetworkStoredCount();
                int capacity = converter.getNetworkCapacity();
                if (online) {
                    player.sendMessage(Text.literal("§b[Digital Converter] §aOnline §7- Digital Storage: §e" + stored + "§7/§e" + capacity + " §7items"), true);
                } else {
                    player.sendMessage(Text.literal("§b[Digital Converter] §cOffline §7- No active Digital Storage Controller or Terminal in range"), true);
                }
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof DigitalConverterBlockEntity converterEntity) {
            ItemScatterer.spawn(world, pos, converterEntity);
        }
        super.onStateReplaced(state, world, pos, moved);
    }
}
