package net.enchantedwood.event;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.enchantedwood.item.ModItems;

public class WoodenShearsHarvestHandler {
    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!world.isClientSide() && world instanceof ServerLevel serverWorld) {
                if (player != null && !player.isCreative()) {
                    ItemStack mainHand = player.getMainHandItem();
                    if (mainHand.is(ModItems.WOODEN_SHEARS)) {
                        ItemStack dropStack = ItemStack.EMPTY;
                        if (state.is(BlockTags.LEAVES) || state.getBlock() instanceof LeavesBlock) {
                            dropStack = new ItemStack(state.getBlock().asItem());
                        } else if (state.is(Blocks.COBWEB)) {
                            dropStack = new ItemStack(Items.COBWEB);
                        } else if (state.is(Blocks.VINE)) {
                            dropStack = new ItemStack(Items.VINE);
                        } else if (state.is(Blocks.SHORT_GRASS)) {
                            dropStack = new ItemStack(Items.SHORT_GRASS);
                        } else if (state.is(Blocks.FERN)) {
                            dropStack = new ItemStack(Items.FERN);
                        } else if (state.is(Blocks.DEAD_BUSH)) {
                            dropStack = new ItemStack(Items.DEAD_BUSH);
                        } else if (state.is(Blocks.SEAGRASS)) {
                            dropStack = new ItemStack(Items.SEAGRASS);
                        }

                        if (!dropStack.isEmpty()) {
                            ItemEntity itemEntity = new ItemEntity(
                                    serverWorld,
                                    pos.getX() + 0.5,
                                    pos.getY() + 0.5,
                                    pos.getZ() + 0.5,
                                    dropStack
                            );
                            itemEntity.setDefaultPickUpDelay();
                            serverWorld.addFreshEntity(itemEntity);
                        }
                    }
                }
            }
            return true;
        });
    }
}

