package net.enchantedwood.item.custom;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class HeadlightsItem extends Item {
    public enum LightTier {
        HALOGEN("Halogen Headlights", "§e", 12, "Standard warm beam (12 Light, 8m range)."),
        LED("LED Floodlights", "§b", 15, "Wide-angle floodlight array (15 Light, 16m broad cone)."),
        XENON("Xenon High-Beams", "§d", 15, "Piercing long-range high-beams (15 Light, 32m reach). Outlines night hazards in forward beam.");

        private final String name;
        private final String colorCode;
        private final int lightLevel;
        private final String description;

        LightTier(String name, String colorCode, int lightLevel, String description) {
            this.name = name;
            this.colorCode = colorCode;
            this.lightLevel = lightLevel;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getColorCode() {
            return colorCode;
        }

        public int getLightLevel() {
            return lightLevel;
        }

        public String getDescription() {
            return description;
        }
    }

    private final LightTier tier;

    public HeadlightsItem(LightTier tier, Settings settings) {
        super(settings);
        this.tier = tier;
    }

    public LightTier getTier() {
        return tier;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        textConsumer.accept(Text.literal("§7ATV Automotive Headlights Module"));
        textConsumer.accept(Text.literal("§eTier: " + tier.getColorCode() + tier.getName()));
        textConsumer.accept(Text.literal("§bLuminance: §fLevel " + tier.getLightLevel()));
        textConsumer.accept(Text.literal("§8" + tier.getDescription()));
        textConsumer.accept(Text.literal("§6Required component for ATV assembly in the Vehicle Fabricator."));
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }
}
