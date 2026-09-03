package net.enchantedwood.item.custom;

import net.minecraft.block.BlockState;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;
import java.util.function.Consumer;

public class BroadAxeItem extends Item {
    private static final ThreadLocal<Boolean> IS_FELLING = ThreadLocal.withInitial(() -> false);
    private final int maxLogs;

    public BroadAxeItem(Settings settings) {
        this(settings, 384);
    }

    public BroadAxeItem(Settings settings, int maxLogs) {
        super(settings);
        this.maxLogs = maxLogs;
    }

    @Override
    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!world.isClient() && miner instanceof ServerPlayerEntity player && !IS_FELLING.get()) {
            // Only trigger tree felling when not sneaking and mining a valid log / wood block
            if (!player.isSneaking() && isLogBlock(state)) {
                IS_FELLING.set(true);
                try {
                    fellTree(stack, (ServerWorld) world, pos, player);
                } finally {
                    IS_FELLING.set(false);
                }
            }
        }
        return super.postMine(stack, world, state, pos, miner);
    }

    private void fellTree(ItemStack stack, ServerWorld world, BlockPos origin, ServerPlayerEntity player) {
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        List<BlockPos> logsToBreak = new ArrayList<>();
        Set<BlockPos> leavesToBreak = new HashSet<>();

        queue.add(origin);
        visited.add(origin);

        int maxLeaves = maxLogs * 3;

        while (!queue.isEmpty() && logsToBreak.size() < maxLogs) {
            BlockPos current = queue.poll();

            for (int ox = -1; ox <= 1; ox++) {
                for (int oy = -1; oy <= 2; oy++) {
                    for (int oz = -1; oz <= 1; oz++) {
                        if (ox == 0 && oy == 0 && oz == 0) continue;

                        BlockPos neighbor = current.add(ox, oy, oz);
                        if (visited.add(neighbor)) {
                            // Keep search within reasonable bounds from origin
                            if (Math.abs(neighbor.getX() - origin.getX()) > 32 ||
                                Math.abs(neighbor.getZ() - origin.getZ()) > 32 ||
                                neighbor.getY() < origin.getY() - 5 ||
                                neighbor.getY() > origin.getY() + 64) {
                                continue;
                            }

                            BlockState st = world.getBlockState(neighbor);
                            if (isLogBlock(st)) {
                                logsToBreak.add(neighbor);
                                queue.add(neighbor);
                            } else if (isLeafBlock(st) && leavesToBreak.size() < maxLeaves) {
                                leavesToBreak.add(neighbor);
                            }
                        }
                    }
                }
            }
        }

        // Break logs from bottom-to-top or top-to-bottom
        for (BlockPos logPos : logsToBreak) {
            if (stack.isEmpty()) break;

            BlockState st = world.getBlockState(logPos);
            if (isLogBlock(st)) {
                player.interactionManager.tryBreakBlock(logPos);
                stack.damage(1, player, EquipmentSlot.MAINHAND);
            }
        }

        // Break connected leaves so tree drops saplings, apples, sticks cleanly without leaving floating foliage
        for (BlockPos leafPos : leavesToBreak) {
            BlockState st = world.getBlockState(leafPos);
            if (isLeafBlock(st)) {
                world.breakBlock(leafPos, true, player);
            }
        }
    }

    public static boolean isLogBlock(BlockState state) {
        return state.isIn(BlockTags.LOGS)
                || state.isOf(net.minecraft.block.Blocks.MANGROVE_ROOTS)
                || state.isOf(net.minecraft.block.Blocks.MUDDY_MANGROVE_ROOTS)
                || state.isOf(net.minecraft.block.Blocks.BAMBOO_BLOCK)
                || state.isOf(net.minecraft.block.Blocks.STRIPPED_BAMBOO_BLOCK)
                || state.isOf(net.minecraft.block.Blocks.CRIMSON_STEM)
                || state.isOf(net.minecraft.block.Blocks.WARPED_STEM)
                || state.isOf(net.minecraft.block.Blocks.STRIPPED_CRIMSON_STEM)
                || state.isOf(net.minecraft.block.Blocks.STRIPPED_WARPED_STEM)
                || state.isOf(net.minecraft.block.Blocks.MUSHROOM_STEM);
    }

    public static boolean isLeafBlock(BlockState state) {
        return state.isIn(BlockTags.LEAVES)
                || state.isIn(BlockTags.WART_BLOCKS)
                || state.isOf(net.minecraft.block.Blocks.SHROOMLIGHT)
                || state.isOf(net.minecraft.block.Blocks.MANGROVE_LEAVES)
                || state.isOf(net.minecraft.block.Blocks.AZALEA_LEAVES)
                || state.isOf(net.minecraft.block.Blocks.FLOWERING_AZALEA_LEAVES)
                || state.isOf(net.minecraft.block.Blocks.CHERRY_LEAVES)
                || state.isOf(net.minecraft.block.Blocks.VINE);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        textConsumer.accept(Text.literal("§6✦ Heavy Lumber Broad Axe"));
        textConsumer.accept(Text.literal("§7Fells §eentire connected trees §7and harvests leaves in one strike."));
        textConsumer.accept(Text.literal("§8(Hold §fShift §8while chopping to harvest a single log)"));
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }
}
