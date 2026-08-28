package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.enchantedwood.block.entity.ModBlockEntities;
import net.enchantedwood.block.entity.TitaniumTankControllerBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class TitaniumTankInboundPortBlock extends BlockWithEntity {
    public static final MapCodec<TitaniumTankInboundPortBlock> CODEC = createCodec(TitaniumTankInboundPortBlock::new);
    public static final BooleanProperty FORMED = BooleanProperty.of("formed");

    public TitaniumTankInboundPortBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FORMED, false));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TitaniumTankControllerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world instanceof ServerWorld serverWorld && type == ModBlockEntities.TITANIUM_TANK_CONTROLLER_BLOCK_ENTITY) {
            return (w, pos, st, blockEntity) -> TitaniumTankControllerBlockEntity.tick(serverWorld, pos, st, (TitaniumTankControllerBlockEntity) blockEntity);
        }
        return null;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient()) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof TitaniumTankControllerBlockEntity controller) {
                if (controller.isFormed()) {
                    player.openHandledScreen(controller);
                    return ActionResult.SUCCESS;
                } else {
                    if (controller.tryFormStructure()) {
                        player.sendMessage(Text.literal("§a✔ 5x5 Titanium Lava Reservoir formed!"), true);
                    } else {
                        player.sendMessage(Text.literal("§e[Titanium Reservoir] Structure incomplete (5x5x5 hollow frame with Top Inbound Port required)."), true);
                    }
                    return ActionResult.SUCCESS;
                }
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient()) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof TitaniumTankControllerBlockEntity controller) {
                controller.dismantleStructure();
            }
        }
        return super.onBreak(world, pos, state, player);
    }
}
