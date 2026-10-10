package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.enchantedwood.block.entity.DigitalConverterBlockEntity;
import net.enchantedwood.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class DigitalConverterBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public DigitalConverterBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DigitalConverterBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        if (world instanceof ServerLevel serverWorld && type == ModBlockEntities.DIGITAL_CONVERTER_BLOCK_ENTITY) {
            return (w, pos, st, blockEntity) -> DigitalConverterBlockEntity.tick(serverWorld, pos, st, (DigitalConverterBlockEntity) blockEntity);
        }
        return null;
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (state.getValue(LIT)) {
            double d = pos.getX() + 0.5;
            double e = pos.getY() + 0.5;
            double f = pos.getZ() + 0.5;

            Direction direction = state.getValue(FACING);
            Direction.Axis axis = direction.getAxis();
            double h = random.nextDouble() * 0.6 - 0.3;
            double i = axis == Direction.Axis.X ? direction.getStepX() * 0.52 : h;
            double j = random.nextDouble() * 6.0 / 16.0;
            double k = axis == Direction.Axis.Z ? direction.getStepZ() * 0.52 : h;

            world.addParticle(ParticleTypes.ENCHANT, d + i, e + j, f + k, 0.0, 0.05, 0.0);
            if (random.nextDouble() < 0.1) {
                world.addParticle(ParticleTypes.ELECTRIC_SPARK, d, e, f, 0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide()) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof DigitalConverterBlockEntity converter) {
                boolean online = converter.isNetworkOnline();
                int stored = converter.getNetworkStoredCount();
                int capacity = converter.getNetworkCapacity();
                if (online) {
                    player.sendOverlayMessage(Component.literal("§b[Digital Converter] §aOnline §7- Digital Storage: §e" + stored + "§7/§e" + capacity + " §7items"));
                } else {
                    player.sendOverlayMessage(Component.literal("§b[Digital Converter] §cOffline §7- No active Digital Storage Controller or Terminal in range"));
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel world, BlockPos pos, boolean moved) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof DigitalConverterBlockEntity converterEntity) {
            Containers.dropContents(world, pos, converterEntity);
        }
        super.affectNeighborsAfterRemoval(state, world, pos, moved);
    }
}
