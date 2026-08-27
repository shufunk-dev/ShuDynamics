package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.enchantedwood.block.entity.ModBlockEntities;
import net.enchantedwood.block.entity.TungstenBatteryBlockEntity;
import net.enchantedwood.energy.EnergyProvider;
import net.enchantedwood.energy.EnergyStorage;
import net.enchantedwood.energy.ItemEnergyProvider;
import org.jetbrains.annotations.Nullable;

public class TungstenBatteryBlock extends BlockWithEntity {
    public static final MapCodec<TungstenBatteryBlock> CODEC = createCodec(TungstenBatteryBlock::new);
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;

    public TungstenBatteryBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TungstenBatteryBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world instanceof ServerWorld serverWorld && type == ModBlockEntities.TUNGSTEN_BATTERY_BE) {
            return (w, pos, st, blockEntity) -> TungstenBatteryBlockEntity.tick(serverWorld, pos, st, (TungstenBatteryBlockEntity) blockEntity);
        }
        return null;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        ItemStack held = player.getStackInHand(player.getActiveHand() != null ? player.getActiveHand() : net.minecraft.util.Hand.MAIN_HAND);
        if (held.getItem() instanceof ItemEnergyProvider batteryItem) {
            if (!world.isClient()) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof EnergyProvider provider) {
                    EnergyStorage blockStorage = provider.getEnergyStorage(null);
                    EnergyStorage itemStorage = batteryItem.getEnergyStorage(held);
                    if (blockStorage != null && itemStorage != null) {
                        int needed = itemStorage.getMaxEnergy() - itemStorage.getEnergy();
                        if (needed > 0 && blockStorage.getEnergy() > 0) {
                            int toTransfer = Math.min(needed, blockStorage.getEnergy());
                            int extracted = blockStorage.extractEnergy(toTransfer, false);
                            itemStorage.insertEnergy(extracted, false);
                            world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 1.0f, 1.5f);
                            player.sendMessage(Text.literal("§b⚡ Battery Charged: §f" + itemStorage.getEnergy() + " / " + itemStorage.getMaxEnergy() + " FE"), true);
                            return ActionResult.SUCCESS;
                        } else if (needed == 0) {
                            player.sendMessage(Text.literal("§a✔ Battery is already fully charged!"), true);
                            return ActionResult.SUCCESS;
                        } else {
                            player.sendMessage(Text.literal("§e⚠ Battery Block is depleted."), true);
                            return ActionResult.SUCCESS;
                        }
                    }
                }
            }
            return ActionResult.SUCCESS;
        }

        if (!world.isClient()) {
            NamedScreenHandlerFactory screenHandlerFactory = (NamedScreenHandlerFactory) world.getBlockEntity(pos);
            if (screenHandlerFactory != null) {
                player.openHandledScreen(screenHandlerFactory);
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        if (!state.isOf(world.getBlockState(pos).getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof TungstenBatteryBlockEntity battery) {
                ItemScatterer.spawn(world, pos, battery);
            }
            super.onStateReplaced(state, world, pos, moved);
        }
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }
}
