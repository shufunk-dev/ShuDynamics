package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.enchantedwood.block.entity.DecontaminationAirlockDoorBlockEntity;
import net.enchantedwood.block.entity.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

public class DecontaminationAirlockDoorBlock extends DoorBlock implements BlockEntityProvider {
    public static final MapCodec<DecontaminationAirlockDoorBlock> CODEC = createCodec(settings -> new DecontaminationAirlockDoorBlock(BlockSetType.IRON, settings));

    public DecontaminationAirlockDoorBlock(BlockSetType type, Settings settings) {
        super(type, settings);
    }

    @Override
    public MapCodec<? extends DoorBlock> getCodec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        if (state.get(HALF) == DoubleBlockHalf.LOWER) {
            return new DecontaminationAirlockDoorBlockEntity(pos, state);
        }
        return null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world instanceof ServerWorld serverWorld && state.get(HALF) == DoubleBlockHalf.LOWER && type == ModBlockEntities.DECONTAMINATION_AIRLOCK_DOOR_BE) {
            return (w, pos, st, blockEntity) -> DecontaminationAirlockDoorBlockEntity.tick(serverWorld, pos, st, (DecontaminationAirlockDoorBlockEntity) blockEntity);
        }
        return null;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        BlockPos lowerPos = state.get(HALF) == DoubleBlockHalf.LOWER ? pos : pos.down();

        // Shift-Right-Click toggles security mode (Cleanroom vs Anteroom)
        if (player.isSneaking()) {
            if (!world.isClient()) {
                BlockEntity be = world.getBlockEntity(lowerPos);
                if (be instanceof DecontaminationAirlockDoorBlockEntity doorBe) {
                    doorBe.toggleSecurityMode(player);
                }
            }
            return ActionResult.SUCCESS;
        }

        // Check if interacting from interior (exit side)
        Direction facing = state.get(FACING);
        Direction hitSide = hit.getSide();

        // If clicking the back face (inside), allow opening to exit
        if (hitSide == facing.getOpposite()) {
            if (!world.isClient()) {
                BlockEntity be = world.getBlockEntity(lowerPos);
                if (be instanceof DecontaminationAirlockDoorBlockEntity doorBe) {
                    doorBe.openForExit();
                }
            }
            return ActionResult.SUCCESS;
        }

        return ActionResult.CONSUME;
    }
}
