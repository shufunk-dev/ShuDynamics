package net.enchantedwood.item.custom;

import net.minecraft.block.BlockState;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.text.Text;

import java.util.function.Consumer;

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

    public TreeSawItem(SawTier tier, Settings settings) {
        super(settings.maxDamage(tier.getDurability()));
        this.tier = tier;
    }

    public SawTier getTier() {
        return tier;
    }

    public boolean canHarvest(BlockState state) {
        return state.isIn(BlockTags.LOGS) || state.isIn(BlockTags.LEAVES)
                || state.isOf(net.minecraft.block.Blocks.BAMBOO)
                || state.isOf(net.minecraft.block.Blocks.BAMBOO_SAPLING)
                || state.isOf(net.minecraft.block.Blocks.SUGAR_CANE)
                || state.isOf(net.minecraft.block.Blocks.CACTUS)
                || state.isOf(net.minecraft.block.Blocks.VINE)
                || state.isOf(net.minecraft.block.Blocks.COCOA);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        textConsumer.accept(Text.literal("§7Front-Mounted ATV Tree Harvester Saw"));
        textConsumer.accept(Text.literal("§eTier: " + tier.getColorCode() + tier.getName()));
        int remaining = stack.getMaxDamage() - stack.getDamage();
        textConsumer.accept(Text.literal("§eDurability: §f" + remaining + " §7/ " + stack.getMaxDamage() + " logs"));
        textConsumer.accept(Text.literal("§bSawing Speed: §f" + tier.getSpeedMultiplier() + "x"));
        textConsumer.accept(Text.literal("§aMax Tree Felling Cap: §f" + tier.getMaxLogsPerTree() + " logs"));
        textConsumer.accept(Text.literal("§8Install in ATV Tool Slot. Timber, saplings & drops auto-route to trunk."));
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }
}
