package net.enchantedwood.item.custom;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;

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

    public CropHarvesterItem(HarvesterTier tier, Properties settings) {
        super(settings.durability(tier.getDurability()));
        this.tier = tier;
    }

    public HarvesterTier getTier() {
        return tier;
    }

    public boolean isMatureCrop(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        if (state.getBlock() instanceof CocoaBlock) {
            return state.getValue(CocoaBlock.AGE) >= 2;
        }
        if (state.getBlock() instanceof NetherWartBlock) {
            return state.getValue(NetherWartBlock.AGE) >= 3;
        }
        if (state.is(net.minecraft.world.level.block.Blocks.BAMBOO)
                || state.is(net.minecraft.world.level.block.Blocks.BAMBOO_SAPLING)
                || state.is(net.minecraft.world.level.block.Blocks.SUGAR_CANE)
                || state.is(net.minecraft.world.level.block.Blocks.CACTUS)) {
            return true;
        }
        if (state.is(BlockTags.CROPS)) {
            return true;
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        textConsumer.accept(Component.literal("§7Front-Mounted ATV Agricultural Harvester"));
        textConsumer.accept(Component.literal("§eTier: " + tier.getColorCode() + tier.getName()));
        int remaining = stack.getMaxDamage() - stack.getDamageValue();
        textConsumer.accept(Component.literal("§eDurability: §f" + remaining + " §7/ " + stack.getMaxDamage() + " crops"));
        textConsumer.accept(Component.literal("§bReaping Speed: §f" + tier.getSpeedMultiplier() + "x"));
        textConsumer.accept(Component.literal("§aHarvest Width: §f" + (tier.getRadius() * 2 + 1) + " blocks wide"));
        textConsumer.accept(Component.literal("§8Reaps mature crops, auto-replants seeds & routes produce to trunk."));
        super.appendHoverText(stack, context, displayComponent, textConsumer, type);
    }
}
