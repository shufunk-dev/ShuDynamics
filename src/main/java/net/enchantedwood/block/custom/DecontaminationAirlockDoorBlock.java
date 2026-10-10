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
import net.enchantedwood.block.entity.DecontaminationAirlockDoorBlockEntity;
import net.enchantedwood.block.entity.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

public class DecontaminationAirlockDoorBlock extends DoorBlock implements EntityBlock {

    public DecontaminationAirlockDoorBlock(BlockSetType type, Properties settings) {
        super(type, settings);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            return new DecontaminationAirlockDoorBlockEntity(pos, state);
        }
        return null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        if (world instanceof ServerLevel serverWorld && state.getValue(HALF) == DoubleBlockHalf.LOWER && type == ModBlockEntities.DECONTAMINATION_AIRLOCK_DOOR_BE) {
            return (w, pos, st, blockEntity) -> DecontaminationAirlockDoorBlockEntity.tick(serverWorld, pos, st, (DecontaminationAirlockDoorBlockEntity) blockEntity);
        }
        return null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos lowerPos = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();

        // Shift-Right-Click toggles security mode (Cleanroom vs Anteroom)
        if (player.isShiftKeyDown()) {
            if (!world.isClientSide()) {
                BlockEntity be = world.getBlockEntity(lowerPos);
                if (be instanceof DecontaminationAirlockDoorBlockEntity doorBe) {
                    doorBe.toggleSecurityMode(player);
                }
            }
            return InteractionResult.SUCCESS;
        }

        // Check if interacting from interior (exit side)
        Direction facing = state.getValue(FACING);
        Direction hitSide = hit.getDirection();

        // If clicking the back face (inside), allow opening to exit
        if (hitSide == facing.getOpposite()) {
            if (!world.isClientSide()) {
                BlockEntity be = world.getBlockEntity(lowerPos);
                if (be instanceof DecontaminationAirlockDoorBlockEntity doorBe) {
                    doorBe.openForExit();
                }
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.CONSUME;
    }
}
