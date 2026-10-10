package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.enchantedwood.block.entity.GowningAirlockDoorBlockEntity;
import net.enchantedwood.block.entity.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

public class GowningAirlockDoorBlock extends DoorBlock implements EntityBlock {

    public GowningAirlockDoorBlock(BlockSetType type, Properties settings) {
        super(type, settings);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            return new GowningAirlockDoorBlockEntity(pos, state);
        }
        return null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        if (world instanceof ServerLevel serverWorld && state.getValue(HALF) == DoubleBlockHalf.LOWER && type == ModBlockEntities.GOWNING_AIRLOCK_DOOR_BE) {
            return (w, pos, st, blockEntity) -> GowningAirlockDoorBlockEntity.tick(serverWorld, pos, st, (GowningAirlockDoorBlockEntity) blockEntity);
        }
        return null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos lowerPos = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();

        // Clicking the interior face opens door for manual exit
        Direction facing = state.getValue(FACING);
        Direction hitSide = hit.getDirection();
        if (hitSide == facing.getOpposite()) {
            if (!world.isClientSide()) {
                BlockEntity be = world.getBlockEntity(lowerPos);
                if (be instanceof GowningAirlockDoorBlockEntity doorBe) {
                    doorBe.openForExit();
                }
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.CONSUME;
    }
}
