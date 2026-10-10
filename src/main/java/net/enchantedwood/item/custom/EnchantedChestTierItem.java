package net.enchantedwood.item.custom;

import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.block.custom.EnchantedChestBlock;
import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.block.entity.EnchantedChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class EnchantedChestTierItem extends Item {
    private final GearTier tier;

    public EnchantedChestTierItem(GearTier tier, Properties settings) {
        super(settings);
        this.tier = tier;
    }

    public GearTier getTier() {
        return this.tier;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockPos placePos = pos.relative(context.getClickedFace());
        Player player = context.getPlayer();

        if (world.getBlockState(placePos).canBeReplaced(new BlockPlaceContext(context))) {
            if (!world.isClientSide()) {
                BlockState state = ModBlocks.ENCHANTED_CHEST.defaultBlockState()
                        .setValue(EnchantedChestBlock.FACING, player != null ? player.getDirection().getOpposite() : net.minecraft.core.Direction.NORTH)
                        .setValue(EnchantedChestBlock.GEAR_TIER, this.tier);

                world.setBlock(placePos, state, 3);
                BlockEntity be = world.getBlockEntity(placePos);
                if (be instanceof EnchantedChestBlockEntity chestEntity) {
                    chestEntity.upgradeTier(this.tier);
                }

                world.playSound(null, placePos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
                if (player != null && !player.isCreative()) {
                    context.getItemInHand().shrink(1);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
