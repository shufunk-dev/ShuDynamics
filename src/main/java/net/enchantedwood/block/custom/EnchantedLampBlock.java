package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.enchantedwood.block.entity.EnchantedLampBlockEntity;
import net.enchantedwood.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class EnchantedLampBlock extends Block implements EntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public EnchantedLampBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, true)); // Defaults to ON
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(LIT, true);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide()) {
            boolean current = state.getValue(LIT);
            boolean newState = !current;
            world.setBlock(pos, state.setValue(LIT, newState), 3);
            world.playSound(null, pos, newState ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.LEVER_CLICK,
                    SoundSource.BLOCKS, 0.8f, newState ? 1.4f : 0.8f);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block sourceBlock, @Nullable net.minecraft.world.level.redstone.Orientation wireOrientation, boolean notify) {
        if (!world.isClientSide()) {
            boolean hasPower = world.hasNeighborSignal(pos);
            if (hasPower && !state.getValue(LIT)) {
                world.setBlock(pos, state.setValue(LIT, true), 3);
            }
        }
        super.neighborChanged(state, world, pos, sourceBlock, wireOrientation, notify);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnchantedLampBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        if (world instanceof ServerLevel serverWorld && type == ModBlockEntities.ENCHANTED_LAMP_BLOCK_ENTITY) {
            return (w, pos, st, blockEntity) -> EnchantedLampBlockEntity.tick(serverWorld, pos, st, (EnchantedLampBlockEntity) blockEntity);
        }
        return null;
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            double x = pos.getX() + 0.5 + (random.nextDouble() * 0.4 - 0.2);
            double y = pos.getY() + 0.5 + (random.nextDouble() * 0.4 - 0.2);
            double z = pos.getZ() + 0.5 + (random.nextDouble() * 0.4 - 0.2);

            if (random.nextDouble() < 0.3) {
                world.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0, 0.01, 0.0);
            }
            if (random.nextDouble() < 0.2) {
                world.addParticle(ParticleTypes.ENCHANT, x, y + 0.3, z, 0.0, 0.05, 0.0);
            }
        }
    }
}
