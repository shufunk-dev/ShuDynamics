package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.enchantedwood.block.entity.TitaniumTankCasingBlockEntity;
import net.enchantedwood.block.entity.TitaniumTankControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class TitaniumTankCasingBlock extends BaseEntityBlock {
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public TitaniumTankCasingBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TitaniumTankCasingBlockEntity(pos, state);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            net.minecraft.world.item.ItemStack hand = player.getMainHandItem();
            net.enchantedwood.fluid.MoltenMetal filterMetal = net.enchantedwood.fluid.MoltenMetal.fromItem(hand);
            if (filterMetal != null) {
                if (!world.isClientSide()) {
                    BlockEntity be = world.getBlockEntity(pos);
                    TitaniumTankControllerBlockEntity master = null;
                    if (be instanceof TitaniumTankCasingBlockEntity casingBE) {
                        master = casingBE.getMaster();
                    }
                    if (master == null) {
                        master = TitaniumTankControllerBlockEntity.findControllerForBlock(world, pos);
                    }
                    if (master != null && master.isFormed()) {
                        if (master.getStoredFluidAmount() > 0 && master.getFluidType() != filterMetal) {
                            player.sendOverlayMessage(Component.literal("§c⚠ Tank contains " + master.getStoredFluidAmount() + " mB of " + master.getFluidType().getDisplayName() + "! Break and replace a block to purge first."));
                            return InteractionResult.SUCCESS;
                        }
                        master.setFilterFluid(filterMetal);
                        player.sendOverlayMessage(Component.literal("§a✔ 5x5 Tank locked to: §f" + filterMetal.getDisplayName()));
                        return InteractionResult.SUCCESS;
                    }
                }
                return InteractionResult.SUCCESS;
            } else if (hand.isEmpty()) {
                if (!world.isClientSide()) {
                    BlockEntity be = world.getBlockEntity(pos);
                    TitaniumTankControllerBlockEntity master = null;
                    if (be instanceof TitaniumTankCasingBlockEntity casingBE) {
                        master = casingBE.getMaster();
                    }
                    if (master == null) {
                        master = TitaniumTankControllerBlockEntity.findControllerForBlock(world, pos);
                    }
                    if (master != null && master.isFormed()) {
                        master.setFilterFluid(net.enchantedwood.fluid.MoltenMetal.NONE);
                        player.sendOverlayMessage(Component.literal("§eTank filter cleared (Accepts any fluid)."));
                        return InteractionResult.SUCCESS;
                    }
                }
                return InteractionResult.SUCCESS;
            }
            // Sneaking with a non-metal item (e.g. Sign, Hanging Sign, Torch, Block) -> PASS so item can be placed on the casing!
            return InteractionResult.PASS;
        }

        if (!world.isClientSide()) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof TitaniumTankCasingBlockEntity casingBE) {
                TitaniumTankControllerBlockEntity master = casingBE.getMaster();
                if (master != null && master.isFormed()) {
                    player.openMenu(master);
                    return InteractionResult.SUCCESS;
                }
            }

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

            player.sendOverlayMessage(Component.literal("§e[Titanium Tank] Structure incomplete (5x5x5 hollow frame with Top Inbound Port required)."));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (!world.isClientSide()) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof TitaniumTankCasingBlockEntity casingBE) {
                TitaniumTankControllerBlockEntity master = casingBE.getMaster();
                if (master != null) {
                    master.dismantleStructure();
                } else {
                    TitaniumTankControllerBlockEntity controller = TitaniumTankControllerBlockEntity.findControllerForBlock(world, pos);
                    if (controller != null && controller.isFormed()) {
                        controller.dismantleStructure();
                    }
                }
            }
        }
        return super.playerWillDestroy(world, pos, state, player);
    }
}
