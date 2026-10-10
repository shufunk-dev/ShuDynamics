package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.enchantedwood.block.entity.ModBlockEntities;
import net.enchantedwood.block.entity.TitaniumTankControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class TitaniumTankInboundPortBlock extends BaseEntityBlock {
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public TitaniumTankInboundPortBlock(Properties settings) {
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
        return new TitaniumTankControllerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        if (world instanceof ServerLevel serverWorld && type == ModBlockEntities.TITANIUM_TANK_CONTROLLER_BLOCK_ENTITY) {
            return (w, pos, st, blockEntity) -> TitaniumTankControllerBlockEntity.tick(serverWorld, pos, st, (TitaniumTankControllerBlockEntity) blockEntity);
        }
        return null;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            net.minecraft.world.item.ItemStack hand = player.getMainHandItem();
            net.enchantedwood.fluid.MoltenMetal filterMetal = net.enchantedwood.fluid.MoltenMetal.fromItem(hand);
            if (filterMetal != null) {
                if (!world.isClientSide()) {
                    BlockEntity be = world.getBlockEntity(pos);
                    if (be instanceof TitaniumTankControllerBlockEntity controller && controller.isFormed()) {
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
                    BlockEntity be = world.getBlockEntity(pos);
                    if (be instanceof TitaniumTankControllerBlockEntity controller && controller.isFormed()) {
                        controller.setFilterFluid(net.enchantedwood.fluid.MoltenMetal.NONE);
                        player.sendOverlayMessage(Component.literal("§eTank filter cleared (Accepts any fluid)."));
                        return InteractionResult.SUCCESS;
                    }
                }
                return InteractionResult.SUCCESS;
            }
            // Sneaking with non-metal item -> PASS so signs/blocks can be placed
            return InteractionResult.PASS;
        }

        if (!world.isClientSide()) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof TitaniumTankControllerBlockEntity controller) {
                if (controller.isFormed()) {
                    player.openMenu(controller);
                    return InteractionResult.SUCCESS;
                } else {
                    if (controller.tryFormStructure()) {
                        player.sendOverlayMessage(Component.literal("§a✔ 5x5 Titanium Multi-Fluid Tank formed!"));
                    } else {
                        player.sendOverlayMessage(Component.literal("§e[Titanium Tank] Structure incomplete (5x5x5 hollow frame with Top Inbound Port required)."));
                    }
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (!world.isClientSide()) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof TitaniumTankControllerBlockEntity controller) {
                controller.dismantleStructure();
            }
        }
        return super.playerWillDestroy(world, pos, state, player);
    }
}
