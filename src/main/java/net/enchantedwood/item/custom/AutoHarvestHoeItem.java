package net.enchantedwood.item.custom;

import net.enchantedwood.block.custom.CornCropBlock;
import net.enchantedwood.block.custom.GenericCropBlock;
import net.enchantedwood.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeetrootBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarrotBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.PotatoBlock;
import net.minecraft.world.level.block.TorchflowerCropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import java.util.List;

public class AutoHarvestHoeItem extends Item {

    public AutoHarvestHoeItem(Properties settings) {
        super(settings);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = world.getBlockState(pos);
        Player player = context.getPlayer();
        InteractionHand hand = context.getHand();
        ItemStack stack = context.getItemInHand();

        if (player == null) {
            return super.useOn(context);
        }

        CropHarvestInfo info = getCropHarvestInfo(state);
        if (info != null && info.isMature) {
            if (world.isClientSide()) {
                return InteractionResult.SUCCESS;
            }

            if (world instanceof ServerLevel serverWorld) {
                BlockEntity blockEntity = world.getBlockEntity(pos);
                List<ItemStack> drops = Block.getDrops(state, serverWorld, pos, blockEntity, player, stack);

                boolean replanted = false;
                if (!player.isCreative()) {
                    ItemStack seedStack = findSeedInInventory(player, info.seedItem);
                    if (!seedStack.isEmpty()) {
                        seedStack.shrink(1);
                        replanted = true;
                    } else {
                        // Use 1 seed from harvested drops to replant automatically
                        for (ItemStack drop : drops) {
                            if (drop.is(info.seedItem) && !drop.isEmpty()) {
                                drop.shrink(1);
                                replanted = true;
                                break;
                            }
                        }

                        if (!replanted) {
                            for (ItemStack drop : drops) {
                                if (drop.is(net.minecraft.tags.ItemTags.VILLAGER_PLANTABLE_SEEDS) || drop.getItem().getDescriptionId().contains("seed")) {
                                    drop.shrink(1);
                                    replanted = true;
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    replanted = true;
                }

                if (replanted) {
                    world.setBlock(pos, info.replantState, Block.UPDATE_ALL);
                    world.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);

                    for (ItemStack drop : drops) {
                        if (!drop.isEmpty()) {
                            Block.popResource(world, pos, drop);
                        }
                    }

                    EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                    stack.hurtAndBreak(1, player, slot);

                    world.playSound(null, pos, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
                    world.playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
                    player.awardStat(Stats.ITEM_USED.get(this));

                    return InteractionResult.SUCCESS;
                }
            }
        }

        return super.useOn(context);
    }

    private ItemStack findSeedInInventory(Player player, Item seedItem) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack itemStack = player.getInventory().getItem(i);
            if (itemStack.is(seedItem)) {
                return itemStack;
            }
        }
        return ItemStack.EMPTY;
    }

    private CropHarvestInfo getCropHarvestInfo(BlockState state) {
        Block block = state.getBlock();

        if (block instanceof CropBlock cropBlock) {
            boolean mature = cropBlock.isMaxAge(state);
            Item seedItem = getCropSeedItem(cropBlock);
            BlockState replantState = cropBlock.getStateForAge(0);
            return new CropHarvestInfo(mature, seedItem, replantState);
        } else if (block instanceof NetherWartBlock) {
            int age = state.getValue(NetherWartBlock.AGE);
            boolean mature = age >= 3;
            Item seedItem = Items.NETHER_WART;
            BlockState replantState = Blocks.NETHER_WART.defaultBlockState();
            return new CropHarvestInfo(mature, seedItem, replantState);
        } else if (block instanceof CocoaBlock) {
            int age = state.getValue(CocoaBlock.AGE);
            boolean mature = age >= 2;
            Item seedItem = Items.COCOA_BEANS;
            BlockState replantState = state.setValue(CocoaBlock.AGE, 0);
            return new CropHarvestInfo(mature, seedItem, replantState);
        }

        return null;
    }

    private Item getCropSeedItem(CropBlock cropBlock) {
        if (cropBlock instanceof CarrotBlock) return Items.CARROT;
        if (cropBlock instanceof PotatoBlock) return Items.POTATO;
        if (cropBlock instanceof BeetrootBlock) return Items.BEETROOT_SEEDS;
        if (cropBlock instanceof TorchflowerCropBlock) return Items.TORCHFLOWER_SEEDS;
        if (cropBlock instanceof CornCropBlock) return ModItems.CORN_SEEDS;
        if (cropBlock instanceof GenericCropBlock genericCrop) return genericCrop.getSeed().asItem();
        return Items.WHEAT_SEEDS;
    }

    private record CropHarvestInfo(boolean isMature, Item seedItem, BlockState replantState) {}
}
