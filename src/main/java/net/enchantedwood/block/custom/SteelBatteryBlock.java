package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.enchantedwood.block.entity.ModBlockEntities;
import net.enchantedwood.block.entity.SteelBatteryBlockEntity;
import org.jetbrains.annotations.Nullable;

public class SteelBatteryBlock extends BlockWithEntity {
    public static final MapCodec<SteelBatteryBlock> CODEC = createCodec(SteelBatteryBlock::new);
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;

    public SteelBatteryBlock(Settings settings) {
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
        return new SteelBatteryBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world instanceof ServerWorld serverWorld && type == ModBlockEntities.STEEL_BATTERY_BLOCK_ENTITY) {
            return (w, pos, st, blockEntity) -> SteelBatteryBlockEntity.tick(serverWorld, pos, st, (SteelBatteryBlockEntity) blockEntity);
        }
        return null;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        ItemStack held = player.getStackInHand(player.getActiveHand() != null ? player.getActiveHand() : net.minecraft.util.Hand.MAIN_HAND);
        if (held.getItem() instanceof net.enchantedwood.energy.ItemEnergyProvider batteryItem) {
            if (!world.isClient()) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof net.enchantedwood.energy.EnergyProvider provider) {
                    net.enchantedwood.energy.EnergyStorage blockStorage = provider.getEnergyStorage(null);
                    net.enchantedwood.energy.EnergyStorage itemStorage = batteryItem.getEnergyStorage(held);
                    if (blockStorage != null && itemStorage != null) {
                        int needed = itemStorage.getMaxEnergy() - itemStorage.getEnergy();
                        if (needed > 0 && blockStorage.getEnergy() > 0) {
                            int toTransfer = Math.min(needed, blockStorage.getEnergy());
                            int extracted = blockStorage.extractEnergy(toTransfer, false);
                            itemStorage.insertEnergy(extracted, false);
                            world.playSound(null, pos, net.minecraft.sound.SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, net.minecraft.sound.SoundCategory.BLOCKS, 1.0f, 1.5f);
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
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }
}
