package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.enchantedwood.block.entity.EnchantedLampBlockEntity;
import net.enchantedwood.block.entity.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

public class EnchantedLampBlock extends Block implements BlockEntityProvider {
    public static final MapCodec<EnchantedLampBlock> CODEC = createCodec(EnchantedLampBlock::new);
    public static final BooleanProperty LIT = Properties.LIT;

    public EnchantedLampBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(LIT, true)); // Defaults to ON
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(LIT, true);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient()) {
            boolean current = state.get(LIT);
            boolean newState = !current;
            world.setBlockState(pos, state.with(LIT, newState), 3);
            world.playSound(null, pos, newState ? SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME : SoundEvents.BLOCK_LEVER_CLICK,
                    SoundCategory.BLOCKS, 0.8f, newState ? 1.4f : 0.8f);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable net.minecraft.world.block.WireOrientation wireOrientation, boolean notify) {
        if (!world.isClient()) {
            boolean hasPower = world.isReceivingRedstonePower(pos);
            if (hasPower && !state.get(LIT)) {
                world.setBlockState(pos, state.with(LIT, true), 3);
            }
        }
        super.neighborUpdate(state, world, pos, sourceBlock, wireOrientation, notify);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new EnchantedLampBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world instanceof ServerWorld serverWorld && type == ModBlockEntities.ENCHANTED_LAMP_BLOCK_ENTITY) {
            return (w, pos, st, blockEntity) -> EnchantedLampBlockEntity.tick(serverWorld, pos, st, (EnchantedLampBlockEntity) blockEntity);
        }
        return null;
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (state.get(LIT)) {
            double x = pos.getX() + 0.5 + (random.nextDouble() * 0.4 - 0.2);
            double y = pos.getY() + 0.5 + (random.nextDouble() * 0.4 - 0.2);
            double z = pos.getZ() + 0.5 + (random.nextDouble() * 0.4 - 0.2);

            if (random.nextDouble() < 0.3) {
                world.addParticleClient(ParticleTypes.END_ROD, x, y, z, 0.0, 0.01, 0.0);
            }
            if (random.nextDouble() < 0.2) {
                world.addParticleClient(ParticleTypes.ENCHANT, x, y + 0.3, z, 0.0, 0.05, 0.0);
            }
        }
    }
}
