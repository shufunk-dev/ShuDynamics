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
import net.enchantedwood.block.entity.GowningAirlockDoorBlockEntity;
import net.enchantedwood.block.entity.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

public class GowningAirlockDoorBlock extends DoorBlock implements BlockEntityProvider {
    public static final MapCodec<GowningAirlockDoorBlock> CODEC = createCodec(settings -> new GowningAirlockDoorBlock(BlockSetType.IRON, settings));

    public GowningAirlockDoorBlock(BlockSetType type, Settings settings) {
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
            return new GowningAirlockDoorBlockEntity(pos, state);
        }
        return null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world instanceof ServerWorld serverWorld && state.get(HALF) == DoubleBlockHalf.LOWER && type == ModBlockEntities.GOWNING_AIRLOCK_DOOR_BE) {
            return (w, pos, st, blockEntity) -> GowningAirlockDoorBlockEntity.tick(serverWorld, pos, st, (GowningAirlockDoorBlockEntity) blockEntity);
        }
        return null;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        BlockPos lowerPos = state.get(HALF) == DoubleBlockHalf.LOWER ? pos : pos.down();

        // Clicking the interior face opens door for manual exit
        Direction facing = state.get(FACING);
        Direction hitSide = hit.getSide();
        if (hitSide == facing.getOpposite()) {
            if (!world.isClient()) {
                BlockEntity be = world.getBlockEntity(lowerPos);
                if (be instanceof GowningAirlockDoorBlockEntity doorBe) {
                    doorBe.openForExit();
                }
            }
            return ActionResult.SUCCESS;
        }

        return ActionResult.CONSUME;
    }
}
