package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.enchantedwood.block.entity.ModBlockEntities;
import net.enchantedwood.block.entity.DustSmelterBlockEntity;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.GearItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

public class DustSmelterBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final EnumProperty<GearTier> GEAR_TIER = EnumProperty.create("gear_tier", GearTier.class);

    public DustSmelterBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false)
                .setValue(GEAR_TIER, GearTier.NONE));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DustSmelterBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        if (world instanceof ServerLevel serverWorld && type == ModBlockEntities.DUST_SMELTER_BLOCK_ENTITY) {
            return (w, pos, st, blockEntity) -> DustSmelterBlockEntity.tick(serverWorld, pos, st, (DustSmelterBlockEntity) blockEntity);
        }
        return null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof DustSmelterBlockEntity dustSmelterEntity) {
                ItemStack handStack = player.getItemInHand(InteractionHand.MAIN_HAND);

                if (handStack.getItem() instanceof GearItem gearItem) {
                    GearTier newTier = gearItem.getGearTier();
                    GearTier currentTier = state.getValue(GEAR_TIER);
                    if (newTier.ordinal() > currentTier.ordinal()) {
                        boolean hasRedstone = player.getInventory().contains(new ItemStack(ModItems.ENCHANTED_REDSTONE));
                        if (hasRedstone || player.isCreative()) {
                            if (!player.isCreative()) {
                                player.getInventory().clearOrCountMatchingItems(stack -> stack.is(ModItems.ENCHANTED_REDSTONE), false, 1, player.inventoryMenu.getCraftSlots());
                                handStack.shrink(1);
                            }
                            dustSmelterEntity.setItem(2, new ItemStack(gearItem));
                            world.setBlock(pos, state.setValue(GEAR_TIER, newTier), 3);
                            player.sendOverlayMessage(Component.translatable("message.enchantedwood.upgraded_tier", newTier.getSerializedName().replace("_", " ").toUpperCase()));
                            return InteractionResult.SUCCESS;
                        } else {
                            player.sendOverlayMessage(Component.translatable("message.enchantedwood.upgrade_requires_redstone"));
                            return InteractionResult.SUCCESS;
                        }
                    }
                }

                if (blockEntity instanceof MenuProvider factory) {
                    player.openMenu(factory);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT, GEAR_TIER);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel world, BlockPos pos, boolean moved) {
        if (!state.is(world.getBlockState(pos).getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof DustSmelterBlockEntity dustSmelterEntity) {
                Containers.dropContents(world, pos, dustSmelterEntity);
            }
            super.affectNeighborsAfterRemoval(state, world, pos, moved);
        }
    }
}
