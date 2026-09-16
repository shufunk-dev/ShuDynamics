package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.enchantedwood.block.entity.CircuitFabricatorBlockEntity;
import net.enchantedwood.block.entity.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

public class CircuitFabricatorBlock extends BlockWithEntity {
    public static final MapCodec<CircuitFabricatorBlock> CODEC = createCodec(CircuitFabricatorBlock::new);
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = Properties.LIT;

    public CircuitFabricatorBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(LIT, false));
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
        builder.add(FACING, LIT);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CircuitFabricatorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world instanceof ServerWorld serverWorld && type == ModBlockEntities.CIRCUIT_FABRICATOR_BE) {
            return (w, pos, st, blockEntity) -> CircuitFabricatorBlockEntity.tick(serverWorld, pos, st, (CircuitFabricatorBlockEntity) blockEntity);
        }
        return null;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        net.minecraft.item.ItemStack held = player.getMainHandStack();
        if (held.getItem() instanceof net.enchantedwood.energy.ItemEnergyProvider batteryItem) {
            if (!world.isClient()) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof CircuitFabricatorBlockEntity fab) {
                    net.enchantedwood.energy.EnergyStorage itemStorage = batteryItem.getEnergyStorage(held);
                    if (itemStorage != null) {
                        int needed = CircuitFabricatorBlockEntity.CAPACITY - fab.getEnergyStorage(null).getEnergy();
                        int extracted = itemStorage.extractEnergy(needed, false);
                        if (extracted > 0) {
                            fab.getEnergyStorage(null).insertEnergy(extracted, false);
                            player.sendMessage(net.minecraft.text.Text.literal(String.format("§b⚡ Charged Fabricator: +%,d FE (%,d / %,d FE)",
                                    extracted, fab.getEnergyStorage(null).getEnergy(), CircuitFabricatorBlockEntity.CAPACITY)), true);
                            world.playSound(null, pos, net.minecraft.sound.SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, net.minecraft.sound.SoundCategory.BLOCKS, 0.7f, 1.4f);
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
        ItemScatterer.onStateReplaced(state, world, pos);
        super.onStateReplaced(state, world, pos, moved);
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }
}
