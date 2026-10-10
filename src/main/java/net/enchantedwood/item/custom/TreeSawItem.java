package net.enchantedwood.item.custom;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.state.BlockState;

public class TreeSawItem extends Item {
    public enum SawTier {
        IRON("Iron", "§f", 800, 1.0f, 64),
        STEEL("Steel", "§b", 1600, 1.5f, 160),
        DIAMOND("Diamond", "§b", 3500, 2.5f, 350),
        TITANIUM("Titanium", "§3", 6000, 3.5f, 600),
        NETHERITE("Netherite", "§d", 10000, 5.0f, 1200);

        private final String name;
        private final String colorCode;
        private final int durability;
        private final float speedMultiplier;
        private final int maxLogsPerTree;

        SawTier(String name, String colorCode, int durability, float speedMultiplier, int maxLogsPerTree) {
            this.name = name;
            this.colorCode = colorCode;
            this.durability = durability;
            this.speedMultiplier = speedMultiplier;
            this.maxLogsPerTree = maxLogsPerTree;
        }

        public String getName() {
            return name;
        }

        public String getColorCode() {
            return colorCode;
        }

        public int getDurability() {
            return durability;
        }

        public float getSpeedMultiplier() {
            return speedMultiplier;
        }

        public int getMaxLogsPerTree() {
            return maxLogsPerTree;
        }
    }

    private final SawTier tier;

    public TreeSawItem(SawTier tier, Properties settings) {
        super(settings.durability(tier.getDurability()));
        this.tier = tier;
    }

    public SawTier getTier() {
        return tier;
    }

    public boolean canHarvest(BlockState state) {
        return state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)
                || state.is(net.minecraft.world.level.block.Blocks.BAMBOO)
                || state.is(net.minecraft.world.level.block.Blocks.BAMBOO_SAPLING)
                || state.is(net.minecraft.world.level.block.Blocks.SUGAR_CANE)
                || state.is(net.minecraft.world.level.block.Blocks.CACTUS)
                || state.is(net.minecraft.world.level.block.Blocks.VINE)
                || state.is(net.minecraft.world.level.block.Blocks.COCOA);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        textConsumer.accept(Component.literal("§7Front-Mounted ATV Tree Harvester Saw"));
        textConsumer.accept(Component.literal("§eTier: " + tier.getColorCode() + tier.getName()));
        int remaining = stack.getMaxDamage() - stack.getDamageValue();
        textConsumer.accept(Component.literal("§eDurability: §f" + remaining + " §7/ " + stack.getMaxDamage() + " logs"));
        textConsumer.accept(Component.literal("§bSawing Speed: §f" + tier.getSpeedMultiplier() + "x"));
        textConsumer.accept(Component.literal("§aMax Tree Felling Cap: §f" + tier.getMaxLogsPerTree() + " logs"));
        textConsumer.accept(Component.literal("§8Install in ATV Tool Slot. Timber, saplings & drops auto-route to trunk."));
        super.appendHoverText(stack, context, displayComponent, textConsumer, type);
    }
}
