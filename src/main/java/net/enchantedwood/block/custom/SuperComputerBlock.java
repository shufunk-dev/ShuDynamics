package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.enchantedwood.block.entity.ModBlockEntities;
import net.enchantedwood.block.entity.SuperComputerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

public class SuperComputerBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public SuperComputerBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SuperComputerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        if (world instanceof ServerLevel serverWorld && type == ModBlockEntities.SUPER_COMPUTER_BLOCK_ENTITY) {
            return (w, pos, st, blockEntity) -> SuperComputerBlockEntity.tick(serverWorld, pos, st, (SuperComputerBlockEntity) blockEntity);
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

            world.addParticle(ParticleTypes.ENCHANT, d + i, e + j, f + k, 0.0, 0.08, 0.0);
            if (random.nextDouble() < 0.15) {
                world.addParticle(ParticleTypes.ELECTRIC_SPARK, d, e + 0.4, f, 0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof MenuProvider factory) {
                player.openMenu(factory);
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
        if (blockEntity instanceof SuperComputerBlockEntity computerEntity) {
            // Drop only real items: Upgrade Slot (9) and Output Buffer (10..13)
            for (int i = SuperComputerBlockEntity.UPGRADE_SLOT; i < SuperComputerBlockEntity.OUTPUT_START + SuperComputerBlockEntity.OUTPUT_SIZE; i++) {
                ItemStack stack = computerEntity.getItem(i);
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), stack);
                }
            }
        }
        super.affectNeighborsAfterRemoval(state, world, pos, moved);
    }
}
