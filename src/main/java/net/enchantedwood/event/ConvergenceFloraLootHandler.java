package net.enchantedwood.event;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.world.dimension.ModDimensions;

public class ConvergenceFloraLootHandler {
    public static void register() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world.isClient() || player.isCreative()) {
                return;
            }

            // Exclusively within The Convergence dimension
            if (world.getRegistryKey() == ModDimensions.CONVERGENCE_WORLD_KEY) {
                if (state.isOf(Blocks.SHORT_GRASS) || state.isOf(Blocks.TALL_GRASS)
                        || state.isOf(Blocks.FERN) || state.isOf(Blocks.LARGE_FERN)) {
                    float rand = world.getRandom().nextFloat();
                    if (rand < 0.28f) {
                        ItemStack drop;
                        if (rand < 0.06f) {
                            drop = new ItemStack(ModItems.RICE_SEEDS);
                        } else if (rand < 0.12f) {
                            drop = new ItemStack(ModItems.CUCUMBER_SEEDS);
                        } else if (rand < 0.17f) {
                            drop = new ItemStack(ModItems.AVOCADO);
                        } else if (rand < 0.21f) {
                            drop = new ItemStack(ModItems.WASABI_ROOT);
                        } else if (rand < 0.25f) {
                            drop = new ItemStack(ModItems.DRAGON_FRUIT);
                        } else {
                            drop = new ItemStack(ModItems.STARFRUIT);
                        }
                        Block.dropStack(world, pos, drop);
                    }
                }
            }
        });
    }
}
