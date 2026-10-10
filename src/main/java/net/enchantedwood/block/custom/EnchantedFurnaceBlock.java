package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.enchantedwood.block.entity.ModBlockEntities;
import net.enchantedwood.block.entity.EnchantedFurnaceBlockEntity;
import net.enchantedwood.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class EnchantedFurnaceBlock extends AbstractFurnaceBlock {

    public EnchantedFurnaceBlock(Properties settings) {
        super(settings);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnchantedFurnaceBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return world instanceof ServerLevel serverWorld
                ? createTickerHelper(type, ModBlockEntities.ENCHANTED_FURNACE_BLOCK_ENTITY, (w, pos, st, blockEntity) -> EnchantedFurnaceBlockEntity.tick(serverWorld, pos, st, blockEntity))
                : null;
    }

    @Override
    protected void openContainer(Level world, BlockPos pos, Player player) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof MenuProvider factory) {
            player.openMenu(factory);
            player.awardStat(Stats.INTERACT_WITH_FURNACE);
        }
    }
}
