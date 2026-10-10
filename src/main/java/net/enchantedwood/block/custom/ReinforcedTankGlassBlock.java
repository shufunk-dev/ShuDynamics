package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.enchantedwood.block.entity.TitaniumTankControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class ReinforcedTankGlassBlock extends TransparentBlock {
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public ReinforcedTankGlassBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            net.minecraft.world.item.ItemStack hand = player.getMainHandItem();
            net.enchantedwood.fluid.MoltenMetal filterMetal = net.enchantedwood.fluid.MoltenMetal.fromItem(hand);
            if (filterMetal != null) {
                if (!world.isClientSide()) {
                    TitaniumTankControllerBlockEntity controller = TitaniumTankControllerBlockEntity.findControllerForBlock(world, pos);
                    if (controller != null && controller.isFormed()) {
                        if (controller.getStoredFluidAmount() > 0 && controller.getFluidType() != filterMetal) {
                            player.sendOverlayMessage(Component.literal("§c⚠ Tank contains " + controller.getStoredFluidAmount() + " mB of " + controller.getFluidType().getDisplayName() + "! Break and replace a block to purge first."));
                            return InteractionResult.SUCCESS;
                        }
                        controller.setFilterFluid(filterMetal);
                        player.sendOverlayMessage(Component.literal("§a✔ 5x5 Tank locked to: §f" + filterMetal.getDisplayName()));
                        return InteractionResult.SUCCESS;
                    }
                }
                return InteractionResult.SUCCESS;
            } else if (hand.isEmpty()) {
                if (!world.isClientSide()) {
                    TitaniumTankControllerBlockEntity controller = TitaniumTankControllerBlockEntity.findControllerForBlock(world, pos);
                    if (controller != null && controller.isFormed()) {
                        controller.setFilterFluid(net.enchantedwood.fluid.MoltenMetal.NONE);
                        player.sendOverlayMessage(Component.literal("§eTank filter cleared (Accepts any fluid)."));
                        return InteractionResult.SUCCESS;
                    }
                }
                return InteractionResult.SUCCESS;
            }
            // Sneaking with non-metal item (like a Sign) -> PASS so signs/blocks can be placed on the glass!
            return InteractionResult.PASS;
        }

        if (!world.isClientSide()) {
            TitaniumTankControllerBlockEntity controller = TitaniumTankControllerBlockEntity.findControllerForBlock(world, pos);
            if (controller != null) {
                if (controller.isFormed()) {
                    player.openMenu(controller);
                    return InteractionResult.SUCCESS;
                } else if (controller.tryFormStructure()) {
                    player.sendOverlayMessage(Component.literal("§a✔ 5x5 Titanium Multi-Fluid Tank formed!"));
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (!world.isClientSide()) {
            TitaniumTankControllerBlockEntity controller = TitaniumTankControllerBlockEntity.findControllerForBlock(world, pos);
            if (controller != null && controller.isFormed()) {
                controller.dismantleStructure();
            }
        }
        return super.playerWillDestroy(world, pos, state, player);
    }
}
