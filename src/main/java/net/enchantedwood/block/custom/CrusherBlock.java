package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
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
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.enchantedwood.block.entity.ModBlockEntities;
import net.enchantedwood.block.entity.CrusherBlockEntity;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.item.custom.GearItem;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class CrusherBlock extends HorizontalFacingBlock implements BlockEntityProvider {
    public static final MapCodec<CrusherBlock> CODEC = createCodec(CrusherBlock::new);
    public static final BooleanProperty LIT = Properties.LIT;
    public static final EnumProperty<GearTier> GEAR_TIER = EnumProperty.of("gear_tier", GearTier.class);

    public CrusherBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(LIT, false)
                .with(GEAR_TIER, GearTier.NONE));
    }

    @Override
    protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
        return CODEC;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CrusherBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world instanceof ServerWorld serverWorld && type == ModBlockEntities.CRUSHER_BLOCK_ENTITY) {
            return (w, pos, st, blockEntity) -> CrusherBlockEntity.tick(serverWorld, pos, st, (CrusherBlockEntity) blockEntity);
        }
        return null;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof CrusherBlockEntity crusherEntity) {
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
                            crusherEntity.setStack(2, new ItemStack(gearItem));
                            world.setBlockState(pos, state.with(GEAR_TIER, newTier), 3);
                            player.sendMessage(Text.translatable("message.enchantedwood.upgraded_tier", newTier.asString().replace("_", " ").toUpperCase()), true);
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
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (!world.isClient()) {
            NbtComponent nbtComponent = itemStack.get(DataComponentTypes.CUSTOM_DATA);
            if (nbtComponent != null) {
                NbtCompound nbt = nbtComponent.copyNbt();
                Optional<String> tierOpt = nbt.getString("GearTier");
                if (tierOpt.isPresent()) {
                    try {
                        GearTier tier = GearTier.valueOf(tierOpt.get());
                        if (tier != GearTier.NONE) {
                            world.setBlockState(pos, state.with(GEAR_TIER, tier), 3);
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient() && !player.isCreative()) {
            GearTier currentTier = state.get(GEAR_TIER);
            if (currentTier != GearTier.NONE) {
                ItemStack dropStack = new ItemStack(this);
                NbtCompound nbt = new NbtCompound();
                nbt.putString("GearTier", currentTier.name());
                NbtComponent.set(DataComponentTypes.CUSTOM_DATA, dropStack, nbt);

                String tierName = currentTier.asString().replace("_", " ").toUpperCase();
                dropStack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(tierName + " Crusher"));

                Block.dropStack(world, pos, dropStack);

                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof CrusherBlockEntity crusherEntity) {
                    ItemScatterer.spawn(world, pos, crusherEntity);
                    crusherEntity.clear();
                }

                world.removeBlock(pos, false);
                return state;
            }
        }
        return super.onBreak(world, pos, state, player);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT, GEAR_TIER);
    }

    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        if (!state.isOf(world.getBlockState(pos).getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof CrusherBlockEntity crusherEntity) {
                ItemScatterer.spawn(world, pos, crusherEntity);
            }
            super.onStateReplaced(state, world, pos, moved);
        }
    }
}
