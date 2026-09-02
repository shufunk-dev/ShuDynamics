package net.enchantedwood.item.custom;

import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.block.CocoaBlock;
import net.minecraft.block.NetherWartBlock;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class CropHarvesterItem extends Item {
    public enum HarvesterTier {
        IRON("Iron", "§f", 1000, 1.0f, 1),
        STEEL("Steel", "§b", 2000, 1.5f, 1),
        DIAMOND("Diamond", "§b", 4000, 2.5f, 2),
        TITANIUM("Titanium", "§3", 7500, 3.5f, 2),
        NETHERITE("Netherite", "§d", 12000, 5.0f, 3);

        private final String name;
        private final String colorCode;
        private final int durability;
        private final float speedMultiplier;
        private final int radius; // 1 = 3x3, 2 = 5x5 width radius

        HarvesterTier(String name, String colorCode, int durability, float speedMultiplier, int radius) {
            this.name = name;
            this.colorCode = colorCode;
            this.durability = durability;
            this.speedMultiplier = speedMultiplier;
            this.radius = radius;
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

        public int getRadius() {
            return radius;
        }
    }

    private final HarvesterTier tier;

    public CropHarvesterItem(HarvesterTier tier, Settings settings) {
        super(settings.maxDamage(tier.getDurability()));
        this.tier = tier;
    }

    public HarvesterTier getTier() {
        return tier;
    }

    public boolean isMatureCrop(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) {
            return crop.isMature(state);
        }
        if (state.getBlock() instanceof CocoaBlock) {
            return state.get(CocoaBlock.AGE) >= 2;
        }
        if (state.getBlock() instanceof NetherWartBlock) {
            return state.get(NetherWartBlock.AGE) >= 3;
        }
        if (state.isOf(net.minecraft.block.Blocks.BAMBOO)
                || state.isOf(net.minecraft.block.Blocks.BAMBOO_SAPLING)
                || state.isOf(net.minecraft.block.Blocks.SUGAR_CANE)
                || state.isOf(net.minecraft.block.Blocks.CACTUS)) {
            return true;
        }
        if (state.isIn(BlockTags.CROPS)) {
            return true;
        }
        return false;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        textConsumer.accept(Text.literal("§7Front-Mounted ATV Agricultural Harvester"));
        textConsumer.accept(Text.literal("§eTier: " + tier.getColorCode() + tier.getName()));
        int remaining = stack.getMaxDamage() - stack.getDamage();
        textConsumer.accept(Text.literal("§eDurability: §f" + remaining + " §7/ " + stack.getMaxDamage() + " crops"));
        textConsumer.accept(Text.literal("§bReaping Speed: §f" + tier.getSpeedMultiplier() + "x"));
        textConsumer.accept(Text.literal("§aHarvest Width: §f" + (tier.getRadius() * 2 + 1) + " blocks wide"));
        textConsumer.accept(Text.literal("§8Reaps mature crops, auto-replants seeds & routes produce to trunk."));
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }
}
