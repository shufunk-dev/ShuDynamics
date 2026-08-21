package net.enchantedwood.item.custom;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CarrotsBlock;
import net.minecraft.block.CocoaBlock;
import net.minecraft.block.CropBlock;
import net.minecraft.block.NetherWartBlock;
import net.minecraft.block.PotatoesBlock;
import net.minecraft.block.BeetrootsBlock;
import net.minecraft.block.TorchflowerBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.item.ToolMaterial;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.List;

public class AutoHarvestHoeItem extends HoeItem {

    public AutoHarvestHoeItem(ToolMaterial material, float attackDamage, float attackSpeed, Settings settings) {
        super(material, attackDamage, attackSpeed, settings);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        BlockState state = world.getBlockState(pos);
        PlayerEntity player = context.getPlayer();
        Hand hand = context.getHand();
        ItemStack stack = context.getStack();

        if (player == null) {
            return super.useOnBlock(context);
        }

        CropHarvestInfo info = getCropHarvestInfo(state);
        if (info != null && info.isMature) {
            if (world.isClient()) {
                return ActionResult.SUCCESS;
            }

            if (world instanceof ServerWorld serverWorld) {
                BlockEntity blockEntity = world.getBlockEntity(pos);
                List<ItemStack> drops = Block.getDroppedStacks(state, serverWorld, pos, blockEntity, player, stack);

                boolean replanted = false;
                if (!player.isCreative()) {
                    ItemStack seedStack = findSeedInInventory(player, info.seedItem);
                    if (!seedStack.isEmpty()) {
                        seedStack.decrement(1);
                        replanted = true;
                    } else {
                        // Use 1 seed from harvested drops to replant automatically
                        for (ItemStack drop : drops) {
                            if (drop.isOf(info.seedItem) && !drop.isEmpty()) {
                                drop.decrement(1);
                                replanted = true;
                                break;
                            }
                        }
                    }
                } else {
                    replanted = true;
                }

                if (replanted) {
                    world.setBlockState(pos, info.replantState, Block.NOTIFY_ALL);
                    world.emitGameEvent(player, GameEvent.BLOCK_CHANGE, pos);

                    for (ItemStack drop : drops) {
                        if (!drop.isEmpty()) {
                            Block.dropStack(world, pos, drop);
                        }
                    }

                    EquipmentSlot slot = hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                    stack.damage(1, player, slot);

                    world.playSound(null, pos, SoundEvents.BLOCK_CROP_BREAK, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    world.playSound(null, pos, SoundEvents.ITEM_CROP_PLANT, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    player.incrementStat(Stats.USED.getOrCreateStat(this));

                    return ActionResult.SUCCESS;
                }
            }
        }

        return super.useOnBlock(context);
    }

    private ItemStack findSeedInInventory(PlayerEntity player, Item seedItem) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack itemStack = player.getInventory().getStack(i);
            if (itemStack.isOf(seedItem)) {
                return itemStack;
            }
        }
        return ItemStack.EMPTY;
    }

    private CropHarvestInfo getCropHarvestInfo(BlockState state) {
        Block block = state.getBlock();

        if (block instanceof CropBlock cropBlock) {
            boolean mature = cropBlock.isMature(state);
            Item seedItem = getCropSeedItem(cropBlock);
            BlockState replantState = cropBlock.withAge(0);
            return new CropHarvestInfo(mature, seedItem, replantState);
        } else if (block instanceof NetherWartBlock) {
            int age = state.get(NetherWartBlock.AGE);
            boolean mature = age >= 3;
            Item seedItem = Items.NETHER_WART;
            BlockState replantState = Blocks.NETHER_WART.getDefaultState();
            return new CropHarvestInfo(mature, seedItem, replantState);
        } else if (block instanceof CocoaBlock) {
            int age = state.get(CocoaBlock.AGE);
            boolean mature = age >= 2;
            Item seedItem = Items.COCOA_BEANS;
            BlockState replantState = state.with(CocoaBlock.AGE, 0);
            return new CropHarvestInfo(mature, seedItem, replantState);
        }

        return null;
    }

    private Item getCropSeedItem(CropBlock cropBlock) {
        if (cropBlock instanceof CarrotsBlock) return Items.CARROT;
        if (cropBlock instanceof PotatoesBlock) return Items.POTATO;
        if (cropBlock instanceof BeetrootsBlock) return Items.BEETROOT_SEEDS;
        if (cropBlock instanceof TorchflowerBlock) return Items.TORCHFLOWER_SEEDS;
        return Items.WHEAT_SEEDS;
    }

    private record CropHarvestInfo(boolean isMature, Item seedItem, BlockState replantState) {}
}
