package net.enchantedwood.item.custom;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.block.custom.EnchantedChestBlock;
import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.block.entity.EnchantedChestBlockEntity;

public class EnchantedChestTierItem extends Item {
    private final GearTier tier;

    public EnchantedChestTierItem(GearTier tier, Settings settings) {
        super(settings);
        this.tier = tier;
    }

    public GearTier getTier() {
        return this.tier;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        BlockPos placePos = pos.offset(context.getSide());
        PlayerEntity player = context.getPlayer();

        if (world.getBlockState(placePos).canReplace(new ItemPlacementContext(context))) {
            if (!world.isClient()) {
                BlockState state = ModBlocks.ENCHANTED_CHEST.getDefaultState()
                        .with(EnchantedChestBlock.FACING, player != null ? player.getHorizontalFacing().getOpposite() : net.minecraft.util.math.Direction.NORTH)
                        .with(EnchantedChestBlock.GEAR_TIER, this.tier);

                world.setBlockState(placePos, state, 3);
                BlockEntity be = world.getBlockEntity(placePos);
                if (be instanceof EnchantedChestBlockEntity chestEntity) {
                    chestEntity.upgradeTier(this.tier);
                }

                world.playSound(null, placePos, SoundEvents.BLOCK_WOOD_PLACE, SoundCategory.BLOCKS, 1.0f, 1.0f);
                if (player != null && !player.isCreative()) {
                    context.getStack().decrement(1);
                }
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }
}
