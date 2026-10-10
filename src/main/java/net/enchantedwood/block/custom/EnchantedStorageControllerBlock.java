package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.enchantedwood.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
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
import net.enchantedwood.block.entity.EnchantedStorageControllerBlockEntity;

public class EnchantedStorageControllerBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public EnchantedStorageControllerBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnchantedStorageControllerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        if (world instanceof ServerLevel serverWorld && type == ModBlockEntities.ENCHANTED_STORAGE_CONTROLLER_BLOCK_ENTITY) {
            return (w, pos, st, blockEntity) -> EnchantedStorageControllerBlockEntity.tick(serverWorld, pos, st, (EnchantedStorageControllerBlockEntity) blockEntity);
        }
        return null;
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (state.getValue(LIT)) {
            double d = pos.getX() + 0.5;
            double e = pos.getY() + 0.6;
            double f = pos.getZ() + 0.5;
            if (random.nextDouble() < 0.2) {
                world.playLocalSound(d, e, f, net.enchantedwood.sound.ModSounds.CONTROLLER_HUM, net.minecraft.sounds.SoundSource.BLOCKS, 0.12F, 1.0F, false);
            }

            Direction direction = state.getValue(FACING);
            Direction.Axis axis = direction.getAxis();
            double h = random.nextDouble() * 0.6 - 0.3;
            double i = axis == Direction.Axis.X ? direction.getStepX() * 0.52 : h;
            double j = random.nextDouble() * 6.0 / 16.0;
            double k = axis == Direction.Axis.Z ? direction.getStepZ() * 0.52 : h;

            world.addParticle(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME, d + i, e + j, f + k, 0.0, 0.0, 0.0);
            world.addParticle(net.minecraft.core.particles.ParticleTypes.ENCHANT, d, e + 0.5, f, 0.0, 0.1, 0.0);
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
        if (blockEntity instanceof EnchantedStorageControllerBlockEntity controllerEntity) {
            Containers.dropContents(world, pos, controllerEntity);
        }
        super.affectNeighborsAfterRemoval(state, world, pos, moved);
    }
}
