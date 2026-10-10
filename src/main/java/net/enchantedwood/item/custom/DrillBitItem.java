package net.enchantedwood.item.custom;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.state.BlockState;

public class DrillBitItem extends Item {
    public enum DrillTier {
        IRON("Iron", 600, 1.0f, 2, "§7"),
        STEEL("Steel", 1200, 1.5f, 2, "§f"),
        DIAMOND("Diamond", 3000, 2.5f, 3, "§b"),
        TITANIUM("Titanium", 5000, 3.5f, 3, "§9"),
        NETHERITE("Netherite", 8000, 5.0f, 4, "§5");

        private final String name;
        private final int durability;
        private final float speedMultiplier;
        private final int harvestLevel;
        private final String colorCode;

        DrillTier(String name, int durability, float speedMultiplier, int harvestLevel, String colorCode) {
            this.name = name;
            this.durability = durability;
            this.speedMultiplier = speedMultiplier;
            this.harvestLevel = harvestLevel;
            this.colorCode = colorCode;
        }

        public String getName() {
            return name;
        }

        public int getDurability() {
            return durability;
        }

        public float getSpeedMultiplier() {
            return speedMultiplier;
        }

        public int getHarvestLevel() {
            return harvestLevel;
        }

        public String getColorCode() {
            return colorCode;
        }
    }

    private final DrillTier tier;

    public DrillBitItem(DrillTier tier, Properties settings) {
        super(settings.durability(tier.getDurability()));
        this.tier = tier;
    }

    public DrillTier getTier() {
        return tier;
    }

    public boolean canHarvest(BlockState state) {
        if (state.isAir()) return false;
        if (state.getDestroySpeed(null, null) < 0) return false; // Unbreakable (Bedrock, End Portal, etc.)

        int level = tier.getHarvestLevel();
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL) && level < 3) return false;
        if (state.is(BlockTags.NEEDS_IRON_TOOL) && level < 2) return false;
        if (state.is(BlockTags.NEEDS_STONE_TOOL) && level < 1) return false;

        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay displayComponent, java.util.function.Consumer<Component> textConsumer, TooltipFlag type) {
        textConsumer.accept(Component.literal("§7Front-Mounted ATV Mining Drill Bit"));
        textConsumer.accept(Component.literal("§eTier: " + tier.getColorCode() + tier.getName()));
        int remaining = stack.getMaxDamage() - stack.getDamageValue();
        textConsumer.accept(Component.literal("§eDurability: §f" + remaining + " §7/ " + stack.getMaxDamage() + " blocks"));
        textConsumer.accept(Component.literal("§bExcavation Speed: §f" + tier.getSpeedMultiplier() + "x"));
        textConsumer.accept(Component.literal("§8Install in ATV Drill Slot. Mined blocks auto-route to cargo trunk."));
        super.appendHoverText(stack, context, displayComponent, textConsumer, type);
    }
}
