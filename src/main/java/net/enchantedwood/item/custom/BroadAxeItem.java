package net.enchantedwood.item.custom;

import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BroadAxeItem extends Item {
    private static final ThreadLocal<Boolean> IS_FELLING = ThreadLocal.withInitial(() -> false);
    private final int maxLogs;

    public BroadAxeItem(Properties settings) {
        this(settings, 384);
    }

    public BroadAxeItem(Properties settings, int maxLogs) {
        super(settings);
        this.maxLogs = maxLogs;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level world, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!world.isClientSide() && miner instanceof ServerPlayer player && !IS_FELLING.get()) {
            // Only trigger tree felling when not sneaking and mining a valid log / wood block
            if (!player.isShiftKeyDown() && isLogBlock(state)) {
                IS_FELLING.set(true);
                try {
                    fellTree(stack, (ServerLevel) world, pos, player);
                } finally {
                    IS_FELLING.set(false);
                }
            }
        }
        return super.mineBlock(stack, world, state, pos, miner);
    }

    private void fellTree(ItemStack stack, ServerLevel world, BlockPos origin, ServerPlayer player) {
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

                        BlockPos neighbor = current.offset(ox, oy, oz);
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
                player.gameMode.destroyBlock(logPos);
                stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            }
        }

        // Break connected leaves so tree drops saplings, apples, sticks cleanly without leaving floating foliage
        for (BlockPos leafPos : leavesToBreak) {
            BlockState st = world.getBlockState(leafPos);
            if (isLeafBlock(st)) {
                world.destroyBlock(leafPos, true, player);
            }
        }
    }

    public static boolean isLogBlock(BlockState state) {
        return state.is(BlockTags.LOGS)
                || state.is(net.minecraft.world.level.block.Blocks.MANGROVE_ROOTS)
                || state.is(net.minecraft.world.level.block.Blocks.MUDDY_MANGROVE_ROOTS)
                || state.is(net.minecraft.world.level.block.Blocks.BAMBOO_BLOCK)
                || state.is(net.minecraft.world.level.block.Blocks.STRIPPED_BAMBOO_BLOCK)
                || state.is(net.minecraft.world.level.block.Blocks.CRIMSON_STEM)
                || state.is(net.minecraft.world.level.block.Blocks.WARPED_STEM)
                || state.is(net.minecraft.world.level.block.Blocks.STRIPPED_CRIMSON_STEM)
                || state.is(net.minecraft.world.level.block.Blocks.STRIPPED_WARPED_STEM)
                || state.is(net.minecraft.world.level.block.Blocks.MUSHROOM_STEM);
    }

    public static boolean isLeafBlock(BlockState state) {
        return state.is(BlockTags.LEAVES)
                || state.is(BlockTags.WART_BLOCKS)
                || state.is(net.minecraft.world.level.block.Blocks.SHROOMLIGHT)
                || state.is(net.minecraft.world.level.block.Blocks.MANGROVE_LEAVES)
                || state.is(net.minecraft.world.level.block.Blocks.AZALEA_LEAVES)
                || state.is(net.minecraft.world.level.block.Blocks.FLOWERING_AZALEA_LEAVES)
                || state.is(net.minecraft.world.level.block.Blocks.CHERRY_LEAVES)
                || state.is(net.minecraft.world.level.block.Blocks.VINE);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        textConsumer.accept(Component.literal("§6✦ Heavy Lumber Broad Axe"));
        textConsumer.accept(Component.literal("§7Fells §eentire connected trees §7and harvests leaves in one strike."));
        textConsumer.accept(Component.literal("§8(Hold §fShift §8while chopping to harvest a single log)"));
        super.appendHoverText(stack, context, displayComponent, textConsumer, type);
    }
}
