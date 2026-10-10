package net.enchantedwood.event;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.enchantedwood.item.ModItems;
import net.enchantedwood.world.dimension.ModDimensions;

public class ConvergenceFloraLootHandler {
    public static void register() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world.isClientSide() || player.isCreative()) {
                return;
            }

            // Exclusively within The Convergence dimension
            if (world.dimension() == ModDimensions.CONVERGENCE_WORLD_KEY) {
                if (state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS)
                        || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN)) {
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
                        Block.popResource(world, pos, drop);
                    }
                }
            }
        });
    }
}
