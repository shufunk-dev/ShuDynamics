package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.enchantedwood.block.entity.ModBlockEntities;
import net.enchantedwood.block.entity.EnchantedStorageControllerBlockEntity;

public class EnchantedStorageControllerBlock extends HorizontalFacingBlock implements BlockEntityProvider {
    public static final MapCodec<EnchantedStorageControllerBlock> CODEC = createCodec(EnchantedStorageControllerBlock::new);
    public static final BooleanProperty LIT = Properties.LIT;

    public EnchantedStorageControllerBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(LIT, false));
    }

    @Override
    protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
        return CODEC;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new EnchantedStorageControllerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world instanceof ServerWorld serverWorld && type == ModBlockEntities.ENCHANTED_STORAGE_CONTROLLER_BLOCK_ENTITY) {
            return (w, pos, st, blockEntity) -> EnchantedStorageControllerBlockEntity.tick(serverWorld, pos, st, (EnchantedStorageControllerBlockEntity) blockEntity);
        }
        return null;
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, net.minecraft.util.math.random.Random random) {
        if (state.get(LIT)) {
            double d = pos.getX() + 0.5;
            double e = pos.getY() + 0.6;
            double f = pos.getZ() + 0.5;
            if (random.nextDouble() < 0.2) {
                world.playSoundClient(d, e, f, net.enchantedwood.sound.ModSounds.CONTROLLER_HUM, net.minecraft.sound.SoundCategory.BLOCKS, 0.12F, 1.0F, false);
            }

            Direction direction = state.get(FACING);
            Direction.Axis axis = direction.getAxis();
            double h = random.nextDouble() * 0.6 - 0.3;
            double i = axis == Direction.Axis.X ? direction.getOffsetX() * 0.52 : h;
            double j = random.nextDouble() * 6.0 / 16.0;
            double k = axis == Direction.Axis.Z ? direction.getOffsetZ() * 0.52 : h;

            world.addParticleClient(net.minecraft.particle.ParticleTypes.SOUL_FIRE_FLAME, d + i, e + j, f + k, 0.0, 0.0, 0.0);
            world.addParticleClient(net.minecraft.particle.ParticleTypes.ENCHANT, d, e + 0.5, f, 0.0, 0.1, 0.0);
        }
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof NamedScreenHandlerFactory factory) {
                player.openHandledScreen(factory);
            }
        }
        return ActionResult.SUCCESS;
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
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof EnchantedStorageControllerBlockEntity controllerEntity) {
            ItemScatterer.spawn(world, pos, controllerEntity);
        }
        super.onStateReplaced(state, world, pos, moved);
    }
}
