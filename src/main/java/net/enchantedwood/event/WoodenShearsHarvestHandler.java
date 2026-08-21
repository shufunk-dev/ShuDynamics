package net.enchantedwood.event;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.enchantedwood.item.ModItems;

public class WoodenShearsHarvestHandler {
    public static void register() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!world.isClient() && world instanceof ServerWorld serverWorld) {
                if (player != null && !player.isCreative()) {
                    ItemStack mainHand = player.getMainHandStack();
                    if (mainHand.isOf(ModItems.WOODEN_SHEARS)) {
                        if (state.isIn(BlockTags.LEAVES) || state.getBlock() instanceof LeavesBlock) {
                            ItemStack leafStack = new ItemStack(state.getBlock().asItem());
                            if (!leafStack.isEmpty()) {
                                ItemEntity itemEntity = new ItemEntity(
                                        serverWorld,
                                        pos.getX() + 0.5,
                                        pos.getY() + 0.5,
                                        pos.getZ() + 0.5,
                                        leafStack
                                );
                                itemEntity.setToDefaultPickupDelay();
                                serverWorld.spawnEntity(itemEntity);
                            }
                        }
                    }
                }
            }
        });
    }
}
