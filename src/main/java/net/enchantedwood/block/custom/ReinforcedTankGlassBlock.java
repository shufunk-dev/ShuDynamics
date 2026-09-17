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
        if (player.isSneaking()) {
            net.minecraft.item.ItemStack hand = player.getMainHandStack();
            net.enchantedwood.fluid.MoltenMetal filterMetal = net.enchantedwood.fluid.MoltenMetal.fromItem(hand);
            if (filterMetal != null) {
                if (!world.isClient()) {
                    TitaniumTankControllerBlockEntity controller = TitaniumTankControllerBlockEntity.findControllerForBlock(world, pos);
                    if (controller != null && controller.isFormed()) {
                        if (controller.getStoredFluidAmount() > 0 && controller.getFluidType() != filterMetal) {
                            player.sendMessage(Text.literal("§c⚠ Tank contains " + controller.getStoredFluidAmount() + " mB of " + controller.getFluidType().getDisplayName() + "! Break and replace a block to purge first."), true);
                            return ActionResult.SUCCESS;
                        }
                        controller.setFilterFluid(filterMetal);
                        player.sendMessage(Text.literal("§a✔ 5x5 Tank locked to: §f" + filterMetal.getDisplayName()), true);
                        return ActionResult.SUCCESS;
                    }
                }
                return ActionResult.SUCCESS;
            } else if (hand.isEmpty()) {
                if (!world.isClient()) {
                    TitaniumTankControllerBlockEntity controller = TitaniumTankControllerBlockEntity.findControllerForBlock(world, pos);
                    if (controller != null && controller.isFormed()) {
                        controller.setFilterFluid(net.enchantedwood.fluid.MoltenMetal.NONE);
                        player.sendMessage(Text.literal("§eTank filter cleared (Accepts any fluid)."), true);
                        return ActionResult.SUCCESS;
                    }
                }
                return ActionResult.SUCCESS;
            }
            // Sneaking with non-metal item (like a Sign) -> PASS so signs/blocks can be placed on the glass!
            return ActionResult.PASS;
        }

        if (!world.isClient()) {
            TitaniumTankControllerBlockEntity controller = TitaniumTankControllerBlockEntity.findControllerForBlock(world, pos);
            if (controller != null) {
                if (controller.isFormed()) {
                    player.openHandledScreen(controller);
                    return ActionResult.SUCCESS;
                } else if (controller.tryFormStructure()) {
                    player.sendMessage(Text.literal("§a✔ 5x5 Titanium Multi-Fluid Tank formed!"), true);
                    return ActionResult.SUCCESS;
                }
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient()) {
            TitaniumTankControllerBlockEntity controller = TitaniumTankControllerBlockEntity.findControllerForBlock(world, pos);
            if (controller != null && controller.isFormed()) {
                controller.dismantleStructure();
            }
        }
        return super.onBreak(world, pos, state, player);
    }
}
