package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.enchantedwood.block.entity.ModBlockEntities;
import net.enchantedwood.block.entity.EnchantedFurnaceBlockEntity;
import net.enchantedwood.item.ModItems;
import org.jetbrains.annotations.Nullable;

public class EnchantedFurnaceBlock extends AbstractFurnaceBlock {
    public static final MapCodec<EnchantedFurnaceBlock> CODEC = createCodec(EnchantedFurnaceBlock::new);

    public EnchantedFurnaceBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends AbstractFurnaceBlock> getCodec() {
        return CODEC;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new EnchantedFurnaceBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world instanceof ServerWorld serverWorld
                ? validateTicker(type, ModBlockEntities.ENCHANTED_FURNACE_BLOCK_ENTITY, (w, pos, st, blockEntity) -> EnchantedFurnaceBlockEntity.tick(serverWorld, pos, st, blockEntity))
                : null;
    }

    @Override
    protected void openScreen(World world, BlockPos pos, PlayerEntity player) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof NamedScreenHandlerFactory factory) {
            player.openHandledScreen(factory);
            player.incrementStat(Stats.INTERACT_WITH_FURNACE);
        }
    }
}
