package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.enchantedwood.block.entity.ModBlockEntities;
import net.enchantedwood.block.entity.EnchantedChestBlockEntity;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.GearItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class EnchantedChestBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final EnumProperty<GearTier> GEAR_TIER = EnumProperty.create("gear_tier", GearTier.class);
    protected static final net.minecraft.world.phys.shapes.VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 14.0, 15.0);

    public EnchantedChestBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(GEAR_TIER, GearTier.NONE));
    }

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter world, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }


    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnchantedChestBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        if (type == ModBlockEntities.ENCHANTED_CHEST_BLOCK_ENTITY) {
            return world.isClientSide()
                    ? (w, pos, st, be) -> EnchantedChestBlockEntity.clientTick(w, pos, st, (EnchantedChestBlockEntity) be)
                    : (w, pos, st, be) -> EnchantedChestBlockEntity.tick(w, pos, st, (EnchantedChestBlockEntity) be);
        }
        return null;
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level world, BlockPos pos, int type, int data) {
        super.triggerEvent(state, world, pos, type, data);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity != null && blockEntity.triggerEvent(type, data);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof EnchantedChestBlockEntity chestEntity) {
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
                            world.setBlock(pos, state.setValue(GEAR_TIER, newTier), 3);
                            chestEntity.upgradeTier(newTier);
                            player.sendOverlayMessage(Component.translatable("message.enchantedwood.upgraded_chest_tier", newTier.getSerializedName().replace("_", " ").toUpperCase()));
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
    public ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
        GearTier currentTier = state.getValue(GEAR_TIER);
        return switch (currentTier) {
            case COPPER -> new ItemStack(ModItems.COPPER_ENCHANTED_CHEST);
            case BRONZE -> new ItemStack(ModItems.BRONZE_ENCHANTED_CHEST);
            case IRON, ENCHANTED_IRON -> new ItemStack(ModItems.ENCHANTED_IRON_ENCHANTED_CHEST);
            case GOLD -> new ItemStack(ModItems.GOLD_ENCHANTED_CHEST);
            case DIAMOND -> new ItemStack(ModItems.DIAMOND_ENCHANTED_CHEST);
            case NETHERITE -> new ItemStack(ModItems.NETHERITE_ENCHANTED_CHEST);
            default -> new ItemStack(this);
        };
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.setPlacedBy(world, pos, state, placer, itemStack);
        if (!world.isClientSide()) {
            GearTier tier = GearTier.NONE;
            if (itemStack.getItem() instanceof net.enchantedwood.item.custom.EnchantedChestTierItem tierItem) {
                tier = tierItem.getTier();
            } else {
                CustomData nbtComponent = itemStack.get(DataComponents.CUSTOM_DATA);
                if (nbtComponent != null) {
                    CompoundTag nbt = nbtComponent.copyTag();
                    Optional<String> tierOpt = nbt.getString("GearTier");
                    if (tierOpt.isPresent()) {
                        try {
                            tier = GearTier.valueOf(tierOpt.get());
                        } catch (Exception ignored) {}
                    }
                }
            }

            if (tier != GearTier.NONE) {
                world.setBlock(pos, state.setValue(GEAR_TIER, tier), 3);
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof EnchantedChestBlockEntity chestEntity) {
                    chestEntity.upgradeTier(tier);
                }
            }
        }
    }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (!world.isClientSide() && !player.isCreative()) {
            GearTier currentTier = state.getValue(GEAR_TIER);
            ItemStack dropStack = switch (currentTier) {
                case COPPER -> new ItemStack(ModItems.COPPER_ENCHANTED_CHEST);
                case BRONZE -> new ItemStack(ModItems.BRONZE_ENCHANTED_CHEST);
                case IRON, ENCHANTED_IRON -> new ItemStack(ModItems.ENCHANTED_IRON_ENCHANTED_CHEST);
                case GOLD -> new ItemStack(ModItems.GOLD_ENCHANTED_CHEST);
                case DIAMOND -> new ItemStack(ModItems.DIAMOND_ENCHANTED_CHEST);
                case NETHERITE -> new ItemStack(ModItems.NETHERITE_ENCHANTED_CHEST);
                default -> new ItemStack(this);
            };

            Block.popResource(world, pos, dropStack);

            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof EnchantedChestBlockEntity chestEntity) {
                Containers.dropContents(world, pos, chestEntity);
                chestEntity.clearContent();
            }

            world.removeBlock(pos, false);
            return state;
        }
        return super.playerWillDestroy(world, pos, state, player);
    }




    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, GEAR_TIER);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel world, BlockPos pos, boolean moved) {
        if (!state.is(world.getBlockState(pos).getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof EnchantedChestBlockEntity chestEntity) {
                Containers.dropContents(world, pos, chestEntity);
            }
            super.affectNeighborsAfterRemoval(state, world, pos, moved);
        }
    }
}
