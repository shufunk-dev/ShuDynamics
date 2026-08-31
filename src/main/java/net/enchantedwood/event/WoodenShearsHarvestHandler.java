package net.enchantedwood.event;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.enchantedwood.item.ModItems;

public class WoodenShearsHarvestHandler {
    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!world.isClient() && world instanceof ServerWorld serverWorld) {
                if (player != null && !player.isCreative()) {
                    ItemStack mainHand = player.getMainHandStack();
                    if (mainHand.isOf(ModItems.WOODEN_SHEARS)) {
                        ItemStack dropStack = ItemStack.EMPTY;
                        if (state.isIn(BlockTags.LEAVES) || state.getBlock() instanceof LeavesBlock) {
                            dropStack = new ItemStack(state.getBlock().asItem());
                        } else if (state.isOf(Blocks.COBWEB)) {
                            dropStack = new ItemStack(Items.COBWEB);
                        } else if (state.isOf(Blocks.VINE)) {
                            dropStack = new ItemStack(Items.VINE);
                        } else if (state.isOf(Blocks.SHORT_GRASS)) {
                            dropStack = new ItemStack(Items.SHORT_GRASS);
                        } else if (state.isOf(Blocks.FERN)) {
                            dropStack = new ItemStack(Items.FERN);
                        } else if (state.isOf(Blocks.DEAD_BUSH)) {
                            dropStack = new ItemStack(Items.DEAD_BUSH);
                        } else if (state.isOf(Blocks.SEAGRASS)) {
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
                            itemEntity.setToDefaultPickupDelay();
                            serverWorld.spawnEntity(itemEntity);
                        }
                    }
                }
            }
            return true;
        });
    }
}

