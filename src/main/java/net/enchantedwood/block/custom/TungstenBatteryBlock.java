package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.enchantedwood.block.entity.ModBlockEntities;
import net.enchantedwood.block.entity.TungstenBatteryBlockEntity;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.ItemEnergyProvider;
import org.jetbrains.annotations.Nullable;

public class TungstenBatteryBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public TungstenBatteryBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TungstenBatteryBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        if (world instanceof ServerLevel serverWorld && type == ModBlockEntities.TUNGSTEN_BATTERY_BE) {
            return (w, pos, st, blockEntity) -> TungstenBatteryBlockEntity.tick(serverWorld, pos, st, (TungstenBatteryBlockEntity) blockEntity);
        }
        return null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(player.getUsedItemHand() != null ? player.getUsedItemHand() : net.minecraft.world.InteractionHand.MAIN_HAND);
        if (held.getItem() instanceof ItemEnergyProvider batteryItem) {
            if (!world.isClientSide()) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof EnergyProvider provider) {
                    EnergyStorage blockStorage = provider.getEnergyStorage(null);
                    EnergyStorage itemStorage = batteryItem.getEnergyStorage(held);
                    if (blockStorage != null && itemStorage != null) {
                        if (player.isShiftKeyDown()) {
                            // Sneak + Right Click: Deposit battery power into the energy cell block
                            int spaceInBlock = blockStorage.getMaxEnergy() - blockStorage.getEnergy();
                            if (spaceInBlock > 0 && itemStorage.getEnergy() > 0) {
                                int extracted = itemStorage.extractEnergy(spaceInBlock, false);
                                blockStorage.insertEnergy(extracted, false);
                                world.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0f, 1.2f);
                                player.sendOverlayMessage(Component.literal("§6⚡ Energy Cell Charged: §f" + blockStorage.getEnergy() + " / " + blockStorage.getMaxEnergy() + " FE"));
                                return InteractionResult.SUCCESS;
                            } else if (spaceInBlock == 0) {
                                player.sendOverlayMessage(Component.literal("§a✔ Energy Cell is already fully charged!"));
                                return InteractionResult.SUCCESS;
                            }
                        } else {
                            // Normal Right Click: Charge held battery from energy cell block
                            int needed = itemStorage.getMaxEnergy() - itemStorage.getEnergy();
                            if (needed > 0 && blockStorage.getEnergy() > 0) {
                                int toTransfer = Math.min(needed, blockStorage.getEnergy());
                                int extracted = blockStorage.extractEnergy(toTransfer, false);
                                itemStorage.insertEnergy(extracted, false);
                                world.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 1.5f);
                                player.sendOverlayMessage(Component.literal("§b⚡ Battery Charged: §f" + itemStorage.getEnergy() + " / " + itemStorage.getMaxEnergy() + " FE"));
                                return InteractionResult.SUCCESS;
                            }
                        }
                    }
                }
            }
        }

        if (!world.isClientSide()) {
            MenuProvider screenHandlerFactory = (MenuProvider) world.getBlockEntity(pos);
            if (screenHandlerFactory != null) {
                player.openMenu(screenHandlerFactory);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel world, BlockPos pos, boolean moved) {
        if (!state.is(world.getBlockState(pos).getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof TungstenBatteryBlockEntity battery) {
                Containers.dropContents(world, pos, battery);
            }
            super.affectNeighborsAfterRemoval(state, world, pos, moved);
        }
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }
}
