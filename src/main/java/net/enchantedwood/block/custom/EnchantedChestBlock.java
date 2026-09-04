package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.enchantedwood.block.entity.ModBlockEntities;
import net.enchantedwood.block.entity.EnchantedChestBlockEntity;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.GearItem;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class EnchantedChestBlock extends HorizontalFacingBlock implements BlockEntityProvider {
    public static final MapCodec<EnchantedChestBlock> CODEC = createCodec(EnchantedChestBlock::new);
    public static final EnumProperty<GearTier> GEAR_TIER = EnumProperty.of("gear_tier", GearTier.class);
    protected static final net.minecraft.util.shape.VoxelShape SHAPE = Block.createCuboidShape(1.0, 0.0, 1.0, 15.0, 14.0, 15.0);

    public EnchantedChestBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(GEAR_TIER, GearTier.NONE));
    }

    @Override
    protected net.minecraft.util.shape.VoxelShape getOutlineShape(BlockState state, net.minecraft.world.BlockView world, BlockPos pos, net.minecraft.block.ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.INVISIBLE;
    }

    @Override
    protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
        return CODEC;
    }


    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new EnchantedChestBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (type == ModBlockEntities.ENCHANTED_CHEST_BLOCK_ENTITY) {
            return world.isClient()
                    ? (w, pos, st, be) -> EnchantedChestBlockEntity.clientTick(w, pos, st, (EnchantedChestBlockEntity) be)
                    : (w, pos, st, be) -> EnchantedChestBlockEntity.tick(w, pos, st, (EnchantedChestBlockEntity) be);
        }
        return null;
    }

    @Override
    protected boolean onSyncedBlockEvent(BlockState state, World world, BlockPos pos, int type, int data) {
        super.onSyncedBlockEvent(state, world, pos, type, data);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity != null && blockEntity.onSyncedBlockEvent(type, data);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof EnchantedChestBlockEntity chestEntity) {
                ItemStack handStack = player.getStackInHand(Hand.MAIN_HAND);

                if (handStack.getItem() instanceof GearItem gearItem) {
                    GearTier newTier = gearItem.getGearTier();
                    GearTier currentTier = state.get(GEAR_TIER);
                    if (newTier.ordinal() > currentTier.ordinal()) {
                        boolean hasRedstone = player.getInventory().contains(new ItemStack(ModItems.ENCHANTED_REDSTONE));
                        if (hasRedstone || player.isCreative()) {
                            if (!player.isCreative()) {
                                player.getInventory().remove(stack -> stack.isOf(ModItems.ENCHANTED_REDSTONE), 1, player.playerScreenHandler.getCraftingInput());
                                handStack.decrement(1);
                            }
                            world.setBlockState(pos, state.with(GEAR_TIER, newTier), 3);
                            chestEntity.upgradeTier(newTier);
                            player.sendMessage(Text.translatable("message.enchantedwood.upgraded_chest_tier", newTier.asString().replace("_", " ").toUpperCase()), true);
                            return ActionResult.SUCCESS;
                        } else {
                            player.sendMessage(Text.translatable("message.enchantedwood.upgrade_requires_redstone"), true);
                            return ActionResult.SUCCESS;
                        }
                    }
                }

                if (blockEntity instanceof NamedScreenHandlerFactory factory) {
                    player.openHandledScreen(factory);
                }
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public ItemStack getPickStack(net.minecraft.world.WorldView world, BlockPos pos, BlockState state, boolean includeData) {
        GearTier currentTier = state.get(GEAR_TIER);
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
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (!world.isClient()) {
            GearTier tier = GearTier.NONE;
            if (itemStack.getItem() instanceof net.enchantedwood.item.custom.EnchantedChestTierItem tierItem) {
                tier = tierItem.getTier();
            } else {
                NbtComponent nbtComponent = itemStack.get(DataComponentTypes.CUSTOM_DATA);
                if (nbtComponent != null) {
                    NbtCompound nbt = nbtComponent.copyNbt();
                    Optional<String> tierOpt = nbt.getString("GearTier");
                    if (tierOpt.isPresent()) {
                        try {
                            tier = GearTier.valueOf(tierOpt.get());
                        } catch (Exception ignored) {}
                    }
                }
            }

            if (tier != GearTier.NONE) {
                world.setBlockState(pos, state.with(GEAR_TIER, tier), 3);
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof EnchantedChestBlockEntity chestEntity) {
                    chestEntity.upgradeTier(tier);
                }
            }
        }
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient() && !player.isCreative()) {
            GearTier currentTier = state.get(GEAR_TIER);
            ItemStack dropStack = switch (currentTier) {
                case COPPER -> new ItemStack(ModItems.COPPER_ENCHANTED_CHEST);
                case BRONZE -> new ItemStack(ModItems.BRONZE_ENCHANTED_CHEST);
                case IRON, ENCHANTED_IRON -> new ItemStack(ModItems.ENCHANTED_IRON_ENCHANTED_CHEST);
                case GOLD -> new ItemStack(ModItems.GOLD_ENCHANTED_CHEST);
                case DIAMOND -> new ItemStack(ModItems.DIAMOND_ENCHANTED_CHEST);
                case NETHERITE -> new ItemStack(ModItems.NETHERITE_ENCHANTED_CHEST);
                default -> new ItemStack(this);
            };

            Block.dropStack(world, pos, dropStack);

            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof EnchantedChestBlockEntity chestEntity) {
                ItemScatterer.spawn(world, pos, chestEntity);
                chestEntity.clear();
            }

            world.removeBlock(pos, false);
            return state;
        }
        return super.onBreak(world, pos, state, player);
    }




    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, GEAR_TIER);
    }

    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        if (!state.isOf(world.getBlockState(pos).getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof EnchantedChestBlockEntity chestEntity) {
                ItemScatterer.spawn(world, pos, chestEntity);
            }
            super.onStateReplaced(state, world, pos, moved);
        }
    }
}
