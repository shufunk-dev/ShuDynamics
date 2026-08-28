package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.enchantedwood.block.entity.TitaniumTankControllerBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.TransparentBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ReinforcedTankGlassBlock extends TransparentBlock {
    public static final MapCodec<ReinforcedTankGlassBlock> CODEC = createCodec(ReinforcedTankGlassBlock::new);
    public static final BooleanProperty FORMED = BooleanProperty.of("formed");

    public ReinforcedTankGlassBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FORMED, false));
    }

    @Override
    protected MapCodec<? extends TransparentBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient()) {
            // Search in a 5x5x5 box for controller to open menu or try forming
            for (int dy = -4; dy <= 4; dy++) {
                for (int dx = -4; dx <= 4; dx++) {
                    for (int dz = -4; dz <= 4; dz++) {
                        BlockPos checkPos = pos.add(dx, dy, dz);
                        BlockEntity targetBE = world.getBlockEntity(checkPos);
                        if (targetBE instanceof TitaniumTankControllerBlockEntity controller) {
                            if (controller.isFormed()) {
                                player.openHandledScreen(controller);
                                return ActionResult.SUCCESS;
                            } else if (controller.tryFormStructure()) {
                                player.sendMessage(Text.literal("§a✔ 5x5 Titanium Lava Reservoir formed!"), true);
                                return ActionResult.SUCCESS;
                            }
                        }
                    }
                }
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient()) {
            // If broken, find controller and dismantle safely
            for (int dy = -4; dy <= 4; dy++) {
                for (int dx = -4; dx <= 4; dx++) {
                    for (int dz = -4; dz <= 4; dz++) {
                        BlockPos checkPos = pos.add(dx, dy, dz);
                        BlockEntity targetBE = world.getBlockEntity(checkPos);
                        if (targetBE instanceof TitaniumTankControllerBlockEntity controller && controller.isFormed()) {
                            controller.dismantleStructure();
                            break;
                        }
                    }
                }
            }
        }
        return super.onBreak(world, pos, state, player);
    }
}
