package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.enchantedwood.block.entity.TitaniumTankCasingBlockEntity;
import net.enchantedwood.block.entity.TitaniumTankControllerBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class TitaniumTankCasingBlock extends BlockWithEntity {
    public static final MapCodec<TitaniumTankCasingBlock> CODEC = createCodec(TitaniumTankCasingBlock::new);
    public static final BooleanProperty FORMED = BooleanProperty.of("formed");

    public TitaniumTankCasingBlock(Settings settings) {
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
        return new TitaniumTankCasingBlockEntity(pos, state);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient()) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof TitaniumTankCasingBlockEntity casingBE) {
                TitaniumTankControllerBlockEntity master = casingBE.getMaster();
                if (master != null && master.isFormed()) {
                    player.openHandledScreen(master);
                    return ActionResult.SUCCESS;
                }
            }

            // If not formed, search in a 5x5x5 cube above for the controller/inbound port to trigger formation
            for (int dy = 0; dy <= 4; dy++) {
                for (int dx = -4; dx <= 4; dx++) {
                    for (int dz = -4; dz <= 4; dz++) {
                        BlockPos checkPos = pos.add(dx, dy, dz);
                        BlockEntity targetBE = world.getBlockEntity(checkPos);
                        if (targetBE instanceof TitaniumTankControllerBlockEntity controller) {
                            if (controller.tryFormStructure()) {
                                player.sendMessage(Text.literal("§a✔ 5x5 Titanium Lava Reservoir formed!"), true);
                                return ActionResult.SUCCESS;
                            }
                        }
                    }
                }
            }

            player.sendMessage(Text.literal("§e[Titanium Reservoir] Structure incomplete (5x5x5 hollow frame with Top Inbound Port required)."), true);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient()) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof TitaniumTankCasingBlockEntity casingBE) {
                TitaniumTankControllerBlockEntity master = casingBE.getMaster();
                if (master != null) {
                    master.dismantleStructure();
                }
            }
        }
        return super.onBreak(world, pos, state, player);
    }
}
